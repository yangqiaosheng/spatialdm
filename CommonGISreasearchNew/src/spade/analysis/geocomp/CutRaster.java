package spade.analysis.geocomp;

import java.awt.Frame;
import java.util.ResourceBundle;

import spade.analysis.system.ESDACore;
import spade.analysis.system.SystemUI;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.OKDialog;
import spade.lib.lang.Language;
import spade.vis.geometry.RasterGeometry;
import spade.vis.map.MapDraw;
import spade.vis.map.MapViewer;
import spade.vis.space.GeoLayer;
import spade.vis.space.LayerManager;

/**
* Cuts a rectangular part of a raster layer with different parameters
*/
public class CutRaster extends GeoCalculator {
	static ResourceBundle res = Language.getTextResource("spade.analysis.geocomp.Res");

	/**
	* Changes parameters of a raster layer. The arguments are a layer manager (a
	* GeoCalculator must itself care about selection of a layer or layers of
	* appropriate type) and SystemUI (to be used for displaying messages and
	* finding an owner frame for dialogs)
	* If calculation was successful, returns the copy of the layer (an instance
	* of DGridLayer).
	*/
	@Override
	public Object doCalculation(LayerManager lman, ESDACore core) {
		SystemUI ui = core.getUI();
		// following text:
		GeoLayer layer = selectRasterLayer(lman, ui, res.getString("Select_RASTER_layer"));
		if (layer == null)
			return null;
		RasterGeometry rg = getRaster(layer, ui);
		if (rg == null)
			return null;

		if (ui == null || ui.getCurrentMapN() < 0) {
			ui.showMessage(res.getString("No map found!"), true);
			return null;
		}
		MapViewer mview = ui.getMapViewer(ui.getCurrentMapN());
		if (mview == null) {
			ui.showMessage(res.getString("No map found!"));
			return null;
		}
		MapDraw map = mview.getMapDrawer();
		if (map == null) {
			ui.showMessage(res.getString("No map found!"));
			return null;
		}

		ParameterSheet p = new ParameterSheet(rg, map, lman);
		Frame win = null;
		if (ui != null) {
			win = ui.getMainFrame();
		}
		if (win == null) {
			win = CManager.getAnyFrame();
		}
		// following text:
		OKDialog okd = new OKDialog(win, res.getString("Set_parameters"), true);
		okd.addContent(p);
		okd.show();
		p.clearHighlighting();
		if (okd.wasCancelled())
			return null;

		RasterGeometry par = new RasterGeometry();

		try {
			p.updateGeometry(par);
			par.maxV = rg.maxV;
			par.minV = rg.minV;
		} catch (Exception ex) {
			// following text:
			if (ui != null) {
				ui.showMessage(res.getString("Invalid_parameters_"), true);
			}
			return null;
		}

		par.ras = new float[par.Col][par.Row];
/*
    if(rg.Col==par.Col && rg.Row==par.Row)
      for (int j=0; j<par.Row; j++)
        for (int i=0; i<par.Col; i++)
          par.ras[i][j]=rg.ras[i][j];
    else
*/
		for (int yy = 0; yy < par.Row; yy++) {
			for (int xx = 0; xx < par.Col; xx++) {
//        par.ras[xx][yy] = (float)rg.getInterpolatedValue(xx*rg.Col/par.Col,yy*rg.Row/par.Row);
//        par.ras[xx][yy] = (float)rg.getInterpolatedValue(rg.getGridX(par.getWorldX(xx)),rg.getGridY(par.getWorldY(yy)));
				par.ras[xx][yy] = rg.getAggregatedValue(par.getWorldX(xx), par.getWorldY(yy), Math.abs(par.DX), Math.abs(par.DY));
			}
		}

		return constructRasterLayer(par, "Part of " + layer.getName());
	}
}
