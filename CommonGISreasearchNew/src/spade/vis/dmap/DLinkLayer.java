package spade.vis.dmap;

import java.awt.Graphics;
import java.beans.PropertyChangeEvent;
import java.util.Vector;

import spade.lib.util.IntArray;
import spade.vis.database.DataTable;
import spade.vis.database.ObjectFilterBySelection;
import spade.vis.map.MapContext;
import spade.vis.space.GeoLayer;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Jan 30, 2009
 * Time: 4:43:57 PM
 * A layer with links (moves) between places.
 */
public class DLinkLayer extends DGeoLayer {
	/**
	 * A reference to the layer fwith the places connected by the links
	 */
	protected DGeoLayer placeLayer = null;
	/**
	 * Indicates which objects of the souLayer are active, i.e. satisfy
	 * the filters of the souLayer
	 */
	protected boolean placeActive[] = null;
	/**
	 * Indicates which objects of this layer have both the origin and destination places
	 * active
	 */
	protected boolean linkComplete[] = null;
	/**
	 * Used for preparation of the layer before the first drawing
	 */
	protected boolean neverDrawn = true;

	/**
	 * Returns the reference to the layer with the places.
	 */
	public DGeoLayer getPlaceLayer() {
		return placeLayer;
	}

	/**
	 * Sets a reference to the layer with the places.
	 * Starts listening to changes of the layer filter.
	 */
	public void setPlaceLayer(DGeoLayer souLayer) {
		if (this.placeLayer != null) {
			this.placeLayer.removePropertyChangeListener(this);
		}
		this.placeLayer = souLayer;
		if (souLayer != null) {
			souLayer.addPropertyChangeListener(this);
		}
	}

	/**
	 * Used for extending the filtering of the links by the places also
	 * to the table describing the properties of the links.
	 */
	protected ObjectFilterBySelection tblSelFilter = null;

	/**
	 * Finds or constructs a filter to be used for extending the filtering of the
	 * links by the places also to the table describing the properties of the links.
	 */
	protected boolean getFilterBySelection() {
		if (tblSelFilter != null)
			return true;
		if (dTable == null || dTable.getDataItemCount() < 1)
			return false;
		tblSelFilter = new ObjectFilterBySelection();
		tblSelFilter.setObjectContainer((DataTable) dTable);
		tblSelFilter.setEntitySetIdentifier(dTable.getEntitySetIdentifier());
		((DataTable) dTable).setObjectFilter(tblSelFilter);
		return true;
	}

	/**
	 * Finds out which of the places are currently active according
	 * to the filter of the layer with the places. Returns true if the active places
	 * have really changed
	 */
	protected boolean findActivePlaces() {
		if (placeLayer == null)
			return false;
		int nPlaces = placeLayer.getObjectCount();
		if (nPlaces < 1)
			return false;
		int nLinks = getObjectCount();
		if (nLinks < 1)
			return false;
		boolean changed = false;
		if (placeActive == null || placeActive.length != nPlaces) {
			placeActive = new boolean[nPlaces];
			for (int i = 0; i < nPlaces; i++) {
				placeActive[i] = true;
			}
		}
		if (linkComplete == null || linkComplete.length != nLinks) {
			linkComplete = new boolean[nLinks];
		}
		for (int i = 0; i < nLinks; i++) {
			linkComplete[i] = true;
		}
		for (int i = 0; i < placeActive.length; i++)
			if (placeActive[i] != placeLayer.isObjectActive(i)) {
				changed = true;
				placeActive[i] = !placeActive[i];
				if (!placeActive[i]) {
					String id = placeLayer.getObjectId(i);
					for (int j = 0; j < nLinks; j++)
						if (linkComplete[j] && (geoObj.elementAt(j) instanceof DLinkObject)) {
							DLinkObject link = (DLinkObject) geoObj.elementAt(j);
							if (link.getStartNode() != null && link.getStartNode().getIdentifier().equals(id)) {
								linkComplete[j] = false;
							}
							if (linkComplete[j] && link.getEndNode() != null && link.getEndNode().getIdentifier().equals(id)) {
								linkComplete[j] = false;
							}
						}
				}
			}
		if (changed && getFilterBySelection()) {
			IntArray activeAggrIdxs = new IntArray(geoObj.size(), 1);
			for (int j = 0; j < nLinks; j++)
				if (linkComplete[j]) {
					activeAggrIdxs.addElement(j);
				}
			if (activeAggrIdxs.size() == nLinks) {
				tblSelFilter.clearFilter();
			} else {
				tblSelFilter.setActiveObjectIndexes(activeAggrIdxs);
			}
		}
		return changed;
	}

	@Override
	public void draw(Graphics g, MapContext mc) {
		if (geoObj == null || geoObj.size() < 1 || g == null || mc == null)
			return;
		if (neverDrawn) {
			neverDrawn = false;
			findActivePlaces();
			return;
		}
		super.draw(g, mc);
	}

	/**
	* Determines whether the object with the given index is active, i.e. not
	* filtered out.
	*/
	@Override
	public boolean isObjectActive(int idx) {
		if (idx < 0 || geoObj == null || idx >= geoObj.size())
			return false;
		if (linkComplete != null && idx < linkComplete.length && !linkComplete[idx])
			return false;
		return super.isObjectActive(idx);
	}

	/**
	 * Reacts to changes of the filter of the trajectory layer(s)
	 */
	@Override
	public void propertyChange(PropertyChangeEvent pce) {
		if (!pce.getSource().equals(placeLayer)) {
			super.propertyChange(pce);
			return;
		}
		if (pce.getPropertyName().equals("ObjectFilter") || pce.getPropertyName().equals("ObjectData") || pce.getPropertyName().equals("ObjectSet")) {
			findActivePlaces();
		}
	}

	/**
	* Returns a copy of this GeoLayer. The reference to the visualizer is also copied.
	*/
	@Override
	public GeoLayer makeCopy() {
		DLinkLayer layer = new DLinkLayer();
		copyTo(layer);
		layer.setPlaceLayer(placeLayer);
		return layer;
	}

	/**
	 * Among the given copies of layers, looks for the copies of the
	 * layers this layer is linked to (by their identifiers) and replaces
	 * the references to the original layers by the references to their copies.
	 */
	public void checkAndCorrectLinks(Vector copiesOfMapLayers) {
		if (placeLayer == null || copiesOfMapLayers == null || copiesOfMapLayers.size() < 1)
			return;
		for (int i = 0; i < copiesOfMapLayers.size(); i++)
			if (copiesOfMapLayers.elementAt(i) instanceof DGeoLayer) {
				DGeoLayer layer = (DGeoLayer) copiesOfMapLayers.elementAt(i);
				if (layer.getEntitySetIdentifier().equals(placeLayer.getEntitySetIdentifier())) {
					setPlaceLayer(layer);
					break;
				}
			}
	}

	/**
	 * Removes itself from listeners of the filtering events of the layers
	 * with the trajectories
	 */
	@Override
	public void destroy() {
		if (destroyed)
			return;
		if (placeLayer != null) {
			placeLayer.removePropertyChangeListener(this);
		}
		if (tblSelFilter != null) {
			if (dTable != null) {
				((DataTable) dTable).removeObjectFilter(tblSelFilter);
			}
			tblSelFilter.destroy();
			tblSelFilter = null;
		}
		super.destroy();
	}
}
