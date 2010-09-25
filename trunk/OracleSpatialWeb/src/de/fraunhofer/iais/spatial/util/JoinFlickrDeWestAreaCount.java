package de.fraunhofer.iais.spatial.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.spatial.geometry.JGeometry;
import oracle.sql.STRUCT;

public class JoinFlickrDeWestAreaCount {
	static long start = System.currentTimeMillis();
	static int count = 0;

	public static void main(String[] args) throws Exception {
		JoinFlickrDeWestAreaCount t = new JoinFlickrDeWestAreaCount();

		//		t.countHour("40000");
		//		t.countHour("20000");
		//		t.countHour("10000");
		//		t.countHour("5000");
		//		t.countDay();
		//		t.countMonth();
		//		t.countYear();
		//		t.countTotal();
		//
		//		t.countPerson("80000");
		//		t.countPerson("40000");
		//		t.countPerson("20000");
		//		t.countPerson("10000");
		//		t.countPerson("5000");

		//		t.groupPhotos();

		t.copyTable();

		System.out.println("escaped time:" + (System.currentTimeMillis() - start) / 1000.0);
		//		
		//		t.update("Haolin",  "FLICKR_DE_WEST_TABLE_COUNT", "PERSON", "1   ", "5000");
		//		t.select();
		//		t.listCalendar();
		//		t.countTotal();
		//				t.insert("Haolin",  "FLICKR_DE_WEST_TABLE_COUNT", "PERSON", "115", radiusString);
	}

	private void countHour(String radiusString) throws Exception {
		Connection conn = DB.getConn();
		PreparedStatement selectAreaStmt = DB.getPstmt(conn, "select * from FLICKR_DE_WEST_TABLE_" + radiusString + " a");
		ResultSet selectAreaRs = DB.getRs(selectAreaStmt);
		String areaId = "";
		int i = 0;
		for (int l = 0; l < 0; l++) {
			selectAreaRs.next(); //skip
		}
		while (selectAreaRs.next()) {
			StringBuffer hourCount = new StringBuffer();
			areaId = selectAreaRs.getString("id");
			STRUCT st = (oracle.sql.STRUCT) selectAreaRs.getObject("geom");
			// convert STRUCT into geometry
			JGeometry j_geom = JGeometry.load(st);
			double mbrX1 = j_geom.getMBR()[0];
			double mbrY1 = j_geom.getMBR()[1];
			double mbrX2 = j_geom.getMBR()[2];
			double mbrY2 = j_geom.getMBR()[3];
			System.out.println("area id:" + areaId + "mbr:" + mbrX1 + ":" + mbrY1 + ":" + mbrX2 + ":" + mbrY2);

			PreparedStatement selectFlickrStmt = DB.getPstmt(conn, "select DISTINCT to_char(dt,'yyyy-MM-dd@HH24') d from FLICKR_DE_WEST_TABLE where longitude > ? and longitude < ? and latitude > ? and latitude < ? order by d");
			selectFlickrStmt.setDouble(1, mbrX1);
			selectFlickrStmt.setDouble(2, mbrX2);
			selectFlickrStmt.setDouble(3, mbrY1);
			selectFlickrStmt.setDouble(4, mbrY2);
			ResultSet selectFlickrRs = DB.getRs(selectFlickrStmt);

			for (int j = 0; selectFlickrRs.next(); j++) {
				String date = selectFlickrRs.getString("d");
				PreparedStatement joinStmt = DB.getPstmt(conn, "select count(*) num " + "from FLICKR_DE_WEST_TABLE_" + radiusString
						+ " t, (select * from flickr_de_west_table f where f.longitude > ? and f.longitude < ? and f.latitude > ? and f.latitude < ? and  f.dt >= to_date(?,'yyyy-MM-dd@HH24:mi:ss') and f.dt <= to_date(?,'yyyy-MM-dd@HH24:mi:ss')) f2"
						+ " WHERE t.id=?  and sdo_relate(t.geom, SDO_geometry(2001,8307,SDO_POINT_TYPE(f2.longitude, f2.latitude, NULL),NULL,NULL),'mask=anyinteract,querytype=join') = 'TRUE'");
				joinStmt.setDouble(1, mbrX1);
				joinStmt.setDouble(2, mbrX2);
				joinStmt.setDouble(3, mbrY1);
				joinStmt.setDouble(4, mbrY2);
				joinStmt.setString(5, date + ":00:00");
				joinStmt.setString(6, date + ":59:59");
				joinStmt.setString(7, areaId);
				ResultSet joinRs = DB.getRs(joinStmt);
				if (joinRs.next()) {
					int num = joinRs.getInt("num");
					i += num;
					String info = date + ":" + num + ";";
					System.out.println(info);
					if (num != 0) {
						hourCount.append(info);
					}
				}
				System.out.println("hour i:" + i + " radius: " + radiusString + " escaped time:" + (System.currentTimeMillis() - start) / 1000.0);
				joinRs.close();
				joinStmt.close();
			}
			System.out.println(hourCount);
			insert(hourCount.toString(), "FLICKR_DE_WEST_TABLE_COUNT", "HOUR", areaId, radiusString);
			selectFlickrRs.close();
			selectFlickrStmt.close();
		}

		selectAreaRs.close();
		selectAreaStmt.close();
		conn.close();
	}

	private void countPerson(String radiusString) throws Exception {
		Connection conn = DB.getConn();
		PreparedStatement selectAreaStmt = DB.getPstmt(conn, "select * from FLICKR_DE_WEST_TABLE_" + radiusString + " a");
		ResultSet selectAreaRs = DB.getRs(selectAreaStmt);
		String areaId = "";
		int i = 0;
		while (selectAreaRs.next()) {
			StringBuffer personCount = new StringBuffer();
			areaId = selectAreaRs.getString("id");
			STRUCT st = (oracle.sql.STRUCT) selectAreaRs.getObject("geom");
			// convert STRUCT into geometry
			JGeometry j_geom = JGeometry.load(st);
			double mbrX1 = j_geom.getMBR()[0];
			double mbrY1 = j_geom.getMBR()[1];
			double mbrX2 = j_geom.getMBR()[2];
			double mbrY2 = j_geom.getMBR()[3];
			System.out.println("area id:" + areaId + "mbr:" + mbrX1 + ":" + mbrY1 + ":" + mbrX2 + ":" + mbrY2);

			PreparedStatement selectFlickrStmt = DB.getPstmt(conn, "select DISTINCT person from FLICKR_DE_WEST_TABLE where longitude > ? and longitude < ? and latitude > ? and latitude < ?");
			selectFlickrStmt.setDouble(1, mbrX1);
			selectFlickrStmt.setDouble(2, mbrX2);
			selectFlickrStmt.setDouble(3, mbrY1);
			selectFlickrStmt.setDouble(4, mbrY2);
			ResultSet selectFlickrRs = DB.getRs(selectFlickrStmt);
			for (int j = 0; selectFlickrRs.next(); j++) {

				PreparedStatement joinStmt = DB.getPstmt(conn, "select count(*) num " + "from FLICKR_DE_WEST_TABLE_" + radiusString
						+ " t, (select * from flickr_de_west_table f where f.longitude > ? and f.longitude < ? and f.latitude > ? and f.latitude < ? and  f.person=?) f2"
						+ " WHERE t.id=?  and sdo_relate(t.geom, SDO_geometry(2001,8307,SDO_POINT_TYPE(f2.longitude, f2.latitude, NULL),NULL,NULL),'mask=anyinteract,querytype=join') = 'TRUE'");
				String person = selectFlickrRs.getString("person");
				joinStmt.setDouble(1, mbrX1);
				joinStmt.setDouble(2, mbrX2);
				joinStmt.setDouble(3, mbrY1);
				joinStmt.setDouble(4, mbrY2);
				joinStmt.setString(5, person);
				joinStmt.setString(6, areaId);
				ResultSet joinRs = DB.getRs(joinStmt);

				if (joinRs.next()) {
					int num = joinRs.getInt("num");
					i += num;
					if (num != 0) {
						personCount.append(person + ":" + num + ";");
					}
				}
				joinRs.close();
				joinStmt.close();
				if (j % 50 == 0) {
					System.out.println("j:" + j);
				}
				System.out.println("person i:" + i + " radius: " + radiusString + " escaped time:" + (System.currentTimeMillis() - start) / 1000.0);
			}
			System.out.println("i:" + i);
			System.out.println(personCount);
			update(personCount.toString(), "FLICKR_DE_WEST_TABLE_COUNT", "PERSON", areaId, radiusString);
			selectFlickrRs.close();
			selectFlickrStmt.close();
		}

		selectAreaRs.close();
		selectAreaStmt.close();
		conn.close();
	}

	private void countDay() throws Exception {
		Connection conn = DB.getConn();

		PreparedStatement personStmt = DB.getPstmt(conn, "select * from FLICKR_DE_WEST_TABLE_Count");
		ResultSet pset = DB.getRs(personStmt);

		while (pset.next()) {
			String id = pset.getString("id");
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
				PreparedStatement iStmt = DB.getPstmt(conn, "update FLICKR_DE_WEST_TABLE_count set day = ? where id = ? and radius = ?");
				iStmt.setString(1, dayStr.toString());
				iStmt.setString(2, id);
				iStmt.setString(3, radius);
				iStmt.executeUpdate();
				iStmt.close();
				System.out.println("id:" + id + "!" + dayStr);
			}
		}
		pset.close();
		personStmt.close();
		conn.close();
	}

	private void countMonth() throws Exception {
		Connection conn = DB.getConn();

		PreparedStatement personStmt = DB.getPstmt(conn, "select * from FLICKR_DE_WEST_TABLE_Count");
		ResultSet pset = DB.getRs(personStmt);

		while (pset.next()) {
			String id = pset.getString("id");
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
				PreparedStatement iStmt = DB.getPstmt(conn, "update FLICKR_DE_WEST_TABLE_count set month = ? where id = ? and radius = ?");
				iStmt.setString(1, monthStr.toString());
				iStmt.setString(2, id);
				iStmt.setString(3, radius);
				iStmt.executeUpdate();
				iStmt.close();
				System.out.println("id:" + id + "!" + monthStr);
			}
		}
		pset.close();
		personStmt.close();
		conn.close();
	}

	private void countYear() throws Exception {
		Connection conn = DB.getConn();

		PreparedStatement personStmt = DB.getPstmt(conn, "select * from FLICKR_DE_WEST_TABLE_Count");
		ResultSet pset = DB.getRs(personStmt);

		while (pset.next()) {
			String id = pset.getString("id");
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
				PreparedStatement iStmt = DB.getPstmt(conn, "update FLICKR_DE_WEST_TABLE_count set year = ? where id = ? and radius = ?");
				iStmt.setString(1, yearStr.toString());
				iStmt.setString(2, id);
				iStmt.setString(3, radius);
				iStmt.executeUpdate();
				iStmt.close();
				System.out.println("id:" + id + "!" + yearStr);
			}
		}
		pset.close();
		personStmt.close();
		conn.close();
	}

	private void countTotal() throws Exception {
		Connection conn = DB.getConn();

		PreparedStatement personStmt = DB.getPstmt(conn, "select * from FLICKR_DE_WEST_TABLE_Count");
		ResultSet pset = DB.getRs(personStmt);

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

				PreparedStatement iStmt = DB.getPstmt(conn, "update FLICKR_DE_WEST_TABLE_count set total = ? where id = ? and radius = ?");
				iStmt.setInt(1, hour);
				iStmt.setString(2, id);
				iStmt.setString(3, radius);
				iStmt.executeUpdate();
				iStmt.close();
			}
		}
		pset.close();
		personStmt.close();
		conn.close();
	}

	private void insert(String count, String table, String column, String id, String radius) throws Exception {
		Connection conn = DB.getConn();
		PreparedStatement insertStmt = DB.getPstmt(conn, "insert into " + table + " (id, radius, " + column + ") values (?, ?, ?)");
		insertStmt.setString(1, id);
		insertStmt.setString(2, radius);
		insertStmt.setString(3, count);
		insertStmt.executeUpdate();
		insertStmt.close();
		conn.close();
	}

	private void update(String count, String table, String column, String id, String radius) throws Exception {
		Connection conn = DB.getConn();
		PreparedStatement insertStmt = DB.getPstmt(conn, "update " + table + " set " + column + " = ? where id = ? and radius = ?");
		insertStmt.setString(1, count);
		insertStmt.setString(2, id);
		insertStmt.setString(3, radius);

		insertStmt.executeUpdate();
		insertStmt.close();
		conn.close();
	}

	private void select() throws Exception {

		Connection conn = DB.getConn();
		PreparedStatement selectStmt = DB.getPstmt(conn, "select * from FLICKR_DE_WEST_TABLE_count");
		ResultSet rset = DB.getRs(selectStmt);

		while (rset.next()) {
			System.out.println(rset.getString("id") + rset.getString("radius") + rset.getString("hour"));
		}

		rset.close();
		selectStmt.close();
		conn.close();
	}

	private void listCalendar() {
		Calendar start = Calendar.getInstance();
		Calendar end = Calendar.getInstance();
		start.set(2005, 00, 01); //2004-12-31 23:59:59
		start.set(Calendar.HOUR, 12);
		start.set(Calendar.MINUTE, 59);
		start.set(Calendar.SECOND, 59);
		start.add(Calendar.HOUR, -1);
		start.add(Calendar.DATE, -1);
		end.set(2010, 00, 01); //2009-12-31 23:59:59
		end.set(Calendar.HOUR, 12);
		end.set(Calendar.MINUTE, 59);
		end.set(Calendar.SECOND, 59);
		end.add(Calendar.HOUR, -1);
		end.add(Calendar.DATE, -1);
		while (start.before(end)) {
			Format s = new SimpleDateFormat("yyyy-MM-dd@HH:mm:ss");

			System.out.print(s.format(start.getTime()) + "|");
			start.add(Calendar.HOUR, 1);
			start.add(Calendar.SECOND, -1);
			System.out.println(s.format(start.getTime()));
			start.add(Calendar.SECOND, 1);
		}
	}

	private void groupPhotos() throws SQLException {
		int i = 0;
		List<String> radiuses = new ArrayList<String>();
		radiuses.add("5000");
		radiuses.add("10000");
		radiuses.add("20000");
		radiuses.add("40000");
		radiuses.add("80000");
		Connection conn = DB.getConn();
		PreparedStatement pstmt1 = DB.getPstmt(conn, "select PHOTO_ID, LONGITUDE, LATITUDE from FLICKR_DE_WEST_TABLE");
		ResultSet rs1 = DB.getRs(pstmt1);
		while (rs1.next()) {
			double x = rs1.getDouble("LONGITUDE");
			double y = rs1.getDouble("LATITUDE");
			for (String radius : radiuses) {
				PreparedStatement pstmt2 = DB.getPstmt(conn, "select ID from FLICKR_DE_WEST_TABLE_" + radius + " c, user_sdo_geom_metadata m" + " WHERE m.table_name = 'FLICKR_DE_WEST_TABLE_" + radius
						+ "' and sdo_relate(c.geom, SDO_geometry(2001,8307,SDO_POINT_TYPE(?, ?, NULL),NULL,NULL),'mask=anyinteract') = 'TRUE'");
				pstmt2.setDouble(1, x);
				pstmt2.setDouble(2, y);
				ResultSet rs2 = DB.getRs(pstmt2);
				if (rs2.next()) {
					System.out.println("i:" + (i++) + " escaped time:" + (System.currentTimeMillis() - start) / 1000.0);
					PreparedStatement pstmt3 = DB.getPstmt(conn, "update FLICKR_DE_WEST_TABLE_GEOM set CLUSTER_R" + radius + "_ID = ? where PHOTO_ID = ?");
					pstmt3.setString(1, rs2.getString("ID"));
					pstmt3.setString(2, rs1.getString("PHOTO_ID"));
					pstmt3.executeUpdate();
					DB.close(pstmt3);
				}
				DB.close(pstmt2);
				DB.close(rs2);
			}
		}
		DB.close(rs1);
		DB.close(pstmt1);
		DB.close(conn);
	}

	private void copyTable() throws ClassNotFoundException, SQLException {
		Connection connFrom = null;
		Connection connTo = null;
		Class.forName("oracle.jdbc.driver.OracleDriver");
		try {
			connFrom = DriverManager.getConnection("jdbc:oracle:thin:@localhost:1521:XE", "Gennady_flickr", "gennady");
			connTo = DriverManager.getConnection("jdbc:oracle:thin:@plan-b.iais.fraunhofer.de:1521:PLANB", "Gennady_flickr", "gennady");
			connTo.setAutoCommit(false);

			PreparedStatement pstmtSelect = connFrom.prepareStatement("select * from flickr_de_west_table_geom");
			ResultSet rsSelect = pstmtSelect.executeQuery();
			while (rsSelect.next()) {
				System.out.println("count:" + (count++) + " escaped time:" + (System.currentTimeMillis() - start) / 1000.0 + "s");
				PreparedStatement pstmtInsert = connTo.prepareStatement("insert into flickr_de_west_table_geom (photo_id, cluster_r5000_id, cluster_r10000_id, cluster_r20000_id, cluster_r40000_id, cluster_r80000_id) values (?, ?, ?, ?, ?, ?)");
				pstmtInsert.setString(1, rsSelect.getString("photo_id"));
				pstmtInsert.setString(2, rsSelect.getString("cluster_r5000_id"));
				pstmtInsert.setString(3, rsSelect.getString("cluster_r10000_id"));
				pstmtInsert.setString(4, rsSelect.getString("cluster_r20000_id"));
				pstmtInsert.setString(5, rsSelect.getString("cluster_r40000_id"));
				pstmtInsert.setString(6, rsSelect.getString("cluster_r80000_id"));
				pstmtInsert.executeUpdate();
				pstmtInsert.close();
			}

			connTo.commit();
			rsSelect.close();
			pstmtSelect.close();
		} catch (SQLException e) {
			e.printStackTrace();
			connTo.rollback();
		} finally {
			connTo.close();
			connFrom.close();
		}

	}
}
