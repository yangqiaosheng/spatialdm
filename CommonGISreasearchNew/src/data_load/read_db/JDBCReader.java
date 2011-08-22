package data_load.read_db;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.system.AttrDataReader;
import spade.analysis.system.GeoDataReader;
import spade.lib.basicwin.OKDialog;
import spade.lib.lang.Language;
import spade.lib.util.StringUtil;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataPortion;
import spade.vis.database.DataRecord;
import spade.vis.database.DataSupplier;
import spade.vis.database.DataTable;
import spade.vis.database.LayerData;
import spade.vis.database.SpatialEntity;
import spade.vis.database.TableContentSupplier;
import spade.vis.database.ThematicDataItem;
import spade.vis.dmap.DGeoLayer;
import spade.vis.geometry.RealPoint;
import spade.vis.spec.DataSourceSpec;
import data_load.readers.BaseDataReader;

public class JDBCReader extends BaseDataReader implements AttrDataReader, GeoDataReader, TableContentSupplier, DataSupplier {
	static ResourceBundle res = Language.getTextResource("data_load.read_db.Res");
	/**
	* Database connection
	*/
	protected Connection connection = null;
	/**
	* Metadata about the database
	*/
	protected DatabaseMetaData meta = null;
	/**
	* The table loaded
	*/
	protected DataTable dtab = null;
	/**
	* The geo layer created, if the table contains coordinates
	*/
	protected DGeoLayer layer = null;
	/**
	* The spatial data for the layer (point entities)
	*/
	protected LayerData data = null;

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
	* Constructs a special query that might be used to retrieve from the database
	* only the list of tables with geographic information (if possible).
	* By default, returns null. May be overridden in subclasses.
	*/
	public String getOnlyGeoTablesQuery() {
		return null;
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
	* Constructs an empty table. Sets its name, identifier, etc. on the basis of
	* the data source specification provided.
	*/
	protected void constructTable() {
		if (dtab != null)
			return;
		dtab = new DataTable();
		if (spec != null) {
			dtab.setDataSource(spec);
			if (spec.name != null) {
				dtab.setName(spec.name);
			} else {
				dtab.setName(spec.source);
			}
		}
	}

	/**
	* Returns the table with the attribute (thematic) data loaded (if any)
	*/
	@Override
	public DataTable getAttrData() {
		if (dtab == null) {
			constructTable();
			dtab.setTableContentSupplier(this);
		}
		return dtab;
	}

	/**
	* Returns the map layer constructed from the coordinates contained in the
	* table (if any). If the table contains no coordinates, returns null.
	* If the table has not been loaded yet, the reader constructs a layer without
	* objects and sets itself as the  data supplier of the layer. Then the data
	* will be loaded when the layer is first drawn.
	*/
	@Override
	public DGeoLayer getMapLayer() {
		if (layer != null)
			return layer;
		if (spec == null || spec.xCoordFieldName == null || spec.yCoordFieldName == null)
			return null;
		layer = new DGeoLayer();
		layer.setDataSource(spec);
		if (spec.name != null) {
			layer.setName(spec.name);
		} else {
			layer.setName(spec.source);
		}
		if (data != null) {
			layer.receiveSpatialData(data);
		}
		if (data == null || !data.hasAllData()) {
			layer.setDataSupplier(this);
		}
		return layer;
	}

	/**
	 * MSSQLReader overrides this method because it needs a non standard
	 * URL.
	 */
	public String getDatabaseURL(DBConnectPanel dbConnect) {
		return dbConnect.getDatabaseURL();
	}

	/**
	* Opens a database connection for loading data. All the necessary information
	* has to be specified in the internal variable spec (an instance of
	* DataSourceSpec). If this information is absent, it is asked from the user
	* (in the case when mayAskUser is true). Returns true if successfully
	* connected.
	*/

	public boolean openConnection(boolean mayAskUser) {
		if (connection != null)
			return true;
		if (spec == null || spec.url == null || spec.source == null)
			if (!mayAskUser)
				return false;
			else {
				if (spec == null) {
					spec = new DataSourceSpec();
				}
				spec.format = getFormat();
				if (spec.driver == null) {
					spec.driver = getDefaultDriver();
				}
				DBConnectPanel dbConnect = new DBConnectPanel(false);
				dbConnect.setDriver(spec.driver);
				dbConnect.setURLPrefix(getURLPrefix());
				dbConnect.setComputer("localhost");
				dbConnect.setPort(1521);
				dbConnect.setDatabase("oracle11g");
				dbConnect.setUser("gennady_flickr");
				dbConnect.setPassword("gennady");
				OKDialog okd = new OKDialog(getFrame(), res.getString("Connect_to_database"), true);
				okd.addContent(dbConnect);
				okd.show();
				if (okd.wasCancelled())
					return false;
				spec.driver = dbConnect.getDriver();
				spec.url = getDatabaseURL(dbConnect);
				spec.user = dbConnect.getUser();
				spec.password = dbConnect.getPassword();
				spec.source = dbConnect.getTable();
				connection = dbConnect.getConnection();
				showMessage(res.getString("Connected_to") + spec.url, false);
				if (spec.source == null) { //allow the user to select the table to load
					DBViewPanel dbView = new DBViewPanel(connection, getOnlyMyTablesQuery());
					if (!dbView.hasValidContent()) {
						showMessage(dbView.getErrorMessage(), true);
						closeConnection();
						return false;
					}
					dbView.setOnlyGeoTablesQuery(getOnlyGeoTablesQuery());
					okd = new OKDialog(getFrame(), res.getString("Select_a_table"), true);
					okd.addContent(dbView);
					okd.show();
					if (okd.wasCancelled()) {
						closeConnection();
						return false;
					}
					spec.source = dbView.getTableName();
					spec.catalog = dbView.getCatalog();
					meta = dbView.getMetaData();
				}
			}
		if (spec == null || spec.url == null || spec.source == null) {
			closeConnection();
			return false;
		}
		if (connection == null) {
			/*/for debugging
			try {
			  PrintWriter pw=new PrintWriter(new FileOutputStream("c:/temp/sql.log",true));
			  DriverManager.setLogWriter(pw);
			} catch (IOException ioe) {}
			//end*/
			if (spec.driver == null) {
				spec.driver = getDefaultDriver();
			}
			try {
				Class.forName(spec.driver);
			} catch (Exception e) {
				//following text:"Failed to load the driver "
				showMessage(res.getString("Failed_to_load_the") + spec.driver + ": " + e.toString(), true);
				return false;
			}
			try {
				connection = DriverManager.getConnection(spec.url, spec.user, spec.password);

			} catch (SQLException se) {
				showMessage(res.getString("Failed_to_connect_to") + spec.url + ": " + se.toString() + res.getString("_driver_") + spec.driver, true);
				return false;
			}
		}
		return connection != null;
	}

	/**
	* Loads the data. When allowed (the argument mayAskUser is true) and necessary,
	* starts a dialog or a wizard for the user
	* to specify the data source, constraints, etc. Returns true if the data
	* have been successfully loaded.
	*/
	@Override
	public boolean loadData(boolean mayAskUser) {
		if (dataReadingInProgress) {
			waitDataReadingFinish();
			return !dataError;
		}
		if (!mayAskUser) {
			setDataReadingInProgress(true);
		}
		if (!openConnection(mayAskUser)) {
			setDataReadingInProgress(false);
			return false;
		}
		if (mayAskUser && (spec.columns == null || spec.idFieldName == null)) {
			if (meta == null) {
				try {
					meta = connection.getMetaData();
				} catch (SQLException se) {
					//following text:"Failed to get metadata: "
					showMessage(res.getString("Failed_to_get") + se.toString(), true);
				}
			}
			if (meta != null) {
				Vector columns = getColumnList(connection, meta);
				if (columns != null && columns.size() > 0) {
					ColumnSelectPanel csel = new ColumnSelectPanel(columns);
					//following text:"Select columns to load"
					OKDialog okd = new OKDialog(getFrame(), res.getString("Select_columns_to1"), true);
					okd.addContent(csel);
					okd.show();
					if (okd.wasCancelled()) {
						closeConnection();
						setDataReadingInProgress(false);
						return false;
					}
					Vector selCol = csel.getSelectedColumns();
					if (selCol != null && selCol.size() < columns.size()) {
						spec.columns = selCol;
					}
					spec.idFieldName = csel.getIdColName();
					spec.nameFieldName = csel.getNameColName();
					spec.xCoordFieldName = csel.getXColName();
					spec.yCoordFieldName = csel.getYColName();
				}
			}
		}
		Statement stat = null;
		ResultSet result = null;
		ResultSetMetaData md = null;
		int ncols = 0;
		try {
			stat = connection.createStatement();
			result = stat.executeQuery(makeSQL());
			md = result.getMetaData();
			ncols = md.getColumnCount();
		} catch (SQLException se) {
			//following text:"Failed to get data from "
			showMessage(res.getString("Failed_to_get_data") + spec.source + ": " + se.toString(), true);
			closeConnection();
			setDataReadingInProgress(false);
			return false;
		}
		if (ncols < 1) {
			//following text:"No columns with data got from "
			showMessage(res.getString("No_columns_with_data") + spec.source, true);
			try {
				result.close();
				stat.close();
			} catch (SQLException e) {
			}
			closeConnection();
			setDataReadingInProgress(false);
			return false;
		}
		constructTable();
		int idFN = -1, nameFN = -1;
		boolean OK = true;
		try {
			for (int i = 1; i <= ncols; i++) {
				String attrId = md.getColumnName(i), attrName = md.getColumnLabel(i);
				if (attrId == null) {
					continue;
				}
				if (idFN < 0 && spec.idFieldName != null && attrId.equalsIgnoreCase(spec.idFieldName)) {
					idFN = i;
				} else if (nameFN < 0 && spec.nameFieldName != null && attrId.equalsIgnoreCase(spec.nameFieldName)) {
					nameFN = i;
				} else {
					char type = getDescartesType(md, i);
					dtab.addAttribute(attrName, attrId, type);
				}
			}
			int row = 0;
			while (result.next()) {
				String id = null;
				String name = null;
				Vector values = new Vector();
				for (int i = 1; i <= ncols; i++) {
					String s = result.getString(i); //in a ResultSet columns are counted starting from 1
					if (s != null) {
						s = s.trim();
					}
					if (i == idFN) {
						id = s;
					} else if (i == nameFN) {
						name = s;
					} else {
						values.addElement(s);
					}
				}
				++row;
				if (id == null || id.length() < 1) {
					id = String.valueOf(row);
				}
				DataRecord drec = new DataRecord(id, name);
				//System.out.println("["+id+"],["+name+"]");
				for (int i = 0; i < values.size(); i++) {
					drec.addAttrValue(values.elementAt(i));
				}
				dtab.addDataRecord(drec);
				if (row % 50 == 0) {
					showMessage(res.getString("Reading") + spec.source + ": " + row + res.getString("rows_got"), false);
				}
			}
			showMessage(res.getString("Got") + dtab.getDataItemCount() + res.getString("database_records_from") + spec.source, false);
			System.out.println("Got " + dtab.getDataItemCount() + " database records from " + spec.source);
		} catch (SQLException se) {
			//following text:"Exception while reading data from "
			showMessage(res.getString("Exception_while") + spec.source + ": " + se.toString(), true);
			OK = false;
		}
		try {
			result.close();
			stat.close();
		} catch (SQLException e) {
		}
		closeConnection();
		setDataReadingInProgress(false);
		if (dtab != null && dtab.hasData()) {
			dtab.finishedDataLoading();
		}
		return OK;
	}

	protected void closeConnection() {
		if (connection != null) {
			try {
				connection.close();
			} catch (SQLException e) {
			}
			connection = null;
		}
		meta = null;
	}

	protected String makeColumnListString() {
		if (spec.columns == null || spec.columns.size() < 1)
			return "*";
		Vector v = (Vector) spec.columns.clone();
		if (spec.idFieldName != null && !StringUtil.isStringInVectorIgnoreCase(spec.idFieldName, v)) {
			v.insertElementAt(spec.idFieldName, 0);
		}
		if (spec.nameFieldName != null && !StringUtil.isStringInVectorIgnoreCase(spec.nameFieldName, v)) {
			v.insertElementAt(spec.nameFieldName, 1);
		}
		if (spec.xCoordFieldName != null && !StringUtil.isStringInVectorIgnoreCase(spec.xCoordFieldName, v)) {
			v.addElement(spec.xCoordFieldName);
		}
		if (spec.yCoordFieldName != null && !StringUtil.isStringInVectorIgnoreCase(spec.yCoordFieldName, v)) {
			v.addElement(spec.yCoordFieldName);
		}
		if (spec.geoFieldName != null && !StringUtil.isStringInVectorIgnoreCase(spec.geoFieldName, v)) {
			v.addElement(spec.geoFieldName);
		}
		String q = "\"" + (String) v.elementAt(0) + "\"";
		for (int i = 1; i < v.size(); i++) {
			q += ", \"" + (String) v.elementAt(i) + "\"";
		}
		return q;
	}

	protected String makeSQL() {
		return "SELECT " + makeColumnListString() + " FROM " + spec.source;
	}

	protected Vector getColumnList(Connection connection, DatabaseMetaData meta) {
		if (connection == null || spec == null || spec.source == null)
			return null;
		if (meta == null) {
			try {
				meta = connection.getMetaData();
			} catch (SQLException se) {
				//following text:"Failed to get metadata: "
				showMessage(res.getString("Failed_to_get") + se.toString(), true);
			}
		}
		Vector columns = new Vector(50, 10);
		if (meta != null) {
			try {
				ResultSet rs = meta.getColumns(spec.catalog, null, spec.source, null);
				while (rs.next()) {
					columns.addElement(rs.getString("COLUMN_NAME"));
				}
			} catch (SQLException se) {
				//following text:"Cannot get the list of columns: "
				showMessage(res.getString("Cannot_get_the_list") + se.toString(), true);
			}
		}
		if (columns.size() < 1) {
			ResultSetMetaData md = null;
			try {
				Statement stat = connection.createStatement();
				stat.setMaxRows(2);
				ResultSet result = stat.executeQuery("SELECT * FROM " + spec.source);
				md = result.getMetaData();
				int ncols = md.getColumnCount();
				for (int i = 1; i <= ncols; i++) {
					columns.addElement(md.getColumnName(i));
				}
				result.close();
				stat.close();
			} catch (SQLException se) {
				//following text:"Cannot get the list of columns: "
				showMessage(res.getString("Cannot_get_the_list") + se.toString(), true);
			}
		}
		if (columns.size() < 1)
			return null;
		showMessage(null, false);
		return columns;
	}

	/**
	* Transforms SQL column type into Descartes column type
	*/
	protected char getDescartesType(ResultSetMetaData md, int colN) {
		if (md == null)
			return AttributeTypes.character;
		int type = Types.OTHER;
		try {
			type = md.getColumnType(colN);
		} catch (SQLException e) {
		}
		switch (type) {
		case Types.BIGINT:
		case Types.INTEGER:
		case Types.SMALLINT:
		case Types.TINYINT:
			return AttributeTypes.integer;
		case Types.BIT:
			return AttributeTypes.logical;
		case Types.DECIMAL:
		case Types.DOUBLE:
		case Types.FLOAT:
		case Types.NUMERIC:
		case Types.REAL:
			return AttributeTypes.real;
		case Types.DATE:
		case Types.TIME:
		case Types.TIMESTAMP:
			return AttributeTypes.time;
		}
		try {
			String typename = md.getColumnTypeName(colN);
			if (typename.equalsIgnoreCase("SDO_GEOMETRY") || typename.equalsIgnoreCase("MDSYS.SDO_GEOMETRY"))
				return AttributeTypes.geometry;
		} catch (SQLException e) {
		}
		return AttributeTypes.character;
	}

	/**
	* If the table contains coordinates of geographical objects, this method
	* constructs the objects and links them to the corresponding thematic data.
	*/
	protected LayerData tryGetGeoObjects() {
		if (dtab == null)
			return null;
		if (spec.xCoordFieldName != null && spec.yCoordFieldName != null) {
			//form a geo layer with point objects
			int xfn = dtab.getAttrIndex(spec.xCoordFieldName), yfn = dtab.getAttrIndex(spec.yCoordFieldName);
			if (xfn < 0 || yfn < 0) {
				//following text:"Could not get point objects from the dtab "
				//following text:": no coordinates found!"
				showMessage(res.getString("Could_not_get_point") + spec.source + res.getString("_no_coordinates_found"), true);
				return null;
			}
			LayerData ld = new LayerData();
			for (int i = 0; i < dtab.getDataItemCount(); i++) {
				RealPoint rp = new RealPoint();
				rp.x = (float) dtab.getNumericAttrValue(xfn, i);
				rp.y = (float) dtab.getNumericAttrValue(yfn, i);
				SpatialEntity spe = new SpatialEntity(dtab.getDataItemId(i));
				spe.setGeometry(rp);
				spe.setThematicData((ThematicDataItem) dtab.getDataItem(i));
				spe.setName(dtab.getDataItemName(i));
				ld.addItemSimple(spe);
				if ((i + 1) % 50 == 0) {
					//following text:"Constructing point objects: "
					//following text:" objects constructed"
					showMessage(res.getString("Constructing_point") + (i + 1) + res.getString("objects_constructed"), false);
				}
			}
			ld.setHasAllData(true);
			return ld;
		}
		return null;
	}

//----------------- TableContentSupplier interface ---------------------------
//----------------- (used for "delayed" loading of dtab data) ---------------
	/**
	* Using this method the table asks its supplier to fill it with the data.
	* The method returns true if the data have been successfully loaded.
	*/
	@Override
	public boolean fillTable() {
		return loadData(false);
	}

//----------------- DataSupplier interface -----------------------------------
//----------------- (used for "delayed" loading of map layers) ---------------
	/**
	* Returns the SpatialDataPortion containing all DataItems available
	*/
	@Override
	public DataPortion getData() {
		if (data != null)
			return data;
		if (dataError)
			return null;
		if (dtab == null || !dtab.hasData()) {
			loadData(false);
		}
		return tryGetGeoObjects();
	}

	/**
	* Constructs and returns a DataPortion containing DataItems fitting in at
	* least one of the specified bounding rectangles (listed in the vector).
	* The elements of the vector are instances of
	* @see spade.vis.geometry.RealRectangle
	* The JDBC reader sets the bounds in the internal instance of DataSourceSpec
	* and calls the method getData() without arguments.
	*/
	@Override
	public DataPortion getData(Vector bounds) {
		if (data != null && data.hasAllData())
			return data;
		if (dtab != null && (spec.bounds == null || spec.bounds.size() < 1 || (spec.geoFieldName == null && (spec.xCoordFieldName == null || spec.yCoordFieldName == null))))
			return getData();
		data = null;
		spec.bounds = bounds;
		return getData();
	}

	/**
	* When no more data from the DataSupplier are needed, this method is
	* called. Here the DataSupplier can clear its internal structures
	*/
	@Override
	public void clearAll() {
		data = null;
	}
}
