package data_load.readers;

import spade.lib.util.CopyFile;
import spade.vis.database.LayerData;
import spade.vis.database.SpatialEntity;
import spade.vis.database.ThematicDataItem;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DQuadTreeLayer;
import spade.vis.geometry.RealRectangle;

/**
* Extracts a description of the top level of a QuadTree layer from a csv file.
*/
public class QuadTreeFromCSVReader extends ASCIIReader {
	/**
	* Returns the map layer constructed from the coordinates contained in the
	* table (if any). If the table contains no coordinates, returns null.
	* If the table has not been loaded yet, the reader constructs a layer without
	* objects and sets itself as the  data supplier of the layer. Then the data
	* will be loaded when the layer is first drawn.
	*/
	@Override
	public DGeoLayer getMapLayer() {
		if (layer != null)
			return layer;
		if (spec == null || spec.xCoordFieldName == null || spec.yCoordFieldName == null)
			return null;
		DQuadTreeLayer qtLayer = new DQuadTreeLayer();
		layer = qtLayer;
		layer.setDataSource(spec);
		if (spec.name != null) {
			layer.setName(spec.name);
		} else if (spec.source != null) {
			layer.setName(CopyFile.getName(spec.source));
		}
		if (data != null) {
			layer.receiveSpatialData(data);
		} else {
			layer.setDataSupplier(this);
		}
		return layer;
	}

	/**
	* If the table contains coordinates of geographical objects, this method
	* constructs the objects and links them to the corresponding thematic data.
	*/
	@Override
	protected LayerData tryGetGeoObjects() {
		if (table == null)
			return null;
		if (spec.xCoordFieldName != null && spec.yCoordFieldName != null) {
			//form a geo layer with point objects
			int xfn = table.getAttrIndex(spec.xCoordFieldName), yfn = table.getAttrIndex(spec.yCoordFieldName);
			if (xfn < 0 || yfn < 0) {
				showMessage(res.getString("Could_not_get_points") + spec.source + res.getString("_no_coord_"), true);
				return null;
			}
			//determine the grid step
			float stepX = 0, stepY = 0;
			double x0 = table.getNumericAttrValue(xfn, 0), y0 = table.getNumericAttrValue(yfn, 0);
			for (int i = 1; i < table.getDataItemCount() && (stepX <= 0 || stepY <= 0); i++) {
				if (stepX <= 0) {
					double x = table.getNumericAttrValue(xfn, i);
					if (x > x0) {
						stepX = (float) (x - x0);
					}
				}
				if (stepY <= 0) {
					double y = table.getNumericAttrValue(yfn, i);
					if (stepY <= 0) {
						stepY = (float) (y - y0);
					}
				}
			}
			//construct geographical objects with geometry type RealRectangle
			LayerData ld = new LayerData();
			for (int i = 0; i < table.getDataItemCount(); i++) {
				RealRectangle rr = new RealRectangle();
				rr.rx1 = (float) table.getNumericAttrValue(xfn, i);
				rr.ry1 = (float) table.getNumericAttrValue(yfn, i);
				rr.rx2 = rr.rx1 + stepX;
				rr.ry2 = rr.ry1 + stepY;
				SpatialEntity spe = new SpatialEntity(table.getDataItemId(i));
				spe.setGeometry(rr);
				spe.setThematicData((ThematicDataItem) table.getDataItem(i));
				spe.setName(table.getDataItemName(i));
				ld.addItemSimple(spe);
				if ((i + 1) % 50 == 0) {
					showMessage(res.getString("constr_point") + (i + 1) + res.getString("_obj_constructed"), false);
				}
			}
			ld.setHasAllData(true);
			return ld;
		}
		return null;
	}
}