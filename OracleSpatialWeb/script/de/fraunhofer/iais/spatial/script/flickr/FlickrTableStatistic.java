package de.fraunhofer.iais.spatial.script.flickr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fraunhofer.iais.spatial.util.DbJdbcUtil;

public class FlickrTableStatistic {

	//logging interval (seconds)
	private static int PERIOD = 300;

	/**
	 * Logger for this class
	 */
	private static final Logger logger = LoggerFactory.getLogger(FlickrTableStatistic.class);

//	private static FlickrEuropeAreaMgr areaMgr = null;
	static DbJdbcUtil db = new DbJdbcUtil("/jdbc_pg.properties");

	Timer timer;

	public FlickrTableStatistic(int seconds) {
		timer = new Timer();
		timer.scheduleAtFixedRate(new TableSizeTracker(), 0, seconds * 1000);
	}

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		
		System.out.println("\nPlease input the sampling period (seconds):\n[Default: " + PERIOD + "]");
		String intputPeroid = new BufferedReader(new InputStreamReader(System.in)).readLine();
		if (StringUtils.isNotEmpty(intputPeroid)){
			PERIOD = NumberUtils.toInt(intputPeroid, PERIOD);
		}
		System.out.println(" INPUT PERIOD:" + PERIOD);
		

		new FlickrTableStatistic(PERIOD);
	}

	class TableSizeTracker extends TimerTask {
		@Override
		public void run() {

			long totalWorldPhotoNum = 0;
			long totalEuropePhotoNum = 0;
			long totalPeopleNum = 0;
			long peoplePhotoUpdateCheckedNum = 0;
			long peopleContactUpdateCheckedNum = 0;

			Connection conn = db.getConn();
			PreparedStatement selectWorldStmt = null;
			PreparedStatement selectEuropeStmt = null;
			PreparedStatement selectPeopleStmt = null;
			PreparedStatement selectPeoplePhotoUpdateCheckedStmt = null;
			PreparedStatement selectPeopleContactUpdateCheckedStmt = null;

			ResultSet worldRs = null;
			ResultSet europeRs = null;
			ResultSet peopleRs = null;
			ResultSet peoplePhotoUpdateCheckedRs = null;
			ResultSet peopleContactUpdateCheckedRs = null;

			PreparedStatement insertStmt = null;
			try {

				selectWorldStmt = db.getPstmt(conn, "select n_tup_ins from pg_stat_user_tables where relname = 'flickr_world'");
				selectEuropeStmt = db.getPstmt(conn, "select n_tup_ins from pg_stat_user_tables where relname = 'flickr_europe'");
				selectPeopleStmt = db.getPstmt(conn, "select n_tup_ins from pg_stat_user_tables where relname = 'flickr_people'");
				selectPeoplePhotoUpdateCheckedStmt = db.getPstmt(conn, "select value as n_tup_ins from flickr_statistic_items where name = 'people_photo_checked_num'");
				selectPeopleContactUpdateCheckedStmt = db.getPstmt(conn, "select value as n_tup_ins from flickr_statistic_items where name = 'people_contact_checked_num'");

				worldRs = db.getRs(selectWorldStmt);
				europeRs = db.getRs(selectEuropeStmt);
				peopleRs = db.getRs(selectPeopleStmt);
				peoplePhotoUpdateCheckedRs = db.getRs(selectPeoplePhotoUpdateCheckedStmt);
				peopleContactUpdateCheckedRs = db.getRs(selectPeopleContactUpdateCheckedStmt);

				if (worldRs.next()) {
					totalWorldPhotoNum = worldRs.getLong("n_tup_ins");
				}
				if (europeRs.next()) {
					totalEuropePhotoNum = europeRs.getLong("n_tup_ins");
				}
				if (peopleRs.next()) {
					totalPeopleNum = peopleRs.getLong("n_tup_ins");
				}
				if (peoplePhotoUpdateCheckedRs.next()) {
					peoplePhotoUpdateCheckedNum = peoplePhotoUpdateCheckedRs.getLong("n_tup_ins");
				}
				if (peopleContactUpdateCheckedRs.next()) {
					peopleContactUpdateCheckedNum = peopleContactUpdateCheckedRs.getLong("n_tup_ins");
				}

				insertStmt = db.getPstmt(conn, "insert into flickr_statistic (checked_date, world_photo_num, europe_photo_num, people_num, people_photo_checked_num, people_contact_checked_num) values (?, ?, ?, ?, ?, ?)");

				int i = 1;
				insertStmt.setTimestamp(i++, new Timestamp(new Date().getTime()));
				insertStmt.setLong(i++, totalWorldPhotoNum);
				insertStmt.setLong(i++, totalEuropePhotoNum);
				insertStmt.setLong(i++, totalPeopleNum);
				insertStmt.setLong(i++, peoplePhotoUpdateCheckedNum);
				insertStmt.setLong(i++, peopleContactUpdateCheckedNum);

				insertStmt.executeUpdate();
			} catch (SQLException e) {
				logger.error("run()", e); //$NON-NLS-1$
			} finally {
				db.close(peoplePhotoUpdateCheckedRs);
				db.close(peopleContactUpdateCheckedRs);
				db.close(peopleRs);
				db.close(europeRs);
				db.close(worldRs);

				db.close(insertStmt);

				db.close(selectPeopleContactUpdateCheckedStmt);
				db.close(selectPeoplePhotoUpdateCheckedStmt);
				db.close(selectPeopleStmt);
				db.close(selectEuropeStmt);
				db.close(selectWorldStmt);

				db.close(conn);
			}

			logger.debug(new Date()
					+ " |#europe Photo:" + totalEuropePhotoNum + " |#world Photo:" + totalWorldPhotoNum + " |#people:" + totalPeopleNum + " |#people_photo:" + peoplePhotoUpdateCheckedNum + " |#people_contact:" + peopleContactUpdateCheckedNum); //$NON-NLS-1$
		}

	}

}
