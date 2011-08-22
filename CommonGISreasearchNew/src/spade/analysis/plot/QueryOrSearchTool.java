package spade.analysis.plot;

import java.util.Vector;

import spade.analysis.system.Supervisor;
import spade.vis.database.ObjectContainer;

/**
* The interface to be implemented by components used for query or search.
* Any such component works with an ObjectContainer
* (spade.vis.database.ObjectContainer), which may be, in particular, a table.
*/
public interface QueryOrSearchTool {
	/**
	* Sets a reference to the system's supervisor used for propagating events
	* among system's components.
	*/
	public void setSupervisor(Supervisor sup);

	/**
	* Sets a reference to the ObjectContainer this tool must work with
	*/
	public void setObjectContainer(ObjectContainer oCont);

	/**
	* Returns the reference to the ObjectContainer this tool works with
	*/
	public ObjectContainer getObjectContainer();

	/**
	* Sets the identifiers of the attributes to be used in the tool.
	*/
	public void setAttributeList(Vector attr);

	/**
	* Constructs the tool using the earlier provided references to the supervisor
	* and the object container. Returns true if successfully constructed.
	*/
	public boolean construct();

	/**
	* If could not construct itself, returns the error message explaining the
	* reason of the failure
	*/
	public String getErrorMessage();
}
