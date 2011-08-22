package data_load.read_oracle;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ResourceBundle;
import java.util.Vector;

import oracle.sdoapi.OraSpatialManager;
import oracle.sdoapi.adapter.GeometryAdapter;
import oracle.sdoapi.geom.GeometryCollection;
import oracle.sdoapi.geom.LineString;
import oracle.sdoapi.geom.Point;
import oracle.sdoapi.geom.Polygon;
import oracle.sql.ARRAY;
import oracle.sql.STRUCT;
import spade.lib.basicwin.OKDialog;
import spade.lib.lang.Language;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataPortion;
import spade.vis.database.DataRecord;
import spade.vis.database.DataSupplier;
import spade.vis.database.DataTable;
import spade.vis.database.LayerData;
import spade.vis.database.SpatialEntity;
import spade.vis.dmap.DGeoLayer;
import spade.vis.geometry.MultiGeometry;
import spade.vis.geometry.RealPoint;
import spade.vis.geometry.RealPolyline;
import spade.vis.geometry.RealRectangle;
import data_load.DataAgent;
import data_load.read_db.ColumnSelectPanel;
import data_load.read_db.JDBCReader;

/**
* A class that reads both spatial and attribute data
* from an an Oracle 8i Spatial database
*/

public class OracleReader extends JDBCReader {
	static ResourceBundle res = Language.getTextResource("data_load.read_oracle.Res");
	private static String TABLE_ALIAS = "t";
	/**
	* The version number allows to control the updates of the table
	*/
	protected long lastVersionNumber = 0l;

	/**
	* Returns "oracle.jdbc.driver.OracleDriver"
	*/
	@Override
	public String getDefaultDriver() {
		return "oracle.jdbc.driver.OracleDriver";
	}

	/**
	* Returns the format accepted by this reader (in this case ORACLE).
	*/
	@Override
	public String getFormat() {
		return "ORACLE";
	}

	/**
	* Returns the prefix needed to construct a database url. For Oracle this is
	* "jdbc:oracle.thin:@".
	*/
	@Override
	public String getURLPrefix() {
		return "jdbc:oracle:thin:";
	}

	/**
	* Constructs a special query that might be used to retrieve from the database
	* only the list of tables with geographic information.
	*/
	@Override
	public String getOnlyGeoTablesQuery() {
		//return "SELECT DISTINCT TABLE_NAME FROM USER_SDO_GEOM_METADATA";
		return "select local.TABLE_NAME from USER_TABLES local, USER_SDO_GEOM_METADATA sdo where local.TABLE_NAME = sdo.TABLE_NAME" + " union "
				+ "select local.VIEW_NAME from USER_VIEWS local, USER_SDO_GEOM_METADATA sdo where local.VIEW_NAME = sdo.TABLE_NAME";
	}

	/**
	* Constructs a special query that might be used to retrieve from the database
	* only the list of tables belonging to user's view.
	*/
	@Override
	public String getOnlyMyTablesQuery() {
		return "select TABLE_NAME from USER_TABLES union select VIEW_NAME from USER_VIEWS";
	}

	/**
	* Retrieves the list of column names from the database and fills the vector
	* columns with these names. Columns with geographical information are put
	* separately in the vector geoColumns. Both vectors must be initialized
	* before calling this method.
	*/
	protected boolean getColumnsAndGeoColumns(Vector columns, Vector geoColumns) {
		if (connection == null || columns == null || geoColumns == null)
			return false;
		try {
			Statement stat = connection.createStatement();
			stat.setMaxRows(2);
			ResultSet result = stat.executeQuery("SELECT * FROM " + spec.source + " " + TABLE_ALIAS);
			ResultSetMetaData md = result.getMetaData();
			int ncols = md.getColumnCount();
			for (int i = 1; i <= ncols; i++)
				if (getDescartesType(md, i) == AttributeTypes.geometry) {
					geoColumns.addElement(md.getColumnName(i));
				} else {
					columns.addElement(md.getColumnName(i));
				}
			result.close();
			stat.close();
		} catch (SQLException se) {
			//following text:"Cannot get the list of columns: "
			showMessage(res.getString("Cannot_get_the_list") + se.toString(), true);
		}
		if (columns.size() < 1 && geoColumns.size() < 1)
			return false;
		return true;
	}

	/**
	* Makes a query to the database in order to determine the number of rows in
	* the table. Returns true if this was actually done. The number of rows is
	* stored in spec.dbRowCount. If the number of rows was
	* already set (i.e. is more than zero), returns true immediately.
	*/
	protected boolean getRowCount() {
		if (connection == null || spec == null || spec.source == null)
			return false;
		if (spec.dbRowCount > 0)
			return true;
		try {
			Statement stat = connection.createStatement();
			ResultSet result = stat.executeQuery("SELECT COUNT(*) FROM " + spec.source);
			result.next();
			spec.dbRowCount = result.getInt(1);
		} catch (SQLException se) {
			//following text:"Failed to get row count for table "
			showMessage(res.getString("Failed_to_get_row") + spec.source + ": " + se.toString(), true);
		}
		return spec.dbRowCount > 0;
	}

	/**
	* Makes a query to the database in order to determine the territory extent of
	* the table data. Returns true if this was actually done. The extent is
	* stored in the spec.dataExtent. If the extent was
	* already set (i.e. is not null), returns true immediately.
	*/
	protected boolean getExtent() {
		if (connection == null || spec == null || spec.source == null || spec.geoFieldName == null)
			return false;
		if (spec.dataExtent != null)
			if (spec.dataExtent instanceof RealRectangle)
				return true;
			else {
				spec.dataExtent = null;
			}
		String sql = "SELECT DIMINFO FROM USER_SDO_GEOM_METADATA WHERE TABLE_NAME='" + spec.source.toUpperCase() + "' AND COLUMN_NAME='" + spec.geoFieldName.toUpperCase() + "'";
		try {
			Statement stat = connection.createStatement();
			ResultSet extents = stat.executeQuery(sql);
			extents.next();
			ARRAY array = (ARRAY) extents.getObject("diminfo");
			Object[] objects = (Object[]) array.getArray();
			STRUCT xStruct = (STRUCT) objects[0];
			STRUCT yStruct = (STRUCT) objects[1];
			Object[] xAttrs = xStruct.getAttributes();
			Object[] yAttrs = yStruct.getAttributes();
			float xmin = ((java.math.BigDecimal) xAttrs[1]).floatValue();
			float xmax = ((java.math.BigDecimal) xAttrs[2]).floatValue();
			float ymin = ((java.math.BigDecimal) yAttrs[1]).floatValue();
			float ymax = ((java.math.BigDecimal) yAttrs[2]).floatValue();
			spec.dataExtent = new RealRectangle(xmin, ymin, xmax, ymax);
			extents.close();
			stat.close();
		} catch (SQLException e) {
			//following text:"Failed to get layer extent: "
			showMessage(res.getString("Failed_to_get_layer") + e.toString(), true);
		}
		return spec.dataExtent != null;
	}

	@Override
	protected String makeSQL() {
		String q = "SELECT " + makeColumnListString() + " FROM " + spec.source + " " + TABLE_ALIAS;
		if (spec.geoFieldName != null && spec.bounds != null && spec.bounds.size() != 0) {
			String elemInfoArray, ordinateArray;
			elemInfoArray = "1,1003,3";
			ordinateArray = ((RealRectangle) spec.bounds.elementAt(0)).toString();
			for (int i = 1; i < spec.bounds.size(); i++) {
				elemInfoArray += "," + (i * 4 + 1) + ",1003,3";
				ordinateArray += "," + ((RealRectangle) spec.bounds.elementAt(i)).toString();
			}
			q += " WHERE SDO_RELATE(" + TABLE_ALIAS + "." + spec.geoFieldName + ", mdsys.sdo_geometry(2003,NULL,NULL," + "mdsys.sdo_elem_info_array(" + elemInfoArray + ")," + "mdsys.sdo_ordinate_array(" + ordinateArray
					+ ")),'mask=anyinteract querytype=WINDOW') = 'TRUE'";
		}
		return q;
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
			Vector columns = new Vector(50, 20), geoColumns = new Vector(10, 5);
			if (getColumnsAndGeoColumns(columns, geoColumns)) {
				ColumnSelectPanel csel = null;
				if (geoColumns != null && geoColumns.size() > 0) {
					csel = new OracleColumnSelectPanel(columns, geoColumns);
				} else {
					csel = new ColumnSelectPanel(columns);
				}
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
				if (geoColumns != null && geoColumns.size() > 0) {
					spec.geoFieldName = ((OracleColumnSelectPanel) csel).getGeoColName();
				} else {
					spec.xCoordFieldName = csel.getXColName();
					spec.yCoordFieldName = csel.getYColName();
				}
			}
		}
		if (mayAskUser && (spec.bounds == null || spec.bounds.size() < 1)) {
			spec.bounds = null;
			//ask the user about the territory limits for which to load the data
			if (getExtent()) {
				RealRectangle mapExt = null;
				if (ui != null && ui.getCurrentMapViewer() != null) {
					mapExt = new RealRectangle(ui.getCurrentMapViewer().getMapExtent());
				}
				GetBoundsPanel gbp = new GetBoundsPanel((RealRectangle) spec.dataExtent, mapExt);
				//following text:"Set territory limits"
				OKDialog okd = new OKDialog(getFrame(), res.getString("Set_territory_limits"), true);
				okd.addContent(gbp);
				okd.show();
				if (!okd.wasCancelled()) {
					RealRectangle rr = gbp.getExtent();
					if (rr != null) {
						spec.bounds = new Vector(1, 1);
						spec.bounds.addElement(rr);
					}
				}
			}
		}
		getRowCount();
		getExtent();
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

		DataTable table = null;
		if (dtab == null) {
			constructTable();
			table = dtab;
		} else if (!dtab.hasAttributes()) {
			table = dtab;
		} else {
			table = new DataTable();
		}

		int idFN = -1, nameFN = -1, geoFN = -1;
		boolean OK = true;
		if (spec.geoFieldName != null) {
			data = new LayerData();
			RealRectangle dataExtent = (RealRectangle) spec.dataExtent;
			if (dataExtent != null) {
				data.setBoundingRectangle(dataExtent.rx1, dataExtent.ry1, dataExtent.rx2, dataExtent.ry2);
			}
			if (spec.bounds == null || spec.bounds.size() < 1) {
				data.setHasAllData(true);
			} else if (dataExtent != null) {
				RealRectangle sb = (RealRectangle) spec.bounds.elementAt(0);
				if (sb.rx1 <= dataExtent.rx1 && sb.ry1 <= dataExtent.ry1 && sb.rx2 >= dataExtent.rx2 && sb.ry2 >= dataExtent.ry2) {
					data.setHasAllData(true);
				}
			}
		}
		GeometryAdapter sdoAdapter = OraSpatialManager.getGeometryAdapter("SDO", "8.1.6", STRUCT.class, STRUCT.class, null, connection);
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
				} else if (geoFN < 0 && spec.geoFieldName != null && attrId.equalsIgnoreCase(spec.geoFieldName)) {
					geoFN = i;
				} else {
					char type = getDescartesType(md, i);
					table.addAttribute(attrName, attrId, type);
				}
			}
			int row = 0;
			while (result.next()) {
				String id = null;
				String name = null;
				Vector values = new Vector();
				spade.vis.geometry.Geometry geometry = null;
				for (int i = 1; i <= ncols; i++)
					//in a ResultSet columns are counted starting from 1
					if (i == geoFN) {
						//retrieve geometry from this field
						STRUCT struct = (STRUCT) result.getObject(i);
						oracle.sdoapi.geom.Geometry geom = null;
						try {
							geom = sdoAdapter.importGeometry(struct);
						} catch (Exception e) {
						}
						if (geom != null) {
							geometry = convertGeom(geom);
						}
					} else {
						String s = result.getString(i);
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
				table.addDataRecord(drec);
				if (geometry != null) {
					SpatialEntity spe = new SpatialEntity(id, name);
					spe.setGeometry(geometry);
					spe.setThematicData(drec);
					data.addDataItem(spe);
				}
				if (row % 50 == 0) {
					//following text:"Reading "
					//following text:" rows got"
					showMessage(res.getString("Reading") + spec.source + ": " + row + res.getString("rows_got"), false);
				}
			}
			//following text:"Got "
			//following text:" database records from "
			showMessage(res.getString("Got") + table.getDataItemCount() + res.getString("database_records_from") + spec.source, false);
			//System.out.println("Got "+table.getDataItemCount()+" database records from "+spec.source);
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
		if (data != null && data.getDataItemCount() < 1) {
			data = null;
		}
		setDataReadingInProgress(false);
		if (table != null && table.hasData()) {
			table.finishedDataLoading();
			if (!table.equals(dtab)) {
				dtab.update(table, false);
			}
		}
		return OK;
	}

	/**
	* Converts an Oracle geometry into Descartes geometry
	*/
	protected spade.vis.geometry.Geometry convertGeom(oracle.sdoapi.geom.Geometry origGeom) {
		if (origGeom == null)
			return null;
		try {
			if (origGeom instanceof Point)
				return convertPoint((Point) origGeom);
			else if (origGeom instanceof LineString)
				return convertLineString((LineString) origGeom);
			else if (origGeom instanceof Polygon)
				return convertPolygon((Polygon) origGeom);
			else if (origGeom instanceof GeometryCollection)
				return convertCollection((GeometryCollection) origGeom);
			else {
				//following text:"Error in OracleReader: unable convert geometry for "
				showMessage(res.getString("Error_in_OracleReader") + origGeom, true);
			}
		} catch (Exception e) {
			//following text:"Error converting geometry: "
			showMessage(res.getString("Error_converting") + e.toString(), true);
		}
		return null;
	}

	protected spade.vis.geometry.Geometry convertPoint(Point origPoint) throws Exception {
		RealPoint point = new RealPoint();
		point.x = (float) origPoint.getX();
		point.y = (float) origPoint.getY();
		return point;
	}

	protected spade.vis.geometry.Geometry convertLineString(LineString origLineString) throws Exception {
		double coordArray[] = origLineString.getCoordArray();
		int numPoints = coordArray.length / 2;
		RealPolyline line = new RealPolyline();
		line.p = new RealPoint[numPoints];
		for (int i = 0; i < numPoints; i++) {
			line.p[i] = new RealPoint();
			line.p[i].x = (float) coordArray[2 * i];
			line.p[i].y = (float) coordArray[2 * i + 1];
			;
		}
		return line;
	}

	protected spade.vis.geometry.Geometry convertPolygon(Polygon origPolygon) throws Exception {
		LineString intLS[] = (LineString[]) origPolygon.getInteriorRingArray();
		LineString extLS = (LineString) origPolygon.getExteriorRing();
		if (intLS == null)
			return convertLineString(extLS);
		MultiGeometry mg = new MultiGeometry();
		mg.addPart(convertGeom(extLS));
		for (LineString element : intLS) {
			mg.addPart(convertGeom(element));
		}
		return mg;
	}

	protected spade.vis.geometry.Geometry convertCollection(GeometryCollection origCollection) throws Exception {
		oracle.sdoapi.geom.Geometry origGeoms[] = origCollection.getGeometryArray();
		MultiGeometry mg = new MultiGeometry();
		for (oracle.sdoapi.geom.Geometry origGeom : origGeoms) {
			mg.addPart(convertGeom(origGeom));
		}
		return mg;
	}

	/**
	* Returns the map layer constructed from the coordinates contained in the
	* table (if any). If the table contains no coordinates, returns null.
	* If the table has not been loaded yet, the reader constructs a layer without
	* objects and sets itself as the  data supplier of the layer. Then the data
	* will be loaded when the layer is first drawn.
	* If the database contains geographic data and not all the data are requested
	* at once, the reader constructs a DataBroker that will optimise the transfer
	* of spatial data from a database to the system by means of caching.
	*/
	@Override
	public DGeoLayer getMapLayer() {
		if (layer != null)
			return layer;
		if (spec == null)
			return null;
		if (spec.geoFieldName == null && (spec.xCoordFieldName == null || spec.yCoordFieldName == null))
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
			DataSupplier ds = this;
			//Check whether the DataBroker class is available
			if (spec.geoFieldName != null && spec.idFieldName != null) {
				DataAgent dataBroker = null;
				try {
					dataBroker = (DataAgent) Class.forName("data_load.cache.DataBroker").newInstance();
				} catch (Exception e) {
				}
				if (dataBroker != null) {
					if (spec.dbRowCount <= 0 || spec.dataExtent == null) { //needed for DataBroker
						if (connection != null || openConnection(false)) {
							if (spec.dbRowCount <= 0) {
								getRowCount();
							}
							if (spec.dataExtent == null) {
								getExtent();
							}
						}
					}
					if (spec.dbRowCount > 0 && spec.dataExtent != null) {
						dataBroker.setDataReader(this);
						dataBroker.setUI(ui);
						dataBroker.init((RealRectangle) spec.dataExtent, spec.dbRowCount);
						ds = dataBroker;
					}
				}
			}
			layer.setDataSupplier(ds);
		}
		System.out.println("layer-type:" + layer.getType());
		return layer;
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
		if (spec.geoFieldName != null || dtab == null || !dtab.hasData()) {
			loadData(false);
		}
		if (data != null)
			return data;
		if (spec.geoFieldName == null)
			return tryGetGeoObjects();
		return null;
	}
}
