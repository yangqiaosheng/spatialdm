package spade.analysis.tools.db_tools;

import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 29-Dec-2006
 * Time: 15:34:26
 * Specifies a division of data from a table according to values in a column
 * containing dates and/or times. The time is treated as a cycle, e.g. hours in
 * a day, days in a week, or months in a year. The structure specifies the
 * relevant time component and the division of the cycle.
 */
public class TimeCycleDivisionSpec extends DivisionSpec {
	/**
	 * The code of the relevant time component:
	 * "MM" - month of a year; "D" - day of a week; "HH24" - hour of a day;
	 * "MI" - minute of a hour; "SS" - second of a minute
	 */
	public String cycleCode = null;
	/**
	 * The partitions of the cycle, which are specified as arrays of integers.
	 * It should not be assumed that the values in the arrays are necessarily
	 * sorted. For example, a valid partition for months of a year may be [12,1,2],
	 * or for hours in a day [22,23,0,1,2,3,4,5].
	 * It should also not be assumed that the arrays are somehow ordered
	 * within the vector.
	 */
	public Vector partitions = null;

	/**
	 * Returns the number of partitions specified by this structure.
	 */
	@Override
	public int getPartitionCount() {
		if (partitions == null)
			return 0;
		return partitions.size();
	}

	@Override
	public String getPartitionLabel(int n) {
		if (partitions == null || n >= partitions.size())
			return null;
		int p[] = getPartition(n);
		return (p[0] == p[p.length - 1]) ? "" + p[0] : p[0] + ".." + p[p.length - 1];
	}

	/**
	 * Returns the partition with the given index. This is an array of integer
	 * values of the corresponding time component. It should not be assumed that
	 * the values are necessarily sorted. For example, a valid partition
	 * for months of a year may be [12,1,2],
	 * or for hours in a day [22,23,0,1,2,3,4,5]
	 */
	public int[] getPartition(int idx) {
		if (partitions == null || idx < 0 || idx >= partitions.size())
			return null;
		return (int[]) partitions.elementAt(idx);
	}
}
