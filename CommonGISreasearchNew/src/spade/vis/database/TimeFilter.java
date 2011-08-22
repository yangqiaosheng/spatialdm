package spade.vis.database;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Vector;

import spade.lib.util.IntArray;
import spade.lib.util.QSortAlgorithm;
import spade.time.IndexedMoment;
import spade.time.TimeMoment;
import spade.time.TimeReference;

/**
* Filters data items according to their time references.
*/
public class TimeFilter extends ObjectFilter {
	/**
	* If the filter is associated with some object container, this vector contains
	* time references of the objects.
	*/
	protected Vector trefs = null;
	/**
	* The first time moment found in the data (i.e. objects' time references)
	*/
	protected TimeMoment firstMoment = null;
	/**
	* The last time moment found in the data (i.e. objects' time references)
	*/
	protected TimeMoment lastMoment = null;
	/**
	* The starting time moment in the time filter
	*/
	protected TimeMoment start = null;
	/**
	* The ending time moment in the time filter
	*/
	protected TimeMoment finish = null;
	/**
	 * Indicates whether the starting time moment is included in the specified
	 * time interval. If not, the items which are only valid until this moment
	 * will be filtered out. By default, the starting moment is included.
	 */
	protected boolean includeIntervalStart = true;
	/**
	 * Indicates whether the ending time moment is included in the specified
	 * time interval. If not, the items which begin to be valid from this moment
	 * will be filtered out. By default, the ending moment is included.
	 */
	protected boolean includeIntervalEnd = true;
	/**
	 * Indicates that the filter must remove objects with a limited lifetime
	 * when the time bounds are not set (both are null). By default false.
	 */
	protected boolean removeTransitoryWhenNoFilter = false;
	/**
	 * Indicates that the filter must remove objects with non-limited lifetime
	 * (no time references or both validFrom and validUntil being nulls)
	 * when the time bounds are set. By default false.
	 */
	protected boolean removePermanentWhenFilter = false;
	/**
	 * The indices of the objects in the container sorted according to the values
	 * in the field "validFrom" in their time references. If some objects have no
	 * time references, they are not included in the orderedListValidFrom.
	 */
	protected IntArray orderedListValidFrom = null;
	/**
	* Opposite to orderedListValidFrom: for each object of the container specifies its
	* index in the orderedListValidFrom.
	*/
	protected IntArray timeIndexValidFrom = null;
	/**
	 * The indices of the objects in the container sorted according to the values
	 * in the field "validUntil" in their time references. If some objects have no
	 * time references, they are not included in the orderedListValidFrom.
	 * This list is created only if there are objects with different values of
	 * validFrom and validUntil.
	 */
	protected IntArray orderedListValidUntil = null;
	/**
	* Opposite to orderedListValidUntil: for each object of the container specifies its
	* index in the orderedListValidUntil.
	*/
	protected IntArray timeIndexValidUntil = null;
	/**
	* The index of the first element of the orderedListValidFrom satisfying the filter
	*/
	protected int firstIdxValidFrom = -1;
	/**
	* The index of the last element of the orderedListValidFrom satisfying the filter
	*/
	protected int lastIdxValidFrom = -1;
	/**
	* The index of the first element of the orderedListValidUntil satisfying the filter
	*/
	protected int firstIdxValidUntil = -1;
	/**
	* The index of the last element of the orderedListValidUntil satisfying the filter
	*/
	protected int lastIdxValidUntil = -1;
	/**
	 * Listeners of the changes of the current interval (irrespective of whether the
	 * filter really changes)
	 */
	protected Vector timeIntervalListeners = null;

	/**
	* Creates or updates its internal structures according to the object data
	*/
	@Override
	protected void updateData() {
		int nObj = 0;
		if (oCont != null) {
			nObj = oCont.getObjectCount();
		}
		trefs = (nObj < 1) ? null : (new Vector(nObj, 1));
		firstMoment = null;
		lastMoment = null;
		if (nObj < 1)
			return;
		Vector toSortValidFrom = new Vector(nObj, 50), toSortValidUntil = new Vector(nObj, 50);
		boolean allEndsAreNull = true;
		for (int i = 0; i < nObj; i++) {
			DataItem dit = oCont.getObjectData(i);
			if (dit == null) {
				trefs.addElement(null);
			} else {
				TimeReference tr = dit.getTimeReference();
				if (tr != null && (tr.getValidFrom() != null || tr.getValidUntil() != null)) {
					trefs.addElement(tr);
					TimeMoment t1 = tr.getValidFrom();
					IndexedMoment imom = new IndexedMoment(t1, i, true);
					toSortValidFrom.addElement(imom);
					//possibly, correct the first and the last date in the dataset
					if (t1 != null) {
						if (firstMoment == null || t1.compareTo(firstMoment) < 0) {
							firstMoment = t1;
						}
						if (lastMoment == null || t1.compareTo(lastMoment) > 0) {
							lastMoment = t1;
						}
					}
					TimeMoment t2 = tr.getValidUntil();
					allEndsAreNull = allEndsAreNull && (t2 == null);
					imom = new IndexedMoment(t2, i, false);
					toSortValidUntil.addElement(imom);
					if (t2 != null && t2.compareTo(lastMoment) > 0) {
						lastMoment = t2;
					}
				} else {
					trefs.addElement(null);
				}
			}
		}
		if (allEndsAreNull) {
			toSortValidUntil.removeAllElements();
		}
		if (toSortValidFrom.size() > 1) {
			QSortAlgorithm.sort(toSortValidFrom);
		}
		orderedListValidFrom = new IntArray(toSortValidFrom.size(), 1);
		if (timeIndexValidFrom == null) {
			timeIndexValidFrom = new IntArray(nObj, 50);
		} else {
			timeIndexValidFrom.removeAllElements();
		}
		for (int i = 0; i < nObj; i++) {
			timeIndexValidFrom.addElement(-1);
		}
		for (int i = 0; i < toSortValidFrom.size(); i++) {
			IndexedMoment imom = (IndexedMoment) toSortValidFrom.elementAt(i);
			orderedListValidFrom.addElement(imom.index);
			timeIndexValidFrom.setElementAt(i, imom.index);
			//System.out.println(imom.index+") "+imom.time.toString());
		}
		firstIdxValidFrom = 0;
		lastIdxValidFrom = orderedListValidFrom.size() - 1;
		if (toSortValidUntil.size() > 0) {
			if (toSortValidUntil.size() > 1) {
				QSortAlgorithm.sort(toSortValidUntil);
			}
			orderedListValidUntil = new IntArray(toSortValidUntil.size(), 1);
			if (timeIndexValidUntil == null) {
				timeIndexValidUntil = new IntArray(nObj, 50);
			} else {
				timeIndexValidUntil.removeAllElements();
			}
			for (int i = 0; i < nObj; i++) {
				timeIndexValidUntil.addElement(-1);
			}
			for (int i = 0; i < toSortValidUntil.size(); i++) {
				IndexedMoment imom = (IndexedMoment) toSortValidUntil.elementAt(i);
				orderedListValidUntil.addElement(imom.index);
				timeIndexValidUntil.setElementAt(i, imom.index);
				//System.out.println(imom.index+") "+imom.time.toString());
			}
			firstIdxValidUntil = 0;
			lastIdxValidUntil = orderedListValidUntil.size() - 1;
		} else {
			orderedListValidUntil = null;
			timeIndexValidUntil = null;
			firstIdxValidUntil = -1;
			lastIdxValidUntil = -1;
		}
	}

	/**
	* Checks whether the data item with the given index in the container is active,
	* i.e. not filtered out.
	*/
	@Override
	public boolean isActive(int idx) {
		if (timeIndexValidFrom == null)
			if (trefs == null)
				return true;
			else
				return isActive((TimeReference) trefs.elementAt(idx));
		int fIdx = timeIndexValidFrom.elementAt(idx);
		if (fIdx < 0)
			return !removePermanentWhenFilter || (start == null && finish == null);
		if (removeTransitoryWhenNoFilter && start == null && finish == null)
			return false;
		if (timeIndexValidUntil == null)
			return fIdx >= firstIdxValidFrom && fIdx <= lastIdxValidFrom;
		int uIdx = timeIndexValidUntil.elementAt(idx);
		if (uIdx < 0)
			return fIdx >= firstIdxValidFrom && fIdx <= lastIdxValidFrom;
		return fIdx <= lastIdxValidFrom && uIdx >= firstIdxValidUntil;
	}

	/**
	* Replies whether the specified object is active (i.e. not filtered out).
	* For this purpose checks if the time reference of the object lies between
	* the starting and ending moments.
	*/
	@Override
	public boolean isActive(String id) {
		if (oCont == null)
			return true;
		int idx = oCont.getObjectIndex(id);
		if (idx < 0)
			return true;
		return isActive(idx);
	}

	/**
	* Replies whether the specified data item is active (i.e. not filtered out).
	* For this purpose checks if the time reference of the item lies between
	* the starting and ending moments.
	*/
	@Override
	public boolean isActive(DataItem item) {
		if (item == null)
			return false;
		int idx = item.getIndexInContainer();
		if (idx >= 0 && item.equals(oCont.getObjectData(idx)))
			return isActive(idx);
		return isActive(item.getTimeReference());
	}

	/**
	* Checks whether the given time reference satisfies the time filter
	*/
	public boolean isActive(TimeReference tr) {
		if (tr == null || (tr.getValidFrom() == null && tr.getValidUntil() == null))
			return !removePermanentWhenFilter || (start == null && finish == null);
		if (removeTransitoryWhenNoFilter && start == null && finish == null)
			return false;
		return tr.isValid(start, finish, includeIntervalStart, includeIntervalEnd);
	}

	/**
	 * Checks whether an object with the specified lifetime interval
	 * satisfies the time filetr
	 */
	public boolean isActive(TimeMoment validFrom, TimeMoment validUntil) {
		if (validFrom == null && validUntil == null)
			return !removePermanentWhenFilter || (start == null && finish == null);
		if (removeTransitoryWhenNoFilter && start == null && finish == null)
			return false;
		if (start == null) {
			if (validFrom == null)
				return true;
			int c = validFrom.compareTo(finish);
			return c < 0 || (c == 0 && includeIntervalEnd);
		}
		if (finish == null) {
			if (validUntil == null)
				return true;
			int c = validUntil.compareTo(start);
			return c > 0 || (c == 0 && includeIntervalStart);
		}
		if (validFrom == null) {
			int c = validUntil.compareTo(start);
			return c > 0 || (c == 0 && includeIntervalStart);
		}
		int c = validFrom.compareTo(finish);
		if (c > 0 || (c == 0 && !includeIntervalEnd))
			return false;
		if (validUntil == null)
			return true;
		c = validUntil.compareTo(start);
		return c > 0 || (c == 0 && includeIntervalStart);
	}

	/**
	 * Returns true if both objects are either null or equal
	 */
	private static boolean same(Object o1, Object o2) {
		return (o1 == null && o2 == null) || (o1 != null && o2 != null && o1.equals(o2));
	}

	/**
	* Sets the filter period
	*/
	public void setFilterPeriod(TimeMoment begin, TimeMoment end) {
		//System.out.println("New begin="+begin+", end="+end);
		//System.out.println("Old begin="+start+", end="+finish);
		if (begin == null && end == null) {
			clearFilter();
			return;
		}
		if (same(begin, start) && same(end, finish))
			return;
		if (orderedListValidFrom == null) {
			start = (begin == null) ? null : begin.copyTo(start);
			finish = (end == null) ? null : end.copyTo(finish);
			filtered = true;
			notifyPropertyChange("Filter", null, null);
			return;
		}
		if (start == null && finish == null) {
			firstIdxValidFrom = 0;
			lastIdxValidFrom = orderedListValidFrom.size() - 1;
			if (orderedListValidUntil != null) {
				firstIdxValidUntil = 0;
				lastIdxValidUntil = orderedListValidUntil.size() - 1;
			}
		}
		int firstLastFrom[] = findFirstAndLast(orderedListValidFrom, firstIdxValidFrom, lastIdxValidFrom, true, begin, end);
		int firstLastUntil[] = null;
		if (orderedListValidUntil != null) {
			firstLastUntil = findFirstAndLast(orderedListValidUntil, firstIdxValidUntil, lastIdxValidUntil, false, begin, end);
		}
		start = (begin == null) ? null : begin.copyTo(start);
		finish = (end == null) ? null : end.copyTo(finish);
		boolean changed = firstIdxValidFrom != firstLastFrom[0] || lastIdxValidFrom != firstLastFrom[1];
		if (!changed && firstLastUntil != null) {
			changed = firstIdxValidUntil != firstLastUntil[0] || lastIdxValidUntil != firstLastUntil[1];
		}
		if (!changed) {
			notifyTimeIntervalChange();
			return;
		}
		firstIdxValidFrom = firstLastFrom[0];
		lastIdxValidFrom = firstLastFrom[1];
		if (firstLastUntil != null) {
			firstIdxValidUntil = firstLastUntil[0];
			lastIdxValidUntil = firstLastUntil[1];
		}
		filtered = firstIdxValidFrom > 0 || lastIdxValidFrom < orderedListValidFrom.size() - 1;
		if (!filtered && orderedListValidUntil != null) {
			filtered = firstIdxValidUntil > 0 || lastIdxValidUntil < orderedListValidUntil.size() - 1;
		}
		//System.out.println("firstIdxValidFrom="+firstIdxValidFrom+", lastIdxValidFrom="+lastIdxValidFrom);
		notifyPropertyChange("Filter", null, null);
	}

	public TimeMoment getFilterPeriodStart() {
		return start;
	}

	public TimeMoment getFilterPeriodEnd() {
		return finish;
	}

	/**
	 * Sets whether the starting time moment must be included in the specified
	 * time interval. If not, the items which are only valid until this moment
	 * will be filtered out. By default, the starting moment is included.
	 */
	public void setIncludeIntervalStart(boolean includeIntervalStart) {
		this.includeIntervalStart = includeIntervalStart;
	}

	/**
	 * Sets whether the ending time moment must be included in the specified
	 * time interval. If not, the items which begin to be valid from this moment
	 * will be filtered out. By default, the ending moment is included.
	 */
	public void setIncludeIntervalEnd(boolean includeIntervalEnd) {
		this.includeIntervalEnd = includeIntervalEnd;
	}

	/**
	 * Sets whether the filter must remove objects with a limited lifetime
	 * when the time bounds are not set (both are null). By default false.
	 */
	public void setRemoveTransitoryWhenNoFilter(boolean removeTransitoryWhenNoFilter) {
		this.removeTransitoryWhenNoFilter = removeTransitoryWhenNoFilter;
	}

	/**
	* Replies whether or not the object set is currently filtered
	*/
	@Override
	public boolean areObjectsFiltered() {
		return filtered || removeTransitoryWhenNoFilter;
	}

	/**
	 * Sets whether the filter must remove objects with non-limited lifetime
	 * (no time references or both validFrom and validUntil being nulls)
	 * when the time bounds are set. By default false.
	 */
	public void setRemovePermanentWhenFilter(boolean removePermanentWhenFilter) {
		this.removePermanentWhenFilter = removePermanentWhenFilter;
	}

	/**
	 * Finds the indexes of the first and last active objects in the given list
	 * for the new time interval. 
	 */
	private int[] findFirstAndLast(IntArray orderedList, int firstIdx, int lastIdx, boolean checkValidFrom, TimeMoment begin, TimeMoment end) {
		if (orderedList == null || orderedList.size() < 1)
			return null;
		int first = firstIdx, last = lastIdx;
		if (begin == null) {
			first = 0;
		} else if (firstIdx > 0 && start != null && begin.compareTo(start) < 0) {
			for (int i = firstIdx - 1; i >= 0; i--) {
				TimeReference tr = (TimeReference) trefs.elementAt(orderedList.elementAt(i));
				TimeMoment t = (checkValidFrom) ? tr.getValidFrom() : tr.getValidUntil();
				if (t == null) {
					break;
				}
				int c = t.compareTo(begin);
				if (c < 0 || (c == 0 && !includeIntervalStart)) {
					break;
				}
				first = i;
			}
		} else if (start == null || begin.compareTo(start) > 0) {
			for (int i = firstIdx; i < orderedList.size(); i++) {
				TimeReference tr = (TimeReference) trefs.elementAt(orderedList.elementAt(i));
				TimeMoment t = (checkValidFrom) ? tr.getValidFrom() : tr.getValidUntil();
				if (t == null) {
					first = i;
					break;
				}
				int c = t.compareTo(begin);
				if (c > 0 || (c == 0 && includeIntervalStart)) {
					first = i;
					break;
				}
				first = i + 1;
			}
		}
		if (end == null) {
			last = orderedList.size() - 1;
		} else if (finish == null || end.compareTo(finish) < 0) {
			for (int i = lastIdx; i >= 0; i--) {
				TimeReference tr = (TimeReference) trefs.elementAt(orderedList.elementAt(i));
				TimeMoment t = (checkValidFrom) ? tr.getValidFrom() : tr.getValidUntil();
				if (t == null) {
					last = i;
					break;
				}
				int c = t.compareTo(end);
				if (c < 0 || (c == 0 && includeIntervalEnd)) {
					last = i;
					break;
				}
				last = i - 1;
			}
		} else if (lastIdx < orderedList.size() - 1 && end.compareTo(finish) > 0) {
			for (int i = lastIdx + 1; i < orderedList.size(); i++) {
				TimeReference tr = (TimeReference) trefs.elementAt(orderedList.elementAt(i));
				TimeMoment t = (checkValidFrom) ? tr.getValidFrom() : tr.getValidUntil();
				if (t == null) {
					break;
				}
				int c = t.compareTo(end);
				if (c > 0 || (c == 0 && !includeIntervalEnd)) {
					break;
				}
				last = i;
			}
		}
		int pair[] = { first, last };
		return pair;
	}

	/**
	* Returns the earliest time moment found in the data
	*/
	public TimeMoment getEarliestMoment() {
		return firstMoment;
	}

	/**
	* Returns the latest time moment found in the data
	*/
	public TimeMoment getLatestMoment() {
		return lastMoment;
	}

	/**
	* Cancels the filter, i.e. makes all objects active
	* Should set the value of the variable "filtered" to false!
	*/
	@Override
	public void clearFilter() {
		if (!filtered) {
			notifyTimeIntervalChange();
			return;
		}
		filtered = false;
		start = finish = null;
		if (orderedListValidFrom != null) {
			firstIdxValidFrom = 0;
			lastIdxValidFrom = orderedListValidFrom.size() - 1;
		}
		if (orderedListValidUntil != null) {
			firstIdxValidUntil = 0;
			lastIdxValidUntil = orderedListValidUntil.size() - 1;
		}
		notifyPropertyChange("Filter", null, null);
	}

	/**
	* Replies whether the filter is based on attribute values
	*/
	@Override
	public boolean isAttributeFilter() {
		return false;
	}

	/**
	* Reacts to changes of its object container or to changes of the currently
	* selected interval
	*/
	@Override
	public void propertyChange(PropertyChangeEvent e) {
		if (e.getPropertyName().equalsIgnoreCase("ObjectSet")) {
			updateData();
		} else if (e.getPropertyName().equalsIgnoreCase("current_interval")) {
			TimeMoment t1 = (TimeMoment) e.getOldValue(), t2 = (TimeMoment) e.getNewValue();
			if (t1 == null && t2 == null) {
				clearFilter();
			} else {
				setFilterPeriod(t1, t2);
			}
		}
	}

	/**
	 * Adds a listener of the changes of the current interval (irrespective of whether the
	 * filter really changes)
	 */
	public void addTimeIntervalListener(PropertyChangeListener listener) {
		if (listener == null)
			return;
		if (timeIntervalListeners == null) {
			timeIntervalListeners = new Vector(5, 5);
		}
		timeIntervalListeners.addElement(listener);
	}

	/**
	 * Removes the listener of the changes of the current interval
	 */
	public void removeTimeIntervalListener(PropertyChangeListener listener) {
		if (listener == null || timeIntervalListeners == null)
			return;
		timeIntervalListeners.removeElement(listener);
	}

	/**
	 * Notifies the listeners of the changes of the current interval about
	 * the new time interval. The notification is sent only in a case when the
	 * filter itself did not change.
	 */
	public void notifyTimeIntervalChange() {
		if (timeIntervalListeners == null || timeIntervalListeners.size() < 1)
			return;
		PropertyChangeEvent pce = new PropertyChangeEvent(this, "current_interval", start, finish);
		for (int i = 0; i < timeIntervalListeners.size(); i++) {
			((PropertyChangeListener) timeIntervalListeners.elementAt(i)).propertyChange(pce);
		}
	}
}