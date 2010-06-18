package de.fraunhofer.iais.spatial.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jdom.JDOMException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import de.fraunhofer.iais.spatial.dao.AreaDao;
import de.fraunhofer.iais.spatial.dao.ibatis.AreaDaoIbatis;
import de.fraunhofer.iais.spatial.entity.Area;
import de.fraunhofer.iais.spatial.service.AreaMgr;
import de.fraunhofer.iais.spatial.util.StringTF;

public class RequestKml extends HttpServlet {

	private static AreaMgr areaMgr = null;

	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		System.out.println("request:");
		// response.setContentType("text/xml");
		response.setContentType("application/vnd.google-earth.kml+xml");
		// Prevents caching
		response.setHeader("Cache-Control", "no-store"); // HTTP1.1
		response.setHeader("Pragma", "no-cache"); // HTTP1.0
		response.setDateHeader("Expires", 0); // proxy server

		String xml = request.getParameter("xml");
		String xml2 = request.getParameter("xml2");
		PrintWriter out = response.getWriter();

		List<Area> as = areaMgr.getAllAreas();

		if (xml != null && !xml.equals("")) {
			List<String> years = new ArrayList<String>();
			List<String> months = new ArrayList<String>();
			List<String> days = new ArrayList<String>();
			List<String> hours = new ArrayList<String>();
			try {
				System.out.println("xml:" + xml);
				areaMgr.parseXmlRequest(as, StringTF.FullMonth2Num(xml), years,
						months, days, hours);
				System.out.println("years:" + years.size());
				System.out.println("months:" + months.size());
				System.out.println("days:" + days.size());
				System.out.println("hours:" + hours.size());
			} catch (JDOMException e) {
				e.printStackTrace();
			}
			out.print(StringTF.escapeHtml(areaMgr.createKml(as, "/srv/tomcat6/webapps/OracleSpatialWeb/kml/areas1.kml").substring(0,1000)));
		}

		if (xml2 != null && !xml2.equals("")) {
			try {
				System.out.println("xml2:" + xml2);
				areaMgr.parseXmlRequest2(as, StringTF.ShortNum2Long(StringTF
						.FullMonth2Num(xml2)));
			} catch (JDOMException e) {
				e.printStackTrace();
			}
			out.print(StringTF.escapeHtml(areaMgr.createKml(as, "/srv/tomcat6/webapps/OracleSpatialWeb/kml/areas2.kml").substring(0,1000)));
		}

		out.flush();
		out.close();
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}

	/**
	 * Initialization of the servlet. <br>
	 * 
	 * @throws ServletException
	 *             - if an error occurs
	 */
	public void init() throws ServletException {
		areaMgr = new AreaMgr();
		areaMgr.setAreaDao(new AreaDaoIbatis());
//		ApplicationContext context = new ClassPathXmlApplicationContext(
//				new String[] { "beans.xml" });
//		areaMgr = context.getBean("areaMgr", AreaMgr.class);
	}

}
