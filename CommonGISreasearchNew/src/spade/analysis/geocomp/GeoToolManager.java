package spade.analysis.geocomp;

import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Component;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.system.ESDACore;
import spade.analysis.system.SystemUI;
import spade.analysis.tools.DataAnalyser;
import spade.analysis.tools.ToolKeeper;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.OKDialog;
import spade.lib.lang.Language;
import spade.vis.database.DataTable;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DLayerManager;
import spade.vis.map.MapViewer;

/**
* Provides access to tools for calculations and other operations with map
* layers
*/
public class GeoToolManager implements DataAnalyser {
	static ResourceBundle res = Language.getTextResource("spade.analysis.geocomp.Res");
	/**
	* The object managing descriptions of all available tools. A
	* class implementing a geocomputation tool must extend the abstract class
	* GeoCalculator
	*/
	protected ToolKeeper toolKeeper = new ToolKeeper(new GeoToolsRegister());

	/**
	* Returns true when the tool has everything necessary for its operation.
	* In this particular case, returns true if there is a class for at least one
	* geocomputational method.
	*/
	@Override
	public boolean isValid(ESDACore core) {
		return toolKeeper.getAvailableToolCount() > 0;
	}

	/**
	* Constructs and runs the multi-table tool with the given identifier and
	* returns its result
	*/
	protected Object applyTool(String toolId, ESDACore core) {
		if (core == null)
			return null;
		Object obj = toolKeeper.getTool(toolId);
		String err = null;
		if (obj == null) {
			err = toolKeeper.getErrorMessage();
		} else if (!(obj instanceof GeoCalculator)) {
			err = toolId + ": class " + obj.getClass().getName() + " does not implement GeoCalculator!";
		}
		if (err != null) {
			if (core.getUI() != null) {
				core.getUI().showMessage(err, true);
			}
			return null;
		}
		GeoCalculator gc = (GeoCalculator) obj;
		int mapN = 0;
		if (core.getUI() != null) {
			mapN = core.getUI().getCurrentMapN();
		}
		return gc.doCalculation(core.getDataKeeper().getMap(mapN), core);
	}

	/**
	* Offers the user to select a geocomputational tool from available tools
	* and runs this tool
	*/
	public void selectAndRunTool(ESDACore core) {
		if (core == null)
			return;

		int ntools = toolKeeper.getAvailableToolCount();
		if (ntools < 1)
			return;
		Frame win = null;
		SystemUI ui = core.getUI();
		if (ui != null) {
			win = ui.getMainFrame();
		}
		if (win == null) {
			win = CManager.getAnyFrame();
		}
		Panel p = new Panel(new spade.lib.basicwin.ColumnLayout());
		p.add(new Label(res.getString("Select_the_analysis")));
		Panel pp = new Panel(new GridLayout((ntools + 1) / 2, 1));
		// following text: "Select the analysis tool:"
		CheckboxGroup cbg = new CheckboxGroup();
		Checkbox cb[] = new Checkbox[ntools];
		for (int i = 0; i < ntools; i++) {
			cb[i] = new Checkbox(toolKeeper.getAvailableToolName(i), cbg, false);
			pp.add(cb[i]);
		}
		p.add(pp);
		p.add(new spade.lib.basicwin.Line(false));
		java.awt.Choice ch = new java.awt.Choice();

		ch.addItem("Main window");
		ch.addItem("New  window");
		Vector mapViews = new Vector();
		addIntoChoice(core, ch, mapViews);
		pp = new Panel();
		pp.add(ch);
		p.add(pp);
		// following text: "Select the analysis tool"
		OKDialog okd = new OKDialog(win, res.getString("Select_the_analysis1"), true);
		okd.addContent(p);
		okd.show();
		if (okd.wasCancelled())
			return;
		int idx = -1;
		for (int i = 0; i < ntools && idx < 0; i++)
			if (cb[i].getState()) {
				idx = i;
			}
		if (idx < 0)
			return;
		Object result = applyTool(toolKeeper.getAvailableToolId(idx), core);
		if (result == null)
			return;

		//=================================

		int sIdx = -1;
		//int mapN = core.getUI().getCurrentMapN();  // ueberfluessig
		sIdx = ch.getSelectedIndex();
		MapViewer selectedMapView = null;

		if (sIdx == 0) {
			selectedMapView = core.getUI().getMapViewer(core.getUI().getCurrentMapN());
		} else if (sIdx == 1) {
			// create copy und find map
			selectedMapView = core.getUI().getMapViewer("_blank_");
		} else {
			selectedMapView = (MapViewer) mapViews.elementAt(sIdx - 2);
		}

		//=======================================================================

		if (result instanceof Component) { //this is a graph
			System.out.println("!-!- geocomputation:this is graph");
			core.getDisplayProducer().showGraph((Component) result);
		} else if (result instanceof DGeoLayer) { //add the new layer to the map
			System.out.println("!-!-geocomputation:this is layer");
			//int mapN=0;
			//if (core.getUI()!=null) mapN=core.getUI().getCurrentMapN();
			DGeoLayer layer = (DGeoLayer) result;
			//allow the user to edit the default name
			Panel pan = new Panel(new GridLayout(2, 1));
			// following text:"Provide a name for the new layer:"
//			pan.add(new Label(res.getString("Provide_a_name_for")));
			TextField tf = new TextField(layer.getName(), 80);
//			pan.add(tf);
			// following text:"Layer name"
//			OKDialog dia = new OKDialog(win, res.getString("Layer_name"), false);
//			dia.addContent(pan);
//			dia.show();
			String str = tf.getText();
			if (str != null) {
				str = str.trim();
				if (str.length() > 0) {
					layer.setName(str);
				}
			}
			core.getDataLoader().addMapLayer(layer, selectedMapView);
			if (layer.hasThematicData()) {
				DataTable table = (DataTable) layer.getThematicData();
				int tblN = core.getDataLoader().addTable(table);
				table.setName(str);
				core.getDataLoader().setLink(layer, tblN);
				layer.setThematicFilter(table.getObjectFilter());
			}
		} else if (result instanceof Vector && ((Vector) result).firstElement() instanceof DGeoLayer) { //add the new layers to the map
			System.out.println("!-!-geocomp:this is " + result + " layers");
			for (int i = 0; i < ((Vector) result).size(); i++) {
				//mapN = 0;
				//if (core.getUI() != null) mapN = core.getUI().getCurrentMapN();
				DGeoLayer layer = (DGeoLayer) ((Vector) result).elementAt(i);
				//allow the user to edit the default name
				Panel pan = new Panel(new GridLayout(2, 1));
				// following text:"Provide a name for the new layer:"
//				pan.add(new Label(res.getString("Provide_a_name_for")));
				TextField tf = new TextField(layer.getName(), 80);
//				pan.add(tf);
				// following text:"Layer name"
//				OKDialog dia = new OKDialog(win, res.getString("Layer_name"), false);
//				dia.addContent(pan);
//				dia.show();
				String str = tf.getText();
				if (str != null) {
					str = str.trim();
					if (str.length() > 0) {
						layer.setName(str);
					}
				}
				core.getDataLoader().addMapLayer(layer, selectedMapView);
				//ID
				if (layer.hasThematicData()) {
					DataTable table = (DataTable) layer.getThematicData();
					int tblN = core.getDataLoader().addTable(table);
					table.setName(str);
					core.getDataLoader().setLink(layer, tblN);
					layer.setThematicFilter(table.getObjectFilter());
				}
				//ID
			}
		} else if (result instanceof AttrSpec) { //a new attribute added to a table
			System.out.println("!-!-geocomp: a new attribute added to a table");
			AttrSpec asp = (AttrSpec) result;
			if (asp.table != null && asp.attrIds != null && asp.attrIds.size() > 0) {
				DLayerManager lman = (DLayerManager) selectedMapView.getLayerManager();
				int lri = lman.getIndexOfLayer(asp.layer.getContainerIdentifier());
				asp.layer = lman.getLayer(lri);
				int tableN = -1;//, mapN=0;
				//if (core.getUI()!=null) mapN=core.getUI().getCurrentMapN();
				if (asp.table.getContainerIdentifier() == null || core.getDataKeeper().getTableIndex(asp.table.getContainerIdentifier()) < 0) {
					//this is a new table that must be properly registered and linked to the layer
					tableN = core.getDataLoader().addTable(asp.table);
					if (asp.layer != null) {
						core.getDataLoader().setLink((DGeoLayer) asp.layer, tableN);
					}
				}
				asp.table.notifyPropertyChange("new_attributes", null, asp.attrIds);
				if (asp.layer != null) {
					if (tableN < 0) {
						tableN = core.getDataKeeper().getTableIndex(asp.table.getContainerIdentifier());
					}
					if (tableN >= 0) {
						//selectedMapView=core.getUI().getMapViewer(core.getUI().getCurrentMapN());
						core.getDisplayProducer().displayOnMap(asp.table, asp.attrIds, asp.layer, selectedMapView);
						//asp.attrIds, asp.layer, );
					}

				}
			}
		}
//ID
		else if (result instanceof DataTable) {
			System.out.println("!-!-!-add data table");
			core.getDataLoader().addTable((DataTable) result);
		}
//~ID
		if (core.getUI() != null) {
			core.getUI().showMessage(null, false);
		}
	}

	/**
	* A method from the DataAnalyser interface. Invokes the method selectAndRunTool.
	*/
	@Override
	public void run(ESDACore core) {
		selectAndRunTool(core);
	}

	protected void addIntoChoice(ESDACore core, java.awt.Choice ch, Vector mapV) {

		spade.analysis.system.WindowManager wm = null;
		spade.analysis.system.Supervisor supervisor = core.getSupervisor();
		if (supervisor != null) {
			wm = supervisor.getWindowManager();
		}
		if (wm != null && wm.getWindowCount() > 0) {
			for (int i = 0; i < wm.getWindowCount(); i++)
				if (wm.getWindow(i) instanceof Frame) {
					Frame w = (Frame) wm.getWindow(i);
					spade.vis.map.MapViewer mv = findMapView(w);
					if (mv != null) {
						ch.addItem(w.getTitle());
						mapV.addElement(mv);
					}
				}
		}
	}

	protected MapViewer findMapView(java.awt.Container c) {
		if (c == null)
			return null;
		if (c instanceof MapViewer)
			return (MapViewer) c;
		for (int i = 0; i < c.getComponentCount(); i++) {
			Component comp = c.getComponent(i);
			if (comp instanceof MapViewer)
				return (MapViewer) comp;
		}
		MapViewer mv = null;
		for (int i = 0; i < c.getComponentCount() && mv == null; i++) {
			Component comp = c.getComponent(i);
			if (comp instanceof java.awt.Container) {
				mv = findMapView((java.awt.Container) comp);
			}
		}
		return mv;
	}

}
