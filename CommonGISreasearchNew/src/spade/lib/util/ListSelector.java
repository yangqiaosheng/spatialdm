package spade.lib.util;

import java.beans.PropertyChangeListener;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 13-Mar-2007
 * Time: 11:26:07
 * Used for the selection of items of any nature from a list. Informs about
 * the indexes of the currently selected items and notifies registered
 * listeners about selection changes. Two sorts (names) of property change
 * events are used for the notification:
 * 1) "items_selected"; the method getNewValue() returns an IntArray of
 *    the indexes of the selected items;
 * 2) "selection_cancelled", which means that no selection currently exists
 */
public interface ListSelector {
	/**
	 * Adds a listener of selection changes
	 */
	public void addSelectListener(PropertyChangeListener listener);

	/**
	 * Removes the listener of selection changes
	 */
	public void removeSelectListener(PropertyChangeListener listener);

	/**
	 * Informs whether any selection was applied to the items
	 */
	public boolean isSelectionApplied();

	/**
	 * Returns the indexes of the currently selected items
	 */
	public IntArray getSelectedIndexes();
}
