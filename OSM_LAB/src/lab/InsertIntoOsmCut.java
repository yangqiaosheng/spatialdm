package lab;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;

import oracle.sql.STRUCT;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import util.DBUtil;
import util.LevenshteinDistance;

public class InsertIntoOsmCut {
	/**
	* Logger for this class
	*/
	private static final Logger logger = LoggerFactory.getLogger(InsertIntoOsmCut.class);

	final static int BATCH_SIZE = 1000;
	final static String BUFFER_TABLE_NAME = "INA.D_BUFF5";
	final static String OSM_TABLE_NAME = "INA.PLANET_OSM_LINE_NEW";
	final static String INSERT_TABLE_NAME = "D_CUT_OSM5";
	static Calendar startDate;

	public static void main(String[] args) throws SQLException {
		System.out.println(SystemUtils.getUserDir());
		System.setProperty("work_dir", SystemUtils.getUserDir().toString());
		long start = System.currentTimeMillis();
		startDate = Calendar.getInstance();
		startDate.setTimeInMillis(start);

		String osmTableName = OSM_TABLE_NAME;
		long startTableInsertTime = System.currentTimeMillis();
		String bufferTableName = BUFFER_TABLE_NAME;
		String insertTableName = INSERT_TABLE_NAME;
		insert(osmTableName, bufferTableName, insertTableName);

		Calendar endDate = Calendar.getInstance();
		endDate.setTimeInMillis(System.currentTimeMillis());
		long endtTableInsertTime = System.currentTimeMillis();
		logger.info("osmTableName:" + osmTableName + "\t|bufferTableName:" + bufferTableName + "\t|insertTableName:" + insertTableName + "\t|cost time:" + (endtTableInsertTime - startTableInsertTime) / 1000 + "s");
		logger.info("start time:" + startDate.getTime()); //$NON-NLS-1$
		logger.info("endTableTime:" + endDate.getTime()); //$NON-NLS-1$
		logger.info("main(String[]) - escaped time:" + (System.currentTimeMillis() - start) / 1000.0); //$NON-NLS-1$

	}

	private static void insert(String osmTableName, String bufferTableName, String insertTableName) throws SQLException {
		Calendar startTableInsertDate = Calendar.getInstance();
		logger.info("startTableDate:" + startTableInsertDate.getTime() + "\t|osmTableName:" + osmTableName + "\t|bufferTableName:" + bufferTableName + "\t|insertTableName:" + insertTableName);
		Connection conn = DBUtil.getConn();
		conn.setAutoCommit(false);
		int selectedNum = 0;
		int insertedNum = 0;

		PreparedStatement osmCountStmt = DBUtil.getPstmt(conn, "select count(*) num from " + osmTableName);
		PreparedStatement osmSelectStmt = DBUtil.getPstmt(conn, "select * from " + osmTableName);
//		osmSelectStmt.setFetchSize(BATCH_SIZE); only for PostGIS

		ResultSet osmCountRs = DBUtil.getRs(osmCountStmt);
		ResultSet osmSelectRs = DBUtil.getRs(osmSelectStmt);

		osmCountRs.next();
		long totalNum = osmCountRs.getLong("num");

		while (osmSelectRs.next()) {
			selectedNum++;

			int osmId = osmSelectRs.getInt("osm_id");
			String osmName = StringUtils.defaultString(osmSelectRs.getString("name"));
			String osmRef = StringUtils.defaultString(osmSelectRs.getString("ref_"));
			STRUCT osmGeom = (STRUCT) osmSelectRs.getObject("way");
			PreparedStatement bufferSelectStmt = DBUtil.getPstmt(conn, "" + "select t.*, " + "SDO_GEOM.SDO_INTERSECTION(t.geo_object, ?, 0.005) cut_geom, "
					+ "SDO_GEOM.SDO_LENGTH(SDO_GEOM.SDO_INTERSECTION(t.geo_object, ?, 0.005), 0.005, 'UNIT=M') osm_seglaenge " + "from " + bufferTableName + " t where sdo_relate(t.geo_object, ?, 'mask=anyinteract') = 'TRUE'");
			bufferSelectStmt.setObject(1, osmGeom);
			bufferSelectStmt.setObject(2, osmGeom);
			bufferSelectStmt.setObject(3, osmGeom);
			ResultSet bufferSelectRs = DBUtil.getRs(bufferSelectStmt);

			while (bufferSelectRs.next()) {

				int navId = bufferSelectRs.getInt("id");
				String navPrimName = StringUtils.defaultString(bufferSelectRs.getString("prim_name"));
				String navSekName = StringUtils.defaultString(bufferSelectRs.getString("sek_name"));
				float navLaenge = bufferSelectRs.getInt("laenge");
				float osmSegLaenge = bufferSelectRs.getFloat("osm_seglaenge");
				STRUCT cutGeom = (STRUCT) bufferSelectRs.getObject("cut_geom");

				int ls1 = LevenshteinDistance.computeLevenshteinDistance(osmName.toUpperCase(), navPrimName.toUpperCase());
				int ls2 = LevenshteinDistance.computeLevenshteinDistance(osmRef.toUpperCase(), navSekName.toUpperCase());
				int ls3 = LevenshteinDistance.computeLevenshteinDistance(osmName.toUpperCase(), navSekName.toUpperCase());
				int ls4 = LevenshteinDistance.computeLevenshteinDistance(osmRef.toUpperCase(), navPrimName.toUpperCase());

				System.out.println("osmID:" + osmId + "\t|navId:" + navId + "\t|osmSegLaenge:" + osmSegLaenge + "\t|navLaenge:" + navLaenge + "\t|ls1:" + ls1 + "\t|ls2:" + ls2 + "\t|ls3:" + ls3 + "\t|ls4:" + ls4);
				System.out.println("cutGeom:" + cutGeom);

				PreparedStatement pgInsertStmt = null;
				if (cutGeom != null) {
					pgInsertStmt = DBUtil.getPstmt(conn, "" + "insert into " + insertTableName + "(osm_id, nav_id, osm_name, osm_name2, nav_name, nav_name2, nav_laenge, osm_seglaenge, ls1, ls2, ls3, ls4, geoloc)"
							+ " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
				} else {
					pgInsertStmt = DBUtil.getPstmt(conn, "" + "insert into " + insertTableName + "(osm_id, nav_id, osm_name, osm_name2, nav_name, nav_name2, nav_laenge, osm_seglaenge, ls1, ls2, ls3, ls4)"
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
				DBUtil.close(pgInsertStmt);
			}
			DBUtil.close(bufferSelectRs);
			DBUtil.close(bufferSelectStmt);

			conn.commit();
			logger.debug("startTableDate:" + startTableInsertDate.getTime() + "\t|osmTableName:" + osmTableName + "\t|bufferTableName:" + bufferTableName + "\t|insertTableName:" + insertTableName);
			logger.debug("totalNum:" + totalNum + "\t|selectedNum:" + selectedNum + "\t|insertedNum:" + insertedNum);
		}

		DBUtil.close(osmSelectRs);
		DBUtil.close(osmCountRs);
		DBUtil.close(osmSelectStmt);
		DBUtil.close(osmCountStmt);
		DBUtil.close(conn);
	}

}
