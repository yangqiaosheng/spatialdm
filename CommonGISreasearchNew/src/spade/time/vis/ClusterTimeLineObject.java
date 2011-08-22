/**
 * 
 */
package spade.time.vis;

import java.awt.Point;

/**
 * @author Admin
 *
 */
public class ClusterTimeLineObject extends TrajTimeLineObject {

	double c_id = -1;

	public void setColors() {

	}

	/**
	   * Checks if the given point is inside the drawn object
	   */
	@Override
	public boolean contains(int mx, int my) {
		int tolerance = 2; // in pixels
		idxPoint = -1;
		if (isTGraphMode) {
			if (tPos == null || vPos == null)
				return false;
			// check endpoint
			if (mx >= tPos[tPos.length - 1] - tolerance && mx <= tPos[tPos.length - 1] + tolerance && my >= vPos[tPos.length - 1] - tolerance && my <= vPos[tPos.length - 1] + tolerance) {
				idxPoint = tPos.length - 1;
				return true;
			}
			for (int i = 1; i < tPos.length; i++) {
				if (mx > tPos[i]) {
					continue;
				}
				if (mx < tPos[i - 1])
					return false;
				if (mx >= tPos[i - 1] - tolerance && mx <= tPos[i - 1] + tolerance) { // check left point
					if (my >= vPos[i - 1] - tolerance && my <= vPos[i - 1] + tolerance) {
						idxPoint = i - 1;
						return true;
					}
					if (tPos[i - 1] == tPos[i]) // vertical line, check the range
						if (my >= Math.min(vPos[i - 1], vPos[i]) && my <= Math.max(vPos[i - 1], vPos[i])) {
							idxPoint = i - 1;
							return true;
						} else {
							continue;
						}
				}
				if (mx >= tPos[i - 1] && mx <= tPos[i]) { // check point on line
					Point p1 = new Point(tPos[i - 1], vPos[i - 1]), p2 = new Point(tPos[i], vPos[i]);
					/*
					boolean b=Computing.isPointOnLine(mx,my,p1,p2);
					if (b)
					  idxPoint=i-1;
					*/
					boolean b = (mx < tPos[i] - tolerance && my >= vPos[i - 1] - tolerance && my <= vPos[i - 1] + tolerance)
							|| (mx >= tPos[i] - tolerance && my >= Math.min(vPos[i - 1], vPos[i]) - tolerance && my <= Math.max(vPos[i - 1], vPos[i]) + tolerance);
					if (b) {
						idxPoint = i - 1;
					}
					return b;
				}
			}
			return false;
		} else {
			if (my < oPos1 || my > oPos2)
				return false;
			if (tPos == null)
				return super.contains(mx, my);
			if (mx >= tPos[tPos.length - 1] - tolerance && mx <= tPos[tPos.length - 1] + tolerance) {
				idxPoint = tPos.length - 1;
				return true;
			}
			for (int i = 1; i < tPos.length; i++)
				if (mx >= tPos[i - 1] && mx <= tPos[i]) {
					idxPoint = i - 1;
					return true;
				}
			return false;
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
