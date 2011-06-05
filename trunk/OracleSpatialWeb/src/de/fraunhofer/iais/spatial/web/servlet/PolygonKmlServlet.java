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

import org.apache.commons.lang.StringUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.support.WebApplicationContextUtils;

import de.fraunhofer.iais.spatial.dto.FlickrEuropeAreaDto;
import de.fraunhofer.iais.spatial.entity.FlickrArea;
import de.fraunhofer.iais.spatial.service.FlickrEuropeAreaMgr;
import de.fraunhofer.iais.spatial.util.StringUtil;
import de.fraunhofer.iais.spatial.util.XmlUtil;

@Deprecated
public class PolygonKmlServlet extends HttpServlet {
	/**
	* Logger for this class
	*/
	private static final Logger logger = LoggerFactory.getLogger(PolygonKmlServlet.class);

	private static final long serialVersionUID = -6814809670117597713L;

	private static FlickrEuropeAreaMgr areaMgr = null;

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		// web base path for remote access
		String remoteBasePath = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath() + "/";
		//		String remoteBasePath = "http://kd-photomap.iais.fraunhofer.de/OracleSpatialWeb/";
//		response.setContentType("text/xml");
		response.setContentType("application/vnd.google-earth.kml+xml");

		// Prevents caching
		response.setHeader("Cache-Control", "no-store"); // HTTP1.1
		response.setHeader("Pragma", "no-cache"); // HTTP1.0
		response.setDateHeader("Expires", 0); // proxy server

		String xml = request.getParameter("xml");

		PrintWriter out = response.getWriter();

		String kmlStr = "error";
		if (StringUtils.isNotEmpty(xml)) {

			try {
				String filenamePrefix = StringUtil.genFilename(new Date());

				FlickrEuropeAreaDto areaDto = new FlickrEuropeAreaDto();

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

				areaMgr.countSelected(areas, areaDto);
				kmlStr = areaMgr.buildKmlFile(areas, null, areaDto.getRadius(), remoteBasePath, false);

			} catch (Exception e) {
				logger.error("doGet(HttpServletRequest, HttpServletResponse)", e); //$NON-NLS-1$
			}
		}

		out.print(kmlStr);
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
		areaMgr = WebApplicationContextUtils.getRequiredWebApplicationContext(this.getServletContext()).getBean("flickrEuropeAreaMgr", FlickrEuropeAreaMgr.class);
//		areaMgr.fillCache();
	}

}
