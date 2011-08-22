package spade.vis.dmap;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.ImageObserver;
import java.util.Vector;

import spade.analysis.tools.moves.DPlaceVisitsCounter;
import spade.lib.util.DoubleArray;
import spade.lib.util.FloatArray;
import spade.lib.util.IntArray;
import spade.lib.util.NumValManager;
import spade.lib.util.ObjectWithCount;
import spade.time.TimeMoment;
import spade.time.TimeReference;
import spade.vis.database.SpatialDataItem;
import spade.vis.database.SpatialEntity;
import spade.vis.geometry.Computing;
import spade.vis.geometry.GeoComp;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.RealPoint;
import spade.vis.map.MapContext;
import spade.vis.space.GeoObject;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: 21-Aug-2007
 * Time: 10:27:37
 * Represents a generalised place visited by one or more trajectories.
 */
public class DPlaceVisitsObject extends DPlaceVisitsCounter {
	/**
	 * Information about all visits of this place. The elements of
	 * the vector are instances of PlaceVisitInfo.
	 */
	public Vector visits = null;
	/**
	 * this array will contain data about the distortions of the
	 * trajectories in the area
	 */
	public int distoCounts[] = null;
	/**
	 * The maximum possible distortion
	 */
	public float maxPossibleDistortion = Float.NaN;
	/**
	 * The maximum distortion in this place
	 */
	public float maxDistortion = 0;
	/**
	 * The total distortion in this place (sum of all distortions)
	 */
	public float sumDistortion = 0;
	/**
	 * The mean distortion in this place
	 */
	public float meanDistortion = 0;
	/**
	 * Indicates whether this object must draw the internal trajectory segments
	 * fitting in this place
	 */
	public boolean drawInternalTracks = false;

	/**
	 * Adds information about a visit of the place.
	 * @param trId - the identifier of the trajectory entering the place
	 * @param track - the sequence of trajectory positions,
	 *                time-referenced instances of SpatialEntity
	 * @param startIdx - the index of the first position inside the place
	 * @param geographic - indicates whether the coordinates are geographic
	 * @return the index of the last position inside the place
	 *         or -1 in case of error
	 */
	@Override
	public int addVisit(String trId, Vector track, int startIdx, boolean geographic) {
		if (trId == null || track == null || startIdx < 0 || startIdx >= track.size())
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
		PlaceVisitInfo vin = new PlaceVisitInfo();
		vin.placeId = getIdentifier();
		vin.placeGeometry = geom;
		vin.trId = trId;
		vin.track = track;
		vin.firstIdx = startIdx;
		vin.lastIdx = startIdx;
		vin.isStart = startIdx == 0;
		TimeReference tref = spe.getTimeReference();
		if (tref != null) {
			vin.enterTime = tref.getValidFrom();
			vin.exitTime = tref.getValidUntil();
			if (vin.exitTime == null) {
				vin.exitTime = vin.enterTime;
			}
		}
		for (int i = startIdx + 1; i < track.size(); i++) {
			spe = (SpatialEntity) track.elementAt(i);
			RealPoint p = spe.getCentre();
			if (!geom.contains(p.x, p.y, 0f, true)) {
				break;
			}
			vin.lastIdx = i;
			vin.len += GeoComp.distance(p.x, p.y, p0.x, p0.y, geographic);
			tref = spe.getTimeReference();
			if (tref != null) {
				TimeMoment t = tref.getValidFrom();
				if (t != null && vin.exitTime != null) {
					long gap = t.subtract(vin.exitTime);
					if (gap > vin.maxTimeGap) {
						vin.maxTimeGap = gap;
					}
				}
				vin.exitTime = tref.getValidUntil();
				if (vin.exitTime == null) {
					vin.exitTime = t;
				}
			}
			p0 = p;
		}
		vin.isFinal = vin.lastIdx == track.size() - 1;
		if (!vin.isFinal) { //add the time to the next position
			spe = (SpatialEntity) track.elementAt(vin.lastIdx + 1);
			tref = spe.getTimeReference();
			if (tref != null) {
				TimeMoment t = tref.getValidFrom();
				if (t != null && vin.exitTime != null) {
					long gap = t.subtract(vin.exitTime);
					if (gap > vin.maxTimeGap) {
						vin.maxTimeGap = gap;
					}
				}
				vin.exitTime = tref.getValidUntil();
				if (vin.exitTime == null) {
					vin.exitTime = t;
				}
			}
		}
		if (vin.enterTime != null && vin.exitTime != null) {
			vin.stayDuration = vin.exitTime.subtract(vin.enterTime);
		}
		if (vin.stayDuration > 0) {
			vin.speed = vin.len / vin.stayDuration;
		}
		if (!vin.isStart && !vin.isFinal) {
			p0 = ((SpatialEntity) track.elementAt(vin.firstIdx - 1)).getCentre();
			RealPoint p1 = ((SpatialEntity) track.elementAt(vin.lastIdx + 1)).getCentre();
			double minCosAngle = 1.0;
			for (int i = vin.firstIdx; i <= vin.lastIdx; i++) {
				RealPoint p = ((SpatialEntity) track.elementAt(i)).getCentre();
				double cosAngle = GeoComp.getCosAngleBetweenVectors(p.x - p0.x, p.y - p0.y, p1.x - p.x, p1.y - p.y);
				if (cosAngle < minCosAngle) {
					minCosAngle = cosAngle;
				}
			}
			vin.angleDirChange = (int) Math.round(GeoComp.getAngleInDegrees(Math.acos(minCosAngle)));
		}
		if (visits == null) {
			visits = new Vector(100, 100);
		}
		visits.addElement(vin);
		return vin.lastIdx;
	}

	/**
	 * Adds information about crossing of the place by a segment of the trajectory.
	 * @param trId - the identifier of the trajectory crossing the place
	 * @param track - the sequence of trajectory positions,
	 *                time-referenced instances of SpatialEntity
	 * @param idxBefore - the index of the last trajectory position before crossing the place
	 * @param time - the approximate time moment when the trajectory crosses the place
	 * @param geographic - indicates whether the coordinates are geographic
	 */
	public void addCross(String trId, Vector track, int idxBefore, TimeMoment time, boolean geographic) {
		if (trId == null || track == null || idxBefore < 0 || idxBefore >= track.size())
			return;
		Geometry geom = getGeometry();
		if (geom == null)
			return;
		isGeo = geographic;
		PlaceVisitInfo vin = new PlaceVisitInfo();
		vin.justCrossed = true;
		vin.placeId = getIdentifier();
		vin.placeGeometry = geom;
		vin.trId = trId;
		vin.track = track;
		vin.firstIdx = idxBefore;
		vin.lastIdx = idxBefore;
		vin.isStart = idxBefore == 0;
		vin.enterTime = time;
		vin.exitTime = time;
		if (visits == null) {
			visits = new Vector(100, 100);
		}
		visits.addElement(vin);
	}

	public void addTrackSegment(DMovingObject mobj, int startIdx, int endIdx, boolean geographic) {
		if (mobj == null || mobj.getTrack() == null || startIdx < 0 || startIdx > endIdx)
			return;
		Vector track = mobj.getTrack();
		if (startIdx >= track.size())
			return;
		if (endIdx >= track.size()) {
			endIdx = track.size();
		}
		SpatialEntity spe = (SpatialEntity) track.elementAt(startIdx);
		RealPoint p0 = spe.getCentre();
		if (p0 == null)
			return;
		isGeo = geographic;
		PlaceVisitInfo vin = new PlaceVisitInfo();
		vin.placeId = getIdentifier();
		vin.placeGeometry = getGeometry();
		vin.trId = mobj.getIdentifier();
		vin.trObj = mobj;
		vin.track = track;
		vin.firstIdx = startIdx;
		vin.lastIdx = endIdx;
		vin.isStart = startIdx == 0;
		TimeReference tref = spe.getTimeReference();
		if (tref != null) {
			vin.enterTime = tref.getValidFrom();
		}
		if (endIdx > startIdx) {
			spe = (SpatialEntity) track.elementAt(endIdx);
			tref = spe.getTimeReference();
		}
		if (tref != null) {
			vin.exitTime = tref.getValidUntil();
			if (vin.exitTime == null) {
				vin.exitTime = tref.getValidFrom();
			}
		}

		for (int i = startIdx + 1; i <= endIdx; i++) {
			spe = (SpatialEntity) track.elementAt(i);
			RealPoint p = spe.getCentre();
			if (p == null) {
				continue;
			}
			vin.len += GeoComp.distance(p.x, p.y, p0.x, p0.y, geographic);
			tref = spe.getTimeReference();
			if (tref != null) {
				TimeMoment t = tref.getValidFrom();
				if (t != null && vin.exitTime != null) {
					long gap = t.subtract(vin.exitTime);
					if (gap > vin.maxTimeGap) {
						vin.maxTimeGap = gap;
					}
				}
			}
			p0 = p;
		}

		vin.isFinal = vin.lastIdx == track.size() - 1;
		if (!vin.isFinal) { //add the time to the next position
			spe = (SpatialEntity) track.elementAt(vin.lastIdx + 1);
			tref = spe.getTimeReference();
			if (tref != null) {
				TimeMoment t = tref.getValidFrom();
				if (t != null && vin.exitTime != null) {
					long gap = t.subtract(vin.exitTime);
					if (gap > vin.maxTimeGap) {
						vin.maxTimeGap = gap;
					}
				}
				vin.exitTime = tref.getValidUntil();
				if (vin.exitTime == null) {
					vin.exitTime = t;
				}
			}
		}
		if (vin.enterTime != null && vin.exitTime != null) {
			vin.stayDuration = vin.exitTime.subtract(vin.enterTime);
		}
		if (vin.stayDuration > 0) {
			vin.speed = vin.len / vin.stayDuration;
		}
		if (!vin.isStart && !vin.isFinal) {
			p0 = ((SpatialEntity) track.elementAt(vin.firstIdx - 1)).getCentre();
			RealPoint p1 = ((SpatialEntity) track.elementAt(vin.lastIdx + 1)).getCentre();
			double minCosAngle = 1.0;
			for (int i = vin.firstIdx; i <= vin.lastIdx; i++) {
				RealPoint p = ((SpatialEntity) track.elementAt(i)).getCentre();
				double cosAngle = GeoComp.getCosAngleBetweenVectors(p.x - p0.x, p.y - p0.y, p1.x - p.x, p1.y - p.y);
				if (cosAngle < minCosAngle) {
					minCosAngle = cosAngle;
				}
			}
			vin.angleDirChange = (int) Math.round(GeoComp.getAngleInDegrees(Math.acos(minCosAngle)));
		}
		if (visits == null) {
			visits = new Vector(100, 100);
		}
		visits.addElement(vin);
		return;
	}

	/**
	 * Finds the index of the last position inside the place starting from the
	 * given index startIdx.
	 * @param track - the sequence of trajectory positions,
	 *                time-referenced instances of SpatialEntity
	 * @param startIdx - the index of the first position inside the place
	 * @return the index of the last position inside the place
	 *         or -1 in case of error
	 */
	public int getLastPosInsidePlace(Vector track, int startIdx) {
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
		for (int i = startIdx + 1; i < track.size(); i++) {
			spe = (SpatialEntity) track.elementAt(i);
			RealPoint p = spe.getCentre();
			if (!geom.contains(p.x, p.y, 0f, true))
				return i - 1;
		}
		return track.size() - 1;
	}

	public int getNVisits() {
		if (visits == null)
			return 0;
		return visits.size();
	}

	public int getNTrueVisits() {
		if (visits == null)
			return 0;
		int n = 0;
		for (int i = 0; i < visits.size(); i++) {
			PlaceVisitInfo vin = (PlaceVisitInfo) visits.elementAt(i);
			if (!vin.justCrossed) {
				++n;
			}
		}
		return n;
	}

	/**
	 * Computes the statistics of visiting this place
	 * @param activeTrajIds - the identifiers of the active trajectories. If
	 *                        not null or empty, only the active trajectories
	 *                        are taken into account
	 * @param timeLimits - if specified, only the visits fitting within
	 *                     these time limits, at least partly, are taken into account
	 * @return the statistics computed
	 */
	public PlaceVisitsStatistics getPlaceVisitsStatistics(Vector activeTrajIds, TimeReference timeLimits) {
		PlaceVisitsStatistics stat = new PlaceVisitsStatistics();
		stat.placeId = getIdentifier();
		if (visits == null || visits.size() < 1)
			return stat;
		if (activeTrajIds != null && activeTrajIds.size() < 1) {
			activeTrajIds = null;
		}
		if (timeLimits != null && (timeLimits.getValidFrom() == null && timeLimits.getValidUntil() != null)) {
			timeLimits = null;
		}
		DoubleArray stays = new DoubleArray(visits.size(), 1), timeGaps = new DoubleArray(visits.size(), 1), lengths = new DoubleArray(visits.size(), 1), speeds = new DoubleArray(visits.size(), 1);
		FloatArray angles = new FloatArray(visits.size(), 1);
		Vector trIds = new Vector(visits.size(), 1);
		IntArray nVisitsPerTraj = new IntArray(visits.size(), 1);
		for (int i = 0; i < visits.size(); i++) {
			PlaceVisitInfo vin = (PlaceVisitInfo) visits.elementAt(i);
			if (activeTrajIds != null && !activeTrajIds.contains(vin.trId)) {
				continue;
			}
			if (timeLimits != null && !timeLimits.isValid(vin.enterTime, vin.exitTime)) {
				continue;
			}
			++stat.nVisits;
			int trIdx = trIds.indexOf(vin.trId);
			if (trIdx >= 0) {
				int nvis = nVisitsPerTraj.elementAt(trIdx) + 1;
				nVisitsPerTraj.setElementAt(nvis, trIdx);
				if (nvis > stat.maxNRepeatedVisits) {
					stat.maxNRepeatedVisits = nvis;
				}
			} else {
				trIds.addElement(vin.trId);
				nVisitsPerTraj.addElement(0);
				++stat.nTrajectories;
			}
			if (vin.isStart) {
				++stat.nStarts;
			}
			if (vin.isFinal) {
				++stat.nEnds;
			}
			if (vin.enterTime != null && (stat.firstEnterTime == null || vin.enterTime.compareTo(stat.firstEnterTime) < 0)) {
				stat.firstEnterTime = vin.enterTime;
			}
			if (vin.exitTime != null && (stat.lastExitTime == null || vin.exitTime.compareTo(stat.lastExitTime) > 0)) {
				stat.lastExitTime = vin.exitTime;
			}
			TimeMoment t1 = vin.enterTime, t2 = vin.exitTime;
			long stayDuration = vin.stayDuration;
			double len = vin.len;
			long maxTimeGap = vin.maxTimeGap;
			if (timeLimits != null) {
				boolean otherTime = false;
				if (t1 == null || t1.compareTo(timeLimits.getValidFrom()) < 0) {
					t1 = timeLimits.getValidFrom();
					otherTime = true;
				}
				if (t2 == null || t2.compareTo(timeLimits.getValidUntil()) > 0) {
					t2 = timeLimits.getValidUntil();
					otherTime = true;
				}
				if (otherTime && t1 != null && t2 != null) {
					stayDuration = t2.subtract(t1);
					len = 0;
					maxTimeGap = 0;
					RealPoint p0 = null;
					TimeMoment tPrev = null;
					for (int j = vin.firstIdx; j <= vin.lastIdx; j++) {
						SpatialEntity spe = (SpatialEntity) vin.track.elementAt(j);
						TimeReference tr = spe.getTimeReference();
						if (tr == null || !tr.isValid(t1, t2)) {
							continue;
						}
						RealPoint p = spe.getCentre();
						if (p0 != null) {
							len += GeoComp.distance(p.x, p.y, p0.x, p0.y, isGeo);
							long gap = tr.getValidFrom().subtract(tPrev);
							if (gap > maxTimeGap) {
								maxTimeGap = gap;
							}
						}
						p0 = p;
						tPrev = tr.getValidUntil();
					}
				}
			}
			if (stat.nVisits == 1) {
				stat.minStayDuration = stat.maxStayDuration = stat.averStayDuration = stat.medianStayDuration = stayDuration;
				stat.minTimeGap = stat.maxTimeGap = stat.averTimeGap = stat.medianTimeGap = maxTimeGap;
				stat.minLen = stat.maxLen = stat.averLen = stat.medianLen = len;
				stat.minSpeed = stat.maxSpeed = stat.averSpeed = stat.medianSpeed = vin.speed;
				stat.minAngleDirChange = stat.maxAngleDirChange = stat.medianAngleDirChange = vin.angleDirChange;
			} else {
				if (stat.minStayDuration > stayDuration) {
					stat.minStayDuration = stayDuration;
				} else if (stat.maxStayDuration < stayDuration) {
					stat.maxStayDuration = stayDuration;
				}
				if (stat.minTimeGap > vin.maxTimeGap) {
					stat.minTimeGap = maxTimeGap;
				} else if (stat.maxTimeGap < vin.maxTimeGap) {
					stat.maxTimeGap = maxTimeGap;
				}
				if (stat.minLen > vin.len) {
					stat.minLen = len;
				} else if (stat.maxLen < vin.len) {
					stat.maxLen = len;
				}
				if (stat.minSpeed > vin.speed) {
					stat.minSpeed = vin.speed;
				} else if (stat.maxSpeed < vin.speed) {
					stat.maxSpeed = vin.speed;
				}
				if (stat.minAngleDirChange > vin.angleDirChange) {
					stat.minAngleDirChange = vin.angleDirChange;
				} else if (stat.maxAngleDirChange < vin.angleDirChange) {
					stat.maxAngleDirChange = vin.angleDirChange;
				}
			}
			stat.totalStayDuration += stayDuration;
			stat.totalLenInside += len;
			stays.addElement(stayDuration);
			timeGaps.addElement(maxTimeGap);
			lengths.addElement(len);
			speeds.addElement(vin.speed);
			angles.addElement(vin.angleDirChange);
		}
		if (stat.totalStayDuration > 0) {
			stat.averSpeed = stat.totalLenInside / stat.totalStayDuration;
		}
		if (stat.nVisits < 2)
			return stat;
		if (stat.nVisits == 2) {
			stat.averStayDuration = stat.medianStayDuration = (stat.minStayDuration + stat.maxStayDuration) / 2;
			stat.averTimeGap = stat.medianTimeGap = (stat.minTimeGap + stat.maxTimeGap) / 2;
			stat.averLen = stat.medianLen = (stat.minLen + stat.maxLen) / 2;
			stat.medianSpeed = (stat.minSpeed + stat.maxSpeed) / 2;
			stat.medianAngleDirChange = (stat.minAngleDirChange + stat.maxAngleDirChange) / 2;
			return stat;
		}
		stat.medianStayDuration = Math.round(NumValManager.getMedian(stays));
		stat.medianTimeGap = Math.round(NumValManager.getMedian(timeGaps));
		stat.medianLen = NumValManager.getMedian(lengths);
		stat.medianSpeed = NumValManager.getMedian(speeds);
		stat.averStayDuration = Math.round(NumValManager.getMean(stays));
		stat.averTimeGap = Math.round(NumValManager.getMean(timeGaps));
		stat.averLen = NumValManager.getMean(lengths);
		stat.medianAngleDirChange = Math.round(NumValManager.getMedian(angles));
		return stat;
	}

	/**
	* Returns a copy of this object. The spatial and thematic data items
	* used by this objects are not duplicated; the copy will use references to the
	* same data items
	*/
	@Override
	public GeoObject makeCopy() {
		DPlaceVisitsObject obj = new DPlaceVisitsObject();
		if (data != null) {
			obj.setup((SpatialDataItem) data.clone());
		}
		if (visits != null) {
			obj.visits = (Vector) visits.clone();
		}
		if (links != null) {
			obj.links = (Vector<ObjectWithCount>) links.clone();
		}
		obj.label = label;
		obj.highlighted = highlighted;
		obj.selected = selected;
		return obj;
	}

	@Override
	public void drawGeometry(Geometry geom, Graphics g, MapContext mc, Color borderColor, Color fillColor, int width, ImageObserver observer) {
		if (g == null)
			return;
		super.drawGeometry(geom, g, mc, borderColor, fillColor, width, observer);
		if (!drawInternalTracks || visits == null || visits.size() < 1)
			return;
		for (int i = 0; i < visits.size(); i++) {
			PlaceVisitInfo pvInfo = (PlaceVisitInfo) visits.elementAt(i);
			if (pvInfo.trObj != null) {
				pvInfo.trObj.drawSegment(g, mc, pvInfo.firstIdx, pvInfo.lastIdx);
			}
		}
	}

	/**
	 * Computes the distortions, i.e. the distances of the trajectories to
	 * the centroid of this place (the closest trajectory points are taken)
	 */
	public void computeDistortions() {
		if (visits == null || visits.size() < 1)
			return;
		if (maxDistortion > 0)
			return; //already computed
		Geometry geom = getGeometry();
		if (geom == null)
			return;
		float centre[] = geom.getCentroid();
		int nVisits = 0;
		for (int n = 0; n < visits.size(); n++) {
			PlaceVisitInfo vin = (PlaceVisitInfo) visits.elementAt(n);
			if (vin.track == null) {
				continue;
			}
			++nVisits;
			if (Double.isNaN(vin.dCen) || vin.pCen == null)
				if (vin.justCrossed) {
					SpatialEntity spe = (SpatialEntity) vin.track.elementAt(vin.firstIdx);
					RealPoint p0 = spe.getCentre();
					if (vin.firstIdx < vin.track.size() - 1) {
						spe = (SpatialEntity) vin.track.elementAt(vin.firstIdx + 1);
					}
					RealPoint p1 = spe.getCentre();
					vin.dCen = Computing.distance(p0.x, p0.y, p1.x, p1.y, centre[0], centre[1], isGeo);
					vin.pCen = Computing.closestPoint(p0.x, p0.y, p1.x, p1.y, centre[0], centre[1]);
				} else {
					int i1 = vin.firstIdx - 1, i2 = vin.lastIdx + 1;
					if (i1 < 0) {
						i1 = 0;
					}
					if (i2 >= vin.track.size()) {
						i2 = vin.track.size() - 1;
					}
					for (int i = i1; i < i2; i++) {
						SpatialEntity spe = (SpatialEntity) vin.track.elementAt(i);
						RealPoint p0 = spe.getCentre();
						spe = (SpatialEntity) vin.track.elementAt(i + 1);
						RealPoint p1 = spe.getCentre();
						double dist = Computing.distance(p0.x, p0.y, p1.x, p1.y, centre[0], centre[1], isGeo);
						if (Double.isNaN(vin.dCen) || vin.dCen > dist) {
							vin.dCen = dist;
							vin.pCen = Computing.closestPoint(p0.x, p0.y, p1.x, p1.y, centre[0], centre[1]);
						}
					}
				}
			if (vin.dCen > maxDistortion) {
				maxDistortion = (float) vin.dCen;
			}
			sumDistortion += (float) vin.dCen;
		}
		if (nVisits > 0) {
			meanDistortion = sumDistortion / nVisits;
		}
	}

	/**
	 * Produces statistics about the distortions by intervals
	 */
	public void countDistortionsByIntervals(float maxPossibleDistortion, int nIntervals) {
		this.maxPossibleDistortion = maxPossibleDistortion;
		distoCounts = new int[nIntervals];
		for (int i = 0; i < nIntervals; i++) {
			distoCounts[i] = 0;
		}
		if (visits == null || visits.size() < 1)
			return;
		if (maxDistortion <= 0) {
			computeDistortions();
		}
		if (maxDistortion <= 0)
			return; //no distortions
		for (int n = 0; n < visits.size(); n++) {
			PlaceVisitInfo vin = (PlaceVisitInfo) visits.elementAt(n);
			if (Double.isNaN(vin.dCen)) {
				continue;
			}
			int distIdx = (int) Math.floor(vin.dCen * distoCounts.length / maxPossibleDistortion);
			if (distIdx >= distoCounts.length) {
				distIdx = distoCounts.length - 1;
			}
			++distoCounts[distIdx];
		}
	}

	/**
	 * Checks if the trajectory with the given identifier has visited this place
	 */
	public boolean wasVisitedBy(String trId) {
		if (trId == null)
			return false;
		if (visits == null || visits.size() < 1)
			return false;
		for (int k = 0; k < visits.size(); k++) {
			PlaceVisitInfo pvInfo = (PlaceVisitInfo) visits.elementAt(k);
			if (trId.equals(pvInfo.trId))
				return true;
		}
		return false;
	}
}
