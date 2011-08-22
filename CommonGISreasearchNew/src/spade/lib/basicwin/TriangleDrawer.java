package spade.lib.basicwin;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

public class TriangleDrawer implements Drawer {
	public static final int prefWidth = 12, prefHeight = 12;
	public static final int N = 0, E = 1, S = 2, W = 3, NE = 4, NW = 5, SE = 6, SW = 7;
	protected static int trX[] = new int[4], trY[] = new int[4];
	public Color color = Color.black;
	public int direction = N;
	public boolean drawEnabled = true;

	protected int width = prefWidth, height = prefHeight, xMarg = 0, yMarg = 0;

	public TriangleDrawer() {
	}

	public TriangleDrawer(int dir) {
		setDirection(dir);
	}

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

	public void setDirection(int dir) {
		direction = dir;
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
		switch (direction) {
		case N:
			trX[0] = x0;
			trY[0] = y0 + h;
			trX[1] = trX[0] + w / 2;
			trY[1] = trY[0] - h;
			trX[2] = trX[0] + w;
			trY[2] = trY[0];
			trX[3] = trX[0];
			trY[3] = trY[0];
			break;
		case S:
			trX[0] = x0;
			trY[0] = y0;
			trX[1] = trX[0] + w / 2;
			trY[1] = trY[0] + h;
			trX[2] = trX[0] + w;
			trY[2] = trY[0];
			trX[3] = trX[0];
			trY[3] = trY[0];
			break;
		case E:
			trX[0] = x0;
			trY[0] = y0;
			trX[1] = trX[0] + w;
			trY[1] = trY[0] + h / 2;
			trX[2] = trX[0];
			trY[2] = trY[0] + h;
			trX[3] = trX[0];
			trY[3] = trY[0];
			break;
		case W:
			trX[0] = x0 + w;
			trY[0] = y0;
			trX[1] = trX[0] - w;
			trY[1] = trY[0] + h / 2;
			trX[2] = trX[0];
			trY[2] = trY[0] + h;
			trX[3] = trX[0];
			trY[3] = trY[0];
			break;
		case NE:
			trX[0] = x0 + w;
			trY[0] = y0;
			trX[1] = trX[0] - w;
			trY[1] = trY[0];
			trX[2] = trX[0];
			trY[2] = trY[0] + h;
			trX[3] = trX[0];
			trY[3] = trY[0];
			break;
		case NW:
			trX[0] = x0;
			trY[0] = y0;
			trX[1] = trX[0] + w;
			trY[1] = trY[0];
			trX[2] = trX[0];
			trY[2] = trY[0] + h;
			trX[3] = trX[0];
			trY[3] = trY[0];
			break;
		case SE:
			trX[0] = x0 + w;
			trY[0] = y0 + h;
			trX[1] = trX[0] - w;
			trY[1] = trY[0];
			trX[2] = trX[0];
			trY[2] = trY[0] - h;
			trX[3] = trX[0];
			trY[3] = trY[0];
			break;
		case SW:
			trX[0] = x0;
			trY[0] = y0 + h;
			trX[1] = trX[0] + w;
			trY[1] = trY[0];
			trX[2] = trX[0];
			trY[2] = trY[0] - h;
			trX[3] = trX[0];
			trY[3] = trY[0];
			break;
		}
		g.setColor((drawEnabled) ? color : Color.gray);
		g.fillPolygon(trX, trY, 4);
		g.drawPolygon(trX, trY, 4);
	}
}