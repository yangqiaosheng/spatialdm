package spade.analysis.geocomp;

import java.util.ResourceBundle;

import spade.analysis.system.ESDACore;
import spade.analysis.system.SystemUI;
import spade.lib.lang.Language;
import spade.vis.geometry.RasterGeometry;
import spade.vis.space.GeoLayer;
import spade.vis.space.LayerManager;

/**
* Makes a copy of a raster layer
*/
public class RasterCloner extends GeoCalculator {
	static ResourceBundle res = Language.getTextResource("spade.analysis.geocomp.Res");

	/**
	* Makes a copy of a raster layer. The arguments are a layer manager (a
	* GeoCalculator must itself care about selection of a layer or layers of
	* appropriate type) and SystemUI (to be used for displaying messages and
	* finding an owner frame for dialogs)
	* If calculation was successful, returns the copy of the layer (an instance
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
		return constructRasterLayer((RasterGeometry) rg.clone(), "Copy of " + layer.getName());
	}
}
