package de.fraunhofer.iais.spatial.web.servlet;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.support.WebApplicationContextUtils;

import de.fraunhofer.iais.spatial.dto.FlickrAreaDto;
import de.fraunhofer.iais.spatial.entity.FlickrArea;
import de.fraunhofer.iais.spatial.entity.FlickrAreaResult;
import de.fraunhofer.iais.spatial.exception.IllegalInputParameterException;
import de.fraunhofer.iais.spatial.service.FlickrAreaMgr;
import de.fraunhofer.iais.spatial.util.StringUtil;
import de.fraunhofer.iais.spatial.web.XmlServletCallback;
import de.fraunhofer.iais.spatial.web.XmlServletTemplate;

/**
 * Provides the Polygons data in kml format
 *
 * @author <a href="mailto:haolin.zhi@iais.fraunhofer.de">Haolin Zhi</A>
 * @author <a href="mailto:iulian.peca@iais.fraunhofer.de">Iulian Peca</a>
 *
 */
public class RequestKmlServlet extends HttpServlet {
	/**
	* Logger for this class
	*/
	private static final Logger logger = LoggerFactory.getLogger(RequestKmlServlet.class);

	private static final long serialVersionUID = -6814809670117597713L;

	// "/srv/tomcat6/webapps/OracleSpatialWeb/kml/";
	public static final String kmlPath = "kml/";

	private static FlickrAreaMgr areaMgr = null;

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		if (MapUtils.isEmpty(request.getParameterMap())) {
			response.sendRedirect("RequestKmlDemo.jsp");
			return;
		}

		new XmlServletTemplate().doExecute(request, response, logger, new XmlServletCallback() {

			@Override
			public void doInXmlServlet(HttpServletRequest request, Logger logger, Element rootElement, Element messageElement) throws Exception {
				String xml = request.getParameter("xml");
				if (StringUtils.isEmpty(xml)) {
					String errMsg = "ERROR: 'xml' parameter is missing!";
					messageElement.setText(errMsg);
					throw new IllegalInputParameterException(errMsg);
				}

				String filenamePrefix = StringUtil.genId();

				FlickrAreaDto areaDto = new FlickrAreaDto();

				logger.trace("doGet(HttpServletRequest, HttpServletResponse) - xml:" + xml); //$NON-NLS-1$

				areaMgr.parseXmlRequest(xml.toString(), areaDto);

				logger.info("doGet(HttpServletRequest, HttpServletResponse) - years:" + areaDto.getYears() + " |months:" + areaDto.getMonths() + "|days:" + areaDto.getDays() + "|hours:" + areaDto.getHours() + "|weekdays:" + areaDto.getWeekdays()); //$NON-NLS-1$

				List<FlickrArea> areas = null;

				if (areaDto.getPolygon() != null) {
					areas = areaMgr.getAreaDao().getAreasByPolygon(areaDto.getPolygon(), areaDto.getRadius());
				} else if (areaDto.getBoundaryRect() != null) {
					int size = areaMgr.getAreaDao().getAreasByRectSize(areaDto.getBoundaryRect().getMinX(), areaDto.getBoundaryRect().getMinY(), areaDto.getBoundaryRect().getMaxX(), areaDto.getBoundaryRect().getMaxY(), areaDto.getRadius());
					if (size > 2000) {
						String errMsg = "The maximun number of return polygons is exceeded! \n" + " Please choose a smaller Bounding Box <bounds> or a lower zoom value <zoom>";
						messageElement.setText(errMsg);
						throw new IllegalInputParameterException(errMsg);
					}
					areas = areaMgr.getAreaDao().getAreasByRect(areaDto.getBoundaryRect().getMinX(), areaDto.getBoundaryRect().getMinY(), areaDto.getBoundaryRect().getMaxX(), areaDto.getBoundaryRect().getMaxY(), areaDto.getRadius());
				}

				List<FlickrAreaResult> areaResults = areaMgr.createAreaResults(areas);
				areaMgr.countSelected(areaResults, areaDto);
				areaMgr.calculateHistograms(areaResults, areaDto);

				// web base path for local operation
//				String localBasePath = getServletContext().getRealPath("/");

				// web base path for remote access
				String remoteBasePath = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath() + "/";

				areaMgr.buildKmlFile(areaResults, kmlPath + filenamePrefix, areaDto.getRadius(), areaDto.getTransfromVector(), remoteBasePath, false);

				Element urlElement = new Element("url");
				rootElement.addContent(urlElement);
				urlElement.setText(remoteBasePath + kmlPath + filenamePrefix + ".kml");
				messageElement.setText("SUCCESS");
			}
		});
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
	}

}
