package de.fraunhofer.iais.spatial.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
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

	final static String PHOTOS_TABLE_NAME = "sample_flickr_europe_photo";
	final static String COUNTS_TABLE_NAME = "sample_flickr_europe_count2";

	DBUtil db = new DBUtil();

	public static void main(String[] args) {
		long start = System.currentTimeMillis();

		JoinFlickrEuropeAreaCount t = new JoinFlickrEuropeAreaCount();
		t.begin();

		logger.debug("main(String[]) - escaped time:" + (System.currentTimeMillis() - start) / 1000.0); //$NON-NLS-1$
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

			// REGION_CHECKED = -1 : building the index
			PreparedStatement updateStmt1 = db.getPstmt(conn, "update " + PHOTOS_TABLE_NAME + " t set t.REGION_CHECKED = -1 where t.REGION_CHECKED = 1");
			updateStmt1.executeUpdate();
			db.close(updateStmt1);

			for (String radius : radiusList) {
				count(conn, Level.HOUR, radius);
				count(conn, Level.DAY, radius);
				count(conn, Level.MONTH, radius);
				count(conn, Level.YEAR, radius);
//				countPeople(conn, radius);
			}
			countTotal(conn);

			// REGION_CHECKED = 2 : already indexed
			PreparedStatement updateStmt2 = db.getPstmt(conn, "update " + PHOTOS_TABLE_NAME + " t set t.REGION_CHECKED = 2 where t.REGION_CHECKED = -1");
			updateStmt2.executeUpdate();
			db.close(updateStmt2);

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
		PreparedStatement selectAreaStmt = db.getPstmt(conn, "select distinct(t.region_" + radiusString + "_id) id from " + PHOTOS_TABLE_NAME + " t where t.REGION_CHECKED = -1");
		ResultSet selectAreaRs = db.getRs(selectAreaStmt);
		int areaId = -1;
		while (selectAreaRs.next()) {
			areaId = selectAreaRs.getInt("id");

			PreparedStatement selectFlickrStmt = db.getPstmt(conn,
					"select date_str, count(*) as num from ("
					+ " select t.photo_id, to_char(t.dt,?) as date_str"
					+ " from " + PHOTOS_TABLE_NAME + " t where t.region_" + radiusString + "_id = ?)"
					+ "group by date_str");

			selectFlickrStmt.setString(1, FlickrEuropeAreaDao.judgeOracleDatePatternStr(queryLevel));
			selectFlickrStmt.setInt(2, areaId);

			ResultSet selectFlickrRs = db.getRs(selectFlickrStmt);

			Map<String, Integer> countsMap = new TreeMap<String, Integer>();

			while (selectFlickrRs.next()) {
				countsMap.put(selectFlickrRs.getString("date_str"), selectFlickrRs.getInt("num"));
			}


			addToIndex(conn, countsMap, COUNTS_TABLE_NAME, queryLevel, areaId, radiusString);

			db.close(selectFlickrRs);
			db.close(selectFlickrStmt);
		}

		db.close(selectAreaRs);
		db.close(selectAreaStmt);
	}

	/**
	 * not incremental
	 * @param conn
	 * @param radiusString
	 * @throws SQLException
	 */
	private void countPeople(Connection conn, String radiusString) throws SQLException {
		PreparedStatement selectAreaStmt = db.getPstmt(conn, "select distinct(p.region_" + radiusString + "_id) id from " + PHOTOS_TABLE_NAME + " t where t.REGION_CHECKED = -1");
		ResultSet selectAreaRs = db.getRs(selectAreaStmt);
		int areaId = -1;
		while (selectAreaRs.next()) {
			StringBuffer count = new StringBuffer();
			areaId = selectAreaRs.getInt("id");

			PreparedStatement selectFlickrStmt = db.getPstmt(conn, "select p.person person, count(*) num  from " + PHOTOS_TABLE_NAME + " p"
					+ " where p.region_" + radiusString	+ "_id = ? " + " group by p.person");
			selectFlickrStmt.setInt(1, areaId);

			ResultSet selectFlickrRs = db.getRs(selectFlickrStmt);

			while (selectFlickrRs.next()) {
				String person = selectFlickrRs.getString("person");
				int num = selectFlickrRs.getInt("num");
				String info = person + ":" + num + ";";
				if (num != 0) {
					count.append(info);
				}
			}
			if (logger.isDebugEnabled()) {
				logger.debug("countPeople(Connection, String) - " + count); //$NON-NLS-1$
			}
			update(conn, count.toString(), COUNTS_TABLE_NAME, "PERSON", areaId, radiusString);

			db.close(selectFlickrRs);
			db.close(selectFlickrStmt);
		}

		db.close(selectAreaRs);
		db.close(selectAreaStmt);
	}

	private void countTotal(Connection conn) throws SQLException {

		PreparedStatement personStmt = db.getPstmt(conn, "select * from " + COUNTS_TABLE_NAME + " t where t.REGION_CHECKED = -1");
		ResultSet pset = db.getRs(personStmt);

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

				PreparedStatement iStmt = db.getPstmt(conn, "update " + COUNTS_TABLE_NAME + " set total = ? where id = ? and radius = ?");
				iStmt.setInt(1, hour);
				iStmt.setString(2, id);
				iStmt.setString(3, radius);
				iStmt.executeUpdate();
				db.close(iStmt);
			}
		}
		db.close(pset);
		db.close(personStmt);
	}

	private void insert(Connection conn, String countStr, String table, String column, int id, String radius) throws SQLException {
		PreparedStatement stmt = db.getPstmt(conn, "insert into " + table + " (id, radius, " + column + ") values (?, ?, ?)");
		stmt.setInt(1, id);
		stmt.setString(2, radius);
		stmt.setString(3, countStr);
		stmt.executeUpdate();
		db.close(stmt);
	}

	private void addToIndex(Connection conn, Map<String, Integer> countsMap, String table, Level queryLevel, int id, String radius) throws SQLException {
		PreparedStatement stmt = db.getPstmt(conn, "select " + queryLevel + " as countStr from " + table + " where id = ? and radius = ?");
		stmt.setInt(1, id);
		stmt.setString(2, radius);
		ResultSet rs = db.getRs(stmt);
		Map<String, Integer> countsMap2 = new TreeMap<String, Integer>();

		if(rs.next()){
			String countStr = rs.getString("countStr");
			if(countStr != null){
				FlickrEuropeAreaDao.parseCounts(countStr, countsMap2, FlickrEuropeAreaDao.judgeOracleRegExPattern(queryLevel));
			}
		}

		// add countsMap and countsMap2 together
		for (Entry<String, Integer> e : countsMap.entrySet()) {
			if(countsMap2.containsKey(e.getKey())){
				int value1 = countsMap.get(e.getKey());
				int value2 = e.getValue();
				countsMap2.put(e.getKey(), value1 + value2);
			}else {
				countsMap2.put(e.getKey(),  e.getValue());
			}
		}

		String countStr = FlickrEuropeAreaDao.createCountsDbString(countsMap2);
		update(conn, countStr, table, queryLevel.toString(), id, radius);
	}

	private void update(Connection conn, String countStr, String table, String column, int id, String radius) throws SQLException {

		PreparedStatement stmt = db.getPstmt(conn, "update " + table + " set " + column + " = ? where id = ? and radius = ?");
		stmt.setString(1, countStr);
		stmt.setInt(2, id);
		stmt.setString(3, radius);

		stmt.executeUpdate();
		db.close(stmt);
	}

}
