package spade.vis.database;

import java.beans.PropertyChangeListener;

import spade.lib.util.EntitySetContainer;

/**
* A DataPortion contains a collection of data items each having its identifier
* and name (the name may be the same as the identifier).
*/

public interface DataPortion extends EntitySetContainer {
	/**
	* A method from the EntitySetContainer interface.
	* Returns the identifier of the set of entities the DataPortion refers to.
	* Data Portions may refer to different sets of entities. Each set has its
	* unique identifier (but this identifier may be common for several data
	* portions of for a table and a geographic layer).
	* The identifiers help in linking thematic and geographic data.
	*/
	@Override
	public String getEntitySetIdentifier();

	/**
	* A method from the EntitySetContainer interface.
	* Sets the identifier of the set of (geographic) objects referred to by this
	* data portion.
	*/
	@Override
	public void setEntitySetIdentifier(String id);

	/**
	* A method from the EntitySetContainer interface. Returns the unique
	* identifier of the data portion.
	*/
	@Override
	public String getContainerIdentifier();

	/**
	* Sets the unique identifier of this data portion
	*/
	public void setContainerIdentifier(String portionId);

	/**
	* Returns the name of this data portion that may be shown to the user.
	* May return null.
	*/
	public String getName();

	/**
	* Sets the name of this data portion that may be shown to the user.
	*/
	public void setName(String name);

	/**
	* Returns the specification of the data source, i.e. all
	* information necessary for loading the data from a file or database.
	* Normally this is an instance of spade.vis.spec.DataSourceSpec.
	*/
	public Object getDataSource();

	/**
	* Reports whether there are any DataItems in this DataPortion.
	*/
	public boolean hasData();

	/**
	* Returns the number of DataItems in this DataPortion.
	*/
	public int getDataItemCount();

	/**
	* Returns the data item with the given index
	*/
	public DataItem getDataItem(int idx);

	/**
	* Returns the ID of the data item with the given index. The result may be null.
	*/
	public String getDataItemId(int idx);

	/**
	* Returns the name of the data item with the given index. The result may be null.
	*/
	public String getDataItemName(int idx);

	/**
	* Returns the index of the data item with the given identifier or -1
	* when there is no such item.
	*/
	public int indexOf(String idx);

	/**
	* Removes the data item with the given index from the data portion
	*/
	public void removeDataItem(int idx);

	/**
	* Removes all the data from the DataPortion.
	*/
	public void removeAllData();

	/**
	 * Completely rebuilds the index of data records.
	 * This operation makes sense when the table is complete and it is
	 * not expected to add new records.
	 */
	public void rebuildDataIndex();

	/**
	* Returns an ObjectFilter associated with this DataPortion (if exists).
	*/
	public ObjectFilter getObjectFilter();

	/**
	* Registers a listener of data changes
	*/
	public void addPropertyChangeListener(PropertyChangeListener l);

	/*
	* Removes a listener of data changes
	*/
	public void removePropertyChangeListener(PropertyChangeListener l);

	/**
	* The method used to notify all the listeners about changes of object
	* set and object data.
	*/
	public void notifyPropertyChange(String propName, Object oldValue, Object newValue);

	/**
	* Sends notification to the listeners about being destroyed.
	*/
	public void destroy();

	/**
	* Replies whether is destroyed or not
	*/
	public boolean isDestroyed();
}
