package spade.time.manage;

import java.beans.PropertyChangeListener;

import spade.time.TimeReference;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.ObjectContainer;
import spade.vis.database.TimeFilter;

/**
* The interface for the component that registers and manages time-referenced data.
*/
public interface TemporalDataManager {
	/**
	* Registers a container of time-referenced data
	*/
	public void addTemporalDataContainer(ObjectContainer oCont);

	/**
	* Returns the number of currently registered containers with time-referenced
	* data
	*/
	public int getContainerCount();

	/**
	* Returns the container of time-referenced data with the given index
	*/
	public ObjectContainer getContainer(int idx);

	/**
	 * Returns the minimum and maximum times found in the time-referenced data
	 */
	public TimeReference getMinMaxTimes();

	/**
	* Returns the time filter of the container with the given index
	*/
	public TimeFilter getTimeFilter(int idx);

	/**
	* Registers a table with temporal parameters
	*/
	public void addTemporalTable(AttributeDataPortion table);

	/**
	* Returns the number of currently registered tables with time-referenced
	* data
	*/
	public int getTemporalTableCount();

	/**
	* Returns the time-referenced data table with the given index
	*/
	public AttributeDataPortion getTemporalTable(int idx);

	/**
	* Registers a listener of data changes (e.g. some container may be destroyed
	* or updated)
	*/
	public void addPropertyChangeListener(PropertyChangeListener list);

	/**
	* Removes the listener of data changes
	*/
	public void removePropertyChangeListener(PropertyChangeListener list);

	/**
	 * Destroys and removes all time filters
	 */
	public void destroyTimeFilters();
}