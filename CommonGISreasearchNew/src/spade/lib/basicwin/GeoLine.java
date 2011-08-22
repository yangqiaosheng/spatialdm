package spade.lib.basicwin;

import java.awt.Graphics;
import java.awt.Point;

public class GeoLine {
	protected float x1 = 0f, y1 = 0f, x2 = 0f, y2 = 0f; // has this points on itself
	protected float dx = 0f, dy = 0f, sin = 0f, cos = 0f, l = 0f;
	public boolean valid = true;

	public GeoLine(int x0, int y0, int x, int y) {
		x1 = x0;
		x2 = x;
		y1 = y0;
		y2 = y;
		dx = x2 - x1;
		dy = y2 - y1;
		l = (float) Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2));
		if (dx != 0 || dy != 0) {
			sin = dy / l;
			cos = dx / l;
		} else {
			valid = false;
		}
	}

	public static float findDistance(float ax, float ay, float bx, float by) {
		return (float) Math.sqrt(Math.pow((ax - bx), 2) + Math.pow((ay - by), 2));
	}

	public Point findPointAtLine(float x0, float y0, float d) {
		return new Point(Math.round(x0 - d * cos), Math.round(y0 - d * sin));
	}

	public void drawLine(Graphics g) {
		g.drawLine(Math.round(x1), Math.round(y1), Math.round(x2), Math.round(y2));
	}

	public float getDX() {
		return dx;
	}

	public float getDY() {
		return dy;
	}

	public float getSin() {
		return sin;
	}

	public float getCos() {
		return cos;
	}

	public float getTan() {
		if (cos != 0f)
			return sin / cos;
		else
			return Float.MAX_VALUE;
	}

	public float getLength() {
		return l;
	}

	public Point getArrow1(float a, float h) {
		return new Point(Math.round(x2 - h * cos + (a / 2) * sin), Math.round(y2 - h * sin - (a / 2) * cos));
	}

	public Point getArrow2(float a, float h) {
		return new Point(Math.round(x2 - h * cos - (a / 2) * sin), Math.round(y2 - h * sin + (a / 2) * cos));
	}

} // end of class Line
