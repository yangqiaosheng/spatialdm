package spade.lib.color;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.beans.PropertyChangeListener;

/**
* Classes implementing this interface provide encoding of numeric values
* by colors
*/
public interface ColorScale {
	/**
	* Sets the minimum and the maximum possible values
	*/
	public void setMinMax(float min, float max);

	public float getMinValue();

	public float getMaxValue();

	/**
	* Sets the filter: the threshold below which the pixels will be invisible
	*/
	public void setMinLimit(float limit);

	public float getMinLimit();

	/**
	* Sets the filter: the threshold above which the pixels will be invisible
	*/
	public void setMaxLimit(float limit);

	public float getMaxLimit();

	/**
	* Returns the color for the given value
	*/
	public Color getColorForValue(float value);

	/**
	* Returns the color for the given value packed into an integer:
	* top 8 bits (0xFF << 24) - alpha value
	* 2nd 8 bits (0xFF << 16) - red value
	* 3rd 8 bits (0xFF << 8) - green value
	* bottom 8 bits (0xFF) - blue value
	*/
	public int getPackedColorForValue(float value);

	/**
	* Sets the alpha (transparency) value between 0 (invisible) and 1 (opaque)
	*/
	public void setAlpha(float value);

	public float getAlpha();

	/**
	* A ColorScale should be able to add its description at the end of the
	* legend formed by previous legend drawers. The argument startY specifies
	* the vertical position from which the LegendDrawer should start drawing
	* its part of the legend.The argument leftMarg specifies the left margin
	* (amount of space on the left to be kept blank). The argument prefW
	* specifies the preferrable width of the legend (to avoid horizontal
	* scrolling).
	* The method should return the rectangle occupied by the drawn part of
	* the legend.
	*/
//ID
	/**
	* Specifies whether notifications for map updates should be sent dynamically
	*/
	public void setDynamic(boolean dynamic);

	public boolean getDynamic();

//~ID
	public Rectangle drawLegend(Graphics g, int startY, int leftmarg, int prefW);

	/**
	* Draws a color bar representing this color scale
	*/
	public void drawColorBar(Graphics g, int x, int y, int w, int h);

	/**
	* Returns a UI component that allows the user to manipulate specific
	* parameters of this color scale (i.e. not transparency or minimum and
	* maximum limits that are manipulated in the same way for all color scales)
	*/
	public Component getManipulator();

	/**
	* Registers a listener of changes of properties of the color scale
	*/
	public void addPropertyChangeListener(PropertyChangeListener list);

	/**
	* Removes the listener of property changes
	*/
	public void removePropertyChangeListener(PropertyChangeListener list);

	/**
	 * Pass parameter string to setup color scale
	 */
	public void setParameters(String par);

	/**
	 * Pass parameter string to setup color scale
	 */
	public String getParameters();
}