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

import de.fraunhofer.iais.spatial.dto.FlickrAreaDto;
import de.fraunhofer.iais.spatial.dto.SessionMutex;
import de.fraunhofer.iais.spatial.entity.FlickrArea;
import de.fraunhofer.iais.spatial.entity.FlickrAreaResult;
import de.fraunhofer.iais.spatial.exception.IllegalInputParameterException;
import de.fraunhofer.iais.spatial.service.FlickrAreaMgr;
import de.fraunhofer.iais.spatial.util.StringUtil;
import de.fraunhofer.iais.spatial.util.XmlUtil;
import de.fraunhofer.iais.spatial.web.XmlServletCallback;
import de.fraunhofer.iais.spatial.web.XmlServletTemplate;

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

		new XmlServletTemplate().doExecute(request, response, logger, new XmlServletCallback() {

			@Override
			public void doInXmlServlet(HttpServletRequest request, HttpServletResponse response, Logger logger, Element rootElement, Element messageElement, XmlServletCallback callback) throws Exception {
				String xml = request.getParameter("xml");
				String persist = request.getParameter("persist");

				if (StringUtils.isEmpty(xml)) {
					String errMsg = "ERROR: 'xml' parameter is missing!";
					messageElement.setText(errMsg);
					throw new IllegalInputParameterException(errMsg);
				} else if ("true".equals(persist) && request.getSession().getAttribute("areaDto") == null) {
					String errMsg = "ERROR: session has time out, please perform a query first!";
					messageElement.setText(errMsg);
					throw new IllegalInputParameterException(errMsg);
				}

				logger.trace("doGet(HttpServletRequest, HttpServletResponse) - xml:" + xml); //$NON-NLS-1$
				FlickrAreaDto areaDto = null;
				if ("true".equals(persist) && request.getSession().getAttribute("areaDto") != null) {
					logger.trace("doGet(HttpServletRequest, HttpServletResponse) - persist:true");
					areaDto = SerializationUtils.clone((FlickrAreaDto) request.getSession().getAttribute("areaDto"));
				} else {
					areaDto = new FlickrAreaDto();
				}

				HttpSession session = request.getSession();
				Date timestamp = new Date();
				timestamp.setTime(NumberUtils.toLong(request.getParameter("timestamp")));
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

				try {
					areaMgr.parseXmlRequest(StringUtil.FullMonth2Num(xml.toString()), areaDto);

					if (session.getAttribute(HistrogramsDataServlet.HISTOGRAM_SESSION_LOCK) != null) {
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

					int size = areaMgr.getAreaDao().getAreasByRectSize(areaDto.getBoundaryRect().getMinX(), areaDto.getBoundaryRect().getMinY(), areaDto.getBoundaryRect().getMaxX(), areaDto.getBoundaryRect().getMaxY(), areaDto.getRadius(),
							areaDto.isCrossDateLine());
					if (size > 2000) {
						throw new IllegalArgumentException("The maximun number of return polygons is exceeded! \n" + " Please choose a smaller Bounding Box <bounds> or a lower zoom value <zoom>");
					}

					List<FlickrArea> areas = null;
					if (areaDto.getZoom() > 2) {
						areas = areaMgr.getAreaCancelableJob().getAreasByRect(timestamp, sessionMutex, areaDto.getBoundaryRect().getMinX(), areaDto.getBoundaryRect().getMinY(), areaDto.getBoundaryRect().getMaxX(), areaDto.getBoundaryRect().getMaxY(),
								areaDto.getRadius(), areaDto.isCrossDateLine());
					} else {
						areas = areaMgr.getAreaCancelableJob().getAllAreas(timestamp, sessionMutex, areaDto.getRadius());
					}

					List<FlickrAreaResult> areaResults = areaMgr.createAreaResults(areas);
					areaMgr.getAreaCancelableJob().countSelected(timestamp, sessionMutex, areaResults, areaDto);
					addAreas2Xml(areaResults, rootElement, areaDto, areaMgr.getAreaDao().getTotalWorldPhotoNum());

					session.setAttribute("areaDto", areaDto);
					session.removeAttribute(HistrogramsDataServlet.HISTOGRAM_SESSION_ID);
					messageElement.setText("SUCCESS");
				} catch (TimeoutException e) {
					messageElement.setText("INFO: Rejected until Timeout!");
					rootElement.addContent(new Element("exceptions").setText(StringUtil.printStackTrace2String(e)));
					rootElement.addContent(new Element("description").setText(e.getMessage()));
				} catch (InterruptedException e) {
					messageElement.setText("INFO: interupted by another query!");
//					rootElement.addContent(new Element("exceptions").setText(StringUtil.printStackTrace2String(e)));
//					rootElement.addContent(new Element("description").setText(e.getMessage()));
				} finally {
					session.removeAttribute(HistrogramsDataServlet.HISTOGRAM_SESSION_LOCK);
				}
			}
		});
	}

	/*
		@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		response.setContentType("text/xml");
		// response.setContentType("application/vnd.google-earth.kml+xml");

		// Prevents caching
		response.setHeader("Cache-Control", "no-store"); // HTTP1.1
		response.setHeader("Pragma", "no-cache"); // HTTP1.0
		response.setDateHeader("Expires", 0); // proxy server
		PrintWriter out = response.getWriter();

		Document document = new Document();
		Element rootElement = new Element("response");
		document.setRootElement(rootElement);
		Element messageElement = new Element("message");
		rootElement.addContent(messageElement);

		try {
			String xml = request.getParameter("xml");
			String persist = request.getParameter("persist");

			if (StringUtils.isEmpty(xml)) {
				String errMsg = "ERROR: 'xml' parameter is missing!";
				messageElement.setText(errMsg);
				throw new IllegalInputParameterException(errMsg);
			} else if ("true".equals(persist) && request.getSession().getAttribute("areaDto") == null) {
				String errMsg = "ERROR: session has time out, please perform a query first!";
				messageElement.setText(errMsg);
				throw new IllegalInputParameterException(errMsg);
			}

			logger.trace("doGet(HttpServletRequest, HttpServletResponse) - xml:" + xml); //$NON-NLS-1$
			FlickrAreaDto areaDto = null;
			if ("true".equals(persist) && request.getSession().getAttribute("areaDto") != null) {
				logger.trace("doGet(HttpServletRequest, HttpServletResponse) - persist:true");
				areaDto = SerializationUtils.clone((FlickrAreaDto) request.getSession().getAttribute("areaDto"));
			} else {
				areaDto = new FlickrAreaDto();
			}

			HttpSession session = request.getSession();
			Date timestamp = new Date();
			timestamp.setTime(NumberUtils.toLong(request.getParameter("timestamp")));
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

			try {
				areaMgr.parseXmlRequest(StringUtil.FullMonth2Num(xml.toString()), areaDto);

				if (session.getAttribute(HistrogramsDataServlet.HISTOGRAM_SESSION_LOCK) != null) {
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

				int size = areaMgr.getAreaDao().getAreasByRectSize(areaDto.getBoundaryRect().getMinX(), areaDto.getBoundaryRect().getMinY(), areaDto.getBoundaryRect().getMaxX(), areaDto.getBoundaryRect().getMaxY(), areaDto.getRadius(),
						areaDto.isCrossDateLine());
				if (size > 2000) {
					throw new IllegalArgumentException("The maximun number of return polygons is exceeded! \n" + " Please choose a smaller Bounding Box <bounds> or a lower zoom value <zoom>");
				}

				List<FlickrArea> areas = null;
				if (areaDto.getZoom() > 2) {
					areas = areaMgr.getAreaCancelableJob().getAreasByRect(timestamp, sessionMutex, areaDto.getBoundaryRect().getMinX(), areaDto.getBoundaryRect().getMinY(), areaDto.getBoundaryRect().getMaxX(), areaDto.getBoundaryRect().getMaxY(),
							areaDto.getRadius(), areaDto.isCrossDateLine());
				} else {
					areas = areaMgr.getAreaCancelableJob().getAllAreas(timestamp, sessionMutex, areaDto.getRadius());
				}

				List<FlickrAreaResult> areaResults = areaMgr.createAreaResults(areas);
				areaMgr.getAreaCancelableJob().countSelected(timestamp, sessionMutex, areaResults, areaDto);
				addAreas2Xml(areaResults, rootElement, areaDto, areaMgr.getAreaDao().getTotalWorldPhotoNum());

				session.setAttribute("areaDto", areaDto);
				session.removeAttribute(HistrogramsDataServlet.HISTOGRAM_SESSION_ID);
				messageElement.setText("SUCCESS");

			} catch (TimeoutException e) {
				messageElement.setText("INFO: Rejected until Timeout!");
				rootElement.addContent(new Element("exceptions").setText(StringUtil.printStackTrace2String(e)));
				rootElement.addContent(new Element("description").setText(e.getMessage()));
			} catch (InterruptedException e) {
				messageElement.setText("INFO: interupted by another query!");
//				rootElement.addContent(new Element("exceptions").setText(StringUtil.printStackTrace2String(e)));
//				rootElement.addContent(new Element("description").setText(e.getMessage()));
			} finally {
				session.removeAttribute(HistrogramsDataServlet.HISTOGRAM_SESSION_LOCK);
			}

		} catch (IllegalInputParameterException e) {
			rootElement.addContent(new Element("description").setText(e.getMessage()));
		} catch (Exception e) {
			logger.error("doGet(HttpServletRequest, HttpServletResponse)", e); //$NON-NLS-1$
			messageElement.setText("ERROR: wrong input parameter!");
//			rootElement.addContent(new Element("exceptions").setText(StringUtil.printStackTrace2String(e)));
			rootElement.addContent(new Element("description").setText(e.getMessage()));
		}

		out.print(XmlUtil.xml2String(document, true));
		out.flush();
		out.close();
		System.gc();
	*/

	private void addAreas2Xml(List<FlickrAreaResult> areaResults, Element rootElement, FlickrAreaDto areaDto, long totalPhotoNum) throws UnsupportedEncodingException {
		Element polygonsElement = new Element("polygons");
		rootElement.addContent(polygonsElement);

		polygonsElement.setAttribute("polygonsNum", String.valueOf(areaResults.size()));
		polygonsElement.setAttribute("zoom", String.valueOf(areaDto.getZoom()));
		polygonsElement.setAttribute("radius", areaDto.getRadius().toString());
		polygonsElement.setAttribute("wholeDbPhotosNum", String.valueOf(totalPhotoNum));

		long maxTotalCount = Long.MIN_VALUE;
		long maxSelectCount = Long.MIN_VALUE;
		long minTotalCount = Long.MAX_VALUE;
		long minSelectCount = Long.MAX_VALUE;

		for (FlickrAreaResult areaResult : areaResults) {
			FlickrArea area = areaResult.getArea();
			Element polygonElement = new Element("polygon");
			polygonsElement.addContent(polygonElement);
			polygonElement.setAttribute("id", area.getId() + "");
			polygonElement.setAttribute("total", area.getTotalCount() + "");
			polygonElement.setAttribute("select", areaResult.getSelectCount() + "");
			polygonElement.setAttribute("area", area.getArea() + "");

			Element lineElement = new Element("line");
			polygonElement.addContent(lineElement);
			lineElement.setAttribute("width", "1");

			List<Point2D> geom = area.getGeom();
			for (Point2D point : geom) {
				Element pointElement = new Element("point");
				lineElement.addContent(pointElement);
				pointElement.setAttribute("lng", point.getX() + "");
				pointElement.setAttribute("lat", point.getY() + "");
			}

			Element centerElement = new Element("center");
			polygonElement.addContent(centerElement);
			Element pointElement = new Element("point");
			centerElement.addContent(pointElement);
			pointElement.setAttribute("lng", area.getCenter().getX() + "");
			pointElement.setAttribute("lat", area.getCenter().getY() + "");

			if (area.getTotalCount() > maxTotalCount) {
				maxTotalCount = area.getTotalCount();
			}
			if (area.getTotalCount() < minTotalCount) {
				minTotalCount = area.getTotalCount();
			}
			if (areaResult.getSelectCount() > maxSelectCount) {
				maxSelectCount = area.getTotalCount();
			}
			if (areaResult.getSelectCount() < minSelectCount) {
				minSelectCount = area.getTotalCount();
			}
		}

		polygonsElement.setAttribute("minTotal", minTotalCount + "");
		polygonsElement.setAttribute("maxTotal", maxTotalCount + "");
		polygonsElement.setAttribute("minSelect", minSelectCount + "");
		polygonsElement.setAttribute("maxSelect", maxSelectCount + "");

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
