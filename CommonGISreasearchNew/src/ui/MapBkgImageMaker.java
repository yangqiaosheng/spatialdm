package ui;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Image;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import spade.analysis.system.ESDACore;
import spade.lib.basicwin.ColumnLayout;
import spade.vis.database.LayerData;
import spade.vis.database.SpatialEntity;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DLayerManager;
import spade.vis.dmap.DOSMLayer;
import spade.vis.dmap.MapCanvas;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.ImageGeometry;
import spade.vis.geometry.RealRectangle;
import spade.vis.map.MapContext;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Mar 26, 2009
 * Time: 1:11:04 PM
 * Used for producing images of desized size with map background
 * for the purposes of documenting. Such an image may be used as a map
 * layer, on which other information is shown.
 * The object works as a thread. When the image is finished, the
 * owner who started the thread is notified. For this purpose,
 * the owner must implement the interface PropertyChangeListener.
 */
public class MapBkgImageMaker extends Thread implements PropertyChangeListener {
	/**
	 * The system core, which is used for accessing necessary components
	 */
	protected ESDACore core = null;
	/**
	 * The original layer manager defining the content of the map.
	 * For producing the image, a copy of the layer manager is created.
	 */
	protected DLayerManager lm0 = null;
	/**
	 * Specifies which of the layers need to be drawn
	 */
	protected boolean toDraw[] = null;
	/**
	 * If true, the visualizers will be removed from the layers
	 * before producing the background map
	 */
	protected boolean removeDataVisualization = false;
	/**
	 * The bounding rectangle of the territory that needs to be shown
	 * in the image.
	 */
	protected RealRectangle terr = null;
	/**
	 * The desired dimensions of the image
	 */
	protected Dimension imSize = null;
	/**
	 * The owner, which must be notified when the image is ready.
	 */
	protected PropertyChangeListener owner = null;
	/**
	 * The constructed image
	 */
	protected Image bkgImage = null;
	/**
	 * A map layer with the image
	 */
	protected DGeoLayer imLayer = null;
	/**
	 * Used for stopping the work of the thread
	 */
	protected boolean mustStop = false;
	/**
	 * Indicates that the thread is running
	 */
	protected boolean running = false;
	/**
	 * Indicates that a background image composed of tiles,
	 * which are loaded from the Internet, is ready
	 */
	protected boolean tileImageReady = false;
	/**
	 * Indicates a failure when loading map tiles from the Internet
	 */
	protected boolean tileImageFailed = false;

	/**
	 * @param core - The system core
	 * @param lmOrig - The original layer manager defining the content of the map.
	 *   For producing the image, a copy of the layer manager is created.
	 * @param toDrawLayer - Specifies which of the layers need to be drawn
	 * @param removeDataVisualization - if true, the visualizers will be removed from the layers
	 *   before producing the background map
	 * @param terr - The bounding rectangle of the territory that needs to be shown
	 *   in the image.
	 * @param imSize - The desired dimensions of the image
	 * @param owner - The owner, which must be notified when the image is ready.
	 */
	public MapBkgImageMaker(ESDACore core, DLayerManager lmOrig, boolean[] toDrawLayer, boolean removeDataVisualization, RealRectangle terr, Dimension imSize, PropertyChangeListener owner) {
		this.core = core;
		this.lm0 = lmOrig;
		this.toDraw = toDrawLayer;
		this.removeDataVisualization = removeDataVisualization;
		this.terr = terr;
		this.imSize = imSize;
		this.owner = owner;
	}

	@Override
	public void run() {
		if (core == null || lm0 == null || terr == null || imSize == null)
			return;
		running = true;
		DLayerManager lm = (DLayerManager) lm0.makeCopy(toDraw == null);
		if (toDraw != null) {
			for (int i = 0; i < lm0.getLayerCount() && i < toDraw.length; i++)
				if (toDraw[i]) {
					DGeoLayer layer = lm0.getLayer(i);
					if (!(layer instanceof DOSMLayer)) {
						DGeoLayer lCopy = (DGeoLayer) layer.makeCopy();
						if (removeDataVisualization && (lCopy.getType() != Geometry.raster)) {
							lCopy.setVisualizer(null);
							lCopy.setBackgroundVisualizer(null);
						}
						lm.addGeoLayer(lCopy);
					}
				}
			lm.checkAndCorrectLinksBetweenLayers();
		}
		MapCanvas map = new MapCanvas();
		map.setMapContent(lm);
		map.setPreferredSize(imSize.width, imSize.height);
		MapContext mc = map.getMapContext();
		mc.setVisibleTerritory(terr);
		Frame mFrame = new Frame("Temporary");
		ColumnLayout cl = new ColumnLayout();
		cl.setAlignment(ColumnLayout.Hor_Left);
		mFrame.setLayout(cl);
		mFrame.add(map);
		mFrame.pack();
		mFrame.setVisible(true);
		map.setSize(imSize.width, imSize.height);

		DOSMLayer tileLayer = lm.getOSMLayer();
		if (tileLayer != null && !tileLayer.hasWholeImage() && !mustStop) {
			tileLayer.addPropertyChangeListener(this);
			while (!mustStop && !tileImageReady && !tileImageFailed && !tileLayer.hasWholeImage()) {
				try {
					sleep(500);
				} catch (InterruptedException e) {
				}
			}
			tileLayer.removePropertyChangeListener(this);
			if (mustStop) {
				running = false;
				mFrame.dispose();
				return;
			}
		}
		if (tileLayer != null && (tileImageReady || tileLayer.hasWholeImage())) {
			tileLayer.allowDynamicLoadingWhenDrawn(false);
			Dimension iSize = tileLayer.getImageSize();
			RealRectangle imageTerrBounds = tileLayer.getImageExtent();
			double stepX = 1.0 * (imageTerrBounds.rx2 - imageTerrBounds.rx1) / iSize.width, stepY = 1.0 * (imageTerrBounds.ry2 - imageTerrBounds.ry1) / iSize.height;
			int mw = (int) Math.round(Math.ceil((terr.rx2 - terr.rx1) / stepX)), mh = (int) Math.round(Math.ceil((terr.ry2 - terr.ry1) / stepY));

			map.setPreferredSize(mw, mh);
			mFrame.pack();
			map.setSize(mw, mh);
			map.showTerrExtent(terr.rx1, terr.ry1, terr.rx2, terr.ry2);
		}

		bkgImage = map.getMapAsImage();
		mFrame.dispose();
		running = false;
		if (bkgImage != null) {
			RealRectangle b = map.getMapContext().getVisibleTerritory();
			LayerData data = new LayerData();
			data.setBoundingRectangle(b.rx1, b.ry1, b.rx2, b.ry2);
			ImageGeometry geom = new ImageGeometry();
			geom.img = bkgImage;
			geom.rx1 = b.rx1;
			geom.ry1 = b.ry1;
			geom.rx2 = b.rx2;
			geom.ry2 = b.ry2;
			SpatialEntity spe = new SpatialEntity("image");
			spe.setGeometry(geom);
			data.addDataItem(spe);
			data.setHasAllData(true);
			imLayer = new DGeoLayer();
			imLayer.receiveSpatialData(data);
		}
		if (owner != null && !mustStop) {
			PropertyChangeEvent pce = new PropertyChangeEvent(this, "bkg_map", null, imLayer);
			owner.propertyChange(pce);
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent pce) {
		String propName = pce.getPropertyName();
		if (propName.equals("tiles_ready")) {
			tileImageReady = true;
		} else if (propName.equals("failure")) {
			tileImageFailed = true;
		}
	}

	public boolean isRunning() {
		return running;
	}

	public void stopWork() {
		mustStop = true;
	}

	public Image getMapBkgImage() {
		return bkgImage;
	}

	public DGeoLayer getImageAsLayer() {
		return imLayer;
	}
}
