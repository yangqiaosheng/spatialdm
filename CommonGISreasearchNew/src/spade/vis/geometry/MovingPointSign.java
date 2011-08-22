package spade.vis.geometry;

import java.awt.Graphics;
import java.awt.Toolkit;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Oct 21, 2010
 * Time: 10:37:15 AM
 * Represent a point in a trajectory of a moving object with the
 * incoming and outgoing direction vectors
 */
public class MovingPointSign {
	public static float mm = Toolkit.getDefaultToolkit().getScreenResolution() / 25.33f;
	/**
	* Default size of the sign
	*/
	public static double vectorLength = 2 * mm, perpLength = 0.7 * mm, pvRatio = perpLength / vectorLength;

	/**
	 * Draws the sign in the screen position x,y
	 * x0,y0 - screen position of the previous point
	 * x1,y1 - screen position of the next point
	 */
	public void draw(Graphics g, int x, int y, double dx0, double dy0, double dx1, double dy1) {
		g.drawOval(x - 1, y - 1, 3, 3);
		double len = Math.sqrt(dx0 * dx0 + dy0 * dy0);
		if (len > 0) {
			double ratio = vectorLength / len;
			int xx0 = (int) Math.round(x + dx0 * ratio), yy0 = (int) Math.round(y - dy0 * ratio);
			g.drawLine(x, y, xx0, yy0);
			double dx = xx0 - x, dy = yy0 - y;
			int ddx = (int) Math.round(dx * pvRatio), ddy = (int) Math.round(dy * pvRatio);
			g.drawLine(xx0 - ddy, yy0 + ddx, xx0 + ddy, yy0 - ddx);
		}
		len = Math.sqrt(dx1 * dx1 + dy1 * dy1);
		if (len > 0) {
			double ratio = vectorLength / len;
			int xx1 = (int) Math.round(x + dx1 * ratio), yy1 = (int) Math.round(y - dy1 * ratio);
			g.drawLine(x, y, xx1, yy1);
		}
	}
}
