package spade.analysis.vis3d;

import java.util.Vector;

import spade.analysis.system.Supervisor;
import spade.vis.space.ObjectManager;

/**
* Object Manager 3D extends standard ObjectManager
* and is specially adapted for Sample3DView
*/

public class ObjectManager3D extends ObjectManager {
	String attr3DID = null;

	public ObjectManager3D(Supervisor sup) {
		super(sup);
	}

	/*
	* Returns identifier of the thematic attribute shown in 3DView
	*/
	public String getIDAttribute3D() {
		return attr3DID;
	}

	/*
	* Sets identifier of the thematic attribute shown in 3DView
	*/
	public void setIDAttribute3D(String a3dID) {
		attr3DID = a3dID;
	}

	/**
	* A method from the DataTreater interface.
	* Returns a vector of IDs of the attributes that are currently visualized
	* in the map and on 3DView
	*/
	@Override
	public Vector getAttributeList() {
		Vector vAttrs = super.getAttributeList();
		if (attr3DID != null) {
			if (vAttrs == null) {
				vAttrs = new Vector(1, 1);
			}
			if (!vAttrs.contains(attr3DID)) {
				vAttrs.addElement(attr3DID);
			}
		}
		return vAttrs;
	}

	/**
	* Requests the highlighter to clear selection (durable highlighting) of objects
	*/
	@Override
	public void clearSelection() {
		super.clearSelection();
		map.restorePicture();
	}

	/**
	* A method from the HighlightListener interface.
	* Reaction to a change of the set of objects to be selected (durably
	* highlighted). The argument "source" is typically a reference to the
	* highlighter.
	*/

	@Override
	public void selectSetChanged(Object source, String setId, Vector selObj) {
		super.selectSetChanged(source, setId, selObj);
		map.restorePicture();
	}
}
