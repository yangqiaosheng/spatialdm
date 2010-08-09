package de.fraunhofer.iais.spatial.test;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import oracle.spatial.geometry.JGeometry;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.junit.BeforeClass;
import org.junit.Test;

import de.fraunhofer.iais.spatial.dao.jdbc.FlickrDeWestAreaDaoJdbc;
import de.fraunhofer.iais.spatial.entity.Area;
import de.fraunhofer.iais.spatial.entity.FlickrDeWestArea;
import de.fraunhofer.iais.spatial.service.AreaMgr;

public class TestFlickrDeWestArea {

	// private static AreaMgr areaMgr = null;
	@BeforeClass
	public static void initClass() {
		// Spring IOC
		// ApplicationContext context =
		// new ClassPathXmlApplicationContext(new String[] {"beans.xml"});
		// areaMgr = context.getBean("areaMgr", AreaMgr.class);
		// init
		// areaMgr = new AreaMgr();
		// areaMgr.setAreaDao(new AreaDaoIbatis());
		System.setProperty("oraclespatialweb.root", "C:/java_file/eclipse/MyEclipse/OracleSpatialWeb/");
		System.out.println("oraclespatialweb.root:" + System.getProperty("oraclespatialweb.root"));
	}

	@Test
	public void testKml() throws UnsupportedEncodingException {
		FlickrDeWestAreaDaoJdbc areaDao = new FlickrDeWestAreaDaoJdbc();
		FlickrDeWestArea.Radius radius = FlickrDeWestArea.Radius._10000;
		System.out.println(createKml(areaDao.getAllAreas(radius), "temp/FlickrDeWestArea" + radius));
		radius = FlickrDeWestArea.Radius._20000;
		System.out.println(createKml(areaDao.getAllAreas(radius), "temp/FlickrDeWestArea" + radius));
		radius = FlickrDeWestArea.Radius._40000;
		System.out.println(createKml(areaDao.getAllAreas(radius), "temp/FlickrDeWestArea" + radius));
		radius = FlickrDeWestArea.Radius._80000;
		System.out.println(createKml(areaDao.getAllAreas(radius), "temp/FlickrDeWestArea" + radius));
		radius = FlickrDeWestArea.Radius._5000;
		System.out.println(createKml(areaDao.getAllAreas(radius), "temp/FlickrDeWestArea" + radius));

	}

	@Test
	public void testJdbcDao1() {
		FlickrDeWestAreaDaoJdbc areaDao = new FlickrDeWestAreaDaoJdbc();
		FlickrDeWestArea a = areaDao.getAreaById("1   ", FlickrDeWestArea.Radius._5000);
		String coordinates = "\t";
		if (a.getGeom().getOrdinatesArray() != null) {
			for (int i = 0; i < a.getGeom().getOrdinatesArray().length; i++) {
				coordinates += a.getGeom().getOrdinatesArray()[i] + ", ";
				if (i % 2 == 1) {
					coordinates += "0\t";
				}
			}

			System.out.println(a.getId() + " radius:" + a.getRadius() + " area:" + a.getArea() + "\t" + "cx:" + a.getCenter().getX() + "\t" + "cy:" + a.getCenter().getY());
			System.out.println("hours:" + a.getHoursCount().size());
			System.out.println("days:" + a.getDaysCount().size());
			System.out.println("months:" + a.getMonthsCount().size());
			System.out.println("years:" + a.getYearsCount().size());
			// System.out.println(person+":"+dao.getPersonCount(a.getId(),
			// person));
		}
	}

	@Test
	public void testJdbcDao2() {
		FlickrDeWestAreaDaoJdbc areaDao = new FlickrDeWestAreaDaoJdbc();
		List<FlickrDeWestArea> as = areaDao.getAllAreas(FlickrDeWestArea.Radius._10000);
		for (FlickrDeWestArea a : as) {
			String coordinates = "\t";
			if (a.getGeom().getOrdinatesArray() != null) {
				for (int i = 0; i < a.getGeom().getOrdinatesArray().length; i++) {
					coordinates += a.getGeom().getOrdinatesArray()[i] + ", ";
					if (i % 2 == 1) {
						coordinates += "0\t";
					}
				}
			}

			System.out.println(a.getId() + " radius:" + a.getRadius() + " area:" + a.getArea() + "\t" + "cx:" + a.getCenter().getX() + "\t" + "cy:" + a.getCenter().getY());
			System.out.println(a.getHoursCount());
			System.out.println(coordinates + "\n");
			// System.out.println(person+":"+dao.getPersonCount(a.getId(),
			// person));
		}
	}

	private String createKml(List<FlickrDeWestArea> as, String file) throws UnsupportedEncodingException {
		String localBasePath = System.getProperty("oraclespatialweb.root");
		String remoteBasePath = "http://kd-photomap.iais.fraunhofer.de/OracleSpatialWeb/";
		Document document = new Document();
		Namespace namespace = Namespace.getNamespace("http://earth.google.com/kml/2.1");
		Element rootElement = new Element("kml", namespace);

		document.setRootElement(rootElement);

		Element documentElement = new Element("Document", namespace);
		rootElement.addContent(documentElement);

		// GroundOverlay
		for (FlickrDeWestArea a : as) {
			if (a.getTotalCount() != 0) {
				String name = "total:" + a.getTotalCount();
				//				String description = "select:" + String.valueOf(a.getSelectCount());
				String description = "<p>select:" + String.valueOf(a.getTotalCount()) + "</p><br><div style='width:600px;height:300px'><img width='600' height='300' src='TimeSeriesChart?id=" + URLEncoder.encode(a.getId(), "UTF-8") + "&xml="
						+ URLEncoder.encode(file + ".xml", "UTF-8") + "'></div>";
				String groundOverlayColor = "aaffffff"; //transparency

				Element groundOverlayElement = new Element("GroundOverlay", namespace);
				documentElement.addContent(groundOverlayElement);
				Element nameElement = new Element("name", namespace);
				nameElement.addContent(name);
				Element descriptionElement = new Element("description", namespace);
				descriptionElement.addContent(description);
				Element colorElement = new Element("name", namespace);
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

				if (a.getTotalCount() < 100) {
					r = (double) Math.log10(a.getTotalCount()) / 85.0 / 2;
					icon = remoteBasePath + "images/circle_bl.ico";
				} else if (a.getTotalCount() < 1000) {
					r = (double) Math.log10(a.getTotalCount()) / 80.0 / 2;
					icon = remoteBasePath + "images/circle_gr.ico";
				} else if (a.getTotalCount() < 10000) {
					r = (double) Math.log10(a.getTotalCount()) / 70.0 / 2;
					icon = remoteBasePath + "images/circle_lgr.ico";
				} else {
					r = (double) Math.log10(a.getTotalCount()) / 60.0 / 2;
					icon = remoteBasePath + "images/circle_or.ico";
					if (r > 0.1) {
						r = 0.1;
					}
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
			}
		}

		// Polygon
		for (FlickrDeWestArea a : as) {
			// if(a.getCount()==0||!a.getName().equals("100")) continue;
			String name = "id:" + a.getId();
			String description = "count: " + String.valueOf(a.getTotalCount());
			String polyStyleColor = "440000"; //not transparent
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
			descriptionElement.addContent(description);
			Element styleElement = new Element("Style", namespace);
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
		xml2File(document, localBasePath + file + ".kml");
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
				if (o != null) {
					o.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
