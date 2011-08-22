package spade.vis.event;

import java.awt.event.MouseEvent;

/**
* A component can transform standard MouseEvents into DMouseEvents for better
* convenience of other components and send them to all listeners.
* The kind of event is specified through the argument eventId that
* may be one of the following strings:
* MouseEntered, MouseExited, MouseMove, MouseClicked, MouseDoubleClicked,
* MouseDrag, MouseDragStop.
*/
public class DMouseEvent extends DEvent {
	public static final String events[] = { "MouseEntered", "MouseExited", "MouseMove", "MouseClicked", "MouseDoubleClicked", "MouseDrag", "MouseLongPressed" };
	public static final String eventFullTexts[] = { "mouse entered", "mouse exited", "mouse move", "mouse click", "mouse double click", "mouse dragging", "mouse pressing" };
	public static final String mEntered = events[0], mExited = events[1], mMove = events[2], mClicked = events[3], mDClicked = events[4], mDrag = events[5], mLongPressed = events[6];

	public static String getEventFullText(String identifier) {
		if (identifier == null)
			return null;
		for (int i = 0; i < events.length; i++)
			if (identifier.equalsIgnoreCase(events[i]))
				return eventFullTexts[i];
		return null;
	}

	protected int dragX0 = 0, dragY0 = 0, dragPrevX = 0, dragPrevY = 0;
	protected boolean draggingFinished = true;
	protected boolean rightButtonPressed = false;

	/**
	* Constructs a DMouseEvent. The argument "type" specifies the kind of the
	* event, i.e. one of the constants "MouseEntered", "MouseExited",
	* "MouseMove" etc. The third argument is the original (standard) mouse event
	* that is transformed into a Descartes mouse event.
	*/
	public DMouseEvent(Object source, String type, MouseEvent mEvent) {
		super(source, type, mEvent);
	}

	/**
	* This constructor is used to construct a DMouseEvent to inform about mouse
	* dragging. The arguments dragX0, dragY0 specify the position of the mouse
	* at the moment of the start of dragging, and prevX, prevY specify mouse
	* position at the moment of previous mouse dragging event
	*/
	public DMouseEvent(Object source, String type, MouseEvent mEvent, int prevX, int prevY, int dragX0, int dragY0, boolean dragFinished) {
		this(source, type, mEvent);
		this.dragX0 = dragX0;
		this.dragY0 = dragY0;
		this.dragPrevX = prevX;
		this.dragPrevY = prevY;
		draggingFinished = dragFinished;
	}

	/**
	* Returns the x-coordinate of the mouse.
	*/
	public int getX() {
		if (sourceME != null)
			return sourceME.getX();
		return 0;
	}

	/**
	* Returns the y-coordinate of the mouse.
	*/
	public int getY() {
		if (sourceME != null)
			return sourceME.getY();
		return 0;
	}

	/**
	* Returns the x-coordinate of the mouse at the moment when dragging started.
	*/
	public int getDragStartX() {
		return dragX0;
	}

	/**
	* Returns the y-coordinate of the mouse at the moment when dragging started.
	*/
	public int getDragStartY() {
		return dragY0;
	}

	/**
	* Returns the x-coordinate of the mouse at the moment of previous mouse dragging event.
	*/
	public int getDragPrevX() {
		return dragPrevX;
	}

	/**
	* Returns the y-coordinate of the mouse at the moment of previous mouse dragging event.
	*/
	public int getDragPrevY() {
		return dragPrevY;
	}

	/**
	* Returns true if dragging has already finished
	*/
	public boolean isDraggingFinished() {
		return draggingFinished;
	}

	public void setRightButtonPressed(boolean value) {
		rightButtonPressed = value;
	}

	public boolean getRightButtonPressed() {
		return rightButtonPressed;
	}
}
