package spade.analysis.tools.distances;

import java.util.HashMap;
import java.util.Vector;

import spade.lib.basicwin.CManager;
import spade.lib.basicwin.Dialogs;
import spade.vis.database.SpatialEntity;
import spade.vis.dmap.TrajectoryObject;

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: 25-Apr-2007
 * Time: 11:05:53
 * To change this template use File | Settings | File Templates.
 */
public class SpatialDistanceTrajectories_EndsAndNMidpoints extends SpatialDistance implements TimeTransformationUser {
	protected int N = 1;

	/**
	 * Informs whether multiple points (e.g. of trajectories) are
	 * used for computing distances.
	 * Returns true if the number of midpoints >1.
	 */
	@Override
	public boolean usesMultiplePoints() {
		return N > 1;
	}

	/**
	 * If necessary, asks the user about additional parameters required for
	 * this Distance Computer. Returns true if successful.
	 */
	@Override
	public boolean askParameters() {
		N = Dialogs.askForIntValue(CManager.getAnyFrame(), "Number of midpoints?", 10, 0, 1000, "Method of computing distances: " + methodName, "Method parameter", false);
		return N > 0;
	}

	/**
	 * Returns a string describing the current parameter settings of the method.
	 * May return null if no additional parameters are used.
	 */
	@Override
	public String getParameterDescription() {
		return "Number of midpoints = " + N;
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
		params = super.getParameters(params);
		if (params == null) {
			params = new HashMap(20);
		}
		params.put("N_midpoints", String.valueOf(N));
		return params;
	}

	/**
	 * Does its internal settings according to the given list of parameters
	 */
	@Override
	public void setup(HashMap params) {
		if (params == null)
			return;
		super.setup(params);
		String val = (String) params.get("N_midpoints");
		if (val != null) {
			try {
				N = Integer.parseInt(val);
			} catch (NumberFormatException e) {
			}
		}
	}

	/**
	 * Returns the required type of time transformation
	 */
	@Override
	public int getTimeTransformationType() {
		return TrajectoryObject.TIME_RELATIVE_ENDS;
	}

	/**
	 * Determines the distance between two trajectories
	 */
	public double distance(TrajectoryObject obj1, TrajectoryObject obj2, boolean useThreshold) {
		useThreshold = useThreshold && !Double.isNaN(distanceThreshold);
		if (obj1 == null || obj2 == null)
			return (useThreshold) ? Double.POSITIVE_INFINITY : Double.NaN;
		Vector tr1 = obj1.mobj.getTrack(), tr2 = obj2.mobj.getTrack();
		SpatialEntity sp1 = (SpatialEntity) tr1.elementAt(0);
		SpatialEntity sp2 = (SpatialEntity) tr2.elementAt(0);
		double d = distance(sp1.getGeometry(), sp2.getGeometry(), true), dsum = d;
		//if (useThreshold && (d>distanceThreshold))
		//  return Double.POSITIVE_INFINITY;
		sp1 = (SpatialEntity) tr1.elementAt(tr1.size() - 1);
		sp2 = (SpatialEntity) tr2.elementAt(tr2.size() - 1);
		d = distance(sp1.getGeometry(), sp2.getGeometry(), true);
		//if (useThreshold && (d>distanceThreshold))
		//  return Double.POSITIVE_INFINITY;
		dsum += d;
		if (useThreshold && (dsum > 2 * distanceThreshold))
			return Double.POSITIVE_INFINITY;
		int idx1[] = obj1.getNRelTimesIdx(N), idx2[] = obj2.getNRelTimesIdx(N);
		int nn = 0;
		for (int i = 0; i < N; i++) {
			int i1 = idx1[i], i2 = idx2[i];
			if (i1 > 0 && i2 > 0) { // sometimes it is impossible to get N midpoints without the use of endpoints
				sp1 = (SpatialEntity) tr1.elementAt(i1);
				sp2 = (SpatialEntity) tr2.elementAt(i2);
				d = distance(sp1.getGeometry(), sp2.getGeometry(), true);
				//if (useThreshold && (d>distanceThreshold))
				//  return Double.POSITIVE_INFINITY;
				dsum += d;
				nn++;
			}
		}
		dsum /= (2 + nn);
		if (useThreshold && (dsum > distanceThreshold))
			return Double.POSITIVE_INFINITY;
		else
			return dsum;
	}

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
