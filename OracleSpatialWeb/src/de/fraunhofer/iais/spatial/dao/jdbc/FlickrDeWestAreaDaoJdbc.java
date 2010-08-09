package de.fraunhofer.iais.spatial.dao.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.spatial.geometry.JGeometry;
import oracle.sql.STRUCT;
import de.fraunhofer.iais.spatial.entity.FlickrDeWestArea;
import de.fraunhofer.iais.spatial.util.DB;
import de.fraunhofer.iais.spatial.util.StringUtil;

public class FlickrDeWestAreaDaoJdbc{
	
	public List<FlickrDeWestArea> getAllAreas(FlickrDeWestArea.Radius radius) {
		List<FlickrDeWestArea> as = new ArrayList<FlickrDeWestArea>();
		Connection conn = DB.getConn();
		PreparedStatement pstmt = DB.getPstmt(conn, "select ID, NAME, GEOM, NUMBER_OF_EVENTS, SDO_GEOM.SDO_AREA(c.geom, 0.005) as area, SDO_GEOM.SDO_CENTROID(c.geom, m.diminfo) as center" + " from FLICKR_DE_WEST_TABLE_" + radius + " c, user_sdo_geom_metadata m"
				+ " where m.table_name = 'FLICKR_DE_WEST_TABLE_" + radius + "'");
		ResultSet rs = DB.getRs(pstmt);

		try {
			while (rs.next()) {
				FlickrDeWestArea a = new FlickrDeWestArea();
				a.setRadius(radius);
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

	public FlickrDeWestArea getAreaById(String areaid, FlickrDeWestArea.Radius radius) {
		FlickrDeWestArea a = new FlickrDeWestArea();
		Connection conn = DB.getConn();
		PreparedStatement pstmt = DB.getPstmt(conn, "select ID, NAME, GEOM, NUMBER_OF_EVENTS, SDO_GEOM.SDO_AREA(c.geom, 0.005) as area, SDO_GEOM.SDO_CENTROID(c.geom, m.diminfo) as center" + " from FLICKR_DE_WEST_TABLE_" + radius + " c, user_sdo_geom_metadata m" + " WHERE c.ID = ?");

		ResultSet rs = null;
		try {
			pstmt.setString(1, areaid);
			rs = DB.getRs(pstmt);
			if (rs.next()) {
				a.setRadius(radius);
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

	public List<FlickrDeWestArea> getAreasByPoint(double x, double y, FlickrDeWestArea.Radius radius) {
		List<FlickrDeWestArea> as = new ArrayList<FlickrDeWestArea>();
		Connection conn = DB.getConn();
		PreparedStatement pstmt = DB.getPstmt(conn, "select ID, NAME, GEOM, NUMBER_OF_EVENTS, SDO_GEOM.SDO_AREA(c.geom, 0.005) as area, SDO_GEOM.SDO_CENTROID(c.geom, m.diminfo) as center" + " from FLICKR_DE_WEST_TABLE_" + radius + " c, user_sdo_geom_metadata m"
				+ " WHERE m.table_name = 'FLICKR_DE_WEST_TABLE_" + radius + "' and sdo_relate(c.geom, SDO_geometry(2001,8307,SDO_POINT_TYPE(?, ?, NULL),NULL,NULL),'mask=anyinteract') = 'TRUE'");

		ResultSet rs = null;

		try {
			pstmt.setDouble(1, x);
			pstmt.setDouble(2, y);
			rs = DB.getRs(pstmt);
			while (rs.next()) {
				FlickrDeWestArea a = new FlickrDeWestArea();
				a.setRadius(radius);
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

	public List<FlickrDeWestArea> getAreasByRect(double x1, double y1, double x2, double y2, FlickrDeWestArea.Radius radius) {
		List<FlickrDeWestArea> as = new ArrayList<FlickrDeWestArea>();
		Connection conn = DB.getConn();
		PreparedStatement pstmt = DB.getPstmt(conn, "" + "select ID, NAME, GEOM, NUMBER_OF_EVENTS, CLUSTERING_OF_SOM_CELL, SDO_GEOM.SDO_AREA(c.geom, 0.005) as area, SDO_GEOM.SDO_CENTROID(c.geom, m.diminfo) as center"
				+ " from FLICKR_DE_WEST_TABLE_" + radius + " c, user_sdo_geom_metadata m"
				+ " WHERE m.table_name = 'FLICKR_DE_WEST_TABLE_" + radius + "' and sdo_relate(c.geom, SDO_geometry(2003,8307,NULL,SDO_elem_info_array(1,1003,3), SDO_ordinate_array(?,?, ?,?)),'mask=anyinteract') = 'TRUE'");

		ResultSet rs = null;

		try {
			pstmt.setDouble(1, x1);
			pstmt.setDouble(2, y1);
			pstmt.setDouble(3, x2);
			pstmt.setDouble(4, y2);
			rs = DB.getRs(pstmt);
			while (rs.next()) {
				FlickrDeWestArea a = new FlickrDeWestArea();
				a.setRadius(radius);
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

	public int getTotalCount(String areaid, FlickrDeWestArea.Radius radius) {
		Connection conn = DB.getConn();
		PreparedStatement selectStmt = null;
		ResultSet rs = null;
		int num = 0;
		try {
			selectStmt = DB.getPstmt(conn, "select total from FLICKR_DE_WEST_TABLE_COUNT where id = ? and radius = ?");
			selectStmt.setString(1, areaid);
			selectStmt.setString(2, StringUtil.radiusValue(radius));
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

	private void loadHoursCount(FlickrDeWestArea a) {
		Connection conn = DB.getConn();
		PreparedStatement selectStmt = null;
		ResultSet rs = null;
	
		Map<String, Integer> hoursCount = new LinkedHashMap<String, Integer>();
		a.setHoursCount(hoursCount);
	
		try {
			selectStmt = DB.getPstmt(conn, "select HOUR from FLICKR_DE_WEST_TABLE_COUNT where id = ? and radius = ?");
			selectStmt.setString(1, a.getId());
			selectStmt.setString(2, StringUtil.radiusValue(a));
			rs = DB.getRs(selectStmt);
			if (rs.next()) {
				String count = rs.getString("HOUR");
				System.out.println(count);
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

	private void loadDaysCount(FlickrDeWestArea a) {
		Connection conn = DB.getConn();
		PreparedStatement selectStmt = null;
		ResultSet rs = null;
	
		Map<String, Integer> daysCount = new LinkedHashMap<String, Integer>();
		a.setDaysCount(daysCount);
	
		try {
			selectStmt = DB.getPstmt(conn, "select DAY from FLICKR_DE_WEST_TABLE_COUNT where id = ? and radius = ?");
			selectStmt.setString(1, a.getId());
			selectStmt.setString(2, StringUtil.radiusValue(a));
			rs = DB.getRs(selectStmt);
			if (rs.next()) {
				String count = rs.getString("DAY");
				System.out.println(count);
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

	private void loadMonthsCount(FlickrDeWestArea a) {
		Connection conn = DB.getConn();
		PreparedStatement selectStmt = null;
		ResultSet rs = null;
	
		Map<String, Integer> monthsCount = new LinkedHashMap<String, Integer>();
		a.setMonthsCount(monthsCount);
	
		try {
			selectStmt = DB.getPstmt(conn, "select MONTH from FLICKR_DE_WEST_TABLE_COUNT where id = ? and radius = ?");
			selectStmt.setString(1, a.getId());
			selectStmt.setString(2, StringUtil.radiusValue(a));
			rs = DB.getRs(selectStmt);
			if (rs.next()) {
				String count = rs.getString("MONTH");
				System.out.println(count);
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

	private void loadYearsCount(FlickrDeWestArea a) {
		Connection conn = DB.getConn();
		PreparedStatement selectStmt = null;
		ResultSet rs = null;
	
		Map<String, Integer> yearsCount = new LinkedHashMap<String, Integer>();
		a.setYearsCount(yearsCount);
	
		try {
			selectStmt = DB.getPstmt(conn, "select YEAR from FLICKR_DE_WEST_TABLE_COUNT where id = ? and radius = ?");
			selectStmt.setString(1, a.getId());
			selectStmt.setString(2, StringUtil.radiusValue(a));
			rs = DB.getRs(selectStmt);
			if (rs.next()) {
				String count = rs.getString("YEAR");
				System.out.println(count);
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
	
	private void initAreas(List<FlickrDeWestArea> as) {
		for (FlickrDeWestArea a : as) {
			initArea(a);
		}
	}

	private void initArea(FlickrDeWestArea a) {
		if (a != null) {
			a.setSelectCount(0);
			a.setTotalCount(0);
			loadYearsCount(a);
			loadMonthsCount(a);
			loadDaysCount(a);
			loadHoursCount(a);
		}
	}
	
	/**
	 * initiate the instance of Area using the values from the ResultSet
	 * @param a - Area
	 * @param rs - ResultSet
	 */
	private void initFromRs(FlickrDeWestArea a, ResultSet rs) {
		try {
			a.setId(rs.getString("ID"));
			a.setName(rs.getString("NAME"));
			a.setTotalCount(rs.getInt("NUMBER_OF_EVENTS"));

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
}
