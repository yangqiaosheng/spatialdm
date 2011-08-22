package spade.time.vis;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;

import spade.lib.basicwin.Metrics;

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: Aug 19, 2009
 * Time: 5:20:56 PM
 * To change this template use File | Settings | File Templates.
 */
public class TrajTimeGraphInfoCanvas extends Canvas {

	protected String strAttrName = null, strTimeRef = null;

	public void setAttrName(String strAttrName) {
		this.strAttrName = strAttrName;
		if (isShowing()) {
			redraw();
		}
	}

	public String getAttrName() {
		return strAttrName;
	}

	public void setTimeRef(String strTimeRef) {
		this.strTimeRef = strTimeRef;
		if (isShowing()) {
			redraw();
		}
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(100, Metrics.fh);
	}

	@Override
	public void paint(Graphics g) {
		draw(g);
	}

	public void redraw() {
		draw(getGraphics());
	}

	public void draw(Graphics g) {
		Dimension size = getSize();
		int w = size.width, h = size.height;
		Color bkgColor = getBackground();
		g.setColor(bkgColor);
		g.fillRect(0, 0, w + 1, h + 1);
		FontMetrics fm = g.getFontMetrics();
		int asc = fm.getAscent();
		g.setColor(Color.black);
		if (strAttrName != null) {
			g.drawString(strAttrName, 2, asc - 2);
		}
		g.setColor(Color.darkGray);
		if (strTimeRef != null && strTimeRef.length() > 0 && isShowing()) {
			int x = -1;
			Point p = getParent().getMousePosition();
			if (p != null) {
				x = (int) p.getX();
			}
			int sw = fm.stringWidth(strTimeRef);
			int xStr = (x >= 0) ? x : w - sw - 2;
			if (xStr + sw > w) {
				xStr = w - sw - 2;
			}
			g.setColor(bkgColor);
			g.fillRect(xStr, 0, sw, h + 1);
			g.setColor(Color.black);
			g.drawString(strTimeRef, xStr, asc - 2);
			if (x >= 0) {
				int xx[] = new int[3], yy[] = new int[3];
				xx[0] = x - 2;
				yy[0] = getSize().height - 3;
				xx[1] = x + 2;
				yy[1] = yy[0];
				xx[2] = x;
				yy[2] = yy[0] + 2;
				g.drawPolygon(xx, yy, 3);
			}
		}
	}

}
