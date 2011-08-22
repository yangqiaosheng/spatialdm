package spade.time.vis;

import java.util.Vector;

import spade.time.TimeMoment;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 12-Mar-2007
 * Time: 17:49:01
 * Listens to simultaneous selections of subsets of items (specified by their
 * identifiers) and time intervals (specified by the start and end time moments)
 */
public interface TimeAndItemsSelectListener {
	/**
	 * Reacts to a simultaneous selection of a subset of items (specified by their
	 * identifiers) and a time interval (specified by the start and end time moments)
	 */
	public void selectionOccurred(Vector selItems, TimeMoment t1, TimeMoment t2);

	/**
	 * Cancels previously made selection(s)
	 */
	public void cancelSelection();
}
