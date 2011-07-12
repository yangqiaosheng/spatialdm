package de.fraunhofer.iais.spatial.web.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.support.WebApplicationContextUtils;

import de.fraunhofer.iais.spatial.dto.FlickrAreaDto;
import de.fraunhofer.iais.spatial.entity.FlickrArea;
import de.fraunhofer.iais.spatial.entity.FlickrPhoto;
import de.fraunhofer.iais.spatial.entity.FlickrArea.Radius;
import de.fraunhofer.iais.spatial.service.FlickrAreaMgr;
import de.fraunhofer.iais.spatial.util.FlickrAreaUtil;
import de.fraunhofer.iais.spatial.util.XmlUtil;

public class SmallPhotoUrlServlet extends HttpServlet {
	/**
	* Logger for this class
	*/
	private static final Logger logger = LoggerFactory.getLogger(SmallPhotoUrlServlet.class);

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

		String areaid = request.getParameter("areaid");
		String page = request.getParameter("page");
		String pageSize = request.getParameter("page_size");
		PrintWriter out = response.getWriter();

		Document document = new Document();
		Element rootElement = new Element("response");
		document.setRootElement(rootElement);
		Element messageElement = new Element("message");
		rootElement.addContent(messageElement);

		logger.info("doGet(HttpServletRequest, HttpServletResponse) - areaid:" + areaid + "|page:" + page + "|pageSize:" + pageSize); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		if (!StringUtils.isNumeric(areaid) || !StringUtils.isNumeric(page) || Integer.parseInt(page) < 1 || !StringUtils.isNumeric(pageSize) || Integer.parseInt(pageSize) < 1 || Integer.parseInt(pageSize) > MAX_PAGE_SIZE) {
			messageElement.setText("ERROR: wrong input parameter!");
		} else if (request.getSession().getAttribute("areaDto") == null) {
			messageElement.setText("ERROR: please perform a query first!");
		} else {
			try {
				FlickrAreaDto areaDto = (FlickrAreaDto) request.getSession().getAttribute("areaDto");
				int zoom = NumberUtils.toInt(request.getParameter("zoom"), areaDto.getZoom());
				Radius radius = FlickrAreaUtil.judgeRadius(zoom);

				logger.debug("doGet(HttpServletRequest, HttpServletResponse) - areaid:" + areaid + "|radius:" + radius + "|queryStrs:" + areaDto.getQueryStrs()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

				FlickrArea area = areaMgr.getAreaDao().getAreaById(Integer.parseInt(areaid), Radius.valueOf("R" + radius));

				if(area != null){
					photosResponseXml(document, area, areaDto, Integer.parseInt(page), Integer.parseInt(pageSize));
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

		out.print(XmlUtil.xml2String(document, true));

		out.flush();
		out.close();
		System.gc();
	}

	private String photosResponseXml(Document document, FlickrArea area, FlickrAreaDto areaDto, int page, int pageSize) {

		List<FlickrPhoto> photos = areaMgr.getAreaDao().getPhotos(area, areaDto, page, pageSize);
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		SimpleDateFormat weekdayFormat = new SimpleDateFormat("EEE", Locale.ENGLISH);

		Element rootElement = document.getRootElement();

		Element photosElement = new Element("photos");
		rootElement.addContent(photosElement);
		photosElement.setAttribute("size", String.valueOf(photos.size()));

		for (FlickrPhoto p : photos) {
			Element photoElement = new Element("photo");
			photosElement.addContent(photoElement);
			photoElement.setAttribute("index", String.valueOf(p.getIndex()));

			photoElement.addContent(new Element("photoId").setText(String.valueOf(p.getId())));
			photoElement.addContent(new Element("polygonId").setText(String.valueOf(p.getArea().getId())));
			photoElement.addContent(new Element("polygonRadius").setText(String.valueOf(p.getArea().getRadius())));
			photoElement.addContent(new Element("date").setText(dateFormat.format(p.getDate())));
			photoElement.addContent(new Element("weekday").setText(weekdayFormat.format(p.getDate())));
			photoElement.addContent(new Element("latitude").setText(String.valueOf(p.getLatitude())));
			photoElement.addContent(new Element("longitude").setText(String.valueOf(p.getLongitude())));
			photoElement.addContent(new Element("personId").setText(String.valueOf(p.getPersonId())));
			photoElement.addContent(new Element("title").setText(String.valueOf(p.getTitle())));
			photoElement.addContent(new Element("smallUrl").setText(String.valueOf(p.getSmallUrl())));
			photoElement.addContent(new Element("viewed").setText(String.valueOf(p.getViewed())));
			photoElement.addContent(new Element("rawTags").setText(String.valueOf(p.getRawTags())));
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
