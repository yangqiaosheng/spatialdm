package spade.analysis.tools.moves;

import java.util.Vector;

import spade.time.TimeReference;
import spade.vis.database.SpatialEntity;
import spade.vis.geometry.GeoComp;
import spade.vis.geometry.RealPoint;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: May 11, 2009
 * Time: 4:00:51 PM
 * Contains computational utilities for trajectories.
 */
public class TrUtil {
	/**
	 * Smoothes the given trajectory
	 * @param track - the original trajectory
	 * @param rad - the radius of the smoothing circle. In case of geographical coordinates,
	 *   the radius is specified in metres.
	 * @param geoFactorX - the ratio between the horizontal distance in meters and
	 *   in degrees of longitude; equals 1 for non-geographical data
	 * @param geoFactorY - the ratio between the vertical distance in meters and
	 *   in degrees of latitude; equals 1 for non-geographical data
	 * @return the transformed trajectory
	 */
	public static Vector getSmoothTrajectory(Vector track, float rad, float geoFactorX, float geoFactorY) {
		if (track == null || track.size() < 3)
			return track;
		PointsInCircle cir = new PointsInCircle(rad, geoFactorX, geoFactorY);
		Vector<RealPoint> smTrackPoints = new Vector<RealPoint>(track.size(), 1);
		int lastAddedIdx = -1;
		for (int i = 0; i < track.size() && lastAddedIdx < track.size() - 1; i++) {
			cir.removeAllPoints();
			SpatialEntity spe1 = (SpatialEntity) track.elementAt(i);
			if (spe1 == null) {
				continue;
			}
			RealPoint p1 = spe1.getCentre();
			if (p1 == null) {
				continue;
			}
			p1.setIndex(i);
			cir.addPoint(p1);
			for (int j = i + 1; j < track.size(); j++) {
				SpatialEntity spe2 = (SpatialEntity) track.elementAt(j);
				if (spe2 == null) {
					continue;
				}
				RealPoint p2 = spe2.getCentre();
				if (p2 == null) {
					continue;
				}
				p2.setIndex(j);
				if (cir.fitsInCircle(p2, true)) {
					cir.addPoint(p2);
				} else {
					break;
				}
			}
			if (smTrackPoints.size() < 1) {
				smTrackPoints.addElement(p1);
			} else {
				RealPoint p = cir.getRepresentativePoint();
				if (p.getIndex() > smTrackPoints.elementAt(smTrackPoints.size() - 1).getIndex()) {
					smTrackPoints.addElement(p);
				}
			}
			lastAddedIdx = smTrackPoints.elementAt(smTrackPoints.size() - 1).getIndex();
		}
		Vector smTrack = new Vector(smTrackPoints.size(), 1);
		for (int i = 0; i < smTrackPoints.size(); i++) {
			int idx = smTrackPoints.elementAt(i).getIndex();
			smTrack.addElement(track.elementAt(idx));
		}
		return smTrack;
	}

	/**
	 * Generalizes the given trajectory
	 * @param track - the original trajectory
	 * @param minAngleRadian  the minimum angle of direction change, in radian,
	 *   for treating a point as significant
	 * @param minStopTime - minimal time interval to the next position treated as
	 *   significant stop
	 * @param minDist - the minimum distance between consecutive positions in the
	 *   resulting trajectory. In case of geographical coordinates,
	 *   the distance is specified in metres.
	 * @param maxDist - the maximum distance between consecutive positions in the
	 *   resulting trajectory. In case of geographical coordinates,
	 *   the distance is specified in metres.
	 * @param geoFactorX - the ratio between the horizontal distance in meters and
	 *   in degrees of longitude; equals 1 for non-geographical data
	 * @param geoFactorY - the ratio between the vertical distance in meters and
	 *   in degrees of latitude; equals 1 for non-geographical data
	 * @return the transformed trajectory
	 */
	public static Vector getCharacteristicPoints(Vector track, double minAngleRadian, long minStopTime, float minDist, float maxDist, float geoFactorX, float geoFactorY) {
		if (track == null || track.size() < 3)
			return track;
		//track=getSmoothTrajectory(track,minDist/2,geoFactorX,geoFactorY);
		float sqMinDist = minDist * minDist, sqMaxDist = maxDist * maxDist;
		double cosMinAngle = Math.cos(minAngleRadian);
		PointsInCircle cir = new PointsInCircle(minDist / 2, geoFactorX, geoFactorY);

		Vector trPoints = new Vector(track.size(), 1);
		SpatialEntity spe0 = (SpatialEntity) track.elementAt(0), //this will be the last added position
		speLast = (SpatialEntity) track.elementAt(track.size() - 1);
		if (spe0 == null || speLast == null)
			return null;
		RealPoint p0 = spe0.getCentre();
		trPoints.addElement(spe0);
		for (int i = 1; i < track.size() - 1; i++) {
			SpatialEntity spe1 = (SpatialEntity) track.elementAt(i);
			if (spe1 == null) {
				continue;
			}
			RealPoint p1 = spe1.getCentre();
			if (p1 == null) {
				continue;
			}
			p1.setIndex(i);
			boolean toAdd = false;
			float dx = (p1.x - p0.x) * geoFactorX, dy = (p1.y - p0.y) * geoFactorY;
			//should the point be added due to long distance from the previous point?
			toAdd = Math.abs(dx) >= maxDist || Math.abs(dy) >= maxDist;
			if (!toAdd) {
				float sqDist = dx * dx + dy * dy;
				toAdd = sqDist >= sqMaxDist;
			}
			if (!toAdd) {
				//should the point be added because this is a point of stop
				// or a turning point?
				cir.removeAllPoints(); //will group close points and return a representative point
				cir.addPoint(p1);
				int lastIndex = i;
				for (int j = i + 1; j < track.size(); j++) {
					SpatialEntity spe2 = (SpatialEntity) track.elementAt(j);
					if (spe2 == null) {
						continue;
					}
					RealPoint p2 = spe2.getCentre();
					if (p2 == null) {
						continue;
					}
					p2.setIndex(j);
					if (!cir.fitsInCircle(p2, true)) {
						break;
					}
					cir.addPoint(p2);
					lastIndex = j;
				}
				if (cir.getPointCount() > 1 && minStopTime > 0) {
					//There are several points in about the same place.
					//Is this a place of stop?
					TimeReference tr1 = spe1.getTimeReference();
					if (tr1 != null && tr1.getValidFrom() != null) {
						SpatialEntity spe2 = (SpatialEntity) track.elementAt(lastIndex);
						TimeReference tr2 = spe2.getTimeReference();
						if (tr2 != null && tr2.getValidFrom() != null) {
							//is this a point of stop?
							toAdd = tr2.getValidFrom().subtract(tr1.getValidFrom()) >= minStopTime;
						}
					}
				}
				if (!toAdd && lastIndex < track.size() - 1) {
					if (cir.getPointCount() > 2) {
						//take one representative point from a group of spatially close points
						p1 = cir.getRepresentativePoint();
						if (p1.getIndex() != i + 1) {
							spe1 = (SpatialEntity) track.elementAt(p1.getIndex());
							dx = (p1.x - p0.x) * geoFactorX;
							dy = (p1.y - p0.y) * geoFactorY;
						}
					}
					//should the point be added because this is a turning point?
					SpatialEntity spe2 = (SpatialEntity) track.elementAt(lastIndex + 1);
					RealPoint p2 = spe2.getCentre();
					float dx2 = (p2.x - p1.x) * geoFactorX;
					float dy2 = (p2.y - p1.y) * geoFactorY;
					double cos = GeoComp.getCosAngleBetweenVectors(dx, dy, dx2, dy2);
					toAdd = cos <= cosMinAngle;
				}
				i += cir.getPointCount() - 1; //to skip close points
			}
			if (toAdd) {
				trPoints.addElement(spe1);
				spe0 = spe1;
				p0 = p1;
			}
		}
		trPoints.addElement(speLast);
		return trPoints;
	}

	/**
	 * Extracts characteristic points from the given trajectory
	 * @param track - the original trajectory
	 * @param getStarts - whether to extract the starts
	 * @param getEnds - whether to extract the ends
	 * @param getStops - whether to extract the stops
	 * @param getTurns - whether to extract the turns
	 * @param minAngleRadian  the minimum angle of direction change, in radian,
	 *   for treating a point as significant
	 * @param minStopTime - minimal time interval to the next position treated as
	 *   significant stop
	 * @param minDist - the minimum distance between consecutive positions in the
	 *   resulting trajectory. In case of geographical coordinates,
	 *   the distance is specified in metres.
	 *   the distance is specified in metres.
	 * @param geoFactorX - the ratio between the horizontal distance in meters and
	 *   in degrees of longitude; equals 1 for non-geographical data
	 * @param geoFactorY - the ratio between the vertical distance in meters and
	 *   in degrees of latitude; equals 1 for non-geographical data
	 * @return vector of extracted points
	 */
	public static Vector getCharacteristicPoints(Vector track, boolean getStarts, boolean getEnds, boolean getStops, boolean getTurns, double minAngleRadian, long minStopTime, float minDist, float geoFactorX, float geoFactorY) {
		if (track == null || track.size() < 1)
			return null;
		double cosMinAngle = Math.cos(minAngleRadian);
		PointsInCircle cir = new PointsInCircle(minDist / 2, geoFactorX, geoFactorY);

		Vector trPoints = new Vector(track.size(), 1);
		SpatialEntity spe0 = (SpatialEntity) track.elementAt(0), speLast = (SpatialEntity) track.elementAt(track.size() - 1);
		if (spe0 == null || speLast == null)
			return null;
		RealPoint p0 = spe0.getCentre();
		if (getStarts) {
			trPoints.addElement(spe0);
		}
		if (getStops || getTurns) {
			for (int i = 1; i < track.size() - 1; i++) {
				SpatialEntity spe1 = (SpatialEntity) track.elementAt(i);
				if (spe1 == null) {
					continue;
				}
				RealPoint p1 = spe1.getCentre();
				if (p1 == null) {
					continue;
				}
				p1.setIndex(i);
				boolean toAdd = false;
				float dx = (p1.x - p0.x) * geoFactorX, dy = (p1.y - p0.y) * geoFactorY;
				//should the point be added because this is a point of stop
				// or a turning point?
				cir.removeAllPoints(); //will group close points and return a representative point
				cir.addPoint(p1);
				int lastIndex = i;
				for (int j = i + 1; j < track.size(); j++) {
					SpatialEntity spe2 = (SpatialEntity) track.elementAt(j);
					if (spe2 == null) {
						continue;
					}
					RealPoint p2 = spe2.getCentre();
					if (p2 == null) {
						continue;
					}
					p2.setIndex(j);
					if (!cir.fitsInCircle(p2, true)) {
						break;
					}
					cir.addPoint(p2);
					lastIndex = j;
				}
				if (getStops && cir.getPointCount() > 1 && minStopTime > 0) {
					//There are several points in about the same place.
					//Is this a place of stop?
					TimeReference tr1 = spe1.getTimeReference();
					if (tr1 != null && tr1.getValidFrom() != null) {
						SpatialEntity spe2 = (SpatialEntity) track.elementAt(lastIndex);
						TimeReference tr2 = spe2.getTimeReference();
						if (tr2 != null && tr2.getValidFrom() != null) {
							//is this a point of stop?
							toAdd = tr2.getValidFrom().subtract(tr1.getValidFrom()) >= minStopTime;
						}
					}
				}
				if (!toAdd && getTurns && lastIndex < track.size() - 1) {
					if (cir.getPointCount() > 2) {
						//take one representative point from a group of spatially close points
						p1 = cir.getRepresentativePoint();
						if (p1.getIndex() != i + 1) {
							spe1 = (SpatialEntity) track.elementAt(p1.getIndex());
							dx = (p1.x - p0.x) * geoFactorX;
							dy = (p1.y - p0.y) * geoFactorY;
						}
					}
					//should the point be added because this is a turning point?
					SpatialEntity spe2 = (SpatialEntity) track.elementAt(lastIndex + 1);
					RealPoint p2 = spe2.getCentre();
					float dx2 = (p2.x - p1.x) * geoFactorX;
					float dy2 = (p2.y - p1.y) * geoFactorY;
					double cos = GeoComp.getCosAngleBetweenVectors(dx, dy, dx2, dy2);
					toAdd = cos <= cosMinAngle;
				}
				i += cir.getPointCount() - 1; //to skip close points
				if (toAdd) {
					trPoints.addElement(spe1);
					spe0 = spe1;
					p0 = p1;
				}
			}
		}
		if (getEnds) {
			trPoints.addElement(speLast);
		}
		if (trPoints.size() < 1)
			return null;
		return trPoints;
	}
}
