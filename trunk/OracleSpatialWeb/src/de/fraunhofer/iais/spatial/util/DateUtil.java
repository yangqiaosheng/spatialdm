package de.fraunhofer.iais.spatial.util;

import java.util.Calendar;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class DateUtil {

	public static Calendar today = Calendar.getInstance();
	public static SortedSet<String> allYearStrs = new TreeSet<String>();
	public static SortedSet<String> allMonthStrs = new TreeSet<String>();
	public static SortedSet<String> allDayStrs = new TreeSet<String>();
	public static SortedSet<String> allHourStrs = new TreeSet<String>();

	public static SortedSet<Integer> allYearInts = new TreeSet<Integer>();
	public static SortedSet<Integer> allMonthInts = new TreeSet<Integer>();
	public static SortedSet<Integer> allDayInts = new TreeSet<Integer>();
	public static SortedSet<Integer> allHourInts = new TreeSet<Integer>();

	static {
		allYearStrs(allYearStrs);
		allMonthStrs(allMonthStrs);
		allDayStrs(allDayStrs);
		allHourStrs(allHourStrs);

		allYearInts(allYearInts);
		allMonthInts(allMonthInts);
		allDayInts(allDayInts);
		allHourInts(allHourInts);
	}


	public static void allYearStrs(Set<String> years) {
		for (int i = 2005; i <= today.get(Calendar.YEAR); i++) {
			years.add(String.format("%04d", i));
		}
	}

	public static void allMonthStrs(Set<String> months) {
		for (int i = 1; i <= 12; i++) {
			months.add(String.format("%02d", i));
		}
	}

	public static void allDayStrs(Set<String> days) {
		for (int i = 1; i <= 31; i++) {
			days.add(String.format("%02d", i));
		}
	}

	public static void allHourStrs(Set<String> hours) {
		for (int i = 0; i <= 23; i++) {
			hours.add(String.format("%02d", i));
		}
	}

	public static void allYearInts(Set<Integer> years) {
		for (int i = 2005; i <= today.get(Calendar.YEAR); i++) {
			years.add(i);
		}
	}

	public static void allMonthInts(Set<Integer> months) {
		for (int i = 1; i <= 12; i++) {
			months.add(i);
		}
	}

	public static void allDayInts(Set<Integer> days) {
		for (int i = 1; i <= 31; i++) {
			days.add(i);
		}
	}

	public static void allHourInts(Set<Integer> hours) {
		for (int i = 0; i <= 23; i++) {
			hours.add(i);
		}
	}

}
