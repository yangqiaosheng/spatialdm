package spade.vis.mapvis;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;

import spade.lib.basicwin.Metrics;
import spade.lib.color.CS;
import spade.vis.database.ThematicDataItem;

/**
* Represents numeric values by colors used for painting polygons
*/

public class NumValuePainter extends NumberDrawer {
	/**
	* This method is called by a GeoObject. The GeoObject sends its DataItem
	* to the Visualizer, and the Visualizer returns an Object specifying the
	* presentation of data contained in this DataItem. This may be, in
	* particular, Color or Diagram. Typically the argument DataItem is
	* a ThematicDataItem, although in some cases (e.g. manual classification)
	* this is not required.
	*/
	@Override
	public Object getPresentation(ThematicDataItem item) {
		double v = getNumericAttrValue(item, 0);
		if (Double.isNaN(v))
			return null;
		Object result = CS.makeColor(focuserMin, focuserMax, v, cmpTo, posHue, negHue);
		if (result == null) {
			result = checkMakeTriangle(v);
		}
		/*
		else {
		  Color c=(Color)result;
		  int alpha = 100, //between 0 and 255
		      alphaFactor= (alpha << 24) | 0x00FFFFFF;
		  result=new Color(c.getRGB() & alphaFactor,true);
		}
		*/
		return result;
	}

	/**
	* Replies whether color shades (degrees of darkness) are used for representing
	* attribute values. Returns true.
	*/
	@Override
	public boolean usesShades() {
		return true;
	}

	/**
	* This method informs whether the Visualizer produces diagrams.
	* This is important for defining the order of drawing of GeoLayers on the
	* map: the diagrams should be drawn on top of all geography.
	*/
	@Override
	public boolean isDiagramPresentation() {
		return false;
	}

	/**
	* Draws a small picture symbolizing this presentation technique
	*/
	@Override
	public void drawIcon(Graphics g, int x, int y, int w, int h) {
		g.setColor(CS.getColor(0.8f, posHue));
		g.fillRect(x, y, w, h);
		int dx = 4 * w / 5, dy = h;
		g.setColor(CS.getColor(0.7f, negHue));
		g.fillArc(x - dx, y, 2 * dx, 2 * dy, 0, 90);
		g.setColor(Color.black);
		g.drawArc(x - dx, y, 2 * dx, 2 * dy, 0, 90);
		dy = h / 2;
		dx = 3 * w / 5;
		g.setColor(CS.getColor(0.5f, posHue));
		g.fillArc(x + w - dx, y + h - dy, 2 * dx, 2 * dy, 90, 90);
		g.setColor(Color.black);
		g.drawArc(x + w - dx, y + h - dy, 2 * dx, 2 * dy, 90, 90);
		g.drawRect(x, y, w, h);
	}

	/**
	* Draws the part of the legend that explains how numeric attribute values
	* are graphically encoded on the map, assuming that the name of the attribute
	* is already shown.
	*/
	@Override
	protected Rectangle showNumberEncoding(Graphics g, int startY, int leftMarg, int prefW) {
		int y = startY, maxX = 0;
		int totalW = prefW - leftMarg - 3, nSteps = 50, barH = 4 * Metrics.mm();
		if (totalW / nSteps < 3) {
			nSteps = totalW / 3;
		}

		double v = focuserMin;
		int x = 0;
		for (int i = 0; i < nSteps; i++) {
			g.setColor(CS.makeColor(focuserMin, focuserMax, v, cmpTo, posHue, negHue));
			double v1 = v + (focuserMax - v) / (nSteps - i);
			if (v < cmpTo && v1 > cmpTo) {
				v1 = cmpTo;
			}
			int dx = (int) Math.round((v1 - focuserMin) * totalW / (focuserMax - focuserMin)) - x;
			g.fillRect(leftMarg + x, y, dx + 1, barH);
			x += dx;
			v = v1;
		}
		if (leftMarg + totalW > maxX) {
			maxX = leftMarg + totalW;
		}
		int by = y + barH;

		FontMetrics fm = g.getFontMetrics();
		int fh = fm.getHeight(), asc = fm.getAscent();
		y = by + Metrics.mm() / 2;

		String str = getFocuserMinAsString();
		g.setColor(Color.black);
		g.drawString(str, leftMarg, y + asc);
		g.drawLine(leftMarg, by, leftMarg, y);
		y += fh;
		int sw = fm.stringWidth(str);
		if (maxX < leftMarg + sw) {
			maxX = leftMarg + sw;
		}
		if (focuserMax > cmpTo && focuserMin < cmpTo) {
			int cx = leftMarg + (int) Math.round((cmpTo - focuserMin) * totalW / (focuserMax - focuserMin));
			str = getCmpAsString();
			sw = fm.stringWidth(str);
			x = cx;
			if (x + sw > leftMarg + prefW) {
				x = leftMarg + prefW - sw;
			}
			g.drawString(str, x, y + asc);
			g.drawLine(cx, by, cx, y);
			y += fh;
			if (maxX < x + sw) {
				maxX = x + sw;
			}
		}
		str = getFocuserMaxAsString();
		sw = fm.stringWidth(str);
		x = leftMarg + totalW - sw - 2;
		if (x < leftMarg) {
			x = leftMarg;
		}
		g.drawString(str, x, y + asc);
		g.drawLine(leftMarg + totalW, by, leftMarg + totalW, y);
		y += fh;
		if (maxX < x + sw) {
			maxX = x + sw;
		}

		return new Rectangle(leftMarg, startY, maxX - leftMarg, y - startY);
	}

	/**
	* Must reply whether any parameters of this visualization method may be
	* changed, e.g. colors or sizes of signs. This does not include interactive
	* analytical manipulation.
	* Returns true.
	*/
	@Override
	public boolean canChangeParameters() {
		return true;
	}

	/**
	* Constructs and displays a dialog for changing parameters of this
	* visualization method: color hues.
	*/
	@Override
	public void startChangeParameters() {
		startChangeColors();
	}
}