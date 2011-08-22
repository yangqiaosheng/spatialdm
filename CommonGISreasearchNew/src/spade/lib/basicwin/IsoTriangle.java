package spade.lib.basicwin;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.Polygon;

/**
*  This class describes and draws arbitrary oriented isosceles triangle.
*  Orientation of triangle made by specifying its symmetry axis (x0,y0)-(x,y)
*/
public class IsoTriangle {

	protected Polygon p; // Representation of triangle
	public static float a = 5f, h = 8f; // Basis and height
	protected GeoLine l = null; // Symmetry axis
	protected int x, y; // Main summit (lies on symmetry axis)

	public IsoTriangle(int x0, int y0, int x, int y, int basis, int height) {
		if (!(basis == 0 || height == 0)) {
			a = basis;
			h = height;
		}
		p = new Polygon();
		l = new GeoLine(x0, y0, x, y);
		if (l != null && p != null && l.valid) {
			Point p1 = l.getArrow1(a, h);
			Point p2 = l.getArrow2(a, h);
			p.addPoint(x, y);
			p.addPoint(p1.x, p1.y);
			p.addPoint(p2.x, p2.y);
			p.addPoint(x, y);
		}
	}

	/**
	*  The next functions draw or/and fill arbitrary oriented isosceles triangle.
	*  Orientation of triangle made by specifying its symmetry axis (x0,y0)-(x,y)
	*  Different drawing and filling options set by parameters.
	*/

	/**
	* The function draws triangle with current color.
	*/
	public static void drawTriangle(Graphics g, int x0, int y0, int x, int y, int basis, int height) {

		IsoTriangle.drawTriangle(g, x0, y0, x, y, basis, height, false, true);
	}

	/**
	* The function draws first triangle and finally fills it with the same color
	*/
	public static void drawTriangle(Graphics g, int x0, int y0, int x, int y, int basis, int height, boolean fill) {

		IsoTriangle.drawTriangle(g, x0, y0, x, y, basis, height, fill, true);
	}

	/**
	* The function draws filled triangle using current color
	*/
	public static void fillTriangle(Graphics g, int x0, int y0, int x, int y, int basis, int height) {

		IsoTriangle.drawTriangle(g, x0, y0, x, y, basis, height, true, true);
	}

	/**
	*  The general function can draw triangle with current color and options
	*  - does not close trianle (looks like arrow)
	*  - does (not) fill triangle
	*/

	public static void drawTriangle(Graphics g, int x0, int y0, int x, int y, int basis, int height, boolean fill, boolean closed) {
		if (!(basis == 0 || height == 0)) {
			a = basis;
			h = height;
		}
		Polygon p = new Polygon();
		GeoLine l = new GeoLine(x0, y0, x, y);
		if (l != null && p != null && l.valid) {
			Point p1 = l.getArrow1(a, h);
			Point p2 = l.getArrow2(a, h);
			p.addPoint(x, y);
			p.addPoint(p1.x, p1.y);
			p.addPoint(p2.x, p2.y);
			p.addPoint(x, y);
			if (fill) {
				g.fillPolygon(p);
			}
			if (closed) {
				g.drawPolygon(p);
			} else {
				g.drawLine(x, y, p1.x, p1.y);
				g.drawLine(x, y, p2.x, p2.y);
			}
		}
	}

	public void drawTriangle(Graphics g) {
		this.drawTriangle(g, true);
	}

	public void drawTriangle(Graphics g, boolean closed) {
		if (p == null)
			return;
		if (closed) {
			g.drawPolygon(p);
		} else {
			Point p1 = l.getArrow1(a, h);
			Point p2 = l.getArrow2(a, h);
			g.drawLine(x, y, p1.x, p1.y);
			g.drawLine(x, y, p2.x, p2.y);
		}
	}

	public void fillTriangle(Graphics g) {
		if (p != null) {
			g.fillPolygon(p);
		}
	}

} // end of Class Triangle