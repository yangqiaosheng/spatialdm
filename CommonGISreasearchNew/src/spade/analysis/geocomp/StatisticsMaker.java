package spade.analysis.geocomp;

import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.plot.StatisticsMultiPanel;
import spade.analysis.plot.StatisticsPanel;
import spade.analysis.system.ESDACore;
import spade.analysis.system.SystemUI;
import spade.lib.lang.Language;
import spade.lib.util.FloatArray;
import spade.vis.geometry.RasterGeometry;
import spade.vis.space.GeoLayer;
import spade.vis.space.LayerManager;

/**
* Builds a histogram showing statistical distribution of values in a raster layer
*/
public class StatisticsMaker extends GeoCalculator {
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
		// following string: "Select RASTER layers"
		Vector layers = selectRasterLayers(lman, ui, res.getString("Select_RASTER_layers"));
		//GeoLayer layer=selectRasterLayer(lman,ui,"Select RASTER layer");
		if (layers == null)
			return null;
		if (layers.size() == 1) {
			GeoLayer layer = (GeoLayer) layers.elementAt(0);
			RasterGeometry rg = getRaster(layer, ui);
			if (rg == null)
				return null;

			int isize = rg.Row * rg.Col;
			float rsize = 1.0f * isize;

			FloatArray fa = new FloatArray(isize, 10);

			for (int yy = 0; yy < rg.Row; yy++) {
				for (int xx = 0; xx < rg.Col; xx++) {
					fa.addElement(rg.ras[xx][yy]);
				}
				if (ui != null) {
					int perc = Math.round(((yy + 1) * rg.Col) / rsize * 100);
					// following string:"Calculation: "+String.valueOf(perc)+"% ready"
					ui.showMessage(res.getString("Calculation_") + String.valueOf(perc) + res.getString("_ready"));
				}
			}
			StatisticsPanel sp = new StatisticsPanel(fa);
			// following string:": value frequencies"
			sp.setName(layer.getName() + res.getString("_value_frequencies"));
			sp.setMethodId("statistics");
			core.getSupervisor().registerTool(sp);
			return sp;
		} else {
			Vector data = new Vector(2 * layers.size(), 2);
			for (int i = 0; i < layers.size(); i++) {
				GeoLayer layer = (GeoLayer) layers.elementAt(i);
				RasterGeometry rg = getRaster(layer, ui);
				if (rg == null)
					return null;

				int isize = rg.Row * rg.Col;
				float rsize = 1.0f * isize;

				FloatArray fa = new FloatArray(isize, 10);

				for (int yy = 0; yy < rg.Row; yy++) {
					for (int xx = 0; xx < rg.Col; xx++) {
						fa.addElement(rg.ras[xx][yy]);
					}
					if (ui != null) {
						int perc = Math.round(((yy + 1) * rg.Col) / rsize * 100);
						// following string:"Calculation: layer "+i+" "+String.valueOf(perc)+"% ready"
						ui.showMessage(res.getString("Calculation_layer") + i + " " + String.valueOf(perc) + res.getString("_ready"));
					}
				}
				data.addElement(new String(layer.getName()));
				data.addElement(fa);
			}
			return new StatisticsMultiPanel(data);
		}
	}
}
