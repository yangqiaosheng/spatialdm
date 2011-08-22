package spade.vis.dmap;

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: Jan 12, 2010
 * Time: 2:22:34 PM
 * To change this template use File | Settings | File Templates.
 *
 * Filter for segments of a trajectory stored in DMovingObject
 */
public class DMovingObjectSegmFilter {

	/**
	 * depending on the attribute of trajectories, either segments or points
	 * should be dispalyed after filtering
	 */
	public static int FilterForSegments = 0, FilterForPoints = 1;

	/**
	 * The DMovingObject the filter is attached to
	 */
	protected DMovingObject dmo = null;

	protected Object filterOwner = null;

	/**
	 * depending on the attribute of trajectories, either segments or points
	 * should be dispalyed after filtering
	 */
	protected int whatIsFiltered = FilterForSegments;

	public boolean isFilterForSegments() {
		return whatIsFiltered == FilterForSegments;
	}

	protected boolean bitmap[] = null;

	/**
	 * Used by filter owners for getting access to bitmap for changing values -
	 * to avoid creating bitmaps every time.
	 * For checking values isSegmentActive should be used.  
	 */
	public boolean[] getBitmap() {
		return bitmap;
	}

	public DMovingObjectSegmFilter(DMovingObject dmo) {
		this.dmo = dmo;
	}

	/**
	 * makes a copy of itself and sets <dmo>
	 * used by DMovingObject.makeCopy
	 */
	public DMovingObjectSegmFilter makeCopy(DMovingObject dmo) {
		DMovingObjectSegmFilter segmFilter = new DMovingObjectSegmFilter(dmo);
		segmFilter.setFilter(filterOwner, whatIsFiltered, bitmap);
		return segmFilter;
	}

	/**
	 * stores the owner and bitmap filter
	 */
	public void setFilter(Object filterOwner, int whatIsFiltered, boolean bitmap[]) {
		this.filterOwner = filterOwner;
		this.whatIsFiltered = whatIsFiltered;
		this.bitmap = bitmap;
	}

	/**
	 * Clears the filter
	 */
	public void clearFilter() {
		if (bitmap != null) {
			for (int b = 0; b < bitmap.length; b++) {
				bitmap[b] = true;
			}
		}
	}

	/**
	 * checks if a given segment satisfyes the filter
	 */
	public boolean isSegmentActive(int idx) {
		return bitmap == null || idx >= bitmap.length || bitmap[idx];
	}

	public boolean areAllSegmentsActive() {
		if (bitmap == null)
			return true;
		for (int b = 0; b < bitmap.length; b++)
			if (!bitmap[b])
				return false;
		return true;
	}

	public boolean isAnySegmentActive() {
		if (bitmap == null)
			return true;
		for (boolean element : bitmap)
			if (element)
				return true;
		return false;
	}

}
