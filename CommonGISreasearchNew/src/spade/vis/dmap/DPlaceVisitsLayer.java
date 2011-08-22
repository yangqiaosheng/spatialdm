package spade.vis.dmap;

import java.awt.Graphics;
import java.beans.PropertyChangeEvent;
import java.util.Vector;

import spade.lib.util.BubbleSort;
import spade.time.TimeMoment;
import spade.time.TimeReference;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.database.TimeFilter;
import spade.vis.map.MapContext;
import spade.vis.space.GeoLayer;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: 21-Aug-2007
 * Time: 09:58:11
 * Contains generalised characteristic positions extracted from
 * trajectories. The elements of the layer are instances of
 * DPlaceVisitObject.
 */
public class DPlaceVisitsLayer extends DGeoLayer implements LinkedToMapLayers {
	/**
	 * A reference to a layer with the original trajectories passing the places.
	 * May be null.
	 */
	protected DGeoLayer trajLayer = null;
	/**
	 * The parameters used for generating this layer
	 */
	public boolean onlyActiveTrajectories = false;
	public boolean onlyStartsEnds = false;
	public boolean findIntersections = true;
	/**
	 * A reference to a layer with the simplified (generalised) trajectories
	 * passing the places. May be null.
	 */
	protected DGeoLayer simpleTrajLayer = null;
	/**
	 * The identifiers of the active trajectories, i.e. trajectories satisfying
	 * the filters of the layers trajLayer and simpleTrajLayer
	 */
	protected Vector activeTrajIds = null;
	/**
	 * The last time interval selected with the use of the time filter.
	 */
	protected TimeMoment tStart = null, tEnd = null;
	/**
	 * Used for preparation of the layer before the first drawing
	 */
	protected boolean neverDrawn = true;
	/**
	 * The maximum, total (sum), and mean distortion of the trajectories in the generalized places
	 */
	public float maxDistortion = 0, maxSumDistortion = 0, maxMeanDistortion = 0, sumDistortion = 0, meanDistortion = 0;
	/**
	 * Indicates that the statistics of the distortions by value intervals
	 * has been at least once counted
	 */
	public boolean distoByIntervalsCounted = false;
	/**
	 * Indices of the table columns with the statistics about active
	 * trajectories passing the places
	 */
	public int nVisIdxA = -1, nTrajIdxA = -1, maxNRepVisIdxA = -1, nStartsIdxA = -1, nEndsIdxA = -1, tEnterIdxA = -1, tExitIdxA = -1, minDurIdxA = -1, maxDurIdxA = -1, meanDurIdxA = -1, medianDurIdxA = -1, totalDurIdxA = -1, minTimeGapIdxA = -1,
			maxTimeGapIdxA = -1, meanTimeGapIdxA = -1, medianTimeGapIdxA = -1, minLenIdxA = -1, maxLenIdxA = -1, meanLenIdxA = -1, medianLenIdxA = -1, totalLenIdxA = -1, minSpeedIdxA = -1, maxSpeedIdxA = -1, meanSpeedIdxA = -1, medianSpeedIdxA = -1,
			minAngleIdxA = -1, maxAngleIdxA = -1, medianAngleIdxA = -1;
	/**
	 * A layer with the points representing the visits of the places
	 */
	protected DGeoLayer visitsPointLayer = null;
	/**
	 * If this layer has been built by refining another DPlaceVisitsLayer,
	 * this is a reference to the original DPlaceVisitsLayer
	 */
	protected DPlaceVisitsLayer origPlaceLayer = null;

	/**
	 * Generates a table with statistical information about the visits
	 * of the places. Attaches the records of the table to the objects
	 * of the layer.
	 */
	public DataTable constructTableWithStatistics() {
		if (geoObj == null || geoObj.size() < 1)
			return null;
		DataTable table = new DataTable();
		table.addAttribute("N visits", "n_visits", AttributeTypes.integer);
		int nVisIdx = table.getAttrCount() - 1;
		table.addAttribute("N different trajectories", "n_traj", AttributeTypes.integer);
		int nTrajIdx = table.getAttrCount() - 1;
		table.addAttribute("Max N repeated visits", "max_n_rep_visits", AttributeTypes.integer);
		int maxNRepVisIdx = table.getAttrCount() - 1;
		table.addAttribute("N starts", "n_starts", AttributeTypes.integer);
		int nStartsIdx = table.getAttrCount() - 1;
		table.addAttribute("N ends", "n_ends", AttributeTypes.integer);
		int nEndsIdx = table.getAttrCount() - 1;
		table.addAttribute("Min time of entering", "min_t_enter", AttributeTypes.time);
		int tEnterIdx = table.getAttrCount() - 1;
		Attribute timeAttr = table.getAttribute(tEnterIdx);
		timeAttr.timeRefMeaning = Attribute.VALID_FROM;
		table.addAttribute("Max time of leaving", "max_t_exit", AttributeTypes.time);
		int tExitIdx = table.getAttrCount() - 1;
		timeAttr = table.getAttribute(tExitIdx);
		timeAttr.timeRefMeaning = Attribute.VALID_UNTIL;
		table.addAttribute("Min duration of stay", "dur_min", AttributeTypes.integer);
		int minDurIdx = table.getAttrCount() - 1;
		table.addAttribute("Max duration of stay", "dur_max", AttributeTypes.integer);
		int maxDurIdx = table.getAttrCount() - 1;
		table.addAttribute("Mean duration of stay", "dur_mean", AttributeTypes.integer);
		int meanDurIdx = table.getAttrCount() - 1;
		table.addAttribute("Median duration of stay", "dur_median", AttributeTypes.integer);
		int medianDurIdx = table.getAttrCount() - 1;
		table.addAttribute("Total duration of stay", "dur_total", AttributeTypes.integer);
		int totalDurIdx = table.getAttrCount() - 1;
		table.addAttribute("Min time gap", "gap_min", AttributeTypes.integer);
		int minTimeGapIdx = table.getAttrCount() - 1;
		table.addAttribute("Max time gap", "gap_max", AttributeTypes.integer);
		int maxTimeGapIdx = table.getAttrCount() - 1;
		table.addAttribute("Mean time gap", "gap_mean", AttributeTypes.integer);
		int meanTimeGapIdx = table.getAttrCount() - 1;
		table.addAttribute("Median time gap", "gap_median", AttributeTypes.integer);
		int medianTimeGapIdx = table.getAttrCount() - 1;
		table.addAttribute("Min length of internal path", "len_min", AttributeTypes.real);
		int minLenIdx = table.getAttrCount() - 1;
		table.addAttribute("Max length of internal path", "len_max", AttributeTypes.real);
		int maxLenIdx = table.getAttrCount() - 1;
		table.addAttribute("Mean length of internal path", "len_mean", AttributeTypes.real);
		int meanLenIdx = table.getAttrCount() - 1;
		table.addAttribute("Median length of internal path", "len_median", AttributeTypes.real);
		int medianLenIdx = table.getAttrCount() - 1;
		table.addAttribute("Total length of internal path", "len_total", AttributeTypes.real);
		int totalLenIdx = table.getAttrCount() - 1;
		table.addAttribute("Min speed", "speed_min", AttributeTypes.real);
		int minSpeedIdx = table.getAttrCount() - 1;
		table.addAttribute("Max speed", "speed_max", AttributeTypes.real);
		int maxSpeedIdx = table.getAttrCount() - 1;
		table.addAttribute("Mean speed", "speed_mean", AttributeTypes.real);
		int meanSpeedIdx = table.getAttrCount() - 1;
		table.addAttribute("Median speed", "speed_median", AttributeTypes.real);
		int medianSpeedIdx = table.getAttrCount() - 1;
		table.addAttribute("Min angle of direction change", "angle_min", AttributeTypes.integer);
		int minAngleIdx = table.getAttrCount() - 1;
		table.addAttribute("Max angle of direction change", "angle_max", AttributeTypes.integer);
		int maxAngleIdx = table.getAttrCount() - 1;
		table.addAttribute("Median angle of direction change", "angle_median", AttributeTypes.integer);
		int medianAngleIdx = table.getAttrCount() - 1;
		//information about the neighbouring places and links with them
		int maxNNeighbours = 0;
		for (int i = 0; i < geoObj.size(); i++) {
			DPlaceVisitsObject place = (DPlaceVisitsObject) geoObj.elementAt(i);
			if (place.links != null && place.links.size() > maxNNeighbours) {
				maxNNeighbours = place.links.size();
			}
		}
		int nNeiCN = -1, firstCN = -1;
		if (maxNNeighbours > 0 && maxNNeighbours <= 10) {
			nNeiCN = table.getAttrCount();
			table.addAttribute("N linked places", "n_links", AttributeTypes.integer);
			firstCN = nNeiCN + 1;
			for (int i = 0; i < maxNNeighbours; i++) {
				table.addAttribute("Linked place " + (i + 1), "link_" + (i + 1), AttributeTypes.character);
				table.addAttribute("N links to place " + (i + 1), "n_links_nei_" + (i + 1), AttributeTypes.integer);
			}
		}
		//for active trajectories (selected by filter)
		table.addAttribute("N visits (active)", "n_visits_active", AttributeTypes.integer);
		nVisIdxA = table.getAttrCount() - 1;
		table.addAttribute("N different trajectories (active)", "n_traj_active", AttributeTypes.integer);
		nTrajIdxA = table.getAttrCount() - 1;
		table.addAttribute("Max N repeated visits (active)", "max_n_rep_visits_active", AttributeTypes.integer);
		maxNRepVisIdxA = table.getAttrCount() - 1;
		table.addAttribute("N starts (active)", "n_starts_active", AttributeTypes.integer);
		nStartsIdxA = table.getAttrCount() - 1;
		table.addAttribute("N ends (active)", "n_ends_active", AttributeTypes.integer);
		nEndsIdxA = table.getAttrCount() - 1;
		table.addAttribute("Min time of entering (active)", "min_t_enter_active", AttributeTypes.time);
		tEnterIdxA = table.getAttrCount() - 1;
		table.addAttribute("Max time of leaving (active)", "max_t_exit_active", AttributeTypes.time);
		tExitIdxA = table.getAttrCount() - 1;
		table.addAttribute("Min duration of stay (active)", "dur_min_active", AttributeTypes.integer);
		minDurIdxA = table.getAttrCount() - 1;
		table.addAttribute("Max duration of stay (active)", "dur_max_active", AttributeTypes.integer);
		maxDurIdxA = table.getAttrCount() - 1;
		table.addAttribute("Mean duration of stay (active)", "dur_mean_active", AttributeTypes.integer);
		meanDurIdxA = table.getAttrCount() - 1;
		table.addAttribute("Median duration of stay (active)", "dur_median_active", AttributeTypes.integer);
		medianDurIdxA = table.getAttrCount() - 1;
		table.addAttribute("Total duration of stay (active)", "dur_total_active", AttributeTypes.integer);
		totalDurIdxA = table.getAttrCount() - 1;
		table.addAttribute("Min time gap (active)", "gap_min_active", AttributeTypes.integer);
		minTimeGapIdxA = table.getAttrCount() - 1;
		table.addAttribute("Max time gap (active)", "gap_max_active", AttributeTypes.integer);
		maxTimeGapIdxA = table.getAttrCount() - 1;
		table.addAttribute("Mean time gap (active)", "gap_mean_active", AttributeTypes.integer);
		meanTimeGapIdxA = table.getAttrCount() - 1;
		table.addAttribute("Median time gap (active)", "gap_median_active", AttributeTypes.integer);
		medianTimeGapIdxA = table.getAttrCount() - 1;
		table.addAttribute("Min length of internal path (active)", "len_min_active", AttributeTypes.real);
		minLenIdxA = table.getAttrCount() - 1;
		table.addAttribute("Max length of internal path (active)", "len_max_active", AttributeTypes.real);
		maxLenIdxA = table.getAttrCount() - 1;
		table.addAttribute("Mean length of internal path (active)", "len_mean_active", AttributeTypes.real);
		meanLenIdxA = table.getAttrCount() - 1;
		table.addAttribute("Median length of internal path (active)", "len_median_active", AttributeTypes.real);
		medianLenIdxA = table.getAttrCount() - 1;
		table.addAttribute("Total length of internal path (active)", "len_total_active", AttributeTypes.real);
		totalLenIdxA = table.getAttrCount() - 1;
		table.addAttribute("Min speed (active)", "speed_min_active", AttributeTypes.real);
		minSpeedIdxA = table.getAttrCount() - 1;
		table.addAttribute("Max speed (active)", "speed_max_active", AttributeTypes.real);
		maxSpeedIdxA = table.getAttrCount() - 1;
		table.addAttribute("Mean speed (active)", "speed_mean_active", AttributeTypes.real);
		meanSpeedIdxA = table.getAttrCount() - 1;
		table.addAttribute("Median speed (active)", "speed_median_active", AttributeTypes.real);
		medianSpeedIdxA = table.getAttrCount() - 1;
		table.addAttribute("Min angle of direction change (active)", "angle_min_active", AttributeTypes.integer);
		minAngleIdxA = table.getAttrCount() - 1;
		table.addAttribute("Max angle of direction change (active)", "angle_max_active", AttributeTypes.integer);
		maxAngleIdxA = table.getAttrCount() - 1;
		table.addAttribute("Median angle of direction change (active)", "angle_median_active", AttributeTypes.integer);
		medianAngleIdxA = table.getAttrCount() - 1;

		for (int i = 0; i < geoObj.size(); i++)
			if (geoObj.elementAt(i) instanceof DPlaceVisitsObject) {
				DPlaceVisitsObject pvObj = (DPlaceVisitsObject) geoObj.elementAt(i);
				PlaceVisitsStatistics pvStat = pvObj.getPlaceVisitsStatistics(null, null);
				if (pvStat == null) {
					continue;
				}
				DataRecord rec = new DataRecord(pvObj.getIdentifier());
				table.addDataRecord(rec);
				rec.setNumericAttrValue(pvStat.nVisits, String.valueOf(pvStat.nVisits), nVisIdx);
				rec.setNumericAttrValue(pvStat.nVisits, String.valueOf(pvStat.nVisits), nVisIdxA);
				rec.setNumericAttrValue(pvStat.nTrajectories, String.valueOf(pvStat.nTrajectories), nTrajIdx);
				rec.setNumericAttrValue(pvStat.nTrajectories, String.valueOf(pvStat.nTrajectories), nTrajIdxA);
				rec.setNumericAttrValue(pvStat.maxNRepeatedVisits, String.valueOf(pvStat.maxNRepeatedVisits), maxNRepVisIdx);
				rec.setNumericAttrValue(pvStat.maxNRepeatedVisits, String.valueOf(pvStat.maxNRepeatedVisits), maxNRepVisIdxA);
				rec.setNumericAttrValue(pvStat.nStarts, String.valueOf(pvStat.nStarts), nStartsIdx);
				rec.setNumericAttrValue(pvStat.nStarts, String.valueOf(pvStat.nStarts), nStartsIdxA);
				rec.setNumericAttrValue(pvStat.nEnds, String.valueOf(pvStat.nEnds), nEndsIdx);
				rec.setNumericAttrValue(pvStat.nEnds, String.valueOf(pvStat.nEnds), nEndsIdxA);
				rec.setAttrValue(pvStat.firstEnterTime, tEnterIdx);
				rec.setAttrValue(pvStat.firstEnterTime, tEnterIdxA);
				rec.setAttrValue(pvStat.lastExitTime, tExitIdx);
				rec.setAttrValue(pvStat.lastExitTime, tExitIdxA);
				rec.setNumericAttrValue(pvStat.minStayDuration, String.valueOf(pvStat.minStayDuration), minDurIdx);
				rec.setNumericAttrValue(pvStat.minStayDuration, String.valueOf(pvStat.minStayDuration), minDurIdxA);
				rec.setNumericAttrValue(pvStat.maxStayDuration, String.valueOf(pvStat.maxStayDuration), maxDurIdx);
				rec.setNumericAttrValue(pvStat.maxStayDuration, String.valueOf(pvStat.maxStayDuration), maxDurIdxA);
				rec.setNumericAttrValue(pvStat.averStayDuration, String.valueOf(pvStat.averStayDuration), meanDurIdx);
				rec.setNumericAttrValue(pvStat.averStayDuration, String.valueOf(pvStat.averStayDuration), meanDurIdxA);
				rec.setNumericAttrValue(pvStat.medianStayDuration, String.valueOf(pvStat.medianStayDuration), medianDurIdx);
				rec.setNumericAttrValue(pvStat.medianStayDuration, String.valueOf(pvStat.medianStayDuration), medianDurIdxA);
				rec.setNumericAttrValue(pvStat.totalStayDuration, String.valueOf(pvStat.totalStayDuration), totalDurIdx);
				rec.setNumericAttrValue(pvStat.totalStayDuration, String.valueOf(pvStat.totalStayDuration), totalDurIdxA);
				rec.setNumericAttrValue(pvStat.minTimeGap, String.valueOf(pvStat.minTimeGap), minTimeGapIdx);
				rec.setNumericAttrValue(pvStat.minTimeGap, String.valueOf(pvStat.minTimeGap), minTimeGapIdxA);
				rec.setNumericAttrValue(pvStat.maxTimeGap, String.valueOf(pvStat.maxTimeGap), maxTimeGapIdx);
				rec.setNumericAttrValue(pvStat.maxTimeGap, String.valueOf(pvStat.maxTimeGap), maxTimeGapIdxA);
				rec.setNumericAttrValue(pvStat.averTimeGap, String.valueOf(pvStat.averTimeGap), meanTimeGapIdx);
				rec.setNumericAttrValue(pvStat.averTimeGap, String.valueOf(pvStat.averTimeGap), meanTimeGapIdxA);
				rec.setNumericAttrValue(pvStat.medianTimeGap, String.valueOf(pvStat.medianTimeGap), medianTimeGapIdx);
				rec.setNumericAttrValue(pvStat.medianTimeGap, String.valueOf(pvStat.medianTimeGap), medianTimeGapIdxA);
				rec.setNumericAttrValue(pvStat.minLen, String.valueOf(pvStat.minLen), minLenIdx);
				rec.setNumericAttrValue(pvStat.minLen, String.valueOf(pvStat.minLen), minLenIdxA);
				rec.setNumericAttrValue(pvStat.maxLen, String.valueOf(pvStat.maxLen), maxLenIdx);
				rec.setNumericAttrValue(pvStat.maxLen, String.valueOf(pvStat.maxLen), maxLenIdxA);
				rec.setNumericAttrValue(pvStat.averLen, String.valueOf(pvStat.averLen), meanLenIdx);
				rec.setNumericAttrValue(pvStat.averLen, String.valueOf(pvStat.averLen), meanLenIdxA);
				rec.setNumericAttrValue(pvStat.medianLen, String.valueOf(pvStat.medianLen), medianLenIdx);
				rec.setNumericAttrValue(pvStat.medianLen, String.valueOf(pvStat.medianLen), medianLenIdxA);
				rec.setNumericAttrValue(pvStat.totalLenInside, String.valueOf(pvStat.totalLenInside), totalLenIdx);
				rec.setNumericAttrValue(pvStat.totalLenInside, String.valueOf(pvStat.totalLenInside), totalLenIdxA);
				rec.setNumericAttrValue(pvStat.minSpeed, String.valueOf(pvStat.minSpeed), minSpeedIdx);
				rec.setNumericAttrValue(pvStat.minSpeed, String.valueOf(pvStat.minSpeed), minSpeedIdxA);
				rec.setNumericAttrValue(pvStat.maxSpeed, String.valueOf(pvStat.maxSpeed), maxSpeedIdx);
				rec.setNumericAttrValue(pvStat.maxSpeed, String.valueOf(pvStat.maxSpeed), maxSpeedIdxA);
				rec.setNumericAttrValue(pvStat.averSpeed, String.valueOf(pvStat.averSpeed), meanSpeedIdx);
				rec.setNumericAttrValue(pvStat.averSpeed, String.valueOf(pvStat.averSpeed), meanSpeedIdxA);
				rec.setNumericAttrValue(pvStat.medianSpeed, String.valueOf(pvStat.medianSpeed), medianSpeedIdx);
				rec.setNumericAttrValue(pvStat.medianSpeed, String.valueOf(pvStat.medianSpeed), medianSpeedIdxA);
				rec.setNumericAttrValue(pvStat.minAngleDirChange, String.valueOf(pvStat.minAngleDirChange), minAngleIdx);
				rec.setNumericAttrValue(pvStat.minAngleDirChange, String.valueOf(pvStat.minAngleDirChange), minAngleIdxA);
				rec.setNumericAttrValue(pvStat.maxAngleDirChange, String.valueOf(pvStat.maxAngleDirChange), maxAngleIdx);
				rec.setNumericAttrValue(pvStat.maxAngleDirChange, String.valueOf(pvStat.maxAngleDirChange), maxAngleIdxA);
				rec.setNumericAttrValue(pvStat.medianAngleDirChange, String.valueOf(pvStat.medianAngleDirChange), medianAngleIdx);
				rec.setNumericAttrValue(pvStat.medianAngleDirChange, String.valueOf(pvStat.medianAngleDirChange), medianAngleIdxA);
				if (nNeiCN >= 0) {
					int aIdx = firstCN;
					if (pvObj.links != null && pvObj.links.size() > 0) {
						rec.setNumericAttrValue(pvObj.links.size(), String.valueOf(pvObj.links.size()), nNeiCN);
						BubbleSort.sort(pvObj.links);
						for (int j = 0; j < pvObj.links.size(); j++) {
							rec.setAttrValue(pvObj.links.elementAt(j).obj, aIdx++);
							rec.setNumericAttrValue(pvObj.links.elementAt(j).count, aIdx++);
						}
						for (int j = pvObj.links.size(); j < maxNNeighbours; j++) {
							rec.setAttrValue(null, aIdx++);
							rec.setNumericAttrValue(0, aIdx++);
						}
					} else {
						rec.setNumericAttrValue(0, "0", nNeiCN);
						for (int j = 0; j < maxNNeighbours; j++) {
							rec.setAttrValue(null, aIdx++);
							rec.setNumericAttrValue(0, aIdx++);
						}
					}
				}
				pvObj.setThematicData(rec);
			}

		return table;
	}

	/**
	 * Computes the distortions in the places and the maximum distortion
	 */
	public void computeDistortions() {
		if (maxDistortion > 0)
			return; //already computed
		DataTable table = null;
		int nSumDisto = 0;
		if (dTable != null && (dTable instanceof DataTable)) {
			table = (DataTable) dTable;
			nSumDisto = table.getAttrCount();
			table.addAttribute("Total displacement", "sum_disto", AttributeTypes.real);
			table.addAttribute("Mean displacement", "mean_disto", AttributeTypes.real);
			table.addAttribute("Max displacement", "max_disto", AttributeTypes.real);
		}
		int nVisits = 0;
		for (int i = 0; i < geoObj.size(); i++)
			if (geoObj.elementAt(i) instanceof DPlaceVisitsObject) {
				DPlaceVisitsObject plObj = (DPlaceVisitsObject) geoObj.elementAt(i);
				DataRecord rec = (DataRecord) plObj.getData();
				if (plObj.visits == null || plObj.visits.size() < 1) {
/*
          if (rec!=null) {
            rec.setNumericAttrValue(0,"0",nSumDisto);
            rec.setNumericAttrValue(0,"0",nSumDisto+1);
            rec.setNumericAttrValue(0,"0",nSumDisto+2);
          }
*/
					continue;
				}
				plObj.computeDistortions();
				if (plObj.maxDistortion > maxDistortion) {
					maxDistortion = plObj.maxDistortion;
				}
				if (plObj.sumDistortion > maxSumDistortion) {
					maxSumDistortion = plObj.sumDistortion;
				}
				if (plObj.meanDistortion > maxMeanDistortion) {
					maxMeanDistortion = plObj.meanDistortion;
				}
				sumDistortion += plObj.sumDistortion;
				nVisits += plObj.visits.size();
				if (rec != null) {
					rec.setNumericAttrValue(plObj.sumDistortion, String.valueOf(plObj.sumDistortion), nSumDisto);
					rec.setNumericAttrValue(plObj.meanDistortion, String.valueOf(plObj.meanDistortion), nSumDisto + 1);
					rec.setNumericAttrValue(plObj.maxDistortion, String.valueOf(plObj.maxDistortion), nSumDisto + 2);
				}
			}
		meanDistortion = sumDistortion / nVisits;
	}

	/**
	 * Computes the distortions in the places by intervals.
	 * Puts the information in table columns
	 */
	public void computeDistortionsByIntervals(float upperLimit, int nIntervals) {
		DataTable table = (DataTable) dTable;
		int nDistoStart = table.getAttrCount();
		float low = 0f;
		for (int i = 0; i < nIntervals; i++) {
			float high = upperLimit * (i + 1) / nIntervals;
			table.addAttribute("Distortion from " + low + " to " + high, "disto_" + (i + 1), AttributeTypes.integer);
			low = high;
		}
		for (int i = 0; i < geoObj.size(); i++)
			if (geoObj.elementAt(i) instanceof DPlaceVisitsObject) {
				DPlaceVisitsObject pvObj = (DPlaceVisitsObject) geoObj.elementAt(i);
				pvObj.countDistortionsByIntervals(upperLimit, nIntervals);
				DataRecord rec = (DataRecord) pvObj.getData();
				if (rec == null) {
					continue;
				}
				for (int j = 0; j < nIntervals; j++)
					if (pvObj.distoCounts == null) {
						rec.setNumericAttrValue(0, "0", nDistoStart + j);
					} else {
						rec.setNumericAttrValue(pvObj.distoCounts[j], String.valueOf(pvObj.distoCounts[j]), nDistoStart + j);
					}
			}
		distoByIntervalsCounted = true;
	}

	/**
	 * Returns the reference to the layer with the original trajectories
	 * the links have been constructed from. May be null.
	 */
	public DGeoLayer getTrajectoryLayer() {
		return trajLayer;
	}

	/**
	 * Sets a reference to the layer with the original trajectories
	 * passing the places. Starts listening to changes of the layer filter.
	 */
	public void setTrajectoryLayer(DGeoLayer trajLayer) {
		if (this.trajLayer != null) {
			this.trajLayer.removePropertyChangeListener(this);
		}
		this.trajLayer = trajLayer;
		if (trajLayer != null) {
			trajLayer.addPropertyChangeListener(this);
		}
	}

	/**
	 * Returns the reference to the layer with the simplified trajectories
	 * the links have been constructed from. May be null.
	 */
	public DGeoLayer getSimpleTrajectoryLayer() {
		return simpleTrajLayer;
	}

	/**
	 * Sets a reference to the layer with the simplified trajectories
	 * passing the places. Starts listening to changes of the layer filter.
	 */
	public void setSimpleTrajectoryLayer(DGeoLayer simpleTrajLayer) {
		if (this.simpleTrajLayer != null) {
			this.simpleTrajLayer.removePropertyChangeListener(this);
		}
		this.simpleTrajLayer = simpleTrajLayer;
		if (simpleTrajLayer != null) {
			simpleTrajLayer.addPropertyChangeListener(this);
		}
	}

	/**
	 * Finds out which trajectories are currently active according to the filter of the
	 * layer(s) with the trajectories. Returns true if the list of active trajectories
	 * has really changed
	 */
	protected boolean findActiveTrajectories() {
		if (trajLayer == null && simpleTrajLayer == null)
			return false;
		int nTraj = (trajLayer == null) ? simpleTrajLayer.getObjectCount() : trajLayer.getObjectCount();
		if (nTraj < 1)
			return false;
		boolean changed = false;
		if (activeTrajIds == null) {
			activeTrajIds = new Vector(nTraj, 1);
			changed = true;
		}
		for (int i = 0; i < nTraj; i++) {
			boolean active = (trajLayer == null || trajLayer.isObjectActive(i)) && (simpleTrajLayer == null || simpleTrajLayer.isObjectActive(i));
			String trId = (trajLayer == null) ? simpleTrajLayer.getObjectId(i) : trajLayer.getObjectId(i);
			int idx = activeTrajIds.indexOf(trId);
			if (active)
				if (idx >= 0) {
					;
				} else {
					activeTrajIds.addElement(trId);
					changed = true;
				}
			else if (idx >= 0) {
				activeTrajIds.removeElementAt(idx);
				changed = true;
			}
		}
		return changed;
	}

	/**
	 * Returns the identifiers of the active trajectories (i.e. satisfying all
	 * current filters)
	 */
	public Vector getActiveTrajIds() {
		return activeTrajIds;
	}

	public void draw(Graphics g, MapContext mc) {
		if (geoObj == null || geoObj.size() < 1 || g == null || mc == null)
			return;
		if (neverDrawn) {
			neverDrawn = false;
			findActiveTrajectories();
			recomputeStatistics();
			if (dTable != null)
				return;
		}
		boolean timeFilterSame = false;
		TimeFilter tf = getTimeFilter();
		if (tf == null) {
			timeFilterSame = tStart == null && tEnd == null;
		} else {
			TimeMoment t1 = tf.getFilterPeriodStart(), t2 = tf.getFilterPeriodEnd();
			timeFilterSame = ((t1 == null && tStart == null) || (t1 != null && tStart != null && t1.equals(tStart))) && ((t2 == null && tEnd == null) || (t2 != null && tEnd != null && t2.equals(tEnd)));
		}
		if (!timeFilterSame) {
			recomputeStatistics();
			if (dTable != null)
				return;
		}
		super.draw(g, mc);
	}

	/**
	 * Reacts to changes of the filter of the trajectory layer(s)
	 */
	public void propertyChange(PropertyChangeEvent pce) {
		if (!pce.getSource().equals(trajLayer) && !pce.getSource().equals(simpleTrajLayer)) {
			super.propertyChange(pce);
			return;
		}
		if (pce.getPropertyName().equals("ObjectFilter")) {
			if (dTable != null && nVisIdxA >= 0 && findActiveTrajectories()) {
				recomputeStatistics();
				notifyPropertyChange("ObjectData", null, null);
			}
		}
	}

	/**
	 * Recomputes the statistics after changes of the filter of the trajectories or
	 * the time filter
	 */
	protected void recomputeStatistics() {
		if (dTable == null || nVisIdxA < 0)
			return;
		TimeFilter tf = getTimeFilter();
		TimeMoment t1 = null, t2 = null;
		boolean timeFiltered = false;
		if (tf != null) {
			t1 = tf.getFilterPeriodStart();
			t2 = tf.getFilterPeriodEnd();
			timeFiltered = t1 != null || t2 != null;
		}
		tStart = (t1 == null) ? null : t1.getCopy();
		tEnd = (t2 == null) ? null : t2.getCopy();
		TimeReference timeLimits = null;
		if (timeFiltered) {
			timeLimits = new TimeReference();
			timeLimits.setValidFrom(tStart);
			timeLimits.setValidUntil(tEnd);
		}
		for (int i = 0; i < geoObj.size(); i++)
			if (geoObj.elementAt(i) instanceof DPlaceVisitsObject) {
				DPlaceVisitsObject pvObj = (DPlaceVisitsObject) geoObj.elementAt(i);
				if (pvObj.getData() == null || !(pvObj.getData() instanceof DataRecord)) {
					continue;
				}
				DataRecord rec = (DataRecord) pvObj.getData();
				PlaceVisitsStatistics pvStat = null;
				if (activeTrajIds != null && activeTrajIds.size() > 0) {
					pvStat = pvObj.getPlaceVisitsStatistics(activeTrajIds, timeLimits);
				}
				if (pvStat == null) {
					rec.setNumericAttrValue(0, "0", nVisIdxA);
					rec.setNumericAttrValue(0, "0", nTrajIdxA);
					rec.setNumericAttrValue(0, "0", maxNRepVisIdxA);
					rec.setNumericAttrValue(0, "0", nStartsIdxA);
					rec.setNumericAttrValue(0, "0", nEndsIdxA);
					rec.setAttrValue(null, tEnterIdxA);
					rec.setAttrValue(null, tExitIdxA);
					rec.setNumericAttrValue(0, "0", minDurIdxA);
					rec.setNumericAttrValue(0, "0", maxDurIdxA);
					rec.setNumericAttrValue(0, "0", meanDurIdxA);
					rec.setNumericAttrValue(0, "0", medianDurIdxA);
					rec.setNumericAttrValue(0, "0", totalDurIdxA);
					rec.setNumericAttrValue(0, "0", minTimeGapIdxA);
					rec.setNumericAttrValue(0, "0", maxTimeGapIdxA);
					rec.setNumericAttrValue(0, "0", meanTimeGapIdxA);
					rec.setNumericAttrValue(0, "0", medianTimeGapIdxA);
					rec.setNumericAttrValue(0, "0", minLenIdxA);
					rec.setNumericAttrValue(0, "0", maxLenIdxA);
					rec.setNumericAttrValue(0, "0", meanLenIdxA);
					rec.setNumericAttrValue(0, "0", medianLenIdxA);
					rec.setNumericAttrValue(0, "0", totalLenIdxA);
					rec.setNumericAttrValue(0, "0", minSpeedIdxA);
					rec.setNumericAttrValue(0, "0", maxSpeedIdxA);
					rec.setNumericAttrValue(0, "0", meanSpeedIdxA);
					rec.setNumericAttrValue(0, "0", medianSpeedIdxA);
					rec.setNumericAttrValue(0, "0", minAngleIdxA);
					rec.setNumericAttrValue(0, "0", maxAngleIdxA);
					rec.setNumericAttrValue(0, "0", medianAngleIdxA);
				} else {
					rec.setNumericAttrValue(pvStat.nVisits, String.valueOf(pvStat.nVisits), nVisIdxA);
					rec.setNumericAttrValue(pvStat.nTrajectories, String.valueOf(pvStat.nTrajectories), nTrajIdxA);
					rec.setNumericAttrValue(pvStat.maxNRepeatedVisits, String.valueOf(pvStat.maxNRepeatedVisits), maxNRepVisIdxA);
					rec.setNumericAttrValue(pvStat.nStarts, String.valueOf(pvStat.nStarts), nStartsIdxA);
					rec.setNumericAttrValue(pvStat.nEnds, String.valueOf(pvStat.nEnds), nEndsIdxA);
					rec.setAttrValue(pvStat.firstEnterTime, tEnterIdxA);
					rec.setAttrValue(pvStat.lastExitTime, tExitIdxA);
					rec.setNumericAttrValue(pvStat.minStayDuration, String.valueOf(pvStat.minStayDuration), minDurIdxA);
					rec.setNumericAttrValue(pvStat.maxStayDuration, String.valueOf(pvStat.maxStayDuration), maxDurIdxA);
					rec.setNumericAttrValue(pvStat.averStayDuration, String.valueOf(pvStat.averStayDuration), meanDurIdxA);
					rec.setNumericAttrValue(pvStat.medianStayDuration, String.valueOf(pvStat.medianStayDuration), medianDurIdxA);
					rec.setNumericAttrValue(pvStat.totalStayDuration, String.valueOf(pvStat.totalStayDuration), totalDurIdxA);
					rec.setNumericAttrValue(pvStat.minTimeGap, String.valueOf(pvStat.minTimeGap), minTimeGapIdxA);
					rec.setNumericAttrValue(pvStat.maxTimeGap, String.valueOf(pvStat.maxTimeGap), maxTimeGapIdxA);
					rec.setNumericAttrValue(pvStat.averTimeGap, String.valueOf(pvStat.averTimeGap), meanTimeGapIdxA);
					rec.setNumericAttrValue(pvStat.medianTimeGap, String.valueOf(pvStat.medianTimeGap), medianTimeGapIdxA);
					rec.setNumericAttrValue(pvStat.minLen, String.valueOf(pvStat.minLen), minLenIdxA);
					rec.setNumericAttrValue(pvStat.maxLen, String.valueOf(pvStat.maxLen), maxLenIdxA);
					rec.setNumericAttrValue(pvStat.averLen, String.valueOf(pvStat.averLen), meanLenIdxA);
					rec.setNumericAttrValue(pvStat.medianLen, String.valueOf(pvStat.medianLen), medianLenIdxA);
					rec.setNumericAttrValue(pvStat.totalLenInside, String.valueOf(pvStat.totalLenInside), totalLenIdxA);
					rec.setNumericAttrValue(pvStat.minSpeed, String.valueOf(pvStat.minSpeed), minSpeedIdxA);
					rec.setNumericAttrValue(pvStat.maxSpeed, String.valueOf(pvStat.maxSpeed), maxSpeedIdxA);
					rec.setNumericAttrValue(pvStat.averSpeed, String.valueOf(pvStat.averSpeed), meanSpeedIdxA);
					rec.setNumericAttrValue(pvStat.medianSpeed, String.valueOf(pvStat.medianSpeed), medianSpeedIdxA);
					rec.setNumericAttrValue(pvStat.minAngleDirChange, String.valueOf(pvStat.minAngleDirChange), minAngleIdxA);
					rec.setNumericAttrValue(pvStat.maxAngleDirChange, String.valueOf(pvStat.maxAngleDirChange), maxAngleIdxA);
					rec.setNumericAttrValue(pvStat.medianAngleDirChange, String.valueOf(pvStat.medianAngleDirChange), medianAngleIdxA);
				}
			}
		Vector attrIds = new Vector(30, 10);
		for (int i = nVisIdxA; i < dTable.getAttrCount(); i++) {
			attrIds.addElement(dTable.getAttributeId(i));
		}
		dTable.notifyPropertyChange("values", null, attrIds);
	}

	/**
	* Returns a copy of this GeoLayer. The reference to the visualizer is also copied.
	*/
	public GeoLayer makeCopy() {
		DPlaceVisitsLayer layer = new DPlaceVisitsLayer();
		if (name != null) {
			layer.name = new String(name);
		}
		if (id != null) {
			layer.setContainerIdentifier(new String(id));
		}
		if (setId != null) {
			layer.setEntitySetIdentifier(new String(setId));
		}
		layer.hasAllObjects = hasAllObjects;
		layer.hasLabels = hasLabels;
		layer.setGeographic(isGeographic());
		if (getObjectCount() > 0) {
			Vector gobj = new Vector(getObjectCount(), 100);
			for (int i = 0; i < getObjectCount(); i++) {
				gobj.addElement(getObject(i).makeCopy());
			}
			layer.setGeoObjects(gobj, hasAllObjects);
		}
		layer.setTrajectoryLayer(trajLayer);
		layer.setSimpleTrajectoryLayer(simpleTrajLayer);
		layer.setDataSupplier(dataSuppl);
		layer.setDrawingParameters(drawParm.makeCopy());
		layer.setIsActive(isActive);
		layer.setType(objType);
		layer.setDataTable(dTable);
		layer.setLinkedToTable(linkedToTable);
		layer.setObjectFilter(oFilter);
		layer.lastPixelValue = lastPixelValue;
		if (vis != null) {
			layer.setVisualizer(vis);
		}
		if (bkgVis != null) {
			layer.setBackgroundVisualizer(bkgVis);
		}
		return layer;
	}

	/**
	 * Among the given copies of layers, looks for the copies of the
	 * layers this layer is linked to (by their identifiers) and replaces
	 * the references to the original layers by the references to their copies.
	 */
	public void checkAndCorrectLinks(Vector copiesOfMapLayers) {
		if ((trajLayer == null && simpleTrajLayer == null) || copiesOfMapLayers == null || copiesOfMapLayers.size() < 1)
			return;
		boolean trajLayerOK = trajLayer == null;
		boolean simpleTrajLayerOK = simpleTrajLayer == null;
		for (int i = 0; i < copiesOfMapLayers.size() && (!trajLayerOK || !simpleTrajLayerOK); i++)
			if (copiesOfMapLayers.elementAt(i) instanceof DGeoLayer) {
				DGeoLayer layer = (DGeoLayer) copiesOfMapLayers.elementAt(i);
				if (!trajLayerOK && layer.getEntitySetIdentifier().equals(trajLayer.getEntitySetIdentifier())) {
					setTrajectoryLayer(layer);
					trajLayerOK = true;
				} else if (!simpleTrajLayerOK && layer.getEntitySetIdentifier().equals(simpleTrajLayer.getEntitySetIdentifier())) {
					setSimpleTrajectoryLayer(layer);
					simpleTrajLayerOK = true;
				}
			}
	}

	/**
	 * Removes itself from listeners of the filtering events of the layers
	 * with the trajectories
	 */
	public void destroy() {
		if (destroyed)
			return;
		if (trajLayer != null) {
			trajLayer.removePropertyChangeListener(this);
		}
		if (simpleTrajLayer != null) {
			simpleTrajLayer.removePropertyChangeListener(this);
		}
		super.destroy();
	}

	/**
	* Returns its time filter, if available
	*/
	public TimeFilter getTimeFilter() {
		TimeFilter tf = null;
		if (trajLayer != null) {
			tf = trajLayer.getTimeFilter();
		}
		if (tf != null)
			return tf;
		if (simpleTrajLayer != null) {
			tf = simpleTrajLayer.getTimeFilter();
		}
		if (tf != null)
			return tf;
		return super.getTimeFilter();
	}

	protected boolean drawContoursOfInactiveObjects() {
		return false;
	}

	/**
	 * A layer with the points representing the visits of the places
	 */
	public DGeoLayer getVisitsPointLayer() {
		return visitsPointLayer;
	}

	/**
	 * A layer with the points representing the visits of the places
	 */
	public void setVisitsPointLayer(DGeoLayer visitsPointLayer) {
		this.visitsPointLayer = visitsPointLayer;
	}

	/**
	 * If this layer has been built by refining another DPlaceVisitsLayer,
	 * this method returns a reference to the original DPlaceVisitsLayer
	 */
	public DPlaceVisitsLayer getOrigPlaceLayer() {
		return origPlaceLayer;
	}

	/**
	 * If this layer has been built by refining another DPlaceVisitsLayer,
	 * this method sets a reference to the original DPlaceVisitsLayer
	 */
	public void setOrigPlaceLayer(DPlaceVisitsLayer origPlaceLayer) {
		this.origPlaceLayer = origPlaceLayer;
	}
}
