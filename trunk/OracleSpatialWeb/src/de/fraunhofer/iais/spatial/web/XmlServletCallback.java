package de.fraunhofer.iais.spatial.web;

import javax.servlet.http.HttpServletRequest;

import org.jdom.Element;
import org.slf4j.Logger;

/**
 * Hook-Operation für XmlServletCallback
 * @author <a href="mailto:haolin.zhi@iais.fraunhofer.de">Haolin Zhi</A>
 * @author <a href="mailto:iulian.peca@iais.fraunhofer.de">Iulian Peca</a>
 *
 */
public interface XmlServletCallback {

	void doInXmlServlet(HttpServletRequest request, Logger logger, Element rootElement, Element messageElement) throws Exception;
}
