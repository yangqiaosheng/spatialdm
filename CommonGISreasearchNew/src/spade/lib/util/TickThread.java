package spade.lib.util;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
* While some specified thread is running, sends "tick" messages with a specified
* interval to some PropertyChangeListener
*/
public class TickThread extends Thread {
	/**
	* The thread to be controlled
	*/
	protected CheckableThread thr = null;
	/**
	* The listener to be regularly notified (after time intervals of a fixed length)
	*/
	protected PropertyChangeListener listener = null;
	/**
	* The time interval (in milliseconds) between sending notifications
	*/
	protected long interval = 100L;

	/**
	* Constructs the thread with passing it the thread to be controlled,
	* the listener to be notified, and the time interval (in milliseconds) between
	* sending notifications
	*/
	public TickThread(CheckableThread thread, PropertyChangeListener listener, long interval) {
		thr = thread;
		this.listener = listener;
		this.interval = interval;
	}

	@Override
	public void run() {
		long time = 0L;
		while (thr.isRunning()) {
			try {
				sleep(interval);
			} catch (Exception ex) {
			}
			if (!thr.isRunning()) {
				break;
			}
			time += interval;
			PropertyChangeEvent e = new PropertyChangeEvent(this, "tick", null, new Long(time));
			listener.propertyChange(e);
		}
	}
}