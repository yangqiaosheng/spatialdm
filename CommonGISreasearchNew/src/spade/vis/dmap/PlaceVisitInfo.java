package spade.vis.dmap;

import java.util.Vector;

import spade.time.TimeMoment;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.RealPoint;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: 21-Aug-2007
 * Time: 10:02:07
 * Contains information about a visit of a place (an area object)
 * during a trip (represented by a trajectory).
 */
public class PlaceVisitInfo {
	/**
	 * The identifier of the place
	 */
	public String placeId = null;
	/**
	 * The boundary of the place
	 */
	public Geometry placeGeometry = null;
	/**
	 * The identifier of the trajectory
	 */
	public String trId = null;
	/**
	 * The trajectory itself; the elements are temporally referenced
	 * instances of SpatialEntity
	 */
	public Vector track = null;
	/**
	 * The trajectory as DMovingObject (may be null)
	 */
	public DMovingObject trObj = null;
	/**
	 * The index of the first trajectory position inside the place
	 */
	public int firstIdx = -1;
	/**
	 * The index of the last trajectory position inside the place
	 */
	public int lastIdx = -1;
	/**
	 * The moment of entering the place
	 */
	public TimeMoment enterTime = null;
	/**
	 * The moment of leaving the place
	 */
	public TimeMoment exitTime = null;
	/**
	 * Indicates whether this place is the starting place of the trajectory
	 */
	public boolean isStart = false;
	/**
	 * Indicates whether this place is the final place of the trajectory
	 */
	public boolean isFinal = false;
	/**
	 * Indicates whether this place is just crossed by a segment of the trajectory
	 * but does not really contain any point.
	 */
	public boolean justCrossed = false;
	/**
	 * The duration of staying in the place
	 */
	public long stayDuration = 0;
	/**
	 * The maximum time gap between consecutive positions of the
	 * trajectory inside the place
	 */
	public long maxTimeGap = 0;
	/**
	 * The length of the trajectory segment inside the place
	 */
	public double len = 0;
	/**
	 * The average speed inside the place (len/stayDuration)
	 */
	public double speed = 0;
	/**
	 * The angle of direction change, in degrees
	 */
	public int angleDirChange = 0;
	/**
	 * The closest point to the place centre (not necessarily belongs to the
	 * trajectory but may be a computed point)
	 */
	public RealPoint pCen = null;
	/**
	 * The minimum distance to the place centre
	 */
	public double dCen = Double.NaN;
}
