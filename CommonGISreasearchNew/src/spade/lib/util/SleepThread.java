package spade.lib.util;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Feb 24, 2009
 * Time: 10:24:28 AM
 * Sleeps for a specified amount of time, then sends an event to the
 * listener.
 */
public class SleepThread extends Thread {
	/**
	* The listener to be notified after the time interval has passed
	*/
	protected PropertyChangeListener listener = null;
	/**
	* The time interval in milliseconds
	*/
	protected long interval = 100L;
	/**
	 * Indicates that the thread must stop, i.e. must not send
	 * a notification when the time has passed.
	 */
	protected boolean mustStop = false;

	/**
	 * Constructs the thread with passing it the listener to be notified
	 * and the time interval in milliseconds
	 */
	public SleepThread(PropertyChangeListener listener, long interval) {
		this.listener = listener;
		this.interval = interval;
	}

	@Override
	public void run() {
		try {
			sleep(interval);
		} catch (Exception ex) {
		}
		if (mustStop)
			return;
		PropertyChangeEvent e = new PropertyChangeEvent(this, "time_passed", null, null);
		listener.propertyChange(e);
	}

	public void setMustStop() {
		mustStop = true;
	}
}
