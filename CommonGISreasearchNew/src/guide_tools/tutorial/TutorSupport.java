package guide_tools.tutorial;

import java.awt.Frame;
import java.util.Vector;

import spade.analysis.system.DataKeeper;
import spade.analysis.system.DisplayProducer;
import spade.analysis.system.ToolManager;
import spade.vis.database.AttributeDataPortion;
import spade.vis.space.GeoLayer;
import spade.vis.space.LayerManager;

public class TutorSupport {
	protected DataKeeper dataKeeper = null;
	protected DisplayProducer displayProducer = null;
	protected ToolManager calcManager = null;
	protected boolean isApplet = false;
	protected String pathToResultStoringScript = null;
	protected String resultDir = null;

	public void setDataKeeper(DataKeeper dk) {
		dataKeeper = dk;
	}

	public void setDisplayProducer(DisplayProducer dprod) {
		displayProducer = dprod;
	}

	public void setCalculationManager(ToolManager calcManager) {
		this.calcManager = calcManager;
	}

	public void setIsApplet(boolean value) {
		isApplet = value;
	}

	public boolean getIsApplet() {
		return isApplet;
	}

	public void setPathToResultStoringScript(String path) {
		pathToResultStoringScript = path;
	}

	public String getPathToResultStoringScript() {
		return pathToResultStoringScript;
	}

	public void setResultDir(String path) {
		resultDir = path;
		if (resultDir != null && !resultDir.endsWith("/") && !resultDir.endsWith("\\")) {
			resultDir += "/";
		}
	}

	public String getResultDir() {
		return resultDir;
	}

	/**
	* Finds the index of the table with the given identifier
	*/
	protected int getTableIndex(String tblId) {
		if (tblId == null || dataKeeper == null)
			return -1;
		return dataKeeper.getTableIndex(tblId);
	}

	/**
	* Visualizes the given attributes on the map by the specified visualization
	* method
	*/
	public void showDataOnMap(String tableId, Vector attr, String methodId) {
		int tableN = getTableIndex(tableId);
		if (tableN < 0) {
			System.out.println("No table " + tableId + " found!");
			return;
		}
		AttributeDataPortion dTable = dataKeeper.getTable(tableN);
		if (dTable == null) {
			System.out.println("No table " + tableId + " loaded!");
			return;
		}
		int mapN = dataKeeper.getTableMapN(dTable);
		if (mapN < 0) {
			System.out.println("No map for the table " + tableId + " found!");
			return;
		}
		GeoLayer layer = dataKeeper.getTableLayer(dTable);
		if (layer == null) {
			System.out.println("No map layer for the table " + tableId + " found!");
			return;
		}
		if (!layer.hasThematicData(dTable)) {
			dataKeeper.linkTableToMapLayer(tableN, mapN, layer.getContainerIdentifier());
		}
		displayProducer.displayOnMap(methodId, dTable, (Vector) attr.clone(), layer, displayProducer.getUI().getMapViewer(mapN));
		if (dataKeeper.getMap(mapN) != null) {
			dataKeeper.getMap(mapN).activateLayer(layer.getContainerIdentifier());
		}
	}

	/**
	* Erases visualization of thematic data from the specified table
	*/
	public void eraseDataFromMap(String tableId) {
		int tableN = getTableIndex(tableId);
		if (tableN < 0) {
			System.out.println("No table " + tableId + " found!");
			return;
		}
		AttributeDataPortion table = dataKeeper.getTable(tableN);
		int mapN = dataKeeper.getTableMapN(table);
		if (mapN < 0) {
			System.out.println("No map for the table " + table.getName() + " found!");
			return;
		}
		GeoLayer layer = dataKeeper.getTableLayer(table);
		if (layer != null) {
			displayProducer.eraseDataFromMap(layer, displayProducer.getUI().getMapViewer(mapN));
		}
	}

	/**
	* Makes the layer corresponding to the given table active in the map view
	*/
	public void activateLayerWithData(String tableId) {
		int tableN = getTableIndex(tableId);
		if (tableN < 0)
			return;
		AttributeDataPortion table = dataKeeper.getTable(tableN);
		int mapN = dataKeeper.getTableMapN(table);
		if (mapN < 0)
			return;
		GeoLayer layer = dataKeeper.getTableLayer(table);
		if (layer == null)
			return;
		LayerManager lman = dataKeeper.getMap(mapN);
		if (lman == null)
			return;
		lman.activateLayer(layer.getContainerIdentifier());
	}

	/**
	* Applies the specified tool to the given data.
	*/
	public void applyTool(String tableId, Vector attr, String toolId) {
		int tableN = getTableIndex(tableId);
		if (tableN < 0) {
			System.out.println("No table " + tableId + " found!");
			return;
		}
		AttributeDataPortion dTable = dataKeeper.getTable(tableN);
		if (dTable == null) {
			System.out.println("No table " + tableId + " loaded!");
			return;
		}
		String layerId = dataKeeper.getTableLayerId(dTable);
		if (calcManager != null && calcManager.isToolAvailable(toolId)) {
			calcManager.applyTool(toolId, dTable, attr, layerId, null);
		} else if (displayProducer != null && displayProducer.isToolAvailable(toolId)) {
			displayProducer.applyTool(toolId, dTable, attr, layerId, null);
		} else {
			System.out.println("Unknown tool: " + toolId);
		}
	}

	/**
	* Closes all tools that are currently open
	*/
	public void closeAllTools() {
		if (calcManager != null) {
			calcManager.closeAllTools();
		}
		if (displayProducer != null) {
			displayProducer.closeAllTools();
		}
	}

	/**
	* Zooms the map so that the whole territory is visible
	*/
	public void showWholeTerritory() {
		if (displayProducer != null) {
			for (int i = 0; i < dataKeeper.getMapCount(); i++)
				if (displayProducer.getUI().getMapViewer(i) != null) {
					displayProducer.getUI().getMapViewer(i).showWholeTerritory();
				}
		}
	}

	/**
	* Returns the main window of the system (takes it from the displayProducer)
	*/
	public Frame getMainFrame() {
		if (displayProducer == null || displayProducer.getUI() == null)
			return null;
		return displayProducer.getUI().getMainFrame();
	}
}
