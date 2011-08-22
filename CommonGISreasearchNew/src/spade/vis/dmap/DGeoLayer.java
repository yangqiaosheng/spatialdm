package spade.vis.dmap;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.ImageObserver;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Hashtable;
import java.util.ResourceBundle;
import java.util.Vector;
import java2d.Drawing2D;

import spade.lib.basicwin.CManager;
import spade.lib.basicwin.Destroyable;
import spade.lib.basicwin.Drawing;
import spade.lib.basicwin.HotSpot;
import spade.lib.basicwin.Icons;
import spade.lib.basicwin.Metrics;
import spade.lib.basicwin.StringInRectangle;
import spade.lib.lang.Language;
import spade.lib.util.GeoDistance;
import spade.lib.util.IntArray;
import spade.lib.util.ObjectWithMeasure;
import spade.lib.util.StringUtil;
import spade.time.TimeMoment;
import spade.time.TimeReference;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.CombinedFilter;
import spade.vis.database.DataItem;
import spade.vis.database.DataPortion;
import spade.vis.database.DataRecord;
import spade.vis.database.DataSupplier;
import spade.vis.database.DataTable;
import spade.vis.database.DataTreater;
import spade.vis.database.ObjectContainer;
import spade.vis.database.ObjectFilter;
import spade.vis.database.SpatialDataItem;
import spade.vis.database.SpatialDataPortion;
import spade.vis.database.SpatialEntity;
import spade.vis.database.ThematicDataItem;
import spade.vis.database.TimeFilter;
import spade.vis.geometry.GeoComp;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.MultiGeometry;
import spade.vis.geometry.RealCircle;
import spade.vis.geometry.RealLine;
import spade.vis.geometry.RealPoint;
import spade.vis.geometry.RealPolygon;
import spade.vis.geometry.RealRectangle;
import spade.vis.map.LegendDrawer;
import spade.vis.map.MapContext;
import spade.vis.mapvis.DefaultColorUser;
import spade.vis.mapvis.Visualizer;
import spade.vis.space.GeoLayer;
import spade.vis.space.GeoObject;
import core.ActionDescr;

/**
* Basically, a GeoLayer is a collection of GeoObjects. A GeoLayer can draw
* itself in a map. To do this, it invokes the drawing method of each of
* its GeoObjects. A GeoLayer keeps all common information for the GeoObjects,
* such as drawing parameters or a reference to the Visualizer used to
* represent thematic data.
* DGeoObject and DGeoLayer are classes of Descartes that implement the basic
* interfaces GeoLayer and GeoObject, respectively.
*/

public class DGeoLayer implements GeoLayer, ObjectContainer, LegendDrawer, ParamListener, ImageObserver, PropertyChangeListener, ActionListener, Destroyable, java.io.Serializable {
	static ResourceBundle res = Language.getTextResource("spade.vis.dmap.Res");

	protected int iconH = 0, switchWidth = 0, iconW = 0;
//----------------- notification about properties changes---------------
	/**
	* A GeoLayer may have a number of listeners of its properties changes.
	* To handle the list of listeners and notify them about changes of the
	* properties (e.g. color of drawing or parameters of the Visualizer),
	* a GeoLayer uses a PropertyChangeSupport.
	*/
	protected PropertyChangeSupport pcSupport = null;

	/**
	* A method from the basic GeoLayer interface.
	*/
	@Override
	public synchronized void addPropertyChangeListener(PropertyChangeListener l) {
		if (l == null)
			return;
		if (pcSupport == null) {
			pcSupport = new PropertyChangeSupport(this);
		}
		pcSupport.addPropertyChangeListener(l);
	}

	/**
	* A method from the basic GeoLayer interface.
	*/
	@Override
	public synchronized void removePropertyChangeListener(PropertyChangeListener l) {
		if (l == null || pcSupport == null)
			return;
		pcSupport.removePropertyChangeListener(l);
	}

	public void notifyPropertyChange(String propName, Object oldValue, Object newValue) {
		if (pcSupport == null)
			return;
		pcSupport.firePropertyChange(propName, oldValue, newValue);
	}

//----------------- internal data members ------------------------
	/**
	 * A generic name of the entities in the container
	 */
	protected String genName = null;
	/**
	* The name of the layer (that can be shown to a user)
	*/
	public String name = null;
	/**
	* This variable keeps a specification of the data source, i.e. all
	* information necessary for loading the layer from a file or database.
	* Normally this is an instance of spade.vis.spec.DataSourceSpec.
	*/
	protected Object source = null;
	/**
	 * If the layer has been produced by means of some analysis operation,
	 * this is a description of the operation
	 */
	protected ActionDescr madeByAction = null;
	/**
	* The identifier of the layer (that is not shown to a user, but can be
	* used for linking tables to layers) In particular, name of the file with
	* the coordinates may be used as the identifier.
	*/
	protected String id = null;
	/**
	* The GeoObjects belonging to this GeoLayer
	*/
	protected Vector geoObj = null;
	/**
	* In a map there may be several GeoLayers. Each has an identifier
	* of the set of geographic entities it includes.
	* The identifiers help in linking thematic and geographic data.
	* A table and a layer referring to the same set of entities will have the
	* same identifier of the entity set.
	*/
	protected String setId = null;
	/**
	* Shows whether all objects of the GeoLayer have been loaded in the memory.
	* Depending on the implementation, either all objects are permanently
	* present in the memory or only the objects currently visible in the
	* map viewport. In the second case after each operation of zooming or
	* shifting the map the GeoLayer should address its DataSupplier for the new
	* set of objects fitting in the current bounding rectangle.
	*/
	protected boolean hasAllObjects = false;
	/**
	 * Shows whether too many objects have been loaded in memory and
	*  Depending on the implementation, either all objects are permanently
	*  present in the memory or only the objects currently visible in the
	*  map viewport. In the second case after each operation of zooming or
	*  shifting the map the GeoLayer should address its DataSupplier for the new
	*  set of objects fitting in the current bounding rectangle.
	*/
	protected boolean hasTooManyObjects = false;
	/**
	 * Indicates whether the coordinates of the objects are geographical,
	 * i.e. X is the longitude and Y is the latitude. By default false.
	 */
	protected boolean isGeographic = false;
	/**
	* Indicates whether the geographical objects in this layer are
	* time-referenced.
	*/
	protected boolean isTimeReferenced = false;
	/**
	 * If the objects are time-referenced, these are the earliest and latest
	 * times among all time references
	 */
	protected TimeMoment firstTime = null, lastTime = null;
	/**
	* The bounding rectangle  of the currently loaded objects of the Layer
	*/
	protected RealRectangle currBounds = null;
//ID
	/**
	* The bounding rectangle  of the currently loaded and not filtered objects of the Layer
	*/
	protected RealRectangle activeBounds = null;
//~ID
	/**
	* The bounding rectangle  of the all the objects comprising the Layer.
	* This rectangle may be inavailable.
	*/
	protected RealRectangle absBounds = null;
	/**
	* DataSupplier provides data necessary for construction of GeoObjects
	*/
	protected DataSupplier dataSuppl = null;
	/**
	* Indicated whether the layer has labels
	*/
	protected boolean hasLabels = false;
	/**
	* Settings concerning layer drawing.
	*/
	protected DrawingParameters drawParm = new DrawingParameters();
	/**
	* Stores the last scaling factor aplied for transformations between real-world
	* and screen coordinates (i.e. the real-world distance represented by one
	* screen pixel).
	*/
	protected float lastPixelValue = 1f;
	/**
	* User-defined scaling factor; must be set by the layer manager
	*/
	protected float user_factor = 1.0f;
	/**
	* The font used for drawing labels
	*/
	protected Font labelFont = null;
	/**
	* Indicates whether the layer is active. All operations (properties editing,
	* visualisation, highlighting) are applied to the active layer.
	*/
	protected boolean isActive = false;
	/**
	* Visualizer specifies how thematic information associated with GeoObjects
	* should be presented. Visualizer may be absent. In this case the layer
	* will not present any thematic information, even when it is available.
	*/
	protected Visualizer vis = null;
	/**
	* For area layers, two visualizers are allowed: one defining the area filling
	* colors and the other producing diagrams or other symbols drawn on top of
	* the painting. This variable contains a reference to the visualizer
	* which will define the colors for painting.
	*/
	protected Visualizer bkgVis = null;
	/**
	* Type of the objects in this GeoLayer. May be undefined.
	*/
	protected char objType = Geometry.undefined;
	/**
	 * Subtype of the objects. There are some special subtypes: circle, rectangle,
	 * vector, link, ... If not any of these subtypes, then Geometry.undefined.
	 */
	protected char objSubType = Geometry.undefined;
	/**
	* Object index: for search optimisation
	*/
	protected Hashtable objIndex = null;
	/**
	 * source layer used to produce this layer
	 * For example, used for linking events to polygons they are related to 
	 */
	protected DGeoLayer sourceLayer = null;

	public DGeoLayer getSourceLayer() {
		return sourceLayer;
	}

	public void setSourceLayer(DGeoLayer sourceLayer) {
		this.sourceLayer = sourceLayer;
	}

	/**
	 * Indicates whether the layer has moving objects
	 */
	protected boolean hasMovingObjects = false;
	/**
	* The filter of objects (may be, in particular, a TableFilter, a TimeFilter,
	* or a CombinedFilter). The objects that are filtered out are not drawn on
	* the map. The DGeoLayer listens to changes of the set of active objects.
	*/
	protected ObjectFilter oFilter = null;
	/**
	* The AttributeDataPortion (table) linked to this GeoLayer
	*/
	protected AttributeDataPortion dTable = null;
	/**
	* Indicates whetehr the objects of this GeoLayer have been actually linked to
	* the corresponding table rows (the presence of a reference to the table does
	* not necessarily imply this)
	*/
	protected boolean linkedToTable = false;
	/**
	 * Used to transmit the filter of geographical objects to the table
	 */
	protected LayerFilter layerFilter = null;
	/**
	* Indicates "destroyed" state. Initially is false.
	*/
	protected boolean destroyed = false;
//ID
	/**
	* Indicates if total number of objects is displayed in legend.
	*/
	public boolean show_nobjects = true;
//~ID
	/**
	* "Hot spots" in the legend. Clicking on each of them results in a particular
	* action: switching on/off the layer, editing the layer's visual appearance,
	* editing the visualization settings, etc.
	*/
	protected HotSpot spots[] = null;

	/**
	 * has holes in polygons for area layer type (default false)
	 *
	 */
	public boolean hasHoles = false;
	/**
	 * Specifies the order in which the objects of the layer should be drawn.
	 */
	protected int order[] = null;
	/**
	 * A layer may have an associated table with metadata about the whole layer
	 * (not with data about every object).
	 */
	protected AttributeDataPortion metaData = null;

	public void removeHotSpotsInLegend() {
		if (spots != null) {
			for (HotSpot spot : spots)
				if (spot != null) {
					spot.destroy();
				}
		}
		spots = null;
	}

	public void reset() {
		removeHotSpotsInLegend();
		order = null;
		isActive = false;
		destroyed = false;
	}

	/**
	 * Copies its internal structures and settings to the given
	 * DGeoLayer
	 */
	public void copyTo(DGeoLayer layer) {
		if (layer == null)
			return;
		if (name != null) {
			layer.name = new String(name);
		}
		if (id != null) {
			layer.setContainerIdentifier(new String(id));
		}
		if (setId != null) {
			layer.setEntitySetIdentifier(new String(setId));
		}
		layer.setGenericNameOfEntity(genName);
		layer.hasAllObjects = hasAllObjects;
		layer.hasLabels = hasLabels;
		layer.setGeographic(isGeographic());
		if (getObjectCount() > 0) {
			Vector gobj = new Vector(getObjectCount(), 100);
			for (int i = 0; i < getObjectCount(); i++) {
				gobj.addElement(getObject(i).makeCopy());
			}
			layer.setGeoObjects(gobj, hasAllObjects);
		}
		layer.setDataSupplier(dataSuppl);
		layer.setDrawingParameters(drawParm.makeCopy());
		layer.setIsActive(isActive);
		layer.setType(objType);
		layer.setDataTable(dTable);
		layer.setLinkedToTable(linkedToTable);
		layer.setMetaData(metaData);
		layer.setObjectFilter(oFilter);
		layer.lastPixelValue = lastPixelValue;
		if (vis != null) {
			layer.setVisualizer(vis);
		}
		if (bkgVis != null) {
			layer.setBackgroundVisualizer(bkgVis);
		}
	}

	/**
	* Returns a copy of this GeoLayer. The reference to the visualizer is also copied.
	*/
	@Override
	public GeoLayer makeCopy() {
		DGeoLayer layer = new DGeoLayer();
		copyTo(layer);
		return layer;
	}

	/**
	* Returns the name of the layer (that can be shown to the user
	*/
	@Override
	public String getName() {
		return name;
	}

	/**
	* Sets the name of the layer
	*/
	@Override
	public void setName(String name) {
		this.name = name;
		notifyPropertyChange("Name", null, name);
	}

	/**
	 * Sets a generic name of the entities in the container
	 */
	@Override
	public void setGenericNameOfEntity(String name) {
		genName = name;
	}

	/**
	 * Returns the generic name of the entities in the container.
	 * May return null, if the name was not previously set.
	 */
	@Override
	public String getGenericNameOfEntity() {
		return genName;
	}

	/**
	* Returns the specification of the data source, i.e. all
	* information necessary for loading the data from a file or database.
	* Normally this is an instance of spade.vis.spec.DataSourceSpec.
	*/
	@Override
	public Object getDataSource() {
		return source;
	}

	/**
	* Stores the specification of the data source
	*/
	public void setDataSource(Object sourceSpec) {
		source = sourceSpec;
	}

	/**
	* A method from the EntitySetContainer interface. Returns the unique
	* identifier of the layer.
	* Each layer in a map should have its unique identifier. The identifier
	* can be used, for example, to specify with what layer a table should be linked.
	*/
	@Override
	public String getContainerIdentifier() {
		return id;
	}

	public void setContainerIdentifier(String ident) {
		id = ident;
	}

	/**
	* A method from the EntitySetContainer interface.
	* In a map there may be several GeoLayers. Each has an identifier
	* of the set of geographic entities it includes.
	* The identifiers help in linking thematic and geographic data.
	* A table and a layer referring to the same set of entities will have the
	* same identifier of the entity set.
	*/
	@Override
	public String getEntitySetIdentifier() {
		return setId;
	}

	/**
	* A method from the EntitySetContainer interface.
	* Sets the identifier of the set of geographic objects referred to by this
	* layer.
	*/
	@Override
	public void setEntitySetIdentifier(String id) {
		setId = id;
	}

	/**
	* Used by the layer manager to inform the layer about the user-defined scaling
	* factor. This is needed for correct checking whether the layer must be drawn
	* at the current scale.
	*/
	public void setUserFactor(float factor) {
		user_factor = factor;
	}

	/**
	* A GeoLayer can be linked to a filter of objects. The objects that are
	* filtered out are not drawn on the map. The GeoLayer should be a
	* PropertyChangeListener and listen to changes of the set of active objects.
	*/
	@Override
	public void setObjectFilter(ObjectFilter oFilter) {
		addFilter(oFilter);
	}

	/**
	* Removes its filter
	*/
	public void removeFilter() {
		if (oFilter != null) {
			oFilter.removePropertyChangeListener(this);
			oFilter = null;
			notifyPropertyChange("ObjectFilter", oFilter, null);
		}
		removeLayerFilter();
	}

	/**
	* Removes the particular filter, possibly, from a combination of filters
	*/
	public void removeObjectFilter(ObjectFilter filter) {
		if (filter == null || oFilter == null)
			return;
		if (oFilter.equals(filter)) {
			removeFilter();
			return;
		}
		if (!(oFilter instanceof CombinedFilter))
			return;
		CombinedFilter cFilter = (CombinedFilter) oFilter;
		cFilter.removeFilter(filter);
		if (cFilter.getFilterCount() < 1) {
			removeFilter();
		} else {
			updateLayerFilter();
			notifyPropertyChange("ObjectFilter", filter, null);
		}
	}

	/**
	* Removes only its table filter and preserves all other filters, if any
	*/
	public void removeThematicFilter() {
		if (oFilter == null || !oFilter.isAttributeFilter())
			return;
		if (oFilter instanceof CombinedFilter) {
			CombinedFilter cFilter = (CombinedFilter) oFilter;
			ObjectFilter tFilter = null;
			for (int i = cFilter.getFilterCount() - 1; i >= 0; i--)
				if (cFilter.getFilter(i).getObjectContainer() instanceof AttributeDataPortion) {
					tFilter = cFilter.getFilter(i);
					cFilter.removeFilter(i);
				}
			if (tFilter != null) {
				notifyPropertyChange("ObjectFilter", tFilter, null);
			}
		} else {
			removeFilter();
		}
	}

	/**
	* A DGeoLayer can be linked to a table with thematic data, one table at a time.
	* Simultaneously, it is linked to the TableFilter of this table. If there is
	* some other object filter associated with this layer (but not a TableFilter),
	* this filter is combined with the TableFilter. If the layer had earlier
	* another TableFilter, the old TableFilter is removed.
	* A TableFilter is distinguished from other filters by returning true
	* from its method isAttributeFilter().
	*/
	public void setThematicFilter(ObjectFilter tFilter) {
		if (tFilter == null || !tFilter.isAttributeFilter() || !tFilter.isRelevantTo(setId))
			return;
		if (tFilter instanceof LayerFilter)
			return;
		if (tFilter.equals(oFilter))
			return;
		ObjectFilter tf1 = getThematicFilter(), tf2 = getThematicFilter(tFilter);
		if (tf1 != null && !tf2.equals(tf1)) {
			removeThematicFilter();
		}
		//System.out.println("setThematicFilter called for layer "+getName());
		if (tFilter instanceof CombinedFilter) {
			CombinedFilter cFilter = (CombinedFilter) tFilter;
			for (int i = 0; i < cFilter.getFilterCount(); i++)
				if (!(cFilter.getFilter(i) instanceof LayerFilter)) {
					addFilter(cFilter.getFilter(i));
				}
		} else {
			addFilter(tFilter);
		}
	}

	/**
	* Combines a new filter with the previously existing filter by creating a
	* CombinedFilter.
	*/
	protected void addFilter(ObjectFilter newFilter) {
		if (newFilter == null || !newFilter.isRelevantTo(setId))
			return;
		if (newFilter.equals(oFilter))
			return;
		if (newFilter.getObjectContainer() == null) {
			newFilter.setObjectContainer(this);
		}
		newFilter.setEntitySetIdentifier(setId);
		if (oFilter == null) {
			oFilter = newFilter;
			oFilter.addPropertyChangeListener(this);
		} else {
			CombinedFilter cFilter = null;
			if (oFilter instanceof CombinedFilter) {
				cFilter = (CombinedFilter) oFilter;
			} else {
				cFilter = new CombinedFilter();
				cFilter.setEntitySetIdentifier(setId);
				cFilter.setObjectContainer(this);
				cFilter.addPropertyChangeListener(this);
				oFilter.removePropertyChangeListener(this);
				cFilter.addFilter(oFilter);
				oFilter = cFilter;
			}
			if (!(newFilter instanceof CombinedFilter)) {
				cFilter.addFilter(newFilter);
			} else {
				CombinedFilter newCFilter = (CombinedFilter) newFilter;
				for (int i = 0; i < newCFilter.getFilterCount(); i++) {
					ObjectFilter filter = newCFilter.getFilter(i);
					if (cFilter.hasFilter(filter)) {
						continue;
					}
					if (filter instanceof TimeFilter) {
						//check if the layer already has a time filter
						TimeFilter timeFilter = null;
						int tfIdx = -1;
						for (int j = 0; j < cFilter.getFilterCount() && timeFilter == null; j++)
							if (cFilter.getFilter(j) instanceof TimeFilter) {
								timeFilter = (TimeFilter) cFilter.getFilter(j);
								tfIdx = j;
							}
						if (timeFilter == null) {
							cFilter.addFilter(filter);
						} else if (!timeFilter.getObjectContainer().equals(this)) {
							cFilter.removeFilter(tfIdx);
							cFilter.addFilter(filter);
						}
					} else {
						cFilter.addFilter(filter);
					}
				}
			}
		}
		notifyPropertyChange("ObjectFilter", null, oFilter);
		checkCreateLayerFilter();
	}

	/**
	* Returns its object filter (@see setObjectFilter)
	*/
	public ObjectFilter getObjectFilter() {
		return oFilter;
	}

	/**
	* Returns its table filter (@see setThematicFilter)
	*/
	public ObjectFilter getThematicFilter() {
		return getThematicFilter(oFilter);
	}

	/**
	* Retrieves the thematic filter from the given filter (possibly, combined)
	*/
	public ObjectFilter getThematicFilter(ObjectFilter filter) {
		if (filter == null)
			return null;
		if (filter instanceof CombinedFilter) {
			CombinedFilter cFilter = (CombinedFilter) filter;
			for (int i = cFilter.getFilterCount() - 1; i >= 0; i--)
				if (cFilter.getFilter(i).isAttributeFilter())
					return cFilter.getFilter(i);
		} else if (filter.isAttributeFilter())
			return filter;
		return null;
	}

	/**
	* Returns its time filter, if available
	*/
	public TimeFilter getTimeFilter() {
		if (oFilter == null)
			return null;
		if (oFilter instanceof TimeFilter)
			return (TimeFilter) oFilter;
		if (oFilter instanceof CombinedFilter) {
			CombinedFilter cFilter = (CombinedFilter) oFilter;
			for (int i = cFilter.getFilterCount() - 1; i >= 0; i--)
				if (cFilter.getFilter(i) instanceof TimeFilter)
					return (TimeFilter) cFilter.getFilter(i);
		}
		return null;
	}

	/**
	* Removes itself from listeners of the ObjectFilter and the table
	*/
	public void destroy() {
		if (destroyed)
			return;
		if (dTable != null) {
			dTable.removePropertyChangeListener(this);
			removeLayerFilter();
		}
		if (oFilter != null) {
			oFilter.removePropertyChangeListener(this);
		}
		if (vis != null) {
			vis.removeVisChangeListener(this);
			vis.destroy();
		}
		if (bkgVis != null) {
			bkgVis.removeVisChangeListener(this);
			bkgVis.destroy();
		}
		removeHotSpotsInLegend();
		destroyed = true;
		notifyPropertyChange("destroyed", null, null);
	}

	/**
	* Replies whether is destroyed or not
	*/
	public boolean isDestroyed() {
		return destroyed;
	}

	/**
	* This method counts how many of the GeoObjects comprising this GeoLayer
	* are found in the given DataPortion
	*/
	public int countOverlap(DataPortion dp) {
		if (dp == null)
			return 0;
		if (geoObj == null)
			return 0;
		int n = 0;
		for (int i = 0; i < getObjectCount(); i++) {
			String oid = ((DGeoObject) geoObj.elementAt(i)).getIdentifier();
			if (oid != null && dp.indexOf(oid) >= 0) {
				++n;
			}
		}
		return n;
	}

	/**
	* This method is used to pass to the layer source thematic data for
	* visualisation on a map. Returns the number of thematic data items
	* successfully linked to corresponding geographical objects.
	*/
	public int receiveThematicData(AttributeDataPortion dp) {
		//System.out.println(name+" receives thematic data; dTable="+dTable+
		//  ", new table="+dp);
		linkedToTable = false;
		if (dp == null)
			return 0;
		if (geoObj == null) {
			loadGeoObjects();
			if (geoObj == null)
				return 0;
		}
		if (dTable != null && dTable != dp) {
			dTable.removePropertyChangeListener(this);
			removeLayerFilter();
		}
		if (dp != null && dTable != dp) {
			dp.addPropertyChangeListener(this);
		}
		dTable = dp;
		int nlinked = linkToThematicData();
		notifyPropertyChange("ObjectData", null, null);
		checkCreateLayerFilter();
		return nlinked;
	}

	/**
	 * Creates a layer filter that will transmit object filtering to the table
	 * attached to the layer.
	 */
	protected void checkCreateLayerFilter() {
		if (layerFilter != null) {
			updateLayerFilter();
			return;
		}
		if (dTable == null || !(dTable instanceof ObjectContainer))
			return;
		if (this.oFilter == null)
			return;
		layerFilter = new LayerFilter(this);
		layerFilter.setObjectContainer((ObjectContainer) dTable);
		((ObjectContainer) dTable).setObjectFilter(layerFilter);
	}

	protected void removeLayerFilter() {
		if (layerFilter != null && dTable != null && (dTable instanceof ObjectContainer)) {
			((ObjectContainer) dTable).removeObjectFilter(layerFilter);
		}
		layerFilter = null;
	}

	protected void updateLayerFilter() {
		if (layerFilter != null) {
			layerFilter.getFiltersToListen();
		}
	}

	/**
	 * A utility method for retrieving a temporal reference from a table record
	 * @param dit - the thematic data item suposedly containing one or more
	 *              temporal attributes
	 * @param occurTimeFN - the number of the column with the occurrence time (for
	 *                      instant events)
	 * @param validFromFN - the number of the column with the time when the object
	 *                      appears (for durable events or other objects that may
	 *                      be created and removed)
	 * @param validUntilFN - the number of the column with the time when the object
	 *                       disappears (for durable events or other objects that may
	 *                       be created and removed)
	 * @return  the time reference retrieved from the data item
	 */
	protected TimeReference getTimeReference(ThematicDataItem dit, int occurTimeFN, int validFromFN, int validUntilFN) {
		if (dit == null)
			return null;
		TimeMoment validFrom = null, validUntil = null;
		if (occurTimeFN >= 0) {
			Object value = dit.getAttrValue(occurTimeFN);
			if (value != null && (value instanceof TimeMoment)) {
				validFrom = (TimeMoment) value;
				validUntil = validFrom;
			}
		} else if (validFromFN >= 0) {
			Object value = dit.getAttrValue(validFromFN);
			if (value != null && (value instanceof TimeMoment)) {
				validFrom = (TimeMoment) value;
			}
			if (validUntilFN >= 0) {
				value = dit.getAttrValue(validUntilFN);
				if (value != null && (value instanceof TimeMoment)) {
					validUntil = (TimeMoment) value;
				}
			}
		}
		if (validFrom != null) {
			TimeReference tref = new TimeReference();
			tref.setValidFrom(validFrom);
			tref.setValidUntil(validUntil);
			return tref;
		}
		return null;
	}

	/**
	* Tries to get temporal references from its table and links them to the
	* appropriate geo objects
	*/
	public boolean tryGetTemporalReferences() {
		if (isTimeReferenced)
			return true;
		firstTime = lastTime = null;
		if (dTable == null || !dTable.hasData() || !dTable.hasTimeReferences())
			return false;
		int nObj = getObjectCount();
		if (nObj < 1)
			return false;
		int occurTimeFN = -1, validFromFN = -1, validUntilFN = -1;
		IntArray timeRefColNs = dTable.getTimeRefColumnNs();
		if (timeRefColNs != null) {
			for (int i = timeRefColNs.size() - 1; i >= 0; i--) {
				Attribute at = dTable.getAttribute(timeRefColNs.elementAt(i));
				switch (at.timeRefMeaning) {
				case Attribute.OCCURRED_AT:
					occurTimeFN = timeRefColNs.elementAt(i);
					break;
				case Attribute.VALID_FROM:
					validFromFN = timeRefColNs.elementAt(i);
					break;
				case Attribute.VALID_UNTIL:
					validUntilFN = timeRefColNs.elementAt(i);
					break;
				}
			}
		}
		int nTimeReferenced = 0;
		for (int i = 0; i < nObj; i++) {
			DGeoObject gObj = (DGeoObject) geoObj.elementAt(i);
			SpatialDataItem spa = gObj.getSpatialData();
			if (spa == null) {
				continue;
			}
			ThematicDataItem dit = spa.getThematicData();
			if (dit == null) {
				continue;
			}
			TimeReference tref = getTimeReference(dit, occurTimeFN, validFromFN, validUntilFN);
			if (tref != null) {
				spa.setTimeReference(tref);
				++nTimeReferenced;
			}
		}
		isTimeReferenced = nTimeReferenced >= nObj / 2 + 1;
		return isTimeReferenced;
	}

	/**
	 * If the objects in this layer are time-referenced, returns the earliest
	 * and the latest times among the time references; otherwise returns null.
	 */
	public TimeReference getTimeSpan() {
		if (firstTime == null || lastTime == null) {
			firstTime = lastTime = null;
			if (!isTimeReferenced)
				return null;
			int nObj = getObjectCount();
			if (nObj < 1)
				return null;
			for (int i = 0; i < nObj; i++) {
				DGeoObject gObj = (DGeoObject) geoObj.elementAt(i);
				TimeReference tref = gObj.getTimeReference();
				if (tref == null) {
					continue;
				}
				TimeMoment t1 = tref.getValidFrom(), t2 = tref.getValidUntil();
				if (t1 != null) {
					if (firstTime == null || firstTime.compareTo(t1) > 0) {
						firstTime = t1;
					}
					if (lastTime == null || lastTime.compareTo(t1) < 0) {
						lastTime = t1;
					}
				}
				if (t2 != null) {
					if (lastTime == null || lastTime.compareTo(t2) < 0) {
						lastTime = t2;
					}
				}
			}
			if (firstTime != null) {
				firstTime = firstTime.getCopy();
			}
			if (lastTime != null) {
				lastTime = lastTime.getCopy();
			}
		}
		if (firstTime == null || lastTime == null)
			return null;
		TimeReference tref = new TimeReference();
		tref.setValidFrom(firstTime);
		tref.setValidUntil(lastTime);
		return tref;
	}

	/**
	 * If the objects in this container are time-referenced, returns the earliest
	 * and the latest times among the original time references irrespective of the
	 * current transformation of the times; otherwise returns null.
	 */
	public TimeReference getOriginalTimeSpan() {
		if (!isTimeReferenced)
			return null;
		int nObj = getObjectCount();
		if (nObj < 1)
			return null;
		TimeMoment tFirst = null, tLast = null;
		for (int i = 0; i < nObj; i++) {
			DGeoObject gObj = (DGeoObject) geoObj.elementAt(i);
			TimeReference tref = gObj.getTimeReference();
			if (tref == null) {
				continue;
			}
			TimeMoment t1 = tref.getOrigFrom(), t2 = tref.getOrigUntil();
			if (t1 != null) {
				if (tFirst == null || tFirst.compareTo(t1) > 0) {
					tFirst = t1;
				}
				if (tLast == null || tLast.compareTo(t1) < 0) {
					tLast = t1;
				}
			}
			if (t2 != null) {
				if (tLast == null || tLast.compareTo(t2) < 0) {
					tLast = t2;
				}
			}
		}
		if (tFirst == null || tLast == null)
			return null;
		tFirst = tFirst.getCopy();
		tLast = tLast.getCopy();
		TimeReference tref = new TimeReference();
		tref.setValidFrom(tFirst);
		tref.setValidUntil(tLast);
		return tref;
	}

	/**
	* Gets thematic data from its table and links them to the appropriate geo objects
	*/
	protected int linkToThematicData() {
		linkedToTable = false;
		if (dTable == null)
			return 0;
		if (!dTable.hasData() && !dTable.loadData())
			return 0;
		System.out.println("Layer " + getName() + " is being linked to table " + dTable.getName());
		int nlinked = 0, nTRec = dTable.getDataItemCount(), nObj = getObjectCount();
		hasLabels = false;
		long t = System.currentTimeMillis();
		int occurTimeFN = -1, validFromFN = -1, validUntilFN = -1;
		boolean getTimeReferences = false;
		if (!isTimeReferenced && dTable.hasTimeReferences()) {
			IntArray timeRefColNs = dTable.getTimeRefColumnNs();
			if (timeRefColNs != null) {
				for (int i = timeRefColNs.size() - 1; i >= 0; i--) {
					Attribute at = dTable.getAttribute(timeRefColNs.elementAt(i));
					switch (at.timeRefMeaning) {
					case Attribute.OCCURRED_AT:
						occurTimeFN = timeRefColNs.elementAt(i);
						break;
					case Attribute.VALID_FROM:
						validFromFN = timeRefColNs.elementAt(i);
						break;
					case Attribute.VALID_UNTIL:
						validUntilFN = timeRefColNs.elementAt(i);
						break;
					}
				}
			}
			getTimeReferences = occurTimeFN >= 0 || validFromFN >= 0 || validUntilFN >= 0;
			firstTime = lastTime = null;
		}
		//initially we assume that the order of the objects is the same in the table
		//and in the layer
		int nTimeReferenced = 0;
		for (int i = 0; i < nTRec && i < nObj; i++) {
			ThematicDataItem dit = (ThematicDataItem) dTable.getDataItem(i);
			if (dit == null) {
				break;
			}
			DGeoObject gObj = (DGeoObject) geoObj.elementAt(i);
			if (!gObj.getIdentifier().equalsIgnoreCase(dit.getId())) {
				break;
			}
			gObj.setThematicData(dit);
			//System.out.println("Layer "+name+": object "+gObj.getIdentifier()+" has been linked to thematic data");
			++nlinked;
			if (dit.hasName() && (gObj.getLabel() == null || gObj.getLabel().equals(gObj.getIdentifier()))) {
				gObj.setLabel(dit.getName());
			}
			//System.out.println("Set thematic data for "+gObj.getIdentifier()+" name="+dit.getName()+
			//                   " label="+gObj.getLabel());
			hasLabels = hasLabels || gObj.getLabel() != null;
			if (getTimeReferences) {
				TimeReference tref = getTimeReference(dit, occurTimeFN, validFromFN, validUntilFN);
				if (tref != null) {
					gObj.getSpatialData().setTimeReference(tref);
					++nTimeReferenced;
				}
			}
		}
		if (nlinked < nTRec && nlinked < nObj) { //the order was not the same
			System.out.println("!--> Inconsistent object order in layer " + getName() + " and table " + dTable.getName() + "!");
			System.out.println("!--> The linking procedure may be slow!");
			//erase old thematic data
			for (int i = nlinked; i < nObj; i++) {
				getObject(i).setThematicData(null);
			}
			if (dTable.getUsesObjectIndex()) {
				for (int i = nlinked; i < nObj; i++) {
					DGeoObject gObj = (DGeoObject) geoObj.elementAt(i);
					int idx = dTable.indexOf(gObj.getIdentifier());
					if (idx >= 0 && dTable.getDataItem(idx) != null) {
						ThematicDataItem dit = (ThematicDataItem) dTable.getDataItem(idx);
						gObj.setThematicData(dit);
						++nlinked;
						if (dit.hasName() && (gObj.getLabel() == null || gObj.getLabel().equals(gObj.getIdentifier()))) {
							gObj.setLabel(dit.getName());
						}
						hasLabels = hasLabels || gObj.getLabel() != null;
						if (getTimeReferences) {
							TimeReference tref = getTimeReference(dit, occurTimeFN, validFromFN, validUntilFN);
							if (tref != null) {
								gObj.getSpatialData().setTimeReference(tref);
								++nTimeReferenced;
							}
						}
					}
				}
			} else {
				for (int i = nlinked; i < nTRec; i++) {
					ThematicDataItem dit = (ThematicDataItem) dTable.getDataItem(i);
					if (dit == null) {
						continue;
					}
					DGeoObject gObj = (DGeoObject) findObjectById(dit.getId());
					if (gObj != null) {
						gObj.setThematicData(dit);
						//System.out.println("Layer "+name+": object "+gObj.getIdentifier()+" has been linked to thematic data");
						++nlinked;
						if (dit.hasName() && (gObj.getLabel() == null || gObj.getLabel().equals(gObj.getIdentifier()))) {
							gObj.setLabel(dit.getName());
						}
						//System.out.println("Set thematic data for "+gObj.getIdentifier()+" name="+dit.getName()+
						//                   " label="+gObj.getLabel());
						hasLabels = hasLabels || gObj.getLabel() != null;
						if (getTimeReferences) {
							TimeReference tref = getTimeReference(dit, occurTimeFN, validFromFN, validUntilFN);
							if (tref != null) {
								gObj.getSpatialData().setTimeReference(tref);
								++nTimeReferenced;
							}
						}
					}
				}
			}
		}
		linkedToTable = nlinked > 0;
		t = System.currentTimeMillis() - t;
		System.out.println("Layer " + name + ": " + nlinked + " objects were linked to thematic data during " + t + " msec");
		if (nTimeReferenced >= nObj / 2 + 1) {
			isTimeReferenced = true;
			notifyPropertyChange("got_time_references", null, null);
		}
		return nlinked;
	}

	/**
	* This method checks if the layer is currently linked to the given portion of
	* thematic data
	*/
	public boolean hasThematicData(AttributeDataPortion data) {
		if (!linkedToTable || data == null || dTable == null)
			return false;
		return dTable.equals(data);
	}

	/**
	* This method checks if the layer is currently linked to any thematic data
	*/
	public boolean hasThematicData() {
		return dTable != null && dTable.hasData() && linkedToTable;
	}

	/**
	* Returns the reference to the AttributeDataPortion with thematic data
	* the layer is currently linked to
	*/
	public AttributeDataPortion getThematicData() {
		return dTable;
	}

	/**
	* Associates the objects of the layer with arbitrary labels.
	* The Vector passed as an argument should consist of arrays each, in
	* its turn, consisting of two strings. The first string in an array is the
	* identifier of an object, and the second is the name.
	*/
	public void receiveLabels(Vector idnames) {
		if (idnames == null || idnames.size() < 1)
			return;
		for (int i = 0; i < getObjectCount(); i++) {
			DGeoObject obj = getObject(i);
			String id = obj.getIdentifier();
			if (id == null) {
				continue;
			}
			boolean found = false;
			for (int j = 0; j < idnames.size() && !found; j++) {
				try {
					String ss[] = (String[]) idnames.elementAt(j);
					found = id.equalsIgnoreCase(ss[0]);
					if (found) {
						obj.setLabel(ss[1]);
					}
				} catch (ClassCastException cce) {
				}
			}
			if (!found) {
				obj.setLabel(null);
			}
			hasLabels = hasLabels || found;
		}
		if (hasLabels && drawParm.drawLabels) {
			notifyPropertyChange("Labels", null, null);
		}
	}

	public void setDataTable(AttributeDataPortion dp) {
		if (dTable != null && dTable.equals(dp))
			return;
		if (dTable != null) {
			linkedToTable = false;
			dTable.removePropertyChangeListener(this);
			removeLayerFilter();
		}
		if (dp != null) {
			dp.addPropertyChangeListener(this);
		}
		dTable = dp;
		checkCreateLayerFilter();
		//System.out.println("Layer "+name+" set a link to table "+dTable);
	}

	public void setLinkedToTable(boolean value) {
		if (value && dTable == null)
			return;
		linkedToTable = value;
	}

	/**
	* Erases thematic data linked to geographical objects
	*/
	public void eraseThematicData() {
		if (geoObj == null)
			return;
		linkedToTable = false;
		for (int i = 0; i < getObjectCount(); i++) {
			getObject(i).setThematicData(null);
		}
		if (dTable != null) {
			dTable.removePropertyChangeListener(this);
			removeLayerFilter();
		}
		dTable = null;
		if (vis != null) {
			vis.removeVisChangeListener(this);
			vis.destroy();
			vis = null;
		}
		if (bkgVis != null) {
			bkgVis.removeVisChangeListener(this);
			bkgVis.destroy();
			bkgVis = null;
		}
		notifyPropertyChange("ThematicDataRemoved", null, null);
	}

	public void setGeoObjects(Vector objects, boolean all) {
		geoObj = objects;
		objIndex = null;
		hasAllObjects = all;
		hasLabels = false;
		firstTime = lastTime = null;
		if (geoObj != null && geoObj.size() > 0) {
			objIndex = new Hashtable(geoObj.size() + 100);
			int nTimeReferenced = 0;
			for (int i = 0; i < geoObj.size(); i++) {
				DGeoObject gObj = (DGeoObject) geoObj.elementAt(i);
				objIndex.put(gObj.getIdentifier(), new Integer(i));
				gObj.setGeographic(isGeographic);
				hasLabels = hasLabels || gObj.getLabel() != null;
				SpatialDataItem spd = gObj.getSpatialData();
				spd.setIndexInContainer(i);
				if (spd.getTimeReference() != null) {
					++nTimeReferenced;
				}
			}
			isTimeReferenced = nTimeReferenced >= geoObj.size() / 2 + 1;
		}
	}

	/**
	* Adds a new GeoObject to its vector of GeoObjects. Notifies the listeners
	* about the change.
	*/
	public void addGeoObject(DGeoObject obj) {
		addGeoObject(obj, true);
	}

	/**
	 * Adds a new GeoObject to its vector of GeoObjects.
	 * @param notifyListeners - when true, notifies the listeners about the change.
	 */
	public void addGeoObject(DGeoObject obj, boolean notifyListeners) {
		if (obj == null)
			return;
		obj.setGeographic(isGeographic);
		if (geoObj == null) {
			geoObj = new Vector(500, 500);
		}
		geoObj.addElement(obj);
		if (objIndex == null) {
			objIndex = new Hashtable(1000);
		}
		objIndex.put(obj.getIdentifier(), new Integer(geoObj.size() - 1));
		TimeReference tref = obj.getTimeReference();
		if (tref != null) {
			TimeMoment t1 = tref.getValidFrom(), t2 = tref.getValidUntil();
			if (t1 != null) {
				if (firstTime == null || firstTime.compareTo(t1) > 0) {
					firstTime = t1.getCopy();
				}
				if (lastTime == null || lastTime.compareTo(t1) < 0) {
					lastTime = t1.getCopy();
				}
				isTimeReferenced = true;
			}
			if (t2 != null) {
				if (lastTime == null || lastTime.compareTo(t2) < 0) {
					lastTime = t2.getCopy();
				}
			}
		}

		obj.getSpatialData().setIndexInContainer(geoObj.size() - 1);
		hasLabels = hasLabels || obj.getLabel() != null;
		absBounds = currBounds = null;

		if (notifyListeners) {
			notifyPropertyChange("ObjectSet", null, null);
		}
	}

	/**
	* Removes the GeoObject with the given index from its vector of GeoObjects.
	* Notifies the listeners about the change.
	*/
	public void removeGeoObject(int idx) {
		if (geoObj == null || idx < 0 || idx >= geoObj.size())
			return;
		geoObj.removeElementAt(idx);
		objIndex = null;
		for (int i = idx; i < geoObj.size(); i++) {
			DGeoObject obj = (DGeoObject) geoObj.elementAt(i);
			obj.getSpatialData().setIndexInContainer(i);
		}
		absBounds = currBounds = null;
		notifyPropertyChange("ObjectSet", null, null);
	}

	/**
	* Removes all the GeoObjects. Does not notify the listeners about the change!
	*/
	public void removeAllObjects() {
		if (geoObj != null) {
			for (int i = 0; i < geoObj.size(); i++) {
				((DGeoObject) geoObj.elementAt(i)).getSpatialData().setIndexInContainer(-1);
			}
			geoObj.removeAllElements();
		}
		objIndex = null;
	}

	/**
	* Construction of GeoObjects on the basis of a SpatialDataPortion.
	* Returns the number of the objects constructed (updated)
	*/
	public int receiveSpatialData(SpatialDataPortion sdp) {
		if (sdp == null)
			return 0;
		int oldCount = getObjectCount();
		if (geoObj == null) {
			geoObj = new Vector(Math.max(100, sdp.getDataItemCount()), 100);
		} else {
			geoObj.removeAllElements();
		}
		objIndex = new Hashtable(sdp.getDataItemCount() + 100);
		isTimeReferenced = false;
		linkedToTable = false;
		firstTime = lastTime = null;
		absBounds = sdp.getBoundingRectangle();
		boolean linkToTable = dTable != null;// && dTable.getDataItemCount()<=sdp.getDataItemCount();
		IntArray objNoData = new IntArray(100, 100);
		boolean orderConsistent = true;
		for (int i = 0; i < sdp.getDataItemCount(); i++) {
			SpatialDataItem spdit = (SpatialDataItem) sdp.getDataItem(i);
			if (spdit == null) {
				continue;
			}
			/*
			System.out.println("Receive SpatialDataItem: id="+spdit.getId()+
			  ", name="+spdit.getName()+", thematic data "+spdit.getThematicData());
			*/
			String id = spdit.getId();
			DGeoObject gObj = new DGeoObject();
			gObj.setup(spdit);
			gObj.setGeographic(isGeographic);
			geoObj.addElement(gObj);
			objIndex.put(gObj.getIdentifier(), new Integer(geoObj.size() - 1));
			gObj.getSpatialData().setIndexInContainer(geoObj.size() - 1);
			isTimeReferenced = isTimeReferenced || spdit.getTimeReference() != null;
			if (spdit.hasName()) {
				gObj.setLabel(spdit.getName());
				hasLabels = true;
			}
			ThematicDataItem td = spdit.getThematicData();
			if (linkToTable && td == null) {
				if (orderConsistent) {
					td = (ThematicDataItem) dTable.getDataItem(i);
					if (td == null || !td.getId().equalsIgnoreCase(id)) {
						orderConsistent = false;
						td = null;
					}
				}
				if (td == null) {
					int idx = dTable.indexOf(id);
					if (idx >= 0) {
						td = (ThematicDataItem) dTable.getDataItem(idx);
					} else {
						td = null;
					}
				}
			}
			if (td != null) {
				gObj.setThematicData(td);
			} else {
				objNoData.addElement(geoObj.size() - 1);
			}
		}
		hasAllObjects = sdp.hasAllData();
		linkedToTable = objNoData.size() < geoObj.size();
		if (!linkToTable && dTable != null && objNoData.size() > 0) {
			for (int i = 0; i < dTable.getDataItemCount(); i++) {
				String id = dTable.getDataItemId(i);
				if (id == null) {
					continue;
				}
				for (int j = 0; j < objNoData.size(); j++) {
					DGeoObject gObj = (DGeoObject) geoObj.elementAt(objNoData.elementAt(j));
					if (id.equalsIgnoreCase(gObj.getIdentifier())) {
						gObj.setThematicData((ThematicDataItem) dTable.getDataItem(i));
						linkedToTable = true;
						break;
					}
				}
			}
		}
		if (geoObj.size() > 0) {
			tryGetTemporalReferences();
		}
		if (oldCount < 1 && geoObj.size() > 0) {
			notifyPropertyChange("got_data", null, null);
		}

		hasHoles = hasHoles(geoObj);

		System.out.println("hasHoles:" + hasHoles);

		return geoObj.size();
		//System.out.println("The objects successfully constructed!");
	}

	protected boolean hasHoles(Vector geoObj) {
		boolean b = false;
		for (int i = 0; i < geoObj.size(); i++) {
			b = hasHoles(((DGeoObject) geoObj.elementAt(i)).getGeometry());
			if (b) {
				break;
			}
		}
		return b;
	}

	protected boolean hasHoles(Geometry geom) {
		boolean b = false;
		if (geom instanceof RealPolygon) {
			RealPolygon rpg = (RealPolygon) geom;
			if (rpg.pp != null && rpg.pp.size() > 0) {
				b = true;
			}
		} else if (geom instanceof MultiGeometry) {
			MultiGeometry mg = (MultiGeometry) geom;
			for (int i = 0; i < mg.getPartsCount(); i++) {
				b = hasHoles(mg.getPart(i));
				if (b) {
					break;
				}
			}
		}
		return b;
	}

	public boolean getHasAllObjects() {
		return hasAllObjects;
	}

	public void setHasAllObjects(boolean value) {
		hasAllObjects = value;
	}

	public boolean getHasTooManyObjects() {
		return hasTooManyObjects;
	}

	public void setHasTooManyObjects(boolean value) {
		hasTooManyObjects = value;
	}

	/**
	* Reports whether the GeoObjects in this layer are temporally referenced.
	*/
	public boolean hasTimeReferences() {
		return isTimeReferenced;
	}

	/**
	 * Reports whether the GeoObjects in this layer represent entities
	 * changing over time, e.g. moving, growing, shrinking, etc.
	 */
	public boolean containsChangingObjects() {
		if (geoObj == null)
			return false;
		for (int i = 0; i < geoObj.size(); i++)
			if (((DGeoObject) geoObj.elementAt(i)).includesChanges())
				return true;
		return false;
	}

//------------ Functions to access and modify layer's properties ---------
	/**
	* Returns the spatial type of its objects (point, line, area, or raster -
	* the constants are defined in the Geometry class). All
	* GeoObjects belonging to the same GeoLayer should have the same
	* spatial type.
	* If the type of this GeoLayer has not been set yet, the GeoLayer takes
	* the type of its GeoObjects. If the GeoObjects have different types, the
	* priorities are raster>line>area>point.
	*/
	public char getType() {
		if (objType != Geometry.undefined)
			return objType;
		if (geoObj == null)
			return objType;
		for (int i = 0; i < getObjectCount(); i++) {
			DGeoObject gobj = getObject(i);
			if (gobj == null) {
				continue;
			}
			switch (gobj.getSpatialType()) {
			case Geometry.image:
			case Geometry.raster:
				objType = gobj.getSpatialType();
				return objType;
			case Geometry.line:
				objType = Geometry.line;
				return objType;
			case Geometry.area:
				objType = Geometry.area;
				break;
			case Geometry.point:
				if (objType != Geometry.area) {
					objType = Geometry.point;
				}
				break;
			}
		}
		return objType;
	}

	/**
	* Sets the spatial type of the objects comprising this GeoLayer.
	*/
	public void setType(char type) {
		objType = type;
		if (geoObj != null) {
			for (int i = 0; i < getObjectCount(); i++) {
				getObject(i).setSpatialType(type);
			}
		}
	}

	/**
	 * Returns the subtype of the objects, which may be one of the special
	 * subtypes: circle, rectangle, vector, link, ... If not any of these subtypes,
	 * returns Geometry.undefined.
	 */
	public char getSubtype() {
		if (objSubType != Geometry.undefined)
			return objSubType;
		if (geoObj == null)
			return objSubType;
		for (int i = 0; i < getObjectCount(); i++) {
			DGeoObject gobj = getObject(i);
			if (gobj == null) {
				continue;
			}
			if (gobj instanceof DLinkObject) {
				objSubType = Geometry.link;
				return objSubType;
			}
			if (gobj instanceof DMovingObject) {
				objSubType = Geometry.movement;
				return objSubType;
			}
			Geometry geom = gobj.getGeometry();
			if (geom == null) {
				continue;
			}
			if (geom instanceof RealCircle) {
				objSubType = Geometry.circle;
			} else if (geom instanceof RealRectangle) {
				objSubType = Geometry.rectangle;
			} else if (geom instanceof RealLine) {
				objSubType = Geometry.vector;
			}
			break;
		}
		return objSubType;
	}

	/**
	* Returns the rectangle that contains all GeoObjects of this GeoLayer.
	* The GeoLayer may return null if the information about the layer bounds
	* is not available (e.g. not all objects are loaded in the memory, and
	* the database does not provide summary information).
	*/
	public RealRectangle getWholeLayerBounds() {
		if (absBounds != null)
			return absBounds;
		/*
		if (geoObj==null) loadGeoObjects();
		if (absBounds!=null) return absBounds;
		*/
		if (geoObj == null)
			return null;
		if (hasAllObjects) {
			if (currBounds == null) {
				getCurrentLayerBounds();
			}
			if (currBounds != null) {
				absBounds = (RealRectangle) currBounds.clone();
			}
		}
		return absBounds;
	}

	/**
	* Sets the bounding rectangle that contains all GeoObjects of this GeoLayer.
	*/
	public void setWholeLayerBounds(RealRectangle bounds) {
		absBounds = bounds;
	}

	/**
	* Sets the bounding rectangle that contains all GeoObjects of this GeoLayer.
	*/
	public void setWholeLayerBounds(float x1, float y1, float x2, float y2) {
		if (absBounds == null) {
			absBounds = new RealRectangle(x1, y1, x2, y2);
		} else {
			absBounds.rx1 = x1;
			absBounds.ry1 = y1;
			absBounds.rx2 = x2;
			absBounds.ry2 = y2;
		}
	}

	/**
	* Returns the rectangle that contains currently loaded GeoObjects of this GeoLayer.
	* The GeoLayer may return null if there are no objects in the memory.
	*/
	public RealRectangle getCurrentLayerBounds() {
		if (currBounds != null)
			return currBounds;
		if (geoObj == null) {
			loadGeoObjects();
		}
		if (geoObj == null)
			return null;
		for (int i = 0; i < getObjectCount(); i++) {
			DGeoObject gobj = getObject(i);
			if (gobj == null) {
				continue;
			}
			RealRectangle objBounds = gobj.getBounds();
			if (objBounds != null)
				if (currBounds == null) {
					currBounds = (RealRectangle) objBounds.clone();
				} else {
					if (currBounds.rx1 > objBounds.rx1) {
						currBounds.rx1 = objBounds.rx1;
					}
					if (currBounds.ry1 > objBounds.ry1) {
						currBounds.ry1 = objBounds.ry1;
					}
					if (currBounds.rx2 < objBounds.rx2) {
						currBounds.rx2 = objBounds.rx2;
					}
					if (currBounds.ry2 < objBounds.ry2) {
						currBounds.ry2 = objBounds.ry2;
					}
				}
		}
		/*
		System.out.println(name+": "+currBounds.rx1+" "+currBounds.rx2+
		  " "+currBounds.ry1+" "+currBounds.ry2);
		*/
		return currBounds;
	}

	/**
	 * Computes the X- and Y-extents of the layer. If the coordinates are geographic,
	 * computes the distance on the Earth's surface in meters.
	 */
	public static double[] getExtentXY(DGeoLayer layer) {
		if (layer == null)
			return null;
		RealRectangle bounds = layer.getWholeLayerBounds();
		if (bounds == null) {
			bounds = layer.getCurrentLayerBounds();
		}
		return getExtentXY(bounds.rx1, bounds.ry1, bounds.rx2, bounds.ry2, layer.isGeographic());
	}

	/**
	 * Computes the X- and Y-extents of the rectangle with the given coordinates.
	 * If the coordinates are geographic, computes the distance on the Earth's surface in meters.
	 */
	public static double[] getExtentXY(float minX, float minY, float maxX, float maxY, boolean isGeographic) {
		double wh[] = { maxX - minX, maxY - minY };
		if (isGeographic) {
			wh[1] = GeoDistance.geoDist(minX, minY, minX, maxY);
			double y = (minY + maxY) / 2;
			if (maxX - minX > 180) {
				wh[0] = GeoDistance.geoDist(minX, y, 0, y) + GeoDistance.geoDist(0, y, maxX, y);
			} else {
				wh[0] = GeoDistance.geoDist(minX, y, maxX, y);
			}
		}
		return wh;
	}

//ID
	/**
	* Returns the rectangle that contains currently loaded and not filtered GeoObjects of this GeoLayer.
	* The GeoLayer may return null if there are no objects in the memory.
	*/
	public RealRectangle getActiveLayerBounds() {
		if (activeBounds != null)
			return activeBounds;
		if (geoObj == null) {
			loadGeoObjects();
		}
		if (geoObj == null)
			return null;
		for (int i = 0; i < getObjectCount(); i++) {
			DGeoObject gobj = getObject(i);
			if (gobj == null) {
				continue;
			}
			if (!isObjectActive(i)) {
				continue;
			}
			RealRectangle objBounds = gobj.getBounds();
			if (objBounds != null)
				if (activeBounds == null) {
					activeBounds = (RealRectangle) objBounds.clone();
				} else {
					if (activeBounds.rx1 > objBounds.rx1) {
						activeBounds.rx1 = objBounds.rx1;
					}
					if (activeBounds.ry1 > objBounds.ry1) {
						activeBounds.ry1 = objBounds.ry1;
					}
					if (activeBounds.rx2 < objBounds.rx2) {
						activeBounds.rx2 = objBounds.rx2;
					}
					if (activeBounds.ry2 < objBounds.ry2) {
						activeBounds.ry2 = objBounds.ry2;
					}
				}
		}
		return activeBounds;
	}

//~ID
	//
	/**
	* LayerDrawn specifies whether the layer is drawn in the map.
	*/
	public boolean getLayerDrawn() {

		return drawParm.drawLayer;
	}

	public boolean getLayerDrawCondition(float pixelValue) {
		if (!drawParm.drawCondition)
			return true;
		if (Float.isNaN(drawParm.minScaleDC) && Float.isNaN(drawParm.maxScaleDC))
			return true;
		lastPixelValue = pixelValue;
		float sc = lastPixelValue * Metrics.cm() * user_factor;
		//System.out.println("lastPixelValue="+lastPixelValue+" sc1:"+sc+":"+drawParm.minScaleDC+":"+drawParm.maxScaleDC);
		if (Float.isNaN(drawParm.minScaleDC))
			return sc >= drawParm.maxScaleDC;
		if (Float.isNaN(drawParm.maxScaleDC))
			return sc <= drawParm.minScaleDC;
		return sc >= drawParm.maxScaleDC && sc <= drawParm.minScaleDC;
	}

	public void setLayerDrawn(boolean value) {
		if (value == drawParm.drawLayer)
			return;
		Boolean oldV = new Boolean(drawParm.drawLayer);
		drawParm.drawLayer = value;
		notifyPropertyChange("LayerDrawn", oldV, new Boolean(drawParm.drawLayer));
	}

	/**
	* Reports whether the layer is active. All operations (properties editing,
	* visualisation, highlighting) are applied to the active layer.
	*/
	public boolean getIsActive() {
		return isActive;
	}

	public void setIsActive(boolean value) {
		isActive = value;
	}

	//
	/**
	* Typically a GeoLayer has labels when it presents thematic data.
	* Labels are usually object names, but, in principle, may be any texts.
	*/
	public boolean getHasLabels() {
		return hasLabels;
	}

	//
	public DrawingParameters getDrawingParameters() {
		return drawParm;
	}

	public void setDrawingParameters(DrawingParameters dp) {
		if (labelFont != null) {
			labelFont = null;
		}
		if (drawParm == null) {
			drawParm = dp;
		} else {
			dp.copyTo(drawParm);
		}
		if (vis != null && (vis instanceof DefaultColorUser)) {
			((DefaultColorUser) vis).setDefaultLineColor(drawParm.lineColor);
			((DefaultColorUser) vis).setDefaultFillColor(drawParm.fillColor);
		}
		if (bkgVis != null && (bkgVis instanceof DefaultColorUser)) {
			((DefaultColorUser) bkgVis).setDefaultLineColor(drawParm.lineColor);
			((DefaultColorUser) bkgVis).setDefaultFillColor(drawParm.fillColor);
		}
		notifyPropertyChange("DrawingParameters", null, drawParm);
	}

//------------------- Visualization ----------------------------------
	/**
	* A method from the basic GeoLayer interface.
	* Visualizer specifies how thematic information associated with GeoObjects
	* should be presented.
	* When a Visualizer is set, the GeoLayer
	* 1) adds itself to VisChangeListeners of this Visualizer;
	* 2) removes itself from VisChangeListeners of the previous Visualizer;
	* 3) notifies PropertyChangeListeners about change of its properties.
	*/
	public void setVisualizer(Visualizer visualizer) {
		if (vis == visualizer)
			return;
		if (visualizer != null && dTable != null && !dTable.hasData()) {
			dTable.removePropertyChangeListener(this);
			dTable.loadData();
			dTable.addPropertyChangeListener(this);
		}
		Visualizer oldVis = vis;
		vis = visualizer;
		if (oldVis != null && !oldVis.equals(bkgVis)) {
			oldVis.removeVisChangeListener(this);
		}
		if (vis != null) {
			vis.addVisChangeListener(this);
			if (dTable != null) {
				vis.setTableIdentifier(dTable.getContainerIdentifier());
			}
			if ((vis instanceof DefaultColorUser) && (drawParm != null)) {
				((DefaultColorUser) vis).setDefaultLineColor(drawParm.lineColor);
				((DefaultColorUser) vis).setDefaultFillColor(drawParm.fillColor);
			}
		}
		notifyPropertyChange("Visualization", oldVis, vis);
	}

	/**
	* A method from the basic GeoLayer interface.
	*/
	public Visualizer getVisualizer() {
		return vis;
	}

	public boolean hasVisualizer() {
		return vis != null || bkgVis != null;
	}

	/**
	* For area layers, two visualizers are allowed: one defining the area filling
	* colors and the other producing diagrams or other symbols drawn on top of
	* the painting. This method is used for linking a layer to the visualizer
	* which will define the colors for painting.
	*/
	public void setBackgroundVisualizer(Visualizer visualizer) {
		if (bkgVis == visualizer)
			return;
		if (visualizer != null && dTable != null && !dTable.hasData()) {
			dTable.removePropertyChangeListener(this);
			dTable.loadData();
			dTable.addPropertyChangeListener(this);
		}
		Visualizer oldVis = bkgVis;
		bkgVis = visualizer;
		if (oldVis != null) {
			oldVis.removeVisChangeListener(this);
		}
		if (bkgVis != null) {
			bkgVis.addVisChangeListener(this);
			if (dTable != null) {
				bkgVis.setTableIdentifier(dTable.getContainerIdentifier());
			}
			if ((bkgVis instanceof DefaultColorUser) && (drawParm != null)) {
				((DefaultColorUser) bkgVis).setDefaultLineColor(drawParm.lineColor);
				((DefaultColorUser) bkgVis).setDefaultFillColor(drawParm.fillColor);
			}
		}
		notifyPropertyChange("Visualization", oldVis, bkgVis);
	}

	public Visualizer getBackgroundVisualizer() {
		return bkgVis;
	}

	/**
	* The GeoLayer asks its Visualizer (if it exists) whether it produces
	* diagrams. If there is no Visualizer, the method returns false.
	*/
	public boolean getHasDiagrams() {
		if (vis == null)
			return false;
		return vis.isDiagramPresentation();
	}

	/**
	* Reaction to changes of object filter
	*/
	public void propertyChange(PropertyChangeEvent pce) {
		//System.out.println("Layer "+name+" received event from "+pce.getSource()+":\n"+
		//pce.getPropertyName()+" "+pce.getOldValue()+" "+pce.getNewValue());
		if (pce.getPropertyName().equals("Visualization")) {
			notifyPropertyChange("VisParameters", null, pce.getSource());
		} else if (pce.getPropertyName().equals("TrajSegmFilter")) {
			notifyPropertyChange("ObjectFilter", null, pce.getSource());
		} else if (pce.getSource().equals(oFilter)) {
			if (pce.getPropertyName().equals("destroyed")) {
				oFilter.removePropertyChangeListener(this);
				oFilter = null;
				removeLayerFilter();
			}
			if (hasMovingObjects && pce.getPropertyName().equals("current_interval"))
				return; //the layer manager will be notified by another object
			notifyPropertyChange("ObjectFilter", null, null);
		} else if (dTable != null && pce.getSource() == dTable)
			if (pce.getPropertyName().equals("destroyed")) {
				eraseThematicData();
			} else if (pce.getPropertyName().equals("structure_complete")) {
				if (geoObj == null)
					return;
				linkedToTable = false;
				for (int i = 0; i < getObjectCount(); i++) {
					getObject(i).setThematicData(null);
				}
				if (vis != null) {
					vis.removeVisChangeListener(this);
					vis.destroy();
					vis = null;
				}
				if (bkgVis != null) {
					bkgVis.removeVisChangeListener(this);
					bkgVis.destroy();
					bkgVis = null;
				}
				notifyPropertyChange("ThematicDataRemoved", null, null);
				linkToThematicData();
			} else if (pce.getPropertyName().equals("values")) {
				Vector attrs = (Vector) pce.getNewValue();
				if (attrs == null || attrs.size() < 1)
					return;
				if (bkgVis != null && bkgVis.isEnabled() && (bkgVis instanceof DataTreater)) {
					Vector vattrs = ((DataTreater) bkgVis).getAttributeList();
					if (vattrs != null && vattrs.size() > 0) {
						for (int i = 0; i < vattrs.size(); i++) {
							String attrId = (String) vattrs.elementAt(i);
							if (attrId != null && StringUtil.isStringInVectorIgnoreCase(attrId, attrs)) {
								notifyPropertyChange("VisParameters", null, bkgVis);
								return;
							}
						}
					}
				}
				if (vis != null && vis.isEnabled() && (vis instanceof DataTreater)) {
					Vector vattrs = ((DataTreater) vis).getAttributeList();
					if (vattrs != null && vattrs.size() > 0) {
						for (int i = 0; i < vattrs.size(); i++) {
							String attrId = (String) vattrs.elementAt(i);
							if (attrId != null && StringUtil.isStringInVectorIgnoreCase(attrId, attrs)) {
								notifyPropertyChange("VisParameters", null, vis);
								return;
							}
						}
					}
				}
			} else if (pce.getPropertyName().equals("filter")) {
				setThematicFilter(dTable.getObjectFilter());
			}
	}

//-------------- Methods for drawing -----------------------------------

	public boolean areObjectsFiltered() {
		if (oFilter == null)
			return false;
		return oFilter.areObjectsFiltered();
	}

	/**
	* Determines whether the object is active, i.e. not filtered out.
	*/
	public boolean isObjectActive(DGeoObject gobj) {
		if (gobj == null)
			return false;
		if (oFilter == null || !oFilter.areObjectsFiltered())
			return true;
		if (gobj.getIdentifier() == null)
			return false;
		return oFilter.isActive(gobj.getSpatialData());
	}

	/**
	* Determines whether the object with the given index is active, i.e. not
	* filtered out.
	*/
	public boolean isObjectActive(int idx) {
		if (idx < 0 || geoObj == null || idx >= geoObj.size())
			return false;
		if (oFilter == null || !oFilter.areObjectsFiltered())
			return true;
		if (this.equals(oFilter.getObjectContainer()))
			return oFilter.isActive(idx);
		if (dTable != null && dTable.equals(oFilter.getObjectContainer()))
			return oFilter.isActive(((DGeoObject) geoObj.elementAt(idx)).getData());
		return oFilter.isActive(((DGeoObject) geoObj.elementAt(idx)).getSpatialData());
	}

	/**
	 * Sets the order in which the objects in this layer should be drawn.
	 * The array contains the indices of the objects.
	 */
	public void setOrder(int order[]) {
		this.order = order;
	}

	/**
	 * Returns the order in which the objects in this layer are drawn.
	 * The array contains the indices of the objects. May return null.
	 */
	public int[] getOrder() {
		return order;
	}

	/**
	 * The number of active objects (satisfying the filter)
	 */
	protected int nActive = 0;

	/**
	 * A DGeoLayer may be able to load geo data dynamically at the time of drawing,
	 * depending on the current scale and the visible territory extent.
	 * It may be desirable to suppress the dynamic loading for quick drawing of a map,
	 * when there is no possibility to wait until new data are loaded.
	 * This variable allows or suppresses this ability. By default, dynamic loading is allowed.
	 */
	protected boolean dynamicLoadingAllowed = true;

	/**
	 * A DGeoLayer may be able to load geo data dynamically at the time of drawing,
	 * depending on the current scale and the visible territory extent.
	 * This method allows or suppresses this ability. It may be desirable to suppress
	 * the dynamic loading for quick drawing of a map, when there is no possibility
	 * to wait until new data are loaded.
	 */
	public void allowDynamicLoadingWhenDrawn(boolean allow) {
		dynamicLoadingAllowed = allow;
	}

	public void draw(Graphics g, MapContext mc) {
		if (g == null || mc == null)
			return;
		lastPixelValue = mc.getPixelValue();
		if (geoObj == null || !hasAllObjects) {
			loadGeoObjects(mc);
		}
		if (geoObj == null)
			return;
		if ((vis != null || bkgVis != null) && dTable != null && !linkedToTable) {
			linkToThematicData();
		}
		RealRectangle rr = mc.getVisibleTerritory();
		if (getType() == Geometry.line) {
			drawParm.fillContours = false;
		}
		Visualizer v1 = null, v2 = null;
		if (vis != null && vis.isEnabled()) {
			v1 = vis;
		}
		if (bkgVis != null && bkgVis.isEnabled()) {
			v2 = bkgVis;
		}

		TimeFilter tf = getTimeFilter();
		TimeMoment t1 = null, t2 = null;
		boolean timeFiltered = false;
		if (tf != null) {
			t1 = tf.getFilterPeriodStart();
			t2 = tf.getFilterPeriodEnd();
			timeFiltered = t1 != null || t2 != null;
		}
		int nObj = getObjectCount();
		boolean ordered = order != null && order.length > 0;
		if (ordered) {
			nObj = order.length;
		}
		nActive = 0;
		for (int i = 0; i < nObj; i++) {
			int ii = (ordered) ? order[i] : i;
			DGeoObject gobj = getObject(ii);
			/*
			boolean debug=false;
			if ((gobj instanceof DMovingObject) &&
			    (gobj.getIdentifier().equals("193") || gobj.getIdentifier().equals("165") ||
			     gobj.getIdentifier().equals("101") || gobj.getIdentifier().equals("68"))) {
			  System.out.println("Trajectory "+gobj.getIdentifier()+": "+gobj.getTimeReference());
			  debug=true;
			}
			*/
			if (timeFiltered) {
				gobj = (DGeoObject) gobj.getObjectVersionForTimeInterval(t1, t2);
				/*
				if (debug)
				  System.out.println("Version for time interval "+t1+".."+t2+" : "+gobj);
				*/
				if (gobj == null) {
					continue;
				}
			}
			boolean active = oFilter == null || oFilter.equals(tf) || isObjectActive(ii);
			if (active) {
				++nActive;
			}
			/*
			if (debug)
			  System.out.println("active="+active);
			*/
			if ((active || drawContoursOfInactiveObjects()) && gobj.fitsInRectangle(rr.rx1, rr.ry1, rr.rx2, rr.ry2))
				if (active) {
					gobj.setDrawingParameters(drawParm);
					if (objType != Geometry.undefined) {
						gobj.setSpatialType(objType);
					}
					gobj.setVisualizer(v1);
					gobj.setBackgroundVisualizer(v2);
					if (gobj.getSpatialType() == Geometry.image) {
						gobj.setImageObserver(this);
					}
					gobj.draw(g, mc);
				} else {
					g.setColor(Color.gray);
					gobj.drawContour(g, mc);
				}
		}
	}

	/*
	* Function drawStrictly() is like normal draw().
	* It is needed if we try to avoid drawing of geo-objects with area geometry
	* that have their center out of visible territory.
	*/
	public void drawStrictly(Graphics g, MapContext mc) {
		if (g == null || mc == null)
			return;
		if (geoObj == null || !hasAllObjects) {
			loadGeoObjects(mc);
		}
		if (geoObj == null)
			return;
		if ((vis != null || bkgVis != null) && dTable != null && !linkedToTable) {
			linkToThematicData();
		}
		RealRectangle rr = mc.getVisibleTerritory();
		if (getType() == Geometry.line) {
			drawParm.fillContours = false;
		}

		Visualizer v1 = null, v2 = null;
		if (vis != null && vis.isEnabled()) {
			v1 = vis;
		}
		if (bkgVis != null && bkgVis.isEnabled()) {
			v2 = bkgVis;
		}

		TimeFilter tf = getTimeFilter();
		TimeMoment t1 = null, t2 = null;
		boolean timeFiltered = false;
		if (tf != null) {
			t1 = tf.getFilterPeriodStart();
			t2 = tf.getFilterPeriodEnd();
			timeFiltered = t1 != null || t2 != null;
		}
		int nObj = getObjectCount();
		boolean ordered = order != null && order.length > 0;
		if (ordered) {
			nObj = order.length;
		}
		for (int i = 0; i < nObj; i++) {
			int ii = (ordered) ? order[i] : i;
			DGeoObject gobj = getObject(ii);
			if (timeFiltered) {
				gobj = (DGeoObject) gobj.getObjectVersionForTimeInterval(t1, t2);
				if (gobj == null) {
					continue;
				}
			}
			boolean active = oFilter == null || oFilter.equals(tf) || isObjectActive(ii);
			RealRectangle objLabelRect = gobj.getLabelRectangle();
			float objCenterX = (objLabelRect.rx1 + objLabelRect.rx2) / 2, objCenterY = (objLabelRect.ry1 + objLabelRect.ry2) / 2;
			if ((active || drawContoursOfInactiveObjects()) && gobj.fitsInRectangle(rr.rx1, rr.ry1, rr.rx2, rr.ry2) && rr.contains(objCenterX, objCenterY, 0.0f))
				if (active) {
					gobj.setDrawingParameters(drawParm);
					gobj.setVisualizer(v1);
					gobj.setBackgroundVisualizer(v2);
					if (gobj.getSpatialType() == Geometry.image) {
						gobj.setImageObserver(this);
					}
					gobj.draw(g, mc);
				} else {
					g.setColor(Color.gray);
					gobj.drawContour(g, mc);
				}
		}
	}

	protected boolean drawContoursOfInactiveObjects() {
		return false;
/*
    if (getType()!=Geometry.area) return false;
    if (vis!=null && vis.isEnabled() && vis.isDiagramPresentation()) return false;
    return drawParm.drawBorders;
*/
	}

	public boolean imageUpdate(Image image, int infoflags, int x, int y, int width, int height) {
		/*
		System.out.println("image update: "+Integer.toHexString(infoflags)+
		  " width="+width+" height="+height);
		*/
		if ((infoflags & ALLBITS) == ALLBITS) {
			notifyPropertyChange("ImageState", null, null);
			return false;
		}
		return true;
	}

	/**
	* Checks if any of selected objects are in the layer in current MapContext
	*/
	public boolean hasSelectedObjects(MapContext mc) {
		RealRectangle rr = mc.getVisibleTerritory();
		boolean found = false;
		for (int i = 0; i < getObjectCount() && !found; i++) {
			DGeoObject gobj = getObject(i);
			if (isObjectActive(i) && gobj.isSelected() && gobj.fitsInRectangle(rr.rx1, rr.ry1, rr.rx2, rr.ry2)) {
				found = true;
			}
		}
		return found;
	}

	/**
	* Draws all selected objects on top of other objects of the layer
	*/
	public void drawSelectedObjects(Graphics g, MapContext mc) {
		if (geoObj == null || g == null || mc == null)
			return;
		TimeFilter tf = getTimeFilter();
		TimeMoment t1 = null, t2 = null;
		boolean timeFiltered = false;
		if (tf != null) {
			t1 = tf.getFilterPeriodStart();
			t2 = tf.getFilterPeriodEnd();
			timeFiltered = t1 != null || t2 != null;
		}
		RealRectangle rr = mc.getVisibleTerritory();
		int nObj = getObjectCount();
		boolean ordered = order != null && order.length > 0;
		if (ordered) {
			nObj = order.length;
		}
		for (int i = 0; i < nObj; i++) {
			int ii = (ordered) ? order[i] : i;
			DGeoObject gobj = getObject(ii);
			if (timeFiltered) {
				gobj = (DGeoObject) gobj.getObjectVersionForTimeInterval(t1, t2);
				if (gobj == null) {
					continue;
				}
			}
			if (gobj.isSelected() && isObjectActive(ii) && gobj.fitsInRectangle(rr.rx1, rr.ry1, rr.rx2, rr.ry2)) {
				gobj.showSelection(g, mc);
			}
		}
	}

	/**
	* Hides visibility of selection (without change of the status of the
	* highlighted objects)
	*/
	public void hideSelection(Graphics g, MapContext mc) {
		if (geoObj == null || g == null || mc == null)
			return;
		TimeFilter tf = getTimeFilter();
		TimeMoment t1 = null, t2 = null;
		boolean timeFiltered = false;
		if (tf != null) {
			t1 = tf.getFilterPeriodStart();
			t2 = tf.getFilterPeriodEnd();
			timeFiltered = t1 != null || t2 != null;
		}
		RealRectangle rr = mc.getVisibleTerritory();
		int nObj = getObjectCount();
		boolean ordered = order != null && order.length > 0;
		if (ordered) {
			nObj = order.length;
		}
		for (int i = 0; i < nObj; i++) {
			int ii = (ordered) ? order[i] : i;
			DGeoObject gobj = getObject(ii);
			if (timeFiltered) {
				gobj = (DGeoObject) gobj.getObjectVersionForTimeInterval(t1, t2);
				if (gobj == null) {
					continue;
				}
			}
			if (isObjectActive(ii) && gobj.isSelected() && gobj.fitsInRectangle(rr.rx1, rr.ry1, rr.rx2, rr.ry2)) {
				gobj.hideSelection(g, mc);
			}
		}
	}

	/**
	* Hides visibility of hihglighting (without change of the status of the
	* highlighted objects)
	*/
	public void hideHighlighting(Graphics g, MapContext mc) {
		if (!drawParm.drawLayer || g == null || mc == null)
			return;
		if (geoObj == null)
			return;
		TimeFilter tf = getTimeFilter();
		TimeMoment t1 = null, t2 = null;
		boolean timeFiltered = false;
		if (tf != null) {
			t1 = tf.getFilterPeriodStart();
			t2 = tf.getFilterPeriodEnd();
			timeFiltered = t1 != null || t2 != null;
		}
		RealRectangle rr = mc.getVisibleTerritory();
		int nObj = getObjectCount();
		boolean ordered = order != null && order.length > 0;
		if (ordered) {
			nObj = order.length;
		}
		for (int i = 0; i < nObj; i++) {
			int ii = (ordered) ? order[i] : i;
			DGeoObject gobj = getObject(ii);
			if (timeFiltered) {
				gobj = (DGeoObject) gobj.getObjectVersionForTimeInterval(t1, t2);
				if (gobj == null) {
					continue;
				}
			}
			if (isObjectActive(ii) && gobj.isHighlighted() && gobj.fitsInRectangle(rr.rx1, rr.ry1, rr.rx2, rr.ry2)) {
				gobj.hideHighlight(g, mc);
			}
		}
	}

	public void showHighlighting(Graphics g, MapContext mc) {
		if (!drawParm.drawLayer || g == null || mc == null)
			return;
		if (geoObj == null)
			return;
		TimeFilter tf = getTimeFilter();
		TimeMoment t1 = null, t2 = null;
		boolean timeFiltered = false;
		if (tf != null) {
			t1 = tf.getFilterPeriodStart();
			t2 = tf.getFilterPeriodEnd();
			timeFiltered = t1 != null || t2 != null;
		}
		RealRectangle rr = mc.getVisibleTerritory();
		int nObj = getObjectCount();
		boolean ordered = order != null && order.length > 0;
		if (ordered) {
			nObj = order.length;
		}
		for (int i = 0; i < nObj; i++) {
			int ii = (ordered) ? order[i] : i;
			DGeoObject gobj = getObject(ii);
			if (timeFiltered) {
				gobj = (DGeoObject) gobj.getObjectVersionForTimeInterval(t1, t2);
				if (gobj == null) {
					continue;
				}
			}
			if (isObjectActive(ii) && gobj.isHighlighted() && gobj.fitsInRectangle(rr.rx1, rr.ry1, rr.rx2, rr.ry2)) {
				gobj.showHighlight(g, mc);
			}
		}
	}

	/**
	* Dehighlights all the highlighted objects (changes their status, but
	* does not draw).
	*/
	public void dehighlightAllObjects() {
		if (geoObj == null)
			return;
		TimeFilter tf = getTimeFilter();
		TimeMoment t1 = null, t2 = null;
		boolean timeFiltered = false;
		if (tf != null) {
			t1 = tf.getFilterPeriodStart();
			t2 = tf.getFilterPeriodEnd();
			timeFiltered = t1 != null || t2 != null;
		}
		int nObj = getObjectCount();
		boolean ordered = order != null && order.length > 0;
		if (ordered) {
			nObj = order.length;
		}
		for (int i = 0; i < nObj; i++) {
			int ii = (ordered) ? order[i] : i;
			DGeoObject gobj = getObject(ii);
			if (gobj != null && gobj.isHighlighted()) {
				if (timeFiltered) {
					gobj = (DGeoObject) gobj.getObjectVersionForTimeInterval(t1, t2);
				}
				if (gobj != null) {
					gobj.setIsHighlighted(false);
				}
			}
		}
	}

	public void drawDiagrams(Graphics g, MapContext mc) {
		if (g == null || mc == null)
			return;
		if (vis == null || !vis.isEnabled() || !vis.isDiagramPresentation())
			return;
		if (geoObj == null || !hasAllObjects) {
			loadGeoObjects(mc);
		}
		if (geoObj == null)
			return;
		if ((vis != null || bkgVis != null) && dTable != null && !linkedToTable) {
			linkToThematicData();
		}
		TimeFilter tf = getTimeFilter();
		TimeMoment t1 = null, t2 = null;
		boolean timeFiltered = false;
		if (tf != null) {
			t1 = tf.getFilterPeriodStart();
			t2 = tf.getFilterPeriodEnd();
			timeFiltered = t1 != null || t2 != null;
		}
		RealRectangle rr = mc.getVisibleTerritory();
		int nObj = getObjectCount();
		boolean ordered = order != null && order.length > 0;
		if (ordered) {
			nObj = order.length;
		}
		for (int i = 0; i < nObj; i++) {
			int ii = (ordered) ? order[i] : i;
			DGeoObject gobj = getObject(ii);
			if (timeFiltered) {
				gobj = (DGeoObject) gobj.getObjectVersionForTimeInterval(t1, t2);
				if (gobj == null) {
					continue;
				}
			}
			if (isObjectActive(ii) && gobj.fitsInRectangle(rr.rx1, rr.ry1, rr.rx2, rr.ry2)) {
				gobj.setDrawingParameters(drawParm);
				gobj.setVisualizer(vis);
				gobj.setBackgroundVisualizer(bkgVis);
				gobj.drawDiagram(g, mc);
			}
		}
	}

	public void drawLabels(Graphics g, MapContext mc) {
		if (g == null || mc == null)
			return;
		if (!hasLabels || !drawParm.drawLabels)
			return;
		Color labColor = drawParm.labelColor;
		if (drawParm.transparency > 0) {
			labColor = Drawing2D.getTransparentColor(labColor, drawParm.transparency);
		}
		g.setColor(labColor);
		Font fOrig = null;
		if (labelFont == null && drawParm.fontName != null) {
			labelFont = new Font(drawParm.fontName, drawParm.fontStyle, drawParm.fontSize);
		}
		if (labelFont != null) {
			fOrig = g.getFont();
			g.setFont(labelFont);
		}
		TimeFilter tf = getTimeFilter();
		TimeMoment t1 = null, t2 = null;
		boolean timeFiltered = false;
		if (tf != null) {
			t1 = tf.getFilterPeriodStart();
			t2 = tf.getFilterPeriodEnd();
			timeFiltered = t1 != null || t2 != null;
		}
		RealRectangle rr = mc.getVisibleTerritory();
		int nObj = getObjectCount();
		boolean ordered = order != null && order.length > 0;
		if (ordered) {
			nObj = order.length;
		}
		for (int i = 0; i < nObj; i++) {
			int ii = (ordered) ? order[i] : i;
			DGeoObject gobj = getObject(ii);
			if (timeFiltered) {
				gobj = (DGeoObject) gobj.getObjectVersionForTimeInterval(t1, t2);
				if (gobj == null) {
					continue;
				}
			}
			if (gobj.getLabel() != null && isObjectActive(ii) && gobj.fitsInRectangle(rr.rx1, rr.ry1, rr.rx2, rr.ry2)) {
				gobj.drawLabel(g, mc, drawParm.labelStyle);
			}
		}
		if (fOrig != null) {
			g.setFont(fOrig);
		}
	}

	/**
	* In this method the layer draws information about it in the legend.
	* When the layer has a Visualizer, the function drawLegend of the
	* Visualizer should be called.
	* The method belongs to the LegendDrawer interface - see the comments there.
	*/
	public Rectangle drawLegend(Component c, Graphics g, int startY, int leftMarg, int prefW) {
		if (g == null)
			return null;
		startY += 4;
		int x0 = leftMarg;
		FontMetrics fm = g.getFontMetrics();
		if (fm == null)
			return null;
		if (switchWidth <= 0) {
			switchWidth = Metrics.mm() * 4;
		}
		if (iconH <= 0) {
			iconH = Metrics.mm() * 5;
		}
		if (iconW <= 0) {
			iconW = Metrics.mm() * 8;
		}
		int iconMarg = Metrics.mm() / 2;
		if (fm.getHeight() + 2 * iconMarg > iconH) {
			iconH = fm.getHeight() + 2 * iconMarg;
		}
		g.setColor(Color.darkGray);
		//System.out.println("~~~ 1 (DGeoLayer.drawLegend)");
		if (drawParm != null && drawParm.drawLayer) {
			Icons.drawChecked(g, leftMarg, startY, switchWidth, iconH);
		} else {
			Icons.drawUnchecked(g, leftMarg, startY, switchWidth, iconH);
		}
		if (c != null) {
			if (spots == null) {
				spots = new HotSpot[5];
				for (int i = 0; i < spots.length; i++) {
					spots[i] = new HotSpot(c);
					spots[i].addActionListener(this);
				}
				spots[0].setActionCommand("switch");
				spots[0].setPopup(res.getString("switch_layer"));
				spots[1].setActionCommand("edit_draw_param");
				spots[1].setPopup(res.getString("change_layer_drawing"));
				spots[2].setActionCommand("activate");
				spots[2].setPopup(res.getString("activate_layer"));
				spots[3].setActionCommand("edit_visualization");
				spots[3].setPopup(res.getString("change_visualization"));
				spots[4].setActionCommand("edit_bkg_visualization");
				spots[4].setPopup(res.getString("change_visualization"));
			} else {
				for (HotSpot spot : spots) {
					spot.setOwner(c);
				}
			}
			spots[0].setLocation(leftMarg, startY);
			spots[0].setSize(switchWidth, iconH);
		}
		//System.out.println("~~~ 2 (DGeoLayer.drawLegend)");
		leftMarg += switchWidth + Metrics.mm();
		//draw a two-color background under the icon
		g.setColor(Color.white);
		g.fillRect(leftMarg, startY, iconW, iconH / 2 + 1);
		g.setColor(Color.gray);
		g.fillRect(leftMarg, startY + iconH / 2, iconW, iconH / 2 + 1);
		int lw = fm.stringWidth("L"), lh = fm.getHeight();
		//System.out.println("~~~ 3 (DGeoLayer.drawLegend)");
		if (c != null && spots != null) {
			spots[1].setLocation(leftMarg, startY);
			spots[1].setSize(iconW + iconMarg + lw + 8, iconH);
		}
		int iH = iconH - 2 * iconMarg, iW = iconW - 2 * iconMarg;
		leftMarg += iconMarg;
		startY += iconMarg;
		boolean iconDrawn = false;
		//System.out.println("~~~ 4 (DGeoLayer.drawLegend)");
		if (bkgVis != null && bkgVis.isEnabled()) {
			bkgVis.drawIcon(g, leftMarg, startY, iW, iH);
			iconDrawn = true;
		}
		if (vis != null && vis.isEnabled()) {
			vis.drawIcon(g, leftMarg, startY, iW, iH);
			iconDrawn = true;
		}
		if (!iconDrawn) {
			//System.out.println("~~~ 5 (DGeoLayer.drawLegend)");
			g.setColor(Color.black);
			if (drawParm == null) {
				g.drawString("?", leftMarg, startY + fm.getAscent());
			} else {
				Color lineColor = drawParm.lineColor, fillColor = drawParm.fillColor;
				if (drawParm.transparency > 0) {
					if (lineColor != null) {
						lineColor = Drawing2D.getTransparentColor(lineColor, drawParm.transparency);
					}
					if (fillColor != null) {
						fillColor = Drawing2D.getTransparentColor(fillColor, drawParm.transparency);
					}
				}
				switch (getType()) {
				case Geometry.image:
				case Geometry.raster:
					g.setColor(Color.blue.darker());
					g.fillRect(leftMarg, startY, iW, iH);
					int m1 = Metrics.mm(),
					m2 = Metrics.mm() / 2;
					g.setColor(Color.orange.darker());
					g.fillOval(leftMarg + m1, startY + m2, iW - 2 * m1, iH - 2 * m2);
					g.setColor(Color.green.darker());
					m1 = (iW - 2 * Metrics.mm()) / 2;
					m2 = (iH - 2 * Metrics.mm()) / 2;
					g.fillOval(leftMarg + m1, startY + m2, iW - 2 * m1, iH - 2 * m2);
					break;
				case Geometry.area:
					if (drawParm.fillContours) {
						g.setColor(fillColor);
						g.fillRect(leftMarg, startY, iW, iH);
					}
					if (drawParm.drawBorders) {
						g.setColor(lineColor);
						Drawing.drawRectangle(g, drawParm.lineWidth, leftMarg, startY, iW, iH);
					}
					break;
				case Geometry.point:
					int stp = Metrics.mm(),
					x = leftMarg,
					dx = iW / 3,
					y = startY + (iH - DGeoObject.signWidth) / 2;
					if (dx < DGeoObject.signWidth + stp) {
						dx = DGeoObject.signWidth + stp;
					}
					do {
						if (drawParm.fillContours) {
							g.setColor(fillColor);
							g.fillOval(x, y, DGeoObject.signWidth, DGeoObject.signWidth);
						}
						if (drawParm.drawBorders) {
							g.setColor(lineColor);
							Drawing.drawOval(g, drawParm.lineWidth, x, y, DGeoObject.signWidth, DGeoObject.signWidth);
						}
						x += dx;
					} while (x + DGeoObject.signWidth < leftMarg + iW);
					break;
				case Geometry.line:
					x = leftMarg;
					y = startY + iH;
					dx = iW / 3;
					g.setColor(lineColor);
					Drawing.drawLine(g, drawParm.lineWidth, x, y, x + dx, startY, true, false);
					x += dx;
					Drawing.drawLine(g, drawParm.lineWidth, x, startY, x + dx, y, false, false);
					x += dx;
					Drawing.drawLine(g, drawParm.lineWidth, x, y, x + dx, startY, false, true);
					break;
				case Geometry.undefined:
					g.drawString("?", leftMarg, startY + fm.getAscent());
					break;
				}
			}
		}
		//System.out.println("~~~ 6 (DGeoLayer.drawLegend)");
		iH += 2 * iconMarg;
		iW += 2 * iconMarg;
		leftMarg -= iconMarg;
		startY -= iconMarg;
		int maxW = leftMarg + iW + Metrics.mm(), maxH = iH + Metrics.mm();
		if (hasLabels && drawParm != null && drawParm.drawLabels) {
			g.setColor(Color.darkGray);
			g.drawRect(maxW, startY, lw + 8, lh + 4);
			g.setColor(drawParm.labelColor);
			g.drawString("L", maxW + 4, startY + fm.getAscent() + 2);
		}
		maxW += 8 + lw + Metrics.mm();
		int nameX = maxW;
		g.setColor(Color.black);
		//System.out.println("~~~ 7 (DGeoLayer.drawLegend)");
		if (name != null) {
			int w = fm.stringWidth(name);
			if (maxW + w <= prefW) {
				g.drawString(name, nameX, startY + fm.getAscent());
				maxW += w;
			} else {
				Point p = StringInRectangle.drawText(g, name, nameX, startY, prefW - maxW, true);
				if (p.y - startY > maxH) {
					maxH = p.y - startY;
				}
				maxW = p.x;
			}
		}
		if (maxW < prefW) {
			maxW = prefW;
		}
		startY -= 4;
		maxH += 4;
		//System.out.println("~~~ 8 (DGeoLayer.drawLegend)");
		if (isActive) {
			g.setColor(Color.red.darker());
			g.drawRect(0, startY, maxW, maxH);
			g.drawRect(1, startY + 1, maxW - 2, maxH - 2);
			g.drawRect(2, startY + 2, maxW - 4, maxH - 4);
		}
		if (c != null && spots != null) {
			spots[2].setLocation(nameX, startY + 1);
			if (isActive) {
				spots[2].setSize(0, 0);
			} else {
				spots[2].setSize(maxW - nameX, maxH - 2);
				//System.out.println(spots[2].getBounds().toString());
			}
		}
		maxH += Metrics.mm();
		//System.out.println("~~~ 9 (DGeoLayer.drawLegend)");
		if (spots != null) {
			spots[3].setSize(0, 0);
			spots[4].setSize(0, 0);
		}
		Rectangle air = showAdditionalLayerInfo(g, startY + maxH, x0, prefW);
		if (air != null && air.height > 0) {
			if (maxW < air.width) {
				maxW = air.width;
			}
			maxH += air.height;
		}
		for (int i = 0; i < 2; i++) {
			Visualizer visualizer = (i == 0) ? vis : bkgVis;
			if (visualizer != null) {
				maxH += 2 * Metrics.mm();
				int prevH = maxH;
				if (c != null) {
					spots[3 + i].setLocation(0, startY + prevH);
				}
				Rectangle r = visualizer.drawLegend(c, g, startY + prevH, x0, prefW);
				if (r != null) {
					if (maxW < r.width) {
						maxW = r.width;
					}
					maxH += r.height;
					prevH += visualizer.getSwitchSize();
					if (c != null) {
						spots[3 + i].setLocation(0, startY + prevH);
					}
				}
				maxH += Metrics.mm();
				if (c != null && visualizer.isEnabled() && visualizer.canChangeParameters()) {
					spots[3 + i].setSize(maxW, maxH - prevH);
				}
			}
		}
		//System.out.println("~~~ 10 (DGeoLayer.drawLegend)");
		if (show_nobjects) {
			// Display now the total number of objects in the layer
			int objCount = getObjectCount();
			String sTotal = res.getString("Total_") + Integer.toString(objCount) + " " + ((objCount == 1) ? res.getString("object") : res.getString("objects"));
			if (nActive < objCount) {
				sTotal += "; active: " + nActive;
			}
			g.setColor(Color.black);
			Point p = StringInRectangle.drawText(g, sTotal, x0, startY + maxH, prefW, false);
			if (p.y - startY > maxH) {
				maxH = p.y - startY;
			}
			if (p.x > maxW) {
				maxW = p.x;
			}
		}
		//System.out.println("~~~11 (DGeoLayer.drawLegend)");
		return new Rectangle(0, startY, maxW, maxH);
	}

	/**
	 * Puts additional information (if any) about a layer in the legend immediately below
	 * the line with the layer name and icon
	 */
	public Rectangle showAdditionalLayerInfo(Graphics g, int startY, int leftMarg, int prefW) {
		return null;
	}

	/**
	* A LegendDrawer may somehow react on mouse clicks in the legend area
	* However, a DGeoLayer does not do anything on mouse click.
	* Its LayerManager reacts on mouse click: makes the corresponding layer active.
	*/
	public void mousePressedInLegend(Object source, int x, int y) {
	}

	public void mouseDoubleClickedInLegend(Object source, int x, int y) {
	}

//-------------- Methods to access the GeoObjects ----------------------------
	/**
	* Reports whether there are any objects in this container.
	*/
	public boolean hasData() {
		return geoObj != null && geoObj.size() > 0;
	}

	/**
	* If the data actually have not been loaded in the container yet, this method
	* loads them. Returns true if data has been successfully loaded.
	*/
	public boolean loadData() {
		return loadGeoObjects();
	}

	/**
	* A method from the basic GeoLayer interface.
	* Returns the current number of GeoObjects.
	*/
	public int getObjectCount() {
		if (geoObj == null)
			return 0;
		return geoObj.size();
	}

	/**
	* Returns the DGeoObject at the given index.
	*/
	public DGeoObject getObject(int n) {
		if (n < 0 || n >= getObjectCount())
			return null;
		return (DGeoObject) geoObj.elementAt(n);
	}

	/**
	* Returns the GeoObject at the given index.
	* A method from the basic GeoLayer interface. Actually, calls getObject,
	* but returns GeoObject instead of DGeoObject
	*/
	public GeoObject getObjectAt(int idx) {
		return getObject(idx);
	}

	/**
	* A method from the basic GeoLayer interface.
	*/
	public Vector getObjects() {
		return geoObj;
	}

//---------------------- Methods to find GeoObjects ---------------------------
	/**
	 * Finds an object containing the specified position irrespective of
	 * the filters and visibility. Returns the index of the object or -1 if not found.
	 */
	public int findObjectContainingPosition(float x, float y) {
		int nObj = getObjectCount();
		if (nObj < 1)
			return -1;
		boolean isArea = getType() == Geometry.area;
		float tolerateDist = 0;
		if (!isArea) {
			RealRectangle rr = getWholeLayerBounds();
			if (rr == null) {
				rr = getCurrentLayerBounds();
			}
			if (rr == null)
				return -1;
			tolerateDist = Math.max(rr.rx2 - rr.rx1, rr.ry2 - rr.ry1) / 10000;
		}
		for (int i = 0; i < nObj; i++) {
			DGeoObject obj = (DGeoObject) geoObj.elementAt(i);
			if (obj.getGeometry() == null) {
				continue;
			}
			if (obj.getGeometry().contains(x, y, tolerateDist, isArea))
				return i;
		}
		return -1;
	}

	/**
	* A method from the basic GeoLayer interface.
	* This method is used to find the objects pointed to with the mouse.
	* Only objects having identifiers can be picked.
	* When the argument findOne is true, the method returns after finding the
	* first object at the mouse position
	*/
	public Vector findObjectsAt(int x, int y, MapContext mc, boolean findOne) {
		if (!drawParm.drawLayer)
			return null;
		if (mc == null)
			return null;
		TimeFilter tf = getTimeFilter();
		TimeMoment t1 = null, t2 = null;
		boolean timeFiltered = false;
		if (tf != null) {
			t1 = tf.getFilterPeriodStart();
			t2 = tf.getFilterPeriodEnd();
			timeFiltered = t1 != null || t2 != null;
		}
		int nObj = getObjectCount();
		boolean ordered = order != null && order.length > 0;
		if (ordered) {
			nObj = order.length;
		}
		Vector pointed = new Vector(10, 10);
		if (getType() == Geometry.point) {
			float rx1 = mc.absX(x - DGeoObject.halfSignWidth), rx2 = mc.absX(x + DGeoObject.halfSignWidth), ry2 = mc.absY(y - DGeoObject.halfSignWidth), ry1 = mc.absY(y + DGeoObject.halfSignWidth);
			for (int i = 0; i < nObj; i++) {
				int ii = (ordered) ? order[i] : i;
				DGeoObject gobj = getObject(ii);
				if (timeFiltered) {
					gobj = (DGeoObject) gobj.getObjectVersionForTimeInterval(t1, t2);
					if (gobj == null) {
						continue;
					}
				}
				if (isObjectActive(ii) && gobj.getIdentifier() != null && gobj.getIdentifier().length() > 0 && gobj.fitsInRectangle(rx1, ry1, rx2, ry2)) {
					pointed.addElement(gobj.getIdentifier());
					if (findOne)
						return pointed;
				}
			}
		} else {
			float rx = mc.absX(x), ry = mc.absY(y);
			float factor = (getType() == Geometry.area) ? 0.2f : 0.8f;
			float tolerateDist = factor * Metrics.mm() * mc.getPixelValue();
			for (int i = nObj - 1; i >= 0; i--) {
				//the backward order is important: smaller contours are drawn later
				int ii = (ordered) ? order[i] : i;
				DGeoObject gobj = getObject(ii);
				if (timeFiltered) {
					gobj = (DGeoObject) gobj.getObjectVersionForTimeInterval(t1, t2);
					if (gobj == null) {
						continue;
					}
				}
				if (isObjectActive(ii) && gobj.contains(rx, ry, tolerateDist)) {
					pointed.addElement(gobj.getIdentifier());
					if (findOne)
						return pointed;
				}
			}
		}
		if (pointed.size() < 1)
			return null;
		return pointed;
	}

	/**
	* Finds objects fitting in the specified rectangle.
	*/
	public Vector findObjectsIn(int x1, int y1, int x2, int y2, MapContext mc) {
		if (!drawParm.drawLayer)
			return null;
		if (mc == null)
			return null;
		if (x1 == x2 && y1 == y2)
			return findObjectsAt(x1, y1, mc, false);
		float rx1 = mc.absX(x1), ry1 = mc.absY(y1), rx2 = mc.absX(x2), ry2 = mc.absY(y2);
		if (rx1 > rx2) {
			float f = rx1;
			rx1 = rx2;
			rx2 = f;
		}
		if (ry1 > ry2) {
			float f = ry1;
			ry1 = ry2;
			ry2 = f;
		}
		Vector fit = new Vector(20, 10);
		TimeFilter tf = getTimeFilter();
		TimeMoment t1 = null, t2 = null;
		boolean timeFiltered = false;
		if (tf != null) {
			t1 = tf.getFilterPeriodStart();
			t2 = tf.getFilterPeriodEnd();
			timeFiltered = t1 != null || t2 != null;
		}
		int nObj = getObjectCount();
		boolean ordered = order != null && order.length > 0;
		if (ordered) {
			nObj = order.length;
		}
		for (int i = 0; i < nObj; i++) {
			int ii = (ordered) ? order[i] : i;
			DGeoObject gobj = getObject(ii);
			if (timeFiltered) {
				gobj = (DGeoObject) gobj.getObjectVersionForTimeInterval(t1, t2);
				if (gobj == null) {
					continue;
				}
			}
			if (isObjectActive(ii) && gobj.getIdentifier() != null && gobj.getIdentifier().length() > 0 && gobj.isInRectangle(rx1, ry1, rx2, ry2)) {
				fit.addElement(gobj.getIdentifier());
			}
		}
		return fit;
	}

	/**
	* A method from the basic GeoLayer interface.
	*/
	public GeoObject findObjectById(String ident) {
		return getObject(getObjectIndex(ident));
	}

	/**
	* Returns the index of the object with the given identifier, if found
	*/
	public int getObjectIndex(String ident) {
		if (ident == null)
			return -1;
		if (objIndex != null) {
			Object oInd = objIndex.get(ident);
			if (oInd != null && (oInd instanceof Integer))
				return ((Integer) oInd).intValue();
			return -1;
		}
		for (int i = 0; i < getObjectCount(); i++) {
			DGeoObject gobj = getObject(i);
			if (gobj != null && ident.equalsIgnoreCase(gobj.getIdentifier()))
				return i;
		}
		return -1;
	}

	/**
	* Returns the data item associated with the object at the given index.
	*/
	public DataItem getObjectData(int idx) {
		DGeoObject obj = getObject(idx);
		if (obj == null)
			return null;
		return obj.getSpatialData();
	}

	/**
	* Returns the ID of the object with the given index. The result may be null.
	*/
	public String getObjectId(int idx) {
		DGeoObject obj = getObject(idx);
		if (obj == null)
			return null;
		return obj.getIdentifier();
	}

//---------------------- highlighting --------------------------------
	/**
	* A method from the basic GeoLayer interface.
	* This method is used to switch on/off transient highlighting of the
	* geographical entity specified through the identifier.
	* The GeoLayer should draw the entity in "highlighted" state.
	*/
	public boolean highlightObject(String objectId, boolean isHighlighted, Graphics g, MapContext mc) {
		if (!drawParm.drawLayer)
			return false;
		DGeoObject gobj = (DGeoObject) findObjectById(objectId);
		if (!isObjectActive(gobj))
			return false;
		gobj.highlight(g, mc, isHighlighted);
		return true;
	}

	/**
	* Switches on/off transient highlighting of the object but does not redraw it.
	*/
	public boolean setObjectHighlight(String objectId, boolean isHighlighted) {
		DGeoObject gobj = (DGeoObject) findObjectById(objectId);
		if (gobj == null)
			return false;
		gobj.setIsHighlighted(isHighlighted);
		return true;
	}

	/**
	* A method from the basic GeoLayer interface.
	* This method is used to switch on/off selection (durable highlighting).
	* The GeoLayer should draw the entity in "selected" state.
	*/
	public boolean selectObject(String objectId, boolean isSelected, Graphics g, MapContext mc) {
		DGeoObject gobj = (DGeoObject) findObjectById(objectId);
		if (gobj == null)
			return false;
		gobj.select(g, mc, isSelected);
		if (!drawParm.drawLayer)
			return false;
		if (isSelected && isObjectActive(gobj)) {
			gobj.showSelection(g, mc);
		}
		return true;
	}

//------------- Methods for loading and updating GeoObjects ----------------

	/**
	* A GeoLayer receives data for constructing GeoObjects from a DataSupplier.
	* A GeoLayer may listen to data change events issued by its DataSupplier.
	* For this purpose it should implement the DataChangeListener interface.
	*/
	public void setDataSupplier(DataSupplier ds) {
		dataSuppl = ds;
		//Here, if necessary, the GeoLayer may register itself as a listener
		//of data change events produced by the DataSupplier.
		//Ensure that the DataChangeListener interface is implemented
	}

	public boolean hasDataSupplier() {
		return dataSuppl != null;
	}

	public DataSupplier getDataSupplier() {
		return dataSuppl;
	}

	/**
	* The bounding rectangle of the last portion of objects received from the
	* DataSupplier
	*/
	private RealRectangle lastBounds = null;

	/**
	* Gets data from its Data supplier and constructs from them GeoObjects.
	* The GeoLayer may specify a query containing a spatial constraint -
	* the bounding rectangle the objects should fit in. The bounding
	* rectangle can be received from the MapContext.
	*/
	public synchronized boolean loadGeoObjects(MapContext mc) {
		if (geoObj != null && hasAllObjects)
			return true;
		if (!this.getLayerDrawCondition(lastPixelValue))
			return true;
		if (dataSuppl == null)
			return false;
		RealRectangle r = null;
		if (mc != null) {
			r = mc.getVisibleTerritory();
			if (r != null)
				if (/*!hasTooManyObjects &&*/geoObj != null && lastBounds != null && lastBounds.rx1 <= r.rx1 && lastBounds.ry1 <= r.ry1 && lastBounds.rx2 >= r.rx2 && lastBounds.ry2 >= r.ry2)
					return true; //no new objects need to be loaded
		}
		System.out.println("Layer " + name + " tries to load geo objects from its supplier");
		long t = System.currentTimeMillis();
		Vector bounds = null;
		DataPortion data = null;
		if (r != null) {
			bounds = new Vector(1, 1);
			bounds.addElement(r);
			data = dataSuppl.getData(bounds);
		} else {
			data = dataSuppl.getData();
		}
		if (data == null) {
			System.out.println("Layer " + name + ((r == null) ? ": could not get any data!" : ": cannot get data for the current view!"));
			return false;
		}
		if (!(data instanceof SpatialDataPortion)) {
			System.out.println("Layer " + name + ": inapropriate data type - " + data);
			return false;
		}
		System.out.println("Layer " + name + " receives " + data.getDataItemCount() + " data items");
		receiveSpatialData((SpatialDataPortion) data);
		if (r != null) {
			lastBounds = r;
		}
		if (hasAllObjects && dataSuppl != null) {
			dataSuppl.clearAll();
			dataSuppl = null;
		}
		if ((dTable instanceof DataTable) && geoObj != null && geoObj.size() > 0 && r != null && dataSuppl != null && dTable != null) {
			//remove records from the table that do not correspond to any object
			DataTable table = (DataTable) dTable;
			Vector<String> oIds = new Vector<String>(geoObj.size(), 10);
			boolean update = false;
			for (int i = 0; i < geoObj.size(); i++) {
				DGeoObject obj = (DGeoObject) geoObj.elementAt(i);
				oIds.addElement(obj.getIdentifier());
				if (obj.getData() != null && table.indexOf(obj.getIdentifier()) < 0 && (obj.getData() instanceof DataRecord)) {
					table.addDataRecord((DataRecord) obj.getData());
					update = true;
				}
			}
			int nRemoved = table.removeExtraRecords(oIds);
			update = update || nRemoved > 0;
			if (update) {
				table.notifyPropertyChange("data_updated", null, null);
			}
		}
		notifyPropertyChange("ObjectSet", null, null);
		System.out.println("Layer " + name + ": dTable=" + dTable);
		System.out.print("Layer has " + geoObj.size() + " objects");
		if (dTable != null) {
			System.out.print("; Table has " + dTable.getDataItemCount() + " objects");
		}
		System.out.println();
		t = System.currentTimeMillis() - t;
		System.out.println("Loading took " + t + " msec");
		return true;
	}

	/**
	* Gets data from its Data supplier and constructs from them GeoObjects
	* (without specifying a query)
	*/
	public synchronized boolean loadGeoObjects() {
		return loadGeoObjects(null);
	}

	/**
	* Update may be called upon zooming/shifting operations with the map.
	* If the layer does not contain all the objects but only those which are
	* currently visible, it should ask the Data Supplier to provide the
	* objects fitting in the new bounding rectangle.
	*/
	public void update(MapContext mc) {
		/*
		if (hasAllObjects) return;
		if (dataSuppl==null) return;
		if (geoObj!=null) geoObj.removeAllElements();
		if (getLayerDrawn()) loadGeoObjects(mc);
		else geoObj=null; //this will force reloading of GeoObjects when the
		                  //GeoLayer is switched on again.
		notifyPropertyChange("ObjectSet",null,null);
		*/
	}

	/**
	* Reaction to "hot spot" clicking in the legend
	*/
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() instanceof HotSpot) {
			String cmd = e.getActionCommand();
			if (cmd.equals("switch")) {
				setLayerDrawn(!getLayerDrawn());
				if (drawParm.drawLayer && drawParm.drawCondition) {
					notifyPropertyChange("ConditionalLayerSwitchedOn", null, null);
				}
			} else if (cmd.equals("activate"))
				if (!isActive) {
					notifyPropertyChange("activation_request", null, null);
				} else {
					;
				}
			else if (cmd.equals("edit_draw_param")) {
				float cmValue = lastPixelValue * Metrics.cm() * user_factor;
				new ParamDlg(CManager.getAnyFrame(), this, this, drawParm, getType(), name, hasLabels, cmValue);
			} else if (cmd.equals("edit_visualization"))
				if (vis != null && vis.isEnabled() && vis.canChangeParameters()) {
					vis.startChangeParameters();
				} else {
					;
				}
			else if (cmd.equals("edit_bkg_visualization"))
				if (bkgVis != null && bkgVis.isEnabled() && bkgVis.canChangeParameters()) {
					bkgVis.startChangeParameters();
				}
		}
	}

	/**
	* Reaction to changing layer parameters through ParamDlg
	*/
	public void paramChanged(Object selector, DrawingParameters dp) {
		setDrawingParameters(dp);
	}

	/**
	* Reaction to changing layer name through ParamDlg
	*/
	public void nameChanged(Object selector, String name) {
		this.name = name;
		notifyPropertyChange("Name", null, name);
	}

	/**
	 * Informs whether the layer has moving objects
	 */
	public boolean getHasMovingObjects() {
		return hasMovingObjects;
	}

	/**
	 * Sets an indicator of the layer having moving objects
	 */
	public void setHasMovingObjects(boolean hasMovingObjects) {
		this.hasMovingObjects = hasMovingObjects;
	}

	/**
	 * Informs whether the coordinates of the objects in this layer are
	 * geographical, i.e. X is the longitude and Y is the latitude.
	 */
	public boolean isGeographic() {
		return isGeographic;
	}

	/**
	 * Sets whether the coordinates of the objects in this layer must be treated
	 * as geographical, i.e. X is the longitude and Y is the latitude.
	 */
	public void setGeographic(boolean geographic) {
		isGeographic = geographic;
		for (int i = 0; i < getObjectCount(); i++) {
			getObject(i).setGeographic(geographic);
		}
	}

	/**
	 * This method is called after a transformation of the time references
	 * of the objects, e.g. from absolute to relative times. The ObjectContainer
	 * may need to change some of its internal settings.
	 */
	public void timesHaveBeenTransformed() {
		firstTime = lastTime = null;
		if (geoObj != null) {
			for (int i = 0; i < geoObj.size(); i++) {
				((DGeoObject) geoObj.elementAt(i)).updateStartEndTimes();
			}
		}
		notifyPropertyChange("time_references", null, null);
	}

	/**
	 * If the layer has been produced by means of some analysis operation,
	 * returns a description of the operation
	 */
	public ActionDescr getMadeByAction() {
		return madeByAction;
	}

	/**
	 * If the layer has been produced by means of some analysis operation,
	 * sets a reference to a description of the operation
	 */
	public void setMadeByAction(ActionDescr madeByAction) {
		this.madeByAction = madeByAction;
	}

	/**
	 * Returns the associated table with metadata about the whole layer
	 * (not with data about every object). May return null, if no
	 * metadata exist.
	 */
	public AttributeDataPortion getMetaData() {
		return metaData;
	}

	/**
	 * Associates the layer with a table with metadata about the whole layer
	 * (not with data about every object).
	 */
	public void setMetaData(AttributeDataPortion metaData) {
		this.metaData = metaData;
	}

	/**
	 * Finds the shortest path between the objects with the given indices
	 * assuming that the objects have information about their neighbours
	 * Returns an instance of ObjectWithMeasure where the "object" is
	 * an array of indices of the objects making the path and the "measure"
	 * is the cumulative length of the path.
	 */
	public ObjectWithMeasure getShortestPath(int idx1, int idx2) {
		return getShortestPath(idx1, idx2, 1);
/*
    int nObj=getObjectCount();
    if (idx1<0 || idx2<0 || idx1>=nObj || idx2>=nObj) return null;
    if (idx1==idx2) {
      int path[]=new int[1];
      path[0]=idx1;
      return new ObjectWithMeasure(path,0);
    }
    DGeoObject go1=(DGeoObject)geoObj.elementAt(idx1), go2=(DGeoObject)geoObj.elementAt(idx2);
    if (go1.neighbours==null || go2.neighbours==null ||
        go1.neighbours.size()<1 || go2.neighbours.size()<1)
      return null; //no information about the neighbours
    double pathLen=0;
    IntArray path=new IntArray(20,10);
    path.addElement(idx1);
    boolean reached=false;
    RealPoint ptGoal=SpatialEntity.getCentre(go2.getGeometry());
    while (!reached) {
      double minDist=Double.NaN;
      int minIdx=-1;
      DGeoObject minNei=null;
      RealPoint pt0=SpatialEntity.getCentre(go1.getGeometry());
      for (int i=0; i<go1.neighbours.size(); i++) {
        int k=getObjectIndex(go1.neighbours.elementAt(i));
        if (k<0 || path.indexOf(k)>=0) continue;
        DGeoObject nei =(DGeoObject)geoObj.elementAt(k);
        if (k==idx2) {
          minDist=0; minIdx=k; minNei=nei; break;
        }
        RealPoint pt=SpatialEntity.getCentre(nei.getGeometry());
        double dist=GeoComp.distance(ptGoal.x,ptGoal.y,pt.x,pt.y,isGeographic)+
          GeoComp.distance(pt0.x,pt0.y,pt.x,pt.y,isGeographic);
        if (Double.isNaN(minDist) || dist<minDist) {
          minDist=dist; minIdx=k; minNei=nei;
        }
      }
      if (minIdx<0)
        break;
      path.addElement(minIdx);
      RealPoint pt=SpatialEntity.getCentre(minNei.getGeometry());
      pathLen+=GeoComp.distance(pt0.x,pt0.y,pt.x,pt.y,isGeographic);
      go1=minNei;
      reached=minIdx==idx2;
    }
    if (!reached) return null; //the destination is unreachable
    return new ObjectWithMeasure(path.getTrimmedArray(),pathLen);
*/
	}

	/**
	 * Finds the shortest path between the objects with the given indices
	 * assuming that the objects have information about their neighbours.
	 * The third parameter is the power to which the distances are raised.
	 * The method uses powered distances to allow for a trade-off between
	 * straight line path and a path following road curves (according to
	 * the paper by D.Guo). The recommended value is 1.5.
	 * Returns an instance of ObjectWithMeasure where the "object" is
	 * an array of indices of the objects making the path and the "measure"
	 * is the cumulative length of the path.
	 */
	public ObjectWithMeasure getShortestPath(int idx1, int idx2, float power) {
		int nObj = getObjectCount();
		if (idx1 < 0 || idx2 < 0 || idx1 >= nObj || idx2 >= nObj)
			return null;
		if (idx1 == idx2) {
			int path[] = new int[1];
			path[0] = idx1;
			return new ObjectWithMeasure(path, 0);
		}
		DGeoObject go1 = (DGeoObject) geoObj.elementAt(idx1), go2 = (DGeoObject) geoObj.elementAt(idx2);
		if (go1.neighbours == null || go2.neighbours == null || go1.neighbours.size() < 1 || go2.neighbours.size() < 1)
			return null; //no information about the neighbours
		//this will be the list of "visited" objects
		IntArray visited = new IntArray(100, 100);
		//this will be the list of possible paths starting from the source (idx1)
		//ordered according to the increasing lengths
		Vector<ObjectWithMeasure> paths = new Vector<ObjectWithMeasure>(100, 100);
		int path[] = new int[1];
		path[0] = idx1;
		paths.addElement(new ObjectWithMeasure(path, 0));
		while (paths.size() > 0) {
			//select the currently shortest path from the list, i.e. the 1st element
			ObjectWithMeasure shp = paths.elementAt(0);
			path = (int[]) shp.obj;
			int lastIdx = path[path.length - 1];
			if (lastIdx == idx2)
				return shp;
			paths.removeElementAt(0);
			if (visited.indexOf(lastIdx) >= 0) {
				continue;
			}
			visited.addElement(lastIdx);
			go1 = (DGeoObject) geoObj.elementAt(lastIdx);
			RealPoint pt0 = SpatialEntity.getCentre(go1.getGeometry());
			for (int i = 0; i < go1.neighbours.size(); i++) {
				int k = getObjectIndex(go1.neighbours.elementAt(i));
				if (k < 0 || visited.indexOf(k) >= 0) {
					continue;
				}
				DGeoObject nei = (DGeoObject) geoObj.elementAt(k);
				RealPoint pt = SpatialEntity.getCentre(nei.getGeometry());
				//double dist=GeoComp.distance(pt0.x,pt0.y,pt.x,pt.y,isGeographic);
				double dist = GeoComp.distance(pt0.x, pt0.y, pt.x, pt.y, false); //to speed up
				double powDist = (power == 1) ? dist : Math.pow(dist, power);
				int path1[] = new int[path.length + 1];
				for (int j = 0; j < path.length; j++) {
					path1[j] = path[j];
				}
				path1[path1.length - 1] = k;
				double length = powDist + shp.measure;
				ObjectWithMeasure pp = new ObjectWithMeasure(path1, length);
				boolean inserted = false;
				for (int j = 0; j < paths.size() && !inserted; j++)
					if (length < paths.elementAt(j).measure) {
						paths.insertElementAt(pp, j);
						inserted = true;
					}
				if (!inserted) {
					paths.addElement(pp);
				}
			}
		}
		return null;
	}
}
