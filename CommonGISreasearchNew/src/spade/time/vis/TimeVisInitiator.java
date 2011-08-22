package spade.time.vis;

import java.awt.Component;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.classification.TableClassifier;
import spade.analysis.system.DataKeeper;
import spade.analysis.system.DisplayProducer;
import spade.analysis.system.Supervisor;
import spade.analysis.system.SystemUI;
import spade.analysis.system.ToolReCreator;
import spade.analysis.transform.AttributeTransformer;
import spade.analysis.transform.TransformRestorer;
import spade.lib.lang.Language;
import spade.time.FocusInterval;
import spade.time.transform.TimeAttrTransformer;
import spade.time.ui.TimeUI;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.DataTreater;
import spade.vis.database.Parameter;
import spade.vis.map.MapViewer;
import spade.vis.mapvis.DataPresenter;
import spade.vis.mapvis.SimpleDataMapper;
import spade.vis.mapvis.Visualizer;
import spade.vis.space.GeoLayer;
import spade.vis.spec.AnimatedVisSpec;
import spade.vis.spec.AnimationAttrSpec;
import spade.vis.spec.TemporalToolSpec;
import spade.vis.spec.ToolSpec;

/**
* Builds visualizations of time-variant data on the basis of given specifications.
*/
public class TimeVisInitiator implements ToolReCreator {
	static ResourceBundle res = Language.getTextResource("spade.time.vis.Res");

	/**
	* Replies whether the given specification can be used for constructing a
	* corresponding visualization of time-variable data.
	*/
	@Override
	public boolean canFulfillSpecification(Object spec) {
		if (spec == null)
			return false;
		if (spec instanceof AnimatedVisSpec)
			return true;
		if (spec instanceof TemporalToolSpec)
			return true;
		return false;
	}

	/**
	* On the basis of the given specification, constructs  a corresponding
	* visualization of time-variable data.
	* The argument @arg visManager is a component used for creating visual data
	* displays and cartographic visualizers. This may be either a DisplayProducer
	* or a SimpleDataMapper, depending on the configuration.
	*/
	@Override
	public void fulfillSpecification(Object spec, DataKeeper dKeeper, Supervisor supervisor, Object visManager, boolean makeMapManipulator) {
		if (spec == null)
			return;
		if (dKeeper == null || visManager == null)
			return;
		SystemUI ui = null;
		if (supervisor != null) {
			ui = supervisor.getUI();
		}
		if (ui == null) {
			System.out.println("ERROR: " + res.getString("no_ui"));
			return;
		}
		ToolSpec tsp = null;
		AnimatedVisSpec avsp = null;
		if (spec instanceof AnimatedVisSpec) {
			avsp = (AnimatedVisSpec) spec;
			tsp = avsp.visSpec;
		}
		TemporalToolSpec ttsp = null;
		if (spec instanceof TemporalToolSpec) {
			ttsp = (TemporalToolSpec) spec;
			tsp = ttsp;
		}
		if (avsp == null && ttsp == null)
			return;
		if (avsp != null) {
			if (avsp.table == null || avsp.attrSpecs == null || avsp.attrSpecs.size() < 1 || avsp.visSpec == null || avsp.visSpec.methodId == null || avsp.visSpec.attributes == null || avsp.visSpec.attributes.size() < 1)
				return;
		} else if (ttsp.table == null || ttsp.attrSpecs == null || ttsp.attrSpecs.size() < 1 || ttsp.methodId == null)
			return;
		boolean buildMap = avsp != null || ttsp.methodId.equalsIgnoreCase("value_flow");
		SimpleDataMapper mapper = null;
		if (buildMap) {
			if (visManager instanceof DisplayProducer) {
				mapper = ((DisplayProducer) visManager).getDataMapper();
			} else if (visManager instanceof SimpleDataMapper) {
				mapper = (SimpleDataMapper) visManager;
			}
			if (mapper == null) {
				showMessage(ui, res.getString("no_data_mapper"), true);
				return;
			}
		} else if (!(visManager instanceof DisplayProducer)) {
			showMessage(ui, res.getString("no_display_producer"), true);
			return;
		}
		//find the table
		AttributeDataPortion table = null;
		String tblId = (avsp != null) ? avsp.table : ttsp.table;
		int tableN = dKeeper.getTableIndex(tblId);
		if (tableN >= 0) {
			table = dKeeper.getTable(tableN);
		}
		if (table == null) {
			showMessage(ui, res.getString("table_not_found") + ": " + tblId + "!", true);
			return;
		}
		if (!table.hasData()) {
			showMessage(ui, res.getString("The_table") + " " + tblId + " " + res.getString("has_no_data_"), false);
			if (!table.loadData()) {
				showMessage(ui, res.getString("Failed_load_table") + " " + tblId + "!", true);
				return;
			}
		}
		Parameter tpar = null;
		for (int i = 0; i < table.getParamCount() && tpar == null; i++)
			if (table.getParameter(i).isTemporal() && table.getParameter(i).getValueCount() > 1) {
				tpar = table.getParameter(i);
			}
		if (tpar == null) {
			showMessage(ui, res.getString("no_temp_param"), true);
			return;
		}
		Vector attrSpecs = (avsp != null) ? avsp.attrSpecs : ttsp.attrSpecs;
		Vector aDescr = new Vector(attrSpecs.size(), 1);
		for (int i = 0; i < attrSpecs.size(); i++) {
			AnimationAttrSpec aasp = (AnimationAttrSpec) attrSpecs.elementAt(i);
			if (aasp.attribute == null && aasp.parent == null) {
				continue;
			}
			VisAttrDescriptor ad = new VisAttrDescriptor();
			if (aasp.attribute != null) {
				ad.attr = table.getAttribute(aasp.attribute);
				if (ad.attr == null) {
					showMessage(ui, res.getString("attr_not_found") + ": " + aasp.attribute + "!", true);
				}
			}
			if (aasp.parent != null) {
				ad.parent = table.getAttribute(aasp.parent);
				if (ad.parent == null) {
					showMessage(ui, res.getString("attr_not_found") + ": " + aasp.parent + "!", true);
				}
			}
			if (ad.attr == null && ad.parent == null) {
				continue;
			}
			if (ad.attr != null) {
				ad.attrId = ad.attr.getIdentifier();
			}
			ad.isTimeDependent = aasp.isTimeDependent;
			ad.offset = aasp.offset;
			if (aasp.fixedParams != null && aasp.fixedParams.size() > 0) {
				ad.fixedParams = new Vector(aasp.fixedParams.size(), 1);
				ad.fixedParamVals = new Vector(aasp.fixedParams.size(), 1);
				for (int j = 0; j < aasp.fixedParams.size(); j++) {
					String name = (String) aasp.fixedParams.elementAt(j), strVal = (String) aasp.fixedParamVals.elementAt(j);
					Parameter par = table.getParameter(name);
					if (par != null && par.getValueCount() > 0) {
						int valIdx = -1;
						if (par.getFirstValue() instanceof String) {
							valIdx = par.getValueIndex(strVal);
						} else {
							for (int k = 0; k < par.getValueCount() && valIdx < 0; k++)
								if (strVal.equalsIgnoreCase(par.getValue(k).toString())) {
									valIdx = k;
								}
						}
						if (valIdx >= 0) {
							ad.fixedParams.addElement(par.getName());
							ad.fixedParamVals.addElement(par.getValue(valIdx));
						}
					}
				}
				if (ad.fixedParams.size() < 1) {
					ad.fixedParams = null;
					ad.fixedParamVals = null;
				}
			}
			aDescr.addElement(ad);
		}
		if (aDescr.size() < 1)
			return;
		int mapN = -1;
		GeoLayer layer = null;
		MapViewer mapView = null;
		if (buildMap) {
			mapN = dKeeper.getTableMapN(table);
			if (mapN < 0) {
				showMessage(ui, res.getString("Failed_find_map") + " " + tblId + "!", true);
				return;
			}
			if (ui.getMapViewer(mapN) == null) {
				ui.openMapView(mapN);
			}
			String location = (avsp != null) ? avsp.visSpec.location : ttsp.location;
			mapView = (location == null) ? ui.getMapViewer(mapN) : ui.getMapViewer(location);
			if (mapView == null) {
				showMessage(ui, res.getString("no_map_view"), true);
				return;
			}
			layer = dKeeper.getTableLayer(table, mapView.getLayerManager());
			if (layer == null) {
				showMessage(ui, res.getString("Failed_find_layer") + " " + tblId + "!", true);
				return;
			}
			if (layer.getObjectCount() < 1) {
				showMessage(ui, res.getString("The_layer") + " " + layer.getName() + " " + res.getString("has_no_objects_"), false);
				if (!layer.loadGeoObjects()) {
					showMessage(ui, res.getString("Failed_load_layer") + " " + layer.getName() + "!", true);
					return;
				}
			}
			if (!layer.hasThematicData(table)) {
				dKeeper.linkTableToMapLayer(tableN, layer);
			}
		}
		AttributeTransformer trans = null;
		if (tsp != null && tsp.transformSeqSpec != null) {
			trans = TransformRestorer.restoreTransformerSequence(tsp.transformSeqSpec);
			if (trans != null) {
				trans.setDataTable(table);
				if (trans instanceof TimeAttrTransformer) {
					((TimeAttrTransformer) trans).setAttributeDescriptions(aDescr);
				} else {
					trans.setAttributes(tsp.attributes);
				}
				if (!trans.isValid()) {
					trans = null;
				}
			}
		}

		if (avsp != null) {
			Object vis = mapper.constructVisualizer(avsp.visSpec.methodId, layer.getType());
			if (vis == null) {
				showMessage(ui, mapper.getErrorMessage(), true);
				return;
			}
			if (!(vis instanceof DataPresenter) && !(vis instanceof TableClassifier)) {
				showMessage(ui, res.getString("ill_vis_method"), true);
				return;
			}
			Vector selAttrIds = new Vector(aDescr.size(), 1);
			for (int i = 0; i < aDescr.size(); i++) {
				VisAttrDescriptor d = (VisAttrDescriptor) aDescr.elementAt(i);
				selAttrIds.addElement(d.attrId);
			}
			DataPresenter dpres = null;
			TableClassifier tcl = null;
			if (vis instanceof DataPresenter) {
				dpres = (DataPresenter) vis;
				Vector attrNames = new Vector(aDescr.size(), 1);
				for (int i = 0; i < aDescr.size(); i++) {
					VisAttrDescriptor d = (VisAttrDescriptor) aDescr.elementAt(i);
					attrNames.addElement((d.parent != null) ? d.parent.getName() : d.attr.getName());
				}
				dpres.setAttributes(selAttrIds, attrNames);
				if (trans != null) {
					dpres.setAttributeTransformer(trans, true);
				}
			} else {
				tcl = (TableClassifier) vis;
				tcl.setAttributes(selAttrIds);
				if (trans != null) {
					tcl.setAttributeTransformer(trans, true);
				}
			}
			Visualizer pres = mapper.visualizeAttributes(vis, avsp.visSpec.methodId, table, selAttrIds, layer.getType());
			if (pres == null) {
				showMessage(ui, res.getString("failed_make_visualizer") + ": " + ((mapper.getErrorMessage() != null) ? mapper.getErrorMessage() : avsp.visSpec.methodId), true);
				return;
			}
			if (trans != null) {
				trans.doTransformation();
			}
			displayOnMap(pres, avsp.visSpec.methodId, table, layer, supervisor, mapView);
			mapView.getLayerManager().activateLayer(layer.getContainerIdentifier());
			DataVisAnimator anim = null;
			if (ui != null && ui.getTimeUI() != null) {
				anim = new DataVisAnimator();
				if (!anim.setup(table, aDescr, tpar, vis)) {
					anim = null;
				}
			}
			if (anim != null) {
				anim.setWrapVisualizer(pres);
			}
			if (avsp.visSpec.properties != null) {
				pres.setProperties(avsp.visSpec.properties);
			}
			if (makeMapManipulator) {
				makeMapManipulator(pres, avsp.visSpec.methodId, table, layer, supervisor, mapView, mapper);
			}
			if (anim != null) {
				anim.setWrapVisualizer(pres);
				supervisor.registerTool(anim);
				TimeUI timeUI = (TimeUI) ui.getTimeUI();
				FocusInterval fint = timeUI.getFocusInterval(tpar);
				anim.setFocusInterval(fint);
			} else {
				supervisor.registerTool(pres);
			}
		} else if (ttsp.methodId.equalsIgnoreCase("value_flow")) {
			VisAttrDescriptor ad = (VisAttrDescriptor) aDescr.elementAt(0);
			ValueFlowVisualizer vis = new ValueFlowVisualizer();
			vis.setDataSource(table);
			vis.setAttribute((ad.parent != null) ? ad.parent : ad.attr);
			vis.setTemporalParameter(tpar);
			if (ad.fixedParams != null) {
				for (int i = 0; i < ad.fixedParams.size(); i++) {
					vis.setOtherParameterValue((String) ad.fixedParams.elementAt(i), ad.fixedParamVals.elementAt(i));
				}
			}
			if (trans != null) {
				trans.doTransformation();
			}
			if (trans != null) {
				vis.setAttributeTransformer(trans, true);
			}
			vis.setup();
			displayOnMap(vis, ttsp.methodId, table, layer, supervisor, mapView);
			mapView.getLayerManager().activateLayer(layer.getContainerIdentifier());
			if (ttsp.properties != null) {
				vis.setProperties(ttsp.properties);
			}
			if (makeMapManipulator) {
				makeMapManipulator(vis, ttsp.methodId, table, layer, supervisor, mapView, mapper);
			}
			supervisor.registerTool(vis);
		} else if (ttsp.methodId.equalsIgnoreCase("time_graph")) {
			TimeGraph tg[] = new TimeGraph[aDescr.size()];
			TimeGraphSummary tsummary[] = new TimeGraphSummary[aDescr.size()];
			TimeUI timeUI = (TimeUI) ui.getTimeUI();
			FocusInterval fint = timeUI.getFocusInterval(tpar);
			for (int i = 0; i < aDescr.size(); i++) {
				VisAttrDescriptor ad = (VisAttrDescriptor) aDescr.elementAt(i);
				tg[i] = new TimeGraph();
				tg[i].setTable(table);
				tg[i].setSupervisor(supervisor);
				tg[i].setAttribute((ad.parent != null) ? ad.parent : ad.attr);
				tg[i].setTemporalParameter(tpar);
				tg[i].setFocusInterval(fint);
				if (ad.fixedParams != null) {
					for (int j = 0; j < ad.fixedParams.size(); j++) {
						tg[i].setOtherParameterValue((String) ad.fixedParams.elementAt(j), ad.fixedParamVals.elementAt(j));
					}
				}
				if (trans != null) {
					tg[i].setAttributeTransformer(trans);
				}
				tsummary[i] = new TimeGraphSummary();
				tsummary[i].setTable(table);
				tsummary[i].setSupervisor(supervisor);
				tsummary[i].setAttribute(tg[i].getAttribute());
				tsummary[i].setTemporalParameter(tpar);
			}
			TimeGraphPanel tgp = new TimeGraphPanel(supervisor, tg, tsummary);
			tgp.setName(res.getString("time_graph"));
			//tgp.setSupervisor(supervisor);
			tgp.setProperties(ttsp.properties);
			supervisor.registerTool(tgp);
			if (trans != null) {
				trans.doTransformation();
			}
			tgp.setCommonMinMax();
			((DisplayProducer) visManager).showGraph(tgp);
		}
	}

	/**
	* Visualizes the specified attributes on the given map layer using the given
	* visualizer or classifier (previously constructed).
	*/
	static void displayOnMap(Visualizer vis, String methodId, AttributeDataPortion dtab, GeoLayer themLayer, //the layer to send the presentation
			Supervisor sup, MapViewer mapView) {
		if (methodId == null || vis == null || mapView == null)
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
	protected static void makeMapManipulator(Visualizer vis, String methodId, AttributeDataPortion dtab, GeoLayer themLayer, //the layer to manipulate
			Supervisor sup, MapViewer mapView, SimpleDataMapper mapper) {
		if (vis == null || methodId == null || themLayer == null || sup == null)
			return;

		if (mapView == null)
			return;
		Component manipulator = mapper.getMapManipulator(methodId, vis, sup, dtab);
		if (manipulator != null) {
			mapView.addMapManipulator(manipulator, vis, themLayer.getContainerIdentifier());
		}
	}

	protected static void showMessage(SystemUI ui, String msg, boolean error) {
		if (ui != null) {
			ui.showMessage(msg, error);
		} else if (error) {
			System.out.println("ERROR: " + msg);
		} else {
			System.out.println(msg);
		}
	}
}
