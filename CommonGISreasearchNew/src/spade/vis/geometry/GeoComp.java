package spade.vis.geometry;

import java.util.Vector;

import spade.lib.util.GeoDistance;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: 20-Aug-2007
 * Time: 14:34:12
 * Contains some geometric computations.
 */
public class GeoComp {
	public static final double pi12 = Math.PI / 2, pi2 = 2 * Math.PI;
	public static final double pi18 = Math.PI / 8;
	public static final double dirLimits[] = { pi18, pi12 - pi18, pi12 + pi18, Math.PI - pi18, Math.PI + pi18, Math.PI + pi12 - pi18, Math.PI + pi12 + pi18, pi2 - pi18 };
	public static String dirStr[] = { "E", "NE", "N", "NW", "W", "SW", "S", "SE" };

	/**
	 * Finds the distance between two points specified by their
	 * coordinates (x1,y1) and (x2,y2).
	 * @param geographic - indicates whether the coordinates are geographic
	 */
	public static double distance(float x1, float y1, float x2, float y2, boolean geographic) {
		if (geographic)
			return GeoDistance.geoDist(x1, y1, x2, y2);
		double dx = (x2 - x1), dy = (y2 - y1);
		return Math.sqrt(dx * dx + dy * dy);
	}

	/**
	 * Finds the Manhattan distance between two points specified by their
	 * coordinates (x1,y1) and (x2,y2).
	 * @param geographic - indicates whether the coordinates are geographic
	 */
	public static double getManhattanDistance(float x1, float y1, float x2, float y2, boolean geographic) {
		double dx, dy;
		if (geographic) {
			dx = GeoDistance.geoDist(x1, y1, x1, y2);
			dy = GeoDistance.geoDist(x1, y2, x2, y2);
		} else {
			dx = (x2 - x1);
			dy = (y2 - y1);
		}
		return Math.abs(dx) + Math.abs(dy);
	}

	/**
	 * Finds the sine of the angle between a vector and the x-axis
	 * @param dx - x-coordinate of the vector (or x2-x1)
	 * @param dy - y-coordinate of the vector (or y2-y1)
	 * @return the sine of the angle sin(angle)
	 */
	public static double getSinAngleXAxis(float dx, float dy) {
		if (dx == 0)
			return Math.signum(dy);
		double hypo = Math.sqrt((double) dx * dx + dy * dy);
		return dy / hypo;
	}

	/**
	 * Finds the angle between a vector and the x-axis
	 * @param dx - x-coordinate of the vector (or x2-x1)
	 * @param dy - y-coordinate of the vector (or y2-y1)
	 * @return the angle, in radians
	 */
	public static double getAngleXAxis(float dx, float dy) {
		double ddx = Math.abs(dx), ddy = Math.abs(dy);
		if (ddx < 0.0001 && ddy < 0.0001)
			return 0;
		double angle = (ddx * 10 < ddy) ? pi12 - Math.atan(ddx / ddy) : Math.atan(ddy / ddx);
		if (dx < 0) {
			angle = Math.PI - angle;
		}
		if (dy < 0) {
			angle = -angle;
		}
		return angle;
	}

	/**
	 * Returns a string denoting the direction: N, NW, S, etc.
	 * @param angleXAxis - the angle with respect to the x-axis, in radians
	 */
	public static String getDirectionAsString(double angleXAxis) {
		if (angleXAxis < 0) {
			angleXAxis += pi2;
		}
		while (angleXAxis > pi2) {
			angleXAxis -= pi2;
		}
		for (int i = 0; i < dirLimits.length; i++)
			if (angleXAxis <= dirLimits[i])
				return dirStr[i];
		return dirStr[0];
	}

	/**
	 * Finds the angle between a vector and the y-axis (North direction)
	 * @param dx - x-coordinate of the vector (or x2-x1)
	 * @param dy - y-coordinate of the vector (or y2-y1)
	 * @return the angle, in degrees
	 * 0 = North, 90 = East, 180 = South, 270 = West
	 */
	public static double getAngleYAxis(float dx, float dy) {
		if (dx == 0 && dy == 0)
			return Double.NaN;
		if (dx == 0) {
			if (dy > 0)
				return 0; //North
			else
				return 180; //South
		}
		if (dy == 0) {
			if (dx > 0)
				return 90; //East
			else
				return 270; //West
		}
		double ddx = Math.abs(dx), ddy = Math.abs(dy);
		double angleY = Math.atan(ddx / ddy); // from 0 to pi/2
		if (Double.isNaN(angleY) || Double.isInfinite(angleY))
			return Double.NaN;
		if (dy > 0)
			if (dx < 0) {
				angleY = pi2 - angleY;
			} else {
				;
			}
		else if (dx > 0) {
			angleY = Math.PI - angleY;
		} else {
			angleY = Math.PI + angleY;
		}
		return angleY * 180 / Math.PI;
	}

	/**
	 * Returns a string denoting the direction: N, NW, S, etc.
	 * @param angleYAxis - the angle with respect to the y-axis, in degrees
	 */
	public static String getAngleYAxisAsString(double angleYAxis) {
		if (Double.isNaN(angleYAxis))
			return null;
		double baseDir = 0, step = 45.0 / 2;
		String dir[] = { "N", "NE", "E", "SE", "S", "SW", "W", "NW" };
		for (String element : dir) {
			if (angleYAxis < baseDir + step)
				return element;
			baseDir += 45;
		}
		return dir[0];
	}

	/**
	 * Finds the cosine of the angle between two vectors
	 * @param dx1 - x-coordinate of the first vector
	 * @param dy1 - y-coordinate of the first vector
	 * @param dx2 - x-coordinate of the second vector
	 * @param dy2 - x-coordinate of the second vector
	 * @return  the angle between the vectors, in radians
	 */
	public static double getCosAngleBetweenVectors(float dx1, float dy1, float dx2, float dy2) {
		double len2 = Math.sqrt(dx1 * dx1 + dy1 * dy1) * Math.sqrt(dx2 * dx2 + dy2 * dy2);
		double cos = 1;
		try {
			cos = (dx1 * dx2 + dy1 * dy2) / len2;
		} catch (Exception ex) {
		}
		if (Double.isNaN(cos) || cos > 1) {
			cos = 1;
		}
		return cos;
	}

	/**
	 * If the Geometry is an instance of RealPoint, returns it, otherwise,
	 * returns the centre of the bounding rectangle.
	 */
	public static RealPoint getPosition(Geometry g) {
		if (g == null)
			return null;
		if (g instanceof RealPoint)
			return (RealPoint) g;
		if (g instanceof RealCircle) {
			RealCircle c = (RealCircle) g;
			return new RealPoint(c.cx, c.cy);
		}
		float bounds[] = g.getBoundRect();
		if (bounds == null)
			return null;
		return new RealPoint((bounds[0] + bounds[2]) / 2, (bounds[1] + bounds[3]) / 2);
	}

	/**
	 * Transforms the angle specified in radians to degrees
	 */
	public static double getAngleInDegrees(double angleInRadians) {
		double absAngle = Math.abs(angleInRadians);
		while (absAngle > pi2) {
			absAngle -= pi2;
		}
		return Math.signum(angleInRadians) * absAngle * 360 / pi2;
	}

	/**
	 * Transforms the angle specified in degrees to radians
	 */
	public static double getAngleInRadians(double angleInDegrees) {
		double absAngle = Math.abs(angleInDegrees);
		while (absAngle > 360) {
			absAngle -= 360;
		}
		return Math.signum(angleInDegrees) * absAngle * pi2 / 360;
	}

	/**
	 * Returns all intersection points between the given two polylines
	 * @param cosMinAngle cosine of the minimum angle between lines
	 * If the angle is less, intersections are not computed
	 */
	public static RealPoint[] findIntersections(RealPolyline poly1, RealPolyline poly2, double cosMinAngle, float geoFactorX, float geoFactorY) {
		if (poly1 == null || poly2 == null || poly1.p == null || poly2.p == null || poly1.p.length < 2 || poly2.p.length < 2)
			return null;
		poly1.determineBounds();
		poly2.determineBounds();
		if (poly1.boundRect == null || poly2.boundRect == null)
			return null;
		RealRectangle br = poly1.boundRect.intersect(poly2.boundRect);
		if (br == null)
			return null;
		RealPoint last1 = poly1.p[poly1.p.length - 1], last2 = poly2.p[poly2.p.length - 1];
		int lastIdx1 = poly1.p.length - 2, lastIdx2 = poly2.p.length - 2;
		if (poly1.isClosed && !last1.equals(poly1.p[0])) {
			last1 = poly1.p[0];
			lastIdx1++;
		}
		if (poly2.isClosed && !last2.equals(poly2.p[0])) {
			last2 = poly2.p[0];
			lastIdx2++;
		}
		Vector<RealPoint> inter = new Vector<RealPoint>(10, 10);
		for (int i = 0; i <= lastIdx1; i++) {
			RealPoint p1 = poly1.p[i], p2 = (i < lastIdx1) ? poly1.p[i + 1] : last1;
			float dx = (p2.x - p1.x) * geoFactorX, dy = (p2.y - p1.y) * geoFactorY;
			if (dx == 0 && dy == 0) {
				continue;
			}
			for (int j = 0; j < lastIdx2; j++) {
				RealPoint p3 = poly2.p[j], p4 = (j < lastIdx2) ? poly2.p[j + 1] : last2;
				float dx2 = (p4.x - p3.x) * geoFactorX;
				float dy2 = (p4.y - p3.y) * geoFactorY;
				if (dx2 == 0 && dy2 == 0) {
					continue;
				}
				double cos = getCosAngleBetweenVectors(dx, dy, dx2, dy2);
				if (cos > cosMinAngle) {
					continue;
				}
				RealPoint ip = Computing.findIntersection(p1, p2, p3, p4);
				if (ip != null) {
					inter.addElement(ip);
				}
			}
		}
		if (inter.size() < 1)
			return null;
		RealPoint result[] = new RealPoint[inter.size()];
		for (int i = 0; i < inter.size(); i++) {
			result[i] = inter.elementAt(i);
		}
		return result;
	}

}
