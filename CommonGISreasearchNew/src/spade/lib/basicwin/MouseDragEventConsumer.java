package spade.lib.basicwin;

/**
* This interface is introduce for convenience purposes. It can be used in cases
* when some component need to process mouse drag events occurring in another
* component. In this case, the component where the mouse events occur may
* (when certain conditions are fulfilled) pass them to the component which
* processes them. For this kind of "conditional" behaviour, this interface
* is more suitable than the standard MouseListener and MouseMotionListener.
*/
public interface MouseDragEventConsumer {
	/**
	* Used to inform the consumer about a start of mouse dragging.
	* x0 and y0 are the mouse coordinates at the moment when the mouse button
	* was pressed, x and y are the current mouse coordinates.
	*/
	public void mouseDragBegin(int x0, int y0, int x, int y);

	/**
	* Used to inform the consumer about a continuation of mouse dragging.
	* x1 and y1 are the old mouse coordinates, x2 and y2 are the new coordinates.
	*/
	public void mouseDragging(int x1, int y1, int x2, int y2);

	/**
	* Used to inform the consumer about the end of mouse dragging.
	* x and y are the mouse coordinates at the moment when the mouse button
	* was released.
	*/
	public void mouseDragEnd(int x, int y);
}