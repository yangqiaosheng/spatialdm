package spade.analysis.tools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.system.ESDACore;
import spade.lib.basicwin.OKFrame;
import spade.lib.basicwin.TextCanvas;
import spade.lib.lang.Language;
import spade.lib.util.GeoDistance;
import spade.vis.event.DEvent;
import spade.vis.event.DMouseEvent;
import spade.vis.event.EventConsumer;
import spade.vis.event.EventMeaningManager;
import spade.vis.geometry.RealPoint;
import spade.vis.map.MapDraw;
import spade.vis.map.MapViewer;

/**
* The component used for measuring distances on the map. Implemented mostly
* as an example of how a component of Descartes may interact with a map.
* A similar interaction would take place in editing map layers, building
* profiles, etc.
*/
public class DistanceMeasurer implements DataAnalyser, ActionListener, EventConsumer, PropertyChangeListener {
	static ResourceBundle res = Language.getTextResource("spade.analysis.tools.Res");
	/**
	* The map in which the distance is measured
	*/
	protected MapDraw map = null;
	/**
	* The manager of meanings of mouse events on the map
	*/
	protected EventMeaningManager emm = null;
	/**
	* The labels which show the distance
	*/
	protected Label lastDistL = null, totalDistL = null, xL = null, yL = null;
	/**
	* The total distance
	*/
	protected double totalDist = 0.0f;
	/**
	* The identifier of the meaning assigned to mouse events on a map by the
	* distance measurer  */

	protected String mouseMeaningId = "distance";
	/**
	* The full text of the meaning assigned to mouse events on a map by the
	* distance measurer
	*/

	// following string:"distance measurement"
	protected String mouseMeaningText = res.getString("distance_measurement");
	/**
	* Current meanings of map events. The distance measurer remembers them in
	* order to restore after the measuring finishes
	*/
	protected String clickMeaning = null, moveMeaning = null, dblClickMeaning = null;
	/**
	* Coordinates of the last point entered
	*/
	protected float px = Float.NaN, py = Float.NaN;
	/**
	 * Indicates whether the coordinates are geographical
	 */
	protected boolean geo = false;
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
	* Indicate whether the measurement has finished
	*/
	protected boolean finished = false;
	/**
	* Used to construct unique identifiers of instances
	*/
	protected static int instanceN = 0;

	/**
	* Returns true when the tool has everything necessary for its operation.
	* A DistanceMeasurer does not need any additional class and therefore always
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
		//get the map canvas
		if (core == null || core.getUI() == null || core.getUI().getCurrentMapN() < 0) {
			// following string:
			reportError(res.getString("No_map_found!"), core);
			return;
		}
		MapViewer mview = core.getUI().getMapViewer(core.getUI().getCurrentMapN());

		if (mview == null) {
			reportError(res.getString("No_map_found!"), core);
			return;
		}
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
		geo = core.getDataKeeper().getMap(0).isGeographic();
		//Construct a window in which the measured distance will be shown.
		//The window contains also an instruction for the user.
		TextCanvas instr = new TextCanvas();
		// following string: "Click in the map to enter the first point. "+ "Move the mouse over the map and click to mark intermediate points."
		instr.addTextLine(res.getString("Click_in_the_map_to") + res.getString("Move_the_mouse_over"));
		// following string: "Double-click to finish drawing the line."
		instr.addTextLine(res.getString("Double_click_to"));
		Panel p = new Panel(new BorderLayout());
		p.add(instr, "Center");
		Panel pp = new Panel(new GridLayout(2, 2));
		// following string: "Distance from last point:"
		pp.add(new Label(res.getString("Distance_from_last")));
		lastDistL = new Label("0");
		pp.add(lastDistL);
		// following string: "Total distance:"
		pp.add(new Label(res.getString("Total_distance_")));
		totalDistL = new Label("0");
		pp.add(totalDistL);
		p.add(pp, "South");
		pp = new Panel(new GridLayout(1, 2));
		xL = new Label("x=");
		pp.add(xL);
		yL = new Label("y=");
		pp.add(yL);
		p.add(pp, "North");
		// following string: "Distance measuring"
		OKFrame okf = new OKFrame(this, res.getString("Distance_measuring"), false);
		okf.addContent(p);
		okf.start();
		//The window must be properly registered in order to be closed in a case
		//when the aplication is closed or changed.
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
		totalDist = 0.0;
		finished = false;
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
	* Reacts to closing the window in which the instruction and the measured
	* distance are shown. Erases the line that was drawn and stops listening
	* to map redrawing and zooming
	*/
	@Override
	public void actionPerformed(ActionEvent e) {
		if ((e.getSource() instanceof OKFrame) && e.getActionCommand().equals("closed")) {
			stop();
			if (map == null)
				return;
			map.removePropertyChangeListener(this);
			if (points != null && points.size() > 0) {
				map.redraw();
				points = null;
			}
		}
	}

	/**
	* Stops consuming mouse events from the map
	*/
	protected void stop() {
		if (finished)
			return;
		finished = true;
		if (map == null)
			return;
		map.removeMapListener(this);
		emm.setCurrentEventMeaning(DMouseEvent.mClicked, clickMeaning);
		emm.setCurrentEventMeaning(DMouseEvent.mMove, moveMeaning);
		emm.setCurrentEventMeaning(DMouseEvent.mDClicked, dblClickMeaning);
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
		if (finished)
			return false;
		return evtType.equals(DMouseEvent.mClicked) || evtType.equals(DMouseEvent.mMove) || evtType.equals(DMouseEvent.mDClicked) || evtType.equals(DMouseEvent.mExited);
	}

	/**
	* This method is used for delivering events to the Event Receiver.
	*/
	@Override
	public void eventOccurred(DEvent evt) {
		if (evt == null || finished)
			return;
		if (!(evt instanceof DMouseEvent))
			return;
		DMouseEvent me = (DMouseEvent) evt;
		if (me.getId().equals(DMouseEvent.mExited)) {
			//erase the last line fragment
			if (!Float.isNaN(px) && !Float.isNaN(py) && x0 != Integer.MIN_VALUE && y0 != Integer.MIN_VALUE) {
				int xx = map.getMapContext().scrX(px, py), yy = map.getMapContext().scrY(px, py);
				drawLine(xx, yy, x0, y0);
				x0 = Integer.MIN_VALUE;
				y0 = Integer.MIN_VALUE;
			}
			return;
		}
		float x = map.getMapContext().absX(me.getX());
		float y = map.getMapContext().absY(me.getY());
		xL.setText("x=" + x);
		yL.setText("y=" + y);
		double d = 0.0f;
		if (!Float.isNaN(px) && !Float.isNaN(py)) {
			int xx = map.getMapContext().scrX(px, py), yy = map.getMapContext().scrY(px, py);
			if (x0 != Integer.MIN_VALUE && y0 != Integer.MIN_VALUE && (x0 != xx || y0 != yy)) {
				drawLine(xx, yy, x0, y0); //erase the last line
			}
			drawLine(xx, yy, me.getX(), me.getY());
			if (geo) {
				d = GeoDistance.geoDist(x, y, px, py);
			} else {
				d = Math.sqrt((x - px) * (x - px) + (y - py) * (y - py));
			}
			lastDistL.setText(String.valueOf(d));
			totalDistL.setText(String.valueOf(totalDist + d));
		}
		x0 = me.getX();
		y0 = me.getY();
		if (me.getId().equals(DMouseEvent.mClicked)) {
			totalDist += d;
			px = x;
			py = y;
			if (points == null) {
				points = new Vector(20, 10);
			}
			RealPoint p = new RealPoint();
			p.x = px;
			p.y = py;
			points.addElement(p);
		} else if (me.getId().equals(DMouseEvent.mDClicked)) {
			stop();
		}
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
		return "distance_measurer_" + instanceN;
	}
}
