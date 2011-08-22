package spade.analysis.tools.distances;

import java.util.HashMap;
import java.util.Vector;

import spade.lib.basicwin.CManager;
import spade.lib.basicwin.Dialogs;
import spade.time.TimeMoment;
import spade.vis.database.SpatialEntity;
import spade.vis.dmap.TrajectoryObject;

public class SpatioTemporalDistanceTrajectories extends SpatialDistance {

	protected int timeCoarsening = 0;

	/**
	 * If necessary, asks the user about additional parameters required for
	 * this Distance Computer. Returns true if successful.
	 */
	@Override
	public boolean askParameters() {
		timeCoarsening = Dialogs.askForIntValue(CManager.getAnyFrame(), "Radius of the interval?", 10, 0, 1000, "Method of computing distances: " + methodName, "Time Coarsening Parameter", false);
		return timeCoarsening > 0;
	}

	/**
	 * Returns a string describing the current parameter settings of the method.
	 * May return null if no additional parameters are used.
	 */
	@Override
	public String getParameterDescription() {
		return "Radius of the interval = " + timeCoarsening;
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
		params.put("timeCoarsening", String.valueOf(timeCoarsening));
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
		String val = (String) params.get("timeCoarsening");
		if (val != null) {
			try {
				timeCoarsening = Integer.parseInt(val);
			} catch (NumberFormatException e) {
			}
		}
	}

	/**
	   * Determines the distance between two trajectories
	   */
	public double distance(TrajectoryObject obj1, TrajectoryObject obj2, boolean useThreshold) {
		useThreshold = useThreshold && !Double.isNaN(distanceThreshold);
		if (obj1 == null || obj2 == null)
			return (useThreshold) ? Double.POSITIVE_INFINITY : Double.NaN;
		Vector tr1 = obj1.mobj.getTrack(), tr2 = obj2.mobj.getTrack();
		int min = Math.min(tr1.size(), tr2.size());
		double dist = 0;
		int i = 0;
		int j = 0;
		boolean computed = false;
		int unsynchro = 0;
		int syncro = 0;
		double inf = (useThreshold ? Double.POSITIVE_INFINITY : Double.NaN);

		if (!obj1.mobj.getTimeReference().isValid(obj2.mobj.getStartTime(), obj2.mobj.getEndTime()))
			return inf;

		while ((i < tr1.size()) && (j < tr2.size())) {
			//if (t1[i].z == t2[j].z)  else
			SpatialEntity sp1 = (SpatialEntity) tr1.elementAt(i);
			SpatialEntity sp2 = (SpatialEntity) tr2.elementAt(j);
//		    TimeReference rf2 = 
			TimeMoment tmf = sp2.getTimeReference().getValidFrom().getCopy();
			TimeMoment tmu = sp2.getTimeReference().getValidUntil().getCopy();
			tmf.add(-1 * timeCoarsening);
			tmu.add(timeCoarsening);

//		    System.out.println(rf2);

			if (sp1.getTimeReference().getValidUntil().compareTo(tmf) == -1) {
				//distance += geoDist(t1[i].x, t1[i].y, t2[j-1].x, t2[j-1].y);
				i++;
				unsynchro++;
				//computed = true;
			} else if (sp1.getTimeReference().getValidFrom().compareTo(tmu) == 1) {
				//distance += geoDist(t1[i-1].x, t1[i-1].y, t2[j].x, t2[j].y);
				j++;
				unsynchro++;
				//computed = true;
			} else {
				dist += distance(sp1.getGeometry(), sp2.getGeometry(), useThreshold);
				i++;
				j++;
				syncro++;
				computed = true;
			}
		}

		computed = ((computed && (syncro > (min / 2))) ? computed : false);

		return (computed ? unsynchro * dist / (syncro * min) : inf);
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
