package spade.analysis.tools.moves;

import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Color;
import java.awt.FlowLayout;
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
import spade.time.TimeMoment;
import spade.time.TimeReference;
import spade.vis.action.Highlighter;
import spade.vis.database.DataTable;
import spade.vis.database.SpatialEntity;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DMovingObject;
import spade.vis.dmap.DrawingParameters;
import spade.vis.geometry.GeoComp;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.RealPoint;
import spade.vis.space.GeoLayer;
import spade.vis.space.GeoObject;
import spade.vis.space.LayerManager;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: 22-Aug-2007
 * Time: 16:04:20
 * Splits trajectories by specified places (areas).
 */
public class TrajectorySplitter implements DataAnalyser {

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
		//and layers with area objects
		LayerManager lman = core.getUI().getCurrentMapViewer().getLayerManager();
		Vector moveLayers = new Vector(lman.getLayerCount(), 1);
		Vector areaLayers = new Vector(lman.getLayerCount(), 1);
		boolean someAreasSelected = false;
		for (int i = 0; i < lman.getLayerCount(); i++) {
			GeoLayer layer = lman.getGeoLayer(i);
			if ((layer instanceof DGeoLayer) && layer.getObjectCount() > 0 && (layer.getObjectAt(0) instanceof DMovingObject)) {
				moveLayers.addElement(layer);
			} else if (layer.getType() == Geometry.area) {
				areaLayers.addElement(layer);
				if (!someAreasSelected) {
					Highlighter hl = core.getHighlighterForSet(layer.getEntitySetIdentifier());
					if (hl != null) {
						Vector sel = hl.getSelectedObjects();
						someAreasSelected = sel != null && sel.size() > 0;
					}
				}
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
		mainP.add(new Label("Select the layer with trajectories to split:"));
		List tList = new List(Math.max(moveLayers.size() + 1, 3));
		for (int i = 0; i < moveLayers.size(); i++) {
			tList.add(((DGeoLayer) moveLayers.elementAt(i)).getName());
		}
		tList.select(0);
		mainP.add(tList);
		mainP.add(new Label("Select the layer with places (areas) to use \nfor splitting:"));
		List aList = new List(Math.max(areaLayers.size() + 1, 3));
		for (int i = 0; i < areaLayers.size(); i++) {
			aList.add(((GeoLayer) areaLayers.elementAt(i)).getName());
		}
		aList.select(0);
		mainP.add(aList);
		mainP.add(new Label("Note: only currently active objects will be used!"));
		CheckboxGroup cbg = null;
		Checkbox cb[] = null;
		if (someAreasSelected) {
			cbg = new CheckboxGroup();
			cb = new Checkbox[3];
			cb[0] = new Checkbox("use only selected areas", true, cbg);
			cb[1] = new Checkbox("exclude selected areas", false, cbg);
			cb[2] = new Checkbox("ignore the selection", false, cbg);
			for (int i = 0; i < 3; i++) {
				mainP.add(cb[i]);
			}
		}
		Panel pan = new Panel(new FlowLayout(FlowLayout.LEFT));
		pan.add(new Label("Minimum time spent in a place:"));
		TextField timeTF = new TextField("300", 10);
		pan.add(timeTF);
		mainP.add(pan);
		OKDialog dia = new OKDialog(core.getUI().getMainFrame(), "Split trajectories", true);
		dia.addContent(mainP);
		dia.show();
		if (dia.wasCancelled())
			return;
		int idxT = tList.getSelectedIndex();
		if (idxT < 0)
			return;
		int idxA = aList.getSelectedIndex();
		if (idxA < 0)
			return;
		DGeoLayer moveLayer = (DGeoLayer) moveLayers.elementAt(idxT);
		GeoLayer areaLayer = (GeoLayer) areaLayers.elementAt(idxA);
		boolean useSelected = false, excludeSelected = false;
		Vector selected = null;
		if (cb != null && (cb[0].getState() || cb[1].getState())) {
			Highlighter hl = core.getHighlighterForSet(areaLayer.getEntitySetIdentifier());
			if (hl != null) {
				selected = hl.getSelectedObjects();
			}
			if (selected == null || selected.size() < 1) {
				if (!Dialogs.askYesOrNo(core.getUI().getMainFrame(), "None of the areas " + "is currently selected. Use all currently active areas?", "No selected areas!"))
					return;
			} else {
				useSelected = cb[0].getState();
				excludeSelected = cb[1].getState();
			}
		}
		String str = timeTF.getText();
		long minTime = 0l;
		if (str != null) {
			try {
				minTime = Long.valueOf(str).longValue();
			} catch (NumberFormatException nfe) {
			}
		}
		if (minTime < 0) {
			minTime = 0;
		}
		//geometries of the places
		Vector places = new Vector((useSelected) ? selected.size() : areaLayer.getObjectCount(), 1);
		if (useSelected) {
			for (int i = 0; i < selected.size(); i++) {
				GeoObject gobj = areaLayer.findObjectById((String) selected.elementAt(i));
				if (gobj.getGeometry() != null) {
					places.addElement(gobj.getGeometry());
				}
			}
		} else {
			for (int i = 0; i < areaLayer.getObjectCount(); i++) {
				GeoObject gobj = areaLayer.getObjectAt(i);
				if (gobj.getGeometry() == null) {
					continue;
				}
				if (excludeSelected && selected.contains(gobj.getIdentifier())) {
					continue;
				}
				if (!areaLayer.isObjectActive(i)) {
					continue;
				}
				places.addElement(gobj.getGeometry());
			}
		}
		if (places.size() < 1) {
			showMessage("No geometries of the areas found!", true);
			return;
		}
		Vector split = new Vector(moveLayer.getObjectCount() * 2, moveLayer.getObjectCount());
		for (int i = 0; i < moveLayer.getObjectCount(); i++)
			if (moveLayer.getObject(i) instanceof DMovingObject) {
				DMovingObject mobj = (DMovingObject) moveLayer.getObject(i);
				Vector track = mobj.getTrack();
				if (track == null || track.size() < 1) {
					continue;
				}
				if (track.size() < 3) {
					split.addElement(mobj.makeCopy());
					continue;
				}
				if (mobj.getIdentifier().equals("0420_255")) {
					System.out.println(mobj.getIdentifier());
				}
				int idx0 = 0, idxEnter = -1, nPieces = 0;
				Geometry place = null;
				TimeMoment tEnter = null, tLast = null;
				long maxTimeGap = 0;
				double minDistGap = Double.NaN;
				int idxMaxTimeGap = -1, idxMinDistGap = -1;
				RealPoint pLast = null;
				for (int j = 0; j < track.size(); j++) {
					SpatialEntity spe = (SpatialEntity) track.elementAt(j);
					RealPoint p = spe.getCentre();
					if (p == null) {
						continue;
					}
					TimeMoment t = null;
					TimeReference tr = spe.getTimeReference();
					if (tr != null) {
						t = tr.getValidFrom();
					}
					if (place != null) {
						if (pLast != null) {
							double dist = GeoComp.distance(p.x, p.y, pLast.x, pLast.y, moveLayer.isGeographic());
							if (Double.isNaN(minDistGap) || minDistGap > dist) {
								minDistGap = dist;
								idxMinDistGap = j;
							}
						}
						if (t != null && tLast != null) {
							long timeGap = t.subtract(tLast);
							if (timeGap > maxTimeGap) {
								maxTimeGap = timeGap;
								idxMaxTimeGap = j;
							}
						}
						if (place.contains(p.x, p.y, 0f, true)) {
							pLast = p;
							tLast = t;
							continue;
						} else {
							if (j + 1 < track.size() && idxEnter > idx0) { //to avoid 1-point trajectories
								long timeSpent = 0;
								if (tEnter != null && t != null) {
									timeSpent = t.subtract(tEnter);
								}
								boolean toSplit = true;
								if (minTime > 0 && tEnter != null) {
									toSplit = timeSpent >= minTime;
								}
								if (toSplit) {
									//produce a new trajectory
									int splitIdx = j;
									if (j - idxEnter > 1 && idxMinDistGap > 0) {
										if (idxMaxTimeGap == idxMinDistGap || idxMaxTimeGap < 0) {
											splitIdx = idxMinDistGap;
										} else {
											double aveTime = 1.0 * (timeSpent - maxTimeGap) / (j - idxEnter - 1);
											if (maxTimeGap > 2 * aveTime) {
												splitIdx = idxMaxTimeGap;
											} else {
												splitIdx = idxMinDistGap;
											}
										}
									}
									Vector sTrack = new Vector(splitIdx - idx0, 1);
									for (int k = idx0; k < splitIdx; k++) {
										sTrack.addElement(track.elementAt(k));
									}
									++nPieces;
									DMovingObject sobj = new DMovingObject();
									sobj.setGeographic(mobj.isGeographic());
									sobj.setIdentifier(mobj.getIdentifier() + "_" + nPieces);
									sobj.setEntityId(mobj.getEntityId());
									sobj.setTrack(sTrack);
									split.addElement(sobj);
									idx0 = splitIdx;
								}
							}
							place = null;
						}
					}
					if (place == null) {
						idxMaxTimeGap = -1;
						idxMinDistGap = -1;
						maxTimeGap = 0;
						minDistGap = Double.NaN;
						for (int k = 0; k < places.size() && place == null; k++) {
							place = (Geometry) places.elementAt(k);
							if (!place.contains(p.x, p.y, 0f, true)) {
								place = null;
							}
						}
						if (place != null) {
							idxEnter = j;
							if (t != null) {
								tEnter = t;
							}
						}
					}
					pLast = p;
					tLast = t;
				}
				if (nPieces > 0) { //put the last fragment
					Vector sTrack = new Vector(track.size() - idx0, 1);
					for (int k = idx0; k < track.size(); k++) {
						sTrack.addElement(track.elementAt(k));
					}
					++nPieces;
					DMovingObject sobj = new DMovingObject();
					sobj.setGeographic(mobj.isGeographic());
					sobj.setIdentifier(mobj.getIdentifier() + "_" + nPieces);
					sobj.setEntityId(mobj.getEntityId());
					sobj.setTrack(sTrack);
					split.addElement(sobj);
				} else {
					split.addElement(mobj.makeCopy());
				}
			}
		if (split.size() <= moveLayer.getObjectCount()) {
			showMessage("No new trajectories have been produced!", true);
			return;
		}
		showMessage("Produced " + (split.size() - moveLayer.getObjectCount()) + " additional trajectories", false);
		String layerName = "Divided " + moveLayer.getName();
/*
    layerName=Dialogs.askForStringValue(core.getUI().getMainFrame(),"Name of the new layer?",layerName,
      "The trajectories resulting from the splitting will form a new map layer. Simultaneously, " +
        "a new table with general data about the tajectories will be produced.","Layer name?",false);
*/
		DataTable table = TrajectoriesTableBuilder.makeTrajectoryDataTable(split);
		DGeoLayer sLayer = new DGeoLayer();
		sLayer.setGeographic(moveLayer.isGeographic());
		sLayer.setType(Geometry.line);
		sLayer.setName(layerName);
		sLayer.setGeoObjects(split, true);
		sLayer.setHasMovingObjects(true);
		DrawingParameters dp = sLayer.getDrawingParameters();
		if (dp == null) {
			dp = new DrawingParameters();
			sLayer.setDrawingParameters(dp);
		}
		dp.lineColor = Color.getHSBColor((float) Math.random(), 1 - 0.2f * (float) Math.random(), 1 - 0.2f * (float) Math.random());
		dp.fillContours = false;
		DataLoader dataLoader = core.getDataLoader();
		dataLoader.addMapLayer(sLayer, -1);
		table.setName(sLayer.getName() + ": general data");
		int tblN = dataLoader.addTable(table);
		table.setEntitySetIdentifier(sLayer.getEntitySetIdentifier());
		dataLoader.setLink(sLayer, tblN);
		sLayer.setLinkedToTable(true);
		if (sLayer.hasTimeReferences()) {
			dataLoader.processTimeReferencedObjectSet(sLayer);
			dataLoader.processTimeReferencedObjectSet(table);
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
