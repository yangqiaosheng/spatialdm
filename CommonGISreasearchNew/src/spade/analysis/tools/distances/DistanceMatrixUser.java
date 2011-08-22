package spade.analysis.tools.distances;

import java.util.HashMap;

import spade.analysis.tools.clustering.DClusterObject;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: 30-Aug-2007
 * Time: 15:57:31
 */
public class DistanceMatrixUser implements DistanceComputer {
	/**
	 * The name of this Distance Computer (if not set externally,
	 * a default value is used).
	 */
	public String methodName = "Distance matrix";

	protected double distanceThreshold = Double.NaN;
	protected float distMatrix[][] = null;

	public DistanceMatrixUser(float[][] distMatrix) {
		this.distMatrix = distMatrix;
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

	@Override
	public void setCoordinatesAreGeographic(boolean geographic) {
	}

	@Override
	public boolean getCoordinatesAreGeographic() {
		return false;
	}

	/**
	 * Sets a threshold for the distances.
	 * If the distance between two objects exceeds the threshold, the method
	 * distance(DGeoObject obj1, DGeoObject obj2) may not return the true
	 * distance but an arbitrary value greater than the threshold.
	 */
	@Override
	public void setDistanceThreshold(double distanceThreshold) {
		this.distanceThreshold = distanceThreshold;
	}

	/**
	 * If necessary, asks the user about additional parameters required for
	 * this Distance Computer. Returns true if successful.
	 * This Distance Computer does not ask anything and returns true immediately.
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
	 * If the argument is not null, adds its parameter-value pairs to the
	 * given hashmap.
	 */
	@Override
	public HashMap getParameters(HashMap params) {
		//has no internal parameters
		return params;
	}

	/**
	 * Does its internal settings according to the given list of parameters
	 */
	@Override
	public void setup(HashMap params) {
		//has no internal parameters; does nothing
	}

	/**
	 * Determines the distance between two geographic objects.
	 * The arguments should be instances of ClusterObject having references to
	 * indexes of objects in the layer as Integers.
	 * The argument useThreshold indicates whether the distance threshold is
	 * taken into account. In such a case, if in the process of computation it
	 * turns out that the distance is higher than the threshold, the method may
	 * stop further computing and return an arbitrary value greater than the
	 * threshold.
	 */
	@Override
	public double findDistance(Object obj1, Object obj2, boolean useThreshold) {
		if (distMatrix == null || obj1 == null || obj2 == null)
			return Double.NaN;
		if (!(obj1 instanceof DClusterObject) || !(obj2 instanceof DClusterObject))
			return Double.NaN;
		DClusterObject clo1 = (DClusterObject) obj1, clo2 = (DClusterObject) obj2;
		int i1 = clo1.numId - 1, i2 = clo2.numId - 1;
		if (i1 < 0 || i1 >= distMatrix.length || i2 < 0 || i2 >= distMatrix.length)
			return Double.NaN;
		return distMatrix[i1][i2];
	}
}
