package spade.lib.basicwin;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

/**
* Used for registering action listeners of some source of action events
* and sending action events to these listeners.
*/

public class ActionSupport {
	/**
	* The vector of registered action listeners
	*/
	protected Vector listeners = null;

	public void addActionListener(ActionListener lst) {
		if (lst == null)
			return;
		if (listeners == null) {
			listeners = new Vector(5, 5);
		}
		if (!listeners.contains(lst)) {
			listeners.addElement(lst);
		}
	}

	public void removeActionListener(ActionListener lst) {
		if (lst == null || listeners == null)
			return;
		int idx = listeners.indexOf(lst);
		if (idx >= 0) {
			listeners.removeElementAt(idx);
		}
	}

	public void fireActionEvent(Object source, String command) {
		PopupManager.hideWindow();
		if (listeners == null || listeners.size() < 1)
			return;
		ActionEvent ae = new ActionEvent(source, ActionEvent.ACTION_PERFORMED, command);
		for (int i = 0; i < listeners.size(); i++) {
			((ActionListener) listeners.elementAt(i)).actionPerformed(ae);
		}
	}
}
