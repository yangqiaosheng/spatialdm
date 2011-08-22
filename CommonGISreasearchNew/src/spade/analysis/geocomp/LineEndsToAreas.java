package spade.analysis.geocomp;

import java.awt.Frame;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.system.ESDACore;
import spade.analysis.system.SystemUI;
import spade.lib.basicwin.CManager;
import spade.lib.lang.Language;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.RealLine;
import spade.vis.geometry.RealPolyline;
import spade.vis.space.GeoLayer;
import spade.vis.space.LayerManager;
import spade.vis.space.SelectLayer;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 24-May-2007
 * Time: 11:23:52
 * Refers the ends of linear objects of a selected map layer to areas from
 * another map layers in which these ends fit. Adds 4 attributes to the
 * table: startID, startName, endID, endName.
 */
public class LineEndsToAreas extends GeoCalculator {
	static ResourceBundle res = Language.getTextResource("spade.analysis.geocomp.Res");

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
		GeoLayer layer = SelectLayer.selectLayer(lman, "L", "Select a layer with LINEAR objects", win);
		if (layer == null || !(layer instanceof DGeoLayer))
			return null;
		DGeoLayer lineLayer = (DGeoLayer) layer;
		layer = SelectLayer.selectLayer(lman, "A", "Select a layer with AREA objects", win);
		if (layer == null || !(layer instanceof DGeoLayer))
			return null;
		DGeoLayer areaLayer = (DGeoLayer) layer;
		DataTable table = null;
		if (!lineLayer.hasThematicData()) {
			table = constructTable(lineLayer);
			if (table == null) {
				if (ui != null) {
					// following text:"Cannot construct a table for the layer"
					ui.showMessage(res.getString("Cannot_construct_a"), true);
				}
				return null;
			}
		} else if (!(lineLayer.getThematicData() instanceof DataTable)) {
			// following text:"Illegal table type!
			if (ui != null) {
				ui.showMessage(res.getString("Illegal_table_type_"), true);
			}
			return null;
		} else {
			table = (DataTable) lineLayer.getThematicData();
		}

		boolean otherTable = !lineLayer.hasThematicData(table);
		if (otherTable) {
			lineLayer.receiveThematicData(table);
			lineLayer.setThematicFilter(table.getObjectFilter());
		}
		int startIdCN = -1, startNameCN = -1, endIdCN = -1, endNameCN = -1;
		table.addAttribute("Start ID in " + areaLayer.getName(), "startID_" + areaLayer.getEntitySetIdentifier(), AttributeTypes.character);
		startIdCN = table.getAttrCount() - 1;
		table.addAttribute("End ID in " + areaLayer.getName(), "endID_" + areaLayer.getEntitySetIdentifier(), AttributeTypes.character);
		endIdCN = table.getAttrCount() - 1;
		if (areaLayer.getHasLabels()) {
			table.addAttribute("Start name in " + areaLayer.getName(), "startName_" + areaLayer.getEntitySetIdentifier(), AttributeTypes.character);
			startNameCN = table.getAttrCount() - 1;
			table.addAttribute("End name in " + areaLayer.getName(), "endName_" + areaLayer.getEntitySetIdentifier(), AttributeTypes.character);
			endNameCN = table.getAttrCount() - 1;
		}
		for (int i = 0; i < lineLayer.getObjectCount(); i++) {
			DGeoObject lineObj = lineLayer.getObject(i);
			if (lineObj == null || lineObj.getGeometry() == null || lineObj.getData() == null || !(lineObj.getData() instanceof DataRecord)) {
				continue;
			}
			Geometry line = lineObj.getGeometry();
			float xs = Float.NaN, ys = Float.NaN, xe = Float.NaN, ye = Float.NaN;
			if (line instanceof RealLine) {
				RealLine rline = (RealLine) line;
				xs = rline.x1;
				ys = rline.y1;
				xe = rline.x2;
				ye = rline.y2;
			} else if (line instanceof RealPolyline) {
				RealPolyline pline = (RealPolyline) line;
				if (pline.p == null || pline.p.length < 2) {
					continue;
				}
				xs = pline.p[0].x;
				ys = pline.p[0].y;
				int k = pline.p.length - 1;
				xe = pline.p[k].x;
				ye = pline.p[k].y;
			}
			if (Float.isNaN(xs) || Float.isNaN(ys) || Float.isNaN(xe) || Float.isNaN(ye)) {
				continue;
			}
			DataRecord rec = (DataRecord) lineObj.getData();
			boolean startFound = false, endFound = false;
			for (int j = 0; j < areaLayer.getObjectCount() && (!startFound || !endFound); j++) {
				DGeoObject arObj = areaLayer.getObject(j);
				if (arObj == null || arObj.getGeometry() == null) {
					continue;
				}
				Geometry area = arObj.getGeometry();
				if (!startFound && area.contains(xs, ys, 0f, true)) {
					rec.setAttrValue(arObj.getIdentifier(), startIdCN);
					if (startNameCN >= 0) {
						rec.setAttrValue(arObj.getLabel(), startNameCN);
					}
					startFound = true;
				}
				if (!endFound && area.contains(xe, ye, 0f, true)) {
					rec.setAttrValue(arObj.getIdentifier(), endIdCN);
					if (endNameCN >= 0) {
						rec.setAttrValue(arObj.getLabel(), endNameCN);
					}
					endFound = true;
				}
			}
		}
		AttrSpec asp = new AttrSpec();
		asp.table = table;
		asp.layer = lineLayer;
		asp.attrIds = new Vector(4, 1);
		asp.attrIds.addElement(table.getAttributeId(startIdCN));
		asp.attrIds.addElement(table.getAttributeId(endIdCN));
		if (startNameCN >= 0) {
			asp.attrIds.addElement(table.getAttributeId(startNameCN));
		}
		if (endNameCN >= 0) {
			asp.attrIds.addElement(table.getAttributeId(endNameCN));
		}
		return asp;
	}
}
