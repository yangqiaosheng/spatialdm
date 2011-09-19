package de.fraunhofer.iais.spatial.web.servlet;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeoutException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.google.common.collect.Lists;

import de.fraunhofer.iais.spatial.dto.FlickrAreaDto;
import de.fraunhofer.iais.spatial.dto.SessionMutex;
import de.fraunhofer.iais.spatial.entity.FlickrArea;
import de.fraunhofer.iais.spatial.entity.FlickrArea.Radius;
import de.fraunhofer.iais.spatial.entity.FlickrAreaResult;
import de.fraunhofer.iais.spatial.service.FlickrAreaCancelableJob;
import de.fraunhofer.iais.spatial.service.FlickrAreaMgr;
import de.fraunhofer.iais.spatial.util.StringUtil;
import de.fraunhofer.iais.spatial.util.XmlUtil;

public class PolygonXmlServlet extends HttpServlet {
	/**
	* Logger for this class
	*/
	private static final Logger logger = LoggerFactory.getLogger(PolygonXmlServlet.class);
	private static final long serialVersionUID = -6814809670117597713L;

	private static FlickrAreaMgr areaMgr = null;

	/**
	 * The doGet method of the servlet. <br>
	 *
	 * This method is called when a form has its tag value method equals to get.
	 *
	 * @param request the request send by the client to the server
	 * @param response the response send by the server to the client
	 * @throws ServletException if an error occurred
	 * @throws IOException if an error occurred
	 */
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		Date timestamp = new Date();
		timestamp.setTime(NumberUtils.toLong(request.getParameter("timestamp")));

		HttpSession session = request.getSession();
		SessionMutex sessionMutex = null;
		synchronized (this) {
			if (session.getAttribute(HistrogramsDataServlet.HISTOGRAM_SESSION_ID) == null) {
				session.setAttribute(HistrogramsDataServlet.HISTOGRAM_SESSION_ID, new SessionMutex(timestamp));
			}
			sessionMutex = (SessionMutex) session.getAttribute(HistrogramsDataServlet.HISTOGRAM_SESSION_ID);
			if (sessionMutex.getTimestamp().before(timestamp)) {
				sessionMutex.setTimestamp(timestamp);
			}
		}

		response.setContentType("text/xml");
		// response.setContentType("application/vnd.google-earth.kml+xml");

		// Prevents caching
		response.setHeader("Cache-Control", "no-store"); // HTTP1.1
		response.setHeader("Pragma", "no-cache"); // HTTP1.0
		response.setDateHeader("Expires", 0); // proxy server

		String xml = request.getParameter("xml");
		String persist = request.getParameter("persist");

		PrintWriter out = response.getWriter();

		String responseStr = "";
		Document document = new Document();
		Element rootElement = new Element("response");
		document.setRootElement(rootElement);
		Element messageElement = new Element("message");
		rootElement.addContent(messageElement);

		logger.debug("doGet(HttpServletRequest, HttpServletResponse) - xml:" + xml); //$NON-NLS-1$

		if (StringUtils.isEmpty(xml)) {
			messageElement.setText("ERROR: wrong input parameter!");
			responseStr = XmlUtil.xml2String(document, true);
		} else {
			FlickrAreaDto areaDto = null;
			if ("true".equals(persist) && request.getSession().getAttribute("areaDto") != null) {
				logger.info("doGet(HttpServletRequest, HttpServletResponse) - persist:true");
				areaDto = SerializationUtils.clone((FlickrAreaDto) request.getSession().getAttribute("areaDto"));
			} else {
				areaDto = new FlickrAreaDto();
			}

			try {
				areaMgr.parseXmlRequest(StringUtil.FullMonth2Num(xml.toString()), areaDto);
				logger.info("doGet(HttpServletRequest, HttpServletResponse) - years:" + areaDto.getYears() + " |months:" + areaDto.getMonths() + "|days:" + areaDto.getDays() + "|hours:" + areaDto.getHours() + "|weekdays:" + areaDto.getWeekdays()); //$NON-NLS-1$

				if(session.getAttribute(HistrogramsDataServlet.HISTOGRAM_SESSION_LOCK) != null){
					int waitSec = 5;
					for (int i = 1; i <= waitSec; i++) {
						Thread.sleep(1000);
						if (session.getAttribute(HistrogramsDataServlet.HISTOGRAM_SESSION_LOCK) == null && sessionMutex.getTimestamp().equals(timestamp)) {
							break;
						} else {
							if (i == waitSec) {
								throw new TimeoutException("Blocked until:" + waitSec + "s");
							}
							if (!sessionMutex.getTimestamp().equals(timestamp)) {
								throw new InterruptedException("Interrupted before");
							}
						}
					}
				}

				session.setAttribute(HistrogramsDataServlet.HISTOGRAM_SESSION_LOCK, new SessionMutex(timestamp));
				int size = areaMgr.getAreaDao().getAreasByRectSize(areaDto.getBoundaryRect().getMinX(), areaDto.getBoundaryRect().getMinY(), areaDto.getBoundaryRect().getMaxX(), areaDto.getBoundaryRect().getMaxY(), areaDto.getRadius());
				if (size > 2000) {
					throw new IllegalArgumentException("The maximun number of return polygons is exceeded! \n" + " Please choose a smaller Bounding Box <bounds> or a lower zoom value <zoom>");
				}

				List<FlickrArea> areas = areaMgr.getAreaCancelableJob().getAreasByRect(timestamp, sessionMutex, areaDto.getBoundaryRect().getMinX(), areaDto.getBoundaryRect().getMinY(), areaDto.getBoundaryRect().getMaxX(), areaDto.getBoundaryRect().getMaxY(), areaDto.getRadius());

				List<FlickrAreaResult> areaResults = Lists.newArrayList();
				areaMgr.getAreaCancelableJob().countSelected(timestamp, sessionMutex, areaResults, areas, areaDto);
				responseStr = createXml(areaResults, null, areaDto, areaMgr.getAreaDao().getTotalEuropePhotoNum());
				request.getSession().setAttribute("areaDto", areaDto);
				session.removeAttribute(HistrogramsDataServlet.HISTOGRAM_SESSION_ID);
			} catch (TimeoutException e) {
				messageElement.setText("INFO: Rejected until Timeout!");
				rootElement.addContent(new Element("exceptions").setText(StringUtil.printStackTrace2String(e)));
				rootElement.addContent(new Element("description").setText(e.getMessage()));
				responseStr = XmlUtil.xml2String(document, true);
			} catch (InterruptedException e) {
				messageElement.setText("INFO: interupted by another query!");
//				rootElement.addContent(new Element("exceptions").setText(StringUtil.printStackTrace2String(e)));
//				rootElement.addContent(new Element("description").setText(e.getMessage()));
				responseStr = XmlUtil.xml2String(document, true);
			} catch (Exception e) {
				logger.error("doGet(HttpServletRequest, HttpServletResponse)", e); //$NON-NLS-1$
				messageElement.setText("ERROR: wrong input parameter!");
//				rootElement.addContent(new Element("exceptions").setText(StringUtil.printStackTrace2String(e)));
				rootElement.addContent(new Element("description").setText(e.getMessage()));
				responseStr = XmlUtil.xml2String(document, true);
			} finally {
				session.removeAttribute(HistrogramsDataServlet.HISTOGRAM_SESSION_LOCK);
			}
		}

		out.print(responseStr);
		out.flush();
		out.close();
		System.gc();
	}

	private String createXml(List<FlickrAreaResult> areaResults, String filenamePrefix, FlickrAreaDto areaDto, long totalPhotoNum) throws UnsupportedEncodingException {
		Document document = new Document();
		Element rootElement = new Element("polygons");
		document.setRootElement(rootElement);
		rootElement.setAttribute("polygonsNum", String.valueOf(areaResults.size()));
		rootElement.setAttribute("zoom", String.valueOf(areaDto.getZoom()));
		rootElement.setAttribute("radius", areaDto.getRadius().toString());
		rootElement.setAttribute("wholeDbPhotosNum", String.valueOf(totalPhotoNum));

		for (FlickrAreaResult areaResult : areaResults) {
			FlickrArea area = areaResult.getArea();
			Element polygonElement = new Element("polygon");
			rootElement.addContent(polygonElement);
			polygonElement.setAttribute("id", String.valueOf(area.getId()));
			polygonElement.setAttribute("total", String.valueOf(area.getTotalCount()));
			polygonElement.setAttribute("select", String.valueOf(areaResult.getSelectCount()));

			Element lineElement = new Element("line");
			polygonElement.addContent(lineElement);
			lineElement.setAttribute("width", "1");

			List<Point2D> geom = area.getGeom();
			for (Point2D point : geom) {
				Element pointElement = new Element("point");
				lineElement.addContent(pointElement);
				pointElement.setAttribute("lng", String.valueOf(point.getX()));
				pointElement.setAttribute("lat", String.valueOf(point.getY()));
			}

			Element centerElement = new Element("center");
			polygonElement.addContent(centerElement);
			Element pointElement = new Element("point");
			centerElement.addContent(pointElement);
			pointElement.setAttribute("lng", String.valueOf(area.getCenter().getX()));
			pointElement.setAttribute("lat", String.valueOf(area.getCenter().getY()));
		}

		if (filenamePrefix != null) {
			XmlUtil.xml2File(document, filenamePrefix + ".xml", false);
		}

		return XmlUtil.xml2String(document, false);
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