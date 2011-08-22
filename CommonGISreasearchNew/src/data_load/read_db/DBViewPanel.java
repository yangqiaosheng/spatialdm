package data_load.read_db;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Cursor;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.lib.basicwin.DialogContent;
import spade.lib.basicwin.OKDialog;
import spade.lib.lang.Language;

/**
* A UI for viewing the contents of a database (catalogs and tables) and
* selection of a table to load.
*/
public class DBViewPanel extends Panel implements ItemListener, ActionListener, DialogContent {
	static ResourceBundle res = Language.getTextResource("data_load.read_db.Res");
	protected static String tableTypes[] = { "TABLE", "VIEW" };
	protected Connection connection = null;
	protected DatabaseMetaData meta = null;
	protected List catList = null, tableList = null;
	protected String currCat = "";
	protected Button bNRec = null;
	protected Panel sPanel = null;
	/**
	* A special query used to retrieve from the database only the list of tables
	* with geographic information. By default, null. If such a query exists,
	* the data reader should set it.
	*/
	protected String onlyGeoTablesQuery = null;
	protected String onlyMyTablesQuery = null;
	/**
	* The checkbox allows the user to see only tables with geographic data
	*/
	protected Checkbox onlyGeoCB = null;
	/**
	* Error message to be shown in the status line of the dialog this panel is
	* included in.
	*/
	protected String err = null;

	public DBViewPanel(Connection con, String onlyMyTablesQuery) {
		if (con == null)
			return;
		this.onlyMyTablesQuery = onlyMyTablesQuery;
		connection = con;
		String op = null;
		Vector catalogs = null;
		try {
			op = "Reading metadata";
			meta = connection.getMetaData();
			//try to get the list of database catalogs
			ResultSet rs = meta.getCatalogs();
			if (rs != null) {
				while (rs.next()) {
					String s = rs.getString(1);
					if (s != null) {
						if (catalogs == null) {
							catalogs = new Vector(10, 10);
						}
						catalogs.addElement(s);
					}
				}
				rs.close();
			}
		} catch (SQLException e) {
			err = op + ": " + e.toString();
			System.out.println(err);
			return;
		}
		tableList = new List(10);
		//check if there are tables without catalogs
		currCat = "";
		fillTableList();
		if (tableList.getItemCount() > 0) {
			if (catalogs == null) {
				catalogs = new Vector(1, 1);
			}
			catalogs.insertElementAt(".", 0);
		}
		if (catalogs != null && catalogs.size() > 0) {
			currCat = (String) catalogs.elementAt(0);
			if (currCat.equals(".")) {
				currCat = "";
			}
		}
		setLayout(new BorderLayout());
		if (catalogs != null && catalogs.size() > 1) {
			Panel p = new Panel(new GridLayout(1, 2));
			catList = new List(10);
			catList.addItemListener(this);
			for (int i = 0; i < catalogs.size(); i++) {
				catList.add((String) catalogs.elementAt(i));
			}
			catList.select(0);
			p.add(catList);
			p.add(tableList);
			add(p, "Center");
			p = new Panel(new GridLayout(1, 2));
			// following text:"Catalogs:"
			p.add(new Label(res.getString("Catalogs_"), Label.CENTER));
			// following text:"Tables:"
			p.add(new Label(res.getString("Tables_"), Label.CENTER));
			add(p, "North");
		} else {
			// following text:"Catalogs: "
			// following text:"none"
			add(new Label(res.getString("Catalog_") + ((currCat.length() < 1) ? res.getString("none") : currCat), Label.CENTER), "North");
			add(tableList, "Center");
		}
		sPanel = new Panel();
		sPanel.setLayout(new BorderLayout());
		add(sPanel, "South");
		//following text: "Rows number ?"
		sPanel.add(bNRec = new Button(res.getString("Rows_number_")), "South");
		bNRec.addActionListener(this);
		if (tableList.getItemCount() < 1) {
			fillTableList();
		}
	}

	public boolean hasValidContent() {
		return tableList != null && tableList.getItemCount() > 0;
	}

	protected void fillTableList() {
		if (meta == null || tableList == null)
			return;
		if (tableList.getItemCount() > 0) {
			tableList.removeAll();
		}
		if (catList != null) {
			currCat = catList.getSelectedItem();
			if (currCat.equals(".")) {
				currCat = "";
			}
		}
		ResultSet rs = null;
		Statement stat = null;
		try {
			if (onlyGeoTablesQuery == null || onlyGeoCB == null || !onlyGeoCB.getState())
				if (onlyMyTablesQuery != null) {
					stat = connection.createStatement();
					rs = stat.executeQuery(onlyMyTablesQuery);
				} else {
					rs = meta.getTables(currCat, null, null, tableTypes);
				}
			else {
				stat = connection.createStatement();
				rs = stat.executeQuery(onlyGeoTablesQuery);
			}
		} catch (SQLException e) {
			//following text:"Failed to get the list of tables for the catalog <"
			err = res.getString("Failed_to_get_the") + currCat + ">: " + e.toString();
			System.out.println(err);
		}
		if (rs != null) {
			try {
				while (rs.next()) {
					String s = rs.getString("TABLE_NAME");
					if (s != null) {
						tableList.add(s);
					}
				}
				rs.close();
			} catch (SQLException e) {
				//following text:"Error while reading the list of tables: "
				err = res.getString("Error_while_reading") + e.toString();
				System.out.println(err);
			}
		}
		if (stat != null) {
			try {
				stat.close();
			} catch (SQLException e) {
			}
		}
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource().equals(catList)) {
			fillTableList();
		} else if (e.getSource().equals(onlyGeoCB))
			if (onlyGeoTablesQuery != null) {
				fillTableList();
			}
	}

	public String getTableName() {
		if (tableList != null)
			return tableList.getSelectedItem();
		return null;
	}

	public String getCatalog() {
		return currCat;
	}

	public DatabaseMetaData getMetaData() {
		return meta;
	}

	@Override
	public boolean canClose() {
		err = null;
		//following text:"No table selected!"
		if (getTableName() == null) {
			err = res.getString("No_table_selected_");
			return false;
		}
		return true;
	}

	@Override
	public String getErrorMessage() {
		return err;
	}

	/**
	* Sets the special query used to retrieve from the database only the list of
	* tables with geographic information (if possible).
	*/
	public void setOnlyGeoTablesQuery(String query) {
		onlyGeoTablesQuery = query;
		if (query != null && onlyGeoCB == null) {
			//following text:"show only tables with geographic data"
			onlyGeoCB = new Checkbox(res.getString("show_only_tables_with"), false);
			sPanel.add(onlyGeoCB, "North");
			onlyGeoCB.addItemListener(this);
		}
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		if (ae.getSource() != bNRec)
			return;
		String str = tableList.getSelectedItem();
		int n = -1;
		if (str != null && str.length() > 0) {
			Cursor curs = sPanel.getCursor();
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			try {
				Statement cstat = connection.createStatement();
				ResultSet crs = cstat.executeQuery("select count(*) from " + str);
				crs.next();
				n = crs.getInt(1);
				crs.close();
				cstat.close();
			} catch (SQLException sqle) {
				System.out.println("* " + sqle);
			}
			setCursor(curs);
		}
		OKDialog dlg = new OKDialog(spade.lib.basicwin.CManager.getAnyFrame(), "Information", true);
		Panel p = new Panel();
		p.setLayout(new BorderLayout());
		//following text:"Table="
		p.add(new Label(res.getString("Table_") + str, Label.CENTER), "North");
		//following text:"Number of records="
		//following text:"(some error)"
		p.add(new Label(res.getString("Number_of_records_") + n + ((n >= 0) ? "" : res.getString("_some_error_")), Label.CENTER), "South");
		dlg.addContent(p);
		dlg.show();
	}
}
