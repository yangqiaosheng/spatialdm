package spade.analysis.tools.schedule;

import java.awt.Color;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.lib.lang.Language;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 09-Mar-2007
 * Time: 14:38:03
 * Contains data about the use of vehicle capacities by time intervals
 */
public class VehicleLoadData {
	static ResourceBundle res = Language.getTextResource("spade.analysis.tools.schedule.SchedulerTexts_tools_schedule");

	/**
	 * Categories of capacity
	 */
	public static final int capacity_idle = 0, //free capacity in unused vehicles
			capacity_free = 1, //free capacity in used vehicles
			capacity_used = 2, //used capacity in used vehicles
			capacity_overload = 3, //overload in used vehicles
			capacity_unsuitable = 4; //used capacity in unsuitable vehicles
	/**
	 * Colors to symbolize these categories
	 */
	public static final Color capacity_colors[] = { new Color(255, 255, 178), new Color(254, 204, 92), new Color(240, 59, 32), new Color(189, 0, 38), new Color(221, 28, 119) };
	public static final String capacity_labels[] = { res.getString("capacity_in_free_vehicles"), res.getString("free_capacity_in_used_vehicles"), res.getString("used_capacity_in_used_vehicles"), res.getString("overload"),
			res.getString("used_capacity_in_unsuitable_vehicles") };
	/**
	 * The list of the identifiers of the vehicles. The identifiers may occur
	 * several times!
	 */
	public Vector vehicleIds = null;
	/**
	 * The matrix containing capacity category for each vehicle and time interval.
	 * The rows of the matrix (first dimension) correspond to the vehicle
	 * identifiers, the columns (second dimension) - to time intervals.
	 */
	public int capCats[][] = null;
	/**
	 * The matrix containing capacities for each vehicle and time interval
	 * according to the category specified in capCats. Like in capCats,
	 * the rows of the matrix (first dimension) correspond to the vehicle
	 * identifiers, the columns (second dimension) - to time intervals.
	 */
	public int capSizes[][] = null;
	/**
	 * Capacity category for each vehicle before the beginning of the
	 * execution of the transportation schedule.
	 * The elements of the array correspond to the vehicle identifiers.
	 */
	public int capCatsBefore[] = null;
	/**
	 * Capacity of each vehicle for the corresponding capacity category
	 * before the beginning of the execution of the transportation schedule.
	 * The elements of the array correspond to the vehicle identifiers.
	 */
	public int capSizesBefore[] = null;
	/**
	 * Capacity category for each vehicle after the end of the
	 * execution of the transportation schedule.
	 * The elements of the array correspond to the vehicle identifiers.
	 */
	public int capCatsAfter[] = null;
	/**
	 * Capacity of each vehicle for the corresponding capacity category
	 * before the end of the execution of the transportation schedule.
	 * The elements of the array correspond to the vehicle identifiers.
	 */
	public int capSizesAfter[] = null;
}
