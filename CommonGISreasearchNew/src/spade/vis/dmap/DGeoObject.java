package spade.vis.dmap;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.image.ImageObserver;
import java.util.StringTokenizer;
import java.util.Vector;
import java2d.Drawing2D;

import spade.lib.basicwin.Drawing;
import spade.lib.basicwin.Icons;
import spade.lib.util.BubbleSort;
import spade.lib.util.IntArray;
import spade.time.TimeMoment;
import spade.time.TimeReference;
import spade.vis.database.DataItem;
import spade.vis.database.SpatialDataItem;
import spade.vis.database.SpatialEntity;
import spade.vis.database.ThematicDataItem;
import spade.vis.geometry.Diagram;
import spade.vis.geometry.GeomSign;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.ImageGeometry;
import spade.vis.geometry.LocatedImage;
import spade.vis.geometry.MultiGeometry;
import spade.vis.geometry.RealCircle;
import spade.vis.geometry.RealLine;
import spade.vis.geometry.RealPoint;
import spade.vis.geometry.RealPolygon;
import spade.vis.geometry.RealPolyline;
import spade.vis.geometry.RealRectangle;
import spade.vis.geometry.Sign;
import spade.vis.geometry.StructSign;
import spade.vis.map.MapContext;
import spade.vis.mapvis.LineDrawSpec;
import spade.vis.mapvis.Visualizer;
import spade.vis.space.GeoObject;

/**
* DGeoObject stands for "Descartes Geo Object", i.e. our own implementation
* of the interface GeoObject. The interface GeoObject itself is used
* to connect thematic visualization and map manipulation components of
* Descartes to "foreign" mapping software with its own map drawing
* facilities, like in Lava.
* Geo Object is something that has spatial location and can be drawn in
* a map. A collection of homogeneous Geo Objects (e.g. countries or rivers)
* constitute a Geo Layer. A Geo Object always has its Geometry, e.g.
* RealPoint (for a point object) or RealPolyline (for a linear or areal
* object). A Geo Object may also have thematic data associated with it.
*/

public class DGeoObject implements GeoObject, java.io.Serializable, spade.lib.util.Comparable {
	/**
	* By default, the signs representing point objects have this width (1 mm)
	*/
	public static int signWidth = 0, halfSignWidth = 0, crossWidth = 0, arrowHeight = 0, arrowWidth = 0;
	public static Color highlightColor = Color.white, selectColor = Color.black;
	protected float mm = Toolkit.getDefaultToolkit().getScreenResolution() / 25.33f;
	/**
	* Used internally for transformmation of real coordinates of polyline
	* vertices into screen coordinates
	*/
	private static IntArray xcoord = new IntArray(50, 50), ycoord = new IntArray(50, 50);
	/**
	* The unique identifier of this Geo Object.
	*/
	protected String id = null;
	/**
	* A label is any text drawn at the location of the object in a map. Most
	* often this is the name of the object (that needs not to be the same as
	* the identifier).
	*/
	public String label = null;
	/**
	* "data" contains data necessary to construct the DGeoObject (geometry etc.)
	* as well as thematic data associated with this object.
	*/
	protected SpatialDataItem data = null;
	/**
	* Type of the object: point, line, area, image, or raster (grid). May be undefined.
	*/
	protected char type = Geometry.undefined;
	/**
	 * Indicates whether the geometry of this object has geographic coordinates
	 * (latitudes and longitudes). By default false.
	 */
	protected boolean isGeo = false;
	/**
	* Parameters for drawing (colors etc.) Usually these are settings common for
	* all DGeoObjects in a DGeoLayer. These parameters may be ignored when
	* the DGeoObject represents thematic data. In the latter case a Visualizer
	* determines how the DGeoObject should draw itself.
	*/
	public DrawingParameters drawParam = null;
	/**
	* A Visualiser says the DGeoObject how it should draw itself, depending
	* on the thematic data associated with it.
	*/
	protected Visualizer vis = null;
	/**
	* For area layers, two visualizers are allowed: one defining the area filling
	* colors and the other producing diagrams or other symbols drawn on top of
	* the painting. This variable contains a reference to the visualizer
	* which will define the color for painting the object.
	*/
	protected Visualizer bkgVis = null;
	/**
	* Indicates whether the object is at the moment highlighted (transient
	* highlighting that switches on/off on mouse movement)
	*/
	protected boolean highlighted = false;
	/**
	* If the type of the object is image (raster), an ImageObserver may observe
	* the state of loading and drawing of the image.
	*/
	protected ImageObserver imgObserver = null;
	/**
	* Indicates whether the object is at the moment selected (durable
	* highlighting that switches on/off on mouse click)
	*/
	protected boolean selected = false;
	/**
	* The color in which the interior of the object was last painted (for area
	* objects). May be null.
	*/
	protected Color lastColor = null;
	/**
	 * The identifiers of the neighbours of this object, i.e. there are common borders segments
	 * if the objects are areas
	 */
	public Vector<String> neighbours = null;

	/**
	* Setup acts as a constructor: a DGeoObject is always created from a
	* SpatialDataItem. This Data Item may be also a ThematicDataItem.
	* The DGeoObject stores a reference to the corresponding DataItem.
	*/
	public void setup(SpatialDataItem spdit) {
		data = spdit;
		if (data != null) {
			id = data.getId();
			//do some initialization...
		}
	}

	/**
	* The method update is called when some data associated with this object
	* have changed. The argument of update need not to contain any spatial
	* information, for example, when only thematic data have changed but the
	* spatial properties of the object remain the same.
	*/
	public void update(DataItem dit) {
		if (dit != null)
			if (dit instanceof SpatialDataItem)
				if (data == null) {
					data = (SpatialDataItem) dit;
				} else {
					dit.copyTo(data);
				}
			else if (dit instanceof ThematicDataItem) {
				setThematicData((ThematicDataItem) dit);
			}
	}

	public void setThematicData(ThematicDataItem dit) {
		data.setThematicData(dit);
	}

	/**
	* Returns a copy of this object. The spatial and thematic data items
	* used by this objects are not duplicated; the copy will use references to the
	* same data items
	*/
	@Override
	public GeoObject makeCopy() {
		DGeoObject obj = new DGeoObject();
		if (data != null) {
			obj.setup((SpatialDataItem) data.clone());
		}
		obj.label = label;
		obj.highlighted = highlighted;
		obj.selected = selected;
		return obj;
	}

	/**
	* A function of the basic GeoObject interface.
	* Returns the identifier got from its Data Item. The identifier of a GeoObject
	* should be unique. Two objects with the same identifier are assumed to
	* be the same object. Thus, if GeoObjects representing islands belonging to
	* Italy have the same identifier as the GeoObject representing Italy, all
	* the objects will be treated as parts of a single object.
	*/
	@Override
	public String getIdentifier() {
		return id;
	}

	@Override
	public void setIdentifier(String id) {
		if (data != null) {
			data.setId(id);
		}
		this.id = id;
	}

	@Override
	public String getName() {
		if (label != null)
			return label;
		if (data != null) {
			if (data.getName() != null)
				return data.getName();
			if (data.getThematicData() != null && data.getThematicData().getName() != null)
				return data.getThematicData().getName();
		}
		return null;
	}

	/**
	* Returns its spatial data
	*/
	public SpatialDataItem getSpatialData() {
		return data;
	}

	/**
	* The function of the basic GeoObject interface. Returns the ThematicDataItem
	* associated with its SpatialDataItem.
	*/
	@Override
	public ThematicDataItem getData() {
		if (data == null)
			return null;
		return data.getThematicData();
	}

	/**
	* The function of the basic GeoObject interface.
	* Returns true if some thematic data are associated with this geographical entity.
	*/
	@Override
	public boolean hasData() {
		return data != null && data.getThematicData() != null;
	}

	/**
	 * The function of the basic GeoObject interface.
	 * Returns a version of this GeoObject corresponding to the specified time
	 * interval. If the object does not change over time, returns itself. May
	 * return null if the object does not exist during the specified interval.
	 */
	@Override
	public GeoObject getObjectVersionForTimeInterval(TimeMoment t1, TimeMoment t2) {
		if (t1 == null && t2 == null)
			return this;
		TimeReference tref = getTimeReference();
		if (tref == null)
			return this;
		if (tref.isValid(t1, t2))
			return this;
		return null;
	}

	/**
	 * Same as getObjectVersionForTimeInterval with a single difference:
	 * when only a part of the object fits into the interval, produces a new
	 * instance of GeoObject with this part.
	 * A basic DGeoObject is indivisible; therefore; this method just calls
	 * getObjectVersionForTimeInterval(t1,t2).
	 */
	@Override
	public GeoObject getObjectCopyForTimeInterval(TimeMoment t1, TimeMoment t2) {
		return getObjectVersionForTimeInterval(t1, t2);
	}

	/**
	 * Returns the geo position of this object around the given time moment.
	 * If the object does not change the position over time, returns the
	 * centre (centroid) of the object.
	 */
	public RealPoint getGeoPositionAroundTime(TimeMoment t) {
		if (t == null || data == null)
			return null;
		Geometry geom = data.getGeometry();
		if (geom == null)
			return null;
		TimeReference tr = data.getTimeReference();
		if (tr == null)
			return SpatialEntity.getCentre(geom);
		if (geom instanceof MultiGeometry) {
			MultiGeometry mg = (MultiGeometry) geom;
			Geometry lastValid = null;
			long minDist = Long.MAX_VALUE;
			for (int i = 0; i < mg.getPartsCount() && minDist > 0; i++) {
				Geometry gp = mg.getPart(i);
				tr = gp.getTimeReference();
				if (tr == null) {
					continue;
				}
				TimeMoment t1 = tr.getValidFrom(), t2 = tr.getValidUntil();
				long d1 = t1.subtract(t);
				if (t2 == null || t2.equals(t1)) {
					d1 = Math.abs(d1);
					if (d1 < minDist) {
						minDist = d1;
						lastValid = gp;
					}
				} else {
					long d2 = t2.subtract(t);
					if (d1 <= 0 && d2 >= 0) {
						minDist = 0;
						lastValid = gp;
					} else {
						d2 = Math.min(Math.abs(d1), Math.abs(d2));
						if (d2 < minDist) {
							minDist = d2;
							lastValid = gp;
						}
					}
				}
			}
			if (lastValid != null)
				return SpatialEntity.getCentre(lastValid);
		} else if (geom instanceof RealPolyline) {
			RealPolyline rp = (RealPolyline) geom;
			if (rp.p != null) {
				RealPoint lastValid = null;
				long minDist = Long.MAX_VALUE;
				for (int i = 0; i < rp.p.length && minDist > 0; i++) {
					tr = rp.p[i].getTimeReference();
					if (tr == null) {
						continue;
					}
					TimeMoment t1 = tr.getValidFrom(), t2 = tr.getValidUntil();
					long d1 = t1.subtract(t);
					if (t2 == null || t2.equals(t1)) {
						d1 = Math.abs(d1);
						if (d1 < minDist) {
							minDist = d1;
							lastValid = rp.p[i];
						}
					} else {
						long d2 = t2.subtract(t);
						if (d1 <= 0 && d2 >= 0) {
							minDist = 0;
							lastValid = rp.p[i];
						} else {
							d2 = Math.min(Math.abs(d1), Math.abs(d2));
							if (d2 < minDist) {
								minDist = d2;
								lastValid = rp.p[i];
							}
						}
					}
				}
				if (lastValid != null)
					return lastValid;
			}
		}
		return SpatialEntity.getCentre(geom);
	}

	/**
	 * Informs whether this object has a time reference
	 */
	public boolean isTimeReferenced() {
		return data != null && data.getTimeReference() != null;
	}

	/**
	 * Returns the time reference of this object, if any
	 */
	public TimeReference getTimeReference() {
		if (data == null)
			return null;
		return data.getTimeReference();
	}

	/**
	 * Reports if this GeoObject contains data about changes of some entity
	 * over time, e.g. about movement (change of position), change of shape,
	 * size, etc. By default, returns false.
	 */
	public boolean includesChanges() {
		return false;
	}

	/**
	* The spatial type may be point, area, line, or raster (see the constants
	* defined in the interface SpatialDataItem). To determine its spatial type,
	* the DGeoObject uses the function getSpatialType() of the SpatialDataItem
	* passed to the constructor.
	*/
	public char getSpatialType() {
		if (type != Geometry.undefined)
			return type;
		if (data == null)
			return Geometry.undefined;
		type = data.getSpatialType();
		return type;
	}

	/**
	* Sets the spatial type of the object (all objects in a GeoLayer have the same
	* type).
	*/
	public void setSpatialType(char objType) {
		type = objType;
	}

	/**
	* Checks if (at least part of) the object is visible in the given rectangle.
	*/
	public boolean fitsInRectangle(float x1, float y1, float x2, float y2) {
		Geometry geom = getGeometry();
		if (geom == null)
			return false;
		return geom.fitsInRectangle(x1, y1, x2, y2);
	}

	/**
	* Checks if the object or a considerable part of it is contained in the given
	* rectangle (used for selection).
	*/
	public boolean isInRectangle(float x1, float y1, float x2, float y2) {
		Geometry geom = getGeometry();
		if (geom == null)
			return false;
		return geom.isInRectangle(x1, y1, x2, y2);
	}

	/**
	* Checks if the point (x,y) belongs to the object. The point, in particular,
	* may represent the position of the mouse in the map.
	*/
	public boolean contains(float x, float y, float tolerateDist) {
		Geometry geom = getGeometry();
		if (geom == null)
			return false;
		return geom.contains(x, y, tolerateDist, type != Geometry.line);
	}

	/**
	* Sets the label of the object (the text to be put on the map) to the
	* specified string.
	*/
	public void setLabel(String txt) {
		label = txt;
		if (txt != null && data != null && (data instanceof SpatialEntity) && (!data.hasName() || data.getName().equals(data.getId()))) {
			((SpatialEntity) data).setName(txt);
		}
	}

	/**
	* Returns the label of the object (the text to be put on the map).QY to the
	*/
	public String getLabel() {
		return label;
	}

	/**
	* Sets parameters for drawing. These parameters may be ignored when
	* the DGeoObject represents thematic data. In the latter case a Visualizer
	* determines how the DGeoObject should draw itself.
	*/
	public void setDrawingParameters(DrawingParameters dp) {
		drawParam = dp;
	}

	public DrawingParameters getDrawingParameters() {
		return drawParam;
	}

	/**
	* Returns the Geometry of this object
	*/
	@Override
	public Geometry getGeometry() {
		if (data == null)
			return null;
		return data.getGeometry();
	}

	/**
	 * Informs whether the geometry of this object has geographic coordinates
	 * (latitudes and longitudes). By default false.
	 */
	@Override
	public boolean isGeographic() {
		return isGeo;
	}

	/**
	 * Sets the property of the object's geometry indicating whether
	 * the geometry has geographic coordinates (latitudes and longitudes).
	 */
	@Override
	public void setGeographic(boolean geographic) {
		isGeo = geographic;
		Geometry geom = getGeometry();
		if (geom != null) {
			geom.setGeographic(geographic);
		}
	}

	/**
	* Returns the bounding rectangle of this DGeoObject
	*/
	public RealRectangle getBounds() {
		Geometry geom = getGeometry();
		if (geom == null)
			return null;
		float bounds[] = geom.getBoundRect();
		if (bounds == null)
			return null;
		return new RealRectangle(bounds);
	}

	/**
	* Returns the rectangle in which the label will be drawn.
	* For an area object, this is the largest rectangle fitting in the
	* contour.
	* For a point or line object, width and height of the rectangle are 0.
	*/
	public RealRectangle getLabelRectangle() {
		return getLabelRectangle(getGeometry());
	}

	public static RealRectangle getLabelRectangle(Geometry geom) {
		if (geom == null)
			return null;
		if (geom instanceof RealPolyline)
			return ((RealPolyline) geom).getLabelRect();
		if (geom instanceof MultiGeometry)
			return ((MultiGeometry) geom).getLabelRect();
		//return the bounding rectangle
		float bounds[] = geom.getBoundRect();
		if (bounds == null)
			return null;
		return new RealRectangle(bounds);
	}

	/**
	* A Visualizer says the DGeoObject how it should draw itself, depending
	* on the thematic data associated with it. The method setVisualizer
	* is used by the DGeoLayer this object belongs to.
	*/
	public void setVisualizer(Visualizer visualizer) {
		vis = visualizer;
	}

	/**
	* For area layers, two visualizers are allowed: one defining the area filling
	* colors and the other producing diagrams or other symbols drawn on top of
	* the painting. This method is used for linking the object to the visualizer
	* which will define the color for painting.
	*/
	public void setBackgroundVisualizer(Visualizer visualizer) {
		bkgVis = visualizer;
	}

	/**
	* If the type of the object is image (raster), an ImageObserver may observe
	* the state of loading and drawing of the image. This method sets the
	* ImageObserver.
	*/
	public void setImageObserver(ImageObserver imgObs) {
		imgObserver = imgObs;
	}

	/**
	* Used for positioning labels below diagrams
	*/
	protected Point labelPos = null;
	/**
	 * The current appearance of the object (with which it was last drawn)
	 */
	protected ObjectAppearance currAppearance = null;

	/**
	 * Defines and returns the current appearance of this object (takes into account the
	 * visualizer, if available).
	 */
	public void defineCurrentAppearance(MapContext mc) {
		if (currAppearance == null) {
			currAppearance = new ObjectAppearance();
		}
		boolean useDefaultFilling = true;
		if (drawParam != null) {
			currAppearance.lineColor = (drawParam.drawBorders) ? drawParam.lineColor : null;
			currAppearance.fillColor = (drawParam.fillContours) ? drawParam.fillColor : null;
			currAppearance.lineWidth = drawParam.lineWidth;
			currAppearance.transparency = drawParam.transparency;
			useDefaultFilling = drawParam.useDefaultFilling;
		}
		boolean isArea = getSpatialType() == Geometry.area, isLine = getSpatialType() == Geometry.line;
		if (bkgVis != null || (vis != null && !vis.isDiagramPresentation())) {
			currAppearance.mustPaint = isArea || isLine;
			currAppearance.fillColor = null;
			currAppearance.presentation = (bkgVis != null) ? bkgVis.getPresentation(data, mc) : vis.getPresentation(data, mc);
			if (currAppearance.presentation != null)
				if (currAppearance.presentation instanceof Color)
					if (isLine) {
						currAppearance.lineColor = (Color) currAppearance.presentation;
						currAppearance.fillColor = currAppearance.lineColor;
					} else {
						currAppearance.fillColor = (Color) currAppearance.presentation;
					}
				else if (currAppearance.presentation instanceof LineDrawSpec) {
					LineDrawSpec lds = (LineDrawSpec) currAppearance.presentation;
					if (!lds.draw) {
						currAppearance.isVisible = false;
						return;
					}
					if (lds.color != null) {
						currAppearance.lineColor = lds.color;
					}
					currAppearance.fillColor = currAppearance.lineColor;
					currAppearance.lineWidth = lds.thickness;
					if (lds.transparent) {
						currAppearance.transparency = (currAppearance.transparency + 100) / 2;
					}
				}
			if (currAppearance.mustPaint && useDefaultFilling && currAppearance.fillColor == null) {
				currAppearance.transparency = (currAppearance.transparency + 100) / 2;
				currAppearance.fillColor = Color.lightGray;
				if (isLine) {
					currAppearance.lineColor = Color.lightGray;
				}
			}
		}
		if (currAppearance.mustPaint) {
			if (currAppearance.transparency > 0 && currAppearance.fillColor != null) {
				currAppearance.fillColor = Drawing2D.getTransparentColor(currAppearance.fillColor, currAppearance.transparency);
			}
			if (currAppearance.transparency > 0 && currAppearance.lineColor != null) {
				currAppearance.lineColor = Drawing2D.getTransparentColor(currAppearance.lineColor, currAppearance.transparency);
			}
		}
		return;
	}

	/**/
	/**
	 * Returns the current appearance of the object (with which it was last drawn)
	 */
	public ObjectAppearance getCurrentAppearance() {
		return currAppearance;
	}

	/**
	* The function draw is called when the DGeoObject must draw itself.
	* When there is no Visualiser available, the DGeoObject uses the
	* previously set DrawingParameters. When a Visualizer is available,
	* and it does not produce diagrams, the DGeoObject uses the
	* function getPresentation of the Visualiser to determine how
	* to draw itself.
	*/
	public void draw(Graphics g, MapContext mc) {
		labelPos = null;
		if (getGeometry() == null)
			return;
		defineCurrentAppearance(mc);
		if (!currAppearance.isVisible)
			return;
		if (currAppearance.mustPaint) {
			lastColor = ((currAppearance.fillColor != null) && (getSpatialType() == Geometry.area)) ? currAppearance.fillColor : currAppearance.lineColor;
			drawGeometry(g, mc, currAppearance.lineColor, currAppearance.fillColor, currAppearance.lineWidth, imgObserver);
		} else {
			lastColor = null;
		}

		drawIconOrDiagram(currAppearance.presentation, g, mc);
	}

	/**
	 * Draws the geometry of this object.
	 * If fillColor is null, the contour is not filled.
	 * If borderColor is null, the border is not drawn
	 */
	public void drawGeometry(Graphics g, MapContext mc, Color borderColor, Color fillColor, int width, ImageObserver observer) {
		drawGeometry(getGeometry(), g, mc, borderColor, fillColor, width, observer);
	}

	/**
	* Transforms real coordinates into screen X-coordinate using the given map
	* context
	*/
	protected int getScreenX(MapContext mc, float rx, float ry) {
		return mc.scrX(rx, ry);
	}

	/**
	* Transforms real coordinates into screen Y-coordinate using the given map
	* context
	*/
	protected int getScreenY(MapContext mc, float rx, float ry) {
		return mc.scrY(rx, ry);
	}

	/**
	* Draws only the contour of the object using the current color
	*/
	public void drawContour(Graphics g, MapContext mc) {
		drawContour(getGeometry(), g, mc);
	}

	public void drawContour(Geometry geom, Graphics g, MapContext mc) {
		if (geom == null)
			return;
		if (geom instanceof RealCircle) {
			RealCircle cir = (RealCircle) geom;
			float b[] = cir.getBoundRect();
			int x1 = getScreenX(mc, b[0], b[1]), y1 = getScreenY(mc, b[0], b[1]), x2 = getScreenX(mc, b[2], b[3]), y2 = getScreenY(mc, b[2], b[3]);
			g.drawOval((x1 + x2) / 2, (y1 + y2) / 2, x2 - x1, y1 - y2);
		} else if (geom instanceof RealRectangle) {
			RealRectangle rr = (RealRectangle) geom;
			int x1 = getScreenX(mc, rr.rx1, rr.ry1), y1 = getScreenY(mc, rr.rx1, rr.ry1), x2 = getScreenX(mc, rr.rx2, rr.ry2), y2 = getScreenY(mc, rr.rx2, rr.ry2);
			g.drawRect(x1, y1, x2 - x1, y2 - y1);
		} else if (geom instanceof RealPolyline) {
			RealPolyline rl = (RealPolyline) geom;
			if (rl.p != null) {
				if (xcoord == null || ycoord == null) {
					int cap = 100;
					if (rl.p.length > cap) {
						cap = rl.p.length;
					}
					xcoord = new IntArray(cap, 50);
					ycoord = new IntArray(cap, 50);
				}
				xcoord.removeAllElements();
				ycoord.removeAllElements();
				for (RealPoint element : rl.p) {
					xcoord.addElement(getScreenX(mc, element.x, element.y));
					ycoord.addElement(getScreenY(mc, element.x, element.y));
				}
				if (rl.getIsClosed()) {
					g.drawPolygon(xcoord.getArray(), ycoord.getArray(), xcoord.size());
				} else {
					g.drawPolyline(xcoord.getArray(), ycoord.getArray(), xcoord.size());
				}
			}
		} else if (geom instanceof MultiGeometry) {
			MultiGeometry mg = (MultiGeometry) geom;
			for (int i = 0; i < mg.getPartsCount(); i++) {
				drawContour(mg.getPart(i), g, mc);
			}
		}
	}

	/**
	* If presentation of an object is an icon (image) or a diagram, this function
	* determines its position and draws the icon or diagram.
	*/
	protected void drawIconOrDiagram(Object pres, Graphics g, MapContext mc) {
		labelPos = null;
		if (pres == null)
			return;
		if (pres instanceof LocatedImage) {
			LocatedImage limg = (LocatedImage) pres;
			g.drawImage(limg.img, limg.x, limg.y, null);
			return;
		}
		Geometry geom = getGeometry();
		if (geom == null)
			return;
		if (geom instanceof MultiGeometry) {
			int xx = 0, yy = 0, np = 0;
			MultiGeometry mg = (MultiGeometry) geom;
			boolean partsArePoints = true;
			for (int i = 0; i < mg.getPartsCount() && partsArePoints; i++) {
				Geometry gp = mg.getPart(i);
				partsArePoints = gp instanceof RealPoint;
				if (!partsArePoints) {
					break;
				}
				Rectangle dpr = getPositionForDiagram(gp, mc);
				if (dpr == null) {
					continue;
				}
				Point pt = drawIconOrDiagramInPosition(pres, g, dpr.x, dpr.y, dpr.width, dpr.height);
				if (pt == null) {
					continue;
				}
				xx += pt.x;
				yy += pt.y;
				++np;
			}
			if (!partsArePoints) {
				Rectangle dpr = getPositionForDiagram(mg, mc);
				labelPos = drawIconOrDiagramInPosition(pres, g, dpr.x, dpr.y, dpr.width, dpr.height);
			} else if (np > 0) {
				labelPos = new Point(xx / np, yy / np);
			}
		} else {
			Rectangle dpr = getPositionForDiagram(geom, mc);
			if (dpr == null)
				return;
			labelPos = drawIconOrDiagramInPosition(pres, g, dpr.x, dpr.y, dpr.width, dpr.height);
		}
	}

	protected Rectangle getPositionForDiagram(Geometry geom, MapContext mc) {
		if (geom == null)
			return null;
		float c[] = geom.getCentroid();
		int x, y, w = 0, h = 0;
		if (c != null) {
			x = mc.scrX(c[0], c[1]);
			y = mc.scrY(c[0], c[1]);
		} else {
			RealRectangle rr = getLabelRectangle(getGeometry());
			if (rr == null)
				return null;
			x = getScreenX(mc, rr.rx1, rr.ry1);
			y = getScreenY(mc, rr.rx2, rr.ry2);
			if (rr.rx1 < rr.rx2 || rr.ry1 < rr.ry2) {//not a point
				w = getScreenX(mc, rr.rx2, rr.ry2) - x;
				h = getScreenY(mc, rr.rx1, rr.ry1) - y;
			}
		}
		return new Rectangle(x, y, w, h);
	}

	protected Point drawIconOrDiagramInPosition(Object pres, Graphics g, int x, int y, int w, int h) {
		if (pres instanceof Diagram) {
			Diagram dia = (Diagram) pres;
			if (pres instanceof Sign) {
				((Sign) pres).setTransparency(drawParam.transparency);
			}

			if (w == 0 && h == 0) {
				if (type == Geometry.point) {
					dia.draw(g, x, y);
				} else {
					w = dia.getWidth();
					h = dia.getHeight();
					dia.draw(g, x - w / 2, y - h / 2, w, h);
				}
			} else {
				dia.draw(g, x, y, w, h);
			}
			return dia.getLabelPosition();
		} else if (pres instanceof Image) {
			if (w > 0) {
				x += w / 2;
			}
			if (h > 0) {
				y += h / 2;
			}
			Image img = (Image) pres;
			x -= img.getWidth(null) / 2;
			y -= img.getHeight(null) / 2;
			g.drawImage(img, x, y, null);
			return new Point(x, y + img.getHeight(null) + 2);
		}
		return null;
	}

	/**
	 * Used for drawing rectangles as polygons
	 */
	private static int xe[] = new int[5];
	private static int ye[] = new int[5];

	/**
	* Draws a geometry depending on its type
	* If fillColor is null, the contour is not filled.
	* If borderColor is null, the border is not drawn
	*/
	public void drawGeometry(Geometry geom, Graphics g, MapContext mc, Color borderColor, Color fillColor, int width, ImageObserver observer) {
		if (g == null || geom == null)
			return;
		if (borderColor == null && fillColor == null)
			return; //nothing is visible
		RealRectangle terr = mc.getVisibleTerritory();
		if (!geom.fitsInRectangle(terr.rx1, terr.ry1, terr.rx2, terr.ry2))
			return;
		if (geom instanceof ImageGeometry) {
			ImageGeometry igeom = (ImageGeometry) geom;
			if (igeom.img != null) {
				if (fillColor == null && (borderColor == null || width < 1))
					return;
				if (igeom.ry2 - igeom.ry1 > 10) {
					int ih = igeom.img.getHeight(null), iw = igeom.img.getWidth(null);
					if (ih <= 0 || iw <= 0)
						return;
					if (fillColor == null)
						return;
					//draw image by stripes
					int x1 = getScreenX(mc, igeom.rx1, igeom.ry1), x2 = getScreenX(mc, igeom.rx2, igeom.ry2), y1 = getScreenY(mc, igeom.rx1, igeom.ry1), minY = y1, maxY = y1;
					float yy1 = igeom.ry1;
					int iy1 = ih;
					int nSteps = (int) Math.ceil((igeom.ry2 - igeom.ry1) / 10);
					float stepY = (igeom.ry2 - igeom.ry1) / nSteps;
					do {
						float yy2 = yy1 + stepY;
						int iy2 = Math.round((igeom.ry2 - yy2) / (igeom.ry2 - igeom.ry1) * ih);
						int y2 = getScreenY(mc, igeom.rx1, yy2);
						g.drawImage(igeom.img, x1, y2, x2, y1,//destination rectangle
								0, iy2, iw, iy1, //source rectangle
								observer);
						yy1 = yy2;
						iy1 = iy2;
						y1 = y2;
						minY = y2;
					} while (yy1 < igeom.ry2);
					if (borderColor != null && width > 1) {
						g.setColor(borderColor);
						Drawing.drawRectangle(g, width, x1, minY, x2 - x1, maxY - minY);
					}
				} else {
					int x1 = getScreenX(mc, igeom.rx1, igeom.ry1), y1 = getScreenY(mc, igeom.rx1, igeom.ry1), x2 = getScreenX(mc, igeom.rx2, igeom.ry2), y2 = getScreenY(mc, igeom.rx2, igeom.ry2);
					if (fillColor != null) {
						g.drawImage(igeom.img, x1, y2, x2 - x1, y1 - y2,//destination rectangle
								observer);
					}
					if (borderColor != null && width > 1) {
						g.setColor(borderColor);
						Drawing.drawRectangle(g, width, x1, y1, x2 - x1, y2 - y1);
					}
				}
			}
		} else if (geom instanceof RealPoint) {
			RealPoint rp = (RealPoint) geom;
			int x = getScreenX(mc, rp.x, rp.y), y = getScreenY(mc, rp.x, rp.y);
			if (signWidth <= 0) {
				signWidth = Math.round(2.0f * Toolkit.getDefaultToolkit().getScreenResolution() / 25.33f);
				halfSignWidth = signWidth / 2;
			}
			if (fillColor != null) {
				g.setColor(fillColor);
				g.fillOval(x - halfSignWidth, y - halfSignWidth, signWidth, signWidth);
			}
			if (borderColor != null) {
				g.setColor(borderColor);
				Drawing.drawOval(g, width, x - halfSignWidth, y - halfSignWidth, signWidth, signWidth);
			}
		} else if (geom instanceof RealRectangle) {
			RealRectangle rr = (RealRectangle) geom;
			xe[0] = getScreenX(mc, rr.rx1, rr.ry1);
			ye[0] = getScreenY(mc, rr.rx1, rr.ry1);
			xe[1] = getScreenX(mc, rr.rx1, rr.ry2);
			ye[1] = getScreenY(mc, rr.rx1, rr.ry2);
			xe[2] = getScreenX(mc, rr.rx2, rr.ry2);
			ye[2] = getScreenY(mc, rr.rx2, rr.ry2);
			xe[3] = getScreenX(mc, rr.rx2, rr.ry1);
			ye[3] = getScreenY(mc, rr.rx2, rr.ry1);
			xe[4] = xe[0];
			ye[4] = ye[0];
			if (fillColor != null) {
				g.setColor(fillColor);
				g.fillPolygon(xe, ye, 5);
			}
			if (borderColor != null) {
				g.setColor(borderColor);
				Drawing.drawPolyline(g, width, xe, ye, 5, true);
			}
		} else if (geom instanceof RealCircle) {
			RealCircle cir = (RealCircle) geom;
			float b[] = cir.getBoundRect();
			if (b == null)
				return;
			int x1 = getScreenX(mc, b[0], b[1]), y1 = getScreenY(mc, b[0], b[1]), x2 = getScreenX(mc, b[2], b[3]), y2 = getScreenY(mc, b[2], b[3]);
			if (fillColor != null) {
				g.setColor(fillColor);
				g.fillOval(x1, y2, x2 - x1 + 1, y1 - y2 + 1);
			}
			if (borderColor != null) {
				g.setColor(borderColor);
				Drawing.drawOval(g, width, x1, y2, x2 - x1 + 1, y1 - y2 + 1);
			}
		} else if (geom instanceof RealLine) {
			if (borderColor == null || width < 1)
				return;
			if (arrowHeight <= 0) {
				arrowHeight = Math.round(3.0f * mm);
				arrowWidth = Math.round(1.5f * mm);
			}
			RealLine line = (RealLine) geom;
			int x1 = getScreenX(mc, line.x1, line.y1), y1 = getScreenY(mc, line.x1, line.y1), x2 = getScreenX(mc, line.x2, line.y2), y2 = getScreenY(mc, line.x2, line.y2);
			g.setColor(borderColor);
			if (line.directed && line.x1 == line.x2 && line.y1 == line.y2) {
				int rad0 = Math.round(1.5f * mm);
				for (int iw = 0; iw < width + 1; iw++) {
					int rad = rad0 + iw, diam = rad * 2;
					g.drawOval(x1 - rad, y1 - rad, diam, diam);
				}
/*
        int aw=Math.max(arrowWidth,width+4);
        int x0=x1-rad0-(width+1)/2;
        int y0=y1+arrowHeight/2;
        int x[]={x0,x0-aw/2,x0+aw/2,x0};
        int y[]={y0,y0-arrowHeight,y0-arrowHeight,y0};
        g.drawPolygon(x,y,4);
        g.fillPolygon(x,y,4);
*/
			} else {
				if (isGeo && cross180Meridian(line.x1, line.x2)) {
					float xx[] = breakLine(line.x1, line.x2);
					int x_2 = getScreenX(mc, xx[0], line.y2), x_1 = getScreenX(mc, xx[1], line.y1);
					if (line.directed) {
						Drawing.drawVector(g, width, x1, y1, x_2, y2, arrowHeight + width / 3, arrowWidth + width, true);
						Drawing.drawVector(g, width, x_1, y1, x2, y2, arrowHeight + width / 3, arrowWidth + width, true);
					} else {
						Drawing.drawLine(g, width, x1, y1, x_2, y2, true, true);
						Drawing.drawLine(g, width, x_1, y1, x2, y2, true, true);
					}
				} else if (line.directed) {
					Drawing.drawVector(g, width, x1, y1, x2, y2, arrowHeight + width / 3, arrowWidth + width, true);
				} else {
					Drawing.drawLine(g, width, x1, y1, x2, y2, true, true);
				}
			}
		} else if (geom instanceof RealPolyline) {
			boolean drawn = false;
			float br[] = geom.getBoundRect();
			boolean cross180 = isGeo && cross180Meridian(br[0], br[2]);
			/**/
			if (!cross180 && (geom instanceof RealPolygon) && drawParam.drawHoles) { // complex polygon with holes
				RealPolygon rp = (RealPolygon) geom;
				if (rp.pp != null) {
					int xe[] = new int[rp.p.length];
					int ye[] = new int[rp.p.length];
					for (int i = 0; i < xe.length; i++) {
						xe[i] = getScreenX(mc, rp.p[i].x, rp.p[i].y);
						ye[i] = getScreenY(mc, rp.p[i].x, rp.p[i].y);
					}
					Object xa[] = new Object[rp.pp.size()];
					Object ya[] = new Object[rp.pp.size()];
					for (int j = 0; j < xa.length; j++) {
						RealPolyline rl = (RealPolyline) rp.pp.elementAt(j);
						int x[] = new int[rl.p.length];
						int y[] = new int[rl.p.length];
						for (int i = 0; i < x.length; i++) {
							x[i] = getScreenX(mc, rl.p[i].x, rl.p[i].y);
							y[i] = getScreenY(mc, rl.p[i].x, rl.p[i].y);
						}
						xa[j] = x;
						ya[j] = y;
					}
					if (fillColor != null && rp.isClosed) {
						g.setColor(fillColor);
						if (Drawing2D.isJava2D) {
							Drawing2D.fillPolygon(g, xe, ye, xa, ya);
						} else {
							g.fillPolygon(xe, ye, xe.length);
						}
					}
					if (borderColor != null) {
						g.setColor(borderColor);
						Drawing.drawPolyline(g, width, xe, ye, xe.length, rp.getIsClosed());
						for (int i = 0; i < rp.pp.size(); i++) {
							Drawing.drawPolyline(g, width, (int[]) xa[i], (int[]) ya[i], ((int[]) xa[i]).length, rp.getIsClosed());
						}
					}
					drawn = true;
				}
			}
			/**/
			if (!drawn) {
				RealPolyline rl = (RealPolyline) geom;
				if (rl.p != null && rl.p.length > 1)
					if (!cross180) {
						synchronized (xcoord) {
							xcoord.removeAllElements();
							ycoord.removeAllElements();
							for (RealPoint element : rl.p) {
								xcoord.addElement(getScreenX(mc, element.x, element.y));
								ycoord.addElement(getScreenY(mc, element.x, element.y));
							}
							/*
							if (rl.getIsClosed() &&
							    (xcoord.elementAt(0)!=xcoord.elementAt(xcoord.size()-1) ||
							     ycoord.elementAt(0)!=ycoord.elementAt(ycoord.size()-1))) {
							  xcoord.addElement(xcoord.elementAt(0));
							  ycoord.addElement(ycoord.elementAt(0));
							}
							*/
							if (fillColor != null && rl.getIsClosed()) {
								g.setColor(fillColor);
								g.fillPolygon(xcoord.getArray(), ycoord.getArray(), xcoord.size());
							}
							if (borderColor != null) {
								g.setColor(borderColor);
								Drawing.drawPolyline(g, width, xcoord.getArray(), ycoord.getArray(), xcoord.size(), rl.getIsClosed());
							}
						}
					} else {
						RealPoint p1 = rl.p[0];
						int x1 = getScreenX(mc, p1.x, p1.y), y1 = getScreenY(mc, p1.x, p1.y);
						for (int i = 1; i < rl.p.length; i++) {
							RealPoint p2 = rl.p[i];
							int x2 = getScreenX(mc, p2.x, p2.y), y2 = getScreenY(mc, p2.x, p2.y);
							if (cross180Meridian(p1.x, p2.x)) {
								float xx[] = breakLine(p1.x, p2.x);
								int x_2 = getScreenX(mc, xx[0], p2.y), x_1 = getScreenX(mc, xx[1], p1.y);
								Drawing.drawLine(g, width, x1, y1, x_2, y2, true, true);
								Drawing.drawLine(g, width, x_1, y1, x2, y2, true, true);
							} else {
								Drawing.drawLine(g, width, x1, y1, x2, y2, true, true);
							}
							p1 = p2;
							x1 = x2;
							y1 = y2;
						}
					}
			}
		} else if (geom instanceof MultiGeometry) {
			MultiGeometry mg = (MultiGeometry) geom;
			for (int i = 0; i < mg.getPartsCount(); i++) {
				drawGeometry(mg.getPart(i), g, mc, borderColor, fillColor, width, null);
			}
		}
	}

	/**
	 * Checks if the points with the given x-coordinates are on two sides of the 180� meridian
	 */
	public static boolean cross180Meridian(float x1, float x2) {
		return Math.abs(x1 - x2) > 270 && Math.min(x1, x2) < 0 && Math.max(x1, x2) > 0;
	}

	/**
	 * If the line connecting the points with the given x-coordinates crosses the 180� meridian,
	 * constructs 2 additional points to avoid a line crossing the whole map display.
	 * Returns the x-coordinates of the additional points.
	 */
	public static float[] breakLine(float x1, float x2) {
		if (x1 < 0 && x2 > 0) {
			float xx[] = { x2 - 360, x1 + 360 };
			return xx;
		} else if (x1 > 0 && x2 < 0) {
			float xx[] = { x2 + 360, x1 - 360 };
			return xx;
		}
		return null;
	}

	/**
	* Marks a geometry (when highlighted or selected) by a special sign, e.g cross
	*/
	public void markGeometry(Geometry geom, Graphics g, MapContext mc, Color signColor) {
		if (g == null || geom == null)
			return;
		RealRectangle terr = mc.getVisibleTerritory();
		if (!geom.fitsInRectangle(terr.rx1, terr.ry1, terr.rx2, terr.ry2))
			return;
		if (crossWidth <= 0) {
			crossWidth = Math.round(2.5f * Toolkit.getDefaultToolkit().getScreenResolution() / 25.33f);
		}
		if (!(geom instanceof MultiGeometry)) {
			RealRectangle rr = getLabelRectangle(geom);
			float rx = (rr.rx1 + rr.rx2) / 2, ry = (rr.ry1 + rr.ry2) / 2;
			if (terr.contains(rx, ry, 0.0f)) {
				g.setColor(signColor);
				Icons.drawCross(g, getScreenX(mc, rx, ry), getScreenY(mc, rx, ry), crossWidth, crossWidth);
			}
		} else {
			MultiGeometry mg = (MultiGeometry) geom;
			for (int i = 0; i < mg.getPartsCount(); i++) {
				markGeometry(mg.getPart(i), g, mc, signColor);
			}
		}
	}

	/**
	* If the DGeoLayer this DGeoObject belongs to has its Visualiser, and the
	* Visualizer produces diagrams (e.g. realises the bar chart presentation
	* method), the diagrams should be drawn on top of all the layers. Therefore
	* there is a separate function drawDiagram, which is to be called after
	* all GeoLayers have drawn themselves.
	*/
	public void drawDiagram(Graphics g, MapContext mc) {
		if (data == null || vis == null)
			return;
		Geometry geom = getGeometry();
		if (geom == null)
			return;
		if (currAppearance == null) {
			defineCurrentAppearance(mc);
		}
		Object diagram = vis.getPresentation(data, mc);
		if (diagram != null) {
			drawIconOrDiagram(diagram, g, mc);
		} else if (getSpatialType() == Geometry.point) {
			defineCurrentAppearance(mc);
			if (!currAppearance.isVisible)
				return;
			drawGeometry(g, mc, currAppearance.lineColor, currAppearance.fillColor, currAppearance.lineWidth, imgObserver);
		}
		currAppearance.signSize = signWidth;
		currAppearance.signColor = (currAppearance.fillColor != null) ? currAppearance.fillColor : currAppearance.lineColor;
		if (diagram != null)
			if (diagram instanceof StructSign) {
				StructSign sign = (StructSign) diagram;
				if (sign.getNSegments() == 1) {
					Color c = sign.getSegmentColor(0);
					if (c != null) {
						currAppearance.signColor = c;
					}
					currAppearance.signSize = Math.round(sign.getSegmentPart(0) * sign.getMaxHeight());
				}
			} else if (diagram instanceof Sign) {
				Sign sign = (Sign) diagram;
				Color c = sign.getColor();
				if (c != null) {
					currAppearance.signColor = c;
				}
				currAppearance.signSize = Math.max(sign.getWidth(), sign.getHeight());
			} else if (diagram instanceof GeomSign) {
				GeomSign sign = (GeomSign) diagram;
				Color c = sign.getFillColor();
				if (c != null) {
					currAppearance.signColor = c;
				}
				currAppearance.signSize = Math.max(sign.getWidth(), sign.getHeight());
			}
	}

	/**
	* Should take into account the position of the diagram, if any.
	* Labels have to be drawn later than diagrams (if diagrams exist)
	* The function drawLabel uses the label color set in the DrawingParameters.
	*/
	public void drawLabel(Graphics g, MapContext mc) {
		this.drawLabel(g, mc, 0);
	}

	public void drawLabel(Graphics g, MapContext mc, int styleOption) {
		if (data == null || label == null || label.length() < 1)
			return;
		RealRectangle rr = getLabelRectangle();
		if (rr == null)
			return;
		Rectangle rClip = g.getClipBounds();
		boolean underlined = (styleOption == DrawingParameters.UNDERLINED || styleOption == (DrawingParameters.UNDERLINED | DrawingParameters.SHADOWED));
		boolean shadowed = (styleOption == DrawingParameters.SHADOWED || styleOption == (DrawingParameters.UNDERLINED | DrawingParameters.SHADOWED));

		FontMetrics fm = g.getFontMetrics();
		int x = getScreenX(mc, rr.rx1, rr.ry1), y = getScreenY(mc, rr.rx2, rr.ry2), asc = fm.getAscent(), yt = y + asc, txtW = fm.stringWidth(label), fontH = fm.getHeight(), txtH = fontH;
		if (labelPos != null) {
			yt = labelPos.y + asc;
		}

		boolean moreLines = false;
		if (getSpatialType() == Geometry.area) {
			int w = getScreenX(mc, rr.rx2, rr.ry2) - x, h = getScreenY(mc, rr.rx1, rr.ry1) - y;
			if (w * 1.1f < txtW || h < txtH) {
				if (h > fontH && label.indexOf(';') > 0) {
					StringTokenizer st = new StringTokenizer(label, ";\r\n");
					int tw = 0, th = 0;
					while (st.hasMoreTokens()) {
						int lw = fm.stringWidth(st.nextToken());
						if (lw > tw) {
							tw = lw;
						}
						th += fontH;
					}
					if (tw < 1 || th < 1)
						return;
					if (w * 1.25f < tw || h < th)
						return;
					moreLines = true;
					txtW = tw;
					txtH = th;
				} else
					return;
			}
			x += (w - txtW) / 2;
			if (labelPos != null) {
				if (x > labelPos.x) {
					x = labelPos.x;
				} else if (x + txtW < labelPos.x + txtW / 3) {
					x = labelPos.x - txtW / 2;
				}
				int y1 = (labelPos.y + y + h) / 2;
				if (y1 < yt) {
					yt = y1;
				}
			} else {
				yt += h / 2 - txtH / 2;
			}
			if (yt > y + h - fontH) {
				yt = y + h - fontH;
			}
		}
		int clipX = x, clipY = yt - asc, clipW = txtW, clipH = txtH;
		if (underlined) {
			clipH += 2;
		}
		if (rClip != null) {
			if (clipX >= rClip.x + rClip.width)
				return;
			if (clipX + clipW <= rClip.x)
				return;
			if (clipY >= rClip.y + rClip.height)
				return;
			if (clipY + clipH <= rClip.y)
				return;
			if (clipX < rClip.x) {
				clipW -= rClip.x - clipX;
				clipX = rClip.x;
			}
			if (clipX + clipW > rClip.x + rClip.width) {
				clipW -= clipX + clipW - (rClip.x + rClip.width);
			}
			if (clipY < rClip.y) {
				clipH -= rClip.y - clipY;
				clipY = rClip.y;
			}
			if (clipY + clipH > rClip.y + rClip.height) {
				clipH -= clipY + clipH - (rClip.y + rClip.height);
			}
		}
		g.setClip(clipX, clipY, clipW, clipH);

		Color c = g.getColor();
		Color cShadow = ((c.getRed() + c.getGreen() + c.getBlue()) > 3 * 128) ? Color.black : Color.white;
		if (moreLines) {
			StringTokenizer st = new StringTokenizer(label, ";\r\n");
			int yPos = yt;
			while (st.hasMoreTokens()) {
				String l = st.nextToken();
				if (st.hasMoreTokens()) {
					l += ";";
				}
				if (shadowed) {
					g.setColor(cShadow);
					g.drawString(l, x + 1, yPos + 1);
				}
				g.setColor(c);
				g.drawString(l, x, yPos);
				yPos += fontH;
			}
		} else {
			if (shadowed) {
				g.setColor(cShadow);
				g.drawString(label, x + 1, yt + 1);
			}
			g.setColor(c);
			g.drawString(label, x, yt);
		}
		if (underlined) {
			g.drawLine(x, yt + 1, x + txtW, yt + 1);
		}
		if (styleOption == (DrawingParameters.UNDERLINED | DrawingParameters.SHADOWED)) {
			g.setColor(cShadow);
			g.drawLine(x, yt + 2, x + txtW, yt + 2);
		}
		g.setColor(c);
		if (rClip != g.getClipBounds()) {
			g.setClip(rClip);
		}
	}

	/**
	* The following functions are used to support highlighting (transient
	* marking made when the mouse moves) and selection (durable marking
	* switched on/off by mouse click).
	*/
	public boolean isHighlighted() {
		return highlighted;
	}

	public void setIsHighlighted(boolean value) {
		highlighted = value;
	}

	public boolean isSelected() {
		return selected;
	}

	public void setIsSelected(boolean value) {
		selected = value;
	}

	public void drawInXORMode(Graphics g, MapContext mc, Color color, int width) {
		if (g == null || data == null)
			return;
		Geometry geom = getGeometry();
		if (geom == null)
			return;
		Color c = Color.black;
		if (selected && !color.equals(selectColor)) {
			c = selectColor;
		} else if (drawParam != null && drawParam.drawBorders) {
			c = drawParam.lineColor;
		} else if (lastColor != null) {
			c = lastColor;
		} else if (drawParam != null) {
			c = drawParam.fillColor;
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
			drawGeometry(geom, g, mc, color, null, width, null);
		}
		if (drawParam != null && drawParam.hlDrawCircles && geom.getType() == Geometry.area) {
			RealRectangle rr = getLabelRectangle(geom);
			if (rr != null) {
				g.setXORMode((lastColor == null) ? drawParam.fillColor : lastColor);
				g.setColor(drawParam.hlCircleColor);
				for (int i = 0; i < mc.getMapCount(); i++) {
					mc.setCurrentMapN(i);
					int x1 = getScreenX(mc, rr.rx1, rr.ry1), y1 = getScreenY(mc, rr.rx2, rr.ry2), x2 = getScreenX(mc, rr.rx2, rr.ry2), y2 = getScreenY(mc, rr.rx1, rr.ry1);
					if (x2 > x1 && y2 > y1) {//enough space
						int x0 = (x1 + x2) / 2, y0 = (y1 + y2) / 2, rad = drawParam.hlCircleSize / 2;
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
						g.fillOval(x0 - rad, y0 - rad, drawParam.hlCircleSize + 1, drawParam.hlCircleSize + 1);
					} else {
						break;
					}
				}
			}
		}
		g.setPaintMode();
		if (multiMap && prevClip != null) {
			g.setClip(prevClip.x, prevClip.y, prevClip.width, prevClip.height);
		}
	}

	/**
	* This method is used to switch on/off transient highlighting.
	* Does not affect the current drawing parameters of the object (lineColor,
	* fillColor etc.)
	* May use XOR operation to combine the highlight color (set by default)
	* with the color in which the object has been last painted.
	*/
	public void highlight(Graphics g, MapContext mc, boolean isHighlighted) {
		if (highlighted == isHighlighted)
			return;
		highlighted = isHighlighted;
		drawInXORMode(g, mc, highlightColor, (drawParam == null) ? 1 : drawParam.hlWidth);
	}

	public void hideHighlight(Graphics g, MapContext mc) {
		if (highlighted) {
			drawInXORMode(g, mc, highlightColor, (drawParam == null) ? 1 : drawParam.hlWidth);
		}
	}

	public void showHighlight(Graphics g, MapContext mc) {
		if (highlighted) {
			drawInXORMode(g, mc, highlightColor, (drawParam == null) ? 1 : drawParam.hlWidth);
		}
	}

	/**
	* This method is used to switch on/off selection, or durable highlighting.
	* Does not really change the appearance of the object, only sets an
	* internal variable.
	*/
	public void select(Graphics g, MapContext mc, boolean isSelected) {
		selected = isSelected;
	}

	public void showSelection(Graphics g, MapContext mc) {
		if (mc == null)
			return;
		Rectangle prevClip = null;
		boolean multiMap = mc.getMapCount() > 1;
		if (multiMap) {
			prevClip = g.getClipBounds();
		}
		Geometry geom = getGeometry();
		for (int i = 0; i < mc.getMapCount(); i++) {
			mc.setCurrentMapN(i);
			if (multiMap) {
				Rectangle mb = mc.getMapBounds(i);
				g.setClip(mb.x, mb.y, mb.width, mb.height);
			}
			if (geom instanceof RealRectangle) {
				drawSelectedRectangle((RealRectangle) geom, g, mc);
			} else {
				drawGeometry(geom, g, mc, selectColor, null, (drawParam == null) ? 1 : drawParam.selWidth, null);
			}
		}
		if (multiMap && prevClip != null) {
			g.setClip(prevClip.x, prevClip.y, prevClip.width, prevClip.height);
		}
	}

	public void hideSelection(Graphics g, MapContext mc) {
	}

	/**
	* A special case: if the object has a geometry of the type RealRectangle,
	* its selection is indicated by hatching
	*/
	protected void drawSelectedRectangle(RealRectangle rr, Graphics g, MapContext mc) {
		int x1 = getScreenX(mc, rr.rx1, rr.ry1), y2 = getScreenY(mc, rr.rx1, rr.ry1), x2 = getScreenX(mc, rr.rx2, rr.ry2), y1 = getScreenY(mc, rr.rx2, rr.ry2);
		int w = x2 - x1, h = y2 - y1, lw = (drawParam == null) ? 1 : drawParam.selWidth;
		g.setColor(selectColor);
		if (lw == 1) {
			g.drawRect(x1, y1, w, h);
		} else {
			Drawing.drawRectangle(g, lw, x1, y1, w, h);
		}
		if (w - lw > 5 && h - lw > 5) {
			for (int x = x1 + 5; x < x2; x += 5) {
				g.drawLine(x, y1, x, y2);
			}
			for (int y = y1 + 5; y < y2; y += 5) {
				g.drawLine(x1, y, x2, y);
			}
		}
	}

	@Override
	public String toString() {
		return "DGeoObject; id=" + getIdentifier();
	}

	/**
	 * Orders the given vector of DGeoObjects by their time references
	 */
	public static void sortGeoObjectsByTimes(Vector geoObj) {
		if (geoObj == null || geoObj.size() < 2)
			return;
		BubbleSort.sort(geoObj);
	}

	/**
	 * Compares this object with the given objects by their time references
	 *  Returns 0 if equal, <0 if THIS is less than the argument, >0 otherwise
	 */
	@Override
	public int compareTo(spade.lib.util.Comparable c) {
		if (c == null)
			return -1;
		if (!(c instanceof DGeoObject))
			return -1;
		TimeReference tr0 = getTimeReference(), tr1 = ((DGeoObject) c).getTimeReference();
		if (tr0 == null || tr0.getValidFrom() == null)
			if (tr1 == null || tr1.getValidFrom() == null)
				return 0;
			else
				return -1;
		else if (tr1 == null || tr1.getValidFrom() == null)
			return 1;
		int cmp = tr0.getValidFrom().compareTo(tr1.getValidFrom());
		if (cmp != 0)
			return cmp;
		return tr0.getValidUntil().compareTo(tr1.getValidUntil());
	}

	/**
	 * After a transformation of the time references, e.g. from absolute
	 * to relative times, updates its internal variables.
	 */
	public void updateStartEndTimes() {
		TimeReference tref = getTimeReference();
		if (tref != null) {
			ThematicDataItem thema = getData();
			if (thema != null) {
				thema.setTimeReference(tref);
			}
		}
	}

	/**
	 * Adds a neighbour of this place, i.e. there are common borders segments.
	 */
	public void addNeighbour(DGeoObject nei) {
		if (nei == null)
			return;
		addNeighbour(nei.getIdentifier());
	}

	/**
	 * Adds a neighbour of this place (specified by the identifier),
	 * i.e. there are common borders segments.
	 */
	public void addNeighbour(String neibId) {
		if (neighbours == null) {
			neighbours = new Vector<String>(10, 10);
		}
		if (!neighbours.contains(neibId)) {
			neighbours.addElement(neibId);
		}
	}

	/**
	 * Checks if the object with the given identifier is a neighbour of this object
	 */
	public boolean isNeighbour(String neibId) {
		if (neighbours == null)
			return false;
		return neighbours.contains(neibId);
	}
}
