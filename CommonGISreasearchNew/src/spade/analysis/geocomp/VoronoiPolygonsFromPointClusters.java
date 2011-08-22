package spade.analysis.geocomp;

import java.awt.Color;
import java.util.Map;
import java.util.Vector;

import spade.analysis.geocomp.voronoi.Voronoi;
import spade.analysis.geocomp.voronoi.VoronoiNew;
import spade.analysis.system.ESDACore;
import spade.analysis.tools.clustering.PointOrganizerSpatialIndex;
import spade.lib.util.GeoDistance;
import spade.vis.database.SpatialEntity;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;
import spade.vis.dmap.DrawingParameters;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.RealPoint;
import spade.vis.geometry.RealPolyline;
import spade.vis.space.LayerManager;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Apr 23, 2009
 * Time: 11:31:49 AM
 * Computes a division of the territory by Voronoi polygons
 * on the basis of grouping points into clusters with user-specified
 * maximum radius.
 */
public class VoronoiPolygonsFromPointClusters extends PointGrouper {
	/**
	 * On the basis of a point layers, construct a layer where objects
	 * are Voronoi polygons enclosing the points.
	 * The arguments are a layer manager (a GeoCalculator must itself care about
	 * selection of a layer or layers of appropriate type, in this case layers
	 * with point objects) and SystemUI (to be used for displaying messages and
	 * finding an owner frame for dialogs)
	 * If calculation was successful, returns the produced layer (an instance
	 * of DGeoLayer) with polygons (instances of RealPolyline).
	*/
	@Override
	public Object doCalculation(LayerManager lman, ESDACore core) {
		this.core = core;
		if (!getSettings(lman, true))
			return false;
		PointOrganizerSpatialIndex pOrg = groupPoints(pointLayer, maxRad);
		if (pOrg == null)
			return null;
		addGroupNumbersToTable(pointLayer, pOrg);
		DGeoLayer cLayer = null;
		if (makeLayerWithCentroids) {
			cLayer = makeLayerWithGroupCentroids(pOrg);
		}
//		DGeoLayer vLayer = buildVoronoiCells(pOrg, maxRad, addPoints, pointLayer.isGeographic());
//		if (vLayer == null)
//			return cLayer;
//		if (cLayer == null)
//			return vLayer;
		Vector<DGeoLayer> result = new Vector<DGeoLayer>(2, 1);
		result.addElement(cLayer);
//		result.addElement(vLayer);
		return result;
	}

	/**
	 * Produces a layer with Voronoi polygons from the given groups of points.
	 */
	public DGeoLayer buildVoronoiCells(PointOrganizerSpatialIndex pOrg, double maxRad, boolean addPoints, boolean geo) {
		int nGroups = pOrg.getGroupCount();
		Vector<RealPoint> points = new Vector<RealPoint>(nGroups, 10);
		for (int i = 0; i < nGroups; i++) {
			points.addElement(pOrg.getCentroid(i));
		}

		//For building the polygons:
		//introduce additional points on the boundaries
		float width, height, geoFactorX = 1f, geoFactorY = 1f;
		if (geo) {
			float my = (pOrg.y1 + pOrg.y2) / 2;
			width = (float) GeoDistance.geoDist(pOrg.x1, my, pOrg.x2, my);
			float mx = (pOrg.x1 + pOrg.x2) / 2;
			height = (float) GeoDistance.geoDist(mx, pOrg.y1, mx, pOrg.y2);
			geoFactorX = width / (pOrg.x2 - pOrg.x1);
			geoFactorY = height / (pOrg.y2 - pOrg.y1);
		} else {
			width = pOrg.x2 - pOrg.x1;
			height = pOrg.y2 - pOrg.y1;
		}
		double maxRadX = maxRad, maxRadY = maxRad;
		if (geo) {
			maxRadX = maxRad / geoFactorX;
			maxRadY = maxRad / geoFactorY;
		}
		float dy = 2 * (float) maxRadY, dx = 2 * (float) maxRadX, dx2 = dx / 2, dy2 = dy / 2;
		float y1 = pOrg.y1 - dy - dy2, y2 = pOrg.y2 + dy + dy2;
		float x1 = pOrg.x1 - dx - dx2, x2 = pOrg.x2 + dx + dx2;
		if (addPoints) {
			//introducing additional points in empty areas and on the boundaries
			int k = 0;
			for (float y = y1; y <= y2 + dy2; y += dy) {
				float ddx = (k % 2 == 0) ? 0 : dx2;
				++k;
				for (float x = x1 + ddx; x <= x2 + dx2; x += dx)
					if (pOrg.isFarFromAll(x, y)) {
						points.addElement(new RealPoint(x, y));
					}
			}
		} else {
			for (float x = x1; x <= x2; x += dx) {
				points.addElement(new RealPoint(x, y1));
				points.addElement(new RealPoint(x, y2));
			}
			for (float y = y1; y <= y2; y += dy) {
				points.addElement(new RealPoint(x1, y));
				points.addElement(new RealPoint(x2, y));
			}
		}
		core.getUI().showMessage("Building Voronoi polygons; wait...", false);
		long t0 = System.currentTimeMillis();
		Voronoi voronoi = new Voronoi(points);
		if (!voronoi.isValid()) {
			core.getUI().showMessage("Failed to triangulate!", true);
			return null;
		}
		voronoi.setBuildNeighbourhoodMatrix(true);
		RealPolyline areas[] = voronoi.getPolygons(x1, y1, x2, y2);
		if (areas == null) {
			core.getUI().showMessage("Failed to build polygons!", true);
			return null;
		}
		long t = System.currentTimeMillis();
		int nPolygons = 0;
		for (RealPolyline area : areas)
			if (area != null) {
				++nPolygons;
			}
		core.getUI().showMessage("Got " + nPolygons + " Voronoi polygons; elapsed time = " + (t - t0) + " msec.", false);
		System.out.println("Got " + nPolygons + " Voronoi polygons; elapsed time = " + (t - t0) + " msec.");

		Vector<DGeoObject> placeObjects = new Vector<DGeoObject>(areas.length, 10);
		int idxs[] = new int[areas.length];
		for (int i = 0; i < areas.length; i++)
			if (areas[i] != null) {
				SpatialEntity spe = new SpatialEntity(String.valueOf(i + 1));
				spe.setGeometry(areas[i]);
				DGeoObject obj = new DGeoObject();
				obj.setup(spe);
				placeObjects.addElement(obj);
				idxs[i] = placeObjects.size() - 1;
			} else {
				idxs[i] = -1;
			}
		Map<Integer, Integer> neighbourMap = voronoi.getNeighbourhoodMap();
		if (neighbourMap != null) {
			for (int i = 0; i < areas.length; i++)
				if (idxs[i] >= 0) {
					DGeoObject pObj = placeObjects.elementAt(idxs[i]);
					for (int j = 0; j < areas.length; j++)
						if (j != i && neighbourMap.get(i) == j && idxs[j] >= 0) {
							pObj.addNeighbour(placeObjects.elementAt(idxs[j]));
						}
				}
		}
		String name = "Areas around groups of " + pointLayer.getName() + " (r=" + maxRad + ")";
		DGeoLayer placeLayer = new DGeoLayer();
		placeLayer.setName(name);
		placeLayer.setGeographic(pointLayer.isGeographic());
		placeLayer.setType(Geometry.area);
		placeLayer.setGeoObjects(placeObjects, true);
		DrawingParameters dp = placeLayer.getDrawingParameters();
		if (dp == null) {
			dp = new DrawingParameters();
			placeLayer.setDrawingParameters(dp);
		}
		dp.lineColor = Color.getHSBColor((float) Math.random(), 1 - 0.2f * (float) Math.random(), 1 - 0.2f * (float) Math.random());
		dp.fillContours = false;
		return placeLayer;
	}
}
