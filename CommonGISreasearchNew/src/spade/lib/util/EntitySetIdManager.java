package spade.lib.util;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Vector;

/**
* Produces unique identifiers of entity sets, remembers which entity set
* container refers to which entity set, returns identifiers of entity sets
* for given containers etc.
* Notifies about changes of entity set identifiers (i.e. situation when a
* container is re-attached to another entity set)
*/
public class EntitySetIdManager {
	/**
	* Used to produce unique identifiers
	*/
	protected int setN = 0;
	/**
	* The register of links between containers and entity sets.
	* This is a vector of pairs (arrays) of strings where the first string is the
	* identifier of a container and the second is the identifier of the
	* corresponding entity set.
	*/
	protected Vector links = null;
	/**
	* Helps in distributing notifications about changes of links (i.e. situations
	* when a container is re-attached to another entity set)
	*/
	protected PropertyChangeSupport pcSupport = null;

	/**
	* For the container with the given identifier returns the register record
	* (the pair <container-id>,<entity_set-id>) if it exists
	*/
	protected String[] getRegisterRecord(String containerId) {
		if (links == null || containerId == null)
			return null;
		for (int i = 0; i < links.size(); i++) {
			String pair[] = (String[]) links.elementAt(i);
			if (pair[0].equals(containerId))
				return pair;
		}
		return null;
	}

	/**
	* Produces a unique identifier to be used for a set of entities
	*/
	protected String makeSetId() {
		return "set_" + (++setN);
	}

	/**
	* Links the given container with the entity set specified by its identifier.
	* If the container was earlier linked to another entity set (and there
	* is a corresponding record in the register), the old link is replaced
	* by the new one, and the listeners are notified about the change.
	*/
	public void linkContainerToEntitySet(EntitySetContainer cont, String setId) {
		if (cont == null || setId == null)
			return;
		cont.setEntitySetIdentifier(setId);
		String pair[] = getRegisterRecord(cont.getContainerIdentifier());
		if (pair != null) {
			if (pair[1].equals(setId))
				return; //already linked
			String oldId = pair[1];
			pair[1] = setId;
			notifyPropertyChange("set_id", oldId, setId);
			return;
		}
		pair = new String[2];
		pair[0] = cont.getContainerIdentifier();
		pair[1] = setId;
		if (links == null) {
			links = new Vector(10, 10);
		}
		links.addElement(pair);
	}

	/**
	* Returns the set identifier for the container with the given identifier,
	* if there is a corresponding record in the register
	*/
	public String findEntitySetIdentifier(String containerId) {
		String pair[] = getRegisterRecord(containerId);
		if (pair != null)
			return pair[1];
		return null;
	}

	/**
	* Returns the set identifier for the container with the given identifier.
	* If there is no corresponding record in the register, produces a new
	* identifier.
	*/
	public String getEntitySetIdentifier(String containerId) {
		String id = findEntitySetIdentifier(containerId);
		if (id == null) {
			id = makeSetId();
		}
		return id;
	}

	/**
	* Checks if there is any container referring to the entity set with the
	* given identifier
	*/
	public boolean anyContainerRefersTo(String setId) {
		if (links == null || setId == null)
			return false;
		for (int i = 0; i < links.size(); i++) {
			String pair[] = (String[]) links.elementAt(i);
			if (pair[1].equals(setId))
				return true;
		}
		return false;
	}

	/**
	* Returns a vector of containers referring to the entity set with the
	* specified identifier
	*/
	public Vector getContainersReferringTo(String setId) {
		if (links == null || setId == null)
			return null;
		Vector result = null;
		for (int i = 0; i < links.size(); i++) {
			String pair[] = (String[]) links.elementAt(i);
			if (pair[1].equals(setId)) {
				if (result == null) {
					result = new Vector(5, 5);
				}
				result.addElement(pair[0]);
			}
		}
		return result;
	}

	/**
	* Removes the container with the given identifier from the register
	*/
	public void removeContainer(String containerId) {
		if (links == null || containerId == null)
			return;
		for (int i = 0; i < links.size(); i++) {
			String pair[] = (String[]) links.elementAt(i);
			if (pair[0].equals(containerId)) {
				links.removeElementAt(i);
				return;
			}
		}
	}

	public void addPropertyChangeListener(PropertyChangeListener pcl) {
		if (pcl == null)
			return;
		if (pcSupport == null) {
			pcSupport = new PropertyChangeSupport(this);
		}
		pcSupport.addPropertyChangeListener(pcl);
	}

	public void removePropertyChangeListener(PropertyChangeListener pcl) {
		if (pcl == null || pcSupport == null)
			return;
		pcSupport.removePropertyChangeListener(pcl);
	}

	public void notifyPropertyChange(String name, Object oldValue, Object newValue) {
		if (pcSupport != null) {
			pcSupport.firePropertyChange(name, oldValue, newValue);
		}
	}
}