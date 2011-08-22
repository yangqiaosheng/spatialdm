package spade.analysis.tools.events;

import java.util.Vector;

import spade.time.TimeMoment;
import spade.time.TimeUtil;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;
import spade.vis.geometry.Geometry;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Jan 4, 2010
 * Time: 5:32:50 PM
 * Used for summarising events in an incremental mode, one by one.
 * E.g. the events may come from a database.
 */
public class EventsAccumulator {
	/**
	 * The layer with the areas (polygons) used for summarising the events.
	 */
	protected DGeoLayer areaLayer = null;
	/**
	 * The start and end of the whole time span to be considered
	 */
	public TimeMoment tStart = null, tEnd = null;
	/**
	 * The temporal breaks
	 */
	public Vector<TimeMoment> timeBreaks = null;
	/**
	 * The number of intervals
	 */
	public int nIntervals = 0;
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
	/**
	 * The counts of the events by the areas and time intervals
	 */
	public int counts[][] = null;
	/**
	 * The total counts of the events by the areas
	 */
	public int totals[] = null;
	/**
	 * Whether to count events in neighbouring places
	 */
	public boolean countEventsAround = false;
	/**
	 * The counts of the events by the areas and time intervals, including neighbourhood
	 */
	public int neiCounts[][] = null;
	/**
	 * The total counts of the events by the areas, including neighbourhood
	 */
	public int neiTotals[] = null;

	/**
	 * Sets the layer with the areas (polygons) to be used for summarising the events.
	 * Returns true if everything has been done successfully.
	 */
	public boolean setAreas(DGeoLayer layer) {
		if (layer == null || layer.getObjectCount() < 1)
			return false;
		if (layer.getType() != Geometry.area)
			return false;
		areaLayer = layer;
		totals = neiTotals = null;
		counts = neiCounts = null;
		return true;
	}

	/**
	 * Whether to count events in neighbouring places
	 */
	public void setCountEventsAround(boolean countEventsAround) {
		this.countEventsAround = countEventsAround;
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
	}

	/**
	 * Allocates structures for counting by time intervals
	 */
	protected void prepareTemporalAggregation() {
		if (counts != null)
			return;
		if (areaLayer == null)
			return;
		int nAreas = areaLayer.getObjectCount();
		if (nAreas < 1)
			return;
		nIntervals = timeBreaks.size();
		if (!useCycle) {
			++nIntervals;
		}
		counts = new int[nAreas][nIntervals];
		totals = new int[nAreas];
		for (int i = 0; i < nAreas; i++) {
			totals[i] = 0;
			for (int j = 0; j < nIntervals; j++) {
				counts[i][j] = 0;
			}
		}
		if (!countEventsAround)
			return;
		boolean hasNeiInfo = false;
		for (int i = 0; i < nAreas && !hasNeiInfo; i++) {
			DGeoObject place = areaLayer.getObject(i);
			hasNeiInfo = place.neighbours != null && place.neighbours.size() > 0;
		}
		if (hasNeiInfo) {
			neiCounts = new int[nAreas][nIntervals];
			neiTotals = new int[nAreas];
			for (int i = 0; i < nAreas; i++) {
				neiTotals[i] = 0;
				for (int j = 0; j < nIntervals; j++) {
					neiCounts[i][j] = 0;
				}
			}
		}
	}

	/**
	 * Accumulates the event with the given x- and y-coordinates and time reference (t1..t2)
	 */
	public void accumulateEvent(float x, float y, TimeMoment t1, TimeMoment t2) {
		if (areaLayer == null)
			return;
		prepareTemporalAggregation();
		int aIdx = -1;
		DGeoObject area = null;
		for (int na = 0; na < areaLayer.getObjectCount() && aIdx < 0; na++) {
			if (!areaLayer.isObjectActive(na)) {
				continue;
			}
			area = areaLayer.getObject(na);
			if (area.contains(x, y, 0f)) {
				aIdx = na;
			}
		}
		if (aIdx < 0)
			return;
		++totals[aIdx];
		if (neiTotals != null) {
			++neiTotals[aIdx];
		}
		int tIdx[] = TimeUtil.getIndexesOfTimeIntervals(t1, t2, timeBreaks, nIntervals, useCycle, cycleUnit, nCycleElements);
		if (tIdx != null) {
			if (tIdx[0] <= tIdx[1]) {
				for (int i = tIdx[0]; i <= tIdx[1]; i++) {
					++counts[aIdx][i];
					if (neiCounts != null) {
						++neiCounts[aIdx][i];
					}
				}
			} else {
				for (int i = tIdx[0]; i < nIntervals; i++) {
					++counts[aIdx][i];
					if (neiCounts != null) {
						++neiCounts[aIdx][i];
					}
				}
				for (int i = 0; i <= tIdx[1]; i++) {
					++counts[aIdx][i];
					if (neiCounts != null) {
						++neiCounts[aIdx][i];
					}
				}
			}
		}
		if (neiCounts == null || area.neighbours == null || area.neighbours.size() < 1)
			return;
		for (int nn = 0; nn < area.neighbours.size(); nn++) {
			int nna = areaLayer.getObjectIndex(area.neighbours.elementAt(nn));
			if (nna < 0) {
				continue;
			}
			++neiTotals[nna];
			if (tIdx != null) {
				if (tIdx[0] <= tIdx[1]) {
					for (int i = tIdx[0]; i <= tIdx[1]; i++) {
						++neiCounts[nna][i];
					}
				} else {
					for (int i = tIdx[0]; i < nIntervals; i++) {
						++neiCounts[nna][i];
					}
					for (int i = 0; i <= tIdx[1]; i++) {
						++neiCounts[nna][i];
					}
				}
			}
		}
	}
}
