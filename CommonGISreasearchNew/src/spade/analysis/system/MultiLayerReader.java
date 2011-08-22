package spade.analysis.system;

import spade.vis.database.DataTable;
import spade.vis.dmap.DGeoLayer;

/**
* The interface to be implemented by readers that can load more than one layers
* in one reading operation. For example, a GML file may contain several layers
* of different types.
*/
public interface MultiLayerReader extends DataReader {
	/**
	* Reports how many layers have been loaded (or will be loaded).
	*/
	public int getLayerCount();

	/**
	* Returns the layer with the given index
	*/
	public DGeoLayer getMapLayer(int idx);

	/**
	* Returns the table with the attribute (thematic) data attached to the layer
	* with the given index (if any).
	*/
	public DataTable getAttrData(int idx);
}