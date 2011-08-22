package spade.analysis.geocomp;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.ScrollPane;
import java.awt.Toolkit;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.geocomp.functions.Function;
import spade.analysis.geocomp.functions.Max;
import spade.analysis.geocomp.functions.Mean;
import spade.analysis.geocomp.functions.Median;
import spade.analysis.geocomp.functions.Min;
import spade.analysis.geocomp.functions.Mode;
import spade.analysis.geocomp.functions.RMS;
import spade.analysis.geocomp.functions.Range;
import spade.analysis.geocomp.functions.Sum;
import spade.analysis.system.DataKeeper;
import spade.analysis.system.ESDACore;
import spade.analysis.system.SystemUI;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.OKDialog;
import spade.lib.lang.Language;
import spade.lib.util.IntArray;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.dmap.DGeoObject;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.MultiGeometry;
import spade.vis.geometry.RealPoint;
import spade.vis.space.GeoLayer;
import spade.vis.space.LayerManager;
import spade.vis.space.SelectLayer;
import ui.AttributeChooser;
import ui.TableManager;

/**
* Calculates attributes of vector objects on the basis of a raster layer
*/
public class AttrFromPoints extends GeoCalculator {
	static ResourceBundle res = Language.getTextResource("spade.analysis.geocomp.Res");
	/**
	* The function used for generation of attributes
	*/
	public static final String[] functions =
	//following string:  "Mean", "Median", "RMS", "Maximum", "Minimum", "Max-Min","Part (%) of area","Area","Integral"/*, "Correlation"*/
	{ res.getString("Mean"), res.getString("Median"), "Mode", "Std.D", "Max", "Min", "Max-Min", res.getString("Sum") };
	protected static final int fMean = 0, fMedian = 1, fMode = 2, fRMS = 3, fMax = 4, fMin = 5, fMaxMin = 6, fSum = 7;

	/**
	* Calculates attributes of polygon objects on the basis of a point layer.
	* The arguments are a layer manager (a GeoCalculator must itself care about
	* selection of a layer or layers of appropriate type, in this case a layer
	* with point objects) and SystemUI (to be used for displaying messages and
	* finding an owner frame for dialogs)
	* If calculation was successful, addt the generated attribute to the table
	* attached to the vector layer and returns its identifier. If there was
	* no table yet, creates the table.
	*/
	@Override
	public Object doCalculation(LayerManager lman, ESDACore core) {
		SystemUI ui = core.getUI();
		//propose the user to select a layer with vector objects
		if (lman == null)
			return null;
		Frame win = null;
		if (ui != null) {
			win = ui.getMainFrame();
		}
		if (win == null) {
			win = CManager.getAnyFrame();
		}
		// following text:"Select VECTOR layer"
		GeoLayer layer = SelectLayer.selectLayer(lman, "A", res.getString("Select_an_AREA_layer"), win);
		if (layer == null)
			return null;
		DataTable table = null;
		if (!layer.hasThematicData()) {
			table = constructTable(layer);
			if (table == null) {
				if (ui != null) {
					// following text:"Cannot construct a table for the layer"
					ui.showMessage(res.getString("Cannot_construct_a"), true);
				}
				return null;
			}
		} else if (!(layer.getThematicData() instanceof DataTable)) {
			// following text:"Illegal table type!
			if (ui != null) {
				ui.showMessage(res.getString("Illegal_table_type_"), true);
			}
			return null;
		} else {
			table = (DataTable) layer.getThematicData();
		}
		//propose the user to select a raster layer
		// following text:"Select RASTER layer"
		GeoLayer pLayer = SelectLayer.selectLayer(lman, "P", res.getString("Select_a_POINT_layer"), win);
		if (pLayer == null)
			return null;

		DataTable pTable = null;
		if (pLayer.hasThematicData())
			if (!(pLayer.getThematicData() instanceof DataTable)) {
				// following text:"Illegal table type!
				if (ui != null) {
					ui.showMessage(res.getString("Illegal_table_type_"), true);
				}
				return null;
			} else {
				pTable = (DataTable) pLayer.getThematicData();
			}

		// selection of target table for calculation results - switches active table
		DataKeeper dk = core.getDataKeeper();
		TableManager tman = new TableManager();
		tman.setDataKeeper(dk);

		String entityID = layer.getEntitySetIdentifier();
		int tablesCount = 0;
		for (int i = 0; i < dk.getTableCount(); i++) {
			if (dk.getTable(i).getEntitySetIdentifier().equals(entityID)) {
				tablesCount++;
			}
		}
		if (tablesCount >= 1) {
			if (tablesCount > 1) {
				table = (DataTable) tman.selectTable("Select target table", entityID).table;
			}
			if (!layer.hasThematicData(table)) {
				layer.receiveThematicData(table);
				if (layer.getObjectFilter() != null) {
					layer.setThematicFilter(table.getObjectFilter());
				}
			}
		}

		AttributeChooser attrSel = new AttributeChooser();
		Vector selAttrs = attrSel.selectColumns(pTable, null, null, true, res.getString("Select_attributes"), ui);

		Vector attrNums = new Vector();
		if (pTable != null && selAttrs != null) {
			for (int attr = 0; attr < pTable.getAttrCount(); attr++)
				if (pTable.getAttribute(attr).isNumeric() && selAttrs.contains(pTable.getAttribute(attr))) {
					attrNums.addElement(new Integer(attr));
				}
		}
		int[] attrNum = new int[attrNums.size()];
		for (int i = 0; i < attrNums.size(); i++) {
			attrNum[i] = ((Integer) attrNums.elementAt(i)).intValue();
		}

		// forming Panel for calculation dialog
		Panel p = new Panel(new BorderLayout());
		// following text:"Specify parameters for computation:"
		p.add(new Label(res.getString("Specify_parameters")), "North");

		Panel pp = new Panel(new BorderLayout());
		Panel pa = new Panel(new GridLayout(attrNum.length + 1, 1));
		Panel pf = new Panel(new GridLayout(attrNum.length + 1, functions.length));
		Panel pq = new Panel(new BorderLayout());

		p.add(pp, "Center");
		pp.add(pa, "West");
		pp.add(pf, "Center");
		pp.add(pq, "South");

		Checkbox qty = new Checkbox(res.getString("Number_of_objects"));
		pq.add(qty, "West");

		Checkbox[][] fcb = new Checkbox[attrNum.length][functions.length];
		for (int attr = 0; attr < attrNum.length; attr++) {
			for (int func = 0; func < functions.length; func++) {
				fcb[attr][func] = new Checkbox();
			}
		}

		// following text:"Function:"
		if (pTable != null) {
			pa.add(new Label(res.getString("Function_")));

			for (String function : functions) {
				pf.add(new Label(function));
			}
		}

		if (pTable != null) {
			for (int attr = 0; attr < attrNum.length; attr++) {
				pa.add(new Label(pTable.getAttributeName(attrNum[attr])));
				for (int func = 0; func < functions.length; func++) {
					pf.add(fcb[attr][func]);
				}
			}
		}
		// following text:"Set parameters"
		OKDialog okd = new OKDialog(win, res.getString("Set_parameters"), true);
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

		AttrSpec asp = new AttrSpec();
		asp.layer = layer;
		asp.table = table;
		asp.attrIds = new Vector();
		IntArray attrNs = new IntArray();

		Sum countOp = null;
		if (qty.getState()) {
			countOp = new Sum();
			String attrName = res.getString("Number_of") + pLayer.getName();
			int n = table.addDerivedAttribute(attrName, AttributeTypes.real, AttributeTypes.compute, null);
			asp.attrIds.addElement(table.getAttributeId(n));
			attrNs.addElement(n);
		}
		Function ops[][] = new Function[attrNum.length][functions.length];
		for (int attr = 0; attr < attrNum.length; attr++) {
			for (int func = 0; func < functions.length; func++) {
				ops[attr][func] = null;
				if (fcb[attr][func].getState()) {
					switch (func) {
					case fMean:
						ops[attr][func] = new Mean();
						break;
					case fMedian:
						ops[attr][func] = new Median();
						break;
					case fRMS:
						ops[attr][func] = new RMS();
						break;
					case fMax:
						ops[attr][func] = new Max();
						break;
					case fMin:
						ops[attr][func] = new Min();
						break;
					case fMaxMin:
						ops[attr][func] = new Range();
						break;
					case fSum:
						ops[attr][func] = new Sum();
						break;
					case fMode:
						ops[attr][func] = new Mode();
						break;
					}
					if (ops[attr][func] == null) {
						continue;
					}
					String attrName = functions[func] + res.getString("of") + pTable.getAttributeName(attrNum[attr]);
					int n = table.addDerivedAttribute(attrName, AttributeTypes.real, AttributeTypes.compute, null);
					asp.attrIds.addElement(table.getAttributeId(n));
					attrNs.addElement(n);
				}
			}
		}
		if (attrNs.size() < 1)
			return null;
		boolean onlyCounts = countOp != null && attrNs.size() == 1;

		float total = pLayer.getObjectCount() * layer.getObjectCount();
		int pct = Math.round(total / 100);
		int num = 0;

		for (int i = 0; i < layer.getObjectCount(); i++) {
			DGeoObject gobj = (DGeoObject) layer.getObjectAt(i);
			if (gobj == null) {
				continue;
			}
			DataRecord pdata = (DataRecord) gobj.getData();
			if (pdata == null) {
				continue;
			}
			if (countOp != null) {
				countOp.init();
			}
			for (int attr = 0; attr < attrNum.length; attr++) {
				for (int func = 0; func < functions.length; func++)
					if (ops[attr][func] != null) {
						ops[attr][func].init();
					}
			}

			for (int j = 0; j < pLayer.getObjectCount(); j++)
				if (pLayer.isObjectActive(j)) {
					DGeoObject pObject = (DGeoObject) pLayer.getObjectAt(j);
					Geometry geom = pObject.getGeometry();
					if (geom == null) {
						continue;
					}
					boolean contains = false;
					if (geom instanceof RealPoint) {
						RealPoint point = (RealPoint) geom;
						contains = gobj.contains(point.x, point.y, 0);
					} else if (geom instanceof MultiGeometry) {
						MultiGeometry mg = (MultiGeometry) geom;
						for (int k = 0; k < mg.getPartsCount() && !contains; k++) {
							geom = mg.getPart(k);
							if (geom instanceof RealPoint) {
								RealPoint point = (RealPoint) geom;
								contains = gobj.contains(point.x, point.y, 0);
							}
						}
					}
					if (contains) {
						if (countOp != null) {
							countOp.addData(1.0);
						}
						if (!onlyCounts) {
							for (int attr = 0; attr < attrNum.length; attr++) {
								float val = (float) pObject.getData().getNumericAttrValue(attrNum[attr]);
								if (Float.isNaN(val)) {
									continue;
								}
								for (int func = 0; func < functions.length; func++)
									if (ops[attr][func] != null) {
										ops[attr][func].addData(val);
									}
							}
						}
					}
					++num;
					if (ui != null && num % pct == 0) {
						int perc = Math.round(num / total * 100);
						ui.showMessage(res.getString("Calculation_object") + (i + 1) + res.getString("of") + layer.getObjectCount() + "; " + perc + res.getString("_ready"));
					}
				}

			if (countOp != null) {
				int result = countOp.getCounter();
				pdata.addAttrValue(String.valueOf(result));
			}
			if (!onlyCounts) {
				for (int attr = 0; attr < attrNum.length; attr++) {
					for (int func = 0; func < functions.length; func++)
						if (ops[attr][func] != null) {
							double result = ops[attr][func].getResult();
							if (Double.isNaN(result)) {
								pdata.addAttrValue("");
							} else {
								pdata.addAttrValue(String.valueOf((float) result));
							}
						}
				}
			}
		}
		return asp;
	}
}
