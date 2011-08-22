package spade.analysis.tools.moves;

import java.util.Vector;

import spade.lib.util.IntArray;
import spade.time.TimeMoment;
import spade.time.TimeReference;
import spade.vis.geometry.GeoComp;
import spade.vis.geometry.RealPoint;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Apr 9, 2008
 * Time: 5:06:57 PM
 * Describe an interaction of two or more trajectories:
 * the trajectories come close together within a given time threshold.
 */
public class InteractionData {
	/**
	 * The close points of the interacting trajectories (instances of RealPoint)
	 */
	public Vector points = null;
	/**
	 * The time references of the points (instances of TimeReference)
	 */
	public Vector timeRefs = null;
	/**
	 * Indexes of the trajectories the points belong to
	 */
	public IntArray trIdxs = null;
	/**
	 * Indexes of the points in the respective trajectories
	 */
	public IntArray pIdxs = null;
	/**
	 * The links between points from different trajectories
	 * that are close in space and time. Each link is an array of
	 * 2 integers indicating the indexes of the points in the vector "points".
	 */
	public Vector links = null;
	/**
	 * The bounding rectangle of all points
	 */
	public float x1 = Float.NaN, x2 = x1, y1 = x1, y2 = x1;
	/**
	 * The minimum and maximum times among all points
	 */
	public TimeMoment t1 = null, t2 = null;

	/**
	 * Checks if the given point is already present in this InteractionData
	 * @param trIdx - index of the trajectory
	 * @param pIdx - index of the point in the trajectory
	 * @return true if the point has already been stored in this structure
	 */
	public boolean hasPoint(int trIdx, int pIdx) {
		if (trIdxs == null || pIdxs == null)
			return false;
		for (int i = 0; i < trIdxs.size(); i++)
			if (trIdxs.elementAt(i) == trIdx && pIdxs.elementAt(i) == pIdx)
				return true;
		return false;
	}

	/**
	 * Tries to find the given point in this InteractionData
	 * @param trIdx - index of the trajectory
	 * @param pIdx - index of the point in the trajectory
	 * @return the index of the point in this structure or -1 if not found
	 */
	public int findPoint(int trIdx, int pIdx) {
		if (trIdxs == null || pIdxs == null)
			return -1;
		for (int i = 0; i < trIdxs.size(); i++)
			if (trIdxs.elementAt(i) == trIdx && pIdxs.elementAt(i) == pIdx)
				return i;
		return -1;
	}

	/**
	 * @param pt - the point to add
	 * @param tref - the temporal reference of the point
	 * @param trIdx - the index of the trajectory the point belongs to
	 * @param pIdx - the index of the point in the trajectory
	 * @return the index of the added point
	 */
	public int addPoint(RealPoint pt, TimeReference tref, int trIdx, int pIdx) {
		if (points == null) {
			points = new Vector(50, 50);
			timeRefs = new Vector(50, 50);
			trIdxs = new IntArray(50, 50);
			pIdxs = new IntArray(50, 50);
		} else {
			int idx = findPoint(trIdx, pIdx);
			if (idx >= 0)
				return idx;
		}
		points.addElement(pt);
		timeRefs.addElement(tref);
		trIdxs.addElement(trIdx);
		pIdxs.addElement(pIdx);
		if (Float.isNaN(x1) || x1 > pt.x) {
			x1 = pt.x;
		}
		if (Float.isNaN(x2) || x2 < pt.x) {
			x2 = pt.x;
		}
		if (Float.isNaN(y1) || y1 > pt.y) {
			y1 = pt.y;
		}
		if (Float.isNaN(y2) || y2 < pt.y) {
			y2 = pt.y;
		}
		if (tref != null) {
			TimeMoment from = tref.getValidFrom(), until = tref.getValidUntil();
			if (from != null && (t1 == null || t1.compareTo(from) > 0)) {
				t1 = from;
			}
			if (until != null && (t2 == null || t2.compareTo(until) < 0)) {
				t2 = until;
			}
		}
		return points.size() - 1;
	}

	/**
	 * Adds a link between two points specified by their indexes in the
	 * vector of points
	 */
	public void addLink(int idx1, int idx2) {
		int link[] = { idx1, idx2 };
		if (links == null) {
			links = new Vector(50, 50);
		}
		links.addElement(link);
	}

	/**
	 * Returns the indexes of all trajectories without repetitions
	 */
	public int[] getAllTrIndexes() {
		if (trIdxs == null || trIdxs.size() < 1)
			return null;
		IntArray difTrIdxs = new IntArray(trIdxs.size(), 1);
		for (int i = 0; i < trIdxs.size(); i++)
			if (difTrIdxs.indexOf(trIdxs.elementAt(i)) < 0) {
				difTrIdxs.addElement(trIdxs.elementAt(i));
			}
		return difTrIdxs.getTrimmedArray();
	}

	/**
	 * For the trajectory with the given index returns the minimum and maximum
	 * point indexes as an array of 2 integers: [min,max]
	 */
	public int[] getMinMaxPointIndexes(int trIdx) {
		if (trIdxs == null || trIdxs.size() < 1)
			return null;
		int minmax[] = null;
		for (int i = 0; i < trIdxs.size(); i++)
			if (trIdx == trIdxs.elementAt(i)) {
				int pIdx = pIdxs.elementAt(i);
				if (minmax == null) {
					minmax = new int[2];
					minmax[0] = minmax[1] = pIdx;
				} else {
					if (minmax[1] < pIdx) {
						minmax[1] = pIdx;
					} else if (minmax[0] > pIdx) {
						minmax[0] = pIdx;
					}
				}
			}
		return minmax;
	}

	/**
	 * Checks if the given point may be close to any of the existing points in space
	 * and time (uses the bounding rectangle and the time interval)
	 */
	public boolean mayFit(RealPoint pt, TimeReference tref, float distThrX, float distThrY, int timeThr) {
		if (points == null || pt == null || tref == null)
			return false;
		if (pt.x < x1 - distThrX || pt.x > x2 + distThrX || pt.y < y1 - distThrY || pt.y > y2 + distThrY)
			return false;
		TimeMoment from = tref.getValidFrom(), until = tref.getValidUntil();
		if (from == null || until == null)
			return false;
		if (t1.subtract(from) > timeThr || until.subtract(t2) > timeThr)
			return false;
		return true;
	}

	/**
	 * Checks if there is a point in the collection spatially and temporally close to the given point
	 * and belonging to a different trajectory.
	 */
	public boolean doesFit(RealPoint pt, int trIdx, TimeReference tref, float distThr, boolean geo, int timeThr) {
		if (points == null || pt == null || tref == null)
			return false;
		if (trIdxs.size() == 1 && trIdx == trIdxs.elementAt(0))
			return false;
		for (int i = 0; i < points.size(); i++)
			if (trIdx != trIdxs.elementAt(i)) {
				RealPoint p = (RealPoint) points.elementAt(i);
				TimeReference tr = (TimeReference) timeRefs.elementAt(i);
				if (areNeighbours(pt, tref, p, tr, distThr, geo, timeThr))
					return true;
			}
		return false;
	}

	public static boolean areNeighbours(RealPoint p1, TimeReference tr1, RealPoint p2, TimeReference tr2, float distThr, boolean geo, int timeThr) {
		if (p1 == null || p2 == null || tr1 == null || tr2 == null)
			return false;
		if (!tr1.isValid(tr2.getValidFrom(), tr2.getValidUntil())) {
			if (tr1.getValidFrom().subtract(tr2.getValidUntil()) > timeThr)
				return false;
			if (tr2.getValidFrom().subtract(tr1.getValidUntil()) > timeThr)
				return false;
		}
		return GeoComp.distance(p1.x, p1.y, p2.x, p2.y, geo) <= distThr;
	}

	/**
	 * Checks if this interaction overlaps with the given interaction, which means
	 * at least one common point
	 */
	public boolean overlaps(InteractionData iData) {
		if (iData == null)
			return false;
		if (trIdxs == null || iData.trIdxs == null)
			return false;
		boolean hasCommon = false;
		for (int i = 0; i < trIdxs.size() && !hasCommon; i++) {
			hasCommon = iData.trIdxs.indexOf(trIdxs.elementAt(i)) >= 0;
		}
		if (!hasCommon)
			return false;
		if (t1 != null && t2 != null && iData.t1 != null && iData.t2 != null) {
			if (t1.compareTo(iData.t2) > 0)
				return false;
			if (t2.compareTo(iData.t1) < 0)
				return false;
		}
		for (int i = 0; i < trIdxs.size(); i++)
			if (iData.hasPoint(trIdxs.elementAt(i), pIdxs.elementAt(i)))
				return true;
		return false;
	}

	/**
	 * Checks if this interaction and the given interaction have a common
	 * trajectory fragment of a significant length
	 */
	public boolean haveCommonTrajectoryFragment(InteractionData iData) {
		if (iData == null)
			return false;
		if (trIdxs == null || iData.trIdxs == null)
			return false;
		if (t1 != null && t2 != null && iData.t1 != null && iData.t2 != null) {
			if (t1.compareTo(iData.t2) > 0)
				return false;
			if (t2.compareTo(iData.t1) < 0)
				return false;
		}
		for (int i = 0; i < trIdxs.size(); i++) {
			int trIdx = trIdxs.elementAt(i);
			if (i > 0 && trIdxs.indexOf(trIdx) < i) {
				continue; //already checked
			}
			if (iData.trIdxs.indexOf(trIdx) < 0) {
				continue;
			}
			int mm0[] = getMinMaxPointIndexes(trIdx), mm[] = iData.getMinMaxPointIndexes(trIdx);
			if (mm0[0] <= mm[0] && mm0[1] >= mm[1])
				return true;
			if (mm0[0] >= mm[0] && mm0[1] <= mm[1])
				return true;
			return false;
			/*
			if (mm0[1]<mm[0] || mm[1]<mm0[0]) continue;
			if (mm0[0]==mm[0] && mm0[1]==mm[1]) return true;
			int nOverlap=Math.min(mm0[1],mm[1])-Math.max(mm0[0],mm[0])+1;
			if (nOverlap>=Math.max(mm0[1]-mm0[0]+1,mm[1]-mm[0]+1)/2) return true;
			*/
		}
		return false;
	}

	/**
	 * Unites this interaction with the given interaction
	 */
	public void unite(InteractionData iData) {
		if (iData == null || iData.trIdxs.size() < 1)
			return;
		int idxs[] = new int[iData.trIdxs.size()];
		for (int i = 0; i < iData.trIdxs.size(); i++) {
			idxs[i] = findPoint(iData.trIdxs.elementAt(i), iData.pIdxs.elementAt(i));
			if (idxs[i] < 0) {
				TimeReference tr = (TimeReference) iData.timeRefs.elementAt(i);
				idxs[i] = addPoint((RealPoint) iData.points.elementAt(i), tr, iData.trIdxs.elementAt(i), iData.pIdxs.elementAt(i));
			}
		}
		for (int i = 0; i < iData.links.size(); i++) {
			int link[] = (int[]) iData.links.elementAt(i);
			addLink(idxs[link[0]], idxs[link[1]]);
		}
	}

}
