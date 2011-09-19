package de.fraunhofer.iais.ta.geometry;

import java.util.ArrayList;
import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.util.AffineTransformation;
import com.vividsolutions.jts.math.Vector2D;

public class FeatureGeometryCalculator {

	public GeometryFactory geometryFactory = new GeometryFactory();

	public Polygon arrow(Point fromPt, Point toPt, float width) {

		return null;

	}

	public List<Polygon> triangles(Coordinate fromCoordinate, Coordinate toCoordinate, float width, float length, float space, float fromMargin, float toMargin) {
		return null;
	}

	public Polygon boundary(Coordinate fromCoordinate, Coordinate toCoordinate, float width) {
		Coordinate[] coordinates = new Coordinate[] {fromCoordinate,  toCoordinate};
		LineString path = geometryFactory.createLineString(coordinates);

		return (Polygon) path.buffer(width);
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
			Polygon trianglePolygon = new FeatureGeometryCalculator().triangle(from, to, width);
			polygons.add(trianglePolygon);

		}

		return polygons;

	}

	public Polygon triangle(Coordinate fromCoordinate, Coordinate toCoordinate, float width) {
		Coordinate deltaVector = new Coordinate(toCoordinate.x - fromCoordinate.x, toCoordinate.y - fromCoordinate.y);
		Coordinate clockwiseVector = AffineTransformation.rotationInstance(-Math.PI / 2).transform(deltaVector, new Coordinate());
		Coordinate clockwiseNormalVector = Vector2D.create(clockwiseVector).normalize().toCoordinate();

		Coordinate[] coordinates = new Coordinate[] {
				new Coordinate(fromCoordinate.x + deltaVector.x, fromCoordinate.y + deltaVector.y),
				new Coordinate(fromCoordinate.x + clockwiseNormalVector.x * width, fromCoordinate.y + clockwiseNormalVector.y * width),
				new Coordinate(fromCoordinate.x - clockwiseNormalVector.x * width, fromCoordinate.y - clockwiseNormalVector.y * width),
				new Coordinate(fromCoordinate.x + deltaVector.x, fromCoordinate.y + deltaVector.y)};

		LinearRing shell = geometryFactory.createLinearRing(coordinates);
		Polygon trianglePolygon = geometryFactory.createPolygon(shell, null);

		return trianglePolygon;

	}

	public List<Polygon> peaks(Coordinate fromCoordinate, Coordinate toCoordinate, float width, float peakBodyLength, float peakHeadLength, float spaceLength) {

		Coordinate deltaVector = new Coordinate(toCoordinate.x - fromCoordinate.x, toCoordinate.y - fromCoordinate.y);
		Coordinate deltaNormalVector = Vector2D.create(deltaVector).normalize().toCoordinate();
		double deltaVectorLength = Vector2D.create(deltaVector).length();
		int num = (int) ((deltaVectorLength - peakHeadLength) /(spaceLength + peakBodyLength));
		double margin = (deltaVectorLength - peakHeadLength) - num * ((spaceLength + peakBodyLength)) ;
		float peakLength = peakBodyLength + peakHeadLength;
		float bodyRatio = peakBodyLength / peakLength;
		List<Polygon> polygons = new ArrayList<Polygon>();
		for(int i = 0; i < num ; i++){
			Coordinate from = new Coordinate(fromCoordinate.x + deltaNormalVector.x * ( margin + i * (spaceLength + peakBodyLength)), fromCoordinate.y + deltaNormalVector.y * ( margin + i * (spaceLength + peakBodyLength)));
			Coordinate to = new Coordinate(from.x + deltaNormalVector.x * peakLength, from.y + deltaNormalVector.y * peakLength);
			Polygon trianglePolygon = new FeatureGeometryCalculator().peak(from, to, width, bodyRatio);
			polygons.add(trianglePolygon);
		}

		return polygons;

	}


	public Polygon peak(Coordinate fromCoordinate, Coordinate dirCoordinate, float width, float peakBodyLength, float peakHeadLength) {
		Coordinate deltaVector = new Coordinate(dirCoordinate.x - fromCoordinate.x, dirCoordinate.y - fromCoordinate.y);
		Coordinate deltaNormalVector = Vector2D.create(deltaVector).normalize().toCoordinate();
		float peakLength = peakBodyLength + peakHeadLength;
		float bodyRatio = peakBodyLength / peakLength;
		Coordinate toCoordinate = new Coordinate(fromCoordinate.x + deltaNormalVector.x * peakLength, fromCoordinate.y + deltaNormalVector.y * peakLength);
		return peak(fromCoordinate, toCoordinate, width, bodyRatio);
	}

	public Polygon peak(Coordinate fromCoordinate, Coordinate toCoordinate, float width, float bodyRatio) {
		Coordinate deltaVector = new Coordinate(toCoordinate.x - fromCoordinate.x, toCoordinate.y - fromCoordinate.y);
		Coordinate clockwiseVector = AffineTransformation.rotationInstance(-Math.PI / 2).transform(deltaVector, new Coordinate());
		Coordinate clockwiseNormalVector = Vector2D.create(clockwiseVector).normalize().toCoordinate();

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
