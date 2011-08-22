package spade.vis.event;

import java.util.EventObject;

/**
* The interface is used for delayed reaction to some events using a
* DelayEventThread. The thread is invoked when an event occurs. It waits for
* a specified time and then, if not cancelled, calls the method DelayExpired
* of the listener. For example, this procedure can be used for capturing events
* of a mouse button being pressed for a long time.
*/

public interface DelayedEventListener {

	public void DelayExpired(EventObject me);
}