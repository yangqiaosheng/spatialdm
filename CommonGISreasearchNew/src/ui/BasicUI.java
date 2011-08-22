package ui;

import help.DescartesHelpIndex;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Frame;
import java.awt.Panel;

import spade.analysis.plot.Plot;
import spade.analysis.system.DataLoader;
import spade.analysis.system.ESDACore;
import spade.analysis.system.MapToolbar;
import spade.analysis.system.Supervisor;
import spade.analysis.system.SystemUI;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.Destroyable;
import spade.lib.basicwin.Metrics;
import spade.lib.basicwin.NotificationLine;
import spade.lib.help.Helper;
import spade.vis.map.MapViewer;
import spade.vis.space.LayerManager;
import data_load.DataManager;

public class BasicUI extends Panel implements SystemUI, Destroyable {
	/**
	* The core provides references to the supervisor, display producer, data
	* keeper etc.
	*/
	protected ESDACore core = null;
	/**
	* Indicates whether the system runs as an applet or as a local system.
	*/
	protected boolean isApplet = false;

	protected AdvancedMapView mapView = null;
	protected NotificationLine lStatus = null;
	/**
	* A configuration file may contain several map specifications. However,
	* this variant of UI allows only one map at a time to be visible.
	* The variable currMapN contains the index of the shown map in the list
	* of all available maps.
	*/
	protected int currMapN = -1;
	/**
	* Indicates whether a new map window must be used for each new visualisation.
	* If not, a new visualisation must be applied to the main map view.
	* The default value is true.
	*/
	protected boolean useNewMapForNewVis = true;

	protected boolean destroyed = false;

	public BasicUI() {
		setLayout(new BorderLayout());
		lStatus = new NotificationLine("Iris, Descartes, CommonGIS, V-Analytics 1995-2010");
		add(lStatus, "South");
	}

	public void startWork(ESDACore core, String configFileName) {
		this.core = core;
		core.setUI(this);
		isApplet = !core.getSystemSettings().checkParameterValue("isLocalSystem", "true");
		Helper.setHelpIndex(new DescartesHelpIndex());
		if (Metrics.getFontMetrics() == null) {
			Metrics.setFontMetrics(getGraphics());
		}

		if (configFileName != null) {
			DataLoader dataLoader = core.getDataLoader();
			if (dataLoader.loadApplication(configFileName, core.getSystemSettings().getParameterAsString("DATA_SERVER"))) {
				String applName = core.getSystemSettings().getParameterAsString("APPL_NAME");
				if (applName != null) {
					if (getMainFrame() != null) {
						getMainFrame().setTitle(applName);
					}
					lStatus.setDefaultMessage("Iris, Descartes, CommonGIS, V-Analytics 1995-2010: " + applName);
				}
			}
		}
//ID
		core.getSystemSettings().setParameter("SYSTEM_CORE", core);
//~ID
	}

	public void startWork(ESDACore core) {
		startWork(core, null);
	}

	/**
	* Automatically starts the tools specified in the system's parameter
	* "AUTOSTART_TOOL" (this parameter is updated when a new project is loaded)
	*/
	public void autostartTools() {
	}

	/**
	* Brings the "legend" tab in the map window to the front
	*/
	public void legendToFront() {
		if (mapView != null) {
			mapView.legendToFront();
			bringMapToTop(mapView);
		}
	}

	public ESDACore getCore() {
		return core;
	}

	@Override
	public NotificationLine getStatusLine() {
		return lStatus;
	}

	/**
	* Shows the given message. If "trouble" is true, then this
	* is an error message.
	*/
	@Override
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
	@Override
	public void showMessage(String msg) {
		showMessage(msg, false);
	}

	/**
	* Clears the status line
	*/
	@Override
	public void clearStatusLine() {
		if (lStatus != null) {
			lStatus.showMessage(null, false);
		}
	}

	/**
	* Notifies about the status of some process. If "trouble" is true, then this
	* is an error message.
	*/
	@Override
	public void notifyProcessState(String processName, String processState, boolean trouble) {
		showMessage(processName + ": " + processState, trouble);
	}

	/**
	* Returns the index of the current map (if multiple maps are possible)
	*/
	@Override
	public int getCurrentMapN() {
		return currMapN;
	}

	/**
	* Informs whether a new map window must be used for each new visualisation.
	* If not, a new visualisation must be applied to the main map view.
	*/
	@Override
	public boolean getUseNewMapForNewVis() {
		return useNewMapForNewVis;
	}

	/**
	* Sets whether a new map window must be used for each new visualisation.
	* If not, a new visualisation must be applied to the main map view.
	*/
	@Override
	public void setUseNewMapForNewVis(boolean value) {
		useNewMapForNewVis = value;
	}

	/**
	* Creates a MapViewer to display the map with the specified number
	*/
	@Override
	public void openMapView(int mapN) {
		if (mapView != null) {
			CManager.destroyComponent(mapView);
			this.remove(mapView);
		}
		currMapN = mapN;
		LayerManager lman = core.getDataKeeper().getMap(currMapN);
		if (lman == null)
			return;
		mapView = new AdvancedMapView(core.getSupervisor(), lman);
		mapView.setIsPrimary(true);
		add(mapView, "Center");
		if (isShowing()) {
			CManager.validateAll(lStatus);
		}
		mapView.setup();
		bringMapToTop(mapView);
	}

	/**
	* A method from the SystemUI interface.
	*/
	@Override
	public Frame getMainFrame() {
		return CManager.getFrame(this);
	}

	/**
	* A method from the SystemUI interface.
	* Brings current (main) map view to top. In this configuration the map
	* view is in a separate frame (mapWin).
	*/
	@Override
	public void bringMapToTop(MapViewer mView) {
		if (mView instanceof Component) {
			Frame fr = CManager.getFrame((Component) mView);
			if (fr != null) {
				fr.toFront();
			}
			if (mView instanceof AdvancedMapView) {
				((AdvancedMapView) mView).catchFocus();
			}
		}
	}

	/**
	* A method from the SystemUI interface.
	* Returns the MapViewer displaying the map with the specified number
	*/
	@Override
	public MapViewer getMapViewer(int mapN) {
		if (mapN != currMapN)
			return null;
		return mapView;
	}

	/**
	* A method from the SystemUI interface.
	* Returns the current MapViewer (if multiple map views are possible) or the
	* only map viewer available
	*/
	@Override
	public MapViewer getCurrentMapViewer() {
		return mapView;
	}

	/**
	* A method from the SystemUI interface.
	* Returns the latest created MapViewer (if multiple map views are possible) or the
	* only map viewer available
	*/
	@Override
	public MapViewer getLatestMapViewer() {
		Supervisor sup = core.getSupervisor();
		if (sup != null && sup.getSaveableToolCount() > 0) {
			for (int i = sup.getSaveableToolCount() - 1; i >= 0; i--)
				if (sup.getSaveableTool(i) instanceof MapViewer)
					return (MapViewer) sup.getSaveableTool(i);
		}
		return mapView;
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
		MapViewer mw1 = null;
		if (id.equalsIgnoreCase("_blank_"))
			if (mapView == null)
				return null;
			else {
				mw1 = mapView.makeClearCopy();
			}
		if (mw1 == null) {
			Supervisor sup = core.getSupervisor();
			if (sup != null && sup.getSaveableToolCount() > 0) {
				for (int i = 0; i < sup.getSaveableToolCount(); i++)
					if (sup.getSaveableTool(i) instanceof MapViewer) {
						MapViewer mw = (MapViewer) sup.getSaveableTool(i);
						if (id.equalsIgnoreCase(mw.getIdentifier()))
							return mw;
					}
			}
		}
		if (mw1 == null) {
			if (mapView == null)
				return null;
			mw1 = mapView.makeClearCopy();
			if (mw1 == null)
				return null;
			mw1.setIdentifier(id);
		}
		Component mwin = (Component) mw1;
		int w = 600, h = 400;
		Frame mf = getMainFrame();
		if (mf != null && mf.getSize() != null) {
			w = mf.getSize().width;
			h = mf.getSize().height;
		}
		Frame frame = core.getDisplayProducer().makeWindow(mwin, mwin.getName(), w, h);
		return mw1;
	}

	/**
	* Tries to find the map viewer with the given identifier. If not found,
	* returns null rather than tries to construct one. If the identifier is
	* null or "main", returns the primary map viewer (e.g. the one included
	* in the main window of the system).
	*/
	@Override
	public MapViewer findMapViewer(String id) {
		if (id == null || id.equalsIgnoreCase("main"))
			return mapView;
		Supervisor sup = core.getSupervisor();
		if (sup != null && sup.getSaveableToolCount() > 0) {
			for (int i = 0; i < sup.getSaveableToolCount(); i++)
				if (sup.getSaveableTool(i) instanceof MapViewer) {
					MapViewer mw = (MapViewer) sup.getSaveableTool(i);
					if (id.equalsIgnoreCase(mw.getIdentifier()))
						return mw;
				}
		}
		return null;
	}

	/**
	 * Asks the system UI to mark the specified absolute spatial (geographic) position
	 * on all currently open maps.
	 */
	@Override
	public void markPositionOnMaps(float posX, float posY) {
		Supervisor sup = core.getSupervisor();
		if (sup != null) {
			sup.notifyPositionSelection(posX, posY);
			if (sup.getSaveableToolCount() > 0) {
				for (int i = 0; i < sup.getSaveableToolCount(); i++)
					if (sup.getSaveableTool(i) instanceof SimpleMapView) {
						SimpleMapView mw = (SimpleMapView) sup.getSaveableTool(i);
						mw.markPosition(posX, posY);
					}
			}
		}
	}

	/**
	 * Asks the system UI to erase the marks of the previously specified absolute spatial
	 * (geographic) position, if any, on all currently open maps.
	 */
	@Override
	public void erasePositionMarksOnMaps() {
		Supervisor sup = core.getSupervisor();
		if (sup != null) {
			sup.notifyPositionSelection(Float.NaN, Float.NaN);
			if (sup.getSaveableToolCount() > 0) {
				for (int i = 0; i < sup.getSaveableToolCount(); i++)
					if (sup.getSaveableTool(i) instanceof SimpleMapView) {
						SimpleMapView mw = (SimpleMapView) sup.getSaveableTool(i);
						mw.erasePositionMark();
					}
			}
		}
	}

//ID
	/**
	* Tries to find the plot with the given identifier. If not found,
	* returns null rather than tries to construct one.
	*/
	@Override
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

//~ID

	/**
	* Returns the map toolbar
	*/
	public MapToolbar getMapToolbar() {
		return (mapView != null ? mapView.getMapToolbar() : null);
	}

	/**
	* Closes the MapViewer displaying the map with the specified number
	* (e.g. when the map is removed from the system)
	*/
	@Override
	public void closeMapView(int mapN) {
		if (mapN != currMapN || mapView == null)
			return;
		CManager.destroyComponent(mapView);
		remove(mapView);
	}

	/**
	* A method from the SystemUI interface.
	* Places the given component somewhere in the UI (e.g. creates a separate frame
	* for it, or puts an a subwindow etc.) May analyse the type of the component.
	*/
	@Override
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
	@Override
	public void removeComponent(Component c) {
		if (c == null || mapView == null)
			return;
		mapView.removeTab(c.getName());
	}

	public void quit() {
		Frame fr = getMainFrame();
		if (fr != null && (fr instanceof MainWin)) {
			fr.dispose();
		} else {
			destroy();
		}
	}

	@Override
	public void destroy() {
		if (destroyed)
			return;
		destroyed = true;
		core.getSupervisor().getWindowManager().closeAllWindows();
		CManager.destroyComponent(this);
		if (core.getDataKeeper() instanceof DataManager) {
			DataManager dataLoader = (DataManager) core.getDataLoader();
			dataLoader.storeSemantics();
		}
		if (!isApplet && !core.getSystemSettings().checkParameterValue("runsInsideOtherSystem", "true")) {
			System.exit(0);
		}
	}

	@Override
	public boolean isDestroyed() {
		return destroyed;
	}

	/**
	* Returns null
	*/
	@Override
	public Object getTimeUI() {
		return null;
	}
}
