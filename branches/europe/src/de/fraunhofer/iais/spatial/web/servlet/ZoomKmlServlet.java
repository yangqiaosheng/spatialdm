package de.fraunhofer.iais.spatial.web.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.google.common.collect.Lists;

import de.fraunhofer.iais.spatial.dto.FlickrAreaDto;
import de.fraunhofer.iais.spatial.entity.FlickrArea;
import de.fraunhofer.iais.spatial.entity.FlickrAreaResult;
import de.fraunhofer.iais.spatial.service.FlickrAreaMgr;
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

	private static FlickrAreaMgr areaMgr = null;

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		if(MapUtils.isEmpty(request.getParameterMap())){
			response.sendRedirect("RequestKmlDemo.jsp");
			return;
		}

		// web base path for local operation
//		String localBasePath = getServletContext().getRealPath("/");
		// web base path for remote access
		String remoteBasePath = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath() + "/";
		//		String remoteBasePath = "http://kd-photomap.iais.fraunhofer.de/OracleSpatialWeb/";
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

		if (StringUtils.isEmpty(xml)) {
			messageElement.setText("ERROR: 'xml' parameter is missing!");
		} else if ("true".equals(persist) && request.getSession().getAttribute("areaDto") == null) {
			messageElement.setText("ERROR: please perform a query first!");
		} else {

			try {
				String filenamePrefix = StringUtil.genId();

				FlickrAreaDto areaDto = null;
				if ("true".equals(persist)) {
					logger.info("doGet(HttpServletRequest, HttpServletResponse) - persist:true");
					areaDto = SerializationUtils.clone((FlickrAreaDto) request.getSession().getAttribute("areaDto"));
				} else {
					areaDto = new FlickrAreaDto();
				}

				logger.debug("doGet(HttpServletRequest, HttpServletResponse) - xml:" + xml); //$NON-NLS-1$

				areaMgr.parseXmlRequest(StringUtil.FullMonth2Num(xml.toString()), areaDto);

				logger.info("doGet(HttpServletRequest, HttpServletResponse) - years:" + areaDto.getYears() + " |months:" + areaDto.getMonths() + "|days:" + areaDto.getDays() + "|hours:" + areaDto.getHours() + "|weekdays:" + areaDto.getWeekdays()); //$NON-NLS-1$

				List<FlickrArea> areas = null;

				if (areaDto.getPolygon() != null) {
					areas = areaMgr.getAreaDao().getAreasByPolygon(areaDto.getPolygon(), areaDto.getRadius());
				} else if (areaDto.getBoundaryRect() != null) {
					int size = areaMgr.getAreaDao().getAreasByRectSize(areaDto.getBoundaryRect().getMinX(), areaDto.getBoundaryRect().getMinY(), areaDto.getBoundaryRect().getMaxX(), areaDto.getBoundaryRect().getMaxY(), areaDto.getRadius());
					if(size > 2000){
						throw new IllegalArgumentException("The maximun number of return polygons is exceeded! \n" +
								" Please choose a smaller Bounding Box <bounds> or a lower zoom value <zoom>");
					}
					areas = areaMgr.getAreaDao().getAreasByRect(areaDto.getBoundaryRect().getMinX(), areaDto.getBoundaryRect().getMinY(), areaDto.getBoundaryRect().getMaxX(), areaDto.getBoundaryRect().getMaxY(), areaDto.getRadius());
				} else {
					areas = areaMgr.getAreaDao().getAllAreas(areaDto.getRadius());
				}
				List<FlickrAreaResult> areaResults = areaMgr.createAreaResults(areas);
				areaMgr.countSelected(areaResults, areaDto);
				areaMgr.calculateHistograms(areaResults, areaDto);
				areaMgr.buildKmlFile(areaResults, kmlPath + filenamePrefix, areaDto.getRadius(), remoteBasePath, false);

				Element urlElement = new Element("url");
				rootElement.addContent(urlElement);
				urlElement.setText(remoteBasePath + kmlPath + filenamePrefix + ".kml");
				request.getSession().setAttribute("areaDto", areaDto);
				messageElement.setText("SUCCESS");
			} catch (Exception e) {
				logger.error("doGet(HttpServletRequest, HttpServletResponse)", e); //$NON-NLS-1$
				messageElement.setText("ERROR: wrong input parameter!");
//				rootElement.addContent(new Element("exceptions").setText(StringUtil.printStackTrace2String(e)));
				rootElement.addContent(new Element("description").setText(e.getMessage()));
			}
		}

		out.print(XmlUtil.xml2String(document, true));
		out.flush();
		out.close();
		System.gc();
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
		areaMgr = WebApplicationContextUtils.getRequiredWebApplicationContext(this.getServletContext()).getBean("flickrAreaMgr", FlickrAreaMgr.class);
//		areaMgr.fillCache();
	}

}
