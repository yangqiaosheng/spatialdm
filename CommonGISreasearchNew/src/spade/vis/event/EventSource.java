package spade.vis.event;

/**
* EventSource is a generic interface to be implemented by objects that
* can produce certain events. Implementation of the interface is necessary
* for event producers that have to be explicitly linked to event receivers.
* Events are differentiated by their identifiers, e.g. "MouseMove" or
* "ObjectSelect".
*/

public interface EventSource {
	/**
	* The EventSource answers whether it can produce the specified
	* type of events.
	*/
	public boolean doesProduceEvent(String eventId);

	/**
	* Returns a unique identifier of the event source (may be produced
	* automatically, used only internally, not shown to the user).
	*/
	public String getIdentifier();
}