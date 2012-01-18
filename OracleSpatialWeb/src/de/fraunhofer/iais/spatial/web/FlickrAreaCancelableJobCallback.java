package de.fraunhofer.iais.spatial.web;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.jdom.Element;
import org.slf4j.Logger;

import de.fraunhofer.iais.spatial.dto.SessionMutex;


public interface FlickrAreaCancelableJobCallback {

	void doCancelableJob(HttpServletRequest request, Logger logger, SessionMutex sessionMutex, Date timestamp, Element rootElement, Element messageElement) throws Exception;
}
