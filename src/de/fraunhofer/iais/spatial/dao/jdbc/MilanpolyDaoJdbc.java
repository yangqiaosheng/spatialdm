package de.fraunhofer.iais.spatial.dao.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;

import oracle.spatial.geometry.JGeometry;
import oracle.sql.STRUCT;
import de.fraunhofer.iais.spatial.dao.MilanpolyDao;
import de.fraunhofer.iais.spatial.entity.Milanpoly;

public class MilanpolyDaoJdbc implements MilanpolyDao {

	private DB db;

	public void setDb(DB db) {
		this.db = db;
	}

	@Override
	public List<Milanpoly> getAllMilanpolys() {
		List<Milanpoly> ms = new ArrayList<Milanpoly>();
		Connection conn = db.getConn();
		PreparedStatement pstmt = db.getPstmt(conn, "select ID, NAME, GEOM, CLUSTERING_OF_SOM_CELL, SDO_GEOM.SDO_AREA(c.geom, 0.005) as area from MILANPOLY c");
		ResultSet rs = db.getRs(pstmt);
		try {
			while (rs.next()) {
				Milanpoly m = new Milanpoly();
				initFromRs(m, rs);
				ms.add(m);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			db.close(rs);
			db.close(pstmt);
			db.close(conn);
		}
		return ms;
	}

	@Override
	public Milanpoly getMilanpolyById(int id) {
		Milanpoly m = new Milanpoly();
		Connection conn = db.getConn();
		PreparedStatement pstmt = db.getPstmt(conn, "select ID, NAME, GEOM, CLUSTERING_OF_SOM_CELL, SDO_GEOM.SDO_AREA(c.geom, 0.005) as area from MILANPOLY c " + "WHERE c.ID = ?");

		ResultSet rs = null;
		try {
			pstmt.setInt(1, id);
			rs = db.getRs(pstmt);
			while (rs.next()) {
				initFromRs(m, rs);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			db.close(rs);
			db.close(pstmt);
			db.close(conn);
		}
		return m;
	}

	@Override
	public List<Milanpoly> getMilanpolysByPoint(double x, double y) {
		List<Milanpoly> ms = new ArrayList<Milanpoly>();
		Connection conn = db.getConn();
		PreparedStatement pstmt = db.getPstmt(conn, "select ID, NAME, GEOM, CLUSTERING_OF_SOM_CELL, SDO_GEOM.SDO_AREA(c.geom, 0.005) as area from MILANPOLY c "
				+ "WHERE sdo_relate(c.geom, SDO_geometry(2001,8307,SDO_POINT_TYPE(?, ?, NULL),NULL,NULL),'mask=anyinteract') = 'TRUE'");

		ResultSet rs = null;

		try {
			pstmt.setDouble(1, x);
			pstmt.setDouble(2, y);
			rs = db.getRs(pstmt);
			while (rs.next()) {
				Milanpoly m = new Milanpoly();
				initFromRs(m, rs);
				ms.add(m);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			db.close(rs);
			db.close(pstmt);
			db.close(conn);
		}
		return ms;
	}

	@Override
	public List<Milanpoly> getMilanpolysByRect(double x1, double y1, double x2, double y2) {
		List<Milanpoly> ms = new ArrayList<Milanpoly>();
		Connection conn = db.getConn();
		PreparedStatement pstmt = db.getPstmt(conn, "select ID, NAME, GEOM, CLUSTERING_OF_SOM_CELL, SDO_GEOM.SDO_AREA(c.geom, 0.005) as area from MILANPOLY c "
				+ "WHERE sdo_relate(c.geom, SDO_geometry(2003,8307,NULL,SDO_elem_info_array(1,1003,3), SDO_ordinate_array(?,?, ?,?)),'mask=anyinteract') = 'TRUE'");

		ResultSet rs = null;

		try {
			pstmt.setDouble(1, x1);
			pstmt.setDouble(2, y1);
			pstmt.setDouble(3, x2);
			pstmt.setDouble(4, y2);
			rs = db.getRs(pstmt);
			while (rs.next()) {
				Milanpoly m = new Milanpoly();
				initFromRs(m, rs);
				ms.add(m);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			db.close(rs);
			db.close(pstmt);
			db.close(conn);
		}
		return ms;
	}

	private void initFromRs(Milanpoly m, ResultSet rs) {
		try {
			m.setId(rs.getInt("ID"));
			m.setName(rs.getString("NAME"));
			m.setClustering(rs.getString("CLUSTERING_OF_SOM_CELL"));
			STRUCT st = (oracle.sql.STRUCT) rs.getObject("GEOM");
			// convert STRUCT into geometry
			JGeometry j_geom = JGeometry.load(st);
			m.setGeom(j_geom);
			m.setArea(rs.getFloat("area"));
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
