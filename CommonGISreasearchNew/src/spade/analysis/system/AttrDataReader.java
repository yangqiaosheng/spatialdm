package spade.analysis.system;

import spade.vis.database.DataTable;

/**
* The interface to be implemented by all classes that can read attribute
* (thematic) data from any sources.
* Readers that read both attribute and geographic data must implement the both
* interfaces AttrDataReader and GeoDataReader.
*/
public interface AttrDataReader {
	/**
	* Returns the table with the attribute (thematic) data loaded (if any)
	*/
	public DataTable getAttrData();
}