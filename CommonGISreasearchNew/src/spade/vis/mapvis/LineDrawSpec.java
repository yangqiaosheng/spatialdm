package spade.vis.mapvis;

import java.awt.Color;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 29-Jan-2007
 * Time: 11:34:11
 * Contains parameters for drawing lines and vectors: colour, thickness, etc.
 */
public class LineDrawSpec {
	/**
	 * Whether to draw the line or not
	 */
	public boolean draw = true;
	/**
	 * The thickness of the line
	 */
	public int thickness = 1;
	/**
	 * The colour of the line
	 */
	public Color color = null;
	/**
	 * "True" means that the line should be semi-transparent (e.g. no data or
	 * out of focus)
	 */
	public boolean transparent = false;
}
