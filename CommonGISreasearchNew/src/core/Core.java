package core;

import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Vector;

import spade.analysis.calc.CalcManager;
import spade.analysis.system.ActionLogger;
import spade.analysis.system.DataKeeper;
import spade.analysis.system.DataLoader;
import spade.analysis.system.DisplayProducer;
import spade.analysis.system.ESDACore;
import spade.analysis.system.Processor;
import spade.analysis.system.Supervisor;
import spade.analysis.system.SystemUI;
import spade.analysis.system.WindowManager;
import spade.analysis.tools.DataAnalyser;
import spade.analysis.tools.ExtraToolManager;
import spade.lib.basicwin.Dialogs;
import spade.lib.page_util.PageCollection;
import spade.lib.page_util.PageMaker;
import spade.lib.util.EntitySetIdManager;
import spade.lib.util.Parameters;
import spade.lib.util.StringUtil;
import spade.vis.action.Highlighter;
import spade.vis.database.AttributeDataPortion;
import spade.vis.dmap.DLayerManager;
import spade.vis.map.MapViewer;
import spade.vis.space.LayerManager;
import ui.SnapshotManager;
import data_load.DataManager;

public class Core implements ESDACore, ActionLogger, PropertyChangeListener {
	protected SupervisorImplement supervisor = null;
	protected DisplayProducer displayProducer = null;
	protected SystemUI ui = null;
	protected DataManager dataLoader = null;
	/**
	* Used for calculations in tables
	*/
	protected CalcManager calcMan = null;
//ID
	protected SnapshotManager snapshotManager = null;
//~ID
	/**
	* Used for keeping references to additional analysis tools such as
	* the calculation manager. The vector consists of 2-element arrays in
	* which the first element is the tool identifier (must be unique within the
	* vector) and the second is the tool (as Object)
	*/
	protected Vector tools = null;
	/**
	 * A register of available processors - any kinds of tools capable of using some
	 * of earlier obtained analysis results for further analysis.
	 * A Processor is created when some results suitable for further use
	 * are obtained. It is registered at the system's core and can be accessed from
	 * there for use.
	 */
	protected Vector processors = null;
	/**
	 * Contains significant actions accomplished in the process of analysis
	 */
	protected Vector<ActionDescr> loggedActions = null;
	/**
	 * Used for generation of HTML pages with analysis results, logged actions, etc.
	 */
	protected PageMaker pageMaker = null;
	/**
	 * Indicates whether the user may be asked about a comment before
	 * producing a page or collection of pages
	 */
	protected boolean askForComments = true;

	public Core() {
		supervisor = new SupervisorImplement();
		supervisor.setActionLogger(this);
		dataLoader = new DataManager();
		dataLoader.setSupervisor(supervisor);
		dataLoader.addPropertyChangeListener(this);
		supervisor.getSuperHighlighter().setEntitySetIdManager(dataLoader.getEntitySetIdManager());
		DisplayProducerImplement dpi = new DisplayProducerImplement();
		displayProducer = dpi;
		dpi.setSupervisor(supervisor);
		supervisor.setWindowManager(dpi);
//ID
		snapshotManager = new SnapshotManager(getSupervisor(), getDataKeeper(), getDisplayProducer(), getWindowManager());
//~ID
	}

	/**
	* Constructs the optional components of the system. Checks the availability
	* of the classes and validity of the components before including them into
	* the system.
	*/
	public void makeOptionalComponents() {
		try {
			calcMan = (CalcManager) Class.forName("spade.analysis.calc.CalcManagerImpl").newInstance();
		} catch (Exception e) {
			System.out.println("Cannot construct a CalcManagerImpl: " + e.toString());
		}
		if (calcMan != null) {
			calcMan.setSupervisor(supervisor);
			calcMan.setDisplayProducer(displayProducer);
			addTool("CalcManager", calcMan);
		}
		try {
			DataAnalyser geoToolMan = (DataAnalyser) Class.forName("spade.analysis.geocomp.GeoToolManager").newInstance();
			if (geoToolMan.isValid(this)) {
				addTool("GeoCalcManager", geoToolMan);
			}
		} catch (Exception e) {
			System.out.println("Cannot construct a GeoToolManager: " + e.toString());
		}
		ExtraToolManager extraMan = new ExtraToolManager();
		if (extraMan.getAvailableToolCount(this) > 0) {
			addTool("ExtraToolManager", extraMan);
		}
	}

	@Override
	public Supervisor getSupervisor() {
		return supervisor;
	}

	@Override
	public DisplayProducer getDisplayProducer() {
		return displayProducer;
	}

	/**
	* The WindowManager is used for registering all windows (frames) opened
	* during the session. It cares about closing all windows at the end of the
	* session.
	*/
	@Override
	public WindowManager getWindowManager() {
		return (WindowManager) displayProducer;
	}

	/**
	* Returns the calculation manager used for computations in tables
	*/
	public CalcManager getCalcManager() {
		return calcMan;
	}

	/**
	* Returns actual system settings (valid for the currently running instance
	* of the system). Takes the settings from the supervisor.
	*/
	@Override
	public Parameters getSystemSettings() {
		return supervisor.getSystemSettings();
	}

	@Override
	public void setSystemSettings(Parameters sysParam) {
		supervisor.setSystemSettings(sysParam);
		if (sysParam != null) {
			sysParam.setParameter("core", this);
		}
	}

	/**
	* Registers an additional component for analysis, e.g. the calculation manager.
	* A reference to this component can be got using the method getTool. Each tool
	* (component) must have its unique identifier.
	*/
	@Override
	public void addTool(String toolId, Object tool) {
		if (toolId != null && tool != null) {
			if (tools == null) {
				tools = new Vector(5, 5);
			}
			Object pair[] = new Object[2];
			pair[0] = toolId;
			pair[1] = tool;
			tools.addElement(pair);
		}
	}

	/**
	* Returns a reference to the tool with the given identifier, if available.
	* Here are some valid identifiers:
	* "CalcManager" - for the component performing computations in tables
	* "GeoCalcManager" - for the component performing geocomputations, i.e.
	*                    computations that involve or produce map layers
	* "ExtraToolManager" - for the component managing additional analysis tools
	*                      extending the core functionality of the system
	*/
	@Override
	public Object getTool(String toolId) {
		if (tools == null || toolId == null)
			return null;
		for (int i = 0; i < tools.size(); i++) {
			Object pair[] = (Object[]) tools.elementAt(i);
			if (pair[0].equals(toolId))
				return pair[1];
		}
		return null;
	}

	/**
	* Tries to run the tool with the given identifier, if available and can be
	* started. Returns true if the tool was found and started.
	* Runs only tools implementing the interface DataAnalyser
	*/
	@Override
	public boolean runTool(String toolId) {
		Object tool = getTool(toolId);
		if (tool != null && (tool instanceof DataAnalyser)) {
			((DataAnalyser) tool).run(this);
			return true;
		}
		return false;
	}

	/**
	 * Registers a Processor - any kind of tool capable of using some
	 * of earlier obtained analysis results for further analysis.
	 * A Processor is created when some results suitable for further use
	 * are obtained. It is registered at the system's core and can be accessed from
	 * there for use.
	 */
	@Override
	public void registerProcessor(Processor processor) {
		if (processor == null)
			return;
		String name = processor.getName();
		if (name == null) {
			name = "Processor";
			processor.setName(name);
		}
		if (processors == null) {
			processors = new Vector(10, 10);
		} else {
			Vector<String> names = new Vector<String>(processors.size(), 1);
			for (int i = 0; i < processors.size(); i++) {
				names.addElement(((Processor) processors.elementAt(i)).getName());
			}
			if (StringUtil.isStringInVectorIgnoreCase(name, names)) {
				String base = name;
				int k = 2;
				do {
					name = base + " (" + k + ")";
					++k;
				} while (StringUtil.isStringInVectorIgnoreCase(name, names));
				processor.setName(name);
			}
		}
		processors.addElement(processor);
	}

	/**
	 * Returns currently registered processors. The elements of the vector
	 * are instances of Processor (i.e. implement the interface Processor).
	 */
	@Override
	public Vector getAvailableProcessors() {
		return processors;
	}

	/**
	 * Returns currently registered processors applicable to the given type
	 * of objects. The possible types of objects are defined in the interface Processor.
	 * The elements of the vector are instances of Processor
	 * (i.e. implement the interface Processor).
	 */
	@Override
	public Vector getProcessorsForObjectType(int objType) {
		if (processors == null || processors.size() < 1)
			return null;
		Vector v = new Vector(processors.size(), 1);
		for (int i = 0; i < processors.size(); i++)
			if (((Processor) processors.elementAt(i)).isApplicableTo(objType)) {
				v.addElement(processors.elementAt(i));
			}
		if (v.size() < 1)
			return null;
		return v;
	}

	/**
	* Returns the DataKeeper that keeps all available tables and maps.
	* In this implementation this is a DataManager.
	*/
	@Override
	public DataKeeper getDataKeeper() {
		return dataLoader;
	}

	/**
	* Returns the DataLoader used for loading or adding tables and maps to the
	* system. In this implementation this is a DataManager (it is at the same
	* time a DataKeeper)
	*/
	@Override
	public DataLoader getDataLoader() {
		return dataLoader;
	}

	/**
	* There may be several tables and/or map layers, and for each of them an
	* individual highlighter is created. This method returns the highlighter
	* corresponding to the set of objects with the given identifier.
	* Set identifiers are used for linking tables to layers. The set identifier
	* of a table is not the same as the table identifier (container identifier).
	*/
	@Override
	public Highlighter getHighlighterForSet(String setId) {
		return supervisor.getHighlighter(setId);
	}

	/**
	* There may be several tables and/or map layers, and for each of them an
	* individual highlighter is created. This method returns the highlighter
	* for the container (e.g. table or map layer) with the given identifier.
	* The container identifier is not the same as the identifier of the
	* object set this container refers to.
	*/
	@Override
	public Highlighter getHighlighterForContainer(String containerId) {
		EntitySetIdManager sman = dataLoader.getEntitySetIdManager();
		if (sman == null)
			return null;
		String setId = sman.getEntitySetIdentifier(containerId);
		if (setId == null)
			return null;
		return getHighlighterForSet(setId);
	}

	/**
	* Makes the the specified objects selected (persistently highlighted).
	* The objects are specified through their identifiers and belong to the
	* specified container.
	* "source" is the object that initializes highlighting.
	*/
	@Override
	public void selectObjects(Object source, String containerId, Vector objIds) {
		EntitySetIdManager sman = dataLoader.getEntitySetIdManager();
		if (sman == null)
			return;
		String setId = sman.getEntitySetIdentifier(containerId);
		if (setId == null)
			return;
		Highlighter hl = getHighlighterForSet(setId);
		if (hl == null)
			return;
		if (objIds != null && objIds.size() > 0) {
			int mapN = 0;
			if (ui != null) {
				mapN = ui.getCurrentMapN();
			}
			LayerManager lman = dataLoader.getMap(mapN);
			if (lman != null) {
				int lidx = -1;
				for (int i = 0; i < lman.getLayerCount() && lidx < 0; i++)
					if (setId.equals(lman.getGeoLayer(i).getEntitySetIdentifier())) {
						lidx = i;
					}
				if (lidx >= 0 && lidx != lman.getIndexOfActiveLayer()) {
					lman.activateLayer(lidx);
				}
			}
		}
		hl.replaceSelectedObjects(source, objIds);
	}

	/**
	* Tries to make the specified objects selected (persistently highlighted).
	* The objects are specified through their identifiers. The container
	* the objects belong to is unknown.
	* In this implementation the first available table is selected.
	* "source" is the object that initializes highlighting.
	*/
	@Override
	public void selectObjects(Object source, Vector objIds) {
		if (dataLoader.getTableCount() > 0) {
			selectObjects(source, dataLoader.getTable(0).getContainerIdentifier(), objIds);
		}
	}

	/**
	* Sets the system UI (the actual implementation of this interface depends
	* on the current system configuration)
	*/
	@Override
	public void setUI(SystemUI ui) {
		this.ui = ui;
		supervisor.setUI(ui);
		dataLoader.setUI(ui);
		((DisplayProducerImplement) displayProducer).setUI(ui);
	}

	/**
	* Returns the system UI that can, in particular, display status messages
	*/
	@Override
	public SystemUI getUI() {
		return ui;
	}

	/**
	* Removes the table with the given identifier from the system.
	*/
	@Override
	public void removeTable(String tableId) {
		if (tableId == null)
			return;
		int idx = dataLoader.getTableIndex(tableId);
		if (idx < 0)
			return;
		dataLoader.removeTable(idx);
		displayProducer.tableIsRemoved(tableId);
		supervisor.tableIsRemoved(tableId);
		if (calcMan != null) {
			calcMan.tableIsRemoved(tableId);
		}
		if (ui != null && dataLoader.getMapCount() > 0) {
			for (int i = 0; i < dataLoader.getMapCount(); i++)
				if (ui.getMapViewer(i) != null) {
					ui.getMapViewer(i).validateView();
				}
		}
	}

	/**
	* Removes the map layer with the given identifier from the system.
	* The second argument indicates whether the table associated with this layer
	* must be also removed
	*/
	@Override
	public void removeMapLayer(String layerId, boolean removeThematicData) {
		if (layerId == null || dataLoader.getMapCount() < 1)
			return;
		if (removeThematicData) {
			removeTable(dataLoader.getLinkedTableId(layerId));
		}
		dataLoader.removeMapLayer(layerId);
	}

	/**
	* Removes the map with the given index from the system, i.e. all the layers
	* composing this map. The second argument indicates whether the tables
	* associated with the layers must be removed as well.
	*/
	@Override
	public void removeMap(int mapN, boolean removeThematicData) {
		LayerManager lman = dataLoader.getMap(mapN);
		if (lman == null)
			return;
		if (ui != null) {
			ui.closeMapView(mapN);
		}
		if (removeThematicData) {
			for (int i = 0; i < lman.getLayerCount(); i++)
				if (lman.getGeoLayer(i) != null && lman.getGeoLayer(i).getThematicData() != null) {
					removeTable(lman.getGeoLayer(i).getThematicData().getContainerIdentifier());
				}
		}
		dataLoader.removeMap(mapN);
	}

	/**
	* Removes all currently loaded data and closes all data displays
	*/
	@Override
	public void removeAllData() {
		displayProducer.closeAllTools();
		if (ui != null) {
			for (int i = 0; i < dataLoader.getMapCount(); i++) {
				ui.closeMapView(i);
			}
		}
		for (int i = dataLoader.getMapCount() - 1; i >= 0; i--) {
			dataLoader.removeMap(i);
		}
		for (int i = dataLoader.getTableCount() - 1; i >= 0; i--) {
			String tableId = dataLoader.getTable(i).getContainerIdentifier();
			dataLoader.removeTable(i);
			displayProducer.tableIsRemoved(tableId);
			supervisor.tableIsRemoved(tableId);
			if (calcMan != null) {
				calcMan.tableIsRemoved(tableId);
			}
		}
		dataLoader.clearAll();
		supervisor.getSystemSettings().setParameter("DATAPIPE", null);
		InstanceCounts.reset();
	}

	/**
	* Reacts to new tables or maps being loaded by the dataLoader
	*/
	@Override
	public void propertyChange(PropertyChangeEvent e) {
		if (e.getPropertyName().equals("map") && ui != null) { //a new map loaded
			if (e.getNewValue() instanceof Integer) {
				int mapN = ((Integer) e.getNewValue()).intValue();
				if (mapN == 0 && dataLoader.getApplicationPath() == null) {
					//ask the user if the data are in geographical coordinates
					LayerManager lman = dataLoader.getMap(0);
					if (!(lman instanceof DLayerManager) || ((DLayerManager) lman).getCoordsAreGeographic() < 0) {
						boolean geo = Dialogs.askYesOrNo(ui.getMainFrame(), "Are the coordinates in the data geographic (latitudes and longitudes)?", "Geographic coordinates?");
						lman.setGeographic(geo);
						if (geo) {
							lman.setUserUnit("degree");
						}
					}
				}
				if (mapN >= 0) {
					ui.openMapView(mapN);
				}
			}
		} else if ((e.getSource() instanceof AttributeDataPortion) && e.getPropertyName().equals("structure_complete")) {
			AttributeDataPortion table = (AttributeDataPortion) e.getSource();
			table.removePropertyChangeListener(this);
			String tableId = table.getContainerIdentifier();
			displayProducer.tableIsRemoved(tableId);
			supervisor.tableIsRemoved(tableId);
			if (calcMan != null) {
				calcMan.tableIsRemoved(tableId);
			}
			WindowManager wm = getWindowManager();
			if (wm != null && wm.getWindowCount() > 0) {
				for (int i = 0; i < wm.getWindowCount(); i++)
					if (wm.getWindow(i) instanceof Frame) {
						Frame w = (Frame) wm.getWindow(i);
						MapViewer mv = findMapView(w);
						if (mv != null) {
							mv.validateView();
						}
					}
			}
			if (ui != null && ui.getMainFrame() != null) {
				MapViewer mv = findMapView(ui.getMainFrame());
				if (mv != null) {
					mv.validateView();
				}
			}
		}
	}

	protected MapViewer findMapView(Container c) {
		if (c == null)
			return null;
		if (c instanceof MapViewer)
			return (MapViewer) c;
		for (int i = 0; i < c.getComponentCount(); i++) {
			Component comp = c.getComponent(i);
			if (comp instanceof MapViewer)
				return (MapViewer) comp;
		}
		MapViewer mv = null;
		for (int i = 0; i < c.getComponentCount() && mv == null; i++) {
			Component comp = c.getComponent(i);
			if (comp instanceof Container) {
				mv = findMapView((Container) comp);
			}
		}
		return mv;
	}

	/**
	* Makes a full URL specification string from a relative one taking into
	* account the document base of the applet or, for a local variant, the
	* current working directory of the application.
	* The URL from the given path passed as the argument is formed in the
	* following way:
	* 1) If sourcePath already contains a protocol, i.e. http: or file:, it is
	*    returned without changing.
	* 2) If sourcePath starts with "/" or drive letter (i.e. this is an absolute
	*    path):
	*    2a) In a local variant the prefix "file://" is added to the sourcePath.
	*    2b) In an applet the protocol, the host name, and the port are taken
	*        from the document base of the applet, and the content of
	*        sourcePath is attached to them. If sourcePath contains a drive letter,
	*        it is removed.
	* 3) If sourcePath does not start with "/" or drive letter:
	*    3a) In a local variant the path to the current directory is attached to
	*        sourcePath at the beginning, and then the prefix "file://" is added.
	*    3b) In an applet the URL is formed by attaching the sourcePath to the
	*        document base of the applet.
	* Before analysing the content of sourcePath, the method replaces back slashes
	* by normal slashes ("/").
	*/
	@Override
	public String getFullURLString(String sourcePath) {
		if (sourcePath == null)
			return null;
		sourcePath = sourcePath.replace('\\', '/');
		int idx = sourcePath.indexOf(":");
		if (idx > 0) {
			String protocol = sourcePath.substring(0, idx).toLowerCase();
			if (protocol.equals("http") || protocol.equals("file"))
				return sourcePath;
		}
		Object dbObj = supervisor.getSystemSettings().getParameter("DocumentBase");
		if (dbObj != null && (dbObj instanceof URL)) {
			if (idx > 0) {
				sourcePath = sourcePath.substring(idx + 1);
			}
			URL docBase = (URL) dbObj;
			System.out.println("Document base:\n>>> protocol=[" + docBase.getProtocol() + "]\n>>> host=[" + docBase.getHost() + "]\n>>> port=" + docBase.getPort() + "\n>>> file=" + docBase.getFile());
			String urlPrefix = docBase.getProtocol() + "://" + docBase.getHost();
			if (docBase.getPort() >= 0) {
				urlPrefix += ":" + docBase.getPort();
			}
			if (sourcePath.startsWith("/"))
				return urlPrefix + sourcePath;
			String fname = docBase.getFile();
			idx = fname.lastIndexOf("/");
			if (idx > 0) {
				fname = fname.substring(0, idx + 1);
			} else {
				fname = fname + "/";
			}
			if (!fname.startsWith("/")) {
				fname = "/" + fname;
			}
			return urlPrefix + fname + sourcePath;
		}
		if (idx > 0 || sourcePath.startsWith("/"))
			//the absolute path is given; only the protocol is needed
			return "file://" + sourcePath;
		String workPath = java.lang.System.getProperty("user.dir");
		if (workPath == null)
			return "file://" + sourcePath;
		workPath = workPath.replace('\\', '/');
		if (!workPath.endsWith("/")) {
			workPath += "/";
		}
		if (workPath.charAt(0) == '/')
			return "file:" + workPath + sourcePath;
		return "file:/" + workPath + sourcePath;
	}

//ID
	/**
	 * Returns Snapshot Manager - the component dealing with saving and restoring of system states
	 */
	@Override
	public SnapshotManager getSnapshotManager() {
		return snapshotManager;
	}

//~ID
	/**
	 * Adds the given action description (must be instance of ActionDescr) to the actions log
	 */
	@Override
	public void logAction(Object actionDescr) {
		if (actionDescr != null && (actionDescr instanceof ActionDescr)) {
			logAction((ActionDescr) actionDescr);
		}
	}

	/**
	 * Adds the given action description to the actions log
	 */
	@Override
	public void logAction(ActionDescr action) {
		if (action == null)
			return;
		if (loggedActions == null) {
			loggedActions = new Vector<ActionDescr>(50, 50);
		}
		loggedActions.addElement(action);
	}

	@Override
	public void logSimpleAction(String action) {
		if (action == null)
			return;
		if (loggedActions == null) {
			loggedActions = new Vector<ActionDescr>(50, 50);
		}
		ActionDescr ad = new ActionDescr();
		ad.aName = action;
		loggedActions.addElement(ad);
	}

	/**
	 * Returns the log of the actions
	 */
	@Override
	public Vector<ActionDescr> getLoggedActions() {
		return loggedActions;
	}

	/**
	 * Prints the action log in the specified text file
	 */
	@Override
	public void printLoggedActions(String filePath) {
		if (loggedActions == null || loggedActions.size() < 1) {
			getUI().showMessage("No actions have been logged yet!", true);
			return;
		}
		try {
			FileOutputStream out = new FileOutputStream(filePath);
			DataOutputStream dos = new DataOutputStream(out);
			for (int i = 0; i < loggedActions.size(); i++) {
				dos.writeBytes(loggedActions.elementAt(i).getDescription() + "\r\n");
			}
			out.close();
		} catch (IOException ioe) {
			getUI().showMessage(ioe.toString(), true);
			System.err.println(ioe.toString());
		}
	}

	/**
	 * Generates HTML pages with the content specified in the given structure.
	 */
	@Override
	public void makePages(PageCollection pages) {
		if (pageMaker == null) {
			pageMaker = new PageMaker(this);
		}
		pageMaker.makePages(pages);
	}

	/**
	 * updates current date and time in PageMaker and
	 * returns them as human-readable string and as filename prefix
	 */
	@Override
	public String[] updateDTinPageMaker() {
		if (pageMaker == null) {
			pageMaker = new PageMaker(this);
		}
		pageMaker.composeDTstr();
		String result[] = new String[3];
		result[0] = pageMaker.getDTstring();
		result[1] = pageMaker.getDTfname();
		result[2] = pageMaker.getLFpath();
		return result;
	}

	/**
	 * Indicates whether the user may be asked about a comment before
	 * producing a page or collection of pages
	 */
	@Override
	public boolean mayAskForComments() {
		return askForComments;
	}

	/**
	 * Sets whether the user may be asked about a comment before
	 * producing a page or collection of pages
	 */
	@Override
	public void setMayAskForComments(boolean askForComments) {
		this.askForComments = askForComments;
	}
}
