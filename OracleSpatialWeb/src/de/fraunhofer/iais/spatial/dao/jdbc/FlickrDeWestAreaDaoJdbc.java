package de.fraunhofer.iais.spatial.dao.jdbc;

import java.awt.geom.Point2D;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.spatial.geometry.JGeometry;
import oracle.sql.STRUCT;
import de.fraunhofer.iais.spatial.dao.FlickrDeWestAreaDao;
import de.fraunhofer.iais.spatial.dto.FlickrDeWestAreaDto.QueryLevel;
import de.fraunhofer.iais.spatial.entity.FlickrDeWestArea;
import de.fraunhofer.iais.spatial.entity.FlickrDeWestPhoto;
import de.fraunhofer.iais.spatial.entity.FlickrDeWestArea.Radius;

public class FlickrDeWestAreaDaoJdbc extends FlickrDeWestAreaDao {

	private DB db;

	public void setDb(DB db) {
		this.db = db;
	}

	/* (non-Javadoc)
	 * @see de.fraunhofer.iais.spatial.dao.jdbc.FlickrDeWestAreaDao#getAllAreas(Radius)
	 */
	@Override
	public List<FlickrDeWestArea> getAllAreas(Radius radius) {
		List<FlickrDeWestArea> as = new ArrayList<FlickrDeWestArea>();
		Connection conn = db.getConn();
		PreparedStatement pstmt = db.getPstmt(conn, "select ID, NAME, GEOM, NUMBER_OF_EVENTS, SDO_GEOM.SDO_AREA(c.geom, 0.005) as area, SDO_GEOM.SDO_CENTROID(c.geom, m.diminfo) as center" + " from FLICKR_DE_WEST_TABLE_" + radius
				+ " c, user_sdo_geom_metadata m" + " where m.table_name = 'FLICKR_DE_WEST_TABLE_" + radius + "'");
		ResultSet rs = db.getRs(pstmt);

		try {
			while (rs.next()) {
				FlickrDeWestArea a = new FlickrDeWestArea();
				a.setRadius(radius);
				initAreaFromRs(a, rs);
				as.add(a);
			}
			initAreas(as);
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			db.close(rs);
			db.close(pstmt);
			db.close(conn);
		}
		return as;
	}

	/* (non-Javadoc)
	 * @see de.fraunhofer.iais.spatial.dao.jdbc.FlickrDeWestAreaDao#getAreaById(java.lang.String, Radius)
	 */
	@Override
	public FlickrDeWestArea getAreaById(int areaid, Radius radius) {
		FlickrDeWestArea a = new FlickrDeWestArea();
		Connection conn = db.getConn();
		PreparedStatement pstmt = db.getPstmt(conn, "select ID, NAME, GEOM, NUMBER_OF_EVENTS, SDO_GEOM.SDO_AREA(c.geom, 0.005) as area, SDO_GEOM.SDO_CENTROID(c.geom, m.diminfo) as center" + " from FLICKR_DE_WEST_TABLE_" + radius
				+ " c, user_sdo_geom_metadata m" + " WHERE c.ID = ?");

		ResultSet rs = null;
		try {
			pstmt.setInt(1, areaid);
			rs = db.getRs(pstmt);
			if (rs.next()) {
				a.setRadius(radius);
				initAreaFromRs(a, rs);
			}
			initArea(a);
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			db.close(rs);
			db.close(pstmt);
			db.close(conn);
		}
		return a;
	}

	/* (non-Javadoc)
	 * @see de.fraunhofer.iais.spatial.dao.jdbc.FlickrDeWestAreaDao#getAreasByPoint(double, double, Radius)
	 */
	@Override
	public List<FlickrDeWestArea> getAreasByPoint(double x, double y, Radius radius) {
		List<FlickrDeWestArea> as = new ArrayList<FlickrDeWestArea>();
		Connection conn = db.getConn();
		PreparedStatement pstmt = db.getPstmt(conn, "select ID, NAME, GEOM, NUMBER_OF_EVENTS, SDO_GEOM.SDO_AREA(c.geom, 0.005) as area, SDO_GEOM.SDO_CENTROID(c.geom, m.diminfo) as center" + " from FLICKR_DE_WEST_TABLE_" + radius
				+ " c, user_sdo_geom_metadata m" + " WHERE m.table_name = 'FLICKR_DE_WEST_TABLE_" + radius + "' and sdo_relate(c.geom, SDO_geometry(2001,8307,SDO_POINT_TYPE(?, ?, NULL),NULL,NULL),'mask=anyinteract') = 'TRUE'");

		ResultSet rs = null;

		try {
			pstmt.setDouble(1, x);
			pstmt.setDouble(2, y);
			rs = db.getRs(pstmt);
			while (rs.next()) {
				FlickrDeWestArea a = new FlickrDeWestArea();
				a.setRadius(radius);
				initAreaFromRs(a, rs);
				as.add(a);
			}
			initAreas(as);
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			db.close(rs);
			db.close(pstmt);
			db.close(conn);
		}
		return as;
	}

	/* (non-Javadoc)
	 * @see de.fraunhofer.iais.spatial.dao.jdbc.FlickrDeWestAreaDao#getAreasByRect(double, double, double, double, Radius)
	 */
	@Override
	public List<FlickrDeWestArea> getAreasByRect(double x1, double y1, double x2, double y2, Radius radius) {
		List<FlickrDeWestArea> as = new ArrayList<FlickrDeWestArea>();
		Connection conn = db.getConn();
		PreparedStatement pstmt = db.getPstmt(conn, "" + "select ID, NAME, GEOM, NUMBER_OF_EVENTS, SDO_GEOM.SDO_AREA(c.geom, 0.005) as area, SDO_GEOM.SDO_CENTROID(c.geom, m.diminfo) as center" + " from FLICKR_DE_WEST_TABLE_" + radius
				+ " c, user_sdo_geom_metadata m" + " WHERE m.table_name = 'FLICKR_DE_WEST_TABLE_" + radius
				+ "' and sdo_relate(c.geom, SDO_geometry(2003,8307,NULL,SDO_elem_info_array(1,1003,3), SDO_ordinate_array(?,?, ?,?)),'mask=anyinteract') = 'TRUE'");

		ResultSet rs = null;

		try {
			pstmt.setDouble(1, x1);
			pstmt.setDouble(2, y1);
			pstmt.setDouble(3, x2);
			pstmt.setDouble(4, y2);
			rs = db.getRs(pstmt);
			while (rs.next()) {
				FlickrDeWestArea a = new FlickrDeWestArea();
				a.setRadius(radius);
				initAreaFromRs(a, rs);
				as.add(a);
			}
			initAreas(as);
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			db.close(rs);
			db.close(pstmt);
			db.close(conn);
		}
		return as;
	}

	/* (non-Javadoc)
	 * @see de.fraunhofer.iais.spatial.dao.jdbc.FlickrDeWestAreaDao#getAreasByPolygon(List<Point2D>, Radius)
	 */
	@Override
	public List<FlickrDeWestArea> getAreasByPolygon(List<Point2D> polygon, Radius radius) {
		List<FlickrDeWestArea> as = new ArrayList<FlickrDeWestArea>();

		String parameters = "";

		for (int i = 0; i < polygon.size() + 1; i++) {
			parameters += ",?,?";
		}
		parameters = parameters.substring(parameters.indexOf(',') + 1);

		Connection conn = db.getConn();
		PreparedStatement pstmt = db.getPstmt(conn, "" + "select ID, NAME, GEOM, NUMBER_OF_EVENTS, SDO_GEOM.SDO_AREA(c.geom, 0.005) as area, SDO_GEOM.SDO_CENTROID(c.geom, m.diminfo) as center" + " from FLICKR_DE_WEST_TABLE_" + radius
				+ " c, user_sdo_geom_metadata m" + " WHERE m.table_name = 'FLICKR_DE_WEST_TABLE_" + radius
				+ "' and sdo_relate(c.geom, SDO_geometry(2003,8307,NULL,SDO_elem_info_array(1,1003,1), SDO_ordinate_array(" + parameters + ")),'mask=anyinteract') = 'TRUE'");

		ResultSet rs = null;

		try {
			for (int i = 0; i < polygon.size(); i++) {
				System.out.println((2 * i + 1) + ":" + polygon.get(i).getX());
				System.out.println((2 * i + 2) + ":" + polygon.get(i).getY());
				pstmt.setDouble(2 * i + 1, polygon.get(i).getX());
				pstmt.setDouble(2 * i + 2, polygon.get(i).getY());
			}
			System.out.println((2 * polygon.size() + 1) + ":" + polygon.get(0).getX());
			System.out.println((2 * polygon.size() + 2) + ":" + polygon.get(0).getY());
			pstmt.setDouble(2 * polygon.size() + 1, polygon.get(0).getX());
			pstmt.setDouble(2 * polygon.size() + 2, polygon.get(0).getY());

			rs = db.getRs(pstmt);
			while (rs.next()) {
				FlickrDeWestArea a = new FlickrDeWestArea();
				a.setRadius(radius);
				initAreaFromRs(a, rs);
				as.add(a);
			}
			initAreas(as);
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			db.close(rs);
			db.close(pstmt);
			db.close(conn);
		}
		return as;
	}

	/* (non-Javadoc)
	 * @see de.fraunhofer.iais.spatial.dao.jdbc.FlickrDeWestAreaDao#getTotalCount(java.lang.String, Radius)
	 */
	@Override
	public int getTotalCount(int areaid, Radius radius) {
		Connection conn = db.getConn();
		PreparedStatement selectStmt = null;
		ResultSet rs = null;
		int num = 0;
		try {
			selectStmt = db.getPstmt(conn, "select TOTAL from FLICKR_DE_WEST_TABLE_COUNT where id = ? and radius = ?");
			selectStmt.setInt(1, areaid);
			selectStmt.setInt(2, Integer.parseInt(radius.toString()));
			rs = db.getRs(selectStmt);
			if (rs.next()) {
				num = rs.getInt("total");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			db.close(rs);
			db.close(selectStmt);
			db.close(conn);
		}
		return num;
	}

	protected FlickrDeWestPhoto getPhoto(FlickrDeWestArea area, String queryStr, int idx) {

		FlickrDeWestPhoto photo = null;
		QueryLevel queryLevel = FlickrDeWestAreaDao.judgeQueryLevel(queryStr);
		String oracleDatePatternStr = FlickrDeWestAreaDao.judgeOracleDatePatternStr(queryLevel);

		Connection conn = db.getConn();
		PreparedStatement selectStmt = null;
		ResultSet rs = null;
		try {
			selectStmt = db.getPstmt(conn, "select * from (select rownum rn, t2.* from (select t1.* from FLICKR_DE_WEST_TABLE t1, FLICKR_DE_WEST_TABLE_GEOM g"
					+ " where t1.PHOTO_ID = g.PHOTO_ID and g.CLUSTER_R" + area.getRadius()
					+ "_ID = ? and to_char(t1.dt, '" + oracleDatePatternStr + "') = ? order by t1.dt desc) t2 )"
					+ " where rn = ?");
			selectStmt.setInt(1, area.getId());
			selectStmt.setString(2, queryStr);
			selectStmt.setInt(3, idx);
			rs = db.getRs(selectStmt);
			if (rs.next()) {
				photo = new FlickrDeWestPhoto();
				initPhotoFromRs(rs, photo);
				photo.setArea(area);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			db.close(rs);
			db.close(selectStmt);
			db.close(conn);
		}

		return photo;
	}

	protected List<FlickrDeWestPhoto> getPhotos(FlickrDeWestArea area, String queryStr, int num) {

		List<FlickrDeWestPhoto> photos = new ArrayList<FlickrDeWestPhoto>();
		QueryLevel queryLevel = FlickrDeWestAreaDao.judgeQueryLevel(queryStr);
		String oracleDatePatternStr = FlickrDeWestAreaDao.judgeOracleDatePatternStr(queryLevel);
		String oracleToDateStr = "to_date('" + queryStr + "', '" + oracleDatePatternStr + "')";

		Connection conn = db.getConn();
		PreparedStatement selectStmt = null;
		ResultSet rs = null;
		try {
			selectStmt = db.getPstmt(conn, "select * from (select t.* from FLICKR_DE_WEST_TABLE t, FLICKR_DE_WEST_TABLE_GEOM g"
					+ " where t.PHOTO_ID = g.PHOTO_ID and g.CLUSTER_R" + area.getRadius()
					+ "_ID = ? and t.dt >= " + oracleToDateStr + " and t.dt < " + oracleToDateStr + " + interval '1' " + queryLevel + " order by t.dt desc)"
					+ " where rownum <= ?");
			selectStmt.setInt(1, area.getId());
			selectStmt.setInt(2, num);
			rs = db.getRs(selectStmt);
			while (rs.next()) {
				FlickrDeWestPhoto photo = new FlickrDeWestPhoto();
				photos.add(photo);
				photo.setArea(area);
				initPhotoFromRs(rs, photo);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			db.close(rs);
			db.close(selectStmt);
			db.close(conn);
		}

		return photos;
	}



	/* (non-Javadoc)
	 * @see de.fraunhofer.iais.spatial.dao.jdbc.FlickrDeWestAreaDao#getPhotos(FlickrDeWestArea, SortedSet<String>, int)
	 */
	@Override
	public List<FlickrDeWestPhoto> getPhotos(int areaid, Radius radius, SortedSet<String> queryStrs, int num) {

		List<FlickrDeWestPhoto> photos = new ArrayList<FlickrDeWestPhoto>();
		FlickrDeWestArea area = this.getAreaById(areaid, radius);
		QueryLevel queryLevel = FlickrDeWestAreaDao.judgeQueryLevel(queryStrs.first());
		Map<String, Integer> count = null;

		switch (queryLevel) {
		case YEAR:
			count = area.getYearsCount();
			break;
		case MONTH:
			count = area.getMonthsCount();
			break;
		case DAY:
			count = area.getDaysCount();
			break;
		case HOUR:
			count = area.getHoursCount();
			break;
		}

		List<String> tempQueryStrs = new ArrayList<String>(queryStrs);
		for (int i = tempQueryStrs.size() - 1; photos.size() < num && i >= 0; i--) {
			if (count.get(tempQueryStrs.get(i)) != null && count.get(tempQueryStrs.get(i)) > 0) {
				photos.addAll(this.getPhotos(area, tempQueryStrs.get(i), num - photos.size()));

			}
		}
		return photos;
	}

	private void initPhotoFromRs(ResultSet rs, FlickrDeWestPhoto photo) throws SQLException {

		photo.setId(rs.getLong("PHOTO_ID"));
		photo.setDate(rs.getTimestamp("DT"));
		photo.setLatitude(rs.getDouble("LATITUDE"));
		photo.setLongitude(rs.getDouble("LONGITUDE"));
		photo.setPersonId(rs.getString("PERSON"));
		photo.setRawTags(rs.getString("RAWTAGS"));
		photo.setSmallUrl(rs.getString("SMALLURL"));
		photo.setTitle(rs.getString("TITLE"));
		photo.setViewed(rs.getInt("VIEWED"));
	}

	private void loadHoursCount(FlickrDeWestArea a) {
		Connection conn = db.getConn();
		PreparedStatement selectStmt = null;
		ResultSet rs = null;

		Map<String, Integer> hoursCount = new LinkedHashMap<String, Integer>();
		a.setHoursCount(hoursCount);

		try {
			selectStmt = db.getPstmt(conn, "select HOUR from FLICKR_DE_WEST_TABLE_COUNT where id = ? and radius = ?");
			selectStmt.setInt(1, a.getId());
			selectStmt.setInt(2, Integer.parseInt(a.getRadius().toString()));
			rs = db.getRs(selectStmt);
			if (rs.next()) {
				String count = rs.getString("HOUR");
				if (count != null) {
					Matcher m = hourRegExPattern.matcher(count);
					while (m.find()) {
						hoursCount.put(m.group(1), Integer.parseInt(m.group(2)));
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			db.close(rs);
			db.close(selectStmt);
			db.close(conn);
		}
	}

	private void loadDaysCount(FlickrDeWestArea a) {
		Connection conn = db.getConn();
		PreparedStatement selectStmt = null;
		ResultSet rs = null;

		Map<String, Integer> daysCount = new LinkedHashMap<String, Integer>();
		a.setDaysCount(daysCount);

		try {
			selectStmt = db.getPstmt(conn, "select DAY from FLICKR_DE_WEST_TABLE_COUNT where id = ? and radius = ?");
			selectStmt.setInt(1, a.getId());
			selectStmt.setInt(2, Integer.parseInt(a.getRadius().toString()));
			rs = db.getRs(selectStmt);
			if (rs.next()) {
				String count = rs.getString("DAY");
				if (count != null) {
					Matcher m = dayRegExPattern.matcher(count);
					while (m.find()) {
						daysCount.put(m.group(1), Integer.parseInt(m.group(2)));
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			db.close(rs);
			db.close(selectStmt);
			db.close(conn);
		}
	}

	private void loadMonthsCount(FlickrDeWestArea a) {
		Connection conn = db.getConn();
		PreparedStatement selectStmt = null;
		ResultSet rs = null;

		Map<String, Integer> monthsCount = new LinkedHashMap<String, Integer>();
		a.setMonthsCount(monthsCount);

		try {
			selectStmt = db.getPstmt(conn, "select MONTH from FLICKR_DE_WEST_TABLE_COUNT where id = ? and radius = ?");
			selectStmt.setInt(1, a.getId());
			selectStmt.setInt(2, Integer.parseInt(a.getRadius().toString()));
			rs = db.getRs(selectStmt);
			if (rs.next()) {
				String count = rs.getString("MONTH");
				if (count != null) {
					Matcher m = monthRegExPattern.matcher(count);
					while (m.find()) {
						monthsCount.put(m.group(1), Integer.parseInt(m.group(2)));
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			db.close(rs);
			db.close(selectStmt);
			db.close(conn);
		}
	}

	private void loadYearsCount(FlickrDeWestArea a) {
		Connection conn = db.getConn();
		PreparedStatement selectStmt = null;
		ResultSet rs = null;

		Map<String, Integer> yearsCount = new LinkedHashMap<String, Integer>();
		a.setYearsCount(yearsCount);

		try {
			selectStmt = db.getPstmt(conn, "select YEAR from FLICKR_DE_WEST_TABLE_COUNT where id = ? and radius = ?");
			selectStmt.setInt(1, a.getId());
			selectStmt.setInt(2, Integer.parseInt(a.getRadius().toString()));
			rs = db.getRs(selectStmt);
			if (rs.next()) {
				String count = rs.getString("YEAR");
				if (count != null) {
					Matcher m = yearRegExPattern.matcher(count);
					while (m.find()) {
						yearsCount.put(m.group(1), Integer.parseInt(m.group(2)));
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			db.close(rs);
			db.close(selectStmt);
			db.close(conn);
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
			a.setTotalCount(getTotalCount(a.getId(), a.getRadius()));
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
	private void initAreaFromRs(FlickrDeWestArea a, ResultSet rs) {
		try {
			a.setId(rs.getInt("ID"));
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
