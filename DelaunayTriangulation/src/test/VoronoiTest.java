package test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import util.DBUtil;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.io.WKTWriter;
import com.vividsolutions.jts.triangulate.DelaunayTriangulationBuilder;
import com.vividsolutions.jts.triangulate.VoronoiDiagramBuilder;

import edu.cornell.cs.delaunay.jre1_6.Pnt;
import edu.cornell.cs.delaunay.jre1_6.Triangle;
import edu.cornell.cs.delaunay.jre1_6.Triangulation;

public class VoronoiTest {

	final private static int BATCH_SIZE = 100;
	final private static String TABLE_NAME = "flickr_europe_100000";
	final static DBUtil db = new DBUtil("/jdbc_pg.properties", 3, 10);
	final public double x1 = -13.119622;
	final public double x2 = 35.287624;
	final public double y1 = 34.26329;
	final public double y2 = 72.09216;

	static List<Coordinate> points;

	@BeforeClass
	public static void loadPoints() throws SQLException {
		long start = System.currentTimeMillis();
		points = new ArrayList<Coordinate>();
		Connection conn = db.getConn();
		conn.setAutoCommit(false);
		PreparedStatement countStmt = db.getPstmt(conn, "select count(*) num from " + TABLE_NAME);
		PreparedStatement selectStmt = db.getPstmt(conn, "select longitude, latitude from " + TABLE_NAME);
		selectStmt.setFetchSize(BATCH_SIZE);
		ResultSet countRs = db.getRs(countStmt);
		ResultSet rs = db.getRs(selectStmt);

		try {
			if (countRs.next()) {
				System.out.println("#Coordinates: " + countRs.getInt("num"));
			}

			int i = 0;
			while (rs.next()) {
				Coordinate point = new Coordinate(rs.getDouble("longitude"), rs.getDouble("latitude"));
				System.out.println(i++ + " point: " + point.toString());
				points.add(point);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			db.close(rs);
			db.close(countRs);
			db.close(selectStmt);
			db.close(countStmt);
			db.close(conn);
		}
		long end = System.currentTimeMillis();
		System.out.println("escape time: " + (end - start) / 1000.0 + "s");
	}

	/**
	 * 1000,000 points: 14s(intel i3-580) 300M (win7-x86)
	 */
	@Test
	public void testJtsVoronoi() {
		long start = System.currentTimeMillis();
		VoronoiDiagramBuilder VoronoiBuilder = new VoronoiDiagramBuilder();
		VoronoiBuilder.setSites(points);

		VoronoiBuilder.setClipEnvelope(new Envelope(x1, x2, y1, y2));
		Geometry geom = VoronoiBuilder.getDiagram(new GeometryFactory());
		System.out.println("Voronoi Polygons:" + geom.getNumGeometries());
//		System.out.println(new WKTWriter().writeFormatted(geom));
//		System.out.println(geom.getGeometryN(0).toText());

		long end = System.currentTimeMillis();
		System.out.println("escape time: " + (end - start) / 1000.0 + "s");
	}

	@Test
	public void testJtsDelaunay() {
		long start = System.currentTimeMillis();
		DelaunayTriangulationBuilder delaunayBulider = new DelaunayTriangulationBuilder();
		delaunayBulider.setSites(points);

		Geometry triangulations = delaunayBulider.getTriangles(new GeometryFactory());
		System.out.println("Delaunay Triangulations:");
		System.out.println(new WKTWriter().writeFormatted(triangulations));

		long end = System.currentTimeMillis();
		System.out.println("escape time: " + (end - start) / 1000.0 + "s");
	}

	/**
	 * 10000 points: 23.944s (intel i3-380)
	 */
	@Test
	public void testCornellDelaunay() {
		long start = System.currentTimeMillis();
		VoronoiDiagramBuilder VoronoiBuilder = new VoronoiDiagramBuilder();
		VoronoiBuilder.setSites(points);

		int initialSize = 10000; // Size of initial triangle
		Triangle initialTriangle = new Triangle(new Pnt(-initialSize, -initialSize), new Pnt(initialSize, -initialSize), new Pnt(0, initialSize));
		System.out.println("Triangle created: " + initialTriangle);
		Triangulation dt = new Triangulation(initialTriangle);

		int i = 0;
		for(Coordinate point : points){
			System.out.println((i++) + "   " + point);
			dt.delaunayPlace(new Pnt(point.x, point.y));
//			if(i == 200)
//				break;
		}
		Triangle.moreInfo = true;

//		for (Triangle triangle : dt) {
//			Pnt[] vertices = triangle.toArray(new Pnt[0]);
//			System.out.println("trangle: " + triangle + " |vertices: " + vertices);
//		}

		long end = System.currentTimeMillis();
		System.out.println("escape time: " + (end - start) / 1000.0 + "s");
	}
}
