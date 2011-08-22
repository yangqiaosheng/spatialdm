package spade.analysis.tools;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.Rectangle;
import java.awt.ScrollPane;
import java.util.Vector;

import spade.analysis.system.DataKeeper;
import spade.analysis.system.ESDACore;
import spade.analysis.tools.clustering.ClusterImage;
import spade.analysis.tools.clustering.ClustersOverviewPanel;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.OKDialog;
import spade.lib.util.Frequencies;
import spade.lib.util.IntArray;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.database.LayerData;
import spade.vis.database.ObjectFilterBySelection;
import spade.vis.database.SpatialEntity;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;
import spade.vis.dmap.DLayerManager;
import spade.vis.dmap.DOSMLayer;
import spade.vis.dmap.DrawingParameters;
import spade.vis.dmap.MapCanvas;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.ImageGeometry;
import spade.vis.geometry.RealRectangle;
import spade.vis.map.MapContext;
import spade.vis.space.GeoLayer;
import spade.vis.space.LayerManager;
import ui.SimpleMapView;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Mar 27, 2009
 * Time: 12:01:02 PM
 * This tool builds a panel with multiple map images each representing
 * one class or cluster of objects. The classes must be specified in
 * a table column.
 */
public class ShowClassesBySmallMultiples extends BaseAnalyser {
	/**
	* This method constructs and starts the tool. Everything the tool may need
	* for integration with other components of the system can be received
	* from the system's core passed as an argument.
	*/
	@Override
	public void run(ESDACore core) {
		this.core = core;
		if (core == null || core.getUI() == null || core.getUI().getCurrentMapViewer() == null) {
			showMessage("No map exists!", true);
			return;
		}
		DataKeeper dk = core.getDataKeeper();
		if (dk == null || dk.getMapCount() < 1 || dk.getTableCount() < 1) {
			showMessage("No data loaded!", true);
			return;
		}
		LayerManager lm = dk.getMap(0);
		if (lm == null || lm.getLayerCount() < 1)
			return;
		Vector<DGeoLayer> layers = new Vector<DGeoLayer>(20, 10);
		for (int j = 0; j < lm.getLayerCount(); j++) {
			GeoLayer gl = lm.getGeoLayer(j);
			if (gl != null && (gl instanceof DGeoLayer) && gl.getType() != Geometry.image && gl.getType() != Geometry.raster && gl.hasThematicData()) {
				layers.addElement((DGeoLayer) gl);
			}
		}
		if (layers.size() < 1) {
			showMessage("No appropriate map layers found!", true);
			return;
		}
		Panel mainP = new Panel(new ColumnLayout());
		mainP.add(new Label("Select the layer with classified objects:"));
		List list = new List(Math.max(layers.size() + 1, 5));
		for (int i = 0; i < layers.size(); i++) {
			list.add((layers.elementAt(i)).getName());
		}
		list.select(0);
		mainP.add(list);
		OKDialog dia = new OKDialog(core.getUI().getMainFrame(), "Show classes of objects from layer", true);
		dia.addContent(mainP);
		dia.show();
		if (dia.wasCancelled())
			return;
		int idx = list.getSelectedIndex();
		if (idx < 0)
			return;

		DGeoLayer layer = layers.elementAt(idx);
		DataTable table = (DataTable) layer.getThematicData();
		if (table == null) {
			showMessage("The layer has no table with thematic data!", true);
			return;
		}
		IntArray aIdxs = new IntArray(20, 10);
		for (int i = 0; i < table.getAttrCount(); i++) {
			Attribute at = table.getAttribute(i);
			if (at.isClassification() || at.getType() == AttributeTypes.character || at.getType() == AttributeTypes.logical) {
				aIdxs.addElement(i);
			}
		}
		if (aIdxs.size() < 1) {
			showMessage("The table has no columns defining classes or clusters!", true);
			return;
		}
		list = new List(Math.max(10, Math.min(aIdxs.size(), 3)));
		for (int i = 0; i < aIdxs.size(); i++) {
			list.add(table.getAttributeName(aIdxs.elementAt(i)));
		}
		list.select(aIdxs.size() - 1);
		mainP = new Panel(new BorderLayout());
		mainP.add(new Label("Select the table column defining the classes or clusters:"), BorderLayout.NORTH);
		mainP.add(list, BorderLayout.CENTER);
		dia = new OKDialog(core.getUI().getMainFrame(), "Select column with classes", true);
		dia.addContent(mainP);
		dia.show();
		if (dia.wasCancelled())
			return;
		int k = list.getSelectedIndex();
		if (k < 0)
			return;

		int colN = aIdxs.elementAt(k);
		int maxIW = Math.round(60f * core.getUI().getMainFrame().getToolkit().getScreenResolution() / 25.33f);
		Vector<ClusterImage> cImages = getClassImages(layer, table, colN, maxIW);
		if (cImages != null && cImages.size() > 0) {
			ClustersOverviewPanel imPanel = new ClustersOverviewPanel(table, colN, cImages, "Classes of " + layer.getName(), "Classes (" + table.getAttributeName(colN) + ") of " + layer.getName(), core);
			ScrollPane scp = new ScrollPane(ScrollPane.SCROLLBARS_ALWAYS);
			ClusterImage cim = cImages.elementAt(0);
			int w = cim.image.getWidth(null), h = cim.image.getHeight(null);
			core.getDisplayProducer().makeWindow(imPanel, layer.getName(), w * 3 + 30 + scp.getVScrollbarWidth(), h * 3 + 50);
		}
	}

	/**
	 * Creates small map images representing in a summarized form
	 * classes or clusters of trajectories
	 */
	public Vector<ClusterImage> getClassImages(DGeoLayer clLayer, DataTable clTable, int clColN, int maxImgWidth) {
		if (clLayer == null || clLayer.getObjectCount() < 2 || clTable == null || clColN < 0)
			return null;
		if (core == null || core.getUI() == null || core.getUI().getCurrentMapViewer() == null || !(core.getUI().getCurrentMapViewer() instanceof SimpleMapView))
			return null;
		SimpleMapView mw0 = (SimpleMapView) core.getUI().getCurrentMapViewer();
		if (mw0.getLayerManager() == null || !(mw0.getLayerManager() instanceof DLayerManager))
			return null;

		clLayer.setHasAllObjects(true);
		RealRectangle bounds = clLayer.getWholeLayerBounds();
		float lw = bounds.rx2 - bounds.rx1, lh = bounds.ry2 - bounds.ry1, dw = lw / 20, dh = lh / 20;
		bounds = new RealRectangle(bounds.rx1 - dw, bounds.ry1 - dh, bounds.rx2 + dw, bounds.ry2 + dh);
		MapContext mc = mw0.getMapDrawer().getMapContext();
		Rectangle scrRect = mc.getScreenRectangle(bounds.rx1, bounds.ry1, bounds.rx2, bounds.ry2);
		int imgWidth = scrRect.width, imgHeight = scrRect.height;
		float ratio = 1f * imgWidth / maxImgWidth;
		if (ratio > 1.1f) {
			imgWidth = Math.round(imgWidth / ratio);
			imgHeight = Math.round(imgHeight / ratio);
		}

		//ordering of the classes according to their sizes
		Frequencies freq = clTable.getAttribute(clColN).getValueFrequencies();
		if (freq == null) {
			freq = clTable.getValueFrequencies(clColN, true, true);
		}
		if (freq == null)
			return null;
		freq.sortItems(Frequencies.order_by_frequency_descend);
		int nClasses = freq.getItemCount();

		DLayerManager lm0 = (DLayerManager) mw0.getLayerManager();
		boolean drawn[] = new boolean[lm0.getLayerCount()];
		boolean needRestore = false;
		for (int i = 0; i < lm0.getLayerCount(); i++) {
			DGeoLayer layer = lm0.getLayer(i);
			drawn[i] = layer.getLayerDrawn();
			if (drawn[i] && layer.equals(clLayer)) {
				layer.setLayerDrawn(false);
				needRestore = true;
			}
		}
		Image bkg = mw0.getMapBkgImage();
		if (needRestore) {
			for (int i = 0; i < lm0.getLayerCount(); i++) {
				DGeoLayer layer = lm0.getLayer(i);
				if (layer.getLayerDrawn() != drawn[i]) {
					layer.setLayerDrawn(true);
				}
			}
		}
		DLayerManager lm = (DLayerManager) lm0.makeCopy(false);
		if (bkg != null) {
			float b[] = mw0.getMapExtent();
			LayerData data = new LayerData();
			data.setBoundingRectangle(b[0], b[1], b[2], b[3]);
			ImageGeometry geom = new ImageGeometry();
			geom.img = bkg;
			geom.rx1 = b[0];
			geom.ry1 = b[1];
			geom.rx2 = b[2];
			geom.ry2 = b[3];
			//following text:"image"
			SpatialEntity spe = new SpatialEntity("image");
			spe.setGeometry(geom);
			data.addDataItem(spe);
			data.setHasAllData(true);
			DGeoLayer bkgLayer = new DGeoLayer();
			bkgLayer.receiveSpatialData(data);
			lm.addGeoLayer(bkgLayer);
		} else {
			for (int i = 0; i < lm0.getLayerCount(); i++) {
				DGeoLayer layer = lm0.getLayer(i);
				if (!layer.getLayerDrawn()) {
					continue;
				}
				if (layer instanceof DOSMLayer) {
					continue;
				}
				if (layer.equals(clLayer)) {
					continue;
				}
				lm.addGeoLayer((DGeoLayer) layer.makeCopy());
			}
		}
		clLayer = (DGeoLayer) clLayer.makeCopy();
		clLayer.removeFilter();
		ObjectFilterBySelection clFilter = new ObjectFilterBySelection();
		clFilter.setObjectContainer(clLayer);
		clFilter.setEntitySetIdentifier(clLayer.getEntitySetIdentifier());
		clLayer.setObjectFilter(clFilter);
		DrawingParameters dp = clLayer.getDrawingParameters();
		dp.transparency = 20;

		lm.addGeoLayer(clLayer);
		lm.activateLayer(lm.getLayerCount() - 1);
		DOSMLayer osmLayer = lm.getOSMLayer();
		if (osmLayer != null) {
			osmLayer.setLayerDrawn(false);
		}
		DOSMLayer gmLayer = lm.getGMLayer();
		if (gmLayer != null) {
			gmLayer.setLayerDrawn(false);
		}

		MapCanvas map = new MapCanvas();
		map.setMapContent(lm);
		map.setPreferredSize(imgWidth, imgHeight);
		mc = map.getMapContext();
		mc.setVisibleTerritory(bounds);
		Frame mFrame = new Frame("Temporary");
		ColumnLayout cl = new ColumnLayout();
		cl.setAlignment(ColumnLayout.Hor_Left);
		mFrame.setLayout(cl);
		mFrame.add(map);
		mFrame.pack();
		mFrame.setVisible(true);
		map.setSize(imgWidth, imgHeight);

		Vector<ClusterImage> images = new Vector<ClusterImage>(nClasses, 1);
		IntArray selObjects = new IntArray(500, 100);
		for (int i = 0; i < nClasses; i++) {
			selObjects.removeAllElements();
			String label = freq.getItemAsString(i);
			for (int j = 0; j < clLayer.getObjectCount(); j++)
				if (clLayer.isObjectActive(j)) {
					DGeoObject lObj = clLayer.getObject(j);
					DataRecord rec = (DataRecord) lObj.getData();
					if (rec == null) {
						continue;
					}
					if (label.equals(rec.getAttrValueAsString(clColN))) {
						selObjects.addElement(j);
					}
				}
			if (selObjects.size() < 1) {
				continue;
			}
			clFilter.setActiveObjectIndexes(selObjects);
			map.invalidateImage();
			Image image = map.getMapAsImage();
			if (image != null) {
				ClusterImage cim = new ClusterImage();
				cim.clusterLabel = label;
				cim.image = image;
				cim.size = freq.getFrequency(i);
				images.addElement(cim);
			}
			clFilter.clearFilter();
		}
		mFrame.dispose();
		if (images.size() < 1)
			return null;
		return images;
	}

}
