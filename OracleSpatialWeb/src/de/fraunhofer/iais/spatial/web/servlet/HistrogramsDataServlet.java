package de.fraunhofer.iais.spatial.web.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

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
import de.fraunhofer.iais.spatial.dto.FlickrEuropeAreaDto.Level;
import de.fraunhofer.iais.spatial.entity.FlickrArea;
import de.fraunhofer.iais.spatial.entity.Histrograms;
import de.fraunhofer.iais.spatial.service.FlickrEuropeAreaMgr;
import de.fraunhofer.iais.spatial.util.StringUtil;
import de.fraunhofer.iais.spatial.util.XmlUtil;

public class HistrogramsDataServlet extends HttpServlet {
	/**
	 *
	 */
	private static final long serialVersionUID = 6872890630342702006L;


	/**
	* Logger for this class
	*/
	private static final Logger logger = LoggerFactory.getLogger(HistrogramsDataServlet.class);


	private static FlickrEuropeAreaMgr areaMgr = null;

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

		String xml = request.getParameter("xml");

		PrintWriter out = response.getWriter();

		Document document = new Document();
		Element rootElement = new Element("response");
		document.setRootElement(rootElement);
		Element messageElement = new Element("message");
		rootElement.addContent(messageElement);

		logger.info("doGet(HttpServletRequest, HttpServletResponse) - xml:" + xml); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		if (StringUtils.isEmpty(xml)) {
			messageElement.setText("ERROR: wrong input parameter!");
		} else {
			FlickrEuropeAreaDto areaDto = new FlickrEuropeAreaDto();
			try {
				areaMgr.parseXmlRequest(StringUtil.FullMonth2Num(xml.toString()), areaDto);
				logger.info("doGet(HttpServletRequest, HttpServletResponse) - years:" + areaDto.getYears() + " |months:" + areaDto.getMonths() + "|days:" + areaDto.getDays() + "|hours:" + areaDto.getHours() + "|weekdays:" + areaDto.getWeekdays()); //$NON-NLS-1$

				List<FlickrArea> areas = null;
				if (areaDto.getPolygon() != null) {
					areas = areaMgr.getAreaDao().getAreasByPolygon(areaDto.getPolygon(), areaDto.getRadius());
				} else if (areaDto.getBoundaryRect() != null) {
					int size = areaMgr.getAreaDao().getAreasByRectSize(areaDto.getBoundaryRect().getMinX(), areaDto.getBoundaryRect().getMinY(), areaDto.getBoundaryRect().getMaxX(), areaDto.getBoundaryRect().getMaxY(), areaDto.getRadius());
					if(size > 2000){
						throw new IllegalArgumentException("The maximun number of return polygons is exceeded! \n" +
								" Please choose a smaller Bounding Box <bounds> or a lower zoom value <zoom>");
					}
					areas = areaMgr.getAreaDao().getAreasByRect(areaDto.getBoundaryRect().getMinX(), areaDto.getBoundaryRect().getMinY(), areaDto.getBoundaryRect().getMaxX(), areaDto.getBoundaryRect().getMaxY(), areaDto.getRadius());
				} else {
					areas = areaMgr.getAreaDao().getAllAreas(areaDto.getRadius());
				}

				Histrograms sumHistrograms = areaMgr.calculateHistrograms(areas, areaDto);
				histrogramsResponseXml(document, sumHistrograms);

				messageElement.setText("SUCCESS");
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
	}

	private String histrogramsResponseXml(Document document, Histrograms histrograms) {

		Element rootElement = document.getRootElement();
		Element histrogramsElement = new Element("histrograms");
		rootElement.addContent(histrogramsElement);

		addHistrogram(document, histrograms.getWeekdays(), Level.WEEKDAY);
		addHistrogram(document, histrograms.getHours(), Level.HOUR);
		addHistrogram(document, histrograms.getDays(), Level.DAY);
		addHistrogram(document, histrograms.getMonths(), Level.MONTH);
		addHistrogram(document, histrograms.getYears(), Level.YEAR);

		return XmlUtil.xml2String(document, true);
	}

	private void addHistrogram(Document document, Map<Integer, Integer> histrogramData, Level displayLevel) {

		int maxValue = new TreeSet<Integer>(histrogramData.values()).last();
		String levelStr = displayLevel.toString().toLowerCase();

		Element rootElement = document.getRootElement();
		Element histrogramElement = new Element(levelStr + "s");
		histrogramElement.setAttribute("maxValue", String.valueOf(maxValue));
		rootElement.addContent(histrogramElement);

		for(Map.Entry<Integer, Integer> e : histrogramData.entrySet()){
			Element valueElement = new Element(levelStr);
			valueElement.setAttribute("id", String.valueOf(e.getKey()));
			valueElement.setText(String.valueOf(e.getValue()));
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
		areaMgr = WebApplicationContextUtils.getRequiredWebApplicationContext(this.getServletContext()).getBean("flickrEuropeAreaMgr", FlickrEuropeAreaMgr.class);
	}
}
