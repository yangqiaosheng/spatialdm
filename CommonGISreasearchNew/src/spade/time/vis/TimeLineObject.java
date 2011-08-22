package spade.time.vis;

import java.awt.Graphics;

import spade.time.TimeMoment;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 02-Feb-2007
 * Time: 12:09:10
 * Represents one object (event) on a time line
 */
public class TimeLineObject {
	/**
	 * The identifier of the object (event) represented by this TimeLineObject
	 */
	public String id = null;
	/**
	 * The index of this object in the container
	 */
	public int idx = -1;
	/**
	 * The time reference of this object: starting and ending moments of its
	 * validity. For an instant event, t2 is null.
	 */
	public TimeMoment t1 = null, t2 = null;
	/**
	 * Indicates whether this is an instant event
	 */
	public boolean instant = false;
	/**
	 * The starting and ending positions of the drawn object on the time axis
	 */
	public int tPos1 = -1, tPos2 = -1;
	/**
	 * The positions of the drawn object on the "object" axis
	 */
	public int oPos1 = -1, oPos2 = -1;
	/**
	 * Indicate whether this object is highlighted and/or selected
	 */
	public boolean isHighlighted = false, isSelected = false;
	/**
	 * Indicates whether the object is active, i.e. not filtered out
	 */
	public boolean active = true;

	/**
	 * Checks if the given point is inside the drawn object
	 */
	public boolean contains(int mx, int my) {
		//if (tPos1<0 || oPos1<0) return false; // does not work with temporal focusser
		return mx >= tPos1 && mx <= tPos2 && my >= oPos1 && my <= oPos2;
	}

	/**
	 * Checks if the object fits in the given rectangle
	 */
	public boolean fitsInRectangle(int x1, int y1, int x2, int y2) {
		if (tPos2 < x1)
			return false;
		if (tPos1 > x2)
			return false;
		if (oPos2 < y1)
			return false;
		if (oPos1 > y2)
			return false;
		return true;
	}

	public void draw(Graphics g) {
		g.fillRect(tPos1, oPos1, tPos2 - tPos1 + 1, oPos2 - oPos1 + 1);
	}
}
