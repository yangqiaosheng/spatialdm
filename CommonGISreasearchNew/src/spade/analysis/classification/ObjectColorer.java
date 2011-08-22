package spade.analysis.classification;

import java.awt.Color;
import java.beans.PropertyChangeListener;

import spade.vis.database.DataItem;
import spade.vis.database.ObjectContainer;

/**
* The interface ObjectColorer is used to facilitate propagation of object
* colors, e.g. resulting from some classification, among simultaneously
* existing data displays. This interface may be implemented, in particular, by
* various classifiers. An ObjectColorer may be given to the system supervisor
* in order to inform components about colors of objects.
*/

public interface ObjectColorer {
	/**
	* Returns the color for the object with the given identifier, depending on
	* the current classification.
	*/
	public Color getColorForObject(String objId);

	/**
	* Returns the color for the object with the given index in the container, depending on
	* the current classification.
	*/
	public Color getColorForObject(int objIdx);

	/**
	* Returns the color for the given data item, depending on the current
	* classification. The data item belongs to the container with the identifier
	* passed as the argument containerId. The ObjectColorer can check whether
	* the given data item is relevant for it.
	*/
	public Color getColorForDataItem(DataItem dit, String containerId);

	/**
	* Returns the color for the given data item, depending on the current
	* classification without checking the container of the data item.
	*/
	public Color getColorForDataItem(DataItem dit);

	/**
	 * Returns the identifier of the set of objects this ObjectColorer deals with
	 */
	public String getEntitySetIdentifier();

	/**
	 * Returns the identifier of the container this ObjectColorer deals with
	 */
	public String getContainerIdentifier();

	/**
	 * Returns a reference to the container with the colored objects
	 */
	public ObjectContainer getObjectContainer();

	/**
	* Registeres a listener of changes of object colors. The
	* listener must implement the PropertyChangeListener interface.
	*/
	public void addPropertyChangeListener(PropertyChangeListener l);

	public void removePropertyChangeListener(PropertyChangeListener l);
}