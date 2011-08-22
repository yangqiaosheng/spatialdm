package spade.lib.basicwin;

import java.awt.Graphics;

import spade.vis.geometry.ThickLine;

public class Drawing {
	public static final int mm = Math.round(java.awt.Toolkit.getDefaultToolkit().getScreenResolution() / 25.33f);
//iitp
	protected static int x[] = new int[6], y[] = new int[6];
//~iitp

	/**
	 *  2004-23-01 AO
	 *  Use ThickLine implementation for drawing Polygons/Polylines
	 */
	protected static ThickLine thickline = new ThickLine();

	//-------------------- drawLine --------------------
	/**
	* Draw a line(segment) with the given width.
	* Source: Frank Tuinman (PGS)
	*/
	public static void drawLine(Graphics g, int width, int x1, int y1, int x2, int y2, boolean first, boolean last) {
		if (width <= 1) {
			g.drawLine(x1, y1, x2, y2);
			return;
		}
		int dx = x2 - x1;
		int dy = y2 - y1;
		if (dx == 0 && dy == 0)
			return;
		/* long pyth -> double pyth */
		double pyth = ((double) dx * (double) dx + (double) dy * (double) dy);
		double length;
		if (pyth == 0)
			return;
		if (pyth == 1) {
			length = 1;
		} else {
			length = Math.sqrt(pyth);
		}
		length = 2 * length / width;
		double ddx = (dx / length);
		double ddy = (dy / length);
		if (first) {
			x[0] = (int) (x1 - ddy + 0.50 - ddx);
			y[0] = (int) (y1 + ddx + 0.50 - ddy);
			x[3] = (int) (x1 + ddy + 0.50 - ddx);
			y[3] = (int) (y1 - ddx + 0.50 - ddy);
		} else {
			x[0] = (int) (x1 - ddy + 0.50);
			y[0] = (int) (y1 + ddx + 0.50);
			x[3] = (int) (x1 + ddy + 0.50);
			y[3] = (int) (y1 - ddx + 0.50);
		}

		if (last) {
			x[1] = (int) (x2 - ddy + 0.50 + ddx);
			y[1] = (int) (y2 + ddx + 0.50 + ddy);
			x[2] = (int) (x2 + ddy + 0.50 + ddx);
			y[2] = (int) (y2 - ddx + 0.50 + ddy);
		} else {
			x[1] = (int) (x2 - ddy + 0.50);
			y[1] = (int) (y2 + ddx + 0.50);
			x[2] = (int) (x2 + ddy + 0.50);
			y[2] = (int) (y2 - ddx + 0.50);
		}

		g.fillPolygon(x, y, 4);
		g.drawPolygon(x, y, 4);
	}

	/**
	 * Draws a directed line segment (vector) of the given width
	 */
	public static void drawVector(Graphics g, int width, int x1, int y1, int x2, int y2, int arrowHeight, int arrowWidth, boolean arrowFilled) {
		if (x1 == x2 && y1 == y2) {
			int r = width / 2;
			if (r < 1) {
				r = 1;
				width = 2;
			}
			g.fillOval(x1 - r, y1 - r, width + 1, width + 1);
			return;
		}
		/* replaced by one-side arrow, see below
		IsoTriangle.drawTriangle(g,x1,y1,x2,y2,arrowWidth,arrowHeight,arrowFilled,arrowFilled);
		int dx=x2-x1, dy=y2-y1;
		double dist=Math.sqrt(1d*dx*dx+1d*dy*dy);
		if (dist>arrowHeight) {
		  double ratio=(dist-arrowHeight+1)/dist;
		  int x0=x1+(int)Math.round(ratio*dx), y0=y1+(int)Math.round(ratio*dy);
		  drawLine(g,width,x1,y1,x0,y0,false,false);
		}
		*/
		int dx = x2 - x1, dy = y2 - y1;
		double dist = Math.sqrt(1d * dx * dx + 1d * dy * dy);
		double ddx = dx / dist, ddy = dy / dist;
		int x[], y[];
		if (dist > arrowHeight) { // full arrow
			double ratio = (dist - arrowHeight + 1) / dist;
			x = new int[5];
			y = new int[5];
			x[0] = x1;
			y[0] = y1;
			x[4] = x[0] + x2 - x1;
			y[4] = y[0] + y2 - y1;
			if (width > 1) {
				x[1] = x1 - (int) Math.ceil(width * ddy);
				y[1] = y1 + (int) Math.ceil(width * ddx);
			} else {
				x[1] = x1;
				y[1] = y1;
			}
			x[2] = x[1] + (int) Math.round(ratio * dx);
			y[2] = y[1] + (int) Math.round(ratio * dy);
			if (arrowWidth <= width) {
				x[3] = x[2];
				y[3] = y[2];
			} else {
				double aw = Math.max(2d, arrowWidth - width);
				x[3] = x[2] - (int) Math.round(aw * ddy);
				y[3] = y[2] + (int) Math.round(aw * ddx);
			}
		} else { // just triangle
			x = new int[3];
			y = new int[3];
			x[0] = x1;
			y[0] = y1;
			x[2] = x[0] + x2 - x1;
			y[2] = y[0] + y2 - y1;
			int aw = Math.max(arrowWidth, width);
			x[1] = x1 - (int) Math.round(aw * ddy);
			y[1] = y1 + (int) Math.round(aw * ddx);
		}
		for (int i = 0; i < x.length; i++) { // displacement to avoid ovelap of arrow
			x[i] -= (int) Math.ceil(1.5d * ddy);
			y[i] += (int) Math.ceil(1.5d * ddx);
		}
		g.drawPolygon(x, y, x.length);
		g.fillPolygon(x, y, x.length);
	}

	public static void drawPolyline(Graphics g, int width, int xcoord[], int ycoord[], int npoints, boolean toClose) {
		if (width < 2) {
			g.drawPolyline(xcoord, ycoord, npoints);
		} else {
			/**
			  * Put the linedrawing into a try block, just for the case I was not careful
			  * enough.
			  */
			try {
				g.fillPolygon(thickline.getPolygon(xcoord, ycoord, npoints, width, toClose));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static void drawRectangle(Graphics g, int lineWidth, int x, int y, int width, int height) {
		if (lineWidth < 2) {
			g.drawRect(x, y, width, height);
		} else {
//iitp
			x += lineWidth / 2;
			y += lineWidth / 2;
			width -= lineWidth;
			height -= lineWidth;
//~iitp
			int k = 0;
			while (k < lineWidth) {
				g.drawRect(x, y, width, height);
				--x;
				--y;
				width += 2;
				height += 2;
				++k;
			}
		}
	}

	public static void drawOval(Graphics g, int lineWidth, int x, int y, int width, int height) {
		if (lineWidth < 2) {
			g.drawOval(x, y, width, height);
		} else {
			int k = 0;
			while (k < lineWidth) {
				g.drawOval(x, y, width, height);
				--x;
				--y;
				width += 2;
				height += 2;
				++k;
			}
		}
	}

	/**
	* Uses the current color
	*/
	public static void drawVerticalArrow(Graphics g, int x, int y, int w, int h, boolean up, boolean down) //may be double-ended
	{
		int xpos = x + w / 2, aw = mm / 2, ah = mm;
		if (aw > w / 2) {
			aw = w / 2;
		}
		if (h - 2 * ah < mm) {
			ah = (h - mm) / 2;
		}
		g.drawLine(xpos, y, xpos, y + h);
		if (up) {
			g.drawLine(xpos, y, xpos - aw, y + ah);
			g.drawLine(xpos, y, xpos + aw, y + ah);
		}
		if (down) {
			g.drawLine(xpos, y + h, xpos - aw, y + h - ah);
			g.drawLine(xpos, y + h, xpos + aw, y + h - ah);
		}
	}

	/**
	* Uses the current color
	*/
	public static void drawHorizontalArrow(Graphics g, int x, int y, int w, int h, boolean left, boolean right) //may be double-ended
	{
		int ypos = y + h / 2, aw = mm / 2, ah = mm;
		if (aw > h / 2) {
			aw = h / 2;
		}
		if (w - 2 * ah < mm) {
			ah = (w - mm) / 2;
		}
		g.drawLine(x, ypos, x + w, ypos);
		if (left) {
			g.drawLine(x, ypos, x + ah, ypos - aw);
			g.drawLine(x, ypos, x + ah, ypos + aw);
		}
		if (right) {
			g.drawLine(x + w, ypos, x + w - ah, ypos - aw);
			g.drawLine(x + w, ypos, x + w - ah, ypos + aw);
		}
	}

	/**
	* The function draws classic arrow with the current color
	*/
	public static void drawArrow(Graphics g, int x0, int y0, int x, int y, int width, int height, boolean left, boolean right) //may be double-ended
	{
		g.drawLine(x0, y0, x, y);
		if (left) {
			IsoTriangle.drawTriangle(g, x, y, x0, y0, height, width, false, false);
		}
		if (right) {
			IsoTriangle.drawTriangle(g, x0, y0, x, y, height, width, false, false);
		}
	}

	/**
	* The function draws filled arrow with the current color
	*/
	public static void fillArrow(Graphics g, int x0, int y0, int x, int y, int width, int height, boolean left, boolean right) //may be double-ended
	{
		g.drawLine(x0, y0, x, y);
		if (left) {
			IsoTriangle.drawTriangle(g, x, y, x0, y0, height, width, true, true);
		}
		if (right) {
			IsoTriangle.drawTriangle(g, x0, y0, x, y, height, width, true, true);
		}
	}

	/**
	* The function draws sector: an arc with connecting lines to its center.
	* Current color used
	*/
	public static void drawSector(Graphics g, int x0, int y0, int diam, int startAngle, int arcLength) {
		int dif = 180 - startAngle;
		int x1 = x0 - (int) Math.round(diam * Math.cos(dif * Math.PI / 180) / 2), y1 = y0 - (int) Math.round(diam * Math.sin(dif * Math.PI / 180) / 2);
		g.drawLine(x0, y0, x1, y1);
		dif -= arcLength;
		x1 = x0 - (int) Math.round(diam * Math.cos(dif * Math.PI / 180) / 2);
		y1 = y0 - (int) Math.round(diam * Math.sin(dif * Math.PI / 180) / 2);
		g.drawLine(x0, y0, x1, y1);
		g.drawArc(x0 - diam / 2, y0 - diam / 2, diam, diam, startAngle, arcLength);
	}

	/**
	* The function draws filled sector: first, fills the corresponding arc,
	* then draws it and connecting lines to its center.
	* Current color used.
	*/
	public static void fillSector(Graphics g, int x0, int y0, int diam, int startAngle, int arcLength) {
		g.fillArc(x0 - diam / 2, y0 - diam / 2, diam, diam, startAngle, arcLength);
		drawSector(g, x0, y0, diam, startAngle, arcLength);
	}
}
