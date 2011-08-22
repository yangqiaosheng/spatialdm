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

import spade.analysis.system.ESDACore;
import spade.analysis.tools.DataAnalyser;
import spade.lib.basicwin.Centimeter;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.OKDialog;
import spade.lib.util.StringUtil;
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
 * Date: Apr 3, 2008
 * Time: 2:10:28 PM
 * Builds simplified trajectories by building generalized positions (circles)
 * around the characteristic points of the trajectories.
 */
public class TrajectoriesSimplifier implements DataAnalyser {

	protected ESDACore core = null;

	/**
	 * Returns true when the tool has everything necessary for its operation.
	 * For example, if the tool manages a number of analysis methods, it should
	 * check whether the class for at least one method is available.
	 * A TrajectoriesGeneraliser always returns true.
	 */
	@Override
	public boolean isValid(ESDACore core) {
		return true;
	}

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
		mainP.add(new Label("Select the layer with trajectories to simplify:"));
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
		float defMinRad = Math.min(width, height) / 200;
		float factor = 1;
		if (defMinRad > 1) {
			while (defMinRad >= 10) {
				factor *= 10;
				defMinRad /= 10;
			}
		} else {
			while (defMinRad < 1) {
				factor /= 10;
				defMinRad *= 10;
			}
		}
		if (defMinRad < 3) {
			defMinRad = 1;
		} else if (defMinRad < 7) {
			defMinRad = 5;
		} else {
			defMinRad = 10;
		}
		defMinRad *= factor;
		String minRadStr = StringUtil.floatToStr(defMinRad, 0, defMinRad * 10), maxRadStr = StringUtil.floatToStr(defMinRad * 2, 0, defMinRad * 10);
		Panel p = new Panel();
		GridBagLayout gridbag = new GridBagLayout();
		p.setLayout(gridbag);
		GridBagConstraints c = new GridBagConstraints();
		c.weightx = 1.0f;
		c.weighty = 1.0f;
		c.fill = GridBagConstraints.HORIZONTAL;
		Label l = new Label("Minimum angle of direction change (degrees):");
		c.gridwidth = 3;
		gridbag.setConstraints(l, c);
		p.add(l);
		TextField angleTF = new TextField("30", 10);
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(angleTF, c);
		p.add(angleTF);
		l = new Label("Minimum duration of a stop:");
		c.gridwidth = 3;
		gridbag.setConstraints(l, c);
		p.add(l);
		TextField timeGapTF = new TextField("60", 10);
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(timeGapTF, c);
		p.add(timeGapTF);
		l = new Label("Minimum radius around a position:");
		c.gridwidth = 3;
		gridbag.setConstraints(l, c);
		p.add(l);
		TextField minRadTF = new TextField(minRadStr, 10);
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(minRadTF, c);
		p.add(minRadTF);
		l = new Label("Maximum radius around a position:");
		c.gridwidth = 3;
		gridbag.setConstraints(l, c);
		p.add(l);
		TextField maxRadTF = new TextField(maxRadStr, 10);
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(maxRadTF, c);
		p.add(maxRadTF);
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

		OKDialog dia = new OKDialog(core.getUI().getMainFrame(), "Simplify trajectories", true);
		dia.addContent(mainP);
		dia.show();
		if (dia.wasCancelled())
			return;
		int idx = list.getSelectedIndex();
		if (idx < 0)
			return;
		float angle = 0f;
		String str = angleTF.getText();
		String parStr = "angle " + str;
		if (str != null) {
			try {
				angle = Float.valueOf(str).floatValue();
			} catch (NumberFormatException nfe) {
			}
		}
		if (angle < 10) {
			angle = 10;
		}
		double angleRadian = angle * Math.PI / 180;
		long stopTime = 0l;
		str = timeGapTF.getText();
		parStr += "; stop time " + str;
		if (str != null) {
			try {
				stopTime = Long.valueOf(str).longValue();
			} catch (NumberFormatException nfe) {
			}
		}
		if (stopTime < 0) {
			stopTime = 0;
		}
		float minRad = 0f;
		str = minRadTF.getText();
		if (str != null) {
			try {
				minRad = Float.valueOf(str).floatValue();
			} catch (NumberFormatException nfe) {
			}
		}
		parStr += "; radius " + str;
		float maxRad = 0f;
		str = maxRadTF.getText();
		if (str != null) {
			try {
				maxRad = Float.valueOf(str).floatValue();
			} catch (NumberFormatException nfe) {
			}
		}
		parStr += "-" + str;
		float minRadOrig = minRad, maxRadOrig = maxRad;
		minRad /= geoFactorX;
		maxRad /= geoFactorX;
		DGeoLayer moveLayer = (DGeoLayer) moveLayers.elementAt(idx);
		//preliminary simplification: extraction of characteristic points
		Vector points = new Vector(100, 50);
		for (int i = 0; i < moveLayer.getObjectCount(); i++) {
			DGeoObject gobj = moveLayer.getObject(i);
			if (gobj instanceof DMovingObject) {
				DMovingObject mobj = (DMovingObject) gobj;
				Vector souTrack = mobj.getTrack();
				if (souTrack == null || souTrack.size() < 2) {
					continue;
				}
				//Vector genTrack=mobj.generaliseTrack(geoFactor,stopTime,angleRadian,minRad,maxRad);
				Vector genTrack = TrUtil.getCharacteristicPoints(mobj.getTrack(), angleRadian, stopTime, minRadOrig, maxRadOrig, geoFactorX, geoFactorY);
				if (genTrack == null) {
					continue;
				}
				mobj.setGeneralisedTrack(genTrack);
			}
		}

		//build a layer with simplified trajectories
		Vector simpObj = new Vector(moveLayer.getObjectCount(), 1);
		for (int i = 0; i < moveLayer.getObjectCount(); i++) {
			DGeoObject gobj = moveLayer.getObject(i);
			if (gobj instanceof DMovingObject) {
				DMovingObject mobj = (DMovingObject) gobj;
				Vector genTrack = mobj.getGeneralisedTrack();
				DMovingObject sobj = (DMovingObject) mobj.makeCopy();
				if (genTrack != null) {
					sobj.setTrack(genTrack);
				}
				simpObj.addElement(sobj);
			}
		}
		DGeoLayer sLayer = new DGeoLayer();
		sLayer.setGeographic(moveLayer.isGeographic());
		sLayer.setType(Geometry.line);
		sLayer.setName("Simplified " + moveLayer.getName() + " (" + parStr + ")");
		sLayer.setGeoObjects(simpObj, true);
		sLayer.setHasMovingObjects(true);
		DrawingParameters dp = sLayer.getDrawingParameters();
		if (dp == null) {
			dp = new DrawingParameters();
			sLayer.setDrawingParameters(dp);
		}
		dp.lineColor = Color.getHSBColor((float) Math.random(), 1 - 0.2f * (float) Math.random(), 1 - 0.2f * (float) Math.random());
		dp.fillContours = false;
		core.getDataLoader().addMapLayer(sLayer, -1);
		sLayer.setEntitySetIdentifier(moveLayer.getEntitySetIdentifier());
		sLayer.setDataTable(moveLayer.getThematicData());
		sLayer.setObjectFilter(moveLayer.getObjectFilter());

	}

	protected void showMessage(String msg, boolean error) {
		if (core != null && core.getUI() != null) {
			core.getUI().showMessage(msg, error);
		} else if (error) {
			System.out.println("!--> " + msg);
		}
	}
}
