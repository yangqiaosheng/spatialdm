package spade.vis.dmap;

import spade.time.TimeMoment;
import spade.time.TimeReference;
import spade.vis.database.SpatialDataItem;
import spade.vis.database.SpatialEntity;
import spade.vis.geometry.GeoComp;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.MultiGeometry;
import spade.vis.geometry.RealLine;
import spade.vis.geometry.RealPoint;
import spade.vis.geometry.RealPolyline;
import spade.vis.geometry.RealRectangle;
import spade.vis.space.GeoObject;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 02-Jan-2007
 * Time: 18:14:11
 * DLinkObject represents a link in a node-link structure. A link connects two
 * ordinary DGeoObject instances, which may be points or areas. Each instance
 * of DLinkObject has references to two DGeoObjects connected by this link.
 * A link may have any attributes. A link is directed. The source and
 * destination nodes of a link may be time-referenced.
 */
public class DLinkObject extends DGeoObject {
	/**
	 * The start (source) node for this link.
	 */
	protected DGeoObject startNode = null;
	/**
	 * The end (destination) node for this link.
	 */
	protected DGeoObject endNode = null;
	/**
	 * The time reference of the start node (may be null)
	 */
	protected TimeMoment startTime = null;
	/**
	 * The time reference of the end node (may be null)
	 */
	protected TimeMoment endTime = null;
	/**
	 * The number of times this link was used (passed)
	 */
	protected int nTimes = 0;

	/**
	* Setup acts as a constructor: a DLinkObject is always created on the basis
	* of two DGeoObjects, possibly, with time references.
	*/
	public void setup(DGeoObject sou, DGeoObject dest, TimeMoment tSou, TimeMoment tDest) {
		startNode = sou;
		endNode = dest;
		startTime = tSou;
		endTime = tDest;
		type = Geometry.line;
		if (sou != null && dest != null && sou.getGeometry() != null && dest.getGeometry() != null) {
			//construct the geometry of this object
			float x1 = Float.NaN, y1 = Float.NaN, x2 = Float.NaN, y2 = Float.NaN;
			Geometry g = sou.getGeometry();
			float coords[] = g.getCentroid();
			if (coords != null) {
				x1 = coords[0];
				y1 = coords[1];
			} else if (g instanceof RealPoint) {
				RealPoint p = (RealPoint) g;
				x1 = p.x;
				y1 = p.y;
			} else {
				float bounds[] = g.getBoundRect();
				RealRectangle rr = null;
				if (g instanceof RealPolyline) {
					rr = ((RealPolyline) g).getLabelRect();
				} else if (g instanceof MultiGeometry) {
					rr = ((MultiGeometry) g).getLabelRect();
				}
				if (rr != null) {
					x1 = (rr.rx1 + rr.rx2) / 2;
					y1 = (rr.ry1 + rr.ry2) / 2;
				} else if (bounds != null) {
					x1 = (bounds[0] + bounds[2]) / 2;
					y1 = (bounds[1] + bounds[3]) / 2;
				}
			}
			if (Float.isNaN(x1) || Float.isNaN(y1))
				return;
			g = dest.getGeometry();
			coords = g.getCentroid();
			if (coords != null) {
				x2 = coords[0];
				y2 = coords[1];
			} else if (g instanceof RealPoint) {
				RealPoint p = (RealPoint) g;
				x2 = p.x;
				y2 = p.y;
			} else {
				float bounds[] = g.getBoundRect();
				RealRectangle rr = null;
				if (g instanceof RealPolyline) {
					rr = ((RealPolyline) g).getLabelRect();
				} else if (g instanceof MultiGeometry) {
					rr = ((MultiGeometry) g).getLabelRect();
				}
				if (rr != null) {
					x2 = (rr.rx1 + rr.rx2) / 2;
					y2 = (rr.ry1 + rr.ry2) / 2;
				} else if (bounds != null) {
					x2 = (bounds[0] + bounds[2]) / 2;
					y2 = (bounds[1] + bounds[3]) / 2;
				}
			}
			if (Float.isNaN(x2) || Float.isNaN(y2))
				return;
			RealLine line = new RealLine();
			if (x1 != x2 || y1 != y2) {
				float dx = x2 - x1, dy = y2 - y1, ddx = dx * 0.08f, ddy = dy * 0.08f;
				x1 += ddx;
				y1 += ddy;
				x2 -= ddx;
				y2 -= ddy;
			}
			line.setup(x1, y1, x2, y2);
			if (data == null) {
				data = new SpatialEntity("link_" + sou.getIdentifier() + "_" + dest.getIdentifier());
			}
			data.setGeometry(line);
			if (startTime != null && endTime != null) {
				TimeReference tref = new TimeReference();
				tref.setValidFrom(startTime);
				tref.setValidUntil(endTime);
				data.setTimeReference(tref);
			}
			this.id = data.getId();
		}
	}

	/**
	 * Sets the identifier of this object
	 */
	@Override
	public void setIdentifier(String id) {
		if (data == null) {
			data = new SpatialEntity(id);
		} else {
			((SpatialEntity) data).setId(id);
		}
		this.id = data.getId();
	}

	/**
	 * Sets the name of this object
	 */
	public void setName(String name) {
		if (data != null) {
			((SpatialEntity) data).setName(name);
		}
		if (label == null) {
			label = name;
		}
	}

	/**
	 * Same as setup(sou,dest,null,null);
	 */
	public void setup(DGeoObject sou, DGeoObject dest) {
		setup(sou, dest, null, null);
	}

	/**
	 * The start (source) node for this link.
	 */
	public DGeoObject getStartNode() {
		return startNode;
	}

	/**
	 * The end (destination) node for this link.
	 */
	public DGeoObject getEndNode() {
		return endNode;
	}

	/**
	 * The time reference of the start node (may be null)
	 */
	public TimeMoment getStartTime() {
		return startTime;
	}

	/**
	 * The time reference of the end node (may be null)
	 */
	public TimeMoment getEndTime() {
		return endTime;
	}

	/**
	 * Returns the number of times this link was used (passed)
	 */
	public int getNTimes() {
		return nTimes;
	}

	/**
	 * Sets the number of times this link was used (passed)
	 */
	public void setNTimes(int nTimes) {
		this.nTimes = nTimes;
	}

	/**
	 * Counts one more use of this link
	 */
	public void incNTimes() {
		++nTimes;
	}

	/**
	 * Returns the direction of the link: N, NE, E, etc.
	 * If the start and end coincide, returns null.
	 */
	public String getLinkDirection() {
		if (startNode == null || endNode == null)
			return null;
		if (startNode.equals(endNode))
			return "0";
		RealPoint p1 = GeoComp.getPosition(startNode.getGeometry()), p2 = GeoComp.getPosition(endNode.getGeometry());
		if (p1 == null || p2 == null)
			return null;
		float dx = p2.x - p1.x, dy = p2.y - p1.y;
		return GeoComp.getDirectionAsString(GeoComp.getAngleXAxis(dx, dy));
	}

	/**
	 * Returns the length of the link
	 */
	public double getLength() {
		if (startNode == null || endNode == null)
			return Double.NaN;
		if (startNode.equals(endNode))
			return 0;
		RealPoint p1 = GeoComp.getPosition(startNode.getGeometry()), p2 = GeoComp.getPosition(endNode.getGeometry());
		if (p1 == null || p2 == null)
			return Double.NaN;
		return GeoComp.distance(p1.x, p1.y, p2.x, p2.y, this.isGeo);
	}

	/**
	* Returns a copy of this object. The spatial and thematic data items
	* used by this objects are not duplicated; the copy will use references to the
	* same data items
	*/
	@Override
	public GeoObject makeCopy() {
		DLinkObject obj = new DLinkObject();
		obj.setup(startNode, endNode, startTime, endTime);
		if (data != null) {
			obj.setup((SpatialDataItem) data.clone());
		}
		if (label != null) {
			obj.label = new String(label);
		}
		obj.highlighted = highlighted;
		obj.selected = selected;
		return obj;
	}

	/**
	* Does nothing; the type of DLinkObject is always Geometry.line.
	*/
	@Override
	public void setSpatialType(char objType) {
	}
}
