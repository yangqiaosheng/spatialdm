package spade.time.manage;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Vector;

import spade.time.TimeMoment;
import spade.time.TimeReference;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.ObjectContainer;
import spade.vis.database.TimeFilter;

/**
* Registers, manages, and provides access to time-referenced data.
*/
public class TimeManager implements TemporalDataManager, PropertyChangeListener {
	/**
	* The containers of time-referenced data currently available in the system
	*/
	protected Vector containers = null;
	/**
	* The time filters associated with the containers
	*/
	protected Vector timeFilters = null;
	/**
	* The tables with temporal parameters currently available in the system.
	*/
	protected Vector timeTables = null;
	/**
	* Used for handling the list of property change listeners and notifying them
	* about changes of the data in the time-referenced data containers.
	*/
	protected PropertyChangeSupport pcSupport = null;

	/**
	* Registers a container of time-referenced data
	*/
	@Override
	public void addTemporalDataContainer(ObjectContainer oCont) {
		if (oCont == null)
			return;
		if (containers == null) {
			containers = new Vector(10, 5);
		}
		if (containers.contains(oCont))
			return;
		containers.addElement(oCont);
		oCont.addPropertyChangeListener(this);
		if (timeFilters == null) {
			timeFilters = new Vector(10, 5);
		}
		timeFilters.addElement(null);
		if (containers.size() == 1) {
			notifyPropertyChange("data_appeared", null, oCont);
		} else {
			notifyPropertyChange("data_added", null, oCont);
		}
	}

	/**
	* Returns the number of currently registered containers with time-referenced
	* data
	*/
	@Override
	public int getContainerCount() {
		if (containers == null)
			return 0;
		return containers.size();
	}

	/**
	* Returns the container of time-referenced data with the given index
	*/
	@Override
	public ObjectContainer getContainer(int idx) {
		if (idx < 0 || idx >= getContainerCount())
			return null;
		return (ObjectContainer) containers.elementAt(idx);
	}

	/**
	 * Returns the minimum and maximum times found in the time-referenced data
	 */
	@Override
	public TimeReference getMinMaxTimes() {
		if (containers == null || containers.size() < 1)
			return null;
		TimeMoment first = null, last = null;
		for (int i = 0; i < containers.size(); i++) {
			TimeFilter filter = getTimeFilter(i);
			TimeMoment t1 = filter.getEarliestMoment(), t2 = filter.getLatestMoment();
			if (t1 != null && t2 != null) {
				if (first == null || first.compareTo(t1) > 0) {
					first = t1;
				}
				if (last == null || last.compareTo(t2) < 0) {
					last = t2;
				}
			}
		}
		if (first == null || last == null)
			return null;
		TimeReference tr = new TimeReference();
		tr.setValidFrom(first);
		tr.setValidUntil(last);
		return tr;
	}

	/**
	* Returns the time filter of the container with the given index
	*/
	@Override
	public TimeFilter getTimeFilter(int idx) {
		if (idx < 0 || idx >= getContainerCount())
			return null;
		if (timeFilters.elementAt(idx) != null)
			return (TimeFilter) timeFilters.elementAt(idx);
		ObjectContainer oCont = getContainer(idx);
		TimeFilter filter = new TimeFilter();
		filter.setObjectContainer(oCont);
		filter.setEntitySetIdentifier(oCont.getEntitySetIdentifier());
		oCont.setObjectFilter(filter);
		timeFilters.setElementAt(filter, idx);
		return filter;
	}

	/**
	* Registers a table with temporal parameters
	*/
	@Override
	public void addTemporalTable(AttributeDataPortion table) {
		if (table == null || !table.hasTemporalParameter())
			return;
		if (timeTables == null) {
			timeTables = new Vector(10, 5);
		}
		if (timeTables.contains(table))
			return;
		timeTables.addElement(table);
		table.addPropertyChangeListener(this);
		if (timeTables.size() == 1) {
			notifyPropertyChange("data_appeared", null, table);
		} else {
			notifyPropertyChange("data_added", null, table);
		}
	}

	/**
	* Returns the number of currently registered tables with time-referenced
	* data
	*/
	@Override
	public int getTemporalTableCount() {
		if (timeTables == null)
			return 0;
		return timeTables.size();
	}

	/**
	* Returns the time-referenced data table with the given index
	*/
	@Override
	public AttributeDataPortion getTemporalTable(int idx) {
		if (idx < 0 || idx >= getTemporalTableCount())
			return null;
		return (AttributeDataPortion) timeTables.elementAt(idx);
	}

	/**
	* Registers a listener of data changes (e.g. some container may be destroyed
	* or updated)
	*/
	@Override
	public synchronized void addPropertyChangeListener(PropertyChangeListener l) {
		if (l == null)
			return;
		if (pcSupport == null) {
			pcSupport = new PropertyChangeSupport(this);
		}
		pcSupport.addPropertyChangeListener(l);
	}

	/**
	* Removes the listener of data changes
	*/
	@Override
	public synchronized void removePropertyChangeListener(PropertyChangeListener l) {
		if (l == null || pcSupport == null)
			return;
		pcSupport.removePropertyChangeListener(l);
	}

	protected void notifyPropertyChange(String propName, Object oldValue, Object newValue) {
		if (pcSupport == null)
			return;
		pcSupport.firePropertyChange(propName, oldValue, newValue);
	}

	/**
	* Reacts to changes in its registered containers: destroying, updating, etc.
	*/
	@Override
	public void propertyChange(PropertyChangeEvent e) {
		if (e.getSource() instanceof AttributeDataPortion) { //temporal table
			if (timeTables == null)
				return;
			AttributeDataPortion table = (AttributeDataPortion) e.getSource();
			int idx = timeTables.indexOf(table);
			if (idx < 0)
				return;
			if (e.getPropertyName().equals("destroyed") || e.getPropertyName().equals("structure_complete") || (e.getPropertyName().equals("update") && !table.hasTemporalParameter())) {
				table.removePropertyChangeListener(this);
				timeTables.removeElementAt(idx);
				if (timeTables.size() > 0 || getContainerCount() > 0) {
					notifyPropertyChange("table_removed", null, table);
				} else {
					notifyPropertyChange("all_data_removed", null, table);
				}
			} else if (e.getPropertyName().equals("update")) {
				notifyPropertyChange("table_updated", null, table);
			}
		} else if (e.getSource() instanceof ObjectContainer) {
			if (containers == null)
				return;
			ObjectContainer oCont = (ObjectContainer) e.getSource();
			int idx = containers.indexOf(oCont);
			if (idx < 0)
				return;
			if (e.getPropertyName().equals("destroyed") || (e.getPropertyName().equals("update") && !oCont.hasTimeReferences())) {
				oCont.removePropertyChangeListener(this);
				TimeFilter filter = (TimeFilter) timeFilters.elementAt(idx);
				oCont.removeObjectFilter(filter);
				containers.removeElementAt(idx);
				timeFilters.removeElementAt(idx);
				if (containers.size() > 0 || getTemporalTableCount() > 0) {
					notifyPropertyChange("container_removed", null, oCont);
				} else {
					notifyPropertyChange("all_data_removed", null, oCont);
				}
			} else if (e.getPropertyName().equals("update")) {
				notifyPropertyChange("container_updated", null, oCont);
			}
		}
	}

	/**
	 * Destroys and removes all time filters
	 */
	@Override
	public void destroyTimeFilters() {
		if (containers == null || timeFilters == null)
			return;
		for (int i = 0; i < timeFilters.size(); i++)
			if (timeFilters.elementAt(i) != null) {
				TimeFilter tfilt = (TimeFilter) timeFilters.elementAt(i);
				ObjectContainer oCont = (ObjectContainer) containers.elementAt(i);
				oCont.removeObjectFilter(tfilt);
				timeFilters.setElementAt(null, i);
			}
	}
}