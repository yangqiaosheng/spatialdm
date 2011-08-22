package spade.analysis.system;

import spade.vis.database.AttributeDataPortion;

/**
* A URLOpener is linked to a table havind an attribute with the name "URL".
* It listens to double-click events related to objects of this table and
* opens the URLs corresponding to these objects, if they are specified
* in the table. A URLOpener should be created only in an applet.
*/
public interface URLOpener {
	/**
	* Sets the table from which to take the URLs
	*/
	public void setTable(AttributeDataPortion table);

	/**
	* Sets the Supervisor from which the URLOpener can receive object events
	*/
	public void setSupervisor(Supervisor sup);
}