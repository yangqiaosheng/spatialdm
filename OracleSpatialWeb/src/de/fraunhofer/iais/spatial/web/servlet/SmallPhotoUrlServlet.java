package de.fraunhofer.iais.spatial.web.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jdom.Document;
import org.jdom.Element;
import org.springframework.web.context.support.WebApplicationContextUtils;

import de.fraunhofer.iais.spatial.entity.FlickrDeWestPhoto;
import de.fraunhofer.iais.spatial.entity.FlickrDeWestArea.Radius;
import de.fraunhofer.iais.spatial.service.FlickrDeWestAreaMgr;
import de.fraunhofer.iais.spatial.util.XmlUtil;

public class SmallPhotoUrlServlet extends HttpServlet {

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
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		response.setContentType("text/html");
		String areaid = request.getParameter("areaid");
		String radius = request.getParameter("radius");
		PrintWriter out = response.getWriter();

		Document document = new Document();
		Element rootElement = new Element("response");
		document.setRootElement(rootElement);
		Element messageElement = new Element("message");
		rootElement.addContent(messageElement);

		if ((areaid == null || areaid.equals("")) && (radius == null || radius.equals(""))) {
			messageElement.setText("wrong input parameter!");
		} else {
			FlickrDeWestAreaMgr areaMgr = WebApplicationContextUtils.getRequiredWebApplicationContext(this.getServletContext()).getBean("flickrDeWestAreaMgr", FlickrDeWestAreaMgr.class);
			FlickrDeWestPhoto photo = areaMgr.getAreaDao().getPhoto(Integer.parseInt(areaid), Radius.valueOf("_" + radius), "2007-08-11@13", 20);

			Element photosElement = new Element("photos");
			rootElement.addContent(photosElement);
			if (photo != null) {
				//				photosElement.addContent(new Element("))
				System.out.println(photo.getSmallUrl());
			}
		}

		out.print(XmlUtil.xml2String(document, true));
		out.flush();
		out.close();
	}

}
