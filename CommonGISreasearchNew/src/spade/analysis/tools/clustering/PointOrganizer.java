package spade.analysis.tools.clustering;

import java.util.Vector;

import spade.analysis.tools.moves.PointsInCircle;
import spade.lib.util.ObjectWithMeasure;
import spade.vis.geometry.GeoComp;
import spade.vis.geometry.RealPoint;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Feb 2, 2009
 * Time: 3:13:19 PM
 * Organizes spatial points in groups (clusters) so that the spatial extent
 * of each group fits ina circle with the given maximum radius. For each
 * group computes its centroid (medoid).
 */
public class PointOrganizer {
	/**
	 * The maximum circle radius
	 */
	public double maxRad = 0;
	/**
	 * Whether the points have geographic coordinates (latitudes and longitudes)
	 */
	public boolean geo = false;
	/**
	 * For geographical coordinates: the ratios between the horizontal and vertical
	 * distances in metres and in degrees.
	 */
	public float geoFactorX = 1, geoFactorY = 1;
	/**
	 * The groups of the points
	 */
	protected Vector<PointsInCircle> groups = null;
	/**
	 * The centroids of the groups
	 */
	protected Vector<RealPoint> centroids = null;

	/**
	 * Whether the points have geographic coordinates (latitudes and longitudes)
	 */
	public void setGeo(boolean geo, float geoFactorX, float geoFactorY) {
		this.geo = geo;
		this.geoFactorX = geoFactorX;
		this.geoFactorY = geoFactorY;
	}

	/**
	 * The maximum circle radius
	 */
	public void setMaxRad(double maxRad) {
		this.maxRad = maxRad;
	}

	/**
	 * Adds a new point to the collection. Checks if the point fits
	 * in any of the existing groups. If so, adds it to the group and
	 * re-computes the centroid. If not, creates a new group with
	 * the centroid in this point.
	 */
	public void addPoint(RealPoint p) {
		if (p == null)
			return;
		boolean found = false;
		if (centroids != null && centroids.size() > 0) {
			for (int i = 0; i < groups.size() && !found; i++)
				if (groups.elementAt(i).fitsInCircle(p, true)) {
					found = true;
					groups.elementAt(i).addPoint(p);
					centroids.setElementAt(groups.elementAt(i).getCentre(), i);
				}
		}
		if (!found) {
			if (groups == null) {
				groups = new Vector<PointsInCircle>(100, 100);
			}
			if (centroids == null) {
				centroids = new Vector<RealPoint>(100, 100);
			}
			PointsInCircle gr = new PointsInCircle((float) maxRad, geoFactorX, geoFactorY);
			gr.addPoint(p);
			groups.addElement(gr);
			centroids.addElement(p);
		}
	}

	/**
	 * Finds the cluster the given point may belong to.
	 * Returns the index of the cluster or -1 if not found.
	 */
	public int findClusterForPoint(RealPoint p) {
		if (p == null || groups == null || groups.size() < 1)
			return -1;
		for (int i = 0; i < groups.size(); i++)
			if (groups.elementAt(i).fitsInCircle(p, true))
				return i;
		return -1;
	}

	/**
	 * Checks if the given point is far from all cluster centres,
	 * i.e. the distance is more than 2*maxRad
	 */
	public boolean isFarFromAll(float x, float y) {
		if (centroids == null || centroids.size() < 1)
			return true;
		double mr2 = maxRad * 2;
		for (int i = 0; i < centroids.size(); i++) {
			RealPoint q = centroids.elementAt(i);
			double d = GeoComp.distance(x, y, q.x, q.y, geo);
			if (d <= mr2)
				return false;
		}
		return true;
	}

	/**
	 * Checks if the given point is far from all cluster centres,
	 * i.e. the distance is more than 2*maxRad
	 */
	public boolean isFarFromAll(RealPoint p) {
		if (p == null || centroids == null || centroids.size() < 1)
			return true;
		return isFarFromAll(p.x, p.y);
	}

	public void mergeCloseGroups() {
		if (groups == null || groups.size() < 2)
			return;
		boolean changed = false;
		do {
			changed = false;
			for (int i = 0; i < groups.size() - 1 && !changed; i++) {
				RealPoint p = centroids.elementAt(i);
				for (int j = i + 1; j < groups.size() && !changed; j++) {
					RealPoint q = centroids.elementAt(j);
					double d = GeoComp.distance(p.x, p.y, q.x, q.y, geo);
					if (d <= maxRad) {
						PointsInCircle gr1 = groups.elementAt(i);
						PointsInCircle gr2 = groups.elementAt(j);
						PointsInCircle gr = new PointsInCircle((float) maxRad, geoFactorX, geoFactorY);
						for (int k = 0; k < gr1.getPointCount(); k++) {
							gr.addPoint(gr1.getPoint(k));
						}
						boolean fit = true;
						for (int k = 0; k < gr2.getPointCount() && fit; k++)
							if (gr.fitsInCircle(gr2.getPoint(k), true)) {
								gr.addPoint(gr2.getPoint(k));
							} else {
								fit = false;
							}
						if (fit) {
							groups.removeElementAt(j);
							centroids.removeElementAt(j);
							groups.setElementAt(gr, i);
							centroids.setElementAt(gr.getCentre(), i);
							changed = true;
						}
					}
				}
			}
		} while (changed);
	}

	public RealPoint getCentre(Vector<RealPoint> group) {
		if (group == null || group.size() < 1)
			return null;
		if (group.size() < 2)
			return group.elementAt(0);

		double sumX = 0, sumY = 0;

		for (int i = 0; i < group.size(); i++) {
			RealPoint p = group.elementAt(i);
			sumX += p.x;
			sumY += p.y;
		}
		return new RealPoint((float) (sumX / group.size()), (float) (sumY / group.size()));
	}

	public double getRadius(Vector<RealPoint> group, RealPoint centroid) {
		if (group == null || centroid == null || group.size() < 2)
			return 0;
		double maxd = 0;
		for (int i = 0; i < group.size(); i++) {
			RealPoint p = group.elementAt(i);
			double d = GeoComp.distance(p.x, p.y, centroid.x, centroid.y, geo);
			if (d > maxd) {
				maxd = d;
			}
		}
		return maxd;
	}

	public void recountMedoids() {
		if (groups == null || groups.size() < 1)
			return;
		for (int i = 0; i < groups.size(); i++)
			if (groups.elementAt(i).getPointCount() >= 5) {
				centroids.setElementAt(groups.elementAt(i).getTrueMedoid(), i);
			}
	}

	public int getGroupCount() {
		if (groups == null)
			return 0;
		return groups.size();
	}

	public Vector<RealPoint> getGroup(int idx) {
		if (idx < 0 || groups == null || idx >= groups.size())
			return null;
		return groups.elementAt(idx).points;
	}

	public PointsInCircle getGroupInCircle(int idx) {
		if (idx < 0 || groups == null || idx >= groups.size())
			return null;
		return groups.elementAt(idx);
	}

	public RealPoint getCentroid(int idx) {
		if (idx < 0 || centroids == null || idx >= centroids.size())
			return null;
		return centroids.elementAt(idx);
	}

	public RealPoint getMedoid(int idx) {
		if (idx < 0 || groups == null || idx >= groups.size())
			return null;
		return groups.elementAt(idx).getTrueMedoid();
	}

	/**
	 * Returns either the medoid or centroid of the group with the specified index,
	 * depending on which of them has the smaller average distance to all points
	 */
	public RealPoint getMedoidOrCentroid(int idx) {
		if (idx < 0 || groups == null || idx >= groups.size())
			return null;
		int np = groups.elementAt(idx).getPointCount();
		RealPoint c = centroids.elementAt(idx);
		if (np < 3)
			return c;
		ObjectWithMeasure om = groups.elementAt(idx).getMedoidAndAveDist();
		if (om == null)
			return c;
		double dc = groups.elementAt(idx).getMeanDistToPoint(c.x, c.y);
		if (dc < om.measure)
			return c;
		return (RealPoint) om.obj;
	}

	public double getRadius(int idx) {
		if (idx < 0 || groups == null || centroids == null || idx >= centroids.size() || idx >= groups.size())
			return 0;
		return groups.elementAt(idx).getRadius();
	}
}
