package de.fraunhofer.iais.spatial.script.db;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fraunhofer.iais.spatial.dao.FlickrEuropeAreaDao;
import de.fraunhofer.iais.spatial.dto.FlickrEuropeAreaDto.Level;
import de.fraunhofer.iais.spatial.util.DBUtil;

public class JoinFlickrEuropeAreaCount {
	/**
	* Logger for this class
	*/
	private static final Logger logger = LoggerFactory.getLogger(JoinFlickrEuropeAreaCount.class);

//	final static int BATCH_SIZE = 1000000;
	final static int BEGIN_REGION_CHECKED_CODE = 1;
	final static int FINISH_REGION_CHECKED_CODE = 2;
	final static String PHOTOS_TABLE_NAME = "FLICKR_EUROPE";
	final static String COUNTS_TABLE_NAME = "FLICKR_EUROPE_COUNT";
	static int rownum = 1;
	static Calendar startDate;

	static DBUtil db = new DBUtil("/jdbc_pg.properties", 18, 3);

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
			conn.setAutoCommit(false);


			int updateSize = 0;
//			do {
//				// REGION_CHECKED = -1 : building the index
//				/* Oracle */
//				PreparedStatement updateStmt1 = db.getPstmt(conn, "" +
//						"update " + PHOTOS_TABLE_NAME + " t set t.REGION_CHECKED = -1 where t.photo_id in (" +
//							"select t2.photo_id from (" +
//								"select t1.photo_id, ROWNUM rn from (" +
//									"select photo_id from " + PHOTOS_TABLE_NAME + " where region_checked = ?" +
//								") t1 where ROWNUM < ? " +
//							") t2 where rn >= ?" +
//						")");
//				updateStmt1.setInt(1, BEGIN_REGION_CHECKED_CODE);
//				updateStmt1.setInt(2, 1 + BATCH_SIZE);
//				updateStmt1.setInt(3, 1);
//
//				/* PostGIS
//				PreparedStatement updateStmt1 = db.getPstmt(conn, "" +
//						"update " + PHOTOS_TABLE_NAME + " set REGION_CHECKED = -1 where photo_id in (" +
//							"select photo_id from " + PHOTOS_TABLE_NAME + " t where t.region_checked = ? " +
//							"limit ? )");
//				updateStmt1.setInt(1, BEGIN_REGION_CHECKED_CODE);
//				updateStmt1.setInt(2, BATCH_SIZE);
//				*/
//				updateSize = updateStmt1.executeUpdate();
//				rownum += updateSize;
//
//				db.close(updateStmt1);
//				conn.commit();

				for (String radius : radiusList) {
					count(conn, Level.HOUR, radius);
//					conn.commit();
//					count(conn, Level.DAY, radius);
//					conn.commit();
//					count(conn, Level.MONTH, radius);
//					conn.commit();
//					count(conn, Level.YEAR, radius);
//					conn.commit();
				}

				// REGION_CHECKED = 2 : already indexed
//				PreparedStatement updateStmt2 = db.getPstmt(conn, "update " + PHOTOS_TABLE_NAME + " set REGION_CHECKED = ? where REGION_CHECKED = -1");
//				updateStmt2.setInt(1, FINISH_REGION_CHECKED_CODE);
//				updateStmt2.executeUpdate();
//				db.close(updateStmt2);
//				conn.commit();
//
//			} while(updateSize == BATCH_SIZE);
			countDay(conn);
			countMonth(conn);
			countYear(conn);
			countTotal(conn);
			conn.commit();
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
		PreparedStatement selectAreaStmt = db.getPstmt(conn, "select distinct(t.region_" + radiusString + "_id) id from " + PHOTOS_TABLE_NAME + " t where t.REGION_CHECKED = 1");
		ResultSet selectAreaRs = db.getRs(selectAreaStmt);


		PreparedStatement selectFlickrStmt = db.getPstmt(conn,
				"select date_str, count(*) as num from ("
				+ " select t.photo_id, to_char(t.TAKEN_DATE, ?) as date_str"
				+ " from " + PHOTOS_TABLE_NAME + " t where t.region_" + radiusString + "_id = ? and t.REGION_CHECKED = 1) temp "
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

	private void addToIndex(Connection conn, Map<String, Integer> countsMapAdd, Level queryLevel, int id, String radius) throws SQLException {
		PreparedStatement selectCountStmt = db.getPstmt(conn, "select " + queryLevel + " as countStr from " + COUNTS_TABLE_NAME + " where id = ? and radius = ?");
		selectCountStmt.setInt(1, id);
		selectCountStmt.setInt(2, Integer.parseInt(radius));
		ResultSet rs = db.getRs(selectCountStmt);
		Map<String, Integer> countsMapPre = new TreeMap<String, Integer>();

		if (rs.next()) {
			String countStr = rs.getString("countStr");

			if (countStr != null) {
				FlickrEuropeAreaDao.parseCountDbString(countStr, countsMapPre, FlickrEuropeAreaDao.judgeDbDateCountRegExPattern(queryLevel));
			}
		}

		db.close(rs);
		db.close(selectCountStmt);

		// add countsMap and countsMap2 together
		for (Entry<String, Integer> e : countsMapAdd.entrySet()) {
			if (countsMapPre.containsKey(e.getKey())) {
				countsMapPre.put(e.getKey(), e.getValue() + countsMapPre.get(e.getKey()));
			} else {
				countsMapPre.put(e.getKey(), e.getValue());
			}
		}

		String countStr = FlickrEuropeAreaDao.createCountDbString(countsMapPre);


		PreparedStatement updateStmt = db.getPstmt(conn, "update " + COUNTS_TABLE_NAME + " set " + queryLevel.toString() + " = ? where id = ? and radius = ?");
		updateStmt.setString(1, countStr);
		updateStmt.setInt(2, id);
		updateStmt.setInt(3, Integer.parseInt(radius));


		System.out.println("countStr:" + countStr);
		System.out.println("id:" + id);
		System.out.println("radius:" + radius);
		System.out.println("executeUpdate:" + updateStmt.executeUpdate());

		db.close(updateStmt);
	}

	private void countDay(Connection conn) throws SQLException {

		PreparedStatement personStmt = db.getPstmt(conn, "select id, radius, hour from " + COUNTS_TABLE_NAME);
		ResultSet pset = db.getRs(personStmt);

		while (pset.next()) {
			int id = pset.getInt("id");
			String radius = pset.getString("radius");
			String hourStr = pset.getString("hour");
			StringBuffer dayStr = new StringBuffer();
			if (hourStr != null) {
				//person:28507282@N03
				Pattern p = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})@\\d{2}:(\\d{1,});");
				Matcher m = p.matcher(hourStr);
				String day = "";
				int hour = 0;
				while (m.find()) {
					if (m.group(1).equals(day)) {
						hour += Integer.parseInt(m.group(2));
					} else {
						if (!day.equals("")) {
							dayStr.append(day + ":" + hour + ";");
						}
						day = m.group(1);
						hour = Integer.parseInt(m.group(2));
					}
				}
				if (!day.equals("")) {
					dayStr.append(day + ":" + hour + ";");
				}
				PreparedStatement iStmt = db.getPstmt(conn, "update " + COUNTS_TABLE_NAME + " set day = ? where id = ? and radius = ?");
				iStmt.setString(1, dayStr.toString());
				iStmt.setInt(2, id);
				iStmt.setInt(3, Integer.parseInt(radius));
				iStmt.executeUpdate();
				iStmt.close();
				System.out.println("id:" + id + "!" + dayStr);
			}
		}
		db.close(pset);
		db.close(personStmt);
	}

	private void countMonth(Connection conn) throws SQLException {

		PreparedStatement personStmt = db.getPstmt(conn, "select id, radius, hour from " + COUNTS_TABLE_NAME);
		ResultSet pset = db.getRs(personStmt);

		while (pset.next()) {
			int id = pset.getInt("id");
			String hourStr = pset.getString("hour");
			String radius = pset.getString("radius");
			StringBuffer monthStr = new StringBuffer();
			if (hourStr != null) {
				//person:28507282@N03
				Pattern p = Pattern.compile("(\\d{4}-\\d{2})-\\d{2}@\\d{2}:(\\d{1,});");
				Matcher m = p.matcher(hourStr);
				String month = "";
				int hour = 0;
				while (m.find()) {
					if (m.group(1).equals(month)) {
						hour += Integer.parseInt(m.group(2));
					} else {
						if (!month.equals("")) {
							monthStr.append(month + ":" + hour + ";");
						}
						month = m.group(1);
						hour = Integer.parseInt(m.group(2));
					}
				}
				if (!month.equals("")) {
					monthStr.append(month + ":" + hour + ";");
				}
				PreparedStatement iStmt = db.getPstmt(conn, "update " + COUNTS_TABLE_NAME + " set month = ? where id = ? and radius = ?");
				iStmt.setString(1, monthStr.toString());
				iStmt.setInt(2, id);
				iStmt.setInt(3, Integer.parseInt(radius));
				iStmt.executeUpdate();
				iStmt.close();
				System.out.println("id:" + id + "!" + monthStr);
			}
		}
		db.close(pset);
		db.close(personStmt);
	}

	private void countYear(Connection conn) throws SQLException {

		PreparedStatement personStmt = db.getPstmt(conn, "select id, radius, hour from " + COUNTS_TABLE_NAME);
		ResultSet pset = db.getRs(personStmt);

		while (pset.next()) {
			int id = pset.getInt("id");
			String hourStr = pset.getString("hour");
			String radius = pset.getString("radius");
			StringBuffer yearStr = new StringBuffer();
			if (hourStr != null) {
				//person:28507282@N03
				Pattern p = Pattern.compile("(\\d{4})-\\d{2}-\\d{2}@\\d{2}:(\\d{1,});");
				Matcher m = p.matcher(hourStr);
				String year = "";
				int hour = 0;
				while (m.find()) {
					if (m.group(1).equals(year)) {
						hour += Integer.parseInt(m.group(2));
					} else {
						if (!year.equals("")) {
							yearStr.append(year + ":" + hour + ";");
						}
						year = m.group(1);
						hour = Integer.parseInt(m.group(2));
					}
				}
				if (!year.equals("")) {
					yearStr.append(year + ":" + hour + ";");
				}
				PreparedStatement iStmt = db.getPstmt(conn, "update " + COUNTS_TABLE_NAME + " set year = ? where id = ? and radius = ?");
				iStmt.setString(1, yearStr.toString());
				iStmt.setInt(2, id);
				iStmt.setInt(3, Integer.parseInt(radius));
				iStmt.executeUpdate();
				iStmt.close();
				System.out.println("id:" + id + "!" + yearStr);
			}
		}
		db.close(pset);
		db.close(personStmt);
	}

	private void countTotal(Connection conn) throws SQLException {

		PreparedStatement pstmt = db.getPstmt(conn, "select id, radius, hour from " + COUNTS_TABLE_NAME + " t");
		ResultSet pset = db.getRs(pstmt);
		PreparedStatement updateStmt = db.getPstmt(conn, "update " + COUNTS_TABLE_NAME + " set total = ? where id = ? and radius = ?");
		int numUpdate = 0;
		while (pset.next()) {
			int id = pset.getInt("id");
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
				updateStmt.setInt(2, id);
				updateStmt.setInt(3, Integer.parseInt(radius));
//				updateStmt.executeUpdate();
				updateStmt.addBatch();
				if((numUpdate++) % 200 == 0){
					updateStmt.executeBatch();
				}
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
