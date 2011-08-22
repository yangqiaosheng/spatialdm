package spade.vis.database;

import java.beans.PropertyChangeEvent;
import java.util.Vector;

import spade.lib.util.IntArray;
import spade.lib.util.StringUtil;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Oct 22, 2010
 * Time: 12:21:57 PM
 * Applies filtering from another object set to the set this filter belongs to.
 * That another object set (second object set) must have a table with a column
 * containing identifiers of objects from the other dataset.
 */
public class OtherObjectFilterApplicator extends ObjectFilter {
	/**
	 * The possible directions of the filtering
	 */
	public static final int NoFilter = 0, Filter2ToSet1 = 1, Filter1ToSet2 = -1;
	/**
	 * The other object set (second object set) from which filtering is applied
	 * to the object set owning this filter
	 */
	protected ObjectContainer oCont2 = null;
	/**
	 * The table of the second dataset having a column (attribute)
	 * containing identifiers of the objects from the second dataset.
	 */
	protected AttributeDataPortion table2 = null;
	/**
	 * The index of the table column containing identifiers of the objects
	 * from the second dataset
	 */
	protected int colN = -1;
	/**
	 * Dependencies between objects of the two sets:
	 * for each object of the first set contains an array of object indexes
	 * in the second set that have references to this object
	 */
	protected int[][] dependencies = null;
	/**
	 * Current direction of the filtering, one of the constants
	 * NoFilter=0, Filter2ToSet1=1, Filter1ToSet2=-1
	 */
	protected int filterDirection = NoFilter;
	/**
	 * The filter that is currently listened, depending on the filtering direction.
	 * Filter2ToSet1 : filter of the second set
	 * Filter1ToSet2 : filter of the first set
	 */
	protected ObjectFilter filterToListen = null;

	/**
	 * The other object set (second object set) from which filtering is applied
	 * to the object set owning this filter
	 */
	public ObjectContainer getSecondObjectContainer() {
		return oCont2;
	}

	/**
	 * Seta a reference to the other object set (second object set)
	 * from which filtering is applied to the object set owning this filter
	 */
	public void setSecondObjectContainer(ObjectContainer oCont2) {
		this.oCont2 = oCont2;
	}

	/**
	 * Sets a reference to the table belonging to the second object set
	 * and the number of the table column containing the identifiers
	 * of the objects of the first object set
	 */
	public void setTableAndColumnWithObjectIds(AttributeDataPortion table2, int colN) {
		this.table2 = table2;
		this.colN = colN;
		getDependencies();
	}

	/**
	 * Checks if the dependencies necessary for filtering is available
	 */
	public boolean hasDependencies() {
		return dependencies != null;
	}

	/**
	 * Retrieves the dependencies between objects of the two sets:
	 * for each object of the first set contains an array of object indexes
	 * in the second set that have references to this object
	 */
	protected void getDependencies() {
		dependencies = null;
		if (oCont == null || oCont.getObjectCount() < 1)
			return;
		if (table2 == null || colN < 0 || !table2.hasData())
			return;
		IntArray depIAr[] = new IntArray[oCont.getObjectCount()];
		boolean hasDependency = false;
		for (int i = 0; i < table2.getDataItemCount(); i++) {
			String values = table2.getAttrValueAsString(colN, i);
			if (values == null) {
				continue;
			}
			Vector ids = StringUtil.getNames(values, ";");
			if (ids == null || ids.size() < 1) {
				continue;
			}
			for (int j = 0; j < ids.size(); j++) {
				int oidx = oCont.getObjectIndex((String) ids.elementAt(j));
				if (oidx >= 0) {
					if (depIAr[oidx] == null) {
						depIAr[oidx] = new IntArray(50, 50);
						hasDependency = true;
					}
					depIAr[oidx].addElement(i);
				}
			}
		}
		if (!hasDependency)
			return;
		dependencies = new int[oCont.getObjectCount()][];
		for (int i = 0; i < dependencies.length; i++) {
			dependencies[i] = (depIAr[i] == null) ? null : depIAr[i].getTrimmedArray();
		}
	}

	/**
	 * Sets the current direction of the filtering, one of the constants
	 * NoFilter=0, Filter2ToSet1=1, Filter1ToSet2=-1
	 */
	public void setFilteringDirection(int dir) {
		dir = (dir < 0) ? Filter1ToSet2 : (dir > 0) ? Filter2ToSet1 : NoFilter;
		if (dir == filterDirection)
			return;
		boolean objectsWereFiltered = filtered;
		//if (filterToListen!=null)
		//filterToListen.removePropertyChangeListener(this);
		filterToListen = null;
		filtered = false;
		if (filterDirection == Filter2ToSet1) {
			oCont2.removePropertyChangeListener(this);
			oCont.removeObjectFilter(this);
		} else if (filterDirection == Filter1ToSet2) {
			oCont.removePropertyChangeListener(this);
			oCont2.removeObjectFilter(this);
		}
		filterDirection = dir;
		if (filterDirection == Filter2ToSet1) {
			oCont2.addPropertyChangeListener(this);
			filterToListen = oCont2.getObjectFilter();
			setId = oCont.getEntitySetIdentifier();
			oCont.setObjectFilter(this);
		} else if (filterDirection == Filter1ToSet2) {
			oCont.addPropertyChangeListener(this);
			filterToListen = oCont.getObjectFilter();
			setId = oCont2.getEntitySetIdentifier();
			oCont2.setObjectFilter(this);
		}
		if (filterToListen != null) {
			//filterToListen.addPropertyChangeListener(this);
			filtered = filterToListen.areObjectsFiltered();
		}
		if (filtered != objectsWereFiltered) {
			notifyPropertyChange("Filter", null, null);
		}
	}

	/**
	* Returns the identifier of the entity set the filter is applied to
	*/
	@Override
	public String getEntitySetIdentifier() {
		if (filterDirection == Filter2ToSet1)
			return oCont.getEntitySetIdentifier();
		if (filterDirection == Filter1ToSet2)
			return oCont2.getEntitySetIdentifier();
		return null;
	}

	/**
	 * Reacts to changes of the filter of the object set from which the filtering is applied
	 */
	@Override
	public void propertyChange(PropertyChangeEvent e) {
		String propName = e.getPropertyName();
		boolean objectsWereFiltered = filtered;
		if (e.getSource().equals(oCont)) {
			if (propName.equalsIgnoreCase("destroyed") || propName.equalsIgnoreCase("ThematicDataRemoved") || propName.equalsIgnoreCase("structure_complete")) {
				destroy();
			} else if (propName.equalsIgnoreCase("Filter") || propName.equalsIgnoreCase("ObjectFilter")) {
				if (filterToListen != oCont.getObjectFilter()) {
					filterToListen = oCont.getObjectFilter();
				}
				if (filterToListen == null) {
					setFilteringDirection(NoFilter);
					return;
				}
				filtered = filterToListen.areObjectsFiltered();
				if (filtered || objectsWereFiltered) {
					notifyPropertyChange("Filter", null, null);
				}
			} else if (propName.equals("data_added") || propName.equals("data_removed") || propName.equals("data_updated") || propName.equals("update") || propName.equals("ObjectSet")) {
				getDependencies();
				filtered = filterToListen != null && filterToListen.areObjectsFiltered();
				if (filtered || objectsWereFiltered) {
					notifyPropertyChange("Filter", null, null);
				}
			}
		} else if (e.getSource().equals(oCont2)) {
			if (propName.equalsIgnoreCase("destroyed") || propName.equalsIgnoreCase("ThematicDataRemoved") || propName.equalsIgnoreCase("structure_complete")) {
				destroy();
			} else if (propName.equalsIgnoreCase("Filter") || propName.equalsIgnoreCase("ObjectFilter")) {
				if (filterToListen != oCont2.getObjectFilter()) {
					filterToListen = oCont2.getObjectFilter();
				}
				if (filterToListen == null) {
					setFilteringDirection(NoFilter);
					return;
				}
				filtered = filterToListen.areObjectsFiltered();
				if (filtered || objectsWereFiltered) {
					notifyPropertyChange("Filter", null, null);
				}
			} else if (propName.equals("data_added") || propName.equals("data_removed") || propName.equals("data_updated") || propName.equals("update") || propName.equals("ObjectSet")) {
				getDependencies();
				filtered = filterToListen != null && filterToListen.areObjectsFiltered();
				if (filtered || objectsWereFiltered) {
					notifyPropertyChange("Filter", null, null);
				}
			}
		}
	}

	/**
	 * Destroys the filter and sends a notification to all listeners about being
	 * destroyed
	 */
	@Override
	public void destroy() {
		setFilteringDirection(NoFilter);
		super.destroy();
	}

	/**
	 * Replies whether the filter is based on attribute values
	 */
	@Override
	public boolean isAttributeFilter() {
		return false;
	}

	/**
	 * Cancels the filter, i.e. makes all objects active
	 * Should set the value of the variable "filtered" to false!
	 */
	@Override
	public void clearFilter() {
		setFilteringDirection(NoFilter);
	}

	/**
	 * Checks whether the data item with the given index in the container is active,
	 * i.e. not filtered out.
	 */
	@Override
	public boolean isActive(int idx) {
		if (!filtered)
			return true;
		if (filterDirection == NoFilter)
			return true;
		if (oCont == null || oCont2 == null || filterToListen == null)
			return true;
		if (dependencies == null)
			return true;
		if (filterDirection == Filter2ToSet1) {
			if (idx >= dependencies.length || dependencies[idx] == null || dependencies[idx].length < 1)
				return false;
			for (int i = 0; i < dependencies[idx].length; i++)
				if (filterToListen.isActive(dependencies[idx][i]))
					return true;
			return false;
		}
		if (filterDirection == Filter1ToSet2) {
			for (int i = 0; i < dependencies.length; i++) {
				if (dependencies[i] == null) {
					continue;
				}
				if (filterToListen.isActive(i)) {
					for (int j = 0; j < dependencies[i].length; j++)
						if (dependencies[i][j] == idx)
							return true;
				}
			}
			return false;
		}
		return true;
	}

	/**
	* Replies whether the specified object is active (i.e. not filtered out)
	*/
	@Override
	public boolean isActive(String id) {
		if (!filtered)
			return true;
		if (filterDirection == NoFilter)
			return true;
		if (oCont == null || oCont2 == null || filterToListen == null)
			return true;
		if (dependencies == null)
			return true;
		if (filterDirection == Filter2ToSet1) {
			int idx = oCont.getObjectIndex(id);
			if (idx < 0)
				return false;
			return isActive(idx);
		}
		if (filterDirection == Filter1ToSet2) {
			int idx = oCont2.getObjectIndex(id);
			if (idx < 0)
				return false;
			return isActive(idx);
		}
		return true;
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
		int idx = item.getIndexInContainer();
		if (idx >= 0)
			return isActive(idx);
		return isActive(item.getId());
	}
}
