package spade.vis.event;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Vector;

/**
* EventMeaningManager is used when one and the same event may have several
* alternative meanings.
* The EventMeaningManager registers events and their possible meanings.
* Events and meanings have identifiers and full names.
* One of alternative meanings of each event is current.
* To allow the user to select the current meaning of an event, some
* user interface object should be built.
* An EventMeaningManager can notify possible PropertyChangeLiateners
* about adding or removing event meanings.
*/

public class EventMeaningManager {
	/**
	* Data about the events to be managed, e.g. mouse click or mouse move.
	* Elements of the vector have the type EventData.
	*/
	protected Vector events = null;

	/**
	* Returns the number of the registered events.
	*/
	public int getEventCount() {
		if (events == null)
			return 0;
		return events.size();
	}

	/**
	* Finds event with the given identifier.
	*/
	public int indexOfEvent(String eventId) {
		if (eventId == null || events == null)
			return -1;
		for (int i = 0; i < events.size(); i++) {
			EventData ed = (EventData) events.elementAt(i);
			if (ed.eventId.equals(eventId))
				return i;
		}
		return -1;
	}

	/**
	* Returns EventData describing the event with the given index.
	*/
	public EventData getEventData(int idx) {
		if (events == null || idx < 0 || idx >= events.size())
			return null;
		return (EventData) events.elementAt(idx);
	}

	/**
	* Returns the identifier of the event with the given index.
	*/
	public String getEventId(int idx) {
		EventData ed = getEventData(idx);
		if (ed == null)
			return null;
		return ed.getIdentifier();
	}

	/**
	* Return number of meanings of the event with the given index
	*/
	public int getNEventMeanings(int idx) {
		EventData ed = getEventData(idx);
		if (ed == null)
			return 0;
		return ed.getNMeanings();
	}

	/**
	* Return the identifier of the meaning with the given number of the event
	* with the given index
	*/
	public String getEventMeaningId(int eventIdx, int meaningIdx) {
		EventData ed = getEventData(eventIdx);
		if (ed == null)
			return null;
		return ed.getMeaningId(meaningIdx);
	}

	/**
	* Returns the current meaning of the event with the given index.
	*/
	public String getCurrentEventMeaning(int idx) {
		EventData ed = getEventData(idx);
		if (ed == null)
			return null;
		return ed.getCurrentMeaning();
	}

	/**
	* Returns the number of the current meaning of the event with the given index.
	*/
	public int getCurrentEventMeaningN(int idx) {
		EventData ed = getEventData(idx);
		if (ed == null)
			return -1;
		return ed.getCurrentMeaningN();
	}

	/**
	* Returns EventData describing the event with the given identifier.
	*/
	public EventData getEventData(String eventId) {
		if (eventId == null || events == null)
			return null;
		return getEventData(indexOfEvent(eventId));
	}

	/**
	* Returns the current meaning of the event with the given identifier.
	*/
	public String getCurrentEventMeaning(String eventId) {
		EventData ed = getEventData(eventId);
		if (ed == null)
			return null;
		return ed.getCurrentMeaning();
	}

	/**
	* Registers an event to be managed.
	*/
	public void addEvent(String eventId, String eventName) {
		if (eventId == null)
			return;
		if (indexOfEvent(eventId) >= 0)
			return; //already registered
		if (events == null) {
			events = new Vector(10, 5);
		}
		events.addElement(new EventData(eventId, eventName));
	}

	/**
	* Registers a meaning (one of possible meanings) of the specified event.
	*/
	public void addEventMeaning(String eventId, String meaningId, String meaningText) {
		if (eventId == null)
			return;
		EventData ed = getEventData(eventId);
		if (ed != null) {
			ed.addMeaning(meaningId, meaningText);
			notifyPropertyChange(eventId + "_meaning_added", null, meaningId);
		}
	}

	/**
	* Removes the specified meaning of the specified event.
	*/
	public void removeEventMeaning(String eventId, String meaningId) {
		if (eventId == null)
			return;
		EventData ed = getEventData(eventId);
		if (ed != null) {
			ed.removeMeaning(meaningId);
			notifyPropertyChange(eventId + "_meaning_removed", null, null);
		}
	}

	/**
	* Sets the specified meaning of the specified event to be the current meaning
	* of the event. This determines who of the listeners will receive this event.
	* Returns true if the current meaning has been successfully set.
	*/
	public boolean setCurrentEventMeaning(String eventId, String meaningId) {
		if (eventId == null)
			return false;
		EventData ed = getEventData(eventId);
		if (ed == null)
			return false;
		if (ed.setCurrentMeaning(meaningId)) {
			notifyPropertyChange(eventId + "_current_meaning", null, meaningId);
			return true;
		}
		return false;
	}

	/**
	* Sets the specified meaning of the specified event to be the current meaning
	* of the event. The event and the meaning are specified by their indexes.
	* Returns true if the current meaning has been successfully set.
	*/
	public boolean setCurrentEventMeaning(int eventIdx, int meaningIdx) {
		EventData ed = getEventData(eventIdx);
		if (ed == null)
			return false;
		if (ed.setCurrentMeaning(meaningIdx)) {
			notifyPropertyChange(getEventId(eventIdx) + "_current_meaning", null, getEventMeaningId(eventIdx, meaningIdx));
			return true;
		}
		return false;
	}

//----------------- notification about properties changes---------------
	/**
	* An EventMeaningManager may have listeners of adding or removing event
	* meanings.
	* To handle the list of listeners and notify them about changes,
	* the EventMeaningManager uses a PropertyChangeSupport.
	*/
	protected PropertyChangeSupport pcSupport = null;

	public synchronized void addPropertyChangeListener(PropertyChangeListener l) {
		if (l == null)
			return;
		//System.out.println("EventMeaningManager: added listener "+l);
		if (pcSupport == null) {
			pcSupport = new PropertyChangeSupport(this);
		}
		pcSupport.addPropertyChangeListener(l);
	}

	public synchronized void removePropertyChangeListener(PropertyChangeListener l) {
		if (l == null || pcSupport == null)
			return;
		//System.out.println("EventMeaningManager: removed listener "+l);
		pcSupport.removePropertyChangeListener(l);
	}

	protected void notifyPropertyChange(String propName, Object oldValue, Object newValue) {
		if (pcSupport == null)
			return;
		pcSupport.firePropertyChange(propName, oldValue, newValue);
	}
}
