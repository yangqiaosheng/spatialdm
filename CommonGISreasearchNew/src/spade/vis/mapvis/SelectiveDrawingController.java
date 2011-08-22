package spade.vis.mapvis;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Vector;

import spade.vis.action.HighlightListener;
import spade.vis.action.Highlighter;

/**
* This class supports the "selective drawing" mode when not all geographical
* objects of a map layer are represented on the map (e.g. by diagrams) but only
* selected ones. A controller is attached to a visualizer and forces it to
* notify its listeners about visualization change when object selection changes.
* In its method getPresentation the visualizer must consult the controller
* whether to produce a representation for each particular object.
*/
public class SelectiveDrawingController implements HighlightListener {
	/**
	* The visualizer controlled by this SelectiveDrawingController
	*/
	protected Visualizer vis = null;
	/**
	* The highlighter the controller must listen to
	*/
	protected Highlighter highlighter = null;
	/**
	* Indicates whether the SignDrawer must produce signs for all objects or only
	* for selected ones
	*/
	protected boolean drawSelectedOnly = false;

	/**
	* Sets the visualizer to be controlled by this SelectiveDrawingController
	*/
	public void setVisualizer(Visualizer v) {
		vis = v;
	}

	/**
	* Sets the highlighter the controller must listen to.
	* The controller needs the highlighter in order to receive object selection
	* events. If the current mode is to represent only selected objects, each
	* change of the selected object set must cause map redrawing.
	*/
	public void setHighlighter(Highlighter hl) {
		highlighter = hl;
		if (drawSelectedOnly) {
			highlighter.addHighlightListener(this);
		}
	}

	/**
	* Tells whether the SignDrawer must produce signs for all objects or only for
	* selected ones
	*/
	public void setDrawSelectedOnly(boolean value) {
		if (drawSelectedOnly != value) {
			drawSelectedOnly = value;
			notifyModeChange();
			if (highlighter != null && vis != null) {
				if (drawSelectedOnly) {
					highlighter.addHighlightListener(this);
				} else {
					highlighter.removeHighlightListener(this);
				}
				vis.notifyVisChange();
			}
		}
	}

	/**
	* Informs whether the SignDrawer currently produces signs for all objects or
	* only for selected ones
	*/
	public boolean getDrawSelectedOnly() {
		return drawSelectedOnly;
	}

	/**
	* This method is used by the visualizer in order to know for which objects
	* to generate representations.
	*/
	public boolean mustDrawObject(String objId) {
		if (!drawSelectedOnly)
			return true;
		return highlighter.isObjectSelected(objId);
	}

//---------- methods from the HighlightListener interface: --------------------
	/**
	* Notification about change of the set of objects to be transiently
	* highlighted. The controller does not react to such events.
	*/
	@Override
	public void highlightSetChanged(Object source, String setId, Vector highlighted) {
	}

	/**
	* Notification about change of the set of objects to be selected (durably
	* highlighted). If the visualizer currently represents only selected objects,
	* it must notify about the change of visualisation.
	*/
	@Override
	public void selectSetChanged(Object source, String setId, Vector selected) {
		if (drawSelectedOnly) {
			vis.notifyVisChange();
		}
	}

//-------------- methods for notification about changing the mode ---------
	protected PropertyChangeSupport pcSupport = null;

	public void addPropertyChangeListener(PropertyChangeListener l) {
		if (l == null)
			return;
		if (pcSupport == null) {
			pcSupport = new PropertyChangeSupport(this);
		}
		pcSupport.addPropertyChangeListener(l);
	}

	public void removePropertyChangeListener(PropertyChangeListener l) {
		if (l == null || pcSupport == null)
			return;
		pcSupport.removePropertyChangeListener(l);
	}

	public void notifyModeChange() {
		if (pcSupport == null)
			return;
		pcSupport.firePropertyChange("Mode", null, new Boolean(drawSelectedOnly));
	}
//---------END-- methods for notification about changing the mode ---------
}
