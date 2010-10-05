package de.fraunhofer.iais.spatial.web.servlet;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jdom.Document;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.support.WebApplicationContextUtils;

import de.fraunhofer.iais.spatial.dto.FlickrDeWestAreaDto;
import de.fraunhofer.iais.spatial.entity.FlickrDeWestArea;
import de.fraunhofer.iais.spatial.service.FlickrDeWestAreaMgr;
import de.fraunhofer.iais.spatial.util.StringUtil;
import de.fraunhofer.iais.spatial.util.XmlUtil;

public class ZoomKmlServlet extends HttpServlet {
	/**
	* Logger for this class
	*/
	private static final Logger logger = LoggerFactory.getLogger(ZoomKmlServlet.class);

	private static final long serialVersionUID = -6814809670117597713L;

	// "/srv/tomcat6/webapps/OracleSpatialWeb/kml/";
	public static final String kmlPath = "kml/";

	private static FlickrDeWestAreaMgr areaMgr = null;

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		// web base path for local operation
		String localBasePath = System.getProperty("oraclespatialweb.root");
		// web base path for remote access
		String remoteBasePath = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath() + "/";
		//		String remoteBasePath = "http://kd-photomap.iais.fraunhofer.de/OracleSpatialWeb/";
		System.out.println(remoteBasePath);
		response.setContentType("text/xml");
		// response.setContentType("application/vnd.google-earth.kml+xml");

		// Prevents caching
		response.setHeader("Cache-Control", "no-store"); // HTTP1.1
		response.setHeader("Pragma", "no-cache"); // HTTP1.0
		response.setDateHeader("Expires", 0); // proxy server

		String xml = request.getParameter("xml");
		String persist = request.getParameter("persist");

		PrintWriter out = response.getWriter();

		Document document = new Document();
		Element rootElement = new Element("response");
		document.setRootElement(rootElement);
		Element messageElement = new Element("message");
		rootElement.addContent(messageElement);

		if (xml == null || xml.equals("")) {
			messageElement.setText("wrong input parameter!");
		} else if ("true".equals(persist) && request.getSession().getAttribute("areaDto") == null) {
			messageElement.setText("please do a query first!");
		} else {
			
			String filenamePrefix = StringUtil.genFilename(new Date());

//			BufferedWriter bw = new BufferedWriter(new FileWriter(localBasePath + kmlPath + filenamePrefix + ".xml"));
//			bw.write(xml);
//			bw.close();
			
			FlickrDeWestAreaDto areaDto = new FlickrDeWestAreaDto();
			if("true".equals(persist)){
				logger.debug("doGet(HttpServletRequest, HttpServletResponse) - persist:true" );
				areaDto = (FlickrDeWestAreaDto) request.getSession().getAttribute("areaDto");
			} 
			
			try {
				logger.debug("doGet(HttpServletRequest, HttpServletResponse) - xml:" + xml); //$NON-NLS-1$

				areaMgr.parseXmlRequest(StringUtil.FullMonth2Num(xml.toString()), areaDto);
				
				logger.debug("doGet(HttpServletRequest, HttpServletResponse) - years:" + areaDto.getYears() + " |months:" + areaDto.getMonths() + "|days:" + areaDto.getDays() + "|hours:" + areaDto.getHours() + "|weekdays:" + areaDto.getWeekdays()); //$NON-NLS-1$

				List<FlickrDeWestArea> as = null;
				if (areaDto.getBoundaryRect() == null) {
					as = areaMgr.getAreaDao().getAllAreas(areaDto.getRadius());
				} else {
					as = areaMgr.getAreaDao().getAreasByRect(areaDto.getBoundaryRect().getMinX(), areaDto.getBoundaryRect().getMinY(), areaDto.getBoundaryRect().getMaxX(), areaDto.getBoundaryRect().getMaxY(), areaDto.getRadius());
				}
				areaMgr.count(as, areaDto);
				areaMgr.createKml(as, kmlPath + filenamePrefix, areaDto.getRadius(), remoteBasePath);

				Element urlElement = new Element("url");
				rootElement.addContent(urlElement);
				urlElement.setText(remoteBasePath + kmlPath + filenamePrefix + ".kml");
				request.getSession().setAttribute("areaDto", areaDto);
				messageElement.setText("SUCCESS");
			} catch (Exception e) {
				logger.error("doGet(HttpServletRequest, HttpServletResponse)", e); //$NON-NLS-1$
				messageElement.setText("wrong input parameter!");
				rootElement.addContent(new Element("exceptions").setText(StringUtil.printStackTrace2String(e)));
			}
		}

		out.print(XmlUtil.xml2String(document, true));
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
		areaMgr = WebApplicationContextUtils.getRequiredWebApplicationContext(this.getServletContext()).getBean("flickrDeWestAreaMgr", FlickrDeWestAreaMgr.class);
	}

}
