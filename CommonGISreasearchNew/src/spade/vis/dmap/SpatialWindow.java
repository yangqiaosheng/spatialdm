package spade.vis.dmap;

import java.awt.Color;
import java.awt.Graphics;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import spade.lib.basicwin.Drawing;
import spade.vis.database.SpatialConstraintChecker;
import spade.vis.event.DEvent;
import spade.vis.event.DMouseEvent;
import spade.vis.event.EventConsumer;
import spade.vis.event.EventMeaningManager;
import spade.vis.geometry.Geometry;
import spade.vis.map.MapContext;
import spade.vis.map.MapDraw;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Apr 23, 2008
 * Time: 11:53:04 AM
 * Filters spatial objects by fitting into a rectangular spatial area (window).
 */
public class SpatialWindow extends SpatialConstraintChecker implements PropertyChangeListener, EventConsumer {
	/**
	 * Current boundaries of the spatial window
	 */
	public float rx1 = Float.NaN, rx2 = rx1, ry1 = rx1, ry2 = rx1;
	/**
	 * Used for drawing of the rectangle on the screen
	 */
	protected int xe[] = new int[5];
	protected int ye[] = new int[5];
	/**
	 * Whether the window is active, i.e. reacts to mouse events
	 */
	protected boolean active = false;
	/**
	 * Whether the window is visible and works as filter
	 */
	protected boolean visible = true;
	/**
	 * The map in which the window boundaries must be drawn
	 */
	protected MapDraw map = null;
	/**
	 * The color used for drawing the boundaries
	 */
	public Color lineColor = Color.red;
	/**
	 * The thickness of the boundary line, in pixels
	 */
	public int lineWidth = 3;
	/**
	* The manager of meanings of mouse events on the map
	*/
	protected EventMeaningManager emm = null;
	/**
	* The identifier of the meaning assigned to mouse events on a map by the
	* spatial window
	*/
	protected String mouseMeaningId = "filter";
	/**
	* The full text of the meaning assigned to mouse events on a map by the
	* spatial window
	*/
	protected String mouseMeaningText = "filtering by spatial window";
	/**
	* Current meaning of map drag events. The spatial window remembers it in
	* order to restore when deactivated.
	*/
	protected String dragMeaning = null;
	/**
	* Screen mouse coordinates from the last mouse operation
	*/
	protected int x0 = Integer.MIN_VALUE, y0 = x0;
	/**
	* Used to construct unique identifiers of instances
	*/
	protected static int instanceN = 0;

	public SpatialWindow() {
		++instanceN;
	}

	/**
	 * Informs if any spatial constraint is currently set
	 */
	@Override
	public boolean hasConstraint() {
		return visible && !Float.isNaN(rx1);
	}

	/**
	 * Checks if a given geometry satisfies some spatial constraint,
	 * which is defined inside this object or elsewhere
	 */
	@Override
	public boolean doesSatisfySpatialConstraint(Geometry geom) {
		if (geom == null)
			return false;
		if (!visible || Float.isNaN(rx1))
			return true;
		return geom.isInRectangle(rx1, ry1, rx2, ry2);
		//return geom.fitsInRectangle(rx1,ry1,rx2,ry2);
	}

	public boolean isVisible() {
		return visible;
	}

	public void setVisible(boolean visible) {
		if (this.visible == visible)
			return;
		this.visible = visible;
		if (visible) {
			drawWindow();
			//start listening to map zooming and redrawing events
			if (map != null) {
				map.addPropertyChangeListener(this);
			}
			if (active) {
				startMouseListening();
			}
		} else {
			if (map != null) {
				map.removePropertyChangeListener(this);
			}
			if (active) {
				finishMouseListening();
			}
			eraseWindow();
		}
		if (!Float.isNaN(rx1)) {
			notifyConstraintChange();
		}
	}

	/**
	 * Informs whether the window is currently active and visible
	 */
	public boolean isActive() {
		return active;
	}

	/**
	 * Sets whether the window will be active and visible
	 */
	public void setActive(boolean active) {
		if (active == this.active)
			return;
		this.active = active;
		if (!visible)
			return;
		if (active) {
			startMouseListening();
		} else {
			finishMouseListening();
		}
	}

	/**
	 * Starts listening to mouse events from the map and map redrawing events
	 */
	protected void startMouseListening() {
		if (!active || !visible)
			return;
		if (map == null)
			return;
		if (emm == null) {
			emm = map.getEventMeaningManager();
			if (emm == null)
				return;
			emm.addEventMeaning(DMouseEvent.mDrag, mouseMeaningId, mouseMeaningText);
		}
		dragMeaning = emm.getCurrentEventMeaning(DMouseEvent.mDrag);
		emm.setCurrentEventMeaning(DMouseEvent.mDrag, mouseMeaningId);
		//start listening to mouse events from the map
		map.addMapListener(this);
	}

	/**
	 * Finishes listening to mouse events from the map and map redrawing events
	 */
	protected void finishMouseListening() {
		if (map == null)
			return;
		map.removeMapListener(this);
		if (emm == null)
			return;
		emm.setCurrentEventMeaning(DMouseEvent.mDrag, dragMeaning);
	}

	/**
	 * Sets the map in which the window boundaries must be drawn
	 */
	public void setMap(MapDraw map) {
		this.map = map;
		if (map == null)
			return;
		if (visible) {
			//start listening to map zooming and redrawing events
			map.addPropertyChangeListener(this);
		}
		if (active && visible) {
			startMouseListening();
		}
		drawWindow();
	}

	/**
	 * Draws the boundaries of the window
	 */
	public void drawWindow() {
		if (!visible || Float.isNaN(rx1) || map == null)
			return;
		Graphics g = map.getGraphics();
		if (g == null)
			return;
		MapContext mc = map.getMapContext();
		if (mc == null)
			return;
		xe[0] = mc.scrX(rx1, ry1);
		ye[0] = mc.scrY(rx1, ry1);
		xe[1] = mc.scrX(rx1, ry2);
		ye[1] = mc.scrY(rx1, ry2);
		xe[2] = mc.scrX(rx2, ry2);
		ye[2] = mc.scrY(rx2, ry2);
		xe[3] = mc.scrX(rx2, ry1);
		ye[3] = mc.scrY(rx2, ry1);
		xe[4] = xe[0];
		ye[4] = ye[0];
		g.setColor(lineColor);
		Drawing.drawPolyline(g, lineWidth, xe, ye, 5, true);
	}

	public void eraseWindow() {
		if (map != null && !Float.isNaN(rx1)) {
			map.redraw();
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent e) {
		if (e.getSource().equals(map)) {
			drawWindow();
		}
	}

	/**
	 * Stops listening to map zooming and panning events
	 */
	public void destroy() {
		if (map != null) {
			map.removePropertyChangeListener(this);
		}
		finishMouseListening();
		if (map != null && visible && !Float.isNaN(rx1)) {
			map.redraw();
		}
		visible = active = false;
		notifyDestroy();
	}

//--------------- EventConsumer interface ----------------------------------
	/**
	* The Event Consumer answers whether it consumes the specified event
	* when it has the given meaning. The SpaceWindow consumes mouse drag events.
	*/
	@Override
	public boolean doesConsumeEvent(String evtType, String evtMeaning) {
		return evtType != null && evtMeaning != null && evtMeaning.equals(mouseMeaningId) && evtType.equals(DMouseEvent.mDrag);
	}

	/**
	* The Event Consumer answers whether it consumes the specified event
	* when it has the given meaning. The DistanceMeasurer consumes mouse drag events.
	*/
	@Override
	public boolean doesConsumeEvent(DEvent evt, String evtMeaning) {
		return evt != null && evt.getSource().equals(map) && doesConsumeEvent(evt.getId(), evtMeaning);
	}

	/**
	* The EventReceiver answers whether it is interested in getting the specified
	* kind of events.
	*/
	@Override
	public boolean doesListenToEvent(String evtType) {
		return evtType.equals(DMouseEvent.mDrag);
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
		DMouseEvent mevt = (DMouseEvent) evt;
		if (!mevt.getId().equals(DMouseEvent.mDrag))
			return;
		int x0 = mevt.getDragStartX(), y0 = mevt.getDragStartY(), x = map.correctMouseX(mevt.getX()), y = map.correctMouseY(mevt.getY()), prx = mevt.getDragPrevX(), pry = mevt.getDragPrevY();
		//erase previous frame
		if (prx >= 0 && pry >= 0) {
			drawFrame(x0, y0, prx, pry);
		}
		if (mevt.isDraggingFinished()) {
			MapContext mc = map.getMapContext();
			if (mc == null)
				return;
			if (x0 > x) {
				int k = x;
				x = x0;
				x0 = k;
			}
			if (y0 > y) {
				int k = y;
				y = y0;
				y0 = k;
			}
			rx1 = mc.absX(x0);
			rx2 = mc.absX(x);
			ry1 = mc.absY(y);
			ry2 = mc.absY(y0);
			map.redraw();
			notifyConstraintChange();
		} else {
			drawFrame(x0, y0, x, y);
		}
	}

	protected void drawFrame(int x0, int y0, int x, int y) {
		if (x - x0 != 0 || y - y0 != 0) {
			Graphics gr = map.getGraphics();
			if (gr == null)
				return;
			gr.setColor(Color.yellow);
			gr.setXORMode(lineColor);
			gr.drawLine(x0, y0, x, y0);
			gr.drawLine(x, y0, x, y);
			gr.drawLine(x, y, x0, y);
			gr.drawLine(x0, y, x0, y0);
			gr.setPaintMode();
			gr.dispose();
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
