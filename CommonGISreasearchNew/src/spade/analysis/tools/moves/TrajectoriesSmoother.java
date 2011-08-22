package spade.analysis.tools.moves;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.TextField;
import java.util.Vector;

import spade.analysis.system.DataLoader;
import spade.analysis.system.ESDACore;
import spade.analysis.tools.BaseAnalyser;
import spade.lib.basicwin.Centimeter;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.OKDialog;
import spade.lib.util.StringUtil;
import spade.vis.database.DataTable;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;
import spade.vis.dmap.DLayerManager;
import spade.vis.dmap.DMovingObject;
import spade.vis.dmap.DrawingParameters;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.RealRectangle;
import spade.vis.space.GeoLayer;
import spade.vis.space.LayerManager;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Oct 8, 2010
 * Time: 1:00:56 PM
 * Smoothes trajectories. Creates a new geo layer with the smoothed trajectories.
 */
public class TrajectoriesSmoother extends BaseAnalyser {
	/**
	 * This method constructs and starts the tool. Everything the tool may need
	 * for integration with other components of the system can be received
	 * from the system's core passed as an argument.
	 */
	@Override
	public void run(ESDACore core) {
		if (core == null || core.getUI() == null)
			return;
		this.core = core;
		if (core.getUI().getCurrentMapViewer() == null || core.getUI().getCurrentMapViewer().getLayerManager() == null) {
			showMessage("No map exists!", true);
			return;
		}
		//Find instances of DGeoLayer containing trajectories
		LayerManager lman = core.getUI().getCurrentMapViewer().getLayerManager();
		Vector moveLayers = new Vector(lman.getLayerCount(), 1);
		boolean geo = false;
		float minx = Float.NaN, miny = Float.NaN, maxx = Float.NaN, maxy = Float.NaN;
		for (int i = 0; i < lman.getLayerCount(); i++) {
			GeoLayer layer = lman.getGeoLayer(i);
			if ((layer instanceof DGeoLayer) && layer.getObjectCount() > 0 && (layer.getObjectAt(0) instanceof DMovingObject)) {
				moveLayers.addElement(layer);
				geo = geo || layer.isGeographic();
				RealRectangle r = ((DGeoLayer) layer).getWholeLayerBounds();
				if (r == null) {
					r = ((DGeoLayer) layer).getCurrentLayerBounds();
				}
				if (r != null) {
					if (Float.isNaN(minx) || minx > r.rx1) {
						minx = r.rx1;
					}
					if (Float.isNaN(maxx) || maxx < r.rx2) {
						maxx = r.rx2;
					}
					if (Float.isNaN(miny) || miny > r.ry1) {
						miny = r.ry1;
					}
					if (Float.isNaN(maxy) || maxy < r.ry2) {
						maxy = r.ry2;
					}
				}
			}
		}
		if (moveLayers.size() < 1) {
			showMessage("No layers with trajectories found!", true);
			return;
		}
		Panel mainP = new Panel(new ColumnLayout());
		mainP.add(new Label("Select the layer with trajectories to smooth:"));
		List list = new List(Math.max(moveLayers.size() + 1, 5));
		for (int i = 0; i < moveLayers.size(); i++) {
			list.add(((DGeoLayer) moveLayers.elementAt(i)).getName());
		}
		list.select(0);
		mainP.add(list);

		double wh[] = DGeoLayer.getExtentXY(minx, miny, maxx, maxy, geo);
		float width = (float) wh[0], height = (float) wh[1];
		float geoFactorX = 1f, geoFactorY = 1f;
		if (geo) {
			geoFactorX = width / (maxx - minx);
			geoFactorY = height / (maxy - miny);
		}
		float defRad = Math.min(width, height) / 200;
		float factor = 1;
		if (defRad > 1) {
			while (defRad >= 10) {
				factor *= 10;
				defRad /= 10;
			}
		} else {
			while (defRad < 1) {
				factor /= 10;
				defRad *= 10;
			}
		}
		if (defRad < 3) {
			defRad = 1;
		} else if (defRad < 7) {
			defRad = 5;
		} else {
			defRad = 10;
		}
		defRad *= factor;
		String radStr = StringUtil.floatToStr(defRad, 0, defRad * 10);
		Panel p = new Panel();
		GridBagLayout gridbag = new GridBagLayout();
		p.setLayout(gridbag);
		GridBagConstraints c = new GridBagConstraints();
		c.weightx = 1.0f;
		c.weighty = 1.0f;
		c.fill = GridBagConstraints.HORIZONTAL;
		Label l = new Label("Radius around a position:");
		c.gridwidth = 3;
		gridbag.setConstraints(l, c);
		p.add(l);
		TextField radTF = new TextField(radStr, 10);
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(radTF, c);
		p.add(radTF);
		l = new Label("X-extent:", Label.RIGHT);
		c.gridwidth = 1;
		gridbag.setConstraints(l, c);
		p.add(l);
		TextField ttf = new TextField(StringUtil.floatToStr(width, 0f, Math.max(width, height)), 10);
		ttf.setEditable(false);
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(ttf, c);
		p.add(ttf);
		l = new Label("Y-extent:", Label.RIGHT);
		c.gridwidth = 1;
		gridbag.setConstraints(l, c);
		p.add(l);
		ttf = new TextField(StringUtil.floatToStr(height, 0f, Math.max(width, height)), 10);
		ttf.setEditable(false);
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(ttf, c);
		p.add(ttf);
		Panel pp = new Panel(new FlowLayout(FlowLayout.LEFT));
		pp.add(new Label("Scale:"));
		Centimeter cm = new Centimeter();
		pp.add(cm);
		float sc = core.getUI().getMapViewer(core.getUI().getCurrentMapN()).getMapDrawer().getMapContext().getPixelValue() * cm.getMinimumSize().width * ((geo) ? geoFactorX : ((DLayerManager) lman).user_factor);
		pp.add(new Label(StringUtil.floatToStr(sc, 2) + " " + ((geo) ? "m" : ((DLayerManager) lman).getUserUnit())));
		gridbag.setConstraints(pp, c);
		p.add(pp);
		mainP.add(p);

		OKDialog dia = new OKDialog(core.getUI().getMainFrame(), "Smooth trajectories", true);
		dia.addContent(mainP);
		dia.show();
		if (dia.wasCancelled())
			return;
		int idx = list.getSelectedIndex();
		if (idx < 0)
			return;
		float rad = 0f;
		String str = radTF.getText();
		if (str != null) {
			try {
				rad = Float.valueOf(str).floatValue();
			} catch (NumberFormatException nfe) {
			}
		}
		String parStr = "r=" + str;
		DGeoLayer moveLayer = (DGeoLayer) moveLayers.elementAt(idx);
		//build a layer with simplified trajectories
		Vector simpObj = new Vector(moveLayer.getObjectCount(), 1);
		for (int i = 0; i < moveLayer.getObjectCount(); i++) {
			DGeoObject gobj = moveLayer.getObject(i);
			if (gobj instanceof DMovingObject) {
				DMovingObject mobj = (DMovingObject) gobj;
				Vector smTrack = TrUtil.getSmoothTrajectory(mobj.getTrack(), rad, geoFactorX, geoFactorY);
				DMovingObject sobj = (DMovingObject) mobj.makeCopy();
				if (smTrack != null) {
					sobj.setTrack(smTrack);
				}
				simpObj.addElement(sobj);
			}
			if ((i + 1) % 100 == 0) {
				showMessage((i + 1) + " trajectories processed", false);
			}
		}
		showMessage("Finished smoothing trajectories", false);
		DGeoLayer sLayer = new DGeoLayer();
		sLayer.setGeographic(moveLayer.isGeographic());
		sLayer.setType(Geometry.line);
		sLayer.setName("Smoothed " + moveLayer.getName() + " (" + parStr + ")");
		sLayer.setGeoObjects(simpObj, true);
		sLayer.setHasMovingObjects(true);
		DrawingParameters dp = sLayer.getDrawingParameters();
		if (dp == null) {
			dp = new DrawingParameters();
			sLayer.setDrawingParameters(dp);
		}
		dp.lineColor = Color.getHSBColor((float) Math.random(), 1 - 0.2f * (float) Math.random(), 1 - 0.2f * (float) Math.random());
		dp.fillContours = false;
		DataLoader dLoader = core.getDataLoader();
		dLoader.addMapLayer(sLayer, -1);
		DataTable dtTraj = TrajectoriesTableBuilder.makeTrajectoryDataTable(sLayer.getObjects());
		dtTraj.setName(sLayer.getName() + ": general data");

		int trTblN = dLoader.addTable(dtTraj);
		sLayer.setDataTable(dtTraj);
		dLoader.setLink(sLayer, trTblN);
		sLayer.setLinkedToTable(true);
		sLayer.setThematicFilter(dtTraj.getObjectFilter());
		sLayer.setLinkedToTable(true);
		if (sLayer.hasTimeReferences()) {
			dLoader.processTimeReferencedObjectSet(sLayer);
			dLoader.processTimeReferencedObjectSet(dtTraj);
		}
	}
}
