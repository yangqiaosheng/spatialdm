package guide_tools.guide;

import java.awt.Component;
import java.util.Vector;

import spade.analysis.system.DataKeeper;
import spade.analysis.system.DisplayProducer;
import spade.analysis.system.SystemManager;
import spade.analysis.system.ToolManager;
import spade.kbase.scenarios.Common;
import spade.lib.help.Helper;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.DataTable;
import spade.vis.database.SemanticsManager;
import spade.vis.mapvis.DataMapper;
import spade.vis.space.GeoLayer;
import spade.vis.space.LayerManager;
import ui.AttributeChooser;

/**
* Links the task guide with the system, i.e. supports access from the
* components of the guide to various system functions.
*/

public class GuideSupport implements SystemManager {
	protected DataKeeper dataKeeper = null;
	protected DisplayProducer displayProducer = null;
	protected ToolManager calcManager = null;
	/**
	* This is an index of already found links between layers and tables
	* stored for easier and faster reuse
	*/
	protected Vector index = new Vector(5, 5);
	/**
	* The error message generated when application of some data analysis tool
	* fails
	*/
	protected String err = null;

	public void setDataKeeper(DataKeeper dk) {
		dataKeeper = dk;
	}

	public void setDisplayProducer(DisplayProducer dprod) {
		displayProducer = dprod;
	}

	public void setCalculationManager(ToolManager calcManager) {
		this.calcManager = calcManager;
	}

	/**
	* Displays help on the specified topic
	*/
	@Override
	public void help(String topicId) {
		Helper.help(topicId);
	}

	/**
	* Check whether a help on the specified topic is available
	*/
	@Override
	public boolean canHelp(String topicId) {
		return Helper.canHelp(topicId);
	}

	/**
	* Returns the number of maps (territories or different configurations of layers)
	* available in the system (not the number of map windows!)
	*/
	@Override
	public int getMapCount() {
		if (dataKeeper == null)
			return 0;
		return dataKeeper.getMapCount();
	}

	/**
	* Returns the layer manager defining the content of the map with the given
	* number
	*/
	public LayerManager getMapLayerManager(int mapN) {
		if (mapN < 0 || mapN >= getMapCount())
			return null;
		return dataKeeper.getMap(mapN);
	}

	/**
	* Returns the list of identifiers of available layers belonging to the
	* map with the given index. Each layer should have its unique identifier.
	*/
	@Override
	public Vector getLayerList(int mapN) {
		LayerManager lman = getMapLayerManager(mapN);
		if (lman == null || lman.getLayerCount() < 1)
			return null;
		Vector layers = new Vector(lman.getLayerCount(), 5);
		for (int i = 0; i < lman.getLayerCount(); i++) {
			layers.addElement(lman.getGeoLayer(i).getContainerIdentifier());
		}
		return layers;
	}

	/**
	* Finds the given layer of the map with the given number in the index (if it
	* is there) and returns the index item or null.
	*/
	protected IndexItem findIndexItem(int mapN, String layerId) {
		for (int i = 0; i < index.size(); i++) {
			IndexItem item = (IndexItem) index.elementAt(i);
			if (item.mapN == mapN && item.layerId.equals(layerId))
				return item;
		}
		return null;
	}

	/**
	* Returns the GeoLayer with the given identifier
	*/
	public GeoLayer getGeoLayer(int mapN, String layerId) {
		if (mapN < 0 || layerId == null)
			return null;
		//find this layer in the index
		IndexItem item = findIndexItem(mapN, layerId);
		if (item != null && item.layer != null)
			return item.layer;
		LayerManager lman = getMapLayerManager(mapN);
		if (lman == null || lman.getLayerCount() < 1)
			return null;
		for (int i = 0; i < lman.getLayerCount(); i++)
			if (layerId.equals(lman.getGeoLayer(i).getContainerIdentifier())) {
				if (item == null) {
					item = new IndexItem();
					item.mapN = mapN;
					item.layerId = layerId;
					index.addElement(item);
				}
				item.layer = lman.getGeoLayer(i);
				return item.layer;
			}
		return null; //the layer was not found
	}

	/**
	* Returns the name of the layer with the given identifier
	*/
	@Override
	public String getLayerName(int mapN, String layerId) {
		GeoLayer layer = getGeoLayer(mapN, layerId);
		if (layer != null)
			return layer.getName();
		return null;
	}

	/**
	* Returns the type of the layer with the given identifier (area, line, ...)
	*/
	@Override
	public char getLayerType(int mapN, String layerId) {
		GeoLayer layer = getGeoLayer(mapN, layerId);
		if (layer != null)
			return layer.getType();
		return 0;
	}

	/**
	* Returns true if the layer with the given identifier is drawn (visible)
	*/
	@Override
	public boolean getLayerIsDrawn(int mapN, String layerId) {
		GeoLayer layer = getGeoLayer(mapN, layerId);
		if (layer != null)
			return layer.getLayerDrawn();
		return false;
	}

	/**
	* Switches on or off drawing of the specified map layer
	*/
	@Override
	public void setLayerIsDrawn(int mapN, String layerId, boolean value) {
		GeoLayer layer = getGeoLayer(mapN, layerId);
		if (layer != null) {
			layer.setLayerDrawn(value);
		}
	}

	/**
	* Returns type of contents of the layer (see the array layerContentTypes).
	* May return "unknown"!
	* !!! temporarily always returns "unknown"
	*/
	@Override
	public String getLayerContentType(int mapN, String layerId) {
		return SystemManager.layerContentTypes[0];
	}

	/**
	* Returns true if there are thematic data associated with the geographical
	* objects of the given layer.
	*/
	@Override
	public boolean getLayerHasAttrData(int mapN, String layerId) {
		if (dataKeeper == null || mapN < 0 || mapN >= getMapCount() || layerId == null)
			return false;
		LayerManager lman = dataKeeper.getMap(mapN);
		int layerN = lman.getIndexOfLayer(layerId);
		if (layerN < 0)
			return false;
		return lman.getGeoLayer(layerN).hasThematicData();
	}

	/**
	* Replies if the system supports selection of territories, e.g. from a list
	* or a hierarchy. Each territory is represented by an individual map.
	*/
	@Override
	public boolean canSelectTerritory() {
		return false;
	}

	/**
	* Starts the function of territory selection (if supported). Returns the number
	* of the map representing the selected territory
	* !!! temporarily always returns 0
	*/
	@Override
	public int selectTerritory() {
		return 0;
	}

	/**
	* Finds a table referring to the given map layer and returns its number
	*/
	protected int getTableNForLayer(int mapN, String layerId) {
		if (dataKeeper == null || mapN < 0 || mapN >= getMapCount() || layerId == null)
			return -1;
		IndexItem item = findIndexItem(mapN, layerId);
		if (item != null && item.tableN >= 0)
			return item.tableN;
		LayerManager lman = dataKeeper.getMap(mapN);
		int layerN = lman.getIndexOfLayer(layerId);
		if (layerN < 0)
			return -1;
		String setId = lman.getGeoLayer(layerN).getEntitySetIdentifier();
		if (setId == null)
			return -1;
		int tableN = -1;
		for (int i = 0; i < dataKeeper.getTableCount() && tableN < 0; i++)
			if (setId.equals(dataKeeper.getTable(i).getEntitySetIdentifier())) {
				tableN = i;
			}
		if (tableN >= 0) {
			if (item == null) {
				item = new IndexItem();
				item.mapN = mapN;
				item.layerId = layerId;
				index.addElement(item);
			}
			item.tableN = tableN;
		}
		return tableN;
	}

	/**
	* Finds a table referring to the given map layer
	*/
	public AttributeDataPortion getTableForLayer(int mapN, String layerId) {
		if (mapN < 0 || mapN >= getMapCount() || layerId == null)
			return null;
		//find this layer in the index
		IndexItem item = findIndexItem(mapN, layerId);
		if (item != null && item.table != null)
			return item.table;
		int tableN = (item == null) ? -1 : item.tableN;
		if (tableN < 0) {
			tableN = getTableNForLayer(mapN, layerId);
		}
		if (tableN < 0)
			return null;
		AttributeDataPortion dTable = dataKeeper.getTable(tableN);
		if (dTable == null)
			return null;
		if (item == null) {
			item = findIndexItem(mapN, layerId);
		}
		item.table = dTable;
		return dTable;
	}

	/**
	* Returns the list of identifiers of the attributes associated with the
	* specified layer of the map with the given number or null if there are no
	* attribute data available
	*/
	@Override
	public Vector getAttributesForLayer(int mapN, String layerId) {
		AttributeDataPortion dTable = getTableForLayer(mapN, layerId);
		if (dTable == null || dTable.getAttrCount() < 1)
			return null;
		Vector attrs = new Vector(dTable.getAttrCount(), 5);
		for (int i = 0; i < dTable.getAttrCount(); i++) {
			attrs.addElement(dTable.getAttributeId(i));
		}
		return attrs;
	}

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
	@Override
	public Vector selectAttributes(int mapN, String layerId, Vector attrSubset, Vector currentSelection, int minN, int maxN, String prompt) {
		AttributeDataPortion dTable = getTableForLayer(mapN, layerId);
		if (dTable == null || dTable.getAttrCount() < 1)
			return null;
		Vector excludeAttr = null;
		if (attrSubset != null && attrSubset.size() > 0) {
			Vector v = new Vector(attrSubset.size(), 1);
			for (int i = 0; i < attrSubset.size(); i++) {
				Attribute at = dTable.getAttribute((String) attrSubset.elementAt(i));
				if (at != null) {
					if (at.getParent() != null) {
						at = at.getParent();
					}
					if (!v.contains(at.getIdentifier())) {
						v.addElement(at.getIdentifier());
					}
				}
			}
			if (v.size() > 0) {
				excludeAttr = dTable.getTopLevelAttributes();
				for (int i = excludeAttr.size() - 1; i >= 0; i--) {
					Attribute at = (Attribute) excludeAttr.elementAt(i);
					if (v.contains(at.getIdentifier())) {
						excludeAttr.removeElementAt(i);
					} else {
						excludeAttr.setElementAt(at.getIdentifier(), i);
					}
				}
			}
		}
		AttributeChooser attrSel = new AttributeChooser();
		if (attrSel.selectColumns(dTable, currentSelection, excludeAttr, false, prompt, displayProducer.getUI()) != null)
			return attrSel.getSelectedColumnIds();
		return null;
	}

	/**
	* Returns the name of the attribute specified by its identifier, number of
	* the map and identifier of the map layer.
	*/
	@Override
	public String getAttributeName(String attrId, int mapN, String layerId) {
		AttributeDataPortion dTable = getTableForLayer(mapN, layerId);
		if (dTable == null || dTable.getAttrCount() < 1)
			return null;
		int idx = dTable.getAttrIndex(attrId);
		if (idx < 0)
			return null;
		return dTable.getAttributeName(idx);
	}

	/**
	* Returns the type of the attribute specified by its identifier, number of
	* the map and identifier of the map layer. The possible types of
	* attributes are defined in spade.kbase.scenarios.Common
	*/
	@Override
	public char getAttributeType(String attrId, int mapN, String layerId) {
		AttributeDataPortion dTable = getTableForLayer(mapN, layerId);
		if (dTable == null || dTable.getAttrCount() < 1)
			return 'U'; //unknown
		int idx = dTable.getAttrIndex(attrId);
		if (idx < 0)
			return 'U'; //unknown
		return Common.encodeAttrType(dTable.getAttributeType(idx));
	}

	/**
	* Replies whether the attributes listed in the vector are comparable
	*/
	@Override
	public boolean areComparable(Vector attrIds, int mapN, String layerId) {
		AttributeDataPortion dTable = getTableForLayer(mapN, layerId);
		if (dTable == null || dTable.getAttrCount() < 1 || !(dTable instanceof DataTable))
			return false; //unknown
		DataTable dt = (DataTable) dTable;
		SemanticsManager sm = dt.getSemanticsManager();
		if (sm == null)
			return false;
		boolean allowAskUser = sm.questionsAllowed;
		sm.questionsAllowed = false;
		boolean result = sm.areAttributesComparable(attrIds, null);
		sm.questionsAllowed = allowAskUser;
		return result;
	}

	/**
	* Replies whether the attributes listed in the vector are parts of some
	* whole
	*/
	@Override
	public boolean arePartsInWhole(Vector attrIds, int mapN, String layerId) {
		AttributeDataPortion dTable = getTableForLayer(mapN, layerId);
		if (dTable == null || dTable.getAttrCount() < 1 || !(dTable instanceof DataTable))
			return false; //unknown
		DataTable dt = (DataTable) dTable;
		SemanticsManager sm = dt.getSemanticsManager();
		if (sm == null)
			return false;
		boolean allowAskUser = sm.questionsAllowed;
		sm.questionsAllowed = false;
		boolean result = sm.areAttributesIncluded((Vector) attrIds.clone(), null);
		sm.questionsAllowed = allowAskUser;
		return result;
	}

	/**
	* Returns true if the system support pre-selection of data according to
	* a user-specified attribute query.
	* Currently there is no such function in the system.
	*/
	@Override
	public boolean canProcessQueries() {
		return false;
	}

	/**
	* Loads attribute data associated with the given map layer. The argument
	* "attributes" specifies the identifiers of the attributes the values of
	* which must be loaded. Currently ignores the argument "constraints".
	*/
	@Override
	public boolean loadAttributeData(int mapN, String layerId, Vector attributes, Vector constraints) {
		int tblN = getTableNForLayer(mapN, layerId);
		if (tblN < 0)
			return false;
		//the DataKeeper must link the table to the layer
		dataKeeper.linkTableToMapLayer(tblN, mapN, layerId);
		return true;
	}

	/**
	* Replies if the map visualization method with the given identifier is
	* available in the system.
	*/
	@Override
	public boolean isMapVisMethodAvailable(String methodId) {
		return displayProducer != null && displayProducer.getDataMapper() != null && (displayProducer.getDataMapper() instanceof DataMapper) && ((DataMapper) displayProducer.getDataMapper()).isMethodAvailable(methodId);
	}

	/**
	* Visualizes the given attributes on the map by the specified visualization
	* method
	*/
	@Override
	public void showDataOnMap(int mapN, String layerId, Vector attr, String methodId) {
		int tableN = getTableNForLayer(mapN, layerId);
		if (tableN < 0)
			return;
		AttributeDataPortion dTable = dataKeeper.getTable(tableN);
		if (dTable == null)
			return;
		GeoLayer layer = getGeoLayer(mapN, layerId);
		if (!layer.hasThematicData(dTable)) {
			dataKeeper.linkTableToMapLayer(tableN, mapN, layerId);
		}
		displayProducer.displayOnMap(methodId, dTable, (Vector) attr.clone(), layer, displayProducer.getUI().getMapViewer(mapN));
		if (getMapLayerManager(mapN) != null) {
			getMapLayerManager(mapN).activateLayer(layerId);
		}
	}

	/**
	* Erases visualization of thematic data on the specified GeoLayer
	*/
	@Override
	public void eraseDataFromMap(int mapN, String layerId) {
		displayProducer.eraseDataFromMap(getGeoLayer(mapN, layerId), displayProducer.getUI().getMapViewer(mapN));
	}

	/**
	* Duplicates the current map view
	*/
	@Override
	public void duplicateMapView(int mapN) {
		if (displayProducer != null) {
			Component c = (Component) displayProducer.getUI().getMapViewer(mapN).makeCopyAndClear();
			displayProducer.makeWindow(c, c.getName());
		}
	}

	/**
	* Replies whether the specified analysis tool is available. The list of
	* possible tools (functions) can be found in the *.dtd file of the knowledge
	* base on task support.
	*/
	@Override
	public boolean isToolAvailable(String function) {
		if (function == null)
			return false;
		//CalcManager is responsible for the functions "similarity",
		//"similarity_class","rank","evaluate"
		if (calcManager != null && calcManager.isToolAvailable(function))
			return true;
		if (displayProducer != null && displayProducer.isToolAvailable(function))
			return true;
		if (function.equals("zoom") || function.equals("toggle_layers") || function.equals("lookup_values") || function.equals("mark_entities"))
			return true;
		//to be continued when more functions are implemented
		return false;
	}

	/**
	* A tool is "runnable" when it should be specially activated in order to be
	* used, for example, generation of a plot or start of calculations.
	* Tools like "toggle_layers", "lookup_values" etc. do not require start of
	* any method, dialog, or wizard and are therefore not "runnable". The guide
	* can simply recommend to use these functions of the system and display
	* the help, if available
	*/
	@Override
	public boolean isToolRunnable(String function) {
		if (function.equals("zoom") || function.equals("toggle_layers") || function.equals("lookup_values") || function.equals("mark_entities"))
			return false;
		if (calcManager != null && calcManager.isToolAvailable(function))
			return true;
		if (displayProducer != null && displayProducer.isToolAvailable(function))
			return true;
		return false;
	}

	/**
	* Checks whether the specified tool is applicable to the given data
	*/
	@Override
	public boolean isToolApplicable(int mapN, String layerId, Vector attr, String toolId) {
		if (toolId == null || !isToolRunnable(toolId))
			return false;
		int tableN = getTableNForLayer(mapN, layerId);
		if (tableN < 0)
			return false;
		AttributeDataPortion dTable = dataKeeper.getTable(tableN);
		if (dTable == null)
			return false;
		if (calcManager != null && calcManager.isToolAvailable(toolId))
			return calcManager.isToolApplicable(toolId, dTable, attr);
		if (displayProducer != null && displayProducer.isToolAvailable(toolId))
			return displayProducer.isToolApplicable(toolId, dTable, attr);
		return false;
	}

	/**
	* Applies the specified tool to the given data. If fails, generate an error
	* message
	*/
	@Override
	public boolean applyTool(int mapN, String layerId, Vector attr, String toolId) {
		err = null;
		if (toolId == null || !isToolAvailable(toolId)) {
			err = "Unknown tool: " + toolId;
			return false;
		}
		if (getMapLayerManager(mapN) != null) {
			getMapLayerManager(mapN).activateLayer(layerId);
		}
		if (!isToolRunnable(toolId)) {
			Helper.help(toolId);
			return true;
		}
		int tableN = getTableNForLayer(mapN, layerId);
		if (tableN < 0) {
			err = "The table for the layer " + layerId + " is not found!";
			return false;
		}
		AttributeDataPortion dTable = dataKeeper.getTable(tableN);
		if (dTable == null) {
			err = "The table for the layer " + layerId + " is not found!";
			return false;
		}
		if (calcManager != null && calcManager.isToolAvailable(toolId)) {
			Object result = calcManager.applyTool(toolId, dTable, attr, layerId, null);
			if (result == null) {
				err = calcManager.getErrorMessage();
			}
			return result != null;
		}
		if (displayProducer != null && displayProducer.isToolAvailable(toolId)) {
			Object result = displayProducer.applyTool(toolId, dTable, attr, layerId, null);
			if (result == null) {
				err = displayProducer.getErrorMessage();
			}
			return result != null;
		}
		return false;
	}

	/**
	* Returns the error message (e.g. generated when the GuideSupport failed to
	* apply a tool to given data)
	*/
	@Override
	public String getErrorMessage() {
		return err;
	}
}
