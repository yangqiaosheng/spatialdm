package de.fraunhofer.iais.spatial.service;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import oracle.spatial.geometry.JGeometry;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
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
import de.fraunhofer.iais.spatial.dao.ibatis.AreaDaoIbatis;
import de.fraunhofer.iais.spatial.dao.jdbc.AreaDaoJdbc;
import de.fraunhofer.iais.spatial.entity.Area;

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
			int count = 0;
			count = areaDao.getHourCount(a.getId(), hours);
			a.setCount(count);
		}
	}

	public void countDays(List<Area> as, Set<String> days) {
		for (Area a : as) {
			int count = 0;
			count = areaDao.getDayCount(a.getId(), days);
			a.setCount(count);
		}
	}

	public void countMonths(List<Area> as, Set<String> months) {
		for (Area a : as) {
			int count = 0;
			count = areaDao.getMonthCount(a.getId(), months);
			a.setCount(count);
		}
	}

	public void countYears(List<Area> as, Set<String> years) {
		for (Area a : as) {
			int count = 0;
			count = areaDao.getYearCount(a.getId(), years);
			a.setCount(count);
		}
	}

	public void countTotal(List<Area> as) {
		for (Area a : as) {
			a.setTotalCount(areaDao.getTotalCount(a.getId()));
		}
	}

	public void count(List<Area> as, List<String> years, List<String> months, List<String> days, List<String> hours) {
		char level = '0';
		Set<String> strs = new HashSet<String>();
		if (years.size() > 0) {
			level = 'y';
			for (String y : years) {
				if (months.size() > 0) {
					level = 'm';
					for (String m : months) {
						if (days.size() > 0) {
							level = 'd';
							for (String d : days) {
								if (hours.size() > 0) {
									level = 'h';
									for (String h : hours) {
										strs.add(y + "-" + m + "-" + d + "@" + h);
									}
								} else {
									strs.add(y + "-" + m + "-" + d);
								}
							}
						} else {
							strs.add(y + "-" + m);
						}
					}
				} else {
					strs.add(y);
				}
			}
		}

		count(as, strs, level);
	}

	public void count(List<Area> as, Set<String> strs, char level) {
		System.out.println("#query:"+strs.size()*139);
		this.countTotal(as);

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

	public void parseXmlRequest2(List<Area> as, String xml) throws JDOMException, IOException {
		char level = '0';
		Set<String> strs = new HashSet<String>();
		SAXBuilder builder = new SAXBuilder();
		Document document = builder.build(new ByteArrayInputStream(xml.getBytes()));
		Element rootElement = document.getRootElement();
		// <years>
		List<Element> yearElements = rootElement.getChildren("year");
		List<Element> monthElements = rootElement.getChildren("month");
		List<Element> dayElements = rootElement.getChildren("day");
		List<Element> hourElements = rootElement.getChildren("hour");

		if (hourElements != null && hourElements.size() > 0) {
			level = 'h';
			for (int i = 0; i < hourElements.size(); i++) {
				strs.add(yearElements.get(i).getValue() + "-" +
						monthElements.get(i).getValue() + "-" +
						dayElements.get(i).getValue() + "@" +
						hourElements.get(i).getValue());
			}
		} else if (dayElements != null && dayElements.size() > 0) {
			level = 'd';
			for (int i = 0; i < dayElements.size(); i++) {
				strs.add(yearElements.get(i).getValue() + "-" +
						monthElements.get(i).getValue() + "-" +
						dayElements.get(i).getValue());
			}
		} else if (monthElements != null && monthElements.size() > 0) {
			level = 'm';
			for (int i = 0; i < monthElements.size(); i++) {
				strs.add(yearElements.get(i).getValue() + "-" +
						monthElements.get(i).getValue());
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

	public void parseXmlRequest(List<Area> as, String xml, List<String> years, List<String> months, List<String> days, List<String> hours) throws JDOMException, IOException {

		SAXBuilder builder = new SAXBuilder();

		Document document = builder.build(new ByteArrayInputStream(xml.getBytes()));
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
			for (Element yearElement : dayElements) {
				String day = yearElement.getText();
				if (day != null && !day.trim().equals("")) {
					System.out.println("day:" + day.trim());
					days.add(day.trim());
				}
			}
		}

		// <hours>
		List<Element> hoursElements = rootElement.getChildren("hours");
		if (hoursElements != null && hoursElements.size() == 1) {
			List<Element> hourElements = hoursElements.get(0).getChildren("hour");
			for (Element yearElement : hourElements) {
				String hour = yearElement.getText();
				if (hour != null && !hour.trim().equals("")) {
					System.out.println("hour:" + hour.trim());
					hours.add(hour.trim());
				}
			}
		}

		count(as, years, months, days, hours);
	}

	public void createChart(Map<String, Integer> cs) {
		System.out.println("chart");
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();

		Iterator it = cs.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pairs = (Map.Entry) it.next();
			dataset.addValue((Integer) pairs.getValue(), (String) pairs.getKey(), "Category 1");
		}

		JFreeChart chart = ChartFactory.createBarChart(
				// JFreeChart chart = ChartFactory.createLineChart(
				// JFreeChart chart = ChartFactory.createPieChart3D(

				"Chart", // Title
				"Month", // X Label
				"Amount of Photo", // Y Label
				dataset, // dataset
				PlotOrientation.VERTICAL, // Plot Orientation
				true, // Print the Case
				false, // Generate the tool
				false // Generate the url
				);

		FileOutputStream fos_jpg = null;

		try {
			fos_jpg = new FileOutputStream("chart.jpg");
			ChartUtilities.writeChartAsJPEG(fos_jpg, 0.8f, chart, 400, 300,
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

	public String createKml(List<Area> as, String file) {
		Document document = new Document();
		Namespace namespace = Namespace.getNamespace("http://earth.google.com/kml/2.1");
		Element rootElement = new Element("kml", namespace);

		document.setRootElement(rootElement);

		Element documentElement = new Element("Document", namespace);
		rootElement.addContent(documentElement);

		//GroundOverlay
		for (Area a : as) {
			if(a.getCount() != 0){
				Element groundOverlayElement = new Element("GroundOverlay", namespace);
				documentElement.addContent(groundOverlayElement);
				Element iconElement = new Element("Icon", namespace);
				groundOverlayElement.addContent(iconElement);
				Element hrefElement = new Element("href", namespace);
				iconElement.addContent(hrefElement);
				double r = 0;
				String icon = "";

				if(a.getCount()<100){
					r = (double)Math.log10(a.getCount())/85.0;
					icon = "http://cdn.iconfinder.net/data/icons/function_icon_set/circle_blue.png";
				}else if(a.getCount()<1000){
					r = (double)Math.log10(a.getCount())/80.0;
					icon = "http://cdn.iconfinder.net/data/icons/function_icon_set/circle_green.png";
				}else if(a.getCount()<10000){
					r = (double)Math.log10(a.getCount())/70.0;
					icon = "http://cdn.iconfinder.net/data/icons/developperss/PNG/Green%20Ball.png";
				}else{
					r = (double)Math.log10(a.getCount())/60.0;
					icon = "http://cdn.iconfinder.net/data/icons/function_icon_set/circle_orange.png";
					if(r > 0.1) r = 0.1;
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

				northElement.addContent(Double.toString(a.getCenter().getY()+r*0.6));
				southElement.addContent(Double.toString(a.getCenter().getY()-r*0.6));
				eastElement.addContent(Double.toString(a.getCenter().getX()+r));
				westElement.addContent(Double.toString(a.getCenter().getX()-r));
			}
		}

		//Polygon
		for (Area a : as) {
			// if(a.getCount()==0||!a.getName().equals("100")) continue;
			JGeometry shape = a.getGeom();
			String name = "total:" + a.getTotalCount();
			String description = "select:" + String.valueOf(a.getCount());
			String polyStyleColor = "440000";
//			String polyStyleColor = "000000ff"; //transparency
			String polyStyleFill = "1";
			String polyStyleOutline = "1";
			String lineStyleWidth = "2";
			String lineStyleColor = "ffff0000";
			String coordinates = "\n";

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
			int i = a.getTotalCount()/30;
			if (i > 255)
				i = 255;

			polyColorElement.addContent(polyStyleColor + Integer.toHexString(i));
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
		xml2File(document, file);
		return xml2String(document);
//		return xml2String(document).replaceAll("\r\n", " ");
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
