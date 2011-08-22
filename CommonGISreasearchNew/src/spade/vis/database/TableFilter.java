package spade.vis.database;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Vector;

import spade.lib.util.Frequencies;

/**
* An AttrFilter filters objects on the basis of values of attributes associated
* with them. The attribute values are contained in an AttributeDataPortion.
* Being a descendant of ObjectFilter, AttrFilter registers listeners
* of changes and notifies them about changes of the subset of active objects.
* The AttrFilter itself listens to changes of data in the table it is attached
* to and notifies its listeners if changes of the data affect selection of
* objects. When the table is destroyed, the filter destroys itself
* and notifies its listeners about this.
*/
public class TableFilter extends ObjectFilter implements PropertyChangeListener {
	/**
	* The table to be filtered
	*/
	protected AttributeDataPortion dataTable = null;
	/**
	* The query conditions referring to individual table columns; must be
	* descendants of AttrCondition. If any other types of query conditions are
	* needed, they must be put in a separate list (an apropriate data field
	* must be added).
	*/
	protected Vector attrConditions = null;
	/**
	* Interpretation of missing values: true -> filter out,
	* false -> keep not filtered
	*/
	protected boolean missingOut = false;

	/**
	* Returns true meaning that the filter is based on attribute values
	*/
	@Override
	public boolean isAttributeFilter() {
		return true;
	}

	/**
	* Sets the table in which the filtering is to be done
	*/
	public void setDataTable(AttributeDataPortion dataTable) {
		this.dataTable = dataTable;
		if (dataTable != null) {
			setId = dataTable.getEntitySetIdentifier();
			dataTable.addPropertyChangeListener(this);
			if (dataTable instanceof ObjectContainer) {
				oCont = (ObjectContainer) dataTable;
			}
		} else {
			setId = null;
		}
		removeAllConditions();
	}

	/**
	 * Removes all conditions
	 */
	public void removeAllConditions() {
		if (attrConditions != null) {
			attrConditions.removeAllElements();
		}
	}

	/**
	* Returns the identifier of the entity set the filter is applied to.
	* This identifier is taken from the table the filter is attached to.
	*/
	@Override
	public String getEntitySetIdentifier() {
		if (dataTable == null)
			return null;
		return dataTable.getEntitySetIdentifier();
	}

	/**
	* Notifies all listeners about a change of the filter conditions.
	* No automatic notification; components that change filter conditions
	* must explicitly call this method.
	*/
	public void notifyFilterChange() {
		notifyPropertyChange("Filter", null, null);
	}

	/**
	* Reacts to changes of data in the table or table being destroyed.
	* In the first case re-filters the objects and notifies listeners if
	* anything changed. In the second case destroys itself.
	*/
	@Override
	public void propertyChange(PropertyChangeEvent e) {
		if (dataTable == null || attrConditions == null || attrConditions.size() < 1)
			return;
		if (e.getSource().equals(dataTable)) {
			if (e.getPropertyName().equals("destroyed")) {
				destroy();
				removeAllConditions();
				return;
			}
			if (e.getPropertyName().equals("structure_complete")) {
				removeAllConditions();
				filtered = false;
				return;
			}
			if (e.getPropertyName().equals("names") || e.getPropertyName().equals("new_attributes")) //does not affect the filter
				return;
			if (e.getPropertyName().equals("data_removed") || e.getPropertyName().equals("data_added") || e.getPropertyName().equals("data_updated")) {
				boolean filtered = missingOut;
				for (int i = 0; i < attrConditions.size(); i++) {
					AttrCondition acon = (AttrCondition) attrConditions.elementAt(i);
					filtered = filtered || acon.hasLimit();
					acon.adaptToDataChange();
				}
				if (filtered) {
					notifyPropertyChange("Filter", null, null);
				}
				return;
			}
			if (e.getPropertyName().equals("values")) {
				Vector v = (Vector) e.getNewValue(); // list of changed attributes
				if (v == null)
					return;
				boolean changed = false;
				for (int i = 0; i < attrConditions.size(); i++) {
					AttrCondition acon = (AttrCondition) attrConditions.elementAt(i);
					if (v.contains(acon.getAttributeId())) {
						changed = changed || acon.hasLimit();
						acon.adaptToDataChange();
					}
				}
				if (changed) {
					notifyPropertyChange("Filter", null, null);
				}
			}
		}
	}

	/**
	* Sets interpretation of missing values: true -> filter out, false -> keep
	* not filtered
	*/
	public void setFilterOutMissingValues(boolean out) {
		if (missingOut == out)
			return;
		missingOut = out;
		if (dataTable == null || attrConditions == null || attrConditions.size() < 1)
			return;
		for (int i = 0; i < attrConditions.size(); i++) {
			AttrCondition acon = (AttrCondition) attrConditions.elementAt(i);
			acon.setMissingValuesOK(!missingOut);
		}
		notifyPropertyChange("Filter", null, null);
	}

	/**
	* Adds a query condition referring to a particula column (instance of
	*  AttrCondition)
	*/
	public void addAttrCondition(AttrCondition acon) {
		if (acon == null)
			return;
		if (attrConditions == null) {
			attrConditions = new Vector(20, 10);
		}
		attrConditions.addElement(acon);
	}

	/**
	* Returns the current number of query conditions
	*/
	public int getAttrConditionCount() {
		if (attrConditions == null)
			return 0;
		return attrConditions.size();
	}

	/**
	* Returns the query condition with the given index
	*/
	public AttrCondition getAttrCondition(int idx) {
		if (idx < 0 || idx >= getAttrConditionCount())
			return null;
		return (AttrCondition) attrConditions.elementAt(idx);
	}

	/**
	* Removes the query condition with the given index
	*/
	public void removeAttrCondition(int idx) {
		if (idx < 0 || idx >= getAttrConditionCount())
			return;
		attrConditions.removeElementAt(idx);
	}

	/**
	* Finds the index of the query condition referring to the attribute (column)
	* with the given index in the table. If not found, returns -1.
	*/
	public int getConditionIndex(int attrIdx) {
		for (int i = 0; i < getAttrConditionCount(); i++) {
			AttrCondition acon = (AttrCondition) attrConditions.elementAt(i);
			if (acon.getAttributeIndex() == attrIdx)
				return i;
		}
		return -1;
	}

	/**
	* Finds the query condition referring to the attribute (column)
	* with the given index in the table. If not found, returns null.
	*/
	public AttrCondition getConditionForAttr(int attrIdx) {
		for (int i = 0; i < getAttrConditionCount(); i++) {
			AttrCondition acon = (AttrCondition) attrConditions.elementAt(i);
			if (acon.getAttributeIndex() == attrIdx)
				return acon;
		}
		return null;
	}

	/**
	* Adds an attribute (specified by its identifier) to participate in the query.
	* Returns true if a condition has been successfully created (this may be not
	* true if there is no apropriate condition implementation for the given
	* attribute type).
	* Currently, this method works only for numeric attributes.
	*/
	public boolean addQueryAttribute(String attrId) {
		if (dataTable == null || attrId == null)
			return false;
		return addQueryAttribute(dataTable.getAttrIndex(attrId));
	}

	/**
	* Adds an attribute (specified by its index in the table) to participate in
	* the query. Returns true if a condition has been successfully created
	* (this may be not true if there is no apropriate condition implementation
	* for the given attribute type).
	* Currently, this method works only for numeric attributes.
	*/
	public boolean addQueryAttribute(int attrIdx) {
		if (dataTable == null)
			return false;
		if (attrIdx < 0 || attrIdx >= dataTable.getAttrCount())
			return false;
		//check whether the attribute is numeric; otherwise, the condition cannot
		//be created
		if (!dataTable.isAttributeNumeric(attrIdx) && !dataTable.isAttributeTemporal(attrIdx))
			return false;
		if (getConditionIndex(attrIdx) >= 0)
			return true; //such condition already exists
		NumAttrCondition ncon = new NumAttrCondition();
		ncon.setTable(dataTable);
		ncon.setAttributeIndex(attrIdx);
		ncon.setMissingValuesOK(!missingOut);
		addAttrCondition(ncon);
		return true;
	}

	/**
	* Removes the attribute with the given index in the table from participation
	* in the query
	*/
	public void removeQueryAttribute(int attrIdx) {
		if (dataTable == null || attrIdx < 0 || attrIdx >= dataTable.getAttrCount() || getAttrConditionCount() < 1)
			return;
		int idx = getConditionIndex(attrIdx);
		if (idx < 0)
			return;
		boolean changed = getAttrCondition(idx).hasLimit();
		removeAttrCondition(idx);
		if (changed) {
			notifyPropertyChange("Filter", null, new Integer(attrIdx));
		}
	}

	/**
	* Removes the attribute with the given identifier from participation
	* in the query
	*/
	public void removeQueryAttribute(String attrId) {
		if (dataTable == null || attrId == null || getAttrConditionCount() < 1)
			return;
		removeQueryAttribute(dataTable.getAttrIndex(attrId));
	}

	/**
	* Returns the number of the attributes participating in the query (equivalent
	* to getAttrConditionCount());
	*/
	public int getNQueryAttributes() {
		return getAttrConditionCount();
	}

	/**
	* Returns the table index of the attribute the query condition with the given
	* index refers to.
	*/
	public int getQueryAttrIndex(int n) {
		AttrCondition acon = getAttrCondition(n);
		return acon.getAttributeIndex();
	}

	/**
	* Replies whether or not the object set is currently filtered
	*/
	@Override
	public boolean areObjectsFiltered() {
		filtered = false;
		if (dataTable == null || attrConditions == null || attrConditions.size() < 1 || dataTable.getDataItemCount() < 1)
			return false;
		if (missingOut) {
			filtered = true;
			return true;
		}
		for (int i = 0; i < attrConditions.size() && !filtered; i++) {
			AttrCondition acon = (AttrCondition) attrConditions.elementAt(i);
			filtered = acon.hasLimit();
		}
		return filtered;
	}

	/**
	* Returns the number of records satisfying the constraints on the given
	* attribute
	*/
	public int getNSatisfying(int attrIdx) {
		if (dataTable == null || dataTable.getDataItemCount() < 1)
			return 0;
		if (getAttrConditionCount() < 1 || attrIdx < 0 || attrIdx >= dataTable.getAttrCount())
			return dataTable.getDataItemCount();
		AttrCondition acon = getConditionForAttr(attrIdx);
		if (acon == null)
			return dataTable.getDataItemCount();
		return acon.getNSatisfying();
	}

	/**
	* Returns the number of records with missing values of the given
	* attribute
	*/
	public int getNMissingValues(int attrIdx) {
		if (!missingOut)
			return 0;
		if (dataTable == null || dataTable.getDataItemCount() < 1)
			return 0;
		if (getAttrConditionCount() < 1 || attrIdx < 0 || attrIdx >= dataTable.getAttrCount())
			return 0;
		AttrCondition acon = getConditionForAttr(attrIdx);
		if (acon == null)
			return 0;
		return acon.getNMissingValues();
	}

	/**
	* Returns the number of records satisfying the constraints on all the
	* attributes participating in the query
	*/
	public int getNSatisfying() {
		if (dataTable == null || dataTable.getDataItemCount() < 1)
			return 0;
		if (getAttrConditionCount() < 1)
			return dataTable.getDataItemCount();
		int num = 0;
		for (int i = 0; i < dataTable.getDataItemCount(); i++) {
			boolean ok = true;
			for (int k = 0; k < attrConditions.size() && ok; k++) {
				AttrCondition acon = (AttrCondition) attrConditions.elementAt(k);
				ok = acon.doesSatisfy(i);
			}
			if (ok) {
				++num;
			}
		}
		return num;
	}

	/**
	* Returns the number of records with missing values of at least one of the
	* attributes participating in the query
	*/
	public int getNMissingValues() {
		if (!missingOut)
			return 0;
		if (dataTable == null || dataTable.getDataItemCount() < 1)
			return 0;
		if (getAttrConditionCount() < 1)
			return 0;
		int num = 0;
		for (int i = 0; i < dataTable.getDataItemCount(); i++) {
			for (int k = 0; k < attrConditions.size(); k++) {
				AttrCondition acon = (AttrCondition) attrConditions.elementAt(k);
				if (acon.isValueMissing(i)) {
					++num;
					break;
				}
			}
		}
		return num;
	}

	/**
	* Checks whether the table record with the given number satisfies the query
	*/
	@Override
	public boolean isActive(int recN) {
		if (dataTable == null || getAttrConditionCount() < 1)
			return true;
		if (recN < 0 || dataTable.getDataItemCount() <= recN)
			return false;
		if (getAttrConditionCount() < 1)
			return true;
		if (recN < 0)
			return false;
		for (int k = 0; k < attrConditions.size(); k++) {
			AttrCondition acon = (AttrCondition) attrConditions.elementAt(k);
			if (!acon.doesSatisfy(recN))
				return false;
		}
		return true;
	}

	/**
	* Replies whether the specified object is active (i.e. not filtered out).
	*/
	@Override
	public boolean isActive(String id) {
		if (dataTable == null || getAttrConditionCount() < 1)
			return true;
		if (id == null)
			return true; //not filtered out
		int n = dataTable.indexOf(id);
		if (n < 0)
			return true; //not in the table, i.e. cannot be filtered out
		return isActive(n);
	}

	/**
	* Replies whether the specified object is active (i.e. not filtered out).
	* For an attribute filter the data item should be a ThematicDataItem
	*/
	@Override
	public boolean isActive(DataItem item) {
		if (dataTable == null || getAttrConditionCount() < 1)
			return true;
		if (item == null)
			return true;
		ThematicDataItem td = null;
		if (item instanceof ThematicDataItem) {
			td = (ThematicDataItem) item;
		} else if (item instanceof ThematicDataOwner) {
			td = ((ThematicDataOwner) item).getThematicData();
		}
		if (td == null)
			return true;

		int idx = td.getIndexInContainer();
		if (idx >= 0 && td.equals(dataTable.getDataItem(idx)))
			return isActive(idx);
		//a heuristics: when the number of records in the table is small,
		//it may be faster to find first the number of this data item in the table
		//and then lookup in the satisfaction arrays than to check values
		if (dataTable.getDataItemCount() <= 100)
			return isActive(item.getId());
		for (int k = 0; k < attrConditions.size(); k++) {
			AttrCondition acon = (AttrCondition) attrConditions.elementAt(k);
			if (!acon.doesSatisfy(td))
				return false;
		}
		return true;
	}

	/**
	 * Returns the frequencies of values in the column specified by its index
	 * taking into account current filter conditions
	 */
	public Frequencies getValueFrequencies(int colN, boolean treatValuesAsStrings) {
		if (dataTable == null || colN < 0 || colN >= dataTable.getAttrCount())
			return null;
		if (dataTable.getDataItemCount() < 1)
			return null;
		Frequencies freq = new Frequencies();
		freq.init(100, 100);
		freq.itemsAreStrings = treatValuesAsStrings;
		if (freq.getItemCount() < 1)
			return null;
		freq.trimToSize();
		for (int i = 0; i < dataTable.getDataItemCount(); i++) {
			boolean ok = true;
			if (attrConditions != null) {
				for (int k = 0; k < attrConditions.size() && ok; k++) {
					AttrCondition acon = (AttrCondition) attrConditions.elementAt(k);
					ok = acon.doesSatisfy(i);
				}
			}
			if (ok) {
				freq.incrementFrequency(dataTable.getAttrValue(colN, i));
			}
		}
		return freq;
	}

	/**
	* Attaches this filter to an ObjectContainer. This must be a table, i.e.
	* AttributeDataPortion.
	*/
	@Override
	public void setObjectContainer(ObjectContainer cont) {
		if (cont instanceof AttributeDataPortion) {
			setDataTable((AttributeDataPortion) cont);
		}
		oCont = (ObjectContainer) dataTable;
	}

	/**
	* Cancels the filter, i.e.:
	* - when missing values are not filtered out, makes all objects active
	* - when missing values are filtered out, makes active only objects
	*   without missing values of the attributes participating in the query
	*/
	@Override
	public void clearFilter() {
		if (getAttrConditionCount() < 1)
			return;
		for (int k = 0; k < attrConditions.size(); k++) {
			AttrCondition acon = (AttrCondition) attrConditions.elementAt(k);
			acon.clearLimits();
		}
	}

	/**
	* Sets the minimum limit for the attribute with the given index in the table
	*/
	public void setLowLimit(int attrIdx, double minVal) {
		if (dataTable == null || attrIdx < 0 || attrIdx >= dataTable.getAttrCount() || (!dataTable.isAttributeNumeric(attrIdx) && !dataTable.isAttributeTemporal(attrIdx)))
			return;
		NumAttrCondition acon = (NumAttrCondition) getConditionForAttr(attrIdx);
		if (acon == null) {
			if (!addQueryAttribute(attrIdx))
				return;
			acon = (NumAttrCondition) attrConditions.elementAt(attrConditions.size() - 1);
		}
		acon.setMinLimit(minVal);
	}

	/**
	* Sets the maximum limit for the attribute with the given index in the table
	*/
	public void setUpLimit(int attrIdx, double maxVal) {
		if (dataTable == null || attrIdx < 0 || attrIdx >= dataTable.getAttrCount() || (!dataTable.isAttributeNumeric(attrIdx) && !dataTable.isAttributeTemporal(attrIdx)))
			return;
		NumAttrCondition acon = (NumAttrCondition) getConditionForAttr(attrIdx);
		if (acon == null) {
			if (!addQueryAttribute(attrIdx))
				return;
			acon = (NumAttrCondition) attrConditions.elementAt(attrConditions.size() - 1);
		}
		acon.setMaxLimit(maxVal);
	}
}