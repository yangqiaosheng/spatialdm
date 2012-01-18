package de.fraunhofer.iais.spatial.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.jdom.Element;
import org.slf4j.Logger;

public interface XmlServletCallback {

	void doInXmlServlet(HttpServletRequest request, HttpServletResponse response, Logger logger, Element rootElement, Element messageElement, XmlServletCallback callback) throws Exception;
}
