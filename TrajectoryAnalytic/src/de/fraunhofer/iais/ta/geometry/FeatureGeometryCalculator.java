package de.fraunhofer.iais.ta.geometry;

import java.util.ArrayList;
import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.util.AffineTransformation;
import com.vividsolutions.jts.math.Vector2D;

public class FeatureGeometryCalculator {

	public Polygon arrow(Point fromPt, Point toPt, float width) {

		return null;

	}

	public MultiPolygon triangles(Coordinate fromCoordinate, Coordinate toCoordinate, float width, float length, float space, float fromMargin, float toMargin) {
		return null;
	}

	public List<Polygon> triangles(Coordinate fromCoordinate, Coordinate toCoordinate, float width, float triangleLength, float spaceLength) {

		Coordinate deltaVector = new Coordinate(toCoordinate.x - fromCoordinate.x, toCoordinate.y - fromCoordinate.y);
		Coordinate deltaNormalVector = Vector2D.create(deltaVector).normalize().toCoordinate();
		double deltaVectorLength = Vector2D.create(deltaVector).length();

		int num = (int) (deltaVectorLength /(spaceLength + triangleLength));
		double margin = deltaVectorLength - num * ((spaceLength + triangleLength));
		List<Polygon> polygons = new ArrayList<Polygon>();
		for(int i = 0; i < num ; i++){
			Coordinate from = new Coordinate(fromCoordinate.x + deltaNormalVector.x * ( margin / 2 + i * (spaceLength + triangleLength)), fromCoordinate.y + deltaNormalVector.y * ( margin / 2 + i * (spaceLength + triangleLength)));
			Coordinate to = new Coordinate(from.x + deltaNormalVector.x * (triangleLength), from.y + deltaNormalVector.y * (triangleLength));
			Polygon trianglePolygon = new FeatureGeometryCalculator().triangle(from, to, 1f);
			polygons.add(trianglePolygon);

		}

		return polygons;

	}

	public Polygon triangle(Coordinate fromCoordinate, Coordinate toCoordinate, float width) {
		Coordinate deltaVector = new Coordinate(toCoordinate.x - fromCoordinate.x, toCoordinate.y - fromCoordinate.y);
		Coordinate clockwiseVector = AffineTransformation.rotationInstance(-Math.PI / 2).transform(deltaVector, new Coordinate());
		Coordinate clockwiseNormalVector = Vector2D.create(clockwiseVector).normalize().toCoordinate();
		GeometryFactory geometryFactory = new GeometryFactory();

		Coordinate[] coordinates = new Coordinate[] {
				new Coordinate(fromCoordinate.x + deltaVector.x, fromCoordinate.y + deltaVector.y),
				new Coordinate(fromCoordinate.x + clockwiseNormalVector.x * width, fromCoordinate.y + clockwiseNormalVector.y * width),
				new Coordinate(fromCoordinate.x - clockwiseNormalVector.x * width, fromCoordinate.y - clockwiseNormalVector.y * width),
				new Coordinate(fromCoordinate.x + deltaVector.x, fromCoordinate.y + deltaVector.y)};

		LinearRing shell = geometryFactory.createLinearRing(coordinates);
		Polygon trianglePolygon = geometryFactory.createPolygon(shell, null);

		return trianglePolygon;

	}

	public Polygon triangleArrow(Coordinate fromCoordinate, Coordinate toCoordinate, float width, float bodyRatio) {
		Coordinate deltaVector = new Coordinate(toCoordinate.x - fromCoordinate.x, toCoordinate.y - fromCoordinate.y);
		Coordinate clockwiseVector = AffineTransformation.rotationInstance(-Math.PI / 2).transform(deltaVector, new Coordinate());
		Coordinate clockwiseNormalVector = Vector2D.create(clockwiseVector).normalize().toCoordinate();
		GeometryFactory geometryFactory = new GeometryFactory();

		Coordinate[] coordinates = new Coordinate[] {
				new Coordinate(fromCoordinate.x + deltaVector.x, fromCoordinate.y + deltaVector.y),
				new Coordinate(fromCoordinate.x + deltaVector.x * bodyRatio + clockwiseNormalVector.x * width, fromCoordinate.y + deltaVector.y * bodyRatio + clockwiseNormalVector.y * width),
				new Coordinate(fromCoordinate.x + clockwiseNormalVector.x * width, fromCoordinate.y + clockwiseNormalVector.y * width),
				new Coordinate(fromCoordinate.x + deltaVector.x * (1 - bodyRatio), fromCoordinate.y + deltaVector.y * (1 - bodyRatio)),
				new Coordinate(fromCoordinate.x - clockwiseNormalVector.x * width, fromCoordinate.y - clockwiseNormalVector.y * width),
				new Coordinate(fromCoordinate.x + deltaVector.x * bodyRatio - clockwiseNormalVector.x * width, fromCoordinate.y + deltaVector.y * bodyRatio - clockwiseNormalVector.y * width),
				new Coordinate(fromCoordinate.x + deltaVector.x, fromCoordinate.y + deltaVector.y)};

		LinearRing shell = geometryFactory.createLinearRing(coordinates);
		Polygon trianglePolygon = geometryFactory.createPolygon(shell, null);

		return trianglePolygon;

	}

}
