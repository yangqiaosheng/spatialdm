package spade.vis.dmap;

import java.awt.Color;

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: 06-May-2008
 * Time: 14:31:12
 * Defines the appearance of a geographical object on a map
 */
public class ObjectAppearance {
	public boolean isVisible = true;
	public boolean mustPaint = true;
	public Color lineColor = Color.blue, fillColor = Color.lightGray;
	public int lineWidth = 1;
	public int transparency = 0;
	public Object presentation = null;
	public Color signColor = null;
	public int signSize = 0;
}
