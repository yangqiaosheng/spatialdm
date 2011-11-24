package de.fraunhofer.iais.spatial.script.db;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import de.fraunhofer.iais.spatial.util.DBUtil;

public class ValidateCount {

	static DBUtil db = new DBUtil("/jdbc_pg.properties", 4, 1);

	public static void main(String[] args) throws Exception {
//		countYear();
		select();
	}
	private static void select() throws Exception {
		Connection conn = db.getConn();

		PreparedStatement personStmt = db.getPstmt(conn, "select id, total from flickr_world_topviewed_5m_tags_count where radius = 2560000");
		ResultSet pset = db.getRs(personStmt);

		while (pset.next()) {
			String id = pset.getString("id");
			String hourStr = pset.getString("total");
			String line = id + ": \t" + StringUtils.substring(hourStr, 0, 1000);
			FileUtils.writeStringToFile(new File("temp\result.txt"), line + "\r\n", true);
			System.out.println(line);
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
