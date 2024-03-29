package de.fraunhofer.iais.spatial.util;

import java.beans.PropertyVetoException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mchange.v2.c3p0.ComboPooledDataSource;

public class DBUtil {

	/**
	 * Logger for this class
	 */
	private static final Logger logger = LoggerFactory.getLogger(DBUtil.class);

	private Properties pros = null;
	//	private static BoneCPDataSource ds = null;
	private ComboPooledDataSource ds = null;

	/**
	 * initialize the c3p0 Connection Pool
	*/
	public DBUtil(String propertieLocation, int maxPoolSize, int minPoolSize) {
		// initialize the JDBC Configuration
		logger.debug("static() - begin to setup Connection Pool");

		pros = new Properties();
		try {
			FileReader propertiesFile = null;
			try {
				propertiesFile = new FileReader(propertieLocation.substring(1));
			} catch (FileNotFoundException e) {
				logger.warn("the individual jdbc properties setting is missing");
				logger.info("using the jdbc properties setting in jar");
				propertiesFile = new FileReader(DBUtil.class.getResource(propertieLocation).getFile());
			}
//			String propertieLocation = "/jdbc_marin.properties";
//			String propertieLocation = "/jdbc_pg.properties";
//			String propertieLocation = "/jdbc.properties";
			
			pros.load(propertiesFile);
			logger.info("jdbc properties:" + pros);
		} catch (IOException e) {
			e.printStackTrace();
			logger.error("e");
		}

		try {
			// setup the connection pool
			ComboPooledDataSource cpds = new ComboPooledDataSource();
			cpds.setDriverClass(pros.getProperty("driver"));

			//loads the jdbc driver
			cpds.setJdbcUrl(pros.getProperty("url"));
			cpds.setUser(pros.getProperty("username"));
			cpds.setPassword(pros.getProperty("password"));
			cpds.setMaxPoolSize(maxPoolSize);
			cpds.setMinPoolSize(minPoolSize);
			ds = cpds;
		} catch (PropertyVetoException e) {
			logger.error("static() - Could not setup Connection Pool", e); //$NON-NLS-1$
			e.printStackTrace();
		}

		logger.debug("static() - finish to setup Connection Pool");
	}

	/**
	 * initialize the BoneCP Connection Pool
	 */
	/*
	static {
	// initialize the JDBC Configuration
	logger.debug("static() - begin to setup Connection Pool");

	pros = new Properties();
	try {
		pros.load(new FileReader(DB.class.getResource("/jdbc.properties").getFile()));
	} catch (IOException e) {
		e.printStackTrace();
	}

	try {
		// load the database driver (make sure this is in your classpath!)
		Class.forName(pros.getProperty("driver"));

		// setup the connection pool
		BoneCPConfig config = new BoneCPConfig();
		config.setJdbcUrl(pros.getProperty("url")); // jdbc url specific to your database, eg jdbc:mysql://127.0.0.1/yourdb
		config.setUsername(pros.getProperty("username"));
		config.setPassword(pros.getProperty("password"));
		config.setMinConnectionsPerPartition(1);
		config.setMaxConnectionsPerPartition(5);
		config.setPartitionCount(1);

		ds = new BoneCPDataSource(config);
	} catch (ClassNotFoundException e) {
		logger.error("static() - Could not setup Connection Pool", e); //$NON-NLS-1$
		e.printStackTrace();
	}

	logger.debug("static() - finish to setup Connection Pool");
	} */

	@Override
	protected void finalize() throws Throwable {
		// shutdown connection pool.
		ds.close();
		super.finalize();
	}

	/**
	 * get Connection from Connection Pool
	 * @return
	 */
	public Connection getConn() {

		Connection conn = null;

		try {
			// fetch a connection
			conn = ds.getConnection();
		} catch (SQLException e) {
			logger.error("getConn() - Could not Connect to the Web_SQL Server", e); //$NON-NLS-1$
		}

		return conn;
	}

	/**
	 * get Connection without using Connection Pool
	 * @return
	 */
	/*
	public Connection getConn() {

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
	}*/

	public PreparedStatement getPstmt(Connection conn, String sql) {

		PreparedStatement pstmt = null;
		try {
			logger.debug("sql:" + sql);
			pstmt = conn.prepareStatement(sql);
		} catch (SQLException e) {
			logger.error("getPstmt(Connection, String)", e); //$NON-NLS-1$
		}

		return pstmt;
	}

	public PreparedStatement getPstmt(Connection conn, String sql, int autoGeneratedkey) {

		PreparedStatement pstmt = null;
		try {
			logger.debug("sql:" + sql);
			pstmt = conn.prepareStatement(sql, autoGeneratedkey);
		} catch (SQLException e) {
			logger.error("getPstmt(Connection, String, int)", e); //$NON-NLS-1$
		}

		return pstmt;
	}

	public ResultSet getRs(PreparedStatement pstmt) {

		ResultSet rs = null;
		try {
			rs = pstmt.executeQuery();
		} catch (SQLException e) {
			logger.error("getRs(PreparedStatement)", e); //$NON-NLS-1$
		}

		return rs;
	}

	public void close(Connection conn) {

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

	public void close(Statement stmt) {

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

	public void close(ResultSet rs) {

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