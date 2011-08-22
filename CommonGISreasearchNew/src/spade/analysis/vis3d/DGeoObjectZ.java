package spade.analysis.vis3d;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;

import spade.lib.util.Comparable;
import spade.vis.dmap.DGeoObject;
import spade.vis.geometry.Diagram;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.RealPoint;
import spade.vis.geometry.RealRectangle;
import spade.vis.map.MapContext;
import spade.vis.space.GeoObject;

/**
* A DGeoObjectZ has a Z-coordinate and therefore can draw itself in a
* perspective view using MapMetrics3D
*/
public class DGeoObjectZ extends DGeoObject implements Comparable {
	/**
	* The Z-position of the object
	*/
	protected double z = Float.NaN;
	protected double zattr = Float.NaN;
	/**
	* The order of this object in the layer taking into account distance
	* to view point from it (important for correct drawing of the layer)
	*/
	protected int order = -1; // -1 not in use
	protected float distanceToViewPoint = Float.NaN; // Float.NaN order not in use

	protected boolean drawAsPoint = false;
	protected RealPoint rpCenter = null;
	//protected RealRectangle bounds=null;
	protected Rectangle bounds3D = null;
	protected Geometry geomOrig = null;

	/**
	* Constructs itself from a "normal" DGeoObject
	*/
	public DGeoObjectZ(DGeoObject obj) {
		if (obj == null)
			return;
		setup(obj.getSpatialData());
		label = obj.getLabel();
		geomOrig = obj.getGeometry();
		RealRectangle rrLabel = obj.getLabelRectangle();
		rpCenter = new RealPoint();
		rpCenter.x = (rrLabel.rx1 + rrLabel.rx2) / 2;
		rpCenter.y = (rrLabel.ry1 + rrLabel.ry2) / 2;
		//bounds=obj.getBounds();
	}

	@Override
	public RealRectangle getBounds() {
		Geometry geom = this.getGeometry();
		if (geom == null)
			return null;
		float bounds[] = geom.getBoundRect();
		if (bounds == null)
			return null;
		return new RealRectangle(bounds);
	}

	public Rectangle getBounds3D() {
		return bounds3D;
	}

	public void setBounds3D(Rectangle b) {
		bounds3D = b;
	}

	public boolean hasValueZ() {
		return !Double.isNaN(z) && !Double.isNaN(zattr);
	}

	public boolean canBeDrawn(double lowZ, double hiZ) {
		return !Double.isNaN(z) && !Double.isNaN(zattr) && isBetween(lowZ, hiZ);
	}

	public boolean isBetween(double lowZ, double hiZ) {
		return (z >= lowZ && z <= hiZ);
	}

	public boolean isPointInside(int x, int y) {
		if (bounds3D == null) {
			System.out.println("bounds3D=null");
			return false;
		}
		return Geometry.isThePoint(bounds3D.x, bounds3D.y, x, y, 3.0f);
	}

	public boolean isInRectangle(int x1, int y1, int x2, int y2) {
		if (bounds3D == null) {
			System.out.println("bounds3D=null");
			return false;
		}
		int x0 = x1 < x2 ? x1 : x2, y0 = y1 < y2 ? y1 : y2;
		Rectangle r = new Rectangle(x0, y0, Math.abs(x2 - x1), Math.abs(y2 - y1));
		return r.contains(bounds3D.x, bounds3D.y);
	}

	/**
	* Sets the Z-position of this object
	*/
	public void setZPosition(double zpos) {
		z = zpos;
	}

	public void setZAttrValue(double zav) {
		zattr = zav;
	}

	/**
	* Returns the Geometry of this GGeoObjectZ:
	* if user prefers points instead of polygons the center of label rectangle
	* of the polygon is being calculated and the function returns corresponding
	* point geometry, else geometry of original parent layer is returned
	*/
	@Override
	public Geometry getGeometry() {
		return drawAsPoint ? rpCenter : geomOrig;
	}

	public void setDrawAsPoint(boolean flag) {
		drawAsPoint = flag;
		if (!flag)
			return;
		if (rpCenter == null) {
			rpCenter = new RealPoint();
		}
		RealRectangle rr = getLabelRectangle();
		if (rr == null)
			return;
		rpCenter.x = (rr.rx1 + rr.rx2) / 2;
		rpCenter.y = (rr.ry1 + rr.ry2) / 2;
	}

	/**
	*  Override the base class functions to be able to handle geometry switching
	*/
	@Override
	public void drawInXORMode(Graphics g, MapContext mc, Color color, int width) {
		if (g == null || data == null || this.getGeometry() == null)
			return;
		Color c = Color.black;
		boolean XORwithSelectionColor = selected && !color.equals(selectColor);
		if (XORwithSelectionColor) {
			c = selectColor;
		} else if (drawParam != null && drawParam.drawBorders) {
			c = drawParam.lineColor;
		} else if (lastColor != null) {
			c = lastColor;
		} else if (drawParam != null) {
			c = drawParam.fillColor;
		}
		g.setXORMode(c);
		drawGeometry(this.getGeometry(), g, mc, color, null, width, null);
		g.setPaintMode();
	}

	@Override
	public void showSelection(Graphics g, MapContext mc) {
		drawGeometry(this.getGeometry(), g, mc, selectColor, null, (drawParam == null) ? 1 : drawParam.selWidth, null);
	}

/*
  public void hideSelection(Graphics g, MapContext mc) {}
*/
	/**
	* Returns the Z-position of this object
	*/
	public double getZPosition() {
		return z;
	}

	public double getZAttrValue() {
		return zattr;
	}

	public RealPoint getCenter() {
		return rpCenter;
	}

	/**
	* Transforms real coordinates into screen X-coordinate using the given map
	* context
	*/
	@Override
	protected int getScreenX(MapContext mc, float rx, float ry) {
		if (mc instanceof MapMetrics3D) {
			Point p = ((MapMetrics3D) mc).get3DTransform(rx, ry, z);
			return p.x;
		}
		return mc.scrX(rx, ry);
	}

	/**
	* Transforms real coordinates into screen Y-coordinate using the given map
	* context
	*/
	@Override
	protected int getScreenY(MapContext mc, float rx, float ry) {
		if (mc instanceof MapMetrics3D) {
			Point p = ((MapMetrics3D) mc).get3DTransform(rx, ry, z);
			return p.y;
		}
		return mc.scrY(rx, ry);
	}

	/**
	* The function returns relative position of this object among other
	* the others in layer according to the distance from viewpoint.
	* Returns -1 if unordered.
	*/
	public int getOrder() {
		return order;
	}

	public float getDistanceToViewpoint() {
		return distanceToViewPoint;
	}

	/**
	* The function sets relative position of this object among other
	* the others in layer according to the distance from viewpoint.
	* Sets -1 if heavy case.
	*/
	public void setDistanceToViewPoint(float d) {
		distanceToViewPoint = d;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	// Implementation of Comparable interface
	/**
	*  The function compares 2 objects on the base of the distance to viewpoint
	*/
	public int compare(Comparable obj1, Comparable obj2) {
		if (!((obj1 instanceof DGeoObjectZ) || (obj1 instanceof DGeoObjectZ)))
			return Integer.MIN_VALUE; // cannot compare!
		return compare((DGeoObjectZ) obj1, (DGeoObjectZ) obj2);
	}

	public int compare(DGeoObjectZ obj1, DGeoObjectZ obj2) {
		if (obj1 == null || obj2 == null)
			return Integer.MIN_VALUE; // cannot compare
		float d1 = obj1.getDistanceToViewpoint(), d2 = obj2.getDistanceToViewpoint();
		if (Float.isNaN(d1) && Float.isNaN(d2))
			return 0;
		else if (Float.isNaN(d1))
			return -1;
		else if (Float.isNaN(d2))
			return 1;

		return (d1 < d2 ? -1 : (d1 == d2 ? 0 : 1));
	}

	@Override
	public int compareTo(Comparable obj) {
		if (!(obj instanceof DGeoObjectZ))
			return Integer.MIN_VALUE; // cannot compare!
		return compare(this, (DGeoObjectZ) obj);
	}

	@Override
	public GeoObject makeCopy() {
		DGeoObjectZ zobj = new DGeoObjectZ((DGeoObject) super.makeCopy());
		if (zobj == null)
			return null;
		zobj.setZPosition(z);
		zobj.setZAttrValue(zattr);
		zobj.setDistanceToViewPoint(distanceToViewPoint);
		zobj.setOrder(order);
		zobj.setDrawAsPoint(drawAsPoint);
		zobj.setBounds3D(new Rectangle(bounds3D));
		return zobj;
	}

	/**
	* If presentation of an object is an icon (image) or a diagram, this function
	* determines its position and draws the icon or diagram.
	*/
	@Override
	protected void drawIconOrDiagram(Object pres, Graphics g, MapContext mc) {
		labelPos = null;
		if (pres == null)
			return;
		if (!(pres instanceof Diagram) && !(pres instanceof Image)) {
			super.drawIconOrDiagram(pres, g, mc);
			return;
		}
		Point p = ((MapMetrics3D) mc).get3DTransform(rpCenter.x, rpCenter.y, z);
		if (pres instanceof Diagram) {
			//this may be a triangle indicating that the value is out of current
			//focus limits
			Diagram dia = (Diagram) pres;
			dia.draw(g, p.x, p.y);
			labelPos = dia.getLabelPosition();
		} else if (pres instanceof Image) {
			Image img = (Image) pres;
			p.x -= img.getWidth(null) / 2;
			p.y -= img.getHeight(null) / 2;
			g.drawImage(img, p.x, p.y, null);
			labelPos = new Point(p.x, p.y + img.getHeight(null) + 2);
		}
	}
}
