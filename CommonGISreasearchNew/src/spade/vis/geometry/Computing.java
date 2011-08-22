package spade.vis.geometry;

import java.awt.Point;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Nov 25, 2008
 * Time: 3:28:25 PM
 * Contains basic computations.
 */
public class Computing {
	/**
	 * Finds the closest point on the line ((x1,y1),(x2,y2)) to the point (x3,y3).
	 * Returns a RealPoint containing the x- and y-coordinates of the point.
	 */
	public static RealPoint closestPoint(float x1, float y1, float x2, float y2, float x3, float y3) {
		float dx = x2 - x1, dy = y2 - y1;
		if (dx == 0 && dy == 0)
			return new RealPoint(x1, y1); //the points coincide
		float u = ((x3 - x1) * dx + (y3 - y1) * dy) / (dx * dx + dy * dy);
		if (u < 0)
			return new RealPoint(x1, y1);
		if (u > 1)
			return new RealPoint(x2, y2);
		return new RealPoint(x1 + u * dx, y1 + u * dy);
	}

	/**
	 * Finds the closest point on the line (p1,p2) to the point p3.
	 * Returns a RealPoint containing the x- and y-coordinates of the point.
	 */
	public static RealPoint closestPoint(RealPoint p1, RealPoint p2, RealPoint p3) {
		if (p1 == null || p2 == null || p3 == null)
			return null;
		return closestPoint(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y);
	}

	/**
	* Checks whether the given point specified by its x,y-coordinates fits on
	* the line segment specified by two points p1 and p2
	*/
	public static boolean isPointOnLine(int x, int y, Point p1, Point p2) {
		if (p1 == null || p2 == null)
			return false;
		if (!between(p1.y, p2.y, y) || !between(p1.x, p2.x, x))
			return false;
		float dx = p2.x - p1.x, adx = Math.abs(dx);
		if (adx <= 2)
			return true;
		float dy = p2.y - p1.y, ady = Math.abs(dy);
		if (ady <= 2)
			return true;
		if (adx > ady) {
			//count y-coordinate for the point on the line with the x-coordinate x
			int y0 = Math.round(p1.y + dy / dx * (x - p1.x));
			return y >= y0 - 2 && y <= y0 + 2;
		}
		//count x-coordinate for the point on the line with the y-coordinate y
		int x0 = Math.round(p1.x + dx / dy * (y - p1.y));
		return x >= x0 - 2 && x <= x0 + 2;
	}

	/**
	 * Finds the distance from the line ((x1,y1),(x2,y2)) to the point (x3,y3).
	 */
	public static double distance(float x1, float y1, float x2, float y2, float x3, float y3, boolean geo) {
		float dx = x2 - x1, dy = y2 - y1;
		if (dx == 0 && dy == 0)
			return GeoComp.distance(x1, y1, x3, y3, geo); //the points coincide
		float u = ((x3 - x1) * dx + (y3 - y1) * dy) / (dx * dx + dy * dy);
		if (u < 0)
			return GeoComp.distance(x1, y1, x3, y3, geo);
		if (u > 1)
			return GeoComp.distance(x2, y2, x3, y3, geo);
		return GeoComp.distance(x1 + u * dx, y1 + u * dy, x3, y3, geo);
	}

	/**
	 * Checks if two rectangles have a common part
	 */
	//public static boolean rectanglesIntersect (RealRectangle)
	/**
	 * Checks if lines can intersect without using expensive computations
	 * for finding real intersection
	 */
	public static boolean linesCanIntersect(RealPoint p1, RealPoint p2, RealPoint p3, RealPoint p4) {
		if (p1 == null || p2 == null || p3 == null || p4 == null)
			return false;
		if (Math.max(p1.x, p2.x) < Math.min(p3.x, p4.x))
			return false;
		if (Math.max(p1.y, p2.y) < Math.min(p3.y, p4.y))
			return false;
		if (Math.min(p1.x, p2.x) > Math.max(p3.x, p4.x))
			return false;
		if (Math.min(p1.y, p2.y) > Math.max(p3.y, p4.y))
			return false;

		if (p1.x == p2.x) {
			if (!between(p3.x, p4.x, p1.x))
				return false;
		} else if (p3.x == p4.x) {
			if (!between(p1.x, p2.x, p3.x))
				return false;
		}
		if (p1.y == p2.y) {
			if (!between(p3.y, p4.y, p1.y))
				return false;
		} else if (p3.y == p4.y) {
			if (!between(p1.y, p2.y, p3.y))
				return false;
		}
		return true;
	}

	/**
	 * calculates intersection and checks for parallel lines.
	 * also checks that the intersection point is actually on
	 * the line segment p1-p2
	 */
	public static RealPoint findIntersection(RealPoint p1, RealPoint p2, RealPoint p3, RealPoint p4) {
		if (!linesCanIntersect(p1, p2, p3, p4))
			return null;
		if (p1.x == p2.x || p3.x == p4.x) { //special case: vertical line
			if (p1.x != p2.x) {
				RealPoint p = p1;
				p1 = p3;
				p3 = p;
				p = p2;
				p2 = p4;
				p4 = p;
			}
			if (p3.y == p4.y) //special case: horizontal line
				return new RealPoint(p1.x, p3.y);
			float y = p3.y + (p1.x - p3.x) * (p4.y - p3.y) / (p4.x - p3.x);
			return new RealPoint(p1.x, y);
		}
		if (p1.y == p2.y || p3.y == p4.y) { //special case: horizontal line
			if (p1.y != p2.y) {
				RealPoint p = p1;
				p1 = p3;
				p3 = p;
				p = p2;
				p2 = p4;
				p4 = p;
			}
			float x = p3.x + (p1.y - p3.y) * (p4.x - p3.x) / (p4.y - p3.y);
			return new RealPoint(x, p1.y);
		}

		float xD1, yD1, xD2, yD2, xD3, yD3;
		double dot, deg, len1, len2;
		float ua, ub, div;

		// calculate differences
		xD1 = p2.x - p1.x;
		xD2 = p4.x - p3.x;
		yD1 = p2.y - p1.y;
		yD2 = p4.y - p3.y;
		xD3 = p1.x - p3.x;
		yD3 = p1.y - p3.y;

		// calculate the lengths of the two lines
		len1 = Math.sqrt(xD1 * xD1 + yD1 * yD1);
		if (len1 == 0)
			return null;
		len2 = Math.sqrt(xD2 * xD2 + yD2 * yD2);
		if (len2 == 0)
			return null;

		// calculate angle between the two lines.
		dot = (xD1 * xD2 + yD1 * yD2); // dot product
		deg = dot / (len1 * len2);

		// if abs(angle)==1 then the lines are parallell,
		// so no intersection is possible
		if (Math.abs(deg) == 1)
			return null;

		// find intersection Pt between two lines
		div = yD2 * xD1 - xD2 * yD1;
		if (div == 0)
			return null;
		ua = (xD2 * yD3 - yD2 * xD3) / div;
		ub = (xD1 * yD3 - yD1 * xD3) / div;
		RealPoint pt = new RealPoint(p1.x + ua * xD1, p1.y + ua * yD1);

		if (!between(p1.x, p2.x, pt.x))
			return null;
		if (!between(p1.y, p2.y, pt.y))
			return null;
		if (!between(p3.x, p4.x, pt.x))
			return null;
		if (!between(p3.y, p4.y, pt.y))
			return null;

		// return the valid intersection
		return pt;
	}

	public static boolean between(float p1, float p2, float x) {
		return ((x >= p2 && x <= p1) || (x >= p1 && x <= p2));
	}

	/**
	 * Checks if the given line segment intersects the given polyline
	 */
	public static boolean doesIntersect(RealPoint p1, RealPoint p2, RealPolyline poly) {
		if (p1 == null || p2 == null || poly == null || poly.p.length < 2)
			return false;
		poly.determineBounds();
		RealRectangle br = poly.boundRect;
		if (p1.x < br.rx1 && p2.x < br.rx1)
			return false;
		if (p1.x > br.rx2 && p2.x > br.rx2)
			return false;
		if (p1.y < br.ry1 && p2.y < br.ry1)
			return false;
		if (p1.y > br.ry2 && p2.y > br.ry2)
			return false;
		for (int i = 0; i < poly.p.length - 1; i++)
			if (findIntersection(p1, p2, poly.p[i], poly.p[i + 1]) != null)
				return true;
		if (!poly.p[poly.p.length - 1].equals(poly.p[0]))
			return findIntersection(p1, p2, poly.p[poly.p.length - 1], poly.p[0]) != null;
		return false;
	}

	/**
	 * Returns all intersection points between the given line and the given polyline
	 */
	public static RealPoint[] findIntersections(RealPoint p1, RealPoint p2, RealPolyline poly) {
		if (p1 == null || p2 == null || poly == null || poly.p == null || poly.p.length < 2)
			return null;
		poly.determineBounds();
		RealRectangle br = poly.boundRect;
		if (p1.x < br.rx1 && p2.x < br.rx1)
			return null;
		if (p1.x > br.rx2 && p2.x > br.rx2)
			return null;
		if (p1.y < br.ry1 && p2.y < br.ry1)
			return null;
		if (p1.y > br.ry2 && p2.y > br.ry2)
			return null;
		Vector<RealPoint> inter = new Vector<RealPoint>(10, 10);
		for (int i = 0; i < poly.p.length - 1; i++) {
			RealPoint ip = findIntersection(p1, p2, poly.p[i], poly.p[i + 1]);
			if (ip != null) {
				inter.addElement(ip);
			}
		}
		if (poly.isClosed && poly.p[poly.p.length - 1] != null && !poly.p[poly.p.length - 1].equals(poly.p[0])) {
			RealPoint ip = findIntersection(p1, p2, poly.p[poly.p.length - 1], poly.p[0]);
			if (ip != null) {
				inter.addElement(ip);
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

	public static double getMinkowskiDistance(double v1[], double v2[], double p) {
		double squaressum = 0;
		if (v1 == null || v2 == null || v1.length != v2.length)
			return 0;
		for (int i = 0; i < v1.length; i++) {
			squaressum += Math.pow(Math.abs(v1[i] - v2[i]), p);
		}
		double exp = 1.0d / p;
		double distance = Math.pow(squaressum, exp);
		return distance;
	}

	/**
	 * Computes the distance from the given point to the given polyline or polygon
	 */
	public static double distance(float x, float y, RealPolyline poly, boolean geo) {
		if (poly == null || poly.p == null || poly.p.length < 1)
			return Double.NaN;
		RealPoint p[] = poly.p;
		int last = p.length - 1;
		boolean closed = poly.isClosed || (last >= 3 && p[0].x == p[last].x && p[0].y == p[last].y);
		if (closed && poly.contains(x, y, 0, true))
			return 0;
		if (closed && p[0].x == p[last].x && p[0].y == p[last].y) {
			--last;
		}
		double minD = Double.NaN;
		for (int i = 0; i <= last; i++) {
			float x1 = p[i].x, x2 = (i < last) ? p[i + 1].x : p[0].x, y1 = p[i].y, y2 = (i < last) ? p[i + 1].y : p[0].y;
			double d = distance(x1, y1, x2, y2, x, y, geo);
			if (!Double.isNaN(d) && (Double.isNaN(minD) || minD > d)) {
				minD = d;
			}
		}
		return minD;
	}

	/**
	 * Computes the distance from the given point to the given geometry
	 */
	public static double distance(float x, float y, Geometry geom, boolean geo) {
		if (geom == null)
			return Double.NaN;
		if (geom.contains(x, y, 0))
			return 0;
		if (geom instanceof RealPoint) {
			RealPoint p = (RealPoint) geom;
			return GeoComp.distance(x, y, p.x, p.y, geo);
		}
		if (geom instanceof RealRectangle) {
			RealRectangle r = (RealRectangle) geom;
			if (r.contains(x, y, 0))
				return 0;
			double minD = Double.NaN;
			if (x >= r.rx1 && x <= r.rx2)
				if (y < r.ry1)
					return (geo) ? GeoComp.distance(x, y, x, r.ry1, true) : r.ry1 - y;
				else if (y > r.ry2)
					return (geo) ? GeoComp.distance(x, y, x, r.ry2, true) : y - r.ry2;
				else
					return 0;
			if (y >= r.ry1 && y <= r.ry2)
				if (x < r.rx1)
					return (geo) ? GeoComp.distance(x, y, r.rx1, y, true) : r.rx1 - x;
				else
					// x>r.rx2
					return (geo) ? GeoComp.distance(x, y, r.rx2, y, true) : x - r.rx2;
			if (x < r.rx1)
				if (y < r.ry1)
					return GeoComp.distance(x, y, r.rx1, r.ry1, geo);
				else
					// y>r.ry2
					return GeoComp.distance(x, y, r.rx1, r.ry2, geo);
			// x>r.rx2
			if (y < r.ry1)
				return GeoComp.distance(x, y, r.rx2, r.ry1, geo);
			// y>r.ry2
			return GeoComp.distance(x, y, r.rx2, r.ry2, geo);
		}
		if (geom instanceof RealPolyline)
			return distance(x, y, (RealPolyline) geom, geo);
		if (geom instanceof RealLine) {
			RealLine l = (RealLine) geom;
			return distance(l.x1, l.y1, l.x2, l.y2, x, y, geo);
		}
		if (geom instanceof MultiGeometry) {
			MultiGeometry mg = (MultiGeometry) geom;
			double minD = Double.NaN;
			for (int i = 0; i < mg.getPartsCount(); i++) {
				double d = distance(x, y, mg.getPart(i), geo);
				if (Double.isNaN(d)) {
					continue;
				}
				if (Double.isNaN(minD) || minD > d) {
					minD = d;
				}
			}
			return minD;
		}
		float b[] = geom.getBoundRect();
		if (b == null)
			return Double.NaN;
		return distance(x, y, new RealRectangle(b), geo);
	}
}
