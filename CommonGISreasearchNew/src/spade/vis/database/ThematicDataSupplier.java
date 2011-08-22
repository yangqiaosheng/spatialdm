package spade.vis.database;

/**
* An ObjectDataSupplier provides access to thematic data associated with some
* objects, e.g. geographical objects. This interface is implemented by
* the class ObjectManager meant to manage GeoObjects of some GeoLayer.
* However, for all other classes that need access to data about objects
* the nature of objects is irrelevant (e.g. for visualizers, statistics
* providers, additional displays, manipulators etc.) Therefore thay may
* refer to the ObjectDataSupplier interface that, in principle, may be
* implemented by any class other than ObjectManager.
* An ObjectDataSupplier should notify about changes of thematic data
* and changes of the set of objects.
*/

public interface ThematicDataSupplier extends ObjectContainer {
	/**
	* Returns thematic data associated with the object at the given index.
	*/
	public ThematicDataItem getThematicData(int idx);

	/**
	* Finds object with the given identifier and returns the data
	* associated with the object.
	*/
	public ThematicDataItem getThematicData(String objectId);
}
