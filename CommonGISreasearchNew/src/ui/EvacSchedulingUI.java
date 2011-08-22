package ui;

import help.DescartesHelpIndex;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.plot.Plot;
import spade.analysis.plot.QueryOrSearchTool;
import spade.analysis.system.DataKeeper;
import spade.analysis.system.DataLoader;
import spade.analysis.system.ESDACore;
import spade.analysis.system.Supervisor;
import spade.analysis.system.SystemUI;
import spade.analysis.tools.DataAnalyser;
import spade.analysis.tools.schedule.ScheduleToolsManager;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Destroyable;
import spade.lib.basicwin.GetPathDlg;
import spade.lib.basicwin.MenuConstructor;
import spade.lib.basicwin.NotificationLine;
import spade.lib.basicwin.OKDialog;
import spade.lib.basicwin.PopupManager;
import spade.lib.basicwin.SelectDialog;
import spade.lib.help.Helper;
import spade.lib.lang.Language;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.ObjectContainer;
import spade.vis.map.MapViewer;
import spade.vis.space.GeoLayer;
import spade.vis.space.LayerManager;
import data_load.DataLoadUI;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: 17-Oct-2007
 * Time: 16:41:28
 * A specialised UI for evacuation scheduling
 */
public class EvacSchedulingUI extends Panel implements SystemUI, ActionListener, Destroyable {
	static ResourceBundle res = Language.getTextResource("ui.SchedulerTexts_ui");
	/**
	* The core provides references to the supervisor, display producer, data
	* keeper etc.
	*/
	protected ESDACore core = null;
	protected SimpleMapView mapView = null;
	protected NotificationLine lStatus = null;
	protected String defaultMessage = res.getString("ev_sched");
	/**
	* The panel has no direct access to the system's menu. It manages the menu
	* through the MenuConstructor.
	*/
	protected MenuConstructor menuConst = null;

	protected Menu helpMenu = null, toolMenu = null;
	/**
	* Used for data export
	*/
	protected DataAnalyser dataExporter = null;
	/**
	* Used for selection of a table for data visualization etc.
	*/
	protected TableManager tman = null;
	/**
	 * Supports the use of the schedule-specific tools
	 */
	protected ScheduleToolsManager scheduleToolsManager = null;

	protected boolean destroyed = false;

	public EvacSchedulingUI(MenuConstructor constructor) {
		super();
		setLayout(new BorderLayout());
		lStatus = new NotificationLine(defaultMessage);
		add(lStatus, BorderLayout.SOUTH);
		setMenuConstructor(constructor);
	}

	public void setMenuConstructor(MenuConstructor constructor) {
		menuConst = constructor;
		if (menuConst instanceof Frame) {
			Frame fr = (Frame) menuConst;
			fr.setTitle(defaultMessage);
			Dimension ss = getToolkit().getScreenSize();
			int w = ss.width * 2 / 3, h = ss.height * 2 / 3;
			fr.setSize(w, h);
			fr.setLocation(1, ss.height - h - 30);
		}
	}

	public void startWork(ESDACore core, String configFileName) {
		this.core = core;
		core.setUI(this);
		Helper.setHelpIndex(new DescartesHelpIndex());

		if (configFileName != null) {
			DataLoader dataLoader = core.getDataLoader();
			if (dataLoader.loadApplication(configFileName, null)) {
				String applName = core.getSystemSettings().getParameterAsString("APPL_NAME");
				if (applName != null) {
					if (getMainFrame() != null) {
						getMainFrame().setTitle(applName);
					}
					lStatus.setDefaultMessage(defaultMessage + ": " + applName);
				}
			}
		}
		tman = new TableManager();
		tman.setDataKeeper(core.getDataKeeper());
		tman.setUI(this);

		scheduleToolsManager = new ScheduleToolsManager(core);

		makeInterface();
	}

	public void startWork(ESDACore core) {
		startWork(core, null);
	}

	public void makeInterface() {
		if (menuConst == null)
			return;
		//"File" menu
		int idx = menuConst.addMenuItem(res.getString("File"), null, this, true);
		if (isShowing()) {
			CManager.validateAll(this);
		}
		Menu menu = menuConst.getMenu(idx);
		MenuItem mi = null;
		mi = menu.add(new MenuItem(res.getString("Load_project")));
		mi.setActionCommand("loadAppl");
		mi = menu.add(new MenuItem(res.getString("Load_data_")));
		mi.setActionCommand("loadData");
		menu.addSeparator();
		mi = menu.add(new MenuItem(res.getString("Save_project")));
		mi.setActionCommand("saveAppl");
		try {
			dataExporter = (DataAnalyser) Class.forName("export.ExportManager").newInstance();
			if (!dataExporter.isValid(core)) {
				dataExporter = null;
			}
		} catch (Exception e) {
		}
		if (dataExporter != null) {
			menu.addSeparator();
			mi = menu.add(new MenuItem(res.getString("Export_data")));
			mi.setActionCommand("export");
		}
		menu.addSeparator();
		mi = menu.add(new MenuItem(res.getString("Remove_table")));
		mi.setActionCommand("removeTable");
		mi = menu.add(new MenuItem(res.getString("Remove_map_layer")));
		mi.setActionCommand("removeLayer");
		mi = menu.add(new MenuItem(res.getString("Remove_all_data")));
		mi.setActionCommand("removeAll");
		menu.addSeparator();
		mi = menu.add(new MenuItem(res.getString("Print_")));
		mi.setActionCommand("print");
		mi = menu.add(new MenuItem(res.getString("Save_")));
		mi.setActionCommand("saveImages");
		menu.addSeparator();
		mi = menu.add(new MenuItem(res.getString("Quit")));
		mi.setActionCommand("quit");

		//specialised menu for the scheduling tools
		idx = menuConst.addMenuItem(scheduleToolsManager.getMenuTitle(), null, scheduleToolsManager, true);
		scheduleToolsManager.fillMenu(menuConst.getMenu(idx));

		//"Tools" menu
		int qsCount = core.getDisplayProducer().getQueryAndSearchToolCount();
		if (qsCount > 0) {
			idx = menuConst.addMenuItem(res.getString("Tools"), null, this, true);
			menu = toolMenu = menuConst.getMenu(idx);
			for (int i = 0; i < qsCount; i++) {
				mi = menu.add(new MenuItem(core.getDisplayProducer().getQueryOrSearchToolName(i)));
				mi.setActionCommand("_query_" + core.getDisplayProducer().getQueryOrSearchToolId(i));
			}
		}

		//"Display" menu
		idx = menuConst.addMenuItem(res.getString("Display"), null, this, true);
		menu = menuConst.getMenu(idx);
		mi = menu.add(new MenuItem(res.getString("Display_wizard_"), null));
		mi.setActionCommand("displayWizard");
		boolean tableViewerAvailable = false;
		try {
			Class c = Class.forName("spade.vis.dataview.TableWin");
			if (c != null) {
				tableViewerAvailable = true;
			}
		} catch (Exception e) {
		}
		if (tableViewerAvailable) {
			mi = menu.add(new MenuItem(res.getString("View_table"), null));
			mi.setActionCommand("displayTable");
		}
		menu.addSeparator();
		mi = menu.add(new MenuItem(res.getString("Clean_the_map"), null));
		mi.setActionCommand("cleanMap");

		if (isShowing()) {
			CManager.validateAll(this);
		}
	}

	/**
	 * Returns the object that supports the use of the schedule-specific tools
	 */
	public ScheduleToolsManager getScheduleToolsManager() {
		return scheduleToolsManager;
	}

	/**
	 * Processing of menu commands
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		lStatus.showMessage(null, false);
		PopupManager.hideWindow();
		String cmd = e.getActionCommand();
		if (cmd == null)
			return;
		if (cmd.equals("quit")) {
			quit();
		}

		if (cmd.equals("loadAppl")) {
			loadAppl();
			return;
		}
		if (cmd.equals("loadData")) {
			DataLoadUI dload = new DataLoadUI();
			dload.start(core.getDataLoader(), core.getUI());
			return;
		}
		if (cmd.equals("export")) {
			if (dataExporter != null) {
				dataExporter.run(core);
			}
			return;
		}
		if (cmd.equals("removeTable")) {
			TableData td = tman.selectCurrTable(res.getString("What_table_to_remove_"));
			if (td != null && td.table != null) {
				OKDialog dia = new OKDialog(CManager.getAnyFrame(this), res.getString("Confirm"), OKDialog.YES_NO_MODE, true);
				dia.addContent(new Label(res.getString("Remove_the_table") + td.table.getName() + "?"));
				dia.show();
				if (!dia.wasCancelled()) {
					core.removeTable(td.table.getContainerIdentifier());
				}
			}
			return;
		}
		if (cmd.equals("removeLayer")) {
			if (mapView == null)
				return;
			LayerManager lman = mapView.getLayerManager();
			int nlayers = lman.getLayerCount();
			if (nlayers < 1)
				return;
			Panel p = new Panel(new GridLayout(nlayers + 2, 1));
			p.add(new Label(res.getString("What_layer_to_remove_")));
			CheckboxGroup cbg = new CheckboxGroup();
			Checkbox cb[] = new Checkbox[nlayers];
			for (int i = 0; i < nlayers; i++) {
				cb[i] = new Checkbox(lman.getGeoLayer(i).getName(), false, cbg);
				p.add(cb[i]);
			}
			// following string: "remove the related table"
			Checkbox removeTable = new Checkbox(res.getString("remove_the_related"), false);
			p.add(removeTable);
			OKDialog dia = new OKDialog(CManager.getAnyFrame(this), res.getString("Remove_map_layer"), true);
			dia.addContent(p);
			dia.show();
			if (dia.wasCancelled())
				return;
			int k = -1;
			for (int i = 0; i < nlayers && k < 0; i++)
				if (cb[i].getState()) {
					k = i;
				}
			if (k >= 0) {
				dia = new OKDialog(CManager.getAnyFrame(this), res.getString("Confirm"), OKDialog.YES_NO_MODE, true);
				dia.addContent(new Label(res.getString("Remove_the_layer") + lman.getGeoLayer(k).getName() + ((removeTable.getState()) ? res.getString("and_the_related_table") : "?")));
				dia.show();
				if (!dia.wasCancelled()) {
					core.removeMapLayer(lman.getGeoLayer(k).getContainerIdentifier(), removeTable.getState());
				}
			}
			return;
		}
		if (cmd.equals("removeAll")) {
			if (core.getDataKeeper().getMapCount() < 1 && core.getDataKeeper().getTableCount() < 1)
				return; //nothing to remove
			OKDialog dia = new OKDialog(CManager.getAnyFrame(this), res.getString("Confirm"), OKDialog.YES_NO_MODE, true);
			dia.addContent(new Label(res.getString("Remove_all_the_data_")));
			dia.show();
			if (!dia.wasCancelled()) {
				core.removeAllData();
				core.getSystemSettings().setParameter("APPL_NAME", null);
				if (getMainFrame() != null) {
					getMainFrame().setTitle(defaultMessage);
				}
				lStatus.setDefaultMessage(defaultMessage);
			}
			return;
		}
		if (cmd.equals("saveAppl")) {
			core.getDataLoader().setCurrentMapN(0);
			core.getDataLoader().saveApplication();
			String applName = core.getSystemSettings().getParameterAsString("APPL_NAME");
			if (applName != null) {
				if (getMainFrame() != null) {
					getMainFrame().setTitle(applName);
				}
				lStatus.setDefaultMessage(defaultMessage + ": " + applName);
			}
			return;
		}
		if (cmd.equals("print")) {
			ImagePrinter printer = new ImagePrinter(core.getSupervisor());
			printer.chooseAndSaveOrPrint(true);
			return;
		}
		if (cmd.equals("saveImages")) {
			ImagePrinter printer = new ImagePrinter(core.getSupervisor());
			printer.chooseAndSaveOrPrint(false);
			return;
		}
		if (cmd.equals("displayTable")) {
			Class twClass = null;
			try {
				twClass = Class.forName("spade.vis.dataview.TableWin");
			} catch (Exception ex) {
			}
			if (twClass == null)
				return;
			QueryOrSearchTool tblView = null;
			try {
				Object obj = twClass.newInstance();
				if (obj == null)
					return;
				if (!(obj instanceof QueryOrSearchTool))
					return;
				tblView = (QueryOrSearchTool) obj;
			} catch (Exception ex) {
				return;
			}
			if (tblView == null)
				return;
			TableData td = tman.selectCurrTable(res.getString("Select_the_table_for2"));
			if (td == null)
				return;
			tblView.setObjectContainer((ObjectContainer) td.table);
			tblView.setSupervisor(core.getSupervisor());
			if (!tblView.construct()) {
				showMessage(tblView.getErrorMessage(), true);
				return;
			}
			core.getDisplayProducer().makeWindow((Component) tblView, ((Component) tblView).getName());
			return;
		}
		if (cmd.equals("displayWizard")) {
			new DisplayAssistant(core.getDataKeeper(), core.getDisplayProducer(), core.getSupervisor(), tman);
			return;
		}
		if (cmd.equals("cleanMap")) {
			if (mapView == null)
				return;
			LayerManager lm = mapView.getLayerManager();
			if (lm == null)
				return;
			Vector layers = new Vector(5, 5);
			for (int i = 0; i < lm.getLayerCount(); i++) {
				GeoLayer gl = lm.getGeoLayer(i);
				if (gl.getVisualizer() != null && gl.getVisualizer().getVisualizationName() != null) {
					layers.addElement(gl);
				}
			}
			if (layers.size() < 1)
				return;
			if (layers.size() == 1) {
				core.getDisplayProducer().eraseDataFromMap((GeoLayer) layers.elementAt(0), mapView);
				return;
			}
			Panel p = new Panel(new ColumnLayout());
			Checkbox cb[] = new Checkbox[layers.size()];
			for (int i = 0; i < layers.size(); i++) {
				GeoLayer gl = (GeoLayer) layers.elementAt(i);
				cb[i] = new Checkbox(gl.getName() + ": " + gl.getVisualizer().getVisualizationName());
				p.add(cb[i]);
			}
			OKDialog dialog = new OKDialog(CManager.getAnyFrame(this),
			// following string: "Select the layers to clean"
					res.getString("Select_the_layers_to"), true);
			dialog.addContent(p);
			dialog.show();
			if (dialog.wasCancelled())
				return;
			for (int i = 0; i < layers.size(); i++)
				if (cb[i].getState()) {
					core.getDisplayProducer().eraseDataFromMap((GeoLayer) layers.elementAt(i), mapView);
				}
			return;
		}
		if (cmd.startsWith("_query_")) {
			DataKeeper dKeeper = core.getDataKeeper();
			if (dKeeper == null)
				return;
			String toolId = cmd.substring(7);
			ObjectContainer objCont = null;
			boolean hasTables = dKeeper.getTableCount() > 0;
			if (core.getDisplayProducer().isToolAttributeFree(toolId)) {
				LayerManager lman = dKeeper.getMap(getCurrentMapN());
				boolean hasLayers = lman != null && lman.getLayerCount() > 0;
				if (!hasTables && !hasLayers)
					return;
				SelectDialog selDia = new SelectDialog(getMainFrame(), core.getDisplayProducer().getQueryOrSearchToolName(toolId), res.getString("Select_table_or_layer"));
				if (hasTables) {
					if (hasLayers) {
						selDia.addLabel(res.getString("tables") + ":");
					}
					for (int i = 0; i < dKeeper.getTableCount(); i++) {
						AttributeDataPortion table = dKeeper.getTable(i);
						selDia.addOption(table.getName(), table.getContainerIdentifier(), i == tman.getCurrentTableN());
					}
				}
				if (hasLayers) {
					if (hasTables) {
						selDia.addSeparator();
						selDia.addLabel(res.getString("layers") + ":");
					}
					for (int i = 0; i < lman.getLayerCount(); i++) {
						GeoLayer gl = lman.getGeoLayer(i);
						selDia.addOption(gl.getName(), gl.getContainerIdentifier(), !hasTables && i == lman.getIndexOfActiveLayer());
					}
				}
				selDia.show();
				if (selDia.wasCancelled())
					return;
				int idx = selDia.getSelectedOptionN();
				if (hasTables && idx < dKeeper.getTableCount()) {
					objCont = (ObjectContainer) dKeeper.getTable(idx);
				} else {
					if (hasTables) {
						idx -= dKeeper.getTableCount();
					}
					GeoLayer gl = lman.getGeoLayer(idx);
					if (gl.getObjectCount() < 1) {
						gl.loadGeoObjects();
					}
					gl.setLayerDrawn(true);
					lman.activateLayer(idx);
					objCont = (ObjectContainer) gl;
				}
			} else {
				if (!hasTables)
					return;
				TableData td = tman.selectCurrTable(res.getString("Select_the_table"));
				if (td == null)
					return;
				if (!td.table.hasData()) {
					td.table.loadData();
					if (!td.table.hasData()) {
						showMessage(res.getString("Failed_to_load_data"), true);
						return;
					}
				}
				objCont = (ObjectContainer) td.table;
			}
			if (objCont != null) {
				core.getDisplayProducer().makeQueryOrSearchTool(toolId, objCont);
			}
			return;
		}
	}

	protected void loadAppl() {
		String path = null;
		// following string:  "Select the file with project description"
		GetPathDlg fd = new GetPathDlg(CManager.getAnyFrame(this), res.getString("Select_the_file_with"));
		fd.setFileMask("*.app;*.mwi");
		fd.show();
		path = fd.getPath();
		if (path == null)
			return;
		System.out.println("Application path=[" + path + "]");
		core.removeAllData();
		core.getDataLoader().loadApplication(path, null);
		String applName = core.getSystemSettings().getParameterAsString("APPL_NAME");
		if (applName != null) {
			if (getMainFrame() != null) {
				getMainFrame().setTitle(applName);
			}
			lStatus.setDefaultMessage(defaultMessage + ": " + applName);
		} else {
			if (getMainFrame() != null) {
				getMainFrame().setTitle(defaultMessage);
			}
			lStatus.setDefaultMessage(defaultMessage);
		}
	}

	/**
	* A method from the SystemUI interface.
	*/
	@Override
	public Frame getMainFrame() {
		return CManager.getFrame(this);
	}

	/**
	* Informs whether a new map window must be used for each new visualisation.
	* If not, a new visualisation must be applied to the main map view.
	*/
	@Override
	public boolean getUseNewMapForNewVis() {
		return false;
	}

	/**
	* Sets whether a new map window must be used for each new visualisation.
	* If not, a new visualisation must be applied to the main map view.
	*/
	@Override
	public void setUseNewMapForNewVis(boolean value) {
	}

	/**
	* Creates a MapViewer to display the map. Ignores the specified number.
	*/
	@Override
	public void openMapView(int mapN) {
		if (mapView != null) {
			CManager.destroyComponent(mapView);
			this.remove(mapView);
		}
		LayerManager lman = core.getDataKeeper().getMap(0);
		if (lman == null)
			return;
		mapView = new SimpleMapView(core.getSupervisor(), lman);
		mapView.setIsPrimary(true);
		add(mapView, BorderLayout.CENTER);
		if (isShowing()) {
			CManager.validateAll(lStatus);
		}
		mapView.setup();
		bringMapToTop(mapView);
	}

	/**
	* A method from the SystemUI interface.
	* Brings current (main) map view to top.
	*/
	@Override
	public void bringMapToTop(MapViewer mView) {
		Frame fr = CManager.getFrame(this);
		if (fr != null) {
			fr.toFront();
		}
	}

	/**
	* A method from the SystemUI interface.
	* Returns the only map viewer available
	*/
	@Override
	public MapViewer getMapViewer(int mapN) {
		return mapView;
	}

	/**
	* A method from the SystemUI interface.
	* Returns the only map viewer available
	*/
	@Override
	public MapViewer getCurrentMapViewer() {
		return mapView;
	}

	/**
	* A method from the SystemUI interface.
	* Returns the only map viewer available
	*/
	@Override
	public MapViewer getLatestMapViewer() {
		return mapView;
	}

	/**
	* Returns the index of the current map (if multiple maps are possible)
	*/
	@Override
	public int getCurrentMapN() {
		return 0;
	}

	/**
	* Returns the map viewer with the given identifier. If the identifier is
	* null or "main", returns the primary map viewer (e.g. the one included
	* in the main window of the system). If the identifier is "_blank_",
	* constructs a  new map viewer.
	*/
	@Override
	public MapViewer getMapViewer(String id) {
		if (id == null || id.equalsIgnoreCase("main"))
			return mapView;
		return null;
	}

	/**
	 * Asks the system UI to mark the specified absolute spatial (geographic) position
	 * on all currently open maps.
	 */
	public void markPositionOnMaps(float posX, float posY) {
		if (mapView != null) {
			mapView.markPosition(posX, posY);
		}
		if (core.getSupervisor() != null) {
			core.getSupervisor().notifyPositionSelection(posX, posY);
		}
	}

	/**
	 * Asks the system UI to erase the marks of the previously specified absolute spatial
	 * (geographic) position, if any, on all currently open maps.
	 */
	public void erasePositionMarksOnMaps() {
		if (mapView != null) {
			mapView.erasePositionMark();
		}
		if (core.getSupervisor() != null) {
			core.getSupervisor().notifyPositionSelection(Float.NaN, Float.NaN);
		}
	}

	/**
	* Tries to find the map viewer with the given identifier. If not found,
	* returns null rather than tries to construct one. If the identifier is
	* null or "main", returns the primary map viewer (e.g. the one included
	* in the main window of the system).
	*/
	public MapViewer findMapViewer(String id) {
		if (id == null || id.equalsIgnoreCase("main"))
			return mapView;
		return null;
	}

	/**
	* Tries to find the plot with the given identifier. If not found,
	* returns null rather than tries to construct one.
	*/
	public Plot findPlot(String id) {
		if (id == null || id.length() == 0)
			return null;
		Supervisor sup = core.getSupervisor();
		if (sup != null && sup.getSaveableToolCount() > 0) {
			for (int i = 0; i < sup.getSaveableToolCount(); i++)
				if (sup.getSaveableTool(i) instanceof Plot) {
					Plot pl = (Plot) sup.getSaveableTool(i);
					if (id.equalsIgnoreCase(pl.getIdentifier()))
						return pl;
				}
		}
		return null;
	}

	/**
	* Closes the MapViewer displaying the map with the specified number
	* (e.g. when the map is removed from the system)
	*/
	public void closeMapView(int mapN) {
		if (mapN != 0 || mapView == null)
			return;
		CManager.destroyComponent(mapView);
		remove(mapView);
	}

	/**
	* A method from the SystemUI interface.
	* Places the given component somewhere in the UI (e.g. creates a separate frame
	* for it, or puts an a subwindow etc.) May analyse the type of the component.
	*/
	public void placeComponent(Component c) {
		if (c == null || mapView == null)
			return;
		String name = c.getName();
		if (name != null) {
			CManager.destroyComponent(mapView.getTabContent(name));
			mapView.removeTab(name);
			mapView.addTab(c, name);
		}
	}

	/**
	* Removes the component from the UI
	*/
	public void removeComponent(Component c) {
		if (c == null || mapView == null)
			return;
		mapView.removeTab(c.getName());
	}

	public NotificationLine getStatusLine() {
		return lStatus;
	}

	/**
	* Shows the given message. If "trouble" is true, then this
	* is an error message.
	*/
	public void showMessage(String msg, boolean trouble) {
		if (lStatus != null) {
			lStatus.showMessage(msg, trouble);
		}
		if (trouble) {
			System.out.println("ERROR: " + msg);
		}
	}

	/**
	* Shows the given message. Assumes that this is not an error message.
	*/
	public void showMessage(String msg) {
		showMessage(msg, false);
	}

	/**
	* Clears the status line
	*/
	public void clearStatusLine() {
		if (lStatus != null) {
			lStatus.showMessage(null, false);
		}
	}

	/**
	* Notifies about the status of some process. If "trouble" is true, then this
	* is an error message.
	*/
	public void notifyProcessState(String processName, String processState, boolean trouble) {
		showMessage(processName + ": " + processState, trouble);
	}

	/**
	* Returns the UI for starting time analysis functions (if available)
	*/
	public Object getTimeUI() {
		return null;
	}

	public void destroy() {
		if (destroyed)
			return;
		destroyed = true;
		if (scheduleToolsManager != null) {
			scheduleToolsManager.stopWork();
		}
		core.getSupervisor().getWindowManager().closeAllWindows();
		CManager.destroyComponent(this);
		if (!core.getSystemSettings().checkParameterValue("runsInsideOtherSystem", "true")) {
			System.exit(0);
		}
	}

	public boolean isDestroyed() {
		return destroyed;
	}

	public void quit() {
		Frame fr = getMainFrame();
		if (fr != null && (fr instanceof MainWin)) {
			fr.dispose();
		} else {
			destroy();
		}
	}
}
