package connection.zeno;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Point;
import java.awt.ScrollPane;
import java.awt.TextField;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.system.ESDACore;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.OKDialog;
import spade.lib.basicwin.TextCanvas;
import spade.lib.lang.Language;
import spade.lib.util.CopyFile;
import spade.lib.util.IdMaker;
import spade.lib.util.IdentifierUseChecker;
import spade.lib.util.IntArray;
import spade.lib.util.StringUtil;
import spade.vis.action.Highlighter;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.database.SpatialEntity;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;
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
import spade.vis.spec.DataPipeSpec;
import spade.vis.spec.DataSourceSpec;
import data_update.DataUpdater;

/**
* Listens to a specified URL, from which data may come (e.g. from Zeno). These
* data describe objects having no georeference yet. The task of the
* DataPipeListener is to ask the user to put the objects on the map. The
* objects are added to the geographical layer that must be specified in the data.
*/
public class DataPipeListener implements WindowListener, ActionListener, IdentifierUseChecker, EventConsumer, PropertyChangeListener {
	static ResourceBundle res = Language.getTextResource("connection.zeno.Res");
	/**
	* Possible states of a DataPipeListener: inactive, idle, checking for new
	* entries, georeferencing, storing new objects, or destroyed (e.g. when the
	* corresponding map layer has been destroyed).
	*/
	public static final int IDLE = 0, PIPE_CHECK = 1, GEOREFERENCE = 2, STORE = 3, INACTIVE = -1, DESTROYED = -100;
	/**
	* The current state of the Listener (equals to one of the above-defined
	* constants).
	*/
	protected int status = INACTIVE;
	/**
	* The datapipe specification: the URL to be listened, where to store the data,
	* etc.
	*/
	protected DataPipeSpec dataPipeSpec = null;
	/**
	* If the URL is a file, this is the time of its last modification
	*/
	protected long fileLastModified = -1;
	/**
	* The layer in which to add the new objects
	*/
	protected DGeoLayer layer = null;
	/**
	* The table in which to add data about the new objects
	*/
	protected DataTable table = null;
	/**
	* The data loaded from the URL (data pipe). Each element of the vector is,
	* in its turn, a vector consisting of attribute-value pairs (two-string
	* arrays). New entries are added to the end of the vector. The entries that
	* have already been georeferenced are removed from the vector (and added to
	* the corresponding table and layer)
	*/
	protected Vector data = null;
	/**
	* The system's core
	*/
	protected ESDACore core = null;
	/**
	* The map in which the layer is constructed
	*/
	protected MapDraw map = null;
	/**
	* The number of the map
	*/
	protected int mapN = -1;
	/**
	* The identifier of the meaning assigned to mouse events on a map by the
	* layer builder
	*/
	protected String mouseMeaningId = "build";
	/**
	* The full text of the meaning assigned to mouse events on a map by the
	* distance measurer
	*/
	protected String mouseMeaningText = res.getString("entering_geographical");//"entering geographical objects"
	/**
	* Current meanings of map events. The layer builder remembers them in
	* order to restore after the measuring finishes
	*/
	protected String clickMeaning = null, moveMeaning = null, dblClickMeaning = null;
	/**
	* Shows whether the component is currently listening to map events
	*/
	protected boolean listensToMap = false;
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
	* The labels showing the current x- and y-coordinates of the mouse
	*/
	protected Label xL = null, yL = null;
	/**
	* Used to construct unique identifiers of instances
	*/
	protected static int instanceN = 0;
	/**
	* The frame with current data and instruction. It is present on the screen
	* while the user enters geographical objects. If during this time one more
	* copy of DataPipeListener tries to do the same, it must immediately stop the
	* attempt.
	*/
	protected Frame dataFrame = null;
	/**
	* The panel in which the user views and edits attribute values
	*/
	protected ObjectDataPanel objDataPanel = null;
	/**
	* The spatial entities created by the user
	*/
	protected Vector entities = null;

	/**
	* Initializes the internal variables of the DataPipeListener. Prepares to
	* checking of the specified URL (passed through the argument dpSpec)
	* for appearance of new data. The checking actually takes place in the method
	* lookForNewEntries().
	*/
	public DataPipeListener(DataPipeSpec dpSpec, //specifies the URL to listen
			DGeoLayer geoLayer, //the layer to be extended with new objects
			int mapIdx, //index of the map the layer belongs to
			ESDACore core) //provides access to all system's tools
	{
		this.core = core;
		dataPipeSpec = dpSpec;
		layer = geoLayer;
		mapN = mapIdx;
		if (dataPipeSpec == null || dataPipeSpec.dataSource == null || layer == null)
			return;
		if (layer.getThematicData() != null && (layer.getThematicData() instanceof DataTable)) {
			table = (DataTable) layer.getThematicData();
		}
		layer.addPropertyChangeListener(this); //listen to "destroy" events
		if (table != null) {
			table.addPropertyChangeListener(this); //listen to "destroy" events
		}
		status = INACTIVE;
	}

	/**
	* Returns the current state of the listener: inactive, idle, checking the for
	*  newentries, georeferencing, storing new objects, or destroyed (e.g. when
	* the corresponding map layer has been destroyed). The result is one of the
	* constants IDLE=0, PIPE_CHECK=1, GEOREFERENCE=2, STORE=3, INACTIVE=-1,
	* DESTROYED=-100.
	*/
	public int getStatus() {
		return status;
	}

	/**
	* Replies whether the listener is active or not. The listener is active if
	* its state is not INACTIVE or DESTROYED.
	*/
	public boolean isActive() {
		return status != INACTIVE && status != DESTROYED;
	}

	/**
	* Activates or deactivates the listener. When the listener is deactivated,
	* its status is INACTIVE. A listener can only be deactivated when its current
	* status is IDLE.
	*/
	public void activate(boolean value) {
		if (value)
			if (status == INACTIVE) {
				status = IDLE;
			} else {
				;
			}
		else if (status == IDLE) {
			status = INACTIVE;
		}
	}

	/**
	* Returns its datapipe specification
	*/
	public DataPipeSpec getDataPipeSpecification() {
		return dataPipeSpec;
	}

	/**
	* Returns the layer this datapipe is attached to
	*/
	public DGeoLayer getLayer() {
		return layer;
	}

	/**
	* Returns the number of entries that have not been georeferenced yet
	*/
	public int getPendingEntriesCount() {
		if (data == null)
			return 0;
		return data.size();
	}

	/**
	* Returns the number of new objects that have not been stored yet
	*/
	public int getNewObjectsCount() {
		if (entities == null)
			return 0;
		return entities.size();
	}

	/**
	* Asks the user to georeference the data collected, i.e. to enter the
	* geographical objects the data will refer to.
	*/
	public void startGeoreferencing() {
		core.getUI().showMessage(null, false);
		if (data == null || data.size() < 1)
			return;
		status = GEOREFERENCE;
		if (mapN < 0) {
			mapN = core.getUI().getCurrentMapN();
		}
		if (map == null) {
			MapViewer mview = core.getUI().getMapViewer(mapN);
			if (mview != null) {
				map = mview.getMapDrawer();
			}
		}
		if (map == null) {
			core.getUI().showMessage(res.getString("No_map_found_"), true);
			status = IDLE;
			return;
		}
		EventMeaningManager emm = map.getEventMeaningManager();
		if (emm == null) {
			// following string: "The map has no event meaning manager!"
			core.getUI().showMessage(res.getString("The_map_has_no_event"), true);
			status = IDLE;
			return;
		}
		processFirstDataEntry();
	}

	/**
	* Asks the user to georeference the first data entry in the vector of
	* collected data, i.e. to enter the geographical object the data entry will
	* refer to. After the user enters the object, the object must be added to the
	* corresponding map layer, and the data must be added to the corresponding table.
	* Then the first entry is removed from the data, and the procedure is repeated
	* for the next data entry.
	* However, it is impossible to wait within this method while the user
	* finishes entering the object. Therefore this method only creates a window
	* with the current data entry (the user may edit attribute values) and quits.
	* Click in the map (for point objects) or double-click (for line and area
	* objects) signals the end of the process of entering an object.
	*/
	protected void processFirstDataEntry() {
		if (data == null || data.size() < 1)
			return;
		status = GEOREFERENCE;
		Vector entry = (Vector) data.elementAt(0);
		String id = null, name = null;
		int idN = -1, nameN = -1;
		for (int i = 0; i < entry.size(); i++) {
			String pair[] = (String[]) entry.elementAt(i);
			if (pair[0].equalsIgnoreCase("identifier") || pair[0].equalsIgnoreCase("ident") || pair[0].equalsIgnoreCase("id")) {
				id = pair[1];
				idN = i;
			} else if (pair[0].equalsIgnoreCase("name")) {
				name = pair[1];
				nameN = i;
			}
		}
		int count = (table == null) ? 1 : table.getDataItemCount() + 1;
		if (id == null) {
			id = String.valueOf(count);
		} else if (name == null) {
			name = id;
		}
		if (name == null) {
			name = "Entry " + String.valueOf(count);
		}
		objDataPanel = new ObjectDataPanel();
		objDataPanel.addAttrValuePair("identifier", id);
		objDataPanel.addAttrValuePair("name", name);
		if (table != null && table.getAttrCount() > 0) {
			for (int i = 0; i < table.getAttrCount(); i++) {
				String attr = table.getAttributeName(i), value = null;
				if (attr.equalsIgnoreCase("X") || attr.equalsIgnoreCase("Y")) {
					continue;
				}
				for (int j = 0; j < entry.size() && value == null; j++) {
					String pair[] = (String[]) entry.elementAt(j);
					if (pair[0].equalsIgnoreCase(attr)) {
						value = pair[1];
						break;
					}
				}
				objDataPanel.addAttrValuePair(attr, value);
			}
		}
		for (int i = 0; i < entry.size(); i++)
			if (i != idN && i != nameN) {
				String pair[] = (String[]) entry.elementAt(i);
				if (!objDataPanel.hasAttribute(pair[0])) {
					objDataPanel.addAttrValuePair(pair[0], pair[1]);
				}
			}
		Panel p = new Panel(new BorderLayout());
		// following string: "Create a geographical object associated with the data:"
		p.add(new Label(res.getString("Create_a_geographical")), "North");
		if (objDataPanel.getAttributeCount() <= 8) {
			p.add(objDataPanel, "Center");
		} else {
			ScrollPane scp = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
			scp.add(objDataPanel);
			p.add(scp, "Center");
		}
		Panel pp = new Panel(new ColumnLayout());
		p.add(pp, "South");
		//Construct the instruction for the user.
		TextCanvas instr = new TextCanvas();
		instr.setBackground(Color.getHSBColor(0.4f, 0.3f, 1.0f));
		if (layer.getType() == Geometry.point) {
			// following string: "Click in the map on the location of the object."
			instr.addTextLine(res.getString("Click_in_the_map_on"));
		} else {
			// following string: "Click in the map to enter the first point. "+
			// "Move the mouse over the map and click to enter further points."
			instr.addTextLine(res.getString("Click_in_the_map_to") + res.getString("Move_the_mouse_over"));
			// following string: "Pressing the right mouse button removes the last entered point."
			instr.addTextLine(res.getString("Pressing_the_right") + res.getString("entered_point_"));
			// following string: "Double-click finishes entering the line."
			instr.addTextLine(res.getString("Double_click_finishes"));
			if (layer.getType() == Geometry.area) {
				// following string: "The contour will be automatically closed."
				instr.addTextLine(res.getString("The_contour_will_be"));
			}
		}
		pp.add(instr);
		GridBagLayout gbLayout = new GridBagLayout();
		Panel p1 = new Panel(gbLayout);
		GridBagConstraints c = new GridBagConstraints();
		c.weighty = 1.0f;
		c.fill = GridBagConstraints.HORIZONTAL;
		Label l = new Label("X:");
		c.gridwidth = 1;
		c.weightx = 0.0f;
		gbLayout.setConstraints(l, c);
		p1.add(l);
		xL = new Label("");
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 1.0f;
		gbLayout.setConstraints(xL, c);
		p1.add(xL);
		l = new Label("Y:");
		c.gridwidth = 1;
		c.weightx = 0.0f;
		gbLayout.setConstraints(l, c);
		p1.add(l);
		yL = new Label("");
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 1.0f;
		gbLayout.setConstraints(yL, c);
		p1.add(yL);
		pp.add(p1);
		// following string:"Enter location":"Construct geographical object"
		dataFrame = new Frame((layer.getType() == Geometry.point) ? res.getString("Enter_location") : res.getString("Construct"));
		dataFrame.setLayout(new BorderLayout());
		dataFrame.add(p, "Center");
		p = new Panel(new FlowLayout(FlowLayout.CENTER, 0, 3));
		Button b = new Button("Cancel");
		b.setActionCommand("cancel");
		b.addActionListener(this);
		p.add(b);
		dataFrame.add(p, "South");
		dataFrame.pack();
		Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension sz = dataFrame.getSize();
		dataFrame.setLocation((d.width - sz.width) / 2, (d.height - sz.height) / 2);
		dataFrame.show();
		core.getWindowManager().registerWindow(dataFrame);
		dataFrame.addWindowListener(this);
		if (!listensToMap) {
			EventMeaningManager emm = map.getEventMeaningManager();
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
			listensToMap = true;
		}
		//initialize internal variables
		px = Float.NaN;
		py = Float.NaN;
		if (points != null) {
			points.removeAllElements();
		}
	}

	@Override
	public boolean isIdentifierUsed(String ident) {
		return layer != null && layer.findObjectById(ident) != null;
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("cancel") && dataFrame != null) {
			stop();
			dataFrame.dispose();
			dataFrame = null;
			status = IDLE;
		}
	}

	/**
	* Adds new entries to its collection of entries that has not been
	* georeferenced yet.
	*/
	public void addNewData(Vector entries) {
		if (entries == null || entries.size() < 1)
			return;
		if (data == null) {
			data = new Vector(20, 10);
		}
		for (int i = 0; i < entries.size(); i++) {
			data.addElement(entries.elementAt(i));
		}
	}

	public void windowActivated(WindowEvent e) {
	}

	public void windowClosing(WindowEvent e) {
	}

	public void windowOpened(WindowEvent e) {
	}

	public void windowClosed(WindowEvent e) {
		if (e.getSource().equals(dataFrame) && listensToMap) {
			stop();
			dataFrame = null;
			status = IDLE;
		}
	}

	public void windowIconified(WindowEvent e) {
	}

	public void windowDeiconified(WindowEvent e) {
	}

	public void windowDeactivated(WindowEvent e) {
	}

//--------------- EventConsumer interface ----------------------------------
	/**
	* The Event Consumer answers whether it consumes the specified event
	* when it has the given meaning. The DataPipeListener consumes mouse click,
	* mouse move, and mouse double click events
	*/
	public boolean doesConsumeEvent(String evtType, String evtMeaning) {
		return evtType != null && evtMeaning != null && evtMeaning.equals(mouseMeaningId) && (evtType.equals(DMouseEvent.mClicked) || evtType.equals(DMouseEvent.mMove) || evtType.equals(DMouseEvent.mDClicked));
	}

	/**
	* The Event Consumer answers whether it consumes the specified event
	* when it has the given meaning. The DistanceMeasurer consumes mouse click,
	* mouse move, and mouse double click events
	*/
	public boolean doesConsumeEvent(DEvent evt, String evtMeaning) {
		return evt != null && evt.getSource().equals(map) && doesConsumeEvent(evt.getId(), evtMeaning);
	}

	/**
	* The EventReceiver answers whether it is interested in getting the specified
	* kind of events. Besides the consumed events, the DistanceMeasurer is
	* interested to get mouse exit events
	*/
	public boolean doesListenToEvent(String evtType) {
		return evtType.equals(DMouseEvent.mClicked) || evtType.equals(DMouseEvent.mMove) || evtType.equals(DMouseEvent.mDClicked) || evtType.equals(DMouseEvent.mExited);
	}

	/**
	* This method is used for delivering events to the Event Receiver.
	*/
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
				if (objects != null && objects.size() > 0 && askDataEdit(objects))
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

	/**
	* After the user finished entering the location or contour of the geographical
	* object, add the object to the layer.
	*/
	protected void addObject(Geometry geom) {
		if (geom == null || dataFrame == null)
			return;
		stopMapListening();
		dataFrame.dispose();
		dataFrame = null;
		/*
		OKDialog okd=new OKDialog(CManager.getAnyFrame(),"Check object data",false);
		okd.addContent(objDataPanel);
		okd.show(getDialogLocation(geom));
		*/
		boolean newTable = table == null;
		if (newTable) {
			table = new DataTable();
			table.setName(layer.getName());
			table.addPropertyChangeListener(this); //listen to "destroy" events
		}
		String id = null, name = null;
		Vector newAttrs = null;
		int nattr = objDataPanel.getAttributeCount();
		IntArray attrNumbers = new IntArray(nattr, 1);
		for (int i = 0; i < nattr; i++) {
			String attrName = objDataPanel.getAttributeName(i), value = objDataPanel.getAttributeValue(i);
			if (attrName.equalsIgnoreCase("identifier")) {
				id = value;
				attrNumbers.addElement(-1);
			} else if (attrName.equalsIgnoreCase("name")) {
				name = value;
				attrNumbers.addElement(-1);
			} else {
				int idx = table.getAttrIndex(attrName); //find the attribute in the table
				if (idx < 0) {
					for (int j = 0; j < table.getAttrCount() && idx < 0; j++)
						if (attrName.equalsIgnoreCase(table.getAttributeName(j))) {
							idx = j;
						}
				}
				if (idx < 0) { //this is a new attribute
					table.addAttribute(attrName, AttributeTypes.character);
					idx = table.getAttrCount() - 1;
					if (newAttrs == null) {
						newAttrs = new Vector(nattr, 1);
					}
					newAttrs.addElement(table.getAttributeName(idx));
				}
				attrNumbers.addElement(idx);
			}
		}
		if (newAttrs != null && newAttrs.size() > 0) {
			table.notifyPropertyChange("new_attributes", null, newAttrs);
		}
		id = IdMaker.makeId(id, this);
		DataRecord dr = new DataRecord(id, name);
		for (int i = 0; i < table.getAttrCount(); i++) {
			dr.addAttrValue(null);
		}
		for (int i = 0; i < nattr; i++)
			if (attrNumbers.elementAt(i) >= 0) {
				String val = objDataPanel.getAttributeValue(i);
				//if this is a field with some URLs, ensure that these are full URLs
				//rather than paths relative to the location of the applet
				if (val != null && objDataPanel.getAttributeName(i).equalsIgnoreCase("url")) {
					Vector urls = StringUtil.getNames(val, ";");
					if (urls != null && urls.size() > 0) {
						val = "";
						for (int j = 0; j < urls.size(); j++) {
							String fullURL = core.getFullURLString((String) urls.elementAt(j));
							val += (val.length() > 0) ? ";" + fullURL : fullURL;
						}
					}
				}
				dr.setAttrValue(val, attrNumbers.elementAt(i));
			}
		if (geom instanceof RealPoint) {
			int ix = table.findAttrByName("X"), iy = table.findAttrByName("Y");
			if (ix >= 0 && iy >= 0) {
				RealPoint p = (RealPoint) geom;
				dr.setAttrValue(String.valueOf(p.x), ix);
				dr.setAttrValue(String.valueOf(p.y), iy);
			}
		}
		table.addDataRecord(dr);
		table.notifyPropertyChange("data_added", null, null);
		SpatialEntity spe = new SpatialEntity(id, name);
		spe.setGeometry(geom);
		spe.setThematicData(dr);
		if (entities == null) {
			entities = new Vector(20, 10);
		}
		entities.addElement(spe);
		DGeoObject obj = new DGeoObject();
		obj.setup(spe);
		if (name != null) {
			obj.setLabel(name);
		}
		layer.setHasAllObjects(true);
		layer.addGeoObject(obj);
		if (newTable) {
			int tableN = core.getDataLoader().addTable(table);
			core.getDataLoader().setLink(layer, tableN);
		}
		data.removeElementAt(0);
		reset();
		if (data.size() > 0) {
			processFirstDataEntry();
		} else {
			status = IDLE;
			table.determineAttributeTypes();
		}
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
	* Stops listening to map events. Restores previous meanings of mouse events.
	*/
	public void stopMapListening() {
		if (!listensToMap || map == null)
			return;
		map.removeMapListener(this);
		EventMeaningManager emm = map.getEventMeaningManager();
		emm.setCurrentEventMeaning(DMouseEvent.mClicked, clickMeaning);
		emm.setCurrentEventMeaning(DMouseEvent.mMove, moveMeaning);
		emm.setCurrentEventMeaning(DMouseEvent.mDClicked, dblClickMeaning);
		map.removePropertyChangeListener(this);
		listensToMap = false;
	}

	/**
	* Resets the internal variables used for object construction and redraws the map
	*/
	protected void reset() {
		px = py = Float.NaN;
		x0 = y0 = Integer.MIN_VALUE;
		if (map != null) {
			map.redraw();
		}
		points = null;
	}

	/**
	* Calls stopMapListening() and reset()
	*/
	protected void stop() {
		stopMapListening();
		reset();
	}

	protected void finishEnterLine() {
		if (layer.getType() == Geometry.point)
			return;
		boolean area = layer.getType() == Geometry.area;
		if (points == null || points.size() < 2 || (area && points.size() < 3)) {
			reset();
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

//--------------------- PropertyChangeListener interface ---------------------
	/**
	* Reacts to map repainting, zooming, etc. In such cases must restore the
	* line entered by the user.
	*/
	public void propertyChange(PropertyChangeEvent e) {
		if (e.getSource().equals(map)) {
			restoreLine();
		} else if ((e.getSource().equals(table) || e.getSource().equals(layer)) && e.getPropertyName().equals("destroyed")) {
			stop();
			if (entities != null && entities.size() > 0) {
				Panel p = new Panel(new ColumnLayout());
				p.add(new Label(layer.getName() + ":", Label.CENTER));
				// following string:
				/*"You created "+entities.size()+
				  " new geographical "+((entities.size()<2)?"object":"objects"+
				  " in map layer "+layer.getName());*/
				String txt = res.getString("You_created") + entities.size() + res.getString("new_geographical") + ((entities.size() < 2) ? res.getString("object") : res.getString("objects"));
				p.add(new Label(txt));
				p.add(new Label(res.getString("Store_the_objects_now"), Label.CENTER));
				OKDialog okd = new OKDialog(CManager.getAnyFrame(), res.getString("Store_objects_"), OKDialog.YES_NO_MODE, true);
				okd.addContent(p);
				okd.show();
				if (!okd.wasCancelled()) {
					storeEntities();
				}
			}
			status = DESTROYED;
		}
	}

	/**
	* Returns a unique identifier of the event receiver (may be produced
	* automatically, used only internally, not shown to the user).
	*/
	public String getIdentifier() {
		return "datapipe_" + instanceN;
	}

	/**
	* Stores the new geographical objects created by the user.
	*/
	public void storeEntities() {
		if (entities == null || entities.size() < 1)
			return;
		status = STORE;
		boolean isApplet = !core.getSystemSettings().checkParameterValue("isLocalSystem", "true");
		//boolean isApplet=true;
		URL storeURL = null;
		if (dataPipeSpec.updater != null) {
			try {
				storeURL = new URL(dataPipeSpec.updater);
			} catch (MalformedURLException mfe) {
				dataPipeSpec.updater = null;
			}
		}
		if (isApplet && storeURL == null) {
			//ask the user about the URL to listen
			URL docBase = null;
			Object obj = core.getSystemSettings().getParameter("DocumentBase");
			if (obj != null && (obj instanceof URL)) {
				docBase = (URL) obj;
			}
			// following string: "Specify the URL of the data storing script:"
			GetURLPanel pan = new GetURLPanel(docBase, storeURL, res.getString("Specify_the_URL_of"));
			// following string: "URL of the data storing script?"
			OKDialog okd = new OKDialog(CManager.getAnyFrame(), res.getString("URL_of_the_data"), true);
			okd.addContent(pan);
			okd.show();
			if (okd.wasCancelled()) {
				status = IDLE;
				return;
			}
			storeURL = pan.getURL();
			System.out.println("Got URL: " + storeURL);
			dataPipeSpec.updater = storeURL.toString();
		}
		if (dataPipeSpec.tableFileName == null)
			//ask the user to enter the path of the file where to store the results
			if (!isApplet) {
				// following string: "Where to store the results?"
				FileDialog getPath = new FileDialog(CManager.getAnyFrame(), res.getString("Where_to_store_the"), FileDialog.SAVE);
				getPath.setFile("*.csv;*.txt");
				String currDir = java.lang.System.getProperty("user.dir");
				if (currDir != null) {
					getPath.setDirectory(currDir);
				}
				getPath.show();
				String file = getPath.getFile(), dir = getPath.getDirectory();
				if (file != null && dir != null) {
					dataPipeSpec.tableFileName = dir + file;
				}
			} else {
				Panel p = new Panel(new ColumnLayout());
				// following string: "In what file (on the server) to store the results?"
				p.add(new Label(res.getString("In_what_file_on_the")));
				TextField tfName = new TextField(40);
				p.add(tfName);
				// following string: "Where to store the results?"
				OKDialog okd = new OKDialog(CManager.getAnyFrame(), res.getString("Where_to_store_the"), true);
				okd.addContent(p);
				okd.show();
				if (okd.wasCancelled()) {
					status = IDLE;
					return;
				}
				dataPipeSpec.tableFileName = tfName.getText().trim();
				if (dataPipeSpec.tableFileName.length() < 1) {
					dataPipeSpec.tableFileName = null;
				}
			}
		if (dataPipeSpec.tableFileName == null) {
			status = IDLE;
			return;
		}
		String geoFileName = null;
		if (layer.getType() != Geometry.point) {
			geoFileName = CopyFile.getNameWithoutExt(dataPipeSpec.tableFileName) + ".ovl";
			String dir = CopyFile.getDir(dataPipeSpec.tableFileName);
			if (dir != null) {
				geoFileName = dir + geoFileName;
			}
		}
		boolean ok = false;
		String errorMsg = null;
		if (dataPipeSpec.delimiter == null) {
			dataPipeSpec.delimiter = ",";
		}
		if (isApplet) {
			try {
				URLConnection con = storeURL.openConnection();
				con.setDoOutput(true);
				OutputStream out = con.getOutputStream();
				ObjectOutputStream objOut = new ObjectOutputStream(out);
				objOut.writeObject("FILENAME=" + dataPipeSpec.tableFileName);
				if (dataPipeSpec.delimiter != null) {
					objOut.writeObject("DELIMITER=" + dataPipeSpec.delimiter);
				}
				if (layer.getType() != Geometry.point) {
					objOut.writeObject("GEOFILENAME=" + geoFileName);
				}
				for (int i = 0; i < entities.size(); i++) {
					objOut.writeObject(entities.elementAt(i));
				}
				try {
					out.close();
				} catch (IOException ioe) {
				}
				InputStream in = con.getInputStream();
				BufferedReader reader = new BufferedReader(new InputStreamReader(in));
				while (true) {
					String str = reader.readLine();
					if (str == null) {
						break;
					}
					str = str.trim();
					System.out.println(str);
					if (str.length() > 6 && str.substring(0, 6).equalsIgnoreCase("ERROR:")) {
						errorMsg = str.substring(6).trim();
					} else if (str.length() >= 2 && str.substring(0, 2).equalsIgnoreCase("OK")) {
						ok = true;
					}
				}
			} catch (IOException e) {
				errorMsg = e.toString();
			}
		} else {
			DataUpdater updater = new DataUpdater();
			ok = updater.storeSpatialData(entities, dataPipeSpec.tableFileName, dataPipeSpec.delimiter, geoFileName);
			if (!ok) {
				errorMsg = updater.getErrorMessage();
			}
		}
		if (!ok) {
			core.getUI().showMessage(errorMsg, true);
			System.out.println(errorMsg);
		} else {
			// following string: "The data successfully stored"
			core.getUI().showMessage(res.getString("The_data_successfully"), false);
			entities.removeAllElements();
			DataSourceSpec dss = null;
			if (layer.getDataSource() != null && (layer.getDataSource() instanceof DataSourceSpec)) {
				dss = (DataSourceSpec) layer.getDataSource();
			}
			if (dss == null) {
				dss = new DataSourceSpec();
				layer.setDataSource(dss);
			}
			if (dss.source == null || dss.source.equalsIgnoreCase("_derived"))
				if (geoFileName != null) {
					dss.source = geoFileName;
				} else {
					dss.source = dataPipeSpec.tableFileName;
					dss.delimiter = dataPipeSpec.delimiter;
					dss.idFieldName = "identifier";
					dss.nameFieldName = "name";
					dss.xCoordFieldName = "X";
					dss.yCoordFieldName = "Y";
					dss.nRowWithFieldNames = 0;
				}
			dss = null;
			if (table.getDataSource() != null && (table.getDataSource() instanceof DataSourceSpec)) {
				dss = (DataSourceSpec) table.getDataSource();
			}
			if (dss == null)
				if (geoFileName == null) {
					table.setDataSource(layer.getDataSource());
				} else {
					dss = new DataSourceSpec();
					table.setDataSource(dss);
				}
			if (dss.source == null || dss.source.equalsIgnoreCase("_derived")) {
				dss.source = dataPipeSpec.tableFileName;
				dss.delimiter = dataPipeSpec.delimiter;
				dss.idFieldName = "identifier";
				dss.nameFieldName = "name";
				dss.nRowWithFieldNames = 0;
			}
		}
		status = IDLE;
	}

	/**
	* When the user clicked on existing object(s), asks if he/she wishes to
	* attach the new data to one of these objects. Returns true if this was the
	* user's intention.
	*/
	protected boolean askDataEdit(Vector objIds) {
		if (objIds == null || objIds.size() < 1)
			return false;
		if (!objDataPanel.hasAttribute("url") || objDataPanel.getAttributeValue("url") == null || table.getAttrIndex("url") < 0)
			return false;
		System.out.println(objIds.toString());
		Highlighter hl = core.getHighlighterForSet(layer.getEntitySetIdentifier());
		hl.clearSelection(this);
		Vector singleObj = new Vector(1, 1);
		for (int i = 0; i < objIds.size(); i++) {
			DGeoObject gobj = (DGeoObject) layer.findObjectById((String) objIds.elementAt(i));
			int idx = table.getObjectIndex((String) objIds.elementAt(i));
			if (gobj != null && idx >= 0) {
				singleObj.removeAllElements();
				singleObj.addElement(objIds.elementAt(i));
				hl.replaceSelectedObjects(this, singleObj);
				RealRectangle lr = gobj.getLabelRectangle();
				float rx = (lr.rx1 + lr.rx2) / 2, ry = (lr.ry1 + lr.ry2) / 2;
				int x = map.getMapContext().scrX(rx, ry), y = map.getMapContext().scrY(rx, ry);
				OKDialog okd = new OKDialog(CManager.getAnyFrame(), res.getString("Data_update"), OKDialog.YES_NO_MODE, true);
				// following string:"Attach the URL to this object?"
				okd.addContent(new Label(res.getString("Attach_the_URL_to")));
				Point pt = null;
				if (map instanceof Component) {
					pt = ((Component) map).getLocationOnScreen();
				} else {
					pt = new Point(0, 0);
				}
				pt.x += x;
				pt.y += y;
				okd.show(pt);
				hl.clearSelection(this);
				if (!okd.wasCancelled()) {
					DataRecord rec = table.getDataRecord(idx);
					Vector attr = new Vector(2, 1);
					int aidx = table.getAttrIndex("url");
					String value = rec.getAttrValueAsString(aidx), val1 = objDataPanel.getAttributeValue("url");
					if (value == null) {
						value = "";
					}
					boolean added = false;
					if (value.indexOf(val1) < 0) {
						value += ";" + val1;
						rec.setAttrValue(value, aidx);
						attr.addElement(table.getAttributeName(aidx));
						added = true;
					}
					if (added) {
						aidx = table.getAttrIndex("author");
						val1 = objDataPanel.getAttributeValue("author");
						if (aidx >= 0 && val1 != null) {
							value = rec.getAttrValueAsString(aidx);
							if (value == null) {
								value = "";
							}
							value += ";" + val1;
							rec.setAttrValue(value, aidx);
							attr.addElement(table.getAttributeName(aidx));
						}
					}
					stopMapListening();
					dataFrame.dispose();
					dataFrame = null;
					if (attr.size() > 0 && rec != null) {
						table.notifyPropertyChange("values", null, attr);
						if (entities == null) {
							entities = new Vector(20, 10);
						}
						if (!entities.contains(gobj.getSpatialData())) {
							entities.addElement(rec);
						}
					}
					data.removeElementAt(0);
					reset();
					if (data.size() > 0) {
						processFirstDataEntry();
					} else {
						status = IDLE;
					}
					return true;
				}
			}
		}
		return false;
	}
}