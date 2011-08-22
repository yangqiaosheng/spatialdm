package spade.vis.dmap;

import java.awt.Point;
import java.awt.Rectangle;

import spade.vis.geometry.RealPoint;
import spade.vis.geometry.RealRectangle;
import spade.vis.map.MapContext;

/**
* Used for drawing geographical objects on a map. Transforms real-world
* coordinates into screen coordinates and back.
* The MapMetrics may be used for drawing several maps ("small multiples") in
* a common viewport.
*/
public class MapMetrics implements MapContext {
	/**
	* Boundaries of the visible territory fragment
	*/
	protected RealRectangle visTerr = null;
	/**
	* Lower-left corner of the visible territory
	*/
	protected double x0 = 0.0f, y0 = 0.0f;
	/**
	* The scaling factor for transformations between real-world and screen
	* coordinates
	*/
	protected double step = 1.0f;
	/**
	 * Indicates whether the coordinates on the map are geographical,
	 * i.e. X is the longitude and Y is the latitude. By default false.
	 */
	protected boolean isGeographic = false;
	/**
	 * In case of geographic coordinates, the scaling factor may be different
	 * for X-coordinate (longitude) and Y-coordinate (latitude). This is
	 * a special scaling factor for the latitude. In case of non-geographic
	 * coordinates, it is equal to step.
	 */
	protected double stepY = 1.0f;
	/**
	 * For Mercator projection: projected minimum and maximum Y (latitude)
	 */
	protected double prLat1 = Double.NaN, prLat2 = Double.NaN;
	/**
	* The rectangular area on the screen in which the map is drawn
	*/
	protected Rectangle viewport = null;
	/**
	* Maximum Y coordinare on the screen
	*/
	protected int scrMaxY = 0;
	/**
	* The upper-left corner of the screen area in which the map is drawn.
	*/
	protected int vpx = 0, vpy = 0;
	/**
	* The MapMetrics may be used for drawing several maps ("small multiples") in
	* a common viewport. This variable indicates the number of individual maps
	* within the viewport. By default, it is 1.
	*/
	protected int mapCount = 1;
	/**
	* The MapMetrics may be used for drawing several maps ("small multiples") in
	* a common viewport. This variable indicates the currently drawn map.
	*/
	protected int currMapN = 0;
	/**
	* The width and hight of the individual maps shown in a common viewport.
	*/
	protected int mapW = 0, mapH = 0;
	/**
	* The space between the maps in a "small multiple", in pixels.
	*/
	protected int space = 5;

	/**
	 * Copies the values of all its fields to the given instance of MapMetrics
	 */
	public void copyTo(MapMetrics mmetr) {
		if (mmetr == null)
			return;
		if (visTerr != null) {
			mmetr.visTerr = (RealRectangle) visTerr.clone();
		}
		mmetr.x0 = x0;
		mmetr.y0 = y0;
		mmetr.step = step;
		mmetr.stepY = stepY;
		mmetr.isGeographic = isGeographic;
		if (viewport != null) {
			mmetr.viewport = (Rectangle) viewport.clone();
		}
		mmetr.scrMaxY = scrMaxY;
		mmetr.vpx = vpx;
		mmetr.vpy = vpy;
		mmetr.mapCount = mapCount;
		mmetr.space = space;
		mmetr.currMapN = currMapN;
		mmetr.mapW = mapW;
		mmetr.mapH = mapH;
		if (currOrg != null) {
			mmetr.currOrg = (Point) currOrg.clone();
		}
		mmetr.minPixelValue = minPixelValue;
		mmetr.changedByFit = changedByFit;
	}

	/**
	 * Informs whether the coordinates on the map are
	 * geographical, i.e. X is the longitude and Y is the latitude.
	 */
	public boolean isGeographic() {
		return isGeographic;
	}

	/**
	 * Sets whether the coordinates on the map must be treated
	 * as geographical, i.e. X is the longitude and Y is the latitude.
	 */
	public void setGeographic(boolean geographic) {
		isGeographic = geographic;
	}

	/**
	* The origin of the current map on the screen (upper-left corner)
	*/
	protected Point currOrg = new Point(0, 0);
// see min scale denominator as parameter in app-file
	public double minPixelValue = Float.NaN;
	public boolean changedByFit = true;

	protected void reset() {
		x0 = 0.0;
		y0 = 0.0;
		step = stepY = 1.0;
		scrMaxY = 0;
		vpx = vpy = 0;
	}

	public RealRectangle fitToMinPixelValue(RealRectangle rr) {
		if (rr == null)
			return null;
		if (viewport == null || Double.isNaN(minPixelValue))
			return rr;

		double newPixelValue = (rr.rx2 - rr.rx1) / viewport.width;

		if (newPixelValue > minPixelValue)
			return rr;

		RealRectangle newrr = new RealRectangle();
		newrr.rx1 = (float) ((rr.rx2 + rr.rx1) / 2 - viewport.width * minPixelValue / 2);
		newrr.rx2 = (float) ((rr.rx2 + rr.rx1) / 2 + viewport.width * minPixelValue / 2);
		newrr.ry1 = (float) ((rr.ry2 + rr.ry1) / 2 - viewport.height * minPixelValue / 2);
		newrr.ry2 = (float) ((rr.ry2 + rr.ry1) / 2 + viewport.height * minPixelValue / 2);

		return newrr;
	}

	/**
	* Sets the boundaries of the visible territory fragment
	*/

	@Override
	public void setVisibleTerritory(RealRectangle r) {
		changedByFit = true;
		RealRectangle rr = fitToMinPixelValue(r);
		if (rr == null) {
			visTerr = null;
			reset();
		}

		else {
			if (visTerr == null) {
				visTerr = (RealRectangle) rr.clone();
			} else {
				if (visTerr.toString().equals(rr.toString())) {
					changedByFit = false;
				}
				visTerr.rx1 = rr.rx1;
				visTerr.ry1 = rr.ry1;
				visTerr.rx2 = rr.rx2;
				visTerr.ry2 = rr.ry2;
			}
			setup();
		}

	}

	/**
	* Returns the boundaries of the visible territory fragment
	*/
	@Override
	public RealRectangle getVisibleTerritory() {
		if (viewport != null)
			return new RealRectangle(absX(0), absY(viewport.height), absX(viewport.width), absY(0));
		return visTerr;
	}

	/**
	* Sets the rectangular area on the screen in which the map is drawn
	*/
	@Override
	public void setViewportBounds(int x, int y, int width, int height) {

		if (viewport == null) {
			viewport = new Rectangle(x, y, width, height);
		} else {
			viewport.x = x;
			viewport.y = y;
			viewport.width = width;
			viewport.height = height;
			RealRectangle rr = fitToMinPixelValue(visTerr);
			if (!rr.equals(visTerr)) {
				visTerr.rx1 = rr.rx1;
				visTerr.ry1 = rr.ry1;
				visTerr.rx2 = rr.rx2;
				visTerr.ry2 = rr.ry2;
			}
		}
		vpx = x;
		vpy = y;
		setup();
	}

	/**
	* Sets the rectangular area on the screen in which the map is drawn
	*/
	@Override
	public void setViewportBounds(Rectangle r) {
		if (r == null) {
			viewport = null;
			reset();
		} else {
			setViewportBounds(r.x, r.y, r.width, r.height);
		}
	}

	/**
	* Returns the rectangular area on the screen in which the map is drawn
	*/
	@Override
	public Rectangle getViewportBounds() {
		return viewport;
	}

	/**
	* Tells the MapMetrics how many maps must be drawn within one viewport.
	*/
	@Override
	public void setMapCount(int nMaps) {
		if (nMaps < 1 || nMaps == mapCount)
			return;
		mapCount = nMaps;
		currOrg = null;
		setup();
	}

	/**
	* Returns the number of maps drawn within one viewport
	*/
	@Override
	public int getMapCount() {
		return mapCount;
	}

	/**
	* Tells the MapMetrics which of the multiple maps will be drawn next.
	*/
	@Override
	public void setCurrentMapN(int mapN) {
		if (currMapN != mapN) {
			currMapN = mapN;
			currOrg = getMapOrigin(mapN);
		}
	}

	public static final double degInRad = 180 / Math.PI, radInDeg = Math.PI / 180, pi_4 = Math.PI / 4, pi_2 = Math.PI / 2;

	/**
	 * Transformation from degrees latitude to the Mercator projection
	 */
	public static double lat2Y(double lat) {
		if (lat < -85.051) {
			lat = -85.051;
		} else if (lat > 85.051) {
			lat = 85.051;
		}
		return degInRad * Math.log(Math.tan(pi_4 + lat * radInDeg / 2));
	}

	/**
	 * Transformation from the Mercator projection to degrees latitude
	 */
	public static double y2Lat(double y) {
		return degInRad * (2 * Math.atan(Math.exp(y * radInDeg)) - pi_2);
	}

	/**
	* Depending on the current territory extent and the screen area where the
	* map is drawn sets its internal variables used for coordinate transformation.
	*/
	public void setup() {
		if (visTerr == null || viewport == null) {
			reset();
			return;
		}
		mapW = viewport.width;
		mapH = viewport.height;
		double rx1 = visTerr.rx1, rx2 = visTerr.rx2, ry1 = visTerr.ry1, ry2 = visTerr.ry2;
		//double distX=1, distY=1;
		if (isGeographic) { //transform to Mercator projection
			if (ry1 < -85.051) {
				ry1 = -85.051;
			} else if (ry2 > 85.051) {
				ry1 = 85.051;
			}
			double dy = ry2 - ry1;
			ry1 = lat2Y(ry1);
			ry2 = lat2Y(ry2);
		}
		prLat1 = ry1;
		prLat2 = ry2;
/*
    if (isGeographic && ry2-ry1<30) {
      double x0=(rx1+rx2)/2, y0=(ry1+ry2)/2;
      //how many meters are in one degree longitude and latitude?
      distX= GeoDistance.geoDist(x0,y0,x0+1,y0);
      distY= GeoDistance.geoDist(x0,y0,x0,y0+1);
      //transform degrees into meters
      rx1*=distX; rx2*=distX; ry1*=distY; ry2*=distY;
    }
*/
		if (mapCount > 1) {
			int nrows = 1, ncols = mapCount, w = (viewport.width - (ncols - 1) * space) / ncols, h = (viewport.height - (nrows - 1) * space) / nrows;
			double stepx = (rx2 - rx1) / w, stepy = (ry2 - ry1) / h, stepxy = Math.max(stepx, stepy);
			while (true) {
				++nrows;
				ncols = (int) Math.ceil(1.0 * mapCount / nrows);
				int w1 = (viewport.width - (ncols - 1) * space) / ncols, h1 = (viewport.height - (nrows - 1) * space) / nrows;
				double stepx1 = (rx2 - rx1) / w1, stepy1 = (ry2 - ry1) / h1, stepxy1 = Math.max(stepx1, stepy1);
				if (stepxy1 > stepxy) {
					break;
				}
				w = w1;
				h = h1;
				stepxy = stepxy1;
			}
			mapW = w;
			mapH = h;
		}
		double stepx = (rx2 - rx1) / mapW, stepy = (ry2 - ry1) / mapH;
/*
    step=Math.max(stepx,stepy)/distX;
    stepY=Math.max(stepx,stepy)/distY;
*/
		step = stepY = Math.max(stepx, stepy);

		x0 = visTerr.rx1;
		y0 = visTerr.ry1;
		scrMaxY = (int) Math.ceil((prLat2 - prLat1) / stepY);
		if (currMapN >= mapCount) {
			currMapN = 0;
		}
		currOrg = getMapOrigin(currMapN);
	}

	/**
	* The scaling factor for transformations between real-world and screen
	* coordinates
	*/
	public double getStep() {
		return step;
	}

	/**
	 * In case of geographic coordinates, the scaling factor may be different
	 * for X-coordinate (longitude) and Y-coordinate (latitude). This is
	 * a special scaling factor for the latitude. In case of non-geographic
	 * coordinates, it is equal to step.
	 */
	public double getStepY() {
		return stepY;
	}

	/**
	* Returns the screen X coordinate for the given real-world point
	*/
	@Override
	public int scrX(float x, float y) {
		return currOrg.x + (int) Math.round((x - x0) / step);
	}

	/**
	* Returns the screen Y coordinate for the given real-world point
	*/
	@Override
	public int scrY(float x, float y) {
		if (!isGeographic)
			return currOrg.y + scrMaxY - (int) Math.round((y - y0) / stepY);
		double yy = lat2Y(y);
		return currOrg.y + scrMaxY - (int) Math.round((yy - prLat1) / stepY);
	}

	/**
	* Returns the real-world X coordinate for the given screen X coordinate
	*/
	@Override
	public float absX(int x) {
		return (float) (x0 + step * ((x - vpx) % (mapW + space)));
	}

	/**
	* Returns the real-world Y coordinate for the given screen Y coordinate
	*/
	@Override
	public float absY(int y) {
		if (!isGeographic)
			return (float) (y0 + stepY * (scrMaxY - (y - vpy) % (mapH + space)));
		double yy = prLat1 + stepY * (scrMaxY - (y - vpy) % (mapH + space));
		return (float) y2Lat(yy);
	}

	/**
	 * Returns the screen rectangle corresponding to the given territory bounds
	 */
	@Override
	public Rectangle getScreenRectangle(float x1, float y1, float x2, float y2) {
		int sx1 = scrX(x1, y1), sy1 = scrY(x1, y1), sx2 = scrX(x2, y2), sy2 = scrY(x2, y2);
		return new Rectangle(sx1, sy2, sx2 - sx1, sy1 - sy2);
	}

	/**
	* Transforms real-world coordinates into screen coordinates
	*/
	@Override
	public Point getScreenPoint(RealPoint rp) {
		return new Point(scrX(rp.x, rp.y), scrY(rp.x, rp.y));
	}

	/**
	* Transforms screen coordinates into real-world coordinates
	*/
	@Override
	public RealPoint getRealPoint(Point sp) {
		RealPoint rp = new RealPoint();
		rp.x = absX(sp.x);
		rp.y = absY(sp.y);
		return rp;
	}

	/**
	* Returns the scaling factor for transformations between real-world and screen
	* coordinates (i.e. the real-world distance represented by one screen pixel).
	*/
	@Override
	public float getPixelValue() {
		return (float) step;
	}

	/**
	* Returns the area occupied by the map with the given index (in "small
	* multiples")
	*/
	@Override
	public Rectangle getMapBounds(int idx) {
		if (mapCount <= 1 || mapW <= 0 || mapH <= 0)
			return getViewportBounds();
		Point p = getMapOrigin(idx);
		return new Rectangle(p.x, p.y, mapW, mapH);
	}

	/**
	* Returns the coordinates of the origin (upper-left corner) of the map with
	* the given index on the screen.
	*/
	public Point getMapOrigin(int idx) {
		Point p = new Point(vpx, vpy);
		if (mapCount <= 1 || mapW <= 0 || mapH <= 0 || idx == 0 || viewport == null)
			return p;
		int maxX = vpx + viewport.width;
		for (int i = 0; i < idx; i++) {
			p.x += mapW + space;
			if (p.x > maxX) {
				p.x = vpx;
				p.y += mapH + space;
			}
		}
		return p;
	}

	/**
	* Returns the index of the map containing the given point
	*/
	public int getMapNWithPoint(int x, int y) {
		if (viewport != null && !viewport.contains(x, y))
			return -1;
		if (mapCount <= 1 || mapW <= 0 || mapH <= 0 || viewport == null)
			return 0;
		int nCols = Math.round(1.0f * viewport.width / mapW);
		int colN = x / mapW, rowN = y / mapH;
		return rowN * nCols + colN;
	}
}
