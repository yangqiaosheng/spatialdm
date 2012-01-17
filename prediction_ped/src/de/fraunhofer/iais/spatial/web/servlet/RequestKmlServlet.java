package de.fraunhofer.iais.spatial.web.servlet;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.math.NumberUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import de.fraunhofer.iais.spatial.dto.AreaDto;
import de.fraunhofer.iais.spatial.dto.AreaDto.Level;
import de.fraunhofer.iais.spatial.entity.Area;
import de.fraunhofer.iais.spatial.entity.AreaResult;
import de.fraunhofer.iais.spatial.service.CsvImporter;
import de.fraunhofer.iais.spatial.service.ModelManager;
import de.fraunhofer.iais.spatial.util.DateUtil;
import de.fraunhofer.iais.spatial.util.StringUtil;
import de.fraunhofer.iais.spatial.util.XmlUtil;

public class RequestKmlServlet extends HttpServlet {
	/**
	* Logger for this class
	*/
	private static final Logger logger = LoggerFactory.getLogger(RequestKmlServlet.class);

	private static final long serialVersionUID = -6814809670117597713L;

	// "/srv/tomcat6/webapps/OracleSpatialWeb/kml/";
	public static final String kmlPath = "kml/";

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		if (MapUtils.isEmpty(request.getParameterMap())) {
			response.sendRedirect("RequestKmlDemo_ped.jsp");
			return;
		}

		// web base path for local operation
		String localBasePath = System.getProperty("prediction_ped.root");
		// web base path for remote access
		String remoteBasePath = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath() + "/";

		response.setContentType("text/xml");
		// response.setContentType("application/vnd.google-earth.kml+xml");

		// Prevents caching
		response.setHeader("Cache-Control", "no-store"); // HTTP1.1
		response.setHeader("Pragma", "no-cache"); // HTTP1.0
		response.setDateHeader("Expires", 0); // proxy server

		String xml = request.getParameter("xml");

		PrintWriter out = response.getWriter();

		Document document = new Document();
		Element rootElement = new Element("response");
		document.setRootElement(rootElement);
		Element messageElement = new Element("message");
		rootElement.addContent(messageElement);

		logger.trace("doGet(HttpServletRequest, HttpServletResponse) - xml:" + xml); //$NON-NLS-1$
		if (StringUtils.isEmpty(xml)) {
			messageElement.setText("ERROR: 'xml' parameter is missing!");
		} else {

			try {
				String filenamePrefix = StringUtil.genId();

				AreaDto areaDto = new AreaDto();
				parseXmlRequest(StringUtil.FullMonth2Num(xml), areaDto);
				areaDto.setQueryLevel(Level.HOUR);
				areaDto.setRadius("100");

				System.out.println(ToStringBuilder.reflectionToString(areaDto));
				System.out.println(localBasePath);
				setCalendarQueryStrs(areaDto);

				Map<Long, Map<String, Integer>> areaEvents = new TreeMap<Long, Map<String, Integer>>();
				CsvImporter.importAreaEvents(areaEvents, localBasePath + "data/ped_presence_predicted_2011_08_05.csv");

				List<Area> areas = Lists.newArrayList();
				CsvImporter.importAreas(areas, areaDto.getBoundaryRect(), localBasePath + "data/ped.xml");
				CsvImporter.initAreas(areaEvents, areas);

				List<AreaResult> areaResults = ModelManager.createAreaResults(areas);
				ModelManager.countSelected(areaResults, areaDto);
				ModelManager.calculateHistograms(areaResults, areaDto);
				ModelManager.buildKmlFile(areaResults, kmlPath + filenamePrefix, areaDto.getRadius(), areaDto.getTransfromVector(), remoteBasePath, false);

				Element urlElement = new Element("url");
				rootElement.addContent(urlElement);
				urlElement.setText(remoteBasePath + kmlPath + filenamePrefix + ".kml");
				messageElement.setText("SUCCESS");
			} catch (JDOMException e) {
				logger.error("doGet(HttpServletRequest, HttpServletResponse)", e); //$NON-NLS-1$
				messageElement.setText("ERROR: wrong xml format!");
//				rootElement.addContent(new Element("exceptions").setText(StringUtil.printStackTrace2String(e)));
				rootElement.addContent(new Element("description").setText(e.getMessage()));
			} catch (ParseException e) {
				logger.error("doGet(HttpServletRequest, HttpServletResponse)", e); //$NON-NLS-1$
				messageElement.setText("ERROR: wrong input parameter!");
//				rootElement.addContent(new Element("exceptions").setText(StringUtil.printStackTrace2String(e)));
				rootElement.addContent(new Element("description").setText(e.getMessage()));
			} catch (RuntimeException e) {
				logger.error("doGet(HttpServletRequest, HttpServletResponse)", e); //$NON-NLS-1$
				messageElement.setText("Runtime ERROR");
//				rootElement.addContent(new Element("exceptions").setText(StringUtil.printStackTrace2String(e)));
				rootElement.addContent(new Element("description").setText(e.getMessage()));
			} catch (Exception e) {
				logger.error("doGet(HttpServletRequest, HttpServletResponse)", e); //$NON-NLS-1$
				messageElement.setText("SYSTEM ERROR!");
				rootElement.addContent(new Element("exceptions").setText(StringUtil.printStackTrace2String(e)));
				rootElement.addContent(new Element("description").setText(e.getMessage()));
			}
		}

		out.print(XmlUtil.xml2String(document, true));
		out.flush();
		out.close();
		System.gc();
	}

	public void parseXmlRequest(String xml, AreaDto areaDto) throws JDOMException, IOException, ParseException {
		xml = StringUtil.shortNum2Long(StringUtil.FullMonth2Num(xml));
		SAXBuilder builder = new SAXBuilder();
		Document document = builder.build(new ByteArrayInputStream(xml.getBytes("UTF-8")));
		Element rootElement = document.getRootElement();

		// <screen>
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
						if (!DateUtil.allYearIntStrs.contains(year)) {
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
						if (!DateUtil.allMonthIntStrs.contains(month)) {
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
						if (!DateUtil.allDayIntStrs.contains(day)) {
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
						if (!DateUtil.allHourIntStrs.contains(hour)) {
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
						if (!DateUtil.allWeekdayFullStrs.contains(weekday)) {
							throw new IllegalArgumentException("wrong string of weekday:" + weekday);
						}
						areaDto.getWeekdays().add(weekday);
					}
				}
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
	}

	private void setCalendarQueryStrs(AreaDto areaDto) {

		// construct the Query Strings
		Set<String> queryStrs = areaDto.getQueryStrs();

		SortedSet<String> tempYears = new TreeSet<String>(areaDto.getYears());
		SortedSet<String> tempMonths = new TreeSet<String>(areaDto.getMonths());
		SortedSet<String> tempDays = new TreeSet<String>(areaDto.getDays());
		SortedSet<String> tempHours = new TreeSet<String>(areaDto.getHours());

		/*// complete the options when they are not selected
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
		}*/

		// day of week in English format
		SimpleDateFormat sdf = new SimpleDateFormat("EEEEE", Locale.ENGLISH);
		Calendar calendar = Calendar.getInstance();
		calendar.setLenient(false);

		SimpleDateFormat hourDateFormat = new SimpleDateFormat(ModelManager.hourDateFormatStr);

		for (String y : tempYears) {
			for (String m : tempMonths) {
				for (String d : tempDays) {
					for (String h : tempHours) {
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

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

}
