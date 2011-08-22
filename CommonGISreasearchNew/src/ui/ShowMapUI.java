package ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Frame;
import java.awt.Panel;

import spade.analysis.plot.Plot;
import spade.analysis.system.DataKeeper;
import spade.analysis.system.Supervisor;
import spade.analysis.system.SystemUI;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.Destroyable;
import spade.lib.basicwin.NotificationLine;
import spade.vis.map.MapViewer;
import spade.vis.space.LayerManager;
import data_load.DataManager;

/**
* Conceived as a UI of a very small CommonGIS subset that only loads a map
* description and then constructs and displays the corresponding map.
*/
public class ShowMapUI extends Panel implements SystemUI, Destroyable {
	/**
	* Indicates whether the system runs as an applet or as a local system.
	*/
	protected boolean isApplet = false;
	/**
	* System's supervisor, which provides an access to the system's parameters
	*/
	protected Supervisor supervisor = null;
	/**
	* Loads and keeps all data
	*/
	protected DataManager dataLoader = null;

	protected SimpleMapView mapView = null;
	protected NotificationLine lStatus = null;
	/**
	* Indicates whether a new map window must be used for each new visualisation.
	* If not, a new visualisation must be applied to the main map view.
	* The default value is false.
	*/
	protected boolean useNewMapForNewVis = false;

	protected boolean destroyed = false;

	public ShowMapUI() {
		setLayout(new BorderLayout());
		lStatus = new NotificationLine("Iris, Descartes, CommonGIS, V-Analytics 1995-2010");
		add(lStatus, "South");
	}

	public void startWork(Supervisor supervisor, String configFileName) {
		this.supervisor = supervisor;
		isApplet = !supervisor.getSystemSettings().checkParameterValue("isLocalSystem", "true");

		if (configFileName != null) {
			//load the application
			dataLoader = new DataManager();
			dataLoader.setSupervisor(supervisor);
			dataLoader.setUI(this);
			if (dataLoader.loadApplication(configFileName, supervisor.getSystemSettings().getParameterAsString("DATA_SERVER"))) {
				System.out.println("Loaded application from " + configFileName);
				openMapView(0);
			}
		}
	}

	public DataKeeper getDataKeeper() {
		return dataLoader;
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
		return 0;
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
		System.out.println("Open map view, mapN=" + mapN);
		if (mapView != null) {
			bringMapToTop(mapView);
			return;
		}
		LayerManager lman = dataLoader.getMap(mapN);
		System.out.println("lman=" + lman);
		if (lman == null)
			return;
		mapView = new SimpleMapView(supervisor, lman);
		mapView.setIsPrimary(true);
		add(mapView, "Center");
		if (isShowing()) {
			CManager.validateAll(lStatus);
		}
		mapView.setup();
		bringMapToTop(mapView);
		String applName = supervisor.getSystemSettings().getParameterAsString("APPL_NAME");
		if (applName != null) {
			if (getMainFrame() != null) {
				getMainFrame().setTitle(applName);
			}
			lStatus.setDefaultMessage("Iris, Descartes, CommonGIS, V-Analytics 1995-2010: " + applName);
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
			if (mView instanceof SimpleMapView) {
				((SimpleMapView) mView).catchFocus();
			}
		}
	}

	/**
	* A method from the SystemUI interface.
	* Returns the MapViewer displaying the map with the specified number
	*/
	@Override
	public MapViewer getMapViewer(int mapN) {
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
		return mapView;
	}

	/**
	 * Asks the system UI to mark the specified absolute spatial (geographic) position
	 * on all currently open maps.
	 */
	@Override
	public void markPositionOnMaps(float posX, float posY) {
		if (mapView != null) {
			mapView.markPosition(posX, posY);
		}
		if (supervisor != null) {
			supervisor.notifyPositionSelection(posX, posY);
		}
	}

	/**
	 * Asks the system UI to erase the marks of the previously specified absolute spatial
	 * (geographic) position, if any, on all currently open maps.
	 */
	@Override
	public void erasePositionMarksOnMaps() {
		if (mapView != null) {
			mapView.erasePositionMark();
		}
		if (supervisor != null) {
			supervisor.notifyPositionSelection(Float.NaN, Float.NaN);
		}
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
			if (supervisor != null && supervisor.getSaveableToolCount() > 0) {
				for (int i = 0; i < supervisor.getSaveableToolCount(); i++)
					if (supervisor.getSaveableTool(i) instanceof MapViewer) {
						MapViewer mw = (MapViewer) supervisor.getSaveableTool(i);
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
		Frame frame = new Frame(mwin.getName());
		frame.setLayout(new BorderLayout());
		frame.add(mwin, BorderLayout.CENTER);
		Frame mf = getMainFrame();
		if (mf != null && mf.getSize() != null) {
			frame.setSize(mf.getSize());
		} else {
			frame.setSize(600, 400);
		}
		CManager.validateFully(frame);
		if (supervisor != null && supervisor.getWindowManager() != null) {
			supervisor.getWindowManager().registerWindow(frame);
		}
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
		if (supervisor != null && supervisor.getSaveableToolCount() > 0) {
			for (int i = 0; i < supervisor.getSaveableToolCount(); i++)
				if (supervisor.getSaveableTool(i) instanceof MapViewer) {
					MapViewer mw = (MapViewer) supervisor.getSaveableTool(i);
					if (id.equalsIgnoreCase(mw.getIdentifier()))
						return mw;
				}
		}
		return null;
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
		if (supervisor != null && supervisor.getSaveableToolCount() > 0) {
			for (int i = 0; i < supervisor.getSaveableToolCount(); i++)
				if (supervisor.getSaveableTool(i) instanceof Plot) {
					Plot pl = (Plot) supervisor.getSaveableTool(i);
					if (id.equalsIgnoreCase(pl.getIdentifier()))
						return pl;
				}
		}
		return null;
	}

//~ID

	/**
	* Closes the MapViewer displaying the map with the specified number
	* (e.g. when the map is removed from the system)
	*/
	@Override
	public void closeMapView(int mapN) {
		if (mapView == null)
			return;
		CManager.destroyComponent(mapView);
		remove(mapView);
		mapView = null;
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
		//close all windows
		//...
		CManager.destroyComponent(this);
		if (!isApplet && !supervisor.getSystemSettings().checkParameterValue("runsInsideOtherSystem", "true")) {
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