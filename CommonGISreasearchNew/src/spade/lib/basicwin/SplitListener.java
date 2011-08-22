package spade.lib.basicwin;

/**
* Interface to SplitLayout:
* inform all interested in border movements and component switching
*/
public interface SplitListener {
	public static final int dragToLeft = -1, dragToCenter = 0, dragToRight = 1;

	public abstract void mouseDragged(Object source, int x, int y);

	public abstract void stopDrag();

	public abstract void arrowClicked(Object source, int where);

	public abstract void bulletClicked(Object source);
}