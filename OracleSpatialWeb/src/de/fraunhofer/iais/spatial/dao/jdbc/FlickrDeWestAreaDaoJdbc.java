package de.fraunhofer.iais.spatial.dao.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.spatial.geometry.JGeometry;
import oracle.sql.STRUCT;
import de.fraunhofer.iais.spatial.dao.FlickrDeWestAreaDao;
import de.fraunhofer.iais.spatial.entity.FlickrDeWestArea;
import de.fraunhofer.iais.spatial.entity.FlickrDeWestPhoto;
import de.fraunhofer.iais.spatial.util.DB;
import de.fraunhofer.iais.spatial.entity.FlickrDeWestArea.Radius;
import de.fraunhofer.iais.spatial.util.StringUtil;

public class FlickrDeWestAreaDaoJdbc implements FlickrDeWestAreaDao{
	
	/* (non-Javadoc)
	 * @see de.fraunhofer.iais.spatial.dao.jdbc.FlickrDeWestAreaDao#getAllAreas(Radius)
	 */
	@Override
	public List<FlickrDeWestArea> getAllAreas(Radius radius) {
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

	/* (non-Javadoc)
	 * @see de.fraunhofer.iais.spatial.dao.jdbc.FlickrDeWestAreaDao#getAreaById(java.lang.String, Radius)
	 */
	@Override
	public FlickrDeWestArea getAreaById(int areaid, Radius radius) {
		FlickrDeWestArea a = new FlickrDeWestArea();
		Connection conn = DB.getConn();
		PreparedStatement pstmt = DB.getPstmt(conn, "select ID, NAME, GEOM, NUMBER_OF_EVENTS, SDO_GEOM.SDO_AREA(c.geom, 0.005) as area, SDO_GEOM.SDO_CENTROID(c.geom, m.diminfo) as center" + " from FLICKR_DE_WEST_TABLE_" + radius + " c, user_sdo_geom_metadata m" + " WHERE c.ID = ?");

		ResultSet rs = null;
		try {
			pstmt.setInt(1, areaid);
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

	/* (non-Javadoc)
	 * @see de.fraunhofer.iais.spatial.dao.jdbc.FlickrDeWestAreaDao#getAreasByPoint(double, double, Radius)
	 */
	@Override
	public List<FlickrDeWestArea> getAreasByPoint(double x, double y, Radius radius) {
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

	/* (non-Javadoc)
	 * @see de.fraunhofer.iais.spatial.dao.jdbc.FlickrDeWestAreaDao#getAreasByRect(double, double, double, double, Radius)
	 */
	@Override
	public List<FlickrDeWestArea> getAreasByRect(double x1, double y1, double x2, double y2, Radius radius) {
		List<FlickrDeWestArea> as = new ArrayList<FlickrDeWestArea>();
		Connection conn = DB.getConn();
		PreparedStatement pstmt = DB.getPstmt(conn, "" + "select ID, NAME, GEOM, NUMBER_OF_EVENTS, SDO_GEOM.SDO_AREA(c.geom, 0.005) as area, SDO_GEOM.SDO_CENTROID(c.geom, m.diminfo) as center"
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

	/* (non-Javadoc)
	 * @see de.fraunhofer.iais.spatial.dao.jdbc.FlickrDeWestAreaDao#getTotalCount(java.lang.String, Radius)
	 */
	@Override
	public int getTotalCount(int areaid, Radius radius) {
		Connection conn = DB.getConn();
		PreparedStatement selectStmt = null;
		ResultSet rs = null;
		int num = 0;
		try {
			selectStmt = DB.getPstmt(conn, "select total from FLICKR_DE_WEST_TABLE_COUNT where id = ? and radius = ?");
			selectStmt.setInt(1, areaid);
			selectStmt.setInt(2, Integer.parseInt(radius.toString()));
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
	
	public List<FlickrDeWestPhoto> getPhotos(int areaid, Radius radius, SortedSet<String> hours, int num) {
		List<FlickrDeWestPhoto> photos = new ArrayList<FlickrDeWestPhoto>();
		while (photos.size() < num && hours.size() > 0){
			photos.addAll(this.getPhotos(areaid, radius, hours.last(), num - photos.size()));
			System.out.println("hour:"+hours.last());
			hours.remove(hours.last());
		}
		return photos;
	}

	public List<FlickrDeWestPhoto> getPhotos(int areaid, Radius radius, String hour, int num) {
		FlickrDeWestArea area = getAreaById(areaid, radius);
		List<FlickrDeWestPhoto> photos = new ArrayList<FlickrDeWestPhoto>();
		
		Connection conn = DB.getConn();
		PreparedStatement selectStmt = null;
		ResultSet rs = null;
		try {
			selectStmt = DB.getPstmt(conn, "select * from (select t.* from FLICKR_DE_WEST_TABLE t, FLICKR_DE_WEST_TABLE_GEOM g" +
													" where t.PHOTO_ID = g.PHOTO_ID and g.CLUSTER_R" + radius + "_ID = ? and TRUNC(t.dt, 'HH24') = to_date (?, 'yyyy-MM-dd@HH24') order by t.dt desc)" +
													" where rownum < ?");
			selectStmt.setInt(1, areaid);
			selectStmt.setString(2, hour);
			selectStmt.setInt(3, num);
			rs = DB.getRs(selectStmt);
			while (rs.next()) {
				FlickrDeWestPhoto photo =  new FlickrDeWestPhoto();
				photos.add(photo);
				
				photo.setArea(area);
				photo.setId(rs.getLong("PHOTO_ID"));
				photo.setDate(rs.getTimestamp("DT"));
				photo.setLatitude(rs.getDouble("LATITUDE"));
				photo.setLongitude(rs.getDouble("LONGITUDE"));
				photo.setPerson(rs.getString("PERSON"));
				photo.setRawTags(rs.getString("RAWTAGS"));
				photo.setSmallUrl(rs.getString("SMALLURL"));
				photo.setTitle(rs.getString("TITLE"));
				photo.setViewed(rs.getInt("VIEWED"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			DB.close(rs);
			DB.close(selectStmt);
			DB.close(conn);
		}
		
		return photos;
	}
	
	@Override
	public FlickrDeWestPhoto getPhoto(int areaid, Radius radius, String hour, int idx) {
		FlickrDeWestArea area = getAreaById(areaid, radius);
		
		
		Connection conn = DB.getConn();
		PreparedStatement selectStmt = null;
		ResultSet rs = null;
		FlickrDeWestPhoto photo = null;
		try {
			selectStmt = DB.getPstmt(conn, "select * from (select rownum rn, t2.* from (select t1.* from FLICKR_DE_WEST_TABLE t1, FLICKR_DE_WEST_TABLE_GEOM g" +
													" where t1.PHOTO_ID = g.PHOTO_ID and g.CLUSTER_R" + radius + "_ID = ? and TRUNC(t1.dt, 'HH24') = to_date (?, 'yyyy-MM-dd@HH24') order by t1.dt desc) t2 )" +
													" where rn = ?");
			selectStmt.setInt(1, areaid);
			selectStmt.setString(2, hour);
			selectStmt.setInt(3, idx);
			rs = DB.getRs(selectStmt);
			if (rs.next()) {
				photo =  new FlickrDeWestPhoto();
				
				photo.setArea(area);
				photo.setId(rs.getLong("PHOTO_ID"));
				photo.setDate(rs.getTimestamp("DT"));
				photo.setLatitude(rs.getDouble("LATITUDE"));
				photo.setLongitude(rs.getDouble("LONGITUDE"));
				photo.setPerson(rs.getString("PERSON"));
				photo.setRawTags(rs.getString("RAWTAGS"));
				photo.setSmallUrl(rs.getString("SMALLURL"));
				photo.setTitle(rs.getString("TITLE"));
				photo.setViewed(rs.getInt("VIEWED"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			DB.close(rs);
			DB.close(selectStmt);
			DB.close(conn);
		}
		
		return photo;
	}
	
	private void loadHoursCount(FlickrDeWestArea a) {
		Connection conn = DB.getConn();
		PreparedStatement selectStmt = null;
		ResultSet rs = null;
	
		Map<String, Integer> hoursCount = new LinkedHashMap<String, Integer>();
		a.setHoursCount(hoursCount);
	
		try {
			selectStmt = DB.getPstmt(conn, "select HOUR from FLICKR_DE_WEST_TABLE_COUNT where id = ? and radius = ?");
			selectStmt.setInt(1, a.getId());
			selectStmt.setInt(2, Integer.parseInt(a.getRadius().toString()));
			rs = DB.getRs(selectStmt);
			if (rs.next()) {
				String count = rs.getString("HOUR");
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
			selectStmt.setInt(1, a.getId());
			selectStmt.setInt(2, Integer.parseInt(a.getRadius().toString()));
			rs = DB.getRs(selectStmt);
			if (rs.next()) {
				String count = rs.getString("DAY");
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
			selectStmt.setInt(1, a.getId());
			selectStmt.setInt(2, Integer.parseInt(a.getRadius().toString()));
			rs = DB.getRs(selectStmt);
			if (rs.next()) {
				String count = rs.getString("MONTH");
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
			selectStmt.setInt(1, a.getId());
			selectStmt.setInt(2, Integer.parseInt(a.getRadius().toString()));
			rs = DB.getRs(selectStmt);
			if (rs.next()) {
				String count = rs.getString("YEAR");
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
	private void initFromRs(FlickrDeWestArea a, ResultSet rs) {
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
