package spade.analysis.tools.schedule;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.Date;
import java.util.Vector;

import spade.analysis.calc.FindPegasus;
import spade.analysis.calc.Pegasus;
import spade.lib.util.CopyFile;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: 29-Oct-2007
 * Time: 11:48:27
 * A thread for starting the external scheduler and stopping it after
 * exceeding a specified time limit
 */
public class RunSchedulerThread extends Thread {
	/**
	 * The interface to the external scheduler
	 */
	protected Pegasus scheduler = null;
	/**
	 * Listens to the messages from the scheduler
	 */
	protected PropertyChangeListener listener = null;

	private boolean mustStop = false, running = false;
	/**
	 * The time to wait for a result, in milliseconds. The thread notifies
	 * the listener when this time has passed.
	 */
	protected long waitTime = 60000;
	/**
	 * The maximum allowed time for the scheduler to run, in milliseconds.
	 * The thread notifies the listener when this time has passed. After this,
	 * the listener may decide to kill the scheduler.
	 */
	protected long maxRunTime = 120000;
	/**
	 * The time when the scheduler has been started
	 */
	protected long startTime = 0l;

	public void setSchedulerListener(PropertyChangeListener listener) {
		this.listener = listener;
	}

	/**
	 * Sets the time to wait for a result, in milliseconds. The thread notifies
	 * the listener when this time has passed.
	 */
	public void setWaitTime(long waitTime) {
		this.waitTime = waitTime;
	}

	/**
	 * Sets te maximum allowed time for the scheduler to run, in milliseconds.
	 * The thread notifies the listener when this time has passed. After this,
	 * the listener may decide to kill the scheduler.
	 */
	public void setMaxRunTime(long maxRunTime) {
		this.maxRunTime = maxRunTime;
	}

	public boolean createScheduler() {
		startTime = (new Date()).getTime();
		try {
			scheduler = new Pegasus();
		} catch (Throwable e) {
			e.printStackTrace();
			scheduler = null;
			return false;
		}
		if (listener != null) {
			scheduler.setSchedulerListener(listener);
		}
		return true;
	}

	public String getPathToSchedulerLibrary() {
		return Pegasus.getPathToScheduler();
	}

	public boolean isRunning() {
		return running;
	}

	public void stopScheduler() {
		if (scheduler == null || !running)
			return;
		synchronized (this) {
			System.out.println("Killing");
			scheduler.kill();
			System.out.println("Killed");
			scheduler = null;
			mustStop = true;
			running = false;
		}
		if (startTime > 0) {
			//erase the temporary directory created by the scheduler
			Vector subdirs = CopyFile.getSubDirList(FindPegasus.getPathToPegasus());
			if (subdirs != null) {
				for (int i = 0; i < subdirs.size(); i++) {
					File subdir = (File) subdirs.elementAt(i);
					try {
						if (subdir.lastModified() >= startTime)
							if (!CopyFile.deleteDirectory(subdir.getAbsolutePath())) {
								subdir.deleteOnExit();
							}
					} catch (Exception e) {
					}
				}
			}
		}
	}

	@Override
	public void run() {
		if (scheduler == null)
			return;
		if (mustStop)
			return;
		synchronized (this) {
			running = true;
		}
		scheduler.startScheduler();
		try {
			sleep(waitTime);
		} catch (InterruptedException ie) {
		}
		;

		if (mustStop) {
			stopScheduler();
			return;
		}
		if (listener != null) {
			listener.propertyChange(new PropertyChangeEvent(this, "wait_time_passed", null, null));
		}

		if (maxRunTime > waitTime) {
			try {
				sleep(maxRunTime - waitTime);
			} catch (InterruptedException ie) {
			}
			;

			if (mustStop) {
				stopScheduler();
				return;
			}
			if (listener != null) {
				listener.propertyChange(new PropertyChangeEvent(this, "max_time_passed", null, null));
			}
		}
		while (!mustStop && scheduler != null && scheduler.quit == 0) {
			try {
				sleep(10000);
			} catch (InterruptedException ie) {
			}
		}
		;

		if (scheduler != null && scheduler.quit == 1) {
			stopScheduler();
		}

	}
}
