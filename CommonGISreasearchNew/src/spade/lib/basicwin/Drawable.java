package spade.lib.basicwin;

import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;

/**
* Any object that can be a content of a canvas.
*/

public interface Drawable extends Destroyable {
	/**
	* The canvas in which this object should draw itself
	*/
	public void setCanvas(Canvas c);

	public Dimension getPreferredSize();

	/**
	* Sets boundaries in which the object should fit itself
	*/
	public void setBounds(Rectangle bounds);

	public Rectangle getBounds();

	/**
	* Draws the object in the given graphics.
	*/
	public void draw(Graphics g);

	/**
	* Makes necessary operations for destroying, e.g. unregisters from
	* listening highlighting and other events.
	*/
	@Override
	public void destroy();

	/**
	* Replies whether is destroyed or not
	*/
	@Override
	public boolean isDestroyed();
}