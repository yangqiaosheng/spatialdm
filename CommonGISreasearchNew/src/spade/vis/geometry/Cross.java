package spade.vis.geometry;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;

import spade.lib.basicwin.Metrics;

public class Cross implements Diagram {
	public static int mm = 0;
	protected int w = 0, h = 0;
	protected Color color = Color.orange;
	/**
	* The position of the label
	*/
	protected int labelX = 0, labelY = 0;

	public Cross() {
		if (mm < 0) {
			mm = Metrics.mm();
		}
		w = 2 * mm;
		h = 2 * mm;
	}

	@Override
	public int getWidth() {
		return w;
	}

	@Override
	public int getHeight() {
		return h;
	}

	/**
	* Returns the position of the label. This method should be called after draw(...).
	*/
	@Override
	public Point getLabelPosition() {
		return new Point(labelX, labelY);
	}

	public void setWidth(int width) {
		w = width;
	}

	public void setHeight(int height) {
		h = height;
	}

	public void setColor(Color c) {
		color = c;
	}

	/**
	* This function is to be used for point objects.
	*/
	@Override
	public void draw(Graphics g, int cx, int cy) {
		int x = cx - w / 2, y = cy - h / 2;
		g.setColor(color);
		g.drawLine(x, y, x + w, y + h);
		g.drawLine(x + 1, y, x + w, y + h - 1);
		g.drawLine(x, y + 1, x + w - 1, y + h);
		g.drawLine(x, y + h, x + w, y);
		g.drawLine(x, y + h - 1, x + w - 1, y);
		g.drawLine(x + 1, y + h, x + w, y + 1);
		labelX = x;
		labelY = y + h + 2;
	}

	/**
	* This function is to be used for area objects.
	* The rectangle in which to fit the diagram is specified.
	*/
	@Override
	public void draw(Graphics g, Rectangle r) {
		draw(g, r.x, r.y, r.width, r.height);
	}

	/**
	* Same as previous, only the arguments are not wrapped into a Rectangle object
	*/
	@Override
	public void draw(Graphics g, int x, int y, int w, int h) {
		draw(g, x + w / 2, y + h / 2);
	}

	/**
	* Replies whether this diagram is centered, i.e. the center of the diagram
	* coinsides with the center of the object
	*/
	@Override
	public boolean isCentered() {
		return true;
	}
}
