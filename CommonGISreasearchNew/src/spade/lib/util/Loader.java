package spade.lib.util;

/**
* A loader is a thread that calls the method loadAll() of some object
* implementing the interface Loadable. As a result, this method can run
* in parallel with other activities. This is especially useful when at the
* beginning of the work of an applet some data should be loaded, and the
* process of loading takes some time. The applet should reflect the
* state of the loading process, for example, in a status bar. However,
* the status bar will not appear until the method start() of the applet
* finishes. To make the status bar appear and show some information while
* data are loaded, the applet can initiate a Loader and immediately return
* from the start() method. 
*/

public class Loader extends Thread {
	protected Loadable loadable = null;

	public void setLoadable(Loadable l) {
		loadable = l;
	}

	@Override
	public void run() {
		if (loadable == null)
			return;
		loadable.loadAll();
	}
}