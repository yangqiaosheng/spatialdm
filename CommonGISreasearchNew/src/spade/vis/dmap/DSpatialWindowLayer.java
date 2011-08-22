package spade.vis.dmap;

import java.awt.Graphics;

import spade.vis.geometry.Geometry;
import spade.vis.geometry.RealRectangle;
import spade.vis.map.MapContext;
import spade.vis.space.GeoLayer;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Apr 23, 2008
 * Time: 2:56:34 PM
 * A "layer" containing the window of the spatial filter
 */
public class DSpatialWindowLayer extends DGeoLayer {
	/**
	 * Defines a rectangular area for filtering spatial objects
	 */
	protected SpatialWindow spWin = null;

	/**
	 * returns spatial extent of the filter
	 */
	public RealRectangle getSpatialWindowExtent() {
		return (spWin == null) ? null : new RealRectangle(spWin.rx1, spWin.ry1, spWin.rx2, spWin.ry2);
	}

	public void setSpatialWindow(SpatialWindow spWin) {
		this.spWin = spWin;
		if (spWin != null) {
			if (drawParm == null) {
				drawParm = new DrawingParameters();
			}
			drawParm.lineColor = spWin.lineColor;
			drawParm.lineWidth = spWin.lineWidth;
			drawParm.fillContours = false;
		}
	}

	@Override
	public void setLayerDrawn(boolean value) {
		if (spWin != null) {
			spWin.setVisible(value);
		}
		super.setLayerDrawn(value);
	}

	@Override
	public void setIsActive(boolean value) {
		if (value == isActive)
			return;
		isActive = value;
		if (spWin != null) {
			spWin.setActive(isActive);
		}
	}

	/**
	* Returns a copy of this layer.
	*/
	@Override
	public GeoLayer makeCopy() {
		DSpatialWindowLayer layer = new DSpatialWindowLayer();
		copyTo(layer);
		layer.setType(Geometry.area);
		layer.setSpatialWindow(spWin);
		return layer;
	}

	/**
	 * Destroys the spatial window
	 */
	@Override
	public void destroy() {
		if (spWin != null) {
			spWin.destroy();
		}
		super.destroy();
	}

	/**
	* Returns the spatial type: area
	*/
	@Override
	public char getType() {
		objType = Geometry.area;
		return objType;
	}

	/**
	* Does nothing
	*/
	@Override
	public void setType(char type) {
	}

	/**
	 * Returns the subtype of the objects: rectangle
	 */
	@Override
	public char getSubtype() {
		objSubType = Geometry.rectangle;
		return objSubType;
	}

	/**
	* Reports whether there are any objects in this container.
	*/
	@Override
	public boolean hasData() {
		return spWin != null;
	}

	/**
	* Returns the rectangle that contains all GeoObjects of this GeoLayer.
	*/
	@Override
	public RealRectangle getWholeLayerBounds() {
		if (spWin == null || Float.isNaN(spWin.rx1))
			return null;
		return new RealRectangle(spWin.rx1, spWin.ry1, spWin.rx2, spWin.ry2);
	}

	/**
	* Returns the rectangle that contains currently loaded GeoObjects of this GeoLayer.
	* The GeoLayer may return null if there are no objects in the memory.
	*/
	@Override
	public RealRectangle getCurrentLayerBounds() {
		return getWholeLayerBounds();
	}

	/**
	* Returns the rectangle that contains currently loaded and not filtered GeoObjects of this GeoLayer.
	* The GeoLayer may return null if there are no objects in the memory.
	*/
	@Override
	public RealRectangle getActiveLayerBounds() {
		return getWholeLayerBounds();
	}

	@Override
	public void setDrawingParameters(DrawingParameters dp) {
		if (drawParm == null) {
			drawParm = dp;
		} else {
			dp.copyTo(drawParm);
		}
		drawParm.fillContours = false;
		drawParm.drawBorders = true;
		if (drawParm.lineWidth < 1) {
			drawParm.lineWidth = 1;
		}
		if (spWin != null) {
			spWin.lineColor = drawParm.lineColor;
			spWin.lineWidth = drawParm.lineWidth;
		}
		notifyPropertyChange("DrawingParameters", null, drawParm);
	}

	@Override
	public void draw(Graphics g, MapContext mc) {
		/*
		if (g==null || mc==null) return;
		if (spWin!=null) {
		  if (drawParm!=null) {
		    spWin.lineColor=drawParm.lineColor;
		    spWin.lineWidth=drawParm.lineWidth;
		    spWin.drawWindow();
		  }
		}
		*/
	}

	/*
	* Function drawStrictly() is like normal draw().
	* It is needed if we try to avoid drawing of geo-objects with area geometry
	* that have their center out of visible territory.
	*/
	@Override
	public void drawStrictly(Graphics g, MapContext mc) {
		draw(g, mc);
	}
}
