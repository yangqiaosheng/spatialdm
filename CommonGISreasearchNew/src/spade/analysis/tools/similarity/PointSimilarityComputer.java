package spade.analysis.tools.similarity;

import spade.analysis.tools.distances.SpatialDistancePoints;
import spade.vis.dmap.DGeoObject;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 24-Apr-2007
 * Time: 12:10:28
 */
public class PointSimilarityComputer extends SimilarityComputer {
	/**
	 * Generates an appropriate distance computer depending on the type
	 * of the objects in the layer. May ask the user for parameters of the
	 * similarity computation. Returns true if successful.
	 */
	@Override
	protected boolean getDistanceComputer() {
		distComp = new SpatialDistancePoints();
		return true;
	}

	/**
	 * Can do some preparation of the data for the computing if necessary
	 */
	@Override
	protected void prepareData() {
	}

	/**
	 * Computes the distance for the object with the given index in the layer.
	 */
	@Override
	protected double getDistanceForObject(int idx) {
		DGeoObject gobj = layer.getObject(idx);
		return distComp.findDistance(gobj, selObj, false);
	}

	/**
	 * Computes the distance between the objects with the given indexes in the layer.
	 */
	@Override
	protected double getDistanceBetweenObjects(int idx1, int idx2) {
		DGeoObject gobj1 = layer.getObject(idx1), gobj2 = layer.getObject(idx2);
		if (gobj1 == null || gobj2 == null)
			return Double.NaN;
		return distComp.findDistance(gobj1, gobj2, false);
	}
}
