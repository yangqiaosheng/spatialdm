package de.fraunhofer.iais.spatial.script.db;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import au.com.bytecode.opencsv.CSVWriter;
import de.fraunhofer.iais.spatial.util.DBUtil;

public class ValidateCount {

	private static final int LINE_LENGTH = 10000;
	private static final int TAG_NUM = 500;
	static DBUtil db = new DBUtil("/jdbc.properties", 4, 1);

	public static void main(String[] args) throws Exception {
//		photoExport();
//		countYear();
//		tagCountExport();
		select();
	}

	private static void tagCountExport() throws Exception {
		Connection conn = db.getConn();

		PreparedStatement personStmt = db.getPstmt(conn, "select id, total from flickr_world_topviewed_5m_tags_count where id = 15175 limit 1");
		personStmt.setFetchSize(1);
		ResultSet pset = db.getRs(personStmt);

		while (pset.next()) {
			String id = pset.getString("id");
			String countStr = pset.getString("total");
			String line = id + ": \t" + StringUtils.substring(countStr, 0, LINE_LENGTH);

			FileUtils.writeStringToFile(new File("temp/result_" + LINE_LENGTH + ".txt"), line + "\r\n", true);
			System.out.println(line);

			CSVWriter writer = new CSVWriter(new FileWriter("temp/result_" + TAG_NUM + ".csv"), ';');

			int num = 0;
			for (String term : StringUtils.split(StringUtils.substringAfter(countStr, "total:<"), ",")) {
				System.out.println(term);
				if (num++ > TAG_NUM) {
					break;
				}
				writer.writeNext(StringUtils.split(term, "|"));
			}
			writer.close();
		}
		db.close(pset);
		db.close(personStmt);
		db.close(conn);
	}

	private static void photoExport() throws Exception {
		Connection conn = db.getConn();

		PreparedStatement personStmt = db.getPstmt(conn, "select photo_id, user_id, taken_date, url, title, description, tags from flickr_world_region_id_15175_r40000_1000 order by taken_date");
		personStmt.setFetchSize(1);
		ResultSet pset = db.getRs(personStmt);

		CSVWriter writer = new CSVWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream("temp/flickr_world_region_id_15175_r40000_1000.csv"), "UTF-8")), ';');
		writer.writeAll(pset, true);
		writer.close();
		db.close(pset);
		db.close(personStmt);
		db.close(conn);
	}

	private static void select() throws Exception {
		Connection conn = db.getConn();

		PreparedStatement personStmt = db.getPstmt(conn, "select title from flickr_europe_201108 t where t.photo_id = 3216703021");
		ResultSet pset = db.getRs(personStmt);

		if(pset.next()){
			System.out.println(pset.getString(1));
		}
		db.close(pset);
		db.close(personStmt);
		db.close(conn);
	}

	private static void countYear() throws Exception {
		Connection conn = db.getConn();

		PreparedStatement personStmt = db.getPstmt(conn, "select * from flickr_europe_topviewed_1m_tags_count_20 where id = 12");
		ResultSet pset = db.getRs(personStmt);

		while (pset.next()) {
			String id = pset.getString("id");
			String hourStr = pset.getString("hour");
			String total = pset.getString("day");
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
				System.out.println("id:" + id + "!" + hourStr);
				System.out.println("id:" + id + "!" + yearStr);
				System.out.println("id:" + id + "!" + total);

			}
		}
		db.close(pset);
		db.close(personStmt);
		db.close(conn);
	}
}
