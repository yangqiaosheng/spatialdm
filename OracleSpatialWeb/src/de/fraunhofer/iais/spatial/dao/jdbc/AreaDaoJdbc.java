package de.fraunhofer.iais.spatial.dao.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.spatial.geometry.JGeometry;
import oracle.sql.STRUCT;
import de.fraunhofer.iais.spatial.dao.AreaDao;
import de.fraunhofer.iais.spatial.entity.Area;
import de.fraunhofer.iais.spatial.util.DB;

public class AreaDaoJdbc implements AreaDao {

	@Override
	public int getPersonCount(String areaid, String person) {
		Connection conn = DB.getConn();
		PreparedStatement selectStmt = null;
		ResultSet rs = null;
		int num = 0;
		try {
			selectStmt = DB.getPstmt(conn, "select person from AREAS20KMRADIUS_COUNT where id = ?");
			selectStmt.setString(1, areaid);
			rs = DB.getRs(selectStmt);
			if (rs.next()) {
				String count = rs.getString("person");
				if (count != null) {
					Pattern p = Pattern.compile(person + ":(\\d{1,});");
					Matcher m = p.matcher(count);
					if (m.find()) {
						num += Integer.parseInt(m.group(1));
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			DB.close(rs);
			DB.close(selectStmt);
			DB.close(conn);
		}
		return num;
	}

	@Override
	public int getHourCount(String areaid, String hour) {
		Connection conn = DB.getConn();
		PreparedStatement selectStmt = null;
		ResultSet rs = null;
		int num = 0;
		try {
			selectStmt = DB.getPstmt(conn, "select hour from AREAS20KMRADIUS_COUNT where id = ?");
			selectStmt.setString(1, areaid);
			rs = DB.getRs(selectStmt);
			if (rs.next()) {
				String count = rs.getString("hour");
				if (count != null) {
					Pattern p = Pattern.compile(hour + ":(\\d{1,});");
					Matcher m = p.matcher(count);
					if (m.find()) {
						num += Integer.parseInt(m.group(1));
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			DB.close(rs);
			DB.close(selectStmt);
			DB.close(conn);
		}
		return num;
	}

	@Override
	public int getHourCount(String areaid, Set<String> hours) {
		Connection conn = DB.getConn();
		PreparedStatement selectStmt = null;
		ResultSet rs = null;
		int num = 0;
		try {
			selectStmt = DB.getPstmt(conn, "select hour from AREAS20KMRADIUS_COUNT where id = ?");
			selectStmt.setString(1, areaid);
			rs = DB.getRs(selectStmt);
			if (rs.next()) {
				String count = rs.getString("hour");
				if (count != null) {
					Pattern p = Pattern.compile("(\\d{4}-\\d{2}-\\d{2}@\\d{2}):(\\d{1,});");
					Matcher m = p.matcher(count);
					while (m.find()) {
						if (hours.contains(m.group(1))) {
							num += Integer.parseInt(m.group(2));
						}
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			DB.close(rs);
			DB.close(selectStmt);
			DB.close(conn);
		}
		return num;
	}

	private void loadHoursCount(Area a) {
		Connection conn = DB.getConn();
		PreparedStatement selectStmt = null;
		ResultSet rs = null;

		Map<String, Integer> hoursCount = new LinkedHashMap<String, Integer>();
		a.setHoursCount(hoursCount);

		try {
			selectStmt = DB.getPstmt(conn, "select hour from AREAS20KMRADIUS_COUNT where id = ?");
			selectStmt.setString(1, a.getId());
			rs = DB.getRs(selectStmt);
			if (rs.next()) {
				String count = rs.getString("hour");
				if (count != null) {
					Pattern p = Pattern.compile("(\\d{4}-\\d{2}-\\d{2}@\\d{2}):(\\d{1,});");
					Matcher m = p.matcher(count);
					while (m.find()) {
						hoursCount.put(m.group(1), Integer.parseInt(m.group(2)));
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			DB.close(rs);
			DB.close(selectStmt);
			DB.close(conn);
		}
	}

	@Override
	public int getDayCount(String areaid, String day) {
		Connection conn = DB.getConn();
		PreparedStatement selectStmt = null;
		ResultSet rs = null;
		int num = 0;
		try {
			selectStmt = DB.getPstmt(conn, "select day from AREAS20KMRADIUS_COUNT where id = ?");
			selectStmt.setString(1, areaid);
			rs = DB.getRs(selectStmt);
			if (rs.next()) {
				String count = rs.getString("day");
				if (count != null) {
					Pattern p = Pattern.compile(day + ":(\\d{1,});");
					Matcher m = p.matcher(count);
					if (m.find()) {
						num += Integer.parseInt(m.group(1));
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			DB.close(rs);
			DB.close(selectStmt);
			DB.close(conn);
		}
		return num;
	}

	@Override
	public int getDayCount(String areaid, Set<String> days) {
		Connection conn = DB.getConn();
		PreparedStatement selectStmt = null;
		ResultSet rs = null;
		int num = 0;
		try {
			selectStmt = DB.getPstmt(conn, "select day from AREAS20KMRADIUS_COUNT where id = ?");
			selectStmt.setString(1, areaid);
			rs = DB.getRs(selectStmt);
			if (rs.next()) {
				String count = rs.getString("day");
				if (count != null) {
					Pattern p = Pattern.compile("(\\d{4}-\\d{2}-\\d{2}):(\\d{1,});");
					Matcher m = p.matcher(count);
					while (m.find()) {
						if (days.contains(m.group(1))) {
							num += Integer.parseInt(m.group(2));
						}
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			DB.close(rs);
			DB.close(selectStmt);
			DB.close(conn);
		}
		return num;
	}

	private void loadDaysCount(Area a) {
		Connection conn = DB.getConn();
		PreparedStatement selectStmt = null;
		ResultSet rs = null;

		Map<String, Integer> daysCount = new LinkedHashMap<String, Integer>();
		a.setDaysCount(daysCount);

		try {
			selectStmt = DB.getPstmt(conn, "select day from AREAS20KMRADIUS_COUNT where id = ?");
			selectStmt.setString(1, a.getId());
			rs = DB.getRs(selectStmt);
			if (rs.next()) {
				String count = rs.getString("day");
				if (count != null) {
					Pattern p = Pattern.compile("(\\d{4}-\\d{2}-\\d{2}):(\\d{1,});");
					Matcher m = p.matcher(count);
					while (m.find()) {
						daysCount.put(m.group(1), Integer.parseInt(m.group(2)));
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			DB.close(rs);
			DB.close(selectStmt);
			DB.close(conn);
		}
	}

	@Override
	public int getMonthCount(String areaid, String month) {
		Connection conn = DB.getConn();
		PreparedStatement selectStmt = null;
		ResultSet rs = null;
		int num = 0;
		try {
			selectStmt = DB.getPstmt(conn, "select month from AREAS20KMRADIUS_COUNT where id = ?");
			selectStmt.setString(1, areaid);
			rs = DB.getRs(selectStmt);
			if (rs.next()) {
				String count = rs.getString("month");
				if (count != null) {
					Pattern p = Pattern.compile(month + ":(\\d{1,});");
					Matcher m = p.matcher(count);
					if (m.find()) {
						num += Integer.parseInt(m.group(1));
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			DB.close(rs);
			DB.close(selectStmt);
			DB.close(conn);
		}
		return num;
	}

	@Override
	public int getMonthCount(String areaid, Set<String> months) {
		Connection conn = DB.getConn();
		PreparedStatement selectStmt = null;
		ResultSet rs = null;
		int num = 0;
		try {
			selectStmt = DB.getPstmt(conn, "select month from AREAS20KMRADIUS_COUNT where id = ?");
			selectStmt.setString(1, areaid);
			rs = DB.getRs(selectStmt);
			if (rs.next()) {
				String count = rs.getString("month");
				if (count != null) {
					Pattern p = Pattern.compile("(\\d{4}-\\d{2}):(\\d{1,});");
					Matcher m = p.matcher(count);
					while (m.find()) {
						if (months.contains(m.group(1))) {
							num += Integer.parseInt(m.group(2));
						}
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			DB.close(rs);
			DB.close(selectStmt);
			DB.close(conn);
		}
		return num;
	}

	private void loadMonthsCount(Area a) {
		Connection conn = DB.getConn();
		PreparedStatement selectStmt = null;
		ResultSet rs = null;

		Map<String, Integer> monthsCount = new LinkedHashMap<String, Integer>();
		a.setMonthsCount(monthsCount);

		try {
			selectStmt = DB.getPstmt(conn, "select month from AREAS20KMRADIUS_COUNT where id = ?");
			selectStmt.setString(1, a.getId());
			rs = DB.getRs(selectStmt);
			if (rs.next()) {
				String count = rs.getString("month");
				if (count != null) {
					Pattern p = Pattern.compile("(\\d{4}-\\d{2}):(\\d{1,});");
					Matcher m = p.matcher(count);
					while (m.find()) {
						monthsCount.put(m.group(1), Integer.parseInt(m.group(2)));
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			DB.close(rs);
			DB.close(selectStmt);
			DB.close(conn);
		}
	}

	@Override
	public int getYearCount(String areaid, String year) {
		Connection conn = DB.getConn();
		PreparedStatement selectStmt = null;
		ResultSet rs = null;
		int num = 0;
		try {
			selectStmt = DB.getPstmt(conn, "select year from AREAS20KMRADIUS_COUNT where id = ?");
			selectStmt.setString(1, areaid);
			rs = DB.getRs(selectStmt);
			if (rs.next()) {
				String count = rs.getString("year");
				if (count != null) {
					Pattern p = Pattern.compile(year + ":(\\d{1,});");
					Matcher m = p.matcher(count);
					if (m.find()) {
						num += Integer.parseInt(m.group(1));
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			DB.close(rs);
			DB.close(selectStmt);
			DB.close(conn);
		}
		return num;
	}

	@Override
	public int getYearCount(String areaid, Set<String> years) {
		Connection conn = DB.getConn();
		PreparedStatement selectStmt = null;
		ResultSet rs = null;
		int num = 0;
		try {
			selectStmt = DB.getPstmt(conn, "select year from AREAS20KMRADIUS_COUNT where id = ?");
			selectStmt.setString(1, areaid);
			rs = DB.getRs(selectStmt);
			if (rs.next()) {
				String count = rs.getString("year");
				if (count != null) {
					Pattern p = Pattern.compile("(\\d{4}):(\\d{1,});");
					Matcher m = p.matcher(count);
					while (m.find()) {
						if (years.contains(m.group(1))) {
							num += Integer.parseInt(m.group(2));
						}
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			DB.close(rs);
			DB.close(selectStmt);
			DB.close(conn);
		}
		return num;
	}

	private void loadYearsCount(Area a) {
		Connection conn = DB.getConn();
		PreparedStatement selectStmt = null;
		ResultSet rs = null;

		Map<String, Integer> yearsCount = new LinkedHashMap<String, Integer>();
		a.setYearsCount(yearsCount);

		try {
			selectStmt = DB.getPstmt(conn, "select year from AREAS20KMRADIUS_COUNT where id = ?");
			selectStmt.setString(1, a.getId());
			rs = DB.getRs(selectStmt);
			if (rs.next()) {
				String count = rs.getString("year");
				if (count != null) {
					Pattern p = Pattern.compile("(\\d{4}):(\\d{1,});");
					Matcher m = p.matcher(count);
					while (m.find()) {
						yearsCount.put(m.group(1), Integer.parseInt(m.group(2)));
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			DB.close(rs);
			DB.close(selectStmt);
			DB.close(conn);
		}
	}

	@Override
	public int getTotalCount(String areaid) {
		Connection conn = DB.getConn();
		PreparedStatement selectStmt = null;
		ResultSet rs = null;
		int num = 0;
		try {
			selectStmt = DB.getPstmt(conn, "select total from AREAS20KMRADIUS_COUNT where id = ?");
			selectStmt.setString(1, areaid);
			rs = DB.getRs(selectStmt);
			if (rs.next()) {
				num = rs.getInt("total");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			DB.close(rs);
			DB.close(selectStmt);
			DB.close(conn);
		}
		return num;
	}

	@Override
	public List<Area> getAllAreas() {
		List<Area> as = new ArrayList<Area>();
		Connection conn = DB.getConn();
		PreparedStatement pstmt = DB.getPstmt(conn, "select ID, NAME, GEOM, SDO_GEOM.SDO_AREA(c.geom, 0.005) as area, SDO_GEOM.SDO_CENTROID(c.geom, m.diminfo) as center" + " from AREAS20KMRADIUS c, user_sdo_geom_metadata m"
				+ " where m.table_name = 'AREAS20KMRADIUS'");
		ResultSet rs = DB.getRs(pstmt);

		try {
			while (rs.next()) {
				Area a = new Area();
				initFromRs(a, rs);
				as.add(a);
			}
			initAreas(as);
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			DB.close(rs);
			DB.close(pstmt);
			DB.close(conn);
		}
		return as;
	}

	@Override
	public Area getAreaById(String id) {
		Area a = new Area();
		Connection conn = DB.getConn();
		PreparedStatement pstmt = DB.getPstmt(conn, "select ID, NAME, GEOM, SDO_GEOM.SDO_AREA(c.geom, 0.005) as area, SDO_GEOM.SDO_CENTROID(c.geom, m.diminfo) as center" + " from AREAS20KMRADIUS c, user_sdo_geom_metadata m" + " WHERE c.ID = ?");

		ResultSet rs = null;
		try {
			pstmt.setString(1, id);
			rs = DB.getRs(pstmt);
			while (rs.next()) {
				initFromRs(a, rs);
			}
			initArea(a);
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			DB.close(rs);
			DB.close(pstmt);
			DB.close(conn);
		}
		return a;
	}

	@Override
	public List<Area> getAreasByPoint(double x, double y) {
		List<Area> as = new ArrayList<Area>();
		Connection conn = DB.getConn();
		PreparedStatement pstmt = DB.getPstmt(conn, "select ID, NAME, GEOM, SDO_GEOM.SDO_AREA(c.geom, 0.005) as area, SDO_GEOM.SDO_CENTROID(c.geom, m.diminfo) as center" + " from AREAS20KMRADIUS c, user_sdo_geom_metadata m"
				+ " WHERE m.table_name = 'AREAS20KMRADIUS' and sdo_relate(c.geom, SDO_geometry(2001,8307,SDO_POINT_TYPE(?, ?, NULL),NULL,NULL),'mask=anyinteract') = 'TRUE'");

		ResultSet rs = null;

		try {
			pstmt.setDouble(1, x);
			pstmt.setDouble(2, y);
			rs = DB.getRs(pstmt);
			while (rs.next()) {
				Area a = new Area();
				initFromRs(a, rs);
				as.add(a);
			}
			initAreas(as);
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			DB.close(rs);
			DB.close(pstmt);
			DB.close(conn);
		}
		return as;
	}

	@Override
	public List<Area> getAreasByRect(double x1, double y1, double x2, double y2) {
		List<Area> as = new ArrayList<Area>();
		Connection conn = DB.getConn();
		PreparedStatement pstmt = DB.getPstmt(conn, "" + "select ID, NAME, GEOM, CLUSTERING_OF_SOM_CELL, SDO_GEOM.SDO_AREA(c.geom, 0.005) as area, SDO_GEOM.SDO_CENTROID(c.geom, m.diminfo) as center"
				+ " from AREAS20KMRADIUS c, user_sdo_geom_metadata m"
				+ " WHERE m.table_name = 'AREAS20KMRADIUS' and sdo_relate(c.geom, SDO_geometry(2003,8307,NULL,SDO_elem_info_array(1,1003,3), SDO_ordinate_array(?,?, ?,?)),'mask=anyinteract') = 'TRUE'");

		ResultSet rs = null;

		try {
			pstmt.setDouble(1, x1);
			pstmt.setDouble(2, y1);
			pstmt.setDouble(3, x2);
			pstmt.setDouble(4, y2);
			rs = DB.getRs(pstmt);
			while (rs.next()) {
				Area a = new Area();
				initFromRs(a, rs);
				as.add(a);
			}
			initAreas(as);
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			DB.close(rs);
			DB.close(pstmt);
			DB.close(conn);
		}
		return as;
	}

	/**
	 * initiate the instance of Area using the values from the ResultSet
	 * @param a - Area
	 * @param rs - ResultSet
	 */
	private void initFromRs(Area a, ResultSet rs) {
		try {
			a.setId(rs.getString("ID"));
			a.setName(rs.getString("NAME"));

			STRUCT st = (STRUCT) rs.getObject("GEOM");
			// convert STRUCT into geometry
			JGeometry j_geom = JGeometry.load(st);
			a.setGeom(j_geom);

			a.setCenter(JGeometry.load((STRUCT) rs.getObject("center")).getJavaPoint());
			a.setArea(rs.getFloat("area"));
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private void initAreas(List<Area> as) {
		for (Area a : as) {
			initArea(a);
		}
	}

	private void initArea(Area a) {
		if (a != null) {
			a.setSelectCount(0);
			a.setTotalCount(0);
			loadYearsCount(a);
			loadMonthsCount(a);
			loadDaysCount(a);
			loadHoursCount(a);
		}
	}
}
