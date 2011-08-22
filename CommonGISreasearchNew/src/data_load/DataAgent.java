package data_load;

import spade.analysis.system.SystemUI;
import spade.vis.database.DataSupplier;
import spade.vis.geometry.RealRectangle;

/**
* The interface to be implemented by a class acting as an intermediary between
* a data supplier and a data consumer. Such a class may, for example, optimize
* data access through caching.
*/
public interface DataAgent extends DataSupplier {
	/**
	* Sets the data reader, which actually reads data.
	*/
	public void setDataReader(DataSupplier reader);

	/**
	* Sets the system UI that can be used, in particular, for displaying
	* messages about the status of data loading.
	*/
	public void setUI(SystemUI ui);

	/**
	* Prepares itself to functioning.
	* @param extent   - the total territory extent of all the data in the data
	*                   source (valid for geographical data)
	* @param rowCount - the total number of rows in the database
	* On the basis of these parameters the DataAgent can, for example, determine
	* the parameters of the cache required.
	*/
	public void init(RealRectangle extent, int rowCount);
}