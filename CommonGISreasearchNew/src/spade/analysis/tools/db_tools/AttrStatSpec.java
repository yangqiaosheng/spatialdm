package spade.analysis.tools.db_tools;

import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 29-Dec-2006
 * Time: 17:57:38
 * Specifies an attribute (database column) for which statistics are required
 * and the types of statistics needed, e.g. min, max, avg, sum, etc.
 */
public class AttrStatSpec {
	/**
	 * The name of the column for which statistics are required
	 */
	public String columnName = null;
	/**
	 * The index of this column
	 */
	public int columnIdx = -1;
	/**
	 * A list of the required statistics specified as strings, e.g.
	 * "min", "max", "avg", "sum", etc.
	 */
	public Vector statNames = null;
	/**
	 * A list of names for the columns to be produced for the statistics
	 */
	public Vector resultColNames = null;
}
