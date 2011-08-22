package spade.analysis.manipulation;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import spade.lib.basicwin.Drawable;
import spade.lib.util.Aligner;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 29-Jan-2007
 * Time: 15:55:33
 * Used in a manipulator of a visualiser where sizes (lengths, widths, etc.)
 * vary from minimum to maximum.
 */
public class Trapezoid implements Drawable, PropertyChangeListener {
	public static final int mm = Math.round(java.awt.Toolkit.getDefaultToolkit().getScreenResolution() / 25.33f);
	protected static int X[] = new int[5], Y[] = new int[5];
	/**
	* Indicates the orientation of the object
	*/
	protected boolean isHorizontal = true;
	/**
	* The canvas in which this object should draw itself
	*/
	protected Canvas canvas = null;
	/**
	* The boundaries in which the object should fit itself
	* origBounds are used in case of alignment with other graphics
	*/
	protected Rectangle bounds = null, origBounds = null;
	/**
	 * The color of the trapezoid
	 */
	protected Color color = Color.gray;
	/**
	 * The minimum and maximum sizes
	 */
	protected int minSize = 1, maxSize = 2;
	/**
	* Aligner is used to align horisontally or vertically several plots
	*/
	protected Aligner aligner = null;
	/**
	* Indicates "destroyed" state. Initially is false.
	*/
	protected boolean destroyed = false;

	/**
	* Sets the orientation of the object
	*/
	public void setIsHorizontal(boolean value) {
		isHorizontal = value;
	}

	/**
	* Sets the canvas in which this object should draw itself
	*/
	@Override
	public void setCanvas(Canvas c) {
		canvas = c;
	}

	public Canvas getCanvas() {
		return canvas;
	}

	@Override
	public Dimension getPreferredSize() {
		return (isHorizontal) ? new Dimension(30 * mm, maxSize + 2) : new Dimension(maxSize + 2, 30 * mm);
	}

	public void setMinSize(int minSize) {
		this.minSize = minSize;
	}

	public void setMaxSize(int maxSize) {
		this.maxSize = maxSize;
	}

	public int getMinSize() {
		return minSize;
	}

	public int getMaxSize() {
		return maxSize;
	}

	public void setColor(Color color) {
		this.color = color;
	}

	public Color getColor() {
		return color;
	}

	/**
	* Sets boundaries in which the object should fit itself
	*/
	@Override
	public void setBounds(Rectangle bounds) {
		this.bounds = bounds;
		if (aligner != null) {
			origBounds = new Rectangle(bounds);
			if (isHorizontal)
				if (aligner.getLeft() >= 0 && aligner.getRight() >= 0) {
					bounds.x += aligner.getLeft();
					bounds.width -= aligner.getLeft() + aligner.getRight();
				} else {
					;
				}
			else if (aligner.getTop() >= 0 && aligner.getBottom() >= 0) {
				bounds.y += aligner.getTop();
				bounds.height -= aligner.getTop() + aligner.getBottom();
			}
		}
	}

	@Override
	public Rectangle getBounds() {
		return bounds;
	}

	public boolean containsPoint(int x, int y) {
		if (bounds == null)
			return false;
		return bounds.contains(x, y);
	}

	/**
	* Draws the object in the given graphics.
	*/
	@Override
	public void draw(Graphics g) {
		if (g == null || bounds == null)
			return;
		//draw the background
		g.setColor((canvas == null) ? Color.white : canvas.getBackground());
		g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
		g.setColor(color);
		if (isHorizontal) {
			X[0] = bounds.x;
			Y[0] = bounds.y + (bounds.height - minSize) / 2;
			X[1] = bounds.x + bounds.width;
			Y[1] = bounds.y + (bounds.height - maxSize) / 2;
			X[2] = X[1];
			Y[2] = Y[1] + maxSize;
			X[3] = X[0];
			Y[3] = Y[0] + minSize;
			X[4] = X[0];
			Y[4] = Y[0];
		} else {
			X[0] = bounds.x + (bounds.width - minSize) / 2;
			Y[0] = bounds.y + bounds.height;
			X[1] = bounds.x + (bounds.width - maxSize) / 2;
			Y[1] = bounds.y;
			X[2] = X[1] + maxSize;
			Y[2] = Y[1];
			X[3] = X[0] + minSize;
			Y[3] = Y[0];
			X[4] = X[0];
			Y[4] = Y[0];
		}
		g.drawPolygon(X, Y, 5);
		g.fillPolygon(X, Y, 5);
	}

	public void redraw() {
		if (canvas != null) {
			Graphics g = canvas.getGraphics();
			if (g != null) {
				draw(g);
				g.dispose();
			}
		}
	}

	public void setAligner(Aligner al) {
		aligner = al;
		aligner.addPropertyChangeListener(this);
	}

	@Override
	public void propertyChange(PropertyChangeEvent pce) {
		if (pce.getSource() == aligner) {
			if (origBounds != null) {
				setBounds(origBounds);
			}
			if (canvas != null) {
				canvas.repaint();
			}
		}
	}

	/**
	* Makes necessary operations for destroying, e.g. unregisters from
	* listening highlighting and other events.
	*/
	@Override
	public void destroy() {
		destroyed = true;
	}

	/**
	* Replies whether is destroyed or not
	*/
	@Override
	public boolean isDestroyed() {
		return destroyed;
	}

}
