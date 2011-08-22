/**
 * 
 */
package spade.vis.dmap;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.util.Vector;

import spade.lib.util.GeoDistance;
import spade.lib.util.IntArray;
import spade.lib.util.SleepThread;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.RealRectangle;
import spade.vis.map.MapContext;

/**
 * @author Christian Class to load and display google maps in a static manner
 *         This is done by downloading a picture from the http server of google
 *         via an information coded url containing the position, extension and
 *         zoom level For instance:
 *         http://maps.goggle.com/staticmap?center=c_x,c_y
 *         &zoom=n&size=XxY&format=png with center meaning the central point of
 *         the map geographic weidth and length zoom as an number from 0 to 19
 *         and size for the extension of the picture in pixel
 * 
 */
public class GMLayer extends DGeoLayer {

	/**
	 * The fixed dimension of an OSM map tile
	 */
	public static Dimension tileSize = new Dimension(256, 256);
	/**
	 * The extension of a file with a map tile; by default, ".png"
	 */
	public static String tileFileExtension = ".png";
	/**
	 * The base URL of the server providing the tiles
	 */
	public static String urlPrefix = "http://maps.google.com/staticmap?";
	/**
	 * The current zoom level, according to
	 * http://wiki.openstreetmap.org/index.php/FAQ Ranges from 1 (lowest
	 * resolution) to 18 (highest resolution). When the zoom level changes, a
	 * different set of tiles must be loaded from the OpenStreetMap server.
	 */
	protected int currZoomLevel = 0; // invalid number; means undefined scale
	/**
	 * The OSM tiles currently available in the memory. The tiles organized in
	 * rows. Each row is, in turn, a vector of tiles. All vectors must have the
	 * same length. Each tile is an image. Null means that the tile has not been
	 * loaded yet.
	 */
	protected Vector tileRows = null;
	/**
	 * The OSM numbers of the leftmost and rightmost OSM tiles available in
	 * tileRows
	 */
	protected int leftTileN = -1, rightTileN = -1;
	/**
	 * The numbers of the top and bottom OSM tiles available in tileRows
	 */
	protected int topTileN = -1, bottomTileN = -1;
	/**
	 * The OSM numbers of the leftmost and rightmost OSM tiles for the current
	 * map scale and extent
	 */
	protected int currLeftN = -1, currRightN = -1;
	/**
	 * The numbers of the top and bottom OSM tiles for the current map scale and
	 * extent
	 */
	protected int currTopN = -1, currBottomN = -1;
	/**
	 * The image combining all tiles for the currently visible map extent.
	 */
	protected BufferedImage wholeImage = null;
	/**
	 * The extent of the territory represented by the image
	 */
	protected RealRectangle imageTerrBounds = null;
	/**
	 * Used for loading tiles in the background mode
	 */
	protected LoadGoogleMapThread tilesLoader = null;
	/**
	 * Contains the zoom levels for which no tiles could be loaded from the
	 * server
	 */
	private IntArray failedZoomLevels = null;
	/**
	 * Allows to renew attempts to load map tiles from the server after some
	 * time has passed since the failure.
	 */
	private SleepThread sleepThread = null;

	@Override
	public char getType() {
		if (objType == Geometry.undefined) {
			objType = Geometry.image;
		}
		return objType;
	}

	float currentX, currentY;
	Rectangle currentRec = null;
	int height = 0;
	int width = 0;
	int last_height = 0, last_width = 0;

	@Override
	public void draw(Graphics g, MapContext mc) {
		if (g == null || mc == null)
			return;
		if (!isGeographic)
			return;
		if (!dynamicLoadingAllowed) {
			drawWholeImageIfPossible(g, mc);
			return;
		}
		RealRectangle rr = mc.getVisibleTerritory();
		currentRec = mc.getMapBounds(1);
		if (rr == null)
			return;
		// find the scale...
		if (rr.ry1 < -90) {
			rr.ry1 = -90;
		}
		if (rr.ry2 > 90) {
			rr.ry2 = 90;
		}
		if (rr.rx1 < -180) {
			rr.rx1 = -180;
		}
		if (rr.rx2 > 180) {
			rr.rx2 = 180;
		}
		float y = (rr.ry1 + rr.ry2) / 2;
		float x = rr.rx2;
		if (x - rr.rx1 > 180) {
			x = rr.rx1 + 180;
		}
		x = (rr.rx1 + rr.rx2) / 2;
		// the length and width of the geospatial window
		double horDist = GeoDistance.geoDist(rr.rx1, y, rr.rx2, y);
		double verDist = GeoDistance.geoDist(x, rr.ry1, x, rr.ry2);
		double ref = 0.0;
		if (horDist > verDist) {
			ref = horDist;
		} else {
			ref = verDist;
		}
		int z = 0;
		double p = 0;
		for (z = 0; z < 20; z++) {
			if ((p = (ref / (Math.pow(2, 17 - z)))) > 640) {
				break;
			}
		}
		z--;
		p = ((ref) / (Math.pow(2, 17 - z)));

		int loc_height = (int) Math.round(p * horDist / (ref) + 8 * (20 - z));

		int loc_width = (int) Math.round(p * verDist / (ref) + 7 * (20 - z));

		Rectangle r = mc.getMapBounds(1);
		double metersPerPixel = horDist / r.width;
		double metersPerPixel2 = verDist / r.height;

		int zoomLevel = getZoomLevel(metersPerPixel);
		boolean load = false;
		if (z < 0 || z >= 19)
			return;
		if (z != currZoomLevel || loc_width != last_width || loc_height != last_height) {
			if (tilesLoader != null) {
				tilesLoader.stopLoading();
				tilesLoader = null;
			}
			// another picture must be loaded
			tileRows = null;
			leftTileN = rightTileN = topTileN = bottomTileN = -1;
			currLeftN = currRightN = currTopN = currBottomN = -1;
			wholeImage = null;
			currZoomLevel = z;
			load = true;
		}
		last_height = loc_height;
		last_width = loc_width;
		int left = getTileNumberX(zoomLevel, rr.rx1), right = getTileNumberX(zoomLevel, rr.rx2);
		int top = getTileNumberY(zoomLevel, rr.ry2), bottom = getTileNumberY(zoomLevel, rr.ry1);
		width = (Math.abs(right - left)) * tileSize.width;
		height = (Math.abs(top - bottom)) * tileSize.height;

		if (tileRows != null && left == currLeftN && right == currRightN && top == currTopN && bottom == currBottomN) {
			drawWholeImageIfPossible(g, mc);
			return;
		}
		// allocate sufficient space for the tiles, if needed
		if (tileRows == null) {
			tileRows = new Vector(1, 1);
			Vector tmp = new Vector(1, 1);
			tmp.add(null);
			tileRows.addElement(tmp);
			topTileN = top;
			bottomTileN = bottom;
			leftTileN = left;
			rightTileN = right;
			load = true;
		} else {
			if (topTileN > top || bottomTileN > bottom || leftTileN < left || rightTileN > right) {
				tileRows = null;
				tileRows = new Vector(1, 1);
				Vector tmp = new Vector(1, 1);
				tmp.add(null);
				tileRows.addElement(tmp);
				topTileN = top;
				bottomTileN = bottom;
				leftTileN = left;
				rightTileN = right;
				wholeImage = null;
				load = true;
			}
		}

		if (tilesLoader != null) {
			tilesLoader.stopLoading();
			tilesLoader = null;
		}

		// load the missing tiles.
		if (load) {
			if (failedZoomLevels != null && failedZoomLevels.indexOf(currZoomLevel) >= 0)
				return;
			drawWholeImageIfPossible(g, mc);
			tilesLoader = new LoadGoogleMapThread();
			tilesLoader.setTileFileExtension(tileFileExtension);
			tilesLoader.setTileServerURL(urlPrefix);
			tilesLoader.setStatusListener(this);
			tilesLoader.loadTiles(tileRows, currZoomLevel, x, y, loc_width, loc_height);
		} else {
			wholeImage = null;
			drawWholeImageIfPossible(g, mc);
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent pce) {
		if (pce.getSource().equals(tilesLoader)) {
			String propName = pce.getPropertyName();
			if (propName.equals("error") || propName.equals("status")) {
				notifyPropertyChange(propName, pce.getOldValue(), pce.getNewValue());
				return;
			}
			if (propName.equals("tiles_ready")) {
				tilesLoader = null;
				notifyPropertyChange("status", null, null);
				notifyPropertyChange("tiles_ready", null, null);
			} else if (propName.equals("failure")) {
				tilesLoader = null;
				if (failedZoomLevels == null) {
					failedZoomLevels = new IntArray(10, 10);
				}
				if (failedZoomLevels.indexOf(currZoomLevel) < 0) {
					failedZoomLevels.addElement(currZoomLevel);
					if (sleepThread != null) {
						sleepThread.setMustStop();
					}
					sleepThread = new SleepThread(this, 120000L);
				}
				notifyPropertyChange(propName, null, "Failed to load a map image from Open Street Map!");
			}
			if (propName.equals("tiles_ready") || propName.equals("tiles_loaded")) {
				wholeImage = null;
				notifyPropertyChange("ImageState", null, null);
			}
		} else if (pce.getSource().equals(sleepThread)) {
			sleepThread = null;
			if (failedZoomLevels != null) {
				failedZoomLevels.removeAllElements();
			}
		} else {
			super.propertyChange(pce);
		}
	}

	public boolean hasWholeImage() {
		return wholeImage != null;
	}

	protected void drawWholeImageIfPossible(Graphics g, MapContext mc) {
		if (wholeImage == null) {
			wholeImage = makeWholeImageIfPossible();
		}
		if (wholeImage == null)
			return;
		int w = currentRec.width, h = currentRec.height;
		g.drawImage(wholeImage, 0, 0, w, h, this);
	}

	/**
	 * If any of the necessary tiles are currently available in the memory,
	 * constructs an image containing these tiles.
	 */
	protected BufferedImage makeWholeImageIfPossible() {

		int w = currentRec.width, h = currentRec.height;
		BufferedImage bigImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		Graphics ig = bigImage.getGraphics();
		ig.setColor(Color.lightGray);
		ig.fillRect(0, 0, w + 1, h + 1);

		Vector row = (Vector) tileRows.elementAt(0);
		if (row == null)
			return null;
		if (row.size() < 1)
			return null;
		Image img = (Image) row.elementAt(0);
		if (img == null)
			return null;
		ig.drawImage(img, 0, 0, w, h, null);
		ig.dispose();
		return bigImage;
	}

	public Dimension getImageSize() {
		if (wholeImage == null)
			return null;
		return new Dimension(wholeImage.getWidth(), wholeImage.getHeight());
	}

	public RealRectangle getImageExtent() {
		return imageTerrBounds;
	}

	@Override
	public void destroy() {
		if (tilesLoader != null) {
			tilesLoader.stopLoading();
			tilesLoader = null;
		}
		super.destroy();
	}

	/**
	 * Returns the appropriate zoom level from 1 (lowest resolution) to 19
	 * (highest resolution).
	 */
	protected int getZoomLevel(double metersPerPixel) {
		if (true) {
			// return (int)Math.round(17 +
			// (Math.log(metersPerPixel)/Math.log(2)));
		}
		if (Double.isNaN(metersPerPixel))
			return 0; // error
		if (metersPerPixel <= 0.298)
			return 19;
		if (metersPerPixel <= 0.5969)
			return 18;
		if (metersPerPixel <= 1.1937)
			return 17;
		if (metersPerPixel <= 2.3875)
			return 16;
		if (metersPerPixel <= 4.775)
			return 15;
		if (metersPerPixel <= 9.55)
			return 14;
		if (metersPerPixel <= 19.1)
			return 13;
		if (metersPerPixel <= 38.2)
			return 12;
		if (metersPerPixel <= 76.4)
			return 11;
		if (metersPerPixel <= 152.8)
			return 10;
		if (metersPerPixel <= 305.6)
			return 9;
		if (metersPerPixel <= 611.2)
			return 8;
		if (metersPerPixel <= 1222.4)
			return 7;
		if (metersPerPixel <= 2444.8)
			return 6;
		if (metersPerPixel <= 4889.6)
			return 5;
		if (metersPerPixel <= 9779.2)
			return 4;
		if (metersPerPixel <= 19558.4)
			return 3;
		if (metersPerPixel <= 39116.8)
			return 2;
		if (metersPerPixel <= 78233.6)
			return 1;
		return 0;
	}

	/**
	 * Returns the longitude of the upper left corner of the tile with the given
	 * horizontal number under the given zoom level
	 */
	public static double getTileLongitude(int zoomLevel, int tileXNumber) {
		return tileXNumber / Math.pow(2, zoomLevel) * 360 - 180;
	}

	/**
	 * Returns the latitude of the upper left corner of the tile with the given
	 * vertical number under the given zoom level
	 */
	public static double getTileLatitude(int zoomLevel, int tileYNumber) {
		double n = Math.PI - 2 * Math.PI * tileYNumber / Math.pow(2, zoomLevel);
		return 180 / Math.PI * Math.atan(0.5 * (Math.exp(n) - Math.exp(-n)));
	}

	/**
	 * Returns the OSM tile number in the longitude direction. See
	 * http://wiki.openstreetmap.org/wiki/Slippy_map_tilenames
	 */
	public static int getTileNumberX(int zoomLevel, double lon) {
		// see http://wiki.openstreetmap.org/wiki/Slippy_map_tilenames
		int n = (int) Math.round(Math.pow(2, zoomLevel));
		int level = (int) Math.floor((lon + 180) / 360 * n);
		if (level < 0)
			return 0;
		if (level > n - 1)
			return n - 1;
		return level;
	}

	/**
	 * Returns the OSM tile number in the latitude direction. See
	 * http://wiki.openstreetmap.org/wiki/Slippy_map_tilenames
	 */
	public static int getTileNumberY(int zoomLevel, double lat) {
		// see http://wiki.openstreetmap.org/wiki/Slippy_map_tilenames
		if (lat >= 85.0511)
			return 0;
		int n = (int) Math.round(Math.pow(2, zoomLevel));
		if (lat <= -85.0511)
			return n - 1;
		double lat_rad = lat * Math.PI / 180;
		int level = (int) Math.floor((1 - Math.log(Math.tan(lat_rad) + 1 / Math.cos(lat_rad)) / Math.PI) / 2 * n);
		if (level < 0)
			return 0;
		if (level > n - 1)
			return n - 1;
		return level;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
