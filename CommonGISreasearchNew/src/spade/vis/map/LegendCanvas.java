package spade.vis.map;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.ScrollPane;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeEvent;
import java.util.Vector;

import spade.vis.event.DMouseEvent;
import spade.vis.event.EventManager;
import spade.vis.event.EventReceiver;

/**
* Although this class is intended primarily to draw map legends, it
* can be used for drawing anything in a Canvas.
* A LegendCanvas registers any items implementing the interface LegendDrawer.
* In the method "paint" the LegendCanvas makes its LegendDrawers
* paint themselves in the canvas one below another in the order they come
* in the list. The LegendCanvas sends the LegendDrawers the recommended
* width (equal to the current width of the canvas). This allows to
* avoid horizontal scrolling.
* A LegendCanvas listens to changes of properties of LegendDrawers that
* require legend redrawing. After legend redrawing its size may change.
* If a LegendCanvas is inserted into a ScrollPane, it forces the ScrollPane
* to redo the layout when the size of the total legend changes. When the
* size of the container changes, the LegendCanvas adjusts itself to the new size.
*/
public class LegendCanvas extends Canvas implements Legend, ComponentListener, MouseListener, MouseMotionListener {
	/**
	* Vector of legend drawers.
	*/
	protected Vector legDrawers = null;
	/**
	* Current preferred width and height.
	*/
	protected int w = 0, h = 0;
	/**
	* Left margin
	*/
	protected int leftMarg = 5;
	/**
	* Used to count preferred sizes.
	*/
	protected Image offScreenImage = null;
	/**
	* EventManager helps the LegendCanvas in distributing mouse events among
	* possible listeners.
	*/
	protected EventManager evtMan = null;

	@Override
	public void addLegendDrawer(LegendDrawer ldr) {
		if (ldr == null)
			return;
		if (legDrawers == null) {
			legDrawers = new Vector(5, 5);
		}
		if (!legDrawers.contains(ldr)) {
			legDrawers.addElement(ldr);
		}
		manageChanges();
	}

	@Override
	public void removeLegendDrawer(LegendDrawer ldr) {
		if (ldr == null || legDrawers == null)
			return;
		int idx = legDrawers.indexOf(ldr);
		if (idx >= 0) {
			legDrawers.removeElementAt(idx);
		}
		if (legDrawers.size() == 0) {
			removeMouseListener(this);
		}
		manageChanges();
	}

	public void setLeftMargin(int width) {
		leftMarg = width;
	}

	/**
	* Fired when properties of some of the LegendDrawers have changed so that
	* the legend should be repainted. The LegendCanvas counts the new sizes
	* needed for its drawing. If the sizes changed, invalidates itself
	* and calls the "validate" method of its container. In any case repaints.
	*/
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		manageChanges();
	}

	public void manageChanges() {
		//if (!isShowing()) return;
		int sw = w, sh = h;
		countSizes();
		if (sw != w || sh != h) {
			setSize(w, h);
			invalidate();
			if (getParent() != null) {
				getParent().validate();
			}
		}
		repaint();
	}

	@Override
	public void print(Graphics g) {
		Dimension d = getSize();
		Image imgOffScreen = createImage(d.width, d.height);
		if (imgOffScreen != null) {
			Graphics gimg = imgOffScreen.getGraphics();
			gimg.setColor(Color.white);
			gimg.fillRect(0, 0, d.width + 1, d.height + 1);
			draw(gimg, d.width);
			g.drawImage(imgOffScreen, 0, 0, null);
		} else {
			draw(g, d.width);
		}
	}

	@Override
	public void paint(Graphics g) {
		Dimension d = getSize();
		draw(g, d.width - 4);
	}

	@Override
	public Graphics getLegendGraphics() {
		return getGraphics();
	}

	/**
	* Calls the methods drawLegend of all its LegendDrawers. Tries to
	* keep the recommended width prefW. Returns the rectangle occupied.
	*/
	public Rectangle draw(Graphics g, int prefW) {
		if (legDrawers == null)
			return null;
		int y = 0, maxW = 0;
		for (int i = 0; i < legDrawers.size(); i++) {
			LegendDrawer ldr = (LegendDrawer) legDrawers.elementAt(i);
			Rectangle r = ldr.drawLegend(this, g, y, leftMarg, prefW);
			if (r != null) {
				y = r.y + r.height;
				if (r.width > maxW) {
					maxW = r.width;
				}
			}
		}
		return new Rectangle(0, 0, maxW, y);
	}

	/**
	* A method from the Legend interface: legend redrawing.
	*/
	@Override
	public void redraw() {
		repaint();
	}

	/**
	* Counts the size of the canvas needed to draw the whole legend.
	* For this purpose uses a bitmap in memory.
	*/
	protected void countSizes() {
		if (getGraphics() == null)
			return;
		if (offScreenImage == null) {
			offScreenImage = createImage(50, 50);
		}
		Dimension d = getSize();
		boolean isScrolled = (getParent() != null) && (getParent() instanceof ScrollPane), vScrollbarPresent = false;
		int sbw = 0;
		Dimension vpSize = null;
		if (isScrolled) {
			ScrollPane scp = (ScrollPane) getParent();
			vpSize = scp.getViewportSize();
			vScrollbarPresent = vpSize.height < d.height;
			d.width = vpSize.width;
			d.height = vpSize.height;
			sbw = scp.getVScrollbarWidth();
		}
		Graphics img = offScreenImage.getGraphics();
		Rectangle r = draw(img, d.width - 4);
		if (r == null) {
			r = new Rectangle(0, 0, 0, 0);
		}
		if (isScrolled && !vScrollbarPresent && r.height > vpSize.height) {
			vpSize.width -= sbw;
			r = draw(img, vpSize.width - 4);
		}
		if (isScrolled) {
			if (r.width < vpSize.width - 2) {
				r.width = vpSize.width - 2;
			}
			if (r.height < vpSize.height - 2) {
				r.height = vpSize.height - 2;
			}
		}
		w = r.width;
		h = r.height;
		img.dispose();
	}

	@Override
	public Dimension getPreferredSize() {
		if (w < 1 || h < 1) {
			countSizes();
		}
		if (w < 1 || h < 1) {
			if (getParent() != null && (getParent() instanceof ScrollPane)) {
				ScrollPane scp = (ScrollPane) getParent();
				Dimension d = scp.getViewportSize();
				if (d != null && d.width > 0 && d.height > 0)
					return d;
			}
			return new Dimension(50, 50);
		}
		return new Dimension(w, h);
	}

	@Override
	public void componentResized(ComponentEvent e) {
		if (e.getSource() instanceof ScrollPane) {
			countSizes();
			setSize(w, h);
			invalidate();
			getParent().validate();
		}
	}

	@Override
	public void componentHidden(ComponentEvent e) {
	}

	@Override
	public void componentMoved(ComponentEvent e) {
	}

	@Override
	public void componentShown(ComponentEvent e) {
	}

	@Override
	public void addMouseEventReceiver(EventReceiver eres) {
		if (eres == null)
			return;
		if (evtMan == null) {
			evtMan = new EventManager();
			addMouseListener(this);
			addMouseMotionListener(this);
		}
		evtMan.addEventReceiver(eres);
	}

	@Override
	public void removeMouseEventReceiver(EventReceiver eres) {
		if (evtMan != null && eres != null) {
			evtMan.removeEventReceiver(eres);
		}
	}

	//-------------- processing of mouse events --------------------------
	/**
	* Screen coordinates of the mouse at the moment of beginning of dragging.
	*/
	private int x0 = 0, y0 = 0, prevX = 0, prevY = 0;
	private boolean mouseIsDragging = false;

	protected void sendEventToListeners(MouseEvent sourceME, String eventId, boolean rightButton) {
		if (evtMan == null)
			return;
		DMouseEvent me = new DMouseEvent(this, eventId, sourceME);
		me.setRightButtonPressed(rightButton);
		Vector list = evtMan.getEventListeners(me);
		if (list == null || list.size() < 1)
			return;
		for (int i = 0; i < list.size(); i++) {
			EventReceiver ml = (EventReceiver) list.elementAt(i);
			ml.eventOccurred(me);
		}
	}

	protected void sendDragEventToListeners(MouseEvent sourceME) {
		if (evtMan == null)
			return;
		DMouseEvent me = new DMouseEvent(this, DMouseEvent.mDrag, sourceME, prevX, prevY, x0, y0, !mouseIsDragging);
		Vector list = evtMan.getEventListeners(me);
		if (list == null || list.size() < 1)
			return;
		for (int i = 0; i < list.size(); i++) {
			EventReceiver ml = (EventReceiver) list.elementAt(i);
			ml.eventOccurred(me);
		}
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		sendEventToListeners(e, DMouseEvent.mEntered, false);
	}

	@Override
	public void mouseExited(MouseEvent e) {
		sendEventToListeners(e, DMouseEvent.mExited, false);
	}

	@Override
	public void mouseClicked(MouseEvent e) {
	}

	@Override
	public void mousePressed(MouseEvent e) {
		x0 = e.getX();
		y0 = e.getY();
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (mouseIsDragging) {
			mouseIsDragging = false;
			sendDragEventToListeners(e);
			prevX = -1;
			prevY = -1;
//iitp
			setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
//~iitp
		} else {
			boolean right = (e.getModifiers() & InputEvent.BUTTON2_MASK) == InputEvent.BUTTON2_MASK || (e.getModifiers() & InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK;
			if (e.getClickCount() > 1) {
				sendEventToListeners(e, DMouseEvent.mDClicked, right);
			} else {
				sendEventToListeners(e, DMouseEvent.mClicked, right);
			}
		}
		x0 = y0 = 0;
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		if (!mouseIsDragging && (Math.abs(e.getX() - x0) > 3 || Math.abs(e.getY() - y0) > 3)) {
			mouseIsDragging = true;
			prevX = -1;
			prevY = -1;
//iitp
			setCursor(new Cursor(Cursor.N_RESIZE_CURSOR));
//~iitp
		}
		if (mouseIsDragging) {
			sendDragEventToListeners(e);
			prevX = e.getX();
			prevY = e.getY();
		}
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		sendEventToListeners(e, DMouseEvent.mMove, false);
	}

}
