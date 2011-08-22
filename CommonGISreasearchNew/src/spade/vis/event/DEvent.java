package spade.vis.event;

import java.awt.event.MouseEvent;
import java.util.EventObject;

/**
* Root class for Descartes events
*/
public class DEvent extends EventObject {
	/**
	* Identifier of the event
	*/
	protected String eventId = null;
	/**
	* If the Descartes event was initiated by some mouse event, a reference to
	* this mouse event remains accessible. Through this reference it is possible
	* to find in what component the event occurred and what the coordinates of the
	* mouse were at this moment
	*/
	protected MouseEvent sourceME = null;

	/**
	* Constructs a Descartes event with the given identifier. The third argument
	* is the mouse event that initiated this Descartes event. This may be null
	* if the event does not originate from a mouse event.
	*/
	public DEvent(Object source, String identifier, MouseEvent sourceMouseEvent) {
		super(source);
		eventId = identifier;
		sourceME = sourceMouseEvent;
	}

	/**
	* Returns the identifier of the event
	*/
	public String getId() {
		return eventId;
	}

	/**
	* Returns the mouse event that initiated this Descartes event. May return null
	* if the event does not originate from a mouse event.
	*/
	public MouseEvent getSourceMouseEvent() {
		return sourceME;
	}
}
