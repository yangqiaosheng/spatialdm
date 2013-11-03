package de.fraunhofer.iais.spatial.script.db;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fraunhofer.iais.spatial.util.DBUtil;

public class UpdateFlickrRegionId {
	/**
	* Logger for this class
	*/
	private static final Logger logger = LoggerFactory.getLogger(UpdateFlickrRegionId.class);

	final static int SELECT_BATCH_SIZE = 500;
	static String AREAS_TABLE_NAME = "flickr_world_area";
	static String PHOTOS_TABLE_NAME = "flickr_world_topviewed_15m";
	static Calendar startDate;

	static DBUtil db = new DBUtil("/jdbc_pg.properties", 3, 1);

	public static void main(String[] args) throws SQLException, IOException {
		System.out.println("\nPlease input the Area TableName:\n[Default: " + AREAS_TABLE_NAME + "]");
		String areaTableName = new BufferedReader(new InputStreamReader(System.in)).readLine();
		if (StringUtils.isNotEmpty(areaTableName)) {
			AREAS_TABLE_NAME = areaTableName;
		}
		System.out.println("Area Table:" + AREAS_TABLE_NAME);

		System.out.println("\nPlease input the Photo TableName:\n[Default: " + PHOTOS_TABLE_NAME + "]");
		String photoTableName = new BufferedReader(new InputStreamReader(System.in)).readLine();
		if (StringUtils.isNotEmpty(photoTableName)) {
			PHOTOS_TABLE_NAME = photoTableName;
		}
		System.out.println("Photo Table:" + PHOTOS_TABLE_NAME);

		long start = System.currentTimeMillis();
		startDate = Calendar.getInstance();
		startDate.setTimeInMillis(start);
		UpdateFlickrRegionId t = new UpdateFlickrRegionId();
		t.updateRegionId();

		long end = System.currentTimeMillis();
		Calendar endDate = Calendar.getInstance();
		endDate.setTimeInMillis(end);

		logger.info("start time:" + startDate.getTime()); //$NON-NLS-1$
		logger.info("end time:" + endDate.getTime()); //$NON-NLS-1$
		logger.info("main(String[]) - escaped time:" + (System.currentTimeMillis() - start) / 1000.0); //$NON-NLS-1$
	}

	private void updateRegionId() throws SQLException {
		Date beginDate = new Date();
		logger.info("updateRegionId() beginDate" + beginDate);
		Connection selectConn = db.getConn();
		Connection conn = db.getConn();
		selectConn.setAutoCommit(false);
		//Oracle
//		PreparedStatement countStmt = db.getPstmt(conn, "select NUM_ROWS as num from user_tables where TABLE_NAME = '"+ PHOTOS_TABLE_NAME.toUpperCase() + "'" );

		//PostGIS
		PreparedStatement countStmt = db.getPstmt(conn, "select n_live_tup as num from pg_stat_user_tables where relname = '" + PHOTOS_TABLE_NAME.toLowerCase() + "'");
		/* non finished 
		PreparedStatement countStmt = db.getPstmt(conn, "select count(*) as num from " + PHOTOS_TABLE_NAME + " where region_625_id is null");
		*/
		
 		PreparedStatement selectStmt = db.getPstmt(selectConn, "select photo_id, longitude, latitude from " + PHOTOS_TABLE_NAME);
  		/* non finished 
		PreparedStatement selectStmt = db.getPstmt(selectConn, "select photo_id, longitude, latitude from " + PHOTOS_TABLE_NAME + " where region_625_id is null");
		*/
 		
		selectStmt.setFetchSize(SELECT_BATCH_SIZE);
		ResultSet countRs = db.getRs(countStmt);
		ResultSet selectPhotoRs = db.getRs(selectStmt);
		String updateStr = "update " + PHOTOS_TABLE_NAME + " set" + 
		        " region_625_id = ?," + 
				" region_1250_id = ?," +
				" region_2500_id = ?," +
				" region_5000_id = ?," +
				" region_10000_id = ?," +
				" region_20000_id = ?," +
				" region_40000_id = ?," +
				" region_80000_id = ?," +
				" region_160000_id = ?," +
				" region_320000_id = ?," +
				" region_640000_id = ?," +
				" region_1280000_id = ?," +
				" region_2560000_id = ?" +
				" where photo_id = ?";
		PreparedStatement updateStmt = db.getPstmt(conn, updateStr);
		
		PreparedStatement selectAreaIdPstmt = null;
		ResultSet areaIdRs = null;
		
		System.out.println(countStmt);
		
		
		int totalNum = 0;
		try {
			if (countRs.next()) {
				totalNum = countRs.getInt("num");
			}
			db.close(countRs);

			int updatedNum = 0;
			while (selectPhotoRs.next()) {

				long photoId = selectPhotoRs.getLong("photo_id");
				double longitude = selectPhotoRs.getDouble("longitude");
				double latitude = selectPhotoRs.getDouble("latitude");
				System.out.println("longitude:" + longitude);
				System.out.println("latitude:" + latitude);

				System.out.println("totalNum/updatedNum: " + totalNum + "/" + updatedNum++);
				
				if(longitude == 0 || latitude == 0){
					continue;
				}

				//Oracle
//				selectAreaIdPstmt = db.getPstmt(conn, "select ID, RADIUS from " + AREAS_TABLE_NAME + " c, user_sdo_geom_metadata m" + " WHERE m.table_name = '" + AREAS_TABLE_NAME.toUpperCase() + "' and sdo_relate(c.geom, SDO_geometry(2001,8307,SDO_POINT_TYPE(" + longitude + ", " + latitude + ", NULL),NULL,NULL),'mask=anyinteract') = 'TRUE'");

				//PostGIS
				selectAreaIdPstmt = db.getPstmt(conn, "select id, radius from " + AREAS_TABLE_NAME + " t where ST_Intersects(ST_GeomFromEWKT('SRID=4326;POINT(" + longitude + " " + latitude + ")'), t.geom::geometry)");

				areaIdRs = db.getRs(selectAreaIdPstmt);
				
				Map<String, Integer> paraMaps = new HashMap<String, Integer>();
				while (areaIdRs.next()) {
					int areaId = areaIdRs.getInt("id");
					String radius = areaIdRs.getString("radius");
					paraMaps.put(radius, areaId);
				}

				if (paraMaps.size() == 0) {
					continue;
				}

				//areaId = 0 when no regions are matched
				updateStmt.setInt(1, MapUtils.getIntValue(paraMaps, "625"));
				updateStmt.setInt(2, MapUtils.getIntValue(paraMaps, "1250"));
				updateStmt.setInt(3, MapUtils.getIntValue(paraMaps, "2500"));
				updateStmt.setInt(4, MapUtils.getIntValue(paraMaps, "5000"));
				updateStmt.setInt(5, MapUtils.getIntValue(paraMaps, "10000"));
				updateStmt.setInt(6, MapUtils.getIntValue(paraMaps, "20000"));
				updateStmt.setInt(7, MapUtils.getIntValue(paraMaps, "40000"));
				updateStmt.setInt(8, MapUtils.getIntValue(paraMaps, "80000"));
				updateStmt.setInt(9, MapUtils.getIntValue(paraMaps, "160000"));
				updateStmt.setInt(10, MapUtils.getIntValue(paraMaps, "320000"));
				updateStmt.setInt(11, MapUtils.getIntValue(paraMaps, "640000"));
				updateStmt.setInt(12, MapUtils.getIntValue(paraMaps, "1280000"));
				updateStmt.setInt(13, MapUtils.getIntValue(paraMaps, "2560000"));
				updateStmt.setLong(14, photoId);
				
				System.out.println("photo_id: " + photoId + "\t|updateStr: " + updateStr + "\t|paraMaps: " + paraMaps);
				updateStmt.addBatch();
				
				if (updatedNum % SELECT_BATCH_SIZE == 0) {
					updateStmt.executeBatch();
					System.err.println("Commit Update Batch | Batch Size: " + SELECT_BATCH_SIZE);
				}
				
				db.close(areaIdRs);
				db.close(selectAreaIdPstmt);
			}
			updateStmt.executeBatch();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			db.close(areaIdRs);
			db.close(selectAreaIdPstmt);
			db.close(updateStmt);
			db.close(selectPhotoRs);
			db.close(selectStmt);
			db.close(countStmt);
			db.close(selectConn);
			db.close(conn);
		}
		Date endDate = new Date();
		logger.info("updateRegionId() endDate" + endDate + " |cost:" + (endDate.getTime() - beginDate.getTime()) / 1000 + " seconds");
	}

}
