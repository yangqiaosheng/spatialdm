package spade.vis.database;

/**
* This interface is used for "delayed" loading of tables described in an
* application specification. When the application is initially loaded, the
* tables are only registered, but the data are really loaded only when they are
* first needed. For this purpose a table may have a reference to a
* TableContentSupplier. When some method of the table is called that requires
* the data to be present, the table asks this supplier to provide the data.
* Behind this interface usually stands a component that reads attribute data
* from a file or a database.
*/
public interface TableContentSupplier {
	/**
	* Using this method the table asks its supplier to fill it with the data.
	* The method returns true if the data have been successfully loaded.
	*/
	public boolean fillTable();
}