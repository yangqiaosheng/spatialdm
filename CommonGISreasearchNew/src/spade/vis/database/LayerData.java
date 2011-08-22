package spade.vis.database;

import spade.vis.geometry.Geometry;
import spade.vis.geometry.MultiGeometry;
import spade.vis.geometry.RealRectangle;

public class LayerData extends GenericDataPortion implements SpatialDataPortion {
	protected RealRectangle bounds = null;
	protected boolean containsAllObjects = false, containsThematicData = false, isTimeReferenced = false;

	/**
	* Returns the bounding rectangle of all the geographical objects contained
	* in the data portion
	*/
	@Override
	public RealRectangle getBoundingRectangle() {
		return bounds;
	}

	public void setBoundingRectangle(float x1, float y1, float x2, float y2) {
		if (bounds == null) {
			bounds = new RealRectangle();
		}
		bounds.rx1 = x1;
		bounds.ry1 = y1;
		bounds.rx2 = x2;
		bounds.ry2 = y2;
	}

	/**
	* Reports whether it contains all spatial entities of a layer
	*/
	@Override
	public boolean hasAllData() {
		return containsAllObjects;
	}

	/**
	* The generator of this data portion may set whether it contains all the
	* geographical objects of the layer or only selected ones.
	*/
	public void setHasAllData(boolean value) {
		containsAllObjects = value;
	}

	/**
	* Reports whether the SpatialDataItems comprising this portion are linked
	* to corresponding ThematicDataItems.
	*/
	@Override
	public boolean hasThematicData() {
		return containsThematicData;
	}

	/**
	* Reports whether the SpatialDataItems comprising this portion are
	* temporally referenced.
	*/
	@Override
	public boolean hasTimeReferences() {
		return isTimeReferenced;
	}

	/**
	* Adds the data item to its vector of data items.
	* Redefines the addDataItem method of the parent so that
	* when >1 data items have the same identifier, they are combined in
	* a single data item with MultiGeometry;
	* Sorts the data items so that polygons are ordered in such a way that, when
	* smaller areas are inside larger ones, they are drawn on top of the larger
	* areas. For this purpose they are ordered to come later in the list.
	*/
	@Override
	public void addDataItem(DataItem item) {
		if (item == null)
			return;
		if (!(item instanceof SpatialDataItem))
			return;
		SpatialDataItem sdi = (SpatialDataItem) item;
		if (sdi.getGeometry() == null)
			return;
		containsThematicData = containsThematicData || sdi.getThematicData() != null;
		isTimeReferenced = isTimeReferenced || sdi.getTimeReference() != null;
		int insertIdx = findPlace(sdi);
		String itemId = item.getId();
		if (data == null || data.size() < 1 || itemId == null || itemId.length() < 1) {
			addInPlace(sdi, insertIdx);
		} else {
			int idx = indexOf(itemId);
			if (idx < 0) {
				addInPlace(sdi, insertIdx);
			} else {
				Geometry g = sdi.getGeometry();
				SpatialDataItem sdi0 = (SpatialDataItem) data.elementAt(idx);
				Geometry g0 = sdi0.getGeometry();
				if (g0 == null) {
					sdi0.setGeometry(g);
				} else { //generate a MultiGeometry
					MultiGeometry mg0 = null;
					if (g0 instanceof MultiGeometry) {
						mg0 = (MultiGeometry) g0;
					} else {
						while (mg0 == null) {
							try {
								mg0 = new MultiGeometry();
							} catch (Exception e) {
							}
						}
						mg0.addPart(g0);
						sdi0.setGeometry(mg0);
					}
					mg0.addPart(g);
				}
				if (insertIdx >= 0 && insertIdx < idx) {
					removeDataItem(idx);
					insertDataItemAt(sdi0, insertIdx);
				}
			}
		}
	}

	private void addInPlace(SpatialDataItem sdi, int idx) {
		if (data == null || idx < 0 || idx >= data.size()) {
			//IMPORTANT!!!
			super.addDataItem(sdi); //to avoid recursive calls of addDataItem!
		} else {
			super.insertDataItemAt(sdi, idx);
		}
	}

	/**
	* Adds a SpatialDataItem to its vector of data items without checking the
	* occurrence of its identifier among the existing data items and without
	* sorting.
	*/
	public void addItemSimple(SpatialDataItem item) {
		if (item == null)
			return;
		if (!(item instanceof SpatialDataItem))
			return;
		containsThematicData = containsThematicData || item.getThematicData() != null;
		isTimeReferenced = isTimeReferenced || item.getTimeReference() != null;
		super.addDataItem(item);
	}

	/**
	* Finds a place in the vector of data items where to insert the given data
	* item. If there are contours covered by the contour of this item, they should
	* come in the vector after this item (in order to be visible on the map).
	* If there are no covered contours, the method returns -1. This means that
	* the order is irrelevant.
	*/
	protected int findPlace(SpatialDataItem sdi) {
		try {
			if (sdi == null || sdi.getGeometry() == null)
				return -1;
			if (data == null || data.size() < 1 || sdi.getSpatialType() != Geometry.area)
				return -1;
			Geometry g = sdi.getGeometry();
			float w = g.getWidth(), h = g.getHeight();
			if (w <= 0 || h <= 0)
				return -1;
			RealRectangle br = new RealRectangle(g.getBoundRect());
			for (int i = 0; i < data.size(); i++) {
				SpatialDataItem spd = (SpatialDataItem) data.elementAt(i);
				Geometry g0 = spd.getGeometry();
				if (g0 == null) {
					continue;
				}
				if (w >= g0.getWidth() && h >= g0.getHeight()) {
					float b[] = g0.getBoundRect();
					if (b == null) {
						continue;
					}
					if (br.rx1 < b[0] && br.rx2 > b[2] && br.ry1 < b[1] && br.ry2 > b[3])
						return i;
				}
			}
		} catch (Exception e) {
		}
		return -1;
	}
}