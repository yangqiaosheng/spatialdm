package de.fraunhofer.iais.spatial.service;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.spatial.geometry.JGeometry;

import org.jdom.CDATA;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;

import de.fraunhofer.iais.spatial.dao.FlickrDeWestAreaDao;
import de.fraunhofer.iais.spatial.dao.jdbc.FlickrDeWestAreaDaoJdbc;
import de.fraunhofer.iais.spatial.dto.FlickrDeWestAreaDto;
import de.fraunhofer.iais.spatial.dto.FlickrDeWestAreaDto.QueryLevel;
import de.fraunhofer.iais.spatial.entity.FlickrDeWestArea;
import de.fraunhofer.iais.spatial.entity.FlickrDeWestArea.Radius;
import de.fraunhofer.iais.spatial.util.ChartUtil;
import de.fraunhofer.iais.spatial.util.StringUtil;
import de.fraunhofer.iais.spatial.util.XmlUtil;

public class FlickrDeWestAreaMgr {

	private FlickrDeWestAreaDao flickrDeWestAreaDao = new FlickrDeWestAreaDaoJdbc();

	public FlickrDeWestAreaDao getAreaDao() {
		return flickrDeWestAreaDao;
	}

	public void setAreaDao(FlickrDeWestAreaDao areaDao) {
		this.flickrDeWestAreaDao = areaDao;
	}

	public void fillCache() {
		this.flickrDeWestAreaDao.getAllAreas(Radius.R80000);
		this.flickrDeWestAreaDao.getAllAreas(Radius.R40000);
		this.flickrDeWestAreaDao.getAllAreas(Radius.R20000);
//		this.flickrDeWestAreaDao.getAllAreas(Radius.R10000);
//		this.flickrDeWestAreaDao.getAllAreas(Radius.R5000);
	}

	public void countHours(List<FlickrDeWestArea> as, Set<String> hours) {
		for (FlickrDeWestArea a : as) {
			int num = 0;
			for (Map.Entry<String, Integer> e : a.getHoursCount().entrySet()) {
				if (hours.contains(e.getKey())) {
					num += e.getValue();
				}
			}
			a.setSelectCount(num);
		}
	}

	public void countDays(List<FlickrDeWestArea> as, Set<String> days) {
		for (FlickrDeWestArea a : as) {
			int num = 0;
			for (Map.Entry<String, Integer> e : a.getDaysCount().entrySet()) {
				if (days.contains(e.getKey())) {
					num += e.getValue();
				}
			}
			a.setSelectCount(num);
		}
	}

	public void countMonths(List<FlickrDeWestArea> as, Set<String> months) {
		for (FlickrDeWestArea a : as) {
			int num = 0;
			for (Map.Entry<String, Integer> e : a.getMonthsCount().entrySet()) {
				if (months.contains(e.getKey())) {
					num += e.getValue();
				}
			}
			a.setSelectCount(num);
		}
	}

	public void countYears(List<FlickrDeWestArea> as, Set<String> years) {
		for (FlickrDeWestArea a : as) {
			int num = 0;
			for (Map.Entry<String, Integer> e : a.getYearsCount().entrySet()) {
				if (years.contains(e.getKey())) {
					num += e.getValue();
				}
			}
			a.setSelectCount(num);
		}
	}

	private void allYears(Set<String> years) {
		for (int i = 2005; i <= 2009; i++) {
			years.add(String.format("%04d", i));
		}
	}

	private void allMonths(Set<String> months) {
		for (int i = 1; i <= 12; i++) {
			months.add(String.format("%02d", i));
		}
	}

	private void allDays(Set<String> days) {
		for (int i = 1; i <= 31; i++) {
			days.add(String.format("%02d", i));
		}
	}

	private void allHours(Set<String> hours) {
		for (int i = 0; i <= 23; i++) {
			hours.add(String.format("%02d", i));
		}
	}

	public void count(List<FlickrDeWestArea> as, FlickrDeWestAreaDto areaDto) throws Exception {

		System.out.println("#query:" + areaDto.getQueryStrs().size() * 139);
		//		if (strs.size() > 5 * 12 * 31 * 24)
		//			throw new Exception("excceed the maximun #queries!");

		if ( areaDto.getQueryStrs().size() > 0 && areaDto.getQueryLevel() != null) {
			switch (areaDto.getQueryLevel()) {
			case HOUR:
				this.countHours(as, areaDto.getQueryStrs());
				break;
			case DAY:
				this.countDays(as, areaDto.getQueryStrs());
				break;
			case MONTH:
				this.countMonths(as, areaDto.getQueryStrs());
				break;
			case YEAR:
				this.countYears(as, areaDto.getQueryStrs());
				break;
			}
		}
	}

	@SuppressWarnings("unchecked")
	public void parseXmlRequest(String xml, FlickrDeWestAreaDto areaDto) throws JDOMException, IOException, ParseException {
		xml = StringUtil.shortNum2Long(StringUtil.FullMonth2Num(xml));
		SAXBuilder builder = new SAXBuilder();
		Document document = builder.build(new ByteArrayInputStream(xml.getBytes()));
		Element rootElement = document.getRootElement();

		// <screen>
		areaDto.setRadius(Radius.R80000); //default Radius
		Element screenElement = rootElement.getChild("screen");
		if (screenElement != null) {
			// <screen><bounds>((51.02339744960504, 5.565434570312502), (52.14626715707633, 8.377934570312501))</bounds>
			String boundsStr = screenElement.getChildText("bounds");
			if (boundsStr != null && !"".equals(boundsStr.trim())) {
				Pattern boundsPattern = Pattern.compile("\\(\\(([-0-9.]*), ([-0-9.]*)\\), \\(([-0-9.]*), ([-0-9.]*)\\)\\)");
				Matcher boundsMatcher = boundsPattern.matcher(boundsStr);
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
			if (centerStr != null && !"".equals(centerStr.trim())) {
				Pattern centerPattern = Pattern.compile("\\(([-0-9.]*), ([-0-9.]*)\\)");
				Matcher centerMachter = centerPattern.matcher(centerStr);
				if (centerMachter.find()) {
					Point2D center = new Point2D.Double();
					areaDto.setCenter(center);
					center.setLocation(Double.parseDouble(centerMachter.group(2)), Double.parseDouble(centerMachter.group(1)));
				}
			}

			// <screen><zoom>9</zoom>
			String zoomStr = screenElement.getChildText("zoom");
			if (zoomStr != null && !"".equals(zoomStr.trim())) {
				int zoom = Integer.parseInt(zoomStr);
				if (zoom <= 7) {
					areaDto.setRadius(Radius.R80000);
				} else if (zoom <= 8) {
					areaDto.setRadius(Radius.R40000);
				} else if (zoom <= 9) {
					areaDto.setRadius(Radius.R20000);
				} else if (zoom <= 10) {
					areaDto.setRadius(Radius.R10000);
				} else if (zoom >= 11) {
					areaDto.setRadius(Radius.R5000);
				}
				areaDto.setZoom(zoom);
			}
		}

		// <polygon>(51.58830123054393, 6.971684570312502)(51.67184146523792, 7.647343750000002)(51.44644311790073, 7.298527832031252)</polygon>
		String polygonStr = rootElement.getChildText("polygon");
		if (polygonStr != null && !"".equals(polygonStr.trim())) {
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
		if (intervalStr != null && !"".equals(intervalStr.trim())) {
			Pattern intervalPattern = Pattern.compile("([\\d]{2}/[\\d]{2}/[\\d]{4}) - ([\\d]{2}/[\\d]{2}/[\\d]{4})");
			Matcher intervalMachter = intervalPattern.matcher(intervalStr);

			SortedSet<String> queryStrs = new TreeSet<String>();
			areaDto.setQueryStrs(queryStrs);
			SortedSet<String> years4Chart = new TreeSet<String>();
			areaDto.setYears4Chart(years4Chart);

			SimpleDateFormat inputDateFormat = new SimpleDateFormat("dd/MM/yyyy");
			areaDto.setQueryLevel(QueryLevel.DAY);

			if (intervalMachter.find()) {
				Date beginDate = inputDateFormat.parse(intervalMachter.group(1));
				Date endDate = inputDateFormat.parse(intervalMachter.group(2));
				areaDto.setBeginDate(beginDate);
				areaDto.setEndDate(endDate);

				Calendar calendar = Calendar.getInstance();
				calendar.setTime(beginDate);
				Calendar end = Calendar.getInstance();
				end.setTime(endDate);
				while (calendar.getTime().before(end.getTime())) {
					queryStrs.add(FlickrDeWestAreaDao.dayDateFormat.format(calendar.getTime()));
					calendar.add(Calendar.DATE, 1);

					// for TimeSeriesChart
					areaDto.getYears4Chart().add(FlickrDeWestAreaDao.yearDateFormat.format(calendar.getTime()));
				}
			}
		}

		// <selected_days>Sep 08 2010,Sep 10 2010,Oct 14 2010,Oct 19 2010,Sep 24 2010,Sep 22 2005,Sep 09 2005</selected_days>
		String selectedDaysStr = rootElement.getChildText("selected_days");
		if (selectedDaysStr != null && !"".equals(selectedDaysStr.trim())) {
			Pattern selectedDaysPattern = Pattern.compile("([A-Z]{1}[a-z]{2} [\\d]{2} [\\d]{4})");
			Matcher selectedDaysMachter = selectedDaysPattern.matcher(selectedDaysStr);
			SortedSet<Date> selectedDays = new TreeSet<Date>();
			areaDto.setSelectedDays(selectedDays);

			SortedSet<String> queryStrs = new TreeSet<String>();
			areaDto.setQueryStrs(queryStrs);
			SortedSet<String> years4Chart = new TreeSet<String>();
			areaDto.setYears4Chart(years4Chart);

			// day of week in English format
			SimpleDateFormat inputDateFormat = new SimpleDateFormat("MMM dd yyyy", Locale.ENGLISH);
			areaDto.setQueryLevel(QueryLevel.DAY);

			while (selectedDaysMachter.find()) {
				Date selectedDay = inputDateFormat.parse(selectedDaysMachter.group());
				selectedDays.add(selectedDay);

				queryStrs.add(FlickrDeWestAreaDao.dayDateFormat.format(selectedDay));

				// for TimeSeriesChart
				areaDto.getYears4Chart().add(FlickrDeWestAreaDao.yearDateFormat.format(selectedDay));
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
					String year = yearElement.getText();
					if (year != null && !year.trim().equals("")) {
						areaDto.getYears().add(year.trim());
					}
				}
			}

			// <calendar><month>
			Element monthsElement = calendarElement.getChild("months");
			if (monthsElement != null) {
				List<Element> monthElements = monthsElement.getChildren("month");
				for (Element monthElement : monthElements) {
					String month = monthElement.getText();
					if (month != null && !month.trim().equals("")) {
						areaDto.getMonths().add(month.trim());
					}
				}
			}

			// <calendar><days>
			Element daysElement = calendarElement.getChild("days");
			if (daysElement != null) {
				List<Element> dayElements = daysElement.getChildren("day");
				for (Element dayElement : dayElements) {
					String day = dayElement.getText();
					if (day != null && !day.trim().equals("")) {
						areaDto.getDays().add(day.trim());
					}
				}
			}

			// <calendar><hours>
			Element hoursElement = calendarElement.getChild("hours");
			if (hoursElement != null) {
				List<Element> hourElements = hoursElement.getChildren("hour");
				for (Element hourElement : hourElements) {
					String hour = hourElement.getText();
					if (hour != null && !hour.trim().equals("")) {
						areaDto.getHours().add(hour.trim());
					}
				}
			}

			// <calendar><weekdays>
			Element weekdaysElement = calendarElement.getChild("weekdays");
			if (weekdaysElement != null) {
				List<Element> weekdayElements = weekdaysElement.getChildren("weekday");
				for (Element weekdayElement : weekdayElements) {
					String weekday = weekdayElement.getText();
					if (weekday != null && !weekday.trim().equals("")) {
						areaDto.getWeekdays().add(weekday.trim());
					}
				}
			}

			setCalendarQueryStrs(areaDto);
		}

	}

	private void setCalendarQueryStrs(FlickrDeWestAreaDto areaDto) {

		areaDto.setQueryLevel(QueryLevel.HOUR);

		// construct the Query Strings
		SortedSet<String> queryStrs = new TreeSet<String>();
		areaDto.setQueryStrs(queryStrs);

		SortedSet<String> tempYears = new TreeSet<String>(areaDto.getYears());
		SortedSet<String> tempMonths = new TreeSet<String>(areaDto.getMonths());
		SortedSet<String> tempDays = new TreeSet<String>(areaDto.getDays());
		SortedSet<String> tempHours = new TreeSet<String>(areaDto.getHours());

		// for TimeSeriesChart
		areaDto.setYears4Chart(tempYears);

		// complete the options when they are not selected
		if (tempYears.size() == 0) {
			this.allYears(tempYears);
		}
		if (tempMonths.size() == 0) {
			this.allMonths(tempMonths);
		}
		if (tempDays.size() == 0) {
			this.allDays(tempDays);
		}
		if (tempHours.size() == 0) {
			this.allHours(tempHours);
		}

		// day of week in English format
		SimpleDateFormat sdf = new SimpleDateFormat("EEEEE", Locale.ENGLISH);
		Calendar calendar = Calendar.getInstance();

		for (String y : tempYears) {
			for (String m : tempMonths) {
				for (String d : tempDays) {
					for (String h : tempHours) {
//						calendar.set(Integer.parseInt(y), Integer.parseInt(m) - 1, Integer.parseInt(d));
						calendar.set(Calendar.YEAR, Integer.parseInt(y));
						calendar.set(Calendar.MONTH, Integer.parseInt(m) - 1);
						calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(d));
						calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(h));
						// filter out the selected weekdays
						if (areaDto.getWeekdays().size() == 0 || areaDto.getWeekdays().contains(sdf.format(calendar.getTime()))) {
//							queryStrs.add(y + "-" + m + "-" + d + "@" + h);
							queryStrs.add(FlickrDeWestAreaDao.hourDateFormat.format(calendar.getTime()));
						}
					}
				}
			}
		}
	}


	public String createMarkersXml(List<FlickrDeWestArea> as, String file) {
		Document document = new Document();
		Element rootElement = new Element("polygons");
		document.setRootElement(rootElement);
		for (FlickrDeWestArea a : as) {
			String polyColor = "#0000";
			int color = a.getTotalCount() / 30;
			if (color > 255) {
				color = 255;
			}

			Element polygonElement = new Element("polygon");
			rootElement.addContent(polygonElement);
			polygonElement.setAttribute("color", polyColor + StringUtil.byteToHexString((byte) color));
			polygonElement.setAttribute("opacity", "2");

			Element lineElement = new Element("line");
			polygonElement.addContent(lineElement);
			lineElement.setAttribute("color", "#111111");
			lineElement.setAttribute("width", "2");
			lineElement.setAttribute("opacity", "1");

			JGeometry shape = a.getGeom();
			for (int i = 0; i < shape.getOrdinatesArray().length; i++) {
				Element pointElement = new Element("point");
				lineElement.addContent(pointElement);
				pointElement.setAttribute("lng", String.valueOf(shape.getOrdinatesArray()[i++]));
				pointElement.setAttribute("lat", String.valueOf(shape.getOrdinatesArray()[i]));
			}
		}
		XmlUtil.xml2File(document, file, false);
		return XmlUtil.xml2String(document, false);
	}

	public void createBarChart(Map<String, Integer> cs) {
		ChartUtil.createBarChart(cs, "temp/bar.jpg");
	}

	public void createTimeSeriesChart(FlickrDeWestArea a, Set<String> years, OutputStream os) throws ParseException, IOException {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Map<Date, Integer> countsMap = new LinkedHashMap<Date, Integer>();
		for (Map.Entry<String, Integer> e : a.getDaysCount().entrySet()) {
			if (years.contains(e.getKey().substring(0, 4))) {
				countsMap.put(sdf.parse(e.getKey()), e.getValue());
			}
		}

		ChartUtil.createTimeSeriesChart(countsMap, os);
	}

	public String createKml(List<FlickrDeWestArea> as, String filenamePrefix, Radius radius, String remoteBasePath, boolean compress) throws UnsupportedEncodingException {
		String localBasePath = System.getProperty("oraclespatialweb.root");
		if (remoteBasePath == null || "".equals(remoteBasePath)) {
			remoteBasePath = "http://localhost:8080/OracleSpatialWeb/";
		}

		Document document = new Document();
		Namespace namespace = Namespace.getNamespace("http://earth.google.com/kml/2.1");
		Element rootElement = new Element("kml", namespace);

		float scale = (float) (Integer.parseInt(radius.toString()) / 30000.0);
		document.setRootElement(rootElement);

		Element documentElement = new Element("Document", namespace);
		rootElement.addContent(documentElement);

		// GroundOverlay
		for (FlickrDeWestArea a : as) {
			if (a.getTotalCount() != 0) {
				String name = "";
				//			String description = "count: " + String.valueOf(a.getTotalCount());
				String description = "areaid=" + a.getId()
									+ "&total=" + a.getTotalCount()
									+ "&selected=" + a.getSelectCount();

				String groundOverlayColor = "bbffffff"; //transparency

				Element groundOverlayElement = new Element("GroundOverlay", namespace);
				documentElement.addContent(groundOverlayElement);
				Element nameElement = new Element("name", namespace);
				nameElement.addContent(name);
				Element descriptionElement = new Element("description", namespace);
				descriptionElement.addContent(new CDATA(description));
				Element colorElement = new Element("color", namespace);
				colorElement.addContent(groundOverlayColor);
				groundOverlayElement.addContent(nameElement);
				groundOverlayElement.addContent(descriptionElement);
				groundOverlayElement.addContent(colorElement);
				Element iconElement = new Element("Icon", namespace);
				groundOverlayElement.addContent(iconElement);
				Element hrefElement = new Element("href", namespace);
				iconElement.addContent(hrefElement);
				double r = 0;
				String icon = "";

				if (a.getSelectCount() < 100) {
//					r = (double) Math.log10(a.getSelectCount() + 1) / 85.0 * scale;
					icon = remoteBasePath + "images/circle_bl.ico";
				} else if (a.getSelectCount() < 1000) {
//					r = (double) Math.log10(a.getSelectCount() + 1) / 80.0 * scale;
					icon = remoteBasePath + "images/circle_gr.ico";
				} else if (a.getSelectCount() < 10000) {
//					r = (double) Math.log10(a.getSelectCount() + 1) / 70.0 * scale;
					icon = remoteBasePath + "images/circle_lgr.ico";
				} else {
//					r = (double) Math.log10(a.getSelectCount() + 1) / 60.0 * scale;
					icon = remoteBasePath + "images/circle_or.ico";
				}

				r = (double) Math.log10(a.getSelectCount() + 1) / 80.0 * scale;
				if (r > 0.1) {
					r = 0.1;
				}

				hrefElement.addContent(icon);

				Element latLonBoxElement = new Element("LatLonBox", namespace);
				groundOverlayElement.addContent(latLonBoxElement);
				Element northElement = new Element("north", namespace);
				Element southElement = new Element("south", namespace);
				Element eastElement = new Element("east", namespace);
				Element westElement = new Element("west", namespace);
				latLonBoxElement.addContent(northElement);
				latLonBoxElement.addContent(southElement);
				latLonBoxElement.addContent(eastElement);
				latLonBoxElement.addContent(westElement);

				northElement.addContent(Double.toString(a.getCenter().getY() + r * 0.55));
				southElement.addContent(Double.toString(a.getCenter().getY() - r * 0.55));
				eastElement.addContent(Double.toString(a.getCenter().getX() + r));
				westElement.addContent(Double.toString(a.getCenter().getX() - r));
				if (Double.isInfinite(a.getCenter().getY() + r * 0.55)) {
					System.exit(0);
				}
			}
		}

		// Polygon
		for (FlickrDeWestArea a : as) {
			// if(a.getCount()==0||!a.getName().equals("100")) continue;
			String name = "";
			//			String description = "count: " + String.valueOf(a.getTotalCount());
			String description = "areaid=" + a.getId()
								+ "&total=" + a.getTotalCount()
								+ "&selected=" + a.getSelectCount();

			String polyStyleColor = "330000"; //not transparent
			//			String polyStyleColor = "000000"; //transparent
			String polyStyleFill = "1";
			String polyStyleOutline = "1";
			String lineStyleWidth = "1";
			String lineStyleColor = "88ff0000";
			String coordinates = "\n";

			JGeometry shape = a.getGeom();
			for (int i = 0; i < shape.getOrdinatesArray().length; i++) {
				coordinates += shape.getOrdinatesArray()[i] + ", ";
				if (i % 2 == 1) {
					coordinates += "0\n";
				}
			}

			// create kml
			Element placemarkElement = new Element("Placemark", namespace);
			documentElement.addContent(placemarkElement);

			Element nameElement = new Element("name", namespace);
			nameElement.addContent(name);
			Element descriptionElement = new Element("description", namespace);
			descriptionElement.addContent(new CDATA(description));

			Element styleElement = new Element("Style", namespace);
			placemarkElement.setAttribute("id", String.valueOf(a.getId()));
			placemarkElement.addContent(nameElement);
			placemarkElement.addContent(descriptionElement);
			placemarkElement.addContent(styleElement);

			Element polyStyleElement = new Element("PolyStyle", namespace);
			styleElement.addContent(polyStyleElement);

			Element polyColorElement = new Element("color", namespace);
			int color = a.getTotalCount() / 30;
			if (color > 255) {
				color = 255;
			}

			polyColorElement.addContent(polyStyleColor + Integer.toHexString(color));
			Element polyFillElement = new Element("fill", namespace);
			polyFillElement.addContent(polyStyleFill);
			Element polyOutlineElement = new Element("outline", namespace);
			polyOutlineElement.addContent(polyStyleOutline);
			polyStyleElement.addContent(polyColorElement);
			polyStyleElement.addContent(polyFillElement);
			polyStyleElement.addContent(polyOutlineElement);

			Element lineStyleElement = new Element("LineStyle", namespace);
			styleElement.addContent(lineStyleElement);

			Element lindWidthElement = new Element("width", namespace);
			lindWidthElement.addContent(lineStyleWidth);
			Element lineColorElement2 = new Element("color", namespace);
			lineColorElement2.addContent(lineStyleColor);
			lineStyleElement.addContent(lindWidthElement);
			lineStyleElement.addContent(lineColorElement2);

			Element multiGeometryElement = new Element("MultiGeometry", namespace);
			Element polygonElement = new Element("Polygon", namespace);
			Element outerBoundaryIsElement = new Element("outerBoundaryIs", namespace);
			Element linearRingElement = new Element("LinearRing", namespace);
			Element coordinatesElement = new Element("coordinates", namespace);
			placemarkElement.addContent(multiGeometryElement);
			multiGeometryElement.addContent(polygonElement);
			polygonElement.addContent(outerBoundaryIsElement);
			outerBoundaryIsElement.addContent(linearRingElement);
			linearRingElement.addContent(coordinatesElement);
			coordinatesElement.addContent(coordinates);
		}
		if(compress == true){
			XmlUtil.xml2Kmz(document, localBasePath + filenamePrefix, true);
		}else {
			XmlUtil.xml2File(document, localBasePath + filenamePrefix + ".kml", false);
		}
		return XmlUtil.xml2String(document, false);
		// return xml2String(document).replaceAll("\r\n", " ");
	}



}
