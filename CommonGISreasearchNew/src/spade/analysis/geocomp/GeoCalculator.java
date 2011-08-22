package spade.analysis.geocomp;

import java.awt.Frame;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.system.ESDACore;
import spade.analysis.system.SystemUI;
import spade.lib.basicwin.CManager;
import spade.lib.lang.Language;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.database.LayerData;
import spade.vis.database.SpatialEntity;
import spade.vis.dmap.DGeoObject;
import spade.vis.dmap.DGridLayer;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.RasterGeometry;
import spade.vis.space.GeoLayer;
import spade.vis.space.LayerManager;
import spade.vis.space.SelectLayer;

/**
* This is the abstract class to be extended by all classes doing computations
* or transformations with map layers.
*/
public abstract class GeoCalculator {
	static ResourceBundle res = Language.getTextResource("spade.analysis.geocomp.Res");

	/**
	* Performs calculations. The arguments are a layer manager (a GeoCalculator
	* must itself care about selection of a layer or layers of appropriate type)
	* and SystemUI (to be used for displaying messages and finding an owner
	* frame for dialogs)
	* If calculation was successful, returns the result. The result may be,
	* for example, a new layer to be added to the map (a GeoCalculator
	* MUST NOT add any layer itself!), or a graph (instance of java.awt.Component;
	* again, a GeoCalculator MUST NOT display it itself!), or a column with
	* values to be added to a table.
	*/
	public abstract Object doCalculation(LayerManager lm, ESDACore core);

	/**
	* A utility method offering the user to select a single raster layer
	* from a map. Returns the selected layer.
	*/
	protected static GeoLayer selectRasterLayer(LayerManager lman, SystemUI ui, String prompt) {
		if (lman == null)
			return null;
		Frame win = null;
		if (ui != null) {
			win = ui.getMainFrame();
		}
		if (win == null) {
			win = CManager.getAnyFrame();
		}
		return SelectLayer.selectLayer(lman, Geometry.raster, prompt, win);
	}

	protected static Vector selectRasterLayers(LayerManager lman, SystemUI ui, String prompt) {
		if (lman == null)
			return null;
		Frame win = null;
		if (ui != null) {
			win = ui.getMainFrame();
		}
		if (win == null) {
			win = CManager.getAnyFrame();
		}
		return SelectLayer.selectLayers(lman, Geometry.raster, prompt, win);
	}

	/**
	* Retrieves a RasterGeometry from a raster layer. May return null if
	* there is no data or the layer has not the appropriate type.
	*/
	protected static RasterGeometry getRaster(GeoLayer layer, SystemUI ui) {
		if (layer == null)
			return null;
		if (layer.getObjectCount() < 1) {
			if (ui != null) {
				ui.showMessage(res.getString("No_data_in_layer") + layer.getName(), true);
			}
			return null;
		}
		if (!(layer.getObjectAt(0) instanceof DGeoObject)) {
			if (ui != null) {
				ui.showMessage(res.getString("Incorrect_object_type"), true);
			}
			return null;
		}
		DGeoObject gobj = (DGeoObject) layer.getObjectAt(0);
		if (gobj.getGeometry() == null) {
			if (ui != null) {
				ui.showMessage(res.getString("No_data_in_layer") + layer.getName(), true);
			}
			return null;
		}
		if (!(gobj.getGeometry() instanceof RasterGeometry)) {
			if (ui != null) {
				ui.showMessage(res.getString("Incorrect_layer_type"), true);
			}
			return null;
		}
		RasterGeometry rg = (RasterGeometry) gobj.getGeometry();
		if (rg.Col < 2 || rg.Row < 2) {
			if (ui != null) {
				ui.showMessage(res.getString("No_data_in_layer") + layer.getName(), true);
			}
			return null;
		}
		return rg;
	}

	/**
	* Produces a raster layer for the given RasterGeometry
	*/
	protected static DGridLayer constructRasterLayer(RasterGeometry rg, String name) {
		if (rg == null)
			return null;
		SpatialEntity nse = new SpatialEntity("raster");
		nse.setGeometry(rg);
		LayerData nld = new LayerData();
		nld.setBoundingRectangle(rg.rx1, rg.ry1, rg.rx2, rg.ry2);
		nld.addDataItem(nse);
		nld.setHasAllData(true);
		DGridLayer ngl = new DGridLayer();
		ngl.receiveSpatialData(nld);
		ngl.setName(name);
		return ngl;
	}

	/**
	* Constructs a table (without attributes) for the given vector layer
	*/
	protected static DataTable constructTable(GeoLayer gl) {
		if (gl == null || gl.getObjectCount() < 1)
			return null;
		DataTable table = new DataTable();
		table.setName(gl.getName());
		gl.receiveThematicData(table);
		for (int i = 0; i < gl.getObjectCount(); i++) {
			DGeoObject gobj = (DGeoObject) gl.getObjectAt(i);
			if (gobj == null) {
				continue;
			}
			DataRecord data = new DataRecord(gobj.getIdentifier(), gobj.getLabel());
			table.addDataRecord(data);
			gobj.setThematicData(data);
		}
		return table;
	}
}
