package de.fraunhofer.iais.spatial.web.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jdom.Document;
import org.jdom.Element;
import org.springframework.web.context.support.WebApplicationContextUtils;

import de.fraunhofer.iais.spatial.dto.FlickrDeWestAreaDto;
import de.fraunhofer.iais.spatial.entity.FlickrDeWestPhoto;
import de.fraunhofer.iais.spatial.entity.FlickrDeWestArea.Radius;
import de.fraunhofer.iais.spatial.service.FlickrDeWestAreaMgr;
import de.fraunhofer.iais.spatial.util.XmlUtil;


public class SmallPhotoUrlServlet extends HttpServlet {

	private static final long serialVersionUID = 289355222687198395L;

	private static final int NUMBER_OF_PHOTOS = 10;
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

		response.setContentType("text/xml");
		String areaid = request.getParameter("areaid");
		PrintWriter out = response.getWriter();

		Document document = new Document();
		Element rootElement = new Element("response");
		document.setRootElement(rootElement);
		Element messageElement = new Element("message");
		rootElement.addContent(messageElement);

		if (areaid == null || areaid.equals("")) {
			messageElement.setText("wrong input parameter!");
		} else {
			FlickrDeWestAreaDto areaDto = (FlickrDeWestAreaDto) request.getSession().getAttribute("areaDto");

			System.out.println("areaid:" + areaid +"|radius:" + areaDto.getRadius() + "|queryStrs:" + areaDto.getQueryStrs());

			photosResponseXml(document, Integer.parseInt(areaid), Radius.valueOf("R" + areaDto.getRadius()), areaDto.getQueryStrs(), 20);
		}

		out.print(XmlUtil.xml2String(document, true));
		System.out.println(XmlUtil.xml2String(document, false));
		out.flush();
		out.close();
	}

//	private Set<String> createQueryStrs(FlickrDeWestAreaDto areaDto){
//
//	}
//
//	private Set<String> createQueryStr(FlickrDeWestAreaDto areaDto){
//
//	}

	private String photosResponseXml(Document document, int areaid, Radius radius, SortedSet<String> queryStrs, int num) {
		List<FlickrDeWestPhoto> photos = areaMgr.getAreaDao().getPhotos(areaid, radius, queryStrs, NUMBER_OF_PHOTOS);

		Element rootElement = document.getRootElement();

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
