package export;

import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.system.SystemUI;
import spade.lib.basicwin.OKDialog;
import spade.lib.lang.Language;
import spade.lib.util.IntArray;
import spade.lib.util.StringUtil;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.ObjectFilter;
import spade.vis.spec.DataSourceSpec;
import data_load.read_db.DBConnectPanel;

/**
* This is a class for writing the contents of a Descartes table into JDBC database
*/
public class TableToJDBC implements DataExporter {
	static ResourceBundle res = Language.getTextResource("export.Res");

	/**
	* Returns the name of the output format suitable for showing to the user.
	* For example, "Shape (ArcView)", "csv (comma-separated values)"
	*/
	@Override
	public String getFormatName() {
		return "JDBC database";
	}

	/**
	* Returns the usual file extension corresponding to this export format
	*/
	@Override
	public String getFileExtension() {
		return null;
	}

	/**
	* Returns true if this data exporter can store attributes from a table
	*/
	@Override
	public boolean canWriteAttributes() {
		return true;
	}

	/**
	* Returns true if the format requires creation of more than one files
	*/
	@Override
	public boolean needsMultipleFiles() {
		return false;
	}

	/**
	* Returns a default driver. For JDBC this is sun.jdbc.odbc.JdbcOdbcDriver.
	* The method may be redefined in descendants reading from different databases
	*/
	public String getDefaultDriver() {
		return "sun.jdbc.odbc.JdbcOdbcDriver";
	}

	/**
	* Returns the format accepted by this reader (in this case JDBC).
	* Subclasses intended for different databases may override this method.
	*/
	public String getFormat() {
		return "JDBC";
	}

	/**
	* Returns the prefix needed to construct a database url. For JDBC this is
	* "jdbc:odbc:". This may be overridden in subclasses reading from different
	* databases.
	*/
	public String getURLPrefix() {
		return "jdbc:odbc:";
	}

	/**
	 * Returns maximal length of SQL identifier
	 */
	public int getMaxSQLidLength() {
		return 10;
	}

	/**
	 * Returns maximal length of SQL identifier
	 */
	public int getMaxTableNameLength() {
		return 8;
	}

	/**
	* Database connection
	*/
	protected Connection connection = null;
	/**
	 * Prepared statement used for inserting data into table
	 */
	protected PreparedStatement ps = null;

	DataSourceSpec spec;
	SystemUI ui;

	/**
	* Metadata about the database
	*/
	protected DatabaseMetaData meta = null;

	/**
	* Opens a database connection for loading data. All the necessary information
	* has to be specified in the internal variable spec (an instance of
	* DataSourceSpec). If this information is absent, it is asked from the user
	* (in the case when mayAskUser is true). Returns true if successfully
	* connected.
	*/
	public boolean openConnection() {
		if (connection != null)
			return true;
		if (spec == null || spec.url == null || spec.source == null)
			if (spec == null) {
				spec = new DataSourceSpec();
			}

		spec.format = getFormat();
		if (spec.driver == null) {
			spec.driver = getDefaultDriver();
		}
		DBConnectPanel dbConnect = new DBConnectPanel(true);
		dbConnect.setDriver(spec.driver);
		dbConnect.setURLPrefix(getURLPrefix());
//		dbConnect.setComputer("plan-b.iais.fraunhofer.de");
		dbConnect.setComputer("localhost");
		dbConnect.setPort(1521);
		dbConnect.setDatabase("oracle11g");
		dbConnect.setUser("gennady_flickr");
		dbConnect.setPassword("gennady");

		OKDialog okd = new OKDialog(ui.getMainFrame(), res.getString("Connect_to_database"), true);
		okd.addContent(dbConnect);
		okd.show();
		if (okd.wasCancelled())
			return false;
		spec.driver = dbConnect.getDriver();
		spec.url = dbConnect.getDatabaseURL();
		spec.user = dbConnect.getUser();
		spec.password = dbConnect.getPassword();
		spec.source = dbConnect.getTable();
		connection = dbConnect.getConnection();
		if (ui != null) {
			ui.showMessage(res.getString("Connected_to") + spec.url, false);
		}

		if (spec.source == null) {
			spec.source = "Export";
		}
		spec.source = StringUtil.SQLid(spec.source, getMaxTableNameLength());

		if (spec == null || spec.url == null) {
			closeConnection();
			return false;
		}
		if (connection == null) {
			if (spec.driver == null) {
				spec.driver = getDefaultDriver();
			}
			try {
				Class.forName(spec.driver);
			} catch (Exception e) {
				//following text:"Failed to load the driver "
				if (ui != null) {
					ui.showMessage(res.getString("Failed_to_load_the") + spec.driver + ": " + e.toString(), true);
				}
				return false;
			}
			try {
				connection = DriverManager.getConnection(spec.url, spec.user, spec.password);
			} catch (SQLException se) {
				if (ui != null) {
					ui.showMessage(res.getString("Failed_to_connect_to") + spec.url + ": " + se.toString() + res.getString("_driver_") + spec.driver, true);
				}
				return false;
			}
		}
		return connection != null;
	}

	protected void closeConnection() {
		if (connection != null) {
			try {
				connection.close();
			} catch (SQLException e) {
			}
			connection = null;
			if (ui != null) {
				ui.showMessage(res.getString("Connection_closed") + spec.url, false);
			}
		}
		meta = null;
	}

	/**
	* Writes the data to the given stream. The SystemUI provided may be used for
	* displaying diagnostic messages. The exporter must check if the object passed
	* to it has the required type. Returns true if the data have been successfully
	* stored. Arguments:
	* data:          the table or layer to be stored
	* filter:        filter of records or objects. May be null. If not null, only
	*                the records (objects) satisfying the filter must be stored.
	* selAttr:       selected attributes to be stored. If null, no attributes
	*                are stored. Not appropriate for exporters that only store
	*                geographic data.
	* stream:        the stream in which to put the data (not necessarily a file,
	*                may be, for example, a script.
	* This method is not suitable for exporters that need to write to several files!
	*/
	@Override
	public boolean storeData(Object data, ObjectFilter filter, Vector selAttr, OutputStream stream, SystemUI ui) {
		this.ui = ui;
		if (data == null)
			return false;
		if (!(data instanceof AttributeDataPortion)) {
			// following string: "Illegal data type: AttributeDataPortion expected!"
			if (ui != null) {
				ui.showMessage(res.getString("Illegal_data_type3"), true);
			}
			return false;
		}
		AttributeDataPortion table = (AttributeDataPortion) data;
		if (!table.hasData() && table.getAttrCount() < 1) {
			// following string: "No data in the table!"
			if (ui != null) {
				ui.showMessage(res.getString("No_data_in_the_table_"), true);
			}
			return false;
		}
		long tStart = System.currentTimeMillis();
		int nrows = 0;
		IntArray attrNumbers = null;
		Vector attrNames = null;
		if (selAttr != null && selAttr.size() > 0 && table != null && table.getAttrCount() > 0) {
			attrNumbers = new IntArray(selAttr.size(), 1);
			attrNames = new Vector();
			for (int i = 0; i < selAttr.size(); i++) {
				int idx = table.getAttrIndex((String) selAttr.elementAt(i));
				if (idx >= 0) {
					attrNumbers.addElement(idx);
					String name = StringUtil.SQLid(table.getAttributeName(idx), getMaxSQLidLength());
					while (attrNames.contains(name) || name.equalsIgnoreCase("id") || name.equalsIgnoreCase("name") || name.equalsIgnoreCase("date") || name.equalsIgnoreCase("long")) {
						name = StringUtil.modifyId(name, getMaxSQLidLength());
					}
					attrNames.addElement(name);
				}
			}
		}
		try {
			// following string: "Writing data..."
			if (ui != null) {
				ui.showMessage(res.getString("Writing_data_"), false);
			}

			//connecting to database
			if (!openConnection())
				return false;
			if (meta == null) {
				try {
					meta = connection.getMetaData();
				} catch (SQLException se) {
					//following text:"Failed to get metadata: "
					if (ui != null) {
						ui.showMessage(res.getString("Failed_to_get") + se.toString(), true);
					}
				}
			}

			boolean goodName = false;
			while (!goodName) {
				try {
					Statement stat = connection.createStatement();
					stat.setMaxRows(1);
					ResultSet result = stat.executeQuery("SELECT * FROM " + spec.source);
					result.close();
					stat.close();
					spec.source = StringUtil.modifyId(spec.source, getMaxTableNameLength());
				} catch (SQLException se) {
					goodName = true;
				}
			}

			int lengths[] = new int[table.getAttrCount() + 2];
			for (int i = 0; i < lengths.length; i++) {
				lengths[i] = 0;
			}

			int aLen, idLen = 0, nameLen = 0;

			for (int i = 0; i < table.getDataItemCount(); i++) {
				if (attrNumbers != null) {
					for (int j = 0; j < attrNumbers.size(); j++) {
						try {
							aLen = table.getAttrValueAsString(attrNumbers.elementAt(j), i).length();
							if (lengths[attrNumbers.elementAt(j)] < aLen) {
								lengths[attrNumbers.elementAt(j)] = aLen;
							}
						} catch (Exception ex) {
						}

						idLen = Math.max(table.getDataItemId(i).length(), idLen);
						nameLen = Math.max(table.getDataItemName(i).length(), nameLen);
					}
				}

			}

			// generate table
			String str = "ID CHAR(" + idLen + "), NAME CHAR(" + nameLen + ")", sval = "";
			if (attrNumbers != null) {
				for (int j = 0; j < attrNumbers.size(); j++) {
					str += ", " + attrNames.elementAt(j) + " ";
					switch (table.getAttributeType(attrNumbers.elementAt(j))) {
					case 'I':
						str += "INTEGER";
						break;
					case 'R':
						str += "FLOAT";
						break;
					case 'T':
						str += "DATE";
						break;
					case 'C':
					default:
						str += "CHAR(" + lengths[attrNumbers.elementAt(j)] + ")";
						break;
					}
				}
			}

			//System.out.println("CREATE TABLE " + spec.source + " (" + str + ")");
			//---------------------------------------------------------------------------------
			//   createTable(str)
			try {
				createTable(str);
				//Statement stat=connection.createStatement();
				//int res=stat.executeUpdate("CREATE TABLE " + spec.source + " (" + str + ")");
				//stat.close();
			} catch (SQLException se) {
				if (ui != null) {
					ui.showMessage(res.getString("Cannot_create_a_table") + se.toString(), true);
				}
				System.out.println("ERROR: " + str);
				return false;
			}

//--------------------------------------------------------------------------------------------

			String columns = "ID, NAME", values = "?,?";
			if (attrNumbers != null) {
				for (int j = 0; j < attrNumbers.size(); j++) {
					columns += ", " + attrNames.elementAt(j);
					values += ",?";
				}
			}
			if (this instanceof LayerToOraSpatial) {
				columns += ",geom";
				values += ",?";
			}

			try {
				ps = connection.prepareStatement("INSERT INTO " + spec.source + " (" + columns + ") VALUES (" + values + ")");
			} catch (SQLException se) {
				if (ui != null) {
					ui.showMessage("Can not prepare SQL statement" + se.toString(), true);
				}
				System.out.println("ERROR: " + "INSERT INTO " + spec.source + " (" + columns + ") VALUES (" + values + ")");
				return false;
			}
			for (int i = 0; i < table.getDataItemCount(); i++) {
				boolean active = true;
				if (filter != null) {
					active = filter.isActive(i);
				} else if (filter != null) {
					active = filter.isActive(table.getDataItem(i));
				}
				if (active && attrNumbers != null) {
					try {
						populatePreparedStatement(table, attrNumbers, i, 3 + attrNumbers.size());
						ps.executeUpdate();
						++nrows;
						if (ui != null && nrows % 100 == 0) {
							// following string: " rows stored"
							ui.showMessage(nrows + res.getString("rows_stored"), false);
						}
					} catch (SQLException se) {
						if (ui != null) {
							ui.showMessage("Can't execute SQL statement" + se.toString(), true);
						}
						System.out.println("ERROR: " + ps);
						return false;
					}
				}
			}

		} catch (Exception ioe) {
			if (ui != null) {
				// following string: "Error writing to the file: "
				ui.showMessage(res.getString("Error_writing_to_the") + ioe.toString(), true);
			}
			return false;
		}
		long tEnd = System.currentTimeMillis() - tStart;
		System.out.println("* Ready. Elapsed time " + StringUtil.floatToStr(tEnd / 1000f, 3) + " (s)");

		if (ui != null)
			// following string: " rows stored"
			if (nrows > 0) {
				ui.showMessage(nrows + res.getString("rows_stored"), false);
			} else {
				ui.showMessage(res.getString("No_records_actually"), true);
			}
		return nrows > 0;
	}

	protected void populatePreparedStatement(AttributeDataPortion table, IntArray attrNumbers, int rowNum, int idxGeom) {
		try {
			ps.setString(1, table.getDataItemId(rowNum));
			ps.setString(2, table.getDataItemName(rowNum));
			for (int j = 0; j < attrNumbers.size(); j++) {
				switch (table.getAttributeType(attrNumbers.elementAt(j))) {
				case 'I':
					int psi = (int) table.getNumericAttrValue(attrNumbers.elementAt(j), rowNum);
					ps.setInt(3 + j, psi);
					break;
				case 'R':
					double psf = table.getNumericAttrValue(attrNumbers.elementAt(j), rowNum);
					if (Double.isNaN(psf)) {
						ps.setString(3 + j, "");
					} else {
						ps.setDouble(3 + j, psf);
					}
					break;
				case 'T':
					Object objVal = table.getAttrValue(attrNumbers.elementAt(j), rowNum);
					ps.setTimestamp(3 + j, getTimestamp((spade.time.Date) objVal));
					break;
				case 'C':
				default:
					String val = table.getAttrValueAsString(attrNumbers.elementAt(j), rowNum);
					if (val == null) {
						ps.setString(3 + j, "");
					} else {
						ps.setString(3 + j, instap(val));
					}
				}
			}
		} catch (SQLException se) {
			if (ui != null) {
				ui.showMessage("Can't execute SQL statement" + se.toString(), true);
			}
			System.out.println("ERROR: " + ps);
		}
	}

	protected String instap(String s) {
/*
    int ii = s.indexOf('\'');
    if(ii < 0) return s;
    String s1 = "";
    if(ii >1) s1 = s.substring(0,ii);
    String s2 = "";
    if(ii < s.length()-1) s2 = s.substring(ii);
    return s1 + "\'" + s2;
*/
//ID
		// I'm not sure what was the original idea of this method, but now it converts every single apostrothe to ''
		if (s == null)
			return null;
		if (s.indexOf('\'') < 0)
			return s;
		StringBuffer sb = new StringBuffer(s.length() + 5);
		for (int i = 0; i < s.length(); i++)
			if (s.charAt(i) == '\'') {
				sb.append("\'" + s.charAt(i));
			} else {
				sb.append(s.charAt(i));
			}
		return sb.toString();
//~ID
	}

	/**
	* Some formats, for example, Shape or ADF, require data to be stored in several
	* files. In this case the data cannot be written in just one stream and,
	* hence, the previous storeData method is not applicable. For this case
	* the method with arguments directory and file name insead of a stream
	* must be defined. Exporters that save data in just one file should in this
	* method open the specified file as a stream and call the method with the
	* stream argument.
	*/
	@Override
	public boolean storeData(Object data, ObjectFilter filter, Vector selAttr, String dir, String filename, SystemUI ui) {
		if (data == null)
			return false;
		boolean result = false;
		result = storeData(data, filter, selAttr, null, ui);
		return result;
	}

	/**
	 *  This Methods was created for overwriting in class LayerToOraSpatial (Mark)
	 *
	 */
	protected void createTable(String columns) throws SQLException {
		Statement stat = connection.createStatement();
		int res = stat.executeUpdate("CREATE TABLE " + spec.source + " (" + columns + ")");
		stat.close();
	}

	/*
	// used only in code witout PreparedStatement - now commented
	protected void insertIntoTable(Statement stat, String id, String columns, String values) throws SQLException
	{
	  int res=stat.executeUpdate("INSERT INTO " + spec.source+ " (" +columns+ ") VALUES (" +values+ ")");
	}
	*/

	protected Timestamp getTimestamp(spade.time.Date date) {
		int h = date.getElementValue('h');
		if (h == -1) {
			h = 0;
		}
		int t = date.getElementValue('t');
		if (t == -1) {
			t = 0;
		}
		int s = date.getElementValue('s');
		if (s == -1) {
			s = 0;
		}
		return new Timestamp(date.getElementValue('y') - 1900, date.getElementValue('m') - 1, date.getElementValue('d'), h, t, s, 0);
	}

	/**
	 * Returns the description of the file or database table to which the data
	 * were stored
	 */
	@Override
	public DataSourceSpec getStoredDataDescriptor() {
		return spec;
	}
}
