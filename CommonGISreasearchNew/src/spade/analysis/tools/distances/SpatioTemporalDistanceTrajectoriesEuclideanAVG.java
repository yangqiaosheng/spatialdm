package spade.analysis.tools.distances;

import java.util.Vector;

import spade.time.TimeReference;
import spade.vis.database.SpatialEntity;
import spade.vis.dmap.TrajectoryObject;

public class SpatioTemporalDistanceTrajectoriesEuclideanAVG extends SpatialDistance {
	/**
	 * Determines the distance between two trajectories
	 */
	public double distance(TrajectoryObject obj1, TrajectoryObject obj2, boolean useThreshold) {
		useThreshold = useThreshold && !Double.isNaN(distanceThreshold);
		double inf = (useThreshold ? Double.POSITIVE_INFINITY : Double.NaN);
		if (obj1 == null || obj2 == null)
			return (useThreshold) ? Double.POSITIVE_INFINITY : Double.NaN;
		if (!obj1.mobj.getTimeReference().isValid(obj2.mobj.getStartTime(), obj2.mobj.getEndTime()))
			return inf;
		Vector tr1 = obj1.mobj.getTrack(), tr2 = obj2.mobj.getTrack();
		int min = Math.min(tr1.size(), tr2.size());
		double dist = 0;
		int i = 1;
		int j = 1;
		boolean computed = false;
		int unsynchro = 0;
		int syncro = 0;

		while ((i < tr1.size()) && (j < tr2.size())) {
			// if (t1[i].z == t2[j].z) else
			SpatialEntity sp1 = (SpatialEntity) tr1.elementAt(i);
			SpatialEntity sp2 = (SpatialEntity) tr2.elementAt(j);
			SpatialEntity sp2prev = (SpatialEntity) (j > 0 ? tr2.elementAt(j - 1) : tr2.elementAt(j));
			TimeReference rf2prev = sp2prev.getTimeReference();

			//sp1.t == sp2.t
			if (sp1.getTimeReference().isValid(sp2.getTimeReference().getValidFrom())) {
				dist += distance(sp1.getGeometry(), sp2.getGeometry(), useThreshold);
				i++;
				j++;
				syncro++;
				computed = true;
			} else if (sp1.getTimeReference().isValid(sp2prev.getTimeReference().getValidFrom(), rf2prev.getValidFrom())) {
				// Compute the interpolated distance. At the moment use the previous point.
				dist += distance(sp1.getGeometry(), sp2prev.getGeometry(), useThreshold);
				i++;
				j++;
				syncro++;
				computed = true;
			} else if (sp1.getTimeReference().getValidFrom().compareTo(sp2.getTimeReference().getValidFrom()) < 0) {
				i++;
				unsynchro++;
			} else if (sp1.getTimeReference().getValidFrom().compareTo(sp2.getTimeReference().getValidFrom()) > 0) {
				j++;
				unsynchro++;
			}
		}
		//computed = ((computed && (syncro > (min / 2))) ? computed : false);
		return (computed ? unsynchro * dist / (syncro * min) : inf);
		//return (computed ? dist / (min) : inf);
	}

	/**
	 * Determines the distance between two objects taking into account the
	 * distance threshold. If in the process of computation it turns out that
	 * the distance is higher than the threshold, the method may stop further
	 * computing and return an arbitrary value greater than the threshold. The
	 * objects must be instances of TrajectoryObject.
	 */
	@Override
	public double findDistance(Object obj1, Object obj2, boolean useThreshold) {
		useThreshold = useThreshold && !Double.isNaN(distanceThreshold);
		if (obj1 == null || obj2 == null)
			return (useThreshold) ? Double.POSITIVE_INFINITY : Double.NaN;
		if (!(obj1 instanceof TrajectoryObject) || !(obj2 instanceof TrajectoryObject))
			return (useThreshold) ? Double.POSITIVE_INFINITY : Double.NaN;
		return distance((TrajectoryObject) obj1, (TrajectoryObject) obj2, useThreshold);
	}

}
