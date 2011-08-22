package spade.vis.database;

import java.beans.PropertyChangeEvent;

import spade.vis.dmap.DGeoLayer;
import spade.vis.geometry.Geometry;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Apr 23, 2008
 * Time: 11:13:04 AM
 * Filters spatial objects according to spatial constraints.
 */
public class SpatialFilter extends ObjectFilter {
	/**
	 * Used to check if a given geometry satisfies the spatial constraint(s),
	 * which is(are) defined externally
	 */
	protected SpatialConstraintChecker checker = null;
	/**
	 * Keeps the results of the last constraint checking (to avoid
	 * repeated checks).
	 */
	protected boolean active[] = null;

	public SpatialConstraintChecker getSpatialConstraintChecker() {
		return checker;
	}

	public void setSpatialConstraintChecker(SpatialConstraintChecker checker) {
		if (this.checker != null)
			if (this.checker.equals(checker))
				return;
			else {
				this.checker.removeConstraintChangeListener(this);
			}
		this.checker = checker;
		if (checker != null) {
			checker.addConstraintChangeListener(this);
		}
	}

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
		active = null;
		return;
	}

	/**
	* Checks whether the data item with the given index in the container is active,
	* i.e. not filtered out.
	*/
	@Override
	public boolean isActive(int idx) {
		return !filtered || (active != null && idx >= 0 && idx < active.length && active[idx]);
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
	* Replies whether the specified object is active (i.e. not filtered out)
	*/
	@Override
	public boolean isActive(String id) {
		if (!filtered)
			return true;
		if (id == null || oCont == null)
			return false;
		int idx = oCont.getObjectIndex(id);
		if (idx >= 0)
			return isActive(idx);
		return false;
	}

	/**
	* Cancels the filter, i.e. makes all objects active
	* Should set the value of the variable "filtered" to false!
	*/
	@Override
	public void clearFilter() {
		if (!filtered)
			return;
		if (active != null) {
			for (int i = 0; i < active.length; i++) {
				active[i] = true;
			}
		}
		filtered = false;
		notifyPropertyChange("Filter", null, null);
	}

	/**
	 * Checks the satisfaction of the spatial constraints with the use of
	 * its spatial constraint checker
	 */
	protected void applyFilter() {
		if (checker == null || oCont == null)
			return;
		if (!checker.hasConstraint()) {
			clearFilter();
			return;
		}
		filtered = true;
		boolean changed = false;
		DGeoLayer layer = null;
		if (oCont instanceof DGeoLayer) {
			layer = (DGeoLayer) oCont;
		}
		if (active == null) {
			active = new boolean[oCont.getObjectCount()];
			changed = true;
		}
		for (int i = 0; i < oCont.getObjectCount(); i++) {
			boolean fits = false;
			Geometry geom = null;
			if (layer != null) {
				geom = layer.getObject(i).getGeometry();
			} else {
				DataItem item = oCont.getObjectData(i);
				if (item instanceof SpatialDataItem) {
					geom = ((SpatialDataItem) item).getGeometry();
				}
			}
			if (geom != null) {
				fits = checker.doesSatisfySpatialConstraint(geom);
			}
			changed = changed || (fits != active[i]);
			active[i] = fits;
		}
		if (changed) {
			notifyPropertyChange("Filter", null, null);
		}
	}

	/**
	* Updates its internal structures when the objects in the container change.
	*/
	@Override
	public void propertyChange(PropertyChangeEvent pce) {
		if (pce.getSource().equals(oCont)) {
			if (pce.getPropertyName().equals("destroyed") || pce.getPropertyName().equals("ThematicDataRemoved") || pce.getPropertyName().equals("structure_complete")) {
				destroy();
			} else if (pce.getPropertyName().equals("data_added") || pce.getPropertyName().equals("data_removed") || pce.getPropertyName().equals("data_updated") || pce.getPropertyName().equals("update") || pce.getPropertyName().equals("ObjectSet")) {
				updateData();
			}
		} else if (pce.getSource().equals(checker)) {
			if (pce.getPropertyName().equals("destroyed")) {
				destroy();
			} else if (pce.getPropertyName().equals("constraint")) {
				applyFilter();
			}
		}
	}

	/**
	* Destroys the filter and sends a notification to all listeners about being
	* destroyed
	*/
	@Override
	public void destroy() {
		active = null;
		if (checker != null) {
			checker.removeConstraintChangeListener(this);
		}
		checker = null;
		super.destroy();
	}
}
