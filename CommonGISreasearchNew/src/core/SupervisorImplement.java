package core;

import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Vector;

import spade.analysis.classification.ObjectColorer;
import spade.analysis.system.ActionLogger;
import spade.analysis.system.Supervisor;
import spade.analysis.system.SystemUI;
import spade.analysis.system.WindowManager;
import spade.lib.util.Parameters;
import spade.lib.util.StringUtil;
import spade.time.TimeMoment;
import spade.vis.action.HighlightListener;
import spade.vis.action.Highlighter;
import spade.vis.action.ObjectEvent;
import spade.vis.action.ObjectEventDispatcher;
import spade.vis.action.SuperHighlighter;
import spade.vis.database.DataItem;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.database.DataTreater;
import spade.vis.database.ObjectContainer;
import spade.vis.event.EventConsumer;
import spade.vis.event.EventMeaningManager;
import spade.vis.event.EventReceiver;
import spade.vis.event.EventSource;
import spade.vis.mapvis.AttrColorHandler;
import spade.vis.mapvis.AttrColorSupport;
import spade.vis.spec.SaveableTool;

public class SupervisorImplement implements Supervisor, PropertyChangeListener {
	public String applName = "Descartes XXI"; //name of the application

	protected SuperHighlighter shigh = null; //common for all displays
	protected ObjectEventDispatcher odisp = null;
	protected AttrColorSupport attrColorer = null;

	/**
	* A vector of registered data display components that visualize attributes.
	* The components implement the DataTreater interface
	*/
	protected Vector displayers = null;
	/**
	* A register of tools that can "save" its state. The elements of the
	* vector implement the interface spade.vis.spec.SaveableTool.
	* This is used for saving system's states ("snapshots"). Any registered
	* tool must notify the supervisor when it is destroyed.
	*/
	protected Vector saveableTools = null;
	/**
	* A Supervisor may propagate among components not only highlighting, but also
	* multi-color painting of objects, for example, according to some
	* classification;
	* colors associated with attributes;
	* information about which attributes are currently visualised
	* (in all existing views) and changes of the set of visualised attributes.
	* A component that would like to receive such notifications
	* should register itself as a listener of property changes.
	* To handle the list of listeners and notify them about changes,
	 * the Supervisor uses a PropertyChangeSupport.
	*/
	protected PropertyChangeSupport pcSupport = null;
	/**
	 * A Supervisor may also propagate notifications about the selection of a
	 * specific spatial position (absolute), which is marked in some displays.
	 * A component that would like to receive such notifications
	 * should register itself as a listener of the changes of the position.
	 * To handle the list of listeners and notify them about changes, the Supervisor
	 * uses a PropertyChangeSupport.
	 */
	protected PropertyChangeSupport posSelectSupport = null;
	/**
	* A supplier of colors for objects. The supervisor registers itself as a
	* listener of property (colors) changes. When a property change event
	* occurs, the Supervisor notifies its listeners.
	*/
	protected ObjectColorer objectColorer = null; //common for all displays
	/**
	* The system UI that can, in particular, display status messages
	*/
	protected SystemUI ui = null;
	/**
	* Current settings of the running version of the system
	*/
	protected Parameters systemSettings = null;
	/**
	* A WindowManager registers all windows that are created during a session and
	* closes all windows on exit.
	*/
	protected WindowManager winMan = null;

	public SupervisorImplement() {
		systemSettings = new Parameters();
		shigh = new SuperHighlighter();
		odisp = new ObjectEventDispatcher();
		odisp.setSuperHighlighter(shigh);
		attrColorer = new AttrColorSupport();
		attrColorer.addPropertyChangeListener(this);
	}

	@Override
	public Highlighter getHighlighter(String setId) {
		return shigh.getHighlighter(setId);
	}

	public SuperHighlighter getSuperHighlighter() {
		return shigh;
	}

	@Override
	public EventMeaningManager getObjectEventMeaningManager() {
		return odisp.getEventMeaningManager();
	}

	/**
	* Passes the HighlightListener to the appropriate Highlighter
	*/
	@Override
	public void registerHighlightListener(HighlightListener lst, String setId) {
		shigh.getHighlighter(setId).addHighlightListener(lst);
	}

	@Override
	public void removeHighlightListener(HighlightListener lst, String setId) {
		shigh.getHighlighter(setId).removeHighlightListener(lst);
	}

	/**
	* Transfers the object event to the ObjectEventDispatcher (if exists) or
	* to the Highlighter (if exists) or ignores the event (in some very basic
	* configurations with only one display)
	*/
	@Override
	public void processObjectEvent(ObjectEvent oevt) {
		if (odisp != null) {
			odisp.processObjectEvent(oevt);
		} else if (shigh != null) {
			shigh.processObjectEvent(oevt);
		}
	}

	/**
	* Registers an object event consumer (in practice, readresses it to the
	* ObjectEventDispatcher). The argument "eventType" should be one
	* of the constants defined in the ObjectEvent interface to denote possible
	* actions (point, click, double click). The argument "eventMeaning"
	* must be a string denoting the way of event interpretation,
	* e.g. "visual comparison". This meaning becomes, by default, the current
	* meaning of this kind of events.
	* The argument "eventMeaningText" is a text to explain the meaning to a user.
	*/
	@Override
	public void registerObjectEventConsumer(EventConsumer oec, String eventType, String eventMeaning, String eventMeaningText) {
		if (odisp != null) {
			odisp.addObjectEventConsumer(oec, eventType, eventMeaning, eventMeaningText);
		}
	}

	@Override
	public void removeObjectEventConsumer(EventConsumer oec, String eventType, String eventMeaning) {
		if (odisp != null) {
			odisp.removeObjectEventConsumer(oec, eventType, eventMeaning);
		}
	}

	/**
	* Registers an object event listener (in practice, readresses it to the
	* ObjectEventDispatcher).
	*/
	@Override
	public void registerObjectEventReceiver(EventReceiver oer) {
		if (odisp != null) {
			odisp.addObjectEventReceiver(oer);
		}
	}

	@Override
	public void removeObjectEventReceiver(EventReceiver oer) {
		if (odisp != null) {
			odisp.removeObjectEventReceiver(oer);
		}
	}

	/**
	* Registers a producer of object events. If the implementation of the
	* Supervisor has some EventBrokers, the producer is readressed to these
	* EventBrokers. This supervisor does nothing.
	*/
	@Override
	public void registerObjectEventSource(EventSource es) {
	}

	/**
	* Unregister the producer of object events (actually makes all EventBrokers,
	* if any, unregister it) This supervisor does nothing in this method.
	*/
	@Override
	public void removeObjectEventSource(EventSource es) {
	}

	/**
	* A Supervisor may notify components about changes of coloring of objects
	* (for example, as a result of some classification), changes of colors
	* associated with attributes, and changes of the set of attributes currently
	* visualised. A component that would like to receive such notifications
	* should register itself as a listener of property changes.
	*/
	@Override
	public void addPropertyChangeListener(PropertyChangeListener list) {
		if (list == null)
			return;
		if (pcSupport == null) {
			pcSupport = new PropertyChangeSupport(this);
		}
		pcSupport.addPropertyChangeListener(list);
	}

	@Override
	public void removePropertyChangeListener(PropertyChangeListener list) {
		if (list == null || pcSupport == null)
			return;
		pcSupport.removePropertyChangeListener(list);
	}

	/**
	* Makes the Supervisor notify the registered PropertyChangeListeners about
	* a change of some global property: coloring of objects, assignment of colors
	* to attributes, the set of currently presented attributes. The argument
	* propName should be one of the strings Supervisor.eventObjectColors,
	* Supervisor.eventAttrColors, or Supervisor.eventDisplayedAttrs.
	*/
	@Override
	public void notifyGlobalPropertyChange(String propName) {
		notifyGlobalPropertyChange(propName, null);
	}

	/**
	* Makes the Supervisor notify the registered PropertyChangeListeners about
	* a change of some global property: coloring of objects, assignment of colors
	* to attributes, the set of currently presented attributes. The argument
	* propName should be one of the strings Supervisor.eventObjectColors,
	* Supervisor.eventAttrColors, or Supervisor.eventDisplayedAttrs. The argument
	* propValue will be sent to all listeners through a the property change event
	* as the "new value" of the property with the name propName.
	*/
	@Override
	public void notifyGlobalPropertyChange(String propName, Object propValue) {
		if (pcSupport != null) {
			pcSupport.firePropertyChange(propName, null, propValue);
		}
	}

	/**
	* A Supervisor may be associated with some component that assigns color to
	* objects, for example, a classifier. All such components should implement
	* the ObjectColorer interface. The supervisor should register itself as a
	* listener of property change events of the ObjectColorer. As a result, the
	* Supervisor will be able to propagate among components coloring of objects.
	* When the supervisors receives a new object colorer, it sends a property
	* change event with the name Supervisor.eventObjectColors and identifier of
	* the entity set the object colorer deals with as the "new value" of the
	* property. If the supervisor had another object colorer before receiving
	* the new one, and the old colorer referred to another object set, the
	* supervisor sends two events: one with the old object set identifier and
	* one with the new one. This allows the displays working with the previous
	* object set to remove object coloring.
	*/
	@Override
	public void setObjectColorer(ObjectColorer oc) {
		if (oc == null)
			if (objectColorer == null)
				return;
			else {
				;
			}
		else if (oc.equals(objectColorer))
			return;
		String oldSetId = null, newSetId = null;
		if (objectColorer != null) {
			if (objectsAreTimes(objectColorer.getObjectContainer())) {
				notifyGlobalPropertyChange(Supervisor.eventTimeColors, null);
			}
			oldSetId = objectColorer.getEntitySetIdentifier();
			objectColorer.removePropertyChangeListener(this);
		}
		objectColorer = oc;
		if (objectColorer != null) {
			objectColorer.addPropertyChangeListener(this);
			newSetId = objectColorer.getEntitySetIdentifier();
		}
		if (oldSetId != null && !oldSetId.equals(newSetId)) {
			notifyGlobalPropertyChange(Supervisor.eventObjectColors, oldSetId);
		}
		if (newSetId != null) {
			notifyGlobalPropertyChange(Supervisor.eventObjectColors, newSetId);
		}
		if (objectColorer != null && objectsAreTimes(objectColorer.getObjectContainer())) {
			notifyGlobalPropertyChange(Supervisor.eventTimeColors, objectColorer);
		}
	}

	@Override
	public ObjectColorer getObjectColorer() {
		return objectColorer;
	}

	/**
	* Equivalent to setObjectColorer(null).
	*/
	@Override
	public void removeObjectColorer(ObjectColorer oc) {
		setObjectColorer(null);
	}

	/**
	* A Supervisor may propagate among components multi-color painting of
	* objects (for example, according to some classification). This method
	* is used by components in order to get the color for each object.
	* If no color is assigned to the object, the Supervisor returns the
	* default color passed to it as an argument. This is done in order to
	* avoid checks for null result in the places where this method is called.
	* The argument setId is the identifier of the set the object belongs to
	*/
	@Override
	public Color getColorForObject(String id, String setId, Color defaultColor) {
		Color c = null;
		if (objectColorer != null && StringUtil.sameStrings(setId, objectColorer.getEntitySetIdentifier())) {
			c = objectColorer.getColorForObject(id);
		}
		if (c != null)
			return c;
		return defaultColor;
	}

	/**
	* A Supervisor may propagate among components multi-color painting of
	* objects (for example, according to some classification). In particular
	* cases coloring is done according to thematic or other data.
	* This method is used by components in order to get the color for a
	* data record corresponding to some object.
	* The argument setId is the identifier of the object set (that can be common
	* for several tables, layers etc.). The argument containerId is the unique
	* identifier of the container from which the data item is taken.
	* If no color is assigned to the object, the Supervisor returns the
	* default color passed to it as an argument.
	*/
	@Override
	public Color getColorForDataItem(DataItem dit, String setId, String containerId, Color defaultColor) {
		Color c = null;
		if (dit != null && objectColorer != null && StringUtil.sameStrings(setId, objectColorer.getEntitySetIdentifier())) {
			c = objectColorer.getColorForDataItem(dit, containerId);
		}
		if (c != null)
			return c;
		return defaultColor;
	}

	/**
	* Through this method components can get information about colors associated
	* with attributes. The method returns null if no color has been assigned yet
	* to the specified attribute.
	*/
	@Override
	public Color getColorForAttribute(String attrId) {
		return attrColorer.getColorForAttribute(attrId);
	}

	/**
	* Provides the global AttrColorHandler for those components that wish not only
	* to get attribute colors, but also to change these colors
	*/
	@Override
	public AttrColorHandler getAttrColorHandler() {
		return attrColorer;
	}

	/**
	* The Supervisor may be registered as a listener of property changes
	* (e.g. changes of object colors by some ObjectColorer). When a property
	* change event occurs, the Supervisor notifies its listeners.
	* In particular, when the object colorer has changed object colors, the
	* supervisor distributes a property change event with the property name
	* Supervisor.eventObjectColors. The "new value" of the property is the
	* identifier of the entity set the object colorer deals with.
	*/
	@Override
	public void propertyChange(PropertyChangeEvent pce) {
		if (saveableTools != null && (pce.getSource() instanceof SaveableTool) && pce.getPropertyName().equals("destroyed")) {
			int idx = saveableTools.indexOf(pce.getSource());
			if (idx >= 0) {
				saveableTools.removeElementAt(idx);
			}
		}
		if (pcSupport == null)
			return;
		if (pce.getSource().equals(objectColorer)) {
			if (objectsAreTimes(objectColorer.getObjectContainer()))
				if (pce.getPropertyName().equals("destroyed")) {
					notifyGlobalPropertyChange(Supervisor.eventTimeColors, null);
				} else {
					notifyGlobalPropertyChange(Supervisor.eventTimeColors, objectColorer);
				}
			String setId = objectColorer.getEntitySetIdentifier();
			if (pce.getPropertyName().equals("destroyed")) {
				objectColorer.removePropertyChangeListener(this);
				objectColorer = null;
			}
			notifyGlobalPropertyChange(Supervisor.eventObjectColors, setId);
		} else if (pce.getSource() == attrColorer) {
			notifyGlobalPropertyChange(Supervisor.eventAttrColors, pce.getNewValue());
		}
	}

	/**
	 * Checks if the objects in the given container are time moments
	 */
	protected boolean objectsAreTimes(ObjectContainer oCont) {
		if (oCont == null)
			return false;
		if (!(oCont instanceof DataTable))
			return false;
		DataTable table = (DataTable) oCont;
		return table.getNatureOfItems() == DataTable.NATURE_TIME && table.getDataRecord(0).getDescribedObject() != null && (table.getDataRecord(0).getDescribedObject() instanceof TimeMoment);
	}

	/**
	 * Answers if the objects handled by the current ObjectColorer are times
	 */
	@Override
	public boolean coloredObjectsAreTimes() {
		return objectColorer != null && objectsAreTimes(objectColorer.getObjectContainer());
	}

	/**
	 * Returns the assignments of colors to time moments if available
	 * (i.e. if the current ObjectColorer handles time moments).
	 * The elements of the returned vectors are pairs [TimeMoment,Color],
	 * i.e. arrays consisting of 2 instances of Object.
	 */
	@Override
	public Vector getColorsForTimes() {
		if (objectColorer == null)
			return null;
		ObjectContainer oCont = objectColorer.getObjectContainer();
		if (oCont == null)
			return null;
		if (!(oCont instanceof DataTable))
			return null;
		DataTable table = (DataTable) oCont;
		if (table.getNatureOfItems() != DataTable.NATURE_TIME)
			return null;
		Vector result = new Vector(table.getDataItemCount(), 1);
		for (int i = 0; i < table.getDataItemCount(); i++) {
			DataRecord rec = table.getDataRecord(i);
			Object obj = rec.getDescribedObject();
			if (obj == null) {
				continue;
			}
			if (!(obj instanceof TimeMoment)) {
				continue;
			}
			TimeMoment t = (TimeMoment) obj;
			Object pair[] = { t.getCopy(), objectColorer.getColorForObject(i) };
			result.addElement(pair);
		}
		if (result.size() < 1)
			return null;
		return result;
	}

	/**
	* Registers a data display component that visualises attributes and implements
	* the DataTreater interface. Such registration helps the supervisor know
	* which attributes are currently displayed. The list of currently displayed
	* attributes can be got using the function getAllPresentedAttributes().
	*/
	@Override
	public void registerDataDisplayer(DataTreater dt) {
		if (dt == null)
			return;
		if (displayers == null) {
			displayers = new Vector(10, 10);
		}
		if (displayers.contains(dt))
			return;
		displayers.addElement(dt);
		notifyGlobalPropertyChange(Supervisor.eventDisplayedAttrs);
	}

	/**
	* Unregisters the data displayer
	*/
	@Override
	public void removeDataDisplayer(DataTreater dt) {
		if (dt == null || displayers == null)
			return;
		int idx = displayers.indexOf(dt);
		if (idx < 0)
			return;
		displayers.removeElementAt(idx);
		notifyGlobalPropertyChange(Supervisor.eventDisplayedAttrs);
	}

	/**
	* When a table is removed, the Supervisor removes all data displayers that
	* are linked to this table. The table is specified by its identifier.
	*/
	@Override
	public void tableIsRemoved(String tableId) {
		if (tableId == null)
			return;
		if (objectColorer != null && (objectColorer instanceof DataTreater) && ((DataTreater) objectColorer).isLinkedToDataSet(tableId)) {
			if (objectsAreTimes(objectColorer.getObjectContainer())) {
				notifyGlobalPropertyChange(Supervisor.eventTimeColors, null);
			}
			String setId = objectColorer.getEntitySetIdentifier();
			objectColorer.removePropertyChangeListener(this);
			objectColorer = null;
			notifyGlobalPropertyChange(Supervisor.eventObjectColors, setId);
		}
		boolean changed = false;
		if (displayers != null) {
			for (int i = displayers.size() - 1; i >= 0; i--) {
				DataTreater dt = (DataTreater) displayers.elementAt(i);
				if (dt.isLinkedToDataSet(tableId)) {
					displayers.removeElementAt(i);
					changed = true;
				}
			}
		}
		if (changed) {
			notifyGlobalPropertyChange(Supervisor.eventDisplayedAttrs);
		}
	}

	/**
	* Returns a vector of identifiers of attributes that are shown on all
	* currently existing data displays, i.e. registered through the function
	* registerDataDisplayer(...).
	*/
	@Override
	public Vector getAllPresentedAttributes() {
		if (displayers == null || displayers.size() < 1)
			return null;
		Vector attrs = new Vector(10, 10);
		for (int i = 0; i < displayers.size(); i++) {
			DataTreater dt = (DataTreater) displayers.elementAt(i);
			Vector dta = dt.getAttributeList();
			if (dta != null) {
				for (int j = 0; j < dta.size(); j++)
					if (!attrs.contains(dta.elementAt(j))) {
						attrs.addElement(dta.elementAt(j));
					}
			}
		}
		if (attrs == null || attrs.size() < 1)
			return null;
		return attrs;
	}

	/**
	* Returns the system UI that can, in particular, display status messages
	*/
	@Override
	public SystemUI getUI() {
		return ui;
	}

	/**
	* Sets the system UI (the actual implementation of this interface depends
	* on the current system configuration)
	*/
	@Override
	public void setUI(SystemUI ui) {
		this.ui = ui;
	}

	/**
	* Returns actual system settings (valid for the currently running instance
	* of the system)
	*/
	@Override
	public Parameters getSystemSettings() {
		return systemSettings;
	}

	/**
	* Set actual system settings (needed to use correct locale)
	*/
	public void setSystemSettings(Parameters sysParam) {
		systemSettings = sysParam;
	}

	/**
	* Returns the WindowManager of the system. A WindowManager registers all
	* windows that are created during a session and closes all windows on exit.
	*/
	@Override
	public WindowManager getWindowManager() {
		return winMan;
	}

	/**
	* Sets the WindowManager to be used by the system. A WindowManager registers all
	* windows that are created during a session and closes all windows on exit.
	*/
	@Override
	public void setWindowManager(WindowManager wm) {
		winMan = wm;
	}

	/**
	* Registers a tool that has been constructed and can "save" its state.
	* This is used for saving system's states ("snapshots"). Any registered
	* tool must notify the supervisor when it is destroyed.
	*/
	@Override
	public void registerTool(SaveableTool tool) {
		if (tool == null)
			return;
		if (saveableTools == null) {
			saveableTools = new Vector(50, 20);
		}
		if (saveableTools.contains(tool))
			return;
		saveableTools.addElement(tool);
		tool.addDestroyingListener(this);
	}

	/**
	* Returns a list of registered tools that can "save" their states. The elements
	* of the resulting vector implement the interface spade.vis.spec.SaveableTool.
	* This is used for saving system's states ("snapshots").
	*/
	@Override
	public Vector getSaveableTools() {
		return saveableTools;
	}

	/**
	* Returns the current number of registered tools that can "save" their states.
	*/
	@Override
	public int getSaveableToolCount() {
		if (saveableTools == null)
			return 0;
		return saveableTools.size();
	}

	/**
	* Returns the registered saveable tool with the given index.
	*/
	@Override
	public SaveableTool getSaveableTool(int idx) {
		if (idx < 0 || idx >= getSaveableToolCount())
			return null;
		return (SaveableTool) saveableTools.elementAt(idx);
	}

	/**
	 * An object used for logging significant analysis operations
	 */
	protected ActionLogger actionLogger = null;

	/**
	 * Sets a reference to an object used for logging significant analysis operations
	 */
	@Override
	public void setActionLogger(ActionLogger logger) {
		actionLogger = logger;
	}

	/**
	 * Returns a reference to an object used for logging significant analysis operations
	 */
	@Override
	public ActionLogger getActionLogger() {
		return actionLogger;
	}

	/**
	 * A Supervisor may also propagate notifications about the selection of a
	 * specific spatial position (absolute), which is marked in some displays.
	 * A component that would like to receive such notifications
	 * should register itself as a listener of position selection events.
	 */
	@Override
	public void addPositionSelectListener(PropertyChangeListener list) {
		if (list == null)
			return;
		if (posSelectSupport == null) {
			posSelectSupport = new PropertyChangeSupport(this);
		}
		posSelectSupport.addPropertyChangeListener(list);
	}

	@Override
	public void removePositionSelectListener(PropertyChangeListener list) {
		if (list == null || posSelectSupport == null)
			return;
		posSelectSupport.removePropertyChangeListener(list);
	}

	/**
	 * Notifies the listeners, if any, about the selection or deselection of the
	 * specific spatial position, which should be marked on some displays.
	 * If x and y are NaN, the previously selected position is deselected.
	 */
	@Override
	public void notifyPositionSelection(float x, float y) {
		if (posSelectSupport == null || !posSelectSupport.hasListeners("position_selection"))
			return;
		if (Float.isNaN(x)) {
			posSelectSupport.firePropertyChange("position_selection", null, null);
		} else {
			float coord[] = { x, y };
			posSelectSupport.firePropertyChange("position_selection", null, coord);
		}
	}
}
