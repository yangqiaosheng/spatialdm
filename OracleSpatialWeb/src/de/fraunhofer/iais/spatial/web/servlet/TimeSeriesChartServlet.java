package de.fraunhofer.iais.spatial.web.servlet;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.support.WebApplicationContextUtils;

import de.fraunhofer.iais.spatial.dto.FlickrAreaDto;
import de.fraunhofer.iais.spatial.dto.FlickrAreaDto.Level;
import de.fraunhofer.iais.spatial.entity.FlickrArea;
import de.fraunhofer.iais.spatial.entity.FlickrArea.Radius;
import de.fraunhofer.iais.spatial.service.FlickrAreaMgr;
import de.fraunhofer.iais.spatial.util.FlickrAreaUtil;

public class TimeSeriesChartServlet extends HttpServlet {
	private static final int DEFAULT_HEIGHT = 300;
	private static final int DEFAULT_WIDTH = 800;
	private static final int MAX_NUM_DATASETS = 5;
	private static final int MAX_WIDTH = 2000;
	private static final int MAX_HEIGHT = 1500;

	/**
	* Logger for this class
	*/
	private static final Logger logger = LoggerFactory.getLogger(TimeSeriesChartServlet.class);

	private static final long serialVersionUID = -4033923021316859791L;

	private static FlickrAreaMgr areaMgr = null;

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String webAppPath = getServletContext().getRealPath("/");
		response.setContentType("image/png");
		response.setHeader("Cache-Control", "no-cache");
		ServletOutputStream sos = response.getOutputStream();

		String[] areaids = ArrayUtils.subarray(request.getParameterValues("areaid"), 0, MAX_NUM_DATASETS);
		String level = request.getParameter("level");
		int width = NumberUtils.toInt(request.getParameter("width"), DEFAULT_WIDTH);
		int height = NumberUtils.toInt(request.getParameter("height"), DEFAULT_HEIGHT);
		boolean smooth = BooleanUtils.toBoolean(request.getParameter("smooth"));

		if (width > MAX_WIDTH) {
			width = MAX_WIDTH;
		}

		if (height > MAX_HEIGHT) {
			height = MAX_HEIGHT;
		}

		logger.info("requestUrl:" + request.getRequestURL() + " |areaids:" + ToStringBuilder.reflectionToString(areaids) + " |level:" + level + " |smooth:" + smooth); //$NON-NLS-1$

		if (ArrayUtils.isEmpty(areaids) || StringUtils.isEmpty(level)) {
			IOUtils.copy(new FileInputStream(webAppPath + "images/tsc-warning1.png"), sos);
		} else if (request.getSession().getAttribute("areaDto") == null) {
			IOUtils.copy(new FileInputStream(webAppPath + "images/tsc-warning2.png"), sos);
		} else {
			try {
				List<FlickrArea> areas = new ArrayList<FlickrArea>();
				FlickrAreaDto areaDto = SerializationUtils.clone((FlickrAreaDto) request.getSession().getAttribute("areaDto"));

				int zoom = NumberUtils.toInt(request.getParameter("zoom"), areaDto.getZoom());
				Radius radius = FlickrAreaUtil.judgeRadius(zoom);
				for (String areaid : areaids) {
					FlickrArea area = areaMgr.getAreaDao().getAreaById(Integer.parseInt(areaid), radius);
					if (area != null) {
						areas.add(area);
					}
				}

				boolean displayLegend = false;
				if (areas.size() > 1) {
					displayLegend = true;
				}

				boolean icon = false;
				if (width <= 300) {
					icon = true;
				}

				areaMgr.createTimeSeriesChart(areas, Level.valueOf(level.toUpperCase()), areaDto, width, height, displayLegend, smooth, icon, sos);

			} catch (Exception e) {
//				IOUtils.copy(new FileInputStream(webAppPath + "images/tsc-warning1.png"), sos);
				logger.error("doGet(HttpServletRequest, HttpServletResponse)", e); //$NON-NLS-1$
			}
		}

		sos.flush();
		sos.close();
		System.gc();
	}

	/**
	 * Initialization of the servlet. <br>
	 *
	 * @throws ServletException
	 *             - if an error occurs
	 */
	@Override
	public void init() throws ServletException {
		areaMgr = WebApplicationContextUtils.getRequiredWebApplicationContext(this.getServletContext()).getBean("flickrAreaMgr", FlickrAreaMgr.class);
	}
}
