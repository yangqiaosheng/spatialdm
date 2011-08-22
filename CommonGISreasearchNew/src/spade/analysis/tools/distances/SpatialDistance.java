package spade.analysis.tools.distances;

import java.util.HashMap;

import spade.lib.util.GeoDistance;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.MultiGeometry;
import spade.vis.geometry.RealPoint;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 23-Apr-2007
 * Time: 18:09:39
 * A base class for classes computing spatial distances between various types of DGeoObjects
 */
public abstract class SpatialDistance implements DistanceComputer {
	/**
	 * The name of this Distance Computer (if not set externally,
	 * a default value is used).
	 */
	public String methodName = "Spatial distance";

	protected boolean geographic = false;
	/**
	 * The distance threshold
	 */
	protected double distanceThreshold = Double.NaN;
	/**
	 * For geographical coordinates, the angular distance corresponding to the
	 * distance threshold.
	 */
	protected double angDist = Double.NaN;

	/**
	 * Sets the name of this Distance Computer
	 */
	@Override
	public void setMethodName(String name) {
		methodName = name;
	}

	/**
	 * Returns the name of this Distance Computer
	 */
	@Override
	public String getMethodName() {
		return methodName;
	}

	/**
	 * Informs whether multiple points (e.g. of trajectories) are
	 * used for computing distances.
	 * By default, returns false.
	 */
	@Override
	public boolean usesMultiplePoints() {
		return false;
	}

	@Override
	public void setCoordinatesAreGeographic(boolean geographic) {
		this.geographic = geographic;
	}

	@Override
	public boolean getCoordinatesAreGeographic() {
		return geographic;
	}

	/**
	 * Sets a threshold for the distances.
	 * If the distance between two objects exceeds the threshold, the method
	 * distance(DGeoObject obj1, DGeoObject obj2) may not return the true distance
	 * but an arbitrary value greater than the threshold.
	 */
	@Override
	public void setDistanceThreshold(double distanceThreshold) {
		this.distanceThreshold = distanceThreshold;
	}

	/**
	 * If necessary, asks the user about additional parameters required for
	 * this Distance Computer. Returns true if successful.
	 * By default, does not ask anything and returns true immediately.
	 */
	@Override
	public boolean askParameters() {
		return true;
	}

	/**
	 * Returns a string describing the current parameter settings of the method.
	 * May return null if no additional parameters are used.
	 */
	@Override
	public String getParameterDescription() {
		return null;
	}

	/**
	 * Returns its parameters, which are necessary and sufficient
	 * for setting up the DistanceComputer. Each parameter has a name,
	 * which is used as a key in the HashMap.
	 * The values of the parameters must be stored in the hashmap as strings!
	 * This is necessary for saving the parameters in a text file.
	 * If the argument is not null, adds its parameter-value pairs to the
	 * given hashmap.
	 */
	@Override
	public HashMap getParameters(HashMap params) {
		if (params == null) {
			params = new HashMap(20);
		}
		params.put("geographic", new Boolean(geographic).toString());
		return params;
	}

	/**
	 * Does its internal settings according to the given list of parameters
	 */
	@Override
	public void setup(HashMap params) {
		if (params == null)
			return;
		String val = (String) params.get("geographic");
		if (val == null)
			return;
		geographic = Boolean.parseBoolean(val);
	}

	/**
	 * Determines the distance between two geographic objects.
	 * The arguments should be instances of DGeoObject or other class having
	 * a reference to a DGeoObject.
	 * The argument useThreshold indicates whether the distance threshold is
	 * taken into account. In such a case, if in the process of computation it
	 * turns out that the distance is higher than the threshold, the method may
	 * stop further computing and return an arbitrary value greater than the
	 * threshold.
	 */
	@Override
	abstract public double findDistance(Object obj1, Object obj2, boolean useThreshold);

	/**
	 * Determines the distance between two points
	 */
	public double distance(RealPoint p1, RealPoint p2, boolean useThreshold) {
		useThreshold = useThreshold && !Double.isNaN(distanceThreshold);
		if (geographic) {
			if (useThreshold) {
				if (Double.isNaN(angDist)) {
					angDist = GeoDistance.distToAngle(distanceThreshold);
				}
				if (Math.abs(p1.x - p2.x) > angDist)
					return Double.POSITIVE_INFINITY;
				if (Math.abs(p1.y - p2.y) > angDist)
					return Double.POSITIVE_INFINITY;
			}
			return GeoDistance.geoDist(p1.x, p1.y, p2.x, p2.y);
		}
		double dx = (p1.x - p2.x);
		if (useThreshold && dx > distanceThreshold)
			return Double.POSITIVE_INFINITY;
		double dy = (p1.y - p2.y);
		if (useThreshold && dy > distanceThreshold)
			return Double.POSITIVE_INFINITY;
		return Math.sqrt(dx * dx + dy * dy);
	}

	/**
	 * Determines the distance between two geometries. If these are not RealPoints,
	 * computes the distance between the centres of their bounding rectangles.
	 */
	public double distance(Geometry g1, Geometry g2, boolean useThreshold) {
		useThreshold = useThreshold && !Double.isNaN(distanceThreshold);
		if (g1 == null || g2 == null)
			return (useThreshold) ? Double.POSITIVE_INFINITY : Double.NaN;
		Geometry gSet1[] = null, gSet2[] = null;
		if (g1 instanceof MultiGeometry) {
			MultiGeometry mg = (MultiGeometry) g1;
			if (mg.getPartsCount() < 1)
				return (useThreshold) ? Double.POSITIVE_INFINITY : Double.NaN;
			gSet1 = new Geometry[mg.getPartsCount()];
			for (int i = 0; i < gSet1.length; i++) {
				gSet1[i] = mg.getPart(i);
			}
		}
		if (g2 instanceof MultiGeometry) {
			MultiGeometry mg = (MultiGeometry) g2;
			if (mg.getPartsCount() < 1)
				return (useThreshold) ? Double.POSITIVE_INFINITY : Double.NaN;
			gSet2 = new Geometry[mg.getPartsCount()];
			for (int i = 0; i < gSet2.length; i++) {
				gSet2[i] = mg.getPart(i);
			}
		}
		if (gSet1 != null || gSet2 != null) {
			if (gSet1 == null) {
				gSet1 = new Geometry[1];
				gSet1[0] = g1;
			}
			if (gSet2 == null) {
				gSet2 = new Geometry[1];
				gSet2[0] = g2;
			}
			double minDist = Double.POSITIVE_INFINITY;
			for (Geometry element : gSet1) {
				for (Geometry element2 : gSet2) {
					double d = distance(element, element2, useThreshold);
					if (d == 0)
						return d;
					if (d < minDist) {
						minDist = d;
					}
				}
			}
			return minDist;
		}
		if (g1 instanceof RealPoint) {
			RealPoint p1 = (RealPoint) g1;
			if (g2 instanceof RealPoint)
				return distance(p1, (RealPoint) g2, useThreshold);
			if (g2.contains(p1.x, p1.y, (useThreshold) ? (float) distanceThreshold : 0f))
				return 0;
			return (useThreshold) ? Double.POSITIVE_INFINITY : Double.NaN;
		}
		if (g2 instanceof RealPoint) {
			RealPoint p2 = (RealPoint) g2;
			if (g1.contains(p2.x, p2.y, (useThreshold) ? (float) distanceThreshold : 0f))
				return 0;
			return (useThreshold) ? Double.POSITIVE_INFINITY : Double.NaN;
		}
		float bounds[] = g1.getBoundRect();
		RealPoint p1 = new RealPoint((bounds[0] + bounds[2]) / 2, (bounds[1] + bounds[3]) / 2);
		bounds = g2.getBoundRect();
		RealPoint p2 = new RealPoint((bounds[0] + bounds[2]) / 2, (bounds[1] + bounds[3]) / 2);
		return distance(p1, p2, useThreshold);
	}
}
