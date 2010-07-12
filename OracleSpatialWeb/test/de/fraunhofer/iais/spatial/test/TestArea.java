package de.fraunhofer.iais.spatial.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import de.fraunhofer.iais.spatial.dao.AreaDao;
import de.fraunhofer.iais.spatial.dao.ibatis.AreaDaoIbatis;
import de.fraunhofer.iais.spatial.entity.Area;
import de.fraunhofer.iais.spatial.service.AreaMgr;
import de.fraunhofer.iais.spatial.servlet.RequestKml;
import de.fraunhofer.iais.spatial.util.StringUtil;

@ContextConfiguration("classpath:beans.xml")
public class TestArea extends AbstractJUnit4SpringContextTests {

	@Resource(name = "areaMgr")
	private AreaMgr areaMgr = null;

	// private static AreaMgr areaMgr = null;
	// @BeforeClass
	// public static void initClass(){
	// Spring IOC
	// ApplicationContext context =
	// new ClassPathXmlApplicationContext(new String[] {"beans.xml"});
	// areaMgr = context.getBean("areaMgr", AreaMgr.class);
	// init
	// areaMgr = new AreaMgr();
	// areaMgr.setAreaDao(new AreaDaoIbatis());
	// }
	
	@Test
	public void testDeleteKmls() {
		// period of existing, format yyMMddHHmmss
		long period = 000000010000;	// 1 hour
		File kmlPath = new File("WebRoot/"+RequestKml.kmlPath);
		File files[]=kmlPath.listFiles(); 
		System.out.println(files.length);	
		long currentDate = Long.parseLong(new SimpleDateFormat("yyMMddHHmmss").format(new Date()));
		System.out.println(currentDate);		
		for(File f : files){
			String filename = f.getName();
			if(filename.length()==25 && filename.matches("\\d{12}-\\w{8}.kml")){
				long fileDate = Long.parseLong(filename.substring(0, 12));
				if(currentDate > fileDate + period){
					System.out.println("expire:"+filename);		
					f.delete();
				}
				else
					System.out.println("exist:"+filename);		
			}
				
		}
		
	}
	@Test
	public void testCalendar() {
		Calendar calendar = Calendar.getInstance();
		calendar.set(2010, 05, 27);
		SimpleDateFormat sdf = new SimpleDateFormat("E");
		System.out.println(calendar.getTime());
		System.out.println(sdf.format(calendar.getTime()));
		
		calendar.set(2010, 05, 28);
		System.out.println(calendar.getTime());
		System.out.println(sdf.format(calendar.getTime()));
	}
	
	@Test
	public void testFileName() {
		System.out.println(StringUtil.genFilename(new Date()));
	}

	@Test
	public void testQuery() {
		AreaDao areaDao = areaMgr.getAreaDao();
		// AreaDao dao = new AreaDaoJdbc();
		List<Area> as = areaDao.getAllAreas();
		// List<Area> as = dao.getAreasByPoint(9.17, 45.46);
		// // List<Area> ms = dao.getAreasByRect(1, 1, 96.5, 95.4);
		int num_person = 0;
		String person = "12533858@N04";
		for (Area a : as) {
			String coordinates = "\t";
			if (a.getGeom().getOrdinatesArray() != null)
				for (int i = 0; i < a.getGeom().getOrdinatesArray().length; i++) {
					coordinates += a.getGeom().getOrdinatesArray()[i] + ", ";
					if (i % 2 == 1)
						coordinates += "0\t";
				}

			System.out.println(a.getName() + " area:" + a.getArea() + "\t"
					+ "cx:" + a.getCenter().getX() + "\t" + "cy:"
					+ a.getCenter().getY());
			System.out.println(coordinates + "\n");
			System.out.println("total:" + areaDao.getTotalCount(a.getId()));
			// System.out.println(person+":"+dao.getPersonCount(a.getId(),
			// person));
			num_person += areaDao.getPersonCount(a.getId(), person);
		}
		System.out.println("person:" + num_person);
		System.out.println("hour:"
				+ areaDao.getHourCount("40 ", "2008-08-14@17"));
		System.out.println("day:" + areaDao.getDayCount("40 ", "2007-10-19"));
		System.out.println("month:" + areaDao.getMonthCount("40 ", "2007-10"));
		System.out.println("year:" + areaDao.getYearCount("40 ", "2007"));
	}

	@Test
	public void testKml2() throws Exception {
		for (int i = 0; i < 10; i++) {
			testKml();
		}
	}

	@Test
	public void testKml() throws Exception {
		List<String> years = new ArrayList<String>();
		List<String> months = new ArrayList<String>();
		List<String> days = new ArrayList<String>();
		List<String> hours = new ArrayList<String>();
		Set<String> weekdays = new HashSet<String>();

//		years.add("2005");
		years.add("2006");
//		years.add("2007");
//		months.add("01");
//		months.add("02");
//		months.add("03");
//		months.add("04");
		months.add("05");
//		months.add("06");
//		months.add("07");
//		months.add("08");
//		months.add("09");
//		months.add("10");
//		months.add("11");
//		months.add("12");
//		days.add("01");
//		days.add("02");
//		days.add("03");
//		days.add("04");
//		days.add("05");
		days.add("06");
		days.add("07");
		days.add("08");
		days.add("09");
		days.add("10");
		days.add("11");
		days.add("12");
		days.add("13");
		days.add("14");
//		days.add("15");
//		days.add("16");
//		days.add("17");
//		days.add("18");
//		days.add("19");
//		days.add("20");
//		days.add("21");
//		days.add("22");
//		days.add("23");
//		days.add("24");
//		days.add("25");
//		days.add("26");
//		days.add("27");
//		days.add("28");
//		days.add("29");
//		days.add("30");
//		days.add("31");
//		
//		hours.add("00");
//		hours.add("01");
//		hours.add("02");
//		hours.add("03");
//		hours.add("04");
//		hours.add("05");
//		hours.add("06");
//		hours.add("07");
//		hours.add("08");
//		hours.add("09");
//		hours.add("10");
//		hours.add("11");		
//		hours.add("12");
//		hours.add("13");
//		hours.add("14");
//		hours.add("15");
//		hours.add("16");
//		hours.add("17");
//		hours.add("18");
//		hours.add("19");
//		hours.add("20");
//		hours.add("21");
//		hours.add("22");
//		hours.add("23");
//		hours.add("24");
//		weekdays.add("Monday");
//		weekdays.add("Tuesday");
//		weekdays.add("Wednesday");
//		weekdays.add("Thursday");
//		weekdays.add("Friday");
//		weekdays.add("Saturday");
//		weekdays.add("Sunday");
		
		
		long init = System.currentTimeMillis();
		long start = System.currentTimeMillis();
		// List<Area> as = new ArrayList<Area>();
		// as.add(kmlMgr.getAreaById("100"));

		List<Area> as = areaMgr.getAllAreas();
		long count = System.currentTimeMillis();
		areaMgr.count(as, years, months, days, hours, weekdays);
		long end = System.currentTimeMillis();
		
		System.out.println(areaMgr.createMarkersXml(as, "Markers.xml"));
//		System.out.println("result:" + as.get(0).getCount());
//		System.out.println("init:" + (start - init));
//		System.out.println("select all:" + (count - start));
//		System.out.println("count time:" + (end - count));
	}

	@Test
	public void testXmlRequestSpeed() throws Exception {
		List<Long> times = new ArrayList<Long>();
		long start = System.currentTimeMillis();
		int iterations = 10;
		for (int i = 0; i < iterations; i++) {
			long startn = System.currentTimeMillis();
			if (i % 2 == 0)
				testXmlRequest();
			else
				testXmlRequest2();
			long endn = System.currentTimeMillis();
			times.add(endn - startn);
		}
		long end = System.currentTimeMillis();
		for (int i = 0; i < times.size(); i++) {
			System.out.println(i + ":" + times.get(i));
		}
		for (int i = 0; i < times.size(); i++) {
			System.out.println(i);
		}
		for (int i = 0; i < times.size(); i++) {
			System.out.println(times.get(i));
		}
		System.out.println("spent time total:" + (end - start));
		System.out.println("spent time avg:" + (end - start-times.get(0))/(iterations-1));
	}

	// #polygons:139
	// #queries = #years * #months * #days * #hours * #polygons
	// #query1:96*139=13344
	// #query2:93*139=12927
	// single jdbc
	// spent time:12563
	
	// single pool
	// spent time1:4453

	// pool
	// spent time1:4203
	// spent time2:2156
	// spent time3:62485

	// cache+pool
	// spent time1:4765
	// spent time2:422
	// spent time3:11781

	@Test
	public void testXmlRequest() throws Exception {
		BufferedReader br = new BufferedReader(new FileReader("request.xml"));
		StringBuffer xml = new StringBuffer();
		String thisLine;
		while ((thisLine = br.readLine()) != null) {
			xml.append(thisLine);
		}
		List<String> years = new ArrayList<String>();
		List<String> months = new ArrayList<String>();
		List<String> days = new ArrayList<String>();
		List<String> hours = new ArrayList<String>();
		Set<String> weekdays = new HashSet<String>();


		List<Area> as = areaMgr.getAllAreas();
		areaMgr.parseXmlRequest(as, StringUtil.FullMonth2Num(xml.toString()),
				years, months, days, hours, weekdays);
		System.out.println(areaMgr.createKml(as, "temp/areas1.kml"));

		br.close();
	}

	@Test
	public void testXmlRequest2() throws Exception {
		BufferedReader br = new BufferedReader(new FileReader("xml2.xml"));
		StringBuffer xml = new StringBuffer();
		String thisLine;
		while ((thisLine = br.readLine()) != null) {
			xml.append(thisLine);
		}
		List<Area> as = areaMgr.getAllAreas();
		areaMgr.parseXmlRequest2(as, StringUtil.ShortNum2Long(StringUtil
				.FullMonth2Num(xml.toString())));
		// for(Area a : as)
		// System.out.println(a.getTotalCount()+":"+a.getCount());
		System.out.println(areaMgr.createKml(as, "temp/areas2.kml"));
		br.close();

	}

	@Test
	public void testChart() throws IOException {

		List<String> months = new ArrayList<String>();
		months.add("2005-01");
		months.add("2005-02");
		months.add("2005-03");
		months.add("2005-04");
		months.add("2005-05");
		months.add("2005-06");
		months.add("2005-07");
		months.add("2005-08");
		months.add("2005-09");
		months.add("2005-10");
		months.add("2005-11");
		months.add("2005-12");
		months.add("2006-01");
		months.add("2006-02");
		months.add("2006-03");
		months.add("2006-04");
		months.add("2006-05");
		months.add("2006-06");
		months.add("2006-07");
		months.add("2006-08");
		months.add("2006-09");
		months.add("2006-10");
		months.add("2006-11");
		months.add("2006-12");
		months.add("2007-01");
		months.add("2007-02");
		months.add("2007-03");
		months.add("2007-04");
		months.add("2007-05");
		months.add("2007-06");
		months.add("2007-07");
		months.add("2007-08");
		months.add("2007-09");
		months.add("2007-10");
		months.add("2007-11");
		months.add("2007-12");

		AreaDao areaDao = areaMgr.getAreaDao();
		Area a = areaMgr.getAreaById("1  ");

		Map<String, Integer> cs = new HashMap<String, Integer>();
		for (String m : months) {
			System.out.println(m + areaDao.getMonthCount(a.getId(), m));
			cs.put(m, areaDao.getMonthCount(a.getId(), m));
		}
		areaMgr.createBarChart(cs);
	}
}
