package de.fraunhofer.iais.spatial.util;

import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DB {
	
	/**
	 * Logger for this class
	 */
	private static final Logger logger = LoggerFactory.getLogger(DB.class);

	private static Properties pros = null;

	private DB() {
		// initialize the JDBC Configuration
		pros = new Properties();
		try {
			pros.load(new FileReader(DB.class.getResource("/jdbc.properties").getFile()));
		} catch (IOException e1) {
			logger.error("getConn()", e1); //$NON-NLS-1$
		}
	}

	public static Connection getConn() {

		Connection conn = null;

		// Use JDBC Connection
		try {
			Class.forName(pros.getProperty("driver"));
			conn = DriverManager.getConnection(pros.getProperty("url"), pros.getProperty("username"), pros.getProperty("password"));
		} catch (ClassNotFoundException e) {
			logger.error("getConn()", e); //$NON-NLS-1$
		} catch (SQLException e) {
			logger.error("getConn() - Could not Connect to the Web_SQL Server", e); //$NON-NLS-1$
		}

		return conn;
	}

	public static PreparedStatement getPstmt(Connection conn, String sql) {

		PreparedStatement pstmt = null;
		try {
			logger.debug("sql:" + sql);
			pstmt = conn.prepareStatement(sql);
		} catch (SQLException e) {
			logger.error("getPstmt(Connection, String)", e); //$NON-NLS-1$
		}

		return pstmt;
	}

	public static PreparedStatement getPstmt(Connection conn, String sql, int autoGeneratedkey) {

		PreparedStatement pstmt = null;
		try {
			logger.debug("sql:" + sql);
			pstmt = conn.prepareStatement(sql, autoGeneratedkey);
		} catch (SQLException e) {
			logger.error("getPstmt(Connection, String, int)", e); //$NON-NLS-1$
		}

		return pstmt;
	}

	public static ResultSet getRs(PreparedStatement pstmt) {

		ResultSet rs = null;
		try {
			rs = pstmt.executeQuery();
		} catch (SQLException e) {
			logger.error("getRs(PreparedStatement)", e); //$NON-NLS-1$
		}

		return rs;
	}


	public static void close(Connection conn) {

		if (conn != null) {
			try {
				conn.close();
			} catch (SQLException e) {
				logger.error("close(Connection)", e); //$NON-NLS-1$
			} finally {
//				conn = null;
			}
		}
	}

	public static void close(Statement stmt) {

		if (stmt != null) {
			try {
				stmt.close();
			} catch (SQLException e) {
				logger.error("close(Statement)", e); //$NON-NLS-1$
			} finally {
				stmt = null;
			}
		}
	}

	public static void close(ResultSet rs) {

		if (rs != null) {
			try {
				rs.close();
			} catch (SQLException e) {
				logger.error("close(ResultSet)", e); //$NON-NLS-1$
			} finally {
				rs = null;
			}
		}
	}
}