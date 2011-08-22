package connection.spin;

import java.awt.Frame;
import java.util.Vector;

import spade.analysis.system.DataLoader;
import spade.analysis.system.ESDACore;
import spade.analysis.system.SystemUI;
import spade.lib.help.Helper;
import spade.lib.util.Parameters;
import spade.vis.action.HighlightListener;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.LayerData;
import spade.vis.database.SpatialEntity;
import spade.vis.dmap.DGridLayer;
import spade.vis.geometry.RasterGeometry;
import spade.vis.geometry.RealRectangle;
import spade.vis.space.GeoLayer;
import spade.vis.space.LayerManager;
import spade.vis.spec.DataSourceSpec;
import ui.DescartesUI;
import ui.MainWin;
import configstart.SysConfigReader;
import core.Core;

/**
* This class connects Descartes to Spin by serving requests from Spin to
* Descartes.
*/
public class DescartesForSpin {
	/**
	* The system core of Descartes that provides access to its main components
	*/
	protected ESDACore core = null;
	/**
	* The main window of Descartes
	*/
	protected MainWin mainWin = null;

	/**
	* Starts Descartes. The argument applFile is the path to a file describing
	* the application to be loaded at start-up. If it is null, Descartes
	* starts with an empty window.
	* Stores the core of Descartes in its internal memory.
	*/
	public Frame runDescartes(boolean runsInBrowser, String applFile) {
		core = new Core();
		Parameters params = core.getSystemSettings();
		params.setParameter("isLocalSystem", (runsInBrowser) ? "false" : "true");
		params.setParameter("runsInsideOtherSystem", "true");
		SysConfigReader scr = new SysConfigReader(params, null);
		if (scr.readConfiguration("system.cnf")) {
			String path = params.getParameterAsString("BROWSER");
			if (path != null) {
				Helper.setPathToBrowser(path);
			}
			path = params.getParameterAsString("PATH_TO_HELP");
			if (path != null) {
				Helper.setPathToHelpFiles(path);
			}
		}
		((Core) core).makeOptionalComponents();
		mainWin = new MainWin();
		DescartesUI ui = new DescartesUI(mainWin);
		mainWin.add(ui, "Center");
		ui.startWork(core, applFile);
		return mainWin;
	}

	/**
	* Sets the system core of Descartes that provides access to its main components
	*/
	public void setDescartesCore(ESDACore core) {
		this.core = core;
	}

	/**
	* Returns the system core of Descartes that provides access to its main components
	*/
	public ESDACore getDescartesCore() {
		return core;
	}

	/**
	* Returns the main window of Descartes
	*/
	public Frame getDescartesMainWindow() {
		return mainWin;
	}

	/**
	* Requests Descartes to load a layer from the specified source (e.g. a
	* database) and add it to the current map. Returns the internal identifier
	* of this layer in Descartes. This identifier can then be used for removing
	* the layer or for controlling visualization of data related to this layer.
	* If null is returned, this means that the layer was not added (due to some
	* errors).
	* If the layer has been successfully added, it automatically becomes active,
	* i.e. highlighting will work in it.
	*/
	public String addLayer(DataSourceSpec tsp) {
		if (core == null || tsp == null)
			return null;
		DataLoader loader = core.getDataLoader();
		if (loader == null)
			return null;
		SystemUI ui = core.getUI();
		if (ui != null && ui.getCurrentMapViewer() != null) {
			//try to take current bounding rectangle from the map
			float bounds[] = ui.getCurrentMapViewer().getMapExtent();
			if (bounds != null) {
				if (tsp.bounds == null) {
					tsp.bounds = new Vector(1, 1);
				} else {
					tsp.bounds.removeAllElements();
				}
				tsp.bounds.addElement(new RealRectangle(bounds));
			}
		}
		int currMapN = loader.getCurrentMapN(), prevLayerCount = 0;
		if (currMapN < 0) {
			currMapN = 0;
		}
		LayerManager lman = loader.getMap(currMapN);
		if (lman != null) {
			prevLayerCount = lman.getLayerCount();
		}
		if (!loader.loadData(tsp))
			return null;
		if (lman == null) {
			lman = loader.getMap(currMapN);
		}
		GeoLayer gl = lman.getGeoLayer(prevLayerCount);
		if (gl == null)
			return null;
		lman.activateLayer(prevLayerCount);
		return gl.getContainerIdentifier();
	}

	/**
	* A convenience method for loading map layers from Oracle databases.
	* Fills itself the data source specification and calls addLayer.
	* url - the URL of the database, e.g. "jdbc:oracle:thin:@spinner:1521:spin"
	* idColName - the name of the column containing object identifiers (if any)
	* nameColName - the name of the column containing object names (if any)
	* geoColName - the name of the column containing geometry (coordinates)
	* columnsToLoad - a list of names of columns to be loaded. If null, all
	*                 columns are loaded
	* bounds - the initial territory extent used as a query constraint in reading
	*          data from the database (makes sense only if there is no map
	*          yet, otherwise the current map extent constrains the amount of
	*          data to be loaded).
	*          This is a float array with 4 elements: x1, y1, x2, y2
	*/
	public String readLayerFromOracle(String url, String user, String password, String tableName, String idColName, String nameColName, String geoColName, Vector columnsToLoad, float bounds[]) {
		if (core == null)
			return null;
		DataSourceSpec dsp = new DataSourceSpec();
		dsp.format = "ORACLE";
		dsp.url = url;
		dsp.user = user;
		dsp.password = password;
		dsp.source = tableName;
		dsp.idFieldName = idColName;
		dsp.nameFieldName = nameColName;
		dsp.geoFieldName = geoColName;
		dsp.columns = columnsToLoad;
		if (bounds != null) {
			dsp.bounds = new Vector(5, 1);
			dsp.bounds.addElement(bounds);
		}
		return addLayer(dsp);
	}

	/**
	* Makes Descartes form a raster layer from the given matrix of values and
	* add it to the current map. Returns the internal identifier
	* of this layer in Descartes. This identifier can then be used for removing
	* the layer or for controlling visualization of data related to this layer.
	* If null is returned, this means that the layer was not added (due to some
	* errors).
	*/
	public String addRaster(int nCols, int nRows, double dX, double dY, double x0, double y0, float values[][], String name) {
		if (core == null || core.getDataLoader() == null)
			return null;
		if (values == null || nCols < 2 || nRows < 2 || dX <= 0 || dY <= 0)
			return null;
		float xmin = (float) x0, ymin = (float) y0, xmax = xmin + (float) dX * nCols, ymax = ymin + (float) dY * nRows;
		RasterGeometry raster = new RasterGeometry(xmin, ymin, xmax, ymax);
		raster.Col = nCols;
		raster.Row = nRows;
		raster.Xbeg = (float) x0;
		raster.Ybeg = (float) y0;
		raster.DX = (float) dX;
		raster.DY = (float) dY;
		raster.ras = values;
		raster.Intr = true;
		raster.recalculateStatistics();
		SpatialEntity spe = new SpatialEntity("1", name);
		spe.setGeometry(raster);
		LayerData ld = new LayerData();
		ld.addItemSimple(spe);
		ld.setBoundingRectangle(xmin, ymin, xmax, ymax);
		ld.setHasAllData(true);
		DGridLayer layer = new DGridLayer();
		layer.receiveSpatialData(ld);
		layer.name = name;
		DataLoader loader = core.getDataLoader();
		loader.addMapLayer(layer, loader.getCurrentMapN());
		return layer.getContainerIdentifier();
	}

	/**
	* Makes the specified layer "active" in the map. Highlighting can only be
	* shown in an active layer.
	*/
	public void activateLayer(String layerId) {
		if (layerId == null || core == null || core.getDataKeeper() == null)
			return;
		int currMapN = 0;
		if (core.getUI() != null) {
			currMapN = core.getUI().getCurrentMapN();
		}
		if (currMapN < 0) {
			currMapN = 0;
		}
		LayerManager lman = core.getDataKeeper().getMap(currMapN);
		if (lman != null) {
			lman.activateLayer(layerId);
		}
	}

	/**
	* Returns the active layer, if exists, otherwise returns null.
	*/
	public GeoLayer getActiveLayer() {
		if (core == null || core.getDataKeeper() == null)
			return null;
		int currMapN = 0;
		if (core.getUI() != null) {
			currMapN = core.getUI().getCurrentMapN();
		}
		if (currMapN < 0) {
			currMapN = 0;
		}
		LayerManager lman = core.getDataKeeper().getMap(currMapN);
		if (lman == null)
			return null;
		return lman.getActiveLayer();
	}

	/**
	* Highlights the specified objects (the identifiers are listed in the vector
	* objects) in the layer with the specified identifier. Activates the layer
	* if necessary. If the vector objects is null or empty, clears current
	* selection in the layer.
	*/
	public void selectObjectsInLayer(Vector objects, String layerId) {
		if (layerId == null || core == null || core.getDataKeeper() == null)
			return;
		int currMapN = 0;
		if (core.getUI() != null) {
			currMapN = core.getUI().getCurrentMapN();
		}
		if (currMapN < 0) {
			currMapN = 0;
		}
		LayerManager lman = core.getDataKeeper().getMap(currMapN);
		if (lman != null) {
			int idx = lman.getIndexOfLayer(layerId);
			if (idx < 0)
				return;
			lman.activateLayer(idx);
			core.selectObjects(this, lman.getGeoLayer(idx).getContainerIdentifier(), objects);
		}
	}

	/**
	* Highlights the specified objects (the identifiers are listed in the vector
	* objects) in the active layer of the current map. If the vector objects is
	* null or empty, clears current selection in the layer.
	*/
	public void selectObjectsInActiveLayer(Vector objects) {
		GeoLayer gl = getActiveLayer();
		if (gl != null) {
			core.getHighlighterForSet(gl.getEntitySetIdentifier()).replaceSelectedObjects(this, objects);
		}
	}

	/**
	* Removes the layer with the given identifier from the current map.
	* If the layer is linked to a table, the table is also removed from the system.
	*/
	public void removeLayer(String layerId) {
		removeLayer(layerId, true);
	}

	/**
	* Removes the layer with the given identifier from the current map.
	* If the layer is linked to a table, and the value of the argument
	* "deleteTable" is true, the table is also removed from the system.
	*/
	public void removeLayer(String layerId, boolean deleteTable) {
		if (layerId == null || core == null)
			return;
		core.removeMapLayer(layerId, deleteTable);
	}

	/**
	* Removes the table with the given identifier from the system.
	*/
	public void removeTable(String tableId) {
		if (tableId == null || core == null)
			return;
		core.removeTable(tableId);
	}

	/**
	* Removes all currently loaded data, clears the map view and closes all
	* additional displays.
	*/
	public void removeAllData() {
		if (core != null) {
			core.removeAllData();
		}
	}

	/**
	* Displays the specified attributes (the identifiers are listed in the vector)
	* attached to the layer with the given identifier. Returns true if success.
	*/
	public boolean displayAttrOnMap(String layerId, Vector attrIds) {
		if (core == null || layerId == null || attrIds == null || attrIds.size() < 1)
			return false;
		if (core.getUI() == null || core.getUI().getCurrentMapViewer() == null)
			return false;
		int currMapN = core.getUI().getCurrentMapN();
		if (currMapN < 0) {
			currMapN = 0;
		}
		LayerManager lman = core.getDataKeeper().getMap(currMapN);
		if (lman == null)
			return false;
		int idx = lman.getIndexOfLayer(layerId);
		if (idx < 0)
			return false;
		GeoLayer gl = lman.getGeoLayer(idx);
		if (!gl.hasThematicData())
			return false;
		AttributeDataPortion table = gl.getThematicData();
		int tblN = core.getDataKeeper().getTableIndex(table.getContainerIdentifier());
		if (tblN < 0)
			return false; //unknown table
		Vector attrs = new Vector(attrIds.size(), 1);
		for (int i = 0; i < attrIds.size(); i++)
			if (table.getAttrIndex((String) attrIds.elementAt(i)) >= 0) {
				attrs.addElement(attrIds.elementAt(i));
			}
		if (attrs.size() < 1)
			return false;
		core.getDisplayProducer().displayOnMap(table, attrs, gl, core.getUI().getCurrentMapViewer());
		return true;
	}

	/**
	* Registers a HighlightListener for the object set with the given identifier.
	* The identifier of the object set correponding to a map layer is returned
	* by the method GeoLayer.getEntitySetIdentifier(). Analogously, the method
	* getEntitySetIdentifier() of a table returns the identifier of the object
	* set corresponding to the table.
	*
	* In order to receive highlighting and/or selection events, a component must
	* implement the interface spade.vis.action.HighlightListener. Highlighting
	* (transient) events are transmitted to the component using the method
	* highlightSetChanged, and selection (durable highlighting) events - using the
	* method selectSetChanged. In these methods the specific reaction of the
	* component to highlighting and selection, respectively, must be done.
	* Please, do not perform in these methods any long computations or other
	* operations taking significant time. Remember that many components may listen
	* to highlighting and selection events, and the highlighter will not be able
	* to proceed with notifying them until your method finishes.
	* Important: the component must remove itself from listeners of highlighting
	* and selection event when it is destroyed (removed from the screen).
	*/
	public void registerHighlightListener(HighlightListener lst, String objSetId) {
		if (core != null && core.getSupervisor() != null) {
			core.getSupervisor().registerHighlightListener(lst, objSetId);
		}
	}

	public void removeHighlightListener(HighlightListener lst, String objSetId) {
		if (core != null && core.getSupervisor() != null) {
			core.getSupervisor().removeHighlightListener(lst, objSetId);
		}
	}

	/**
	* Closes the main window of the system and all additional displays.
	*/
	public void quit() {
		if (mainWin != null) {
			mainWin.dispose();
		}
		mainWin = null;
		core = null;
	}
}
