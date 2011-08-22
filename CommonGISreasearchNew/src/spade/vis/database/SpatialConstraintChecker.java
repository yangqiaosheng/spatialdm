package spade.vis.database;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Vector;

import spade.vis.geometry.Geometry;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Apr 23, 2008
 * Time: 11:09:39 AM
 * Can check if a given geometry satisfies some spatial constraint
 */
public abstract class SpatialConstraintChecker {
	/**
	 * Contains the listeners, which are informed about changes of the spatial constraint(s)
	 */
	protected Vector listeners = null;

	/**
	 * Informs if any spatial constraint is currently set
	 */
	public abstract boolean hasConstraint();

	/**
	 * Checks if a given geometry satisfies some spatial constraint,
	 * which is defined inside this object or elsewhere
	 */
	public abstract boolean doesSatisfySpatialConstraint(Geometry geom);

	/**
	 * The listeners are informed about changes of the spatial constraint(s)
	 */
	public void addConstraintChangeListener(PropertyChangeListener listener) {
		if (listener == null)
			return;
		if (listeners == null) {
			listeners = new Vector(20, 20);
		} else if (listeners.contains(listener))
			return;
		listeners.addElement(listener);
	}

	/**
	 * Removes the listener of changes of the spatial constraint(s)
	 */
	public void removeConstraintChangeListener(PropertyChangeListener listener) {
		if (listeners == null || listener == null)
			return;
		int idx = listeners.indexOf(listener);
		if (idx >= 0) {
			listeners.removeElementAt(idx);
		}
	}

	/**
	 * Notifies all its listeners about changes of the spatial constraint(s)
	 */
	public void notifyConstraintChange() {
		if (listeners == null || listeners.size() < 1)
			return;
		PropertyChangeEvent pce = new PropertyChangeEvent(this, "constraint", null, null);
		for (int i = 0; i < listeners.size(); i++) {
			((PropertyChangeListener) listeners.elementAt(i)).propertyChange(pce);
		}
	}

	/**
	 * Notifies all its listeners about its destroying
	 */
	public void notifyDestroy() {
		if (listeners == null || listeners.size() < 1)
			return;
		PropertyChangeEvent pce = new PropertyChangeEvent(this, "destroyed", null, null);
		for (int i = 0; i < listeners.size(); i++) {
			((PropertyChangeListener) listeners.elementAt(i)).propertyChange(pce);
		}
	}
}
