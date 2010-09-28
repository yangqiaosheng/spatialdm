package de.fraunhofer.iais.spatial.test;

import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileReader;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import de.fraunhofer.iais.spatial.dto.FlickrDeWestAreaDto;
import de.fraunhofer.iais.spatial.entity.FlickrDeWestArea;
import de.fraunhofer.iais.spatial.entity.FlickrDeWestPhoto;
import de.fraunhofer.iais.spatial.entity.FlickrDeWestArea.Radius;
import de.fraunhofer.iais.spatial.service.FlickrDeWestAreaMgr;
import de.fraunhofer.iais.spatial.util.StringUtil;
import de.fraunhofer.iais.spatial.util.XmlUtil;
import de.fraunhofer.iais.spatial.web.servlet.SmallPhotoUrlServlet;

public class TestFlickrDeWestArea {

	private static FlickrDeWestAreaMgr areaMgr = null;

	@BeforeClass
	public static void initClass() {
		// Spring IOC
		ApplicationContext context = new ClassPathXmlApplicationContext(new String[] { "beans.xml" });
		areaMgr = context.getBean("flickrDeWestAreaMgr", FlickrDeWestAreaMgr.class);
		System.setProperty("oraclespatialweb.root", System.getProperty("user.dir") + "\\");
		System.out.println("oraclespatialweb.root:" + System.getProperty("oraclespatialweb.root"));
	}

	@Test
	public void testRegEx() throws ParseException {
		Pattern polygonPattern = Pattern.compile("\\(([-0-9.]*), ([-0-9.]*)\\)");
		Matcher polygonMachter = polygonPattern.matcher("<polygon>(51.58830123054393, 6.971684570312502)(51.67184146523792, 7.647343750000002)(51.44644311790073, 7.298527832031252)</polygon>");
		while (polygonMachter.find()) {
			Point2D point = new Point2D.Double();
			point.setLocation(Double.parseDouble(polygonMachter.group(1)), Double.parseDouble(polygonMachter.group(2)));
			System.out.println(point);
		}

		Pattern intervalPattern = Pattern.compile("([\\d]{2}/[\\d]{2}/[\\d]{4}) - ([\\d]{2}/[\\d]{2}/[\\d]{4})");
		Matcher intervalMachter = intervalPattern.matcher("<interval>15/09/2010 - 19/10/2010</interval>");
		if (intervalMachter.find()) {
			SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
			System.out.println("beginDate:" + sdf.parse(intervalMachter.group(1)));
			System.out.println("endDate:" + sdf.parse(intervalMachter.group(2)));
		}

		Pattern selectedDaysPattern = Pattern.compile("([A-Z]{1}[a-z]{2} [\\d]{2} [\\d]{4})");
		Matcher selectedDaysMachter = selectedDaysPattern.matcher("<selected_days>Sep 08 2010,Sep 10 2010,Oct 14 2010,Oct 19 2010,Sep 24 2010,Sep 22 2005,Sep 09 2005</selected_days>");
		while (selectedDaysMachter.find()) {
			SimpleDateFormat sdf = new SimpleDateFormat("MMM dd yyyy", Locale.ENGLISH);
			System.out.println(sdf.parse(selectedDaysMachter.group()));
		}
	}

	@Test
	public void testJdbcDao1() {
		FlickrDeWestArea a = areaMgr.getAreaDao().getAreaById(1, FlickrDeWestArea.Radius.R80000);

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
		//		List<FlickrDeWestArea> as = areaMgr.getAreaDao().getAllAreas(Radius._10000);
		// 		List<FlickrDeWestArea> as = areaMgr.getAreaDao().getAreasByPoint(8.83, 50.58, Radius._5000);
		List<FlickrDeWestArea> as = areaMgr.getAreaDao().getAreasByRect(1, 1, 96.5, 95.4, Radius.R80000);
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
			//			System.out.println(a.getHoursCount());
			//			System.out.println(coordinates + "\n");
			// System.out.println(person+":"+dao.getPersonCount(a.getId(),
			// person));
		}
	}

	@Test
	public void testPhoto() {
		for (int i = 1; i <= 20; i++) {
			FlickrDeWestPhoto p = areaMgr.getAreaDao().getPhoto(1, Radius.R80000, "2007-08-11@13", i);
			if (p != null) {
				System.out.print("PHOTO_ID:" + p.getId());
				System.out.print("\tAreaid:" + p.getArea().getId());
				System.out.print("\tRadius:" + p.getArea().getRadius());
				System.out.print("\tarea:" + p.getArea().getArea());
				System.out.print("\tDT:" + p.getDate());
				System.out.print("\tLATITUDE:" + p.getLatitude());
				System.out.print("\tLONGITUDE:" + p.getLongitude());
				System.out.print("\tPERSON:" + p.getPersonId());
				System.out.print("\tRAWTAGS:" + p.getRawTags());
				System.out.print("\tTITLE:" + p.getTitle());
				System.out.print("\tSMALLURL:" + p.getSmallUrl());
				System.out.println("\tVIEWED:" + p.getViewed());
			}
		}
	}

	@Test
	public void testPhoto2() {
		Set<String> hours = new HashSet<String>();
		hours.add("2007-08-11@13");
		hours.add("2007-08-11@11");
		hours.add("2007-05-09@13");

		List<FlickrDeWestPhoto> photos = areaMgr.getAreaDao().getPhotos(1, Radius.R80000, hours, 20);
		for (FlickrDeWestPhoto p : photos) {
			System.out.print("PHOTO_ID:" + p.getId());
			System.out.print("\tAreaid:" + p.getArea().getId());
			System.out.print("\tRadius:" + p.getArea().getRadius());
			System.out.print("\tarea:" + p.getArea().getArea());
			System.out.print("\tDT:" + p.getDate());
			System.out.print("\tLATITUDE:" + p.getLatitude());
			System.out.print("\tLONGITUDE:" + p.getLongitude());
			System.out.print("\tPERSON:" + p.getPersonId());
			System.out.print("\tRAWTAGS:" + p.getRawTags());
			System.out.print("\tTITLE:" + p.getTitle());
			System.out.print("\tSMALLURL:" + p.getSmallUrl());
			System.out.println("\tVIEWED:" + p.getViewed());
		}

		System.out.println(new SmallPhotoUrlServlet().photosResponseXml(1, Radius.R80000, hours, 20));
	}

	@Test
	public void testTree() {
		SortedSet<String> s = new TreeSet<String>();
		s.add("2007-05-08");
		s.add("2007-06-08");
		s.add("2007-05-09");
		s.add("2007-05-18");
		s.add("2009-01-08");
		s.add("2008-01-08");
		s.add("1995-01-08");
		while (s.size() > 0) {
			System.out.println(s.last());
			s.remove(s.last());
		}
	}

	@Test
	public void testRequestXml() throws Exception {
		FlickrDeWestAreaDto areaDto = new FlickrDeWestAreaDto();
//		BufferedReader br = new BufferedReader(new FileReader("FlickrDeWestKmlRequest2.xml"));
		BufferedReader br = new BufferedReader(new FileReader("FlickrDeWestKmlRequest1.xml"));
		StringBuffer xml = new StringBuffer();
		String thisLine;
		while ((thisLine = br.readLine()) != null) {
			xml.append(thisLine);
		}
		areaMgr.parseXmlRequest(StringUtil.FullMonth2Num(xml.toString()), areaDto);

		System.out.println("radius:" + areaDto.getRadius());
		System.out.println("zoom:" + areaDto.getZoom());
		System.out.println("center:" + areaDto.getCenter());
		System.out.println("boundaryRect:" + areaDto.getBoundaryRect());
		if (areaDto.getPolygon() != null) {
			System.out.print("polygon:" + areaDto.getPolygon());
			for (Point2D p : areaDto.getPolygon()) {
				System.out.print(p + "|");
			}
			System.out.println("");
		}
		System.out.println("queryLevel:" + areaDto.getQueryLevel());
		System.out.println("beginDate:" + areaDto.getBeginDate() + ", endDate" + areaDto.getEndDate());
		if (areaDto.getSelectedDays() != null) {
			System.out.print("Select_days:" + areaDto.getSelectedDays());
			for (Date d : areaDto.getSelectedDays()) {
				System.out.print(d + "|");
			}
			System.out.println("");
		}
		System.out.println("years:" + areaDto.getYears());
		System.out.println("months:" + areaDto.getMonths());
		System.out.println("days:" + areaDto.getDays());
		System.out.println("hours:" + areaDto.getHours());
		System.out.println("weekdays:" + areaDto.getWeekdays());
		System.out.println("queryStringsSize:" + areaDto.getQueryStrs().size() + " |queryStrings:" + areaDto.getQueryStrs());
//		List<FlickrDeWestArea> as = null;
//		if (areaDto.getBoundaryRect() == null) {
//			as = areaMgr.getAreaDao().getAllAreas(areaDto.getRadius());
//		} else {
//			as = areaMgr.getAreaDao().getAreasByRect(areaDto.getBoundaryRect().getMinX(), areaDto.getBoundaryRect().getMinY(), areaDto.getBoundaryRect().getMaxX(), areaDto.getBoundaryRect().getMaxY(), areaDto.getRadius());
//		}
//		areaMgr.count(as, areaDto);
//		System.out.println(as.size());
	}

	@Test
	public void testKml1() throws Exception {
		FlickrDeWestAreaDto areaDto = new FlickrDeWestAreaDto();
		System.out.println("oraclespatialweb.root:" + System.getProperty("oraclespatialweb.root"));
		BufferedReader br = new BufferedReader(new FileReader("FlickrDeWestKmlRequest1.xml"));
		StringBuffer xml = new StringBuffer();
		String thisLine;
		while ((thisLine = br.readLine()) != null) {
			xml.append(thisLine);
		}
		areaMgr.parseXmlRequest(StringUtil.FullMonth2Num(xml.toString()), areaDto);
		List<FlickrDeWestArea> as = null;
		if (areaDto.getBoundaryRect() == null) {
			as = areaMgr.getAreaDao().getAllAreas(areaDto.getRadius());
		} else {
			as = areaMgr.getAreaDao().getAreasByRect(areaDto.getBoundaryRect().getMinX(), areaDto.getBoundaryRect().getMinY(), areaDto.getBoundaryRect().getMaxX(), areaDto.getBoundaryRect().getMaxY(), areaDto.getRadius());
		}
		areaMgr.count(as, areaDto);

		System.out.println(areaMgr.createKml(as, "temp/FlickrDeWestArea" + areaDto.getRadius(), areaDto.getRadius(), null));

		//		System.out.println(createKml(as, "temp/FlickrDeWestArea" + radius, null));
		//		radius = FlickrDeWestArea.Radius._40000;
		//		System.out.println(createKml(as, "temp/FlickrDeWestArea" + radius, null));
		//		radius = FlickrDeWestArea.Radius._80000;
		//		System.out.println(createKml(as, "temp/FlickrDeWestArea" + radius, null));
		//		radius = FlickrDeWestArea.Radius._5000;
		//		System.out.println(createKml(as, "temp/FlickrDeWestArea" + radius, null));
	}

	@Test
	public void testXmlGeneration() throws Exception {
		BufferedReader br = new BufferedReader(new FileReader("FlickrDeWestKmlRequest1.xml"));
		StringBuffer xml = new StringBuffer();
		String thisLine;
		while ((thisLine = br.readLine()) != null) {
			xml.append(thisLine);
		}
		String xmlStr = StringUtil.shortNum2Long(StringUtil.FullMonth2Num(xml.toString()));
		SAXBuilder builder = new SAXBuilder();
		Document document = builder.build(new ByteArrayInputStream(xmlStr.getBytes()));
		System.out.println(XmlUtil.xml2String(document, false));
		System.out.println(URLEncoder.encode((XmlUtil.xml2String(document, true)), "UTF-8"));
		
		
	}
}
