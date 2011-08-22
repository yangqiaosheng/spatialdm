package spade.analysis.system;

import java.awt.Color;
import java.beans.PropertyChangeListener;
import java.util.Vector;

import spade.analysis.classification.ObjectColorer;
import spade.lib.util.Parameters;
import spade.vis.action.HighlightListener;
import spade.vis.action.Highlighter;
import spade.vis.action.ObjectEvent;
import spade.vis.action.ObjectEventHandler;
import spade.vis.database.DataItem;
import spade.vis.database.DataTreater;
import spade.vis.event.EventConsumer;
import spade.vis.event.EventMeaningManager;
import spade.vis.event.EventReceiver;
import spade.vis.event.EventSource;
import spade.vis.mapvis.AttrColorHandler;
import spade.vis.spec.SaveableTool;

/**
* A Supervisor controls linkage between multiple views. The ways of linking
* that can be supported are:
* 1) simultaneous marking (highlighting) of objects in all views;
* 2) propagation of multi-color painting of objects, for example, according to
*    some classification;
* 3) propagation of colors associated with attributes;
* 4) providing information about which attributes are currently visualised
*    (in all existing views) and notification about changes of the set of
*    visualised attributes.
*
* Different configurations of the system would probably require different
* supervisors (varying in the level of sophistication), but some functions need
* to be accessible by various components independent of the variant used.
* Therefore we define an interface that is to be implemented by whatever
* variant of a supervisor we may need.
*/

public interface Supervisor extends ObjectEventHandler {

	public static String eventObjectColors = "object_colors", eventAttrColors = "attr_colors", eventDisplayedAttrs = "displayed_attrs", eventTimeColors = "time_colors";

	/**
	* Returns the Highlighter dealing with objects of the specified set (such as
	* map layer). There is a separate highlighter for each set of objects.
	* A Highlighter is an object that propagates highlighting or selection events
	* among various components.
	*/
	public Highlighter getHighlighter(String objSetId);

	/**
	* If the Supervisor has a Highlighter, the HighlightListener is passed to
	* the Highlighter, otherwise the Supervisor remembers itself the listener
	* in order to be able to find then all displays and get the attributes
	* presented on them.
	*/
	public void registerHighlightListener(HighlightListener lst, String objSetId);

	public void removeHighlightListener(HighlightListener lst, String objSetId);

	/**
	* A method from the ObjectEventHandler interface.
	* Transfers the object event to the ObjectEventDispatcher (if exists) or
	* to the Highlighter (if exists) or ignores the event (in some very basic
	* configurations with only one display).
	* Unlike the Highlighter, the ObjectEventDispatcher may interpet object events
	* differently from highlighting, for example, as setting of a reference value
	* for visual comparison.
	*/
	@Override
	public void processObjectEvent(ObjectEvent oevt);

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
	public void registerObjectEventConsumer(EventConsumer oec, String eventType, String eventMeaning, String eventMeaningText);

	public void removeObjectEventConsumer(EventConsumer oec, String eventType, String eventMeaning);

	/**
	* Registers an object event listener (in practice, readresses it to the
	* ObjectEventDispatcher).
	*/
	public void registerObjectEventReceiver(EventReceiver oer);

	public void removeObjectEventReceiver(EventReceiver oer);

	/**
	* Registers a producer of object events. If the implementation of the
	* Supervisor has some EventBrokers, the producer is readressed to these
	* EventBrokers. Otherwise, the supervisor may do nothing.
	*/
	public void registerObjectEventSource(EventSource es);

	/**
	* Unregister the producer of object events (actually makes all EventBrokers,
	* if any, unregister it)
	*/
	public void removeObjectEventSource(EventSource es);

	/**
	* Registers a data display component that visualises attributes and implements
	* the DataTreater interface. Such registration helps the supervisor know
	* which attributes are currently displayed. The list of currently displayed
	* attributes can be got using the function getAllPresentedAttributes().
	*/
	public void registerDataDisplayer(DataTreater dt);

	/**
	* Unregisters the data displayer
	*/
	public void removeDataDisplayer(DataTreater dt);

	/**
	* When a table is removed, the Supervisor removes all data displayers that
	* are linked to this table. The table is specified by its identifier.
	*/
	public void tableIsRemoved(String tableId);

	/**
	* Returns a vector of identifiers of attributes that are shown on all
	* currently existing data displays, i.e. registered through the function
	* registerDataDisplayer(...).
	*/
	public Vector getAllPresentedAttributes();

	/**
	* Registers a tool that has been constructed and can "save" its state.
	* This is used for saving system's states ("snapshots"). Any registered
	* tool must notify the supervisor when it is destroyed.
	*/
	public void registerTool(SaveableTool tool);

	/**
	* Returns the current number of registered tools that can "save" their states.
	*/
	public int getSaveableToolCount();

	/**
	* Returns the registered saveable tool with the given index.
	*/
	public SaveableTool getSaveableTool(int idx);

	/**
	* Returns a list of registered tools that can "save" their states. The elements
	* of the resulting vector implement the interface spade.vis.spec.SaveableTool.
	* This is used for saving system's states ("snapshots").
	*/
	public Vector getSaveableTools();

	/**
	* A Supervisor may notify components about changes of coloring of objects
	* (for example, as a result of some classification), changes of colors
	* associated with attributes, and changes of the set of attributes currently
	* visualised. A component that would like to receive such notifications
	* should register itself as a listener of property changes.
	*/
	public void addPropertyChangeListener(PropertyChangeListener list);

	public void removePropertyChangeListener(PropertyChangeListener list);

	/**
	* Makes the Supervisor notify the registered PropertyChangeListeners about
	* a change of some global property: coloring of objects, assignment of colors
	* to attributes, the set of currently presented attributes. The argument
	* propName should be one of the strings Supervisor.eventObjectColors,
	* Supervisor.eventAttrColors, or Supervisor.eventDisplayedAttrs.
	*/
	public void notifyGlobalPropertyChange(String propName);

	/**
	* Makes the Supervisor notify the registered PropertyChangeListeners about
	* a change of some global property: coloring of objects, assignment of colors
	* to attributes, the set of currently presented attributes. The argument
	* propName should be one of the strings Supervisor.eventObjectColors,
	* Supervisor.eventAttrColors, or Supervisor.eventDisplayedAttrs. The argument
	* propValue will be sent to all listeners through a the property change event
	* as the "new value" of the property with the name propName.
	*/
	public void notifyGlobalPropertyChange(String propName, Object propValue);

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
	public void setObjectColorer(ObjectColorer oc);

	public ObjectColorer getObjectColorer();

	/**
	 * Answers if the objects handled by the current ObjectColorer are times
	 */
	public boolean coloredObjectsAreTimes();

	/**
	 * Returns the assignments of colors to time moments if available
	 * (i.e. if the current ObjectColorer handles time moments).
	 * The elements of the returned vectors are pairs [TimeMoment,Color],
	 * i.e. arrays consisting of 2 instances of Object.
	 */
	public Vector getColorsForTimes();

	/**
	* Equivalent to setObjectColorer(null).
	*/
	public void removeObjectColorer(ObjectColorer oc);

	/**
	* A Supervisor may propagate among components multi-color painting of
	* objects (for example, according to some classification). This method
	* is used by components in order to get the color for each object.
	* If no color is assigned to the object, the Supervisor returns the
	* default color passed to it as an argument. This is done in order to
	* avoid checks for null result in the places where this method is called.
	* The argument setId is the identifier of the set the object belongs to
	*/
	public Color getColorForObject(String id, String setId, Color defaultColor);

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
	public Color getColorForDataItem(DataItem dit, String setId, String containerId, Color defaultColor);

	/**
	* Through this method components can get information about colors associated
	* with attributes. The method returns null if no color has been assigned yet
	* to the specified attribute.
	*/
	public Color getColorForAttribute(String attrId);

	/**
	* Provides the global AttrColorHandler for those components that wish not only
	* to get attribute colors, but also to change these colors
	*/
	public AttrColorHandler getAttrColorHandler();

	public EventMeaningManager getObjectEventMeaningManager();

	/**
	* Returns the system UI that can, in particular, display status messages
	*/
	public SystemUI getUI();

	/**
	* Sets the system UI (the actual implementation of this interface depends
	* on the current system configuration)
	*/
	public void setUI(SystemUI ui);

	/**
	* Returns actual system settings (valid for the currently running instance
	* of the system)
	*/
	public Parameters getSystemSettings();

	/**
	* Returns the WindowManager of the system. A WindowManager registers all
	* windows that are created during a session and closes all windows on exit.
	*/
	public WindowManager getWindowManager();

	/**
	* Sets the WindowManager to be used by the system. A WindowManager registers all
	* windows that are created during a session and closes all windows on exit.
	*/
	public void setWindowManager(WindowManager wm);

	/**
	 * Sets a reference to an object used for logging significant analysis operations
	 */
	public void setActionLogger(ActionLogger logger);

	/**
	 * Returns a reference to an object used for logging significant analysis operations
	 */
	public ActionLogger getActionLogger();

	/**
	 * A Supervisor may also propagate notifications about the selection of a
	 * specific spatial position (absolute), which is marked in some displays.
	 * A component that would like to receive such notifications
	 * should register itself as a listener of position selection events.
	 */
	public void addPositionSelectListener(PropertyChangeListener list);

	public void removePositionSelectListener(PropertyChangeListener list);

	/**
	 * Notifies the listeners, if any, about the selection or deselection of the
	 * specific spatial position, which should be marked on some displays.
	 * If x and y are NaN, the previously selected position is deselected.
	 */
	public void notifyPositionSelection(float x, float y);
}
