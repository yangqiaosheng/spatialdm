package de.fraunhofer.iais.spatial.web.servlet;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.support.WebApplicationContextUtils;

import de.fraunhofer.iais.spatial.dto.FlickrAreaDto;
import de.fraunhofer.iais.spatial.entity.FlickrArea;
import de.fraunhofer.iais.spatial.entity.FlickrArea.Radius;
import de.fraunhofer.iais.spatial.exception.IllegalInputParameterException;
import de.fraunhofer.iais.spatial.service.FlickrAreaMgr;
import de.fraunhofer.iais.spatial.util.FlickrAreaUtil;
import de.fraunhofer.iais.spatial.web.XmlServletCallback;
import de.fraunhofer.iais.spatial.web.XmlServletTemplate;

/**
 * Provides the interactable Tag-TimeSeriesChart data in json format
 * @author <a href="mailto:haolin.zhi@iais.fraunhofer.de">Haolin Zhi</A>
 * @author <a href="mailto:iulian.peca@iais.fraunhofer.de">Iulian Peca</a>
 *
 */
public class TagTimeSeriesDataServlet extends HttpServlet {
	/**
	* Logger for this class
	*/
	private static final Logger logger = LoggerFactory.getLogger(TagTimeSeriesDataServlet.class);

	private static final long serialVersionUID = 289355222687198395L;

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

		new XmlServletTemplate().doExecute(request, response, logger, new XmlServletCallback() {

			@Override
			public void doInXmlServlet(HttpServletRequest request, Logger logger, Element rootElement, Element messageElement) throws Exception {
				int areaid = NumberUtils.toInt(request.getParameter("areaid"), -1);
				String tag = request.getParameter("tag");

				tag = new String(tag.getBytes("ISO-8859-1"), "utf-8");
				logger.info("doGet(HttpServletRequest, HttpServletResponse) - areaid:" + areaid + "|tag:" + tag); //$NON-NLS-1$ //$NON-NLS-2$
				if (areaid <= 0 || StringUtils.isEmpty(tag)) {
					String errMsg = "ERROR: wrong input parameter!";
					messageElement.setText(errMsg);
					throw new IllegalInputParameterException(errMsg);
				} else if (request.getSession().getAttribute("areaDto") == null) {
					String errMsg = "ERROR: session has timed out, please refresh the page.";
					messageElement.setText(errMsg);
					throw new IllegalInputParameterException(errMsg);
				}

				FlickrAreaDto areaDto = SerializationUtils.clone((FlickrAreaDto) request.getSession().getAttribute("areaDto"));
				areaDto.setWithoutStopWords(BooleanUtils.toBoolean(StringUtils.defaultString(request.getParameter("stopwords"))));

				int zoom = NumberUtils.toInt(request.getParameter("zoom"), areaDto.getZoom());
				Radius radius = FlickrAreaUtil.judgeRadius(zoom);
				FlickrArea area = areaMgr.getAreaDao().getAreaById(areaid, radius);

				if (area != null) {
					Map<Date, Integer> seriesData = areaMgr.createTagTimeSeriesData(area, tag, areaDto);
					buildXmlDoc(rootElement, seriesData);
					messageElement.setText("SUCCESS");
				} else {
					messageElement.setText("ERROR: the request polygon doesn't exist in the current zoom level!");
				}

			}
		});
	}

	private void buildXmlDoc(Element rootElement, Map<Date, Integer> seriesData) {
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
		Element dataElement = new Element("data");
		rootElement.addContent(dataElement);
		for (int year : countsGroupedMap.keySet()) {
			Element sereisElement = new Element("series");
			sereisElement.setAttribute("year", String.valueOf(year));
			dataElement.addContent(sereisElement);
			
			Element sereisDumpElement = new Element("seriesDump");
			sereisDumpElement.setAttribute("year", String.valueOf(year));
			sereisDumpElement.addContent(sereisDumpElement);

			StringBuffer dataStr = new StringBuffer();
			StringBuffer dataDumpStr = new StringBuffer();
			dataStr.append("[");
			dataDumpStr.append("[");
			SimpleDateFormat dsf = new SimpleDateFormat("yyyy-MM-dd'T'HH");
			int i = 1;
			for (Map.Entry<Date, Integer> e : countsGroupedMap.get(year).entrySet()) {
				dataStr.append("[");
				/*
				//need to add 1 day to the result, because of the bug from HighCharts
				dataStr.append(DateUtils.addDays(DateUtils.setYears(e.getKey(), 2000), 1).getTime()); //time
				*/
				dataStr.append(DateUtils.setYears(e.getKey(), 2000).getTime()); //time
				
				dataStr.append(",");
				dataStr.append(e.getValue()); //value
				dataStr.append("]");
				
				if(e.getValue() > 0){
					
				
				dataDumpStr.append("[");
				/*
				    //need to add 1 day to the result, because of the bug from HighCharts
					dataDumpStr.append(dsf.format(DateUtils.addDays(DateUtils.setYears(e.getKey(), 2000), 1).getTime())); //time
				*/
					dataDumpStr.append(dsf.format(DateUtils.setYears(e.getKey(), 2000).getTime())); //time

					dataDumpStr.append(",");
					dataDumpStr.append(e.getValue()); //value
					dataDumpStr.append("]");
				}
				
				if (i++ < countsGroupedMap.get(year).size()) {
					dataStr.append(",");
					dataDumpStr.append(",");
				}
			}
			dataStr.append("]");
			sereisElement.addContent(dataStr.toString());
		}
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
