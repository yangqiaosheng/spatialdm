package spade.analysis.tools.distances;

import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 23-Apr-2007
 * Time: 17:42:04
 */
public interface DistanceComputer {
	/**
	 * Sets the name of this Distance Computer
	 */
	public void setMethodName(String name);

	/**
	 * Returns the name of this Distance Computer
	 */
	public String getMethodName();

	/**
	 * Informs whether multiple points (e.g. of trajectories) are
	 * used for computing distances
	 */
	public boolean usesMultiplePoints();

	/**
	 * @param geographic - indicates that the coordinates are geographic,
	 * i.e. longitudes and latitudes
	 */
	public void setCoordinatesAreGeographic(boolean geographic);

	/**
	 * informs whether the coordinates are geographic,
	 * i.e. longitudes and latitudes
	 */
	public boolean getCoordinatesAreGeographic();

	/**
	 * Sets a threshold for the distances.
	 * If the distance between two objects exceeds the threshold, the method
	 * distance(DGeoObject obj1, DGeoObject obj2) may not return the true
	 * distance but an arbitrary value greater than the threshold.
	 */
	public void setDistanceThreshold(double distanceThreshold);

	/**
	 * If necessary, asks the user about additional parameters required for
	 * this Distance Computer. Returns true if successful.
	 */
	public boolean askParameters();

	/**
	 * Returns a string describing the current parameter settings of the method.
	 * May return null if no additional parameters are used.
	 */
	public String getParameterDescription();

	/**
	 * Returns its parameters, which are necessary and sufficient
	 * for setting up the DistanceComputer. Each parameter has a name,
	 * which is used as a key in the HashMap.
	 * The values of the parameters must be stored in the hashmap as strings!
	 * This is necessary for saving the parameters in a text file.
	 * If the argument is not null, adds its parameter-value pairs to the
	 * given hashmap.
	 */
	public HashMap getParameters(HashMap params);

	/**
	 * Does its internal settings according to the given list of parameters
	 */
	public void setup(HashMap params);

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
	public double findDistance(Object obj1, Object obj2, boolean useThreshold);
}
