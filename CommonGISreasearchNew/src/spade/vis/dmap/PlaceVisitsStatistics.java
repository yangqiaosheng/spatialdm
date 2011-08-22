package spade.vis.dmap;

import spade.time.TimeMoment;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: 21-Aug-2007
 * Time: 11:17:45
 * Contains statistics about visits of a place.
 */
public class PlaceVisitsStatistics {
	/**
	 * The identifier of the place
	 */
	public String placeId = null;
	/**
	 * The number of visits of the place
	 */
	public int nVisits = 0;
	/**
	 * The number of different trajectories visiting this place
	 */
	public int nTrajectories = 0;
	/**
	 * The maximum number of repeated visits by the same trajectory
	 */
	public int maxNRepeatedVisits = 0;
	/**
	 * The number of trajectories starting in this place
	 */
	public int nStarts = 0;
	/**
	 * The number of trajectories ending in this place
	 */
	public int nEnds = 0;
	/**
	 * The earliest time of entering the place
	 */
	public TimeMoment firstEnterTime = null;
	/**
	 * The latest moment of leaving the place
	 */
	public TimeMoment lastExitTime = null;
	/**
	 * The minimum, maximum, average and median duration of staying in the place
	 */
	public long minStayDuration = 0, maxStayDuration = 0, averStayDuration = 0, medianStayDuration = 0;
	/**
	 * The sum of durations
	 */
	public long totalStayDuration = 0;
	/**
	 * The minimum, maximum, average and median time gaps between consecutive positions
	 * (computed from the maximum time gaps for the individual visits)
	 */
	public long minTimeGap = 0, maxTimeGap = 0, averTimeGap = 0, medianTimeGap = 0;
	/**
	 * The minimum, maximum, average and median lengths of the paths inside the place
	 */
	public double minLen = 0, maxLen = 0, averLen = 0, medianLen = 0;
	/**
	 * The sum of lengths
	 */
	public double totalLenInside = 0;
	/**
	 * The minimum, maximum, average and median speeds inside the place
	 */
	public double minSpeed = 0, maxSpeed = 0, averSpeed = 0, medianSpeed = 0;
	/**
	 * The minimum, maximum and median angle of direction change, in degrees
	 */
	public int minAngleDirChange = 0, maxAngleDirChange = 0, medianAngleDirChange = 0;
}
