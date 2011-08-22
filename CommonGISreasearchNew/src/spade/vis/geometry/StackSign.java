package spade.vis.geometry;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.Vector;

/**
* A stack sign is used to represent qualitative attributes that may have
* multiple values associated with a single object. Draws "stacked" rectangles
* according to the number of items in a list.
*/
public class StackSign extends Sign {
	/**
	* The number of items in the stack. By default 1.
	*/
	protected int nItems = 1;
	/**
	* The shift between the rectangles in a stack (in pixels)
	*/
	public static final int shift = 3;
	/**
	* Indicates whether different items are shown using different colors. By
	* default is false.
	*/
	protected boolean useColors = false;
	/**
	* The vector of colors to be used for showing items in the order from left to
	* right.
	*/
	protected Vector colors = null;

	@Override
	public int getWidth() {
		return minW + (nItems - 1) * shift;
	}

	@Override
	public int getHeight() {
		return minH + (nItems - 1) * shift;
	}

	/**
	* Sets the number of items in a stack.
	*/
	public void setNItems(int n) {
		nItems = n;
	}

	/**
	* Replies whether different items are shown by different colors.
	*/
	public boolean getUseColors() {
		return useColors;
	}

	/**
	* Sets the StackSign to represent different items by different colors (if
	* the argument is true) or by the same color (if false).
	*/
	public void setUseColors(boolean value) {
		useColors = value;
	}

	/**
	* Sets the color for the item with the given index
	*/
	public void setColorForItem(Color itemColor, int idx) {
		if (colors == null) {
			colors = new Vector(10, 10);
		}
		while (colors.size() < idx) {
			colors.addElement(color);
		}
		if (idx >= colors.size()) {
			colors.addElement(itemColor);
		} else {
			colors.setElementAt(itemColor, idx);
		}
	}

	/**
	* This function is to be used for point objects.
	*/
	@Override
	public void draw(Graphics g, int x, int y) {
		if (nItems < 1)
			return;
		int w = getWidth(), h = getHeight();
		int x0 = x + w / 2 - minW, y0 = y - h / 2;
		labelX = x0;
		labelY = y0 + h + 2;
		for (int i = nItems - 1; i >= 0; i--) {
			if (!useColors || colors == null || colors.size() <= i) {
				g.setColor(color);
			} else {
				//g.setColor((Color)colors.elementAt(i));
				g.setColor(java2d.Drawing2D.getTransparentColor((Color) colors.elementAt(i), transparency));
			}
			g.fillRect(x0, y0, minW, minH);
			g.setColor(borderColor);
			g.drawRect(x0, y0, minW, minH);
			x0 -= shift;
			y0 += shift;
		}
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

	@Override
	protected void checkSizes() {
	}

}
