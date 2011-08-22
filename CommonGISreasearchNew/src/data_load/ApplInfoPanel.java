package data_load;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ResourceBundle;

import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.DialogContent;
import spade.lib.lang.Language;

/**
* Used for asking application name, data server URL, and other information
* about an application (project) to be saved.
*/
public class ApplInfoPanel extends Panel implements DialogContent, ItemListener {
	static ResourceBundle res = Language.getTextResource("data_load.Res");
	/**
	* The error message
	*/
	protected String err = null;
	protected TextField applNameTF = null, terrNameTF = null, urlTF = null;
	protected Checkbox useServerCB = null;

	public ApplInfoPanel(String applName, String terrName, String serverURL) {
		setLayout(new ColumnLayout());
		Panel pp = new Panel(new BorderLayout());
		//following text:"Project name?"
		pp.add(new Label(res.getString("Project_name_")), "West");
		applNameTF = new TextField(30);
		if (applName != null) {
			applNameTF.setText(applName);
		}
		pp.add(applNameTF, "Center");
		add(pp);
		pp = new Panel(new BorderLayout());
		//following text:"Territory name?"
		pp.add(new Label(res.getString("Territory_name_")), "West");
		terrNameTF = new TextField(30);
		if (terrName != null) {
			terrNameTF.setText(terrName);
		}
		pp.add(terrNameTF, "Center");
		add(pp);
		//following text:"use the data server for loading the data"
		useServerCB = new Checkbox(res.getString("use_the_data_server"), serverURL != null);
		useServerCB.addItemListener(this);
		add(useServerCB);
		pp = new Panel(new BorderLayout());
		//following text:"Server URL?"
		pp.add(new Label(res.getString("Server_URL_")), "West");
		urlTF = new TextField(30);
		if (serverURL != null) {
			urlTF.setText(serverURL);
		} else {
			urlTF.setEnabled(false);
		}
		pp.add(urlTF, "Center");
		add(pp);
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource().equals(useServerCB)) {
			urlTF.setEnabled(useServerCB.getState());
		}
	}

	@Override
	public boolean canClose() {
		if (!useServerCB.getState())
			return true;
		String str = CManager.getTextFromField(urlTF);
		if (str == null) {
			//following text:"The server URL is not specified!"
			err = res.getString("The_server_URL_is_not");
			return false;
		}
		try {
			URL url = new URL(str);
		} catch (MalformedURLException mfe) {
			//following text:"Invalid URL: "
			err = res.getString("Invalid_URL_") + mfe.toString();
			return false;
		}
		return true;
	}

	/**
	* Returns the error message if the panel cannot be closed
	*/
	@Override
	public String getErrorMessage() {
		return err;
	}

	public String getApplName() {
		return CManager.getTextFromField(applNameTF);
	}

	public String getTerrName() {
		return CManager.getTextFromField(terrNameTF);
	}

	public String getServerURL() {
		return CManager.getTextFromField(urlTF);
	}
}
