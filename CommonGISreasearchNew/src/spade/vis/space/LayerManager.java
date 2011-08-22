package spade.vis.space;

/**
* LayerManager contains a set of GeoLayers and cares about the order of their
* drawing, putting diagrams, labels, etc.
*/

public interface LayerManager {
	/**
	 * Informs whether the coordinates are geographic, i.e. X is the longitude and Y is the latitude.
	 */
	public boolean isGeographic();

	/**
	 * Sets whether the coordinates should be treated as geographic, i.e. X as the longitude and Y as the latitude.
	 */
	public void setGeographic(boolean geographic);

	/**
	* Returns the unit in which coordinates are specified
	*/
	public String getUserUnit();

	/**
	* Sets the unit in which coordinates are specified
	*/
	public void setUserUnit(String unit);

	public int getLayerCount();

	public GeoLayer getGeoLayer(int idx);

	/**
	* Returns the index of the layer with the given identifier or -1 if there is
	* no such layer
	*/
	public int getIndexOfLayer(String layerId);

	/**
	* Returns the currently active layer
	*/
	public GeoLayer getActiveLayer();

	/**
	* Returns the index of the currently active layer
	*/
	public int getIndexOfActiveLayer();

	/**
	* Makes the layer with the given identifier currently active
	*/
	public void activateLayer(String layerId);

	/**
	* Makes the layer with the given index currently active
	*/
	public void activateLayer(int idx);

	/**
	* Removes the layer with the specified index
	*/
	public void removeGeoLayer(int idx);

	/**
	* Sets the ObjectManager to be used to support highlighting of objects of the
	* active GeoLayer.
	*/
	public void setObjectManager(ObjectManager manager);

	/**
	* Makes a copy of this LayerManager (without copying the ObjectManager attached)
	*/
	public LayerManager makeCopy();
}
