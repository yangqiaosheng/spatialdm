package de.fraunhofer.iais.spatial.script.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.mock.jndi.SimpleNamingContextBuilder;

import de.fraunhofer.iais.spatial.scheduling.CleanOldKmlsTimerTask;
import de.fraunhofer.iais.spatial.service.FlickrEuropeAreaMgr;
import de.fraunhofer.iais.spatial.util.DBUtil;

public class FlickrTableStatistic {

	private static final int PERIOD = 900;

	/**
	 * Logger for this class
	 */
	private static final Logger logger = LoggerFactory.getLogger(FlickrTableStatistic.class);

//	private static FlickrEuropeAreaMgr areaMgr = null;
	static DBUtil db = new DBUtil("/jdbc_pg.properties", 3, 1);

	Timer timer;

	public FlickrTableStatistic(int seconds) {
		timer = new Timer();
		timer.scheduleAtFixedRate(new TableSizeTracker(), 0, seconds * 1000);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		new FlickrTableStatistic(PERIOD);
	}

	class TableSizeTracker extends TimerTask {
		@Override
		public void run() {

			long totalWorldPhotoNum = -1;
			long totalEuropePhotoNum = -1;
			long totalPeopleNum = -1;

			Connection conn = db.getConn();
			PreparedStatement selectWorldStmt = null;
			PreparedStatement selectEuropeStmt = null;
			PreparedStatement selectPeopleStmt = null;

			ResultSet worldRs = null;
			ResultSet europeRs = null;
			ResultSet peopleRs = null;

			PreparedStatement insertStmt = null;
			try {

				selectWorldStmt = db.getPstmt(conn, "select n_tup_ins from pg_stat_user_tables where relname = 'flickr_photo'");
				selectEuropeStmt = db.getPstmt(conn, "select n_tup_ins from pg_stat_user_tables where relname = 'flickr_europe'");
				selectPeopleStmt = db.getPstmt(conn, "select n_tup_ins from pg_stat_user_tables where relname = 'flickr_people'");
				worldRs = db.getRs(selectWorldStmt);
				europeRs = db.getRs(selectEuropeStmt);
				peopleRs = db.getRs(selectPeopleStmt);

				if(worldRs.next()){
					totalWorldPhotoNum = worldRs.getLong("n_tup_ins");
				}
				if(europeRs.next()){
					totalEuropePhotoNum = europeRs.getLong("n_tup_ins");
				}
				if(peopleRs.next()){
					totalPeopleNum = peopleRs.getLong("n_tup_ins");
				}

				insertStmt = db.getPstmt(conn, "insert into flickr_statistic (checked_date, world_photo_num, europe_photo_num, people_num) values (?, ?, ?, ?)");

				int i = 1;
				insertStmt.setTimestamp(i++, new Timestamp(new Date().getTime()));
				insertStmt.setLong(i++, totalWorldPhotoNum);
				insertStmt.setLong(i++, totalEuropePhotoNum);
				insertStmt.setLong(i++, totalPeopleNum);

				insertStmt.executeUpdate();
			} catch (SQLException e) {
				logger.error("run()", e); //$NON-NLS-1$
			} finally {
				db.close(worldRs);
				db.close(europeRs);
				db.close(peopleRs);

				db.close(selectWorldStmt);
				db.close(selectEuropeStmt);
				db.close(selectPeopleStmt);
				db.close(insertStmt);

				db.close(conn);
			}

			logger.debug(new Date() + " |#people:" + totalPeopleNum + " |#europe Photo:" + totalEuropePhotoNum + " |#world Photo:" + totalWorldPhotoNum); //$NON-NLS-1$
		}

	}

}
