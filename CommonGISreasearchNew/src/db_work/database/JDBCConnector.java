package db_work.database;

import java.awt.Frame;
import java.awt.List;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;

import spade.lib.basicwin.CManager;
import spade.lib.basicwin.OKDialog;
import spade.lib.util.StringUtil;
import data_load.read_db.ColumnSelectPanel;
import data_load.read_db.DBConnectPanel;
import data_load.read_db.DBViewPanel;
import db_work.data_descr.ColumnDescriptor;
import db_work.data_descr.ColumnDescriptorDate;
import db_work.data_descr.ColumnDescriptorNum;
import db_work.data_descr.ColumnDescriptorQual;
import db_work.data_descr.TableDescriptor;

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: 24-Jan-2006
 * Time: 17:32:01
 * To change this template use File | Settings | File Templates.
 */
public class JDBCConnector {

	/**
	* The table loaded
	* Should be different in comparison to CommonGIS
	*/
	//protected DataTable dtab=null;

	// main window
	protected Frame frame = null;

	public void setFrame(Frame frame) {
		this.frame = frame;
	}

	/**
	* Metadata about the database
	*/
	protected DatabaseMetaData meta = null;

	/**
	* Database connection
	*/
	protected Connection connection = null;

	public Connection getConnection() {
		return connection;
	}

	/**
	* The specification of the data tableName
	*/
	protected DataSourceSpecification ds = null;
	/**
	 * Computer name
	 */
	protected String computerName = "localhost";//"plan-b.iais.fraunhofer.de"; //"stockholm";  sprod-b
	/**
	 * Database name
	 */
	protected String databaseName = "oracle11g"; //"stockhol";  //"gennady"; sprodb
	/**
	 * Port, e.g. 5432
	 */
	protected int port = 5432;
	/**
	 * Table name in the database
	 */
	public String dbTableName = null;
	/**
	 * Error message
	 */
	public String err = null;

	public String getErrorMessage() {
		return err;
	}

	/**
	* Returns the format accepted by this reader (in this case JDBC).
	* Subclasses intended for different databases may override this method.
	*/
	public String getFormat() {
		return "JDBC";
	}

	/**
	* Returns a default driver. For JDBC this is sun.jdbc.odbc.JdbcOdbcDriver.
	* The method may be redefined in descendants reading from different databases
	*/
	public String getDefaultDriver() {
		return "sun.jdbc.odbc.JdbcOdbcDriver";
	}

	/**
	* Returns the prefix needed to construct a database url. For JDBC this is
	* "jdbc:odbc:". This may be overridden in subclasses reading from different
	* databases.
	*/
	public String getURLPrefix() {
		return "jdbc:odbc:";
	}

	public String getURL() {
		return (ds == null) ? "" : ds.url;
	}

	public String getUserName() {
		return (ds == null) ? "" : ds.user;
	}

	public void setUserName(String name) {
		if (ds == null) {
			ds = new DataSourceSpecification();
		}
		ds.user = name;
	}

	public String getPSW() {
		if (ds == null)
			return null;
		return ds.password;
	}

	public void setPSW(String str) {
		if (ds == null) {
			ds = new DataSourceSpecification();
		}
		ds.password = str;
	}

	public String getDriver() {
		if (ds == null || ds.driver == null)
			return getDefaultDriver();
		return ds.driver;
	}

	public String getComputerName() {
		return computerName;
	}

	public void setComputerName(String name) {
		computerName = name;
	}

	public String getDatabaseName() {
		return databaseName;
	}

	public void setDatabaseName(String name) {
		databaseName = name;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int portN) {
		port = portN;
	}

	public String getDBTableName() {
		return dbTableName;
	}

	public void setDBTableName(String name) {
		dbTableName = name;
	}

	public int getNumOfTableDescriptors() {
		return (ds == null || ds.tableDescriptors == null) ? 0 : ds.tableDescriptors.size();
	}

	public TableDescriptor getTableDescriptor(int tn) {
		return (ds == null || ds.tableDescriptors == null || ds.tableDescriptors.size() <= tn) ? null : (TableDescriptor) ds.tableDescriptors.elementAt(tn);
	}

	public String getTableName(int tn) {
		TableDescriptor td = getTableDescriptor(tn);
		return (td == null) ? "" : td.tableName;
	}

	public int getNcols(int tn) {
		TableDescriptor td = getTableDescriptor(tn);
		return (td == null) ? -1 : td.columns.size();
	}

	public int getNrows(int tn) {
		TableDescriptor td = getTableDescriptor(tn);
		return (td == null) ? -1 : td.dbRowCount;
	}

	/**
	 * MSSQLReader overrides this method because it needs a non standard
	 * URL.
	 */
	public String getDatabaseURL(DBConnectPanel dbConnect) {
		return dbConnect.getDatabaseURL();
	}

	public String makeDatabaseURL() {
		if (databaseName == null || computerName == null)
			return null;
		String urlPrefix = getURLPrefix();
		if (urlPrefix == null)
			return new String(databaseName);
		String url = "";
		url = urlPrefix;
		if (urlPrefix.contains("oracle")) {
			if (computerName != null) {
				url += "@" + computerName + ":";
			}
			if (port > 0) {
				url += String.valueOf(port) + ":";
			}
			return url + databaseName;
		}
		if (urlPrefix.contains("postgres")) {
			if (computerName != null) {
				url += "//" + computerName + "/";
			}
			return url + databaseName;
		}
		if (computerName != null) {
			url += "@" + computerName + ":";
		}
		if (port > 0) {
			url += String.valueOf(port) + ":";
		}
		return url + databaseName;
	}

	/**
	* Constructs a special query that might be used to retrieve from the database
	* only the list of tables belonging to user's view.
	* By default, returns null. May be overridden in subclasses.
	*/
	public String getOnlyMyTablesQuery() {
		return null;
	}

	/**
	* Gets a reference to a frame (needed for construction of dialogs).
	* First tries to get the main frame from the system UI. If this fails,
	* uses an invisible "dummy" frame
	*/
	protected Frame getFrame() {
		return (frame == null) ? CManager.getAnyFrame() : frame;
	}

	/**
	* Opens a database connection for loading data. All the necessary information
	* has to be specified in the internal variable spec (an instance of
	* DataSourceSpec). If this information is absent, it is asked from the user
	* (in the case when mayAskUser is true). Returns true if successfully
	* connected.
	*/

	public boolean openConnection(boolean mayAskUser) {
		err = null;
		if (connection != null)
			return true;
		if ((ds == null || ds.url == null) && dbTableName != null) {
			String url = makeDatabaseURL();
			if (url != null) {
				if (ds == null) {
					ds = new DataSourceSpecification();
				}
				ds.url = url;
				if (ds.driver == null) {
					ds.driver = getDefaultDriver();
				}
			}
		}
		if (ds == null || ds.url == null) {
			if (!mayAskUser) {
				err = "No database URL specified!";
				return false;
			}
			if (ds == null) {
				ds = new DataSourceSpecification();
			}
			if (ds.driver == null) {
				ds.driver = getDefaultDriver();
			}
			DBConnectPanelAdvanced dbConnectPanel = new DBConnectPanelAdvanced(false);
			dbConnectPanel.setDriver(ds.driver);
			dbConnectPanel.setURLPrefix(getURLPrefix());
			//
			dbConnectPanel.setComputer(computerName);
			dbConnectPanel.setDatabase(databaseName);
			dbConnectPanel.setPort(port);
			if (dbTableName != null) {
				dbConnectPanel.setTable(dbTableName);
			}
			dbConnectPanel.setUser("gennady_");
			dbConnectPanel.setPassword("gennady");
			System.out.println("set user gennady");
			//
			OKDialog okd = new OKDialog(getFrame(), "Connect to database", true);
			okd.addContent(dbConnectPanel);
			okd.show();
			if (okd.wasCancelled())
				return false;
			computerName = dbConnectPanel.getComputer();
			databaseName = dbConnectPanel.getDatabase();
			ds.driver = dbConnectPanel.getDriver();
			ds.url = getDatabaseURL(dbConnectPanel);
			ds.user = dbConnectPanel.getUser();
			ds.password = dbConnectPanel.getPassword();

			ds.tableDescriptors = new Vector(5, 5);
			TableDescriptor td = new TableDescriptor();
			ds.tableDescriptors.addElement(td);
			td.tableName = dbConnectPanel.getTable();
			connection = dbConnectPanel.getConnection();
			System.out.println("Connected to " + ds.url);
			if (td.tableName == null) { //allow the user to select the table to load
				DBViewPanel dbView = new DBViewPanel(connection, getOnlyMyTablesQuery());
				if (!dbView.hasValidContent()) {
					err = dbView.getErrorMessage();
					System.out.println(err);
					closeConnection();
					return false;
				}
				okd = new OKDialog(getFrame(), "Select a table", true);
				okd.addContent(dbView);
				okd.show();
				if (okd.wasCancelled()) {
					closeConnection();
					err = "No table selected!";
					return false;
				}
				td.tableName = dbView.getTableName();
				meta = dbView.getMetaData();
			}
			if (td.tableName == null) {
				ds.tableDescriptors = null;
			}
		} else {
			try {
				connection = DriverManager.getConnection(ds.url, ds.user, ds.password);
			} catch (SQLException se) {
				//following text:"Failed to connect to "
				err = "Failed to connect to " + ds.url + ": " + se.toString();
				return false;
			}
			System.out.println("Connected to " + ds.url);
			ds.tableDescriptors = new Vector(5, 5);
			TableDescriptor td = new TableDescriptor();
			ds.tableDescriptors.addElement(td);
			td.tableName = dbTableName;
		}
		if (ds == null || ds.url == null || ds.tableDescriptors == null) {
			closeConnection();
			return false;
		}
		if (connection == null) {
			if (ds.driver == null) {
				ds.driver = getDefaultDriver();
			}
			try {
				Class.forName(ds.driver);
			} catch (Exception e) {
				err = "Failed to load the " + ds.driver + ": " + e.toString();
				System.out.println(err);
				return false;
			}
			try {
				connection = DriverManager.getConnection(ds.url, ds.user, ds.password);
			} catch (SQLException se) {
				err = "Failed to connect to " + ds.url + ": " + se.toString() + " driver " + ds.driver;
				System.out.println(err);
				return false;
			}
		}
		return connection != null;
	}

	public boolean reOpenConnection() {
		if (connection != null)
			return true;
		if (ds == null || ds.url == null)
			return false;
		if (ds.driver == null) {
			ds.driver = getDefaultDriver();
		}
		try {
			Class.forName(ds.driver);
		} catch (Exception e) {
			err = "Failed to connect to " + ds.driver + ": " + e.toString();
			System.out.println(err);
			return false;
		}
		try {
			connection = DriverManager.getConnection(ds.url, ds.user, ds.password);
		} catch (SQLException se) {
			err = "Failed to connect to " + ds.url + ": " + se.toString() + " driver " + ds.driver;
			System.out.println(err);
			return false;
		}
		return connection != null;
	}

	public void closeConnection() {
		if (connection != null) {
			try {
				connection.close();
			} catch (SQLException e) {
			}
			connection = null;
		}
		meta = null;
	}

	protected String makeColumnListString(int tn) {
		TableDescriptor td = (TableDescriptor) ds.tableDescriptors.elementAt(tn);
		if (td.getNColumns() == 0)
			return "*";
		Vector v = (Vector) td.columns.clone();
		String q = "\"" + (String) v.elementAt(0) + "\"";
		for (int i = 1; i < v.size(); i++) {
			q += ", \"" + (String) v.elementAt(i) + "\"";
		}
		return q;
	}

	protected String makeColumnListSQL(int tn) {
		return "SELECT " + makeColumnListString(tn) + " FROM " + ((TableDescriptor) ds.tableDescriptors.elementAt(tn));
	}

	/**
	* Makes a query to the database in order to determine the number of rows in
	* the table. Returns true if this was actually done. The number of rows is
	* stored in spec.dbRowCount. If the number of rows was
	* already set (i.e. is more than zero), returns true immediately.
	*/
	public boolean retrieveRowCount(int tn) {
		if (connection == null)
			return false;
		TableDescriptor td = ds.getTableDescriptor(tn);
		if (td.dbRowCount > 0)
			return true;
		try {
			String sqls = SQLStatements.getSQL(1, td.tableName); //"SELECT COUNT(*) FROM "+ds.tableName;
			System.out.print("* <" + sqls + ">");
			Statement statement = connection.createStatement();
			ResultSet result = statement.executeQuery(sqls);
			result.next();
			td.dbRowCount = result.getInt(1);
			System.out.println(" " + td.dbRowCount);
		} catch (SQLException se) {
			System.out.println("Failed to get row count for " + td.tableName + ": " + se.toString());
		}
		return td.dbRowCount > 0;
	}

	/**
	* Retrieves the list of column names from the database and fills the vector
	* columns with these names. Vector must be initialized
	* before calling this method.
	*/
	public Vector getColumnsOfTable(Vector toFill, String tableName) {
		if (connection == null)
			return null;
		Vector v = (toFill == null) ? new Vector(20, 20) : toFill;
		try {
			Statement statement = connection.createStatement();
			statement.setMaxRows(2);
			String sqlString = "SELECT * FROM " + tableName + " where rownum<2"; //SQLStatements.getSQL(0,tableName); // +" "+TABLE_ALIAS;
			System.out.println("* <" + sqlString + ">");
			ResultSet result = statement.executeQuery(sqlString);
			ResultSetMetaData md = result.getMetaData();
			int ncols = md.getColumnCount();
			for (int i = 1; i <= ncols; i++) {
				v.addElement(md.getColumnName(i));
			}
			result.close();
			statement.close();
		} catch (SQLException se) {
			System.out.println("Cannot get the list" + se.toString());
		}
		return v;
	}

	protected boolean getColumns(int tn, Vector columns) {
		if (connection == null || columns == null)
			return false;
		TableDescriptor td = (TableDescriptor) ds.tableDescriptors.elementAt(tn);
		columns = getColumnsOfTable(columns, td.tableName);
		if (columns.size() < 1)
			return false;
		return true;
	}

	public Vector /*ColumnDescriptor[]*/getColumnDescriptors(TableDescriptor td, Vector colnames) {
		if (colnames == null || colnames.size() == 0)
			return null;
		Vector cds = new Vector(colnames.size(), 1);
		int types[] = new int[colnames.size()];
		String stypes[] = new String[colnames.size()];
		try {
			// get types of all columns
			Statement statement = connection.createStatement();
			statement.setMaxRows(2);
			String sqlString = "SELECT " + (String) colnames.elementAt(0);
			for (int i = 1; i < colnames.size(); i++) {
				sqlString += ", " + (String) colnames.elementAt(i);
			}
			sqlString += "\nFROM " + td.tableName;
			System.out.println("* <" + sqlString + ">");
			ResultSet result = statement.executeQuery(sqlString);
			ResultSetMetaData md = result.getMetaData();
			for (int i = 0; i < types.length; i++) {
				types[i] = md.getColumnType(1 + i);
				stypes[i] = md.getColumnTypeName(1 + i);
			}
			result.close();
			statement.close();
			// create column descriptors
			for (int i = 0; i < colnames.size(); i++) {
				ColumnDescriptor cd = null;
				switch (types[i]) {
				case java.sql.Types.CHAR:
				case java.sql.Types.VARCHAR:
					cd = new ColumnDescriptorQual();
					break;
				case java.sql.Types.DATE:
					cd = new ColumnDescriptorDate();
					break;
				case java.sql.Types.NUMERIC:
				case java.sql.Types.DECIMAL:
				case java.sql.Types.BIGINT:
				case java.sql.Types.DOUBLE:
				case java.sql.Types.FLOAT:
				case java.sql.Types.REAL:
				case java.sql.Types.INTEGER:
					cd = new ColumnDescriptorNum();
					break;
				default:
					cd = new ColumnDescriptor();
					break;
				}
				cd.name = (String) colnames.elementAt(i);
				cd.type = stypes[i];
				cds.addElement(cd);
			}
			// get basic statistics
			// do we need this here?
		} catch (SQLException se) {
			System.out.println("Cannot load column descriptors: " + se.toString());
		}
		return cds;
	}

	public boolean getStatsForColumnDescriptors(TableDescriptor td, ColumnDescriptor cds[]) {
		String sqlString = "select ";
		for (int i = 0; i < cds.length; i++) {
			sqlString += " min(" + cds[i].name + "), " + " max(" + cds[i].name + ")" + ((i < cds.length - 1) ? ", " : "");
		}
		sqlString += "\nfrom " + td.tableName;
		System.out.println("* <" + sqlString + ">");
		try {
			long tStart = System.currentTimeMillis();
			Statement statement = connection.createStatement();
			ResultSet result = statement.executeQuery(sqlString);
			result = statement.executeQuery(sqlString);
			result.next();
			for (int i = 0; i < cds.length; i++)
				if (cds[i] instanceof ColumnDescriptorDate) {
					((ColumnDescriptorDate) cds[i]).min = result.getDate(2 * i + 1) + " " + result.getTime(2 * i + 1);
					((ColumnDescriptorDate) cds[i]).max = result.getDate(2 * i + 2) + " " + result.getTime(2 * i + 2);
				} else if (cds[i] instanceof ColumnDescriptorNum) { // ColumnDescriptorNum
					((ColumnDescriptorNum) cds[i]).min = result.getFloat(2 * i + 1);
					((ColumnDescriptorNum) cds[i]).max = result.getFloat(2 * i + 2);
				}
			result.close();
			statement.close();
			long tEnd = System.currentTimeMillis() - tStart;
			System.out.println("* Ready. Elapsed time " + StringUtil.floatToStr(tEnd / 1000f, 3) + " (s)");
		} catch (SQLException se) {
			System.out.println("Cannot load statistics into column descriptor: " + se.toString());
			return true;
		}
		return false;
	}

	public ColumnDescriptor getColumnDescriptor(TableDescriptor td, String colname) {
		ColumnDescriptor cd = null;

		try {

			// get column type
			Statement statement = connection.createStatement();
			statement.setMaxRows(2);
			String st[] = new String[2];
			st[0] = colname;
			st[1] = td.tableName;
			String sqlString = SQLStatements.getSQL(2, st);
			System.out.println("* <" + sqlString + ">");
			ResultSet result = statement.executeQuery(sqlString);
			ResultSetMetaData md = result.getMetaData();
			//int ncols=md.getColumnCount();
			//if (ncols<1) return null;
			int type = md.getColumnType(1);
			String stype = md.getColumnTypeName(1);
			result.close();
			statement.close();

			// get basic statistics
			switch (type) {
			case java.sql.Types.CHAR:
			case java.sql.Types.VARCHAR:
				cd = new ColumnDescriptorQual();
				break;
			case java.sql.Types.DATE:
				cd = new ColumnDescriptorDate();
				ColumnDescriptorDate cdd = (ColumnDescriptorDate) cd;
				statement = connection.createStatement();
				st = new String[3];
				st[0] = colname;
				st[1] = colname;
				st[2] = td.tableName;
				sqlString = SQLStatements.getSQL(17, st); // select min,max ...
				System.out.println("** <" + sqlString + ">");
				result = statement.executeQuery(sqlString);
				result.next();
				//System.out.println("** min=<"+result.getDate(1)+" "+result.getTime(1)+">");
				//System.out.println("** max=<"+result.getDate(2)+" "+result.getTime(2)+">");
				cdd.min = result.getDate(1) + " " + result.getTime(1);
				cdd.max = result.getDate(2) + " " + result.getTime(2);
				result.close();
				statement.close();
				break;
			case java.sql.Types.NUMERIC:
			case java.sql.Types.DECIMAL:
			case java.sql.Types.BIGINT:
			case java.sql.Types.DOUBLE:
			case java.sql.Types.FLOAT:
			case java.sql.Types.REAL:
			case java.sql.Types.INTEGER:
				cd = new ColumnDescriptorNum();
				ColumnDescriptorNum cdm = (ColumnDescriptorNum) cd;

				statement = connection.createStatement();
				st = new String[4];
				st[0] = colname;
				st[1] = colname;
				st[2] = colname;
				st[3] = td.tableName;
				sqlString = SQLStatements.getSQL(6, st); // select min,avg,max ...
				System.out.println("** <" + sqlString + ">");
				result = statement.executeQuery(sqlString);
				result.next();
				cdm.min = result.getFloat(1);
				cdm.avg = result.getFloat(2);
				cdm.max = result.getFloat(3);
				result.close();
				statement.close();
				/*
				st=new String[3]; st[0]=td.tableName; st[1]=colname; st[2]=colname;
				PreparedStatement ps=connection.prepareStatement(SQLStatements.getSQL(8,st));
				cd.nBins=10;
				cd.counts=new int[cd.nBins];
				float f=0f, delta=cdm.max-cdm.min;
				for (int i=0; i<cd.nBins; i++) {
				  float prevf=f;
				  f=cdm.min+(i+1)*delta/cd.nBins;
				  ps.setFloat(1, (i==0) ? cdm.min-delta/10f : prevf);
				  ps.setFloat(2, (i==cd.nBins-1) ? cdm.max+delta/10f : f);
				  result=ps.executeQuery();
				  result.next();
				  cd.counts[i]=result.getInt(1);
				  result.close();
				}
				ps.close();
				*/
				break;
			default:
				cd = new ColumnDescriptor();
			}
			cd.name = colname;
			cd.type = stype;

		} catch (SQLException se) {
			System.out.println("Cannot load column descriptor: " + se.toString());
		}
		return cd;
	}

	public boolean getColumnDescriptorDetails(TableDescriptor td, ColumnDescriptor cd) {
		try {
			long tStart = System.currentTimeMillis();
			Statement statement = connection.createStatement();
			String st[] = new String[2];
			st[1] = cd.name;
			st[0] = td.tableName;
			String sqlString = SQLStatements.getSQL(9, st); // count nulls ...
			System.out.println("** <" + sqlString + ">");
			ResultSet result = statement.executeQuery(sqlString);
			result.next();
			cd.nNulls = result.getInt(1);
			result.close();
			statement.close();
			long tEnd = System.currentTimeMillis() - tStart;
			System.out.println("* Ready. Elapsed time " + StringUtil.floatToStr(tEnd / 1000f, 3) + " (s)");

			if (cd instanceof ColumnDescriptorNum && ((ColumnDescriptorNum) cd).max <= ((ColumnDescriptorNum) cd).min) {
				cd.nUniqueValues = (Float.isNaN(((ColumnDescriptorNum) cd).min)) ? 0 : 1;
			} else {
				tStart = System.currentTimeMillis();
				statement = connection.createStatement();
				st = new String[2];
				st[0] = cd.name;
				st[1] = td.tableName;
				sqlString = SQLStatements.getSQL(10, st); // count distinct ...
				System.out.println("** <" + sqlString + ">");
				result = statement.executeQuery(sqlString);
				result.next();
				cd.nUniqueValues = result.getInt(1);
				result.close();
				statement.close();
				tEnd = System.currentTimeMillis() - tStart;
				System.out.println("* Ready. Elapsed time " + StringUtil.floatToStr(tEnd / 1000f, 3) + " (s)");
			}

			cd.numsDefined = true;
			/* // alternative method
			statement=connection.createStatement();
			st=new String[2]; st[0]=colname; st[1]=td.tableName;
			sqlString=database.SQLStatements.getSQL(5,st); // select distinct ...
			System.out.println("** <"+sqlString+">");
			result=statement.executeQuery(sqlString);
			cd.nUniqueValues=0;
			while (result.next()) {
			  result.getString(1);
			  if (result.wasNull())
			    cd.nNulls++;
			  else
			    cd.nUniqueValues++;
			}
			result.close();
			statement.close();
			*/
		} catch (SQLException se) {
			System.out.println("Cannot get column detailst: " + se.toString());
			return false;
		}
		return true;
	}

	public void dropTmpTable(String tmpName) {
		String sqlString = "";
		boolean dropped = false;
		try {
			Statement statement = connection.createStatement();
			sqlString = SQLStatements.getSQL(11, tmpName); // drop table ...
			statement.execute(sqlString);
			statement.close();
			dropped = true;
		} catch (SQLException se) {
			System.out.println("** <" + sqlString + ">");
			System.out.println("Cannot drop table: " + se.toString());
		}
		if (!dropped) {
			try {
				Statement statement = connection.createStatement();
				sqlString = "drop view " + tmpName; // drop table ...
				statement.execute(sqlString);
				statement.close();
			} catch (SQLException se) {
				System.out.println("** <" + sqlString + ">");
				System.out.println("Cannot drop view: " + se.toString());
			}
		}
	}

	protected static int L = 128; // Limit for number of parameters; in ORACLE is 128

	public int getColumnDescriptorBinCounts(TableDescriptor td, ColumnDescriptorNum cdn, int nInt, Vector opers, Vector attrs) {
		if (opers == null || opers.size() == 0)
			return 0;
		int N = opers.size();
		if (nInt <= L) {
			try {
				String values[] = new String[nInt];
				Statement statement = connection.createStatement();
				String st[] = new String[2];
				st[0] = "";
				st[1] = "";
				//st[0]=", COUNT(*)"; st[1]="";
				for (int n = 0; n < N; n++) {
					String s = (attrs.elementAt(n) == null) ? "*" : (String) attrs.elementAt(n);
					String op = (String) opers.elementAt(n);
					if (op.equals("MAX-MIN")) {
						st[0] += ", MAX(" + s + ")-MIN(" + s + ")";
					} else {
						st[0] += ", " + op + "(" + s + ")";
					}
					if (attrs.elementAt(n) != null && !((String) attrs.elementAt(n)).startsWith("*") && n == attrs.indexOf(attrs.elementAt(n))) {
						st[1] += ((String) attrs.elementAt(n)) + ",\n";
						//st[0]=", MEDIAN(AREA_KM)"; st[1]="AREA_KM,\n";
					}
				}
				String sqlString = SQLStatements.getSQL(12, st); // count ... group by ...
				float prevf = cdn.min;
				for (int i = 0; i < nInt - 1; i++) {
					float f = cdn.min + (i + 1) * (cdn.max - cdn.min) / nInt;
					st = new String[3];
					st[0] = cdn.name;
					st[1] = Float.valueOf(f).toString();
					st[2] = Integer.valueOf(1 + i).toString();
					sqlString += SQLStatements.getSQL(13, st);
					values[i] = ((i == 0) ? "[" : "(") + StringUtil.floatToStr(prevf, cdn.min, cdn.max) + ".." + StringUtil.floatToStr(f, cdn.min, cdn.max) + "]";
					prevf = f;
				}
				values[nInt - 1] = "(" + StringUtil.floatToStr(prevf, cdn.min, cdn.max) + ".." + StringUtil.floatToStr(cdn.max, cdn.min, cdn.max) + "]";
				st = new String[2];
				st[0] = Integer.valueOf(nInt).toString();
				st[1] = td.tableName;
				sqlString += SQLStatements.getSQL(14, st);
				sqlString += SQLStatements.getSQL(15, cdn.name);
				System.out.println("** <" + sqlString + ">");
				ResultSet result = statement.executeQuery(sqlString);
				float[][] f = new float[N][];
				for (int n = 0; n < N; n++) {
					f[n] = new float[nInt];
					for (int i = 0; i < nInt; i++) {
						f[n][i] = 0f;
					}
				}
				while (result.next()) {
					int i = result.getInt(1) - 1;
					if (result.wasNull() || i < 0 || i >= nInt) {
						break;
					}
					for (int n = 0; n < N; n++) {
						float fv = result.getFloat(2 + n);
						f[n][i] = ((result.wasNull() || Float.isNaN(fv))) ? 0f : fv;
					}
				}
				statement.close();
				for (int n = 0; n < N; n++) {
					cdn.addAggregation(nInt, (String) opers.elementAt(n), (String) attrs.elementAt(n), f[n], values);
				}
			} catch (SQLException se) {
				System.out.println("Cannot get bin counts: " + se.toString());
				return 0;
			}
		} else {
			try {
				Statement statement = connection.createStatement();
				//String sqlString=database.SQLStatements.getSQL(16); // create table ...
				// ... to be done later .. if needed
			} catch (SQLException se) {
				System.out.println("Cannot get bin counts: " + se.toString());
				return 0;
			}
		}
		return N;
	}

	public int getColumnDescriptorBinCounts(TableDescriptor td, ColumnDescriptor cd, Vector opers, Vector attrs) {
		if (opers == null || opers.size() == 0)
			return 0;
		int N = opers.size(), nInt = cd.nUniqueValues;
		try {
			Statement statement = connection.createStatement();
			String ss = "";
			for (int n = 0; n < N; n++) {
				String s = (attrs.elementAt(n) == null) ? "*" : (String) attrs.elementAt(n);
				String op = (String) opers.elementAt(n);
				if (op.equals("MAX-MIN")) {
					ss += ", MAX(" + s + ")-MIN(" + s + ")";
				} else {
					ss += ", " + op + "(" + s + ")";
				}
			}
			String st[] = new String[6];
			st[0] = cd.name;
			st[1] = ss;
			st[2] = td.tableName;
			st[3] = cd.name;
			st[4] = cd.name;
			st[5] = cd.name;
			String sqlString = SQLStatements.getSQL(16, st); // count ... group by
			System.out.println("** <" + sqlString + ">");
			ResultSet result = statement.executeQuery(sqlString);
			float[][] f = new float[N][];
			for (int n = 0; n < N; n++) {
				f[n] = new float[nInt];
				for (int i = 0; i < nInt; i++) {
					f[n][i] = 0f;
				}
			}
			String values[] = new String[nInt];
			for (int i = 0; i < nInt; i++) {
				values[i] = null;
			}
			int k = -1;
			while (result.next()) {
				String st1 = result.getString(1);
				if (result.wasNull()) {
					break;
				}
				k++;
				values[k] = st1.trim();
				for (int n = 0; n < N; n++) {
					float fv = result.getFloat(2 + n);
					f[n][k] = ((result.wasNull() || Float.isNaN(fv))) ? 0f : fv;
				}
			}
			statement.close();
			for (int n = 0; n < N; n++) {
				cd.addAggregation(nInt, (String) opers.elementAt(n), (String) attrs.elementAt(n), f[n], values);
			}
		} catch (SQLException se) {
			System.out.println("Cannot get bin counts: " + se.toString());
			return 0;
		}
		return N;
	}

	public Vector get2dBinCounts(TableDescriptor td, Vector referrers, Vector breaks, Vector opers, Vector attrs) {
		if (referrers == null || referrers.size() != 2 || breaks == null || opers == null || attrs == null)
			return null;
		int N = opers.size();
		try {
			Statement statement = connection.createStatement();
			String st[] = new String[1];
			if (breaks.elementAt(0) != null || breaks.elementAt(1) != null) {
				st[0] = "V1,V2";
			} else {
				st[0] = (String) referrers.elementAt(0) + ", " + (String) referrers.elementAt(1);
			}
			for (int n = 0; n < N; n++) {
				String s = (attrs.elementAt(n) == null) ? "*" : (String) attrs.elementAt(n);
				String op = (String) opers.elementAt(n);
				if (op.equals("MAX-MIN")) {
					st[0] += ", MAX(" + s + ")-MIN(" + s + ")";
				} else {
					st[0] += ", " + op + "(" + s + ")";
				}
			}
			String sqlString = SQLStatements.getSQL(18, st);
			if ((breaks.elementAt(0) == null) && (breaks.elementAt(1) == null)) {
				st[0] = td.tableName;
				sqlString += SQLStatements.getSQL(24, st);
				st[0] = (String) referrers.elementAt(0) + "," + (String) referrers.elementAt(1);
				sqlString += SQLStatements.getSQL(25, st);
			} else if ((breaks.elementAt(0) != null) && (breaks.elementAt(1) != null)) {
				st[0] = "";
				for (int n = 0; n < N; n++)
					if (attrs.elementAt(n) != null && !((String) attrs.elementAt(n)).startsWith("*") && n == attrs.indexOf(attrs.elementAt(n))) {
						st[0] += ((String) attrs.elementAt(n)) + ",";
					}
				sqlString += SQLStatements.getSQL(19, st);
				for (int j = 0; j < 2; j++) {
					sqlString += SQLStatements.getSQL(20);
					float f[] = (float[]) breaks.elementAt(j);
					st = new String[3];
					st[0] = (String) referrers.elementAt(j);
					for (int k = 0; k < f.length; k++) {
						st[1] = Float.valueOf(f[k]).toString();
						st[2] = Integer.valueOf(1 + k).toString();
						sqlString += SQLStatements.getSQL(21, st);
					}
					st = new String[2];
					st[0] = Integer.valueOf(1 + f.length).toString();
					st[1] = "V" + (j + 1);
					sqlString += SQLStatements.getSQL(22, st);
					if (j == 0) {
						sqlString += ",\n";
					}
				}
				st = new String[3];
				st[0] = td.tableName;
				st[1] = (String) referrers.elementAt(0);
				st[2] = (String) referrers.elementAt(1);
				sqlString += SQLStatements.getSQL(23, st);
				st = new String[1];
				st[0] = "V1,V2";
				sqlString += SQLStatements.getSQL(25, st);
			} else { // either breaks.elementAt(0) or breaks.elementAt(0) == null
				st[0] = "";
				for (int n = 0; n < N; n++)
					if (attrs.elementAt(n) != null && !((String) attrs.elementAt(n)).startsWith("*") && n == attrs.indexOf(attrs.elementAt(n))) {
						st[0] += ((String) attrs.elementAt(n)) + ",";
					}
				for (int j = 0; j < 2; j++)
					if (breaks.elementAt(j) == null) {
						st[0] += ((String) referrers.elementAt(j)) + " AS V" + (1 + j) + ",";
					}
				sqlString += SQLStatements.getSQL(19, st);
				for (int j = 0; j < 2; j++)
					if (breaks.elementAt(j) != null) {
						sqlString += SQLStatements.getSQL(20);
						float f[] = (float[]) breaks.elementAt(j);
						st = new String[3];
						st[0] = (String) referrers.elementAt(j);
						for (int k = 0; k < f.length; k++) {
							st[1] = Float.valueOf(f[k]).toString();
							st[2] = Integer.valueOf(1 + k).toString();
							sqlString += SQLStatements.getSQL(21, st);
						}
						st = new String[2];
						st[0] = Integer.valueOf(1 + f.length).toString();
						st[1] = "V" + (j + 1);
						sqlString += SQLStatements.getSQL(22, st);
					}
				st = new String[3];
				st[0] = td.tableName;
				st[1] = (String) referrers.elementAt(0);
				st[2] = (String) referrers.elementAt(1);
				sqlString += SQLStatements.getSQL(23, st);
				st = new String[1];
				st[0] = "V1,V2";
				sqlString += SQLStatements.getSQL(25, st);
			}
			System.out.println("** <" + sqlString + ">");
			Vector valsRef1 = new Vector(50, 50), valsRef2 = new Vector(50, 50), vals = new Vector(50, 50);
			if (breaks.elementAt(0) != null) {
				float f[] = (float[]) breaks.elementAt(0);
				for (int i = 1; i <= f.length + 1; i++) {
					valsRef1.addElement(Integer.valueOf(i).toString());
				}
			}
			if (breaks.elementAt(1) != null) {
				float f[] = (float[]) breaks.elementAt(1);
				for (int i = 1; i <= f.length + 1; i++) {
					valsRef2.addElement(Integer.valueOf(i).toString());
				}
			}
			ResultSet result = statement.executeQuery(sqlString);
			while (result.next()) {
				//System.out.println("* "+result.getString(1)+", "+result.getString(2)+", "+result.getInt(3));
				String v1 = result.getString(1), v2 = result.getString(2);
				int idx1 = valsRef1.indexOf(v1);
				if (idx1 < 0) {
					valsRef1.addElement(v1);
					idx1 = valsRef1.size() - 1;
				}
				int idx2 = valsRef2.indexOf(v2);
				if (idx2 < 0) {
					valsRef2.addElement(v2);
					idx2 = valsRef2.size() - 1;
				}
				float f[] = new float[2 + opers.size()];
				f[0] = idx1;
				f[1] = idx2;
				for (int i = 0; i < opers.size(); i++) {
					f[2 + i] = result.getFloat(3 + i);
				}
				vals.addElement(f);
			}
			statement.close();
			int l01 = valsRef1.size(), l02 = valsRef2.size(), l03 = opers.size();
			float ff[][][] = new float[l03][][];
			for (int l = 0; l < l03; l++) {
				ff[l] = new float[l02][];
				for (int ll = 0; ll < l02; ll++) {
					ff[l][ll] = new float[l01];
					//if (opers.elementAt(l).equals("COUNT"))
					//  for (int lll=0; lll<ff[l][ll].length; lll++)
					//    ff[l][ll][lll]=0;
					//else
					for (int lll = 0; lll < ff[l][ll].length; lll++) {
						ff[l][ll][lll] = Float.NaN;
					}
				}
			}
			for (int i = 0; i < vals.size(); i++) {
				float f[] = (float[]) vals.elementAt(i);
				for (int l = 0; l < l03; l++) {
					ff[l][Math.round(f[1])][Math.round(f[0])] = f[2 + l];
				}
			}
			Vector output = new Vector(3, 3);
			output.addElement(valsRef2);
			output.addElement(valsRef1);
			output.addElement(ff);
			return output;
		} catch (SQLException se) {
			System.out.println("Cannot get 2D bin counts: " + se.toString());
			return null;
		}
	}

	public boolean loadTableDescriptor(int tn, Vector columnNames) {
		return loadTableDescriptor(tn, columnNames, true);
	}

	public boolean loadTableDescriptor(int tn, Vector columnNames, boolean userMaySelectColumns) {
		TableDescriptor td = (TableDescriptor) ds.tableDescriptors.elementAt(tn);
		long t0 = System.currentTimeMillis();
		Vector selCol = null;
		if (columnNames != null) {
			selCol = columnNames;
		} else {
			Vector columns = new Vector(50, 20);
			boolean result = getColumns(tn, columns);
			if (!result) {
				closeConnection();
				return false;
			}
			if (userMaySelectColumns) {
				ColumnSelectPanel csel = new ColumnSelectPanel(columns);
				OKDialog okd = new OKDialog(getFrame(), "Select columns to load", true);
				okd.addContent(csel);
				okd.show();
				if (okd.wasCancelled()) {
					closeConnection();
					return false;
				}
				selCol = csel.getSelectedColumns();
			} else {
				selCol = columns;
			}
		}
		if (selCol != null) {
			td.columns = new Vector(selCol.size(), 5);
			Vector cds = getColumnDescriptors(ds.getTableDescriptor(tn), selCol);
			for (int i = 0; i < cds.size(); i++) {
				td.columns.addElement(cds.elementAt(i));
			}
		}
		long t1 = System.currentTimeMillis() - t0;
		// takes much time for large tables, and in fact not used is VA toolkit...
		// although is needed for the database aggregator
		//retrieveRowCount(tn);
		long t2 = System.currentTimeMillis() - t0 - t1;
		t1 /= 1000;
		t2 /= 1000;
		System.out.println("* cols: " + t1 + " sec; rows: " + t2 + " sec;");
		return true;
	}

	public ColumnDescriptorNum getColumnDescriptorNumFromDate(TableDescriptor td, ColumnDescriptorDate cdd) {
		List l = new List(12);
		l.add("Year");
		l.add("Month");
		l.add("Day");
		l.add("Quartal");
		l.add("Julian Day");
		l.add("Day in Year");
		l.add("Day of Week");
		l.add("Week number");
		l.add("Week in Month");
		if (this instanceof OracleConnector) {
			l.add("Hour");
			l.add("Minute");
			l.add("Second");
		}
		l.select(0);
		OKDialog ok = new OKDialog(getFrame(), "Select date component", true);
		ok.addContent(l);
		ok.show();
		if (ok.wasCancelled())
			return null;
		String fmt = null;
		switch (l.getSelectedIndex()) {
		case 0:
			fmt = (this instanceof OracleConnector) ? "RRRR" : "YYYY";
			break;
		case 1:
			fmt = "MM";
			break;
		case 2:
			fmt = "DD";
			break;
		case 3:
			fmt = "Q";
			break;
		case 4:
			fmt = "J";
			break;
		case 5:
			fmt = "DDD";
			break;
		case 6:
			fmt = "D";
			break;
		case 7:
			fmt = "WW";
			break;
		case 8:
			fmt = "W";
			break;
		case 9:
			fmt = "HH24";
			break;
		case 10:
			fmt = "MI";
			break;
		case 11:
			fmt = "SS";
			break;
		}
		if (this instanceof OracleConnector)
			return (ColumnDescriptorNum) getColumnDescriptor(td, "TO_NUMBER(TO_CHAR(" + cdd.name + ", '" + fmt + "'))");
		else if (this instanceof PostgresConnector)
			return (ColumnDescriptorNum) getColumnDescriptor(td, "TO_NUMBER(TO_CHAR(" + cdd.name + ", '" + fmt + "'),'099999')");
		return null;
	}

	/*
	* Checks if all combinations of values in columns from the given list are unique
	* if YES, returns NULL
	* if NO, returns Vector with 0,2,4,... containing combinations of values; 1,3,5,... - counts
	*/
	public Vector checkIfValuesAreUnique(String tableName, String columnNames[]) {
		Vector v = null;
		String allColumnNames = columnNames[0];
		for (int i = 1; i < columnNames.length; i++) {
			allColumnNames += ", " + columnNames[i];
		}
		try {
			String tableNameDuplicates = tableName + "_duplicates";
			dropTmpTable(tableNameDuplicates);
			long tStart = System.currentTimeMillis();
			Statement statement = connection.createStatement();
			String sqlString = "create table " + tableNameDuplicates + " as\n" + "select \n" + allColumnNames + ", count(*) as count_of_cases\n" + "from " + tableName + "\n" + "group by " + allColumnNames + "\n" + "having count(*)>1";
			System.out.println("* <" + sqlString + ">");
			ResultSet result = statement.executeQuery(sqlString);
			sqlString = "select * from " + tableNameDuplicates + "\nwhere rownum<1000";
			System.out.println("* <" + sqlString + ">");
			result = statement.executeQuery(sqlString);
			while (result.next()) {
				if (v == null) {
					v = new Vector(100, 100);
				}
				String vals[] = new String[columnNames.length];
				for (int i = 0; i < vals.length; i++) {
					vals[i] = result.getString(1 + i);
				}
				long n = result.getLong(1 + vals.length);
				v.addElement(vals);
				v.addElement(new Long(n));
			}
			if (v == null || v.size() == 0) {
				dropTmpTable(tableNameDuplicates);
			} else {
				sqlString = "select count(*), sum(count_of_cases) from " + tableNameDuplicates;
				result = statement.executeQuery(sqlString);
				result.next();
				long count = result.getInt(1), total = result.getInt(2);
				v.add(0, new Long(count));
				v.add(1, new Long(total));
			}
			long tEnd = System.currentTimeMillis() - tStart;
			System.out.println("* Ready. Elapsed time " + StringUtil.floatToStr(tEnd / 1000f, 3) + " (s)");
		} catch (SQLException se) {
			System.out.println("SQL error:" + se.toString());
		}
		return v;
	}

}
