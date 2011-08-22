package spade.analysis.tools.schedule;

import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 14-Mar-2007
 * Time: 12:15:07
 * Contains data about suitable vehicles and their activities for a single
 * item category
 */
public class VehicleActivityData {
	/**
	 * The item category the data are valid for. Is null if the data do not refer
	 * to any category.
	 */
	public String itemCat = null;
	/**
	 * The identifiers of the vehicles suitable for this item category.
	 */
	public Vector vehicleIds = null;
	/**
	 * The matrix with the information about the activities of the vehicles
	 * in the course of the transportation schedule execution
	 */
	public int vehicleActivity[][] = null;
	/**
	 * Vehicle states or activities before the beginning of the transportation
	 * schedule execution (usually idle)
	 */
	public int statesBefore[] = null;
	/**
	 * Vehicle states or activities after the beginning of the transportation
	 * schedule execution (usually idle)
	 */
	public int statesAfter[] = null;
}
