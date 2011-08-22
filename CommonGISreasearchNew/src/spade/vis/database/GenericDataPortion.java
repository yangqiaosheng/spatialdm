package spade.vis.database;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Hashtable;
import java.util.Vector;

import spade.lib.util.NotifyThread;
import spade.lib.util.StringUtil;

/**
* Contains a collection of DataItems. Has its corresponding data version mark
* (time stamp).
* A GenericDataPortion may have listeners of changes of the  data.
* The listeners should implement the PropertyChangeListener interface.
*/

public class GenericDataPortion implements DataPortion, java.io.Serializable {
	/**
	* In an application there may be several Data Portions loaded simultaneously.
	* in accordance with the EntitySetContainer interface, each portion has
	* its unique identifier
	*/
	protected String portionId = null;
	/**
	* The identifier of the set of entities the DataPortion refers to.
	* Data Portions may refer to different sets of entities. Each set has its
	* unique identifier (but this identifier may be common for several data
	* portions of for a table and a geographic layer).
	* The identifiers help in linking thematic and geographic data.
	*/
	protected String setId = null;
	/**
	 * A generic name of the entities in the container
	 */
	protected String genName = null;
	/**
	* The name of this data portion that may be shown to the user.
	*/
	protected String name = null;
	/**
	* Vector with the data. Elements of the vector are DataItems.
	*/
	protected Vector data = null;
	/**
	* Object index: for search optimisation
	*/
	protected Hashtable objIndex = null;
	/**
	* Allows or prohibits using of the object index. The index may not be used
	* if objects are inserted (not added to the end of the list) or removed
	*/
	protected boolean mayUseIndex = true;
	/**
	* This variable keeps a specification of the data source, i.e. all
	* information necessary for loading the data from a file or database.
	* Normally this is an instance of spade.vis.spec.DataSourceSpec.
	*/
	protected Object source = null;
	/**
	* An ObjectFilter associated with this DataPortion.
	*/
	protected ObjectFilter filter = null;
	/**
	* Indicates the "destroyed" (invalid) state. Initially is false.
	*/
	protected boolean destroyed = false;

	/**
	* A method from the EntitySetContainer interface. Returns the unique
	* identifier of the data portion.
	*/
	@Override
	public String getContainerIdentifier() {
		return portionId;
	}

	/**
	* Sets the unique identifier of this data portion
	*/
	@Override
	public void setContainerIdentifier(String tblId) {
		portionId = tblId;
	}

	/**
	* A method from the EntitySetContainer interface.
	* Returns the identifier of the set of entities the DataPortion refers to.
	* Data Portions may refer to different sets of entities. Each set has its
	* unique identifier (but this identifier may be common for several data
	* portions of for a table and a geographic layer).
	* The identifiers help in linking thematic and geographic data.
	*/
	@Override
	public String getEntitySetIdentifier() {
		return setId;
	}

	/**
	* A method from the EntitySetContainer interface.
	* Sets the identifier of the set of (geographic) objects referred to by this
	* data portion.
	*/
	@Override
	public void setEntitySetIdentifier(String id) {
		setId = id;
		if (filter != null) {
			filter.setEntitySetIdentifier(setId);
		}
	}

	/**
	 * Sets a generic name of the entities in the container
	 */
	@Override
	public void setGenericNameOfEntity(String name) {
		genName = name;
	}

	/**
	 * Returns the generic name of the entities in the container.
	 * May return null, if the name was not previously set.
	 */
	@Override
	public String getGenericNameOfEntity() {
		return genName;
	}

	/**
	* Returns the name of this data portion that may be shown to the user.
	* May return null.
	*/
	@Override
	public String getName() {
		return name;
	}

	/**
	* Sets the name of this data portion that may be shown to the user.
	*/
	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public boolean hasData() {
		return data != null && data.size() > 0;
	}

	@Override
	public int getDataItemCount() {
		if (data == null)
			return 0;
		return data.size();
	}

	public Vector getData() {
		return data;
	}

	/**
	* Returns the data item with the given index. The result may be null.
	*/
	@Override
	public DataItem getDataItem(int idx) {
		if (idx < 0 || idx >= getDataItemCount())
			return null;
		return (DataItem) data.elementAt(idx);
	}

	/**
	* Returns the ID of the data item with the given index. The result may be null.
	*/
	@Override
	public String getDataItemId(int idx) {
		DataItem dit = getDataItem(idx);
		if (dit == null)
			return null;
		return dit.getId();
	}

	/**
	* Returns the name of the data item with the given index. The result may be null.
	*/
	@Override
	public String getDataItemName(int idx) {
		DataItem dit = getDataItem(idx);
		if (dit == null)
			return null;
		return dit.getName();
	}

	/**
	* Returns the index of the data item with the given identifier or -1
	* when there is no such item.
	*/
	@Override
	public int indexOf(String identifier) {
		if (data == null || identifier == null)
			return -1;
		if (objIndex != null) {
			Object oInd = objIndex.get(identifier);
			if (oInd != null && (oInd instanceof Integer))
				return ((Integer) oInd).intValue();
			return -1;
		}
		for (int i = 0; i < data.size(); i++) {
			DataItem dit = (DataItem) data.elementAt(i);
			if (StringUtil.sameStringsIgnoreCase(identifier, dit.getId()))
				return i;
		}
		return -1;
	}

	/**
	* Reports if an object index is used. An index, if exists, may speed up the
	* search for data items by identifiers (method indexOf(String)).
	*/
	public boolean getUsesObjectIndex() {
		return mayUseIndex && objIndex != null;
	}

	/**
	* Adds the data item to its vector of data items.
	*/
	public void addDataItem(DataItem item) {
		if (item == null)
			return;
		if (data == null) {
			data = new Vector(1000, 100);
		}
		data.addElement(item);
		int idx = data.size() - 1;
		item.setIndexInContainer(idx);
		if (mayUseIndex) {
			if (objIndex == null) {
				objIndex = new Hashtable(1000);
			}
			objIndex.put(item.getId(), new Integer(idx));
		}
	}

	/**
	 * Completely rebuilds the index of data records.
	 * This operation makes sense when the table is complete and it is
	 * not expected to add new records.
	 */
	@Override
	public void rebuildDataIndex() {
		if (data == null || data.size() < 1)
			return;
		objIndex = new Hashtable(data.size());
		for (int i = 0; i < data.size(); i++) {
			objIndex.put(((DataItem) data.elementAt(i)).getId(), new Integer(i));
		}
		mayUseIndex = true;
	}

	/**
	* Inserts the data item to its vector of data items at the given position.
	*/
	public void insertDataItemAt(DataItem item, int pos) {
		if (item == null)
			return;
		if (data == null || pos < 0 || pos >= data.size()) {
			addDataItem(item);
			return;
		}
		data.insertElementAt(item, pos);
		item.setIndexInContainer(pos);
		if (objIndex != null) {
			objIndex = null;
			mayUseIndex = false;
			System.out.println("!--> Index destroyed in data portion <" + portionId + "> after inserting an item with id <" + item.getId() + ">.");
			System.out.println("!--> The further work with this data portion may slow down !!!");
		}
	}

	/**
	* Removes the data item with the given index from the data portion
	*/
	@Override
	public void removeDataItem(int idx) {
		if (data == null || idx < 0 || idx >= data.size())
			return;
		data.removeElementAt(idx);
		for (int i = idx; i < data.size(); i++) {
			((DataItem) data.elementAt(i)).setIndexInContainer(i);
		}
		if (objIndex != null) {
			objIndex = null;
			mayUseIndex = false;
			System.out.println("!--> Index destroyed in data portion <" + portionId + "> after removing an item at position <" + idx + ">.");
			System.out.println("!--> The further work with this data portion may slow down !!!");
		}
	}

	/**
	* Removes all the data from the DataPortion.
	*/
	@Override
	public void removeAllData() {
		if (data != null) {
			for (int i = 0; i < data.size(); i++) {
				((DataItem) data.elementAt(i)).setIndexInContainer(-1);
			}
			data.removeAllElements();
		}
		if (objIndex != null) {
			objIndex.clear();
		}
		mayUseIndex = true;
	}

	/**
	* Returns the specification of the data source, i.e. all
	* information necessary for loading the data from a file or database.
	* Normally this is an instance of spade.vis.spec.DataSourceSpec.
	*/
	@Override
	public Object getDataSource() {
		return source;
	}

	/**
	* Stores the specification of the data source
	*/
	public void setDataSource(Object sourceSpec) {
		source = sourceSpec;
	}

	/**
	* Returns an ObjectFilter associated with this DataPortion (if exists).
	*/
	@Override
	public ObjectFilter getObjectFilter() {
		return filter;
	}

	/**
	* Typically, must be re-implemented in descendants.
	*/
	public void setObjectFilter(ObjectFilter oFilter) {
		filter = oFilter;
	}

	/**
	* Typically, must be re-implemented in descendants.
	*/
	public void removeObjectFilter(ObjectFilter oFilter) {
		if (oFilter != null && oFilter.equals(filter)) {
			filter = null;
		}
	}

	/**
	* Reports whether the objects in this container are temporally referenced.
	* By default, returns false.
	*/
	public boolean hasTimeReferences() {
		return false;
	}

	/****************************************************************
	* notification about data changes
	*/
	/**
	* A GenericDataPortion may have listeners of changes of the  data.
	* The listeners should implement the PropertyChangeListener interface.
	* To handle the list of listeners and notify them about changes of the
	* object set or object data, the GenericDataPortion uses a
	* PropertyChangeSupport.
	*/
	protected PropertyChangeSupport pcSupport = null;

	/**
	* Registeres a listener of changes of the data. The
	* listener must implement the PropertyChangeListener interface.
	*/
	@Override
	public synchronized void addPropertyChangeListener(PropertyChangeListener l) {
		if (l == null)
			return;
		if (pcSupport == null) {
			pcSupport = new PropertyChangeSupport(this);
		}
		pcSupport.addPropertyChangeListener(l);
		//System.out.println("Added listener "+l.toString()+" to "+this.toString()+" ("+this.getName()+")");
	}

	/**
	* Unregisteres a listener of changes of the data.
	*/
	@Override
	public synchronized void removePropertyChangeListener(PropertyChangeListener l) {
		if (l == null || pcSupport == null)
			return;
		pcSupport.removePropertyChangeListener(l);
	}

	/**
	* The method used to notify all the listeners about changes of object
	* set and object data.
	*/
	@Override
	public void notifyPropertyChange(String propName, Object oldValue, Object newValue) {
		if (pcSupport == null)
			return;
		pcSupport.firePropertyChange(propName, oldValue, newValue);
	}

	/**
	 * Notifies about changes using a thread
	 */
	public void notifyPropertyChangeByThread(String propName, Object oldValue, Object newValue) {
		if (pcSupport == null)
			return;
		NotifyThread nt = new NotifyThread(pcSupport, propName, oldValue, newValue);
		nt.start();
	}

	/**
	* Sends notification to the listeners about being destroyed.
	*/
	@Override
	public void destroy() {
		notifyPropertyChange("destroyed", null, null);
		destroyed = true;
	}

	/**
	* Replies whether is destroyed or not
	*/
	@Override
	public boolean isDestroyed() {
		return destroyed;
	}

}
