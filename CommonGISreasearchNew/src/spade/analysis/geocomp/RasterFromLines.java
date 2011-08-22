package spade.analysis.geocomp;

import java.awt.Choice;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.system.ESDACore;
import spade.analysis.system.SystemUI;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.Centimeter;
import spade.lib.basicwin.OKDialog;
import spade.lib.color.GrayColorScale;
import spade.lib.lang.Language;
import spade.lib.util.StringUtil;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.ThematicDataItem;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;
import spade.vis.dmap.DGridLayer;
import spade.vis.dmap.DLayerManager;
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

/**
* Produces a raster layer from a layer with linear objects
*/
public class RasterFromLines extends GeoCalculator {
	static ResourceBundle res = Language.getTextResource("spade.analysis.geocomp.Res");
	/**
	* The functions used for producing a raster from lines
	*/
	public static final String[] functions = { res.getString("Distance"), res.getString("Closeness"), res.getString("Influence"), res.getString("Total_length"), res.getString("Density"), res.getString("Weighted_density") };
	protected static final int fDistance = 0, fCloseness = 1, fInfluence = 2, fLength = 3, fDensity = 4, fWeighted = 5;

	/**
	* Produces a raster layer from a layer with linear objects.
	* The arguments are a layer manager (a GeoCalculator must itself care about
	* selection of a layer or layers of appropriate type, in this case a layer
	* with point objects) and SystemUI (to be used for displaying messages and
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
		GeoLayer layer = SelectLayer.selectLayer(lman, "" + Geometry.area + Geometry.line, res.getString("Select_a_LINE_layer"), win);
		if (layer == null)
			return null;
		//propose the user to set the parameters
		Panel p = new Panel(new GridLayout(4, 1));
		// following string: "Specify parameters for computation:"
		p.add(new Label(res.getString("Specify_parameters")));
		Panel pp = new Panel(new FlowLayout(FlowLayout.LEFT));
		pp.add(new Label(res.getString("Function_")));
		Choice c = new Choice();
		for (String function : functions) {
			c.add(function);
		}
		pp.add(c);
		p.add(pp);
		pp = new Panel(new FlowLayout(FlowLayout.LEFT));
		pp.add(new Label(res.getString("Radius_")));
		TextField radTF = new TextField("1.0", 5);
		pp.add(radTF);
		p.add(pp);

		pp = new Panel(new FlowLayout(FlowLayout.LEFT));
		pp.add(new Label(res.getString("Scale")));
		Centimeter cm = new Centimeter();
		pp.add(cm);
		float sc = ui.getMapViewer(ui.getCurrentMapN()).getMapDrawer().getMapContext().getPixelValue() * cm.getMinimumSize().width * ((DLayerManager) lman).user_factor;
		pp.add(new Label(StringUtil.floatToStr(sc, 2) + " " + ((DLayerManager) lman).getUserUnit()));
		p.add(pp);

		OKDialog okd = new OKDialog(win, res.getString("Set_parameters"), true);
		okd.addContent(p);
		okd.show();
		if (okd.wasCancelled())
			return null;
		int func = c.getSelectedIndex();
		float rad = 1.0f;
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

		int attrN = -1; //the attribute used for weighting;
		if (func == fWeighted) {
			AttributeDataPortion table = layer.getThematicData();
			if (table == null || !table.hasData()) {
				// following string:"No thematic data in the layer!"
				if (ui != null) {
					ui.showMessage(res.getString("No_thematic_data_in"), true);
				}
				return null;
			}
			p = new Panel(new GridLayout(3, 1));
			// following string:"Select the attribute for weighting:"
			p.add(new Label(res.getString("Select_the_attribute")));
			c = new Choice();
			Vector aIds = new Vector(table.getAttrCount(), 5);
			for (int i = 0; i < table.getAttrCount(); i++)
				if (table.isAttributeNumeric(i)) {
					aIds.addElement(table.getAttributeId(i));
					c.add(table.getAttributeName(i));
				}
			p.add(c);
			// following string:"Select attribute"
			okd = new OKDialog(win, res.getString("Select_attribute"), true);
			okd.addContent(p);
			okd.show();
			if (okd.wasCancelled())
				return null;
			int idx = c.getSelectedIndex();
			String attrId = (String) aIds.elementAt(idx);
			attrN = table.getAttrIndex(attrId);
		}

		RealRectangle bounds = ((DGeoLayer) layer).getActiveLayerBounds();
		RasterGeometry lin = new RasterGeometry(bounds.rx1 - rad, bounds.ry1 - rad, bounds.rx2 + rad, bounds.ry2 + rad);
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
		lin.Col = Math.round((bounds.rx2 - bounds.rx1 + 2 * rad) / lin.DX);
		lin.Row = Math.round((bounds.ry2 - bounds.ry1 + 2 * rad) / lin.DY);

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
		} catch (Exception ex) {
			if (ui != null) {
				ui.showMessage(res.getString("Invalid_formula_"), true);
			}
			return null;
		}

		lin.ras = new float[lin.Col][lin.Row];

		double RadX = rad / lin.DX;
		double RadY = rad / lin.DY;
		double dist;

		int xx, yy, x1, x2, y1, y2;

		RealPolyline rl;

		MultiGeometry mg = null;
		boolean multi = false;
		int mgparts = 1;

		ThematicDataItem pdata;
		float weight = 0;
		float incr = (float) (1 / (Math.PI * rad * rad));

//clear raster
		switch (func) {
		case fDistance:
			for (yy = 0; yy < lin.Row; yy++) {
				for (xx = 0; xx < lin.Col; xx++) {
					lin.ras[xx][yy] = rad;
				}
			}
			break;
		case fWeighted:
			if (attrN < 0)
				return null;
		case fDensity:
		case fCloseness:
		case fInfluence:
		case fLength:
			for (yy = 0; yy < lin.Row; yy++) {
				for (xx = 0; xx < lin.Col; xx++) {
					lin.ras[xx][yy] = 0;
				}
			}
			break;
		}

		for (int i = 0; i < layer.getObjectCount(); i++) {
			DGeoObject gobj = (DGeoObject) layer.getObjectAt(i);
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

				if (func == fWeighted) {
					pdata = gobj.getData();
					if (pdata == null) {
						continue;
					}
					weight = (float) pdata.getNumericAttrValue(attrN);
				}

				float[] bnd = rl.getBoundRect();
				x1 = (int) Math.floor(lin.getGridX(bnd[0]) - RadX);
				y1 = (int) Math.floor(lin.getGridY(bnd[1]) - RadY);
				x2 = (int) Math.ceil(lin.getGridX(bnd[2]) + RadX);
				y2 = (int) Math.ceil(lin.getGridY(bnd[3]) + RadY);
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
						switch (func) {
						case fDistance:
							dist = rl.distanceToPolyline(lin.getWorldX(rx), lin.getWorldY(ry));
							if (dist < rad) {
								lin.ras[rx][ry] = (float) Math.min(lin.ras[rx][ry], dist);
							}
							break;
						case fCloseness:
							dist = rl.distanceToPolyline(lin.getWorldX(rx), lin.getWorldY(ry));
							dist = 1 - dist / rad;
							if (dist > 0) {
								lin.ras[rx][ry] = (float) Math.max(lin.ras[rx][ry], dist);
							}
							break;
						case fInfluence:
							dist = rl.distanceToPolyline(lin.getWorldX(rx), lin.getWorldY(ry));
							dist = 1 - dist / rad;
							if (dist > 0) {
								lin.ras[rx][ry] += (float) dist;
							}
							break;
						case fLength:
							dist = rl.totalLengthInCircle(lin.getWorldX(rx), lin.getWorldY(ry), rad);
							if (dist > 0) {
								lin.ras[rx][ry] += (float) dist;
							}
							break;
						case fDensity:
							dist = rl.distanceToPolyline(lin.getWorldX(rx), lin.getWorldY(ry));
							if (dist < rad) {
								lin.ras[rx][ry] += incr;
							}
							break;
						case fWeighted:
							dist = rl.distanceToPolyline(lin.getWorldX(rx), lin.getWorldY(ry));
							if (dist < rad) {
								lin.ras[rx][ry] += incr * weight;
							}
							break;
						}

					}
				}
				if (ui != null) {
					ui.showMessage(res.getString("Calculation_object") + (i + 1) + res.getString("of") + layer.getObjectCount());
				}
			}
		}

		lin.recalculateStatistics();
		String prep = " " + ((func <= 1) ? res.getString("to1") : res.getString("of1")) + " ";
		DGridLayer grl = constructRasterLayer(lin, functions[func] + prep + layer.getName());
		GrayColorScale cs = new GrayColorScale();
		cs.reversed = true;
		grl.getGridVisualizer().setColorScale(cs);
		return grl;
	}
}