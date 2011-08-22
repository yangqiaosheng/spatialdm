package data_input;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Choice;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Point;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.system.DataLoader;
import spade.analysis.system.Supervisor;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Metrics;
import spade.lib.basicwin.OKDialog;
import spade.lib.basicwin.RowLayout;
import spade.lib.basicwin.TextCanvas;
import spade.lib.lang.Language;
import spade.vis.action.HighlightListener;
import spade.vis.action.Highlighter;
import spade.vis.database.AttributeDataPortion;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;
import spade.vis.dmap.DLayerManager;
import spade.vis.dmap.DQuadTreeLayer;
import spade.vis.dmap.DrawingParameters;
import spade.vis.event.DEvent;
import spade.vis.event.DMouseEvent;
import spade.vis.event.EventConsumer;
import spade.vis.event.EventMeaningManager;
import spade.vis.geometry.RealRectangle;
import spade.vis.map.MapDraw;
import spade.vis.map.Zoomable;
import spade.vis.mapvis.IconPresenter;
import spade.vis.mapvis.Visualizer;
import spade.vis.preference.IconCorrespondence;
import spade.vis.preference.IconVisSpec;
import configstart.MapVisInitiator;

/**
 * Supports input of the locations of some objects, observations, etc. through
 * a map. In particular, is used for FloraWeb.
 */
public class DataInputManager extends Panel implements ItemListener, PropertyChangeListener, EventConsumer, HighlightListener, ComponentListener {
	static ResourceBundle res = Language.getTextResource("data_input.Res");
	/**
	 * A canvas used for displaying various messages
	 */
	protected TextCanvas textC = null;
	/**
	 * The system's supervisor
	 */
	protected Supervisor supervisor = null;
	/**
	 * The data loader loads and keeps all data in the system
	 */
	protected DataLoader dataLoader = null;
	/**
	 * The map drawing component
	 */
	protected MapDraw map = null;
	/**
	 * The layer manager of the map
	 */
	protected DLayerManager layerMan = null;
	/**
	 * The layer (grid) in which object selections are made
	 */
	protected DQuadTreeLayer layer = null;
	/**
	 * The original layer (grid), which is used for the visualization of
	 * already existing data. This is not a hierarchical grid!
	 */
	protected DGeoLayer origLayer = null;
	/**
	 * The manager of meanings of mouse events on the map
	 */
	protected EventMeaningManager emm = null;
	/**
	 * The identifier of the meaning assigned to mouse click events on the map
	 */
	protected String mouseMeaningId = "area_select";
	/**
	 * The full text of the meaning assigned to mouse click events on the map
	 */
	protected String mouseMeaningText = "area selection";
	/**
	 * Remembers the previous meaning of map click events.
	 */
	protected String clickMeaning = null;
	/**
	 * Used for the selection of the grid depth
	 */
	protected Choice levelCh = null;
	/**
	 * The list of the identifiers of the cells chosen by the user
	 */
	protected Vector selCells = null;
	/**
	 * The list of points entered by the user
	 */
	protected Vector enteredPoints = null;
	/**
	 * Indicates the current input mode: false means cell selection (default) and true
	 * means point entering.
	 */
	protected boolean pointInputMode = false;

	public float switchScale = 0f;

	String methodId = null;
	/**
	 *
	 * @param sup
	 */
	protected AttributeDataPortion dTable = null;

	/**
	 *  Visualizers
	 */
	protected Visualizer stdVis = null;
	protected Visualizer icpVis = null;

	/**
	 *  Icons for visualizer
	 */

	protected Vector iconVisSpecList = null;

	/**
	 * Sets the system's supervisor
	 */
	public void setSupervisor(Supervisor sup) {
		supervisor = sup;
	}

	/**
	 * Sets the system's data loader and keeper
	 */
	public void setDataLoader(DataLoader loader) {
		dataLoader = loader;
	}

	/**
	 * Prepares to work: finds the layer in which object selections will be made
	 * and starts listening to selection events. Stores references to relevant
	 * structures in its internal variables.
	 */
	public boolean prepareToWork() {
		if (supervisor == null || dataLoader == null)
			return false;
		map = supervisor.getUI().getMapViewer(0).getMapDrawer();
		if (map == null)
			return false;
		layerMan = (DLayerManager) dataLoader.getMap(0);
		if (layerMan == null)
			return false;
		String layerId = supervisor.getSystemSettings().getParameterAsString("select_in_layer");
		DGeoLayer l = null;
		int idx = -1;
		if (layerId != null) {
			idx = layerMan.getIndexOfLayer(layerId);
		} else {
			idx = layerMan.getIndexOfActiveLayer();
		}
		if (idx < 0)
			return false;
		l = (DGeoLayer) layerMan.getGeoLayer(idx);
		if (l == null)
			return false;
		if (l.getObjectCount() < 1) {
			l.loadGeoObjects();
		}
		if (l instanceof DQuadTreeLayer) {
			layer = (DQuadTreeLayer) l;
		} else {
			origLayer = l;
			layer = new DQuadTreeLayer();
			if (!layer.constructGrid(origLayer))
				return false;
			layer.setContainerIdentifier("hier_grid_" + origLayer.getContainerIdentifier());
			layer.setName(res.getString("new_observ"));
			origLayer.setName(res.getString("previous_observ"));
			DrawingParameters dparm = origLayer.getDrawingParameters();
			dparm.drawCondition = false;
			dparm.maxScaleDC = dparm.minScaleDC = Float.NaN;
			dparm.lineWidth = 1;
			dparm.drawBorders = false;
			dparm.useDefaultFilling = false;
			origLayer.show_nobjects = false;
			if (dataLoader != null) {
				dataLoader.addMapLayer(layer, 0);
			} else {
				layer.setEntitySetIdentifier("hier_grid_" + origLayer.getEntitySetIdentifier());
				layerMan.addGeoLayer(layer);
			}
			idx = layerMan.getIndexOfLayer(layer.getContainerIdentifier());
		}
		layer.setUsedForInput(true);
		layer.show_nobjects = false;
		DrawingParameters dparm = layer.getDrawingParameters();
		dparm.useDefaultFilling = false;
		layerMan.activateLayer(idx);
		supervisor.getSystemSettings().setParameter("Allow_Background_Visualization", "false");
		//start listening to object (i.e. grid cell) selection events in the layer
		supervisor.registerHighlightListener(this, layer.getEntitySetIdentifier());
		//start listening to map zooming and redrawing events
		map.addPropertyChangeListener(this);
		//prepare to change, if needed, the meaning of map click events
		emm = map.getEventMeaningManager();
		emm.addEventMeaning(DMouseEvent.mClicked, mouseMeaningId, mouseMeaningText);
		constructUI();
		addComponentListener(this);
		return true;
	}

	/**
	 * If all the settings have been made correctly, constructs the UI for
	 * the data input support
	 */
	protected void constructUI() {
		setName(res.getString("Data_input"));
		setLayout(new BorderLayout());
		textC = new TextCanvas();
		add(textC, BorderLayout.CENTER);
		textC.setText("Data input console");
		Panel p = new Panel(new ColumnLayout());
		Panel pp = new Panel(new RowLayout());
		pp.add(new Label(res.getString("grid_depth") + ":"));
		levelCh = new Choice();
		levelCh.addItemListener(this);
		for (int i = 0; i <= layer.getMaxLevel(); i++) {
			levelCh.add(String.valueOf(i));
		}
		levelCh.select(layer.getCurrentLevel());
		pp.add(levelCh);
		p.add(pp);
		p.add(new Label(res.getString("Mouse_click_means")));
		CheckboxGroup cbg = new CheckboxGroup();
		Checkbox cb = new Checkbox(res.getString("select_cell"), true, cbg);
		cb.setName("select_cell");
		cb.addItemListener(this);
		p.add(cb);
		cb = new Checkbox(res.getString("point_input"), false, cbg);
		cb.setName("point_input");
		cb.addItemListener(this);
		p.add(cb);
		add(p, BorderLayout.SOUTH);
	}

	/**
	 * Reacts to switching modes
	 */
	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource() instanceof Checkbox) {
			//switch from cell selection to point input and back
			Checkbox cb = (Checkbox) e.getSource();
			if (cb.getState()) {
				String setId = layer.getEntitySetIdentifier();
				Highlighter hl = supervisor.getHighlighter(setId);
				textC.setText("");
				if (cb.getName().equals("select_cell")) {
					pointInputMode = false;
					if (enteredPoints != null && enteredPoints.size() > 0) {
						map.restorePicture(); //erase points
					}
					if (clickMeaning != null) {
						emm.setCurrentEventMeaning(DMouseEvent.mClicked, clickMeaning);
					}
					map.removeMapListener(this);
					layer.setLayerDrawn(true);
					layerMan.activateLayer(layer.getContainerIdentifier());
					//start listening to selection events from the layer
					supervisor.registerHighlightListener(this, setId);
					//show the identifiers of the currently selected cells in the text canvas
					if (selCells != null && selCells.size() > 0) {
						hl.replaceSelectedObjects(this, selCells);
					} else {
						hl.clearSelection(this);
					}
				} else {
					pointInputMode = true;
					//clear current selection (after remembering it) and hide the grid
					Vector v = hl.getSelectedObjects();
					if (v != null) {
						selCells = (Vector) v.clone();
					} else {
						selCells = null;
					}
					hl.clearSelection(this);
					layer.setLayerDrawn(false);
					//stop listening to selection events from the layer
					supervisor.removeHighlightListener(this, setId);
					//Change the current meaning of map events
					clickMeaning = emm.getCurrentEventMeaning(DMouseEvent.mClicked);
					emm.setCurrentEventMeaning(DMouseEvent.mClicked, mouseMeaningId);
					//start listening to mouse events from the map
					map.addMapListener(this);
					if (enteredPoints != null && enteredPoints.size() > 0) {
						for (int i = 0; i < enteredPoints.size() && i < 15; i++) {
							EnteredPoint pt = (EnteredPoint) enteredPoints.elementAt(i);
							textC.addTextLine("[" + pt.tk_grid + "] x=" + pt.x + " y=" + pt.y + " r=" + pt.radius + (i == 14 ? " ..." : ""));

						}
						textC.addTextLine("total records:" + enteredPoints.size());
						redrawPoints();
					}
				}
			}
		} else if (e.getSource().equals(levelCh)) {
			//change the depth level of the grid
			layer.setCurrentLevel(levelCh.getSelectedIndex());
		}
	}

	/**
	 * Reacts to map zooming and redrawing events
	 */
	@Override
	public void propertyChange(PropertyChangeEvent e) {

		if (e.getPropertyName().equalsIgnoreCase("MapScale") && methodId != null && (methodId.equals("qualitative_colour") || methodId.equals("icons"))

		) {

			setVisualizer();

		}
		if (e.getSource().equals(map)) {
			redrawPoints();
		}
	}

//--------------- HighlightListener interface ----------------------------------
	/**
	 * Notification about change of the set of objects to be transiently
	 * highlighted. The Data Input Manager does not react to these events.
	 */
	@Override
	public void highlightSetChanged(Object source, String setId, Vector highlighted) {
	}

	/**
	 * Notification about change of the set of objects to be selected (durably
	 * highlighted). The argument "selected" is a vector of identifiers of
	 * currently selected objects.
	 */

	@Override
	public void selectSetChanged(Object source, String setId, Vector selected) {
		if (!layer.getIsActive())
			return;
		textC.setText("");
		if (selected != null && selected.size() > 0) {
			if (selected.size() > 1) {
				//probably, it is necessary to "clean" the set of selected cells so
				//that it does not contain simultaneously a cell and some of its subcells
				Vector newSel = new Vector(selected.size(), 1);
				for (int i = 0; i < selected.size(); i++) {
					boolean isSubCell = false;
					String id0 = (String) selected.elementAt(i);
					for (int j = 0; j < selected.size() && !isSubCell; j++)
						if (j != i) {
							String id1 = (String) selected.elementAt(j);
							isSubCell = id0.startsWith(id1);
						}
					if (!isSubCell) {
						newSel.addElement(id0);
					}
				}
				if (newSel.size() < selected.size()) {
					supervisor.getHighlighter(layer.getEntitySetIdentifier()).replaceSelectedObjects(this, newSel);
					return;
				}
			}

			for (int i = 0; i < selected.size() && i < 15; i++) {
				String id = (String) selected.elementAt(i);
				if (layer.findObjectById(id) != null) {
					textC.addTextLine("[" + id + "]" + (i == 14 ? " ..." : ""));
				}
			}
			textC.addTextLine("total records:" + selected.size());
		}
	}

//--------------- EventConsumer interface ----------------------------------
	/**
	 * The Event Consumer answers whether it consumes the specified event
	 * when it has the given meaning. The DataInputManager consumes mouse clicks
	 */
	@Override
	public boolean doesConsumeEvent(String evtType, String evtMeaning) {
		return isShowing() && evtType != null && evtType.equals(DMouseEvent.mClicked) && evtMeaning != null && evtMeaning.equals(mouseMeaningId);
	}

	/**
	 * The Event Consumer answers whether it consumes the specified event
	 * when it has the given meaning. The DistanceMeasurer consumes mouse click,
	 * mouse move, and mouse double click events
	 */
	@Override
	public boolean doesConsumeEvent(DEvent evt, String evtMeaning) {
		return isShowing() && evt != null && evt.getSource().equals(map) && doesConsumeEvent(evt.getId(), evtMeaning);
	}

	/**
	     * The EventReceiver answers whether it is interested in getting the specified
	 * kind of events. Besides the consumed events, the DistanceMeasurer is
	 * interested to get mouse exit events
	 */
	@Override
	public boolean doesListenToEvent(String evtType) {
		return isShowing() && evtType.equals(DMouseEvent.mClicked);
	}

	/**
	 * This method is used for delivering events to the Event Receiver.
	 */
	@Override
	public void eventOccurred(DEvent evt) {
		if (!isShowing())
			return;
		if (evt == null)
			return;
		if (!(evt instanceof DMouseEvent))
			return;
		DMouseEvent me = (DMouseEvent) evt;
		if (!me.getId().equals(DMouseEvent.mClicked))
			return;
		//check whether the user clicked on an existing point
		int x = me.getX(), y = me.getY();
		if (enteredPoints != null && enteredPoints.size() > 0) {
			int idx = -1, sx = 0, sy = 0;
			for (int i = 0; i < enteredPoints.size() && idx < 0; i++) {
				EnteredPoint pt = (EnteredPoint) enteredPoints.elementAt(i);
				sx = map.getMapContext().scrX(pt.x, pt.y);
				sy = map.getMapContext().scrY(pt.x, pt.y);
				if (Math.abs(x - sx) <= Metrics.mm() && Math.abs(y - sy) <= Metrics.mm()) {
					idx = i;
				}
			}
			if (idx >= 0) { //erase this point and redraw the map
				int r = Metrics.mm() + 3, d = r * 2;
				Graphics g = map.getGraphics();
				if (g != null) {
					g.setColor(Color.black);
					g.fillOval(sx - r, sy - r, d, d);
					g.drawOval(sx - r - 1, sy - r - 1, d + 2, d + 2);
					g.setColor(Color.yellow);
					g.drawOval(sx - r, sy - r, d, d);
					g.drawOval(sx - r + 1, sy - r + 1, d - 2, d - 2);
				}
				OKDialog okd = new OKDialog(CManager.getAnyFrame(this), res.getString("erase_point"), OKDialog.YES_NO_MODE, true);
				okd.addContent(new Label(res.getString("erase_this_point")));
				Point p = ((Component) map).getLocationOnScreen();
				p.x += sx + r;
				p.y += sy;
				okd.show(p);
				if (!okd.wasCancelled()) {
					enteredPoints.removeElementAt(idx);
					textC.setText("");
					for (int i = 0; i < enteredPoints.size() && i < 15; i++) {
						EnteredPoint pt = (EnteredPoint) enteredPoints.elementAt(i);
						textC.addTextLine("[" + pt.tk_grid + "] x=" + pt.x + " y=" + pt.y + " r=" + pt.radius + (i == 14 ? " ..." : ""));

					}
					textC.addTextLine("total records:" + enteredPoints.size());

				}
				map.restorePicture();
				return;
			}
		}
		Graphics g = map.getGraphics();
		int r = Metrics.mm(), d = r * 2;
		if (g != null) {
			g.setColor(Color.red);
			g.fillOval(x - r, y - r, d, d);
			g.drawOval(x - r - 1, y - r - 1, d + 2, d + 2);
			g.setColor(Color.yellow);
			g.drawOval(x - r, y - r, d, d);
			g.drawOval(x - r + 1, y - r + 1, d - 2, d - 2);
		}
		EnteredPoint pt = new EnteredPoint();
		pt.x = map.getMapContext().absX(x);
		pt.y = map.getMapContext().absY(y);
		pt.radius = r * map.getMapContext().getPixelValue();
		boolean dl = layer.getDrawingParameters().drawLayer;
		layer.getDrawingParameters().drawLayer = true;
		Vector vt = layer.findObjectsAt(x, y, map.getMapContext(), false);
		layer.getDrawingParameters().drawLayer = dl;
		if (vt != null) {
			pt.tk_grid = (String) vt.elementAt(0);
		}

		if (enteredPoints == null) {
			enteredPoints = new Vector(20, 20);

		}

		enteredPoints.addElement(pt);

		if (enteredPoints.size() < 15) {
			textC.addTextLine("[" + pt.tk_grid + "] x=" + pt.x + " y=" + pt.y + " r=" + pt.radius + (enteredPoints.size() == 14 ? " ..." : ""));
			textC.addTextLine("total records:" + enteredPoints.size());

		}

	}

	/**
	 * Redraws all the points entered so far after the map has been redrawn or
	 * zoomed.
	 */
	protected void redrawPoints() {
		if (!pointInputMode || enteredPoints == null || enteredPoints.size() < 1)
			return;
		Graphics g = map.getGraphics();
		if (g == null)
			return;
		int r = Metrics.mm(), d = r * 2;
		for (int i = 0; i < enteredPoints.size(); i++) {
			EnteredPoint pt = (EnteredPoint) enteredPoints.elementAt(i);
			int x = map.getMapContext().scrX(pt.x, pt.y), y = map.getMapContext().scrY(pt.x, pt.y);
			g.setColor(Color.red);
			g.fillOval(x - r, y - r, d, d);
			g.drawOval(x - r - 1, y - r - 1, d + 2, d + 2);
			g.setColor(Color.yellow);
			g.drawOval(x - r, y - r, d, d);
			g.drawOval(x - r + 1, y - r + 1, d - 2, d - 2);
		}
	}

	/**
	 * Returns a unique identifier of the event receiver (may be produced
	 * automatically, used only internally, not shown to the user).
	 */
	@Override
	public String getIdentifier() {
		return "data_input_manager";
	}

	/**
	 * Returns 1 if current mode for data input is grid cell selection.
	 * Returns 2 if current mode for data input is point entering.
	 */
	public int getInputMode() {
		if (pointInputMode)
			return 2;
		return 1;
	}

	/**
	 * Returns a vector of identifiers of selected grid cells. May return null
	 * if the user did not select any cell. Modified by mark for floraweb
	 */
	public Vector getSelectedCells() {
		if (!pointInputMode) {
			Vector selected = supervisor.getHighlighter(layer.getEntitySetIdentifier()).getSelectedObjects();
			if (selected != null && selected.size() > 0)
				return (Vector) selected.clone();
			return null;

		} else {
			// allways return cells ! Mark
			if (selCells != null && selCells.size() > 0)
				return (Vector) selCells.clone();
			return null;
		}

		/*
		if (enteredPoints == null || enteredPoints.size() < 1)
		  return null;
		Vector cells = new Vector(enteredPoints.size(), 1);
		for (int i = 0; i < enteredPoints.size(); i++) {
		  RealPoint pt = (RealPoint) enteredPoints.elementAt(i);
		  String id = layer.findObjectContainingPoint(pt.x, pt.y);
		  if (id != null && !cells.contains(id))
		    cells.addElement(id);
		}
		if (cells.size() < 1)
		  return null;
		return cells;
		*/
	}

	/**
	 * Returns the vector of points entered by the user. Returns null if the
	 * user. Modified by mark for floraweb
	 */
	public Vector getEnteredPoints() {
		if (enteredPoints != null && enteredPoints.size() > 0)
			return enteredPoints;
		return null;
	}

	public void setDataTable(AttributeDataPortion dTable) {
		this.dTable = dTable;
	}

	public void showDataOnMap() {
		if (dTable == null)
			return;
		System.out.println("showDataOnMap");
		Vector attributes = new Vector();
		methodId = "";
		System.out.println("attr:" + dTable.getAttributeName(0));
		origLayer.setDataTable(dTable);

		String an = dTable.getAttributeName(0);
		if (an != null) {
			if (an.equalsIgnoreCase("specnum")) {
				attributes.addElement("specnum");
				methodId = "class1D";
				MapVisInitiator.showDataOnMap(methodId, dTable, attributes, origLayer, supervisor, true);
			} else if (an.equalsIgnoreCase("group")) {
				attributes.addElement("group");
				methodId = "qualitative_colour";
				MapVisInitiator.showDataOnMap(methodId, dTable, attributes, origLayer, supervisor, true);
			} else
				return;
		} else {

		}
		origLayer.notifyPropertyChange("data_updated", null, null);
		origLayer.notifyPropertyChange("ObjectSet", null, null);
		origLayer.notifyPropertyChange("ObjectData", null, null);
	}

	public void setIconVisSpecList(Vector list) {
		iconVisSpecList = list;
	}

	protected void setVisualizer() {
		if (origLayer == null)
			return;
		if (iconVisSpecList == null)
			return;
		Vector attributes = null;
		//if (stdVis == null)  stdVis = origLayer.getVisualizer();
		if (icpVis == null) {
			IconPresenter ip = new IconPresenter();
			ip.addVisChangeListener(this);
			attributes = new Vector();
			attributes.addElement("descr");
			ip.setAttributes(attributes);
			ip.setDataSource(dTable);

			IconCorrespondence ic = new IconCorrespondence();
			ic.setAttributes(attributes);
			for (int i = 0; i < iconVisSpecList.size(); i++) {
				ic.addCorrespondence((IconVisSpec) iconVisSpecList.elementAt(i));
			}
			ip.setCorrespondence(ic);
			ip.setup();
			icpVis = ip;
		}
		float sc = map.getMapContext().getPixelValue() * Metrics.cm() * layerMan.user_factor;

		if (sc < switchScale) {
			if (methodId.equals("icons"))
				return;
			attributes = new Vector();
			attributes.addElement("descr");
			methodId = "icons";
			System.out.println("setVisualizer:" + methodId);
			MapVisInitiator.showDataOnMap("icons", dTable, attributes, origLayer, supervisor, icpVis);
		} else {
			if (methodId.equals("qualitative_colour"))
				return;
			methodId = "qualitative_colour";
			attributes = new Vector();
			attributes.addElement("group");
			System.out.println("setVisualizer:" + methodId);
			MapVisInitiator.showDataOnMap("qualitative_colour", dTable, attributes, origLayer, supervisor, true);
		}
	}

	public void setSelectedObjects(Vector ids) {
		if (supervisor == null || ids == null)
			return;
		layerMan.activateLayer(layerMan.getLayers().indexOf(origLayer));
		Highlighter hl = supervisor.getHighlighter(origLayer.getEntitySetIdentifier());
		System.out.println("makeObjectsSelected:" + ids.size());
		hl.replaceSelectedObjects(map, ids);
		setSelectedTerritory(true);
	}

	public void clearSelectedObjects() {
		if (supervisor == null)
			return;
		System.out.println("clearSelection");
		Highlighter hl = supervisor.getHighlighter(origLayer.getEntitySetIdentifier());
		hl.clearSelection(map);

	}

	// set visible territory for selected objects
	public void setSelectedTerritory(boolean withCurr) {
		if (supervisor == null || !(map instanceof Zoomable))
			return;
		System.out.println("setSelectedTerritory");
		Highlighter hl = supervisor.getHighlighter(origLayer.getEntitySetIdentifier());
		if (hl == null)
			return;
		Vector objs = hl.getSelectedObjects();
		if (objs == null || objs.size() == 0)
			return;

		RealRectangle rr = null;

		for (int i = 0; i < objs.size(); i++) {
			int idx = origLayer.getObjectIndex((String) objs.elementAt(i));
			DGeoObject dgo = origLayer.getObject(idx);
			if (dgo == null) {
				continue;
			}

			if (rr == null) {
				rr = dgo.getBounds();
			} else {
				rr = rr.union(dgo.getBounds());
			}
		}

		if (withCurr) {
			RealRectangle ct = map.getMapContext().getVisibleTerritory();
			rr = rr.union(ct);
			if (rr.equals(ct))
				return;
		}
		System.out.println("visible territory: " + rr.rx1 + " " + rr.ry1 + " " + rr.rx2 + " " + rr.ry2);
		((Zoomable) map).setVisibleTerritory(rr);
	}

	/**
	 *
	 *
	 */
	public Vector getSelectedObjects() {
		if (supervisor == null || origLayer == null)
			return null;
		Highlighter hl = supervisor.getHighlighter(origLayer.getEntitySetIdentifier());
		if (hl == null)
			return null;
		return hl.getSelectedObjects();
	}

	@Override
	public void componentHidden(ComponentEvent e) {
		if (layer != null && origLayer != null && layer.getIsActive()) {
			layerMan.activateLayer(origLayer.getContainerIdentifier());
		}
	}

	@Override
	public void componentShown(ComponentEvent e) {
		if (layer != null && !layer.getIsActive()) {
			layerMan.activateLayer(layer.getContainerIdentifier());
		}
	}

	@Override
	public void componentMoved(ComponentEvent e) {
	}

	@Override
	public void componentResized(ComponentEvent e) {
	}

	public void clearDataOnMap() {
		origLayer.eraseThematicData();
	}

}
