package spade.vis.dmap;

import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: Jan 12, 2010
 * Time: 2:24:07 PM
 * To change this template use File | Settings | File Templates.
 *
 * Combiner for Filters for segments of a trajectory stored in DMovingObject
 */
public class DMovingObjectSegmFilterCombiner {

	/**
	 * The DMovingObject the filter is attached to
	 */
	protected DMovingObject dmo = null;

	/**
	 * vectors of filters and their owners
	 */
	protected Vector<Object> filterOwners = null;
	protected Vector<DMovingObjectSegmFilter> filters = null;

	public DMovingObjectSegmFilterCombiner(DMovingObject dmo) {
		this.dmo = dmo;
	}

	/**
	 * makes a copy of itself and sets <dmo>
	 * used by DMovingObject.makeCopy
	 */
	public DMovingObjectSegmFilterCombiner makeCopy(DMovingObject dmo) {
		DMovingObjectSegmFilterCombiner segmFilterCombiner = new DMovingObjectSegmFilterCombiner(dmo);
		if (filterOwners != null) {
			segmFilterCombiner.filterOwners = new Vector<Object>(2, 2);
			segmFilterCombiner.filters = new Vector<DMovingObjectSegmFilter>(2, 2);
			for (int i = 0; i < filterOwners.size(); i++) {
				segmFilterCombiner.filterOwners.addElement(filterOwners.elementAt(i));
				segmFilterCombiner.filters.addElement(filters.elementAt(i).makeCopy(dmo));
			}
		}
		return segmFilterCombiner;
	}

	/**
	 * adds a new filter by storing owner's name and filter bitmap
	 */
	public void addFilter(Object filterOwner, int whatIsFiltered, boolean bitmap[]) {
		if (filterOwners == null) {
			filterOwners = new Vector<Object>(2, 2);
			filters = new Vector<DMovingObjectSegmFilter>(2, 2);
		}
		int idx = -1;
		for (int i = 0; idx == -1 && i < filterOwners.size(); i++)
			if (filterOwner.equals(filterOwners.elementAt(i))) {
				idx = i;
			}
		if (idx == -1) {
			DMovingObjectSegmFilter dmosf = new DMovingObjectSegmFilter(dmo);
			filterOwners.addElement(filterOwner);
			filters.addElement(dmosf);
			dmosf.setFilter(filterOwner, whatIsFiltered, bitmap);
		} else {
			filters.elementAt(idx).setFilter(filterOwner, whatIsFiltered, bitmap);
		}
	}

	/**
	 * clears the filter of the given owner
	 */
	public void clearFilter(Object filterOwner) {
		if (filterOwners == null)
			return;
		int idx = -1;
		for (int i = 0; idx == -1 && i < filterOwners.size(); i++)
			if (filterOwner.equals(filterOwners.elementAt(i))) {
				idx = i;
			}
		if (idx >= 0) {
			//filters.elementAt(idx).clearFilter();
			filters.removeElementAt(idx);
			filterOwners.removeElementAt(idx);
		}
	}

	/**
	 * checks if a given segment satisfyes all filters
	 */
	public boolean isSegmentActive(int idx) {
		if (filters == null || filters.size() == 0)
			return true;
		for (int i = 0; i < filters.size(); i++)
			if (!filters.elementAt(i).isSegmentActive(idx))
				return false;
		return true;
	}

	/**
	 * checks if any segment satisfyes the filter
	 */
	public boolean isAnySegmentActive() {
		if (filters == null || filters.size() == 0)
			return true;
		Vector trk = dmo.getTrack();
		if (trk == null)
			return false;
		int np = trk.size();
		for (int j = 0; j < np; j++) {
			boolean active = true;
			for (int i = 0; i < filters.size() && active; i++) {
				active = filters.elementAt(i).isSegmentActive(j);
			}
			if (active)
				return true;
		}
		return false;
	}

	/**
	 * checks if all segments are active
	 */
	public boolean areAllSegmentsActive() {
		if (filters == null || filters.size() == 0)
			return true;
		for (int i = 0; i < filters.size(); i++)
			if (!filters.elementAt(i).areAllSegmentsActive())
				return false;
		return true;
	}

	/**
	 * Checks if filter has any condition
	 */
	public boolean isFilterEmpty() {
		return (filters == null || filters.size() == 0 || filters.elementAt(0).getBitmap() == null);
	}

	public boolean hasFilterForSegments() {
		for (int i = 0; i < filters.size(); i++)
			if (filters.elementAt(i).isFilterForSegments())
				return true;
		return false;
	}

	public boolean hasFilterForPoints() {
		for (int i = 0; i < filters.size(); i++)
			if (!filters.elementAt(i).isFilterForSegments())
				return true;
		return false;
	}

	/**
	 * Used by filter owners for getting access to bitmap for changing values -
	 * to avoid creating bitmaps every time.
	 * For checking values isSegmentActive should be used.  
	 */
	public boolean[] getBitmap(Object owner) {
		if (filterOwners == null)
			return null;
		for (int i = 0; i < filterOwners.size(); i++)
			if (filterOwners.elementAt(i).equals(owner))
				return filters.elementAt(i).getBitmap();
		return null;
	}

}
