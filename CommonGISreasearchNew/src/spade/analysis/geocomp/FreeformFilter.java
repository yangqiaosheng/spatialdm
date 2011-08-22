package spade.analysis.geocomp;

import java.awt.Choice;
import java.awt.Frame;
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
* Performs filtering using free-form convolution matrix
*/
public class FreeformFilter extends GeoCalculator {
	static ResourceBundle res = Language.getTextResource("spade.analysis.geocomp.Res");

	/**
	* Filtering functions
	*/
//  protected static final String [] functions =
//    {"Mean", "Median", "RMS", "Local anomalies", "Maximum", "Minimum", "Max-Min"};
/*
  protected static final int
    fMean   = 0,
    fMedian = 1,
    fRMS    = 2,
    fLocal  = 3,
    fMax    = 4,
    fMin    = 5,
    fMaxMin = 6;
*/
	/**
	* Filters a raster layer. The arguments are a layer manager (a
	* GeoCalculator must itself care about selection of a layer or layers of
	* appropriate type) and SystemUI (to be used for displaying messages and
	* finding an owner frame for dialogs)
	* If calculation was successful, returns a new layer (an instance
	* of DGridLayer) being the result of filtering.
	*/
	@Override
	public Object doCalculation(LayerManager lman, ESDACore core) {
		SystemUI ui = core.getUI();
		GeoLayer layer = selectRasterLayer(lman, ui, res.getString("Select_RASTER_layer"));
		if (layer == null)
			return null;
		RasterGeometry rg = getRaster(layer, ui);
		if (rg == null)
			return null;

		Choice ch = new Choice();
		ch.addItem("3x3");
		ch.addItem("5x5");
		ch.addItem("7x7");
		Frame win = null;
		if (ui != null) {
			win = ui.getMainFrame();
		}
		if (win == null) {
			win = CManager.getAnyFrame();
		}
		// following text: "Set matrix size"
		OKDialog okd = new OKDialog(win, res.getString("Set_matrix_size"), true);
		okd.addContent(ch);
		okd.show();
		if (okd.wasCancelled())
			return null;

		int size;
		switch (ch.getSelectedIndex()) {
		case 0:
			size = 3;
			break;
		case 1:
			size = 5;
			break;
		case 2:
			size = 7;
			break;
		default:
			return null;
		}

		FreeformMatrix p = new FreeformMatrix(size);
		win = null;
		if (ui != null) {
			win = ui.getMainFrame();
		}
		if (win == null) {
			win = CManager.getAnyFrame();
		}
		// following text:"Set parameters"
		okd = new OKDialog(win, res.getString("Set_parameters"), true);
		okd.addContent(p);
		okd.show();
		if (okd.wasCancelled())
			return null;
		if (!p.update())
			return null;

		RasterGeometry filter = (RasterGeometry) rg.clone();
		int Rad = (size - 1) / 2;
		int x1, x2, y1, y2;
		double sum, val;
		boolean meaningful;

		float rsize = 1.0f * filter.Row * filter.Col;

		for (int yy = 0; yy < filter.Row; yy++) {
			for (int xx = 0; xx < filter.Col; xx++) {

				x1 = xx - Rad;
				x2 = xx + Rad;
				y1 = yy - Rad;
				y2 = yy + Rad;

				sum = 0;
				meaningful = false;
				for (int ry = y1; ry <= y2; ry++) {
					for (int rx = x1; rx <= x2; rx++) {
						try {
							val = rg.ras[rx][ry] * p.getRelativeValue(rx - xx, ry - yy);
						} catch (Exception ex) {
							val = rg.ras[xx][yy] * p.getRelativeValue(rx - xx, ry - yy);
						}
						if (!Double.isNaN(val)) {
							meaningful = true;
							sum += val;
						}
					}
				}

				if (Double.isInfinite(sum) || !meaningful) {
					filter.ras[xx][yy] = Float.NaN;
				} else {
					filter.ras[xx][yy] = (float) sum;
				}
			}
			if (ui != null) {
				int perc = Math.round(((yy + 1) * filter.Col) / rsize * 100);
				// following text:"Calculation: "+String.valueOf(perc)+"% ready"
				ui.showMessage(res.getString("Calculation_") + String.valueOf(perc) + res.getString("_ready"));
			}
		}

		filter.recalculateStatistics();
		return constructRasterLayer(filter, "Freeform filtered " + layer.getName());
	}
}
