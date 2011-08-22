package spade.vis.dmap;

import java.beans.PropertyChangeEvent;

import spade.vis.database.AttributeDataPortion;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.database.LayerData;
import spade.vis.database.SpatialEntity;
import spade.vis.geometry.RealRectangle;
import spade.vis.space.GeoLayer;

/**
 * Created by IntelliJ IDEA.
 * User: DIvan
 * Date: Apr 15, 2004
 * Time: 5:34:16 PM
 */
public class DBridgeLayer extends DGeoLayer {
	@Override
	public void propertyChange(PropertyChangeEvent pce) {
		if (pce.getPropertyName().equals("data_updated")) {
			System.out.println("Rebuilding layer...");
			AttributeDataPortion table = getThematicData();
			if (table != null && table instanceof DataTable) {
				rebuildLayer((DataTable) table);
			} else {
				System.out.println("Bad table!");
			}
			notifyPropertyChange("ObjectSet", null, null);
		} else {
			super.propertyChange(pce);
		}
	}

	protected void rebuildLayer(DataTable table) {
		LayerData ldata = new LayerData();

		int ax1 = table.findAttrByName("x1");
		int ay1 = table.findAttrByName("y1");
		int ax2 = table.findAttrByName("x2");
		int ay2 = table.findAttrByName("y2");

		if (ax1 < 0 || ax2 < 0 || ay1 < 0 || ay2 < 0)
			return;

		for (int i = 0; i < table.getDataItemCount(); i++) {
			DataRecord rec = table.getDataRecord(i);
			SpatialEntity spe = new SpatialEntity(rec.getId());
			RealRectangle sq = new RealRectangle((float) rec.getNumericAttrValue(ax1), (float) rec.getNumericAttrValue(ay1), (float) rec.getNumericAttrValue(ax2), (float) rec.getNumericAttrValue(ay2));
			spe.setGeometry(sq);
			spe.setThematicData(rec);
			ldata.addItemSimple(spe);
		}
		ldata.setHasAllData(true);
		receiveSpatialData(ldata);
	}

	@Override
	public GeoLayer makeCopy() {
		DGeoLayer layer = new DBridgeLayer();
		copyTo(layer);
		return layer;
	}

	@Override
	protected boolean drawContoursOfInactiveObjects() {
		return false;
	}
}
