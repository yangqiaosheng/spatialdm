package ui;

import java.beans.PropertyChangeEvent;

import spade.analysis.system.Supervisor;
import spade.vis.dmap.DGridLayer;
import spade.vis.geometry.RealRectangle;
import spade.vis.map.MapContext;
import spade.vis.map.MapViewer;
import spade.vis.mapvis.GridVisualizer;
import spade.vis.space.GeoLayer;
import spade.vis.space.LayerManager;

/**
* An advanced map view has some special code for dealing with grid layers.
*/
public class AdvancedMapView extends SimpleMapView {

	/*
	protected Component makeToolBar () {
	  return new AdvancedMapToolBar(this,supervisor,map,evtMeanMan);
	}
	*/
	public AdvancedMapView(Supervisor sup, LayerManager lman) {
		super(sup, lman);
	}

	@Override
	public MapViewer makeCopyAndClear() {
		if (lman == null)
			return null;
		RealRectangle rr = null;
		MapContext mc = map.getMapContext();
		if (mc != null) {
			rr = mc.getVisibleTerritory();
		}
		AdvancedMapView mw = new AdvancedMapView(supervisor, lman.makeCopy());
		mw.setup();
		moveManipulators(mw);
		for (int i = 0; i < lman.getLayerCount(); i++) {
			lman.getGeoLayer(i).setVisualizer(null);
			lman.getGeoLayer(i).setBackgroundVisualizer(null);
		}
		if (rr != null) {
			mw.showTerrExtent(rr.rx1, rr.ry1, rr.rx2, rr.ry2);
		}
		return mw;
	}

	/**
	* Reacts to selection of the active manipulator (if there are more than one):
	* activates the corresponding map layer
	*/
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if (evt.getPropertyName().equals("Visualization")) {
			if (evt != null && evt.getNewValue() != null && evt.getNewValue() instanceof DGridLayer) {
				DGridLayer l = (DGridLayer) evt.getNewValue();
				if (l.getLayerDrawn() && l.getGridVisualizer() != null) { // IDimprove - terrible code...
					removeManipulatorsOfLayer(l.getContainerIdentifier());
					addMapManipulator(l.getGridVisualizer().getGridManipulator(), l.getVisualizer(), l.getContainerIdentifier());
				}
			}
		}
		if (evt.getPropertyName().equals("LayerDrawn")) {
			if (evt != null && evt.getNewValue() != null && evt.getNewValue() instanceof DGridLayer) {
				DGridLayer l = (DGridLayer) evt.getNewValue();
				if (l.getLayerDrawn()) {
					removeManipulatorsOfLayer(l.getContainerIdentifier());
					addMapManipulator(l.getGridVisualizer().getGridManipulator(), l.getVisualizer(), l.getContainerIdentifier());
				} else {
					removeManipulatorsOfLayer(l.getContainerIdentifier());
				}
			}
		} else {
			super.propertyChange(evt);
		}
	}

//ID
	/**
	* Makes necessary preparations when a layer is added to the map.
	* In particular, adds a manipulator if a grid layer is added
	*/
	@Override
	protected void layerAdded(GeoLayer l) {
		super.layerAdded(l);
		// add manipulator for grid layer
		if (l instanceof DGridLayer) {
//          addMapManipulator(((GridVisualizer)l.getVisualizer()).getColorScaleManipulator(), l.getVisualizer(), l.getContainerIdentifier());
			addMapManipulator(((GridVisualizer) l.getVisualizer()).getGridManipulator(), l.getVisualizer(), l.getContainerIdentifier());
		}
	}
//~ID
}
