package spade.analysis.geocomp;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.Choice;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.geocomp.functions.Area;
import spade.analysis.geocomp.functions.Function;
import spade.analysis.geocomp.functions.Integral;
import spade.analysis.geocomp.functions.Max;
import spade.analysis.geocomp.functions.Mean;
import spade.analysis.geocomp.functions.Median;
import spade.analysis.geocomp.functions.Min;
import spade.analysis.geocomp.functions.PartOfArea;
import spade.analysis.geocomp.functions.RMS;
import spade.analysis.geocomp.functions.Range;
import spade.analysis.system.ESDACore;
import spade.analysis.system.SystemUI;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.Centimeter;
import spade.lib.basicwin.OKDialog;
import spade.lib.lang.Language;
import spade.lib.util.StringUtil;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.dmap.DGeoObject;
import spade.vis.dmap.DLayerManager;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.RasterGeometry;
import spade.vis.geometry.RealPoint;
import spade.vis.space.GeoLayer;
import spade.vis.space.LayerManager;
import spade.vis.space.SelectLayer;

/**
* Calculates attributes of vector objects on the basis of a raster layer
*/
public class AttrFromRaster extends GeoCalculator {
	static ResourceBundle res = Language.getTextResource("spade.analysis.geocomp.Res");
	/**
	* The function used for generation of attributes
	*/
	public static final String[] functions =
	//following string:  "Mean", "Median", "RMS", "Maximum", "Minimum", "Max-Min","Part (%) of area","Area","Integral"/*, "Correlation"*/
	{ res.getString("Mean"), res.getString("Median"), "RMS", "Maximum", "Minimum", "Max-Min", res.getString("Part_of_area"), res.getString("Area"), res.getString("Integral") /*, "Correlation"*/};
	protected static final int fMean = 0, fMedian = 1, fRMS = 2, fMax = 3, fMin = 4, fMaxMin = 5, fPart = 6, fArea = 7, fInteg = 8;

	/**
	* Calculates attributes of vector objects on the basis of a raster layer.
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
		GeoLayer layer = SelectLayer.selectLayer(lman, "PAL", res.getString("Select_VECTOR_layer"), win);
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
		GeoLayer rlayer = selectRasterLayer(lman, ui, res.getString("Select_RASTER_layer"));
		if (rlayer == null)
			return null;
		RasterGeometry rg = getRaster(rlayer, ui);
		if (rg == null)
			return null;
		//propose the user to set the parameters
		boolean needRadius = layer.getType() != Geometry.area;
		Panel p = new Panel(new BorderLayout());
		// following text:"Specify parameters for computation:"
		p.add(new Label(res.getString("Specify_parameters")), "North");
		Panel pp = new Panel(new GridLayout((needRadius) ? 4 : 2, 2));
		p.add(pp, "Center");
		// following text:"Function:"
		pp.add(new Label(res.getString("Function_")));
		Choice c = new Choice();
		for (String function : functions) {
			c.add(function);
		}
		pp.add(c);
		TextField radTF = null;
		if (needRadius) {
			// following text:"Radius:"
			pp.add(new Label(res.getString("Radius_")));
			radTF = new TextField("1.0", 5);
			pp.add(radTF);

			Panel p2 = new Panel(new FlowLayout(FlowLayout.LEFT));
			pp.add(new Label(res.getString("Scale")));
			Centimeter cm = new Centimeter();
			p2.add(cm);
			float sc = ui.getMapViewer(ui.getCurrentMapN()).getMapDrawer().getMapContext().getPixelValue() * cm.getMinimumSize().width * ((DLayerManager) lman).user_factor;
			p2.add(new Label(StringUtil.floatToStr(sc, 2) + " " + ((DLayerManager) lman).getUserUnit()));
			pp.add(p2);
		}
		// following text:"Attribute name:"
		pp.add(new Label(res.getString("Attribute_name_")));
		TextField attrTF = new TextField(20);
		pp.add(attrTF);
		// following text:"include partial zones"
		Checkbox sPart = new Checkbox(res.getString("include_partial_zones"), false);
		p.add(sPart, "South");
		// following text:"Set parameters"
		OKDialog okd = new OKDialog(win, res.getString("Set_parameters"), true);
		okd.addContent(p);
		okd.show();
		if (okd.wasCancelled())
			return null;
		int func = c.getSelectedIndex();
		float rad = 1.0f;
		if (radTF != null) {
			String str = radTF.getText();
			if (str != null) {
				try {
					rad = Float.valueOf(str).floatValue();
				} catch (NumberFormatException nfe) {
				}
			}
			if (rad <= 0) {
				rad = 0.1f;
			}
		}
		if (!needRadius) {
			rad = 0;
		}
		String attrName = attrTF.getText();
		if (attrName != null) {
			attrName = attrName.trim();
		}
		if (attrName == null || attrName.length() < 1) {
			attrName = functions[func] + " of " + rlayer.getName();
		}
		boolean partial = sPart.getState();

		float minLimit = Float.NaN, maxLimit = Float.NaN;
		if (func == fPart || func == fArea)
			if (!rg.isBinary) {
				p = new Panel(new GridLayout(2, 1));
				// following text:"Values of "+rlayer.getName()+" must be"
				p.add(new Label(res.getString("Values_of") + rlayer.getName() + res.getString("must_be")));
				pp = new Panel(new GridLayout(1, 4));
				// following text:"from"
				pp.add(new Label(res.getString("from"), Label.CENTER));
				TextField tf1 = new TextField(StringUtil.floatToStr(rg.minV, rg.minV, rg.maxV), 5);
				pp.add(tf1);
				// following text:"to"
				pp.add(new Label(res.getString("to"), Label.CENTER));
				TextField tf2 = new TextField(StringUtil.floatToStr(rg.maxV, rg.minV, rg.maxV), 5);
				pp.add(tf2);
				p.add(pp);
				// following text:"Set limits"
				okd = new OKDialog(win, res.getString("Set_limits"), true);
				okd.addContent(p);
				okd.show();
				if (!okd.wasCancelled()) {
					String str = tf1.getText();
					if (str != null) {
						try {
							minLimit = Float.valueOf(str.trim()).floatValue();
						} catch (NumberFormatException nfe) {
							minLimit = Float.NaN;
						}
					}
					str = tf2.getText();
					if (str != null) {
						try {
							maxLimit = Float.valueOf(str.trim()).floatValue();
						} catch (NumberFormatException nfe) {
							maxLimit = Float.NaN;
						}
					}
				} else if (func == fPart)
					return null;
				System.out.println("minLimit=" + minLimit + ", maxLimit=" + maxLimit);
			} else {
				minLimit = 0.5f;
			}

		Function op = null;
		switch (func) {
		case fMean:
			op = new Mean();
			break;
		case fMedian:
			op = new Median();
			break;
		case fRMS:
			op = new RMS();
			break;
		case fMax:
			op = new Max();
			break;
		case fMin:
			op = new Min();
			break;
		case fMaxMin:
			op = new Range();
			break;
		case fPart:
			PartOfArea par = new PartOfArea();
			par.setLimits(minLimit, maxLimit);
			op = par;
			break;
		case fArea:
			Area ar = new Area();
			ar.setLimits(minLimit, maxLimit);
			op = ar;
			break;
		case fInteg:
			op = new Integral();
			break;
		}
		if (op == null)
			return null;

		int attrN = table.addDerivedAttribute(attrName, AttributeTypes.real, AttributeTypes.compute, null);

		double RadX = rad / rg.DX;
		double RadY = rad / rg.DY;
		// for each geo object
		for (int i = 0; i < layer.getObjectCount(); i++) {
			DGeoObject gobj = (DGeoObject) layer.getObjectAt(i);
			if (gobj == null) {
				continue;
			}
			DataRecord pdata = (DataRecord) gobj.getData();
			if (pdata == null) {
				continue;
			}
			// calculate bounding box
			Geometry geom = gobj.getGeometry();
			if (geom == null) {
				continue;
			}
			float[] bounds = geom.getBoundRect();
/*
      int x1=(int)Math.floor(rg.getGridX(bounds[0])-RadX);
      int y1=(int)Math.floor(rg.getGridY(bounds[1])-RadY);
      int x2=(int)Math.ceil( rg.getGridX(bounds[2])+RadX);
      int y2=(int)Math.ceil( rg.getGridY(bounds[3])+RadY);
*/
			int x1 = (int) Math.ceil(rg.getGridX(bounds[0]) - RadX);
			int y1 = (int) Math.ceil(rg.getGridY(bounds[1]) - RadY);
			int x2 = (int) Math.floor(rg.getGridX(bounds[2]) + RadX);
			int y2 = (int) Math.floor(rg.getGridY(bounds[3]) + RadY);

			// raster MUST contain bounding box if not ordered otherwise
			if (x1 >= rg.Col || x2 < 0 || y1 >= rg.Row || y2 < 0) { // out
				pdata.addAttrValue("");
				continue;
			} else if (x1 < 0 || x2 >= rg.Col || y1 < 0 || y2 >= rg.Row) { // intersects
				if (partial) {
					if (x1 < 0) {
						x1 = 0;
					}
					if (y1 < 0) {
						y1 = 0;
					}
					if (x2 >= rg.Col) {
						x2 = rg.Col - 1;
					}
					if (y2 >= rg.Row) {
						y2 = rg.Row - 1;
					}
				} else {
					pdata.addAttrValue("");
					continue;
				}
			}
			// common block
			if (func == fArea || func == fInteg) {
				op.init(rg.DX * rg.DY);
			} else {
				op.init();
			}

			float total = 1.0f * (y2 - y1 + 1) * (x2 - x1 + 1);

			for (int ry = y1; ry <= y2; ry++) {
				for (int rx = x1; rx <= x2; rx++) {
					if (geom.contains(rg.getWorldX(rx), rg.getWorldY(ry), rad)) {
						float val = rg.ras[rx][ry];
						if (!Float.isNaN(val)) {
							op.addData(val);
						}
					}
				}
				if (ui != null && x2 - x1 > 100) {
					int perc = Math.round((ry - y1 + 1) * (x2 - x1 + 1) / total * 100);
					// following text:"Calculation: object "+(i+1)+" of "+layer.getObjectCount()+"; "+perc+"% ready"
					ui.showMessage(res.getString("Calculation_object") + (i + 1) + res.getString("of") + layer.getObjectCount() + "; " + perc + res.getString("_ready"));
				}
			}

			// workaround for point data and small radius
			if (geom.getType() == Geometry.point && geom instanceof RealPoint) {
				float val = rg.getInterpolatedValue(rg.getGridX(((RealPoint) geom).x), rg.getGridY(((RealPoint) geom).y));
				if (!Float.isNaN(val)) {
					op.addData(val);
				}
			}

			double result = op.getResult();
			if (Double.isNaN(result)) {
				pdata.addAttrValue("");
			} else {
				pdata.addAttrValue(String.valueOf((float) result));
			}
		}

		AttrSpec asp = new AttrSpec();
		asp.layer = layer;
		asp.table = table;
		asp.attrIds = new Vector(1, 1);
		asp.attrIds.addElement(table.getAttributeId(attrN));
		return asp;
	}
}
