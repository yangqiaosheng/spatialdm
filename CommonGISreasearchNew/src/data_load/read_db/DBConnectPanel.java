package data_load.read_db;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ResourceBundle;

import spade.lib.basicwin.DialogContent;
import spade.lib.lang.Language;

/**
* The UI for getting from the user information required for connection to
* a database
*/
public class DBConnectPanel extends Panel implements DialogContent {
	static ResourceBundle res = Language.getTextResource("data_load.read_db.Res");
	// following text:"Driver:","Computer:","Port:","Database:","User:","Password:","* Table:"
	protected static final String fieldNames[] = { res.getString("Driver_"), res.getString("Computer_"), res.getString("Port_"), res.getString("Database_"), res.getString("User_"), res.getString("Password_"), res.getString("_Table_") };
	protected static String lastValues[] = null;
	protected TextField tFields[] = null;
	protected Connection connection = null;
	protected String urlPrefix = null;
	protected String err = null;

	public DBConnectPanel(boolean askForTable) {
		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		c.weightx = 1.0f;
		c.weighty = 1.0f;
		c.fill = GridBagConstraints.HORIZONTAL;
		setLayout(gridbag);
		int nrows = fieldNames.length;
		tFields = new TextField[nrows];
		for (int i = 0; i < nrows; i++) {
			Label l = new Label(fieldNames[i]);
			c.gridwidth = 1;
			gridbag.setConstraints(l, c);
			add(l);
			tFields[i] = new TextField(30);
			c.gridwidth = GridBagConstraints.REMAINDER;
			gridbag.setConstraints(tFields[i], c);
			add(tFields[i]);
			if (fieldNames[i].startsWith("Password")) {
				tFields[i].setEchoChar('*');
			}
		}
		if (!askForTable) {
			//following text:"* The table may be selected later from a list"
			Label l = new Label(res.getString("_The_table_may_be"));
			c.gridwidth = GridBagConstraints.REMAINDER;
			gridbag.setConstraints(l, c);
			add(l);
		}

		if (lastValues != null) {
			for (int i = 0; i < lastValues.length; i++)
				if (lastValues[i] != null) {
					tFields[i].setText(lastValues[i]);
				}
		}
	}

	protected String getTextFromField(int n) {
		if (n < 0 || n >= tFields.length)
			return null;
		String s = tFields[n].getText();
		if (s == null)
			return null;
		s = s.trim();
		if (s.length() < 1)
			return null;
		return s;
	}

	public void setDriver(String driver) {
		if (driver != null) {
			tFields[0].setText(driver);
		}
	}

	public String getDriver() {
		return getTextFromField(0);
	}

	public void setDatabase(String database) {
		if (database != null) {
			tFields[3].setText(database);
		}
	}

	public String getDatabase() {
		return getTextFromField(3);
	}

	public void setComputer(String computer) {
		if (computer != null) {
			tFields[1].setText(computer);
		}
	}

	public String getComputer() {
		return getTextFromField(1);
	}

	public void setPort(int port) {
		if (port > 0) {
			tFields[2].setText(String.valueOf(port));
		}
	}

	public void setURLPrefix(String prefix) {
		urlPrefix = prefix;
	}

	public String getDatabaseURL() {
		String s = getDatabase();
		if (s == null)
			return null;
		String url = "";
		if (urlPrefix != null) {
			url = urlPrefix;
			String str = getTextFromField(1); //computer
			if (str != null) {
				url += "@" + str + ":";
			}
			str = getTextFromField(2); //port
			if (str != null) {
				url += str + ":";
			}
			return url + s;
		}
		return s;
	}

	public void setUser(String user) {
		if (user != null) {
			tFields[4].setText(user);
		}
	}

	public String getUser() {
		return getTextFromField(4);
	}

	public void setPassword(String psw) {
		if (psw != null) {
			tFields[5].setText(psw);
		}
	}

	public String getPassword() {
		return getTextFromField(5);
	}

	public void setTable(String tbl) {
		if (tbl != null) {
			tFields[6].setText(tbl);
		}
	}

	public String getTable() {
		return getTextFromField(6);
	}

	public Connection getConnection() {
		return connection;
	}

	@Override
	public boolean canClose() {
		err = null;
		String driver = getDriver();
		//following text:"No database driver specified!"
		if (driver == null) {
			err = res.getString("No_database_driver");
			return false;
		}
		String dbase = getDatabase();
		//following text:"The database name is not specified!"
		if (dbase == null) {
			err = res.getString("The_database_name_is");
			return false;
		}
		try {
			Class.forName(driver);
		} catch (Exception e) {
			//following text:"Failed to load the driver "
			err = res.getString("Failed_to_load_the") + driver + ": " + e.toString();
			return false;
		}
		String url = getDatabaseURL();
		try {

			connection = DriverManager.getConnection(url, getUser(), getPassword());
		} catch (SQLException se) {
			//following text:"Failed to connect to "
			err = res.getString("Failed_to_connect_to") + url + ": " + se.toString();
			return false;
		}
		if (lastValues == null) {
			lastValues = new String[tFields.length];
		}
		for (int i = 0; i < tFields.length; i++) {
			lastValues[i] = getTextFromField(i);
		}
		return true;
	}

	@Override
	public String getErrorMessage() {
		return err;
	}
}
