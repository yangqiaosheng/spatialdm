package spade.analysis.aggregates;

import java.beans.PropertyChangeEvent;
import java.util.Vector;

import spade.lib.util.IntArray;
import spade.lib.util.VectorUtil;
import spade.vis.database.DataItem;
import spade.vis.database.ObjectFilter;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Apr 21, 2008
 * Time: 2:37:38 PM
 * Filters aggregates by their members
 */
public class AggregateFilter extends ObjectFilter {
	/**
	 * A list of the currently selected aggregate members
	 */
	protected Vector memberIds = null;
	/**
	 * Indicates whether only aggregates containing ALL selected objects
	 * are active
	 */
	protected boolean requireAllSelectedMembers = false;
	/**
	 * Keeps the results of the last constraint checking (to avoid
	 * repeated checks).
	 */
	protected boolean active[] = null;

	/**
	* Replies whether the filter is based on attribute values; returns false.
	*/
	@Override
	public boolean isAttributeFilter() {
		return false;
	}

	public boolean mustHaveAllSelectedMembers() {
		return requireAllSelectedMembers;
	}

	public void setMustHaveAllSelectedMembers(boolean mustHaveAllSelectedMembers) {
		if (this.requireAllSelectedMembers == mustHaveAllSelectedMembers)
			return;
		this.requireAllSelectedMembers = mustHaveAllSelectedMembers;
		if (filtered && memberIds != null && memberIds.size() > 1) {
			applyFilter();
		}
	}

	/**
	* Creates or updates its internal structures when attached to some object
	* container.
	*/
	@Override
	protected void updateData() {
		filtered = false;
		if (memberIds != null) {
			memberIds.removeAllElements();
		}
		active = null;
	}

	/**
	 * Sets the currently selected subset of aggregate members, which are specified by
	 * their identifiers.
	 */
	public void setActiveMembers(Vector ids) {
		filtered = true;
		if (VectorUtil.sameVectors(ids, memberIds))
			return;
		if (ids != null && ids.size() > 0) {
			memberIds = (Vector) ids.clone();
		} else {
			memberIds.removeAllElements();
		}
		applyFilter();
	}

	/**
	 * Applies current filter constraints, i.e. list of active members
	 * taking into account the value of requireAllSelectedMembers
	 */
	protected void applyFilter() {
		if (oCont == null || !(oCont instanceof AggregateContainer))
			return;
		AggregateContainer aCont = (AggregateContainer) oCont;
		Vector aggregates = aCont.getAggregates();
		if (aggregates == null || aggregates.size() < 1)
			return;
		boolean changed = false;
		if (active == null) {
			active = new boolean[aggregates.size()];
			changed = true;
		}
		boolean noSelMembers = memberIds == null || memberIds.size() < 1;
		for (int i = 0; i < active.length; i++)
			if (noSelMembers) {
				active[i] = false;
			} else {
				Aggregate aggr = (Aggregate) aggregates.elementAt(i);
				Vector members = aggr.getAggregateMembers();
				boolean lastValue = active[i];
				if (members == null || members.size() < 1) {
					active[i] = false;
				} else if (requireAllSelectedMembers && members.size() < memberIds.size()) {
					active[i] = false;
				} else {
					active[i] = requireAllSelectedMembers;
					for (int j = 0; j < memberIds.size(); j++) {
						int midx = AggregateMember.findMemberById(members, (String) memberIds.elementAt(j));
						if (midx < 0)
							if (requireAllSelectedMembers) {
								active[i] = false;
								break;
							} else {
								;
							}
						else if (!requireAllSelectedMembers) {
							active[i] = true;
							break;
						}
					}
				}
				changed = changed || active[i] != lastValue;
			}
		if (changed) {
			notifyPropertyChange("Filter", null, null);
		}
	}

	/**
	 * Sets the currently selected subset of aggregate members, which are specified by
	 * their identifiers.
	 */
	public void setActiveMembers(String ids[]) {
		filtered = true;
		if (memberIds != null) {
			memberIds.removeAllElements();
		}
		if (ids != null && ids.length > 0) {
			if (memberIds == null) {
				memberIds = new Vector(50, 50);
			}
			for (String id : ids) {
				memberIds.addElement(id);
			}
		}
		applyFilter();
	}

	/**
	 * Returns the currently selected subset of aggregate members, which are specified by
	 * their identifiers.
	 */
	public Vector getActiveMemberIds() {
		return memberIds;
	}

	/**
	 * Checks whether the given aggregate is active
	 */
	public boolean isActive(Aggregate aggr) {
		if (aggr == null)
			return false;
		if (!filtered)
			return true;
		if (memberIds == null || memberIds.size() < 1)
			return false;
		Vector members = aggr.getAggregateMembers();
		if (members == null || members.size() < 1)
			return false;
		if (requireAllSelectedMembers && members.size() < memberIds.size())
			return false;
		for (int i = 0; i < memberIds.size(); i++) {
			int midx = AggregateMember.findMemberById(members, (String) memberIds.elementAt(i));
			if (midx < 0)
				if (requireAllSelectedMembers)
					return false;
				else {
					;
				}
			else if (!requireAllSelectedMembers)
				return true;
		}
		return requireAllSelectedMembers;
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
	 * Returns indexes of currently active aggregates
	 */
	public IntArray getActiveAggregatesIndexes() {
		if (!(oCont instanceof AggregateContainer))
			return null;
		if (memberIds == null || memberIds.size() < 1)
			return null;
		IntArray aai = new IntArray(oCont.getObjectCount(), 1);
		if (!filtered) {
			for (int i = 0; i < oCont.getObjectCount(); i++) {
				aai.addElement(i);
			}
			return aai;
		}
		if (active == null)
			return null;
		for (int i = 0; i < oCont.getObjectCount(); i++)
			if (active[i]) {
				aai.addElement(i);
			}
		if (aai.size() < 1)
			return null;
		return aai;
	}

	/**
	* Cancels the filter, i.e. makes all objects active
	* Should set the value of the variable "filtered" to false!
	*/
	@Override
	public void clearFilter() {
		if (!filtered)
			return;
		if (memberIds != null) {
			memberIds.removeAllElements();
		}
		if (active != null) {
			for (int i = 0; i < active.length; i++) {
				active[i] = true;
			}
		}
		filtered = false;
		notifyPropertyChange("Filter", null, null);
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
		memberIds = null;
		active = null;
		filtered = false;
		super.destroy();
	}
}
