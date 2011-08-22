package db_work.database;

import data_load.read_db.DBConnectPanel;

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: 09-Jun-2006
 * Time: 13:49:28
 * To change this template use File | Settings | File Templates.
 */
public class DBConnectPanelAdvanced extends DBConnectPanel {

	public DBConnectPanelAdvanced(boolean askForTable) {
		super(askForTable);
	}

	@Override
	public String getDatabaseURL() {
		String s = getDatabase();
		if (s == null)
			return null;
		String url = "";
		if (urlPrefix != null) {
			url = urlPrefix;
			if (urlPrefix.contains("oracle")) {
				String str = getComputer(); //computer
				if (str != null) {
					url += "@" + str + ":";
				}
				str = getTextFromField(2); //port
				if (str != null) {
					url += str + ":";
				}
				return url + s;
			}
			if (urlPrefix.contains("postgres")) {
				String str = getComputer(); //computer
				if (str != null) {
					url += "//" + str + "/";
				}
				//str=getTextFromField(2); //port
				//if (str!=null) url+=str+":";
				return url + s;
			}
		}
		return s;
	}

	@Override
	public String getComputer() {
		return getTextFromField(1);
	}

}
