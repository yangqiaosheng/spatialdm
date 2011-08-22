package spade.analysis.tools.schedule;

import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Vector;

import spade.analysis.system.DataLoader;
import spade.analysis.system.ESDACore;
import spade.lib.util.IdMaker;
import spade.time.TimeReference;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.dataview.ShowRecManager;
import spade.vis.dmap.DAggregateLinkLayer;
import spade.vis.dmap.DAggregateLinkObject;
import spade.vis.dmap.DGeoObject;
import spade.vis.dmap.DLinkObject;
import spade.vis.dmap.DrawingParameters;
import spade.vis.geometry.Geometry;
import spade.vis.space.LayerManager;
import spade.vis.spec.DataSourceSpec;
import spade.vis.spec.LinkDataDescription;
import data_load.DataManager;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 01-Mar-2007
 * Time: 16:55:27
 * Builds a map layer (with a corresponding table) representing aggregated
 * transportation schedule data where several trips from A to B are united
 * into a single aggregate link (vector) between A and B.
 */
public class AggrScheduleLayerBuilder implements PropertyChangeListener {
	/**
	 * The system core, which is used for adding the generated table and layer
	 * to the system
	 */
	protected ESDACore core = null;
	/**
	 * The data needed for the generation of the layer and the table
	 */
	protected ScheduleData sData = null;

	public AggrScheduleLayerBuilder(ESDACore core, ScheduleData sData) {
		this.core = core;
		this.sData = sData;
	}

	public void buildAggregateLinks() {
		if (core == null || sData == null)
			return;
		if (sData.linkLayer == null || sData.linkLayer.getObjectCount() < 1) {
			core.getUI().showMessage("No moves or links to aggregate!", true);
			return;
		}
		if (sData.locLayer == null || sData.locLayer.getObjectCount() < 1) {
			core.getUI().showMessage("No locations given!", true);
			return;
		}
		Vector aggLinks = new Vector(sData.locLayer.getObjectCount() * sData.locLayer.getObjectCount(), 1);
		for (int i = 0; i < sData.linkLayer.getObjectCount(); i++)
			if (sData.linkLayer.getObject(i) instanceof DLinkObject) {
				DLinkObject link = (DLinkObject) sData.linkLayer.getObject(i);
				DGeoObject startNode = link.getStartNode(), endNode = link.getEndNode();
				if (startNode == null || endNode == null) {
					continue;
				}
				DAggregateLinkObject aggLink = null;
				for (int k = 0; k < aggLinks.size() && aggLink == null; k++) {
					aggLink = (DAggregateLinkObject) aggLinks.elementAt(k);
					if (!aggLink.startNode.getIdentifier().equals(startNode.getIdentifier()) || !aggLink.endNode.getIdentifier().equals(endNode.getIdentifier())) {
						aggLink = null;
					}
				}
				if (aggLink == null) {
					aggLink = new DAggregateLinkObject();
					aggLinks.addElement(aggLink);
				}
				aggLink.addLink(link, link.getIdentifier());
			}
		//construct a table with thematic information about the aggregated moves
		DataTable aggTbl = new DataTable();
		aggTbl.setName(sData.linkLayer.getName() + " (aggregated)");
		aggTbl.addAttribute("Start ID", "startId", AttributeTypes.character);
		int startIdIdx = aggTbl.getAttrCount() - 1;
		aggTbl.addAttribute("Start name", "start_name", AttributeTypes.character);
		int startNameIdx = aggTbl.getAttrCount() - 1;
		aggTbl.addAttribute("End ID", "endId", AttributeTypes.character);
		int endIdIdx = aggTbl.getAttrCount() - 1;
		aggTbl.addAttribute("End name", "end_name", AttributeTypes.character);
		int endNameIdx = aggTbl.getAttrCount() - 1;
		aggTbl.addAttribute("Direction", "direction", AttributeTypes.character);
		int dirIdx = aggTbl.getAttrCount() - 1;
		int distIdx = -1;
		if (sData.distancesMatrix != null) {
			aggTbl.addAttribute("Distance", "distance", AttributeTypes.integer);
			distIdx = aggTbl.getAttrCount() - 1;
		}
		aggTbl.addAttribute("Earliest start time", "startTime", AttributeTypes.time);
		int startTimeIdx = aggTbl.getAttrCount() - 1;
		Attribute timeAttr = aggTbl.getAttribute(startTimeIdx);
		timeAttr.timeRefMeaning = Attribute.VALID_FROM;
		aggTbl.addAttribute("Latest end time", "endTime", AttributeTypes.time);
		int endTimeIdx = aggTbl.getAttrCount() - 1;
		timeAttr = aggTbl.getAttribute(endTimeIdx);
		timeAttr.timeRefMeaning = Attribute.VALID_UNTIL;
		aggTbl.addAttribute("N of trips", "n_moves", AttributeTypes.integer);
		int nMovesIdx = aggTbl.getAttrCount() - 1;
		aggTbl.addAttribute("Min trip duration", "min_dur", AttributeTypes.integer);
		int minDurIdx = aggTbl.getAttrCount() - 1;
		aggTbl.addAttribute("Max trip duration", "max_dur", AttributeTypes.integer);
		int maxDurIdx = aggTbl.getAttrCount() - 1;
		int addAttrIdx = -1;
		String aNames[] = null;
		if (sData.moveAggregator != null) {
			aNames = sData.moveAggregator.getAttrNames();
			if (aNames.length > 1) {
				addAttrIdx = aggTbl.getAttrCount();
				for (int i = 1; i < aNames.length; i++) {
					aggTbl.addAttribute(aNames[i], IdMaker.makeId(aNames[i], aggTbl), AttributeTypes.integer);
				}
			}
		}
		DataSourceSpec spec = new DataSourceSpec();
		spec.id = aggTbl.getContainerIdentifier();
		spec.name = aggTbl.getName();
		spec.toBuildMapLayer = true;
		spec.descriptors = new Vector(5, 5);
		LinkDataDescription aggldd = new LinkDataDescription();
		aggldd.layerRef = sData.locLayer.getContainerIdentifier();
		aggldd.souColIdx = startIdIdx;
		aggldd.destColIdx = endIdIdx;
		aggldd.souTimeColIdx = startTimeIdx;
		aggldd.destTimeColIdx = endTimeIdx;
		spec.descriptors.addElement(aggldd);
		aggTbl.setDataSource(spec);
		for (int i = 0; i < aggLinks.size(); i++) {
			DAggregateLinkObject lobj = (DAggregateLinkObject) aggLinks.elementAt(i);
			DataRecord rec = new DataRecord(lobj.getIdentifier());
			aggTbl.addDataRecord(rec);
			String srcId = lobj.startNode.getIdentifier(), destId = lobj.endNode.getIdentifier();
			rec.setAttrValue(srcId, startIdIdx);
			rec.setAttrValue(destId, endIdIdx);
			String name = lobj.startNode.getName();
			if (name != null) {
				rec.setAttrValue(name, startNameIdx);
			}
			name = lobj.endNode.getName();
			if (name != null) {
				rec.setAttrValue(name, endNameIdx);
			}
			rec.setAttrValue(lobj.getLinkDirection(), dirIdx);
			if (sData.distancesMatrix != null && distIdx >= 0) {
				Float fl = (Float) sData.distancesMatrix.get(0, srcId, destId);
				if (fl != null) {
					rec.setNumericAttrValue(fl.floatValue(), fl.toString(), distIdx);
				}
			}
			rec.setAttrValue(lobj.firstTime, startTimeIdx);
			rec.setAttrValue(lobj.lastTime, endTimeIdx);
			TimeReference tr = new TimeReference();
			tr.setValidFrom(lobj.firstTime);
			tr.setValidUntil(lobj.lastTime);
			rec.setTimeReference(tr);
			int nLinks = lobj.souLinks.size();
			rec.setNumericAttrValue(nLinks, String.valueOf(nLinks), nMovesIdx);
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
			}
			rec.setNumericAttrValue(minDur, String.valueOf(minDur), minDurIdx);
			rec.setNumericAttrValue(maxDur, String.valueOf(maxDur), maxDurIdx);
			if (addAttrIdx >= 0) {
				int sIdx = sData.moveAggregator.getSrcIdx(srcId), dIdx = sData.moveAggregator.getDestIdx(destId);
				if (sIdx >= 0 && dIdx >= 0) {
					for (int j = 1; j < aNames.length; j++) {
						Object val = sData.moveAggregator.getMatrixValue(j, sIdx, dIdx);
						if (val != null && (val instanceof Integer)) {
							rec.setNumericAttrValue(((Integer) val).intValue(), val.toString(), addAttrIdx + j - 1);
						}
					}
				}
			}
			lobj.setThematicData(rec);
		}
		DataLoader dataLoader = core.getDataLoader();
		int aggTblN = dataLoader.addTable(aggTbl);

		DAggregateLinkLayer aggLinkLayer = new DAggregateLinkLayer();
		aggLinkLayer.setType(Geometry.line);
		aggLinkLayer.setName(aggTbl.getName());
		aggLinkLayer.setGeographic(sData.locLayer.isGeographic());
		aggLinkLayer.setGeoObjects(aggLinks, true);
		aggLinkLayer.setHasMovingObjects(true);
		aggLinkLayer.setTrajectoryLayer(sData.linkLayer);
		aggLinkLayer.setPlaceLayer(sData.locLayer);
		DrawingParameters dp1 = aggLinkLayer.getDrawingParameters();
		if (dp1 == null) {
			dp1 = new DrawingParameters();
			aggLinkLayer.setDrawingParameters(dp1);
		}
		dp1.lineColor = Color.red.darker();
		spec.drawParm = dp1;
		dataLoader.addMapLayer(aggLinkLayer, -1);
		aggTbl.setEntitySetIdentifier(aggLinkLayer.getEntitySetIdentifier());
		dataLoader.setLink(aggLinkLayer, aggTblN);
		aggLinkLayer.setLinkedToTable(true);
		aggLinkLayer.countActiveLinks();
		LayerManager lman = core.getUI().getCurrentMapViewer().getLayerManager();
		lman.activateLayer(aggLinkLayer.getContainerIdentifier());
		sData.aggLinkLayer = aggLinkLayer;
		sData.aggTbl = aggTbl;

		if (addAttrIdx >= 0) {
			sData.firstUpdatableColIdx = aggTbl.getAttrCount();
			for (int i = 1; i < aNames.length; i++) {
				aggTbl.addAttribute(aNames[i] + " for active trips", IdMaker.makeId(aNames[i] + " active", aggTbl), AttributeTypes.integer);
			}
			updateStatistics();
			sData.moveAggregator.addPropertyChangeListener(this);
		}

		ShowRecManager recMan = null;
		if (dataLoader instanceof DataManager) {
			recMan = ((DataManager) dataLoader).getShowRecManager(aggTblN);
		}
		if (recMan != null) {
			Vector showAttr = new Vector(aggTbl.getAttrCount(), 10);
			for (int i = 0; i < aggTbl.getAttrCount(); i++) {
				showAttr.addElement(aggTbl.getAttributeId(i));
			}
			recMan.setPopupAddAttrs(showAttr);
		}
	}

	protected void updateStatistics() {
		if (sData.aggTbl == null || sData.moveAggregator == null || sData.firstUpdatableColIdx < 0)
			return;
		String aNames[] = sData.moveAggregator.getAttrNames();
		if (aNames.length <= 1)
			return;
		DataSourceSpec spec = (DataSourceSpec) sData.aggTbl.getDataSource();
		if (spec == null || spec.descriptors == null)
			return;
		LinkDataDescription ldd = null;
		for (int i = 0; i < spec.descriptors.size() && ldd == null; i++)
			if (spec.descriptors.elementAt(i) instanceof LinkDataDescription) {
				ldd = (LinkDataDescription) spec.descriptors.elementAt(i);
			}
		if (ldd == null || ldd.souColIdx < 0 || ldd.destColIdx < 0)
			return;
		for (int i = 0; i < sData.aggTbl.getDataItemCount(); i++) {
			DataRecord rec = sData.aggTbl.getDataRecord(i);
			String souId = rec.getAttrValueAsString(ldd.souColIdx), destId = rec.getAttrValueAsString(ldd.destColIdx);
			if (souId == null || destId == null) {
				continue;
			}
			int sIdx = sData.moveAggregator.getSrcIdx(souId), dIdx = sData.moveAggregator.getDestIdx(destId);
			if (sIdx >= 0 && dIdx >= 0) {
				for (int j = 1; j < aNames.length; j++) {
					Object val = sData.moveAggregator.getMatrixValue(j, sIdx, dIdx);
					if (val != null && (val instanceof Integer)) {
						rec.setNumericAttrValue(((Integer) val).intValue(), val.toString(), sData.firstUpdatableColIdx + j - 1);
					}
				}
			}
		}
		Vector attr = new Vector(aNames.length - 1);
		for (int j = 1; j < aNames.length; j++) {
			attr.addElement(sData.aggTbl.getAttributeId(sData.firstUpdatableColIdx + j - 1));
		}
		sData.aggTbl.notifyPropertyChange("values", null, attr);
	}

	@Override
	public void propertyChange(PropertyChangeEvent e) {
		if (sData == null)
			return;
		if (e.getSource().equals(sData.moveAggregator))
			if ((e.getPropertyName().equals("destroyed") || e.getPropertyName().equals("destroy"))) {
				sData.moveAggregator.removePropertyChangeListener(this);
			} else if (e.getPropertyName().equals("data")) {
				updateStatistics();
			}
	}

}
