package de.fraunhofer.iais.spatial.servlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.HashSet;
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

import de.fraunhofer.iais.spatial.entity.Area;
import de.fraunhofer.iais.spatial.service.AreaMgr;

public class TimeSeriesChart extends HttpServlet {
	/**
	* Logger for this class
	*/
	private static final Logger logger = LoggerFactory.getLogger(TimeSeriesChart.class);

	private static final long serialVersionUID = -4033923021316859791L;

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

		Set<String> years = new HashSet<String>();

		String xmlfile = request.getParameter("xml");
		
		if (xmlfile != null && !"".equals(xmlfile)) {

			BufferedReader br = new BufferedReader(new FileReader(localBasePath + xmlfile));
			StringBuffer xml = new StringBuffer();
			String thisLine;
			while ((thisLine = br.readLine()) != null) {
				xml.append(thisLine);
			}

			SAXBuilder builder = new SAXBuilder();
			Document document;
			try {
				document = builder.build(new ByteArrayInputStream(xml.toString().getBytes()));
				Element rootElement = document.getRootElement();
				// <years>
				List<Element> yearsElements = rootElement.getChildren("years");
				if (yearsElements != null && yearsElements.size() == 1) {
					List<Element> yearElements = yearsElements.get(0).getChildren("year");
					for (Element yearElement : yearElements) {
						String year = yearElement.getText();
						if (year != null && !year.trim().equals("")) {
							years.add(year.trim());
						}
					}
				}
			} catch (JDOMException e1) {
				logger.error("doGet(HttpServletRequest, HttpServletResponse)", e1); //$NON-NLS-1$
			}
		}

		if (years.size() == 0) {
			years.add("2009"); //for xml2
		}

		AreaMgr areaMgr = WebApplicationContextUtils.getRequiredWebApplicationContext(this.getServletContext()).getBean("areaMgr", AreaMgr.class);

		Area a = areaMgr.getAreaById(Integer.parseInt(areaid));
		if (a == null)
			return;

		try {
			areaMgr.createTimeSeriesChart(a, years, sos);
		} catch (ParseException e) {
			logger.error("doGet(HttpServletRequest, HttpServletResponse)", e); //$NON-NLS-1$
		}
	}

}
