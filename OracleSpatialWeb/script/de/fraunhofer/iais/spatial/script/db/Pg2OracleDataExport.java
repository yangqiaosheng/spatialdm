package de.fraunhofer.iais.spatial.script.db;

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

import org.apache.commons.lang3.StringUtils;

import de.fraunhofer.iais.spatial.util.DBUtil;

public class Pg2OracleDataExport {

	private static final int BATCH_SIZE = 1;
	static DBUtil fromDb = new DBUtil("/jdbc_pg.properties", 3, 1);
	static DBUtil toDb = new DBUtil("/jdbc.properties", 3, 1);

	public static void main(String[] args) throws IOException, SQLException {
		Date startDate = new Date();
		long start = System.currentTimeMillis();

//		copyFlickrEurope("FLICKR_EUROPE_COUNT");
		String tableName = new BufferedReader(new InputStreamReader(System.in)).readLine();
		System.out.println("TableName:" + tableName);
		copyFlickrEurope(tableName);

		Date endDate = new Date();
		long end = System.currentTimeMillis();
		System.out.println("start:" + startDate + " | end: " + endDate + " |cost:" + (end - start) / 1000 + "s");
	}

	private static void copyFlickrEurope(String tableName) throws SQLException {
		Connection fromConn = fromDb.getConn();
		fromConn.setAutoCommit(false);
		Connection toConn = toDb.getConn();
		PreparedStatement fromCountStmt = fromDb.getPstmt(fromConn, "select n_tup_ins as num from pg_stat_user_tables where relname = '"+ tableName + "'" );
		PreparedStatement fromSelectStmt = fromDb.getPstmt(fromConn, "select * from " + tableName);
		fromSelectStmt.setFetchSize(BATCH_SIZE);
		PreparedStatement pgInsertStmt = null;
		ResultSet fromCountRs = fromDb.getRs(fromCountStmt);
		ResultSet fromRs = fromDb.getRs(fromSelectStmt);

		try {
			fromCountRs.next();
			long totalNum = fromCountRs.getLong("num");

			ResultSetMetaData rsMetaData = fromRs.getMetaData();
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
			pgInsertStmt = toDb.getPstmt(toConn, insertSql);
			int insertedNum = 0;
			while (fromRs.next()) {
				for (int i = 1; i <= rsMetaData.getColumnCount(); i++) {
					if (rsMetaData.getColumnType(i) == Types.TIMESTAMP) {
						pgInsertStmt.setTimestamp(i, fromRs.getTimestamp(i));
						System.out.println("timestamp:" + rsMetaData.getColumnLabel(i) + " | " + rsMetaData.getColumnTypeName(i) + " | " + rsMetaData.getColumnType(i));
					} else if (rsMetaData.getColumnType(i) == Types.DOUBLE) {
						double value = fromRs.getDouble(i);
						pgInsertStmt.setDouble(i, value);
						System.out.println("double:" + rsMetaData.getColumnLabel(i) + " | " + rsMetaData.getColumnTypeName(i) + " | " + rsMetaData.getColumnType(i));
					} else if (rsMetaData.getColumnType(i) == Types.INTEGER) {
						int value = fromRs.getInt(i);
						pgInsertStmt.setInt(i, value);
						System.out.println("int:" + rsMetaData.getColumnLabel(i) + " | " + rsMetaData.getColumnTypeName(i) + " | " + rsMetaData.getColumnType(i));
					} else {
						pgInsertStmt.setString(i, fromRs.getString(i));
						System.out.println("string:" + rsMetaData.getColumnLabel(i) + " | "+ rsMetaData.getColumnClassName(i) + " | " + rsMetaData.getColumnName(i) + " | " + rsMetaData.getColumnTypeName(i) + " | " + rsMetaData.getColumnType(i));
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
			fromDb.close(fromCountRs);
			fromDb.close(fromRs);
			fromDb.close(fromSelectStmt);
			fromDb.close(fromConn);
			toDb.close(pgInsertStmt);
			toDb.close(toConn);
		}

	}
}
