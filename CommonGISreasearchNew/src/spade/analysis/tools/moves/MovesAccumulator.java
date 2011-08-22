package spade.analysis.tools.moves;

import java.util.Vector;

import spade.analysis.geocomp.voronoi.VoronoiNew;
import spade.analysis.tools.clustering.PointOrganizerSpatialIndex;
import spade.lib.util.GeoDistance;
import spade.lib.util.IntArray;
import spade.lib.util.ObjectWithMeasure;
import spade.time.TimeMoment;
import spade.time.TimeUtil;
import spade.vis.database.SpatialEntity;
import spade.vis.dmap.DAggregateLinkObject;
import spade.vis.dmap.DLinkObject;
import spade.vis.dmap.DMovingObject;
import spade.vis.dmap.DPlaceVisitsObject;
import spade.vis.geometry.GeoComp;
import spade.vis.geometry.RealPoint;
import spade.vis.geometry.RealPolyline;
import spade.vis.geometry.RealRectangle;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Mar 12, 2009
 * Time: 10:18:15 AM
 * Used for summarising trajectories in an incremental mode, one by one.
 * E.g. the trajectories may come from a database.
 */
public class MovesAccumulator {
	/**
	 * Whether the Accumulator produces dynamic aggregators.
	 * By default, static aggregates are computed.
	 */
	protected boolean makeDynamicAggregates = false;
	/**
	 * The original trajectories, which are used for building the Voronoi cells
	 */
	protected Vector<DMovingObject> origTrajectories = null;
	/**
	 * When the Accumulator is used for summarising trajectories from a specific
	 * class or cluster, this is the label of the class (cluster)
	 */
	public String cLabel = null;
	/**
	 * When the Accumulator is used for summarising trajectories from a specific
	 * class or cluster, this is the index of the class (cluster)
	 */
	public int cIdx = -1;
	/**
	 * The number of accumulated trajectories
	 */
	public long count = 0;
	/**
	 * Whether the coordinates in the accumulated trajectories are geographic,
	 * i.e. latituides and longitudes
	 */
	public boolean geo = false;
	/**
	 * Whether only start and end points of the trajectories must be taken into account
	 */
	public boolean useOnlyStartsEnds = false;
	/**
	 * Whether the intersections of the trajectories with the areas must be
	 * computed and used.
	 */
	public boolean findIntersections = true;
	/**
	 * The areas (polygons) used for summarising the trajectories.
	 * Note: some elements may be null! (result of Voronoi tesselation)
	 */
	public Vector<DPlaceVisitsCounter> sumPlaces = null;
	/**
	 * Matrix of neighbourhood between the places
	 */
	protected boolean neiMatrix[][] = null;
	/**
	 * The moves between the generalised places, extracted from the trajectories
	 */
	public Vector<DLinkObject> sumMoves = null;
	/**
	 * In case of dynamic aggregations, these are dynamically aggregated moves
	 */
	public Vector<DAggregateLinkObject> aggLinks = null;
	//---- In case of aggregation by time intervals, the parameters of the temporal aggregation ----
	/**
	 * Whether to aggregate by time intervals
	 */
	public boolean aggregateByTimeIntervals = false;
	/**
	 * The start and end of the whole time span to be considered
	 */
	public TimeMoment tStart = null, tEnd = null;
	/**
	 * The temporal breaks
	 */
	public Vector<TimeMoment> timeBreaks = null;
	/**
	 * Whether cyclic time fivision is used
	 */
	public boolean useCycle = false;
	/**
	 * The unit of the selected cycle elements
	 */
	public char cycleUnit = 'u';
	/**
	 * Length of the cycle, i.e. number of elements (e.g. days in week)
	 */
	public int nCycleElements = 0;
	/**
	 * The name of the cycle, if used
	 */
	public String cycleName = null;
	//---- In case of aggregation by time intervals, the structures used for counting
	/**
	 * The counts of visitors and visits by the areas and time intervals
	 */
	protected int visitors[][] = null, visits[][] = null;
	/**
	 * The total counts of visitors and visits by the areas
	 */
	protected int totalVisitors[] = null, totalVisits[] = null;
	/**
	 * The counts of trajectories and moves by time intervals for each element of sumMoves
	 */
	protected Vector<int[]> trajByTime = null, movesByTime = null;
	/**
	 * The total counts of trajectories and moves for each element of sumMoves
	 */
	protected IntArray totalTraj = null, totalMoves = null;
	/**
	 * Whether to count visits in neighbouring places
	 */
	public boolean countVisitsAround = false;
	/**
	 * Indicates whether information about neighbours of the areas is available
	 */
	protected boolean hasNeiInfo = false;
	/**
	 * The counts of visitors and visits by the areas together with the neighbours and by the time intervals
	 */
	protected int neiVisitors[][] = null, neiVisits[][] = null;
	/**
	 * The total counts of visitors and visits by the areas together with the neighbours
	 */
	protected int neiTotalVisitors[] = null, neiTotalVisits[] = null;

	/**
	 * Whether the Accumulator produces dynamic aggregators.
	 * By default, static aggregates are computed.
	 */
	public boolean makesDynamicAggregates() {
		return makeDynamicAggregates;
	}

	/**
	 * Whether the Accumulator produces dynamic aggregators.
	 */
	public void setMakeDynamicAggregates(boolean makeDynamicAggregates) {
		this.makeDynamicAggregates = makeDynamicAggregates;
	}

	/**
	 * Whether only start and end points of the trajectories must be taken into account
	 */
	public void setUseOnlyStartsEnds(boolean useOnlyStartsEnds) {
		this.useOnlyStartsEnds = useOnlyStartsEnds;
	}

	/**
	 * Whether the intersections of the trajectories with the areas must be
	 * computed and used.
	 */
	public void setFindIntersections(boolean findIntersections) {
		this.findIntersections = findIntersections;
	}

	/**
	 * Whether to count visits in neighbouring places
	 */
	public void setCountVisitsAround(boolean countVisitsAround) {
		this.countVisitsAround = countVisitsAround;
	}

	/**
	 * Whether the coordinates in the accumulated trajectories are geographic,
	 * i.e. latituides and longitudes
	 */
	public void setGeo(boolean geo) {
		this.geo = geo;
	}

	/**
	 * The areas (polygons) used for summarising the trajectories.
	 * Note: some elements may be null! (result of Voronoi tesselation)
	 */
	public void setSumPlaces(Vector<DPlaceVisitsCounter> sumPlaces) {
		this.sumPlaces = sumPlaces;
	}

	/**
	 * Matrix of neighbourhood between the places
	 */
	public void setNeiMatrix(boolean[][] neiMatrix) {
		this.neiMatrix = neiMatrix;
	}

	/**
	 * Adds a trajectory to the original trajectories, which are used for building
	 * the Voronoi cells
	 */
	public void addOriginalTrajectory(DMovingObject tr) {
		if (tr == null || tr.getTrack() == null || tr.getTrack().size() < 1)
			return;
		if (origTrajectories == null) {
			origTrajectories = new Vector<DMovingObject>(100, 100);
		}
		origTrajectories.addElement(tr);
	}

	/**
	 * The original trajectories, which are used for building the Voronoi cells
	 */
	public Vector<DMovingObject> getOrigTrajectories() {
		return origTrajectories;
	}

	/**
	 * The original trajectories, which are used for building the Voronoi cells
	 */
	public void setOrigTrajectories(Vector<DMovingObject> origTrajectories) {
		this.origTrajectories = origTrajectories;
	}

	/**
	 * Builds generalized places from the original set of trajectories,
	 * which has been previously provided
	 * @param geo - whether the coordinates in the trajectories are geographic
	 * @param angle - the minimum angle, in degrees, of turn considered as significant,
	 *   i.e. the point of this turn will be extracted as a characteristic point
	 * @param stopTime - the minimum stop time considered as significant,
	 *   i.e. the point of the stop will be extracted as a characteristic point
	 * @param minDist - to disregard small changes of positions in extracting
	 *   characteristic points from the trajectories
	 * @param maxDist - maximum distance to keep between the extracted characteristic
	 *   points of the trajectories
	 * @param clRadius - the desired cluster radius for clustering the characteristic points
	 * @param useOnlyStartsEnds - whether only start and end points of the trajectories
	 *    must be taken into account
	 * @param terrBounds - the bounding rectangle of the whole territory
	 * @return true if successful
	 */
	public boolean buildAreas(boolean geo, double angle, long stopTime, float minDist, float maxDist, float clRadius, boolean useOnlyStartsEnds, RealRectangle terrBounds) {
		return buildAreas(origTrajectories, geo, angle, stopTime, minDist, maxDist, clRadius, useOnlyStartsEnds, terrBounds);
	}

	/**
	 * Uses the given set of trajectories to build the generalised places (areas),
	 * which will be than used for summarising trajectories.
	 * @param trajectories - the trajectories used to build generalised places
	 * @param geo - whether the coordinates in the trajectories are geographic
	 * @param angle - the minimum angle, in degrees, of turn considered as significant,
	 *   i.e. the point of this turn will be extracted as a characteristic point
	 * @param stopTime - the minimum stop time considered as significant,
	 *   i.e. the point of the stop will be extracted as a characteristic point
	 * @param minDist - to disregard small changes of positions in extracting
	 *   characteristic points from the trajectories
	 * @param maxDist - maximum distance to keep between the extracted characteristic
	 *   points of the trajectories
	 * @param clRadius - the desired cluster radius for clustering the characteristic points
	 * @param useOnlyStartsEnds - whether only start and end points of the trajectories
	 *    must be taken into account
	 * @param terrBounds - the bounding rectangle of the whole territory
	 * @return true if successful
	 */
	public boolean buildAreas(Vector<DMovingObject> trajectories, boolean geo, double angle, long stopTime, float minDist, float maxDist, float clRadius, boolean useOnlyStartsEnds, RealRectangle terrBounds) {
		if (trajectories == null || trajectories.size() < 1)
			return false;
		this.geo = geo;
		this.useOnlyStartsEnds = useOnlyStartsEnds;

		float clMinX = Float.NaN, clMaxX = clMinX, clMinY = clMinX, clMaxY = clMinX;
		for (int i = 0; i < trajectories.size(); i++) {
			Vector track = trajectories.elementAt(i).getTrack();
			if (track == null) {
				continue;
			}
			for (int j = 0; j < track.size(); j++) {
				RealPoint p = ((SpatialEntity) track.elementAt(j)).getCentre();
				if (p == null) {
					continue;
				}
				if (Float.isNaN(clMinX) || clMinX > p.x) {
					clMinX = p.x;
				}
				if (Float.isNaN(clMaxX) || clMaxX < p.x) {
					clMaxX = p.x;
				}
				if (Float.isNaN(clMinY) || clMinY > p.y) {
					clMinY = p.y;
				}
				if (Float.isNaN(clMaxY) || clMaxY < p.y) {
					clMaxY = p.y;
				}
			}
		}
		if (Float.isNaN(clMinX) || Float.isNaN(clMinY))
			return false;
		if (terrBounds == null) {
			terrBounds = new RealRectangle(clMinX, clMinY, clMaxX, clMaxY);
		}
		this.geo = geo;
		float minx = terrBounds.rx1, miny = terrBounds.ry1, maxx = terrBounds.rx2, maxy = terrBounds.ry2;
		float width, height, geoFactorX = 1f, geoFactorY = 1f;
		if (geo) {
			float my = (miny + maxy) / 2;
			width = (float) GeoDistance.geoDist(minx, my, maxx, my);
			float mx = (minx + maxx) / 2;
			height = (float) GeoDistance.geoDist(mx, miny, mx, maxy);
			geoFactorX = width / (maxx - minx);
			geoFactorY = height / (maxy - miny);
		} else {
			width = maxx - minx;
			height = maxy - miny;
		}
		float minDistOrig = minDist, maxDistOrig = maxDist, clRadiusOrig = clRadius;
		minDist /= geoFactorX;
		maxDist /= geoFactorX;
		clRadius /= geoFactorX;
		float clRadiusY = clRadiusOrig / geoFactorY;
		double angleRadian = angle * Math.PI / 180;

		PointOrganizerSpatialIndex pOrg = new PointOrganizerSpatialIndex();
		pOrg.setSpatialExtent(minx, miny, maxx, maxy);
		//since the Voronoi polygons are built on the basis of Euclidean distances,
		//we should use Euclidean distances also in point clustering
		pOrg.setMaxRad(Math.min(clRadius, clRadiusY));
		pOrg.setGeo(false, 1, 1);
		if (useOnlyStartsEnds) {
			for (int i = 0; i < trajectories.size(); i++) {
				Vector track = trajectories.elementAt(i).getTrack();
				if (track == null || track.size() < 2) {
					continue;
				}
				pOrg.addPoint(((SpatialEntity) track.elementAt(0)).getCentre());
				pOrg.addPoint(((SpatialEntity) track.elementAt(track.size() - 1)).getCentre());
			}
		} else {
			//preliminary simplification: extraction of characteristic points
			for (int i = 0; i < trajectories.size(); i++) {
				//Vector genTrack=trajectories.elementAt(i).generaliseTrack
				//(geoFactorX,stopTime,angleRadian,minRad,Math.min(maxRad,maxRadY));
				Vector genTrack = TrUtil.getCharacteristicPoints(trajectories.elementAt(i).getTrack(), angleRadian, stopTime, minDistOrig, maxDistOrig, geoFactorX, geoFactorY);
				if (genTrack != null) {
					for (int j = 0; j < genTrack.size(); j++) {
						pOrg.addPoint(((SpatialEntity) genTrack.elementAt(j)).getCentre());
					}
				}
			}
		}
		//pOrg.mergeCloseGroups();
		pOrg.reDistributePoints();
		pOrg.optimizeGrouping();
		int nGroups = pOrg.getGroupCount();
		Vector<RealPoint> points = new Vector<RealPoint>(nGroups, 10);
		for (int i = 0; i < nGroups; i++) {
			points.addElement(pOrg.getCentroid(i));
		}
		//introducing additional points in empty areas and on the boundaries
		float dy = 2 * clRadiusY, dx = 2 * clRadius, dx2 = dx / 2, dy2 = dy / 2;
		float y1 = miny - dy - dy2, y2 = maxy + dy + dy2;
		float x1 = minx - dx - dx2, x2 = maxx + dx + dx2;
		int k = 0;
		for (float y = y1; y <= y2 + dy2; y += dy) {
			float ddx = (k % 2 == 0) ? 0 : dx2;
			++k;
			for (float x = x1 + ddx; x <= x2 + dx2; x += dx)
				if (pOrg.isFarFromAll(x, y)) {
					points.addElement(new RealPoint(x, y));
				}
		}
		pOrg = null;
		VoronoiNew voronoi = new VoronoiNew(points);
		if (!voronoi.isValid())
			return false;
		voronoi.setBuildNeighbourhoodMatrix(true);
		RealPolyline areas[] = voronoi.getPolygons(x1, y1, x2, y2);
		if (areas == null)
			return false;
//    boolean neiMatrix[][]=voronoi.getNeighbourhoodMatrix();
		//exclude the neibourhood relations with the boarding polygons
		/*
		if (neiMatrix!=null)
		  for (k=0; k<areas.length; k++)
		    if (areas[k]!=null) {
		      float cnt[]=areas[k].getCentroid();
		      if (cnt[0]<minx || cnt[0]>maxx || cnt[1]<miny || cnt[1]>maxy) {
		        for (int n=0; n<areas.length; n++)
		          if (n!=k) {
		            neiMatrix[n][k]=false;
		            neiMatrix[k][n]=false;
		          }
		        //areas[k]=null;
		      }
		    }
		*/
		sumPlaces = new Vector<DPlaceVisitsCounter>(areas.length, 10);
		int nPlaces = 0;
		if (cLabel == null) {
			cLabel = "";
		}
		for (int i = 0; i < areas.length; i++)
			if (areas[i] != null) {
				SpatialEntity spe = new SpatialEntity(cLabel + "_" + String.valueOf(i + 1));
				spe.setGeometry(areas[i]);
				DPlaceVisitsCounter obj = null;
				if (this.makeDynamicAggregates) {
					obj = new DPlaceVisitsObject();
				} else {
					obj = new DPlaceVisitsCounter();
				}
				obj.setup(spe);
				sumPlaces.addElement(obj);
				++nPlaces;
			} else {
				sumPlaces.addElement(null);
			}
		if (nPlaces < 2) {
			sumPlaces = null;
			return false;
		}
		//adding information about the neighbours of the places
		if (neiMatrix != null) {
			for (int i = 0; i < areas.length; i++)
				if (areas[i] != null) {
					DPlaceVisitsCounter pObj = sumPlaces.elementAt(i);
					for (int j = 0; j < areas.length; j++)
						if (j != i && neiMatrix[i][j] && areas[j] != null) {
							pObj.addNeighbour(sumPlaces.elementAt(j));
						}
				}
		}

		return isReady();
	}

	/**
	 * Whether this accumulator has everything needed for summarising trajectories
	 */
	public boolean isReady() {
		return sumPlaces != null && sumPlaces.size() > 1;
	}

	/**
	 * @param start - start of the whole time span (what is before must be ignored)
	 * @param end - end of the whole time span (what is after must be ignored)
	 * @param breaks - the breaks that divide the time span into intervals
	 * @param useCycle - whether the division is done according to the cyclical time model
	 * @param cycleUnit - units of the cycle
	 * @param nCycleElements - length of the cycle
	 * @param cycleName - name of the cycle
	 */
	public void setTemporalAggregationParameters(TimeMoment start, TimeMoment end, Vector<TimeMoment> breaks, boolean useCycle, char cycleUnit, int nCycleElements, String cycleName) {
		this.tStart = start;
		this.tEnd = end;
		this.timeBreaks = breaks;
		this.useCycle = useCycle;
		this.cycleUnit = cycleUnit;
		this.nCycleElements = nCycleElements;
		this.cycleName = cycleName;
		aggregateByTimeIntervals = start != null && breaks != null;
	}

	/**
	 * Allocates structures for counting by time intervals
	 */
	protected void prepareTemporalAggregation() {
		if (!aggregateByTimeIntervals)
			return;
		if (visitors != null)
			return;
		if (sumPlaces == null)
			return;
		int nAreas = sumPlaces.size();
		if (nAreas < 1)
			return;
		int nIntervals = timeBreaks.size();
		if (!useCycle) {
			++nIntervals;
		}
		visitors = new int[nAreas][nIntervals];
		visits = new int[nAreas][nIntervals];
		totalVisitors = new int[nAreas];
		totalVisits = new int[nAreas];
		for (int i = 0; i < nAreas; i++) {
			totalVisitors[i] = 0;
			totalVisits[i] = 0;
			for (int j = 0; j < nIntervals; j++) {
				visitors[i][j] = 0;
				visits[i][j] = 0;
			}
		}
		if (!countVisitsAround)
			return;
		boolean hasNeiInfo = false;
		for (int i = 0; i < sumPlaces.size() && !hasNeiInfo; i++) {
			DPlaceVisitsCounter place = sumPlaces.elementAt(i);
			hasNeiInfo = place.neighbours != null && place.neighbours.size() > 0;
		}
		if (hasNeiInfo) {
			neiVisitors = new int[nAreas][nIntervals];
			neiVisits = new int[nAreas][nIntervals];
			neiTotalVisitors = new int[nAreas];
			neiTotalVisits = new int[nAreas];
			for (int i = 0; i < nAreas; i++) {
				neiTotalVisitors[i] = 0;
				neiTotalVisits[i] = 0;
				for (int j = 0; j < nIntervals; j++) {
					neiVisitors[i][j] = 0;
					neiVisits[i][j] = 0;
				}
			}
		}
	}

	public ObjectWithMeasure getShortestPath(int idx1, int idx2) {
		if (sumPlaces == null || neiMatrix == null)
			return null;
		int nObj = sumPlaces.size();
		if (nObj < 1)
			return null;
		if (idx1 < 0 || idx2 < 0 || idx1 >= nObj || idx2 >= nObj)
			return null;
		if (idx1 == idx2) {
			int path[] = new int[1];
			path[0] = idx1;
			return new ObjectWithMeasure(path, 0);
		}
		double pathLen = 0;
		IntArray path = new IntArray(20, 10);
		path.addElement(idx1);
		boolean reached = false;
		RealPoint ptGoal = SpatialEntity.getCentre(sumPlaces.elementAt(idx2).getGeometry());
		while (!reached) {
			double minDist = Double.NaN;
			int minIdx = -1;
			RealPoint pt0 = SpatialEntity.getCentre(sumPlaces.elementAt(idx1).getGeometry());
			for (int i = 0; i < nObj; i++)
				if (i != idx1 && neiMatrix[idx1][i] && path.indexOf(i) < 0) {
					if (i == idx2) {
						minDist = 0;
						minIdx = i;
						break;
					}
					RealPoint pt = SpatialEntity.getCentre(sumPlaces.elementAt(i).getGeometry());
					double dist = GeoComp.distance(ptGoal.x, ptGoal.y, pt.x, pt.y, geo) + GeoComp.distance(pt0.x, pt0.y, pt.x, pt.y, geo);
					if (Double.isNaN(minDist) || dist < minDist) {
						minDist = dist;
						minIdx = i;
					}
				}
			if (minIdx < 0) {
				break;
			}
			path.addElement(minIdx);
			RealPoint pt = SpatialEntity.getCentre(sumPlaces.elementAt(minIdx).getGeometry());
			pathLen += GeoComp.distance(pt0.x, pt0.y, pt.x, pt.y, geo);
			idx1 = minIdx;
			reached = minIdx == idx2;
		}
		if (!reached)
			return null; //the destination is unreachable
		return new ObjectWithMeasure(path.getTrimmedArray(), pathLen);
	}

	/**
	 * Accumulates the given trajectory using previously prepared structures
	 */
	public boolean accumulate(DMovingObject mobj) {
		++count;
		if (mobj == null || sumPlaces == null || sumPlaces.size() < 2)
			return false;
		Vector track = mobj.getTrack();
		if (track == null || track.size() < 1)
			return false;
		if (track.size() > 2)
			if (useOnlyStartsEnds) {
				Vector t = new Vector(2, 1);
				t.addElement(track.elementAt(0));
				t.addElement(track.elementAt(track.size() - 1));
				track = t;
			}
		int cap = Math.min(track.size(), 50);
		Vector<DPlaceVisitsCounter> places = new Vector<DPlaceVisitsCounter>(cap, 50);
		Vector<TimeMoment> enterTimes = new Vector<TimeMoment>(cap, 50), exitTimes = new Vector<TimeMoment>(cap, 50);
		int j = 0;
		int lastAreaIdx = -1;
		while (j < track.size()) {
			SpatialEntity spe = (SpatialEntity) track.elementAt(j);
			RealPoint pt = spe.getCentre();
			int aIdx = -1;
			if (useOnlyStartsEnds || lastAreaIdx < 0 || j == 0 || neiMatrix == null) {
				aIdx = findPlaceContainingPosition(pt.x, pt.y);
			} else {
				for (int k = 0; k < sumPlaces.size() && aIdx < 0; k++)
					if (k != lastAreaIdx && sumPlaces.elementAt(k) != null && neiMatrix[lastAreaIdx][k])
						if (sumPlaces.elementAt(k).contains(pt.x, pt.y)) {
							aIdx = k;
						}
				if (aIdx < 0 && findIntersections) {
					aIdx = findPlaceContainingPosition(pt.x, pt.y);
					if (aIdx >= 0 && aIdx != lastAreaIdx) {
						ObjectWithMeasure om = getShortestPath(lastAreaIdx, aIdx);
						if (om != null) {
							int path[] = (int[]) om.obj;
							int np = path.length - 1;
							if (np > 0) {
								SpatialEntity spe0 = (SpatialEntity) track.elementAt(j - 1);
								TimeMoment t0 = spe0.getTimeReference().getValidUntil();
								TimeMoment t1 = spe.getTimeReference().getValidFrom();
								long timeDiff = t1.subtract(t0), step = timeDiff / np;
								for (int pi = 1; pi < path.length - 1; pi++) {
									TimeMoment t = t0.getCopy();
									t.add(step);
									DPlaceVisitsCounter place = sumPlaces.elementAt(path[pi]);
									place.addCross(t);
									places.addElement(place);
									enterTimes.addElement(t);
									exitTimes.addElement(t);
									t0 = t;
								}
							}
						}
					}
				}
				if (aIdx < 0) {
					aIdx = findPlaceContainingPosition(pt.x, pt.y);
				}
			}
			if (aIdx < 0 || aIdx == lastAreaIdx) {
				++j;
				continue;
			}
			lastAreaIdx = aIdx;
			DPlaceVisitsCounter place = sumPlaces.elementAt(aIdx);
			int exitIdx = place.addVisit(mobj.getIdentifier(), track, j, geo);
			TimeMoment t0 = null, t1 = null;
			spe = (SpatialEntity) track.elementAt(j);
			if (spe.getTimeReference() != null) {
				t0 = spe.getTimeReference().getValidFrom();
			}
			spe = (SpatialEntity) track.elementAt(exitIdx);
			if (spe.getTimeReference() != null) {
				t1 = spe.getTimeReference().getValidUntil();
				if (t1 == null) {
					t1 = spe.getTimeReference().getValidFrom();
				}
			}
			places.addElement(place);
			enterTimes.addElement(t0);
			exitTimes.addElement(t1);
			j = exitIdx + 1;
		}
		if (places.size() < 2)
			return false;
		if (aggregateByTimeIntervals) {
			accumulateVisitsByTimeIntervals(places, enterTimes, exitTimes);
		}
		//a trajectory consisting of a single point is not counted as a move!
		if (track.size() < 2)
			return true;

		IntArray moveIdxs = null;
		int nIntervals = 0;
		if (!makeDynamicAggregates && aggregateByTimeIntervals) {
			moveIdxs = new IntArray(places.size(), 10);
			nIntervals = timeBreaks.size();
			if (!useCycle) {
				++nIntervals;
			}
		}
		if (places.size() < 2) {
			DPlaceVisitsCounter place = places.elementAt(0);
			TimeMoment t0 = exitTimes.elementAt(0), t1 = enterTimes.elementAt(0);
			if (makeDynamicAggregates) {
				DLinkObject link = new DLinkObject();
				link.setup(place, place, t0, t1);
				DAggregateLinkObject aggLink = null;
				if (aggLinks != null) {
					for (int k = 0; k < aggLinks.size() && aggLink == null; k++) {
						aggLink = aggLinks.elementAt(k);
						if (!aggLink.startNode.getIdentifier().equals(place.getIdentifier()) || !aggLink.endNode.getIdentifier().equals(place.getIdentifier())) {
							aggLink = null;
						}
					}
				}
				if (aggLink == null) {
					aggLink = new DAggregateLinkObject();
					if (aggLinks == null) {
						aggLinks = new Vector<DAggregateLinkObject>(1000, 500);
					}
					aggLinks.addElement(aggLink);
				}
				aggLink.addLink(link, mobj.getIdentifier());
			} else {
				DLinkObject move = null;
				int mIdx = -1;
				if (sumMoves != null) {
					for (int k = 0; k < sumMoves.size() && move == null; k++) {
						move = sumMoves.elementAt(k);
						if (!move.getStartNode().getIdentifier().equals(place.getIdentifier()) || !move.getEndNode().getIdentifier().equals(place.getIdentifier())) {
							move = null;
						} else {
							mIdx = k;
						}
					}
				}
				if (move == null) {
					move = new DLinkObject();
					move.setup(place, place, null, null);
					if (sumMoves == null) {
						sumMoves = new Vector<DLinkObject>(1000, 500);
						if (aggregateByTimeIntervals) {
							trajByTime = new Vector<int[]>(1000, 500);
							movesByTime = new Vector<int[]>(1000, 500);
							totalTraj = new IntArray(1000, 500);
							totalMoves = new IntArray(1000, 500);
						}
					}
					sumMoves.addElement(move);
					mIdx = sumMoves.size() - 1;
					if (aggregateByTimeIntervals) {
						int tr[] = new int[nIntervals], mv[] = new int[nIntervals];
						for (int ti = 0; ti < nIntervals; ti++) {
							tr[ti] = mv[ti] = 0;
						}
						trajByTime.addElement(tr);
						movesByTime.addElement(mv);
						totalTraj.addElement(0);
						totalMoves.addElement(0);
					}
				}
				move.incNTimes();
				if (moveIdxs != null) {
					moveIdxs.addElement(mIdx);
				}
			}
		} else {
			for (int n = 1; n < places.size(); n++) {
				DPlaceVisitsCounter start = places.elementAt(n - 1), end = places.elementAt(n);
				start.addLinkToPlace(end);
				end.addLinkToPlace(start);
				TimeMoment t0 = exitTimes.elementAt(n - 1), t1 = enterTimes.elementAt(n);
				if (makeDynamicAggregates) {
					DLinkObject link = new DLinkObject();
					link.setup(start, end, t0, t1);
					DAggregateLinkObject aggLink = null;
					if (aggLinks != null) {
						for (int k = 0; k < aggLinks.size() && aggLink == null; k++) {
							aggLink = aggLinks.elementAt(k);
							if (!aggLink.startNode.getIdentifier().equals(start.getIdentifier()) || !aggLink.endNode.getIdentifier().equals(end.getIdentifier())) {
								aggLink = null;
							}
						}
					}
					if (aggLink == null) {
						aggLink = new DAggregateLinkObject();
						if (aggLinks == null) {
							aggLinks = new Vector<DAggregateLinkObject>(1000, 500);
						}
						aggLinks.addElement(aggLink);
					}
					aggLink.addLink(link, mobj.getIdentifier());
				} else {
					DLinkObject move = null;
					int mIdx = -1;
					if (sumMoves != null) {
						for (int k = 0; k < sumMoves.size() && move == null; k++) {
							move = sumMoves.elementAt(k);
							if (!move.getStartNode().getIdentifier().equals(start.getIdentifier()) || !move.getEndNode().getIdentifier().equals(end.getIdentifier())) {
								move = null;
							} else {
								mIdx = k;
							}
						}
					}
					if (move == null) {
						move = new DLinkObject();
						move.setup(start, end, null, null);
						if (sumMoves == null) {
							sumMoves = new Vector(1000, 500);
							if (aggregateByTimeIntervals) {
								trajByTime = new Vector<int[]>(1000, 500);
								movesByTime = new Vector<int[]>(1000, 500);
								totalTraj = new IntArray(1000, 500);
								totalMoves = new IntArray(1000, 500);
							}
						}
						sumMoves.addElement(move);
						mIdx = sumMoves.size() - 1;
						if (aggregateByTimeIntervals) {
							int tr[] = new int[nIntervals], mv[] = new int[nIntervals];
							for (int ti = 0; ti < nIntervals; ti++) {
								tr[ti] = mv[ti] = 0;
							}
							trajByTime.addElement(tr);
							movesByTime.addElement(mv);
							totalTraj.addElement(0);
							totalMoves.addElement(0);
						}
					}
					move.incNTimes();
					if (moveIdxs != null) {
						moveIdxs.addElement(mIdx);
					}
				}
			}
		}
		if (aggregateByTimeIntervals && moveIdxs != null) {
			accumulateMovesByTimeIntervals(places, enterTimes, exitTimes, moveIdxs);
		}
		return true;
	}

	protected int findPlaceContainingPosition(float x, float y) {
		if (sumPlaces == null)
			return -1;
		for (int i = 0; i < sumPlaces.size(); i++)
			if (sumPlaces.elementAt(i) != null && sumPlaces.elementAt(i).contains(x, y))
				return i;
		return -1;
	}

	public int findPlaceById(String id) {
		if (id == null || sumPlaces == null)
			return -1;
		for (int i = 0; i < sumPlaces.size(); i++)
			if (sumPlaces.elementAt(i) != null && id.equals(sumPlaces.elementAt(i).getIdentifier()))
				return i;
		return -1;
	}

	public void accumulateMovesByTimeIntervals(Vector<DPlaceVisitsCounter> places, Vector<TimeMoment> enterTimes, Vector<TimeMoment> exitTimes, IntArray moveIdxs) {
		if (places == null || places.size() < 1 || moveIdxs == null || moveIdxs.size() < 1)
			return;
		if (trajByTime == null || trajByTime.size() < 1)
			return;
		int counts[] = trajByTime.elementAt(0);
		if (counts == null || counts.length < 1)
			return;
		int nIntervals = counts.length;
		if (nIntervals < 2)
			return;
		if (places.size() == 1) {
			TimeMoment t1 = enterTimes.elementAt(0), t2 = exitTimes.elementAt(0);
			int tIdx[] = TimeUtil.getIndexesOfTimeIntervals(t1, t2, timeBreaks, nIntervals, useCycle, cycleUnit, nCycleElements);
			if (tIdx == null)
				return;
			int mIdx = moveIdxs.elementAt(0);
			int tr[] = trajByTime.elementAt(mIdx), mv[] = movesByTime.elementAt(mIdx);
			if (tIdx[0] <= tIdx[1]) {
				for (int i = tIdx[0]; i <= tIdx[1]; i++) {
					++tr[i];
					++mv[i];
				}
			} else {
				for (int i = tIdx[0]; i < nIntervals; i++) {
					++tr[i];
					++mv[i];
				}
				for (int i = 0; i <= tIdx[1]; i++) {
					++tr[i];
					++mv[i];
				}
			}
			totalMoves.setElementAt(totalMoves.elementAt(mIdx) + 1, mIdx);
			totalTraj.setElementAt(totalTraj.elementAt(mIdx) + 1, mIdx);
			return;
		}
		IntArray diffMoveIdxs = new IntArray(moveIdxs.size(), 10);
		for (int i = 0; i < moveIdxs.size(); i++)
			if (diffMoveIdxs.indexOf(moveIdxs.elementAt(i)) < 0) {
				diffMoveIdxs.addElement(moveIdxs.elementAt(i));
			}
		boolean counted[] = new boolean[diffMoveIdxs.size()];
		boolean countedInInterval[][] = new boolean[diffMoveIdxs.size()][];
		for (int i = 0; i < diffMoveIdxs.size(); i++) {
			counted[i] = false;
			countedInInterval[i] = new boolean[nIntervals];
			for (int j = 0; j < nIntervals; j++) {
				countedInInterval[i][j] = false;
			}
		}
		for (int n = 0; n < moveIdxs.size(); n++) {
			int mIdx = moveIdxs.elementAt(n);
			int nd = diffMoveIdxs.indexOf(mIdx);
			TimeMoment t1 = exitTimes.elementAt(n), t2 = enterTimes.elementAt(n + 1);
			int tIdx[] = TimeUtil.getIndexesOfTimeIntervals(t1, t2, timeBreaks, nIntervals, useCycle, cycleUnit, nCycleElements);
			if (tIdx == null) {
				continue;
			}
			int tr[] = trajByTime.elementAt(mIdx), mv[] = movesByTime.elementAt(mIdx);
			if (tIdx[0] <= tIdx[1]) {
				for (int i = tIdx[0]; i <= tIdx[1]; i++) {
					++mv[i];
					if (!countedInInterval[nd][i]) {
						++tr[i];
						countedInInterval[nd][i] = true;
					}
				}
			} else {
				for (int i = tIdx[0]; i < nIntervals; i++) {
					++mv[i];
					if (!countedInInterval[nd][i]) {
						++tr[i];
						countedInInterval[nd][i] = true;
					}
				}
				for (int i = 0; i <= tIdx[1]; i++) {
					++mv[i];
					if (!countedInInterval[nd][i]) {
						++tr[i];
						countedInInterval[nd][i] = true;
					}
				}
			}
			totalMoves.setElementAt(totalMoves.elementAt(mIdx) + 1, mIdx);
			if (!counted[nd]) {
				totalTraj.setElementAt(totalTraj.elementAt(mIdx) + 1, mIdx);
				counted[nd] = true;
			}
		}
	}

	public void accumulateVisitsByTimeIntervals(Vector<DPlaceVisitsCounter> places, Vector<TimeMoment> enterTimes, Vector<TimeMoment> exitTimes) {
		if (places == null || places.size() < 1)
			return;
		if (visitors == null) {
			prepareTemporalAggregation();
		}
		if (visitors == null || visitors.length < 1)
			return;
		int nIntervals = visitors[0].length;
		if (nIntervals < 2)
			return;
		Vector<DPlaceVisitsCounter> diffPlaces = new Vector((neiVisitors == null) ? places.size() : places.size() * 5, 1);
		for (int i = 0; i < places.size(); i++)
			if (!diffPlaces.contains(places.elementAt(i))) {
				diffPlaces.addElement(places.elementAt(i));
			}
		int nDiffPlaces = diffPlaces.size();
		if (neiVisitors != null) {
			for (int i = 0; i < places.size(); i++) {
				DPlaceVisitsCounter place = places.elementAt(i);
				if (place.neighbours == null || place.neighbours.size() < 1) {
					continue;
				}
				for (int nn = 0; nn < place.neighbours.size(); nn++) {
					int nna = findPlaceById(place.neighbours.elementAt(nn));
					if (nna >= 0) {
						DPlaceVisitsCounter neighbor = sumPlaces.elementAt(nna);
						if (!diffPlaces.contains(neighbor)) {
							diffPlaces.addElement(neighbor);
						}
					}
				}
			}
		}
		boolean counted[] = new boolean[diffPlaces.size()];
		boolean countedInInterval[][] = new boolean[diffPlaces.size()][];
		for (int i = 0; i < diffPlaces.size(); i++) {
			counted[i] = false;
			countedInInterval[i] = new boolean[nIntervals];
			for (int j = 0; j < nIntervals; j++) {
				countedInInterval[i][j] = false;
			}
		}
		for (int n = 0; n < places.size(); n++) {
			DPlaceVisitsCounter place = places.elementAt(n);
			int na = sumPlaces.indexOf(place);
			if (na < 0) {
				continue;
			}
			int nd = diffPlaces.indexOf(place);
			TimeMoment t1 = enterTimes.elementAt(n), t2 = exitTimes.elementAt(n);
			int tIdx[] = TimeUtil.getIndexesOfTimeIntervals(t1, t2, timeBreaks, nIntervals, useCycle, cycleUnit, nCycleElements);
			if (tIdx == null) {
				continue;
			}
			if (tIdx[0] <= tIdx[1]) {
				for (int i = tIdx[0]; i <= tIdx[1]; i++) {
					++visits[na][i];
					if (neiVisits != null) {
						++neiVisits[na][i];
					}
					if (!countedInInterval[nd][i]) {
						++visitors[na][i];
						if (neiVisitors != null) {
							++neiVisitors[na][i];
						}
						countedInInterval[nd][i] = true;
					}
				}
			} else {
				for (int i = tIdx[0]; i < nIntervals; i++) {
					++visits[na][i];
					if (neiVisits != null) {
						++neiVisits[na][i];
					}
					if (!countedInInterval[nd][i]) {
						++visitors[na][i];
						if (neiVisitors != null) {
							++neiVisitors[na][i];
						}
						countedInInterval[nd][i] = true;
					}
				}
				for (int i = 0; i <= tIdx[1]; i++) {
					++visits[na][i];
					if (neiVisits != null) {
						++neiVisits[na][i];
					}
					if (!countedInInterval[nd][i]) {
						++visitors[na][i];
						if (neiVisitors != null) {
							++neiVisitors[na][i];
						}
						countedInInterval[nd][i] = true;
					}
				}
			}
			++totalVisits[na];
			if (!counted[nd]) {
				++totalVisitors[na];
				if (neiTotalVisitors != null) {
					++neiTotalVisitors[na];
				}
				counted[nd] = true;
			}
		}
		if (neiVisitors != null) {
			for (int n = 0; n < places.size(); n++) {
				DPlaceVisitsCounter place = places.elementAt(n);
				if (place.neighbours == null || place.neighbours.size() < 1) {
					continue;
				}
				int na = sumPlaces.indexOf(place);
				if (na < 0) {
					continue;
				}
				TimeMoment t1 = enterTimes.elementAt(n), t2 = exitTimes.elementAt(n);
				int tIdx[] = TimeUtil.getIndexesOfTimeIntervals(t1, t2, timeBreaks, nIntervals, useCycle, cycleUnit, nCycleElements);
				if (tIdx == null) {
					continue;
				}
				for (int nn = 0; nn < place.neighbours.size(); nn++) {
					int nna = findPlaceById(place.neighbours.elementAt(nn));
					if (nna >= 0) {
						DPlaceVisitsCounter neighbor = sumPlaces.elementAt(nna);
						int nd = diffPlaces.indexOf(neighbor);
						if (tIdx[0] <= tIdx[1]) {
							for (int i = tIdx[0]; i <= tIdx[1]; i++) {
								++neiVisits[nna][i];
								if (!countedInInterval[nd][i]) {
									++neiVisitors[nna][i];
									countedInInterval[nd][i] = true;
								}
							}
						} else {
							for (int i = tIdx[0]; i < nIntervals; i++) {
								++neiVisits[nna][i];
								if (!countedInInterval[nd][i]) {
									++neiVisitors[nna][i];
									countedInInterval[nd][i] = true;
								}
							}
							for (int i = 0; i <= tIdx[1]; i++) {
								++neiVisits[nna][i];
								if (!countedInInterval[nd][i]) {
									++neiVisitors[nna][i];
									countedInInterval[nd][i] = true;
								}
							}
						}
						++neiTotalVisits[nna];
						if (!counted[nd]) {
							++neiTotalVisitors[nna];
							counted[nd] = true;
						}
					}
				}
			}
		}
	}
}
