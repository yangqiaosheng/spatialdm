package de.fraunhofer.iais.spatial.test;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.NamingException;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.mock.jndi.SimpleNamingContextBuilder;

import com.google.common.collect.Lists;

import de.fraunhofer.iais.spatial.dao.FlickrAreaDao;
import de.fraunhofer.iais.spatial.dto.FlickrAreaDto;
import de.fraunhofer.iais.spatial.dto.FlickrAreaDto.Level;
import de.fraunhofer.iais.spatial.dto.SessionMutex;
import de.fraunhofer.iais.spatial.entity.FlickrArea;
import de.fraunhofer.iais.spatial.entity.FlickrArea.Radius;
import de.fraunhofer.iais.spatial.entity.FlickrAreaResult;
import de.fraunhofer.iais.spatial.entity.FlickrPhoto;
import de.fraunhofer.iais.spatial.entity.Histograms;
import de.fraunhofer.iais.spatial.script.db.JoinFlickrAreaTagsCount;
import de.fraunhofer.iais.spatial.service.FlickrAreaCancelableJob;
import de.fraunhofer.iais.spatial.service.FlickrAreaMgr;
import de.fraunhofer.iais.spatial.util.DateUtil;
import de.fraunhofer.iais.spatial.util.StringUtil;
import de.fraunhofer.iais.spatial.util.XmlUtil;
import de.fraunhofer.iais.spatial.web.servlet.HistrogramsDataServlet;

//@ContextConfiguration("classpath:beans.xml")
public class TestFlickrEuropeArea {

//	@Resource(name = "flickrAreaMgr")
	private static FlickrAreaMgr areaMgr = null;
//	@Resource(name = "flickrAreaCancelableJob")

	@BeforeClass
	public static void initClass() throws NamingException {
		System.setProperty("flickr_world.root", System.getProperty("user.dir") + "/");
		System.out.println("flickr_world.root:" + System.getProperty("flickr_world.root"));

		ApplicationContext context = new ClassPathXmlApplicationContext(new String[] { "classpath:beans.xml" });
//		ApplicationContext context = new ClassPathXmlApplicationContext(new String[] { "classpath:beans_oracle.xml" });
		SimpleNamingContextBuilder builder = SimpleNamingContextBuilder.emptyActivatedContextBuilder();
		builder.bind("java:comp/env/jdbc/OracleCP", context.getBean("oracleIccDataSource"));

		areaMgr = context.getBean("flickrAreaMgr", FlickrAreaMgr.class);
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
	public void testHoursTags() throws ParseException {
		String count = "2010-03-04@23:<test|32,live|334,west|12,es|12,ht|12,>;2010-03-05@00:<test|132,live|1334,west|112,es|12,ht|112,>;";
		FlickrArea area = new FlickrArea();
		FlickrAreaDao.parseHoursTagsCountDbString(count, area.getHoursTagsCount());
		System.out.println(area.getHoursTagsCount());
		System.out.println(FlickrAreaDao.createDatesTagsCountDbString(area.getHoursTagsCount()));
	}

	@Test
	public void testHoursTags2() throws ParseException {
		String count1 = "2010-03-04@23:<test|32,west|12,es|12,ht|12,>;2010-03-05@00:<test|132,live|1334,west|112,es|12,ht|112,>;";
		FlickrArea area1 = new FlickrArea();
		FlickrAreaDao.parseHoursTagsCountDbString(count1, area1.getHoursTagsCount());
		System.out.println(area1.getHoursTagsCount());

		String count2 = "2010-03-04@23:<test|30,live|334,west|12,es|12,ht|12,>;2010-03-05@00:<test|132,live|1334,west|112,es|12,ht|112,>;";
		FlickrArea area2 = new FlickrArea();
		FlickrAreaDao.parseHoursTagsCountDbString(count2, area2.getHoursTagsCount());
		System.out.println(area2.getHoursTagsCount());

		JoinFlickrAreaTagsCount.mergeTagsCountsMap(area1.getHoursTagsCount(), area2.getHoursTagsCount());

		System.out.println(FlickrAreaDao.createDatesTagsCountDbString(area1.getHoursTagsCount()));
	}

	@Test
	public void testHoursTags3() throws ParseException {
		String hourStr = "2009-01-11@14:<deutschland|1,bayern|1,wuerzburg|1,hats|1,franken|1,winter|1,bavaria|1,julia|1,snow|1,franconia|1,germany|1,schnee|1,portraits|1,huete|1,>;2009-05-20@19:<clouds|1,deutschland|1,bavaria|1,wolken|1,sonnenstrahlen|1,sunrays|1,würzburg|1,bayern|1,germany|1,regen|1,rain|1,sun|1,>;2009-05-20@20:<port|1,deutschland|1,hafen|1,flüsse|1,markers|1,würzburg|1,bayern|1,oldport|1,zero|1,rivers|1,alterhafen|1,himmel|1,main|1,jsovtmb|1,sky|1,bavaria|1,julia|1,schiffe|1,germany|1,ships|1,null|1,>;";
		SortedMap<String, Map<String, Integer>> hoursTagsCount = new TreeMap<String, Map<String, Integer>>();
		SortedMap<String, Map<String, Integer>> daysTagsCount = new TreeMap<String, Map<String, Integer>>();

		FlickrAreaDao.parseHoursTagsCountDbString(hourStr, hoursTagsCount);

		System.out.println("hoursTagsCount:" + hoursTagsCount);

		for(Entry<String, Map<String, Integer>> e : hoursTagsCount.entrySet()){
			Pattern p = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})@\\d{2}");
			Matcher m = p.matcher(e.getKey());
			m.find();
			String key = m.group(1);
			System.out.println(key + ":" + e.getValue());
			JoinFlickrAreaTagsCount.addToTagsCountsMap(daysTagsCount, key, e.getValue());
		}


		String dayStr = FlickrAreaDao.createDatesTagsCountDbString(daysTagsCount);
		System.out.println("!" + dayStr);
	}

	@Test
	public void testTag() {
		long start = System.currentTimeMillis();

		FlickrAreaDto areaDto = new FlickrAreaDto();
		areaDto.setQueryLevel(Level.HOUR);
		Set<String> queryStr = areaDto.getQueryStrs();

		for (int i = 2005; i < 2010; i++) {
			for (int j = 1; j <= 12; j++) {
				for (int k = 1; k < 30; k++) {
					for (int l = 1; l < 24; l++) {
						queryStr.add(i + "-" + new DecimalFormat("00").format(j) + "-" +  new DecimalFormat("00").format(k) + "@" +  new DecimalFormat("00").format(l));
					}
				}
			}
		}

		FlickrArea area = new FlickrArea();
		area.setId(10);
		System.out.println(tagsResponseXml(area, areaDto, 30));
		System.out.println(System.currentTimeMillis() - start);
	}

	@Test
	public void testJdbcDao0() {
		long total = areaMgr.getAreaDao().getTotalCountWithinArea(1);

		System.out.println("total:" + total);
	}


	@Test
	public void testJdbcDao1() {
		FlickrArea a = areaMgr.getAreaDao().getAreaById(1, FlickrArea.Radius.R320000);

		long totalNum = areaMgr.getAreaDao().getTotalEuropePhotoNum();
		long totalNumPeople = areaMgr.getAreaDao().getTotalPeopleNum();

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
			System.out.println("Total Number of Photo:" + totalNum);
			System.out.println("Total Number of People:" + totalNumPeople);
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

	private String tagsResponseXml(FlickrArea area, FlickrAreaDto areaDto, int size) {

		FlickrAreaResult areaResult = areaMgr.countTag(area, areaDto);

		List<Map.Entry<String, Integer>> entries = Lists.newLinkedList(areaResult.getTagsCount().entrySet());

		//sort list based on comparator
		Collections.sort(entries, new Comparator<Map.Entry<String, Integer>>() {
			@Override
			public int compare(Map.Entry<String, Integer> e1, Map.Entry<String, Integer> e2) {
				return e2.getValue() - e1.getValue();
			}
		});

		Document document = new Document();
		Element tagsElement = new Element("tags");
		document.setRootElement(tagsElement);

		int num = 0;
		for(Map.Entry<String, Integer> entry : entries){
			if(num++ > size){
				break;
			}
			Element tagElement = new Element("tag");
			tagsElement.addContent(tagElement);
			tagElement.setAttribute("name", entry.getKey());
			tagElement.setAttribute("num", String.valueOf(entry.getValue()));
		}

		return XmlUtil.xml2String(document, false);
	}

	@Test
	public void testPhoto2() {
		long start = System.currentTimeMillis();

		FlickrAreaDto areaDto = new FlickrAreaDto();
		areaDto.setQueryLevel(Level.HOUR);
		Set<String> queryStr = areaDto.getQueryStrs();
		queryStr.add("2006-01-01@00");
		queryStr.add("2006-10-07@17");
		queryStr.add("2009-05-03@11");

		List<FlickrPhoto> photos = areaMgr.getAreaDao().getPhotos(areaMgr.getAreaDao().getAreaById(3646, Radius.R40000), areaDto, 1, 20);
		for (FlickrPhoto p : photos) {
			System.out.println(p);
		}

		System.out.println(System.currentTimeMillis() - start);
	}


	@Test
	public void testPhoto4() {
		long start = System.currentTimeMillis();

		FlickrAreaDto areaDto = new FlickrAreaDto();
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

		List<FlickrPhoto> photos0 = areaMgr.getAreaDao().getPhotos(areaMgr.getAreaDao().getAreaById(3646, Radius.R40000), areaDto, 1, 20);
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

		FlickrAreaDto areaDto2 = new FlickrAreaDto();
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

		FlickrAreaDto areaDto = new FlickrAreaDto();
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

		FlickrAreaDto areaDto = new FlickrAreaDto();
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
		System.out.println(photosResponseXml(10, Radius.R320000, areaDto, 100));
	}

	private String photosResponseXml(int areaid, Radius radius, FlickrAreaDto areaDto, int num) {
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
	public void testRequestXml() throws Exception {

		FlickrAreaDto areaDto = new FlickrAreaDto();
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
//		System.out.println("queryStringsSize:" + areaDto.getQueryStrs().size() + " |queryStrings:" + areaDto.getQueryStrs());
//		List<FlickrEuropeArea> as = null;
//		if (areaDto.getBoundaryRect() == null) {
//			as = areaMgr.getAreaDao().getAllAreas(areaDto.getRadius());
//		} else {
//			as = areaMgr.getAreaDao().getAreasByRect(areaDto.getBoundaryRect().getMinX(), areaDto.getBoundaryRect().getMinY(), areaDto.getBoundaryRect().getMaxX(), areaDto.getBoundaryRect().getMaxY(), areaDto.getRadius());
//		}
//		areaMgr.count(as, areaDto);
//		System.out.println(as.size());
		FlickrAreaDto areaDto2 = (FlickrAreaDto) SerializationUtils.clone(areaDto);
		System.out.println("areaDto2 == areaDto:" + (areaDto2 == areaDto));
		System.out.println("areaDto2.equals(areaDto):" + areaDto2.equals(areaDto));
//		System.out.println("areaDto2.queryStringsSize:" + areaDto2.getQueryStrs().size() + " |areaDto2.queryStrings:" + areaDto2.getQueryStrs());
	}

	@Test
	public void testHistogram2() throws JDOMException, IOException, ParseException, InterruptedException {
		testHistogram();
		testHistogram();
	}
	@Test
	public void testHistogram() throws JDOMException, IOException, ParseException, InterruptedException {
		FlickrAreaDto areaDto = new FlickrAreaDto();
		areaMgr.parseXmlRequest(StringUtil.FullMonth2Num(FileUtils.readFileToString(new File("FlickrDateHistrogramRequest3.xml"))), areaDto);
		long start = System.currentTimeMillis();
		Date timestamp = new Date();
		SessionMutex sessionMutex = new SessionMutex(timestamp);
		List<FlickrArea> areas = null;
//		areas = areaMgr.getAreaDao().getAllAreas(Radius.R320000);
		areas = areaMgr.getAreaDao().getAreasByRect(areaDto.getBoundaryRect().getMinX(), areaDto.getBoundaryRect().getMinY(), areaDto.getBoundaryRect().getMaxX(), areaDto.getBoundaryRect().getMaxY(), areaDto.getRadius());
		long middle = System.currentTimeMillis();
		Histograms sumHistrograms = areaMgr.getAreaCancelableJob().calculateSumHistogram(timestamp, sessionMutex, areas, areaDto);

		Document document = new Document();
		Element rootElement = new Element("response");
		document.setRootElement(rootElement);
		HistrogramsDataServlet histrogramsDataServlet = new HistrogramsDataServlet();
		histrogramsDataServlet.setAreaMgr(areaMgr);
		String resultXml = histrogramsDataServlet.histrogramsResponseXml(document, sumHistrograms, true);
		System.out.println(resultXml);

		System.out.println("db time:" + (middle - start));
		System.out.println("histrogram time:" + (System.currentTimeMillis() - middle));
	}

	@Test
	public void testKml() throws Exception {
		long start = System.currentTimeMillis();
		FlickrAreaDto areaDto = new FlickrAreaDto();
		System.out.println("flickr_world.root:" + System.getProperty("flickr_world.root"));
		areaMgr.parseXmlRequest(StringUtil.FullMonth2Num(FileUtils.readFileToString(new File("FlickrDateHistrogramRequest3.xml"))), areaDto);
		List<FlickrArea> areas = null;
		List<FlickrAreaResult> areaResults = Lists.newArrayList();
		if (areaDto.getBoundaryRect() == null) {
			areas = areaMgr.getAreaDao().getAllAreas(areaDto.getRadius());
		} else {
			areas = areaMgr.getAreaDao().getAreasByRect(areaDto.getBoundaryRect().getMinX(), areaDto.getBoundaryRect().getMinY(), areaDto.getBoundaryRect().getMaxX(), areaDto.getBoundaryRect().getMaxY(), areaDto.getRadius());
		}

		Date timestamp = new Date();
		SessionMutex sessionMutex = new SessionMutex(timestamp);

		areaMgr.getAreaCancelableJob().countSelected(timestamp, sessionMutex, areaResults, areas, areaDto);

		System.out.println(areaMgr.buildKmlString(areaResults, areaDto.getRadius(), ""));
		System.out.println(System.currentTimeMillis() - start);
//		System.out.println(areaMgr.createXml(as, "temp/FlickrEuropeArea" + areaDto.getRadius(), areaDto.getRadius()));
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
		List<FlickrArea> areas = new ArrayList<FlickrArea>();
		FlickrArea area1 = areaMgr.getAreaDao().getAreaById(23312, FlickrArea.Radius.R80000);
//		FlickrArea area3 = areaMgr.getAreaDao().getAreaById(23318, FlickrArea.Radius.R80000);
		areas.add(area1);
//		areas.add(area3);

		boolean smmoth = true;
		FlickrAreaDto areaDto = new FlickrAreaDto();
		String xml = IOUtils.toString(new FileReader("FlickrEuropeKmlRequest1.xml"));
		areaMgr.parseXmlRequest(StringUtil.FullMonth2Num(xml), areaDto);
		boolean icon = true;
//		areaMgr.createXYLineChart(areas, Level.MONTH, areaDto, 800, 300, true, smmoth, new FileOutputStream("temp/xyChart.png"));
//		areaMgr.createTimeSeriesChart(areas, Level.HOUR, areaDto, 800, 300, true, smmoth, icon, new FileOutputStream("temp/tsCharth.png"));
//		areaMgr.createTimeSeriesChart(areas, Level.DAY, areaDto, 800, 300, true, smmoth, icon, new FileOutputStream("temp/tsChartd.png"));
//		areaMgr.createTimeSeriesChart(areas, Level.MONTH, areaDto, 800, 300, true, smmoth, icon, new FileOutputStream("temp/tsChartm.png"));
		areaMgr.createTimeSeriesChart(areas, Level.YEAR, areaDto,  100, 60, false, smmoth, icon, new FileOutputStream("temp/tsCharty.png"));
		areaMgr.createTimeSeriesChart(areas, Level.WEEKDAY, areaDto, 100, 60, false, smmoth, icon, new FileOutputStream("temp/tsChartw.png"));
	}

	@Test
	public void testTagTimeSeriesChart() throws Exception {
		testRequestXml();
		Set<String> years = new HashSet<String>();
		years.add("2005");
		years.add("2006");
		years.add("2009");
		years.add("2010");
		years.add("2011");
		years.add("2007");
		years.add("2008");
		FlickrArea area = areaMgr.getAreaDao().getAreaById(1, FlickrArea.Radius.R320000);

		areaMgr.createTagTimeSeriesChartOld(area, "motorsport", years, new FileOutputStream("temp/tsTagCharty.png"));
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
