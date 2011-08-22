package data_load;

import java.awt.Color;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Label;
import java.awt.Panel;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.decision.DecisionSupportFactory;
import spade.analysis.system.AttrDataReader;
import spade.analysis.system.CompositeDataReader;
import spade.analysis.system.DataKeeper;
import spade.analysis.system.DataLoader;
import spade.analysis.system.DataReader;
import spade.analysis.system.DataReaderFactory;
import spade.analysis.system.GeoDataReader;
import spade.analysis.system.MultiLayerReader;
import spade.analysis.system.Supervisor;
import spade.analysis.system.SystemUI;
import spade.analysis.system.URLOpener;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.NotificationLine;
import spade.lib.basicwin.OKDialog;
import spade.lib.lang.Language;
import spade.lib.util.CopyFile;
import spade.lib.util.EntitySetIdManager;
import spade.lib.util.IntArray;
import spade.lib.util.Parameters;
import spade.lib.util.StringUtil;
import spade.lib.util.URLSupport;
import spade.time.manage.TemporalDataManager;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.database.ObjectContainer;
import spade.vis.dataview.ShowRecManager;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DLayerManager;
import spade.vis.dmap.DMovingObject;
import spade.vis.dmap.DrawingParameters;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.RealRectangle;
import spade.vis.map.MapViewer;
import spade.vis.mapvis.AttrColorAssigner;
import spade.vis.space.GeoLayer;
import spade.vis.space.LayerManager;
import spade.vis.spec.DataSourceSpec;
import spade.vis.spec.DecisionSpec;
import core.InstanceCounts;

/**
* Loads data into Descartes and provides access to the data loaded
*/
public class DataManager implements DataKeeper, DataLoader, PropertyChangeListener {
	static ResourceBundle res = Language.getTextResource("data_load.Res");
	/**
	* The class used for managing time-referenced data
	*/
	protected static final String TIME_MANAGER_CLASS = "spade.time.manage.TimeManager";
	/**
	 * List of known classes implementing the interface LayerFromTableGenerator.
	 * Such classes can construct map layers on the basis of tables previously
	 * loaded in the system.
	 */
	public static final String layerFromTableGenerators[] = { "spade.analysis.tools.LinkLayerBuilder", "spade.analysis.tools.moves.MovementLayerBuilder" };
	/**
	* Path to the application (if the application is loaded using a configuration
	* file, @see loadApplication
	*/
	protected String applPath = null;
	/**
	* The register and manager of all available data readers for loading data
	* from various sources
	*/
	protected DataReaderRegister readerReg = new DataReaderRegister();

	protected Vector lManagers = null; //vector of LayerManagers
	protected Vector dataTables = null; //vector of DataTables
	protected Vector recMans = null; //vector of ShowRecManagers
	protected Vector<DataSourceSpec> exportedTableDescriptors = null;
	protected Vector<AttributeDataPortion> exportedTables = null;
	/**
	* The index of the current ("active") map, i.e. layer manager
	*/
	protected int currMapN = -1;
	/**
	* The system UI that can, in particular, display status messages
	*/
	protected SystemUI ui = null;
	/**
	* Used to generate entity set identifiers for map layers and tables
	*/
	protected EntitySetIdManager setIdMan = new EntitySetIdManager();
	/**
	* The supervisor, first of all, provides an access to the system settings
	*/
	protected Supervisor supervisor = null;
	/**
	* A DataLoader can notify registered listeners about new data loaded.
	* To maintain the list of listeners and notify them about changes,
	* it uses a PropertyChangeSupport
	*/
	protected PropertyChangeSupport pcSupport = null;
	/**
	* The component used for managing time-referenced data
	*/
	protected TemporalDataManager timeMan = null;

	/**
	* The supervisor, first of all, provides an access to the system settings
	*/
	public void setSupervisor(Supervisor sup) {
		supervisor = sup;
	}

	/**
	* Sets the system UI (the actual implementation of this interface depends
	* on the current system configuration)
	* The system UI that can, in particular, display status messages
	*/
	public void setUI(SystemUI ui) {
		this.ui = ui;
	}

	/**
	* Sets the index of the current ("active") map, i.e. layer manager
	*/
	public void setCurrentMapN(int idx) {
		currMapN = idx;
	}

	/**
	* Returns the index of the current ("active") map, i.e. layer manager
	*/
	public int getCurrentMapN() {
		return currMapN;
	}

	/**
	* Displays the notification message using the system UI. The second argument
	* indicates whether this is an error message.
	*/
	protected void showMessage(String msg, boolean error) {
		if (ui != null) {
			ui.showMessage(msg, error);
		}
		if (msg != null && error) {
			System.err.println("ERROR: " + msg);
		}
		System.out.println(msg);
	}

	/**
	* Returns the manager of entity set identifiers for map layers and tables
	*/
	public EntitySetIdManager getEntitySetIdManager() {
		return setIdMan;
	}

	/**
	* Returns the "factory" of data readers where all available classes for
	* reading data from various formats are registered
	*/
	public DataReaderFactory getDataReaderFactory() {
		return readerReg;
	}

	/**
	* Loads data according to the given data source specification. Returns
	* true if successfully loaded.
	*/
	public boolean loadData(DataSourceSpec dss) {
		if (dss == null)
			return false;
		if (dss.source == null) {
			//following text:"The source is not specified for "
			showMessage(res.getString("The_source_is_not") + dss.name, true);
			return false;
		}
		if (dss.id == null) {
			dss.id = CopyFile.getName(dss.source);
		}
		if (dss.name == null) {
			dss.name = dss.id;
		}
		if (dss.format == null) {
			dss.format = CopyFile.getExtension(dss.source);
		}
		if (dss.format == null) {
			//following text:"Cannot determine the format of "
			showMessage(res.getString("Cannot_determine_the") + dss.name, true);
			return false;
		}
		DataReader reader = null;
		if (dss.server != null) {//the data must be loaded using the data server
			showMessage(res.getString("Using_the_data_server") + dss.server + res.getString("for_loading") + dss.source, false);
			//first check if the URL is OK
			URL serverURL = null;
			try {
				serverURL = new URL(dss.server);
			} catch (MalformedURLException mfe) {
				//following text:"Invalid URL of the data server: "
				showMessage(res.getString("Invalid_URL_of_the") + dss.server, true);
			}
			if (serverURL != null) {
				try {
					reader = (DataReader) Class.forName("data_load.connect_server.DataServerConnector").newInstance();
				} catch (Exception ex) {
					//following text:"The class DataServerConnector is not available"
					showMessage(res.getString("The_class"), true);
				}
			}
		}
		if (reader == null) {
			//following text:"Trying to construct a reader for the format "
			showMessage(res.getString("Trying_to_construct_a") + dss.format, false);
			reader = readerReg.getReaderOfFormat(dss.format);
			System.out.println(dss.format);
			System.out.println("" + reader);

		}
		if (reader == null) {
			//following text:"No data reader found for the format "
			showMessage(res.getString("No_data_reader_found") + dss.format, true);
			return false;
		}
		reader.setUI(ui);
		if (reader instanceof CompositeDataReader) {
			((CompositeDataReader) reader).setDataReaderFactory(readerReg);
		}
		reader.setDataSource(dss);
		Vector layers = new Vector(10, 5), tables = new Vector(10, 5);
		if (reader instanceof MultiLayerReader) {
			MultiLayerReader mlReader = (MultiLayerReader) reader;
			for (int i = 0; i < mlReader.getLayerCount(); i++) {
				DGeoLayer layer = mlReader.getMapLayer(i);
				if (layer != null) {
					layers.addElement(layer);
					tables.addElement(mlReader.getAttrData(i));
				}
			}
		} else {
			if (reader instanceof GeoDataReader) {
				DGeoLayer layer = ((GeoDataReader) reader).getMapLayer();
				if (layer != null) {
					layers.addElement(layer);
				}
			}
			if (reader instanceof AttrDataReader) {
				DataTable table = ((AttrDataReader) reader).getAttrData();
				if (table != null) {
					tables.addElement(table);
				}
			}
		}
		if (layers.size() < 1 && tables.size() < 1)
			return false;
		if (layers.size() < 1) {
			for (int i = 0; i < tables.size(); i++) {
				DataTable table = (DataTable) tables.elementAt(i);
				if (table == null) {
					continue;
				}
				DataSourceSpec tSpec = (DataSourceSpec) table.getDataSource();
				if (tSpec == null) {
					tSpec = dss;
				}
				if (tSpec.toBuildMapLayer && tSpec.multipleRowsPerObject) {
					continue;
				}
				int tableN = addTable(table);
				//select a layer to link to the table
				if (lManagers != null && lManagers.size() > 0 && tSpec.layerName != null) {
					//find the layer with this name (source file name) and link the table to it
					DGeoLayer linkLayer = null;
					for (int k = 0; k < lManagers.size() && linkLayer == null; k++) {
						LayerManager lman = (LayerManager) lManagers.elementAt(k);
						for (int j = 0; j < lman.getLayerCount() && linkLayer == null; j++) {
							DGeoLayer layer = (DGeoLayer) lman.getGeoLayer(j);
							if (layer.getDataSource() != null && (layer.getDataSource() instanceof DataSourceSpec)) {
								DataSourceSpec lSpec = (DataSourceSpec) layer.getDataSource();
								if (lSpec.source == null) {
									continue;
								}
								String s1 = lSpec.source.toUpperCase(), s2 = tSpec.layerName.toUpperCase();
								if (s1.endsWith(s2)) {
									linkLayer = layer;
								}
							}
						}
					}
					if (linkLayer != null)
						if (table.hasData()) {
							linkTableToMapLayer(tableN, linkLayer);
						} else {
							setLink(linkLayer, tableN);
						}
				}
			}
		} else {
			for (int i = 0; i < layers.size(); i++) {
				DGeoLayer gl = (DGeoLayer) layers.elementAt(i);
				DataSourceSpec lSpec = dss;
				if (gl.getDataSource() != null && (gl.getDataSource() instanceof DataSourceSpec)) {
					lSpec = (DataSourceSpec) gl.getDataSource();
				}
				if (gl.getContainerIdentifier() == null)
					if (lSpec.id != null) {
						gl.setContainerIdentifier(lSpec.id);
					} else {
						if (lSpec.typeName != null) {
							gl.setContainerIdentifier(dss.id + "__" + lSpec.typeName);
						} else {
							gl.setContainerIdentifier(dss.id);
						}
					}
				if (lSpec.name != null) {
					gl.setName(lSpec.name);
				} else if (lSpec.typeName != null) {
					gl.setName(lSpec.typeName);
				}
				if (lSpec.drawParm != null) {
					gl.setDrawingParameters((DrawingParameters) lSpec.drawParm);
				} else if (dss.drawParm != null) {
					gl.setDrawingParameters((DrawingParameters) dss.drawParm);
				} else {
					DrawingParameters dp = new DrawingParameters();
					gl.setDrawingParameters(dp);
					Random random = new Random(System.currentTimeMillis());
					dp.fillColor = Color.getHSBColor(random.nextFloat(), 0.3f, 0.9f);
					dp.lineColor = Color.getHSBColor(random.nextFloat(), random.nextFloat(), random.nextFloat());
				}
				if (lSpec.objType != 0 && lSpec.objType != Geometry.undefined) {
					gl.setType(lSpec.objType);
				}
				addMapLayer(gl, currMapN);
				if (tables.size() > i && tables.elementAt(i) != null) {
					DataTable table = (DataTable) tables.elementAt(i);
					int tableN = addTable(table);
					//register the link between the layer and the table
					setLink(gl, tableN);
				}
			}
		}
		if (tables.size() > 0 && layerFromTableGenerators != null && layerFromTableGenerators.length > 0) {
			for (int i = 0; i < tables.size(); i++) {
				DataTable table = (DataTable) tables.elementAt(i);
				if (table == null) {
					continue;
				}
				DataSourceSpec tSpec = (DataSourceSpec) table.getDataSource();
				if (tSpec == null) {
					continue;
				}
				if (!tSpec.toBuildMapLayer) {
					continue;
				}
				for (String layerFromTableGenerator : layerFromTableGenerators) {
					LayerFromTableGenerator lgen = null;
					try {
						lgen = (LayerFromTableGenerator) Class.forName(layerFromTableGenerator).newInstance();
					} catch (Exception e) {
					}
					if (lgen == null) {
						showMessage("Failed to generate an instance of " + layerFromTableGenerator + "!", true);
						continue;
					}
					if (!lgen.isRelevant(tSpec)) {
						continue;
					}
					DGeoLayer gl = lgen.buildLayer(table, this, currMapN);
					if (gl == null) {
						String msg = lgen.getErrorMessage();
						if (msg != null) {
							showMessage(msg, true);
						}
						continue;
					}
					DataSourceSpec sp = (DataSourceSpec) gl.getDataSource();
					if (sp != null) {
						sp.source = tSpec.source;
					} else {
						gl.setDataSource(tSpec);
					}
					addMapLayer(gl, currMapN);
					if (!tSpec.multipleRowsPerObject) {
						setLink(gl, getTableIndex(table.getContainerIdentifier()));
					}
					if (gl.hasTimeReferences()) {
						processTimeReferencedObjectSet(gl);
					}
				}
			}
		}
		//load tables linked to trajectories
		if (lManagers != null && lManagers.size() > 0 && tables.size() > 0) {
			for (int i = 0; i < tables.size(); i++) {
				DataTable table = (DataTable) tables.elementAt(i);
				if (table == null || table.hasData()) {
					continue;
				}
				DataSourceSpec tSpec = (DataSourceSpec) table.getDataSource();
				if (tSpec == null || tSpec.layerName == null) {
					continue;
				}
				DGeoLayer linkLayer = null;
				for (int k = 0; k < lManagers.size() && linkLayer == null; k++) {
					LayerManager lman = (LayerManager) lManagers.elementAt(k);
					for (int j = 0; j < lman.getLayerCount() && linkLayer == null; j++) {
						DGeoLayer layer = (DGeoLayer) lman.getGeoLayer(j);
						if (layer.getDataSource() != null && (layer.getDataSource() instanceof DataSourceSpec)) {
							DataSourceSpec lSpec = (DataSourceSpec) layer.getDataSource();
							if (lSpec.source == null) {
								continue;
							}
							String s1 = lSpec.source.toUpperCase(), s2 = tSpec.layerName.toUpperCase();
							if (s1.endsWith(s2)) {
								linkLayer = layer;
							}
						}
					}
				}
				if (linkLayer != null && linkLayer.getObjectCount() > 0 && (linkLayer.getObject(0) instanceof DMovingObject)) {
					table.loadData();
				}
			}
		}
		return true;
	}

	/**
	* Adds the table to its list of tables. Returns the index of the table in the
	* list.
	*/
	public int addTable(DataTable table) {
		if (table == null)
			return -1;
		DataSourceSpec spec = (DataSourceSpec) table.getDataSource();
		if (spec == null) {
			spec = new DataSourceSpec();
			spec.source = "_derived";
			int k = 2;
			while (getTableIndex(spec.source) >= 0) {
				spec.source = "_derived_" + (k++);
			}
			spec.name = table.getName();
			table.setDataSource(spec);
		}
		setupTable(spec, table);
		if (dataTables == null) {
			dataTables = new Vector(10, 10);
			recMans = new Vector(10, 10);
		}
		dataTables.addElement(table);
		if (supervisor != null) {
			ShowRecManager srm = new ShowRecManager(supervisor, table);
			recMans.addElement(srm);
			srm.setEnabled(true);
		} else {
			recMans.addElement(null);
		}
		showMessage(res.getString("Added_table") + table.getName(), false);
		int idx = dataTables.size() - 1;
		notifyDataAdded("table", idx);
		if (table.hasTemporalParameter()) {
			processTimeParameterTable(table);
		}
		if (!table.hasData()) {
			table.addPropertyChangeListener(this);
		}
		return idx;
	}

	/**
	* Adds the layer to the map with the given index. Returns the index of
	* the layer in the map. If mapN<0, adds the layer to the current map.
	* If the map does not exist yet, creates it.
	*/
	@Override
	public int addMapLayer(DGeoLayer layer, int mapN) {
		return addMapLayer(layer, mapN, -1);
	}

	/**
	* Adds the layer to the map with the given index. Returns the index of
	* the layer in the map. If mapN<0, adds the layer to the current map.
	* If the map does not exist yet, creates it.
	 * @param coordsAreGeographic - 0 if not, 1 if yes, -1 if unknown
	*/
	@Override
	public int addMapLayer(DGeoLayer layer, int mapN, int coordsAreGeographic) {
		if (layer == null)
			return -1;
		layer.setContainerIdentifier(makeUniqueLayerId(layer.getContainerIdentifier()));
		String setId = setIdMan.getEntitySetIdentifier(layer.getContainerIdentifier());
		setIdMan.linkContainerToEntitySet(layer, setId);
		DLayerManager lman = null;
		boolean mapAdded = false;
		if (lManagers == null || lManagers.size() < 1) {
			lman = new DLayerManager();
			if (coordsAreGeographic >= 0) {
				lman.setCoordsAreGeographic(coordsAreGeographic);
			}
			if (lManagers == null) {
				lManagers = new Vector(1, 1);
			}
			lManagers.addElement(lman);
			currMapN = 0;
			mapAdded = true;
		} else {
			if (currMapN < 0 || currMapN >= lManagers.size()) {
				currMapN = lManagers.size() - 1;
			}
			if (mapN < 0 || mapN >= lManagers.size()) {
				mapN = currMapN;
			}
			lman = (DLayerManager) lManagers.elementAt(mapN);
		}
		lman.addGeoLayer(layer);
		if (layer.getObjectCount() > 0) {
			if (!layer.hasTimeReferences()) {
				layer.tryGetTemporalReferences();
			}
			if (layer.hasTimeReferences()) {
				processTimeReferencedObjectSet(layer);
			}
		} else {
			layer.addPropertyChangeListener(this); //waits for the moment when the
		}
		//layer gets data. Checks if the data have time references. If yes,
		//creates a time filter, etc.
		showMessage(res.getString("Added_layer") + layer.getName(), false);
		if (mapAdded) {
			notifyDataAdded("map", currMapN);
		}
		return lman.getLayerCount() - 1;
	}

	@Override
	public int addMapLayer(DGeoLayer layer, MapViewer mapView) {
		if (layer == null)
			return -1;
		layer.setContainerIdentifier(makeUniqueLayerId(layer.getContainerIdentifier()));
		String setId = setIdMan.getEntitySetIdentifier(layer.getContainerIdentifier());
		setIdMan.linkContainerToEntitySet(layer, setId);
		DLayerManager lman = null;
		boolean mapAdded = false;
		if (mapView == null) {
			if (lManagers == null || lManagers.size() < 1) {
				lman = new DLayerManager();
				if (lManagers == null) {
					lManagers = new Vector(1, 1);
				}
				lManagers.addElement(lman);
				currMapN = 0;
				mapAdded = true;
			} else {
				if (currMapN < 0 || currMapN >= lManagers.size()) {
					currMapN = lManagers.size() - 1;
				}
				lman = (DLayerManager) lManagers.elementAt(currMapN);
			}
		} else {
			lman = (DLayerManager) mapView.getLayerManager();
		}
		lman.addGeoLayer(layer);
		if (layer.getObjectCount() > 0) {
			if (!layer.hasTimeReferences()) {
				layer.tryGetTemporalReferences();
			}
			if (layer.hasTimeReferences()) {
				processTimeReferencedObjectSet(layer);
			}
		} else {
			layer.addPropertyChangeListener(this); //waits for the moment when the
		}
		//layer gets data. Checks if the data have time references. If yes,
		//creates a time filter, etc.
		showMessage(res.getString("Added_layer") + layer.getName(), false);
		if (mapAdded) {
			notifyDataAdded("map", currMapN);
		}
		return lman.getLayerCount() - 1;

	}

	/**
	* Returns the identifier of the table linked to the specified map layer.
	* If there is no such table returns null.
	*/
	public String getLinkedTableId(String layerId) {
		if (layerId == null || getMapCount() < 1)
			return null;
		for (int i = 0; i < getMapCount(); i++) {
			LayerManager lman = getMap(i);
			if (lman == null) {
				continue;
			}
			int idx = lman.getIndexOfLayer(layerId);
			if (idx < 0) {
				continue;
			}
			GeoLayer layer = lman.getGeoLayer(idx);
			if (layer == null) {
				continue;
			}
			AttributeDataPortion table = layer.getThematicData();
			if (table != null)
				return table.getContainerIdentifier();
		}
		return null;
	}

	/**
	* Removes the map layer with the given identifier from the map(s) containing it.
	*/
	public void removeMapLayer(String layerId) {
		if (layerId == null || getMapCount() < 1)
			return;
		for (int i = 0; i < getMapCount(); i++) {
			LayerManager lman = getMap(i);
			if (lman == null) {
				continue;
			}
			int idx = lman.getIndexOfLayer(layerId);
			if (idx >= 0) {
				lman.removeGeoLayer(idx);
			}
		}
	}

	/**
	* Removes the map (layer manager) with the given index
	*/
	public void removeMap(int idx) {
		if (lManagers == null || idx < 0 || idx >= lManagers.size())
			return;
		lManagers.removeElementAt(idx);
	}

	/**
	* Sets a formal link between the given layer and the table with the specified
	* number. The data from the table are not sent to the layer: it is assumed
	* that the layer already has them.
	*/
	public void setLink(DGeoLayer layer, int tblN) {
		if (layer == null || tblN < 0)
			return;
		DataTable tbl = (DataTable) getTable(tblN);
		if (tbl == null)
			return;
		setIdMan.linkContainerToEntitySet(tbl, layer.getEntitySetIdentifier());
		if (!layer.hasThematicData()) {
			layer.setDataTable(tbl);
			layer.setThematicFilter(tbl.getObjectFilter());
		}
		if (tbl.hasData() && !tbl.getSemanticsManager().hasAnySemantics()) {
			tryGetSemantics((DataSourceSpec) tbl.getDataSource(), tbl);
		}
	}

	/**
	* Returns the layer the given table refers to among the layers belonging to
	* the specified layer manager.
	*/
	public GeoLayer getTableLayer(AttributeDataPortion table, LayerManager lman) {
		if (table == null || lman == null)
			return null;
		String setId = table.getEntitySetIdentifier();
		if (setId == null)
			return null;
		for (int j = 0; j < lman.getLayerCount(); j++)
			if (setId.equals(lman.getGeoLayer(j).getEntitySetIdentifier()))
				return lman.getGeoLayer(j);
		return null;
	}

	/**
	* Returns the identifier of the layer the given table refers to
	*/
	public String getTableLayerId(AttributeDataPortion table) {
		if (table == null || lManagers == null || lManagers.size() < 1)
			return null;
		String setId = table.getEntitySetIdentifier();
		if (setId == null)
			return null;
		for (int i = 0; i < getMapCount(); i++) {
			LayerManager lman = getMap(i);
			for (int j = 0; j < lman.getLayerCount(); j++)
				if (setId.equals(lman.getGeoLayer(j).getEntitySetIdentifier()))
					return lman.getGeoLayer(j).getContainerIdentifier();
		}
		return null;
	}

	/**
	* Returns the layer the given table refers to. The layer, if found, belongs
	* to the main map view (not to any of the auxiliary map windows).
	*/
	public GeoLayer getTableLayer(AttributeDataPortion table) {
		if (table == null || lManagers == null || lManagers.size() < 1)
			return null;
		String setId = table.getEntitySetIdentifier();
		if (setId == null)
			return null;
		for (int i = 0; i < getMapCount(); i++) {
			LayerManager lman = getMap(i);
			for (int j = 0; j < lman.getLayerCount(); j++)
				if (setId.equals(lman.getGeoLayer(j).getEntitySetIdentifier()))
					return lman.getGeoLayer(j);
		}
		return null;
	}

	/**
	* Returns the index of the map (layer manager) containing the layer the
	* given table is linked to
	*/
	public int getTableMapN(AttributeDataPortion table) {
		if (table == null || lManagers == null || lManagers.size() < 1)
			return -1;
		String setId = table.getEntitySetIdentifier();
		if (setId == null)
			return -1;
		if (lManagers.size() == 1)
			return 0;
		for (int i = 0; i < getMapCount(); i++) {
			LayerManager lman = getMap(i);
			for (int j = 0; j < lman.getLayerCount(); j++)
				if (setId.equals(lman.getGeoLayer(j).getEntitySetIdentifier()))
					return i;
		}
		return -1;
	}

	/**
	* Checks if the given table identifier is already in use. If so, modifies it
	* in order to make it unique
	*/
	protected String makeUniqueTableId(String id) {
		if (id == null) {
			id = "table0";
		} else {
			id = id.trim();
			if (id.length() < 1) {
				id = "table0";
			}
		}
		if (dataTables == null || dataTables.size() < 1)
			return id;
		//collect all table identifiers
		Vector tids = new Vector(20, 10);
		for (int i = 0; i < dataTables.size(); i++) {
			AttributeDataPortion table = (AttributeDataPortion) dataTables.elementAt(i);
			if (table == null) {
				continue;
			}
			tids.addElement(table.getContainerIdentifier());
		}
		if (tids.size() < 1)
			return id;
		int k = -1, n = 0;
		while (tids.contains(id)) {
			if (k < 0) {
				k = id.length() - 1;
				while (k >= 0 && id.charAt(k) >= '0' && id.charAt(k) <= '9') {
					--k;
				}
				++k;
				if (k >= id.length()) {
					id += "_";
					++k;
				}
			}
			++n;
			id = id.substring(0, k) + String.valueOf(n);
		}
		return id;
	}

	/**
	* Checks if the given layer identifier is already in use. If so, modifies it
	* in order to make it unique
	*/
	protected String makeUniqueLayerId(String id) {
		if (id == null) {
			id = "layer0";
		} else {
			id = id.trim();
			if (id.length() < 1) {
				id = "layer0";
			}
		}
		if (lManagers == null || lManagers.size() < 1)
			return id;
		//collect all layer identifiers
		Vector lids = new Vector(20, 10);
		for (int i = 0; i < lManagers.size(); i++) {
			LayerManager lman = (LayerManager) lManagers.elementAt(i);
			if (lman == null) {
				continue;
			}
			for (int j = 0; j < lman.getLayerCount(); j++) {
				GeoLayer gl = lman.getGeoLayer(j);
				if (gl != null) {
					lids.addElement(gl.getContainerIdentifier());
				}
			}
		}
		if (lids.size() < 1)
			return id;
		int k = -1, n = 0;
		while (lids.contains(id)) {
			if (k < 0) {
				k = id.length() - 1;
				while (k >= 0 && id.charAt(k) >= '0' && id.charAt(k) <= '9') {
					--k;
				}
				++k;
				if (k >= id.length()) {
					id += "_";
					++k;
				}
			}
			++n;
			id = id.substring(0, k) + String.valueOf(n);
		}
		return id;
	}

	/**
	* Assigns unique container and set identifiers to the table. Checks if
	* the table is to be used for decision making (this must be indicated
	* in the dataSourceSpec). If so, constructs a decision supporter for this
	* table. If the table contains a column with URLs, constructs a URLOpener.
	* Tries to load semantics description for this table.
	*/
	protected void setupTable(DataSourceSpec spec, DataTable dTable) {
		if (spec == null || dTable == null)
			return;
		if (spec.id == null && spec.source != null) {
			spec.id = CopyFile.getName(spec.source);
		}
		spec.id = makeUniqueTableId(spec.id);
		dTable.setContainerIdentifier(spec.id);
		if (spec.name == null)
			if (dTable.getName() != null) {
				spec.name = dTable.getName();
			} else if (spec.source != null) {
				spec.name = CopyFile.getNameWithoutExt(CopyFile.getName(spec.source));
			} else {
				spec.name = spec.id;
			}
		if (dTable.getName() == null) {
			dTable.setName(spec.name);
		}
		String setId = dTable.getGenericNameOfEntity();
		if (setId == null) {
			setId = setIdMan.getEntitySetIdentifier(spec.id);
		}
		setIdMan.linkContainerToEntitySet(dTable, setId);
		dTable.setHasTemporalParamInfo(spec.hasTemporalParameter);
		if (spec.useForDecision) {
			System.out.println("Table " + spec.name + " is to be used for decision making");
			DecisionSupportFactory.makeDecisionSupporter((DecisionSpec) spec.decisionInfo, dTable, supervisor);
		}
		if (dTable.hasData()) {
			checkMakeURLOpener(dTable);
		} else {
			dTable.addPropertyChangeListener(this);
		}
		//try to find file with semantics
		if (dTable.hasData()) {
			tryGetSemantics(spec, dTable);
		}
	}

	/**
	* Checks if the given table has a field with URLs. If yes, creates a URLOpener
	* that will open the URLs.
	*/
	protected void checkMakeURLOpener(DataTable dTable) {
		if (dTable.getURLOpener() == null && dTable.hasData() && dTable.findAttrByName("URL") >= 0) {
			try {
				URLOpener urlOpen = (URLOpener) Class.forName("core.URLOpenerImpl").newInstance();
				if (urlOpen != null) {
					urlOpen.setTable(dTable);
					urlOpen.setSupervisor(supervisor);
					dTable.setURLOpener(urlOpen);
				}
			} catch (Exception e) {
			}
		}
	}

	/**
	* When a table got data from its supplier, checks if the table has a field
	* with URLs. If yes, creates a URLOpener that will open the URLs.
	*/
	public void propertyChange(PropertyChangeEvent e) {
		if ((e.getSource() instanceof DataTable) && e.getPropertyName().equals("got_data")) {
			DataTable dTable = (DataTable) e.getSource();
			//System.out.println("Table "+dTable.getName()+" got data!");
			checkMakeURLOpener(dTable);
			//dTable.removePropertyChangeListener(this);
		} else if ((e.getSource() instanceof AttributeDataPortion) && e.getPropertyName().equals("structure_complete")) {
			AttributeDataPortion table = (AttributeDataPortion) e.getSource();
			table.removePropertyChangeListener(this);
			if (table.hasTimeReferences()) {
				GeoLayer layer = getTableLayer(table);
				if (layer != null && !layer.hasTimeReferences()) {
					layer.addPropertyChangeListener(this); //listen when this layer gets time references from the table
				}
			}
			DataTable dTable = (DataTable) table;
			tryGetSemantics((DataSourceSpec) dTable.getDataSource(), dTable);
			if (supervisor != null) {
				AttrColorAssigner.assignColors(table, supervisor.getAttrColorHandler());
			}
			if (table.hasTemporalParameter()) {
				processTimeParameterTable(table);
			} else if (dTable.isTimeReferenced()) {
				processTimeReferencedObjectSet(dTable);
			}
		} else if ((e.getSource() instanceof ObjectContainer) && (e.getPropertyName().equals("got_data") || e.getPropertyName().equals("got_time_references"))) {
			ObjectContainer oCont = (ObjectContainer) e.getSource();
			oCont.removePropertyChangeListener(this);
			if (oCont.hasTimeReferences()) {
				processTimeReferencedObjectSet(oCont);
			} else if (oCont instanceof DGeoLayer) {
				DGeoLayer layer = (DGeoLayer) oCont;
				layer.tryGetTemporalReferences();
				if (layer.hasTimeReferences()) {
					processTimeReferencedObjectSet(oCont);
				}
			}
		}
	}

	/**
	* Tries to load semantic knowledge about the given table from a file
	*/
	protected void tryGetSemantics(DataSourceSpec spec, DataTable dTable) {
		if (spec == null || dTable == null || dTable.getSemanticsManager().hasAnySemantics())
			return;
		String fname = dTable.getSemanticsManager().getPathToSemantics();
		if (fname == null) {
			fname = getSemFileName(spec);
		}
		if (fname == null) {
			DGeoLayer layer = (DGeoLayer) getTableLayer(dTable);
			if (layer != null && layer.getDataSource() != null && (layer.getDataSource() instanceof DataSourceSpec)) {
				fname = getSemFileName((DataSourceSpec) layer.getDataSource());
			}
		}
		//System.out.println("Semantics file name = "+fname);
		if (fname == null)
			return;
		int idx = fname.indexOf(':');
		boolean isURL = false;
		if (idx > 0) {
			String pref = fname.substring(0, idx);
			if (pref.equalsIgnoreCase("HTTP") || pref.equalsIgnoreCase("FILE")) {
				isURL = true;
			}
		}
		if (!isURL && !supervisor.getSystemSettings().checkParameterValue("isLocalSystem", "true"))
			return; //an applet may access only URLs but not files
		BufferedReader reader = null;
		try {
			if (isURL) { //try to access the source as a URL
				URL url = new URL(fname);
				reader = new BufferedReader(new InputStreamReader(url.openStream()));
			} else {
				reader = new BufferedReader(new FileReader(fname));
			}
		} catch (IOException ioe) {
			return;
		}
		if (reader == null)
			return;
		dTable.getSemanticsManager().setPathToSemantics(fname);
		//System.out.println("Read semantics from "+fname);
		dTable.getSemanticsManager().readSemanticsFromFile(reader);
	}

	/**
	* Generates the name for the file with semantic knowledge about the table.
	*/
	protected String getSemFileName(DataSourceSpec spec) {
		if (spec == null || spec.source == null)
			return null;
		String dir = null;
		if (spec.format == null || !spec.format.equals("JDBC")) {
			dir = CopyFile.getDir(spec.source);
		}
		if ((dir == null || dir.length() < 2) && applPath != null) {
			dir = CopyFile.getDir(applPath);
		}
		if (dir == null || dir.length() < 2)
			return null;
		String fname = CopyFile.getNameWithoutExt(spec.source);
		if (fname == null || fname.length() < 1) {
			fname = spec.name;
		}
		if (fname != null && fname.length() > 0)
			return dir + fname + ".sem";
		return null;
	}

	/**
	* Sets the path to the application
	*/
	public void setApplicationPath(String path) {
		applPath = path;
	}

	/**
	* Returns the path to the application
	*/
	public String getApplicationPath() {
		return applPath;
	}

	/**
	* Loads application data according to the specification contained in the
	* given file. If the URL of the data server is specified, uses the data
	* server for loading the application.
	*/
	public boolean loadApplication(String path, String dataServerURL) {
		if (path == null)
			return false;
		ApplData applData = null;
		if (dataServerURL != null) {
			if (!dataServerURL.endsWith("/")) {
				dataServerURL += "/";
			}
			//start the servlet for loading the application description
			//following text:"Connecting to the data server...
			showMessage(res.getString("Connecting_to_the"), false);
			URL url = null;
			try {
				url = new URL(dataServerURL + "ApplLoader?Application=\"" + path + "\"");
			} catch (MalformedURLException mfe) {
				showMessage(mfe.toString(), true);
			}
			if (url != null) {
				InputStream in = null;
				//following text:"Getting application description from "
				showMessage(res.getString("Getting_application") + url.toString(), false);
				try {
					in = url.openStream();
				} catch (IOException ioe) {
					showMessage(ioe.toString(), true);
				}
				if (in != null) {
					ObjectInputStream oin = null;
					try {
						oin = new ObjectInputStream(in);
					} catch (IOException ioe) {
						showMessage(ioe.toString(), true);
					}
					if (oin != null) {
						while (true) {
							try {
								Object obj = oin.readObject();
								if (obj == null) {
									break;
								}
								if (obj instanceof String) {
									String str = (String) obj;
									if (str.startsWith("ERROR:")) {
										showMessage(res.getString("Data_server_error_") + str.substring(6).trim(), true);
									} else {
										System.out.println(str);
									}
								} else if (obj instanceof ApplData) {
									applData = (ApplData) obj;
									applData.dataServerURL = dataServerURL;
									int nspec = applData.getDataSpecCount();
									//following text:"Got application description from the data server: "
									showMessage(res.getString("Got_application") + nspec + res.getString("data_specifications"), false);
									for (int i = 0; i < nspec; i++)
										if (applData.getDataSpecification(i).server == null) {
											applData.getDataSpecification(i).server = dataServerURL;
										}
								}
							} catch (ClassNotFoundException cnfe) {
								//following text:"Reading data server output: "
								showMessage(res.getString("Reading_data_server") + cnfe.toString(), true);
							} catch (EOFException eof) {
								break;
							} catch (IOException ioe) {
								showMessage(ioe.toString(), true);
								break;
							}
						}
					}
					try {
						in.close();
					} catch (IOException ioe) {
					}
				}
			}
		}
		if (applData == null) {
			URL docBase = (URL) supervisor.getSystemSettings().getParameter("DocumentBase");
			if (docBase != null) {
				path = URLSupport.makeURLbyPath(docBase, path).toString();
			}
			System.out.println("Transformed application path =[" + path + "]");
			ApplDescrReader adr = new ApplDescrReader();
			NotificationLine lStatus = null;
			if (ui != null) {
				lStatus = ui.getStatusLine();
			}
			if (!path.startsWith("HTTP:") && !path.startsWith("http:") && !path.startsWith("FILE:") && !path.startsWith("file:")) {
				try {
					File applFile = new File(path);
					String str = applFile.getAbsolutePath();
					if (str != null) {
						path = str;
					}
				} catch (Exception e) {
				}
			}
			;
			applData = adr.readMapDescription(path, lStatus, supervisor.getSystemSettings());
		}
		if (applData == null)
			return false;
		applPath = path;
		System.out.println("applPath=" + applPath);
		return loadApplication(applData);
	}

	/**
	* Loads application data according to the given specification
	*/
	public boolean loadApplication(ApplData ad) {
		if (ad == null)
			return false;
		InstanceCounts.reset();
		Parameters sysParam = supervisor.getSystemSettings();
		sysParam.setParameter("APPL_NAME", ad.applName);
		sysParam.setParameter("TERR_NAME", ad.terrName);
		sysParam.setParameter("DATA_SERVER", ad.dataServerURL);

		if (ad.pathToTaskKB != null) {
			sysParam.setParameter("TASK_KBASE", ad.pathToTaskKB);
		}
		sysParam.setParameter("TUTORIAL", ad.pathToTutorial);
		if (ad.applBgColor != null) {
			sysParam.setParameter("APPL_BGCOLOR", ad.applBgColor);
		} else {
			sysParam.setParameter("APPL_BGCOLOR", null);
		}

		if (ad.oraSpaConn != null) {
			sysParam.setParameter("ORACLE_SPATIAL_CONNECT", ad.oraSpaConn);
		} else {
			sysParam.setParameter("ORACLE_SPATIAL_CONNECT", null);
		}

		if (!Float.isNaN(ad.minScaleDenominator)) {
			sysParam.setParameter("MIN_SCALE_DENOMINATOR", new Float(ad.minScaleDenominator));
		} else {
			sysParam.setParameter("MIN_SCALE_DENOMINATOR", null);
		}

// for GIMMI area selector
		if (ad.partOfAttrName != null && ad.partOfAttrName.length() > 0) {
			sysParam.setParameter("PART_OF_ATTR_NAME", ad.partOfAttrName);
		}
		if (ad.levelDownAttrName != null && ad.levelDownAttrName.length() > 0) {
			sysParam.setParameter("LEVEL_DOWN_ATTR_NAME", ad.levelDownAttrName);
		}

//ID
		if (ad.about_project.length() < 1) {
			ad.about_project = null;
		}
		sysParam.setParameter("ABOUT_PROJECT", ad.about_project);
//~ID
		sysParam.setParameter("OBJECT_SELECTION_IN", ad.selectObjectsIn);
		if (ad.getDataSpecCount() < 1)
			return false;
		DLayerManager lman = new DLayerManager();
		if (ad.applBgColor != null) {
			lman.terrBgColor = ad.applBgColor;
		}
//ID
		lman.show_nobjects = ad.show_nobjects;
//~ID
		if (lManagers == null) {
			lManagers = new Vector(1, 1);
		}
		lManagers.addElement(lman);
		currMapN = lManagers.size() - 1;
		int nTables = getTableCount();
		for (int i = 0; i < ad.getDataSpecCount(); i++) {
			loadData(ad.getDataSpecification(i));
		}

		if (lman.getLayerCount() < 1) {
			lManagers.removeElementAt(currMapN);
			currMapN = lManagers.size() - 1;
		} else {
			if (ad.terrName != null) {
				lman.terrName = ad.terrName;
			} else if (ad.applName != null) {
				lman.terrName = ad.applName;
			}
			if (ad.extent != null) {
				lman.initialExtent = ad.extent;
			}
			if (ad.fullExtent != null) {
				lman.fullExtent = ad.fullExtent;
			}
			lman.user_factor = ad.user_factor;
			lman.setUserUnit(ad.user_unit);
			if (ad.coordsAreGeographic >= 0) {
				lman.setGeographic(ad.coordsAreGeographic > 0);
			} else {
				lman.setGeographic(lman.isGeographic());
			}
//ID
			lman.show_legend = ad.show_legend;
			lman.percent_of_legend = ad.percent_of_legend;
			lman.show_terrname = ad.show_terrname;
			lman.show_bgcolor = ad.show_bgcolor;
			lman.show_scale = ad.show_scale;
//~ID
			lman.show_manipulator = ad.show_manipulator;
			lman.percent_of_manipulator = ad.percent_of_manipulator;
			if (ad.pathOSMTilesIndex != null) {
				lman.setPathOSMTilesIndex(ad.pathOSMTilesIndex);
			}
			lman.activateLayer(lman.getLayerCount() - 1);
			notifyDataAdded("map", currMapN);
//ID
// It was desided, that application parameters should affect only the layer corresponding to the first table
			if (recMans != null) {
				if (ad.show_persistent) {
					ui.placeComponent(((ShowRecManager) recMans.elementAt(0)).getPersistentRecordShow());
				}
				((ShowRecManager) recMans.elementAt(0)).setEnabled(ad.show_tooltip);
			}
//~ID
		}
		sysParam.setParameter("DATAPIPE", ad.dataPipeSpecs);
		if (ad.dataPipeSpecs != null && ad.dataPipeSpecs.size() > 0) {
			sysParam.setParameter("AUTOSTART_TOOL", "data_pipe");
		} else {
			sysParam.setParameter("AUTOSTART_TOOL", null);
		}
		return lman.getLayerCount() > 0 || getTableCount() > nTables;
	}

	/**
	* Returns the number of maps (layer managers) loaded
	*/
	public int getMapCount() {
		if (lManagers == null)
			return 0;
		return lManagers.size();
	}

	/**
	* Returns the map (layer manager) with the given index
	*/
	public LayerManager getMap(int idx) {
		if (idx < 0 || idx >= getMapCount())
			return null;
		return (LayerManager) lManagers.elementAt(idx);
	}

	/**
	* Returns the number of tables loaded
	*/
	public int getTableCount() {
		if (dataTables == null)
			return 0;
		return dataTables.size();
	}

	/**
	* Returns the table with the given index
	*/
	public AttributeDataPortion getTable(int idx) {
		if (idx < 0 || idx >= getTableCount())
			return null;
		return (AttributeDataPortion) dataTables.elementAt(idx);
	}

	/**
	* Returns the index of the table with the given identifier or -1 if there
	* is no such table
	*/
	public int getTableIndex(String tableId) {
		if (tableId == null || dataTables == null)
			return -1;
		for (int i = 0; i < getTableCount(); i++)
			if (tableId.equals(getTable(i).getContainerIdentifier()))
				return i;
		return -1;
	}

	/**
	* Returns the ShowRecManager corresponding to the table with the given
	* index
	*/
	public ShowRecManager getShowRecManager(int idx) {
		if (recMans == null || idx < 0 || idx >= recMans.size())
			return null;
		return (ShowRecManager) recMans.elementAt(idx);
	}

	/**
	* Removes the table from the list of tables
	*/
	public void removeTable(String tableId) {
		removeTable(getTableIndex(tableId));
	}

	public void removeTable(int idx) {
		if (idx < 0 || dataTables == null || dataTables.size() <= idx)
			return;
		AttributeDataPortion table = getTable(idx);
		table.destroy();
		setIdMan.removeContainer(table.getContainerIdentifier());
		dataTables.removeElementAt(idx);
		ShowRecManager srm = (ShowRecManager) recMans.elementAt(idx);
		if (srm != null) {
			srm.destroy();
		}
		recMans.removeElementAt(idx);
	}

	/**
	* Links the table to the map layer. The table is specified by its index in the
	* list of tables. The layer is specified by the index of the map in the
	* list of maps and the identifier of a layer of this map.
	*/
	public GeoLayer linkTableToMapLayer(int tblN, int mapN, String layerId) {
		if (tblN < 0 || tblN >= getTableCount())
			return null;
		if (mapN < 0 || mapN >= getMapCount())
			return null;
		if (layerId == null)
			return null;
		LayerManager lman = getMap(mapN);
		if (lman == null)
			return null;
		int idx = lman.getIndexOfLayer(layerId);
		if (idx < 0) {
			//following text:"Linking table to map layer: no layer "
			//following text:" in the map!"
			showMessage(res.getString("Linking_table_to_map") + layerId + res.getString("in_the_map_"), true);
			return null;
		}
		GeoLayer rightLayer = linkTableToMapLayer(tblN, lman.getGeoLayer(idx));
		lman.activateLayer(idx);
		return rightLayer;
	}

	/**
	* Links the table to the map layer. The table is specified by its index in the
	* list of tables.
	*/
	public GeoLayer linkTableToMapLayer(int tblN, GeoLayer rightLayer) {
		if (rightLayer == null)
			return null;
		AttributeDataPortion dtab = getTable(tblN);
		if (dtab == null)
			return null;
		if (!rightLayer.getLayerDrawn()) {
			rightLayer.setLayerDrawn(true);
		}
		boolean dataReceived = false;
		if (rightLayer.hasThematicData(dtab))
			if (rightLayer.getEntitySetIdentifier() != null && dtab.getEntitySetIdentifier() != null && rightLayer.getEntitySetIdentifier().equals(dtab.getEntitySetIdentifier()))
				return rightLayer; //already linked
			else {
				dataReceived = true;
			}
		else {
			//following text:"Linking table to map layer
			showMessage(res.getString("Linking_table_to_map1") + rightLayer.getName(), false);
			System.out.println("Linking table to map layer " + rightLayer.getName() + " with " + rightLayer.getObjectCount() + " objects");
			int nlinked = 0;
			if (rightLayer.hasThematicData()) {
				nlinked = rightLayer.countOverlap(dtab);
			} else {
				nlinked = rightLayer.receiveThematicData(dtab);
				dataReceived = true;
			}
			if (nlinked == 0) {
				//following text:"Linking table to map layer
				//following text:"no correspondence between identifiers!"
				if (ui != null) {
					ui.notifyProcessState(res.getString("Linking_table_to_map2"), res.getString("no_correspondence"), true);
				}
				return null;
			}
			if (nlinked < rightLayer.getObjectCount()) {
				if (ui != null) {
					ui.notifyProcessState(
							res.getString("Linking_table_to_map2"),
							res.getString("Only") + nlinked + res.getString("out_of") + rightLayer.getObjectCount() + res.getString("objects1") + ((dataReceived) ? res.getString("have_been_linked") : res.getString("correspond"))
									+ res.getString("to_table_rows_"), false);
				}
			} else if (ui != null) {
				ui.notifyProcessState(res.getString("Linking_table_to_map2"),
						res.getString("All") + nlinked + res.getString("objects1") + ((dataReceived) ? res.getString("have_been_linked") : res.getString("correspond")) + res.getString("to_table_rows_"), false);
			}
		}
		setIdMan.linkContainerToEntitySet(dtab, rightLayer.getEntitySetIdentifier());
		rightLayer.setThematicFilter(dtab.getObjectFilter());
		return rightLayer;
	}

	/**
	* Registeres a listener of changes of the set of available tables and maps.
	* The listener must implement the PropertyChangeListener interface.
	*/
	public synchronized void addPropertyChangeListener(PropertyChangeListener l) {
		if (l == null)
			return;
		if (pcSupport == null) {
			pcSupport = new PropertyChangeSupport(this);
		}
		pcSupport.addPropertyChangeListener(l);
	}

	/**
	* Unregisteres a listener of changes of the set of data.
	*/
	public synchronized void removePropertyChangeListener(PropertyChangeListener l) {
		if (l == null || pcSupport == null)
			return;
		pcSupport.removePropertyChangeListener(l);
	}

	/**
	* An internal method used to notify all the listeners that new data
	* have been loaded. The argument "what" may be "table" or "map".
	* The argument "idx" is the index of the table or the map in the list of
	* tables or maps
	*/
	protected void notifyDataAdded(String what, int idx) {
		if (pcSupport == null)
			return;
		pcSupport.firePropertyChange(what, null, new Integer(idx));
	}

	/**
	* Properly finishes the work. In particular, tries to store the semantic
	* knowledge collected during the session.
	*/
	public void finish() {
		storeSemantics();
	}

	/**
	* Stores on the disk semantic knowledge about tables got during the session
	* of the program's work
	*/
	public void storeSemantics() {
		if (dataTables == null)
			return;
		if (supervisor != null && supervisor.getSystemSettings() != null && !supervisor.getSystemSettings().checkParameterValue("isLocalSystem", "true"))
			return;
		for (int i = 0; i < dataTables.size(); i++)
			if (dataTables.elementAt(i) != null && (dataTables.elementAt(i) instanceof DataTable)) {
				DataTable dt = (DataTable) dataTables.elementAt(i);
				if (dt.getSemanticsManager().getWasAnythingChanged()) {
					String fname = dt.getSemanticsManager().getPathToSemantics();
					if (fname == null) {
						fname = getSemFileName((DataSourceSpec) dt.getDataSource());
					}
					//System.out.println("Semantics file name = "+fname);
					if (fname != null) {
						try {
							DataOutputStream dos = new DataOutputStream(new FileOutputStream(fname));
							if (dos != null) {
								System.out.println("Save semantics to " + fname);
								dt.getSemanticsManager().writeSemanticsToFile(dos);
								dt.getSemanticsManager().setPathToSemantics(fname);
							}
						} catch (IOException ioe) {
						}
					}
				}
			}
	}

	/**
	* Writes the application description into an *.app file.
	*/
	public void saveApplication() {
		if (supervisor != null && supervisor.getSystemSettings() != null && !supervisor.getSystemSettings().checkParameterValue("isLocalSystem", "true")) {
			//following text:"Saving is only possible in a local variant of the system"
			showMessage(res.getString("Saving_is_only"), true);
			return;
		}
		if (getTableCount() < 1 && getMapCount() < 1) {
			showMessage(res.getString("No_data_loaded_"), true);
			return;
		}
		Frame frame = null;
		if (ui != null) {
			frame = ui.getMainFrame();
		}
		if (frame == null) {
			frame = CManager.getAnyFrame();
		}
		String applName = supervisor.getSystemSettings().getParameterAsString("APPL_NAME"), terrName = supervisor.getSystemSettings().getParameterAsString("TERR_NAME");
		String dataServerURL = supervisor.getSystemSettings().getParameterAsString("DATA_SERVER");

		//MO~ is not yet implemented

		//exportBeforeSaveProject();

		//~MO

		//ask the user about the name of the application
		ApplInfoPanel p = new ApplInfoPanel(applName, terrName, dataServerURL);
		OKDialog okd = new OKDialog(frame, res.getString("Save_project"), true);
		okd.addContent(p);
		okd.show();
		if (okd.wasCancelled())
			return;
		applName = p.getApplName();
		terrName = p.getTerrName();
		dataServerURL = p.getServerURL();
		if (applName != null) {
			supervisor.getSystemSettings().setParameter("APPL_NAME", applName);
		}
		if (terrName != null) {
			supervisor.getSystemSettings().setParameter("TERR_NAME", terrName);
		}
		if (dataServerURL != null) {
			supervisor.getSystemSettings().setParameter("DATA_SERVER", dataServerURL);
		}

		//following text:"Save project in a file"
		FileDialog fd = new FileDialog(frame, res.getString("Save_project_in_a"), FileDialog.SAVE);
		if (applPath != null) {
			fd.setDirectory(CopyFile.getDir(applPath));
		}
		fd.setFile("*.app");
		fd.show();
		String fname = fd.getFile(), dir = fd.getDirectory();
		if (fname == null)
			return;
		if (fname.lastIndexOf(".") < 0) {
			fname += ".app";
		} else if (fname.lastIndexOf(".") == fname.length() - 1) {
			fname += "app";
		}
		applPath = dir + fname;
		ApplData ad = new ApplData();
		ad.applName = applName;
		ad.terrName = terrName;
		ad.dataServerURL = dataServerURL;
		ad.pathToTaskKB = supervisor.getSystemSettings().getParameterAsString("TASK_KBASE");
		ad.pathToTutorial = supervisor.getSystemSettings().getParameterAsString("TUTORIAL");
		ad.applBgColor = (Color) supervisor.getSystemSettings().getParameter("APPL_BGCOLOR");
		Object obj = supervisor.getSystemSettings().getParameter("DATAPIPE");
		if (obj != null && (obj instanceof Vector)) {
			ad.dataPipeSpecs = (Vector) obj;
		}
		DLayerManager lman = (DLayerManager) getMap(currMapN);
		if (lman != null) {
			ad.user_factor = lman.user_factor;
			ad.user_unit = lman.getUserUnit();
			ad.coordsAreGeographic = (lman.isGeographic()) ? 1 : 0;
			ad.pathOSMTilesIndex = lman.getPathOSMTilesIndex();
			if (lman.terrBgColor != null) {
				ad.applBgColor = lman.terrBgColor;
				supervisor.getSystemSettings().setParameter("APPL_BGCOLOR", ad.applBgColor);
			}
			float cr[] = null;
			if (ui != null && ui.getCurrentMapViewer() != null) {
				cr = ui.getCurrentMapViewer().getMapExtent();
			}
			if (cr != null) {
				RealRectangle wr = lman.getWholeTerritoryBounds();
				if (wr != null && cr[0] <= wr.rx1 && cr[2] >= wr.rx2 && cr[1] <= wr.ry1 && cr[3] >= wr.ry2) {
					cr = null;
				}
			}
			if (cr != null) {
				ad.extent = cr;
			}
			ad.fullExtent = lman.fullExtent;
			for (int i = 0; i < lman.getLayerCount(); i++) {
				DGeoLayer layer = (DGeoLayer) lman.getGeoLayer(i);
				if (layer.getDataSource() == null || !(layer.getDataSource() instanceof DataSourceSpec)) {
					continue;
				}
				DataSourceSpec dss = (DataSourceSpec) layer.getDataSource();
				if (dss.source == null) {
					continue;
				}
				dss.objType = layer.getType();
				dss.objSubType = layer.getSubtype();
				dss.drawParm = layer.getDrawingParameters();
				dss.name = layer.getName();
				dss.id = layer.getContainerIdentifier();
				if (cr != null && dss.format != null && dss.geoFieldName != null && (dss.format.equalsIgnoreCase("JDBC") || dss.format.equalsIgnoreCase("ODBC") || dss.format.equalsIgnoreCase("ORACLE") || dss.format.equalsIgnoreCase("ArcSDE"))) {
					if (dss.bounds == null) {
						dss.bounds = new Vector(1, 1);
					} else {
						dss.bounds.removeAllElements();
					}
					dss.bounds.addElement(new RealRectangle(cr));
				}
				ad.addDataSpecification(dss);
			}
		}
		if (exportedTableDescriptors != null) {
			for (int i = 0; i < exportedTableDescriptors.size(); i++) {
				DataSourceSpec dss = exportedTableDescriptors.elementAt(i);
				//possibly, this table has been already added as a layer?
				if (findSamePlace(dss, ad.dataSpecs) >= 0) {
					continue;
				}
				//possibly, this table has been already added as a layer?
				boolean found = false;
				for (int j = 0; j < ad.getDataSpecCount() && !found; j++)
					if (dss.source.equals(ad.getDataSpecification(j).objDescrSource)) {
						found = true;
					}
				if (found) {
					continue;
				}
				AttributeDataPortion table = exportedTables.elementAt(i);
				dss.name = table.getName();
				DGeoLayer layer = (DGeoLayer) getTableLayer(table);
				if (layer != null && layer.getDataSource() != null && (layer.getDataSource() instanceof DataSourceSpec)) {
					DataSourceSpec spec = (DataSourceSpec) layer.getDataSource();
					if (spec.source != null) {
						dss.layerName = CopyFile.getName(spec.source);
					}
				}
				ad.addDataSpecification(dss);
			}
		}
		for (int i = 0; i < getTableCount(); i++) {
			DataTable table = (DataTable) getTable(i);
			DataSourceSpec dss = (DataSourceSpec) table.getDataSource();
			if (dss == null || dss.source == null || dss.source.startsWith("_derived")) {
				continue;
			}
			//possibly, this table has been already added as an exported table?
			if (exportedTables != null && exportedTables.contains(table)) {
				continue;
			}
			if (findSamePlace(dss, ad.dataSpecs) >= 0) {
				continue;
			}
			//possibly, this table has been already added as a layer?
			boolean found = false;
			for (int j = 0; j < ad.getDataSpecCount() && !found; j++)
				if (dss.source.equals(ad.getDataSpecification(j).source) || dss.source.equals(ad.getDataSpecification(j).objDescrSource)) {
					found = true;
				}
			if (found) {
				continue;
			}
			dss.name = table.getName();
			DGeoLayer layer = (DGeoLayer) getTableLayer(table);
			if (layer != null && layer.getDataSource() != null && (layer.getDataSource() instanceof DataSourceSpec)) {
				DataSourceSpec spec = (DataSourceSpec) layer.getDataSource();
				if (spec.source != null) {
					dss.layerName = CopyFile.getName(spec.source);
				}
			}
			ad.addDataSpecification(dss);
		}
		if (!ad.writeToFile(dir, fname)) {
			showMessage(ad.getErrorMessage(), true);
		} else {
			//following text:"The application successfully saved"
			showMessage(res.getString("The_application"), false);
		}
	}

	/**
	* Checks if the given two data source specifications refer to the same data
	* source or to files with the same names but different extensions.
	*/
	protected boolean areSourcesRelated(DataSourceSpec spec1, DataSourceSpec spec2) {
		return spec1 != null && spec2 != null && spec1.source != null && spec2.source != null && StringUtil.sameStrings(CopyFile.getDir(spec1.source), CopyFile.getDir(spec2.source))
				&& StringUtil.sameStrings(CopyFile.getNameWithoutExt(spec1.source), CopyFile.getNameWithoutExt(spec2.source));
	}

	/**
	* Updates the given table or layer by reloading the data from the data source
	* (the data might be changed by someone else).
	*/
	public boolean updateData(Object tableOrLayer) {
		if (tableOrLayer == null)
			return false;
		DataTable sourceTable = null;
		DGeoLayer sourceLayer = null;
		if (tableOrLayer instanceof DataTable) {
			sourceTable = (DataTable) tableOrLayer;
		} else if (tableOrLayer instanceof DGeoLayer) {
			sourceLayer = (DGeoLayer) tableOrLayer;
		} else
			return false;
		DataSourceSpec dss = null;
		if (sourceTable != null) {
			dss = (DataSourceSpec) sourceTable.getDataSource();
			sourceLayer = (DGeoLayer) getTableLayer(sourceTable);
			if (sourceLayer != null && areSourcesRelated(dss, (DataSourceSpec) sourceLayer.getDataSource())) {
				dss = (DataSourceSpec) sourceLayer.getDataSource();
			} else {
				sourceLayer = null;
			}
		} else {
			dss = (DataSourceSpec) sourceLayer.getDataSource();
			sourceTable = (DataTable) sourceLayer.getThematicData();
			if (sourceTable != null && !areSourcesRelated(dss, (DataSourceSpec) sourceTable.getDataSource())) {
				sourceTable = null;
			}
		}
		if (dss == null || dss.source == null) {
			//following text:"The source is not specified for the "
			showMessage(res.getString("The_source_is_not1") + ((sourceTable == null) ? res.getString("layer") : res.getString("table")), true);
			return false;
		}
		if (dss.format == null) {
			dss.format = CopyFile.getExtension(dss.source);
		}
		if (dss.format == null) {
			//following text:"Cannot determine the format of "
			showMessage(res.getString("Cannot_determine_the") + dss.name, true);
			return false;
		}
		DataReader reader = readerReg.getReaderOfFormat(dss.format);
		if (reader == null) {
			//following text:"No data reader found for the format "
			System.out.println("2");
			showMessage(res.getString("No_data_reader_found") + dss.format, true);
			return false;
		}
		reader.setMayUseCache(false);
		reader.setUI(ui);
		if (reader instanceof CompositeDataReader) {
			((CompositeDataReader) reader).setDataReaderFactory(readerReg);
		}
		reader.setDataSource(dss);
		if (!reader.loadData(false))
			return false;
		DataTable table = null;
		if (reader instanceof AttrDataReader) {
			table = ((AttrDataReader) reader).getAttrData();
		}
		DGeoLayer gl = null;
		if (reader instanceof GeoDataReader) {
			gl = ((GeoDataReader) reader).getMapLayer();
		}
		if (table == null && gl == null) {
			//following text:"Failed to reload the data!"
			showMessage(res.getString("Failed_to_reload_the"), true);
			return false;
		}
		if (table != null && sourceTable == null) {
			for (int i = 0; i < getTableCount() && sourceTable == null; i++)
				if (getTable(i) instanceof DataTable) {
					DataTable t = (DataTable) getTable(i);
					if (areSourcesRelated(dss, (DataSourceSpec) t.getDataSource())) {
						sourceTable = t;
					}
				}
		}
		if (gl != null && sourceLayer == null) {
			for (int i = 0; i < getMapCount() && sourceLayer == null; i++) {
				LayerManager lm = getMap(i);
				for (int j = 0; j < lm.getLayerCount() && sourceLayer == null; j++)
					if (lm.getGeoLayer(j) instanceof DGeoLayer) {
						DGeoLayer l = (DGeoLayer) lm.getGeoLayer(j);
						if (areSourcesRelated(dss, (DataSourceSpec) l.getDataSource())) {
							sourceLayer = l;
						}
					}
			}
		}
		dss = null;
		//if the table and the layer are loaded from different files, create a
		//reader for the other file and try to reload it
		if (sourceTable != null && table == null) {
			dss = (DataSourceSpec) sourceTable.getDataSource();
		} else if (sourceLayer != null && gl == null) {
			dss = (DataSourceSpec) sourceLayer.getDataSource();
		}
		if (dss != null && dss.source != null) {
			if (dss.format == null) {
				dss.format = CopyFile.getExtension(dss.source);
			}
			if (dss.format != null) {
				reader = readerReg.getReaderOfFormat(dss.format);
				if (reader != null && ((table == null && (reader instanceof AttrDataReader)) || (gl == null && (reader instanceof GeoDataReader)))) {
					reader.setUI(ui);
					reader.setMayUseCache(false);
					if (reader instanceof CompositeDataReader) {
						((CompositeDataReader) reader).setDataReaderFactory(readerReg);
					}
					reader.setDataSource(dss);
					if (reader.loadData(false)) {
						if (table == null && (reader instanceof AttrDataReader)) {
							table = ((AttrDataReader) reader).getAttrData();
						}
						if (gl == null && (reader instanceof GeoDataReader)) {
							gl = ((GeoDataReader) reader).getMapLayer();
						}
					}
				}
			}
		}
		boolean objectsAdded = false;
		if (table != null && sourceTable != null) {
			//check if any new attributes have been added to the table during the session
			if (sourceTable.hasAttributes()) {
				IntArray userAttrs = new IntArray(20, 10);
				for (int i = 0; i < sourceTable.getAttrCount(); i++)
					if (table.findAttrByName(sourceTable.getAttributeName(i)) < 0) {
						userAttrs.addElement(i);
					}
				if (userAttrs.size() > 0) {
					int nOldAttrs = table.getAttrCount();
					for (int j = 0; j < userAttrs.size(); j++) {
						table.addAttribute(sourceTable.getAttribute(userAttrs.elementAt(j)));
					}
					for (int i = 0; i < table.getDataItemCount(); i++) {
						DataRecord rec = table.getDataRecord(i);
						int idx = sourceTable.getObjectIndex(rec.getId());
						if (idx >= 0) {
							DataRecord rec0 = sourceTable.getDataRecord(idx);
							for (int j = 0; j < userAttrs.size(); j++) {
								rec.setAttrValue(rec0.getAttrValue(userAttrs.elementAt(j)), nOldAttrs + j);
							}
						}
					}
				}
			}
			int nPrev = sourceTable.getDataItemCount();
			sourceTable.removeAllData();
			sourceTable.setAttrList(table.getAttrList());
			for (int i = 0; i < table.getDataItemCount(); i++) {
				sourceTable.addDataRecord(table.getDataRecord(i));
			}
			objectsAdded = sourceTable.getDataItemCount() > nPrev;
		}
		if (gl != null && sourceLayer != null) {
			sourceLayer.setGeoObjects(gl.getObjects(), gl.getHasAllObjects());
			sourceLayer.setWholeLayerBounds(gl.getWholeLayerBounds());
		}
		if (sourceTable != null && sourceLayer != null) {
			sourceLayer.setDataTable(null);
			sourceLayer.receiveThematicData(sourceTable);
		}
		if (sourceLayer != null) {
			sourceLayer.notifyPropertyChange("ObjectSet", null, null);
		}
		if (sourceTable != null)
			if (objectsAdded) {
				sourceTable.notifyPropertyChange("data_added", null, null);
			} else {
				Vector attr = new Vector(sourceTable.getAttrCount(), 1);
				for (int i = 0; i < sourceTable.getAttrCount(); i++) {
					attr.addElement(sourceTable.getAttributeId(i));
				}
				sourceTable.notifyPropertyChange("values", null, attr);
			}
		return true;
	}

	/**
	* Constructs a time manager dealing with time-dependent tables and layers.
	*/
	protected TemporalDataManager makeTemporalDataManager() {
		if (timeMan == null) {
			//try to construct the component for managing time-referenced data
			try {
				timeMan = (TemporalDataManager) Class.forName(TIME_MANAGER_CLASS).newInstance();
			} catch (Exception e) {
			}
		}
		return timeMan;
	}

	/**
	 * Creates the necessary component for dealing with a time-referenced object
	 * set, which must be previously added.
	 * Notifies about appearance of such data.
	 */
	public void processTimeReferencedObjectSet(ObjectContainer oCont) {
		boolean constructed = false;
		if (timeMan == null) {
			timeMan = makeTemporalDataManager();
			if (timeMan == null)
				return;
			constructed = true;
		}
		timeMan.addTemporalDataContainer(oCont);
		if (constructed && pcSupport != null) {
			pcSupport.firePropertyChange("time_manager", null, timeMan);
		}
	}

	/**
	 * Creates the necessary component for dealing with time-referenced data
	 * table, which must be previously added.
	 * Notifies about appearance of such data.
	 */
	public void processTimeParameterTable(AttributeDataPortion table) {
		boolean constructed = false;
		if (timeMan == null) {
			timeMan = makeTemporalDataManager();
			if (timeMan == null)
				return;
			constructed = true;
		}
		timeMan.addTemporalTable(table);
		if (constructed && pcSupport != null) {
			;
		}
		pcSupport.firePropertyChange("time_manager", null, timeMan);
	}

	/**
	* Returns the component used for managing time-referenced data
	*/
	public TemporalDataManager getTimeManager() {
		return timeMan;
	}

	/**
	 * ~MO is not yet implemented - draft
	 */
	protected void exportBeforeSaveProject() {
		OKDialog okd = new OKDialog(CManager.getAnyFrame(), "Export", false);
		Panel p = new Panel(new spade.lib.basicwin.ColumnLayout());
		LayerManager lm = getMap(currMapN);

		for (int i = 0; i < this.getTableCount(); i++) {
			AttributeDataPortion dp = this.getTable(i);

			Panel pp = new Panel(new FlowLayout(FlowLayout.LEFT));
			spade.lib.basicwin.TImgButton bt = new spade.lib.basicwin.TImgButton("/icons/Save.gif");
			pp.add(bt);
			pp.add(new Label(dp.getName()));
			p.add(pp);
		}
		okd.addContent(p);
		okd.show();
	}

	/**
	 * Stores information about an exported table in order to include it
	 * in an *.app file when the project is saved.
	 */
	public void tableWasExported(AttributeDataPortion table, DataSourceSpec tableDescr) {
		if (table == null || tableDescr == null || tableDescr.source == null)
			return;
		if (exportedTableDescriptors == null || exportedTables == null) {
			exportedTableDescriptors = new Vector<DataSourceSpec>(10, 10);
			exportedTables = new Vector<AttributeDataPortion>(10, 10);
		}
		int idx = exportedTables.indexOf(table);
		if (idx < 0) {
			idx = findSamePlace(tableDescr, exportedTableDescriptors);
		}
		if (idx < 0) {
			exportedTables.addElement(table);
			exportedTableDescriptors.addElement(tableDescr);
		} else {
			exportedTables.setElementAt(table, idx);
			exportedTableDescriptors.setElementAt(tableDescr, idx);
		}
	}

	/**
	 * Tries to find a descriptor with the same file or database table as specified in tableDescr.
	 * If found, returns its index in the vector descriptors; otherwise returns -1.
	 */
	public int findSamePlace(DataSourceSpec tableDescr, Vector<DataSourceSpec> descriptors) {
		if (descriptors == null)
			return -1;
		//find if there was a previous export to the same file
		for (int i = 0; i < descriptors.size(); i++) {
			DataSourceSpec dsc = descriptors.elementAt(i);
			if (StringUtil.sameStringsIgnoreCase(dsc.source, tableDescr.source) && StringUtil.sameStringsIgnoreCase(dsc.server, tableDescr.server) && StringUtil.sameStringsIgnoreCase(dsc.driver, tableDescr.driver)
					&& StringUtil.sameStringsIgnoreCase(dsc.url, tableDescr.url) && StringUtil.sameStringsIgnoreCase(dsc.user, tableDescr.user) && StringUtil.sameStringsIgnoreCase(dsc.catalog, tableDescr.catalog))
				return i;
		}
		return -1;
	}

	/**
	 * Clears all internal structures when all data are removed
	 */
	public void clearAll() {
		applPath = null;
		lManagers = null;
		dataTables = null;
		recMans = null;
		currMapN = -1;
		timeMan = null;
	}
}
