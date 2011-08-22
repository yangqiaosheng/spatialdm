package spade.analysis.tools;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Point;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.system.ESDACore;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.OKDialog;
import spade.lib.basicwin.OKFrame;
import spade.lib.basicwin.TextCanvas;
import spade.lib.lang.Language;
import spade.lib.util.IdMaker;
import spade.lib.util.IdentifierUseChecker;
import spade.lib.util.StringUtil;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.database.SpatialEntity;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;
import spade.vis.dmap.DLayerManager;
import spade.vis.event.DEvent;
import spade.vis.event.DMouseEvent;
import spade.vis.event.EventConsumer;
import spade.vis.event.EventMeaningManager;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.RealPoint;
import spade.vis.geometry.RealPolyline;
import spade.vis.geometry.RealRectangle;
import spade.vis.map.MapDraw;
import spade.vis.map.MapViewer;
import spade.vis.space.LayerManager;

/**
* The component allowing the user to construct his/her own map layer
*/
public class LayerBuilder implements DataAnalyser, ActionListener, EventConsumer, PropertyChangeListener, IdentifierUseChecker {
	static ResourceBundle res = Language.getTextResource("spade.analysis.tools.Res");
	/**
	* The system's core providing access to all its components
	*/
	protected ESDACore core = null;
	/**
	* The layer being constructed
	*/
	protected DGeoLayer layer = null;
	/**
	* The table linked to the layer
	*/
	protected DataTable table = null;
	/**
	* The map in which the layer is constructed
	*/
	protected MapDraw map = null;
	/**
	* The number of the map
	*/
	protected int mapN = -1;
	/**
	* The manager of meanings of mouse events on the map
	*/
	protected EventMeaningManager emm = null;
	/**
	* The identifier of the meaning assigned to mouse events on a map by the
	* layer builder
	*/
	protected String mouseMeaningId = "build";
	/**
	* The full text of the meaning assigned to mouse events on a map by the
	* distance measurer
	*/
	// following string: "entering geographical objects"
	protected String mouseMeaningText = res.getString("entering_geographical");
	/**
	* Current meanings of map events. The layer builder remembers them in
	* order to restore after the measuring finishes
	*/
	protected String clickMeaning = null, moveMeaning = null, dblClickMeaning = null;
	/**
	* Coordinates of the last point entered
	*/
	protected float px = Float.NaN, py = Float.NaN;
	/**
	* Screen mouse coordinates from the last mouse operation
	*/
	protected int x0 = Integer.MIN_VALUE, y0 = x0;
	/**
	* The list of all points. Used to redraw the line when the map is zoomed or
	* repainted
	*/
	protected Vector points = null;
	/**
	* The label showing the total number of objects entered by the user
	*/
	protected Label objNLabel = null;
	/**
	* The labels showing the current x- and y-coordinates of the mouse
	*/
	protected Label xL = null, yL = null;
	/**
	* Used to construct unique identifiers of instances
	*/
	protected static int instanceN = 0;
	/**
	* An internal counter used to avoid duplicate object identifiers
	*/
	protected int count = 0;
	/**
	* The frame with instruction. Is present on the screen while the tool is
	* running. In order to avoid starting of several copies of the tool, the
	* reference to the frame is static. Each new copy checks if the frame exists.
	* If so, closes it. This makes the previous copy of the tool finish its work.
	*/
	protected static OKFrame okf = null;

	/**
	* Returns true when the tool has everything necessary for its operation.
	* A LayerBuilder does not need any additional classes and therefore always
	* returns true.
	*/
	@Override
	public boolean isValid(ESDACore core) {
		return true;
	}

	/**
	* This method constructs and starts the tool. Everything the tool may need
	* for integration with other components of the system can be received
	* from the system's core passed as an argument.
	*/
	@Override
	public void run(ESDACore core) {
		++instanceN;
		//if the previous copy of LayerBuilder is still running (okf is not null),
		//close the okf - this makes the previous copy stop its work
		if (okf != null) {
			okf.close();
			okf = null;
		}
		this.core = core;
		//possibly, the user wishes to edit an existing layer
		LayerManager lman = null;
		if (core.getUI() != null && core.getUI().getCurrentMapViewer() != null) {
			lman = core.getDataKeeper().getMap(core.getUI().getCurrentMapN());
		}
		Vector layers = null;
		if (lman != null) {
			layers = new Vector(lman.getLayerCount(), 5);
			for (int i = 0; i < lman.getLayerCount(); i++)
				if (lman.getGeoLayer(i) instanceof DGeoLayer) {
					DGeoLayer gl = (DGeoLayer) lman.getGeoLayer(i);
					if (gl.getThematicData() != null && !(gl.getThematicData() instanceof DataTable)) {
						continue;
					}
					if (gl.getType() == Geometry.point || gl.getType() == Geometry.line || gl.getType() == Geometry.area) {
						layers.addElement(gl);
					}
				}
		}
		if (layers != null && layers.size() > 0) {
			Panel p = new Panel(new ColumnLayout());
			// following string: "Edit layer:"
			p.add(new Label(res.getString("Edit_layer_")));
			CheckboxGroup cbg = new CheckboxGroup();
			Checkbox cb[] = new Checkbox[layers.size()];
			for (int i = 0; i < cb.length; i++) {
				DGeoLayer gl = (DGeoLayer) layers.elementAt(i);
				String txt = null;
				// following string: " (points)"
				if (gl.getType() == Geometry.point) {
					txt = gl.getName() + res.getString("_points_");
				} else
				// following string: " (lines)"
				if (gl.getType() == Geometry.line) {
					txt = gl.getName() + res.getString("_lines_");
				} else {
					txt = gl.getName() + res.getString("_areas_");
				}
				cb[i] = new Checkbox(txt, cbg, false);
				p.add(cb[i]);
			}
			p.add(new Line(false));
			// following string: "Add a new layer"
			Checkbox addLayer = new Checkbox(res.getString("Add_a_new_layer"), cbg, true);
			p.add(addLayer);
			// following string: "Layer to edit"
			OKDialog dia = new OKDialog(CManager.getAnyFrame(), res.getString("Layer_to_edit"), true);
			dia.addContent(p);
			dia.show();
			if (dia.wasCancelled())
				return;
			if (!addLayer.getState()) {
				for (int i = 0; i < cb.length && layer == null; i++)
					if (cb[i].getState()) {
						layer = (DGeoLayer) layers.elementAt(i);
						if (layer.getThematicData() != null) {
							table = (DataTable) layer.getThematicData();
						} else {
							table = new DataTable();
							table.setName(layer.getName());
							for (int j = 0; j < layer.getObjectCount(); j++) {
								DGeoObject obj = layer.getObject(j);
								DataRecord rec = new DataRecord(obj.getIdentifier(), obj.getLabel());
								table.addDataRecord(rec);
							}
							int tableN = core.getDataLoader().addTable(table);
							core.getDataLoader().setLink(layer, tableN);
						}
					}
			}
		}
		if (layer == null) { //construction of a new layer
			//Asks the user about the type of the objects in the layer
			Panel p = new Panel(new ColumnLayout());
			Panel pp = new Panel(new BorderLayout());
			// following string: "Layer name:"
			pp.add(new Label(res.getString("Layer_name_")), "West");
			TextField tf = new TextField();
			pp.add(tf, "Center");
			p.add(pp);
			// following string: "The type of objects in the layer:"
			p.add(new Label(res.getString("The_type_of_objects")));
			CheckboxGroup cbg = new CheckboxGroup();
			// following string: "point"
			Checkbox cbPoint = new Checkbox(res.getString("point"), cbg, true),
			// following string: "line"
			cbLine = new Checkbox(res.getString("line"), cbg, false),
			// following string: "area"
			cbArea = new Checkbox(res.getString("area"), cbg, false);
			p.add(cbPoint);
			p.add(cbLine);
			p.add(cbArea);
			RealRectangle extent = null;
			if (lman != null && (lman instanceof DLayerManager)) {
				DLayerManager dlm = (DLayerManager) lman;
				extent = dlm.getWholeTerritoryBounds();
				if (extent == null) {
					extent = dlm.getWholeTerritoryBounds();
				}
			}
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
			// following string:
			OKDialog dia = new OKDialog(CManager.getAnyFrame(), res.getString("Layer_type"), true);
			dia.addContent(p);
			dia.show();
			if (dia.wasCancelled())
				return;
			layer = new DGeoLayer();
			if (cbPoint.getState()) {
				layer.setType(Geometry.point);
			} else if (cbLine.getState()) {
				layer.setType(Geometry.line);
			} else {
				layer.setType(Geometry.area);
			}
			layer.setName(tf.getText().trim());
			if (layer.getName().length() < 1) {
				layer.setName("USER LAYER");
			}
			layer.setHasAllObjects(true);
			layer.getDrawingParameters().drawLabels = true;
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
			layer.setWholeLayerBounds(extent);
			core.getDataLoader().addMapLayer(layer, -1);
		}
		count = layer.getObjectCount();
		MapViewer mview = core.getUI().getCurrentMapViewer();
		if (mview == null) {
			reportError(res.getString("No_map_found!"), core);
			return;
		}
		mapN = core.getUI().getCurrentMapN();
		map = mview.getMapDrawer();
		if (map == null) {
			reportError(res.getString("No_map_found!"), core);
			return;
		}
		emm = map.getEventMeaningManager();
		if (emm == null) {
			reportError("The map has no event meaning manager!", core);
			return;
		}

		//Construct a window in which an instruction for the user will be shown.
		TextCanvas instr = new TextCanvas();
		if (layer.getType() == Geometry.point) {
			// following string: "Click in the map on the locations of the point "+"objects you wish to create."
			instr.addTextLine(res.getString("Click_in_the_map_on") + res.getString("objects_you_wish_to"));
		} else {
			// following string: "Click in the map to enter the first point. "+"Move the mouse over the map and click to enter further points."
			instr.addTextLine(res.getString("Click_in_the_map_to") + res.getString("Move_the_mouse_over1"));
			// following string: "Pressing the right mouse button removes the last "+ "entered point."
			instr.addTextLine(res.getString("Pressing_the_right") + res.getString("entered_point_"));
			// following string: "Double-click finishes entering the line."
			instr.addTextLine(res.getString("Double_click_finishes"));
			if (layer.getType() == Geometry.area) {
				// following string: "The contour will be automatically closed."
				instr.addTextLine(res.getString("The_contour_will_be"));
			}
		}
		// following string: "Clicking on an existing object will "+"allow you to remove it or to edit its attribute values."
		instr.addTextLine(res.getString("Clicking_on_an") + res.getString("allow_you_to_remove"));
		Panel p = new Panel(new BorderLayout());
		// following string: "Construct or edit the layer \""
		p.add(new Label(res.getString("Construct_or_edit_the") + layer.getName() + "\"", Label.CENTER), "North");
		p.add(instr, "Center");
		Panel pp = new Panel(new BorderLayout());
		// following string: "Total number of objects in the layer:"
		pp.add(new Label(res.getString("Total_number_of")), "West");
		objNLabel = new Label(String.valueOf(layer.getObjectCount()));
		pp.add(objNLabel, "Center");
		Panel p1 = new Panel(new GridLayout(2, 1));
		p1.add(pp);
		pp = new Panel(new GridLayout(1, 2));
		xL = new Label("x=");
		pp.add(xL);
		yL = new Label("y=");
		pp.add(yL);
		p1.add(pp);
		p.add(p1, "South");
		// following string: "Editing or construction of a layer"
		okf = new OKFrame(this, res.getString("Editing_or"), false);
		okf.addContent(p);
		okf.start();
		core.getWindowManager().registerWindow(okf);
		//Change the current meaning of map events
		emm.addEventMeaning(DMouseEvent.mClicked, mouseMeaningId, mouseMeaningText);
		clickMeaning = emm.getCurrentEventMeaning(DMouseEvent.mClicked);
		emm.setCurrentEventMeaning(DMouseEvent.mClicked, mouseMeaningId);
		emm.addEventMeaning(DMouseEvent.mMove, mouseMeaningId, mouseMeaningText);
		moveMeaning = emm.getCurrentEventMeaning(DMouseEvent.mMove);
		emm.setCurrentEventMeaning(DMouseEvent.mMove, mouseMeaningId);
		emm.addEventMeaning(DMouseEvent.mDClicked, mouseMeaningId, mouseMeaningText);
		dblClickMeaning = emm.getCurrentEventMeaning(DMouseEvent.mDClicked);
		emm.setCurrentEventMeaning(DMouseEvent.mDClicked, mouseMeaningId);
		//start listening to mouse events from the map
		map.addMapListener(this);
		//start listening to map zooming and redrawing events
		map.addPropertyChangeListener(this);
		//initialize internal variables
		px = Float.NaN;
		py = Float.NaN;
		if (points != null) {
			points.removeAllElements();
		}
	}

	/**
	* Displays an error message in the system's status line
	*/
	protected void reportError(String text, ESDACore core) {
		if (text == null || core.getUI() == null)
			return;
		core.getUI().showMessage(text, true);
	}

	/**
	* Reacts to closing the window in which the instruction is shown: stops its
	* work.
	*/
	@Override
	public void actionPerformed(ActionEvent e) {
		if ((e.getSource() instanceof OKFrame) && e.getActionCommand().equals("closed")) {
			okf = null;
			stop();
		}
	}

	/**
	* Reacts to closing the window in which the instruction is shown.
	* Finishes layer construction and stops listening to map events.
	* Restores previous meanings of mouse events.
	*/
	public void stop() {
		px = py = Float.NaN;
		x0 = y0 = Integer.MIN_VALUE;
		if (map == null)
			return;
		map.removeMapListener(this);
		emm.setCurrentEventMeaning(DMouseEvent.mClicked, clickMeaning);
		emm.setCurrentEventMeaning(DMouseEvent.mMove, moveMeaning);
		emm.setCurrentEventMeaning(DMouseEvent.mDClicked, dblClickMeaning);
		map.removePropertyChangeListener(this);
		if (points != null && points.size() > 0) {
			map.redraw();
			points = null;
		}
	}

	/**
	* Stops consuming mouse events from the map
	*/
	protected void finishEnterLine() {
		if (layer.getType() == Geometry.point)
			return;
		boolean area = layer.getType() == Geometry.area;
		if (points == null || points.size() < 2 || (area && points.size() < 3)) {
			if (points != null) {
				points.removeAllElements();
			}
			px = py = Float.NaN;
			x0 = y0 = Integer.MIN_VALUE;
			map.redraw();
			return;
		}
		RealPolyline rp = new RealPolyline();
		rp.isClosed = area;
		int np = points.size();
		if (area) {
			++np;
		}
		rp.p = new RealPoint[np];
		for (int i = 0; i < points.size(); i++) {
			rp.p[i] = (RealPoint) points.elementAt(i);
		}
		if (area) {
			rp.p[np - 1] = rp.p[0];
		}
		addObject(rp);
	}

	/**
	* Draws a line fragment over the map in XOR mode
	*/
	protected void drawLine(int x1, int y1, int x2, int y2) {
		Graphics g = map.getGraphics();
		if (g == null)
			return;
		g.setXORMode(Color.gray);
		g.setColor(Color.red);
		g.drawLine(x1, y1, x2, y2);
		g.setPaintMode();
	}

	/**
	* Restores the whole line entered by the user after map redrawing or
	* zooming.
	*/
	protected void restoreLine() {
		if (points == null || points.size() < 2)
			return;
		Graphics g = map.getGraphics();
		if (g == null)
			return;
		g.setXORMode(Color.gray);
		g.setColor(Color.red);
		RealPoint p = (RealPoint) points.elementAt(0);
		int x = map.getMapContext().scrX(p.x, p.y), y = map.getMapContext().scrY(p.x, p.y);
		for (int i = 1; i < points.size(); i++) {
			p = (RealPoint) points.elementAt(i);
			int x1 = map.getMapContext().scrX(p.x, p.y), y1 = map.getMapContext().scrY(p.x, p.y);
			g.drawLine(x, y, x1, y1);
			x = x1;
			y = y1;
		}
		g.setPaintMode();
		x0 = x;
		y0 = y;
	}

//--------------- EventConsumer interface ----------------------------------
	/**
	* The Event Consumer answers whether it consumes the specified event
	* when it has the given meaning. The DistanceMeasurer consumes mouse click,
	* mouse move, and mouse double click events
	*/
	@Override
	public boolean doesConsumeEvent(String evtType, String evtMeaning) {
		return evtType != null && evtMeaning != null && evtMeaning.equals(mouseMeaningId) && (evtType.equals(DMouseEvent.mClicked) || evtType.equals(DMouseEvent.mMove) || evtType.equals(DMouseEvent.mDClicked));
	}

	/**
	* The Event Consumer answers whether it consumes the specified event
	* when it has the given meaning. The DistanceMeasurer consumes mouse click,
	* mouse move, and mouse double click events
	*/
	@Override
	public boolean doesConsumeEvent(DEvent evt, String evtMeaning) {
		return evt != null && evt.getSource().equals(map) && doesConsumeEvent(evt.getId(), evtMeaning);
	}

	/**
	* The EventReceiver answers whether it is interested in getting the specified
	* kind of events. Besides the consumed events, the DistanceMeasurer is
	* interested to get mouse exit events
	*/
	@Override
	public boolean doesListenToEvent(String evtType) {
		return evtType.equals(DMouseEvent.mClicked) || evtType.equals(DMouseEvent.mMove) || evtType.equals(DMouseEvent.mDClicked) || evtType.equals(DMouseEvent.mExited);
	}

	/**
	* This method is used for delivering events to the Event Receiver.
	*/
	@Override
	public void eventOccurred(DEvent evt) {
		if (evt == null)
			return;
		if (!(evt instanceof DMouseEvent))
			return;
		DMouseEvent me = (DMouseEvent) evt;
		if (me.getId().equals(DMouseEvent.mExited)) {
			//erase the last line fragment
			if (layer.getType() != Geometry.point && !Float.isNaN(px) && !Float.isNaN(py) && x0 != Integer.MIN_VALUE && y0 != Integer.MIN_VALUE) {
				int xx = map.getMapContext().scrX(px, py), yy = map.getMapContext().scrY(px, py);
				drawLine(xx, yy, x0, y0);
			}
			x0 = Integer.MIN_VALUE;
			y0 = Integer.MIN_VALUE;
			return;
		}
		float x = map.getMapContext().absX(me.getX());
		float y = map.getMapContext().absY(me.getY());
		xL.setText("x=" + x);
		yL.setText("y=" + y);
		if (layer.getType() != Geometry.point && !Float.isNaN(px) && !Float.isNaN(py)) {
			int xx = map.getMapContext().scrX(px, py), yy = map.getMapContext().scrY(px, py);
			if (x0 != Integer.MIN_VALUE && y0 != Integer.MIN_VALUE && (x0 != xx || y0 != yy)) {
				drawLine(xx, yy, x0, y0); //erase the last line
			}
			if (me.getId().equals(DMouseEvent.mClicked) && me.getRightButtonPressed()) {
				//erase the last point
				points.removeElementAt(points.size() - 1);
				if (points.size() > 0) {
					RealPoint rp = (RealPoint) points.elementAt(points.size() - 1);
					int xx0 = map.getMapContext().scrX(rp.x, rp.y), yy0 = map.getMapContext().scrY(rp.x, rp.y);
					drawLine(xx0, yy0, xx, yy);
					drawLine(xx0, yy0, me.getX(), me.getY());
					px = rp.x;
					py = rp.y;
				} else {
					px = py = Float.NaN;
				}
			} else {
				drawLine(xx, yy, me.getX(), me.getY());
			}
		}
		x0 = me.getX();
		y0 = me.getY();
		if (me.getId().equals(DMouseEvent.mClicked) && !me.getRightButtonPressed()) {
			if (layer.getType() == Geometry.point || Float.isNaN(px) || Float.isNaN(py)) {
				//probably, the user clicked in an existing object
				Vector objects = layer.findObjectsAt(x0, y0, map.getMapContext(), false);
				if (objects != null && objects.size() > 0 && askEditOrRemove(objects, x0, y0))
					return;
			}
			RealPoint p = new RealPoint();
			p.x = x;
			p.y = y;
			if (layer.getType() == Geometry.point) {
				Graphics g = map.getGraphics();
				if (g != null) {
					g.setColor(Color.yellow);
					g.drawLine(x0 - 5, y0 - 5, x0 + 5, y0 + 5);
					g.drawLine(x0 - 5, y0 + 5, x0 + 5, y0 - 5);
				}
				addObject(p);
			} else {
				px = x;
				py = y;
				if (points == null) {
					points = new Vector(20, 10);
				}
				points.addElement(p);
			}
		} else if (me.getId().equals(DMouseEvent.mDClicked)) {
			finishEnterLine();
		}
	}

	/**
	* When the user clicked on existing object(s), asks if he/she wishes to
	* edit or remove them. Returns true if this was the user's intention.
	*/
	protected boolean askEditOrRemove(Vector objIds, int x, int y) {
		if (objIds == null || objIds.size() < 1)
			return false;
		Panel p = new Panel(new ColumnLayout());
		Checkbox cb[] = null;
		if (objIds.size() > 1) {
			cb = new Checkbox[objIds.size()];
		}
		for (int i = 0; i < objIds.size(); i++) {
			DGeoObject obj = (DGeoObject) layer.findObjectById((String) objIds.elementAt(i));
			String name = obj.getLabel();
			if (name == null) {
				name = obj.getIdentifier();
			}
			if (objIds.size() < 2) {
				p.add(new Label(name));
			} else {
				cb[i] = new Checkbox(name);
				p.add(cb[i]);
			}
		}
		CheckboxGroup cbg = new CheckboxGroup();
		// following string: "edit"
		Checkbox editCB = new Checkbox(res.getString("edit"), cbg, true);
		// following string: "remove"
		Checkbox removeCB = new Checkbox(res.getString("remove"), cbg, false);
		Panel pp = new Panel(new GridLayout(1, 2));
		pp.add(editCB);
		pp.add(removeCB);
		p.add(pp);
		// following string: "Edit or remove"
		OKDialog okd = new OKDialog(CManager.getAnyFrame(), res.getString("Edit_or_remove"), true);
		okd.addContent(p);
		Point pt = null;
		if (map instanceof Component) {
			pt = ((Component) map).getLocationOnScreen();
		} else {
			pt = new Point(0, 0);
		}
		pt.x += x;
		pt.y += y;
		okd.show(pt);
		if (okd.wasCancelled())
			return false;
		Vector toEdit = new Vector(objIds.size());
		if (cb == null) {
			toEdit.addElement(objIds.elementAt(0));
		} else {
			for (int i = 0; i < cb.length; i++)
				if (cb[i].getState()) {
					toEdit.addElement(objIds.elementAt(i));
				}
		}
		if (toEdit.size() < 1)
			return false;
		if (removeCB.getState()) {
			boolean changed = false;
			for (int i = 0; i < toEdit.size(); i++) {
				int idx = table.getObjectIndex((String) toEdit.elementAt(i));
				if (idx >= 0) {
					table.removeDataItem(idx);
					changed = true;
				}
				idx = layer.getObjectIndex((String) toEdit.elementAt(i));
				if (idx >= 0) {
					layer.removeGeoObject(idx);
				}
			}
			if (changed) {
				table.notifyPropertyChange("data_removed", null, null);
			}
			objNLabel.setText(String.valueOf(layer.getObjectCount()));
			return true;
		}
		ObjectDataEditor ded = new ObjectDataEditor();
		ded.setMayEditId(false);
		for (int i = 0; i < table.getAttrCount(); i++) {
			ded.addAttribute(table.getAttributeName(i), table.getAttributeId(i), table.getAttributeType(i));
		}
		ded.finishConstruction(false);
		okd = new OKDialog(CManager.getAnyFrame(), null, true);
		okd.addContent(ded);
		boolean labelChanged = false, rowsAdded = false;
		Vector attrs = new Vector(table.getAttrCount(), 1);
		for (int i = 0; i < toEdit.size(); i++) {
			int idx = table.getObjectIndex((String) toEdit.elementAt(i));
			DataRecord rec = null;
			if (idx < 0) { //there was no record in the table yet
				rec = new DataRecord((String) toEdit.elementAt(i));
				table.addDataRecord(rec);
				idx = table.getDataItemCount() - 1;
				DGeoObject obj = (DGeoObject) layer.findObjectById(rec.getId());
				obj.setThematicData(rec);
				rowsAdded = true;
			} else {
				rec = table.getDataRecord(idx);
			}
			ded.setObject(rec.getId(), rec.getName());
			for (int j = 0; j < table.getAttrCount(); j++) {
				ded.setAttrValue(j, rec.getAttrValueAsString(j));
			}
			String name = rec.getName();
			if (name == null) {
				name = rec.getId();
			}
			// following string: "Edit data about object "
			okd.setTitle(res.getString("Edit_data_about") + name);
			okd.show(pt);
			if (!okd.wasCancelled()) {
				name = ded.getObjectName();
				if (!StringUtil.sameStrings(name, rec.getName())) {
					labelChanged = true;
					rec.setName(name);
					DGeoObject obj = (DGeoObject) layer.findObjectById(rec.getId());
					if (obj != null) {
						obj.setLabel(name);
					}
				}
				for (int j = 0; j < table.getAttrCount(); j++)
					if (!StringUtil.sameStrings(ded.getAttrValue(j), rec.getAttrValueAsString(j))) {
						rec.setAttrValue(ded.getAttrValue(j), j);
						String attrId = table.getAttributeId(j);
						if (!attrs.contains(attrId)) {
							attrs.addElement(attrId);
						}
					}
			}
		}
		if (rowsAdded) {
			table.notifyPropertyChange("data_added", null, null);
		}
		if (labelChanged) {
			if (layer.getDrawingParameters().drawLabels) {
				layer.notifyPropertyChange("Labels", null, null);
			}
			table.notifyPropertyChange("names", null, null);
		}
		if (attrs.size() > 0) {
			table.notifyPropertyChange("values", null, attrs);
		}
		return true;
	}

	/**
	* Finds a suitable location for the dialog related to the given geometry
	*/
	protected Point getDialogLocation(Geometry geom) {
		if (geom == null || map == null)
			return null;
		if (map instanceof Component) {
			float bounds[] = geom.getBoundRect();
			if (bounds != null) {
				Point p = new Point();
				p.x = map.getMapContext().scrX(bounds[2], bounds[3]);
				p.y = map.getMapContext().scrY(bounds[0], bounds[1]);
				Point p1 = ((Component) map).getLocationOnScreen();
				p.x += p1.x;
				p.y += p1.y;
				return p;
			}
		}
		return null;
	}

	/**
	* Displays a dialog that asks the user about the identifier and the name of
	* a new geographical object and, possibly, its attributes. Adds the object to
	* the layer.
	*/
	protected void addObject(Geometry geom) {
		if (geom == null)
			return;
		String id = IdMaker.makeId(String.valueOf(++count), this);
		ObjectDataEditor ded = new ObjectDataEditor();
		ded.setMayEditId(true);
		ded.setObject(id, null);
		if (table != null) {
			for (int i = 0; i < table.getAttrCount(); i++) {
				ded.addAttribute(table.getAttributeName(i), table.getAttributeId(i), table.getAttributeType(i));
			}
		}
		ded.finishConstruction(true);
		int nAttrOld = (table == null) ? 0 : table.getAttrCount(), nAttr = nAttrOld;
		// following string: "Object information"
		OKDialog dia = new OKDialog(CManager.getAnyFrame(), res.getString("Object_information"), true);
		dia.addContent(ded);
		dia.show(getDialogLocation(geom));
		Vector newAttrs = null;
		if (!dia.wasCancelled()) {
			boolean newTable = table == null;
			if (newTable) {
				table = new DataTable();
				table.setName(layer.getName());
			}
			nAttr = ded.getAttrCount();
			if (nAttr > nAttrOld) {
				newAttrs = new Vector(nAttr - nAttrOld, 1);
				for (int i = nAttrOld; i < nAttr; i++) {
					AttrDescr ad = ded.getAttrDescr(i);
					table.addAttribute(ad.name, ad.id, ad.type);
					newAttrs.addElement(table.getAttributeId(i));
				}
				table.notifyPropertyChange("new_attributes", null, newAttrs);
			}
			String id1 = ded.getObjectId();
			if (id1 != null) {
				id = IdMaker.makeId(id1, this);
			}
			String name = ded.getObjectName();
			DataRecord dr = new DataRecord(id, name);
			for (int i = 0; i < nAttr; i++) {
				dr.addAttrValue(ded.getAttrValue(i));
			}
			table.addDataRecord(dr);
			table.notifyPropertyChange("data_added", null, null);
			SpatialEntity spe = new SpatialEntity(id, name);
			spe.setGeometry(geom);
			spe.setThematicData(dr);
			DGeoObject obj = new DGeoObject();
			obj.setup(spe);
			if (name != null) {
				obj.setLabel(name);
			}
			layer.addGeoObject(obj);
			if (newTable) {
				int tableN = core.getDataLoader().addTable(table);
				//int layerIdx=core.getDataLoader().addMapLayer(layer,mapN);
				core.getDataLoader().setLink(layer, tableN);
			}
			objNLabel.setText(String.valueOf(layer.getObjectCount()));
		}
		if (points != null) {
			points.removeAllElements();
		}
		px = py = Float.NaN;
		x0 = y0 = Integer.MIN_VALUE;
		if (dia.wasCancelled()) {
			map.redraw();
		}
		if (nAttr > nAttrOld && layer.getObjectCount() > 1) {
			//The user has added new attributes. Hence, data about previously entered
			//objects must be completed.
			ded.setMayAddAttributes(false);
			ded.setMayEditId(false);
			for (int i = 0; i < layer.getObjectCount() - 1; i++) {
				DataRecord rec = table.getDataRecord(i);
				ded.setObject(rec.getId(), rec.getName());
				for (int j = 0; j < nAttrOld; j++) {
					ded.setAttrValue(j, rec.getAttrValueAsString(j));
				}
				for (int j = nAttrOld; j < nAttr; j++) {
					ded.setAttrValue(j, null);
				}
				String name = rec.getName();
				if (name == null) {
					name = rec.getId();
				}
				// following string: "Complete data about object "
				dia.setTitle(res.getString("Complete_data_about") + name);
				DGeoObject obj = layer.getObject(i);
				Geometry g = obj.getGeometry();
				dia.show(getDialogLocation(g));
				if (!dia.wasCancelled()) {
					name = ded.getObjectName();
					rec.setName(name);
					obj.setLabel(name);
					for (int j = 0; j < nAttr; j++) {
						rec.setAttrValue(ded.getAttrValue(j), j);
					}
				}
			}
			table.notifyPropertyChange("values", null, newAttrs);
		}
	}

	@Override
	public boolean isIdentifierUsed(String ident) {
		return layer.findObjectById(ident) != null;
	}

//--------------------- PropertyChangeListener interface ---------------------
	/**
	* Reacts to map repainting, zooming, etc. In such cases must restore the
	* line entered by the user.
	*/
	@Override
	public void propertyChange(PropertyChangeEvent e) {
		if (e.getSource().equals(map)) {
			restoreLine();
		}
	}

	/**
	* Returns a unique identifier of the event receiver (may be produced
	* automatically, used only internally, not shown to the user).
	*/
	@Override
	public String getIdentifier() {
		return "layer_builder_" + instanceN;
	}
}
