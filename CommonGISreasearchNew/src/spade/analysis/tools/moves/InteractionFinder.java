package spade.analysis.tools.moves;

import java.util.Vector;

import spade.lib.basicwin.NotificationLine;
import spade.lib.util.BubbleSort;
import spade.lib.util.Comparator;
import spade.lib.util.GeoDistance;
import spade.time.TimeMoment;
import spade.time.TimeReference;
import spade.vis.database.SpatialEntity;
import spade.vis.dmap.DMovingObject;
import spade.vis.geometry.RealPoint;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Apr 9, 2008
 * Time: 11:59:57 AM
 * Detects interactions between two or more trajectories.
 * An interaction is defined as close positions (within a specified
 * threshold distThr) during certain time (not less than a specified
 * threshold timeThr).
 */
public class InteractionFinder implements Comparator {
	/**
	 * The original collection of trajectories (instances of DMovingObject)
	 */
	protected Vector trajectories = null;
	/**
	 * The trajectories divided into fragments by time intervals, for efficiency.
	 * The array keeps a reference to the first fragment of each trajectory.
	 * Each fragment, in turn, keeps a reference to the next fragment of the same trajectory.
	 */
	protected TrajectoryFragment trFragments[] = null;
	/**
	 * The error message, which may be generated in the course of the work.
	 */
	protected String err = null;

	/**
	 * Returns the error message, which may be generated in the course of the work.
	 */
	public String getErrorMessage() {
		return err;
	}

	/**
	 * Used for displaying status messages
	 */
	public NotificationLine lStatus = null;

	/**
	 * Divides the given trajectories into suitable fragments and
	 * prepares everything for the search.
	 * Returns false if the input is not suitable for the search.
	 */
	public boolean setTrajectories(Vector trajectories) {
		this.trajectories = trajectories;
		if (trajectories == null || trajectories.size() < 1) {
			err = "No trajectories found!";
			return false;
		}
		if (trajectories.size() < 2) {
			err = "Only one trajectory found!";
			return false;
		}
		int trN = 0;
		TimeMoment firstTime = null, lastTime = null;
		for (int i = 0; i < trajectories.size(); i++)
			if (trajectories.elementAt(i) instanceof DMovingObject) {
				DMovingObject mobj = (DMovingObject) trajectories.elementAt(i);
				TimeMoment t1 = mobj.getStartTime(), t2 = mobj.getEndTime();
				if (t1 == null || t2 == null) {
					continue;
				}
				if (firstTime == null || firstTime.compareTo(t1) > 0) {
					firstTime = t1;
				}
				if (lastTime == null || lastTime.compareTo(t2) < 0) {
					lastTime = t2;
				}
				++trN;
			}
		if (firstTime == null || lastTime == null || firstTime.compareTo(lastTime) >= 0) {
			err = "No valid time references found!";
			return false;
		}
		if (trN < 2) {
			err = (trN == 0) ? "No trajectories found!" : "Only one trajectory found!";
			return false;
		}
		long totalLen = lastTime.subtract(firstTime);
		int intLen = (int) (totalLen / Math.min(totalLen, 100));
		int nIntervals = (int) Math.round((Math.ceil(1.0 * totalLen / intLen)));
		TimeMoment times[] = new TimeMoment[nIntervals];
		times[0] = firstTime;
		for (int i = 1; i < nIntervals; i++) {
			times[i] = times[i - 1].getCopy();
			times[i].add(intLen);
		}
		//divide the trajectories into fragments
		trFragments = new TrajectoryFragment[trajectories.size()];
		int nTr = 0;
		for (int i = 0; i < trajectories.size(); i++) {
			trFragments[i] = null;
			if (trajectories.elementAt(i) instanceof DMovingObject) {
				DMovingObject mobj = (DMovingObject) trajectories.elementAt(i);
				Vector track = mobj.getTrack();
				if (track == null || track.size() < 1) {
					continue;
				}
				TimeMoment t1 = mobj.getStartTime(), t2 = mobj.getEndTime();
				if (t1 == null || t2 == null) {
					continue;
				}
				int tIdx = 0;
				for (int j = 1; j < times.length; j++)
					if (t1.compareTo(times[j]) < 0) {
						break;
					} else {
						tIdx = j;
					}
				TrajectoryFragment fragm = getFragmentForTimeInterval(mobj, times[tIdx], (tIdx + 1 < times.length) ? times[tIdx + 1] : lastTime, 0);
				trFragments[i] = fragm;
				++nTr;
				while (fragm != null) {
					fragm.trIdx = i;
					if (fragm.idx2 + 1 >= track.size()) {
						break;
					}
					++tIdx;
					if (tIdx >= times.length) {
						break;
					}
					if (fragm.t2.compareTo(t2) >= 0) {
						break;
					}
					do {
						fragm.next = getFragmentForTimeInterval(mobj, times[tIdx], (tIdx + 1 < times.length) ? times[tIdx + 1] : lastTime, fragm.idx2 + 1);
						if (fragm.next == null) {
							++tIdx;
						}
					} while (fragm.next == null && tIdx < times.length);
					fragm = fragm.next;
				}
			}
		}
		if (nTr < 2) {
			err = "No appropriate data found!";
			return false;
		}
		return true;
	}

	/**
	 * Used for displaying status messages
	 */
	public void setNotificationLine(NotificationLine lStatus) {
		this.lStatus = lStatus;
	}

	public void showMessage(String msg) {
		if (lStatus != null) {
			lStatus.showMessage(msg, false);
		}
	}

	/**
	 * Detects interactions between trajectories, i.e. places and times where and when
	 * two or more trajectories come close together.
	 * @param distThr - distance threshold
	 * @param geo - whether the coordinates should be treated as geographic
	 *              (x as longituge and y as latitude)
	 * @param timeThr - temporal threshold, in the smallest time units
	 * @return a vector of instances of InteractionData
	 */
	public Vector findPairwiseInteractions(float distThr, boolean geo, int timeThr) {
		if (trFragments == null || trFragments.length < 2)
			return null;
		float distThrInDataUnits = distThr;
		if (geo) {
			for (TrajectoryFragment fragm : trFragments)
				if (fragm != null) {
					double x0 = (fragm.x1 + fragm.x2) / 2, y0 = (fragm.y1 + fragm.y2) / 2;
					double d1 = GeoDistance.geoDist(x0, y0, x0 + 1, y0), d2 = GeoDistance.geoDist(x0, y0, x0, y0 + 1), d = (d1 < d2) ? d1 : d2;
					distThrInDataUnits /= d;
					break;
				}
		}
		Vector inter = null;
		float minX = Float.NaN, maxX = minX, minY = minX, maxY = minX;
		for (int i = 0; i < trFragments.length; i++)
			if (trFragments[i] != null) {
				TrajectoryFragment fragm0 = trFragments[i];
				int trIdx0 = fragm0.trIdx;
				DMovingObject mobj0 = (DMovingObject) trajectories.elementAt(trIdx0);

				for (int j = i + 1; j < trFragments.length; j++) {
					TrajectoryFragment fragm1 = trFragments[j];
					if (fragm1 == null) {
						continue;
					}
					fragm0 = trFragments[i];
					if (fragm1.entityId != null && fragm0.entityId != null && fragm1.entityId.equalsIgnoreCase(fragm0.entityId)) {
						continue; //same entity
					}
					int trIdx1 = fragm1.trIdx;
					DMovingObject mobj1 = (DMovingObject) trajectories.elementAt(trIdx1);
					int prevNumInter = (inter == null) ? 0 : inter.size(); //the number of the previously found interactions
					/*
					if (mobj0.getIdentifier().equals("151") && mobj1.getIdentifier().equals("248")) {
					  System.out.println("Trajectories "+mobj0.getIdentifier()+" and "+mobj1.getIdentifier());
					}
					/**/
					while (fragm0 != null && fragm1 != null) {
						while (fragm0 != null && fragm1 != null && !areCloseInTime(fragm0.t1, fragm0.t2, fragm1.t1, fragm1.t2, timeThr)) {
							if (fragm0.t1.compareTo(fragm1.t1) < 0) {
								fragm0 = fragm0.next;
							} else {
								fragm1 = fragm1.next;
							}
						}
						if (fragm0 == null || fragm1 == null) {
							break;
						}
						while (fragm0 != null && fragm1 != null && !areCloseInSpace(fragm0.x1, fragm0.y1, fragm0.x2, fragm0.y2, fragm1.x1, fragm1.y1, fragm1.x2, fragm1.y2, distThrInDataUnits)) {
							if (fragm0.t1.compareTo(fragm1.t1) <= 0) {
								fragm0 = fragm0.next;
							} else {
								fragm1 = fragm1.next;
							}
						}
						if (fragm0 == null || fragm1 == null) {
							break;
						}
						int np0 = 0;
						while (np0 < fragm0.points.length) {
							TimeReference tr0 = mobj0.getPositionTime(fragm0.idx1 + np0);
							int np1 = 0;
							while (np0 < fragm0.points.length && np1 < fragm1.points.length) {
								TimeReference tr1 = mobj1.getPositionTime(fragm1.idx1 + np1);
								if (tr1.getValidFrom().subtract(tr0.getValidUntil()) > timeThr) {
									break;
								} else if (tr0.getValidFrom().subtract(tr1.getValidUntil()) > timeThr) {
									++np1;
								} else if (InteractionData.areNeighbours(fragm0.points[np0], tr0, fragm1.points[np1], tr1, distThr, geo, timeThr)) {
									InteractionData idata = new InteractionData();
									if (inter == null) {
										inter = new Vector(500, 100);
									}
									inter.addElement(idata);
									boolean neighbours = true;
									while (neighbours) {
										int idx1 = idata.addPoint(fragm0.points[np0], tr0, trIdx0, fragm0.idx1 + np0);
										int idx2 = idata.addPoint(fragm1.points[np1], tr1, trIdx1, fragm1.idx1 + np1);
										idata.addLink(idx1, idx2);
										int cmp = tr0.getValidUntil().compareTo(tr1.getValidUntil());
										if (cmp <= 0) {
											++np0;
											if (np0 >= fragm0.points.length)
												if (fragm0.next == null) {
													break;
												} else {
													fragm0 = fragm0.next;
													np0 = 0;
												}
											tr0 = mobj0.getPositionTime(fragm0.idx1 + np0);
											neighbours = InteractionData.areNeighbours(fragm0.points[np0], tr0, fragm1.points[np1], tr1, distThr, geo, timeThr);
										}
										if (!neighbours || cmp > 0) {
											++np1;
											if (np1 >= fragm1.points.length)
												if (fragm1.next == null) {
													break;
												} else {
													fragm1 = fragm1.next;
													np1 = 0;
												}
											tr1 = mobj1.getPositionTime(fragm1.idx1 + np1);
											neighbours = InteractionData.areNeighbours(fragm0.points[np0], tr0, fragm1.points[np1], tr1, distThr, geo, timeThr);
											if (!neighbours && cmp > 0) {
												++np0;
												if (np0 >= fragm0.points.length)
													if (fragm0.next == null) {
														break;
													} else {
														fragm0 = fragm0.next;
														np0 = 0;
													}
												tr0 = mobj0.getPositionTime(fragm0.idx1 + np0);
												neighbours = InteractionData.areNeighbours(fragm0.points[np0], tr0, fragm1.points[np1], tr1, distThr, geo, timeThr);
											}
										}
									}
									++np1;
								} else {
									++np1;
								}
							}
							++np0;
						}
						if (fragm0.t1.compareTo(fragm1.t1) <= 0) {
							fragm0 = fragm0.next;
						} else {
							fragm1 = fragm1.next;
						}
					}
					//unite interactions of the two trajectories adjacent in time
					if (inter != null && inter.size() - prevNumInter > 1) {
						for (int int1 = prevNumInter; int1 < inter.size() - 1; int1++) {
							InteractionData idata1 = (InteractionData) inter.elementAt(int1);
							int mm01[] = idata1.getMinMaxPointIndexes(trIdx0), mm11[] = idata1.getMinMaxPointIndexes(trIdx1);
							int int2 = int1 + 1;
							while (int2 < inter.size()) {
								InteractionData idata2 = (InteractionData) inter.elementAt(int2);
								boolean adjacent = idata2.t1.subtract(idata1.t2) <= 1;
								if (!adjacent) {
									int mm02[] = idata2.getMinMaxPointIndexes(trIdx0), mm12[] = idata2.getMinMaxPointIndexes(trIdx1);
									adjacent = ((mm01[1] < mm02[1]) ? mm02[0] - mm01[1] <= 1 : mm01[0] - mm02[1] <= 1) && ((mm11[1] < mm12[1]) ? mm12[0] - mm11[1] <= 1 : mm11[0] - mm12[1] <= 1);
								}
								//if (idata2.t1.subtract(idata1.t2)<=1) {
								if (adjacent) {
									//unite the interactions
									idata1.unite(idata2);
									inter.removeElementAt(int2);
								} else {
									++int2;
								}
							}
						}
					}
				}
				if ((i + 1) % 10 == 0) {
					showMessage("Processed " + (i + 1) + " trajectories of " + trFragments.length);
				}
			}
		sortInteractions(inter);
		return inter;
	}

	/**
	 * Unites pairwise interactions into interactions with more members
	 * @param inter - source interactions
	 * @return  vector with resulting united interactions
	 */
	public Vector uniteInteractions(Vector inter) {
		if (inter == null)
			return null;
		//unite overlapping interactions
		boolean changed = false;
		int nloops = 0, nUnions = 0;
		do {
			showMessage("Uniting interactions; loop " + (nloops + 1));
			changed = false;
			for (int i = 0; i < inter.size() - 1; i++) {
				InteractionData idata1 = (InteractionData) inter.elementAt(i);
				int j = i + 1;
				while (j < inter.size()) {
					InteractionData idata2 = (InteractionData) inter.elementAt(j);
					//if (idata1.overlaps(idata2)) {
					if (idata1.haveCommonTrajectoryFragment(idata2)) {
						idata1.unite(idata2);
						inter.removeElementAt(j);
						changed = true;
						++nUnions;
						showMessage("Uniting interactions: " + nUnions + " unions made");
					} else {
						j++;
					}
				}
				if ((i + 1) % 5 == 0) {
					showMessage("Uniting interactions; loop " + (nloops + 1) + " (" + (i + 1) + ")");
				}
			}
			++nloops;
		} while (changed);
		sortInteractions(inter);
		return inter;
	}

	/**
	 * Sorts the interactions by the times of their occurrences
	 */
	public void sortInteractions(Vector inter) {
		BubbleSort.sort(inter, this);
	}

	public boolean areCloseInTime(TimeMoment start1, TimeMoment end1, TimeMoment start2, TimeMoment end2, int timeThr) {
		if (start1.subtract(end2) > timeThr || start2.subtract(end1) > timeThr)
			return false;
		return true;
	}

	public boolean areCloseInSpace(float minX1, float minY1, float maxX1, float maxY1, float minX2, float minY2, float maxX2, float maxY2, float distThr) {
		if (minX1 - maxX2 > distThr || minX2 - maxX1 > distThr)
			return false;
		if (minY1 - maxY2 > distThr || minY2 - maxY1 > distThr)
			return false;
		return true;
	}

	/**
	 * Compares two interactions by the times of their occurrences.
	 * Returns 0 if the objects are equal, <0 if the first object is less than the
	 * second one, >0 otherwise
	 */
	@Override
	public int compare(Object o1, Object o2) {
		if (o1 == null)
			if (o2 == null)
				return 0;
			else
				return 1;
		else if (o2 == null)
			return -1;
		if (!(o1 instanceof InteractionData) || !(o2 instanceof InteractionData))
			return 0;
		InteractionData int1 = (InteractionData) o1, int2 = (InteractionData) o2;
		if (int1.t1 == null)
			if (int2.t1 == null)
				return 0;
			else
				return 1;
		else if (int2.t1 == null)
			return -1;
		int c = int1.t1.compareTo(int2.t1);
		if (c != 0)
			return c;
		return int1.t2.compareTo(int2.t2);
	}

	/**
	 * Finds a fragment of the trajectory fitting in the given time interval
	 * @param mobj - the trajectory
	 * @param t1 - start of the time interval
	 * @param t2 - end of the time interval
	 * @param startIdx - the starting index of the trajectory position from
	 *                   which to begin the search.
	 * @return an instance of TrajectoryFragment or null
	 */
	public TrajectoryFragment getFragmentForTimeInterval(DMovingObject mobj, TimeMoment t1, TimeMoment t2, int startIdx) {
		if (mobj == null)
			return null;
		if (startIdx < 0) {
			startIdx = 0;
		}
		Vector track = mobj.getTrack();
		if (track == null || track.size() <= startIdx)
			return null;
		TimeMoment startTime = mobj.getStartTime();
		if (startTime == null)
			return null;
		int c1 = startTime.compareTo(t2);
		if (c1 > 0)
			return null;
		TimeMoment endTime = mobj.getEndTime();
		if (endTime == null)
			return null;
		if (t2 == null) {
			t2 = endTime;
		}
		int c2 = endTime.compareTo(t1);
		if (c2 < 0)
			return null;
		int i2 = track.size() - 1, i1 = i2;
		if (c2 > 0) {
			i1 = i2 = -1;
			for (int i = startIdx; i < track.size(); i++) {
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
					continue;
				}
				if (i1 < 0) {
					i1 = i;
				}
				i2 = i;
			}
			if (i1 < 0)
				return null;
		}
		TrajectoryFragment fragm = new TrajectoryFragment();
		fragm.trId = mobj.getIdentifier();
		fragm.entityId = mobj.getEntityId();
		fragm.idx1 = i1;
		fragm.idx2 = i2;
		fragm.points = new RealPoint[i2 - i1 + 1];
		for (int i = i1; i <= i2; i++) {
			SpatialEntity spe = (SpatialEntity) track.elementAt(i);
			if (i == i1) {
				fragm.t1 = spe.getTimeReference().getValidFrom();
			}
			if (i == i2) {
				fragm.t2 = spe.getTimeReference().getValidUntil();
			}
			int k = i - i1;
			fragm.points[k] = spe.getCentre();
			if (fragm.points[k] == null) {
				continue;
			}
			if (Float.isNaN(fragm.x1) || fragm.x1 > fragm.points[k].x) {
				fragm.x1 = fragm.points[k].x;
			}
			if (Float.isNaN(fragm.x2) || fragm.x2 < fragm.points[k].x) {
				fragm.x2 = fragm.points[k].x;
			}
			if (Float.isNaN(fragm.y1) || fragm.y1 > fragm.points[k].y) {
				fragm.y1 = fragm.points[k].y;
			}
			if (Float.isNaN(fragm.y2) || fragm.y2 < fragm.points[k].y) {
				fragm.y2 = fragm.points[k].y;
			}
		}
		if (Float.isNaN(fragm.x1) || Float.isNaN(fragm.y1))
			return null;
		return fragm;
	}
}
