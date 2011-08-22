package spade.analysis.tools.moves;

import java.awt.Color;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.util.Vector;

import spade.analysis.system.DataLoader;
import spade.analysis.system.ESDACore;
import spade.analysis.tools.DataAnalyser;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.OKDialog;
import spade.lib.basicwin.SelectDialog;
import spade.lib.util.IdMaker;
import spade.lib.util.IntArray;
import spade.lib.util.ObjectWithMeasure;
import spade.time.TimeMoment;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.database.SpatialEntity;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;
import spade.vis.dmap.DMovingObject;
import spade.vis.dmap.DPlaceVisitsObject;
import spade.vis.dmap.DrawingParameters;
import spade.vis.geometry.Computing;
import spade.vis.geometry.GeoComp;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.RealPoint;
import spade.vis.geometry.RealPolyline;
import spade.vis.space.GeoLayer;
import spade.vis.space.LayerManager;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Apr 3, 2008
 * Time: 2:55:33 PM
 * Builds simplified trajectories using existing area objects as
 * generalized positions.
 */
public class TrajectoriesByAreasSimplifier implements DataAnalyser {
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
		Vector areaLayers = new Vector(lman.getLayerCount(), 1);
		for (int i = 0; i < lman.getLayerCount(); i++) {
			GeoLayer layer = lman.getGeoLayer(i);
			if ((layer instanceof DGeoLayer) && layer.getObjectCount() > 0)
				if (layer.getObjectAt(0) instanceof DMovingObject) {
					moveLayers.addElement(layer);
				} else if (layer.getType() == Geometry.area && layer.getObjectCount() > 1) {
					areaLayers.addElement(layer);
				}
		}
		if (moveLayers.size() < 1) {
			showMessage("No layers with trajectories found!", true);
			return;
		}
		if (areaLayers.size() < 1) {
			showMessage("No layers with areas found!", true);
			return;
		}
		Panel mainP = new Panel(new ColumnLayout());
		mainP.add(new Label("Select the layer with trajectories to generalize"));
		List mList = new List(Math.max(moveLayers.size() + 1, 5));
		for (int i = 0; i < moveLayers.size(); i++) {
			mList.add(((DGeoLayer) moveLayers.elementAt(i)).getName());
		}
		mList.select(0);
		mainP.add(mList);
		mainP.add(new Label("Select the layer with areas:"));
		List aList = new List(Math.max(areaLayers.size() + 1, 5));
		for (int i = 0; i < areaLayers.size(); i++) {
			aList.add(((DGeoLayer) areaLayers.elementAt(i)).getName());
		}
		aList.select(0);
		mainP.add(aList);
		OKDialog dia = new OKDialog(core.getUI().getMainFrame(), "Generalize trajectories", true);
		dia.addContent(mainP);
		dia.show();
		if (dia.wasCancelled())
			return;
		int idx = mList.getSelectedIndex();
		if (idx < 0)
			return;
		DGeoLayer moveLayer = (DGeoLayer) moveLayers.elementAt(idx);
		idx = aList.getSelectedIndex();
		if (idx < 0)
			return;
		DGeoLayer areaLayer = (DGeoLayer) areaLayers.elementAt(idx);
		int nAreas = areaLayer.getObjectCount();
		boolean hasNeiLists = false;
		for (int i = 0; i < nAreas && !hasNeiLists; i++) {
			Vector<String> nei = areaLayer.getObject(i).neighbours;
			hasNeiLists = nei != null && nei.size() > 0;
		}
		boolean interByCross = false, interByShortestPath = false;
		if (hasNeiLists) {
			SelectDialog selDia = new SelectDialog(core.getUI().getMainFrame(), "Interpolate?", "What to do if two consecutive points are not in neighbouring areas?");
			selDia.addOption("insert areas crossed by the connecting lines", "cross", false);
			selDia.addOption("build the shortest path through adjoining areas", "path", false);
			selDia.addOption("do not interpolate", "no", true);
			selDia.show();
			interByCross = selDia.getSelectedOptionId().equals("cross");
			interByShortestPath = selDia.getSelectedOptionId().equals("path");
		}
		boolean interpolate = interByCross || interByShortestPath;

		//construct simplified trajectories containing only
		//the centroid for each generalized place
		Vector<DPlaceVisitsObject> pvObjects = new Vector(nAreas, 10);
		for (int i = 0; i < nAreas; i++) {
			DGeoObject area = areaLayer.getObject(i);
			DPlaceVisitsObject obj = new DPlaceVisitsObject();
			obj.setup(area.getSpatialData());
			obj.neighbours = area.neighbours;
			pvObjects.addElement(obj);
		}
		boolean geo = areaLayer.isGeographic();
		Vector<DMovingObject> genTr = new Vector<DMovingObject>(moveLayer.getObjectCount(), 1);
		for (int i = 0; i < moveLayer.getObjectCount(); i++) {
			DGeoObject gobj = moveLayer.getObject(i);
			if (!(gobj instanceof DMovingObject)) {
				continue;
			}
			DMovingObject mobj = (DMovingObject) gobj;
			String id = mobj.getIdentifier();
			Vector track = mobj.getTrack();
			if (track == null || track.size() < 2) {
				continue;
			}
			DMovingObject genMObj = new DMovingObject();
			genMObj.setIdentifier(mobj.getIdentifier());
			genMObj.setEntityId(mobj.getEntityId());
			genMObj.setGeographic(mobj.isGeographic());
			if (mobj.getData() != null) {
				genMObj.setThematicData(mobj.getData());
			}
			genMObj.listOfVisitedAreas = null;
			genTr.addElement(genMObj);
			int j = 0;
			int lastAreaIdx = -1;
			DPlaceVisitsObject lastArea = null;
			while (j < track.size()) {
				SpatialEntity spe = (SpatialEntity) track.elementAt(j);
				RealPoint pt = spe.getCentre();
				int aIdx = -1;
				if (!interpolate || lastAreaIdx < 0 || j == 0 || lastArea.neighbours == null || lastArea.neighbours.size() < 1) {
					aIdx = areaLayer.findObjectContainingPosition(pt.x, pt.y);
				} else {
					for (int nn = 0; nn < lastArea.neighbours.size() && aIdx < 0; nn++) {
						int k = areaLayer.getObjectIndex(lastArea.neighbours.elementAt(nn));
						if (k < 0 || k == lastAreaIdx) {
							continue;
						}
						if (areaLayer.getObject(k).contains(pt.x, pt.y, 0)) {
							aIdx = k;
						}
					}
					if (aIdx < 0)
						if (interByCross) {
							SpatialEntity spe0 = (SpatialEntity) track.elementAt(j - 1);
							RealPoint pt0 = spe0.getCentre();
							TimeMoment t0 = spe0.getTimeReference().getValidUntil();
							TimeMoment t1 = spe.getTimeReference().getValidFrom();
							IntArray visited = new IntArray(10, 10);
							visited.addElement(lastAreaIdx);
							do {
								//find the neibouring area intersected by the trajectory segment
								double minDist = Double.NaN;
								int neiIdx = -1;
								DPlaceVisitsObject nei = null;
								for (int nn = 0; nn < lastArea.neighbours.size() && aIdx < 0; nn++) {
									int k = areaLayer.getObjectIndex(lastArea.neighbours.elementAt(nn));
									if (k < 0 || k == lastAreaIdx || visited.indexOf(k) >= 0) {
										continue;
									}
									DPlaceVisitsObject pl = pvObjects.elementAt(k);
									RealPoint inter[] = Computing.findIntersections(pt0, pt, (RealPolyline) pl.getGeometry());
									if (inter == null) {
										continue;
									}
									for (RealPoint element : inter) {
										double d = GeoComp.distance(pt0.x, pt0.y, element.x, element.y, geo);
										if (Double.isNaN(minDist) || minDist > d) {
											minDist = d;
											neiIdx = k;
											nei = pl;
										}
									}
								}
								if (neiIdx < 0) {
									break; //seems impossible...
								}
								RealPoint c = SpatialEntity.getCentre(nei.getGeometry());
								RealPoint pCross = Computing.closestPoint(pt.x, pt.y, pt0.x, pt0.y, c.x, c.y);
								double rest = GeoComp.distance(pt.x, pt.y, pCross.x, pCross.y, geo);
								double distRatio = minDist / (minDist + rest);
								long timeDiff = t1.subtract(t0);
								TimeMoment t = t0.getCopy();
								t.add(Math.round(distRatio * timeDiff));
								genMObj.addPosition(c, t0, t);
								if (genMObj.listOfVisitedAreas == null) {
									genMObj.listOfVisitedAreas = nei.getIdentifier();
									genMObj.nVisitedAreas = 1;
								} else {
									genMObj.listOfVisitedAreas += " " + nei.getIdentifier();
									++genMObj.nVisitedAreas;
								}
								lastAreaIdx = neiIdx;
								lastArea = nei;
								visited.addElement(lastAreaIdx);
								pt0 = pCross;
								t0 = t;
								for (int nn = 0; nn < lastArea.neighbours.size() && aIdx < 0; nn++) {
									int k = areaLayer.getObjectIndex(lastArea.neighbours.elementAt(nn));
									if (k < 0 || k == lastAreaIdx || visited.indexOf(k) >= 0) {
										continue;
									}
									if (pvObjects.elementAt(k).contains(pt.x, pt.y, 0)) {
										aIdx = k;
									}
								}
							} while (aIdx < 0);
						} else if (interByShortestPath) {
							aIdx = areaLayer.findObjectContainingPosition(pt.x, pt.y);
							ObjectWithMeasure om = areaLayer.getShortestPath(lastAreaIdx, aIdx, 1.5f);
							if (om != null) {
								int path[] = (int[]) om.obj;
								int np = path.length - 1;
								SpatialEntity spe0 = (SpatialEntity) track.elementAt(j - 1);
								TimeMoment t0 = spe0.getTimeReference().getValidUntil();
								TimeMoment t1 = spe.getTimeReference().getValidFrom();
								long timeDiff = t1.subtract(t0), step = timeDiff / np;
								for (int pi = 1; pi < path.length - 1; pi++) {
									TimeMoment t = t0.getCopy();
									t.add(step);
									DGeoObject area = areaLayer.getObject(path[pi]);
									RealPoint pp = SpatialEntity.getCentre(area.getGeometry());
									genMObj.addPosition(pp, t0, t);
									if (genMObj.listOfVisitedAreas == null) {
										genMObj.listOfVisitedAreas = area.getIdentifier();
										genMObj.nVisitedAreas = 1;
									} else {
										genMObj.listOfVisitedAreas += " " + area.getIdentifier();
										++genMObj.nVisitedAreas;
									}
									t0 = t;
								}
							}
						}
					if (aIdx < 0) {
						aIdx = areaLayer.findObjectContainingPosition(pt.x, pt.y);
					}
				}
				if (aIdx < 0) {
					continue;
				}
				DPlaceVisitsObject place = pvObjects.elementAt(aIdx);
				lastAreaIdx = aIdx;
				lastArea = place;
				TimeMoment t0 = spe.getTimeReference().getValidFrom();
				int exitIdx = place.addVisit(id, track, j, moveLayer.isGeographic());
				SpatialEntity spe1 = (SpatialEntity) track.elementAt(exitIdx);
				TimeMoment t1 = spe1.getTimeReference().getValidUntil();
				if (t1 == null) {
					t1 = spe1.getTimeReference().getValidFrom();
				}
				RealPoint pos = SpatialEntity.getCentre(place.getGeometry());
				genMObj.addPosition(pos, t0, t1);
				if (genMObj.listOfVisitedAreas == null) {
					genMObj.listOfVisitedAreas = place.getIdentifier();
					genMObj.nVisitedAreas = 1;
				} else {
					genMObj.listOfVisitedAreas += " " + place.getIdentifier();
					++genMObj.nVisitedAreas;
				}
				j = exitIdx + 1;
			}
			mobj.setGeneralisedTrack(genMObj.getTrack());
			if ((i + 1) % 100 == 0) {
				showMessage("Processed " + (i + 1) + " trajectories of " + moveLayer.getObjectCount(), false);
			}
		}

		//build a layer with generalized trajectories
		String layerName = "Generalised " + moveLayer.getName() + " by " + areaLayer.getName();
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

	protected void showMessage(String msg, boolean error) {
		if (core != null && core.getUI() != null) {
			core.getUI().showMessage(msg, error);
		} else if (error) {
			System.out.println("!--> " + msg);
		}
	}
}
