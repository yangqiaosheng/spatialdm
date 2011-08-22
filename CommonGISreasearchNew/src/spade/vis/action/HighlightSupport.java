package spade.vis.action;

import java.util.Vector;

/**
* A HighlightSupport helps a Highlighter to notify HighlightListeners
* about changes of the set of highlighted objects.
* A Thread is needed because a new highlight changing event (e.g. mouse move)
* may occur while the previous highlighting has not been finished. The
* Highlighter should be always ready to listen to such events while
* the thread is busy with highlighting activities.
*/

public class HighlightSupport extends Thread {
	/**
	* The source of the current highlighting event.
	*/
	protected Object source = null;
	/**
	* HighlightListeners
	*/
	protected Vector hlist = null;
	/**
	* Highlighted objects
	*/
	protected Vector hlObjects = null;
	/**
	* The identifier of the set (e.g. map layer or table) the highlighted objects
	* belong to.
	*/
	public String setId = null;
	/**
	* This variable is true when the thread is running (i.e. inside the "run"
	* method).
	*/
	protected boolean running = false;
	/**
	* This is a control variable that says the thread that it should stop
	* as soon as possible.
	*/
	protected boolean mustStop = false;
	/**
	* Indicates whether the HighlightSupport should notify about change of
	* durable or transient highlighting.
	*/
	protected boolean applyToDurableHl = false;
	/**
	* A reference to the previous HighlightSupport. This HighlightSupport must
	* kill the previous one before starting any notification activity.
	*/
	protected HighlightSupport prevHS = null;

	/**
	* Constructs a HighlightSupport thread.
	* Creates a copy of the vector of HighlightListeners because while
	* highlighting is being done, the list of HighlightListeners may change.
	* For the same reason creates a copy of the list of highlighted objects.
	* The HighlightSupport sends this list to HighlighListeners and should be
	* ready to return it on demand.
	* "source" is the object that initialized highlighting.
	* setId is the identifier of the set (e.g. map layer or table) the objects
	* affected by the event belong to.
	* The argument applyToDurableHighlighting specifies whether the
	* HighlightSupport should  notify about change of
	* durable or of transient highlighting.
	*/
	public HighlightSupport(String name, Object source, Vector highlightListeners, Vector highlightedObjects, String setId, boolean applyToDurableHighlighting, HighlightSupport previousThread) {
		super(name);
		prevHS = previousThread;
		if (prevHS != null) {
			prevHS.stopRun();
		}
		this.source = source;
		hlist = highlightListeners;
		this.setId = setId;
		if (highlightedObjects == null || highlightedObjects.size() < 1) {
			hlObjects = null;
		} else {
			synchronized (highlightedObjects) {
				hlObjects = (Vector) highlightedObjects.clone();
			}
		}
		applyToDurableHl = applyToDurableHighlighting;
	}

	public synchronized boolean isRunning() {
		return running;
	}

	/**
	* Returns the vector of highlighted objects kept in this HighlightSupport
	*/
	public Vector getHighlightedObjects() {
		return hlObjects;
	}

	/**
	* Replies whether the HighlightSupport notifies about change of
	* durable or transient highlighting.
	*/
	public boolean isAppliedToDurableHl() {
		return applyToDurableHl;
	}

	/**
	* In its "run" method the HighlightSupport activates the corresponding
	* methods of HighlightListeners, in which they react to the changes of
	* highlighting.
	*/
	@Override
	public void run() {
		running = true;
		if (hlist == null || hlist.size() < 1) {
			running = false;
			return;
		}
		if (!applyToDurableHl && hlObjects != null) {
			try {
				sleep(200);
			} catch (InterruptedException e) {
			}
			if (mustStop) {
				running = false;
				return;
			}
		}
		if (prevHS != null) {
			while (prevHS.isRunning()) {
				try {
					sleep(100);
				} catch (InterruptedException e) {
				}
			}
		}
		if (mustStop && hlObjects != null) {
			running = false;
			return;
		}
		synchronized (hlist) {
			hlist = (Vector) hlist.clone();
		}
		if (mustStop && hlObjects != null) {
			running = false;
			return;
		}
		for (int i = 0; i < hlist.size() && !mustStop; i++) {
			HighlightListener hl = (HighlightListener) hlist.elementAt(i);
			try {
				if (applyToDurableHl) {
					hl.selectSetChanged(source, setId, hlObjects);
				} else {
					hl.highlightSetChanged(source, setId, hlObjects);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		running = false;
	}

	/**
	* Instead of the use of the method stop() of the basic class Thread (the
	* method is deprecated), this method sets an internal variable that is
	* regularly checked inside the body of the method run().
	*/
	public synchronized void stopRun() {
		mustStop = true;
		//System.out.println("thread "+getName()+" must stop!");
	}
}
