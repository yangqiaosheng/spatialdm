package de.fraunhofer.iais.spatial.test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.annotation.Resource;

import org.jdom.CDATA;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import de.fraunhofer.iais.spatial.dao.jdbc.DB;
import de.fraunhofer.iais.spatial.util.XmlUtil;

@ContextConfiguration("classpath:beans.xml")
public class TestUKRode extends AbstractJUnit4SpringContextTests {

	@Resource(name = "db")
	private DB db;

	@Test
	public void testView() throws SQLException {
		Connection conn = db.getConn();

		PreparedStatement selectStmt = db.getPstmt(conn, "select * from datasample_geom_view");

		ResultSet rset = db.getRs(selectStmt);
		int i = 0;
		while (rset.next()) {

			System.out.println(rset.getTimestamp("PUBLICATION_DATE"));
			System.out.println(rset.getString("LINK_CODE"));
			System.out.println(rset.getString("LOCATION_CODE_TO"));
			System.out.println(rset.getString("NAME_FROM"));
			System.out.println(rset.getString("LATITUDE_FROM"));

			if (i >= 500) {
				break;
			}
			i++;
		}
		rset.close();
		selectStmt.close();
		conn.close();
	}

	@Test
	public void testSimpleKmlfromView() throws SQLException {

		Document document = new Document();
		Namespace namespace = Namespace.getNamespace("http://earth.google.com/kml/2.1");
		Element rootElement = new Element("kml", namespace);
		document.setRootElement(rootElement);

		Element documentElement = new Element("Document", namespace);
		rootElement.addContent(documentElement);

		Connection conn = db.getConn();

		PreparedStatement selectStmt = db.getPstmt(conn, "select t.LINK_CODE, AVG(t.MEASURED_SPEED_KPH) AS SPEED from datasample t group by (t.LINK_CODE)");
		ResultSet rset = db.getRs(selectStmt);
		int i = 0;
		while (rset.next()) {
			int linkCode = rset.getInt("LINK_CODE");
			double avgSpeed = rset.getDouble("SPEED");

			PreparedStatement selectStmt2 = db.getPstmt(conn, "select t.LINK_CODE, t.LATITUDE_FROM, t.LONGITUDE_FROM, t.LATITUDE_TO, t.LONGITUDE_TO, t.LOCATION_CODE_FROM, t.LOCATION_CODE_TO from datasample_geom_view t" + " where t.LINK_CODE = ?");
			selectStmt2.setInt(1, linkCode);
			ResultSet rset2 = db.getRs(selectStmt2);
			if (rset2.next()) {

				String id = String.valueOf(linkCode);
				String name = "link-id: " + String.valueOf(linkCode);
				String description = "avg-speed: " + String.valueOf((float) avgSpeed) + "kph";
				String latFrom = rset2.getString("LATITUDE_FROM");
				String lngFrom = rset2.getString("LONGITUDE_FROM");
				String latTo = rset2.getString("LATITUDE_TO");
				String lngTo = rset2.getString("LONGITUDE_TO");

				if (latFrom == null || lngFrom == null || latTo == null || lngTo == null) {
					continue;
				}

				String lineStyleWidth = String.valueOf((int) ((avgSpeed - 40) / 15) + 1);
				String lineStyleColor = "FF0000";
				String coordinates = lngFrom + "," + latFrom + ", 0 \n" + lngTo + "," + latTo + ", 0";

				String redColor = Integer.toHexString((int) ((avgSpeed - 40) / 130 * 255) & 0x000000FF);

				// create kml
				Element placemarkElement = new Element("Placemark", namespace);
				documentElement.addContent(placemarkElement);

				Element nameElement = new Element("name", namespace);
				nameElement.addContent(name);
				Element descriptionElement = new Element("description", namespace);
				descriptionElement.addContent(new CDATA(description));

				Element styleElement = new Element("Style", namespace);
				placemarkElement.setAttribute("id", id);
				placemarkElement.addContent(nameElement);
				placemarkElement.addContent(descriptionElement);
				placemarkElement.addContent(styleElement);

				Element lineStyleElement = new Element("LineStyle", namespace);
				styleElement.addContent(lineStyleElement);

				Element lindWidthElement = new Element("width", namespace);
				lindWidthElement.addContent(lineStyleWidth);
				Element lineColorElement2 = new Element("color", namespace);
				lineColorElement2.addContent(lineStyleColor + redColor);
				lineStyleElement.addContent(lindWidthElement);
				lineStyleElement.addContent(lineColorElement2);

				Element lineStringElement = new Element("LineString", namespace);
				Element coordinatesElement = new Element("coordinates", namespace);
				placemarkElement.addContent(lineStringElement);
				lineStringElement.addContent(coordinatesElement);
				coordinatesElement.addContent(coordinates);
			}

			db.close(rset2);
			db.close(selectStmt2);

//			if (i >= 100) {
//				break;
//			}
//			i++;
		}

		db.close(rset);
		db.close(selectStmt);
		db.close(conn);
		rset.close();
		selectStmt.close();
		conn.close();

		XmlUtil.xml2File(document, "temp/testUK.kml", false);
		XmlUtil.xml2String(document, false);
	}

	@Test
	public void testSimpleXmlfromView() throws SQLException {

		long start = System.currentTimeMillis();

		Document document = new Document();
		Element rootElement = new Element("Paths");
		document.setRootElement(rootElement);

		Connection conn = db.getConn();

		PreparedStatement selectStmt = db.getPstmt(conn, "select t.LINK_CODE, AVG(t.MEASURED_SPEED_KPH) AS SPEED from datasample t group by (t.LINK_CODE)");
		ResultSet rset = db.getRs(selectStmt);
		int i = 0;
		while (rset.next()) {
			int linkCode = rset.getInt("LINK_CODE");
			double avgSpeed = rset.getDouble("SPEED");

			PreparedStatement selectStmt2 = db.getPstmt(conn, "select t.LINK_CODE, t.LATITUDE_FROM, t.LONGITUDE_FROM, t.LATITUDE_TO, t.LONGITUDE_TO, t.LOCATION_CODE_FROM, t.LOCATION_CODE_TO from datasample_geom_view t" + " where t.LINK_CODE = ?");
			selectStmt2.setInt(1, linkCode);
			ResultSet rset2 = db.getRs(selectStmt2);
			if (rset2.next()) {

				String id = String.valueOf(linkCode);
				String description = String.valueOf((float) avgSpeed) + "kph";
				String latFrom = rset2.getString("LATITUDE_FROM");
				String lngFrom = rset2.getString("LONGITUDE_FROM");
				String latTo = rset2.getString("LATITUDE_TO");
				String lngTo = rset2.getString("LONGITUDE_TO");

				if (latFrom == null || lngFrom == null || latTo == null || lngTo == null) {
					continue;
				}

				String lineStyleWidth = String.valueOf((int) ((avgSpeed - 40) / 15) + 1);
				String lineStyleColor = "FF0000";
				String coordinates = lngFrom + "," + latFrom + ", 0 \n" + lngTo + "," + latTo + ", 0";

				String redColor = Integer.toHexString((int) ((avgSpeed - 40) / 130 * 255) & 0x000000FF);

				// create kml
				Element pathElement = new Element("Path");
				rootElement.addContent(pathElement);

				Element speedElement = new Element("Speed");
				speedElement.addContent(description);

				pathElement.setAttribute("id", id);
				pathElement.addContent(speedElement);

				Element lindWidthElement = new Element("LineWidth");
				lindWidthElement.addContent(lineStyleWidth);
				Element lineColorElement2 = new Element("LineColor");
				lineColorElement2.addContent(lineStyleColor + redColor);
				pathElement.addContent(lindWidthElement);
				pathElement.addContent(lineColorElement2);

				Element coordinatesElement = new Element("Coordinates");
				pathElement.addContent(coordinatesElement);
				coordinatesElement.addContent(coordinates);
			}

			db.close(rset2);
			db.close(selectStmt2);

//				if (i >= 5) {
//					break;
//				}
			i++;
		}

		System.out.println("i:" + i);
		db.close(rset);
		db.close(selectStmt);
		db.close(conn);
		rset.close();
		selectStmt.close();
		conn.close();

		XmlUtil.xml2File(document, "temp/testUK.xml", false);
		XmlUtil.xml2String(document, false);

		System.out.println("cost time: " + (System.currentTimeMillis() - start));
	}

}