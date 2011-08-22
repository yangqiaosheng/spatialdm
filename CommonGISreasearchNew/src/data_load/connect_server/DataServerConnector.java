package data_load.connect_server;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.system.AttrDataReader;
import spade.analysis.system.CompositeDataReader;
import spade.analysis.system.DataReader;
import spade.analysis.system.DataReaderFactory;
import spade.analysis.system.GeoDataReader;
import spade.lib.lang.Language;
import spade.lib.util.CopyFile;
import spade.vis.database.DataPortion;
import spade.vis.database.DataSupplier;
import spade.vis.database.DataTable;
import spade.vis.database.LayerData;
import spade.vis.database.TableContentSupplier;
import spade.vis.dmap.DGeoLayer;
import spade.vis.geometry.RealRectangle;
import spade.vis.spec.DataSourceSpec;
import data_load.DataAgent;
import data_load.DataReaderRegister;
import data_load.readers.BaseDataReader;

/**
* Gets geographic and thematic data from a data server.
*/
public class DataServerConnector extends BaseDataReader implements AttrDataReader, TableContentSupplier, GeoDataReader, DataSupplier, CompositeDataReader {
	static ResourceBundle res = Language.getTextResource("data_load.connect_server.Res");
	/**
	* The table loaded
	*/
	protected DataTable dtab = null;
	/**
	* The geo layer created, if the table contains coordinates
	*/
	protected DGeoLayer layer = null;
	/**
	* The spatial data for the layer (used in delayed loading or in caching)
	*/
	protected LayerData layerData = null;
	/**
	* The version number allows to control the updates of the table and layer
	*/
	protected long lastVersionNumber = 0l;
	/**
	* The DataReaderFactory makes it possible to use the data Server only when
	* there is no appropriate data reader in the configuration or such reader
	* cannot access the data. The factory is used for constructing the data reader.
	*/
	protected DataReaderFactory readerFactory = null;
	/**
	* Indicates whether the DataServerConnector may try to use an alternative
	* reader for loading the data before attempting to use the data server.
	* This may be the reader constructed with the use of the readerFactory for the
	* specified data format, if such a reader is available in the configuration.
	*/
	protected boolean mayTryAltReader = false;
	/**
	* The alternative internal reader used for loading data (if successfully
	* constructed and can access the data)
	*/
	protected DataReader altReader = null;

	/**
	* Sets the DataReaderFactory. The factory is used for the following purposes:
	* the DataServerConnector first checks if a reader for the specified data
	* format is available in the system configuration. If so, constructs this
	* reader and uses it for loading data. Only if the reader is not available or
	* cannot access the data, the DataServerConnector uses the data server.
	*/
	@Override
	public void setDataReaderFactory(DataReaderFactory factory) {
		readerFactory = factory;
	}

	/**
	* Loads the data. When allowed (the argument mayAskUser is true) and necessary,
	* starts a dialog or a wizard for the user
	* to specify the data source, constraints, etc. Returns true if the data
	* have been successfully loaded.
	*/
	@Override
	public boolean loadData(boolean mayAskUser) {
		if (mayAskUser) {
			mayTryAltReader = false;
		}
		if (spec == null || spec.source == null || spec.server == null)
			if (!mayAskUser || !getSpecification())
				return false;
		if (dataError)
			return false;
		if (dataReadingInProgress) {
			waitDataReadingFinish();
			return !dataError;
		}
		if (!mayAskUser) {
			setDataReadingInProgress(true);
		}
		DataTable table = null;
		DGeoLayer gl = null;
		/**/
		if (altReader == null && mayTryAltReader && readerFactory != null && !GetSpecWizard.isDB(spec.format)) {
			//try to construct an internal altReader for this data format
			altReader = readerFactory.getReaderOfFormat(spec.format);
			if (altReader != null) {
				altReader.setUI(ui);
				if (altReader instanceof CompositeDataReader) {
					((CompositeDataReader) altReader).setDataReaderFactory(readerFactory);
				}
			}
		}
		/**/
		if (altReader != null) {
			altReader.setDataSource(spec);
			boolean result = false;
			try {
				result = altReader.loadData(mayAskUser);
			} catch (Throwable thr) {
				//following text:"Reader "
				showMessage(res.getString("Reader") + altReader.getClass().getName() + ": " + thr.toString(), true);
				result = false;
			}
			if (result) {
				//following text:"Reader "
				showMessage(res.getString("Reader") + altReader.getClass().getName() +
				//following text:" actually used instead of data server for loading "
						res.getString("actually_used_instead") + spec.name, false);
				if (altReader instanceof AttrDataReader) {
					table = ((AttrDataReader) altReader).getAttrData();
				}
				if (altReader instanceof GeoDataReader) {
					gl = ((GeoDataReader) altReader).getMapLayer();
				}
			} else {
				altReader = null;
			}
		}
		mayTryAltReader = false;
		if (table == null && gl == null) {
			//start the servlet for loading the data
			URL servletURL = null;
			spec.server = spec.server.replace('\\', '/');
			if (!spec.server.endsWith("/")) {
				spec.server += "/";
			}
			try {
				servletURL = new URL(spec.server + "DataReadServlet");
			} catch (MalformedURLException mfe) {
				showMessage(mfe.toString(), true);
				dataError = true;
				setDataReadingInProgress(false);
				return false;
			}
			if (servletURL == null) {
				//following text:"Could not form the URL of the servlet from "
				showMessage(res.getString("Could_not_form_the") + spec.url, true);
				dataError = true;
				setDataReadingInProgress(false);
				return false;
			}
			if ((spec.bounds == null || spec.bounds.size() < 1) && spec.geoFieldName != null)
				if (ui != null && ui.getCurrentMapViewer() != null) {
					RealRectangle mapExt = new RealRectangle(ui.getCurrentMapViewer().getMapExtent());
					if (mapExt != null) {
						if (spec.bounds == null) {
							spec.bounds = new Vector(1, 1);
						}
						spec.bounds.addElement(mapExt);
					}
				}
			//following text:"Connecting to the server "
			showMessage(res.getString("Connecting_to_the") + servletURL.toString(), false);
			URLConnection con = null;
			OutputStream out = null;
			ObjectOutputStream objOut = null;
			InputStream in = null;
			ObjectInputStream objIn = null;
			try {
				con = servletURL.openConnection();
				con.setDoOutput(true);
				con.setDoInput(true);
				con.setUseCaches(false);
				con.setRequestProperty("Content-Type", "application/octet-stream");
			} catch (IOException e) {
				//following text:"Failed to open connection with server: "
				showMessage(res.getString("Failed_to_open") + e.toString(), true);
				dataError = true;
			}
			try {
				out = con.getOutputStream();
			} catch (IOException e) {
				//following text:"Failed to open output stream to server: "
				showMessage(res.getString("Failed_to_open_output") + e.toString(), true);
				dataError = true;
			}
			Object drawParm = spec.drawParm;
			spec.drawParm = null;
			if (!dataError && out != null) {
				try {
					objOut = new ObjectOutputStream(out);
					objOut.writeObject(spec);
					objOut.flush();
					System.out.println("Specification for " + spec.name + " sent to the server");
				} catch (IOException e) {
					//following text:"Failed to send data specification to server: "
					showMessage(res.getString("Failed_to_send_data") + e.toString(), true);
					dataError = true;
				}
			}
			/*
			if (objOut!=null) try { objOut.close(); } catch (IOException ioe) {
			  showMessage("Failed to close OBJECT output stream: "+ioe.getMessage(),true);
			}
			*/
			if (out != null) {
				try {
					out.close();
				} catch (IOException ioe) {
					//following text:"Failed to close output stream: "
					showMessage(res.getString("Failed_to_close") + ioe.getMessage(), true);
				}
			}
			spec.drawParm = drawParm;
			if (!dataError) {
				try {
					in = con.getInputStream();
				} catch (IOException e) {
					//following text:"Failed to open input stream from server: "
					showMessage(res.getString("Failed_to_open_input") + e.getMessage(), true);
					dataError = true;
				}
			}
			if (in != null) {
				try {
					objIn = new ObjectInputStream(in);
				} catch (IOException e) {
					//following text:"Failed to construct an ObjectInputStream:
					showMessage(res.getString("Failed_to_construct") + e.toString(), true);
				}
			}
			if (objIn != null) {
				//following text:"Getting data from the server
				showMessage(res.getString("Getting_data_from_the") + servletURL.toString(), false);
				while (true) {
					try {
						Object obj = objIn.readObject();
						if (obj == null) {
							break;
						}
						if (obj instanceof String) {
							String str = (String) obj;
							if (str.startsWith("ERROR")) {
								//following text:"Error when getting data from "
								showMessage(res.getString("Error_when_getting") + servletURL + ": " + str.substring(6).trim(), true);
								dataError = true;
							} else {
								System.out.println("Received from server: " + str);
							}
						} else if (obj instanceof DataTable) {
							table = (DataTable) obj;
							//following text:"Got table: N attributes = "
							showMessage(res.getString("Got_table_N") + table.getAttrCount() +
							//following text:"; N records = "
									res.getString("_N_records_") + table.getDataItemCount(), false);
						} else if (obj instanceof DGeoLayer) {
							gl = (DGeoLayer) obj;
							//following text:"Got layer: N objects = "
							showMessage(res.getString("Got_layer_N_objects_") + gl.getObjectCount(), false);
						}
					} catch (EOFException e) {
						break;
					} catch (IOException ioe) {
						//following text:"Error in reading data server\'s output: "
						showMessage(res.getString("Error_in_reading_data") + ioe.toString(), true);
						dataError = true;
						break;
					} catch (ClassNotFoundException cnfe) {
						//following text:"Error in getting object from data server: "
						showMessage(res.getString("Error_in_getting") + cnfe.toString(), true);
					}
				}
			}
			/*
			if (objIn!=null) try { objIn.close(); } catch (IOException ioe) {
			  showMessage("Failed to close OBJECT input stream: "+ioe.toString(),true);
			}
			*/
			if (in != null) {
				try {
					in.close();
				} catch (IOException ioe) {
					//following text:"Failed to close input stream: "
					showMessage(res.getString("Failed_to_close_input") + ioe.toString(), true);
				}
			}
			if (dataError) {
				setDataReadingInProgress(false);
				return false;
			}
		}
		Vector descr = null;
		if (table != null) {
			table.setDataSource(spec);
			table.setTableContentSupplier(null);
			if (dtab == null) {
				dtab = table;
			} else {
				if (spec.descriptors != null && (table.getParamCount() > 0 || table.hasTimeReferences())) {
					descr = spec.descriptors;
					spec.descriptors = null;
				}
				dtab.update(table, false);
			}
			if (descr != null) {
				spec.descriptors = descr;
			}
		}
		if (gl != null) {
			if (spec.dataExtent == null || spec.dbRowCount <= 0) {
				DataSourceSpec dss = (DataSourceSpec) gl.getDataSource();
				if (dss != null) {
					if (dss.dataExtent != null) {
						spec.dataExtent = dss.dataExtent;
					}
					if (dss.dbRowCount > 0) {
						spec.dbRowCount = dss.dbRowCount;
					}
				}
			}
			gl.setDataSource(spec);
			gl.setDataSupplier(null);
			if (layer == null) {
				layer = gl;
			} else {
				layerData = getLayerData(gl);
			}
			connectLayerToSupplier();
		}
		setDataReadingInProgress(false);
		if (dtab != null && dtab.hasData()) {
			dtab.finishedDataLoading();
		}
		return table != null || gl != null;
	}

	/**
	* If the layer does not contain all geographical objects available in the
	* source database, connects it to the appropriate data supplier. This may
	* be either this reader itself or a DataBroker connected to this reader.
	*/
	protected void connectLayerToSupplier() {
		if (layer == null)
			return;
		if (layer.getDataSupplier() != null && (layer.getDataSupplier() instanceof DataAgent))
			return;
		if (layer.getHasAllObjects()) {
			layer.setDataSupplier(null);
			return;
		}
		layer.setDataSupplier(this);
		if (GetSpecWizard.isDB(spec.format) && spec.geoFieldName != null && spec.idFieldName != null && spec.dbRowCount > 0 && spec.dataExtent != null && (spec.dataExtent instanceof RealRectangle)) {
			//Check whether the DataBroker class is available
			DataAgent dataBroker = null;
			try {
				dataBroker = (DataAgent) Class.forName("data_load.cache.DataBroker").newInstance();
			} catch (Exception e) {
				//following text:"Could not construct Data Broker: "
				showMessage(res.getString("Could_not_construct") + e.toString(), true);
			}
			if (dataBroker != null) {
				//following text:"Constructed Data Broker"
				showMessage(res.getString("Constructed_Data"), false);
				dataBroker.setDataReader(this);
				dataBroker.setUI(ui);
				dataBroker.init((RealRectangle) spec.dataExtent, spec.dbRowCount);
				layer.setDataSupplier(dataBroker);
			}
		}
	}

	/**
	* Gets the specification of the data to be loaded in dialog with the user
	*/
	protected boolean getSpecification() {
		GetSpecWizard wizard = new GetSpecWizard();
		if (spec == null) {
			spec = new DataSourceSpec();
		}
		return wizard.getSpecification(spec);
	}

	/**
	* Returns the table with the attribute (thematic) data loaded (if any)
	*/
	@Override
	public DataTable getAttrData() {
		if (dtab == null && (layer == null || layer.getObjectCount() < 1) && spec != null && spec.source != null) {
			//check if the table may exist for this data format
			String format = spec.format;
			if (format == null) {
				format = CopyFile.getExtension(spec.source);
			}
			if (DataReaderRegister.mayHaveAttrData(format)) {
				dtab = new DataTable();
				dtab.setDataSource(spec);
				if (spec.name != null) {
					dtab.setName(spec.name);
				} else {
					dtab.setName(spec.source);
				}
				dtab.setTableContentSupplier(this);
			}
		}
		return dtab;
	}

//----------------- TableContentSupplier interface ---------------------------
//----------------- (used for "delayed" loading of table data) ---------------
	/**
	* Using this method the table asks its supplier to fill it with the data.
	* The method returns true if the data have been successfully loaded.
	*/
	@Override
	public boolean fillTable() {
		return loadData(false);
	}

	/**
	* Returns the map layer constructed from the geographical data loaded (if any)
	*/
	@Override
	public DGeoLayer getMapLayer() {
		if ((dtab == null || !dtab.hasData()) && layer == null && spec != null && spec.source != null) {
			//check if the table may exist for this data format
			String format = spec.format;
			if (format == null) {
				format = CopyFile.getExtension(spec.source);
			}
			boolean mayBeLayer = true;
			if (GetSpecWizard.isASCII(format) || GetSpecWizard.isDBF(format)) {
				mayBeLayer = spec.xCoordFieldName != null && spec.yCoordFieldName != null;
			} else if (GetSpecWizard.isDB(format)) {
				mayBeLayer = spec.geoFieldName != null || (spec.xCoordFieldName != null && spec.yCoordFieldName != null);
			}
			if (mayBeLayer) {
				layer = new DGeoLayer();
				layer.setDataSource(spec);
				if (spec.name != null) {
					layer.setName(spec.name);
				} else {
					layer.setName(spec.source);
				}
				connectLayerToSupplier();
			}
		}
		return layer;
	}

//----------------- DataSupplier interface -----------------------------------
//----------------- (used for "delayed" loading of map layers) ---------------
	/**
	* Constructs and returns a DataPortion containing all DataItems
	* available
	*/
	@Override
	public DataPortion getData() {
		if (layerData != null)
			return layerData;
		if (spec == null)
			return null;
		if (spec.geoFieldName != null || dtab == null || !dtab.hasData()) {
			loadData(false);
		}
		return layerData;
	}

	/**
	* Constructs and returns a DataPortion containing DataItems fitting in at
	* least one of the specified bounding rectangles (listed in the vector).
	* The elements of the vector are instances of
	* @see spade.vis.geometry.RealRectangle
	*/
	@Override
	public DataPortion getData(Vector bounds) {
		if (layerData != null && layerData.hasAllData())
			return layerData;
		if (dtab != null && (spec.bounds == null || spec.bounds.size() < 1 || (spec.geoFieldName == null && (spec.xCoordFieldName == null || spec.yCoordFieldName == null))))
			return getData();
		layerData = null;
		if (!GetSpecWizard.isImage(spec.format) || spec.bounds == null || spec.bounds.size() < 1) {
			spec.bounds = bounds;
		}
		return getData();
	}

	/**
	* When no more data from the DataSupplier are needed, this method is
	* called. Here the DataSupplier can clear its internal structures
	*/
	@Override
	public void clearAll() {
		layerData = null;
	}

	/**
	* In order to implement the Data Supplier interface, the reader must be
	* able to return a DataPortion (more specifically, LayerData). However, the
	* the data server sends DGeoLayers rather than DataPortions. This method
	* extracts a DataPortion from a DGeoLayer.
	*/
	protected LayerData getLayerData(DGeoLayer gl) {
		if (gl == null)
			return null;
		LayerData data = new LayerData();
		RealRectangle bounds = gl.getCurrentLayerBounds();
		if (bounds == null && spec != null && spec.bounds != null && spec.bounds.size() > 1) {
			bounds = (RealRectangle) spec.bounds.elementAt(0);
		}
		if (bounds != null) {
			data.setBoundingRectangle(bounds.rx1, bounds.ry1, bounds.rx2, bounds.ry2);
		}
		data.setHasAllData(gl.getHasAllObjects());
		for (int i = 0; i < gl.getObjectCount(); i++) {
			data.addItemSimple(gl.getObject(i).getSpatialData());
		}
		return data;
	}
}
