package spade.time.vis;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;

import spade.vis.geometry.Computing;

/**
* Represents temporal variation of attribute values of a single object on a
* time graph.
*/
public class TimeLine {
	/**
	* The identifier of the object
	*/
	public String objId = null;
	/**
	* The index of the object or table record
	*/
	public int objIdx = -1;
	/**
	* The index of the class the object belongs to or -1 if the object does not
	* belong to any class
	*/
	public int classIdx = -1;
	/**
	* Indicates the selected (durably highlighted) status
	*/
	public boolean selected = false;
	/**
	* The array of points (vertices) of the line. Some points may be null,
	* if there is no corresponding attribute value.
	*/
	protected Point points[] = null;
	/**
	* The array of source or transformed attribute values corresponding to the
	* vertices of the line. Some values may be NaNs.
	*/
	protected double values[] = null;
	/**
	 * These may be additionally transformed values, e.g. after subtracting the trend
	 */
	protected double transValues[] = null;

	/**
	* Sets the number of points in the line
	*/
	public void setNPoints(int nPoints) {
		if (points == null || points.length != nPoints) {
			points = new Point[nPoints];
		}
		if (values == null || values.length != nPoints) {
			values = new double[nPoints];
		}
		for (int i = 0; i < nPoints; i++) {
			points[i] = null;
			values[i] = Double.NaN;
		}
	}

	public int getNPoints() {
		return points.length;
	}

	public Point getPoint(int idx) {
		if (points == null || idx < 0 || idx >= points.length)
			return null;
		return points[idx];
	}

	/**
	* Sets the point with the given index
	*/
	public void setPoint(Point p, int idx) {
		if (points != null && idx >= 0 && idx < points.length) {
			points[idx] = p;
		}
	}

	/**
	* Sets the value at the given index
	*/
	public void setValue(double val, int idx) {
		if (values != null && idx >= 0 && idx < values.length) {
			values[idx] = val;
		}
	}

	/**
	* Returns the value at the given index
	*/
	public double getValue(int idx) {
		if (values != null && idx >= 0 && idx < values.length)
			return values[idx];
		return Double.NaN;
	}

	public double[] getValues() {
		return values;
	}

	/**
	 * Sets the transformed value at the given index. The original value
	 * does not change.
	 */
	public void setTransformedValue(double val, int idx) {
		if (values == null)
			return;
		if (transValues == null) {
			transValues = values.clone();
		}
		if (idx >= 0 && idx < transValues.length) {
			transValues[idx] = val;
		}
	}

	/**
	 * Returns the transformed value at the given index.
	 * If there are no transformed values, returns the original value.
	 */
	public double getTransformedValue(int idx) {
		if (transValues != null && idx >= 0 && idx < transValues.length)
			return transValues[idx];
		return getValue(idx);
	}

	/**
	 * Returns the array of transformed values.
	 * If there are no transformed values, returns the array of original values.
	 */
	public double[] getTransformedValues() {
		if (transValues != null)
			return transValues;
		return values;
	}

	public boolean hasTransformedValues() {
		return transValues != null;
	}

	/**
	* Draws the line
	*/
	public void draw(Graphics g) {
		if (points == null)
			return;
		for (int i = 0; i < points.length; i++)
			if (points[i] != null) {
				if (i > 0)
					if (points[i - 1] != null)
						if (points[i - 1].x == points[i].x && points[i - 1].y == points[i].y) {
							;
						} else {
							g.drawLine(points[i - 1].x, points[i - 1].y, points[i].x, points[i].y);
						}
					else {
						g.drawLine(points[i].x - 3, points[i].y, points[i].x, points[i].y);
					}
				if (i < points.length - 1 && points[i + 1] == null) {
					g.drawLine(points[i].x, points[i].y, points[i].x + 3, points[i].y);
				}
			}
	}

	/**
	* Draws the think line
	*/
	public void drawThickLine(Graphics g) {
		if (points == null)
			return;
		for (int k = -1; k <= 1; k++) {
			for (int i = 0; i < points.length; i++)
				if (points[i] != null) {
					if (i > 0)
						if (points[i - 1] != null) {
							g.drawLine(points[i - 1].x, k + points[i - 1].y, points[i].x, k + points[i].y);
						} else {
							g.drawLine(points[i].x - 3, k + points[i].y, points[i].x, k + points[i].y);
						}
					if (i < points.length - 1 && points[i + 1] == null) {
						g.drawLine(points[i].x, k + points[i].y, points[i].x + 3, k + points[i].y);
					}
				}
		}
	}

	/**
	* Draws the bordered line
	*/
	public void drawBorderedLine(Graphics g) {
		Color c = g.getColor();
		drawThickLine(g);
		g.setColor(Color.black);
		draw(g);
		g.setColor(c);
	}

	public int getClosestPointIdx(int x) {
		int idx = -1, dist = Integer.MAX_VALUE;
		for (int i = 0; i < points.length; i++)
			if (points[i] != null) {
				int d = Math.abs(x - points[i].x);
				if (d < dist) {
					dist = d;
					idx = i;
				} else {
					break;
				}
			}
		return idx;
	}

	/**
	* Checks whether the given mouse position fits close to this line. If so,
	* returns the index of the closest point, otherwise returns -1.
	*/
	public int fitPointIdx(int x, int y) {
		if (points == null)
			return -1;
		//find the closest point, according to the x-position
		int idx = getClosestPointIdx(x);
		/*
		for (int i=0; i<points.length; i++)
		  if (points[i]!=null) {
		    int d=Math.abs(x-points[i].x);
		    if (d<dist) {
		      dist=d; idx=i;
		    }
		    else break;
		  }
		*/
		if (idx < 0)
			return -1;
		if (x >= points[idx].x - 2 && x <= points[idx].x + 2 && y >= points[idx].y - 2 && y <= points[idx].y + 2)
			return idx;
		Point p = null;
		if (x > points[idx].x && idx < points.length - 1) {
			p = points[idx + 1];
		} else if (x < points[idx].x && idx > 0) {
			p = points[idx - 1];
		}
		if (p == null)
			return -1;
		if (Computing.isPointOnLine(x, y, p, points[idx]))
			return idx;
		return -1;
	}

	/**
	* Checks whether the given number n is between the numbers k1 and k2.
	*/
	protected static boolean between(int n, int k1, int k2) {
		if (k1 < k2)
			return n >= k1 && n <= k2;
		return n >= k2 && n <= k1;
	}

	/**
	* Checks whether a part of the line fit in the given rectangle
	*/
	public boolean fitsInRectangle(int x1, int y1, int x2, int y2) {
		if (points == null)
			return false;
		if (x1 > x2) {
			int x = x1;
			x1 = x2;
			x2 = x;
		}
		if (y1 > y2) {
			int y = y1;
			y1 = y2;
			y2 = y;
		}
		for (Point point : points)
			if (point != null && point.x >= x1 && point.x <= x2 && point.y >= y1 && point.y <= y2)
				return true;
		for (int i = 0; i < points.length - 1; i++)
			if (points[i] != null && points[i + 1] != null) {
				if (points[i].x < x1 && points[i + 1].x > x1) {
					//count y-coordinate for the point on the line with the x-coordinate x1
					int y0 = points[i].y + Math.round(1.0f * (x1 - points[i].x) * (points[i + 1].y - points[i].y) / (points[i + 1].x - points[i].x));
					if (between(y0, y1, y2))
						return true;
				}
				if (points[i].x < x2 && points[i + 1].x > x2) {
					//count y-coordinate for the point on the line with the x-coordinate x2
					int y0 = points[i].y + Math.round(1.0f * (x2 - points[i].x) * (points[i + 1].y - points[i].y) / (points[i + 1].x - points[i].x));
					if (between(y0, y1, y2))
						return true;
				}
				if (between(y1, points[i].y, points[i + 1].y)) {
					//count x-coordinate for the point on the line with the y-coordinate y1
					int x0 = points[i].x + Math.round(1.0f * (y1 - points[i].y) * (points[i + 1].x - points[i].x) / (points[i + 1].y - points[i].y));
					if (x0 >= x1 && x0 <= x2)
						return true;
				}
				if (between(y2, points[i].y, points[i + 1].y)) {
					//count x-coordinate for the point on the line with the y-coordinate y2
					int x0 = points[i].x + Math.round(1.0f * (y2 - points[i].y) * (points[i + 1].x - points[i].x) / (points[i + 1].y - points[i].y));
					if (x0 >= x1 && x0 <= x2)
						return true;
				}
			}
		return false;
	}
}