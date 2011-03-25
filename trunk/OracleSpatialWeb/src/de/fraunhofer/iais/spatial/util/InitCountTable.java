package de.fraunhofer.iais.spatial.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * initialize the key (id, radius) of count table according to the polygons
 * @author haolin
 *
 */
public class InitCountTable {

	/**
	 * @param args
	 */
	final static String AREA_TABLE_NAME = "FLICKR_EUROPE_AREA_";
	final static String COUNTS_TABLE_NAME = "FLICKR_EUROPE_COUNT";
	static DBUtil db = new DBUtil("/jdbc_pg.properties", 1, 1);

	public static void main(String[] args) throws SQLException {
		ArrayList<String> radiusList = new ArrayList<String>();
		radiusList.add("5000");
		radiusList.add("10000");
		radiusList.add("20000");
		radiusList.add("40000");
		radiusList.add("80000");
		radiusList.add("160000");
		radiusList.add("320000");

		Connection conn = db.getConn();

		PreparedStatement deleteStmt = db.getPstmt(conn, "delete from " + COUNTS_TABLE_NAME);
		deleteStmt.executeUpdate();
		db.close(deleteStmt);


		for (String radius : radiusList) {
			PreparedStatement insertStmt = db.getPstmt(conn,
					"insert into " + COUNTS_TABLE_NAME + " (id, radius)" +
					" select t.id AS id, "+ radius +" AS radius from " + AREA_TABLE_NAME + radius + " t");
			insertStmt.executeUpdate();
			db.close(insertStmt);
		}

		db.close(conn);

	}

}
