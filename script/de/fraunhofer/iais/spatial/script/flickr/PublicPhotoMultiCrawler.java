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
import com.aetrion.flickr.tags.Tag;

import de.fraunhofer.iais.spatial.util.DBUtil;

public class PublicPhotoMultiCrawler extends Thread {
	private static final int PG_FETCH_SIZE = 2000;

	/**
	* Logger for this class
	*/
	private static final Logger logger = LoggerFactory.getLogger(PublicPhotoMultiCrawler.class);

	static final int pageSize = 500;
	static int NUM_THREAD = 0;
	static final int MAX_NUM_RETRY = 5000;
	static final int MAX_TITLE_LENGTH = 1024;

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

	static DBUtil db = new DBUtil("/jdbc_pg.properties", 18, 6);

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
		Connection conn = db.getConn();
//		PreparedStatement pstmt = oracleDb.getPstmt(conn, "select USER_ID, PHOTO_UPDATE_CHECKED_DATE from FLICKR_PEOPLE t where t.PHOTO_UPDATE_CHECKED = 0");
//		PreparedStatement pstmt = oracleDb.getPstmt(conn, "select USER_ID, PHOTO_UPDATE_CHECKED_DATE from FLICKR_PEOPLE t where t.PHOTO_UPDATE_CHECKED = 0 and abs(mod(ora_hash(USER_ID), ?)) = ?");
		PreparedStatement pstmt = db.getPstmt(conn, "select USER_ID, PHOTO_UPDATE_CHECKED_DATE from FLICKR_PEOPLE t where t.PHOTO_UPDATE_CHECKED = 0 and abs(mod(hashtext(USER_ID), ?)) = ?");

		conn.setAutoCommit(false);
		pstmt.setFetchSize(PG_FETCH_SIZE);

		pstmt.setInt(1, NUM_THREAD);
		pstmt.setInt(2, threadId);
		ResultSet rs = null;
		try {
			rs = db.getRs(pstmt);
			while (rs.next()) {

				String userId = rs.getString("USER_ID");
				Date lastUploadDate = rs.getTimestamp("PHOTO_UPDATE_CHECKED_DATE");

				// assign the task to different thread
//				if (Math.abs(userId.hashCode() % NUM_THREAD) == threadId) {
					retrievePeoplesPhotos(peopleInterface, userId, lastUploadDate);
					System.out.println("numPeople:" + increaseNumPeople());
//				}
			}

			// process finished
			finished = true;
		} finally {
			db.close(rs);
			db.close(pstmt);
			db.close(conn);
		}
	}

	private void retrievePeoplesPhotos(PeopleInterface peopleInterface, String userId, Date lastUploadDate) throws IOException, SQLException {

		Set<String> extras = new HashSet<String>();
		extras.add(Extras.DATE_TAKEN);
		extras.add(Extras.DATE_UPLOAD);
		extras.add(Extras.GEO);
		extras.add(Extras.VIEWS);
		extras.add(Extras.TAGS);

		PhotoList photos = null;
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

		PhotoList insertEuropePhotos = new PhotoList();
		PhotoList insertWorldPhotos = new PhotoList();

		try {
			do {
				//get all the photo with and without GEO info
			photos = peopleInterface.getPhotos(userId, minUploadDate.getTime(), maxUploadDate.getTime(), minTakenDate.getTime(), maxTakenDate.getTime(), extras, pageSize, page++);

				//there is bugs in the Search method with bbox option, which will only return the result of accuracy=16
//			    photos = peopleInterface.getSearchWithGeoPhoto(userId, minUploadDate.getTime(), maxUploadDate.getTime(), minTakenDate.getTime(), maxTakenDate.getTime(), peopleInterface.new Bbox(MIN_LONGITUDE, MIN_LATITUDE, MAX_LONGITUDE, MAX_LATITUDE), extras, pageSize, page++);
//				photos = peopleInterface.searchWithGeoPhotos(userId, minUploadDate.getTime(), maxUploadDate.getTime(), minTakenDate.getTime(), maxTakenDate.getTime(), null, extras, pageSize, page++);

				total = photos.getTotal();
				pages = photos.getPages();
				increaseNumTotalQuery();

				for (int i = 0; i < photos.size(); i++) {
					Photo p = (Photo) photos.get(i);

					if (checkDate(p.getDateTaken(), p.getDatePosted()) && p.getGeoData() != null) {
						insertWorldPhotos.add(p);
					}

					if (checkDate(p.getDateTaken(), p.getDatePosted()) && checkLocation(p.getGeoData())) {
						insertEuropePhotos.add(p);
					}
				}
				num += photos.size();

				System.out.println("owner_id:" + userId);
				System.out.println("total:" + total + " | num:" + num);
				System.out.println("numTotalQuery:" + getNumTotalQuery());
			} while (page <= pages);

			if (insertPhotos(insertEuropePhotos, insertWorldPhotos, userId)) {

			}

		} catch (SAXException e) {
			flickrExceptionHandler(userId, page, e, -1);
		} catch (FlickrException e) {
			flickrExceptionHandler(userId, page, e, -2);
		}
	}

	private void flickrExceptionHandler(String userId, int page, Exception e, int photoCheckedFlag) throws SQLException {
		logger.error("userID:" + userId + "|page:" + page, e);
		logger.error("retrievePeoplesPhotos()", e); //$NON-NLS-1$
		Connection conn = db.getConn();
		try {
			updatePeoplesInfo(conn, userId, photoCheckedFlag, 0, 0);
		} finally {
			db.close(conn);
		}
	}

	private boolean insertPhotos(PhotoList europePhotos, PhotoList worldPhotos, String userId) {
		boolean success = true;
		Connection conn = db.getConn();

		try {
			conn.setAutoCommit(false);
			conn.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);

			int addEuorpeNum = 0;
			int addWorldNum = 0;

			//remove duplicate insertPhotos
			HashSet<String> europePhotosId = new HashSet<String>();
			HashSet<String> worldPhotosId = new HashSet<String>();

			for (Photo photo : europePhotos) {
				if (!europePhotosId.contains(photo.getId())) {
					insertPhoto(conn, photo, "FLICKR_EUROPE");
//					updatePhotoRegionInfo(conn, photo, radiusList);
					europePhotosId.add(photo.getId());
					addEuorpeNum++;
				}else{
					logger.warn("Duplicate Photo to Euorpe:" + photo.toString());
				}
			}

			for (Photo photo : worldPhotos) {
				if (!worldPhotosId.contains(photo.getId())) {
					insertPhoto(conn, photo, "FLICKR_WORLD");
					worldPhotosId.add(photo.getId());
					addWorldNum++;
				}else{
					logger.warn("Duplicate Photo to World:" + photo.toString());
				}
			}

			updatePeoplesInfo(conn, userId, 1, addWorldNum, addEuorpeNum);

			conn.commit();
		} catch (SQLException e) {
			logger.error("User:" + userId + "|size:" + europePhotos.size());
			logger.error("insertPhotosToEurope()", e); //$NON-NLS-1$
			try {
				success = false;
				conn.rollback();
			} catch (SQLException e1) {
				logger.error("insertPhotosToEurope()", e); //$NON-NLS-1$
			}
		} finally {
			db.close(conn);
		}
		return success;
	}

	private void insertPhoto(Connection conn, Photo photo, String tableName) throws SQLException {
		PreparedStatement pstmt = db
				.getPstmt(
						conn,
						"insert into " + tableName + " (PHOTO_ID, USER_ID, LONGITUDE, LATITUDE, TAKEN_DATE, UPLOAD_DATE, VIEWED, TITLE, TAGS, SMALLURL, PLACE_ID, WOE_ID, ACCURACY) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
		try {
			int i = 1;
			pstmt.setLong(i++, NumberUtils.toLong(photo.getId()));
			pstmt.setString(i++, photo.getOwner().getId());
			pstmt.setDouble(i++, photo.getGeoData().getLongitude());
			pstmt.setDouble(i++, photo.getGeoData().getLatitude());

			pstmt.setTimestamp(i++, new Timestamp(photo.getDateTaken().getTime()));
			pstmt.setTimestamp(i++, new Timestamp(photo.getDatePosted().getTime()));

			pstmt.setInt(i++, photo.getViews());
			String title = StringUtils.substring(photo.getTitle(), 0, MAX_TITLE_LENGTH);
			Collection<Tag> tags = photo.getTags();
			String tagsStr = "";
			for (Tag tag : tags) {
				tagsStr += tag.getValue() + ",";
			}
			pstmt.setString(i++, title);
			pstmt.setString(i++, tagsStr);
			pstmt.setString(i++, photo.getSmallUrl());
			pstmt.setString(i++, photo.getPlaceId());
			pstmt.setString(i++, photo.getWoeId());
			pstmt.setInt(i++, photo.getGeoData().getAccuracy());
			pstmt.executeUpdate();
			System.out.println("numPhoto:" + increaseNumPhoto());
		} catch (PSQLException e){
			logger.debug("Wrong input Photo to PostgreSQL:" + photo.toString());
			logger.debug("insertPhoto()", e); //$NON-NLS-1$
		} catch (SQLException e){
			logger.error("Wrong input Photo:" + photo.toString());
			throw e;
		} finally {
			db.close(pstmt);
		}
	}

	private void updatePhotoRegionInfo(Connection conn, Photo photo, Collection<String> radiuses) throws SQLException {

		double x = photo.getGeoData().getLongitude();
		double y = photo.getGeoData().getLatitude();

		for (String radius : radiuses) {
			PreparedStatement selectPstmt = null;
			ResultSet selectRs = null;
			try {
//				selectPstmt = oracleDb.getPstmt(conn, "select ID from FLICKR_EUROPE_AREA_" + radius + " c, user_sdo_geom_metadata m" + " WHERE m.table_name = 'FLICKR_EUROPE_AREA_" + radius
//						+ "' and sdo_relate(c.geom, SDO_geometry(2001,8307,SDO_POINT_TYPE(?, ?, NULL),NULL,NULL),'mask=anyinteract') = 'TRUE'");
				selectPstmt = db.getPstmt(conn, "select ID from FLICKR_EUROPE_AREA_" + radius + " t where ST_Intersects(ST_GeomFromEWKT('SRID=4326;POINT("+x+" "+y+")'), t.geom::geometry)");
//				selectPstmt.setDouble(1, x);
//				selectPstmt.setDouble(2, y);
				selectRs = db.getRs(selectPstmt);
				if (selectRs.next()) {
					PreparedStatement updateRegionPstmt = null;
					try {
						updateRegionPstmt = db.getPstmt(conn, "update FLICKR_EUROPE set REGION_" + radius + "_ID = ? where PHOTO_ID = ?");
						updateRegionPstmt.setInt(1, Integer.parseInt(selectRs.getString("ID")));
						updateRegionPstmt.setLong(2, Long.parseLong(photo.getId()));
						updateRegionPstmt.executeUpdate();
					} finally {
						db.close(updateRegionPstmt);
					}
				}
			} finally {
				db.close(selectPstmt);
				db.close(selectRs);
			}
		}

		PreparedStatement updateCheckedPstmt = null;
		try {
			updateCheckedPstmt = db.getPstmt(conn, "update FLICKR_EUROPE set REGION_CHECKED = 1 where PHOTO_ID = ?");
			updateCheckedPstmt.setLong(1, Long.parseLong(photo.getId()));
			updateCheckedPstmt.executeUpdate();
		} finally {
			db.close(updateCheckedPstmt);
		}
	}

	private void updatePeoplesInfo(Connection conn, String userId, int photoCheckedFlag, int addWorldNum, int addEuropeNum) throws SQLException {
		PreparedStatement pstmt = db.getPstmt(conn, "update FLICKR_PEOPLE set PHOTO_UPDATE_CHECKED = ?, PHOTO_UPDATE_CHECKED_DATE = ?, WORLD_NUM = WORLD_NUM + ?, EUROPE_NUM = EUROPE_NUM + ? where USER_ID = ?");
		PreparedStatement updatePeoplePhotoCheckedNumPstmt = db.getPstmt(conn, "update flickr_statistic_items set value = value + 1 where name = 'people_photo_checked_num'");
		try {

			int i = 1;
			pstmt.setInt(i++, photoCheckedFlag);
			pstmt.setTimestamp(i++, new Timestamp(new Date().getTime()));
			pstmt.setInt(i++, addWorldNum);
			pstmt.setInt(i++, addEuropeNum);
			pstmt.setString(i++, userId);
			System.out.println("user_id:" + userId);
			pstmt.executeUpdate();
			updatePeoplePhotoCheckedNumPstmt.executeUpdate();
		} finally {
			db.close(updatePeoplePhotoCheckedNumPstmt);
			db.close(pstmt);
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
			PublicPhotoMultiCrawler crawler = new PublicPhotoMultiCrawler();
			crawler.start();
			crawler.setName(String.valueOf(i));
		}

	}
}
