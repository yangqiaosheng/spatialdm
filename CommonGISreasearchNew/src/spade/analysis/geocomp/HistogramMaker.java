package spade.analysis.geocomp;

import java.util.ResourceBundle;

import spade.analysis.plot.Histogram;
import spade.analysis.system.ESDACore;
import spade.analysis.system.SystemUI;
import spade.lib.lang.Language;
import spade.vis.geometry.RasterGeometry;
import spade.vis.space.GeoLayer;
import spade.vis.space.LayerManager;

/**
* Builds a histogram showing statistical distribution of values in a raster layer
*/
public class HistogramMaker extends GeoCalculator {
	static ResourceBundle res = Language.getTextResource("spade.analysis.geocomp.Res");

	/**
	* Builds a histogram showing statistical distribution of values in a
	* raster layer. The arguments are a layer manager (a GeoCalculator
	* must itself care about selection of a layer or layers of appropriate type)
	* and SystemUI (to be used for displaying messages and finding an owner
	* frame for dialogs)
	* If calculation was successful, returns the histogram generated (a component).
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
		Histogram hg = new Histogram(rg.minV, rg.maxV);

		float rsize = 1.0f * rg.Row * rg.Col;

		for (int yy = 0; yy < rg.Row; yy++) {
			for (int xx = 0; xx < rg.Col; xx++) {
				hg.addValue(rg.ras[xx][yy]);
			}
			if (ui != null) {
				int perc = Math.round(((yy + 1) * rg.Col) / rsize * 100);
				// following string: "Calculation: "+String.valueOf(perc)+"% ready"
				ui.showMessage(res.getString("Calculation_") + String.valueOf(perc) + res.getString("_ready"));
			}
		}
		// following string: layer.getName()+": value frequencies"
		hg.setName(layer.getName() + res.getString("_value_frequencies"));
		return hg;
	}
}
