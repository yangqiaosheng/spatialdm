package spade.analysis.tools.distances;

import spade.vis.dmap.TrajectoryObject;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 23-Apr-2007
 * Time: 18:08:29
 * Computes distances between geographical objects which are assumed to be
 * trajectories (instances of DMovingObject).
 */
public abstract class SpatialDistanceTrajectories extends SpatialDistance {
	/**
	 * Determines the distance between two trajectories
	 */
	public abstract double distance(TrajectoryObject obj1, TrajectoryObject obj2, boolean useThreshold);

/*
  public double distance (TrajectoryObject obj1, TrajectoryObject obj2, boolean useThreshold) {
    useThreshold=useThreshold && !Double.isNaN(distanceThreshold);
    if (obj1==null || obj2==null) return (useThreshold)?Double.POSITIVE_INFINITY:Double.NaN;
    Vector tr1=obj1.mobj.getTrack(), tr2=obj2.mobj.getTrack();
    int min=Math.min(tr1.size(),tr2.size());
    double dist=0;
    int np=0;
    for (int i=0; i<min; i++) {
      int i1=tr1.size()-i-1, i2=tr2.size()-i-1;
      SpatialEntity sp1=(SpatialEntity)tr1.elementAt(i1);
      SpatialEntity sp2=(SpatialEntity)tr2.elementAt(i2);
      double d=distance(sp1.getGeometry(),sp2.getGeometry(),false);
      //if (useThreshold && (d>distanceThreshold))
        //return Double.POSITIVE_INFINITY;
      ++np;
      dist+=d;
      if (useThreshold && (dist/np>distanceThreshold))
        return Double.POSITIVE_INFINITY;
    }
    return dist/np;
  }
*/
	/**
	 * Determines the distance between two objects taking into account the
	 * distance threshold. If in the process of computation it turns out that the
	 * distance is higher than the threshold, the method may stop further
	 * computing and return an arbitrary value greater than the threshold.
	 * The objects must be instances of TrajectoryObject.
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
