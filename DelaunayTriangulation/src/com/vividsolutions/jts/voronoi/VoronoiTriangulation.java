package com.vividsolutions.jts.voronoi;

import java.util.HashSet;
import java.util.Set;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.io.WKTWriter;
import com.vividsolutions.jts.triangulate.DelaunayTriangulationBuilder;
import com.vividsolutions.jts.triangulate.VoronoiDiagramBuilder;

public class VoronoiTriangulation {
	public static void main(String[] args) {
		VoronoiDiagramBuilder VoronoiBuilder = new VoronoiDiagramBuilder();
		Set<Coordinate> points = new HashSet<Coordinate>();
		points.add(new Coordinate(0, 0));
		points.add(new Coordinate(0, 1));
		points.add(new Coordinate(1, 0));
		points.add(new Coordinate(1, 1));
		VoronoiBuilder.setSites(points);
		VoronoiBuilder.setClipEnvelope(new Envelope(-5, 5, -4, 4));
		Geometry geom = VoronoiBuilder.getDiagram(new GeometryFactory());
		GeometryCollection geomColletion = (GeometryCollection) geom;
		System.out.println("Voronoi Polygons:");
		System.out.println(geomColletion.toText());
		System.out.println(new WKTWriter().writeFormatted(geom));
		System.out.println(geom.getGeometryN(0).toText());
		System.out.println(geomColletion.getGeometryN(0).toText());

		DelaunayTriangulationBuilder delaunayBulider = new DelaunayTriangulationBuilder();
		delaunayBulider.setSites(points);
		Geometry triangulations = delaunayBulider.getTriangles(new GeometryFactory());
		System.out.println("\nDelaunay Triangulations:");
		System.out.println(new WKTWriter().writeFormatted(triangulations));

	}
}
