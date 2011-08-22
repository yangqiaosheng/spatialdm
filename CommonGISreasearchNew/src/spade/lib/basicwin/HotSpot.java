package spade.lib.basicwin;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Vector;

/**
* This class is needed to create mouse-clickable areas over a Component,
* in other words "hot spots". When mouse is clicked in such area ActionEvent
* is being generated and sent to the listeners.
* If mouse is over the hot spot MouseMotion event is being sent to listeners.
* It intended to use for definition "mouse-active" zones in large or structured
* components like Canvas with manipulators or legend drawings.
*
* Generally this class is NOT a component by its nature.
* It extends Component and simulates its behaviour to be owner for PopupManager.
*/

public class HotSpot extends Component implements MouseListener, MouseMotionListener {
	protected String actionCmd = "HotSpot";
	protected ActionSupport asup = null;
	protected Vector m_listeners = null; // MouseListeners
	protected Vector mm_listeners = null; // MouseMotionListeners

	protected boolean isEnabled = true;
	protected PopupManager hotspotTip = null;
	private Component owner = null;
	private Rectangle spot = null;
	private boolean mouseOver = false;

	public HotSpot(Component cParent) {
		this(cParent, new Rectangle(-1, -1, 0, 0));
	}

	public HotSpot(Component cParent, int x0, int y0, int w, int h) {
		this(cParent, new Rectangle(x0, y0, w, h));
	}

	public HotSpot(Component cParent, Rectangle spotArea) {
		setOwner(cParent);
		spot = spotArea;
	}

	@Override
	public boolean isShowing() {
		if (owner == null)
			return false;
		return isEnabled && owner.isShowing();
	}

	public Component getOwner() {
		return owner;
	}

	@Override
	public Container getParent() {
		if (owner == null)
			return null;
		if (owner instanceof Container)
			return (Container) owner;
		return owner.getParent();
	}

	public void setOwner(Component newOwner) {
		if (newOwner == null || newOwner.equals(owner))
			return;
		if (owner != null) {
			owner.removeMouseListener(this);
			owner.removeMouseMotionListener(this);
		}
		owner = newOwner;
		owner.addMouseListener(this);
		owner.addMouseMotionListener(this);
	}

	@Override
	public Dimension getSize() {
		return this.getPreferredSize();
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(spot.width, spot.height);
	}

	@Override
	public Point getLocation() {
		return new Point(spot.x, spot.y);
	}

	@Override
	public Point getLocationOnScreen() {
		if (owner == null)
			return null;
		int x = owner.getLocationOnScreen().x + spot.x, y = owner.getLocationOnScreen().y + spot.y;
		return new Point(x, y);
	}

	public String getActionCommand() {
		return actionCmd;
	}

	public PopupManager getPopupManager() {
		return hotspotTip;
	}

	public void setActionCommand(String cmd) {
		actionCmd = cmd;
	}

	@Override
	public void setEnabled(boolean value) {
		isEnabled = value;
		if (owner != null)
			if (!isEnabled) {
				owner.removeMouseListener(this);
				owner.removeMouseMotionListener(this);
			} else {
				owner.addMouseListener(this);
				owner.addMouseMotionListener(this);
			}
	}

	public void addActionListener(ActionListener listener) {
		if (listener == null)
			return;
		if (asup == null) {
			asup = new ActionSupport();
		}
		asup.addActionListener(listener);
	}

	public void removeActionListener(ActionListener listener) {
		if (listener == null)
			return;
		if (asup == null)
			return;
		asup.removeActionListener(listener);
	}

	@Override
	public synchronized void addMouseListener(MouseListener l) {
		if (l == null)
			return;
		if (m_listeners == null) {
			m_listeners = new Vector(5, 5);
		}
		m_listeners.addElement(l);
	}

	@Override
	public synchronized void removeMouseListener(MouseListener l) {
		if (l == null)
			return;
		if (m_listeners == null)
			return;
		m_listeners.removeElement(l);
	}

	@Override
	public synchronized void addMouseMotionListener(MouseMotionListener l) {
		if (l == null)
			return;
		if (mm_listeners == null) {
			mm_listeners = new Vector(5, 5);
		}
		mm_listeners.addElement(l);
	}

	@Override
	public synchronized void removeMouseMotionListener(MouseMotionListener l) {
		if (l == null)
			return;
		if (mm_listeners == null)
			return;
		mm_listeners.removeElement(l);
	}

	protected void sendActionEventToListeners() {
		if (asup != null && isEnabled) {
			asup.fireActionEvent(this, actionCmd);
		}
	}

	protected void sendMouseExitedToListeners(MouseEvent me) {
		if (m_listeners != null && isEnabled) {
			for (int i = 0; i < m_listeners.size(); i++) {
				((MouseListener) m_listeners.elementAt(i)).mouseExited(me);
			}
		}
	}

	protected void sendMouseEnteredToListeners(MouseEvent me) {
		if (m_listeners != null && isEnabled) {
			for (int i = 0; i < m_listeners.size(); i++) {
				((MouseListener) m_listeners.elementAt(i)).mouseEntered(me);
			}
		}
	}

	protected void sendMouseMovedToListeners(MouseEvent me) {
		if (mm_listeners != null && isEnabled) {
			for (int i = 0; i < m_listeners.size(); i++) {
				((MouseMotionListener) mm_listeners.elementAt(i)).mouseMoved(me);
			}
		}
	}

	/**
	 * "Forgets" all its listeners.
	 */
	public void destroy() {
		asup = null;
		m_listeners = null;
		mm_listeners = null;
		setEnabled(false);
	}

	@Override
	public void mouseClicked(MouseEvent me) {
		if (hotspotTip != null) {
			PopupManager.hideWindow();
		}
		int x = me.getX();
		int y = me.getY();
		if (spot.contains(x, y)) {
			sendActionEventToListeners();
		}
	}

	/*
	* The function needed for preserving functionality of hot spot
	* if the owner component is not available i.e. null (heavy case, but why not?)
	*/
	public void mouseClickedAt(int x, int y) {
		if (spot.contains(x, y)) {
			sendActionEventToListeners();
		}
	}

	@Override
	public void mouseMoved(MouseEvent me) {
		int x = me.getX(), y = me.getY(), mod = me.getModifiers();
		long when = me.getWhen();

		if (spot.contains(x, y)) {
			if (!mouseOver) {
				mouseOver = true;
				sendMouseEnteredToListeners(new MouseEvent(this, MouseEvent.MOUSE_ENTERED, when, mod, x - spot.x, y - spot.y, 0, me.isPopupTrigger()));
				return;
			}
			sendMouseMovedToListeners(new MouseEvent(this, MouseEvent.MOUSE_MOVED, when, mod, x - spot.x, y - spot.y, 0, me.isPopupTrigger()));
		} else if (mouseOver) {
			sendMouseExitedToListeners(new MouseEvent(this, MouseEvent.MOUSE_EXITED, when, mod, x - spot.x, y - spot.y, 0, me.isPopupTrigger()));
			mouseOver = false;
		}
	}

	@Override
	public void setLocation(int x, int y) {
		spot.setLocation(x, y);
	}

	@Override
	public void setSize(int w, int h) {
		spot.setSize(w, h);
	}

	@Override
	public boolean contains(int x, int y) {
		return spot.contains(x, y);
	}

	public void setPopup(Component content) {
		if (hotspotTip == null) {
			hotspotTip = new PopupManager(this, content, true);
		} else {
			hotspotTip.setContent(content);
		}
	}

	public void setPopup(String contentTxt) {
		if (hotspotTip == null) {
			hotspotTip = new PopupManager(this, contentTxt, true);
		} else {
			hotspotTip.setText(contentTxt);
		}
	}

	public void setPopupActive(boolean value) {
		if (hotspotTip != null) {
			if (!value) {
				PopupManager.hideWindow();
			}
			hotspotTip.setKeepHidden(!value);
		}
	}

	public void showPopup(int mouseX, int mouseY) {
		if (isEnabled && hotspotTip != null) {
			hotspotTip.startShow(mouseX, mouseY);
		}
	}

	@Override
	public void mouseReleased(MouseEvent me) {
	}

	@Override
	public void mousePressed(MouseEvent me) {
		if (hotspotTip != null) {
			PopupManager.hideWindow();
		}
	}

	@Override
	public void mouseExited(MouseEvent me) {
		if (hotspotTip != null) {
			PopupManager.hideWindow();
		}
		int x = me.getX(), y = me.getY(), mod = me.getModifiers();
		long when = me.getWhen();
		sendMouseExitedToListeners(new MouseEvent(this, MouseEvent.MOUSE_EXITED, when, mod, x - spot.x, y - spot.y, 0, me.isPopupTrigger()));
		mouseOver = false;
	}

	@Override
	public void mouseEntered(MouseEvent me) {
		int x = me.getX(), y = me.getY(), mod = me.getModifiers();
		long when = me.getWhen();
		if (spot.contains(x, y)) {
			sendMouseEnteredToListeners(new MouseEvent(this, MouseEvent.MOUSE_ENTERED, when, mod, x - spot.x, y - spot.y, 0, me.isPopupTrigger()));
			mouseOver = false;
		}
	}

	@Override
	public void mouseDragged(MouseEvent me) {
	}
}