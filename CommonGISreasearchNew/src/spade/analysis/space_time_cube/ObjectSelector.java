package spade.analysis.space_time_cube;

import java.awt.AWTEvent;
import java.awt.event.MouseEvent;
import java.util.Enumeration;
import java.util.Vector;

import javax.media.j3d.Behavior;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.Node;
import javax.media.j3d.PickInfo;
import javax.media.j3d.WakeupOnAWTEvent;
import javax.vecmath.Vector3d;

import spade.analysis.system.Supervisor;
import spade.lib.util.StringUtil;
import spade.vis.action.ObjectEvent;
import spade.vis.database.DataTreater;

import com.sun.j3d.utils.pickfast.PickCanvas;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Jun 9, 2008
 * Time: 2:14:25 PM
 * Translates mouse clicks in the cube into object selections
 */
public class ObjectSelector extends Behavior implements DataTreater {
	/**
	 * direction for picking -- into the scene
	 */
	protected final static Vector3d IN_VEC = new Vector3d(0.f, 0.f, -1.f);
	/**
	 * The event activating the behaviour
	 */
	protected WakeupOnAWTEvent clickEvent = new WakeupOnAWTEvent(MouseEvent.MOUSE_CLICKED);
	/**
	 * Supports picking objects at mouse position
	 */
	protected PickCanvas pickCanvas = null;
	/**
	 * Used for selection and deselection of objects
	 */
	protected Supervisor supervisor = null;
	/**
	 * All objects (instances of SpaceTimeObject) among which the selection is done
	 */
	protected Vector<SpaceTimeObject> stObjects = null;

	/**
	 * Constructs the object selector
	 * @param canvas3D - the canvas where the 3D view is drawn
	 * @param pickableGroup - includes the pickable objects
	 * @param supervisor - used for selection and deselection of objects
	 */
	public ObjectSelector(Canvas3D canvas3D, BranchGroup pickableGroup, Supervisor supervisor) {
		this.supervisor = supervisor;
		pickCanvas = new PickCanvas(canvas3D, pickableGroup);
		pickCanvas.setMode(PickInfo.PICK_GEOMETRY);
		pickCanvas.setFlags(PickInfo.NODE | PickInfo.CLOSEST_INTERSECTION_POINT);
		pickCanvas.setTolerance(3f);
	}

	/**
	 * Adds the given group of objects to the list of objects among which the selection is done
	 */
	public void setObjectsToSelect(Vector<SpaceTimeObject> objGroup) {
		stObjects = objGroup;
	}

	/**
	 * Initialize the behavior: set the initial wakeup condition.
	 * This method is called when the behavior becomes live.
	 */
	@Override
	public void initialize() {
		// set initial wakeup condition
		if (supervisor != null) {
			this.wakeupOn(clickEvent);
		}
	}

	/**
	 * The identifiers of the entity sets in which any object
	 * has been selected
	 */
	protected Vector setIds = null;

	/**
	 * Called by Java3D when appropriate stimulus occurs
	 */
	@Override
	public void processStimulus(Enumeration criteria) {
		if (supervisor == null || stObjects == null || stObjects.size() < 1)
			return;
		if (criteria == null || !criteria.hasMoreElements())
			return;
		//decode the stimulus...
		while (criteria.hasMoreElements()) {
			Object elem = criteria.nextElement();
			if (elem instanceof WakeupOnAWTEvent) {
				WakeupOnAWTEvent ae = (WakeupOnAWTEvent) elem;
				AWTEvent events[] = ae.getAWTEvent();
				if (events == null || events.length < 1) {
					continue;
				}
				for (AWTEvent event : events)
					if (event instanceof MouseEvent) {
						MouseEvent me = (MouseEvent) event;
						if (me.getID() == MouseEvent.MOUSE_CLICKED) {
							if (me.getClickCount() > 1) {
								if (setIds != null && setIds.size() > 0) {
									for (int j = 0; j < setIds.size(); j++) {
										supervisor.getHighlighter((String) setIds.elementAt(j)).clearSelection(this);
									}
									setIds.removeAllElements();
								}
							} else {
								processMouseEvent(me, me.getButton() == MouseEvent.BUTTON1);
							}
							break;
						}
					}
			}
		}
		wakeupOn(clickEvent);
	}

	/**
	 * Called upon a mouse click in the cube.
	 * Sends a pick ray into the world starting from the mouse position.
	 * Gets the closest intersecting node and takes the first SpaceTimeObject as
	 * the object to select or deselect.
	 */
	protected void processMouseEvent(MouseEvent mouseEvent, boolean clicked) {
		if (supervisor == null || stObjects == null || stObjects.size() < 1)
			return;
		SpaceTimeObject stObj = null;
		pickCanvas.setShapeLocation(mouseEvent);
		PickInfo pci = pickCanvas.pickClosest();
		if (pci != null) {
			Node node = pci.getNode();
			if (node != null) {
				for (int i = 0; i < stObjects.size() && stObj == null; i++)
					if (stObjects.elementAt(i).hasNode(node)) {
						stObj = stObjects.elementAt(i);
					}
			}
		}
		if (stObj == null) {
			if (setIds != null && setIds.size() > 0) {
				for (int i = 0; i < setIds.size(); i++) {
					supervisor.processObjectEvent(new ObjectEvent(this, (clicked) ? "ObjectClicked" : "ObjectPointed", mouseEvent, (String) setIds.elementAt(i)));
				}
			}
		} else {
			String sid = stObj.getEntitySetId();
			if (setIds == null || !StringUtil.isStringInVectorIgnoreCase(sid, setIds)) {
				if (setIds == null) {
					setIds = new Vector(5, 5);
				}
				setIds.addElement(sid);
			}
			supervisor.processObjectEvent(new ObjectEvent(this, (clicked) ? "ObjectClicked" : "ObjectPointed", mouseEvent, sid, stObj.getGeoObjectId()));
		}
	}

	/**
	 * A DataTreater returns a vector of IDs of the attributes this Data Treater deals with.
	 * An ObjectSelector returns null.
	 */
	@Override
	public Vector getAttributeList() {
		return null;
	}

	/**
	* Replies whether it is linked to the data set (table) with the given identifier
	*/
	@Override
	public boolean isLinkedToDataSet(String setId) {
		return true;
	}

	/**
	* A DataTreater returns a vector of colors used for representation of the attributes this
	* Data Treater deals with. An ObjectSelector returns null.
	*/
	@Override
	public Vector getAttributeColors() {
		return null;
	}
}
