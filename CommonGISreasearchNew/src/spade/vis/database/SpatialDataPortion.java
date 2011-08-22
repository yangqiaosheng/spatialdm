package spade.vis.database;

import spade.vis.geometry.RealRectangle;

/**
* A SpatialDataPortion contains SpatialDataItems necessary for constructing
* GeoObjects. From a SpatialDataPortion a GeoLayer may be constructed.
*/
public interface SpatialDataPortion extends DataPortion {
	/**
	* Returns the bounding rectangle of all the geographical objects contained
	* in the data portion
	*/
	public RealRectangle getBoundingRectangle();

	/**
	* Reports whether it contains all spatial entities of a layer
	*/
	public boolean hasAllData();

	/**
	* Reports whether the SpatialDataItems comprising this portion are linked
	* to corresponding ThematicDataItems.
	*/
	public boolean hasThematicData();
}