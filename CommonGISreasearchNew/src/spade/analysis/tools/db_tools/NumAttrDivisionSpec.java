package spade.analysis.tools.db_tools;

import spade.lib.util.FloatArray;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 29-Dec-2006
 * Time: 15:20:04
 * Specifies a division of data from a table according to values of a numeric
 * attribute.
 */
public class NumAttrDivisionSpec extends DivisionSpec {
	/**
	 * The breaks that define the division, including the minimum and the
	 * maximum value. The breaks are assumed to be always sorted in accending
	 * order.
	 */
	public FloatArray breaks = null;

	/**
	 * Returns the number of partitions specified by this structure.
	 * Since the array <breaks> is assumed to include the minimum and maximum
	 * values, the number of partitions is the length of the array minus one.
	 */
	@Override
	public int getPartitionCount() {
		if (breaks == null)
			return 0;
		return breaks.size() - 1;
	}

	@Override
	public String getPartitionLabel(int n) {
		return "" + n + ": >=" + getBreak(n);
	}

	/**
	 * Returns the break value with the given index.
	 */
	public float getBreak(int idx) {
		if (breaks == null || idx < 0 || idx >= breaks.size())
			return Float.NaN;
		return breaks.elementAt(idx);
	}

	/**
	 * Returns the minimum and maximum values for the partition (interval) with
	 * the given index in a 2-element array of floats.
	 * Note that the maximum value is actually the beginning of the next interval,
	 * i.e. it is not included in this interval!
	 */
	public float[] getInterval(int idx) {
		if (breaks == null || idx < 0 || idx >= breaks.size() - 1)
			return null;
		float limits[] = new float[2];
		limits[0] = breaks.elementAt(idx);
		limits[1] = breaks.elementAt(idx + 1);
		return limits;
	}
}
