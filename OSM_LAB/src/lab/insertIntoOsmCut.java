package lab;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.lang.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import util.DBUtil;

public class insertIntoOsmCut {
	/**
	* Logger for this class
	*/
	private static final Logger logger = LoggerFactory.getLogger(insertIntoOsmCut.class);

	final static int BATCH_SIZE = 1000;
	final static String BUFFER_TABLE_NAME = "D_BUFF";
	final static String OSM_TABLE_NAME = "PLANET_OSM_NEW";
	final static String INSERT_TABLE_NAME = "D_CUT_OSM";
	static int rownum = 1;
	static Calendar startDate;

	static DBUtil db = new DBUtil("/jdbc.properties", 18, 3);

	public static void main(String[] args) throws SQLException {
		System.out.println(SystemUtils.getUserDir());
		System.setProperty("work_dir", SystemUtils.getUserDir().toString());
		long start = System.currentTimeMillis();
		startDate = Calendar.getInstance();
		startDate.setTimeInMillis(start);

		long end = System.currentTimeMillis();
		Calendar endDate = Calendar.getInstance();
		endDate.setTimeInMillis(end);



		Integer[] bufferSizes = { 5, 10, 30 };
		String[] osmSuffixs = { "", "_2011" };

		for (String osmSuffix : osmSuffixs) {
			String osmTableName = OSM_TABLE_NAME + osmSuffix;
			for (int bufferSize : bufferSizes) {
				String bufferTableName = BUFFER_TABLE_NAME + bufferSize;
				String insertTableName = INSERT_TABLE_NAME + bufferSize + osmSuffix;
				insert(osmTableName, bufferTableName, insertTableName);
			}
		}


		logger.info("start time:" + startDate.getTime()); //$NON-NLS-1$
		logger.info("end time:" + endDate.getTime()); //$NON-NLS-1$
		logger.info("main(String[]) - escaped time:" + (System.currentTimeMillis() - start) / 1000.0); //$NON-NLS-1$
	}

	private static void insert(String osmTableName, String bufferTableName, String insertTableName) throws SQLException {
		logger.info("osmTableName:" + osmTableName + "\t|bufferTableName:" + bufferTableName + "\t|insertTableName:" + insertTableName);
		Connection conn = db.getConn();
		conn.setAutoCommit(false);
		int selectedNum = 0;
		int insertedNum = 0;

		PreparedStatement osmCountStmt = db.getPstmt(conn, "select count(*) num from " + osmTableName);
		PreparedStatement osmSelectStmt = db.getPstmt(conn, "select * from " + osmTableName);
		osmSelectStmt.setFetchSize(BATCH_SIZE);

		ResultSet osmCountRs = db.getRs(osmCountStmt);
		ResultSet osmSelectRs = db.getRs(osmSelectStmt);

		osmCountRs.next();
		long totalNum = osmCountRs.getLong("num");


		while(osmSelectRs.next()){
			selectedNum++;

			PreparedStatement bufferSelectStmt = db.getPstmt(conn, "select * from " + bufferTableName + "where ");
			ResultSet bufferSelectRs = db.getRs(bufferSelectStmt);

			while(bufferSelectRs.next()){

				PreparedStatement pgInsertStmt = db.getPstmt(conn, "insert into " + insertTableName + "() values ()");
				insertedNum += pgInsertStmt.executeUpdate();
				db.close(pgInsertStmt);
			}
			db.close(bufferSelectRs);
			db.close(bufferSelectStmt);



			conn.commit();
			logger.debug("osmTableName:" + osmTableName + "\t|bufferTableName:" + bufferTableName + "\t|insertTableName:" + insertTableName);
			logger.debug("totalNum:" + totalNum + "\t|selectedNum:" + selectedNum + "\t|insertedNum:" + insertedNum);
		}

		db.close(osmSelectRs);
		db.close(osmCountRs);
		db.close(osmSelectStmt);
		db.close(osmCountStmt);
		db.close(conn);
	}

}
