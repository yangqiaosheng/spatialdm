package spade.vis.action;

import java.util.Vector;

import spade.lib.util.StringUtil;

/**
* Highlighter is an object that receives events informing that certain objects
* are pointed to or clicked with the mouse (ObjectEvents).
* In response the Highlighter marks these objects as (transiently) highlighted
* or selected (durably highlighted) and notifies all registered
* HighlightListeners that these objects are to be highlighted/selected.
* Point or click in an object-free area results in deghighlighting/deselecting
* of all currently highlighted/selected objects.
* A Highlighter is always attached to a single object set such as the objects
* contained in one map layer or in one table. The set is specified by its
* identifier. A table and a map layer it is linked to refer to the same object
* set. If there are several tables referring to the same object set, they
* should have the same set identifier.
*/

public class Highlighter implements ObjectEventHandler {
	/**
	* Vector of HighlightListeners.
	*/
	protected Vector hlist = null;

	/**
	* Registers a HighlightListener.
	*/
	public void addHighlightListener(HighlightListener lst) {
		//System.out.println("Highlighter registers "+lst.toString());
		//System.out.println("setId="+setId);
		if (lst == null)
			return;
		if (hlist == null) {
			hlist = new Vector(10, 5);
		}
		if (!hlist.contains(lst)) {
			hlist.addElement(lst);
		}
	}

	/**
	* Unregisters the HighlightListener.
	*/
	public void removeHighlightListener(HighlightListener lst) {
		if (lst == null || hlist == null)
			return;
		int idx = hlist.indexOf(lst);
		if (idx >= 0) {
			hlist.removeElementAt(idx);
			System.out.println("Highlighter: removed " + lst);
		}
	}

	/**
	* Returns a vector of currently registered listeners of highlighting
	*/
	public Vector getListeners() {
		return hlist;
	}

	/**
	* The identifier of the object set the highlighter deals with
	*/
	protected String setId = null;
	/**
	* Vector of identifiers of currently highlighted objects.
	*/
	protected Vector highlighted = null;
	/**
	* Vector of identifiers of currently selected (durably highlighted) objects.
	* Each identifier has a format <set_id>;<obj_id>, where <obj_id> is the
	* individual identifier of the object and <set_id> is the identifier of the
	* set this object belongs to.
	*/
	protected Vector selected = null;

	/**
	* Sets the identifier of the object set the highlighter deals with
	*/
	public void setEntitySetIdentifier(String ident) {
		setId = ident;
	}

	/**
	* Returns the identifier of the object set the highlighter deals with
	*/
	public String getEntitySetIdentifier() {
		return setId;
	}

	/**
	* Returns the vector of identifiers of currently highlighted objects
	*/
	public Vector getHighlightedObjects() {
		if (transHS != null)
			return transHS.getHighlightedObjects();
		return highlighted;
	}

	/**
	* Returns the vector of identifiers of currently selected (durably
	* highlighted) objects.
	*/
	public Vector getSelectedObjects() {
		if (durHS != null)
			return durHS.getHighlightedObjects();
		return selected;
	}

	/**
	* Reports whether the specified object is currently highlighted.
	*/
	public boolean isObjectHighlighted(String objId) {
		if (objId == null)
			return false;
		Vector hl = getHighlightedObjects();
		if (hl == null)
			return false;
		return StringUtil.isStringInVectorIgnoreCase(objId, hl);
	}

	/**
	* Reports whether the specified object is currently selected.
	*/
	public boolean isObjectSelected(String objId) {
		if (objId == null || selected == null)
			return false;
		Vector hl = getSelectedObjects();
		if (hl == null)
			return false;
		return StringUtil.isStringInVectorIgnoreCase(objId, hl);
	}

	//--------------------- highlighting activities -------------------
	private HighlightSupport transHS = null, durHS = null;
	private int clearHN = 0, clearSN = 0, hlN = 0, selN = 0;

	/**
	* Removes highlighting of the all currently highlighted objects and
	* informs all the HighlightListeners about the change of highlighting.
	* Uses a HighlightSupport that runs as an autonomous thread.
	* "source" is the object (system component) that requests highlighting
	* to be cleared.
	*/
	public void clearHighlighting(Object source) {
		if (highlighted == null || highlighted.size() < 1)
			return;
		highlighted.removeAllElements();
		if (hlist == null)
			return;
		transHS = new HighlightSupport("clear highlight " + (++clearHN), source, hlist, highlighted, setId, false, transHS);
		transHS.start();
		//System.out.println("Highlighted started the thread "+transHS.getName());
	}

	/**
	* Removes selection of the all currently selected objects and
	* informs all the HighlightListeners about the change of highlighting.
	* Uses a HighlightSupport that runs as an autonomous thread.
	* "source" is the object (system component) that requests the selection to be
	* cleared.
	*/
	public void clearSelection(Object source) {
		if (selected == null || selected.size() < 1)
			return;
		selected.removeAllElements();
		if (hlist == null)
			return;
		durHS = new HighlightSupport("clear selection " + (++clearSN), source, hlist, selected, setId, true, durHS);
		durHS.start();
	}

	/**
	* Marks the specified objects as highlighted and informs all the
	* HighlightListeners about the change of highlighting.
	* Uses a HighlightSupport that runs as an autonomous thread.
	* "source" is the object (system component) that initialized highlighting.
	*/
	public void makeObjectsHighlighted(Object source, Vector objIds) {
		if (objIds == null || objIds.size() < 1)
			return;
		//possibly, the set of highlighted objects did not really change?
		if (highlighted != null && highlighted.size() == objIds.size()) {
			boolean changed = false;
			for (int i = 0; i < objIds.size() && !changed; i++) {
				changed = !StringUtil.isStringInVectorIgnoreCase((String) objIds.elementAt(i), highlighted);
			}
			if (!changed)
				return;
		}
		//first clears the existing highlighting, if any
		if (highlighted == null) {
			highlighted = new Vector(20, 10);
		} else {
			highlighted.removeAllElements();
		}
		int nobj = objIds.size();
		if (nobj > 20) {
			nobj = 20; //to avoid "hanging"
		}
		for (int i = 0; i < nobj; i++) {
			highlighted.addElement(objIds.elementAt(i));
		}
		if (hlist == null)
			return; //no highlight listeners
		transHS = new HighlightSupport("highlight " + (++hlN), source, hlist, highlighted, setId, false, transHS);
		transHS.start();
		//System.out.println("Highlighted started the thread "+transHS.getName());
	}

	/**
	* Marks the specified objects as selected (durably highlighted), if they
	* have not been previously selected, or deselects them, if they have
	* been selected.
	* Informs all the HighlightListeners about the change of highlighting.
	* Uses a HighlightSupport that runs as an autonomous thread.
	* "source" is the object (system component) that initialized selection.
	*/
	public void makeObjectsSelected(Object source, Vector objIds) {
		if (objIds == null || objIds.size() < 1)
			return;
		if (selected == null) {
			selected = new Vector(20, 10);
		}
		//are all the objects already selected?
		boolean allSelected = true;
		for (int i = 0; i < objIds.size() && allSelected; i++) {
			allSelected = StringUtil.isStringInVectorIgnoreCase((String) objIds.elementAt(i), selected);
		}
		if (allSelected) {
			for (int i = 0; i < objIds.size(); i++) {
				selected.removeElement(objIds.elementAt(i));
			}
		} else {
			for (int i = 0; i < objIds.size(); i++) {
				String id = (String) objIds.elementAt(i);
				if (!isObjectSelected(id)) {
					selected.addElement(id);
				}
			}
		}
		if (hlist == null)
			return;
		durHS = new HighlightSupport("select " + (++selN), source, hlist, selected, setId, true, durHS);
		durHS.start();
	}

	/**
	* Only the objects from the given list are selected. All previously
	* selected objects become deselected.
	* "source" is the object (system component) that changes selection.
	*/
	public void replaceSelectedObjects(Object source, Vector objIds) {
		if (selected == null) {
			selected = new Vector(20, 10);
		} else {
			selected.removeAllElements();
		}
		if (objIds != null) {
			for (int i = 0; i < objIds.size(); i++) {
				selected.addElement(objIds.elementAt(i));
			}
		}
		if (hlist == null)
			return;
		durHS = new HighlightSupport("select " + (++selN), source, hlist, selected, setId, true, durHS);
		durHS.start();
	}

	/**
	* When some ObjectEvent occurs (object pointed or object clicked), the
	* Highlighter fires object highlighting.
	*/
	@Override
	public void processObjectEvent(ObjectEvent oevt) {
		if (oevt == null)
			return;
		String type = oevt.getType();
		Vector evtObj = oevt.getAffectedObjects();
		if (hlist != null) {
			synchronized (hlist) {
				int idx = hlist.indexOf(oevt.getSource());
				if (idx > 0) { //the source of the event should be the first to receive
					//the notification
					hlist.removeElementAt(idx);
					hlist.insertElementAt(oevt.getSource(), 0);
				}
			}
		}
		if (type.equals(ObjectEvent.point)) { //transient highlighting
			if (evtObj == null || evtObj.size() < 1) {
				clearHighlighting(oevt.getSource()); //clears the existing highlighting
			} else {
				makeObjectsHighlighted(oevt.getSource(), evtObj);
				//clears the existing highlighting inside the function
			}
		} else if (evtObj == null || evtObj.size() < 1) {//action in an object-free area
			if (type.equals(ObjectEvent.dblClick)) {
				clearSelection(oevt.getSource()); //clears the existing selection
			}
		} else if (type.equals(ObjectEvent.click) || type.equals(ObjectEvent.frame)) {
			//durable highlighting (selection)
			makeObjectsSelected(oevt.getSource(), evtObj); //adds the objects to the list of currently
			//selected objects or removes them if they were there before
		}
	}
}
