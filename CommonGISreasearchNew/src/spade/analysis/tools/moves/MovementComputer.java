package spade.analysis.tools.moves;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.TextField;
import java.util.Vector;

import spade.analysis.system.DataLoader;
import spade.analysis.system.ESDACore;
import spade.analysis.tools.DataAnalyser;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Dialogs;
import spade.lib.basicwin.OKDialog;
import spade.lib.util.DoubleArray;
import spade.lib.util.IdMaker;
import spade.lib.util.NumValManager;
import spade.time.Date;
import spade.time.TimeMoment;
import spade.time.TimeReference;
import spade.time.ui.GetIntervalLengthUI;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.database.Parameter;
import spade.vis.database.SpatialEntity;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;
import spade.vis.dmap.DMovingObject;
import spade.vis.geometry.GeoComp;
import spade.vis.geometry.RealPoint;
import spade.vis.geometry.RealRectangle;
import spade.vis.space.GeoLayer;
import spade.vis.space.LayerManager;
import core.ActionDescr;
import core.ResultDescr;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Jun 18, 2008
 * Time: 5:10:00 PM
 * An interface to various computational functions on trajectories.
 */
public class MovementComputer implements DataAnalyser {

	protected ESDACore core = null;

	/**
	 * Returns true when the tool has everything necessary for its operation.
	 * For example, if the tool manages a number of analysis methods, it should
	 * check whether the class for at least one method is available.
	 * A TrajectoriesGeneraliser always returns true.
	 */
	public boolean isValid(ESDACore core) {
		return true;
	}

	/**
	 * This method constructs and starts the tool. Everything the tool may need
	 * for integration with other components of the system can be received
	 * from the system's core passed as an argument.
	 */
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
		mainP.add(new Label("Select the layer with trajectories:"));
		List list = new List(Math.max(moveLayers.size() + 1, 5));
		for (int i = 0; i < moveLayers.size(); i++) {
			list.add(((DGeoLayer) moveLayers.elementAt(i)).getName());
		}
		list.select(0);
		mainP.add(list);
		CheckboxGroup cbg = new CheckboxGroup();
		Checkbox cbClean = new Checkbox("Clean trajectories by removing outliers", true, cbg);
		mainP.add(cbClean);
		Checkbox cbLengthFromTo = new Checkbox("Compute the path length for a time interval or sequence of intervals", false, cbg);
		mainP.add(cbLengthFromTo);
		Checkbox cbDur = new Checkbox("Get the trip duration", false, cbg);
		mainP.add(cbDur);
		Checkbox cbPoints = new Checkbox("Get the coordinates of a desired number of trajectory points", false, cbg);
		mainP.add(cbPoints);
		Checkbox cbMedoid = new Checkbox("Get the coordinates of the medoid of each trajectory", false, cbg);
		mainP.add(cbMedoid);
		Checkbox cbDistStat = new Checkbox("Compute the statistics of the distances between the positions", false, cbg);
		mainP.add(cbDistStat);
		Checkbox cbSpeedStat = new Checkbox("Compute the statistics of the speeds in the points", false, cbg);
		mainP.add(cbSpeedStat);
		Checkbox cbSpPosFeatures = new Checkbox("Compute a set of positional features", false, cbg);
		mainP.add(cbSpPosFeatures);
		Checkbox cbGeomFeatures = new Checkbox("Compute a set of geometric features", false, cbg);
		mainP.add(cbGeomFeatures);
		OKDialog dia = new OKDialog(core.getUI().getMainFrame(), "Computations on trajectories", true);
		dia.addContent(mainP);
		dia.show();
		if (dia.wasCancelled())
			return;
		int idx = list.getSelectedIndex();
		if (idx < 0)
			return;
		DGeoLayer moveLayer = (DGeoLayer) moveLayers.elementAt(idx);
		ActionDescr aDescr = new ActionDescr();
		aDescr.aName = "Computation for trajectories: " + cbg.getSelectedCheckbox().getLabel();
		aDescr.addParamValue("Layer id", moveLayer.getContainerIdentifier());
		aDescr.addParamValue("Layer name", moveLayer.getName());
		aDescr.startTime = System.currentTimeMillis();
		if (cbClean.getState()) {
			OutlierRemover oRem = new OutlierRemover();
			oRem.run(moveLayer, core);
		} else {
			DataTable table = null;
			boolean newTable = false;
			if (moveLayer.getThematicData() != null && (moveLayer.getThematicData() instanceof DataTable)) {
				table = (DataTable) moveLayer.getThematicData();
			} else {
				String tblName = moveLayer.getName();
/*
        tblName=Dialogs.askForStringValue(core.getUI().getMainFrame(),"Table name?",
                         tblName,
                         "A new table will be created and attached to the layer",
                         "New table",true);
        if (tblName==null) return;
*/
				table = new DataTable();
				table.setName(tblName);
				for (int i = 0; i < moveLayer.getObjectCount(); i++) {
					DGeoObject gobj = moveLayer.getObject(i);
					DataRecord rec = new DataRecord(gobj.getIdentifier(), gobj.getName());
					rec.setTimeReference(gobj.getTimeReference());
					table.addDataRecord(rec);
					gobj.setThematicData(rec);
				}
				ResultDescr res = new ResultDescr();
				res.product = table;
				aDescr.addResultDescr(res);
				aDescr.startTime = System.currentTimeMillis();
			}
			Vector attr = new Vector(50, 50);
			int nAttrPrev = table.getAttrCount();
			boolean resultsArePositions = false;

			if (cbLengthFromTo.getState()) {
				attr = getPathLengthsForTimeInterval(moveLayer, table);
			} else if (cbDur.getState()) {
				table.addAttribute("Trip duration", IdMaker.makeId("trip_duration", table), AttributeTypes.integer);
				int durCN = table.getAttrCount() - 1;
				attr.addElement(table.getAttribute(durCN));
				for (int i = 0; i < moveLayer.getObjectCount(); i++) {
					DGeoObject gobj = moveLayer.getObject(i);
					if (gobj instanceof DMovingObject) {
						DMovingObject mobj = (DMovingObject) gobj;
						if (mobj.getData() == null || !(mobj.getData() instanceof DataRecord)) {
							continue;
						}
						DataRecord rec = (DataRecord) mobj.getData();
						long len = mobj.getDuration();
						rec.setNumericAttrValue(len, String.valueOf(len), durCN);
					}
				}
			} else if (cbPoints.getState()) {
				resultsArePositions = true;
				mainP = new Panel(new ColumnLayout());
				mainP.add(new Label("Get the coordinates of the points:"));
				Checkbox startCB = new Checkbox("start point", true);
				mainP.add(startCB);
				Checkbox endCB = new Checkbox("end point", true);
				mainP.add(endCB);
				TextField numTF = new TextField("3", 2);
				Panel p = new Panel(new BorderLayout());
				p.add(numTF, BorderLayout.EAST);
				p.add(new Label("intermediate points"), BorderLayout.CENTER);
				mainP.add(p);
				dia = new OKDialog(core.getUI().getMainFrame(), "Coordinates of points", true);
				dia.addContent(mainP);
				dia.show();
				if (dia.wasCancelled())
					return;
				int nInter = 0;
				String str = numTF.getText();
				if (str != null) {
					try {
						nInter = Integer.parseInt(str);
					} catch (NumberFormatException e) {
					}
				}
				if (nInter < 0) {
					nInter = 0;
				}
				int firstIdx = table.getAttrCount(), startXCN = -1, endXCN = -1, interCN0 = -1;
				if (startCB.getState()) {
					startXCN = table.getAttrCount();
					table.addAttribute("start X", IdMaker.makeId("startX", table), AttributeTypes.real);
					table.addAttribute("start Y", IdMaker.makeId("startY", table), AttributeTypes.real);
				}
				if (nInter > 0) {
					interCN0 = table.getAttrCount();
					for (int i = 0; i < nInter; i++) {
						String prefix = "point " + (i + 1) + " ", nameX = prefix + "X", nameY = prefix + "Y";
						table.addAttribute(nameX, IdMaker.makeId(nameX, table), AttributeTypes.real);
						table.addAttribute(nameY, IdMaker.makeId(nameY, table), AttributeTypes.real);
					}
				}
				if (endCB.getState()) {
					endXCN = table.getAttrCount();
					table.addAttribute("end X", IdMaker.makeId("endX", table), AttributeTypes.real);
					table.addAttribute("end Y", IdMaker.makeId("endY", table), AttributeTypes.real);
				}
				for (int i = firstIdx; i < table.getAttrCount(); i++) {
					attr.addElement(table.getAttribute(i));
				}

				for (int i = 0; i < moveLayer.getObjectCount(); i++) {
					DGeoObject gobj = moveLayer.getObject(i);
					if (gobj instanceof DMovingObject) {
						DMovingObject mobj = (DMovingObject) gobj;
						if (mobj.getData() == null || !(mobj.getData() instanceof DataRecord)) {
							continue;
						}
						Vector track = mobj.getTrack();
						if (track == null || track.size() < 1) {
							continue;
						}
						DataRecord rec = (DataRecord) mobj.getData();

						if (startXCN >= 0) {
							RealPoint p0 = ((SpatialEntity) track.elementAt(0)).getCentre();
							rec.setNumericAttrValue(p0.x, startXCN);
							rec.setNumericAttrValue(p0.y, startXCN + 1);
						}
						if (endXCN >= 0) {
							RealPoint pEnd = ((SpatialEntity) track.elementAt(track.size() - 1)).getCentre();
							rec.setNumericAttrValue(pEnd.x, endXCN);
							rec.setNumericAttrValue(pEnd.y, endXCN + 1);
						}
						if (nInter > 0) {
							int pIdxs[] = mobj.getNIntermPointsEqDist(nInter);
							if (pIdxs != null) {
								for (int j = 0; j < nInter; j++) {
									RealPoint pt = ((SpatialEntity) track.elementAt(pIdxs[j])).getCentre();
									rec.setNumericAttrValue(pt.x, interCN0 + j * 2);
									rec.setNumericAttrValue(pt.y, interCN0 + j * 2 + 1);
								}
							}
						}
					}
				}
			} else if (cbMedoid.getState()) {
				resultsArePositions = true;
				int idx0 = table.getAttrCount();
				table.addAttribute("medoid X", IdMaker.makeId("medoidX", table), AttributeTypes.real);
				table.addAttribute("medoid Y", IdMaker.makeId("medoidY", table), AttributeTypes.real);
				int nTr = 0;
				for (int i = 0; i < moveLayer.getObjectCount(); i++) {
					DGeoObject gobj = moveLayer.getObject(i);
					if (gobj instanceof DMovingObject) {
						DMovingObject mobj = (DMovingObject) gobj;
						if (mobj.getData() == null || !(mobj.getData() instanceof DataRecord)) {
							continue;
						}
						Vector track = mobj.getTrack();
						if (track == null || track.size() < 1) {
							continue;
						}
						PointsInCircle pc = new PointsInCircle(1, 1);
						for (int j = 0; j < track.size(); j++) {
							pc.addPoint(((SpatialEntity) track.elementAt(j)).getCentre());
						}
						RealPoint mp = pc.getTrueMedoid();
						if (mp == null) {
							continue;
						}
						DataRecord rec = (DataRecord) mobj.getData();
						rec.setNumericAttrValue(mp.x, idx0);
						rec.setNumericAttrValue(mp.y, idx0 + 1);
					}
					++nTr;
					if (nTr % 10 == 0) {
						showMessage(nTr + " trajectories processed", false);
					}
				}
			} else if (cbDistStat.getState()) {
				table.addAttribute("Min distance between points", "min_distance_btw_points", AttributeTypes.real);
				int minCN = table.getAttrCount() - 1;
				attr.addElement(table.getAttribute(minCN));
				table.addAttribute("1st quartile distance between points", "q1_distance_btw_points", AttributeTypes.real);
				int q1CN = table.getAttrCount() - 1;
				attr.addElement(table.getAttribute(q1CN));
				table.addAttribute("Median distance between points", "median_distance_btw_points", AttributeTypes.real);
				int medCN = table.getAttrCount() - 1;
				attr.addElement(table.getAttribute(medCN));
				table.addAttribute("3rd quartile distance between points", "q3_distance_btw_points", AttributeTypes.real);
				int q3CN = table.getAttrCount() - 1;
				attr.addElement(table.getAttribute(q3CN));
				table.addAttribute("Max distance between points", "max_distance_btw_points", AttributeTypes.real);
				int maxCN = table.getAttrCount() - 1;
				attr.addElement(table.getAttribute(maxCN));
				if (geo) {
					for (int i = nAttrPrev; i < table.getAttrCount(); i++) {
						Attribute at = table.getAttribute(i);
						at.setName(at.getName() + " (m)");
					}
				}
				for (int i = 0; i < moveLayer.getObjectCount(); i++) {
					DGeoObject gobj = moveLayer.getObject(i);
					if (gobj instanceof DMovingObject) {
						DMovingObject mobj = (DMovingObject) gobj;
						if (mobj.getData() == null || !(mobj.getData() instanceof DataRecord)) {
							continue;
						}
						double distances[] = mobj.getDistances();
						if (distances == null || distances.length < 1) {
							continue;
						}
						DataRecord rec = (DataRecord) mobj.getData();
						if (distances.length == 1) {
							rec.setNumericAttrValue(distances[0], minCN);
							rec.setNumericAttrValue(distances[0], q1CN);
							rec.setNumericAttrValue(distances[0], medCN);
							rec.setNumericAttrValue(distances[0], q3CN);
							rec.setNumericAttrValue(distances[0], maxCN);
							continue;
						}
						DoubleArray dist = new DoubleArray(distances.length, 1);
						for (double distance : distances) {
							dist.addElement(distance);
						}
						int perc[] = { 0, 25, 50, 75, 100 };
						double stat[] = NumValManager.getPercentiles(dist, perc);
						if (stat == null) {
							continue;
						}
						rec.setNumericAttrValue(stat[0], minCN);
						rec.setNumericAttrValue(stat[1], q1CN);
						rec.setNumericAttrValue(stat[2], medCN);
						rec.setNumericAttrValue(stat[3], q3CN);
						rec.setNumericAttrValue(stat[4], maxCN);
					}
				}
			} else if (cbSpeedStat.getState()) {
				table.addAttribute("Min speed", IdMaker.makeId("min_speed", table), AttributeTypes.real);
				int minCN = table.getAttrCount() - 1;
				attr.addElement(table.getAttribute(minCN));
				table.addAttribute("1st quartile speed", IdMaker.makeId("q1_speed", table), AttributeTypes.real);
				int q1CN = table.getAttrCount() - 1;
				attr.addElement(table.getAttribute(q1CN));
				table.addAttribute("Median speed", IdMaker.makeId("median_speed", table), AttributeTypes.real);
				int medCN = table.getAttrCount() - 1;
				attr.addElement(table.getAttribute(medCN));
				table.addAttribute("3rd quartile speed", IdMaker.makeId("q3_speed", table), AttributeTypes.real);
				int q3CN = table.getAttrCount() - 1;
				attr.addElement(table.getAttribute(q3CN));
				table.addAttribute("Max speed", IdMaker.makeId("max_speed", table), AttributeTypes.real);
				int maxCN = table.getAttrCount() - 1;
				attr.addElement(table.getAttribute(maxCN));
				for (int i = 0; i < moveLayer.getObjectCount(); i++) {
					DGeoObject gobj = moveLayer.getObject(i);
					if (gobj instanceof DMovingObject) {
						DMovingObject mobj = (DMovingObject) gobj;
						if (mobj.getData() == null || !(mobj.getData() instanceof DataRecord)) {
							continue;
						}
						double speeds[] = mobj.getSpeeds();
						if (speeds == null || speeds.length < 1) {
							continue;
						}
						int length = mobj.getTrack().size() - 1;
						DataRecord rec = (DataRecord) mobj.getData();
						if (length == 1) {
							rec.setNumericAttrValue(speeds[0], minCN);
							rec.setNumericAttrValue(speeds[0], q1CN);
							rec.setNumericAttrValue(speeds[0], medCN);
							rec.setNumericAttrValue(speeds[0], q3CN);
							rec.setNumericAttrValue(speeds[0], maxCN);
							continue;
						}
						DoubleArray dist = new DoubleArray(speeds.length, 1);
						for (int j = 0; j < length; j++) {
							dist.addElement(speeds[j]);
						}
						int perc[] = { 0, 25, 50, 75, 100 };
						double stat[] = NumValManager.getPercentiles(dist, perc);
						if (stat == null) {
							continue;
						}
						rec.setNumericAttrValue(stat[0], minCN);
						rec.setNumericAttrValue(stat[1], q1CN);
						rec.setNumericAttrValue(stat[2], medCN);
						rec.setNumericAttrValue(stat[3], q3CN);
						rec.setNumericAttrValue(stat[4], maxCN);
					}
				}
			}
			if (cbSpPosFeatures.getState()) {
				resultsArePositions = true;
				int firstIdx = table.getAttrCount();
				table.addAttribute("start X", IdMaker.makeId("startX", table), AttributeTypes.real);
				int startXCN = table.getAttrCount() - 1;
				table.addAttribute("start Y", IdMaker.makeId("startY", table), AttributeTypes.real);
				table.addAttribute("end X", IdMaker.makeId("endX", table), AttributeTypes.real);
				int endXCN = table.getAttrCount() - 1;
				table.addAttribute("end Y", IdMaker.makeId("endY", table), AttributeTypes.real);

				int extPtsCN1 = table.getAttrCount();
				//extreme points: westernmost, easternmost, southernmost, northernmost
				table.addAttribute("westmost X", IdMaker.makeId("westmostX", table), AttributeTypes.real);
				table.addAttribute("westmost Y", IdMaker.makeId("westmostY", table), AttributeTypes.real);
				table.addAttribute("eastmost X", IdMaker.makeId("eastmostX", table), AttributeTypes.real);
				table.addAttribute("eastmost Y", IdMaker.makeId("eastmostY", table), AttributeTypes.real);
				table.addAttribute("southmost X", IdMaker.makeId("southmostX", table), AttributeTypes.real);
				table.addAttribute("southmost Y", IdMaker.makeId("southmostY", table), AttributeTypes.real);
				table.addAttribute("northmost X", IdMaker.makeId("northmostX", table), AttributeTypes.real);
				table.addAttribute("northmost Y", IdMaker.makeId("northmostY", table), AttributeTypes.real);

				table.addAttribute("farthestFromStart X", IdMaker.makeId("farthestFromStartX", table), AttributeTypes.real);
				int farFromStartXCN = table.getAttrCount() - 1;
				table.addAttribute("farthestFromStart Y", IdMaker.makeId("farthestFromStartY", table), AttributeTypes.real);
				table.addAttribute("farthestFromEnd X", IdMaker.makeId("farthestFromEndX", table), AttributeTypes.real);
				int farFromEndXCN = table.getAttrCount() - 1;
				table.addAttribute("farthestFromEnd Y", IdMaker.makeId("farthestFromEndY", table), AttributeTypes.real);
				table.addAttribute("farthestFromStartEnd X", IdMaker.makeId("farthestFromStartEndX", table), AttributeTypes.real);
				int farFromStartEndXCN = table.getAttrCount() - 1;
				table.addAttribute("farthestFromStartEnd Y", IdMaker.makeId("farthestFromStartEndY", table), AttributeTypes.real);

				table.addAttribute("midPath X", IdMaker.makeId("midPathX", table), AttributeTypes.real);
				int midXCN = table.getAttrCount() - 1;
				table.addAttribute("midPath Y", IdMaker.makeId("midPathY", table), AttributeTypes.real);
				table.addAttribute("mean X", IdMaker.makeId("meanX", table), AttributeTypes.real);
				int meanXCN = table.getAttrCount() - 1;
				table.addAttribute("mean Y", IdMaker.makeId("meanY", table), AttributeTypes.real);
				table.addAttribute("median X", IdMaker.makeId("medianX", table), AttributeTypes.real);
				int medianXCN = table.getAttrCount() - 1;
				table.addAttribute("median Y", IdMaker.makeId("medianY", table), AttributeTypes.real);

				for (int i = firstIdx; i < table.getAttrCount(); i++) {
					attr.addElement(table.getAttribute(i));
				}

				for (int i = 0; i < moveLayer.getObjectCount(); i++) {
					DGeoObject gobj = moveLayer.getObject(i);
					if (gobj instanceof DMovingObject) {
						DMovingObject mobj = (DMovingObject) gobj;
						if (mobj.getData() == null || !(mobj.getData() instanceof DataRecord)) {
							continue;
						}
						Vector track = mobj.getTrack();
						if (track == null || track.size() < 1) {
							continue;
						}
						DataRecord rec = (DataRecord) mobj.getData();

						RealPoint p0 = ((SpatialEntity) track.elementAt(0)).getCentre();
						rec.setNumericAttrValue(p0.x, startXCN);
						rec.setNumericAttrValue(p0.y, startXCN + 1);
						RealPoint pEnd = ((SpatialEntity) track.elementAt(track.size() - 1)).getCentre();
						rec.setNumericAttrValue(pEnd.x, endXCN);
						rec.setNumericAttrValue(pEnd.y, endXCN + 1);

						RealPoint exp[] = mobj.getExtremePoints();
						if (exp != null) {
							int cn = extPtsCN1;
							for (RealPoint element : exp) {
								rec.setNumericAttrValue(element.x, cn++);
								rec.setNumericAttrValue(element.y, cn++);
							}
						}

						RealPoint p = mobj.getFarthestPointFrom(p0);
						if (p != null) {
							rec.setNumericAttrValue(p.x, farFromStartXCN);
							rec.setNumericAttrValue(p.y, farFromStartXCN + 1);
						}
						p = mobj.getFarthestPointFrom(pEnd);
						if (p != null) {
							rec.setNumericAttrValue(p.x, farFromEndXCN);
							rec.setNumericAttrValue(p.y, farFromEndXCN + 1);
						}
						p = mobj.getFarthestPointFromStartEnd();
						if (p != null) {
							rec.setNumericAttrValue(p.x, farFromStartEndXCN);
							rec.setNumericAttrValue(p.y, farFromStartEndXCN + 1);
						}

						p = mobj.getMidPointOfPath();
						if (p != null) {
							rec.setNumericAttrValue(p.x, midXCN);
							rec.setNumericAttrValue(p.y, midXCN + 1);
						}
						p = mobj.getMeanPoint();
						if (p != null) {
							rec.setNumericAttrValue(p.x, meanXCN);
							rec.setNumericAttrValue(p.y, meanXCN + 1);
						}
						p = mobj.getMedianPoint();
						if (p != null) {
							rec.setNumericAttrValue(p.x, medianXCN);
							rec.setNumericAttrValue(p.y, medianXCN + 1);
						}
					}
				}
			} else if (cbGeomFeatures.getState()) {
				int firstIdx = table.getAttrCount();
				table.addAttribute("path length", IdMaker.makeId("path_length", table), AttributeTypes.real);
				int lenCN = table.getAttrCount() - 1;

				int extentCN1 = table.getAttrCount();
				table.addAttribute("extent X", IdMaker.makeId("extent_X", table), AttributeTypes.real);
				table.addAttribute("extent Y", IdMaker.makeId("extent_Y", table), AttributeTypes.real);
				table.addAttribute("extent diagonal", IdMaker.makeId("extent_diag", table), AttributeTypes.real);
				table.addAttribute("extent area", IdMaker.makeId("extent_area", table), AttributeTypes.real);

				int displCN1 = table.getAttrCount();
				table.addAttribute("displacement distance", IdMaker.makeId("d_dist", table), AttributeTypes.real);
				table.addAttribute("displacement X", IdMaker.makeId("dx", table), AttributeTypes.real);
				table.addAttribute("displacement Y", IdMaker.makeId("dy", table), AttributeTypes.real);

				int maxDistCN1 = table.getAttrCount();
				table.addAttribute("max distance from start", IdMaker.makeId("maxDistanceFromStart", table), AttributeTypes.real);
				table.addAttribute("max distance from end", IdMaker.makeId("maxDistanceFromEnd", table), AttributeTypes.real);

				int factor = 1;
				if (geo) {
					factor = 1000;
					for (int i = firstIdx; i < table.getAttrCount(); i++) {
						Attribute at = table.getAttribute(i);
						at.setName(at.getName() + " (km)");
					}
				}

				table.addAttribute("sinuosity", IdMaker.makeId("sinuosity", table), AttributeTypes.real);
				int sinuCN = table.getAttrCount() - 1;

				int dirCN1 = table.getAttrCount();
				table.addAttribute("sin direction start>end", IdMaker.makeId("dir_start_end", table), AttributeTypes.real);
				table.addAttribute("sin direction start>farthest point", IdMaker.makeId("dir_start_far_pt", table), AttributeTypes.real);
				table.addAttribute("sin direction farthest point>end", IdMaker.makeId("dir_far_pt_end", table), AttributeTypes.real);

				for (int i = firstIdx; i < table.getAttrCount(); i++) {
					attr.addElement(table.getAttribute(i));
				}

				for (int i = 0; i < moveLayer.getObjectCount(); i++) {
					DGeoObject gobj = moveLayer.getObject(i);
					if (gobj instanceof DMovingObject) {
						DMovingObject mobj = (DMovingObject) gobj;
						if (mobj.getData() == null || !(mobj.getData() instanceof DataRecord)) {
							continue;
						}
						Vector track = mobj.getTrack();
						if (track == null || track.size() < 1) {
							continue;
						}
						DataRecord rec = (DataRecord) mobj.getData();

						double len = mobj.getTrackLength() / factor;
						if (!Double.isNaN(len)) {
							rec.setNumericAttrValue(len, lenCN);
						}
						RealRectangle br = mobj.getBounds();
						if (br != null) {
							double wh[] = DGeoLayer.getExtentXY(br.rx1, br.ry1, br.rx2, br.ry2, geo);
							wh[0] /= factor;
							wh[1] /= factor;
							rec.setNumericAttrValue(wh[0], extentCN1);
							rec.setNumericAttrValue(wh[1], extentCN1 + 1);
							double dist = GeoComp.distance(br.rx1, br.ry1, br.rx2, br.ry2, geo) / factor;
							rec.setNumericAttrValue(dist, extentCN1 + 2);
							rec.setNumericAttrValue(wh[0] * wh[1], extentCN1 + 3);
						}

						RealPoint p0 = ((SpatialEntity) track.elementAt(0)).getCentre();
						RealPoint pEnd = ((SpatialEntity) track.elementAt(track.size() - 1)).getCentre();
						if (p0 == null || pEnd == null) {
							continue;
						}
						double dist = GeoComp.distance(p0.x, p0.y, pEnd.x, pEnd.y, geo) / factor;
						rec.setNumericAttrValue(dist, displCN1);
						dist = GeoComp.distance(p0.x, p0.y, pEnd.x, p0.y, geo) / factor;
						rec.setNumericAttrValue(dist, displCN1 + 1);
						dist = GeoComp.distance(pEnd.x, p0.y, pEnd.x, pEnd.y, geo) / factor;
						rec.setNumericAttrValue(dist, displCN1 + 2);
						if (!Double.isNaN(len) && dist > 0) {
							rec.setNumericAttrValue(len / dist, sinuCN);
						}
						RealPoint p = mobj.getFarthestPointFrom(p0);
						if (p != null) {
							dist = GeoComp.distance(p0.x, p0.y, p.x, p.y, geo) / factor;
							rec.setNumericAttrValue(dist, maxDistCN1);
						}
						p = mobj.getFarthestPointFrom(pEnd);
						if (p != null) {
							dist = GeoComp.distance(pEnd.x, pEnd.y, p.x, p.y, geo) / factor;
							rec.setNumericAttrValue(dist, maxDistCN1 + 1);
						}

						rec.setNumericAttrValue(GeoComp.getSinAngleXAxis(pEnd.x - p0.x, pEnd.y - p0.y), dirCN1);
						p = mobj.getFarthestPointFromStartEnd();
						if (p != null) {
							rec.setNumericAttrValue(GeoComp.getSinAngleXAxis(p.x - p0.x, p.y - p0.y), dirCN1 + 1);
							rec.setNumericAttrValue(GeoComp.getSinAngleXAxis(pEnd.x - p.x, pEnd.y - p.y), dirCN1 + 2);
						}
					}
				}
			}

			aDescr.endTime = System.currentTimeMillis();
			core.logAction(aDescr);
			showMessage("Computation finished!", false);
			if (!resultsArePositions && table.getAttrCount() > nAttrPrev) {
				int colNs[] = new int[table.getAttrCount() - nAttrPrev];
				for (int i = 0; i < colNs.length; i++) {
					colNs[i] = nAttrPrev + i;
				}
				table.setNiceStringsForNumbers(colNs, false);
			}
			if (attr == null || attr.size() < 1)
				return;
			Attribute at = (Attribute) attr.elementAt(0);
			boolean paramAttr = at.hasChildren();
			if (!paramAttr) {
				String aNames[] = new String[attr.size()];
				for (int i = 0; i < aNames.length; i++) {
					aNames[i] = ((Attribute) attr.elementAt(i)).getName();
				}
				aNames = Dialogs.editStringValues(core.getUI().getMainFrame(), null, aNames, "Edit the names of the table attributes if needed", "Attribute names", true);
				if (aNames != null) {
					for (int i = 0; i < aNames.length; i++)
						if (aNames[i] != null) {
							((Attribute) attr.elementAt(i)).setName(aNames[i]);
						}
				}
			}
			table.makeUniqueAttrIdentifiers();
			for (int i = 0; i < attr.size(); i++) {
				ResultDescr res = new ResultDescr();
				res.product = attr.elementAt(i);
				res.owner = table;
				aDescr.addResultDescr(res);
			}
			if (newTable) {
				DataLoader dLoader = core.getDataLoader();
				dLoader.setLink(moveLayer, dLoader.addTable(table));
				moveLayer.setThematicFilter(table.getObjectFilter());
				moveLayer.setLinkedToTable(true);
				showMessage("Table " + table.getName() + " has been attached to layer " + moveLayer.getName(), false);
			}
		}
	}

	/**
	 * @param moveLayer - the layer with trajectories
	 * @param table - the table with thematic data about the trajectories
	 * @return a vector with attributes added to the table (instances of Attribute)
	 */
	protected Vector getPathLengthsForTimeInterval(DGeoLayer moveLayer, DataTable table) {
		TimeReference tref = moveLayer.getTimeSpan();
		if (tref == null || tref.getValidFrom() == null || tref.getValidUntil() == null) {
			showMessage("The layer has no time references!", true);
			return null;
		}
		TimeMoment t1 = tref.getValidFrom(), t2 = tref.getValidUntil();
		t1.checkPrecision();
		t2.checkPrecision();
		Panel p = new Panel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		Panel pp = new Panel(new GridLayout(2, 1));
		pp.add(new Label("from", Label.RIGHT));
		pp.add(new Label("to", Label.RIGHT));
		p.add(pp);
		pp = new Panel(new GridLayout(2, 1));
		TextField tf1 = new TextField(t1.toString());
		pp.add(tf1);
		TextField tf2 = new TextField(t2.toString());
		pp.add(tf2);
		p.add(pp);
		pp = new Panel(new ColumnLayout());
		pp.add(new Label("Specify the time interval:"));
		pp.add(p);
		p = new Panel(new FlowLayout(FlowLayout.LEFT));
		Checkbox cbDivide = new Checkbox("Divide into sub-intervals of the length", false);
		p.add(cbDivide);
		GetIntervalLengthUI iLenUI = new GetIntervalLengthUI(t1, t2);
		p.add(iLenUI);
		pp.add(p);
		p = new Panel(new FlowLayout(FlowLayout.LEFT));
		Checkbox cumulCB = new Checkbox("compute also cumulative path lengths", false);
		p.add(cumulCB);
		pp.add(p);
		Label errLab = new Label("");
		pp.add(errLab);
		OKDialog dia = new OKDialog(core.getUI().getMainFrame(), "Time interval?", true);
		dia.addContent(pp);
		TimeMoment t01 = null, t02 = null;
		int iLen = 0, subLen = 0;
		do {
			dia.show();
			if (dia.wasCancelled())
				return null;
			iLen = subLen = 0;
			String str = tf1.getText();
			if (str == null || str.trim().length() < 1) {
				errLab.setText("The start of the interval is not specified!");
			} else {
				if (t01 == null) {
					t01 = t1.getCopy();
				}
				if (!t01.setMoment(str.trim())) {
					errLab.setText("Incorrect format of the start of the interval!");
					t01 = null;
				} else {
					str = tf2.getText();
					if (str == null || str.trim().length() < 1) {
						errLab.setText("The end of the interval is not specified!");
					} else {
						if (t02 == null) {
							t02 = t2.getCopy();
						}
						if (!t02.setMoment(str.trim())) {
							errLab.setText("Incorrect format of the end of the interval!");
							t02 = null;
						}
					}
				}
			}
			if (t01 != null && t02 != null) {
				iLen = (int) t02.subtract(t01);
				if (iLen < 1) {
					errLab.setText("The end of the interval must be later than the start!");
					iLen = 0;
				} else if (cbDivide.getState()) {
					subLen = (int) iLenUI.getIntervalLengthInOrigUnits();
					if (subLen <= 0) {
						errLab.setText("The length of the sub-intervals must be >0!");
					} else if (subLen >= iLen) {
						errLab.setText("The length of the sub-intervals must be less than " + iLen + " " + t01.getUnits() + "!");
						subLen = 0;
					}
				}
			}
			if (iLen < 1 || (cbDivide.getState() && subLen <= 0)) {
				errLab.setBackground(Color.red.darker());
				errLab.setForeground(Color.yellow);
			}
		} while (t01 == null || t02 == null);
		int nBreaks = 2;
		if (cbDivide.getState()) {
			nBreaks += (int) Math.round(Math.ceil(1.0 * iLen / subLen)) - 1;
		}
		TimeMoment timeBreaks[] = new TimeMoment[nBreaks];
		timeBreaks[0] = t01;
		timeBreaks[nBreaks - 1] = t02;
		for (int i = 1; i < nBreaks - 1; i++) {
			timeBreaks[i] = timeBreaks[i - 1].getCopy();
			timeBreaks[i].add(subLen);
		}
		Vector attr = new Vector(nBreaks - 1, 1);
		int len1CN = table.getAttrCount();
		String aName = "Path length from " + t01 + " to " + t02, a1Name = null;
		if (nBreaks > 2) {
			if (cumulCB.getState()) {
				a1Name = "Cumulative path length from " + t01 + " to " + t02;
			}
			String iStr = iLenUI.getIntervalLengthInCurrentUnits() + "-" + iLenUI.getCurrentUnit();
			String suffix = " by " + iStr + " intervals";
			aName += suffix;
			if (a1Name != null) {
				a1Name += suffix;
			}
			Parameter par = new Parameter();
			par.setTemporal(true);
			for (int i = 0; i < nBreaks; i++) {
				par.addValue(timeBreaks[i]);
			}
			String parName = iStr + " interval (end)";
			par.setName(parName);
			for (int k = 2; table.getParameter(par.getName()) != null; k++) {
				par.setName(parName + ", v" + k);
			}
			p = new Panel(new ColumnLayout());
			if (a1Name == null) {
				p.add(new Label("A parameter-dependent attribute will be produced.", Label.CENTER));
				p.add(new Label("Edit the names of the attribute and the parameter if needed.", Label.CENTER));
				p.add(new Label("Attribute name:"));
			} else {
				p.add(new Label("Two parameter-dependent attributes will be produced.", Label.CENTER));
				p.add(new Label("Edit the names of the attributes and the parameter if needed.", Label.CENTER));
				p.add(new Label("Attribute names:"));
			}
			TextField atf = new TextField(aName), a1tf = null;
			p.add(atf);
			if (a1Name != null) {
				a1tf = new TextField(a1Name);
				p.add(a1tf);
			}
			p.add(new Label("Parameter name:"));
			TextField ptf = new TextField(par.getName());
			p.add(ptf);
			p.add(new Label("Parameter values from " + par.getFirstValue() + " to " + par.getLastValue()));
			TextField dtf = null;
			if (timeBreaks[0] instanceof Date) {
				p.add(new Label("Template for displaying the dates:"));
				Date d = (Date) timeBreaks[0];
				dtf = new TextField(d.scheme);
				p.add(dtf);
			}
			dia = new OKDialog(core.getUI().getMainFrame(), "Parameter-dependent attribute", true);
			dia.addContent(p);
			dia.show();
			if (dia.wasCancelled())
				return null;
			String str = atf.getText();
			if (str != null && str.trim().length() > 0) {
				aName = str.trim();
			}
			if (a1tf != null) {
				str = a1tf.getText();
				if (str != null && str.trim().length() > 0) {
					a1Name = str.trim();
				}
			}
			str = ptf.getText();
			if (str != null && str.trim().length() > 0) {
				parName = str.trim();
				par.setName(parName);
				for (int k = 2; table.getParameter(par.getName()) != null; k++) {
					par.setName(parName + ", v" + k);
				}
			}
			if (dtf != null) {
				str = dtf.getText();
				if (str != null) {
					str = str.trim();
					if (str.length() > 0 && Date.checkTemplateValidity(str) == null) {
						for (int i = 0; i < timeBreaks.length; i++) {
							((Date) timeBreaks[i]).scheme = str;
						}
					}
				}
			}

			table.addParameter(par);
			Attribute attrParent = new Attribute(IdMaker.makeId(aName, table), AttributeTypes.real);
			attrParent.setName(aName);
			for (int i = 1; i < par.getValueCount(); i++) {
				Attribute attrChild = new Attribute(attrParent.getIdentifier() + "_" + i, attrParent.getType());
				attrChild.addParamValPair(par.getName(), par.getValue(i));
				attrParent.addChild(attrChild);
				table.addAttribute(attrChild);
			}
			attr.addElement(attrParent);
			if (a1Name != null) {
				attrParent = new Attribute(IdMaker.makeId(a1Name, table), AttributeTypes.real);
				attrParent.setName(a1Name);
				for (int i = 0; i < par.getValueCount(); i++) {
					Attribute attrChild = new Attribute(attrParent.getIdentifier() + "_" + i, attrParent.getType());
					attrChild.addParamValPair(par.getName(), par.getValue(i));
					attrParent.addChild(attrChild);
					table.addAttribute(attrChild);
				}
				attr.addElement(attrParent);
			}
		} else {
			table.addAttribute(aName, IdMaker.makeId(aName, table), AttributeTypes.real);
			attr.addElement(table.getAttribute(table.getAttrCount() - 1));
		}
		int aIdx2 = len1CN + nBreaks - 1;
		for (int i = 0; i < moveLayer.getObjectCount(); i++) {
			DGeoObject gobj = moveLayer.getObject(i);
			if (gobj instanceof DMovingObject) {
				DMovingObject mobj = (DMovingObject) gobj;
				if (mobj.getData() == null || !(mobj.getData() instanceof DataRecord)) {
					continue;
				}
				DataRecord rec = (DataRecord) mobj.getData();
				for (int j = 0; j < nBreaks - 1; j++) {
					double len = mobj.getTrackLength(timeBreaks[j], timeBreaks[j + 1]);
					if (!Double.isNaN(len)) {
						rec.setNumericAttrValue(len, len1CN + j);
					}
				}
				if (a1Name != null) {
					double prevLen = mobj.getTrackLengthBy(timeBreaks[0]);
					rec.setNumericAttrValue(0.0, aIdx2);
					for (int j = 1; j < nBreaks; j++) {
						double len = mobj.getTrackLengthBy(timeBreaks[j]);
						if (!Double.isNaN(len)) {
							rec.setNumericAttrValue(len - prevLen, aIdx2 + j);
						}
					}
				}
			}
		}
		return attr;
	}

	protected void showMessage(String msg, boolean error) {
		if (core != null && core.getUI() != null) {
			core.getUI().showMessage(msg, error);
		} else if (error) {
			System.out.println("!--> " + msg);
		}
	}
}
