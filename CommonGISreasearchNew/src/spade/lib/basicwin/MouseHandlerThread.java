package spade.lib.basicwin;

import java.awt.Window;

public class MouseHandlerThread extends Thread {
	public static long popupDelay = 500; // in millis
	boolean mouseRelocated = false, finished = false;
	protected Window owner = null;

	public MouseHandlerThread(Window win) {
		owner = win;
	}

	@Override
	public void run() {
		if (mouseRelocated)
			return;
		try {
			sleep(popupDelay);
		} catch (InterruptedException e) {
		}
		synchronized (this) {
			if (!mouseRelocated) {
				synchronized (owner) {
					owner.pack();
					owner.show();
					owner.toFront();
					//System.out.println("popup: visible="+owner.isShowing());
				}
			}
		}
		finished = true;
	}
}
