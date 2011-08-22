package configstart;

import java.awt.Component;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.classification.TableClassifier;
import spade.analysis.plot.Plot;
import spade.analysis.system.DataKeeper;
import spade.analysis.system.DisplayProducer;
import spade.analysis.system.Supervisor;
import spade.analysis.system.SystemUI;
import spade.analysis.system.ToolReCreator;
import spade.analysis.transform.AttributeTransformer;
import spade.analysis.transform.TransformRestorer;
import spade.lib.lang.Language;
import spade.vis.database.AttributeDataPortion;
import spade.vis.map.MapViewer;
import spade.vis.mapvis.DataPresenter;
import spade.vis.mapvis.SimpleDataMapper;
import spade.vis.mapvis.Visualizer;
import spade.vis.space.GeoLayer;
import spade.vis.spec.RestorableTool;
import spade.vis.spec.ToolSpec;

/**
* Builds map visualization and other displays at the system startup, according
* to an externally provided specification.
*/
public class VisInitiator implements ToolReCreator {
	static ResourceBundle res = Language.getTextResource("configstart.Res");

	/**
	* Replies whether a VisInitiator can use the given specification for
	* re-constructing the corresponding tool. The specification must be an
	* instance of the class spade.vis.spec.ToolSpec.
	*/
	@Override
	public boolean canFulfillSpecification(Object spec) {
		if (spec == null || !(spec instanceof ToolSpec))
			return false;
		ToolSpec tsp = (ToolSpec) spec;
		if (tsp.tagName == null)
			return false;
		return tsp.tagName.equalsIgnoreCase("map") || tsp.tagName.equalsIgnoreCase("chart") || tsp.tagName.equalsIgnoreCase("query") || tsp.tagName.equalsIgnoreCase("tool");
	}

	/**
	* On the basis of the given specification, re-constructs the corresponding
	* tool. The specification must be an instance of the class
	* spade.vis.spec.ToolSpec.
	*/
	@Override
	public void fulfillSpecification(Object spec, DataKeeper dKeeper, Supervisor supervisor, Object visManager, boolean makeMapManipulator) {
		if (spec == null || !(spec instanceof ToolSpec))
			return;
		if (dKeeper == null || visManager == null)
			return;
		if (!(visManager instanceof DisplayProducer))
			return;
		DisplayProducer dprod = (DisplayProducer) visManager;
		ToolSpec tsp = (ToolSpec) spec;
		if (tsp.tagName.equals("map")) {
			showDataOnMap(tsp, dKeeper, dprod, makeMapManipulator);
		} else {
			int toolsBefore = supervisor.getSaveableToolCount(); // IDbad - a really wicked way to set identifiers...
			applyTool(tsp, dKeeper, dprod);
			Object lastTool = null;
			for (int i = toolsBefore; i < supervisor.getSaveableToolCount(); i++) {
				lastTool = supervisor.getSaveableTools().elementAt(i);
				if (lastTool instanceof Plot && tsp.chartId != null) {
					((Plot) lastTool).setIdentifier(tsp.chartId);
				}
			}
		}
	}

	/**
	* Visualizes the given attributes on the map by the specified visualization
	* method
	*/
	public static void showDataOnMap(ToolSpec vsp, DataKeeper dKeeper, DisplayProducer dprod) {
		showDataOnMap(vsp, dKeeper, dprod, true);
	}

	public static void showDataOnMap(ToolSpec vsp, DataKeeper dKeeper, DisplayProducer dprod, boolean makeMapManipulator) {
		if (vsp == null || dKeeper == null || dprod == null)
			return;
		SystemUI ui = dprod.getUI();
		if (ui == null) {
			System.out.println("ERROR: " + res.getString("no_ui"));
			return;
		}
		AttributeDataPortion dTable = null;
		int tableN = dKeeper.getTableIndex(vsp.table);
		if (tableN >= 0) {
			dTable = dKeeper.getTable(tableN);
		}
		if (dTable == null) {
			ui.showMessage(res.getString("table_not_found") + ": " + vsp.table + "!", true);
			return;
		}
		int mapN = dKeeper.getTableMapN(dTable);
		if (mapN < 0) {
			ui.showMessage(res.getString("Failed_find_map") + " " + vsp.table + "!", true);
			return;
		}
		if (ui.getMapViewer(mapN) == null) {
			ui.openMapView(mapN);
		}
		MapViewer mapView = (vsp.location == null) ? ui.getMapViewer(mapN) : ui.getMapViewer(vsp.location);
		if (mapView == null) {
			ui.showMessage(res.getString("no_map_view"), true);
			return;
		}

		GeoLayer layer = dKeeper.getTableLayer(dTable, mapView.getLayerManager());
		if (layer == null) {
			ui.showMessage(res.getString("Failed_find_layer") + " " + vsp.table + "!", true);
			return;
		}
		boolean freshLoaded = false;
		if (layer.getObjectCount() < 1) {
			ui.showMessage(res.getString("The_layer") + " " + layer.getName() + " " + res.getString("has_no_objects_"), false);
			if (!layer.loadGeoObjects()) {
				ui.showMessage(res.getString("Failed_load_layer") + " " + layer.getName() + "!", true);
				return;
			}
		}
		if (!dTable.hasData()) {
			ui.showMessage(res.getString("The_table") + " " + vsp.table + " " + res.getString("has_no_data_"), false);
			if (!dTable.loadData()) {
				ui.showMessage(res.getString("Failed_load_table") + " " + vsp.table + "!", true);
				return;
			}
			freshLoaded = true;
		}
		if (freshLoaded || !layer.hasThematicData(dTable)) {
			dKeeper.linkTableToMapLayer(tableN, layer);
		}

		SimpleDataMapper mapper = dprod.getDataMapper();
		Object vis = mapper.constructVisualizer(vsp.methodId, layer.getType());
		if (vis == null) {
			ui.showMessage(res.getString("failed_make_visualizer") + ": " + ((mapper.getErrorMessage() != null) ? mapper.getErrorMessage() : vsp.methodId), true);
			return;
		}
		Visualizer visualizer = dprod.displayOnMap(vis, vsp.methodId, dTable, (Vector) vsp.attributes.clone(), layer, false, mapView);
		if (visualizer == null) {
			ui.showMessage(res.getString("failed_make_visualizer") + ": " + ((dprod.getErrorMessage() != null) ? dprod.getErrorMessage() : vsp.methodId), true);
			return;
		}
		boolean needNotify = false;
		if (vsp.transformSeqSpec != null) {
			AttributeTransformer trans = TransformRestorer.restoreTransformerSequence(vsp.transformSeqSpec);
			if (trans != null) {
				trans.setDataTable(dTable);
				trans.setAttributes(vsp.attributes);
				if (trans.isValid()) {
					if (vis instanceof DataPresenter) {
						((DataPresenter) vis).setAttributeTransformer(trans, true);
					} else if (vis instanceof TableClassifier) {
						((TableClassifier) vis).setAttributeTransformer(trans, true);
					}
					trans.doTransformation();
					needNotify = true;
				} else {
					trans = null;
				}
			}
		}
		if (vsp.properties != null) {
			visualizer.setProperties(vsp.properties);
			needNotify = true;
		}
		if (dprod.getSupervisor() != null) {
			dprod.getSupervisor().registerTool(visualizer);
		}
		if (needNotify) {
			visualizer.notifyVisChange();
		}
		if (makeMapManipulator) {
			dprod.makeMapManipulator(visualizer, vsp.methodId, dTable, layer, mapView);
		}
		mapView.getLayerManager().activateLayer(layer.getContainerIdentifier());
	}

	/**
	* Applies the specified tool to the given data.
	*/
	public static void applyTool(ToolSpec psp, DataKeeper dKeeper, DisplayProducer displayProducer) {
		if (psp == null || dKeeper == null || displayProducer == null)
			return;
		SystemUI ui = displayProducer.getUI();
		if (ui == null) {
			System.out.println("ERROR: " + res.getString("no_ui"));
			return;
		}
		if (!displayProducer.isToolAvailable(psp.methodId)) {
			ui.showMessage(res.getString("Unknown_tool") + ": " + psp.methodId, true);
			return;
		}
		int tableN = dKeeper.getTableIndex(psp.table);
		AttributeDataPortion dTable = null;
		if (tableN >= 0) {
			dTable = dKeeper.getTable(tableN);
		}
		if (dTable == null) {
			ui.showMessage(res.getString("table_not_found") + ": " + psp.table + "!", true);
			return;
		}
		int mapN = dKeeper.getTableMapN(dTable);
		if (mapN >= 0 && ui.getMapViewer(mapN) == null) {
			ui.openMapView(mapN);
		}
		MapViewer mapView = null;
		if (mapN >= 0) {
			mapView = ui.getMapViewer(mapN);
		}
		GeoLayer layer = null;
		if (mapView != null) {
			layer = dKeeper.getTableLayer(dTable, mapView.getLayerManager());
			if (layer != null && layer.getObjectCount() < 1) {
				layer.loadGeoObjects();
			}
		}
		if (!dTable.hasData()) {
			ui.showMessage(res.getString("The_table") + " " + psp.table + " " + res.getString("has_no_data_"), false);
			if (!dTable.loadData()) {
				ui.showMessage(res.getString("Failed_load_table") + " " + psp.table + "!", true);
				return;
			}
			if (layer != null) {
				layer.receiveThematicData(dTable);
				layer.setThematicFilter(dTable.getObjectFilter());
			}
		}
		if (layer != null && !layer.hasThematicData(dTable)) {
			dKeeper.linkTableToMapLayer(tableN, layer);
		}
		Object tool = null;
		if (psp.location != null && psp.location.equalsIgnoreCase("main_frame")) {
			Component comp = displayProducer.makeDisplay(psp, dTable);
			if (comp == null) {
				ui.showMessage(res.getString("Failed_make_display") + psp.methodId + ": " + displayProducer.getErrorMessage(), true);
				return;
			}
			displayProducer.getUI().placeComponent(comp);
			tool = comp;
		} else {
			tool = displayProducer.applyTool(psp, dTable, layer.getContainerIdentifier());
		}
		if (tool instanceof RestorableTool) {
			RestorableTool rtool = (RestorableTool) tool;
			rtool.applySpecification(psp);
		}
	}
}
