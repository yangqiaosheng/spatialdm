package spade.vis.geometry;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

public class Triangle extends Sign {
	protected static int trX[] = new int[4], trY[] = new int[4];
	public boolean isPositive = true;

	/**
	* The constructor is used to set the variable usesMinSize to true and
	* also to set different default sizes
	*/
	public Triangle() {
		setMayChangeProperty(MAX_SIZE, true);
		setMayChangeProperty(MIN_SIZE, true);
		setMayChangeProperty(COLOR, true);
		setMayChangeProperty(BORDER_COLOR, true);
		usesMinSize = true;
		setSizes(Math.round(0.5f * mm), Math.round(0.5f * mm));
		setMinSizes(Math.round(0.5f * mm), Math.round(0.5f * mm));
		borderColor = color = Color.green.darker();
	}

	/**
	* This function is to be used for point objects.
	*/
	@Override
	public void draw(Graphics g, int x, int y) {
		drawTriangle(g, x, y, color, borderColor, isPositive);
	}

	/**
	* This function is to be used for area objects.
	* The rectangle in which to fit the diagram is specified.
	* Returns the bounding rectangle of the drawn diagram.
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
		drawTriangle(g, x + w / 2, y + h / 2/*+height/2*/, color, borderColor, isPositive);
	}

	public void drawTriangle(Graphics g, int x, int y, Color c, boolean positive) {
		drawTriangle(g, x, y, c, c, isPositive);
	}

	public void drawTriangle(Graphics g, int x, int y, Color c, Color borderC, boolean positive) {
		if (c != null) {
			g.setColor(c);
		}
		int dy = height;
		if (positive) {
			dy = -dy;
		}
		trX[0] = x;
		trY[0] = y + dy;
		trX[1] = x - width / 2;
		trY[1] = y;
		trX[2] = trX[1] + width;
		trY[2] = trY[1];
		trX[3] = trX[0];
		trY[3] = trY[0];
		g.setColor(java2d.Drawing2D.getTransparentColor(g.getColor(), transparency));
		g.fillPolygon(trX, trY, 4);
		if (borderC != null) {
			g.setColor(borderC);
		}
		g.drawPolygon(trX, trY, 4);
		labelX = x;
		labelY = (positive) ? y + 2 : y + height + 2;
	}

	public boolean toBeDrawnVCentered() {
		return false;
	}

	public boolean shouldBeHCentered() {
		return true;
	}
}
