package spade.analysis.system;

import java.util.Vector;

import spade.lib.page_util.PageCollection;
import spade.lib.util.Parameters;
import spade.vis.action.Highlighter;
import ui.SnapshotManager;
import core.ActionDescr;

public interface ESDACore {
	/**
	* The supervisor is used for dynamic linking of data displays simultaneously
	* present on the screen.
	*/
	public Supervisor getSupervisor();

	/**
	* The display producer is used for visualisation of data on maps and other
	* displays.
	*/
	public DisplayProducer getDisplayProducer();

	/**
	* The WindowManager is used for registering all windows (frames) opened
	* during the session. It cares about closing all windows at the end of the
	* session.
	*/
	public WindowManager getWindowManager();

	/**
	* Registers an additional component for analysis, e.g. the calculation manager,
	* the manager of geocomputational tools, etc.
	* A reference to this component can be got using the method getTool. Each tool
	* (component) must have its unique identifier.
	*/
	public void addTool(String toolId, Object tool);

	/**
	* Returns a reference to the tool with the given identifier, if available.
	* Here are some valid identifiers:
	* "CalcManager" - for the component performing computations in tables
	* "GeoCalcManager" - for the component performing geocomputations, i.e.
	*                    computations that involve or produce map layers
	* "ExtraToolManager" - for the component managing additional analysis tools
	*                      extending the core functionality of the system
	*/
	public Object getTool(String toolId);

	/**
	* Tries to run the tool with the given identifier, if available and can be
	* started. Returns true if the tool was found and started.
	* Here are some valid tool identifiers:
	* "CalcManager" - for the component performing computations in tables
	* "GeoCalcManager" - for the component performing geocomputations, i.e.
	*                    computations that involve or produce map layers
	* "ExtraToolManager" - for the component managing additional analysis tools
	*                      extending the core functionality of the system
	*/
	public boolean runTool(String toolId);

	/**
	 * Registers a Processor - any kind of tool capable of using some
	 * of earlier obtained analysis results for further analysis.
	 * A Processor is created when some results suitable for further use
	 * are obtained. It is registered at the system's core and can be accessed from
	 * there for use.
	 */
	public void registerProcessor(Processor processor);

	/**
	 * Returns currently registered processors. The elements of the vector
	 * are instances of Processor (i.e. implement the interface Processor).
	 */
	public Vector getAvailableProcessors();

	/**
	 * Returns currently registered processors applicable to the given type
	 * of objects. The possible types of objects are defined in the interface Processor.
	 * The elements of the vector are instances of Processor
	 * (i.e. implement the interface Processor).
	 */
	public Vector getProcessorsForObjectType(int objType);

	/**
	* Returns the DataKeeper that provides access to all available tables and maps
	*/
	public DataKeeper getDataKeeper();

	/**
	* Returns the DataLoader used for loading or adding tables and maps to the system
	*/
	public DataLoader getDataLoader();

	/**
	* Returns actual system settings (valid for the currently running instance
	* of the system)
	*/
	public Parameters getSystemSettings();

	public void setSystemSettings(Parameters param);

	/**
	* There may be several tables and/or map layers, and for each of them an
	* individual highlighter is created. This method returns the highlighter
	* corresponding to the set of objects with the given identifier.
	* Set identifiers are used for linking tables to layers. The set identifier
	* of a table is not the same as the table identifier (container identifier).
	*/
	public Highlighter getHighlighterForSet(String setId);

	/**
	* There may be several tables and/or map layers, and for each of them an
	* individual highlighter is created. This method returns the highlighter
	* for the container (e.g. table or map layer) with the given identifier.
	* The container identifier is not the same as the identifier of the
	* object set this container refers to.
	*/
	public Highlighter getHighlighterForContainer(String containerId);

	/**
	* Makes the the specified objects selected (persistently highlighted).
	* The objects are specified through their identifiers and belong to the
	* specified container.
	* "source" is the object that initializes highlighting.
	*/
	public void selectObjects(Object source, String containerId, Vector objIds);

	/**
	* Tries to make the specified objects selected (persistently highlighted).
	* The objects are specified through their identifiers. The container
	* the objects belong to is unknown.
	* "source" is the object that initializes highlighting.
	*/
	public void selectObjects(Object source, Vector objIds);

	/**
	* Sets the system UI (the actual implementation of this interface depends
	* on the current system configuration)
	*/
	public void setUI(SystemUI ui);

	/**
	* Returns the system UI that can, in particular, display status messages
	*/
	public SystemUI getUI();

	/**
	* Removes the table with the given identifier from the system.
	*/
	public void removeTable(String tableId);

	/**
	* Removes the map layer with the given identifier from the system.
	* The second argument indicates whether the table associated with this layer
	* must be also removed
	*/
	public void removeMapLayer(String layerId, boolean removeThematicData);

	/**
	* Removes the map with the given index from the system, i.e. all the layers
	* composing this map. The second argument indicates whether the tables
	* associated with the layers must be removed as well.
	*/
	public void removeMap(int mapN, boolean removeThematicData);

	/**
	* Removes all currently loaded data and closes all data displays
	*/
	public void removeAllData();

	/**
	* Makes a full URL specification string from a relative one taking into
	* account the document base of the applet or, for a local variant, the
	* current working directory of the application.
	* The URL from the given path passed as the argument is formed in the
	* following way:
	* 1) If sourcePath already contains a protocol, i.e. http: or file:, it is
	*    returned without changing.
	* 2) If sourcePath starts with "/" or drive letter (i.e. this is an absolute
	*    path):
	*    2a) In a local variant the prefix "file://" is added to the sourcePath.
	*    2b) In an applet the protocol, the host name, and the port are taken
	*        from the document base of the applet, and the content of
	*        sourcePath is attached to them. If sourcePath contains a drive letter,
	*        it is removed.
	* 3) If sourcePath does not start with "/" or drive letter:
	*    3a) In a local variant the path to the current directory is attached to
	*        sourcePath at the beginning, and then the prefix "file://" is added.
	*    3b) In an applet the URL is formed by attaching the sourcePath to the
	*        document base of the applet.
	* Before analysing the content of sourcePath, the method replaces back slashes
	* by normal slashes ("/").
	*/
	public String getFullURLString(String sourcePath);

//ID
	/**
	 * Returns Snapshot Manager - the component dealing with saving and restoring of system states
	 */
	public SnapshotManager getSnapshotManager();

//~ID
	/**
	 * Adds the given action description to the actions log
	 */
	public void logAction(ActionDescr action);

	public void logSimpleAction(String action);

	/**
	 * Returns the log of the actions
	 */
	public Vector<ActionDescr> getLoggedActions();

	/**
	 * Prints the action log in the specified file
	 */
	public void printLoggedActions(String filePath);

	/**
	 * Generates HTML pages with the content specified in the given structure.
	 */
	public void makePages(PageCollection pages);

	/**
	 * updates current date and time in PageMaker and
	 * returns them as human-readable string and as filename prefix
	 */
	public String[] updateDTinPageMaker();

	/**
	 * Indicates whether the user may be asked about a comment before
	 * producing a page or collection of pages
	 */
	public boolean mayAskForComments();

	/**
	 * Sets whether the user may be asked about a comment before
	 * producing a page or collection of pages
	 */
	public void setMayAskForComments(boolean askForComments);
}