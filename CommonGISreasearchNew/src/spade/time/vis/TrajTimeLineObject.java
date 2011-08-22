package spade.time.vis;

import java.awt.Color;
import java.awt.Graphics;

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: Aug 18, 2009
 * Time: 5:33:36 PM
 * To change this template use File | Settings | File Templates.
 */
public class TrajTimeLineObject extends TimeLineObject {

	/**
	 * visual appearance: time graph or time line
	 */
	protected boolean isTGraphMode = true;

	/**
	 * point of the line under pointer or left point of the segment
	 */
	protected int idxPoint = -1;

	public void setIsTGraphMode(boolean TGraphMode) {
		isTGraphMode = TGraphMode;
	}

	/**
	 * Screen positions of trajectory points
	 * tPos - time stamps
	 * vPos - values (not used in TimeLine mode)
	 * and corresponding colors of the segments between them and for the last point
	 */
	protected int tPos[] = null;
	protected int vPos[] = null;
	protected Color colors[] = null;
	/**
	 * Maximum segment length - to represent values with limited validity time.
	 * If negative, ignored.
	 */
	protected int maxSegmLength = -1;

	/**
	 * Sets the maximum segment length, to represent values with limited validity time.
	 * If negative, ignored.
	 */
	public void setMaxSegmentLength(int maxSegmLength) {
		this.maxSegmLength = maxSegmLength;
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
				if (mx > tPos[i] + tolerance) {
					continue;
				}
				if (mx < tPos[i - 1] - tolerance)
					return false;
				if (vPos[i - 1] <= -1000 || vPos[i] <= -1000) {
					continue;
				}
				if (colors != null && colors[i - 1] == null) {
					continue;
				}
				if (mx <= tPos[i - 1] + tolerance) { // check left point
					if (my >= vPos[i - 1] - tolerance && my <= vPos[i - 1] + tolerance) {
						idxPoint = i - 1;
						return true;
					}
				}
				int yMin = vPos[i - 1], yMax = vPos[i];
				if (yMin > yMax) {
					yMin = yMax;
					yMax = vPos[i - 1];
				}
				if (my < yMin || my > yMax) {
					continue;
				}
				if (tPos[i] - tPos[i - 1] < tolerance) {// vertical line
					idxPoint = i - 1;
					return true;
				}
				// check point on line
				float ratio = ((float) mx - tPos[i - 1]) / (tPos[i] - tPos[i - 1]);
				int dy = Math.round(ratio * (vPos[i] - vPos[i - 1])), y = vPos[i - 1] + dy;
				if (my >= y - tolerance && my <= y + tolerance) {
					idxPoint = i - 1;
					return true;
				}
			}
			return false;
		} else { // TimeLine mode
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
	 * Checks if any point of the line is inside the given rectangle
	 */
	public boolean hasPointInRectangle(int x1, int y1, int x2, int y2) {
		if (isTGraphMode) {
			if (tPos == null || vPos == null)
				return false;
			for (int i = 0; i < tPos.length; i++) {
				if (tPos[i] < x1) {
					continue;
				}
				if (tPos[i] > x2)
					return false;
				if (vPos[i] <= -1000) {
					continue;
				}
				if (colors != null && colors[i] == null) {
					continue;
				}
				if (vPos[i] >= y1 && vPos[i] <= y2)
					return true;
			}
			return false;
		} else { // TimeLine mode
			if (oPos1 > y2 || oPos2 < y1)
				return false;
			if (tPos == null)
				return false;
			int tp1 = tPos[0], tp2 = tPos[tPos.length - 1];
			if (tp2 < x1 || tp1 > x2)
				return false;
			return true;
		}
	}

	@Override
	public void draw(Graphics g) {
		draw(g, 0);
	}

	public void draw(Graphics g, int yShift) {
		if (isTGraphMode) {
			if (tPos == null)
				return;
			for (int i = 1; i < tPos.length; i++) {
				boolean active1 = vPos[i - 1] > -1000, active2 = vPos[i] > -1000;
				if (!active1 && !active2) {
					continue;
				}
				if (colors != null) {
					active1 = active1 && colors[i - 1] != null;
					active2 = active2 && colors[i] != null;
					if (!active1 && !active2) {
						continue;
					}
				}
				if (active1 && active2) {
					int len = tPos[i] - tPos[i - 1] + 1;
					if (maxSegmLength <= 0 || len <= maxSegmLength) {
						g.drawLine(tPos[i - 1], vPos[i - 1] + yShift, tPos[i], vPos[i] + yShift);
					} else {
						g.drawLine(tPos[i - 1], vPos[i - 1] + yShift, tPos[i], vPos[i - 1] + yShift);
						if (vPos[i - 1] != vPos[i]) {
							g.drawLine(tPos[i], vPos[i - 1] + yShift, tPos[i], vPos[i] + yShift);
						}
					}
				} else if (active1) {
					g.drawLine(tPos[i - 1], vPos[i - 1] + yShift, tPos[i - 1] + 3, vPos[i - 1] + yShift);
				} else if (active2) {
					g.drawLine(tPos[i] - 3, vPos[i] + yShift, tPos[i], vPos[i] + yShift);
				}
			}
		} else { // time line mode
			if (tPos == null || colors == null || tPos.length != colors.length) {
				g.fillRect(tPos1, oPos1 + yShift, tPos2 - tPos1 + 1, oPos2 - oPos1 + 1);
			} else {
				g.drawLine(tPos1, oPos1 + yShift, tPos2, oPos1 + yShift);
				g.drawLine(tPos1, oPos2 + yShift, tPos2, oPos2 + yShift);
				for (int i = 1; i < tPos.length; i++)
					if (colors[i - 1] != null) {
						g.setColor(colors[i - 1]);
						int len = tPos[i] - tPos[i - 1] + 1;
						if (maxSegmLength > 0 && len > maxSegmLength) {
							len = maxSegmLength;
						}
						g.fillRect(tPos[i - 1], oPos1 + 1 + yShift, len, oPos2 - oPos1 + 1 - 2);
						//blurredFillRect(g,tPos[i-1],oPos1+1+yShift,len,oPos2-oPos1+1-2);
					}
				if (colors[tPos.length - 1] != null) {
					g.setColor(colors[tPos.length - 1]);
					g.drawLine(tPos[tPos.length - 1], oPos1 + 1 + yShift, tPos[tPos.length - 1], oPos2 - 1 + yShift);
				}
			}
		}
	}

/*
  protected void blurredFillRect (Graphics g, int x, int y, int width, int height) {
    float data[] = { 0.0625f, 0.125f, 0.0625f, 0.125f, 0.25f, 0.125f,
        0.0625f, 0.125f, 0.0625f};
    Kernel kernel = new Kernel(3, 3, data);
    ConvolveOp blur = new ConvolveOp(kernel);
    BufferedImage img=new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
    Graphics gimg=img.getGraphics();
    gimg.fillRect(0,0,width,height);
    Graphics2D g2d=(Graphics2D)g;
    g2d.drawImage(img,blur,x,y);
  }
*/

}
