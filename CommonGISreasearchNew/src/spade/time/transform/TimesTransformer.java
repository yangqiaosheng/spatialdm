package spade.time.transform;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Vector;

import spade.time.Date;
import spade.time.TimeCount;
import spade.time.TimeMoment;
import spade.time.TimeReference;
import spade.vis.database.DataItem;
import spade.vis.database.ObjectContainer;
import spade.vis.database.SpatialDataItem;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Jul 25, 2008
 * Time: 3:38:42 PM
 * Transforms time references in all containers with time referenced objects
 * from absolute times to various variants of relative times.
 */
public class TimesTransformer {
	/**
	 * Restores the time references in the given object container to the original
	 * states.
	 */
	public static void restoreOriginalTimes(ObjectContainer oCont) {
		if (oCont == null)
			return;
		int nObj = oCont.getObjectCount();
		if (nObj < 1)
			return;
		for (int i = 0; i < nObj; i++) {
			DataItem data = oCont.getObjectData(i);
			if (data == null) {
				continue;
			}
			TimeReference tref = data.getTimeReference();
			if (tref == null) {
				continue;
			}
			tref.restoreOriginalTimes();
			if (data instanceof SpatialDataItem) {
				Vector states = ((SpatialDataItem) data).getStates();
				if (states == null) {
					continue;
				}
				for (int j = 0; j < states.size(); j++) {
					SpatialDataItem state = (SpatialDataItem) states.elementAt(j);
					tref = state.getTimeReference();
					if (tref == null) {
						continue;
					}
					tref.restoreOriginalTimes();
				}
			}
		}
		oCont.timesHaveBeenTransformed();
	}

	/**
	 * Transforms all time references in the given object container to relative
	 * times in relation to the daily cycle.
	 * Returns the new minimum and maximum times in the container or null if
	 * the transformation could not be done.
	 */
	public static TimeReference transformTimesToDayCycle(ObjectContainer oCont) {
		if (oCont == null)
			return null;
		int nObj = oCont.getObjectCount();
		if (nObj < 1)
			return null;
		TimeMoment minTime = null, maxTime = null;
		String scheme = null;
		boolean schemeIncludesDay = false;
		char precision = 'h';
		for (int i = 0; i < nObj; i++) {
			DataItem data = oCont.getObjectData(i);
			if (data == null) {
				continue;
			}
			TimeReference tref = data.getTimeReference();
			if (tref == null) {
				continue;
			}
			TimeMoment t = tref.getOrigFrom();
			if (t == null) {
				continue;
			}
			if (!(t instanceof Date)) {
				continue;
			}
			Date d0 = (Date) t;
			if (!d0.hasElement('h')) {
				continue;
			}
			if (scheme == null) {
				scheme = "hh";
				if (d0.hasElement('t')) {
					scheme += ":tt";
					precision = 't';
				}
				if (d0.hasElement('s')) {
					scheme += ":ss";
					precision = 's';
				}
			}
			Date dTrans0 = new Date(), dTrans1 = null;
			dTrans0.scheme = scheme;
			dTrans0.setElementValue('d', 1);
			dTrans0.setElementValue('h', d0.getElementValue('h'));
			if (d0.hasElement('t')) {
				dTrans0.setElementValue('t', d0.getElementValue('t'));
			}
			if (d0.hasElement('s')) {
				dTrans0.setElementValue('s', d0.getElementValue('s'));
			}
			dTrans0.setPrecision(precision);
			if (tref.getOrigUntil() == null) {
				tref.setValidUntil(d0);
				dTrans1 = dTrans0;
			} else {
				Date d1 = (Date) tref.getOrigUntil();
				int diff = (int) d1.subtract(d0, 'd');
				dTrans1 = new Date();
				dTrans1.scheme = scheme;
				dTrans1.setElementValue('d', diff + 1);
				dTrans1.setElementValue('h', d1.getElementValue('h'));
				if (d1.hasElement('t')) {
					dTrans1.setElementValue('t', d1.getElementValue('t'));
				}
				if (d1.hasElement('s')) {
					dTrans1.setElementValue('s', d1.getElementValue('s'));
				}
				dTrans1.setPrecision(precision);
				if (diff > 0 && !schemeIncludesDay) {
					scheme = scheme + " (dd)";
					schemeIncludesDay = true;
				}
			}
			tref.setTransformedTimes(dTrans0, dTrans1);
			if (minTime == null || minTime.compareTo(dTrans0) > 0) {
				minTime = dTrans0;
			}
			if (maxTime == null || maxTime.compareTo(dTrans1) < 0) {
				maxTime = dTrans1;
			}
			if (data instanceof SpatialDataItem) {
				Vector states = ((SpatialDataItem) data).getStates();
				if (states == null) {
					continue;
				}
				for (int j = 0; j < states.size(); j++) {
					SpatialDataItem state = (SpatialDataItem) states.elementAt(j);
					tref = state.getTimeReference();
					if (tref == null) {
						continue;
					}
					t = tref.getOrigFrom();
					if (t == null) {
						continue;
					}
					if (!(t instanceof Date)) {
						continue;
					}
					Date d = (Date) t;
					if (!d.hasElement('h')) {
						continue;
					}
					int diff = (int) d.subtract(d0, 'd');
					dTrans0 = new Date();
					dTrans0.scheme = scheme;
					dTrans0.setElementValue('d', diff + 1);
					dTrans0.setElementValue('h', d.getElementValue('h'));
					if (d.hasElement('t')) {
						dTrans0.setElementValue('t', d.getElementValue('t'));
					}
					if (d.hasElement('s')) {
						dTrans0.setElementValue('s', d.getElementValue('s'));
					}
					dTrans0.setPrecision(precision);
					if (tref.getOrigUntil() == null) {
						tref.setValidUntil(d);
						dTrans1 = dTrans0;
					} else {
						d = (Date) tref.getOrigUntil();
						diff = (int) d.subtract(d0, 'd');
						dTrans1 = new Date();
						dTrans1.scheme = scheme;
						dTrans1.setElementValue('d', diff + 1);
						dTrans1.setElementValue('h', d.getElementValue('h'));
						if (d.hasElement('t')) {
							dTrans1.setElementValue('t', d.getElementValue('t'));
						}
						if (d.hasElement('s')) {
							dTrans1.setElementValue('s', d.getElementValue('s'));
						}
						dTrans1.setPrecision(precision);
					}
					tref.setTransformedTimes(dTrans0, dTrans1);
				}
			}
		}
		if (minTime == null || maxTime == null)
			return null;
		oCont.timesHaveBeenTransformed();
		TimeReference tref = new TimeReference();
		tref.setValidFrom(minTime);
		tref.setValidUntil(maxTime);
		return tref;
	}

	/**
	 * Transforms all time references in the given object container to relative
	 * times in relation to the weekly cycle.
	 * Returns the new minimum and maximum times in the container or null if
	 * the transformation could not be done.
	 */
	public static TimeReference transformTimesToWeekCycle(ObjectContainer oCont) {
		if (oCont == null)
			return null;
		int nObj = oCont.getObjectCount();
		if (nObj < 1)
			return null;
		long nDays = 0;
		Date someDate = null;
		for (int i = 0; i < nObj; i++) {
			DataItem data = oCont.getObjectData(i);
			if (data == null) {
				continue;
			}
			TimeReference tref = data.getTimeReference();
			if (tref == null) {
				continue;
			}
			TimeMoment t0 = tref.getOrigFrom(), t1 = tref.getOrigUntil();
			if (t0 == null || t1 == null) {
				continue;
			}
			if (!(t0 instanceof Date)) {
				continue;
			}
			Date d0 = (Date) t0, d1 = (Date) t1;
			long n = d1.subtract(d0, 'd');
			if (n > nDays) {
				nDays = n;
			}
			if (someDate == null) {
				someDate = d0;
			}
		}
		if (someDate == null)
			return null;
		String scheme = "d";
		while (nDays > 9) {
			scheme += "d";
			nDays /= 10;
		}
		char precision = 'd';
		if (someDate.hasElement('h')) {
			scheme += ";hh";
			precision = 'h';
		}
		if (someDate.hasElement('t')) {
			scheme += ":tt";
			precision = 't';
		}
		if (someDate.hasElement('s')) {
			scheme += ":ss";
			precision = 's';
		}

		TimeMoment minTime = null, maxTime = null;
		GregorianCalendar calendar = new GregorianCalendar();
		for (int i = 0; i < nObj; i++) {
			DataItem data = oCont.getObjectData(i);
			if (data == null) {
				continue;
			}
			TimeReference tref = data.getTimeReference();
			if (tref == null) {
				continue;
			}
			TimeMoment t = tref.getOrigFrom();
			if (t == null) {
				continue;
			}
			if (!(t instanceof Date)) {
				continue;
			}
			Date d0 = (Date) t;
			if (!d0.hasElement('d')) {
				continue;
			}
			Date dTrans0 = new Date(), dTrans1 = null;
			dTrans0.scheme = scheme;
			calendar.set(d0.getElementValue('y'), d0.getElementValue('m') - 1, d0.getElementValue('d'));
			int dow0 = calendar.get(Calendar.DAY_OF_WEEK) - 1;
			if (dow0 == 0) {
				dow0 = 7;
			}
			dTrans0.setElementValue('d', dow0);
			if (d0.hasElement('h')) {
				dTrans0.setElementValue('h', d0.getElementValue('h'));
			}
			if (d0.hasElement('t')) {
				dTrans0.setElementValue('t', d0.getElementValue('t'));
			}
			if (d0.hasElement('s')) {
				dTrans0.setElementValue('s', d0.getElementValue('s'));
			}
			dTrans0.setPrecision(precision);
			if (tref.getOrigUntil() == null) {
				tref.setValidUntil(d0);
				dTrans1 = dTrans0;
			} else {
				Date d1 = (Date) tref.getOrigUntil();
				int diff = (int) d1.subtract(d0, 'd');
				dTrans1 = new Date();
				dTrans1.scheme = scheme;
				dTrans1.setElementValue('d', diff + dow0);
				if (d1.hasElement('h')) {
					dTrans1.setElementValue('h', d1.getElementValue('h'));
				}
				if (d1.hasElement('t')) {
					dTrans1.setElementValue('t', d1.getElementValue('t'));
				}
				if (d1.hasElement('s')) {
					dTrans1.setElementValue('s', d1.getElementValue('s'));
				}
				dTrans1.setPrecision(precision);
			}
			tref.setTransformedTimes(dTrans0, dTrans1);
			if (minTime == null || minTime.compareTo(dTrans0) > 0) {
				minTime = dTrans0;
			}
			if (maxTime == null || maxTime.compareTo(dTrans1) < 0) {
				maxTime = dTrans1;
			}
			if (data instanceof SpatialDataItem) {
				Vector states = ((SpatialDataItem) data).getStates();
				if (states == null) {
					continue;
				}
				for (int j = 0; j < states.size(); j++) {
					SpatialDataItem state = (SpatialDataItem) states.elementAt(j);
					tref = state.getTimeReference();
					if (tref == null) {
						continue;
					}
					t = tref.getOrigFrom();
					if (t == null) {
						continue;
					}
					if (!(t instanceof Date)) {
						continue;
					}
					Date d = (Date) t;
					if (!d.hasElement('d')) {
						continue;
					}
					int diff = (int) d.subtract(d0, 'd');
					dTrans0 = new Date();
					dTrans0.scheme = scheme;
					dTrans0.setElementValue('d', diff + dow0);
					if (d.hasElement('h')) {
						dTrans0.setElementValue('h', d.getElementValue('h'));
					}
					if (d.hasElement('t')) {
						dTrans0.setElementValue('t', d.getElementValue('t'));
					}
					if (d.hasElement('s')) {
						dTrans0.setElementValue('s', d.getElementValue('s'));
					}
					dTrans0.setPrecision(precision);
					if (tref.getOrigUntil() == null) {
						tref.setValidUntil(d);
						dTrans1 = dTrans0;
					} else {
						d = (Date) tref.getOrigUntil();
						diff = (int) d.subtract(d0, 'd');
						dTrans1 = new Date();
						dTrans1.scheme = scheme;
						dTrans1.setElementValue('d', diff + dow0);
						if (d.hasElement('h')) {
							dTrans1.setElementValue('h', d.getElementValue('h'));
						}
						if (d.hasElement('t')) {
							dTrans1.setElementValue('t', d.getElementValue('t'));
						}
						if (d.hasElement('s')) {
							dTrans1.setElementValue('s', d.getElementValue('s'));
						}
						dTrans1.setPrecision(precision);
					}
					tref.setTransformedTimes(dTrans0, dTrans1);
				}
			}
		}
		if (minTime == null || maxTime == null)
			return null;
		oCont.timesHaveBeenTransformed();
		TimeReference tref = new TimeReference();
		tref.setValidFrom(minTime);
		tref.setValidUntil(maxTime);
		return tref;
	}

	/**
	 * Transforms all time references in the given object container to relative
	 * times in relation to the days of month.
	 * Returns the new minimum and maximum times in the container or null if
	 * the transformation could not be done.
	 */
	public static TimeReference transformTimesToDaysOfMonth(ObjectContainer oCont) {
		if (oCont == null)
			return null;
		int nObj = oCont.getObjectCount();
		if (nObj < 1)
			return null;
		long nDays = 0;
		Date someDate = null;
		for (int i = 0; i < nObj; i++) {
			DataItem data = oCont.getObjectData(i);
			if (data == null) {
				continue;
			}
			TimeReference tref = data.getTimeReference();
			if (tref == null) {
				continue;
			}
			TimeMoment t0 = tref.getOrigFrom(), t1 = tref.getOrigUntil();
			if (t0 == null || t1 == null) {
				continue;
			}
			if (!(t0 instanceof Date)) {
				continue;
			}
			Date d0 = (Date) t0, d1 = (Date) t1;
			long n = d1.subtract(d0, 'd');
			if (n > nDays) {
				nDays = n;
			}
			if (someDate == null) {
				someDate = d0;
			}
		}
		if (someDate == null)
			return null;
		String scheme = "dd";
		while (nDays > 99) {
			scheme += "d";
			nDays /= 10;
		}
		char precision = 'd';
		if (someDate.hasElement('h')) {
			scheme += ";hh";
			precision = 'h';
		}
		if (someDate.hasElement('t')) {
			scheme += ":tt";
			precision = 't';
		}
		if (someDate.hasElement('s')) {
			scheme += ":ss";
			precision = 's';
		}

		TimeMoment minTime = null, maxTime = null;
		for (int i = 0; i < nObj; i++) {
			DataItem data = oCont.getObjectData(i);
			if (data == null) {
				continue;
			}
			TimeReference tref = data.getTimeReference();
			if (tref == null) {
				continue;
			}
			TimeMoment t = tref.getOrigFrom();
			if (t == null) {
				continue;
			}
			if (!(t instanceof Date)) {
				continue;
			}
			Date d0 = (Date) t;
			if (!d0.hasElement('d')) {
				continue;
			}
			Date dTrans0 = new Date(), dTrans1 = null;
			dTrans0.scheme = scheme;
			int day0 = d0.getElementValue('d');
			dTrans0.setElementValue('d', day0);
			if (d0.hasElement('h')) {
				dTrans0.setElementValue('h', d0.getElementValue('h'));
			}
			if (d0.hasElement('t')) {
				dTrans0.setElementValue('t', d0.getElementValue('t'));
			}
			if (d0.hasElement('s')) {
				dTrans0.setElementValue('s', d0.getElementValue('s'));
			}
			dTrans0.setPrecision(precision);
			if (tref.getOrigUntil() == null) {
				tref.setValidUntil(d0);
				dTrans1 = dTrans0;
			} else {
				Date d1 = (Date) tref.getOrigUntil();
				int diff = (int) d1.subtract(d0, 'd');
				dTrans1 = new Date();
				dTrans1.scheme = scheme;
				dTrans1.setElementValue('d', diff + day0);
				if (d1.hasElement('h')) {
					dTrans1.setElementValue('h', d1.getElementValue('h'));
				}
				if (d1.hasElement('t')) {
					dTrans1.setElementValue('t', d1.getElementValue('t'));
				}
				if (d1.hasElement('s')) {
					dTrans1.setElementValue('s', d1.getElementValue('s'));
				}
				dTrans1.setPrecision(precision);
			}
			tref.setTransformedTimes(dTrans0, dTrans1);
			if (minTime == null || minTime.compareTo(dTrans0) > 0) {
				minTime = dTrans0;
			}
			if (maxTime == null || maxTime.compareTo(dTrans1) < 0) {
				maxTime = dTrans1;
			}
			if (data instanceof SpatialDataItem) {
				Vector states = ((SpatialDataItem) data).getStates();
				if (states == null) {
					continue;
				}
				for (int j = 0; j < states.size(); j++) {
					SpatialDataItem state = (SpatialDataItem) states.elementAt(j);
					tref = state.getTimeReference();
					if (tref == null) {
						continue;
					}
					t = tref.getOrigFrom();
					if (t == null) {
						continue;
					}
					if (!(t instanceof Date)) {
						continue;
					}
					Date d = (Date) t;
					if (!d.hasElement('d')) {
						continue;
					}
					int diff = (int) d.subtract(d0, 'd');
					dTrans0 = new Date();
					dTrans0.scheme = scheme;
					dTrans0.setElementValue('d', diff + day0);
					if (d.hasElement('h')) {
						dTrans0.setElementValue('h', d.getElementValue('h'));
					}
					if (d.hasElement('t')) {
						dTrans0.setElementValue('t', d.getElementValue('t'));
					}
					if (d.hasElement('s')) {
						dTrans0.setElementValue('s', d.getElementValue('s'));
					}
					dTrans0.setPrecision(precision);
					if (tref.getOrigUntil() == null) {
						tref.setValidUntil(d);
						dTrans1 = dTrans0;
					} else {
						d = (Date) tref.getOrigUntil();
						diff = (int) d.subtract(d0, 'd');
						dTrans1 = new Date();
						dTrans1.scheme = scheme;
						dTrans1.setElementValue('d', diff + day0);
						if (d.hasElement('h')) {
							dTrans1.setElementValue('h', d.getElementValue('h'));
						}
						if (d.hasElement('t')) {
							dTrans1.setElementValue('t', d.getElementValue('t'));
						}
						if (d.hasElement('s')) {
							dTrans1.setElementValue('s', d.getElementValue('s'));
						}
						dTrans1.setPrecision(precision);
					}
					tref.setTransformedTimes(dTrans0, dTrans1);
				}
			}
		}
		if (minTime == null || maxTime == null)
			return null;
		oCont.timesHaveBeenTransformed();
		TimeReference tref = new TimeReference();
		tref.setValidFrom(minTime);
		tref.setValidUntil(maxTime);
		return tref;
	}

	/**
	 * Transforms all time references in the given object container to relative
	 * times in relation to the yearly or seasonal cycle.
	 * Returns the new minimum and maximum times in the container or null if
	 * the transformation could not be done.
	 */
	public static TimeReference transformTimesToSeasonalCycle(ObjectContainer oCont, Date seasonBegin) {
		if (oCont == null)
			return null;
		int nObj = oCont.getObjectCount();
		if (nObj < 1)
			return null;
		long nDays = 0;
		Date someDate = null;
		int sbYearOrig = seasonBegin.getElementValue('y');
		for (int i = 0; i < nObj; i++) {
			DataItem data = oCont.getObjectData(i);
			if (data == null) {
				continue;
			}
			TimeReference tref = data.getTimeReference();
			if (tref == null) {
				continue;
			}
			TimeMoment t0 = tref.getOrigFrom(), t1 = tref.getOrigUntil();
			if (t0 == null || t1 == null) {
				continue;
			}
			if (!(t0 instanceof Date)) {
				continue;
			}
			Date d0 = (Date) t0, d1 = (Date) t1;
			seasonBegin.setElementValue('y', d0.getElementValue('y'));
			long n = d1.subtract(seasonBegin, 'd');
			if (n > nDays) {
				nDays = n;
			}
			if (someDate == null) {
				someDate = d0;
			}
		}
		if (someDate == null)
			return null;
		String scheme = "ddd";
		while (nDays > 999) {
			scheme += "d";
			nDays /= 10;
		}
		char precision = 'd';
		if (someDate.hasElement('h')) {
			scheme += ";hh";
			precision = 'h';
		}
		if (someDate.hasElement('t')) {
			scheme += ":tt";
			precision = 't';
		}
		if (someDate.hasElement('s')) {
			scheme += ":ss";
			precision = 's';
		}

		TimeMoment minTime = null, maxTime = null;
		for (int i = 0; i < nObj; i++) {
			DataItem data = oCont.getObjectData(i);
			if (data == null) {
				continue;
			}
			TimeReference tref = data.getTimeReference();
			if (tref == null) {
				continue;
			}
			TimeMoment t = tref.getOrigFrom();
			if (t == null) {
				continue;
			}
			if (!(t instanceof Date)) {
				continue;
			}
			Date d0 = (Date) t;
			if (!d0.hasElement('d') || !d0.hasElement('m') || !d0.hasElement('y')) {
				continue;
			}
			seasonBegin.setElementValue('y', d0.getElementValue('y'));
			Date dTrans0 = new Date(), dTrans1 = null;
			dTrans0.scheme = scheme;
			int dayN = (int) d0.subtract(seasonBegin, 'd') + 1;
			dTrans0.setElementValue('d', dayN);
			if (d0.hasElement('h')) {
				dTrans0.setElementValue('h', d0.getElementValue('h'));
			}
			if (d0.hasElement('t')) {
				dTrans0.setElementValue('t', d0.getElementValue('t'));
			}
			if (d0.hasElement('s')) {
				dTrans0.setElementValue('s', d0.getElementValue('s'));
			}
			dTrans0.setPrecision(precision);
			if (tref.getOrigUntil() == null) {
				tref.setValidUntil(d0);
				dTrans1 = dTrans0;
			} else {
				Date d1 = (Date) tref.getOrigUntil();
				dTrans1 = new Date();
				dTrans1.scheme = scheme;
				dayN = (int) d1.subtract(seasonBegin, 'd') + 1;
				dTrans1.setElementValue('d', dayN);
				if (d1.hasElement('h')) {
					dTrans1.setElementValue('h', d1.getElementValue('h'));
				}
				if (d1.hasElement('t')) {
					dTrans1.setElementValue('t', d1.getElementValue('t'));
				}
				if (d1.hasElement('s')) {
					dTrans1.setElementValue('s', d1.getElementValue('s'));
				}
				dTrans1.setPrecision(precision);
			}
			tref.setTransformedTimes(dTrans0, dTrans1);
			if (minTime == null || minTime.compareTo(dTrans0) > 0) {
				minTime = dTrans0;
			}
			if (maxTime == null || maxTime.compareTo(dTrans1) < 0) {
				maxTime = dTrans1;
			}
			if (data instanceof SpatialDataItem) {
				Vector states = ((SpatialDataItem) data).getStates();
				if (states == null) {
					continue;
				}
				for (int j = 0; j < states.size(); j++) {
					SpatialDataItem state = (SpatialDataItem) states.elementAt(j);
					tref = state.getTimeReference();
					if (tref == null) {
						continue;
					}
					t = tref.getOrigFrom();
					if (t == null) {
						continue;
					}
					if (!(t instanceof Date)) {
						continue;
					}
					Date d = (Date) t;
					if (!d.hasElement('d')) {
						continue;
					}
					dTrans0 = new Date();
					dTrans0.scheme = scheme;
					dayN = (int) d.subtract(seasonBegin, 'd') + 1;
					dTrans0.setElementValue('d', dayN);
					if (d.hasElement('h')) {
						dTrans0.setElementValue('h', d.getElementValue('h'));
					}
					if (d.hasElement('t')) {
						dTrans0.setElementValue('t', d.getElementValue('t'));
					}
					if (d.hasElement('s')) {
						dTrans0.setElementValue('s', d.getElementValue('s'));
					}
					dTrans0.setPrecision(precision);
					if (tref.getOrigUntil() == null) {
						tref.setValidUntil(d);
						dTrans1 = dTrans0;
					} else {
						d = (Date) tref.getOrigUntil();
						dTrans1 = new Date();
						dTrans1.scheme = scheme;
						dayN = (int) d.subtract(seasonBegin, 'd') + 1;
						dTrans1.setElementValue('d', dayN);
						if (d.hasElement('h')) {
							dTrans1.setElementValue('h', d.getElementValue('h'));
						}
						if (d.hasElement('t')) {
							dTrans1.setElementValue('t', d.getElementValue('t'));
						}
						if (d.hasElement('s')) {
							dTrans1.setElementValue('s', d.getElementValue('s'));
						}
						dTrans1.setPrecision(precision);
					}
					tref.setTransformedTimes(dTrans0, dTrans1);
				}
			}
		}
		seasonBegin.setElementValue('y', sbYearOrig);
		if (minTime == null || maxTime == null)
			return null;
		oCont.timesHaveBeenTransformed();
		TimeReference tref = new TimeReference();
		tref.setValidFrom(minTime);
		tref.setValidUntil(maxTime);
		return tref;
	}

	/**
	 * Transforms all time references in the given object container to relative
	 * times in relation to the starting times of the existence of the objects.
	 * Returns the new minimum and maximum times in the container or null if
	 * the transformation could not be done.
	 */
	public static TimeReference transformTimesRelativeToStart(ObjectContainer oCont) {
		if (oCont == null)
			return null;
		int nObj = oCont.getObjectCount();
		if (nObj < 1)
			return null;
		TimeReference wholeTime = oCont.getOriginalTimeSpan();
		TimeMoment minTime = wholeTime.getValidFrom().getCopy(), maxTime = null;
		char pMax = minTime.getMinPrecision();
		minTime.setPrecision(pMax);
		minTime.reset();
		long minTimeNum = minTime.toNumber();
		for (int i = 0; i < nObj; i++) {
			DataItem data = oCont.getObjectData(i);
			if (data == null) {
				continue;
			}
			TimeReference tref = data.getTimeReference();
			if (tref == null) {
				continue;
			}
			TimeMoment t0 = tref.getOrigFrom();
			if (t0 == null) {
				continue;
			}
			TimeMoment t2 = tref.getOrigUntil();
			if (t2 == null) {
				t2 = t0;
			}
			long diff = t2.subtract(t0, pMax);
			tref.setTransformedTimes(minTime.getCopy(), minTime.valueOf(minTimeNum + diff));
			t2 = tref.getValidUntil();
			if (maxTime == null || maxTime.compareTo(t2) < 0) {
				maxTime = t2;
			}
			if (data instanceof SpatialDataItem) {
				Vector states = ((SpatialDataItem) data).getStates();
				if (states == null) {
					continue;
				}
				for (int j = 0; j < states.size(); j++) {
					SpatialDataItem state = (SpatialDataItem) states.elementAt(j);
					tref = state.getTimeReference();
					if (tref == null) {
						continue;
					}
					TimeMoment t1 = tref.getOrigFrom();
					if (t1 == null) {
						continue;
					}
					t2 = tref.getOrigUntil();
					if (t2 == null) {
						t2 = t1;
					}
					long diff1 = t1.subtract(t0, pMax), diff2 = t2.subtract(t0, pMax);
					tref.setTransformedTimes(minTime.valueOf(minTimeNum + diff1), minTime.valueOf(minTimeNum + diff2));
				}
			}
		}
		if (minTime == null || maxTime == null)
			return null;
		oCont.timesHaveBeenTransformed();
		TimeReference tref = new TimeReference();
		tref.setValidFrom(minTime);
		tref.setValidUntil(maxTime);
		return tref;
	}

	/**
	 * Transforms all time references in the given object container to relative
	 * times in relation to the starting times of the existence of the objects.
	 * Returns the new minimum and maximum times in the container or null if
	 * the transformation could not be done.
	 */
	public static TimeReference transformTimesRelativeToEnd(ObjectContainer oCont, long maxLen) {
		if (oCont == null || maxLen < 1)
			return null;
		int nObj = oCont.getObjectCount();
		if (nObj < 1)
			return null;
		TimeReference wholeTime = oCont.getOriginalTimeSpan();
		TimeMoment minTime = null, maxTime = wholeTime.getValidUntil().getCopy();
		char pMax = maxTime.getMinPrecision();
		maxTime.setPrecision(pMax);
		maxTime.reset();
		if (pMax == 'd' || pMax == 'm') {
			maxTime.add(-1);
		}
		long maxTimeNum = maxTime.toNumber();
		for (int i = 0; i < nObj; i++) {
			DataItem data = oCont.getObjectData(i);
			if (data == null) {
				continue;
			}
			TimeReference tref = data.getTimeReference();
			if (tref == null) {
				continue;
			}
			TimeMoment t0 = tref.getOrigFrom();
			if (t0 == null) {
				continue;
			}
			TimeMoment tEnd = tref.getOrigUntil();
			if (tEnd == null) {
				tEnd = t0;
			}
			long diff = tEnd.subtract(t0, pMax);
			tref.setTransformedTimes(maxTime.valueOf(maxTimeNum - diff), maxTime.getCopy());
			TimeMoment t1 = tref.getValidFrom();
			if (minTime == null || minTime.compareTo(t1) > 0) {
				minTime = t1;
			}
			if (data instanceof SpatialDataItem) {
				Vector states = ((SpatialDataItem) data).getStates();
				if (states == null) {
					continue;
				}
				for (int j = 0; j < states.size(); j++) {
					SpatialDataItem state = (SpatialDataItem) states.elementAt(j);
					tref = state.getTimeReference();
					if (tref == null) {
						continue;
					}
					t1 = tref.getOrigFrom();
					if (t1 == null) {
						continue;
					}
					TimeMoment t2 = tref.getOrigUntil();
					if (t2 == null) {
						t2 = t1;
					}
					long diff1 = tEnd.subtract(t1, pMax), diff2 = tEnd.subtract(t2, pMax);
					tref.setTransformedTimes(maxTime.valueOf(maxTimeNum - diff1), maxTime.valueOf(maxTimeNum - diff2));
				}
			}
		}
		if (minTime == null || maxTime == null)
			return null;
		oCont.timesHaveBeenTransformed();
		TimeReference tref = new TimeReference();
		tref.setValidFrom(minTime);
		tref.setValidUntil(maxTime);
		return tref;
	}

	/**
	 * Transforms all time references in the given object container to relative
	 * times in relation to the starting and ending times of the existence of the objects.
	 * Returns the new minimum and maximum times in the container or null if
	 * the transformation could not be done.
	 */
	public static TimeReference transformTimesRelativeToStartAndEnd(ObjectContainer oCont) {
		if (oCont == null)
			return null;
		int nObj = oCont.getObjectCount();
		if (nObj < 1)
			return null;
		TimeMoment minTime = null, maxTime = null;
		for (int i = 0; i < nObj; i++) {
			DataItem data = oCont.getObjectData(i);
			if (data == null) {
				continue;
			}
			TimeReference tref = data.getTimeReference();
			if (tref == null) {
				continue;
			}
			TimeMoment t0 = tref.getValidFrom(), t2 = tref.getValidUntil();
			TimeCount c0 = new TimeCount(0), c2 = new TimeCount(1000);
			if (t0 == null || t2 == null) {
				tref.setTransformedTimes(c0, c0.getCopy());
				continue;
			}
			long diff = t2.subtract(t0);
			tref.setTransformedTimes(c0, c2);
			if (minTime == null) {
				minTime = c0;
			}
			if (maxTime == null) {
				maxTime = c2;
			}
			if (data instanceof SpatialDataItem) {
				Vector states = ((SpatialDataItem) data).getStates();
				if (states == null) {
					continue;
				}
				for (int j = 0; j < states.size(); j++) {
					SpatialDataItem state = (SpatialDataItem) states.elementAt(j);
					tref = state.getTimeReference();
					if (tref == null) {
						continue;
					}
					TimeMoment t1 = tref.getValidFrom();
					if (t1 == null) {
						continue;
					}
					t2 = tref.getValidUntil();
					if (t2 == null) {
						t2 = t1;
					}
					TimeCount c1 = new TimeCount(Math.round(1000.0 * t1.subtract(t0) / diff));
					c2 = new TimeCount(Math.round(1000.0 * t2.subtract(t0) / diff));
					tref.setTransformedTimes(c1, c2);
				}
			}
		}
		if (minTime == null || maxTime == null)
			return null;
		oCont.timesHaveBeenTransformed();
		TimeReference tref = new TimeReference();
		tref.setValidFrom(minTime);
		tref.setValidUntil(maxTime);
		return tref;
	}
}
