package de.fraunhofer.iais.spatial.service;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.jdom.CDATA;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;

import au.com.bytecode.opencsv.CSVReader;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;

import de.fraunhofer.iais.spatial.entity.Area;
import de.fraunhofer.iais.spatial.entity.AreaResult;
import de.fraunhofer.iais.spatial.util.StringUtil;
import de.fraunhofer.iais.spatial.util.XmlUtil;

public class CsvImporter {

	/**
	 * @param args
	 * @throws IOException
	 * @throws JDOMException
	 * @throws SQLException
	 */
	public static void main(String[] args) throws IOException, JDOMException, SQLException {
		Map<Long, Map<String, Integer>> areaEvents = Maps.newHashMap();
		List<Area> areas = Lists.newArrayList();

		importAreaEvents(areaEvents, "WebRoot/data/ped_presence_predicted_2011_08_05.csv");
		importAreas(areas, null, "WebRoot/data/ped.xml");
		initAreas(areaEvents, areas);

		List<AreaResult> areaResults = ModelManager.createAreaResults(areas);
		System.out.println(ModelManager.buildKmlFile(areaResults, "temp/ped", "100", null, "/", false));
	}

	public static void initAreas(Map<Long, Map<String, Integer>> areaEvents, List<Area> areas) {
		for (Area area : areas) {
			if (MapUtils.isNotEmpty(areaEvents.get(area.getId()))) {
				for (Map.Entry<String, Integer> events : areaEvents.get(area.getId()).entrySet()) {
					area.getDatesCount().put(events.getKey(), events.getValue());
				}
//				System.out.println(area.getId() + " " + area.getDatesCount());
			}
		}
	}

	public static void importAreas(List<Area> areas, Rectangle2D boundaryRect, String file) throws JDOMException, IOException {
		SAXBuilder builder = new SAXBuilder();
		Document document = builder.build(new File(file));
		Element rootElement = document.getRootElement();
		GeometryFactory geometryFactory = new GeometryFactory();
		for (Element objectElement : (List<Element>) rootElement.getChildren("Object")) {
			long areaId = Integer.parseInt(objectElement.getChildText("id"));
			Element shapeElement = objectElement.getChild("shape");
			Area area = new Area();

			List<Point2D> geom = new LinkedList<Point2D>();
			List<Coordinate> coordinates = Lists.newArrayList();

			boolean inBoundaryRect = false;

			for (Element pointElement : (List<Element>) shapeElement.getChildren("point")) {
				Point2D point = new Point2D.Double(Double.parseDouble(pointElement.getChildText("xCoord")), Double.parseDouble(pointElement.getChildText("yCoord")));
				geom.add(point);
				coordinates.add(new Coordinate(Double.parseDouble(pointElement.getChildText("xCoord")), Double.parseDouble(pointElement.getChildText("yCoord"))));
				if (boundaryRect != null && boundaryRect.contains(point)) {
					inBoundaryRect = true;
				}
			}
			geom.add(geom.get(0));
			coordinates.add(coordinates.get(0));

			if (boundaryRect != null && inBoundaryRect == false) {
				continue;
			}

			LinearRing shell = geometryFactory.createLinearRing(coordinates.toArray(new Coordinate[0]));
			Polygon polygon = geometryFactory.createPolygon(shell, null);
			area.setCenter(new Point2D.Double(polygon.getCentroid().getX(), polygon.getCentroid().getY()));

			area.setGeom(geom);
			area.setId(areaId);
//			System.out.println(area.getId());
//			System.out.println(area.getGeom());
			areas.add(area);
		}
	}

	public static void importAreaEvents(Map<Long, Map<String, Integer>> areaEvents, String file) throws FileNotFoundException, IOException {
		CSVReader reader = new CSVReader(new FileReader(file), ',');
		String[] nextLine;
		List<String> dates = Lists.newArrayList();

		Pattern datePattern = Pattern.compile("N visits by hours; time=(\\d{2})/(\\d{2})/(\\d{4});(\\d{2})");
		if ((nextLine = reader.readNext()) != null) {
			for (int i = 0; i < nextLine.length; i++) {
				String term = nextLine[i];
				if (i >= 1) {
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
					events.put(dates.get(i - 2), (int)(Integer.parseInt(term) * ModelManager.NUM_SCALE));
				}
			}
			areaEvents.put(areaId, events);
//			System.out.println(events);
		}
	}

}
