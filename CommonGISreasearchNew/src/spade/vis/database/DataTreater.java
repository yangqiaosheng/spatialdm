package spade.vis.database;

import java.util.Vector;

/**
* A general interface for classes that do something with data, e.g. visualize.
* A Data Treater should be able to reply which attributes it deals with.
*/

public interface DataTreater {
	/**
	* Returns a vector of IDs of the attributes this Data Treater deals with
	*/
	public Vector getAttributeList();

	/**
	* Replies whether it is linked to the data set (table) with the given identifier
	*/
	public boolean isLinkedToDataSet(String setId);

	/**
	* Returns a vector of colors used for representation of the attributes this
	* Data Treater deals with. May return null if no colors are used.
	*/
	public Vector getAttributeColors();
}