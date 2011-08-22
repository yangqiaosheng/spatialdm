package spade.vis.geometry;

public class RealPoint extends Geometry {
	public float x, y;

	public RealPoint() {
	}

	public RealPoint(float x, float y) {
		this.x = x;
		this.y = y;
	}

	public RealPoint(float coords[]) {
		if (coords != null && coords.length >= 2) {
			x = coords[0];
			y = coords[1];
		}
	}

	@Override
	public String toString() {
		return "RealPoint(" + x + "," + y + ")";
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (!(obj instanceof RealPoint))
			return false;
		if (super.equals(obj))
			return true;
		RealPoint p = (RealPoint) obj;
		return p.x == x && p.y == y;
	}

	/**
	* The function allowing to determine the type of this geometry. Returns point.
	*/
	@Override
	public char getType() {
		return point;
	}

	@Override
	public float[] getCentroid() {
		if (centroid == null) {
			setCentroid(x, y);
		}
		return centroid;
	}

	/**
	* Determines the bounding rectangle of the geometry. Returns an array of 4
	* floats: 1) minimum x; 2) minimum y; 3) maximum x; 4) maximum y.
	* Uses the same static array for all instances!
	* For a point 1==3 and 2==4
	*/
	@Override
	public float[] getBoundRect() {
		bounds[0] = bounds[2] = x;
		bounds[1] = bounds[3] = y;
		return bounds;
	}

	/**
	* Returns the width of the geometry (in real coordinates).
	* For a point the width is zero.
	*/
	@Override
	public float getWidth() {
		return 0.0f;
	}

	/**
	* Returns the height of the geometry (in real coordinates)
	* For a point the height is zero.
	*/
	@Override
	public float getHeight() {
		return 0.0f;
	}

	/**
	* Used to determine whether at least a part of the geometry is visible in the
	* current map viewport.
	*/
	@Override
	public boolean fitsInRectangle(float x1, float y1, float x2, float y2) {
		return x >= x1 && x <= x2 && y >= y1 && y <= y2;
	}

	/**
	* Used to determine whether a considerable part of the geometry is contained
	* within the given rectangle (e.g.selection of objects in a map with a frame)
	*/
	@Override
	public boolean isInRectangle(float x1, float y1, float x2, float y2) {
		return fitsInRectangle(x1, y1, x2, y2);
	}

//ID
	/**
	* Returns the distance to a given point
	*/
	public double distanceToPoint(float cx, float cy) {
		double dx = x - cx, dy = y - cy;
		return Math.sqrt(dx * dx + dy * dy);
	}

//~ID
	/**
	* Checks if the point (x,y) belongs to the object. The point, in particular,
	* may represent the position of the mouse in the map.
	*/
	@Override
	public boolean contains(float x, float y, float tolerateDist) {
		return isThePoint(x, y, this.x, this.y, tolerateDist);
	}

	@Override
	public Object clone() {
		RealPoint p = new RealPoint();
		p.x = x;
		p.y = y;
		return p;
	}

//ID
// this is needed to use this class in bean serialization
	public float getX() {
		return x;
	}

	public void setX(float x) {
		this.x = x;
	}

	public float getY() {
		return y;
	}

	public void setY(float y) {
		this.y = y;
	}
//~ID
}