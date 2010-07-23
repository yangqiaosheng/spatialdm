package de.fraunhofer.iais.spatial.dao.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import oracle.spatial.geometry.JGeometry;
import oracle.sql.STRUCT;
import de.fraunhofer.iais.spatial.entity.FlickrDeWestArea;
import de.fraunhofer.iais.spatial.util.DB;

public class FlickrDeWestAreaDaoJdbc{

	public List<FlickrDeWestArea> getAllAreas(FlickrDeWestArea.Radius radius) {
		List<FlickrDeWestArea> as = new ArrayList<FlickrDeWestArea>();
		Connection conn = DB.getConn();
		PreparedStatement pstmt = DB.getPstmt(conn, "select ID, NAME, GEOM, NUMBER_OF_EVENTS, SDO_GEOM.SDO_AREA(c.geom, 0.005) as area, SDO_GEOM.SDO_CENTROID(c.geom, m.diminfo) as center" + " from FLICKR_DE_WEST_TABLE" + radius + " c, user_sdo_geom_metadata m"
				+ " where m.table_name = 'FLICKR_DE_WEST_TABLE" + radius + "'");
		ResultSet rs = DB.getRs(pstmt);

		try {
			while (rs.next()) {
				FlickrDeWestArea a = new FlickrDeWestArea();
				initFromRs(a, rs);
				as.add(a);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			DB.close(rs);
			DB.close(pstmt);
			DB.close(conn);
		}
		return as;
	}

	public FlickrDeWestArea getAreaById(String id, FlickrDeWestArea.Radius radius) {
		FlickrDeWestArea a = new FlickrDeWestArea();
		Connection conn = DB.getConn();
		PreparedStatement pstmt = DB.getPstmt(conn, "select ID, NAME, GEOM, NUMBER_OF_EVENTS, SDO_GEOM.SDO_AREA(c.geom, 0.005) as area, SDO_GEOM.SDO_CENTROID(c.geom, m.diminfo) as center" + " from FLICKR_DE_WEST_TABLE" + radius + " c, user_sdo_geom_metadata m" + " WHERE c.ID = ?");

		ResultSet rs = null;
		try {
			pstmt.setString(1, id);
			rs = DB.getRs(pstmt);
			while (rs.next()) {
				initFromRs(a, rs);
			}
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
		PreparedStatement pstmt = DB.getPstmt(conn, "select ID, NAME, GEOM, NUMBER_OF_EVENTS, SDO_GEOM.SDO_AREA(c.geom, 0.005) as area, SDO_GEOM.SDO_CENTROID(c.geom, m.diminfo) as center" + " from FLICKR_DE_WEST_TABLE" + radius + " c, user_sdo_geom_metadata m"
				+ " WHERE m.table_name = 'FLICKR_DE_WEST_TABLE" + radius + "' and sdo_relate(c.geom, SDO_geometry(2001,8307,SDO_POINT_TYPE(?, ?, NULL),NULL,NULL),'mask=anyinteract') = 'TRUE'");

		ResultSet rs = null;

		try {
			pstmt.setDouble(1, x);
			pstmt.setDouble(2, y);
			rs = DB.getRs(pstmt);
			while (rs.next()) {
				FlickrDeWestArea a = new FlickrDeWestArea();
				initFromRs(a, rs);
				as.add(a);
			}
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
				+ " from FLICKR_DE_WEST_TABLE" + radius + " c, user_sdo_geom_metadata m"
				+ " WHERE m.table_name = 'FLICKR_DE_WEST_TABLE" + radius + "' and sdo_relate(c.geom, SDO_geometry(2003,8307,NULL,SDO_elem_info_array(1,1003,3), SDO_ordinate_array(?,?, ?,?)),'mask=anyinteract') = 'TRUE'");

		ResultSet rs = null;

		try {
			pstmt.setDouble(1, x1);
			pstmt.setDouble(2, y1);
			pstmt.setDouble(3, x2);
			pstmt.setDouble(4, y2);
			rs = DB.getRs(pstmt);
			while (rs.next()) {
				FlickrDeWestArea a = new FlickrDeWestArea();
				initFromRs(a, rs);
				as.add(a);
			}
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
