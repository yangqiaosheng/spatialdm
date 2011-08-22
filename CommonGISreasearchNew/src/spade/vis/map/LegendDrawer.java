package spade.vis.map;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;

public interface LegendDrawer {
	/**
	* A LegendDrawer should be able to add its description at the end of the
	* legend formed by previous legend drawers. The argument startY specifies
	* the vertical position from which the LegendDrawer should start drawing
	* its part of the legend.The argument leftMarg specifies the left margin
	* (amount of space on the left to be kept blank). The argument prefW
	* specifies the preferrable width of the legend (to avoid horizontal
	* scrolling).
	* The method should return the rectangle occupied by the drawn part of
	* the legend.
	*/
	public Rectangle drawLegend(Component c, Graphics g, int startY, int leftmarg, int prefW);
}
