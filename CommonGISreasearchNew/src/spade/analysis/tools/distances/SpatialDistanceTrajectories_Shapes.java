package spade.analysis.tools.distances;

import java.util.Vector;

import spade.vis.database.SpatialEntity;
import spade.vis.dmap.TrajectoryObject;
import spade.vis.geometry.RealPoint;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: 17-Aug-2007
 * Time: 16:06:15
 * Estimates the distance between two trajectories according to their shapes
 */
public class SpatialDistanceTrajectories_Shapes extends SpatialDistanceTrajectories {
	public static double interPointDistDiffTolerance = 1.2;

	/**
	 * Informs whether multiple points (e.g. of trajectories) are
	 * used for computing distances.
	 * Returns true.
	 */
	@Override
	public boolean usesMultiplePoints() {
		return true;
	}

	/**
	 * Determines the distance between two trajectories
	 */
	@Override
	public double distance(TrajectoryObject obj1, TrajectoryObject obj2, boolean useThreshold) {
		useThreshold = useThreshold && !Double.isNaN(distanceThreshold);
		if (obj1 == null || obj2 == null)
			return (useThreshold) ? Double.POSITIVE_INFINITY : Double.NaN;
		Vector tr1 = obj1.mobj.getTrack(), tr2 = obj2.mobj.getTrack();
		if (tr1 == null || tr2 == null || tr1.size() < 1 || tr2.size() < 1)
			return (useThreshold) ? Double.POSITIVE_INFINITY : Double.NaN;
		double dist = 0, penaltyDist = 0, commonDist = 0;
		int i1 = 0, i2 = 0, np = 0;
		RealPoint commonP = null;
		double d1[] = obj1.mobj.getDistances(), d2[] = obj2.mobj.getDistances();
		while (i1 < tr1.size() && i2 < tr2.size()) {
			if (i1 > 0 && i2 > 0) {
				//this is to compensate for unequal distances between successive points in
				//two trajectories
				double dp1 = d1[i1 - 1], dp2 = d2[i2 - 1], diff = Math.abs(dp1 - dp2);
				if (dp1 * interPointDistDiffTolerance < dp2) {
					while (i1 < d1.length) {
						dp1 += d1[i1];
						double diff1 = Math.abs(dp2 - dp1);
						if (diff1 < diff) {
							++i1;
							diff = diff1;
						} else {
							break;
						}
					}
				} else if (dp1 > dp2 * interPointDistDiffTolerance) {
					while (i2 < d2.length) {
						dp2 += d2[i2];
						double diff1 = Math.abs(dp2 - dp1);
						if (diff1 < diff) {
							++i2;
							diff = diff1;
						} else {
							break;
						}
					}
				}
			}
			SpatialEntity sp1 = (SpatialEntity) tr1.elementAt(i1);
			SpatialEntity sp2 = (SpatialEntity) tr2.elementAt(i2);
			double d = distance(sp1.getGeometry(), sp2.getGeometry(), false);
			if (i1 < tr1.size() - 1) {
				int i01 = i1 + 1;
				boolean closer = false;
				do {
					SpatialEntity sp01 = (SpatialEntity) tr1.elementAt(i01);
					double d0 = distance(sp01.getGeometry(), sp2.getGeometry(), false);
					closer = d0 < d;
					if (closer) {
						penaltyDist += distance(sp1.getGeometry(), sp01.getGeometry(), false);
						d = d0;
						i1 = i01;
						sp1 = sp01;
						++i01;
					}
				} while (closer && i01 < tr1.size());
			}
			if (i2 < tr2.size() - 1) {
				int i02 = i2 + 1;
				boolean closer = false;
				do {
					SpatialEntity sp02 = (SpatialEntity) tr2.elementAt(i02);
					double d0 = distance(sp1.getGeometry(), sp02.getGeometry(), false);
					closer = d0 < d;
					if (closer) {
						penaltyDist += distance(sp2.getGeometry(), sp02.getGeometry(), false);
						d = d0;
						i2 = i02;
						sp2 = sp02;
						++i02;
					}
				} while (closer && i02 < tr2.size());
			}
			dist += d;
			++np;
			if (useThreshold && dist / np > distanceThreshold)
				return Double.POSITIVE_INFINITY;
			RealPoint p1 = sp1.getCentre(), p2 = sp2.getCentre(), p = new RealPoint((p1.x + p2.x) / 2, (p1.y + p2.y) / 2);
			//The following code works well for the Sank Augustin trajectories
/*
      if (commonP!=null)
        penaltyDist-=0.5*distance(p,commonP,false);
*/
			//end
			//The following code needs to be tested
			if (commonP != null) {
				double dd = distance(p, commonP, false);
				commonDist += dd;
			}
			//end
			commonP = p;
			++i1;
			++i2;
		}
		if (np < 1)
			return (useThreshold) ? Double.POSITIVE_INFINITY : Double.NaN;
		RealPoint cpOld = commonP;
		while (i1 < tr1.size()) {
			RealPoint p = ((SpatialEntity) tr1.elementAt(i1)).getCentre();
			penaltyDist += distance(p, commonP, false);
			commonP = p;
			++i1;
		}
		commonP = cpOld;
		while (i2 < tr2.size()) {
			RealPoint p = ((SpatialEntity) tr2.elementAt(i2)).getCentre();
			penaltyDist += distance(p, commonP, false);
			commonP = p;
			++i2;
		}
		//the following code needs to be thought about...
		dist /= np;
		if (penaltyDist > 0) {
			if (commonDist == 0)
				return (useThreshold) ? Double.POSITIVE_INFINITY : Double.NaN;
			//dist+=penaltyDist;
			dist += penaltyDist * penaltyDist / commonDist;
		}
		//end
		if (useThreshold && dist > distanceThreshold)
			return Double.POSITIVE_INFINITY;
		return dist;
	}
}
