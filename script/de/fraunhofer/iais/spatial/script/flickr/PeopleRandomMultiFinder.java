package de.fraunhofer.iais.spatial.script.flickr;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.math.NumberUtils;
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
import com.aetrion.flickr.photos.Photo;
import com.aetrion.flickr.photos.PhotoList;

import de.fraunhofer.iais.spatial.util.DBUtil;

public class PeopleRandomMultiFinder extends Thread {
	/**
	* Logger for this class
	*/
	private static final Logger logger = LoggerFactory.getLogger(PeopleRandomMultiFinder.class);

	static final int pageSize = 100;
	static int NUM_THREAD = 0;
	static final int MAX_NUM_RETRY = 5000;
	static final int MAX_USERNAME_LENGTH = 200;

	static int numReTry = 0;
	static long numPeople = 0;
	static long numPhoto = 0;
	static long numTotalQuery = 0;

//	static DBUtil oracleDb = new DBUtil("/jdbc.properties", 7, 6);
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


	@Override
	public void run() {

		Date startDate = new Date();
		long start = System.currentTimeMillis();

		try {
			sleep(100);
		} catch (InterruptedException e) {
			logger.error("main(String[])", e); //$NON-NLS-1$
		}

		while (getNumReTry() <= MAX_NUM_RETRY) {

			try {
				PeopleInterface peopleInterface = init(this.getName());
				retrievePeoplesPhotos(peopleInterface);
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
		properties.load(PeopleRandomMultiFinder.class.getResourceAsStream("/flickr.properties"));
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


	private void retrievePeoplesPhotos(PeopleInterface peopleInterface) throws IOException, SQLException {

		Set<String> extras = new HashSet<String>();
		extras.add(Extras.OWNER_NAME);


		while (true) {
			Map<String, String> users = new HashMap<String, String>();
			PhotoList photos = null;
			int pages = 0;
			int page = 1;
			int total = 0;
			int num = 0;

//			double lat = RandomUtils.nextDouble() * 180 - 90;
//			double lon = RandomUtils.nextDouble() * 360 - 180;
			double lat = -0.1;
			double lon = -0.1;
			float radius = 32;

			try {
				do {
					photos = peopleInterface.searchRandomLocationPhotos(lat, lon, radius, extras, pageSize, page++);

					total = photos.getTotal();
					pages = photos.getPages();
					increaseNumTotalQuery();

					for (int i = 0; i < photos.size(); i++) {
						Photo p = (Photo) photos.get(i);
						if (!users.containsKey(p.getOwner().getId())) {
							users.put(p.getOwner().getId(), p.getOwner().getUsername());
						}

					}
					num += photos.size();

					System.out.println("total:" + total + " | num:" + num);
					System.out.println("numTotalQuery:" + getNumTotalQuery());
				} while (page <= pages);

				if(users.size()>0){
					insertPeoples(users);
				}

			} catch (SAXException e) {
				logger.error("lat:" + lat + "|lon:" + lon + "|page:" + page, e);
			} catch (FlickrException e) {
				logger.error("lat:" + lat + "|lon:" + lon + "|page:" + page, e);
			}
		}
	}

	private void insertPeoples(Map<String, String> users) throws SQLException {
		Connection conn = db.getConn();

		PreparedStatement selectPstmt = db.getPstmt(conn, "select USER_ID from FLICKR_PEOPLE t where t.USER_ID = ?");
		PreparedStatement insertPstmt = db.getPstmt(conn, "insert into FLICKR_PEOPLE (USER_ID, USERNAME) values (?, ?)");
		PreparedStatement updatePeopleContactCheckedNumPstmt = db.getPstmt(conn, "update flickr_statistic_items set value = value + 1 where name = 'people_random_location_num'");
		ResultSet rs = null;
		try {
			for (Map.Entry<String, String> entry : users.entrySet()) {
				selectPstmt.setString(1, entry.getKey());
				rs = db.getRs(selectPstmt);

				if (rs.next()) {
					System.out.println(entry.getKey() + " already existed");
				} else {
					String username = entry.getValue();
					if (username != null && username.length() > MAX_USERNAME_LENGTH) {
						username = username.substring(0, MAX_USERNAME_LENGTH);
					}
					insertPstmt.setString(1, entry.getKey());
					insertPstmt.setString(2, username);
					insertPstmt.addBatch();
					updatePeopleContactCheckedNumPstmt.executeUpdate();
					System.out.println(entry.getKey() + " inserted");
				}
			}
			insertPstmt.executeBatch();

		} finally {
			db.close(rs);
			db.close(insertPstmt);
			db.close(updatePeopleContactCheckedNumPstmt);
			db.close(selectPstmt);
			db.close(conn);
		}
	}



	public static void main(String[] args) throws IOException {

		Properties properties = new Properties();
		properties.load(PeopleMultiCrawler.class.getResourceAsStream("/flickr.properties"));
		NUM_THREAD = NumberUtils.toInt(properties.getProperty("threads_num"), 1);

		for (int i = 0; i < NUM_THREAD; i++) {
			PeopleRandomMultiFinder crawler = new PeopleRandomMultiFinder();
			crawler.start();
			crawler.setName(String.valueOf(i));
		}

	}
}
