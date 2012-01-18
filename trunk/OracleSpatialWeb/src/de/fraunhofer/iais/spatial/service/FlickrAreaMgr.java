package de.fraunhofer.iais.spatial.service;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.math.NumberUtils;
import org.jdom.CDATA;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import de.fraunhofer.iais.spatial.dao.FlickrAreaDao;
import de.fraunhofer.iais.spatial.dto.FlickrAreaDto;
import de.fraunhofer.iais.spatial.dto.SessionMutex;
import de.fraunhofer.iais.spatial.dto.FlickrAreaDto.Level;
import de.fraunhofer.iais.spatial.entity.FlickrArea;
import de.fraunhofer.iais.spatial.entity.FlickrAreaResult;
import de.fraunhofer.iais.spatial.entity.FlickrArea.Radius;
import de.fraunhofer.iais.spatial.util.ChartUtil;
import de.fraunhofer.iais.spatial.util.DateUtil;
import de.fraunhofer.iais.spatial.util.FlickrAreaUtil;
import de.fraunhofer.iais.spatial.util.StringUtil;
import de.fraunhofer.iais.spatial.util.XmlUtil;

public class FlickrAreaMgr {

	private FlickrAreaCancelableJob areaCancelableJob;
	private FlickrAreaDao areaDao;

	public FlickrAreaCancelableJob getAreaCancelableJob() {
		return areaCancelableJob;
	}

	public void setAreaCancelableJob(FlickrAreaCancelableJob areaCancelableJob) {
		this.areaCancelableJob = areaCancelableJob;
	}

	public FlickrAreaDao getAreaDao() {
		return areaDao;
	}

	public void setAreaDao(FlickrAreaDao areaDao) {
		this.areaDao = areaDao;
	}

	public void countSelected(List<FlickrAreaResult> areaResults, FlickrAreaDto areaDto) throws InterruptedException{
		Date timestamp = new Date();
		SessionMutex sessionMutex = new SessionMutex(timestamp);
		this.getAreaCancelableJob().countSelected(timestamp, sessionMutex, areaResults, areaDto);
	}

	public List<FlickrAreaResult> createAreaResults(List<FlickrArea> areas) {
		List<FlickrAreaResult> areaResults = Lists.newArrayList();
		for (FlickrArea area : areas) {
			FlickrAreaResult areaResult = new FlickrAreaResult(area);
			areaResults.add(areaResult);
		}
		return areaResults;
	}


	/**
	 * calculate the histograms DataSets for each FlickrArea
	 * @param areas
	 * @param areaDto
	 */
	public void calculateHistograms(List<FlickrAreaResult> areaResults, FlickrAreaDto areaDto) {

		int queryStrsLength = areaDto.getQueryStrsLength();
		for (FlickrAreaResult areaResult : areaResults) {
			Map<Integer, Integer> yearData = areaResult.getHistograms().getYears();
			Map<Integer, Integer> monthData = areaResult.getHistograms().getMonths();
			Map<Integer, Integer> dayData = areaResult.getHistograms().getDays();
			Map<Integer, Integer> hourData = areaResult.getHistograms().getHours();
			Map<Integer, Integer> weekdayData = areaResult.getHistograms().getWeekdays();

			Calendar calendar = DateUtil.createReferenceCalendar();
			calendar.setLenient(false);

			//set values
			for (Map.Entry<String, Integer> e : areaResult.getArea().getHoursCount().entrySet()) {
				if (areaDto.getQueryStrs().contains(e.getKey().substring(0, queryStrsLength))) {
					int hour = Integer.parseInt(e.getKey().substring(11, 13));
					hourData.put(hour, e.getValue() + hourData.get(hour));

					int day = Integer.parseInt(e.getKey().substring(8, 10));
					dayData.put(day, e.getValue() + dayData.get(day));

					int month = Integer.parseInt(e.getKey().substring(5, 7));
					monthData.put(month, e.getValue() + monthData.get(month));

					int year = Integer.parseInt(e.getKey().substring(0, 4));
					yearData.put(year, e.getValue() + yearData.get(year));

					calendar.set(Calendar.YEAR, Integer.parseInt(e.getKey().substring(0, 4)));
					calendar.set(Calendar.MONTH, Integer.parseInt(e.getKey().substring(5, 7)) - 1);
					calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(e.getKey().substring(8, 10)));
					int weekday = DateUtil.getWeekdayInt(calendar.getTime());
					weekdayData.put(weekday, e.getValue() + weekdayData.get(weekday));
				}
			}
		}
	}


	public List<FlickrAreaResult> countTags(List<FlickrArea> areas, FlickrAreaDto areaDto) throws Exception {
		List<FlickrAreaResult> areaResults = Lists.newArrayList();
		for (FlickrArea area : areas) {
			areaResults.add(countTag(area, areaDto));
		}

		return areaResults;
	}

	public FlickrAreaResult countTag(FlickrArea area, FlickrAreaDto areaDto) throws InterruptedException {
		Date timestamp = new Date();
		SessionMutex sessionMutex = new SessionMutex(timestamp);
		return this.getAreaCancelableJob().countTag(timestamp, sessionMutex, area, areaDto);
	}



	@SuppressWarnings("unchecked")
	public void parseXmlRequest(String xml, FlickrAreaDto areaDto) throws JDOMException, IOException, ParseException {
		xml = StringUtil.shortNum2Long(StringUtil.FullMonth2Num(xml));
		Document document = XmlUtil.string2Xml(xml);
		Element rootElement = document.getRootElement();

		// <screen>
		areaDto.setRadius(Radius.R80000); //default Radius
		Element screenElement = rootElement.getChild("screen");
		if (screenElement != null) {
			// <screen><bounds>((51.02339744960504, 5.565434570312502), (52.14626715707633, 8.377934570312501))</bounds>
			String boundsStr = screenElement.getChildText("bounds");
			if (StringUtils.isNotBlank(boundsStr)) {
				Pattern boundsPattern = Pattern.compile("\\(\\(([-0-9.]*), ([-0-9.]*)\\), \\(([-0-9.]*), ([-0-9.]*)\\)\\)");
				Matcher boundsMatcher = boundsPattern.matcher(boundsStr.trim());
				if (boundsMatcher.find()) {
					Rectangle2D boundaryRect = new Rectangle2D.Double();
					areaDto.setBoundaryRect(boundaryRect);
					double minY = Double.parseDouble(boundsMatcher.group(1));
					double minX = Double.parseDouble(boundsMatcher.group(2));
					double maxY = Double.parseDouble(boundsMatcher.group(3));
					double maxX = Double.parseDouble(boundsMatcher.group(4));
					boundaryRect.setRect(minX, minY, maxX - minX, maxY - minY);
				}
			}

			// <screen><center>(51.58830123054393, 6.971684570312502)</center>
			String centerStr = screenElement.getChildText("center");
			if (StringUtils.isNotBlank(centerStr)) {
				Pattern centerPattern = Pattern.compile("\\(([-0-9.]*), ([-0-9.]*)\\)");
				Matcher centerMachter = centerPattern.matcher(centerStr.trim());
				if (centerMachter.find()) {
					Point2D center = new Point2D.Double();
					areaDto.setCenter(center);
					center.setLocation(Double.parseDouble(centerMachter.group(2)), Double.parseDouble(centerMachter.group(1)));
				}
			}

			if(!areaDto.getBoundaryRect().contains(areaDto.getCenter())){
				areaDto.setCrossDateLine(true);
			}

			// <screen><zoom>9</zoom>
			String zoomStr = screenElement.getChildText("zoom");
			if (StringUtils.isNotBlank(zoomStr)) {
				int zoom = Integer.parseInt(zoomStr.trim());
				Radius radius = FlickrAreaUtil.judgeRadius(zoom);
				areaDto.setRadius(radius);
				areaDto.setZoom(zoom);
			}
		}

		/*
//		<transform>
//		    <move>
//		      	<from> 45.478158, 9.1237</from>
//		      	<to> 43.816111, 4.359167</to>
//		    </move>
//		    <scale>1.0</scale>
//		</transform>*/

		Element transformElement = rootElement.getChild("transform");
		if (transformElement != null) {
			Element moveElement = transformElement.getChild("move");
			String fromStr = moveElement.getChildText("from");
			String toStr = moveElement.getChildText("to");
			Pattern pointPattern = Pattern.compile("\\(([-0-9.]*), ([-0-9.]*)\\)");
			Matcher fromMachter = pointPattern.matcher(fromStr.trim());
			Matcher toMachter = pointPattern.matcher(toStr.trim());
			String scaleStr = transformElement.getChildText("scale");
			double scale = NumberUtils.toDouble(scaleStr.trim());
			if (fromMachter.find() && toMachter.find() && scale != 0) {
				Point2D from = new Point2D.Double(Double.parseDouble(fromMachter.group(2)), Double.parseDouble(fromMachter.group(1)));
				Point2D to = new Point2D.Double(Double.parseDouble(toMachter.group(2)), Double.parseDouble(toMachter.group(1)));
				Point2D transfromVector = new Point2D.Double((to.getX() - from.getX()) * scale, (to.getY() - from.getY()) * scale);
				areaDto.setTransfromVector(transfromVector);
			}
		}

		// <polygon>(51.58830123054393, 6.971684570312502)(51.67184146523792, 7.647343750000002)(51.44644311790073, 7.298527832031252)</polygon>
		String polygonStr = rootElement.getChildText("polygon");
		if (StringUtils.isNotBlank(polygonStr)) {
			Pattern polygonPattern = Pattern.compile("\\(([-0-9.]*), ([-0-9.]*)\\)");
			Matcher polygonMachter = polygonPattern.matcher(polygonStr);
			List<Point2D> polygon = new LinkedList<Point2D>();
			areaDto.setPolygon(polygon);
			while (polygonMachter.find()) {
				Point2D point = new Point2D.Double();
				point.setLocation(Double.parseDouble(polygonMachter.group(2)), Double.parseDouble(polygonMachter.group(1)));
				polygon.add(point);
			}

			setCalendarQueryStrs(areaDto);
		}

		// <interval>15/09/2010 - 19/10/2010</interval>
		String intervalStr = rootElement.getChildText("interval");
		if (StringUtils.isNotBlank(intervalStr)) {
			Pattern intervalPattern = Pattern.compile("([\\d]{2}/[\\d]{2}/[\\d]{4}) - ([\\d]{2}/[\\d]{2}/[\\d]{4})");
			Matcher intervalMachter = intervalPattern.matcher(intervalStr);

			Set<String> queryStrs = areaDto.getQueryStrs();

			SimpleDateFormat inputDateFormat = new SimpleDateFormat("dd/MM/yyyy");
			areaDto.setQueryLevel(Level.HOUR);

			if (intervalMachter.find()) {
				SimpleDateFormat hourDateFormat = new SimpleDateFormat(FlickrAreaDao.hourDateFormatStr);
				Date beginDate = inputDateFormat.parse(intervalMachter.group(1));
				Date endDate = inputDateFormat.parse(intervalMachter.group(2));
				areaDto.setBeginDate(beginDate);
				areaDto.setEndDate(endDate);

				Calendar calendar = Calendar.getInstance();
				calendar.setTime(beginDate);
				Calendar end = Calendar.getInstance();
				end.setTime(endDate);
				end.add(Calendar.DATE, 1);
				while (calendar.getTime().before(end.getTime())) {
					queryStrs.add(hourDateFormat.format(calendar.getTime()));
					calendar.add(Calendar.HOUR, 1);
				}
			}
		}


		// <selected_days>Sep 08 2010,Sep 10 2010,Oct 14 2010,Oct 19 2010,Sep 24 2010,Sep 22 2005,Sep 09 2005</selected_days>
		String selectedDaysStr = rootElement.getChildText("selected_days");
		if (StringUtils.isNotBlank(selectedDaysStr)) {
			SimpleDateFormat dayDateFormat = new SimpleDateFormat(FlickrAreaDao.dayDateFormatStr);
			Pattern selectedDaysPattern = Pattern.compile("([A-Z]{1}[a-z]{2} [\\d]{2} [\\d]{4})");
			Matcher selectedDaysMachter = selectedDaysPattern.matcher(selectedDaysStr);
			SortedSet<Date> selectedDays = new TreeSet<Date>();
			Set<String> queryStrs = areaDto.getQueryStrs();

			// day of week in English format
			SimpleDateFormat inputDateFormat = new SimpleDateFormat("MMM dd yyyy", Locale.ENGLISH);
			areaDto.setQueryLevel(Level.HOUR);

			while (selectedDaysMachter.find()) {
				Date selectedDay = inputDateFormat.parse(selectedDaysMachter.group());
				selectedDays.add(selectedDay);
				String dayStr = dayDateFormat.format(selectedDay);
				for(String hourStr : DateUtil.allHourIntStrs){
					queryStrs.add(dayStr + "@" + hourStr);
				}
			}
		}


		// <calendar>
		Element calendarElement = rootElement.getChild("calendar");
		if (calendarElement != null) {

			// <calendar><years>
			Element yearsElement = calendarElement.getChild("years");
			if (yearsElement != null) {
				List<Element> yearElements = yearsElement.getChildren("year");
				for (Element yearElement : yearElements) {
					String year = yearElement.getText().trim();
					if (StringUtils.isNotBlank(year)) {
						if(!DateUtil.allYearIntStrs.contains(year)){
							throw new IllegalArgumentException("wrong string of year:" + year);
						}
						areaDto.getYears().add(year);
					}
				}
			}

			// <calendar><month>
			Element monthsElement = calendarElement.getChild("months");
			if (monthsElement != null) {
				List<Element> monthElements = monthsElement.getChildren("month");
				for (Element monthElement : monthElements) {
					String month = monthElement.getText().trim();
					if (StringUtils.isNotBlank(month)) {
						if(!DateUtil.allMonthIntStrs.contains(month)){
							throw new IllegalArgumentException("wrong string of month:" + month);
						}
						areaDto.getMonths().add(month);
					}
				}
			}

			// <calendar><days>
			Element daysElement = calendarElement.getChild("days");
			if (daysElement != null) {
				List<Element> dayElements = daysElement.getChildren("day");
				for (Element dayElement : dayElements) {
					String day = dayElement.getText().trim();
					if (StringUtils.isNotBlank(day)) {
						if(!DateUtil.allDayIntStrs.contains(day)){
							throw new IllegalArgumentException("wrong string of day:" + day);
						}
						areaDto.getDays().add(day);
					}
				}
			}

			// <calendar><hours>
			Element hoursElement = calendarElement.getChild("hours");
			if (hoursElement != null) {
				List<Element> hourElements = hoursElement.getChildren("hour");
				for (Element hourElement : hourElements) {
					String hour = hourElement.getText().trim();
					if (StringUtils.isNotBlank(hour)) {
						if(!DateUtil.allHourIntStrs.contains(hour)){
							throw new IllegalArgumentException("wrong string of hour:" + hour);
						}
						areaDto.getHours().add(hour);
					}
				}
			}

			// <calendar><weekdays>
			Element weekdaysElement = calendarElement.getChild("weekdays");
			if (weekdaysElement != null) {
				List<Element> weekdayElements = weekdaysElement.getChildren("weekday");
				for (Element weekdayElement : weekdayElements) {
					String weekday = weekdayElement.getText().trim();
					if (StringUtils.isNotBlank(weekday)) {
						if(!DateUtil.allWeekdayFullStrs.contains(weekday)){
							throw new IllegalArgumentException("wrong string of weekday:" + weekday);
						}
						areaDto.getWeekdays().add(weekday);
					}
				}
			}

			setCalendarQueryStrs(areaDto);
		}

	}

	private void setCalendarQueryStrs(FlickrAreaDto areaDto) {

		areaDto.setQueryLevel(Level.HOUR);

		// construct the Query Strings
		Set<String> queryStrs = areaDto.getQueryStrs();

		SortedSet<String> tempYears = new TreeSet<String>(areaDto.getYears());
		SortedSet<String> tempMonths = new TreeSet<String>(areaDto.getMonths());
		SortedSet<String> tempDays = new TreeSet<String>(areaDto.getDays());
		SortedSet<String> tempHours = new TreeSet<String>(areaDto.getHours());

		// complete the options when they are not selected
		if (tempYears.size() == 0) {
			tempYears = new TreeSet<String>(DateUtil.allYearIntStrs);
		}
		if (tempMonths.size() == 0) {
			tempMonths = new TreeSet<String>(DateUtil.allMonthIntStrs);
		}
		if (tempDays.size() == 0) {
			tempDays = new TreeSet<String>(DateUtil.allDayIntStrs);
		}
		if (tempHours.size() == 0) {
			tempHours = new TreeSet<String>(DateUtil.allHourIntStrs);
		}

		// day of week in English format
		SimpleDateFormat sdf = new SimpleDateFormat("EEEEE", Locale.ENGLISH);
		Calendar calendar = Calendar.getInstance();
		calendar.setLenient(false);

		SimpleDateFormat hourDateFormat = new SimpleDateFormat(FlickrAreaDao.hourDateFormatStr);

		for (String y : tempYears) {
			for (String m : tempMonths) {
				for (String d : tempDays) {
					for (String h : tempHours) {
//						calendar.set(Integer.parseInt(y), Integer.parseInt(m) - 1, Integer.parseInt(d));
						calendar.set(Calendar.YEAR, Integer.parseInt(y));
						calendar.set(Calendar.MONTH, Integer.parseInt(m) - 1);
						calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(d));
						calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(h));
						calendar.set(Calendar.MINUTE, 0);
						calendar.set(Calendar.SECOND, 0);

						try {
							// filter out the selected weekdays
							if (areaDto.getWeekdays().size() == 0 || areaDto.getWeekdays().contains(sdf.format(calendar.getTime()))) {
								//queryStrs.add(y + "-" + m + "-" + d + "@" + h);
								queryStrs.add(hourDateFormat.format(calendar.getTime()));
							}
						} catch (IllegalArgumentException e) {
							// omit the wrong date
						}
					}
				}
			}
		}
	}

	@Deprecated
	public void createBarChart(Map<String, Integer> cs) {
		ChartUtil.createBarChart(cs, "temp/bar.jpg");
	}

	public void createTagTimeSeriesChartOld(FlickrArea area, String tag, FlickrAreaDto areaDto, String title, OutputStream os) throws ParseException, IOException {

		Map<Date, Integer> seriesChartData = createTagTimeSeriesData(area, tag, areaDto);

		ChartUtil.createTimeSeriesChartOld(seriesChartData, title, os);
	}

	public Map<Date, Integer> createTagTimeSeriesData(FlickrArea area, String tag, FlickrAreaDto areaDto) throws ParseException {
		Map<Date, Integer> seriesData = new TreeMap<Date, Integer>();

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

		if (MapUtils.isEmpty(area.getHoursTagsCount())) {
			areaDao.loadHoursTagsCount(area);
		}

		Map<String, Map<String, Integer>> hoursTagsCount = area.getHoursTagsCount();

		Set<String> displayYears = defineDisplayYears(tag, areaDto, hoursTagsCount, 10);

		// init
		for (String year : displayYears) {
			for (int j : DateUtil.allMonthInts) {
				for (int k : DateUtil.allDayInts) {
					seriesData.put(sdf.parse(year + "-" + new DecimalFormat("00").format(j) + "-" +  new DecimalFormat("00").format(k)), 0);
				}
			}
		}

		// fill with values
		for (Map.Entry<String, Map<String, Integer>> e : hoursTagsCount.entrySet()) {
			if (displayYears.contains(e.getKey().substring(0, 4))) {
				for (Map.Entry<String, Integer> term : hoursTagsCount.get(e.getKey()).entrySet()) {
					if(term.getKey().equals(tag) && areaDto.getQueryStrs().contains(e.getKey())){
						seriesData.put(sdf.parse(e.getKey().substring(0, FlickrAreaDao.hourDateFormatStr.length())), term.getValue());
					}
				}
			}
		}
		return seriesData;
	}

	private Set<String> defineDisplayYears(String tag, FlickrAreaDto areaDto, Map<String, Map<String, Integer>> hoursTagsCount, int num) {

		Set<String> selectedYears = Sets.newTreeSet();
		for(String queryStr: areaDto.getQueryStrs()){
			selectedYears.add(queryStr.substring(0, 4));
		}
		Set<String> displayYears = Sets.newTreeSet();
		TreeSet<String> approvedYears = Sets.newTreeSet();
		for (Map.Entry<String, Map<String, Integer>> e : hoursTagsCount.entrySet()) {
			if (selectedYears.contains(e.getKey().substring(0, 4))) {
				for (Map.Entry<String, Integer> term : hoursTagsCount.get(e.getKey()).entrySet()) {
					if(term.getKey().equals(tag)){
						approvedYears.add(e.getKey().substring(0, 4));
						break;
					}
				}
			}
		}

		approvedYears.retainAll(selectedYears);
		int i = 0;
		if (approvedYears.size() > num) {
			i = approvedYears.size() - num;
		}

		for (String year : approvedYears) {
			if (--i < 0) {
				displayYears.add(year);
			}
		}
		return displayYears;
	}


	@Deprecated
	public void createTimeSeriesChartOld(FlickrArea area, Set<String> years, OutputStream os) throws ParseException, IOException {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

		Map<Date, Integer> countsMap = new TreeMap<Date, Integer>();
		for (Map.Entry<String, Integer> e : area.getDaysCount().entrySet()) {
			if (years.contains(e.getKey().substring(0, 4))) {
				countsMap.put(sdf.parse(e.getKey()), e.getValue());
			}
		}

		ChartUtil.createTimeSeriesChartOld(countsMap, "#Photos Distribution", os);
	}

	public void createTimeSeriesChart(List<FlickrArea> areas, Level displayLevel, FlickrAreaDto areaDto, int width, int height, boolean displayLegend, boolean smooth, boolean icon, OutputStream os) throws ParseException, IOException {

		Map<String, Map<Date, Integer>> displayCountsMap = new LinkedHashMap<String, Map<Date, Integer>>();

		int queryStrsLength = areaDto.getQueryStrsLength();

		for (FlickrArea area : areas) {

			Map<Integer, Integer> intCounts = new TreeMap<Integer, Integer>();
			Map<Date, Integer> dateCounts = new TreeMap<Date, Integer>();
			switch (displayLevel) {
			case HOUR:
				// init
				for (int hour : DateUtil.allHourInts) {
					intCounts.put(hour, 0);
				}

				//set values
				for (Map.Entry<String, Integer> e : area.getHoursCount().entrySet()) {
					if (areaDto.getQueryStrs().contains(e.getKey().substring(0, queryStrsLength))) {
						int hour = Integer.parseInt(e.getKey().substring(11, 13));
						intCounts.put(hour, e.getValue() + intCounts.get(hour));
					}
				}

				for (Map.Entry<Integer, Integer> e: intCounts.entrySet()){
					dateCounts.put(DateUtil.createHour(e.getKey()), e.getValue());
				}
				break;

			case DAY:
				// init
				for (int day : DateUtil.allDayInts) {
					intCounts.put(day, 0);
				}

				//set values
				for (Map.Entry<String, Integer> e : area.getHoursCount().entrySet()) {
					if (areaDto.getQueryStrs().contains(e.getKey().substring(0, queryStrsLength))) {
						int day = Integer.parseInt(e.getKey().substring(8, 10));
						intCounts.put(day, e.getValue() + intCounts.get(day));
					}
				}

				for (Map.Entry<Integer, Integer> e: intCounts.entrySet()){
					dateCounts.put(DateUtil.createDay(e.getKey()), e.getValue());
				}
				break;

			case MONTH:
				// init
				for (int month : DateUtil.allMonthInts) {
					intCounts.put(month, 0);
				}

				//set values
				for (Map.Entry<String, Integer> e : area.getHoursCount().entrySet()) {
					if (areaDto.getQueryStrs().contains(e.getKey().substring(0, queryStrsLength))) {
						int month = Integer.parseInt(e.getKey().substring(5, 7));
						intCounts.put(month, e.getValue() + intCounts.get(month));
					}
				}

				for (Map.Entry<Integer, Integer> e: intCounts.entrySet()){
					dateCounts.put(DateUtil.createMonth(e.getKey()), e.getValue());
				}
				break;

			case YEAR:
				// init
				for (int year : DateUtil.allYearInts) {
					intCounts.put(year, 0);
				}

				//set values
				for (Map.Entry<String, Integer> e : area.getHoursCount().entrySet()) {
					if (areaDto.getQueryStrs().contains(e.getKey().substring(0, queryStrsLength))) {
						int year = Integer.parseInt(e.getKey().substring(0, 4));
						intCounts.put(year, e.getValue() + intCounts.get(year));
					}
				}

				for (Map.Entry<Integer, Integer> e: intCounts.entrySet()){
					dateCounts.put(DateUtil.createYear(e.getKey()), e.getValue());
				}
				break;

			case WEEKDAY:
				// init
				for (int weekday : DateUtil.allWeekdayInts) {
					intCounts.put(weekday, 0);
				}

				Calendar calendar = DateUtil.createReferenceCalendar();
				calendar.setLenient(false);

				//set values
				for (Map.Entry<String, Integer> e : area.getHoursCount().entrySet()) {
					if (areaDto.getQueryStrs().contains(e.getKey().substring(0, queryStrsLength))) {
						calendar.set(Calendar.YEAR, Integer.parseInt(e.getKey().substring(0, 4)));
						calendar.set(Calendar.MONTH, Integer.parseInt(e.getKey().substring(5, 7)) - 1);
						calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(e.getKey().substring(8, 10)));
						int weekday = DateUtil.getWeekdayInt(calendar.getTime());
						intCounts.put(weekday, e.getValue() + intCounts.get(weekday));
					}
				}

				for (Map.Entry<Integer, Integer> e: intCounts.entrySet()){
					dateCounts.put(DateUtil.createWeekday(e.getKey()), e.getValue());
				}
				break;
			}
			displayCountsMap.put("Area ID: " + area.getRadius() + "-" + area.getId(), dateCounts);
		}


		ChartUtil.createTimeSeriesChart(displayCountsMap, displayLevel, width, height, displayLegend, smooth, icon, os);
	}

	@Deprecated
	public void createXYLineChart(List<FlickrArea> areas, Level displayLevel, FlickrAreaDto areaDto, int width, int height, boolean displayLegend, boolean smooth, OutputStream os) throws ParseException, IOException {

			Map<String, Map<Integer, Integer>> displayCountsMap = new LinkedHashMap<String, Map<Integer, Integer>>();

			int queryStrsLength = areaDto.getQueryStrsLength();

			for (FlickrArea area : areas) {

				Map<Integer, Integer> displayCounts = new TreeMap<Integer, Integer>();
				switch (displayLevel) {
				case HOUR:
					// init
					for (int hour : DateUtil.allHourInts) {
						displayCounts.put(hour, 0);
					}

					//set values
					for (Map.Entry<String, Integer> e : area.getHoursCount().entrySet()) {
						if (areaDto.getQueryStrs().contains(e.getKey().substring(0, queryStrsLength))) {
							int hour = Integer.parseInt(e.getKey().substring(11, 13));
							displayCounts.put(hour, e.getValue() + displayCounts.get(hour));
						}
					}
					break;

				case DAY:
					// init
					for (int day : DateUtil.allDayInts) {
						displayCounts.put(day, 0);
					}

					//set values
					for (Map.Entry<String, Integer> e : area.getHoursCount().entrySet()) {
						if (areaDto.getQueryStrs().contains(e.getKey().substring(0, queryStrsLength))) {
							int day = Integer.parseInt(e.getKey().substring(8, 10));
							displayCounts.put(day, e.getValue() + displayCounts.get(day));
						}
					}
					break;

				case MONTH:
					// init
					for (int month : DateUtil.allMonthInts) {
						displayCounts.put(month, 0);
					}

					//set values
					for (Map.Entry<String, Integer> e : area.getHoursCount().entrySet()) {
						if (areaDto.getQueryStrs().contains(e.getKey().substring(0, queryStrsLength))) {
							int month = Integer.parseInt(e.getKey().substring(5, 7));
							displayCounts.put(month, e.getValue() + displayCounts.get(month));
						}
					}
					break;

				case YEAR:
					// init
					for (int year : DateUtil.allYearInts) {
						displayCounts.put(year, 0);
					}

					//set values
					for (Map.Entry<String, Integer> e : area.getHoursCount().entrySet()) {
						if (areaDto.getQueryStrs().contains(e.getKey().substring(0, queryStrsLength))) {
							int year = Integer.parseInt(e.getKey().substring(0, 4));
							displayCounts.put(year, e.getValue() + displayCounts.get(year));
						}
					}
					break;
				}
				displayCountsMap.put("Area ID: " + area.getRadius() + "-" + area.getId(), displayCounts);
			}

	//		Map<Integer, Integer> cumulativeCounts = new TreeMap<Integer, Integer>();
	//		int cumulativeValue = 0;
	//		for (Map.Entry<Integer, Integer> e : displayCounts.entrySet()) {
	//			System.out.println(e.getKey() + ":" + e.getValue());
	//			cumulativeValue += e.getValue();
	//			cumulativeCounts.put(e.getKey(), cumulativeValue);
	//		}
	//		displayCountsMap.put("cumulative", cumulativeCounts);

			ChartUtil.createXYLineChart(displayCountsMap, displayLevel, width, height, displayLegend, smooth, os);
		}

	public String buildKmlFile(List<FlickrAreaResult> areaResults, String filenamePrefix, Radius radius, Point2D transfromVector, String remoteBasePath, boolean compress) throws UnsupportedEncodingException {
		String localBasePath = this.getClass().getResource("/../../").getPath();
		if (StringUtils.isEmpty(remoteBasePath)) {
			remoteBasePath = "http://localhost:8080/OracleSpatialWeb/";
		}

		Document document = createKmlDoc(areaResults, radius, transfromVector, remoteBasePath);

		if(filenamePrefix != null){
			if(StringUtils.isNotEmpty(filenamePrefix)){
				if(compress == true){
					XmlUtil.xml2Kmz(document, localBasePath + filenamePrefix, true);
				}else {
					XmlUtil.xml2File(document, localBasePath + filenamePrefix + ".kml", false);
				}
			}
		}

		return XmlUtil.xml2String(document, false);
	}

	public String buildKmlString(List<FlickrAreaResult> areaResults, Radius radius, Point2D transfromVector, String remoteBasePath) throws IOException {
		Document document = createKmlDoc(areaResults, radius, transfromVector, remoteBasePath);

		return XmlUtil.xml2String(document, false);
	}

	private Document createKmlDoc(List<FlickrAreaResult> areaResults, Radius radius, Point2D transformVector, String remoteBasePath) {
		if(transformVector == null){
			transformVector = new Point2D.Double();
		}

		Document document = new Document();

		Namespace namespace = Namespace.getNamespace("http://earth.google.com/kml/2.1");
		Element rootElement = new Element("kml", namespace);

		float scale = (float) (Integer.parseInt(radius.toString()) / 30000.0);
		document.setRootElement(rootElement);

		Element documentElement = new Element("Document", namespace);
		rootElement.addContent(documentElement);

		// GroundOverlay
		for (FlickrAreaResult areaResult : areaResults) {
			FlickrArea area = areaResult.getArea();
			if (area.getTotalCount() != 0) {
				String name = String.valueOf(area.getId());
				String description = "";

				Element groundOverlayElement = new Element("GroundOverlay", namespace);
				documentElement.addContent(groundOverlayElement);
				groundOverlayElement.addContent(new Element("name", namespace).addContent(name));
				groundOverlayElement.addContent(new Element("description", namespace).addContent(new CDATA(description)));
//				groundOverlayElement.addContent(new Element("color", namespace).addContent("eeffffff"));
				Element iconElement = new Element("Icon", namespace);
				groundOverlayElement.addContent(iconElement);
				double r = 0;
				String icon = "";

				if (areaResult.getSelectCount() < 100) {
//					r = (double) Math.log10(area.getSelectCount() + 1) / 85.0 * scale;
					icon = remoteBasePath + "images/circle_bl.ico";
				} else if (areaResult.getSelectCount() < 1000) {
//					r = (double) Math.log10(area.getSelectCount() + 1) / 80.0 * scale;
					icon = remoteBasePath + "images/circle_gr.ico";
				} else if (areaResult.getSelectCount() < 10000) {
//					r = (double) Math.log10(area.getSelectCount() + 1) / 70.0 * scale;
					icon = remoteBasePath + "images/circle_lgr.ico";
				} else {
//					r = (double) Math.log10(area.getSelectCount() + 1) / 60.0 * scale;
					icon = remoteBasePath + "images/circle_or.ico";
				}

				r = (double) Math.log10(areaResult.getSelectCount() + 1) / 50.0 * scale;

				Element hrefElement = new Element("href", namespace).addContent(icon);
				iconElement.addContent(hrefElement);

//				Element altitudeElement = new Element("altitude", namespace).addContent(String.valueOf(area.getTotalCount()*100));
//				groundOverlayElement.addContent(altitudeElement);
//				Element altitudeModeElement = new Element("altitudeMode", namespace).addContent("absolute");
//				groundOverlayElement.addContent(altitudeModeElement);
				Element latLonBoxElement = new Element("LatLonBox", namespace);
				groundOverlayElement.addContent(latLonBoxElement);
				Element northElement = new Element("north", namespace).addContent(Double.toString(area.getCenter().getY() + transformVector.getY() + r * 0.55));
				Element southElement = new Element("south", namespace).addContent(Double.toString(area.getCenter().getY() + transformVector.getY()  - r * 0.55));
				Element eastElement = new Element("east", namespace).addContent(Double.toString(area.getCenter().getX() + transformVector.getX()  + r));
				Element westElement = new Element("west", namespace).addContent(Double.toString(area.getCenter().getX() + transformVector.getX()  - r));;
				latLonBoxElement.addContent(northElement);
				latLonBoxElement.addContent(southElement);
				latLonBoxElement.addContent(eastElement);
				latLonBoxElement.addContent(westElement);

//				if (Double.isInfinite(area.getCenter().getY() + r * 0.55)) {
//					System.exit(0);
//				}
			}
		}

		// Polygon
		for (FlickrAreaResult areaResult : areaResults) {
			FlickrArea area = areaResult.getArea();
			String name = areaResult.getSelectCount() + " / " + area.getTotalCount();
			String description = buildKmlDescription(areaResult);

//			String polyStyleColor = "440000"; //not transparent
			String polyStyleColor = "000000"; //transparent
			String polyStyleFill = "1";
			String polyStyleOutline = "1";
			String lineStyleWidth = "2";
			String lineStyleColor = "88ff0000";
			String coordinates = "\n";

			List<Point2D> shape = area.getGeom();
			for (Point2D point: shape) {
				coordinates += (point.getX() + transformVector.getX()) + "," + (point.getY() + transformVector.getY())  + "0\n";
			}

			// create kml
			Element placemarkElement = new Element("Placemark", namespace);
			documentElement.addContent(placemarkElement);

			Element styleElement = new Element("Style", namespace);
			placemarkElement.setAttribute("id", String.valueOf(area.getId()));
			placemarkElement.addContent(new Element("name", namespace).addContent(name));
			placemarkElement.addContent(new Element("description", namespace).addContent(new CDATA(description)));
			placemarkElement.addContent(styleElement);

			Element polyStyleElement = new Element("PolyStyle", namespace);
			styleElement.addContent(polyStyleElement);

			long color = area.getTotalCount() / 30;
			if (color > 255) {
				color = 255;
			}

			polyStyleElement.addContent(new Element("color", namespace).addContent(polyStyleColor + StringUtil.byteToHexString((byte) color)));
			polyStyleElement.addContent(new Element("fill", namespace).addContent(polyStyleFill));
			polyStyleElement.addContent(new Element("outline", namespace).addContent(polyStyleOutline));

			Element lineStyleElement = new Element("LineStyle", namespace);
			styleElement.addContent(lineStyleElement);

			lineStyleElement.addContent(new Element("width", namespace).addContent(lineStyleWidth));
			lineStyleElement.addContent(new Element("color", namespace).addContent(lineStyleColor));

			Element multiGeometryElement = new Element("MultiGeometry", namespace);
			Element polygonElement = new Element("Polygon", namespace);
			Element outerBoundaryIsElement = new Element("outerBoundaryIs", namespace);
			Element coordinatesElement = new Element("coordinates", namespace).addContent(coordinates);
			Element linearRingElement = new Element("LinearRing", namespace).addContent(coordinatesElement);
			placemarkElement.addContent(multiGeometryElement);
			multiGeometryElement.addContent(polygonElement);
			polygonElement.addContent(outerBoundaryIsElement);
//			polygonElement.addContent(new Element("extrude", namespace).addContent("1"));
//			polygonElement.addContent(new Element("altitudeMode", namespace).addContent("absolute"));
			outerBoundaryIsElement.addContent(linearRingElement);

			Element extendedDataElement = new Element("ExtendedData", namespace);
			placemarkElement.addContent(extendedDataElement);
			addKmlExtendedElement(extendedDataElement, namespace, areaResult.getHistograms().getWeekdays(), Level.WEEKDAY);
			addKmlExtendedElement(extendedDataElement, namespace, areaResult.getHistograms().getYears(), Level.YEAR);
			addKmlExtendedElement(extendedDataElement, namespace, areaResult.getHistograms().getMonths(), Level.MONTH);
			addKmlExtendedElement(extendedDataElement, namespace, areaResult.getHistograms().getDays(), Level.DAY);
			addKmlExtendedElement(extendedDataElement, namespace, areaResult.getHistograms().getHours(), Level.HOUR);
		}
		return document;
	}

	private void addKmlExtendedElement(Element extendedDataElement, Namespace namespace, Map<Integer, Integer> chartData, Level displayLevel) {
		for(Map.Entry<Integer, Integer> e : chartData.entrySet()){
			String dataName = displayLevel + "_" + DateUtil.getChartLabelStr(e.getKey(), displayLevel);
			Element dataElement = new Element("Data", namespace).setAttribute("name", dataName);
			dataElement.addContent(new Element("displayName", namespace).addContent(DateUtil.getChartLabelStr(e.getKey(), displayLevel)));
			dataElement.addContent(new Element("value", namespace).addContent(e.getValue() + ""));
			extendedDataElement.addContent(dataElement);
		}
	}

	private String buildKmlDescription(FlickrAreaResult areaResult) {
		int width = 640;
		String weekdayChartImg = createGoogleChartImg("Photos Distribution", width/2, 160, areaResult.getHistograms().getWeekdays(), Level.WEEKDAY);
		String yearChartImg = createGoogleChartImg("Photos Distribution", width/2, 160, areaResult.getHistograms().getYears(), Level.YEAR);
		String monthChartImg = createGoogleChartImg("Photos Distribution", width, 160, areaResult.getHistograms().getMonths(), Level.MONTH);
		String dayChartImg = createGoogleChartImg("Photos Distribution", width, 160, areaResult.getHistograms().getDays(), Level.DAY);
		String hourChartImg = createGoogleChartImg("Photos Distribution", width, 160, areaResult.getHistograms().getHours(), Level.HOUR);

		String description = weekdayChartImg + yearChartImg + "<BR>" + monthChartImg + "<BR>" + dayChartImg + "<BR>" + hourChartImg;
		return description;
	}

	public String createGoogleChartImg(String title, int width, int height, Map<Integer, Integer> histrogramData, Level displayLevel) {
		String values = "";
		String labels = "";
		int maxValue = new TreeSet<Integer>(histrogramData.values()).last();
		int barWithd = (int) (width / histrogramData.size() / 1.3);
		for(Map.Entry<Integer, Integer> e : histrogramData.entrySet()){
			labels += "|" + DateUtil.getChartLabelStr(e.getKey(), displayLevel);
			values += "," + e.getValue();
		}

//		for(Map.Entry<Integer, Integer> e : chartData.entrySet()){
//			String dataName = displayLevel + "_" + DateUtil.getChartLabelStr(e.getKey(), displayLevel);
//			labels += "|$[" + dataName + "/displayName]";
//			values += ",$[" + dataName + "]";
//		}

		String img = "<img src='" +
		"http://chart.apis.google.com/chart" + 	//chart engine
//		"http://kd-photomap.iais.fraunhofer.de/chart_engine/chart" + 	//chart engine
		"?chtt=" + displayLevel +				//chart title
		"&cht=bvs" +							//chart type
		"&chs=" + width + "x" + height +		//chart size(pixel)
		"&chd=t:" + values.substring(1) + 		//values
		"&chxt=x,y" +							//display axises
		"&chxl=0:" + labels +					//labels in x axis
		"&chbh=" + barWithd +					//chbh=<bar_width_or_scale>,<space_between_bars>,<space_between_groups>
		"&chm=N,444444,-1,,12" +				//data marker chm= <marker_type>,<color>,<series_index>,<which_points>,<size>,<z_order>,<placement>
		"&chds=0," + maxValue * 1.2 +			//scale of y axis (default 1-100)
		"&chxr=1,0," + maxValue * 1.2 +			//scale in value (default 1-100)
		"'/>";

		return img;
	}

}
