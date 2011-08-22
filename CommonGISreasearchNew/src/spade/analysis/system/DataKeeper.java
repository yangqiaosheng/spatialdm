package spade.analysis.system;

import spade.time.manage.TemporalDataManager;
import spade.vis.database.AttributeDataPortion;
import spade.vis.space.GeoLayer;
import spade.vis.space.LayerManager;
import spade.vis.spec.DataSourceSpec;

/**
* A DataKeeper provides access to all data loaded in the system
*/
public interface DataKeeper {
	/**
	* Returns the "factory" of data readers where all available classes for
	* reading data from various formats are registered
	*/
	public DataReaderFactory getDataReaderFactory();

	/**
	* Returns the number of available tables
	*/
	public int getTableCount();

	/**
	* Returns the number of available maps (layer managers)
	*/
	public int getMapCount();

	/**
	* Returns the index of the table with the given identifier or -1 if there
	* is no such table
	*/
	public int getTableIndex(String tableId);

	/**
	* Returns the table with the given index. If necessary, previously loads it.
	*/
	public AttributeDataPortion getTable(int idx);

	/**
	* Removes the table with the given index
	*/
	public void removeTable(int idx);

	/**
	* Returns the map (LayerManager) with the given index.
	*/
	public LayerManager getMap(int idx);

	/**
	* Returns the number of the map the given table refers to
	*/
	public int getTableMapN(AttributeDataPortion table);

	/**
	* Returns the identifier of the layer the given table refers to
	*/
	public String getTableLayerId(AttributeDataPortion table);

	/**
	* Returns the layer the given table refers to. The layer, if found, belongs
	* to the main map view (not to any of the auxiliary map windows).
	*/
	public GeoLayer getTableLayer(AttributeDataPortion table);

	/**
	* Returns the layer the given table refers to among the layers belonging to
	* the specified layer manager.
	*/
	public GeoLayer getTableLayer(AttributeDataPortion table, LayerManager lman);

	/**
	* Links the table to the map layer. The table is specified by its index in the
	* list of tables.
	*/
	public GeoLayer linkTableToMapLayer(int tblN, GeoLayer rightLayer);

	/**
	* Links a table to a map layer. The table is specified by its number in the
	* list of tables. The layer is specified by the number of the map in the
	* list of maps and the identifier of a layer of this map.
	*/
	public GeoLayer linkTableToMapLayer(int tblN, int mapN, String layerId);

	/**
	* Returns the component used for managing time-referenced data
	*/
	public TemporalDataManager getTimeManager();

	/**
	* Returns the path to the current application (project)
	*/
	public String getApplicationPath();

	/**
	 * Stores information about an exported table in order to include it
	 * in an *.app file when the project is saved.
	 */
	public void tableWasExported(AttributeDataPortion table, DataSourceSpec tableDescr);
}
