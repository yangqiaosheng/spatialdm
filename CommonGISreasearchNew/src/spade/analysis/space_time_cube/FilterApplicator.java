package spade.analysis.space_time_cube;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.BitSet;
import java.util.Vector;

import javax.media.j3d.Switch;

import spade.lib.basicwin.Destroyable;
import spade.vis.database.ObjectContainer;
import spade.vis.database.ObjectFilter;
import spade.vis.dmap.DGeoLayer;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Apr 29, 2008
 * Time: 3:52:36 PM
 * Applies filtering to java3D shapes representing spatio-temporal objects
 */
public class FilterApplicator implements PropertyChangeListener, Destroyable {
	/**
	 * The container with the original objects
	 */
	protected ObjectContainer oCont = null;
	/**
	 * The drawn 3D objects, instances of SpaceTimeObject
	 */
	protected Vector<SpaceTimeObject> drawnObjects = null;
	/**
	 * The switcher of drawing of the objects
	 */
	protected Switch drawSwitch = null;

	public FilterApplicator(Switch drawSwitch) {
		this.drawSwitch = drawSwitch;
	}

	public void setObjectContainer(ObjectContainer oCont) {
		this.oCont = oCont;
		if (oCont != null) {
			oCont.addPropertyChangeListener(this);
		}
	}

	public void setDrawnObjects(Vector<SpaceTimeObject> drawnObjects) {
		this.drawnObjects = drawnObjects;
	}

	public void doFiltering() {
		if (oCont == null || drawSwitch == null || drawnObjects == null || drawnObjects.size() < 1)
			return;
		if (oCont instanceof DGeoLayer) {
			DGeoLayer layer = (DGeoLayer) oCont;
			if (!layer.getLayerDrawn()) {
				drawSwitch.setWhichChild(Switch.CHILD_NONE);
				return;
			}
			if (!layer.areObjectsFiltered()) {
				drawSwitch.setWhichChild(Switch.CHILD_ALL);
				return;
			}
			BitSet bits = new BitSet(drawnObjects.size());
			for (int i = 0; i < drawnObjects.size(); i++) {
				SpaceTimeObject stobj = drawnObjects.elementAt(i);
				bits.set(i, layer.isObjectActive(stobj.getGeoObjectIdxInContainer()));
			}
			drawSwitch.setWhichChild(Switch.CHILD_MASK);
			drawSwitch.setChildMask(bits);
			return;
		}
		ObjectFilter filter = oCont.getObjectFilter();
		if (filter == null || !filter.areObjectsFiltered()) {
			drawSwitch.setWhichChild(Switch.CHILD_ALL);
			return;
		}
		BitSet bits = new BitSet(drawnObjects.size());
		for (int i = 0; i < drawnObjects.size(); i++) {
			SpaceTimeObject stobj = drawnObjects.elementAt(i);
			bits.set(i, filter.isActive(stobj.getGeoObjectIdxInContainer()));
		}
		drawSwitch.setWhichChild(Switch.CHILD_MASK);
		drawSwitch.setChildMask(bits);
	}

	/**
	 * Reacts to changes of the filter of the object container
	 */
	@Override
	public void propertyChange(PropertyChangeEvent pce) {
		if (pce.getSource() == oCont) {
			if (pce.getPropertyName().equalsIgnoreCase("ObjectFilter") || pce.getPropertyName().equalsIgnoreCase("filter")) {
				doFiltering();
			} else if (pce.getPropertyName().equalsIgnoreCase("destroyed")) {
				destroy();
			} else if (pce.getPropertyName().equalsIgnoreCase("LayerDrawn")) {
				DGeoLayer layer = (DGeoLayer) oCont;
				if (layer.getLayerDrawn()) {
					doFiltering();
				} else {
					drawSwitch.setWhichChild(Switch.CHILD_NONE);
				}
			}
		}
	}

	protected boolean destroyed = false;

	@Override
	public boolean isDestroyed() {
		return destroyed;
	}

	@Override
	public void destroy() {
		if (destroyed)
			return;
		System.out.println("FilterApplicator is destroyed");
		if (oCont != null) {
			oCont.removePropertyChangeListener(this);
		}
		destroyed = true;
	}
}
