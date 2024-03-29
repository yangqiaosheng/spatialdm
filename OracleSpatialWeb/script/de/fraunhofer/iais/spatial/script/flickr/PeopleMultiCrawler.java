package de.fraunhofer.iais.spatial.script.flickr;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;

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
import com.aetrion.flickr.contacts.ContactsInterface;
import com.aetrion.flickr.groups.members.Member;
import com.aetrion.flickr.groups.members.MembersList;

import de.fraunhofer.iais.spatial.util.DBUtil;

public class PeopleMultiCrawler extends Thread {

	/**
	* Logger for this class
	*/
	private static final Logger logger = LoggerFactory.getLogger(PeopleMultiCrawler.class);

	static final int pageSize = 1000;
	static int NUM_THREAD = 0;
	static final int MAX_NUM_RETRY = 5000;
	static final int MAX_USERNAME_LENGTH = 200;
	static final int INIT_CONTACT_UPDATE_CHECKED = 0;
	static final int FINISH_CONTACT_UPDATE_CHECKED = 1;

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
//		PreparedStatement pstmt = db.getPstmt(conn, "select USER_ID from FLICKR_PEOPLE t where t.CONTACT_UPDATE_CHECKED = ?");
//		PreparedStatement pstmt = db.getPstmt(conn, "select USER_ID from FLICKR_PEOPLE t where t.CONTACT_UPDATE_CHECKED = ? and abs(mod(ora_hash(USER_ID), ?)) = ?");
		PreparedStatement pstmt = db.getPstmt(conn, "select USER_ID from FLICKR_PEOPLE t where t.CONTACT_UPDATE_CHECKED = ? and abs(mod(hashtext(USER_ID), ?)) = ?");
		pstmt.setInt(1, INIT_CONTACT_UPDATE_CHECKED);
		pstmt.setInt(2, NUM_THREAD);
		pstmt.setInt(3, threadId);
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

	private void retrievePeopleContacts(ContactsInterface contactsInterface, String userId) throws IOException, SAXException {
		MembersList memberslist = null;
		int pages = 0;
		int page = 1;
		int total = 0;
		int num = 0;

		System.out.println(userId);
		try {
			do {
				memberslist = contactsInterface.getPublicList(userId, pageSize, page++);
				total = memberslist.getTotal();
				pages = memberslist.getPages();
				increaseNumTotalQuery();
				this.insertPeoples(userId, memberslist);

				num += memberslist.size();

				System.out.println("owner_id:" + userId);
				System.out.println("total:" + total + " | num:" + memberslist.size() + " | sumNum:" + num);
				System.out.println("numTotalQuery:" + getNumTotalQuery());
			} while (page <= pages);
		} catch (FlickrException e) {
			logger.error("retrievePeopleContacts()", e); //$NON-NLS-1$
			logger.info("retrievePeopleContacts() - ThreadId:" + this.getName()); //$NON-NLS-1$
			logger.info("retrievePeopleContacts() - time:" + new Date()); //$NON-NLS-1$
		}
	}

	private void insertPeoples(String userId, MembersList memberslist) {
		Connection conn = db.getConn();
		PreparedStatement updatePstmt = null;
		PreparedStatement selectPstmt = null;
		PreparedStatement insertPeoplePstmt = null;
		PreparedStatement updatePeopleContactRefNumPstmt = null;
		PreparedStatement updatePeopleContactNumPstmt = null;
		PreparedStatement updateStatContactCheckedNumPstmt = null;
		PreparedStatement insertPeopleRelPstmt = null;
		ResultSet rs = null;
		
		try {
			conn.setAutoCommit(false);
			updatePstmt = db.getPstmt(conn, "update FLICKR_PEOPLE t set CONTACT_UPDATE_CHECKED = ? where t.USER_ID = ?");
			selectPstmt = db.getPstmt(conn, "select USER_ID from FLICKR_PEOPLE t where t.USER_ID = ?");
			insertPeoplePstmt = db.getPstmt(conn, "insert into FLICKR_PEOPLE (USER_ID, USERNAME, CONTACT_UPDATE_CHECKED) values (?, ?, ?)");
			updatePeopleContactRefNumPstmt = db.getPstmt(conn, "update flickr_people set contact_referenced_num = contact_referenced_num + 1 where user_id = ?");
			updatePeopleContactNumPstmt = db.getPstmt(conn, "update flickr_people set contact_num = ? where user_id = ?");
			updateStatContactCheckedNumPstmt = db.getPstmt(conn, "update flickr_statistic_items set value = value + 1 where name = 'people_contact_checked_num'");
			insertPeopleRelPstmt = db.getPstmt(conn, "insert into FLICKR_PEOPLE_REL_CONTACT (USER_ID, CONTACT_USER_ID) values (?, ?)");
			
			for (int i = 0; i < memberslist.size(); i++) {
				Member m = memberslist.get(i);
				selectPstmt.setString(1, m.getId());
				rs = db.getRs(selectPstmt);

				if (rs.next()) {
					System.out.println(m.getId() + " already existed");
					
					//update number of contact reference 
					updatePeopleContactRefNumPstmt.setString(1, m.getId());
					updatePeopleContactRefNumPstmt.addBatch();
				} else {
					String username = m.getUserName();
					if (username != null && username.length() > MAX_USERNAME_LENGTH) {
						username = username.substring(0, MAX_USERNAME_LENGTH);
					}

					insertPeoplePstmt.setString(1, m.getId());
					insertPeoplePstmt.setString(2, username);
					insertPeoplePstmt.setInt(3, INIT_CONTACT_UPDATE_CHECKED);
					insertPeoplePstmt.addBatch();
					increaseNumInsertedPeople();
					System.out.println(m.getId() + " inserted");
				}
				
				// add contact relation
				insertPeopleRelPstmt.setString(1, userId);
				insertPeopleRelPstmt.setString(2, m.getId());
				insertPeopleRelPstmt.addBatch();
			}
				
			updateStatContactCheckedNumPstmt.executeUpdate();
			insertPeoplePstmt.executeBatch();
			updatePeopleContactRefNumPstmt.executeBatch();
			insertPeopleRelPstmt.executeBatch();

			updatePeopleContactNumPstmt.setInt(1, memberslist.size());
			updatePeopleContactNumPstmt.setString(2, userId);
			updatePeopleContactNumPstmt.executeUpdate();

			updatePstmt.setInt(1, FINISH_CONTACT_UPDATE_CHECKED);
			updatePstmt.setString(2, userId);
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
			db.close(rs);
			db.close(updatePeopleContactRefNumPstmt);
			db.close(selectPstmt);
			db.close(insertPeoplePstmt);
			db.close(updateStatContactCheckedNumPstmt);
			db.close(selectPstmt);
			db.close(updatePstmt);
			db.close(updatePeopleContactNumPstmt);
			db.close(conn);
		}
	}

	public static void main(String[] args) throws IOException {

		beginDateLimit.set(2005, 00, 01);
		Properties properties = new Properties();
		properties.load(PeopleMultiCrawler.class.getResourceAsStream("/flickr.properties"));
		NUM_THREAD = NumberUtils.toInt(properties.getProperty("threads_num"), 1);

		for (int i = 0; i < NUM_THREAD; i++) {
			PeopleMultiCrawler crawler = new PeopleMultiCrawler();
			crawler.start();
			crawler.setName(String.valueOf(i));
		}

	}
}
