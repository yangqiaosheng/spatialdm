package spade.time.vis;

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: Apr 21, 2008
 * Time: 3:20:17 PM
 * To change this template use File | Settings | File Templates.
 */
public class TimeLineInteractionObject extends TimeLineObject {
	/**
	 * for the interaction that involves N TimeLineObject objects
	 * N TimeLineInteractionObject objects are created
	 * Fields <tloPID> and <tloPIdx> of the parent describe the primary TimeLineObject,
	 * i.e. which is used as background for drawing this TimeLineInteractionObject
	 */
	public String tloPID = null;
	//public int tloPIdx=-1;
	/*
	 * The identifiers and indices of the (N-1) remaining TimeLineObject objects
	 * that represent timelines involved in the interaction
	 */
	public String tloIDs[] = null;
	//public int tloIdxs[]=null;

}
