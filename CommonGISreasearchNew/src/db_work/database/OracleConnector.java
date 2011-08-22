package db_work.database;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;

import spade.lib.util.FloatArray;
import spade.lib.util.IntArray;
import spade.lib.util.LongArray;
import spade.lib.util.StringUtil;

//import data_load.read_db.ColumnSelectPanel;
//import data_load.read_oracle.OracleColumnSelectPanel;

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: 24-Jan-2006
 * Time: 17:22:54
 * To change this template use File | Settings | File Templates.
 */
public class OracleConnector extends JDBCConnector {

	//private static String TABLE_ALIAS = "t"; // ???

	public OracleConnector() {
		super();
		port = 1521;
		//databaseName="stockhol"; //"ora10g";
	}

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
	public String getURLPrefix() {
		return "jdbc:oracle:thin:";
	}

	/**
	* Constructs a special query that might be used to retrieve from the database
	* only the list of tables belonging to user's view.
	*/
	public String getOnlyMyTablesQuery() {
		return "select TABLE_NAME from USER_TABLES union select VIEW_NAME from USER_VIEWS";
	}

	/**
	 * Checks if a table with the given name exists
	 */
	public boolean checkIfTableOrViewExists(String name) {
		String sql = "";
		try {
			Statement statement = connection.createStatement();
			sql = "select * from user_tables where table_name like '" + name.toUpperCase() + "'";
			//System.out.println("* <"+sql+">");
			ResultSet result = statement.executeQuery(sql);
			boolean exists = result.next();
			statement.close();
			//System.out.println("* table "+name.toUpperCase()+" "+((exists)?"":"not ")+"exists");
			if (!exists) {
				statement = connection.createStatement();
				sql = "select * from user_views where view_name like '" + name.toUpperCase() + "'";
				//System.out.println("* <"+sql+">");
				result = statement.executeQuery(sql);
				exists = result.next();
				statement.close();
				//System.out.println("* view "+name.toUpperCase()+" "+((exists)?"":"not ")+"exists");
			}
			return exists;
		} catch (SQLException se) {
			System.out.println("* <" + sql + ">");
			System.out.println("Cannot check the existance of table " + se.toString());
			return false;
		}
	}

	public void dropTmpTable(String tmpName) {
		if (checkIfTableOrViewExists(tmpName)) {
			super.dropTmpTable(tmpName);
		}
	}

	/**
	* Retrieves the list of column names from the database and fills the vector
	* columns with these names. Columns with geographical information are ignored (put
	* separately in the vector geoColumns). Both vectors must be initialized
	* before calling this method.
	*/
	public Vector getColumnsOfTable(Vector toFill, String tableName) {
		if (connection == null)
			return null;
		Vector v = (toFill == null) ? new Vector(20, 20) : toFill;
		try {
			Statement statement = connection.createStatement();
			statement.setMaxRows(2);
			String sqlString = SQLStatements.getSQL(0, tableName); // +" "+TABLE_ALIAS;
			System.out.println("* <" + sqlString + ">");
			ResultSet result = statement.executeQuery(sqlString);
			ResultSetMetaData md = result.getMetaData();
			int ncols = md.getColumnCount();
			for (int i = 1; i <= ncols; i++) {
				boolean isGeoColumn = false;
				try {
					String typename = md.getColumnTypeName(i);
					if (typename.equalsIgnoreCase("SDO_GEOMETRY") || typename.equalsIgnoreCase("MDSYS.SDO_GEOMETRY")) {
						isGeoColumn = true;
					}
				} catch (SQLException e) {
				}
				if (isGeoColumn) {
					;//geoColumns.addElement(md.getColumnName(i));
				} else {
					v.addElement(md.getColumnName(i));
				}
			}
			result.close();
			statement.close();
		} catch (SQLException se) {
			System.out.println("Cannot get the list" + se.toString());
		}
		return v;
	}

	/**
	 * creating NoDuplicates _ND table within Trajectory preprocessor
	 */
	protected String addPrefToList(String list, String pref) {
		String result = list;
		StringBuffer sb = new StringBuffer(list);
		for (int i = list.length() - 1; i >= 0; i--)
			if (list.charAt(i) == ',') {
				sb.insert(i + 1, pref);
			}
		result = sb.toString();
		return result;
	}

	public boolean createNDtable(boolean bCleanDuplicates, String tname, String idStr, String dummyID, String dtStr, String xStr, String yStr, String extraAttrStr) {
		long tStart = System.currentTimeMillis();
		this.dropTmpTable(tname + "ND");
		boolean useDummy = idStr == null || idStr.length() == 0;
		String id_dt = (useDummy) ? dtStr : idStr + "," + dtStr;
		try {
			String sqlString = "";
			if (bCleanDuplicates) {
				sqlString = "create table " + tname + "_nd as\n" + "select " + ((useDummy) ? "'" + dummyID + "'" : "a." + idStr) + " as ID_, a." + dtStr + " as DT_, a.X_, a.Y_ " + ((extraAttrStr == null) ? "" : addPrefToList(extraAttrStr, "b."))
						+ " \n" + "from\n" + "  (select min(rrr_) as rrr_orig_, avg(" + xStr + ") as X_, avg(" + yStr + ") as Y_, " + id_dt + " from \n" + "     (select rownum as rrr_, aa.* from (select * from " + tname + " order by " + id_dt
						+ ") aa) \n" + "   group by " + id_dt + " order by min(rrr_)) a,\n" + "  (select rownum as rrr_, aa.* from (select * from " + tname + " order by " + id_dt + ") aa) b\n" + "where a.rrr_orig_=b.rrr_";
			} else {
				sqlString = "create view " + tname + "_nd as\n" + "select " + ((useDummy) ? "'" + dummyID + "'" : idStr) + " as ID_, " + dtStr + " as DT_, " + xStr + " as X_, " + yStr + " as Y_ "
						+ ((extraAttrStr == null || extraAttrStr.trim().length() == 0) ? "" : extraAttrStr) + " \n" + "from " + tname;
			}
			System.out.println("* <" + sqlString + ">");
			Statement statement = connection.createStatement();
			boolean b = statement.execute(sqlString);
			statement.close();
			//System.out.println("* result1="+b);
			long tEnd = System.currentTimeMillis() - tStart;
			System.out.println("* Ready. Elapsed time " + StringUtil.floatToStr(tEnd / 1000f, 3) + " (s)");
			return true;
		} catch (SQLException se) {
			System.out.println("Cannot get the list" + se.toString());
			return false;
		}
	}

	public long[] countStationaryPoints(String tname, float speed, String attr) {
		long n[] = new long[2];
		try {
			String sqlString = "select count(*) from " + tname + " a, " + tname + " b, " + tname + " c\n" + "where (a.speed_c<" + speed + " and \n" + "(b.speed_c<" + speed + " and b.tid_=a.tid_ and b.tnum_=a.tnum_-1) and \n" + "(c.speed_c<" + speed
					+ " and c.tid_=a.tid_ and c.tnum_=a.tnum_+1)) ";
			if (attr != null) {
				sqlString += "\nor\n" + "(a." + attr + "<" + speed + " and \n" + "(b." + attr + "<" + speed + " and b.tid_=a.tid_ and b.tnum_=a.tnum_-1) and \n" + "(c." + attr + "<" + speed + " and c.tid_=a.tid_ and c.tnum_=a.tnum_+1)) ";
			}

			System.out.println("* <" + sqlString + ">");
			Statement statement = connection.createStatement();
			ResultSet r = statement.executeQuery(sqlString);
			r.next();
			n[0] = r.getLong(1);
			statement.close();
			sqlString = "select count(*) from " + tname;
			System.out.println("* <" + sqlString + ">");
			statement = connection.createStatement();
			r = statement.executeQuery(sqlString);
			r.next();
			n[1] = r.getLong(1);
			statement.close();
		} catch (SQLException se) {
			System.out.println("SQL error:" + se.toString());
		}
		return n;
	}

	public boolean removeStationaryPoints(String trajTname, String tname, float speed, String speedAttr, String extraAttrStr) {
		try {
			String sqlString = "create table " + tname + "_NDR" + " as\n" + "select a.nd_rowid_ from " + trajTname + " a, " + trajTname + " b, " + trajTname + " c\n" + "where (a.speed_c<" + speed + " and \n" + "(b.speed_c<" + speed
					+ " and b.tid_=a.tid_ and b.tnum_=a.tnum_-1) and \n" + "(c.speed_c<" + speed + " and c.tid_=a.tid_ and c.tnum_=a.tnum_+1)) ";
			if (speedAttr != null) {
				sqlString += "\nor\n" + "(a." + speedAttr + "<" + speed + " and \n" + "(b." + speedAttr + "<" + speed + " and b.tid_=a.tid_ and b.tnum_=a.tnum_-1) and \n" + "(c." + speedAttr + "<" + speed
						+ " and c.tid_=a.tid_ and c.tnum_=a.tnum_+1)) ";
			}
			System.out.println("* <" + sqlString + ">");
			Statement statement = connection.createStatement();
			long tStart = System.currentTimeMillis();
			statement.execute(sqlString);
			sqlString = "create table " + tname + "_NDS" + " as\n" + "select rownum as rrr_, a.id_, a.dt_, a.x_, a.y_" + addPrefToList(extraAttrStr, "a.") + " from " + tname + "_ND a\n" + "where not exists\n"
					+ "(select /*+ INDEX (b create index tmp_idx on " + tname + "_NDR (nd_rowid_)) */ nd_rowid_ from " + tname + "_NDR b where a.rowid = b.nd_rowid_)";
			System.out.println("* <" + sqlString + ">");
			statement.execute(sqlString);
			statement.close();
			long tEnd = System.currentTimeMillis() - tStart;
			System.out.println("* Ready. Elapsed time " + StringUtil.floatToStr(tEnd / 1000f, 3) + " (s)");
		} catch (SQLException se) {
			System.out.println("SQL error:" + se.toString());
			return false;
		}
		return true;
	}

	protected void createCreateCircleFunction() {
		try {
			String sqlString = "select count(*) from user_source where name='GREAT_CIRCLE'";
			Statement statement = connection.createStatement();
			ResultSet r = statement.executeQuery(sqlString);
			r.next();
			if (r.getInt(1) > 0) // function already exists
				return;
			sqlString = "CREATE OR REPLACE FUNCTION GREAT_CIRCLE(lat1 number, lon1 number,\n" + "                                        lat2 number, lon2 number)\n" + "RETURN NUMBER AUTHID CURRENT_USER AS\n" + "BEGIN\n" + "        DECLARE\n"
					+ "                PI NUMBER := 3.1415926532;\n" + "                la1 NUMBER := PI / 180 * lat1;\n" + "                lo1 NUMBER := PI / 180 * lon1;\n" + "                la2 NUMBER := PI / 180 * lat2;\n"
					+ "                lo2 NUMBER := PI / 180 * lon2;\n" + "        BEGIN\n" + "                RETURN 6372.795 * 2 * \n" + "                       ASIN(SQRT(POWER(SIN((la2 - la1) / 2), 2) +\n"
					+ "                                 COS(la1) * COS(la2) *\n" + "                                 POWER(SIN((lo2 - lo1) / 2), 2)));\n" + "        END;\n" + "END;";
			System.out.println("* <" + sqlString + ">");
			statement = connection.createStatement();
			boolean b = statement.execute(sqlString);
			statement.close();
		} catch (SQLException se) {
			System.out.println("SQL error:" + se.toString());
		}
	}

	/**
	 * Creates table name_SE with starts, ends, and statistics of trajectories
	 */
	public void createSEtable(String tableName, int coordsAreGeographic) {
		long tStart = System.currentTimeMillis();
		if (checkIfTableOrViewExists(tableName + "_se"))
			return;
		try {
			Statement statement = connection.createStatement();
			String sql = "create table "
					+ tableName
					+ "_se as\n"
					+ "select starts.id_, starts.tid_, starts.sdt, starts.sx, starts.sy,\n"
					+ "ends.edt, ends.ex, ends.ey, \n"
					+ "ends.tnum_ as npoints, \n"
					+ "ends.edt-starts.sdt as duration,\n"
					+ "starts.distance,\n"
					+
					//
					((coordsAreGeographic == 1) ? "great_circle(starts.sy,starts.sx,ends.ey,ends.ex) as displacement,\n" + "great_circle(starts.y1,starts.x1,starts.y2,starts.x2) as br_diagonal,\n"
							: "sqrt(power(starts.sy-ends.ey,2)+power(starts.sx-ends.ex,2)) as displacement,\n" + "sqrt(power(starts.y2-starts.y1,2)+power(starts.x2-starts.x1,2)) as br_diagonal,\n")
					+
					//
					"starts.x1 as br_x1, starts.y1 as br_y1, starts.x2 as br_x2, starts.y2 as br_y2\n" + "from\n" + "(select \n" + "a.id_, a.tid_, a.tnum_, b.distance, a.dt_ as sdt, a.x_ as sx, a.y_ as sy, b.x1, b.y1, b.x2, b.y2\n" + "from "
					+ tableName + " a,\n" + "(select tid_,max(tnum_) as maxtnum, sum(distance_) as distance, min(x_) as x1, max(x_) as x2, min(y_) as y1, max(y_) as y2 from " + tableName + " group by tid_) b\n"
					+ "where a.tid_=b.tid_ and a.tnum_=1) starts,\n" + "(select \n" + "a.id_, a.tid_, a.tnum_, a.dt_ as edt, a.x_ as ex, a.y_ as ey\n" + "from " + tableName + " a,\n" + "(select tid_,max(tnum_) as maxtnum from " + tableName
					+ " group by tid_) b\n" + "where a.tid_=b.tid_ and a.tnum_=b.maxtnum) ends\n" + "where starts.tid_=ends.tid_\n" + "order by starts.tid_";
			System.out.println("* <" + sql + ">");
			statement.executeUpdate(sql);
			long tEnd = System.currentTimeMillis() - tStart;
			System.out.println("* Ready. Elapsed time " + StringUtil.floatToStr(tEnd / 1000f, 3) + " (s)");
		} catch (SQLException se) {
			System.out.println("Cannot get the list" + se.toString());
		}

	}

	public boolean createTrajectories(String newTName, String tname, String idStr, String dummyID, String dtStr, String xStr, String yStr, boolean bCoordinatesAreGeo, boolean bTimeIsPhysical) {
		try {
			createCreateCircleFunction();
			dropTmpTable(newTName);
			//dropTmpTable(tname+"__");
			String sqlString = "create table " + newTName + " as \n" + "select aaaa.id_ as tid_, aaaa.*,\n" + "  case when DIFFTIME_>0 then (SPEED_C-LAG(SPEED_C) OVER (partition by ID_ order by DT_ desc))/(DIFFTIME_"
					+ ((bTimeIsPhysical) ? "*24*3600" : "") + ") else null end as ACCELERATION_C,\n" + "  COURSE_C-lag(COURSE_C) over (partition by ID_ order by DT_ desc) as TURN_C\n" + "from\n" + "(\n" + "select aaa.*,\n"
					+ "  case when DIFFTIME_>0 then DISTANCE_/(DIFFTIME_" + ((bTimeIsPhysical) ? "*24" : "") + ") else null end as SPEED_C,\n" + "  case \n" + "    when DY_>0 then \n" + "      case \n"
					+ "        when DX_>0 then 180*atan(DX_/DY_)/3.141592653589793238462643383279502884197169399375105820974944\n" + "        else 360+180*atan(DX_/DY_)/3.141592653589793238462643383279502884197169399375105820974944 \n"
					+ "      end\n" + "    when DY_<0 then 180+180*atan(DX_/DY_)/3.141592653589793238462643383279502884197169399375105820974944\n" + "    else\n" + "      case when DX_=0 then null else 180-90*sign(DX_) end \n"
					+ "  end as COURSE_C\n" + "from\n" + "(\n" + "select aa.*,\n" + "  NEXTTIME_-DT_ as DIFFTIME_,\n" + "  NEXTX_-X_ as DX_,\n" + "  NEXTY_-Y_ as DY_,\n" + "  "
					+ ((bCoordinatesAreGeo) ? "GREAT_CIRCLE(Y_,X_,NEXTY_,NEXTX_)" : "SQRT(POWER(NEXTX_-X_,2)+POWER(NEXTY_-Y_,2))") + " as DISTANCE_\n" + "from\n" + "(\n" + "select /*+ INDEX (a create index tmp_idx on " + tname
					+ " (id_,dt_)) */ a.rowid as nd_rowid_, a.*, \n" + "  count(*) over (partition by ID_ order by DT_ asc) as tnum_, \n" + "  lag(X_) over (partition by ID_ order by DT_ desc) as NEXTX_,\n"
					+ "  lag(Y_) over (partition by ID_ order by DT_ desc) as NEXTY_,\n" + "  lag(DT_) over (partition by ID_ order by DT_ desc) as NEXTTIME_\n" + "from " + tname + " a\n" + ") aa\n" + "order by ID_,DT_\n" + ") aaa\n"
					+ "order by ID_,DT_\n" + ") aaaa\n" + "order by ID_,DT_ ";
			System.out.println("* <" + sqlString + ">");
			Statement statement = connection.createStatement();
			long tStart = System.currentTimeMillis();
			boolean b = statement.execute(sqlString);
			statement.close();
			//System.out.println("* result="+b);
			long tEnd = System.currentTimeMillis() - tStart;
			System.out.println("* Ready. Elapsed time " + StringUtil.floatToStr(tEnd / 1000f, 3) + " (s)");
		} catch (SQLException se) {
			System.out.println("SQL error:" + se.toString());
			return false;
		}
		return true;
	}

/* old version, as on December 29, 2010
  public boolean createTrajectories (String newTName, String tname, String idStr, String dummyID,
                                     String dtStr, String xStr, String yStr,
                                     boolean bCoordinatesAreGeo, boolean bTimeIsPhysical)
  {
    try {
      createCreateCircleFunction();
      dropTmpTable(newTName);
      dropTmpTable(tname+"__");
      String sqlString="create table "+tname+"__ as \n"+
          "select aaaa.*,\n"+
          "  case when DIFFTIME_>0 then (SPEED_C-LAG(SPEED_C) OVER (partition by ID_ order by DT_ desc))/(DIFFTIME_"+((bTimeIsPhysical)?"*24*3600":"")+") else null end as ACCELERATION_C,\n"+
          "  COURSE_C-lag(COURSE_C) over (partition by ID_ order by DT_ desc) as TURN_C\n"+
          "from\n"+
          "(\n"+
          "select aaa.*,\n"+
          "  case when DIFFTIME_>0 then DISTANCE_/(DIFFTIME_"+((bTimeIsPhysical)?"*24":"")+") else null end as SPEED_C,\n"+
          "  case \n"+
          "    when DY_>0 then \n"+
          "      case \n"+
          "        when DX_>0 then 180*atan(DX_/DY_)/3.141592653589793238462643383279502884197169399375105820974944\n"+
          "        else 360+180*atan(DX_/DY_)/3.141592653589793238462643383279502884197169399375105820974944 \n"+
          "      end\n"+
          "    when DY_<0 then 180+180*atan(DX_/DY_)/3.141592653589793238462643383279502884197169399375105820974944\n"+
          "    else\n"+
          "      case when DX_=0 then null else 180-90*sign(DX_) end \n"+
          "  end as COURSE_C\n"+
          "from\n"+
          "(\n"+
          "select aa.*,\n"+
          "  NEXTTIME_-DT_ as DIFFTIME_,\n"+
          "  NEXTX_-X_ as DX_,\n"+
          "  NEXTY_-Y_ as DY_,\n"+
          "  "+
          ((bCoordinatesAreGeo)?
             "GREAT_CIRCLE(Y_,X_,NEXTY_,NEXTX_)":
             "SQRT(POWER(NEXTX_-X_,2)+POWER(NEXTY_-Y_,2))")
          +" as DISTANCE_\n"+
          "from\n"+
          "(\n"+
          "select a.rowid as nd_rowid_, a.*, \n"+
          "  lag(X_) over (partition by ID_ order by DT_ desc) as NEXTX_,\n"+
          "  lag(Y_) over (partition by ID_ order by DT_ desc) as NEXTY_,\n"+
          "  lag(DT_) over (partition by ID_ order by DT_ desc) as NEXTTIME_\n"+
          "from "+tname+" a\n"+
          ") aa\n"+
          "order by ID_,DT_\n"+
          ") aaa\n"+
          "order by ID_,DT_\n"+
          ") aaaa\n"+
          "order by ID_,DT_ ";
      System.out.println("* <"+sqlString+">");
      Statement statement=connection.createStatement();
      long tStart=System.currentTimeMillis();
      boolean b=statement.execute(sqlString);
      statement.close();
      //System.out.println("* result="+b);
      long tEnd=System.currentTimeMillis()-tStart;
      System.out.println("* Ready. Elapsed time "+StringUtil.floatToStr(tEnd/1000f,3)+" (s)");
      sqlString="create table "+newTName+" as\n"+
          "select a.id_ as tid_, a.rrr_-b.rrr_min+1 as tnum_, a.*, 0 as difftime_next_, 0 as distance_next_\n"+
          "from "+tname+"__ a,\n"+
          "(select id_, min(rrr_) as rrr_min \n"+
          "from "+tname+"__ \n"+
          "group by id_) b\n"+
          "where a.id_=b.id_";
      System.out.println("* <"+sqlString+">");
      statement=connection.createStatement();
      tStart=System.currentTimeMillis();
      b=statement.execute(sqlString);
      statement.close();
      //System.out.println("* result="+b);
      tEnd=System.currentTimeMillis()-tStart;
      System.out.println("* Ready. Elapsed time "+StringUtil.floatToStr(tEnd/1000f,3)+" (s)");
    } catch (SQLException se) {
      System.out.println("SQL error:"+se.toString());
      return false;
    }
    return true;
  }

*/
	public boolean deriveMovementCharacteristics(String newTName, String tname, String idStr, String dummyID, String dtStr, String xStr, String yStr, String extraAttrStr, boolean bCoordinatesAreGeo, boolean bTimeIsPhysical, boolean bCleanDuplicates) {
		try {
			createCreateCircleFunction();
			dropTmpTable(newTName);
			dropTmpTable(newTName + "_00");
			String sqlString = "create table " + newTName + "_00 as \n" + "select ID_, ID_ as TID_, TNUM_, DT_, NEXTTIME_, NEXTTIME_-DT_ as DIFFTIME_,\n" + "X_, NEXTX_, NEXTX_-X_ as DX_,\n" + "Y_, NEXTY_, NEXTY_-Y_ as DY_\n"
					+ ((extraAttrStr == null || extraAttrStr.length() == 0) ? "," : extraAttrStr + ",\n") + ((bCoordinatesAreGeo) ? "GREAT_CIRCLE(Y_,X_,NEXTY_,NEXTX_)" : "SQRT(POWER(NEXTX_-X_,2)+POWER(NEXTY_-Y_,2))") + " as DISTANCE_\n" + "from (\n";
			if (idStr == null || idStr.length() == 0) {
				sqlString += "select \n" + "\'"
						+ dummyID
						+ "\' as ID_,\n"
						+ dtStr
						+ " as DT_,\n"
						+ "ROW_NUMBER() OVER (order by "
						+ dtStr
						+ " asc) as TNUM_,\n"
						+ "LAG("
						+ dtStr
						+ ") OVER (order by "
						+ dtStr
						+ " desc) as NEXTTIME_,\n"
						+ "LAG("
						+ dtStr
						+ ") OVER (order by "
						+ dtStr
						+ " desc) - "
						+ dtStr
						+ " as DIFFTIME_,\n"
						+ xStr
						+ " as X_,\n"
						+ "LAG("
						+ xStr
						+ ") OVER (order by "
						+ dtStr
						+ " desc) as NEXTX_,\n"
						+ yStr
						+ " as Y_,\n"
						+ "LAG("
						+ yStr
						+ ") OVER (order by "
						+ dtStr
						+ " desc) as NEXTY_\n"
						+ ((extraAttrStr == null || extraAttrStr.length() == 0) ? "" : "\n" + extraAttrStr)
						+ "\n"
						+ "from "
						+ ((bCleanDuplicates) ? "\n(select * FROM " + tname + "\n" + "WHERE ROWID NOT IN\n" + "  (SELECT ROWID FROM\n" + "  (SELECT ROWID, ROW_NUMBER() OVER (PARTITION BY " + dtStr + " ORDER BY " + dtStr + ") R FROM " + tname + ")\n"
								+ "  WHERE R>1))" : tname) + "\norder by DT_)";
			} else {
				sqlString += "select \n"
						+ idStr
						+ " as ID_,\n"
						+ // 0: oid
						dtStr
						+ " as DT_,\n"
						+ // 1: time
						"ROW_NUMBER() OVER (partition by "
						+ idStr
						+ " order by "
						+ dtStr
						+ " asc) as TNUM_,\n"
						+ "LAG("
						+ dtStr
						+ ") OVER (partition by "
						+ idStr
						+ " order by "
						+ dtStr
						+ " desc) as NEXTTIME_,\n"
						+ "LAG("
						+ dtStr
						+ ") OVER (partition by "
						+ idStr
						+ " order by "
						+ dtStr
						+ " desc) - "
						+ dtStr
						+ " as DIFFTIME_,\n"
						+ xStr
						+ " as X_,\n"
						+ "LAG("
						+ xStr
						+ ") OVER (partition by "
						+ idStr
						+ " order by "
						+ dtStr
						+ " desc) as NEXTX_,\n"
						+ yStr
						+ " as Y_,\n"
						+ "LAG("
						+ yStr
						+ ") OVER (partition by "
						+ idStr
						+ " order by "
						+ dtStr
						+ " desc) as NEXTY_\n"
						+ ((extraAttrStr == null || extraAttrStr.length() == 0) ? "" : extraAttrStr + "\n")
						+ "from "
						+ ((bCleanDuplicates) ? "\n(select * FROM " + tname + "\n" + "WHERE ROWID NOT IN\n" + "  (SELECT ROWID FROM\n" + "  (SELECT ROWID, ROW_NUMBER() OVER (PARTITION BY " + idStr + ", " + dtStr + " ORDER BY " + idStr + ", " + dtStr
								+ ") R FROM " + tname + ")\n" + "  WHERE R>1))" : tname) + "\norder by ID_,DT_)";
			}
			System.out.println("* <" + sqlString + ">");
			Statement statement = connection.createStatement();
			boolean b = statement.execute(sqlString);
			statement.close();
			System.out.println("* result=" + b);
			sqlString = "create table "
					+ newTName
					+ " as \n"
					+ "select ID_, TID_, TNUM_, DT_, NEXTTIME_, DIFFTIME_,\n"
					+ "X_, NEXTX_, DX_, Y_, NEXTY_, DY_, \n"
					+ "DISTANCE_, COURSE_C, SPEED_C,\n"
					+ "COURSE_C-LAG(COURSE_C) OVER (partition by ID_ order by DT_ ) as TURN_C,\n"
					+ ((bTimeIsPhysical) ? "case when (DIFFTIME_)>0 then (SPEED_C-LAG(SPEED_C) OVER (partition by ID_ order by DT_ ))/(DIFFTIME_*24*3600) else null end as ACCELERATION_C\n"
							: "case when (DIFFTIME_)>0 then (SPEED_C-LAG(SPEED_C) OVER (partition by ID_ order by DT_ ))/DIFFTIME_ else null end as ACCELERATION_C\n") + ", 0 as DIFFTIME_NEXT_, 0 as DISTANCE_NEXT_\n"
					+ ((extraAttrStr == null || extraAttrStr.length() == 0) ? "" : extraAttrStr + "\n") + "from (\n" + "select ID_, TID_, TNUM_, DT_, NEXTTIME_, DIFFTIME_,\n" + "X_, NEXTX_, DX_, Y_, NEXTY_, DY_, DISTANCE_,\n"
					+ "case when dy_>0 then \n" + "case when dx_>0 then 180*atan(DX_/DY_)/3.141592653589793238462643383279502884197169399375105820974944\n"
					+ "else 360+180*atan(DX_/DY_)/3.141592653589793238462643383279502884197169399375105820974944 end\n" + "when dy_<0 then 180+180*atan(DX_/DY_)/3.141592653589793238462643383279502884197169399375105820974944\n" + "else \n"
					+ "case when dx_=0 then null else 180-90*sign(DX_) end end as COURSE_C,\n"
					+
					//"case when DY_=0 then case when DX_=0 then null else 180+180*sign(DX_) end else 180+360*atan(DX_/DY_)/3.141592653589793238462643383279502884197169399375105820974944 end as COURSE_C,\n"+
					((bTimeIsPhysical) ? "case when (DIFFTIME_)>0 then DISTANCE_/(24*DIFFTIME_) else null end as SPEED_C\n" : "case when (DIFFTIME_)>0 then DISTANCE_/DIFFTIME_ else null end as SPEED_C\n")
					+ ((extraAttrStr == null || extraAttrStr.length() == 0) ? "" : extraAttrStr + "\n") + "from " + newTName + "_00)";
			System.out.println("* <" + sqlString + ">");
			statement = connection.createStatement();
			b = statement.execute(sqlString);
			statement.close();
			System.out.println("* result1=" + b);
			dropTmpTable(newTName + "_00");
		} catch (SQLException se) {
			System.out.println("SQL error:" + se.toString());
			return false;
		}
		return true;
	}

	public int getCountOfTrajectories(String tname, String idStr, String dtStr, String nextDtStr, String trIdColumn, String conditionStr, String conditionAttrStr) {
		try {
			Statement statement = connection.createStatement();
			statement.setMaxRows(2);
			String sqlString = "select count(*) from " + "(select rownum TID_, a.*, endtime_-starttime_ as duration_ from \n" + "(select EID_, starttime_, min(endtime_) as endtime_ from \n" + "(select EID_, starttime_, endtime_ from \n" + "(select "
					+ idStr + " as EID_, starttime_ from \n" + "(select " + idStr + ", min(" + dtStr + ") as starttime_ from " + tname + " group by " + idStr + ((trIdColumn == null) ? "" : ",TID_") + " union all select " + idStr + ", " + nextDtStr
					+ " as starttime_ from " + tname + " \n" + "where (" + conditionAttrStr + " is null or " + conditionStr + ") order by " + idStr + " asc) a) a,\n" + "(select " + idStr + " as EID_1, " + dtStr + " as endtime_ from " + tname
					+ " a \n" + "where (" + conditionAttrStr + " is null or " + conditionStr + ") order by " + idStr + " asc, " + dtStr + " asc) b\n" + "where a.EID_ = b.EID_1 and a.starttime_ <= b.endtime_) \n" + "group by EID_, starttime_ \n"
					+ "order by EID_, starttime_) a)";
			System.out.println("* <" + sqlString + ">");
			ResultSet result = statement.executeQuery(sqlString);
			result.next();
			return result.getInt(1);
		} catch (SQLException se) {
			System.out.println("SQL error:" + se.toString());
		}
		return 0;
	}

	public Vector getDetailsOfTrajectories(String tname, String idStr, String dtStr, String nextDtStr, String trIdColumn, String conditionStr, String conditionAttrStr) {
		try {
			Statement statement = connection.createStatement();
			statement.setMaxRows(1000);
			String sqlString = "select * from \n" + "(select rownum TID_, a.*, endtime_-starttime_ as duration_ from \n" + "(select EID_, starttime_, min(endtime_) as endtime_ from \n" + "(select EID_, starttime_, endtime_ from \n" + "(select "
					+ idStr + " as EID_, starttime_ from \n" + "(select " + idStr + ", min(" + dtStr + ") as starttime_ from " + tname + " group by " + idStr + ((trIdColumn == null) ? "" : ",TID_") + " union all select " + idStr + ", " + nextDtStr
					+ " as starttime_ from " + tname + " \n" + "where (" + conditionAttrStr + " is null or " + conditionStr + ") order by " + idStr + " asc) a) a,\n" + "(select " + idStr + " as EID_1, " + dtStr + " as endtime_ from " + tname
					+ " a \n" + "where (" + conditionAttrStr + " is null or " + conditionStr + ") order by " + idStr + " asc, " + dtStr + " asc) b\n" + "where a.EID_ = b.EID_1 and a.starttime_ <= b.endtime_) \n" + "group by EID_, starttime_ \n"
					+ "order by EID_, starttime_) a)";
			System.out.println("* <" + sqlString + ">");
			ResultSet result = statement.executeQuery(sqlString);
			Vector v = new Vector(100, 40);
			while (result.next()) {
				String s[] = new String[5];
				for (int i = 1; i <= 5; i++) {
					s[i - 1] = result.getString(i);
				}
				v.add(s);
			}
			return v;
		} catch (SQLException se) {
			System.out.println("SQL error:" + se.toString());
		}
		return null;
	}

	public int getTrajectories_Step01(String destTName, String srcTName, String idStr, String dtStr, String nextDtStr, String trIdColumn, String conditionStr, String conditionAttrStr) {
		try {
			Statement statement = connection.createStatement();
			statement.setMaxRows(2);
			String sqlString = "create table " + destTName + " as \nselect * from \n" + "(select trim(EID_) || '_' || lpad(row_number() over (partition by a.eid_ order by starttime_ asc),3,' ') as TID_, a.* from \n"
					+ "(select EID_, starttime_, min(endtime_) as endtime_ from \n" + "(select EID_, starttime_, endtime_ from \n" + "(select " + idStr + " as EID_, starttime_ from \n" + "(select " + idStr + ", min(" + dtStr
					+ ") as starttime_ from " + srcTName + " group by " + idStr + ((trIdColumn == null) ? "" : ",TID_") + " union all select " + idStr + ", " + nextDtStr + " as starttime_ from " + srcTName + " \n" + "where (" + conditionAttrStr
					+ " is null or " + conditionStr + ") order by " + idStr + " asc) a) a,\n" + "(select " + idStr + " as EID_1, " + dtStr + " as endtime_ from " + srcTName + " a \n" + "where (" + conditionAttrStr + " is null or " + conditionStr
					+ ") order by " + idStr + " asc, " + dtStr + " asc) b\n" + "where a.EID_ = b.EID_1 and a.starttime_ <= b.endtime_) \n" + "group by EID_, starttime_ \n" + "order by EID_, starttime_) a)";
			System.out.println("* <" + sqlString + ">");
			boolean b = statement.execute(sqlString);
			statement.close();
			System.out.println("* result=" + b);
			return 1;
		} catch (SQLException se) {
			System.out.println("SQL error:" + se.toString());
		}
		return 0;
	}

	public int getTrajectories_Step02(String tableSrc0, String tableSrc1, String tableDest2, String extraAttrs, String extraAttrs1) {
		try {
			Statement statement = connection.createStatement();
			statement.setMaxRows(2);
			String sqlString = "create table " + tableDest2 + " as \n" + "" + "select \n"
					+ "a.TID_, a.TNUM_ - b.TNUM_ + 1 as TNUM_, a.ID_, a.DT_, a.NEXTTIME_, a.DIFFTIME_, a.X_, a.Y_, a.DX_, a.DY_, a.DISTANCE_, a.SPEED_C, a.ACCELERATION_C, a.COURSE_C, a.TURN_C " + extraAttrs + "\n" + "from \n"
					+ "(select rownum as TNUM_, a.* from \n" + "(select a.TID_, a.starttime_, a.endtime_, b.* from \n" + tableSrc1 + " a, \n" + ((extraAttrs1 == null) ? tableSrc0 : "(select " + extraAttrs1 + " from " + tableSrc0 + ")") + " b \n"
					+ "where a.EID_ = b.ID_ and b.DT_ between a.starttime_ and a.endtime_ order by ID_, DT_) a) a, \n" + "(select a.ID_, a.starttime_, min(TNUM_) as TNUM_ from \n" + "(select rownum as TNUM_, a.ID_, a.starttime_ from \n"
					+ "(select a.TID_, a.starttime_, a.endtime_, b.* from \n" + tableSrc1 + " a, \n" + ((extraAttrs1 == null) ? tableSrc0 : "(select " + extraAttrs1 + " from " + tableSrc0 + ")") + " b \n"
					+ "where a.EID_ = b.ID_ and b.DT_ between a.starttime_ and a.endtime_ order by ID_, DT_) a) a group by a.ID_, a.starttime_) b\n" + "where a.ID_ = b.ID_ and a.starttime_ = b.starttime_\n" + "order by a.TID_, a.TNUM_";

			System.out.println("* <" + sqlString + ">");
			boolean b = statement.execute(sqlString);
			statement.close();
			System.out.println("* result=" + b);
			return 1;
		} catch (SQLException se) {
			System.out.println("SQL error:" + se.toString());
		}
		return 0;
	}

	public int getTrajectories_Step03(String tableSrc2, String tableDest3, String extraAttrs) {
		try {
			Statement statement = connection.createStatement();
			statement.setMaxRows(2);
			/*
			String st[]=new String[4];
			st[0]=tableDest3;
			st[2]=st[3]=tableSrc2;
			st[1]=extraAttrs;*/
			String sqlString = "create table " + tableDest3 + " as\n" + "select a.ID_, a.TNUM_, a.TID_, a.DT_, a.X_, a.Y_,\n" + "(case when a.DT_ = b.DT_ and a.TID_ = b.TID_ then null else a.DX_ end) as DX_,\n"
					+ "(case when a.DT_ = b.DT_ and a.TID_ = b.TID_ then null else a.DY_ end) as DY_,\n" + "(case when a.DT_ = b.DT_ and a.TID_ = b.TID_ then null else a.SPEED_C end) as SPEED_C,\n"
					+ "(case when a.DT_ = b.DT_ and a.TID_ = b.TID_ then null else a.COURSE_C end) as COURSE_C,\n" + "(case when a.DT_ = b.DT_ and a.TID_ = b.TID_ then null else a.NEXTTIME_ end) as NEXTTIME_,\n"
					+ "(case when a.DT_ = b.DT_ and a.TID_ = b.TID_ then null else a.DIFFTIME_ end) as DIFFTIME_,\n" + "(case when a.DT_ = b.DT_ and a.TID_ = b.TID_ then null else a.DISTANCE_ end) as DISTANCE_, \n"
					+ "(case when a.DT_ = b.DT_ and a.TID_ = b.TID_ then a.DIFFTIME_ else null end) as DIFFTIME_NEXT_,\n" + "(case when a.DT_ = b.DT_ and a.TID_ = b.TID_ then a.DISTANCE_ else null end) as DISTANCE_NEXT_, \n"
					+ "ACCELERATION_C, TURN_C " + extraAttrs + "\n" + "from " + tableSrc2 + " a, (select max(DT_) as DT_, TID_ from " + tableSrc2 + " group by TID_) b where a.TID_ = b.TID_";
			System.out.println("* <" + sqlString + ">");
			boolean b = statement.execute(sqlString);
			statement.close();
			System.out.println("* result=" + b);
			return 1;
		} catch (SQLException se) {
			System.out.println("SQL error:" + se.toString());
		}
		return 0;
	}

	public void getMostFrequentValueForGroups(String tblName, String columnName, Vector vGroups, Vector vVals) {
		try {
			if (connection == null) {
				openConnection(false);
			}
			Statement statement = connection.createStatement();
			String sqlString = "select a.tid_, a." + columnName + " from (\n" + "select tid_, " + columnName + ", count(*) c from " + tblName + " group by tid_, " + columnName + ") a,\n" + "(select tid_, max(c) c from (select tid_, " + columnName
					+ ", count(*) c from " + tblName + " group by tid_, " + columnName + ") group by tid_) b\n" + "where a.tid_ = b.tid_ and a.c = b.c";
			System.out.println("* <" + sqlString + ">");
			long tStart = System.currentTimeMillis();
			ResultSet result = statement.executeQuery(sqlString);
			long tEnd = System.currentTimeMillis() - tStart;
			System.out.println("* Ready. Elapsed time " + StringUtil.floatToStr(tEnd / 1000f, 3) + " (s)");
			while (result.next()) {
				vGroups.addElement(result.getString(1));
				vVals.addElement(result.getString(2));
			}
		} catch (SQLException se) {
			System.out.println("SQL error:" + se.toString());
		}
	}

	public Vector getTrajectoryAggregates(String tblName, String attr[], int attrAggrType[]) {
		Vector v = new Vector(20, 10);
		try {
			if (connection == null) {
				openConnection(false);
			}
			String tblResult1 = tblName + "_tmp1", tblResult2 = tblName + "_tmp2";
			dropTmpTable(tblResult1);
			Statement statement = connection.createStatement();
			String sqlString = "create table " + tblResult1 + " as\n" + "(select tid_, id_, max(tnum_) as N_, \n" + "min(dt_) as starttime_, max(dt_) as endtime_,\n" + "max(dt_)-min(dt_) as duration_,\n" + "sum(distance_) as distance_,\n"
					+ "max(difftime_next_) as difftime_next_, max(distance_next_) as distance_next_";
			String attrNames[] = (attr == null) ? new String[0] : new String[attr.length];
			if (attrNames.length > 0) {
				for (int i = 0; i < attr.length; i++) {
					switch (attrAggrType[i]) {
					//  "max","min","max-min","average","standard deviation","median","median & quartiles","deciles"
					case 0:
						attrNames[i] = "max_" + attr[i];
						sqlString += ",\nMAX(" + attr[i] + ") as " + attrNames[i];
						break;
					case 1:
						attrNames[i] = "min_" + attr[i];
						sqlString += ",\nMIN(" + attr[i] + ") as " + attrNames[i];
						break;
					case 2:
						attrNames[i] = "maxmin_" + attr[i];
						sqlString += ",\nMAX(" + attr[i] + ")-MIN(" + attr[i] + ") as " + attrNames[i];
						break;
					case 3:
						attrNames[i] = "avg_" + attr[i];
						sqlString += ",\nAVG(" + attr[i] + ") as " + attrNames[i];
						break;
					case 4:
						attrNames[i] = "stddev_" + attr[i];
						sqlString += ",\nSTDDEV(" + attr[i] + ") as " + attrNames[i];
						break;
					case 5:
						attrNames[i] = "median_" + attr[i];
						sqlString += ",\nMEDIAN(" + attr[i] + ") as " + attrNames[i];
						break;
					default:
						break;
					}
				}
			}
			sqlString += "\nfrom " + tblName + "\ngroup by tid_,id_)";
			System.out.println("* <" + sqlString + ">");
			long tStart = System.currentTimeMillis();
			boolean b = statement.execute(sqlString);
			statement.close();
			System.out.println("* result=" + b);
			long tEnd = System.currentTimeMillis() - tStart;
			System.out.println("* Ready. Elapsed time " + StringUtil.floatToStr(tEnd / 1000f, 3) + " (s)");
			dropTmpTable(tblResult2);
			statement = connection.createStatement();
			sqlString = "create table " + tblResult2 + " as\n" + "(select starts.id_, starts.tid_, startx_, starty_, endx_, endy_ from \n" + "(select b.x_ as startx_, b.y_ as starty_, b.tid_, b.id_ from\n" + tblResult1 + " a,\n" + tblName + " b\n"
					+ "where a.tid_=b.tid_ and a.id_=b.id_ and b.tnum_=1) starts,\n" + "(select b.x_ as endx_, b.y_ as endy_, b.tid_, b.id_ from\n" + tblResult1 + " a,\n" + tblName + " b\n"
					+ "where a.tid_=b.tid_ and a.id_=b.id_ and b.tnum_=a.n_) ends \n" + "where starts.tid_ = ends.tid_ and starts.id_ = ends.id_)";
			System.out.println("* <" + sqlString + ">");
			b = statement.execute(sqlString);
			statement.close();
			System.out.println("* result=" + b);
			statement = connection.createStatement();
			sqlString = "select \n" + "t1.*, t2.startx_, t2.starty_, t2.endx_, t2.endy_\n" + "from\n" + tblResult1 + " t1,\n" + tblResult2 + " t2\n" + "where t1.tid_=t2.tid_ and t1.id_=t2.id_ \n" +
			//tblResult2+" t2,\n"+
			//"(select id_, tid_, max(speed) as speed_max, avg(speed) as speed_avg from "+tblName+" group by id_, tid_) t3\n"+
			//"where t1.tid_=t2.tid_ and t1.id_=t2.id_ and t1.tid_=t3.tid_ and t1.id_=t3.id_ \n"+
					"order by t1.id_, t1.tid_";
			/* order of columns:
			 TID_ ID_ N_ STARTTIME_ ENDTIME_   DURATION_  DISTANCE_
			 DIFFTIME_NEXT_ DISTANCE_NEXT_
			 STARTX_    STARTY_      ENDX_      ENDY_
			*/
			System.out.println("* <" + sqlString + ">");
			tStart = System.currentTimeMillis();
			ResultSet result = statement.executeQuery(sqlString);
			tEnd = System.currentTimeMillis() - tStart;
			System.out.println("* Ready. Elapsed time " + StringUtil.floatToStr(tEnd / 1000f, 3) + " (s)");
			IntArray iaN = new IntArray(100, 100);
			Vector vEID = new Vector(100, 100), vTID = new Vector(100, 100);
			FloatArray faDuration = new FloatArray(100, 100), faDistance = new FloatArray(100, 100), faStopDuration = new FloatArray(100, 100), faDistanceNext = new FloatArray(100, 100);
			FloatArray faExtra[] = new FloatArray[attrNames.length];
			for (int i = 0; i < faExtra.length; i++) {
				faExtra[i] = new FloatArray(100, 100);
			}
			while (result.next()) {
				String tid = result.getString("tid_");
				vTID.addElement(tid);
				String eid = result.getString("id_");
				vEID.addElement(eid);
				int N = result.getInt("N_");
				iaN.addElement(N);
				float duration = result.getFloat("duration_"), distance = result.getFloat("distance_"), stopDuration = result.getFloat("difftime_next_"), distanceNext = result.getFloat("distance_next_");
				faDuration.addElement(duration);
				faDistance.addElement(distance);
				faStopDuration.addElement(stopDuration);
				faDistanceNext.addElement(distanceNext);
				for (int i = 0; i < faExtra.length; i++) {
					float f = result.getFloat(attrNames[i]);
					faExtra[i].addElement(f);
				}
			}
			dropTmpTable(tblResult1);
			dropTmpTable(tblResult2);
			v.addElement(attrNames);
			v.addElement(vTID);
			v.addElement(vEID);
			v.addElement(iaN);
			v.addElement(faDuration);
			v.addElement(faDistance);
			v.addElement(faStopDuration);
			v.addElement(faDistanceNext);
			v.addElement(faExtra);
		} catch (SQLException se) {
			System.out.println("SQL error:" + se.toString());
			return null;
		}
		return v;
	}

	public Vector getTrajectoriesStartsEnds(String tblName) {
		Vector v = null;
		try {
			if (connection == null) {
				openConnection(false);
			}
			String tblResult1 = tblName + "_tmp1", tblResult2 = tblName + "_tmp2";
			dropTmpTable(tblResult1);
			Statement statement = connection.createStatement();
			String sqlString = "create table " + tblResult1 + " as\n" + "(select tid_, id_, max(tnum_) as N_, \n" + "min(dt_) as starttime_, max(dt_) as endtime_, max(difftime_next_) as difftime_next_";
			sqlString += "\nfrom " + tblName + "\ngroup by tid_,id_)";
			System.out.println("* <" + sqlString + ">");
			long tStart = System.currentTimeMillis();
			boolean b = statement.execute(sqlString);
			statement.close();
			System.out.println("* result=" + b);
			long tEnd = System.currentTimeMillis() - tStart;
			System.out.println("* Ready. Elapsed time " + StringUtil.floatToStr(tEnd / 1000f, 3) + " (s)");
			dropTmpTable(tblResult2);
			statement = connection.createStatement();
			sqlString = "create table " + tblResult2 + " as\n" + "(select starts.id_, starts.tid_, startx_, starty_, endx_, endy_ from \n" + "(select b.x_ as startx_, b.y_ as starty_, b.tid_, b.id_ from\n" + tblResult1 + " a,\n" + tblName + " b\n"
					+ "where a.tid_=b.tid_ and a.id_=b.id_ and b.tnum_=1) starts,\n" + "(select b.x_ as endx_, b.y_ as endy_, b.tid_, b.id_ from\n" + tblResult1 + " a,\n" + tblName + " b\n"
					+ "where a.tid_=b.tid_ and a.id_=b.id_ and b.tnum_=a.n_) ends \n" + "where starts.tid_ = ends.tid_ and starts.id_ = ends.id_)";
			System.out.println("* <" + sqlString + ">");
			b = statement.execute(sqlString);
			statement.close();
			System.out.println("* result=" + b);
			statement = connection.createStatement();
			sqlString = "select \n" + "t1.*, t2.startx_, t2.starty_, t2.endx_, t2.endy_\n" + "from\n" + tblResult1 + " t1,\n" + tblResult2 + " t2\n" + "where t1.tid_=t2.tid_ and t1.id_=t2.id_ \n" + "order by t1.id_, t1.tid_";
			System.out.println("* <" + sqlString + ">");
			tStart = System.currentTimeMillis();
			ResultSet result = statement.executeQuery(sqlString);
			tEnd = System.currentTimeMillis() - tStart;
			System.out.println("* Ready. Elapsed time " + StringUtil.floatToStr(tEnd / 1000f, 3) + " (s)");
			boolean bPhysicalTime = false;
			ResultSetMetaData md = result.getMetaData();
			int timeColumnType = md.getColumnType(4); // "STARTTIME_"
			bPhysicalTime = timeColumnType == java.sql.Types.DATE || timeColumnType == java.sql.Types.TIME || timeColumnType == java.sql.Types.TIMESTAMP;
			Vector iaTID = new Vector(100, 100);
			Vector vEID = new Vector(100, 100), vStartDate = (bPhysicalTime) ? new Vector(100, 100) : null, vStartTime = (bPhysicalTime) ? new Vector(100, 100) : null, vEndDate = (bPhysicalTime) ? new Vector(100, 100) : null, vEndTime = (bPhysicalTime) ? new Vector(
					100, 100) : null;
			FloatArray faStartX = new FloatArray(100, 100), faStartY = new FloatArray(100, 100), faEndX = new FloatArray(100, 100), faEndY = new FloatArray(100, 100), faStopDuration = new FloatArray(100, 100);
			LongArray laStartTime = (bPhysicalTime) ? null : new LongArray(100, 100), laEndTime = (bPhysicalTime) ? null : new LongArray(100, 100);
			while (result.next()) {
				String tid = result.getString("tid_");
				iaTID.addElement(tid);
				String eid = result.getString("id_");
				vEID.addElement(eid);
				float startx = result.getFloat("startx_"), starty = result.getFloat("starty_"), endx = result.getFloat("endx_"), endy = result.getFloat("endy_");
				faStartX.addElement(startx);
				faStartY.addElement(starty);
				faEndX.addElement(endx);
				faEndY.addElement(endy);
				if (bPhysicalTime) {
					java.util.Date startdate = result.getDate("starttime_"), enddate = result.getDate("endtime_");
					java.sql.Time starttime = result.getTime("starttime_"), endtime = result.getTime("endtime_");
					vStartDate.addElement(startdate);
					vStartTime.addElement(starttime);
					vEndDate.addElement(enddate);
					vEndTime.addElement(endtime);
				} else {
					long starttime = result.getLong("starttime_"), endtime = result.getLong("endtime_");
					laStartTime.addElement(starttime);
					laEndTime.addElement(endtime);
				}
				float stopDuration = result.getFloat("difftime_next_");
				faStopDuration.addElement(stopDuration);
			}
			dropTmpTable(tblResult1);
			dropTmpTable(tblResult2);

			v = new Vector(20, 10);
			v.addElement(iaTID);
			v.addElement(vEID);
			if (bPhysicalTime) {
				v.addElement(vStartDate);
				v.addElement(vStartTime);
				v.addElement(vEndDate);
				v.addElement(vEndTime);
			} else {
				v.addElement(null);
				v.addElement(laStartTime);
				v.addElement(null);
				v.addElement(laEndTime);
			}
			v.addElement(faStartX);
			v.addElement(faStartY);
			v.addElement(faEndX);
			v.addElement(faEndY);
			v.addElement(faStopDuration);
		} catch (SQLException se) {
			System.out.println("SQL error:" + se.toString());
			return null;
		}
		return v;
	}

	/*
	 public Vector getPointsOfTrajectories (String tblName) {
	   Vector v=null;
	   if (connection==null) openConnection(false);
	   try {
	     Statement statement=connection.createStatement();
	     String sqlString="select tid_,x_,y_,dt_ from "+tblName;
	     ResultSet result=statement.executeQuery(sqlString);
	     v=new Vector(10000,10000);
	     while (result.next()) {
	       String id=result.getString(1);
	       float x=result.getFloat(2),
	             y=result.getFloat(3);
	       java.util.Date date=result.getDate(4);
	       java.sql.Time time=result.getTime(4);
	       // to do on call:
	       // spade.time.Date dt=new spade.time.Date();
	       // dt.setDateScheme("dd/mm/yyyy hh:tt:ss");
	       // dt.setDate(date,time);
	     }
	     if (v.size()==0) v=null;
	   } catch (SQLException se) {
	     System.out.println("SQL error:"+se.toString());
	   }
	   return v;
	 }
	 */

}
