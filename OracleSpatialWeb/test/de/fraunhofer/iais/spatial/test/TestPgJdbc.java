package de.fraunhofer.iais.spatial.test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import oracle.spatial.geometry.JGeometry.Point;

import org.junit.Test;
import org.postgis.Geometry;
import org.postgis.PGgeometry;
import org.postgis.Polygon;

import de.fraunhofer.iais.spatial.util.DBUtil;

public class TestPgJdbc {

	static DBUtil pgDb = new DBUtil("/jdbc_pg.properties", 18, 6);

	@Test
	public void testCalendar() throws Exception {
		String tableName = "flickr_europe_area_320000";
		Connection pgConn = pgDb.getConn();
		PreparedStatement selectStmt = pgDb.getPstmt(pgConn, "select id, Center(geom) as center, geom from " + tableName);
		ResultSet rs = pgDb.getRs(selectStmt);
		try {
			while (rs.next()) {
				PGgeometry geom = (PGgeometry)rs.getObject("geom");
				System.out.println("id:" + rs.getInt("id"));
				System.out.println("center:" + rs.getString("center"));
				System.out.println("geom:" + geom.getGeometry());
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			pgDb.close(rs);
			pgDb.close(selectStmt);
			pgDb.close(pgConn);
		}
	}
}
