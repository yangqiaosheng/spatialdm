package de.fraunhofer.iais.spatial.web;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.jdom.Element;
import org.slf4j.Logger;

import de.fraunhofer.iais.spatial.dto.SessionMutex;

public interface CancelableJobServletCallback {

	void doCancelableJob(HttpServletRequest request, Logger logger, SessionMutex sessionMutex, Date timestamp, Element rootElement, Element messageElement) throws Exception;
}
