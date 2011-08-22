package spade.vis.database;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
* An ObjectFilter is used to support dynamic queries and other tools of
* filtering a subset of objects out of consideration (e.g. manually).
* So, an object may be in one of the two states: active or passive (filtered
* out). The ObjectFilter can inform whether a particular object (specified
* through the identifier or a data item associated with it) is active or not.
* It also registers listeners of changes and notifies them about changes of the
* subset of active objects.
* An object filter may be destroyed (deleted). In this case it sends
* a notification to the listeners about its destroying.
*/

public abstract class ObjectFilter implements PropertyChangeListener {
	/**
	* In an application there may be several sets of entities (for example, map
	* layers). An ObjectFilter always refers to a single set of entities.
	* Different sets of entities and are distinguished by identifiers.
	* An ObjectFilter should store the identifier of the set of entities
	* it is applied to.
	*/
	protected String setId = null;
	/**
	* An ObjectFilter may be associated with some container of objects.
	* This allows to ask the filter whether a particular object is active
	* by specifying just the index of this object in the container.
	*/
	protected ObjectContainer oCont = null;
	/**
	* Indicates whether the object set is currently filtered
	*/
	protected boolean filtered = false;
	/**
	* An ObjectFilter may have listeners of its properties changes, for example,
	* change of filter conditions resulting in changes of the set of active objects.
	* To handle the list of listeners and notify them about changes of the
	* properties, the ObjectFilter uses a standard PropertyChangeSupport.
	*/
	protected PropertyChangeSupport pcSupport = null;

	public synchronized void addPropertyChangeListener(PropertyChangeListener l) {
		if (l == null)
			return;
		if (pcSupport == null) {
			pcSupport = new PropertyChangeSupport(this);
		}
		pcSupport.addPropertyChangeListener(l);
	}

	public synchronized void removePropertyChangeListener(PropertyChangeListener l) {
		if (l == null || pcSupport == null)
			return;
		pcSupport.removePropertyChangeListener(l);
	}

	protected void notifyPropertyChange(String propName, Object oldValue, Object newValue) {
		if (pcSupport == null)
			return;
		pcSupport.firePropertyChange(propName, oldValue, newValue);
	}

	/**
	* Reacts to changes of data in the object container or the container being
	* destroyed.
	*/
	@Override
	abstract public void propertyChange(PropertyChangeEvent e);

	/**
	* Attaches this filter to an ObjectContainer
	*/
	public void setObjectContainer(ObjectContainer cont) {
		if (oCont != null)
			if (oCont.equals(cont))
				return;
			else {
				oCont.removePropertyChangeListener(this);
			}
		oCont = cont;
		updateData();
	}

	/**
	* May create or update its internal structures when attached to some object
	* container. By default, does nothing.
	*/
	protected void updateData() {
	}

	/**
	* Returns the ObjectContainer this filter is attached to
	*/
	public ObjectContainer getObjectContainer() {
		return oCont;
	}

	/**
	* Destroys the filter and sends a notification to all listeners about being
	* destroyed
	*/
	public void destroy() {
		filtered = false;
		setId = null;
		if (oCont != null) {
			oCont.removePropertyChangeListener(this);
		}
		notifyPropertyChange("destroyed", null, null);
	}

	//
	/**
	* Sets the identifier of the entity set the filter is applied to
	*/
	public void setEntitySetIdentifier(String identifier) {
		setId = identifier;
	}

	/**
	* Returns the identifier of the entity set the filter is applied to
	*/
	public String getEntitySetIdentifier() {
		return setId;
	}

	/**
	* Checks if the identifier passed as the argument is the same as the
	* identifier returned by the function getEntitySetIdentifier().
	*/
	public boolean isRelevantTo(String entitySetId) {
		boolean ok = entitySetId != null && entitySetId.equals(setId);
		return ok;
	}

	/**
	* Replies whether the specified object is active (i.e. not filtered out)
	*/
	abstract public boolean isActive(String id);

	/**
	* Replies whether the specified object is active (i.e. not filtered out).
	* For an attribute filter the data item should be a ThematicDataItem
	*/
	abstract public boolean isActive(DataItem item);

	/**
	* Checks whether the data item with the given index in the container is active,
	* i.e. not filtered out.
	*/
	abstract public boolean isActive(int idx);

	/**
	* Cancels the filter, i.e. makes all objects active
	* Should set the value of the variable "filtered" to false!
	*/
	abstract public void clearFilter();

	/**
	* Replies whether or not the object set is currently filtered
	*/
	public boolean areObjectsFiltered() {
		return filtered;
	}

	/**
	* Replies whether the filter is based on attribute values
	*/
	abstract public boolean isAttributeFilter();

}