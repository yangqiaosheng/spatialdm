package spade.time;

import spade.lib.util.Comparable;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 23-Jul-2004
 * Time: 16:47:19
 * Used for the sorting purposes: sorting is done according to time moments,
 * but the index of this time moment in the initial list can be remembered
 * and then used.
 */
public class IndexedMoment implements Comparable {
	public TimeMoment time = null;
	public int index = -1;
	public boolean isStart = true;

	public IndexedMoment(TimeMoment t, int idx, boolean isStart) {
		time = t;
		index = idx;
		this.isStart = isStart;
	};

	/**
	*  Returns 0 if equal, <0 if THIS is less than the argument, >0 otherwise
	*/
	@Override
	public int compareTo(Comparable c) {
		if (time == null)
			return (isStart) ? -1 : 1;
		if (c == null || !(c instanceof IndexedMoment))
			return -1;
		IndexedMoment imom = (IndexedMoment) c;
		if (imom.time == null)
			return (imom.isStart) ? 1 : -1;
		return time.compareTo(imom.time);
	}
}
