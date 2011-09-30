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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fraunhofer.iais.spatial.util.DBUtil;

public class UpdateFlickrRegionId {
	/**
	* Logger for this class
	*/
	private static final Logger logger = LoggerFactory.getLogger(UpdateFlickrRegionId.class);

	final static int SELECT_BATCH_SIZE = 400;
	static String AREAS_TABLE_NAME = "flickr_world_area";
	static String PHOTOS_TABLE_NAME = "flickr_world_topviewed_5m";
	static Calendar startDate;

	static DBUtil db = new DBUtil("/jdbc_pg.properties", 18, 3);

	public static void main(String[] args) throws SQLException, IOException {

		System.out.println("\nPlease input the Area TableName:\n[Default: " + AREAS_TABLE_NAME + "]");
		String areaTableName = new BufferedReader(new InputStreamReader(System.in)).readLine();
		if (StringUtils.isNotEmpty(areaTableName)){
			AREAS_TABLE_NAME = areaTableName;
		}
		System.out.println("Area Table:" + AREAS_TABLE_NAME);

		System.out.println("\nPlease input the Photo TableName:\n[Default: " + PHOTOS_TABLE_NAME + "]");
		String photoTableName = new BufferedReader(new InputStreamReader(System.in)).readLine();
		if (StringUtils.isNotEmpty(photoTableName)){
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
//		conn.setAutoCommit(false);
		PreparedStatement countStmt = db.getPstmt(conn, "select n_tup_ins as num from pg_stat_user_tables where relname = '"+ PHOTOS_TABLE_NAME + "'" );
		PreparedStatement selectStmt = db.getPstmt(selectConn, "select photo_id, longitude, latitude from " + PHOTOS_TABLE_NAME);
		selectStmt.setFetchSize(SELECT_BATCH_SIZE);
		ResultSet countRs = db.getRs(countStmt);
		ResultSet rs = db.getRs(selectStmt);

		int totalNum = 0;
		try {
			if (countRs.next()) {
				totalNum = countRs.getInt("num");
			}
			db.close(countRs);

			int updatedNum = 0;
			while (rs.next()) {
				long photoId = rs.getLong("photo_id");
				double longitude = rs.getDouble("longitude");
				double latitude = rs.getDouble("latitude");
				System.out.println("longitude:" + longitude);
				System.out.println("latitude:" + latitude);

				System.out.println("totalNum/updatedNum: " + totalNum + "/" + updatedNum++);
				PreparedStatement selectAreaIdPstmt = db.getPstmt(conn, "select id, radius from " + AREAS_TABLE_NAME + " t where ST_Intersects(ST_GeomFromEWKT('SRID=4326;POINT(" + longitude + " " + latitude + ")'), t.geom::geometry)");
//				PreparedStatement selectAreaIdPstmt = db.getPstmt(conn, "select region_id as id, radius from " + PHOTOS_TABLE_NAME + "_REGION_ID where photo_id = ?");
//				selectAreaIdPstmt.setLong(1, photoId);
				ResultSet areaIdRs = db.getRs(selectAreaIdPstmt);
				Map<String, String> paraMaps = new HashMap<String, String>();
				while (areaIdRs.next()) {
					int areaId = areaIdRs.getInt("id");
					String radius = areaIdRs.getString("radius");
					paraMaps.put(radius, " region_" + radius + "_id = " + areaId);
				}

				List<String> paraStrs = new ArrayList<String>();

				for(Map.Entry<String, String> entry : paraMaps.entrySet()){
					paraStrs.add(entry.getValue());
				}

				String updateStr = "update " + PHOTOS_TABLE_NAME + " set" + StringUtils.join(paraStrs, ",") + " where photo_id = ?";
				PreparedStatement updateStmt = db.getPstmt(conn, updateStr);
				updateStmt.setLong(1, photoId);
				System.out.println("photo_id: " + photoId + "\t|updateStr: " + updateStr);
				updateStmt.executeUpdate();
				db.close(updateStmt);
				db.close(areaIdRs);
				db.close(selectAreaIdPstmt);
//				if(updatedNum/SELECT_BATCH_SIZE == 0){
//					conn.commit();
//				}
			}
//			conn.commit();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			db.close(rs);
			db.close(selectStmt);
			db.close(countStmt);
			db.close(selectConn);
			db.close(conn);
		}
		Date endDate = new Date();
		logger.info("updateRegionId() endDate" + endDate +" |cost:" + (endDate.getTime() - beginDate.getTime()) / 1000 + " seconds");
	}

}