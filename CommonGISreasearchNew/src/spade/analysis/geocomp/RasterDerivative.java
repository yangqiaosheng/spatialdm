package spade.analysis.geocomp;

import java.awt.Choice;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.util.ResourceBundle;

import spade.analysis.system.ESDACore;
import spade.analysis.system.SystemUI;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.OKDialog;
import spade.lib.lang.Language;
import spade.vis.geometry.RasterGeometry;
import spade.vis.space.GeoLayer;
import spade.vis.space.LayerManager;

/**
* Calculates a derivative for a raster layer
*/
public class RasterDerivative extends GeoCalculator {
	static ResourceBundle res = Language.getTextResource("spade.analysis.geocomp.Res");
	/**
	* The functions used for derivation
	*/
	public static final String[] functions =
	// following strings: "Gradient module", "Gradient azimut", "Laplacian"
	{ res.getString("Gradient_module"), res.getString("Gradient_azimut"), res.getString("Laplacian") };
	public static final int fGrModule = 0, fGrAzimut = 1, fLaplacian = 2;

	/**
	* Calculates a derivative for a raster layer. The arguments are a layer manager
	* (a GeoCalculator must itself care about selection of a layer or layers of
	* appropriate type) and SystemUI (to be used for displaying messages and
	* finding an owner frame for dialogs)
	* If calculation was successful, returns the derivative of the layer (an
	* instance of DGridLayer).
	*/
	@Override
	public Object doCalculation(LayerManager lman, ESDACore core) {
		SystemUI ui = core.getUI();
		// following strings:"Select RASTER layer"
		GeoLayer layer = selectRasterLayer(lman, ui, res.getString("Select_RASTER_layer"));
		if (layer == null)
			return null;
		RasterGeometry rg = getRaster(layer, ui);
		if (rg == null)
			return null;

		Panel p = new Panel(new GridLayout(3, 1));
		// following strings:"Select the function:"
		p.add(new Label(res.getString("Select_the_function_")));
		Choice c = new Choice();
		for (String function : functions) {
			c.add(function);
		}
		p.add(c);
		Frame win = null;
		if (ui != null) {
			win = ui.getMainFrame();
		}
		if (win == null) {
			win = CManager.getAnyFrame();
		}
		// following strings:"Set parameters"
		OKDialog okd = new OKDialog(win, res.getString("Set_parameters"), true);
		okd.addContent(p);
		okd.show();
		if (okd.wasCancelled())
			return null;
		int func = c.getSelectedIndex();

		RasterGeometry der = (RasterGeometry) rg.clone();
		for (int yy = 0; yy < der.Row; yy++) {
			for (int xx = 0; xx < der.Col; xx++) {
				der.ras[xx][yy] = 0;
			}
		}
		double f1 = 0, f2 = 0, f3 = 0, f4 = 0, f = 0;
		double A = 0, B = 0;

		float rsize = 1.0f * der.Row * der.Col;

		switch (func) {
		case fGrModule:
			for (int yy = 0; yy < der.Row; yy++) {
				for (int xx = 0; xx < der.Col; xx++) {
					try {
						f1 = rg.ras[xx - 1][yy];
						f2 = rg.ras[xx + 1][yy];
						f3 = rg.ras[xx][yy - 1];
						f4 = rg.ras[xx][yy + 1];
						A = (f2 - f1) / 2 / rg.DX;
						B = (f4 - f3) / 2 / rg.DY;
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
						A = (f2 - f1) / cDX;
						B = (f4 - f3) / cDY;
					}
					if (Double.isNaN(A) || Double.isNaN(B)) {
						der.ras[xx][yy] = Float.NaN;
					} else {
						der.ras[xx][yy] = (float) Math.sqrt(A * A + B * B);
					}
				}
				if (ui != null) {
					int perc = Math.round(((yy + 1) * der.Col) / rsize * 100);
					// following strings:"Calculation: "+String.valueOf(perc)+"% ready"
					ui.showMessage(res.getString("Calculation_") + String.valueOf(perc) + res.getString("_ready"));
				}
			}
			der.recalculateStatistics();
			break;
		case fGrAzimut:
			for (int yy = 0; yy < der.Row; yy++) {
				for (int xx = 0; xx < der.Col; xx++) {
					try {
						f1 = rg.ras[xx - 1][yy];
						f2 = rg.ras[xx + 1][yy];
						f3 = rg.ras[xx][yy - 1];
						f4 = rg.ras[xx][yy + 1];
						A = (f2 - f1) / 2 / rg.DX;
						B = (f4 - f3) / 2 / rg.DY;
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
						A = (f2 - f1) / cDX;
						B = (f4 - f3) / cDY;
					}
					if (Double.isNaN(A) || Double.isNaN(B)) {
						der.ras[xx][yy] = Float.NaN;
					} else {
						der.ras[xx][yy] = (float) ((Math.atan2(B, -A) / Math.PI + 1) * 180);
					}
				}
				if (ui != null) {
					int perc = Math.round(((yy + 1) * der.Col) / rsize * 100);
					// following strings:"Calculation: "+String.valueOf(perc)+"% ready"
					ui.showMessage(res.getString("Calculation_") + String.valueOf(perc) + res.getString("_ready"));
				}
			}
			der.minV = 0;
			der.maxV = 360;
			der.Intr = false;
			break;
		case fLaplacian:
			for (int yy = 0; yy < der.Row; yy++) {
				for (int xx = 0; xx < der.Col; xx++) {
					try {
						f = rg.ras[xx][yy];
						f1 = rg.ras[xx - 1][yy];
						f2 = rg.ras[xx + 1][yy];
						f3 = rg.ras[xx][yy - 1];
						f4 = rg.ras[xx][yy + 1];
					} catch (ArrayIndexOutOfBoundsException ex) {
						if (xx == 0) {
							f1 = rg.ras[xx][yy];
							f2 = rg.ras[xx + 1][yy];
						} else if (xx == rg.Col - 1) {
							f1 = rg.ras[xx - 1][yy];
							f2 = rg.ras[xx][yy];
						} else {
							f1 = rg.ras[xx - 1][yy];
							f2 = rg.ras[xx + 1][yy];
						}
						if (yy == 0) {
							f3 = rg.ras[xx][yy];
							f4 = rg.ras[xx][yy + 1];
						} else if (yy == rg.Row - 1) {
							f3 = rg.ras[xx][yy - 1];
							f4 = rg.ras[xx][yy];
						} else {
							f3 = rg.ras[xx][yy - 1];
							f4 = rg.ras[xx][yy + 1];
						}
					}
					A = 4 * f;
					B = f1 + f2 + f3 + f4;
					if (Double.isNaN(B) && !Double.isNaN(A)) {
						der.ras[xx][yy] = Float.NaN;
					} else {
						der.ras[xx][yy] = (float) (A - B);
					}
				}
				if (ui != null) {
					int perc = Math.round(((yy + 1) * der.Col) / rsize * 100);
					// following strings: "Calculation: "+String.valueOf(perc)+"% ready"
					ui.showMessage(res.getString("Calculation_") + String.valueOf(perc) + res.getString("_ready"));
				}
			}
			der.recalculateStatistics();
			break;
		} //end switch
		return constructRasterLayer(der, functions[func] + " from " + layer.getName());
	}
}
