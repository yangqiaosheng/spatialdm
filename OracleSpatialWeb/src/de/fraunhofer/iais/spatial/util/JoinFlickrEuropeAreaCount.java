package de.fraunhofer.iais.spatial.util;

import oracle.jdbc.OraclePreparedStatement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.fraunhofer.iais.spatial.dao.FlickrEuropeAreaDao;
import de.fraunhofer.iais.spatial.dto.FlickrEuropeAreaDto.Level;

public class JoinFlickrEuropeAreaCount {
	/**
	* Logger for this class
	*/
	private static final Logger logger = LoggerFactory.getLogger(JoinFlickrEuropeAreaCount.class);

	final static int BATCH_SIZE = 1000000;
	final static String PHOTOS_TABLE_NAME = "FLICKR_EUROPE";
	final static String COUNTS_TABLE_NAME = "FLICKR_EUROPE_COUNT";
	static int rownum = 1;
	static Calendar startDate;

	static DBUtil db = new DBUtil("/jdbc.properties", 3, 1);

	public static void main(String[] args) {
		long start = System.currentTimeMillis();
		startDate = Calendar.getInstance();
		startDate.setTimeInMillis(start);

		JoinFlickrEuropeAreaCount t = new JoinFlickrEuropeAreaCount();
		t.begin();

		long end = System.currentTimeMillis();
		Calendar endDate = Calendar.getInstance();
		endDate.setTimeInMillis(end);

		logger.info("start time:" + startDate.getTime()); //$NON-NLS-1$
		logger.info("end time:" + endDate.getTime()); //$NON-NLS-1$
		logger.info("main(String[]) - escaped time:" + (System.currentTimeMillis() - start) / 1000.0); //$NON-NLS-1$
	}

	public void begin(){

		ArrayList<String> radiusList = new ArrayList<String>();
		radiusList.add("5000");
		radiusList.add("10000");
		radiusList.add("20000");
		radiusList.add("40000");
		radiusList.add("80000");
		radiusList.add("160000");
		radiusList.add("320000");

		Connection conn = db.getConn();
		try {
//			conn.setAutoCommit(false);


			int updateSize = 0;
			do {
				// REGION_CHECKED = -1 : building the index
				PreparedStatement updateStmt1 = db.getPstmt(conn, "" +
						"update " + PHOTOS_TABLE_NAME + " t set t.REGION_CHECKED = -1 where t.photo_id in (" +
							"select t2.photo_id from (" +
								"select t1.photo_id, ROWNUM rn from (" +
									"select photo_id from flickr_europe where region_checked = 1" +
								") t1 where ROWNUM < ? " +
							") t2 where rn >= ?" +
						")");
				updateStmt1.setInt(1, 1 + BATCH_SIZE);
				updateStmt1.setInt(2, 1);
				updateSize = updateStmt1.executeUpdate();
				rownum += updateSize;

				db.close(updateStmt1);
				conn.commit();

				for (String radius : radiusList) {
					count(conn, Level.HOUR, radius);
					conn.commit();
					count(conn, Level.DAY, radius);
					conn.commit();
					count(conn, Level.MONTH, radius);
					conn.commit();
					count(conn, Level.YEAR, radius);
					conn.commit();
				}
				countTotal(conn);
				conn.commit();

				// REGION_CHECKED = 2 : already indexed
				PreparedStatement updateStmt2 = db.getPstmt(conn, "update " + PHOTOS_TABLE_NAME + " t set t.REGION_CHECKED = 2 where t.REGION_CHECKED = -1");
				updateStmt2.executeUpdate();
				db.close(updateStmt2);

				conn.commit();

			} while(updateSize == BATCH_SIZE);

		} catch (SQLException e) {
			logger.error("begin()", e); //$NON-NLS-1$
			try {
				conn.rollback();
			} catch (SQLException e1) {
				logger.error("roolback()", e); //$NON-NLS-1$
			}
		} finally {
			db.close(conn);
		}
	}

	private void count(Connection conn, Level queryLevel, String radiusString) throws SQLException {
		PreparedStatement selectAreaStmt = db.getPstmt(conn, "select distinct(t.region_" + radiusString + "_id) id from " + PHOTOS_TABLE_NAME + " t where t.REGION_CHECKED = -1");
		ResultSet selectAreaRs = db.getRs(selectAreaStmt);


		PreparedStatement selectFlickrStmt = db.getPstmt(conn,
				"select date_str, count(*) as num from ("
				+ " select t.photo_id, to_char(t.TAKEN_DATE,?) as date_str"
				+ " from " + PHOTOS_TABLE_NAME + " t where t.region_" + radiusString + "_id = ? and t.REGION_CHECKED = -1)"
				+ "group by date_str");

		int areaId = -1;
		int checkedSize = 0;
		while (selectAreaRs.next()) {
			areaId = selectAreaRs.getInt("id");


			selectFlickrStmt.setString(1, FlickrEuropeAreaDao.judgeDbDateCountPatternStr(queryLevel));
			selectFlickrStmt.setInt(2, areaId);

			ResultSet selectFlickrRs = db.getRs(selectFlickrStmt);

			Map<String, Integer> countsMap = new TreeMap<String, Integer>();

			while (selectFlickrRs.next()) {
				countsMap.put(selectFlickrRs.getString("date_str"), selectFlickrRs.getInt("num"));
			}

			addToIndex(conn, countsMap, queryLevel, areaId, radiusString);

			db.close(selectFlickrRs);

			logger.debug("radius:" + radiusString + "|level:" + queryLevel + "|already checked:" + checkedSize++);
			logger.debug("rownum to:" + rownum);
			logger.debug("start time:" + startDate.getTime());
		}

		logger.info("radius:" + radiusString + "|level:" + queryLevel + "|already checked:" + checkedSize);
		logger.info("rownum to:" + rownum);
		logger.info("start time:" + startDate.getTime());

		db.close(selectFlickrStmt);
		db.close(selectAreaRs);
		db.close(selectAreaStmt);
	}

	private void addToIndex(Connection conn, Map<String, Integer> countsMap, Level queryLevel, int id, String radius) throws SQLException {
		PreparedStatement selectCountStmt = db.getPstmt(conn, "select " + queryLevel + " as countStr from " + COUNTS_TABLE_NAME + " where id = ? and radius = ?");
		selectCountStmt.setInt(1, id);
		selectCountStmt.setString(2, radius);
		ResultSet rs = db.getRs(selectCountStmt);
		Map<String, Integer> countsMap2 = new TreeMap<String, Integer>();

		if (rs.next()) {
			String countStr = rs.getString("countStr");

			if (countStr != null) {
				FlickrEuropeAreaDao.parseCounts(countStr, countsMap2, FlickrEuropeAreaDao.judgeDbDateCountRegExPattern(queryLevel));
			}
		}

		db.close(rs);
		db.close(selectCountStmt);

		// add countsMap and countsMap2 together
		for (Entry<String, Integer> e : countsMap.entrySet()) {
			if (countsMap2.containsKey(e.getKey())) {
				int value1 = countsMap.get(e.getKey());
				int value2 = e.getValue();
				countsMap2.put(e.getKey(), value1 + value2);
			} else {
				countsMap2.put(e.getKey(), e.getValue());
			}
		}

		String countStr = FlickrEuropeAreaDao.createCountsDbString(countsMap2);


		PreparedStatement updateStmt = db.getPstmt(conn, "update " + COUNTS_TABLE_NAME + " set " + queryLevel.toString() + " = ? where id = ? and radius = ?");
		updateStmt.setString(1, countStr);
		updateStmt.setInt(2, id);
		updateStmt.setString(3, radius);


		System.out.println("countStr:" + countStr);
		System.out.println("id:" + id);
		System.out.println("radius:" + radius);
		System.out.println("executeUpdate:" + updateStmt.executeUpdate());

		db.close(updateStmt);
	}

	private void countTotal(Connection conn) throws SQLException {

		PreparedStatement pstmt = db.getPstmt(conn, "select id, radius, hour from " + COUNTS_TABLE_NAME + " t");
		ResultSet pset = db.getRs(pstmt);
		PreparedStatement updateStmt = db.getPstmt(conn, "update " + COUNTS_TABLE_NAME + " set total = ? where id = ? and radius = ?");
		while (pset.next()) {
			String id = pset.getString("id");
			String hourStr = pset.getString("hour");
			String radius = pset.getString("radius");
			if (hourStr != null) {
				//person:28507282@N03
				Pattern p = Pattern.compile("\\d{4}-\\d{2}-\\d{2}@\\d{2}:(\\d{1,});");
				Matcher m = p.matcher(hourStr);
				int hour = 0;
				while (m.find()) {
					hour += Integer.parseInt(m.group(1));
				}

				updateStmt.setInt(1, hour);
				updateStmt.setString(2, id);
				updateStmt.setString(3, radius);
				updateStmt.addBatch();
			}
		}
		updateStmt.executeBatch();
		db.close(updateStmt);
		db.close(pset);
		db.close(pstmt);
	}

	/*
	private void insert(Connection conn, String countStr, String table, String column, int id, String radius) throws SQLException {
		PreparedStatement stmt = db.getPstmt(conn, "insert into " + table + " (id, radius, " + column + ") values (?, ?, ?)");
		stmt.setInt(1, id);
		stmt.setString(2, radius);
		stmt.setString(3, countStr);
		stmt.executeUpdate();
		db.close(stmt);
	}*/

}
