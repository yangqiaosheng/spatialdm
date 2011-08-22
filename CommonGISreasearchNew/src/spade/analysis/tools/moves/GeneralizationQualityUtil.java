package spade.analysis.tools.moves;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Color;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.util.Vector;

import spade.analysis.geocomp.voronoi.VoronoiNew;
import spade.analysis.system.DataLoader;
import spade.analysis.system.ESDACore;
import spade.analysis.tools.clustering.PointOrganizerSpatialIndex;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Dialogs;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.OKDialog;
import spade.lib.basicwin.TextCanvas;
import spade.lib.util.BubbleSort;
import spade.lib.util.Comparator;
import spade.lib.util.GeoDistance;
import spade.lib.util.ObjectWithMeasure;
import spade.lib.util.QSortAlgorithm;
import spade.lib.util.StringUtil;
import spade.time.TimeReference;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.database.SpatialEntity;
import spade.vis.dmap.DAggregateLinkLayer;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;
import spade.vis.dmap.DPlaceVisitsLayer;
import spade.vis.dmap.DPlaceVisitsObject;
import spade.vis.dmap.DrawingParameters;
import spade.vis.dmap.PlaceVisitInfo;
import spade.vis.geometry.GeoComp;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.RealPoint;
import spade.vis.geometry.RealPolyline;
import spade.vis.geometry.RealRectangle;
import spade.vis.space.GeoObject;
import core.ActionDescr;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Apr 29, 2009
 * Time: 2:37:17 PM
 * Contains various utility methods for the assessment and
 * improvement of generalization quality (points -> areas) in
 * the summarization of trajectories.
 */
public class GeneralizationQualityUtil implements Comparator {
	protected ESDACore core = null;

	protected void showMessage(String msg, boolean error) {
		if (core != null && core.getUI() != null) {
			core.getUI().showMessage(msg, error);
		} else if (error) {
			System.out.println("!--> " + msg);
		}
	}

	public GeneralizationQualityUtil(ESDACore core) {
		this.core = core;
	}

	/**
	 * Refines the division of the territory and the aggregation of the
	 * trajectories by introducing new generating points for the Voronoi polygons
	 */
	public void refineSummarization(DPlaceVisitsLayer placeLayer, Vector<RealPoint> points) {
		if (placeLayer == null || placeLayer.getObjectCount() < 1 || placeLayer.getTrajectoryLayer() == null || points == null || points.size() < 1)
			return;
		DGeoLayer moveLayer = placeLayer.getTrajectoryLayer();
		ActionDescr aDescr = new ActionDescr();
		aDescr.aName = "Refine summarization of trajectories";
		aDescr.addParamValue("Trajectories layer id", moveLayer.getContainerIdentifier());
		aDescr.addParamValue("Trajectories layer name", moveLayer.getName());
		aDescr.addParamValue("Areas layer id", placeLayer.getContainerIdentifier());
		aDescr.addParamValue("Areas layer name", placeLayer.getName());
		aDescr.startTime = System.currentTimeMillis();
		core.logAction(aDescr);

		VoronoiNew voronoi = refineTerritoryDivision(placeLayer, points, placeLayer.maxDistortion / 5);
		if (voronoi == null)
			return;
		TrajectoriesGeneraliser trGen = new TrajectoriesGeneraliser();
		trGen.setCore(core);
		boolean ok = trGen.summarizeByPolygons(moveLayer, voronoi.getResultingCells(), voronoi.getNeighbourhoodMap(), placeLayer.onlyActiveTrajectories, placeLayer.onlyStartsEnds, placeLayer.findIntersections, false);
		if (ok) {
			trGen.makeLayersAndTables(moveLayer, "refined; " + points.size() + " points added", trGen.getPlaces(), trGen.getAggMoves(), placeLayer.onlyActiveTrajectories, placeLayer.onlyStartsEnds, placeLayer.findIntersections, aDescr);
			DPlaceVisitsLayer refPlaceLayer = trGen.getPlaceLayer();
			if (refPlaceLayer != null) {
				refPlaceLayer.setOrigPlaceLayer(placeLayer);
			}
		}
	}

	/**
	 * Selects the places from which to extract the visits.
	 * Returns a vector of DPlaceVisitsObject
	 */
	public Vector getSuitablePlaces(DPlaceVisitsLayer placeLayer) {
		if (placeLayer == null || placeLayer.getObjectCount() < 1)
			return null;
		CheckboxGroup cbg = new CheckboxGroup();
		Checkbox cbAll = new Checkbox("all areas", false, cbg);
		Checkbox cbSelected = null;
		Vector selObj = core.getHighlighterForSet(placeLayer.getEntitySetIdentifier()).getSelectedObjects();
		if (selObj != null && selObj.size() > 0) {
			cbSelected = new Checkbox("currently selected areas", true, cbg);
		}
		Checkbox cbFilter = null;
		if (placeLayer.areObjectsFiltered()) {
			cbFilter = new Checkbox("active areas (after filtering)", cbSelected == null, cbg);
		}
		Checkbox cbBigSumDist = new Checkbox("areas with total displacement over", cbSelected == null && cbFilter == null, cbg);
		float lowLimit = 0.5f * placeLayer.maxSumDistortion;
		TextField tfSumDist = new TextField(String.valueOf(lowLimit));
		Checkbox cbUseNeihgbours = new Checkbox("also from the neighbouring areas", false);
		Panel p = new Panel(new ColumnLayout());
		TextCanvas tc = new TextCanvas();
		tc.setText("A new point layer will be produced with the points of trajectories " + "representing their visits to the areas. These are the closest points to the " + "centroids of the areas");
		p.add(tc);
		p.add(new Line(false));
		p.add(new Label("From what areas must the points be extracted?"));
		p.add(cbAll);
		if (cbSelected != null) {
			p.add(cbSelected);
		}
		if (cbFilter != null) {
			p.add(cbFilter);
		}
		Panel pp = new Panel(new BorderLayout(0, 0));
		pp.add(cbBigSumDist, BorderLayout.WEST);
		pp.add(tfSumDist, BorderLayout.CENTER);
		p.add(pp);
		p.add(new Line(false));
		p.add(cbUseNeihgbours);
		OKDialog dia = new OKDialog(core.getUI().getMainFrame(), "Visits of areas", true);
		dia.addContent(p);
		dia.show();
		if (dia.wasCancelled())
			return null;

		if (cbAll.getState())
			return placeLayer.getObjects();

		Vector places = new Vector(placeLayer.getObjectCount(), 1);
		if (cbSelected != null && cbSelected.getState()) {
			for (int i = 0; i < selObj.size(); i++) {
				String oId = (String) selObj.elementAt(i);
				GeoObject obj = placeLayer.findObjectById(oId);
				if (obj != null) {
					places.addElement(obj);
				}
			}
			if (places.size() < 1) {
				showMessage("No selected areas found!", true);
				return null;
			}
		} else if (cbFilter != null && cbFilter.getState()) {
			for (int i = 0; i < placeLayer.getObjectCount(); i++)
				if (placeLayer.isObjectActive(i)) {
					places.addElement(placeLayer.getObject(i));
				}
			if (places.size() < 1) {
				showMessage("No active areas (satisfying the filter) found!", true);
				return null;
			}
		} else if (cbBigSumDist.getState()) {
			double val = Double.NaN;
			String str = tfSumDist.getText();
			if (str != null) {
				try {
					val = Float.parseFloat(str);
				} catch (NumberFormatException nfe) {
				}
			}
			if (Double.isNaN(val) || val <= 0) {
				val = Dialogs.askForDoubleValue(core.getUI().getMainFrame(), "Lower limit for the total displacement?", (double) lowLimit, 0.1 * placeLayer.maxSumDistortion, (double) placeLayer.maxSumDistortion, null, "Set the limit", true);
				if (Double.isNaN(val))
					return null;
			}
			lowLimit = (float) val;
			for (int i = 0; i < placeLayer.getObjectCount(); i++) {
				DPlaceVisitsObject pObj = (DPlaceVisitsObject) placeLayer.getObject(i);
				if (pObj.sumDistortion >= lowLimit) {
					places.addElement(pObj);
				}
			}
			if (places.size() < 1) {
				showMessage("No areas satisfying the condition found!", true);
				return null;
			}
		}
		if (places.size() < 1) {
			showMessage("No suitable areas found!", true);
			return null;
		}
		if (cbUseNeihgbours.getState() && places.size() < placeLayer.getObjectCount()) {
			int nOrigPlaces = places.size();
			for (int i = 0; i < nOrigPlaces && places.size() < placeLayer.getObjectCount(); i++) {
				DPlaceVisitsObject pObj = (DPlaceVisitsObject) places.elementAt(i);
				if (pObj.neighbours != null && pObj.neighbours.size() > 0) {
					Vector<DPlaceVisitsObject> nb = new Vector<DPlaceVisitsObject>(pObj.neighbours.size(), 1);
					for (int j = 0; j < pObj.neighbours.size() && places.size() < placeLayer.getObjectCount(); j++) {
						DPlaceVisitsObject nei = (DPlaceVisitsObject) placeLayer.findObjectById(pObj.neighbours.elementAt(j));
						if (nei != null && !places.contains(nei)) {
							places.addElement(nei);
						}
					}
				}
			}
		}
		return places;
	}

	/**
	 * For research purposes: produces a map layer with points representing
	 * the visits of the generalized places by the trajectories.
	 */
	public DGeoLayer makeVisitsLayer(DPlaceVisitsLayer placeLayer, Vector places) {
		if (placeLayer == null || placeLayer.getObjectCount() < 1 || places == null || places.size() < 1)
			return null;
		Vector<DGeoObject> pObj = new Vector<DGeoObject>(places.size() * 50, 100);
		String name = "Visits of " + placeLayer.getName();
/*
    name= Dialogs.askForStringValue(core.getUI().getMainFrame(),"Name of the layer?",name,
      "A map layer with points representing visits of places will be generated","Visits of places",false);
*/
		DataTable table = new DataTable();
		table.setName(name);
		table.addAttribute("Trajectory id", "traj_id", AttributeTypes.character);
		table.addAttribute("Place id", "place_id", AttributeTypes.character);
		table.addAttribute("Distance to centre", "dist_cen", AttributeTypes.real);
		table.addAttribute("Start place?", "place_start", AttributeTypes.logical);
		table.addAttribute("End place?", "place_end", AttributeTypes.logical);
		table.addAttribute("Just crossed?", "place_cross", AttributeTypes.logical);
		table.addAttribute("Time of entering", "enter_time", AttributeTypes.time);
		table.addAttribute("Time of leaving", "leave_time", AttributeTypes.time);
		table.addAttribute("Stay duration", "duration", AttributeTypes.integer);
		table.addAttribute("Internal track length", "length", AttributeTypes.real);
		table.addAttribute("Average speed", "speed", AttributeTypes.real);
		table.addAttribute("Turn (degrees)", "turn", AttributeTypes.real);
		for (int i = 0; i < places.size(); i++) {
			DPlaceVisitsObject pvObj = (DPlaceVisitsObject) places.elementAt(i);
			if (pvObj.visits == null) {
				continue;
			}
			for (int j = 0; j < pvObj.visits.size(); j++) {
				PlaceVisitInfo pvi = (PlaceVisitInfo) pvObj.visits.elementAt(j);
				if (pvi.pCen == null) {
					continue;
				}
				SpatialEntity spe = new SpatialEntity(pvi.placeId + "_" + pvi.trId + "_" + pvi.firstIdx);
				spe.setGeometry(pvi.pCen);
				DataRecord rec = new DataRecord(spe.getId());
				table.addDataRecord(rec);
				rec.addAttrValue(pvi.trId);
				rec.addAttrValue(pvi.placeId);
				rec.setNumericAttrValue(pvi.dCen, StringUtil.doubleToStr(pvi.dCen, 3), 2);
				rec.addAttrValue((pvi.isStart) ? "true" : "false");
				rec.addAttrValue((pvi.isFinal) ? "true" : "false");
				rec.addAttrValue((pvi.justCrossed) ? "true" : "false");
				rec.addAttrValue(pvi.enterTime);
				rec.addAttrValue(pvi.exitTime);
				rec.setNumericAttrValue(pvi.stayDuration, String.valueOf(pvi.stayDuration), 8);
				rec.setNumericAttrValue(pvi.len, String.valueOf((float) pvi.len), 9);
				rec.setNumericAttrValue(pvi.speed, String.valueOf((float) pvi.speed), 10);
				rec.setNumericAttrValue(pvi.angleDirChange, String.valueOf((float) pvi.angleDirChange), 11);
				spe.setThematicData(rec);
				TimeReference tref = new TimeReference();
				tref.setValidFrom(pvi.enterTime);
				tref.setValidUntil(pvi.exitTime);
				spe.setTimeReference(tref);
				rec.setTimeReference(tref);
				DGeoObject gObj = new DGeoObject();
				gObj.setup(spe);
				pObj.addElement(gObj);
			}
		}
		if (pObj.size() < 1)
			return null;
		DGeoLayer layer = new DGeoLayer();
		layer.setName(table.getName());
		layer.setType(Geometry.point);
		layer.setGeographic(placeLayer.isGeographic());
		layer.setGeoObjects(pObj, true);
		DrawingParameters dp = layer.getDrawingParameters();
		if (dp == null) {
			dp = new DrawingParameters();
			layer.setDrawingParameters(dp);
		}
		dp.lineColor = Color.getHSBColor((float) Math.random(), 1 - 0.2f * (float) Math.random(), 1 - 0.2f * (float) Math.random());
		dp.fillColor = Color.getHSBColor((float) Math.random(), 1 - 0.2f * (float) Math.random(), 1 - 0.2f * (float) Math.random());
		dp.fillContours = false;
		DataLoader dataLoader = core.getDataLoader();
		dataLoader.addMapLayer(layer, -1);
		int tblN = dataLoader.addTable(table);
		table.setEntitySetIdentifier(layer.getEntitySetIdentifier());
		dataLoader.setLink(layer, tblN);
		layer.setLinkedToTable(true);
		dataLoader.processTimeReferencedObjectSet(layer);
		dataLoader.processTimeReferencedObjectSet(table);
		return layer;
	}

	/**
	 * Generates a table that describes the changes after each refinement step.
	 * Puts there a record with the initial displacement measures of the layer.
	 */
	public static DataTable makeTableOfChanges(DPlaceVisitsLayer placeLayer) {
		if (placeLayer == null)
			return null;
		DataTable metaData = new DataTable();
		metaData.setName("Refinement of " + placeLayer.getName());
		metaData.addAttribute("Iteration step N", "step_n", AttributeTypes.integer);
		metaData.addAttribute("N of areas", "n_areas", AttributeTypes.integer);
		metaData.addAttribute("Overall max displacement", "max_distortion", AttributeTypes.real);
		metaData.addAttribute("Max of mean local displacements", "max_mean_distortion", AttributeTypes.real);
		metaData.addAttribute("Max of total local displacements", "max_sum_distortion", AttributeTypes.real);
		metaData.addAttribute("Overall mean displacement", "mean_distortion", AttributeTypes.real);
		metaData.addAttribute("Overall total displacement", "sum_distortion", AttributeTypes.real);
		metaData.addAttribute("Improvement max mean %", "impr_max_mean", AttributeTypes.real);
		metaData.addAttribute("Improvement max total %", "impr_max_sum", AttributeTypes.real);
		metaData.addAttribute("Improvement overall mean %", "impr_mean", AttributeTypes.real);
		metaData.addAttribute("Improvement overall total %", "impr_sum", AttributeTypes.real);
		DataRecord rec = new DataRecord("0");
		metaData.addDataRecord(rec);
		rec.setNumericAttrValue(0, "0", 0);
		rec.setNumericAttrValue(placeLayer.getObjectCount(), String.valueOf(placeLayer.getObjectCount()), 1);
		rec.setNumericAttrValue(placeLayer.maxDistortion, StringUtil.floatToStr(placeLayer.maxDistortion, 2), 2);
		rec.setNumericAttrValue(placeLayer.maxMeanDistortion, StringUtil.floatToStr(placeLayer.maxMeanDistortion, 2), 3);
		rec.setNumericAttrValue(placeLayer.maxSumDistortion, StringUtil.floatToStr(placeLayer.maxSumDistortion, 2), 4);
		rec.setNumericAttrValue(placeLayer.meanDistortion, StringUtil.floatToStr(placeLayer.meanDistortion, 2), 5);
		rec.setNumericAttrValue(placeLayer.sumDistortion, StringUtil.floatToStr(placeLayer.sumDistortion, 2), 6);
		rec.setNumericAttrValue(0, "0.00", 7);
		rec.setNumericAttrValue(0, "0.00", 8);
		rec.setNumericAttrValue(0, "0.00", 9);
		rec.setNumericAttrValue(0, "0.00", 10);
		return metaData;
	}

	/**
	 * Puts a new record in the table describing the changes after each refinement step.
	 * @param metaData - the table in which to put the record (must be previously
	 *   constructed using the method makeTableOfChanges(...).
	 * @param stepN - the number of the iteration step
	 * @param refinedPlaceLayer - the layer resulting from the refinement.
	 * @param imprMaxSum - improvement of the maximum sum displacement in an area
	 * @param imprMean - improvement of the overall mean displacement
	 * @param imprSum - improvement of the overall sum displacement
	 */
	public static void addRecordAboutChange(DataTable metaData, int stepN, DPlaceVisitsLayer refinedPlaceLayer, float imprMaxMean, float imprMaxSum, float imprMean, float imprSum) {
		DataRecord rec = new DataRecord(String.valueOf(metaData.getDataItemCount()));
		metaData.addDataRecord(rec);
		rec.setNumericAttrValue(stepN, String.valueOf(stepN), 0);
		rec.setNumericAttrValue(refinedPlaceLayer.getObjectCount(), String.valueOf(refinedPlaceLayer.getObjectCount()), 1);
		rec.setNumericAttrValue(refinedPlaceLayer.maxDistortion, StringUtil.floatToStr(refinedPlaceLayer.maxDistortion, 2), 2);
		rec.setNumericAttrValue(refinedPlaceLayer.maxMeanDistortion, StringUtil.floatToStr(refinedPlaceLayer.maxMeanDistortion, 2), 3);
		rec.setNumericAttrValue(refinedPlaceLayer.maxSumDistortion, StringUtil.floatToStr(refinedPlaceLayer.maxSumDistortion, 2), 4);
		rec.setNumericAttrValue(refinedPlaceLayer.meanDistortion, StringUtil.floatToStr(refinedPlaceLayer.meanDistortion, 2), 5);
		rec.setNumericAttrValue(refinedPlaceLayer.sumDistortion, StringUtil.floatToStr(refinedPlaceLayer.sumDistortion, 2), 6);
		rec.setNumericAttrValue(imprMaxSum, StringUtil.floatToStr(imprMaxMean, 2), 7);
		rec.setNumericAttrValue(imprMaxSum, StringUtil.floatToStr(imprMaxSum, 2), 8);
		rec.setNumericAttrValue(imprMean, StringUtil.floatToStr(imprMean, 2), 9);
		rec.setNumericAttrValue(imprSum, StringUtil.floatToStr(imprSum, 2), 10);
		metaData.notifyPropertyChange("data_added", null, null);
	}

	/**
	 * Automated optimization of the quality of the generalization.
	 * @param souLayer - the original layer with aggregate moves
	 * @param sumDistPercentOfMax - the lower limit of the sum displacement,
	 *   in % to the current maximum, for selecting the areas to be refined
	 * @param meanDistPercentOfMax - the lower limit of the mean displacement,
	 *   in % to the current maximum, for selecting the areas to be refined
	 * @param maxNPlacesPerStep - maximum number of places to process
	 *   at each step
	 * @param minImprovement - termination condition: if the improvement
	 *   after some iteration is less than this value, no further
	 *   improvement attempts are made.
	 * @param maxNSteps - the maximum number of iteration steps;
	 *   if <1, other termination conditions are used.
	 * @param maxTimeSec - the maximum allowed time, in seconds, for the whole
	 *   procedure; if <1, other termination conditions are used.
	 * @return  a new map layer with aggregate moves.
	 */
	public DAggregateLinkLayer optimizeQuality(DAggregateLinkLayer souLayer, float sumDistPercentOfMax, float meanDistPercentOfMax, int maxNPlacesPerStep, float minImprovement, int maxNSteps, long maxTimeSec) {
		if (souLayer == null || souLayer.getObjectCount() < 1 || souLayer.getPlaceLayer() == null || !(souLayer.getPlaceLayer() instanceof DPlaceVisitsLayer))
			return null; //inappropriate data
		DPlaceVisitsLayer placeLayer = (DPlaceVisitsLayer) souLayer.getPlaceLayer();
		if (placeLayer.getObjectCount() < 1)
			return null;
		DGeoLayer moveLayer = placeLayer.getTrajectoryLayer();
		if (moveLayer == null)
			return null;
		if (placeLayer.maxDistortion <= 0) {
			placeLayer.computeDistortions();
		}
		if (placeLayer.maxDistortion <= 0)
			return null; //no distortions?
		//float distLowLimitToExtractVisits =placeLayer.maxDistortion*0.3f;
		float distLowLimitToExtractVisits = placeLayer.meanDistortion;

		DataTable metaData = makeTableOfChanges(placeLayer);
		metaData.setName("Iterative automatic refinement of " + souLayer.getName());
		core.getDataLoader().addTable(metaData);

		DPlaceVisitsLayer currPlaceLayer = placeLayer;
		TrajectoriesGeneraliser currTrGen = null;
		int nSteps = 0;
		long t0 = System.currentTimeMillis(), t = t0, t1Step = 0;
		float minImpr = 0f, maxImpr = 0f;
		int npAdded = 0;
		boolean refined = false;
		do {
			float minSumDistToSelectPlace = sumDistPercentOfMax / 100 * currPlaceLayer.maxSumDistortion;
			float minMeanDistToSelectPlace = meanDistPercentOfMax / 100 * currPlaceLayer.maxMeanDistortion;
			Vector<DPlaceVisitsObject> placesMaxDisto = getPlacesWithHighDistortions(placeLayer, minSumDistToSelectPlace, minMeanDistToSelectPlace, maxNPlacesPerStep);
			if (placesMaxDisto == null) {
				break;
			}
			float minMeanDist = placesMaxDisto.elementAt(0).meanDistortion;
			for (int i = 1; i < placesMaxDisto.size(); i++) {
				float dist = placesMaxDisto.elementAt(i).meanDistortion;
				if (dist < minMeanDist) {
					minMeanDist = dist;
				}
			}

			//Vector<PlaceVisitInfo> visits=getVisitsBigDistortion(placesMaxDisto,distLowLimitToExtractVisits);
			Vector<PlaceVisitInfo> visits = getVisitsBigDistortion(placesMaxDisto, minMeanDist);
			if (visits == null) {
				break;
			}
			Vector<RealPoint> centres = getCentresOfBiggestGroups(visits, placesMaxDisto, placeLayer.maxDistortion / 2, souLayer.isGeographic());
			if (centres == null || centres.size() < 1) {
				break;
			}
			VoronoiNew voronoi = refineTerritoryDivision(currPlaceLayer, centres, placeLayer.maxDistortion / 5);
			if (voronoi == null) {
				break;
			}
			TrajectoriesGeneraliser trGen = new TrajectoriesGeneraliser();
			trGen.setCore(core);
			boolean ok = trGen.summarizeByPolygons(moveLayer, voronoi.getResultingCells(), voronoi.getNeighbourhoodMap(), placeLayer.onlyActiveTrajectories, placeLayer.onlyStartsEnds, placeLayer.findIntersections, false);
			if (!ok) {
				break;
			}
			Vector<DPlaceVisitsObject> refinedPlaces = trGen.getPlaces();
			if (refinedPlaces == null || refinedPlaces.size() < 1) {
				break;
			}
			DPlaceVisitsLayer refinedPlaceLayer = new DPlaceVisitsLayer();
			refinedPlaceLayer.setGeographic(moveLayer.isGeographic());
			refinedPlaceLayer.setType(Geometry.area);
			refinedPlaceLayer.setGeoObjects(refinedPlaces, true);
			refinedPlaceLayer.setTrajectoryLayer(moveLayer);
			refinedPlaceLayer.computeDistortions();

			++nSteps;
			t = System.currentTimeMillis();
			if (nSteps == 1) {
				t1Step = t - t0;
			}

			float imprMaxMean = (currPlaceLayer.maxMeanDistortion - refinedPlaceLayer.maxMeanDistortion) / currPlaceLayer.maxMeanDistortion * 100, imprMaxSum = (currPlaceLayer.maxSumDistortion - refinedPlaceLayer.maxSumDistortion)
					/ currPlaceLayer.maxSumDistortion * 100, imprSum = (currPlaceLayer.sumDistortion - refinedPlaceLayer.sumDistortion) / currPlaceLayer.sumDistortion * 100, imprMean = (currPlaceLayer.meanDistortion - refinedPlaceLayer.meanDistortion)
					/ currPlaceLayer.meanDistortion * 100;
			minImpr = Math.min(imprMaxMean, Math.min(imprMaxSum, Math.min(imprSum, imprMean)));
			maxImpr = Math.max(imprMaxMean, Math.max(imprMaxSum, Math.max(imprSum, imprMean)));

			addRecordAboutChange(metaData, nSteps, refinedPlaceLayer, imprMaxMean, imprMaxSum, imprMean, imprSum);

			refined = minImpr > 0 || (minImpr > -1 && maxImpr >= minImprovement);
			if (refined) {
				currPlaceLayer = refinedPlaceLayer;
				currTrGen = trGen;
				npAdded += centres.size();
			} else {
				--nSteps;
			}
		} while (refined && maxImpr >= minImprovement && (maxNSteps < 1 || nSteps < maxNSteps) && (maxTimeSec < 1 || Math.round((t - t0 + t1Step) / 1000.0) <= maxTimeSec));
		if (currTrGen == null)
			return null;

		float imprMaxMean = (placeLayer.maxMeanDistortion - currPlaceLayer.maxMeanDistortion) / placeLayer.maxMeanDistortion * 100, imprMaxSum = (placeLayer.maxSumDistortion - currPlaceLayer.maxSumDistortion) / placeLayer.maxSumDistortion * 100, imprSum = (placeLayer.sumDistortion - currPlaceLayer.sumDistortion)
				/ placeLayer.sumDistortion * 100, imprMean = (placeLayer.meanDistortion - currPlaceLayer.meanDistortion) / placeLayer.meanDistortion * 100;
		addRecordAboutChange(metaData, nSteps, currPlaceLayer, imprMaxMean, imprMaxSum, imprMean, imprSum);

		ActionDescr aDescr = new ActionDescr();
		aDescr.aName = "Optimize summarization of trajectories";
		aDescr.addParamValue("Trajectories layer id", moveLayer.getContainerIdentifier());
		aDescr.addParamValue("Trajectories layer name", moveLayer.getName());
		aDescr.addParamValue("Areas layer id", placeLayer.getContainerIdentifier());
		aDescr.addParamValue("Areas layer name", placeLayer.getName());
		aDescr.addParamValue("N iterations", nSteps);
		aDescr.addParamValue("N points added", npAdded);
		aDescr.startTime = System.currentTimeMillis();
		core.logAction(aDescr);
		currTrGen.makeLayersAndTables(moveLayer, "optimized; " + npAdded + " points added", currTrGen.getPlaces(), currTrGen.getAggMoves(), placeLayer.onlyActiveTrajectories, placeLayer.onlyStartsEnds, placeLayer.findIntersections, aDescr);
		DPlaceVisitsLayer refPlaceLayer = currTrGen.getPlaceLayer();
		if (refPlaceLayer != null) {
			refPlaceLayer.setOrigPlaceLayer(placeLayer);
		}
		return currTrGen.getAggLinkLayer();
	}

	/**
	 * From the given layer with generalized places, extracts the places with the
	 * sum distortions >= minSumDistToSelectPlace and
	 * mean distortions >= minMeanDistToSelectPlace.
	 * If the parameter maxNPlaces>0, and the number of extracted places is more
	 * than maxNPlaces, the method selects maxNPlaces places with the highest distortions.
	 */
	public static Vector<DPlaceVisitsObject> getPlacesWithHighDistortions(DPlaceVisitsLayer placeLayer, double minSumDistToSelectPlace, double minMeanDistToSelectPlace, int maxNPlaces) {
		if (placeLayer == null)
			return null;
		Vector<DPlaceVisitsObject> places = new Vector(placeLayer.getObjectCount(), 1);
		float maxSum = 0, maxMean = 0;
		for (int i = 0; i < placeLayer.getObjectCount(); i++) {
			DPlaceVisitsObject place = (DPlaceVisitsObject) placeLayer.getObject(i);
			if (place.sumDistortion >= minSumDistToSelectPlace && place.meanDistortion >= minMeanDistToSelectPlace) {
				places.addElement(place);
				if (place.sumDistortion > maxSum) {
					maxSum = place.sumDistortion;
				}
				if (place.meanDistortion > maxMean) {
					maxMean = place.meanDistortion;
				}
			}
		}
		if (places.size() < 1)
			return null;
		if (maxNPlaces > 0 && places.size() > maxNPlaces) {
			Vector<ObjectWithMeasure> placeOrder = new Vector<ObjectWithMeasure>(places.size(), 1);
			for (int i = 0; i < places.size(); i++) {
				placeOrder.addElement(new ObjectWithMeasure(new Integer(i), places.elementAt(i).sumDistortion / maxSum + places.elementAt(i).meanDistortion / maxMean, true));
			}
			BubbleSort.sort(placeOrder);
			Vector<DPlaceVisitsObject> maxDistPlaces = new Vector<DPlaceVisitsObject>(maxNPlaces, 1);
			for (int i = 0; i < maxNPlaces; i++) {
				int idx = ((Integer) placeOrder.elementAt(i).obj).intValue();
				maxDistPlaces.addElement(places.elementAt(idx));
			}
			return maxDistPlaces;
		}
		places.trimToSize();
		return places;
	}

	/**
	 * From the given layer with generalized places, extracts the specified
	 * number of places with the maximum distortions. The method looks for the
	 * "gaps" in the distribution of the distortions and returns the number
	 * of objects approximately equal to nPlaces (tolerance = 25%).
	 * @param placeLayer - the given layer with generalized places
	 * @param nPlaces - the desired number of places with big aggregate distortions
	 * @param tolerance - the tolerance concerning the number of places, i.e.
	 *   the maximum allowed deviation from nPlaces.
	 * @return the extracted places
	 */
	public Vector<DPlaceVisitsObject> getPlacesWithMaxDistortion(DPlaceVisitsLayer placeLayer, int nPlaces, float tolerance) {
		if (placeLayer == null || placeLayer.getObjectCount() < 1)
			return null;
		Vector<DPlaceVisitsObject> places = new Vector(placeLayer.getObjectCount(), 1);
		for (int i = 0; i < placeLayer.getObjectCount(); i++) {
			places.addElement((DPlaceVisitsObject) placeLayer.getObject(i));
		}
		if (places.size() < 1)
			return null;
		if (placeLayer.getObjectCount() <= nPlaces)
			return places;
		BubbleSort.sort(places, this);
		Vector<ObjectWithMeasure> gaps = new Vector<ObjectWithMeasure>(places.size() - 1, 1);
		for (int i = 0; i < places.size() - 1; i++) {
			gaps.addElement(new ObjectWithMeasure(new Integer(i), places.elementAt(i).sumDistortion - places.elementAt(i + 1).sumDistortion));
		}
		BubbleSort.sort(gaps);
		int lastIdx = -1;
		for (int i = gaps.size() - 1; i >= 0 && lastIdx < 0; i--) {
			int idx = ((Integer) gaps.elementAt(i).obj).intValue();
			if (Math.abs(idx - nPlaces) <= tolerance) {
				lastIdx = idx;
			}
		}
		Vector<DPlaceVisitsObject> selPlaces = new Vector<DPlaceVisitsObject>(lastIdx + 1, 10);
		for (int i = 0; i <= lastIdx; i++) {
			selPlaces.addElement(places.elementAt(i));
		}
		return selPlaces;
	}

	/**
	 * Compares two places according to the distortions for sorting the
	 * places in the order of decreaseing distortions
	 */
	public int compare(Object o1, Object o2) {
		if (o1 == null)
			if (o2 == null)
				return 0;
			else
				return 1;
		if (o2 == null)
			return -1;
		if (!(o1 instanceof DPlaceVisitsObject) || !(o2 instanceof DPlaceVisitsObject))
			return 0;
		DPlaceVisitsObject po1 = (DPlaceVisitsObject) o1, po2 = (DPlaceVisitsObject) o2;
		if (po1.sumDistortion > po2.sumDistortion)
			return -1;
		if (po1.sumDistortion < po2.sumDistortion)
			return 1;
		return 0;
	}

	/**
	 * From the given set of places, extracts the visits where the displacement (distance
	 * of the trajectory to the centre) is more than or equal to the specified distance threshold.
	 */
	public static Vector<PlaceVisitInfo> getVisitsBigDistortion(Vector<DPlaceVisitsObject> places, float minDistance) {
		if (places == null || places.size() < 1)
			return null;
		Vector<PlaceVisitInfo> visits = new Vector<PlaceVisitInfo>(places.size() * 50, 100);
		for (int i = 0; i < places.size(); i++) {
			DPlaceVisitsObject pvObj = places.elementAt(i);
			if (pvObj.visits == null) {
				continue;
			}
			int prevSize = visits.size();
			for (int j = 0; j < pvObj.visits.size(); j++) {
				PlaceVisitInfo pvi = (PlaceVisitInfo) pvObj.visits.elementAt(j);
				if (pvi.pCen == null) {
					continue;
				}
				if (pvi.dCen >= minDistance) {
					visits.addElement(pvi);
				}
			}
			if (visits.size() <= prevSize) {
				//no points with so high displacement have been found
				//find a single point with the highest displacement
				PlaceVisitInfo maxDVisit = null;
				for (int j = 0; j < pvObj.visits.size(); j++) {
					PlaceVisitInfo pvi = (PlaceVisitInfo) pvObj.visits.elementAt(j);
					if (pvi.pCen == null) {
						continue;
					}
					if (pvi.dCen > 0 && (maxDVisit == null || pvi.dCen > maxDVisit.dCen)) {
						maxDVisit = pvi;
					}
				}
				if (maxDVisit != null) {
					visits.addElement(maxDVisit);
				}
			}
		}
		if (visits.size() < 1)
			return null;
		return visits;
	}

	/**
	 * Spatially groups the given set of place visits and returns the
	 * centres of the biggest groups.
	 * @param visits - previously extracted visits of some places by moving agents
	 * @param places - the places from which the visits have been extracted. At least
	 *   one of the resulting points must be in each place but not more than two.
	 * @param maxGroupRadius - maximum radius of a group (spatial cluster) of points
	 * @param isGeographic - whether the coordinates are geographic
	 * @return - the centres of the biggest groups of points.
	 */
	public static Vector<RealPoint> getCentresOfBiggestGroups(Vector<PlaceVisitInfo> visits, Vector<DPlaceVisitsObject> places, float maxGroupRadius, boolean isGeographic) {
		if (visits == null || visits.size() < 1)
			return null;
		float minx = Float.NaN, maxx = Float.NaN, miny = Float.NaN, maxy = Float.NaN;
		for (int i = 0; i < visits.size(); i++) {
			RealPoint p = visits.elementAt(i).pCen;
			if (p == null) {
				continue;
			}
			if (Float.isNaN(minx) || minx > p.x) {
				minx = p.x;
			}
			if (Float.isNaN(maxx) || maxx < p.x) {
				maxx = p.x;
			}
			if (Float.isNaN(miny) || miny > p.y) {
				miny = p.y;
			}
			if (Float.isNaN(maxy) || maxy < p.y) {
				maxy = p.y;
			}
		}
		if (Float.isNaN(minx))
			return null;
		if (maxx - minx <= 0 || maxy - miny <= 0)
			return null;
		float width, height, geoFactorX = 1f, geoFactorY = 1f;
		if (isGeographic) {
			float my = (miny + maxy) / 2;
			width = (float) GeoDistance.geoDist(minx, my, maxx, my);
			float mx = (minx + maxx) / 2;
			height = (float) GeoDistance.geoDist(mx, miny, mx, maxy);
			geoFactorX = width / (maxx - minx);
			geoFactorY = height / (maxy - miny);
		} else {
			width = maxx - minx;
			height = maxy - miny;
		}
		double maxRadX = maxGroupRadius, maxRadY = maxGroupRadius;
		if (isGeographic) {
			maxRadX = maxGroupRadius / geoFactorX;
			maxRadY = maxGroupRadius / geoFactorY;
		}
		PointOrganizerSpatialIndex pOrg = new PointOrganizerSpatialIndex();
		pOrg.setSpatialExtent(minx, miny, maxx, maxy);
		pOrg.setMaxRad(Math.min(maxRadX, maxRadY));
		pOrg.setGeo(false, 1, 1);
		for (int i = 0; i < visits.size(); i++) {
			RealPoint p = visits.elementAt(i).pCen;
			if (p == null) {
				continue;
			}
			pOrg.addPoint(p);
		}
		int nGroups = pOrg.getGroupCount();
		if (nGroups < 1)
			return null;
		//pOrg.mergeCloseGroups();
		pOrg.reDistributePoints();
		pOrg.optimizeGrouping();
		nGroups = pOrg.getGroupCount();
		Vector<ObjectWithMeasure> grSizes = new Vector<ObjectWithMeasure>(nGroups, 1);
		for (int i = 0; i < nGroups; i++) {
			grSizes.addElement(new ObjectWithMeasure(new Integer(i), pOrg.getGroup(i).size(), true));
		}
		BubbleSort.sort(grSizes);
		boolean placeHasPoint[] = new boolean[places.size()];
		for (int i = 0; i < places.size(); i++) {
			placeHasPoint[i] = false;
		}
		int nPlacesWithPoints = 0;
		Vector<RealPoint> centres = new Vector<RealPoint>(places.size(), 10);
		for (int i = 0; i < nGroups && nPlacesWithPoints < places.size(); i++) {
			int grIdx = ((Integer) grSizes.elementAt(i).obj).intValue();
			RealPoint p = pOrg.getCentroid(grIdx);
			int pIdx = -1;
			for (int j = 0; j < places.size() && pIdx < 0; j++)
				if (!placeHasPoint[j] && places.elementAt(j).getGeometry().contains(p.x, p.y, 0.0f, true)) {
					pIdx = j;
				}
			if (pIdx >= 0) {
				++nPlacesWithPoints;
				centres.addElement(p);
				placeHasPoint[pIdx] = true;
			}
		}
		return centres;
	}

	protected double findMinDistance(float x, float y, Vector<RealPoint> points, boolean isGeographic) {
		double minD = Double.NaN;
		for (int j = 0; j < points.size(); j++) {
			RealPoint gp = points.elementAt(j);
			double dist = GeoComp.distance(gp.x, gp.y, x, y, isGeographic);
			if (Double.isNaN(minD) || minD > dist) {
				minD = dist;
			}
		}
		return minD;
	}

	/**
	 * Refines the division of the territory by introducing new generating points
	 * for the Voronoi polygons. Returns the object containing the resulting division
	 * and the neighbourhood matrix.
	 */
	public VoronoiNew refineTerritoryDivision(DPlaceVisitsLayer placeLayer, Vector<RealPoint> points, double minDistBetweenGenPoints) {
		if (placeLayer == null || placeLayer.getObjectCount() < 1 || placeLayer.getTrajectoryLayer() == null || points == null || points.size() < 1)
			return null;
		DGeoLayer moveLayer = placeLayer.getTrajectoryLayer();

		Vector<RealPoint> genPoints = new Vector<RealPoint>(placeLayer.getObjectCount() + points.size(), 10);
		boolean processed[] = new boolean[placeLayer.getObjectCount()];
		for (int i = 0; i < processed.length; i++) {
			processed[i] = false;
		}

		float x1 = Float.NaN, x2 = x1, y1 = x1, y2 = x1, sumW = 0, sumH = 0;
		for (int np = 0; np < points.size(); np++) {
			RealPoint p = points.elementAt(np);
			int oIdx = placeLayer.findObjectContainingPosition(p.x, p.y);
			if (oIdx < 0) {
				continue;
			}
			float coord[] = placeLayer.getObject(oIdx).getGeometry().getBoundRect();
			if (Float.isNaN(x1) || x1 > coord[0]) {
				x1 = coord[0];
			}
			if (Float.isNaN(x2) || x2 < coord[2]) {
				x2 = coord[2];
			}
			if (Float.isNaN(y1) || y1 > coord[1]) {
				y1 = coord[1];
			}
			if (Float.isNaN(y2) || y2 < coord[3]) {
				y2 = coord[3];
			}
			sumW += x2 - x1;
			sumH += y2 - y1;
		}
		if (Float.isNaN(x1) || sumW <= 0 || sumH <= 0)
			return null;
		float w = sumW / points.size(), h = sumH / points.size();
		PointOrganizerSpatialIndex pOrg = new PointOrganizerSpatialIndex();
		pOrg.setSpatialExtent(x1, y1, x2, y2);
		pOrg.setGeo(false, 1, 1);
		pOrg.setMaxRad(Math.max(w, h) * 0.3);

		for (int np = 0; np < points.size(); np++) {
			RealPoint p = points.elementAt(np);
			genPoints.addElement(p);
			int oIdx = placeLayer.findObjectContainingPosition(p.x, p.y);
			if (oIdx < 0 || processed[oIdx]) {
				continue;
			}
			DPlaceVisitsObject pObj = (DPlaceVisitsObject) placeLayer.getObject(oIdx);
			if (pObj.visits == null || pObj.visits.size() < 1) {
				continue;
			}
			Geometry geom = pObj.getGeometry();
			Vector<RealPoint> pInside = new Vector<RealPoint>(5, 5);
			pInside.addElement(p);
			for (int j = np + 1; j < points.size(); j++) {
				RealPoint q = points.elementAt(j);
				if (geom.contains(q.x, q.y, 0f, true)) {
					pInside.addElement(q);
				}
			}
			float sumX = 0, sumY = 0;
			int nCloseToCentre = 0;
			for (int i = 0; i < pObj.visits.size(); i++) {
				PlaceVisitInfo vin = (PlaceVisitInfo) pObj.visits.elementAt(i);
				if (vin.pCen == null) {
					continue;
				}
				boolean closeToNewPoint = false;
				for (int j = 0; j < pInside.size() && !closeToNewPoint; j++) {
					RealPoint q = pInside.elementAt(j);
					double dist = GeoComp.distance(vin.pCen.x, vin.pCen.y, q.x, q.y, moveLayer.isGeographic());
					closeToNewPoint = dist < vin.dCen;
				}
				if (closeToNewPoint) {
					continue;
				}
				sumX += vin.pCen.x;
				sumY += vin.pCen.y;
				++nCloseToCentre;
				pOrg.addPoint(vin.pCen);
			}
			/*
			if (nCloseToCentre>0) {
			  sumX/=nCloseToCentre; sumY/=nCloseToCentre;
			  double minD=findMinDistance(sumX,sumY,genPoints,moveLayer.isGeographic());
			  if (Double.isNaN(minD) || minD>=minDistBetweenGenPoints)
			    genPoints.addElement(new RealPoint(sumX,sumY));
			}
			*/
			processed[oIdx] = true;
		}

		pOrg.reDistributePoints();
		pOrg.optimizeGrouping();
		int nGroups = pOrg.getGroupCount();
		Vector<ObjectWithMeasure> grOrd = new Vector<ObjectWithMeasure>(nGroups, 1);
		for (int i = 0; i < nGroups; i++) {
			int size = pOrg.getGroup(i).size();
			grOrd.addElement(new ObjectWithMeasure(new Integer(i), size, true));
		}
		QSortAlgorithm.sort(grOrd);
		double maxD = 0.0;
		int maxIdx = -1, nAdded = 0;
		for (int i = 0; i < grOrd.size(); i++) {
			int idx = ((Integer) grOrd.elementAt(i).obj).intValue();
			RealPoint c = pOrg.getCentroid(idx);
			double minD = findMinDistance(c.x, c.y, genPoints, moveLayer.isGeographic());
			if (Double.isNaN(minD) || minD >= minDistBetweenGenPoints) {
				genPoints.addElement(c);
				++nAdded;
			} else if (minD > maxD) {
				maxD = minD;
				maxIdx = idx;
			}
		}
		if (nAdded < 1) {
			RealPoint c = pOrg.getCentroid(maxIdx);
			genPoints.addElement(c);
		}

		for (int i = 0; i < processed.length; i++)
			if (!processed[i]) {
				float c[] = placeLayer.getObject(i).getGeometry().getCentroid();
				genPoints.addElement(new RealPoint(c[0], c[1]));
			}
		showMessage("Building Voronoi polygons; wait...", false);
		long t0 = System.currentTimeMillis();
		VoronoiNew voronoi = new VoronoiNew(genPoints);
		if (!voronoi.isValid()) {
			showMessage("Failed to triangulate!", true);
			return null;
		}
		voronoi.setBuildNeighbourhoodMatrix(true);
		RealRectangle br = placeLayer.getWholeLayerBounds();
		if (br == null) {
			br = placeLayer.getCurrentLayerBounds();
		}
		RealPolyline areas[] = voronoi.getPolygons(br.rx1, br.ry1, br.rx2, br.ry2);
		if (areas == null) {
			showMessage("Failed to build polygons!", true);
			return null;
		}
		long t = System.currentTimeMillis();
		int nPolygons = 0;
		for (RealPolyline area : areas)
			if (area != null) {
				++nPolygons;
			}
		showMessage("Got " + nPolygons + " Voronoi polygons; elapsed time = " + (t - t0) + " msec.", false);
		System.out.println("Got " + nPolygons + " Voronoi polygons; elapsed time = " + (t - t0) + " msec.");
		return voronoi;
	}
}
