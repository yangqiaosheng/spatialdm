package spade.vis.action;

import java.awt.event.MouseEvent;
import java.util.Vector;

import spade.vis.database.DataTreater;
import spade.vis.event.DEvent;

/**
* An ObjectEvent can be generated upon user's action in some display showing
* in any way some objects (not necessarily GeoObjects, but anything having
* its unique identifiers). For example, such events may be generated when
* the user moves the mouse over a map, dot plot, scatter plot etc., or clicks
* within the display area.
* Several objects may be simultaneously affected by a mouse event.
* Therefore an ObjectEvent stores a vector of affected objects.
*/

public class ObjectEvent extends DEvent {
	/**
	* Possible actions on objects.
	*/
	public static final String actions[] = { "ObjectPointed", "ObjectClicked", "ObjectDoubleClicked", "ObjectsFramed", "ObjectSelected" };
	public static final String actionFullNames[] = { "pointing object", "clicking object", "double-clicking object", "framing object(s)", "selecting object by pressing mouse" };
	public static final String point = actions[0], click = actions[1], dblClick = actions[2], frame = actions[3], select = actions[4];
	/**
	* The identifier(s) of the object(s) pointed to or clicked with the mouse.
	* When the mouse points/clicks to an object-free area, the vector is null
	* or empty.
	*/
	public Vector oIds = null;
	/**
	* The identifier of the set (e.g. map layer or table) the objects
	* belong to.
	*/
	public String setId = null;
	/**
	* If the event comes from a DataTreater, i.e. a component representing thematic
	* (attribute) data, this variable contains a reference to this component.
	*/
	public DataTreater dataT = null;
	/**
	 * If the objects have time references, this vector will contain the time references
	 * of the objects, instances of the class spade.time.TimeReference
	 */
	public Vector timeRefs = null;

	/**
	* Returns the name of the event type with the given identifier
	*/
	public static String getEventTypeName(String evtTypeId) {
		if (evtTypeId == null)
			return null;
		for (int i = 0; i < actions.length; i++)
			if (actions[i].equals(evtTypeId))
				return actionFullNames[i];
		return evtTypeId;
	}

	/**
	* Constructs an ObjectEvent. The argument "type" specifies the kind of the
	* event, i.e. one of the constants "ObjectPointed", "ObjectClicked",
	* "ObjectDoubleClicked" etc.
	* The identifiers of the objects affected by the event may be added later
	* using the function addEventAffectedObject.
	* When the action has occurred in an object-free area, the objects are not
	* added, and the vector of object identifiers remains null. Such an event may,
	* for example, be a signal to switch off highlighting of all currently
	* highlighted objects.
	* The argument sourceMouseEvent is the original (standard) mouse event
	* that gives rise to this object event.
	* setId is the identifier of the set (e.g. map layer or table) the objects
	* affected by the event belong to.
	*/
	public ObjectEvent(Object source, String type, MouseEvent sourceMouseEvent, String setId) {
		super(source, type, sourceMouseEvent);
		this.setId = setId;
		if (source instanceof DataTreater) {
			dataT = (DataTreater) source;
		}
	}

	/**
	* This constructor is recommended for use when only one object is affected
	* by the event. The argument "objId" specifies the (single) object
	* on  which the action occurred.
	* However, it is still possible to add more object identifiers
	* using the function addEventAffectedObject.
	* The argument sourceMouseEvent is the original (standard) mouse event
	* that gives rise to this object event.
	* setId is the identifier of the set (e.g. map layer or table) the objects
	* affected by the event belong to.
	*/
	public ObjectEvent(Object source, String type, MouseEvent sourceMouseEvent, String setId, String objId) {
		this(source, type, sourceMouseEvent, setId);
		if (objId != null) {
			oIds = new Vector(10, 10);
			oIds.addElement(objId);
		}
	}

	/**
	* The argument "objIds" specifies the list (vector) of objects affected
	* by the event. This list, in particular, may be null or empty.
	* The argument sourceMouseEvent is the original (standard) mouse event
	* that gives rise to this object event.
	* setId is the identifier of the set (e.g. map layer or table) the objects
	* affected by the event belong to.
	*/
	public ObjectEvent(Object source, String type, MouseEvent sourceMouseEvent, String setId, Vector objIds) {
		this(source, type, sourceMouseEvent, setId);
		oIds = objIds;
	}

	/**
	* Returns the x-position of the mouse in absolute (screen) coordinates
	*/
	public int getX() {
		if (sourceME == null || sourceME.getComponent() == null || !sourceME.getComponent().isShowing())
			return -1;
		return sourceME.getX() + sourceME.getComponent().getLocationOnScreen().x;
	}

	/**
	* Returns the y-position of the mouse in absolute (screen) coordinates
	*/
	public int getY() {
		if (sourceME == null || sourceME.getComponent() == null || !sourceME.getComponent().isShowing())
			return -1;
		return sourceME.getY() + sourceME.getComponent().getLocationOnScreen().y;
	}

	/**
	* Returns the identifier of the set (e.g. map layer or table) the objects
	* affected by the event belong to.
	*/
	public String getSetIdentifier() {
		return setId;
	}

	/**
	* Adds the object identifier to the list of identifiers of event-affected
	* objects.
	*/
	public void addEventAffectedObject(String objId) {
		if (objId == null)
			return;
		if (oIds == null) {
			oIds = new Vector(10, 10);
		}
		for (int i = 0; i < oIds.size(); i++)
			if (objId.equalsIgnoreCase((String) oIds.elementAt(i)))
				return;
		oIds.addElement(objId);
	}

	/**
	* Returns the type (identifier) of the event, i.e. one of the constants
	* "ObjectPointed", "ObjectClicked", "ObjectDoubleClicked" etc.
	* The same as the getId() function of the superclass (actually calls
	* that function).
	*/
	public String getType() {
		return getId();
	}

	/**
	* Returns the vector of the objects affected by the action.
	*/
	public Vector getAffectedObjects() {
		return oIds;
	}

	/**
	/**
	* Returns the number of the objects affected by the action.
	*/
	public int getAffectedObjectCount() {
		if (oIds == null)
			return 0;
		return oIds.size();
	}

	/**
	* Returns the identifier of the object having the specified index in the list
	* of objects affected by the action.
	*/
	public String getObjectIdentifier(int idx) {
		if (idx < 0 || idx >= getAffectedObjectCount())
			return null;
		return (String) oIds.elementAt(idx);
	}

	/**
	* If available, returns a reference to a DataTreater this event comes from,
	* i.e. a component representing thematic (attribute) data.
	*/
	public DataTreater getDataTreater() {
		if (dataT == null && (source instanceof DataTreater)) {
			dataT = (DataTreater) source;
		}
		return dataT;
	}

	/**
	 * If the objects have time references, this vector will contain the time references
	 * of the objects, instances of the class spade.time.TimeReference
	 */
	public Vector getTimeRefs() {
		return timeRefs;
	}

	/**
	 * If the objects have time references, sets the vector containing the time references
	 * of the objects, instances of the class spade.time.TimeReference
	 */
	public void setTimeRefs(Vector timeRefs) {
		this.timeRefs = timeRefs;
	}
}
