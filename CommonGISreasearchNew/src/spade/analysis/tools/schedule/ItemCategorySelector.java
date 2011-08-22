package spade.analysis.tools.schedule;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Vector;

import spade.lib.util.BubbleSort;
import spade.lib.util.Comparator;
import spade.lib.util.IntArray;
import spade.lib.util.ListSelector;
import spade.lib.util.StringUtil;
import spade.time.TimeMoment;
import spade.vis.database.DataRecord;
import spade.vis.database.ObjectFilterBySelection;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 12-Mar-2007
 * Time: 14:25:14
 * A non-UI class supporting the selection of the item category for viewing
 * corresponding transportation orders and other relevant data in course of
 * exploring a transportation schedule. The selection affects all currently
 * existing displays of the data relevant to the schedule.
 */
public class ItemCategorySelector implements Comparator, ListSelector {
	/**
	 * Contains the names of the categories of the transported items. Does not
	 * contain "LEER" or "EMPTY"!
	 */
	public Vector itemCategories = null;
	/**
	 * The index of the currently selected item category
	 */
	public int catIdx = -1;
	/**
	 * Data relevant to a transportation schedule, including the table with the
	 * transportation orders
	 */
	protected ScheduleData sd = null;
	/**
	 * Specifies the sequence of the records in the table with the transportation
	 * orders where the transportation orders of the same vehicle are grouped
	 * together and ordered temporally.
	 * This ordering is necessary for the selection of the relevant table
	 * records according to the selected item category.
	 */
	protected int recOrder[] = null;
	/**
	 * For each item category, contains an IntArray of the indexes of the relevant
	 * records in the table with the transportation orders (to avoid multiple
	 * searches).
	 */
	protected IntArray relRecNs[] = null;
	/**
	 * The filter attached to the table with the transportation orders, which
	 * is used to filter the records according to the selected item category.
	 */
	protected ObjectFilterBySelection catFilter = null;
	/**
	 * Listeners of changes of the selected category
	 */
	protected Vector catSelListeners = null;
	/**
	 * Listeners of changes of the selection of the relevant table records
	 */
	protected Vector recordSelListeners = null;

	public void setItemCategories(Vector itemCategories) {
		this.itemCategories = itemCategories;
	}

	public void setScheduleData(ScheduleData sd) {
		this.sd = sd;
		if (sd == null || sd.souTbl == null || sd.ldd == null || sd.souTbl.getDataItemCount() < 2 || sd.vehicleIdColIdx < 0 || sd.itemCatColIdx < 0 || sd.ldd.souTimeColIdx < 0)
			return;
		//determine the ordering of the table records
		Vector idxs = new Vector(sd.souTbl.getDataItemCount(), 10);
		for (int i = 0; i < sd.souTbl.getDataItemCount(); i++) {
			idxs.addElement(new Integer(i));
		}
		BubbleSort.sort(idxs, this);
		recOrder = new int[idxs.size()];
		for (int i = 0; i < idxs.size(); i++) {
			recOrder[i] = ((Integer) idxs.elementAt(i)).intValue();
		}
		catFilter = new ObjectFilterBySelection();
		catFilter.setObjectContainer(sd.souTbl);
		catFilter.setEntitySetIdentifier(sd.souTbl.getEntitySetIdentifier());
		sd.souTbl.setObjectFilter(catFilter);
		if (sd.linkLayer != null) {
			sd.linkLayer.setThematicFilter(sd.souTbl.getObjectFilter());
		}
	}

	/**
	 * A method from the Comparator interface.
	 * Returns 0 if the objects are equal, <0 if the first object is less than the
	 * second one, >0 otherwise
	 */
	@Override
	public int compare(Object obj1, Object obj2) {
		if (obj1 == null || obj2 == null || !(obj1 instanceof Integer) || !(obj2 instanceof Integer))
			return 0;
		int i1 = ((Integer) obj1).intValue(), i2 = ((Integer) obj2).intValue();
		if (i1 == i2)
			return 0;
		DataRecord rec1 = sd.souTbl.getDataRecord(i1), rec2 = sd.souTbl.getDataRecord(i2);
		String vId1 = rec1.getAttrValueAsString(sd.vehicleIdColIdx), vId2 = rec2.getAttrValueAsString(sd.vehicleIdColIdx);
		if (!StringUtil.sameStringsIgnoreCase(vId1, vId2))
			return vId1.compareTo(vId2);
		Object val1 = rec1.getAttrValue(sd.ldd.souTimeColIdx), val2 = rec2.getAttrValue(sd.ldd.souTimeColIdx);
		if (val1 == null || !(val1 instanceof TimeMoment))
			if (val2 == null || !(val2 instanceof TimeMoment))
				return 0;
			else
				return 1;
		if (val2 == null || !(val2 instanceof TimeMoment))
			return -1;
		TimeMoment t1 = (TimeMoment) val1, t2 = (TimeMoment) val2;
		return t1.compareTo(t2);
	}

	/**
	 * Performs the necessary operations for the selected item category so that
	 * all displays could show category-relevant data. Notifies all registered
	 * listeners about the change of the item category.
	 * The argument is the index of the selected category in the list of categories.
	 */
	public void doCategorySelection(int catN) {
		if (catIdx == catN)
			return;
		catIdx = catN;
		if (itemCategories == null || itemCategories.size() < 1)
			return;
		notifyCategoryChange();
		if (catIdx < 0 || catIdx >= itemCategories.size()) {
			if (recOrder != null) {
				notifyAboutRecordSelection();
			}
			return;
		}
		if (recOrder == null)
			return;
		String catName = (String) itemCategories.elementAt(catIdx);
		if (catName == null)
			return;
		//select relevant table records
		if (relRecNs == null) {
			relRecNs = new IntArray[itemCategories.size()];
			for (int i = 0; i < itemCategories.size(); i++) {
				relRecNs[i] = null;
			}
		}
		if (relRecNs[catIdx] != null) {
			notifyAboutRecordSelection();
			return;
		}
		relRecNs[catIdx] = new IntArray(recOrder.length, 10);
		for (int i = 0; i < recOrder.length; i++) {
			DataRecord rec = sd.souTbl.getDataRecord(recOrder[i]);
			String cat = rec.getAttrValueAsString(sd.itemCatColIdx);
			if (cat != null && cat.equalsIgnoreCase(catName)) {
				relRecNs[catIdx].addElement(recOrder[i]);
				continue;
			}
			if ((i >= recOrder.length - 1) || (cat != null && !cat.equalsIgnoreCase("LEER") && !cat.equalsIgnoreCase("EMPTY"))) {
				continue;
			}
			boolean nextIsRight = false;
			for (int j = i + 1; j < recOrder.length; j++) {
				//check the next record
				DataRecord rec1 = sd.souTbl.getDataRecord(recOrder[j]);
				cat = rec1.getAttrValueAsString(sd.itemCatColIdx);
				nextIsRight = cat != null && cat.equalsIgnoreCase(catName);
				if (nextIsRight) {
					break;
				}
				if (cat != null && !cat.equalsIgnoreCase("LEER") && !cat.equalsIgnoreCase("EMPTY")) {
					break;
				}
			}
			if (nextIsRight) {
				relRecNs[catIdx].addElement(recOrder[i]);
			}
		}
		notifyAboutRecordSelection();
	}

	/**
	 * Returns the index of the currently selected category
	 */
	public int getSelectedCategoryIndex() {
		return catIdx;
	}

	/**
	 * Returns the name of the currently selected category
	 */
	public String getSelectedCategory() {
		if (catIdx < 0 || itemCategories == null || catIdx >= itemCategories.size())
			return null;
		return (String) itemCategories.elementAt(catIdx);
	}

	/**
	 * Adds a listener of changes of the selected category
	 */
	public void addCategoryChangeListener(PropertyChangeListener pList) {
		if (pList == null)
			return;
		if (catSelListeners == null) {
			catSelListeners = new Vector(20, 20);
		}
		if (catSelListeners.contains(pList))
			return;
		catSelListeners.addElement(pList);
	}

	/**
	 * Removes a previously selected listener
	 */
	public void removeCategoryChangeListener(PropertyChangeListener pList) {
		if (pList == null || catSelListeners == null)
			return;
		int idx = catSelListeners.indexOf(pList);
		if (idx >= 0) {
			catSelListeners.removeElementAt(idx);
		}
	}

	/**
	 * Notifies its listeners about a change of the selected item category
	 */
	public void notifyCategoryChange() {
		if (catSelListeners == null || catSelListeners.size() < 1)
			return;
		PropertyChangeEvent pce = new PropertyChangeEvent(this, "category_selection", null, (catIdx >= 0) ? itemCategories.elementAt(catIdx) : null);
		for (int i = 0; i < catSelListeners.size(); i++) {
			((PropertyChangeListener) catSelListeners.elementAt(i)).propertyChange(pce);
		}
	}

	/**
	 * Adds a listener of changes of the selection of the relevant table records
	 */
	@Override
	public void addSelectListener(PropertyChangeListener pList) {
		if (pList == null)
			return;
		if (recordSelListeners == null) {
			recordSelListeners = new Vector(20, 20);
		}
		if (recordSelListeners.contains(pList))
			return;
		recordSelListeners.addElement(pList);
	}

	/**
	 * Removes the listener of changes of the selection of the relevant table records
	 */
	@Override
	public void removeSelectListener(PropertyChangeListener pList) {
		if (pList == null || recordSelListeners == null)
			return;
		int idx = recordSelListeners.indexOf(pList);
		if (idx >= 0) {
			recordSelListeners.removeElementAt(idx);
		}
	}

	/**
	 * Notify the listeners of changes of the selection of the relevant table
	 * records about the current selection status
	 */
	public void notifyAboutRecordSelection() {
		if (catFilter != null)
			if (catIdx < 0 || catIdx >= itemCategories.size()) {
				catFilter.clearFilter();
			} else {
				catFilter.setActiveObjectIndexes(relRecNs[catIdx]);
			}
		if (recordSelListeners == null || recordSelListeners.size() < 1)
			return;
		PropertyChangeEvent pce = null;
		if (catIdx < 0 || catIdx >= itemCategories.size()) {
			pce = new PropertyChangeEvent(this, "selection_cancelled", null, null);
		} else {
			pce = new PropertyChangeEvent(this, "items_selected", null, relRecNs[catIdx]);
		}
		for (int i = 0; i < recordSelListeners.size(); i++) {
			((PropertyChangeListener) recordSelListeners.elementAt(i)).propertyChange(pce);
		}
	}

	/**
	 * Informs whether any selection was applied to the table with the
	 * transportation orders
	 */
	@Override
	public boolean isSelectionApplied() {
		return catIdx >= 0 && relRecNs != null && catIdx < relRecNs.length && relRecNs[catIdx] != null;
	}

	/**
	 * Returns the indexes of the currently selected items
	 */
	@Override
	public IntArray getSelectedIndexes() {
		if (!isSelectionApplied())
			return null;
		return relRecNs[catIdx];
	}
}
