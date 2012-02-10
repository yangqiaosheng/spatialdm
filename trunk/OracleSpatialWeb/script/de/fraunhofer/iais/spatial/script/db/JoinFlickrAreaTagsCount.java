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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fraunhofer.iais.spatial.dao.FlickrAreaDao;
import de.fraunhofer.iais.spatial.dto.FlickrAreaDto.Level;
import de.fraunhofer.iais.spatial.util.DBUtil;

public class JoinFlickrAreaTagsCount {

	/**
	* Logger for this class
	*/
	private static final Logger logger = LoggerFactory.getLogger(JoinFlickrAreaTagsCount.class);

	final static int FETCH_SIZE = 1;
	static int TAG_LIMIT = -1;
	static String PHOTO_TABLE_NAME = "flickr_world_100000";
	static String COUNTS_TABLE_NAME = "flickr_world_100000_tags_count";
	static String STOPWORD_TABLE_NAME = "flickr_world_topviewed_5m_tags_stopword";
	static int rownum = 1;
	static Calendar startDate;

	static DBUtil db = new DBUtil("/jdbc.properties", 18, 3);

//	static DBUtil db = new DBUtil("/jdbc_pg.properties", 18, 3);

	public static void main(String[] args) throws IOException {
		System.out.println("\nPlease input the Count TableName:\n[Default: " + COUNTS_TABLE_NAME + "]");
		String countTableName = new BufferedReader(new InputStreamReader(System.in)).readLine();
		if (StringUtils.isNotEmpty(countTableName)) {
			COUNTS_TABLE_NAME = countTableName;
		}
		System.out.println("Count Table:" + COUNTS_TABLE_NAME);

		System.out.println("\nPlease input the Photo TableName:\n[Default: " + PHOTO_TABLE_NAME + "]");
		String photoTableName = new BufferedReader(new InputStreamReader(System.in)).readLine();
		if (StringUtils.isNotEmpty(photoTableName)) {
			PHOTO_TABLE_NAME = photoTableName;
		}
		System.out.println("Photo Table:" + PHOTO_TABLE_NAME);

		System.out.println("\nPlease input the Tag limit:\n[Default: " + TAG_LIMIT + "]");
		String tagLimit = new BufferedReader(new InputStreamReader(System.in)).readLine();
		if (StringUtils.isNotEmpty(tagLimit)) {
			TAG_LIMIT = NumberUtils.toInt(tagLimit);
		}
		System.out.println("Tag Limit:" + TAG_LIMIT);

		long start = System.currentTimeMillis();
		startDate = Calendar.getInstance();
		startDate.setTimeInMillis(start);

		JoinFlickrAreaTagsCount t = new JoinFlickrAreaTagsCount();
		t.begin();

		long end = System.currentTimeMillis();
		Calendar endDate = Calendar.getInstance();
		endDate.setTimeInMillis(end);

		logger.info("start time:" + startDate.getTime()); //$NON-NLS-1$
		logger.info("end time:" + endDate.getTime()); //$NON-NLS-1$
		logger.info("main(String[]) - escaped time:" + (System.currentTimeMillis() - start) / 1000.0); //$NON-NLS-1$
	}

	public void begin() {

		ArrayList<String> radiusList = new ArrayList<String>();

		if (PHOTO_TABLE_NAME.contains("world")) {
			System.out.println("For the World");
			radiusList.add("625");
			radiusList.add("1250");
			radiusList.add("2500");
			radiusList.add("5000");
			radiusList.add("10000");
			radiusList.add("20000");
			radiusList.add("40000");
			radiusList.add("80000");
			radiusList.add("160000");
			radiusList.add("320000");
			radiusList.add("640000");
			radiusList.add("1280000");
			radiusList.add("2560000");
		} else if (PHOTO_TABLE_NAME.contains("europe")) {
			System.out.println("For the Europe");
			radiusList.add("375");
			radiusList.add("750");
			radiusList.add("1250");
			radiusList.add("2500");
			radiusList.add("5000");
			radiusList.add("10000");
			radiusList.add("20000");
			radiusList.add("40000");
			radiusList.add("80000");
			radiusList.add("160000");
			radiusList.add("320000");
		} else {
			System.out.println("Wrong!");
			return;
		}

		Connection conn = db.getConn();
		Connection selectConn = db.getConn();
		try {
			conn.setAutoCommit(false);
			selectConn.setAutoCommit(false);

			for (String radius : radiusList) {
				countHoursTags(conn, radius);
			}

//			joinPartition();
//			countDay(selectConn);
//			countMonth(selectConn);
//			countYear(selectConn);
//			countTotal(selectConn);
//			countTotalSum(selectConn);
		} catch (SQLException e) {
			logger.error("begin()", e); //$NON-NLS-1$
			try {
				conn.rollback();
			} catch (SQLException e1) {
				logger.error("roolback()", e); //$NON-NLS-1$
			}
		} finally {
			db.close(conn);
			db.close(selectConn);
		}
	}

	private void joinPartition() throws SQLException {
		Connection conn = db.getConn();
		Connection updateConn = db.getConn();
		conn.setAutoCommit(false);

		int tableNum = 5;
		for (int i = 1; i <= tableNum; i++) {

			PreparedStatement stmt = db.getPstmt(conn, "select id, radius, hour from " + COUNTS_TABLE_NAME);
			ResultSet pset = db.getRs(stmt);


			while (pset.next()) {
				int id = pset.getInt("id");
				String radius = pset.getString("radius");
				PreparedStatement hourStmt = db.getPstmt(conn, "select hour from " + COUNTS_TABLE_NAME + i + " where id = ? and radius = ?");
				hourStmt.setFetchSize(FETCH_SIZE);
				hourStmt.setInt(1, id);
				hourStmt.setInt(2, Integer.parseInt(radius));

				ResultSet hourRs = db.getRs(hourStmt);
				SortedMap<String, Map<String, Integer>> hoursTotalTagsCount = new TreeMap<String, Map<String, Integer>>();
				while (hourRs.next()) {
					String hourStr = hourRs.getString("hour");
					if (hourStr != null) {
						SortedMap<String, Map<String, Integer>> hoursTagsCount = new TreeMap<String, Map<String, Integer>>();
						FlickrAreaDao.parseHoursTagsCountDbString(hourStr, hoursTagsCount);
						mergeTagsCountsMap(hoursTagsCount, hoursTotalTagsCount);
					}
					db.close(hourRs);
					db.close(hourStmt);
				}

				String countStr = FlickrAreaDao.createDatesTagsCountDbString(hoursTotalTagsCount, -1);
				PreparedStatement updateStmt = db.getPstmt(updateConn, "update " + COUNTS_TABLE_NAME + " set hour = ? where id = ? and radius = ?");
				updateStmt.setString(1, countStr);
				updateStmt.setInt(2, id);
				updateStmt.setInt(3, Integer.parseInt(radius));

				System.out.println("countStr:" + StringUtils.substring(countStr, 0, 200));
				System.out.println("id:" + id);
				System.out.println("radius:" + radius);
				System.out.println("executeUpdate:" + updateStmt.executeUpdate());
				db.close(updateStmt);
			}
			db.close(pset);
			db.close(stmt);

		}
		db.close(conn);
		db.close(updateConn);
	}

	private void countHoursTags(Connection conn, String radiusString) throws SQLException {
//		PreparedStatement selectAreaStmt = db.getPstmt(conn, "select distinct(t.region_" + radiusString + "_id) id from " + PHOTO_TABLE_NAME + " t");
		PreparedStatement selectAreaStmt = db.getPstmt(conn, "select id from " + COUNTS_TABLE_NAME + " t where radius = " + radiusString + " and hour is null");
		ResultSet selectAreaRs = db.getRs(selectAreaStmt);
		PreparedStatement selectDateStmt = db.getPstmt(conn, "select DISTINCT to_char(t.TAKEN_DATE,?) d from " + PHOTO_TABLE_NAME + " t where t.region_" + radiusString + "_id = ? order by d");

		//PostGIS: to_timestamp()
//		PreparedStatement selectTagsStmt = db.getPstmt(conn,
//				"select tags from " + PHOTO_TABLE_NAME + " t " +
//					"where t.region_" + radiusString + "_id = ? and " +
//						  "t.taken_date >= to_timestamp(?,'yyyy-MM-dd@HH24') and " +
//						  "t.taken_date <= to_timestamp(?,'yyyy-MM-dd@HH24') + interval '1' " + Level.HOUR);

		//Oracle: to_date()
		/* */
		PreparedStatement selectTagsStmt = db.getPstmt(conn, "select tags from " + PHOTO_TABLE_NAME + " t " + "where t.region_" + radiusString + "_id = ? and " + "t.taken_date >= to_date(?,'yyyy-MM-dd@HH24') and "
				+ "t.taken_date <= to_date(?,'yyyy-MM-dd@HH24') + interval '1' " + Level.HOUR);

		int areaId = -1;
		int checkedSize = 0;
		while (selectAreaRs.next()) {
			Map<String, Map<String, Integer>> hoursTagsCount = new TreeMap<String, Map<String, Integer>>();

			areaId = selectAreaRs.getInt("id");

			selectDateStmt.setString(1, FlickrAreaDao.judgeDbDateCountPatternStr(Level.HOUR));
			selectDateStmt.setInt(2, areaId);

			ResultSet selectDateRs = db.getRs(selectDateStmt);
			while (selectDateRs.next()) {
				String dateStr = selectDateRs.getString("d");
				selectTagsStmt.setInt(1, areaId);
				selectTagsStmt.setString(2, dateStr);
				selectTagsStmt.setString(3, dateStr);

				ResultSet selectTagsRs = db.getRs(selectTagsStmt);

				Map<String, Integer> tagsCount = new HashMap<String, Integer>();

				while (selectTagsRs.next()) {
					for (String tag : StringUtils.split(StringUtils.defaultString(selectTagsRs.getString("tags")), ",")) {
						tagsCount.put(tag, MapUtils.getIntValue(tagsCount, tag) + 1);
					}
				}

				// discards the empty tags
				if (tagsCount.size() > 0) {
					hoursTagsCount.put(dateStr, tagsCount);
				}

				db.close(selectTagsRs);
			}
			db.close(selectDateRs);

			if (hoursTagsCount.size() > 0) {
				addToIndex(hoursTagsCount, Level.HOUR.toString(), areaId, radiusString);
			}

			logger.debug("radius:" + radiusString + "|areaid:" + areaId + "|level:" + Level.HOUR + "|already checked:" + checkedSize++);
			logger.debug("rownum to:" + rownum);
			logger.debug("hoursTagsCount:" + StringUtils.substring(hoursTagsCount.toString(), 0, 200));
			logger.debug("start time:" + startDate.getTime());
			conn.commit();
		}

		logger.info("radius:" + radiusString + "|level:" + Level.HOUR + "|already checked:" + checkedSize);
		logger.info("rownum to:" + rownum);
		logger.info("start time:" + startDate.getTime());

		db.close(selectTagsStmt);
		db.close(selectDateStmt);
		db.close(selectAreaRs);
		db.close(selectAreaStmt);
	}

	private void addToIndex(Map<String, Map<String, Integer>> countsMapToAdd, String queryLevel, int id, String radius) throws SQLException {
		Connection conn = db.getConn();
		PreparedStatement selectCountStmt = db.getPstmt(conn, "select " + queryLevel + " as countStr from " + COUNTS_TABLE_NAME + " where id = ? and radius = ?");
		selectCountStmt.setInt(1, id);
		selectCountStmt.setInt(2, Integer.parseInt(radius));
		ResultSet rs = db.getRs(selectCountStmt);
		SortedMap<String, Map<String, Integer>> countsMap = new TreeMap<String, Map<String, Integer>>();

		if (rs.next()) {
			String preCountStr = rs.getString("countStr");

			if (preCountStr != null) {
				FlickrAreaDao.parseHoursTagsCountDbString(preCountStr, countsMap);
			}
		}

		db.close(rs);
		db.close(selectCountStmt);

		// add countsMap and countsMapToAdd together
		mergeTagsCountsMap(countsMap, countsMapToAdd);

		String countStr = FlickrAreaDao.createDatesTagsCountDbString(countsMap, TAG_LIMIT);

		PreparedStatement updateStmt = db.getPstmt(conn, "update " + COUNTS_TABLE_NAME + " set " + queryLevel.toString() + " = ? where id = ? and radius = ?");
		updateStmt.setString(1, countStr);
		updateStmt.setInt(2, id);
		updateStmt.setInt(3, Integer.parseInt(radius));

//		System.out.println("countStr:" + countStr);
		System.out.println("id:" + id);
		System.out.println("radius:" + radius);
		System.out.println("executeUpdate:" + updateStmt.executeUpdate());

		db.close(updateStmt);
		db.close(conn);
	}

	private void createStopwords(Connection conn) throws SQLException {

		PreparedStatement stmt = db.getPstmt(conn, "select id, radius, hour from " + COUNTS_TABLE_NAME + " where radius = 2560000");
		stmt.setFetchSize(FETCH_SIZE);
		ResultSet pset = db.getRs(stmt);
		Connection connUpdate = db.getConn();

		while (pset.next()) {
			int id = pset.getInt("id");
			String radius = pset.getString("radius");
			String hourStr = pset.getString("hour");

			if (hourStr != null) {
				SortedMap<String, Map<String, Integer>> hoursTagsCount = new TreeMap<String, Map<String, Integer>>();
				Map<String, Integer> tagsCount = new HashMap<String, Integer>();
				List<String> stopwords = new ArrayList<String>();
				FlickrAreaDao.parseHoursTagsCountDbString(hourStr, hoursTagsCount);

				int tagSumNum = 0;
				for (Entry<String, Map<String, Integer>> e : hoursTagsCount.entrySet()) {
					for (Entry<String, Integer> item : e.getValue().entrySet()) {
						tagsCount.put(item.getKey(), MapUtils.getInteger(tagsCount, item.getKey(), 0) + item.getValue());
						tagSumNum += item.getValue();
					}
				}

				Map<String, Integer> sortedTagsCount = FlickrAreaDao.sortTagsCountByValuesDesc(tagsCount);

				int limitNum = 52;
				int i = 0;
				for (Entry<String, Integer> e : sortedTagsCount.entrySet()) {
					if (i++ > limitNum || e.getValue() < 100) {
						break;
					}
					stopwords.add(e.getKey());
				}
				String stopwordsStr = StringUtils.join(stopwords, ',');

				System.out.println("id:" + id + " radius:" + radius);
				System.out.println("stopwordsStr:" + stopwordsStr);
//				System.out.println("hourStr:" + hourStr);
//				System.out.println("tagsCount:" + tagsCount);

/*
				PreparedStatement updateStmt = db.getPstmt(connUpdate, "update " + COUNTS_TABLE_NAME + " set tag_num = ?, tag_sum_num = ? where id = ? and radius = ?");
				updateStmt.setInt(1, tagNum);
				updateStmt.setInt(2, tagSumNum);
				updateStmt.setInt(3, id);
				updateStmt.setInt(4, Integer.parseInt(radius));
				System.out.println("id:" + id);
				System.out.println("radius:" + radius);
				System.out.println("executeUpdate:" + updateStmt.executeUpdate());
				db.close(updateStmt);
*/
				PreparedStatement insertStmt = db.getPstmt(connUpdate, "insert into " + STOPWORD_TABLE_NAME + " (stopword, id, radius) values (?, ?, ?)");
				insertStmt.setString(1, stopwordsStr);
				insertStmt.setInt(2, id);
				insertStmt.setInt(3, Integer.parseInt(radius));

				//		System.out.println("countStr:" + countStr);
				System.out.println("id:" + id);
				System.out.println("radius:" + radius);
				System.out.println("executeInsert:" + insertStmt.executeUpdate());
				db.close(insertStmt);
			}
		}

		db.close(pset);
		db.close(stmt);
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

	public static void mergeTagsCountsMap(SortedMap<String, Map<String, Integer>> countsMap, Map<String, Map<String, Integer>> countsMapToAdd) {
		for (Entry<String, Map<String, Integer>> entry : countsMapToAdd.entrySet()) {
			addToTagsCountsMap(countsMap, entry.getKey(), entry.getValue());
		}
	}

	public static void addToTagsCountsMap(SortedMap<String, Map<String, Integer>> countsMap, String key, Map<String, Integer> value) {
		if (countsMap.containsKey(key)) {
			for (Entry<String, Integer> term : value.entrySet()) {
				Map<String, Integer> count = countsMap.get(key);
				count.put(term.getKey(), term.getValue() + MapUtils.getIntValue(count, term.getKey()));
			}
		} else {
			countsMap.put(key, value);
		}
	}
}
