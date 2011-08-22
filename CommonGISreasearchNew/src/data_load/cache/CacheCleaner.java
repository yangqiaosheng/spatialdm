package data_load.cache;

import java.util.Hashtable;
import java.util.Vector;

import spade.vis.database.SpatialDataItem;

public abstract class CacheCleaner {
	protected static long MEMORY_GAP;
	protected long memoryToClear;
	protected Hashtable sizes;

	public CacheCleaner() {
		sizes = new Hashtable();
		MEMORY_GAP = Runtime.getRuntime().freeMemory() / 2;
	}

	/**
	* Prepares to loading DataItems fitting in at least one of the specified
	* bounding rectangles (listed in the vector). The elements of the vector are
	* instances of
	* @see spade.vis.geometry.RealRectangle
	*/
	public void prepare(Vector bounds) {
		System.gc();
		memoryToClear = MEMORY_GAP - Runtime.getRuntime().freeMemory();
		trace("Preparing cache for cleaning");
		trace("Objects in cache: " + sizes.size());
		trace("Free memory: " + Runtime.getRuntime().freeMemory());
		trace("MEMORY_GAP: " + MEMORY_GAP);
		if (memoryToClear > 0) {
			trace("Memory to clear: " + memoryToClear);
		} else {
			trace("No need to clear cache");
		}
	}

	public abstract String nextObjectId();

	public abstract void add(SpatialDataItem sdi);

	public abstract void remove(String id);

	protected void trace(String message) {
		try {
			if (System.getProperty("debug") != null) {
				System.err.println(message);
			}
		} catch (Exception e) {
			System.err.println(message);
		}
	}
}