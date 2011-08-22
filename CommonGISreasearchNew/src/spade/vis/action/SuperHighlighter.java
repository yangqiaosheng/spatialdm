package spade.vis.action;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Vector;

import spade.lib.util.EntitySetIdManager;
import spade.lib.util.StringUtil;

/**
* A SuperHighlighter manages highlighting of objects belonging to several
* objec sets (such as map layers). Each object set must have its unique
* identifier. For each object set the SuperHighlighter creates a Highlighter.
* When an object event occurs, the SuperHighlighter transfers it to the
* appropriate Highlighter (each object event contains the identifier of
* the set the affected objects belong to).
*/

public class SuperHighlighter implements ObjectEventHandler, PropertyChangeListener {
	/**
	* Vector of Highlighters. Highlighters for different object sets are
	* created when needed (when a new object set identifier comes).
	*/
	protected Vector highlighters = null;
	/**
	* Provides information about identifiers of entity sets and notifies about
	* their changes.
	* A user may decide to link a table with a map layer after he/she worked with
	* the table for a while. After linking the entity set identifier of the
	* table changes. If a highlighter for the old identifier existed, the
	* SuperHighlighter must manage the resulting situation.
	*/
	protected EntitySetIdManager setIdMan = null;

	/**
	* Finds a Highlighter dealing with the specified object set.
	* If such a Highlighter does not exist yet, returns null.
	*/
	public Highlighter findHighlighter(String objSetId) {
		Highlighter hl = null;
		if (highlighters != null) {
			for (int i = 0; i < highlighters.size() && hl == null; i++) {
				hl = (Highlighter) highlighters.elementAt(i);
				if (!StringUtil.sameStrings(objSetId, hl.getEntitySetIdentifier())) {
					hl = null;
				}
			}
		}
		return hl;
	}

	/**
	* Returns the Highlighter dealing with the specified object set.
	* If such a Highlighter does not exist yet, it is created.
	*/
	public Highlighter getHighlighter(String objSetId) {
		Highlighter hl = findHighlighter(objSetId);
		if (hl == null) {
			hl = new Highlighter();
			hl.setEntitySetIdentifier(objSetId);
			if (highlighters == null) {
				highlighters = new Vector(5, 5);
			}
			highlighters.addElement(hl);
		}
		return hl;
	}

	public int getHighlighterCount() {
		if (highlighters == null)
			return 0;
		return highlighters.size();
	}

	public Highlighter getHighlighter(int idx) {
		if (idx < 0 || idx >= getHighlighterCount())
			return null;
		return (Highlighter) highlighters.elementAt(idx);
	}

	/**
	* When some ObjectEvent occurs (object pointed or object clicked), the
	* SuperHighlighter transfers it to the appropriate highlighter according
	* to the object set identifier.
	*/
	@Override
	public void processObjectEvent(ObjectEvent oevt) {
		if (oevt == null)
			return;
		getHighlighter(oevt.getSetIdentifier()).processObjectEvent(oevt);
	}

	/**
	* Sets a reference to the object that provides information about identifiers
	* of entity sets and notifies about their changes. The SuperHighlighter
	* registers as a listener of its property changes.
	*/
	public void setEntitySetIdManager(EntitySetIdManager man) {
		setIdMan = man;
		if (setIdMan != null) {
			setIdMan.addPropertyChangeListener(this);
		}
	}

	/**
	* Reacts to a change of an entity set identifier.
	* A user may decide to link a table with a map layer after he/she worked with
	* the table for a while. After linking the entity set identifier of the
	* table changes. If a highlighter for the old identifier existed, the
	* SuperHighlighter must manage the resulting situation.
	*/
	@Override
	public void propertyChange(PropertyChangeEvent pce) {
		if (pce.getPropertyName().equals("set_id") && setIdMan != null) {
			String oldId = (String) pce.getOldValue(), newId = (String) pce.getNewValue();
			if (oldId == null || newId == null)
				return;
			if (setIdMan.anyContainerRefersTo(oldId))
				return;
			Highlighter hl1 = findHighlighter(oldId);
			if (hl1 == null)
				return;
			Highlighter hl2 = findHighlighter(newId);
			if (hl2 == null) {
				hl1.setEntitySetIdentifier(newId);
				return;
			}
			//move highlight listeners from hl1 to hl2
			Vector list = hl1.getListeners();
			if (list != null && list.size() > 0) {
				list = (Vector) list.clone();
				for (int i = 0; i < list.size(); i++) {
					HighlightListener l = (HighlightListener) list.elementAt(i);
					hl1.removeHighlightListener(l);
					hl2.addHighlightListener(l);
				}
			}
			highlighters.removeElement(hl1);
		}
	}
}
