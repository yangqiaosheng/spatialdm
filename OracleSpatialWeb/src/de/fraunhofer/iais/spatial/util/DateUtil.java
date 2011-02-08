package de.fraunhofer.iais.spatial.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang.time.DateUtils;

import de.fraunhofer.iais.spatial.dto.FlickrEuropeAreaDto.Level;

public class DateUtil {

	public final static Calendar today = Calendar.getInstance();
	public final static SortedSet<String> allYearIntStrs = new TreeSet<String>();
	public final static SortedSet<String> allMonthIntStrs = new TreeSet<String>();
	public final static SortedSet<String> allMonthShortStrs = new TreeSet<String>();
	public final static SortedSet<String> allMonthFullStrs = new TreeSet<String>();
	public final static SortedSet<String> allDayIntStrs = new TreeSet<String>();
	public final static SortedSet<String> allHourIntStrs = new TreeSet<String>();
	public final static SortedSet<String> allWeekdayShortStrs = new TreeSet<String>();
	public final static SortedSet<String> allWeekdayFullStrs = new TreeSet<String>();

	public final static SortedSet<Integer> allYearInts = new TreeSet<Integer>();
	public final static SortedSet<Integer> allMonthInts = new TreeSet<Integer>();
	public final static SortedSet<Integer> allDayInts = new TreeSet<Integer>();
	public final static SortedSet<Integer> allHourInts = new TreeSet<Integer>();
	public final static SortedSet<Integer> allWeekdayInts = new TreeSet<Integer>();

	public final static SortedSet<Date> allYearDates = new TreeSet<Date>();
	public final static SortedSet<Date> allMonthDates = new TreeSet<Date>();
	public final static SortedSet<Date> allDayDates = new TreeSet<Date>();
	public final static SortedSet<Date> allHourDates = new TreeSet<Date>();
	public final static SortedSet<Date> allWeekdayDates = new TreeSet<Date>();

	static {
		allYearIntStrs(allYearIntStrs);
		allMonthIntStrs(allMonthIntStrs);
		allDayIntStrs(allDayIntStrs);
		allHourIntStrs(allHourIntStrs);
		allMonthShortStrs(allMonthShortStrs);
		allMonthFullStrs(allMonthFullStrs);
		allWeekdayShortStrs(allWeekdayShortStrs);
		allWeekdayFullStrs(allWeekdayFullStrs);

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


	private static void allYearIntStrs(Set<String> years) {
		for (int i = 2005; i <= today.get(Calendar.YEAR); i++) {
			years.add(String.format("%04d", i));
		}
	}

	private static void allMonthIntStrs(Set<String> months) {
		for (int i = 1; i <= 12; i++) {
			months.add(String.format("%02d", i));
		}
	}

	private static void allMonthShortStrs(Set<String> months) {
		for (int i = 1; i <= 12; i++) {
			months.add(getShortMonthStr(i));
		}
	}

	private static void allMonthFullStrs(Set<String> months) {
		for (int i = 1; i <= 12; i++) {
			months.add(getFullMonthStr(i));
		}
	}

	private static void allDayIntStrs(Set<String> days) {
		for (int i = 1; i <= 31; i++) {
			days.add(String.format("%02d", i));
		}
	}

	private static void allHourIntStrs(Set<String> hours) {
		for (int i = 0; i <= 23; i++) {
			hours.add(String.format("%02d", i));
		}
	}

	private static void allWeekdayShortStrs(Set<String> weekdays) {
		for (int i = 1; i <= 7; i++) {
			weekdays.add(getWeekdayShortStr(i));
		}
	}

	private static void allWeekdayFullStrs(Set<String> weekdays) {
		for (int i = 1; i <= 31; i++) {
			weekdays.add(getWeekdayFullStr(i));
		}
	}

	private static void allYearInts(Set<Integer> years) {
		for (int i = 2005; i <= today.get(Calendar.YEAR); i++) {
			years.add(i);
		}
	}

	private static void allMonthInts(Set<Integer> months) {
		for (int i = 1; i <= 12; i++) {
			months.add(i);
		}
	}

	private static void allDayInts(Set<Integer> days) {
		for (int i = 1; i <= 31; i++) {
			days.add(i);
		}
	}

	private static void allHourInts(Set<Integer> hours) {
		for (int i = 0; i <= 23; i++) {
			hours.add(i);
		}
	}

	private static void allWeekdayInts(Set<Integer> weekdays) {
		for (int i = 1; i <= 7; i++) {
			weekdays.add(i);
		}
	}

	private static void allYearDates(Set<Date> years) {
		for (int i = 2005; i <= today.get(Calendar.YEAR); i++) {
			years.add(createYear(i));
		}
	}

	private static void allMonthDates(Set<Date> months) {
		for (int i = 1; i <= 12; i++) {
			months.add(createMonth(i));
		}
	}

	private static void allDayDates(Set<Date> days) {
		for (int i = 1; i <= 31; i++) {
			days.add(createDay(i));
		}
	}

	private static void allHourDates(Set<Date> hours) {
		for (int i = 0; i <= 23; i++) {
			hours.add(createHour(i));
		}
	}

	private static void allWeekdayDates(Set<Date> weekdays) {
		for (int i = 1; i <= 7; i++) {
			weekdays.add(createWeekday(i));
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
	 * return the weekday in Short String format given a Date Object
	 * eg. Sun, Mon, Wed
	 * @param date
	 * @return
	 */
	public static String getWeekdayShortStr(Date date) {
		SimpleDateFormat sdf = new SimpleDateFormat("E", Locale.ENGLISH);
		return sdf.format(date);
	}

	/**
	 * return the weekday in Short String format given a int value
	 * 1->Sun, 2->Mon, 7->Sat
	 * @param weekday
	 * @return
	 */
	public static String getWeekdayShortStr(int weekday) {
		return getWeekdayShortStr(createWeekday(weekday));
	}

	/**
	 * return the weekday in Full String format given a Date Object
	 * eg. Sunday, Monday, Wednesday
	 * @param date
	 * @return
	 */
	public static String getWeekdayFullStr(Date date) {
		SimpleDateFormat sdf = new SimpleDateFormat("EEEEE", Locale.ENGLISH);
		return sdf.format(date);
	}

	/**
	 * return the weekday in Full String format given a int value
	 * 1->Sunday, 2->Monday, 7->Saturday
	 * @param weekday
	 * @return
	 */
	public static String getWeekdayFullStr(int weekday) {
		return getWeekdayFullStr(createWeekday(weekday));
	}

	/**
	 * return the weekday in int format given a Date Object
	 * Sun -> 1, Mon -> 2, Sat ->7
	 * @param date
	 * @return
	 */
	public static int getWeekdayInt(Date date) {
		String weekdayStr = getWeekdayShortStr(date);
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

	/**
	 * return the month in Short String format given a Date Object
	 * eg. Jan <- 1, Feb <- 2, Jul <- 7
	 * @param date
	 * @return
	 */
	public static String getShortMonthStr(Date date) {
		SimpleDateFormat sdf = new SimpleDateFormat("MMM", Locale.ENGLISH);
		return sdf.format(date);
	}

	/**
	 * return the month in Short String format given a int value
	 * eg. Jan <- 1, Feb <- 2, Jul <- 7
	 * @param month
	 * @return
	 */
	public static String getShortMonthStr(int month) {
		return getShortMonthStr(createMonth(month));
	}

	/**
	 * return the month in Full String format given a Date Object
	 * eg. January <- 1, February <- 2, July <- 7
	 * @param date
	 * @return
	 */
	public static String getFullMonthStr(Date date) {
		SimpleDateFormat sdf = new SimpleDateFormat("MMM", Locale.ENGLISH);
		return sdf.format(date);
	}

	/**
	 * return the month in Full String format given a int value
	 * eg. January <- 1, February <- 2, July <- 7
	 * @param month
	 * @return
	 */
	public static String getFullMonthStr(int month) {
		return getFullMonthStr(createMonth(month));
	}

	/**
	 * return the Chart DataLabel in String format given a int value
	 * eg. 	WEEKDAY:Sun <- 1, Mon <- 2, Sat <- 7
	 * 		YEAR: 	2007 <- 2007
	 * 		MONTH: 	Jan <- 1, Feb <- 2, Jul <- 7
	 * 		DAY: 	1 <- 1, 2 <- 2, 7 <- 7
	 * 		HOUR: 	1 <- 1, 2 <- 2, 7 <- 7
	 * @param date
	 * @param displayLevel
	 * @return
	 */
	public static String getChartLabelStr(int date, Level displayLevel) {
		String str = "";

		switch (displayLevel) {
		case WEEKDAY:
			str = getWeekdayShortStr(createWeekday(date));
			break;

		case YEAR:
			str = date + "";
			break;

		case MONTH:
			str = getShortMonthStr(createMonth(date));
			break;

		case DAY:
			str = date + "";
			break;

		case HOUR:
			str = date + "";
			break;
		}

		return str;
	}

}