package spade.vis.mapvis;

import spade.vis.geometry.Sign;

/**
* The interface to be implemented by all Visualizers representing thematic
* data by signs (symbols or diagrams). Informs what sign properties may be
* changed by the users (the methods starting with "may...") and changes the
* properties (the methods starting with "set..."). If some property is not
* relevant for a specific Visualizer, the corresponding "may..." method
* should return false, and the "set..." method should be empty.
* A SignDrawer may operate in 2 modes:
* 1) represent by signs all geographical objects in a layer;
* 2) represent only selected objects. This gives an opportunity to investigate
*    selected objects of interest more in detail and compare them without
*    being interfered by other signs.
* To enable the second mode, the SignDrawer must have a
* SelectiveDrawingController that informs the SignDrawer what objects to
* represent. The controller must be constructed by the SignDrawer at the
* moment of its initiation.
*/
public interface SignDrawer {
	/**
	* Returns the controller of selective sign drawing (i.e. when the visualizer
	* generates representations not for all objects but only for selected ones).
	* Such a controller must always exist for each SignDrawer
	*/
	public SelectiveDrawingController getSelectiveDrawingController();

	/**
	* Produces an instance of the Sign used by this visualizer. This instance will
	* be used for drawing the sign in the dialog for changing sign properties.
	*/
	public Sign getSignInstance();

	/**
	* Through this method a SignDrawer is informed that some of the sign properties
	* have changed. The sign drawer must care about repainting the map.
	* The possible identifiers of sign properties are listed in the class
	* @see spade.vis.geometry.Sign.
	*/
	public void signPropertyChanged(int propertyId, Sign sign);
}
