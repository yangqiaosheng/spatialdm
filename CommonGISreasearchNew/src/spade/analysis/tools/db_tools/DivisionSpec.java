package spade.analysis.tools.db_tools;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 29-Dec-2006
 * Time: 15:12:32
 * Specifies a division of data from a table according to values in some
 * table column. This is a parent class for classes suited to different
 * column types.
 */
public abstract class DivisionSpec {
	/**
	 * The name of the column used as the base for the division
	 */
	public String columnName = null;
	/**
	 * The index of this column
	 */
	public int columnIdx = -1;

	/**
	 * Returns the number of partitions specified by this structure.
	 */
	public abstract int getPartitionCount();

	/**
	 * Returns partition label
	 */
	public abstract String getPartitionLabel(int n);

}
