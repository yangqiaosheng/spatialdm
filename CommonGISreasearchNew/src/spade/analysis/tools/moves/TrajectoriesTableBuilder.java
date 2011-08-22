package spade.analysis.tools.moves;

import java.util.Vector;

import spade.lib.util.DoubleArray;
import spade.lib.util.IntArray;
import spade.time.Date;
import spade.time.TimeMoment;
import spade.time.TimeReference;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.database.SpatialEntity;
import spade.vis.dmap.DMovingObject;
import spade.vis.geometry.RealPoint;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: 23-Aug-2007
 * Time: 12:12:04
 * For a given set of trajectories, generates a table with thematic data.
 */
public class TrajectoriesTableBuilder {
	/**
	 * For a given set of trajectories, generates a table with thematic data.
	 * @param mObjects - trajectories, i.e. instances of DMovingObject
	 * @return the generated table
	 */
	public static DataTable makeTrajectoryDataTable(Vector mObjects) {
		if (mObjects == null || mObjects.size() < 1)
			return null;
		int nMovingObj = mObjects.size();
		Vector trIds = new Vector(nMovingObj, 1), eIds = null, startTimes = new Vector(nMovingObj, 1), endTimes = new Vector(nMovingObj, 1);
		IntArray nPos = new IntArray(nMovingObj, 1);
		DoubleArray dist = new DoubleArray(nMovingObj, 1);
		nMovingObj = 0;
		for (int i = 0; i < mObjects.size(); i++)
			if (mObjects.elementAt(i) instanceof DMovingObject) {
				++nMovingObj;
				DMovingObject mobj = (DMovingObject) mObjects.elementAt(i);
				trIds.addElement(mobj.getIdentifier());
				if (mobj.getEntityId() != null) {
					if (eIds == null) {
						eIds = new Vector(nMovingObj, 1);
					}
					while (eIds.size() < i) {
						eIds.addElement(null);
					}
					eIds.addElement(mobj.getEntityId());
				}
				startTimes.addElement(mobj.getStartTime());
				endTimes.addElement(mobj.getEndTime());
				nPos.addElement(mobj.getPositionCount());
				dist.addElement(mobj.getTrackLength());
			}
		if (nMovingObj < 1)
			return null;
		DataTable dtTraj = makeMoveSummaryTable(trIds, eIds, nPos, dist, null, null, startTimes, endTimes);
		int k = 0;
		for (int i = 0; i < mObjects.size(); i++)
			if (mObjects.elementAt(i) instanceof DMovingObject) {
				DMovingObject mObj = (DMovingObject) mObjects.elementAt(i);
				mObj.setThematicData(dtTraj.getDataRecord(k++));
			}
		return dtTraj;
	}

	/**
	 * For a given set of trajectories, generates a table with thematic data.
	 * @param trIds - identifiers of trajectories (strings)
	 * @param eIds - identifiers of entities (strings)
	 * @param nPos - numbers of positions (integers) - optional
	 * @param dist - track length (doubles) - optional
	 * @param starts - start places (instances of SpatialEntity) - optional
	 * @param ends - destination places (instances of SpatialEntity) - optional
	 * @param startTimes - start times (instances of TimeMoment)
	 * @param endTimes - end times (instances of TimeMoment)
	 * @return the generated table
	 */
	public static DataTable makeMoveSummaryTable(Vector trIds, Vector eIds, IntArray nPos, DoubleArray dist, Vector starts, Vector ends, Vector startTimes, Vector endTimes) {
		if (trIds == null || trIds.size() < 1)
			return null;
		if (startTimes == null || startTimes.size() < trIds.size())
			return null;
		if (endTimes == null || endTimes.size() < trIds.size())
			return null;
		boolean hasEId = eIds != null && eIds.size() >= trIds.size(), physicalTime = false;
		TimeMoment start = null, end = null;
		long minDuration = Long.MAX_VALUE;
		float maxDuration = 0;
		for (int i = 0; i < startTimes.size(); i++) {
			TimeMoment t1 = (TimeMoment) startTimes.elementAt(i), t2 = (TimeMoment) endTimes.elementAt(i);
			if (t1 != null && t2 != null) {
				physicalTime = physicalTime || (t1 instanceof Date);
				long dur = t2.subtract(t1);
				if (minDuration > dur) {
					minDuration = dur;
				}
				if (maxDuration < dur) {
					maxDuration = dur;
				}
				if (start == null || start.compareTo(t1) > 0) {
					start = t1;
				}
				if (end == null || end.compareTo(t2) < 0) {
					end = t2;
				}
			}
		}
		boolean hasYears = false, hasMonths = false, hasDays = false, hasHours = false;
		char datePrecision = start.getPrecision(), optPrecision = datePrecision;
		if (start != null && end != null && physicalTime) {
			start = start.getCopy();
			end = end.getCopy();
			Date startDate = (Date) start;
			hasYears = startDate.hasElement('y');
			hasMonths = startDate.hasElement('m') && startDate.requiresElement('m');
			hasDays = startDate.hasElement('d') && startDate.requiresElement('d');
			hasHours = startDate.hasElement('h') && startDate.requiresElement('h');
			if (hasYears) {
				start.setPrecision('y');
				end.setPrecision('y');
				hasYears = end.subtract(start) > 0;
			}
			if (!hasYears && hasMonths) {
				start.setPrecision('m');
				end.setPrecision('m');
				hasMonths = end.subtract(start) > 0;
			}
			if (!hasMonths && hasDays) {
				start.setPrecision('d');
				end.setPrecision('d');
				hasDays = end.subtract(start) > 0;
			}
			if (!hasDays && hasHours) {
				start.setPrecision('h');
				end.setPrecision('h');
				hasHours = end.subtract(start) > 0;
			}
			start.setPrecision(datePrecision);
			end.setPrecision(datePrecision);
			if (minDuration > 600 && startDate.requiresElement('s')) {
				optPrecision = 't';
				minDuration /= 60;
				if (minDuration > 600) {
					optPrecision = 'h';
					minDuration /= 60;
					if (minDuration > 240) {
						optPrecision = 'd';
						minDuration /= 24;
						if (minDuration > 300) {
							optPrecision = 'm';
							minDuration /= 30;
						}
					}
				}
			}
		}
		DataTable dtTraj = new DataTable();
		int eIdCN = -1, nCN = -1, durCN = -1, distCN = -1, startIdCN = -1, xStartCN = -1, yStartCN = -1, endIdCN = -1, xEndCN = -1, yEndCN = -1, startDTCN = -1, endDTCN = -1, startDateCN = -1, endDateCN = -1, startTimeCN = -1, endTimeCN = -1, yearCN = -1, monthCN = -1, dowCN = -1, hourCN = -1;
		if (hasEId) {
			dtTraj.addAttribute("Entity ID", "eID", AttributeTypes.character);
			eIdCN = dtTraj.getAttrCount() - 1;
		}
		if (nPos != null && nPos.size() >= trIds.size()) {
			dtTraj.addAttribute("Number of positions", "N", AttributeTypes.integer);
			nCN = dtTraj.getAttrCount() - 1;
		}
		if (dist != null && dist.size() >= trIds.size()) {
			dtTraj.addAttribute("Track length", "tLength", AttributeTypes.real);
			distCN = dtTraj.getAttrCount() - 1;
		}
		if (starts != null && starts.size() >= trIds.size() && ends != null && ends.size() >= trIds.size()) {
			dtTraj.addAttribute("Start ID", "startID", AttributeTypes.character);
			startIdCN = dtTraj.getAttrCount() - 1;
			dtTraj.addAttribute("Start X", "xs", AttributeTypes.real);
			xStartCN = dtTraj.getAttrCount() - 1;
			dtTraj.addAttribute("Start Y", "ys", AttributeTypes.real);
			yStartCN = dtTraj.getAttrCount() - 1;
			dtTraj.addAttribute("End ID", "endID", AttributeTypes.character);
			endIdCN = dtTraj.getAttrCount() - 1;
			dtTraj.addAttribute("End X", "xe", AttributeTypes.real);
			xEndCN = dtTraj.getAttrCount() - 1;
			dtTraj.addAttribute("End Y", "ye", AttributeTypes.real);
			yEndCN = dtTraj.getAttrCount() - 1;
		}
		String suffix = (physicalTime && hasDays) ? (hasHours) ? "date+time" : "date" : "time";
		dtTraj.addAttribute("Start " + suffix, "start_date_time", AttributeTypes.time);
		startDTCN = dtTraj.getAttrCount() - 1;
		Attribute timeAttr = dtTraj.getAttribute(startDTCN);
		timeAttr.timeRefMeaning = Attribute.VALID_FROM;
		dtTraj.addAttribute("End " + suffix, "end_date_time", AttributeTypes.time);
		endDTCN = dtTraj.getAttrCount() - 1;
		timeAttr = dtTraj.getAttribute(endDTCN);
		timeAttr.timeRefMeaning = Attribute.VALID_UNTIL;
		if (physicalTime && hasDays && hasHours) {
			dtTraj.addAttribute("Start date", "start_date", AttributeTypes.time);
			startDateCN = dtTraj.getAttrCount() - 1;
			dtTraj.addAttribute("End date", "end_date", AttributeTypes.time);
			endDateCN = dtTraj.getAttrCount() - 1;
			dtTraj.addAttribute("Start time", "start_time", AttributeTypes.time);
			startTimeCN = dtTraj.getAttrCount() - 1;
			dtTraj.addAttribute("End time", "end_time", AttributeTypes.time);
			endTimeCN = dtTraj.getAttrCount() - 1;
		}
		String aName = (physicalTime) ? "Duration (" + Date.getTextForTimeSymbol(optPrecision) + ")" : "Duration";
		dtTraj.addAttribute(aName, "duration", AttributeTypes.integer);
		durCN = dtTraj.getAttrCount() - 1;
		int durMinsCN = -1, durHoursCN = -1, durDaysCN = -1;
		if (physicalTime) {
			if (optPrecision == 's' && maxDuration / 60 > 1) {
				dtTraj.addAttribute("Duration (minutes)", "duration_mins", AttributeTypes.real);
				durMinsCN = dtTraj.getAttrCount() - 1;
				maxDuration /= 60;
			}
			if ((optPrecision == 's' || optPrecision == 't') && maxDuration / 60 > 1) {
				dtTraj.addAttribute("Duration (hours)", "duration_hours", AttributeTypes.real);
				durHoursCN = dtTraj.getAttrCount() - 1;
				maxDuration /= 60;
			}
			if ((optPrecision == 's' || optPrecision == 't' || optPrecision == 'h') && maxDuration / 24 > 1) {
				dtTraj.addAttribute("Duration (days)", "duration_days", AttributeTypes.real);
				durDaysCN = dtTraj.getAttrCount() - 1;
				maxDuration /= 24;
			}
			if (hasYears) {
				dtTraj.addAttribute("Year (start)", "year_start", AttributeTypes.integer);
				yearCN = dtTraj.getAttrCount() - 1;
				dtTraj.addAttribute("Year (end)", "year_end", AttributeTypes.integer);
			}
			if (hasMonths) {
				dtTraj.addAttribute("Month (start)", "month_start", AttributeTypes.integer);
				monthCN = dtTraj.getAttrCount() - 1;
				dtTraj.addAttribute("Month (end)", "month_end", AttributeTypes.integer);
			}
			if (hasDays) {
				dtTraj.addAttribute("Day of week (start)", "dow_start", AttributeTypes.integer);
				dowCN = dtTraj.getAttrCount() - 1;
				dtTraj.addAttribute("Day of week (end)", "dow_end", AttributeTypes.integer);
			}
			if (hasHours) {
				dtTraj.addAttribute("Hour (start)", "hour_start", AttributeTypes.integer);
				hourCN = dtTraj.getAttrCount() - 1;
				dtTraj.addAttribute("Hour (end)", "hour_end", AttributeTypes.integer);
			}
		}
		Date date = null, time = null;
		if (physicalTime) {
			if (startDateCN >= 0) {
				date = new Date();
				date.setDateScheme("dd/mm/yyyy");
				date.setPrecision('d');
			}
			time = new Date();
			time.setDateScheme("hh:tt:ss");
			time.setPrecision('s');
		}

		for (int i = 0; i < trIds.size(); i++) {
			TimeMoment d1 = (TimeMoment) startTimes.elementAt(i), d2 = (TimeMoment) endTimes.elementAt(i);
			DataRecord rec = new DataRecord((String) trIds.elementAt(i));
			dtTraj.addDataRecord(rec);
			if (hasEId) {
				rec.setAttrValue(eIds.elementAt(i), eIdCN);
			}
			if (nCN >= 0) {
				rec.setNumericAttrValue(nPos.elementAt(i), String.valueOf(nPos.elementAt(i)), nCN);
			}
			if (distCN >= 0) {
				rec.setNumericAttrValue(dist.elementAt(i), String.valueOf(dist.elementAt(i)), distCN);
			}
			if (startIdCN >= 0) {
				SpatialEntity spe = (SpatialEntity) starts.elementAt(i);
				if (spe != null) {
					rec.setAttrValue(spe.getId(), startIdCN);
					RealPoint p = spe.getCentre();
					if (p != null) {
						rec.setNumericAttrValue(p.x, String.valueOf(p.x), xStartCN);
						rec.setNumericAttrValue(p.y, String.valueOf(p.y), yStartCN);
					}
				}
				spe = (SpatialEntity) ends.elementAt(i);
				if (spe != null) {
					rec.setAttrValue(spe.getId(), endIdCN);
					RealPoint p = spe.getCentre();
					if (p != null) {
						rec.setNumericAttrValue(p.x, String.valueOf(p.x), xEndCN);
						rec.setNumericAttrValue(p.y, String.valueOf(p.y), yEndCN);
					}
				}
			}
			if (startDTCN >= 0) {
				rec.setAttrValue(d1, startDTCN);
			}
			if (endDTCN >= 0) {
				rec.setAttrValue(d2, endDTCN);
			}
			if (date != null && startDateCN >= 0 && endDateCN >= 0) {
				date.setElementValue('y', d1.getElementValue('y'));
				date.setElementValue('m', d1.getElementValue('m'));
				date.setElementValue('d', d1.getElementValue('d'));
				rec.setAttrValue(date.getCopy(), startDateCN);
				date.setElementValue('y', d2.getElementValue('y'));
				date.setElementValue('m', d2.getElementValue('m'));
				date.setElementValue('d', d2.getElementValue('d'));
				rec.setAttrValue(date.getCopy(), endDateCN);
			}
			if (time != null && startTimeCN >= 0 && endTimeCN >= 0) {
				time.setElementValue('h', d1.getElementValue('h'));
				time.setElementValue('t', d1.getElementValue('t'));
				time.setElementValue('s', d1.getElementValue('s'));
				rec.setAttrValue(time.getCopy(), startTimeCN);
				time.setElementValue('h', d2.getElementValue('h'));
				time.setElementValue('t', d2.getElementValue('t'));
				time.setElementValue('s', d2.getElementValue('s'));
				rec.setAttrValue(time.getCopy(), endTimeCN);
			}
			if (physicalTime) {
				d1.setPrecision(optPrecision);
				d2.setPrecision(optPrecision);
			}
			int dur = (int) d2.subtract(d1);
			if (physicalTime) {
				d1.setPrecision('s');
				d2.setPrecision('s');
			}
			rec.setNumericAttrValue(dur, String.valueOf(dur), durCN);
			float duration = dur;
			if (durMinsCN >= 0) {
				duration /= 60;
				rec.setNumericAttrValue(duration, String.valueOf(duration), durMinsCN);
			}
			if (durHoursCN >= 0) {
				duration /= 60;
				rec.setNumericAttrValue(duration, String.valueOf(duration), durHoursCN);
			}
			if (durDaysCN >= 0) {
				duration /= 24;
				rec.setNumericAttrValue(duration, String.valueOf(duration), durDaysCN);
			}
			if (yearCN >= 0) {
				int n = d1.getElementValue('y');
				rec.setNumericAttrValue(n, String.valueOf(n), yearCN);
				n = d2.getElementValue('y');
				rec.setNumericAttrValue(n, String.valueOf(n), yearCN + 1);
			}
			if (monthCN >= 0) {
				int n = d1.getElementValue('m');
				rec.setNumericAttrValue(n, String.valueOf(n), monthCN);
				n = d2.getElementValue('m');
				rec.setNumericAttrValue(n, String.valueOf(n), monthCN + 1);
			}
			if (dowCN >= 0 && (d1 instanceof spade.time.Date)) {
				int n = ((spade.time.Date) d1).getDayOfWeek();
				rec.setNumericAttrValue(n, String.valueOf(n), dowCN);
				n = ((spade.time.Date) d2).getDayOfWeek();
				rec.setNumericAttrValue(n, String.valueOf(n), dowCN + 1);
			}
			if (hourCN >= 0) {
				int n = d1.getElementValue('h');
				rec.setNumericAttrValue(n, String.valueOf(n), hourCN);
				n = d2.getElementValue('h');
				rec.setNumericAttrValue(n, String.valueOf(n), hourCN + 1);
			}
			TimeReference tref = new TimeReference();
			tref.setValidFrom(d1);
			tref.setValidUntil(d2);
			rec.setTimeReference(tref);
		}

		return dtTraj;
	}
}
