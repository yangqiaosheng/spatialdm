package spade.vis.map;

import java.awt.Component;

import spade.analysis.system.MapToolbar;
import spade.vis.event.EventMeaningManager;
import spade.vis.space.LayerManager;

/**
* The interface to be implemented by classes supporting map viewing.
* A MapViewer should be able to add and remove map manipulator
*/
public interface MapViewer {
	/**
	* Returns the unique identifier of the map view.
	*/
	public String getIdentifier();

	/**
	* Sets the unique identifier of the map view.
	*/
	public void setIdentifier(String id);

	/**
	* Replies whether this map view is the main map view in the system
	* (i.e. included in the main window).
	*/
	public boolean getIsPrimary();

	/**
	* Sets the indicator of this map view being the main map view in the system
	* (i.e. included in the main window).
	*/
	public void setIsPrimary(boolean value);

	/**
	* Returns the layer manager of the map
	*/
	public LayerManager getLayerManager();

	/**
	* Returns the component drawing the map
	*/
	public MapDraw getMapDrawer();

	/**
	* Returns the component of map toolbar
	*/
	public MapToolbar getMapToolbar();

	/**
	* Adds to the map view the map manipulator attached to the layer with the
	* given identifier
	*/
	public void addMapManipulator(Component man, Object visualizer, String layerId);

	/**
	* Removes from the map view the map manipulator attached to the layer with the
	* given identifier
	*/
	public void removeMapManipulator(Object visualizer, String layerId);

	/**
	 * Returns the manipulator of the layer with the given identifier
	 * and the given visualizer
	 */
	public Component getMapManipulator(Object visualizer, String layerId);

	/**
	* Returns the manager of interpretations of mouse events occurring in the
	* map area
	*/
	public EventMeaningManager getMapEventMeaningManager();

	/**
	* Used for moving all data visualizations from this map view to another map
	* view. This map view becomes "clear", i.e. without representing any thematic
	* data but only geographic data.
	*/
	public MapViewer makeCopyAndClear();

	/**
	* Copies all geographical data to another map view and returns this map view.
	* Does not copy any visualization of thematic data.
	*/
	public MapViewer makeClearCopy();

	/**
	* Zooms the map so that the whole territory is visible
	*/
	public void showWholeTerritory();

	/**
	* Zooms the map so that the specified territory extent is visible
	*/
	public void showTerrExtent(float x1, float y1, float x2, float y2);

	/**
	* Returns the currently visible territory extent in the map.
	* The extent is returned as an array of 4 floats:
	* 0) x1; 1) y1; 2) x2; 3) y2
	*/
	public float[] getMapExtent();

	/**
	* Checks if all the components of the MapView are valid; removes invalid
	* components (e.g. after a table has been removed the map manipulator
	* working with its data must be also removed)
	*/
	public void validateView();
}