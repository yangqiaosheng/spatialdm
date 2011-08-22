package spade.vis.dmap;

import java.awt.Graphics;

import spade.vis.database.SpatialDataPortion;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.RasterGeometry;
import spade.vis.geometry.RealRectangle;
import spade.vis.map.MapContext;
import spade.vis.mapvis.GridVisualizer;
import spade.vis.mapvis.Visualizer;
import spade.vis.space.GeoLayer;

/**
* A variant of DGeoLayer dealing with grid (raster) data.
*/

public class DGridLayer extends DGeoLayer {
	/**
	* Construction of GeoObjects on the basis of a SpatialDataPortion.
	* Returns the number of the objects constructed (updated)
	*/
	@Override
	public int receiveSpatialData(SpatialDataPortion sdp) {
		int nObj = super.receiveSpatialData(sdp);
		if (nObj > 0) {
			if (vis == null || !(vis instanceof GridVisualizer)) {
				checkMakeVisualizer();
			} else {
				setupVisualizer((GridVisualizer) vis);
			}
		}
		return nObj;
	}

	protected void setupVisualizer(GridVisualizer gvis) {
		if (gvis == null)
			return;
		if (getObjectCount() < 1) {
			loadGeoObjects();
		}
		if (getObjectCount() < 1)
			return;
		float min = Float.NaN, max = Float.NaN;
		for (int i = 0; i < getObjectCount(); i++) {
			DGeoObject gobj = getObject(i);
			Geometry geom = gobj.getGeometry();
			if (geom != null && (geom instanceof RasterGeometry)) {
				RasterGeometry rgeom = (RasterGeometry) geom;
				if (rgeom.ras != null) {
					if (Float.isNaN(min) || rgeom.minV < min) {
						min = rgeom.minV;
					}
					if (Float.isNaN(max) || rgeom.maxV > max) {
						max = rgeom.maxV;
					}
				}
			}
		}
		gvis.setMinMax(min, max);
		if (drawParm != null) {
			gvis.setDrawingParameters(drawParm);
		}
	}

	protected void checkMakeVisualizer() {
		if (getObjectCount() < 1) {
			loadGeoObjects();
		}
		if (getObjectCount() < 1)
			return;
		if (vis == null || !(vis instanceof GridVisualizer)) {
			vis = null;
			try {
				Object gvis = Class.forName("spade.vis.mapvis.GridVisImplement").newInstance();
				if (gvis != null && (gvis instanceof GridVisualizer) && (gvis instanceof Visualizer)) {
					setupVisualizer((GridVisualizer) gvis);
					super.setVisualizer((Visualizer) gvis);
				}
			} catch (Exception e) {
			}
		}
	}

	public GridVisualizer getGridVisualizer() {
		checkMakeVisualizer();
		return (GridVisualizer) vis;
	}

	@Override
	public void setVisualizer(Visualizer visualizer) {
		if (visualizer != null && (visualizer instanceof GridVisualizer)) {
			setupVisualizer((GridVisualizer) visualizer);
		}
		super.setVisualizer(visualizer);
	}

//------------ Functions to access and modify layer's properties ---------
	/**
	* Returns the spatial type of its objects (i.e. raster)
	*/
	@Override
	public char getType() {
		if (objType == Geometry.undefined) {
			objType = Geometry.raster;
		}
		return objType;
	}

	/**
	* Does not do anything - the type is raster.
	*/
	@Override
	public void setType(char type) {
	}

	/**
	* Returns false.
	*/
	@Override
	public boolean getHasDiagrams() {
		return false;
	}

	@Override
	public void draw(Graphics g, MapContext mc) {
		if (g == null || mc == null)
			return;
		if (geoObj == null) {
			loadGeoObjects(mc);
		}
		if (geoObj == null)
			return;
		checkMakeVisualizer();
		RealRectangle rr = mc.getVisibleTerritory();
		nActive = 0;
		for (int i = 0; i < getObjectCount(); i++) {
			DGeoObject gobj = getObject(i);
			if (!isObjectActive(gobj)) {
				continue;
			}
			++nActive;
			if (gobj.fitsInRectangle(rr.rx1, rr.ry1, rr.rx2, rr.ry2)) {
				gobj.setVisualizer(vis);
				gobj.setImageObserver(this);
				gobj.draw(g, mc);
			}
		}
	}

//ID
	/**
	* Returns a copy of this GeoLayer. The reference to the visualizer is also copied.
	*/
	@Override
	public GeoLayer makeCopy() {
		DGridLayer layer = new DGridLayer();
		copyTo(layer);
		return layer;
	}
//~ID
}
