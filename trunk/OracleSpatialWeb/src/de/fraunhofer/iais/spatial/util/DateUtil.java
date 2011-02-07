package de.fraunhofer.iais.spatial.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang.time.DateUtils;

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
	public static SortedSet<Integer> allWeekdayInts = new TreeSet<Integer>();

	public static SortedSet<Date> allYearDates = new TreeSet<Date>();
	public static SortedSet<Date> allMonthDates = new TreeSet<Date>();
	public static SortedSet<Date> allDayDates = new TreeSet<Date>();
	public static SortedSet<Date> allHourDates = new TreeSet<Date>();
	public static SortedSet<Date> allWeekdayDates = new TreeSet<Date>();

	static {
		allYearStrs(allYearStrs);
		allMonthStrs(allMonthStrs);
		allDayStrs(allDayStrs);
		allHourStrs(allHourStrs);

		allYearInts(allYearInts);
		allMonthInts(allMonthInts);
		allDayInts(allDayInts);
		allHourInts(allHourInts);
		allWeekdayInts(allWeekdayInts);

		allYearDates(allYearDates);
		allMonthDates(allMonthDates);
		allDayDates(allDayDates);
		allHourDates(allHourDates);
		allWeekdayDates(allWeekdayDates);
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

	public static void allWeekdayInts(Set<Integer> weekdays) {
		for (int i = 1; i <= 7; i++) {
			weekdays.add(i);
		}
	}

	/**
	 * create a reference calendar at the date: Mon. 2006-01-01 00:00:00.000
	 * @return instance of Calendar
	 */
	public static Calendar createReferenceCalendar(){
		Calendar calendar = Calendar.getInstance();
		calendar.setLenient(false);
		calendar.set(2006, 0, 1, 0, 0, 0);
		DateUtils.ceiling(calendar, Calendar.SECOND);
		return calendar;
	}

	public static void allYearDates(Set<Date> years) {
		for (int i = 2005; i <= today.get(Calendar.YEAR); i++) {
			years.add(createYear(i));
		}
	}

	public static void allMonthDates(Set<Date> months) {
		for (int i = 1; i <= 12; i++) {
			months.add(createMonth(i));
		}
	}

	public static void allDayDates(Set<Date> days) {
		for (int i = 1; i <= 31; i++) {
			days.add(createDay(i));
		}
	}

	public static void allHourDates(Set<Date> hours) {
		for (int i = 0; i <= 23; i++) {
			hours.add(createHour(i));
		}
	}

	public static void allWeekdayDates(Set<Date> weekdays) {
		for (int i = 1; i <= 7; i++) {
			weekdays.add(createWeekday(i));
		}
	}

	/**
	 * create a Date Object at the reference calendar by a given year value
	 * @param year eg. 2009
	 * @return
	 */
	public static Date createYear(int year){
		Calendar calendar = createReferenceCalendar();
		calendar.set(Calendar.YEAR, year);
		return calendar.getTime();
	}


	/**
	 * create a Date Object at the reference calendar by a given month value
	 * @param month 1 - 12
	 * @return
	 */
	public static Date createMonth(int month){
		Calendar calendar = createReferenceCalendar();
		calendar.set(Calendar.MONTH, month - 1);
		return calendar.getTime();
	}

	/**
	 * create a Date Object at the reference calendar by a given day value
	 * @param day 1 - 31
	 * @return
	 */
	public static Date createDay(int day){
		Calendar calendar = createReferenceCalendar();
		calendar.set(Calendar.DAY_OF_MONTH, day);
		return calendar.getTime();
	}

	/**
	 * create a Date Object at the reference calendar by a given hour value
	 * @param hour 0 - 23
	 * @return
	 */
	public static Date createHour(int hour){
		Calendar calendar = createReferenceCalendar();
		calendar.set(Calendar.HOUR_OF_DAY, hour);
		return calendar.getTime();
	}

	/**
	 * create a Date Object at the reference calendar by a given weekday value
	 * @param weekday 1(Calendar.SUNDAY) - 7(Calendar.SATURDAY)
	 * @return
	 */
	public static Date createWeekday(int weekday){
		Calendar calendar = createReferenceCalendar();
		calendar.set(Calendar.DAY_OF_MONTH, weekday);
		return calendar.getTime();
	}

	/**
	 * return the weekday in String format given a Calendar Object
	 * eg. Sun, Mon, Wed
	 * @param calendar
	 * @return
	 */
	public static String getWeekdayStr(Calendar calendar) {
		return getWeekdayStr(calendar.getTime());
	}

	/**
	 * return the weekday in String format given a Date Object
	 * eg. Sun, Mon, Wed
	 * @param date
	 * @return
	 */
	public static String getWeekdayStr(Date date) {
		SimpleDateFormat sdf = new SimpleDateFormat("E", Locale.ENGLISH);
		return sdf.format(date);
	}

	/**
	 * return the weekday in String format given a int value
	 * 1->Sun, 2->Mon, 7->Sat
	 * @param weekday
	 * @return
	 */
	public static String getWeekdayStr(int weekday) {
		return getWeekdayStr(createWeekday(weekday));
	}

	/**
	 * return the weekday in int format given a Calendar Object
	 * Sun -> 1, Mon -> 2, Sat ->7
	 * @param calendar
	 * @return
	 */
	public static int getWeekdayInt(Calendar calendar) {
		return getWeekdayInt(calendar.getTime());
	}

	/**
	 * return the weekday in int format given a Date Object
	 * Sun -> 1, Mon -> 2, Sat ->7
	 * @param date
	 * @return
	 */
	public static int getWeekdayInt(Date date) {
		String weekdayStr = getWeekdayStr(date);
		int weekday = -1;

		if("Sun".equals(weekdayStr)){
			weekday = Calendar.SUNDAY;
		} else if("Mon".equals(weekdayStr)){
			weekday = Calendar.MONTH;
		}else if("Tue".equals(weekdayStr)){
			weekday = Calendar.TUESDAY;
		}else if("Wed".equals(weekdayStr)){
			weekday = Calendar.WEDNESDAY;
		}else if("Thu".equals(weekdayStr)){
			weekday = Calendar.THURSDAY;
		}else if("Fri".equals(weekdayStr)){
			weekday = Calendar.FRIDAY;
		}else if("Sat".equals(weekdayStr)){
			weekday = Calendar.SATURDAY;
		}

		return weekday;
	}
}
