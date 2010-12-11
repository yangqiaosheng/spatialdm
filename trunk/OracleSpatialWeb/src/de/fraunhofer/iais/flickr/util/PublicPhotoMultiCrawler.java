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
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

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
	static final int NUM_THREAD = 6;
	static final int MAX_NUM_RETRY = 250;
	static final int MAX_TITLE_LENGTH = 255;

	static final double MIN_LONGITUDE = -13.119622;
	static final double MIN_LATITUDE = 34.26329;
	static final double MAX_LONGITUDE = 35.287624;
	static final double MAX_LATITUDE = 72.09216;

	static Calendar beginDateLimit;
	static int numReTry = 0;
	static long numPeople = 0;
	static long numPhoto = 0;
	static long numTotalQuery = 0;

	static DBUtil db = new DBUtil();

	boolean finished = false;

	public synchronized static int increaseNumReTry() {
		return ++numReTry;
	}

	public synchronized static long increaseNumPeople() {
		return ++numPeople;
	}

	public synchronized static long increaseNumPhoto() {
		return ++numPhoto;
	}

	public synchronized static long increaseNumTotalQuery() {
		return ++numTotalQuery;
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

	public boolean checkLocation(GeoData geoData) {

		if (geoData != null && geoData.getLongitude() > MIN_LONGITUDE && geoData.getLongitude() < MAX_LONGITUDE && geoData.getLatitude() > MIN_LATITUDE && geoData.getLatitude() < MAX_LATITUDE) {
			return true;
		} else {
			return false;
		}
	}

	public boolean checkDate(Date takenDate, Date uploadDate) {
		if (takenDate != null && uploadDate != null && !takenDate.before(beginDateLimit.getTime()) && !takenDate.after(new Date()) && uploadDate.after(takenDate)) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void run() {

		Date startDate = new Date();
		long start = System.currentTimeMillis();

		try {
			sleep(100);
		} catch (InterruptedException e) {
			logger.error("main(String[])", e); //$NON-NLS-1$
		}

		int threadId = Integer.parseInt(this.getName());

		while (getNumReTry() <= MAX_NUM_RETRY) {

			try {
				PeopleInterface peopleInterface = init(this.getName());
				selectPeople(threadId, peopleInterface);
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

			if (finished) {
				// process finished and exit
				break;
			}

			try {
				sleep(300 * 1000);
			} catch (InterruptedException e) {
				logger.error("main(String[])", e); //$NON-NLS-1$
			} finally {
				increaseNumReTry();
			}
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

	private void selectPeople(int threadId, PeopleInterface peopleInterface) throws IOException, SAXException, FlickrException, SQLException {
		Connection conn = db.getConn();
		PreparedStatement pstmt = db.getPstmt(conn, "select USER_ID, LAST_UPLOAD_DATE from FLICKR_PEOPLE t where t.CHECKED_PHOTO_UPDATE = 0");

		ResultSet rs = null;
		try {
			rs = db.getRs(pstmt);
			while (rs.next()) {

				String userId = rs.getString("USER_ID");
				Date lastUploadDate = rs.getTimestamp("LAST_UPLOAD_DATE");

				// assign the task to different thread
				if (new Random(userId.hashCode()).nextInt(NUM_THREAD) == threadId) {
					retrievePeoplesPhotos(peopleInterface, userId, lastUploadDate);
					System.out.println("numPeople:" + increaseNumPeople());
				}
			}

			// process finished
			finished = true;
		} finally {
			db.close(rs);
			db.close(pstmt);
			db.close(conn);
		}
	}

	private void retrievePeoplesPhotos(PeopleInterface peopleInterface, String userId, Date lastUploadDate) throws IOException, SAXException, FlickrException, SQLException {

		Set<String> extras = new HashSet<String>();
		extras.add(Extras.DATE_TAKEN);
		extras.add(Extras.DATE_UPLOAD);
		extras.add(Extras.GEO);
		extras.add(Extras.VIEWS);
		//		extras.add(Extras.TAGS);

		PhotoList photos;
		int pages = 0;
		int page = 1;
		int total = 0;
		int num = 0;

		Calendar minUploadDate = beginDateLimit;
		if (lastUploadDate != null && lastUploadDate.after(beginDateLimit.getTime())) {
			minUploadDate.setTime(lastUploadDate);
		}

		Calendar maxUploadDate = Calendar.getInstance();
		Calendar minTakenDate = beginDateLimit;
		Calendar maxTakenDate = Calendar.getInstance();

		PhotoList insertPhotos = new PhotoList();
		do {
			photos = peopleInterface.getPhotos(userId, minUploadDate.getTime(), maxUploadDate.getTime(), minTakenDate.getTime(), maxTakenDate.getTime(), extras, pageSize, page++);
//			photos = peopleInterface.getSearchWithGeoPhoto(userId, minUploadDate.getTime(), maxUploadDate.getTime(), minTakenDate.getTime(), maxTakenDate.getTime(), MIN_LONGITUDE, MIN_LATITUDE, MAX_LONGITUDE, MAX_LATITUDE, extras, pageSize, page++);
			total = photos.getTotal();
			pages = photos.getPages();
			increaseNumTotalQuery();

			for (int i = 0; i < photos.size(); i++) {
				Photo p = (Photo) photos.get(i);
				if (checkDate(p.getDateTaken(), p.getDatePosted()) && checkLocation(p.getGeoData())) {
					insertPhotos.add(p);
				}
			}
			num += photos.size();

			System.out.println("owner_id:" + userId);
			System.out.println("total:" + total + " | num:" + num);
			System.out.println("numTotalQuery:" + getNumTotalQuery());
		} while (page <= pages);

		insertPhotos(insertPhotos, userId);
	}

	private void insertPhotos(PhotoList photos, String userId) throws SQLException {
		Connection conn = db.getConn();
		TreeSet<Date> uploadDates = new TreeSet<Date>();
		TreeSet<Date> takenDates = new TreeSet<Date>();
		try {
			conn.setAutoCommit(false);

			for (int i = 0; i < photos.size(); i++) {
				Photo p = (Photo) photos.get(i);
				insertPhoto(conn, p);
				uploadDates.add(p.getDatePosted());
				takenDates.add(p.getDateTaken());
			}

			if (photos.size() > 0) {
				updatePeoplesInfo(conn, userId, uploadDates.last(), takenDates.last());
			} else {
				updatePeoplesInfo(conn, userId);
			}

			conn.commit();
		} catch (SQLException e) {
			logger.error("insertPeople()", e); //$NON-NLS-1$
			try {
				conn.rollback();
			} catch (SQLException e1) {
				logger.error("insertPeople()", e); //$NON-NLS-1$
			}
		} finally {
			db.close(conn);
		}
	}

	private void insertPhoto(Connection conn, Photo photo) throws SQLException {
		PreparedStatement pstmt = db
				.getPstmt(
						conn,
						"insert into FLICKR_EUROPE (PHOTO_ID, USER_ID, LONGITUDE, LATITUDE, TAKEN_DATE, UPLOAD_DATE, VIEWED, TITLE, SMALLURL, PLACE_ID, WOE_ID, ACCURACY) values (?, ?, ?, ?, TO_DATE(?, 'YYYY-MM-DD HH24:MI:SS'), TO_DATE(?, 'YYYY-MM-DD HH24:MI:SS'), ?, ?, ?, ?, ?, ?)");
		try {
			int i = 1;
			pstmt.setString(i++, photo.getId());
			pstmt.setString(i++, photo.getOwner().getId());
			pstmt.setDouble(i++, photo.getGeoData().getLongitude());
			pstmt.setDouble(i++, photo.getGeoData().getLatitude());

			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

			pstmt.setString(i++, formatter.format(photo.getDateTaken()));
			pstmt.setString(i++, formatter.format(photo.getDatePosted()));
			pstmt.setInt(i++, photo.getViews());
			String title = photo.getTitle();
			if (title != null && title.length() > MAX_TITLE_LENGTH) {
				title = title.substring(0, MAX_TITLE_LENGTH);
			}
			pstmt.setString(i++, title);
			pstmt.setString(i++, photo.getSmallUrl());
			pstmt.setString(i++, photo.getPlaceId());
			pstmt.setString(i++, photo.getWoeId());
			pstmt.setInt(i++, photo.getGeoData().getAccuracy());
			pstmt.executeUpdate();

			System.out.println("numPhoto:" + increaseNumPhoto());
		} finally {
			db.close(pstmt);
		}
	}

	private void updatePeoplesInfo(Connection conn, String userId, Date lastUploadDate, Date lastTakenDate) throws SQLException {
		PreparedStatement pstmt = db.getPstmt(conn, "update FLICKR_PEOPLE t set t.CHECKED_PHOTO_UPDATE = 1, t.LAST_UPLOAD_DATE = TO_DATE(?, 'YYYY-MM-DD HH24:MI:SS'), t.LAST_TAKEN_DATE = TO_DATE(?, 'YYYY-MM-DD HH24:MI:SS') where USER_ID = ?");
		try {

			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

			int i = 1;
			pstmt.setString(i++, formatter.format(lastUploadDate));
			pstmt.setString(i++, formatter.format(lastTakenDate));
			pstmt.setString(i++, userId);
			pstmt.executeUpdate();

		} finally {
			db.close(pstmt);
		}
	}

	private void updatePeoplesInfo(Connection conn, String userId) throws SQLException {
		PreparedStatement pstmt = db.getPstmt(conn, "update FLICKR_PEOPLE t set t.CHECKED_PHOTO_UPDATE = 1 where USER_ID = ?");
		try {
			System.out.println("update user: " + userId);
			pstmt.setString(1, userId);
			pstmt.executeUpdate();

		} finally {
			db.close(pstmt);
		}
	}

	public static void main(String[] args) {

		beginDateLimit = Calendar.getInstance();
		beginDateLimit.set(2005, 00, 01);

		for (int i = 0; i < NUM_THREAD; i++) {
			PublicPhotoMultiCrawler crawler = new PublicPhotoMultiCrawler();
			crawler.start();
			crawler.setName(String.valueOf(i));
		}

	}
}
