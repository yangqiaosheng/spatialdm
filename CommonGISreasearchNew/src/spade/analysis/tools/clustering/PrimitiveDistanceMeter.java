package spade.analysis.tools.clustering;

import java.util.Collection;
import java.util.HashMap;
import java.util.Vector;

import spade.analysis.tools.distances.DistanceComputer;
import spade.lib.util.BubbleSort;
import spade.lib.util.ObjectWithMeasure;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Jan 7, 2009
 * Time: 11:24:28 AM
 */
public class PrimitiveDistanceMeter implements DistanceMeterExt<DClusterObject> {
	/**
	 * The DistanceComputer is used to compute distances
	 */
	protected DistanceComputer distComp = null;

	/**
	 * Informs if it deals with geographical coordinates (latitudes and longitudes)
	 */
	@Override
	public boolean isGeographic() {
		if (distComp != null)
			return distComp.getCoordinatesAreGeographic();
		return false;
	}

	/**
	 * Returns its parameters, which are necessary and sufficient
	 * for setting up the DistanceMeter. Each parameter has a name,
	 * which is used as a key in the HashMap.
	 * The values of the parameters must be stored in the hashmap as strings!
	 * This is necessary for saving the parameters in a text file.
	 * If the argument is not null, adds its parameter-value pairs to the
	 * given hashmap.
	 */
	@Override
	public HashMap getParameters(HashMap params) {
		if (distComp != null) {
			if (params == null) {
				params = new HashMap(20);
			}
			params.put("distanceComputer", distComp.getClass().getName());
			distComp.getParameters(params);
		}
		return params;
	}

	/**
	 * Does its internal settings according to the given list of parameters
	 */
	@Override
	public void setup(HashMap params) {
		if (params == null)
			return;
		String val = (String) params.get("distanceComputer");
		if (val != null) {
			try {
				Object obj = Class.forName(val).newInstance();
				if (obj != null && (obj instanceof DistanceComputer)) {
					distComp = (DistanceComputer) obj;
				}
			} catch (Exception e) {
			}
		}
		if (distComp != null) {
			distComp.setup(params);
		}
	}

	/**
	 * Returns true if it has all necessary internal settings
	 */
	@Override
	public boolean hasValidSettings() {
		return distComp != null;
	}

	public DistanceComputer getDistanceComputer() {
		return distComp;
	}

	public void setDistanceComputer(DistanceComputer distComp) {
		this.distComp = distComp;
	}

	@Override
	public double distance(DClusterObject o1, DClusterObject o2) {
		if (o1 == null || o2 == null)
			return Double.POSITIVE_INFINITY;
		return distComp.findDistance(o1.originalObject, o2.originalObject, false);
	}

	@Override
	public Collection<DClusterObject> neighbors(DClusterObject core, Collection<DClusterObject> objects, double eps) {
		Vector neighborsWithDist = new Vector(100, 100);
		for (DClusterObject o : objects) {
			if (!o.equals(core)) {
				double dist = distance(o, core);
				if (dist <= eps) {
					ObjectWithMeasure om = new ObjectWithMeasure(o, dist);
					neighborsWithDist.addElement(om);
				}
			}
		}
		if (neighborsWithDist.size() > 1) {
			BubbleSort.sort(neighborsWithDist);
		}
		Vector<DClusterObject> neighbors = new Vector<DClusterObject>(neighborsWithDist.size(), 10);
		for (int i = 0; i < neighborsWithDist.size(); i++) {
			neighbors.addElement((DClusterObject) ((ObjectWithMeasure) neighborsWithDist.elementAt(i)).obj);
		}
		return neighbors;
	}
}
