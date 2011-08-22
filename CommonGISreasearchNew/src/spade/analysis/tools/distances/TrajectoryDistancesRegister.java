package spade.analysis.tools.distances;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Jun 18, 2008
 * Time: 11:24:03 AM
 * Contains a list of available distance functions for trajectories
 */
public class TrajectoryDistancesRegister {
	/**
	 * The list of available distance functions for trajectories
	 */
	protected static final String methods[][] = { { "Route similarity", "spade.analysis.tools.distances.SpatialDistanceTrajectories_Shapes" },
			{ "Composite: starts, ends, path length, duration", "spade.analysis.tools.distances.SpatialDistanceTrajectories_Composite" },
			{ "Composite: starts/ends positions and times", "spade.analysis.tools.distances.SpatialDistanceTrajectories_EndsAndTimes" },
			{ "Starts, ends & midpoints (equal time)", "spade.analysis.tools.distances.SpatialDistanceTrajectories_EndsAndNMidpoints" },
			{ "Starts, ends & midpoints (equal distance)", "spade.analysis.tools.distances.SpatialDistanceTrajectories_EndsAndNMidpointsDist" },
			{ "Starts, ends & time steps", "spade.analysis.tools.distances.SpatialDistanceTrajectories_EndsAndTimeSteps" }, { "Starts, ends & distance steps", "spade.analysis.tools.distances.SpatialDistanceTrajectories_EndsAndDistSteps" },
			{ "Spatio-temporal synchronization", "spade.analysis.tools.distances.SpatioTemporalDistanceTrajectories" }, { "AVG Euclidean temporal based", "spade.analysis.tools.distances.SpatioTemporalDistanceTrajectoriesEuclideanAVG" },
			{ "Route similarity & dynamics", "spade.analysis.tools.distances.SpatialDistanceTrajectories_Shapes_Synchro" } };

	public static int getMethodCount() {
		return methods.length;
	}

	public static String getMethodName(int idx) {
		if (idx < 0 || idx >= methods.length)
			return null;
		return methods[idx][0];
	}

	public static String getMethodClassName(int idx) {
		if (idx < 0 || idx >= methods.length)
			return null;
		return methods[idx][1];
	}

	public static DistanceComputer getMethodInstance(int idx) {
		String clName = getMethodClassName(idx);
		if (clName == null)
			return null;
		try {
			Object method = Class.forName(clName).newInstance();
			if (method == null)
				return null;
			if (!(method instanceof DistanceComputer))
				return null;
			DistanceComputer distComp = (DistanceComputer) method;
			distComp.setMethodName(getMethodName(idx));
			return distComp;
		} catch (Exception e) {
		}
		return null;
	}
}
