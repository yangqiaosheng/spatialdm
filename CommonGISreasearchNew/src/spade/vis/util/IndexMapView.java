package spade.vis.util;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Frame;
import java.awt.Panel;
import java.awt.image.BufferedImage;

import spade.lib.basicwin.ColumnLayout;
import spade.vis.database.ObjectFilterBySelection;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DLayerManager;
import spade.vis.dmap.DrawingParameters;
import spade.vis.dmap.MapCanvas;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.ImageGeometry;
import spade.vis.map.MapContext;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Nov 9, 2009
 * Time: 2:34:03 PM
 * Shows an "index" map where selected geographical objects are marked
 * e.g. by black borders and filling
 */
public class IndexMapView extends Panel {
	/**
	 * The layer containing the map background image
	 */
	public DGeoLayer bkgLayer = null;
	/**
	 * The layer in which the objects may be selected or not selected
	 */
	public DGeoLayer objLayer = null;
	/**
	 * The layer manager
	 */
	public DLayerManager lman = null;
	/**
	 * The map canvas
	 */
	public MapCanvas map = null;
	/**
	 * Selects objects in the layer
	 */
	protected ObjectFilterBySelection sFilter = null;

	public IndexMapView(DGeoLayer bkgLayer, DGeoLayer objLayer) {
		if (objLayer == null)
			return;
		this.objLayer = (DGeoLayer) objLayer.makeCopy();
		this.objLayer.setVisualizer(null);
		this.objLayer.setBackgroundVisualizer(null);
		this.objLayer.removeFilter();
		this.objLayer.setDataTable(null);
		DrawingParameters dp = this.objLayer.getDrawingParameters();
		dp.lineColor = Color.black;
		dp.fillColor = Color.black;
		dp.lineWidth = 1;
		dp.transparency = 0;
		dp.fillContours = true;
		dp.drawLabels = false;
		dp.drawLayer = true;
		if (bkgLayer != null) {
			this.bkgLayer = (DGeoLayer) bkgLayer.makeCopy();
		}
		lman = new DLayerManager();
		if (this.bkgLayer != null) {
			lman.addGeoLayer(this.bkgLayer);
		}
		lman.addGeoLayer(this.objLayer);
		lman.setGeographic(objLayer.isGeographic());
		if (lman.getOSMLayer() != null) {
			lman.getOSMLayer().setLayerDrawn(false);
		}
		map = new MapCanvas();
		map.setMapContent(lman);
		int iw = 0, ih = 0;
		if (bkgLayer != null && bkgLayer.getType() == Geometry.image && bkgLayer.getObjectCount() > 0) {
			Geometry geom = bkgLayer.getObject(0).getGeometry();
			if (geom instanceof ImageGeometry) {
				ImageGeometry ig = (ImageGeometry) geom;
				if (ig.img != null) {
					iw = ig.img.getWidth(null);
					ih = ig.img.getHeight(null);
					map.setPreferredSize(iw, ih);
					map.setSize(iw, ih);
					MapContext mc = map.getMapContext();
					mc.setVisibleTerritory(bkgLayer.getWholeLayerBounds());
					mc.setViewportBounds(0, 0, iw, ih);
				}
			}
		}
		if (iw == 0 || ih == 0) {
			iw = ih = 100;
			map.setPreferredSize(iw, ih);
			map.setSize(iw, ih);
			MapContext mc = map.getMapContext();
			mc.setVisibleTerritory(objLayer.getWholeLayerBounds());
			mc.setViewportBounds(0, 0, iw, ih);
		}
		setLayout(new BorderLayout());
		add(map, BorderLayout.CENTER);
	}

	public void addSelectedObject(String oId) {
		if (objLayer == null || oId == null)
			return;
		int oidx = objLayer.getObjectIndex(oId);
		if (oidx < 0)
			return;
		if (sFilter == null) {
			sFilter = new ObjectFilterBySelection();
			sFilter.setObjectContainer(objLayer);
			sFilter.setEntitySetIdentifier(objLayer.getEntitySetIdentifier());
			sFilter.makeAllDeselected();
			objLayer.setObjectFilter(sFilter);
		}
		sFilter.setObjectStatus(oidx, true);
	}

	public void deselectAllObjects() {
		if (sFilter == null) {
			sFilter = new ObjectFilterBySelection();
			sFilter.setObjectContainer(objLayer);
			sFilter.setEntitySetIdentifier(objLayer.getEntitySetIdentifier());
			sFilter.makeAllDeselected();
			objLayer.setObjectFilter(sFilter);
		} else {
			sFilter.makeAllDeselected();
		}
		map.invalidateImage();
	}

	private Frame mFrame = null;

	public BufferedImage getImage() {
		map.invalidateImage();
		if (map.isShowing()) {
			synchronized (map) {
				return (BufferedImage) map.getMapAsImage();
			}
		}
		if (mFrame == null) {
			mFrame = new Frame("Temporary");
			ColumnLayout cl = new ColumnLayout();
			cl.setAlignment(ColumnLayout.Hor_Left);
			mFrame.setLayout(cl);
			mFrame.add(map);
			mFrame.pack();
			mFrame.setVisible(true);
		}
		synchronized (map) {
			return (BufferedImage) map.getMapAsImage();
		}
	}

	public void destroy() {
		if (mFrame != null) {
			mFrame.dispose();
		}
		mFrame = null;
	}
}
