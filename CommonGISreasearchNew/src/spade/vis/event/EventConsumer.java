package spade.vis.event;

/**
* EventConsumer is an EventReceiver that would like to capture certain kinds
* of events in its exclusive possession. This means that such events should be
* delivered solely to this object.
* There may be several objects claiming for the same kind of events.
* In such cases it is said that an event has several "meanings". Each
* Event Consumer has its corresponding meaning (or more than one meaning)
* of the event. Meanings are represented by strings. For example, possible
* meanings of the event "MouseDrag" may be "ZoomIn", "Shift", and "Select",
* where the first two meanings may correspond to the same component MapZoomer.
* At each moment one of the possible meanings of an event is selected as
* the current meaning. The events are sent to the EventConsumer that
* "understands" the current meaning.
* The event management may be done with the use of the class EventManager.
*/

public interface EventConsumer extends EventReceiver {
	/**
	* The Event Consumer answers whether it consumes the specified event
	* when it has the given meaning.
	*/
	public boolean doesConsumeEvent(String evtType, String eventMeaning);

	/**
	* The Event Consumer answers whether it consumes the specified event
	* when it has the given meaning.
	*/
	public boolean doesConsumeEvent(DEvent evt, String eventMeaning);
}