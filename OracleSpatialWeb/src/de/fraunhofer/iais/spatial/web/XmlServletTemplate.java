package de.fraunhofer.iais.spatial.web;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.jdom.Document;
import org.jdom.Element;
import org.slf4j.Logger;

import de.fraunhofer.iais.spatial.exception.IllegalInputParameterException;
import de.fraunhofer.iais.spatial.util.XmlUtil;

public class XmlServletTemplate {

	/**
	 * Execute the action specified by the given action object within a Servelt.
	 * @param request
	 * @param response
	 * @param logger
	 * @param callback
	 * @throws IOException
	 */
	public void doExecute(HttpServletRequest request, HttpServletResponse response, Logger logger, XmlServletCallback callback) throws IOException {

		response.setContentType("text/xml; charset=UTF-8");
		// response.setContentType("application/vnd.google-earth.kml+xml");

		// Prevents caching
		response.setHeader("Cache-Control", "no-store"); // HTTP1.1
		response.setHeader("Pragma", "no-cache"); // HTTP1.0
		response.setDateHeader("Expires", 0); // proxy server
		PrintWriter out = response.getWriter();

		Document document = new Document();
		Element rootElement = new Element("response");
		document.setRootElement(rootElement);
		Element messageElement = new Element("message");
		rootElement.addContent(messageElement);
		try {
			callback.doInXmlServlet(request, logger, rootElement, messageElement);

		} catch (IllegalInputParameterException e) {
			rootElement.addContent(new Element("description").setText(e.getMessage()));
		} catch (Exception e) {
			logger.error("doGet(HttpServletRequest, HttpServletResponse)", e); //$NON-NLS-1$
			messageElement.setText("ERROR: wrong input parameter!");
//			rootElement.addContent(new Element("exceptions").setText(StringUtil.printStackTrace2String(e)));
			rootElement.addContent(new Element("description").setText(e.getMessage()));
		}

		out.print(XmlUtil.xml2String(document, true));
		out.flush();
		out.close();
		System.gc();
	}
}
