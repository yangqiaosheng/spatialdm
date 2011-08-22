package spade.vis.event;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Vector;

/**
* Multiple linked components may receive events from each other.
* It may be convenient for the user to enable or disable explicitly some
* of the links, i.e. set for each component events from which sources it
* must process. An EventBroker links producers and recipients of a specific
* type of event and allows to enable or disable particular links.
*
* An EventBroker may register itself as a consumer of the type of event
* it is responsible for. When it receives an event of this type from some of
* its registered event producers, it distributes the event among the registered
* event receivers according to the allowed links.
*/
public class EventBroker implements EventConsumer {
	/**
	* The identifier of the event type the EventBroker is responsible for
	*/
	protected String eventId = null;
	/**
	* The name of the event type the EventBroker is responsible for. The name
	* may be shown to the user.
	*/
	protected String eventName = null;
	/**
	* The identifier of the event meaning corresponding to this type of event
	*/
	protected String evtMeaningId = null;
	/**
	* The descriptive text of the event meaning corresponding to this type of event
	*/
	protected String evtMeaningText = null;
	/**
	* The producers of events. Each event producer must implement the interface
	* spade.vis.event.EventSource
	*/
	protected Vector eventSources = null;
	/**
	* The recipients of events. Each event recipient must implement the
	* interface EventReceiver.
	*/
	protected Vector eventReceivers = null;
	/**
	* The allowed links between the event sources and event receivers. Each link
	* is an array consisting of two strings. The first string is the identifier
	* of an event source, the second string is the identifier of an event
	* receiver. The source and the receiver should be present in the vectors
	* eventSources and eventReceivers, respectively.
	*/
	protected Vector links = null;
	/**
	* An EventBroker registers itself at some EventManager as an event receiver.
	* This is the EventManager the EventBroker is linked to.
	* The EventManager is also used to get information about current event
	* meanings, to add or remove meanings, and to set current meanings.
	*/
	protected EventManager evtMan = null;
	/**
	* An internal variable that shows whether the EventBroker is registered
	* at the current moment as a receiver of events.
	*/
	protected boolean receivesEvents = false;

	/**
	* Sets the identifierand the name (that may be shown to the user) of the
	* event type the EventBroker will be responsible for
	*/
	public void setEventType(String eventId, String eventName) {
		this.eventId = eventId;
		this.eventName = eventName;
	}

	/**
	* Returns the identifier of the event type the EventBroker is responsible for
	*/
	public String getEventType() {
		return eventId;
	}

	/**
	* Returns the name of the event type the EventBroker is responsible for.
	* The name may be shown to the user.
	*/
	public String getEventName() {
		return eventName;
	}

	/**
	* Sets the identifier and the descriptive text of the event meaning
	* corresponding to the type of event the broker is responsible for.
	* Only when the event has this meaning, the broker may "consume" it.
	*/
	public void setEventMeaning(String id, String txt) {
		evtMeaningId = id;
		evtMeaningText = txt;
	}

	/**
	* Returns the identifier of the event meaning corresponding to the type of
	* event the broker is responsible for.
	*/
	public String getEventMeaningId() {
		return evtMeaningId;
	}

	/**
	* Returns the descriptive text of the event meaning corresponding to the type of
	* event the broker is responsible for. The text may be shown to the user.
	*/
	public String getEventMeaningText() {
		return evtMeaningText;
	}

	/**
	* This method links the EventBroker with an EventManager.
	* The EventBroker registers itself at this EventManager as an event receiver
	* but only when the EventBroker has some sources and receivers and some
	* allowed links between them.
	* The EventManager will be used to get information about current event
	* meanings, to add or remove meanings, and to set current meanings.
	*/
	public void setEventManager(EventManager em) {
		evtMan = em;
		receivesEvents = false;
		checkStartEventReceiving();
	}

	/**
	* The EventBroker registers itself at this EventManager as an event receiver
	* but only when the EventBroker has some sources and receivers and some
	* allowed links between them.
	* When the EventBroker registers as a receiver of the events, the
	* corresponding event meaning is added to the EventMeaningManager of this
	* EventManager and becomes the current meaning of this event type.
	*/
	protected void checkStartEventReceiving() {
		if (receivesEvents || evtMan == null || eventId == null || evtMeaningId == null)
			return;
		if (eventSources != null && eventSources.size() > 0 && eventReceivers != null && eventReceivers.size() > 0 && links != null && links.size() > 0) {
			EventMeaningManager emm = evtMan.getEventMeaningManager();
			if (emm == null)
				return;
			emm.addEvent(eventId, eventName);
			emm.addEventMeaning(eventId, evtMeaningId, evtMeaningText);
			emm.setCurrentEventMeaning(eventId, evtMeaningId);
			evtMan.addEventReceiver(this);
			receivesEvents = true;
		}
	}

	/**
	* When there are no registered event sources or event receivers or allowed
	* links between the sources and the receivers, the EventBroker unregisters
	* itself at the EventManager from receiving events. The corresponding
	* event meaning is removed from the EventMeaningManager of this EventManager.
	*/
	protected void checkStopEventReceiving() {
		if (!receivesEvents || evtMan == null || eventId == null || evtMeaningId == null)
			return;
		if (eventSources == null || eventSources.size() < 1 || eventReceivers == null || eventReceivers.size() < 1 || links == null || links.size() < 1) {
			EventMeaningManager emm = evtMan.getEventMeaningManager();
			if (emm == null)
				return;
			emm.removeEventMeaning(eventId, evtMeaningId);
			evtMan.removeEventReceiver(this);
			receivesEvents = false;
		}
	}

	/**
	* Finds in the vector of event sources the source with the given identifier.
	*/
	protected EventSource findEventSource(String id) {
		if (id == null || eventSources == null)
			return null;
		for (int i = 0; i < eventSources.size(); i++) {
			EventSource es = (EventSource) eventSources.elementAt(i);
			if (es.getIdentifier().equals(id))
				return es;
		}
		return null;
	}

	/**
	* Adds a producer of events of the type the Broker is responsible for.
	* The Broker first checks if the producer produces the right type of
	* events. The producer should implement the interface EventSource.
	*/
	public void addEventSource(EventSource es) {
		if (es == null || es.getIdentifier() == null || eventId == null || !es.doesProduceEvent(eventId))
			return;
		if (findEventSource(es.getIdentifier()) == null) {
			if (eventSources == null) {
				eventSources = new Vector(10, 10);
			}
			eventSources.addElement(es);
			System.out.println("Broker of events <" + eventId + "> registered a new event source " + es.getIdentifier());
			checkStartEventReceiving();
			notifyChange("sources", eventSources);
		}
	}

	/**
	* Removes the specified source of events and all the links of event receivers
	* with this event source
	*/
	public void removeEventSource(EventSource es) {
		if (es == null || es.getIdentifier() == null || eventSources == null)
			return;
		int idx = eventSources.indexOf(es);
		if (idx >= 0) {
			eventSources.removeElementAt(idx);
			if (links != null && links.size() > 0) { //remove links with this source
				String sid = es.getIdentifier();
				for (int i = links.size() - 1; i >= 0; i--) {
					String link[] = (String[]) links.elementAt(i);
					if (link[0].equals(sid)) {
						links.removeElementAt(i);
					}
				}
			}
			System.out.println("Broker of events <" + eventId + "> removed the event source " + es.getIdentifier());
			checkStopEventReceiving();
			notifyChange("sources", eventSources);
		}
	}

	/**
	* Returns its vector of event sources. The elements of the vector
	* implement the EventSource interface.
	*/
	public Vector getAllEventSources() {
		return eventSources;
	}

	/**
	* Replies if there is at least one event source registered
	*/
	public boolean hasEventSources() {
		return eventSources != null && eventSources.size() > 0;
	}

	/**
	* Returns its vector of event receivers. The elements of the vector
	* implement the EventReceiver interface.
	*/
	public Vector getAllEventReceivers() {
		return eventReceivers;
	}

	/**
	* Replies if there is at least one event receiver registered
	*/
	public boolean hasEventReceivers() {
		return eventReceivers != null && eventReceivers.size() > 0;
	}

	/**
	* Finds in the vector of event receivers the receiver with the given identifier.
	*/
	protected EventReceiver findEventReceiver(String id) {
		if (id == null || eventReceivers == null)
			return null;
		for (int i = 0; i < eventReceivers.size(); i++) {
			EventReceiver er = (EventReceiver) eventReceivers.elementAt(i);
			if (er.getIdentifier().equals(id))
				return er;
		}
		return null;
	}

	/**
	* Adds a receiver of events of the type the Broker is responsible for.
	* The receiver should implement the interface EventReceiver.
	*/
	public void addEventReceiver(EventReceiver er) {
		if (er == null || er.getIdentifier() == null || eventId == null)
			return;
		if (!er.doesListenToEvent(eventId))
			return;
		if (findEventReceiver(er.getIdentifier()) == null) {
			if (eventReceivers == null) {
				eventReceivers = new Vector(10, 10);
			}
			eventReceivers.addElement(er);
			System.out.println("Broker of events <" + eventId + "> registered a new event receiver " + er.getIdentifier());
			checkStartEventReceiving();
			notifyChange("receivers", eventReceivers);
		}
	}

	/**
	* Removes the specified receiver of events and all the links of event sources
	* with this event receiver
	*/
	public void removeEventReceiver(EventReceiver er) {
		if (er == null || er.getIdentifier() == null || eventReceivers == null)
			return;
		int idx = eventReceivers.indexOf(er);
		if (idx >= 0) {
			eventReceivers.removeElementAt(idx);
			if (links != null && links.size() > 0) { //remove links with this source
				String rid = er.getIdentifier();
				for (int i = links.size() - 1; i >= 0; i--) {
					String link[] = (String[]) links.elementAt(i);
					if (link[1].equals(rid)) {
						links.removeElementAt(i);
					}
				}
			}
			System.out.println("Broker of events <" + eventId + "> removed the event receiver " + er.getIdentifier());
			checkStopEventReceiving();
			notifyChange("receivers", eventReceivers);
		}
	}

	/**
	* Finds the index of the link between the specified event source and
	* event receiver in the vector of allowed links, if it is present there.
	*/
	public int getIndexOfLink(String sourceId, String receiverId) {
		if (sourceId == null || receiverId == null || links == null)
			return -1;
		for (int i = 0; i < links.size(); i++) {
			String link[] = (String[]) links.elementAt(i);
			if (link[0].equals(sourceId) && link[1].equals(receiverId))
				return i;
		}
		return -1;
	}

	/**
	* Finds the index of the link between the specified event source and
	* event receiver in the vector of allowed links, if it is present there.
	*/
	public int getIndexOfLink(EventSource es, EventReceiver er) {
		if (es == null || er == null)
			return -1;
		return getIndexOfLink(es.getIdentifier(), er.getIdentifier());
	}

	/**
	* Answers whether there is an allowed link between the specified (by its
	* identifier) event source and the event receiver (also specified through
	* its identifier). For this purpose the EventBroker looks whether such link
	* exists in the vector of allowed links.
	*/
	public boolean isLinkSet(String sourceId, String receiverId) {
		return getIndexOfLink(sourceId, receiverId) >= 0;
	}

	/**
	* Answers whether there is an allowed link between the specified event source
	* and the event receiver. For this purpose looks whether such link
	* exists in the vector of allowed links.
	*/
	public boolean isLinkSet(EventSource es, EventReceiver er) {
		return getIndexOfLink(es, er) >= 0;
	}

	/**
	* Sets a link between the specified event source and the event receiver.
	* First checks whether the source and the reciever are registered at this
	* Broker. If not, the link is not set.
	*/
	public void setLink(EventSource es, EventReceiver er) {
		if (es == null || er == null)
			return;
		setLink(es.getIdentifier(), er.getIdentifier());
	}

	/**
	* Sets a link between the specified event source and the event receiver.
	* The source and the recievers are specified by their identifiers.
	* First checks whether the source and the reciever are registered at this
	* Broker. If not, the link is not set.
	*/
	public void setLink(String sourceId, String receiverId) {
		if (sourceId == null || receiverId == null)
			return;
		if (findEventSource(sourceId) == null || findEventReceiver(receiverId) == null)
			return;
		if (isLinkSet(sourceId, receiverId))
			return;
		String lk[] = new String[2];
		lk[0] = sourceId;
		lk[1] = receiverId;
		if (links == null) {
			links = new Vector(20, 10);
		}
		links.addElement(lk);
		System.out.println("Broker of events <" + eventId + "> has set a link from " + sourceId + " to " + receiverId);
		checkStartEventReceiving();
		notifyChange("links", links);
	}

	/**
	* Breaks the link between the specified event source and the event receiver.
	*/
	public void breakLink(EventSource es, EventReceiver er) {
		if (es == null || er == null)
			return;
		breakLink(es.getIdentifier(), er.getIdentifier());
	}

	/**
	* Breaks the link between the specified event source and the event receiver.
	* The source and the recievers are specified by their identifiers.
	*/
	public void breakLink(String sourceId, String receiverId) {
		if (links == null || links.size() < 1 || sourceId == null || receiverId == null)
			return;
		if (findEventSource(sourceId) == null || findEventReceiver(receiverId) == null)
			return;
		int idx = -1;
		for (int i = 0; i < links.size() && idx < 0; i++) {
			String link[] = (String[]) links.elementAt(i);
			if (link[0].equals(sourceId) && link[1].equals(receiverId)) {
				idx = i;
			}
		}
		if (idx >= 0) {
			links.removeElementAt(idx);
			System.out.println("Broker of events <" + eventId + "> has broken the link from " + sourceId + " to " + receiverId);
			checkStopEventReceiving();
			notifyChange("links", links);
		}
	}

	/**
	* Returns all currently set links between the event sources and event
	* receivers. Each link is an array consisting of two strings. The first
	* string is the identifier of an event source, the second string is the
	* identifier of an event receiver.
	*/
	public Vector getAllLinks() {
		return links;
	}

	/**
	* Returns true if there is at least one link
	*/
	public boolean hasLinks() {
		return links != null && links.size() > 0;
	}

	/**
	* Returns a vector of event receivers linked to the specified event source.
	*/
	public Vector getEventReceivers(EventSource es) {
		if (es == null || eventSources == null || eventReceivers == null || links == null || es.getIdentifier() == null || !eventSources.contains(es))
			return null;
		String sid = es.getIdentifier();
		Vector v = new Vector(10, 10);
		for (int i = 0; i < links.size(); i++) {
			String link[] = (String[]) links.elementAt(i);
			if (link[0].equals(sid)) {
				EventReceiver er = findEventReceiver(link[1]);
				if (er != null) {
					v.addElement(er);
				}
			}
		}
		if (v.size() < 1)
			return null;
		return v;
	}

	//------ notification about changes of event sources, recipients, and links ---------
	/**
	* An EventBroker may have listeners of changes of the sets of event sources,
	* event receivers, and links between them.
	* The listeners should implement the PropertyChangeListener interface.
	* To handle the list of listeners and notify them about changes of the
	* object set or object data, the EventBroker uses a PropertyChangeSupport.
	*/
	protected PropertyChangeSupport pcSupport = null;

	/**
	* Registeres a listener of changes.
	* The listener must implement the PropertyChangeListener interface.
	*/
	public synchronized void addPropertyChangeListener(PropertyChangeListener l) {
		if (l == null)
			return;
		if (pcSupport == null) {
			pcSupport = new PropertyChangeSupport(this);
		}
		pcSupport.addPropertyChangeListener(l);
	}

	/**
	* Unregisteres a listener of changes.
	*/
	public synchronized void removePropertyChangeListener(PropertyChangeListener l) {
		if (l == null || pcSupport == null)
			return;
		pcSupport.removePropertyChangeListener(l);
	}

	/**
	* An internal method used to notify all the listeners about changes.
	* The argument "what" may be one of the string constants "sources",
	* "receivers", or "links". The argument "value" contains the vector
	* of sources, receivers, or links, respectively.
	*/
	protected void notifyChange(String what, Object value) {
		if (pcSupport == null)
			return;
		pcSupport.firePropertyChange(what, null, value);
	}

//------------- implementation of the EventConsumer interface --------------
	/**
	* The EventReceiver answers whether it is interested in getting the specified
	* kind of events.
	* The ObjectBroker returns false because it wants to not only receive events
	* but also consume them.
	*/
	@Override
	public boolean doesListenToEvent(String eventId) {
		return false;
	}

	/**
	* This method is used for delivering events to the Event Receiver.
	*/
	@Override
	public void eventOccurred(DEvent evt) {
		if (evt.getSource() != null && (evt.getSource() instanceof EventSource)) {
			Vector receivers = getEventReceivers((EventSource) evt.getSource());
			if (receivers != null) {
				for (int i = 0; i < receivers.size(); i++) {
					((EventReceiver) receivers.elementAt(i)).eventOccurred(evt);
				}
			}
		}
	}

	/**
	* Returns a unique identifier of the event receiver (may be produced
	* automatically, used only internally, not shown to the user).
	*/
	@Override
	public String getIdentifier() {
		return "ObjectBroker_" + eventId;
	}

	/**
	* The Event Consumer answers whether it consumes the specified event
	* when it has the given meaning.
	*/
	@Override
	public boolean doesConsumeEvent(DEvent evt, String eventMeaning) {
		return doesConsumeEvent(evt.getId(), eventMeaning);
	}

	/**
	* The Event Consumer answers whether it consumes the specified event
	* when it has the given meaning.
	*/
	@Override
	public boolean doesConsumeEvent(String evtType, String eventMeaning) {
		if (eventId == null || evtMeaningId == null)
			return false;
		return this.eventId.equals(evtType) && evtMeaningId.equals(eventMeaning);
	}
}
