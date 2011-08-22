package spade.analysis.system;

import java.beans.PropertyChangeListener;

import spade.vis.database.AttributeDataPortion;
import spade.vis.database.DataTable;
import spade.vis.database.ObjectContainer;
import spade.vis.dmap.DGeoLayer;
import spade.vis.map.MapViewer;
import spade.vis.spec.DataSourceSpec;

/**
* A DataLoader loads data into the system.
*/
public interface DataLoader extends DataKeeper {
	/**
	* Loads data according to the given data source specification. Returns
	* true if successfully loaded.
	*/
	public boolean loadData(DataSourceSpec dss);

	/**
	* Loads application data according to the specification contained in the
	* given file. If the URL of the data server is specified, uses the data
	* server for loading the application.
	*/
	public boolean loadApplication(String path, String dataServerURL);

	/**
	* Writes the application description into an *.app file.
	*/
	public void saveApplication();

	/**
	* Adds the table to the system. Returns the index of the table in the
	* list of available tables.
	*/
	public int addTable(DataTable table);

	/**
	* Sets a formal link between the given layer and the table with the specified
	* number. The data from the table are not sent to the layer: it is assumed
	* that the layer already has them.
	*/
	public void setLink(DGeoLayer layer, int tblN);

	/**
	* Adds the layer to the map with the given index. Returns the index of
	* the layer in the map. If mapN<0, adds the layer to the current map.
	* If the map does not exist yet, creates it.
	*/
	public int addMapLayer(DGeoLayer layer, int mapN);

	/**
	* Adds the layer to the map with the given index. Returns the index of
	* the layer in the map. If mapN<0, adds the layer to the current map.
	* If the map does not exist yet, creates it.
	 * @param coordsAreGeographic - 0 if not, 1 if yes, -1 if unknown
	*/
	public int addMapLayer(DGeoLayer layer, int mapN, int coordsAreGeographic);

	/**
	* Adds the layer to the given map viewer. Returns the index of
	* the layer in the map. If mapView is null, adds the layer to the current map.
	*  If the map does not exist yet, creates it.

	 * @param layer DGeoLayer
	 * @param mapView MapViewer
	 */
	public int addMapLayer(DGeoLayer layer, MapViewer mapView);

	/**
	 * Creates the necessary component for dealing with a time-referenced object
	 * set, which must be previously added.
	 * Notifies about appearance of such data.
	 */
	public void processTimeReferencedObjectSet(ObjectContainer oCont);

	/**
	 * Creates the necessary component for dealing with time-referenced data
	 * table, which must be previously added.
	 * Notifies about appearance of such data.
	 */
	public void processTimeParameterTable(AttributeDataPortion table);

	/**
	* Updates the given table or layer by reloading the data from the data source
	* (the data might be changed by someone else).
	*/
	public boolean updateData(Object tableOrLayer);

	/**
	* Removes the table from the list of tables
	*/
	public void removeTable(String tableId);

	@Override
	public void removeTable(int idx);

	/**
	* Removes the map (layer manager) with the given index
	*/
	public void removeMap(int idx);

	/**
	* Sets the index of the current ("active") map, i.e. layer manager
	*/
	public void setCurrentMapN(int idx);

	/**
	* Returns the index of the current ("active") map, i.e. layer manager
	*/
	public int getCurrentMapN();

	/**
	* Registeres a listener of changes of the set of available tables and maps.
	* The listener must implement the PropertyChangeListener interface.
	*/
	public void addPropertyChangeListener(PropertyChangeListener l);

	/**
	* Unregisteres a listener of changes of the set of data.
	*/
	public void removePropertyChangeListener(PropertyChangeListener l);
}
