package spade.analysis.tools.db_tools;

import db_work.database.JDBCConnector;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 22-Dec-2006
 * Time: 17:12:12
 */
public class DBConnectorManager {
	/**
	 * Known database formats
	 */
	public static final String DB_FORMATS[] = { "Oracle", "Postgres", "JDBC" };
	/**
	 * For each format, the class of the corresponding connector
	 */
	public static final String DB_CONNECTOR_CLASSES[] = { "db_work.database.OracleConnector", "db_work.database.PostgresConnector", "db_work.database.JDBCConnector" };

	public static String errMsg = null;

	/**
	 * Finds an appropriate connector class for the specified database format and
	 * constructs an instance of it
	 */
	public static JDBCConnector getConnector(String dbFormat) {
		errMsg = null;
		if (dbFormat == null) {
			errMsg = "Database format not specified!";
			return null;
		}
		int idx = -1;
		for (int i = 0; i < DB_FORMATS.length && idx < 0; i++)
			if (dbFormat.equalsIgnoreCase(DB_FORMATS[i])) {
				idx = i;
			}
		if (idx < 0) {
			errMsg = "Unknown database format: " + dbFormat;
			return null;
		}
		return getConnector(idx);
	}

	/**
	 * Constructs an instance of the connector class with the specified index
	 */
	public static JDBCConnector getConnector(int idx) {
		errMsg = null;
		try {
			Object obj = Class.forName(DB_CONNECTOR_CLASSES[idx]).newInstance();
			if (obj != null) {
				if (!(obj instanceof JDBCConnector)) { //hardly possible...
					errMsg = "The class " + DB_CONNECTOR_CLASSES[idx] + " is not a subclass of " + "database.JDBCConnector";
					return null;
				}
				return (JDBCConnector) obj;
			}
		} catch (Exception e) {
			errMsg = e.toString();
		}
		return null;
	}
}
