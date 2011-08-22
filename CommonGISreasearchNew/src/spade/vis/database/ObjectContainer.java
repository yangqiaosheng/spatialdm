package spade.vis.database;

import java.beans.PropertyChangeListener;

import spade.lib.util.EntitySetContainer;
import spade.time.TimeReference;

/**
* An ObjectContainer provides access to a collection of data items, for
* example, table records or spatial data items associated with geographical
* objects. An ObjectContainer should notify about changes of the data items
* by sending propertyChange event with the name "ObjectSet" or "ObjectData".
*/
public interface ObjectContainer extends EntitySetContainer {
	/**
	* Returns the name of the container (that can be shown to the user)
	*/
	public String getName();

	/**
	* Registeres a listener of changes of object set and object data. The
	* listener must implement the PropertyChangeListener interface.
	*/
	public void addPropertyChangeListener(PropertyChangeListener l);

	/**
	* Unregisteres a listener of changes of object set and object data.
	*/
	public void removePropertyChangeListener(PropertyChangeListener l);

	/**
	* Reports whether there are any objects in this container.
	*/
	public boolean hasData();

	/**
	* If the data actually have not been loaded in the container yet, this method
	* loads them. Returns true if data has been successfully loaded.
	*/
	public boolean loadData();

	/**
	* Returns the number of currently available objects.
	*/
	public int getObjectCount();

	/**
	* Finds the object with the given identifier and returns its index.
	*/
	public int getObjectIndex(String id);

	/**
	* Returns the object with the given index.
	*/
	public Object getObject(int idx);

	/**
	* Returns the data item associated with the object at the given index.
	*/
	public DataItem getObjectData(int idx);

	/**
	* Returns the ID of the object with the given index. The result may be null.
	*/
	public String getObjectId(int idx);

	/**
	* Reports whether the objects in this container are temporally referenced.
	*/
	public boolean hasTimeReferences();

	/**
	 * If the objects in this container are time-referenced, returns the earliest
	 * and the latest times among the time references; otherwise returns null.
	 * If the time references have been previously transformed, returns the
	 * transformed times.
	 */
	public TimeReference getTimeSpan();

	/**
	 * If the objects in this container are time-referenced, returns the earliest
	 * and the latest times among the original time references irrespective of the
	 * current transformation of the times; otherwise returns null.
	 */
	public TimeReference getOriginalTimeSpan();

	/**
	 * Reports whether the objects in this container represent entities
	 * changing over time, e.g. moving, growing, shrinking, etc.
	 * The ObjectContainer returns true only if it contains data about
	 * these changes.
	 */
	public boolean containsChangingObjects();

	/**
	* An ObjectContainer can be linked to a filter of objects, in particular,
	* a time filter.
	*/
	public void setObjectFilter(ObjectFilter oFilter);

	/**
	* Returns its object filter
	*/
	public ObjectFilter getObjectFilter();

	/**
	* Removes the particular filter, possibly, from a combination of filters
	*/
	public void removeObjectFilter(ObjectFilter filter);

	/**
	 * This method is called after a transformation of the time references
	 * of the objects, e.g. from absolute to relative times. The ObjectContainer
	 * may need to change some of its internal settings.
	 */
	public void timesHaveBeenTransformed();
}