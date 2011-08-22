package spade.analysis.geocomp;

import java.awt.Frame;
import java.util.Vector;

import spade.analysis.system.ESDACore;
import spade.analysis.system.SystemUI;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.Dialogs;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.database.SpatialEntity;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;
import spade.vis.geometry.GeoComp;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.RealPoint;
import spade.vis.geometry.RealPolyline;
import spade.vis.geometry.RealRectangle;
import spade.vis.space.GeoLayer;
import spade.vis.space.LayerManager;
import spade.vis.space.SelectLayer;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 02-Apr-2007
 * Time: 18:37:52
 * On the basis of one or more point layers, construct a layer where objects
 * are circles enclosing the points.
 */
public class CirclesFromPoints extends GeoCalculator {
	protected ESDACore core = null;

	/**
	 * On the basis of one or more point layers, construct a layer where objects
	 * are circles enclosing the points.
	 * The arguments are a layer manager (a GeoCalculator must itself care about
	 * selection of a layer or layers of appropriate type, in this case layers
	 * with point objects) and SystemUI (to be used for displaying messages and
	 * finding an owner frame for dialogs)
	 * If calculation was successful, returns the produced layer (an instance
	 * of DGeoLayer) with circles (instances of RealCircle).
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
		boolean geo = pointLayer.isGeographic();
		float geoFactorX = 1f, geoFactorY = 1f;
		RealRectangle r = pointLayer.getWholeLayerBounds();
		if (r == null) {
			r = pointLayer.getCurrentLayerBounds();
		}
		if (r == null) {
			showMessage("The layer has no spatial extent!", true);
			return null;
		}
		if (geo) {
			double wh[] = DGeoLayer.getExtentXY(pointLayer);
			if (wh == null || wh[0] <= 0 || wh[1] <= 0)
				return null;
			float width = (float) wh[0], height = (float) wh[1];
			geoFactorX = width / (r.rx2 - r.rx1);
			geoFactorY = height / (r.ry2 - r.ry1);
		}
		double extent = GeoComp.distance(r.rx1, r.ry1, r.rx2, r.ry2, geo);
		double radius = Dialogs.askForDoubleValue(win, "Desired circle radius?", 0, 0, extent / 2, null, "Circle radius", false);
		double rx = radius / geoFactorX, ry = radius / geoFactorY;
		int nPoints = 36;
		double dAngle = 2 * Math.PI / nPoints;
		Vector<DGeoObject> objects = new Vector<DGeoObject>(500, 100);
		DataTable table = null, pTable = null;
		if (pointLayer.getThematicData() != null && (pointLayer.getThematicData() instanceof DataTable)) {
			pTable = (DataTable) pointLayer.getThematicData();
		}
		if (pTable != null) {
			table = new DataTable();
			for (int i = 0; i < pTable.getAttrCount(); i++) {
				table.addAttribute(pTable.getAttribute(i));
			}
			for (int i = 0; i < pTable.getParamCount(); i++) {
				table.addParameter(pTable.getParameter(i));
			}
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
			RealPoint p = (pObj.getGeometry() instanceof RealPoint) ? (RealPoint) geom : SpatialEntity.getCentre(geom);
			//generate a polyline approximating the circle
			RealPolyline z = new RealPolyline();
			z.isClosed = true;
			z.p = new RealPoint[nPoints + 1];
			double angle = 0;
			for (int j = 0; j < nPoints; j++) {
				double cos = Math.cos(angle), sin = Math.sin(angle);
				z.p[j] = new RealPoint(p.x + (float) (rx * cos), p.y + (float) (ry * sin));
				angle += dAngle;
			}
			z.p[nPoints] = z.p[0];
			SpatialEntity spe = new SpatialEntity(pObj.getIdentifier());
			spe.setGeometry(z);
			if (table != null && pObj.getData() != null) {
				DataRecord r1 = (DataRecord) pObj.getData();
				DataRecord r2 = (DataRecord) r1.clone();
				table.addDataRecord(r2);
				spe.setThematicData(r2);
			}
			DGeoObject gObj = new DGeoObject();
			gObj.setup(spe);
			objects.addElement(gObj);
		}
		if (table != null) {
			String name = "Properties of circles from " + pointLayer.getName();
/*
      name= Dialogs.askForStringValue(win,"Name of the table?",name,
        "A copy of the table describing the points will be created for the circles.",
        "New table",false);
*/
			table.setName(name);
		}
		String name = "Circles around points of the layer " + pointLayer.getName();
		DGeoLayer circleLayer = new DGeoLayer();
		circleLayer.setName(name);
		circleLayer.setType(Geometry.area);
		circleLayer.setGeographic(pointLayer.isGeographic());
		circleLayer.setGeoObjects(objects, true);
		if (table != null) {
			circleLayer.setDataTable(table);
		}
		circleLayer.setLinkedToTable(true);
		return circleLayer;
	}

	protected void showMessage(String msg, boolean error) {
		if (core != null && core.getUI() != null) {
			core.getUI().showMessage(msg, error);
		} else if (error) {
			System.out.println("!--> " + msg);
		}
	}
}
