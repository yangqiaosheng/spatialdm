package data_load.cache;

import java.util.Hashtable;
import java.util.Vector;

import spade.vis.database.SpatialDataItem;
import spade.vis.geometry.RealRectangle;

public class DistanceCacheCleaner extends LRUCacheCleaner {
	String ids[];
	float distances[];
	int pos;
	private Hashtable boundRects;
	private Vector requestedBounds;

	public DistanceCacheCleaner() {
		super();
		boundRects = new Hashtable();
	}

	/**
	* Prepares to loading DataItems fitting in at least one of the specified
	* bounding rectangles (listed in the vector). The elements of the vector are
	* instances of
	* @see spade.vis.geometry.RealRectangle
	*/
	@Override
	public void prepare(Vector bounds) {
		super.prepare(bounds);
		requestedBounds = bounds;
		int objcnt = history.size();
		if (objcnt == 0)
			return;
		ids = new String[objcnt];
		distances = new float[objcnt];
		pos = distances.length - 1;
		for (int i = 0; i < objcnt; i++) {
			ids[i] = (String) history.elementAt(i);
			distances[i] = distance(ids[i]);
		}
		sort(ids, distances, 0, objcnt - 1);
	}

	@Override
	public String nextObjectId() {
		if (memoryToClear <= 0 || pos < 0)
			return null;
		return ids[pos--];
	}

	@Override
	public void add(SpatialDataItem sdi) {
		super.add(sdi);
		String id = sdi.getId();
		boundRects.put(id, new RealRectangle(sdi.getGeometry().getBoundRect()));
	}

	private float distance(String id) {
		if (requestedBounds == null || requestedBounds.size() == 0)
			return 0;
		RealRectangle b = (RealRectangle) boundRects.get(id);
		float dist = b.distance((RealRectangle) requestedBounds.elementAt(0));
		for (int i = 1; i < requestedBounds.size(); i++) {
			dist = Math.min(dist, b.distance((RealRectangle) requestedBounds.elementAt(i)));
		}
		return dist;
	}

	private void sort(String id[], float data[], int l, int r) {
		int i = l;
		int j = r;
		float val = data[(l + r) / 2];
		while (i <= j) {
			while (data[i] < val) {
				i++;
			}
			while (data[j] > val) {
				j--;
			}
			if (i <= j) {
				float ival = data[i];
				data[i] = data[j];
				data[j] = ival;
				String iid = id[i];
				id[i] = id[j];
				id[j] = iid;
				i++;
				j--;
			}
		}
		if (l < j) {
			sort(id, data, l, j);
		}
		if (i < r) {
			sort(id, data, i, r);
		}
	}
}
