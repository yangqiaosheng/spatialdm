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
import spade.time.TimeReference;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.dataview.ShowRecManager;
import spade.vis.dmap.DAggregateLinkLayer;
import spade.vis.dmap.DAggregateLinkObject;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;
import spade.vis.dmap.DLinkObject;
import spade.vis.dmap.DrawingParameters;
import spade.vis.geometry.Geometry;
import spade.vis.space.GeoLayer;
import spade.vis.space.LayerManager;
import spade.vis.spec.DataSourceSpec;
import spade.vis.spec.LinkDataDescription;
import data_load.DataManager;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: 22-Aug-2007
 * Time: 12:21:16
 * Provides a set of tools to summarise and visualise generalised movement data
 * (vectors).
 */
public class MovesSummariser implements DataAnalyser {

	protected ESDACore core = null;

	/**
	 * Returns true when the tool has everything necessary for its operation.
	 * For example, if the tool manages a number of analysis methods, it should
	 * check whether the class for at least one method is available.
	 * Always returns true.
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
		//Find instances of DGeoLayer containing movement vectors
		LayerManager lman = core.getUI().getCurrentMapViewer().getLayerManager();
		Vector moveLayers = new Vector(lman.getLayerCount(), 1);
		Vector ldds = new Vector(lman.getLayerCount(), 1);
		for (int i = 0; i < lman.getLayerCount(); i++) {
			GeoLayer layer = lman.getGeoLayer(i);
			if (layer.getType() == Geometry.line && layer.getThematicData() != null && (layer.getThematicData() instanceof DataTable)) {
				LinkDataDescription ldd = null;
				DataSourceSpec spec = (DataSourceSpec) layer.getDataSource();
				if (spec != null && spec.descriptors != null) {
					for (int j = 0; j < spec.descriptors.size() && ldd == null; j++)
						if (spec.descriptors.elementAt(j) instanceof LinkDataDescription) {
							ldd = (LinkDataDescription) spec.descriptors.elementAt(j);
						}
				}
				if (ldd != null) {
					moveLayers.addElement(layer);
					ldds.addElement(ldd);
				} else {
					DataTable table = (DataTable) layer.getThematicData();
					boolean hasTimeRefs = false;
					for (int j = 0; j < table.getDataItemCount() && !hasTimeRefs; j++) {
						TimeReference tref = table.getDataRecord(j).getTimeReference();
						if (tref == null) {
							continue;
						}
						hasTimeRefs = tref.getValidFrom() != null && tref.getValidUntil() != null && tref.getValidFrom().compareTo(tref.getValidUntil()) < 0;
					}
					if (hasTimeRefs) {
						moveLayers.addElement(layer);
						ldds.addElement(null);
					}
				}
			}
		}
		if (moveLayers.size() < 1) {
			showMessage("No layers with moves (vectors) found!", true);
			return;
		}
		Panel p = new Panel(new ColumnLayout());
		p.add(new Label("Select the map layer (+ table) with moves (vectors):"));
		List list = new List(Math.max(moveLayers.size() + 1, 5));
		for (int i = 0; i < moveLayers.size(); i++) {
			list.add(((DGeoLayer) moveLayers.elementAt(i)).getName());
		}
		list.select(0);
		p.add(list);
		OKDialog dia = new OKDialog(core.getUI().getMainFrame(), "Summarise moves", true);
		dia.addContent(p);
		dia.show();
		if (dia == null || dia.wasCancelled()) {
			dia = null;
			return;
		}
		int idx = list.getSelectedIndex();
		if (idx < 0)
			return;
		DGeoLayer moveLayer = (DGeoLayer) moveLayers.elementAt(idx);
		LinkDataDescription ldd = (LinkDataDescription) ldds.elementAt(idx);
		boolean moveLayerHasTimes = false;
		//
		//find the layer containing the sources and the destinations
		int sdLayerIdx = -1;
		if (ldd != null) {
			lman.getIndexOfLayer(ldd.layerRef);
		}
		if (sdLayerIdx < 0) {
			for (int i = 0; i < moveLayer.getObjectCount() && sdLayerIdx < 0; i++)
				if (moveLayer.getObject(i) instanceof DLinkObject) {
					DLinkObject link = (DLinkObject) moveLayer.getObject(i);
					DGeoObject place = link.getStartNode();
					if (place == null) {
						place = link.getEndNode();
					}
					if (place == null) {
						continue;
					}
					for (int j = 0; j < lman.getLayerCount() && sdLayerIdx < 0; j++)
						if (place.equals(lman.getGeoLayer(j).findObjectById(place.getIdentifier()))) {
							sdLayerIdx = j;
						}
				}
		}
		if (sdLayerIdx < 0) {
			showMessage("Failed to find the layer with the places (sources and destinations)!", true);
			return;
		}
		DGeoLayer placeLayer = (DGeoLayer) lman.getGeoLayer(sdLayerIdx);
		boolean placesHaveNames = false;
		for (int i = 0; i < placeLayer.getObjectCount() && !placesHaveNames; i++) {
			DGeoObject obj = placeLayer.getObject(i);
			placesHaveNames = obj.getName() != null;
		}
		Vector aggLinks = new Vector(placeLayer.getObjectCount() * 5, 100);
		for (int i = 0; i < moveLayer.getObjectCount(); i++)
			if (moveLayer.getObject(i) instanceof DLinkObject) {
				DLinkObject link = (DLinkObject) moveLayer.getObject(i);
				DGeoObject start = link.getStartNode(), end = link.getEndNode();
				if (start == null || end == null) {
					continue;
				}
				moveLayerHasTimes = moveLayerHasTimes || (link.getStartTime() != null && link.getEndTime() != null);
				DAggregateLinkObject aggLink = null;
				for (int k = 0; k < aggLinks.size() && aggLink == null; k++) {
					aggLink = (DAggregateLinkObject) aggLinks.elementAt(k);
					if (!aggLink.startNode.getIdentifier().equals(start.getIdentifier()) || !aggLink.endNode.getIdentifier().equals(end.getIdentifier())) {
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
		aggTbl.setName(moveLayer.getName() + " (aggregated)");
		aggTbl.addAttribute("Start ID", "startId", AttributeTypes.character);
		int startIdIdx = aggTbl.getAttrCount() - 1;
		aggTbl.addAttribute("End ID", "endId", AttributeTypes.character);
		int endIdIdx = aggTbl.getAttrCount() - 1;
		int startNameIdx = -1, endNameIdx = -1;
		if (placesHaveNames) {
			aggTbl.addAttribute("Start name", "startName", AttributeTypes.character);
			startNameIdx = aggTbl.getAttrCount() - 1;
			aggTbl.addAttribute("End name", "endName", AttributeTypes.character);
			endNameIdx = aggTbl.getAttrCount() - 1;
		}
		aggTbl.addAttribute("Direction", "direction", AttributeTypes.character);
		int dirIdx = aggTbl.getAttrCount() - 1;
		aggTbl.addAttribute("N of moves", "n_moves", AttributeTypes.integer);
		int nMovesIdx = aggTbl.getAttrCount() - 1;
		int minDurIdx = -1, maxDurIdx = -1;
		if (moveLayerHasTimes) {
			aggTbl.addAttribute("Min move duration", "min_dur", AttributeTypes.integer);
			minDurIdx = aggTbl.getAttrCount() - 1;
			aggTbl.addAttribute("Max move duration", "max_dur", AttributeTypes.integer);
			maxDurIdx = aggTbl.getAttrCount() - 1;
		}
		aggTbl.addAttribute("N of different moves", "n_traj", AttributeTypes.integer);
		int nTrajIdx = aggTbl.getAttrCount() - 1;
		aggTbl.addAttribute("IDs of moves", "trIds", AttributeTypes.character);
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
		for (int i = 0; i < aggLinks.size(); i++) {
			DAggregateLinkObject lobj = (DAggregateLinkObject) aggLinks.elementAt(i);
			DataRecord rec = new DataRecord(lobj.getIdentifier());
			aggTbl.addDataRecord(rec);
			rec.setAttrValue(lobj.startNode.getIdentifier(), startIdIdx);
			rec.setAttrValue(lobj.endNode.getIdentifier(), endIdIdx);
			if (placesHaveNames) {
				String startName = lobj.startNode.getName();
				if (startName != null) {
					rec.setAttrValue(startName, startNameIdx);
				}
				String endName = lobj.endNode.getName();
				if (endName != null) {
					rec.setAttrValue(endName, endNameIdx);
				}
				if (startName != null && endName != null) {
					rec.setName("Flow from " + startName + " to " + endName);
				}
			}
			rec.setAttrValue(lobj.getLinkDirection(), dirIdx);
			int nLinks = lobj.souLinks.size();
			rec.setNumericAttrValue(nLinks, String.valueOf(nLinks), nMovesIdx);
			if (minDurIdx >= 0 && maxDurIdx >= 0) {
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
			}
			Vector trIds = new Vector(nLinks, 1);
			String trIdsStr = "";
			for (int j = 0; j < nLinks; j++) {
				DLinkObject link = (DLinkObject) lobj.souLinks.elementAt(j);
				String trId = link.getIdentifier();
				if (!trIds.contains(trId)) {
					trIds.addElement(trId);
					if (trIds.size() > 1) {
						trIdsStr += ";";
					}
					trIdsStr += trId;
				}
			}
			rec.setNumericAttrValue(trIds.size(), String.valueOf(trIds.size()), nTrajIdx);
			rec.setAttrValue(trIdsStr, trIdsIdx);
			lobj.setThematicData(rec);
		}
		DataLoader dataLoader = core.getDataLoader();
		int aggTblN = dataLoader.addTable(aggTbl);
		DAggregateLinkLayer aggLinkLayer = new DAggregateLinkLayer();
		aggLinkLayer.setType(Geometry.line);
		aggLinkLayer.setName(aggTbl.getName());
		aggLinkLayer.setGeographic(moveLayer.isGeographic());
		aggLinkLayer.setGeoObjects(aggLinks, true);
		aggLinkLayer.setHasMovingObjects(true);
		aggLinkLayer.setTrajectoryLayer(moveLayer);
		aggLinkLayer.setPlaceLayer(placeLayer);
		DrawingParameters dp1 = aggLinkLayer.getDrawingParameters();
		if (dp1 == null) {
			dp1 = new DrawingParameters();
			aggLinkLayer.setDrawingParameters(dp1);
		}
		dp1.lineColor = Color.getHSBColor((float) Math.random(), (float) Math.random(), (float) Math.random());
		spec.drawParm = dp1;
		dataLoader.addMapLayer(aggLinkLayer, -1);
		aggTbl.setEntitySetIdentifier(aggLinkLayer.getEntitySetIdentifier());
		dataLoader.setLink(aggLinkLayer, aggTblN);
		aggLinkLayer.setLinkedToTable(true);
		aggLinkLayer.countActiveLinks();

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
	}

	protected void showMessage(String msg, boolean error) {
		if (core != null && core.getUI() != null) {
			core.getUI().showMessage(msg, error);
		} else if (error) {
			System.out.println("!--> " + msg);
		}
	}

}
