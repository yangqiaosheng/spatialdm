package de.fraunhofer.iais.spatial.script.prediction;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.jdom.CDATA;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;

import au.com.bytecode.opencsv.CSVReader;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import de.fraunhofer.iais.spatial.dto.FlickrAreaDto.Level;
import de.fraunhofer.iais.spatial.entity.FlickrArea;
import de.fraunhofer.iais.spatial.entity.FlickrAreaResult;
import de.fraunhofer.iais.spatial.entity.FlickrArea.Radius;
import de.fraunhofer.iais.spatial.util.DateUtil;
import de.fraunhofer.iais.spatial.util.StringUtil;
import de.fraunhofer.iais.spatial.util.XmlUtil;

public class PredictionKmlExport {

	/**
	 * @param args
	 * @throws IOException
	 * @throws JDOMException
	 */
	public static void main(String[] args) throws IOException, JDOMException {
		Map<Long, Map<String, Integer>> areaEvents = Maps.newHashMap();
		List<FlickrArea> areas = Lists.newArrayList();

		importAreaEvents(areaEvents);
		importAreas(areaEvents, areas);
		initAreas(areaEvents, areas);

		System.out.println(buildKmlString(areas, Radius.R1250, ""));
		System.out.println(buildKmlFile(areas, "temp/areas1000", Radius.R1250, null, false));
	}

	private static void initAreas(Map<Long, Map<String, Integer>> areaEvents, List<FlickrArea> areas) {
		for (FlickrArea area : areas) {
			for (Map.Entry<String, Integer> events : areaEvents.get(area.getId()).entrySet()) {
				area.getHoursCount().put(events.getKey(), events.getValue());
			}
		}
	}

	private static void importAreas(Map<Long, Map<String, Integer>> areaEvents, List<FlickrArea> areas) throws JDOMException, IOException {
		SAXBuilder builder = new SAXBuilder();
		Document document = builder.build(new File("temp/areas1000.xml"));
		Element rootElement = document.getRootElement();
		for (Element objectElement : (List<Element>) rootElement.getChildren("Object")) {
			long areaId = Integer.parseInt(objectElement.getChildText("id"));
			Element shapeElement = objectElement.getChild("shape");
			FlickrArea area = new FlickrArea();

			Point2D center = new Point2D.Double(Double.parseDouble(objectElement.getChild("centroid").getChildText("xCoord")), Double.parseDouble(objectElement.getChild("centroid").getChildText("yCoord")));

			List<Point2D> geom = new LinkedList<Point2D>();
			for (Element pointElement : (List<Element>) shapeElement.getChildren("point")) {
				geom.add(new Point2D.Double(Double.parseDouble(pointElement.getChildText("xCoord")), Double.parseDouble(pointElement.getChildText("yCoord"))));
			}

			area.setCenter(center);
			area.setGeom(geom);
			area.setId(areaId);
			System.out.println(area.getId());
			System.out.println(area.getGeom());
			for (Map.Entry<String, Integer> events : areaEvents.get(areaId).entrySet()) {
				area.getHoursCount().put(events.getKey(), events.getValue());
			}
			areas.add(area);
		}
	}

	private static void importAreaEvents(Map<Long, Map<String, Integer>> areaEvents) throws FileNotFoundException, IOException {
		CSVReader reader = new CSVReader(new FileReader("temp/places_presence_predicted_2011_09_19-25.csv"), ',');
		String[] nextLine;
		List<String> dates = Lists.newArrayList();

		Pattern datePattern = Pattern.compile("N visits by hours; time=(\\d{2})/(\\d{2})/(\\d{4});(\\d{2})");
		if ((nextLine = reader.readNext()) != null) {
			for (int i = 0; i < nextLine.length; i++) {
				String term = nextLine[i];
				if (i >= 2) {
					Matcher dateMatcher = datePattern.matcher(term);
					System.out.println(term);
					if (dateMatcher.matches()) {
						System.out.println(dateMatcher.group());
						String day = dateMatcher.group(1);
						String month = dateMatcher.group(2);
						String year = dateMatcher.group(3);
						String hour = dateMatcher.group(4);
						dates.add(year + "-" + month + "-" + day + "@" + hour);
					}
				}
			}
		}
		System.out.println(dates);

		while ((nextLine = reader.readNext()) != null) {
			long areaId = -1;
			Map<String, Integer> events = Maps.newTreeMap();
			for (int i = 0; i < nextLine.length; i++) {
				String term = nextLine[i];
				if (i == 0) {
					areaId = Long.parseLong(term);
				} else if (i >= 2) {
					events.put(dates.get(i - 2), Integer.parseInt(term));
				}
			}
			areaEvents.put(areaId, events);
//			System.out.println(events);
		}
	}

	public static String buildKmlFile(List<FlickrArea> areas, String filenamePrefix, Radius radius, String remoteBasePath, boolean compress) throws UnsupportedEncodingException {
		if (StringUtils.isEmpty(remoteBasePath)) {
			remoteBasePath = "http://kd-photomap.iais.fraunhofer.de/OracleSpatialWeb/";
		}

		Document document = createKmlDoc(areas, radius, remoteBasePath);

		if(filenamePrefix != null){
			if(StringUtils.isNotEmpty(filenamePrefix)){
				if(compress == true){
					XmlUtil.xml2Kmz(document,  filenamePrefix, true);
				}else {
					XmlUtil.xml2File(document, filenamePrefix + ".kml", false);
				}
			}
		}

		return XmlUtil.xml2String(document, false);
	}

	public static String buildKmlString(List<FlickrArea> areas, Radius radius, String remoteBasePath) throws IOException {
		Document document = createKmlDoc(areas, radius, remoteBasePath);

		return XmlUtil.xml2String(document, false);
	}

	private static Document createKmlDoc(List<FlickrArea> areas, Radius radius, String remoteBasePath) {
		Document document = new Document();

		Namespace namespace = Namespace.getNamespace("http://earth.google.com/kml/2.1");
		Element rootElement = new Element("kml", namespace);

		float scale = (float) (Integer.parseInt(radius.toString()) / 30000.0);
		document.setRootElement(rootElement);

		Element documentElement = new Element("Document", namespace);
		rootElement.addContent(documentElement);

		// GroundOverlay
		for (FlickrArea area : areas) {
			String name = String.valueOf(area.getId());
			String description = "";
			Element groundOverlayElement = new Element("GroundOverlay", namespace);
			documentElement.addContent(groundOverlayElement);
			groundOverlayElement.addContent(new Element("name", namespace).addContent(name));
			groundOverlayElement.addContent(new Element("description", namespace).addContent(new CDATA(description)));
//				groundOverlayElement.addContent(new Element("color", namespace).addContent("eeffffff"));
			Element iconElement = new Element("Icon", namespace);
			groundOverlayElement.addContent(iconElement);
			String icon = remoteBasePath + "images/circle_or.ico";

			int count = 0;
			for(int num : area.getHoursCount().values()){
				count += num;
			}

			double r = 0;
			if (count < 100) {
				r = (double) Math.log10(count + 1) / 85.0 * scale;
				icon = remoteBasePath + "images/circle_bl.ico";
			} else if (count < 1000) {
				r = (double) Math.log10(count + 1) / 80.0 * scale;
				icon = remoteBasePath + "images/circle_gr.ico";
			} else if (count < 10000) {
				r = (double) Math.log10(count + 1) / 70.0 * scale;
				icon = remoteBasePath + "images/circle_lgr.ico";
			} else {
				r = (double) Math.log10(count + 1) / 60.0 * scale;
				icon = remoteBasePath + "images/circle_or.ico";
			}


			Element hrefElement = new Element("href", namespace).addContent(icon);
			iconElement.addContent(hrefElement);

//				Element altitudeElement = new Element("altitude", namespace).addContent(String.valueOf(area.getTotalCount()*100));
//				groundOverlayElement.addContent(altitudeElement);
//				Element altitudeModeElement = new Element("altitudeMode", namespace).addContent("absolute");
//				groundOverlayElement.addContent(altitudeModeElement);
			Element latLonBoxElement = new Element("LatLonBox", namespace);
			groundOverlayElement.addContent(latLonBoxElement);
			Element northElement = new Element("north", namespace).addContent(Double.toString(area.getCenter().getY() + r * 0.55));
			Element southElement = new Element("south", namespace).addContent(Double.toString(area.getCenter().getY() - r * 0.55));
			Element eastElement = new Element("east", namespace).addContent(Double.toString(area.getCenter().getX() + r));
			Element westElement = new Element("west", namespace).addContent(Double.toString(area.getCenter().getX() - r));
			;
			latLonBoxElement.addContent(northElement);
			latLonBoxElement.addContent(southElement);
			latLonBoxElement.addContent(eastElement);
			latLonBoxElement.addContent(westElement);

		}

		// Polygon
		for (FlickrArea area : areas) {
			String name = "id:" + area.getId();
			String description = buildKmlDescription(area);

//			String polyStyleColor = "440000"; //not transparent
			String polyStyleColor = "000000"; //transparent
			String polyStyleFill = "1";
			String polyStyleOutline = "1";
			String lineStyleWidth = "2";
			String lineStyleColor = "88ff0000";
			String coordinates = "\n";

			List<Point2D> shape = area.getGeom();
			for (Point2D point : shape) {
				coordinates += point.getX() + ", " + point.getY() + "0\n";
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
		}
		return document;
	}

	private static String buildKmlDescription(FlickrArea area) {
		int width = 1000;
		int height = 300;
		String weekdayChartImg = createGoogleChartImg("Photos Distribution", width, height, area.getHoursCount(), Level.HOUR);

		String description = weekdayChartImg;
		return description;
	}

	public static String createGoogleChartImg(String title, int width, int height, Map<String, Integer> data, Level displayLevel) {
		String values = "";
		String labels = "";
		int maxValue = new TreeSet<Integer>(data.values()).last();
		int labelsInterval = 24;
		int valuesInterval = 1;
		int i = 0;
		for (Map.Entry<String, Integer> e : data.entrySet()) {
			if (i++ % labelsInterval == 0) {
				labels += "|" + e.getKey();
			}
			if (i % valuesInterval == 0) {
				values += "," + e.getValue();
			}
		}
		labels += "| ";

		String img = "<img src='" + "http://chart.apis.google.com/chart" + //chart engine
				"?chtt=" + displayLevel + //chart title
				"&cht=lc" + //chart type
				"&chs=" + width + "x" + height + //chart size(pixel)
				"&chd=t:" + values.substring(1) + //values
				"&chxt=x,y" + //display axises
				"&chg=1,10" + //chg=<x_axis_step_size>,<y_axis_step_size>,<opt_dash_length>,<opt_space_length>,<opt_x_offset>,<opt_y_offset>
				"&chxl=0:" + labels + //labels in x axis
				"&chm=N,444444,-1,-4,12" + //data marker chm= <marker_type>,<color>,<series_index>,<which_points>,<size>,<z_order>,<placement>
				"&chds=0," + maxValue * 1.2 + //scale of y axis (default 1-100)
				"&chxr=1,0," + maxValue * 1.2 + //scale in value (default 1-100)
				"'/>";

		return img;
	}

}
