package spade.vis.database;

import java.beans.PropertyChangeEvent;
import java.util.Vector;

import spade.lib.util.IntArray;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 13-Mar-2007
 * Time: 14:00:21
 * Filters a set of objects through direct selection of the objects.
 */
public class ObjectFilterBySelection extends ObjectFilter {
	/**
	 * Indicates which of the objects of the container are active (true)
	 * and which are hidden (false)
	 */
	protected boolean objActive[] = null;
	/**
	 * Contains indexes of currently active objects
	 */
	protected IntArray objIndexes = null;

	/**
	* Replies whether the filter is based on attribute values; returns false.
	*/
	@Override
	public boolean isAttributeFilter() {
		return false;
	}

	/**
	* Creates or updates its internal structures when attached to some object
	* container.
	*/
	@Override
	protected void updateData() {
		filtered = false;
		if (objActive != null && oCont != null && oCont.getObjectCount() > 0 && objActive.length == oCont.getObjectCount()) {
			for (int i = 0; i < objActive.length; i++) {
				objActive[i] = true;
			}
		} else {
			objActive = null;
		}
		if (objIndexes != null) {
			objIndexes.removeAllElements();
		}
	}

	/**
	* Checks whether the data item with the given index in the container is active,
	* i.e. not filtered out.
	*/
	@Override
	public boolean isActive(int idx) {
		if (idx < 0)
			return false;
		return !filtered || objActive == null || idx >= objActive.length || objActive[idx];
	}

	/**
	* Replies whether the specified object is active (i.e. not filtered out)
	*/
	@Override
	public boolean isActive(String id) {
		if (!filtered)
			return true;
		if (oCont == null || id == null)
			return true;
		return isActive(oCont.getObjectIndex(id));
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
		if (idx >= 0 && item.equals(oCont.getObjectData(idx)))
			return isActive(idx);
		return isActive(item.getId());
	}

	/**
	* Cancels the filter, i.e. makes all objects active
	* Should set the value of the variable "filtered" to false!
	*/
	@Override
	public void clearFilter() {
		if (!filtered)
			return;
		updateData();
		notifyPropertyChange("Filter", null, null);
	}

	public void makeAllDeselected() {
		if (oCont == null || oCont.getObjectCount() < 1)
			return;
		if (objActive == null || objActive.length != oCont.getObjectCount()) {
			objActive = new boolean[oCont.getObjectCount()];
		}
		for (int i = 0; i < objActive.length; i++) {
			objActive[i] = false;
		}
		if (objIndexes != null) {
			objIndexes.removeAllElements();
		}
		filtered = true;
	}

	/**
	 * Selects or deselects the object with the given index
	 */
	public void setObjectStatus(int oIdx, boolean active) {
		if (oCont == null || oCont.getObjectCount() < 1)
			return;
		if (oIdx < 0 || oIdx >= oCont.getObjectCount())
			return;
		if (objActive == null || objActive.length != oCont.getObjectCount()) {
			objActive = new boolean[oCont.getObjectCount()];
			for (int i = 0; i < objActive.length; i++) {
				objActive[i] = false;
			}
			filtered = true;
		}
		objActive[oIdx] = active;
		if (objIndexes != null)
			if (active) {
				if (objIndexes.indexOf(oIdx) < 0) {
					objIndexes.addElement(oIdx);
				}
			} else {
				int k = objIndexes.indexOf(oIdx);
				if (k >= 0) {
					objIndexes.removeElementAt(k);
				}
			}
	}

	/**
	 * Sets the indexes of currently active (selected) objects.
	 */
	public void setActiveObjectIndexes(IntArray idxs) {
		if (oCont == null || oCont.getObjectCount() < 1)
			return;
		makeAllDeselected();
		filtered = true;
		if (idxs != null) {
			for (int i = 0; i < idxs.size(); i++) {
				int idx = idxs.elementAt(i);
				if (idx >= 0 && idx < objActive.length) {
					objActive[idx] = true;
					if (objIndexes != null) {
						objIndexes.addElement(idx);
					}
				}
			}
		}
		notifyPropertyChange("Filter", null, null);
	}

	/**
	 * Sets the indexes of currently active (selected) objects.
	 */
	public void setActiveObjectIndexes(int idxs[]) {
		if (oCont == null || oCont.getObjectCount() < 1)
			return;
		makeAllDeselected();
		filtered = true;
		if (idxs != null) {
			for (int i = 0; i < idxs.length; i++)
				if (idxs[i] >= 0 && idxs[i] < objActive.length) {
					objActive[idxs[i]] = true;
					if (objIndexes != null) {
						objIndexes.addElement(idxs[i]);
					}
				}
		}
		notifyPropertyChange("Filter", null, null);
	}

	/**
	 * Sets the currently selected subset of objects, which are specified by
	 * their identifiers.
	 */
	public void setActiveObjects(Vector ids) {
		if (oCont == null || oCont.getObjectCount() < 1)
			return;
		makeAllDeselected();
		filtered = true;
		if (ids != null) {
			for (int i = 0; i < ids.size(); i++) {
				int idx = oCont.getObjectIndex((String) ids.elementAt(i));
				if (idx >= 0 && idx < objActive.length) {
					objActive[idx] = true;
					if (objIndexes != null) {
						objIndexes.addElement(idx);
					}
				}
			}
		}
		notifyPropertyChange("Filter", null, null);
	}

	/**
	 * Returns the indexes of the selected (active) objects
	 */
	public IntArray getActiveObjectIndexes() {
		if (objIndexes != null)
			return objIndexes;
		if (oCont == null || oCont.getObjectCount() < 1)
			return null;
		objIndexes = new IntArray(oCont.getObjectCount(), 10);
		if (objActive == null) {
			for (int i = 0; i < oCont.getObjectCount(); i++) {
				objIndexes.addElement(i);
			}
		} else {
			for (int i = 0; i < objActive.length; i++)
				if (objActive[i]) {
					objIndexes.addElement(i);
				}
		}
		return objIndexes;
	}

	/**
	 * Returns the identifiers of the selected (active) objects
	 */
	public Vector getActiveObjects() {
		if (objIndexes == null) {
			getActiveObjectIndexes();
		}
		if (objIndexes == null || objIndexes.size() < 1)
			return null;
		Vector active = new Vector(objIndexes.size(), 5);
		for (int i = 0; i < objIndexes.size(); i++) {
			active.addElement(oCont.getObjectId(objIndexes.elementAt(i)));
		}
		return active;
	}

	/**
	* Updates its internal structures when the objects in the container change.
	*/
	@Override
	public void propertyChange(PropertyChangeEvent pce) {
		if (pce.getSource().equals(oCont))
			if (pce.getPropertyName().equals("destroyed") || pce.getPropertyName().equals("ThematicDataRemoved") || pce.getPropertyName().equals("structure_complete")) {
				destroy();
			} else if (pce.getPropertyName().equals("data_added") || pce.getPropertyName().equals("data_removed") || pce.getPropertyName().equals("data_updated") || pce.getPropertyName().equals("update") || pce.getPropertyName().equals("ObjectSet")) {
				updateData();
			}
	}

	/**
	* Destroys the filter and sends a notification to all listeners about being
	* destroyed
	*/
	@Override
	public void destroy() {
		objActive = null;
		objIndexes = null;
		super.destroy();
	}
}
