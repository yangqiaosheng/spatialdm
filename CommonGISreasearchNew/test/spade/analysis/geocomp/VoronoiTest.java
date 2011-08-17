package spade.analysis.geocomp;

import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import org.junit.Test;

import spade.analysis.geocomp.voronoi.VoronoiNew;
import spade.vis.geometry.RealPoint;
import spade.vis.geometry.RealPolyline;
import au.com.bytecode.opencsv.CSVReader;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.io.WKTWriter;
import com.vividsolutions.jts.triangulate.DelaunayTriangulationBuilder;
import com.vividsolutions.jts.triangulate.VoronoiDiagramBuilder;

public class VoronoiTest {

	@Test
	public void testPolygonCreate() {
		Vector<RealPoint> points = new Vector<RealPoint>(500, 100);
		long t0 = System.currentTimeMillis();

		points.add(new RealPoint(0, 1));
		points.add(new RealPoint(1, 1));
		points.add(new RealPoint(1, 0));

		VoronoiNew voronoi = new VoronoiNew(points);
		if (!voronoi.isValid()) {
			System.out.println("Failed to triangulate!");
			return;
		}
		voronoi.setBuildNeighbourhoodMatrix(true);
		RealPolyline areas[] = voronoi.getPolygons();
		if (areas == null || areas.length < 1) {
			System.out.println("Failed to build polygons!");
			return;
		}
		long t = System.currentTimeMillis();
		int nPolygons = 0;
		for (RealPolyline area : areas)
			if (area != null) {
				++nPolygons;
			}
		System.out.println("Got " + nPolygons + " Voronoi polygons; elapsed time = " + (t - t0) + " msec.");

	}

	@Test
	public void testVoronoiPolygonsFromPointClusters() throws IOException {
		Set<Coordinate> coordinates = new HashSet<Coordinate>();
		long t0 = System.currentTimeMillis();

		CSVReader cr = new CSVReader(new FileReader("points.csv"));
		String[] terms;
		while ((terms = cr.readNext()) != null) {
			Coordinate coordinate = new Coordinate(Float.parseFloat(terms[0]), Float.parseFloat(terms[1]));
			Coordinate coordinate2 = new Coordinate(Float.parseFloat(terms[0]), Float.parseFloat(terms[1]));
			System.out.println(coordinate);
			System.out.println(coordinate2);
			System.out.println();

//			if(!coordinate.equals(coordinate2))
//				throw new NumberFormatException();
			coordinates.add(coordinate2);
		}
		cr.close();

		VoronoiDiagramBuilder voronoiBuilder = new VoronoiDiagramBuilder();
		voronoiBuilder.setSites(coordinates);

		try {
			Geometry geoms = voronoiBuilder.getDiagram(new GeometryFactory());
		} catch (Exception e) {
			e.printStackTrace();
		}
		DelaunayTriangulationBuilder delaunayBulider = new DelaunayTriangulationBuilder();
		delaunayBulider.setSites(coordinates);

		Geometry triangulations = delaunayBulider.getTriangles(new GeometryFactory());
		System.out.println("Delaunay Triangulations:");
		System.out.println(new WKTWriter().writeFormatted(triangulations));

//		Voronoi voronoi = new Voronoi(points);
//		if (!voronoi.isValid()) {
//			System.out.println("Failed to triangulate!");
//			return;
//		}
//		voronoi.setBuildNeighbourhoodMatrix(true);
//		RealPolyline areas[] = voronoi.getPolygons();
//		if (areas == null || areas.length < 1) {
//			System.out.println("Failed to build polygons!");
//			return;
//		}
//		long t = System.currentTimeMillis();
//		int nPolygons = 0;
//		for (RealPolyline area : areas)
//			if (area != null) {
//				++nPolygons;
//			}
//		System.out.println("Got " + nPolygons + " Voronoi polygons; elapsed time = " + (t - t0) + " msec.");
//
	}

}
