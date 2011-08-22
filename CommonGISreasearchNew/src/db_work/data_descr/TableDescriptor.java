package db_work.data_descr;

import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: 02-Feb-2006
 * Time: 12:44:41
 * To change this template use File | Settings | File Templates.
 */
public class TableDescriptor {

	public String tableName = null;

	public String derivedFromTable = null;

	/**
	* The descriptors of the columns, instances of ColumnDescriptor
	*/
	public Vector columns = null;

	public int getNColumns() {
		return (columns == null) ? 0 : columns.size();
	}

	public ColumnDescriptor getColumnDescriptor(int cn) {
		return (columns == null || columns.size() <= cn) ? null : (ColumnDescriptor) columns.elementAt(cn);
	}

	/**
	* The total number of rows in the database table.
	*/
	public int dbRowCount = 0;

	public void clear() {
		//tableName=null;
		derivedFromTable = null;
		if (columns != null) {
			columns.clear();
			columns = null;
		}
		dbRowCount = 0;
	}

}
