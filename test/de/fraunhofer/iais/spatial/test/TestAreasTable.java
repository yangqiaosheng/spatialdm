package de.fraunhofer.iais.spatial.test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

import de.fraunhofer.iais.spatial.dao.jdbc.DB;

public class TestAreasTable {

	public static DB db = new DB();

	@Test
	public void testPerson() throws Exception {
		Connection conn = db.getConn();

		PreparedStatement personStmt = db.getPstmt(conn, "select DISTINCT person from FLICKR_DE_WEST_TABLE");
		ResultSet pset = db.getRs(personStmt);
		while (pset.next()) {
			String person = pset.getString(1);
			PreparedStatement selectStmt = db.getPstmt(conn, "select * from AREAS20KMRADIUS_count");
			ResultSet rset = db.getRs(selectStmt);
			int num = 0;
			while (rset.next()) {
				String count = rset.getString("person");
				if (count != null) {
					Pattern p = Pattern.compile(person + ":(\\d{1,});");
					Matcher m = p.matcher(count);
					if (m.find()) {
						num += Integer.parseInt(m.group(1));
					}
				}
			}

			PreparedStatement fStmt = db.getPstmt(conn, "select count(*) from FLICKR_DE_WEST_TABLE where Person = ?");
			fStmt.setString(1, person);
			ResultSet frs = db.getRs(fStmt);
			frs.next();
			System.out.println(person + ":" + num + ":" + frs.getInt(1));
			assert (num == frs.getInt(1));

			frs.close();
			fStmt.close();

			rset.close();
			selectStmt.close();
		}
		pset.close();
		personStmt.close();
		conn.close();
	}

	@Test
	public void testPersonTotal() throws Exception {
		Connection conn = db.getConn();

		PreparedStatement selectStmt = db.getPstmt(conn, "select * from AREAS20KMRADIUS_count");

		ResultSet rset = db.getRs(selectStmt);
		int num = 0;
		while (rset.next()) {
			String count = rset.getString("person");
			if (count != null) {
				//person:28507282@N03
				Pattern p = Pattern.compile("\\d{6,8}@\\w{3}:(\\d{1,});");
				Matcher m = p.matcher(count);
				while (m.find()) {
					num += Integer.parseInt(m.group(1));
				}
			}
		}

		PreparedStatement fStmt = db.getPstmt(conn, "select count(*) from FLICKR_DE_WEST_TABLE");
		ResultSet frs = db.getRs(fStmt);
		frs.next();
		System.out.println("total:" + num + ":" + frs.getInt(1));
		assert (num == frs.getInt(1));
		frs.close();
		fStmt.close();

		rset.close();
		selectStmt.close();

		conn.close();
	}

	@Test
	public void testHour() throws Exception {
		Connection conn = db.getConn();

		PreparedStatement personStmt = db.getPstmt(conn, "select DISTINCT to_char(dt,'yyyy-MM-dd@HH24') d from FLICKR_DE_WEST_TABLE");
		ResultSet pset = db.getRs(personStmt);
		while (pset.next()) {
			String time = pset.getString(1);
			PreparedStatement selectStmt = db.getPstmt(conn, "select * from AREAS20KMRADIUS_Count");
			ResultSet rset = db.getRs(selectStmt);
			int num = 0;
			while (rset.next()) {
				String count = rset.getString("hour");
				if (count != null) {
					Pattern p = Pattern.compile(time + ":(\\d{1,});");
					Matcher m = p.matcher(count);
					if (m.find()) {
						num += Integer.parseInt(m.group(1));
					}
				}
			}

			PreparedStatement fStmt = db.getPstmt(conn, "select count(*) from FLICKR_DE_WEST_TABLE f where f.dt >= to_date(?,'yyyy-MM-dd@HH24:mi:ss') and f.dt <= to_date(?,'yyyy-MM-dd@HH24:mi:ss')");
			fStmt.setString(1, time + ":00:00");
			fStmt.setString(2, time + ":59:59");
			ResultSet frs = db.getRs(fStmt);
			frs.next();
			System.out.println(time + ":" + num + ":" + frs.getInt(1));
			assert (num == frs.getInt(1));

			frs.close();
			fStmt.close();

			rset.close();
			selectStmt.close();
		}
		pset.close();
		personStmt.close();
		conn.close();
	}

	@Test
	public void testHourTotal() throws Exception {
		Connection conn = db.getConn();

		PreparedStatement selectStmt = db.getPstmt(conn, "select * from AREAS20KMRADIUS_Count");

		ResultSet rset = db.getRs(selectStmt);
		int num = 0;
		while (rset.next()) {
			String count = rset.getString("hour");
			if (count != null) {
				//person:28507282@N03
				Pattern p = Pattern.compile("\\d{4}-\\d{2}-\\d{2}@\\d{2}:(\\d{1,});");
				Matcher m = p.matcher(count);
				while (m.find()) {
					num += Integer.parseInt(m.group(1));
				}
			}
		}

		PreparedStatement fStmt = db.getPstmt(conn, "select count(*) from FLICKR_DE_WEST_TABLE");
		ResultSet frs = db.getRs(fStmt);
		frs.next();
		System.out.println("total:" + num + ":" + frs.getInt(1));
		assert (num == frs.getInt(1));
		frs.close();
		fStmt.close();

		rset.close();
		selectStmt.close();

		conn.close();
	}

	@Test
	public void testDay() throws Exception {
		Connection conn = db.getConn();

		PreparedStatement personStmt = db.getPstmt(conn, "select DISTINCT to_char(dt,'yyyy-MM-dd') d from FLICKR_DE_WEST_TABLE");
		ResultSet pset = db.getRs(personStmt);
		while (pset.next()) {
			String time = pset.getString(1);
			PreparedStatement selectStmt = db.getPstmt(conn, "select * from AREAS20KMRADIUS_Count");
			ResultSet rset = db.getRs(selectStmt);
			int num = 0;
			while (rset.next()) {
				String count = rset.getString("day");
				if (count != null) {
					Pattern p = Pattern.compile(time + ":(\\d{1,});");
					Matcher m = p.matcher(count);
					if (m.find()) {
						num += Integer.parseInt(m.group(1));
					}
				}
			}

			PreparedStatement fStmt = db.getPstmt(conn, "select count(*) from FLICKR_DE_WEST_TABLE f where f.dt >= to_date(?,'yyyy-MM-dd@HH24:mi:ss') and f.dt <= to_date(?,'yyyy-MM-dd@HH24:mi:ss')");
			fStmt.setString(1, time + "@00:00:00");
			fStmt.setString(2, time + "@23:59:59");
			ResultSet frs = db.getRs(fStmt);
			frs.next();
			System.out.println(time + ":" + num + ":" + frs.getInt(1));
			assert (num == frs.getInt(1));

			frs.close();
			fStmt.close();

			rset.close();
			selectStmt.close();
		}
		pset.close();
		personStmt.close();
		conn.close();
	}

	@Test
	public void testDayTotal() throws Exception {
		Connection conn = db.getConn();

		PreparedStatement selectStmt = db.getPstmt(conn, "select * from AREAS20KMRADIUS_Count");

		ResultSet rset = db.getRs(selectStmt);
		int num = 0;
		while (rset.next()) {
			String count = rset.getString("day");
			if (count != null) {
				//person:28507282@N03
				Pattern p = Pattern.compile("\\d{4}-\\d{2}-\\d{2}:(\\d{1,});");
				Matcher m = p.matcher(count);
				while (m.find()) {
					num += Integer.parseInt(m.group(1));
				}
			}
		}

		PreparedStatement fStmt = db.getPstmt(conn, "select count(*) from FLICKR_DE_WEST_TABLE");
		ResultSet frs = db.getRs(fStmt);
		frs.next();
		System.out.println("total:" + num + ":" + frs.getInt(1));
		assert (num == frs.getInt(1));
		frs.close();
		fStmt.close();

		rset.close();
		selectStmt.close();

		conn.close();
	}

	@Test
	public void testMonth() throws Exception {
		Connection conn = db.getConn();

		PreparedStatement personStmt = db.getPstmt(conn, "select DISTINCT to_char(dt,'yyyy-MM') d from FLICKR_DE_WEST_TABLE");
		ResultSet pset = db.getRs(personStmt);
		while (pset.next()) {
			String time = pset.getString(1);
			PreparedStatement selectStmt = db.getPstmt(conn, "select * from AREAS20KMRADIUS_Count");
			ResultSet rset = db.getRs(selectStmt);
			int num = 0;
			while (rset.next()) {
				String count = rset.getString("month");
				if (count != null) {
					Pattern p = Pattern.compile(time + ":(\\d{1,});");
					Matcher m = p.matcher(count);
					if (m.find()) {
						num += Integer.parseInt(m.group(1));
					}
				}
			}

			PreparedStatement fStmt = db.getPstmt(conn, "select count(*) from FLICKR_DE_WEST_TABLE f where f.dt >= to_date(?,'yyyy-MM-dd@HH24:mi:ss') and f.dt <= to_date(?,'yyyy-MM-dd@HH24:mi:ss')");
			fStmt.setString(1, time + "-01@00:00:00");
			fStmt.setString(2, time + "-31@23:59:59");
			ResultSet frs = db.getRs(fStmt);
			frs.next();
			System.out.println(time + ":" + num + ":" + frs.getInt(1));
			assert (num == frs.getInt(1));

			frs.close();
			fStmt.close();

			rset.close();
			selectStmt.close();
		}
		pset.close();
		personStmt.close();
		conn.close();
	}

	@Test
	public void testMonthTotal() throws Exception {
		Connection conn = db.getConn();

		PreparedStatement selectStmt = db.getPstmt(conn, "select * from AREAS20KMRADIUS_Count");

		ResultSet rset = db.getRs(selectStmt);
		int num = 0;
		while (rset.next()) {
			String count = rset.getString("month");
			if (count != null) {
				//person:28507282@N03
				Pattern p = Pattern.compile("\\d{4}-\\d{2}:(\\d{1,});");
				Matcher m = p.matcher(count);
				while (m.find()) {
					num += Integer.parseInt(m.group(1));
				}
			}
		}

		PreparedStatement fStmt = db.getPstmt(conn, "select count(*) from FLICKR_DE_WEST_TABLE");
		ResultSet frs = db.getRs(fStmt);
		frs.next();
		System.out.println("total:" + num + ":" + frs.getInt(1));
		assert (num == frs.getInt(1));
		frs.close();
		fStmt.close();

		rset.close();
		selectStmt.close();

		conn.close();
	}

	@Test
	public void testYear() throws Exception {
		Connection conn = db.getConn();

		PreparedStatement personStmt = db.getPstmt(conn, "select DISTINCT to_char(dt,'yyyy') d from FLICKR_DE_WEST_TABLE");
		ResultSet pset = db.getRs(personStmt);
		while (pset.next()) {
			String time = pset.getString(1);
			PreparedStatement selectStmt = db.getPstmt(conn, "select * from AREAS20KMRADIUS_Count");
			ResultSet rset = db.getRs(selectStmt);
			int num = 0;
			while (rset.next()) {
				String count = rset.getString("year");
				if (count != null) {
					Pattern p = Pattern.compile(time + ":(\\d{1,});");
					Matcher m = p.matcher(count);
					if (m.find()) {
						num += Integer.parseInt(m.group(1));
					}
				}
			}

			PreparedStatement fStmt = db.getPstmt(conn, "select count(*) from FLICKR_DE_WEST_TABLE f where f.dt >= to_date(?,'yyyy-MM-dd@HH24:mi:ss') and f.dt <= to_date(?,'yyyy-MM-dd@HH24:mi:ss')");
			fStmt.setString(1, time + "-01-01@00:00:00");
			fStmt.setString(2, time + "-12-31@23:59:59");
			ResultSet frs = db.getRs(fStmt);
			frs.next();
			System.out.println(time + ":" + num + ":" + frs.getInt(1));
			assert (num == frs.getInt(1));

			frs.close();
			fStmt.close();

			rset.close();
			selectStmt.close();
		}
		pset.close();
		personStmt.close();
		conn.close();
	}

	@Test
	public void testYearTotal() throws Exception {
		Connection conn = db.getConn();

		PreparedStatement selectStmt = db.getPstmt(conn, "select * from AREAS20KMRADIUS_Count");

		ResultSet rset = db.getRs(selectStmt);
		int num = 0;
		while (rset.next()) {
			String count = rset.getString("year");
			if (count != null) {
				//person:28507282@N03
				Pattern p = Pattern.compile("\\d{4}:(\\d{1,});");
				Matcher m = p.matcher(count);
				while (m.find()) {
					num += Integer.parseInt(m.group(1));
				}
			}
		}

		PreparedStatement fStmt = db.getPstmt(conn, "select count(*) from FLICKR_DE_WEST_TABLE");
		ResultSet frs = db.getRs(fStmt);
		frs.next();
		System.out.println("total:" + num + ":" + frs.getInt(1));
		assert (num == frs.getInt(1));
		frs.close();
		fStmt.close();

		rset.close();
		selectStmt.close();

		conn.close();
	}
}
