package core;

import java.util.Vector;

import spade.analysis.system.EventBrokeringSupervisor;
import spade.vis.action.ObjectEvent;
import spade.vis.event.EventBroker;
import spade.vis.event.EventReceiver;
import spade.vis.event.EventSource;

/**
* A Supervisor controls linkage between multiple views. This variant of
* Supervisor supports brokering of object click events, i.e. explicit setting
* of links between event sources and event receivers.
*/
public class SupervisorObjectSelectBroker extends SupervisorImplement implements EventBrokeringSupervisor {
	/**
	* The brokers of object clicking and object framing events
	*/
	protected EventBroker brokers[] = null;

	/**
	* In the constructor creates an EventBroker for the object click events
	* and links it to the ObjectEventDispatcher
	*/
	public SupervisorObjectSelectBroker() {
		super();
		brokers = new EventBroker[2];
		brokers[0] = new EventBroker();
		brokers[0].setEventType(ObjectEvent.click, ObjectEvent.getEventTypeName(ObjectEvent.click));
		brokers[0].setEventMeaning("click_man", "display manipulation");
		brokers[1] = new EventBroker();
		brokers[1].setEventType(ObjectEvent.frame, ObjectEvent.getEventTypeName(ObjectEvent.frame));
		brokers[1].setEventMeaning("frame_man", "grouping of objects");
		brokers[0].setEventManager(odisp.getEventManager());
		brokers[1].setEventManager(odisp.getEventManager());
	}

	/**
	* If the implementation of the Supervisor supports brokering of object events
	* of the specified type between components (i.e. explicit setting of links
	* from event producers to recipients), the method returns an instance of
	* EventBroker. In all other cases the method returns null.
	*/
	@Override
	public Object getObjectEventBroker(String objEventId) {
		if (objEventId != null) {
			for (EventBroker broker : brokers)
				if (objEventId.equals(broker.getEventType()))
					return broker;
		}
		return null;
	}

	/**
	* Returns a vector of all event brokers used
	*/
	@Override
	public Vector getObjectEventBrokers() {
		if (brokers == null)
			return null;
		Vector v = new Vector(brokers.length, 5);
		for (EventBroker broker : brokers) {
			v.addElement(broker);
		}
		return v;
	}

	/**
	* Registers a producer of object events. If the implementation of the
	* Supervisor has some EventBrokers, the producer is readressed to these
	* EventBrokers. Each EventBroker checks itself if the producer
	* can produce the type of events the broker is responsible for. If yes, the
	* broker registers the producer.
	*/
	@Override
	public void registerObjectEventSource(EventSource es) {
		for (EventBroker broker : brokers) {
			broker.addEventSource(es);
		}
	}

	/**
	* Unregister the producer of object events (actually makes all EventBrokers
	* unregister it)
	*/
	@Override
	public void removeObjectEventSource(EventSource es) {
		for (EventBroker broker : brokers) {
			broker.removeEventSource(es);
		}
	}

	/**
	* Registers a display manipulator that should implement the interface
	* EventReceiver. In practice, readresses the manipulator to the available
	* EventBrokers in order to make the manipulator able to process object
	* events from different components.
	*/
	@Override
	public void registerDisplayManipulator(EventReceiver oer) {
		for (EventBroker broker : brokers) {
			broker.addEventReceiver(oer);
		}
	}

	@Override
	public void removeDisplayManipulator(EventReceiver oer) {
		for (EventBroker broker : brokers) {
			broker.removeEventReceiver(oer);
		}
	}
}