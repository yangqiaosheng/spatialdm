package spade.analysis.geocomp;

import java.awt.Color;
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
import spade.vis.dmap.DrawingParameters;
import spade.vis.geometry.GeoComp;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.MultiGeometry;
import spade.vis.geometry.RealLine;
import spade.vis.geometry.RealPoint;
import spade.vis.geometry.RealPolyline;
import spade.vis.geometry.RealRectangle;
import spade.vis.space.GeoLayer;
import spade.vis.space.LayerManager;
import spade.vis.space.SelectLayer;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Jan 18, 2011
 * Time: 3:19:36 PM
 * Builds buffers around geographical objects from a selected layer
 */
public class BufferBuilder extends GeoCalculator {
	protected ESDACore core = null;

	/**
	 * Builds convex hulls or buffers around groups (classes) of point objects
	 * @return the created layer with polygons
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
		StringBuffer sb = new StringBuffer(3);
		sb.append(Geometry.point);
		sb.append(Geometry.line);
		sb.append(Geometry.area);
		GeoLayer pl = SelectLayer.selectLayer(lman, sb.toString(), "Select a layer for building buffers around its objects:", win);
		if (pl == null || !(pl instanceof DGeoLayer))
			return false;

		DGeoLayer origLayer = (DGeoLayer) pl;
		boolean geo = origLayer.isGeographic();
		float geoFactorX = 1f, geoFactorY = 1f;
		RealRectangle r = origLayer.getWholeLayerBounds();
		if (r == null) {
			r = origLayer.getCurrentLayerBounds();
		}
		if (geo && r != null) {
			double wh[] = DGeoLayer.getExtentXY(origLayer);
			if (wh == null || wh[0] <= 0 || wh[1] <= 0)
				return null;
			float width = (float) wh[0], height = (float) wh[1];
			geoFactorX = width / (r.rx2 - r.rx1);
			geoFactorY = height / (r.ry2 - r.ry1);
		}
		double extent = (r != null) ? GeoComp.distance(r.rx1, r.ry1, r.rx2, r.ry2, geo) : 0;
		double bufDistOrig = Dialogs.askForDoubleValue(win, "Buffer width" + ((geo) ? " (m)?" : "?"), 0, 0, extent / 2, "Building buffers around the objects from the layer " + origLayer.getName(), "Buffer width", true);
		if (Double.isNaN(bufDistOrig))
			return null;
		double bufDist = bufDistOrig;
		if (geo) {
			bufDist /= Math.min(geoFactorX, geoFactorY);
		}

		DataTable bufTable = null;
		if (origLayer.getThematicData() != null) {
			DataTable origTable = (DataTable) origLayer.getThematicData();
			bufTable = new DataTable();
			for (int i = 0; i < origTable.getDataItemCount(); i++) {
				bufTable.addAttribute(origTable.getAttribute(i));
			}
		}

		Vector<DGeoObject> pObj = new Vector<DGeoObject>(500, 100);
		//The GeometryFactory will be used to construct JTS geometries
		GeometryFactory geoFactory = new GeometryFactory();

		for (int i = 0; i < origLayer.getObjectCount(); i++) {
			DGeoObject gObj1 = origLayer.getObject(i);
			Geometry geom = gObj1.getGeometry();
			if (geom == null) {
				continue;
			}
			com.vividsolutions.jts.geom.Geometry jtsGeom = makeJTSGeometry(geom, geoFactory);
			if (jtsGeom == null) {
				continue;
			}
			com.vividsolutions.jts.geom.Geometry hull = null;
			try {
				hull = jtsGeom.buffer(bufDist);
			} catch (Exception e) {
				continue;
			}
			if (hull == null) {
				continue;
			}
			if (!(hull instanceof com.vividsolutions.jts.geom.Polygon)) {
				continue;
			}
			com.vividsolutions.jts.geom.Polygon poly = (com.vividsolutions.jts.geom.Polygon) hull;
			Coordinate coords[] = poly.getCoordinates();
			if (coords == null || coords.length < 3) {
				continue;
			}

			//generate a CommonGIS polyline from the JTS polygon
			RealPolyline z = new RealPolyline();
			z.isClosed = true;
			z.p = new RealPoint[coords.length];
			for (int j = 0; j < coords.length; j++) {
				z.p[j] = new RealPoint((float) coords[j].x, (float) coords[j].y);
			}
			SpatialEntity spe = new SpatialEntity(gObj1.getIdentifier());
			spe.setGeometry(z);
			if (bufTable != null && gObj1.getData() != null) {
				DataRecord rec1 = (DataRecord) gObj1.getData();
				DataRecord rec2 = rec1.makeCopy(false, true);
				bufTable.addDataRecord(rec2);
				spe.setThematicData(rec2);
			}
			DGeoObject gObj2 = new DGeoObject();
			gObj2.setup(spe);
			pObj.addElement(gObj2);
		}

		if (pObj.size() < 1) {
			showMessage("Could not build any buffer!", true);
			return null;
		}

		String name = "Buffers (" + bufDistOrig + ")" + " around " + origLayer.getName();
		DGeoLayer hullLayer = new DGeoLayer();
		hullLayer.setName(name);
		hullLayer.setType(Geometry.area);
		hullLayer.setGeographic(origLayer.isGeographic());
		hullLayer.setGeoObjects(pObj, true);
		hullLayer.setDataTable(bufTable);
		hullLayer.setLinkedToTable(true);
		DrawingParameters dp = hullLayer.getDrawingParameters();
		if (dp == null) {
			dp = new DrawingParameters();
			hullLayer.setDrawingParameters(dp);
		}
		dp.lineColor = Color.getHSBColor((float) Math.random(), 1 - 0.2f * (float) Math.random(), 1 - 0.2f * (float) Math.random());
		dp.fillColor = Color.lightGray;
		dp.fillContours = true;
		dp.transparency = 50;
		dp.lineWidth = 3;
		return hullLayer;
	}

	protected com.vividsolutions.jts.geom.Geometry makeJTSGeometry(Geometry geom, GeometryFactory geoFactory) {
		if (geom == null)
			return null;
		if (geom instanceof RealPoint) {
			RealPoint p = (RealPoint) geom;
			return geoFactory.createPoint(new Coordinate(p.x, p.y));
		}
		if (geom instanceof RealPolyline) {
			RealPolyline l = (RealPolyline) geom;
			if (l.p == null || l.p.length < 2)
				return null;
			Coordinate coords[] = new Coordinate[l.p.length];
			for (int i = 0; i < l.p.length; i++) {
				coords[i] = new Coordinate(l.p[i].x, l.p[i].y);
			}
			if (!l.isClosed)
				return geoFactory.createLineString(coords);
			LinearRing lr = geoFactory.createLinearRing(coords);
			if (lr == null)
				return null;
			return geoFactory.createPolygon(lr, null);
		}
		if (geom instanceof RealLine) {
			RealLine l = (RealLine) geom;
			Coordinate coords[] = new Coordinate[2];
			coords[0] = new Coordinate(l.x1, l.y1);
			coords[1] = new Coordinate(l.x2, l.y2);
			return geoFactory.createLineString(coords);
		}
		if (geom instanceof MultiGeometry) {
			MultiGeometry mg = (MultiGeometry) geom;
			int nParts = mg.getPartsCount();
			if (nParts == 0)
				return null;
			if (nParts == 1)
				return makeJTSGeometry(mg.getPart(0), geoFactory);
			Vector<com.vividsolutions.jts.geom.Geometry> vg = new Vector<com.vividsolutions.jts.geom.Geometry>(mg.getPartsCount(), 1);
			for (int i = 0; i < nParts; i++) {
				com.vividsolutions.jts.geom.Geometry g = makeJTSGeometry(mg.getPart(i), geoFactory);
				if (g != null) {
					vg.addElement(g);
				}
			}
			if (vg.size() < 1)
				return null;
			if (vg.size() == 1)
				return vg.elementAt(0);
			com.vividsolutions.jts.geom.Geometry gg[] = new com.vividsolutions.jts.geom.Geometry[vg.size()];
			for (int i = 0; i < vg.size(); i++) {
				gg[i] = vg.elementAt(i);
			}
			return geoFactory.createGeometryCollection(gg);
		}
		return null;
	}

	protected void showMessage(String msg, boolean error) {
		if (core != null && core.getUI() != null) {
			core.getUI().showMessage(msg, error);
		} else if (error) {
			System.out.println("!--> " + msg);
		}
	}
}
