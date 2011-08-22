package spade.analysis.tools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Rectangle;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ResourceBundle;

import spade.analysis.system.ESDACore;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Destroyable;
import spade.lib.basicwin.OKFrame;
import spade.lib.lang.Language;
import spade.vis.event.DEvent;
import spade.vis.event.DMouseEvent;
import spade.vis.event.EventConsumer;
import spade.vis.event.EventMeaningManager;
import spade.vis.geometry.RealPoint;
import spade.vis.map.MapDraw;
import spade.vis.map.MapViewer;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: 02-Nov-2007
 * Time: 14:54:41
 * Allows a user to create a point (e.g. a new point object) on the map
 */
public class EnterPointOnMapUI implements ActionListener, PropertyChangeListener, EventConsumer, Destroyable {
	static ResourceBundle res = Language.getTextResource("spade.analysis.tools.Res");
	/**
	* The system's core providing access to all its components
	*/
	protected ESDACore core = null;
	/**
	 * The prompt text to be shown in the window
	 */
	public String prompt = "Enter a point on the map";
	/**
	 * The title of the window
	 */
	public String winTitle = "Enter a point";
	/**
	* The map in which the point is to be entered
	*/
	protected MapDraw map = null;
	/**
	* The number of the map in which the point is to be entered
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
	protected String mouseMeaningId = "make_point";
	/**
	* The full text of the meaning assigned to mouse events on a map by the
	* distance measurer
	*/
	protected String mouseMeaningText = "make a point";
	/**
	* Current meanings of map events. The layer builder remembers them in
	* order to restore after the measuring finishes
	*/
	protected String clickMeaning = null;
	/**
	* Coordinates of the last point entered
	*/
	protected float px = Float.NaN, py = Float.NaN;
	/**
	* Screen mouse coordinates from the last mouse operation
	*/
	protected int x0 = Integer.MIN_VALUE, y0 = x0;
	/**
	* Contain the current x- and y-coordinates of the mouse
	*/
	protected TextField xTF = null, yTF = null;
	/**
	* The frame with instruction. Is present on the screen while the tool is
	* running. In order to avoid starting of several copies of the tool, the
	* reference to the frame is static. Each new copy checks if the frame exists.
	* If so, closes it. This makes the previous copy of the tool finish its work.
	*/
	protected static OKFrame okf = null;
	/**
	* Used to construct unique identifiers of instances
	*/
	protected static int instanceN = 0;
	/**
	 * The owner must be notified when the process is finished
	 */
	protected ActionListener owner = null;

	protected boolean destroyed = false;

	/**
	 * The owner must be notified when the process is finished
	 */
	public void setOwner(ActionListener owner) {
		this.owner = owner;
	}

	/**
	 * Sets the title of the window
	 */
	public void setWindowTitle(String winTitle) {
		this.winTitle = winTitle;
	}

	/**
	 * Sets the prompt text to be shown in the window
	 */
	public void setPromptText(String prompt) {
		this.prompt = prompt;
	}

	/**
	* This method constructs and starts the tool. Everything the tool may need
	* for integration with other components of the system can be received
	* from the system's core passed as an argument.
	*/
	public void run(ESDACore core) {
		++instanceN;
		//if the previous copy of the tool is still running (okf is not null),
		//close the okf - this makes the previous copy stop its work
		if (okf != null) {
			okf.close();
			okf = null;
		}
		this.core = core;
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
		Panel p = new Panel(new ColumnLayout());
		p.add(new Label(prompt, Label.CENTER));
		Panel pp = new Panel(new BorderLayout());
		pp.add(new Label("X = "), BorderLayout.WEST);
		xTF = new TextField(10);
		xTF.setEditable(false);
		pp.add(xTF, BorderLayout.CENTER);
		p.add(pp);
		pp = new Panel(new BorderLayout());
		pp.add(new Label("Y = "), BorderLayout.WEST);
		yTF = new TextField(10);
		yTF.setEditable(false);
		pp.add(yTF, BorderLayout.CENTER);
		p.add(pp);
		okf = new OKFrame(this, winTitle, false);
		okf.addContent(p);
		okf.start();
		Frame fr = core.getUI().getMainFrame();
		if (fr != null) {
			Rectangle bf = fr.getBounds();
			Dimension ds = fr.getToolkit().getScreenSize(), d = okf.getSize();
			int x0 = bf.x + bf.width, y0 = bf.y + bf.height - d.height;
			if (x0 + d.width > ds.width) {
				x0 = ds.width - d.width;
			}
			okf.setLocation(x0, y0);
		}
		core.getWindowManager().registerWindow(okf);
		//Change the current meaning of map events
		emm.addEventMeaning(DMouseEvent.mClicked, mouseMeaningId, mouseMeaningText);
		clickMeaning = emm.getCurrentEventMeaning(DMouseEvent.mClicked);
		emm.setCurrentEventMeaning(DMouseEvent.mClicked, mouseMeaningId);
		//start listening to mouse events from the map
		map.addMapListener(this);
		//start listening to map zooming and redrawing events
		map.addPropertyChangeListener(this);
		//initialize internal variables
		px = Float.NaN;
		py = Float.NaN;
	}

	public RealPoint getPoint() {
		if (Float.isNaN(px) || Float.isNaN(py))
			return null;
		return new RealPoint(px, py);
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
	* Reacts to map repainting, zooming, etc. In such cases must restore the
	* line entered by the user.
	*/
	@Override
	public void propertyChange(PropertyChangeEvent e) {
		if (e.getSource().equals(map)) {
			restorePoint();
		}
	}

	/**
	* Restores the whole line entered by the user after map redrawing or
	* zooming.
	*/
	public void restorePoint() {
		Graphics g = map.getGraphics();
		if (g == null)
			return;
		if (Float.isNaN(px) || Float.isNaN(py))
			return;
		//draw a symbol (e.g. a cross)
		g.setColor(Color.yellow);
		spade.lib.basicwin.Drawing.drawLine(g, 3, x0 - 5, y0 - 5, x0 + 5, y0 + 5, true, true);
		spade.lib.basicwin.Drawing.drawLine(g, 3, x0 - 5, y0 + 5, x0 + 5, y0 - 5, true, true);
		g.setColor(Color.black);
		g.drawLine(x0 - 5, y0 - 5, x0 + 5, y0 + 5);
		g.drawLine(x0 - 5, y0 + 5, x0 + 5, y0 - 5);
	}

	public void erasePoint() {
		if (!Float.isNaN(px) && !Float.isNaN(py)) {
			map.redraw();
		}
	}

	/**
	* Reacts to closing the window in which the instruction is shown.
	* Finishes layer construction and stops listening to map events.
	* Restores previous meanings of mouse events.
	*/
	public void stop() {
		destroy();
		if (owner != null) {
			owner.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "make_point"));
		}
	}

	@Override
	public void destroy() {
		if (destroyed)
			return;
		if (okf != null) {
			okf.dispose();
			okf = null;
		}
		destroyed = true;
		if (map == null)
			return;
		map.removeMapListener(this);
		emm.setCurrentEventMeaning(DMouseEvent.mClicked, clickMeaning);
		map.removePropertyChangeListener(this);
		if (!Float.isNaN(px) && !Float.isNaN(py)) {
			map.redraw();
		}
	}

	@Override
	public boolean isDestroyed() {
		return destroyed;
	}

	/**
	* Displays an error message in the system's status line
	*/
	protected void reportError(String text, ESDACore core) {
		if (text == null || core.getUI() == null)
			return;
		core.getUI().showMessage(text, true);
	}

//--------------- EventConsumer interface ----------------------------------
	/**
	* The Event Consumer answers whether it consumes the specified event
	* when it has the given meaning. The tool consumes only mouse click events.
	*/
	@Override
	public boolean doesConsumeEvent(String evtType, String evtMeaning) {
		return evtType != null && evtMeaning != null && evtMeaning.equals(mouseMeaningId) && evtType.equals(DMouseEvent.mClicked);
	}

	/**
	* The Event Consumer answers whether it consumes the specified event
	* when it has the given meaning. The tool consumes only mouse click events.
	*/
	@Override
	public boolean doesConsumeEvent(DEvent evt, String evtMeaning) {
		return evt != null && evt.getSource().equals(map) && doesConsumeEvent(evt.getId(), evtMeaning);
	}

	/**
	* The EventReceiver answers whether it is interested in getting the specified
	* kind of events. Besides the consumed events, the tool is
	* interested to get mouse move and mouse exit events
	*/
	@Override
	public boolean doesListenToEvent(String evtType) {
		return evtType.equals(DMouseEvent.mClicked) || evtType.equals(DMouseEvent.mMove) || evtType.equals(DMouseEvent.mExited);
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
			xTF.setText("");
			yTF.setText("");
			return;
		}
		float x = map.getMapContext().absX(me.getX());
		float y = map.getMapContext().absY(me.getY());
		xTF.setText(String.valueOf(x));
		yTF.setText(String.valueOf(y));
		if (me.getId().equals(DMouseEvent.mClicked) && !me.getRightButtonPressed()) {
			if (!Float.isNaN(px) && !Float.isNaN(py)) {
				px = py = Float.NaN;
				map.redraw();
			}
			px = x;
			py = y;
			x0 = me.getX();
			y0 = me.getY();
			restorePoint();
		}
	}

	/**
	* Returns a unique identifier of the event receiver (may be produced
	* automatically, used only internally, not shown to the user).
	*/
	@Override
	public String getIdentifier() {
		return "point_maker_" + instanceN;
	}
}
