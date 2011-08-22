package spade.vis.map;

import java.awt.Point;
import java.awt.Rectangle;

import spade.vis.geometry.RealPoint;
import spade.vis.geometry.RealRectangle;

/**
* Used for drawing geographical objects on a map. Transforms real-world
* coordinates into screen coordinates and back.
* Some MapContexts may allow drawing several maps ("small multiples") in
* a common viewport.
*/
public interface MapContext {
	/**
	* Sets the rectangular area on the screen in which the map is drawn
	*/
	public void setViewportBounds(Rectangle r);

	/**
	* Sets the rectangular area on the screen in which the map is drawn
	*/
	public void setViewportBounds(int x, int y, int width, int height);

	/**
	* Returns the rectangular area on the screen in which the map is drawn
	*/
	public Rectangle getViewportBounds();

	/**
	* Sets the boundaries of the visible territory fragment
	*/
	public void setVisibleTerritory(RealRectangle rr);

	/**
	* Returns the boundaries of the visible territory fragment
	*/
	public RealRectangle getVisibleTerritory();

	/**
	* Transforms real-world coordinates into screen coordinates
	*/
	public Point getScreenPoint(RealPoint rp);

	/**
	* Transforms screen coordinates into real-world coordinates
	*/
	public RealPoint getRealPoint(Point sp);

	/**
	* Returns the screen X coordinate for the given real-world point
	*/
	public int scrX(float x, float y);

	/**
	* Returns the screen Y coordinate for the given real-world point
	*/
	public int scrY(float x, float y);

	/**
	 * Returns the screen rectangle corresponding to the given territory bounds
	 */
	public Rectangle getScreenRectangle(float x1, float y1, float x2, float y2);

	/**
	* Returns the real-world X coordinate for the given screen X coordinate
	*/
	public float absX(int x);

	/**
	* Returns the real-world Y coordinate for the given screen Y coordinate
	*/
	public float absY(int y);

	/**
	* Returns the scaling factor for transformations between real-world and screen
	* coordinates (i.e. the real-world distance represented by one screen pixel).
	*/
	public float getPixelValue();

	/**
	* Tells the MapContext how many maps must be drawn within one viewport.
	*/
	public void setMapCount(int nMaps);

	/**
	* Returns the number of maps drawn within one viewport
	*/
	public int getMapCount();

	/**
	* Tells the MapContext which of the multiple maps will be drawn next.
	*/
	public void setCurrentMapN(int mapN);

	/**
	* Returns the area occupied by the map with the given index (in "small
	* multiples")
	*/
	public Rectangle getMapBounds(int idx);
}
