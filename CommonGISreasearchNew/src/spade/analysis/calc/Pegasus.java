package spade.analysis.calc;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: 29-Oct-2007
 * Time: 09:40:50
 * Starts the external scheduler (Pegasus) and receives progress
 * messages from it.
 */
public class Pegasus {
	public int quit = 0;
	/**
	 * The path to the external scheduler
	 */
	protected static String pathToScheduler = null; //"pegasus";
	/**
	 * Listens to the messages from the scheduler
	 */
	protected PropertyChangeListener listener = null;

	static {
		pathToScheduler = FindPegasus.getPegasusLibName();
		System.out.println("Before");
		System.loadLibrary(pathToScheduler + "/pegasus");
		System.out.println("after");
	}

	public native int initialize(String path);

	public native int kill();

	public native int pegasus(String path);

	public void pegasusPrintf(String s) {
		System.out.println(s);
	}

	public void setSchedulerListener(PropertyChangeListener listener) {
		this.listener = listener;
	}

	public static String getPathToScheduler() {
		return pathToScheduler;
	}

	public void startScheduler() {
		initialize(pathToScheduler);
	}

	public void pegasusCallback(String message1, String message2, int generation) {
		System.out.println("Callback from native: " + message1 + ":" + message2 + ", Generation: " + generation);
		if (listener != null) {
			listener.propertyChange(new PropertyChangeEvent(this, "scheduler_" + generation, message1, message2));
		}
	}

	public void pegasusSignal(int signal) {
		System.out.println("Scheduler signal: " + signal);
		if (signal == 0) {
			quit = 1;
			if (listener != null) {
				listener.propertyChange(new PropertyChangeEvent(this, "scheduler_finish", null, null));
			}
		} else if (listener != null) {
			listener.propertyChange(new PropertyChangeEvent(this, "scheduler_signal", null, null));
		}
	}

}
