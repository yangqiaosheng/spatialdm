package spade.lib.basicwin;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

public class CrossDrawer implements Drawer {
	public static final int prefWidth = 12, prefHeight = 12;
	public Color color = Color.black;
	public boolean drawEnabled = true;
	protected int width = prefWidth, height = prefHeight, xMarg = 0, yMarg = 0;

	public void setPreferredSize(int width, int height) {
		this.width = width;
		this.height = height;
	}

	public void setMargins(int xMarg, int yMarg) {
		this.xMarg = xMarg;
		this.yMarg = yMarg;
	}

	public void setColor(Color color) {
		this.color = color;
	}

	/**
	* Makes the drawer to draw the icon in enabled or disabled state
	*/
	@Override
	public void setDrawEnabled(boolean value) {
		drawEnabled = value;
	}

	/**
	* Returns the preferred icon size
	*/
	@Override
	public Dimension getIconSize() {
		return new Dimension(width + 2 * xMarg, height + 2 * yMarg);
	}

	/**
	* Draws the icon of the preferred size at the specified position
	*/
	@Override
	public void draw(Graphics g, int x0, int y0) {
		draw(g, x0, y0, width, height, false);
	}

	/**
	* Draws the icon in a component with the specified size and origin.
	* The argument "stretch" indicated whether the icon should be stretched
	* if the component is larger than the preferred size of the icon.
	*/
	@Override
	public void draw(Graphics g, int x0, int y0, int w, int h, boolean stretch) {
		if (xMarg > 0 && w > 2 * xMarg) {
			x0 += xMarg;
			w -= 2 * xMarg;
		}
		if (yMarg > 0 && h > 2 * yMarg) {
			y0 += yMarg;
			h -= 2 * yMarg;
		}
		if (!stretch) {
			if (w > width) {
				x0 += (w - width) / 2;
				w = width;
			}
			if (h > height) {
				y0 += (h - height) / 2;
				h = height;
			}
		}
		g.setColor((drawEnabled) ? color : Color.gray);
		g.drawLine(x0, y0 + h, x0 + w, y0);
		g.drawLine(x0, y0 + h - 1, x0 + w - 1, y0);
		g.drawLine(x0 + 1, y0 + h, x0 + w, y0 + 1);
		g.drawLine(x0, y0, x0 + w, y0 + h);
		g.drawLine(x0 + 1, y0, x0 + w, y0 + h - 1);
		g.drawLine(x0, y0 + 1, x0 + w - 1, y0 + h);
	}
}