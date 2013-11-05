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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fraunhofer.iais.spatial.dao.FlickrAreaDao;
import de.fraunhofer.iais.spatial.dto.FlickrAreaDto.Level;
import de.fraunhofer.iais.spatial.util.DBUtil;
import de.fraunhofer.iais.spatial.util.StopWordUtil;

public class LimitFlickrAreaTagsCount {


	/**
	* Logger for this class
	*/
	private static final Logger logger = LoggerFactory.getLogger(LimitFlickrAreaTagsCount.class);

	static int FETCH_SIZE = 1;
	static int TAGS_NUM = 25;
	static String INPUT_COUNTS_TABLE_NAME = "flickr_world_topviewed_1m_tags_count";
	static String OUTPUT_COUNTS_TABLE_NAME = "flickr_world_topviewed_1m_tags_count_25";
	static String OUTPUT_COUNTS_WITHOUT_STOPWORD_TABLE_NAME = "flickr_world_topviewed_1m_tags_count_sw_25";
	static String STOPWORD_TABLE_NAME = "flickr_world_topviewed_5m_tags_stopword";
	static Calendar startDate;
	static boolean ISSTOPWORD = false;

	static DBUtil db = new DBUtil("/jdbc_pg.properties", 3, 1);

	public static void main(String[] args) throws SQLException, IOException {
		
		System.out.println("\nPlease input the INPUT Tag Count TableName:\n[Default: " + INPUT_COUNTS_TABLE_NAME + "]");
		String intputCountTableName = new BufferedReader(new InputStreamReader(System.in)).readLine();
		if (StringUtils.isNotEmpty(intputCountTableName)){
			INPUT_COUNTS_TABLE_NAME = intputCountTableName;
		}
		System.out.println(" INPUT Tag Count Table:" + INPUT_COUNTS_TABLE_NAME);
		
		System.out.println("\nPlease input the OUTPUT Tag Count TableName:\n[Default: " + OUTPUT_COUNTS_TABLE_NAME + "]");
		String outputCountTableName = new BufferedReader(new InputStreamReader(System.in)).readLine();
		if (StringUtils.isNotEmpty(outputCountTableName)){
			OUTPUT_COUNTS_TABLE_NAME = outputCountTableName;
		}
		System.out.println(" OUTPUT Tag Count Table:" + OUTPUT_COUNTS_TABLE_NAME);
		
		System.out.println("\nPlease input the OUTPUT Stopword Tag Count TableName:\n[Default: " + OUTPUT_COUNTS_WITHOUT_STOPWORD_TABLE_NAME + "]");
		String outputSWCountTableName = new BufferedReader(new InputStreamReader(System.in)).readLine();
		if (StringUtils.isNotEmpty(outputSWCountTableName)){
			OUTPUT_COUNTS_WITHOUT_STOPWORD_TABLE_NAME = outputSWCountTableName;
		}
		System.out.println(" OUTPUT Stopword Tag Count Table:" + OUTPUT_COUNTS_WITHOUT_STOPWORD_TABLE_NAME);
		
		System.out.println("\nPlease input the Tag limit:\n[Default: " + TAGS_NUM + "]");
		String tagLimit = new BufferedReader(new InputStreamReader(System.in)).readLine();
		if (StringUtils.isNotEmpty(tagLimit)){
			TAGS_NUM = NumberUtils.toInt(tagLimit);
		}
		System.out.println("Tag Limit:" + TAGS_NUM);
		
		System.out.println("\nFor Stopword:\n[Default: " + ISSTOPWORD + "]");
		String isStopWordStr = new BufferedReader(new InputStreamReader(System.in)).readLine();
		if (Boolean.parseBoolean(isStopWordStr)){
			ISSTOPWORD = true;
		}
		System.out.println("For Stopword:" + ISSTOPWORD);
		
		long start = System.currentTimeMillis();
		startDate = Calendar.getInstance();
		startDate.setTimeInMillis(start);

		LimitFlickrAreaTagsCount t = new LimitFlickrAreaTagsCount();
		if(ISSTOPWORD){
			t.limitWithStopWords();
		}else{
			t.limit();
		}

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
		stmt.setFetchSize(FETCH_SIZE);
		ResultSet pset = db.getRs(stmt);

		while (pset.next()) {
			int id = pset.getInt("id");
			String radius = pset.getString("radius");
			String hourStr = pset.getString("hour");

			if (hourStr != null) {
				SortedMap<String, Map<String, Integer>> hoursTagsCount = new TreeMap<String, Map<String, Integer>>();

				FlickrAreaDao.parseHoursTagsCountDbString(hourStr, hoursTagsCount);

				String countStr = FlickrAreaDao.createDatesTagsCountDbString(hoursTagsCount, TAGS_NUM);

				PreparedStatement updateStmt = db.getPstmt(updateConn, "update " + OUTPUT_COUNTS_TABLE_NAME + " set hour = ? where id = ? and radius = ?");
				updateStmt.setString(1, countStr);
				updateStmt.setInt(2, id);
				updateStmt.setInt(3, Integer.parseInt(radius));

				System.out.println("countStr:" + StringUtils.substring(countStr, 0, 200));
				System.out.println("id:" + id);
				System.out.println("radius:" + radius);
				System.out.println("executeUpdate:" + updateStmt.executeUpdate());
				db.close(updateStmt);
			}
		}
		db.close(pset);
		db.close(stmt);
		db.close(conn);
		db.close(updateConn);
	}

	private void limitWithStopWords() throws SQLException {
		Connection conn = db.getConn();
		Connection updateConn = db.getConn();
		Map<Integer, Set<String>> areasStopwords = loadAreasStopwords(conn);
		conn.setAutoCommit(false);
		PreparedStatement stmt = db.getPstmt(conn, "select t1.id, t1.radius, t1.hour from " + INPUT_COUNTS_TABLE_NAME + " t1 ," + OUTPUT_COUNTS_TABLE_NAME + " t2 where t2.hour is null and t2.id = t1.id");
		stmt.setFetchSize(FETCH_SIZE);
		ResultSet pset = db.getRs(stmt);

		while (pset.next()) {
			int id = pset.getInt("id");
			String radius = pset.getString("radius");
			String hourStr = pset.getString("hour");

			if (hourStr != null) {
				SortedMap<String, Map<String, Integer>> hoursTagsCount = new TreeMap<String, Map<String, Integer>>();

				List<Set<String>> stopwordsList = getStopWordsList(conn, areasStopwords, id);

				FlickrAreaDao.parseHoursTagsCountDbString(hourStr, hoursTagsCount, stopwordsList);

				String countStr = FlickrAreaDao.createDatesTagsCountDbString(hoursTagsCount, TAGS_NUM);

				PreparedStatement updateStmt = db.getPstmt(updateConn, "update " + OUTPUT_COUNTS_WITHOUT_STOPWORD_TABLE_NAME + " set hour = ? where id = ? and radius = ?");
				updateStmt.setString(1, countStr);
				updateStmt.setInt(2, id);
				updateStmt.setInt(3, Integer.parseInt(radius));

				System.out.println("countStr_without_stopwords:" + StringUtils.substring(countStr, 0, 200));
				System.out.println("id:" + id);
				System.out.println("radius:" + radius);
				System.out.println("executeUpdate:" + updateStmt.executeUpdate());
				db.close(updateStmt);
			}

			if (hourStr != null) {
				SortedMap<String, Map<String, Integer>> hoursTagsCount = new TreeMap<String, Map<String, Integer>>();

				FlickrAreaDao.parseHoursTagsCountDbString(hourStr, hoursTagsCount);

				String countStr = FlickrAreaDao.createDatesTagsCountDbString(hoursTagsCount, TAGS_NUM);

				PreparedStatement updateStmt = db.getPstmt(updateConn, "update " + OUTPUT_COUNTS_TABLE_NAME + " set hour = ? where id = ? and radius = ?");
				updateStmt.setString(1, countStr);
				updateStmt.setInt(2, id);
				updateStmt.setInt(3, Integer.parseInt(radius));

				System.out.println("countStr:" + StringUtils.substring(countStr, 0, 200));
				System.out.println("id:" + id);
				System.out.println("radius:" + radius);
				System.out.println("executeUpdate:" + updateStmt.executeUpdate());
				db.close(updateStmt);
			}
			
			System.out.println("current time:" + new Date());
			System.out.println("escaped time:" + (System.currentTimeMillis() - startDate.getTimeInMillis()) / 1000.0); //$NON-NLS-1$
		}
		db.close(pset);
		db.close(stmt);
		db.close(conn);
		db.close(updateConn);
	}

	public static List<Set<String>> getStopWordsList(Connection conn, Map<Integer, Set<String>> areasStopwords, int id) throws SQLException {
	    List<Set<String>> stopwordsList = new ArrayList<Set<String>>();
	    stopwordsList.add(StopWordUtil.stopwordsGloble);

	    PreparedStatement areaStmt = db.getPstmt(conn, "select t1.id as pid from flickr_world_area_2560000 t1, flickr_world_area t2 where t2.id = ? and ST_Intersects(t1.geom, t2.geom)");
	    areaStmt.setInt(1, id);
	    ResultSet swRs = db.getRs(areaStmt);
	    while (swRs.next()) {
	    	int pid = swRs.getInt("pid");
	    	stopwordsList.add(areasStopwords.get(pid));
	    	System.out.println("id:" + id + " pid" + pid + " stopwords" + areasStopwords.get(pid));
	    }
	    db.close(swRs);
	    db.close(areaStmt);
	    return stopwordsList;
    }

	public static Map<Integer, Set<String>> loadAreasStopwords(Connection conn) throws SQLException {
		Map<Integer, Set<String>> areasStopwords = new HashMap<Integer, Set<String>>();
		PreparedStatement stmt = db.getPstmt(conn, "select id, stopword from " + STOPWORD_TABLE_NAME);
		ResultSet rs = db.getRs(stmt);
		while (rs.next()) {
			int id = rs.getInt("id");
			String stopwordsStr = rs.getString("stopword");
			Set<String> stopwords = new HashSet<String>();
			CollectionUtils.addAll(stopwords, StringUtils.split(stopwordsStr, ','));
			areasStopwords.put(id, stopwords);
			System.out.println("id:" + id + " " + stopwords);
		}
		db.close(rs);
		db.close(stmt);
		return areasStopwords;
	}

	private void countDay(Connection conn) throws SQLException {

		PreparedStatement stmt = db.getPstmt(conn, "select id, radius, hour from " + INPUT_COUNTS_TABLE_NAME);
		stmt.setFetchSize(FETCH_SIZE);
		ResultSet pset = db.getRs(stmt);

		while (pset.next()) {
			int id = pset.getInt("id");
			String radius = pset.getString("radius");
			String hourStr = pset.getString("hour");
			System.out.println("id:" + id + "\thourStr:" + hourStr);

			if (hourStr != null) {
				SortedMap<String, Map<String, Integer>> hoursTagsCount = new TreeMap<String, Map<String, Integer>>();
				SortedMap<String, Map<String, Integer>> datesTagsCount = new TreeMap<String, Map<String, Integer>>();

				FlickrAreaDao.parseHoursTagsCountDbString(hourStr, hoursTagsCount);

				for (Entry<String, Map<String, Integer>> e : hoursTagsCount.entrySet()) {
					Pattern p = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})@\\d{2}");
					Matcher m = p.matcher(e.getKey());
					m.find();
					String key = m.group(1);
					JoinFlickrAreaTagsCount.addToTagsCountsMap(datesTagsCount, key, e.getValue());
				}

				System.out.println("datesTagsCount:" + datesTagsCount);
				update(datesTagsCount, Level.DAY.toString(), id, radius);
			}
		}
		db.close(pset);
		db.close(stmt);
	}

	private void countMonth(Connection conn) throws SQLException {

		PreparedStatement stmt = db.getPstmt(conn, "select id, radius, hour from " + INPUT_COUNTS_TABLE_NAME);
		stmt.setFetchSize(FETCH_SIZE);
		ResultSet pset = db.getRs(stmt);

		while (pset.next()) {
			int id = pset.getInt("id");
			String radius = pset.getString("radius");
			String hourStr = pset.getString("hour");

			if (hourStr != null) {
				SortedMap<String, Map<String, Integer>> hoursTagsCount = new TreeMap<String, Map<String, Integer>>();
				SortedMap<String, Map<String, Integer>> datesTagsCount = new TreeMap<String, Map<String, Integer>>();

				FlickrAreaDao.parseHoursTagsCountDbString(hourStr, hoursTagsCount);

				for (Entry<String, Map<String, Integer>> e : hoursTagsCount.entrySet()) {
					Pattern p = Pattern.compile("(\\d{4}-\\d{2})-\\d{2}@\\d{2}");
					Matcher m = p.matcher(e.getKey());
					m.find();
					String key = m.group(1);
					JoinFlickrAreaTagsCount.addToTagsCountsMap(datesTagsCount, key, e.getValue());
				}

				update(datesTagsCount, Level.MONTH.toString(), id, radius);
			}
		}
		db.close(pset);
		db.close(stmt);
	}

	private void countYear(Connection conn) throws SQLException {

		PreparedStatement stmt = db.getPstmt(conn, "select id, radius, hour from " + INPUT_COUNTS_TABLE_NAME);
		stmt.setFetchSize(FETCH_SIZE);
		ResultSet pset = db.getRs(stmt);

		while (pset.next()) {
			int id = pset.getInt("id");
			String radius = pset.getString("radius");
			String hourStr = pset.getString("hour");

			if (hourStr != null) {
				SortedMap<String, Map<String, Integer>> hoursTagsCount = new TreeMap<String, Map<String, Integer>>();
				SortedMap<String, Map<String, Integer>> datesTagsCount = new TreeMap<String, Map<String, Integer>>();

				FlickrAreaDao.parseHoursTagsCountDbString(hourStr, hoursTagsCount);

				for (Entry<String, Map<String, Integer>> e : hoursTagsCount.entrySet()) {
					Pattern p = Pattern.compile("(\\d{4})-\\d{2}-\\d{2}@\\d{2}");
					Matcher m = p.matcher(e.getKey());
					m.find();
					String key = m.group(1);
					JoinFlickrAreaTagsCount.addToTagsCountsMap(datesTagsCount, key, e.getValue());
				}

				update(datesTagsCount, Level.YEAR.toString(), id, radius);
			}
		}
		db.close(pset);
		db.close(stmt);
	}

	private void countTotal(Connection conn) throws SQLException {

		PreparedStatement stmt = db.getPstmt(conn, "select id, radius, hour from " + INPUT_COUNTS_TABLE_NAME);
		stmt.setFetchSize(FETCH_SIZE);
		ResultSet pset = db.getRs(stmt);

		while (pset.next()) {
			int id = pset.getInt("id");
			String radius = pset.getString("radius");
			String hourStr = pset.getString("hour");

			if (hourStr != null) {
				SortedMap<String, Map<String, Integer>> hoursTagsCount = new TreeMap<String, Map<String, Integer>>();
				SortedMap<String, Map<String, Integer>> datesTagsCount = new TreeMap<String, Map<String, Integer>>();

				FlickrAreaDao.parseHoursTagsCountDbString(hourStr, hoursTagsCount);

				for (Entry<String, Map<String, Integer>> e : hoursTagsCount.entrySet()) {
					JoinFlickrAreaTagsCount.addToTagsCountsMap(datesTagsCount, "total", e.getValue());
				}
				System.out.println("hourStr:" + hourStr);
				System.out.println("areasTagsCount:" + datesTagsCount);

				update(datesTagsCount, "total", id, radius);
			}
		}
		db.close(pset);
		db.close(stmt);
	}

	private void countTotalSum(Connection conn) throws SQLException {

		PreparedStatement stmt = db.getPstmt(conn, "select id, radius, hour from " + INPUT_COUNTS_TABLE_NAME + " where radius = 2560000");
		stmt.setFetchSize(FETCH_SIZE);
		ResultSet pset = db.getRs(stmt);
		Connection connUpdate = db.getConn();

		while (pset.next()) {
			int id = pset.getInt("id");
			String radius = pset.getString("radius");
			String hourStr = pset.getString("hour");

			if (hourStr != null) {
				SortedMap<String, Map<String, Integer>> hoursTagsCount = new TreeMap<String, Map<String, Integer>>();
				Map<String, Integer> tagsCount = new TreeMap<String, Integer>();

				FlickrAreaDao.parseHoursTagsCountDbString(hourStr, hoursTagsCount);

				int tagSumNum = 0;
				for (Entry<String, Map<String, Integer>> e : hoursTagsCount.entrySet()) {
					for (Entry<String, Integer> item : e.getValue().entrySet()) {
						tagsCount.put(item.getKey(), MapUtils.getInteger(tagsCount, item.getKey(), 0) + item.getValue());
						tagSumNum += item.getValue();
					}
				}

				System.out.println("id:" + id + " radius:" + radius);
//				System.out.println("hourStr:" + hourStr);
//				System.out.println("tagsCount:" + tagsCount);

				int tagNum = tagsCount.size();

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
				PreparedStatement insertStmt = db.getPstmt(connUpdate, "insert into flickr_world_topviewed_5m_tags_count_tag_sum (tag_num, tag_sum_num, id, radius) values (?, ?, ?, ?)");
				insertStmt.setInt(1, tagNum);
				insertStmt.setInt(2, tagSumNum);
				insertStmt.setInt(3, id);
				insertStmt.setInt(4, Integer.parseInt(radius));

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

	private void update(SortedMap<String, Map<String, Integer>> countsMap, String queryLevel, int id, String radius) throws SQLException {

		Connection conn = db.getConn();
		String countStr = FlickrAreaDao.createDatesTagsCountDbString(countsMap);

		PreparedStatement updateStmt = db.getPstmt(conn, "update " + OUTPUT_COUNTS_TABLE_NAME + " set " + queryLevel.toString() + " = ? where id = ? and radius = ?");
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
}
