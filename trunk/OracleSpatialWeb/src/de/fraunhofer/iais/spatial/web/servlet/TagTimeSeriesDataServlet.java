package de.fraunhofer.iais.spatial.web.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.support.WebApplicationContextUtils;

import de.fraunhofer.iais.spatial.dto.FlickrAreaDto;
import de.fraunhofer.iais.spatial.entity.FlickrArea;
import de.fraunhofer.iais.spatial.entity.FlickrArea.Radius;
import de.fraunhofer.iais.spatial.service.FlickrAreaMgr;
import de.fraunhofer.iais.spatial.util.DateUtil;
import de.fraunhofer.iais.spatial.util.FlickrAreaUtil;
import de.fraunhofer.iais.spatial.util.XmlUtil;

public class TagTimeSeriesDataServlet extends HttpServlet {
	/**
	* Logger for this class
	*/
	private static final Logger logger = LoggerFactory.getLogger(TagTimeSeriesDataServlet.class);

	private static final long serialVersionUID = 289355222687198395L;

	private static final int MAX_PAGE_SIZE = 200;
	private static FlickrAreaMgr areaMgr = null;

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

		response.setContentType("text/xml");
		// Prevents caching
		response.setHeader("Cache-Control", "no-store"); // HTTP1.1
		response.setHeader("Pragma", "no-cache"); // HTTP1.0
		response.setDateHeader("Expires", 0); // proxy server

		int areaid = NumberUtils.toInt(request.getParameter("areaid"), -1);
		String tag = request.getParameter("tag");
		PrintWriter out = response.getWriter();

		Document document = new Document();
		Element rootElement = new Element("response");
		document.setRootElement(rootElement);
		Element messageElement = new Element("message");
		rootElement.addContent(messageElement);

		logger.info("doGet(HttpServletRequest, HttpServletResponse) - areaid:" + areaid + "|tag:" + tag); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		if (areaid <= 0 || StringUtils.isEmpty(tag)) {
			messageElement.setText("ERROR: wrong input parameter!");
		} else if (request.getSession().getAttribute("areaDto") == null) {
			messageElement.setText("ERROR: session has timed out, please refresh the page.");
		} else {
			try {
				FlickrAreaDto areaDto = SerializationUtils.clone((FlickrAreaDto) request.getSession().getAttribute("areaDto"));

				int zoom = NumberUtils.toInt(request.getParameter("zoom"), areaDto.getZoom());
				Radius radius = FlickrAreaUtil.judgeRadius(zoom);
				FlickrArea area = areaMgr.getAreaDao().getAreaById(areaid, radius);

				if (area != null) {
					Map<Date, Integer> seriesData = areaMgr.createTagTimeSeriesData(area, tag, areaDto.getYears());
					buildXmlDoc(document, seriesData);

					messageElement.setText("SUCCESS");
				} else {
					messageElement.setText("ERROR: the request polygon doesn't exist in the current zoom level!");
				}
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

	private String buildXmlDoc(Document document, Map<Date, Integer> seriesData) {
		// spilt the data by year
		SimpleDateFormat ysdf = new SimpleDateFormat("yyyy", Locale.ENGLISH);
		Map<Integer, Map<Date, Integer>> countsGroupedMap = new TreeMap<Integer, Map<Date, Integer>>();
		for (Map.Entry<Date, Integer> e : seriesData.entrySet()) {
			int year = Integer.valueOf(ysdf.format(e.getKey().getTime()));
			if (!countsGroupedMap.containsKey(year)) {
				Map<Date, Integer> countsSubMap = new TreeMap<Date, Integer>();
				countsGroupedMap.put(year, countsSubMap);
			}
			countsGroupedMap.get(year).put(e.getKey(), e.getValue());
		}

		// build XmlDoc
		Element rootElement = document.getRootElement();
		Element dataElement = new Element("data");
		rootElement.addContent(dataElement);
		for (int year : countsGroupedMap.keySet()) {
			Element sereisElement = new Element("series");
			sereisElement.setAttribute("year", String.valueOf(year));
			dataElement.addContent(sereisElement);

			StringBuffer dataStr = new StringBuffer();
			dataStr.append("[");
			int i = 1;
			for (Map.Entry<Date, Integer> e : countsGroupedMap.get(year).entrySet()) {
				dataStr.append("[");
				dataStr.append(DateUtils.setYears(e.getKey(), 2000).getTime()); //time
				dataStr.append(",");
				dataStr.append(e.getValue());		//value
				dataStr.append("]");
				if(i++ < countsGroupedMap.get(year).size()){
					dataStr.append(",");
				}
			}
			dataStr.append("]");
			sereisElement.addContent(dataStr.toString());
		}

		return XmlUtil.xml2String(document, true);
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
