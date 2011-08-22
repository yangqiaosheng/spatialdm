package spade.vis.database;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Vector;

import spade.lib.util.IntArray;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 23-Apr-2007
 * Time: 12:43:39
 */
public class ObjectFilterByCluster extends ObjectFilter {
	/**
	 * The numbers of the clusters the objects belong to.
	 * The order of the objects is the same as in the container.
	 * -1 means that the object is not in any cluster.
	 */
	protected int objClusters[] = null;
	/**
	 * Indicates for each cluster whether it is visible (true) or hidden (false).
	 * The numeration starts with 0 and ends with the largest cluster number.
	 * Some of the clusters may be absent.
	 */
	protected boolean clusterVisible[] = null;
	/**
	 * If the information about the clusters is stored in a table column,
	 * this is the index of the column; otherwise -1.
	 */
	protected int tblColN = -1;
	/**
	 * The filter may be operated in parallel by two or more controllers
	 * (UI components allowing the user to select/unselect clusters).
	 * This is the list of all current controllers. When one of the
	 * controllers changes the filter, the other controllers must be
	 * notified about the change.
	 */
	protected Vector<PropertyChangeListener> controllers = null;

	/**
	* Replies whether the filter is based on attribute values.
	*/
	@Override
	public boolean isAttributeFilter() {
		return tblColN >= 0;
	}

	/**
	 * If the information about the clusters is stored in a table column,
	 * this method returns the index of the column; otherwise -1.
	 */
	public int getTableColN() {
		return tblColN;
	}

	/**
	 * If the information about the clusters is stored in a table column,
	 * this method sets the index of the column.
	 */
	public void setTableColN(int tblColN) {
		this.tblColN = tblColN;
	}

	/**
	* Creates or updates its internal structures when attached to some object
	* container.
	*/
	@Override
	protected void updateData() {
		filtered = false;
		objClusters = null;
		clusterVisible = null;
	}

	/**
	 * Sets the results of clustering: object identifiers and their clusters.
	 * Note that the order of the object identifiers may be different from the
	 * order in the container.
	 */
	public void setClustering(String clObjIds[], int clNumbers[]) {
		clearFilter();
		if (oCont == null || oCont.getObjectCount() < 1)
			return;
		if (objClusters == null || objClusters.length != oCont.getObjectCount()) {
			objClusters = new int[oCont.getObjectCount()];
		}
		for (int i = 0; i < objClusters.length; i++) {
			objClusters[i] = -1;
		}
		int maxClN = -1;
		for (int i = 0; i < clObjIds.length; i++) {
			int idx = oCont.getObjectIndex(clObjIds[i]);
			if (idx >= 0) {
				objClusters[idx] = clNumbers[i];
				if (maxClN < clNumbers[i]) {
					maxClN = clNumbers[i];
				}
			}
		}
		int nClusters = maxClN + 1;
		if (clusterVisible == null || clusterVisible.length < nClusters) {
			clusterVisible = new boolean[nClusters];
		}
		for (int i = 0; i < clusterVisible.length; i++) {
			clusterVisible[i] = true;
		}
	}

	/**
	* Checks whether the data item with the given index in the container is active,
	* i.e. not filtered out.
	*/
	@Override
	public boolean isActive(int idx) {
		if (!filtered || objClusters == null || clusterVisible == null)
			return true;
		if (idx < 0 || idx >= objClusters.length)
			return true;
		if (objClusters[idx] < 0)
			return false;
		int clN = objClusters[idx];
		if (clN >= clusterVisible.length)
			return true;
		return clusterVisible[clN];
	}

	public int findObjectById(String id) {
		if (oCont == null)
			return -1;
		return oCont.getObjectIndex(id);
	}

	/**
	* Replies whether the specified object is active (i.e. not filtered out)
	*/
	@Override
	public boolean isActive(String id) {
		if (!filtered || objClusters == null || clusterVisible == null)
			return true;
		int idx = findObjectById(id);
		if (idx < 0)
			return false;
		return isActive(idx);
	}

	/**
	* Replies whether the specified object is active (i.e. not filtered out).
	* For an attribute filter the data item should be a ThematicDataItem
	*/
	@Override
	public boolean isActive(DataItem item) {
		if (!filtered)
			return true;
		if (item == null || oCont == null)
			return false;
		int idx = item.getIndexInContainer();
		if (idx >= 0 && item.equals(oCont.getObjectData(idx)))
			return isActive(idx);
		return isActive(item.getId());
	}

	/**
	 * The filter may be operated in parallel by two or more controllers
	 * (UI components allowing the user to select/unselect clusters).
	 * When one of the controllers changes the filter, the other controllers
	 * must be notified about the change.
	 * This method adds a controller to the filter.
	 */
	public void addController(PropertyChangeListener controller) {
		if (controller == null)
			return;
		if (controllers == null) {
			controllers = new Vector<PropertyChangeListener>(5, 5);
		}
		if (!controllers.contains(controller)) {
			controllers.addElement(controller);
		}
	}

	/**
	 * This method removes the given controller from the list of current controllers.
	 * If no other controllers remain, the filter is destroyed.
	 */
	public void removeController(PropertyChangeListener controller) {
		if (controller == null || controllers == null)
			return;
		int idx = controllers.indexOf(controller);
		if (idx < 0)
			return;
		controllers.removeElementAt(idx);
		if (controllers.size() < 1) {
			destroy();
		}
	}

	/**
	 * First calls super.notifyPropertyChange(...).
	 * Then, if there are 2 or more controllers, notifies the controllers
	 * about the change.
	 */
	@Override
	protected void notifyPropertyChange(String propName, Object oldValue, Object newValue) {
		super.notifyPropertyChange(propName, oldValue, newValue);
		if (controllers != null && controllers.size() > 1) {
			PropertyChangeEvent pce = new PropertyChangeEvent(this, "Filter", null, null);
			for (int i = 0; i < controllers.size(); i++) {
				controllers.elementAt(i).propertyChange(pce);
			}
		}
	}

	/**
	* Cancels the filter, i.e. makes all objects active
	* Should set the value of the variable "filtered" to false!
	*/
	@Override
	public void clearFilter() {
		if (!filtered)
			return;
		if (clusterVisible != null) {
			for (int i = 0; i < clusterVisible.length; i++) {
				clusterVisible[i] = true;
			}
		}
		filtered = false;
		notifyPropertyChange("Filter", null, null);
	}

	public boolean isClusterHidden(int classN) {
		return filtered && classN >= 0 && clusterVisible != null && classN < clusterVisible.length && !clusterVisible[classN];
	}

	public boolean checkIfFiltered() {
		if (clusterVisible == null)
			return false;
		for (int i = 0; i < clusterVisible.length; i++)
			if (!clusterVisible[i])
				return true;
		return false;
	}

	public void setClusterIsHidden(boolean hidden, int classN) {
		if (clusterVisible == null)
			return;
		if (classN < 0 || classN >= clusterVisible.length)
			return;
		if (hidden == !clusterVisible[classN])
			return; //no change
		clusterVisible[classN] = !hidden;
		filtered = checkIfFiltered();
		notifyPropertyChange("Filter", null, null);
	}

	public void setClustersAreHidden(IntArray numbers) {
		if (clusterVisible == null)
			return;
		if (numbers == null || numbers.size() < 1)
			return;
		boolean changed = false;
		for (int i = 0; i < numbers.size(); i++) {
			int classN = numbers.elementAt(i);
			if (classN >= 0 && classN < clusterVisible.length && clusterVisible[classN]) {
				clusterVisible[classN] = false;
				changed = true;
			}
		}
		filtered = filtered || changed;
		if (changed) {
			notifyPropertyChange("Filter", null, null);
		}
	}

	/**
	* Updates its internal structures when the objects in the container change.
	*/
	@Override
	public void propertyChange(PropertyChangeEvent pce) {
		if (pce.getSource().equals(oCont))
			if (pce.getPropertyName().equals("destroyed") || pce.getPropertyName().equals("ThematicDataRemoved") || pce.getPropertyName().equals("structure_complete")) {
				if (controllers != null && controllers.size() > 0) {
					PropertyChangeEvent e = new PropertyChangeEvent(this, "destroyed", null, null);
					for (int i = 0; i < controllers.size(); i++) {
						controllers.elementAt(i).propertyChange(e);
					}
				}
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
		filtered = false;
		objClusters = null;
		clusterVisible = null;
		controllers = null;
		super.destroy();
	}
}
