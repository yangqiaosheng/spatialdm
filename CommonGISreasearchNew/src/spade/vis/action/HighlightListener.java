package spade.vis.action;

import java.util.Vector;

/**
* A HighlightListener receives notifications when certain object(s) are to be
* highlighted (transient selection occurring on mouse move) or selected
* (durable selection occurring on mouse click or drag). The HighlightListener
* may display the object in a specific manner, or show the data associated with
* the object, or react in any other way to the selection event.
* A HighlightListener receives notifications about changes of the set
* of currently highlighted/selected objects, not about individual objects.
*/

public interface HighlightListener {
	/**
	* Notification about change of the set of objects to be transiently
	* highlighted. The argument "source" is usually a reference to a
	* Highlighter. The argument setId is the identifier of the set the
	* highlighted objects belong to (e.g. map layer or table).
	* The argument highlighted is a vector of identifiers of
	* currently highlighted objects.
	*/
	public void highlightSetChanged(Object source, String setId, Vector highlighted);

	/**
	* Notification about change of the set of objects to be selected (durably
	* highlighted). The argument "source" is usually a reference to a
	* Highlighter. The argument setId is the identifier of the set the
	* highlighted objects belong to (e.g. map layer or table).
	* The argument selected is a vector of identifiers of
	* currently selected objects.
	*/
	public void selectSetChanged(Object source, String setId, Vector selected);
}
