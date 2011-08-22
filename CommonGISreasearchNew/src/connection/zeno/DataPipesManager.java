package connection.zeno;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Calendar;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.system.ESDACore;
import spade.analysis.system.MapToolbar;
import spade.analysis.tools.DataAnalyser;
import spade.analysis.tools.SingleInstanceTool;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.OKDialog;
import spade.lib.basicwin.PopupManager;
import spade.lib.basicwin.TImgButton;
import spade.lib.basicwin.TextCanvas;
import spade.lib.lang.Language;
import spade.lib.util.StringUtil;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DLayerManager;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.RealRectangle;
import spade.vis.space.GeoLayer;
import spade.vis.space.LayerManager;
import spade.vis.spec.DataPipeSpec;

/**
* Manages listening to specified URLs, from which data may come (e.g. from Zeno).
* For each URL creates a corresponding DataPipeListener.
* It is important that only a single instance of this tool is created per
* work session. For this purpose the class implements the interface
* spade.analysis.tools.SingleInstanceTool.
*/
public class DataPipesManager implements DataAnalyser, SingleInstanceTool, WindowListener, ActionListener, DataReceiver {
	static ResourceBundle res = Language.getTextResource("connection.zeno.Res");
	/**
	* prefixes of button commands
	*/
	protected static String prefixes[] = { "start_", "stop_", "georeference_", "store_" };
	/**
	* The list of existing datapipe listeners.
	*/
	protected Vector dpListeners = null;
	/**
	* The system's core
	*/
	protected ESDACore core = null;
	/**
	* The main window of the system: when it is activated, the DataPipesManager
	* activates checking the data pipes for new entries.
	*/
	protected Window mainWindow = null;
	/**
	* The time of the last check for data
	*/
	protected long lastCheckTime = -1;
	/**
	* The minimum delay (in milliseconds) between subsequent checks
	*/
	protected long delay = 10000;
	/**
	* The dialog to display the current status of each datapipe. In this dialog
	* the user may press some buttons, therefore the DataPipesManager must
	* listen to action events from the dialog. After any action event the dialog
	* must be disposed. Therefore a reference to it must be kept in order to be
	* able to dispose the dialog from the method actionPerformed.
	*/
	protected OKDialog statusDialog = null;
	/**
	* When entries come from a file, the manager checks if the file has been
	* modified since the previous access. This vector stores the names (paths)
	* of the files that have been ever accessed.
	*/
	protected Vector entryFiles = null;
	/**
	* This vector stores the last modification times for all files from which
	* entries were got. The times are stored as instances of the Long class.
	*/
	protected Vector modTimes = null;
	/**
	* The thread used to start a dialog that proposes the user to georeference
	* new entries. The thread is needed because the Java plugin hangs when some
	* dialog is started directly from the method windowActivated of the
	* WindowListener interface.
	*/
	protected DataPipeThread dpThread = null;

	/**
	* Returns true.
	*/
	public boolean isValid(ESDACore core) {
		return true;
	}

	/**
	* This method displays a dialog in which the user can switch on or off
	* existing datapipes, create new datapipes, and see the status of currently
	* active datapipes.
	*/
	public void run(ESDACore core) {
		this.core = core;
		core.getUI().showMessage(null, false);
		removeDestroyedListeners();
		//If any of the datapipe listeners is currently georeferencing or storing
		//new data, immediately return.
		if (dpListeners != null) {
			for (int i = 0; i < dpListeners.size(); i++) {
				DataPipeListener dpl = (DataPipeListener) dpListeners.elementAt(i);
				int status = dpl.getStatus();
				if (status != DataPipeListener.IDLE && status != DataPipeListener.INACTIVE) {
					core.getUI().showMessage(res.getString("Layer") + " " + dpl.getLayer().getName() + ": " + res.getString("georef_in_progress"), true);
					return;
				}
			}
		}
		boolean firstStart = dpListeners == null;
		if (dpListeners == null || dpListeners.size() < 1) {
			//Look whether any datapipes were specified for the current application.
			//If so, the vector of the specification is stored as the value of the
			//parameter "DATAPIPE"
			Object obj = core.getSystemSettings().getParameter("DATAPIPE");
			if (obj != null && (obj instanceof Vector)) {
				LayerManager lm = null;
				int mapN = -1;
				if (core.getUI() != null) {
					mapN = core.getUI().getCurrentMapN();
					if (mapN >= 0) {
						lm = core.getDataKeeper().getMap(mapN);
					}
					// P.G. add button for calling Dito to create annotations
					TImgButton timgbAnnotateInDito = new TImgButton("/icons/annotate.gif");
					if (timgbAnnotateInDito == null) {
						timgbAnnotateInDito = new TImgButton("Annotate", true);
					}
					Panel pAnnotateInDito = new Panel(new BorderLayout());
					pAnnotateInDito.add(new Label("", Label.CENTER), BorderLayout.WEST);
					pAnnotateInDito.add(timgbAnnotateInDito, BorderLayout.CENTER);
					new PopupManager(timgbAnnotateInDito, "Annotate", true);
					MapToolbar mtb = core.getUI().getCurrentMapViewer().getMapToolbar();
					if (mtb != null) {
						mtb.addToolbarElement(pAnnotateInDito);
						// ~P.G.
					}
				}
				if (lm != null) {
					Vector pipes = (Vector) obj;
					for (int i = 0; i < pipes.size(); i++) {
						DataPipeSpec dataPipeSpec = (DataPipeSpec) pipes.elementAt(i);
						if (dataPipeSpec != null) {
							System.out.println("Datapipe specification:");
							System.out.println("--- data source = [" + dataPipeSpec.dataSource + "]");
							System.out.println("--- layer Id = [" + dataPipeSpec.layerId + "]");
							System.out.println("--- table = [" + dataPipeSpec.tableFileName + "]");
						}
						if (dataPipeSpec == null || dataPipeSpec.dataSource == null || dataPipeSpec.layerId == null) {
							continue;
						}
						if (dataPipeSpec.updater != null) {
							dataPipeSpec.updater = core.getFullURLString(dataPipeSpec.updater);
						}
						dataPipeSpec.dataSource = core.getFullURLString(dataPipeSpec.dataSource);
						URL dataURL = null;
						try {
							System.out.println("Trying to construct a URL from [" + dataPipeSpec.dataSource + "]");
							dataURL = new URL(dataPipeSpec.dataSource);
							System.out.println("Constructed the datapipe URL: [" + dataURL + "]");
						} catch (MalformedURLException mfe) {
							System.out.println("Exception: " + mfe);
							dataPipeSpec.dataSource = null;
							continue;
						}
						int idx = lm.getIndexOfLayer(dataPipeSpec.layerId);
						if (idx >= 0 && (lm.getGeoLayer(idx) instanceof DGeoLayer)) {
							//construct a DataPipeListener for this layer and this dataURL
							DataPipeListener dpl = new DataPipeListener(dataPipeSpec, (DGeoLayer) lm.getGeoLayer(idx), mapN, core);
							if (dpListeners == null) {
								dpListeners = new Vector(5, 5);
							}
							dpListeners.addElement(dpl);
						} else {
							System.out.println("The layer [" + dataPipeSpec.layerId + "] not found!");
						}
					}
					updateSystemSettings();
				}
			} else {
				System.out.println("No datapipe specifications read from the application file!");
			}
		}
		if (firstStart && dpListeners != null && dpListeners.size() > 0) {
			//start all the specified datapipes automatically when the application is
			//just loaded
			for (int i = 0; i < dpListeners.size(); i++) {
				DataPipeListener dpl = (DataPipeListener) dpListeners.elementAt(i);
				dpl.activate(true);
			}
			if (mainWindow == null) {
				mainWindow = core.getUI().getMainFrame();
			}
			//Listen to activation events of the main window of the system.
			if (mainWindow != null) {
				mainWindow.addWindowListener(this);
			}
			startDataPipeChecking();
			return;
		}
		//Construct a dialog to show the current status of each existing datapipe
		//listener.
		Panel pan = new Panel(new ColumnLayout());
		pan.add(new Label(res.getString("Data_pipelines"), Label.CENTER));
		if (dpListeners == null || dpListeners.size() < 1) {
			// following string: "No data pipelines defined yet"
			pan.add(new Label(res.getString("No_data_pipelines")));
		} else {
			for (int i = 0; i < dpListeners.size(); i++) {
				DataPipeListener dpl = (DataPipeListener) dpListeners.elementAt(i);
				DataPipeSpec spec = dpl.getDataPipeSpecification();
				Label lab = new Label(res.getString("Layer") + " " + dpl.getLayer().getName() + " <<< " + spec.dataSource);
				boolean hasNewData = dpl.getPendingEntriesCount() > 0 || dpl.getNewObjectsCount() > 0;
				if (!dpl.isActive() || !hasNewData) {
					Panel p = new Panel(new BorderLayout(10, 0));
					Button b = new Button((dpl.isActive()) ? res.getString("Stop") : res.getString("Start"));
					b.setActionCommand(((dpl.isActive()) ? prefixes[1] : prefixes[0]) + i);
					b.addActionListener(this);
					p.add(b, "West");
					p.add(lab, "Center");
					pan.add(p);
				} else {
					pan.add(lab);
				}
				if (dpl.isActive() && !hasNewData) {
					pan.add(new Label(res.getString("no_new_data"), Label.CENTER));
				}
				int nobj = dpl.getPendingEntriesCount();
				if (nobj > 0) {
					Panel p = new Panel(new BorderLayout(10, 0));
					// following string:
					/* nobj+" new "+((nobj>1)?"entries wait":"entry waits")+
					                 " for georeferencing"),*/
					p.add(new Label(nobj + res.getString("new") + ((nobj > 1) ? res.getString("entries_wait") : res.getString("entry_waits")) + res.getString("for_georeferencing")), "Center");
					Button b = new Button(res.getString("Start"));
					b.setActionCommand(prefixes[2] + i);
					b.addActionListener(this);
					p.add(b, "East");
					pan.add(p);
				}
				nobj = dpl.getNewObjectsCount();
				if (nobj > 0) {
					Panel p = new Panel(new BorderLayout());
					p.add(new Label(nobj + res.getString("new_georeferenced") + ((nobj > 1) ? res.getString("objects_have") : res.getString("object_has")) + res.getString("not_been_stored_yet")), "Center");
					Button b = new Button(res.getString("Store"));
					b.setActionCommand(prefixes[3] + i);
					b.addActionListener(this);
					p.add(b, "East");
					pan.add(p);
				}
			}
		}
		if (mainWindow == null) {
			mainWindow = core.getUI().getMainFrame();
		} else {
			mainWindow.removeWindowListener(this);
		}
		Button b = new Button(res.getString("Create_a_new_datapipe"));
		b.setActionCommand("create_datapipe");
		b.addActionListener(this);
		Panel p = new Panel(new FlowLayout(FlowLayout.CENTER, 0, 2));
		p.add(b);
		pan.add(p);
		statusDialog = new OKDialog(CManager.getAnyFrame(), res.getString("Data_pipelines"), false);
		statusDialog.addContent(pan);
		statusDialog.show();
		//Listen to activation events of the main window of the system.
		if (mainWindow != null) {
			mainWindow.addWindowListener(this);
		}
	}

	/**
	* Checks if any of currently existing datapipe listener has been destroyed.
	* If so, removes it from the vector of listeners.
	*/
	protected void removeDestroyedListeners() {
		if (dpListeners == null || dpListeners.size() < 1)
			return;
		for (int i = dpListeners.size() - 1; i >= 0; i--) {
			DataPipeListener dpl = (DataPipeListener) dpListeners.elementAt(i);
			if (dpl.getStatus() == DataPipeListener.DESTROYED) {
				dpListeners.removeElementAt(i);
			}
		}
	}

	/**
	* Reacts to commands coming from the statusDialog. Disposes the dialog.
	*/
	public void actionPerformed(ActionEvent e) {
		if (mainWindow != null) {
			mainWindow.removeWindowListener(this);
		}
		if (statusDialog != null) {
			statusDialog.dispose();
			statusDialog = null;
		}
		String cmd = e.getActionCommand();
		if (cmd.equals("create_datapipe")) {
			DataPipeListener dpl = createDataPipe();
			if (dpl != null) {
				if (dpListeners == null) {
					dpListeners = new Vector(5, 5);
				}
				dpListeners.addElement(dpl);
				updateSystemSettings();
				dpl.activate(true);
			}
		} else if (dpListeners != null) {
			for (int i = 0; i < prefixes.length; i++)
				if (cmd.startsWith(prefixes[i])) {
					String str = cmd.substring(prefixes[i].length());
					try {
						int idx = Integer.valueOf(str).intValue();
						if (idx >= 0 && idx < dpListeners.size()) {
							DataPipeListener dpl = (DataPipeListener) dpListeners.elementAt(idx);
							if (i == 0) {
								dpl.activate(true);
							} else if (i == 1) {
								dpl.activate(false);
							} else if (i == 2) {
								dpl.startGeoreferencing();
							} else if (i == 3) {
								dpl.storeEntities();
							}
						}
					} catch (NumberFormatException nfe) {
					}
					break;
				}
		}
		if (mainWindow != null) {
			mainWindow.addWindowListener(this);
		}
	}

	/**
	* Defines and creates a new datapipe. For this puprose asks the user about the
	* URL to listen to and the layer to which to add new objects. Returns the
	* DataPipeListener responsible for the datapipe.
	*/
	protected DataPipeListener createDataPipe() {
		URL dataURL = null;
		//ask the user about the URL to listen
		URL docBase = null;
		Object obj = core.getSystemSettings().getParameter("DocumentBase");
		if (obj != null && (obj instanceof URL)) {
			docBase = (URL) obj;
		}
		// following string: "Specify the URL to listen:"
		GetURLPanel pan = new GetURLPanel(docBase, dataURL, res.getString("Specify_the_URL_to"));
		OKDialog okd = new OKDialog(CManager.getAnyFrame(), res.getString("Data_source"), true);
		okd.addContent(pan);
		okd.show();
		if (okd.wasCancelled())
			return null;
		dataURL = pan.getURL();
		System.out.println("Got URL: " + dataURL);
		//
		LayerManager lm = null;
		int mapN = -1;
		if (core.getUI() != null) {
			mapN = core.getUI().getCurrentMapN();
			if (mapN >= 0) {
				lm = core.getDataKeeper().getMap(mapN);
			}
		}
		//ask the user about the layer in which to add new objects
		DGeoLayer layer = null;
		int nVectorLayers = 0;
		if (lm != null) {
			for (int i = 0; i < lm.getLayerCount(); i++) {
				GeoLayer gl = lm.getGeoLayer(i);
				if (gl.getType() == Geometry.point || gl.getType() == Geometry.line || gl.getType() == Geometry.area) {
					++nVectorLayers;
				}
			}
		}
		if (nVectorLayers < 1) { //a completely new map must be constructed
			Panel p = new Panel(new GridLayout(2, 1));
			// following string: "There are no map layers appropriate for adding data."
			p.add(new Label(res.getString("There_are_no_map"), Label.CENTER));
			// following string: "Would you like to construct a new map layer?"
			p.add(new Label(res.getString("Would_you_like_to"), Label.CENTER));
			// following string: "Construct a new layer?"
			okd = new OKDialog(CManager.getAnyFrame(), res.getString("Construct_a_new_layer"), OKDialog.YES_NO_MODE, true);
			okd.addContent(p);
			okd.show();
			if (okd.wasCancelled())
				return null;
		} else {
			List lst = new List((nVectorLayers < 8) ? nVectorLayers : 8);
			Vector layers = new Vector(nVectorLayers, 1);
			for (int i = 0; i < lm.getLayerCount(); i++) {
				GeoLayer gl = lm.getGeoLayer(i);
				if (gl.getType() == Geometry.point || gl.getType() == Geometry.line || gl.getType() == Geometry.area) {
					layers.addElement(gl);
					lst.add(gl.getName());
				}
			}
			lst.select(0);
			Panel p = new Panel(new BorderLayout());
			CheckboxGroup cbg = new CheckboxGroup();
			Checkbox selectCB = new Checkbox(res.getString("Add_data_to_the_layer"), true, cbg);
			p.add(selectCB, "North");
			p.add(lst, "Center");
			// following string: "Add data to a NEW layer"
			Checkbox constructCB = new Checkbox(res.getString("Add_data_to_a_NEW"), false, cbg);
			p.add(constructCB, "South");
			okd = new OKDialog(CManager.getAnyFrame(), res.getString("The_layer_to_add_data"), true);
			okd.addContent(p);
			okd.show();
			if (okd.wasCancelled())
				return null;
			if (selectCB.getState()) {
				layer = (DGeoLayer) layers.elementAt(lst.getSelectedIndex());
			}
		}
		if (layer == null) { //construction of a new layer
			String layerName = null;
			char layerType = Geometry.point;
			RealRectangle extent = null;
			if (lm != null && (lm instanceof DLayerManager)) {
				DLayerManager dlm = (DLayerManager) lm;
				extent = dlm.getWholeTerritoryBounds();
			}
			//Asks the user about the type of the objects in the layer
			Panel p = new Panel(new ColumnLayout());
			Panel pp = new Panel(new BorderLayout());
			pp.add(new Label(res.getString("Layer_name_")), "West");
			TextField tf = new TextField();
			pp.add(tf, "Center");
			p.add(pp);
			// following string: "The type of objects in the layer:"
			p.add(new Label(res.getString("The_type_of_objects")));
			CheckboxGroup cbg = new CheckboxGroup();
			Checkbox cbPoint = new Checkbox(res.getString("point"), cbg, true), cbLine = new Checkbox(res.getString("line"), cbg, false), cbArea = new Checkbox(res.getString("area"), cbg, false);
			p.add(cbPoint);
			p.add(cbLine);
			p.add(cbArea);
			TextField tfExt[] = null;
			if (extent == null) { //ask the user about the layer extent
				// following string: "Define the layer extent:"
				p.add(new Label(res.getString("Define_the_layer")));
				pp = new Panel(new GridLayout(2, 2));
				tfExt = new TextField[4];
				for (int i = 0; i < 4; i++) {
					String txt = ((i % 2 == 0) ? "X" : "Y") + ((i < 2) ? "1" : "2") + ":";
					Panel p1 = new Panel(new BorderLayout());
					p1.add(new Label(txt), "West");
					tfExt[i] = new TextField(8);
					p1.add(tfExt[i], "Center");
					pp.add(p1);
				}
				p.add(pp);
			}
			OKDialog dia = new OKDialog(CManager.getAnyFrame(), res.getString("New_map_layer"), true);
			dia.addContent(p);
			dia.show();
			if (dia.wasCancelled())
				return null;
			layerName = tf.getText().trim();
			if (layerName == null || layerName.length() < 1) {
				layerName = "USER LAYER";
			}
			if (cbPoint.getState()) {
				layerType = Geometry.point;
			} else if (cbLine.getState()) {
				layerType = Geometry.line;
			} else {
				layerType = Geometry.area;
			}
			if (extent == null) {
				float rect[] = new float[4];
				boolean error = false;
				for (int i = 0; i < 4 && !error; i++) {
					String str = tfExt[i].getText();
					if (str == null) {
						error = true;
					} else {
						str = str.trim();
						if (str.length() < 1) {
							error = true;
						} else {
							try {
								rect[i] = Float.valueOf(str).floatValue();
							} catch (NumberFormatException nfe) {
								error = true;
							}
						}
					}
				}
				error = error || rect[2] <= rect[0] || rect[3] <= rect[1];
				if (!error) {
					extent = new RealRectangle(rect);
				} else {
					extent = new RealRectangle(0f, 0f, 100f, 100f);
				}
			}
			layer = new DGeoLayer();
			layer.setType(layerType);
			layer.setName(layerName);
			layer.setHasAllObjects(true);
			layer.getDrawingParameters().drawLabels = true;
			layer.setWholeLayerBounds(extent);
			core.getDataLoader().addMapLayer(layer, -1);
		}
		DataPipeSpec dataPipeSpec = new DataPipeSpec();
		dataPipeSpec.dataSource = dataURL.toString();
		dataPipeSpec.layerId = layer.getContainerIdentifier();
		//Construct and return an instance of DataPipeListener
		DataPipeListener dpl = new DataPipeListener(dataPipeSpec, layer, mapN, core);
		return dpl;
	}

	/**
	* Updates the vector of datapipe specifications in the system using the
	* specifications for currently existing datapipes. Destroyed datapipes
	* are previously removed.
	*/
	protected void updateSystemSettings() {
		Vector pipes = null;
		Object obj = core.getSystemSettings().getParameter("DATAPIPE");
		if (obj != null && (obj instanceof Vector)) {
			pipes = (Vector) obj;
		}
		if (pipes != null) {
			pipes.removeAllElements();
		}
		removeDestroyedListeners();
		if (dpListeners != null && dpListeners.size() > 0) {
			if (pipes == null) {
				pipes = new Vector(5, 5);
				core.getSystemSettings().setParameter("DATAPIPE", pipes);
			}
			for (int i = 0; i < dpListeners.size(); i++) {
				DataPipeListener dpl = (DataPipeListener) dpListeners.elementAt(i);
				pipes.addElement(dpl.getDataPipeSpecification());
			}
		}
	}

	/**
	* Checks existing datapipes if new data entries appeared.
	*/
	protected void startDataPipeChecking() {
		if (dpThread != null)
			return; //previous checking haven't finished
		if (dpListeners == null || dpListeners.size() < 1)
			return;
		//if some of the listeners is not idle, return immediately
		for (int i = 0; i < dpListeners.size(); i++) {
			DataPipeListener dpl = (DataPipeListener) dpListeners.elementAt(i);
			if (dpl.isActive() && dpl.getStatus() != DataPipeListener.IDLE)
				return;
		}
		//make a list of existing datapipes
		Vector pipeURLs = new Vector(dpListeners.size(), 1);
		for (int i = 0; i < dpListeners.size(); i++) {
			DataPipeListener dpl = (DataPipeListener) dpListeners.elementAt(i);
			if (dpl.isActive()) {
				DataPipeSpec dps = dpl.getDataPipeSpecification();
				if (dps != null && dps.dataSource != null && !pipeURLs.contains(dps.dataSource)) {
					pipeURLs.addElement(dps.dataSource);
				}
			}
		}
		if (pipeURLs.size() < 1)
			return;
		for (int i = 0; i < pipeURLs.size(); i++) {
			Vector newEntries = getNewData((String) pipeURLs.elementAt(i));
			if (newEntries != null && newEntries.size() > 0) {
				dpThread = new DataPipeThread(this, (String) pipeURLs.elementAt(i), newEntries);
				dpThread.start();
				return;
			}
		}
	}

	/**
	* Tries to read data from the URL specified. Returns a vector of new entries
	* received or null. Each entry, in its turn, is a vector of pairs tag+value.
	*/
	public Vector getNewData(String urlString) {
		if (urlString == null)
			return null;
		System.out.println("Trying to get new entries from " + urlString);
		URL dataURL = null;
		try {
			dataURL = new URL(urlString);
			System.out.println("Constructed the URL: " + dataURL);
		} catch (MalformedURLException mfe) {
			core.getUI().showMessage(mfe.toString(), true);
			System.out.println("Failed to construct the URL: " + mfe);
			return null;
		}
		if (dataURL.getProtocol().startsWith("file") || dataURL.getProtocol().startsWith("FILE")) {
			String filePath = dataURL.getFile();
			File file = new File(filePath);
			try {
				if (!file.exists())
					return null;
			} catch (Throwable e) {
			}
			long fileLastModified = -1;
			int idx = -1;
			if (entryFiles != null) {
				idx = entryFiles.indexOf(filePath);
			}
			if (idx >= 0) {
				fileLastModified = ((Long) modTimes.elementAt(idx)).longValue();
			}
			try {
				long modTime = file.lastModified();
				if (modTime == fileLastModified)
					return null;
				fileLastModified = modTime;
				if (idx < 0) {
					if (entryFiles == null) {
						entryFiles = new Vector(5, 5);
						modTimes = new Vector(5, 5);
					}
					entryFiles.addElement(filePath);
					modTimes.addElement(new Long(fileLastModified));
				} else {
					modTimes.setElementAt(new Long(fileLastModified), idx);
				}
			} catch (Throwable e) {
			}
		}
		InputStream stream = null;
		try {
			//System.out.println("...opening URL connection...");
			URLConnection urlc = dataURL.openConnection();
			urlc.setUseCaches(false);
			//System.out.println("...getting the input stream...");
			stream = urlc.getInputStream();
			/*
			if (stream!=null)
			  System.out.println("The input stream got!");
			else
			  System.out.println("The input stream is null!");
			*/
		} catch (IOException ioe) {
			core.getUI().showMessage(ioe.toString(), true);
			System.out.println("Failed to open the input stream: " + ioe);
			return null;
		}
		if (stream == null)
			return null;
		BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
		Vector entriesGot = null;
		Vector currentEntry = null;
		boolean error = false;
		//System.out.println("...reading the input stream...");
		while (true) {
			try {
				String str = reader.readLine();
				if (str == null) {
					break;
				}
				str = StringUtil.decodeWebString(str);
				//System.out.println("["+str+"]");
				str = str.trim();
				if (str.length() < 1) {
					continue;
				}
				if (str.equalsIgnoreCase("<EOF>")) {
					break;
				}
				if (str.equalsIgnoreCase("</ENTRY>") || str.equalsIgnoreCase("<ENTRY>")) {
					if (currentEntry != null && currentEntry.size() > 0) {
						if (entriesGot == null) {
							entriesGot = new Vector(5, 5);
						}
						entriesGot.addElement(currentEntry);
					}
					currentEntry = null;
					if (str.equalsIgnoreCase("<ENTRY>")) {
						currentEntry = new Vector(20, 10);
					}
				} else if (currentEntry == null) {
					continue;
				} else {
					Vector tokens = StringUtil.getNames(str, "=");
					if (tokens == null || tokens.size() < 2) {
						continue;
					}
					String pair[] = { (String) tokens.elementAt(0), (String) tokens.elementAt(1) };
					currentEntry.addElement(pair);
					//System.out.println(pair);
				}
			} catch (EOFException eof) {
				break;
			} catch (IOException ioe) {
				core.getUI().showMessage(ioe.toString(), true);
				break;
			}
		}
		try {
			stream.close();
		} catch (IOException ioe) {
		}
		return entriesGot;
	}

	private DGeoLayer lastSelectedLayer = null;

	/**
	* A method from the DataReceiver interface. The interface was introduced
	* to avoid cyclic references between the classes DataPipesManager and
	* DataPipeThread. The method is started from a DataPipeThread.
	* In this method the DataPipesManager starts a dialog
	* proposing the user to georeference the new data. The dialog needs to
	* be started using a thread because the Java plugin hangs when some dialog
	* is started directly from the method * windowActivated of the WindowListener
	* interface.
	*/
	public void newDataReceived(String dataURL, Vector entriesGot) {
		if (dpListeners == null || dataURL == null || entriesGot == null || entriesGot.size() < 1) {
			dpThread = null;
			return;
		}
		//what listeners listen to this URL? (may be >1 !!!)
		Vector list = null;
		int activeIdx = 0;
		if (dpListeners.size() < 2) {
			list = dpListeners;
		} else {
			list = new Vector(dpListeners.size(), 1);
			for (int i = 0; i < dpListeners.size(); i++) {
				DataPipeListener dpl = (DataPipeListener) dpListeners.elementAt(i);
				if (dpl.isActive()) {
					DataPipeSpec dps = dpl.getDataPipeSpecification();
					if (dps != null && dps.dataSource != null && dataURL.equals(dps.dataSource)) {
						list.addElement(dpl);
						if (lastSelectedLayer != null && lastSelectedLayer.equals(dpl.getLayer())) {
							activeIdx = list.size() - 1;
						}
					}
				}
			}
		}
		if (list.size() < 1) {
			dpThread = null;
			return;
		}
		String etxt = (entriesGot.size() == 1) ? res.getString("entry") : res.getString("entries");
		String txt = String.valueOf(entriesGot.size()) + res.getString("new") + etxt;
		core.getUI().showMessage(res.getString("Got") + txt + res.getString("from") + dataURL, false);
		Panel p = new Panel(new ColumnLayout());
		TextCanvas tc = new TextCanvas();
		tc.addTextLine(txt + res.getString("have_come_from") + dataURL + ".");
		if (list.size() > 1) {
			String article = (entriesGot.size() == 1) ? res.getString("The1") : res.getString("The2");
			tc.addTextLine(article + " " + etxt + res.getString("may_be_added_to_one_of") + list.size() + " " + res.getString("layers") + ".");
		}
		p.add(tc);
		Checkbox lcb[] = null;
		CheckboxGroup cbg = null;
		if (list.size() > 1) {
			p.add(new Label(res.getString("Select_the_layer") + //"Select the layer for adding "
					((entriesGot.size() == 1) ? res.getString("the_new_entry") : res.getString("the_new_entries")) + ":"));
			cbg = new CheckboxGroup();
			lcb = new Checkbox[list.size()];
			for (int i = 0; i < list.size(); i++) {
				DataPipeListener dpl = (DataPipeListener) list.elementAt(i);
				lcb[i] = new Checkbox(dpl.getLayer().getName(), i == activeIdx, cbg);
				p.add(lcb[i]);
				int k = dpl.getPendingEntriesCount();
				if (k > 0) {
					p.add(new Label("(" + String.valueOf(k) + ((k == 1) ? res.getString("prev_entry_pending)") : res.getString("prev_entries_pending)")), Label.RIGHT));
				}
			}
			p.add(new Line(false));
		}
		cbg = new CheckboxGroup();
		Checkbox enterCB = new Checkbox(res.getString("georeference_the_data"), true, cbg);
		p.add(enterCB);
		Checkbox laterCB = new Checkbox(res.getString("postpone"), false, cbg);
		p.add(laterCB);
		OKDialog okd = new OKDialog(CManager.getAnyFrame(mainWindow), res.getString("Data_input"), false);
		okd.addContent(p);
		okd.show();
		int idx = 0;
		if (list.size() > 1) {
			for (int i = 0; i < list.size(); i++)
				if (lcb[i].getState()) {
					idx = i;
					break;
				}
		}
		DataPipeListener dpl = (DataPipeListener) list.elementAt(idx);
		lastSelectedLayer = dpl.getLayer();
		dpl.addNewData(entriesGot);
		if (enterCB.getState()) {
			dpl.startGeoreferencing();
		}
		dpThread = null;
	}

	/**
	* When the main window of the system is activated, starts looking in the
	* datapipes for new data.
	*/
	public void windowActivated(WindowEvent e) {
		if (!e.getSource().equals(mainWindow))
			return;
		if (dpThread != null)
			return; //previous checking haven't finished
		removeDestroyedListeners();
		//Check if all the datapipe listeners are currently idle. Only in this case
		//looking for new data may start.
		if (dpListeners != null) {
			for (int i = 0; i < dpListeners.size(); i++) {
				DataPipeListener dpl = (DataPipeListener) dpListeners.elementAt(i);
				int status = dpl.getStatus();
				if (status != DataPipeListener.IDLE && status != DataPipeListener.INACTIVE)
					return;
			}
		}
		//Looking into the datapipes must not occur too often. Therefore the manager
		//checks if the time interval since last checking is long enough.
		Calendar c = Calendar.getInstance();
		long time = c.getTime().getTime();
		if (lastCheckTime < 0 || time - lastCheckTime >= delay) {
			startDataPipeChecking();
			lastCheckTime = c.getTime().getTime();
		}
	}

	public void windowClosing(WindowEvent e) {
		if (!e.getSource().equals(mainWindow))
			return;
		//If any datapipe listener has unstored objects, propose the user to store
		//them before quitting the system.
		//...
		mainWindow.removeWindowListener(this);
		mainWindow = null;
	}

	public void windowOpened(WindowEvent e) {
	}

	public void windowClosed(WindowEvent e) {
	}

	public void windowIconified(WindowEvent e) {
	}

	public void windowDeiconified(WindowEvent e) {
	}

	public void windowDeactivated(WindowEvent e) {
	}

}
