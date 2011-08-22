package data_load.tiles;

import java.awt.Choice;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Vector;

import spade.analysis.system.ESDACore;
import spade.analysis.tools.BaseAnalyser;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Dialogs;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.OKDialog;
import spade.lib.util.CopyFile;
import spade.lib.util.StringUtil;
import spade.vis.dmap.DLayerManager;
import spade.vis.dmap.DOSMLayer;
import spade.vis.dmap.LoadTilesThread;
import spade.vis.geometry.RealRectangle;
import spade.vis.space.LayerManager;
import ui.ImagePrinter;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Aug 9, 2010
 * Time: 12:38:25 PM
 * Gets OSM tiles from the server and saves them locally as files
 */
public class MapTileSaver extends BaseAnalyser implements ItemListener, PropertyChangeListener {
	/**
	* Remembers the last directory where data were saved
	*/
	protected static String lastDir = null;
	/**
	 * Bounding rectangle of the currently visible territory
	 */
	protected RealRectangle currBounds = null;
	/**
	 * Current zoom level in the map
	 */
	protected int currZoomLevel = 0;
	/**
	 * The desized lowest zoom level
	 */
	protected int chosenZoomLevel = 0;
	/**
	 * Used to choose the desired lowest zoom level
	 */
	protected Choice chZoomLevel = null;
	/**
	 * Used to display the estimated number of tiles and total file size
	 */
	protected TextField tfNTiles = null, tfSize = null;
	/**
	 * Loads the tiles in the "background" mode
	 */
	protected static LoadTilesThread tilesLoader = null;
	/**
	 * The path to the top directory with the tiles
	 */
	protected String tileTopDirPath = null;
	/**
	 * The path to the tile index file
	 */
	protected String pathOSMTilesIndex = null;

	/**
	* MapTileSaver checks if this is not an applet.
	*/
	@Override
	public boolean isValid(ESDACore core) {
		return core.getSystemSettings().checkParameterValue("isLocalSystem", "true");
	}

	/**
	* This method constructs and starts the tool. Everything the tool may need
	* for integration with other components of the system can be received
	* from the system's core passed as an argument.
	*/
	@Override
	public void run(ESDACore core) {
		this.core = core;
		if (core == null || core.getUI() == null)
			return;
		if (core.getUI().getCurrentMapViewer() == null || core.getUI().getCurrentMapViewer().getLayerManager() == null) {
			showMessage("No map exists!", true);
			return;
		}
		if (tilesLoader != null) {
			showMessage("Map tiles are currently being loaded!", true);
			if (!Dialogs.askYesOrNo(getFrame(), "Map tiles are currently being loaded. " + "Stop the loading process?", "Tiles loading in progress!"))
				return;
			tilesLoader.stopLoading();
			tilesLoader = null;
		}
		LayerManager lman = core.getUI().getCurrentMapViewer().getLayerManager();
		DOSMLayer osmLayer = null;
		for (int i = 0; i < lman.getLayerCount() && osmLayer == null; i++)
			if (lman.getGeoLayer(i) instanceof DOSMLayer) {
				osmLayer = (DOSMLayer) lman.getGeoLayer(i);
				if (osmLayer.getMapSource() != DOSMLayer.source_OpenStreetMaps) {
					osmLayer = null;
				}
			}
		if (osmLayer == null) {
			showMessage("No OpenStreetMaps map layer exists!", true);
			return;
		}
		float ext[] = core.getUI().getCurrentMapViewer().getMapExtent();
		if (ext == null) {
			showMessage("Cannot get the current map extent!", true);
			return;
		}
		int zoomLevel = osmLayer.getCurrZoomLevel();
		if (zoomLevel < 1) {
			showMessage("Wrong value of current zoom level: " + zoomLevel, true);
			return;
		}
		currZoomLevel = zoomLevel;
		currBounds = new RealRectangle(ext);
		if (currBounds.ry1 < -90) {
			currBounds.ry1 = -90;
		}
		if (currBounds.ry2 > 90) {
			currBounds.ry2 = 90;
		}
		if (currBounds.rx1 < -180) {
			currBounds.rx1 = -180;
		}
		if (currBounds.rx2 > 180) {
			currBounds.rx2 = 180;
		}
		int left = DOSMLayer.getTileNumberX(zoomLevel, currBounds.rx1), right = DOSMLayer.getTileNumberX(zoomLevel, currBounds.rx2);
		int top = DOSMLayer.getTileNumberY(zoomLevel, currBounds.ry2), bottom = DOSMLayer.getTileNumberY(zoomLevel, currBounds.ry1);
		int nColumns = right - left + 1, nRows = bottom - top + 1, nTiles = nColumns * nRows;

		Panel p = new Panel(new ColumnLayout());
		p.add(new Label("Current zoom level: " + zoomLevel));
		p.add(new Label("Current number of tiles: " + nTiles + " (" + nColumns + " columns x " + nRows + " rows)"));
		p.add(new Line(false));
		p.add(new Label("Save map tiles", Label.CENTER));
		p.add(new Label("starting from the current zoom level " + zoomLevel));
		chZoomLevel = new Choice();
		chZoomLevel.addItemListener(this);
		for (int i = zoomLevel; i <= 18; i++) {
			chZoomLevel.addItem(StringUtil.padString(String.valueOf(i), '0', 2, true));
		}
		chZoomLevel.select(0);
		Panel pp = new Panel(new FlowLayout());
		pp.add(new Label("and ending with the level"));
		pp.add(chZoomLevel);
		p.add(pp);
		p.add(new Line(false));
		pp = new Panel(new FlowLayout());
		pp.add(new Label("Estimated number of tiles:"));
		tfNTiles = new TextField(String.valueOf(nTiles), 10);
		tfNTiles.setEditable(false);
		pp.add(tfNTiles);
		p.add(pp);
		pp = new Panel(new FlowLayout());
		pp.add(new Label("Estimated total size of the files, Mbytes:"));
		tfSize = new TextField(StringUtil.floatToStr(25f * nTiles / 1000, 3), 10);
		tfSize.setEditable(false);
		pp.add(tfSize);
		p.add(pp);
		OKDialog okd = new OKDialog(getFrame(), "Save map tiles", true);
		okd.addContent(p);
		okd.show();
		if (okd.wasCancelled())
			return;

		FileDialog fd = new FileDialog(getFrame(), "OSM tile index file");
		if (lastDir != null) {
			fd.setDirectory(lastDir);
		}
		fd.setFile("*.osm");
		fd.setMode(FileDialog.SAVE);
		fd.setVisible(true);
		if (fd.getDirectory() == null)
			return;
		lastDir = fd.getDirectory();
		String indexFName = fd.getFile();
		String str = CopyFile.getExtension(indexFName);
		if (str == null || str.length() < 1) {
			indexFName += ".osm";
		}
		str = CopyFile.getNameWithoutExt(indexFName);

		String dirName = str + "_TILES", fullDirName = lastDir + dirName;
		File tdf = new File(fullDirName);
		if (!tdf.exists() || !tdf.isDirectory()) {
			tdf.mkdir();
		}
		if (!tdf.exists() || !tdf.isDirectory()) {
			showMessage("could not create directory " + tdf.getAbsolutePath(), true);
			return;
		}
		chosenZoomLevel = currZoomLevel + chZoomLevel.getSelectedIndex();
		for (zoomLevel = currZoomLevel; zoomLevel <= chosenZoomLevel; zoomLevel++) {
			tdf = new File(fullDirName + "/" + zoomLevel);
			if (!tdf.exists() || !tdf.isDirectory()) {
				tdf.mkdir();
			}
			if (!tdf.exists() || !tdf.isDirectory()) {
				showMessage("could not create directory " + tdf.getAbsolutePath(), true);
				return;
			}
		}
		pathOSMTilesIndex = lastDir + indexFName;
		FileWriter out = null;
		try {
			out = new FileWriter(pathOSMTilesIndex);
		} catch (IOException ioe) {
			System.out.println(ioe);
		}
		if (out == null) {
			showMessage("Could not create file " + pathOSMTilesIndex, true);
			return;
		}
		try {
			out.write("DIRECTORY=" + dirName + "\r\n");
			out.write("MIN_LEVEL=" + currZoomLevel + "\r\n");
			out.write("MAX_LEVEL=" + chosenZoomLevel + "\r\n");
		} catch (IOException ioe) {
			showMessage("Could not write file " + lastDir + indexFName, true);
		}
		try {
			out.close();
		} catch (IOException ioe) {
		}

		tileTopDirPath = fullDirName;

		Vector tileRows = new Vector(nRows, 10);
		for (int i = top; i <= bottom; i++) {
			Vector row = new Vector(nColumns, 10);
			for (int j = left; j <= right; j++) {
				row.addElement(osmLayer.getTile(i, j));
			}
			tileRows.addElement(row);
		}

		tilesLoader = new LoadTilesThread();
		tilesLoader.setTileFileExtension(DOSMLayer.tileFileExtension);
		tilesLoader.setMapSource(DOSMLayer.source_OpenStreetMaps);
		tilesLoader.setTileServerURL(DOSMLayer.urlPrefixOSM);
		tilesLoader.setTileTopDirPath(tileTopDirPath);
		tilesLoader.setStatusListener(this);
		tilesLoader.loadTiles(tileRows, currZoomLevel, left, right, top, bottom, left, right, top, bottom);
	}

	/**
	 * Reacts to choosing a desired lowest zoom level
	 */
	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource().equals(chZoomLevel)) {
			int chosenZoomLevel = currZoomLevel + chZoomLevel.getSelectedIndex();
			int nTiles = 0;
			for (int zoomLevel = currZoomLevel; zoomLevel <= chosenZoomLevel; zoomLevel++) {
				int left = DOSMLayer.getTileNumberX(zoomLevel, currBounds.rx1), right = DOSMLayer.getTileNumberX(zoomLevel, currBounds.rx2);
				int top = DOSMLayer.getTileNumberY(zoomLevel, currBounds.ry2), bottom = DOSMLayer.getTileNumberY(zoomLevel, currBounds.ry1);
				int nColumns = right - left + 1, nRows = bottom - top + 1, nTilesLevel = nColumns * nRows;
				nTiles += nTilesLevel;
			}
			tfNTiles.setText(String.valueOf(nTiles));
			tfSize.setText(StringUtil.floatToStr(25f * nTiles / 1000, 3));
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent pce) {
		if (pce.getSource().equals(tilesLoader)) {
			String propName = pce.getPropertyName();
			if (propName.equals("failure")) {
				showMessage("Failure when loading map tiles for zoom level " + tilesLoader.getCurrZoomLevel(), true);
				tilesLoader = null;
				return;
			}
			if (propName.equals("tiles_ready")) {
				int zoomLevel = tilesLoader.getCurrZoomLevel();
				showMessage("Successfully loaded map tiles for zoom level " + zoomLevel + "!", false);
				Vector tileRows = tilesLoader.getTileRows();
				int left = tilesLoader.getLeftTileN(), right = tilesLoader.getRightTileN();
				int top = tilesLoader.getTopTileN(), bottom = tilesLoader.getBottomTileN();
				tilesLoader = null;
				if (zoomLevel < chosenZoomLevel) {
					startTileLoading(zoomLevel + 1);
				}
				int nImages = (right - left + 1) * (bottom - top + 1);
				Vector vImages = new Vector(nImages, 10), vFnames = new Vector(nImages, 10);
				for (int i = top; i <= bottom; i++) {
					int rowN = i - top;
					Vector row = (Vector) tileRows.elementAt(rowN);
					if (row == null) {
						continue;
					}
					for (int j = left; j <= right; j++) {
						int colN = j - left;
						Image img = (Image) row.elementAt(colN);
						if (img == null) {
							continue;
						}
						File tdf = new File(tileTopDirPath + "/" + zoomLevel + "/" + j);
						if (!tdf.exists() || !tdf.isDirectory()) {
							tdf.mkdir();
						}
						String fName = j + "/" + i;
						vImages.addElement(img);
						vFnames.addElement(fName);
					}
				}
				ImagePrinter impr = new ImagePrinter(core.getSupervisor());
				impr.setImages(vImages, vFnames);
				boolean saved = impr.saveImages(false, DOSMLayer.tileFileExtension, tileTopDirPath + "/" + zoomLevel + "/");
				if (saved) {
					if (zoomLevel == chosenZoomLevel) {
						showMessage("Successfully finished saving map tiles; zoom levels from " + currZoomLevel + " to " + chosenZoomLevel, false);
						LayerManager lman = core.getUI().getCurrentMapViewer().getLayerManager();
						if (lman instanceof DLayerManager) {
							DLayerManager lMan = (DLayerManager) lman;
							lMan.setPathOSMTilesIndex(pathOSMTilesIndex);
						}

					} else {
						showMessage("Successfully saved map tiles for zoom level " + zoomLevel + "!", false);
					}
				} else {
					showMessage("Failed to save map tiles for zoom level " + zoomLevel + "!", true);
				}
			}
		}
	}

	protected void startTileLoading(int zoomLevel) {
		int left = DOSMLayer.getTileNumberX(zoomLevel, currBounds.rx1), right = DOSMLayer.getTileNumberX(zoomLevel, currBounds.rx2);
		int top = DOSMLayer.getTileNumberY(zoomLevel, currBounds.ry2), bottom = DOSMLayer.getTileNumberY(zoomLevel, currBounds.ry1);
		int nColumns = right - left + 1, nRows = bottom - top + 1, nTiles = nColumns * nRows;

		Vector tileRows = new Vector(nRows, 10);
		for (int i = top; i <= bottom; i++) {
			Vector row = new Vector(nColumns, 10);
			for (int j = left; j <= right; j++) {
				row.addElement(null);
			}
			tileRows.addElement(row);
		}

		tilesLoader = new LoadTilesThread();
		tilesLoader.setTileFileExtension(DOSMLayer.tileFileExtension);
		tilesLoader.setMapSource(DOSMLayer.source_OpenStreetMaps);
		tilesLoader.setTileServerURL(DOSMLayer.urlPrefixOSM);
		tilesLoader.setTileTopDirPath(tileTopDirPath);
		tilesLoader.setStatusListener(this);
		tilesLoader.loadTiles(tileRows, zoomLevel, left, right, top, bottom, left, right, top, bottom);
	}
}
