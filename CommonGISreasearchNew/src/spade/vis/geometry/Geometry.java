package spade.vis.geometry;

import spade.time.TimeReference;

/**
* The basic class for Realpoint and RealPolyline
*/

public abstract class Geometry implements java.io.Serializable {
	/**
	* possible types of geometries
	*/
	public static final char point = 'P', area = 'A', line = 'L', raster = 'R', image = 'I', undefined = 'U';
	/**
	 * Subtypes of some of the basic types
	 */
	public static final char circle = 'C', rectangle = '4', vector = 'V', link = 'J', movement = 'M';
	/**
	* Used in the function getBoundRect to avoid creating of an array each time
	* when the function is called.
	*/
	protected static float bounds[] = new float[4];
	/**
	 * Indicates whether the geometry has geographic coordinates
	 * (latitudes and longitudes). By default false.
	 */
	protected boolean isGeo = false;
	/**
	 * A "centroid"; may be useful for some operations.
	 * Consists of 2 coordinates: x,y
	 */
	protected float centroid[] = null;
	/**
	 * A Geometry may have an index (e.g. in some container). This simplifies
	 * locating a geometry in a container. Originally -1.
	 */
	protected int index = -1;
	/**
	 * In some cases a Geometry may have a time reference
	 */
	protected TimeReference tref = null;

	/**
	* The function allowing to determine the type of this geometry:
	* point, line, area, or raster
	*/
	public abstract char getType();

	/**
	 * Informs whether the geometry has geographic coordinates
	 * (latitudes and longitudes). By default false.
	 */
	public boolean isGeographic() {
		return isGeo;
	}

	/**
	 * Sets the property indicating whether the geometry has geographic coordinates
	 * (latitudes and longitudes). 
	 */
	public void setGeographic(boolean geographic) {
		isGeo = geographic;
	}

	public float[] getCentroid() {
		return centroid;
	}

	public void setCentroid(float[] centroid) {
		this.centroid = centroid;
	}

	public void setCentroid(float x, float y) {
		centroid = new float[2];
		centroid[0] = x;
		centroid[1] = y;
	}

	/**
	 * A Geometry may have an index (e.g. in some container). This simplifies
	 * locating a geometry in a container.
	 * Returns the index attached to this geometry (may be -1).
	 */
	public int getIndex() {
		return index;
	}

	/**
	 * A Geometry may have an index (e.g. in some container). This simplifies
	 * locating a geometry in a container.
	 * Attaches the index to the geometry.
	 */
	public void setIndex(int index) {
		this.index = index;
	}

	/**
	 * Returns the time reference of this Geometry (may be null)
	 */
	public TimeReference getTimeReference() {
		return tref;
	}

	/**
	 * Sets the time reference of this Geometry
	 */
	public void setTimeReference(TimeReference tref) {
		this.tref = tref;
	}

	/**
	* Used to determine whether at least a part of the geometry is visible in the
	* current map viewport.
	*/
	public abstract boolean fitsInRectangle(float x1, float y1, float x2, float y2);

	/**
	* Used to determine whether a considerable part of the geometry is contained
	* within the given rectangle (e.g.selection of objects in a map with a frame)
	*/
	public abstract boolean isInRectangle(float x1, float y1, float x2, float y2);

	/**
	* Checks if the point (x,y) belongs to the object. The point, in particular,
	* may represent the position of the mouse in the map.
	*/
	public abstract boolean contains(float x, float y, float tolerateDist);

	/**
	* Checks if the point (x,y) belongs to the object. The argument treatAsArea
	* indicates whether the geometry must be treated as a closed region. In this
	* case, the method returns true when the point is inside the region or on the
	* boundary; otherwise, true is returned only when the point is on the line.
	* By default, pays no attention to the last argument. However, may be redefined
	* in the ancestors, for example, in an implementation of a linear geometry.
	*/
	public boolean contains(float x, float y, float tolerateDist, boolean treatAsArea) {
		return contains(x, y, tolerateDist);
	}

	@Override
	public abstract Object clone();

	/**
	* Useful utilities for children
	*/
	// prov() - remains for compatibility (for ex. with other code IPPI)
	public static boolean prov(float p1, float p2, float x) {
		return between(p1, p2, x);
	}

	public static boolean prov(float p1, float p2, float x, float tolerateDist) {
		if (between(p1, p2, x) || x == p1 || x == p2)
			return true;
		if (Math.abs(p1 - p2) < tolerateDist)
			return Math.abs(p1 - x) < tolerateDist || Math.abs(p2 - x) < tolerateDist;
		return false;
	}

	public static boolean between(float p1, float p2, float x) {
		return ((x > p2 && x < p1) || (x > p1 && x < p2));
	}

	public static boolean isThePoint(float x, float y, float x0, float y0, float tolerateDist) {
		float dx = x - x0;
		if (dx > tolerateDist)
			return false;
		float dy = y - y0;
		if (dy > tolerateDist)
			return false;
		return dx * dx + dy * dy <= tolerateDist * tolerateDist;
	}

	/**
	* Determines the bounding rectangle of the geometry. Returns an array of 4
	* floats: 1) minimum x; 2) minimum y; 3) maximum x; 4) maximum y.
	* Uses the same static array for all instances!
	*/
	public abstract float[] getBoundRect();

	/**
	* Returns the width of the geometry (in real coordinates)
	*/
	public abstract float getWidth();

	/**
	* Returns the height of the geometry (in real coordinates)
	*/
	public abstract float getHeight();

}
