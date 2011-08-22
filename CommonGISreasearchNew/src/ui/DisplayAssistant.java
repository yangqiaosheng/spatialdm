package ui;

import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Component;
import java.awt.Frame;
import java.awt.Panel;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.system.DataKeeper;
import spade.analysis.system.DisplayProducer;
import spade.analysis.system.Supervisor;
import spade.analysis.system.SystemUI;
import spade.analysis.transform.TransformerGenerator;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.OKDialog;
import spade.lib.basicwin.SelectDialog;
import spade.lib.basicwin.TextCanvas;
import spade.lib.lang.Language;
import spade.lib.util.VectorUtil;
import spade.time.ui.TimeUI;
import spade.vis.database.Attribute;
import spade.vis.database.Parameter;
import spade.vis.geometry.Geometry;
import spade.vis.map.MapViewer;
import spade.vis.mapvis.DataMapper;
import spade.vis.mapvis.Visualizer;
import spade.vis.space.GeoLayer;
import spade.vis.space.LayerManager;

public class DisplayAssistant {
	static ResourceBundle res = Language.getTextResource("ui.Res");
	/**
	* The DataKeeper keeps available tables and maps
	*/
	protected DataKeeper dataKeeper = null;
	/**
	* A component performing generation of thematic maps and other data displays
	*/
	protected DisplayProducer displayProducer = null;
	/**
	* When a display is created, it must be supplied with a reference to the
	* supervisor that links together all the displays
	*/
	protected Supervisor supervisor = null;
	/**
	* Used for selection of a table for data visualization etc.
	*/
	protected TableManager tman = null;
	/**
	* The system UI that can, in particular, provide access to the map viewer
	*/
	protected SystemUI ui = null;

	public DisplayAssistant(DataKeeper keeper, DisplayProducer dp, Supervisor sup, TableManager tman) {
		dataKeeper = keeper;
		displayProducer = dp;
		supervisor = sup;
		this.tman = tman;
		if (supervisor != null) {
			ui = supervisor.getUI();
		}
		runDisplayWizard();
	}

	protected Frame getFrame() {
		if (ui != null && ui.getMainFrame() != null)
			return ui.getMainFrame();
		return CManager.getAnyFrame();
	}

	public void runDisplayWizard() {
		// following string:  "Select the table for visualisation"
		TableData td = tman.selectCurrTable(res.getString("Select_the_table_for3"));
		if (td == null)
			return;
		if (!td.table.hasData()) {
			td.table.loadData();
		}
		if (!td.table.hasData()) {
			if (ui != null) {
				ui.showMessage(res.getString("no_data_in_table"), true);
			}
			return;
		}
		if (ui != null) {
			ui.clearStatusLine();
		}
		boolean timeUIExists = ui != null && ui.getTimeUI() != null;
		Parameter tpar = null; //the temporal parameter;
		if (timeUIExists) {
			tpar = td.table.getTemporalParameter();
		}
		TimeUI timeUI = null;
		if (tpar != null) {
			timeUI = (TimeUI) ui.getTimeUI();
			if (timeUI.getTimeSeriesVisMethodsCount() < 1) {
				timeUIExists = false;
				timeUI = null;
				tpar = null;
			}
		}
		int nMapVisMethods = 0;
		DataMapper dataMapper = null;
		if (displayProducer.getDataMapper() instanceof DataMapper) {
			dataMapper = (DataMapper) displayProducer.getDataMapper();
			nMapVisMethods = dataMapper.getAvailableMethodCount();
		}
		if (nMapVisMethods > 0 && td.themLayer == null) {
			linkTableToMapLayer(td);
		}
		Vector selTopAttr = null; //selected top-level  attributes;
									//instances of spade.vis database.Attribute
		Vector colIds = null; //identifiers of the selected columns (strings).
		AttributeChooser attrChooser = new AttributeChooser();
		attrChooser.setDataKeeper(dataKeeper);
		attrChooser.setSupervisor(supervisor);
		attrChooser.setColumnsMayBeReordered(false);
		boolean backpressed = false;
		int tsVisN = 0;
		boolean timeDep = false;
		String mapMethodId = null, chartMethodId = null;
		Object mapViewSelection = null;
		do {
			backpressed = false;
			selTopAttr = attrChooser.selectTopLevelAttributes(td.table, res.getString("Select_attribute_s_to"), ui);
			if (selTopAttr == null || selTopAttr.size() < 1)
				return;
			//is there a time-dependent attribute among the selected attributes?
			if (tpar != null) {
				for (int i = 0; i < selTopAttr.size() && !timeDep; i++) {
					timeDep = ((Attribute) selTopAttr.elementAt(i)).dependsOnParameter(tpar);
				}
			}
			if (timeDep) {
				boolean timeVisRepeat = false;
				do {
					timeVisRepeat = false;
					//ask the user if he wishes to use special vis. methods for time-series
					Panel p = new Panel(new ColumnLayout());
					TextCanvas tc = new TextCanvas();
					tc.addTextLine(res.getString("ask_use_special_time_vis"));
					p.add(tc);
					Checkbox cb[] = new Checkbox[timeUI.getTimeSeriesVisMethodsCount() + 1];
					CheckboxGroup cbg = new CheckboxGroup();
					for (int i = 0; i < cb.length; i++) {
						boolean last = i == cb.length - 1;
						cb[i] = new Checkbox((last) ? res.getString("other_vis_methods") : timeUI.getTimeSeriesVisMethodName(i), i == tsVisN, cbg);
						if (last) {
							p.add(new Line(false));
						}
						p.add(cb[i]);
					}
					OKDialog okd = new OKDialog(getFrame(), res.getString("special_time_vis"), true, true);
					okd.addContent(p);
					okd.show();
					if (okd.wasCancelled())
						return;
					backpressed = okd.wasBackPressed();
					if (!backpressed)
						if (!cb[cb.length - 1].getState()) {
							//some specific visualisation method for time-series has been selected
							String visMethodId = null;
							for (int i = 0; i < cb.length - 1 && visMethodId == null; i++)
								if (cb[i].getState()) {
									visMethodId = timeUI.getTimeSeriesVisMethodId(i);
									tsVisN = i;
								}
							int result = timeUI.visualizeTable(td.table, attrChooser, visMethodId);
							timeVisRepeat = result < 0;
							if (!timeVisRepeat)
								return;
						}
				} while (timeVisRepeat);
			}
			if (!backpressed) {
				//is there a parameter-dependent attribute among the selected attributes?
				boolean parDep = timeDep;
				for (int i = 0; i < selTopAttr.size() && !parDep; i++) {
					parDep = ((Attribute) selTopAttr.elementAt(i)).getChildrenCount() > 1;
				}
				do {
					if (parDep) {
						Vector attrDescr = attrChooser.selectParamValues(null);
						if (attrDescr == null) {
							backpressed = attrChooser.backButtonPressed();
							if (backpressed) {
								break;
							} else
								return;
						}
					} else {
						attrChooser.getAttrDescriptors();
					}
					ChooseVisMethodPanel vmPan = new ChooseVisMethodPanel(td, attrChooser, displayProducer, dataMapper, supervisor);
					OKDialog dlg = new OKDialog(getFrame(), res.getString("Select_display_type"), true, true);
					dlg.addContent(vmPan);
					dlg.show();
					if (dlg.wasCancelled())
						return;
					backpressed = dlg.wasBackPressed();
					if (!backpressed) {
						mapMethodId = vmPan.getMapVisMethodId();
						chartMethodId = vmPan.getChartVisMethodId();
						colIds = vmPan.getReorderedColumnIds();
						mapViewSelection = vmPan.getMapViewSelection();
					}
				} while (backpressed && parDep);
			}
		} while (backpressed);
		if (mapMethodId == null && chartMethodId == null)
			return;
		if (td.themLayer != null && supervisor != null) {
			for (int i = 0; i < supervisor.getSaveableToolCount(); i++)
				if (supervisor.getSaveableTool(i) instanceof SimpleMapView) {
					SimpleMapView map = (SimpleMapView) supervisor.getSaveableTool(i);
					LayerManager lman = map.getLayerManager();
					lman.activateLayer(td.themLayer.getContainerIdentifier());
				}
		}
		if (mapMethodId != null) {
			Object vis = dataMapper.constructVisualizer(mapMethodId, td.themLayer.getType());
			if (vis == null) {
				ui.showMessage(dataMapper.getErrorMessage(), true);
			} else {
				TransformerGenerator.makeTransformerChain(vis, td.table, colIds, timeDep);
				boolean useMainView = false;
				MapViewer mapView = null;
				if (mapViewSelection == null) {
					mapViewSelection = "auto";
				}
				if (mapViewSelection instanceof MapViewer) {
					mapView = (MapViewer) mapViewSelection;
				} else {
					String mLoc = (String) mapViewSelection;
					if (mLoc.equals("main")) {
						mapView = ui.getMapViewer("main");
						useMainView = true;
					} else if (mLoc.equals("new")) {
						mapView = ui.getMapViewer("_blank_");
					} else {
						Visualizer lvis = td.themLayer.getVisualizer(), lbvis = td.themLayer.getBackgroundVisualizer();
						boolean layerHasVisualizer = lvis != null || lbvis != null;
						if (layerHasVisualizer && td.themLayer.getType() == Geometry.area && (lvis == null || lbvis == null))
							if ((vis instanceof Visualizer) && ((Visualizer) vis).isDiagramPresentation()) {
								layerHasVisualizer = lvis != null && lvis.isDiagramPresentation();
							} else {
								layerHasVisualizer = lvis == null || !lvis.isDiagramPresentation();
							}
						useMainView = !layerHasVisualizer || !ui.getUseNewMapForNewVis();
						mapView = ui.getMapViewer((useMainView) ? "main" : "_blank_");
					}
				}
				if (mapView == null || mapView.getLayerManager() == null) {
					ui.showMessage(res.getString("no_map_view"), true);
				} else {
					if (!useMainView) {
						//find the copy of the geographical layer in the new map view
						int lidx = mapView.getLayerManager().getIndexOfLayer(td.layerId);
						if (lidx < 0) {
							mapView = ui.getMapViewer("main");
							useMainView = true;
						} else {
							td.themLayer = mapView.getLayerManager().getGeoLayer(lidx);
						}
					}
					Visualizer visualizer = displayProducer.displayOnMap(vis, mapMethodId, td.table, colIds, td.themLayer, mapView);
					if (!useMainView && (mapView instanceof Component)) {
						Component c = (Component) mapView;
						c.setName(c.getName() + ": " + visualizer.getVisualizationName());
						Frame win = CManager.getFrame(c);
						if (win != null) {
							win.setName(c.getName());
							win.setTitle(c.getName());
						}
					}
					supervisor.registerTool(visualizer);
					td.lman.activateLayer(td.layerId);
				}
				if (VectorUtil.contains(colIds, null)) {
					Vector newVector = new Vector(colIds.size(), 10);
					for (int i = 0; i < colIds.size(); i++)
						if (colIds.elementAt(i) != null) {
							newVector.addElement(colIds.elementAt(i));
						}
					colIds = newVector;
				}
			}
		}
		if (chartMethodId != null) {
			displayProducer.display(td.table, colIds, chartMethodId, null);
		}
	}

	protected void linkTableToMapLayer(TableData td) {
		if (dataKeeper.getMapCount() < 1)
			return;
		int mapN = 0;
		//find current map number
		if (ui != null) {
			while (mapN < dataKeeper.getMapCount() && ui.getMapViewer(mapN) == null) {
				++mapN;
			}
		}
		if (mapN >= dataKeeper.getMapCount()) {
			mapN = 0;
		}
		LayerManager lman = dataKeeper.getMap(mapN);
		if (lman == null || lman.getLayerCount() < 1)
			return;
		SelectDialog sd = new SelectDialog(getFrame(),
		// following string: "Select the layer to link to the table"
				res.getString("Select_the_layer_to"), res.getString("Select_the_layer_to"));
		for (int i = 0; i < lman.getLayerCount(); i++) {
			GeoLayer gl = lman.getGeoLayer(i);
			if (gl.getType() != Geometry.raster && gl.getType() != Geometry.image) {
				sd.addOption(gl.getName(), gl.getContainerIdentifier(), false);
			}
		}
		sd.show();
		if (sd.wasCancelled())
			return;
		td.layerId = sd.getSelectedOptionId();
		if (td.layerId != null) {
			td.themLayer = dataKeeper.linkTableToMapLayer(td.tableN, mapN, td.layerId);
			if (td.themLayer != null) {
				td.mapN = mapN;
				td.lman = lman;
			} else {
				td.layerId = null;
			}
		}
	}

}
