package de.fraunhofer.iais.spatial.web.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
import de.fraunhofer.iais.spatial.entity.FlickrArea;
import de.fraunhofer.iais.spatial.entity.FlickrAreaResult;
import de.fraunhofer.iais.spatial.entity.FlickrArea.Radius;
import de.fraunhofer.iais.spatial.service.FlickrAreaMgr;
import de.fraunhofer.iais.spatial.util.FlickrAreaUtil;
import de.fraunhofer.iais.spatial.util.XmlUtil;

public class TagServlet extends HttpServlet {
	/**
	* Logger for this class
	*/
	private static final Logger logger = LoggerFactory.getLogger(TagServlet.class);

	private static final long serialVersionUID = 289355222687198395L;

	private static final int MAX_SIZE = 50;
	private static final int DEFAULT_SIZE = 20;
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

		String areaid = request.getParameter("areaid");
		int size = NumberUtils.toInt(request.getParameter("size"), DEFAULT_SIZE);
		PrintWriter out = response.getWriter();

		Document document = new Document();
		Element rootElement = new Element("response");
		document.setRootElement(rootElement);
		Element messageElement = new Element("message");
		rootElement.addContent(messageElement);

		logger.info("doGet(HttpServletRequest, HttpServletResponse) - areaid:" + areaid + "|size:" + size); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		if (!StringUtils.isNumeric(areaid)) {
			messageElement.setText("ERROR: wrong input parameter!");
		} else if (request.getSession().getAttribute("areaDto") == null) {
			messageElement.setText("ERROR: please perform a query first!");
		} else {
			try {
				FlickrAreaDto areaDto = SerializationUtils.clone((FlickrAreaDto) request.getSession().getAttribute("areaDto"));
				int zoom = NumberUtils.toInt(request.getParameter("zoom"), areaDto.getZoom());
				Radius radius = FlickrAreaUtil.judgeRadius(zoom);

				logger.debug("doGet(HttpServletRequest, HttpServletResponse) - areaid:" + areaid + "|radius:" + radius ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

				FlickrArea area = areaMgr.getAreaDao().getAreaById(Integer.parseInt(areaid), Radius.valueOf("R" + radius));

				if(area != null){
					tagsResponseXml(document, area, areaDto, size);
					messageElement.setText("SUCCESS");
				}else{
					messageElement.setText("ERROR: the request polygon doesn't exist in the current zoom level!");
				}
			} catch (Exception e) {
				logger.error("doGet(HttpServletRequest, HttpServletResponse)", e); //$NON-NLS-1$
				messageElement.setText("ERROR: wrong input parameter!");
//				rootElement.addContent(new Element("exceptions").setText(StringUtil.printStackTrace2String(e)));
				rootElement.addContent(new Element("description").setText(e.getMessage()));
			}
		}

		out.print(XmlUtil.xml2String(document, false));

		out.flush();
		out.close();
		System.gc();
	}

	private String tagsResponseXml(Document document, FlickrArea area, FlickrAreaDto areaDto, int size) {

		FlickrAreaResult areaResult = areaMgr.countTag(area, areaDto);

		List<Map.Entry<String, Integer>> entries = Lists.newLinkedList(areaResult.getTagsCount().entrySet());

		//sort list based on comparator
		Collections.sort(entries, new Comparator<Map.Entry<String, Integer>>() {
			@Override
			public int compare(Map.Entry<String, Integer> e1, Map.Entry<String, Integer> e2) {
				return e2.getValue() - e1.getValue();
			}
		});

		Element tagsElement = new Element("tags");
		document.getRootElement().addContent(tagsElement);

		int num = 0;
		for(Map.Entry<String, Integer> entry : entries){
			if(num++ > size){
				break;
			}
			Element tagElement = new Element("tag");
			tagsElement.addContent(tagElement);
			tagElement.setAttribute("name", entry.getKey());
			tagElement.setAttribute("num", String.valueOf(entry.getValue()));
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
