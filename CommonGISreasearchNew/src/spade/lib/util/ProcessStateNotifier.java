package spade.lib.util;

import java.util.Vector;

/**
* A utility class used for registering listeners of processes and notification
* of them about the state of the processes.
*/

public class ProcessStateNotifier {
	/**
	* Listeners of the state of the process using this Notifier (a process may be
	* time consuming, so, probably, the status of it should be shown somewhere).
	*/
	private Vector plist = null;

	/**
	* Adds a listener of the process - owner of this Notifier
	*/
	public void addProcessListener(ProcessListener lst) {
		if (lst == null)
			return;
		if (plist == null) {
			plist = new Vector(5, 5);
		}
		if (plist.contains(lst))
			return;
		plist.addElement(lst);
	}

	/**
	* Notifies all listeners about the status of the process. "trouble" indicates
	* whether there is an error or other trouble in the course of the process,
	* or this is just a normal work notification.
	*/
	public void notifyProcessState(Object source, String processName, String processState, boolean trouble) {
		if (plist == null || plist.size() < 1)
			return;
		for (int i = 0; i < plist.size(); i++) {
			ProcessListener lst = (ProcessListener) plist.elementAt(i);
			lst.receiveNotification(source, processName, processState, trouble);
		}
	}
}