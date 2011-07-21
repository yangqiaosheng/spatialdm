package de.fraunhofer.iais.ta.geometry;



import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import de.fraunhofer.iais.ta.util.CoordinateTransformer;

public class ArrowGeometryCalculator {

	public Polygon arrow(Point fromPt, Point toPt, float width) {

		return arrow(fromPt.getCoordinate(), toPt.getCoordinate(), width);

	}


	public Polygon arrow(Coordinate fromCoordinate, Coordinate toCoordinate, float width, float fromMargin, float toMargin) {
		Coordinate deltaVector = new Coordinate(toCoordinate.x - fromCoordinate.x, toCoordinate.y - fromCoordinate.y);
		return arrow(
				new Coordinate(fromCoordinate.x + deltaVector.x * fromMargin, fromCoordinate.y + deltaVector.y * fromMargin),
				new Coordinate(fromCoordinate.x + deltaVector.x * (1 - toMargin), fromCoordinate.y + deltaVector.y * (1 - toMargin)),
				width);
	}

	public Polygon arrow(Coordinate fromCoordinate, Coordinate toCoordinate, float width) {
		float bodyRatio = 0.8f;
		Coordinate deltaVector = new Coordinate(toCoordinate.x - fromCoordinate.x, toCoordinate.y - fromCoordinate.y);
		Coordinate clockwiseNormalVector = CoordinateTransformer.unitVector(CoordinateTransformer.clockwiseRotation(deltaVector, Math.PI / 2));
		GeometryFactory geometryFactory = new GeometryFactory();
		Coordinate[] coordinates = new Coordinate[] {
				new Coordinate(fromCoordinate.x, fromCoordinate.y),
				new Coordinate(fromCoordinate.x + deltaVector.x, fromCoordinate.y + deltaVector.y),
				new Coordinate(fromCoordinate.x + deltaVector.x * bodyRatio + clockwiseNormalVector.x * width * 2, fromCoordinate.y + deltaVector.y * bodyRatio + clockwiseNormalVector.y * width * 2),
				new Coordinate(fromCoordinate.x + deltaVector.x * bodyRatio + clockwiseNormalVector.x * width, fromCoordinate.y + deltaVector.y * bodyRatio + clockwiseNormalVector.y * width),
				new Coordinate(fromCoordinate.x + clockwiseNormalVector.x * width, fromCoordinate.y + clockwiseNormalVector.y * width), new Coordinate(fromCoordinate.x, fromCoordinate.y) };

		LinearRing shell = geometryFactory.createLinearRing(coordinates);
		Polygon arrowPolygon = geometryFactory.createPolygon(shell, null);

		return arrowPolygon;

	}

}
