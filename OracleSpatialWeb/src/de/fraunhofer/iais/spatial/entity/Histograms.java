package de.fraunhofer.iais.spatial.entity;

import java.util.Map;

import com.google.common.collect.Maps;

import de.fraunhofer.iais.spatial.util.DateUtil;

/**
 * Entity which stores the statistical histograms' data
 * @author <a href="mailto:haolin.zhi@iais.fraunhofer.de">Haolin Zhi</A>
 * @author <a href="mailto:iulian.peca@iais.fraunhofer.de">Iulian Peca</a>
 *
 */
public class Histograms {
	private Map<Integer, Integer> years = Maps.newLinkedHashMap();
	private Map<Integer, Integer> months = Maps.newLinkedHashMap();
	private Map<Integer, Integer> days = Maps.newLinkedHashMap();
	private Map<Integer, Integer> hours = Maps.newLinkedHashMap();
	private Map<Integer, Integer> weekdays = Maps.newLinkedHashMap();

	public Histograms() {
		init();
	}

	private void init() {
		// init
		for (int year : DateUtil.allYearInts) {
			years.put(year, 0);
		}

		for (int month : DateUtil.allMonthInts) {
			months.put(month, 0);
		}

		for (int day : DateUtil.allDayInts) {
			days.put(day, 0);
		}

		for (int hour : DateUtil.allHourInts) {
			hours.put(hour, 0);
		}

		for (int weekday : DateUtil.allWeekdayInts) {
			weekdays.put(weekday, 0);
		}
	}

	public Map<Integer, Integer> getYears() {
		return years;
	}

	public Map<Integer, Integer> getMonths() {
		return months;
	}

	public Map<Integer, Integer> getDays() {
		return days;
	}

	public Map<Integer, Integer> getHours() {
		return hours;
	}

	public Map<Integer, Integer> getWeekdays() {
		return weekdays;
	}
}