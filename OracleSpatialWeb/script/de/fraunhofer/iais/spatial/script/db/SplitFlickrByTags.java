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
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fraunhofer.iais.spatial.dao.FlickrAreaDao;
import de.fraunhofer.iais.spatial.dto.FlickrAreaDto.Level;
import de.fraunhofer.iais.spatial.util.DBUtil;

public class SplitFlickrByTags {

	private static final int MAX_TAG_LENGTH = 30;

	/**
	* Logger for this class
	*/
	private static final Logger logger = LoggerFactory.getLogger(SplitFlickrByTags.class);

	final static int FETCH_SIZE = 1;
	final static int BATCH_SIZE = 1000;
	static String PHOTO_TABLE_NAME = "flickr_world_topviewed_1m";
	static String SPLIT_TABLE_NAME = "flickr_world_topviewed_1m_spilt_tag";
	static long start = System.currentTimeMillis();

	static int rownum = 1;
	static Calendar startDate;

	static DBUtil db = new DBUtil("/jdbc_pg.properties", 18, 3);

	public static void main(String[] args) throws IOException {

		System.out.println("\nPlease input the Photo TableName:\n[Default: " + PHOTO_TABLE_NAME + "]");
		String photoTableName = new BufferedReader(new InputStreamReader(System.in)).readLine();
		if (StringUtils.isNotEmpty(photoTableName)) {
			PHOTO_TABLE_NAME = photoTableName;
		}
		System.out.println("Photo Table:" + PHOTO_TABLE_NAME);

		System.out.println("\nPlease input the Split TableName:\n[Default: " + SPLIT_TABLE_NAME + "]");
		String countTableName = new BufferedReader(new InputStreamReader(System.in)).readLine();
		if (StringUtils.isNotEmpty(countTableName)) {
			SPLIT_TABLE_NAME = countTableName;
		}
		System.out.println("Split Table:" + SPLIT_TABLE_NAME);

		startDate = Calendar.getInstance();
		startDate.setTimeInMillis(start);

		SplitFlickrByTags t = new SplitFlickrByTags();
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

		Connection selectConn = db.getConn();
		try {
			selectConn.setAutoCommit(false);

			split(selectConn, radiusList);
		} catch (SQLException e) {
			logger.error("begin()", e); //$NON-NLS-1$
		} finally {
			db.close(selectConn);
		}
	}

	private void split(Connection selectConn, ArrayList<String> radiusList) throws SQLException {
		Connection conn = db.getConn();
		String radiusIdStr = "";
		String radiusIdParameterStr = "";
		for (String radius : radiusList) {
			radiusIdStr += "region_" + radius + "_id, ";
			radiusIdParameterStr += "?, ";
		}
		try {
			radiusIdStr = StringUtils.removeEnd(radiusIdStr, ", ");
			radiusIdParameterStr = StringUtils.removeEnd(radiusIdParameterStr, ", ");
			PreparedStatement stmt = db.getPstmt(selectConn, "select photo_id, taken_date, tags, " + radiusIdStr + " from " + PHOTO_TABLE_NAME + " where length(tags)>1");
			PreparedStatement insertStmt = db.getPstmt(conn, "insert into " + SPLIT_TABLE_NAME + " (photo_id, taken_date, tag, " + radiusIdStr + ") values (?, ?, ?, " + radiusIdParameterStr + ")");
			stmt.setFetchSize(FETCH_SIZE);
			ResultSet rs = db.getRs(stmt);
			int splitNum = 0;
			int insertNum = 0;
			while (rs.next()) {
				String tags[] = (StringUtils.split(rs.getString("tags"), ','));
				if (ArrayUtils.isNotEmpty(tags)) {
					Set<String> tagsSet = new TreeSet<String>();
					CollectionUtils.addAll(tagsSet, tags);
					for (String tag : tagsSet) {
						if (tag.length() <= MAX_TAG_LENGTH && StringUtils.isNotBlank(tag)) {
							System.out.println(rs.getLong("photo_id") + " " + tag);
							int i = 1;
							insertStmt.setLong(i++, rs.getLong("photo_id"));
							insertStmt.setTimestamp(i++, rs.getTimestamp("taken_date"));
							insertStmt.setString(i++, tag);
							for (String radius : radiusList) {
								insertStmt.setInt(i++, rs.getInt("region_" + radius + "_id"));
							}
							insertNum++;
//							insertStmt.executeUpdate();
							insertStmt.addBatch();
						}
					}
				}
				splitNum++;
				if (splitNum % BATCH_SIZE == 0) {
					insertStmt.executeBatch();
				}
				logger.info("splitNum: " + splitNum + " \tinsertNum: " + insertNum + " \t- escaped time:" + (System.currentTimeMillis() - start) / 1000.0); //$NON-NLS-1$
			}
			insertStmt.executeBatch();
			db.close(rs);
			db.close(insertStmt);
			db.close(stmt);
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {

			db.close(conn);
		}
	}

}
