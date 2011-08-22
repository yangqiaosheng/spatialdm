package spade.vis.dmap;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Vector;

import javax.imageio.ImageIO;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Aug 19, 2008
 * Time: 4:55:47 PM
 * Intended for loading tile maps from the OpenStreetMaps server
 */
public class LoadTilesThread extends Thread {
	/**
	 * Indicates whether to load tiles from the Open Street Maps or Google Maps
	 */
	protected int mapSource = DOSMLayer.source_OpenStreetMaps;
	/**
	 * For GoogleMaps, indicates the desired map type (e.g. hybrid or terrain)
	 */
	protected String mapType = "hybrid";
	/**
	 * The extension of a file with a map tile (e.g. ".png")
	 */
	protected String tileFileExtension = null;
	/**
	 * The fixed dimension of an Google Maps map tile
	 */
	public static Dimension tileSize = new Dimension(256, 256);
	/**
	 * The base URL of the server providing the tiles
	 */
	protected String urlPrefix = null;
	/**
	 * The path to the top directory with the tiles (if there are tiles saved to files)
	 */
	protected String tileTopDirPath = null;
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
	 * Listens to the messages from the thread, e.g. when all tiles have been loaded
	 */
	protected PropertyChangeListener listener = null;

	private boolean mustStop = false, running = false;

	public void setTileFileExtension(String fileExtension) {
		tileFileExtension = fileExtension;
	}

	public void setTileServerURL(String urlString) {
		urlPrefix = urlString;
	}

	public void setMapSource(int mapSource) {
		this.mapSource = mapSource;
	}

	public void setMapType(String mapType) {
		this.mapType = mapType;
	}

	public void setStatusListener(PropertyChangeListener listener) {
		this.listener = listener;
	}

	/**
	 * Sets the path to the top directory with the tiles (if there are tiles saved to files)
	 */
	public void setTileTopDirPath(String tileTopDirPath) {
		this.tileTopDirPath = tileTopDirPath;
	}

	public void loadTiles(Vector tileRows, int currZoomLevel, int leftTileN, int rightTileN, int topTileN, int bottomTileN, int currLeftN, int currRightN, int currTopN, int currBottomN) {
		this.tileRows = tileRows;
		this.currZoomLevel = currZoomLevel;
		this.leftTileN = leftTileN;
		this.rightTileN = rightTileN;
		this.topTileN = topTileN;
		this.bottomTileN = bottomTileN;
		this.currLeftN = currLeftN;
		this.currRightN = currRightN;
		this.currTopN = currTopN;
		this.currBottomN = currBottomN;
		start();
	}

	public Vector getTileRows() {
		return tileRows;
	}

	public int getLeftTileN() {
		return leftTileN;
	}

	public int getCurrZoomLevel() {
		return currZoomLevel;
	}

	public int getRightTileN() {
		return rightTileN;
	}

	public int getTopTileN() {
		return topTileN;
	}

	public int getBottomTileN() {
		return bottomTileN;
	}

	@Override
	public void run() {
		if (tileRows == null || leftTileN < 0 || topTileN < 0 || currLeftN < 0 || currTopN < 0 || tileFileExtension == null || urlPrefix == null)
			return;
		if (mustStop)
			return;
		boolean loaded = false, all = true;
		if (listener != null) {
			PropertyChangeEvent pce = new PropertyChangeEvent(this, "status", null, "Loading tiles; zoom level = " + currZoomLevel + "; rows from " + currTopN + " to " + currBottomN + "; columns from " + currLeftN + " to " + currRightN);
			listener.propertyChange(pce);
		}
		do {
			loaded = false;
			for (int i = currTopN; i <= currBottomN && !mustStop; i++) {
				int rowN = i - topTileN;
				Vector row = (Vector) tileRows.elementAt(rowN);
				if (row == null) {
					continue;
				}
				for (int j = currLeftN; j <= currRightN && !mustStop; j++) {
					int colN = j - leftTileN;
					if (row.elementAt(colN) == null) {
						//for Google Maps, try to get images of the size 512x512
						boolean xDoubled = false, yDoubled = false;
						String urlString = null;
						Image img = null;
						if (tileTopDirPath != null) {
							urlString = tileTopDirPath + "/" + currZoomLevel + "/" + j + "/" + i + tileFileExtension;
							File file = new File(urlString);
							if (!file.exists()) {
								urlString = null;
							} else {
								//System.out.println("Getting image from file "+urlString);
								try {
									img = ImageIO.read(file);
								} catch (IOException e) {
									img = null;
								}
							}
						}
						if (img == null) {
							if (mapSource == DOSMLayer.source_OpenStreetMaps) {
								urlString = urlPrefix + "/" + currZoomLevel + "/" + j + "/" + i + tileFileExtension;
							} else if (mapSource == DOSMLayer.source_GoogleMaps) {
/*
                double x1=getTileLongitude(currZoomLevel,j);
                double x2=(float)getTileLongitude(currZoomLevel,j+1);
                double y2=(float)getTileLatitude(currZoomLevel,i);
                double y1=(float)getTileLatitude(currZoomLevel,i+1);
                double x=(x1+x2)/2, y=(y1+y2)/2;
                urlString=urlPrefix+"" + "center="+y+","+x+"&zoom="+currZoomLevel+
                  "&size="+tileSize.width+"x"+tileSize.height+"&format=png&maptype="+mapType;
*/
								xDoubled = true;
								yDoubled = true;
								int width = tileSize.width * 2, height = tileSize.height * 2;
								double x1 = getTileLongitude(currZoomLevel, j);
								double x2 = (float) getTileLongitude(currZoomLevel, j + 2);
								if (x2 > 180) {
									xDoubled = false;
									x2 = (float) getTileLongitude(currZoomLevel, j + 1);
									width = tileSize.width;
								}
								double y2 = (float) getTileLatitude(currZoomLevel, i);
								double y1 = (float) getTileLatitude(currZoomLevel, i + 2);
								if (y1 < -90) {
									yDoubled = false;
									y1 = (float) getTileLatitude(currZoomLevel, i + 1);
									height = tileSize.height;
								}
								double x = (x1 + x2) / 2, y = (y1 + y2) / 2;
								urlString = urlPrefix + "" + "center=" + y + "," + x + "&zoom=" + currZoomLevel + "&size=" + width + "x" + height + "&format=png&maptype=" + mapType;
							}
							if (urlString == null) {
								if (listener != null) {
									PropertyChangeEvent pce = new PropertyChangeEvent(this, "error", null, "Could not construct a URL string: unknown source " + mapSource);
									listener.propertyChange(pce);
								}
								break;
							}
							if (listener != null) {
								PropertyChangeEvent pce = new PropertyChangeEvent(this, "status", null, "Loading a tile from " + urlString);
								listener.propertyChange(pce);
							}
							try {
								URL url = new URL(urlString);
								if (!mustStop) {
									img = ImageIO.read(url);
								}
							} catch (Throwable e) {
								String errMsg = "Error when reading an image from " + urlString + ":" + e.toString();
								System.out.println(errMsg);
								if (listener != null) {
									PropertyChangeEvent pce = new PropertyChangeEvent(this, "error", null, errMsg);
									listener.propertyChange(pce);
								}
							}
						}
						if (img != null) {
							System.out.println("Successfully loaded image from " + urlString);
/*
              row.setElementAt(img,colN);
*/
							if (!xDoubled && !yDoubled) {
								row.setElementAt(img, colN);
							} else {
								int c0 = 0, r0 = 0, c1 = (xDoubled) ? 2 : 1, r1 = (yDoubled) ? 2 : 1;
								int y0 = 0;
								Vector rRef = row;
								for (int r = r0; r < r1; r++) {
									int x0 = 0;
									for (int c = c0; c < c1; c++) {
										while (rRef.size() <= colN + c) {
											rRef.addElement(null);
										}
										if (rRef.elementAt(colN + c) == null) {
											BufferedImage imPart = new BufferedImage(tileSize.width, tileSize.height, BufferedImage.TYPE_INT_RGB);
											Graphics g = imPart.getGraphics();
											g.drawImage(img, 0, 0, tileSize.width, tileSize.height, x0, y0, x0 + tileSize.width, y0 + tileSize.height, null);
											g.dispose();
											rRef.setElementAt(imPart, colN + c);
										}
										x0 += tileSize.width;
									}
									if (r + 1 < r1) {
										rRef = null;
										if (rowN + r + 1 < tileRows.size()) {
											rRef = (Vector) tileRows.elementAt(rowN + r + 1);
										}
										if (rRef == null) {
											rRef = new Vector(row.capacity(), 10);
											for (int c = 0; c < row.size(); c++) {
												rRef.addElement(null);
											}
											if (rowN + r + 1 < tileRows.size()) {
												tileRows.setElementAt(rRef, rowN + r + 1);
											} else {
												tileRows.addElement(rRef);
											}
										}
										y0 += tileSize.height;
									}
								}
							}
							loaded = true;
						} else {
							all = false;
						}
					}
				}
			}
			if (!mustStop && listener != null) {
				PropertyChangeEvent pce = new PropertyChangeEvent(this, (all) ? "tiles_ready" : (loaded) ? "tiles_loaded" : "failure", null, null);
				listener.propertyChange(pce);
			}
		} while (!mustStop && loaded && !all);
		if (mustStop)
			return;
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

	public boolean isRunning() {
		return running;
	}

	public void stopLoading() {
		mustStop = true;
	}
}
