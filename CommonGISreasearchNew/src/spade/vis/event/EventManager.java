package spade.vis.event;

import java.util.Vector;

/**
* EventManager is used when one and the same event may have several alternative
* meanings. Depending on the current  meaning of the event, it should be
* sent to one of the registered event listeners (see the interface
* EventConsumer). For example, mouse click may
* mean selection of an object in a map or visual comparison. In the first
* case the events should be sent solely to the component that performs
* selection. In the second case the events should be sent to the
* map manipulation widget.
* One of the alternative meanings of the event is current. The event is always
* sent to the consumer that "understands" the current meaning and consumes
* the event. If there is no such consumer, the event is distributed among
* all the event listeners that are interested in it. For example, a component
* may be interested in mouse movement events without consuming them, i.e. it
* allows other components to react to the same event.
* An EventManager is always connected to an EventMeaningManager that
* provides information about current meanings of events.
*/

public class EventManager {
	/**
	* Listeners of the events. Elements of the vector have the type EventReceiver.
	*/
	protected Vector listeners = null;
	/**
	* The EventMeaningManager provides information about current meanings of events
	*/
	protected EventMeaningManager evtMeanMan = null;

	public void setEventMeaningManager(EventMeaningManager man) {
		evtMeanMan = man;
	}

	public EventMeaningManager getEventMeaningManager() {
		return evtMeanMan;
	}

	/**
	* Registers an event receiver (that may or may not be a consumer).
	*/
	public void addEventReceiver(EventReceiver er) {
		if (er == null)
			return;
		if (listeners == null) {
			listeners = new Vector(10, 10);
		}
		if (listeners.indexOf(er) <= 0) {
			listeners.addElement(er);
		}
	}

	/**
	* Unregisters the event receiver.
	*/
	public void removeEventReceiver(EventReceiver er) {
		if (er == null)
			return;
		if (listeners == null)
			return;
		int idx = listeners.indexOf(er);
		if (idx >= 0) {
			listeners.removeElementAt(idx);
		}
	}

	/**
	* Returns its vector of event receivers
	*/
	public Vector getAllEventReceivers() {
		return listeners;
	}

	/**
	* Returns the consumer of the given event type (taking into account the
	* current meaning of the event). If there is no consumer for this event,
	* returns null.
	*/
	public Vector getEventConsumers(DEvent evt) {
		if (evt == null || listeners == null)
			return null;
		if (evtMeanMan == null)
			return null; //no manager of alternative meanings;
		//so, the may be no consumers
		EventData ed = evtMeanMan.getEventData(evt.getId());
		if (ed == null)
			return null; //an event unknown to the meaning manager
		//evidently, it has no alternative meanings and no consumers
		String mean = ed.getCurrentMeaning();
		if (mean == null)
			return null; //unknown current meaning of the event
		Vector v = new Vector(5, 5);
		for (int i = 0; i < listeners.size(); i++)
			if ((listeners.elementAt(i) instanceof EventConsumer)) {
				EventConsumer ec = (EventConsumer) listeners.elementAt(i);
				if (ec.doesConsumeEvent(evt, mean)) {
					v.addElement(ec);
				}
			}
		if (v.size() < 1)
			return null; //no event consumers found
		return v;
	}

	/**
	* Returns the consumer of the given event type (taking into account the
	* current meaning of the event). If there is no consumer for this event,
	* returns null.
	*/
	public boolean hasEventConsumers(String evtType, String evtMeaning) {
		if (evtType == null || evtMeaning == null || listeners == null || listeners.size() < 1)
			return false;
		for (int i = 0; i < listeners.size(); i++)
			if ((listeners.elementAt(i) instanceof EventConsumer)) {
				EventConsumer ec = (EventConsumer) listeners.elementAt(i);
				if (ec.doesConsumeEvent(evtType, evtMeaning))
					return true;
			}
		return false;
	}

	/**
	* The main function of the EventManager: returns the list of objects
	* who should receive the specified event. If the event is consumed,
	* only one object is returned. This is the object that consumes the
	* specified event given its current meaning.
	* For example, when mouse click means selection of an object in a map,
	* the EventManager returns the component that performs selection.
	* When mouse click means visual comparison, the EventManager returns the
	* map manipulation widget.
	* If no "shared" events have been registered, all events are to be sent
	* to all the listeners that are listening to them.
	*/
	public Vector getEventListeners(DEvent evt) {
		if (evt == null || listeners == null)
			return null;
		Vector list = getEventConsumers(evt);
		if (list != null)
			return list;
		list = new Vector(listeners.size(), 5);
		for (int i = 0; i < listeners.size(); i++) {
			EventReceiver er = (EventReceiver) listeners.elementAt(i);
			if (er.doesListenToEvent(evt.getId())) {
				list.addElement(er);
			}
		}
		return list;
	}
}
