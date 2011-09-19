package de.fraunhofer.iais.ta.service;

import java.util.List;

import com.google.common.collect.Lists;
import com.vividsolutions.jts.algorithm.Angle;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.math.Vector2D;

import de.fraunhofer.iais.ta.entity.TrajectorySegment;
import de.fraunhofer.iais.ta.geometry.FeatureGeometryCalculator;

public class TrajectoryRenderer {

	float MAX_PEAK_HEAD_LENGTH = 2.0f;
	int MAX_SPEED = 200;
	private FeatureGeometryCalculator featureGeometryCalculator = new FeatureGeometryCalculator();

	public void renderTrajectorySegment(TrajectorySegment segment) {
		float width = 0.9f;
		float peakBodyLength = 0.2f;
		float peakHeadLength = segment.getSpeed() * MAX_PEAK_HEAD_LENGTH / MAX_SPEED;
		float spaceLength = 0.5f;

		segment.getFeature().setWidth(width);

		Polygon boundary = featureGeometryCalculator.boundary(segment.getFromCoordinate(), segment.getToCoordinate(), segment.getFeature().getWidth());
		segment.getFeature().setBoundary(boundary);

		List<Polygon> polygons = Lists.newArrayList();
		polygons = featureGeometryCalculator.peaks(segment.getFromCoordinate(), segment.getToCoordinate(), segment.getFeature().getWidth() * 0.8f, peakBodyLength, peakHeadLength, spaceLength);
		segment.getFeature().setTexture(polygons);
	}

	public void joinTrajectorySegments(List<TrajectorySegment> segments) {
		TrajectorySegment lastSegment = null;
		for (int i = 0; i < segments.size(); i++) {
			TrajectorySegment segment = segments.get(i);
			if (i != 0) {
				List<Polygon> texture = lastSegment.getFeature().getTexture();
				Geometry convexHall = featureGeometryCalculator.geometryFactory.buildGeometry(segment.getFeature().getTexture()).convexHull();
				List<Polygon> newTexture = Lists.newArrayList();
				for (int j = 0; j < texture.size(); j++) {
					Polygon polygon = texture.get(j);
					if(!convexHall.intersects(polygon)){
						newTexture.add(polygon);
					}
				}
				lastSegment.getFeature().setTexture(newTexture);
			}
			lastSegment = segment;
		}
	}


	public Polygon joinTrajectorySegments2(List<TrajectorySegment> segments) {
		Coordinate from = null;
		Coordinate join = null;
		Coordinate to = null;
		GeometryFactory geometryFactory = new GeometryFactory();
		Polygon p = null;
		for (int i = 0; i < segments.size(); i++) {
			TrajectorySegment segment = segments.get(i);
			if (i == 0) {
				join = segment.getFromCoordinate();
				to = segment.getToCoordinate();
			} else {
				TrajectorySegment lastSegment = segments.get(i - 1);
				from = (Coordinate) join.clone();
				System.out.println("from:" + from);
				join = segment.getFromCoordinate();
				to = segment.getToCoordinate();

				System.out.println("from:" + from);
				System.out.println("join:" + join);
				System.out.println("to:" + to);

				Coordinate vector1 = Vector2D.create(new Coordinate((from.x - join.x), (from.y - join.y))).normalize().toCoordinate();
				Coordinate vector2 = Vector2D.create(new Coordinate((to.x - join.x), (to.y - join.y))).normalize().toCoordinate();
				double angle = Angle.angleBetween(from, join, to);
				System.out.println("vector1:" + vector1);
				System.out.println("vector2:" + vector2);

				Coordinate bisectorNormalVector = Vector2D.create(new Coordinate((vector1.x + vector2.x), (vector1.y + vector2.y))).normalize().toCoordinate();
				System.out.println("bisectorNormalVector:" + bisectorNormalVector);
				Coordinate c1 = new Coordinate((bisectorNormalVector.x * (segment.getLength() + segment.getFeature().getWidth()) + join.x), (bisectorNormalVector.y * (segment.getLength() + segment.getFeature().getWidth()) + join.y));
				Coordinate c2 = new Coordinate(-bisectorNormalVector.x / Math.sin(angle / 2) * segment.getFeature().getWidth() * 2 + join.x, -bisectorNormalVector.y / Math.sin(angle / 2) * segment.getFeature().getWidth() * 2 + join.y);
				Coordinate c3 = new Coordinate(to.x, to.y);

				LinearRing shell = geometryFactory.createLinearRing(new Coordinate[] { c1, c2, c3, c1 });
				Polygon trianglePolygon = geometryFactory.createPolygon(shell, null);
				lastSegment.getFeature().setBoundary((Polygon) (lastSegment.getFeature().getBoundary().difference(trianglePolygon)));

				p = (Polygon) (lastSegment.getFeature().getBoundary().difference(trianglePolygon));
			}

		}
		return p;
	}
}
