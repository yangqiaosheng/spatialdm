package spade.analysis.plot;

import java.awt.BorderLayout;
import java.awt.List;
import java.awt.Panel;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Vector;

import spade.analysis.system.Supervisor;
import spade.lib.basicwin.Destroyable;
import spade.lib.util.IntArray;
import spade.lib.util.StringUtil;
import spade.vis.action.HighlightListener;
import spade.vis.action.Highlighter;
import spade.vis.map.MapViewer;
import spade.vis.map.PlaceMarker;
import spade.vis.space.GeoLayer;
import spade.vis.space.GeoObject;
import spade.vis.space.LayerManager;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: 12-Nov-2007
 * Time: 10:18:31
 * Shows a list of identifiers and names of objects from a map layer.
 * These are not necessarily all objects of the layer but may be a subset
 * of objects.
 * The user can select objects in this list and look where they are situated
 * on a map.
 */
public class GeoObjectSelector extends Panel implements ItemListener, HighlightListener, Destroyable {
	/**
	 * The map layer containing the objects
	 */
	protected GeoLayer layer = null;
	/**
	 * Indicates whether the layer was initially visible
	 */
	protected boolean layerWasVisible = true;
	/**
	 * The layer manager of the map (to activate the layer with the objects)
	 */
	protected LayerManager lman = null;

	protected Supervisor supervisor = null;
	/**
	 * Used for adjusting the map view so that all selected objects are visible
	 */
	protected MapViewer mapView = null;
	/**
	* The objects in the list are sorted. This array specifies the order of the
	* objects, i.e. which record corresponds to each position in the list.
	*/
	protected IntArray order = null;
	/**
	 * The objects to select from (the elements are instances of GeoObject).
	 */
	protected Vector geoObjects = null;
	/**
	* The list containing object names
	*/
	protected List nameList = null;
	/**
	 * Additional markers of selected objects
	 */
	protected PlaceMarker placeMarkers[] = null;
	/**
	* Indicates "destroyed" state. Initially is false.
	*/
	protected boolean destroyed = false;

	public GeoObjectSelector(GeoLayer layer, Vector objects, LayerManager lman, Supervisor supervisor, boolean multipleSelect) {
		this.layer = layer;
		this.geoObjects = objects;
		this.lman = lman;
		this.supervisor = supervisor;
		if (geoObjects == null) {
			geoObjects = layer.getObjects();
		}
		if (geoObjects == null || geoObjects.size() < 1)
			return;

		setLayout(new BorderLayout());
		nameList = new List(Math.min(10, geoObjects.size()), multipleSelect);
		add(nameList, BorderLayout.CENTER);
		nameList.addItemListener(this);
		boolean namesDifferFromIds = false;
		for (int i = 0; i < geoObjects.size() && !namesDifferFromIds; i++) {
			GeoObject obj = (GeoObject) geoObjects.elementAt(i);
			String str = obj.getName();
			namesDifferFromIds = str == null || str.length() < 1 || !str.equals(obj.getIdentifier());
		}
		order = new IntArray(geoObjects.size(), 1);
		for (int i = 0; i < geoObjects.size(); i++) {
			GeoObject obj = (GeoObject) geoObjects.elementAt(i);
			String str = obj.getName();
			if (str == null || str.length() == 0) {
				str = obj.getIdentifier();
			} else if (namesDifferFromIds) {
				str = obj.getIdentifier() + "   " + str;
			}
			if (str == null || str.length() == 0) {
				continue;
			}
			int k = nameList.getItemCount();
			for (int j = 0; j < nameList.getItemCount(); j++)
				if (str.compareTo(nameList.getItem(j)) < 0) {
					k = j;
					break;
				}
			nameList.add(str, k);
			order.insertElementAt(i, k);
		}
		if (lman != null) {
			lman.activateLayer(layer.getContainerIdentifier());
		}
		if (supervisor != null) {
			supervisor.registerHighlightListener(this, layer.getEntitySetIdentifier());
			Highlighter highlighter = supervisor.getHighlighter(layer.getEntitySetIdentifier());
			if (highlighter != null) {
				highlighter.clearSelection(this);
			}
		}
		layerWasVisible = layer.getLayerDrawn();
		layer.setLayerDrawn(true);
	}

	public void setMapView(MapViewer mapView) {
		this.mapView = mapView;
	}

	@Override
	public void highlightSetChanged(Object source, String setId, Vector hlObj) {
	}

	@Override
	public void selectSetChanged(Object source, String setId, Vector hlObj) {
		if (supervisor == null || layer == null || geoObjects == null)
			return;
		if (source.equals(this))
			return;
		if (!StringUtil.sameStrings(setId, layer.getEntitySetIdentifier()))
			return;
		Highlighter highlighter = supervisor.getHighlighter(setId);
		if (highlighter == null)
			return;
		int nSelected = 0;
		for (int i = 0; i < nameList.getItemCount(); i++) {
			GeoObject obj = (GeoObject) geoObjects.elementAt(order.elementAt(i));
			String id = obj.getIdentifier();
			if (highlighter.isObjectSelected(id)) {
				nameList.select(i);
				++nSelected;
				markObject(obj, i);
			} else {
				nameList.deselect(i);
				unmarkObject(i);
			}
		}
		if (nSelected > 1 && !nameList.isMultipleMode()) {
			int selNums[] = nameList.getSelectedIndexes();
			if (selNums != null && selNums.length > 0) {
				GeoObject obj = (GeoObject) geoObjects.elementAt(order.elementAt(selNums[0]));
				Vector selId = new Vector(1, 1);
				selId.addElement(obj.getIdentifier());
				highlighter.replaceSelectedObjects(this, selId);
			}
		}
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource().equals(nameList)) {
			if (supervisor == null)
				return;
			Highlighter highlighter = supervisor.getHighlighter(layer.getEntitySetIdentifier());
			if (highlighter == null)
				return;
			int selNums[] = nameList.getSelectedIndexes();
			if (selNums == null || selNums.length < 1) {
				eraseAllMarkers();
				highlighter.clearSelection(this);
			} else {
				unmarkUnselectedObjects();
				float x1 = Float.NaN, x2 = x1, y1 = x1, y2 = x1;
				Vector v = new Vector(selNums.length, 10);
				for (int idx : selNums) {
					GeoObject obj = (GeoObject) geoObjects.elementAt(order.elementAt(idx));
					v.addElement(obj.getIdentifier());
					if (placeMarkers == null || placeMarkers[idx] == null) {
						markObject(obj, idx);
					}
					if (mapView != null && obj.getGeometry() != null) {
						float b[] = obj.getGeometry().getBoundRect();
						if (b != null) {
							if (Float.isNaN(x1) || x1 > b[0]) {
								x1 = b[0];
							}
							if (Float.isNaN(x2) || x2 < b[2]) {
								x2 = b[2];
							}
							if (Float.isNaN(y1) || y1 > b[1]) {
								y1 = b[1];
							}
							if (Float.isNaN(y2) || y2 < b[3]) {
								y2 = b[3];
							}
						}
					}
				}
				mapView.getMapDrawer().adjustExtentToShowArea(x1, y1, x2, y2);
				highlighter.replaceSelectedObjects(this, v);
			}
			if (lman != null) {
				lman.activateLayer(layer.getContainerIdentifier());
			}
		}
	}

	protected void markObject(int objIdx) {
		if (objIdx < 0)
			return;
		markObject((GeoObject) geoObjects.elementAt(order.elementAt(objIdx)), objIdx);
	}

	protected void markObject(GeoObject obj, int objIdx) {
		if (obj == null || obj.getGeometry() == null || mapView == null)
			return;
		if (placeMarkers == null) {
			placeMarkers = new PlaceMarker[nameList.getItemCount()];
			for (int i = 0; i < placeMarkers.length; i++) {
				placeMarkers[i] = null;
			}
		}
		if (placeMarkers[objIdx] == null) {
			placeMarkers[objIdx] = new PlaceMarker(obj.getGeometry(), mapView.getMapDrawer(), 2, 2);
		}
	}

	protected void unmarkObject(int objIdx) {
		if (placeMarkers == null || objIdx < 0 || objIdx >= placeMarkers.length || placeMarkers[objIdx] == null)
			return;
		placeMarkers[objIdx].destroy();
		placeMarkers[objIdx] = null;
	}

	protected void unmarkUnselectedObjects() {
		if (placeMarkers == null)
			return;
		for (int i = 0; i < placeMarkers.length; i++)
			if (placeMarkers[i] != null && !nameList.isIndexSelected(i)) {
				placeMarkers[i].destroy();
				placeMarkers[i] = null;
			}
	}

	protected void eraseAllMarkers() {
		if (placeMarkers != null && placeMarkers.length > 0) {
			for (int i = 0; i < placeMarkers.length; i++)
				if (placeMarkers[i] != null) {
					placeMarkers[i].destroy();
					placeMarkers[i] = null;
				}
		}
	}

	@Override
	public void destroy() {
		if (destroyed)
			return;
		eraseAllMarkers();
		if (supervisor != null && layer != null) {
			Highlighter highlighter = supervisor.getHighlighter(layer.getEntitySetIdentifier());
			if (highlighter != null) {
				highlighter.clearSelection(this);
			}
			supervisor.removeHighlightListener(this, layer.getEntitySetIdentifier());
		}
		if (!layerWasVisible) {
			layer.setLayerDrawn(false);
		}
		destroyed = true;
	}

	/**
	* Replies whether is destroyed or not
	*/
	@Override
	public boolean isDestroyed() {
		return destroyed;
	}

	/**
	 * Returns the selected objects (instances of GeoObject)
	 */
	public Vector getSelectedObjects() {
		int selNums[] = nameList.getSelectedIndexes();
		if (selNums == null || selNums.length < 1)
			return null;
		Vector v = new Vector(selNums.length, 10);
		for (int selNum : selNums) {
			GeoObject obj = (GeoObject) geoObjects.elementAt(order.elementAt(selNum));
			v.addElement(obj);
		}
		return v;
	}
}
