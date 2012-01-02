package lab;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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

import util.DbJdbcUtil;
import util.LevenshteinDistance;

public class InsertIntoOsmCut {
	/**
	* Logger for this class
	*/
	private static final Logger logger = LoggerFactory.getLogger(InsertIntoOsmCut.class);

	final static int BATCH_SIZE = 1000;
	final static String EG_BUFFER_TABLE_NAME = "D_BUFF5";
	final static String EG_OSM_TABLE_NAME = "PLANET_OSM_LINE_NEW";
	final static String EG_OUTPUT_TABLE_NAME = "D_CUT_OSM5";

	public static void main(String[] args) throws SQLException, IOException {
		System.out.println(SystemUtils.getUserDir());
		System.setProperty("work_dir", SystemUtils.getUserDir().toString());
		long startTableInsertTime = System.currentTimeMillis();

		System.out.println("\nPlease input the Buffer TableName:\n[Default: " + EG_BUFFER_TABLE_NAME + "]");
		String bufferTableName = new BufferedReader(new InputStreamReader(System.in)).readLine();
		if (StringUtils.isEmpty(bufferTableName)){
			bufferTableName = EG_BUFFER_TABLE_NAME;
		}
		System.out.println("Buffer Table:" + bufferTableName);

		System.out.println("\nPlease input the OSM TableName:\n[Default: " + EG_OSM_TABLE_NAME + "]");
		String osmTableName = new BufferedReader(new InputStreamReader(System.in)).readLine();
		if (StringUtils.isEmpty(osmTableName)){
			osmTableName = EG_OSM_TABLE_NAME;
		}
		System.out.println("OSM Table:" + osmTableName);

		System.out.println("\nPlease input the Output TableName to store the result:\n[Default: " + EG_OUTPUT_TABLE_NAME + "]");
		String outputTableName = new BufferedReader(new InputStreamReader(System.in)).readLine();
		if (StringUtils.isEmpty(outputTableName)){
			outputTableName = EG_OUTPUT_TABLE_NAME;
		}
		System.out.println("Output Table:" + outputTableName);

		logger.info("osmTableName:" + osmTableName + "\t|bufferTableName:" + bufferTableName + "\t|outputTableName:" + outputTableName);
		insert(osmTableName, bufferTableName, outputTableName);

		long endtTableInsertTime = System.currentTimeMillis();
		logger.info("main(String[]) - cost time:" + (endtTableInsertTime - startTableInsertTime) / 1000 + "s"); //$NON-NLS-1$

	}

	private static void insert(String osmTableName, String bufferTableName, String outputTableName) throws SQLException {
		Calendar startTableInsertDate = Calendar.getInstance();
		logger.info("startTableDate:" + startTableInsertDate.getTime() + "\t|osmTableName:" + osmTableName + "\t|bufferTableName:" + bufferTableName + "\t|insertTableName:" + outputTableName);
		Connection conn = DbJdbcUtil.getConn();
		conn.setAutoCommit(false);
		int selectedNum = 0;
		int insertedNum = 0;

		PreparedStatement osmCountStmt = DbJdbcUtil.getPstmt(conn, "select count(*) num from " + osmTableName);
		PreparedStatement osmSelectStmt = DbJdbcUtil.getPstmt(conn, "select * from " + osmTableName);
//		osmSelectStmt.setFetchSize(BATCH_SIZE); only for PostGIS

		ResultSet osmCountRs = DbJdbcUtil.getRs(osmCountStmt);
		ResultSet osmSelectRs = DbJdbcUtil.getRs(osmSelectStmt);

		osmCountRs.next();
		long totalNum = osmCountRs.getLong("num");

		while (osmSelectRs.next()) {
			selectedNum++;

			int osmId = osmSelectRs.getInt("osm_id");
			String osmName = StringUtils.defaultString(osmSelectRs.getString("name"));
			String osmRef = StringUtils.defaultString(osmSelectRs.getString("ref_"));
			STRUCT osmGeom = (STRUCT) osmSelectRs.getObject("way");
			PreparedStatement bufferSelectStmt = DbJdbcUtil.getPstmt(conn, "" + "select t.*, " + "SDO_GEOM.SDO_INTERSECTION(t.geo_object, ?, 0.005) cut_geom, "
					+ "SDO_GEOM.SDO_LENGTH(SDO_GEOM.SDO_INTERSECTION(t.geo_object, ?, 0.005), 0.005, 'UNIT=M') osm_seglaenge " + "from " + bufferTableName + " t where sdo_relate(t.geo_object, ?, 'mask=anyinteract') = 'TRUE'");
			bufferSelectStmt.setObject(1, osmGeom);
			bufferSelectStmt.setObject(2, osmGeom);
			bufferSelectStmt.setObject(3, osmGeom);
			ResultSet bufferSelectRs = DbJdbcUtil.getRs(bufferSelectStmt);

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
					pgInsertStmt = DbJdbcUtil.getPstmt(conn, "" + "insert into " + outputTableName + "(osm_id, nav_id, osm_name, osm_name2, nav_name, nav_name2, nav_laenge, osm_seglaenge, ls1, ls2, ls3, ls4, geoloc)"
							+ " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
				} else {
					pgInsertStmt = DbJdbcUtil.getPstmt(conn, "" + "insert into " + outputTableName + "(osm_id, nav_id, osm_name, osm_name2, nav_name, nav_name2, nav_laenge, osm_seglaenge, ls1, ls2, ls3, ls4)"
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
				DbJdbcUtil.close(pgInsertStmt);
			}
			DbJdbcUtil.close(bufferSelectRs);
			DbJdbcUtil.close(bufferSelectStmt);

			conn.commit();
			logger.debug("startTableDate:" + startTableInsertDate.getTime() + "\t|osmTableName:" + osmTableName + "\t|bufferTableName:" + bufferTableName + "\t|insertTableName:" + outputTableName);
			logger.debug("totalNum:" + totalNum + "\t|selectedNum:" + selectedNum + "\t|insertedNum:" + insertedNum);
		}

		DbJdbcUtil.close(osmSelectRs);
		DbJdbcUtil.close(osmCountRs);
		DbJdbcUtil.close(osmSelectStmt);
		DbJdbcUtil.close(osmCountStmt);
		DbJdbcUtil.close(conn);
	}

}