package spade.analysis.tools.moves;

import java.util.Vector;

import spade.lib.util.ObjectWithCount;
import spade.time.TimeMoment;
import spade.vis.database.SpatialEntity;
import spade.vis.dmap.DGeoObject;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.RealPoint;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Mar 12, 2009
 * Time: 2:08:11 PM
 * Represents a generalised place visited by one or more trajectories.
 * Unlike DPlaceVisitsObject, does not keep information about the trajectories
 * and individual visits but just counts the visits.
 */
public class DPlaceVisitsCounter extends DGeoObject {
	/**
	 * The number of "visits" of this place (i.e. at least one trajectory point is inside)
	 */
	public int nVisits = 0;
	/**
	 * The number of trajectories starting in this place
	 */
	public int nStarts = 0;
	/**
	 * The number of trajectories ending in this place
	 */
	public int nEnds = 0;
	/**
	 * The number of crosses of this place by trajectory segments (i.e. no explicit points inside)
	 */
	public int nCrosses = 0;
	/**
	 * The links from/to this place, i.e. there are trajectories going from
	 * one place to another
	 */
	public Vector<ObjectWithCount> links = null;

	/**
	 * Counts the visit of the place.
	 * @param trId - the identifier of the trajectory entering the place
	 *   - ignored!
	 * @param track - the sequence of trajectory positions,
	 *                time-referenced instances of SpatialEntity
	 * @param startIdx - the index of the first position inside the place
	 * @param geographic - indicates whether the coordinates are geographic
	 * @return the index of the last position inside the place
	 *         or -1 in case of error
	 */
	public int addVisit(String trId, Vector track, int startIdx, boolean geographic) {
		return addVisit(track, startIdx, geographic);
	}

	/**
	 * Counts a visit of the place.
	 * @param track - the sequence of trajectory positions,
	 *                time-referenced instances of SpatialEntity
	 * @param startIdx - the index of the first position inside the place
	 * @param geographic - indicates whether the coordinates are geographic
	 * @return the index of the last position inside the place
	 *         or -1 in case of error
	 */
	public int addVisit(Vector track, int startIdx, boolean geographic) {
		if (track == null || startIdx < 0 || startIdx >= track.size())
			return -1;
		Geometry geom = getGeometry();
		if (geom == null)
			return -1;
		SpatialEntity spe = (SpatialEntity) track.elementAt(startIdx);
		RealPoint p0 = spe.getCentre();
		if (p0 == null)
			return -1;
		if (!geom.contains(p0.x, p0.y, 0f, true))
			return -1;
		isGeo = geographic;
		int lastIdx = startIdx;
		for (int i = startIdx + 1; i < track.size(); i++) {
			spe = (SpatialEntity) track.elementAt(i);
			RealPoint p = spe.getCentre();
			if (!geom.contains(p.x, p.y, 0f, true)) {
				break;
			}
			lastIdx = i;
			p0 = p;
		}
		++nVisits;
		if (startIdx == 0) {
			++nStarts;
		}
		if (lastIdx >= track.size() - 1) {
			++nEnds;
		}
		return lastIdx;
	}

	/**
	 * Counts a crossing of the place by a segment of the trajectory.
	 * @param time - the approximate time moment when the trajectory crosses the place
	 *               (not used now, reserved for the future - summarisation by time intervals)
	 */
	public void addCross(TimeMoment time) {
		++nCrosses;
	}

	/**
	* Checks if the point (x,y) belongs to this place, i.e. is inside or on the boundary
	*/
	public boolean contains(float x, float y) {
		Geometry geom = getGeometry();
		if (geom == null)
			return false;
		return geom.contains(x, y, 0, true);
	}

	public boolean hasVisitsOrCrosses() {
		return nVisits > 0 || nCrosses > 0;
	}

	/**
	 * Adds information about a link to a neighbouring place, i.e.
	 * some trajectory goes from one place to the other without
	 * entering any intermediate places.
	 */
	public void addLinkToPlace(DPlaceVisitsCounter place) {
		if (place == null)
			return;
		String id = place.getIdentifier();
		ObjectWithCount oc = null;
		if (links != null) {
			for (int i = 0; i < links.size() && oc == null; i++)
				if (links.elementAt(i).hasObject(id)) {
					oc = links.elementAt(i);
				}
		}
		if (oc == null) {
			oc = new ObjectWithCount(id);
			if (links == null) {
				links = new Vector<ObjectWithCount>(10, 10);
			}
			links.addElement(oc);
		}
		oc.add();
	}
}
