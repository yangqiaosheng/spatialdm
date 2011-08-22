package spade.analysis.geocomp;

import java.awt.Frame;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.system.ESDACore;
import spade.analysis.system.SystemUI;
import spade.lib.basicwin.CManager;
import spade.lib.lang.Language;
import spade.lib.util.IdMaker;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.database.SpatialEntity;
import spade.vis.dmap.DGeoObject;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.MultiGeometry;
import spade.vis.geometry.RealLine;
import spade.vis.geometry.RealPoint;
import spade.vis.geometry.RealPolyline;
import spade.vis.geometry.RealRectangle;
import spade.vis.space.GeoLayer;
import spade.vis.space.LayerManager;
import spade.vis.space.SelectLayer;

/**
* Calculates attributes of vector objects on the basis of a raster layer
*/
public class CoordsFromVectors extends GeoCalculator {
	static ResourceBundle res = Language.getTextResource("spade.analysis.geocomp.Res");

	/**
	* Calculates attributes of polygon objects on the basis of a point layer.
	* The arguments are a layer manager (a GeoCalculator must itself care about
	* selection of a layer or layers of appropriate type, in this case a layer
	* with point objects) and SystemUI (to be used for displaying messages and
	* finding an owner frame for dialogs)
	* If calculation was successful, addt the generated attribute to the table
	* attached to the vector layer and returns its identifier. If there was
	* no table yet, creates the table.
	*/
	@Override
	public Object doCalculation(LayerManager lman, ESDACore core) {
		SystemUI ui = core.getUI();
		//propose the user to select a layer with vector objects
		if (lman == null)
			return null;
		Frame win = null;
		if (ui != null) {
			win = ui.getMainFrame();
		}
		if (win == null) {
			win = CManager.getAnyFrame();
		}
		// following text:"Select VECTOR layer"
		GeoLayer layer = SelectLayer.selectLayer(lman, "PAL", res.getString("Select_VECTOR_layer"), win);
		if (layer == null)
			return null;
		DataTable table = null;
		if (!layer.hasThematicData()) {
			table = constructTable(layer);
			if (table == null) {
				if (ui != null) {
					// following text:"Cannot construct a table for the layer"
					ui.showMessage(res.getString("Cannot_construct_a"), true);
				}
				return null;
			}
		} else if (!(layer.getThematicData() instanceof DataTable)) {
			// following text:"Illegal table type!
			if (ui != null) {
				ui.showMessage(res.getString("Illegal_table_type_"), true);
			}
			return null;
		} else {
			table = (DataTable) layer.getThematicData();
		}

		AttrSpec asp = new AttrSpec();
		asp.layer = layer;
		asp.table = table;
		asp.attrIds = new Vector(10, 10);

		int firstIdx = table.getAttrCount();
		if (layer.getType() == Geometry.point) {
			table.addAttribute("X", IdMaker.makeId("X", table), AttributeTypes.real);
			table.addAttribute("Y", IdMaker.makeId("Y", table), AttributeTypes.real);
		} else if (layer.getType() == Geometry.line) {
			table.addAttribute("start_X", IdMaker.makeId("startX", table), AttributeTypes.real);
			table.addAttribute("start_Y", IdMaker.makeId("startY", table), AttributeTypes.real);
			table.addAttribute("end_X", IdMaker.makeId("endX", table), AttributeTypes.real);
			table.addAttribute("end_Y", IdMaker.makeId("endY", table), AttributeTypes.real);
			table.addAttribute("mean_X", IdMaker.makeId("meanX", table), AttributeTypes.real);
			table.addAttribute("mean_Y", IdMaker.makeId("meanY", table), AttributeTypes.real);
		}
		int rectIdx0 = table.getAttrCount();
		if (layer.getType() == Geometry.line || layer.getType() == Geometry.area) {
			table.addAttribute("min_X", IdMaker.makeId("minX", table), AttributeTypes.real);
			table.addAttribute("max_X", IdMaker.makeId("maxX", table), AttributeTypes.real);
			table.addAttribute("min_Y", IdMaker.makeId("minY", table), AttributeTypes.real);
			table.addAttribute("max_Y", IdMaker.makeId("maxY", table), AttributeTypes.real);
			table.addAttribute("center_X", IdMaker.makeId("centerX", table), AttributeTypes.real);
			table.addAttribute("center_Y", IdMaker.makeId("centerY", table), AttributeTypes.real);
		}
		for (int i = firstIdx; i < table.getAttrCount(); i++) {
			asp.attrIds.addElement(table.getAttributeId(i));
		}

		for (int i = 0; i < layer.getObjectCount(); i++) {
			DGeoObject gobj = (DGeoObject) layer.getObjectAt(i);
			if (gobj == null || gobj.getGeometry() == null) {
				continue;
			}
			DataRecord rec = (DataRecord) gobj.getData();
			if (rec == null) {
				continue;
			}
			Geometry geom = gobj.getGeometry();
			if (geom == null) {
				continue;
			}
			if (layer.getType() == Geometry.point) {
				RealPoint p = (geom instanceof RealPoint) ? (RealPoint) geom : SpatialEntity.getCentre(geom);
				rec.setNumericAttrValue(p.x, firstIdx);
				rec.setNumericAttrValue(p.y, firstIdx + 1);
				continue;
			}
			if (layer.getType() == Geometry.line) {
				putLineData(geom, rec, firstIdx, true);
			}
			RealRectangle r = gobj.getBounds();
			if (r == null) {
				continue;
			}
			rec.setNumericAttrValue(r.rx1, rectIdx0);
			rec.setNumericAttrValue(r.rx2, rectIdx0 + 1);
			rec.setNumericAttrValue(r.ry1, rectIdx0 + 2);
			rec.setNumericAttrValue(r.ry2, rectIdx0 + 3);
			float c[] = geom.getCentroid();
			if (c == null) {
				c = new float[2];
				c[0] = (r.rx1 + r.rx2) / 2;
				c[1] = (r.ry1 + r.ry2) / 2;
			}
			rec.setNumericAttrValue(c[0], rectIdx0 + 4);
			rec.setNumericAttrValue(c[1], rectIdx0 + 5);
		}

		return asp;
	}

	protected void putLineData(Geometry geom, DataRecord rec, int firstIdx, boolean putMeanPoint) {
		if (geom == null || rec == null || firstIdx < 0)
			return;
		if (geom instanceof RealLine) {
			RealLine line = (RealLine) geom;
			rec.setNumericAttrValue(line.x1, firstIdx);
			rec.setNumericAttrValue(line.y1, firstIdx + 1);
			rec.setNumericAttrValue(line.x2, firstIdx + 2);
			rec.setNumericAttrValue(line.y2, firstIdx + 3);
		} else if (geom instanceof RealPolyline) {
			RealPolyline poly = (RealPolyline) geom;
			if (poly.p == null || poly.p.length < 1)
				return;
			RealPoint p = poly.p[0];
			rec.setNumericAttrValue(p.x, firstIdx);
			rec.setNumericAttrValue(p.y, firstIdx + 1);
			p = poly.p[poly.p.length - 1];
			rec.setNumericAttrValue(p.x, firstIdx + 2);
			rec.setNumericAttrValue(p.y, firstIdx + 3);
		} else if (geom instanceof MultiGeometry) {
			MultiGeometry multi = (MultiGeometry) geom;
			if (multi.getPartsCount() < 1)
				return;
			putLineData(multi.getPart(0), rec, firstIdx, false);
		}
		if (putMeanPoint) {
			RealPoint p = getMeanPoint(geom);
			if (p != null) {
				rec.setNumericAttrValue(p.x, firstIdx + 4);
				rec.setNumericAttrValue(p.y, firstIdx + 5);
			}
		}
	}

	protected RealPoint getMeanPoint(Geometry geom) {
		if (geom == null)
			return null;
		if (geom instanceof RealPoint)
			return (RealPoint) geom;
		if (geom instanceof RealLine) {
			RealLine line = (RealLine) geom;
			return new RealPoint((line.x1 + line.x2) / 2, (line.y1 + line.y2) / 2);
		}
		if (geom instanceof RealRectangle) {
			RealRectangle r = (RealRectangle) geom;
			return new RealPoint((r.rx1 + r.rx2) / 2, (r.ry1 + r.ry2) / 2);
		}
		if (geom instanceof RealPolyline) {
			RealPolyline poly = (RealPolyline) geom;
			if (poly.p == null || poly.p.length < 1)
				return null;
			float xSum = 0, ySum = 0;
			for (RealPoint p : poly.p) {
				xSum += p.x;
				ySum += p.y;
			}
			return new RealPoint(xSum / poly.p.length, ySum / poly.p.length);
		}
		if (geom instanceof MultiGeometry) {
			MultiGeometry multi = (MultiGeometry) geom;
			if (multi.getPartsCount() < 1)
				return null;
			int count = 0;
			float xSum = 0, ySum = 0;
			for (int i = 0; i < multi.getPartsCount(); i++) {
				int np = getPointCount(multi.getPart(i));
				if (np < 1) {
					continue;
				}
				RealPoint p = getMeanPoint(multi.getPart(i));
				if (p == null) {
					continue;
				}
				count += np;
				xSum += p.x * np;
				ySum += p.y * np;
			}
			return new RealPoint(xSum / count, ySum / count);
		}
		return null;
	}

	protected int getPointCount(Geometry geom) {
		if (geom == null)
			return 0;
		if (geom instanceof RealPoint)
			return 1;
		if (geom instanceof RealLine)
			return 2;
		if (geom instanceof RealRectangle)
			return 4;
		if (geom instanceof RealPolyline) {
			RealPolyline poly = (RealPolyline) geom;
			if (poly.p == null)
				return 0;
			return poly.p.length;
		}
		if (geom instanceof MultiGeometry) {
			MultiGeometry multi = (MultiGeometry) geom;
			if (multi.getPartsCount() < 1)
				return 0;
			int count = 0;
			for (int i = 0; i < multi.getPartsCount(); i++) {
				count += getPointCount(multi.getPart(i));
			}
			return count;
		}
		return 0;
	}
}
