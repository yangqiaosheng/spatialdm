package spade.vis.geometry;

import java.awt.Polygon;
import java.util.Vector;

/**
 * 2004-01-03 AO
 * TODO: Comments
 * test gklein
 */
public class ThickLine {
	private RealVector[] vector_array;

	public ThickLine() {

	}

	public ThickLine(int[] x, int[] y) {
		this(x, y, Math.min(x.length, y.length));
	}

	public ThickLine(int[] x, int[] y, int npoints) {
		createRealVector(x, y, npoints);
	}

	public ThickLine(float[] x, float[] y) {
		this(x, y, Math.min(x.length, y.length));
	}

	public ThickLine(float[] x, float[] y, int npoints) {
		createRealVector(x, y, npoints);
	}

	private void createRealVector(int[] x, int[] y, int npoints) {
		Vector v = new Vector();
		RealVector v1 = null;
		RealVector v2 = null;

		v1 = new RealVector(x[npoints - 1], y[npoints - 1], x[0], y[0]);
		if (v1.getLength() != 0) {
			v.addElement(v1);
		}
		for (int i = 0; i < npoints - 1; i++) {
			v2 = new RealVector(x[i], y[i], x[i + 1], y[i + 1]);
			if (v2.getLength() != 0 && !v2.equals(v1)) {
				v.addElement(v2);
			}
			v1 = v2;
		}
		createRealVectorArray(v);
	}

	private void createRealVector(float[] x, float[] y, int npoints) {
		Vector v = new Vector();
		RealVector v1 = null;
		RealVector v2 = null;

		v1 = new RealVector(x[npoints - 1], y[npoints - 1], x[0], y[0]);
		if (v1.getLength() != 0) {
			v.addElement(v1);
		}
		for (int i = 0; i < npoints - 1; i++) {
			v2 = new RealVector(x[i], y[i], x[i + 1], y[i + 1]);
			if (v2.getLength() != 0 && !v2.equals(v1)) {
				v.addElement(v2);
			}
			v1 = v2;
		}
		createRealVectorArray(v);
	}

	private void createRealVectorArray(Vector v) {
		if (v.size() > 0) {
			vector_array = new RealVector[v.size() + 1];
			for (int i = 0; i < v.size(); i++) {
				vector_array[i] = (RealVector) v.elementAt(i);
			}
			vector_array[v.size()] = (RealVector) vector_array[0].clone();
		}
	}

	private Vector getPolygon(float w, int a) {
		int i = 0;
		Vector v = new Vector();
		RealVector v1 = vector_array[i];
		RealVector v2 = null;

		float scalar = 0.0f;
		float angle = 0.0f;

		while (i < vector_array.length - 1) {
			v2 = vector_array[++i];
			if (v2 != null && v2.getLength() != 0) {
				while (i < vector_array.length - 1 && (scalar = v1.getScalar(v2)) == 1) {
					v1.extend(v2);
					v2 = vector_array[++i];
				}

				angle = v1.getAngle(v2);
				RealPoint p = null;
				RealVector pv1 = v1.getParallelLine(a * w);
				RealVector pv2 = v2.getParallelLine(a * w);
				if (angle >= 30 && angle <= 150 && (p = pv1.getIntersection(pv2)) != null) {
					v.addElement(p);
				} else {
					v.addElement(pv1.getPoint2());
					v.addElement(pv2.getPoint1());
				}
				v1 = v2;
			}
		}
		return v;
	}

	public RealPolyline getRealPolyline(float width, boolean close) {
		RealPolyline l = new RealPolyline();
		if (vector_array != null) {
			Vector v1 = getPolygon(width / 2.0f, -1);
			Vector v2 = getPolygon(width / 2.0f, 1);

			int v1size = v1.size();
			int v2size = v2.size();

			if (v1size > 0 && v2size > 0) {
				int c = 0;
				if (close) {
					l.p = new RealPoint[v1size + v2size + 2];
				} else {
					l.p = new RealPoint[v1size + v2size];
				}

				for (int i = 0; i < v1size; i++) {
					l.p[c++] = (RealPoint) v1.elementAt(i);
				}

				if (close) {
					l.p[c++] = (RealPoint) v1.elementAt(0);
					l.p[c++] = (RealPoint) v2.elementAt(0);
				}

				for (int i = v2size - 1; i >= 0; i--) {
					l.p[c++] = (RealPoint) v2.elementAt(i);
				}

			}
		}
		return l;
	}

	public Polygon getPolygon(float width, boolean close) {

		RealPolyline l = getRealPolyline(width, close);
		Polygon p = new Polygon();

		if (l.p != null) {
			for (RealPoint element : l.p) {
				p.addPoint((int) element.x, (int) element.y);
			}
		}
		return p;
	}

	public Polygon getPolygon(int[] x, int[] y, int npoints, int width, boolean close) {
		Polygon polygon = null;

		if (close) {
			Polygon p = new Polygon();
			for (int i = 0; i < npoints; i++) {
				p.addPoint(x[i], y[i]);
			}
			if (width >= p.getBounds().width || width >= p.getBounds().height) {
				int w1 = width / 2;
				int w2 = width - w1;

				polygon = new Polygon();
				polygon.addPoint(p.getBounds().x - w1, p.getBounds().y - w1);
				polygon.addPoint(p.getBounds().x + w2, p.getBounds().y - w1);
				polygon.addPoint(p.getBounds().x + w2, p.getBounds().y + w2);
				polygon.addPoint(p.getBounds().x - w1, p.getBounds().y + w2);

			}
		}

		if (polygon == null) {
			createRealVector(x, y, npoints);
			polygon = getPolygon(width, close);
		}

		return polygon;
	}
}
