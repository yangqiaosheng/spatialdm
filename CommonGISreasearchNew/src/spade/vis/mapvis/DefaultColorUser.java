package spade.vis.mapvis;

import java.awt.Color;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 29-Jan-2007
 * Time: 17:07:58
 * Allows to pass default colors to visualisers when necessary
 */
public interface DefaultColorUser {
	/**
	 * Sets the default line color
	 */
	public void setDefaultLineColor(Color lineColor);

	/**
	 * Sets the default fill color
	 */
	public void setDefaultFillColor(Color fillColor);
}
