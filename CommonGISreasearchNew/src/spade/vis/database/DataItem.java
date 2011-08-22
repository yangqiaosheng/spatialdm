package spade.vis.database;

import spade.time.TimeReference;

public interface DataItem {
	/**
	* Returns the unique identifier of this data item. The identifier should
	* always be present.
	*/
	public String getId();

	/**
	* Changes the identifier of the item; use cautiously!
	*/
	public void setId(String ident);

	/**
	* Returns the name of this data item. Availability of a name is optional.
	*/
	public String getName();

	public void setName(String aName);

	/**
	* Informs whether the data item has a name.
	*/
	public boolean hasName();

	/**
	* Returns the index of this data item in the container in which it is included.
	* May return -1 if not included in any container.
	*/
	public int getIndexInContainer();

	/**
	* Sets the index of this data item in the container in which it is included.
	*/
	public void setIndexInContainer(int idx);

	/**
	* The method copyTo(DataItem) is used for updating data items and spatial
	* objects derived from them when data change events occur, for example,
	* in visualisation of temporal data.
	* The identifier of the data item is not copied! It is assumed that the
	* DataItem passed as an argument has the same identifier as this DataItem.
	*/
	public void copyTo(DataItem dit);

	/**
	* Produces and returns a copy of itself.
	*/
	public Object clone();

	/**
	* Associated this DataItem with a time reference.
	*/
	public void setTimeReference(TimeReference ref);

	/**
	* Returns its time reference
	*/
	public TimeReference getTimeReference();
}