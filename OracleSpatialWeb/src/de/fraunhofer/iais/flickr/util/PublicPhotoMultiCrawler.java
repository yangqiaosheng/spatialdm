package de.fraunhofer.iais.flickr.util;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang.math.NumberUtils;
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
	static Collection<String> radiusList;
	static int numReTry = 0;
	static long numPeople = 0;
	static long numPhoto = 0;
	static long numTotalQuery = 0;

	static DBUtil oracleDb = new DBUtil("/jdbc.properties", 18, 6);
	static DBUtil pgDb = new DBUtil("/jdbc_pg.properties", 18, 6);

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

	public boolean checkDate(Date takenDate, Date uploadDate) {
		if (takenDate != null && uploadDate != null && !takenDate.before(beginDateLimit.getTime()) && !takenDate.after(new Date()) && uploadDate.after(takenDate)) {
			return true;
		} else {
			return false;
		}
	}

	public boolean checkLocation(GeoData geoData) {

		if (geoData != null && geoData.getLongitude() > MIN_LONGITUDE && geoData.getLongitude() < MAX_LONGITUDE && geoData.getLatitude() > MIN_LATITUDE && geoData.getLatitude() < MAX_LATITUDE) {
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
		Connection conn = oracleDb.getConn();
		PreparedStatement pstmt = oracleDb.getPstmt(conn, "select USER_ID, LAST_UPLOAD_DATE from FLICKR_PEOPLE t where t.PHOTO_UPDATE_CHECKED = 0");

		ResultSet rs = null;
		try {
			rs = oracleDb.getRs(pstmt);
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
			oracleDb.close(rs);
			oracleDb.close(pstmt);
			oracleDb.close(conn);
		}
	}

	private void retrievePeoplesPhotos(PeopleInterface peopleInterface, String userId, Date lastUploadDate) throws IOException, SAXException, FlickrException  {

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
		Connection pgConn = pgDb.getConn();
		try {
			do {
				//get all the photo with and without GEO info
//			photos = peopleInterface.getPhotos(userId, minUploadDate.getTime(), maxUploadDate.getTime(), minTakenDate.getTime(), maxTakenDate.getTime(), extras, pageSize, page++);

				//there is bugs in the Search method with bbox option, which will only return the result of accuracy=16
//			photos = peopleInterface.getSearchWithGeoPhoto(userId, minUploadDate.getTime(), maxUploadDate.getTime(), minTakenDate.getTime(), maxTakenDate.getTime(), peopleInterface.new Bbox(MIN_LONGITUDE, MIN_LATITUDE, MAX_LONGITUDE, MAX_LATITUDE), extras, pageSize, page++);

				photos = peopleInterface.getSearchWithGeoPhoto(userId, minUploadDate.getTime(), maxUploadDate.getTime(), minTakenDate.getTime(), maxTakenDate.getTime(), null, extras, pageSize, page++);
				total = photos.getTotal();
				pages = photos.getPages();
				increaseNumTotalQuery();

				for (int i = 0; i < photos.size(); i++) {
					Photo p = (Photo) photos.get(i);

					if (checkDate(p.getDateTaken(), p.getDatePosted()) && p.getGeoData() != null) {
						try {
							insertPhoto(pgConn, p, "FLICKR_PHOTO");
						} catch (SQLException e) {
							logger.error("insertPhotosToPostgreSQL", e); //$NON-NLS-1$
						}
					}

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
		} finally {
			pgDb.close(pgConn);
		}
	}

	private void insertPhotos(PhotoList photos, String userId) {
		Connection conn = oracleDb.getConn();


		TreeSet<Date> uploadDates = new TreeSet<Date>();
		TreeSet<Date> takenDates = new TreeSet<Date>();
		try {
			conn.setAutoCommit(false);

			//remove duplicate insertPhotos
			HashSet<String> insertedPhotosId = new HashSet<String>();

			for (Photo photo : photos) {
				if (!insertedPhotosId.contains(photo.getId())) {
					insertPhoto(conn, photo, "FLICKR_EUROPE");
					updatePhotoRegionInfo(conn, photo, radiusList);
					uploadDates.add(photo.getDatePosted());
					takenDates.add(photo.getDateTaken());
					insertedPhotosId.add(photo.getId());
				}else{
					logger.error("Duplicate Photo:" + photo.toString());
				}
			}

			if (photos.size() > 0) {
				updatePeoplesInfo(conn, userId, uploadDates.last(), takenDates.last());
			} else {
				updatePeoplesInfo(conn, userId);
			}

			conn.commit();
		} catch (SQLException e) {
			logger.error("User:" + userId + "|size:" + photos.size());
			logger.error("insertPhotos()", e); //$NON-NLS-1$
			try {
				conn.rollback();
			} catch (SQLException e1) {
				logger.error("insertPhotos()", e); //$NON-NLS-1$
			}
		} finally {
			oracleDb.close(conn);
		}
	}

	private void insertPhoto(Connection conn, Photo photo, String tableName) throws SQLException {
		PreparedStatement pstmt = oracleDb
				.getPstmt(
						conn,
						"insert into " + tableName + " (PHOTO_ID, USER_ID, LONGITUDE, LATITUDE, TAKEN_DATE, UPLOAD_DATE, VIEWED, TITLE, SMALLURL, PLACE_ID, WOE_ID, ACCURACY) values (?, ?, ?, ?, TO_DATE(?, 'YYYY-MM-DD HH24:MI:SS'), TO_DATE(?, 'YYYY-MM-DD HH24:MI:SS'), ?, ?, ?, ?, ?, ?)");
		try {
			int i = 1;
			pstmt.setLong(i++, NumberUtils.toLong(photo.getId()));
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
		} catch (PSQLException e){
			logger.debug("Wrong input Photo to PostgreSQL:" + photo.toString());
		} catch (SQLException e){
			logger.error("Wrong input Photo:" + photo.toString());
			throw e;
		} finally {
			oracleDb.close(pstmt);
		}
	}

	private void updatePhotoRegionInfo(Connection conn, Photo photo, Collection<String> radiuses) throws SQLException {

		double x = photo.getGeoData().getLongitude();
		double y = photo.getGeoData().getLatitude();

		for (String radius : radiuses) {
			PreparedStatement selectPstmt = null;
			ResultSet selectRs = null;
			try {
				selectPstmt = oracleDb.getPstmt(conn, "select ID from FLICKR_EUROPE_AREA_" + radius + " c, user_sdo_geom_metadata m" + " WHERE m.table_name = 'FLICKR_EUROPE_AREA_" + radius
						+ "' and sdo_relate(c.geom, SDO_geometry(2001,8307,SDO_POINT_TYPE(?, ?, NULL),NULL,NULL),'mask=anyinteract') = 'TRUE'");
				selectPstmt.setDouble(1, x);
				selectPstmt.setDouble(2, y);
				selectRs = oracleDb.getRs(selectPstmt);
				if (selectRs.next()) {
					PreparedStatement updateRegionPstmt = null;
					try {
						updateRegionPstmt = oracleDb.getPstmt(conn, "update FLICKR_EUROPE p set p.REGION_" + radius + "_ID = ? where p.PHOTO_ID = ?");
						updateRegionPstmt.setString(1, selectRs.getString("ID"));
						updateRegionPstmt.setString(2, photo.getId());
						updateRegionPstmt.executeUpdate();
					} finally {
						oracleDb.close(updateRegionPstmt);
					}
				}
			} finally {
				oracleDb.close(selectPstmt);
				oracleDb.close(selectRs);
			}
		}

		PreparedStatement updateCheckedPstmt = null;
		try {
			updateCheckedPstmt = oracleDb.getPstmt(conn, "update FLICKR_EUROPE p set p.REGION_CHECKED = 1 where p.PHOTO_ID = ?");
			updateCheckedPstmt.setString(1, photo.getId());
			updateCheckedPstmt.executeUpdate();
		} finally {
			oracleDb.close(updateCheckedPstmt);
		}
	}

	private void updatePeoplesInfo(Connection conn, String userId, Date lastUploadDate, Date lastTakenDate) throws SQLException {
		PreparedStatement pstmt = oracleDb.getPstmt(conn, "update FLICKR_PEOPLE t set t.PHOTO_UPDATE_CHECKED = 1, t.LAST_UPLOAD_DATE = TO_DATE(?, 'YYYY-MM-DD HH24:MI:SS'), t.LAST_TAKEN_DATE = TO_DATE(?, 'YYYY-MM-DD HH24:MI:SS') where USER_ID = ?");
		try {

			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

			int i = 1;
			pstmt.setString(i++, formatter.format(lastUploadDate));
			pstmt.setString(i++, formatter.format(lastTakenDate));
			pstmt.setString(i++, userId);
			pstmt.executeUpdate();

		} finally {
			oracleDb.close(pstmt);
		}
	}

	private void updatePeoplesInfo(Connection conn, String userId) throws SQLException {
		PreparedStatement pstmt = oracleDb.getPstmt(conn, "update FLICKR_PEOPLE t set t.PHOTO_UPDATE_CHECKED = 1 where USER_ID = ?");
		try {
			System.out.println("update user: " + userId);
			pstmt.setString(1, userId);
			pstmt.executeUpdate();

		} finally {
			oracleDb.close(pstmt);
		}
	}

	public static void main(String[] args) {

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

		for (int i = 0; i < NUM_THREAD; i++) {
			PublicPhotoMultiCrawler crawler = new PublicPhotoMultiCrawler();
			crawler.start();
			crawler.setName(String.valueOf(i));
		}

	}
}
