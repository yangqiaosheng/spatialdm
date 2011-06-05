package de.fraunhofer.iais.spatial.entity;

import java.util.Map;
import java.util.TreeMap;

import de.fraunhofer.iais.spatial.util.DateUtil;

public class Histrograms{
	private Map<Integer, Integer> years = new TreeMap<Integer, Integer>();
	private Map<Integer, Integer> months = new TreeMap<Integer, Integer>();
	private Map<Integer, Integer> days = new TreeMap<Integer, Integer>();
	private Map<Integer, Integer> hours = new TreeMap<Integer, Integer>();
	private Map<Integer, Integer> weekdays = new TreeMap<Integer, Integer>();

	public Histrograms(){
		init();
	}

	private void init(){
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