package spade.vis.dmap;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.lib.basicwin.CManager;
import spade.lib.basicwin.OKDialog;
import spade.lib.basicwin.TextCanvas;
import spade.lib.lang.Language;
import spade.vis.database.CombinedFilter;
import spade.vis.database.ObjectFilter;
import spade.vis.database.SpatialFilter;
import spade.vis.event.DEvent;
import spade.vis.event.DMouseEvent;
import spade.vis.event.EventReceiver;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.RealRectangle;
import spade.vis.map.Legend;
import spade.vis.map.LegendDrawer;
import spade.vis.map.MapContext;
import spade.vis.map.Mappable;
import spade.vis.mapvis.MultiMapVisualizer;
import spade.vis.mapvis.Visualizer;
import spade.vis.space.GeoLayer;
import spade.vis.space.LayerManager;
import spade.vis.space.ObjectManager;

/**
* DLayerManager contains a set of GeoLayers and cares about the order of their
* drawing, putting diagrams, labels, etc. DLayerManager receives notifications
* about changes of properties from its layers and propagates them to its
* own listeners. Besides, a DLayerManager may generate its own events, e.g.
* when the order of the layers changed.
* Typical listeners of properties changes of a DLayerManager
* are MapCanvas and MapLegend.
* The changes that may happen with layers may be classified into 3 kinds:
* 1) changes that affect both the map and the legend: the order of the layers,
*    adding/removing of a layer, switching on/off, change of colors etc.;
* 2) changes that affect only the map but not the legend: change of the
*    set of drawn GeoObjects (e.g. when the map is shifted), switching
*    on/off drawing of labels;
* 3) changes that affect only the legend but not the map, e.g. changes of data
*    statistics that are shown in the legend.
* Therefore the DLayerManager manages two different lists of listeners,
* one for sending events requiring map redrawing and another for events
* requiring redrawing of the legend. The DLayerManager recognizes itself
* when to inform one of the groups of listeners and when both groups.
*/

public class DLayerManager implements LayerManager, PropertyChangeListener, Mappable, EventReceiver {
	static ResourceBundle res = Language.getTextResource("spade.vis.dmap.Res");
	/**
	* Used to generate unique identifiers of instances of DLayerManager
	*/
	protected static int nInstances = 0;
	protected int instanceN = 0;
//-------------- management of properties changes ---------------
	/**
	* Listeners of changes affecting map appearance
	*/
	protected Vector mapPropCL = null;
	/**
	* Listeners of changes affecting legend content
	*/
	protected Vector legendPropCL = null;
	/**
	* Listeners of status events
	*/
	protected Vector statusListeners = null;

	/**
	* The method of the Mappable interface.
	* Registers a new PropertyChangeListener. Requires to specify if the
	* listener is interested to be notified about changing affecting map
	* appearance and about changes affecting legend. A listener may listen
	* to both kinds of changes. If none of the boolean arguments
	* listensMapAffectingChanges and listensLegendAffectingChanges is true,
	* the listener is not registered.
	*/
	@Override
	public synchronized void addPropertyChangeListener(PropertyChangeListener l, boolean listensMapAffectingChanges, boolean listensLegendAffectingChanges) {
		if (l == null)
			return;
		if (listensMapAffectingChanges) {
			if (mapPropCL == null) {
				mapPropCL = new Vector(5, 5);
			}
			if (!mapPropCL.contains(l)) {
				mapPropCL.addElement(l);
			}
		}
		if (listensLegendAffectingChanges) {
			if (legendPropCL == null) {
				legendPropCL = new Vector(5, 5);
			}
			if (!legendPropCL.contains(l)) {
				legendPropCL.addElement(l);
			}
		}
	}

	public synchronized void addPropertyChangeListener(PropertyChangeListener l, boolean listensMapAffectingChanges, boolean listensLegendAffectingChanges, boolean listensStatusEvents) {
		if (l == null)
			return;
		addPropertyChangeListener(l, listensMapAffectingChanges, listensLegendAffectingChanges);
		if (listensStatusEvents) {
			if (statusListeners == null) {
				statusListeners = new Vector(5, 5);
			}
			if (!statusListeners.contains(l)) {
				statusListeners.addElement(l);
			}
		}
	}

	/**
	* Removes an earlier registered listener from both lists.
	*/
	@Override
	public synchronized void removePropertyChangeListener(PropertyChangeListener l) {
		if (l == null)
			return;
		if (mapPropCL != null) {
			int idx = mapPropCL.indexOf(l);
			if (idx >= 0) {
				mapPropCL.removeElementAt(idx);
			}
		}
		if (legendPropCL != null) {
			int idx = legendPropCL.indexOf(l);
			if (idx >= 0) {
				legendPropCL.removeElementAt(idx);
			}
		}
		if (statusListeners != null) {
			int idx = statusListeners.indexOf(l);
			if (idx >= 0) {
				statusListeners.removeElementAt(idx);
			}
		}
	}

	/**
	* Notifies the listeners about the property change.
	* Depending on the values of the arguments affectsMap and affectsLegend,
	* notifies listeners from one of the lists or from both. If a listener
	* is present in both lists, it is notified only once.
	*/
	@Override
	public void notifyPropertyChange(String propName, Object oldValue, Object newValue, boolean affectsMap, boolean affectsLegend) {
		if (!(affectsMap || affectsLegend)) {
			if (statusListeners != null && statusListeners.size() > 0) {
				PropertyChangeEvent evt = new PropertyChangeEvent(this, propName, oldValue, newValue);
				for (int i = 0; i < statusListeners.size(); i++) {
					((PropertyChangeListener) statusListeners.elementAt(i)).propertyChange(evt);
				}
			}
			return;
		}
		if (mapPropCL == null && legendPropCL == null)
			return;
		PropertyChangeEvent evt = new PropertyChangeEvent(this, propName, oldValue, newValue);
		if (affectsMap && mapPropCL != null) {
			for (int i = 0; i < mapPropCL.size(); i++) {
				((PropertyChangeListener) mapPropCL.elementAt(i)).propertyChange(evt);
			}
		}
		if (affectsLegend && legendPropCL != null) {
			for (int i = 0; i < legendPropCL.size(); i++) {
				PropertyChangeListener pcl = (PropertyChangeListener) legendPropCL.elementAt(i);
				if (mapPropCL == null || !mapPropCL.contains(pcl) || !affectsMap) {
					pcl.propertyChange(evt);
				}
			}
		}
	}

//---------END----- management of properties changes ---------------
	/**
	* Name of the territory - should be got from the map description
	*/
	public String terrName = null;
	/**
	* Initial territory extent
	*/
	public float initialExtent[] = null;
	/**
	* Full territory extent (may be undefined)
	*/
	public float fullExtent[] = null;
	/**
	* User-defined scaling factor - should be got from the map description
	*/
	public float user_factor = 1.0f;
	/**
	* User-defined unit in which coordinates are specified - should be got from
	* the map description
	*/
	protected String user_unit = "m";
	/**
	 * Indicates if the coordinates are geographic:
	 * 0 - not, 1 - yes, -1 - unknown
	 */
	protected int coordsAreGeographic = -1;
//ID
	/**
	* A parameter controlling visibility of a legend
	*/
	public boolean show_legend = true;
	/**
	* Percentage of space taken by legend in application window
	*/
	public int percent_of_legend = 30;
	/**
	* A parameter controlling visibility of a territory name
	*/
	public boolean show_terrname = true;
	/**
	* A parameter controlling visibility of a background color change control
	*/
	public boolean show_bgcolor = true;
	/**
	* A parameter controlling visibility of a scale
	*/
	public boolean show_scale = true;
	/**
	* Indicates if total number of objects is displayed in legend.
	*/
	public boolean show_nobjects = true;
//~ID
	/**
	* A parameter controlling visibility of a legend
	*/
	public boolean show_manipulator = true;
	/**
	* Percentage of space taken by legend in application window
	*/
	public int percent_of_manipulator = 30;
	/**
	* User-defined territory background color - should be got from
	* the map description and updated from MapCanvas (if changed by user)
	*/
	public Color terrBgColor = Color.lightGray;
	/**
	* Vector of GeoLayers.
	*/
	protected Vector layers = null;
	/**
	* Array of rectangles within which the layers draw themselves in the legend -
	* needed for selection and highlighting of an "active" layer
	*/
	protected Rectangle[] rectangles = null;
	/**
	* The index of the active GeoLayer
	*/
	protected int activeIdx = -1;
	/**
	* The ObjectManager is used to support highlighting of objects of the active
	* GeoLayer. When the active layer changes, the ObjectManager must be linked
	* to the new active layer.
	*/
	protected ObjectManager objMan = null;
	/**
	* What layer is currently being edited
	*/
	protected DGeoLayer editedLayer = null;
	/**
	* After the layers have been drawn for the first time, the legend must be
	* redrawn (otherwise it does not appear). This variable indicates
	* whether the layers are drawn for the first time
	*/
	protected boolean firstDraw = true;
	/**
	* This variable indicates whether the user is allowed to change the active
	* layer. In some specific cases (e.g. Naturdetektive) this may be undesirable.
	* By default, changing of the active layer is allowed.
	*/
	protected boolean allowChangeActiveLayer = true;
	/**
	* If one of the layers has a visualizer that implements the "small multiples"
	* map visualization technique, this variable indicates which of the
	* multiple maps is currently being drawn.
	*/
	protected int currMapN = 0;
	/**
	* Stores the last scaling factor aplied for transformations between real-world
	* and screen coordinates (i.e. the real-world distance represented by one
	* screen pixel).
	*/
	protected float lastPixelValue = 1f;
	/**
	 * This object may show in the legend some common information relevant to
	 * the application as a whole or to several layers
	 */
	protected LegendDrawer genInfoProvider = null;

	public DLayerManager() {
		instanceN = ++nInstances;
	}

	/**
	 * Checks whether the coordinates of the objects in this layer are
	 * geographical, i.e. X is the longitude and Y is the latitude.
	 * This is indicated by the value of the user_unit, which should be
	 * "degree" or "grad".
	 */
	@Override
	public boolean isGeographic() {
		if (coordsAreGeographic >= 0)
			return coordsAreGeographic > 0;
		if (user_unit == null)
			return false;
		String str = user_unit.trim().toLowerCase();
		return str.startsWith("degree") || str.startsWith("grad");
	}

	/**
	 * A layer connected to Open Street Maps
	 */
	protected DOSMLayer bkgOSMLayer = null;
	/**
	 * OSM map tiles may be stored in a local file system.
	 * If there are such files, the following field contains the path
	 * to the tile index file with the extension ".osm"
	 */
	protected String pathOSMTilesIndex = null;

	/**
	 * A layer connected to Google Maps and showing hybrid maps (satellite + roads)
	 */
	protected DOSMLayer bkgGMLayer = null;
	/**
	 * A layer connected to Google Maps and showing terrain maps
	 */
	protected DOSMLayer bkgGMTerrainLayer = null;

	/**
	 * Sets whether the coordinates should be treated as geographic, i.e. X as the longitude and Y as the latitude.
	 * The argument makeOSMLayer specify whether the layer manager should automatically
	 * create an OpenStreetMap layer (in case of geographical coordinates)
	 */
	public void setGeographic(boolean geographic, boolean makeOSMLayer) {
		coordsAreGeographic = (geographic) ? 1 : 0;
		if (layers != null) {
			for (int i = 0; i < layers.size(); i++) {
				getGeoLayer(i).setGeographic(geographic);
			}
		}
		if (makeOSMLayer && geographic && bkgOSMLayer == null) {
			bkgOSMLayer = new DOSMLayer();
			bkgOSMLayer.setMapSource(DOSMLayer.source_OpenStreetMaps);
			bkgOSMLayer.setName("Open Street Maps");
			bkgOSMLayer.setContainerIdentifier("open_street_maps");
			bkgOSMLayer.setEntitySetIdentifier("open_street_maps");
			bkgOSMLayer.setGeographic(true);
			bkgOSMLayer.addPropertyChangeListener(this);
			if (pathOSMTilesIndex != null) {
				bkgOSMLayer.setPathTilesIndex(pathOSMTilesIndex);
			}
			if (layers == null) {
				layers = new Vector(20, 20);
			}
			layers.insertElementAt(bkgOSMLayer, 0);
			RealRectangle rr = getCurrentTerritoryBounds();
			if (rr == null) {
				rr = getWholeTerritoryBounds();
			}
			if (rr != null && rr.ry2 - rr.ry1 > 30) {
				bkgOSMLayer.setLayerDrawn(false);
			}
			notifyPropertyChange("LayerAdded", null, bkgOSMLayer, true, true);
		}
		if (makeOSMLayer && geographic && bkgGMLayer == null) {
			bkgGMTerrainLayer = new DOSMLayer();
			bkgGMTerrainLayer.setMapSource(DOSMLayer.source_GoogleMaps);
			bkgGMTerrainLayer.setMapType("terrain");
			bkgGMTerrainLayer.setName("Google Maps terrain map");
			bkgGMTerrainLayer.setContainerIdentifier("google_maps_terrain");
			bkgGMTerrainLayer.setEntitySetIdentifier("google_maps_terrain");
			bkgGMTerrainLayer.setGeographic(true);
			bkgGMTerrainLayer.addPropertyChangeListener(this);
			layers.insertElementAt(bkgGMTerrainLayer, 1);
			bkgGMTerrainLayer.setLayerDrawn(false);
			notifyPropertyChange("LayerAdded", null, bkgGMTerrainLayer, true, true);

			bkgGMLayer = new DOSMLayer();
			bkgGMLayer.setMapSource(DOSMLayer.source_GoogleMaps);
			bkgGMLayer.setMapType("hybrid");
			bkgGMLayer.setName("Google Maps hybrid map");
			bkgGMLayer.setContainerIdentifier("google_maps");
			bkgGMLayer.setEntitySetIdentifier("google_maps");
			bkgGMLayer.setGeographic(true);
			bkgGMLayer.addPropertyChangeListener(this);
			layers.insertElementAt(bkgGMLayer, 2);
			bkgGMLayer.setLayerDrawn(false);
			notifyPropertyChange("LayerAdded", null, bkgGMLayer, true, true);
		}

	}

	@Override
	public void setGeographic(boolean geographic) {
		setGeographic(geographic, true);
	}

	public int getCoordsAreGeographic() {
		return coordsAreGeographic;
	}

	public void setCoordsAreGeographic(int intValue) {
		if (intValue < 0)
			return;
		setGeographic(intValue == 1);
	}

	/**
	 * Reports whether it has an Google Maps layer
	 */
	public boolean hasGMLayer() {
		return bkgGMLayer != null;
	}

	/**
	 * Returns the Google Maps layer, if exists
	 */
	public DOSMLayer getGMLayer() {
		return bkgGMLayer;
	}

	/**
	 * Reports whether it has an Open Street Map layer
	 */
	public boolean hasOSMLayer() {
		return bkgOSMLayer != null;
	}

	/**
	 * Returns the Open Street Map layer, if exists
	 */
	public DOSMLayer getOSMLayer() {
		return bkgOSMLayer;
	}

	/**
	 * OSM map tiles may be stored in a local file system.
	 * If there are such files, returns the path
	 * to the tile index file with the extension ".osm"
	 */
	public String getPathOSMTilesIndex() {
		return pathOSMTilesIndex;
	}

	/**
	 * OSM map tiles may be stored in a local file system.
	 * If there are such files, sets the path
	 * to the tile index file with the extension ".osm"
	 */
	public void setPathOSMTilesIndex(String pathOSMTilesIndex) {
		this.pathOSMTilesIndex = pathOSMTilesIndex;
		if (bkgOSMLayer != null) {
			bkgOSMLayer.setPathTilesIndex(pathOSMTilesIndex);
		}
	}

	/**
	* Returns the unit in which coordinates are specified
	*/
	@Override
	public String getUserUnit() {
		return user_unit;
	}

	/**
	* Sets the unit in which coordinates are specified
	*/
	@Override
	public void setUserUnit(String unit) {
		user_unit = unit;
	}

	/**
	* Sets whether the user is allowed to change the active layer. In some
	* specific cases (e.g. Naturdetektive) this may be undesirable.
	* By default, changing of the active layer is allowed.
	*/
	public void setAllowChangeActiveLayer(boolean value) {
		allowChangeActiveLayer = value;
	}

	/**
	* When a DGeoLayer is added, the DLayerManager should register itself as
	* a listener of properties change events from this DGeoLayer.
	*/
	public void addGeoLayer(DGeoLayer layer) {
		if (layer == null)
			return;
		layer.reset();
		if (layers == null) {
			layers = new Vector(20, 10);
		}
		if (layers.contains(layer))
			return;
		int n = getLayerCount();
		layer.show_nobjects = show_nobjects;
		layer.setUserFactor(user_factor);
		layer.setGeographic(isGeographic());
		layers.addElement(layer);
		layer.addPropertyChangeListener(this);
		ignoreDrag = true;
		if (!(layer instanceof DSpatialWindowLayer)) {
			DSpatialWindowLayer spwLayer = getSpatialWindowLayer();
			if (spwLayer != null) {
				addSpatialFilter(layer, spwLayer.spWin);
			}
		}
		notifyPropertyChange("LayerAdded", null, layer, true, true);
	}

	/**
	 * Makes a copy of this LayerManager (without copying the ObjectManager attached).
	 * Copies all layers, except for the layer with Open Street Map tiles, to the
	 * new LayerManager.
	 */
	@Override
	public LayerManager makeCopy() {
		return makeCopy(true);
	}

	/**
	* Makes a copy of this LayerManager (without copying the ObjectManager attached)
	*/
	public LayerManager makeCopy(boolean copyLayers) {
		DLayerManager lman = new DLayerManager();
		lman.terrName = terrName;
		lman.user_factor = user_factor;
		lman.setUserUnit(user_unit);
		lman.setGeographic(isGeographic(), (bkgOSMLayer != null || bkgGMLayer != null) && bkgOSMLayer.getLayerDrawn());
//ID
		lman.show_legend = show_legend;
		lman.percent_of_legend = percent_of_legend;
		lman.show_terrname = show_terrname;
		lman.show_bgcolor = show_bgcolor;
		lman.show_scale = show_scale;
		lman.show_nobjects = show_nobjects;
//~ID
		lman.percent_of_manipulator = percent_of_manipulator;

		if (copyLayers) {
			for (int i = 0; i < getLayerCount(); i++) {
				DGeoLayer layer = getLayer(i);
				if (layer == null) {
					continue;
				}
				if (layer instanceof DOSMLayer) {
					continue;
				}
				lman.addGeoLayer((DGeoLayer) layer.makeCopy());
			}
			lman.checkAndCorrectLinksBetweenLayers();
			lman.activateLayer(activeIdx);
		}
		return lman;
	}

	/**
	 * Some of the layers may keep references to other layers.
	 * After making a copy, the links must be corrected so that
	 * copies of layers refer to copies of other layers but not
	 * to the original layers from the previous map.
	 */
	public void checkAndCorrectLinksBetweenLayers() {
		if (layers == null || layers.size() < 1)
			return;
		for (int i = 0; i < layers.size(); i++)
			if (layers.elementAt(i) instanceof LinkedToMapLayers) {
				((LinkedToMapLayers) layers.elementAt(i)).checkAndCorrectLinks(layers);
			}
	}

	/**
	* When a DGeoLayer is removed, the DLayerManager should remove itself
	* from the list of listeners of the layer's properties changes.
	*/
	public void removeGeoLayer(DGeoLayer l) {
		if (l == null || layers == null)
			return;
		int idx = layers.indexOf(l);
		if (idx < 0)
			return;
		removeGeoLayer(idx);
	}

	/**
	* Removes the layer with the specified index
	*/
	@Override
	public void removeGeoLayer(int idx) {
		if (layers == null || idx < 0 || idx >= layers.size())
			return;
		int n = getLayerCount();
		if (activeIdx == idx) {
			activateLayer(-1);
		} else if (activeIdx > idx) {
			--activeIdx;
		}
		DGeoLayer l = (DGeoLayer) layers.elementAt(idx);
		l.removePropertyChangeListener(this);
		l.destroy();
		layers.removeElementAt(idx);
		if (l.equals(bkgOSMLayer)) {
			bkgOSMLayer = null;
		}
		if (l.equals(bkgGMLayer)) {
			bkgGMLayer = null;
		}
		if (l.equals(bkgGMTerrainLayer)) {
			bkgGMTerrainLayer = null;
		}
		notifyPropertyChange("LayerRemoved", l, null, true, true);
	}

	public void replaceGeoLayer(DGeoLayer oldLayer, DGeoLayer newLayer) {
		if (layers == null || oldLayer == null || newLayer == null || oldLayer.equals(newLayer))
			return;
		int idx = layers.indexOf(oldLayer);
		if (idx < 0)
			return;
		replaceGeoLayer(idx, newLayer);
	}

	public void replaceGeoLayer(int idx, DGeoLayer layer) {
		if (layer == null || layers == null || idx < 0 || idx >= layers.size())
			return;
		DGeoLayer l = (DGeoLayer) layers.elementAt(idx);
		l.removePropertyChangeListener(this);
		l.destroy();
		layers.setElementAt(layer, idx);
		if (activeIdx == idx) {
			layer.setIsActive(true);
			if (objMan != null) {
				objMan.setGeoLayer(layer);
			}
		}
		layer.show_nobjects = show_nobjects;
		layer.setUserFactor(user_factor);
		layer.addPropertyChangeListener(this);
		notifyPropertyChange("LayerAdded", null, layer, true, true);
	}

	public Vector getLayers() {
		return layers;
	}

	@Override
	public int getLayerCount() {
		if (layers == null)
			return 0;
		return layers.size();
	}

	public DGeoLayer getLayer(int idx) {
		return (DGeoLayer) getGeoLayer(idx);
	}

	@Override
	public GeoLayer getGeoLayer(int idx) {
		if (idx < 0 || idx >= getLayerCount())
			return null;
		return (GeoLayer) layers.elementAt(idx);
	}

	@Override
	public GeoLayer getActiveLayer() {
		return getGeoLayer(activeIdx);
	}

	/**
	* Returns the bounding rectangle including currently loaded GeoObjects of all
	* the GeoLayers.
	*/
	@Override
	public RealRectangle getCurrentTerritoryBounds() {
		if (firstDraw && initialExtent != null)
			return new RealRectangle(initialExtent);
		if (getLayerCount() < 1)
			return null;
		RealRectangle rr = null;
		for (int i = 0; i < getLayerCount(); i++) {
			DGeoLayer layer = getLayer(i);
			if (layer.getLayerDrawn()) {
				RealRectangle r = layer.getCurrentLayerBounds();
				if (r == null) {
					r = layer.getWholeLayerBounds();
				}
				if (r == null) {
					continue;
				}
				if (rr == null) {
					rr = (RealRectangle) r.clone();
				} else {
					if (rr.rx1 > r.rx1) {
						rr.rx1 = r.rx1;
					}
					if (rr.ry1 > r.ry1) {
						rr.ry1 = r.ry1;
					}
					if (rr.rx2 < r.rx2) {
						rr.rx2 = r.rx2;
					}
					if (rr.ry2 < r.ry2) {
						rr.ry2 = r.ry2;
					}
				}
			}
		}
		return rr;
	}

	/**
	* Returns the bounding rectangle including all existing GeoObjects of all
	* the GeoLayers.
	*/
	@Override
	public RealRectangle getWholeTerritoryBounds() {
		RealRectangle fext = null;
		if (fullExtent != null) {
			fext = new RealRectangle(fullExtent);
		}
		if (getLayerCount() < 1)
			return fext;
		RealRectangle rr = null;
		for (int i = 0; i < getLayerCount(); i++) {
			DGeoLayer layer = getLayer(i);
			if (layer.getLayerDrawn() && layer.getObjectCount() > 0) {
				RealRectangle r = layer.getWholeLayerBounds();
				if (r == null)
					if (!layer.getHasAllObjects())
						return fext;
					else {
						continue;
					}
				if (rr == null) {
					rr = (RealRectangle) r.clone();
				} else {
					if (rr.rx1 > r.rx1) {
						rr.rx1 = r.rx1;
					}
					if (rr.ry1 > r.ry1) {
						rr.ry1 = r.ry1;
					}
					if (rr.rx2 < r.rx2) {
						rr.rx2 = r.rx2;
					}
					if (rr.ry2 < r.ry2) {
						rr.ry2 = r.ry2;
					}
				}
			}
		}
		if (fullExtent == null && rr != null) {
			fullExtent = new float[4];
			fullExtent[0] = rr.rx1;
			fullExtent[1] = rr.ry1;
			fullExtent[2] = rr.rx2;
			fullExtent[3] = rr.ry2;
		}
		return rr;
	}

	/**
	* Specifies the order in that the GeoLayers are drawn. The order is
	* specified by the parameter "order". This is an array of integer
	* number. Each number specifies the index of the DGeoLayer in the current
	* list of layers that should stand at the corresponding position
	* in the new list. For example, the array [2,0,1] means that the 3rd DGeoLayer
	* of the current list should now come first, the first DGeoLayer becomes the
	* second, and the second moves to the end of the list.
	*/
	public void setLayerOrder(int order[]) {
		if (order == null || order.length != layers.size())
			return;
		Vector v = new Vector(20, 10);
		synchronized (layers) {
			for (int element : order) {
				v.addElement(layers.elementAt(element));
			}
			layers.removeAllElements();
			layers = v;
		}
		notifyPropertyChange("LayerOrder", null, layers, true, true);
	}

	/**
	* This function is fired when properties of some of the GeoLayers change.
	* The DLayerManager should notify its listeners that properties have
	* changed (in particular, to fire map redrawing).
	*/
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		String prName = evt.getPropertyName();
		if (prName.equals("error") || prName.equals("status") || prName.equals("failure")) {
			notifyPropertyChange(prName, evt.getOldValue(), evt.getNewValue(), false, false);
			return;
		}
		if ((evt.getSource() instanceof DGeoLayer) && layers != null && layers.contains(evt.getSource())) {
			if (prName.equals("activation_request")) {
				if (layers == null)
					return;
				int idx = layers.indexOf(evt.getSource());
				if (idx >= 0) {
					activateLayer(idx);
				}
				return;
			}
			if (prName.equals("ConditionalLayerSwitchedOn")) {
				warnAboutConditionalDrawing((DGeoLayer) evt.getSource());
				return;
			}
			if (prName.equals("ThematicDataRemoved")) {
				notifyPropertyChange("ThematicDataRemoved", null, evt.getSource(), true, true);
				return;
			}
//ID
			if (prName.equals("Visualization")) {
				notifyPropertyChange("Visualization", null, evt.getSource(), true, true);
				return;
			}
//~ID
			boolean contentChange = prName.equals("LayerDrawn") || prName.equals("ObjectData") || prName.equals("ObjectSet");
			boolean affectsMap = contentChange || prName.equals("LabelsDrawn") || prName.equals("Labels") || prName.equals("DrawingParameters") || prName.equals("ImageState") || prName.equals("ObjectFilter") || prName.equals("Visualization")
					|| prName.equals("VisParameters");
			boolean affectsLegend = contentChange || prName.equals("Name") || prName.equals("DrawingParameters") || prName.equals("Visualization") || prName.equals("VisParameters") || prName.equals("ObjectFilter") || prName.equals("LegendContent");
			boolean surfaceChange = !contentChange && affectsMap && (prName.equals("LabelsDrawn") || prName.equals("Labels"));
			if (!contentChange && affectsMap && !surfaceChange)
				if (prName.equals("ObjectData") || prName.equals("ObjectFilter")) {
					DGeoLayer gl = (DGeoLayer) evt.getSource();
					if (gl.getBackgroundVisualizer() == null && gl.getVisualizer() != null) {
						surfaceChange = (gl.getVisualizer()).isDiagramPresentation();
					}
				} else if (prName.equals("VisParameters") && evt.getNewValue() != null && (evt.getNewValue() instanceof Visualizer)) {
					Visualizer vis = (Visualizer) evt.getNewValue();
					surfaceChange = vis.isDiagramPresentation();
				}
			if (contentChange) {
				notifyPropertyChange("content", null, null, affectsMap, affectsLegend);
			} else if (surfaceChange) {
				notifyPropertyChange("foreground", null, null, affectsMap, affectsLegend);
			} else {
				notifyPropertyChange("LayerProperty", null, null, affectsMap, affectsLegend);
			}
		}
		if (prName.equals("LayerDrawn")) {
			notifyPropertyChange("LayerDrawn", null, evt.getSource(), true, true);
			return;
		}
		if (prName.equals("current_interval")) {
			if (layers == null)
				return;
			//check if there is a layer with moving objects
			boolean hasMovingObjects = false;
			for (int i = 0; i < layers.size() && !hasMovingObjects; i++)
				if (layers.elementAt(i) instanceof DGeoLayer) {
					DGeoLayer gl = (DGeoLayer) layers.elementAt(i);
					hasMovingObjects = gl.getHasMovingObjects();
				}
			if (hasMovingObjects) {
				notifyPropertyChange("LayerProperty", null, null, true, genInfoProvider != null);
			} else if (genInfoProvider != null) {
				notifyPropertyChange("legend", null, null, false, true);
			}
		}
	}

	/**
	* The method of the Mappable interface.
	* If one of the layers has a visualizer that implements the "small multiples"
	* map visualization technique, this method returns the number of the
	* individual maps (got from the visualizer). By default, returns 0.
	*/
	@Override
	public int getMapCount() {
		if (getLayerCount() < 1)
			return 1;
		int count = 1;
		for (int i = 0; i < getLayerCount(); i++) {
			DGeoLayer gl = getLayer(i);
			if (gl == null || !gl.getLayerDrawn()) {
				continue;
			}
			for (int j = 0; j < 2; j++) {
				Visualizer vis = (j == 0) ? gl.getVisualizer() : gl.getBackgroundVisualizer();
				if (vis != null && vis.isEnabled() && (vis instanceof MultiMapVisualizer)) {
					MultiMapVisualizer mvis = (MultiMapVisualizer) vis;
					int k = mvis.getNIndMaps();
					if (k > count) {
						count = k;
					}
				}
			}
		}
		return count;
	}

	/**
	* Sets the internal variable currMapN indicateing which of the multiple maps
	* must be drawn next (for the implementation of the "small multiples"
	* visualization technique).
	*/
	@Override
	public void setCurrentMapN(int mapN) {
		currMapN = mapN;
	}

	/**
	* Returns the name of the map with the given index
	*/
	@Override
	public String getMapName(int mapN) {
		if (getLayerCount() < 1)
			return null;
		String name = null;
		for (int i = 0; i < getLayerCount(); i++) {
			DGeoLayer gl = getLayer(i);
			if (gl == null || !gl.getLayerDrawn()) {
				continue;
			}
			for (int j = 0; j < 2; j++) {
				Visualizer vis = (j == 0) ? gl.getVisualizer() : gl.getBackgroundVisualizer();
				if (vis != null && vis.isEnabled() && (vis instanceof MultiMapVisualizer)) {
					MultiMapVisualizer mvis = (MultiMapVisualizer) vis;
					if (mapN < mvis.getNIndMaps()) {
						String str = mvis.getIndMapName(mapN);
						if (str != null)
							if (name != null) {
								name += "; " + str;
							} else {
								name = str;
							}
					}
				}
			}
		}
		return name;
	}

	/**
	 * A DLayerManager may be able to load geo data dynamically at the time of drawing,
	 * depending on the current scale and the visible territory extent.
	 * It may be desirable to suppress the dynamic loading for quick drawing of a map,
	 * when there is no possibility to wait until new data are loaded.
	 * This variable allows or suppresses this ability. By default, dynamic loading is allowed.
	 */
	protected boolean dynamicLoadingAllowed = true;

	/**
	 * A DLayerManager may be able to load geo data dynamically at the time of drawing,
	 * depending on the current scale and the visible territory extent.
	 * This method allows or suppresses this ability. It may be desirable to suppress
	 * the dynamic loading for quick drawing of a map, when there is no possibility
	 * to wait until new data are loaded.
	 */
	@Override
	public void allowDynamicLoadingWhenDrawn(boolean allow) {
		dynamicLoadingAllowed = allow;
	}

	protected boolean mustDrawDiagrams = false, mustDrawLabels = false;

	/**
	* The method of the Mappable interface.
	* Drawing of GeoLayers in the map. The layers are drawn in the order
	* they appear in the list of layers.  If some layers contain diagrams and/or
	* labels, they are not drawn in this method but must be drawn using the
	* method "drawForeground".
	*/
	@Override
	public void drawBackground(Graphics g, MapContext mc) {
		mustDrawDiagrams = false;
		mustDrawLabels = false;
		lastPixelValue = mc.getPixelValue();
		for (int i = 0; i < getLayerCount(); i++) {
			DGeoLayer gl = getLayer(i);
			if (gl == null || !gl.getLayerDrawn()) {
				continue;
			}
			if (!gl.getLayerDrawCondition(lastPixelValue)) {
				continue;
			}
			gl.allowDynamicLoadingWhenDrawn(dynamicLoadingAllowed);
			mustDrawDiagrams = mustDrawDiagrams || gl.getHasDiagrams();
			mustDrawLabels = mustDrawLabels || gl.getDrawingParameters().drawLabels;
			if (gl.getHasDiagrams() && gl.getType() == Geometry.point) {
				continue;
			}
			for (int j = 0; j < 2; j++) {
				Visualizer vis = (j == 0) ? gl.getVisualizer() : gl.getBackgroundVisualizer();
				if (vis != null && vis.isEnabled() && (vis instanceof MultiMapVisualizer)) {
					MultiMapVisualizer mvis = (MultiMapVisualizer) vis;
					mvis.setCurrentMapIndex(currMapN);
				}
			}
			gl.draw(g, mc);
		}
	}

	/**
	* This function is used to speed up map repainting when only diagrams
	* or labels change but not the background. The diagrams or labels are drawn
	* on the top of all since the method "drawForeground" is tipically called
	* after "drawBackground". Labels are drawn after diagrams.
	*/
	@Override
	public void drawForeground(Graphics g, MapContext mc) {
		lastPixelValue = mc.getPixelValue();
		if (mustDrawDiagrams) {
			for (int i = 0; i < getLayerCount(); i++) {
				DGeoLayer gl = getLayer(i);
				if (gl == null || !gl.getLayerDrawn() || !gl.getHasDiagrams()) {
					continue;
				}
				if (!gl.getLayerDrawCondition(lastPixelValue)) {
					continue;
				}
				gl.allowDynamicLoadingWhenDrawn(dynamicLoadingAllowed);
				for (int j = 0; j < 2; j++) {
					Visualizer vis = (j == 0) ? gl.getVisualizer() : gl.getBackgroundVisualizer();
					if (vis != null && vis.isEnabled() && (vis instanceof MultiMapVisualizer)) {
						MultiMapVisualizer mvis = (MultiMapVisualizer) vis;
						mvis.setCurrentMapIndex(currMapN);
					}
				}
				gl.drawDiagrams(g, mc);
			}
		}
		if (mustDrawLabels) {
			for (int i = 0; i < getLayerCount(); i++) {
				DGeoLayer gl = getLayer(i);
				if (gl == null || !gl.getLayerDrawn() || !gl.getHasLabels() || !gl.getDrawingParameters().drawLabels) {
					continue;
				}
				if (!gl.getLayerDrawCondition(lastPixelValue)) {
					continue;
				}
				gl.drawLabels(g, mc);
			}
		}
		if (firstDraw) {
//ID
			// fix for no manipulator for the first loaded raster layer. If it breaks something else - you know who to blame...
			if (layers != null && layers.size() == 1 && (layers.elementAt(0) instanceof DGridLayer)) {
				notifyPropertyChange("LayerAdded", null, layers.elementAt(0), false, true);
			}
//~ID
			notifyPropertyChange("LayersLoaded", null, null, false, true);
			firstDraw = false;
		}
	}

	/**
	* A function from the Mappable interface.
	* Draws highlighted objects of the active layer in the MapCanvas.
	* The idea is that the marked objects
	* are drawn on top of everything else. Besides, the MapCanvas may
	* have a bitmap in the memory where the picture without marking is stored.
	* This can be used for quick change of marking.
	* MapContext provides information necessary for transforming world
	* coordinates into screen coordinates.
	*/
	@Override
	public void drawMarkedObjects(Graphics g, MapContext mc) {
		if (activeIdx >= 0) {
			DGeoLayer layer = getLayer(activeIdx);
			if (layer != null && layer.getLayerDrawn()) {
				layer.drawSelectedObjects(g, mc);
				layer.showHighlighting(g, mc);
			}
		}
	}

	/**
	 * Sets a reference to an object that may show in the legend some common
	 * information relevant to the application as a whole or to several layers
	 */
	public void setGeneralInfoProvider(LegendDrawer genInfoProvider) {
		this.genInfoProvider = genInfoProvider;
	}

	/**
	* Calls the "drawLegend" method of each of the layers.
	* The method belongs to the LegendDrawer interface - see the comments there.
	*/
	@Override
	public Rectangle drawLegend(Component c, Graphics g, int startY, int leftmarg, int prefW) {
		if (g == null)
			return new Rectangle(0, startY, 0, 0);
		int y = startY, maxW = 0;
		if (genInfoProvider != null) {
			Rectangle r = genInfoProvider.drawLegend(c, g, startY, leftmarg, prefW);
			if (r != null && r.height > 0) {
				y = r.y + r.height;
				maxW = r.width;
			}
		}
		if (getLayerCount() > 0) {
			if (rectangles == null || rectangles.length < getLayerCount()) {
				rectangles = new Rectangle[getLayerCount()];
			}
			for (int i = getLayerCount() - 1; i >= 0; i--) {
				DGeoLayer gl = getLayer(i);
				rectangles[i] = null;
				if (gl == null) {
					continue;
				}
				try {
					rectangles[i] = gl.drawLegend(c, g, y, leftmarg, prefW);
				} catch (Exception e) {
				}
				if (rectangles[i] != null) {
					y = rectangles[i].y + rectangles[i].height;
					if (rectangles[i].width > maxW) {
						maxW = rectangles[i].width;
					}
				}
			}
		}
		return new Rectangle(0, startY, maxW, y - startY);
	}

	/**
	 * Draws only the legend of the currently visible layers
	 * (in particular, for printing).
	 */
	public Rectangle drawLegendOnlyVisible(Component c, Graphics g, int startY, int leftmarg, int prefW) {
		if (g == null)
			return new Rectangle(0, startY, 0, 0);
		int y = startY, maxW = 0;
		if (genInfoProvider != null) {
			Rectangle r = genInfoProvider.drawLegend(c, g, startY, leftmarg, prefW);
			if (r != null && r.height > 0) {
				y = r.y + r.height;
				maxW = r.width;
			}
		}
		if (getLayerCount() > 0) {
			for (int i = getLayerCount() - 1; i >= 0; i--) {
				DGeoLayer gl = getLayer(i);
				if (gl == null || !gl.getLayerDrawn()) {
					continue;
				}
				Rectangle r = null;
				try {
					r = gl.drawLegend(c, g, y, leftmarg, prefW);
				} catch (Exception e) {
				}
				if (r != null) {
					y = r.y + r.height;
					if (r.width > maxW) {
						maxW = r.width;
					}
				}
			}
		}
		return new Rectangle(0, startY, maxW, y - startY);
	}

	/**
	* Sets the ObjectManager to be used to support highlighting of objects of the
	* active GeoLayer.
	*/
	@Override
	public void setObjectManager(ObjectManager manager) {
		objMan = manager;
		if (objMan != null && activeIdx >= 0) {
			objMan.setGeoLayer(getLayer(activeIdx));
		}
	}

	/**
	* Returns the ObjectManager used to support highlighting of objects of the
	* active GeoLayer.
	*/
	public ObjectManager getObjectManager() {
		return objMan;
	}

	/**
	* Returns the index of the layer with the given identifier or -1 if there is
	* no such layer
	*/
	@Override
	public int getIndexOfLayer(String layerId) {
		if (layerId == null)
			return -1;
		for (int i = 0; i < getLayerCount(); i++)
			if (layerId.equals(getGeoLayer(i).getContainerIdentifier()))
				return i;
		return -1;
	}

	/**
	* Returns the index of the currently active layer
	*/
	@Override
	public int getIndexOfActiveLayer() {
		return activeIdx;
	}

	/**
	* Makes the layer with the given identifier currently active
	*/
	@Override
	public void activateLayer(String layerId) {
		activateLayer(getIndexOfLayer(layerId));
	}

	/**
	* Changes the active layer to the one with the given index.
	*/
	@Override
	public void activateLayer(int idx) {
		if (idx < 0 || idx >= getLayerCount())
			return;
		if (activeIdx != idx) {
			DGeoLayer oldAL = null;
			if (activeIdx >= 0) {
				oldAL = getLayer(activeIdx);
				if (oldAL != null) {
					oldAL.setIsActive(false);
				}
			}
			DGeoLayer layer = null;
			if (idx >= 0) {
				layer = getLayer(idx);
				if (allowChangeActiveLayer) {
					layer.setIsActive(true);
				}
			}
			activeIdx = idx;
			notifyPropertyChange("ActiveLayer", oldAL, layer, false, true);
			if (objMan != null) {
				objMan.setGeoLayer(layer);
			}
		}
	}

	/**
	* A LegendDrawer may somehow react on mouse events in the legend area
	* A DLayerManager changes layer order upon mouse drag.
	*/
	@Override
	public boolean doesListenToEvent(String eventId) {
		return eventId.equals(DMouseEvent.mDrag);
	}

	/**
	* This method is used for delivering events to the Event Receiver.
	*/
	@Override
	public void eventOccurred(DEvent evt) {
		if ((evt instanceof DMouseEvent) && (evt.getSource() instanceof Legend)) {
			DMouseEvent mevt = (DMouseEvent) evt;
			if (mevt.getId().equals(DMouseEvent.mDrag)) {
				mouseDraggedInLegend((Legend) mevt.getSource(), mevt.getDragStartX(), mevt.getDragStartY(), mevt.getX(), mevt.getY(), mevt.getDragPrevX(), mevt.getDragPrevY(), mevt.isDraggingFinished());
			}
		}
	}

	private int pos = -100; //before what item to place the layer being moved
	private boolean ignoreDrag = false;

	public void mouseDraggedInLegend(Legend legend, int x0, int y0, //mouse position at the moment when dragging started
			int currX, int currY, //current mouse position
			int prevX, int prevY, //previous mouse position - used to clear previously drawn lines etc.
			boolean dragFinished) //shows whether dragging is continuing or finished
	{
		if (ignoreDrag) {
			ignoreDrag = !dragFinished;
			return;
		}
		int startl = -1, nlayers = getLayerCount(); //what layer has been moved?
		for (int i = 0; i < nlayers && startl < 0; i++)
			if (rectangles[i] != null)
				if (rectangles[i].contains(x0, y0)) {
					startl = i;
				}
		if (startl < 0)
			return;
		//after what layer the current mouse position is?
		int newPos = -1;
		for (int i = nlayers - 1; i >= 0 && newPos < 0; i--)
			if (rectangles[i] != null)
				if (currY <= rectangles[i].y) {
					newPos = i;
				}
		if (!dragFinished && pos == newPos)
			return;
		//draw the arrow showing the position where the layer will be inserted
		Graphics g = legend.getLegendGraphics();
		g.setXORMode(Color.lightGray);
		g.setColor(Color.magenta);
		int ntimes = (dragFinished) ? 1 : 2;
		for (int i = 0; i < ntimes; i++) {
			if (pos != -100) { //erase the old arrow
				int y = (pos >= 0) ? rectangles[pos].y : rectangles[0].y + rectangles[0].height;
				g.drawLine(0, y, 20, y);
				g.drawLine(20, y, 15, y - 3);
				g.drawLine(20, y, 15, y + 3);
			}
			pos = newPos;
		}
		g.setPaintMode();
		g.dispose();
		if (!dragFinished)
			return;
		pos = -100;
		if (newPos == startl || newPos == startl - 1)
			return;
		Object layer = layers.elementAt(startl);
		++newPos;
		layers.insertElementAt(layer, newPos);
		if (startl >= newPos) {
			++startl;
		}
		layers.removeElementAt(startl);

		activeIdx = -1;
		for (int i = 0; i < nlayers && activeIdx < 0; i++)
			if (getLayer(i).getIsActive()) {
				activeIdx = i;
			}

		notifyPropertyChange("LayerOrder", null, layers, true, true);
	}

	/**
	* This is a method from the Mappable interface.
	* Update may be called upon zooming/shifting operations with the map.
	* The DLayerManager should run the "update" method of each of the layers.
	*/
	@Override
	public void update(MapContext mc) {
		if (activeIdx >= 0) {
			GeoLayer layer = getLayer(activeIdx);
			if (layer != null) {
				layer.dehighlightAllObjects();
			}
		}
		if (objMan != null) {
			objMan.clearHighlight();
		}
		for (int i = 0; i < getLayerCount(); i++) {
			DGeoLayer gl = getLayer(i);
			if (gl != null) {
				gl.update(mc);
			}
		}
	}

	/**
	* This is a method from the Mappable interface.
	* Makes necessary operations for destroying, specifically, unregisters from
	* listening properties change events of the layers and makes the object
	* maager unregister from listening to highlighting and other events.
	*/
	@Override
	public void destroy() {
		if (objMan != null) {
			objMan.destroy();
		}
		for (int i = 0; i < getLayerCount(); i++) {
			getLayer(i).removePropertyChangeListener(this);
			getLayer(i).destroy();
		}
	}

	/**
	* Returns a unique identifier of the component
	* (used only internally, not shown to the user).
	* The identifier is used for explicit linking of producers and recipients of
	* object events.
	*/
	@Override
	public String getIdentifier() {
		return "DLayerManager_" + instanceN;
	}

	/**
	* Displays a warning when the user switches on a layer which is not shown
	* at the current map scale.
	*/
	protected void warnAboutConditionalDrawing(DGeoLayer layer) {
		if (layer.getLayerDrawCondition(lastPixelValue))
			return;
		OKDialog dlg = new OKDialog(CManager.getAnyFrame(), res.getString("Attention"), false, false);
		TextCanvas tc = new TextCanvas();
		tc.setText(res.getString("layer_conditional_draw"));
		dlg.addContent(tc);
		dlg.show();
	}

	/**
	 * Replies about the presence of a special map layer with a window for spatial filtering
	 */
	public boolean hasSpatialWindow() {
		if (layers == null)
			return false;
		for (int i = 0; i < layers.size(); i++)
			if (layers.elementAt(i) instanceof DSpatialWindowLayer)
				return true;
		return false;
	}

	/**
	 * Finds among its layers a special map layer with a window for spatial filtering
	 */
	public DSpatialWindowLayer getSpatialWindowLayer() {
		if (layers == null)
			return null;
		for (int i = 0; i < layers.size(); i++)
			if (layers.elementAt(i) instanceof DSpatialWindowLayer)
				return (DSpatialWindowLayer) layers.elementAt(i);
		return null;
	}

	/**
	 * returns spatial extent of the filter, if it exists
	 */
	public RealRectangle getSpatialWindowExtent() {
		DSpatialWindowLayer layer = getSpatialWindowLayer();
		if (layer != null && layer.getLayerDrawn())
			return layer.getSpatialWindowExtent();
		else
			return null;
	}

	/**
	 * Removes the special map layer with a window for spatial filtering
	 */
	public void removeSpatialWindow() {
		if (layers == null)
			return;
		int idx = -1;
		for (int i = 0; i < layers.size() && idx < 0; i++)
			if (layers.elementAt(i) instanceof DSpatialWindowLayer) {
				idx = i;
			}
		if (idx < 0)
			return;
		removeSpatialFilterInAllLayers();
		removeGeoLayer(idx);
	}

	/**
	 * Makes a special map layer with a window for spatial filtering
	 */
	public void addSpatialWindow(SpatialWindow spWin) {
		if (spWin == null)
			return;
		DSpatialWindowLayer spwLayer = new DSpatialWindowLayer();
		spwLayer.setSpatialWindow(spWin);
		spwLayer.setContainerIdentifier("spatial_window");
		spwLayer.setEntitySetIdentifier("spatial_window");
		spwLayer.setName("Spatial window (filter)");
		addGeoLayer(spwLayer);
		activateLayer("spatial_window");
		setSpatialFilterInAllLayers(spWin);
	}

	protected void setSpatialFilterInAllLayers(SpatialWindow spWin) {
		if (layers == null)
			return;
		for (int i = 0; i < layers.size(); i++)
			if (layers.elementAt(i) instanceof DGeoLayer) {
				addSpatialFilter((DGeoLayer) layers.elementAt(i), spWin);
			}
	}

	protected void addSpatialFilter(DGeoLayer layer, SpatialWindow spWin) {
		if (layer == null || spWin == null)
			return;
		DrawingParameters dp = layer.getDrawingParameters();
		if (!dp.allowSpatialFilter)
			return;
		if ((layer instanceof DSpatialWindowLayer) || (layer.getType() == Geometry.image || layer.getType() == Geometry.raster)) {
			dp.allowSpatialFilter = false;
			return;
		}
		SpatialFilter spf = new SpatialFilter();
		spf.setSpatialConstraintChecker(spWin);
		spf.setObjectContainer(layer);
		spf.setEntitySetIdentifier(layer.getEntitySetIdentifier());
		layer.setObjectFilter(spf);
	}

	protected void removeSpatialFilterInAllLayers() {
		if (layers == null)
			return;
		for (int i = 0; i < layers.size(); i++)
			if (layers.elementAt(i) instanceof DGeoLayer) {
				removeSpatialFilter((DGeoLayer) layers.elementAt(i));
			}
	}

	protected void removeSpatialFilter(DGeoLayer layer) {
		if (layer == null || (layer instanceof DSpatialWindowLayer) || layer.getType() == Geometry.image || layer.getType() == Geometry.raster)
			return;
		ObjectFilter oFilter = layer.getObjectFilter();
		if (oFilter == null)
			return;
		DrawingParameters dp = layer.getDrawingParameters();
		if (!dp.allowSpatialFilter)
			return;
		if (oFilter instanceof SpatialFilter) {
			layer.removeObjectFilter(oFilter);
			return;
		}
		if (oFilter instanceof CombinedFilter) {
			CombinedFilter cFilter = (CombinedFilter) oFilter;
			for (int i = 0; i < cFilter.getFilterCount(); i++) {
				ObjectFilter filter = cFilter.getFilter(i);
				if (filter instanceof SpatialFilter) {
					layer.removeObjectFilter(filter);
					return;
				}
			}
		}
	}

}
