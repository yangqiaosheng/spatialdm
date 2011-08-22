package spade.analysis.system;

import java.awt.Component;
import java.awt.Frame;
import java.beans.PropertyChangeListener;
import java.util.Hashtable;
import java.util.Vector;

import spade.vis.database.AttributeDataPortion;
import spade.vis.database.ObjectContainer;
import spade.vis.map.MapViewer;
import spade.vis.mapvis.SimpleDataMapper;
import spade.vis.mapvis.Visualizer;
import spade.vis.space.GeoLayer;
import spade.vis.spec.ToolSpec;

/**
* A DisplayProducer facilitate creation of various presentations of data
* (map visualisations, charts etc.)
*/

public interface DisplayProducer extends ToolManager {
	/**
	* Returns the number of available query and search tools such as dynamic query
	*/
	public int getQueryAndSearchToolCount();

	/**
	* Returns the identifier of the available query or search tool with the given
	* index
	*/
	public String getQueryOrSearchToolId(int idx);

	/**
	* Returns the name of the available query or search tool with the given
	* index
	*/
	public String getQueryOrSearchToolName(int idx);

	/**
	* Returns the name of the available query or search tool with the given
	* identifier
	*/
	public String getQueryOrSearchToolName(String toolId);

	/**
	* Creates the query or search tool with the given identifier. Returns a
	* reference to the tool constructed or null if failed.
	*/
	public Object makeQueryOrSearchTool(String toolId, ObjectContainer oCont);

	/**
	* Replies whether the tool with the given identifier requires attributes
	* for its operation.
	*/
	public boolean isToolAttributeFree(String toolId);

	/**
	 * Replies whether the tool with the given identifier implements the interface
	 * ObjectsSuitabilityChecker, i.e. can check if a given object container is
	 * suitable for this tool
	 */
	public boolean canToolCheckObjectsSuitability(String toolId);

	/**
	 * Generates an instance of the tool with the given identifier
	 */
	public Object makeToolInstance(String toolId);

	/**
	* Returns the number of available display methods (excluding map
	* visualization methods, dynamic query and other query/search/navigation
	* tools)
	*/
	public int getDisplayMethodCount();

	/**
	* Returns the name of the display method with the given index (to be shown
	* to the user)
	*/
	public String getDisplayMethodName(int idx);

	/**
	* Returns the identifier (internal) of the display method with the given index
	*/
	public String getDisplayMethodId(int idx);

	/**
	* Replies if the given display type is attribute-free (i.e. does not visualize
	* any attributes)
	*/
	public boolean isDisplayMethodAttributeFree(String methodId);

	/**
	* Checks if the display method with the given identifier is applicable to the
	* given data
	*/
	public boolean isDisplayMethodApplicable(int methodN, AttributeDataPortion dtab, Vector attr);

	/**
	* Applies the display method with the given index to the data in order to
	* generate a data display. Returns the resulting display (as a Component)
	*/
	public Component makeDisplay(AttributeDataPortion dtab, Vector attr, //attributes to visualize
			int methodN); //N of vis. method to apply

	/**
	* Applies the display method with the given identifier to the data in order to
	* generate a data display. Returns the resulting display (as a Component)
	* The argument properties may specify individual properties for the
	* display to be constructed.
	*/
	public Component makeDisplay(AttributeDataPortion dtab, Vector attr, //attributes to visualize
			String methodId, //identifier of vis. method to apply
			Hashtable properties);

	/**
	* Tries to produce a graphical display according to the given specification.
	* Returns the resulting display (as a Component).
	* If the graphic cannot be produced, returns null.
	*/
	public Component makeDisplay(ToolSpec spec, AttributeDataPortion dtab);

	/**
	* Applies the display method with the given index to the data in order to
	* generate a data display. Displays the resulting component on the screen.
	* Returns a reference to the component built.
	*/
	public Object display(AttributeDataPortion dtab, Vector attr, //attributes to visualize
			int methodN); //N of vis. method to apply

	/**
	* Applies the display method with the given identifier to the data in order to
	* generate a data display. Displays the resulting component on the screen.
	* The argument properties may specify individual properties for the
	* display to be constructed. Returns a reference to the component built.
	*/
	public Object display(AttributeDataPortion dtab, Vector attr, //attributes to visualize
			String methodId, //N of vis. method to apply
			Hashtable properties);

	/**
	* Displays already created graph
	*/
	public void showGraph(Component c);

	/**
	* Returns the dataMapper used for representation of data on a map
	*/
	public SimpleDataMapper getDataMapper();

	/**
	* Returns the supervisor that links together all displays
	*/
	public Supervisor getSupervisor();

	/**
	* Visualizes the specified attributes on the given map layers by the
	* "default" method selected by the DataMapper according to numbers and
	* types of attributes. Returns the Visualizer constructed.
	*/
	public Visualizer displayOnMap(AttributeDataPortion dtab, Vector attr, //the attributes to visualize on the map
			GeoLayer themLayer, //the layer to send the presentation
			MapViewer mapView); //where to add map manipulator

	/**
	* Visualizes the specified attributes on the given map layers by the method
	* specified through its identifier. Returns the Visualizer constructed.
	*/
	public Visualizer displayOnMap(String methodId, AttributeDataPortion dtab, Vector attr, //the attributes to visualize on the map
			GeoLayer themLayer, //the layer to send the presentation
			MapViewer mapView); //where to add map manipulator

	/**
	* Visualizes the specified attributes on the given map layers using the given
	* visualizer or classifier (previously constructed). For a given Classifier,
	* creates an appropriate Visualizer. Returns the Visualizer constructed.
	*/
	public Visualizer displayOnMap(Object visualizer, String methodId, AttributeDataPortion dtab, Vector attr, //the attributes to visualize on the map
			GeoLayer themLayer, //the layer to send the presentation
			boolean makeManipulator, //whether to create a manipulator
			MapViewer mapView); //where to add map manipulator

	/**
	* Visualizes the specified attributes on the given map layers using the given
	* visualizer or classifier (previously constructed). By default, creates a
	* map manipulator. Returns the Visualizer constructed.
	*/
	public Visualizer displayOnMap(Object visualizer, String methodId, AttributeDataPortion dtab, Vector attr, //the attributes to visualize on the map
			GeoLayer themLayer, //the layer to send the presentation
			MapViewer mapView); //where to add map manipulator

	/**
	* Creates an appropriate manipulator for the given visualizer and adds it to
	* the map window.
	*/
	public void makeMapManipulator(Object visualizer, String methodId, AttributeDataPortion dtab, GeoLayer themLayer, //the layer to manipulate
			MapViewer mapView); //where to add map manipulator

	/**
	 * Attaches the given visualizer to the given layer without producing a
	 * manipulator.
	 */
	public void setVisualizerInLayer(Visualizer vis, GeoLayer layer, MapViewer mapView);

	/**
	* Erases visualization of thematic data on the specified GeoLayer
	*/
	public void eraseDataFromMap(GeoLayer themLayer, //the layer to erase presentation
			MapViewer mapView); //from where to remove map manipulator

	/**
	* When a table is removed, the DisPlayProducer must close all displays that
	* are linked to this table. The table is specified by its identifier.
	*/
	public void tableIsRemoved(String tableId);

	/**
	* Provides access from non-UI modules to the system UI (that may vary
	* from configuration to configuration). This helps to make non-UI modules
	* independent from current UI implementation
	*/
	public SystemUI getUI();

	/**
	* Constructs a frame with the given title containing the given component
	*/
	public Frame makeWindow(Component c, String title);

	/**
	* Constructs a frame with the given title and the desired size containing the
	* given component.
	*/
	public Frame makeWindow(Component c, String title, int width, int height);

//ID
	/**
	* Returns a reference to the frame with non-cartographical displays (if exists).
	*/
	public Frame getChartFrame();

//~ID
	/**
	* Returns a reference to the frame with query tools (if exists).
	*/
	public Frame getQueryFrame();

	/**
	 * Closes tools that are destroyed
	 */
	public void closeDestroyedTools();

	/**
	* Registers a listener of appearing particular windows, such as the window
	* with non-cartographical displays or with a dynamic query.
	*/
	public void addWinCreateListener(PropertyChangeListener lst);

	/**
	* Removes a listener of appearing particular windows.
	*/
	public void removeWinCreateListener(PropertyChangeListener lst);
}
