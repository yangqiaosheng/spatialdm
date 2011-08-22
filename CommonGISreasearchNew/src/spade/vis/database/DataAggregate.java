package spade.vis.database;

import java.util.Vector;

import spade.time.TimeMoment;
import spade.time.TimeReference;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: 27-Nov-2007
 * Time: 10:48:58
 * Aggregates several DataItems. An item may occur in an aggregate several times
 * if each occurrence refers to a different time interval.
 */
public class DataAggregate extends DataRecord {

	public DataAggregate(String identifier) {
		super(identifier);
	}

	public DataAggregate(String identifier, String name) {
		super(identifier, name);
	}

	/**
	 * Occurrences of DataItem united in this aggregate.
	 * An item may occur in an aggregate several times
	 * if each occurrence refers to a different time interval.
	 */
	protected Vector souItems = null;
	/**
	 * The time intervals the occurrences of the items refer to.
	 * Elements of the vector are instances of TimeReferences.
	 */
	protected Vector timeRefs = null;
	/**
	 * Which of the items are active, i.e. satisfy the current filter
	 */
	public boolean active[] = null;
	/**
	 * The number of currently active items
	 */
	public int nActive = 0;

	/**
	 * Adds an original (atomic) item to be aggregated.
	 */
	public void addItem(DataItem object) {
		addItem(object, null, null);
	}

	/**
	 * Adds a occurrence of an original (atomic) item to be aggregated
	 * and the time interval of the validity of this occurrence.
	 */
	public void addItem(DataItem object, TimeMoment startTime, TimeMoment endTime) {
		if (object == null)
			return;
		if (souItems == null) {
			souItems = new Vector(100, 100);
		}
		souItems.addElement(object);
		nActive = souItems.size();
		if (startTime == null && endTime == null) {
			TimeReference tr = object.getTimeReference();
			if (tr != null) {
				startTime = tr.getValidFrom();
				endTime = tr.getValidUntil();
			}
		}
		if (startTime != null || endTime != null) {
			if (tref == null) {
				tref = new TimeReference();
				tref.setValidFrom(startTime);
				tref.setValidUntil(endTime);
			} else {
				if (startTime == null) {
					tref.setValidFrom(null);
				} else if (tref.getValidFrom() != null && startTime.compareTo(tref.getValidFrom()) < 0) {
					tref.setValidFrom(startTime);
				}
				if (endTime == null) {
					tref.setValidUntil(null);
				} else if (tref.getValidUntil() != null && endTime.compareTo(tref.getValidUntil()) > 0) {
					tref.setValidUntil(endTime);
				}
			}
			if (timeRefs == null) {
				timeRefs = new Vector(100, 100);
			}
			while (timeRefs.size() < souItems.size() - 1) {
				timeRefs.addElement(null);
			}
			TimeReference tr = new TimeReference();
			tr.setValidFrom(startTime);
			tr.setValidUntil(endTime);
			timeRefs.addElement(tr);
		}
	}

	/**
	 * Returns the number of all item occurrences in this aggregate
	 */
	public int getItemOccurrencesCount() {
		if (souItems == null)
			return 0;
		return souItems.size();
	}

	/**
	 * Returns the item occurrence with the given index
	 */
	public DataItem getItemOccurrence(int idx) {
		if (souItems == null || idx < 0 || idx >= souItems.size())
			return null;
		return (DataItem) souItems.elementAt(idx);
	}

	/**
	 * Returns all item occurrences
	 */
	public Vector getAllItemOccurrences() {
		return souItems;
	}

	/**
	 * Returns all different items (each item is present in the resulting vector
	 * only once irrespective of the number of its occurrences)
	 */
	public Vector getAllDifferentItems() {
		if (souItems == null)
			return null;
		Vector v = new Vector(souItems.size(), 1);
		for (int i = 0; i < souItems.size(); i++)
			if (i == 0 || !v.contains(souItems.elementAt(i))) {
				v.addElement(souItems.elementAt(i));
			}
		return v;
	}

	/**
	 * Determines which of the items in this aggregate are currently active
	 * taking into account the filter. Returns the number of active items.
	 */
	public int accountForFilter(ObjectFilter filter, TimeFilter timeFilter) {
		if (souItems == null || souItems.size() < 1)
			return 0;
		if (active == null || active.length != souItems.size()) {
			active = new boolean[souItems.size()];
		}
		for (int i = 0; i < active.length; i++) {
			active[i] = true;
		}
		nActive = active.length;
		boolean timeFiltered = timeFilter != null && timeFilter.areObjectsFiltered() && timeRefs != null;
/*
    if (timeFiltered && id.equals("68")) {
      System.out.println("Filtering for site "+id);
    }
*/
		if (!timeFiltered && (filter == null || !filter.areObjectsFiltered()))
			return nActive;

		for (int i = 0; i < souItems.size(); i++)
			if (!filter.isActive((DataItem) souItems.elementAt(i))) {
				active[i] = false;
				--nActive;
			} else if (timeFiltered) {
				TimeReference tr = (i < timeRefs.size()) ? (TimeReference) timeRefs.elementAt(i) : null;
				if (tr != null && !timeFilter.isActive(tr)) {
					active[i] = false;
					--nActive;
				}
			}
		return nActive;
	}

	/**
	 * Informs if the item occurrence with the given index is currently active
	 */
	public boolean isItemOccurrenceActive(int idx) {
		if (souItems == null || idx < 0 || idx >= souItems.size())
			return false;
		if (active == null)
			return true;
		return active[idx];
	}

	/**
	 * Returns currently active item occurrences
	 */
	public Vector getActiveItemOccurrences() {
		if (souItems == null || souItems.size() < 1)
			return null;
		if (active == null || nActive == souItems.size())
			return souItems;
		if (nActive < 1)
			return null;
		Vector v = new Vector(nActive, 1);
		for (int i = 0; i < active.length; i++)
			if (active[i]) {
				v.addElement(souItems.elementAt(i));
			}
		return v;
	}

	/**
	 * Returns the different items having active occurrences (each item is
	 * present in the resulting vector only once irrespective of the number
	 * of its active occurrences)
	 */
	public Vector getDifferentActiveItems() {
		if (souItems == null)
			return null;
		if (active == null || nActive == souItems.size())
			return getAllDifferentItems();
		Vector v = new Vector(nActive, 1);
		for (int i = 0; i < active.length; i++)
			if (active[i] && !v.contains(souItems.elementAt(i))) {
				v.addElement(souItems.elementAt(i));
			}
		return v;
	}

	/**
	* The method copyTo(DataItem) is used for updating data items and spatial
	* objects derived from them when data change events occur, for example,
	* in visualisation of temporal data.
	* The DataItem passed as an argument should be an instance of DataAggregate.
	* The identifier of the data item is not copied! It is assumed that the
	* DataItem passed as an argument has the same identifier as this DataItem.
	*/
	@Override
	public void copyTo(DataItem dit) {
		if (dit == null)
			return;
		if (dit instanceof DataAggregate) {
			DataAggregate dr = (DataAggregate) dit;
			dr.setName(getName());
			if (souItems != null) {
				for (int i = 0; i < souItems.size(); i++) {
					dr.addItem((DataItem) souItems.elementAt(i));
				}
			}
			dr.setAttrList(getAttrList());
			dr.setAttrValues(getAttrValues());
		}
	}

	/**
	* Produces and returns a copy of itself.
	*/
	@Override
	public Object clone() {
		DataAggregate dr = new DataAggregate(id, name);
		if (souItems != null) {
			for (int i = 0; i < souItems.size(); i++) {
				dr.addItem((DataItem) souItems.elementAt(i));
			}
		}
		dr.setAttrList(getAttrList());
		dr.setAttrValues(getAttrValues());
		return dr;
	}
}
