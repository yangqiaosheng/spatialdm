package configstart;

import java.awt.Component;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.system.DataKeeper;
import spade.analysis.system.DisplayProducer;
import spade.analysis.system.Supervisor;
import spade.analysis.system.SystemUI;
import spade.analysis.system.ToolReCreator;
import spade.lib.lang.Language;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.DataTreater;
import spade.vis.map.MapViewer;
import spade.vis.mapvis.DataPresenter;
import spade.vis.mapvis.SimpleDataMapper;
import spade.vis.mapvis.Visualizer;
import spade.vis.space.GeoLayer;
import spade.vis.spec.ToolSpec;

/**
* Builds map visualization at the system startup, according to an externally
* provided specification.
*/
public class MapVisInitiator implements ToolReCreator {
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
		return tsp.tagName.equalsIgnoreCase("map");
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
		SimpleDataMapper mapper = null;
		if (visManager instanceof DisplayProducer) {
			mapper = ((DisplayProducer) visManager).getDataMapper();
		} else if (visManager instanceof SimpleDataMapper) {
			mapper = (SimpleDataMapper) visManager;
		}
		if (mapper == null)
			return;
		ToolSpec tsp = (ToolSpec) spec;
		if (tsp.tagName.equals("map")) {
			showDataOnMap(tsp, dKeeper, supervisor, mapper, makeMapManipulator);
		}
	}

	/**
	* Visualizes the given attributes on the map by the specified visualization
	* method. The DataMapper is responsible for constructing the Visualizer and
	* its manipulator.
	*/
	static public void showDataOnMap(ToolSpec vsp, DataKeeper dKeeper, Supervisor supervisor, SimpleDataMapper mapper) {
		showDataOnMap(vsp, dKeeper, supervisor, mapper, true);
	}

	static public void showDataOnMap(ToolSpec vsp, DataKeeper dKeeper, Supervisor supervisor, SimpleDataMapper mapper, boolean makeMapManipulator) {
		if (vsp == null || dKeeper == null || mapper == null || supervisor == null)
			return;
		SystemUI ui = supervisor.getUI();
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
		if (layer.getObjectCount() < 1) {
			ui.showMessage(res.getString("The_layer") + " " + layer.getName() + " " + res.getString("has_no_objects_"), false);
			if (!layer.loadGeoObjects()) {
				ui.showMessage(res.getString("Failed_load_layer") + " " + layer.getName() + "!", true);
				return;
			}
		}
		boolean freshLoaded = false;
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

		Object vis = mapper.constructVisualizer(vsp.methodId, layer.getType());
		if (vis == null) {
			ui.showMessage(res.getString("failed_make_visualizer") + ": " + ((mapper.getErrorMessage() != null) ? mapper.getErrorMessage() : vsp.methodId), true);
			return;
		}
		vis = mapper.visualizeAttributes(vis, vsp.methodId, dTable, (Vector) vsp.attributes.clone(), layer.getType());
		if (vis == null || !(vis instanceof Visualizer)) {
			ui.showMessage(res.getString("failed_make_visualizer") + ": " + ((mapper.getErrorMessage() != null) ? mapper.getErrorMessage() : vsp.methodId), true);
			return;
		}
		Visualizer visualizer = (Visualizer) vis;
		displayOnMap(visualizer, vsp.methodId, dTable, (Vector) vsp.attributes.clone(), layer, makeMapManipulator, supervisor, mapView);
		supervisor.registerTool(visualizer);
		if (vsp.properties != null) {
			visualizer.setProperties(vsp.properties);
			visualizer.notifyVisChange();
		}
		if (makeMapManipulator) {
			makeMapManipulator(visualizer, vsp.methodId, dTable, layer, supervisor, mapView, mapper);
		}
		mapView.getLayerManager().activateLayer(layer.getContainerIdentifier());
	}

	/**
	* Visualizes the specified attributes on the given map layer using the
	* visualizer or classifier specified by its identifier.
	*/
	static public void showDataOnMap(String methodId, AttributeDataPortion dTable, Vector attr, //the attributes to visualize on the map
			GeoLayer layer, //the layer to send the presentation
			Supervisor supervisor, boolean makeMapManipulator) {
		if (methodId == null || dTable == null || attr == null || attr.size() < 1 || layer == null || supervisor == null)
			return;
		SystemUI ui = supervisor.getUI();
		if (ui == null) {
			System.out.println("ERROR: " + res.getString("no_ui"));
			return;
		}
		if (layer.getObjectCount() < 1) {
			ui.showMessage(res.getString("The_layer") + " " + layer.getName() + " " + res.getString("has_no_objects_"), false);
			if (!layer.loadGeoObjects()) {
				ui.showMessage(res.getString("Failed_load_layer") + " " + layer.getName() + "!", true);
				return;
			}
		}
		if (!dTable.hasData()) {
			ui.showMessage(res.getString("The_table") + " " + dTable.getName() + " " + res.getString("has_no_data_"), false);
			if (!dTable.loadData()) {
				ui.showMessage(res.getString("Failed_load_table") + " " + dTable.getName() + "!", true);
				return;
			}
		}
		if (!layer.hasThematicData(dTable)) {
			layer.receiveThematicData(dTable);
			layer.setThematicFilter(dTable.getObjectFilter());
		}

		MapViewer mapView = supervisor.getUI().getCurrentMapViewer();
		if (mapView == null) {
			ui.showMessage(res.getString("no_map_view"), true);
			return;
		}

		SimpleDataMapper mapper = new SimpleDataMapper();
		Object vis = mapper.constructVisualizer(methodId, layer.getType());
		if (vis != null) {
			vis = mapper.visualizeAttributes(vis, methodId, dTable, attr, layer.getType());
		}
		if (vis != null && (vis instanceof Visualizer)) {
			displayOnMap((Visualizer) vis, methodId, dTable, attr, layer, makeMapManipulator, supervisor, mapView);
			if (makeMapManipulator) {
				makeMapManipulator((Visualizer) vis, methodId, dTable, layer, supervisor, mapView, mapper);
			}
		}
	}

	/**
	* Visualizes the specified attributes on the given map layer using the given
	* visualizer or classifier (previously constructed).
	*/
	static public void displayOnMap(Visualizer vis, String methodId, AttributeDataPortion dtab, Vector attr, //the attributes to visualize on the map
			GeoLayer themLayer, //the layer to send the presentation
			boolean makeManipulator, //whether to create a manipulator
			Supervisor sup, MapViewer mapView) {
		if (methodId == null || vis == null)
			return;
		if (mapView == null)
			return;
		vis.setLocation((mapView.getIsPrimary()) ? "main" : mapView.getIdentifier());
		if (vis instanceof DataPresenter) {
			((DataPresenter) vis).setAttrColorHandler(sup.getAttrColorHandler());
		}
		boolean otherTable = !themLayer.hasThematicData(dtab);
		if (otherTable) {
			themLayer.receiveThematicData(dtab);
			if (dtab.getObjectFilter() != null) {
				themLayer.setThematicFilter(dtab.getObjectFilter());
			}
		}
		//**
		boolean allowBkgVis = sup.getSystemSettings().checkParameterValue("Allow_Background_Visualization", "true");
		Visualizer oldVis = themLayer.getVisualizer(), oldBkgVis = themLayer.getBackgroundVisualizer();
		if (oldVis != null && (!allowBkgVis || otherTable || oldVis.isDiagramPresentation() == vis.isDiagramPresentation())) {
			if (oldVis instanceof DataTreater) {
				sup.removeDataDisplayer((DataTreater) vis);
			}
			mapView.removeMapManipulator(oldVis, themLayer.getContainerIdentifier());
			oldVis.destroy();
			oldVis = null;
		}
		if (oldBkgVis != null && (!allowBkgVis || otherTable || !vis.isDiagramPresentation())) {
			if (oldBkgVis instanceof DataTreater) {
				sup.removeDataDisplayer((DataTreater) oldBkgVis);
			}
			mapView.removeMapManipulator(oldBkgVis, themLayer.getContainerIdentifier());
			oldBkgVis.destroy();
			oldBkgVis = null;
		}
		if (oldVis != null)
			if (!oldVis.isDiagramPresentation()) {
				themLayer.setVisualizer(vis);
				themLayer.setBackgroundVisualizer(oldVis);
			} else {
				themLayer.setBackgroundVisualizer(vis);
			}
		else {
			themLayer.setVisualizer(vis);
		}
		themLayer.setLayerDrawn(true);
		if (vis instanceof DataTreater) {
			sup.registerDataDisplayer((DataTreater) vis);
		}
		sup.getUI().bringMapToTop(mapView);
		sup.notifyGlobalPropertyChange(Supervisor.eventDisplayedAttrs);
		return;
	}

	/**
	* Creates an appropriate manipulator for the given visualizer and adds it to
	* the map window.
	*/
	static public void makeMapManipulator(Visualizer vis, String methodId, AttributeDataPortion dtab, GeoLayer themLayer, //the layer to manipulate
			Supervisor sup, MapViewer mapView, SimpleDataMapper mapper) {
		if (vis == null || methodId == null || themLayer == null || sup == null || mapView == null)
			return;

		Component manipulator = mapper.getMapManipulator(methodId, vis, sup, dtab);
		if (manipulator != null) {
			mapView.addMapManipulator(manipulator, vis, themLayer.getContainerIdentifier());
		}
	}

	/**
	* Visualizes the specified attributes on the given map layer using the
	* visualizer or classifier specified by its identifier.
	*/
	static public void showDataOnMap(String methodId, AttributeDataPortion dTable, Vector attr, //the attributes to visualize on the map
			GeoLayer layer, //the layer to send the presentation
			Supervisor supervisor, Visualizer visualizer) {
		if (visualizer == null || methodId == null || dTable == null || attr == null || attr.size() < 1 || layer == null)
			return;
		SystemUI ui = supervisor.getUI();
		if (ui == null) {
			System.out.println("ERROR: " + res.getString("no_ui"));
			return;
		}
		if (layer.getObjectCount() < 1) {
			ui.showMessage(res.getString("The_layer") + " " + layer.getName() + " " + res.getString("has_no_objects_"), false);
			if (!layer.loadGeoObjects()) {
				ui.showMessage(res.getString("Failed_load_layer") + " " + layer.getName() + "!", true);
				return;
			}
		}
		if (!dTable.hasData()) {
			ui.showMessage(res.getString("The_table") + " " + dTable.getName() + " " + res.getString("has_no_data_"), false);
			if (!dTable.loadData()) {
				ui.showMessage(res.getString("Failed_load_table") + " " + dTable.getName() + "!", true);
				return;
			}
		}
		if (!layer.hasThematicData(dTable)) {
			layer.receiveThematicData(dTable);
			layer.setThematicFilter(dTable.getObjectFilter());
		}

		MapViewer mapView = supervisor.getUI().getCurrentMapViewer();
		if (mapView == null) {
			ui.showMessage(res.getString("no_map_view"), true);
			return;
		}

		SimpleDataMapper mapper = new SimpleDataMapper();
		mapper.visualizeAttributes(visualizer, methodId, dTable, attr, layer.getType());
		displayOnMap(visualizer, methodId, dTable, attr, layer, true, supervisor, mapView);
		makeMapManipulator(visualizer, methodId, dTable, layer, supervisor, mapView, mapper);
	}

}