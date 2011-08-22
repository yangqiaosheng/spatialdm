package spade.analysis.geocomp;

import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.ScrollPane;
import java.awt.TextArea;
import java.awt.TextField;
import java.util.Hashtable;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.system.ESDACore;
import spade.analysis.system.SystemUI;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.OKDialog;
import spade.lib.color.GrayColorScale;
import spade.lib.lang.Language;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.ThematicDataItem;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;
import spade.vis.dmap.DGridLayer;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.MultiGeometry;
import spade.vis.geometry.RasterGeometry;
import spade.vis.geometry.RealPolyline;
import spade.vis.geometry.RealRectangle;
import spade.vis.map.MapDraw;
import spade.vis.map.MapViewer;
import spade.vis.space.GeoLayer;
import spade.vis.space.LayerManager;
import spade.vis.space.SelectLayer;
import ui.AttributeChooser;
import ui.TableData;
import ui.TableManager;

/**
* Produces a raster layer from a layer with area objects
*/
public class RasterFromPolygons extends GeoCalculator {
	static ResourceBundle res = Language.getTextResource("spade.analysis.geocomp.Res");

	/**
	* Produces a raster layer from a layer with polygonal objects.
	* The arguments are a layer manager (a GeoCalculator must itself care about
	* selection of a layer or layers of appropriate type, in this case a layer
	* with area objects) and SystemUI (to be used for displaying messages and
	* finding an owner frame for dialogs)
	* If calculation was successful, returns the produced raster layer (an instance
	* of DGridLayer).
	*/
	@Override
	public Object doCalculation(LayerManager lman, ESDACore core) {
		SystemUI ui = core.getUI();
		//propose the user to select a layer with point objects
		if (lman == null)
			return null;
		Frame win = null;
		if (ui != null) {
			win = ui.getMainFrame();
		}
		if (win == null) {
			win = CManager.getAnyFrame();
		}
		GeoLayer layer = SelectLayer.selectLayer(lman, "" + Geometry.area, res.getString("Select_an_AREA_layer"), win);
		if (layer == null)
			return null;

//    int attrN=-1; //the attribute used for weighting;

		TableManager tman = new TableManager();
		tman.setDataKeeper(core.getDataKeeper());
		TableData td = tman.selectTable("Select table"/*res.getString("Select_the_table_for3")*/, layer.getEntitySetIdentifier());

		AttributeDataPortion table = td.table;//layer.getThematicData();
		if (table != null) {
			table.loadData();
		}
		if (table == null || !table.hasData() || !table.getEntitySetIdentifier().equals(layer.getEntitySetIdentifier())) {
			// following string:"No thematic data in the layer!"
			if (ui != null) {
				ui.showMessage(res.getString("No_thematic_data_in"), true);
			}
			return null;
		}

		AttributeChooser aSel = new AttributeChooser();
/**/	aSel.selectColumns(table, "Select attributes", core.getUI());
		Vector selAttrs = aSel.getSelectedColumnIds();
///*!!!*/    attrN = table.getAttrIndex(((String)selAttrs.firstElement()));
		if (selAttrs == null || selAttrs.isEmpty())
			return null;

		Panel p;
		OKDialog okd;

		okd = new OKDialog(win, res.getString("Select_attribute"), true);
		TextArea ta = new TextArea();
		String attrs = "Rasters will be generated for the following attributes:\n\n";
		for (int i = 0; i < selAttrs.size(); i++) {
			attrs += table.getAttributeName((String) selAttrs.elementAt(i)) + "\n";
		}
		ta.setText(attrs);
		okd.addContent(ta);
		okd.show();
		if (okd.wasCancelled())
			return null;
/*
    Panel p=new Panel(new GridLayout(3,1));
    // following string:"Select the attribute for weighting:"
    p.add(new Label(res.getString("Select_the_attribute")));
    Choice c=new Choice();
    Vector aIds=new Vector(table.getAttrCount(),5);
    for (int i=0; i<table.getAttrCount(); i++)
      {
        aIds.addElement(table.getAttributeId(i));
        c.add(table.getAttributeName(i));
      }
    p.add(c);
    // following string:"Select attribute"
    OKDialog okd=new OKDialog(win,res.getString("Select_attribute"),true);
    okd.addContent(p);
    okd.show();
    if (okd.wasCancelled()) return null;
    int idx=c.getSelectedIndex();
    String attrId=(String)aIds.elementAt(idx);
    attrN=table.getAttrIndex(attrId);
*/

		boolean otherTable = !layer.hasThematicData(table);
		if (otherTable) {
			layer.receiveThematicData(table);
			if (layer.getObjectFilter() != null) {
				layer.setThematicFilter(table.getObjectFilter());
			}
		}
		Vector qualMaps = new Vector();
		Vector grids = new Vector();
		Vector weights = new Vector();

		for (int j = 0; j < selAttrs.size(); j++) {
			Hashtable qualMap = new Hashtable();
			Hashtable qualMapTF = new Hashtable();
			if (!table.isAttributeNumeric(table.getAttrIndex(((String) selAttrs.elementAt(j))))) {
				Vector attrVals = table.getAllAttrValuesAsStrings(table.getAttributeId(table.getAttrIndex(((String) selAttrs.elementAt(j)))));

				ScrollPane sp = new ScrollPane();
				p = new Panel(new GridLayout(attrVals.size(), 2));
				sp.add(p);

				for (int i = 0; i < attrVals.size(); i++) {
					p.add(new Label((String) attrVals.elementAt(i)));
					TextField tf = new TextField(Integer.toString(i));
					p.add(tf);

					qualMap.put(attrVals.elementAt(i), new Integer(i));
					qualMapTF.put(attrVals.elementAt(i), tf);

				}

				okd = new OKDialog(win, "Mapping for " + table.getAttributeName((String) selAttrs.elementAt(j)), true);
				okd.addContent(sp);
				okd.show();
				if (okd.wasCancelled())
					return null;

				for (int i = 0; i < attrVals.size(); i++) {
					try {
						qualMap.put(attrVals.elementAt(i), new Float(((TextField) qualMapTF.get(attrVals.elementAt(i))).getText()));
					} catch (NumberFormatException ex) {
						qualMap.put(attrVals.elementAt(i), new Float(Float.NaN));
					}
				}
			}
			qualMaps.addElement(qualMap);
		}

		RealRectangle bounds = ((DGeoLayer) layer).getActiveLayerBounds();
		RasterGeometry lin = new RasterGeometry(bounds.rx1, bounds.ry1, bounds.rx2, bounds.ry2);
		lin.Xbeg = lin.rx1;
		lin.Ybeg = lin.ry1;
		lin.Col = 200;
		lin.Row = 200;
		lin.DX = (lin.rx2 - lin.rx1) / lin.Col;
		lin.DY = (lin.ry2 - lin.ry1) / lin.Row;
		lin.Intr = true;
		lin.Geog = true;

		double step = Math.min(Math.abs((lin.rx2 - lin.rx1) / 200), Math.abs((lin.ry2 - lin.ry1) / 200));
		if (lin.DX > 0) {
			lin.DX = (float) step;
		} else {
			lin.DX = (float) -step;
		}
		if (lin.DY > 0) {
			lin.DY = (float) step;
		} else {
			lin.DY = (float) -step;
		}
		lin.Col = Math.round((bounds.rx2 - bounds.rx1) / lin.DX);
		lin.Row = Math.round((bounds.ry2 - bounds.ry1) / lin.DY);

		if (ui == null || ui.getCurrentMapN() < 0) {
			ui.showMessage(res.getString("No_map_found"), true);
			return null;
		}
		MapViewer mview = ui.getMapViewer(ui.getCurrentMapN());
		if (mview == null) {
			ui.showMessage(res.getString("No_map_found"));
			return null;
		}
		MapDraw map = mview.getMapDrawer();
		if (map == null) {
			ui.showMessage(res.getString("No_map_found"));
			return null;
		}

		ParameterSheet ps = new ParameterSheet(lin, map, lman);
		okd = new OKDialog(win, res.getString("Set_parameters"), true);
		okd.addContent(ps);
		okd.show();
		ps.clearHighlighting();
		if (okd.wasCancelled())
			return null;

		try {
			ps.updateGeometry(lin);
			for (int j = 0; j < selAttrs.size(); j++) {
				RasterGeometry rg = new RasterGeometry(/*lin.rx1, lin.ry1, lin.rx2, lin.ry2*/);
				ps.updateGeometry(rg);
				rg.ras = new float[rg.Col][rg.Row];
//clear raster
				for (int yy = 0; yy < rg.Row; yy++) {
					for (int xx = 0; xx < rg.Col; xx++) {
						rg.ras[xx][yy] = Float.NaN;
					}
				}
				grids.addElement(rg);
			}
		} catch (Exception ex) {
			if (ui != null) {
				ui.showMessage(res.getString("Invalid_formula_"), true);
			}
			return null;
		}

//    lin.ras = new float[lin.Col][lin.Row];

		int xx, yy, x1, x2, y1, y2;

		RealPolyline rl;

		MultiGeometry mg = null;
		boolean multi = false;
		int mgparts = 1;

		ThematicDataItem pdata;
		float weight = 0;

		DGeoObject gobj;
		float[] bnd;
		for (int i = 0; i < layer.getObjectCount(); i++) {
			gobj = (DGeoObject) layer.getObjectAt(i);
			if (gobj == null) {
				continue;
			}
			if (!((DGeoLayer) layer).isObjectActive(i)) {
				continue;
			}

			if (gobj.getGeometry() instanceof MultiGeometry) {
				mg = (MultiGeometry) gobj.getGeometry();
				multi = true;
				mgparts = mg.getPartsCount();
			} else {
				multi = false;
				mgparts = 1;
			}
			for (int part = 0; part < mgparts; part++) {
				if (multi) {
					rl = (RealPolyline) mg.getPart(part);
				} else {
					rl = (RealPolyline) gobj.getGeometry();
				}
				if (rl == null) {
					continue;
				}
				if (rl.p.length < 2) {
					continue; // questionable
				}

				pdata = gobj.getData();
				if (pdata == null) {
					continue;
				}

				weights.removeAllElements();

				for (int j = 0; j < selAttrs.size(); j++) {
					if (!table.isAttributeNumeric(table.getAttrIndex(((String) selAttrs.elementAt(j))))) {
						try {
							weight = ((Float) ((Hashtable) qualMaps.elementAt(j)).get(pdata.getAttrValue(table.getAttrIndex(((String) selAttrs.elementAt(j)))))).floatValue();
						} catch (Exception ex) {
							weight = Float.NaN;
						}
					} else {
						weight = (float) pdata.getNumericAttrValue(table.getAttrIndex(((String) selAttrs.elementAt(j))));
					}
					weights.addElement(new Float(weight));
				}

				bnd = rl.getBoundRect();
				x1 = (int) Math.floor(lin.getGridX(bnd[0]));
				y1 = (int) Math.floor(lin.getGridY(bnd[1]));
				x2 = (int) Math.ceil(lin.getGridX(bnd[2]));
				y2 = (int) Math.ceil(lin.getGridY(bnd[3]));
				if (x1 < 0) {
					x1 = 0;
				}
				if (y1 < 0) {
					y1 = 0;
				}
				if (x2 >= lin.Col) {
					x2 = lin.Col - 1;
				}
				if (y2 >= lin.Row) {
					y2 = lin.Row - 1;
				}
				for (int ry = y1; ry <= y2; ry++) {
					for (int rx = x1; rx <= x2; rx++) {

//calculation for each point
						if (rl.isPointInPolygon(lin.getWorldX(rx), lin.getWorldY(ry), 0)) {
							for (int j = 0; j < selAttrs.size(); j++) {
								lin = (RasterGeometry) grids.elementAt(j);
								if (Float.isNaN(lin.ras[rx][ry])) {
									lin.ras[rx][ry] = ((Float) weights.elementAt(j)).floatValue();
								}
							}
						}

					}
				}
				if (ui != null) {
					ui.showMessage(res.getString("Calculation_object") + (i + 1) + res.getString("of") + layer.getObjectCount());
				}
			}
		}

		Vector layers = new Vector();
		for (int j = 0; j < selAttrs.size(); j++) {
			lin = (RasterGeometry) grids.elementAt(j);
			lin.recalculateStatistics();
			/**/String prep = ": " + table.getAttributeName(table.getAttrIndex(((String) selAttrs.elementAt(j))));
			DGridLayer grl = constructRasterLayer(lin, layer.getName() + prep);
			GrayColorScale cs = new GrayColorScale();
			cs.reversed = true;
			grl.getGridVisualizer().setColorScale(cs);
			layers.addElement(grl);
		}
		return layers;
	}
}
