package spade.analysis.system;

import java.util.Vector;

/**
* A System Manager supplies information about current state of the system and
* serves certain requests. Is needed mainly to support functioning of a Guide
* (task-driven user guidance) so that the Guide is separated from any
* specific implementation and system configuration details.
*/

public interface SystemManager {
	public static final String layerContentTypes[] = { "unknown", "entities", "localities", "occurrences", "territory_division", "sample_locations", "grid" };

	/**
	* Displays help on the specified topic
	*/
	public void help(String topicId);

	/**
	* Check whether a help on the specified topic is available
	*/
	public boolean canHelp(String topicId);

	/**
	* Returns the number of maps (territories or different configurations of layers)
	* available in the system (not the number of map windows!)
	*/
	public int getMapCount();

	/**
	* Returns the list of identifiers of available layers belonging to the
	* map with the given index. Each layer should have its unique identifier.
	*/
	public Vector getLayerList(int mapN);

	/**
	* Returns the name of the layer with the given identifier in the map with
	* the given index
	*/
	public String getLayerName(int mapN, String layerId);

	/**
	* Returns the type of the layer with the given identifier (area, line, ...)
	* in the map with the given index
	*/
	public char getLayerType(int mapN, String layerId);

	/**
	* Returns true if the layer with the given identifier is drawn (visible)
	* on the map with the given index
	*/
	public boolean getLayerIsDrawn(int mapN, String layerId);

	/**
	* Switches on or off drawing of the specified map layer
	*/
	public void setLayerIsDrawn(int mapN, String layerId, boolean value);

	/**
	* Returns type of contents of the layer (see the array layerContentTypes)
	* in the map with the given index. May return "unknown"!
	*/
	public String getLayerContentType(int mapN, String layerId);

	/**
	* Returns true if there are thematic data associated with the geographical
	* objects of the given layer.
	*/
	public boolean getLayerHasAttrData(int mapN, String layerId);

	/**
	* Replies if the system supports selection of territories, e.g. from a list
	* or a hierarchy. Each territory is represented by an individual map.
	*/
	public boolean canSelectTerritory();

	/**
	* Starts the function of territory selection (if supported). Returns the number
	* of the map representing the selected territory
	*/
	public int selectTerritory();

	/**
	* Replies if the system supports selection of the territory extent to be
	* shown and considered by the user
	*/
	//public boolean canSelectExtent ();
	/**
	* Starts the function of selection of a territory extent on the map with
	* the given number (if supported).
	* Returns the bounding rectangle of the selected territory part
	*/
	//public RealRectangle selectExtent (int mapN);
	/**
	* Returns the currently visible territory extent (bounding rectangle)
	* on the map with the given number
	*/
	//public RealRectangle getCurrentExtent (int mapN);
	/**
	* Returns true if the system can draw a map into a bitmap in the memory
	* (Image) and return this image
	*/
	//public boolean canMakeMapImage ();
	/**
	* Draws a map containing the given set of layers (specified by their
	* identifiers) into a bitmap in the memory (Image) and returns this image
	*/
	/*
	public Image makeMapImage (int mapN,
	                           RealRectangle boundRect, Vector layers,
	                           int imageWidth, int imageHeight);
	*/
	/**
	* Returns the list of identifiers of the attributes associated with the
	* specified layer of the map with the given number or null if there are no
	* attribute data available
	*/
	public Vector getAttributesForLayer(int mapN, String layerId);

	/**
	* Returns the name of the attribute specified by its identifier, number of
	* the map and identifier of the map layer.
	*/
	public String getAttributeName(String attrId, int mapN, String layerId);

	/**
	* Returns the type of the attribute specified by its identifier, number of
	* the map and identifier of the map layer. The possible types of
	* attributes are defined in spade.kbase.scenarios.Common
	*/
	public char getAttributeType(String attrId, int mapN, String layerId);

	/**
	* Replies whether the attributes listed in the vector are comparable
	*/
	public boolean areComparable(Vector attrIds, int mapN, String layerId);

	/**
	* Replies whether the attributes listed in the vector are parts of some
	* whole
	*/
	public boolean arePartsInWhole(Vector attrIds, int mapN, String layerId);

	/**
	* Returns true if the system support pre-selection of data according to
	* a user-specified attribute query
	*/
	public boolean canProcessQueries();

	/**
	* Loads attribute data associated with the given map layer. The argument
	* "attributes" specifies the identifiers of the attributes the values of
	* which must be loaded. If attribute queries are supported,
	* processes the specified query (consisting of some constraints on
	* attribute values). The query may be null.
	*/
	public boolean loadAttributeData(int mapN, String layerId, Vector attributes, Vector constraints);

	/**
	* Lets the user select some attributes (e.g. for visualization). Returns
	* a vector of identifiers of selected attributes. The agruments minN and maxN
	* specify the minimum and the maximum number of attributes to be selected.
	* If equal to -1, then the minimum or the maximum number is not restricted.
	* The vector attrSubset specifies a subset of attributes from which to select.
	* If this subset is null or empty, allows selection from all attributes
	* available in the table.
	* The argument currentSelection is a vector of identifiers of attributes
	* that have been last selected.
	*/
	public Vector selectAttributes(int mapN, String layerId, Vector attrSubset, Vector currentSelection, int minN, int maxN, String prompt);

	/**
	* Replies if the map visualization method with the given identifier is
	* available in the system.
	*/
	public boolean isMapVisMethodAvailable(String methodId);

	/**
	* Visualizes the given attributes on the map by the specified visualization
	* method
	*/
	public void showDataOnMap(int mapN, String layerId, Vector attr, String methodId);

	/**
	* Erases visualization of thematic data on the specified GeoLayer
	*/
	public void eraseDataFromMap(int mapN, String layerId);

	/**
	* Duplicates the current map view
	*/
	public void duplicateMapView(int mapN);

	/**
	* Replies whether a tool performing the specified function is available
	* in the system. The list of possible functions can be found in the
	* *.dtd file of the knowledge base on task support.
	*/
	public boolean isToolAvailable(String function);

	/**
	* A tool is "runnable" when it should be specially activated in order to be
	* used, for example, generation of a plot or start of calculations.
	* Tools like "toggle_layers", "lookup_values" etc. do not require start of
	* any method, dialog, or wizard and are therefore not "runnable". The guide
	* can simply recommend to use these functions of the system and display
	* the help, if available
	*/
	public boolean isToolRunnable(String function);

	/**
	* Checks whether the specified tool is applicable to the given data
	*/
	public boolean isToolApplicable(int mapN, String layerId, Vector attr, String toolId);

	/**
	* Applies the specified tool to the given data. If fails, generate an error
	* message
	*/
	public boolean applyTool(int mapN, String layerId, Vector attr, String toolId);

	/**
	* Returns the error message (e.g. generated when the SystemManager failed to
	* apply a tool to given data)
	*/
	public String getErrorMessage();
}
