package spade.analysis.geocomp;

import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.util.ResourceBundle;

import spade.analysis.system.ESDACore;
import spade.analysis.system.SystemUI;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.OKDialog;
import spade.lib.color.GrayColorScale;
import spade.lib.lang.Language;
import spade.vis.dmap.DGridLayer;
import spade.vis.geometry.RasterGeometry;
import spade.vis.space.GeoLayer;
import spade.vis.space.LayerManager;

/**
* Makes a topological (lighting) model of a raster layer
*/
public class TopoModel extends GeoCalculator {
	static ResourceBundle res = Language.getTextResource("spade.analysis.geocomp.Res");

	/**
	* Makes an illumination model of a raster layer. The arguments are a layer
	* manager (a GeoCalculator must itself care about selection of a layer or
	* layers) of appropriate type and SystemUI (to be used for displaying messages
	* and finding an owner frame for dialogs)
	* If calculation was successful, returns a new raster layer (an instance
	* of DGridLayer).
	*/
	@Override
	public Object doCalculation(LayerManager lman, ESDACore core) {
		SystemUI ui = core.getUI();
		// following string: "Select RASTER layer"
		GeoLayer layer = selectRasterLayer(lman, ui, res.getString("Select_RASTER_layer"));
		if (layer == null)
			return null;
		RasterGeometry rg = getRaster(layer, ui);
		if (rg == null)
			return null;

		Panel p = new Panel(new GridLayout(3, 1));
		// following string: "Illumination model parameters"
		p.add(new Label(res.getString("Illumination_model")));
		Panel pp = new Panel(new FlowLayout(FlowLayout.RIGHT));
		// following string: "Azimuth:"
		pp.add(new Label(res.getString("Azimuth_")));
		TextField tfaz = new TextField("45", 3);
		pp.add(tfaz);
		p.add(pp);
		pp = new Panel(new FlowLayout(FlowLayout.RIGHT));
		// following string: "Angle:"
		pp.add(new Label(res.getString("Angle_")));
		TextField tfan = new TextField("45", 3);
		pp.add(tfan);
		p.add(pp);
		Frame win = null;
		if (ui != null) {
			win = ui.getMainFrame();
		}
		if (win == null) {
			win = CManager.getAnyFrame();
		}
		// following string: "Set parameters"
		OKDialog okd = new OKDialog(win, res.getString("Set_parameters"), true);
		okd.addContent(p);
		okd.show();
		if (okd.wasCancelled())
			return null;

		double v1 = 45d, v2 = 45d;
		String str = tfaz.getText();
		if (str != null) {
			try {
				v1 = Double.valueOf(str.trim()).doubleValue();
			} catch (NumberFormatException nfe) {
				v1 = 45d;
			}
		}
		str = tfan.getText();
		if (str != null) {
			try {
				v2 = Double.valueOf(str.trim()).doubleValue();
			} catch (NumberFormatException nfe) {
				v2 = 45d;
			}
		}
		if (v2 < 0) {
			v2 = 0d;
		} else if (v2 > 90) {
			v2 = 90d;
		}

		double az = Math.PI / 180d * v1;
		double an = Math.PI / 180d * (90 - v2);
		double lx = Math.sin(an) * Math.cos(az);
		double ly = Math.sin(an) * Math.sin(az);
		double lz = Math.cos(an);
		double nx, ny, nz, len, val;
		double factor = (rg.maxV - rg.minV) / (rg.Col * rg.DX + rg.Row * rg.DY) * 4;
		double f1 = 0, f2 = 0, f3 = 0, f4 = 0;

		RasterGeometry topo = (RasterGeometry) rg.clone();

		float rsize = 1.0f * topo.Row * topo.Col;

		for (int yy = 0; yy < topo.Row; yy++) {
			for (int xx = 0; xx < topo.Col; xx++) {
				try {
					f1 = rg.ras[xx - 1][yy];
					f2 = rg.ras[xx + 1][yy];
					f3 = rg.ras[xx][yy - 1];
					f4 = rg.ras[xx][yy + 1];

					nx = (f1 - f2) * 2 * rg.DY / factor;
					ny = -2 * rg.DX * (f4 - f3) / factor;
					nz = rg.DX * rg.DY * 4;
				} catch (ArrayIndexOutOfBoundsException ex) {
					double cDX = 2 * rg.DX;
					double cDY = 2 * rg.DY;

					if (xx == 0) {
						f1 = rg.ras[xx][yy];
						f2 = rg.ras[xx + 1][yy];
						cDX /= 2;
					} else if (xx == rg.Col - 1) {
						f1 = rg.ras[xx - 1][yy];
						f2 = rg.ras[xx][yy];
						cDX /= 2;
					} else {
						f1 = rg.ras[xx - 1][yy];
						f2 = rg.ras[xx + 1][yy];
					}
					if (yy == 0) {
						f3 = rg.ras[xx][yy];
						f4 = rg.ras[xx][yy + 1];
						cDY /= 2;
					} else if (yy == rg.Row - 1) {
						f3 = rg.ras[xx][yy - 1];
						f4 = rg.ras[xx][yy];
						cDY /= 2;
					} else {
						f3 = rg.ras[xx][yy - 1];
						f4 = rg.ras[xx][yy + 1];
					}

					nx = (f1 - f2) * cDY / factor;
					ny = -cDX * (f4 - f3) / factor;
					nz = cDX * cDY;
				}

				len = Math.sqrt(nx * nx + ny * ny + nz * nz);
				nx /= len;
				ny /= len;
				nz /= len;
				val = nx * lx + ny * ly + nz * lz;
				topo.ras[xx][yy] = (val > 0) ? (float) val : 0;
			}
			if (ui != null) {
				int perc = Math.round(((yy + 1) * topo.Col) / rsize * 100);
				// following string: "Calculation: "+String.valueOf(perc)+"% ready"
				ui.showMessage(res.getString("Calculation_") + String.valueOf(perc) + res.getString("_ready"));
			}
		}
		topo.recalculateStatistics();

		DGridLayer grl = constructRasterLayer(topo, "Illumination of " + layer.getName());
		GrayColorScale cs = new GrayColorScale();
		cs.setAlpha(0.6f);
		grl.getGridVisualizer().setColorScale(cs);
		return grl;
	}
}
