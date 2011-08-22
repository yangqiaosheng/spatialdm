package spade.time;

import java.util.Vector;

import spade.lib.util.BubbleSort;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Aug 6, 2009
 * Time: 5:58:56 PM
 * Utilities for dealing with time
 */
public class TimeUtil {
	/**
	 * For the given time interval finds the start and end indexes in the array
	 * of time intervals.
	 * @param t1 - "valid from" (start of the interval)
	 * @param t2 - "valid until" (end of the interval)
	 * @param br - the time breaks
	 * @param nIntervals - number of intervals; equals br.size()+1 except
	 *   for the case of using a time cycle with all its elements as breaks,
	 *   e.g. all days of a week, all hours of a day, etc.
	 * @param useCycle - whether a time cycle is used
	 * @param cycleUnit  - 's','t','h','d','m' for seconds in minute, minutes in hour,
	 *   hours in day, days in week, months in year
	 * @param nCycleElements - the number of elements in the time cycle
	 * @return  the first and last indexes
	 */
	public static int[] getIndexesOfTimeIntervals(TimeMoment t1, TimeMoment t2, Vector<TimeMoment> br, int nIntervals, boolean useCycle, char cycleUnit, int nCycleElements) {
		if (t1 == null || br == null || nIntervals < 2)
			return null;
		if (t2 == null) {
			t2 = t1;
		}
		if (useCycle) {
			int t1Idx = -1;
			Date d1 = (Date) t1;
			int n1 = d1.getElementValue(cycleUnit);
			if (cycleUnit == 'd') {
				n1 = d1.getDayOfWeek();
			}
			for (int i = 0; i < br.size() && t1Idx < 0; i++)
				if (n1 == br.elementAt(i).toNumber()) {
					t1Idx = i;
				} else if (br.size() < nCycleElements && n1 < br.elementAt(i).toNumber()) {
					t1Idx = i - 1;
					break;
				}
			if (t1Idx < 0) {
				t1Idx = nIntervals - 1;
			}
			int ind[] = { t1Idx, t1Idx };
			Date d2 = (Date) t2;
			long diff = d2.subtract(d1, cycleUnit);
			if (diff == 0)
				return ind;
			if (diff >= nCycleElements) {
				ind[0] = 0;
				ind[1] = nIntervals - 1;
				return ind;
			}
			int n2 = d2.getElementValue(cycleUnit);
			if (cycleUnit == 'd') {
				n2 = d2.getDayOfWeek();
			}
			if (n2 < n1 || t1Idx == nIntervals - 1) {
				t1Idx = 0;
			}
			int t2Idx = -1;
			for (int i = t1Idx; i < br.size() && t2Idx < 0; i++)
				if (n2 == br.elementAt(i).toNumber()) {
					t2Idx = i;
				} else if (br.size() < nCycleElements && n2 < br.elementAt(i).toNumber()) {
					t2Idx = i - 1;
					break;
				}
			if (t2Idx < 0) {
				t2Idx = nIntervals - 1;
			}
			ind[1] = t2Idx;
			return ind;
		}
		int t1Idx = -1;
		for (int i = 0; i < br.size() && t1Idx < 0; i++)
			if (t1.compareTo(br.elementAt(i)) < 0) {
				t1Idx = i;
			}
		if (t1Idx < 0) {
			t1Idx = br.size();
		}
		int t2Idx = t1Idx;
		for (int i = t1Idx; i < br.size(); i++)
			if (t2.compareTo(br.elementAt(i)) < 0) {
				break;
			} else {
				++t2Idx;
			}
		int ind[] = { t1Idx, t2Idx };
		return ind;
	}

	/**
	 * Using the given vector containing time moments associated with colors,
	 * produces a vector where colors are associated with time intervals.
	 * @param timesAndColors - contains arrays Object[2] = [TimeMoment,Color].
	 * @return vector containing arrays Object[2] = [TimeMoment,Color],
	 *   where the time moments are the starting moments of the intervals.
	 *   The intervals are chronologically sorted.
	 */
	public static Vector<Object[]> getColorsForTimeIntervals(Vector timesAndColors) {
		if (timesAndColors == null || timesAndColors.size() < 1)
			return null;
		if (!(timesAndColors.elementAt(0) instanceof Object[]))
			return null;
		Object[] pair = (Object[]) timesAndColors.elementAt(0);
		if (pair == null)
			return null;
		if (pair.length < 2)
			return null;
		if (!(pair[0] instanceof TimeMoment))
			return null;
		if (timesAndColors.size() == 1) {
			Vector result = new Vector(1, 1);
			result.addElement(pair.clone());
			return result;
		}
		BubbleSort.sort(timesAndColors);
		Vector result = new Vector(timesAndColors.size(), 1);
		pair = (Object[]) timesAndColors.elementAt(0);
		Object curr[] = { pair[0], pair[1] };
		result.addElement(curr);
		for (int i = 1; i < timesAndColors.size(); i++) {
			pair = (Object[]) timesAndColors.elementAt(i);
			boolean sameColor = (pair[1] == null && curr[1] == null) || (pair[1] != null && curr[1] != null && pair[1].equals(curr[1]));
			if (!sameColor) {
				curr = pair.clone();
				result.addElement(curr);
			}
		}
		return result;
	}
}
