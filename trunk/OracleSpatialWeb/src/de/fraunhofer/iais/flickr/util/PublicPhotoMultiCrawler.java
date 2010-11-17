package de.fraunhofer.iais.flickr.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.util.Random;
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

public class PublicPhotoMultiCrawler extends Thread {
	/**
	* Logger for this class
	*/
	private static final Logger logger = LoggerFactory.getLogger(PublicPhotoMultiCrawler.class);

	static final int pageSize = 500;
	static final int NUM_THREAD = 3;
	static final int MAX_NUM_RETRY = 500;

	static int numReTry = 0;
	static long numPeople = 0;
	static long numPhoto = 0;
	static long numTotalQuery = 0;

	static DBUtil db = new DBUtil();


	public synchronized static int increaseNumReTry(){
		return numReTry++;
	}

	public synchronized static long increaseNumPeople(){
		return numPeople++;
	}

	public synchronized static long increaseNumPhoto(){
		return numPhoto++;
	}

	public synchronized static long increaseNumTotalQuery(){
		return numTotalQuery++;
	}


	public synchronized static int getNumReTry() {
		return numReTry;
	}

	public synchronized static long getNumPeople() {
		return numPeople;
	}

	public synchronized static long getNumPhoto() {
		return numPhoto;
	}

	public synchronized static long getNumTotalQuery() {
		return numTotalQuery;
	}

	@Override
	public void run() {

		Date startDate = new Date();
		long start = System.currentTimeMillis();
		int id = Integer.parseInt(this.getName());

		/*
		System.setProperty("oraclespatialweb.root", System.getProperty("user.dir"));
		System.out.println("oraclespatialweb.root:" + System.getProperty("oraclespatialweb.root"));
		 */

		PeopleInterface peopleInterface = null;
		try {
			sleep(id * 1000);
			peopleInterface = init(this.getName());
		} catch (Exception e) {
			logger.error("main(String[])", e); //$NON-NLS-1$
		}

		while (getNumReTry() <= MAX_NUM_RETRY) {

			try {
				selectPeople(id, peopleInterface);

			} catch (Exception e) {
				logger.error("main(String[])", e); //$NON-NLS-1$
			} finally {
				Date endDate = new Date();
				long end = System.currentTimeMillis();
				logger.info("main(String[]) - ThreadId:" + this.getName()); //$NON-NLS-1$
				logger.info("main(String[]) - cost time:" + (end - start) + "ms"); //$NON-NLS-1$
				logger.info("main(String[]) - start date:" + startDate); //$NON-NLS-1$
				logger.info("main(String[]) - end date:" + endDate); //$NON-NLS-1$
				logger.info("main(String[]) - numPhoto:" + getNumPhoto()); //$NON-NLS-1$
				logger.info("main(String[]) - numPeople:" + getNumPeople()); //$NON-NLS-1$
				logger.info("main(String[]) - numTotalQuery:" + getNumTotalQuery()); //$NON-NLS-1$
				logger.info("main(String[]) - numReTry:" + getNumReTry()); //$NON-NLS-1$
			}

			try {
				sleep(30 * 1000);
			} catch (InterruptedException e) {
				logger.error("main(String[])", e); //$NON-NLS-1$
			}
			increaseNumReTry();
		}
	}

	private PeopleInterface init(String ThreadId) throws IOException, ParserConfigurationException {

		Properties properties = new Properties();
		properties.load(PublicPhotoMultiCrawler.class.getResourceAsStream("/flickr.properties"));
		String apiKey = properties.getProperty("apiKey_" + ThreadId);
		String secret = properties.getProperty("secret_" + ThreadId);
		String token = properties.getProperty("token_" + ThreadId);
		Flickr flickr = new Flickr(apiKey, secret, new REST());
		RequestContext requestContext = RequestContext.getRequestContext();
		Auth auth = new Auth();
		auth.setPermission(Permission.READ);
		auth.setToken(token);

		logger.info("init(String) - ThreadID:" + ThreadId); //$NON-NLS-1$
		logger.info("init(String) - apiKey:" + apiKey); //$NON-NLS-1$
		logger.info("init(String) - secret:" + secret); //$NON-NLS-1$
		logger.info("init(String) - token:" + token); //$NON-NLS-1$

		requestContext.setAuth(auth);
		Flickr.debugRequest = true;
		Flickr.debugStream = true;
		return flickr.getPeopleInterface();
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

	public static void getPeoplesPhotos(PeopleInterface peopleInterface, String peopleId) throws IOException, SAXException, FlickrException, SQLException {

		Set<String> extras = new HashSet<String>();
		extras.add(Extras.DATE_TAKEN);
		extras.add(Extras.GEO);
		extras.add(Extras.VIEWS);
//		extras.add(Extras.TAGS);

		PhotoList photolist;
		int total = 0;
		int num = 0;

		updatePeoplesNum(peopleId, -2);
		do {
			photolist = peopleInterface.getPublicPhotos(peopleId, extras, pageSize, (int) (num / pageSize + 1));
			total = photolist.getTotal();
			increaseNumTotalQuery();

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
			System.out.println("numTotalQuery:" + getNumTotalQuery());
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

	@SuppressWarnings("deprecation")
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
			pstmt.setString(i++, photo.getSmallUrl());

			pstmt.executeUpdate();

			System.out.println("numPhoto:" + increaseNumPhoto());
		} finally {
			db.close(pstmt);
			db.close(conn);
		}
	}

	private static void selectPeople(int id, PeopleInterface peopleInterface) throws IOException, SAXException, FlickrException, SQLException {
		Connection conn = db.getConn();
		PreparedStatement pstmt = db.getPstmt(conn, "select PERSON from PEOPLE_EUROPE where photos_num_new = -1");

		ResultSet rs = null;
		try {
			rs = db.getRs(pstmt);
			while (rs.next()) {

				String peopleId = rs.getString("PERSON");

				// assign the task to different thread
				if (new Random(peopleId.hashCode()).nextInt(NUM_THREAD) == id) {
					getPeoplesPhotos(peopleInterface, peopleId);
					System.out.println("numPeople:" + increaseNumPeople());
				}
			}
		} finally {
			db.close(rs);
			db.close(pstmt);
			db.close(conn);
		}
	}

	public static void main(String[] args) {


		for (int i = 0; i < NUM_THREAD; i++) {
			PublicPhotoMultiCrawler crawler = new PublicPhotoMultiCrawler();
			crawler.start();
			crawler.setName(String.valueOf(i));
		}

	}
}
