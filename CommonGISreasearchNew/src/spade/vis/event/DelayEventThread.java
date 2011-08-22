package spade.vis.event;

import java.util.EventObject;

/**
* The thread is used for delayed reaction to some events. The thread is invoked
* when an event occurs. It waits for a specified time and then, if not
* cancelled, calls the method DelayExpired of its owner. For example, this
* procedure can be used for capturing events of a mouse button being pressed
* for a long time.
*/

public class DelayEventThread extends Thread {
	/**
	* The time interval to wait (in milliseconds).
	*/
	protected long delay = 1000;
	/**
	* The original event that caused this thread being initiated.
	*/
	protected EventObject origEvent = null;
	/**
	* The thread owner that must be notified when the time interval has passed and
	* the thread has not been "cancelled".
	*/
	protected DelayedEventListener owner = null;
	/**
	* Indicates that the thread has been stopped, and it must not notify the
	* owner.
	*/
	protected boolean cancelled = false;

	/**
	* Creates a DelayEventThread. The first argument must be the original event.
	* The second argument is the owner that must be notified when the time
	* interval has passed but the thread has not been "cancelled".
	*/
	public DelayEventThread(EventObject event, DelayedEventListener owner) {
		origEvent = event;
		this.owner = owner;
	}

	/**
	* Sets the time to wait before notifying the owner
	*/
	public void setDelay(long time) {
		delay = time;
	}

	/**
	* Makes the thread "forget" the event, i.e. the thread will not notify
	* the owner after the time has passed.
	*/
	public void cancelEvent() {
		synchronized (this) {
			cancelled = true;
		}
	}

	/**
	* Waits for the specified time and then, if not cancelled, invokes the
	* method DelayExpired of the owner.
	*/
	@Override
	public void run() {
		if (cancelled || owner == null || origEvent == null)
			return;
		try {
			sleep(delay);
		} catch (InterruptedException ie) {
		}
		if (cancelled)
			return;
		owner.DelayExpired(origEvent);
	}
}