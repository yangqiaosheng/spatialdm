package de.fraunhofer.iais.spatial.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Date;

import org.apache.commons.lang.StringUtils;

public class Oracle2PgDataExport {

	private static final int BATCH_SIZE = 100;
	static DBUtil oracleDb = new DBUtil("/jdbc.properties", 18, 6);
	static DBUtil pgDb = new DBUtil("/jdbc_pg.properties", 18, 6);

	public static void main(String[] args) throws IOException {
		Date startDate = new Date();
		long start = System.currentTimeMillis();

//		copyFlickrEurope("FLICKR_EUROPE_area_320000");
//		copyFlickrEurope("FLICKR_PEOPLE");
//		copyFlickrEurope("FLICKR_EUROPE");
		copyFlickrEurope("FLICKR_EUROPE_COUNT");
//		String tableName = new BufferedReader(new InputStreamReader(System.in)).readLine();
//		System.out.println("TableName:" + tableName);
//		copyFlickrEurope(tableName);

		Date endDate = new Date();
		long end = System.currentTimeMillis();
		System.out.println("start:" + startDate + " | end: " + endDate + " |cost:" + (end - start) / 1000 + "s");
	}

	private static void copyFlickrEurope(String tableName) {
		Connection oracleConn = oracleDb.getConn();
		Connection pgConn = pgDb.getConn();
		PreparedStatement oracleCountStmt = oracleDb.getPstmt(oracleConn, "select count(*) num from " + tableName);
		PreparedStatement oracleSelectStmt = oracleDb.getPstmt(oracleConn, "select * from " + tableName);
		PreparedStatement pgInsertStmt = null;
		ResultSet oracleCountRs = oracleDb.getRs(oracleCountStmt);
		ResultSet oracleRs = oracleDb.getRs(oracleSelectStmt);

		try {
			oracleCountRs.next();
			long totalNum = oracleCountRs.getLong("num");

			ResultSetMetaData rsMetaData = oracleRs.getMetaData();
			System.out.println(rsMetaData.getColumnCount());
			String insertSqlParaNames = "";
			String insertSqlParaNotes = "";
			for (int i = 1; i <= rsMetaData.getColumnCount(); i++) {
				insertSqlParaNames += rsMetaData.getColumnLabel(i) + ", ";
				insertSqlParaNotes += "?, ";

			}
			insertSqlParaNames = StringUtils.removeEnd(insertSqlParaNames, ", ");
			insertSqlParaNotes = StringUtils.removeEnd(insertSqlParaNotes, ", ");

			String insertSql = "insert into " + tableName + " (" + insertSqlParaNames + ") values (" + insertSqlParaNotes + ")";
			System.out.println(insertSql);
			pgInsertStmt = pgDb.getPstmt(pgConn, insertSql);
			int insertedNum = 0;
			while (oracleRs.next()) {
				for (int i = 1; i <= rsMetaData.getColumnCount(); i++) {
					if (rsMetaData.getColumnType(i) == Types.TIMESTAMP) {
						pgInsertStmt.setTimestamp(i, oracleRs.getTimestamp(i));
					} else if (rsMetaData.getColumnType(i) == Types.NUMERIC) {
						double value = oracleRs.getDouble(i);
						if (value == (int) value) {
							pgInsertStmt.setInt(i, (int) value);
							System.out.println("int:" + rsMetaData.getColumnLabel(i) + " | " + rsMetaData.getColumnTypeName(i) + " | " + rsMetaData.getColumnType(i));
						} else if (value == (long) value) {
							pgInsertStmt.setLong(i, (long) value);
							System.out.println("long:" + rsMetaData.getColumnLabel(i) + " | " + rsMetaData.getColumnTypeName(i) + " | " + rsMetaData.getColumnType(i));
						} else if (value == (float) value) {
							pgInsertStmt.setFloat(i, (float) value);
							System.out.println("float:" + rsMetaData.getColumnLabel(i) + " | " + rsMetaData.getColumnTypeName(i) + " | " + rsMetaData.getColumnType(i));
						} else {
							pgInsertStmt.setDouble(i, value);
							System.out.println("double:" + rsMetaData.getColumnLabel(i) + " | " + rsMetaData.getColumnTypeName(i) + " | " + rsMetaData.getColumnType(i));
						}

					} else {
						pgInsertStmt.setString(i, oracleRs.getString(i));
						System.out.println(rsMetaData.getColumnLabel(i) + " | "+ rsMetaData.getColumnClassName(i) + " | " + rsMetaData.getColumnName(i) + " | " + rsMetaData.getColumnTypeName(i) + " | " + rsMetaData.getColumnType(i));
					}
				}
//				pgInsertStmt.executeUpdate();
				pgInsertStmt.addBatch();
				System.out.println("inserted num:" + (insertedNum++) + "/" + totalNum);
				if (insertedNum % BATCH_SIZE == 0) {
					pgInsertStmt.executeBatch();
				}
			}
			pgInsertStmt.executeBatch();

		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			oracleDb.close(oracleCountRs);
			oracleDb.close(oracleRs);
			oracleDb.close(oracleSelectStmt);
			oracleDb.close(oracleConn);
			pgDb.close(pgInsertStmt);
			pgDb.close(pgConn);
		}

	}
}
