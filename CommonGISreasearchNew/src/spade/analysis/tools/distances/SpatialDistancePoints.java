package spade.analysis.tools.distances;

import spade.vis.dmap.DGeoObject;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.RealPoint;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 23-Apr-2007
 * Time: 17:39:45
 * Computes distances between geographical objects which are assumed to be
 * point objects.
 */
public class SpatialDistancePoints extends SpatialDistance {
	/**
	 * Determines the distance between two objects taking into account the
	 * distance threshold. If in the process of computation it turns out that the
	 * distance is higher than the threshold, the method may stop further
	 * computing and return an arbitrary value greater than the threshold.
	 */
	@Override
	public double findDistance(Object obj1, Object obj2, boolean useThreshold) {
		useThreshold = useThreshold && !Double.isNaN(distanceThreshold);
		if (obj1 == null || obj2 == null)
			return (useThreshold) ? Double.POSITIVE_INFINITY : Double.NaN;
		if (!(obj1 instanceof DGeoObject) || !(obj2 instanceof DGeoObject))
			return (useThreshold) ? Double.POSITIVE_INFINITY : Double.NaN;
		DGeoObject gobj1 = (DGeoObject) obj1, gobj2 = (DGeoObject) obj2;
		Geometry g1 = gobj1.getGeometry(), g2 = gobj2.getGeometry();
		if (g1 == null || g2 == null)
			return (useThreshold) ? Double.POSITIVE_INFINITY : Double.NaN;
		if ((g1 instanceof RealPoint) && (g2 instanceof RealPoint))
			return distance((RealPoint) g1, (RealPoint) g2, useThreshold);
		return distance(g1, g2, useThreshold);
	}
}
