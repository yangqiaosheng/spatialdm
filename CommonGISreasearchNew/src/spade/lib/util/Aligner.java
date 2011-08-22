package spade.lib.util;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
* The class is used to align several plots, e.g. a dot plot with a slider.
* One of the plots should set either horisontal or vertical margins,
* or both, and the other plots will align themselves accordingly.
* The aligner propagates information about changes of margins to
* registered PropertyChangeListeners
*/

public class Aligner {
	protected int left = -1, right = -1, top = -1, bottom = -1;

	public void setMargins(int left, int right, int top, int bottom) {
		if (this.left != left || this.right != right || this.top != top || this.bottom != bottom) {
			this.left = left;
			this.right = right;
			this.top = top;
			this.bottom = bottom;
			notifyMarginsChange();
		}
	}

	public int getLeft() {
		return left;
	}

	public int getRight() {
		return right;
	}

	public int getTop() {
		return top;
	}

	public int getBottom() {
		return bottom;
	}

	//----------------- notification about changes of margins ---------------
	/**
	* An Aligner may have listeners of changes of margins.
	* The listeners should implement the PropertyChangeListener interface.
	* To handle the list of listeners and notify them about changes of the
	* margins, the Aligner uses a PropertyChangeSupport.
	*/
	protected PropertyChangeSupport pcSupport = null;

	/**
	* Registeres a listener of changes of margins. The
	* listener must implement the PropertyChangeListener interface.
	*/
	public synchronized void addPropertyChangeListener(PropertyChangeListener l) {
		if (l == null)
			return;
		if (pcSupport == null) {
			pcSupport = new PropertyChangeSupport(this);
		}
		pcSupport.addPropertyChangeListener(l);
	}

	/**
	* Unregisteres a listener of changes of margins.
	*/
	public synchronized void removePropertyChangeListener(PropertyChangeListener l) {
		if (l == null || pcSupport == null)
			return;
		pcSupport.removePropertyChangeListener(l);
	}

	/**
	* An internal method used to notify all the listeners about changes of
	* margins.
	*/
	protected void notifyMarginsChange() {
		if (pcSupport == null)
			return;
		pcSupport.firePropertyChange("margins", null, null);
	}

}
