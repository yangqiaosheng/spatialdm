package spade.lib.util;

public interface ProcessListener {
	public void receiveNotification(Object source, String processName, String processState, boolean trouble);
}