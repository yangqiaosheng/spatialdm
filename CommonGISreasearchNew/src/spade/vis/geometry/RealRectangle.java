package spade.vis.geometry;

import java.util.StringTokenizer;

public class RealRectangle extends Geometry {
	public float rx1 = Float.NaN, ry1 = Float.NaN, rx2 = Float.NaN, ry2 = Float.NaN;

	public void init(float x1, float y1, float x2, float y2) {
		if (x1 <= x2) {
			rx1 = x1;
			rx2 = x2;
		} else {
			rx1 = x2;
			rx2 = x1;
		}
		if (y1 <= y2) {
			ry1 = y1;
			ry2 = y2;
		} else {
			ry1 = y2;
			ry2 = y1;
		}
	}

	public RealRectangle() {
	}

	public RealRectangle(float x1, float y1, float x2, float y2) {
		init(x1, y1, x2, y2);
	}

	/**
	* Constructs a RealRectangle from an array of 4 float numbers:
	* 1) minimum x; 2) minimum y; 3) maximum x; 4) maximum y.
	*/
	public RealRectangle(float rect[]) {
		if (rect != null && rect.length >= 4) {
			init(rect[0], rect[1], rect[2], rect[3]);
		}
	}

	@Override
	public float[] getCentroid() {
		if (centroid == null && !Float.isNaN(rx1)) {
			setCentroid((rx1 + rx2) / 2, (ry1 + ry2) / 2);
		}
		return centroid;
	}

	/**
	* Constructs a RealRectangle from a string containing 4 float numbers
	* separated by commas:
	* 1) minimum x; 2) minimum y; 3) maximum x; 4) maximum y.
	*/
	public RealRectangle(String s) {
		StringTokenizer coord = new StringTokenizer(s, ",");
		rx1 = new Float(coord.nextToken()).floatValue();
		ry1 = new Float(coord.nextToken()).floatValue();
		rx2 = new Float(coord.nextToken()).floatValue();
		ry2 = new Float(coord.nextToken()).floatValue();
		if (rx1 > rx2) {
			float v = rx1;
			rx1 = rx2;
			rx2 = v;
		}
		if (ry1 > ry2) {
			float v = ry1;
			ry1 = ry2;
			ry2 = v;
		}
	}

	/**
	* The function allowing to determine the type of this geometry. Returns area.
	*/
	@Override
	public char getType() {
		return area;
	}

	/**
	* Determines the bounding rectangle of the geometry. Returns an array of 4
	* floats: 1) minimum x; 2) minimum y; 3) maximum x; 4) maximum y.
	* Uses the same static array for all instances!
	*/
	@Override
	public float[] getBoundRect() {
		if (Float.isNaN(rx1) || Float.isNaN(rx2) || Float.isNaN(ry1) || Float.isNaN(ry2))
			return null;
		bounds[0] = rx1;
		bounds[1] = ry1;
		bounds[2] = rx2;
		bounds[3] = ry2;
		return bounds;
	}

	/**
	* Returns the width of the geometry (in real coordinates).
	*/
	@Override
	public float getWidth() {
		return rx2 - rx1;
	}

	/**
	* Returns the height of the geometry (in real coordinates)
	*/
	@Override
	public float getHeight() {
		return ry2 - ry1;
	}

	@Override
	public boolean fitsInRectangle(float x1, float y1, float x2, float y2) {
		if (rx2 <= x1 || rx1 >= x2 || ry2 <= y1 || ry1 >= y2)
			return false;
		return true;
	}

	/**
	* Used to determine whether a considerable part of the geometry is contained
	* within the given rectangle (e.g.selection of objects in a map with a frame)
	*/
	@Override
	public boolean isInRectangle(float x1, float y1, float x2, float y2) {
		return rx1 >= x1 && rx2 <= x2 && ry1 >= y1 && ry2 <= y2;
	}

	/**
	* Checks if the point (x,y) belongs to the object. The point, in particular,
	* may represent the position of the mouse in the map.
	* Ignores the argument tolerateDist
	*/
	@Override
	public boolean contains(float x, float y, float tolerateDist) {
		return x >= rx1 && x <= rx2 && y >= ry1 && y <= ry2;
	}

	/**
	* Builds a minimum RealRectangle that includes both rectangles 
	*/
	public RealRectangle union(RealRectangle r) {
		if (r == null)
			return null;
		return new RealRectangle(Math.min(rx1, r.rx1), Math.min(ry1, r.ry1), Math.max(rx2, r.rx2), Math.max(ry2, r.ry2));
	}

	/**
	* Finds the intersection of the two rectangles
	*/
	public RealRectangle intersect(RealRectangle r) {
		if (r == null)
			return null;
		if (r.rx1 >= this.rx2 || r.rx2 <= this.rx1 || r.ry1 >= this.ry2 || r.ry2 <= this.ry1)
			return null; //the intersection is empty
		RealRectangle ir = new RealRectangle(Math.max(rx1, r.rx1), Math.max(ry1, r.ry1), Math.min(rx2, r.rx2), Math.min(ry2, r.ry2));
		return ir;
	}

	/**
	* Finds the intersection of the two rectangles
	*/
	public RealRectangle intersect(float x1, float y1, float x2, float y2) {
		if (x1 > x2) {
			float x = x1;
			x1 = x2;
			x2 = x;
		}
		if (y1 > y2) {
			float y = y1;
			y1 = y2;
			y2 = y;
		}
		if (x1 >= this.rx2 || x2 <= this.rx1 || y1 >= this.ry2 || y2 <= this.ry1)
			return null; //the intersection is empty
		RealRectangle ir = new RealRectangle(Math.max(rx1, x1), Math.max(ry1, y1), Math.min(rx2, x2), Math.min(ry2, y2));
		return ir;
	}

	public boolean doesIntersect(RealRectangle r) {
		if (r == null)
			return false;
		if (r.rx1 >= this.rx2 || r.rx2 <= this.rx1 || r.ry1 >= this.ry2 || r.ry2 <= this.ry1)
			return false; //the intersection is empty
		return true;
	}

	public boolean doesIntersect(float x1, float y1, float x2, float y2) {
		if (x1 > x2) {
			float x = x1;
			x1 = x2;
			x2 = x;
		}
		if (y1 > y2) {
			float y = y1;
			y1 = y2;
			y2 = y;
		}
		if (x1 >= this.rx2 || x2 <= this.rx1 || y1 >= this.ry2 || y2 <= this.ry1)
			return false; //the intersection is empty
		return true;
	}

	public float distance(RealRectangle r) {
		if (r == null)
			return Float.NaN;
		double dx = Math.max(Math.max(rx1 - r.rx2, r.rx1 - rx2), 0.0);
		double dy = Math.max(Math.max(ry1 - r.ry2, r.ry1 - ry2), 0.0);
		return (float) Math.sqrt(dx * dx + dy * dy);
	}

	@Override
	public Object clone() {
		return new RealRectangle(rx1, ry1, rx2, ry2);
	}

	@Override
	public String toString() {
		return rx1 + "," + ry1 + "," + rx2 + "," + ry2;
	}

}