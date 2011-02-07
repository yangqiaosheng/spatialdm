package de.fraunhofer.iais.spatial.web.servlet;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Calendar;
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

public class PolygonXmlServlet extends HttpServlet {
	/**
	* Logger for this class
	*/
	private static final Logger logger = LoggerFactory.getLogger(PolygonXmlServlet.class);
	private static final long serialVersionUID = -6814809670117597713L;

	private static FlickrEuropeAreaMgr areaMgr = null;

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

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

		if (StringUtils.isEmpty(xml)) {
			messageElement.setText("ERROR: wrong input parameter!");
			responseStr = XmlUtil.xml2String(document, true);
		} else {

			FlickrEuropeAreaDto areaDto = new FlickrEuropeAreaDto();
			if("true".equals(persist) && request.getSession().getAttribute("areaDto") != null){
				logger.info("doGet(HttpServletRequest, HttpServletResponse) - persist:true" );
				areaDto = (FlickrEuropeAreaDto) request.getSession().getAttribute("areaDto");
			}

			try {
				logger.debug("doGet(HttpServletRequest, HttpServletResponse) - xml:" + xml); //$NON-NLS-1$

				areaMgr.parseXmlRequest(StringUtil.FullMonth2Num(xml.toString()), areaDto);

				logger.info("doGet(HttpServletRequest, HttpServletResponse) - years:" + areaDto.getYears() + " |months:" + areaDto.getMonths() + "|days:" + areaDto.getDays() + "|hours:" + areaDto.getHours() + "|weekdays:" + areaDto.getWeekdays()); //$NON-NLS-1$

				List<FlickrArea> areas = null;

				if (areaDto.getPolygon() != null) {
					areas = areaMgr.getAreaDao().getAreasByPolygon(areaDto.getPolygon(), areaDto.getRadius());
				} else if (areaDto.getBoundaryRect() != null) {
					areas = areaMgr.getAreaDao().getAreasByRect(areaDto.getBoundaryRect().getMinX(), areaDto.getBoundaryRect().getMinY(), areaDto.getBoundaryRect().getMaxX(), areaDto.getBoundaryRect().getMaxY(), areaDto.getRadius());
				} else {
					areas = areaMgr.getAreaDao().getAllAreas(areaDto.getRadius());
				}
				areaMgr.countSelected(areas, areaDto);
				responseStr = areaMgr.createXml(areas, null, areaDto.getRadius());
				request.getSession().setAttribute("areaDto", areaDto);
			} catch (Exception e) {
				logger.error("doGet(HttpServletRequest, HttpServletResponse)", e); //$NON-NLS-1$
				messageElement.setText("ERROR: wrong input parameter!");
//				rootElement.addContent(new Element("exceptions").setText(StringUtil.printStackTrace2String(e)));
				rootElement.addContent(new Element("exceptions").setText(e.getMessage()));
				responseStr = XmlUtil.xml2String(document, true);
			}
		}


		out.print(responseStr);
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
	}

}
