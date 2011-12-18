package de.fraunhofer.iais.spatial.web.servlet;

import java.awt.geom.Point2D;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.text.translate.UnicodeEscaper;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fraunhofer.iais.spatial.dto.ModelDto;
import de.fraunhofer.iais.spatial.util.StringUtil;
import de.fraunhofer.iais.spatial.util.XmlUtil;

public class RequestKmlServlet extends HttpServlet {
	/**
	* Logger for this class
	*/
	private static final Logger logger = LoggerFactory.getLogger(RequestKmlServlet.class);

	private static final long serialVersionUID = -6814809670117597713L;

	public static final String kmlPath = "kml/";

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		// web base path for local operation
//		String localBasePath = getServletContext().getRealPath("/");
		// web base path for remote access
		String remoteBasePath = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath() + "/";
		//		String remoteBasePath = "http://kd-photomap.iais.fraunhofer.de/OracleSpatialWeb/";
		response.setContentType("text/xml; charset=UTF-8");
		// response.setContentType("application/vnd.google-earth.kml+xml");

		// Prevents caching
		response.setHeader("Cache-Control", "no-store"); // HTTP1.1
//		response.setHeader("Content-Type", "text/html; charset=UTF-8");
		response.setHeader("Pragma", "no-cache"); // HTTP1.0
		response.setDateHeader("Expires", 0); // proxy server

		String xml = FileUtils.readFileToString(new File(this.getClass().getResource("/../../request.xml").getPath()), "UTF-8");
		String persist = request.getParameter("persist");

		PrintWriter out = response.getWriter();

		Document document = new Document();
		Element rootElement = new Element("response");
		document.setRootElement(rootElement);
		Element messageElement = new Element("message");
		rootElement.addContent(messageElement);
		if (StringUtils.isEmpty(xml)) {
			messageElement.setText("ERROR: 'xml' parameter is missing!");
		} else if ("true".equals(persist) && request.getSession().getAttribute("areaDto") == null) {
			messageElement.setText("ERROR: please perform a query first!");
		} else {

			try {
				String filenamePrefix = StringUtil.genId();

				logger.debug("doGet(HttpServletRequest, HttpServletResponse) - xml:" + xml); //$NON-NLS-1$

				List<ModelDto> modelDtos = parseXmlRequest(StringUtil.FullMonth2Num(xml.toString()));
				System.out.println(ToStringBuilder.reflectionToString(modelDtos));
				if (CollectionUtils.isEmpty(modelDtos)) {
					String modelsXml = FileUtils.readFileToString(new File(this.getClass().getResource("/../../models.xml").getPath()), "UTF-8");
					document = XmlUtil.string2Xml(modelsXml, "UTF-8");
				} else {

				}

				Element urlElement = new Element("url");
				rootElement.addContent(urlElement);
				urlElement.setText(remoteBasePath + kmlPath + filenamePrefix + ".kml");
				messageElement.setText("SUCCESS");
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

	public List<ModelDto> parseXmlRequest(String xml) throws JDOMException, IOException, ParseException {
		Document document = XmlUtil.string2Xml(xml, "UTF-8");
		Element rootElement = document.getRootElement();

		List<ModelDto> modelDtos = new ArrayList<ModelDto>();

//		<Models>
		Element modelsElement = rootElement.getChild("Models");
		if (modelsElement != null) {
			// <HDAModel>
			List<Element> modelElements = modelsElement.getChildren("HDAModel");
			for (Element modelElement : modelElements) {
				ModelDto modelDto = new ModelDto();
				modelDtos.add(modelDto);
				modelDto.setModelType(modelElement.getAttributeValue("type"));
				modelDto.setModelNamel(modelElement.getAttributeValue("name"));
				modelDto.setTitle(modelElement.getChildText("title"));
				modelDto.setCall(modelElement.getChildText("call"));
				modelDto.setValid(modelElement.getChildText("valid"));

				Pattern positionPattern = Pattern.compile("\\(([-0-9.]*), ([-0-9.]*)\\)");
				Matcher positionMachter = positionPattern.matcher(modelElement.getChildText("position"));

				if (positionMachter.find()) {
					Point2D position = new Point2D.Double();
					modelDto.setPosition(position);
					position.setLocation(Double.parseDouble(positionMachter.group(2)), Double.parseDouble(positionMachter.group(1)));
				}
			}
		}
		return modelDtos;
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
	}

}
