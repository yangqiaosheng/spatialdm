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
	static final int MAX_TITLE_LENGTH = 300;

	static Calendar beginDateLimit = Calendar.getInstance();
	static int numReTry = 0;
	static long numInsertedPeople = 0;
	static long numCheckedPeople = 0;
	static long numTotalQuery = 0;

	static DBUtil db = new DBUtil();

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
		int id = Integer.parseInt(this.getName());

		/*
		System.setProperty("oraclespatialweb.root", System.getProperty("user.dir"));
		System.out.println("oraclespatialweb.root:" + System.getProperty("oraclespatialweb.root"));
		 */

//		PeopleInterface peopleInterface = null;
//		try {
//			sleep(id * 1000);
//			peopleInterface = init(this.getName());
//		} catch (Exception e) {
//			logger.error("main(String[])", e); //$NON-NLS-1$
//		}

		while (getNumReTry() <= MAX_NUM_RETRY) {

			try {
				ContactsInterface contactsInterface = init(this.getName());
				selectPeople(id, contactsInterface);
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

	private static void getPeopleContacts(ContactsInterface contactsInterface, String userid) throws IOException, SAXException, FlickrException, SQLException {
		MembersList memberslist;
		int pages = 0;
		int page = 1;
		int total = 0;
		int num = 0;

		do {
			memberslist = contactsInterface.getPublicList(userid, pageSize, page++);
			total = memberslist.getTotal();
			pages = memberslist.getPages();
			increaseNumTotalQuery();
			if (memberslist.size() > 0) {
				insertPeople(userid, memberslist);
			}

			num += memberslist.size();

			System.out.println("owner_id:" + userid);
			System.out.println("total:" + total + " | num:" + memberslist.size() + " | sumNum:" + num);
			System.out.println("numTotalQuery:" + getNumTotalQuery());
		} while (page <= pages);
	}

	private static void insertPeople(String userid, MembersList memberslist) {
		Connection conn = db.getConn();
		PreparedStatement updatePstmt = db.getPstmt(conn, "update FLICKR_PEOPLE t set t.CHECKED = 1 where t.USERID = ?");

		try {
			conn.setAutoCommit(false);
			for (int i = 0; i < memberslist.size(); i++) {
				PreparedStatement selectPstmt = null;
				PreparedStatement insertPstmt = null;
				ResultSet rs = null;
				try {
					Member m = (Member) memberslist.get(i);
					selectPstmt = db.getPstmt(conn, "select USERID from FLICKR_PEOPLE where USERID = ?");
					selectPstmt.setString(1, m.getId());
					rs = db.getRs(selectPstmt);

					if(rs.next()){
						System.out.println(userid + " already existed");
					} else {
						insertPstmt = db.getPstmt(conn, "insert into FLICKR_PEOPLE (USERID, USERNAME, CHECKED) values (?, ?, 0)");
						insertPstmt.setString(1, m.getId());
						insertPstmt.setString(2, m.getUserName());
						insertPstmt.executeUpdate();
						increaseNumInsertedPeople();
						System.out.println(userid + " inserted");
					}

				} finally {
					db.close(rs);
					db.close(insertPstmt);
					db.close(selectPstmt);
				}
			}

			updatePstmt.setString(1, userid);
			updatePstmt.executeUpdate();
			increaseNumCheckedPeople();

			conn.commit();
		}catch (SQLIntegrityConstraintViolationException e) {
			System.out.println("EEEEEEEEEEROROOROROOR");
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

	private static void selectPeople(int id, ContactsInterface contactsInterface) throws IOException, SAXException, FlickrException, SQLException {
		Connection conn = db.getConn();
		PreparedStatement pstmt = db.getPstmt(conn, "select USERID from flickr_people t where t.checked = 0");

		ResultSet rs = null;
		try {
			rs = db.getRs(pstmt);
			while (rs.next()) {

				String userid = rs.getString("USERID");

				// assign the task to different thread
				if (new Random(userid.hashCode()).nextInt(NUM_THREAD) == id) {
					getPeopleContacts(contactsInterface, userid);
					System.out.println("numInsertedPeople:" + increaseNumInsertedPeople());
				}
			}
		} finally {
			db.close(rs);
			db.close(pstmt);
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
