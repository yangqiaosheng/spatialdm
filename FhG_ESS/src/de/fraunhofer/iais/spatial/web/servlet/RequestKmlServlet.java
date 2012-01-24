package de.fraunhofer.iais.spatial.web.servlet;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import main.NormalTrafficPredictor;
import main.TrafficSimulator;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import de.fraunhofer.iais.spatial.dto.AreaDto;
import de.fraunhofer.iais.spatial.dto.ModelDto;
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

	public static final String kmlPath = "kml/";

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		// web base path for local operation
//		String localBasePath = getServletContext().getRealPath("/");
		// web base path for remote access
		String remoteBasePath = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath() + "/";
		//		String remoteBasePath = "http://kd-photomap.iais.fraunhofer.de/OracleSpatialWeb/";
		response.setContentType("text/xml; charset=UTF-8");
		// response.setContentType("application/vnd.google-earth.kml+xml");

		// Prevents caching
		response.setHeader("Cache-Control", "no-store"); // HTTP1.1
//		response.setHeader("Content-Type", "text/html; charset=UTF-8");
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


				if (StringUtils.isBlank(areaDto.getModelNamel())) {
					String modelsXml = FileUtils.readFileToString(new File(this.getClass().getResource("/../../models.xml").getPath()), "UTF-8");
					document = XmlUtil.string2Xml(modelsXml, "UTF-8");
				} else {
					if (StringUtils.equals(areaDto.getModelNamel(), "NimesMatchPed1")) {
						response.sendRedirect("http://kd-photomap.iais.fraunhofer.de/prediction_ped/RequestKml?xml=" + xml);
						return;
					}
					Map<Long, Map<String, Integer>> areaEvents = ModelManager.generateEvents(areaDto);
					System.out.println(ToStringBuilder.reflectionToString(areaDto));
					setCalendarQueryStrs(areaDto);

					List<Area> areas = Lists.newArrayList();
					CsvImporter.importAreas(areas, areaDto.getBoundaryRect(), "data/places.xml");
					CsvImporter.initAreas(areaEvents, areas);

					List<AreaResult> areaResults = ModelManager.createAreaResults(areas);
					ModelManager.countSelected(areaResults, areaDto);
					ModelManager.calculateHistograms(areaResults, areaDto);
					ModelManager.buildKmlFile(areaResults, kmlPath + filenamePrefix, areaDto.getRadius(), areaDto.getTransfromVector(), remoteBasePath, false);

					addAveElement(rootElement, areaResults);

					Element urlElement = new Element("url");

					rootElement.addContent(urlElement);
					urlElement.setText(remoteBasePath + kmlPath + filenamePrefix + ".kml");
					messageElement.setText("SUCCESS");
				}
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

		out.print(XmlUtil.xml2String(document, false));
		out.flush();
		out.close();
		System.gc();
	}

	private void addAveElement(Element rootElement, List<AreaResult> areaResults) {
		int allMin = Integer.MAX_VALUE;
		int allMax = Integer.MIN_VALUE;
		int allSum = 0;
		int allNum = 0;
		for (AreaResult areaResult : areaResults) {
			Map<Integer, Integer> hoursCount = areaResult.getHistograms().getHours();
			int min = Integer.MAX_VALUE;
			int max = Integer.MIN_VALUE;
			int sum = 0;
			int num = 0;
			for (int houtCount : hoursCount.values()) {
				if (houtCount > max) {
					max = houtCount;
				}

				if (houtCount > 0) {
					if (houtCount < min) {
						min = houtCount;
					}
					num++;
					sum += houtCount;
				}
			}
			int avg = sum / num;

			if (avg > allMax) {
				allMax = avg;
			}

			if (avg > 0) {
				if (avg < allMin) {
					allMin = avg;
				}
				allNum++;
				allSum += avg;
			}
		}
		int allAvg = allSum / allNum;

		Element avgElement = new Element("avg");
		rootElement.addContent(avgElement);
		avgElement.setText(String.valueOf(allAvg));
		Element minElement = new Element("min");
		rootElement.addContent(minElement);
		minElement.setText(String.valueOf(allMin));
		Element maxElement = new Element("max");
		rootElement.addContent(maxElement);
		maxElement.setText(String.valueOf(allMax));
	}

	@Deprecated
	public List<ModelDto> parseXmlRequest(String xml) throws JDOMException, IOException, ParseException {
		Document document = XmlUtil.string2Xml(xml, "UTF-8");
		Element rootElement = document.getRootElement();

		List<ModelDto> modelDtos = new ArrayList<ModelDto>();

//		<Models>
		Element modelsElement = rootElement.getChild("Models");
		if (modelsElement != null) {
			// <HDAModel>
			List<Element> modelElements = modelsElement.getChildren("HDAModel");
			for (Element modelElement : modelElements) {
				ModelDto modelDto = new ModelDto();
				modelDtos.add(modelDto);
				modelDto.setModelType(modelElement.getAttributeValue("type"));
				modelDto.setModelNamel(modelElement.getAttributeValue("name"));
				modelDto.setTitle(modelElement.getChildText("title"));
				modelDto.setCall(modelElement.getChildText("call"));
				modelDto.setValid(modelElement.getChildText("valid"));

				Pattern positionPattern = Pattern.compile("\\(([-0-9.]*), ([-0-9.]*)\\)");
				Matcher positionMachter = positionPattern.matcher(modelElement.getChildText("position"));

				if (positionMachter.find()) {
					Point2D position = new Point2D.Double();
					modelDto.setPosition(position);
					position.setLocation(Double.parseDouble(positionMachter.group(2)), Double.parseDouble(positionMachter.group(1)));
				}
			}
		}
		return modelDtos;
	}

	public void parseXmlRequest(String xml, AreaDto areaDto) throws JDOMException, IOException, ParseException {
		xml = StringUtil.shortNum2Long(StringUtil.FullMonth2Num(xml));
		SAXBuilder builder = new SAXBuilder();
		Document document = builder.build(new ByteArrayInputStream(xml.getBytes("UTF-8")));
		Element rootElement = document.getRootElement();

		// <model>
		Element modelElement = rootElement.getChild("model");
		if (modelElement != null) {
			areaDto.setModelNamel(StringUtils.trim(modelElement.getText()));
		}

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

			// <calendar><minutes>
			Element minutesElement = calendarElement.getChild("minutes");
			if (minutesElement != null) {
				List<Element> minutesElements = minutesElement.getChildren("minute");
				for (Element minuteElement : minutesElements) {
					String minute = minuteElement.getText().trim();
					if (StringUtils.isNotBlank(minute)) {
						areaDto.getMinutes().add(minute);
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
	}

	private void setCalendarQueryStrs(AreaDto areaDto) {

		// construct the Query Strings
		Set<String> queryStrs = areaDto.getQueryStrs();

		SortedSet<String> tempYears = new TreeSet<String>(areaDto.getYears());
		SortedSet<String> tempMonths = new TreeSet<String>(areaDto.getMonths());
		SortedSet<String> tempDays = new TreeSet<String>(areaDto.getDays());
		SortedSet<String> tempHours = new TreeSet<String>(areaDto.getHours());
		SortedSet<String> tempMinutes = new TreeSet<String>(areaDto.getMinutes());


		if (areaDto.getQueryLevel() == Level.MINUTE) {
			tempMinutes = new TreeSet<String>(DateUtil.allMinuteIntStrs(areaDto.getMinuteInterval()));
		}

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
		SimpleDateFormat minuteDateFormat = new SimpleDateFormat(ModelManager.minuteDateFormatStr);

		for (String y : tempYears) {
			for (String m : tempMonths) {
				for (String d : tempDays) {
					for (String h : tempHours) {
						if(areaDto.getQueryLevel() == Level.HOUR){
							calendar.set(Calendar.YEAR, Integer.parseInt(y));
							calendar.set(Calendar.MONTH, Integer.parseInt(m) - 1);
							calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(d));
							calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(h));
							calendar.set(Calendar.MINUTE,  0);
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
						} else if(areaDto.getQueryLevel() == Level.MINUTE){
							for (String mi : tempMinutes) {
								if (!DateUtil.allMinuteIntStrs(areaDto.getMinuteInterval()).contains(mi)) {
									throw new IllegalArgumentException("Only the following minute parameters are acceptable:" + DateUtil.allMinuteIntStrs(areaDto.getMinuteInterval()));
								}
								calendar.set(Calendar.YEAR, Integer.parseInt(y));
								calendar.set(Calendar.MONTH, Integer.parseInt(m) - 1);
								calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(d));
								calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(h));
								calendar.set(Calendar.MINUTE,  Integer.parseInt(mi));
								calendar.set(Calendar.SECOND, 0);

								try {
									// filter out the selected weekdays
									if (areaDto.getWeekdays().size() == 0 || areaDto.getWeekdays().contains(sdf.format(calendar.getTime()))) {
										//queryStrs.add(y + "-" + m + "-" + d + "@" + h);
										queryStrs.add(minuteDateFormat.format(calendar.getTime()));
									}
								} catch (IllegalArgumentException e) {
									// omit the wrong date
								}
							}
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

	/**
	 * Initialization of the servlet. <br>
	 *
	 * @throws ServletException
	 *             - if an error occurs
	 */
	@Override
	public void init() throws ServletException {
	}

}
