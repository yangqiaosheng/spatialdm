package spade.analysis.tools.db_tools.statistics;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 24-Jan-2007
 * Time: 12:44:12
 * Specifies information to be shown in one row of a table statistics display
 * (see TableStatisticsDisplay)
 */
public class TableStatisticsItem {
	/**
	 * Name of the item, e.g. "Table name", "N records", "x-coordinate" etc.
	 */
	public String name = null;
	/**
	 * Optional comment
	 */
	public String comment = null;
	/**
	 * Value or minimum value of the item specified as a string
	 */
	public String value = null;
	/**
	 * Maximum value of the item specified as a string
	 */
	public String maxValue = null;
}
