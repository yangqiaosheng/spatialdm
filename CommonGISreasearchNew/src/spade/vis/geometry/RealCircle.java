package spade.vis.geometry;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 02-Apr-2007
 * Time: 18:22:02
 */
public class RealCircle extends Geometry {
	public float cx = Float.NaN, cy = Float.NaN, rad = Float.NaN;

	public void init(float centerX, float centerY, float radius) {
		cx = centerX;
		cy = centerY;
		rad = radius;
	}

	public RealCircle() {
	}

	public RealCircle(float centerX, float centerY, float radius) {
		init(centerX, centerY, radius);
	}

	/**
	* The function allowing to determine the type of this geometry. Returns area.
	*/
	@Override
	public char getType() {
		return area;
	}

	@Override
	public float[] getCentroid() {
		if (centroid == null) {
			setCentroid(cx, cy);
		}
		return centroid;
	}

	/**
	* Determines the bounding rectangle of the geometry. Returns an array of 4
	* floats: 1) minimum x; 2) minimum y; 3) maximum x; 4) maximum y.
	* Uses the same static array for all instances!
	*/
	@Override
	public float[] getBoundRect() {
		if (Float.isNaN(cx) || Float.isNaN(cy) || Float.isNaN(rad))
			return null;
		bounds[0] = cx - rad;
		bounds[1] = cy - rad;
		bounds[2] = cx + rad;
		bounds[3] = cy + rad;
		return bounds;
	}

	/**
	* Returns the width of the geometry (in real coordinates).
	*/
	@Override
	public float getWidth() {
		return rad * 2;
	}

	/**
	* Returns the height of the geometry (in real coordinates)
	*/
	@Override
	public float getHeight() {
		return rad * 2;
	}

	/**
	 * Returns a closed polyline approximating this circle.
	 * @param nPoints - the desired number of points (one more will be added to close the line)
	 */
	public RealPolyline getPolygon(int nPoints) {
		if (Float.isNaN(cx) || Float.isNaN(cy) || Float.isNaN(rad))
			return null;
		RealPolyline poly = new RealPolyline();
		poly.p = new RealPoint[nPoints + 1];
		poly.p[0] = new RealPoint(cx + rad, cy);
		poly.p[nPoints] = poly.p[0];
		double dAngle = Math.PI * 2 / nPoints, angle = 0;
		for (int i = 1; i < nPoints; i++) {
			angle += dAngle;
			poly.p[i] = new RealPoint(cx + (float) (rad * Math.cos(angle)), cy + (float) (rad * Math.sin(angle)));
		}
		poly.isClosed = true;
		return poly;
	}

	@Override
	public boolean fitsInRectangle(float x1, float y1, float x2, float y2) {
		if (cx + rad <= x1 || cx - rad >= x2 || cy + rad <= y1 || cy - rad >= y2)
			return false;
		return true;
	}

	/**
	* Used to determine whether a considerable part of the geometry is contained
	* within the given rectangle (e.g.selection of objects in a map with a frame)
	*/
	@Override
	public boolean isInRectangle(float x1, float y1, float x2, float y2) {
		return fitsInRectangle(x1, y1, x2, y2);
	}

	/**
	* Checks if the point (x,y) belongs to the object. The point, in particular,
	* may represent the position of the mouse in the map.
	* Ignores the argument tolerateDist
	*/
	@Override
	public boolean contains(float x, float y, float tolerateDist) {
		float dx = x - cx, dy = y - cy;
		if (Math.abs(dx) > rad || Math.abs(dy) > rad)
			return false;
		return dx * dx + dy * dy <= rad * rad;
	}

	/**
	 * Checks if the given line segment crosses the circle.
	 * If so, returns the closest point to the centre.
	 * @return the closest point to the centre or null if the line
	 * segment does not cross the circle.
	 */
	public RealPoint getPointOfLineInsideCircle(float x1, float y1, float x2, float y2) {
		float t = cx - rad;
		if (t > x1 && t > x2)
			return null;
		t = cx + rad;
		if (t < x1 && t < x2)
			return null;
		t = cy - rad;
		if (t > y1 && t > y2)
			return null;
		t = cy + rad;
		if (t < y1 && t < y2)
			return null;
		RealPoint p = Computing.closestPoint(x1, y1, x2, y2, cx, cy);
		if (p == null)
			return null;
		if (contains(p.x, p.y, 0))
			return p;
		return null;
	}

	@Override
	public Object clone() {
		return new RealCircle(cx, cy, rad);
	}
}
