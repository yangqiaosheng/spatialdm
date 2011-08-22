package spade.analysis.space_time_cube;

import java.util.Vector;

import javax.media.j3d.Appearance;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.LineAttributes;
import javax.media.j3d.PointAttributes;

import spade.analysis.system.Supervisor;
import spade.lib.basicwin.Destroyable;
import spade.lib.util.IntArray;
import spade.lib.util.StringUtil;
import spade.vis.action.HighlightListener;
import spade.vis.database.ObjectContainer;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Apr 29, 2008
 * Time: 5:10:51 PM
 * Applies selection to java3D shapes representing spatio-temporal objects
 */
public class SelectionApplicator implements HighlightListener, Destroyable {
	/**
	 * The container with the original objects
	 */
	protected ObjectContainer oCont = null;
	/**
	 * The drawn 3D objects, instances of SpaceTimeObject
	 */
	protected Vector<SpaceTimeObject> drawnObjects = null;
	/**
	 * Propagates highlighting and selection of objects
	 */
	protected Supervisor supervisor = null;
	/**
	 * Identifiers of the currently selected objects in the vector drawnObjects
	 */
	protected IntArray selIdxs = null;
	/**
	 * The appearance of a selected object
	 */
	protected Appearance selAppearance = null;
	/**
	 * The original appearances of the selected objects
	 */
	protected Vector origAppsSelected = null;
	/**
	 * Identifiers of the currently selected objects in the vector drawnObjects
	 */
	protected IntArray hlIdxs = null;
	/**
	 * The appearance of a selected object
	 */
	protected Appearance hlAppearance = null;
	/**
	 * The original appearances of the selected objects
	 */
	protected Vector origAppsHighlighted = null;

	public void setObjectContainer(ObjectContainer oCont) {
		this.oCont = oCont;
		if (supervisor != null && oCont != null) {
			supervisor.registerHighlightListener(this, oCont.getEntitySetIdentifier());
		}
	}

	public void setDrawnObjects(Vector<SpaceTimeObject> drawnObjects) {
		this.drawnObjects = drawnObjects;
	}

	/**
	* Sets a reference to the system's supervisor used for propagating events
	* among system's components.
	*/
	public void setSupervisor(Supervisor sup) {
		supervisor = sup;
		if (supervisor != null && oCont != null) {
			supervisor.registerHighlightListener(this, oCont.getEntitySetIdentifier());
		}
	}

	public void applySelection() {
		if (supervisor == null || oCont == null || drawnObjects == null || drawnObjects.size() < 1)
			return;
		Vector selected = supervisor.getHighlighter(oCont.getEntitySetIdentifier()).getSelectedObjects();
		if (selected == null || selected.size() < 1) {
			if (selIdxs == null || selIdxs.size() < 1)
				return;
			for (int i = 0; i < selIdxs.size(); i++) {
				SpaceTimeObject stobj = drawnObjects.elementAt(selIdxs.elementAt(i));
				stobj.setAppearance((Appearance) origAppsSelected.elementAt(i));
			}
			selIdxs.removeAllElements();
			origAppsSelected.removeAllElements();
			return;
		}
		if (selAppearance == null) {
			selAppearance = new Appearance();
			ColoringAttributes ca = new ColoringAttributes();
			ca.setColor(0, 0, 0);
			selAppearance.setColoringAttributes(ca);
			LineAttributes la = new LineAttributes();
			la.setLineWidth(3);
			selAppearance.setLineAttributes(la);
			selAppearance.setPointAttributes(new PointAttributes(SpaceTimeObject.pointSize, true));
		}
		if (selIdxs == null) {
			selIdxs = new IntArray(50, 50);
			origAppsSelected = new Vector(50, 50);
		}
		for (int i = 0; i < drawnObjects.size(); i++) {
			SpaceTimeObject stobj = drawnObjects.elementAt(i);
			if (StringUtil.isStringInVectorIgnoreCase(stobj.getGeoObjectId(), selected))
				if (selIdxs.indexOf(i) < 0) {
					selIdxs.addElement(i);
					Appearance app = stobj.getAppearance();
					if (hlIdxs != null) {
						int hidx = hlIdxs.indexOf(i);
						if (hidx >= 0) {
							app = (Appearance) origAppsHighlighted.elementAt(hidx);
						}
					}
					origAppsSelected.addElement(app);
					stobj.setAppearance(selAppearance);
				} else {
					;
				}
			else {
				int idx = selIdxs.indexOf(i);
				if (idx >= 0) {
					Appearance origApp = (Appearance) origAppsSelected.elementAt(idx);
					origAppsSelected.removeElementAt(idx);
					selIdxs.removeElementAt(idx);
					stobj.setAppearance(origApp);
				}
			}
		}
	}

	/**
	* Notification about change of the set of objects to be transiently
	* highlighted. The argument "source" is usually a reference to a
	* Highlighter. The argument setId is the identifier of the set the
	* highlighted objects belong to (e.g. map layer or table).
	* The argument highlighted is a vector of identifiers of
	* currently highlighted objects.
	*/
	@Override
	public void highlightSetChanged(Object source, String setId, Vector highlighted) {
		if (highlighted == null || highlighted.size() < 1) {
			if (hlIdxs == null || hlIdxs.size() < 1)
				return;
			for (int i = 0; i < hlIdxs.size(); i++) {
				int oIdx = hlIdxs.elementAt(i);
				SpaceTimeObject stobj = drawnObjects.elementAt(oIdx);
				if (selIdxs != null && selIdxs.indexOf(oIdx) >= 0) {
					stobj.setAppearance(selAppearance);
				} else {
					stobj.setAppearance((Appearance) origAppsHighlighted.elementAt(i));
				}
			}
			hlIdxs.removeAllElements();
			origAppsHighlighted.removeAllElements();
			return;
		}
		if (hlAppearance == null) {
			hlAppearance = new Appearance();
			ColoringAttributes ca = new ColoringAttributes();
			ca.setColor(1, 1, 1);
			hlAppearance.setColoringAttributes(ca);
			LineAttributes la = new LineAttributes();
			la.setLineWidth(3);
			hlAppearance.setLineAttributes(la);
			hlAppearance.setPointAttributes(new PointAttributes(SpaceTimeObject.pointSize, true));
		}
		if (hlIdxs == null) {
			hlIdxs = new IntArray(50, 50);
			origAppsHighlighted = new Vector(50, 50);
		}
		for (int i = 0; i < drawnObjects.size(); i++) {
			SpaceTimeObject stobj = drawnObjects.elementAt(i);
			if (StringUtil.isStringInVectorIgnoreCase(stobj.getGeoObjectId(), highlighted))
				if (hlIdxs.indexOf(i) < 0) {
					hlIdxs.addElement(i);
					Appearance app = stobj.getAppearance();
					if (selIdxs != null) {
						int sidx = selIdxs.indexOf(i);
						if (sidx >= 0) {
							app = (Appearance) origAppsSelected.elementAt(sidx);
						}
					}
					origAppsHighlighted.addElement(app);
					stobj.setAppearance(hlAppearance);
				} else {
					;
				}
			else {
				int idx = hlIdxs.indexOf(i);
				if (idx >= 0) {
					if (selIdxs != null && selIdxs.indexOf(i) >= 0) {
						stobj.setAppearance(selAppearance);
					} else {
						stobj.setAppearance((Appearance) origAppsHighlighted.elementAt(idx));
					}
					origAppsHighlighted.removeElementAt(idx);
					hlIdxs.removeElementAt(idx);
				}
			}
		}
	}

	/**
	* Notification about change of the set of objects to be selected (durably
	* highlighted). The argument "source" is usually a reference to a
	* Highlighter. The argument setId is the identifier of the set the
	* highlighted objects belong to (e.g. map layer or table).
	* The argument selected is a vector of identifiers of
	* currently selected objects.
	*/
	@Override
	public void selectSetChanged(Object source, String setId, Vector selected) {
		applySelection();
	}

	protected boolean destroyed = false;

	@Override
	public boolean isDestroyed() {
		return destroyed;
	}

	@Override
	public void destroy() {
		if (destroyed)
			return;
		System.out.println("SelectionApplicator is destroyed");
		if (supervisor != null && oCont != null) {
			supervisor.removeHighlightListener(this, oCont.getEntitySetIdentifier());
		}
		destroyed = true;
	}
}
