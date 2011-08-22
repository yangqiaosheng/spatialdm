package db_work.database;

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: 08-Jun-2006
 * Time: 16:03:24
 * To change this template use File | Settings | File Templates.
 */
public class PostgresConnector extends JDBCConnector {

	@Override
	public String getDefaultDriver() {
		return "org.postgresql.Driver";
	}

	@Override
	public String getFormat() {
		return "Postgres";
	}

	/**
	* Returns the prefix needed to construct a database url. For Oracle this is
	* "jdbc:oracle.thin:@".
	*/
	@Override
	public String getURLPrefix() {
		return "jdbc:postgresql:";
	}

}
