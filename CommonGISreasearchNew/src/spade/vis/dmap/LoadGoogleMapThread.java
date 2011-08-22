package spade.vis.dmap;

import java.awt.Image;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.util.Vector;

import javax.imageio.ImageIO;

/**
 * Created by IntelliJ IDEA. User: Natascha Date: Aug 19, 2008 Time: 4:55:47 PM
 * Intended for loading tile maps from the OpenStreetMaps server
 */
public class LoadGoogleMapThread extends Thread {
	/**
	 * The extension of a file with a map tile (e.g. ".png")
	 */
	protected String tileFileExtension = null;
	/**
	 * The base URL of the server providing the tiles
	 */
	protected String urlPrefix = null;
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

	double x = 180.00, y = -90.00;
	int width = 0, height = 0;

	/**
	 * Listens to the messages from the thread, e.g. when all tiles have been
	 * loaded
	 */
	protected PropertyChangeListener listener = null;

	private boolean mustStop = false, running = false;

	public void setTileFileExtension(String fileExtension) {
		tileFileExtension = fileExtension;
	}

	public void setTileServerURL(String urlString) {
		urlPrefix = urlString;
	}

	public void setStatusListener(PropertyChangeListener listener) {
		this.listener = listener;
	}

	public void loadTiles(Vector tileRows, int currZoomLevel, double x, double y, int w, int h) {
		this.tileRows = tileRows;
		this.currZoomLevel = currZoomLevel;
		this.x = x;
		this.y = y;
		this.width = w;
		this.height = h;
		start();
	}

	@Override
	public void run() {
		if (tileRows == null || x > 180 || y < -90 || tileFileExtension == null || urlPrefix == null)
			return;
		if (mustStop)
			return;
		boolean loaded = false, all = true;
		if (listener != null) {
			PropertyChangeEvent pce = new PropertyChangeEvent(this, "status", null, "Loading tiles; zoom level = " + currZoomLevel + "; x: " + x + " y: " + y);
			listener.propertyChange(pce);
		}
		do {
			loaded = false;
			Vector row = (Vector) tileRows.elementAt(0);
			if (row == null) {
				continue;
			}
			if (row.elementAt(0) == null) {
				// lon, lat not lat, lon !
				String urlString = urlPrefix + "" + "center=" + y + "," + x + "&zoom=" + currZoomLevel + "&size=" + width + "x" + height + "&format=png&maptype=hybrid";
				// urlString =
				// "http://maps.google.com/staticmap?center=40.71,-73.99&zoom=10&size=256x256&format=png";
				System.out.println("Loading an image " + urlString);
				if (listener != null) {
					PropertyChangeEvent pce = new PropertyChangeEvent(this, "status", null, "Loading a tile from " + urlString);
					listener.propertyChange(pce);
				}
				Image img = null;
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
				if (img != null) {
					row.setElementAt(img, 0);
					loaded = true;
				} else {
					all = false;
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

	public boolean isRunning() {
		return running;
	}

	public void stopLoading() {
		mustStop = true;
	}
}
