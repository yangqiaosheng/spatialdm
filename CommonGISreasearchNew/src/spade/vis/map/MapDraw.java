package spade.vis.map;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.Point;
import java.beans.PropertyChangeListener;

import spade.vis.event.EventMeaningManager;
import spade.vis.event.EventReceiver;

/**
* MapDraw is a basic interface used to link visualization and map manipulation
* components of Descartes to map drawing modules, in particular, to some
* "foreign" mapping software. From the perspective of the visualization
* and manipulation components, a MapDraw should be able to inform about
* mouse operations in the map area and to provide a MapContext allowing to
* transform screen coordinates into real coordinates.
*/

public interface MapDraw {
	/**
	* MapContext provides information necessary for transforming world
	* coordinates into screen coordinates and vice versa.
	*/
	public abstract MapContext getMapContext();

	/**
	 * Adjusts the current territory extent extent to make the given rectangular area visible
	 */
	public void adjustExtentToShowArea(float x1, float y1, float x2, float y2);

	/**
	 * Returns the current size of the map on the screen
	 */
	public Dimension getMapSize();

	/**
	* Returns its graphics. This may be necessary, for example, for dynamic
	* highlighting of objects in the map.
	*/
	public java.awt.Graphics getGraphics();

	/**
	 * Erases all marks such as position marks, if any.
	 */
	public void eraseAllMarks();

	/**
	* This method is used to clear all kinds of marking (e.g. highlighting)
	* drawn on top of the original map.
	*/
	public void restorePicture();

	/**
	* Redraws the map.
	*/
	public void redraw();

	/**
	* Returns the image with the drawn map
	*/
	public Image getMapImage();

	/**
	* Produces a new image with the drawn map
	*/
	public Image getMapAsImage();

	/**
	* Draws the map content to the specified bitmap in the memory
	*/
	public void paintToImage(Image img, int width, int height);

	/**
	* Registers a map listener that should implement the interface EventReceiver.
	* The MapDraw should inform this map listener about mouse events
	* occurring in the map. For this purpose the MapDraw generates
	* a MapEvent and invokes the function eventOccurred of each listener
	* with passing it this MapEvent as an argument.
	*/
	public void addMapListener(EventReceiver ml);

	/**
	* Unregisters an earlier registered Map Listener.
	*/
	public void removeMapListener(EventReceiver ml);

	/**
	* Listeners to map events may treat these events in their own way. For
	* example, mouse dragging may be used for zooming, shifting, or selection
	* of objects in the active layer. MapDraw can take into account current
	* (user-selected) meaning of each event and send the event to those
	* listeners that "understand" this meaning.
	* The method addMapEventMeaning is used by a map listener to register
	* the meaning it would like to process. A meaning should have a unique
	* (among meanings of the specified event) identifier and a full text that
	* can be shown to the user.
	*/
	public void addMapEventMeaning(String eventId, String meaningId, String meaningText);

	/**
	* Returns the manager of meanings of mouse events occurring in the map.
	* The manager may be used to change the current meaning (interpretation) of
	* some event.
	*/
	public EventMeaningManager getEventMeaningManager();

	/**
	* Returns location of the map on the screen in absolute (screen) coordinates
	*/
	public Point getLocationOnScreen();

	/**
	* Makes a correction of the given x-coordinate of the mouse (during dragging)
	* in order to avoid drawing outside the current map area.
	*/
	public int correctMouseX(int x);

	/**
	* Makes a correction of the given y-coordinate of the mouse (during dragging)
	* in order to avoid drawing outside the current map area.
	*/
	public int correctMouseY(int y);

	/**
	* A MapDraw may have listeners of its properties changes, for example,
	* zooming. This method registers a property change listener.
	*/
	public void addPropertyChangeListener(PropertyChangeListener l);

	public void removePropertyChangeListener(PropertyChangeListener l);
}
