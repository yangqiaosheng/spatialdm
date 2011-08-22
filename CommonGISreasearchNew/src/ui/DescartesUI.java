package ui;

import guide_tools.GuidingToolRegister;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.CheckboxMenuItem;
import java.awt.Component;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.List;
import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.MenuShortcut;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.calc.CalcManager;
import spade.analysis.plot.ObjectsSuitabilityChecker;
import spade.analysis.plot.QueryOrSearchTool;
import spade.analysis.system.DataKeeper;
import spade.analysis.system.DataLoader;
import spade.analysis.system.ESDACore;
import spade.analysis.system.Supervisor;
import spade.analysis.tools.DataAnalyser;
import spade.analysis.tools.ExtraToolManager;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Dialogs;
import spade.lib.basicwin.GetPathDlg;
import spade.lib.basicwin.MenuConstructor;
import spade.lib.basicwin.OKDialog;
import spade.lib.basicwin.PopupManager;
import spade.lib.basicwin.SelectDialog;
import spade.lib.help.Helper;
import spade.lib.lang.Language;
import spade.lib.util.CopyFile;
import spade.lib.util.Parameters;
import spade.lib.util.StringUtil;
import spade.time.FocusInterval;
import spade.time.manage.TemporalDataManager;
import spade.time.ui.TimeUI;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.DataTable;
import spade.vis.database.ObjectContainer;
import spade.vis.dataview.ShowRecManager;
import spade.vis.dmap.DLayerManager;
import spade.vis.dmap.SpatialWindow;
import spade.vis.map.MapViewer;
import spade.vis.space.GeoLayer;
import spade.vis.space.LayerManager;
import core.ActionDescr;
import data_load.DataLoadUI;
import data_load.DataManager;
import data_load.PointLayerFromTableBuilder;
import data_load.intelligence.TableIndexer;

public class DescartesUI extends BasicUI implements ActionListener, ItemListener, PropertyChangeListener {
	static ResourceBundle res = Language.getTextResource("ui.Res");
	/**
	* The class providing a UI for time analysis functions
	*/
	protected static final String TIME_UI_CLASS = "spade.time.ui.TimeFunctionsUI";
	/**
	* Used for selection of a table for data visualization etc.
	*/
	protected TableManager tman = null;
	/**
	* Used for data export
	*/
	protected DataAnalyser dataExporter = null;
	/**
	* The panel has no direct access to the system's menu. It manages the menu
	* through the MenuConstructor.
	*/
	protected MenuConstructor menuConst = null;

	protected Menu helpMenu = null, toolMenu = null;
	protected CheckboxMenuItem mayAskSemanticsQuestions = null, mayWarnAboutSemantics = null, spatialWindowCB = null, askForCommentsCB = null;
	protected CheckboxMenuItem mapVisInNewWin = null;
	/**
	* The manager of the tools for helping, guiding, teaching, and testing the user.
	*/
	protected GuidingToolRegister guideMan = null;
	/**
	* The UI component operating time analysis functions
	*/
	protected TimeUI timeUI = null;
	/**
	 * Default texts apearing in the title of the window and in the status line at
	 * the bottom
	 */
	protected String defTitle = "V-Analytics - Geospatial Visual Analytics", defStatusMessage = "Iris, Descartes, CommonGIS, V-Analytics 1995-2010";

	public DescartesUI(MenuConstructor constructor) {
		super();
		setMenuConstructor(constructor);
	}

	public void setMenuConstructor(MenuConstructor constructor) {
		menuConst = constructor;
	}

	public void startWork(ESDACore core, String configFileName) {
		super.startWork(core, configFileName);
		guideMan = new GuidingToolRegister();
		tman = new TableManager();
		tman.setDataKeeper(core.getDataKeeper());
		tman.setUI(this);
		makeInterface();
		editHelpMenu();
		if (core.getDataLoader().getTimeManager() == null) {
			core.getDataLoader().addPropertyChangeListener(this); //will wait for
			//appearance of a time manager
		} else {
			addMenuItemForTime();
		}
	}

	public void startWork(ESDACore core) {
		startWork(core, null);
	}

	protected boolean isOff(Parameters menuParm, String command) {
		if (menuParm == null)
			return false;
		return menuParm.checkParameterValue(command, "OFF");
	}

	public void makeInterface() {
		if (menuConst == null)
			return;
		Parameters menuParm = (Parameters) core.getSystemSettings().getParameter("MainMenu");
		//"File" menu
		int idx = menuConst.addMenuItem(res.getString("File"), null, this, true);
		if (isShowing()) {
			CManager.validateAll(this);
		}
		Menu menu = menuConst.getMenu(idx);
		MenuItem mi = null;
		boolean separated = true;
		if (core.getDataLoader() != null) {
			if (core.getDataKeeper().getMapCount() == 0 && core.getDataKeeper().getTableCount() == 0) {
				mi = menu.add(new MenuItem(res.getString("Load_project")));
				mi.setActionCommand("loadAppl");
			}
			if (!isApplet && !isOff(menuParm, "loadData")) {
				mi = menu.add(new MenuItem(res.getString("Load_data_")));
				mi.setActionCommand("loadData");
			}
			if (!isApplet) {
				TableIndexer tind = null;
				try {
					tind = (TableIndexer) Class.forName("data_load.intelligence.TableIndexAssistant").newInstance();
				} catch (Exception e) {
				}
				if (tind != null && !isOff(menuParm, tind.getCommandId())) {
					tind.setCore(core);
					mi = menu.add(new MenuItem(tind.getCommandText()));
					mi.setActionCommand(tind.getCommandId());
					mi.addActionListener(tind);
				}
			}
			if (!isApplet && !isOff(menuParm, "layerFromTable")) {
				mi = menu.add(new MenuItem("Generate a layer from a table"));
				mi.setActionCommand("layerFromTable");
			}
			if (menu.getItemCount() > 0) {
				separated = false;
			}
		}
		if (!isOff(menuParm, "view_links")) {
			mi = menu.add(new MenuItem(res.getString("view_links")));
			mi.setActionCommand("view_links");
		}
		if (!isOff(menuParm, "reload")) {
			if (!separated) {
				menu.addSeparator();
				separated = true;
			}
			mi = menu.add(new MenuItem(res.getString("Reload_data_")));
			mi.setActionCommand("reload");
			separated = false;
		}
		boolean hasSaveFunctions = false;
		if (!isApplet) {
			if (!isOff(menuParm, "export")) {
				try {
					dataExporter = (DataAnalyser) Class.forName("export.ExportManager").newInstance();
					if (!dataExporter.isValid(core)) {
						dataExporter = null;
					}
				} catch (Exception e) {
				}
				if (dataExporter != null) {
					if (!separated) {
						menu.addSeparator();
					}
					mi = menu.add(new MenuItem(res.getString("Export_data")));
					mi.setActionCommand("export");
					hasSaveFunctions = true;
				}
			}
			if (!isOff(menuParm, "saveAppl")) {
				if (!separated) {
					menu.addSeparator();
				}
				mi = menu.add(new MenuItem(res.getString("Save_project")));
				mi.setActionCommand("saveAppl");
				hasSaveFunctions = true;
			}
			if (!isOff(menuParm, "saveTiles")) {
				DataAnalyser tileSaver = null;
				try {
					tileSaver = (DataAnalyser) Class.forName("data_load.tiles.MapTileSaver").newInstance();
					if (!tileSaver.isValid(core)) {
						tileSaver = null;
					}
				} catch (Exception e) {
				}
				if (tileSaver != null) {
					if (!separated) {
						menu.addSeparator();
					}
					mi = menu.add(new MenuItem("Save OSM map tiles locally"));
					mi.setActionCommand("save_tiles");
					hasSaveFunctions = true;
				}
			}
		}
		if (!isOff(menuParm, "storeData")) {
			String saveDataDir = core.getSystemSettings().getParameterAsString("SaveDataDir");
			if (saveDataDir != null && (isApplet || CopyFile.fileExists(saveDataDir))) {
				String pathToScript = null;
				if (isApplet) {
					pathToScript = core.getSystemSettings().getParameterAsString("SaveDataScript");
				}
				if (!isApplet || pathToScript != null) {
					if (!separated) {
						menu.addSeparator();
					}
					mi = menu.add(new MenuItem(res.getString("Store_data_from_a"))); //"Store data from a table"
					mi.setActionCommand("storeData");
					hasSaveFunctions = true;
				}
			}
		}
		if (hasSaveFunctions) {
			separated = false;
		}
		boolean hasEditFunctions = false;
		if (!isOff(menuParm, "editTableName")) {
			if (!separated) {
				menu.addSeparator();
			}
			mi = menu.add(new MenuItem("Edit table names"));
			mi.setActionCommand("editTableName");
			separated = hasEditFunctions = true;
		}
		if (!isOff(menuParm, "editAttrName")) {
			if (!separated) {
				menu.addSeparator();
			}
			mi = menu.add(new MenuItem("Edit attribute names"));
			mi.setActionCommand("editAttrName");
			separated = hasEditFunctions = true;
		}
		if (!isOff(menuParm, "addAttribute")) {
			if (!separated) {
				menu.addSeparator();
			}
			mi = menu.add(new MenuItem(res.getString("Add_attribute")));
			mi.setActionCommand("addAttribute");
			separated = hasEditFunctions = true;
		}
		if (!isOff(menuParm, "removeAttribute")) {
			if (!separated) {
				menu.addSeparator();
			}
			mi = menu.add(new MenuItem("Remove attributes"));
			mi.setActionCommand("removeAttribute");
			separated = hasEditFunctions = true;
		}
		if (hasEditFunctions) {
			separated = false;
		}
		if (!isOff(menuParm, "joinTables")) {
			if (!separated) {
				menu.addSeparator();
			}
			mi = menu.add(new MenuItem(res.getString("Join_tables")));
			mi.setActionCommand("joinTables");
			separated = false;
		}
		if (!isOff(menuParm, "removeTable")) {
			if (!separated) {
				menu.addSeparator();
			}
			mi = menu.add(new MenuItem(res.getString("Remove_table")));
			mi.setActionCommand("removeTable");
			separated = false;
		}
		if (!isOff(menuParm, "removeLayer")) {
			mi = menu.add(new MenuItem(res.getString("Remove_map_layer")));
			mi.setActionCommand("removeLayer");
			separated = false;
		}
		if (!isOff(menuParm, "removeMap")) {
			mi = menu.add(new MenuItem(res.getString("Remove_map")));
			mi.setActionCommand("removeMap");
			separated = false;
		}
		if (!isOff(menuParm, "removeAll")) {
			mi = menu.add(new MenuItem(res.getString("Remove_all_data")));
			mi.setActionCommand("removeAll");
			separated = false;
		}
//ID

		if (!isOff(menuParm, "print")) {
			if (!separated) {
				menu.addSeparator();
			}
			mi = menu.add(new MenuItem(res.getString("Print_")));
			if (isApplet) {
				mi.setActionCommand("saveImages");
			} else {
				mi.setActionCommand("print");
			}
			separated = true;
		}
		if (!isOff(menuParm, "saveImages") && !isApplet) {
			if (!separated) {
				menu.addSeparator();
			}
			mi = menu.add(new MenuItem(res.getString("Save_")));
			mi.setActionCommand("saveImages");
			separated = false;
		}
//~ID
		if (!isApplet && (!isOff(menuParm, "makePage") || !isOff(menuParm, "printLog"))) {
			if (!separated) {
				menu.addSeparator();
			}
			if (!isOff(menuParm, "makePage")) {
				mi = menu.add(new MenuItem("Make a page"));
				mi.setActionCommand("makePage");
			}
			if (!isOff(menuParm, "printLog")) {
				mi = menu.add(new MenuItem("Print actions log"));
				mi.setActionCommand("printLog");
			}
			separated = false;
		}
		if (!isApplet && !isOff(menuParm, "quit")) {
			if (!separated) {
				menu.addSeparator();
			}
			mi = menu.add(new MenuItem(res.getString("Quit")));
			mi.setActionCommand("quit");
		}
		if (menu.getItemCount() < 1) {
			menuConst.removeMenuItem(idx);
		}
		//"Display" menu
		idx = menuConst.addMenuItem(res.getString("Display"), null, this, true);
		menu = menuConst.getMenu(idx);
		boolean noMS = !menuConst.allowShortcuts();
		mi = menu.add(new MenuItem(res.getString("Display_wizard_"), (noMS) ? null : new MenuShortcut(KeyEvent.VK_D)));
		mi.setActionCommand("displayWizard");
		if (!isOff(menuParm, "displayTable")) {
			boolean tableViewerAvailable = false;
			try {
				Class c = Class.forName("spade.vis.dataview.TableWin");
				if (c != null) {
					tableViewerAvailable = true;
				}
			} catch (Exception e) {
			}
			if (tableViewerAvailable) {
				mi = menu.add(new MenuItem(res.getString("View_table"), (noMS) ? null : new MenuShortcut(KeyEvent.VK_T)));
				mi.setActionCommand("displayTable");
			}
		}
		separated = false;
		if (!isOff(menuParm, "spatialWindow")) {
			menu.addSeparator();
			spatialWindowCB = new CheckboxMenuItem("Filter objects by spatial window", false);
			menu.add(spatialWindowCB);
			spatialWindowCB.addItemListener(this);
			menu.addSeparator();
			separated = true;
		}
		if (!isOff(menuParm, "cleanMap")) {
			if (!separated) {
				menu.addSeparator();
				separated = true;
			}
			mi = menu.add(new MenuItem(res.getString("Clean_the_map"), (noMS) ? null : new MenuShortcut(KeyEvent.VK_C)));
			mi.setActionCommand("cleanMap");
		}
		if (!isOff(menuParm, "cloneMap")) {
			if (!separated) {
				menu.addSeparator();
			}
			mi = menu.add(new MenuItem(res.getString("Another_window"), (noMS) ? null : new MenuShortcut(KeyEvent.VK_W)));
			mi.setActionCommand("cloneMap");
		}
		if (!isOff(menuParm, "windows")) {
			if (!separated) {
				menu.addSeparator();
			}
			mi = menu.add(new MenuItem(res.getString("Windows") + "...", null));
			mi.setActionCommand("windows");
		}
//ID
		boolean isLocal = core.getSystemSettings().checkParameterValue("isLocalSystem", "true");
		boolean hasKBML = false;
		boolean hasSAX2 = false;
		if (isLocal) {
			try {
				Class.forName("fr.dyade.koala.xml.kbml.KBMLDeserializer");
				hasKBML = true;
			} catch (Throwable ex) {
			}
			try {
				Class.forName("org.xml.sax.XMLReader");
				hasSAX2 = true;
			} catch (Throwable ex) {
			}

			separated = false;
			if (!isOff(menuParm, "saveSnapshot") && hasSAX2 && hasKBML && isLocal) {
				if (!separated) {
					menu.addSeparator();
					separated = true;
				}
				mi = menu.add(new MenuItem("Save Snapshot", (noMS) ? null : new MenuShortcut(KeyEvent.VK_S)));
				mi.setActionCommand("saveSnapshot");
			}
			if (!isOff(menuParm, "loadSnapshot") && hasSAX2 && hasKBML) {
				if (!separated) {
					menu.addSeparator();
				}
				mi = menu.add(new MenuItem("Load Snapshot", (noMS) ? null : new MenuShortcut(KeyEvent.VK_L)));
				mi.setActionCommand("loadSnapshot");
			}
		}
//~ID
		//"Calculate"
		if (!isOff(menuParm, "calculate") || !isOff(menuParm, "geocomp")) {
			boolean hasTableCalcManager = !isOff(menuParm, "calculate") && core.getTool("CalcManager") != null, hasGeoCalcManager = !isOff(menuParm, "geocomp") && core.getTool("GeoCalcManager") != null, hasBoth = hasTableCalcManager
					&& hasGeoCalcManager;
			if (hasTableCalcManager || hasGeoCalcManager) {
				String cmd = (hasBoth) ? null : (hasTableCalcManager) ? "calculate" : "geocomp";
				idx = menuConst.addMenuItem(res.getString("Calculate"), cmd, this, hasBoth);
				menu = menuConst.getMenu(idx);
				if (menu != null) {
					if (hasTableCalcManager) {
						mi = menu.add(new MenuItem(res.getString("Calculate_in_a_table")));
						mi.setActionCommand("calculate");
					}
					if (hasGeoCalcManager) {
						mi = menu.add(new MenuItem(res.getString("Geographic"))); //"Geographic computations"
						mi.setActionCommand("geocomp");
					}
				}
			}
		}
		//"Tools"
		int qsCount = core.getDisplayProducer().getQueryAndSearchToolCount();
		boolean extraTools = core.getTool("ExtraToolManager") != null;
		if (qsCount > 0 || extraTools) {
			idx = menuConst.addMenuItem(res.getString("Tools"), null, this, true);
			menu = toolMenu = menuConst.getMenu(idx);
			if (qsCount > 0) {
				for (int i = 0; i < qsCount; i++) {
					mi = menu.add(new MenuItem(core.getDisplayProducer().getQueryOrSearchToolName(i)));
					mi.setActionCommand("_query_" + core.getDisplayProducer().getQueryOrSearchToolId(i));
				}
				if (extraTools) {
					menu.addSeparator();
				}
			}
			if (extraTools) {
				ExtraToolManager extraMan = (ExtraToolManager) core.getTool("ExtraToolManager");
				for (int i = 0; i < extraMan.getAvailableToolCount(core); i++) {
					mi = menu.add(new MenuItem(extraMan.getAvailableToolName(i)));
					mi.setActionCommand("_tool_" + extraMan.getAvailableToolId(i));
				}
			}
		}
		//"Options"
		idx = menuConst.addMenuItem(res.getString("Options"), null, this, true);
		menu = menuConst.getMenu(idx);
		mapVisInNewWin = new CheckboxMenuItem(res.getString("mapVisInNewWin"), getUseNewMapForNewVis());
		menu.add(mapVisInNewWin);
		mapVisInNewWin.addItemListener(this);
		if (core.getDataKeeper() instanceof DataManager) {
			mi = menu.add(new MenuItem(res.getString("Show_records_options_"), (noMS) ? null : new MenuShortcut(KeyEvent.VK_R)));
			mi.setActionCommand("showRecOptions");
			menu.addSeparator();
		}
		askForCommentsCB = new CheckboxMenuItem("Ask for comments when producing pages", core.mayAskForComments());
		menu.add(askForCommentsCB);
		askForCommentsCB.addItemListener(this);
		mayAskSemanticsQuestions =
		// following string: "Verify relationships between variables"
		new CheckboxMenuItem(res.getString("Verify_relationships"), true);
		menu.add(mayAskSemanticsQuestions);
		mayAskSemanticsQuestions.addItemListener(this);
		mayWarnAboutSemantics = new CheckboxMenuItem(res.getString("Warn_of_misuse"), true);
		menu.add(mayWarnAboutSemantics);
		mayWarnAboutSemantics.addItemListener(this);
		//"Help"
		idx = menuConst.addMenuItem(res.getString("Help"), null, this, true);
		menu = menuConst.getMenu(idx);
		helpMenu = menu;
		if (Helper.canHelp("index")) {
			mi = menu.add(new MenuItem(res.getString("Index"), (noMS) ? null : new MenuShortcut(KeyEvent.VK_I)));
			mi.setActionCommand("help_index");
		}
		if (!isOff(menuParm, "help_decision_support") && Helper.canHelp("decision_support")) {
			mi = menu.add(new MenuItem(res.getString("Decision_support")));
			mi.setActionCommand("help_decision_support");
		}
		if (guideMan != null && guideMan.checkTools(core) > 0) {
			for (int i = 0; i < guideMan.getToolCount(); i++)
				if (!isOff(menuParm, guideMan.getToolId(i)) && guideMan.isToolValid(i)) {
					mi = menu.add(new MenuItem(guideMan.getToolName(i)));
					mi.setActionCommand("_guide_" + guideMan.getToolId(i));
				}
		}
		menu.addSeparator();
		mi = menu.add(new MenuItem(res.getString("About"), (noMS) ? null : new MenuShortcut(KeyEvent.VK_A)));
		mi.setActionCommand("about");
		if (isShowing()) {
			CManager.validateAll(this);
		}
	}

	/**
	* After application (re)loading checks if guiding tools are still valid and
	* edits the menu. Besides, checks if there is any project information and,
	* accordingly, adds or removes the menu item "About project".
	*/
	protected void editHelpMenu() {
		if (helpMenu == null)
			return;
		boolean hasProjectInfo = core.getSystemSettings().getParameter("ABOUT_PROJECT") != null;
		int idxAP = -1;
		for (int i = 0; i < helpMenu.getItemCount() && idxAP < 0; i++) {
			String cmd = helpMenu.getItem(i).getActionCommand();
			if (cmd != null && cmd.equals("about_project")) {
				idxAP = i;
			}
		}
		if (hasProjectInfo && idxAP < 0) { //insert the menu item "about project"
			MenuItem mi = new MenuItem(res.getString("About_project"), (menuConst.allowShortcuts()) ? new MenuShortcut(KeyEvent.VK_P) : null);
			mi.setActionCommand("about_project");
			helpMenu.add(mi);
		} else if (!hasProjectInfo && idxAP >= 0) {
			helpMenu.remove(idxAP);
		}
		int idx = helpMenu.getItemCount();
		for (int i = helpMenu.getItemCount() - 1; i >= 0; i--) {
			String cmd = helpMenu.getItem(i).getActionCommand();
			if (cmd != null && cmd.startsWith("_guide_")) {
				helpMenu.remove(i);
				idx = i - 1;
			}
		}
		if (guideMan.checkTools(core) > 0) {
			for (int i = 0; i < guideMan.getToolCount(); i++)
				if (guideMan.isToolValid(i)) {
					MenuItem mi = new MenuItem(guideMan.getToolName(i));
					helpMenu.insert(mi, idx + i);
					mi.setActionCommand("_guide_" + guideMan.getToolId(i));
				}
		}
	}

	public void itemStateChanged(ItemEvent e) {
		if (e.getSource().equals(spatialWindowCB)) {
			toggleSpatialWindow();
		} else if (e.getSource().equals(askForCommentsCB)) {
			core.setMayAskForComments(askForCommentsCB.getState());
		} else if (e.getSource().equals(mayAskSemanticsQuestions)) {
			for (int i = 0; i < core.getDataKeeper().getTableCount(); i++) {
				((DataTable) core.getDataKeeper().getTable(i)).getSemanticsManager().questionsAllowed = mayAskSemanticsQuestions.getState();
			}
		} else if (e.getSource().equals(mayWarnAboutSemantics)) {
			for (int i = 0; i < core.getDataKeeper().getTableCount(); i++) {
				((DataTable) core.getDataKeeper().getTable(i)).getSemanticsManager().warningsAllowed = mayWarnAboutSemantics.getState();
			}
		} else if (e.getSource().equals(mapVisInNewWin)) {
			setUseNewMapForNewVis(mapVisInNewWin.getState());
		}
	}

	protected void loadAppl() {
		String path = null, dataServerURL = core.getSystemSettings().getParameterAsString("DATA_SERVER");
		if (isApplet && dataServerURL == null) {
			Panel p = new Panel(new BorderLayout());
			p.add(new Label(res.getString("Data_Server_URL_")), "West");
			TextField urlTF = new TextField(50);
			Object obj = core.getSystemSettings().getParameter("DocumentBase");
			if (obj != null && (obj instanceof URL)) {
				URL url = (URL) obj;
				urlTF.setText(url.getProtocol() + "://" + ((url.getHost() == null) ? "localhost" : url.getHost()) + ":8080/DataServer/");
			}
			p.add(urlTF, "Center");
			OKDialog okd = new OKDialog(CManager.getAnyFrame(this), res.getString("Data_Server_URL"), true);
			okd.addContent(p);
			okd.show();
			if (okd.wasCancelled())
				return;
			dataServerURL = urlTF.getText();
			if (dataServerURL != null) {
				dataServerURL = dataServerURL.trim();
				if (dataServerURL.length() < 1) {
					dataServerURL = null;
				}
			}
			if (dataServerURL == null)
				return;
			core.getSystemSettings().setParameter("DATA_SERVER", dataServerURL);
		}
		if (dataServerURL == null) {
			// following string:  "Select the file with project description"
			GetPathDlg fd = new GetPathDlg(CManager.getAnyFrame(this), res.getString("Select_the_file_with"));
			fd.setFileMask("*.app;*.mwi");
			fd.show();
			path = fd.getPath();
		} else {
			Vector appls = getApplListFromServer(dataServerURL);
			if (appls == null)
				return;
			int nrows = appls.size();
			if (nrows < 2) {
				nrows = 2;
			} else if (nrows > 10) {
				nrows = 10;
			}
			List lst = new List(nrows);
			for (int i = 0; i < appls.size(); i++) {
				String str = (String) appls.elementAt(i);
				Vector v = StringUtil.getNames(str, ":");
				if (v == null || v.size() < 2) {
					continue;
				}
				lst.add((String) v.elementAt(0));
			}
			lst.select(0);
			Panel p = new Panel(new BorderLayout());
			p.add(new Label(res.getString("Select_project_")), "North");
			p.add(lst, "Center");
			OKDialog okd = new OKDialog(CManager.getAnyFrame(this), res.getString("Select_project"), true);
			okd.addContent(p);
			okd.show();
			if (okd.wasCancelled())
				return;
			int n = lst.getSelectedIndex();
			if (n < 0)
				return;
			String str = (String) appls.elementAt(n);
			Vector v = StringUtil.getNames(str, ":");
			path = (String) v.elementAt(1);
		}
		if (path == null)
			return;
		System.out.println("Application path=[" + path + "]");
		guideMan.stopAllTools();
		core.removeAllData();
		if (timeUI != null) {
			removeMenuItemForTime();
			timeUI = null;
			core.getDataLoader().addPropertyChangeListener(this); //will wait for
			//appearance of a time manager
		}
		ActionDescr aDescr = new ActionDescr();
		aDescr.aName = "Load application";
		aDescr.startTime = System.currentTimeMillis();
		boolean ok = core.getDataLoader().loadApplication(path, dataServerURL);
		String applName = core.getSystemSettings().getParameterAsString("APPL_NAME");
		if (ok) {
			aDescr.addParamValue("Path", core.getDataLoader().getApplicationPath());
			if (applName != null) {
				aDescr.addParamValue("Name", applName);
			}
			aDescr.endTime = System.currentTimeMillis();
			core.logAction(aDescr);
		}
		if (applName != null) {
			if (getMainFrame() != null) {
				getMainFrame().setTitle(defTitle + ": " + applName);
			}
			lStatus.setDefaultMessage(defStatusMessage + ": " + applName);
		} else {
			if (getMainFrame() != null) {
				getMainFrame().setTitle(defTitle);
			}
			lStatus.setDefaultMessage(defStatusMessage);
		}
		editHelpMenu();
		autostartTools();
	}

	/**
	* Runs the data server component called ApplLister. Receives from it a list
	* of strings in the format
	* "application name":"path or URL"
	* Returns these strings in a vector. Returns null if an error occurred.
	*/
	protected Vector getApplListFromServer(String servletURLStr) {
		if (servletURLStr == null)
			return null;
		servletURLStr = servletURLStr.replace('\\', '/');
		if (!servletURLStr.endsWith("/")) {
			servletURLStr += "/";
		}
		URL servletURL = null;
		try {
			servletURL = new URL(servletURLStr + "ApplLister");
		} catch (MalformedURLException mfe) {
			showMessage(mfe.toString(), true);
		}
		if (servletURL == null)
			return null;
		InputStream in = null;
		try {
			in = servletURL.openStream();
		} catch (IOException ioe) {
			// following string: "Could not open the stream: "
			showMessage(res.getString("Could_not_open_the") + ioe.toString(), true);
		}
		if (in == null)
			return null;
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		Vector result = new Vector(20, 10);
		while (true) {
			try {
				String str = reader.readLine();
				if (str == null) {
					break;
				}
				str = str.trim();
				if (str.length() < 1) {
					continue;
				}
				if (str.startsWith("ERROR:")) {
					showMessage(res.getString("Server_error_") + str.substring(6).trim(), true);
					break;
				}
				if (str.startsWith("Working directory")) {
					continue;
				}
				result.addElement(str);
			} catch (EOFException eof) {
				break;
			} catch (IOException ioe) {
				showMessage(ioe.toString(), true);
			}
		}
		try {
			in.close();
		} catch (IOException ioe) {
		}
		if (result.size() < 1)
			return null;
		return result;
	}

	public void actionPerformed(ActionEvent e) {
		lStatus.showMessage(null, false);
		PopupManager.hideWindow();
		String cmd = e.getActionCommand();
		if (cmd == null)
			return;
		if (cmd.equals("quit")) {
			quit();
		} else if (cmd.equals("loadAppl")) {
			loadAppl();
		} else if (cmd.startsWith("help_")) {
			Helper.help(cmd.substring(5));
		} else if (cmd.startsWith("_guide_")) {
			guideMan.startTool(cmd.substring(7), core);
		} else if (cmd.equals("loadData")) {
			DataLoadUI dload = new DataLoadUI();
			dload.start(core.getDataLoader(), core.getUI());
		} else if (cmd.equals("layerFromTable")) {
			PointLayerFromTableBuilder plBuilder = new PointLayerFromTableBuilder();
			plBuilder.run(core);
		} else if (cmd.equals("view_links")) {
			DataLinkView lView = new DataLinkView();
			if (lView.construct(core)) {
				OKDialog okd = new OKDialog(getMainFrame(), res.getString("view_links"), false);
				okd.addContent(lView);
				okd.show();
				lView.checkNames();
			}
		} else if (cmd.equals("reload")) {
			DataReloadUI drui = new DataReloadUI();
			drui.reloadData(core);
		} else if (cmd.equals("editTableName")) {
			DataKeeper dataLoader = core.getDataKeeper();
			int nTables = dataLoader.getTableCount();
			if (nTables < 1) {
				showMessage("No tables available!", true);
				return;
			}
			String tNames[] = new String[nTables];
			for (int i = 0; i < nTables; i++) {
				tNames[i] = dataLoader.getTable(i).getName();
			}
			tNames = Dialogs.editStringValues(getMainFrame(), null, tNames, "Edit the names of the tables:", "Table names", true);
			if (tNames != null) {
				for (int i = 0; i < tNames.length; i++)
					if (tNames[i] != null) {
						dataLoader.getTable(i).setName(tNames[i]);
					}
			}
		} else if (cmd.equals("editAttrName")) {
			TableData td = tman.selectCurrTable("Select the table for renaming attributes");
			if (td == null)
				return;
			if (!(td.table instanceof DataTable))
				return;
			DataTable table = (DataTable) td.table;
			Vector attr = table.getTopLevelAttributes();
			if (attr.size() < 1) {
				showMessage("No attributes in the table!", true);
				return;
			}
			String aNames[] = new String[attr.size()];
			for (int i = 0; i < aNames.length; i++) {
				aNames[i] = ((Attribute) attr.elementAt(i)).getName();
			}
			aNames = Dialogs.editStringValues(getMainFrame(), null, aNames, "Edit the names of the attributes:", "Attribute names", true);
			if (aNames != null) {
				for (int i = 0; i < aNames.length; i++)
					if (aNames[i] != null) {
						((Attribute) attr.elementAt(i)).setName(aNames[i]);
					}
			}
		} else if (cmd.equals("addAttribute")) {
			AddAttribute addAttr = new AddAttribute();
			int aIdx = addAttr.addAttribute(tman, core.getSupervisor());
			if (aIdx >= 0 && mapView != null) { //immediately visualize the new attribute
				TableData td = addAttr.getTableData();
				Vector attr = new Vector(1, 1);
				attr.addElement(td.table.getAttributeId(aIdx));
				core.getDisplayProducer().displayOnMap(td.table, attr, td.themLayer, mapView);
			}
		} else if (cmd.equals("removeAttribute")) {
			TableData td = tman.selectCurrTable("Select the table for removing attributes");
			if (td == null)
				return;
			if (!(td.table instanceof DataTable))
				return;
			DataTable table = (DataTable) td.table;
			AttributeChooser attrSel = new AttributeChooser();
			Vector attr = attrSel.selectTopLevelAttributes(table, "Select the attributes to remove", this);
			if (attr == null || attr.size() < 1)
				return;
			table.removeAttributes(attr);
			showMessage("The attributes have been removed", false);
		} else if (cmd.equals("export")) {
			if (dataExporter != null) {
				dataExporter.run(core);
			}
		} else if (cmd.equals("storeData")) {
			String saveDataDir = core.getSystemSettings().getParameterAsString("SaveDataDir");
			if (saveDataDir == null) {
				// following string: "The directory for storing data is not specified"
				showMessage(res.getString("The_directory_for"), true);
				return;
			}
			String pathToScript = null;
			if (isApplet) {
				pathToScript = core.getSystemSettings().getParameterAsString("SaveDataScript");
			}
			if (isApplet && pathToScript == null) {
				// following string: "The script to use for storing data is not specified"
				showMessage(res.getString("The_script_to_use_for"), true);
				return;
			}
			tman.storeDataFromTable(saveDataDir, pathToScript, isApplet);
		} else if (cmd.equals("save_tiles")) {
			DataAnalyser tileSaver = null;
			try {
				tileSaver = (DataAnalyser) Class.forName("data_load.tiles.MapTileSaver").newInstance();
			} catch (Exception ex) {
			}
			if (tileSaver != null) {
				tileSaver.run(core);
			}
		} else
//ID
		if (cmd.equals("joinTables")) {
			if (mapView == null)
				return;
			LayerManager lman = mapView.getLayerManager();
			TableJoiner joiner = new TableJoiner();
			joiner.joinTables(lman, core);
		} else
//~ID
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
		} else if (cmd.equals("removeLayer")) {
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
		} else if (cmd.equals("removeMap")) {
			if (mapView == null)
				return;
			OKDialog dia = new OKDialog(CManager.getAnyFrame(this), res.getString("Confirm"), OKDialog.YES_NO_MODE, true);
			// following string: "Remove the current map?"
			dia.addContent(new Label(res.getString("Remove_the_current")));
			dia.show();
			if (!dia.wasCancelled()) {
				core.removeMap(currMapN, false);
			}
		} else if (cmd.equals("removeAll")) {
			if (core.getDataKeeper().getMapCount() < 1 && core.getDataKeeper().getTableCount() < 1)
				return; //nothing to remove
			OKDialog dia = new OKDialog(CManager.getAnyFrame(this), res.getString("Confirm"), OKDialog.YES_NO_MODE, true);
			dia.addContent(new Label(res.getString("Remove_all_the_data_")));
			dia.show();
			if (!dia.wasCancelled()) {
				guideMan.stopAllTools();
				core.removeAllData();
				if (timeUI != null) {
					removeMenuItemForTime();
					timeUI = null;
					core.getDataLoader().addPropertyChangeListener(this); //will wait for
					//appearance of a time manager
				}
				core.getSystemSettings().setParameter("APPL_NAME", null);
				if (getMainFrame() != null) {
					getMainFrame().setTitle(defTitle);
				}
				lStatus.setDefaultMessage(defStatusMessage);
				core.getDataLoader().addPropertyChangeListener(this); //will wait for
				//appearance of a time manager
			}
		} else if (cmd.equals("saveAppl")) {
			//if (currMapN>=0) {
			core.getDataLoader().setCurrentMapN(currMapN);
			core.getDataLoader().saveApplication();
			String applName = core.getSystemSettings().getParameterAsString("APPL_NAME");
			if (applName != null) {
				if (getMainFrame() != null) {
					getMainFrame().setTitle(defTitle + ": " + applName);
				}
				lStatus.setDefaultMessage(defStatusMessage + ": " + applName);
			}
			//}
		} else
//ID
		if (cmd.equals("print")) {
			ImagePrinter printer = new ImagePrinter(core.getSupervisor());
			printer.chooseAndSaveOrPrint(true);
		} else if (cmd.equals("saveImages")) {
			ImagePrinter printer = new ImagePrinter(core.getSupervisor());
			printer.chooseAndSaveOrPrint(false);
		} else
//~ID
		if (cmd.equals("makePage")) {
			PageProducer pp = new PageProducer(core);
			pp.chooseAndPrint();
		} else if (cmd.equals("printLog")) {
			FileDialog fd = new FileDialog(getMainFrame(), "Print action log in a file", FileDialog.SAVE);
			String applPath = core.getDataLoader().getApplicationPath();
			if (applPath != null) {
				fd.setDirectory(CopyFile.getDir(applPath));
			}
			fd.setFile("*.txt;*.htm;*.html");
			fd.show();
			String fname = fd.getFile(), dir = fd.getDirectory();
			if (fname == null)
				return;
			if (fname.lastIndexOf(".") < 0) {
				fname += ".txt";
			} else if (fname.lastIndexOf(".") == fname.length() - 1) {
				fname += "txt";
			}
			core.printLoggedActions(dir + fname);
		} else if (cmd.equals("displayTable")) {
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
		} else if (cmd.equals("displayWizard")) {
			new DisplayAssistant(core.getDataKeeper(), core.getDisplayProducer(), core.getSupervisor(), tman);
		} else
		//ID
		if (cmd.equals("saveSnapshot")) {
			if (mapView == null)
				return;
			LayerManager lman = mapView.getLayerManager();
			SnapshotManager sman = core.getSnapshotManager();
			sman.saveProjectSnapshot();
		} else if (cmd.equals("loadSnapshot")) {
			SnapshotManager sman = core.getSnapshotManager();
			sman.loadProjectSnapshot();
		} else
//~ID
		if (cmd.equals("calculate")) {
			Object calcMan = core.getTool("CalcManager");
			if (calcMan != null && (calcMan instanceof CalcManager)) {
				new CalcWizard(core.getDataKeeper(), (CalcManager) calcMan, core.getSupervisor(), tman);
			}
		} else if (cmd.equals("geocomp")) {
			core.runTool("GeoCalcManager");
		} else if (cmd.equals("showRecOptions")) {
			if (!(core.getDataKeeper() instanceof DataManager))
				return;
			DataManager dataLoader = (DataManager) core.getDataKeeper();
			TableData td = tman.selectCurrTable(res.getString("Select_the_table"));
			if (td == null)
				return;
			ShowRecManager recMan = dataLoader.getShowRecManager(td.tableN);
			if (recMan == null)
				return;
			ShowRecTuner tuner = new ShowRecTuner(recMan, this);
			OKDialog dlg = new OKDialog(CManager.getAnyFrame(this),
			// following string: "Record viewing options"
					res.getString("Record_viewing"), false);
			dlg.addContent(tuner);
			dlg.show();
			tuner.setPopupAddAttrs();
		} else if (cmd.equals("about")) {
			Component about = AboutFinder.getAbout();
			if (about != null) {
				OKDialog dlg = new OKDialog(CManager.getAnyFrame(this), "V-Analytics - Geospatial Visual Analytics", false);
				dlg.addContent(about);
				dlg.show();
			}
		} else
//ID
		if (cmd.equals("about_project")) {
			String about_project = core.getSystemSettings().getParameterAsString("ABOUT_PROJECT");
			if (about_project != null && about_project != "") {
				OKDialog dlg = new OKDialog(CManager.getAnyFrame(this), "V-Analytics", false);
				dlg.addContent(new AboutProjectTab(about_project));
				dlg.show();
			}
		} else
//~ID
		if (cmd.equals("windows")) {
			core.getWindowManager().showWindowList();
		} else if (cmd.equals("cloneMap")) {
			if (mapView != null) {
				Hashtable prop = mapView.getProperties();
				AdvancedMapView mw1 = (AdvancedMapView) mapView.makeCopyAndClear();
				mw1.setIsPrimary(false);
				int w = 600, h = 400;
				Frame mf = getMainFrame();
				if (mf != null && mf.getSize() != null) {
					w = mf.getSize().width;
					h = mf.getSize().height;
				}
				Frame fClone = core.getDisplayProducer().makeWindow(mw1, mw1.getName(), w, h);
				mw1.setProperties(prop);
				/*
				Panel p=new Panel(new BorderLayout());
				p.add(new Label(res.getString("window_name")+"?"),BorderLayout.WEST);
				TextField tf=new TextField(mw1.getName(),60);
				p.add(tf,BorderLayout.CENTER);
				OKDialog okd=new OKDialog(fClone,res.getString("window_name"),true);
				okd.addContent(p);
				okd.show();
				if (!okd.wasCancelled()) {
				  String str=tf.getText();
				  if (str!=null) {
				    str=str.trim();
				    if (str.length()>0) {
				      mw1.setName(str); fClone.setName(str); fClone.setTitle(str);
				    }
				  }
				}*/
			}
		} else if (cmd.equals("cleanMap")) {
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
		} else if (cmd.startsWith("_tool_")) {
			ExtraToolManager extraMan = (ExtraToolManager) core.getTool("ExtraToolManager");
			if (extraMan != null) {
				extraMan.startTool(cmd.substring(6), core);
			}
		} else if (cmd.startsWith("_query_")) {
			DataKeeper dKeeper = core.getDataKeeper();
			if (dKeeper == null)
				return;
			String toolId = cmd.substring(7);
			ObjectContainer objCont = null;
			boolean hasTables = dKeeper.getTableCount() > 0;
			ObjectsSuitabilityChecker suitCh = null;
			if (core.getDisplayProducer().canToolCheckObjectsSuitability(toolId)) {
				Object tool = core.getDisplayProducer().makeToolInstance(toolId);
				if (tool != null && (tool instanceof ObjectsSuitabilityChecker)) {
					suitCh = (ObjectsSuitabilityChecker) tool;
				}
			}
			if (suitCh != null) {
				Vector containers = new Vector(10, 10);
				for (int i = 0; i < dKeeper.getTableCount(); i++) {
					AttributeDataPortion table = dKeeper.getTable(i);
					if (suitCh.isSuitable((ObjectContainer) table)) {
						containers.addElement(table);
					}
				}
				LayerManager lman = dKeeper.getMap(getCurrentMapN());
				if (lman != null) {
					for (int i = 0; i < lman.getLayerCount(); i++) {
						GeoLayer gl = lman.getGeoLayer(i);
						if (suitCh.isSuitable((ObjectContainer) gl)) {
							containers.addElement(gl);
						}
					}
				}
				if (containers.size() < 1) {
					showMessage("No suitable object sets found!", true);
					return;
				}
				SelectDialog selDia = new SelectDialog(getMainFrame(), core.getDisplayProducer().getQueryOrSearchToolName(toolId), "Select the object container");
				for (int i = 0; i < containers.size(); i++) {
					ObjectContainer oCont = (ObjectContainer) containers.elementAt(i);
					selDia.addOption(oCont.getName(), oCont.getContainerIdentifier(), i == 0);
				}
				selDia.show();
				if (selDia.wasCancelled())
					return;
				int idx = selDia.getSelectedOptionN();
				objCont = (ObjectContainer) containers.elementAt(idx);
			} else if (core.getDisplayProducer().isToolAttributeFree(toolId)) {
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
		} else if (cmd.equals("time") && timeUI != null) {
			timeUI.startTimeAnalysis();
		}
	}

	public void toggleSpatialWindow() {
		if (mapView == null || mapView.getLayerManager() == null || !(mapView.getLayerManager() instanceof DLayerManager)) {
			spatialWindowCB.setState(false);
			return;
		}
		DLayerManager lman = (DLayerManager) mapView.getLayerManager();
		if (spatialWindowCB.getState())
			if (lman.hasSpatialWindow())
				return;
			else {
				SpatialWindow spWin = new SpatialWindow();
				spWin.setMap(mapView.getMapDrawer());
				lman.addSpatialWindow(spWin);
			}
		else {
			lman.removeSpatialWindow();
		}
	}

	/**
	* Automatically starts the tools specified in the system's parameter
	* "AUTOSTART_TOOL" (this parameter is updated when a new project is loaded)
	*/
	public void autostartTools() {
		String toolList = core.getSystemSettings().getParameterAsString("AUTOSTART_TOOL");
		if (toolList == null || toolList.length() < 1)
			return;
		ExtraToolManager extraMan = (ExtraToolManager) core.getTool("ExtraToolManager");
		if (extraMan == null)
			return;
		Vector toolIds = StringUtil.getNames(toolList, ";");
		if (toolIds == null)
			return;
		for (int i = 0; i < toolIds.size(); i++) {
			extraMan.startTool((String) toolIds.elementAt(i), core);
		}
	}

	/**
	* Reacts to events from the data manager, in particular, to the appearance
	* of time-referenced data.
	*/
	public void propertyChange(PropertyChangeEvent e) {
		if ((e.getSource() instanceof DataLoader) && e.getPropertyName().equals("time_manager")) {
			core.getDataLoader().removePropertyChangeListener(this);
			addMenuItemForTime();
			core.getDataLoader().getTimeManager().addPropertyChangeListener(this);
		} else if (e.getSource() instanceof TemporalDataManager) {
			if (e.getPropertyName().equals("all_data_removed")) {
				removeMenuItemForTime();
			} else if (e.getPropertyName().equals("data_appeared")) {
				addMenuItemForTime();
			}
		} else if (e.getSource().equals(timeUI) && (e.getPropertyName().equals("open_time_filter_controls") || e.getPropertyName().equals("close_time_filter_controls"))) {
			FocusInterval fint = timeUI.getTimeFilterFocusInterval();
			if (fint != null)
				if (e.getPropertyName().equals("open_time_filter_controls")) {
					fint.addPropertyChangeListener(this);
				} else {
					fint.removePropertyChangeListener(this);
				}
		} else if (e.getPropertyName().equals("current_interval")) {
			if (mapView == null)
				return;
			((PropertyChangeListener) mapView.getLayerManager()).propertyChange(e);
			//propagate this to all maps
			Supervisor sup = core.getSupervisor();
			if (sup != null && sup.getSaveableToolCount() > 0) {
				for (int i = 0; i < sup.getSaveableToolCount(); i++)
					if (sup.getSaveableTool(i) instanceof MapViewer) {
						MapViewer mw = (MapViewer) sup.getSaveableTool(i);
						if (!mw.equals(mapView)) {
							((PropertyChangeListener) mw.getLayerManager()).propertyChange(e);
						}
					}
			}
		}
	}

	/**
	* Returns the UI for starting time analysis functions (if available)
	*/
	public Object getTimeUI() {
		if (timeUI == null) {
			//try to construct the UI component for accessing time analysis functions
			try {
				timeUI = (TimeUI) Class.forName(TIME_UI_CLASS).newInstance();
				timeUI.setCore(core);
				timeUI.setDataManager(core.getDataLoader().getTimeManager());
			} catch (Exception e) {
			}
			if (timeUI != null) {
				timeUI.addPropertyChangeListener(this);
			}
		}
		return timeUI;
	}

	/**
	* Adds a menu item for activating time analysis functions
	*/
	protected void addMenuItemForTime() {
		if (toolMenu == null)
			return;
		if (timeUI == null) {
			getTimeUI();
		}
		if (timeUI == null || !timeUI.canStartTimeAnalysis())
			return;
		for (int i = 0; i < toolMenu.getItemCount(); i++) {
			String cmd = toolMenu.getItem(i).getActionCommand();
			if (cmd != null && cmd.equals("time"))
				return; //already exists
		}
		MenuItem mi = new MenuItem(res.getString("time_functions"), null);
		mi.setActionCommand("time");
		toolMenu.addSeparator();
		toolMenu.add(mi);
	}

	/**
	* Removes the menu item that activates time analysis functions
	*/
	protected void removeMenuItemForTime() {
		if (toolMenu == null)
			return;
		int idx = -1;
		for (int i = 0; i < toolMenu.getItemCount() && idx < 0; i++) {
			String cmd = toolMenu.getItem(i).getActionCommand();
			if (cmd != null && cmd.equals("time")) {
				idx = i;
			}
		}
		if (idx < 0)
			return;
		toolMenu.remove(idx);
		toolMenu.remove(idx - 1); //separator
	}

	public void destroy() {
		if (destroyed)
			return;
		guideMan.stopAllTools();
		super.destroy();
	}

}
