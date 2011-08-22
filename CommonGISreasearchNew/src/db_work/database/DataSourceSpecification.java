package db_work.database;

import java.util.Vector;

import db_work.data_descr.TableDescriptor;

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: 25-Jan-2006
 * Time: 14:25:02
 * To change this template use File | Settings | File Templates.
 */
public class DataSourceSpecification {

	// JDBC connection data
	public String driver = null;
	public String url = null;
	public String user = null;
	public String password = null;

	// loaded tables
	public Vector tableDescriptors = null;

	public TableDescriptor getTableDescriptor(int tn) {
		return (tableDescriptors == null || tableDescriptors.size() <= tn) ? null : (TableDescriptor) tableDescriptors.elementAt(tn);
	}

}
