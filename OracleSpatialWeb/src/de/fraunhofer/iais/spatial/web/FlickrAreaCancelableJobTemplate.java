package de.fraunhofer.iais.spatial.web;

import java.util.Date;
import java.util.concurrent.TimeoutException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.math.NumberUtils;
import org.jdom.Element;
import org.slf4j.Logger;

import de.fraunhofer.iais.spatial.dto.SessionMutex;
import de.fraunhofer.iais.spatial.util.StringUtil;
import de.fraunhofer.iais.spatial.web.servlet.HistogramsDataServlet;

public class FlickrAreaCancelableJobTemplate {

	public void doExecute(HttpServletRequest request, Logger logger, Element rootElement, Element messageElement, FlickrAreaCancelableJobCallback callback) throws Exception {
		HttpSession session = request.getSession();
		Date timestamp = new Date();
		timestamp.setTime(NumberUtils.toLong(request.getParameter("timestamp")));
		SessionMutex sessionMutex = null;
		synchronized (this) {
			if (session.getAttribute(HistogramsDataServlet.HISTOGRAM_SESSION_ID) == null) {
				session.setAttribute(HistogramsDataServlet.HISTOGRAM_SESSION_ID, new SessionMutex(timestamp));
			}
			sessionMutex = (SessionMutex) session.getAttribute(HistogramsDataServlet.HISTOGRAM_SESSION_ID);
			if (sessionMutex.getTimestamp().before(timestamp)) {
				sessionMutex.setTimestamp(timestamp);
			}
		}

		try {
			if (session.getAttribute(HistogramsDataServlet.HISTOGRAM_SESSION_LOCK) != null) {
				int waitSec = 5;
				for (int i = 1; i <= waitSec; i++) {
					Thread.sleep(1000);
					if (session.getAttribute(HistogramsDataServlet.HISTOGRAM_SESSION_LOCK) == null && sessionMutex.getTimestamp().equals(timestamp)) {
						break;
					} else {
						if (i == waitSec) {
							throw new TimeoutException("Blocked until:" + waitSec + "s");
						}
						if (!sessionMutex.getTimestamp().equals(timestamp)) {
							throw new InterruptedException("Interrupted before");
						}
					}
				}
			}
			session.setAttribute(HistogramsDataServlet.HISTOGRAM_SESSION_LOCK, new SessionMutex(timestamp));

			callback.doCancelableJob(request, logger, sessionMutex, timestamp, rootElement, messageElement);

			session.removeAttribute(HistogramsDataServlet.HISTOGRAM_SESSION_ID);
		} catch (TimeoutException e) {
			messageElement.setText("INFO: Rejected until Timeout!");
			rootElement.addContent(new Element("exceptions").setText(StringUtil.printStackTrace2String(e)));
			rootElement.addContent(new Element("description").setText(e.getMessage()));
		} catch (InterruptedException e) {
			messageElement.setText("INFO: interupted by another query!");
//			rootElement.addContent(new Element("exceptions").setText(StringUtil.printStackTrace2String(e)));
//			rootElement.addContent(new Element("description").setText(e.getMessage()));
		} finally {
			session.removeAttribute(HistogramsDataServlet.HISTOGRAM_SESSION_LOCK);
		}
	}
}
