package de.fraunhofer.iais.spatial.script.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aetrion.flickr.photos.Photo;

import de.fraunhofer.iais.spatial.dao.FlickrAreaDao;
import de.fraunhofer.iais.spatial.dto.FlickrAreaDto.Level;
import de.fraunhofer.iais.spatial.util.DBUtil;

public class UpdateFlickrRegionId {
	/**
	* Logger for this class
	*/
	private static final Logger logger = LoggerFactory.getLogger(UpdateFlickrRegionId.class);

	final static int SELECT_BATCH_SIZE = 400;
	final static String PHOTOS_TABLE_NAME = "FLICKR_EUROPE";
	static Calendar startDate;

	static DBUtil db = new DBUtil("/jdbc_pg.properties", 18, 3);

	public static void main(String[] args) throws SQLException {
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
		PreparedStatement countStmt = db.getPstmt(conn, "select count(*) num from " + PHOTOS_TABLE_NAME);
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
				double longitude = rs.getLong("longitude");
				double latitude = rs.getLong("latitude");

				System.out.println("totalNum/updatedNum: " + totalNum + "/" + updatedNum++);
				PreparedStatement selectAreaIdPstmt = db.getPstmt(conn, "select id, radius from FLICKR_EUROPE_AREA t where ST_Intersects(ST_GeomFromEWKT('SRID=4326;POINT(" + longitude + " " + latitude + ")'), t.geom::geometry)");
//				PreparedStatement selectAreaIdPstmt = db.getPstmt(conn, "select region_id as id, radius from " + PHOTOS_TABLE_NAME + "_REGION_ID where photo_id = ?");
//				selectAreaIdPstmt.setLong(1, photoId);
				ResultSet areaIdRs = db.getRs(selectAreaIdPstmt);
				List<String> paraStrs = new ArrayList<String>();
				while (areaIdRs.next()) {
					int areaId = areaIdRs.getInt("id");
					String radius = areaIdRs.getString("radius");
					paraStrs.add(" region_" + radius + "_id = " + areaId);
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
