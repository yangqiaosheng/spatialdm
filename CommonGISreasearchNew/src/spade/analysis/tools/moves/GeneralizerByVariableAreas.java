package spade.analysis.tools.moves;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Color;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.TextField;
import java.util.Map;
import java.util.Vector;

import spade.analysis.geocomp.voronoi.VoronoiNew;
import spade.analysis.system.DataLoader;
import spade.analysis.system.ESDACore;
import spade.analysis.tools.BaseAnalyser;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Dialogs;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.OKDialog;
import spade.lib.basicwin.RowLayout;
import spade.lib.util.IdMaker;
import spade.lib.util.ObjectWithMeasure;
import spade.lib.util.QSortAlgorithm;
import spade.lib.util.StringUtil;
import spade.vis.action.Highlighter;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.database.SpatialEntity;
import spade.vis.dmap.DAggregateLinkLayer;
import spade.vis.dmap.DAggregateLinkObject;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;
import spade.vis.dmap.DMovingObject;
import spade.vis.dmap.DPlaceVisitsLayer;
import spade.vis.dmap.DPlaceVisitsObject;
import spade.vis.dmap.DrawingParameters;
import spade.vis.dmap.PlaceVisitInfo;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.RealPoint;
import spade.vis.geometry.RealPolyline;
import spade.vis.geometry.RealRectangle;
import spade.vis.space.GeoLayer;
import spade.vis.space.LayerManager;
import core.ActionDescr;

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: 25-Jan-2010
 * Time: 17:45:05
 * Varies the level of generalization depending on the data density:
 * dense -> small areas, sparse -> large areas
 */
public class GeneralizerByVariableAreas extends BaseAnalyser {
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
		//find a suitable map layer:
		//1) instanceof DAggregateLinkLayer
		//2) has a reference to a DPlaceVisitsLayer, which is also present on the map
		//3) the DPlaceVisitsLayer consists of polygons (supposedly Voronoi polygons)
		LayerManager lman = core.getUI().getCurrentMapViewer().getLayerManager();
		Vector<DAggregateLinkLayer> aggLayers = new Vector(lman.getLayerCount(), 1);
		for (int i = 0; i < lman.getLayerCount(); i++) {
			GeoLayer layer = lman.getGeoLayer(i);
			if ((layer instanceof DAggregateLinkLayer) && layer.getObjectCount() > 1) {
				DAggregateLinkLayer agLayer = (DAggregateLinkLayer) layer;
				DGeoLayer pLayer = agLayer.getPlaceLayer();
				if (pLayer == null || pLayer.getObjectCount() < 2) {
					continue;
				}
				if (!(pLayer instanceof DPlaceVisitsLayer)) {
					continue;
				}
				int idx = lman.getIndexOfLayer(pLayer.getContainerIdentifier());
				if (idx < 0 || !pLayer.equals(lman.getGeoLayer(idx))) {
					continue;
				}
				DGeoObject pObj = pLayer.getObject(0);
				if (pObj.getGeometry() == null || !(pObj.getGeometry() instanceof RealPolyline)) {
					continue;
				}
				aggLayers.addElement(agLayer);
			}
		}
		if (aggLayers.size() < 1) {
			showMessage("No appropriate layers with aggregate moves found!", true);
			return;
		}
		Panel mainP = new Panel(new ColumnLayout());
		mainP.add(new Label("Select the layer with aggregate moves:"));
		List list = new List(Math.max(aggLayers.size() + 1, 5));
		for (int i = 0; i < aggLayers.size(); i++) {
			list.add(aggLayers.elementAt(i).getName());
		}
		list.select(0);
		mainP.add(list);
		OKDialog dia = new OKDialog(getFrame(), "Vary generalization level", true);
		dia.addContent(mainP);
		dia.show();
		if (dia.wasCancelled())
			return;
		int idx = list.getSelectedIndex();
		if (idx < 0)
			return;
		DAggregateLinkLayer agLayer = aggLayers.elementAt(idx);
		DPlaceVisitsLayer placeLayer = (DPlaceVisitsLayer) agLayer.getPlaceLayer();
		Highlighter hl = core.getHighlighterForContainer(placeLayer.getContainerIdentifier());
		if (hl != null) {
			Vector sel = hl.getSelectedObjects();
			if (sel != null && sel.size() > 0 && Dialogs.askYesOrNo(getFrame(), "Do you wish to unite the celected cells?", "Unite selected cells?")) {
				uniteSelectedAreas(placeLayer, sel);
				return;
			}
		}

		mainP = new Panel(new ColumnLayout());
		mainP.add(new Label("The tool will unite neighbouring areas"));
		mainP.add(new Label("when the number of moves between them is"));
		Panel p = new Panel(new BorderLayout());
		p.add(new Label("less than"), BorderLayout.WEST);
		TextField tf = new TextField("5", 3);
		p.add(tf, BorderLayout.CENTER);
		mainP.add(p);
		Checkbox cb2dir = new Checkbox("in each of the two directions", true);
		mainP.add(cb2dir);
		Checkbox cbMaxVisits = new Checkbox("join only in case of less than", false);
		Panel pp = new Panel(new RowLayout());
		pp.add(cbMaxVisits);
		TextField tfMaxVisits = new TextField("20", 3);
		pp.add(tfMaxVisits);
		pp.add(new Label("visits in each area"));
		mainP.add(pp);
		mainP.add(new Line(false));
		CheckboxGroup cbg = new CheckboxGroup();
		Checkbox cbInter = new Checkbox("present result of each step", true, cbg);
		mainP.add(cbInter);
		Checkbox cbNSteps = new Checkbox("make", false, cbg);
		pp = new Panel(new RowLayout());
		pp.add(cbNSteps);
		TextField tfNSteps = new TextField("10", 3);
		pp.add(tfNSteps);
		pp.add(new Label("steps and finish"));
		mainP.add(pp);
		Checkbox cbAuto = new Checkbox("work automatically until there are no areas to join", false, cbg);
		mainP.add(cbAuto);
		dia = new OKDialog(getFrame(), "Condition to join areas", true);
		dia.addContent(mainP);
		dia.show();
		if (dia.wasCancelled())
			return;
		String str = tf.getText();
		int minNMoves = 0;
		if (str != null) {
			try {
				minNMoves = Integer.valueOf(str).intValue();
			} catch (NumberFormatException nfe) {
			}
		}
		if (minNMoves < 2) {
			showMessage("Wrong minimum number of moves specified!", true);
			return;
		}
		int nSteps = 0;
		boolean eachWay = cb2dir.getState(), askUser = cbInter.getState();
		if (!askUser && cbNSteps.getState()) {
			str = tfNSteps.getText();
			if (str != null) {
				try {
					nSteps = Integer.parseInt(str.trim());
				} catch (NumberFormatException nfe) {
				}
			}
			askUser = nSteps <= 0;
		}
		int maxNVisits = -1;
		if (cbMaxVisits.getState()) {
			str = tfMaxVisits.getText();
			if (str != null) {
				try {
					maxNVisits = Integer.parseInt(str.trim());
				} catch (NumberFormatException nfe) {
				}
			}
		}
		furtherGeneralize(agLayer, minNMoves, eachWay, askUser, nSteps, maxNVisits);
	}

	protected void furtherGeneralize(DAggregateLinkLayer agLayer, int minNMoves, boolean eachWay, boolean askUser, int nSteps, int maxNVisits) {
		if (agLayer == null || minNMoves < 2)
			return;
		DPlaceVisitsLayer placeLayer = (DPlaceVisitsLayer) agLayer.getPlaceLayer();
		DPlaceVisitsLayer origPlaceLayer = placeLayer;
		DAggregateLinkLayer origLinkLayer = agLayer;
		DGeoLayer moveLayer = placeLayer.getTrajectoryLayer();
		boolean repeat = false;
		int nCycles = 0;
		//Generalised trajectories
		Vector<DMovingObject> genTr = null;
		ActionDescr aDescr = new ActionDescr();
		aDescr.aName = "Increase the level of spatial generalization in data-sparse regions";
		aDescr.addParamValue("Trajectories layer id", moveLayer.getContainerIdentifier());
		aDescr.addParamValue("Trajectories layer name", moveLayer.getName());
		aDescr.addParamValue("Areas layer id", placeLayer.getContainerIdentifier());
		aDescr.addParamValue("Areas layer name", placeLayer.getName());
		aDescr.startTime = System.currentTimeMillis();
		core.logAction(aDescr);
		do {
			System.gc();
			Vector agLinks = agLayer.getObjects();
			//make a list of aggregate links falling below the threshold ordered by increasing N of moves
			Vector<ObjectWithMeasure> weakLinks = new Vector<ObjectWithMeasure>(agLayer.getObjectCount(), 10);
			for (int i = 0; i < agLinks.size(); i++)
				if (agLinks.elementAt(i) instanceof DAggregateLinkObject) {
					DAggregateLinkObject agLink = (DAggregateLinkObject) agLinks.elementAt(i);
					if (maxNVisits > 0 && (agLink.startNode instanceof DPlaceVisitsObject)) {
						if (((DPlaceVisitsObject) agLink.startNode).getNTrueVisits() > maxNVisits) {
							continue;
						}
						if (((DPlaceVisitsObject) agLink.endNode).getNTrueVisits() > maxNVisits) {
							continue;
						}
					}
					if (agLink.souLinks != null && agLink.souLinks.size() > 0) {
						if (!agLink.startNode.isNeighbour(agLink.endNode.getIdentifier())) {
							continue;
						}
						int idx = -1;
						if (weakLinks.size() > 0) {
							//find if this pair of areas is already present in the list of candidates for joining
							for (int j = 0; j < weakLinks.size() && idx < 0; j++) {
								DAggregateLinkObject lnk = (DAggregateLinkObject) weakLinks.elementAt(j).obj;
								if (lnk.startNode.equals(agLink.endNode) && lnk.endNode.equals(agLink.startNode)) {
									idx = j;
								}
							}
						}
						if (idx < 0) {
							if (agLink.souLinks.size() < minNMoves) {
								int measure = agLink.souLinks.size();
								if (agLink.startNode instanceof DPlaceVisitsObject) {
									measure = ((DPlaceVisitsObject) agLink.startNode).getNTrueVisits() + ((DPlaceVisitsObject) agLink.endNode).getNTrueVisits();
								}
								ObjectWithMeasure om = new ObjectWithMeasure(agLink, measure);
								weakLinks.addElement(om);
							}
						} else {
							if (eachWay && agLink.souLinks.size() >= minNMoves) {
								weakLinks.removeElementAt(idx);
								/*
								else
								if (agLink.souLinks.size()<weakLinks.elementAt(idx).measure)
								  weakLinks.elementAt(idx).measure=agLink.souLinks.size();
								*/
							}
						}
					}
				}
			if (weakLinks.size() < 1) {
				showMessage("No pairs of neighbouring areas with N moves < " + minNMoves + " found!", true);
				break;
			}
			showMessage("Found " + weakLinks.size() + " candidate pairs of areas to join", false);
			System.out.println("Found " + weakLinks.size() + " candidate pairs of areas to join");
			QSortAlgorithm.sort(weakLinks, false);
			//remove the pairs where one of the areas appears also in another pair
			for (int i = weakLinks.size() - 1; i > 1; i--) {
				DAggregateLinkObject lnk1 = (DAggregateLinkObject) weakLinks.elementAt(i).obj;
				boolean remove = false;
				for (int j = 0; j < i && !remove; j++) {
					DAggregateLinkObject lnk2 = (DAggregateLinkObject) weakLinks.elementAt(j).obj;
					remove = lnk1.startNode.equals(lnk2.startNode) || lnk1.startNode.equals(lnk2.endNode) || lnk1.endNode.equals(lnk2.startNode) || lnk1.endNode.equals(lnk2.endNode);
				}
				if (remove) {
					weakLinks.removeElementAt(i);
				}
			}
			showMessage(weakLinks.size() + " pairs of areas remained after removing duplicates", false);
			System.out.println(weakLinks.size() + " pairs of areas remained after removing duplicates");
			//make a list of generating points for a new Voronoi tessellation
			Vector<RealPoint> genPts = new Vector<RealPoint>(placeLayer.getObjectCount(), 1);
			Vector<DGeoObject> usedAreas = new Vector<DGeoObject>(placeLayer.getObjectCount(), 1);
			for (int i = 0; i < weakLinks.size(); i++) {
				DAggregateLinkObject lnk = (DAggregateLinkObject) weakLinks.elementAt(i).obj;
				PointsInCircle pic = new PointsInCircle(1, 1);
				for (int j = 0; j < 2; j++) {
					DPlaceVisitsObject pvo = (DPlaceVisitsObject) ((j == 0) ? lnk.startNode : lnk.endNode);
					for (int k = 0; k < pvo.visits.size(); k++) {
						PlaceVisitInfo vi = (PlaceVisitInfo) pvo.visits.elementAt(k);
						if (vi.justCrossed) {
/*
              if (vi.pCen!=null)
                pic.addPoint(vi.pCen);
*/
						} else {
							if (vi.track == null) {
								continue;
							}
							for (int n = vi.firstIdx; n <= vi.lastIdx; n++) {
								SpatialEntity spe = (SpatialEntity) vi.track.elementAt(n);
								pic.addPoint(spe.getCentre());
							}
						}
					}
					usedAreas.addElement(pvo);
				}
				RealPoint pt = pic.getTrueMedoid();
				if (pt != null) {
					genPts.addElement(pt);
				}
			}
			for (int i = 0; i < placeLayer.getObjectCount(); i++) {
				DGeoObject place = placeLayer.getObject(i);
				if (!usedAreas.contains(place)) {
					float c[] = place.getGeometry().getCentroid();
					genPts.addElement(new RealPoint(c[0], c[1]));
					usedAreas.addElement(place);
				}
			}
			showMessage("Building Voronoi polygons; wait...", false);
			long t0 = System.currentTimeMillis();
			VoronoiNew voronoi = new VoronoiNew(genPts);
			if (!voronoi.isValid()) {
				showMessage("Failed to triangulate!", true);
				break;
			}
			voronoi.setBuildNeighbourhoodMatrix(true);
			RealRectangle br = placeLayer.getWholeLayerBounds();
			if (br == null) {
				br = placeLayer.getCurrentLayerBounds();
			}
			RealPolyline areas[] = voronoi.getPolygons(br.rx1, br.ry1, br.rx2, br.ry2);
			if (areas == null) {
				showMessage("Failed to build polygons!", true);
				break;
			}
			long t = System.currentTimeMillis();
			int nPolygons = 0;
			for (RealPolyline area : areas)
				if (area != null) {
					++nPolygons;
				}
			showMessage("Got " + nPolygons + " Voronoi polygons; elapsed time = " + (t - t0) + " msec.", false);
			System.out.println("Got " + nPolygons + " Voronoi polygons; elapsed time = " + (t - t0) + " msec.");
			TrajectoriesGeneraliser trGen = new TrajectoriesGeneraliser();
			trGen.setCore(core);
			trGen.setMayAskUser(false);
			trGen.setAddLayersAndTables(askUser);
			boolean ok = trGen.summarizeByPolygons(moveLayer, voronoi.getResultingCells(), voronoi.getNeighbourhoodMap(), placeLayer.onlyActiveTrajectories, placeLayer.onlyStartsEnds, placeLayer.findIntersections, true);
			if (!ok) {
				break;
			}
			Vector<DPlaceVisitsObject> genPlaces = trGen.getPlaces();
			if (genPlaces == null || genPlaces.size() < 1) {
				break;
			}
			int nRemoved = origPlaceLayer.getObjectCount() - genPlaces.size();
			aDescr.addParamValue("N areas removed in step " + (nCycles + 1), new Integer(weakLinks.size()));
			trGen.makeLayersAndTables(moveLayer, "generalized; " + nRemoved + "(" + weakLinks.size() + ") areas removed", genPlaces, trGen.getAggMoves(), placeLayer.onlyActiveTrajectories, placeLayer.onlyStartsEnds, placeLayer.findIntersections,
					aDescr);
			DPlaceVisitsLayer genPlaceLayer = trGen.getPlaceLayer();
			if (genPlaceLayer != null) {
				genPlaceLayer.setOrigPlaceLayer(placeLayer);
			}
			DAggregateLinkLayer genLinkLayer = trGen.getAggLinkLayer();
			++nCycles;
			System.out.println(nCycles + " iteration steps have been done");
			if (askUser) {
				repeat = Dialogs.askYesOrNo(getFrame(), nCycles + " steps have been done, \n" + weakLinks.size() + " areas have been removed " + "in the last step, \n" + nRemoved + " in total. \n"
						+ "Do you wish to do one more step of the generalization?", "Repeat?");
			} else {
				repeat = nSteps < 1 || nCycles < nSteps;
			}
			if (repeat) {
				if (!agLayer.equals(origLinkLayer)) {
					core.removeMapLayer(agLayer.getContainerIdentifier(), true);
				}
				agLayer = genLinkLayer;
				if (!placeLayer.equals(origPlaceLayer)) {
					core.removeMapLayer(placeLayer.getContainerIdentifier(), true);
				}
				placeLayer = genPlaceLayer;
				genTr = trGen.getGeneralizedTrajectories();
			} else if (!agLayer.equals(origLinkLayer)) {
				if (!askUser
						|| Dialogs.askYesOrNo(getFrame(), "Press \"Yes\" if you wish to keep the " + "last result and erase the previous one. Otherwise, the previous result " + "will be taken as the final and the last result will be erased.",
								"Keep the last result?")) {
					core.removeMapLayer(agLayer.getContainerIdentifier(), true);
					agLayer = genLinkLayer;
					core.removeMapLayer(placeLayer.getContainerIdentifier(), true);
					placeLayer = genPlaceLayer;
					genTr = trGen.getGeneralizedTrajectories();
				} else {
					core.removeMapLayer(genLinkLayer.getContainerIdentifier(), true);
					core.removeMapLayer(genPlaceLayer.getContainerIdentifier(), true);
				}
			}
		} while (repeat);
		aDescr.endTime = System.currentTimeMillis();
		aDescr.addParamValue("N steps", new Integer(nCycles));
		int nRemoved = origPlaceLayer.getObjectCount() - placeLayer.getObjectCount();
		aDescr.addParamValue("N areas removed in total", new Integer(nRemoved));
		if (!askUser && agLayer != null && !agLayer.equals(origLinkLayer)) {
			DataLoader dataLoader = core.getDataLoader();
			dataLoader.addMapLayer(placeLayer, -1);
			DataTable placeTbl = (DataTable) placeLayer.getThematicData();
			int tblN = dataLoader.addTable(placeTbl);
			placeTbl.setEntitySetIdentifier(placeLayer.getEntitySetIdentifier());
			dataLoader.setLink(placeLayer, tblN);
			placeLayer.setLinkedToTable(true);
			DataTable aggTbl = (DataTable) agLayer.getThematicData();
			int aggTblN = dataLoader.addTable(aggTbl);
			dataLoader.addMapLayer(agLayer, -1);
			aggTbl.setEntitySetIdentifier(agLayer.getEntitySetIdentifier());
			dataLoader.setLink(agLayer, aggTblN);
			agLayer.setLinkedToTable(true);
		}
		if (genTr != null && genTr.size() > 0 && Dialogs.askYesOrNo(getFrame(), "Make a map layer with generalized trajectories?", "Generalized trajectories")) {
			//make a layer with the generalised trajectories and the corresponding table
			String layerName = "Generalised " + moveLayer.getName() + " by " + placeLayer.getName();
			DMovingObject mobj = genTr.elementAt(0);
			DataTable dtTraj = null;
			if (mobj.getData() != null && (mobj.getData() instanceof DataRecord)) {
				AttributeDataPortion tblSrcTraj = moveLayer.getThematicData();
				int idx0 = 0;
				if (tblSrcTraj != null) {
					idx0 = tblSrcTraj.getAttrCount();
					String pref = "List of places", name = pref;
					for (int i = 2; tblSrcTraj.findAttrByName(name) >= 0; i++) {
						name = pref + " (" + i + ")";
					}
					tblSrcTraj.addAttribute(name, IdMaker.makeId(name, (DataTable) tblSrcTraj), AttributeTypes.character);
					pref = "N of visited places";
					name = pref;
					for (int i = 2; tblSrcTraj.findAttrByName(name) >= 0; i++) {
						name = pref + " (" + i + ")";
					}
					tblSrcTraj.addAttribute(name, IdMaker.makeId(name, (DataTable) tblSrcTraj), AttributeTypes.integer);
				}
				dtTraj = new DataTable();
				dtTraj.setName(layerName + ": general data");
				DataRecord rec = (DataRecord) mobj.getData();
				rec.setAttrValue(mobj.listOfVisitedAreas, idx0);
				rec.setNumericAttrValue(mobj.nVisitedAreas, String.valueOf(mobj.nVisitedAreas), idx0 + 1);
				DataRecord rec1 = rec.makeCopy(true, true);
				dtTraj.setAttrList(rec1.getAttrList());
				dtTraj.addDataRecord(rec1);
				mobj.setThematicData(rec1);
				for (int i = 1; i < genTr.size(); i++) {
					mobj = genTr.elementAt(i);
					rec = (DataRecord) mobj.getData();
					if (rec != null) {
						rec.setAttrValue(mobj.listOfVisitedAreas, idx0);
						rec.setNumericAttrValue(mobj.nVisitedAreas, String.valueOf(mobj.nVisitedAreas), idx0 + 1);
						rec1 = rec.makeCopy(false, true);
						dtTraj.addDataRecord(rec1);
						mobj.setThematicData(rec1);
					}
				}
			}
			DGeoLayer tLayer = new DGeoLayer();
			tLayer.setType(Geometry.line);
			tLayer.setName(layerName);
			tLayer.setGeoObjects(genTr, true);
			tLayer.setHasMovingObjects(true);
			tLayer.setGeographic(moveLayer.isGeographic());
			DrawingParameters dp = tLayer.getDrawingParameters();
			if (dp == null) {
				dp = new DrawingParameters();
				tLayer.setDrawingParameters(dp);
			}
			dp.lineColor = Color.getHSBColor((float) Math.random(), 1 - 0.2f * (float) Math.random(), 1 - 0.2f * (float) Math.random());
			dp.lineWidth = 2;
			dp.transparency = 0;
			DataLoader dLoader = core.getDataLoader();
			dLoader.addMapLayer(tLayer, -1);
			dLoader.processTimeReferencedObjectSet(tLayer);
			if (dtTraj != null) {
				int trTblN = dLoader.addTable(dtTraj);
				tLayer.setDataTable(dtTraj);
				dLoader.setLink(tLayer, trTblN);
				tLayer.setLinkedToTable(true);
				tLayer.setThematicFilter(dtTraj.getObjectFilter());
				dLoader.processTimeReferencedObjectSet(dtTraj);
			}
		}
	}

	protected Vector<DGeoObject> areNeighbours(DGeoLayer areaLayer, Vector placeIds) {
		if (areaLayer == null || placeIds == null || placeIds.size() < 2)
			return null;
		Vector<DGeoObject> areas = new Vector<DGeoObject>(placeIds.size(), 1);
		for (int i = 0; i < placeIds.size(); i++) {
			int k = areaLayer.getObjectIndex((String) placeIds.elementAt(i));
			if (k < 0) {
				continue;
			}
			DGeoObject area = areaLayer.getObject(k);
			if (area.neighbours == null || area.neighbours.size() < 1)
				return null;
			areas.addElement(area);
		}
		if (areas.size() < 2)
			return null;
		//check if each area is a neighbour of some other area
		for (int i = 0; i < areas.size(); i++) {
			boolean hasNei = false;
			String id = areas.elementAt(i).getIdentifier();
			for (int j = 0; j < areas.size() && !hasNei; j++)
				if (j != i) {
					hasNei = areas.elementAt(j).isNeighbour(id);
				}
			if (!hasNei)
				return null;
		}
		return areas;
	}

	protected void uniteSelectedAreas(DPlaceVisitsLayer placeLayer, Vector<String> placeIds) {
		if (placeLayer == null || placeIds == null)
			return;
		Vector<DGeoObject> places = areNeighbours(placeLayer, placeIds);
		if (places == null) {
			showMessage("The areas are not neighbours!", true);
			return;
		}
		ActionDescr aDescr = new ActionDescr();
		aDescr.aName = "Join selected areas";
		aDescr.addParamValue("Areas layer id", placeLayer.getContainerIdentifier());
		aDescr.addParamValue("Areas layer name", placeLayer.getName());
		aDescr.addParamValue("Ids of areas to join", placeIds);
		aDescr.startTime = System.currentTimeMillis();
		core.logAction(aDescr);
		PointsInCircle pic = new PointsInCircle(1, 1);
		Vector<RealPoint> genPts = new Vector<RealPoint>(placeLayer.getObjectCount(), 1);
		Vector<DGeoObject> usedAreas = new Vector<DGeoObject>(placeLayer.getObjectCount(), 1);
		for (int j = 0; j < places.size(); j++) {
			DPlaceVisitsObject pvo = (DPlaceVisitsObject) places.elementAt(j);
			for (int k = 0; k < pvo.visits.size(); k++) {
				PlaceVisitInfo vi = (PlaceVisitInfo) pvo.visits.elementAt(k);
				if (!vi.justCrossed) {
					if (vi.track == null) {
						continue;
					}
					for (int n = vi.firstIdx; n <= vi.lastIdx; n++) {
						SpatialEntity spe = (SpatialEntity) vi.track.elementAt(n);
						pic.addPoint(spe.getCentre());
					}
				}
			}
			usedAreas.addElement(pvo);
		}
		RealPoint pt = pic.getTrueMedoid();
		if (pt != null) {
			genPts.addElement(pt);
		}
		for (int i = 0; i < placeLayer.getObjectCount(); i++) {
			DGeoObject place = placeLayer.getObject(i);
			if (!usedAreas.contains(place)) {
				float c[] = place.getGeometry().getCentroid();
				genPts.addElement(new RealPoint(c[0], c[1]));
				usedAreas.addElement(place);
			}
		}
		showMessage("Building Voronoi polygons; wait...", false);
		long t0 = System.currentTimeMillis();
		VoronoiNew voronoi = new VoronoiNew(genPts);
		if (!voronoi.isValid()) {
			showMessage("Failed to triangulate!", true);
			return;
		}
		voronoi.setBuildNeighbourhoodMatrix(true);
		RealRectangle br = placeLayer.getWholeLayerBounds();
		if (br == null) {
			br = placeLayer.getCurrentLayerBounds();
		}
		RealPolyline areas[] = voronoi.getPolygons(br.rx1, br.ry1, br.rx2, br.ry2);
		if (areas == null) {
			showMessage("Failed to build polygons!", true);
			return;
		}
		long t = System.currentTimeMillis();
		int nPolygons = 0;
		for (RealPolyline area : areas)
			if (area != null) {
				++nPolygons;
			}
		showMessage("Got " + nPolygons + " Voronoi polygons; elapsed time = " + (t - t0) + " msec.", false);
		System.out.println("Got " + nPolygons + " Voronoi polygons; elapsed time = " + (t - t0) + " msec.");
		Map<Integer, Integer> neighbourMap = voronoi.getNeighbourhoodMap();
		Vector<DGeoObject> pvObjects = new Vector<DGeoObject>(areas.length, 10);
		int pvIdxs[] = new int[areas.length];
		int nAr = areas.length, nDigits = 0;
		do {
			nAr /= 10;
			++nDigits;
		} while (nAr > 1);
		for (int i = 0; i < areas.length; i++)
			if (areas[i] != null) {
				String id = StringUtil.padString(String.valueOf(i + 1), '0', nDigits, true);
				SpatialEntity spe = new SpatialEntity(id, id);
				spe.setGeometry(areas[i]);
				DPlaceVisitsObject obj = new DPlaceVisitsObject();
				obj.setup(spe);
				obj.setLabel(id);
				pvObjects.addElement(obj);
				pvIdxs[i] = pvObjects.size() - 1;
			} else {
				pvIdxs[i] = -1;
			}
		//adding information about the neighbours of the places
		if (neighbourMap != null) {
			for (int i = 0; i < areas.length; i++)
				if (pvIdxs[i] >= 0) {
					DGeoObject pObj = pvObjects.elementAt(pvIdxs[i]);
					for (int j = 0; j < areas.length; j++)
						if (j != i && neighbourMap.get(i) == j && pvIdxs[j] >= 0) {
							pObj.addNeighbour(pvObjects.elementAt(pvIdxs[j]));
						}
				}
		}
		TrajectoriesGeneraliser trGen = new TrajectoriesGeneraliser();
		trGen.setCore(core);
		trGen.setMayAskUser(true);
		trGen.setAddLayersAndTables(true);
		boolean ok = trGen.summarizeByPolygons(placeLayer.getTrajectoryLayer(), voronoi.getResultingCells(), voronoi.getNeighbourhoodMap(), placeLayer.onlyActiveTrajectories, placeLayer.onlyStartsEnds, placeLayer.findIntersections, false);
		if (!ok)
			return;
		Vector<DPlaceVisitsObject> genPlaces = trGen.getPlaces();
		if (genPlaces == null || genPlaces.size() < 1)
			return;
		trGen.makeLayersAndTables(placeLayer.getTrajectoryLayer(), places.size() + " areas joined", genPlaces, trGen.getAggMoves(), placeLayer.onlyActiveTrajectories, placeLayer.onlyStartsEnds, placeLayer.findIntersections, aDescr);
		aDescr.endTime = System.currentTimeMillis();
	}
}
