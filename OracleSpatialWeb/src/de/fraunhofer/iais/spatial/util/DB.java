package de.fraunhofer.iais.spatial.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.taglibs.standard.extra.spath.Path;

public class DB {
	/**
	 * Logger for this class
	 */

	private static Properties pros = null;

	private static final Log logger = LogFactory.getLog(DB.class);

	private DB() {

	}

	public static Connection getConn() {
		if (logger.isDebugEnabled()) {
			logger.debug("getConn() - start"); //$NON-NLS-1$
		}
		// initialize the JDBC Configuration
		if (pros == null) {
			pros = new Properties();
			try {
				if (new File("src/jdbc.properties").exists()) {
					//J2SE Application
					pros.load(new FileReader("src/jdbc.properties"));
				} else {
					//Web Application
					pros.load(new InputStreamReader(Thread.currentThread()
							.getContextClassLoader().getResourceAsStream(
									"jdbc.properties")));
				}
			} catch (IOException e1) {
				logger.error("getConn()", e1); //$NON-NLS-1$
			}
		}

		Connection conn = null;

		// Use JDBC Connection
		try {
			Class.forName(pros.getProperty("driver"));
			conn = DriverManager.getConnection(pros.getProperty("url"), pros
					.getProperty("username"), pros.getProperty("password"));
		} catch (ClassNotFoundException e) {
			logger.error("getConn()", e); //$NON-NLS-1$
		} catch (SQLException e) {
			logger.error("getConn()", e); //$NON-NLS-1$
			if (logger.isDebugEnabled()) {
				logger
						.debug("getConn() - Could not Connect to the Web_SQL Server"); //$NON-NLS-1$
			}
		}

		if (logger.isDebugEnabled()) {
			logger.debug("getConn() - end"); //$NON-NLS-1$
		}
		return conn;
	}

	public static PreparedStatement getPstmt(Connection conn, String sql) {
		if (logger.isDebugEnabled()) {
			logger.debug("getPstmt(Connection, String) - start"); //$NON-NLS-1$
		}

		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement(sql);
		} catch (SQLException e) {
			logger.error("getPstmt(Connection, String)", e); //$NON-NLS-1$
		}

		if (logger.isDebugEnabled()) {
			logger.debug("getPstmt(Connection, String) - end"); //$NON-NLS-1$
		}
		return pstmt;
	}

	public static PreparedStatement getPstmt(Connection conn, String sql,
			int autoGeneratedkey) {
		if (logger.isDebugEnabled()) {
			logger.debug("getPstmt(Connection, String, int) - start"); //$NON-NLS-1$
		}

		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement(sql, autoGeneratedkey);
		} catch (SQLException e) {
			logger.error("getPstmt(Connection, String, int)", e); //$NON-NLS-1$
		}

		if (logger.isDebugEnabled()) {
			logger.debug("getPstmt(Connection, String, int) - end"); //$NON-NLS-1$
		}
		return pstmt;
	}

	public static ResultSet getRs(PreparedStatement pstmt) {
		if (logger.isDebugEnabled()) {
			logger.debug("getRs(PreparedStatement) - start"); //$NON-NLS-1$
		}

		ResultSet rs = null;
		try {
			rs = pstmt.executeQuery();
		} catch (SQLException e) {
			logger.error("getRs(PreparedStatement)", e); //$NON-NLS-1$
		}

		if (logger.isDebugEnabled()) {
			logger.debug("getRs(PreparedStatement) - end"); //$NON-NLS-1$
		}
		return rs;
	}

	public static ResultSet getRs(Statement stmt, String sql) {
		if (logger.isDebugEnabled()) {
			logger.debug("getRs(Statement, String) - start"); //$NON-NLS-1$
		}

		ResultSet rs = null;
		try {
			rs = stmt.executeQuery(sql);
		} catch (SQLException e) {
			logger.error("getRs(Statement, String)", e); //$NON-NLS-1$
		}

		if (logger.isDebugEnabled()) {
			logger.debug("getRs(Statement, String) - end"); //$NON-NLS-1$
		}
		return rs;
	}

	public static void close(Connection conn) {
		if (logger.isDebugEnabled()) {
			logger.debug("close(Connection) - start"); //$NON-NLS-1$
		}

		if (conn != null) {
			try {
				conn.close();

			} catch (SQLException e) {
				logger.error("close(Connection)", e); //$NON-NLS-1$
			}
			conn = null;
		}

		if (logger.isDebugEnabled()) {
			logger.debug("close(Connection) - end"); //$NON-NLS-1$
		}
	}

	public static void close(Statement stmt) {
		if (logger.isDebugEnabled()) {
			logger.debug("close(Statement) - start"); //$NON-NLS-1$
		}

		if (stmt != null) {
			try {
				stmt.close();
			} catch (SQLException e) {
				logger.error("close(Statement)", e); //$NON-NLS-1$
			}
			stmt = null;
		}

		if (logger.isDebugEnabled()) {
			logger.debug("close(Statement) - end"); //$NON-NLS-1$
		}
	}

	public static void close(ResultSet rs) {
		if (logger.isDebugEnabled()) {
			logger.debug("close(ResultSet) - start"); //$NON-NLS-1$
		}

		if (rs != null) {
			try {
				rs.close();
			} catch (SQLException e) {
				logger.error("close(ResultSet)", e); //$NON-NLS-1$
			}
			rs = null;
		}

		if (logger.isDebugEnabled()) {
			logger.debug("close(ResultSet) - end"); //$NON-NLS-1$
		}
	}
}