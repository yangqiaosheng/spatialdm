package spade.analysis.tools.db_tools;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 22-Dec-2006
 * Time: 14:32:52
 * Contains information necessary to connect to a database table and to interpret
 * the data in the table.
 */
public class DBTableDescriptor {
	/**
	 * A nice (user-given) name of this table; may be null
	 */
	public String name = null;
	/**
	 * The database format for this table ("Oracle", "Postgres", or "JDBC")
	 */
	public String format = null;
	/**
	 * The driver used to read the data
	 */
	public String driver = "oracle.jdbc.driver.OracleDriver";
	/**
	 * URL prefix for the connection to the database
	 */
	public String urlPrefix = "jdbc:oracle:thin:";
	/**
	 * Computer name
	 */
	public String computerName = "alexa";
	/**
	 * Port, e.g. 1521 for Oracle
	 */
	public int port = 1521;
	/**
	 * Database name
	 */
	public String databaseName = "ora10g";
	/**
	 * User name
	 */
	public String userName = null;
	/**
	 * Password
	 */
	public String psw = null;
	/**
	 * Table name in the database
	 */
	public String dbTableName = null;
	/**
	 * The meaning of the data in the table, e.g. points, events, or movements;
	 * see the possible meanings defined in class DataSemantics
	 */
	public int dataMeaning = DataSemantics.UNKNOWN;
	/**
	 * Identifiers of columns corresponding to the meanings specified in the class
	 * DataSemantics. The length of this array, when it is not null, is the same
	 * as the length of the corresponding array of possible meanings in the class
	 * DataSemantics, depending on the meaning of the data in the table:
	 * pointSemantics for static points (dataMeaning==STATIC_POINTS)
	 * eventSemantics for events (dataMeaning==EVENTS)
	 * movementSemantics for movements (dataMeaning==MOVEMENTS)
	 * Some values may be null when there are no columns for some of the meanings.
	 */
	public String relevantColumns[] = null;
	/**
	 * The name of the column with x-coordinate
	 */
	public String xColName = null;
	/**
	 * The index of the column with x-coordinate
	 */
	public int xColIdx = -1;
	/**
	 * The name of the column with y-coordinate
	 */
	public String yColName = null;
	/**
	 * The index of the column with y-coordinate
	 */
	public int yColIdx = -1;
	/**
	 * The name of the column with date/time
	 */
	public String timeColName = null;
	/**
	 * The index of the column with date/time
	 */
	public int timeColIdx = -1;
	/**
	 * The name of the column with entity identifiers
	 */
	public String idColName = null;
	/**
	 * The index of the column with entity identifiers
	 */
	public int idColIdx = -1;
	/**
	 * A specification of a database procedure to be applied to the table.
	 */
	public DBProcedureSpec dbProc = null;

	/**
	 * Copies the values of all its fields, except for the table names,
	 * to the given structure.
	 */
	public void copyTo(DBTableDescriptor tDescr) {
		if (tDescr == null)
			return;
		tDescr.format = format;
		tDescr.driver = driver;
		tDescr.urlPrefix = urlPrefix;
		tDescr.computerName = computerName;
		tDescr.port = port;
		tDescr.databaseName = databaseName;
		tDescr.userName = userName;
		tDescr.psw = psw;
		tDescr.dataMeaning = dataMeaning;
		tDescr.xColIdx = xColIdx;
		tDescr.xColName = xColName;
		tDescr.yColIdx = yColIdx;
		tDescr.yColName = yColName;
		tDescr.timeColIdx = timeColIdx;
		tDescr.timeColName = timeColName;
		tDescr.idColIdx = idColIdx;
		tDescr.idColName = idColName;
		if (relevantColumns != null) {
			tDescr.relevantColumns = new String[relevantColumns.length];
			for (int i = 0; i < relevantColumns.length; i++) {
				tDescr.relevantColumns[i] = relevantColumns[i];
			}
		}
	}

	/**
	 * Searches for the specified column name among the relevantColumns and
	 * returns the index if found or -1 otherwise.
	 */
	public int getRelevantColumnIndex(String columnName) {
		if (columnName == null || relevantColumns == null)
			return -1;
		for (int i = 0; i < relevantColumns.length; i++)
			if (relevantColumns[i] != null && relevantColumns[i].equals(columnName))
				return i;
		return -1;
	}

	/**
	 * If the specified column name occurs among the relevantColumns, returns
	 * the corresponding meaning (from DataSemantics), depending on the value of
	 * dataMeaning
	 */
	public String getColumnMeaning(String columnName) {
		int idx = getRelevantColumnIndex(columnName);
		if (idx < 0)
			return null;
		switch (dataMeaning) {
		case DataSemantics.STATIC_POINTS:
			return DataSemantics.pointSemantics[idx];
		case DataSemantics.EVENTS:
			return DataSemantics.eventSemantics[idx];
		case DataSemantics.MOVEMENTS:
			return DataSemantics.movementSemantics[idx];
		}
		return null;
	}
}
