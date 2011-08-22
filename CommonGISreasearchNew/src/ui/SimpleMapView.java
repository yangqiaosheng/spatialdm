package ui;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.LayoutManager;
import java.awt.Panel;
import java.awt.Rectangle;
import java.awt.ScrollPane;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Hashtable;
import java.util.ResourceBundle;
import java.util.Vector;

import observer.annotation.AnnotationSurfaceInterface;
import observer.annotation.Markable;
import spade.analysis.manipulation.ObjectEventReactor;
import spade.analysis.system.MapToolbar;
import spade.analysis.system.Supervisor;
import spade.lib.basicwin.ActionCanvas;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.Destroyable;
import spade.lib.basicwin.Metrics;
import spade.lib.basicwin.PopupManager;
import spade.lib.basicwin.SplitLayout;
import spade.lib.basicwin.TImgButton;
import spade.lib.basicwin.TabbedPanel;
import spade.lib.basicwin.TriangleDrawer;
import spade.lib.help.Helper;
import spade.lib.lang.Language;
import spade.vis.action.ObjectEvent;
import spade.vis.action.ObjectEventHandler;
import spade.vis.database.DataTreater;
import spade.vis.dataview.DataViewInformer;
import spade.vis.dataview.DataViewRegulator;
import spade.vis.dataview.TransformedDataPresenter;
import spade.vis.dmap.DLayerManager;
import spade.vis.dmap.DOSMLayer;
import spade.vis.dmap.MapCanvas;
import spade.vis.event.DMouseEvent;
import spade.vis.event.EventMeaningManager;
import spade.vis.event.EventSource;
import spade.vis.geometry.RealRectangle;
import spade.vis.map.LegendCanvas;
import spade.vis.map.MapContext;
import spade.vis.map.MapDraw;
import spade.vis.map.MapViewer;
import spade.vis.map.Mappable;
import spade.vis.mapvis.Visualizer;
import spade.vis.space.GeoLayer;
import spade.vis.space.LayerManager;
import spade.vis.space.ObjectManager;
import spade.vis.spec.MapWindowSpec;
import spade.vis.spec.SaveableTool;
import core.InstanceCounts;

/**
* A panel containing a map canvas, a canvas with the corresponding legend, and
* one or more manipulators of data visualisation.
*/
public class SimpleMapView extends Panel implements Destroyable, MapViewer, ActionListener, PropertyChangeListener, DataViewInformer, ObjectEventHandler, EventSource, SaveableTool, Markable {
	static ResourceBundle res = Language.getTextResource("ui.Res");
	/**
	* Used to generate unique identifiers of instances of MapView
	*/
	protected int instanceN = 0;
	/**
	* The unique identifier of this map view. The identifier is used
	* 1) for explicit linking of producers and recipients of object events;
	* 2) for correct restoring of system states with multiple map windows.
	*/
	protected String mapViewId = null;

	protected TabbedPanel toolP = null;
	protected EventMeaningManager evtMeanMan = new EventMeaningManager();
	protected LayerManager lman = null;
	protected ObjectManager objMan = null;

	protected MapCanvas map = null;
	protected Component legendView = null;
	protected Vector sAttr = null;

	protected ActionCanvas ac[] = null;
	protected SimpleMapToolBar toolbar = null;
	protected ManipulatorHandler manHandler = null;

	protected TImgButton bZoomToImageResolution = null;
	public static String cmdZoomToImageResolution = "ZoomToImageResolution";

	protected int bDir[] = { TriangleDrawer.NW, TriangleDrawer.N, TriangleDrawer.NE, TriangleDrawer.E, TriangleDrawer.SE, TriangleDrawer.S, TriangleDrawer.SW, TriangleDrawer.W };
	protected String bCmd[] = { "NW", "N", "NE", "E", "SE", "S", "SW", "W" }, bComment[] = { res.getString("North_West"), res.getString("North"), res.getString("North_East"), res.getString("East"), res.getString("South_East"),
			res.getString("South"), res.getString("South_West"), res.getString("West") };
	/**
	* The supervisor links together multiple data displays
	*/
	protected Supervisor supervisor = null;
	/**
	* Indicates "destroyed" state. Initially is false.
	*/
	protected boolean destroyed = false;
	/**
	* Indicates whether this map view is included in the main window.
	*/
	protected boolean isPrimary = false;

	/**
	* Returns the component drawing the map
	*/
	public MapDraw getMapDrawer() {
		return map;
	}

	/**
	* Returns the map toolbar
	*/
	public MapToolbar getMapToolbar() {
		return toolbar;
	}

	/**
	* Returns the manager of interpretations of mouse events occurring in the
	* map area
	*/
	public EventMeaningManager getMapEventMeaningManager() {
		return evtMeanMan;
	}

	/*
	* Relative part of manipulator in split
	*/
	float manipulatorSize = 0.3f;

	protected Component makeToolBar() {
		boolean allowPrint = false, allowSave = false;
		allowSave = isSaveAvailable();
		if (supervisor != null && supervisor.getSystemSettings() != null) {
			allowPrint = supervisor.getSystemSettings().checkParameterValue("isLocalSystem", "true");
		}
		return new SimpleMapToolBar(map, evtMeanMan, allowSave, allowPrint, this);
	}

	public SimpleMapView(Supervisor sup, LayerManager lman) {
		this.lman = lman;
		this.supervisor = sup;
		instanceN = InstanceCounts.incMapViewInstanceCount();
		String id = "Map_" + instanceN;
		if (supervisor != null && supervisor.getUI() != null) {
			while (supervisor.getUI().findMapViewer(id) != null) {
				instanceN = InstanceCounts.incMapViewInstanceCount();
				id = "Map_" + instanceN;
			}
		}
		setIdentifier(id);
		setName("Map N " + instanceN);
//ID
		supervisor.registerTool(this);
//~ID
	}

	/**
	* Sets the indicator of this map view being the main map view in the system
	* (i.e. included in the main window).
	*/
	public void setIsPrimary(boolean value) {
		isPrimary = value;
	}

	/**
	* Replies whether this map view is the main map view in the system
	* (i.e. included in the main window).
	*/
	public boolean getIsPrimary() {
		return isPrimary;
	}

	public void setup() {
		if (lman == null || !(lman instanceof Mappable))
			return;
		if (lman instanceof DLayerManager) {
			((DLayerManager) lman).addPropertyChangeListener(this, true, true, true);
		} else {
			((Mappable) lman).addPropertyChangeListener(this, true, true);
		}

		for (int i = 0; i < DMouseEvent.events.length; i++) {
			evtMeanMan.addEvent(DMouseEvent.events[i], DMouseEvent.eventFullTexts[i]);
		}

//ID
		map = new MapCanvas() {
			protected void draw(Graphics g) {
				super.draw(g);
				if (annotationSurfacePresent()) {
					getAnnotationSurface().paint(g);
				}
			}

			public void restorePicture() {
				if (offScreenImage != null) {
					Graphics g = getGraphics();
					if (g != null) {
						g.drawImage(offScreenImage, 0, 0, null);
						if (annotationSurfacePresent()) {
							getAnnotationSurface().paint(g);
						}
						g.dispose();
						notifyPropertyChange("MapPainting", null, null);
					}
				}
			}

			public void update(Graphics g) {
				paint(g);
			}
		};
		if (annotationSurfacePresent()) {
			map.addMouseListener(getAnnotationSurface().addMouseRedirector(map));
			map.addMouseMotionListener(getAnnotationSurface().addMouseMotionRedirector(map));
		} else {
			map.addMouseListener(map);
			map.addMouseMotionListener(map);
		}
//~ID
		map.setMapContent((Mappable) lman);
		map.setEventMeaningManager(evtMeanMan);
		map.addPropertyChangeListener(this);
		// P.G.: setting background color for map canvas
		if (supervisor != null) {
			spade.lib.util.Parameters sysParam = supervisor.getSystemSettings();
			if (sysParam != null) {
				Object objAppBgColor = supervisor.getSystemSettings().getParameter("APPL_BGCOLOR");
				if (objAppBgColor != null) {
					map.setBackground((Color) objAppBgColor);
				} else {
					System.out.println("Cannon find parameter APPL_BGCOLOR!");
				}
				Object objDenom = supervisor.getSystemSettings().getParameter("MIN_SCALE_DENOMINATOR");

				if (objDenom != null) {
					map.setMinPixelValue(((Float) objDenom).floatValue() / (Metrics.cm() * ((spade.vis.dmap.DLayerManager) lman).user_factor));
				}
			} else {
				System.out.println("System properties not found!");
			}
		} else {
			System.out.println("Supervisor not found!");
		}
		// ~P.G.
		objMan = new ObjectManager(supervisor);
		objMan.setMap(map);
		objMan.setObjectEventHandler(this); //object events will be sent to this MapView
		lman.setObjectManager(objMan);
		if (supervisor != null) {
			supervisor.registerObjectEventSource(this);
		}

		Component mtb = makeToolBar();
		toolbar = (SimpleMapToolBar) mtb;

		Panel aroundMapPanel = new Panel();
		aroundMapPanel.setLayout(new BorderLayout());
		Panel subP = new Panel();
		subP.setLayout(new BorderLayout());
		ac = new ActionCanvas[bCmd.length];
		for (int i = 0; i < ac.length; i++) {
			TriangleDrawer td = new TriangleDrawer(bDir[i]);
			td.setPreferredSize(8, 8);
			ac[i] = new ActionCanvas(td);
			ac[i].addActionListener(this);
			ac[i].setActionCommand(bCmd[i]);
			new PopupManager(ac[i], res.getString("Move_to_") + bComment[i], true);
		}

		subP.add(ac[0], "West");
		subP.add(ac[1], "Center");
		subP.add(ac[2], "East");
		aroundMapPanel.add(subP, "North");
		aroundMapPanel.add(ac[7], "West");
		aroundMapPanel.add(map, "Center");
		aroundMapPanel.add(ac[3], "East");
		subP = new Panel();
		subP.setLayout(new BorderLayout());
		subP.add(ac[6], "West");
		subP.add(ac[5], "Center");
		subP.add(ac[4], "East");
		aroundMapPanel.add(subP, "South");

		Panel mp = new Panel();
		mp.setLayout(new BorderLayout());
		mp.add(aroundMapPanel, "Center");
		mp.add(mtb, "North");

		sAttr = new Vector(2, 2);
		sAttr.addElement("Id");
		sAttr.addElement("Name");

		LegendCanvas legend = new LegendCanvas();
		map.setLegend(legend);

//ID
		SplitLayout spl = new SplitLayout(this, SplitLayout.VERT);
		setLayout(spl);

		float legendSize;
		try {
			legendSize = (float) (((spade.vis.dmap.DLayerManager) lman).percent_of_legend) / 100;
			manipulatorSize = (float) (((spade.vis.dmap.DLayerManager) lman).percent_of_manipulator) / 100;
		} catch (Exception ex) {
			System.out.println("Cannot aquire preferred size of a legend");
			legendSize = 0.3f;
			manipulatorSize = 0.3f;
		}
		if (legendSize < 0.0f) {
			legendSize = 0.0f;
		}
		if (legendSize > 1.0f) {
			legendSize = 1.0f;
		}
		if (manipulatorSize < 0.0f) {
			manipulatorSize = 0.0f;
		}
		if (manipulatorSize > 1.0f) {
			manipulatorSize = 1.0f;
		}

//      spl.addComponent(mp,1.0f - legendSize);

		ScrollPane scroll = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
		scroll.add(legend);
		scroll.addComponentListener(legend);
		Panel p = new Panel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
		Button b = new Button("?");
		b.setActionCommand("help_legend");
		b.addActionListener(this);
		p.add(b);
		Panel lp = new Panel(new BorderLayout());
		lp.add(p, "North");
		lp.add(scroll, "Center");
		legendView = lp;
		// legend is located left to the map from now (if not changed by user):
		spl.addComponent(legendView, legendSize);
		spl.addComponent(mp, 1.0f - legendSize);

		try {
			int whereIsLegend = -1, whereIsMap = -1;
			whereIsLegend = spl.getComponentIndex(legendView);
			whereIsMap = spl.getComponentIndex(mp);
			if (!((spade.vis.dmap.DLayerManager) lman).show_legend) {
				if (whereIsMap > -1) {
					spl.changePart(whereIsMap, 1.0f);
				}
				if (whereIsLegend > -1) {
					spl.changePart(whereIsLegend, 0.0f);
				}
			} else {
				if (whereIsMap > -1) {
					spl.changePart(whereIsMap, 1.0f - legendSize);
				}
				if (whereIsLegend > -1) {
					spl.changePart(whereIsLegend, legendSize);
				}
			}
		} catch (Exception ex) {
			System.out.println("Cannot change visibility status of a legend");
		}
//~ID
		DOSMLayer bkgOSMLayer = getBkgImageLayer();
		if (bkgOSMLayer != null) {
			enableZoomToImageResolution();
		}
		setButtonStatus();
		CManager.validateAll(this);
	}

	public DOSMLayer getBkgImageLayer() {
		if (lman == null)
			return null;
		if (!(lman instanceof DLayerManager))
			return null;
		DLayerManager lm = (DLayerManager) lman;
		DOSMLayer bkgOSMLayer = lm.getOSMLayer();
		if (bkgOSMLayer != null)
			return bkgOSMLayer;
		bkgOSMLayer = lm.getGMLayer();
		return bkgOSMLayer;
	}

	/**
	* Returns the currently visible territory extent in the map.
	* The extent is returned as an array of 4 floats:
	* 0) x1; 1) y1; 2) x2; 3) y2
	*/
	public float[] getMapExtent() {
		if (map == null)
			return null;
		MapContext mc = map.getMapContext();
		if (mc == null)
			return null;
		RealRectangle rr = mc.getVisibleTerritory();
		if (rr == null)
			return null;
		float ext[] = new float[4];
		ext[0] = rr.rx1;
		ext[1] = rr.ry1;
		ext[2] = rr.rx2;
		ext[3] = rr.ry2;
		return ext;
	}

	protected void forceLayout() {
		try {
			LayoutManager layout = getLayout();
			if (layout != null && (layout instanceof SplitLayout)) {
				SplitLayout spl = (SplitLayout) layout;
				float p1 = spl.getComponentPart(0) + 0.01f, p2 = spl.getComponentPart(1) - 0.01f;
				spl.changePart(0, p1);
				spl.changePart(1, p2);
				invalidate();
				CManager.validateAll(this);
				spl.changePart(0, p1 - 0.01f);
				spl.changePart(1, p2 + 0.01f);
				invalidate();
				CManager.validateAll(this);
			} else {
				CManager.validateAll((toolP != null) ? toolP : legendView);
			}
		} catch (Throwable thr) {
		}
	}

	public void addTab(Component c, String name) {
		if (c == null)
			return;
		if (Metrics.getFontMetrics() == null || Metrics.fh < 1) {
			Graphics g = getGraphics();
			if (g != null) {
				Metrics.setFontMetrics(g.getFontMetrics());
				g.dispose();
			}
		}
		if (toolP == null) {
			toolP = new TabbedPanel();
		}
		toolP.setTabsAtTheBottom(true);
		SplitLayout spl = null;
		int legendIdx = -1;
		LayoutManager layout = getLayout();

		if (layout != null && (layout instanceof SplitLayout)) {
			spl = (SplitLayout) layout;
		}
		if (!isAncestorOf(toolP)) { // there is no tabbed panel created yet
			if (legendView != null) {
				/*
				// it was before: remove legend and replace its location with tabs
				if (spl!=null) {
				  legendIdx=spl.getComponentIndex(legendView);
				  if (legendIdx>-1) spl.replaceComponent(toolP,legendIdx);
				} else {
				  remove(legendView);
				  add(toolP);
				}
				toolP.addComponent(res.getString("Legend"),legendView);
				*/
			}
			if (spl != null) {
				if (legendView != null) {
					legendIdx = spl.getComponentIndex(legendView);
				}
				float part = manipulatorSize;
				//if (legendIdx>-1) part=1.0f-2*spl.getComponentPart(legendIdx);
				spl.addComponent(toolP, part);

				try {
					int whereIsManipulator = -1;
					whereIsManipulator = spl.getComponentIndex(toolP);
					if (!((spade.vis.dmap.DLayerManager) lman).show_manipulator) {
						if (whereIsManipulator > -1) {
							spl.changePart(whereIsManipulator, 0.0f);
						}
					} else {
						if (whereIsManipulator > -1) {
							spl.changePart(whereIsManipulator, manipulatorSize);
						}
					}
				} catch (Exception ex) {
					System.out.println("Cannot change visibility status of a manipulator");
				}
			}
		}
		// if we have to add manipulator...
		toolP.addComponent(name, c);
		toolP.makeLayout();
		toolP.showTab(name);
		forceLayout();
	}

	public void removeTab(String name) {
		if (toolP == null || name == null)
			return;
		toolP.removeComponent(name);
		if (toolP.getTabCount() < 1) {
			if (getLayout() != null && (getLayout() instanceof SplitLayout)) {
				int toolPIdx = -1;
				SplitLayout spl = null;

				spl = (SplitLayout) getLayout();
				toolPIdx = spl.getComponentIndex(toolP);
				toolP.removeAllComponents();
				if (toolPIdx > -1) {
					spl.removeComponent(toolPIdx);
				}
			} else {
				remove(toolP);
				toolP.removeAllComponents();
				add(legendView);
			}
		} else {
			toolP.showTab(0);
		}
		if (isShowing()) {
			CManager.validateAll(legendView);
		}
	}

	public void removeDestroyedTabs() {
		if (toolP == null || toolP.getTabCount() < 1)
			return;
		for (int i = toolP.getTabCount() - 1; i >= 0; i--)
			if (toolP.getTabContent(i) instanceof Destroyable) {
				Destroyable dc = (Destroyable) toolP.getTabContent(i);
				if (dc.isDestroyed()) {
					toolP.removeComponentAt(i);
				}
			}
		if (toolP.getTabCount() < 1) {
			if (getLayout() != null && (getLayout() instanceof SplitLayout)) {
				int toolPIdx = -1;
				SplitLayout spl = null;

				spl = (SplitLayout) getLayout();
				toolPIdx = spl.getComponentIndex(toolP);
				toolP.removeAllComponents();
				if (toolPIdx > -1) {
					spl.removeComponent(toolPIdx);
				}
			} else {
				remove(toolP);
				toolP.removeAllComponents();
				add(legendView);
			}
		} else {
			toolP.showTab(0);
		}
		if (isShowing()) {
			CManager.validateAll(legendView);
		}
	}

	public void legendToFront() {
		if (toolP == null)
			return;
		toolP.showTab(0);
		if (isShowing()) {
			CManager.validateAll(legendView);
		}
	}

	/**
	* Checks if all the components of the MapView are valid; removes invalid
	* components (e.g. after a table has been removed the map manipulator
	* working with its data must be also removed)
	*/
	public void validateView() {
		removeDestroyedTabs();
	}

	public Component getTabContent(String name) {
		if (toolP == null || name == null)
			return null;
		return toolP.getComponent(name);
	}

	/**
	* Used for moving all data visualizations from this map view to another map
	* view. This map view becomes "clear", i.e. without representing any thematic
	* data but only geographic data.
	*/
	public MapViewer makeCopyAndClear() {
		if (lman == null || lman.getLayerCount() < 1)
			return null;
		RealRectangle rr = null;
		MapContext mc = map.getMapContext();
		if (mc != null) {
			rr = mc.getVisibleTerritory();
		}
		LayerManager lmCopy = lman.makeCopy();
		SimpleMapView mw = new SimpleMapView(supervisor, lmCopy);
		mw.setup();
		moveManipulators(mw);
		for (int i = 0; i < lman.getLayerCount(); i++) {
			lman.getGeoLayer(i).setVisualizer(null);
			lman.getGeoLayer(i).setBackgroundVisualizer(null);
		}
		for (int i = 0; i < lmCopy.getLayerCount(); i++) {
			Visualizer vis = lmCopy.getGeoLayer(i).getVisualizer();
			if (vis != null) {
				vis.setLocation(mw.getIdentifier());
			}
			vis = lmCopy.getGeoLayer(i).getBackgroundVisualizer();
			if (vis != null) {
				vis.setLocation(mw.getIdentifier());
			}
		}
		mw.setIsPrimary(false);
		if (rr != null) {
			mw.showTerrExtent(rr.rx1, rr.ry1, rr.rx2, rr.ry2);
		}
		return mw;
	}

	/**
	* Copies all geographical data to another map view and returns this map view.
	* Does not copy any visualization of thematic data.
	*/
	public MapViewer makeClearCopy() {
		if (lman == null || lman.getLayerCount() < 1)
			return null;
		LayerManager lmCopy = lman.makeCopy();
		for (int i = 0; i < lmCopy.getLayerCount(); i++) {
			lmCopy.getGeoLayer(i).setVisualizer(null);
			lmCopy.getGeoLayer(i).setBackgroundVisualizer(null);
		}
		SimpleMapView mw = new SimpleMapView(supervisor, lmCopy);
		mw.setIsPrimary(false);
		mw.setup();
		MapContext mc = map.getMapContext();
		if (mc != null) {
			RealRectangle rr = mc.getVisibleTerritory();
			if (rr != null) {
				mw.showTerrExtent(rr.rx1, rr.ry1, rr.rx2, rr.ry2);
			}
		}
		return mw;
	}

	protected void moveManipulators(SimpleMapView mw) {
		if (manHandler != null) {
			int nm = manHandler.getManipulatorCount();
			Component mans[] = new Component[nm];
			String ids[] = new String[nm];
			Object visualizers[] = new Object[nm];
			for (int i = 0; i < nm; i++) {
				mans[i] = manHandler.getManipulator(i);
				ids[i] = manHandler.getOwnerId(i);
				visualizers[i] = manHandler.getVisualizer(i);
			}
			removeTab(res.getString("Manipulate"));
			manHandler.removeAllManipulators();
			for (int i = 0; i < nm; i++) {
				unlinkMapAndManipulator(mans[i]);
				mw.addMapManipulator(mans[i], visualizers[i], ids[i]);
			}
		}
	}

	/**
	* Adds to the map view the map manipulator attached to the layer with the
	* given identifier
	*/
	public void addMapManipulator(Component man, Object visualizer, String layerId) {
		if (lman == null || man == null || layerId == null)
			return;
		int idx = lman.getIndexOfLayer(layerId);
		if (idx < 0)
			return; //no such layer on the map
		if (manHandler != null) {
			//unlink the map with the previous manipulator of the same layer
			Component oldMan = manHandler.getManipulator(visualizer, layerId);
			if (oldMan != null) {
				unlinkMapAndManipulator(oldMan);
			}
		} else {
			manHandler = new ManipulatorHandler();
			manHandler.addPropertyChangeListener(this);
		}
		manHandler.addManipulator(man, visualizer, layerId, lman.getGeoLayer(idx).getName());
		if (manHandler.getManipulatorCount() == 1) {
			addTab(manHandler, res.getString("Manipulate"));
		}
		linkMapAndManipulator(man);
	}

	/**
	* Removes from the map view the map manipulator attached to the layer with the
	* given identifier
	*/
	public void removeMapManipulator(Object visualizer, String layerId) {
		if (lman == null || manHandler == null || layerId == null)
			return;
		Component oldMan = manHandler.getManipulator(visualizer, layerId);
		if (oldMan != null) {
			unlinkMapAndManipulator(oldMan);
			manHandler.removeManipulator(visualizer, layerId);
			CManager.destroyComponent(oldMan);
			if (manHandler.getManipulatorCount() < 1) {
				removeTab(res.getString("Manipulate"));
			}
		}
	}

	/**
	 * Returns the manipulator of the layer with the given identifier
	 * and the given visualizer 
	 */
	public Component getMapManipulator(Object visualizer, String layerId) {
		if (lman == null || manHandler == null || layerId == null)
			return null;
		return manHandler.getManipulator(visualizer, layerId);
	}

	/**
	* Sets a link between this map and the map manipulator added to it so that
	* the manipulator could receive object events from the map.
	*/
	protected void linkMapAndManipulator(Component manipulator) {
		if (manipulator != null)
			if (manipulator instanceof ObjectEventReactor) {
				//set a link between the map and the manipulator
				((ObjectEventReactor) manipulator).setPrimaryEventSource(this);
			} else if (manipulator instanceof Container) {
				Container c = (Container) manipulator;
				for (int i = 0; i < c.getComponentCount(); i++) {
					linkMapAndManipulator(c.getComponent(i));
				}
			}
	}

	/**
	* Breaks in existing ObjectBrokers (taken from the supervisor) the links
	* between this map and the map manipulator (when manipulator is removed)
	*/
	protected void unlinkMapAndManipulator(Component manipulator) {
		if (manipulator != null && (manipulator instanceof ObjectEventReactor)) {
			//set a link between the map and the manipulator
			((ObjectEventReactor) manipulator).setPrimaryEventSource(null);
		}
	}

	/**
	* Removes all map manipulators associated with the given layer
	*/
	protected void removeManipulatorsOfLayer(String layerId) {
		if (lman == null || manHandler == null || layerId == null)
			return;
		Vector vis = manHandler.getVisualizers(layerId);
		if (vis == null)
			return;
		for (int i = 0; i < vis.size(); i++) {
			removeMapManipulator(vis.elementAt(i), layerId);
		}
	}

	/**
	* When map scale changes, state of the buttons may also change
	*/
	public void propertyChange(PropertyChangeEvent evt) {
		String propName = evt.getPropertyName();
		if (propName.equals("error") || propName.equals("status")) {
			if (supervisor != null && supervisor.getUI() != null) {
				supervisor.getUI().showMessage((String) evt.getNewValue(), propName.equals("error"));
			}
			return;
		}
		if (evt.getSource() == map && propName.equals("MapScale")) {
			setButtonStatus();
		} else if (propName.equals("LayerAdded")) {
			//ID
			layerAdded((GeoLayer) evt.getNewValue());
//~ID
		} else if (propName.equals("LayerRemoved")) {
			layerRemoved((GeoLayer) evt.getOldValue());
			setButtonStatus();
		} else if (propName.equals("ActiveLayer")) {
			if (manHandler != null && manHandler.getManipulatorCount() > 0) {
				Object obj = evt.getNewValue();
				String layerId = null;
				if (obj != null && obj instanceof GeoLayer) {
					layerId = ((GeoLayer) obj).getContainerIdentifier();
				}
				if (layerId != null) {
					manHandler.activateManipulator(layerId);
				}
			}
		} else if (propName.equals("ThematicDataRemoved")) {
			if (evt.getNewValue() != null && (evt.getNewValue() instanceof GeoLayer)) {
				GeoLayer gl = (GeoLayer) evt.getNewValue();
				removeManipulatorsOfLayer(gl.getContainerIdentifier());
			}
		} else if (evt.getSource() == manHandler) {
			if (!evt.getPropertyName().equals("active_man"))
				return;
			String layerId = (String) evt.getNewValue();
			if (layerId == null)
				return;
			lman.activateLayer(layerId);
		}
	}

//ID
	/**
	* Makes necessary preparations when a layer is added to the map.
	* In particular, enables panning buttons
	*/
	protected void layerAdded(GeoLayer l) {
		if (l == null)
			return;
		setButtonStatus();
		if (l instanceof DOSMLayer) {
			enableZoomToImageResolution();
		}
	}

//~ID
	/**
	* Makes necessary cleaning when a layer is removed from the map.
	* In particular, unregisters the layer's visualizer at the supervisor
	* and removes the manipulator related to this layer.
	*/
	protected void layerRemoved(GeoLayer l) {
		if (l == null)
			return;
		removeManipulatorsOfLayer(l.getContainerIdentifier());
		Visualizer vis = l.getVisualizer();
		if (vis != null && (vis instanceof DataTreater)) {
			supervisor.removeDataDisplayer((DataTreater) vis);
		}
		DOSMLayer bkgOSMLayer = getBkgImageLayer();
		if (bkgOSMLayer == null) {
			this.disableZoomToImageResolution();
		}
	}

	public void setButtonStatus() {
		ac[0].setEnabled(map.canMove("North") && map.canMove("West"));
		ac[1].setEnabled(map.canMove("North"));
		ac[2].setEnabled(map.canMove("North") && map.canMove("East"));
		ac[3].setEnabled(map.canMove("East"));
		ac[4].setEnabled(map.canMove("South") && map.canMove("East"));
		ac[5].setEnabled(map.canMove("South"));
		ac[6].setEnabled(map.canMove("South") && map.canMove("West"));
		ac[7].setEnabled(map.canMove("West"));
	}

	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		if (cmd == null)
			return;
		if (cmd.startsWith("help_")) {
			Helper.help(cmd.substring(5));
			return;
		}
		if (map == null)
			return;
		if (cmd.equals(bCmd[0])) { // NW
			map.move("North");
			map.move("West");
			return;
		}
		if (cmd.equals(bCmd[1])) { // N
			map.move("North");
			return;
		}
		if (cmd.equals(bCmd[2])) { // NE
			map.move("North");
			map.move("East");
			return;
		}
		if (cmd.equals(bCmd[3])) { // E
			map.move("East");
			return;
		}
		if (cmd.equals(bCmd[4])) { // SE
			map.move("South");
			map.move("East");
			return;
		}
		if (cmd.equals(bCmd[5])) { // S
			map.move("South");
			return;
		}
		if (cmd.equals(bCmd[6])) { // SW
			map.move("South");
			map.move("West");
			return;
		}
		if (cmd.equals(bCmd[7])) { // W
			map.move("West");
			return;
		}
		if (cmd.equals(SimpleMapToolBar.cmdPrint)) {
			ImagePrinter printer = new ImagePrinter(supervisor);
			printer.saveOrPrintMap(this, true);
			return;
		}
		if (cmd.equals(SimpleMapToolBar.cmdSaveToFile)) {
			ImagePrinter printer = new ImagePrinter(supervisor);
			printer.saveOrPrintMap(this, false);
			return;
		}
		if (cmd.equals(SimpleMapToolBar.cmdDeselect)) {
			if (objMan != null) {
				objMan.clearSelection();
			}
			return;
		}
		if (cmd.equals("ZoomToImageResolution")) {
			if (!(lman instanceof DLayerManager))
				return;
			Dimension iSize = null;
			DLayerManager lm = (DLayerManager) lman;
			DOSMLayer bkgOSMLayer = lm.getOSMLayer();
			if (bkgOSMLayer != null) {
				iSize = bkgOSMLayer.getImageSize();
			}
			if (iSize == null || iSize.width < 1 || iSize.height < 1) {
				bkgOSMLayer = lm.getGMLayer();
				if (bkgOSMLayer != null) {
					iSize = bkgOSMLayer.getImageSize();
				}
				if (iSize == null || iSize.width < 1 || iSize.height < 1)
					return;
			}
			RealRectangle imageTerrBounds = bkgOSMLayer.getImageExtent();
			if (imageTerrBounds == null)
				return;
			double stepX = 1.0 * (imageTerrBounds.rx2 - imageTerrBounds.rx1) / iSize.width, stepY = 1.0 * (imageTerrBounds.ry2 - imageTerrBounds.ry1) / iSize.height;
			MapContext mc = map.getMapContext();
			Rectangle vp = mc.getViewportBounds();
			float mx = mc.absX(vp.x + vp.width / 2), my = mc.absY(vp.y + vp.height / 2);
			double dx = (stepX * vp.width) / 2, dy = (stepY * vp.height) / 2;
			map.showTerrExtent((float) (mx - dx), (float) (my - dy), (float) (mx + dx), (float) (my + dy));
			return;
		}
	}

	/**
	* Returns the layer manager of the map
	*/
	public LayerManager getLayerManager() {
		return lman;
	}

	/**
	* Zooms the map so that the whole territory is visible
	*/
	public void showWholeTerritory() {
		map.pan();
	}

	/**
	* Zooms the map so that the specified territory extent is visible
	*/
	public void showTerrExtent(float x1, float y1, float x2, float y2) {
		map.showTerrExtent(x1, y1, x2, y2);
	}

	/**
	 * Asks the map canvas to mark the specified absolute spatial (geographic) position
	 * on the map.
	 */
	public void markPosition(float posX, float posY) {
		map.markPosition(posX, posY);
	}

	/**
	 * Asks the map canvas to erase the mark of the previously specified absolute spatial
	 * (geographic) position, if any.
	 */
	public void erasePositionMark() {
		map.erasePositionMark();
	}

	/**
	 * If there is a layer connected to OpenStreetMap, enables
	 * zooming to the current image resolution
	 */
	protected void enableZoomToImageResolution() {
		if (bZoomToImageResolution == null && toolbar != null) {
			bZoomToImageResolution = new TImgButton("/icons/image_symbol.gif");
			bZoomToImageResolution.setActionCommand(cmdZoomToImageResolution);
			bZoomToImageResolution.addActionListener(this);
			toolbar.addToolbarElementRight(bZoomToImageResolution);
			new PopupManager(bZoomToImageResolution, "Zoom to the current resolution of the OpenStreetMap image", true);
		}
	}

	/**
	 * Disables zooming to image resolution
	 */
	protected void disableZoomToImageResolution() {
		if (bZoomToImageResolution != null && toolbar != null) {
			toolbar.removeToolbarElementRight(bZoomToImageResolution);
			bZoomToImageResolution = null;
		}
	}

	/**
	* A method from the ObjectEventHandler interface.
	* The MapView receives object events from its ObjectManager and tranferres
	* them to the supervisor.
	*/
	public void processObjectEvent(ObjectEvent oevt) {
		if (supervisor != null && oevt != null) {
			ObjectEvent e = new ObjectEvent(this, oevt.getType(), oevt.getSourceMouseEvent(), oevt.getSetIdentifier(), oevt.getAffectedObjects());
			e.setTimeRefs(oevt.getTimeRefs());
			e.dataT = objMan;
			supervisor.processObjectEvent(e);
		}
	}

	/**
	* A method from the DataViewInformer interface.
	* A map view calls the corresponding method of its object manager.
	*/
	public DataViewRegulator getDataViewRegulator() {
		return objMan.getDataViewRegulator();
	}

	/**
	* A method from the DataViewInformer interface.
	* A map view calls the corresponding method of its object manager.
	*/
	public TransformedDataPresenter getTransformedDataPresenter() {
		return objMan.getTransformedDataPresenter();
	}

//---------------- implementation of the EventSource interface ------------
	/**
	* The EventSource answers whether it can produce the specified
	* type of events.
	*/
	public boolean doesProduceEvent(String eventId) {
		return eventId.equals(ObjectEvent.click) || eventId.equals(ObjectEvent.point) || eventId.equals(ObjectEvent.frame);
	}

	/**
	* Returns the unique identifier of this map view. The identifier is used
	* 1) for explicit linking of producers and recipients of object events;
	* 2) for correct restoring of system states with multiple map windows.
	*/
	public String getIdentifier() {
		return mapViewId;
	}

	public int getInstanceN() {
		return instanceN;
	}

	/**
	* Sets the unique identifier of the map view.
	*/
	public void setIdentifier(String id) {
		if (id != null) {
			mapViewId = id;
		}
		if (lman != null) {
			for (int i = 0; i < lman.getLayerCount(); i++) {
				Visualizer vis = lman.getGeoLayer(i).getVisualizer();
				if (vis != null) {
					vis.setLocation(mapViewId);
				}
				vis = lman.getGeoLayer(i).getBackgroundVisualizer();
				if (vis != null) {
					vis.setLocation(mapViewId);
				}
			}
		}
	}

//ID
	/**
	 * Returns the keyword used in the opening tag of a stored state description
	 * of this tool. A tool state description (specification) is stored as a
	 * sequence of lines starting with <tagName> and ending with </tagName>, where
	 * tagName is a unique keyword for a particular class of tools.
	 */
	public String getTagName() {
		return "map_window";
	}

	/**
	 * Returns the specification (i.e. state description) of this tool for storing
	 * in a file. The specification must allow correct re-construction of the tool.
	 */
	public Object getSpecification() {
		MapWindowSpec spec = new MapWindowSpec();
		spec.tagName = getTagName();
		spec.primary = getIsPrimary();
		spec.windowId = getIdentifier();
		float[] extent = getMapExtent();
		spec.extent = new RealRectangle(extent[0], extent[1], extent[2], extent[3]);
		Frame mapFrame = CManager.getFrame(this);
		if (mapFrame != null) {
			spec.title = mapFrame.getTitle();
			spec.bounds = mapFrame.getBounds();
		}
		spec.properties = getProperties();
		return spec;
	}

	/**
	* Returns the properties which can be stored in an ASCII file. Each property
	* has its unique key and a value, both are strings.
	*/
	public Hashtable getProperties() {
		Hashtable prop = new Hashtable();
		LayoutManager l = getLayout();
		if (l != null && (l instanceof SplitLayout)) {
			SplitLayout spl = (SplitLayout) l;
			float p = spl.getComponentPart(0);
			if (Float.isNaN(p)) {
				p = 0;
			}
			prop.put("legendPart", String.valueOf(p));
			p = spl.getComponentPart(2);
			if (Float.isNaN(p)) {
				p = 0;
			}
			prop.put("manipulatorPart", String.valueOf(p));
		}
		if (prop.isEmpty())
			return null;
		return prop;
	}

	/**
	 * After the tool is constructed, it may be requested to setup its individual
	 * properties according to the given list of stored properties.
	 */
	public void setProperties(Hashtable properties) {
		if (properties == null || properties.isEmpty())
			return;
		float lp = Float.NaN, mp = Float.NaN;
		String str = (String) properties.get("legendPart");
		if (str != null) {
			try {
				lp = Float.valueOf(str).floatValue();
			} catch (NumberFormatException nfe) {
			}
		}
		str = (String) properties.get("manipulatorPart");
		if (str != null) {
			try {
				mp = Float.valueOf(str).floatValue();
			} catch (NumberFormatException nfe) {
			}
		}
		if (!Float.isNaN(lp) || !Float.isNaN(mp)) {
			LayoutManager l = getLayout();
			if (l != null && (l instanceof SplitLayout)) {
				SplitLayout spl = (SplitLayout) l;
				if (!Float.isNaN(lp) && !Float.isNaN(mp)) {
					float p[] = { lp, 1.0f - lp - mp, mp };
					spl.setProportions(p);
				} else if (!Float.isNaN(lp)) {
					spl.changePart(0, lp);
				} else {
					spl.changePart(2, mp);
				}
			}
			CManager.validateAll(map);
		}
	}

	/**
	 * Adds a listener to be notified about destroying the tool.
	 * A SaveableTool may be registered somewhere and, hence, must notify the
	 * component where it is registered about its destroying.
	 */
	public void addDestroyingListener(PropertyChangeListener lst) {
		if (lst != null) {
			destroyingListeners.addElement(lst);
		}
	}

	Vector destroyingListeners = new Vector();

//~ID

	/**
	* Unregisters itself and its ObjectManager from listening events and
	* other lists.
	*/
	public void destroy() {
		if (supervisor != null) {
			supervisor.removeObjectEventSource(this);
			for (int i = 0; i < lman.getLayerCount(); i++) {
				Visualizer vis = lman.getGeoLayer(i).getVisualizer();
				if (vis != null && (vis instanceof DataTreater)) {
					supervisor.removeDataDisplayer((DataTreater) vis);
				}
			}
		}
		objMan.destroy();
		destroyed = true;
//ID
		for (int i = 0; i < destroyingListeners.size(); i++) {
			((PropertyChangeListener) destroyingListeners.elementAt(i)).propertyChange(new PropertyChangeEvent(this, "destroyed", null, null));
//~ID
		}
	}

	/**
	* Replies whether is destroyed or not
	*/
	public boolean isDestroyed() {
		return destroyed;
	}

	protected boolean isSaveAvailable() {
		String servletURL = null;
		boolean OK = false;
		if (supervisor != null) {
			servletURL = supervisor.getSystemSettings().getParameterAsString("ImageServlet");
			OK = (servletURL != null || supervisor.getSystemSettings().checkParameterValue("isLocalSystem", "true"));
		}
		return OK;
	}

	public void catchFocus() {
		if (map != null) {
			map.requestFocus();
		}
	}

//ID
	public Image getMapAsImage(float ratio) {
		Dimension d = map.getSize();
		d.width = Math.round(ratio * d.width);
		d.height = Math.round(ratio * d.height);
		return getMapAsImage(d, false);
	}

	public Image getMapAsImage() {
		Image img = map.createImage(map.getSize().width, map.getSize().height);
		if (img == null)
			return null;
		Image img2 = map.print(img.getGraphics(), map.getSize(), false, false, false, null);
		//if (annotationSurfacePresent()) getAnnotationSurface().paint(img2.getGraphics());
		return img2;
	}

	public Image getMapBkgImage() {
		return map.getBackgroundImage();
	}

	public Image getMapAsImage(Dimension destViewPort) {
		return getMapAsImage(destViewPort, true);
	}

	public Image getMapAsImage(Dimension destViewPort, boolean copyrightMsg) {
		Image img = map.createImage(destViewPort.width, destViewPort.height);
		if (img == null)
			return null;
		Image img2 = map.print(img.getGraphics(), destViewPort, false, false, copyrightMsg, supervisor.getSystemSettings().getParameterAsString("ABOUT_PROJECT"));
		if (annotationSurfacePresent()) {
			getAnnotationSurface().paint(img2.getGraphics());
		}
		return img2;
	}

	public Image getLegendAsImage(Dimension imgSize) {
		Image img = map.createImage(imgSize.width, imgSize.height);
		if (img == null)
			return null;
		return map.printLegend(img.getGraphics(), imgSize, false);
	}

	public Image getMapAndLegendAsImage(Dimension destViewPort) {
		Image img = map.createImage(destViewPort.width, destViewPort.height);
		if (img == null)
			return null;
		Image img2 = map.printMapAndLegend(img.getGraphics(), destViewPort, false, false, true, supervisor.getSystemSettings().getParameterAsString("ABOUT_PROJECT"));
		if (annotationSurfacePresent()) {
			getAnnotationSurface().paint(img2.getGraphics());
		}
		return img2;
	}

//~ID
//ID
	// --------------------  annotation support  --------------------------
	private String helperClassName = "connection.observer.annotation.MapAnnotationSurface";

	protected AnnotationSurfaceInterface annotationSurface;
	protected boolean presenceChecked = false;

	public boolean annotationSurfacePresent() {
		if (presenceChecked)
			return annotationSurface != null;
		return getAnnotationSurface() != null;
	}

	public AnnotationSurfaceInterface getAnnotationSurface() {
		if (annotationSurface == null) {
			try {
				annotationSurface = (AnnotationSurfaceInterface) (Object) Class.forName(helperClassName).newInstance();
				annotationSurface.connect(this);
			} catch (Exception ex) {
			}
		}
		presenceChecked = true;
		return annotationSurface;
	}
//~ID
}
