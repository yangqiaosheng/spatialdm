package spade.analysis.system;

import spade.vis.dmap.DGeoLayer;

/**
* The interface to be implemented by all classes that can read geographic data
* from any sources.
* Readers that read both attribute and geographic data must implement the both
* interfaces AttrDataReader and GeoDataReader.
*/
public interface GeoDataReader {
	/**
	* Returns the map layer constructed from the geographical data loaded (if any)
	*/
	public DGeoLayer getMapLayer();
}