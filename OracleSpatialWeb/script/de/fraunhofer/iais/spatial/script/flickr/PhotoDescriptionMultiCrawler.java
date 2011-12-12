package de.fraunhofer.iais.spatial.script.flickr;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.postgresql.util.PSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.aetrion.flickr.photos.PhotosInterface;
import com.aetrion.flickr.tags.Tag;

import de.fraunhofer.iais.spatial.util.DBUtil;

public class PhotoDescriptionMultiCrawler extends Thread {
	private static final int PG_FETCH_SIZE = 2000;

	/**
	* Logger for this class
	*/
	private static final Logger logger = LoggerFactory.getLogger(PhotoDescriptionMultiCrawler.class);

	static int NUM_THREAD = 0;
	static int  MAX_NUM_RETRY = 100;
	static final String TABLE_NAME = "flickr_world_region_id_15175_r40000_10000";


	static Calendar beginDateLimit;
	static Collection<String> radiusList;
	static int numReTry = 0;
	static long numPhoto = 0;
	static long numTotalQuery = 0;

	static DBUtil db = new DBUtil("/jdbc_pg.properties", 18, 6);

	boolean finished = false;

	public synchronized static int increaseNumReTry() {
		return ++numReTry;
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

		try {
			sleep(100);
		} catch (InterruptedException e) {
			logger.error("main(String[])", e); //$NON-NLS-1$
		}

		int threadId = Integer.parseInt(this.getName());

		while (getNumReTry() <= MAX_NUM_RETRY) {

			try {
				PhotosInterface photosInterface = init(this.getName());
				selectPhoto(threadId, photosInterface);
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

	private PhotosInterface init(String ThreadId) throws IOException, ParserConfigurationException {

		Properties properties = new Properties();
		properties.load(PhotoDescriptionMultiCrawler.class.getResourceAsStream("/flickr.properties"));
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
		return flickr.getPhotosInterface();
	}

	private void selectPhoto(int threadId, PhotosInterface photosInterface) throws IOException, SAXException, FlickrException, SQLException {
		Connection conn = db.getConn();
		PreparedStatement pstmt = db.getPstmt(conn, "select photo_id from " + TABLE_NAME + " t where abs(mod(hashtext(user_id), ?)) = ?");

		conn.setAutoCommit(false);
		pstmt.setFetchSize(PG_FETCH_SIZE);
		pstmt.setInt(1, NUM_THREAD);
		pstmt.setInt(2, threadId);
		ResultSet rs = db.getRs(pstmt);
		try {
			while (rs.next()) {
				Long photoId = rs.getLong("photo_id");
				addPhotosDescription(photosInterface, photoId);
				System.out.println("numPhoto:" + increaseNumPhoto());
			}

			// process finished
			finished = true;
		} finally {
			db.close(rs);
			db.close(pstmt);
			db.close(conn);
		}
	}

	private void addPhotosDescription(PhotosInterface photosInterface, Long photoId) throws IOException, SQLException {

		Set<String> extras = new HashSet<String>();
		extras.add(Extras.DESCRIPTION);

		try {
			Photo photo = photosInterface.getPhoto(String.valueOf(photoId));

			if (StringUtils.isNotBlank(photo.getDescription())) {
				System.out.println(photo.getId() + " " + photo.getTitle());
				System.out.println(photo.getId() + " " + photo.getDescription());
				updatePhotoDescription(photo);
			}

			/*
			if (checkDate(p.getDateTaken(), p.getDatePosted()) && checkLocation(p.getGeoData())) {
				insertEuropePhotos.add(p);
			}*/

		} catch (SAXException e) {
			e.printStackTrace();
		} catch (FlickrException e) {
			e.printStackTrace();
		}
	}

	private void updatePhotoDescription(Photo photo) throws SQLException {
		Connection conn = db.getConn();
		PreparedStatement updateDescPstmt = null;
		try {
			updateDescPstmt = db.getPstmt(conn, "update " + TABLE_NAME + " set description = ? where PHOTO_ID = ?");
			updateDescPstmt.setString(1, photo.getDescription());
			updateDescPstmt.setLong(2, Long.parseLong(photo.getId()));
			updateDescPstmt.executeUpdate();
		} finally {
			db.close(updateDescPstmt);
			db.close(conn);
		}

	}

	public static void main(String[] args) throws IOException {

		beginDateLimit = Calendar.getInstance();
		beginDateLimit.set(2005, 00, 01);

		radiusList = new LinkedHashSet<String>();
		radiusList.add("5000");
		radiusList.add("10000");
		radiusList.add("20000");
		radiusList.add("40000");
		radiusList.add("80000");
		radiusList.add("160000");
		radiusList.add("320000");

		Properties properties = new Properties();
		properties.load(PeopleMultiCrawler.class.getResourceAsStream("/flickr.properties"));
		NUM_THREAD = NumberUtils.toInt(properties.getProperty("threads_num"), 1);

		for (int i = 0; i < NUM_THREAD; i++) {
			PhotoDescriptionMultiCrawler crawler = new PhotoDescriptionMultiCrawler();
			crawler.start();
			crawler.setName(String.valueOf(i));
		}

	}
}
