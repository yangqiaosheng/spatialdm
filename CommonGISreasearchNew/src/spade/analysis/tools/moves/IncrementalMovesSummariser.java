package spade.analysis.tools.moves;

import java.awt.Checkbox;
import java.awt.Color;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.TextField;
import java.util.Vector;

import spade.analysis.system.DataLoader;
import spade.analysis.system.DisplayProducer;
import spade.analysis.system.ESDACore;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.OKDialog;
import spade.lib.util.BubbleSort;
import spade.lib.util.IdMaker;
import spade.lib.util.StringUtil;
import spade.time.Date;
import spade.time.TimeMoment;
import spade.time.TimeReference;
import spade.time.ui.EnterDateSchemeUI;
import spade.time.ui.TimeDialogs;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.database.Parameter;
import spade.vis.database.SpatialEntity;
import spade.vis.dataview.ShowRecManager;
import spade.vis.dmap.DAggregateLinkLayer;
import spade.vis.dmap.DAggregateLinkObject;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;
import spade.vis.dmap.DLinkLayer;
import spade.vis.dmap.DLinkObject;
import spade.vis.dmap.DMovingObject;
import spade.vis.dmap.DPlaceVisitsLayer;
import spade.vis.dmap.DPlaceVisitsObject;
import spade.vis.dmap.DVectorGridLayer;
import spade.vis.dmap.DrawingParameters;
import spade.vis.geometry.Geometry;
import spade.vis.map.MapViewer;
import spade.vis.mapvis.DataMapper;
import spade.vis.mapvis.Visualizer;
import spade.vis.space.GeoLayer;
import spade.vis.space.LayerManager;
import spade.vis.spec.DataSourceSpec;
import spade.vis.spec.LinkDataDescription;
import core.ActionDescr;
import core.ResultDescr;
import data_load.DataManager;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: May 4, 2009
 * Time: 12:20:07 PM
 * Uses a given territory division (e.g. Voronoi cells) to summarise incrementally
 * trajectories, e.g. coming from a database. The trajectories are added one by one.
 */
public class IncrementalMovesSummariser {
	/**
	 * provides access to the status bar, main window, data loader, etc.
	 */
	protected ESDACore core = null;
	/**
	 * Whether the coordinates in the summarised trajectories are geographic,
	 * i.e. latituides and longitudes
	 */
	public boolean geo = false;
	/**
	 * The name of the source DB table or layer with the trajectories
	 */
	public String origTrajSetName = null;
	/**
	 * Actually summarises the trajectories
	 */
	protected MovesAccumulator mAcc = null;
	/**
	 * The description of the operation
	 */
	public ActionDescr aDescr = null;
	/**
	 * The temporal parameter in case of aggregation by time intervals
	 */
	protected Parameter timeParam = null;

	/**
	 * @param core - provides access to the status bar, main window, data loader, etc.
	 * @param origTrajSetName - the name of the table or layer with the original data
	 *   (used for the generation of the names of the resulting layer and table)
	 * @param makeDynamicAggregates - whether the result must be dynamic aggregates,
	 *   which react to filtering and colouring of the original trajectories
	 */
	public boolean init(ESDACore core, String origTrajSetName, boolean makeDynamicAggregates) {
		this.core = core;
		this.origTrajSetName = origTrajSetName;
		if (core.getUI().getCurrentMapViewer() == null || core.getUI().getCurrentMapViewer().getLayerManager() == null) {
			core.getUI().showMessage("No map exists!", true);
			return false;
		}
		//Find instances of DGeoLayer containing areas
		LayerManager lman = core.getUI().getCurrentMapViewer().getLayerManager();
		Vector areaLayers = new Vector(lman.getLayerCount(), 1);
		for (int i = 0; i < lman.getLayerCount(); i++) {
			GeoLayer layer = lman.getGeoLayer(i);
			if ((layer instanceof DGeoLayer) && layer.getObjectCount() > 0)
				if (layer.getType() == Geometry.area && layer.getObjectCount() > 1) {
					areaLayers.addElement(layer);
				}
		}
		if (areaLayers.size() < 1) {
			core.getUI().showMessage("No layers with areas found!", true);
			return false;
		}
		Panel mainP = new Panel(new ColumnLayout());
		mainP.add(new Label("Select the layer with areas:"));
		List aList = new List(Math.max(areaLayers.size() + 1, 5));
		for (int i = 0; i < areaLayers.size(); i++) {
			aList.add(((DGeoLayer) areaLayers.elementAt(i)).getName());
		}
		aList.select(aList.getItemCount() - 1);
		mainP.add(aList);
		Checkbox cbNei = new Checkbox("count also visits in neighbouring areas, if possible", false);
		mainP.add(cbNei);
		Checkbox cbStartsEnds = new Checkbox("use only the start and end positions", false);
		mainP.add(cbStartsEnds);
		Checkbox cbIntersect = new Checkbox("intersect the trajectories with the areas", true);
		mainP.add(cbIntersect);
		Checkbox cbActiveAreas = new Checkbox("use only active (after filtering) areas", false);
		mainP.add(cbActiveAreas);
		OKDialog dia = new OKDialog(core.getUI().getMainFrame(), "Summarise trajectories", true);
		dia.addContent(mainP);
		dia.show();
		if (dia.wasCancelled())
			return false;
		int idx = aList.getSelectedIndex();
		if (idx < 0)
			return false;
		DGeoLayer areaLayer = (DGeoLayer) areaLayers.elementAt(idx);
		boolean intersect = cbIntersect.getState();
		boolean useOnlyStartsEnds = cbStartsEnds.getState();
		if (!init(core, origTrajSetName, areaLayer, cbActiveAreas.getState(), useOnlyStartsEnds, intersect, makeDynamicAggregates))
			return false;
		mAcc.setCountVisitsAround(cbNei.getState());
		return true;
	}

	/**
	 * @param core - provides access to the status bar, main window, data loader, etc.
	 * @param origTrajSetName - the name of the table or layer with the original data
	 *   (used for the generation of the names of the resulting layer and table)
	 * @param areaLayer - the layer with the territory division, which must be used
	 *   for the incremental summarisation
	 * @param onlyActiveAreas - whether only active (after filtering) areas are used
	 * @param onlyStartsEnds - whether only start and end points of the trajectories
	 *   must be taken into account
	 * @param findIntersections - whether the intersections of the trajectories
	 *   with the areas must be computed and used
	 * @param makeDynamicAggregates - whether the result must be dynamic aggregates,
	 *   which react to filtering and colouring of the original trajectories
	 */
	public boolean init(ESDACore core, String origTrajSetName, DGeoLayer areaLayer, boolean onlyActiveAreas, boolean onlyStartsEnds, boolean findIntersections, boolean makeDynamicAggregates) {
		this.core = core;
		this.origTrajSetName = origTrajSetName;
		if (core == null)
			return false;
		if (areaLayer == null || areaLayer.getObjectCount() < 1) {
			core.getUI().showMessage("No areas found!", true);
			return false;
		}
		geo = areaLayer.isGeographic();

		aDescr = new ActionDescr();
		aDescr.aName = "Summarisation of trajectories from " + origTrajSetName + " by " + areaLayer.getName();
		aDescr.startTime = System.currentTimeMillis();
		aDescr.addParamValue("Table with trajectories", origTrajSetName);
		aDescr.addParamValue("Layer with areas", areaLayer.getName());
		aDescr.addParamValue("Use only starts and ends", new Boolean(onlyStartsEnds));
		aDescr.addParamValue("Compute intersections of trajectories with areas", new Boolean(findIntersections));
		core.logAction(aDescr);

		int nAreas = areaLayer.getObjectCount();
		boolean placesHaveNames = false;
		for (int i = 0; i < nAreas && !placesHaveNames; i++) {
			DGeoObject obj = areaLayer.getObject(i);
			placesHaveNames = obj.getName() != null;
		}
		int nActiveAreas = (onlyActiveAreas) ? 0 : nAreas;
		if (onlyActiveAreas) {
			for (int i = 0; i < nAreas; i++)
				if (areaLayer.isObjectActive(i)) {
					++nActiveAreas;
				}
		}
		if (nActiveAreas < 2) {
			core.getUI().showMessage("Less than 2 areas are available!", true);
			return false;
		}
		boolean areasFiltered = nActiveAreas < nAreas;

		Vector<DPlaceVisitsCounter> sumPlaces = new Vector(nActiveAreas, 1);
		int lIdxs[] = new int[nActiveAreas];
		for (int i = 0; i < lIdxs.length; i++) {
			lIdxs[i] = -1;
		}

		DVectorGridLayer grid = (areaLayer instanceof DVectorGridLayer) ? (DVectorGridLayer) areaLayer : null;

		for (int i = 0; i < nAreas; i++) {
			if (areasFiltered && !areaLayer.isObjectActive(i)) {
				continue;
			}
			DGeoObject gobj = areaLayer.getObject(i);
			if (gobj.getGeometry() == null) {
				continue;
			}
			SpatialEntity spe = new SpatialEntity(gobj.getIdentifier());
			spe.setGeometry(gobj.getGeometry());
			DPlaceVisitsCounter pvCounter = (makeDynamicAggregates) ? new DPlaceVisitsObject() : new DPlaceVisitsCounter();
			pvCounter.setup(spe);
			if (!areasFiltered)
				if (gobj.neighbours != null) {
					pvCounter.neighbours = (Vector) gobj.neighbours.clone();
				} else if (grid != null) {
					int pos[] = grid.getRowAndColumn(i);
					if (pos == null) {
						continue;
					}
					pvCounter.neighbours = new Vector<String>(8, 1);
					int r1 = pos[0] - 1, r2 = pos[0] + 1, c1 = pos[1] - 1, c2 = pos[1] + 1;
					if (r1 < 0) {
						r1 = 0;
					}
					if (r2 >= grid.getNRows()) {
						r2 = grid.getNRows() - 1;
					}
					if (c1 < 0) {
						c1 = 0;
					}
					if (c2 >= grid.getNCols()) {
						c2 = grid.getNCols() - 1;
					}
					for (int r = r1; r <= r2; r++) {
						for (int c = c1; c <= c2; c++)
							if (r != pos[0] || c != pos[1]) {
								int oIdx = grid.getObjectIndex(r, c);
								if (oIdx >= 0) {
									pvCounter.neighbours.addElement(grid.getObjectId(oIdx));
								}
							}
					}
				}
			if (placesHaveNames) {
				pvCounter.setLabel(areaLayer.getObject(i).getName());
			}
			sumPlaces.addElement(pvCounter);
			lIdxs[sumPlaces.size() - 1] = i;
		}
		boolean hasDataAboutNeighbours = false;
		//add data about the neighbours, if available
		if (!areasFiltered) {
			for (int i = 0; i < sumPlaces.size(); i++) {
				DPlaceVisitsCounter place = sumPlaces.elementAt(i);
				DGeoObject gobj = areaLayer.getObject(lIdxs[i]);
				if (gobj.neighbours != null) {
					for (int j = 0; j < gobj.neighbours.size(); j++) {
						DPlaceVisitsCounter nb = findPlaceWithId(sumPlaces, gobj.neighbours.elementAt(j));
						if (nb != null) {
							place.addNeighbour(nb);
						}
					}
				}
				hasDataAboutNeighbours = hasDataAboutNeighbours || (place.neighbours != null && place.neighbours.size() > 0);
			}
		}
		boolean[][] neiMatrix = null;
		if (hasDataAboutNeighbours) {
			neiMatrix = new boolean[sumPlaces.size()][sumPlaces.size()];
			for (int i = 0; i < sumPlaces.size(); i++) {
				for (int j = 0; j < sumPlaces.size(); j++) {
					neiMatrix[i][j] = false;
				}
			}
			for (int i = 0; i < sumPlaces.size(); i++) {
				DPlaceVisitsCounter place = sumPlaces.elementAt(i);
				if (place.neighbours != null) {
					for (int j = 0; j < place.neighbours.size(); j++) {
						int k = indexOfPlaceWithId(sumPlaces, place.neighbours.elementAt(j));
						if (k >= 0) {
							neiMatrix[i][k] = neiMatrix[k][i] = true;
						}
					}
				}
			}
		}
		mAcc = new MovesAccumulator();
		mAcc.setGeo(geo);
		mAcc.setSumPlaces(sumPlaces);
		mAcc.setNeiMatrix(neiMatrix);
		mAcc.setUseOnlyStartsEnds(onlyStartsEnds);
		mAcc.setFindIntersections(findIntersections);
		mAcc.setMakeDynamicAggregates(makeDynamicAggregates);

		aDescr.endTime = System.currentTimeMillis();
		return true;
	}

	/**
	 * @param start - start of the whole time span (what is before must be ignored)
	 * @param end - end of the whole time span (what is after must be ignored)
	 * @param breaks - the breaks that divide the time span into intervals
	 * @param useCycle - whether the division is done according to the cyclical time model
	 * @param cycleUnit - units of the cycle
	 * @param nCycleElements - length of the cycle
	 * @param cycleName - name of the cycle
	 */
	public void setTemporalAggregationParameters(TimeMoment start, TimeMoment end, Vector<TimeMoment> breaks, boolean useCycle, char cycleUnit, int nCycleElements, String cycleName) {
		mAcc.setTemporalAggregationParameters(start, end, breaks, useCycle, cycleUnit, nCycleElements, cycleName);
	}

	protected DPlaceVisitsCounter findPlaceWithId(Vector<DPlaceVisitsCounter> sumPlaces, String id) {
		for (int i = 0; i < sumPlaces.size(); i++)
			if (sumPlaces.elementAt(i).getIdentifier().equals(id))
				return sumPlaces.elementAt(i);
		return null;
	}

	protected int indexOfPlaceWithId(Vector<DPlaceVisitsCounter> sumPlaces, String id) {
		for (int i = 0; i < sumPlaces.size(); i++)
			if (sumPlaces.elementAt(i).getIdentifier().equals(id))
				return i;
		return -1;
	}

	/**
	 * Adds the given trajectory to the summary
	 */
	public boolean addTrajectory(DMovingObject trajectory) {
		if (mAcc != null && trajectory != null)
			return mAcc.accumulate(trajectory);
		return false;
	}

	/**
	 * Builds
	 * 1) a map layer and a corresponding table with data about the places
	 * 2) a map layer and a corresponding table from the accumulated moves
	 * @param moveLayer - the layer with the original trajectories; may be null
	 */
	public DGeoLayer[] makeLayers(DGeoLayer moveLayer) {
		if (mAcc == null || mAcc.sumPlaces == null || mAcc.sumPlaces.size() < 1 || ((mAcc.sumMoves == null || mAcc.sumMoves.size() < 1) && (mAcc.aggLinks == null || mAcc.aggLinks.size() < 1))) {
			core.getUI().showMessage("No summarisation produced!", true);
			return null;
		}

		DGeoLayer placeLayer = (mAcc.makesDynamicAggregates()) ? makePlaceVisitsLayer(moveLayer) : makeSimplePlaceLayer();

		DGeoLayer sumMovesLayer = null;
		if (mAcc.sumMoves != null && mAcc.sumMoves.size() > 0) {
			sumMovesLayer = makeSimpleLinkLayer(placeLayer);
		} else if (mAcc.aggLinks != null && mAcc.aggLinks.size() > 0) {
			sumMovesLayer = makeAggregateLinkLayer(placeLayer, moveLayer);
		}

		aDescr.addParamValue("N trajectories processed", new Long(mAcc.count));
		aDescr.endTime = System.currentTimeMillis();
		DGeoLayer result[] = { placeLayer, sumMovesLayer };
		return result;
	}

	protected DPlaceVisitsLayer makePlaceVisitsLayer(DGeoLayer moveLayer) {
		String name = "Places visited by trajectories from " + origTrajSetName;
/*
    name= Dialogs.askForStringValue(core.getUI().getMainFrame(),"Name of the layer?",name,
      "A map layer with the visited places will be generated","Visited places",false);
*/
		DPlaceVisitsLayer placeLayer = new DPlaceVisitsLayer();
		placeLayer.setGeographic(moveLayer.isGeographic());
		placeLayer.onlyActiveTrajectories = false;
		placeLayer.onlyStartsEnds = mAcc.useOnlyStartsEnds;
		placeLayer.findIntersections = mAcc.findIntersections;
		placeLayer.setType(Geometry.area);
		placeLayer.setName(name);
		placeLayer.setGeoObjects(mAcc.sumPlaces, true);
		placeLayer.setTrajectoryLayer(moveLayer);
		DrawingParameters dp = placeLayer.getDrawingParameters();
		if (dp == null) {
			dp = new DrawingParameters();
			placeLayer.setDrawingParameters(dp);
		}
		dp.lineColor = Color.getHSBColor((float) Math.random(), 1 - 0.2f * (float) Math.random(), 1 - 0.2f * (float) Math.random());
		dp.fillContours = false;

		DataLoader dataLoader = core.getDataLoader();
		dataLoader.addMapLayer(placeLayer, -1);
		ResultDescr rd = new ResultDescr();
		rd.product = placeLayer;
		rd.comment = "generalised places from the trajectories";
		aDescr.addResultDescr(rd);
		placeLayer.setMadeByAction(aDescr);

		DataTable placeTbl = placeLayer.constructTableWithStatistics();
		if (placeTbl != null) {
			placeTbl.setName(placeLayer.getName());
			int tblN = dataLoader.addTable(placeTbl);
			placeTbl.setEntitySetIdentifier(placeLayer.getEntitySetIdentifier());
			dataLoader.setLink(placeLayer, tblN);
			placeLayer.setLinkedToTable(true);
			rd = new ResultDescr();
			rd.product = placeTbl;
			rd.comment = "statistics about the generalised places";
			aDescr.addResultDescr(rd);
			placeTbl.setMadeByAction(aDescr);
		}
		return placeLayer;
	}

	protected DGeoLayer makeSimplePlaceLayer() {
		String name = "Places visited by trajectories from " + origTrajSetName;
/*
    name= Dialogs.askForStringValue(core.getUI().getMainFrame(),"Name of the layer?",name,
      "A map layer with the visited places will be generated","Visited places",false);
*/
		DataLoader dataLoader = core.getDataLoader();
		DGeoLayer placeLayer = new DGeoLayer();
		placeLayer.setGeographic(geo);
		placeLayer.setType(Geometry.area);
		placeLayer.setName(name);
		placeLayer.setGeoObjects(mAcc.sumPlaces, true);
		ResultDescr rd = new ResultDescr();
		rd.product = placeLayer;
		rd.comment = "places visited by the trajectories";
		aDescr.addResultDescr(rd);
		placeLayer.setMadeByAction(aDescr);

		DrawingParameters dp = placeLayer.getDrawingParameters();
		if (dp == null) {
			dp = new DrawingParameters();
			placeLayer.setDrawingParameters(dp);
		}
		dp.lineColor = Color.getHSBColor((float) Math.random(), 1 - 0.2f * (float) Math.random(), 1 - 0.2f * (float) Math.random());
		dp.fillContours = false;

		DataTable placeTbl = new DataTable();
		placeTbl.addAttribute("N visits", "n_visits", AttributeTypes.integer);
		int nVisIdx = placeTbl.getAttrCount() - 1;
		placeTbl.addAttribute("N starts", "n_starts", AttributeTypes.integer);
		int nStartsIdx = placeTbl.getAttrCount() - 1;
		placeTbl.addAttribute("N ends", "n_ends", AttributeTypes.integer);
		int nEndsIdx = placeTbl.getAttrCount() - 1;
		//information about the neighbouring places and links with them
		int maxNNeighbours = 0;
		for (int i = 0; i < mAcc.sumPlaces.size(); i++) {
			DPlaceVisitsCounter place = mAcc.sumPlaces.elementAt(i);
			if (place.links != null && place.links.size() > maxNNeighbours) {
				maxNNeighbours = place.links.size();
			}
		}
		int nNeiCN = -1, firstCN = -1;
		if (maxNNeighbours > 0 && maxNNeighbours <= 10) {
			nNeiCN = placeTbl.getAttrCount();
			placeTbl.addAttribute("N linked places", "n_links", AttributeTypes.integer);
			firstCN = nNeiCN + 1;
			for (int i = 0; i < maxNNeighbours; i++) {
				placeTbl.addAttribute("Linked place " + (i + 1), "link_" + (i + 1), AttributeTypes.character);
				placeTbl.addAttribute("N links to place " + (i + 1), "n_links_nei_" + (i + 1), AttributeTypes.integer);
			}
		}
		for (int i = 0; i < mAcc.sumPlaces.size(); i++) {
			DPlaceVisitsCounter place = mAcc.sumPlaces.elementAt(i);
			DataRecord rec = new DataRecord(place.getIdentifier());
			placeTbl.addDataRecord(rec);
			rec.setNumericAttrValue(place.nVisits, String.valueOf(place.nVisits), nVisIdx);
			rec.setNumericAttrValue(place.nStarts, String.valueOf(place.nStarts), nStartsIdx);
			rec.setNumericAttrValue(place.nEnds, String.valueOf(place.nEnds), nEndsIdx);
			if (nNeiCN >= 0) {
				int aIdx = firstCN;
				if (place.links != null && place.links.size() > 0) {
					rec.setNumericAttrValue(place.links.size(), String.valueOf(place.links.size()), nNeiCN);
					BubbleSort.sort(place.links);
					for (int j = 0; j < place.links.size(); j++) {
						rec.setAttrValue(place.links.elementAt(j).obj, aIdx++);
						rec.setNumericAttrValue(place.links.elementAt(j).count, aIdx++);
					}
					for (int j = place.links.size(); j < maxNNeighbours; j++) {
						rec.setAttrValue(null, aIdx++);
						rec.setNumericAttrValue(0, aIdx++);
					}
				} else {
					rec.setNumericAttrValue(0, "0", nNeiCN);
					for (int j = 0; j < maxNNeighbours; j++) {
						rec.setAttrValue(null, aIdx++);
						rec.setNumericAttrValue(0, aIdx++);
					}
				}
			}
			place.setThematicData(rec);
		}
		addVisitsByTimeIntervals(placeTbl, placeLayer);
		placeTbl.setName(placeLayer.getName());
		dataLoader.addMapLayer(placeLayer, -1);
		int tblN = dataLoader.addTable(placeTbl);
		placeTbl.setEntitySetIdentifier(placeLayer.getEntitySetIdentifier());
		dataLoader.setLink(placeLayer, tblN);
		placeLayer.setLinkedToTable(true);
		rd = new ResultDescr();
		rd.product = placeTbl;
		rd.comment = "statistics about the visited places";
		aDescr.addResultDescr(rd);
		placeTbl.setMadeByAction(aDescr);
		return placeLayer;
	}

	protected Parameter getTimeParameter() {
		if (mAcc == null || mAcc.timeBreaks == null || mAcc.timeBreaks.size() < 1)
			return null;
		Parameter par = new Parameter();
		if (timeParam != null) {
			par.setName(timeParam.getName());
			for (int i = 0; i < timeParam.getValueCount(); i++) {
				par.addValue(timeParam.getValue(i));
			}
			return par;
		}
		String parName = (mAcc.useCycle) ? mAcc.cycleName : "Time interval (start)";
		if (mAcc.useCycle) {
			for (int i = 0; i < mAcc.timeBreaks.size(); i++) {
				par.addValue(mAcc.timeBreaks.elementAt(i));
			}
		} else {
			boolean dates = mAcc.timeBreaks.elementAt(0) instanceof Date;
			char datePrecision = 0;
			String dateScheme = null;
			if (dates) {
				String expl[] = { "(in the parameter values)", "Time interval: from " + mAcc.tStart + " to " + mAcc.tEnd };
				datePrecision = TimeDialogs.askDesiredDatePrecision(expl, (Date) mAcc.tStart);
				dateScheme = ((Date) mAcc.timeBreaks.elementAt(0)).scheme;
				if (datePrecision != mAcc.timeBreaks.elementAt(0).getPrecision()) {
					dateScheme = Date.removeExtraElementsFromScheme(dateScheme, datePrecision);
				}
				EnterDateSchemeUI enterSch = new EnterDateSchemeUI("Edit, if desired, the date/time scheme (template) for the parameter values. " + " The chosen precision is " + Date.getTextForTimeSymbol(datePrecision) + ". " + expl[0],
						"Date scheme:", dateScheme);
				OKDialog dia = new OKDialog(core.getUI().getMainFrame(), "Date/time scheme", false);
				dia.addContent(enterSch);
				dia.show();
				dateScheme = enterSch.getScheme();
			}
			par.addValue(mAcc.tStart.getCopy());
			for (int i = 0; i < mAcc.timeBreaks.size(); i++) {
				par.addValue(mAcc.timeBreaks.elementAt(i));
			}
			if (dates) {
				for (int i = 0; i < par.getValueCount(); i++) {
					((Date) par.getValue(i)).setPrecision(datePrecision);
					((Date) par.getValue(i)).setDateScheme(dateScheme);
				}
			}
		}
		par.setName(parName);
		timeParam = par;
		return par;
	}

	protected void addMovesByTimeIntervals(DataTable movesTbl, DGeoLayer movesLayer) {
		if (mAcc.timeBreaks == null || mAcc.trajByTime == null)
			return;
		Parameter par = getTimeParameter();
		String itemsName1 = "trajectories";
		String aName1 = "N " + itemsName1 + " by " + ((mAcc.useCycle) ? par.getName() : "time intervals");
		String aNameTotal1 = "N " + itemsName1 + " total";
		String str = aName1 + " (";
		for (int k = 2; movesTbl.findAttrByName(aName1) >= 0; k++) {
			aName1 = str + k + ")";
		}
		str = aNameTotal1 + " (";
		for (int k = 2; movesTbl.findAttrByName(aNameTotal1) >= 0; k++) {
			aNameTotal1 = str + k + ")";
		}
		String itemsName2 = "moves";
		String aName2 = "N " + itemsName2 + " by " + ((mAcc.useCycle) ? par.getName() : "time intervals");
		String aNameTotal2 = "N " + itemsName2 + " total";
		str = aName2 + " (";
		for (int k = 2; movesTbl.findAttrByName(aName2) >= 0; k++) {
			aName2 = str + k + ")";
		}
		str = aNameTotal2 + " (";
		for (int k = 2; movesTbl.findAttrByName(aNameTotal2) >= 0; k++) {
			aNameTotal2 = str + k + ")";
		}
		Panel p = new Panel(new ColumnLayout());
		p.add(new Label("Two parameter-dependent attributes will be produced for the aggregate moves.", Label.CENTER));
		p.add(new Label("Edit the names of the attributes and the parameter if needed.", Label.CENTER));
		p.add(new Label("Names of the parameter-dependent attributes:"));
		TextField atf1 = new TextField(aName1), atf2 = new TextField(aName2);
		p.add(atf1);
		p.add(atf2);
		p.add(new Label("Parameter name:"));
		TextField ptf = new TextField(par.getName());
		p.add(ptf);
		p.add(new Label("Parameter values from " + par.getFirstValue() + " to " + par.getLastValue()));
		p.add(new Label("Attribute names for \"totals\":"));
		TextField aTottf1 = new TextField(aNameTotal1), aTottf2 = new TextField(aNameTotal2);
		p.add(aTottf1);
		p.add(aTottf2);
		OKDialog dia = new OKDialog(core.getUI().getMainFrame(), "New attributes", false);
		dia.addContent(p);
		dia.show();
		str = atf1.getText();
		if (str != null && str.trim().length() > 0) {
			aName1 = str.trim();
		}
		str = atf2.getText();
		if (str != null && str.trim().length() > 0) {
			aName2 = str.trim();
		}
		str = aTottf1.getText();
		if (str != null && str.trim().length() > 0) {
			aNameTotal1 = str.trim();
		}
		str = aTottf2.getText();
		if (str != null && str.trim().length() > 0) {
			aNameTotal2 = str.trim();
		}
		str = ptf.getText();
		if (str != null && str.trim().length() > 0) {
			par.setName(str.trim());
		}
		String parName = par.getName();
		for (int k = 1; movesTbl.getParameter(par.getName()) != null; k++) {
			par.setName(parName + k);
		}
		par.setTemporal(true);
		movesTbl.addParameter(par);

		//add columns to the table
		Attribute attrParentNum = new Attribute(IdMaker.makeId(movesTbl.getAttrCount() + " " + aName1, movesTbl), AttributeTypes.integer);
		attrParentNum.setName(aName1);
		int aIdx1 = movesTbl.getAttrCount();
		for (int i = 0; i < par.getValueCount(); i++) {
			String name = par.getValue(i).toString();
			Attribute attrChild = new Attribute(attrParentNum.getIdentifier() + "_" + name, attrParentNum.getType());
			attrChild.addParamValPair(par.getName(), par.getValue(i));
			attrParentNum.addChild(attrChild);
			movesTbl.addAttribute(attrChild);
		}
		Attribute attrTotal = new Attribute(IdMaker.makeId(movesTbl.getAttrCount() + " " + aNameTotal1, movesTbl), AttributeTypes.integer);
		attrTotal.setName(aNameTotal1);
		int totIdx1 = movesTbl.getAttrCount();
		movesTbl.addAttribute(attrTotal);

		attrParentNum = new Attribute(IdMaker.makeId(movesTbl.getAttrCount() + " " + aName2, movesTbl), AttributeTypes.integer);
		attrParentNum.setName(aName2);
		int aIdx2 = movesTbl.getAttrCount();
		for (int i = 0; i < par.getValueCount(); i++) {
			String name = par.getValue(i).toString();
			Attribute attrChild = new Attribute(attrParentNum.getIdentifier() + "_" + name, attrParentNum.getType());
			attrChild.addParamValPair(par.getName(), par.getValue(i));
			attrParentNum.addChild(attrChild);
			movesTbl.addAttribute(attrChild);
		}
		attrTotal = new Attribute(IdMaker.makeId(movesTbl.getAttrCount() + " " + aNameTotal2, movesTbl), AttributeTypes.integer);
		attrTotal.setName(aNameTotal2);
		int totIdx2 = movesTbl.getAttrCount();
		movesTbl.addAttribute(attrTotal);
		movesTbl.makeUniqueAttrIdentifiers();

		for (int i = 0; i < movesLayer.getObjectCount(); i++) {
			DGeoObject gobj = movesLayer.getObject(i);
			int tr[] = mAcc.trajByTime.elementAt(i), mv[] = mAcc.movesByTime.elementAt(i);
			DataRecord rec = (DataRecord) gobj.getData();
			for (int j = 0; j < tr.length; j++) {
				rec.setNumericAttrValue(tr[j], String.valueOf(tr[j]), aIdx1 + j);
				rec.setNumericAttrValue(mv[j], String.valueOf(mv[j]), aIdx2 + j);
			}
			rec.setNumericAttrValue(mAcc.totalTraj.elementAt(i), String.valueOf(mAcc.totalTraj.elementAt(i)), totIdx1);
			rec.setNumericAttrValue(mAcc.totalMoves.elementAt(i), String.valueOf(mAcc.totalMoves.elementAt(i)), totIdx2);
		}
	}

	protected void addVisitsByTimeIntervals(DataTable placeTbl, DGeoLayer placeLayer) {
		if (mAcc.timeBreaks == null || mAcc.visitors == null)
			return;
		Parameter par = getTimeParameter();
		String itemsName = "visitors";
		String aName = "N " + itemsName + " by " + ((mAcc.useCycle) ? par.getName() : "time intervals");
		String aNameTotal = "N " + itemsName + " total";
		String str = aName + " (";
		for (int k = 2; placeTbl.findAttrByName(aName) >= 0; k++) {
			aName = str + k + ")";
		}
		str = aNameTotal + " (";
		for (int k = 2; placeTbl.findAttrByName(aNameTotal) >= 0; k++) {
			aNameTotal = str + k + ")";
		}
		Panel p = new Panel(new ColumnLayout());
		p.add(new Label("A parameter-dependent attribute will be produced for the areas.", Label.CENTER));
		p.add(new Label("Edit the names of the attributes and the parameter if needed.", Label.CENTER));
		p.add(new Label("Name of the parameter-dependent attribute:"));
		TextField atf = new TextField(aName);
		p.add(atf);
		p.add(new Label("Parameter name:"));
		TextField ptf = new TextField(par.getName());
		p.add(ptf);
		p.add(new Label("Parameter values from " + par.getFirstValue() + " to " + par.getLastValue()));
		p.add(new Label("Attribute name for \"total\":"));
		TextField aTottf = new TextField(aNameTotal);
		p.add(aTottf);
		OKDialog dia = new OKDialog(core.getUI().getMainFrame(), "New attributes", false);
		dia.addContent(p);
		dia.show();
		str = atf.getText();
		if (str != null && str.trim().length() > 0) {
			aName = str.trim();
		}
		str = aTottf.getText();
		if (str != null && str.trim().length() > 0) {
			aNameTotal = str.trim();
		}
		str = ptf.getText();
		if (str != null && str.trim().length() > 0) {
			par.setName(str.trim());
		}
		String parName = par.getName();
		for (int k = 1; placeTbl.getParameter(par.getName()) != null; k++) {
			par.setName(parName + k);
		}
		par.setTemporal(true);
		placeTbl.addParameter(par);

		//add columns to the table
		Attribute attrParentNum = new Attribute(IdMaker.makeId(placeTbl.getAttrCount() + " " + aName, placeTbl), AttributeTypes.integer);
		attrParentNum.setName(aName);
		int aIdx0 = placeTbl.getAttrCount();
		for (int i = 0; i < par.getValueCount(); i++) {
			String name = par.getValue(i).toString();
			Attribute attrChild = new Attribute(attrParentNum.getIdentifier() + "_" + name, attrParentNum.getType());
			attrChild.addParamValPair(par.getName(), par.getValue(i));
			attrParentNum.addChild(attrChild);
			placeTbl.addAttribute(attrChild);
		}
		Attribute attrTotal = new Attribute(IdMaker.makeId(placeTbl.getAttrCount() + " " + aNameTotal, placeTbl), AttributeTypes.integer);
		attrTotal.setName(aNameTotal);
		int totIdx = placeTbl.getAttrCount();
		placeTbl.addAttribute(attrTotal);
		for (int i = 0; i < placeLayer.getObjectCount(); i++) {
			DGeoObject gobj = placeLayer.getObject(i);
			int na = mAcc.findPlaceById(gobj.getIdentifier());
			if (na < 0) {
				continue;
			}
			DataRecord rec = (DataRecord) gobj.getData();
			for (int j = 0; j < mAcc.visitors[na].length; j++) {
				rec.setNumericAttrValue(mAcc.visitors[na][j], String.valueOf(mAcc.visitors[na][j]), aIdx0 + j);
			}
			rec.setNumericAttrValue(mAcc.totalVisitors[na], String.valueOf(mAcc.totalVisitors[na]), totIdx);
		}
		placeTbl.makeUniqueAttrIdentifiers();
		if (mAcc.neiVisitors != null) {
			String aN = aName, aNT = aNameTotal;
			aName += ", including neighbourhood";
			aNameTotal += ", including neighbourhood";
			p = new Panel(new ColumnLayout());
			p.add(new Label("Additional attributes will be produced.", Label.CENTER));
			p.add(new Label("Edit the names of the attributes if needed.", Label.CENTER));
			p.add(new Label("Name of the parameter-dependent attribute:"));
			atf = new TextField(aName);
			p.add(atf);
			p.add(new Label("Attribute name for \"total\":"));
			aTottf = new TextField(aNameTotal);
			p.add(aTottf);
			dia = new OKDialog(core.getUI().getMainFrame(), "Additional attributes", true);
			dia.addContent(p);
			dia.show();
			if (!dia.wasCancelled()) {
				str = atf.getText();
				if (str != null && str.trim().length() > 0) {
					aName = str.trim();
				}
				str = aTottf.getText();
				if (str != null && str.trim().length() > 0) {
					aNameTotal = str.trim();
				}

				attrParentNum = new Attribute(IdMaker.makeId(placeTbl.getAttrCount() + " " + aName, placeTbl) + "_nei", AttributeTypes.integer);
				attrParentNum.setName(aName);
				int aIdx1 = placeTbl.getAttrCount();
				for (int i = 0; i < par.getValueCount(); i++) {
					String name = par.getValue(i).toString();
					Attribute attrChild = new Attribute(IdMaker.makeId(attrParentNum.getIdentifier() + "_" + name, placeTbl), attrParentNum.getType());
					attrChild.addParamValPair(par.getName(), par.getValue(i));
					attrParentNum.addChild(attrChild);
					placeTbl.addAttribute(attrChild);
				}
				attrTotal = new Attribute(IdMaker.makeId(placeTbl.getAttrCount() + " " + aNameTotal, placeTbl), AttributeTypes.integer);
				attrTotal.setName(aNameTotal);
				totIdx = placeTbl.getAttrCount();
				placeTbl.addAttribute(attrTotal);
				for (int i = 0; i < placeLayer.getObjectCount(); i++) {
					DGeoObject gobj = placeLayer.getObject(i);
					int na = mAcc.findPlaceById(gobj.getIdentifier());
					if (na < 0) {
						continue;
					}
					DataRecord rec = (DataRecord) gobj.getData();
					for (int j = 0; j < mAcc.neiVisitors[na].length; j++) {
						rec.setNumericAttrValue(mAcc.neiVisitors[na][j], String.valueOf(mAcc.neiVisitors[na][j]), aIdx1 + j);
					}
					rec.setNumericAttrValue(mAcc.neiTotalVisitors[na], String.valueOf(mAcc.neiTotalVisitors[na]), totIdx);
				}
				placeTbl.makeUniqueAttrIdentifiers();
			}
			aName = "Average " + aN + ", including neighbourhood";
			aNameTotal = "Average " + aNT + ", including neighbourhood";
			atf.setText(aName);
			aTottf.setText(aNameTotal);
			dia = new OKDialog(core.getUI().getMainFrame(), "Additional attributes", true);
			dia.addContent(p);
			dia.show();
			if (!dia.wasCancelled()) {
				str = atf.getText();
				if (str != null && str.trim().length() > 0) {
					aName = str.trim();
				}
				str = aTottf.getText();
				if (str != null && str.trim().length() > 0) {
					aNameTotal = str.trim();
				}

				attrParentNum = new Attribute(IdMaker.makeId(placeTbl.getAttrCount() + " " + aName, placeTbl), AttributeTypes.real);
				attrParentNum.setName(aName);
				int aIdx1 = placeTbl.getAttrCount();
				for (int i = 0; i < par.getValueCount(); i++) {
					String name = par.getValue(i).toString();
					Attribute attrChild = new Attribute(IdMaker.makeId(attrParentNum.getIdentifier() + "_" + name, placeTbl), attrParentNum.getType());
					attrChild.addParamValPair(par.getName(), par.getValue(i));
					attrParentNum.addChild(attrChild);
					placeTbl.addAttribute(attrChild);
				}
				attrTotal = new Attribute(IdMaker.makeId(placeTbl.getAttrCount() + " " + aNameTotal, placeTbl), AttributeTypes.real);
				attrTotal.setName(aNameTotal);
				totIdx = placeTbl.getAttrCount();
				placeTbl.addAttribute(attrTotal);
				for (int i = 0; i < placeLayer.getObjectCount(); i++) {
					DGeoObject gobj = placeLayer.getObject(i);
					int na = mAcc.findPlaceById(gobj.getIdentifier());
					if (na < 0) {
						continue;
					}
					int nNb = 1;
					if (gobj.neighbours != null) {
						for (int j = 0; j < gobj.neighbours.size(); j++)
							if (mAcc.findPlaceById(gobj.neighbours.elementAt(j)) >= 0) {
								++nNb;
							}
					}
					DataRecord rec = (DataRecord) gobj.getData();
					for (int j = 0; j < mAcc.visitors[na].length; j++) {
						float val = 1.0f * mAcc.neiVisitors[na][j] / nNb;
						rec.setNumericAttrValue(val, StringUtil.floatToStr(val, 2), aIdx1 + j);
					}
					float val = 1.0f * mAcc.neiTotalVisitors[na] / nNb;
					rec.setNumericAttrValue(val, StringUtil.floatToStr(val, 2), totIdx);
				}
				placeTbl.makeUniqueAttrIdentifiers();
			}
		}
		itemsName = "visits";
		aName = "N " + itemsName + " by " + ((mAcc.useCycle) ? parName : "time intervals");
		aNameTotal = "N " + itemsName + " total";
		str = aName + " (";
		for (int k = 2; placeTbl.findAttrByName(aName) >= 0; k++) {
			aName = str + k + ")";
		}
		str = aNameTotal + " (";
		for (int k = 2; placeTbl.findAttrByName(aNameTotal) >= 0; k++) {
			aNameTotal = str + k + ")";
		}
		p = new Panel(new ColumnLayout());
		p.add(new Label("Additional attributes will be produced.", Label.CENTER));
		p.add(new Label("Edit the names of the attributes if needed.", Label.CENTER));
		p.add(new Label("Name of the parameter-dependent attribute:"));
		atf = new TextField(aName);
		p.add(atf);
		p.add(new Label("Attribute name for \"total\":"));
		aTottf = new TextField(aNameTotal);
		p.add(aTottf);
		dia = new OKDialog(core.getUI().getMainFrame(), "Additional attributes", true);
		dia.addContent(p);
		dia.show();
		str = atf.getText();
		if (str != null && str.trim().length() > 0) {
			aName = str.trim();
		}
		str = aTottf.getText();
		if (str != null && str.trim().length() > 0) {
			aNameTotal = str.trim();
		}

		attrParentNum = new Attribute(IdMaker.makeId(placeTbl.getAttrCount() + " " + aName, placeTbl), AttributeTypes.integer);
		attrParentNum.setName(aName);
		aIdx0 = placeTbl.getAttrCount();
		for (int i = 0; i < par.getValueCount(); i++) {
			String name = par.getValue(i).toString();
			Attribute attrChild = new Attribute(IdMaker.makeId(attrParentNum.getIdentifier() + "_" + name, placeTbl), attrParentNum.getType());
			attrChild.addParamValPair(par.getName(), par.getValue(i));
			attrParentNum.addChild(attrChild);
			placeTbl.addAttribute(attrChild);
		}
		attrTotal = new Attribute(IdMaker.makeId(placeTbl.getAttrCount() + " " + aNameTotal, placeTbl), AttributeTypes.integer);
		attrTotal.setName(aNameTotal);
		totIdx = placeTbl.getAttrCount();
		placeTbl.addAttribute(attrTotal);
		for (int i = 0; i < placeLayer.getObjectCount(); i++) {
			DGeoObject gobj = placeLayer.getObject(i);
			int na = mAcc.findPlaceById(gobj.getIdentifier());
			if (na < 0) {
				continue;
			}
			DataRecord rec = (DataRecord) gobj.getData();
			for (int j = 0; j < mAcc.visits[na].length; j++) {
				rec.setNumericAttrValue(mAcc.visits[na][j], String.valueOf(mAcc.visits[na][j]), aIdx0 + j);
			}
			rec.setNumericAttrValue(mAcc.totalVisits[na], String.valueOf(mAcc.totalVisits[na]), totIdx);
		}
		placeTbl.makeUniqueAttrIdentifiers();
		if (mAcc.neiVisits != null) {
			String aN = aName, aNT = aNameTotal;
			aName += ", including neighbourhood";
			aNameTotal += ", including neighbourhood";
			atf.setText(aName);
			aTottf.setText(aNameTotal);
			dia = new OKDialog(core.getUI().getMainFrame(), "Additional attributes", true);
			dia.addContent(p);
			dia.show();
			if (!dia.wasCancelled()) {
				str = atf.getText();
				if (str != null && str.trim().length() > 0) {
					aName = str.trim();
				}
				str = aTottf.getText();
				if (str != null && str.trim().length() > 0) {
					aNameTotal = str.trim();
				}

				attrParentNum = new Attribute(IdMaker.makeId(placeTbl.getAttrCount() + " " + aName, placeTbl) + "_nei", AttributeTypes.integer);
				attrParentNum.setName(aName);
				int aIdx1 = placeTbl.getAttrCount();
				for (int i = 0; i < par.getValueCount(); i++) {
					String name = par.getValue(i).toString();
					Attribute attrChild = new Attribute(IdMaker.makeId(attrParentNum.getIdentifier() + "_" + name, placeTbl), attrParentNum.getType());
					attrChild.addParamValPair(par.getName(), par.getValue(i));
					attrParentNum.addChild(attrChild);
					placeTbl.addAttribute(attrChild);
				}
				attrTotal = new Attribute(IdMaker.makeId(placeTbl.getAttrCount() + " " + aNameTotal, placeTbl), AttributeTypes.integer);
				attrTotal.setName(aNameTotal);
				totIdx = placeTbl.getAttrCount();
				placeTbl.addAttribute(attrTotal);
				for (int i = 0; i < placeLayer.getObjectCount(); i++) {
					DGeoObject gobj = placeLayer.getObject(i);
					int na = mAcc.findPlaceById(gobj.getIdentifier());
					if (na < 0) {
						continue;
					}
					DataRecord rec = (DataRecord) gobj.getData();
					for (int j = 0; j < mAcc.neiVisits[na].length; j++) {
						rec.setNumericAttrValue(mAcc.neiVisits[na][j], String.valueOf(mAcc.neiVisits[na][j]), aIdx1 + j);
					}
					rec.setNumericAttrValue(mAcc.neiTotalVisits[na], String.valueOf(mAcc.neiTotalVisits[na]), totIdx);
				}
				//placeTbl.finishedDataLoading();
				placeTbl.makeUniqueAttrIdentifiers();
			}
			aName = "Average " + aN + ", including neighbourhood";
			aNameTotal = "Average " + aNT + ", including neighbourhood";
			atf.setText(aName);
			aTottf.setText(aNameTotal);
			dia = new OKDialog(core.getUI().getMainFrame(), "Additional attributes", true);
			dia.addContent(p);
			dia.show();
			if (!dia.wasCancelled()) {
				str = atf.getText();
				if (str != null && str.trim().length() > 0) {
					aName = str.trim();
				}
				str = aTottf.getText();
				if (str != null && str.trim().length() > 0) {
					aNameTotal = str.trim();
				}

				attrParentNum = new Attribute(IdMaker.makeId(placeTbl.getAttrCount() + " " + aName, placeTbl), AttributeTypes.real);
				attrParentNum.setName(aName);
				int aIdx1 = placeTbl.getAttrCount();
				for (int i = 0; i < par.getValueCount(); i++) {
					String name = par.getValue(i).toString();
					Attribute attrChild = new Attribute(IdMaker.makeId(attrParentNum.getIdentifier() + "_" + name, placeTbl), attrParentNum.getType());
					attrChild.addParamValPair(par.getName(), par.getValue(i));
					attrParentNum.addChild(attrChild);
					placeTbl.addAttribute(attrChild);
				}
				attrTotal = new Attribute(IdMaker.makeId(placeTbl.getAttrCount() + " " + aNameTotal, placeTbl), AttributeTypes.real);
				attrTotal.setName(aNameTotal);
				totIdx = placeTbl.getAttrCount();
				placeTbl.addAttribute(attrTotal);
				for (int i = 0; i < placeLayer.getObjectCount(); i++) {
					DGeoObject gobj = placeLayer.getObject(i);
					int na = mAcc.findPlaceById(gobj.getIdentifier());
					if (na < 0) {
						continue;
					}
					int nNb = 1;
					if (gobj.neighbours != null) {
						for (int j = 0; j < gobj.neighbours.size(); j++)
							if (mAcc.findPlaceById(gobj.neighbours.elementAt(j)) >= 0) {
								++nNb;
							}
					}
					DataRecord rec = (DataRecord) gobj.getData();
					for (int j = 0; j < mAcc.visits[na].length; j++) {
						float val = 1.0f * mAcc.neiVisits[na][j] / nNb;
						rec.setNumericAttrValue(val, StringUtil.floatToStr(val, 2), aIdx1 + j);
					}
					float val = 1.0f * mAcc.neiTotalVisits[na] / nNb;
					rec.setNumericAttrValue(val, StringUtil.floatToStr(val, 2), totIdx);
				}
				placeTbl.makeUniqueAttrIdentifiers();
			}
		}
	}

	protected DLinkLayer makeSimpleLinkLayer(DGeoLayer placeLayer) {
		if (mAcc == null || mAcc.sumMoves == null || mAcc.sumMoves.size() < 1)
			return null;
		String name = "Aggregated moves from " + origTrajSetName;
/*
    name= Dialogs.askForStringValue(core.getUI().getMainFrame(),"Name of the layer?",name,
      "A map layer with the aggregate moves will be generated","Aggregate moves",false);
*/
		DataTable sumTbl = new DataTable();
		sumTbl.setName(name);
		sumTbl.addAttribute("Start ID", "startId", AttributeTypes.character);
		int startIdIdx = sumTbl.getAttrCount() - 1;
		sumTbl.addAttribute("End ID", "endId", AttributeTypes.character);
		int endIdIdx = sumTbl.getAttrCount() - 1;
		sumTbl.addAttribute("N of moves", "n_moves", AttributeTypes.integer);
		int nMovesIdx = sumTbl.getAttrCount() - 1;
		sumTbl.addAttribute("Direction", "direction", AttributeTypes.character);
		int dirIdx = sumTbl.getAttrCount() - 1;
		sumTbl.addAttribute("Length", "length", AttributeTypes.real);
		int lenIdx = sumTbl.getAttrCount() - 1;
		DataSourceSpec spec = new DataSourceSpec();
		spec.id = sumTbl.getContainerIdentifier();
		spec.name = sumTbl.getName();
		spec.toBuildMapLayer = true;
		spec.descriptors = new Vector(5, 5);
		LinkDataDescription aggldd = new LinkDataDescription();
		aggldd.layerRef = placeLayer.getContainerIdentifier();
		aggldd.souColIdx = startIdIdx;
		aggldd.destColIdx = endIdIdx;
		spec.descriptors.addElement(aggldd);
		sumTbl.setDataSource(spec);

		for (int i = 0; i < mAcc.sumMoves.size(); i++) {
			DLinkObject lobj = mAcc.sumMoves.elementAt(i);
			lobj.setGeographic(geo);
			DataRecord rec = new DataRecord(lobj.getIdentifier());
			sumTbl.addDataRecord(rec);
			rec.setAttrValue(lobj.getStartNode().getIdentifier(), startIdIdx);
			rec.setAttrValue(lobj.getEndNode().getIdentifier(), endIdIdx);
			rec.setAttrValue(lobj.getLinkDirection(), dirIdx);
			int nLinks = lobj.getNTimes();
			rec.setNumericAttrValue(nLinks, String.valueOf(nLinks), nMovesIdx);
			double length = lobj.getLength();
			rec.setNumericAttrValue(length, String.valueOf(length), lenIdx);
			lobj.setThematicData(rec);
		}

		DataLoader dataLoader = core.getDataLoader();
		int aggTblN = dataLoader.addTable(sumTbl);
		DLinkLayer sumMovesLayer = new DLinkLayer();
		sumMovesLayer.setType(Geometry.line);
		sumMovesLayer.setName(name);
		sumMovesLayer.setGeographic(geo);
		sumMovesLayer.setGeoObjects(mAcc.sumMoves, true);
		sumMovesLayer.setHasMovingObjects(false);
		sumMovesLayer.setPlaceLayer(placeLayer);
		sumMovesLayer.setDataSource(spec);
		DrawingParameters dp1 = sumMovesLayer.getDrawingParameters();
		if (dp1 == null) {
			dp1 = new DrawingParameters();
			sumMovesLayer.setDrawingParameters(dp1);
		}
		dp1.lineColor = Color.getHSBColor((float) Math.random(), 1 - 0.2f * (float) Math.random(), 1 - 0.2f * (float) Math.random());
		dp1.transparency = 40;
		spec.drawParm = dp1;
		dataLoader.addMapLayer(sumMovesLayer, -1);
		sumTbl.setEntitySetIdentifier(sumMovesLayer.getEntitySetIdentifier());
		dataLoader.setLink(sumMovesLayer, aggTblN);
		sumMovesLayer.setLinkedToTable(true);
		addMovesByTimeIntervals(sumTbl, sumMovesLayer);

		ShowRecManager recMan = null;
		if (dataLoader instanceof DataManager) {
			recMan = ((DataManager) dataLoader).getShowRecManager(aggTblN);
		}
		if (recMan != null) {
			Vector showAttr = new Vector(sumTbl.getAttrCount(), 10);
			for (int i = 0; i < sumTbl.getAttrCount() - 1; i++) {
				showAttr.addElement(sumTbl.getAttributeId(i));
			}
			recMan.setPopupAddAttrs(showAttr);
		}
		ResultDescr rd = new ResultDescr();
		rd.product = sumMovesLayer;
		rd.comment = "summarised moves from the trajectories";
		aDescr.addResultDescr(rd);
		sumMovesLayer.setMadeByAction(aDescr);
		rd = new ResultDescr();
		rd.product = sumTbl;
		rd.comment = "statistics about the summarised moves";
		aDescr.addResultDescr(rd);
		sumTbl.setMadeByAction(aDescr);

		DisplayProducer displayProducer = core.getDisplayProducer();
		if (displayProducer != null) {
			DataMapper dataMapper = null;
			if (displayProducer.getDataMapper() != null && (displayProducer.getDataMapper() instanceof DataMapper)) {
				dataMapper = (DataMapper) displayProducer.getDataMapper();
			}
			MapViewer mapView = core.getUI().getCurrentMapViewer();
			if (dataMapper != null && mapView != null) {
				Vector attr = new Vector(1, 1);
				attr.addElement(sumTbl.getAttributeId(nMovesIdx));
				Object vis = dataMapper.constructVisualizer("line_thickness", Geometry.line);
				Visualizer visualizer = displayProducer.displayOnMap(vis, "line_thickness", sumTbl, attr, sumMovesLayer, mapView);
				core.getSupervisor().registerTool(visualizer);
			}
		}
		return sumMovesLayer;
	}

	protected DAggregateLinkLayer makeAggregateLinkLayer(DGeoLayer placeLayer, DGeoLayer moveLayer) {
		if (mAcc == null || mAcc.aggLinks == null || mAcc.aggLinks.size() < 1)
			return null;
		String name = "Aggregated moves from " + origTrajSetName;
/*
    name= Dialogs.askForStringValue(core.getUI().getMainFrame(),"Name of the layer?",name,
      "A map layer with the aggregate moves will be generated","Aggregate moves",false);
*/
		DataTable aggTbl = new DataTable();
		aggTbl.setName(name);
		aggTbl.addAttribute("Start ID", "startId", AttributeTypes.character);
		int startIdIdx = aggTbl.getAttrCount() - 1;
		aggTbl.addAttribute("End ID", "endId", AttributeTypes.character);
		int endIdIdx = aggTbl.getAttrCount() - 1;
		aggTbl.addAttribute("Direction", "direction", AttributeTypes.character);
		int dirIdx = aggTbl.getAttrCount() - 1;
		aggTbl.addAttribute("Length", "length", AttributeTypes.real);
		int lenIdx = aggTbl.getAttrCount() - 1;
		aggTbl.addAttribute("N of moves", "n_moves", AttributeTypes.integer);
		int nMovesIdx = aggTbl.getAttrCount() - 1;
		aggTbl.addAttribute("Min move duration", "min_dur", AttributeTypes.integer);
		int minDurIdx = aggTbl.getAttrCount() - 1;
		aggTbl.addAttribute("Max move duration", "max_dur", AttributeTypes.integer);
		int maxDurIdx = aggTbl.getAttrCount() - 1;
		aggTbl.addAttribute("N of different trajectories", "n_traj", AttributeTypes.integer);
		int nTrajIdx = aggTbl.getAttrCount() - 1;
		aggTbl.addAttribute("IDs of trajectories", "trIds", AttributeTypes.character);
		int trIdsIdx = aggTbl.getAttrCount() - 1;
		DataSourceSpec spec = new DataSourceSpec();
		spec.id = aggTbl.getContainerIdentifier();
		spec.name = aggTbl.getName();
		spec.toBuildMapLayer = true;
		spec.descriptors = new Vector(5, 5);
		LinkDataDescription aggldd = new LinkDataDescription();
		aggldd.layerRef = placeLayer.getContainerIdentifier();
		aggldd.souColIdx = startIdIdx;
		aggldd.destColIdx = endIdIdx;
		spec.descriptors.addElement(aggldd);
		aggTbl.setDataSource(spec);
		for (int i = 0; i < mAcc.aggLinks.size(); i++) {
			DAggregateLinkObject lobj = mAcc.aggLinks.elementAt(i);
			lobj.setGeographic(geo);
			DataRecord rec = new DataRecord(lobj.getIdentifier());
			aggTbl.addDataRecord(rec);
			rec.setAttrValue(lobj.startNode.getIdentifier(), startIdIdx);
			rec.setAttrValue(lobj.endNode.getIdentifier(), endIdIdx);
			rec.setAttrValue(lobj.getLinkDirection(), dirIdx);
			double length = lobj.getLength();
			rec.setNumericAttrValue(length, String.valueOf(length), lenIdx);
			int nLinks = lobj.souLinks.size();
			rec.setNumericAttrValue(nLinks, String.valueOf(nLinks), nMovesIdx);
			Vector trIds = new Vector(nLinks, 1);
			String trIdsStr = "";
			long minDur = 0, maxDur = 0;
			for (int j = 0; j < nLinks; j++) {
				DLinkObject link = (DLinkObject) lobj.souLinks.elementAt(j);
				TimeReference tref = link.getTimeReference();
				if (tref != null && tref.getValidFrom() != null && tref.getValidUntil() != null) {
					long dur = tref.getValidUntil().subtract(tref.getValidFrom());
					if (dur > 0) {
						if (maxDur == 0) {
							minDur = dur;
							maxDur = dur;
						} else if (maxDur < dur) {
							maxDur = dur;
						} else if (minDur > dur) {
							minDur = dur;
						}
					}
				}
				String trId = (String) lobj.souTrajIds.elementAt(j);
				if (!trIds.contains(trId)) {
					trIds.addElement(trId);
					if (trIds.size() > 1) {
						trIdsStr += ";";
					}
					trIdsStr += trId;
				}
			}
			rec.setNumericAttrValue(minDur, String.valueOf(minDur), minDurIdx);
			rec.setNumericAttrValue(maxDur, String.valueOf(maxDur), maxDurIdx);
			rec.setNumericAttrValue(trIds.size(), String.valueOf(trIds.size()), nTrajIdx);
			rec.setAttrValue(trIdsStr, trIdsIdx);
			lobj.setThematicData(rec);
		}

		DataLoader dataLoader = core.getDataLoader();
		int aggTblN = dataLoader.addTable(aggTbl);

		DAggregateLinkLayer aggLinkLayer = new DAggregateLinkLayer();
		aggLinkLayer.onlyActiveTrajectories = false;
		aggLinkLayer.onlyStartsEnds = mAcc.useOnlyStartsEnds;
		aggLinkLayer.findIntersections = mAcc.findIntersections;
		aggLinkLayer.setType(Geometry.line);
		aggLinkLayer.setName(aggTbl.getName());
		aggLinkLayer.setGeographic(geo);
		aggLinkLayer.setGeoObjects(mAcc.aggLinks, true);
		aggLinkLayer.setHasMovingObjects(true);
		aggLinkLayer.setTrajectoryLayer(moveLayer);
		aggLinkLayer.setPlaceLayer(placeLayer);
		aggLinkLayer.setDataSource(spec);
		DrawingParameters dp1 = aggLinkLayer.getDrawingParameters();
		if (dp1 == null) {
			dp1 = new DrawingParameters();
			aggLinkLayer.setDrawingParameters(dp1);
		}
		dp1.lineColor = Color.getHSBColor((float) Math.random(), 1 - 0.2f * (float) Math.random(), 1 - 0.2f * (float) Math.random());
		dp1.transparency = 40;
		spec.drawParm = dp1;
		dataLoader.addMapLayer(aggLinkLayer, -1);
		aggTbl.setEntitySetIdentifier(aggLinkLayer.getEntitySetIdentifier());
		dataLoader.setLink(aggLinkLayer, aggTblN);
		aggLinkLayer.setLinkedToTable(true);
		aggLinkLayer.countActiveLinks();

		ResultDescr rd = new ResultDescr();
		rd.product = aggLinkLayer;
		rd.comment = "aggregated moves constructed from the trajectories";
		aDescr.addResultDescr(rd);
		aggLinkLayer.setMadeByAction(aDescr);
		rd = new ResultDescr();
		rd.product = aggTbl;
		rd.comment = "statistics about the aggregated moves";
		aDescr.addResultDescr(rd);
		aggTbl.setMadeByAction(aDescr);

		ShowRecManager recMan = null;
		if (dataLoader instanceof DataManager) {
			recMan = ((DataManager) dataLoader).getShowRecManager(aggTblN);
		}
		if (recMan != null) {
			Vector showAttr = new Vector(aggTbl.getAttrCount(), 10);
			for (int i = 0; i < aggTbl.getAttrCount() - 1; i++)
				if (i != trIdsIdx) {
					showAttr.addElement(aggTbl.getAttributeId(i));
				}
			recMan.setPopupAddAttrs(showAttr);
		}
		return aggLinkLayer;
	}
}
