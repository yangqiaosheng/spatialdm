package de.fraunhofer.iais.spatial.web;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.jdom.Element;
import org.slf4j.Logger;

import de.fraunhofer.iais.spatial.dto.SessionMutex;

/**
 * Hook-Operation
 * @author <a href="mailto:haolin.zhi@iais.fraunhofer.de">Haolin Zhi</A>
 * @author <a href="mailto:iulian.peca@iais.fraunhofer.de">Iulian Peca</a>
 *
 */
public interface CancelableJobServletCallback {

	void doCancelableJob(HttpServletRequest request, Logger logger, SessionMutex sessionMutex, Date timestamp, Element rootElement, Element messageElement) throws Exception;
}
