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
import de.fraunhofer.iais.spatial.dao.FlickrDeWestAreaDao;
import de.fraunhofer.iais.spatial.dto.FlickrDeWestAreaDto.QueryLevel;

public class JoinFlickrEuropeAreaCount {
	static long start = System.currentTimeMillis();
	static int count = 0;
	DBUtil db = new DBUtil();

	public static void main(String[] args) throws Exception {
		JoinFlickrEuropeAreaCount t = new JoinFlickrEuropeAreaCount();

		ArrayList<String> radiusList = new ArrayList<String>();
		radiusList.add("5000");
		radiusList.add("10000");
		radiusList.add("20000");
		radiusList.add("40000");
		radiusList.add("80000");
		radiusList.add("160000");
		radiusList.add("320000");
		t.groupPhotos(radiusList);
//		for (String radius : radiusList) {
//			t.count(QueryLevel.HOUR, radius);
//			t.count(QueryLevel.DAY, radius);
//			t.count(QueryLevel.MONTH, radius);
//			t.count(QueryLevel.YEAR, radius);
//			t.countPeople(radius);
//			t.countTotal();
//		}

//		t.countPeople("5000");
//		t.countTotal();

//				t.countDay();
//				t.countMonth();
//				t.countYear();
//				t.countTotal();
		//
		//		t.countPerson("80000");
		//		t.countPerson("40000");
		//		t.countPerson("20000");
		//		t.countPerson("10000");
		//		t.countPerson("5000");

//				t.groupPhotos();

//		t.copyTable();

		System.out.println("escaped time:" + (System.currentTimeMillis() - start) / 1000.0);
		//
		//		t.update("Haolin",  "FLICKR_DE_WEST_TABLE_COUNT", "PERSON", "1   ", "5000");
		//		t.select();
		//		t.listCalendar();
		//		t.countTotal();
		//				t.insert("Haolin",  "FLICKR_DE_WEST_TABLE_COUNT", "PERSON", "115", radiusString);
	}

	private void count(QueryLevel queryLevel, String radiusString) throws Exception {
		String oracleDatePatternStr = judgeOracleDatePatternStr(queryLevel);
		Connection conn = db.getConn();
		PreparedStatement selectAreaStmt = db.getPstmt(conn, "select distinct(t.region_" + radiusString + "_id) id from day_flickr_europe e, day_flickr_europe_geom t" + " where e.photo_id = t.photo_id");
		ResultSet selectAreaRs = db.getRs(selectAreaStmt);
		int areaId = -1;
		int i = 0;
		while (selectAreaRs.next()) {
			StringBuffer count = new StringBuffer();
			areaId = selectAreaRs.getInt("id");

			PreparedStatement selectFlickrStmt = db.getPstmt(conn, "select DISTINCT(to_char(dt,?)) d from day_flickr_europe_photo p" +
					" where p.region_" + radiusString
					+ "_id = ? and p.dt >= to_date('2005-01-01','yyyy-MM-dd')" + " order by d");
			selectFlickrStmt.setString(1, oracleDatePatternStr);
			selectFlickrStmt.setInt(2, areaId);

			ResultSet selectFlickrRs = db.getRs(selectFlickrStmt);

			for (int j = 0; selectFlickrRs.next(); j++) {
				String date = selectFlickrRs.getString("d");
				PreparedStatement joinStmt = db.getPstmt(conn, "select count(*) num from day_flickr_europe_photo p"
						+ " where p.region_" + radiusString + "_id = ? and"
						+ " p.dt >= to_date(?,?) and p.dt < to_date(?,?) + interval '1' " + queryLevel.toString());

				joinStmt.setInt(1, areaId);
				joinStmt.setString(2, date);
				joinStmt.setString(3, oracleDatePatternStr);
				joinStmt.setString(4, date);
				joinStmt.setString(5, oracleDatePatternStr);
				ResultSet joinRs = db.getRs(joinStmt);
				if (joinRs.next()) {
					int num = joinRs.getInt("num");
					i += num;
					String info = date + ":" + num + ";";
					System.out.println("area id:" + areaId + "|" + info);
					if (num != 0) {
						count.append(info);
					}
				}
				System.out.println(queryLevel + " i:" + i + " radius: " + radiusString + " escaped time:" + (System.currentTimeMillis() - start) / 1000.0);
				joinRs.close();
				joinStmt.close();
			}
			System.out.println(count);
			if (queryLevel == QueryLevel.HOUR) {
				insert(count.toString(), "DAY_FLICKR_EUROPE_COUNT", queryLevel.toString(), areaId, radiusString);
			} else {
				update(count.toString(), "DAY_FLICKR_EUROPE_COUNT", queryLevel.toString(), areaId, radiusString);
			}

			selectFlickrRs.close();
			selectFlickrStmt.close();
		}

		selectAreaRs.close();
		selectAreaStmt.close();
		conn.close();
	}

	private void countPeople(String radiusString) throws Exception {
		Connection conn = db.getConn();
		PreparedStatement selectAreaStmt = db.getPstmt(conn, "select distinct(p.region_" + radiusString + "_id) id from DAY_FLICKR_EUROPE_PHOTO p");
		ResultSet selectAreaRs = db.getRs(selectAreaStmt);
		int areaId = -1;
		while (selectAreaRs.next()) {
			StringBuffer count = new StringBuffer();
			areaId = selectAreaRs.getInt("id");

			PreparedStatement selectFlickrStmt = db.getPstmt(conn, "select p.person person, count(*) num  from DAY_FLICKR_EUROPE_PHOTO p"
					+ " where p.region_" + radiusString	+ "_id = ? and p.dt >= to_date('2005-01-01','yyyy-MM-dd')" + " group by p.person");
			selectFlickrStmt.setInt(1, areaId);

			ResultSet selectFlickrRs = db.getRs(selectFlickrStmt);

			while (selectFlickrRs.next()) {
				String person = selectFlickrRs.getString("person");
				int num = selectFlickrRs.getInt("num");
				String info = person + ":" + num + ";";
				System.out.println("area id:" + areaId + "|" + info);
				if (num != 0) {
					count.append(info);
				}
				System.out.println("person:" + person + " |radius: " + radiusString + " escaped time:" + (System.currentTimeMillis() - start) / 1000.0);
			}
			System.out.println(count);
			update(count.toString(), "DAY_FLICKR_EUROPE_COUNT", "PERSON", areaId, radiusString);

			selectFlickrRs.close();
			selectFlickrStmt.close();
		}

		selectAreaRs.close();
		selectAreaStmt.close();
		conn.close();
	}

	private void countPersonOld(String radiusString) throws Exception {
		Connection conn = db.getConn();
		PreparedStatement selectAreaStmt = db.getPstmt(conn, "select * from FLICKR_DE_WEST_TABLE_" + radiusString + " a");
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

				PreparedStatement joinStmt = db.getPstmt(conn, "select count(*) num " + "from FLICKR_DE_WEST_TABLE_" + radiusString
						+ " t, (select * from flickr_de_west_table f where f.longitude > ? and f.longitude < ? and f.latitude > ? and f.latitude < ? and  f.person=?) f2"
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
				System.out.println("person i:" + i + " radius: " + radiusString + " escaped time:" + (System.currentTimeMillis() - start) / 1000.0);
			}
			System.out.println("i:" + i);
			System.out.println(personCount);
//			update(personCount.toString(), "DAY_FLICKR_EUROPE_COUNT", "PERSON", areaId, radiusString);
			selectFlickrRs.close();
			selectFlickrStmt.close();
		}

		selectAreaRs.close();
		selectAreaStmt.close();
		conn.close();
	}


	private void countDay() throws Exception {
		Connection conn = db.getConn();

		PreparedStatement personStmt = db.getPstmt(conn, "select * from DAY_FLICKR_EUROPE_COUNT");
		ResultSet pset = db.getRs(personStmt);

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
				PreparedStatement iStmt = db.getPstmt(conn, "update DAY_FLICKR_EUROPE_COUNT set day = ? where id = ? and radius = ?");
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
		Connection conn = db.getConn();

		PreparedStatement personStmt = db.getPstmt(conn, "select * from DAY_FLICKR_EUROPE_COUNT");
		ResultSet pset = db.getRs(personStmt);

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
				PreparedStatement iStmt = db.getPstmt(conn, "update DAY_FLICKR_EUROPE_COUNT set month = ? where id = ? and radius = ?");
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
		Connection conn = db.getConn();

		PreparedStatement personStmt = db.getPstmt(conn, "select * from DAY_FLICKR_EUROPE_COUNT");
		ResultSet pset = db.getRs(personStmt);

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
				PreparedStatement iStmt = db.getPstmt(conn, "update DAY_FLICKR_EUROPE_COUNT set year = ? where id = ? and radius = ?");
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
		Connection conn = db.getConn();

		PreparedStatement personStmt = db.getPstmt(conn, "select * from DAY_FLICKR_EUROPE_COUNT");
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

				PreparedStatement iStmt = db.getPstmt(conn, "update DAY_FLICKR_EUROPE_COUNT set total = ? where id = ? and radius = ?");
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

	private void countTotalNew() throws Exception {
		Connection conn = db.getConn();

		PreparedStatement personStmt = db.getPstmt(conn, "select * from day_flickr_europe_count");
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

				PreparedStatement iStmt = db.getPstmt(conn, "update DAY_FLICKR_EUROPE_COUNT set total = ? where id = ? and radius = ?");
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

	private void insert(String count, String table, String column, int id, String radius) throws Exception {
		Connection conn = db.getConn();
		PreparedStatement insertStmt = db.getPstmt(conn, "insert into " + table + " (id, radius, " + column + ") values (?, ?, ?)");
		insertStmt.setInt(1, id);
		insertStmt.setString(2, radius);
		insertStmt.setString(3, count);
		insertStmt.executeUpdate();
		insertStmt.close();
		conn.close();
	}

	private void update(String count, String table, String column, int id, String radius) throws Exception {
		Connection conn = db.getConn();
		PreparedStatement insertStmt = db.getPstmt(conn, "update " + table + " set " + column + " = ? where id = ? and radius = ?");
		insertStmt.setString(1, count);
		insertStmt.setInt(2, id);
		insertStmt.setString(3, radius);

		insertStmt.executeUpdate();
		insertStmt.close();
		conn.close();
	}

	private void select() throws Exception {

		Connection conn = db.getConn();
		PreparedStatement selectStmt = db.getPstmt(conn, "select * from FLICKR_DE_WEST_TABLE_count");
		ResultSet rset = db.getRs(selectStmt);

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

	private void groupPhotos(List<String> radiuses) throws SQLException {
		int i = 0;

		Connection conn = db.getConn();
		PreparedStatement pstmt1 = db.getPstmt(conn, "select p.PHOTO_ID, LONGITUDE, LATITUDE from FLICKR_EUROPE p where p.REGION_CHECKED = 0");
		ResultSet rs1 = db.getRs(pstmt1);
		while (rs1.next()) {
			double x = rs1.getDouble("LONGITUDE");
			double y = rs1.getDouble("LATITUDE");
			for (String radius : radiuses) {
				PreparedStatement pstmt2 = db.getPstmt(conn, "select ID from FLICKR_EUROPE_AREA_" + radius + " c, user_sdo_geom_metadata m" + " WHERE m.table_name = 'FLICKR_EUROPE_AREA_" + radius
						+ "' and sdo_relate(c.geom, SDO_geometry(2001,8307,SDO_POINT_TYPE(?, ?, NULL),NULL,NULL),'mask=anyinteract') = 'TRUE'");
				pstmt2.setDouble(1, x);
				pstmt2.setDouble(2, y);
				ResultSet rs2 = db.getRs(pstmt2);
				if (rs2.next()) {
					PreparedStatement pstmt3 = db.getPstmt(conn, "update FLICKR_EUROPE p set p.REGION_CHECKED = 1 , p.REGION_" + radius + "_ID = ? where p.PHOTO_ID = ?");
					pstmt3.setString(1, rs2.getString("ID"));
					pstmt3.setString(2, rs1.getString("PHOTO_ID"));
					pstmt3.executeUpdate();
					db.close(pstmt3);
				}
				db.close(pstmt2);
				db.close(rs2);
			}
			System.out.println("i:" + (i++) + " escaped time:" + (System.currentTimeMillis() - start) / 1000.0);
		}
		db.close(rs1);
		db.close(pstmt1);
		db.close(conn);
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

	protected static String judgeOracleDatePatternStr(QueryLevel queryLevel) {
		String oracleDatePatternStr = null;

		switch (queryLevel) {
		case YEAR:
			oracleDatePatternStr = FlickrDeWestAreaDao.oracleYearPatternStr;
			break;
		case MONTH:
			oracleDatePatternStr = FlickrDeWestAreaDao.oracleMonthPatternStr;
			break;
		case DAY:
			oracleDatePatternStr = FlickrDeWestAreaDao.oracleDayPatternStr;
			break;
		case HOUR:
			oracleDatePatternStr = FlickrDeWestAreaDao.oracleHourPatternStr;
			break;
		}
		return oracleDatePatternStr;
	}
}
