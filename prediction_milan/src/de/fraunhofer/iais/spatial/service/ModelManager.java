package de.fraunhofer.iais.spatial.service;

import java.awt.geom.Point2D;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.jdom.CDATA;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;

import com.google.common.collect.Lists;

import de.fraunhofer.iais.spatial.dto.AreaDto;
import de.fraunhofer.iais.spatial.dto.AreaDto.Level;
import de.fraunhofer.iais.spatial.entity.Area;
import de.fraunhofer.iais.spatial.entity.AreaResult;
import de.fraunhofer.iais.spatial.util.DateUtil;
import de.fraunhofer.iais.spatial.util.StringUtil;
import de.fraunhofer.iais.spatial.util.XmlUtil;

public class ModelManager {
	public static String minuteDateFormatStr = "yyyy-MM-dd@HH:mm";
	public static String hourDateFormatStr = "yyyy-MM-dd@HH";
	public static String dayDateFormatStr = "yyyy-MM-dd";
	public static String monthDateFormatStr = "yyyy-MM";
	public static String yearDateFormatStr = "yyyy";

	public static String buildKmlFile(List<AreaResult> areaResults, String filenamePrefix, String radius, Point2D transfromVector, String remoteBasePath, boolean compress) throws UnsupportedEncodingException {
		String localBasePath = "";
		try{
			localBasePath = ModelManager.class.getResource("/../../").getPath();
		}catch(Throwable t){
		}

		if (StringUtils.isEmpty(remoteBasePath)) {
			remoteBasePath = "http://localhost:8080/prediction_milan/";
		}

		Document document = createKmlDoc(areaResults, radius, transfromVector, remoteBasePath);

		if (filenamePrefix != null) {
			if (StringUtils.isNotEmpty(filenamePrefix)) {
				if (compress == true) {
					XmlUtil.xml2Kmz(document, localBasePath + filenamePrefix, true);
				} else {
					XmlUtil.xml2File(document, localBasePath + filenamePrefix + ".kml", false);
				}
			}
		}

		return XmlUtil.xml2String(document, false);
	}

	public String buildKmlString(List<AreaResult> areaResults, String radius, Point2D transfromVector, String remoteBasePath) throws IOException {
		Document document = createKmlDoc(areaResults, radius, transfromVector, remoteBasePath);

		return XmlUtil.xml2String(document, false);
	}

	private static Document createKmlDoc(List<AreaResult> areaResults, String radius, Point2D transformVector, String remoteBasePath) {
		if (transformVector == null) {
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
		for (AreaResult areaResult : areaResults) {
			Area area = areaResult.getArea();
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
				} else if (areaResult.getSelectCount() < 4000) {
//					r = (double) Math.log10(area.getSelectCount() + 1) / 70.0 * scale;
					icon = remoteBasePath + "images/circle_lgr.ico";
				} else {
//					r = (double) Math.log10(area.getSelectCount() + 1) / 60.0 * scale;
					icon = remoteBasePath + "images/circle_or.ico";
				}

				r = (double) Math.log10(areaResult.getSelectCount() + 1) / 70.0 * scale;

				Element hrefElement = new Element("href", namespace).addContent(icon);
				iconElement.addContent(hrefElement);

//				Element altitudeElement = new Element("altitude", namespace).addContent(String.valueOf(area.getTotalCount()*100));
//				groundOverlayElement.addContent(altitudeElement);
//				Element altitudeModeElement = new Element("altitudeMode", namespace).addContent("absolute");
//				groundOverlayElement.addContent(altitudeModeElement);
				Element latLonBoxElement = new Element("LatLonBox", namespace);
				groundOverlayElement.addContent(latLonBoxElement);
				Element northElement = new Element("north", namespace).addContent(Double.toString(area.getCenter().getY() + transformVector.getY() + r * 0.55));
				Element southElement = new Element("south", namespace).addContent(Double.toString(area.getCenter().getY() + transformVector.getY() - r * 0.55));
				Element eastElement = new Element("east", namespace).addContent(Double.toString(area.getCenter().getX() + transformVector.getX() + r));
				Element westElement = new Element("west", namespace).addContent(Double.toString(area.getCenter().getX() + transformVector.getX() - r));
				;
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
		for (AreaResult areaResult : areaResults) {
			Area area = areaResult.getArea();
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
			for (Point2D point : shape) {
				coordinates += (point.getX() + transformVector.getX()) + "," + (point.getY() + transformVector.getY()) + "0\n";
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

	private static void addKmlExtendedElement(Element extendedDataElement, Namespace namespace, Map<Integer, Integer> chartData, Level displayLevel) {
		for (Map.Entry<Integer, Integer> e : chartData.entrySet()) {
			String dataName = displayLevel + "_" + DateUtil.getChartLabelStr(e.getKey(), displayLevel);
			Element dataElement = new Element("Data", namespace).setAttribute("name", dataName);
			dataElement.addContent(new Element("displayName", namespace).addContent(DateUtil.getChartLabelStr(e.getKey(), displayLevel)));
			dataElement.addContent(new Element("value", namespace).addContent(e.getValue() + ""));
			extendedDataElement.addContent(dataElement);
		}
	}

	private static String buildKmlDescription(AreaResult areaResult) {
		int width = 640;
		String weekdayChartImg = createChartImg("Photos Distribution", width / 2, 160, areaResult.getHistograms().getWeekdays(), Level.WEEKDAY);
		String yearChartImg = createChartImg("Photos Distribution", width / 2, 160, areaResult.getHistograms().getYears(), Level.YEAR);
		String monthChartImg = createChartImg("Photos Distribution", width, 160, areaResult.getHistograms().getMonths(), Level.MONTH);
		String dayChartImg = createChartImg("Photos Distribution", width, 160, areaResult.getHistograms().getDays(), Level.DAY);
		String hourChartImg = createChartImg("Photos Distribution", width, 160, areaResult.getHistograms().getHours(), Level.HOUR);
		String description = weekdayChartImg + yearChartImg + "<BR>" + monthChartImg + "<BR>" + dayChartImg + "<BR>" + hourChartImg;
		return description;
	}

	public static String createChartImg(String title, int width, int height, Map<Integer, Integer> histrogramData, Level displayLevel) {
		String values = "";
		String labels = "";
		int maxValue = new TreeSet<Integer>(histrogramData.values()).last();
		int barWithd = (int) (width / histrogramData.size() / 1.3);
		for (Map.Entry<Integer, Integer> e : histrogramData.entrySet()) {
			labels += "|" + DateUtil.getChartLabelStr(e.getKey(), displayLevel);
			values += "," + e.getValue();
		}

//		for(Map.Entry<Integer, Integer> e : chartData.entrySet()){
//			String dataName = displayLevel + "_" + DateUtil.getChartLabelStr(e.getKey(), displayLevel);
//			labels += "|$[" + dataName + "/displayName]";
//			values += ",$[" + dataName + "]";
//		}

		String img = "<img src='" +
				"https://chart.googleapis.com/chart" + 	//chart engine
				"?chtt=" + displayLevel + //chart title
				"&cht=bvs" + //chart type
				"&chs=" + width + "x" + height + //chart size(pixel)
				"&chd=t:" + values.substring(1) + //values
				"&chxt=x,y" + //display axises
				"&chxl=0:" + labels + //labels in x axis
				"&chbh=" + barWithd + //chbh=<bar_width_or_scale>,<space_between_bars>,<space_between_groups>
				"&chm=N,444444,-1,,12" + //data marker chm= <marker_type>,<color>,<series_index>,<which_points>,<size>,<z_order>,<placement>
				"&chds=0," + maxValue * 1.2 + //scale of y axis (default 1-100)
				"&chxr=1,0," + maxValue * 1.2 + //scale in value (default 1-100)
				"'/>";

		return img;
	}

	public static void countSelected(List<AreaResult> areaResults, AreaDto areaDto) {
		for (AreaResult areaResult : areaResults) {
			Area area = areaResult.getArea();
			int selectCount = 0;
			int totalCount = 0;
			Map<String, Integer> counts = null;

			counts = area.getDatesCount();

			for (Map.Entry<String, Integer> e : counts.entrySet()) {
				if (areaDto.getQueryStrs().contains(e.getKey())) {
					selectCount += e.getValue();
				}
				totalCount += e.getValue();
			}

//			for (String queryStr : areaDto.getQueryStrs()) {
//				if (counts.containsKey(queryStr)) {
//					num += counts.get(queryStr);
//				}
//			}

			areaResult.setSelectedCount(selectCount);
			area.setTotalCount(totalCount);
		}
	}

	public static List<AreaResult> createAreaResults(List<Area> areas) {
		List<AreaResult> areaResults = Lists.newArrayList();
		for (Area area : areas) {
			AreaResult areaResult = new AreaResult(area);
			areaResults.add(areaResult);
		}
		return areaResults;
	}

	/**
	 * calculate the histograms DataSets for each Area
	 * @param areas
	 * @param areaDto
	 */
	public static void calculateHistograms(List<AreaResult> areaResults, AreaDto areaDto) {

		int queryStrsLength = areaDto.getQueryStrsLength();
		for (AreaResult areaResult : areaResults) {
			Map<Integer, Integer> yearData = areaResult.getHistograms().getYears();
			Map<Integer, Integer> monthData = areaResult.getHistograms().getMonths();
			Map<Integer, Integer> dayData = areaResult.getHistograms().getDays();
			Map<Integer, Integer> hourData = areaResult.getHistograms().getHours();
			Map<Integer, Integer> weekdayData = areaResult.getHistograms().getWeekdays();

			Calendar calendar = DateUtil.createReferenceCalendar();
			calendar.setLenient(false);

			//set values
			for (Map.Entry<String, Integer> e : areaResult.getArea().getDatesCount().entrySet()) {
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
}
