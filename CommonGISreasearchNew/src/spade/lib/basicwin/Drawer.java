package spade.lib.basicwin;

import java.awt.Dimension;
import java.awt.Graphics;

/**
* Basic interface for classes drawing icons in a given graphical window
*/
public interface Drawer {
	/**
	* Returns the preferred icon size
	*/
	public Dimension getIconSize();

	/**
	* Makes the drawer to draw the icon in enabled or disabled state
	*/
	public void setDrawEnabled(boolean value);

	/**
	* Draws the icon of the preferred size at the specified position
	*/
	public void draw(Graphics g, int x0, int y0);

	/**
	* Draws the icon in a component with the specified size and origin.
	* The argument "stretch" indicated whether the icon should be stretched
	* if the component is larger than the preferred size of the icon.
	*/
	public void draw(Graphics g, int x0, int y0, int w, int h, boolean stretch);
}