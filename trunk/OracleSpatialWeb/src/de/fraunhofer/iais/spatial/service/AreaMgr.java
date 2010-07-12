package de.fraunhofer.iais.spatial.service;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import oracle.spatial.geometry.JGeometry;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

import de.fraunhofer.iais.spatial.dao.AreaDao;
import de.fraunhofer.iais.spatial.entity.Area;
import de.fraunhofer.iais.spatial.util.StringUtil;

public class AreaMgr {

	private AreaDao areaDao = null;

	public AreaDao getAreaDao() {
		return areaDao;
	}

	public void setAreaDao(AreaDao areaDao) {
		this.areaDao = areaDao;
	}

	public List<Area> getAllAreas() {
		return areaDao.getAllAreas();
	}

	public Area getAreaById(String id) {
		return areaDao.getAreaById(id);
	}

	public List<Area> getAreasByPoint(double x, double y) {
		return areaDao.getAreasByPoint(x, y);
	}

	public List<Area> getAreasByRect(double x1, double y1, double x2, double y2) {
		return areaDao.getAreasByRect(x1, y1, x2, y2);
	}

	public void countPersons(List<Area> as, List<String> persons) {
		for (Area a : as) {
			int count = 0;
			for (String p : persons) {
				count += areaDao.getPersonCount(a.getId(), p);
			}
			a.setCount(count);
		}
	}

	public void countHours(List<Area> as, Set<String> hours) {
		for (Area a : as) {
			a.setCount(areaDao.getHourCount(a.getId(), hours));
		}
	}

	public void countDays(List<Area> as, Set<String> days) {
		for (Area a : as) {
			a.setCount(areaDao.getDayCount(a.getId(), days));
		}
	}

	public void countMonths(List<Area> as, Set<String> months) {
		for (Area a : as) {
			a.setCount(areaDao.getMonthCount(a.getId(), months));
		}
	}

	public void countYears(List<Area> as, Set<String> years) {
		for (Area a : as) {
			a.setCount(areaDao.getYearCount(a.getId(), years));
		}
	}

	public void countTotal(List<Area> as) {
		for (Area a : as) {
			a.setTotalCount(areaDao.getTotalCount(a.getId()));
		}
	}
	
	public void allYears(List<String> years){
		for (int i = 2005; i <= 2009; i++) {
			years.add(String.format("%04d", i)); 
		}
	}
	
	public void allMonths(List<String> months){
		for (int i = 1; i <= 12; i++) {
			months.add(String.format("%02d", i)); 
		}
	}
	
	public void allDays(List<String> days){
		for (int i = 1; i <= 31; i++) {
			days.add(String.format("%02d", i)); 
		}
	}
	
	public void allHours(List<String> hours){
		for (int i = 0; i <= 23; i++) {
			hours.add(String.format("%02d", i)); 
		}
	}

	public void count(List<Area> as, List<String> years, List<String> months,
			List<String> days, List<String> hours, Set<String> weekdays) throws Exception {
		Set<String> strs = new HashSet<String>();
		
		// complete the options when they are not selected
		if (years.size() == 0)
			this.allYears(years);
		if (months.size() == 0)
			this.allMonths(months);
		if (days.size() == 0)
			this.allDays(days);
		if (hours.size() == 0)
			this.allHours(hours);
		
		Calendar calendar = Calendar.getInstance();		
		// day of week in English format
		SimpleDateFormat sdf = new SimpleDateFormat("EEEEE", Locale.ENGLISH); 
		
		for (String y : years) {
			for (String m : months) {
				for (String d : days) {
					for (String h : hours) {
						calendar.set(Integer.parseInt(y), Integer.parseInt(m)-1, Integer.parseInt(d));
						// filter out the selected weekdays
						if (weekdays.size() == 0 || weekdays.contains(sdf.format(calendar.getTime()))){
							strs.add(y + "-" + m + "-" + d + "@" + h);			
//							System.out.println(calendar.getTime() + ":" + sdf.format(calendar.getTime()));
						}
						
					}
				}
			}
		}
		
		count(as, strs, 'h');
	}

	public void count(List<Area> as, Set<String> strs, char level) throws Exception {
		System.out.println("#query:" + strs.size() * 139);
		this.countTotal(as);

		if (strs.size() > 5 * 12 * 31 * 24) {
			throw new Exception("excceed the maximun #queries!");
		}
			
		
		if (strs.size() > 0 && level != '0') {
			switch (level) {
			case 'h':
				this.countHours(as, strs);
				break;
			case 'd':
				this.countDays(as, strs);
				break;
			case 'm':
				this.countMonths(as, strs);
				break;
			case 'y':
				this.countYears(as, strs);
				break;
			}
		}
	}

	@SuppressWarnings("unchecked")
	public void parseXmlRequest(List<Area> as, String xml, List<String> years,
			List<String> months, List<String> days, List<String> hours, Set<String> weekdays)
			throws Exception {
		xml = StringUtil.ShortNum2Long(StringUtil.FullMonth2Num(xml));
		SAXBuilder builder = new SAXBuilder();

		Document document = builder.build(new ByteArrayInputStream(xml
				.getBytes()));
		Element rootElement = document.getRootElement();
		// <years>
		List<Element> yearsElements = rootElement.getChildren("years");
		if (yearsElements != null && yearsElements.size() == 1) {
			List<Element> yearElements = yearsElements.get(0).getChildren("year");
			for (Element yearElement : yearElements) {
				String year = yearElement.getText();
				if (year != null && !year.trim().equals("")) {
					System.out.println("year:" + year.trim());
					years.add(year.trim());
				}
			}
		}
		// <month>
		List<Element> monthsElements = rootElement.getChildren("months");
		if (monthsElements != null && monthsElements.size() == 1) {
			List<Element> monthElements = monthsElements.get(0).getChildren("month");
			for (Element monthElement : monthElements) {
				String month = monthElement.getText();
				if (month != null && !month.trim().equals("")) {
					System.out.println("month:" + month.trim());
					months.add(month.trim());
				}
			}
		}
		// <days>
		List<Element> daysElements = rootElement.getChildren("days");
		if (daysElements != null && daysElements.size() == 1) {
			List<Element> dayElements = daysElements.get(0).getChildren("day");
			for (Element dayElement : dayElements) {
				String day = dayElement.getText();
				if (day != null && !day.trim().equals("")) {
					System.out.println("day:" + day.trim());
					days.add(day.trim());
				}
			}
		}

		// <hours>
		List<Element> hoursElements = rootElement.getChildren("hours");
		if (hoursElements != null && hoursElements.size() == 1) {
			List<Element> hourElements = hoursElements.get(0).getChildren(
					"hour");
			for (Element hourElement : hourElements) {
				String hour = hourElement.getText();
				if (hour != null && !hour.trim().equals("")) {
					System.out.println("hour:" + hour.trim());
					hours.add(hour.trim());
				}
			}
		}
		
		// <weekday>
		List<Element> weekdaysElements = rootElement.getChildren("weekdays");
		if (weekdaysElements != null && weekdaysElements.size() == 1) {
			List<Element> weekdayElements = weekdaysElements.get(0).getChildren("weekday");
			for (Element weekdayElement : weekdayElements) {
				String weekday = weekdayElement.getText();
				if (weekday != null && !weekday.trim().equals("")) {
					System.out.println("weekday:" + weekday.trim());
					weekdays.add(weekday.trim());
				}
			}
		}

		count(as, years, months, days, hours, weekdays);
	}

	@SuppressWarnings("unchecked")
	public void parseXmlRequest2(List<Area> as, String xml)
			throws Exception {
		xml = StringUtil.ShortNum2Long(StringUtil.FullMonth2Num(xml));
		char level = '0';
		Set<String> strs = new HashSet<String>();
		SAXBuilder builder = new SAXBuilder();
		Document document = builder.build(new ByteArrayInputStream(xml
				.getBytes()));
		Element rootElement = document.getRootElement();
		// <years>
		List<Element> yearElements = rootElement.getChildren("year");
		List<Element> monthElements = rootElement.getChildren("month");
		List<Element> dayElements = rootElement.getChildren("day");
		List<Element> hourElements = rootElement.getChildren("hour");
	
		if (hourElements != null && hourElements.size() > 0) {
			level = 'h';
			for (int i = 0; i < hourElements.size(); i++) {
				strs.add(yearElements.get(i).getValue() + "-"
						+ monthElements.get(i).getValue() + "-"
						+ dayElements.get(i).getValue() + "@"
						+ hourElements.get(i).getValue());
			}
		} else if (dayElements != null && dayElements.size() > 0) {
			level = 'd';
			for (int i = 0; i < dayElements.size(); i++) {
				strs.add(yearElements.get(i).getValue() + "-"
						+ monthElements.get(i).getValue() + "-"
						+ dayElements.get(i).getValue());
			}
		} else if (monthElements != null && monthElements.size() > 0) {
			level = 'm';
			for (int i = 0; i < monthElements.size(); i++) {
				strs.add(yearElements.get(i).getValue() + "-"
						+ monthElements.get(i).getValue());
			}
		} else if (yearElements != null && yearElements.size() > 0) {
			level = 'y';
			for (int i = 0; i < yearElements.size(); i++) {
				strs.add(yearElements.get(i).getValue());
			}
		}
	
		System.out.println("level:" + level);
		System.out.println(strs.size());
		count(as, strs, level);
	}

	@SuppressWarnings("unchecked")
	public void createBarChart(Map<String, Integer> cs) {
		System.out.println("chart");
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();

		Iterator it = cs.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pairs = (Map.Entry) it.next();
			dataset.addValue((Integer) pairs.getValue(), (String) pairs
					.getKey(), "Category 1");
		}

		JFreeChart chart = ChartFactory.createBarChart(
//		JFreeChart chart = ChartFactory.createLineChart3D(
				// JFreeChart chart = ChartFactory.createPieChart3D(

				"Chart", // Title
				"Month", // X Label
				"Amount of Photo", // Y Label
				dataset, // dataset
				PlotOrientation.VERTICAL, // Plot Orientation
				false, // Print the Case
				false, // Generate the tool
				false // Generate the url
				);

		FileOutputStream fos_jpg = null;

		try {
			fos_jpg = new FileOutputStream("temp/chart.jpg");
			ChartUtilities.writeChartAsJPEG(fos_jpg, 0.8f, chart, 800, 600,
					null);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				fos_jpg.close();
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
		}

	}
	
	public String createMarkersXml(List<Area> as, String file) {
		Document document = new Document();
		Element rootElement = new Element("polygons");
		document.setRootElement(rootElement);
		for (Area a : as) {			
			String polyColor = "#0000";
			int color = a.getTotalCount() / 30;
			if (color > 255)
				color = 255;
			
			Element polygonElement = new Element("polygon");
			rootElement.addContent(polygonElement);
			polygonElement.setAttribute("color", polyColor + StringUtil.byteToHexString((byte)color));
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
		xml2File(document, file);
		return xml2String(document);
	}

	public String createKml(List<Area> as, String file) {
		String remoteBasePath = "http://kd-photomap.iais.fraunhofer.de/OracleSpatialWeb/";
		Document document = new Document();
		Namespace namespace = Namespace
				.getNamespace("http://earth.google.com/kml/2.1");
		Element rootElement = new Element("kml", namespace);

		document.setRootElement(rootElement);

		Element documentElement = new Element("Document", namespace);
		rootElement.addContent(documentElement);

		// GroundOverlay
		for (Area a : as) {			
			if (a.getCount() != 0) {
				String name = "total:" + a.getTotalCount();
				String description = "select:" + String.valueOf(a.getCount());
				
				Element groundOverlayElement = new Element("GroundOverlay",
						namespace);
				documentElement.addContent(groundOverlayElement);
				Element nameElement = new Element("name", namespace);
				nameElement.addContent(name);
				Element descriptionElement = new Element("description", namespace);
				descriptionElement.addContent(description);				
				groundOverlayElement.addContent(nameElement);
				groundOverlayElement.addContent(descriptionElement);
				Element iconElement = new Element("Icon", namespace);
				groundOverlayElement.addContent(iconElement);				
				Element hrefElement = new Element("href", namespace);
				iconElement.addContent(hrefElement);
				double r = 0;
				String icon = "";

				if (a.getCount() < 100) {
					r = (double) Math.log10(a.getCount()) / 85.0;
					icon = remoteBasePath + "images/circle_bl.ico";
				} else if (a.getCount() < 1000) {
					r = (double) Math.log10(a.getCount()) / 80.0;
					icon = remoteBasePath + "images/circle_gr.ico";
				} else if (a.getCount() < 10000) {
					r = (double) Math.log10(a.getCount()) / 70.0;
					icon = remoteBasePath + "images/circle_lgr.ico";
				} else {
					r = (double) Math.log10(a.getCount()) / 60.0;
					icon = remoteBasePath + "images/circle_or.ico";
					if (r > 0.1)
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

				northElement.addContent(Double.toString(a.getCenter().getY()
						+ r * 0.7));
				southElement.addContent(Double.toString(a.getCenter().getY()
						- r * 0.7));
				eastElement.addContent(Double
						.toString(a.getCenter().getX() + r));
				westElement.addContent(Double
						.toString(a.getCenter().getX() - r));
			}
		}

		// Polygon
		for (Area a : as) {
			// if(a.getCount()==0||!a.getName().equals("100")) continue;
			String name = "total:" + a.getTotalCount();
			String description = "select:" + String.valueOf(a.getCount());
//			String polyStyleColor = "440000";	   	//not transparent
			String polyStyleColor = "000000"; 	//transparent
			String polyStyleFill = "1";
			String polyStyleOutline = "1";
			String lineStyleWidth = "1";
			String lineStyleColor = "88ff0000";
			String coordinates = "\n";

			JGeometry shape = a.getGeom();
			for (int i = 0; i < shape.getOrdinatesArray().length; i++) {
				coordinates += shape.getOrdinatesArray()[i] + ", ";
				if (i % 2 == 1)
					coordinates += "0\n";
			}

			// create kml
			Element placemarkElement = new Element("Placemark", namespace);
			documentElement.addContent(placemarkElement);

			Element nameElement = new Element("name", namespace);
			nameElement.addContent(name);
			Element descriptionElement = new Element("description", namespace);
			descriptionElement.addContent(description);
			Element styleElement = new Element("Style", namespace);
			placemarkElement.addContent(nameElement);
			placemarkElement.addContent(descriptionElement);
			placemarkElement.addContent(styleElement);
			

			Element polyStyleElement = new Element("PolyStyle", namespace);
			styleElement.addContent(polyStyleElement);

			Element polyColorElement = new Element("color", namespace);
			int color = a.getTotalCount() / 30;
			if (color > 255)
				color = 255;

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

			Element multiGeometryElement = new Element("MultiGeometry",
					namespace);
			Element polygonElement = new Element("Polygon", namespace);
			Element outerBoundaryIsElement = new Element("outerBoundaryIs",
					namespace);
			Element linearRingElement = new Element("LinearRing", namespace);
			Element coordinatesElement = new Element("coordinates", namespace);
			placemarkElement.addContent(multiGeometryElement);
			multiGeometryElement.addContent(polygonElement);
			polygonElement.addContent(outerBoundaryIsElement);
			outerBoundaryIsElement.addContent(linearRingElement);
			linearRingElement.addContent(coordinatesElement);
			coordinatesElement.addContent(coordinates);
		}
		xml2File(document, file);
		return xml2String(document);
		// return xml2String(document).replaceAll("\r\n", " ");
	}

	private static String xml2String(Document document) {
		XMLOutputter xmlOutputter = new XMLOutputter();
		xmlOutputter.setFormat(Format.getPrettyFormat());
		return xmlOutputter.outputString(document);
	}

	private static void xml2File(Document document, String url) {
		XMLOutputter xmlOutputter = new XMLOutputter();
		xmlOutputter.setFormat(Format.getPrettyFormat());
		FileOutputStream o = null;
		try {
			o = new FileOutputStream(url);
			xmlOutputter.output(document, o);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (o != null)
					o.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
