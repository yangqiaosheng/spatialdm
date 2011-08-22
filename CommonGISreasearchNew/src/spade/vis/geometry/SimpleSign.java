package spade.vis.geometry;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;

import spade.lib.basicwin.Drawer;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Mar 8, 2010
 * Time: 12:34:13 PM
 * This is a class to implement geometric icons that may vary in shape and color.
 * No borders, no structure.
 */
public class SimpleSign implements Diagram, DrawableSymbol, Drawer {
	/**
	* Possible shapes
	*/
	public static final int SQUARE = 0, CIRCLE = 1, DIAMOND = 2, UP_TRIANGLE = 3, DOWN_TRIANGLE = 4, HOURGLASS_HOR = 5, HOURGLASS_VERT = 6, UP_ARC = 7, DOWN_ARC = 8, CROSS_STRAIGHT = 9, CROSS_45 = 10, STAR = 11, RING = 12, SHAPE_FIRST = 0,
			SHAPE_LAST = 12;
	public static float mm = Toolkit.getDefaultToolkit().getScreenResolution() / 25.33f;
	/**
	* Default size of the sign
	*/
	public static int defaultSize = Math.round(2 * mm);
	/**
	* The shape of this sign
	*/
	protected int shape = SHAPE_FIRST;
	/**
	* The color of the icon.
	*/
	protected Color iconColor = Color.red;
	/**
	* The width and height of the sign
	*/
	protected int width = defaultSize, height = defaultSize;
	/**
	* The position of the label
	*/
	protected int labelX = 0, labelY = 0;
	/**
	* The polygon used for drawing non-rectangular and non-round signs
	*/
	protected static int xp[] = null, yp[] = null;

	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public int getHeight() {
		return height;
	}

	/**
	* Returns the position of the label. This method should be called after draw(...).
	*/
	@Override
	public Point getLabelPosition() {
		return new Point(labelX, labelY);
	}

	public void setWidth(int w) {
		width = w;
	}

	public void setHeight(int h) {
		height = h;
	}

	@Override
	public void setSize(int size) {
		width = height = size;
	}

	public void setShape(int sh) {
		if (sh >= SHAPE_FIRST && sh <= SHAPE_LAST) {
			shape = sh;
		}
	}

	public int getShape() {
		return shape;
	}

	public Color getColor() {
		return iconColor;
	}

	public void setColor(Color fillColor) {
		this.iconColor = fillColor;
	}

	/**
	* This function is to be used for point objects.
	*/
	@Override
	public void draw(Graphics g, int x, int y) {
		if (g == null)
			return;
		int w = width, h = height, x0 = x - w / 2, y0 = y - h / 2;
		drawInRectangle(g, x0, y0, w, h, shape, iconColor);
		labelX = x0;
		labelY = y0 + height + 2;
	}

	/**
	* A utility function: draws the sign inside the given rectangle.
	*/
	protected static void drawInRectangle(Graphics g, int x0, int y0, int w, int h, int shape, Color color) {
		g.setColor(color);
		//allocate memory for the polygon vertices, if needed
		if ((xp == null || yp == null) && (shape == DIAMOND || shape == UP_TRIANGLE || shape == DOWN_TRIANGLE || shape == UP_ARC || shape == DOWN_ARC)) {
			xp = new int[5];
			yp = new int[5];
		}
		switch (shape) {
		case SQUARE:
			g.fillRect(x0, y0, w + 1, h + 1);
			break;
		case CIRCLE:
			g.fillOval(x0, y0, w, h);
			g.drawOval(x0, y0, w, h);
			break;
		case DIAMOND:
			xp[0] = x0;
			yp[0] = y0 + h / 2;
			xp[1] = x0 + w / 2;
			yp[1] = y0;
			xp[2] = x0 + w;
			yp[2] = yp[0];
			xp[3] = xp[1];
			yp[3] = y0 + h;
			xp[4] = xp[0];
			yp[4] = yp[0];
			g.fillPolygon(xp, yp, 4);
			g.drawPolygon(xp, yp, 5);
			break;
		case UP_TRIANGLE:
			xp[0] = x0;
			yp[0] = y0 + h;
			xp[1] = x0 + w / 2;
			yp[1] = y0;
			xp[2] = x0 + w;
			yp[2] = yp[0];
			xp[3] = xp[0];
			yp[3] = yp[0];
			g.fillPolygon(xp, yp, 3);
			g.drawPolygon(xp, yp, 4);
			break;
		case DOWN_TRIANGLE:
			xp[0] = x0;
			yp[0] = y0;
			xp[1] = x0 + w / 2;
			yp[1] = y0 + h;
			xp[2] = x0 + w;
			yp[2] = yp[0];
			xp[3] = xp[0];
			yp[3] = yp[0];
			g.fillPolygon(xp, yp, 3);
			g.drawPolygon(xp, yp, 4);
			break;
		case UP_ARC:
			//g.drawArc(x0,y0,w,h*2,0,180);
			g.fillArc(x0, y0, w, h * 2, 0, 180);
			break;
		case DOWN_ARC:
			//g.drawArc(x0,y0,w,h*2,180,180);
			g.fillArc(x0, y0 - h, w, h * 2, 180, 180);
			break;
		case HOURGLASS_HOR:
			xp[0] = x0;
			yp[0] = y0;
			xp[1] = x0 + w;
			yp[1] = y0 + h;
			xp[2] = x0 + w;
			yp[2] = y0;
			xp[3] = x0;
			yp[3] = y0 + h;
			xp[4] = xp[0];
			yp[4] = yp[0];
			g.fillPolygon(xp, yp, 4);
			g.drawPolygon(xp, yp, 5);
			break;
		case HOURGLASS_VERT:
			xp[0] = x0;
			yp[0] = y0;
			xp[1] = x0 + w;
			yp[1] = y0;
			xp[2] = x0;
			yp[2] = y0 + h;
			xp[3] = x0 + w;
			yp[3] = y0 + h;
			xp[4] = xp[0];
			yp[4] = yp[0];
			g.fillPolygon(xp, yp, 4);
			g.drawPolygon(xp, yp, 5);
			break;
		case STAR:
		case CROSS_STRAIGHT: {
			int lw = Math.round(Math.max(w, h) * 0.2f);
			if (lw < 2) {
				lw = 2;
			}
			int x1 = x0 + (w - lw + 1) / 2, y1 = y0 + (h - lw + 1) / 2;
			for (int i = 0; i < lw; i++) {
				g.drawLine(x1 + i, y0, x1 + i, y0 + h);
				g.drawLine(x0, y1 + i, x0 + w, y1 + i);
			}
			if (shape == CROSS_STRAIGHT) {
				break;
			}
		}
		case CROSS_45: {
			int lw = Math.round(Math.max(w, h) * 0.1f);
			if (lw < 1) {
				lw = 1;
			}
			g.drawLine(x0, y0, x0 + w, y0 + h);
			g.drawLine(x0, y0 + h, x0 + w, y0);
			for (int i = 1; i <= lw; i++) {
				g.drawLine(x0 + i, y0, x0 + w, y0 + h - i);
				g.drawLine(x0, y0 + i, x0 + w - i, y0 + h);
				g.drawLine(x0 + i, y0 + h, x0 + w, y0 + i);
				g.drawLine(x0, y0 + h - i, x0 + w - i, y0);
			}
			break;
		}
		case RING:
			g.drawOval(x0, y0, w, h);
			g.drawOval(x0 + 1, y0 + 1, w - 2, h - 2);
			break;
		}
	}

	/**
	* Draws the specified shape of default size at the given position (upper-left
	* corner of the icon). If frameColor is null, no frame is drawn. If fillColor
	* is null, the sign is not filled.
	*/
	protected static void drawShape(Graphics g, int x, int y, int shape, Color fillColor) {
		drawInRectangle(g, x, y, defaultSize, defaultSize, shape, fillColor);
	}

	/**
	* This function is to be used for area objects.
	* The rectangle in which to fit the diagram is specified.
	*/
	@Override
	public void draw(Graphics g, Rectangle r) {
		if (r == null)
			return;
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

	/**
	* Returns the preferred icon size
	*/
	@Override
	public Dimension getIconSize() {
		return new Dimension(width, height);
	}

	/**
	* Makes the drawer to draw the icon in enabled or disabled state
	*/
	@Override
	public void setDrawEnabled(boolean value) {
	}

	/**
	* Draws the icon in a component with the specified size and origin.
	* The argument "stretch" indicated whether the icon should be stretched
	* if the component is larger than the preferred size of the icon.
	*/
	@Override
	public void draw(Graphics g, int x0, int y0, int w, int h, boolean stretch) {
		draw(g, x0, y0, w, h);
	}
}
