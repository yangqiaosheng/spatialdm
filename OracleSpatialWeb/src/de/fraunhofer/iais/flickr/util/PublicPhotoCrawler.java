package de.fraunhofer.iais.flickr.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.aetrion.flickr.Flickr;
import com.aetrion.flickr.FlickrException;
import com.aetrion.flickr.REST;
import com.aetrion.flickr.RequestContext;
import com.aetrion.flickr.auth.Auth;
import com.aetrion.flickr.auth.Permission;
import com.aetrion.flickr.people.PeopleInterface;
import com.aetrion.flickr.photos.Extras;
import com.aetrion.flickr.photos.GeoData;
import com.aetrion.flickr.photos.Photo;
import com.aetrion.flickr.photos.PhotoList;

import de.fraunhofer.iais.spatial.util.DBUtil;

public class PublicPhotoCrawler {

	static long numPeople = 0;
	static long numPhoto = 0;

	static int pageSize = 500;

	static DBUtil db = new DBUtil();
	static String apiKey;
	static String sharedSecret;
	static Flickr flickr;
	static RequestContext requestContext;
	static Properties properties = null;

	private static void init() throws IOException, ParserConfigurationException {
		InputStream in = null;
		in = PublicPhotoCrawler.class.getResourceAsStream("/flickr.properties");
		properties = new Properties();
		properties.load(in);
		flickr = new Flickr(properties.getProperty("apiKey"), properties.getProperty("secret"), new REST());
		requestContext = RequestContext.getRequestContext();
		Auth auth = new Auth();
		auth.setPermission(Permission.READ);
		auth.setToken(properties.getProperty("token"));
		requestContext.setAuth(auth);
		Flickr.debugRequest = true;
		Flickr.debugStream = true;
	}

	public static boolean checkLocation(GeoData geoData) {
		double x1 = -13.119622;
		double x2 = 35.287624;
		double y1 = 34.26329;
		double y2 = 72.09216;

		if (geoData.getLongitude() > x1 && geoData.getLongitude() < x2 && geoData.getLatitude() > y1 && geoData.getLatitude() < y2) {
			return true;
		} else {
			return false;
		}
	}

	public static void getPeoplesPhotos(String peopleId) throws IOException, SAXException, FlickrException, SQLException {
		PeopleInterface people = flickr.getPeopleInterface();
		Set<String> extras = new HashSet<String>();
		extras.add(Extras.DATE_TAKEN);
		extras.add(Extras.GEO);
		extras.add(Extras.VIEWS);
//		extras.add(Extras.TAGS);

		PhotoList photolist;
		int total = 0;
		int num = 0;

		do {

			photolist = people.getPublicPhotos(peopleId, extras, pageSize, (int) (num / pageSize + 1));
			total = photolist.getTotal();
			for (int i = 0; i < photolist.size(); i++) {
				Photo p = (Photo) photolist.get(i);
				if (p.getGeoData() != null && checkLocation(p.getGeoData())) {
//					PlacesInterface placeI = flickr.getPlacesInterface();
//					System.out.println(p.getId() + ":" + p.getDateTaken() + ":" + p.getGeoData() + ":" + p.getDescription() + ":" + p.getPlaceId() + ":" + p.getWoeId());
//					System.out.println("place_id:" + p.getPlaceId());
					insertPhoto(p);
				}
			}
			num += photolist.size();

			System.out.println("owner_id:" + peopleId);
			System.out.println("total:" + total + " | num:" + num);
		} while (num < total);

		updatePeoplesNum(peopleId, total);
	}

	private static void updatePeoplesNum(String person, int num) throws SQLException {
		Connection conn = db.getConn();
		PreparedStatement pstmt = db.getPstmt(conn, "update PEOPLE_EUROPE set PHOTOS_NUM_NEW = ? where PERSON = ?");
		try {
			int i = 1;
			pstmt.setInt(i++, num);
			pstmt.setString(i++, person);
			pstmt.executeUpdate();

		} finally {
			db.close(pstmt);
			db.close(conn);
		}
	}

	private static void insertPhoto(Photo photo) throws SQLException {
		Connection conn = db.getConn();
		PreparedStatement pstmt = db.getPstmt(conn, "insert into FLICKR_EUROPE (PERSON, PHOTO_ID, LONGITUDE, LATITUDE, DT, VIEWED, TITLE, SMALLURL) values (?, ?, ?, ?, TO_DATE(?, 'YYYY-MM-DD HH24:MI:SS'), ?, ?, ?)");
		try {
			int i = 1;
			pstmt.setString(i++, photo.getOwner().getId());
			pstmt.setString(i++, photo.getId());
			pstmt.setDouble(i++, photo.getGeoData().getLongitude());
			pstmt.setDouble(i++, photo.getGeoData().getLatitude());

			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

			pstmt.setString(i++, formatter.format(photo.getDateTaken()));
			pstmt.setInt(i++, photo.getViews());
			pstmt.setString(i++, photo.getTitle());
//			pstmt.setString(i++, photo.getTags().toString());
			pstmt.setString(i++, photo.getSmallUrl());

			pstmt.executeUpdate();

			System.out.println("numPhoto:" + numPhoto++);
		} finally {
			db.close(pstmt);
			db.close(conn);
		}
	}

	private static void selectPeople() throws IOException, SAXException, FlickrException, SQLException {
		Connection conn = db.getConn();
		PreparedStatement pstmt = db.getPstmt(conn, "select PERSON from PEOPLE_EUROPE where photos_num_new = -1");

		ResultSet rs = null;
		try {
			rs = db.getRs(pstmt);
			while (rs.next()) {
//				if (numPeople < 1786) {
//					numPeople++;
//					continue;
//				}

				String peopleId = rs.getString("PERSON");
				getPeoplesPhotos(peopleId);
				System.out.println("numPeople:" + numPeople++);
			}
		} finally {
			db.close(rs);
			db.close(pstmt);
			db.close(conn);
		}
	}

	public static void main(String[] args) {

		Date startDate = new Date();
		long start = System.currentTimeMillis();

		try {
			init();
			selectPeople();
//			getPeoplesPhotos("27076251@N05");

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			Date endDate = new Date();
			long end = System.currentTimeMillis();
			System.out.println("cost time:" + (end - start));
			System.out.println("start date:" + startDate);
			System.out.println("end date:" + endDate);
			System.out.println("numPhoto:" + numPhoto);
			System.out.println("numPeople:" + numPeople);
		}

	}
}
