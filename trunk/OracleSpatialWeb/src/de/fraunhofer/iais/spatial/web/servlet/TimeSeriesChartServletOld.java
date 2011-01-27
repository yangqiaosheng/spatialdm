package de.fraunhofer.iais.spatial.web.servlet;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.support.WebApplicationContextUtils;

import de.fraunhofer.iais.spatial.dto.FlickrEuropeAreaDto;
import de.fraunhofer.iais.spatial.entity.FlickrPolygon;
import de.fraunhofer.iais.spatial.service.FlickrEuropeAreaMgr;
import de.fraunhofer.iais.spatial.util.StringUtil;

public class TimeSeriesChartServletOld extends HttpServlet {
	/**
	* Logger for this class
	*/
	private static final Logger logger = LoggerFactory.getLogger(TimeSeriesChartServletOld.class);

	private static final long serialVersionUID = -4033923021316859791L;

	private static FlickrEuropeAreaMgr areaMgr = null;

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		logger.debug("requestUrl:" + request.getRequestURL()); //$NON-NLS-1$
		response.setContentType("image/png");
		response.setHeader("Cache-Control", "no-cache");
		ServletOutputStream sos = response.getOutputStream();

		String areaid = request.getParameter("areaid");

		if (!StringUtils.isNumeric(areaid)) {
			return;
		} else {

			try {
				FlickrEuropeAreaDto areaDto = (FlickrEuropeAreaDto) request.getSession().getAttribute("areaDto");
				if (areaDto != null) {
					FlickrPolygon area = areaMgr.getAreaDao().getAreaById(Integer.parseInt(areaid), areaDto.getRadius());
					if (area != null) {
						areaMgr.createTimeSeriesChartOld(area, areaDto.getYears4Chart(), sos);
					}
				}
			} catch (Exception e) {
				logger.error("doGet(HttpServletRequest, HttpServletResponse)", e); //$NON-NLS-1$
			}
		}

		sos.flush();
		sos.close();
	}

	/**
	 * Initialization of the servlet. <br>
	 *
	 * @throws ServletException
	 *             - if an error occurs
	 */
	@Override
	public void init() throws ServletException {
		areaMgr = WebApplicationContextUtils.getRequiredWebApplicationContext(this.getServletContext()).getBean("flickrDeWestAreaMgr", FlickrEuropeAreaMgr.class);
	}
}
