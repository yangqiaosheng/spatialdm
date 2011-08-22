package spade.vis.map;

import java.awt.Graphics;
import java.beans.PropertyChangeListener;

import spade.vis.geometry.RealRectangle;

/**
* In order to be drawn in MapCanvas, an entity should implement the Mappable
* interface. The MapCanvas does not care what really is drawn in it.
* The only important thing is adherence to the Mappable interface.
* The Mappable entity may notify the map about changes of its properties.
* The changes may be of three kinds:
* 1) changes that affect both the map and the legend: the order of the layers,
*    adding/removing of a layer, switching on/off, change of colors etc.;
* 2) changes that affect only the map but not the legend: change of the
*    set of drawn GeoObjects (e.g. when the map is shifted), switching
*    on/off drawing of labels;
* 3) changes that affect only the legend but not the map, e.g. changes of data
*    statistics that are shown in the legend.
* When a Mappable entity registers a listener of its properties changes,
* it should remember which kind of events it is interested to be notified about.
* Any mappable entity should be able to draw itself in the map legend,
* therefore the Mappable interface extends the LegendDrawer interface.
*/

public interface Mappable extends LegendDrawer {
	/**
	 * Informs whether the coordinates are geographic, i.e. X is the longitude and Y is the latitude.
	 */
	public boolean isGeographic();

	/**
	* Returns the bounding rectangle including all available objects that can be
	* drawn in a map, irrespective of whether they are loaded into the memory or
	* not. A Mappable may return null if territory bounds are inavailable.
	*/
	public RealRectangle getWholeTerritoryBounds();

	/**
	* Returns the bounding rectangle including all currently loaded objects.
	* A Mappable may return null if there are no objects loaded.
	*/
	public RealRectangle getCurrentTerritoryBounds();

	/**
	* A Mappable may be drawn within a map canvas several times, for example,
	* in order to implement the "small multiples" visualization technique. This
	* method returns the number of the individual maps, i.e. how many times
	* the Mappable needs to be drawn in a map canvas.
	*/
	public int getMapCount();

	/**
	* Tells the Mappable which of the multiple maps it must draw next.
	*/
	public void setCurrentMapN(int mapN);

	/**
	* Returns the name of the map with the given index
	*/
	public String getMapName(int mapN);

	/**
	* In this function the Mappable thing should draw its "background" (polygons,
	* lines, grids, images, etc., but not diagrams and labels) in the MapCanvas.
	* MapContext provides information necessary for transforming world
	* coordinates into screen coordinates.
	*/
	public void drawBackground(Graphics g, MapContext mc);

	/**
	* This function is used to speed up map repainting when only diagrams
	* or labels change but not the background.
	*/
	public void drawForeground(Graphics g, MapContext mc);

	/**
	* In this function the Mappable thing should draw marked objects (e.g.
	* highlighted) in the MapCanvas. The idea is that the marked objects
	* are drawn on top of everything else. Besides, the MapCanvas may
	* have a bitmap in the memory where the picture without marking is stored.
	* This can be used for quick change of marking.
	* MapContext provides information necessary for transforming world
	* coordinates into screen coordinates.
	*/
	public void drawMarkedObjects(Graphics g, MapContext mc);

	/**
	 * A Mappable may be able to load geo data dynamically at the time of drawing,
	 * depending on the current scale and the visible territory extent.
	 * This method allows or suppresses this ability. It may be desirable to suppress
	 * the dynamic loading for quick drawing of a map, when there is no possibility
	 * to wait until new data are loaded.
	 */
	public void allowDynamicLoadingWhenDrawn(boolean allow);

	/**
	* Update may be called upon zooming/shifting operations with the map.
	* The Mappable may need to do some operations in this case, e.g. reload
	* the set of the objects to be drawn on the map.
	*/
	public void update(MapContext mc);

	/**
	* Registers a new PropertyChangeListener. Requires to specify if the
	* listener is interested to be notified about changes affecting map
	* appearance and about changes affecting legend. A listener may listen
	* to both kinds of changes. If none of the boolean arguments
	* listensMapAffectingChanges and listensLegendAffectingChanges is true,
	* the listener is not registered.
	* Practically, the Mappable can maintain two lists of listeners:
	* one with those interested in changes affecting map appearance and the
	* other with the listeners interested in changes affecting the legend.
	* A listener may be present in both lists simultaneously.
	*/
	public void addPropertyChangeListener(PropertyChangeListener l, boolean listensMapAffectingChanges, boolean listensLegendAffectingChanges);

	/**
	* The listener is removed from both lists.
	*/
	public void removePropertyChangeListener(PropertyChangeListener l);

	/**
	* Notifies the listeners about the property change.
	* Depending on the values of the arguments affectsMap and affectsLegend,
	* notifies listeners from one of the lists or from both. If a listener
	* is present in both lists, it is notified only once.
	*/
	public void notifyPropertyChange(String propName, Object oldValue, Object newValue, boolean affectsMap, boolean affectsLegend);

	/**
	* Makes necessary operations for destroying, e.g. unregisters from
	* listening highlighting and other events.
	*/
	public void destroy();
}
