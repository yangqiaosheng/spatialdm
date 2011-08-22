package spade.analysis.system;

import java.util.Vector;

import spade.vis.event.EventReceiver;

/**
* A Supervisor controls linkage between multiple views. This variant of
* Supervisor supports event brokering, i.e. explicit setting of links between
* event sources and event receivers.
*/

public interface EventBrokeringSupervisor {
	/**
	* If the implementation of the Supervisor supports brokering of object events
	* of the specified type between components (i.e. explicit setting of links
	* from event producers to recipients), the method returns an instance of
	* EventBroker. In all other cases the method returns null.
	*/
	public Object getObjectEventBroker(String objEventId);

	/**
	* Returns a vector of all event brokers used
	*/
	public Vector getObjectEventBrokers();

	/**
	* Registers a display manipulator that should implement the interface
	* EventReceiver. In practice, readresses the manipulator to the available
	* EventBrokers in order to make the manipulator able to process object
	* events from different components.
	*/
	public void registerDisplayManipulator(EventReceiver oer);

	public void removeDisplayManipulator(EventReceiver oer);
}
