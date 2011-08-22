package spade.vis.database;

import java.util.Vector;

import spade.vis.geometry.Geometry;

public interface SpatialDataItem extends DataItem {
	public Geometry getGeometry();

	public void setGeometry(Geometry geom);

	/**
	* Returns the type of its geometry
	*/
	public char getSpatialType();

	/**
	* A SpatialDataItem may have a ThematicDataItem associated with it.
	*/
	public void setThematicData(ThematicDataItem item);

	/**
	* Returns its ThematicDataItem
	*/
	public ThematicDataItem getThematicData();

	/**
	 * A SpatialDataItem may have multiple different states corresponding to
	 * different time moments or intervals. This method returns a vector of
	 * these states. The elements of the vector are instances of SpatialDataItem.
	 */
	public Vector getStates();

	/**
	 * A SpatialDataItem may have multiple different states corresponding to
	 * different time moments or intervals. This method sets a vector of these states.
	 * The elements of the vector are instances of SpatialDataItem.
	 */
	public void setStates(Vector states);
}