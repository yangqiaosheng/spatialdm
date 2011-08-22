package spade.lib.basicwin;

import java.awt.Color;
import java.awt.Graphics;

public class Icons {
	public static final int mm = Math.round(java.awt.Toolkit.getDefaultToolkit().getScreenResolution() / 25.33f);
	public static final int th = Math.round(0.6f * mm), maxLength = 2 * mm;
	private static int px[] = null, py[] = null;

	public static void drawMinus(Graphics g, int x, int y, int w, int h) {
		g.setColor(Color.red.darker());
		int xmarg = mm / 2, length = w - 2 * xmarg, ymarg = (h - th) / 2;
		if (length < mm) {
			length = mm;
		} else if (length > maxLength) {
			length = maxLength;
		}
		xmarg = (w - length) / 2;
		g.fillRect(x + xmarg, y + ymarg, length + 1, th + 1);
	}

	public static void drawPlus(Graphics g, int x, int y, int w, int h) {
		g.setColor(Color.green.darker());
		int minw = (w < h) ? w : h;
		int marg = mm / 2, length = minw - 2 * marg;
		if (length < mm) {
			length = mm;
		} else if (length > maxLength) {
			length = maxLength;
		}
		int xmarg = (w - length) / 2, ymarg = (h - th) / 2;
		g.fillRect(x + xmarg, y + ymarg, length + 1, th + 1);
		xmarg = (w - th) / 2;
		ymarg = (h - length) / 2;
		g.fillRect(x + xmarg, y + ymarg, th + 1, length + 1);
	}

	public static void drawUnchecked(Graphics g, int x, int y, int w, int h) {
		int rw = 3 * mm;
		if (rw > w) {
			rw = w;
		}
		if (rw > h) {
			rw = h;
		}
		int xmarg = (w - rw) / 2, ymarg = (h - rw) / 2;
		for (int i = 0; i < 2 && rw >= 2 * mm; i++) {
			g.drawRect(x + xmarg, y + ymarg, rw, rw);
			++xmarg;
			++ymarg;
			rw -= 2;
		}
	}

	public static void drawChecked(Graphics g, int x, int y, int w, int h) {
		int rw = 3 * mm;
		if (rw > w) {
			rw = w;
		}
		if (rw > h) {
			rw = h;
		}
		int xmarg = (w - rw) / 2, ymarg = (h - rw) / 2;
		for (int i = 0; i < 2 && rw >= 2 * mm; i++) {
			g.drawRect(x + xmarg, y + ymarg, rw, rw);
			++xmarg;
			++ymarg;
			rw -= 2;
		}
		if (px == null || px.length < 5) {
			px = new int[5];
			py = new int[5];
		}
		px[0] = x + xmarg;
		py[0] = y + ymarg + rw / 2;
		px[1] = px[0] + rw / 2;
		py[1] = y + ymarg + rw;
		px[2] = px[0] + rw;
		py[2] = y + ymarg + 1;
		px[3] = px[1];
		py[3] = py[1] - 3;
		px[4] = px[0];
		py[4] = py[0];
		g.drawPolygon(px, py, 5);
		g.fillPolygon(px, py, 4);
	}

	/**
	* Draws a cross (3 pixels line width) with the given center using current
	* color.
	*/
	public static void drawCross(Graphics g, int centerX, int centerY, int w, int h) {
		int x = centerX - w / 2, y = centerY - h / 2;
		g.drawLine(x, y, x + w, y + h);
		g.drawLine(x + 1, y, x + w, y + h - 1);
		g.drawLine(x, y + 1, x + w - 1, y + h);
		g.drawLine(x, y + h, x + w, y);
		g.drawLine(x, y + h - 1, x + w - 1, y);
		g.drawLine(x + 1, y + h, x + w, y + 1);
	}

	/**
	* Draws a symbol of folder (closed). Uses current color of the Graphics.
	*/
	public static void drawClosedFolder(Graphics g, int x, int y, int w, int h, //the field to draw (incl. margins)
			int iconW, int iconH) //size of the icon
	{
		int mx = (w - iconW) / 2, my = (h - iconH) / 2;
		if (mx < 2) {
			mx = 2;
			iconW = w - mx * 2;
		}
		if (my < 2) {
			my = 2;
			iconH = h - my * 2;
		}
		int x1 = x + mx, yt = y + my, yb = yt + iconH, tabH = 2, tabW = 6, xgap = 4, xcgap = 1, ygap = 1, iw1 = iconW - xgap, ih1 = iconH - ygap - tabH;
		g.drawLine(x1, yt + tabH, x1, yb);
		g.drawLine(x1, yb, x1 + iw1 + xcgap, yb);
		g.drawLine(x1 + xcgap, yb, x1 + xcgap, yb - ih1);
		g.drawLine(x1 + xcgap, yb - ih1, x1 + xcgap + iw1, yb - ih1);
		g.drawLine(x1 + xcgap + iw1, yb, x1 + xcgap + iw1, yb - ih1);
		g.drawLine(x1, yt + tabH, x1 + 1, yt);
		g.drawLine(x1 + 1, yt, x1 + tabW - 1, yt);
		g.drawLine(x1 + tabW - 1, yt, x1 + tabW, yt + tabH);
		g.drawLine(x1 + tabW, yt + tabH, x1 + iw1, yt + tabH);
		g.drawLine(x1 + iw1, yt + tabH, x1 + iw1, yb - ih1);
	}

	/**
	* Draws a symbol of folder (open). Uses current color of the Graphics.
	*/
	public static void drawOpenFolder(Graphics g, int x, int y, int w, int h, //the field to draw (incl. margins)
			int iconW, int iconH) //size of the icon
	{
		int mx = (w - iconW) / 2, my = (h - iconH) / 2;
		if (mx < 2) {
			mx = 2;
			iconW = w - mx * 2;
		}
		if (my < 2) {
			my = 2;
			iconH = h - my * 2;
		}
		int x1 = x + mx, yt = y + my, yb = yt + iconH, tabH = 2, tabW = 6, xgap = 4, ygap = 2, iw1 = iconW - xgap, ih1 = iconH - ygap - tabH;
		g.drawLine(x1, yt + tabH, x1, yb);
		g.drawLine(x1, yb, x1 + iw1, yb);
		g.drawLine(x1, yb, x1 + xgap, yb - ih1);
		g.drawLine(x1 + xgap, yb - ih1, x1 + iconW, yb - ih1);
		g.drawLine(x1 + iw1, yb, x1 + iconW, yb - ih1);
		g.drawLine(x1, yt + tabH, x1 + 1, yt);
		g.drawLine(x1 + 1, yt, x1 + tabW - 1, yt);
		g.drawLine(x1 + tabW - 1, yt, x1 + tabW, yt + tabH);
		g.drawLine(x1 + tabW, yt + tabH, x1 + iw1, yt + tabH);
		g.drawLine(x1 + iw1, yt + tabH, x1 + iw1, yb - ih1);
	}

	/**
	* Draws a circle with the given diameter. Uses current color of the Graphics.
	*/
	public static void drawCircle(Graphics g, int x, int y, int w, int h, //the field to draw (incl. margins)
			int diam) //size of the icon
	{
		int mx = (w - diam) / 2, my = (h - diam) / 2;
		g.drawOval(x + mx, y + my, diam, diam);
		g.drawOval(x + mx + 1, y + my + 1, diam - 2, diam - 2);
	}
}