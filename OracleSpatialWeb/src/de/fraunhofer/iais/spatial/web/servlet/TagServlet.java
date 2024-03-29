package de.fraunhofer.iais.spatial.web.servlet;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.google.common.collect.Lists;

import de.fraunhofer.iais.spatial.dto.FlickrAreaDto;
import de.fraunhofer.iais.spatial.dto.SessionMutex;
import de.fraunhofer.iais.spatial.entity.FlickrArea;
import de.fraunhofer.iais.spatial.entity.FlickrAreaResult;
import de.fraunhofer.iais.spatial.entity.FlickrArea.Radius;
import de.fraunhofer.iais.spatial.exception.IllegalInputParameterException;
import de.fraunhofer.iais.spatial.service.FlickrAreaMgr;
import de.fraunhofer.iais.spatial.util.FlickrAreaUtil;
import de.fraunhofer.iais.spatial.util.StopWordUtil;
import de.fraunhofer.iais.spatial.web.CancelableJobServletCallback;
import de.fraunhofer.iais.spatial.web.CancelableJobServletTemplate;
import de.fraunhofer.iais.spatial.web.XmlServletCallback;
import de.fraunhofer.iais.spatial.web.XmlServletTemplate;

/**
 * Provides the Tags data in xml format
 *
 * @author <a href="mailto:haolin.zhi@iais.fraunhofer.de">Haolin Zhi</A>
 * @author <a href="mailto:iulian.peca@iais.fraunhofer.de">Iulian Peca</a>
 *
 */
public class TagServlet extends HttpServlet {
	/**
	* Logger for this class
	*/
	private static final Logger logger = LoggerFactory.getLogger(TagServlet.class);

	private static final long serialVersionUID = 289355222687198395L;

	private static final int MAX_SIZE = 150;
	private static final int DEFAULT_SIZE = 20;
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
			public void doInXmlServlet(HttpServletRequest request, Logger logger, Element rootElement, Element messageElement) throws Exception {
				new CancelableJobServletTemplate().doExecute(request, logger, rootElement, messageElement, new CancelableJobServletCallback() {

					@Override
					public void doCancelableJob(HttpServletRequest request, Logger logger, SessionMutex sessionMutex, Date timestamp, Element rootElement, Element messageElement) throws Exception {
						String areaid = request.getParameter("areaid");
						int size = NumberUtils.toInt(request.getParameter("size"), DEFAULT_SIZE);
						logger.info("doGet(HttpServletRequest, HttpServletResponse) - areaid:" + areaid + "|size:" + size); //$NON-NLS-1$ //$NON-NLS-2$
						if (!StringUtils.isNumeric(areaid)) {
							String errMsg = "ERROR: wrong input parameter!";
							messageElement.setText(errMsg);
							throw new IllegalInputParameterException(errMsg);
						} else if (size > MAX_SIZE) {
							String errMsg = "ERROR: the size excceed " + MAX_SIZE + " !";
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

						FlickrArea area = areaMgr.getAreaDao().getAreaById(Integer.parseInt(areaid), Radius.valueOf("R" + radius));

						if (area != null) {
							tagsResponseXml(timestamp, sessionMutex, rootElement, area, areaDto, size);
							messageElement.setText("SUCCESS");
						} else {
							messageElement.setText("ERROR: the request polygon doesn't exist in the current zoom level!");
						}
					}
				});
			}
		});
	}

	private void tagsResponseXml(Date timestamp, SessionMutex sessionMutex, Element rootElement, FlickrArea area, FlickrAreaDto areaDto, int size) throws InterruptedException {

		FlickrAreaResult areaResult = areaMgr.getAreaCancelableJob().countTag(timestamp, sessionMutex, area, areaDto);

		List<Map.Entry<String, Integer>> entries = Lists.newLinkedList(areaResult.getTagsCount().entrySet());

		//sort list based on comparator
		Collections.sort(entries, new Comparator<Map.Entry<String, Integer>>() {
			@Override
			public int compare(Map.Entry<String, Integer> e1, Map.Entry<String, Integer> e2) {
				return e2.getValue() - e1.getValue();
			}
		});

		Element tagsElement = new Element("tags");
		rootElement.addContent(tagsElement);

		int num = 0;
		int i = 0;
		for (Map.Entry<String, Integer> entry : entries) {
			//random
			if (entries.size() > size * 5 && num > 9 && Math.random() * (Math.sqrt(num)) < 1) {
				continue;
			}
			if (areaDto.isWithoutStopWords() && entries.size() > size * 2 && i++ < 2) {
				continue;
			}
			if (areaDto.isWithoutStopWords() && StopWordUtil.stopwordsTemp.contains(entry.getKey())) {
				continue;
			}
			if (num++ > size) {
				break;
			}

			Element tagElement = new Element("tag");
			tagsElement.addContent(tagElement);
			tagElement.setAttribute("name", entry.getKey());
			tagElement.setAttribute("num", String.valueOf(entry.getValue()));
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doGet(req, resp);
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
