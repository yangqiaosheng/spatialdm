package spade.lib.basicwin;

import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;

/**
* PlotCanvas provides its area to an object implementing Drawable
* interface for drawing.
*/

public class PlotCanvas extends Canvas implements Destroyable {
	protected Drawable content = null;
	protected int leftMarg = 0, rightMarg = 0, topMarg = 0, bottomMarg = 0;

	public void setContent(Drawable drawableObject) {
		content = drawableObject;
	}

	@Override
	public Dimension getPreferredSize() {
		if (content != null) {
			Dimension d = content.getPreferredSize();
			return new Dimension(d.width + leftMarg + rightMarg, d.height + topMarg + bottomMarg);
		}
		return new Dimension(10, 10);
	}

	public void setInsets(int left, int top, int right, int bottom) {
		leftMarg = left;
		rightMarg = right;
		topMarg = top;
		bottomMarg = bottom;
	}

	@Override
	public void paint(Graphics g) {
		if (content != null) {
			Dimension d = getSize();
			d.width -= leftMarg + rightMarg;
			d.height -= topMarg + bottomMarg;
			Rectangle r = content.getBounds();
			if (r == null || r.width != d.width || r.height != d.height) {
				if (r == null) {
					r = new Rectangle(leftMarg, topMarg, d.width, d.height);
				} else {
					r.x = leftMarg;
					r.y = topMarg;
					r.width = d.width;
					r.height = d.height;
				}
				content.setBounds(r);
			}
			content.draw(g);
		}
	}

	/**
	* Tells the object drawing in this canvas to destroy itselg, e.g. to
	* unregister from listening highlighting and other events.
	*/
	@Override
	public void destroy() {
		if (content != null) {
			content.destroy();
		}
	}

	/**
	* Replies whether is destroyed or not
	*/
	@Override
	public boolean isDestroyed() {
		if (content != null)
			return content.isDestroyed();
		return false;
	}
}