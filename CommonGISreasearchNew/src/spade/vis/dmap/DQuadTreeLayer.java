package spade.vis.dmap;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.Vector;

import spade.lib.basicwin.Drawing;
import spade.lib.util.IntArray;
import spade.lib.util.StringUtil;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.RealPoint;
import spade.vis.geometry.RealPolyline;
import spade.vis.geometry.RealRectangle;
import spade.vis.map.MapContext;
import spade.vis.mapvis.Visualizer;

public class DQuadTreeLayer extends DGeoLayer {
	/**
	* The current level of detail (by default 3):
	* 0 - original objects (no division)
	* 1 - division into 4 parts
	* 2 - division into 16 parts (each part of the level 1 is divided into 4 parts)
	* 3 - division into 64 parts (each part of the level 2 is divided into 4 parts)
	* and so on
	*/
	protected int level = 3;
	/**
	* Indicates whether the layer is currently used for data input (the value
	* is true) or for visualisation (the value is false). Depending on this
	* variable, the layer will be drawn differently.
	*/
	protected boolean usedForInput = false;
	/**
	* Contains the identifiers of currently selected virtual objects
	*/
	protected Vector selIds = null;
	/**
	* Contains the rectangles of currently selected virtual objects. The elements
	* of the vectors are instances of either RealRectangle or RealPolyline
	* (polygons having 4 vertices).
	*/
	protected Vector selRectangles = null;
	/**
	* The identifier of the highlighted object (only one object may be highlighted
	* at a time)
	*/
	protected String hlObjId = null;
	/**
	* The rectangle (or 4-angular polygon) of the highlighted object. Only one
	* object may be highlighted at a time.
	*/
	protected Geometry hlObjRect = null;
	/**
	* Used for the purposes of drawing polygons
	*/
	protected static int polyX[] = null, polyY[] = null, pX[] = null, pY[] = null;
	/**
	* Used for internal calculations
	*/
	protected static RealPoint rPoly[] = null;

	/**
	* Constructs its internal structures by copying everything from the
	* specified layer. The objects are ensured to be either rectangles
	* (instances of RealRectangle) or 4-angular polygons (instances of
	* RealPolyline). Returns true if successfully constructed.
	*/
	public boolean constructGrid(DGeoLayer layer) {
		if (layer == null)
			return false;
		name = layer.name;
		id = layer.getContainerIdentifier();
		setId = layer.getEntitySetIdentifier();
		hasAllObjects = layer.hasAllObjects;
		hasLabels = layer.hasLabels;
		dataSuppl = layer.getDataSupplier();
		drawParm = layer.getDrawingParameters().makeCopy();
		isActive = layer.isActive;
		objType = Geometry.area;
		dTable = layer.getThematicData();
		oFilter = layer.getObjectFilter();
		geoObj = null;
		if (layer.getObjectCount() > 0) {
			geoObj = new Vector(layer.getObjectCount() + 100, 100);
			for (int i = 0; i < layer.getObjectCount(); i++) {
				DGeoObject gobj = layer.getObject(i);
				Geometry geom = gobj.getGeometry();
				if (geom == null) {
					continue;
				}
				if (geom instanceof RealRectangle) {
					geoObj.addElement(gobj);
				} else if (geom instanceof RealPolyline) {
					RealPolyline poly = (RealPolyline) geom;
					if (poly.p == null || poly.p.length < 4) {
						continue;
					}
					if (poly.p.length != 5) {
						RealPoint rp[] = new RealPoint[5];
						for (int j = 0; j < 4; j++) {
							rp[j] = poly.p[j];
						}
						poly.p = rp;
					}
					poly.p[4] = poly.p[0];
					orderPoly(poly);
					geoObj.addElement(gobj);
				}
			}
		}
		return geoObj != null && geoObj.size() > 0;
	}

	/**
	* Sets the current level of detail. Checks if the maximum allowed level is not
	* exceeded.
	*/
	public void setCurrentLevel(int lev) {
		if (lev == level)
			return;
		if (drawParm.maxLevels < 0) {
			drawParm.maxLevels = 3; //default value
		}
		if (lev >= 0 && lev <= drawParm.maxLevels) {
			level = lev;
		}
		notifyPropertyChange("ObjectSet", null, null);
	}

	/**
	* Returns the current level of detail.
	*/
	public int getCurrentLevel() {
		return level;
	}

	/**
	* Sets the maximum allowed level of detail.
	*/
	public void setMaxLevel(int lev) {
		drawParm.maxLevels = lev;
	}

	/**
	* Returns the maximum allowed level of detail.
	*/
	public int getMaxLevel() {
		if (drawParm.maxLevels < 0) {
			drawParm.maxLevels = Math.min(3, level);
		}
		return drawParm.maxLevels;
	}

	/**
	* Returns the spatial type of its objects: area.
	*/
	@Override
	public char getType() {
		return Geometry.area;
	}

	/**
	* Does not allow to change the spatial type of the objects comprising this
	* GeoLayer.
	*/
	@Override
	public void setType(char type) {
	}

	/**
	* Sets whether the layer will be used for data input (the value
	* is true) or for visualisation (the value is false). Depending on this
	* variable, the layer will be drawn differently.
	*/
	public void setUsedForInput(boolean value) {
		usedForInput = value;
	}

	/**
	* Reports whether the layer is currently used for data input (the result
	* is true) or for visualisation (the result is false).
	*/
	public boolean getUsedForInput() {
		return usedForInput;
	}

	/**
	* Arranges the vertices of the specified polyline in the following order:
	* 0) upper-left corner; 1) upper-right corner; 2) lower-right corner;
	* 3) lower-left corner; 4) upper-left corner
	*/
	protected static void orderPoly(RealPolyline poly) {
		if (poly == null || poly.p == null || poly.p.length < 4)
			return;
		//check if the vertices are already in the desired order
		if (poly.p.length == 5 && poly.p[4].equals(poly.p[0]) && poly.p[0].y > poly.p[2].y && poly.p[0].y > poly.p[3].y && poly.p[1].y > poly.p[2].y && poly.p[1].y > poly.p[3].y && poly.p[0].x < poly.p[1].x && poly.p[0].x < poly.p[2].x
				&& poly.p[3].x < poly.p[1].x && poly.p[3].x < poly.p[2].x)
			return;
		int xOrd[] = new int[4], k = 1;
		xOrd[0] = 0;
		for (int i = 1; i < 4; i++) {
			int pos = k;
			for (int j = 0; j < k; j++)
				if (poly.p[i].x < poly.p[xOrd[j]].x) {
					pos = j;
					break;
				}
			if (pos < k) {
				for (int j = k; j > pos; j--) {
					xOrd[j] = xOrd[j - 1];
				}
			}
			xOrd[pos] = i;
			++k;
		}
		RealPoint rp[] = new RealPoint[5];
		if (poly.p[xOrd[0]].y > poly.p[xOrd[1]].y) {
			rp[0] = poly.p[xOrd[0]];
			rp[3] = poly.p[xOrd[1]];
		} else {
			rp[3] = poly.p[xOrd[0]];
			rp[0] = poly.p[xOrd[1]];
		}
		if (poly.p[xOrd[2]].y > poly.p[xOrd[3]].y) {
			rp[1] = poly.p[xOrd[2]];
			rp[2] = poly.p[xOrd[3]];
		} else {
			rp[2] = poly.p[xOrd[2]];
			rp[1] = poly.p[xOrd[3]];
		}
		rp[4] = rp[0];
		poly.p = rp;
	}

	/**
	* Depending on the current level of detail, draws the grid corresponding to
	* the given object. If the level is 0, does not draw any additional lines.
	*/
	protected void drawGridForObject(DGeoObject gobj, Graphics g, MapContext mc) {
		if (gobj.getGeometry() == null)
			return;
		if (gobj.getGeometry() instanceof RealRectangle) {
			RealRectangle rr = (RealRectangle) gobj.getGeometry();
			int x1 = mc.scrX(rr.rx1, rr.ry1), y2 = mc.scrY(rr.rx1, rr.ry1), x2 = mc.scrX(rr.rx2, rr.ry2), y1 = mc.scrY(rr.rx2, rr.ry2);
			g.setColor(drawParm.lineColor);
			g.drawRect(x1, y1, x2 - x1, y2 - y1);
			g.drawRect(x1 + 1, y1 + 1, x2 - x1 - 2, y2 - y1 - 2);
			divideRectangle(g, level, x1, y1, x2, y2);
		} else if (gobj.getGeometry() instanceof RealPolyline) {
			RealPolyline poly = (RealPolyline) gobj.getGeometry();
			if (poly.p == null || poly.p.length < 4)
				return;
			g.setColor(drawParm.lineColor);
			if (pX == null) {
				pX = new int[5];
				pY = new int[5];
			}
			pX[0] = mc.scrX(poly.p[0].x, poly.p[0].y);
			pY[0] = mc.scrY(poly.p[0].x, poly.p[0].y);
			for (int i = 1; i <= 4; i++) {
				pX[i] = (i == 4) ? pX[0] : mc.scrX(poly.p[i].x, poly.p[i].y);
				pY[i] = (i == 4) ? pY[0] : mc.scrY(poly.p[i].x, poly.p[i].y);
				g.drawLine(pX[i - 1], pY[i - 1], pX[i], pY[i]);
				if (Math.abs(pX[i] - pX[i - 1]) > Math.abs(pY[i] - pY[i - 1])) {
					g.drawLine(pX[i - 1], pY[i - 1] + 1, pX[i], pY[i] + 1);
				} else {
					g.drawLine(pX[i - 1] + 1, pY[i - 1], pX[i] + 1, pY[i]);
				}
			}
			dividePolygon(g, level, pX, pY);
		}
	}

	/**
	* Divides the given rectangle into 4 equal parts by 1 horizontal and 1
	* vertical line. Then recursively aplies the same procedure to each of the
	* parts until the specified level is reached
	*/
	protected static void divideRectangle(Graphics g, int level, int x1, int y1, int x2, int y2) {
		if (level < 1 || x2 - x1 < 2 || y2 - y1 < 2)
			return;
		int w = (x2 - x1) / 2, h = (y2 - y1) / 2;
		g.drawLine(x1 + w, y1, x1 + w, y2);
		g.drawLine(x1, y1 + h, x2, y1 + h);
		--level;
		if (level < 1)
			return;
		divideRectangle(g, level, x1, y1, x1 + w, y1 + h);
		divideRectangle(g, level, x1 + w + 1, y1, x2, y1 + h);
		divideRectangle(g, level, x1, y1 + h + 1, x1 + w, y2);
		divideRectangle(g, level, x1 + w + 1, y1 + h + 1, x2, y2);
	}

	/**
	* Divides the given 4-angular polygon into 4 equal parts by 1 horizontal and 1
	* vertical line. Then recursively aplies the same procedure to each of the
	* parts until the specified level is reached
	*/
	protected static void dividePolygon(Graphics g, int level, int px[], int py[]) {
		if (level < 1)
			return;
		if (px[1] - px[0] < 2 || py[2] - py[1] < 2)
			return;
		int mx1 = (px[1] + px[0] + 1) / 2, mx2 = (px[2] + px[1] + 1) / 2, mx3 = (px[3] + px[2] + 1) / 2, mx4 = (px[0] + px[3] + 1) / 2;
		int my1 = (py[1] + py[0] + 1) / 2, my2 = (py[2] + py[1] + 1) / 2, my3 = (py[3] + py[2] + 1) / 2, my4 = (py[0] + py[3] + 1) / 2;
		g.drawLine(mx1, my1, mx3, my3);
		g.drawLine(mx2, my2, mx4, my4);
		--level;
		if (level < 1)
			return;
		int cx = (mx2 + mx4 + 1) / 2, cy = (my1 + my3 + 1) / 2;
		int ppx[] = new int[5], ppy[] = new int[5];
		ppx[0] = px[0];
		ppy[0] = py[0];
		ppx[1] = mx1;
		ppy[1] = my1;
		ppx[2] = cx;
		ppy[2] = cy;
		ppx[3] = mx4;
		ppy[3] = my4;
		ppx[4] = ppx[0];
		ppy[4] = ppy[0];
		dividePolygon(g, level, ppx, ppy);
		ppx[0] = mx1;
		ppy[0] = my1;
		ppx[1] = px[1];
		ppy[1] = py[1];
		ppx[2] = mx2;
		ppy[2] = my2;
		ppx[3] = cx;
		ppy[3] = cy;
		ppx[4] = ppx[0];
		ppy[4] = ppy[0];
		dividePolygon(g, level, ppx, ppy);
		ppx[0] = cx;
		ppy[0] = cy;
		ppx[1] = mx2;
		ppy[1] = my2;
		ppx[2] = px[2];
		ppy[2] = py[2];
		ppx[3] = mx3;
		ppy[3] = my3;
		ppx[4] = ppx[0];
		ppy[4] = ppy[0];
		dividePolygon(g, level, ppx, ppy);
		ppx[0] = mx4;
		ppy[0] = my4;
		ppx[1] = cx;
		ppy[1] = cy;
		ppx[2] = mx3;
		ppy[2] = my3;
		ppx[3] = px[3];
		ppy[3] = py[3];
		ppx[4] = ppx[0];
		ppy[4] = ppy[0];
		dividePolygon(g, level, ppx, ppy);
	}

	/**
	* A QuadTreeLayer is drawn in a special way (as a grid), depending on the
	* current level of detail.
	*/
	@Override
	public void draw(Graphics g, MapContext mc) {
		if (g == null || mc == null)
			return;
		if (drawParm.maxLevels < 0) {
			drawParm.maxLevels = 3; //default value
		}
		lastPixelValue = mc.getPixelValue();
		if (geoObj == null || !hasAllObjects) {
			loadGeoObjects(mc);
		}
		if (geoObj == null)
			return;
		if ((vis != null || bkgVis != null) && dTable != null && !linkedToTable) {
			linkToThematicData();
		}
		RealRectangle rr = mc.getVisibleTerritory();
		Visualizer v1 = null, v2 = null;
		if (vis != null && vis.isEnabled()) {
			v1 = vis;
		}
		if (bkgVis != null && bkgVis.isEnabled()) {
			v2 = bkgVis;
		}
		nActive = 0;
		for (int i = 0; i < getObjectCount(); i++) {
			if (!isObjectActive(i)) {
				continue;
			}
			++nActive;
			DGeoObject gobj = getObject(i);
			gobj.setDrawingParameters(drawParm);
			if (gobj.fitsInRectangle(rr.rx1, rr.ry1, rr.rx2, rr.ry2)) {
				if (usedForInput) {
					drawGridForObject(gobj, g, mc);
				} else if ((v1 != null || v2 != null) && gobj.getData() != null) {
					gobj.setVisualizer(v1);
					gobj.setBackgroundVisualizer(v2);
					gobj.draw(g, mc);
				}
			}
		}
	}

	/*
	* Function drawStrictly() is like normal draw().
	* It is needed if we try to avoid drawing of geo-objects with area geometry
	* that have their center out of visible territory.
	*/
	@Override
	public void drawStrictly(Graphics g, MapContext mc) {
		if (g == null || mc == null)
			return;
		lastPixelValue = mc.getPixelValue();
		if (geoObj == null || !hasAllObjects) {
			loadGeoObjects(mc);
		}
		if (geoObj == null)
			return;
		if ((vis != null || bkgVis != null) && dTable != null && !linkedToTable) {
			linkToThematicData();
		}
		RealRectangle rr = mc.getVisibleTerritory();
		Visualizer v1 = null, v2 = null;
		if (vis != null && vis.isEnabled()) {
			v1 = vis;
		}
		if (bkgVis != null && bkgVis.isEnabled()) {
			v2 = bkgVis;
		}
		for (int i = 0; i < getObjectCount(); i++) {
			if (!isObjectActive(i)) {
				continue;
			}
			DGeoObject gobj = getObject(i);
			gobj.setDrawingParameters(drawParm);
			RealRectangle objLabelRect = gobj.getLabelRectangle();
			float objCenterX = (objLabelRect.rx1 + objLabelRect.rx2) / 2, objCenterY = (objLabelRect.ry1 + objLabelRect.ry2) / 2;
			if (gobj.fitsInRectangle(rr.rx1, rr.ry1, rr.rx2, rr.ry2) && rr.contains(objCenterX, objCenterY, 0.0f)) {
				if (usedForInput) {
					drawGridForObject(gobj, g, mc);
				} else if ((v1 != null || v2 != null) && gobj.getData() != null) {
					gobj.setVisualizer(v1);
					gobj.setBackgroundVisualizer(v2);
					gobj.draw(g, mc);
				}
			}
		}
	}

	/**
	* This method is used to find the objects pointed to with the mouse.
	* Depending on the current depth level, may return identifiers of "virtual"
	* objects: the identifier of the top-level rectangle is modified by representing
	* the position of the selected part in it.
	* When the argument findOne is true, the method returns after finding the
	* first object at the mouse position
	*/
	@Override
	public Vector findObjectsAt(int x, int y, MapContext mc, boolean findOne) {
		if (!drawParm.drawLayer)
			return null;
		if (mc == null)
			return null;
		Vector pointed = new Vector(10, 10);
		float rx = mc.absX(x), ry = mc.absY(y);
		for (int i = 0; i < getObjectCount(); i++) {
			if (!isObjectActive(i)) {
				continue;
			}
			DGeoObject gobj = getObject(i);
			Geometry geom = gobj.getGeometry();
			if (geom == null) {
				continue;
			}
			if (geom.contains(rx, ry, 0f)) {
				String id = gobj.getIdentifier();
				if (level > 0) {
					//determine to which part of the object's rectangle the point belongs
					IntArray pos = null;
					if (geom instanceof RealRectangle) {
						RealRectangle r = (RealRectangle) gobj.getGeometry();
						pos = getPointPosInRect(rx, ry, r.rx1, r.ry1, r.rx2, r.ry2, level, null);
					} else if (geom instanceof RealPolyline) {
						RealPolyline pLine = (RealPolyline) geom;
						pos = getPointPosInPolygon(rx, ry, pLine.p, level, null);
					}
					if (pos != null) {
						for (int j = 0; j < pos.size(); j++) {
							id += "." + pos.elementAt(j);
						}
					}
				}
				pointed.addElement(id);
				if (findOne)
					return pointed;
			}
		}
		if (pointed.size() < 1)
			return null;
		return pointed;
	}

	/**
	* Returns the identifier of the object (cell) containing the given position.
	* Depending on the current depth level, may return identifiers of "virtual"
	* objects: the identifier of the top-level rectangle is modified by representing
	* the position of the selected part in it.
	*/
	public String findObjectContainingPoint(float rx, float ry) {
		for (int i = 0; i < getObjectCount(); i++) {
			DGeoObject gobj = getObject(i);
			Geometry geom = gobj.getGeometry();
			if (geom == null) {
				continue;
			}
			if (geom.contains(rx, ry, 0)) {
				String id = gobj.getIdentifier();
				if (level > 0) {
					//determine to which part of the object's rectangle the point belongs
					IntArray pos = null;
					if (geom instanceof RealRectangle) {
						RealRectangle r = (RealRectangle) gobj.getGeometry();
						pos = getPointPosInRect(rx, ry, r.rx1, r.ry1, r.rx2, r.ry2, level, null);
					} else if (geom instanceof RealPolyline) {
						RealPolyline pLine = (RealPolyline) geom;
						pos = getPointPosInPolygon(rx, ry, pLine.p, level, null);
					}
					if (pos != null) {
						for (int j = 0; j < pos.size(); j++) {
							id += "." + pos.elementAt(j);
						}
					}
				}
				return id;
			}
		}
		return null;
	}

	/**
	* Determines to which part of the given rectangle the given point fits.
	* Returns the sequence of the indexes in the hierarchy.
	*/
	protected IntArray getPointPosInRect(float px, float py, float x1, float y1, float x2, float y2, int level, IntArray result) {
		if (level < 1)
			return result;
		if (result == null) {
			result = new IntArray(getMaxLevel(), 1);
		}
		float mx = (x1 + x2) / 2, my = (y1 + y2) / 2;
		if (px <= mx)
			if (py >= my) {
				result.addElement(1);
				return getPointPosInRect(px, py, x1, my, mx, y2, level - 1, result);
			} else {
				result.addElement(3);
				return getPointPosInRect(px, py, x1, y1, mx, my, level - 1, result);
			}
		if (py >= my) {
			result.addElement(2);
			return getPointPosInRect(px, py, mx, my, x2, y2, level - 1, result);
		}
		result.addElement(4);
		return getPointPosInRect(px, py, mx, y1, x2, my, level - 1, result);
	}

	/**
	* Determines to which part of the given 4-angular polygon the given point fits.
	* Returns the sequence of the indexes in the hierarchy.
	*/
	protected IntArray getPointPosInPolygon(float px, float py, RealPoint poly[], int level, IntArray result) {
		if (level < 1)
			return result;
		if (result == null) {
			result = new IntArray(getMaxLevel(), 1);
		}
		RealPoint mp1 = new RealPoint((poly[0].x + poly[1].x) / 2, (poly[0].y + poly[1].y) / 2), mp2 = new RealPoint((poly[1].x + poly[2].x) / 2, (poly[1].y + poly[2].y) / 2), mp3 = new RealPoint((poly[2].x + poly[3].x) / 2,
				(poly[2].y + poly[3].y) / 2), mp4 = new RealPoint((poly[3].x + poly[0].x) / 2, (poly[3].y + poly[0].y) / 2), cp = new RealPoint((mp4.x + mp2.x) / 2, (mp1.y + mp3.y) / 2);
		RealPoint rPoly[] = new RealPoint[5];
		if (px <= mp1.x && px <= cp.x) {
			if (py >= mp4.y && py >= cp.y) {
				result.addElement(1);
				rPoly[0] = poly[0];
				rPoly[1] = mp1;
				rPoly[2] = cp;
				rPoly[3] = mp4;
				rPoly[4] = rPoly[0];
				return getPointPosInPolygon(px, py, rPoly, level - 1, result);
			}
			if (py <= mp4.y && py <= cp.y) {
				result.addElement(3);
				rPoly[0] = mp4;
				rPoly[1] = cp;
				rPoly[2] = mp3;
				rPoly[3] = poly[3];
				rPoly[4] = rPoly[0];
				return getPointPosInPolygon(px, py, rPoly, level - 1, result);
			}
		}
		if (px >= mp1.x && px >= cp.x) {
			if (py >= mp4.y && py >= cp.y) {
				result.addElement(2);
				rPoly[0] = mp1;
				rPoly[1] = poly[1];
				rPoly[2] = mp2;
				rPoly[3] = cp;
				rPoly[4] = rPoly[0];
				return getPointPosInPolygon(px, py, rPoly, level - 1, result);
			}
			if (py <= mp4.y && py <= cp.y) {
				result.addElement(4);
				rPoly[0] = cp;
				rPoly[1] = mp2;
				rPoly[2] = poly[2];
				rPoly[3] = mp3;
				rPoly[4] = rPoly[0];
				return getPointPosInPolygon(px, py, rPoly, level - 1, result);
			}
		}
		RealPolyline pLine = new RealPolyline();
		pLine.p = rPoly;
		rPoly[0] = poly[0];
		rPoly[1] = mp1;
		rPoly[2] = cp;
		rPoly[3] = mp4;
		rPoly[4] = rPoly[0];
		if (pLine.contains(px, py, 0f, true)) {
			result.addElement(1);
			return getPointPosInPolygon(px, py, rPoly, level - 1, result);
		}
		rPoly[0] = mp4;
		rPoly[1] = cp;
		rPoly[2] = mp3;
		rPoly[3] = poly[3];
		rPoly[4] = rPoly[0];
		if (pLine.contains(px, py, 0f, true)) {
			result.addElement(3);
			return getPointPosInPolygon(px, py, rPoly, level - 1, result);
		}
		rPoly[0] = mp1;
		rPoly[1] = poly[1];
		rPoly[2] = mp2;
		rPoly[3] = cp;
		rPoly[4] = rPoly[0];
		if (pLine.contains(px, py, 0f, true)) {
			result.addElement(2);
			return getPointPosInPolygon(px, py, rPoly, level - 1, result);
		}
		result.addElement(4);
		rPoly[0] = cp;
		rPoly[1] = mp2;
		rPoly[2] = poly[2];
		rPoly[3] = mp3;
		rPoly[4] = rPoly[0];
		return getPointPosInPolygon(px, py, rPoly, level - 1, result);
	}

	/**
	* Finds objects fitting in the specified rectangle.
	*/
	@Override
	public Vector findObjectsIn(int x1, int y1, int x2, int y2, MapContext mc) {
		if (!drawParm.drawLayer)
			return null;
		if (mc == null)
			return null;
		if (x1 == x2 && y1 == y2)
			return findObjectsAt(x1, y1, mc, false);
		float rx1 = mc.absX(x1), ry1 = mc.absY(y1), rx2 = mc.absX(x2), ry2 = mc.absY(y2);
		if (rx1 > rx2) {
			float f = rx1;
			rx1 = rx2;
			rx2 = f;
		}
		if (ry1 > ry2) {
			float f = ry1;
			ry1 = ry2;
			ry2 = f;
		}
		Vector fit = new Vector(20, 10);
		for (int i = 0; i < getObjectCount(); i++) {
			if (!isObjectActive(i)) {
				continue;
			}
			DGeoObject gobj = getObject(i);
			Geometry geom = gobj.getGeometry();
			if (geom == null) {
				continue;
			}
			if (!geom.fitsInRectangle(rx1, ry1, rx2, ry2)) {
				continue;
			}
			if (level < 1) {
				fit.addElement(gobj.getIdentifier());
				continue;
			}
			Vector v = null;
			if (gobj.getGeometry() instanceof RealRectangle) {
				v = getLowLevelObjectsFitInRectangle(gobj.getIdentifier(), (RealRectangle) gobj.getGeometry(), rx1, ry1, rx2, ry2, 0, null);
			} else if (gobj.getGeometry() instanceof RealPolyline) {
				v = getLowLevelObjectsFitInRectangle(gobj.getIdentifier(), (RealPolyline) gobj.getGeometry(), rx1, ry1, rx2, ry2, 0, null);
			}
			if (v == null) {
				continue;
			}
			for (int j = 0; j < v.size(); j++) {
				Object pair[] = (Object[]) v.elementAt(j);
				fit.addElement(pair[0].toString());
			}
		}
		if (fit.size() < 1)
			return null;
		return fit;
	}

	/**
	* Returns all sub-objects of the given object according to the current grid
	* depth (level of detail). The objects are returned as arrays containing
	* object identifiers on the first place and their rectangles (instances
	* of RealRectangle) on the second  place.
	*/
	protected Vector getLowLevelObjects(String objId, RealRectangle r, int lev, Vector result) {
		if (result == null) {
			int maxN = 1;
			for (int i = 1; i <= level; i++) {
				maxN *= 4;
			}
			result = new Vector(maxN, 10);
		}
		if (lev == level) {
			Object pair[] = new Object[2];
			pair[0] = objId;
			pair[1] = r;
			result.addElement(pair);
		} else {
			float mx = (r.rx1 + r.rx2) / 2, my = (r.ry1 + r.ry2) / 2;
			getLowLevelObjects(objId + ".1", new RealRectangle(r.rx1, my, mx, r.ry2), lev + 1, result);
			getLowLevelObjects(objId + ".2", new RealRectangle(mx, my, r.rx2, r.ry2), lev + 1, result);
			getLowLevelObjects(objId + ".3", new RealRectangle(r.rx1, r.ry1, mx, my), lev + 1, result);
			getLowLevelObjects(objId + ".4", new RealRectangle(mx, r.ry1, r.rx2, my), lev + 1, result);
		}
		return result;
	}

	/**
	* Returns all sub-objects of the given object according to the current grid
	* depth (level of detail). The objects are returned as arrays containing
	* object identifiers on the first place and their polygons (instances
	* of RealPolyline) on the second  place.
	*/
	protected Vector getLowLevelObjects(String objId, RealPolyline poly, int lev, Vector result) {
		if (result == null) {
			int maxN = 1;
			for (int i = 1; i <= level; i++) {
				maxN *= 4;
			}
			result = new Vector(maxN, 10);
		}
		if (lev == level) {
			Object pair[] = new Object[2];
			pair[0] = objId;
			pair[1] = poly;
			result.addElement(pair);
		} else {
			RealPoint mp1 = new RealPoint((poly.p[0].x + poly.p[1].x) / 2, (poly.p[0].y + poly.p[1].y) / 2), mp2 = new RealPoint((poly.p[1].x + poly.p[2].x) / 2, (poly.p[1].y + poly.p[2].y) / 2), mp3 = new RealPoint((poly.p[2].x + poly.p[3].x) / 2,
					(poly.p[2].y + poly.p[3].y) / 2), mp4 = new RealPoint((poly.p[3].x + poly.p[0].x) / 2, (poly.p[3].y + poly.p[0].y) / 2), cp = new RealPoint((mp4.x + mp2.x) / 2, (mp1.y + mp3.y) / 2);
			RealPolyline pLine = new RealPolyline();
			pLine.p = new RealPoint[5];
			pLine.p[0] = poly.p[0];
			pLine.p[1] = mp1;
			pLine.p[2] = cp;
			pLine.p[3] = mp4;
			pLine.p[4] = pLine.p[0];
			getLowLevelObjects(objId + ".1", pLine, lev + 1, result);
			pLine = new RealPolyline();
			pLine.p = new RealPoint[5];
			pLine.p[0] = mp1;
			pLine.p[1] = poly.p[1];
			pLine.p[2] = mp2;
			pLine.p[3] = cp;
			pLine.p[4] = pLine.p[0];
			getLowLevelObjects(objId + ".2", pLine, lev + 1, result);
			pLine = new RealPolyline();
			pLine.p = new RealPoint[5];
			pLine.p[0] = mp4;
			pLine.p[1] = cp;
			pLine.p[2] = mp3;
			pLine.p[3] = poly.p[3];
			pLine.p[4] = pLine.p[0];
			getLowLevelObjects(objId + ".3", pLine, lev + 1, result);
			pLine = new RealPolyline();
			pLine.p = new RealPoint[5];
			pLine.p[0] = cp;
			pLine.p[1] = mp2;
			pLine.p[2] = poly.p[2];
			pLine.p[3] = mp3;
			pLine.p[4] = pLine.p[0];
			getLowLevelObjects(objId + ".4", pLine, lev + 1, result);
		}
		return result;
	}

	/**
	* According to the current grid depth (level of detail), returns the
	* sub-objects of the given object which fit (at least partly) in the given
	* rectangle. The objects are returned as arrays containing
	* object identifiers on the first place and their rectangles (instances
	* of RealRectangle) on the second  place.
	*/
	protected Vector getLowLevelObjectsFitInRectangle(String objId, RealRectangle r, float x1, float y1, float x2, float y2, int lev, Vector result) {
		if (!r.fitsInRectangle(x1, y1, x2, y2))
			return result;
		if (result == null) {
			int maxN = 1;
			for (int i = 1; i <= level; i++) {
				maxN *= 4;
			}
			result = new Vector(maxN, 10);
		}
		if (lev == level) {
			Object pair[] = new Object[2];
			pair[0] = objId;
			pair[1] = r;
			result.addElement(pair);
		} else {
			float mx = (r.rx1 + r.rx2) / 2, my = (r.ry1 + r.ry2) / 2;
			getLowLevelObjectsFitInRectangle(objId + ".1", new RealRectangle(r.rx1, my, mx, r.ry2), x1, y1, x2, y2, lev + 1, result);
			getLowLevelObjectsFitInRectangle(objId + ".2", new RealRectangle(mx, my, r.rx2, r.ry2), x1, y1, x2, y2, lev + 1, result);
			getLowLevelObjectsFitInRectangle(objId + ".3", new RealRectangle(r.rx1, r.ry1, mx, my), x1, y1, x2, y2, lev + 1, result);
			getLowLevelObjectsFitInRectangle(objId + ".4", new RealRectangle(mx, r.ry1, r.rx2, my), x1, y1, x2, y2, lev + 1, result);
		}
		return result;
	}

	/**
	* According to the current grid depth (level of detail), returns the
	* sub-objects of the given object which fit (at least partly) in the given
	* rectangle. The objects are returned as arrays containing
	* object identifiers on the first place and their polygons (instances
	* of RealPolyline) on the second  place.
	*/
	protected Vector getLowLevelObjectsFitInRectangle(String objId, RealPolyline poly, float x1, float y1, float x2, float y2, int lev, Vector result) {
		if (!poly.fitsInRectangle(x1, y1, x2, y2))
			return result;
		if (result == null) {
			int maxN = 1;
			for (int i = 1; i <= level; i++) {
				maxN *= 4;
			}
			result = new Vector(maxN, 10);
		}
		if (lev == level) {
			Object pair[] = new Object[2];
			pair[0] = objId;
			pair[1] = poly;
			result.addElement(pair);
		} else {
			RealPoint mp1 = new RealPoint((poly.p[0].x + poly.p[1].x) / 2, (poly.p[0].y + poly.p[1].y) / 2), mp2 = new RealPoint((poly.p[1].x + poly.p[2].x) / 2, (poly.p[1].y + poly.p[2].y) / 2), mp3 = new RealPoint((poly.p[2].x + poly.p[3].x) / 2,
					(poly.p[2].y + poly.p[3].y) / 2), mp4 = new RealPoint((poly.p[3].x + poly.p[0].x) / 2, (poly.p[3].y + poly.p[0].y) / 2), cp = new RealPoint((mp4.x + mp2.x) / 2, (mp1.y + mp3.y) / 2);
			RealPolyline pLine = new RealPolyline();
			pLine.p = new RealPoint[5];
			pLine.p[0] = poly.p[0];
			pLine.p[1] = mp1;
			pLine.p[2] = cp;
			pLine.p[3] = mp4;
			pLine.p[4] = pLine.p[0];
			getLowLevelObjectsFitInRectangle(objId + ".1", pLine, x1, y1, x2, y2, lev + 1, result);
			pLine = new RealPolyline();
			pLine.p = new RealPoint[5];
			pLine.p[0] = mp1;
			pLine.p[1] = poly.p[1];
			pLine.p[2] = mp2;
			pLine.p[3] = cp;
			pLine.p[4] = pLine.p[0];
			getLowLevelObjectsFitInRectangle(objId + ".2", pLine, x1, y1, x2, y2, lev + 1, result);
			pLine = new RealPolyline();
			pLine.p = new RealPoint[5];
			pLine.p[0] = mp4;
			pLine.p[1] = cp;
			pLine.p[2] = mp3;
			pLine.p[3] = poly.p[3];
			pLine.p[4] = pLine.p[0];
			getLowLevelObjectsFitInRectangle(objId + ".3", pLine, x1, y1, x2, y2, lev + 1, result);
			pLine = new RealPolyline();
			pLine.p = new RealPoint[5];
			pLine.p[0] = cp;
			pLine.p[1] = mp2;
			pLine.p[2] = poly.p[2];
			pLine.p[3] = mp3;
			pLine.p[4] = pLine.p[0];
			getLowLevelObjectsFitInRectangle(objId + ".4", pLine, x1, y1, x2, y2, lev + 1, result);
		}
		return result;
	}

	/**
	* Returns the index of the object with the given identifier.
	* If this is a hierarchical identifier, returns the index of the top level
	* object.
	*/
	@Override
	public int getObjectIndex(String ident) {
		if (ident == null)
			return -1;
		int didx = ident.indexOf('.');
		if (didx > 0) {
			ident = ident.substring(0, didx);
		}
		for (int i = 0; i < getObjectCount(); i++) {
			DGeoObject gobj = getObject(i);
			if (gobj != null && ident.equalsIgnoreCase(gobj.getIdentifier()))
				return i;
		}
		return -1;
	}

//---------------------- highlighting --------------------------------
	/**
	* Checks if the object with the given identifier is currently selected.
	* Identifiers of selected objects are stored in the vector selIds.
	*/
	public boolean isObjectSelected(String objId) {
		if (objId == null || selIds == null)
			return false;
		return StringUtil.isStringInVectorIgnoreCase(objId, selIds);
	}

	/**
	* Checks if the object with the given identifier is currently highlighted.
	*/
	public boolean isObjectHighlighted(String objId) {
		if (objId == null || hlObjId == null)
			return false;
		return objId.equalsIgnoreCase(hlObjId);
	}

	/**
	* A method from the basic GeoLayer interface.
	* This method is used to switch on/off transient highlighting of the
	* geographical entity specified through the identifier.
	* The GeoLayer should draw the entity in "highlighted" state.
	*/
	@Override
	public boolean highlightObject(String objectId, boolean isHighlighted, Graphics g, MapContext mc) {
		if (!drawParm.drawLayer)
			return false;
		boolean hl = isObjectHighlighted(objectId);
		if (isHighlighted == hl)
			return true;
		Geometry r = (hl) ? hlObjRect : getObjectRectangle(objectId);
		if (r == null)
			return false;
		highlight(g, mc, objectId, r);
		if (!isHighlighted) {
			hlObjId = null;
			hlObjRect = null;
		} else {
			hlObjId = objectId;
			hlObjRect = r;
		}
		return true;
	}

	/**
	* Switches on/off transient highlighting of the object but does not redraw it.
	*/
	@Override
	public boolean setObjectHighlight(String objectId, boolean isHighlighted) {
		if (!drawParm.drawLayer)
			return false;
		boolean hl = isObjectHighlighted(objectId);
		if (isHighlighted == hl)
			return true;
		Geometry r = (hl) ? hlObjRect : getObjectRectangle(objectId);
		if (r == null)
			return false;
		if (!isHighlighted) {
			hlObjId = null;
			hlObjRect = null;
		} else {
			hlObjId = objectId;
			hlObjRect = r;
		}
		return true;
	}

	/**
	* Hides visibility of highlighting (without change of the status of the
	* highlighted objects)
	*/
	@Override
	public void hideHighlighting(Graphics g, MapContext mc) {
		if (!drawParm.drawLayer || g == null || mc == null)
			return;
		if (hlObjId == null)
			return;
		highlight(g, mc, hlObjId, hlObjRect);
	}

	/**
	* Shows currently highlighted objects (without change of the status of the
	* highlighted objects)
	*/
	@Override
	public void showHighlighting(Graphics g, MapContext mc) {
		if (!drawParm.drawLayer || g == null || mc == null)
			return;
		if (hlObjId == null)
			return;
		highlight(g, mc, hlObjId, hlObjRect);
	}

	/**
	* Dehighlights all the highlighted objects (changes their status, but
	* does not draw).
	*/
	@Override
	public void dehighlightAllObjects() {
		hlObjId = null;
		hlObjRect = null;
	}

	/**
	* A method from the basic GeoLayer interface.
	* This method is used to switch on/off selection (durable highlighting).
	* The GeoLayer should draw the entity in "selected" state.
	*/
	@Override
	public boolean selectObject(String objectId, boolean isSelected, Graphics g, MapContext mc) {
		boolean sel = isObjectSelected(objectId);
		if (sel == isSelected)
			return true;
		if (isSelected) {
			Geometry r = getObjectRectangle(objectId);
			if (r == null)
				return false;
			if (selIds == null) {
				selIds = new Vector(100, 20);
				selRectangles = new Vector(100, 20);
			}
			selIds.addElement(objectId);
			selRectangles.addElement(r);
			if (drawParm.drawLayer) {
				showSelection(g, mc, r);
			}
		} else {
			int idx = StringUtil.indexOfStringInVectorIgnoreCase(objectId, selIds);
			if (idx < 0)
				return false;
			selIds.removeElementAt(idx);
			selRectangles.removeElementAt(idx);
		}
		return true;
	}

	/**
	* Draws all selected objects on top of other objects of the layer
	*/
	@Override
	public void drawSelectedObjects(Graphics g, MapContext mc) {
		if (selIds == null || g == null || mc == null || selIds.size() < 1)
			return;
		RealRectangle rr = mc.getVisibleTerritory();
		for (int i = 0; i < selIds.size(); i++) {
			Geometry r = (Geometry) selRectangles.elementAt(i);
			if (r.fitsInRectangle(rr.rx1, rr.ry1, rr.rx2, rr.ry2)) {
				showSelection(g, mc, r);
			}
		}
	}

	/**
	* For the given identifier of the object (possibly, virtual) returns the
	* corresponding rectangle
	*/
	protected Geometry getObjectRectangle(String ident) {
		if (ident == null)
			return null;
		if (selIds != null) {
			int sidx = StringUtil.indexOfStringInVectorIgnoreCase(ident, selIds);
			if (sidx >= 0)
				return (Geometry) selRectangles.elementAt(sidx);
		}
		int didx = ident.indexOf('.');
		String topId = (didx > 0) ? ident.substring(0, didx) : ident;
		for (int i = 0; i < getObjectCount(); i++) {
			DGeoObject gobj = (DGeoObject) geoObj.elementAt(i);
			if (gobj != null && topId.equalsIgnoreCase(gobj.getIdentifier())) {
				Geometry geom = gobj.getGeometry();
				if (geom == null)
					return null;
				if (didx < 0)
					return geom;
				return getSubRectangle(geom, ident.substring(didx + 1));
			}
		}
		return null;
	}

	/**
	* According to the specified hierarchical "path", retrieves the coordinates
	* of the corresponding sub-rectangle or sub-polygon of the given rectangle
	* or 4-angular polygon
	*/
	protected Geometry getSubRectangle(Geometry sou, String path) {
		if (path == null || path.length() < 1 || sou == null)
			return sou;
		int didx = path.indexOf('.');
		String posStr = (didx > 0) ? path.substring(0, didx) : path;
		int pos = -1;
		try {
			pos = Integer.valueOf(posStr).intValue();
		} catch (NumberFormatException e) {
		}
		if (pos < 1 || pos > 4)
			return sou;
		if (sou instanceof RealRectangle) {
			RealRectangle rr = (RealRectangle) sou;
			float mx = (rr.rx1 + rr.rx2) / 2, my = (rr.ry1 + rr.ry2) / 2;
			RealRectangle r = null;
			switch (pos) {
			case 1:
				r = new RealRectangle(rr.rx1, my, mx, rr.ry2);
				break;
			case 2:
				r = new RealRectangle(mx, my, rr.rx2, rr.ry2);
				break;
			case 3:
				r = new RealRectangle(rr.rx1, rr.ry1, mx, my);
				break;
			case 4:
				r = new RealRectangle(mx, rr.ry1, rr.rx2, my);
				break;
			}
			if (didx < 0)
				return r;
			return getSubRectangle(r, path.substring(didx + 1));
		}
		if (sou instanceof RealPolyline) {
			RealPolyline poly = (RealPolyline) sou;
			if (poly.p == null || poly.p.length < 4)
				return sou;
			RealPoint mp1 = new RealPoint((poly.p[0].x + poly.p[1].x) / 2, (poly.p[0].y + poly.p[1].y) / 2), mp2 = new RealPoint((poly.p[1].x + poly.p[2].x) / 2, (poly.p[1].y + poly.p[2].y) / 2), mp3 = new RealPoint((poly.p[2].x + poly.p[3].x) / 2,
					(poly.p[2].y + poly.p[3].y) / 2), mp4 = new RealPoint((poly.p[3].x + poly.p[0].x) / 2, (poly.p[3].y + poly.p[0].y) / 2), cp = new RealPoint((mp4.x + mp2.x) / 2, (mp1.y + mp3.y) / 2);
			RealPolyline pLine = new RealPolyline();
			pLine.p = new RealPoint[5];
			switch (pos) {
			case 1:
				pLine.p[0] = poly.p[0];
				pLine.p[1] = mp1;
				pLine.p[2] = cp;
				pLine.p[3] = mp4;
				break;
			case 2:
				pLine.p[0] = mp1;
				pLine.p[1] = poly.p[1];
				pLine.p[2] = mp2;
				pLine.p[3] = cp;
				break;
			case 3:
				pLine.p[0] = mp4;
				pLine.p[1] = cp;
				pLine.p[2] = mp3;
				pLine.p[3] = poly.p[3];
				break;
			case 4:
				pLine.p[0] = cp;
				pLine.p[1] = mp2;
				pLine.p[2] = poly.p[2];
				pLine.p[3] = mp3;
				break;
			}
			pLine.p[4] = pLine.p[0];
			if (didx < 0)
				return pLine;
			return getSubRectangle(pLine, path.substring(didx + 1));
		}
		return sou;
	}

	/**
	* This method is used to switch on/off transient highlighting of "virtual"
	* objects. The argument geom must be an instance of either RealRectangle or
	* RealPolyline (4-angular polygon)
	*/
	public void highlight(Graphics g, MapContext mc, String objId, Geometry geom) {
		drawInXORMode(g, mc, geom, isObjectSelected(objId));
	}

	/**
	* Used for highlighting of "virtual" objects. The argument geom must be an
	* instance of either RealRectangle or RealPolyline (4-angular polygon)
	*/
	protected void drawInXORMode(Graphics g, MapContext mc, Geometry geom, boolean selected) {
		if (g == null || mc == null || geom == null)
			return;
		RealRectangle rect = null;
		RealPolyline poly = null;
		if (geom instanceof RealRectangle) {
			rect = (RealRectangle) geom;
		} else if (geom instanceof RealPolyline) {
			poly = (RealPolyline) geom;
			if (poly.p == null || poly.p.length < 4)
				return;
		}
		if (rect == null && poly == null)
			return;
		Color c = Color.gray;
		if (selected) {
			c = DGeoObject.selectColor;
		} else if (drawParm.drawBorders) {
			c = drawParm.lineColor;
		}
		g.setXORMode(c);
		Rectangle prevClip = null;
		boolean multiMap = mc.getMapCount() > 1;
		if (multiMap) {
			prevClip = g.getClipBounds();
			if (prevClip == null) {
				prevClip = mc.getViewportBounds();
			}
		}
		g.setColor(DGeoObject.highlightColor);
		for (int i = 0; i < mc.getMapCount(); i++) {
			mc.setCurrentMapN(i);
			if (multiMap) {
				Rectangle mb = mc.getMapBounds(i);
				if (mb == null) {
					continue;
				}
				int left = Math.max(mb.x, prevClip.x), top = Math.max(mb.y, prevClip.y), right = Math.min(mb.x + mb.width, prevClip.x + prevClip.width), bottom = Math.min(mb.y + mb.height, prevClip.y + prevClip.height);
				if (right - left <= 0 || bottom - top <= 0) {
					continue;
				}
				g.setClip(left, top, right - left, bottom - top);
			}
			if (rect != null) {
				int x1 = mc.scrX(rect.rx1, rect.ry1), y2 = mc.scrY(rect.rx1, rect.ry1), x2 = mc.scrX(rect.rx2, rect.ry2), y1 = mc.scrY(rect.rx2, rect.ry2);
				int w = x2 - x1, h = y2 - y1, lw = drawParm.selWidth;
				if (drawParm.selWidth == 1) {
					g.drawRect(x1, y1, w, h);
				} else {
					Drawing.drawRectangle(g, drawParm.selWidth, x1, y1, w, h);
				}
			} else {
				if (polyX == null) {
					polyX = new int[5];
					polyY = new int[5];
				}
				for (int j = 0; j < 4; j++) {
					polyX[j] = mc.scrX(poly.p[j].x, poly.p[j].y);
					polyY[j] = mc.scrY(poly.p[j].x, poly.p[j].y);
				}
				polyX[4] = polyX[0];
				polyY[4] = polyY[0];
				if (drawParm.selWidth == 1) {
					g.drawPolygon(polyX, polyY, 5);
				} else {
					Drawing.drawPolyline(g, drawParm.selWidth, polyX, polyY, 5, false);
				}
			}
		}
		g.setPaintMode();
		if (multiMap && prevClip != null) {
			g.setClip(prevClip.x, prevClip.y, prevClip.width, prevClip.height);
		}
	}

	public void showSelection(Graphics g, MapContext mc, Geometry geom) {
		if (mc == null || geom == null)
			return;
		Rectangle prevClip = null;
		boolean multiMap = mc.getMapCount() > 1;
		if (multiMap) {
			prevClip = g.getClipBounds();
		}
		for (int i = 0; i < mc.getMapCount(); i++) {
			mc.setCurrentMapN(i);
			if (multiMap) {
				Rectangle mb = mc.getMapBounds(i);
				g.setClip(mb.x, mb.y, mb.width, mb.height);
			}
			g.setColor(DGeoObject.selectColor);
			if (geom instanceof RealRectangle) {
				RealRectangle rr = (RealRectangle) geom;
				int x1 = mc.scrX(rr.rx1, rr.ry1), y2 = mc.scrY(rr.rx1, rr.ry1), x2 = mc.scrX(rr.rx2, rr.ry2), y1 = mc.scrY(rr.rx2, rr.ry2), w = x2 - x1, h = y2 - y1;
				if (drawParm.selWidth == 1) {
					g.drawRect(x1, y1, w, h);
				} else {
					Drawing.drawRectangle(g, drawParm.selWidth, x1, y1, w, h);
				}
				int gap = 5;
				if (w - drawParm.selWidth > gap && h - drawParm.selWidth > gap) {
					int nSteps = Math.min(w / gap, h / gap);
					int xx1 = x2, yy1 = y1, xx2 = x1, yy2 = y2;
					for (int j = 0; j < nSteps && xx1 > x1 && yy2 > y1; j++) {
						g.drawLine(xx1, yy1, xx2, yy2);
						xx1 -= gap;
						yy2 -= gap;
					}
					xx1 = x2;
					yy1 = y1 + gap;
					xx2 = x1 + gap;
					yy2 = y2;
					for (int j = 1; j < nSteps && xx2 < x2 && yy1 < y2; j++) {
						g.drawLine(xx1, yy1, xx2, yy2);
						xx2 += gap;
						yy1 += gap;
					}
				}
			} else if (geom instanceof RealPolyline) {
				RealPolyline poly = (RealPolyline) geom;
				if (polyX == null) {
					polyX = new int[5];
					polyY = new int[5];
				}
				for (int j = 0; j < 4; j++) {
					polyX[j] = mc.scrX(poly.p[j].x, poly.p[j].y);
					polyY[j] = mc.scrY(poly.p[j].x, poly.p[j].y);
				}
				polyX[4] = polyX[0];
				polyY[4] = polyY[0];
				if (drawParm.selWidth == 1) {
					g.drawPolygon(polyX, polyY, 5);
				} else {
					Drawing.drawPolyline(g, drawParm.selWidth, polyX, polyY, 5, false);
				}
				/*
				g.setClip(new Polygon(polyX,polyY,5));
				int x1=Math.min(polyX[0],polyX[3]), x2=Math.max(polyX[1],polyX[2]),
				    y1=Math.min(polyY[0],polyY[1]), y2=Math.max(polyY[2],polyY[3]),
				    w=x2-x1, h=y2-y1;
				int gap=5;
				if (w-drawParm.selWidth>gap && h-drawParm.selWidth>gap) {
				  if (w<h) w=h; else h=w;
				  x2=x1+w; y2=y1+h;
				}
				int nSteps=Math.min(w/gap,h/gap);
				int xx1=x2, yy1=y1, xx2=x1, yy2=y2;
				for (int j=0; j<nSteps && xx1>x1 && yy2>y1; j++) {
				  g.drawLine(xx1,yy1,xx2,yy2);
				  xx1-=gap; yy2-=gap;
				}
				xx1=x2; yy1=y1+gap; xx2=x1+gap; yy2=y2;
				for (int j=1; j<nSteps && xx2<x2 && yy1<y2; j++) {
				  g.drawLine(xx1,yy1,xx2,yy2);
				  xx2+=gap; yy1+=gap;
				}
				g.setClip(null);
				*/
				int gap = 4, gap2 = gap * 2;
				while (polyX[0] < polyX[1] - gap2 && polyX[3] < polyX[2] - gap2 && polyY[0] < polyY[3] - gap2 && polyY[1] < polyY[2] - gap2) {
					polyX[0] += gap;
					polyX[1] -= gap;
					polyX[2] -= gap;
					polyX[3] += gap;
					polyX[4] = polyX[0];
					polyY[0] += gap;
					polyY[1] += gap;
					polyY[2] -= gap;
					polyY[3] -= gap;
					polyY[4] = polyY[0];
					g.drawPolygon(polyX, polyY, 5);
				}
			}
		}
		if (multiMap && prevClip != null) {
			g.setClip(prevClip.x, prevClip.y, prevClip.width, prevClip.height);
		}
	}
}