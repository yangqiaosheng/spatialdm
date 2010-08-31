package de.fraunhofer.iais.spatial.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.context.support.WebApplicationContextUtils;

import de.fraunhofer.iais.spatial.entity.Area;
import de.fraunhofer.iais.spatial.entity.FlickrDeWestPhoto;
import de.fraunhofer.iais.spatial.entity.FlickrDeWestArea.Radius;
import de.fraunhofer.iais.spatial.service.AreaMgr;
import de.fraunhofer.iais.spatial.service.FlickrDeWestAreaMgr;

public class SmallPhotoUrl extends HttpServlet {

	/**
		 * The doGet method of the servlet. <br>
		 *
		 * This method is called when a form has its tag value method equals to get.
		 * 
		 * @param request the request send by the client to the server
		 * @param response the response send by the server to the client
		 * @throws ServletException if an error occurred
		 * @throws IOException if an error occurred
		 */
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		response.setContentType("text/html");
		String areaid = request.getParameter("areaid");
		String radius = request.getParameter("radius");
		PrintWriter out = response.getWriter();
		if ((areaid == null || areaid.equals("")) && (radius == null || radius.equals(""))) {
			out.print("<response><msg>no parameters!</msg></response>");
			return;
		}
		
		FlickrDeWestAreaMgr areaMgr = WebApplicationContextUtils.getRequiredWebApplicationContext(this.getServletContext()).getBean("flickrDeWestAreaMgr", FlickrDeWestAreaMgr.class);
		FlickrDeWestPhoto photo = areaMgr.getPhoto(Integer.parseInt(areaid), Radius.valueOf("_" + radius), "2007-08-11@13", 20);
		
		if(photo !=null){
			out.print(photo.getSmallUrl());
			System.out.println(photo.getSmallUrl());
		}
		out.flush();
		out.close();
	}

}
