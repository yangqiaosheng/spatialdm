package spade.vis.dmap;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Vector;

import spade.lib.util.CopyFile;
import spade.lib.util.GeoDistance;
import spade.lib.util.IntArray;
import spade.lib.util.SleepThread;
import spade.lib.util.StringUtil;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.RealRectangle;
import spade.vis.map.MapContext;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Aug 19, 2008
 * Time: 3:04:01 PM
 * Loads and displays tile maps from Open Street Map
 */
public class DOSMLayer extends DGeoLayer {
	/**
	 * The fixed dimension of an OSM map tile
	 */
	public static Dimension tileSize = new Dimension(256, 256);
	/**
	 * The extension of a file with a map tile; by default, ".png"
	 */
	public static String tileFileExtension = ".png";
	/**
	 * The base URL of the OSM server providing the tiles
	 */
	public static String urlPrefixOSM = "http://tile.openstreetmap.org";
	/**
	 * The base URL of the Google maps server providing the tiles
	 */
	public static String urlPrefixGM = "http://maps.google.com/staticmap?";
	/**
	 * The current zoom level, according to http://wiki.openstreetmap.org/index.php/FAQ
	 * Ranges from 1 (lowest resolution) to 18 (highest resolution).
	 * When the zoom level changes, a different set of tiles must be loaded
	 * from the OpenStreetMap server.
	 */
	protected int currZoomLevel = 0; //invalid number; means undefined scale
	/**
	 * The OSM tiles currently available in the memory.
	 * The tiles organized in rows. Each row is, in turn, a vector of tiles.
	 * All vectors must have the same length.
	 * Each tile is an image. Null means that the tile has not been loaded yet.
	 */
	protected Vector tileRows = null;
	/**
	 * The OSM numbers of the leftmost and rightmost OSM tiles
	 * available in tileRows
	 */
	protected int leftTileN = -1, rightTileN = -1;
	/**
	 * The numbers of the top and bottom OSM tiles
	 * available in tileRows
	 */
	protected int topTileN = -1, bottomTileN = -1;
	/**
	 * The OSM numbers of the leftmost and rightmost OSM tiles
	 * for the current map scale and extent
	 */
	protected int currLeftN = -1, currRightN = -1;
	/**
	 * The numbers of the top and bottom OSM tiles
	 * for the current map scale and extent
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
	protected LoadTilesThread tilesLoader = null;
	/**
	 * Contains the zoom levels for which no tiles could be loaded
	 * from the server
	 */
	private IntArray failedZoomLevels = null;
	/**
	 * Allows to renew attempts to load map tiles from the server
	 * after some time has passed since the failure.
	 */
	private SleepThread sleepThread = null;

	public static int source_OpenStreetMaps = 0, source_GoogleMaps = 1;

	/**
	 * Indicates whether to load tiles from the Open Street Maps or Google Maps
	 */
	protected int mapSource = source_OpenStreetMaps;
	/**
	 * For GoogleMaps, indicates the desired map type (e.g. hybrid or terrain)
	 */
	protected String mapType = "hybrid";
	/**
	 * Map tiles may be stored in a local file system.
	 * If there are such files, the following field contains the path
	 * to the tile index file with the extension ".osm"
	 */
	protected String pathTilesIndex = null;
	/**
	 * The path to the top directory with the tiles (if there are tiles saved to files)
	 */
	protected String tileTopDirPath = null;
	/**
	 * If there are tiles saved to files, these are the lowest and highest zoom levels
	 * of these tiles
	 */
	protected int minZoomLevelFiles = 0, maxZoomLevelFiles = 0;

	public int getMapSource() {
		return mapSource;
	}

	public void setMapSource(int mapSource) {
		this.mapSource = mapSource;
	}

	public void setMapType(String mapType) {
		this.mapType = mapType;
	}

	@Override
	public char getType() {
		if (objType == Geometry.undefined) {
			objType = Geometry.image;
		}
		return objType;
	}

	/**
	 * Map tiles may be stored in a local file system.
	 * If there are such files, the following method returns the path
	 * to the tile index file with the extension ".osm"
	 */
	public String getPathTilesIndex() {
		return pathTilesIndex;
	}

	/**
	 * Map tiles may be stored in a local file system.
	 * If there are such files, the following method sets the path
	 * to the tile index file with the extension ".osm"
	 */
	public void setPathTilesIndex(String pathTilesIndex) {
		this.pathTilesIndex = pathTilesIndex;
		if (pathTilesIndex == null)
			return;
		FileReader fReader = null;
		BufferedReader reader = null;
		try {
			fReader = new FileReader(pathTilesIndex);
			reader = new BufferedReader(fReader);
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		if (reader == null)
			return;
		boolean stop = false;
		while (!stop) {
			try {
				String str = reader.readLine();
				if (str == null) {
					break;
				}
				if (str.startsWith("*")) {
					continue; //this is a comment
				}
				str = str.trim();
				if (str.length() < 1) {
					continue; //empty line
				}
				Vector tokens = StringUtil.getNames(str, "=");
				if (tokens == null || tokens.size() < 1) {
					continue;
				}
				String keyWord = ((String) tokens.elementAt(0)).trim().toUpperCase();
				String value = ((String) tokens.elementAt(1)).trim();
				if (keyWord.equals("DIRECTORY")) {
					tileTopDirPath = CopyFile.getDir(pathTilesIndex) + value;
				} else if (keyWord.equals("MIN_LEVEL")) {
					try {
						minZoomLevelFiles = Integer.parseInt(value);
					} catch (NumberFormatException nfe) {
					}
				} else if (keyWord.equals("MAX_LEVEL")) {
					try {
						maxZoomLevelFiles = Integer.parseInt(value);
					} catch (NumberFormatException nfe) {
					}
				}
			} catch (IOException ioe) {
				stop = true;
			}
		}
		try {
			fReader.close();
		} catch (IOException ioe) {
		}
	}

	public int getCurrZoomLevel() {
		return currZoomLevel;
	}

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
		if (rr == null)
			return;
		//find the scale...
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
		double horDist = GeoDistance.geoDist(rr.rx1, y, x, y);
		Rectangle r = mc.getMapBounds(0);
		double metersPerPixel = horDist / r.width;
		int zoomLevel = getZoomLevel(metersPerPixel);
		if (zoomLevel < 1 || zoomLevel > 18)
			return;
		if (zoomLevel != currZoomLevel) {
			if (tilesLoader != null) {
				tilesLoader.stopLoading();
				tilesLoader = null;
			}
			//another set of tiles must be loaded
			tileRows = null;
			leftTileN = rightTileN = topTileN = bottomTileN = -1;
			currLeftN = currRightN = currTopN = currBottomN = -1;
			wholeImage = null;
			currZoomLevel = zoomLevel;
		}
		int left = getTileNumberX(zoomLevel, rr.rx1), right = getTileNumberX(zoomLevel, rr.rx2);
		int top = getTileNumberY(zoomLevel, rr.ry2), bottom = getTileNumberY(zoomLevel, rr.ry1);
		if (tileRows != null && left == currLeftN && right == currRightN && top == currTopN && bottom == currBottomN) {
			drawWholeImageIfPossible(g, mc);
			return;
		}
		//allocate sufficient space for the tiles, if needed
		if (tileRows == null) {
			tileRows = new Vector(20, 20);
		}
		if (topTileN < 0) {
			for (int i = top; i <= bottom; i++) {
				tileRows.addElement(null);
			}
			topTileN = top;
			bottomTileN = bottom;
		} else {
			if (top < topTileN) {
				for (int i = top; i < topTileN; i++) {
					tileRows.insertElementAt(null, 0);
				}
				topTileN = top;
			}
			if (bottom > bottomTileN) {
				for (int i = bottomTileN + 1; i <= bottom; i++) {
					tileRows.addElement(null);
				}
				bottomTileN = bottom;
			}
		}
		for (int i = topTileN; i <= bottomTileN; i++) {
			Vector row = (Vector) tileRows.elementAt(i - topTileN);
			if (row == null || leftTileN < 0) {
				row = new Vector(20, 20);
				tileRows.setElementAt(row, i - topTileN);
				int min = (leftTileN >= 0) ? Math.min(left, leftTileN) : left;
				for (int j = min; j <= Math.max(right, rightTileN); j++) {
					row.addElement(null);
				}
			} else {
				if (left < leftTileN) {
					for (int j = left; j < leftTileN; j++) {
						row.insertElementAt(null, 0);
					}
				}
				if (right > rightTileN) {
					for (int j = rightTileN + 1; j <= right; j++) {
						row.addElement(null);
					}
				}
			}
		}
		if (leftTileN < 0 || left < leftTileN) {
			leftTileN = left;
		}
		if (rightTileN < 0 || right > rightTileN) {
			rightTileN = right;
		}
		currLeftN = left;
		currRightN = right;
		currTopN = top;
		currBottomN = bottom;
		if (tilesLoader != null) {
			tilesLoader.stopLoading();
			tilesLoader = null;
		}
		//check if all necessary tiles are available.
		boolean missing = false;
		for (int i = top; i <= bottom && !missing; i++) {
			Vector row = (Vector) tileRows.elementAt(i - topTileN);
			for (int j = left; j <= right && !missing; j++) {
				while (j - leftTileN >= row.size()) {
					row.addElement(null);
				}
				missing = row.elementAt(j - leftTileN) == null;
			}
		}
		//load the missing tiles.
		if (missing) {
			if (failedZoomLevels != null && failedZoomLevels.indexOf(currZoomLevel) >= 0)
				return;
			drawWholeImageIfPossible(g, mc);
			tilesLoader = new LoadTilesThread();
			tilesLoader.setTileFileExtension(tileFileExtension);
			tilesLoader.setMapSource(mapSource);
			tilesLoader.setMapType(mapType);
			tilesLoader.setTileServerURL((mapSource == source_OpenStreetMaps) ? urlPrefixOSM : urlPrefixGM);
			tilesLoader.setStatusListener(this);
			if (tileTopDirPath != null && currZoomLevel >= minZoomLevelFiles && currZoomLevel <= maxZoomLevelFiles) {
				tilesLoader.setTileTopDirPath(tileTopDirPath);
			}
			tilesLoader.loadTiles(tileRows, currZoomLevel, leftTileN, rightTileN, topTileN, bottomTileN, currLeftN, currRightN, currTopN, currBottomN);
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
					sleepThread = new SleepThread(this, 1000L);
					sleepThread.start();
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
		int x1 = mc.scrX(imageTerrBounds.rx1, imageTerrBounds.ry1), y1 = mc.scrY(imageTerrBounds.rx1, imageTerrBounds.ry1), x2 = mc.scrX(imageTerrBounds.rx2, imageTerrBounds.ry2), y2 = mc.scrY(imageTerrBounds.rx2, imageTerrBounds.ry2);
		g.drawImage(wholeImage, x1, y2, x2 - x1, y1 - y2, this);
	}

	/**
	 * If any of the necessary tiles are currently available in the memory,
	 * constructs an image containing these tiles.
	 */
	protected BufferedImage makeWholeImageIfPossible() {
		boolean hasTile = false;
		for (int i = currTopN; i <= currBottomN && !hasTile; i++) {
			Vector row = (Vector) tileRows.elementAt(i - topTileN);
			if (row == null) {
				continue;
			}
			for (int j = currLeftN; j <= currRightN && !hasTile; j++) {
				Image img = (Image) row.elementAt(j - leftTileN);
				hasTile = img != null && img.getWidth(null) >= tileSize.width && img.getHeight(null) >= tileSize.height;
			}
		}
		if (!hasTile)
			return null;
		if (imageTerrBounds == null) {
			imageTerrBounds = new RealRectangle();
		}
		imageTerrBounds.rx1 = (float) getTileLongitude(currZoomLevel, currLeftN);
		imageTerrBounds.rx2 = (float) getTileLongitude(currZoomLevel, currRightN + 1);
		imageTerrBounds.ry2 = (float) getTileLatitude(currZoomLevel, currTopN);
		imageTerrBounds.ry1 = (float) getTileLatitude(currZoomLevel, currBottomN + 1);
		int w = (currRightN - currLeftN + 1) * tileSize.width, h = (currBottomN - currTopN + 1) * tileSize.height;
		BufferedImage bigImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		Graphics ig = bigImage.getGraphics();
		ig.setColor(Color.lightGray);
		ig.fillRect(0, 0, w + 1, h + 1);
		for (int i = currTopN; i <= currBottomN; i++) {
			Vector row = (Vector) tileRows.elementAt(i - topTileN);
			if (row == null) {
				continue;
			}
			for (int j = currLeftN; j <= currRightN; j++) {
				Image img = (Image) row.elementAt(j - leftTileN);
				if (img == null || img.getWidth(null) < tileSize.width || img.getHeight(null) < tileSize.height) {
					continue;
				}
				ig.drawImage(img, (j - currLeftN) * tileSize.width, (i - currTopN) * tileSize.height, null);
			}
		}
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

	public Image getTile(int rowIdx, int columnIdx) {
		if (tileRows == null || tileRows.size() < 1 || leftTileN < 0 || rightTileN < 0 || topTileN < 0 || bottomTileN < 0)
			return null;
		int rowN = rowIdx - topTileN;
		if (rowN < 0 || rowN >= tileRows.size())
			return null;
		Vector row = (Vector) tileRows.elementAt(rowN);
		if (row == null || row.size() < 1)
			return null;
		int colN = columnIdx - leftTileN;
		if (colN < 0 || colN >= row.size())
			return null;
		return (Image) row.elementAt(colN);
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
	 * Returns the appropriate zoom level, according to the OSM conventions,
	 * from 1 (lowest resolution) to 18 (highest resolution).
	 * See http://wiki.openstreetmap.org/index.php/FAQ#What_is_the_map_scale_for_a_particular_zoom_level_of_the_map.3F
	 */
	protected int getZoomLevel(double metersPerPixel) {
		if (Double.isNaN(metersPerPixel))
			return 0; //error
		if (metersPerPixel <= 0.6)
			return 18;
		if (metersPerPixel <= 1.25)
			return 17;
		if (metersPerPixel <= 2.5)
			return 16;
		if (metersPerPixel <= 5)
			return 15;
		if (metersPerPixel <= 10)
			return 14;
		if (metersPerPixel <= 20)
			return 13;
		if (metersPerPixel <= 40)
			return 12;
		if (metersPerPixel <= 80)
			return 11;
		if (metersPerPixel <= 160)
			return 10;
		if (metersPerPixel <= 320)
			return 9;
		if (metersPerPixel <= 640)
			return 8;
		if (metersPerPixel <= 1250)
			return 7;
		if (metersPerPixel <= 2500)
			return 6;
		if (metersPerPixel <= 5000)
			return 5;
		if (metersPerPixel <= 10000)
			return 4;
		if (metersPerPixel <= 20000)
			return 3;
		if (metersPerPixel <= 40000)
			return 2;
		return 1;
	}

	/**
	 * Returns the OSM tile number in the longitude direction.
	 * See http://wiki.openstreetmap.org/wiki/Slippy_map_tilenames
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
	 * Returns the OSM tile number in the latitude direction.
	 * See http://wiki.openstreetmap.org/wiki/Slippy_map_tilenames
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
}
