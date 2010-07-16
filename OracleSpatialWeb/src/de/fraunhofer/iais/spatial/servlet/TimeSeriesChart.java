package de.fraunhofer.iais.spatial.servlet;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
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
import de.fraunhofer.iais.spatial.util.StringUtil;

public class TimeSeriesChart extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4033923021316859791L;

	@Override
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		response.setContentType("image/jpeg");
		response.setHeader("Cache-Control", "no-cache");
		ServletOutputStream sos = response.getOutputStream();
		
		String localBasePath = System.getProperty("oraclespatialweb.root");
		
		String areaid = request.getParameter("id");
		if(areaid == null || "".equals(areaid)) return;
		
		String xmlfile = request.getParameter("xml");
		if(xmlfile == null || "".equals(xmlfile)) return;
		
		BufferedReader br = new BufferedReader(new FileReader(localBasePath + xmlfile));
		StringBuffer xml = new StringBuffer();
		String thisLine;
		while ((thisLine = br.readLine()) != null) {
			xml.append(thisLine);
		}
		
		Set<String> years = new HashSet<String>();
		
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
			e1.printStackTrace();
		}
		
		if(years.size()==0)
			years.add("2009"); //for xml2
		
		AreaMgr areaMgr = WebApplicationContextUtils.getRequiredWebApplicationContext(this.getServletContext()).getBean("areaMgr", AreaMgr.class);

		Area a = areaMgr.getAreaById(areaid);
		if(a == null) return;
		
		try {
			areaMgr.createTimeSeriesChart(a, years, sos);
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

}
