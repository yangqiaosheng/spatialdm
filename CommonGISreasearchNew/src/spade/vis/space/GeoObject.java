package spade.vis.space;

import spade.time.TimeMoment;
import spade.vis.database.ThematicDataItem;
import spade.vis.geometry.Geometry;

/**
* Geo Object is something that has spatial location and can be drawn in
* a map. A collection of homogeneous Geo Objects (e.g. countries or rivers)
* constitute a Geo Layer.
* A Geo Object may also have thematic data associated with it.
*
* GeoObject is a basic interface used for linking visualization and map
* manipulation components of Descartes to components performing mapping.
* The latter may be, in particular, some "foreign" modules.
* Such modules should be able to wrap information about geographical entities
* drawn in the map into an object implementing the GeoObject interface.
* From the perspective of the visualization-manipulation part of Descartes,
* a GeoObject has a unique identifier and associated thematic data.
* The details of its geometry, drawing etc. are not important.
*/

public interface GeoObject {
	/**
	* Returns the identifier of the geographical entity. The identifier of a
	* GeoObject should be unique. Two objects with the same identifier are assumed
	* to represent (parts of) the same entity.
	* Thus, GeoObjects representing islands belonging to Italy may have the same
	* identifier as the GeoObject representing Italy. In this case all
	* the objects will be treated as parts of a single object.
	*/
	public String getIdentifier();

	/**
	* Changes the identifier of the object; use cautiously!
	*/
	public void setIdentifier(String ident);

	/**
	 * Returns the name of this object
	 */
	public String getName();

	/**
	* Returns the Geometry of this object
	*/
	public Geometry getGeometry();

	/**
	 * Informs whether the geometry of this object has geographic coordinates
	 * (latitudes and longitudes). By default false.
	 */
	public boolean isGeographic();

	/**
	 * Sets the property of the object's geometry indicating whether
	 * the geometry has geographic coordinates (latitudes and longitudes).
	 */
	public void setGeographic(boolean geographic);

	/**
	* Returns thematic data associated with the geographical entity.
	*/
	public ThematicDataItem getData();

	/**
	* Returns true if some thematic data are associated with this geographical entity.
	*/
	public boolean hasData();

	/**
	* Returns a copy of this object. The spatial and thematic data items
	* used by this objects are not duplicated; the copy will use references to the
	* same data items
	*/
	public GeoObject makeCopy();

	/**
	 * Returns a version of this GeoObject corresponding to the specified time
	 * interval. If the object does not change over time, returns itself. May
	 * return null if the object does not exist during the specified interval.
	 */
	public GeoObject getObjectVersionForTimeInterval(TimeMoment t1, TimeMoment t2);

	/**
	 * Same as getObjectVersionForTimeInterval with a single difference:
	 * when only a part of the object fits into the interval, produces a new
	 * instance of GeoObject with this part.
	 */
	public GeoObject getObjectCopyForTimeInterval(TimeMoment t1, TimeMoment t2);
}
