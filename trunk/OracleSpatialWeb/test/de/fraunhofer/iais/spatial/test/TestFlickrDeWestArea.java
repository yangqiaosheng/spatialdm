package de.fraunhofer.iais.spatial.test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import de.fraunhofer.iais.spatial.dao.FlickrDeWestAreaDao;
import de.fraunhofer.iais.spatial.dao.jdbc.FlickrDeWestAreaDaoJdbc;
import de.fraunhofer.iais.spatial.dto.FlickrDeWestAreaDto;
import de.fraunhofer.iais.spatial.entity.FlickrDeWestArea;
import de.fraunhofer.iais.spatial.entity.FlickrDeWestPhoto;
import de.fraunhofer.iais.spatial.entity.FlickrDeWestArea.Radius;
import de.fraunhofer.iais.spatial.service.FlickrDeWestAreaMgr;
import de.fraunhofer.iais.spatial.util.StringUtil;

public class TestFlickrDeWestArea {

	private static FlickrDeWestAreaMgr areaMgr = null;

	@BeforeClass
	public static void initClass() {
		// Spring IOC
		ApplicationContext context = new ClassPathXmlApplicationContext(new String[] { "beans.xml" });
		areaMgr = context.getBean("flickrDeWestAreaMgr", FlickrDeWestAreaMgr.class);
		System.setProperty("oraclespatialweb.root", "C:/java_file/eclipse/MyEclipse/OracleSpatialWeb/");
		System.out.println("oraclespatialweb.root:" + System.getProperty("oraclespatialweb.root"));
	}

	@Test
	public void testJdbcDao1() {
		FlickrDeWestArea a = areaMgr.getAreaDao().getAreaById(1, FlickrDeWestArea.Radius._80000);

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
		List<FlickrDeWestArea> as = areaMgr.getAreaDao().getAreasByRect(1, 1, 96.5, 95.4, Radius._80000);
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
			FlickrDeWestPhoto p = areaMgr.getAreaDao().getPhoto(1, Radius._80000, "2007-08-11@13", i);
			if (p != null) {
				System.out.print("PHOTO_ID:" + p.getId());
				System.out.print("\tAreaid:" + p.getArea().getId());
				System.out.print("\tRadius:" + p.getArea().getRadius());
				System.out.print("\tarea:" + p.getArea().getArea());
				System.out.print("\tDT:" + p.getDate());
				System.out.print("\tLATITUDE:" + p.getLatitude());
				System.out.print("\tLONGITUDE:" + p.getLongitude());
				System.out.print("\tPERSON:" + p.getPerson());
				System.out.print("\tRAWTAGS:" + p.getRawTags());
				System.out.print("\tTITLE:" + p.getTitle());
				System.out.print("\tSMALLURL:" + p.getSmallUrl());
				System.out.println("\tVIEWED:" + p.getViewed());
			}
		}
	}
	
	@Test
	public void testPhoto2() {
		SortedSet<String> hours = new TreeSet<String>(); 
		hours.add("2007-08-11@13");
		hours.add("2007-08-11@11");
		hours.add("2007-05-09@13");
		
		List<FlickrDeWestPhoto> photos = areaMgr.getAreaDao().getPhotos(1, Radius._80000, hours, 20);
		for (FlickrDeWestPhoto p : photos){
			if (p != null) {
				System.out.print("PHOTO_ID:" + p.getId());
				System.out.print("\tAreaid:" + p.getArea().getId());
				System.out.print("\tRadius:" + p.getArea().getRadius());
				System.out.print("\tarea:" + p.getArea().getArea());
				System.out.print("\tDT:" + p.getDate());
				System.out.print("\tLATITUDE:" + p.getLatitude());
				System.out.print("\tLONGITUDE:" + p.getLongitude());
				System.out.print("\tPERSON:" + p.getPerson());
				System.out.print("\tRAWTAGS:" + p.getRawTags());
				System.out.print("\tTITLE:" + p.getTitle());
				System.out.print("\tSMALLURL:" + p.getSmallUrl());
				System.out.println("\tVIEWED:" + p.getViewed());
			}
		}
	}

	@Test
	public void testTree(){
		SortedSet<String> s = new TreeSet<String>(); 
		s.add("2007-05-08");
		s.add("2007-06-08");
		s.add("2007-05-09");
		s.add("2007-05-18");
		s.add("2009-01-08");
		s.add("2008-01-08");
		s.add("1995-01-08");
		while (s.size() > 0){
			System.out.println(s.last());
			s.remove(s.last());
		}
	}
	
	@Test
	public void testKml1() throws Exception {
		FlickrDeWestAreaDto areaDto = new FlickrDeWestAreaDto();
		BufferedReader br = new BufferedReader(new FileReader("FlickrDeWestRequest1.xml"));
		StringBuffer xml = new StringBuffer();
		String thisLine;
		while ((thisLine = br.readLine()) != null) {
			xml.append(thisLine);
		}
		areaMgr.parseXmlRequest1(StringUtil.FullMonth2Num(xml.toString()), areaDto);
		System.out.println("radius:" + areaDto.getRadius());
		System.out.println("zoom:" + areaDto.getZoom());
		System.out.println("center:" + areaDto.getCenter().getX() + "," + areaDto.getCenter().getY());
		System.out.println("boundaryRect:" + areaDto.getBoundaryRect().getMinX() + "," + areaDto.getBoundaryRect().getMinY() + "," + areaDto.getBoundaryRect().getMaxX() + "," + areaDto.getBoundaryRect().getMaxY());
//	 	List<FlickrDeWestArea> as = areaMgr.getAllAreas(areaDto.getRadius());
		List<FlickrDeWestArea> as = areaMgr.getAreaDao().getAreasByRect(areaDto.getBoundaryRect().getMinX(), areaDto.getBoundaryRect().getMinY(), areaDto.getBoundaryRect().getMaxX(), areaDto.getBoundaryRect().getMaxY(), areaDto.getRadius());
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

}
