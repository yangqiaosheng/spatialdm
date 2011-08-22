package spade.vis.geometry;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 03-Jan-2007
 * Time: 10:26:37
 * A straight line connecting two points
 */
public class RealLine extends Geometry {
	/**
	 * Coordinates of the ends of the line: (x1,y1) and (x2,y2).
	 */
	public float x1 = Float.NaN, y1 = Float.NaN, x2 = Float.NaN, y2 = Float.NaN;
	/**
	 * Indicates whether the line is directed (i.e. is a vector).
	 * By default is true.
	 */
	public boolean directed = true;

	/**
	* The function allowing to determine the type of this geometry.
	* Returns Geometry.line.
	*/
	@Override
	public char getType() {
		return Geometry.line;
	}

	/**
	 * Checks whether all the coordinates have valid values
	 */
	public boolean isValid() {
		return !Float.isNaN(x1) && !Float.isNaN(x2) && !Float.isNaN(y1) && !Float.isNaN(y2);
	}

	public void setup(float x1, float y1, float x2, float y2) {
		this.x1 = x1;
		this.x2 = x2;
		this.y1 = y1;
		this.y2 = y2;
	}

	@Override
	public Object clone() {
		RealLine line = new RealLine();
		line.setup(x1, y1, x2, y2);
		return line;
	}

	/**
	* Used to determine whether at least a part of the geometry is visible in the
	* current map viewport.
	*/
	@Override
	public boolean fitsInRectangle(float rx1, float ry1, float rx2, float ry2) {
		if (!isValid())
			return false;
		if (x1 < rx1 && x2 < rx1)
			return false;
		if (x1 > rx2 && x2 > rx2)
			return false;
		if (y1 < ry1 && y2 < ry1)
			return false;
		if (y1 > ry2 && y2 > ry2)
			return false;
		if (x1 >= rx1 && x1 <= rx2 && y1 >= ry1 && y1 <= ry2)
			return true;
		if (x2 >= rx1 && x2 <= rx2 && y2 >= ry1 && y2 <= ry2)
			return true;
		float c = findX(ry1);
		if (!Float.isNaN(c) && c >= rx1 && c <= rx2)
			return true;
		c = findX(ry2);
		if (!Float.isNaN(c) && c >= rx1 && c <= rx2)
			return true;
		c = findY(rx1);
		if (!Float.isNaN(c) && c >= ry1 && c <= ry2)
			return true;
		c = findY(rx2);
		if (!Float.isNaN(c) && c >= ry1 && c <= ry2)
			return true;
		return false;
	}

	/**
	* Used to determine whether a considerable part of the geometry is contained
	* within the given rectangle (e.g.selection of objects in a map with a frame)
	*/
	@Override
	public boolean isInRectangle(float rx1, float ry1, float rx2, float ry2) {
		if (!isValid())
			return false;
		if (x1 >= rx1 && x1 <= rx2 && y1 >= ry1 && y1 <= ry2)
			return true;
		if (x2 >= rx1 && x2 <= rx2 && y2 >= ry1 && y2 <= ry2)
			return true;
		return false;
	}

	/**
	* Checks if the point (x,y) belongs to the object. The point, in particular,
	* may represent the position of the mouse in the map.
	*/
	@Override
	public boolean contains(float x, float y, float tolerateDist) {
		if (!isValid())
			return false;
		if (isThePoint(x, y, x1, y1, tolerateDist))
			return true;
		if (isThePoint(x, y, x2, y2, tolerateDist))
			return true;
		if (prov(y1, y2, y, tolerateDist) && prov(x1, x2, x, tolerateDist)) {
			float dx = x2 - x1;
			if (Math.abs(dx) < tolerateDist) //vertical line
				return true;
			float dy = y2 - y1;
			if (Math.abs(dy) < tolerateDist) //horizontal line
				return true;
			if (Math.abs(dx) > Math.abs(dy)) {
				float r = y1 + (dy * (x - x1)) / dx;
				//this is the ordinate of the point on the line
				//with the abscissa equal to x
				if (Math.abs(r - y) < tolerateDist)
					return true;
			} else {
				float r = x1 + (dx * (y - y1)) / dy;
				//this is the abscissa of the point on the line
				//with the ordinate equal to y
				if (Math.abs(r - x) < tolerateDist)
					return true;
			}
		}
		return false;
	}

	/**
	* Determines the bounding rectangle of the geometry. Returns an array of 4
	* floats: 1) minimum x; 2) minimum y; 3) maximum x; 4) maximum y.
	* Uses the same static array for all instances!
	*/
	@Override
	public float[] getBoundRect() {
		if (!isValid())
			return null;
		bounds[0] = x1;
		bounds[1] = y1;
		bounds[2] = x2;
		bounds[3] = y2;
		return bounds;
	}

	/**
	* Returns the width of the geometry (in real coordinates)
	*/
	@Override
	public float getWidth() {
		return Math.abs(x1 - x2);
	}

	/**
	* Returns the height of the geometry (in real coordinates)
	*/
	@Override
	public float getHeight() {
		return Math.abs(y1 - y2);
	}

	/**
	 * Finds the x-coordinate of the point on the line with the given y-coordinate.
	 * Returns Float.NaN if there is no such point.
	 */
	public float findX(float y) {
		if (!isValid())
			return Float.NaN;
		if (y == y1)
			return x1;
		if (y == y2)
			return x2;
		if (y1 == y2)
			return Float.NaN;
		float dy = y2 - y1, dx = x2 - x1;
		return x1 + (dx * (y - y1)) / dy;
	}

	/**
	 * Finds the y-coordinate of the point on the line with the given x-coordinate.
	 * Returns Float.NaN if there is no such point.
	 */
	public float findY(float x) {
		if (!isValid())
			return Float.NaN;
		if (x == x1)
			return y1;
		if (x == x2)
			return y2;
		if (x1 == x2)
			return Float.NaN;
		float dy = y2 - y1, dx = x2 - x1;
		return y1 + (dy * (x - x1)) / dx;
	}
}
