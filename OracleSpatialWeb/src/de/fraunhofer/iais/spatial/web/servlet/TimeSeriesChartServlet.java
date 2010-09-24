package de.fraunhofer.iais.spatial.web.servlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.springframework.web.context.support.WebApplicationContextUtils;

import de.fraunhofer.iais.spatial.dao.AreaDao;
import de.fraunhofer.iais.spatial.dto.FlickrDeWestAreaDto;
import de.fraunhofer.iais.spatial.entity.Area;
import de.fraunhofer.iais.spatial.entity.FlickrDeWestArea;
import de.fraunhofer.iais.spatial.service.AreaMgr;
import de.fraunhofer.iais.spatial.service.FlickrDeWestAreaMgr;
import de.fraunhofer.iais.spatial.util.StringUtil;

public class TimeSeriesChartServlet extends HttpServlet {
	/**
	* Logger for this class
	*/
	private static final Logger logger = LoggerFactory.getLogger(TimeSeriesChartServlet.class);

	private static final long serialVersionUID = -4033923021316859791L;

	private static FlickrDeWestAreaMgr areaMgr = null;

	@SuppressWarnings("unchecked")
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		logger.debug("requestUrl:" + request.getRequestURL()); //$NON-NLS-1$
		response.setContentType("image/jpeg");
		response.setHeader("Cache-Control", "no-cache");
		ServletOutputStream sos = response.getOutputStream();
		String localBasePath = System.getProperty("oraclespatialweb.root");

		String areaid = request.getParameter("areaid");
		if (areaid == null || "".equals(areaid))
			return;

		String xmlfile = request.getParameter("xml");

		if (xmlfile != null && !"".equals(xmlfile)) {

			BufferedReader br = new BufferedReader(new FileReader(localBasePath + xmlfile));
			StringBuffer xml = new StringBuffer();
			String thisLine;
			while ((thisLine = br.readLine()) != null) {
				xml.append(thisLine);
			}

			FlickrDeWestAreaDto areaDto = new FlickrDeWestAreaDto();
			try {
				areaMgr.parseXmlRequest1(StringUtil.FullMonth2Num(xml.toString()), areaDto);

				FlickrDeWestArea area = areaMgr.getAreaDao().getAreaById(Integer.parseInt(areaid), areaDto.getRadius());
				if (area != null){
					areaMgr.createTimeSeriesChart(area, new LinkedHashSet<String>(areaDto.getYears()), sos);
				}
			} catch (Exception e) {
				logger.error("doGet(HttpServletRequest, HttpServletResponse)", e); //$NON-NLS-1$
			}
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
		areaMgr = WebApplicationContextUtils.getRequiredWebApplicationContext(this.getServletContext()).getBean("flickrDeWestAreaMgr", FlickrDeWestAreaMgr.class);
	}
}
