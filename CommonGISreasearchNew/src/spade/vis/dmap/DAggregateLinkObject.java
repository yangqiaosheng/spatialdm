package spade.vis.dmap;

import java.awt.Color;
import java.util.Vector;

import spade.time.TimeMoment;
import spade.vis.database.SpatialEntity;
import spade.vis.geometry.GeoComp;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.RealPoint;
import spade.vis.space.GeoObject;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: 13-Aug-2007
 * Time: 15:12:28
 * Represents aggregate links summarising several ordinary links or moves
 * with the same starting and ending positions.
 */
public class DAggregateLinkObject extends DGeoObject {
	/**
	 * The list of the original links or moves this object aggregates.
	 * The elements are instances of DLinkObject.
	 */
	public Vector souLinks = null;
	/**
	 * The list of identifiers of the trajectories the original links have
	 * been produced from. May be null.
	 */
	public Vector souTrajIds = null;
	/**
	 * The start (source) node for this aggregate link.
	 */
	public DGeoObject startNode = null;
	/**
	 * The end (destination) node for this aggregate link.
	 */
	public DGeoObject endNode = null;
	/**
	 * The earliest time of visiting the start node (may be null)
	 */
	public TimeMoment firstTime = null;
	/**
	 * The latest time of visiting the end node (may be null)
	 */
	public TimeMoment lastTime = null;
	/**
	 * The number of active links, i.e. links satisfying the filter
	 */
	public int nActiveLinks = 0;
	/**
	 * The color of this link (may be null)
	 */
	public Color color = null;

	/**
	 * Adds an original link to be aggregated. If this is the first link,
	 * the start and end nodes are taken from it.
	 * The second argument may be null.
	 */
	public void addLink(DLinkObject link, String trajectoryId) {
		if (souLinks == null || souLinks.size() < 1) {
			souLinks = new Vector(100, 100);
			if (trajectoryId != null) {
				souTrajIds = new Vector(100, 100);
			}
			startNode = link.startNode;
			endNode = link.endNode;
			if (data == null) {
				data = new SpatialEntity("flows_" + startNode.getIdentifier() + "_" + endNode.getIdentifier());
			}
			Geometry geom = link.getGeometry();
			if (geom != null) {
				data.setGeometry((Geometry) geom.clone());
			}
			this.id = data.getId();
		}
		souLinks.addElement(link);
		nActiveLinks = souLinks.size();
		if (souTrajIds != null) {
			souTrajIds.addElement(trajectoryId);
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
	 * Returns the direction of the link: N, NE, E, etc.
	 * If the start and end coincide, returns null.
	 */
	public String getLinkDirection() {
		if (startNode == null || endNode == null || startNode.equals(endNode))
			return null;
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
		if (startNode == null || endNode == null || startNode.equals(endNode))
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
		DAggregateLinkObject obj = new DAggregateLinkObject();
		if (souLinks != null && souLinks.size() > 0) {
			for (int i = 0; i < souLinks.size(); i++) {
				obj.addLink((DLinkObject) souLinks.elementAt(i), (souTrajIds == null) ? null : (String) souTrajIds.elementAt(i));
			}
		}
		obj.label = label;
		obj.highlighted = highlighted;
		obj.selected = selected;
		obj.setThematicData(getData());
		return obj;
	}

	/**
	* Does nothing; the type of DLinkObject is always Geometry.line.
	*/
	@Override
	public void setSpatialType(char objType) {
	}

	/**
	 * Checks if this aggregate link includes the trajectory with the given identifier
	 */
	public boolean includesTrajectory(String trId) {
		if (trId == null)
			return false;
		return souTrajIds != null && souTrajIds.contains(trId);
	}
}
