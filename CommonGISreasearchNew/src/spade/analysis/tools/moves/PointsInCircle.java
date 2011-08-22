package spade.analysis.tools.moves;

import java.util.Vector;

import spade.lib.util.DoubleArray;
import spade.lib.util.FloatArray;
import spade.lib.util.NumValManager;
import spade.lib.util.ObjectWithMeasure;
import spade.vis.geometry.RealPoint;
import spade.vis.geometry.RealRectangle;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: May 11, 2009
 * Time: 4:06:59 PM
 * Used for grouping points and for smoothing. Contains points.
 */
public class PointsInCircle {
	/**
	 * The maximum radius (in case of geographical coordinates, the radius is specified in metres).
	 * May be NaN if not used.
	 */
	public float maxRad = Float.NaN;
	/**
	 * For geographical coordinates: the ratios between the horizontal and vertical
	 * distances in metres and in degrees.
	 */
	public float geoFactorX = 1, geoFactorY = 1;
	/**
	 * Squared maxRad.
	 * May be NaN if not used.
	 */
	public float sqMaxRad = Float.NaN;
	/**
	 * The points contained in this circle
	 */
	public Vector<RealPoint> points = null;
	/**
	 * The centre of the circle
	 */
	public float cx = Float.NaN, cy = Float.NaN;
	/**
	 * The sum of x- and y-coordinates of the points
	 */
	public float sumX = 0, sumY = 0;
	/**
	 * The medians of the x- and y-coordinates of the points
	 */
	public float mx = Float.NaN, my = Float.NaN;
	/**
	 * The quasi-medoid: the closest point to the median (mx,my)
	 */
	public RealPoint qmed = null;
	/**
	 * The medoid: the point with the smallest average distance
	 * to all other points
	 */
	public RealPoint med = null;
	/**
	 * The average distance from the medoid to the other points
	 */
	public double medAveDist = Double.NaN;

	public PointsInCircle(float maxRad, float geoFactorX, float geoFactorY) {
		this.maxRad = maxRad;
		this.geoFactorX = geoFactorX;
		this.geoFactorY = geoFactorY;
		sqMaxRad = maxRad * maxRad;
	}

	/**
	 * Used when no maximum radius is needed
	 */
	public PointsInCircle(float geoFactorX, float geoFactorY) {
		this.geoFactorX = geoFactorX;
		this.geoFactorY = geoFactorY;
	}

	/**
	 * Removes all points
	 */
	public void removeAllPoints() {
		if (points != null) {
			points.removeAllElements();
		}
		cx = cy = Float.NaN;
		mx = my = Float.NaN;
		med = null;
		qmed = null;
		medAveDist = Double.NaN;
		sumX = sumY = 0;
	}

	public boolean distanceBelowMaxRad(float x1, float y1, float x2, float y2) {
		if (Float.isNaN(maxRad))
			return true;
		float dx = (x1 - x2) * geoFactorX;
		if (Math.abs(dx) > maxRad)
			return false;
		float dy = (y1 - y2) * geoFactorY;
		if (Math.abs(dy) > maxRad)
			return false;
		return dx * dx + dy * dy <= sqMaxRad;
	}

	/**
	 * Checks if the given point fits in this circle, i.e. the distance to
	 * the centre is below maxRad.
	 * @param checkByAdding - check if the maximum radius will not be exceeded
	 *   when the point is added to the cluster.
	 */
	public boolean fitsInCircle(RealPoint p, boolean checkByAdding) {
		if (p == null)
			return false;
		if (Float.isNaN(maxRad))
			return true;
		if (points == null || points.size() < 1)
			return true;
		if (!distanceBelowMaxRad(p.x, p.y, cx, cy))
			return false;
		if (!checkByAdding)
			return true;
		float sumX1 = sumX + p.x, sumY1 = sumY + p.y;
		float cx1 = sumX1 / (points.size() + 1), cy1 = sumY1 / (points.size() + 1);
		for (int i = 0; i < points.size(); i++)
			if (!distanceBelowMaxRad(points.elementAt(i).x, points.elementAt(i).y, cx1, cy1))
				return false;
		return true;
	}

	/**
	 * Adds the given point to the set of points contained in this circle.
	 * Re-computes the centroid of the circle.
	 */
	public void addPoint(RealPoint p) {
		if (p == null)
			return;
		if (points == null) {
			points = new Vector<RealPoint>(20, 20);
		}
		points.addElement(p);
		mx = my = Float.NaN;
		med = null;
		qmed = null;
		medAveDist = Double.NaN;
		if (points.size() == 1) {
			cx = p.x;
			cy = p.y;
			sumX = cx;
			sumY = cy;
		} else {
			sumX += p.x;
			sumY += p.y;
			cx = sumX / points.size();
			cy = sumY / points.size();
		}
	}

	/**
	 * Returns the centre as a RealPoint
	 */
	public RealPoint getCentre() {
		if (Float.isNaN(cx))
			return null;
		return new RealPoint(cx, cy);
	}

	/**
	 * Returns the representative point, which is the closest point
	 * to the centroid of the set of points.
	 */
	public RealPoint getRepresentativePoint() {
		if (points == null)
			return null;
		if (points.size() == 1)
			return points.elementAt(0);
		float minDist = getSquaredDistance(points.elementAt(0).x, points.elementAt(0).y, cx, cy);
		int pIdx = 0;
		for (int i = 1; i < points.size(); i++) {
			float d = getSquaredDistance(points.elementAt(i).x, points.elementAt(i).y, cx, cy);
			if (d < minDist) {
				minDist = d;
				pIdx = i;
			}
		}
		return points.elementAt(pIdx);
	}

	public double getRadius() {
		if (points == null || points.size() < 2)
			return 0;
		double maxd = 0;
		for (int i = 0; i < points.size(); i++) {
			RealPoint p = points.elementAt(i);
			double d = Math.sqrt(getSquaredDistance(p.x, p.y, cx, cy));
			if (d > maxd) {
				maxd = d;
			}
		}
		return maxd;
	}

	public RealRectangle getBounds() {
		if (points == null || points.size() < 1)
			return null;
		RealPoint p = points.elementAt(0);
		RealRectangle r = new RealRectangle(p.x, p.y, p.x, p.y);
		for (int i = 1; i < points.size(); i++) {
			p = points.elementAt(i);
			if (r.rx1 > p.x) {
				r.rx1 = p.x;
			} else if (r.rx2 < p.x) {
				r.rx2 = p.x;
			}
			if (r.ry1 > p.y) {
				r.ry1 = p.y;
			} else if (r.ry2 < p.y) {
				r.ry2 = p.y;
			}
		}
		return r;
	}

	public double getMaxDistToPoint(float x, float y) {
		if (points == null || points.size() < 1)
			return 0;
		double max = 0;
		for (int i = 0; i < points.size(); i++) {
			RealPoint p = points.elementAt(i);
			double d = Math.sqrt(getSquaredDistance(p.x, p.y, x, y));
			if (max < d) {
				max = d;
			}
		}
		return max;
	}

	public double getMeanDistToPoint(float x, float y) {
		if (points == null || points.size() < 1)
			return 0;
		double sumD = 0;
		for (int i = 0; i < points.size(); i++) {
			RealPoint p = points.elementAt(i);
			double d = Math.sqrt(getSquaredDistance(p.x, p.y, x, y));
			sumD += d;
		}
		return sumD / points.size();
	}

	public double getMeanDistToCentre() {
		if (points == null || points.size() < 2)
			return 0;
		return getMeanDistToPoint(cx, cy);
	}

	public float getSquaredDistance(float x1, float y1, float x2, float y2) {
		float dx = (x1 - x2) * geoFactorX;
		float dy = (y1 - y2) * geoFactorY;
		return dx * dx + dy * dy;
	}

	public int getPointCount() {
		if (points == null)
			return 0;
		return points.size();
	}

	public RealPoint getPoint(int idx) {
		if (idx < 0 || points == null || idx >= points.size())
			return null;
		return points.elementAt(idx);
	}

	/**
	 * Computes the medians of the x- and y-coordinates of the points
	 */
	public void computeMedian() {
		if (points == null || points.size() < 1)
			return;
		if (points.size() <= 2) {
			mx = cx;
			my = cy;
			return;
		}
		FloatArray xx = new FloatArray(points.size(), 1), yy = new FloatArray(points.size(), 1);
		for (int i = 0; i < points.size(); i++) {
			RealPoint p = points.elementAt(i);
			xx.addElement(p.x);
			yy.addElement(p.y);
		}
		mx = NumValManager.getMedian(xx);
		my = NumValManager.getMedian(yy);
	}

	/**
	 * Returns the point (mx,my), where mx and my are the medians of the
	 * x- and y-coordinates of the points.
	 */
	public RealPoint getMedian() {
		if (Float.isNaN(mx)) {
			computeMedian();
		}
		if (Float.isNaN(mx))
			return null;
		return new RealPoint(mx, my);
	}

	/**
	 * Returns the point which is the closest to the median point.
	 */
	public RealPoint getQuasiMedoid() {
		if (points == null || points.size() < 1)
			return null;
		if (qmed != null)
			return qmed;
		if (Float.isNaN(mx)) {
			computeMedian();
		}
		if (Float.isNaN(mx))
			return null;
		qmed = points.elementAt(0);
		double d0 = getSquaredDistance(qmed.x, qmed.y, mx, my);
		for (int i = 1; i < points.size(); i++) {
			RealPoint p = points.elementAt(i);
			double d = getSquaredDistance(p.x, p.y, mx, my);
			if (d < d0) {
				d0 = d;
				qmed = p;
			}
		}
		return qmed;
	}

	/**
	 * Returns the true medoid, i.e. the point with the smallest
	 * sum of distances (or average distance) to all other points.
	 */
	public RealPoint getTrueMedoid() {
		if (points == null || points.size() < 1)
			return null;
		if (med != null)
			return med;
		if (points.size() < 3) {
			med = points.elementAt(0);
			return med;
		}
		int mIdx = -1;
		double minSumDist = Double.NaN;
		for (int i = 0; i < points.size(); i++) {
			double sumDist = 0;
			RealPoint p1 = points.elementAt(i);
			for (int j = 0; j < points.size(); j++)
				if (j != i) {
					RealPoint p2 = points.elementAt(j);
					double d = getSquaredDistance(p1.x, p1.y, p2.x, p2.y);
					sumDist += Math.sqrt(d);
					if (!Double.isNaN(minSumDist) && sumDist >= minSumDist) {
						break;
					}
				}
			if (Double.isNaN(minSumDist) || sumDist < minSumDist) {
				minSumDist = sumDist;
				mIdx = i;
			}
		}
		med = points.elementAt(mIdx);
		medAveDist = minSumDist / points.size();
		return med;
	}

	/**
	 * Returns the true medoid, i.e. the point with the smallest
	 * average distance to all other points, together with the average distance.
	 */
	public ObjectWithMeasure getMedoidAndAveDist() {
		if (points == null || points.size() < 1)
			return null;
		if (med == null) {
			med = getTrueMedoid();
		}
		if (med == null)
			return null;
		if (!Double.isNaN(medAveDist))
			return new ObjectWithMeasure(med, medAveDist);
		double sumDist = 0;
		for (int j = 0; j < points.size(); j++) {
			sumDist += getSquaredDistance(med.x, med.y, points.elementAt(j).x, points.elementAt(j).y);
		}
		medAveDist = sumDist / points.size();
		return new ObjectWithMeasure(med, medAveDist);
	}

	/**
	 * @return the mean distance to the median point
	 */
	public double getMeanDistToMedian() {
		if (points == null || points.size() < 2)
			return 0;
		if (Float.isNaN(mx)) {
			computeMedian();
		}
		if (Float.isNaN(mx))
			return 0;
		return getMeanDistToPoint(mx, my);
	}

	/**
	 * Returns the number of points whose distance to the given
	 * point (specified by x- and y-coordinates) does not
	 * exceed the given threshold
	 */
	public int countPointsAround(float x, float y, double maxDist) {
		if (points == null || points.size() < 1)
			return 0;
		if (Float.isNaN(x) || Float.isNaN(y) || Double.isNaN(maxDist))
			return 0;
		double sqMaxDist = maxDist * maxDist;
		int count = 0;
		for (int i = 0; i < points.size(); i++) {
			RealPoint p = points.elementAt(i);
			double d = Math.sqrt(getSquaredDistance(p.x, p.y, x, y));
			if (d <= maxDist) {
				++count;
			}
		}
		return count;
	}

	public double getMedianDistToPoint(float x, float y) {
		if (points == null || points.size() < 1)
			return 0;
		DoubleArray dd = new DoubleArray(points.size(), 1);
		for (int i = 0; i < points.size(); i++) {
			RealPoint p = points.elementAt(i);
			dd.addElement(Math.sqrt(getSquaredDistance(p.x, p.y, x, y)));
		}
		return NumValManager.getMedian(dd);
	}

}
