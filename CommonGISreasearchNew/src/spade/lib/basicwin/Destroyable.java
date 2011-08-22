package spade.lib.basicwin;

public interface Destroyable {
	/**
	* Makes necessary operations for destroying, e.g. unregisters from
	* listening highlighting and other events.
	*/
	public void destroy();

	/**
	* Replies whether is destroyed or not
	*/
	public boolean isDestroyed();
}