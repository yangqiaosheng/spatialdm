package spade.vis.dmap;

import java.awt.Color;

import spade.time.TimeMoment;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Apr 16, 2008
 * Time: 11:22:16 AM
 * Describes a DGeoObject which is a member of some aggregate (DAggregateObject).
 */
public class AggregateMemberObject {
	/**
	 * The object - member of the aggregate
	 */
	public DGeoObject obj = null;
	/**
	 * The time of entering the aggregate
	 */
	public TimeMoment enterTime = null;
	/**
	 * The time of exiting the aggregate
	 */
	public TimeMoment exitTime = null;
	/**
	 * If only a part of the object is a member of the aggregate (e.g. a fragment
	 * of a trajectory), this is an instance of DGeoObject representing this part.
	 * It is obtained using the method getObjectCopyForTimeInterval(t1,t2).
	 * This element may be null.
	 */
	public DGeoObject validPart = null;
	/**
	 * Indicates if the object is active, i.e. not filtered out
	 */
	public boolean active = true;
	/**
	 * The color used to represent this aggregate member
	 */
	public Color color = null;
}
