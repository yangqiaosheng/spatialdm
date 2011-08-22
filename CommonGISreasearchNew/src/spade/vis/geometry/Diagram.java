package spade.vis.geometry;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;

public interface Diagram {
	public int getWidth();

	public int getHeight();

	/**
	* This function is to be used for point objects.
	*/
	public void draw(Graphics g, int x, int y);

	/**
	* This function is to be used for area objects.
	* The rectangle in which to fit the diagram is specified.
	*/
	public void draw(Graphics g, Rectangle r);

	/**
	* Same as previous, only the arguments are not wrapped into a Rectangle object
	*/
	public void draw(Graphics g, int x, int y, int w, int h);

	/**
	* Returns the position of the label. This method should be called after draw(...).
	*/
	public Point getLabelPosition();

	/**
	* Replies whether this diagram is centered, i.e. the center of the diagram
	* coinsides with the center of the object
	*/
	public boolean isCentered();
}