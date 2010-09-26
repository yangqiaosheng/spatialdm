package de.fraunhofer.iais.spatial.web.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Set;

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

	private static FlickrDeWestAreaMgr areaMgr = null;
	
	
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
			FlickrDeWestPhoto photo = areaMgr.getAreaDao().getPhoto(Integer.parseInt(areaid), Radius.valueOf("R" + radius), "2007-08-11@13", 20);

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
	
	public String photosResponseXml(int areaid, Radius radius, Set<String> hours, int num) {
		List<FlickrDeWestPhoto> photos = areaMgr.getAreaDao().getPhotos(areaid, Radius.R80000, hours, 20);

		Document document = new Document();
		Element rootElement = new Element("response");
		document.setRootElement(rootElement);

		Element photosElement = new Element("photos");
		rootElement.addContent(photosElement);

		int i = 1;
		for (FlickrDeWestPhoto p : photos) {
			Element photoElement = new Element("photo");
			photosElement.addContent(photoElement);
			photoElement.setAttribute("index", String.valueOf(i++));

			photoElement.addContent(new Element("photoId").setText(String.valueOf(p.getId())));
			photoElement.addContent(new Element("polygonId").setText(String.valueOf(p.getArea().getId())));
			photoElement.addContent(new Element("polygonRadius").setText(String.valueOf(p.getArea().getRadius())));
			photoElement.addContent(new Element("polygonArea").setText(String.valueOf(p.getArea().getArea())));
			photoElement.addContent(new Element("date").setText(String.valueOf(p.getDate())));
			photoElement.addContent(new Element("latitude").setText(String.valueOf(p.getLatitude())));
			photoElement.addContent(new Element("longitude").setText(String.valueOf(p.getLongitude())));
			photoElement.addContent(new Element("personId").setText(String.valueOf(p.getPersonId())));
			photoElement.addContent(new Element("title").setText(String.valueOf(p.getTitle())));
			photoElement.addContent(new Element("smallUrl").setText(String.valueOf(p.getSmallUrl())));
			photoElement.addContent(new Element("viewed").setText(String.valueOf(p.getViewed())));
			photoElement.addContent(new Element("rawTags").setText(String.valueOf(p.getRawTags())));
		}

		return XmlUtil.xml2String(document, true);
	}
	
	/**
	 * Initialization of the servlet. <br>
	 * 
	 * @throws ServletException
	 *             - if an error occurs
	 */
	@Override
	public void init() throws ServletException {
		areaMgr = WebApplicationContextUtils.getRequiredWebApplicationContext(this.getServletContext()).getBean("flickrDeWestAreaMgr", FlickrDeWestAreaMgr.class);
	}
}
