package spade.analysis.geocomp;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.util.Vector;

import spade.analysis.system.ESDACore;
import spade.analysis.system.SystemUI;
import spade.analysis.tools.moves.PointsInCircle;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.Dialogs;
import spade.lib.basicwin.OKDialog;
import spade.lib.util.BubbleSort;
import spade.lib.util.IdMaker;
import spade.lib.util.IntArray;
import spade.lib.util.StringUtil;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.database.SpatialEntity;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;
import spade.vis.geometry.GeoComp;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.MultiGeometry;
import spade.vis.geometry.RealPoint;
import spade.vis.geometry.RealPolyline;
import spade.vis.geometry.RealRectangle;
import spade.vis.space.GeoLayer;
import spade.vis.space.LayerManager;
import spade.vis.space.SelectLayer;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Oct 13, 2010
 * Time: 6:28:14 PM
 * Builds circles (approximated by polygons) around groups of point objects
 */
public class CirclesBuilder extends GeoCalculator implements spade.lib.util.Comparator {
	protected ESDACore core = null;

	/**
	 * Builds circles (approximated by polygons) around groups of point objects
	 * @return the created layer with circles
	 */
	@Override
	public Object doCalculation(LayerManager lman, ESDACore core) {
		this.core = core;
		SystemUI ui = core.getUI();
		//propose the user to select a layer with point objects
		if (lman == null)
			return false;
		Frame win = null;
		if (ui != null) {
			win = ui.getMainFrame();
		}
		if (win == null) {
			win = CManager.getAnyFrame();
		}
		GeoLayer pl = SelectLayer.selectLayer(lman, Geometry.point, "Select a layer with points:", win);
		if (pl == null || !(pl instanceof DGeoLayer))
			return false;
		DGeoLayer pointLayer = (DGeoLayer) pl;
		DataTable pointTable = (DataTable) pointLayer.getThematicData();
		int colN = -1;
		if (pointTable != null) {
			//ask the user which table column specifies the groups of points
			IntArray aIdxs = new IntArray(20, 10);
			for (int i = 0; i < pointTable.getAttrCount(); i++) {
				Attribute at = pointTable.getAttribute(i);
				if (at.isClassification() || at.getType() == AttributeTypes.character || at.getType() == AttributeTypes.logical || at.getType() == AttributeTypes.integer) {
					aIdxs.addElement(i);
				}
			}
			if (aIdxs.size() > 0) {
				List list = new List(Math.max(10, Math.min(aIdxs.size(), 3)));
				for (int i = 0; i < aIdxs.size(); i++) {
					list.add(pointTable.getAttributeName(aIdxs.elementAt(i)));
				}
				list.select(aIdxs.size() - 1);
				Panel mainP = new Panel(new BorderLayout());
				mainP.add(new Label("Select the table column defining the classes or clusters:"), BorderLayout.NORTH);
				mainP.add(list, BorderLayout.CENTER);
				OKDialog dia = new OKDialog(core.getUI().getMainFrame(), "Select column with classes", true);
				dia.addContent(mainP);
				dia.show();
				if (!dia.wasCancelled()) {
					int k = list.getSelectedIndex();
					if (k >= 0) {
						colN = aIdxs.elementAt(k);
					}
				}
			}
		}
		if (pointTable == null || colN < 0)
			return null;
		boolean geo = pointLayer.isGeographic();
		float geoFactorX = 1f, geoFactorY = 1f;
		RealRectangle r = pointLayer.getWholeLayerBounds();
		if (r == null) {
			r = pointLayer.getCurrentLayerBounds();
		}
		if (geo && r != null) {
			double wh[] = DGeoLayer.getExtentXY(pointLayer);
			if (wh == null || wh[0] <= 0 || wh[1] <= 0)
				return null;
			float width = (float) wh[0], height = (float) wh[1];
			geoFactorX = width / (r.rx2 - r.rx1);
			geoFactorY = height / (r.ry2 - r.ry1);
		}

		Vector<String> classes = null;
		Vector<PointsInCircle> groups = null;
		IntArray iar = new IntArray(1, 1);
		iar.addElement(colN);
		classes = pointTable.getAllValuesInColumnsAsStrings(iar);
		if (classes == null || classes.size() < 1) {
			showMessage("No values in the table column!", true);
			return null;
		}
		if (classes.size() > 1) {
			BubbleSort.sort(classes, this);
		}
		groups = new Vector<PointsInCircle>(classes.size(), 1);
		for (int i = 0; i < classes.size(); i++) {
			groups.addElement(new PointsInCircle(geoFactorX, geoFactorY));
		}
		for (int i = 0; i < pointLayer.getObjectCount(); i++) {
			if (!pointLayer.isObjectActive(i)) {
				continue;
			}
			DGeoObject pObj = pointLayer.getObject(i);
			Geometry geom = pObj.getGeometry();
			if (geom == null) {
				continue;
			}
			if (pObj.getData() == null) {
				continue;
			}
			String val = pObj.getData().getAttrValueAsString(colN);
			if (val == null) {
				continue;
			}
			int cIdx = StringUtil.indexOfStringInVectorIgnoreCase(val, classes);
			if (cIdx < 0) {
				continue;
			}
			PointsInCircle group = groups.elementAt(cIdx);
			if (geom instanceof RealPoint) {
				group.addPoint((RealPoint) geom);
			} else if (geom instanceof MultiGeometry) {
				MultiGeometry mg = (MultiGeometry) geom;
				for (int j = 0; j < mg.getPartsCount(); j++) {
					geom = mg.getPart(j);
					if (geom instanceof RealPoint) {
						group.addPoint((RealPoint) geom);
					} else {
						group.addPoint(SpatialEntity.getCentre(geom));
					}
				}
			} else {
				group.addPoint(SpatialEntity.getCentre(geom));
			}
		}
		boolean somePointsFound = false;
		for (int i = 0; i < groups.size() && !somePointsFound; i++) {
			somePointsFound = groups.elementAt(i).getPointCount() > 0;
		}
		if (!somePointsFound) {
			showMessage("Could not find any group of points!", true);
			return null;
		}
		String name = "Summary spatial properties of groups of points in the layer " + pointLayer.getName();
/*
    name= Dialogs.askForStringValue(win,"Name of the table?",name,
      "A new table with summarized spatial properties will be created.",
      "New table",false);
*/
		DataTable table = new DataTable();
		table.setName(name);
		table.addAttribute("N points", IdMaker.makeId("n_points", table), AttributeTypes.integer);
		table.addAttribute("centroid_X", IdMaker.makeId("centroid_X", table), AttributeTypes.real);
		table.addAttribute("centroid_Y", IdMaker.makeId("centroid_Y", table), AttributeTypes.real);
		table.addAttribute("radius", IdMaker.makeId("radius", table), AttributeTypes.real);
		table.addAttribute("mean distance to centroid", IdMaker.makeId("mean_dist_centr", table), AttributeTypes.real);
		table.addAttribute("median distance to centroid", IdMaker.makeId("median_dist_centr", table), AttributeTypes.real);
		table.addAttribute("middle_X", IdMaker.makeId("middle_X", table), AttributeTypes.real);
		table.addAttribute("middle_Y", IdMaker.makeId("middle_Y", table), AttributeTypes.real);
		table.addAttribute("median_X", IdMaker.makeId("median_X", table), AttributeTypes.real);
		table.addAttribute("median_Y", IdMaker.makeId("median_Y", table), AttributeTypes.real);
		table.addAttribute("mean distance to median", IdMaker.makeId("mean_dist_median", table), AttributeTypes.real);
		table.addAttribute("medoid_X", IdMaker.makeId("medoid_X", table), AttributeTypes.real);
		table.addAttribute("medoid_Y", IdMaker.makeId("medoid_Y", table), AttributeTypes.real);
		table.addAttribute("distance from medoid to centroid", IdMaker.makeId("dist_med_cen", table), AttributeTypes.real);
		table.addAttribute("distance from medoid to median", IdMaker.makeId("dist_med_median", table), AttributeTypes.real);
		table.addAttribute("quasi-medoid_X", IdMaker.makeId("quasi_medoid_X", table), AttributeTypes.real);
		table.addAttribute("quasi-medoid_Y", IdMaker.makeId("quasi_medoid_Y", table), AttributeTypes.real);
		table.addAttribute("distance from quasi-medoid to medoid", IdMaker.makeId("quasi_medoid_to_medoid", table), AttributeTypes.real);
		table.addAttribute("distance from quasi-medoid to median", IdMaker.makeId("quasi_medoid_to_median", table), AttributeTypes.real);
		table.addAttribute("mean distance to medoid", IdMaker.makeId("mean_dist_medoid", table), AttributeTypes.real);
		table.addAttribute("mean distance to quasi-medoid", IdMaker.makeId("mean_dist_quasi_medoid", table), AttributeTypes.real);
		table.addAttribute("min_X", IdMaker.makeId("min_X", table), AttributeTypes.real);
		table.addAttribute("max_X", IdMaker.makeId("max_X", table), AttributeTypes.real);
		table.addAttribute("min_Y", IdMaker.makeId("min_Y", table), AttributeTypes.real);
		table.addAttribute("max_Y", IdMaker.makeId("max_Y", table), AttributeTypes.real);
		table.addAttribute("extent", IdMaker.makeId("extent", table), AttributeTypes.real);
		table.addAttribute("extent_X", IdMaker.makeId("extent_X", table), AttributeTypes.real);
		table.addAttribute("extent_Y", IdMaker.makeId("extent_Y", table), AttributeTypes.real);
		table.addAttribute("density", IdMaker.makeId("density", table), AttributeTypes.real);
		table.addAttribute("N points around centroid", IdMaker.makeId("n_points_centr", table), AttributeTypes.integer);
		table.addAttribute("% points around centroid", IdMaker.makeId("perc_points_centr", table), AttributeTypes.real);
		table.addAttribute("density around centroid", IdMaker.makeId("density_centr", table), AttributeTypes.real);
		table.addAttribute("N points around median", IdMaker.makeId("n_points_median", table), AttributeTypes.integer);
		table.addAttribute("% points around median", IdMaker.makeId("perc_points_median", table), AttributeTypes.real);
		table.addAttribute("density around median", IdMaker.makeId("density_median", table), AttributeTypes.real);
		table.addAttribute("N points around medoid", IdMaker.makeId("n_points_medoid", table), AttributeTypes.integer);
		table.addAttribute("% points around medoid", IdMaker.makeId("perc_points_medoid", table), AttributeTypes.real);
		table.addAttribute("density around medoid", IdMaker.makeId("density_medoid", table), AttributeTypes.real);

		int precCoord = (geo) ? 5 : 2, precDist = (geo) ? 2 : 3, precDens = 5;
		double extent = (r != null) ? GeoComp.distance(r.rx1, r.ry1, r.rx2, r.ry2, geo) : 0;
		if (r != null && !geo) {
			precCoord = StringUtil.getPreferredPrecision(Math.min(r.rx1, r.ry1), Math.min(r.rx1, r.ry1), Math.max(r.rx2, r.ry2));
			precDist = StringUtil.getPreferredPrecision(extent / 2, 0, extent);
			double den = pointLayer.getObjectCount() / (extent * extent);
			precDens = StringUtil.getPreferredPrecision(den, 0, den * 10);
		}

		double maxRadX = 0, maxRadY = 0;
		for (int i = 0; i < groups.size(); i++) {
			PointsInCircle gr = groups.elementAt(i);
			RealRectangle bounds = gr.getBounds();
			if (bounds == null) {
				continue;
			}
			float dx = bounds.rx2 - bounds.rx1, dy = bounds.ry2 - bounds.ry1;
			if (maxRadX < dx) {
				maxRadX = dx;
			}
			if (maxRadY < dy) {
				maxRadY = dy;
			}
		}
		double minRadius = Dialogs.askForDoubleValue(win, "Desired minimal circle radius?", 0, 0, extent / 2, null, "Minimal circle radius", false);

		double dx = maxRadX / 100, dy = maxRadY / 100;
		int nPoints = 36;
		double dAngle = 2 * Math.PI / nPoints;

		Vector<DGeoObject> pObj = new Vector<DGeoObject>(500, 100);

		for (int i = 0; i < groups.size(); i++) {
			PointsInCircle gr = groups.elementAt(i);
			int grSize = gr.getPointCount();
			if (grSize < 1) {
				continue;
			}

			RealRectangle bounds = gr.getBounds();
			if (bounds == null) {
				continue;
			}
			RealPoint c = new RealPoint((bounds.rx1 + bounds.rx2) / 2, (bounds.ry1 + bounds.ry2) / 2);
			double radius = Math.max(gr.getMaxDistToPoint(c.x, c.y), minRadius), rx = radius / gr.geoFactorX + dx, ry = radius / gr.geoFactorY + dy;
			//generate a polyline approximating the circle
			RealPolyline z = new RealPolyline();
			z.isClosed = true;
			z.p = new RealPoint[nPoints + 1];
			double angle = 0;
			for (int j = 0; j < nPoints; j++) {
				double cos = Math.cos(angle), sin = Math.sin(angle);
				z.p[j] = new RealPoint(c.x + (float) (rx * cos), c.y + (float) (ry * sin));
				angle += dAngle;
			}
			z.p[nPoints] = z.p[0];
			SpatialEntity spe = new SpatialEntity(classes.elementAt(i));
			spe.setGeometry(z);

			DataRecord rec = new DataRecord(classes.elementAt(i));
			table.addDataRecord(rec);
			rec.setNumericAttrValue(grSize, String.valueOf(grSize), 0);
			rec.setNumericAttrValue(gr.cx, StringUtil.floatToStr(gr.cx, precCoord), 1);
			rec.setNumericAttrValue(gr.cy, StringUtil.floatToStr(gr.cy, precCoord), 2);
			double rad = gr.getRadius();
			rec.setNumericAttrValue(rad, StringUtil.doubleToStr(rad, precDist), 3);
			double meanDCen = gr.getMeanDistToCentre();
			rec.setNumericAttrValue(meanDCen, StringUtil.doubleToStr(meanDCen, precDist + 1), 4);
			double medianDCen = gr.getMedianDistToPoint(gr.cx, gr.cy);
			rec.setNumericAttrValue(medianDCen, StringUtil.doubleToStr(medianDCen, precDist + 1), 5);
			RealPoint p = gr.getRepresentativePoint();
			rec.setNumericAttrValue(p.x, StringUtil.floatToStr(p.x, precCoord), 6);
			rec.setNumericAttrValue(p.y, StringUtil.floatToStr(p.y, precCoord), 7);
			gr.computeMedian();
			rec.setNumericAttrValue(gr.mx, StringUtil.floatToStr(gr.mx, precCoord), 8);
			rec.setNumericAttrValue(gr.my, StringUtil.floatToStr(gr.my, precCoord), 9);
			double meanDMedian = gr.getMeanDistToMedian();
			rec.setNumericAttrValue(meanDMedian, StringUtil.doubleToStr(meanDMedian, precDist + 1), 10);
			RealPoint med = gr.getTrueMedoid(), qmed = gr.getQuasiMedoid();
			rec.setNumericAttrValue(med.x, StringUtil.floatToStr(med.x, precCoord), 11);
			rec.setNumericAttrValue(med.y, StringUtil.floatToStr(med.y, precCoord), 12);
			double d = GeoComp.distance(gr.cx, gr.cy, med.x, med.y, geo);
			rec.setNumericAttrValue(d, StringUtil.doubleToStr(d, precDist + 1), 13);
			d = GeoComp.distance(gr.mx, gr.my, med.x, med.y, geo);
			rec.setNumericAttrValue(d, StringUtil.doubleToStr(d, precDist + 1), 14);
			rec.setNumericAttrValue(qmed.x, StringUtil.floatToStr(qmed.x, precCoord), 15);
			rec.setNumericAttrValue(qmed.y, StringUtil.floatToStr(qmed.y, precCoord), 16);
			d = Math.sqrt(gr.getSquaredDistance(med.x, med.y, qmed.x, qmed.y));
			rec.setNumericAttrValue(d, StringUtil.doubleToStr(d, precDist + 1), 17);
			d = Math.sqrt(gr.getSquaredDistance(gr.mx, gr.my, qmed.x, qmed.y));
			rec.setNumericAttrValue(d, StringUtil.doubleToStr(d, precDist + 1), 18);
			double meanDMedoid = gr.getMeanDistToPoint(med.x, med.y), meanDQMedoid = gr.getMeanDistToPoint(qmed.x, qmed.y);
			rec.setNumericAttrValue(meanDMedoid, StringUtil.doubleToStr(meanDMedoid, precDist + 1), 19);
			rec.setNumericAttrValue(meanDQMedoid, StringUtil.doubleToStr(meanDQMedoid, precDist + 1), 20);
			r = gr.getBounds();
			rec.setNumericAttrValue(r.rx1, StringUtil.floatToStr(r.rx1, precCoord), 21);
			rec.setNumericAttrValue(r.rx2, StringUtil.floatToStr(r.rx2, precCoord), 22);
			rec.setNumericAttrValue(r.ry1, StringUtil.floatToStr(r.ry1, precCoord), 23);
			rec.setNumericAttrValue(r.ry2, StringUtil.floatToStr(r.ry2, precCoord), 24);
			d = GeoComp.distance(r.rx1, r.ry1, r.rx2, r.ry2, geo);
			rec.setNumericAttrValue(d, StringUtil.doubleToStr(d, precDist), 25);
			d = (r.rx2 - r.rx1) * geoFactorX;
			rec.setNumericAttrValue(d, StringUtil.doubleToStr(d, precDist), 26);
			d = (r.ry2 - r.ry1) * geoFactorY;
			rec.setNumericAttrValue(d, StringUtil.doubleToStr(d, precDist), 27);
			if (rad > 0) {
				if (geo) {
					rad /= 1000;
				}
				d = grSize / (rad * rad * Math.PI);
				rec.setNumericAttrValue(d, StringUtil.doubleToStr(d, precDens), 28);
			}
			int nPointsCentre = gr.countPointsAround(gr.cx, gr.cy, meanDCen);
			rec.setNumericAttrValue(nPointsCentre, String.valueOf(nPointsCentre), 29);
			float perc = 100.0f * nPointsCentre / grSize;
			rec.setNumericAttrValue(perc, StringUtil.floatToStr(perc, 2), 30);
			if (meanDCen > 0) {
				if (geo) {
					meanDCen /= 1000;
				}
				d = nPointsCentre / (meanDCen * meanDCen * Math.PI);
				rec.setNumericAttrValue(d, StringUtil.doubleToStr(d, precDens), 31);
			}
			int nPMed = gr.countPointsAround(gr.mx, gr.my, meanDMedian);
			rec.setNumericAttrValue(nPMed, String.valueOf(nPMed), 32);
			perc = 100.0f * nPMed / grSize;
			rec.setNumericAttrValue(perc, StringUtil.floatToStr(perc, 2), 33);
			if (meanDMedian > 0) {
				if (geo) {
					meanDMedian /= 1000;
				}
				d = nPMed / (meanDMedian * meanDMedian * Math.PI);
				rec.setNumericAttrValue(d, StringUtil.doubleToStr(d, precDens), 34);
			}
			nPMed = gr.countPointsAround(med.x, med.y, meanDMedoid);
			rec.setNumericAttrValue(nPMed, String.valueOf(nPMed), 35);
			perc = 100.0f * nPMed / grSize;
			rec.setNumericAttrValue(perc, StringUtil.floatToStr(perc, 2), 36);
			if (meanDMedoid > 0) {
				if (geo) {
					meanDMedoid /= 1000;
				}
				d = nPMed / (meanDMedoid * meanDMedoid * Math.PI);
				rec.setNumericAttrValue(d, StringUtil.doubleToStr(d, precDens), 37);
			}
			spe.setThematicData(rec);
			DGeoObject gObj = new DGeoObject();
			gObj.setup(spe);
			pObj.addElement(gObj);
		}

		name = "Circles around groups of points in the layer " + pointLayer.getName();
		DGeoLayer circleLayer = new DGeoLayer();
		circleLayer.setName(name);
		circleLayer.setType(Geometry.area);
		circleLayer.setGeographic(pointLayer.isGeographic());
		circleLayer.setGeoObjects(pObj, true);
		circleLayer.setDataTable(table);
		circleLayer.setLinkedToTable(true);
		return circleLayer;
	}

	/**
	 * Compares two strings. First checks if the strings represent numbers.
	 * If so, compares the numbers.
	 * Returns 0 if the objects are equal, <0 if the first object is less than the
	 * second one, >0 otherwise
	 */
	@Override
	public int compare(Object obj1, Object obj2) {
		if (obj1 == null || obj2 == null || !(obj1 instanceof String) || !(obj2 instanceof String))
			return 0;
		String st1 = (String) obj1, st2 = (String) obj2;
		try {
			int i1 = Integer.parseInt(st1);
			int i2 = Integer.parseInt(st2);
			if (i1 < i2)
				return -1;
			if (i1 > i2)
				return 1;
			return 0;
		} catch (NumberFormatException e) {
		}
		return st1.compareTo(st2);
	}

	protected void showMessage(String msg, boolean error) {
		if (core != null && core.getUI() != null) {
			core.getUI().showMessage(msg, error);
		} else if (error) {
			System.out.println("!--> " + msg);
		}
	}
}
