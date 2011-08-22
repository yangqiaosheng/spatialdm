package spade.vis.database;

import java.beans.PropertyChangeEvent;
import java.util.Vector;

/**
* A Combined Filter is an Object Filter that may consist of several filters,
* for example, attribute, geographic, and temporal filter. The filters are
* combined through logical "AND" (in the future it will be extended to other
* logical operations).
* A Combined Filter listens to property change events of all the individual
* filters included in it and propagates these events to its listeners.
*/

public class CombinedFilter extends ObjectFilter {
	/**
	* The filters being combined
	*/
	protected Vector filters = null;
	/**
	 * For each object of the container, contains true if active and false otherwise
	 */
	protected boolean objIsActive[] = null;
	/**
	 * Whether the values in objIsActive are valid
	 */
	protected boolean objIsActiveValid = false;

	/**
	* Sets the identifier of the entity set the filter is applied to
	*/
	@Override
	public void setEntitySetIdentifier(String identifier) {
		super.setEntitySetIdentifier(identifier);
		if (filters != null) {
			for (int i = 0; i < filters.size(); i++) {
				((ObjectFilter) filters.elementAt(i)).setEntitySetIdentifier(identifier);
			}
		}
	}

	/**
	* Adds a new filter to the combination of current filters
	*/
	public void addFilter(ObjectFilter ofilter) {
		if (ofilter == null)
			return;
		if (filters == null) {
			filters = new Vector(5, 5);
			setEntitySetIdentifier(ofilter.getEntitySetIdentifier());
		}
		if (ofilter instanceof CombinedFilter) {
			CombinedFilter cFilter = (CombinedFilter) ofilter;
			for (int i = 0; i < cFilter.getFilterCount(); i++) {
				addFilter(cFilter.getFilter(i));
			}
		} else if (!filters.contains(ofilter)) {
			ofilter.addPropertyChangeListener(this);
			filters.addElement(ofilter);
		}
		objIsActiveValid = false;
	}

	/**
	* Returns the number of individual filters in the combined filter
	*/
	public int getFilterCount() {
		if (filters == null)
			return 0;
		return filters.size();
	}

	/**
	* Returns the filter with the given index
	*/
	public ObjectFilter getFilter(int idx) {
		if (idx < 0 || idx >= getFilterCount())
			return null;
		return (ObjectFilter) filters.elementAt(idx);
	}

	/**
	* Checks if the specified filter is included in this combined filter
	*/
	public boolean hasFilter(ObjectFilter ofilter) {
		if (filters != null && ofilter != null)
			return filters.contains(ofilter);
		return false;
	}

	/**
	* Removes the filter with the given index
	*/
	public void removeFilter(int idx) {
		if (idx >= 0 && idx < getFilterCount()) {
			((ObjectFilter) filters.elementAt(idx)).removePropertyChangeListener(this);
			filters.removeElementAt(idx);
			objIsActiveValid = false;
		}
	}

	/**
	* Removes the specified filter
	*/
	public void removeFilter(ObjectFilter ofilter) {
		if (filters != null && ofilter != null) {
			ofilter.removePropertyChangeListener(this);
			filters.removeElement(ofilter);
			objIsActiveValid = false;
		}
	}

	/**
	* Removes all the filters included in the combination
	*/
	public void removeAllFilters() {
		if (filters != null) {
			for (int i = 0; i < filters.size(); i++) {
				((ObjectFilter) filters.elementAt(i)).removePropertyChangeListener(this);
			}
			filters.removeAllElements();
		}
		objIsActiveValid = false;
	}

	/**
	* Attaches this filter to an ObjectContainer. If some of the subfilters are
	* already attached to some containers, does not change this.
	*/
	@Override
	public void setObjectContainer(ObjectContainer cont) {
		super.setObjectContainer(cont);
		if (oCont != null && filters != null) {
			for (int i = 0; i < getFilterCount(); i++)
				if (getFilter(i).getObjectContainer() == null) {
					getFilter(i).setObjectContainer(oCont);
				}
		}
	}

	/**
	 * Fills the array objIsActive with the actual values showing the statuses of the objects
	 */
	public void applyFilters() {
		if (objIsActiveValid && objIsActive != null)
			return;
		if (oCont == null || oCont.getObjectCount() < 1)
			return;
		if (objIsActive == null || objIsActive.length != oCont.getObjectCount()) {
			objIsActive = new boolean[oCont.getObjectCount()];
		}
		filtered = false;
		for (int j = 0; j < objIsActive.length; j++) {
			objIsActive[j] = true;
			for (int i = 0; i < getFilterCount() && objIsActive[j]; i++) {
				ObjectFilter flt = (ObjectFilter) filters.elementAt(i);
				if (oCont.equals(flt.getObjectContainer())) {
					objIsActive[j] = flt.isActive(j);
				} else {
					objIsActive[j] = flt.isActive(oCont.getObjectData(j));
				}
			}
			filtered = filtered || !objIsActive[j];
		}
		objIsActiveValid = true;
	}

	/**
	* Checks whether the data item with the given index in the container is active,
	* i.e. not filtered out.
	*/
	@Override
	public boolean isActive(int idx) {
		if (!filtered || oCont == null)
			return true;
		if (objIsActive != null && objIsActiveValid)
			return objIsActive[idx];
		applyFilters();
		if (objIsActive != null && objIsActiveValid)
			return objIsActive[idx];
		boolean active = true;
		for (int i = 0; i < getFilterCount() && active; i++) {
			ObjectFilter flt = (ObjectFilter) filters.elementAt(i);
			if (oCont.equals(flt.getObjectContainer())) {
				active = flt.isActive(idx);
			} else {
				active = flt.isActive(oCont.getObjectData(idx));
			}
		}
		return active;
	}

	/**
	* Replies whether the specified object is active (i.e. not filtered out)
	*/
	@Override
	public boolean isActive(String id) {
		if (!filtered)
			return true;
		boolean active = true;
		for (int i = 0; i < getFilterCount() && active; i++) {
			active = ((ObjectFilter) filters.elementAt(i)).isActive(id);
		}
		return active;
	}

	/**
	* Replies whether the specified object is active (i.e. not filtered out).
	* For an attribute filter the data item should be a ThematicDataItem
	*/
	@Override
	public boolean isActive(DataItem item) {
		if (!filtered)
			return true;
		if (item == null)
			return false;
		boolean active = true;
		for (int i = 0; i < getFilterCount() && active; i++) {
			ObjectFilter oFilter = (ObjectFilter) filters.elementAt(i);
			active = oFilter.isActive(item);
		}
		return active;
	}

	/**
	* Clears all the filters, i.e. makes all objects active
	*/
	@Override
	public void clearFilter() {
		if (!areObjectsFiltered())
			return;
		for (int i = 0; i < getFilterCount(); i++) {
			((ObjectFilter) filters.elementAt(i)).clearFilter();
		}
		if (objIsActive != null) {
			for (int i = 0; i < objIsActive.length; i++) {
				objIsActive[i] = true;
			}
		}
		objIsActiveValid = true;
		filtered = false;
	}

	/**
	* Replies whether or not the object set is currently filtered
	*/
	@Override
	public boolean areObjectsFiltered() {
		if (getFilterCount() < 1)
			return false;
		if (!objIsActiveValid) {
			applyFilters();
		}
		if (objIsActiveValid)
			return filtered;
		filtered = false;
		for (int i = 0; i < getFilterCount() && !filtered; i++) {
			filtered = ((ObjectFilter) filters.elementAt(i)).areObjectsFiltered();
		}
		return filtered;
	}

	/**
	* Replies whether the filter is based on attribute values
	*/
	@Override
	public boolean isAttributeFilter() {
		if (getFilterCount() < 1)
			return false;
		for (int i = 0; i < getFilterCount(); i++)
			if (((ObjectFilter) filters.elementAt(i)).isAttributeFilter())
				return true;
		return false;
	}

	/**
	* Upon receiving a property change event from any of its component filters,
	* propagate this event to all its listeners. When one of the filters
	* is destroyed, removes it. If no more filters remained, destroys itself.
	*/
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if (evt.getSource() instanceof ObjectFilter)
			if (evt.getPropertyName().equals("destroyed")) {
				int idx = filters.indexOf(evt.getSource());
				if (idx >= 0) {
					((ObjectFilter) filters.elementAt(idx)).removePropertyChangeListener(this);
					filters.removeElementAt(idx);
					if (filters.size() < 1) {
						destroy();
					} else {
						objIsActiveValid = false;
						notifyPropertyChange("Filter", null, null);
					}
				}
			} else {
				objIsActiveValid = false;
				areObjectsFiltered();
				notifyPropertyChange(evt.getPropertyName(), evt.getOldValue(), evt.getNewValue());
			}
	}
}