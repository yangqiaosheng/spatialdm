package spade.analysis.geocomp;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.ScrollPane;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.system.ESDACore;
import spade.analysis.system.SystemUI;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.OKDialog;
import spade.lib.lang.Language;
import spade.lib.util.IdMaker;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataTable;
import spade.vis.database.Parameter;
import spade.vis.dmap.DBridgeLayer;
import spade.vis.dmap.DGeoLayer;
import spade.vis.geometry.RasterGeometry;
import spade.vis.space.GeoLayer;
import spade.vis.space.LayerManager;
import spade.vis.spec.DataSourceSpec;

public class TableFromRaster extends GeoCalculator {
	static ResourceBundle res = Language.getTextResource("spade.analysis.geocomp.Res");
	/**
	* The function used for generation of attributes
	*/
	public static final String[] functions =
	//following string:  "Mean", "Median", "RMS", "Maximum", "Minimum", "Max-Min","Part (%) of area","Area","Integral"/*, "Correlation"*/
	{ res.getString("Mean"), res.getString("Median"), "Mode", "Std.D", "Max", "Min", "Max-Min", res.getString("Sum") };
	public static final int fMean = 0, fMedian = 1, fMode = 2, fRMS = 3, fMax = 4, fMin = 5, fMaxMin = 6, fSum = 7;

	protected Checkbox[][] fcb;

	@Override
	public Object doCalculation(LayerManager lman, ESDACore core) {
		SystemUI ui = core.getUI();
		// following string: "Select RASTER layer"
		Vector layers = selectRasterLayers(lman, ui, res.getString("Select_RASTER_layer"));
		if (layers == null || layers.isEmpty())
			return null;

		// forming Panel for calculation dialog
		Panel p = new Panel(new BorderLayout());
		// following text:"Specify parameters for computation:"
		p.add(new Label(res.getString("Specify_parameters")), "North");

		Panel pp = new Panel(new BorderLayout());
		Panel pa = new Panel(new GridLayout(layers.size() + 1, 1));
		Panel pf = new Panel(new GridLayout(layers.size() + 1, functions.length));
//    Panel pq=new Panel(new BorderLayout());

		p.add(pp, "Center");
		pp.add(pa, "West");
		pp.add(pf, "Center");
//    pp.add(pq, "South");

//    Checkbox qty = new Checkbox(res.getString("Number_of_objects"));
//    pq.add(qty, "West");

		fcb = new Checkbox[layers.size()][functions.length];
		for (int attr = 0; attr < layers.size(); attr++) {
			for (int func = 0; func < functions.length; func++) {
				fcb[attr][func] = new Checkbox();
			}
		}

		for (Checkbox[] element : fcb) {
			element[fMean].setState(true);
		}

		// following text:"Function:"
		pa.add(new Label(res.getString("Function_")));

		for (int i = 0; i < functions.length; i++) {
			Button b = new Button(functions[i]);
			b.setName("f" + i);
			b.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					int num = Integer.parseInt(((Component) e.getSource()).getName().substring(1));
					for (int j = 0; j < fcb.length; j++) {
						fcb[j][num].setState(!fcb[j][num].getState());
					}
				}
			});
			pf.add(b);
		}

		for (int lay = 0; lay < layers.size(); lay++) {
			pa.add(new Label(((GeoLayer) layers.elementAt(lay)).getName()));
			for (int func = 0; func < functions.length; func++) {
				pf.add(fcb[lay][func]);
			}
		}
		// following text:"Set parameters"
		OKDialog okd = new OKDialog(CManager.getAnyFrame(), res.getString("Set_parameters"), true);
		ScrollPane ps = new ScrollPane() {
			@Override
			public Dimension getPreferredSize() {
				Dimension cSize = getComponent(0).getPreferredSize();
				return new Dimension(Math.min(cSize.width, Toolkit.getDefaultToolkit().getScreenSize().width) + 16, Math.min(cSize.height, Toolkit.getDefaultToolkit().getScreenSize().height) + 16);
			}
		};
		ps.add(p);
		okd.addContent(ps);
		okd.show();
		if (okd.wasCancelled())
			return null;

//    Checkbox cbTimeDep = new Checkbox("rasters are time-dependent", true);
//    OKDialog okTimeDep = new OKDialog(CManager.getAnyFrame(), "Parameters of the raster set", true);
//    okTimeDep.addContent(cbTimeDep);
//    okTimeDep.show();
//    if (okTimeDep.wasCancelled()) return null;
		DataTable table = new DataTable();
		DGeoLayer layer = new DBridgeLayer();
/**/	String name = "Grid";

		DataSourceSpec spec;
		Hashtable parents = new Hashtable();
		Hashtable parameters = new Hashtable();

		Attribute cx = new Attribute(IdMaker.makeId("x", table), AttributeTypes.real);
		cx.setName("x");
		table.addAttribute(cx);

		Attribute cy = new Attribute(IdMaker.makeId("y", table), AttributeTypes.real);
		cy.setName("y");
		table.addAttribute(cy);

		Attribute x1 = new Attribute(IdMaker.makeId("x1", table), AttributeTypes.real);
		x1.setName("x1");
		table.addAttribute(x1);

		Attribute y1 = new Attribute(IdMaker.makeId("y1", table), AttributeTypes.real);
		y1.setName("y1");
		table.addAttribute(y1);

		Attribute x2 = new Attribute(IdMaker.makeId("x2", table), AttributeTypes.real);
		x2.setName("x2");
		table.addAttribute(x2);

		Attribute y2 = new Attribute(IdMaker.makeId("y2", table), AttributeTypes.real);
		y2.setName("y2");
		table.addAttribute(y2);

		for (int i = 0; i < layers.size(); i++) {
			GeoLayer clayer = (GeoLayer) layers.elementAt(i);

			RasterGeometry rg = getRaster(clayer, ui);
			if (rg == null)
				return null;

			for (int func = 0; func < functions.length; func++)
				if (fcb[i][func].getState()) {
					Attribute child = new Attribute(IdMaker.makeId("id", table), AttributeTypes.real);
/**/				child.setName(clayer.getName() + " - " + functions[func]);
					table.addAttribute(child);

					try {
						spec = (DataSourceSpec) clayer.getDataSource();
					} catch (Exception ex) {
						spec = null;
					}

					Attribute parent = null;
					if (spec != null && spec.gridAttribute.length() > 0)
						if (parents.containsKey(spec.gridAttribute)) {
							parent = (Attribute) parents.get(spec.gridAttribute);
						} else {
							parent = new Attribute(IdMaker.makeId("par", table), AttributeTypes.real);
							parent.setName(spec.gridAttribute);
							parents.put(spec.gridAttribute, parent);
						}
					if (parent != null) {
						parent.addChild(child);
					}

					if (spec != null && spec.gridParameterNames != null && spec.gridParameterValues != null && !spec.gridParameterNames.isEmpty() && !spec.gridParameterValues.isEmpty()
							&& spec.gridParameterNames.size() == spec.gridParameterValues.size()) {
						for (int j = 0; j < spec.gridParameterNames.size(); j++) {
							String curParName = (String) spec.gridParameterNames.elementAt(j);
							Object curParValue = spec.gridParameterValues.elementAt(j);

							Parameter par;
							if (parameters.containsKey(curParName)) {
								par = (Parameter) parameters.get(curParName);
							} else {
								par = new Parameter();
								par.setName(curParName);
								table.addParameter(par);
								parameters.put(curParName, par);
							}

							par.addValue(curParValue);
							child.addParamValPair(par.getName(), curParValue);
						}
					}

					// treating the function used for calculation as another parameter
					{
						String curParName = "Function";
						Object curParValue = functions[func];

						Parameter par;
						if (parameters.containsKey(curParName)) {
							par = (Parameter) parameters.get(curParName);
						} else {
							par = new Parameter();
							par.setName(curParName);
							table.addParameter(par);
							parameters.put(curParName, par);
						}

						par.addValue(curParValue);
						child.addParamValPair(par.getName(), curParValue);
					}

				}
/*
      if (i == 0)
        name += " " +clayer.getName();
      else
        name += ", " +clayer.getName();
*/
		}
		table.setName(name);
		layer.setName(name);

		layer.setDataTable(table);
		layer.setLinkedToTable(true);
		layer.setHasAllObjects(true);

		TableCreatePanel sizeControl = new TableCreatePanel(layers, fcb, table, layer, core);
		core.getDisplayProducer().makeWindow(sizeControl, name);

		layer.getDrawingParameters().fillContours = false;

		return layer;//table;
	}
}