package spade.analysis.vis3d;

//awt
import java.util.ResourceBundle;

import spade.analysis.system.DataKeeper;
import spade.analysis.system.ESDACore;
import spade.analysis.tools.DataAnalyser;
import spade.lib.lang.Language;
import spade.vis.dmap.DLayerManager;
import spade.vis.space.LayerManager;

public class Run3DView implements DataAnalyser {

	static ResourceBundle res = Language.getTextResource("spade.analysis.vis3d.Res");
	private ESDACore core = null;

	@Override
	public void run(ESDACore core) {
		this.core = core;
		//select the layer to show in 3D view
		DataKeeper dk = core.getDataKeeper();
		int mapN = 0;
		if (core.getUI() != null) {
			mapN = core.getUI().getCurrentMapN();
		}
		if (mapN < 0) {
			mapN = 0;
		}
		LayerManager lman = dk.getMap(mapN);
		if (lman == null || lman.getLayerCount() < 1) {
			if (core.getUI() != null) {
				// following text: "No map layers found!"
				core.getUI().showMessage(res.getString("No_map_layers_found_"), true);
			}
			return;
		}
		// Simple3DView cannot work now with foreign (non-CommonGIS) LayerManager:
		if (!(lman instanceof DLayerManager))
			return;
		Sample3DView view = new Sample3DView(core, (DLayerManager) lman);
		if (view != null && view.initOK) {
			view.registerAsMapListener(core.getUI().getMapViewer(mapN).getMapDrawer());
		}
		// following text:"Perspective view window started (Ver. 2.15a, 04.02.2003)"
		if (view != null && view.initOK) {
			core.getUI().showMessage(res.getString("Perspective_view"));
		}
	}

	@Override
	public boolean isValid(ESDACore esda) {
		return true;
	}
}
