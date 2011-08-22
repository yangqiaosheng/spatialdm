package spade.vis.space;

import java.awt.Graphics;
import java.beans.PropertyChangeListener;
import java.util.Vector;

import spade.lib.util.EntitySetContainer;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.DataPortion;
import spade.vis.database.ObjectFilter;
import spade.vis.database.SpatialDataPortion;
import spade.vis.map.MapContext;
import spade.vis.mapvis.Visualizer;

/**
* This is the interface for linking Descartes components to third-party software.
* For the Descartes components it is important that a GeoLayer is some object
* that
* 1) can draw geographical entities in a map;
* 2) can receive and use information from a Visualizer concerning how to draw
*    each geographical entity depending on thematic data;
* 3) listens to events from the Visualizer about visualization changes (for this
*    purpose implements PropertyChangeListener interface) and
*    can redraw itself after the changes;
* 4) informs about changes of the set of entities drawn in the map and about
*    changes of data associated with the entities;
* 5) can supply a vector of currently available geographical entities. Each
*    entity should be represented in this vector by an object implementing the
*    GeoObject interface. This interface makes it possible, in particular,
*    to refer to each entity through a unique identifier;
* 6) can determine which of the geographical entities is located in the map at
*    the given point (mouse cursor position);
* 7) can draw any specified geographical entity in "highlighted" and
*    in "selected" state and later return it to the "normal" state. The entity
*    to highlight/select is referred to through its identifier.
*/

public interface GeoLayer extends EntitySetContainer {
	/**
	* Adding and removing listeners to be notified about changes of the set
	* of geographical entities drawn in the map or of the thematic data
	* associated with them.
	*/
	public void addPropertyChangeListener(PropertyChangeListener l);

	public void removePropertyChangeListener(PropertyChangeListener l);

	/**
	* A method from the EntitySetContainer interface. Returns the unique
	* identifier of the layer.
	* Each layer in a map should have its unique identifier. The identifier
	* can be used, for example, to specify with what layer a table should be linked.
	*/
	@Override
	public String getContainerIdentifier();

	/**
	* A method from the EntitySetContainer interface.
	* In a map there may be several GeoLayers. Each has an identifier
	* of the set of geographic entities it includes.
	* The identifiers help in linking thematic and geographic data.
	* A table and a layer referring to the same set of entities will have the
	* same identifier of the entity set.
	*/
	@Override
	public String getEntitySetIdentifier();

	/**
	* A method from the EntitySetContainer interface.
	* Sets the identifier of the set of geographic objects referred to by this
	* layer.
	*/
	@Override
	public void setEntitySetIdentifier(String id);

	/**
	* Returns the name of the layer (that can be shown to the user
	*/
	public String getName();

	/**
	* Sets the name of the layer
	*/
	public void setName(String name);

	/**
	* Returns the specification of the data source, i.e. all
	* information necessary for loading the data from a file or database.
	* Normally this is an instance of spade.vis.spec.DataSourceSpec.
	*/
	public Object getDataSource();

	/**
	 * Informs whether the coordinates of the objects in this layer are
	 * geographical, i.e. X is the longitude and Y is the latitude.
	 */
	public boolean isGeographic();

	/**
	 * Sets whether the coordinates of the objects in this layer must be treated
	 * as geographical, i.e. X is the longitude and Y is the latitude.
	 */
	public void setGeographic(boolean geographic);

	/**
	* Replies whether the layer is drawn in the map.
	*/
	public boolean getLayerDrawn();

	/**
	* Sets the layer to be drawn on the map or to be invisible
	*/
	public void setLayerDrawn(boolean value);

	/**
	* A GeoLayer can be linked to a filter of objects. The objects that are
	* filtered out are not drawn on the map. The GeoLayer should be a
	* PropertyChangeListener and listen to changes of the set of active objects.
	*/
	public void setObjectFilter(ObjectFilter oFilter);

	/**
	* A GeoLayer can be linked to a tible with thematic data, one table at a time.
	* Simultaneously, it is linked to the TableFilter of this table. This method
	* sets a reference to the appropriate TableFilter. If there is some other
	* object filter associated with this layer (but not a TableFilter), this
	* filter is combined with the TableFilter. If the layer had earlier another
	* TableFilter, the old TableFilter is removed.
	* A TableFilter is distinguished from other filters by returning true
	* from its method isAttributeFilter().
	*/
	public void setThematicFilter(ObjectFilter tFilter);

	/**
	* Returns its object filter (@see setObjectFilter)
	*/
	public ObjectFilter getObjectFilter();

	/**
	* Returns its table filter (@see setThematicFilter)
	*/
	public ObjectFilter getThematicFilter();

	/**
	* Removes its filter (i.e. no filter remains)
	*/
	public void removeFilter();

	/**
	* Removes only its table filter and preserves all other filters, if any
	*/
	public void removeThematicFilter();

//------------------- Visualization of thematic data ---------------------------
	/**
	* Visualizer specifies how thematic information associated with GeoObjects
	* should be presented.
	* When a Visualizer is set, the GeoLayer should somehow trigger map redrawing
	* to draw on it thematic data as is specified by the Visualizer.
	* IMPORTANT!
	* The GeoLayer should also register itself as listener
	* of visualization changes of this Visualizer. If it has previously had
	* another Visualizer, it should remove itself from listeners of that
	* Visualizer.
	*/
	public void setVisualizer(Visualizer visualizer);

	public Visualizer getVisualizer();

	/**
	* For area layers, two visualizers are allowed: one defining the area filling
	* colors and the other producing diagrams or other symbols drawn on top of
	* the painting. This method is used for linking a layer to the visualizer
	* which will define the colors for painting.
	*/
	public void setBackgroundVisualizer(Visualizer visualizer);

	public Visualizer getBackgroundVisualizer();

//------------------ Data update -------------------------------------------
	/**
	* Gets data from its Data supplier and constructs from them GeoObjects
	* (without specifying a query)
	*/
	public boolean loadGeoObjects();

	/**
	* Gets data from its Data supplier and constructs from them GeoObjects.
	* The GeoLayer may specify a query containing a spatial constraint -
	* the bounding rectangle the objects should fit in. The bounding
	* rectangle can be received from the MapContext.
	*/
	public boolean loadGeoObjects(MapContext mc);

	/**
	* This method is used to pass to the layer source data necessary for
	* construction or update of GeoObjects. Returns the number of the objects
	* constructed (updated)
	*/
	public int receiveSpatialData(SpatialDataPortion data);

	/**
	* This method is used to pass to the layer source thematic data for
	* visualisation on a map. Returns the number of thematic data items
	* successfully linked to corresponding geographical objects.
	*/
	public int receiveThematicData(AttributeDataPortion data);

	/**
	* This method checks if the layer is currently linked to the given portion of
	* thematic data
	*/
	public boolean hasThematicData(AttributeDataPortion data);

	/**
	* This method checks if the layer is currently linked to any thematic data
	*/
	public boolean hasThematicData();

	/**
	* Reports whether the GeoObjects in this layer are temporally referenced.
	*/
	public boolean hasTimeReferences();

	/**
	* Returns the reference to the AttributeDataPortion with thematic data
	* the layer is currently linked to
	*/
	public AttributeDataPortion getThematicData();

	/**
	* Erases thematic data linked to geographical objects
	*/
	public void eraseThematicData();

	/**
	* This method counts how many of the GeoObjects comprising this GeoLayer
	* are found in the given DataPortion
	*/
	public int countOverlap(DataPortion dp);

//------------------ Access to geographical entities --------------------------
	/**
	* Returns the spatial type of its objects (point, line, area, or raster -
	* the constants are defined in the Geometry class). All
	* GeoObjects belonging to the same GeoLayer should have the same
	* spatial type.
	*/
	public char getType();

	/**
	 * Returns the subtype of the objects, which may be one of the special
	 * subtypes: circle, rectangle, vector, link, ... If not any of these subtypes,
	 * returns Geometry.undefined.
	 */
	public char getSubtype();

	/**
	* Returns the current number of GeoObjects.
	*/
	public int getObjectCount();

	/**
	* Returns the GeoObject at the given index.
	*/
	public GeoObject getObjectAt(int idx);

	/**
	* Informs whether the object with the given index is active, i.e. not
	* filtered out.
	*/
	public boolean isObjectActive(int idx);

	/**
	* Returns a vector of GeoObjects representing all currently available
	* geographical entities.
	*/
	public Vector getObjects();

	/**
	* Returns the identifiers (!) of geographical entities located in the map at
	* the given point.
	* This method is used to find the objects pointed to with the mouse
	* When the argument findOne is true, the method returns after finding the
	* first object at the mouse position
	*/
	public Vector findObjectsAt(int x, int y, MapContext mc, boolean findOne);

	/**
	* Finds objects fitting in the specified rectangle.
	*/
	public Vector findObjectsIn(int x1, int y1, int x2, int y2, MapContext mc);

	/**
	* Returns the geographical entity with the specified identifier.
	*/
	public GeoObject findObjectById(String ident);

	/**
	* Returns the index of the object with the given identifier, if found
	*/
	public int getObjectIndex(String ident);

//---------------------- highlighting --------------------------------
	/**
	* This method is used to switch on/off transient highlighting of the
	* geographical entity specified through the identifier.
	* When the argument isHighlighted is true, the GeoLayer should draw the
	* entity in "highlighted" state, otherwise in a "normal" state.
	* Returns true if the object with the given ID was found, otherwise
	* returns false.
	*/
	public boolean highlightObject(String objectId, boolean isHighlighted, Graphics g, MapContext mc);

	/**
	* Switches on/off transient highlighting of the object but does not redraw it.
	*/
	public boolean setObjectHighlight(String objectId, boolean isHighlighted);

	/**
	* Hides visibility of highlighting (without change of the status of the
	* highlighted objects)
	*/
	public void hideHighlighting(Graphics g, MapContext mc);

	/**
	* Visually marks highlighted objects
	*/
	public void showHighlighting(Graphics g, MapContext mc);

	/**
	* Dehighlights all the highlighted objects (changes their status, but
	* does not draw).
	*/
	public void dehighlightAllObjects();

	/**
	* This method is used to switch on/off selection (durable highlighting).
	* When the argument isSelected is true, the GeoLayer should draw the
	* entity in "selected" state, otherwise in a "normal" state.
	* Returns true if the object with the given ID was found, otherwise
	* returns false.
	*/
	public boolean selectObject(String objectId, boolean isSelected, Graphics g, MapContext mc);

	/**
	* Draws all selected objects on top of other objects of the layer
	*/
	public void drawSelectedObjects(Graphics g, MapContext mc);

	/**
	* Hides visibility of selection (without change of the status of the
	* highlighted objects)
	*/
	public void hideSelection(Graphics g, MapContext mc);

	/**
	* Returns a copy of this GeoLayer. The reference to the visualizer is also copied.
	*/
	public GeoLayer makeCopy();
}
