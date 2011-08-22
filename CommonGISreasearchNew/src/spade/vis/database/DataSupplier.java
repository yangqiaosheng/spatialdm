package spade.vis.database;

import java.util.Vector;

public interface DataSupplier {
	/**
	* Constructs and returns a DataPortion containing all DataItems
	* available
	*/
	public DataPortion getData();

	/**
	* Constructs and returns a DataPortion containing DataItems fitting in at
	* least one of the specified bounding rectangles (listed in the vector).
	* The elements of the vector are instances of
	* @see spade.vis.geometry.RealRectangle
	*/
	public DataPortion getData(Vector bounds);

	/**
	* When no more data from the DataSupplier are needed, this method is
	* called. Here the DataSupplier can clear its internal structures
	*/
	public void clearAll();
}
