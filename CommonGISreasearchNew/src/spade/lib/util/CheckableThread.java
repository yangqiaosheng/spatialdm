package spade.lib.util;

/**
* A Thread implementing this interface allows one to check whether the thread
* is running or not.
*/
public interface CheckableThread {
	/**
	* Reports whether the thread is currently running
	*/
	public boolean isRunning();
}