package de.fraunhofer.iais.spatial.test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import org.junit.Test;
import org.postgis.MultiPolygon;
import org.postgis.PGgeometry;
import org.postgis.Polygon;

import de.fraunhofer.iais.spatial.util.DBUtil;

public class TestPgJdbc {

	static DBUtil pgDb = new DBUtil("/jdbc_pg.properties", 18, 6);

	@Test
	public void testJdbc() throws Exception {
		String tableName = "flickr_europe_area_320000";
		Connection pgConn = pgDb.getConn();
		PreparedStatement selectStmt = pgDb.getPstmt(pgConn, "select id, Center(geom) as center, geom from " + tableName);
		ResultSet rs = pgDb.getRs(selectStmt);
		ResultSetMetaData rsMetaData = rs.getMetaData();
		try {
			while (rs.next()) {
				System.out.println("id:" + rs.getInt("id"));
				System.out.println("center:" + rs.getString("center"));
				PGgeometry geom = (PGgeometry)rs.getObject("geom");
				System.out.println("geom:" + geom.getGeometry());
				MultiPolygon p = (MultiPolygon)geom.getGeometry();
			}

			for (int i = 1; i <= rsMetaData.getColumnCount(); i++) {
				System.out.println(rsMetaData.getColumnLabel(i) + " | " + rsMetaData.getColumnName(i) + " | " + rsMetaData.getColumnTypeName(i) +" | "+ rsMetaData.getColumnClassName(i) + " | " + rsMetaData.getColumnType(i));
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
