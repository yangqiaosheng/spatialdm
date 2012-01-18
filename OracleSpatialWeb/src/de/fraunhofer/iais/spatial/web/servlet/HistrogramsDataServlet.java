package de.fraunhofer.iais.spatial.web.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.TimeoutException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.jdom.CDATA;
import org.jdom.Document;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.support.WebApplicationContextUtils;

import de.fraunhofer.iais.spatial.dto.FlickrAreaDto;
import de.fraunhofer.iais.spatial.dto.SessionMutex;
import de.fraunhofer.iais.spatial.dto.FlickrAreaDto.Level;
import de.fraunhofer.iais.spatial.entity.FlickrArea;
import de.fraunhofer.iais.spatial.entity.Histograms;
import de.fraunhofer.iais.spatial.exception.IllegalInputParameterException;
import de.fraunhofer.iais.spatial.service.FlickrAreaMgr;
import de.fraunhofer.iais.spatial.util.DateUtil;
import de.fraunhofer.iais.spatial.util.StringUtil;
import de.fraunhofer.iais.spatial.util.XmlUtil;
import de.fraunhofer.iais.spatial.web.FlickrAreaCancelableJobCallback;
import de.fraunhofer.iais.spatial.web.FlickrAreaCancelableJobTemplate;
import de.fraunhofer.iais.spatial.web.XmlServletCallback;
import de.fraunhofer.iais.spatial.web.XmlServletTemplate;

public class HistrogramsDataServlet extends HttpServlet {
	/**
	 *
	 */
	private static final long serialVersionUID = 6872890630342702006L;

	/**
	* Logger for this class
	*/
	private static final Logger logger = LoggerFactory.getLogger(HistrogramsDataServlet.class);

	private static FlickrAreaMgr areaMgr = null;
	final public static String HISTOGRAM_SESSION_ID = "HISTOGRAM_SESSION_ID";
	final public static String HISTOGRAM_SESSION_LOCK = "HISTOGRAM_SESSION_LOCK";

//	public static StringBuffer idStrBuf = null;

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doGet(req, resp);
	}

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
			public void doInXmlServlet(HttpServletRequest request, Logger logger, Element rootElement, Element messageElement) throws Exception {
				new FlickrAreaCancelableJobTemplate().doExecute(request, logger, rootElement, messageElement, new FlickrAreaCancelableJobCallback() {

					@Override
					public void doCancelableJob(HttpServletRequest request, Logger logger, SessionMutex sessionMutex, Date timestamp, Element rootElement, Element messageElement) throws Exception {
						//Google Chart for debug
						String hasChart = request.getParameter("chart");
						String xml = request.getParameter("xml");
						logger.trace("doGet(HttpServletRequest, HttpServletResponse) - xml:" + xml); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

						if (StringUtils.isEmpty(xml)) {
							String errMsg = "ERROR: 'xml' parameter is missing!";
							messageElement.setText(errMsg);
							throw new IllegalInputParameterException(errMsg);
						}
						FlickrAreaDto areaDto = new FlickrAreaDto();
						areaMgr.parseXmlRequest(StringUtil.FullMonth2Num(xml.toString()), areaDto);
						logger.info("doGet(HttpServletRequest, HttpServletResponse) - years:" + areaDto.getYears() + " |months:" + areaDto.getMonths() + "|days:" + areaDto.getDays() + "|hours:" + areaDto.getHours() + "|weekdays:" + areaDto.getWeekdays()); //$NON-NLS-1$


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

						Histograms sumHistrograms = areaMgr.getAreaCancelableJob().calculateSumHistogram(timestamp, sessionMutex, areas, areaDto);
						histrogramsResponseXml(rootElement, sumHistrograms, BooleanUtils.toBoolean(hasChart));

						messageElement.setText("SUCCESS");
					}
				});
			}
		});
	}

	/* replace by XmlServeltTemplate
	@Deprecated
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		response.setContentType("text/xml");
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

			//Google Chart
			String hasChart = request.getParameter("chart");
			logger.trace("doGet(HttpServletRequest, HttpServletResponse) - xml:" + xml); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

			if (StringUtils.isEmpty(xml)) {
				String errMsg = "ERROR: 'xml' parameter is missing!";
				messageElement.setText(errMsg);
				throw new IllegalInputParameterException(errMsg);
			}
			FlickrAreaDto areaDto = new FlickrAreaDto();

			HttpSession session = request.getSession();
			Date timestamp = new Date();
			timestamp.setTime(NumberUtils.toLong(request.getParameter("timestamp")));
			SessionMutex sessionMutex = null;
			synchronized (this) {
				if (session.getAttribute(HISTOGRAM_SESSION_ID) == null) {
					session.setAttribute(HISTOGRAM_SESSION_ID, new SessionMutex(timestamp));
				}
				sessionMutex = (SessionMutex) session.getAttribute(HISTOGRAM_SESSION_ID);
				if (sessionMutex.getTimestamp().before(timestamp)) {
					sessionMutex.setTimestamp(timestamp);
				}
			}
			try {
				areaMgr.parseXmlRequest(StringUtil.FullMonth2Num(xml.toString()), areaDto);
				logger.info("doGet(HttpServletRequest, HttpServletResponse) - years:" + areaDto.getYears() + " |months:" + areaDto.getMonths() + "|days:" + areaDto.getDays() + "|hours:" + areaDto.getHours() + "|weekdays:" + areaDto.getWeekdays()); //$NON-NLS-1$

				if (session.getAttribute(HISTOGRAM_SESSION_LOCK) != null) {
					int waitSec = 15;
					for (int i = 1; i <= waitSec; i++) {
						Thread.sleep(1000);
						if (session.getAttribute(HISTOGRAM_SESSION_LOCK) == null && sessionMutex.getTimestamp().equals(timestamp)) {
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

				session.setAttribute(HISTOGRAM_SESSION_LOCK, new SessionMutex(timestamp));

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

				Histograms sumHistrograms = areaMgr.getAreaCancelableJob().calculateSumHistogram(timestamp, sessionMutex, areas, areaDto);
				histrogramsResponseXml(document, sumHistrograms, BooleanUtils.toBoolean(hasChart));

				session.removeAttribute(HISTOGRAM_SESSION_ID);
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
				session.removeAttribute(HISTOGRAM_SESSION_LOCK);
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
	}
	 */

	public void histrogramsResponseXml(Element rootElement, Histograms histrograms, boolean hasChart) {

		Element histrogramsDataElement = new Element("histrogramsData");
		rootElement.addContent(histrogramsDataElement);

		addHistrogram(histrogramsDataElement, histrograms.getWeekdays(), Level.WEEKDAY);
		addHistrogram(histrogramsDataElement, histrograms.getHours(), Level.HOUR);
		addHistrogram(histrogramsDataElement, histrograms.getDays(), Level.DAY);
		addHistrogram(histrogramsDataElement, histrograms.getMonths(), Level.MONTH);
		addHistrogram(histrogramsDataElement, histrograms.getYears(), Level.YEAR);

		if (hasChart) {
			Element histrogramsChartElement = new Element("histrogramsChart");
			rootElement.addContent(histrogramsChartElement);

			addGoogleHistrogramChart(histrogramsChartElement, "Photos Distribution", 400, 200, histrograms.getWeekdays(), Level.WEEKDAY);
			addGoogleHistrogramChart(histrogramsChartElement, "Photos Distribution", 400, 200, histrograms.getHours(), Level.HOUR);
			addGoogleHistrogramChart(histrogramsChartElement, "Photos Distribution", 400, 200, histrograms.getDays(), Level.DAY);
			addGoogleHistrogramChart(histrogramsChartElement, "Photos Distribution", 400, 200, histrograms.getMonths(), Level.MONTH);
			addGoogleHistrogramChart(histrogramsChartElement, "Photos Distribution", 400, 200, histrograms.getYears(), Level.YEAR);
		}
	}

	private void addHistrogram(Element element, Map<Integer, Integer> histrogramData, Level displayLevel) {

		int maxValue = new TreeSet<Integer>(histrogramData.values()).last();
		int minValue = new TreeSet<Integer>(histrogramData.values()).first();
		String levelStr = displayLevel.toString().toLowerCase();

		Element histrogramElement = new Element(levelStr + "s");
		histrogramElement.setAttribute("maxValue", String.valueOf(maxValue));
		histrogramElement.setAttribute("minValue", String.valueOf(minValue));
		element.addContent(histrogramElement);

		for (Map.Entry<Integer, Integer> e : histrogramData.entrySet()) {
			Element valueElement = new Element(levelStr);
			histrogramElement.addContent(valueElement);
			valueElement.setAttribute("id", DateUtil.getChartLabelStr(e.getKey(), displayLevel));
			valueElement.setText(String.valueOf(e.getValue()));
		}
	}

	private void addGoogleHistrogramChart(Element element, String title, int width, int height, Map<Integer, Integer> histrogramData, Level displayLevel) {
		String levelStr = displayLevel.toString().toLowerCase();
		Element histrogramChartElement = new Element("histrogramChart");
		element.addContent(histrogramChartElement);
		histrogramChartElement.setAttribute("type", levelStr);

		String imgUrl = areaMgr.createGoogleChartImg(title, width, height, histrogramData, displayLevel);

		histrogramChartElement.addContent(new CDATA(imgUrl));
	}

	public void setAreaMgr(FlickrAreaMgr areaMgr) {
		HistrogramsDataServlet.areaMgr = areaMgr;
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
