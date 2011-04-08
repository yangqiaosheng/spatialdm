package de.fraunhofer.iais.flickr.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLInvalidAuthorizationSpecException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
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
import com.aetrion.flickr.contacts.Contact;
import com.aetrion.flickr.contacts.ContactsInterface;
import com.aetrion.flickr.groups.members.Member;
import com.aetrion.flickr.groups.members.MembersList;
import com.aetrion.flickr.people.PeopleInterface;
import com.aetrion.flickr.photos.Extras;
import com.aetrion.flickr.photos.GeoData;
import com.aetrion.flickr.photos.Photo;
import com.aetrion.flickr.photos.PhotoList;

import de.fraunhofer.iais.spatial.util.DBUtil;

public class PeopleMultiCrawler extends Thread {

	/**
	* Logger for this class
	*/
	private static final Logger logger = LoggerFactory.getLogger(PeopleMultiCrawler.class);

	static final int pageSize = 1000;
	static final int NUM_THREAD = 1;
	static final int MAX_NUM_RETRY = 500;
	static final int MAX_USERNAME_LENGTH = 200;

	static Calendar beginDateLimit = Calendar.getInstance();
	static int numReTry = 0;
	static long numInsertedPeople = 0;
	static long numCheckedPeople = 0;
	static long numTotalQuery = 0;

//	static DBUtil db = new DBUtil("/jdbc.properties", 6, 6);
	static DBUtil db = new DBUtil("/jdbc_pg.properties", 18, 6);

	boolean finished = false;

	public synchronized static int increaseNumReTry() {
		return numReTry++;
	}

	public synchronized static long increaseNumCheckedPeople() {
		return numCheckedPeople++;
	}

	public synchronized static long increaseNumInsertedPeople() {
		return numInsertedPeople++;
	}

	public synchronized static long increaseNumTotalQuery() {
		return numTotalQuery++;
	}

	public synchronized static int getNumReTry() {
		return numReTry;
	}

	public synchronized static long getNumCheckedPeople() {
		return numCheckedPeople;
	}

	public synchronized static long getNumInsertedPeople() {
		return numInsertedPeople;
	}

	public synchronized static long getNumTotalQuery() {
		return numTotalQuery;
	}

	@Override
	public void run() {

		Date startDate = new Date();
		long start = System.currentTimeMillis();
		int threadId = Integer.parseInt(this.getName());

		while (getNumReTry() <= MAX_NUM_RETRY) {

			try {
				ContactsInterface contactsInterface = this.init(this.getName());
				this.selectPeople(threadId, contactsInterface);
			} catch (Exception e) {
				logger.error("main(String[])", e); //$NON-NLS-1$
			} finally {
				Date endDate = new Date();
				long end = System.currentTimeMillis();
				logger.info("main(String[]) - ThreadId:" + this.getName()); //$NON-NLS-1$
				logger.info("main(String[]) - cost time:" + (end - start) + "ms"); //$NON-NLS-1$
				logger.info("main(String[]) - start date:" + startDate); //$NON-NLS-1$
				logger.info("main(String[]) - end date:" + endDate); //$NON-NLS-1$
				logger.info("main(String[]) - numCheckedPeople:" + numCheckedPeople + " |numInsertedPeople:" + getNumInsertedPeople()); //$NON-NLS-1$
				logger.info("main(String[]) - numTotalQuery:" + getNumTotalQuery()); //$NON-NLS-1$
				logger.info("main(String[]) - numReTry:" + getNumReTry()); //$NON-NLS-1$
			}

			if (finished) {
				// process finished and exit
				System.out.println("process finished and exit");
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

	private ContactsInterface init(String ThreadId) throws IOException, ParserConfigurationException {

		Properties properties = new Properties();
		properties.load(PeopleMultiCrawler.class.getResourceAsStream("/flickr.properties"));
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
		return flickr.getContactsInterface();
	}

	private void selectPeople(int threadId, ContactsInterface contactsInterface) throws IOException, SAXException, FlickrException, SQLException {
		Connection conn = db.getConn();
//		PreparedStatement pstmt = db.getPstmt(conn, "select USER_ID from FLICKR_PEOPLE t where t.CONTACT_UPDATE_CHECKED = 0");
//		PreparedStatement pstmt = db.getPstmt(conn, "select USER_ID from FLICKR_PEOPLE t where t.CONTACT_UPDATE_CHECKED = 0 and abs(mod(ora_hash(USER_ID), ?)) = ?");
		PreparedStatement pstmt = db.getPstmt(conn, "select USER_ID from FLICKR_PEOPLE t where t.CONTACT_UPDATE_CHECKED = 0 and abs(mod(hashtext(USER_ID), ?)) = ?");
		pstmt.setInt(1, NUM_THREAD);
		pstmt.setInt(2, threadId);
		ResultSet rs = null;
		try {
			rs = db.getRs(pstmt);
			while (rs.next()) {

				String userId = rs.getString("USER_ID");

				// assign the task to different thread
//				if (Math.abs(userId.hashCode() % NUM_THREAD) == threadId) {
					this.retrievePeopleContacts(contactsInterface, userId);
					System.out.println("thread_id:" + threadId + "user_id=" + userId + " |numCheckedPeople:" + getNumCheckedPeople() + " |numInsertedPeople:" + increaseNumInsertedPeople());
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

	private void retrievePeopleContacts(ContactsInterface contactsInterface, String userId) throws IOException, SAXException, FlickrException {
		MembersList memberslist;
		int pages = 0;
		int page = 1;
		int total = 0;
		int num = 0;

		System.out.println(userId);
		do {
			memberslist = contactsInterface.getPublicList(userId, pageSize, page++);
			total = memberslist.getTotal();
			pages = memberslist.getPages();
			increaseNumTotalQuery();
			if (memberslist.size() > 0) {
				this.insertPeoples(userId, memberslist);
			}

			num += memberslist.size();

			System.out.println("owner_id:" + userId);
			System.out.println("total:" + total + " | num:" + memberslist.size() + " | sumNum:" + num);
			System.out.println("numTotalQuery:" + getNumTotalQuery());
		} while (page <= pages);
	}

	private void insertPeoples(String userId, MembersList memberslist) {
		Connection conn = db.getConn();
		PreparedStatement updatePstmt = db.getPstmt(conn, "update FLICKR_PEOPLE t set CONTACT_UPDATE_CHECKED = 1 where t.USER_ID = ?");

		try {
			conn.setAutoCommit(false);
			PreparedStatement selectPstmt = null;
			PreparedStatement insertPstmt = null;
			selectPstmt = db.getPstmt(conn, "select USER_ID from FLICKR_PEOPLE t where t.USER_ID = ?");
			insertPstmt = db.getPstmt(conn, "insert into FLICKR_PEOPLE (USER_ID, USERNAME) values (?, ?)");
			ResultSet rs = null;
			try {
				for (int i = 0; i < memberslist.size(); i++) {
					Member m = (Member) memberslist.get(i);
					selectPstmt.setString(1, m.getId());
					rs = db.getRs(selectPstmt);

					if (rs.next()) {
						System.out.println(userId + " already existed");
					} else {
						String username = m.getUserName();
						if (username != null && username.length() > MAX_USERNAME_LENGTH) {
							username = username.substring(0, MAX_USERNAME_LENGTH);
						}

						insertPstmt.setString(1, m.getId());
						insertPstmt.setString(2, username);
						insertPstmt.addBatch();
						increaseNumInsertedPeople();
						System.out.println(userId + " inserted");
					}
				}
				insertPstmt.executeBatch();
			} finally {
				db.close(rs);
				db.close(insertPstmt);
				db.close(selectPstmt);
			}

			updatePstmt.setString(1, userId);
			updatePstmt.executeUpdate();
			increaseNumCheckedPeople();

			conn.commit();

		} catch (SQLException e) {
			logger.error("insertPeople()", e); //$NON-NLS-1$
			try {
				conn.rollback();
			} catch (SQLException e1) {
				logger.error("insertPeople()", e); //$NON-NLS-1$
			}
		} finally {
			db.close(updatePstmt);
			db.close(conn);
		}
	}

	public static void main(String[] args) {

		beginDateLimit.set(2005, 00, 01);
		for (int i = 0; i < NUM_THREAD; i++) {
			PeopleMultiCrawler crawler = new PeopleMultiCrawler();
			crawler.start();
			crawler.setName(String.valueOf(i));
		}

	}
}
