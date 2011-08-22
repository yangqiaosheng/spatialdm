package spade.lib.util;

import java.beans.PropertyChangeSupport;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: May 28, 2010
 * Time: 1:40:20 PM
 * Used for sending property change messages using property change support
 */
public class NotifyThread extends Thread {
	protected PropertyChangeSupport pcSupport = null;
	protected String propName = null;
	protected Object oldValue = null, newValue = null;

	public NotifyThread(PropertyChangeSupport pcSupport, String propName, Object oldValue, Object newValue) {
		this.pcSupport = pcSupport;
		this.propName = propName;
		this.oldValue = oldValue;
		this.newValue = newValue;
	}

	@Override
	public void run() {
		if (pcSupport == null)
			return;
		pcSupport.firePropertyChange(propName, oldValue, newValue);
	}
}
