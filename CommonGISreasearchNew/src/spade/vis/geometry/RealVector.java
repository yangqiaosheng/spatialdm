package spade.vis.geometry;

/*
 * This class represents a (mathematic) vector
 * I think it is a good idea, to double check the
 * used mathmetics to ensure everything is correct.
 */

public class RealVector {
	/* startpoint */
	private RealPoint point1;
	/* endpoint   */
	private RealPoint point2;

	public RealVector() {
		this(0, 0, 0, 0);
	}

	public RealVector(int x1, int y1, int x2, int y2) {
		this((float) x1, (float) y1, (float) x2, (float) y2);
	}

	public RealVector(float x1, float y1, float x2, float y2) {
		this.point1 = new RealPoint(x1, y1);
		this.point2 = new RealPoint(x2, y2);
	}

	public RealVector(RealPoint point1, RealPoint point2) {
		/*
		 * Operations on this RealVector can change x/y values
		 * of point1/point2. Better clone the points to avoid
		 * side effects on the original point1/point2.
		 */
		this.point1 = (RealPoint) point1.clone();
		this.point2 = (RealPoint) point2.clone();
	}

	public RealPoint getPoint1() {
		return point1;
	}

	public RealPoint getPoint2() {
		return point2;
	}

	/**
	 * Returns the length of this RealVector;
	 */
	public float getLength() {
		float dx = point2.x - point1.x;
		float dy = point2.y - point1.y;

		return (float) Math.sqrt(dx * dx + dy * dy);
	}

	/**
	 * Returns the direction (Richtungsvektor) of this RealVector as RealPoint
	 */
	public RealPoint getDirection() {
		float dx = point2.x - point1.x;
		float dy = point2.y - point1.y;

		return new RealPoint(dx, dy);
	}

	/**
	 * Returns a new Point at the distance d from point1
	 */
	public RealPoint getPointAt(float d) {
		float l = getLength();
		RealPoint dir = getDirection();

		float x = point1.x + d * (dir.x / l);
		float y = point1.y + d * (dir.y / l);

		return new RealPoint(x, y);
	}

	/*
	 * Returns the scalar product of two RealVector's
	 */
	public float getScalar(RealVector v) {
		float l1 = getLength();
		float l2 = v.getLength();

		RealPoint d1 = getDirection();
		RealPoint d2 = v.getDirection();

		return (d1.x * d2.x + d1.y * d2.y) / (l1 * l2);
	}

	/*
	 * Construct a new RealVector, which is parallel to this
	 * RealVector at a distance d
	 */
	public RealVector getParallelLine(float d) {
		float nx1, nx2, ny1, ny2;

		RealPoint direction = getDirection();
		float length = getLength();

		nx1 = point1.x + d * (-1 * direction.y / length);
		ny1 = point1.y + d * (direction.x / length);

		nx2 = point2.x + d * (-1 * direction.y / length);
		ny2 = point2.y + d * (direction.x / length);

		return new RealVector(nx1, ny1, nx2, ny2);
	}

	/*
	 * Returns the angle between this RealVector and v.
	 * check for NaN and Infinte values, if you use this
	 * method!
	 */
	public float getAngle(RealVector v) {
		return (float) (180.0 / Math.PI * Math.acos(getScalar(v)));
	}

	/*
	 * Returns the intersection point of two Vectors, if any,
	 * otherwise null.
	 *
	 *           /x1\         /dx\
	 * vector1 =|    | + s * |    |
	 *           \y1/         \dy/
	 *
	 * 
	 *           /p1\         /dp\
	 * vector2 =|    | + t * |    |
	 *           \q1/         \dq/
	 *
	 *
	 * ==> 
	 *    x1 + s * dx = p1 + t * dp
	 *    y1 + s * dy = q2 + t * dq
	 */
	public RealPoint getIntersection(RealVector v) {
		float v1 = getLength();
		float v2 = v.getLength();

		RealPoint d1 = getDirection();
		RealPoint d2 = v.getDirection();

		float x1 = point1.x;
		float y1 = point1.y;
		float x2 = point2.x;
		float y2 = point2.y;

		float p1 = v.getPoint1().x;
		float q1 = v.getPoint1().y;
		float p2 = v.getPoint2().x;
		float q2 = v.getPoint2().y;

		float dx = x2 - x1;
		float dy = y2 - y1;

		float dp = p2 - p1;
		float dq = q2 - q1;

		float ix, iy;

		if ((dx == 0 && dp == 0) || (dy == 0 && dq == 0) || ((x2 == p1) && (y2 == q1))) {
			ix = p2;
			iy = q1;
		} else if (dx == 0) {
			float s = (x1 - p1) / dp;
			ix = p1 + s * dp;
			iy = q1 + s * dq;
		} else if (dy == 0) {
			float s = (y1 - q1) / dq;
			ix = p1 + s * dp;
			iy = q1 + s * dq;
		} else if (dp == 0) {
			float t = (p1 - x1) / dx;
			ix = x1 + t * dx;
			iy = y1 + t * dy;
		} else if (dq == 0) {
			float t = (q1 - y1) / dy;
			ix = x1 + t * dx;
			iy = y1 + t * dy;
		} else {
			float s = (((p1 - x1) * dy / dx) - (q1 - y1)) / ((-1 * (dp * dy) / dx) + dq);
			ix = p1 + s * dp;
			iy = q1 + s * dq;
		}

		RealPoint ip = null;
		if (!(Float.isNaN(ix) || Float.isNaN(iy) || Float.isInfinite(ix) || Float.isInfinite(iy))) {
			ip = new RealPoint(ix, iy);
		}
		return ip;
	}

	public boolean contains(RealPoint v) {

		float t1 = 0;
		float t2 = 0;
		float dx = point2.x - point1.x;
		float dy = point2.y - point1.y;

		if (dx != 0) {
			t1 = (v.x - point1.x) / dx;
		} else {
			t1 = (point1.x == v.x ? 0 : -1);
		}

		if (dy != 0) {
			t2 = (v.y - point1.y) / dy;
		} else {
			t2 = (point1.y == v.y ? 0 : -1);
		}
		return (t1 > 0.0 && t1 < 1.0 && t2 >= 0.0 && t2 <= 1.0);
	}

	public void extend(RealVector v) {
		RealPoint vp1 = v.getPoint1();
		RealPoint vp2 = v.getPoint2();

		if (point2.x == vp1.x && point2.y == vp1.y) {
			point2 = (RealPoint) vp2.clone();
		} else if (point1.x == vp2.x && point1.y == vp2.y) {
			point1 = (RealPoint) vp1.clone();
		} else {
			if (!(contains(v.getPoint1()) && contains(v.getPoint2()))) {
				float dx = v.getPoint2().x - point2.x;
				float dy = v.getPoint2().y - point2.y;
				point2.x += dx; //v.getDirection().x;
				point2.y += dy; //v.getDirection().y;
			}
		}
	}

	public boolean equals(RealVector v) {
		return point1.x == v.getPoint1().x && point1.y == v.getPoint1().y && point2.x == v.getPoint2().x && point2.y == v.getPoint2().y;
	}

	@Override
	public Object clone() {
		return new RealVector(point1, point2);
	}

	@Override
	public String toString() {
		return getClass().getName() + "[x1=" + point1.x + "," + "y1=" + point1.y + "," + "x2=" + point2.x + "," + "y2=" + point2.y + "]";
	}

}
