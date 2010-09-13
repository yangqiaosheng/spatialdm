package de.fraunhofer.iais.spatial.web.servlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jdom.JDOMException;
import org.springframework.web.context.support.WebApplicationContextUtils;

import de.fraunhofer.iais.spatial.entity.Area;
import de.fraunhofer.iais.spatial.service.AreaMgr;
import de.fraunhofer.iais.spatial.util.StringUtil;

public class RequestKmlServlet extends HttpServlet {
	/**
	* Logger for this class
	*/
	private static final Logger logger = LoggerFactory.getLogger(RequestKmlServlet.class);

	private static final long serialVersionUID = -6814809670117597713L;

	// "/srv/tomcat6/webapps/OracleSpatialWeb/kml/";
	public static final String kmlPath = "kml/"; 

	private static AreaMgr areaMgr = null;

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		// web base path for local operation
		String localBasePath = System.getProperty("oraclespatialweb.root");
		// web base path for remote access
		String remoteBasePath = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath() + "/";
//		String remoteBasePath = "http://kd-photomap.iais.fraunhofer.de/OracleSpatialWeb/";
		
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
				logger.debug("doGet(HttpServletRequest, HttpServletResponse) - xml1:" + xml1); //$NON-NLS-1$
				areaMgr.parseXmlRequest(as, xml1, years, months, days, hours, weekdays);
				logger.debug("doGet(HttpServletRequest, HttpServletResponse) - years:" + years.size() + " |months:" + months.size() + "|days:" + days.size() + "|hours:" + hours.size() + "|weekdays:" + weekdays.size()); //$NON-NLS-1$
				bw.write(xml1);
			} catch (JDOMException e) {
				logger.error("doGet(HttpServletRequest, HttpServletResponse)", e); //$NON-NLS-1$
			} catch (Exception e) {
				logger.error("doGet(HttpServletRequest, HttpServletResponse)", e); //$NON-NLS-1$
			}
		}

		if (xml2 != null && !xml2.equals("")) {
			try {
				logger.debug("doGet(HttpServletRequest, HttpServletResponse) - xml2:" + xml2); //$NON-NLS-1$
				areaMgr.parseXmlRequest2(as, xml2);
				bw.write(xml2);
			} catch (JDOMException e) {
				logger.error("doGet(HttpServletRequest, HttpServletResponse)", e); //$NON-NLS-1$
			} catch (Exception e) {
				logger.error("doGet(HttpServletRequest, HttpServletResponse)", e); //$NON-NLS-1$
			}
		}

		bw.close();

		areaMgr.createKml(as, kmlPath + filenamePrefix, remoteBasePath);

		out.print("<?xml version='1.0' encoding='ISO-8859-1' ?>");
		out.print("<response><url>" + remoteBasePath + kmlPath + filenamePrefix + ".kml" + "</url></response>");
		out.flush();
		out.close();
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

	/**
	 * Initialization of the servlet. <br>
	 * 
	 * @throws ServletException
	 *             - if an error occurs
	 */
	@Override
	public void init() throws ServletException {
		// areaMgr = new AreaMgr();
		// areaMgr.setAreaDao(new AreaDaoJdbc());

		// ApplicationContext context = new ClassPathXmlApplicationContext(
		// new String[] { "beans.xml", "schedulingContext-timer.xml" });
		// areaMgr = context.getBean("areaMgr", AreaMgr.class);

		areaMgr = WebApplicationContextUtils.getRequiredWebApplicationContext(this.getServletContext()).getBean("areaMgr", AreaMgr.class);
	}

}
