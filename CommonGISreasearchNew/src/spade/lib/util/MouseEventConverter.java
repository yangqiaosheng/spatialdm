package spade.lib.util;

import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;

import spade.vis.event.DMouseEvent;

/**
 * Created by IntelliJ IDEA.
 * User: DIvan
 * Date: 29-Apr-2004
 * Time: 19:21:36
 */
public class MouseEventConverter {
	private Object eventSource;

	public long DCLICK_DELAY = 500;

	private Point pressedAt = null;
	private long releasedAt = 0;
	private Point prev = null;
	private Point curr = null;
	private boolean dragging = false;

	public MouseEventConverter(Object eventSource) {
		this.eventSource = eventSource;
	}

	/**
	 * Converts AWT mouse events to CommonGIS-style events.
	 * Press-and-hold event is not supported
	 */
	public DMouseEvent convertMouseEvent(MouseEvent e) {
		String eventId = null;
		prev = curr;
		curr = e.getPoint();
		if (prev == null) {
			prev = curr;
		}
		switch (e.getID()) {
		case MouseEvent.MOUSE_ENTERED:
//System.out.println("*** Entered"); if (true) return null;
			eventId = DMouseEvent.mEntered;
			break;
		case MouseEvent.MOUSE_EXITED:
//System.out.println("*** Exited"); if (true) return null;
			eventId = DMouseEvent.mExited;
			break;
		case MouseEvent.MOUSE_MOVED:
//System.out.println("*** Moved"); if (true) return null;
			eventId = DMouseEvent.mMove;
			dragging = false;
			break;
		case MouseEvent.MOUSE_PRESSED:
//System.out.println("*** Pressed"); if (true) return null;
			pressedAt = curr;
			break;
		case MouseEvent.MOUSE_RELEASED:
//System.out.println("*** Released"); if (true) return null;
			long releaseTime = System.currentTimeMillis();
			if (dragging) {
				eventId = DMouseEvent.mDrag;
			} else if (pressedAt.equals(curr))
				if (releaseTime - releasedAt < DCLICK_DELAY) {
					eventId = DMouseEvent.mDClicked;
				} else {
					eventId = DMouseEvent.mClicked;
				}
			dragging = false;
			releasedAt = releaseTime;
			break;
		case MouseEvent.MOUSE_CLICKED:
//System.out.println("*** Clicked"); if (true) return null;
			break;
		case MouseEvent.MOUSE_DRAGGED:
//System.out.println("*** Dragged"); if (true) return null;
			eventId = DMouseEvent.mDrag;
			dragging = true;
			break;
		default:
			throw new IllegalArgumentException("Wrong mouse event received!");
		}
		if (eventId == null)
			return null;
		DMouseEvent me;
		if (eventId.equals(DMouseEvent.mDrag)) {
			me = new DMouseEvent(eventSource, eventId, e, prev.x, prev.y, pressedAt.x, pressedAt.y, !dragging);
		} else {
			me = new DMouseEvent(eventSource, eventId, e);
		}
		me.setRightButtonPressed(checkRightButtonPressed(e));
		return me;
	}

	protected boolean checkRightButtonPressed(MouseEvent e) {
		if (e == null)
			return false;
		if ((e.getModifiers() & InputEvent.BUTTON2_MASK) == InputEvent.BUTTON2_MASK)
			return true;
		if ((e.getModifiers() & InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK)
			return true;
		return false;
	}

}
