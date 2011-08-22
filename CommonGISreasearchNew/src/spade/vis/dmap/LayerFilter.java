package spade.vis.dmap;

import java.beans.PropertyChangeEvent;
import java.util.Vector;

import spade.vis.database.CombinedFilter;
import spade.vis.database.DataItem;
import spade.vis.database.ObjectContainer;
import spade.vis.database.ObjectFilter;
import spade.vis.space.GeoLayer;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: May 27, 2010
 * Time: 4:16:14 PM
 * Allows to apply a filter of a map layer also to a table attached to the layer
 */
public class LayerFilter extends ObjectFilter {
	/**
	 * The layer whose filter must affect a table
	 */
	protected GeoLayer layer = null;
	/**
	 * The list of layer filters this filter accounts for
	 */
	protected Vector<ObjectFilter> filters = null;
	/**
	 * For the table records, contains indexes of the corresponding geo objects in the layer
	 */
	protected int corr[] = null;
	/**
	 * For each object of the container, contains true if active and false otherwise
	 */
	protected boolean objIsActive[] = null;
	/**
	 * Whether the values in objIsActive are valid
	 */
	protected boolean objIsActiveValid = false;

	public LayerFilter(GeoLayer layer) {
		this.layer = layer;
		setEntitySetIdentifier(layer.getEntitySetIdentifier());
		getFiltersToListen();
	}

	/**
	* Replies whether the filter is based on attribute values; returns false.
	*/
	@Override
	public boolean isAttributeFilter() {
		return false;
	}

	/**
	 * If listened to some layer filters, stops listening and removes them from the list.
	 */
	protected void removeFiltersToListen() {
		if (filters == null)
			return;
		boolean notify = this.areObjectsFiltered();
		for (int i = 0; i < filters.size(); i++) {
			filters.elementAt(i).removePropertyChangeListener(this);
		}
		filters.removeAllElements();
		if (objIsActive != null) {
			for (int i = 0; i < objIsActive.length; i++) {
				objIsActive[i] = true;
			}
		}
		objIsActiveValid = true;
		filtered = false;
		if (notify) {
			notifyPropertyChange("filter", null, this);
		}
	}

	/**
	 * Creates the list of layer filters to account for and starts listening to them.
	 * If some filters previously existed, stops listening to them and removes them from the list.
	 */
	public void getFiltersToListen() {
		removeFiltersToListen();
		ObjectFilter oFilter = layer.getObjectFilter();
		if (oFilter == null)
			return;
		if (oFilter instanceof CombinedFilter) {
			CombinedFilter cFilter = (CombinedFilter) oFilter;
			for (int i = 0; i < cFilter.getFilterCount(); i++) {
				ObjectFilter flt = cFilter.getFilter(i);
				if (flt.equals(this)) {
					continue;
				}
				if (!layer.equals(flt.getObjectContainer())) {
					continue;
				}
				if (flt.isAttributeFilter()) {
					continue;
				}
				if (filters == null) {
					filters = new Vector<ObjectFilter>(10, 10);
				}
				filters.addElement(flt);
				flt.addPropertyChangeListener(this);
			}
		} else if (!oFilter.isAttributeFilter() && !oFilter.equals(this) && oFilter.getObjectContainer().equals(layer)) {
			if (filters == null) {
				filters = new Vector<ObjectFilter>(10, 10);
			}
			filters.addElement(oFilter);
			oFilter.addPropertyChangeListener(this);
		}
		objIsActiveValid = false;
	}

	/**
	* Destroys the filter and sends a notification to all listeners about being
	* destroyed
	*/
	@Override
	public void destroy() {
		removeFiltersToListen();
		super.destroy();
	}

	/**
	* Updates its internal structures when the objects in the container change.
	*/
	@Override
	public void propertyChange(PropertyChangeEvent pce) {
		if (pce.getSource() instanceof ObjectFilter) {
			objIsActiveValid = false;
			notifyPropertyChange("filter", null, this);
		}
	}

	/**
	* Attaches this filter to an ObjectContainer
	*/
	@Override
	public void setObjectContainer(ObjectContainer cont) {
		super.setObjectContainer(cont);
		getCorrespondences();
	}

	/**
	 * Establishes correspondences between the table records and the geo objects
	 */
	protected void getCorrespondences() {
		corr = null;
		if (oCont == null || oCont.getObjectCount() < 1 || layer == null || layer.getObjectCount() < 1)
			return;
		corr = new int[oCont.getObjectCount()];
		int nObj = layer.getObjectCount();
		//System.out.println("LayerFilter: trying to get "+corr.length+" correspondences");
		//long t0=System.currentTimeMillis();
		for (int i = 0; i < corr.length; i++) {
			if (i < nObj && layer.getObjectAt(i).getIdentifier().equals(oCont.getObjectId(i))) {
				corr[i] = i;
			} else {
				corr[i] = layer.getObjectIndex(oCont.getObjectId(i));
			}
		}
		//long t1=System.currentTimeMillis();
		//System.out.println("LayerFilter: getting "+corr.length+" correspondences took "+(t1-t0)+" msec");
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
		if (filters == null || filters.size() < 1) {
			for (int j = 0; j < objIsActive.length; j++) {
				objIsActive[j] = true;
			}
		} else {
			for (int j = 0; j < objIsActive.length; j++) {
				objIsActive[j] = true;
				int idx = (corr != null) ? corr[j] : layer.getObjectIndex(oCont.getObjectId(j));
				if (idx >= 0) {
					for (int i = 0; i < filters.size() && objIsActive[j]; i++) {
						ObjectFilter flt = filters.elementAt(i);
						objIsActive[j] = flt.isActive(idx);
					}
				}
				filtered = filtered || !objIsActive[j];
			}
		}
		objIsActiveValid = true;
	}

	/**
	* Replies whether the specified object is active (i.e. not filtered out)
	*/
	@Override
	public boolean isActive(String id) {
		if (filters == null || filters.size() < 1)
			return true;
		int idx = layer.getObjectIndex(id);
		if (idx < 0)
			return true; //the layer filter does not apply to this object
		if (objIsActive != null && objIsActiveValid)
			return objIsActive[idx];
		applyFilters();
		if (objIsActive != null && objIsActiveValid)
			return objIsActive[idx];
		for (int i = 0; i < filters.size(); i++)
			if (!filters.elementAt(i).isActive(idx))
				return false;
		return true;
	}

	/**
	* Replies whether the specified object is active (i.e. not filtered out).
	* For an attribute filter the data item should be a ThematicDataItem
	*/
	@Override
	public boolean isActive(DataItem item) {
		if (item == null)
			return false;
		int idx = item.getIndexInContainer();
		if (idx >= 0 && item.equals(oCont.getObjectData(idx)))
			return isActive(idx);
		return isActive(item.getId());
	}

	/**
	* Checks whether the data item with the given index in the container is active,
	* i.e. not filtered out.
	*/
	@Override
	public boolean isActive(int idx) {
		if (filters == null || filters.size() < 1)
			return true;
		if (objIsActive != null && objIsActiveValid)
			return objIsActive[idx];
		applyFilters();
		if (objIsActive != null && objIsActiveValid)
			return objIsActive[idx];
		if (corr == null)
			return isActive(oCont.getObjectId(idx));
		if (idx < 0 || idx >= corr.length)
			return false;
		for (int i = 0; i < filters.size(); i++)
			if (!filters.elementAt(i).isActive(corr[idx]))
				return false;
		return true;
	}

	/**
	* Cancels the filter, i.e. makes all objects active
	* This object does not do anything because it does not filter but only transmits
	 * filtering from a layer to a table.
	*/
	@Override
	public void clearFilter() {
	}

	/**
	* Replies whether or not the object set is currently filtered
	*/
	@Override
	public boolean areObjectsFiltered() {
		if (filters == null || filters.size() < 1)
			return false;
		if (!objIsActiveValid) {
			applyFilters();
		}
		if (objIsActiveValid)
			return filtered;
		for (int i = 0; i < filters.size(); i++)
			if (filters.elementAt(i).areObjectsFiltered())
				return true;
		return false;
	}
}
