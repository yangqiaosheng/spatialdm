package spade.analysis.tools.moves;

import spade.time.TimeMoment;
import spade.vis.geometry.RealPoint;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Apr 9, 2008
 * Time: 11:46:22 AM
 */
public class TrajectoryFragment {
	/**
	 * The identifier of the trajectory
	 */
	public String trId = null;
	/**
	 * The identifier of the moving entity
	 */
	public String entityId = null;
	/**
	 * The index of the trajectory in the layer (or other container)
	 */
	public int trIdx = -1;
	/**
	 * Indexes of the first and last trajectory points belonging to this fragment
	 */
	public int idx1 = -1, idx2 = -1;
	/**
	 * The start and end time of the trajectory fragment
	 */
	public TimeMoment t1 = null, t2 = null;
	/**
	 * The positions of the trajectory stored as instances of RealPoint
	 */
	public RealPoint points[] = null;
	/**
	 * The bounding rectangle of the trajectory fragment
	 */
	public float x1 = Float.NaN, x2 = x1, y1 = x1, y2 = x1;
	/**
	 * Reference to the next fragment
	 */
	public TrajectoryFragment next = null;

	@Override
	public String toString() {
		return trId + ": " + t1 + ".." + t2;
	}
}
