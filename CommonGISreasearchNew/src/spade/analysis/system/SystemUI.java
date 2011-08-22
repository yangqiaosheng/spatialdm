package spade.analysis.system;

import java.awt.Component;
import java.awt.Frame;

import spade.analysis.plot.Plot;
import spade.lib.basicwin.NotificationLine;
import spade.vis.map.MapViewer;

/**
* This interface supports access from non-UI modules to UI (that may vary
* from configuration to configuration). This helps to make non-UI modules
* independent from current UI implementation
*/
public interface SystemUI {
	/**
	* Returns the main window (Frame) of the system, if it exists
	*/
	public Frame getMainFrame();

	/**
	* Informs whether a new map window must be used for each new visualisation.
	* If not, a new visualisation must be applied to the main map view.
	*/
	public boolean getUseNewMapForNewVis();

	/**
	* Sets whether a new map window must be used for each new visualisation.
	* If not, a new visualisation must be applied to the main map view.
	*/
	public void setUseNewMapForNewVis(boolean value);

	/**
	* Creates a MapViewer to display the map with the specified number
	*/
	public void openMapView(int mapN);

	/**
	* Brings the specified map view to top. In different configuration the map
	* view may be in a separate frame or in a tab, or ...
	*/
	public void bringMapToTop(MapViewer mview);

	/**
	* Returns the MapViewer displaying the map with the specified number
	*/
	public MapViewer getMapViewer(int mapN);

	/**
	* Returns the index of the current map (if multiple maps are possible)
	*/
	public int getCurrentMapN();

	/**
	* Returns the current MapViewer (if multiple map views are possible) or the
	* only map viewer available
	*/
	public MapViewer getCurrentMapViewer();

	/**
	* Returns the latest created MapViewer (if multiple map views are possible) or the
	* only map viewer available
	*/
	public MapViewer getLatestMapViewer();

	/**
	* Returns the map viewer with the given identifier. If the identifier is
	* null or "main", returns the primary map viewer (e.g. the one included
	* in the main window of the system). If the identifier is "_blank_",
	* constructs a  new map viewer.
	*/
	public MapViewer getMapViewer(String id);

	/**
	* Tries to find the map viewer with the given identifier. If not found,
	* returns null rather than tries to construct one. If the identifier is
	* null or "main", returns the primary map viewer (e.g. the one included
	* in the main window of the system).
	*/
	public MapViewer findMapViewer(String id);

	/**
	 * Asks the system UI to mark the specified absolute spatial (geographic) position
	 * on all currently open maps.
	 */
	public void markPositionOnMaps(float posX, float posY);

	/**
	 * Asks the system UI to erase the marks of the previously specified absolute spatial
	 * (geographic) position, if any, on all currently open maps.
	 */
	public void erasePositionMarksOnMaps();

//ID
	/**
	* Tries to find the plot with the given identifier. If not found,
	* returns null rather than tries to construct one.
	*/
	public Plot findPlot(String id);

//~ID
	/**
	* Closes the MapViewer displaying the map with the specified number
	* (e.g. when the map is removed from the system)
	*/
	public void closeMapView(int mapN);

	/**
	* Places the given component somewhere in the UI (e.g. creates a separate frame
	* for it, or puts an a subwindow etc.) May analyse the type of the component.
	*/
	public void placeComponent(Component c);

	/**
	* Removes the component from the UI
	*/
	public void removeComponent(Component c);

	/**
	* Shows the given message. If "trouble" is true, then this
	* is an error message.
	*/
	public void showMessage(String msg, boolean trouble);

	/**
	* Shows the given message. Assumes that this is not an error message.
	*/
	public void showMessage(String msg);

	/**
	* Clears the status line
	*/
	public void clearStatusLine();

	/**
	* Notifies about the status of some process. If "trouble" is true, then this
	* is an error message.
	*/
	public void notifyProcessState(String processName, String processState, boolean trouble);

	/**
	* Returns the status line (if exists)
	*/
	public NotificationLine getStatusLine();

	/**
	* Returns the UI for starting time analysis functions (if available)
	*/
	public Object getTimeUI();
}
