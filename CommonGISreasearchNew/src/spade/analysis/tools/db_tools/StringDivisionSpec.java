package spade.analysis.tools.db_tools;

import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: May 27, 2010
 * Time: 4:43:05 PM
 * To change this template use File | Settings | File Templates.
 */
public class StringDivisionSpec extends DivisionSpec {

	/**
	 * The name of the column used as the base for the division
	 */
	public String columnName = null;
	/**
	 * The index of this column
	 */
	public int columnIdx = -1;
	/**
	 * Vector with distinct values of the column 
	 */
	public Vector<String> labels = null;

	/**
	 * Returns the number of partitions specified by this structure.
	 */
	@Override
	public int getPartitionCount() {
		return (labels == null) ? 0 : labels.size();
	}

	/**
	 * Returns partition label
	 */
	@Override
	public String getPartitionLabel(int n) {
		return (labels == null || n >= labels.size()) ? null : labels.elementAt(n);
	}

}
