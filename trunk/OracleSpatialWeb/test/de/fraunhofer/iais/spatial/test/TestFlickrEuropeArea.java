package de.fraunhofer.iais.spatial.test;

import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.NamingException;

import junit.framework.Assert;

import org.apache.commons.io.IOUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.mock.jndi.SimpleNamingContextBuilder;

import de.fraunhofer.iais.spatial.dto.FlickrEuropeAreaDto;
import de.fraunhofer.iais.spatial.dto.FlickrEuropeAreaDto.Level;
import de.fraunhofer.iais.spatial.entity.FlickrArea;
import de.fraunhofer.iais.spatial.entity.FlickrPhoto;
import de.fraunhofer.iais.spatial.entity.FlickrArea.Radius;
import de.fraunhofer.iais.spatial.service.FlickrEuropeAreaMgr;
import de.fraunhofer.iais.spatial.util.DateUtil;
import de.fraunhofer.iais.spatial.util.StringUtil;
import de.fraunhofer.iais.spatial.util.XmlUtil;

//@ContextConfiguration("classpath:beans.xml")
public class TestFlickrEuropeArea {

//	@Resource(name = "flickrEuropeAreaMgr")
	private static FlickrEuropeAreaMgr areaMgr = null;


	@BeforeClass
	public static void initClass() throws NamingException {
		System.setProperty("oraclespatialweb.root", System.getProperty("user.dir") + "/");
		System.out.println("oraclespatialweb.root:" + System.getProperty("oraclespatialweb.root"));

		ApplicationContext context = new ClassPathXmlApplicationContext(new String[] { "classpath:beans.xml" });
		SimpleNamingContextBuilder builder = SimpleNamingContextBuilder.emptyActivatedContextBuilder();
		builder.bind("java:comp/env/jdbc/OracleCP", context.getBean("oracleIccDataSource"));

		areaMgr = (FlickrEuropeAreaMgr) context.getBean("flickrEuropeAreaMgr");
	}

	@Test
	public void testCalendar() throws ParseException {

		System.out.println("allWeekday:" + DateUtil.allWeekdayDates);
		System.out.println("allHour:" + DateUtil.allHourDates);
		System.out.println("allDay:" + DateUtil.allDayDates);
		System.out.println("allMonth:" + DateUtil.allMonthDates);
		System.out.println("allYear:" + DateUtil.allYearDates);
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
	public void testJdbcDao0() {
		int total = areaMgr.getAreaDao().getTotalCount(1, FlickrArea.Radius.R5000);

		System.out.println("total:" + total);
	}

	@Test
	public void testJdbcDao1() {
		FlickrArea a = areaMgr.getAreaDao().getAreaById(1, FlickrArea.Radius.R5000);

		String coordinates = "\t";
		if (a != null) {
			for (Point2D point : a.getGeom()) {
				coordinates += point.getX() + ", " + point.getY() + "0\n";
			}

			System.out.println(a.getId() + " radius:" + a.getRadius() + " area:" + a.getArea() + "\t" + "cx:" + a.getCenter().getX() + "\t" + "cy:" + a.getCenter().getY());
			System.out.println("coordinates:" + coordinates);
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
		//		List<FlickrEuropeArea> as = areaMgr.getAreaDao().getAllAreas(Radius._10000);
		// 		List<FlickrEuropeArea> as = areaMgr.getAreaDao().getAreasByPoint(8.83, 50.58, Radius._5000);
		int size = areaMgr.getAreaDao().getAreasByRectSize(1, 1, 96.5, 95.4, Radius.R320000);
		List<FlickrArea> as = areaMgr.getAreaDao().getAreasByRect(1, 1, 96.5, 95.4, Radius.R320000);
		Assert.assertEquals(size, as.size());

		for (FlickrArea a : as) {
			String coordinates = "\t";
			for (Point2D point : a.getGeom()) {
				coordinates += point.getX() + ", " + point.getY() + "0\n";
			}

			System.out.println(a.getId() + " radius:" + a.getRadius() + " area:" + a.getArea() + "\t" + "cx:" + a.getCenter().getX() + "\t" + "cy:" + a.getCenter().getY());
			//			System.out.println(a.getHoursCount());
			//			System.out.println(coordinates + "\n");
			// System.out.println(person+":"+dao.getPersonCount(a.getId(),
			// person));
		}
	}

	@Test
	public void testPhoto2() {
		long start = System.currentTimeMillis();

		FlickrEuropeAreaDto areaDto = new FlickrEuropeAreaDto();
		areaDto.setQueryLevel(Level.DAY);
		Set<String> queryStr = areaDto.getQueryStrs();
		queryStr.add("2007-08-04");

		List<FlickrPhoto> photos = areaMgr.getAreaDao().getPhotos(areaMgr.getAreaDao().getAreaById(23338, Radius.R80000), areaDto, 1, 20);
		for (FlickrPhoto p : photos) {
			System.out.println(p);
		}

		System.out.println(System.currentTimeMillis() - start);
	}

	@Test
	public void testPhoto3() {
		long start = System.currentTimeMillis();

		FlickrEuropeAreaDto areaDto = new FlickrEuropeAreaDto();
		Set<String> hours = areaDto.getQueryStrs();
		hours.add("2007-08-11@13");
		hours.add("2007-08-11@11");
		hours.add("2007-05-09@13");
		areaDto.setQueryLevel(Level.HOUR);

		List<FlickrPhoto> photos = areaMgr.getAreaDao().getPhotos(areaMgr.getAreaDao().getAreaById(23315, Radius.R80000), areaDto, 1, 20);
		for (FlickrPhoto p : photos) {
			System.out.println(p);
		}

		System.out.println(System.currentTimeMillis() - start);
	}

	@Test
	public void testPhoto4() {
		long start = System.currentTimeMillis();

		FlickrEuropeAreaDto areaDto = new FlickrEuropeAreaDto();
		Set<String> hours = areaDto.getQueryStrs();
		hours.add("2007-08-11@13");
		hours.add("2007-08-11@11");
		hours.add("2007-05-09@13");
		areaDto.setQueryLevel(Level.HOUR);

		for (int i = 2005; i < 2010; i++) {
			for (int j = 10; j <= 12; j++) {
				for (int k = 10; k < 30; k++) {
					for (int l = 10; l < 24; l++) {
						hours.add(i + "-" + j + "-" + k + "@" + l);
					}
				}
			}
		}

		List<FlickrPhoto> photos0 = areaMgr.getAreaDao().getPhotos(areaMgr.getAreaDao().getAreaById(24669, Radius.R320000), areaDto, 1, 20);
		for (FlickrPhoto p : photos0) {
			System.out.println(p);
		}

		List<FlickrPhoto> photos1 = areaMgr.getAreaDao().getPhotos(areaMgr.getAreaDao().getAreaById(24272, Radius.R160000), areaDto, 1, 20);
		for (FlickrPhoto p : photos1) {
			System.out.println(p);
		}

		List<FlickrPhoto> photos = areaMgr.getAreaDao().getPhotos(areaMgr.getAreaDao().getAreaById(23315, Radius.R80000), areaDto, 1, 20);
		for (FlickrPhoto p : photos) {
			System.out.println(p);
		}

		List<FlickrPhoto> photos2 = areaMgr.getAreaDao().getPhotos(areaMgr.getAreaDao().getAreaById(21170, Radius.R40000), areaDto, 1, 20);
		for (FlickrPhoto p : photos2) {
			System.out.println(p);
		}

		List<FlickrPhoto> photos3 = areaMgr.getAreaDao().getPhotos(areaMgr.getAreaDao().getAreaById(16988, Radius.R20000), areaDto, 1, 20);
		for (FlickrPhoto p : photos3) {
			System.out.println(p);
		}

		List<FlickrPhoto> photos4 = areaMgr.getAreaDao().getPhotos(areaMgr.getAreaDao().getAreaById(10091, Radius.R10000), areaDto, 1, 20);
		for (FlickrPhoto p : photos4) {
			System.out.println(p);
		}

		List<FlickrPhoto> photos5 = areaMgr.getAreaDao().getPhotos(areaMgr.getAreaDao().getAreaById(27, Radius.R5000), areaDto, 1, 20);
		for (FlickrPhoto p : photos5) {
			System.out.println(p);
		}

		Set<String> years = areaDto.getQueryStrs();
		years.add("2007");
		years.add("2009");
		years.add("2006");

		FlickrEuropeAreaDto areaDto2 = new FlickrEuropeAreaDto();
		areaDto2.setQueryLevel(Level.YEAR);
		List<FlickrPhoto> photos6 = areaMgr.getAreaDao().getPhotos(areaMgr.getAreaDao().getAreaById(27, Radius.R5000), areaDto2, 1, 20);
		for (FlickrPhoto p : photos6) {
			System.out.println(p);
		}

		System.out.println(System.currentTimeMillis() - start);
	}

	@Test
	public void testPhoto5() {
		long start = System.currentTimeMillis();

		FlickrEuropeAreaDto areaDto = new FlickrEuropeAreaDto();
		Set<String> hours = areaDto.getQueryStrs();
		hours.add("2007-08-11@13");
		hours.add("2007-08-11@11");
		hours.add("2007-05-09@13");

		areaDto.setQueryLevel(Level.HOUR);

		for (int i = 2005; i < 2010; i++) {
			for (int j = 10; j <= 12; j++) {
				for (int k = 10; k < 30; k++) {
					for (int l = 10; l < 24; l++) {
						hours.add(i + "-" + j + "-" + k + "@" + l);
					}
				}
			}
		}
		//
		//		List<FlickrEuropePhoto> photos = areaMgr.getAreaDao().getPhotos(1, Radius.R80000, hours, 20);
		//		for (FlickrEuropePhoto p : photos) {
		//			System.out.println(p);
		//		}
		//
		//		List<FlickrEuropePhoto> photos2 = areaMgr.getAreaDao().getPhotos(1, Radius.R40000, hours, 20);
		//		for (FlickrEuropePhoto p : photos2) {
		//			System.out.println(p);
		//		}

		FlickrArea area = areaMgr.getAreaDao().getAreaById(23315, Radius.R80000);
		List<FlickrPhoto> photos3 = areaMgr.getAreaDao().getPhotos(area, areaDto, 1, 90);
		for (int i = 0; i < photos3.size(); i++) {
			FlickrPhoto p = photos3.get(i);
			System.out.println(p);
		}

		int page = 3;
		int pageSize = 5;
		List<FlickrPhoto> photos32 = areaMgr.getAreaDao().getPhotos(area, areaDto, page, pageSize);
		for (int i = 0; i < photos32.size(); i++) {
			FlickrPhoto p = photos32.get(i);
			System.out.println(p);
			Assert.assertEquals(photos3.get((page - 1) * pageSize + i).getId(), p.getId());
		}

		//		List<FlickrEuropePhoto> photos4 = areaMgr.getAreaDao().getPhotos(1, Radius.R10000, hours, 20);
		//		for (FlickrEuropePhoto p : photos4) {
		//			System.out.println(p);
		//		}
		//
		//		List<FlickrEuropePhoto> photos5 = areaMgr.getAreaDao().getPhotos(1, Radius.R5000, hours, 20);
		//		for (FlickrEuropePhoto p : photos5) {
		//			System.out.println(p);
		//		}

		//		TreeSet<String> years = new TreeSet<String>();
		//		years.add("2007");
		//		years.add("2009");
		//		years.add("2006");
		//		List<FlickrEuropePhoto> photos6 = areaMgr.getAreaDao().getPhotos(1, Radius.R5000, years, 20);
		//		for (FlickrEuropePhoto p : photos6) {
		//			System.out.println(p);
		//		}

		System.out.println(System.currentTimeMillis() - start);
	}

	@Test
	public void testPhotoXml() {
		long start = System.currentTimeMillis();

		FlickrEuropeAreaDto areaDto = new FlickrEuropeAreaDto();
		Set<String> hours = areaDto.getHours();
		hours.add("2007-08-11@13");
		hours.add("2007-08-11@11");
		hours.add("2007-05-09@13");
		areaDto.setQueryLevel(Level.HOUR);

		for (int i = 2005; i < 2010; i++) {
			for (int j = 10; j <= 12; j++) {
				for (int k = 10; k < 30; k++) {
					for (int l = 10; l < 24; l++) {
						hours.add(i + "-" + j + "-" + k + "@" + l);
					}
				}
			}
		}
		System.out.println(photosResponseXml(11349, Radius.R10000, areaDto, 100));
	}

	private String photosResponseXml(int areaid, Radius radius, FlickrEuropeAreaDto areaDto, int num) {
		FlickrArea area = areaMgr.getAreaDao().getAreaById(areaid, radius);
		List<FlickrPhoto> photos = areaMgr.getAreaDao().getPhotos(area, areaDto, 1, num);

		Document document = new Document();
		Element rootElement = new Element("photos");
		document.setRootElement(rootElement);

		int i = 1;
		for (FlickrPhoto p : photos) {
			Element photoElement = new Element("photo");
			rootElement.addContent(photoElement);
			photoElement.setAttribute("index", String.valueOf(i++));

//			photoElement.addContent(new Element("photoId").setText(String.valueOf(p.getId())));
//			photoElement.addContent(new Element("personId").setText(String.valueOf(p.getPersonId())));
//			photoElement.addContent(new Element("polygonId").setText(String.valueOf(p.getArea().getId())));
//			photoElement.addContent(new Element("polygonRadius").setText(String.valueOf(p.getArea().getRadius())));
			photoElement.addContent(new Element("date").setText(String.valueOf(p.getDate())));
			photoElement.addContent(new Element("latitude").setText(String.valueOf(p.getLatitude())));
			photoElement.addContent(new Element("longitude").setText(String.valueOf(p.getLongitude())));
			photoElement.addContent(new Element("title").setText(String.valueOf(p.getTitle())));
			photoElement.addContent(new Element("pageUrl").setText("http://www.flickr.com/photos/" + p.getPersonId() + "/" + p.getId()));
			photoElement.addContent(new Element("photoUrl").setText(String.valueOf(p.getSmallUrl())));
//			photoElement.addContent(new Element("viewed").setText(String.valueOf(p.getViewed())));
//			photoElement.addContent(new Element("rawTags").setText(String.valueOf(p.getRawTags())));
		}
		XmlUtil.xml2File(document, "temp/FlickrEuropePhoto_" + radius + ".xml", false);
		return XmlUtil.xml2String(document, false);
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
		FlickrEuropeAreaDto areaDto = new FlickrEuropeAreaDto();
//		BufferedReader br = new BufferedReader(new FileReader("FlickrEuropeKmlRequest2.xml"));
//		BufferedReader br = new BufferedReader(new FileReader("FlickrEuropeKmlRequest1.xml"));
//		StringBuffer xml = new StringBuffer();
//		String thisLine;
//		while ((thisLine = br.readLine()) != null) {
//			xml.append(thisLine);
//		}
		String xml = IOUtils.toString(new FileReader("FlickrEuropeKmlRequest1.xml"));
		areaMgr.parseXmlRequest(StringUtil.FullMonth2Num(xml), areaDto);

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

		System.out.println("years:" + areaDto.getYears());
		System.out.println("months:" + areaDto.getMonths());
		System.out.println("days:" + areaDto.getDays());
		System.out.println("hours:" + areaDto.getHours());
		System.out.println("weekdays:" + areaDto.getWeekdays());
		System.out.println("queryStringsSize:" + areaDto.getQueryStrs().size() + " |queryStrings:" + areaDto.getQueryStrs());
//		List<FlickrEuropeArea> as = null;
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
		FlickrEuropeAreaDto areaDto = new FlickrEuropeAreaDto();
		System.out.println("oraclespatialweb.root:" + System.getProperty("oraclespatialweb.root"));
		BufferedReader br = new BufferedReader(new FileReader("FlickrEuropeKmlRequest1.xml"));
		StringBuffer xml = new StringBuffer();
		String thisLine;
		while ((thisLine = br.readLine()) != null) {
			xml.append(thisLine);
		}
		areaMgr.parseXmlRequest(StringUtil.FullMonth2Num(xml.toString()), areaDto);
		List<FlickrArea> as = null;
		if (areaDto.getBoundaryRect() == null) {
			as = areaMgr.getAreaDao().getAllAreas(areaDto.getRadius());
		} else {
			as = areaMgr.getAreaDao().getAreasByRect(areaDto.getBoundaryRect().getMinX(), areaDto.getBoundaryRect().getMinY(), areaDto.getBoundaryRect().getMaxX(), areaDto.getBoundaryRect().getMaxY(), areaDto.getRadius());
		}

		long start = System.currentTimeMillis();
		areaMgr.countSelected(as, areaDto);
		System.out.println(System.currentTimeMillis() - start);
//		System.out.println(areaMgr.createKml(as, "temp/FlickrEuropeArea" + areaDto.getRadius(), areaDto.getRadius(), null, true));
//		System.out.println(areaMgr.createXml(as, "temp/FlickrEuropeArea" + areaDto.getRadius(), areaDto.getRadius()));
	}

	@Test
	public void testXmlGeneration() throws Exception {
		BufferedReader br = new BufferedReader(new FileReader("FlickrEuropeKmlRequest1.xml"));
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

	@Test
	@Ignore
	public void testSelectAll() {
		List<FlickrArea> as = areaMgr.getAreaDao().getAllAreas(Radius.R160000);
		for (FlickrArea a : as) {
			String coordinates = "\t";
			for (Point2D point : a.getGeom()) {
				coordinates += point.getX() + ", " + point.getY() + "0\n";
			}

			System.out.println(a.getId() + " radius:" + a.getRadius() + " area:" + a.getArea() + "\t" + "cx:" + a.getCenter().getX() + "\t" + "cy:" + a.getCenter().getY());
			System.out.println(coordinates + "\n");
		}
	}

	@Test
	public void testSelectById() {
		FlickrArea a = areaMgr.getAreaDao().getAreaById(23312, Radius.R80000);
		String coordinates = "\t";
		for (Point2D point : a.getGeom()) {
			coordinates += point.getX() + ", " + point.getY() + "0\n";
		}

		System.out.println(a.getId() + " radius:" + a.getRadius() + " area:" + a.getArea() + "\t" + "cx:" + a.getCenter().getX() + "\t" + "cy:" + a.getCenter().getY());
		System.out.println(coordinates + "\n");
	}

	@Test
	public void testTimeSeriesChart() throws Exception {
		testRequestXml();
//		Set<String> years = new HashSet<String>();
//		years.add("2005");
//		years.add("2009");
//		years.add("2007");
//		years.add("2001");
//		years.add("2005-01");
		FileOutputStream fos1 = new FileOutputStream("temp/xyChart.png");
		FileOutputStream fos2 = new FileOutputStream("temp/tsCharth.png");
		FileOutputStream fos3 = new FileOutputStream("temp/tsChartd.png");
		FileOutputStream fos4 = new FileOutputStream("temp/tsChartm.png");
		FileOutputStream fos5 = new FileOutputStream("temp/tsCharty.png");
		FileOutputStream fos6 = new FileOutputStream("temp/tsChartw.png");
		List<FlickrArea> areas = new ArrayList<FlickrArea>();
		FlickrArea area1 = areaMgr.getAreaDao().getAreaById(23312, FlickrArea.Radius.R80000);
		FlickrArea area3 = areaMgr.getAreaDao().getAreaById(23318, FlickrArea.Radius.R80000);
		areas.add(area1);
		areas.add(area3);

		boolean smmoth = true;
		FlickrEuropeAreaDto areaDto = new FlickrEuropeAreaDto();
		String xml = IOUtils.toString(new FileReader("FlickrEuropeKmlRequest1.xml"));
		areaMgr.parseXmlRequest(StringUtil.FullMonth2Num(xml), areaDto);
		areaMgr.createXYLineChart(areas, Level.MONTH, areaDto, 800, 300, true, smmoth, fos1);
		areaMgr.createTimeSeriesChart(areas, Level.HOUR, areaDto, 800, 300, true, smmoth, fos2);
		areaMgr.createTimeSeriesChart(areas, Level.DAY, areaDto, 800, 300, true, smmoth, fos3);
		areaMgr.createTimeSeriesChart(areas, Level.MONTH, areaDto, 800, 300, true, smmoth, fos4);
		areaMgr.createTimeSeriesChart(areas, Level.YEAR, areaDto, 800, 300, true, smmoth, fos5);
		areaMgr.createTimeSeriesChart(areas, Level.WEEKDAY, areaDto, 800, 300, true, smmoth, fos6);
	}

//	@Test
//	public void testQueryLevel() {
//		String hour = "2009-07-07@13";
//		System.out.println(FlickrEuropeAreaDao.judgeQueryLevel(hour));
//		System.out.println(FlickrEuropeAreaDao.oracleDataPatternStr(FlickrEuropeAreaDao.judgeQueryLevel(hour)));
//		String day = "2009-07-07";
//		System.out.println(FlickrEuropeAreaDao.judgeQueryLevel(day));
//		String month = "2009-07";
//		System.out.println(FlickrEuropeAreaDao.judgeQueryLevel(month));
//		String year = "2009";
//		System.out.println(FlickrEuropeAreaDao.judgeQueryLevel(year));
//	}

}
