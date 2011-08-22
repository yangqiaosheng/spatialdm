package spade.analysis.geocomp;

import java.awt.Color;
import java.awt.Frame;
import java.util.Map;
import java.util.Vector;

import spade.analysis.geocomp.voronoi.Voronoi;
import spade.analysis.geocomp.voronoi.VoronoiNew;
import spade.analysis.system.ESDACore;
import spade.analysis.system.SystemUI;
import spade.lib.basicwin.CManager;
import spade.lib.util.IntArray;
import spade.vis.database.SpatialEntity;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;
import spade.vis.dmap.DrawingParameters;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.RealPoint;
import spade.vis.geometry.RealPolyline;
import spade.vis.space.GeoLayer;
import spade.vis.space.LayerManager;
import spade.vis.space.SelectLayer;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Feb 3, 2009
 * Time: 3:48:22 PM
 * Generates Voronoi (Thiessen) polygons from a set of points.
 */
public class VoronoiPolygonsFromPoints extends GeoCalculator {
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
		SystemUI ui = core.getUI();
		//propose the user to select a layer with point objects
		if (lman == null)
			return null;
		Frame win = null;
		if (ui != null) {
			win = ui.getMainFrame();
		}
		if (win == null) {
			win = CManager.getAnyFrame();
		}
		GeoLayer pl = SelectLayer.selectLayer(lman, Geometry.point, "Select a layer with points:", win);
		if (pl == null)
			return null;
		Vector<RealPoint> points = new Vector<RealPoint>(500, 100);
		IntArray pIdxs = new IntArray(500, 100);
		for (int j = 0; j < pl.getObjectCount(); j++)
			if ((pl.getObjectAt(j) instanceof DGeoObject) && pl.isObjectActive(j)) {
				DGeoObject gobj = (DGeoObject) pl.getObjectAt(j);
				Geometry geom = gobj.getGeometry();
				if (geom != null) {
					RealPoint p = (geom instanceof RealPoint) ? (RealPoint) geom : SpatialEntity.getCentre(geom);
					points.addElement(p);
					pIdxs.addElement(j);
				}
			}
		if (points.size() < 1) {
			ui.showMessage("No points found!", true);
			return null;
		}
		if (points.size() < 3) {
			ui.showMessage("Too few points: " + points.size(), true);
			return null;
		}
		long t0 = System.currentTimeMillis();
		Voronoi voronoi = new Voronoi(points);
		if (!voronoi.isValid()) {
			ui.showMessage("Failed to triangulate!", true);
			return null;
		}
		voronoi.setBuildNeighbourhoodMatrix(true);
		RealPolyline areas[] = voronoi.getPolygons();
		if (areas == null || areas.length < 1) {
			ui.showMessage("Failed to build polygons!", true);
			return null;
		}
		long t = System.currentTimeMillis();
		int nPolygons = 0;
		for (RealPolyline area : areas)
			if (area != null) {
				++nPolygons;
			}
		ui.showMessage("Got " + nPolygons + " polygons; elapsed time = " + (t - t0) + " msec.", false);
		System.out.println("Got " + nPolygons + " Voronoi polygons; elapsed time = " + (t - t0) + " msec.");

		Vector<DGeoObject> placeObjects = new Vector<DGeoObject>(nPolygons, 10);
		int idxs[] = new int[areas.length];
		for (int i = 0; i < areas.length; i++)
			if (areas[i] != null) {
				DGeoObject ptObj = (DGeoObject) pl.getObjectAt(pIdxs.elementAt(i));
				SpatialEntity spe = new SpatialEntity(ptObj.getIdentifier());
				spe.setGeometry(areas[i]);
				spe.setName(ptObj.getName());
				DGeoObject obj = new DGeoObject();
				obj.setup(spe);
				if (spe.getName() != null) {
					obj.setLabel(spe.getName());
				}
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
						if (j != i && neighbourMap.get(i)==j && idxs[j] >= 0) {
							pObj.addNeighbour(placeObjects.elementAt(idxs[j]));
						}
				}
		}

		DGeoLayer layer = new DGeoLayer();
		layer.setType(Geometry.area);
		layer.setName("Voronoi polygons for " + pl.getName());
		layer.setGeoObjects(placeObjects, true);
		DrawingParameters dp = layer.getDrawingParameters();
		if (dp == null) {
			dp = new DrawingParameters();
			layer.setDrawingParameters(dp);
		}
		dp.lineColor = Color.getHSBColor((float) Math.random(), 1 - 0.2f * (float) Math.random(), 1 - 0.2f * (float) Math.random());
		dp.fillContours = false;
		return layer;
	}
}
