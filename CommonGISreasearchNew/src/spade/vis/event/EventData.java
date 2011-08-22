package spade.vis.event;

import java.util.Vector;

/**
* EventData describes an event that may have several alternative meanings
* (e.g. mouse drag may mean zoom, shift, or select) and/or be processed
* concurrently by several listeners.
* One of the alternative meanings of the event is called current.
* The current meaning may be externally changed. The current meaning
* may define who of the listeners should receive the event.
*/

public class EventData {
	/**
	* Identifier of the event, e.g. "MouseClick", "MouseMove" or some code
	*/
	public String eventId = null;
	/**
	* Text (name) of the event that can be shown to the user, e.g. "mouse click".
	* In multilingual applications the name may depend on the current interface language
	*/
	public String eventName = null;
	/**
	* Identifiers of meanings of the event, if the event can have more than one
	* alternative meanings. For example, "MouseClick" may mean "info", "select",
	* or "compare". If the event has only one meaning, the vector meanings
	* may be null or empty.
	*/
	public Vector meanings = null;
	/**
	* Texts explaining meanings of the event to the user. May depend on the
	* current interface language.
	*/
	public Vector meaningTexts = null;
	/**
	* Index of the current meaning of the event.
	*/
	protected int currN = 0;

	/**
	* Constructor: requires identifier and name of the event.
	*/
	public EventData(String id, String name) {
		eventId = id;
		eventName = name;
	}

	public String getIdentifier() {
		return eventId;
	}

	public String getName() {
		if (eventName != null)
			return eventName;
		return eventId;
	}

	public void setName(String name) {
		eventName = name;
	}

	/**
	* Registers one of alternative meanings of the event. A meaning has an
	* internal identifier (unique among the meanings of the same event)
	* and an external text that can be shown to the user.
	*/
	public void addMeaning(String meaningId, String meaningText) {
		if (meaningId == null)
			return;
		if (meanings == null) {
			meanings = new Vector(5, 5);
		}
		if (meanings.indexOf(meaningId) >= 0)
			return; //already registered
		meanings.addElement(meaningId);
		if (meaningTexts == null) {
			meaningTexts = new Vector(5, 5);
		}
		meaningTexts.addElement(meaningText);
	}

	public void removeMeaning(String meaningId) {
		if (meanings == null || meaningId == null)
			return;
		int idx = meanings.indexOf(meaningId);
		if (idx >= 0) {
			meanings.removeElementAt(idx);
			meaningTexts.removeElementAt(idx);
		}
		if (currN >= meanings.size()) {
			currN = meanings.size() - 1;
		}
	}

	/**
	* Returns the number of different meanings of the event
	*/
	public int getNMeanings() {
		if (meanings == null)
			return 0;
		return meanings.size();
	}

	/**
	* Returns the identifier of the meaning with the given index
	*/
	public String getMeaningId(int idx) {
		if (idx < 0 || idx >= getNMeanings())
			return null;
		return (String) meanings.elementAt(idx);
	}

	/**
	* Returns the identifier of the meaning with the given index
	*/
	public String getMeaningText(int idx) {
		if (idx < 0 || idx >= getNMeanings())
			return null;
		String name = null;
		if (meaningTexts != null && idx < meaningTexts.size()) {
			name = (String) meaningTexts.elementAt(idx);
		}
		if (name == null) {
			name = (String) meanings.elementAt(idx);
		}
		return name;
	}

	/**
	* Returns false if the meaning index is not valid
	*/
	public boolean setCurrentMeaning(int idx) {
		if (meanings == null)
			return false;
		if (idx < 0 || idx >= meanings.size())
			return false;
		currN = idx;
		return true;
	}

	/**
	* Returns false if the specified meaning has not been found among the
	* registered meanings.
	*/
	public boolean setCurrentMeaning(String meaningId) {
		if (meanings == null)
			return false;
		if (meaningId == null) {
			currN = -1;
			return true;
		}
		int idx = meanings.indexOf(meaningId);
		if (idx < 0)
			return false;
		currN = idx;
		return true;
	}

	/**
	* Returns the identifier of the current meaning or null if no meanings
	* have been registered.
	*/
	public String getCurrentMeaning() {
		return getMeaningId(currN);
	}

	/**
	* Returns the number of the current meaning or -1 if no meanings
	* have been registered.
	*/
	public int getCurrentMeaningN() {
		return currN;
	}

	/**
	* Returns the text (name) of the current meaning or null if no meanings
	* have been registered.
	* If the current meaning has no name (the name is null), returns its
	* identifier.
	*/
	public String getCurrentMeaningText() {
		return getMeaningText(currN);
	}
}
