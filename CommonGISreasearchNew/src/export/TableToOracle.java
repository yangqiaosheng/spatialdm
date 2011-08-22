package export;

/**
* This is a class for writing the contents of a Descartes table into Oracle database
*/
public class TableToOracle extends TableToJDBC {
	/**
	* Returns the name of the output format suitable for showing to the user.
	* For example, "Shape (ArcView)", "csv (comma-separated values)"
	*/
	@Override
	public String getFormatName() {
		return "Oracle database";
	}

	/**
	* Returns a default driver. For JDBC this is sun.jdbc.odbc.JdbcOdbcDriver.
	* The method may be redefined in descendants reading from different databases
	*/
	@Override
	public String getDefaultDriver() {
		return "oracle.jdbc.driver.OracleDriver";
	}

	/**
	* Returns the format accepted by this reader (in this case JDBC).
	* Subclasses intended for different databases may override this method.
	*/
	@Override
	public String getFormat() {
		return "Oracle";
	}

	/**
	* Returns the prefix needed to construct a database url. For JDBC this is
	* "jdbc:odbc:". This may be overridden in subclasses reading from different
	* databases.
	*/
	@Override
	public String getURLPrefix() {
		return "jdbc:oracle:thin:";
	}

	/**
	 * Returns maximal length of SQL identifier
	 */
	@Override
	public int getMaxSQLidLength() {
		return 32;
	}

	/**
	 * Returns maximal length of SQL identifier
	 */
	@Override
	public int getMaxTableNameLength() {
		return 32;
	}
}
