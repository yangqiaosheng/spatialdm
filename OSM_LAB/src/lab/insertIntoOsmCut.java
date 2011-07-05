package lab;

import java.awt.geom.Point2D;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import oracle.spatial.geometry.JGeometry;
import oracle.sql.STRUCT;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import util.DBUtil;
import util.LevenshteinDistance;

public class insertIntoOsmCut {
	/**
	* Logger for this class
	*/
	private static final Logger logger = LoggerFactory.getLogger(insertIntoOsmCut.class);

	final static int BATCH_SIZE = 1000;
	final static String BUFFER_TABLE_NAME = "INA.D_BUFF";
	final static String OSM_TABLE_NAME = "INA.PLANET_OSM_LINE_NEW";
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
				long startTableInsertTime = System.currentTimeMillis();
				String bufferTableName = BUFFER_TABLE_NAME + bufferSize;
				String insertTableName = INSERT_TABLE_NAME + bufferSize + osmSuffix;
				insert(osmTableName, bufferTableName, insertTableName);
				long endtTableInsertTime = System.currentTimeMillis();
				logger.info("osmTableName:" + osmTableName + "\t|bufferTableName:" + bufferTableName + "\t|insertTableName:" + insertTableName + "\t|cost time:" + (endtTableInsertTime - startTableInsertTime)/1000 + "s");
				logger.info("start time:" + startDate.getTime()); //$NON-NLS-1$
				logger.info("end time:" + endDate.getTime()); //$NON-NLS-1$
				logger.info("main(String[]) - escaped time:" + (System.currentTimeMillis() - start) / 1000.0); //$NON-NLS-1$
			}
		}


	}

	private static void insert(String osmTableName, String bufferTableName, String insertTableName) throws SQLException {
		logger.info("osmTableName:" + osmTableName + "\t|bufferTableName:" + bufferTableName + "\t|insertTableName:" + insertTableName);
		Connection conn = db.getConn();
		conn.setAutoCommit(false);
		int selectedNum = 0;
		int insertedNum = 0;

		PreparedStatement osmCountStmt = db.getPstmt(conn, "select count(*) num from " + osmTableName);
		PreparedStatement osmSelectStmt = db.getPstmt(conn, "select * from " + osmTableName);
//		osmSelectStmt.setFetchSize(BATCH_SIZE);

		ResultSet osmCountRs = db.getRs(osmCountStmt);
		ResultSet osmSelectRs = db.getRs(osmSelectStmt);

		osmCountRs.next();
		long totalNum = osmCountRs.getLong("num");


		while(osmSelectRs.next()){
			selectedNum++;

			int osmId = osmSelectRs.getInt("osm_id");
			String osmName = StringUtils.defaultString(osmSelectRs.getString("name"));
			String osmRef = StringUtils.defaultString(osmSelectRs.getString("ref_"));
			STRUCT osmGeom = (STRUCT) osmSelectRs.getObject("way");
			PreparedStatement bufferSelectStmt = db.getPstmt(conn, "" +
					"select t.*, " +
						"SDO_GEOM.SDO_INTERSECTION(t.geo_object, ?, 0.005) cut_geom, " +
						"SDO_GEOM.SDO_LENGTH(SDO_GEOM.SDO_INTERSECTION(t.geo_object, ?, 0.005), 0.005, 'UNIT=M') osm_seglaenge " +
					"from " + bufferTableName + " t where sdo_relate(t.geo_object, ?, 'mask=anyinteract') = 'TRUE'");
			bufferSelectStmt.setObject(1, osmGeom);
			bufferSelectStmt.setObject(2, osmGeom);
			bufferSelectStmt.setObject(3, osmGeom);
			ResultSet bufferSelectRs = db.getRs(bufferSelectStmt);

			while(bufferSelectRs.next()){

				int navId = bufferSelectRs.getInt("id");
				String navPrimName = StringUtils.defaultString(bufferSelectRs.getString("prim_name"));
				String navSekName = StringUtils.defaultString(bufferSelectRs.getString("sek_name"));
				float navLaenge = bufferSelectRs.getInt("laenge");
				float osmSegLaenge =  bufferSelectRs.getFloat("osm_seglaenge");
				STRUCT cutGeom = (STRUCT) bufferSelectRs.getObject("cut_geom");

				int ls1 = LevenshteinDistance.computeLevenshteinDistance(osmName, navPrimName);
				int ls2 = LevenshteinDistance.computeLevenshteinDistance(osmRef, navSekName);
				int ls3 = LevenshteinDistance.computeLevenshteinDistance(osmName, navSekName);
				int ls4 = LevenshteinDistance.computeLevenshteinDistance(osmRef, navPrimName);

				System.out.println("osmID:" + osmId + "\t|navId:" + navId + "\t|osmSegLaenge:" + osmSegLaenge + "\t|navLaenge:" + navLaenge + "\t|ls1:" + ls1 + "\t|ls2:" + ls2 + "\t|ls3:" + ls3 + "\t|ls4:" + ls4);
				System.out.println("cutGeom:" + cutGeom);

				PreparedStatement pgInsertStmt = null;
				if (cutGeom != null) {
					pgInsertStmt = db.getPstmt(conn, ""
							+ "insert into " + insertTableName + "(osm_id, nav_id, osm_name, osm_name2, nav_name, nav_name2, nav_laenge, osm_seglaenge, ls1, ls2, ls3, ls4, geoloc)"
							+ " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
				}else{
					pgInsertStmt = db.getPstmt(conn, ""
							+ "insert into " + insertTableName + "(osm_id, nav_id, osm_name, osm_name2, nav_name, nav_name2, nav_laenge, osm_seglaenge, ls1, ls2, ls3, ls4)"
							+ " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
				}

				int i = 1;
				pgInsertStmt.setInt(i++, osmId);
				pgInsertStmt.setInt(i++, navId);
				pgInsertStmt.setString(i++, osmName);
				pgInsertStmt.setString(i++, osmRef);
				pgInsertStmt.setString(i++, navPrimName);
				pgInsertStmt.setString(i++, navSekName);
				pgInsertStmt.setFloat(i++, navLaenge);
				pgInsertStmt.setFloat(i++, osmSegLaenge);
				pgInsertStmt.setInt(i++, ls1);
				pgInsertStmt.setInt(i++, ls2);
				pgInsertStmt.setInt(i++, ls3);
				pgInsertStmt.setInt(i++, ls4);
				if (cutGeom != null) {
					pgInsertStmt.setObject(i++, cutGeom);
				}

				insertedNum += pgInsertStmt.executeUpdate();

				conn.commit();
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
