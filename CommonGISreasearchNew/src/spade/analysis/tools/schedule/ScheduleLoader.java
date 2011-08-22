package spade.analysis.tools.schedule;

import java.awt.Color;
import java.util.Vector;

import spade.analysis.system.AttrDataReader;
import spade.analysis.system.DataLoader;
import spade.analysis.system.DataReader;
import spade.analysis.system.DataReaderFactory;
import spade.analysis.system.DisplayProducer;
import spade.analysis.system.ESDACore;
import spade.analysis.tools.DataAnalyser;
import spade.analysis.tools.moves.MovementToolRegister;
import spade.lib.basicwin.GetPathDlg;
import spade.lib.util.CopyFile;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.database.ThematicDataItem;
import spade.vis.dataview.ShowRecManager;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;
import spade.vis.dmap.DrawingParameters;
import spade.vis.map.MapViewer;
import spade.vis.mapvis.DataMapper;
import spade.vis.mapvis.Visualizer;
import spade.vis.space.GeoObject;
import spade.vis.space.LayerManager;
import spade.vis.spec.DataSourceSpec;
import spade.vis.spec.LinkDataDescription;
import data_load.DataManager;
import data_load.LayerFromTableGenerator;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 12-Feb-2007
 * Time: 15:17:46
 * Loading of a transportation schedule represented as a table and contained
 * in an ASCII file (csv format or with another delimiter). Assumes that the
 * project directory contains a file orders.descr with the metadata required
 * to interpret the content of the file with the order. Additionally, the user
 * may load a file with time-referenced data about the numbers of different
 * types of items in the source and destination locations at different time
 * moments. The necessary metadata must be specified in a file
 * item_dynamics.descr
 */
public class ScheduleLoader implements DataAnalyser {

	protected ESDACore core = null;
	/**
	 * Path to the directory with the metadata files
	 */
	protected String metadataPath = null;
	/**
	 * The layer with start and end locations of the transportation orders
	 */
	protected DGeoLayer locLayer = null;

	protected int scheduleN = 0;

	protected static int counter = 0;

	/**
	* Returns true when the tool has everything necessary for its operation.
	* A ScheduleLoader returns true if the class LinkLayerBuilder is available.
	*/
	@Override
	public boolean isValid(ESDACore core) {
		String linkBuilderClassName = MovementToolRegister.getToolClassName("build_links");
		if (linkBuilderClassName == null)
			return false;
		try {
			Class.forName(linkBuilderClassName);
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	public void setCore(ESDACore core) {
		this.core = core;
	}

	/**
	 * Sets the path to the directory with the metadata files
	 */
	public void setMetadataPath(String metadataPath) {
		this.metadataPath = metadataPath;
	}

	/**
	* This method constructs and starts the tool. Everything the tool may need
	* for integration with other components of the system can be received
	* from the system's core passed as an argument.
	*/
	@Override
	public void run(ESDACore core) {
		if (core == null || core.getUI() == null)
			return;
		this.core = core;
		if (core.getUI().getCurrentMapViewer() == null || core.getUI().getCurrentMapViewer().getLayerManager() == null) {
			showMessage("No map exists!", true);
			return;
		}
		LayerManager lman = core.getUI().getCurrentMapViewer().getLayerManager();
		if (lman.getLayerCount() < 1) {
			showMessage("No map layers available!", true);
			return;
		}
		if (loadAndVisualiseOrders()) {
			//loadItemMovementData();
		}
	}

	/**
	 * Loads the table with the transportation orders, uses it to construct
	 * a map layer consisting of vectors (source, destination), and visualises
	 * the data. For the loading, calls loadOrders()
	 */
	protected boolean loadAndVisualiseOrders() {
		DGeoLayer layer = loadOrders();
		if (layer == null)
			return false;
		DataTable table = (DataTable) layer.getThematicData();
		DataSourceSpec spec = (DataSourceSpec) table.getDataSource();
		if (spec.drawParm == null) {
			spec.drawParm = new DrawingParameters();
		}
		((DrawingParameters) spec.drawParm).lineColor = Color.getHSBColor((float) Math.random(), (float) Math.min(0.5 + Math.random(), 1.0), (float) Math.min(0.5 + Math.random(), 1.0));
		layer.setDrawingParameters((DrawingParameters) spec.drawParm);

		LayerManager lman = core.getUI().getCurrentMapViewer().getLayerManager();
		lman.activateLayer(layer.getContainerIdentifier());

		if (spec.extraInfo != null && (spec.extraInfo.get("ITEM_CLASS_FIELD_NAME") != null || spec.extraInfo.get("ITEM_NUMBER_FIELD_NAME") != null)) {
			String visMethodId = null;
			Vector attr = new Vector(2, 1);
			int idx = table.findAttrByName((String) spec.extraInfo.get("ITEM_CLASS_FIELD_NAME"));
			if (idx >= 0) {
				attr.addElement(table.getAttributeId(idx));
				visMethodId = "qualitative_colour";
			}
			idx = table.findAttrByName((String) spec.extraInfo.get("ITEM_NUMBER_FIELD_NAME"));
			if (idx >= 0) {
				attr.addElement(table.getAttributeId(idx));
				if (attr.size() == 2) {
					visMethodId = "line_thickness_color";
				} else {
					visMethodId = "line_thickness";
				}
			}
			if (visMethodId != null) {
				DisplayProducer displayProducer = core.getDisplayProducer();
				if (displayProducer != null) {
					DataMapper dataMapper = null;
					if (displayProducer.getDataMapper() != null && (displayProducer.getDataMapper() instanceof DataMapper)) {
						dataMapper = (DataMapper) displayProducer.getDataMapper();
					}
					MapViewer mapView = core.getUI().getCurrentMapViewer();
					if (dataMapper != null && mapView != null) {
						Object vis = dataMapper.constructVisualizer(visMethodId, layer.getType());
						Visualizer visualizer = displayProducer.displayOnMap(vis, visMethodId, table, attr, layer, mapView);
						core.getSupervisor().registerTool(visualizer);
					}
				}
			}
		}
		return true;
	}

	protected String pathToSchedule = null;

	public void setPathToSchedule(String pathToSchedule) {
		this.pathToSchedule = pathToSchedule;
	}

	public String getPathToSchedule() {
		return pathToSchedule;
	}

	/**
	 * Sets the layer with start and end locations of the transportation orders
	 */
	public void setLocLayer(DGeoLayer locLayer) {
		this.locLayer = locLayer;
	}

	/**
	 * Loads the table with the transportation orders and uses it to construct
	 * a map layer consisting of vectors (source, destination).
	 * Returns the layer, which is linked with the table containing the orders.
	 */
	protected DGeoLayer loadOrders() {
		String mfName = "orders.descr";
		if (metadataPath != null) {
			mfName = metadataPath + mfName;
		}
		//load the metadata
		DataSourceSpec spec = DataSourceSpec.readMetadata(mfName, core.getDataLoader().getApplicationPath());
		if (spec == null) {
			showMessage("Could not find the metadata file orders.descr describing" + " a table with transportation orders!", true);
			return null;
		}
		if (spec.descriptors == null || spec.descriptors.size() < 1) {
			showMessage("The required description <LinkData> not found in the metadata!", true);
			return null;
		}
		LinkDataDescription ldd = null;
		for (int i = 0; i < spec.descriptors.size() && ldd == null; i++)
			if (spec.descriptors.elementAt(i) instanceof LinkDataDescription) {
				ldd = (LinkDataDescription) spec.descriptors.elementAt(i);
			}
		if (ldd == null) {
			showMessage("The required description <LinkData> not found in the metadata!", true);
			return null;
		}
		if (locLayer != null) {
			ldd.layerRef = locLayer.getContainerIdentifier();
		}
		if (ldd.layerRef == null) {
			showMessage("No reference to the layer with locations found in the metadata!", true);
			return null;
		}
		if (ldd.souColName == null && ldd.souColIdx < 0) {
			showMessage("The column with source locations is not specified in the metadata!", true);
			return null;
		}
		if (ldd.destColName == null && ldd.destColIdx < 0) {
			showMessage("The column with destination locations is not specified in the metadata!", true);
			return null;
		}
		if (ldd.souTimeColName == null && ldd.souTimeColIdx < 0) {
			showMessage("The column with start times is not specified in the metadata!", true);
			return null;
		}
		if (ldd.destTimeColName == null && ldd.destTimeColIdx < 0) {
			showMessage("The column with end times is not specified in the metadata!", true);
			return null;
		}
		if (ldd.souTimeScheme == null && ldd.destTimeScheme == null) {
			showMessage("The time format is not specified in the metadata!", true);
			return null;
		}
		if (spec.name == null) {
			spec.name = "Transportation orders";
		}
		++scheduleN;
		if (scheduleN > 1) {
			spec.name += " " + scheduleN;
		}

		if (pathToSchedule == null || !CopyFile.fileExists(pathToSchedule)) {
			GetPathDlg fd = new GetPathDlg(core.getUI().getMainFrame(), "Specify the file with the orders");
			fd.setFileMask("*.txt;*.csv");
			fd.show();
			spec.source = fd.getPath();
			if (spec.source == null)
				return null;
			pathToSchedule = spec.source;
		} else {
			spec.source = pathToSchedule;
		}

		DataTable table = loadTableData(spec);
		if (table == null)
			return null;
		if (spec.extraInfo != null) {
			Object vhTypeAName = spec.extraInfo.get("VEHICLE_CLASS_FIELD_NAME");
			if (vhTypeAName != null && (vhTypeAName instanceof String)) {
				int aIdx = table.findAttrByName((String) vhTypeAName);
				if (aIdx >= 0) {
					for (int i = table.getDataItemCount() - 1; i >= 0; i--) {
						String val = table.getAttrValueAsString(aIdx, i);
						if (val == null || val.equals("0")) {
							table.removeDataItem(i);
						}
					}
				}
			}
		}
		DataLoader dataLoader = core.getDataLoader();
		int tableN = dataLoader.addTable(table);

		LayerFromTableGenerator lgen = null;
		String className = MovementToolRegister.getToolClassName("build_links");
		if (className != null) {
			try {
				lgen = (LayerFromTableGenerator) Class.forName(className).newInstance();
			} catch (Exception e) {
			}
		}
		if (lgen == null) {
			showMessage("Failed to generate an instance of " + className + "!", true);
			return null;
		}
		DGeoLayer gl = lgen.buildLayer(table, dataLoader, dataLoader.getCurrentMapN());
		if (gl == null) {
			String msg = lgen.getErrorMessage();
			if (msg != null) {
				showMessage(msg, true);
			}
			return null;
		}
		gl.setContainerIdentifier("schedule" + (counter++));
		dataLoader.addMapLayer(gl, dataLoader.getCurrentMapN());
		dataLoader.setLink(gl, tableN);
		gl.setLinkedToTable(true);
		dataLoader.processTimeReferencedObjectSet(gl);
		dataLoader.processTimeReferencedObjectSet(table);

		ShowRecManager recMan = null;
		if (dataLoader instanceof DataManager) {
			recMan = ((DataManager) dataLoader).getShowRecManager(tableN);
		}
		if (recMan != null) {
			Vector showAttr = new Vector(20, 10);
			if (ldd.souTimeColIdx >= 0) {
				showAttr.addElement(table.getAttributeId(ldd.souTimeColIdx));
			}
			if (ldd.destTimeColIdx >= 0) {
				showAttr.addElement(table.getAttributeId(ldd.destTimeColIdx));
			}
			if (spec.extraInfo != null && !spec.extraInfo.isEmpty()) {
				String orderedKeys[] = { "ITEM_CLASS_FIELD_NAME", "ITEM_NUMBER_FIELD_NAME", "SOURCE_NAME_FIELD_NAME", "DESTIN_NAME_FIELD_NAME", "VEHICLE_CLASS_FIELD_NAME", "VEHICLE_ID_FIELD_NAME", "VEHICLE_HOME_NAME_FIELD_NAME",
						"VEHICLE_HOME_ID_FIELD_NAME" };
				for (String orderedKey : orderedKeys) {
					Object val = spec.extraInfo.get(orderedKey);
					if (val != null && (val instanceof String)) {
						int idx = table.findAttrByName((String) val);
						if (idx >= 0) {
							showAttr.addElement(table.getAttributeId(idx));
						}
					}
				}
			}
			if (showAttr.size() > 0) {
				recMan.setPopupAddAttrs(showAttr);
			}
		}
		return gl;
	}

	/**
	 * Loads a table with data about the movement of the people (or other items)
	 * according to the transportation orders. The data specify how many items
	 * were present in different locations at different time moments. If the
	 * data are loaded successfully, transforms the table into a parameter-
	 * dependent table and visualises the data.
	 */
	protected boolean loadItemMovementData() {
		//load the metadata
		DataSourceSpec spec = DataSourceSpec.readMetadata("item_dynamics.descr", core.getDataLoader().getApplicationPath());
		if (spec == null) {
			showMessage("Could not find the metadata file item_dynamics.descr describing" + " a table with data about the item movement!", true);
			return false;
		}
		if (spec.descriptors == null || spec.descriptors.size() < 1) {
			showMessage("The required description <TimeReference> not found in " + "the metadata!", true);
			return false;
		}
		if (spec.name == null) {
			spec.name = "Item movement";
		}
		if (scheduleN > 1) {
			spec.name += " " + scheduleN;
		}

		GetPathDlg fd = new GetPathDlg(core.getUI().getMainFrame(), "Specify the file with the item movement data");
		fd.setFileMask("*.txt;*.csv");
		fd.show();
		spec.source = fd.getPath();
		if (spec.source == null)
			return false;

		DataTable table = loadTableData(spec);
		if (table == null)
			return false;
		DataLoader dataLoader = core.getDataLoader();
		int tableN = dataLoader.addTable(table);

		if (spec.layerName == null) {
			showMessage("The layer with the locations is not specified in the metadata!", true);
			return false;
		}
		//find the layer with this name (source file name) and link the table to it
		DGeoLayer refLayer = null;
		LayerManager lman = null;
		int mapIdx = -1, layerIdx = -1;
		for (int k = 0; k < dataLoader.getMapCount() && refLayer == null; k++) {
			lman = dataLoader.getMap(k);
			mapIdx = k;
			for (int j = 0; j < lman.getLayerCount() && refLayer == null; j++) {
				DGeoLayer layer = (DGeoLayer) lman.getGeoLayer(j);
				if (layer.getDataSource() != null && (layer.getDataSource() instanceof DataSourceSpec)) {
					DataSourceSpec lSpec = (DataSourceSpec) layer.getDataSource();
					if (lSpec.source == null) {
						continue;
					}
					String s1 = lSpec.source.toUpperCase(), s2 = spec.layerName.toUpperCase();
					if (s1.endsWith(s2)) {
						refLayer = layer;
						layerIdx = j;
					}
				}
			}
		}
		if (refLayer == null) {
			showMessage("The layer with the locations is not found!", true);
			return false;
		}
		//dataLoader.linkTableToMapLayer(tableN,linkLayer);
		//Construct a new layer on the basis of this linkLayer.
		//The new layer will contain only the objects occurring in the table.
		Vector geoObj = new Vector(table.getDataItemCount(), 100);
		for (int i = 0; i < table.getDataItemCount(); i++) {
			ThematicDataItem tit = (ThematicDataItem) table.getDataItem(i);
			String id = tit.getId();
			GeoObject souObj = refLayer.findObjectById(id);
			if (souObj == null || !(souObj instanceof DGeoObject)) {
				continue;
			}
			DGeoObject souDGO = (DGeoObject) souObj, gObj = (DGeoObject) souDGO.makeCopy();
			gObj.setThematicData(tit);
			if (gObj.getLabel() == null && souDGO.getData() != null && souDGO.getData().getName() != null) {
				gObj.setLabel(souDGO.getData().getName());
			}
			if (gObj.getLabel() != null && (tit.getName() == null || tit.getName().equalsIgnoreCase(tit.getId())) && (tit instanceof DataRecord)) {
				((DataRecord) tit).setName(gObj.getLabel());
			}
			geoObj.addElement(gObj);
		}
		if (geoObj.size() < 1) {
			showMessage("The locations specified in the table were not found in the layer " + refLayer.getName() + "!", true);
			return false;
		}
		if (geoObj.size() < table.getDataItemCount()) {
			int diff = table.getDataItemCount() - geoObj.size();
			showMessage(diff + " of " + table.getDataItemCount() + " locations specified in " + "the table were not found in the layer " + refLayer.getName() + "!", true);
		}
		DGeoLayer newLayer = new DGeoLayer();
		newLayer.setType(refLayer.getType());
		newLayer.setName(table.getName());
		newLayer.setGeoObjects(geoObj, true);
		newLayer.setDrawingParameters(refLayer.getDrawingParameters().makeCopy());
		newLayer.setDataSource(null);
		dataLoader.addMapLayer(newLayer, mapIdx);
		dataLoader.setLink(newLayer, tableN);
		newLayer.setLinkedToTable(true);
		refLayer.setLayerDrawn(false);
		return true;
	}

	/**
	 * Reads data from a table described in the given DataSourceSpec
	 */
	public DataTable loadTableData(DataSourceSpec spec) {
		if (spec == null)
			return null;
		DataLoader dataLoader = core.getDataLoader();
		DataReaderFactory rfac = dataLoader.getDataReaderFactory();
		DataReader reader = rfac.getReaderOfFormat("ASCII");
		if (reader == null || !(reader instanceof AttrDataReader)) {
			showMessage("The system configuration has no appropriate data reader!", true);
			return null;
		}
		reader.setUI(core.getUI());
		if (!CopyFile.isAbsolutePath(spec.source)) {
			String applPath = core.getDataKeeper().getApplicationPath();
			if (applPath != null) {
				applPath = CopyFile.getDir(applPath);
				if (applPath != null) {
					spec.source = applPath + spec.source;
				}
			}
		}
		reader.setDataSource(spec);
		if (!reader.loadData(false))
			return null;
		DataTable table = ((AttrDataReader) reader).getAttrData();
		if (table == null) {
			showMessage("Could not get any data from the table " + spec.source + "!", true);
			return null;
		}
		showMessage("Loaded " + table.getDataItemCount() + " records from " + spec.source + "!", false);
		table.completeTableStructure();
		table.setDataSource(spec);
		return table;
	}

	protected void showMessage(String msg, boolean error) {
		if (core != null && core.getUI() != null) {
			core.getUI().showMessage(msg, error);
		} else if (error) {
			System.out.println("!--> " + msg);
		}
	}

}
