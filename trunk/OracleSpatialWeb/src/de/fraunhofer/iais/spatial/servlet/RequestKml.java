package de.fraunhofer.iais.spatial.servlet;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jdom.JDOMException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import de.fraunhofer.iais.spatial.dao.AreaDao;
import de.fraunhofer.iais.spatial.dao.ibatis.AreaDaoIbatis;
import de.fraunhofer.iais.spatial.dao.jdbc.AreaDaoJdbc;
import de.fraunhofer.iais.spatial.entity.Area;
import de.fraunhofer.iais.spatial.service.AreaMgr;
import de.fraunhofer.iais.spatial.util.StringUtil;

public class RequestKml extends HttpServlet {

	// "/srv/tomcat6/webapps/OracleSpatialWeb/kml/";
	public static final String kmlPath = "kml/";

	private static AreaMgr areaMgr = null;

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		// web base path for local operation
		String localBasePath = System.getProperty("oraclespatialweb.root");
		// web base path for remote access
		String remoteBasePath = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath() + "/";

		response.setContentType("text/xml");
		// response.setContentType("application/vnd.google-earth.kml+xml");

		// Prevents caching
		response.setHeader("Cache-Control", "no-store"); // HTTP1.1
		response.setHeader("Pragma", "no-cache"); // HTTP1.0
		response.setDateHeader("Expires", 0); // proxy server

		String xml1 = request.getParameter("xml");
		String xml2 = request.getParameter("xml2");

		PrintWriter out = response.getWriter();

		if ((xml1 == null || xml1.equals("")) && (xml2 == null || xml2.equals(""))) {
			out.print("<response><msg>no parameters!</msg></response>");
			return;
		}

		String filenamePrefix = StringUtil.genFilename(new Date());

		List<Area> as = areaMgr.getAllAreas();
		
		BufferedWriter bw = new BufferedWriter(new FileWriter(localBasePath + kmlPath + filenamePrefix + ".xml"));
		

		if (xml1 != null && !xml1.equals("")) {
			List<String> years = new ArrayList<String>();
			List<String> months = new ArrayList<String>();
			List<String> days = new ArrayList<String>();
			List<String> hours = new ArrayList<String>();
			Set<String> weekdays = new HashSet<String>();
			try {
				System.out.println("xml1:" + xml1);
				areaMgr.parseXmlRequest(as, xml1, years, months, days, hours, weekdays);
				System.out.println("years:" + years.size());
				System.out.println("months:" + months.size());
				System.out.println("days:" + days.size());
				System.out.println("hours:" + hours.size());
				System.out.println("weekdays:" + weekdays.size());
				bw.write(xml1);
			} catch (JDOMException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if (xml2 != null && !xml2.equals("")) {
			try {
				System.out.println("xml2:" + xml2);
				areaMgr.parseXmlRequest2(as, xml2);
				bw.write(xml2);
			} catch (JDOMException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		bw.close();
		
		areaMgr.createKml(as, kmlPath + filenamePrefix);
		
		out.print("<?xml version='1.0' encoding='ISO-8859-1' ?>");
		out.print("<response><url>" + remoteBasePath + kmlPath + filenamePrefix + ".kml" + "</url></response>");
		out.flush();
		out.close();
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

	/**
	 * Initialization of the servlet. <br>
	 * 
	 * @throws ServletException
	 *             - if an error occurs
	 */
	public void init() throws ServletException {
		// areaMgr = new AreaMgr();
		// areaMgr.setAreaDao(new AreaDaoJdbc());

		// ApplicationContext context = new ClassPathXmlApplicationContext(
		// new String[] { "beans.xml", "schedulingContext-timer.xml" });
		// areaMgr = context.getBean("areaMgr", AreaMgr.class);
 
		areaMgr = WebApplicationContextUtils.getRequiredWebApplicationContext(this.getServletContext()).getBean("areaMgr", AreaMgr.class);
	}

}
