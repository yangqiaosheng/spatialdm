package de.fraunhofer.iais.spatial.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.fraunhofer.iais.spatial.dao.jdbc.DB;

import oracle.spatial.geometry.JGeometry;
import oracle.sql.STRUCT;

public class JoinAreaCount {

	DB db = new DB();

	public static void main(String[] args) throws Exception {
		JoinAreaCount t = new JoinAreaCount();
		//		 t.select();
		//		t.countPerson();
		//		t.listCalendar();
		//		t.countTotal();
		//		t.insert("test", "2", "AREAS20KMRADIUS_HOUR");
	}

	private void select() throws Exception {

		Connection conn = db.getConn();

		PreparedStatement selectStmt = db.getPstmt(conn, "select * from AREAS20KMRADIUS_Person");

		ResultSet rset = db.getRs(selectStmt);

		while (rset.next()) {

			System.out.println(rset.getString("count"));
		}
		rset.close();
		selectStmt.close();
		conn.close();
	}

	private void countPerson() throws Exception {
		Connection conn = db.getConn();
		PreparedStatement selectAreaStmt = db.getPstmt(conn, "select * from areas20kmradius a");
		ResultSet selectAreaRs = db.getRs(selectAreaStmt);
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

			PreparedStatement selectFlickrStmt = db.getPstmt(conn, "select DISTINCT person from FLICKR_DE_WEST_TABLE where longitude > ? and longitude < ? and latitude > ? and latitude < ?");
			selectFlickrStmt.setDouble(1, mbrX1);
			selectFlickrStmt.setDouble(2, mbrX2);
			selectFlickrStmt.setDouble(3, mbrY1);
			selectFlickrStmt.setDouble(4, mbrY2);
			ResultSet selectFlickrRs = db.getRs(selectFlickrStmt);
			for (int j = 0; selectFlickrRs.next(); j++) {

				PreparedStatement joinStmt = db.getPstmt(conn, "select count(*) num "
						+ "from areas20kmradius t, (select * from flickr_de_west_table f where f.longitude > ? and f.longitude < ? and f.latitude > ? and f.latitude < ? and  f.person=?) f2"
						+ " WHERE t.id=?  and sdo_relate(t.geom, SDO_geometry(2001,8307,SDO_POINT_TYPE(f2.longitude, f2.latitude, NULL),NULL,NULL),'mask=anyinteract,querytype=join') = 'TRUE'");
				String person = selectFlickrRs.getString("person");
				joinStmt.setDouble(1, mbrX1);
				joinStmt.setDouble(2, mbrX2);
				joinStmt.setDouble(3, mbrY1);
				joinStmt.setDouble(4, mbrY2);
				joinStmt.setString(5, person);
				joinStmt.setString(6, areaId);
				ResultSet joinRs = db.getRs(joinStmt);

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
			}
			System.out.println("i:" + i);
			System.out.println(personCount);
			insert(personCount.toString(), areaId, "AREAS20KMRADIUS_PERSON");
			selectFlickrRs.close();
			selectFlickrStmt.close();

		}

		selectAreaRs.close();
		selectAreaStmt.close();
		conn.close();
	}

	private void insert(String count, String id, String table) throws Exception {
		Connection conn = db.getConn();
		PreparedStatement insertStmt = db.getPstmt(conn, "insert into " + table + " (id,name,count) values (?, ?, ?)");
		insertStmt.setString(1, id);
		insertStmt.setString(2, id);
		insertStmt.setString(3, count);
		insertStmt.executeUpdate();
		insertStmt.close();
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

	private void countHour() throws Exception {
		Connection conn = db.getConn();
		PreparedStatement selectAreaStmt = db.getPstmt(conn, "select * from areas20kmradius a");
		ResultSet selectAreaRs = db.getRs(selectAreaStmt);
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

			PreparedStatement selectFlickrStmt = db.getPstmt(conn, "select DISTINCT to_char(dt,'yyyy-MM-dd@HH24') d from FLICKR_DE_WEST_TABLE where longitude > ? and longitude < ? and latitude > ? and latitude < ? order by d");
			selectFlickrStmt.setDouble(1, mbrX1);
			selectFlickrStmt.setDouble(2, mbrX2);
			selectFlickrStmt.setDouble(3, mbrY1);
			selectFlickrStmt.setDouble(4, mbrY2);
			ResultSet selectFlickrRs = db.getRs(selectFlickrStmt);

			for (int j = 0; selectFlickrRs.next(); j++) {
				String date = selectFlickrRs.getString("d");
				PreparedStatement joinStmt = db
						.getPstmt(
								conn,
								"select count(*) num "
										+ "from areas20kmradius t, (select * from flickr_de_west_table f where f.longitude > ? and f.longitude < ? and f.latitude > ? and f.latitude < ? and  f.dt >= to_date(?,'yyyy-MM-dd@HH24:mi:ss') and f.dt <= to_date(?,'yyyy-MM-dd@HH24:mi:ss')) f2"
										+ " WHERE t.id=?  and sdo_relate(t.geom, SDO_geometry(2001,8307,SDO_POINT_TYPE(f2.longitude, f2.latitude, NULL),NULL,NULL),'mask=anyinteract,querytype=join') = 'TRUE'");
				joinStmt.setDouble(1, mbrX1);
				joinStmt.setDouble(2, mbrX2);
				joinStmt.setDouble(3, mbrY1);
				joinStmt.setDouble(4, mbrY2);
				joinStmt.setString(5, date + ":00:00");
				joinStmt.setString(6, date + ":59:59");
				joinStmt.setString(7, areaId);
				ResultSet joinRs = db.getRs(joinStmt);
				if (joinRs.next()) {
					int num = joinRs.getInt("num");
					i += num;
					String info = date + ":" + num + ";";
					System.out.println(info);
					if (num != 0) {
						hourCount.append(info);
					}
				}
				joinRs.close();
				joinStmt.close();
			}
			System.out.println("i:" + i);
			System.out.println(hourCount);
			insert(hourCount.toString(), areaId, "AREAS20KMRADIUS_HOUR");
			selectFlickrRs.close();
			selectFlickrStmt.close();
		}

		selectAreaRs.close();
		selectAreaStmt.close();
		conn.close();
	}

	private void countDay() throws Exception {
		Connection conn = db.getConn();

		PreparedStatement personStmt = db.getPstmt(conn, "select * from AREAS20KMRADIUS_Count");
		ResultSet pset = db.getRs(personStmt);

		while (pset.next()) {
			String id = pset.getString("id");
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
				PreparedStatement iStmt = db.getPstmt(conn, "update areas20kmradius_count set day = ? where id = ?");
				iStmt.setString(1, dayStr.toString());
				iStmt.setString(2, id);
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
		Connection conn = db.getConn();

		PreparedStatement personStmt = db.getPstmt(conn, "select * from AREAS20KMRADIUS_Count");
		ResultSet pset = db.getRs(personStmt);

		while (pset.next()) {
			String id = pset.getString("id");
			String hourStr = pset.getString("hour");
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
				PreparedStatement iStmt = db.getPstmt(conn, "update areas20kmradius_count set month = ? where id = ?");
				iStmt.setString(1, monthStr.toString());
				iStmt.setString(2, id);
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
		Connection conn = db.getConn();

		PreparedStatement personStmt = db.getPstmt(conn, "select * from AREAS20KMRADIUS_Count");
		ResultSet pset = db.getRs(personStmt);

		while (pset.next()) {
			String id = pset.getString("id");
			String hourStr = pset.getString("hour");
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
				PreparedStatement iStmt = db.getPstmt(conn, "update areas20kmradius_count set year = ? where id = ?");
				iStmt.setString(1, yearStr.toString());
				iStmt.setString(2, id);
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
		Connection conn = db.getConn();

		PreparedStatement personStmt = db.getPstmt(conn, "select * from AREAS20KMRADIUS_Count");
		ResultSet pset = db.getRs(personStmt);

		while (pset.next()) {
			String id = pset.getString("id");
			String hourStr = pset.getString("hour");
			if (hourStr != null) {
				//person:28507282@N03
				Pattern p = Pattern.compile("\\d{4}-\\d{2}-\\d{2}@\\d{2}:(\\d{1,});");
				Matcher m = p.matcher(hourStr);
				int hour = 0;
				while (m.find()) {
					hour += Integer.parseInt(m.group(1));
				}

				PreparedStatement iStmt = db.getPstmt(conn, "update areas20kmradius_count set total = ? where id = ?");
				iStmt.setInt(1, hour);
				iStmt.setString(2, id);
				iStmt.executeUpdate();
				iStmt.close();
			}
		}
		pset.close();
		personStmt.close();
		conn.close();
	}
}
