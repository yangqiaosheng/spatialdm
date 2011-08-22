package spade.analysis.geocomp;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.geocomp.trans.Calc;
import spade.analysis.system.ESDACore;
import spade.analysis.system.SystemUI;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.OKDialog;
import spade.lib.color.BinaryColorScale;
import spade.lib.lang.Language;
import spade.vis.dmap.DGeoObject;
import spade.vis.dmap.DGridLayer;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.RasterGeometry;
import spade.vis.geometry.RealRectangle;
import spade.vis.space.GeoLayer;
import spade.vis.space.LayerManager;

/**
* Allows the user to specify a logical expression over one or more rasters.
* Constructs a new ("binary") raster with values 1 for condition satisfaction
* and 0 for non-satisfaction.
*/
public class RasterQuery extends GeoCalculator {
	static ResourceBundle res = Language.getTextResource("spade.analysis.geocomp.Res");

	/**
	* Constructs a raster by querying one or more raster layers. The arguments
	* are a layer manager (a * GeoCalculator must itself care about selection
	* of a layer or layers of appropriate type) and SystemUI (to be used for
	* displaying messages and finding an owner frame for dialogs).
	* If calculation was successful, returns the resulting layer (an instance
	* of DGridLayer).
	*/
	@Override
	public Object doCalculation(LayerManager lman, ESDACore core) {
		SystemUI ui = core.getUI();
		if (lman == null || lman.getLayerCount() < 1) {
			// following string: "No layers available!"
			if (ui != null) {
				ui.showMessage(res.getString("No_layers_available_"), true);
			}
			return null;
		}
		Vector lids = new Vector(lman.getLayerCount(), 1), lnames = new Vector(lman.getLayerCount(), 1);
		for (int i = 0; i < lman.getLayerCount(); i++) {
			GeoLayer gl = lman.getGeoLayer(i);
			if (gl.getObjectCount() > 0 && gl.getType() == Geometry.raster) {
				lnames.addElement(gl.getName());
				lids.addElement(gl.getContainerIdentifier());
			}
		}
		if (lids.size() < 1) {
			// following string:"No RASTER layers available!"
			if (ui != null) {
				ui.showMessage(res.getString("No_RASTER_layers"), true);
			}
			return null;
		}
		EnterCondition enterFormula = new EnterCondition(lnames);
		Frame win = null;
		if (ui != null) {
			win = ui.getMainFrame();
		}
		if (win == null) {
			win = CManager.getAnyFrame();
		}
		// following string:"Specify the query condition"
		OKDialog okd = new OKDialog(win, res.getString("Specify_the_query"), true);
		okd.addContent(enterFormula);
		okd.show();
		if (okd.wasCancelled())
			return null;

		Calc ca = new Calc(lids.size());
		if (!ca.MakeCalcTrack(enterFormula.getFormula())) {
			// following string:"Invalid formula!"
			if (ui != null) {
				ui.showMessage(res.getString("Invalid_formula_"), true);
			}
			return null;
		}
		int[] inUse = ca.indexInUse();
		if (inUse == null) {
			// following string:"Invalid formula!"
			if (ui != null) {
				ui.showMessage(res.getString("Invalid_formula_"), true);
			}
			return null;
		}

		//double cDX=Double.MAX_VALUE, cDY=Double.MAX_VALUE;
		float cDX = 0.0f, cDY = 0.0f;
		int lidx[] = new int[inUse.length]; //indices of the layers in the map
		for (int i = 0; i < inUse.length; i++) {
			lidx[i] = lman.getIndexOfLayer((String) lids.elementAt(inUse[i] - 1));
		}
		Vector rasters = new Vector(inUse.length, 1);
		RealRectangle bounds = null;
		boolean sameBounds = true;
		for (int i = 0; i < inUse.length; i++) {
			GeoLayer gl = lman.getGeoLayer(lidx[i]);
			DGeoObject gobj = (DGeoObject) gl.getObjectAt(0);
			if (gobj == null || gobj.getGeometry() == null || !(gobj.getGeometry() instanceof RasterGeometry)) {
				if (ui != null) {
					// following string:"No data in layer "+gl.getName()+" or illegal data format!"
					ui.showMessage(res.getString("No_data_in_layer") + gl.getName() + res.getString("or_illegal_data"), true);
				}
				return null;
			}
			RasterGeometry crg = (RasterGeometry) gobj.getGeometry();
			if (crg.getBoundRect() == null) {
				// following string:"No data in layer "
				if (ui != null) {
					ui.showMessage(res.getString("No_data_in_layer") + gl.getName(), true);
				}
				return null;
			}
			rasters.addElement(crg);
			/*
			if (bounds==null) bounds=new RealRectangle(crg.getBoundRect());
			else bounds=bounds.union(new RealRectangle(crg.getBoundRect()));
			*/
			if (bounds == null) {
				bounds = new RealRectangle(crg.getBoundRect());
			} else if (sameBounds) {
				RealRectangle b1 = new RealRectangle(crg.getBoundRect());
				sameBounds = (b1.rx1 == bounds.rx1) && (b1.rx2 == bounds.rx2) && (b1.ry1 == bounds.ry1) && (b1.ry2 == bounds.ry2);
			}
			/*
			if (cDX > crg.DX) cDX=crg.DX;
			if (cDY > crg.DY) cDY=crg.DY;
			*/
			if (cDX < crg.DX) {
				cDX = crg.DX;
			}
			if (cDY < crg.DY) {
				cDY = crg.DY;
			}
		}
		if (bounds == null) {// following string:"No data in the layers!"
			if (ui != null) {
				ui.showMessage(res.getString("No_data_in_the_layers"), true);
			}
			return null;
		}
		if (!sameBounds) {
			Panel p = new Panel(new BorderLayout());
			// following string:"Define the bounds of the resulting raster as"
			p.add(new Label(res.getString("Define_the_bounds_of")), "North");
			Panel pp = new Panel(new GridLayout(1, 2));
			CheckboxGroup cbg = new CheckboxGroup();
			// following string:"intersection"
			Checkbox cbInter = new Checkbox(res.getString("intersection"), true, cbg),
			// following string:"union"
			cbUnion = new Checkbox(res.getString("union"), false, cbg);
			pp.add(cbInter);
			pp.add(cbUnion);
			p.add(pp, "Center");
			// following string:"of the bounds of the source rasters"
			p.add(new Label(res.getString("of_the_bounds_of_the")), "South");
			// following string:"Resulting raster bounds
			okd = new OKDialog(win, res.getString("Resulting_raster"), false);
			okd.addContent(p);
			okd.show();
			boolean union = cbUnion.getState();
			for (int i = 1; i < rasters.size() && bounds != null; i++) {
				RasterGeometry crg = (RasterGeometry) rasters.elementAt(i);
				if (union) {
					bounds = bounds.union(new RealRectangle(crg.getBoundRect()));
				} else {
					bounds = bounds.intersect(new RealRectangle(crg.getBoundRect()));
				}
			}
		}
		if (bounds == null || bounds.rx1 >= bounds.rx2 || bounds.ry1 >= bounds.ry2) {
			// following string:"The intersection of the layers is empty!"
			if (ui != null) {
				ui.showMessage(res.getString("The_intersection_of"), true);
			}
			return null;
		}
		RasterGeometry comb = new RasterGeometry();
		comb.Intr = false;
		comb.isBinary = true;
		comb.rx1 = bounds.rx1;
		comb.rx2 = bounds.rx2;
		comb.ry1 = bounds.ry1;
		comb.ry2 = bounds.ry2;
		comb.Xbeg = comb.rx1;
		comb.Ybeg = comb.ry1;
		comb.DX = cDX;
		comb.DY = cDY;
		comb.Col = (int) ((comb.rx2 - comb.rx1) / cDX + 1);
		comb.Row = (int) ((comb.ry2 - comb.ry1) / cDY + 1);
		while (comb.ras == null) {
			long size = comb.Col * comb.Row * 4l, freeMem = Runtime.getRuntime().freeMemory();
			if (size < freeMem / 2) {
				try {
					comb.ras = new float[comb.Col][comb.Row];
					System.out.println("* created a grid " + comb.Col + "x" + comb.Row);
				} catch (Exception e) {
					System.out.println("Exception caught: " + e.toString());
				}
			} else {
				System.out.println("* not enough memory for a grid " + comb.Col + "x" + comb.Row + ": free memory = " + freeMem);
			}
			if (comb.ras == null) {
				comb.DX *= 2;
				comb.DY *= 2;
				comb.Col = (int) ((comb.rx2 - comb.rx1) / comb.DX + 1);
				comb.Row = (int) ((comb.ry2 - comb.ry1) / comb.DY + 1);
			}
		}
		comb.Intr = true;

		float rsize = 1.0f * comb.Row * comb.Col;

		for (int yy = 0; yy < comb.Row; yy++) {
			for (int xx = 0; xx < comb.Col; xx++) {
				for (int i = 0; i < inUse.length; i++) {
					RasterGeometry crg = (RasterGeometry) rasters.elementAt(i);
					ca.setElement(inUse[i],
//            (crg.getInterpolatedValue(crg.getGridX(comb.getWorldX(xx)), crg.getGridY(comb.getWorldY(yy))))
							(crg.getAggregatedValue(comb.getWorldX(xx), comb.getWorldY(yy), Math.abs(comb.DX), Math.abs(comb.DY))));
				}
				float v = (float) ca.useTrack();
				if (Float.isInfinite(v)) {
					v = Float.NaN;
				}
				if (Float.isNaN(v)) {
					comb.ras[xx][yy] = Float.NaN;
				} else {
					comb.ras[xx][yy] = (v <= 0.0001f) ? 0 : 1;
				}
			}
			if (ui != null) {
				int perc = Math.round(((yy + 1) * comb.Col) / rsize * 100);
				// following string:"Calculation: "+String.valueOf(perc)+"% ready"
				ui.showMessage(res.getString("Calculation_") + String.valueOf(perc) + res.getString("_ready"));
			}
		}
		comb.minV = 0;
		comb.maxV = 1;
		String name = enterFormula.getCondition();
		for (int i = 0; i < inUse.length; i++) {
			GeoLayer gl = lman.getGeoLayer(lidx[i]);
			String sub = "$" + String.valueOf(inUse[i]);
			int p = name.indexOf(sub), l = sub.length();
			while (p >= 0) {
				name = name.substring(0, p) + gl.getName() + name.substring(p + l);
				p = name.indexOf(sub, p + gl.getName().length() + 1);
			}
		}

		DGridLayer grl = constructRasterLayer(comb, name);
		BinaryColorScale cs = new BinaryColorScale();
		grl.getGridVisualizer().setColorScale(cs);
		return grl;
	}
}
