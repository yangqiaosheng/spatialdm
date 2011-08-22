package spade.vis.dmap;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.ImageObserver;
import java.util.Vector;

import spade.lib.basicwin.Drawing;
import spade.lib.util.FloatArray;
import spade.lib.util.NumValManager;
import spade.time.Date;
import spade.time.TimeMoment;
import spade.time.TimeReference;
import spade.vis.database.SpatialEntity;
import spade.vis.database.ThematicDataItem;
import spade.vis.geometry.Computing;
import spade.vis.geometry.GeoComp;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.MovingPointSign;
import spade.vis.geometry.RealLine;
import spade.vis.geometry.RealPoint;
import spade.vis.geometry.RealPolyline;
import spade.vis.geometry.RealRectangle;
import spade.vis.map.MapContext;
import spade.vis.space.GeoObject;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 17-Apr-2007
 * Time: 13:00:47
 * Represents an object changing its geographical position over time
 */
public class DMovingObject extends DGeoObject {
	/**
	 * A sequence of instances of SpatialEntity representing different positions
	 * of this object corresponding to consecutive time moments or intervals.
	 */
	protected Vector track = null;
	/**
	 * Generalised track with a reduced number of positions (the positions of the
	 * stops and turns are preserved)
	 */
	protected Vector mainPositions = null;
	/**
	 * List of visited areas, filled by the TraejctoryGeneraliser
	 */
	public String listOfVisitedAreas = null;
	/**
	 * Number of visited areas, filled by the TraejctoryGeneraliser
	 */
	public int nVisitedAreas = 0;
	/**
	 * The start and end time of the movement
	 */
	protected TimeMoment startTime = null, endTime = null;
	/**
	 * The identifier of the moving entity
	 */
	protected String entityId = null;
	/**
	 * The index of the first and last positions in the trajectory satisfying
	 * the current temporal filter.
	 */
	protected int firstIdx = -1, lastIdx = -1;
	/**
	 * The distances between consecutive points
	 */
	protected double distances[] = null;
	/**
	 * Combiner for segment filters
	 */
	protected DMovingObjectSegmFilterCombiner segmFilterCombiner = null;

	public DMovingObjectSegmFilterCombiner getSegmFilterCombiner() {
		return segmFilterCombiner;
	}

	public boolean hasSegmFilter() {
		return segmFilterCombiner != null;
	}

	public boolean[] getSegmFilterBitmap(Object owner) {
		return (segmFilterCombiner == null) ? null : segmFilterCombiner.getBitmap(owner);
	}

	public void setSegmFilterBitmap(Object owner, int whatIsFiltered, boolean bitmap[]) {
		if (segmFilterCombiner == null) {
			segmFilterCombiner = new DMovingObjectSegmFilterCombiner(this);
		}
		segmFilterCombiner.addFilter(owner, whatIsFiltered, bitmap);
	}

	public void clearFilter(Object owner) {
		if (segmFilterCombiner != null) {
			segmFilterCombiner.clearFilter(owner);
		}
	}

	public boolean isSegmActive(int idxSegm) {
		if (segmFilterCombiner == null)
			return true;
		else
			return segmFilterCombiner.isSegmentActive(idxSegm);
	}

	public boolean isAnySegmActive() {
		if (segmFilterCombiner == null)
			return true;
		else
			return segmFilterCombiner.isAnySegmentActive();
	}

	public boolean areAllSegmentsActive() {
		if (segmFilterCombiner == null)
			return true;
		else
			return segmFilterCombiner.areAllSegmentsActive();
	}

	/**
	* Checks if the point (x,y) belongs to the object. The point, in particular,
	* may represent the position of the mouse in the map.
	*/
	@Override
	public boolean contains(float x, float y, float tolerateDist) {
		if (segmFilterCombiner == null)
			return super.contains(x, y, tolerateDist);
		if (!isAnySegmActive())
			return false;
		for (int i = 0; i < track.size() - 1; i++)
			if (isSegmActive(i)) {
				RealLine line = new RealLine();
				line.x1 = ((SpatialEntity) track.elementAt(i)).getCentre().getX();
				line.y1 = ((SpatialEntity) track.elementAt(i)).getCentre().getY();
				line.x2 = ((SpatialEntity) track.elementAt(i + 1)).getCentre().getX();
				line.y2 = ((SpatialEntity) track.elementAt(i + 1)).getCentre().getY();
				if (line.contains(x, y, tolerateDist, false))
					return true;
			}
		return false;
	}

	/**
	 * Returns the index of the closest point to the given point within the given time interval
	 * taking into account current segment filter
	 */
	public int getClosestPointTo(float x, float y, TimeMoment t1, TimeMoment t2) {
		if (track == null || track.size() < 1)
			return -1;
		if (!isAnySegmActive())
			return -1;
		if (t1 == null) {
			t1 = startTime;
		}
		if (t2 == null) {
			t2 = endTime;
		}
		int idx0 = 0, idx1 = track.size();
		if (t1 != null && t2 != null) {
			int c1 = startTime.compareTo(t2);
			if (c1 > 0)
				return -1;
			int c2 = endTime.compareTo(t1);
			if (c2 < 0)
				return -1;
			if (c1 == 0) {
				idx1 = 1;
			}
			if (c2 == 0) {
				idx0 = track.size() - 1;
			}
		}
		double dist = Double.NaN;
		int pIdx = -1;
		for (int i = idx0; i < idx1; i++)
			if (isSegmActive(i)) {
				SpatialEntity spe = (SpatialEntity) track.elementAt(i);
				if (t1 != null && t2 != null) {
					TimeReference tref = spe.getTimeReference();
					if (tref == null) {
						continue;
					}
					int c1 = tref.getValidFrom().compareTo(t2);
					if (c1 > 0) {
						break;
					}
					int c2 = tref.getValidUntil().compareTo(t1);
					if (c2 < 0) {
						continue;
					}
				}
				RealPoint p = spe.getCentre();
				double d = GeoComp.distance(x, y, p.x, p.y, isGeo);
				if (Double.isNaN(dist) || dist > d) {
					dist = d;
					pIdx = i;
				}
			}
		return pIdx;
	}

	/**
	 * The total length of the track
	 */
	protected double trackLength = Double.NaN;

	public void setTrackLength(double trackLength) {
		this.trackLength = trackLength;
	}

	public void incrementTrackLength(double segmentLength) {
		trackLength += segmentLength;
	}

	/**
	 * The duration of the trip
	 */
	protected long duration = -1;

	/**
	 * Returns the identifier of the moving entity
	 */
	public String getEntityId() {
		return entityId;
	}

	/**
	 * Sets the identifier of the moving entity
	 */
	public void setEntityId(String entityId) {
		this.entityId = entityId;
	}

	public Vector getTrack() {
		return track;
	}

	/**
	 * Returns the generalised track if available. The generalised track consists of
	 * a reduced number of positions but the positions of the stops and turns
	 * must be preserved.
	 */
	public Vector getGeneralisedTrack() {
		return mainPositions;
	}

	/**
	 * Sets a generalised track consisting of a reduced number of positions
	 * (but the positions of the stops and turns must be preserved!)
	 */
	public void setGeneralisedTrack(Vector genTrack) {
		this.mainPositions = genTrack;
	}

	/**
	 * Returns the geo position of this object around the given time moment.
	 */
	@Override
	public RealPoint getGeoPositionAroundTime(TimeMoment t) {
		if (t == null)
			return null;
		if (track == null || track.size() < 1 || startTime == null || endTime == null)
			return null;
		SpatialEntity lastValid = (SpatialEntity) track.elementAt(0);
		for (int i = 1; i < track.size(); i++) {
			SpatialEntity spe = (SpatialEntity) track.elementAt(i);
			TimeReference tref = spe.getTimeReference();
			if (tref == null) {
				continue;
			}
			if (tref.getValidFrom().compareTo(t) > 0) {
				break;
			}
			lastValid = spe;
		}
		if (lastValid == null)
			return null;
		return lastValid.getCentre();
	}

	/**
	 * The function of the basic GeoObject interface.
	 * Returns a version of this GeoObject corresponding to the specified time
	 * interval. If the object does not change over time, returns itself. May
	 * return null if the object does not exist during the specified interval.
	 */
	@Override
	public GeoObject getObjectVersionForTimeInterval(TimeMoment t1, TimeMoment t2) {
		if (track == null || track.size() < 1 || startTime == null || endTime == null)
			return super.getObjectVersionForTimeInterval(t1, t2);
		firstIdx = 0;
		lastIdx = track.size() - 1;
		if (t1 == null) {
			t1 = startTime;
		}
		if (t2 == null) {
			t2 = endTime;
		}
		if (startTime.compareTo(t1) >= 0 && endTime.compareTo(t2) <= 0)
			return this;
		int c1 = startTime.compareTo(t2);
		if (c1 > 0)
			return null;
		int c2 = endTime.compareTo(t1);
		if (c2 < 0)
			return null;
		if (c1 == 0) {
			lastIdx = 0;
			return this;
		}
		if (c2 == 0) {
			firstIdx = track.size() - 1;
			return this;
		}
		int lastKnown = -1;
		firstIdx = lastIdx = -1;
		for (int i = 0; i < track.size(); i++) {
			SpatialEntity spe = (SpatialEntity) track.elementAt(i);
			TimeReference tref = spe.getTimeReference();
			if (tref == null) {
				continue;
			}
			c1 = tref.getValidFrom().compareTo(t2);
			if (c1 > 0) {
				break;
			}
			c2 = tref.getValidUntil().compareTo(t1);
			if (c2 < 0) {
				lastKnown = i;
				continue;
			}
			if (firstIdx < 0) {
				firstIdx = i;
			}
			lastIdx = i;
		}
		if (firstIdx < 0) {
			if (lastKnown < 0)
				return null;
			firstIdx = lastIdx = lastKnown;
		}
		return this;
	}

	/**
	 * Same as getObjectVersionForTimeInterval with a single difference:
	 * when only a part of the trajectory fits into the interval, produces a new
	 * instance of DMovingObject with this part of the trajectory.
	 */
	@Override
	public GeoObject getObjectCopyForTimeInterval(TimeMoment t1, TimeMoment t2) {
		if (track == null || track.size() < 1 || startTime == null || endTime == null)
			return this;
		if (t1 == null)
			return this;
		if (t2 == null) {
			t2 = endTime;
		}
		if (startTime.compareTo(t1) >= 0 && endTime.compareTo(t2) <= 0)
			return this;
		int c1 = startTime.compareTo(t2);
		if (c1 > 0)
			return null;
		int c2 = endTime.compareTo(t1);
		if (c2 < 0)
			return null;
		int i1 = 0, i2 = track.size() - 1;
		if (c1 == 0) {
			i2 = 0;
		} else if (c2 == 0) {
			i1 = track.size() - 1;
		} else {
			int lastKnown = -1;
			i1 = i2 = -1;
			for (int i = 0; i < track.size(); i++) {
				SpatialEntity spe = (SpatialEntity) track.elementAt(i);
				TimeReference tref = spe.getTimeReference();
				if (tref == null) {
					continue;
				}
				c1 = tref.getValidFrom().compareTo(t2);
				if (c1 > 0) {
					break;
				}
				c2 = tref.getValidUntil().compareTo(t1);
				if (c2 < 0) {
					lastKnown = i;
					continue;
				}
				if (i1 < 0) {
					i1 = i;
				}
				i2 = i;
			}
			if (i1 < 0) {
				if (lastKnown < 0)
					return null;
				i1 = i2 = lastKnown;
			}
		}
		if (i1 == 0 && i2 >= track.size() - 1)
			return this;
		Vector tt = new Vector(i2 - i1 + 1, 10);
		for (int i = i1; i <= i2; i++) {
			SpatialEntity spe = (SpatialEntity) ((SpatialEntity) track.elementAt(i)).clone();
			TimeReference tref = new TimeReference();
			tref.setValidFrom(spe.getTimeReference().getValidFrom().getCopy());
			tref.setValidUntil(spe.getTimeReference().getValidUntil().getCopy());
			spe.setTimeReference(tref);
			tt.addElement(spe);
		}
		DMovingObject mobj = (DMovingObject) makeCopy();
		mobj.setTrack(tt);
		return mobj;
	}

	/**
	 * Sets the sequence of instances of SpatialEntity representing different
	 * positions of this object corresponding to consecutive time moments or
	 * intervals.
	 */
	public void setTrack(Vector track) {
		this.track = track;
		data.setStates(track);
		distances = null;
		trackLength = Double.NaN;
		duration = -1;
		startTime = null;
		endTime = null;
		if (track == null || track.size() < 1)
			return;
		SpatialEntity spe = (SpatialEntity) track.elementAt(0);
		TimeReference tref = spe.getTimeReference();
		if (tref != null) {
			startTime = tref.getValidFrom();
			endTime = tref.getValidUntil();
			if (endTime == null) {
				endTime = startTime;
			}
		}
		if (track.size() > 1) {
			spe = (SpatialEntity) track.elementAt(track.size() - 1);
			tref = spe.getTimeReference();
			if (tref != null) {
				if (startTime == null) {
					startTime = tref.getValidFrom();
				}
				endTime = tref.getValidUntil();
				if (endTime == null) {
					endTime = tref.getValidFrom();
				}
			}
		}
		tref = data.getTimeReference();
		if (tref == null) {
			tref = new TimeReference();
			data.setTimeReference(tref);
		}
		tref.setValidFrom(startTime);
		tref.setValidUntil(endTime);
		firstIdx = 0;
		lastIdx = track.size() - 1;
	}

	/**
	 * Adds a new position to the track: a geometry and the time interval of
	 * its validity.
	 */
	public void addPosition(Geometry position, TimeMoment fromT, TimeMoment toT, ThematicDataItem themData) {
		if (position == null || fromT == null || data == null)
			return;
		distances = null;
		duration = -1;
		position.setGeographic(isGeo);
		if (track == null) {
			track = new Vector(100, 100);
			firstIdx = 0;
			data.setStates(track);
		}
		SpatialEntity spe = new SpatialEntity(data.getId(), data.getName());
		spe.setGeometry(position);
		TimeReference tref = new TimeReference();
		tref.setValidFrom(fromT);
		tref.setValidUntil((toT != null) ? toT : fromT);
		spe.setTimeReference(tref);
		if (themData != null) {
			spe.setThematicData(themData);
		}
		//find the right place among the track points
		int idx = track.size();
		for (int i = track.size() - 1; i >= 0; i--) {
			SpatialEntity spe1 = (SpatialEntity) track.elementAt(i);
			TimeReference tref1 = spe1.getTimeReference();
			if (fromT.compareTo(tref1.getValidFrom()) >= 0) {
				break;
			}
			idx = i;
		}
		track.insertElementAt(spe, idx);
		lastIdx = track.size() - 1;
		if (startTime == null || startTime.compareTo(fromT) > 0) {
			startTime = fromT;
		}
		if (endTime == null || endTime.compareTo(tref.getValidUntil()) < 0) {
			endTime = tref.getValidUntil();
		}
		tref = data.getTimeReference();
		if (tref == null) {
			tref = new TimeReference();
			data.setTimeReference(tref);
		}
		tref.setValidFrom(startTime);
		tref.setValidUntil(endTime);
	}

	/**
	 * Adds a new position without thematic data.
	 */
	public void addPosition(Geometry position, TimeMoment fromT, TimeMoment toT) {
		addPosition(position, fromT, toT, null);
	}

	/**
	* The type of DMovingObject is always Geometry.line.
	*/
	@Override
	public char getSpatialType() {
		if (type != Geometry.line) {
			type = Geometry.line;
		}
		return type;
	}

	/**
	* Does nothing; the type of DMovingObject is always Geometry.line.
	*/
	@Override
	public void setSpatialType(char objType) {
		if (type != Geometry.line) {
			type = Geometry.line;
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
	 * Additionally to what is done in the superclass, tries to find
	 * entity identifier in the thematic data.
	 */
	@Override
	public void setThematicData(ThematicDataItem dit) {
		super.setThematicData(dit);
		if (dit != null && entityId == null) {
			int idx = -1;
			for (int i = 0; i < dit.getAttrCount() && idx < 0; i++) {
				String name = dit.getAttributeName(i);
				if (name == null) {
					continue;
				}
				name = name.toUpperCase();
				if (name.indexOf("ENTITY") >= 0 || name.indexOf("EID") >= 0 || name.indexOf("E_ID") >= 0) {
					idx = i;
				}
			}
			if (idx >= 0) {
				entityId = dit.getAttrValueAsString(idx);
			}
		}
	}

	/**
	 * Reports if this GeoObject contains data about changes of some entity
	 * over time, e.g. about movement (change of position), change of shape,
	 * size, etc. DMovingObject returns true if the start and end times
	 * are defined and the duration is not zero.
	 */
	@Override
	public boolean includesChanges() {
		if (data == null)
			return false;
		if (startTime == null || endTime == null)
			return false;
		return startTime.compareTo(endTime) < 0;
	}

	/**
	* Returns a copy of this object. The spatial and thematic data items
	* used by this objects are not duplicated; the copy will use references to the
	* same data items
	*/
	@Override
	public GeoObject makeCopy() {
		DMovingObject mobj = new DMovingObject();
		if (data != null) {
			mobj.setup((SpatialEntity) data.clone());
		}
		mobj.setEntityId(entityId);
		mobj.setTrack(track);
		mobj.setGeneralisedTrack(mainPositions);
		mobj.setLabel(label);
		mobj.setSpatialType(type);
		mobj.setDrawingParameters(drawParam);
		mobj.setVisualizer(vis);
		mobj.setBackgroundVisualizer(bkgVis);
		mobj.distances = distances;
		mobj.trackLength = trackLength;
		mobj.duration = duration;
		mobj.segmFilterCombiner = (segmFilterCombiner == null) ? null : segmFilterCombiner.makeCopy(mobj);
		return mobj;
	}

	/**
	* The function draw is called when the DGeoObject must draw itself.
	* When there is no Visualiser available, the DGeoObject uses the
	* previously set DrawingParameters. When a Visualizer is available,
	* and it does not produce diagrams, the DGeoObject uses the
	* function getPresentation of the Visualiser to determine how
	* to draw itself.
	*/
	@Override
	public void draw(Graphics g, MapContext mc) {
		labelPos = null;
		if (track == null || track.size() < 1 || firstIdx < 0 || lastIdx < firstIdx)
			return;
		drawSegment(g, mc, firstIdx, lastIdx);
		Object pres = (vis != null) ? vis.getPresentation(data, mc) : (bkgVis != null) ? bkgVis.getPresentation(data, mc) : null;
		drawIconOrDiagram(pres, g, mc);
	}

	/**
	 * Used to draw trajectory points selected by a segment filter
	 */
	protected static MovingPointSign mpSign = null;

	/**
	* Draws a trajectory segment.
	*/
	public void drawSegment(Graphics g, MapContext mc, int idx1, int idx2) {
		labelPos = null;
		if (track == null || track.size() < 1 || idx1 < 0 || idx2 < idx1)
			return;
		defineCurrentAppearance(mc);
		if (idx1 < firstIdx) {
			idx1 = firstIdx;
		}
		if (idx2 > lastIdx) {
			idx2 = lastIdx;
		}
		if (idx2 >= track.size()) {
			idx2 = track.size() - 1;
		}
		if (idx2 < idx1)
			return;
		lastColor = currAppearance.lineColor;

		boolean active[] = null;
		boolean filtered = segmFilterCombiner != null && !segmFilterCombiner.areAllSegmentsActive();
		if (filtered) {
			active = new boolean[track.size()];
			for (int i = 0; i < active.length; i++) {
				active[i] = segmFilterCombiner.isSegmentActive(i);
			}
		}

		g.setColor(currAppearance.lineColor);
		if (idx1 == idx2) {
			SpatialEntity spe = (SpatialEntity) track.elementAt(idx1);
			drawGeometry(spe.getGeometry(), g, mc, currAppearance.lineColor, currAppearance.fillColor, currAppearance.lineWidth, null);
		} else {
			RealPoint prevP = null;
			for (int i = idx1; i <= idx2; i++) {
				SpatialEntity spe = (SpatialEntity) track.elementAt(i);
				Geometry geom = spe.getGeometry();
				if (!(geom instanceof RealPoint)) {
					drawGeometry(geom, g, mc, currAppearance.lineColor, currAppearance.fillColor, currAppearance.lineWidth, null);
				}
				RealPoint p = spe.getCentre();
				boolean pointIsActive = !filtered || active[i], toDrawLine = prevP != null && (!filtered || active[i - 1]), toMarkPoint = false;
				if (filtered) {
					toDrawLine = toDrawLine && (pointIsActive || segmFilterCombiner.hasFilterForSegments());
					toMarkPoint = pointIsActive /*&& segmFilterCombiner.hasFilterForPoints()*/;
				}
				if (!toDrawLine && !pointIsActive) {
					prevP = p;
					continue;
				}
				int sx = getScreenX(mc, p.x, p.y), sy = getScreenY(mc, p.x, p.y);
				if (toDrawLine) {
					int sx0 = getScreenX(mc, prevP.x, prevP.y);
					int sy0 = getScreenY(mc, prevP.x, prevP.y);
					if (sx != sx0 || sy != sy0) {
						if (isGeo && cross180Meridian(prevP.x, p.x)) {
							float xx[] = breakLine(prevP.x, p.x);
							int x_2 = getScreenX(mc, xx[0], p.y), x_1 = getScreenX(mc, xx[1], prevP.y);
							if (currAppearance.lineWidth < 2) {
								g.drawLine(sx0, sy0, x_2, sy);
								g.drawLine(x_1, sy0, sx, sy);
							} else {
								Drawing.drawLine(g, currAppearance.lineWidth, sx0, sy0, x_2, sy, true, true);
								Drawing.drawLine(g, currAppearance.lineWidth, x_1, sy0, sx, sy, true, true);
							}
						} else {
							if (currAppearance.lineWidth < 2) {
								g.drawLine(sx0, sy0, sx, sy);
							} else {
								Drawing.drawLine(g, currAppearance.lineWidth, sx0, sy0, sx, sy, true, true);
							}
						}
					}
				}
				if (toMarkPoint) {
					if (mpSign == null) {
						mpSign = new MovingPointSign();
					}
					double dx0 = 0, dy0 = 0, dx1 = 0, dy1 = 0;
					if (prevP == null && i > 0) {
						prevP = ((SpatialEntity) track.elementAt(i - 1)).getCentre();
					}
					if (prevP != null) {
						dx0 = prevP.x - p.x;
						dy0 = prevP.y - p.y;
					}
					RealPoint p1 = null;
					if (i < track.size() - 1) {
						p1 = ((SpatialEntity) track.elementAt(i + 1)).getCentre();
					}
					if (p1 != null) {
						dx1 = p1.x - p.x;
						dy1 = p1.y - p.y;
					}
					mpSign.draw(g, sx, sy, dx0, dy0, dx1, dy1);
				}
				if (i == idx1 || i == idx2) {
					// drawing squares for start and end of the trajectory
					if (i != idx2) {
						g.drawRect(sx - 2, sy - 2, 4, 4);
					} else {
						g.fillRect(sx - 2, sy - 2, 5, 5);
					}
				}
				prevP = p;
			}
		}
	}

	/**
	 * Draws the geometry of this object.
	 * If fillColor is null, the contour is not filled.
	 * If borderColor is null, the border is not drawn
	 */
	@Override
	public void drawGeometry(Graphics g, MapContext mc, Color borderColor, Color fillColor, int width, ImageObserver observer) {
		if (firstIdx == lastIdx) {
			SpatialEntity spe = (SpatialEntity) track.elementAt(firstIdx);
			drawGeometry(spe.getGeometry(), g, mc, borderColor, fillColor, width, null);
		} else {
			RealPoint prevP = null;
			for (int i = firstIdx; i <= lastIdx; i++) {
				SpatialEntity spe = (SpatialEntity) track.elementAt(i);
				Geometry geom = spe.getGeometry();
				if (!(geom instanceof RealPoint)) {
					drawGeometry(geom, g, mc, borderColor, fillColor, width, null);
				}
				RealPoint p = spe.getCentre();
				if (prevP != null) {
					RealLine line = new RealLine();
					line.x1 = prevP.x;
					line.y1 = prevP.y;
					line.x2 = p.x;
					line.y2 = p.y;
					line.directed = false;
					if (segmFilterCombiner == null || segmFilterCombiner.isSegmentActive(i)) {
						drawGeometry(line, g, mc, borderColor, fillColor, width, null);
					}
				}
				if ((i == firstIdx || i == lastIdx) && (segmFilterCombiner == null || segmFilterCombiner.isSegmentActive(i))) {
					g.setColor(borderColor);
					int sx = getScreenX(mc, p.x, p.y), sy = getScreenY(mc, p.x, p.y);
					if (i != lastIdx) {
						g.drawRect(sx - 2, sy - 2, 4, 4);
					} else {
						g.fillRect(sx - 2, sy - 2, 5, 5);
					}
				}
				prevP = p;
			}
		}
	}

	/**
	* Returns the Geometry of this object
	*/
	@Override
	public Geometry getGeometry() {
		int first = firstIdx, last = lastIdx;
		if (track == null || track.size() < 1 || first < 0 || last < first)
			return null;
		if (last >= track.size()) {
			last = track.size() - 1;
		}
		if (first >= last) {
			SpatialEntity spe = (SpatialEntity) track.elementAt(last);
			return spe.getGeometry();
		}
		RealPolyline rpol = new RealPolyline();
		rpol.p = new RealPoint[last - first + 1];
		for (int i = first; i <= last; i++) {
			SpatialEntity spe = (SpatialEntity) track.elementAt(i);
			Geometry geom = spe.getGeometry();
			if (geom instanceof RealPoint) {
				rpol.p[i - first] = (RealPoint) geom;
			} else {
				float bounds[] = geom.getBoundRect();
				rpol.p[i - first] = new RealPoint((bounds[0] + bounds[2]) / 2, (bounds[1] + bounds[3]) / 2);
			}
		}
		return rpol;
	}

	public TimeMoment getStartTime() {
		return startTime;
	}

	public void setStartTime(TimeMoment startTime) {
		this.startTime = startTime;
	}

	public TimeMoment getEndTime() {
		return endTime;
	}

	public void setEndTime(TimeMoment endTime) {
		this.endTime = endTime;
	}

	/**
	 * After a transformation of the time references, e.g. from absolute
	 * to relative times, updates its internal variables.
	 */
	@Override
	public void updateStartEndTimes() {
		startTime = endTime = null;
		if (track == null || track.size() < 1)
			return;
		SpatialEntity spe = (SpatialEntity) track.elementAt(0);
		TimeReference tref = spe.getTimeReference();
		if (tref != null) {
			startTime = tref.getValidFrom();
		}
		spe = (SpatialEntity) track.elementAt(track.size() - 1);
		tref = spe.getTimeReference();
		if (tref != null) {
			endTime = tref.getValidUntil();
			if (endTime == null) {
				endTime = tref.getValidFrom();
			}
		}
		super.updateStartEndTimes();
	}

	public long getDuration() {
		if (duration >= 0)
			return duration;
		if (startTime == null || endTime == null) {
			duration = 0;
		} else {
			duration = endTime.subtract(startTime);
		}
		return duration;
	}

	public int getPositionCount() {
		if (track == null)
			return 0;
		return track.size();
	}

	public SpatialEntity getPosition(int idx) {
		if (track == null || idx < 0 || idx >= track.size())
			return null;
		return (SpatialEntity) track.elementAt(idx);
	}

	public Geometry getPositionGeometry(int idx) {
		SpatialEntity spe = getPosition(idx);
		if (spe == null)
			return null;
		return spe.getGeometry();
	}

	public RealPoint getPositionAsPoint(int idx) {
		SpatialEntity spe = getPosition(idx);
		if (spe == null)
			return null;
		return spe.getCentre();
	}

	public TimeReference getPositionTime(int idx) {
		SpatialEntity spe = getPosition(idx);
		if (spe == null)
			return null;
		return spe.getTimeReference();
	}

	/**
	 * For a given track, returns a track with a simplified shape. Preserves
	 * positions of major turns and stops.
	 * @param geoFactor - for geographic coordinates, a factor to transform distances
	 *                    (otherwise the distances are too small).
	 * @param minStopTime - minimal time interval treated as stop to be preserved
	 * @param minAngleRadian - the minimum angle of direction change, in radian
	 * @param minDist - minimum distance between consecutive positions
	 * @param maxDist - maximum distance between consecutive positions
	 * @return generalised track supposed to contain fewer positions.
	 */
	public Vector generaliseTrack(float geoFactor, long minStopTime, double minAngleRadian, float minDist, float maxDist) {
		if (track == null || track.size() < 1)
			return null;
		if (track.size() < 3)
			return (Vector) track.clone();
		SpatialEntity spe0 = (SpatialEntity) track.elementAt(0), //this will be the last added position
		speLast = (SpatialEntity) track.elementAt(track.size() - 1);
		if (spe0 == null || speLast == null)
			return null;
		RealPoint p0 = spe0.getCentre();
		if (p0 == null)
			return (Vector) track.clone();
		minDist *= geoFactor;
		maxDist *= geoFactor;
		float sqMinDist = minDist * minDist, sqMaxDist = maxDist * maxDist, smallDist = minDist / 100;
		TimeReference tr0 = spe0.getTimeReference();
		Vector genTrack = new Vector(track.size(), 1);
		genTrack.addElement(spe0);
		if (mainPositions == null) {
			mainPositions = new Vector(track.size(), 100);
		} else {
			mainPositions.removeAllElements();
		}
		mainPositions.addElement(spe0);
		double cosMinAngle = Math.cos(minAngleRadian);
		for (int i = 1; i < track.size() - 1; i++) {
			SpatialEntity spe1 = (SpatialEntity) track.elementAt(i);
			if (spe1 == null) {
				continue;
			}
			RealPoint p1 = spe1.getCentre();
			if (p1 == null) {
				continue;
			}
			TimeReference tr = spe1.getTimeReference();
			boolean toAdd = false;
			//check if the i-th position should be added to the generalised track
			float dx1 = (p1.x - p0.x) * geoFactor, dy1 = (p1.y - p0.y) * geoFactor;
			if (Math.abs(dx1) > smallDist || Math.abs(dy1) > smallDist) {
				toAdd = dx1 * dx1 + dy1 * dy1 >= sqMaxDist;
				if (!toAdd) {
					SpatialEntity spe2 = (SpatialEntity) track.elementAt(i + 1);
					if (spe2 != null) {
						RealPoint p2 = spe2.getCentre();
						if (p2 != null) {
							float dx2 = (p2.x - p1.x) * geoFactor, dy2 = (p2.y - p1.y) * geoFactor;
							double cos = GeoComp.getCosAngleBetweenVectors(dx1, dy1, dx2, dy2);
							toAdd = cos <= cosMinAngle;
						}
					}
				}
			}
			if (!toAdd && minStopTime > 0 && tr != null && tr.getValidFrom() != null) {
				toAdd = tr.getValidFrom().subtract(tr0.getValidFrom()) > minStopTime && dx1 * dx1 + dy1 * dy1 < sqMinDist;
			}
			tr0 = spe1.getTimeReference();
			if (toAdd) {
				genTrack.addElement(spe1);
				mainPositions.addElement(spe1);
				spe0 = spe1;
				p0 = p1;
			}
		}
		genTrack.addElement(speLast);
		mainPositions.addElement(speLast);
		mainPositions.trimToSize();
		if (genTrack.size() < 4) {
			genTrack.trimToSize();
			return genTrack;
		}
		/**/
		Vector simpleTrack = new Vector(genTrack.size(), 1);
		//add the start position (must be preserved)
		simpleTrack.addElement(genTrack.elementAt(0));
		spe0 = (SpatialEntity) genTrack.elementAt(1);
		p0 = spe0.getCentre();
		float sumX = p0.x, sumY = p0.y;
		TimeMoment validFrom = null, validUntil = null;
		TimeReference tref = spe0.getTimeReference();
		if (tref != null) {
			validFrom = tref.getValidFrom();
			validUntil = tref.getValidUntil();
			if (validUntil == null) {
				validUntil = validFrom;
			}
		}
		int from = 1;
		for (int i = 2; i < genTrack.size() - 1; i++) {
			SpatialEntity spe1 = (SpatialEntity) genTrack.elementAt(i);
			RealPoint p1 = spe1.getCentre();
			float dx = (p1.x - p0.x) * geoFactor, dy = (p1.y - p0.y) * geoFactor;
			float sqDist = dx * dx + dy * dy;
			if (sqDist >= sqMinDist) {
				int nPoints = i - from;
				if (nPoints == 1) {
					simpleTrack.addElement(spe0);
				} else {
					SpatialEntity spe = new SpatialEntity(spe0.getId());
					RealPoint pGen = new RealPoint(sumX / nPoints, sumY / nPoints);
					spe.setGeometry(pGen);
					if (validFrom != null && validUntil != null) {
						TimeReference tr = new TimeReference();
						tr.setValidFrom(validFrom);
						tr.setValidUntil(validUntil);
						spe.setTimeReference(tr);
					}
					simpleTrack.addElement(spe);
				}
				spe0 = spe1;
				p0 = p1;
				from = i;
				sumX = p0.x;
				sumY = p0.y;
				if (validFrom != null) {
					tref = spe0.getTimeReference();
					if (tref != null) {
						validFrom = tref.getValidFrom();
						validUntil = tref.getValidUntil();
						if (validUntil == null) {
							validUntil = validFrom;
						}
					} else {
						validFrom = null;
						validUntil = null;
					}
				}
			} else {
				sumX += p1.x;
				sumY += p1.y;
				if (validFrom != null) {
					tref = spe1.getTimeReference();
					if (tref != null) {
						validUntil = tref.getValidUntil();
						if (validUntil == null) {
							validUntil = tref.getValidFrom();
						}
					}
				}
			}
		}
		//add the latest position before the end
		int nPoints = genTrack.size() - 1 - from;
		if (nPoints == 1) {
			simpleTrack.addElement(spe0);
		} else {
			SpatialEntity spe = new SpatialEntity(spe0.getId());
			RealPoint pGen = new RealPoint(sumX / nPoints, sumY / nPoints);
			spe.setGeometry(pGen);
			if (validFrom != null && validUntil != null) {
				TimeReference tr = new TimeReference();
				tr.setValidFrom(validFrom);
				tr.setValidUntil(validUntil);
				spe.setTimeReference(tr);
			}
			simpleTrack.addElement(spe);
		}
		//add the end position (must be preserved)
		simpleTrack.addElement(genTrack.elementAt(genTrack.size() - 1));
		simpleTrack.trimToSize();
		return simpleTrack;
		/**/
	}

	/**
	 * Returns the index of the position of the given track in between
	 * the positions with the given indexes which has the maximum distance
	 * from these two positions
	 */
	public static int getMaxDistantPositionBetween(Vector track, int idx1, int idx2, boolean isGeo) {
		if (track == null || idx1 < 0 || idx2 - idx1 < 2 || idx2 >= track.size())
			return -1;
		RealPoint pt1 = ((SpatialEntity) track.elementAt(idx1)).getCentre(), pt2 = ((SpatialEntity) track.elementAt(idx2)).getCentre();
		double maxD1 = GeoComp.distance(pt1.x, pt1.y, pt2.x, pt2.y, isGeo) / 2, maxD2 = maxD1;
		int pIdx = -1;
		for (int k = idx1 + 1; k < idx2; k++) {
			RealPoint p0 = ((SpatialEntity) track.elementAt(k)).getCentre();
			double d1 = GeoComp.distance(p0.x, p0.y, pt1.x, pt1.y, isGeo), d2 = GeoComp.distance(p0.x, p0.y, pt2.x, pt2.y, isGeo);
			if (d1 > maxD1 && d2 > maxD2) {
				maxD1 = d1;
				maxD2 = d2;
				pIdx = k;
			}
		}
		return pIdx;
	}

	/**
	 * Returns an array with 4 extreme points of the trajectory, i.e. the points
	 * lying on the bounding rectangle:
	 * 0) the westernmost point (x = min)
	 * 1) the easternmost point (x = max)
	 * 2) the southernmost point (y = min)
	 * 3) the northernmost point (y = max)
	 */
	public RealPoint[] getExtremePoints() {
		if (track == null || track.size() < 1)
			return null;
		RealPoint p = ((SpatialEntity) track.elementAt(0)).getCentre();
		RealPoint exp[] = new RealPoint[4];
		for (int i = 0; i < exp.length; i++) {
			exp[i] = p;
		}
		for (int k = 1; k < track.size(); k++) {
			p = ((SpatialEntity) track.elementAt(k)).getCentre();
			if (p.x < exp[0].x) {
				exp[0] = p;
			} else if (p.x > exp[1].x) {
				exp[1] = p;
			}
			if (p.y < exp[2].y) {
				exp[2] = p;
			} else if (p.y > exp[3].y) {
				exp[3] = p;
			}
		}
		return exp;
	}

	/**
	 * Returns the index of the middle point of the trajectory,
	 * i.e. such that the path travelled before this point approximately equals
	 * the path travelled after it.
	 */
	public RealPoint getMidPointOfPath() {
		if (track == null || track.size() < 1)
			return null;
		RealPoint p1 = ((SpatialEntity) track.elementAt(0)).getCentre();
		if (track.size() < 2)
			return p1;
		getTrackLength();
		if (trackLength <= 0 || distances == null)
			return p1;
		double halfLen = trackLength / 2;
		if (halfLen <= 0)
			return p1;
		if (track.size() < 3) {
			RealPoint p2 = ((SpatialEntity) track.elementAt(track.size() - 1)).getCentre();
			return new RealPoint((p1.x + p2.x) / 2, (p1.y + p2.y) / 2);
		}
		double dd = 0;
		for (int j = 0; j < distances.length; j++) {
			dd += distances[j];
			if (dd < halfLen) {
				continue;
			}
			RealPoint p = ((SpatialEntity) track.elementAt(j + 1)).getCentre();
			if (dd == halfLen)
				return p;
			double ratio = (dd - halfLen) / distances[j];
			RealPoint p0 = ((SpatialEntity) track.elementAt(j)).getCentre();
			double dx = (p.x - p0.x) * ratio, dy = (p.y - p0.y) * ratio;
			return new RealPoint(p.x - (float) dx, p.y - (float) dy);
		}
		return null;
	}

	/**
	 * Get indices of N equidistant (according to distances between points) intermediate points
	 * Sometimes returns zeros at the end because starts and ends are not included...
	 */
	public int[] getNIntermPointsEqDist(int n) {
		if (n < 1 || track == null || track.size() < 1)
			return null;
		int idx[] = new int[n];
		if (track.size() == 1) {
			for (int i = 0; i < n; i++) {
				idx[i] = 0;
			}
			return idx;
		}
		if (track.size() == 2) {
			for (int i = 0; i < (n + 1) / 2; i++) {
				idx[i] = 0;
			}
			for (int i = (n + 1) / 2; i < n; i++) {
				idx[i] = 1;
			}
			return idx;
		}
		if (track.size() == 3) {
			for (int i = 0; i < n; i++) {
				idx[i] = 1;
			}
			return idx;
		}
		getTrackLength();
		if (trackLength <= 0 || distances == null) {
			for (int i = 0; i < n; i++) {
				idx[i] = 0;
			}
			return idx;
		}
		double step = trackLength / (n + 1);
		double nextDist = step, d = 0;
		int k = 0;
		for (int i = 0; i < distances.length && k < idx.length; i++) {
			double d1 = d + distances[i];
			if (d1 >= nextDist) {
				if (nextDist - d < d1 - nextDist) {
					idx[k++] = i;
				} else {
					idx[k++] = i + 1;
				}
				nextDist += step;
			}
			d = d1;
		}
		for (int i = k; i < idx.length; i++) {
			idx[i] = track.size() - 1;
		}
		return idx;
	}

	/**
	 * Returns the most distant point from the given point
	 */
	public RealPoint getFarthestPointFrom(RealPoint pt) {
		if (pt == null)
			return null;
		if (track == null || track.size() < 1)
			return null;
		RealPoint p = ((SpatialEntity) track.elementAt(0)).getCentre();
		if (track.size() < 2)
			return p;
		getTrackLength();
		if (trackLength <= 0)
			return p;
		int pIdx = 0;
		double maxD = 0;
		for (int k = 0; k < track.size(); k++) {
			p = ((SpatialEntity) track.elementAt(k)).getCentre();
			double d = GeoComp.distance(pt.x, pt.y, p.x, p.y, isGeo);
			if (d > maxD) {
				maxD = d;
				pIdx = k;
			}
		}
		return ((SpatialEntity) track.elementAt(pIdx)).getCentre();
	}

	/**
	 * Returns the most distant point from the start and the end
	 */
	public RealPoint getFarthestPointFromStartEnd() {
		if (track == null || track.size() < 1)
			return null;
		RealPoint p1 = ((SpatialEntity) track.elementAt(0)).getCentre();
		if (track.size() < 2)
			return p1;
		getTrackLength();
		if (trackLength <= 0)
			return p1;
		RealPoint p2 = ((SpatialEntity) track.elementAt(track.size() - 1)).getCentre();
		if (track.size() < 3)
			return new RealPoint((p1.x + p2.x) / 2, (p1.y + p2.y) / 2);
		int pIdx = 0;
		double maxD = 0;
		for (int k = 0; k < track.size(); k++) {
			RealPoint p = ((SpatialEntity) track.elementAt(k)).getCentre();
			double d = GeoComp.distance(p1.x, p1.y, p.x, p.y, isGeo) + GeoComp.distance(p2.x, p2.y, p.x, p.y, isGeo);
			if (d > maxD) {
				maxD = d;
				pIdx = k;
			}
		}
		return ((SpatialEntity) track.elementAt(pIdx)).getCentre();
	}

	/**
	 * Returns the mean point, i.e. x= the mean of all x and y= the mean of all y
	 */
	public RealPoint getMeanPoint() {
		if (track == null || track.size() < 1)
			return null;
		RealPoint p = ((SpatialEntity) track.elementAt(0)).getCentre();
		if (track.size() < 2)
			return p;
		float sumX = p.x, sumY = p.y;
		for (int k = 1; k < track.size(); k++) {
			p = ((SpatialEntity) track.elementAt(k)).getCentre();
			sumX += p.x;
			sumY += p.y;
		}
		return new RealPoint(sumX / track.size(), sumY / track.size());
	}

	/**
	 * Returns the median point, i.e. x= the mean of all x and y= the mean of all y
	 */
	public RealPoint getMedianPoint() {
		if (track == null || track.size() < 1)
			return null;
		RealPoint p = ((SpatialEntity) track.elementAt(0)).getCentre();
		if (track.size() < 2)
			return p;
		FloatArray xAr = new FloatArray(track.size(), 10), yAr = new FloatArray(track.size(), 10);
		for (int k = 0; k < track.size(); k++) {
			p = ((SpatialEntity) track.elementAt(k)).getCentre();
			xAr.addElement(p.x);
			yAr.addElement(p.y);
		}
		float x = NumValManager.getMedian(xAr), y = NumValManager.getMedian(yAr);
		return new RealPoint(x, y);
	}

	/**
	 * Returns the directions between consecutive points. 0=E
	 */
	public double[] getDirections() {
		if (track == null || track.size() < 2)
			return null;
		double directions[] = new double[track.size()];
		RealPoint p0 = ((SpatialEntity) track.elementAt(0)).getCentre();
		for (int i = 1; i < track.size(); i++) {
			RealPoint p1 = ((SpatialEntity) track.elementAt(i)).getCentre();
			directions[i - 1] = GeoComp.getAngleYAxis(p1.x - p0.x, p1.y - p0.y);
			p0 = p1;
		}
		directions[directions.length - 1] = Double.NaN;
		return directions;
	}

	/**
	 * Returns the distances between consecutive points. If not computed yet, computes them.
	 */
	public double[] getDistances() {
		if (distances != null)
			return distances;
		if (track == null || track.size() < 2)
			return null;
		distances = new double[track.size() - 1];
		trackLength = 0;
		RealPoint p0 = ((SpatialEntity) track.elementAt(0)).getCentre();
		for (int i = 1; i < track.size(); i++) {
			RealPoint p1 = ((SpatialEntity) track.elementAt(i)).getCentre();
			double dist = GeoComp.distance(p0.x, p0.y, p1.x, p1.y, isGeo);
			distances[i - 1] = dist;
			trackLength += dist;
			p0 = p1;
		}
		return distances;
	}

	/**
	 * Sets the property of the object's geometry indicating whether
	 * the geometry has geographic coordinates (latitudes and longitudes).
	 */
	@Override
	public void setGeographic(boolean geographic) {
		if (isGeo != geographic) {
			distances = null;
		}
		super.setGeographic(geographic);
	}

	/**
	 * computes minimal distance to previous points starting from points from those the
	 * travelled distance is larger than distanceFreshold
	 */
	public double[] getDistancesToPast(double distanceFreshold) {
		getDistances();
		if (distances == null)
			return null;
		double d[] = new double[track.size()];
		d[0] = Double.NaN;
		for (int i = 1; i < track.size(); i++) {
			RealPoint pi = ((SpatialEntity) track.elementAt(i)).getCentre();
			double dist = 0d, minDist = Double.NaN;
			for (int j = i - 1; j >= 0; j--) {
				dist += distances[j];
				if (dist >= distanceFreshold) {
					RealPoint pj = ((SpatialEntity) track.elementAt(j)).getCentre();
					double distij = GeoComp.distance(pi.x, pi.y, pj.x, pj.y, isGeo);
					if (Double.isNaN(minDist) || distij < minDist) {
						minDist = distij;
					}
				}
			}
			d[i] = minDist;
		}
		if (isGeo) {
			for (int i = 0; i < d.length; i++) {
				d[i] /= 1000d;
			}
		}
		return d;
	}

	/**
	 * Returns speeds in all points computed as distance to next point divided by time.
	 * Last value is 0.
	 */
	public double[] getSpeeds() {
		getDistances();
		if (distances == null)
			return null;
		TimeMoment tm = ((SpatialEntity) track.elementAt(0)).getTimeReference().getValidFrom();
		boolean isPhysicalTime = (tm instanceof Date) ? true : false;
		double d[] = new double[distances.length + 1];
		d[d.length - 1] = Double.NaN;
		for (int i = 0; i < distances.length; i++) {
			double dt = ((SpatialEntity) track.elementAt(i + 1)).getTimeReference().getValidFrom().subtract(((SpatialEntity) track.elementAt(i)).getTimeReference().getValidFrom());
			if (isPhysicalTime) {
				switch (tm.getPrecision()) { // 'y','m','d','h','t','s'
				case 'h':
					break;
				case 't':
					dt /= 60;
					break;
				case 's':
					dt /= 60 * 60;
					break;
				default:
					break;
				}
			}
			d[i] = (dt == 0) ? 0 : distances[i] / dt;
			if (isGeo) {
				d[i] /= 1000d;
			}
		}
		return d;
	}

	/**
	 * Returns the cumulative traveleld path
	 */
	public double[] getDistancesCumulative() {
		getDistances();
		if (distances == null)
			return null;
		double d[] = new double[distances.length + 1];
		d[0] = 0d;
		for (int i = 1; i < d.length; i++) {
			d[i] = d[i - 1] + distances[i - 1];
		}
		if (isGeo) {
			for (int i = 0; i < d.length; i++) {
				d[i] /= 1000d;
			}
		}
		return d;
	}

	/**
	 * Returns the cumulative traveleld path
	 */
	public double[] getRemainingTripDistances() {
		getDistances();
		if (distances == null)
			return null;
		double d[] = new double[distances.length + 1];
		d[d.length - 1] = 0d;
		for (int i = d.length - 2; i >= 0; i--) {
			d[i] = d[i + 1] + distances[i];
		}
		if (isGeo) {
			for (int i = 0; i < d.length; i++) {
				d[i] /= 1000d;
			}
		}
		return d;
	}

	/**
	 * Returns the distances to the starying point of the trajectory
	 */
	public double[] getDistancesToStart() {
		if (track == null || track.size() < 2)
			return null;
		RealPoint p0 = ((SpatialEntity) track.elementAt(0)).getCentre();
		return getDistancesToPoint(p0);
	}

	/**
	 * Returns the distances to the ending point of the trajectory
	 */
	public double[] getDistancesToEnd() {
		if (track == null || track.size() < 2)
			return null;
		RealPoint p0 = ((SpatialEntity) track.elementAt(track.size() - 1)).getCentre();
		return getDistancesToPoint(p0);
	}

	/**
	 * returns distances to the set of points, i.e. distances to the closest point
	 */
	public double[] getDistancesToPoints(Vector<RealPoint> vp) {
		if (vp == null || vp.size() == 0)
			return null;
		double d[] = getDistancesToPoint(vp.elementAt(0));
		for (int i = 1; i < vp.size(); i++) {
			double dd[] = getDistancesToPoint(vp.elementAt(i));
			for (int j = 0; j < d.length; j++)
				if (dd[j] < d[j]) {
					d[j] = dd[j];
				}
		}
		return d;
	}

	/**
	 * Returns the distances to the set of geographical objects
	 */
	public double[] getDistancesToObjects(Vector<DGeoObject> vObj) {
		if (vObj == null || vObj.size() == 0)
			return null;
		RealRectangle b = getBounds();
		if (b == null)
			return null;
		boolean allPoints = true;
		for (int k = 0; k < vObj.size() && allPoints; k++) {
			Geometry geom = vObj.elementAt(k).getGeometry();
			if (geom == null) {
				continue;
			}
			allPoints = (geom instanceof RealPoint);
		}
		if (allPoints) {
			double d[] = new double[track.size()];
			for (int i = 0; i < track.size(); i++) {
				d[i] = Double.NaN;
			}
			for (int i = 0; i < track.size(); i++) {
				d[i] = Double.NaN;
				RealPoint p1 = ((SpatialEntity) track.elementAt(i)).getCentre();
				boolean in = false;
				double dx0 = Double.NaN, dy0 = Double.NaN, dxy02 = Double.NaN;
				for (int k = 0; k < vObj.size() && !in; k++) {
					RealPoint p0 = (RealPoint) vObj.elementAt(k).getGeometry();
					if (p0 == null) {
						continue;
					}
					double dx = Math.abs(p0.x - p1.x), dy = Math.abs(p0.y - p1.y);
					if (!Double.isNaN(dx0) && dx >= dx0 && dx >= dy0 && dy >= dx0 && dy >= dy0) {
						continue;
					}
					double dxy2 = dx * dx + dy * dy;
					if (!Double.isNaN(dxy02) && dxy2 >= dxy02) {
						continue;
					}
					double dist = GeoComp.distance(p1.x, p1.y, p0.x, p0.y, isGeo);
					if (Double.isNaN(d[i]) || dist < d[i]) {
						d[i] = dist;
						in = dist == 0;
						dx0 = dx;
						dy0 = dy;
						dxy02 = dxy2;
					}
				}
			}
			if (isGeo) {
				for (int i = 0; i < d.length; i++) {
					d[i] /= 1000d;
				}
			}
			return d;
		}
		boolean close[] = new boolean[vObj.size()];
		for (int k = 0; k < vObj.size(); k++) {
			close[k] = false;
			RealRectangle bo = vObj.elementAt(k).getBounds();
			if (bo == null) {
				continue;
			}
			close[k] = bo != null && bo.doesIntersect(b);
		}
		double d[] = new double[track.size()];
		boolean in[] = new boolean[track.size()];
		for (int i = 0; i < track.size(); i++) {
			d[i] = Double.NaN;
			in[i] = false;
		}
		for (int k = 0; k < vObj.size(); k++)
			if (close[k]) {
				Geometry geom = vObj.elementAt(k).getGeometry();
				for (int i = 0; i < track.size(); i++)
					if (!in[i]) {
						RealPoint p1 = ((SpatialEntity) track.elementAt(i)).getCentre();
						RealRectangle bo = vObj.elementAt(k).getBounds();
						if (bo.contains(p1.x, p1.y, 0)) {
							in[i] = geom.contains(p1.x, p1.y, 0);
							if (in[i]) {
								d[i] = 0;
							}
						}
					}
			}
		boolean closeToPoint[] = new boolean[vObj.size()];
		for (int i = 0; i < track.size(); i++)
			if (Double.isNaN(d[i])) {
				RealPoint p1 = ((SpatialEntity) track.elementAt(i)).getCentre();
				for (int k = 0; k < vObj.size(); k++) {
					closeToPoint[k] = false;
					if (close[k]) {
						RealRectangle bo = vObj.elementAt(k).getBounds();
						closeToPoint[k] = bo.contains(p1.x, p1.y, 0);
					}
				}
				int closestIdx = -1;
				double d0 = Double.NaN;
				for (int k = 0; k < vObj.size() && !in[i]; k++)
					if (closeToPoint[k]) {
						Geometry geom = vObj.elementAt(k).getGeometry();
						double dist = Computing.distance(p1.x, p1.y, geom, false);
						if (!Double.isNaN(dist) && (Double.isNaN(d0) || d0 > dist)) {
							in[i] = dist == 0;
							closestIdx = k;
							d0 = dist;
						}
					}
				if (!in[i]) {
					for (int k = 0; k < vObj.size(); k++)
						if (!closeToPoint[k]) {
							RealRectangle bo = vObj.elementAt(k).getBounds();
							if (bo == null) {
								continue;
							}
							if (closestIdx >= 0) {
								if (Math.min(Math.abs(p1.x - bo.rx1), Math.abs(p1.x - bo.rx2)) >= d0 && Math.min(Math.abs(p1.y - bo.ry1), Math.abs(p1.y - bo.ry2)) >= d0) {
									continue;
								}
								double dist = Computing.distance(p1.x, p1.y, bo, false); //estimation: distance to bounding rectangle
								if (dist >= d0) {
									continue;
								}
							}
							Geometry geom = vObj.elementAt(k).getGeometry();
							double dist = Computing.distance(p1.x, p1.y, geom, false); //Euclidean distance to geometry
							if (closestIdx < 0 || d0 > dist) {
								closestIdx = k;
								d0 = dist;
							}
						}
				}
				if (closestIdx < 0) {
					continue;
				}
				if (!isGeo) {
					d[i] = d0;
				} else {
					Geometry geom = vObj.elementAt(closestIdx).getGeometry();
					d[i] = Computing.distance(p1.x, p1.y, geom, true);
				}
			}
		if (isGeo) {
			for (int i = 0; i < d.length; i++) {
				d[i] /= 1000d;
			}
		}
		return d;
	}

	/**
	 * returns paths made during the time interval of the specified length
	 * before each point
	 */
	public double[] getPathLengthInInterval(int iLen, char unit) {
		getDistances();
		if (distances == null)
			return null;
		double d[] = new double[track.size()];
		for (int j = 0; j < d.length; j++) {
			d[j] = Double.NaN;
		}
		double tolerance = 0.2 * iLen;
		TimeReference tr0 = getPositionTime(0);
		TimeMoment t0 = tr0.getValidUntil();
		if (t0 == null) {
			t0 = tr0.getValidFrom();
		}
		int idx0 = 0;
		for (int i = 1; i < d.length; i++) {
			TimeReference tr = getPositionTime(i);
			TimeMoment t = tr.getValidFrom();
			long tDiff = t.subtract(t0, unit);
			while (tDiff > iLen && idx0 + 1 < i) {
				TimeReference tr1 = getPositionTime(idx0 + 1);
				TimeMoment t1 = tr1.getValidUntil();
				if (t1 == null) {
					t1 = tr1.getValidFrom();
				}
				long t1Diff = t.subtract(t1, unit);
				boolean takeNext = t1Diff >= iLen;
				if (!takeNext) {
					double relDiff = (tDiff - iLen) * 1.0 / iLen;
					takeNext = relDiff > tolerance;
				}
				if (!takeNext) {
					break;
				}
				tr0 = tr1;
				t0 = t1;
				++idx0;
				tDiff = t1Diff;
			}
			double relDiff = Math.abs(tDiff - iLen) * 1.0 / iLen;
			if (relDiff <= tolerance) {
				d[i] = 0;
				for (int j = idx0; j < i; j++) {
					d[i] += distances[j];
				}
			}
		}
		if (isGeo) {
			for (int i = 0; i < d.length; i++) {
				d[i] /= 1000d;
			}
		}
		return d;
	}

	/**
	 * returns distances to the set of events, i.e. distances to the closest event in space
	 */
	public double[] getDistancesToEvents(Vector<DGeoObject> events, int tolerance, char unit) {
		if (events == null || events.size() == 0)
			return null;
		double d[] = new double[track.size()];
		for (int j = 0; j < d.length; j++) {
			d[j] = Double.NaN;
		}
		for (int i = 0; i < events.size(); i++) {
			Geometry geom = events.elementAt(i).getGeometry();
			if (geom == null) {
				continue;
			}
			TimeReference etr = events.elementAt(i).getTimeReference();
			if (etr == null) {
				continue;
			}
			for (int j = 0; j < track.size(); j++) {
				SpatialEntity se = (SpatialEntity) track.elementAt(j);
				TimeReference ttr = se.getTimeReference();
				if (ttr == null) {
					continue;
				}
				long diff = TimeReference.getTemporalDistance(ttr, etr, unit);
				if (Math.abs(diff) <= tolerance) {
					RealPoint p0 = se.getCentre();
					double ddd = Computing.distance(p0.x, p0.y, geom, isGeo);
					if (isGeo) {
						ddd /= 1000;
					}
					if (Double.isNaN(d[j]) || d[j] > ddd) {
						d[j] = ddd;
					}
				}
			}
		}
		return d;
	}

	/**
	 * returns spatial distances to the nearest in time events from the given set
	 * @param events - the set of events
	 * @param past - whether to compute the distances to the past events
	 * @param future - whether to compute the distances to the future events
	 * @param maxDistTime - temporal threshold: if the time from/to an event exceeds this,
	 *   the spatial distance is not computed. If the value is negative, it is ignored.
	 * @param unit - in which units the temporal threshold is specified
	 * @return for each point of the trajectory, the spatial distance to the nearest in time event
	 */
	public double[] getDistancesToEvents(Vector<DGeoObject> events, boolean past, boolean future, int maxDistTime, char unit) {
		if (events == null || events.size() == 0)
			return null;
		double d[] = new double[track.size()];
		for (int j = 0; j < d.length; j++) {
			d[j] = Double.NaN;
			SpatialEntity se = (SpatialEntity) track.elementAt(j);
			RealPoint p0 = se.getCentre();
			TimeReference ttr = se.getTimeReference();
			//find the nearest in time event
			long minTDist = Long.MAX_VALUE;
			for (int i = 0; i < events.size(); i++) {
				Geometry geom = events.elementAt(i).getGeometry();
				if (geom == null) {
					continue;
				}
				TimeReference etr = events.elementAt(i).getTimeReference();
				if (etr == null) {
					continue;
				}
				long tDist = TimeReference.getTemporalDistance(ttr, etr);
				if (tDist != 0) {
					if (tDist < 0 && !future) {
						continue;
					}
					if (tDist > 0 && !past) {
						continue;
					}
					if (maxDistTime >= 0 && unit != ttr.getValidFrom().getPrecision()) {
						long d1 = TimeReference.getTemporalDistance(ttr, etr, unit);
						if (Math.abs(d1) > maxDistTime) {
							continue;
						}
					}
				}
				tDist = Math.abs(tDist);
				if (tDist <= minTDist) {
					double ddd = Computing.distance(p0.x, p0.y, geom, isGeo);
					if (isGeo) {
						ddd /= 1000;
					}
					if (Double.isNaN(d[j]) || tDist < minTDist || d[j] > ddd) {
						d[j] = ddd;
					}
					minTDist = tDist;
				}
			}
		}
		return d;
	}

	/**
	 * Returns temporal distances from all points to the given time moment
	 */
	public double[] getTemporalDistancesToTimeMoment(TimeMoment ttt, char unit) {
		if (ttt == null || !ttt.isValid())
			return null;
		TimeReference tr = new TimeReference(ttt, null);
		double d[] = new double[track.size()];
		for (int j = 0; j < d.length; j++) {
			d[j] = Double.NaN;
			SpatialEntity se = (SpatialEntity) track.elementAt(j);
			TimeReference ttr = se.getTimeReference();
			d[j] = TimeReference.getTemporalDistance(ttr, tr, unit);
		}
		return d;
	}

	/**
	 * returns temporal distances to the nearest in time events from the given set
	 * @param events - the set of events
	 * @param past - whether to compute the distances to the past events
	 * @param future - whether to compute the distances to the future events
	 * @param unit - in which units the distance is specified
	 * @return for each point of the trajectory, the temporal distance to the nearest in time event
	 */
	public double[] getTemporalDistancesToEvents(Vector<DGeoObject> events, boolean past, boolean future, char unit) {
		if (events == null || events.size() == 0)
			return null;
		double d[] = new double[track.size()];
		for (int j = 0; j < d.length; j++) {
			d[j] = Double.NaN;
			SpatialEntity se = (SpatialEntity) track.elementAt(j);
			TimeReference ttr = se.getTimeReference();
			//find the nearest in time event
			long minTDist = Long.MAX_VALUE;
			for (int i = 0; i < events.size(); i++) {
				Geometry geom = events.elementAt(i).getGeometry();
				if (geom == null) {
					continue;
				}
				TimeReference etr = events.elementAt(i).getTimeReference();
				if (etr == null) {
					continue;
				}
				long tDist = TimeReference.getTemporalDistance(ttr, etr);
				if (tDist != 0) {
					if (tDist < 0 && !future) {
						continue;
					}
					if (tDist > 0 && !past) {
						continue;
					}
				}
				if (Math.abs(tDist) <= minTDist) {
					if (unit != ttr.getValidFrom().getPrecision()) {
						d[j] = TimeReference.getTemporalDistance(ttr, etr, unit);
					} else {
						d[j] = tDist;
					}
					minTDist = Math.abs(tDist);
				}
			}
		}
		return d;
	}

	/**
	 * returns distances to the set of trajectories, i.e. distances to the closest point
	 * of the closest trajectory at every time moment
	 */
	public double[] getDistancesToTrajectories(Vector<DGeoObject> vp, int tolerance, char unit) {
		if (vp == null || vp.size() == 0)
			return null;
		double d[] = new double[track.size()];
		for (int j = 0; j < d.length; j++) {
			d[j] = Double.NaN;
		}
		for (int i = 0; i < vp.size(); i++)
			if (vp.elementAt(i) instanceof DMovingObject && !this.equals(vp.elementAt(i))) {
				DMovingObject dmo = (DMovingObject) vp.elementAt(i);
				long diff = dmo.getStartTime().subtract(this.getEndTime(), unit);
				if (diff > tolerance) {
					continue;
				}
				diff = this.getStartTime().subtract(dmo.getEndTime(), unit);
				if (diff > tolerance) {
					continue;
				}
				double dd[] = getDistancesToTrajectory(dmo, tolerance, unit);
				for (int j = 0; j < d.length; j++)
					if (Double.isNaN(d[j]) || dd[j] < d[j]) {
						d[j] = dd[j];
					}
			}
		return d;
	}

	public double[] getDistancesToTrajectory(DMovingObject dmo, int tolerance, char unit) {
		double d[] = new double[track.size()];
		for (int j = 0; j < d.length; j++) {
			d[j] = Double.NaN;
		}
		int idx = 0; // idx - pointer to points of the <dmo> track
		int tr2Size = dmo.getTrack().size();
		for (int i = 0; i < d.length; i++) { // i - pointer to points of the <track>
			if (idx >= tr2Size) {
				break;
			}
			SpatialEntity se = (SpatialEntity) track.elementAt(i);
			TimeMoment tm = se.getTimeReference().getValidFrom();
			long timeDiff = tm.subtract(dmo.getPositionTime(idx).getValidFrom(), unit);
			if (timeDiff < -tolerance) {
				continue;
			}
			while (timeDiff > tolerance && idx < tr2Size) {
				//if the position in the second trajectory is much earlier, scan the second trajectory
				//until a suitable position is found
				idx++;
				if (idx < tr2Size) {
					timeDiff = tm.subtract(dmo.getPositionTime(idx).getValidFrom(), unit);
				}
			}
			if (idx >= tr2Size) {
				break;
			}
			RealPoint p0 = se.getCentre();
			for (int j = idx; j < tr2Size && Math.abs(timeDiff) <= tolerance; j++) {
				RealPoint p1 = ((SpatialEntity) dmo.getTrack().elementAt(j)).getCentre();
				double dist = GeoComp.distance(p0.x, p0.y, p1.x, p1.y, isGeo);
				if (!Double.isNaN(dist) && (Double.isNaN(d[i]) || d[i] > dist)) {
					d[i] = dist;
				}
				if (j + 1 < tr2Size) {
					timeDiff = tm.subtract(dmo.getPositionTime(j + 1).getValidFrom(), unit);
				}
			}
			if (isGeo) {
				d[i] /= 1000d;
			}
		}
		return d;
	}

	/**
	 * Returns the distances to the given point
	 */
	public double[] getDistancesToPoint(RealPoint p0) {
		double d[] = new double[track.size()];
		for (int i = 0; i < track.size(); i++) {
			RealPoint p1 = ((SpatialEntity) track.elementAt(i)).getCentre();
			double dist = GeoComp.distance(p0.x, p0.y, p1.x, p1.y, isGeo);
			d[i] = dist;
		}
		if (isGeo) {
			for (int i = 0; i < d.length; i++) {
				d[i] /= 1000d;
			}
		}
		return d;
	}

	/**
	 * Returns the distances to the given geometry
	 */
	public double[] getDistancesToGeometry(Geometry geom) {
		double d[] = new double[track.size()];
		for (int i = 0; i < track.size(); i++) {
			RealPoint p1 = ((SpatialEntity) track.elementAt(i)).getCentre();
			double dist = Computing.distance(p1.x, p1.y, geom, isGeo);
			d[i] = dist;
		}
		if (isGeo) {
			for (int i = 0; i < d.length; i++) {
				d[i] /= 1000d;
			}
		}
		return d;
	}

	/**
	 * Returns the total length of the track. If not computed yet, computes.
	 */
	public double getTrackLength() {
		if (!Double.isNaN(trackLength))
			return trackLength;
		if (track == null || track.size() < 2)
			return 0;
		getDistances();
		return trackLength;
	}

	/**
	 * Returns the track length for the given time interval
	 */
	public double getTrackLength(TimeMoment from, TimeMoment to) {
		if (track == null || track.size() < 2)
			return 0;
		if (from == null || to == null)
			return 0;
		if (startTime == null || endTime == null)
			return 0;
		if (from.compareTo(to) >= 0)
			return 0;
		if (startTime.compareTo(to) >= 0)
			return 0; //starts later than this interval
		if (endTime.compareTo(from) <= 0)
			return 0; //ends before this interval
		getDistances();
		if (distances == null || distances.length < 1)
			return 0;
		int i0 = -1, iEnd = -1;
		for (int i = 1; i < track.size() && iEnd < 0; i++) {
			TimeReference tref = getPositionTime(i);
			if (tref == null) {
				continue;
			}
			if (i0 < 0)
				if (tref.getValidUntil().compareTo(from) > 0)
					if (tref.getValidFrom().compareTo(to) > 0)
						return 0;
					else {
						i0 = i;
					}
				else {
					continue;
				}
			else if (tref.getValidFrom().compareTo(to) > 0) {
				iEnd = i - 1;
			}
		}
		if (i0 < 1)
			return 0;
		if (iEnd < 0) {
			iEnd = track.size() - 1;
		}
		double d = 0;
		long iLen = to.subtract(from);
		for (int i = i0; i <= iEnd; i++) {
			TimeReference tref = getPositionTime(i);
			if (tref == null) {
				continue;
			}
			double dAdd = distances[i - 1];
			if (dAdd <= 0) {
				continue;
			}
			if (i == i0) {
				long diff = tref.getValidUntil().subtract(from);
				if (diff <= 0) {
					continue;
				}
				if (diff < iLen) {
					TimeReference trPrev = getPositionTime(i - 1);
					if (trPrev != null) {
						long diffPrev = tref.getValidFrom().subtract(trPrev.getValidUntil());
						if (diffPrev > diff) {
							dAdd = dAdd * diff / diffPrev;
						}
					}
				}
			}
			d += dAdd;
		}
		if (iEnd < track.size() - 1) {
			TimeReference tref = getPositionTime(iEnd);
			long diff = to.subtract(tref.getValidUntil());
			if (diff > 0) {
				TimeReference trNext = getPositionTime(iEnd + 1);
				if (trNext != null) {
					long diffNext = trNext.getValidFrom().subtract(tref.getValidUntil());
					if (diffNext > diff) {
						d += distances[iEnd] * diff / diffNext;
					}
				}
			}
		}
		return d;
	}

	/**
	 * Returns the length of the path travelled by the given time moment
	 */
	public double getTrackLengthBy(TimeMoment to) {
		if (track == null || track.size() < 2)
			return 0;
		if (to == null)
			return 0;
		if (startTime == null || endTime == null)
			return 0;
		if (startTime.compareTo(to) >= 0)
			return 0; //starts later than "to"
		getDistances();
		if (distances == null || distances.length < 1)
			return 0;
		double d = 0;
		for (int i = 1; i < track.size(); i++) {
			TimeReference tref = getPositionTime(i);
			if (tref == null) {
				continue;
			}
			long diff = tref.getValidFrom().subtract(to);
			if (diff <= 0) {
				d += distances[i - 1];
			}
			if (diff >= 0) {
				break;
			}
		}
		return d;
	}

	/**
	 * On the basis of the statistics of the distances between the consecutive positions,
	 * removes the outliers from the trajectory, i.e. points with abnormally high distances
	 * to the previous points.
	 * Returns true if changed.
	 */
	public boolean removeOutliers(double maxSpeed) {
		getDistances();
		if (distances == null || distances.length < 3)
			return false;
		double speeds[] = getSpeeds();
		int length = track.size() - 1;
/*
    DoubleArray sp =new DoubleArray(length,1);
    for (int i=0; i<length; i++)
      sp.addElement(speeds[i]);
    int perc[]={25,75,100};
    double stat[]= NumValManager.getPercentiles(sp,perc);
    if (stat==null) return false;
    double qDiff=stat[1]-stat[0]; //inter-quartile difference
    double maxSpeed =stat[1]+3*qDiff;
    if (stat[2]<=maxSpeed) return false; //no outliers
*/
		double max = 0;
		for (int i = 0; i < length; i++)
			if (!Double.isNaN(speeds[i]) && max < speeds[i]) {
				max = speeds[i];
			}
		if (max <= maxSpeed)
			return false; //no outliers

		TimeMoment tm = ((SpatialEntity) track.elementAt(0)).getTimeReference().getValidFrom();
		boolean isPhysicalTime = (tm instanceof Date) ? true : false;

		//removing outliers
		Vector cleanTrack = new Vector(track.size(), 1);
		int lastAddedIdx = -1;
		for (int i = 0; i < length && lastAddedIdx < 0; i++)
			if (Double.isNaN(speeds[i]) || speeds[i] <= maxSpeed) {
				cleanTrack.addElement(track.elementAt(i));
				lastAddedIdx = i;
			}
		int lastToAdd = -1;
		for (int i = length - 1; i >= 0 && lastToAdd < 0; i--)
			if (Double.isNaN(speeds[i]) || speeds[i] <= maxSpeed) {
				lastToAdd = i + 1;
			}
		if (lastToAdd <= lastAddedIdx)
			return false;
		while (lastAddedIdx < lastToAdd) {
			for (int i = lastAddedIdx + 1; i <= lastToAdd && (Double.isNaN(speeds[i - 1]) || speeds[i - 1] <= maxSpeed); i++) {
				cleanTrack.addElement(track.elementAt(i));
				lastAddedIdx = i;
			}
			if (lastAddedIdx < lastToAdd) {
				boolean found = false;
				SpatialEntity sp0 = (SpatialEntity) cleanTrack.elementAt(cleanTrack.size() - 1);
				RealPoint p0 = sp0.getCentre();
				TimeMoment t0 = sp0.getTimeReference().getValidFrom();
				for (int i = lastAddedIdx + 1; i < lastToAdd && !found; i++)
					if (Double.isNaN(speeds[i]) || speeds[i] <= maxSpeed) {
						SpatialEntity sp1 = (SpatialEntity) track.elementAt(i);
						RealPoint p1 = sp1.getCentre();
						TimeMoment t1 = sp1.getTimeReference().getValidFrom();
						double distance = GeoComp.distance(p0.x, p0.y, p1.x, p1.y, isGeo);
						if (isGeo) {
							distance /= 1000;
						}
						long tDiff = t1.subtract(t0);
						if (tDiff < 1) {
							tDiff = 1;
						}
						if (isPhysicalTime) {
							switch (tm.getPrecision()) { // 'y','m','d','h','t','s'
							case 't':
								tDiff /= 60;
								break;
							case 's':
								tDiff /= 60 * 60;
								break;
							default:
								break;
							}
						}
						double speed = distance / tDiff;
						if (speed <= maxSpeed) {
							found = true;
							cleanTrack.addElement(track.elementAt(i));
							lastAddedIdx = i;
						}
					}
				if (!found) {
					for (int i = lastAddedIdx + 1; i < lastToAdd && !found; i++)
						if (Double.isNaN(speeds[i]) || speeds[i] <= maxSpeed) {
							found = true;
							cleanTrack.addElement(track.elementAt(i));
							lastAddedIdx = i;
						}
				}
				if (!found) {
					break;
				}
			}
		}
		if (cleanTrack.size() == track.size())
			return false;
		this.setTrack(cleanTrack);
		return true;
	}
}
