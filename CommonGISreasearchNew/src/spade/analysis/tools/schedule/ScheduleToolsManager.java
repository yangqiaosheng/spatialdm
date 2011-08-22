package spade.analysis.tools.schedule;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.CheckboxMenuItem;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Label;
import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.Panel;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.TextField;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.calc.FindPegasus;
import spade.analysis.calc.Pegasus;
import spade.analysis.classification.Classifier;
import spade.analysis.system.DataLoader;
import spade.analysis.system.DisplayProducer;
import spade.analysis.system.ESDACore;
import spade.analysis.tools.moves.MovementMatrixPanel;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Dialogs;
import spade.lib.basicwin.GetPathDlg;
import spade.lib.basicwin.OKDialog;
import spade.lib.basicwin.SelectDialog;
import spade.lib.basicwin.StringInRectangle;
import spade.lib.lang.Language;
import spade.lib.util.CopyFile;
import spade.lib.util.IntArray;
import spade.lib.util.Matrix;
import spade.lib.util.NumRange;
import spade.lib.util.StringUtil;
import spade.time.TimeIntervalSelectorImpl;
import spade.time.TimeMoment;
import spade.time.vis.TimeLineView;
import spade.vis.database.AttrCondition;
import spade.vis.database.CombinedFilter;
import spade.vis.database.DataAggregator;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.database.NumAttrCondition;
import spade.vis.database.ObjectContainer;
import spade.vis.database.ObjectFilter;
import spade.vis.database.ObjectFilterBySelection;
import spade.vis.database.QualAttrCondition;
import spade.vis.database.TableFilter;
import spade.vis.database.TimeFilter;
import spade.vis.dataview.ShowRecManager;
import spade.vis.dmap.DAggregateLinkLayer;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;
import spade.vis.dmap.DLayerManager;
import spade.vis.dmap.DrawingParameters;
import spade.vis.map.LegendDrawer;
import spade.vis.map.MapViewer;
import spade.vis.mapvis.ClassDrawer;
import spade.vis.mapvis.DataMapper;
import spade.vis.mapvis.MultiBarDrawer;
import spade.vis.mapvis.NumValueSignPainter;
import spade.vis.mapvis.NumberDrawer;
import spade.vis.mapvis.PieChartDrawer;
import spade.vis.mapvis.Visualizer;
import spade.vis.space.LayerManager;
import spade.vis.spec.DataSourceSpec;
import data_load.DataManager;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: 18-Oct-2007
 * Time: 12:19:23
 * Used for a simplified version of the system. Supports loading and
 * exploration of a transportation schedule.
 */
public class ScheduleToolsManager implements LegendDrawer, ActionListener, ItemListener, WindowListener, PropertyChangeListener {
	static ResourceBundle res = Language.getTextResource("spade.analysis.tools.schedule.SchedulerTexts_tools_schedule");

	protected ESDACore core = null;
	/**
	 * A layer with endangered objects received from IDAS. The objects may contain
	 * people or items subject to evacuation.
	 */
	protected DGeoLayer objToSave = null;
	/**
	 * A layer with potentially endangered objects received from IDAS.
	 * The objects may contain people or items subject to evacuation.
	 */
	protected DGeoLayer objPossiblyToSave = null;
	/**
	 * A layer with potential shelters received from IDAS.
	 */
	protected DGeoLayer shelters = null;
	/**
	 * The thread to run the external scheduler
	 */
	protected RunSchedulerThread schedulerThread = null;

	protected String startSchedulerText = res.getString("sch_start");
	/**
	 * The text to replace the item "Start the scheduler" after the scheduler
	 * has been started
	 */
	protected String stopSchedulerText = res.getString("sch_stop");
	/**
	 * The texts of the menu items corresponding to the functions of the tool
	 */
	protected String menuTexts[] = { startSchedulerText, res.getString("sch_load"), res.getString("sch_summary"), res.getString("sch_gantt"), res.getString("sch_matrix"), res.getString("sch_print") };
	/**
	 * The menu commands corresponding to the functions of the tool
	 */
	protected String menuCommands[] = { "sch_start", "sch_load", "sch_summary", "sch_gantt", "sch_matrix", "sch_print" };
	/**
	 * Menu items
	 */
	protected MenuItem menuItems[] = null;
	/**
	 * Path to the directory with the metadata files
	 */
	protected String metadataPath = null;
	/**
	 * Path to the directory with the static data files (vehicle types, suitability, ...)
	 */
	protected String staticDataPath = null;
	/**
	 * Path to the directory with the scheduler input files
	 */
	protected String schedulerInputPath = null;
	/**
	 * Path to the directory with the scheduler output files
	 */
	protected String schedulerOutputPath = null;
	/**
	 * The layer with locations which may (or may not) contain source
	 * locations and/or destination locations and/or locations of vehicles
	 */
	public DGeoLayer locLayer = null;
	/**
	 * A structure containing all data relevant to one transportation schedule:
	 */
	protected ScheduleData scheduleData = null;
	/**
	 * The matrix with the distances and/or travel times between the source and
	 * destination locations
	 */
	public Matrix distancesMatrix = null;
	/**
	 * Computes data about the use of the vehicles
	 */
	public VehicleCounter vehicleCounter = null;
	/**
	 * A table with static data about the vehicles including type, identifier and
	 * name of the home base (initial location) and, possibly, something else.
	 * Produced by VehicleCounter!
	 */
	public DataTable vehicleInfo = null;
	/**
	 * Contains the names of the categories of the transported items. Does not
	 * contain "LEER" or "EMPTY"!
	 */
	protected Vector itemCategories = null;
	/**
	 * Contains the codes of the categories of the transported items, for example:
	 * 10 - general people or children
	 * 12 - infants
	 * 20 - invalids who can seat
	 * 21 - invalids who cannot seat
	 * 22 - disabled people using wheelchairs
	 * 23 - critically sick or injured people
	 * 30 - prisoners
	 */
	protected Vector itemCatCodes = null;
	/**
	 * This is a non-UI object supporting the selection of the item category for viewing
	 * corresponding transportation orders and other relevant data in course of
	 * exploring a transportation schedule. The selection affects all currently
	 * existing displays of the data relevant to the schedule.
	 */
	protected ItemCategorySelector itemCatSelector = null;
	/**
	 * Keeps information about classes of vehicles used for transportation.
	 */
	protected VehicleTypesInfo vehicleTypesInfo = null;
	/**
	 * Contains data about the original numbers of vehicles at their sources
	 */
	protected DataTable vehiclesInSources = null;
	/**
	 * Contains data about the available destinations and their original capacities
	 */
	protected DataTable destCap = null;
	/**
	 * Contains data about the original numbers of items in the sources.
	 */
	protected DataTable itemsInSources = null;
	/**
	 * Computes the dynamics of using the capacities in the destination sites.
	 */
	protected DestinationUseCounter destUseCounter = null;
	/**
	 * Used for selection of time intervals not through the time slider but
	 * through the schedule summary view
	 */
	protected TimeIntervalSelectorImpl timeIntSelector = null;
	/**
	 * Windows with different types of schedule displays
	 */
	protected Frame summaryViewFrame = null, ganttViewFrame = null, matrixViewFrame = null;
	/**
	 * Prepared input data for the scheduler
	 */
	protected SchedulerInputPreparer schInPreparer = null;
	/**
	 * Paths to the results produced by the scheduler
	 */
	private String pathToLatestSchedule = null, //latest result produced
			pathToLastScheduleOfRun = null, //final result of the latest completed run
			pathToCurrentSchedule = null; //currently loaded schedule

	public ScheduleToolsManager(ESDACore core) {
		this.core = core;
		if (FindPegasus.getPathToPegasus() == null) {
			showMessage("Failed to find the external scheduler (Pegasus)!", true);
		}
	}

	/**
	 * Returns the text of the item in the main menu providing access to
	 * the functions of the tool.
	 */
	public String getMenuTitle() {
		return res.getString("schedule");
	}

	/**
	 * Fills the given menu with the items corresponding to the functions of the tool
	 */
	public void fillMenu(Menu menu) {
		menuItems = new MenuItem[menuTexts.length];
		for (int i = 0; i < menuTexts.length; i++) {
			if (menuCommands[i].equals("sch_summary") || menuCommands[i].equals("sch_gantt") || menuCommands[i].equals("sch_matrix")) {
				menuItems[i] = new CheckboxMenuItem(menuTexts[i], false);
				((CheckboxMenuItem) menuItems[i]).addItemListener(this);
			} else {
				menuItems[i] = new MenuItem(menuTexts[i]);
				menuItems[i].addActionListener(this);
			}
			menu.add(menuItems[i]);
			menuItems[i].setActionCommand(menuCommands[i]);
			menuItems[i].setEnabled(menuCommands[i].equals("sch_start") || menuCommands[i].equals("sch_load"));
		}
	}

	/**
	 * Sets a layer with endangered objects, which may contain
	 * people or items subject to evacuation.
	 */
	public void setObjectsToSave(DGeoLayer objToSave) {
		this.objToSave = objToSave;
	}

	/**
	 * Sets a layer with potentially endangered objects, which may contain
	 * people or items subject to evacuation.
	 */
	public void setObjectsPossiblyToSave(DGeoLayer objPossiblyToSave) {
		this.objPossiblyToSave = objPossiblyToSave;
	}

	/**
	 * Sets a layer with locations that may be used as shelters
	 */
	public void setShelters(DGeoLayer shelters) {
		this.shelters = shelters;
	}

	/**
	 * Reacts to selections of appropriate items from the menu
	 */
	public void actionPerformed(ActionEvent e) {
		showMessage(null, false);
		String cmd = e.getActionCommand();
		if (e.getSource().equals(schInPreparer)) {
			for (MenuItem menuItem : menuItems) {
				menuItem.setEnabled(true);
			}
			//Finished data preparation!
			if (cmd.equals("done")) {
				startScheduler();
			}
			return;
		}
		if (cmd == null)
			return;
		if (cmd.equals("sch_load")) {
			loadSchedule(null, false);
			return;
		}
		if (cmd.equals("sch_start")) {
			startOrStopScheduler();
			return;
		}
		if (cmd.equals("sch_print")) {
			if (scheduleData == null || scheduleData.souTbl == null)
				return;
			Vector catNames = null, catCodes = null;
			boolean hasCatNames = itemCategories != null && itemCategories.size() > 1;
			boolean hasCatCodes = itemCatCodes != null && itemCatCodes.size() > 1;
			if (hasCatNames || hasCatCodes) {
				int ncat = (hasCatNames) ? itemCategories.size() : itemCatCodes.size();
				Checkbox catCB[] = new Checkbox[ncat];
				Panel p = new Panel(new ColumnLayout());
				p.add(new Label(res.getString("Select_item_categories") + ":", Label.CENTER));
				for (int i = 0; i < ncat; i++) {
					catCB[i] = new Checkbox((hasCatNames) ? (String) itemCategories.elementAt(i) : (String) itemCatCodes.elementAt(i), true);
					p.add(catCB[i]);
				}
				OKDialog okDia = new OKDialog(core.getUI().getMainFrame(), res.getString("Select_item_categories"), true);
				okDia.addContent(p);
				okDia.show();
				if (okDia.wasCancelled())
					return;
				int nselected = 0;
				for (int i = 0; i < ncat; i++)
					if (catCB[i].getState()) {
						++nselected;
					}
				if (nselected < 1)
					return;
				if (nselected < ncat) {
					if (hasCatNames) {
						catNames = new Vector(nselected, 1);
					}
					if (hasCatCodes) {
						catCodes = new Vector(nselected, 1);
					}
					for (int i = 0; i < ncat; i++)
						if (catCB[i].getState()) {
							if (hasCatNames) {
								catNames.addElement(itemCategories.elementAt(i));
							}
							if (hasCatCodes) {
								catCodes.addElement(itemCatCodes.elementAt(i));
							}
						}
				}
			}
			FileDialog fd = new FileDialog(core.getUI().getMainFrame(), res.getString("File_name"));
			fd.setFile("*.txt");
			fd.setMode(FileDialog.SAVE);
			fd.setVisible(true);
			String path = fd.getDirectory() + fd.getFile();
			if (path == null)
				return;
			SchedulePrinter sp = new SchedulePrinter();
			sp.setOrders(scheduleData.souTbl);
			sp.setVehicleInfo(vehicleInfo);
			sp.setLocLayer(locLayer);
			sp.writeOrdersToFile(path, catNames, catCodes);
			return;
		}
	}

	/**
	 * Reacts to selection/deselection of menu items
	 */
	public void itemStateChanged(ItemEvent e) {
		showMessage(null, false);
		Object item = e.getSource();
		if (item == null || !(item instanceof CheckboxMenuItem))
			return;
		CheckboxMenuItem cbmItem = (CheckboxMenuItem) item;
		if (cbmItem.getActionCommand().equals("sch_summary")) {
			if (cbmItem.getState()) {
				openSummaryView();
			} else if (summaryViewFrame != null) {
				summaryViewFrame.dispose();
				summaryViewClosed();
			}
		} else if (cbmItem.getActionCommand().equals("sch_gantt")) {
			if (cbmItem.getState()) {
				openGanttView();
			} else if (ganttViewFrame != null) {
				ganttViewFrame.dispose();
				ganttViewClosed();
			}
		} else if (cbmItem.getActionCommand().equals("sch_matrix")) {
			if (cbmItem.getState()) {
				openMatrixView();
			} else if (matrixViewFrame != null) {
				matrixViewFrame.dispose();
				matrixViewClosed();
			}
		}
	}

	public String getApplPath() {
		String applPath = core.getDataKeeper().getApplicationPath();
		if (applPath != null) {
			File f = new File(applPath);
			try {
				String str = f.getCanonicalPath();
				applPath = str;
			} catch (Exception e) {
			}
		}
		return applPath;
	}

	/**
	 * Finds the paths to the directories with the metadata, static data,
	 * and scheduler input
	 */
	protected void findPaths() {
		if (metadataPath == null || staticDataPath == null || schedulerInputPath == null || schedulerOutputPath == null) {
			String applPath = getApplPath();
			if (applPath != null) {
				applPath = CopyFile.attachSeparator(CopyFile.getDir(applPath));
			}
			if (metadataPath == null) {
				metadataPath = "metadata";
				if (applPath != null) {
					metadataPath = applPath + "metadata";
				}
				File dir = new File(metadataPath);
				if (!dir.exists() || !dir.isDirectory())
					if (applPath != null) {
						metadataPath = applPath;
					} else {
						metadataPath = "";
					}
				metadataPath = CopyFile.attachSeparator(metadataPath);
			}
			if (staticDataPath == null) {
				staticDataPath = "static_data";
				if (applPath != null) {
					staticDataPath = applPath + "static_data";
				}
				File dir = new File(staticDataPath);
				if (!dir.exists() || !dir.isDirectory())
					if (applPath != null) {
						staticDataPath = applPath;
					} else {
						staticDataPath = "";
					}
				staticDataPath = CopyFile.attachSeparator(staticDataPath);
			}
			if (schedulerInputPath == null) {
				schedulerInputPath = "scheduler_input";
				//schedulerInputPath="scheduler_input_paper";
				if (applPath != null) {
					schedulerInputPath = applPath + "scheduler_input";
				}
				//schedulerInputPath=applPath+"scheduler_input_paper";
				File dir = new File(schedulerInputPath);
				if ((!dir.exists() || !dir.isDirectory()) && !dir.mkdir()) {
					if (applPath != null) {
						schedulerInputPath = applPath;
					} else {
						schedulerInputPath = "";
					}
				}
				schedulerInputPath = CopyFile.attachSeparator(schedulerInputPath);
			}
			if (schedulerOutputPath == null) {
				schedulerOutputPath = "scheduler_output";
				if (applPath != null) {
					schedulerOutputPath = applPath + "scheduler_output";
				}
			}
		}
	}

	private void tryGetSchedulerInputData(String inputPath) {
		findPaths();
		if (itemCategories == null || locLayer == null || itemsInSources == null || destCap == null || vehicleTypesInfo == null || vehicleInfo == null || vehiclesInSources == null) {
			SchedulerInputPreparer schInPreparer = new SchedulerInputPreparer();
			schInPreparer.setCore(core);
			schInPreparer.setPaths(metadataPath, staticDataPath, schedulerInputPath, schedulerOutputPath);
			if (itemCategories != null || itemCatCodes != null) {
				schInPreparer.setItemCategories(itemCatCodes, itemCategories);
			}
			if (locLayer != null) {
				schInPreparer.setLayerWithPotentialPlaces(locLayer);
			}
			if (distancesMatrix != null) {
				schInPreparer.setDistancesMatrix(distancesMatrix);
			}
			if (destCap != null) {
				schInPreparer.setDestinationCapacitiesTable(destCap);
			}
			if (vehicleTypesInfo != null) {
				schInPreparer.setVehicleTypesInfo(vehicleTypesInfo);
			}
			if (itemsInSources != null) {
				schInPreparer.setItemsInSources(itemsInSources);
			}
			if (vehiclesInSources != null) {
				schInPreparer.setVehiclesInSources(vehiclesInSources);
			}
			if (schInPreparer.tryGetPreviousData(inputPath)) {
				getDataFromScheduleInputPreparer(schInPreparer);
			}
		}
	}

	private void getDataFromScheduleInputPreparer(SchedulerInputPreparer schInPreparer) {
		if (schInPreparer == null)
			return;
		if (schInPreparer.locLayer != null) {
			locLayer = schInPreparer.locLayer;
		}
		if (schInPreparer.distancesMatrix != null) {
			distancesMatrix = schInPreparer.distancesMatrix;
		}
		if (schInPreparer.itemCatNames != null) {
			itemCategories = schInPreparer.itemCatNames;
		}
		if (schInPreparer.itemCatCodes != null) {
			itemCatCodes = schInPreparer.itemCatCodes;
		}
		if (schInPreparer.itemsInSources != null) {
			itemsInSources = schInPreparer.itemsInSources;
		}
		if (destCap == null && schInPreparer.destCap != null) {
			destCap = schInPreparer.destCap;
			core.getDataLoader().addTable(destCap);
		}
		if (vehiclesInSources == null && schInPreparer.vehiclesInSources != null) {
			vehiclesInSources = schInPreparer.vehiclesInSources;
			core.getDataLoader().addTable(vehiclesInSources);
		}
		if (vehicleTypesInfo == null && schInPreparer.vehicleTypesInfo != null) {
			vehicleTypesInfo = schInPreparer.vehicleTypesInfo;
			if (vehicleInfo != null) {
				ScheduleExplorer.fillVehicleClassNames(vehicleInfo, vehicleTypesInfo, vehiclesInSources);
				ScheduleExplorer.fillVehicleCapacities(vehicleInfo, itemCategories, vehicleTypesInfo);
			}
		}
	}

	/**
	 * Indicates whether the currently loaded schedule is a result of
	 * the currently working instance of the scheduler.
	 */
	private boolean prevScheduleComesFromScheduler = false;

	/**
	 * Loads a generated schedule from an ASCII file
	 */
	public void loadSchedule(String pathToSchedule, boolean comesFromScheduler) {
		if (core == null || core.getUI() == null)
			return;
		if (core.getUI().getCurrentMapViewer() == null || core.getUI().getCurrentMapViewer().getLayerManager() == null) {
			showMessage(res.getString("no_map_exists"), true);
			return;
		}
		LayerManager lman = core.getUI().getCurrentMapViewer().getLayerManager();
		if (lman.getLayerCount() < 1) {
			showMessage(res.getString("no_map_layers"), true);
			return;
		}
		if (pathToSchedule == null || !CopyFile.fileExists(pathToSchedule)) {
			GetPathDlg fd = new GetPathDlg(core.getUI().getMainFrame(), res.getString("Specify_file_with_orders"));
			fd.setFileMask("*.txt;*.csv");
			fd.show();
			pathToSchedule = fd.getPath();
			if (pathToSchedule == null)
				return;
			comesFromScheduler = false;
		}

		if (scheduleData != null && scheduleData.souTbl != null) {
			if (!Dialogs.askYesOrNo(core.getUI().getMainFrame(), res.getString("confirm_unload"), res.getString("remove_prev_schedule")))
				return;
			boolean completeReload = !prevScheduleComesFromScheduler || !comesFromScheduler;
			if (summaryViewFrame != null) {
				summaryViewFrame.dispose();
				summaryViewClosed();
			}
			if (ganttViewFrame != null) {
				ganttViewFrame.dispose();
				ganttViewClosed();
			}
			if (matrixViewFrame != null) {
				matrixViewFrame.dispose();
				matrixViewClosed();
			}
			if (scheduleData.vehicleSites != null) {
				core.removeMapLayer(scheduleData.vehicleSites.getContainerIdentifier(), true);
			}
			if (scheduleData.aggLinkLayer != null) {
				core.removeMapLayer(scheduleData.aggLinkLayer.getContainerIdentifier(), true);
			}
			if (scheduleData.aggTbl != null) {
				core.removeTable(scheduleData.aggTbl.getContainerIdentifier());
			}
			if (scheduleData.linkLayer != null) {
				core.removeMapLayer(scheduleData.linkLayer.getContainerIdentifier(), true);
			}
			if (scheduleData.itemData != null) {
				core.removeTable(scheduleData.itemData.getContainerIdentifier());
			}
			if (scheduleData.souItemNumLayer != null) {
				core.removeMapLayer(scheduleData.souItemNumLayer.getContainerIdentifier(), true);
			}
			if (scheduleData.destUseLayer != null) {
				core.removeMapLayer(scheduleData.destUseLayer.getContainerIdentifier(), true);
			}
			core.removeTable(scheduleData.souTbl.getContainerIdentifier());
			for (int i = 1; i < menuItems.length; i++) {
				if (menuItems[i] instanceof CheckboxMenuItem) {
					((CheckboxMenuItem) menuItems[i]).setState(false);
				}
				menuItems[i].setEnabled(false);
			}
			vehicleCounter = null;
			scheduleData = null;
			itemCatSelector = null;
			timeIntSelector = null;
			destUseCounter = null;
			if (completeReload) {
				if (schInPreparer != null && schInPreparer.locLayerWasAdded && schInPreparer.locLayer != null) {
					core.removeMapLayer(schInPreparer.locLayer.getContainerIdentifier(), true);
				}
				itemsInSources = null;
				vehiclesInSources = null;
				destCap = null;
				distancesMatrix = null;
				locLayer = null;
				vehicleInfo = null;
				schInPreparer = null;
			}
			if (lman instanceof DLayerManager) {
				((DLayerManager) lman).setGeneralInfoProvider(null);
			}
			pathToCurrentSchedule = null;
		} else {
			vehicleCounter = null;
			scheduleData = null;
			itemCatSelector = null;
			timeIntSelector = null;
			destUseCounter = null;
			if (schInPreparer != null && schInPreparer.locLayerWasAdded && schInPreparer.locLayer != null) {
				core.removeMapLayer(schInPreparer.locLayer.getContainerIdentifier(), true);
			}
			itemsInSources = null;
			vehiclesInSources = null;
			destCap = null;
			distancesMatrix = null;
			locLayer = null;
			vehicleInfo = null;
			schInPreparer = null;
		}

		prevScheduleComesFromScheduler = comesFromScheduler;

		if (schInPreparer == null) {
			String pathToInput = CopyFile.getDir(pathToSchedule) + "input";
			if (CopyFile.fileExists(pathToInput)) {
				pathToInput = CopyFile.attachSeparator(pathToInput);
			} else {
				pathToInput = null;
			}
			tryGetSchedulerInputData(pathToInput);
		} else {
			getDataFromScheduleInputPreparer(schInPreparer);
		}

		ScheduleLoader schLoader = new ScheduleLoader();
		schLoader.setCore(core);
		schLoader.setMetadataPath(metadataPath);
		if (pathToSchedule != null) {
			schLoader.setPathToSchedule(pathToSchedule);
		}
		schLoader.setLocLayer(locLayer);

		DGeoLayer layer = schLoader.loadOrders();
		if (layer == null)
			return;

		pathToCurrentSchedule = schLoader.getPathToSchedule();
		DataTable table = (DataTable) layer.getThematicData();
		DataSourceSpec spec = (DataSourceSpec) table.getDataSource();

		lman.activateLayer(layer.getContainerIdentifier());
		if (lman instanceof DLayerManager) {
			((DLayerManager) lman).setGeneralInfoProvider(this);
		}

		scheduleData = ScheduleExplorer.makeScheduleData(layer, lman);
		if (scheduleData.locLayer == null && locLayer != null) {
			scheduleData.locLayer = locLayer;
			scheduleData.ldd.layerRef = locLayer.getContainerIdentifier();
		} else {
			locLayer = scheduleData.locLayer;
		}
		if (vehicleInfo != null) {
			scheduleData.vehicleInfo = vehicleInfo;
		}
		if (distancesMatrix != null) {
			scheduleData.distancesMatrix = distancesMatrix;
		}
		if (scheduleData.ldd != null && schInPreparer != null && schInPreparer.distancesFName != null) {
			scheduleData.ldd.distancesFilePath = schInPreparer.distancesFName;
		}

		if (scheduleData.itemData == null) {
			ItemCollector iCollector = new ItemCollector();
			if (iCollector.collectItems(scheduleData) && scheduleData.itemData != null) {
				core.getDataLoader().addTable(scheduleData.itemData);
				if (itemsInSources != null) {
					iCollector.completeItemData(scheduleData.itemData, itemsInSources);
				}
				scheduleData.souItemNumLayer = iCollector.makeSourceDynamicsLayer(scheduleData.itemData, scheduleData.locLayer);
				if (scheduleData.souItemNumLayer != null) {
					scheduleData.souItemNumTable = (DataTable) scheduleData.souItemNumLayer.getThematicData();
					DataLoader dataLoader = core.getDataLoader();
					int tableN = core.getDataLoader().addTable(scheduleData.souItemNumTable);
					dataLoader.addMapLayer(scheduleData.souItemNumLayer, -1);
					dataLoader.setLink(scheduleData.souItemNumLayer, tableN);
					scheduleData.souItemNumLayer.setLinkedToTable(true);
					ShowRecManager recMan = null;
					if (dataLoader instanceof DataManager) {
						recMan = ((DataManager) dataLoader).getShowRecManager(tableN);
					}
					if (recMan != null) {
						Vector showAttr = new Vector(scheduleData.souItemNumTable.getAttrCount(), 10);
						for (int i = 0; i < scheduleData.souItemNumTable.getAttrCount(); i++) {
							showAttr.addElement(scheduleData.souItemNumTable.getAttributeId(i));
						}
						recMan.setPopupAddAttrs(showAttr);
					}
				}
			}
		}
		if (scheduleData.itemCatColIdx >= 0 && (itemCategories == null || itemCategories.size() < 1)) {
			IntArray iar = new IntArray(1, 1);
			iar.addElement(scheduleData.itemCatColIdx);
			itemCategories = table.getAllValuesInColumnsAsStrings(iar);
			if (itemCategories != null && itemCategories.size() > 1) {
				int k = StringUtil.indexOfStringInVectorIgnoreCase("LEER", itemCategories);
				if (k < 0) {
					k = StringUtil.indexOfStringInVectorIgnoreCase("EMPTY", itemCategories);
				}
				if (k >= 0) {
					itemCategories.removeElementAt(k);
				}
				if (itemCategories.size() > 1) {
					ScheduleExplorer.orderCategoriesByPreference(ScheduleExplorer.findOrderPrefSpec(spec, table.getAttributeName(scheduleData.itemCatColIdx)), itemCategories, itemCatCodes);
				}
			}
		}

		if (itemCategories.size() > 1) {
			colorOrdersByItemCategory();
			itemCatSelector = new ItemCategorySelector();
			itemCatSelector.setItemCategories(itemCategories);
			itemCatSelector.setScheduleData(scheduleData);
		}
		for (int i = 1; i < menuItems.length; i++) {
			menuItems[i].setEnabled(true);
		}

		if (timeIntSelector == null) {
			timeIntSelector = new TimeIntervalSelectorImpl();
			timeIntSelector.setIncludeIntervalStart(false);
			timeIntSelector.setIncludeIntervalEnd(false);
			timeIntSelector.setDataManager(core.getDataKeeper().getTimeManager());
			if (lman instanceof PropertyChangeListener) {
				timeIntSelector.addTimeSelectionListener((PropertyChangeListener) lman);
			}
		}
		if (destUseCounter == null) {
			destUseCounter = new DestinationUseCounter();
			destUseCounter.setItemCategories(itemCategories);
			destUseCounter.setItemCategoriesCodes(itemCatCodes);
			destUseCounter.setDestinationCapacitiesTable(destCap);
		}
		if (!destUseCounter.countDestinationUse(scheduleData.souTbl, scheduleData.ldd, scheduleData.itemNumColIdx, scheduleData.itemCatColIdx, timeIntSelector.getDataIntervalStart())) {
			destUseCounter = null;
		}
		if (destUseCounter != null) {
			scheduleData.destUseLayer = destUseCounter.makeDestinationUseLayer(scheduleData.locLayer);
			if (scheduleData.destUseLayer != null) {
				scheduleData.destUseTable = (DataTable) scheduleData.destUseLayer.getThematicData();
				DataLoader dataLoader = core.getDataLoader();
				int tableN = core.getDataLoader().addTable(scheduleData.destUseTable);
				dataLoader.addMapLayer(scheduleData.destUseLayer, -1);
				dataLoader.setLink(scheduleData.destUseLayer, tableN);
				scheduleData.destUseLayer.setLinkedToTable(true);
				ShowRecManager recMan = null;
				if (dataLoader instanceof DataManager) {
					recMan = ((DataManager) dataLoader).getShowRecManager(tableN);
				}
				if (recMan != null) {
					Vector showAttr = new Vector(scheduleData.destUseTable.getAttrCount(), 10);
					for (int i = 0; i < scheduleData.destUseTable.getAttrCount(); i++) {
						showAttr.addElement(scheduleData.destUseTable.getAttributeId(i));
					}
					recMan.setPopupAddAttrs(showAttr);
				}
			}
		}

		openSummaryView();
		if (summaryViewFrame != null) {
			Frame mf = core.getUI().getMainFrame();
			if (mf != null) {
				Dimension ss = summaryViewFrame.getToolkit().getScreenSize(), ms = mf.getSize(), fs = summaryViewFrame.getSize();
				int w1 = ms.width, w2 = fs.width;
				if (w1 + w2 > ss.width) {
					w2 = Math.max(ss.width / 3, 700);
					w1 = Math.max(ss.width - w2, 800);
				}
				mf.setBounds(0, 0, w1, ms.height);
				summaryViewFrame.setBounds(ss.width - w2, 0, w2, fs.height);
				CManager.validateAll(mf);
				CManager.validateAll(summaryViewFrame);
			}
			summaryViewFrame.toFront();
		}
		makeAggregatedLinks();
		//setFilterByItemNumber();
		if (itemCatSelector != null) {
			setFilterByItemCategory(null);
			itemCatSelector.addCategoryChangeListener(this);
		}
		if (scheduleData.souItemNumLayer != null) {
			TimeFilter tf = getTimeFilter(scheduleData.souItemNumLayer);
			if (tf != null) {
				tf.setRemoveTransitoryWhenNoFilter(true);
				tf.setRemovePermanentWhenFilter(true);
				tf.clearFilter();
			}
			tf = getTimeFilter(scheduleData.souItemNumTable);
			if (tf != null) {
				tf.setRemoveTransitoryWhenNoFilter(true);
				tf.setRemovePermanentWhenFilter(true);
				tf.clearFilter();
			}
			DisplayProducer displayProducer = core.getDisplayProducer();
			if (displayProducer != null) {
				DataMapper dataMapper = null;
				if (displayProducer.getDataMapper() != null && (displayProducer.getDataMapper() instanceof DataMapper)) {
					dataMapper = (DataMapper) displayProducer.getDataMapper();
				}
				MapViewer mapView = core.getUI().getCurrentMapViewer();
				if (dataMapper != null && mapView != null) {
					Vector attr = new Vector(2, 1);
					attr.add("number");
					attr.add("number_delayed");
					scheduleData.souItemNumTable.getSemanticsManager().addInclusionRelationship(attr);
					Object vis = dataMapper.constructVisualizer("pies", scheduleData.souItemNumLayer.getType());
					Visualizer visualizer = displayProducer.displayOnMap(vis, "pies", scheduleData.souItemNumTable, attr, scheduleData.souItemNumLayer, false, mapView);
					if (visualizer instanceof PieChartDrawer) {
						PieChartDrawer pied = (PieChartDrawer) vis;
						pied.setColorForAttribute(Color.pink, 0);
						pied.setColorForAttribute(Color.red, 1);
						pied.setUseSize(true);
						pied.setUseMinSize(true);
						pied.setFocuserMinMax(1, pied.getSumMAX());
						pied.notifyVisChange();
					}
					core.getSupervisor().registerTool(visualizer);
				}
			}
		}
		if (scheduleData.destUseLayer != null) {
			TimeFilter tf = getTimeFilter(scheduleData.destUseLayer);
			if (tf != null) {
				tf.setRemoveTransitoryWhenNoFilter(true);
				tf.setRemovePermanentWhenFilter(true);
				tf.clearFilter();
			}
			tf = getTimeFilter(scheduleData.destUseTable);
			if (tf != null) {
				tf.setRemoveTransitoryWhenNoFilter(true);
				tf.setRemovePermanentWhenFilter(true);
				tf.clearFilter();
			}
			DisplayProducer displayProducer = core.getDisplayProducer();
			if (displayProducer != null) {
				DataMapper dataMapper = null;
				if (displayProducer.getDataMapper() != null && (displayProducer.getDataMapper() instanceof DataMapper)) {
					dataMapper = (DataMapper) displayProducer.getDataMapper();
				}
				MapViewer mapView = core.getUI().getCurrentMapViewer();
				if (dataMapper != null && mapView != null) {
					Vector attr = new Vector(3, 1);
					attr.add("capacity");
					attr.add("number");
					attr.add("rest_capacity");
					scheduleData.destUseTable.getSemanticsManager().setAttributesComparable(attr);
					attr.removeElementAt(0);
					Object vis = dataMapper.constructVisualizer("parallel_bars", scheduleData.destUseLayer.getType());
					Visualizer visualizer = displayProducer.displayOnMap(vis, "parallel_bars", scheduleData.destUseTable, attr, scheduleData.destUseLayer, false, mapView);
					if (visualizer instanceof MultiBarDrawer) {
						MultiBarDrawer barDrawer = (MultiBarDrawer) vis;
						barDrawer.setColorForAttribute(CapacityUseData.state_colors[CapacityUseData.capacity_partly_filled], 0);
						barDrawer.setColorForAttribute(CapacityUseData.state_colors[CapacityUseData.capacity_fully_unused], 1);
						barDrawer.notifyVisChange();
					}
					core.getSupervisor().registerTool(visualizer);
				}
			}
		}
	}

	protected void colorOrdersByItemCategory() {
		if (scheduleData == null || scheduleData.souTbl == null || scheduleData.linkLayer == null)
			return;
		DataSourceSpec spec = (DataSourceSpec) scheduleData.souTbl.getDataSource();
		if (spec.extraInfo != null && spec.extraInfo.get("ITEM_CLASS_FIELD_NAME") != null) {
			String visMethodId = null;
			Vector attr = new Vector(2, 1);
			int idx = scheduleData.souTbl.findAttrByName((String) spec.extraInfo.get("ITEM_CLASS_FIELD_NAME"));
			if (idx >= 0) {
				attr.addElement(scheduleData.souTbl.getAttributeId(idx));
				visMethodId = "qualitative_colour";
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
						Object vis = dataMapper.constructVisualizer(visMethodId, scheduleData.linkLayer.getType());
						Visualizer visualizer = displayProducer.displayOnMap(vis, visMethodId, scheduleData.souTbl, attr, scheduleData.linkLayer, false, mapView);
						core.getSupervisor().registerTool(visualizer);
						Classifier cl = null;
						if (scheduleData.linkLayer.getVisualizer() != null && (scheduleData.linkLayer.getVisualizer() instanceof ClassDrawer)) {
							cl = ((ClassDrawer) scheduleData.linkLayer.getVisualizer()).getClassifier();
						}
						if (cl != null) {
							core.getSupervisor().setObjectColorer(cl);
						}
					}
				}
			}
		}
	}

	protected void makeAggregatedLinks() {
		if (scheduleData == null || scheduleData.souTbl == null || scheduleData.linkLayer == null || scheduleData.ldd == null)
			return;
		if (distancesMatrix == null) {
			if (schInPreparer != null && schInPreparer.distancesFName != null) {
				scheduleData.ldd.distancesFilePath = schInPreparer.distancesFName;
			}
			showMessage(res.getString("try_load_distances_from_") + schedulerInputPath + scheduleData.ldd.distancesFilePath, false);
			DistancesLoader distLoader = new DistancesLoader();
			distancesMatrix = scheduleData.distancesMatrix = distLoader.loadDistances(schedulerInputPath + scheduleData.ldd.distancesFilePath, getApplPath());
		}
		//build a table and a layer on the basis of the aggregated data
		showMessage(res.getString("wait_aggr_progress"), false);
		if (scheduleData.moveAggregator == null) {
			scheduleData.moveAggregator = new MovementAggregator(scheduleData.souTbl, scheduleData.ldd, scheduleData.souNameColIdx, scheduleData.destNameColIdx, scheduleData.itemNumColIdx, scheduleData.vehicleIdColIdx);
		}
		AggrScheduleLayerBuilder agb = new AggrScheduleLayerBuilder(core, scheduleData);
		agb.buildAggregateLinks();
		scheduleData.linkLayer.setLayerDrawn(false);
		showMessage(null, false);
	}

	protected void openSummaryView() {
		if (summaryViewFrame != null)
			return;
		if (scheduleData == null)
			return;
		if (vehicleCounter == null) {
			vehicleCounter = new VehicleCounter();
			if (vehicleTypesInfo != null && vehicleTypesInfo.vehicleCap != null) {
				vehicleCounter.setVehicleCapacityTable(vehicleTypesInfo.vehicleCap);
			}
			if (itemCategories != null) {
				vehicleCounter.setItemCategories(itemCategories);
			}
			if (scheduleData.itemData != null) {
				vehicleCounter.setItemData(scheduleData.itemData);
			}
			if (vehicleCounter.countVehicles(scheduleData.souTbl, scheduleData.ldd, scheduleData.vehicleIdColIdx, scheduleData.vehicleTypeColIdx, scheduleData.vehicleHomeIdColIdx, scheduleData.vehicleHomeNameColIdx, scheduleData.itemNumColIdx,
					scheduleData.itemCatColIdx, vehiclesInSources)) {
				showMessage(res.getString("got_data_vehicle_use"), false);
				if (vehicleInfo == null && vehicleCounter.vehicleInfo != null) {
					vehicleInfo = scheduleData.vehicleInfo = vehicleCounter.vehicleInfo;
					core.getDataLoader().addTable(vehicleInfo);
					ScheduleExplorer.fillVehicleClassNames(vehicleInfo, vehicleTypesInfo, vehiclesInSources);
					ScheduleExplorer.fillVehicleCapacities(vehicleInfo, itemCategories, vehicleTypesInfo);
					showVehiclePositions(vehicleCounter.vehicleVisitsAggregator);
				}
				if (vehicleTypesInfo != null && vehicleTypesInfo.vehicleCap != null) {
					vehicleCounter.computeCapacityUse();
				}
			} else {
				showMessage(res.getString("failed_get_data_vehicle_use"), true);
				vehicleCounter = null;
				return;
			}
		}

		TimeSegmentedBarPanel summaryView = new TimeSegmentedBarPanel(vehicleCounter, destUseCounter, itemCatSelector);
		summaryView.setName(res.getString("schedule_summary"));
		summaryViewFrame = core.getDisplayProducer().makeWindow(summaryView, summaryView.getName());
		summaryViewFrame.addWindowListener(this);
		((CheckboxMenuItem) getMenuItemById("sch_summary")).setState(true);
		summaryView.setTimeIntervalSelector(timeIntSelector);
	}

	protected void showVehiclePositions(DataAggregator vehiclePosAggregator) {
		if (vehiclePosAggregator == null)
			return;
		DataTable aggrTable = vehiclePosAggregator.getAggregateDataTable();
		if (aggrTable == null || aggrTable.getDataItemCount() < 1)
			return;
		Vector sites = null;
		if (scheduleData.locLayer != null) {
			sites = new Vector(aggrTable.getDataItemCount(), 1);
			//add the names of the sites
			for (int i = 0; i < aggrTable.getDataItemCount(); i++) {
				DataRecord rec = aggrTable.getDataRecord(i);
				DGeoObject site = (DGeoObject) scheduleData.locLayer.findObjectById(rec.getId());
				if (site != null && site.getName() != null) {
					rec.setName(site.getName());
					DGeoObject site1 = (DGeoObject) site.makeCopy();
					site1.setThematicData(rec);
					sites.addElement(site1);
				}
			}
		}
		int tblN = core.getDataLoader().addTable(aggrTable);
		core.getDataLoader().processTimeReferencedObjectSet(aggrTable);
		setFilterByNumber(aggrTable, "current_num", 1);
		scheduleData.aggrDataVehicleSites = aggrTable;
		vehiclePosAggregator.accountForFilter();
		if (sites != null && sites.size() > 0) {
			DGeoLayer siteLayer = new DGeoLayer();
			siteLayer.setName(res.getString("pos_vehicles"));
			siteLayer.setType(scheduleData.locLayer.getType());
			siteLayer.setGeoObjects(sites, true);
			DrawingParameters dp = siteLayer.getDrawingParameters();
			if (dp == null) {
				dp = new DrawingParameters();
				siteLayer.setDrawingParameters(dp);
			}
			dp.lineColor = Color.magenta;
			dp.lineWidth = 1;
			dp.fillColor = Color.magenta.brighter();
			dp.fillContours = false;
			dp.drawLabels = false;
			dp.labelColor = Color.gray;
			dp.transparency = 50;
			core.getDataLoader().addMapLayer(siteLayer, -1);
			core.getDataLoader().setLink(siteLayer, tblN);
			siteLayer.setLinkedToTable(true);
			scheduleData.vehicleSites = siteLayer;
			//visualise the data
			DisplayProducer displayProducer = core.getDisplayProducer();
			if (displayProducer != null) {
				DataMapper dataMapper = null;
				if (displayProducer.getDataMapper() != null && (displayProducer.getDataMapper() instanceof DataMapper)) {
					dataMapper = (DataMapper) displayProducer.getDataMapper();
				}
				MapViewer mapView = core.getUI().getCurrentMapViewer();
				if (dataMapper != null && mapView != null) {
					String visMethodId = "value_paint";
					Vector attr = new Vector(1, 1);
					attr.addElement("current_num");
					Object vis = dataMapper.constructVisualizer(visMethodId, siteLayer.getType());
					if (vis instanceof NumberDrawer) {
						float hsb[] = new float[3];
						Color.RGBtoHSB(255, 0, 255, hsb);
						float h1 = hsb[0];
						Color.RGBtoHSB(0, 0, 255, hsb);
						float h2 = hsb[0];
						((NumberDrawer) vis).setColors(h1, h2);
					}
					if (vis instanceof NumValueSignPainter) {
						NumValueSignPainter sp = (NumValueSignPainter) vis;
						sp.setDiameter(Math.round(2.5f * Toolkit.getDefaultToolkit().getScreenResolution() / 25.33f));
						sp.setBorderColor(Color.magenta);
						sp.setDrawBorder(true);
					}
					Visualizer visualizer = displayProducer.displayOnMap(vis, visMethodId, aggrTable, attr, siteLayer, false, mapView);
					core.getSupervisor().registerTool(visualizer);
				}
			}
		}
		vehiclePosAggregator.setSupervisor(core.getSupervisor());
		if (timeIntSelector != null) {
			timeIntSelector.addTimeSelectionListener(vehiclePosAggregator);
		}
	}

	protected void openGanttView() {
		TimeLineView tlv = new TimeLineView(scheduleData.souTbl);
		tlv.setName(res.getString("Gantt_chart_") + scheduleData.linkLayer.getName());
		tlv.setSupervisor(core.getSupervisor());
		if (itemCatSelector != null) {
			tlv.setListSelector(itemCatSelector);
			tlv.addUIElement(new ItemCategorySelectUI(itemCatSelector));
		}
		ganttViewFrame = core.getDisplayProducer().makeWindow(tlv, tlv.getName());
		ganttViewFrame.addWindowListener(this);
		((CheckboxMenuItem) getMenuItemById("sch_gantt")).setState(true);
		if (scheduleData.vehicleTypeColIdx >= 0 || scheduleData.vehicleIdColIdx >= 0 || scheduleData.vehicleHomeIdColIdx >= 0 || scheduleData.vehicleHomeNameColIdx >= 0) {
			Vector sortAttrIds = new Vector(5, 10);
			if (scheduleData.vehicleTypeColIdx >= 0) {
				sortAttrIds.addElement(scheduleData.souTbl.getAttributeId(scheduleData.vehicleTypeColIdx));
			}
			if (scheduleData.vehicleHomeNameColIdx >= 0) {
				sortAttrIds.addElement(scheduleData.souTbl.getAttributeId(scheduleData.vehicleHomeNameColIdx));
			} else if (scheduleData.vehicleHomeIdColIdx >= 0) {
				sortAttrIds.addElement(scheduleData.souTbl.getAttributeId(scheduleData.vehicleHomeIdColIdx));
			}
			if (scheduleData.vehicleIdColIdx >= 0) {
				sortAttrIds.addElement(scheduleData.souTbl.getAttributeId(scheduleData.vehicleIdColIdx));
			}
			if (sortAttrIds.size() > 0) {
				tlv.sortBy(sortAttrIds);
			}
		}
	}

	protected void openMatrixView() {
		if (scheduleData.moveAggregator == null) {
			scheduleData.moveAggregator = new MovementAggregator(scheduleData.souTbl, scheduleData.ldd, scheduleData.souNameColIdx, scheduleData.destNameColIdx, scheduleData.itemNumColIdx, scheduleData.vehicleIdColIdx);
		}
		MovementMatrixPanel matr = new MovementMatrixPanel(core.getSupervisor(), (scheduleData.locLayer == null) ? null : scheduleData.locLayer.getEntitySetIdentifier(), (scheduleData.aggLinkLayer == null) ? null
				: scheduleData.aggLinkLayer.getEntitySetIdentifier(), scheduleData.souTbl, scheduleData.moveAggregator, distancesMatrix, false);
		matr.setName(res.getString("move_matrix_") + scheduleData.linkLayer.getName());
		if (itemCatSelector != null) {
			matr.addUIElement(new ItemCategorySelectUI(itemCatSelector));
		}
		matr.addSiteSelectionListener(this);
		matrixViewFrame = core.getDisplayProducer().makeWindow(matr, matr.getName());
		matrixViewFrame.addWindowListener(this);
		((CheckboxMenuItem) getMenuItemById("sch_matrix")).setState(true);
		if (scheduleData.firstUpdatableColIdx >= 0) {
			matr.addPropertyChangeListener(this);
		}
	}

	/**
	 * Shows in the map legend what information about the schedule is
	 * currently displayed, according to the currently imposed filters
	 * The argument startY specifies the vertical position from which the
	 * LegendDrawer should start drawing its part of the legend.
	 * The argument leftMarg specifies the left margin (amount of space on
	 * the left to be kept blank). The argument prefW specifies the preferable
	 * width of the legend (to avoid horizontal scrolling).
	 * The method should return the rectangle occupied by the drawn part of
	 * the legend.
	 */
	public Rectangle drawLegend(Component c, Graphics g, int startY, int leftmarg, int prefW) {
		int w = 0, h = 0, y = startY;
		if (itemCatSelector != null) {
			String catName = itemCatSelector.getSelectedCategory();
			if (catName == null) {
				catName = res.getString("all_cat");
			}
			String str = res.getString("item_cat") + ": " + catName;
			Point p = StringInRectangle.drawText(g, str, leftmarg, y, prefW, false);
			h = p.y - startY;
			w = p.x;
			y = p.y;
		}
		if (timeIntSelector != null) {
			TimeMoment t1 = timeIntSelector.getCurrIntervalStart(), t2 = timeIntSelector.getCurrIntervalEnd();
			if (t1 != null || t2 != null) {
				String str = res.getString("time_int_");
				if (t1 != null) {
					str += " " + res.getString("from") + " " + t1.toString();
				}
				if (t2 != null) {
					str += " " + res.getString("till") + " " + t2.toString();
				}
				Point p = StringInRectangle.drawText(g, str, leftmarg, y, prefW, false);
				h = p.y - startY;
				w = p.x;
				y = p.y;
			}
		}
		if (h < 1)
			return null;
		return new Rectangle(leftmarg, startY, w, h);
	}

	/**
	 * Starts or stops (kills) the external scheduler
	 */
	protected void startOrStopScheduler() {
		if (schedulerThread == null) {//start the scheduler
			if (FindPegasus.getPathToPegasus() == null) {
				showMessage("Failed to find the external scheduler (Pegasus)!", true);
				return;
			}
			for (MenuItem menuItem : menuItems) {
				menuItem.setEnabled(false);
			}
			if (schInPreparer != null && schInPreparer.isRunning())
				return;
			prepareInputForScheduler();
		} else {//stop the scheduler
			stopScheduler();
			if (pathToLatestSchedule != null && !CopyFile.sameFiles(pathToLatestSchedule, pathToCurrentSchedule))
				if (pathToLastScheduleOfRun == null || CopyFile.sameFiles(pathToLastScheduleOfRun, pathToLatestSchedule) || CopyFile.sameFiles(pathToLastScheduleOfRun, pathToCurrentSchedule)) {
					if (Dialogs.askYesOrNo(core.getUI().getMainFrame(), res.getString("Load_latest_schedule" + "?"), res.getString("Load_schedule" + "?"))) {
						loadSchedule(pathToLatestSchedule, true);
					}
				} else {
					SelectDialog selDia = new SelectDialog(core.getUI().getMainFrame(), res.getString("Load_schedule" + "?"), res.getString("Load_schedule_version" + "?"));
					selDia.addOption(res.getString("latest_schedule"), "1", false);
					selDia.addOption(res.getString("final_schedule_prev_run"), "2", false);
					selDia.show();
					if (selDia.wasCancelled())
						return;
					if (selDia.getSelectedOptionN() == 1) {
						loadSchedule(pathToLastScheduleOfRun, true);
					} else if (selDia.getSelectedOptionN() == 0) {
						loadSchedule(pathToLatestSchedule, true);
					}
				}
		}
	}

	/**
	 * Starts the external scheduler
	 */
	protected void startScheduler() {
		if (schedulerThread == null) { //start the scheduler
			Panel p = new Panel(new ColumnLayout());
			p.add(new Label(res.getString("sch_start"), Label.CENTER));
			Panel pp = new Panel(new BorderLayout());
			p.add(pp);
			pp.add(new Label(res.getString("allowed_time_")), BorderLayout.WEST);
			TextField tf = new TextField("5", 2);
			pp.add(tf, BorderLayout.CENTER);
			OKDialog okDia = new OKDialog(core.getUI().getMainFrame(), res.getString("sch_start"), true);
			okDia.addContent(p);
			long time = 0l;
			do {
				okDia.show();
				if (!okDia.wasCancelled()) {
					String str = tf.getText();
					if (str != null) {
						try {
							time = Long.parseLong(str.trim());
						} catch (NumberFormatException nfe) {
						}
					}
					if (time < 1) {
						Dialogs.showMessage(core.getUI().getMainFrame(), res.getString("invalid_time"), res.getString("Error!"));
					}
				}
			} while (!okDia.wasCancelled() && time < 1);
			if (okDia.wasCancelled())
				return;
			time *= 60 * 1000;
			prevScheduleComesFromScheduler = false;

			schedulerThread = new RunSchedulerThread();
			schedulerThread.setSchedulerListener(this);
			schedulerThread.setWaitTime(time);
			schedulerThread.setMaxRunTime(time);
			if (!schedulerThread.createScheduler()) {
				showMessage(res.getString("failed_init_scheduler"), true);
				schedulerThread = null;
				return;
			}
			//create the setup file for the scheduler
			String pegasusDir = FindPegasus.getPathToPegasus();
			String setupFileName = pegasusDir + "/setup_file";
			FileWriter fwr = null;
			try {
				fwr = new FileWriter(setupFileName, false);
			} catch (IOException ioe) {
				showMessage(res.getString("failed_make_setup_file"), true);
				schedulerThread = null;
				return;
			}
			try {
				fwr.write(schedulerOutputPath + "\r\n");
				fwr.write(schedulerInputPath + schInPreparer.itemClassesFName + "\r\n");
				fwr.write(schedulerInputPath + schInPreparer.vehicleClassesFName + "\r\n");
				fwr.write(schedulerInputPath + schInPreparer.loadTimesFName + "\r\n");
				fwr.write(schedulerInputPath + schInPreparer.sourcesFName + "\r\n");
				fwr.write(schedulerInputPath + schInPreparer.destFName + "\r\n");
				fwr.write(schedulerInputPath + schInPreparer.vehiclesSuitFName + "\r\n");
				fwr.write(schedulerInputPath + schInPreparer.distancesFName + "\r\n");
				fwr.write(schedulerInputPath + schInPreparer.vehiclesFName + "\r\n");
			} catch (IOException ioe) {
				showMessage(res.getString("cannot_write_to") + " " + setupFileName + ": " + ioe.toString(), true);
				schedulerThread = null;
				return;
			}
			try {
				fwr.close();
			} catch (IOException ioe) {
			}
/*
      if (true) {
        schedulerThread=null;
        return;
      }
/**/
			schedulerThread.start();
			showMessage(res.getString("scheduler_working"), false);

			pathToLatestSchedule = null;
			pathToLastScheduleOfRun = null;

			MenuItem mit = getMenuItemById("sch_start");
			mit.setLabel(stopSchedulerText);
			mit.setEnabled(true);
			//make a window to display the status...
			//...
		}
	}

	/**
	 * Stops (kills) the external scheduler
	 */
	protected void stopScheduler() {
		if (schedulerThread != null) {
			schedulerThread.stopScheduler();
			schedulerThread = null;
			showMessage(res.getString("scheduler_stopped"), false);
		}
		if (pathToLatestSchedule != null) {
			String path = CopyFile.getDir(pathToLatestSchedule);
			Vector schedules = CopyFile.getFileList(path, "txt"), sh = CopyFile.getFileList(path, "csv");
			if (sh != null)
				if (schedules == null) {
					schedules = sh;
				} else {
					for (int i = 0; i < sh.size(); i++) {
						schedules.addElement(sh.elementAt(i));
					}
				}
			if (schedules != null) {
				CopyFile.eraseFilesInDirectory(path, schedules);
			} else {
				CopyFile.deleteDirectory(path);
			}
		}
	}

	/**
	 * Puts the latest input files of the scheduler in a subdirectory of the
	 * output directory of the scheduler
	 */
	protected void saveLastSchedulerInput(String schedulerOutputPath) {
		if (schedulerOutputPath == null || schInPreparer == null)
			return;
		File fdir = new File(schedulerOutputPath + "input");
		if (fdir.exists() && fdir.isDirectory())
			return; //already saved
		if (!fdir.mkdir())
			return;
		String path = fdir.getPath();
		path = CopyFile.attachSeparator(path);
		CopyFile.copyFile(schedulerInputPath + schInPreparer.itemClassesFName, path + schInPreparer.itemClassesFName);
		CopyFile.copyFile(schedulerInputPath + schInPreparer.vehicleClassesFName, path + schInPreparer.vehicleClassesFName);
		CopyFile.copyFile(schedulerInputPath + schInPreparer.loadTimesFName, path + schInPreparer.loadTimesFName);
		CopyFile.copyFile(schedulerInputPath + schInPreparer.sourcesFName, path + schInPreparer.sourcesFName);
		CopyFile.copyFile(schedulerInputPath + schInPreparer.sourcesExtFName, path + schInPreparer.sourcesExtFName);
		CopyFile.copyFile(schedulerInputPath + schInPreparer.destFName, path + schInPreparer.destFName);
		CopyFile.copyFile(schedulerInputPath + schInPreparer.vehiclesSuitFName, path + schInPreparer.vehiclesSuitFName);
		CopyFile.copyFile(schedulerInputPath + schInPreparer.distancesFName, path + schInPreparer.distancesFName);
		CopyFile.copyFile(schedulerInputPath + schInPreparer.vehiclesFName, path + schInPreparer.vehiclesFName);
		CopyFile.copyFile(schedulerInputPath + schInPreparer.allPlacesFName, path + schInPreparer.allPlacesFName);
	}

	private TimeFilter getTimeFilter(ObjectContainer container) {
		if (container == null)
			return null;
		ObjectFilter of = container.getObjectFilter();
		if (of == null)
			return null;
		if (of instanceof TimeFilter)
			return (TimeFilter) of;
		if (of instanceof CombinedFilter) {
			CombinedFilter cFilter = (CombinedFilter) of;
			for (int i = 0; i < cFilter.getFilterCount(); i++)
				if (cFilter.getFilter(i) instanceof TimeFilter)
					return (TimeFilter) cFilter.getFilter(i);
		}
		return null;
	}

	private TableFilter getTableFilter(DataTable table) {
		if (table == null)
			return null;
		ObjectFilter of = table.getObjectFilter();
		if (of == null)
			return null;
		if (of instanceof TableFilter)
			return (TableFilter) of;
		if (of instanceof CombinedFilter) {
			CombinedFilter cFilter = (CombinedFilter) of;
			for (int i = 0; i < cFilter.getFilterCount(); i++)
				if (cFilter.getFilter(i) instanceof TableFilter)
					return (TableFilter) cFilter.getFilter(i);
		}
		return null;
	}

	private ObjectFilterBySelection getSelectionFilter(DataTable table) {
		if (table == null)
			return null;
		ObjectFilter of = table.getObjectFilter();
		if (of == null)
			return null;
		if (of instanceof ObjectFilterBySelection)
			return (ObjectFilterBySelection) of;
		if (of instanceof CombinedFilter) {
			CombinedFilter cFilter = (CombinedFilter) of;
			for (int i = 0; i < cFilter.getFilterCount(); i++)
				if (cFilter.getFilter(i) instanceof ObjectFilterBySelection)
					return (ObjectFilterBySelection) cFilter.getFilter(i);
		}
		return null;
	}

	private static void setQualAttrCondition(DataTable table, TableFilter filter, int aIdx, String value) {
		if (table == null || filter == null || aIdx < 0)
			return;
		AttrCondition ac = filter.getConditionForAttr(aIdx);
		QualAttrCondition qac = null;
		if (ac != null && (ac instanceof QualAttrCondition)) {
			qac = (QualAttrCondition) ac;
		}
		if (qac == null) {
			qac = new QualAttrCondition();
			qac.setTable(table);
			qac.setAttributeIndex(aIdx);
			qac.setMissingValuesOK(false);
			filter.addAttrCondition(qac);
		}
		if (value != null) {
			Vector vals = new Vector(1, 1);
			vals.addElement(value);
			qac.setRightValues(vals);
		} else {
			qac.clearLimits();
		}
	}

	private void setFilterByItemCategory(String currentCategory) {
		if (scheduleData == null)
			return;
		if (vehicleCounter != null && scheduleData.vehicleInfo != null) {
			ObjectFilterBySelection selFilter = getSelectionFilter(scheduleData.vehicleInfo);
			if (selFilter == null) {
				selFilter = new ObjectFilterBySelection();
				selFilter.setObjectContainer(scheduleData.vehicleInfo);
				selFilter.setEntitySetIdentifier(scheduleData.vehicleInfo.getEntitySetIdentifier());
				scheduleData.vehicleInfo.setObjectFilter(selFilter);
			}
			selFilter.setActiveObjects(vehicleCounter.getSuitableVehicleIds(currentCategory));
		}
		if (currentCategory == null) {
			currentCategory = "all";
		}
		if (scheduleData.souItemNumTable != null) {
			TableFilter filter = getTableFilter(scheduleData.souItemNumTable);
			if (filter != null) {
				int aIdx = scheduleData.souItemNumTable.getAttrIndex("class_name");
				if (aIdx < 0) {
					aIdx = scheduleData.souItemNumTable.getAttrIndex("class_code");
				}
				if (aIdx >= 0) {
					setQualAttrCondition(scheduleData.souItemNumTable, filter, aIdx, currentCategory);
					filter.notifyFilterChange();
				}
			}
		}
		if (scheduleData.destUseTable != null) {
			TableFilter filter = getTableFilter(scheduleData.destUseTable);
			if (filter != null) {
				int aIdx = scheduleData.destUseTable.getAttrIndex("class_name");
				if (aIdx < 0) {
					aIdx = scheduleData.destUseTable.getAttrIndex("class_code");
				}
				if (aIdx >= 0) {
					setQualAttrCondition(scheduleData.destUseTable, filter, aIdx, currentCategory);
					filter.notifyFilterChange();
				}
				if (scheduleData.destUseLayer != null && scheduleData.destUseLayer.getVisualizer() != null && (scheduleData.destUseLayer.getVisualizer() instanceof MultiBarDrawer)) {
					Vector attr = new Vector(2, 1);
					attr.add("number");
					attr.add("rest_capacity");
					NumRange nr = scheduleData.destUseTable.getAttrValueRange(attr, true);
					if (nr != null) {
						((MultiBarDrawer) scheduleData.destUseLayer.getVisualizer()).setFocuserMinMax(nr.minValue, nr.maxValue);
					}
				}
			}
		}
	}

	private void setFilterBySourceAndDestination(String souId, String destId) {
		if (scheduleData == null)
			return;
		if (scheduleData.souTbl != null) {
			if (scheduleData.ldd == null || scheduleData.ldd.souColIdx < 0 || scheduleData.ldd.destColIdx < 0)
				return;
			TableFilter filter = getTableFilter(scheduleData.souTbl);
			if (filter == null)
				return;
			setQualAttrCondition(scheduleData.souTbl, filter, scheduleData.ldd.souColIdx, souId);
			setQualAttrCondition(scheduleData.souTbl, filter, scheduleData.ldd.destColIdx, destId);
			filter.notifyFilterChange();
		}
		if (scheduleData.souItemNumTable != null) {
			TableFilter filter = getTableFilter(scheduleData.souItemNumTable);
			if (filter != null) {
				int aIdx = scheduleData.souItemNumTable.getAttrIndex("source_id");
				if (aIdx >= 0) {
					setQualAttrCondition(scheduleData.souItemNumTable, filter, aIdx, souId);
					filter.notifyFilterChange();
				}
			}
		}
		/*
		if (scheduleData.destUseTable!=null) {
		  TableFilter filter=getTableFilter(scheduleData.destUseTable);
		  if (filter!=null) {
		    int aIdx=scheduleData.destUseTable.getAttrIndex("dest_id");
		    if (aIdx>=0) {
		      setQualAttrCondition(scheduleData.destUseTable,filter,aIdx,destId);
		      filter.notifyFilterChange();
		    }
		  }
		}
		*/
	}

	private void setFilterByNumber(DataTable table, String attrId, int minValue) {
		if (table == null)
			return;
		TableFilter filter = getTableFilter(table);
		if (filter != null) {
			int aIdx = table.getAttrIndex(attrId);
			if (aIdx >= 0) {
				AttrCondition ac = filter.getConditionForAttr(aIdx);
				if (ac == null) {
					filter.addQueryAttribute(aIdx);
					ac = filter.getConditionForAttr(aIdx);
				}
				if (ac != null && (ac instanceof NumAttrCondition)) {
					NumAttrCondition nac = (NumAttrCondition) ac;
					ac.setAllowAdjust(false);
					nac.setMinLimit(minValue);
					filter.notifyFilterChange();
				}
			}
		}
	}

	/**
	* Reacts to changes of the aggregation attribute in the matrix view
	*/
	public void propertyChange(PropertyChangeEvent pce) {
		if (pce.getPropertyName().equals("category_selection")) {
			setFilterByItemCategory((String) pce.getNewValue());
		} else if (pce.getPropertyName().equals("site_selection")) {
			setFilterBySourceAndDestination((String) pce.getOldValue(), (String) pce.getNewValue());
		} else if (pce.getSource() instanceof MovementMatrixPanel) {
			MovementMatrixPanel matr = (MovementMatrixPanel) pce.getSource();
			if (pce.getPropertyName().equals("destroy")) {
				matr.removePropertyChangeListener(this);
			} else {
				if (scheduleData.aggLinkLayer == null || !(scheduleData.aggLinkLayer instanceof DAggregateLinkLayer))
					return;
				if (pce.getNewValue() == null || !(pce.getNewValue() instanceof Integer))
					return;
				int aIdx = ((Integer) pce.getNewValue()).intValue();
				if (aIdx < 0)
					return;
				DAggregateLinkLayer aggL = (DAggregateLinkLayer) scheduleData.aggLinkLayer;
				if (aIdx == 0) {
					aggL.setThicknessColN(-1, false);
				} else {
					aggL.setThicknessColN(scheduleData.firstUpdatableColIdx + aIdx - 1, true);
				}
			}
		} else if (pce.getSource().equals(schedulerThread) || (pce.getSource() instanceof Pegasus)) {
			System.out.println("Property change event:\n>>> " + pce.getPropertyName() + ": " + pce.getOldValue() + "; " + pce.getNewValue());
			String message = null;
			boolean finished = false;
			if (pce.getPropertyName().equals("scheduler_finish")) {
				message = res.getString("scheduler_finished");
				if (schedulerThread != null) {
					stopScheduler();
				}
				MenuItem mit = getMenuItemById("sch_start");
				mit.setLabel(startSchedulerText);
				finished = true;
			} else if (pce.getPropertyName().equals("scheduler_signal")) {
				message = res.getString("scheduler_run_complete");
				pathToLastScheduleOfRun = pathToLatestSchedule;
			} else if (pce.getPropertyName().equals("wait_time_passed") || pce.getPropertyName().equals("max_time_passed")) {
				message = res.getString("wait_time_passed");
				if (pathToLatestSchedule == null) {
					Panel p = new Panel(new ColumnLayout());
					p.add(new Label(message));
					p.add(new Label(res.getString("no_result_yet")));
					p.add(new Label(res.getString("let_scheduler_continue_")));
					OKDialog okDia = new OKDialog(core.getUI().getMainFrame(), res.getString("scheduler_status"), OKDialog.YES_NO_MODE, true);
					okDia.addContent(p);
					okDia.show();
					if (okDia.wasCancelled()) {
						stopScheduler();
					}
					return;
				}
			} else if (pce.getPropertyName().startsWith("scheduler_") && pce.getOldValue() != null && (pce.getOldValue() instanceof String)) {
				boolean first = pathToLatestSchedule == null;
				pathToLatestSchedule = (String) pce.getOldValue();
				if (first) {
					showMessage(res.getString("first_version_produced") + ": " + pathToLatestSchedule, false);
				} else {
					showMessage(res.getString("new_version_produced") + ": " + pathToLatestSchedule, false);
				}
				message = null;
			}
			if (pathToLatestSchedule != null) {
				saveLastSchedulerInput(CopyFile.getDir(pathToLatestSchedule));
			}
			if (message == null)
				return;
			showMessage(message, false);
			if (finished && CopyFile.sameFiles(pathToCurrentSchedule, pathToLatestSchedule))
				return;
			Panel p = new Panel(new ColumnLayout());
			p.add(new Label(message));
			CheckboxGroup cbg = null;
			if (pathToLastScheduleOfRun != null && !CopyFile.sameFiles(pathToLastScheduleOfRun, pathToLatestSchedule)) {
				cbg = new CheckboxGroup();
			}
			Checkbox loadCB = new Checkbox(res.getString("Load_latest_schedule"), cbg == null, cbg);
			p.add(loadCB);
			Checkbox loadPrevCB = null;
			if (cbg != null) {
				loadPrevCB = new Checkbox(res.getString("load_final_schedule_prev_run"), true, cbg);
				p.add(loadPrevCB);
			}
			Checkbox stopCB = null;
			if (!finished) {
				stopCB = new Checkbox(res.getString("sch_stop"), true);
				p.add(stopCB);
			}
			OKDialog okDia = new OKDialog(core.getUI().getMainFrame(), res.getString("scheduler_status"), true);
			okDia.addContent(p);
			okDia.show();
			if (okDia.wasCancelled())
				return;
			if (stopCB != null && stopCB.getState()) {
				stopScheduler();
			}
			if (loadPrevCB != null && loadPrevCB.getState()) {
				loadSchedule(pathToLastScheduleOfRun, true);
			} else if (loadCB.getState()) {
				loadSchedule(pathToLatestSchedule, true);
			}
			return;
		}
	}

	protected void showMessage(String msg, boolean error) {
		if (core != null && core.getUI() != null) {
			core.getUI().showMessage(msg, error);
		}
		if (error) {
			System.out.println("!--> " + msg);
		}
	}

	protected MenuItem getMenuItemById(String id) {
		if (id == null)
			return null;
		for (MenuItem menuItem : menuItems)
			if (id.equals(menuItem.getActionCommand()))
				return menuItem;
		return null;
	}

	public void summaryViewClosed() {
		if (summaryViewFrame != null) {
			CManager.destroyComponent(summaryViewFrame.getComponent(0));
			summaryViewFrame = null;
			MenuItem mit = getMenuItemById("sch_summary");
			((CheckboxMenuItem) mit).setState(false);
		}
	}

	public void ganttViewClosed() {
		if (ganttViewFrame != null) {
			CManager.destroyComponent(ganttViewFrame.getComponent(0));
			ganttViewFrame = null;
			MenuItem mit = getMenuItemById("sch_gantt");
			((CheckboxMenuItem) mit).setState(false);
		}
	}

	public void matrixViewClosed() {
		if (matrixViewFrame != null) {
			CManager.destroyComponent(matrixViewFrame.getComponent(0));
			matrixViewFrame = null;
			MenuItem mit = getMenuItemById("sch_matrix");
			((CheckboxMenuItem) mit).setState(false);
			if (scheduleData != null && scheduleData.aggLinkLayer != null && (scheduleData.aggLinkLayer instanceof DAggregateLinkLayer)) {
				DAggregateLinkLayer aggL = (DAggregateLinkLayer) scheduleData.aggLinkLayer;
				aggL.setThicknessColN(-1, false);
			}
		}
	}

	public void windowClosed(WindowEvent e) {
		if (e.getSource().equals(summaryViewFrame)) {
			summaryViewClosed();
		} else if (e.getSource().equals(ganttViewFrame)) {
			ganttViewClosed();
		} else if (e.getSource().equals(matrixViewFrame)) {
			matrixViewClosed();
		}
	}

	public void windowOpened(WindowEvent e) {
	}

	public void windowClosing(WindowEvent e) {
	}

	public void windowIconified(WindowEvent e) {
	}

	public void windowDeiconified(WindowEvent e) {
	}

	public void windowActivated(WindowEvent e) {
	}

	public void windowDeactivated(WindowEvent e) {
	}

	/**
	 * Prepares input data for the transportation scheduler.
	 * Returns true if successful.
	 */
	protected void prepareInputForScheduler() {
		if (schInPreparer != null && schInPreparer.isRunning())
			return;
		if (schInPreparer == null) {
			findPaths();
			schInPreparer = new SchedulerInputPreparer();
			schInPreparer.setCore(core);
			schInPreparer.setOwner(this);
		}
		schInPreparer.setObjectsToSave(objToSave);
		schInPreparer.setObjectsPossiblyToSave(objPossiblyToSave);
		schInPreparer.setShelters(shelters);
		objToSave = null;
		objPossiblyToSave = null;
		shelters = null;
		schInPreparer.setPaths(metadataPath, staticDataPath, schedulerInputPath, schedulerOutputPath);
		if (itemCategories != null || itemCatCodes != null) {
			schInPreparer.setItemCategories(itemCatCodes, itemCategories);
		}
		if (schInPreparer.locLayer == null && locLayer != null) {
			schInPreparer.setLayerWithPotentialPlaces(locLayer);
		}
		if (distancesMatrix != null) {
			schInPreparer.setDistancesMatrix(distancesMatrix);
		}
		if (schInPreparer.itemsInSources == null && itemsInSources != null) {
			schInPreparer.setItemsInSources(itemsInSources);
		}
		if (schInPreparer.destCap == null && destCap != null) {
			schInPreparer.setDestinationCapacitiesTable(destCap);
		}
		if (schInPreparer.vehicleTypesInfo == null && vehicleTypesInfo != null) {
			schInPreparer.setVehicleTypesInfo(vehicleTypesInfo);
		}
		if (schInPreparer.vehiclesInSources == null && vehiclesInSources != null) {
			schInPreparer.setVehiclesInSources(vehiclesInSources);
		}
		schInPreparer.tryGetPreviousData(null);
		schInPreparer.prepareInputData();
	}

	/**
	 * Stops the work of the scheduler if the user quits the system
	 * while the scheduler is running
	 */
	public void stopWork() {
		stopScheduler();
	}
}
