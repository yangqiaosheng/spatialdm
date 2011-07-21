package de.fraunhofer.iais.ta.util;

import com.vividsolutions.jts.geom.Coordinate;

public class CoordinateTransformer {

	public static Coordinate counterClockwiseRotation(Coordinate coordinate, double angle) {
		double x = coordinate.x * Math.cos(angle) - coordinate.y * Math.sin(angle);
		double y = coordinate.x * Math.sin(angle) + coordinate.y * Math.cos(angle);
		return new Coordinate(x, y);
	}

	public static Coordinate clockwiseRotation(Coordinate coordinate, double angle) {
		return counterClockwiseRotation(coordinate, -angle);
	}

	public static double vectorLength(Coordinate vector) {
		return Math.sqrt(vector.x * vector.x + vector.y * vector.y);
	}

	public static Coordinate unitVector(Coordinate vector) {
		double length = vectorLength(vector);
		return new Coordinate(vector.x / length, vector.y / length);
	}
}
