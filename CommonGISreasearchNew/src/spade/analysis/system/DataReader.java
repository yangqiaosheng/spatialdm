package spade.analysis.system;

import spade.vis.spec.DataSourceSpec;

/**
* The interface to be implemented by all classes reading data from any sources.
* The classes reading geographic data must additionally implement the interface
* GeoDataReader. The classes reading attribute data must additionally implement
* the interface AttrDataReader.
* Readers that read both attribute and geographic data must implement the both
* interfaces AttrDataReader and GeoDataReader.
*/
public interface DataReader {
	/**
	* Specifies the source of the data to be loaded.
	*/
	public void setDataSource(DataSourceSpec spec);

	/**
	* Returns its specification of the data source
	*/
	public DataSourceSpec getDataSourceSpecification();

	/**
	* Allows or prohibits using the cache while loading data over the Internet.
	*/
	public void setMayUseCache(boolean value);

	/**
	* Loads the data. When allowed (the argument mayAskUser is true) and necessary,
	* starts a dialog or a wizard for the user
	* to specify the data source, constraints, etc. Returns true if the data
	* have been successfully loaded.
	*/
	public boolean loadData(boolean mayAskUser);

	/**
	* Sets the system UI that can be used, in particular, for displaying
	* messages about the status of data loading. Through the system UI the reader
	* may get access to the main frame of the system (needed for dialogs). When
	* necessary, the reader may also get the map view and the map canvas from it.
	* In this way the current territory extent may be found out.
	*/
	public void setUI(SystemUI ui);
}
