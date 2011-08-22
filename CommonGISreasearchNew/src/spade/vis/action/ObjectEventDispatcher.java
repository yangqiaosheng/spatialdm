package spade.vis.action;

import java.util.Vector;

import spade.vis.event.EventConsumer;
import spade.vis.event.EventManager;
import spade.vis.event.EventMeaningManager;
import spade.vis.event.EventReceiver;

/**
* ObjectEventDispatcher is an object that receives events informing that certain
* objects are pointed to or clicked with the mouse (ObjectEvents).
* Some components may exist that would like to interpret certain kinds
* of ObjectEvents in their own way and, moreover, have the exclusive right
* to process these events. For example, click on an object may be used in
* visual comparison. Such a component is called Event Consumer (see the
* EventConsumer interface).
* An EventConsumer may be registered at the ObjectEventDispatcher. Then, when
* an object event comes, the ObjectEventDispatcher checks if any component
* consumes it. If so, the event is forwarded to this consumer. If no, the
* default object event processing takes place, i.e. highlighting.
* For highlighting, the ObjectEventDispatcher forwards the event to a
* Highlighter. For this purpose the ObjectEventDispatcher needs a reference
* to this Highlighter.
*/

public class ObjectEventDispatcher implements ObjectEventHandler {
	/**
	* A reference to a SuperHighlighter, i.e. a component handling highlighting
	* and selection in several object sets (e.g. map layers). When an object
	* event is not consumed by any component, it is interpreted as
	* highlighting/selection change event and is forwarded to the highlighter.
	*/
	protected SuperHighlighter shigh = null;

	/**
	* EventManager helps the ObjectEventDispatcher in registering Object Event
	* Consumers and deciding to which of them each particular Object Event should
	* be forwarded.
	* A ObjectEventDispatcher creates an EventManager when this is needed
	* (i.e. when really any event consumers or receivers are registered).
	*/
	protected EventManager evtMan = null;
	/**
	* The EventMeaningManager keeps a list of possible event meanings
	*/
	protected EventMeaningManager evtMeanMan = null;

	/**
	* In the constructor the ObjectEventDispatcher creates a manager of events
	* that will be used for distributing object events. It creates also
	* an event meaning manager and adds to it the meaning "highlight"
	* for object click and object frame events.
	*/
	public ObjectEventDispatcher() {
		evtMan = new EventManager();
		evtMeanMan = new EventMeaningManager();
		evtMan.setEventMeaningManager(evtMeanMan);
		evtMeanMan.addEvent(ObjectEvent.click, ObjectEvent.getEventTypeName(ObjectEvent.click));
		evtMeanMan.addEventMeaning(ObjectEvent.click, "highlight", "durable highlighting");
		evtMeanMan.addEvent(ObjectEvent.frame, ObjectEvent.getEventTypeName(ObjectEvent.frame));
		evtMeanMan.addEventMeaning(ObjectEvent.frame, "highlight", "durable highlighting");
	}

	public void setSuperHighlighter(SuperHighlighter shigh) {
		this.shigh = shigh;
	}

	public SuperHighlighter getSuperHighlighter() {
		return shigh;
	}

	public EventManager getEventManager() {
		return evtMan;
	}

	public EventMeaningManager getEventMeaningManager() {
		return evtMeanMan;
	}

	/**
	* Registers an object event consumer. The argument "eventType" should be one
	* of the constants defined in the ObjectEvent interface to denote possible
	* actions (point, click, double click). The argument "eventMeaning"
	* must be a string denoting the way of event interpretation,
	* e.g. "visual comparison". This meaning becomes, by default, the current
	* meaning of this kind of events.
	* The argument "eventMeaningText" is a text to explain the meaning to a user.
	*/
	public void addObjectEventConsumer(EventConsumer oec, String eventType, String eventMeaning, String eventMeaningText) {
		if (oec == null || eventType == null)
			return;
		int evtIdx = -1;
		for (int i = 0; i < ObjectEvent.actions.length && evtIdx < 0; i++)
			if (eventType.equalsIgnoreCase(ObjectEvent.actions[i])) {
				evtIdx = i;
			}
		if (evtIdx < 0)
			return; //unknown event type!
		eventType = ObjectEvent.actions[evtIdx];
		if (evtMeanMan.indexOfEvent(eventType) < 0) {
			evtMeanMan.addEvent(eventType, ObjectEvent.actionFullNames[evtIdx]);
		}
		evtMeanMan.addEventMeaning(eventType, eventMeaning, eventMeaningText);
		evtMeanMan.setCurrentEventMeaning(eventType, eventMeaning);
		evtMan.addEventReceiver(oec);
	}

	/**
	* When a consumer is removed, the current meaning of the type of events
	* it consumed becomes "highlight".
	*/
	public void removeObjectEventConsumer(EventConsumer oec, String eventType, String eventMeaning) {
		if (oec == null || eventType == null || eventMeaning == null)
			return;
		evtMan.removeEventReceiver(oec);
		if (!evtMan.hasEventConsumers(eventType, eventMeaning)) {
			evtMeanMan.removeEventMeaning(eventType, eventMeaning);
		}
	}

	/**
	* Registers an object event receiver. Unlike a consumer, a receiver may
	* receive notifications about events but not prevent other objects from
	* receiving the same events and reacting to them in their own way.
	* Receivers receive an event if there is no consumer for it.
	*/
	public void addObjectEventReceiver(EventReceiver oer) {
		if (oer == null)
			return;
		System.out.println("Dispatcher registers a new event receiver: " + oer.toString());
		evtMan.addEventReceiver(oer);
	}

	/**
	* Removes the receiver of object events. First checks if this is not
	* a consumer. If so, calls removeObjectEventConsumer.
	*/
	public void removeObjectEventReceiver(EventReceiver oer) {
		if (oer == null)
			return;
		evtMan.removeEventReceiver(oer);
	}

	/**
	* When some ObjectEvent occurs (object pointed or object clicked), the
	* ObjectEventDispatcher checks whether any of the registered Event Receivers
	* consumes this event. If yes, the event is forwarded to the consumer.
	* If no, the event is forwarded to the Highlighter in order to fire object
	* highlighting.
	*/
	@Override
	public void processObjectEvent(ObjectEvent oevt) {
		if (oevt == null)
			return;
		Vector list = evtMan.getEventConsumers(oevt);
		if (list != null) {
			for (int i = 0; i < list.size(); i++) {
				((EventReceiver) list.elementAt(i)).eventOccurred(oevt);
			}
			return; //the event is consumed, i.e. interpreted in a way other
					//than highlighting
		}
		list = evtMan.getEventListeners(oevt);
		if (list != null) {
			for (int i = 0; i < list.size(); i++) {
				((EventReceiver) list.elementAt(i)).eventOccurred(oevt);
			}
		}
		if (shigh != null) {
			shigh.processObjectEvent(oevt);
		}
	}
}
