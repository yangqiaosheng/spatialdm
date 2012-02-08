package de.fraunhofer.iais.spatial.script.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import de.fraunhofer.iais.spatial.util.DBUtil;

/**
 * initialize the key (id, radius) of count table according to the polygons
 * @author haolin
 *
 */
public class InitCountTable {

	/**
	 * @param args
	 */
	final static String AREA_TABLE_NAME = "FLICKR_WORLD_AREA";
//	final static String COUNTS_TABLE_NAME = "FLICKR_EUROPE_TOPVIEWED_5M_COUNT";
	final static String COUNTS_TABLE_NAME = "flickr_world_topviewed_1m_tags_count";
	static DBUtil db = new DBUtil("/jdbc_pg.properties", 1, 1);

	public static void main(String[] args) throws SQLException {

		Connection conn = db.getConn();

		PreparedStatement deleteStmt = db.getPstmt(conn, "truncate " + COUNTS_TABLE_NAME);
		deleteStmt.executeUpdate();
		db.close(deleteStmt);

		PreparedStatement insertStmt = db.getPstmt(conn, "insert into " + COUNTS_TABLE_NAME + " (id, radius)" + " select id, CAST(radius as integer) from " + AREA_TABLE_NAME + " t");
		insertStmt.executeUpdate();
		db.close(insertStmt);

		db.close(conn);

	}

}
