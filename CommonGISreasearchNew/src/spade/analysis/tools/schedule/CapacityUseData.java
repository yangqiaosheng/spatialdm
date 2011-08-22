package spade.analysis.tools.schedule;

import java.awt.Color;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.lib.lang.Language;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 16-Mar-2007
 * Time: 17:58:41
 */
public class CapacityUseData {
	static ResourceBundle res = Language.getTextResource("spade.analysis.tools.schedule.SchedulerTexts_tools_schedule");

	/**
	 * The possible states of the destination sites
	 */
	public static final int capacity_fully_unused = 0, capacity_partly_unused = 1, capacity_partly_filled = 2, capacity_full = 3, capacity_overloaded = 4, capacity_unsuitable = 5;
	/**
	 * The names of these states
	 */
	public static final String state_names[] = { res.getString("Unused"), res.getString("Partly_unused"), res.getString("Partly_filled"), res.getString("Full"), res.getString("Overloaded"), res.getString("Unsuitable") };
	/**
	 * Colors to symbolize these states
	 */
	public static final Color state_colors[] = { new Color(255, 255, 178), new Color(254, 204, 92), new Color(253, 141, 60), new Color(240, 59, 32), new Color(189, 0, 38), new Color(122, 1, 119) };
	/**
	 * The list of the identifiers of the objects posessing some capacities
	 */
	public Vector ids = null;
	/**
	 * Ordered list of the time moments. The elements are instances of TimeMoment
	 * and indicate the beginnings of the time intervals. The last element in the
	 * list is the end of the last time interval.
	 */
	public Vector times = null;
	/**
	 * A matrix of capacity states (overloaded, full, partly filled,
	 * or unused) by time (matrix columns) for all objects (matrix rows).
	 * The number of columns is the number of time moments minus 1.
	 */
	public int states[][] = null;
	/**
	 * A matrix of capacity filling (number of items filled) by time (matrix
	 * columns) for all objects (matrix rows).
	 * The number of columns is the number of time moments minus 1.
	 */
	public int fills[][] = null;
	/**
	 * Capacity states (overloaded, full, partly filled, or unused) before
	 * the beginning of the transportation schedule execution.
	 */
	public int statesBefore[] = null;
	/**
	 * Capacity states (overloaded, full, partly filled, or unused) after
	 * the end of the transportation schedule execution.
	 */
	public int statesAfter[] = null;
	/**
	 * Capacity filling (number of items filled) before
	 * the beginning of the transportation schedule execution.
	 */
	public int fillsBefore[] = null;
	/**
	 * Capacity filling (number of items filled) after
	 * the end of the transportation schedule execution.
	 */
	public int fillsAfter[] = null;
}
