package spade.analysis.tools.moves;

import java.awt.Checkbox;
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
import spade.analysis.tools.DataAnalyser;
import spade.lib.basicwin.Centimeter;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Dialogs;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.OKDialog;
import spade.lib.util.GeoDistance;
import spade.lib.util.IdMaker;
import spade.lib.util.IntArray;
import spade.lib.util.StringUtil;
import spade.time.TimeMoment;
import spade.time.TimeReference;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.database.SpatialEntity;
import spade.vis.dataview.ShowRecManager;
import spade.vis.dmap.DAggregateLayer;
import spade.vis.dmap.DAggregateObject;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;
import spade.vis.dmap.DLayerManager;
import spade.vis.dmap.DMovingObject;
import spade.vis.dmap.DrawingParameters;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.RealRectangle;
import spade.vis.space.GeoLayer;
import spade.vis.space.LayerManager;
import data_load.DataManager;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Apr 10, 2008
 * Time: 1:51:15 PM
 * A tool to search for interactions between ttrajectories.
 * An interaction is defined as close positions (within a specified
 * threshold distThr) during certain time (not less than a specified
 * threshold timeThr).
 */
public class InteractionsSearchTool implements DataAnalyser {

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
		boolean geo = false, hasDates = false;
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
		mainP.add(new Label("Select the layer with trajectories:"));
		List list = new List(Math.max(moveLayers.size() + 1, 5));
		for (int i = 0; i < moveLayers.size(); i++) {
			list.add(((DGeoLayer) moveLayers.elementAt(i)).getName());
		}
		list.select(0);
		mainP.add(list);

		double wh[] = DGeoLayer.getExtentXY(minx, miny, maxx, maxy, geo);
		float width = (float) wh[0], height = (float) wh[1];
		float geoFactor = 1f;
		if (geo) {
			geoFactor = width / (maxx - minx);
		}
		float dist = Math.min(width, height) / 200;
		float factor = 1;
		if (dist > 1) {
			while (dist >= 10) {
				factor *= 10;
				dist /= 10;
			}
		} else {
			while (dist < 1) {
				factor /= 10;
				dist *= 10;
			}
		}
		if (dist < 3) {
			dist = 1;
		} else if (dist < 7) {
			dist = 5;
		} else {
			dist = 10;
		}
		dist *= factor;
		String distStr = StringUtil.floatToStr(dist, 0, dist * 10);
		Panel p = new Panel();
		GridBagLayout gridbag = new GridBagLayout();
		p.setLayout(gridbag);
		GridBagConstraints c = new GridBagConstraints();
		c.weightx = 1.0f;
		c.weighty = 1.0f;
		c.fill = GridBagConstraints.HORIZONTAL;
		String unit = getTimeUnit((DGeoLayer) moveLayers.elementAt(0));
		Label l = new Label("Time threshold" + ((unit == null) ? ":" : " (" + unit + "):"));
		c.gridwidth = 3;
		gridbag.setConstraints(l, c);
		p.add(l);
		TextField timeTF = new TextField(String.valueOf(getSuitableTimeThreshold((DGeoLayer) moveLayers.elementAt(0))), 10);
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(timeTF, c);
		p.add(timeTF);
		l = new Label("Distance threshold:");
		c.gridwidth = 3;
		gridbag.setConstraints(l, c);
		p.add(l);
		TextField distTF = new TextField(distStr, 10);
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(distTF, c);
		p.add(distTF);
		l = new Label("Buffer distance:");
		c.gridwidth = 3;
		gridbag.setConstraints(l, c);
		p.add(l);
		TextField bufTF = new TextField(distStr, 10);
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(bufTF, c);
		p.add(bufTF);
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
		float sc = core.getUI().getMapViewer(core.getUI().getCurrentMapN()).getMapDrawer().getMapContext().getPixelValue() * cm.getMinimumSize().width * ((geo) ? geoFactor : ((DLayerManager) lman).user_factor);
		pp.add(new Label(StringUtil.floatToStr(sc, 2) + " " + ((geo) ? "m" : ((DLayerManager) lman).getUserUnit())));
		gridbag.setConstraints(pp, c);
		p.add(pp);
		mainP.add(p);
		Checkbox onlyActiveCB = new Checkbox("use only active (after filtering) trajectories", true);
		mainP.add(onlyActiveCB);

		OKDialog dia = new OKDialog(core.getUI().getMainFrame(), "Detect interactions", true);
		dia.addContent(mainP);
		dia.show();
		if (dia.wasCancelled())
			return;
		int idx = list.getSelectedIndex();
		if (idx < 0)
			return;
		int timeThr = 0;
		String str = timeTF.getText();
		String parStr = "time<=" + str;
		if (str != null) {
			try {
				timeThr = Integer.parseInt(str);
			} catch (NumberFormatException nfe) {
			}
		}
		if (timeThr < 0) {
			timeThr = 0;
		}
		float distThr = 0f;
		str = distTF.getText();
		if (str != null) {
			try {
				distThr = Float.valueOf(str).floatValue();
			} catch (NumberFormatException nfe) {
			}
		}
		parStr += "; distance<=" + str;
		str = bufTF.getText();
		float buf = 0f;
		if (str != null) {
			try {
				buf = Float.valueOf(str).floatValue();
			} catch (NumberFormatException nfe) {
			}
		}
		DGeoLayer moveLayer = (DGeoLayer) moveLayers.elementAt(idx);

		InteractionFinder iFinder = new InteractionFinder();
		showMessage("Data preparation...", false);
		Vector trajectories = null;
		if (onlyActiveCB.getState()) {
			trajectories = new Vector(moveLayer.getObjectCount(), 10);
			for (int i = 0; i < moveLayer.getObjectCount(); i++)
				if (moveLayer.isObjectActive(i)) {
					trajectories.addElement(moveLayer.getObject(i));
				}
		} else {
			trajectories = moveLayer.getObjects();
		}
		if (!iFinder.setTrajectories(trajectories)) {
			String err = iFinder.getErrorMessage();
			if (err == null) {
				err = "Failed to prepare the data for the search!";
			}
			showMessage(err, true);
			return;
		}
		showMessage("The data have been prepared. Start searching...", false);
		iFinder.setNotificationLine(core.getUI().getStatusLine());
		Vector inter = iFinder.findPairwiseInteractions(distThr, moveLayer.isGeographic(), timeThr);
		if (inter == null || inter.size() < 1) {
			showMessage("No interactions detected!", true);
			return;
		}
		showMessage(inter.size() + " pairwise interactions detected!", false);

		p = new Panel(new ColumnLayout());
		Checkbox pairsCB = new Checkbox("build map layer with pairwise interactions", false);
		p.add(pairsCB);
		Checkbox moreCB = new Checkbox("unite overlapping pairwise interactions", true);
		p.add(moreCB);
		p.add(new Line(false));
		Checkbox ignoreCB = new Checkbox("ignore interactions with duration below", false);
		TextField durTF = new TextField("1", 3);
		pp = new Panel(new FlowLayout());
		pp.add(ignoreCB);
		pp.add(durTF);
		p.add(pp);
		dia = new OKDialog(core.getUI().getMainFrame(), "Unite interactions", true);
		dia.addContent(p);
		dia.show();
		if (dia.wasCancelled() || (!pairsCB.getState() && !moreCB.getState()))
			return;
		int minDur = 0;
		if (ignoreCB.getState()) {
			str = durTF.getText();
			if (str != null) {
				try {
					minDur = Integer.parseInt(str.trim());
				} catch (Exception e) {
					minDur = 0;
				}
			}
		}
		if (minDur > 0) {
			parStr += "; duration>=" + minDur;
		}
		String name = "Pairwise interactions from " + moveLayer.getName() + " (" + parStr + ")";
		DAggregateLayer aggrLayer = null;
		for (int i = 0; i < 2; i++) {
			if (i == 0)
				if (!pairsCB.getState()) {
					continue;
				} else {
					;
				}
			else {
				if (!moreCB.getState()) {
					break;
				}
				int oldNInter = inter.size();
				showMessage("Uniting interactions...", false);
				inter = iFinder.uniteInteractions(inter);
				if (inter.size() >= oldNInter) {
					showMessage("No overlapping interactions found!", true);
					if (pairsCB.getState()) {
						break;
					}
				} else {
					showMessage(inter.size() + " interactions built out of " + oldNInter, false);
					name = "Interactions from " + moveLayer.getName() + " (" + parStr + ")";
				}
			}
			Vector aggrObjects = makeAggregateObjects(inter, trajectories, buf, moveLayer.isGeographic(), minDur);
			if (aggrObjects == null) {
				String msg = "No " + ((i == 0) ? "pairwise" : "united") + " interactions with required duration have been found. " + "Build a layer with all interactions?";
				if (!Dialogs.askYesOrNo(core.getUI().getMainFrame(), msg, "Low duration!")) {
					continue;
				}
			}
			DataLoader dataLoader = core.getDataLoader();
			aggrLayer = new DAggregateLayer();
			aggrLayer.setGeographic(moveLayer.isGeographic());
			aggrLayer.setType(Geometry.area);
			aggrLayer.setName(name);
			aggrLayer.setGeoObjects(aggrObjects, true);
			aggrLayer.setSourceLayer(moveLayer);
			DrawingParameters dp = aggrLayer.getDrawingParameters();
			if (dp == null) {
				dp = new DrawingParameters();
				aggrLayer.setDrawingParameters(dp);
			}
			dp.lineColor = Color.getHSBColor((float) Math.random(), 1 - 0.2f * (float) Math.random(), 1 - 0.2f * (float) Math.random());
			dp.fillContours = false;
			dataLoader.addMapLayer(aggrLayer, -1);
			DataTable aggrTable = aggrLayer.constructTableWithStatistics();
			if (aggrTable != null) {
				aggrTable.setName("Data about " + aggrLayer.getName());
				int tblN = dataLoader.addTable(aggrTable);
				aggrTable.setEntitySetIdentifier(aggrLayer.getEntitySetIdentifier());
				dataLoader.setLink(aggrLayer, tblN);
				aggrLayer.setLinkedToTable(true);
				ShowRecManager recMan = null;
				if (dataLoader instanceof DataManager) {
					recMan = ((DataManager) dataLoader).getShowRecManager(tblN);
				}
				if (recMan != null) {
					Vector showAttr = new Vector(aggrTable.getAttrCount(), 10);
					for (int j = 0; j < aggrTable.getAttrCount(); j++) {
						showAttr.addElement(aggrTable.getAttributeId(j));
					}
					recMan.setPopupAddAttrs(showAttr);
				}
			}
			dataLoader.processTimeReferencedObjectSet(aggrLayer);
			dataLoader.processTimeReferencedObjectSet(aggrTable);
			aggrLayer.setSupervisor(core.getSupervisor());
		}
		if (inter != null
				&& inter.size() > 0
				&& moveLayer.getThematicData() != null
				&& (moveLayer.getThematicData() instanceof DataTable)
				&& Dialogs.askYesOrNo(core.getUI().getMainFrame(), "Compute new attributes of the trajectories based on their interactions " + "(number of interactions, number and identifiers of interacting trajectories)?",
						"New attributes of trajectories")) {
			Vector trInt = new Vector(moveLayer.getObjectCount());
			for (int i = 0; i < moveLayer.getObjectCount(); i++) {
				trInt.addElement(null);
			}
			for (int i = 0; i < inter.size(); i++) {
				InteractionData idata = (InteractionData) inter.elementAt(i);
				if (minDur > 0 && idata.t2.subtract(idata.t1) < minDur) {
					continue;
				}
				int trIdxs[] = idata.getAllTrIndexes();
				if (trIdxs == null) {
					continue;
				}
				for (int trIdx : trIdxs) {
					if (trInt.elementAt(trIdx) == null) {
						trInt.setElementAt(new Vector(100, 50), trIdx);
					}
					((Vector) trInt.elementAt(trIdx)).addElement(idata);
				}
			}
			DataTable table = (DataTable) moveLayer.getThematicData();
			String aNames[] = { "N interactions (" + parStr + ")", "N trajectories (" + parStr + ")", "Ids of trajectories (" + parStr + ")" };
			int aIdx0 = table.getAttrCount();
			for (int i = 0; i < aNames.length; i++) {
				table.addAttribute(aNames[i], IdMaker.makeId(aNames[i], table), (i < 2) ? AttributeTypes.integer : AttributeTypes.character);
			}
			for (int i = 0; i < trInt.size(); i++) {
				DGeoObject gobj = moveLayer.getObject(i);
				DataRecord rec = (DataRecord) gobj.getData();
				if (trInt.elementAt(i) != null) {
					Vector vint = (Vector) trInt.elementAt(i);
					rec.setNumericAttrValue(vint.size(), String.valueOf(vint.size()), aIdx0);
					IntArray trNs = new IntArray(vint.size() * 5, 50);
					for (int j = 0; j < vint.size(); j++) {
						InteractionData idata = (InteractionData) vint.elementAt(j);
						int trIdxs[] = idata.getAllTrIndexes();
						for (int trIdx : trIdxs)
							if (trIdx != i && trNs.indexOf(trIdx) < 0) {
								trNs.addElement(trIdx);
							}
					}
					rec.setNumericAttrValue(trNs.size(), String.valueOf(trNs.size()), aIdx0 + 1);
					str = null;
					for (int j = 0; j < trNs.size(); j++) {
						String id = moveLayer.getObjectId(trNs.elementAt(j));
						if (j == 0) {
							str = id;
						} else {
							str += ";" + id;
						}
					}
					rec.setAttrValue(str, aIdx0 + 2);
				} else {
					rec.setNumericAttrValue(0, "0", aIdx0);
					rec.setNumericAttrValue(0, "0", aIdx0 + 1);
				}
			}
			aNames = Dialogs.editStringValues(core.getUI().getMainFrame(), null, aNames, "Edit the names of the new attributes if needed", "Attribute names", true);
			if (aNames != null) {
				for (int i = 0; i < aNames.length; i++)
					if (aNames[i] != null) {
						table.getAttribute(aIdx0 + i).setName(aNames[i]);
					}
			}
		}
		if (aggrLayer != null) {
			//visualise the interactions
			InteractionsTimeLineViewPanel tlv = new InteractionsTimeLineViewPanel(aggrLayer, aggrLayer.getSourceLayer());
			tlv.setName(aggrLayer.getName());
			tlv.setSupervisor(core.getSupervisor());
			core.getDisplayProducer().showGraph(tlv);
		}
	}

	/**
	 * From the given interactions (instances of InteractionData) makes
	 * aggregate objects - instances of DAggregateObject
	 * @param inter - contains instances of InteractionData
	 * @param trajectories - source trajectories, instances of DMovingObject
	 * @param bufferDist - buffer distance around the places of the interactions
	 * @param geo - whether the coordinates should be treated as geographic
	 *              (x as longituge and y as latitude)
	 * @return  vector containing instances of DAggregateObject
	 */
	protected Vector makeAggregateObjects(Vector inter, Vector trajectories, float bufferDist, boolean geo, int minDuration) {
		if (inter == null || inter.size() < 1)
			return null;
		Vector aggrObj = new Vector(inter.size(), 10);
		float bufferDistInDataUnits = bufferDist;
		if (geo && bufferDist > 0) {
			InteractionData idata = (InteractionData) inter.elementAt(0);
			double x0 = (idata.x1 + idata.x2) / 2, y0 = (idata.y1 + idata.y2) / 2;
			double d1 = GeoDistance.geoDist(x0, y0, x0 + 1, y0), d2 = GeoDistance.geoDist(x0, y0, x0, y0 + 1), d = (d1 < d2) ? d1 : d2;
			bufferDistInDataUnits /= d;
		}
		for (int i = 0; i < inter.size(); i++) {
			InteractionData idata = (InteractionData) inter.elementAt(i);
			if (minDuration > 0 && idata.t2.subtract(idata.t1) < minDuration) {
				continue;
			}
			int trIdxs[] = idata.getAllTrIndexes();
			if (trIdxs == null) {
				continue;
			}
			SpatialEntity spe = new SpatialEntity(String.valueOf(i + 1));
			spe.setGeometry(new RealRectangle(idata.x1 - bufferDistInDataUnits, idata.y1 - bufferDistInDataUnits, idata.x2 + bufferDistInDataUnits, idata.y2 + bufferDistInDataUnits));
			DAggregateObject obj = new DAggregateObject();
			obj.setup(spe);
			obj.setExtraInfo(idata);
			aggrObj.addElement(obj);
			for (int trIdx : trIdxs) {
				int minmax[] = idata.getMinMaxPointIndexes(trIdx);
				if (minmax == null) {
					continue;
				}
				DMovingObject mobj = (DMovingObject) trajectories.elementAt(trIdx);
				TimeMoment t1 = null, t2 = null;
				TimeReference tr = mobj.getPositionTime(minmax[0]);
				if (tr != null) {
					t1 = tr.getValidFrom();
				}
				tr = mobj.getPositionTime(minmax[1]);
				if (tr != null) {
					t2 = tr.getValidUntil();
				}
				if (t2 == null) {
					t2 = tr.getValidFrom();
				}
				obj.addMember(mobj, t1, t2);
			}
		}
		if (aggrObj.size() < 1)
			return null;
		return aggrObj;
	}

	protected int getSuitableTimeThreshold(DGeoLayer moveLayer) {
		if (moveLayer == null || moveLayer.getObjectCount() < 1)
			return 1;
		float min = Integer.MAX_VALUE, max = 1;
		int nObj = moveLayer.getObjectCount(), nSampled = Math.min(5, nObj);
		IntArray tested = new IntArray(nSampled, 1);
		for (int i = 0; i < nSampled; i++) {
			int idx = (int) (Math.random() * (nObj - 1));
			boolean wasTested = tested.indexOf(idx) >= 0;
			for (int j = 0; j < 5 && wasTested; j++) {
				idx = (int) (Math.random() * (nObj - 1));
				wasTested = tested.indexOf(idx) >= 0;
			}
			if (wasTested) {
				break;
			}
			tested.addElement(idx);
			if (moveLayer.getObject(idx) instanceof DMovingObject) {
				DMovingObject mObj = (DMovingObject) moveLayer.getObject(idx);
				int nPoints = mObj.getTrack().size();
				if (nPoints < 2) {
					++nSampled;
					continue;
				}
				TimeMoment t1 = mObj.getStartTime(), t2 = mObj.getEndTime();
				float aveT = 1f * t2.subtract(t1) / (nPoints - 1);
				if (aveT < min) {
					min = aveT;
				}
				if (aveT > max) {
					max = aveT;
				}
			}
		}
		if (min > max)
			return 1;
		return Math.round((min + max) / 2);
	}

	protected String getTimeUnit(DGeoLayer moveLayer) {
		if (moveLayer == null || moveLayer.getObjectCount() < 1)
			return null;
		for (int i = 0; i < moveLayer.getObjectCount(); i++)
			if (moveLayer.getObject(i) instanceof DMovingObject) {
				DMovingObject mObj = (DMovingObject) moveLayer.getObject(i);
				TimeMoment t = mObj.getStartTime();
				if (t != null)
					return t.getUnit();
			}
		return null;
	}

	protected void showMessage(String msg, boolean error) {
		if (core != null && core.getUI() != null) {
			core.getUI().showMessage(msg, error);
		} else if (error) {
			System.out.println("!--> " + msg);
		}
	}
}
