package de.fraunhofer.iais.spatial.test;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import oracle.spatial.geometry.JGeometry;

import org.jdom.CDATA;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.testng.internal.thread.CountDownAdapter;

import de.fraunhofer.iais.spatial.dao.FlickrDeWestAreaDao;
import de.fraunhofer.iais.spatial.dao.jdbc.FlickrDeWestAreaDaoJdbc;
import de.fraunhofer.iais.spatial.dto.FlickrDeWestAreaDto;
import de.fraunhofer.iais.spatial.entity.FlickrDeWestArea;
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
		FlickrDeWestAreaDao areaDao = new FlickrDeWestAreaDaoJdbc();
		FlickrDeWestArea a = areaDao.getAreaById(1, FlickrDeWestArea.Radius._80000);

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
		FlickrDeWestAreaDao areaDao = new FlickrDeWestAreaDaoJdbc();
//		List<FlickrDeWestArea> as = areaDao.getAllAreas(Radius._10000);
// 		List<FlickrDeWestArea> as = areaDao.getAreasByPoint(8.83, 50.58, Radius._5000);
		List<FlickrDeWestArea> as = areaDao.getAreasByRect(1, 1, 96.5, 95.4, Radius._80000);
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
//		List<FlickrDeWestArea> as = areaMgr.getAllAreas(areaDto.getRadius());
		List<FlickrDeWestArea> as = areaMgr.getAreasByRect(areaDto.getBoundaryRect().getMinX(), areaDto.getBoundaryRect().getMinY(), areaDto.getBoundaryRect().getMaxX(), areaDto.getBoundaryRect().getMaxY(), areaDto.getRadius());
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
