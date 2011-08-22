package data_load.cache;

import java.util.Vector;

import spade.vis.database.SpatialDataItem;

public class LRUCacheCleaner extends CacheCleaner {
	protected Vector history;

	public LRUCacheCleaner() {
		super();
		history = new Vector();
	}

	@Override
	public String nextObjectId() {
		if (memoryToClear <= 0 || sizes.size() == 0)
			return null;
		return (String) history.elementAt(0);
	}

	@Override
	public void add(SpatialDataItem sdi) {
		String id = sdi.getId();
		history.removeElement(id);
		history.addElement(id);
		sizes.put(id, new Integer(MemoryUtil.sizeOf(sdi)));
	}

	@Override
	public void remove(String id) {
		history.removeElement(id);
		memoryToClear -= ((Integer) sizes.get(id)).intValue();
		sizes.remove(id);
	}
}