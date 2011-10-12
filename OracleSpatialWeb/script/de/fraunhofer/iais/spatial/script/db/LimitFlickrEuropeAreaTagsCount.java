package de.fraunhofer.iais.spatial.script.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fraunhofer.iais.spatial.dao.FlickrAreaDao;
import de.fraunhofer.iais.spatial.util.DBUtil;

public class LimitFlickrEuropeAreaTagsCount {
	/**
	* Logger for this class
	*/
	private static final Logger logger = LoggerFactory.getLogger(LimitFlickrEuropeAreaTagsCount.class);

	final static int BATCH_SIZE = 1;
	final static String INPUT_COUNTS_TABLE_NAME = "flickr_europe_topviewed_1m_tags_count";
	final static String OUTPUT_COUNTS_TABLE_NAME = "flickr_europe_topviewed_1m_tags_count_20";
	static int rownum = 1;
	static Calendar startDate;

	static DBUtil db = new DBUtil("/jdbc_pg.properties", 18, 3);

	public static void main(String[] args) throws SQLException {
		long start = System.currentTimeMillis();
		startDate = Calendar.getInstance();
		startDate.setTimeInMillis(start);

		LimitFlickrEuropeAreaTagsCount t = new LimitFlickrEuropeAreaTagsCount();
		t.limit();

		long end = System.currentTimeMillis();
		Calendar endDate = Calendar.getInstance();
		endDate.setTimeInMillis(end);

		logger.info("start time:" + startDate.getTime()); //$NON-NLS-1$
		logger.info("end time:" + endDate.getTime()); //$NON-NLS-1$
		logger.info("main(String[]) - escaped time:" + (System.currentTimeMillis() - start) / 1000.0); //$NON-NLS-1$
	}


	private void limit() throws SQLException {
		Connection conn = db.getConn();
		Connection updateConn = db.getConn();
		conn.setAutoCommit(false);
		PreparedStatement stmt = db.getPstmt(conn, "select id, radius, hour from " + INPUT_COUNTS_TABLE_NAME);
		stmt.setFetchSize(BATCH_SIZE);
		ResultSet pset = db.getRs(stmt);

		while (pset.next()) {
			int id = pset.getInt("id");
			String radius = pset.getString("radius");
			String hourStr = pset.getString("hour");

			if (hourStr != null) {
				SortedMap<String, Map<String, Integer>> hoursTagsCount = new TreeMap<String, Map<String, Integer>>();

				FlickrAreaDao.parseHoursTagsCountDbString(hourStr, hoursTagsCount);
				String countStr = FlickrAreaDao.createDatesTagsCountDbString(hoursTagsCount, 30);

				PreparedStatement updateStmt = db.getPstmt(updateConn, "update " + OUTPUT_COUNTS_TABLE_NAME + " set hour = ? where id = ? and radius = ?");
				updateStmt.setString(1, countStr);
				updateStmt.setInt(2, id);
				updateStmt.setInt(3, Integer.parseInt(radius));

				System.out.println("countStr:" + countStr);
				System.out.println("id:" + id);
				System.out.println("radius:" + radius);
				System.out.println("executeUpdate:" + updateStmt.executeUpdate());
				db.close(updateStmt);
			}
		}
		db.close(pset);
		db.close(stmt);
	}

}
