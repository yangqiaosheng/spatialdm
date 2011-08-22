package spade.analysis.tools.db_tools;

import java.util.Vector;

import spade.time.TimeMoment;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 29-Dec-2006
 * Time: 15:53:12
 * Specifies a division of data from a table according to values in a column
 * containing dates and/or times. The time is treated as a linear range,
 * from a start date to an end date.
 */
public class TimeLineDivisionSpec extends DivisionSpec {
	/**
	 * The breaks, which are instances of TimeMoment (i.e. Date or TimeCount).
	 * The breaks are sorted; the minimum (start) and the maximum (end) moments
	 * are included.
	 */
	public Vector breaks = null;

	/**
	 * Returns the number of partitions specified by this structure.
	 * Since the array <breaks> is assumed to include the start and end
	 * moments, the number of partitions is the length of the array minus one.
	 */
	@Override
	public int getPartitionCount() {
		if (breaks == null)
			return 0;
		return breaks.size() - 1;
	}

	@Override
	public String getPartitionLabel(int n) {
		return ">= " + getBreak(n).toString();
	}

	/**
	 * Returns the break value with the given index.
	 */
	public TimeMoment getBreak(int idx) {
		if (breaks == null || idx < 0 || idx >= breaks.size())
			return null;
		return (TimeMoment) breaks.elementAt(idx);
	}

	/**
	 * Returns the start and end moments for the partition (time interval) with
	 * the given index in a 2-element array of TimeMoment instances.
	 * Note that the end value is actually the beginning of the next interval,
	 * i.e. it is not included in this interval!
	 */
	public TimeMoment[] getInterval(int idx) {
		if (breaks == null || idx < 0 || idx >= breaks.size() - 1)
			return null;
		TimeMoment limits[] = new TimeMoment[2];
		limits[0] = (TimeMoment) breaks.elementAt(idx);
		limits[1] = (TimeMoment) breaks.elementAt(idx + 1);
		return limits;
	}
}
