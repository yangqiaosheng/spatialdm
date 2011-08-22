package spade.vis.spec;

import java.beans.PropertyChangeListener;
import java.util.Hashtable;

/**
* The interface to be implemented by all CommonGIS tools able to save and
* restore their states (e.g. cartographic visualizers, graphs, filters,
* dynamic computation tools, etc.). A tool state description (specification) is
* stored as a sequence of lines starting with <tagName> and ending with
* </tagName>, where tagName is a unique keyword for a particular class of tools.
*/
public interface SaveableTool {
	/**
	* Returns the keyword used in the opening tag of a stored state description
	* of this tool. A tool state description (specification) is stored as a
	* sequence of lines starting with <tagName> and ending with </tagName>, where
	* tagName is a unique keyword for a particular class of tools.
	*/
	public String getTagName();

	/**
	* Returns the specification (i.e. state description) of this tool for storing
	* in a file. The specification must allow correct re-construction of the tool.
	*/
	public Object getSpecification();

	/**
	* After the tool is constructed, it may be requested to setup its individual
	* properties according to the given list of stored properties.
	*/
	public void setProperties(Hashtable properties);

	/**
	* Adds a listener to be notified about destroying the tool.
	* A SaveableTool may be registered somewhere and, hence, must notify the
	* component where it is registered about its destroying.
	*/
	public void addDestroyingListener(PropertyChangeListener lst);

	/**
	* Sends a PropertyChangeEvent with the name "destroyed" to its
	* destroying listener(s), @see addDestroyingListener.
	*/
	public void destroy();
}