package spade.analysis.tools.moves;

import java.awt.Checkbox;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.util.Vector;

import spade.analysis.system.ESDACore;
import spade.analysis.tools.BaseAnalyser;
import spade.analysis.tools.events.EventMaker;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Dialogs;
import spade.lib.basicwin.OKDialog;
import spade.lib.basicwin.SelectDialog;
import spade.lib.util.IdMaker;
import spade.time.TimeMoment;
import spade.time.TimeReference;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.database.SpatialEntity;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;
import spade.vis.dmap.DMovingObject;
import spade.vis.dmap.DMovingObjectSegmFilterCombiner;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.MultiGeometry;
import spade.vis.geometry.RealPoint;
import spade.vis.space.GeoLayer;
import spade.vis.space.LayerManager;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Feb 3, 2010
 * Time: 12:38:31 PM
 * Extracts events from trajectories using the current filter of the trajectory segments.
 */
public class EventExtractor extends BaseAnalyser {
	/**
	 * Reserved identifiers for the table columns with the trajectory id and mover id
	 */
	public static final String trajIdColId = "__traj_id__", moverIdColId = "__mover_id__";
	/**
	 * Possible strategies for dealing with 2 or more consecutive trajectory points
	 * satisfying the current filter:
	 * firstPoint - create an event only from the first point in the sequence
	 * lastPoint - create an event only from the last point in the sequence
	 * midPoint - create an event from the middle point in the sequence
	 * separatePoints - create separate events from all points of the sequence
	 * avePoint - create an event from the average point of all points
	 * medoid - create an event from the medoid (the point with the minimal distance to all others)
	 * multiPoints - create a multi-point event from all points of the sequence
	 */
	public static final int firstPoint = 0, lastPoint = 1, midPoint = 2, separatePoints = 3, avePoint = 4, medoid = 5, multiPoints = 6;

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
		for (int i = 0; i < lman.getLayerCount(); i++) {
			GeoLayer layer = lman.getGeoLayer(i);
			if ((layer instanceof DGeoLayer) && layer.getObjectCount() > 0 && (layer.getObjectAt(0) instanceof DMovingObject)) {
				moveLayers.addElement(layer);
			}
		}
		if (moveLayers.size() < 1) {
			showMessage("No layers with trajectories found!", true);
			return;
		}
		Panel mainP = new Panel(new ColumnLayout());
		mainP.add(new Label("Extraction of events from trajectories", Label.CENTER));
		mainP.add(new Label("(based on current filtering of segments of trajectories)", Label.CENTER));
		mainP.add(new Label("Select the layer with trajectories:"));
		List list = new List(Math.max(moveLayers.size() + 1, 5));
		for (int i = 0; i < moveLayers.size(); i++) {
			list.add(((DGeoLayer) moveLayers.elementAt(i)).getName());
		}
		list.select(0);
		mainP.add(list);
		Checkbox onlyActiveCB = new Checkbox("use only active (after filtering) trajectories", true);
		mainP.add(onlyActiveCB);

		OKDialog dia = new OKDialog(getFrame(), "Extract events", true);
		dia.addContent(mainP);
		dia.show();
		if (dia.wasCancelled())
			return;
		int idx = list.getSelectedIndex();
		if (idx < 0)
			return;
		boolean onlyActive = onlyActiveCB.getState();
		DGeoLayer moveLayer = (DGeoLayer) moveLayers.elementAt(idx);

		extractEventsFromTrajectories(moveLayer, onlyActive, null, null, core);
	}

	/**
	 * @param moveLayer - the layer with the trajectories
	 * @param onlyActive - whether to extract events only from active (not filtered out) trajectories
	 * @param attrVals - values of an attribute associated with the trajectory points (may be null)
	 * @param attrName - name of the attribute associated with the trajectory points (may be null)
	 * @param core - used for displaying messages, adding map layers and tables, etc.
	 */
	public void extractEventsFromTrajectories(DGeoLayer moveLayer, boolean onlyActive, double attrVals[][], String attrName, ESDACore core) {
		this.core = core;
		//check if there is any segment filter attached to the layer
		boolean filtered = false;
		for (int i = 0; i < moveLayer.getObjectCount() && !filtered; i++) {
			DMovingObject mobj = (DMovingObject) moveLayer.getObject(i);
			DMovingObjectSegmFilterCombiner sfc = mobj.getSegmFilterCombiner();
			if (sfc == null) {
				continue;
			}
			filtered = !sfc.areAllSegmentsActive();
		}
		if (!filtered) {
			showMessage("The segments of the trajectories are not filtered!", true);
			Dialogs.showMessage(getFrame(), "The segments of the trajectories are currently not filtered. " + "Apply filtering to the segments by means of Trajectories Time Graph.", "Apply segment filtering");
			return;
		}
		String evType = Dialogs.askForStringValue(getFrame(), "Event type?", "", null, "Event type?", false);
		EventMaker evMaker = new EventMaker();
		evMaker.setSystemCore(core);
		if (evMaker.chooseOrMakeGeoEventLayer(Geometry.point, moveLayer.isGeographic(), true) == null)
			return;
		TimeReference tSpan = moveLayer.getTimeSpan();
		if (tSpan != null) {
			evMaker.accountForEventTimeRange(tSpan.getValidFrom(), tSpan.getValidUntil());
		}
		DataTable trTable = (DataTable) moveLayer.getThematicData();
		String aName = "Count of " + evType, add = "";
		for (int i = 0; i < 10000; i++) {
			int aidx = trTable.findAttrByName(aName + add);
			if (aidx >= 0) {
				add = " (" + (i + 2) + ")";
			}
		}
		aName = aName + add;
/*
    aName=Dialogs.askForStringValue(getFrame(),"Attribute name?",aName,
      "A new attribute will be added to the table "+trTable.getName(),
      "New attribute",false);
*/
		trTable.addAttribute(aName, IdMaker.makeId(aName, trTable), AttributeTypes.integer);
		trTable.makeUniqueAttrIdentifiers();
		int evCountColN = trTable.getAttrCount() - 1;
		DataTable evTbl = evMaker.getEventTable();
		int trajIdColN = evTbl.getAttrIndex(trajIdColId), moverIdColN = evTbl.getAttrIndex(moverIdColId);
		Vector v = new Vector(3, 1);
		boolean attrAdded = false;
		if (trajIdColN < 0) {
			evTbl.addAttribute("Trajectory Id", trajIdColId, AttributeTypes.character);
			trajIdColN = evTbl.getAttrCount() - 1;
			v.addElement(trajIdColId);
			attrAdded = true;
		}
		if (moverIdColN < 0) {
			evTbl.addAttribute("Mover Id", moverIdColId, AttributeTypes.character);
			moverIdColN = evTbl.getAttrCount() - 1;
			v.addElement(moverIdColId);
			attrAdded = true;
		}
		int attrColN = -1;
		if (attrVals != null && attrName != null) {
			evTbl.addAttribute(attrName, IdMaker.makeId(attrName, evTbl), AttributeTypes.real);
			attrColN = evTbl.getAttrCount() - 1;
			v.addElement(evTbl.getAttributeId(attrColN));
			attrAdded = true;
		}
		int nEvents = 0, nPrevEvents = evTbl.getDataItemCount();
		boolean asked = false;
		int multiplePointStrategy = -1;
		boolean pointGetsTimeOfSequence = true;
		Vector<SpatialEntity> sequence = null;
		PointsInCircle pc = null;
		for (int i = 0; i < moveLayer.getObjectCount(); i++) {
			DMovingObject mobj = (DMovingObject) moveLayer.getObject(i);
			int oIdx = trTable.getObjectIndex(mobj.getIdentifier());
			if (oIdx >= 0) {
				trTable.setNumericAttributeValue(0, evCountColN, oIdx);
			}
			if (!onlyActive || moveLayer.isObjectActive(i)) {
				Vector track = mobj.getTrack();
				if (track == null || track.size() < 1) {
					continue;
				}
				DMovingObjectSegmFilterCombiner sfc = mobj.getSegmFilterCombiner();
				if (sfc != null && !sfc.isAnySegmentActive()) {
					continue;
				}
				if (sfc != null && sfc.areAllSegmentsActive()) {
					sfc = null;
				}
				int evCount = 0;
				int j = 0;
				double directions[] = mobj.getDirections();
				while (j < track.size())
					if (sfc != null && !sfc.isSegmentActive(j)) {
						++j;
					} else {
						SpatialEntity spe = (SpatialEntity) track.elementAt(j);
						TimeReference tr = spe.getTimeReference();
						TimeMoment t1 = tr.getValidFrom(), t2 = tr.getValidUntil();
						if (t2 == null) {
							t2 = t1;
						}
						Geometry geom = null;
						boolean makeMultipleEvents = false;
						if (sequence != null) {
							sequence.removeAllElements();
						}
						if (j + 1 >= track.size() || (sfc != null && !sfc.isSegmentActive(j + 1))) {
							geom = spe.getCentre();
							++j;
						} else {
							if (!asked) {
								multiplePointStrategy = askMultiplePointStrategy();
								asked = true;
								if (multiplePointStrategy == separatePoints) {
									SelectDialog selDia = new SelectDialog(getFrame(), "Time references?", "What start and end times should be attached to the separate events " + "generated from consecutive points?");
									selDia.addOption("Each event gets the start and end times of the whole sequence", "all", false);
									selDia.addOption("Each event get the start and end time of the single point", "single", true);
									selDia.show();
									pointGetsTimeOfSequence = selDia.getSelectedOptionN() == 0;
								}
							}
							if (sequence == null) {
								sequence = new Vector<SpatialEntity>(10, 10);
							} else {
								sequence.removeAllElements();
							}
							int j0 = j;
							sequence.addElement(spe);
							++j;
							while (j < track.size() && (sfc == null || sfc.isSegmentActive(j))) {
								sequence.addElement((SpatialEntity) track.elementAt(j));
								++j;
							}
							if (pointGetsTimeOfSequence && sequence.size() > 1) {
								tr = sequence.elementAt(sequence.size() - 1).getTimeReference();
								t2 = tr.getValidUntil();
								if (t2 == null) {
									t2 = tr.getValidFrom();
								}
							}
							switch (multiplePointStrategy) {
							case firstPoint:
								geom = spe.getCentre();
								break;
							case lastPoint:
								spe = sequence.elementAt(sequence.size() - 1);
								geom = spe.getCentre();
								break;
							case midPoint:
								spe = sequence.elementAt(sequence.size() / 2);
								geom = spe.getCentre();
								tr = spe.getTimeReference();
								if (sequence.size() % 2 == 0) {
									SpatialEntity spe0 = sequence.elementAt(sequence.size() / 2 - 1);
									RealPoint p0 = spe0.getCentre(), p1 = (RealPoint) geom;
									p0.x = (p0.x + p1.x) / 2;
									p0.y = (p0.y + p1.y) / 2;
									geom = p0;
								}
								break;
							case separatePoints:
								makeMultipleEvents = true;
								for (int k = 0; k < sequence.size(); k++) {
									spe = sequence.elementAt(k);
									RealPoint p = (RealPoint) spe.getCentre().clone();
									if (!pointGetsTimeOfSequence) {
										tr = spe.getTimeReference();
										t1 = tr.getValidFrom();
										t2 = tr.getValidUntil();
									}
									DGeoObject eo = evMaker.addEvent(String.valueOf(nPrevEvents + nEvents + 1), evType, p, t1, t2);
									if (eo != null) {
										if (pointGetsTimeOfSequence && sequence.size() > 1) {
											evMaker.getEventSpatialPropertiesFromSequence(eo, sequence);
										} else {
											evMaker.recordEventDirection(eo, directions[j0 + k]);
										}
										DataRecord rec = (DataRecord) eo.getData();
										rec.setAttrValue(mobj.getIdentifier(), trajIdColN);
										rec.setAttrValue(mobj.getEntityId(), moverIdColN);
										if (attrColN >= 0 && attrVals[i] != null && !Double.isNaN(attrVals[i][j0 + k])) {
											rec.setNumericAttrValue(attrVals[i][j0 + k], attrColN);
										}
										++nEvents;
										++evCount;
									}
								}
								break;
							case avePoint:
								float sumX = 0,
								sumY = 0;
								int np = 0;
								for (int k = 0; k < sequence.size(); k++) {
									spe = sequence.elementAt(k);
									RealPoint p = spe.getCentre();
									sumX += p.x;
									sumY += p.y;
									++np;
								}
								geom = new RealPoint(sumX / np, sumY / np);
								break;
							case medoid:
								if (sequence.size() < 3) {
									geom = spe.getCentre();
								} else {
									if (pc == null) {
										pc = new PointsInCircle(1, 1);
									} else {
										pc.removeAllPoints();
									}
									for (int k = 0; k < sequence.size(); k++) {
										pc.addPoint(sequence.elementAt(k).getCentre());
									}
									geom = pc.getTrueMedoid();
								}
								break;
							case multiPoints:
								MultiGeometry mg = new MultiGeometry();
								geom = mg;
								for (int k = 0; k < sequence.size(); k++) {
									spe = sequence.elementAt(k);
									RealPoint p = (RealPoint) spe.getCentre().clone();
									tr = spe.getTimeReference();
									p.setTimeReference(tr);
									mg.addPart(p);
								}
								break;
							}
						}
						if (!makeMultipleEvents) {
							DGeoObject eo = evMaker.addEvent(String.valueOf(nPrevEvents + nEvents + 1), evType, geom, t1, t2);
							if (eo != null) {
								if (!(geom instanceof MultiGeometry)) {
									if (pointGetsTimeOfSequence && sequence != null && sequence.size() > 1) {
										evMaker.getEventSpatialPropertiesFromSequence(eo, sequence);
									} else {
										evMaker.recordEventDirection(eo, directions[j - 1]);
									}
								}
								DataRecord rec = (DataRecord) eo.getData();
								rec.setAttrValue(mobj.getIdentifier(), trajIdColN);
								rec.setAttrValue(mobj.getEntityId(), moverIdColN);
								if (attrColN >= 0 && attrVals[i] != null) {
									if (sequence == null || sequence.size() < 2) {
										if (!Double.isNaN(attrVals[i][j - 1])) {
											rec.setNumericAttrValue(attrVals[i][j - 1], attrColN);
										}
									} else { //compute and write the average value
										double sum = 0;
										int j0 = j - sequence.size(), nVals = 0;
										for (int k = j0; k < j; k++)
											if (!Double.isNaN(attrVals[i][k])) {
												sum += attrVals[i][k];
												++nVals;
											}
										if (nVals > 0) {
											rec.setNumericAttrValue(sum / nVals, attrColN);
										}
									}
								}
								++nEvents;
								++evCount;
							}
						}
					}
				if (oIdx >= 0) {
					trTable.setNumericAttributeValue(evCount, evCountColN, oIdx);
				}
			}
		}
		if (nEvents < 1) {
			showMessage("No active segments have been found!", true);
		} else {
			evMaker.finishLayerBuilding();
			showMessage("Created " + nEvents + " events", false);
			if (attrAdded) {
				evTbl.notifyPropertyChange("new_attributes", null, v);
			}
		}
	}

	/**
	 * Asks the user what to do with several consecutive points satisfying the filter.
	 * Returns one of the following constants:
	 * firstPoint - create an event only from the first point of the sequence
	 * lastPoint - create an event only from the last point of the sequence
	 * midPoint - create an event from the middle point of the sequence
	 * separatePoints - create separate events from all points of the sequence
	 * avePoint - create an event from the average point of all points
	 * medoid - create an event from the medoid (the point with the minimal distance to all others)
	 * multiPoints - create a multi-point event from all points of the sequence
	 */
	public int askMultiplePointStrategy() {
		SelectDialog selDia = new SelectDialog(getFrame(), "Dealing with multiple points", "What to do with two or more consecutive points satisfying the filter?");
		selDia.addOption("FIRST: create one event from the first point of the sequence", "first", false);
		selDia.addOption("LAST: create one event from the last point of the sequence", "last", false);
		selDia.addOption("MIDDLE: create one event from the middle point of the sequence", "middle", false);
		selDia.addOption("SEPARATE: create separate events from all points of the sequence", "separate", false);
		selDia.addOption("AVERAGE: create one event with the average position from all points", "average", false);
		selDia.addOption("MEDOID: create one event from the medoid of the points", "medoid", false);
		selDia.addOption("MULTI-POINT: create one multi-point event from all points", "multi", true);
		selDia.show();
		return selDia.getSelectedOptionN();
	}

	/**
	 * From the given layer with events, retrieves events relevant for the object with
	 * the given identifier. It is assumed that the layer has a table where one of the
	 * columns contains the identifiers of the objects relevant to the events.
	 * @param objId - the identifier of the object
	 * @param evLayer - the layer with the events
	 * @param evTable - the table describing the events
	 * @param objIdColN - the index of the table column with the identifier of the relevant objects
	 * @param onlyActiveEvents - whether only active (after filtering) events are considered
	 * @return a vector of relevant events
	 */
	public static Vector<DGeoObject> getEventsForObject(String objId, DGeoLayer evLayer, AttributeDataPortion evTable, int objIdColN, boolean onlyActiveEvents) {
		if (objId == null || evLayer == null || evTable == null || objIdColN < 0)
			return null;
		if (evLayer.getObjectCount() < 1 || evTable.getDataItemCount() < 1)
			return null;
		if (objIdColN >= evTable.getAttrCount())
			return null;
		Vector<DGeoObject> events = new Vector<DGeoObject>(100, 100);
		boolean sameTable = evTable.equals(evLayer.getThematicData());
		for (int i = 0; i < evLayer.getObjectCount(); i++)
			if (!onlyActiveEvents || evLayer.isObjectActive(i)) {
				DGeoObject evt = evLayer.getObject(i);
				boolean relevant = false;
				if (sameTable)
					if (evt.getData() == null) {
						continue;
					} else {
						relevant = objId.equals(evt.getData().getAttrValueAsString(objIdColN));
					}
				else {
					int rIdx = evTable.indexOf(evt.getIdentifier());
					if (rIdx < 0) {
						continue;
					}
					relevant = objId.equals(evTable.getAttrValueAsString(objIdColN, rIdx));
				}
				if (relevant) {
					events.addElement(evt);
				}

			}
		if (events.size() < 1)
			return null;
		return events;
	}
}
