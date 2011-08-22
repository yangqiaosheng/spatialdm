package ui;

import java.awt.BorderLayout;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.system.DataLoader;
import spade.analysis.system.ESDACore;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.OKDialog;
import spade.lib.lang.Language;
import spade.vis.database.AttributeDataPortion;
import spade.vis.space.GeoLayer;
import spade.vis.space.LayerManager;

/**
* The UI of the function of data reloading: while the user works with data
* loaded from a server, another user may update the data on the server.
* Therefore a user must be able to reload data without quitting the system.
*/

public class DataReloadUI {
	static ResourceBundle res = Language.getTextResource("ui.Res");

	/**
	* Asks the user what map layer or/and table to reload. Then uses the
	* corresponding function of the system's data loader.
	*/
	public void reloadData(ESDACore core) {
		if (core == null)
			return;
		DataLoader loader = core.getDataLoader();
		if (loader == null || (loader.getMapCount() < 1 && loader.getTableCount() < 1))
			return;
		//this vector will contain the candidates for updating, i.e. tables and layers
		Vector dataSets = new Vector(20, 20);
		List lst = new List(8);
		//first add all the tables
		for (int i = 0; i < loader.getTableCount(); i++) {
			AttributeDataPortion tbl = loader.getTable(i);
			dataSets.addElement(tbl);
			// following string: "Table \""
			String txt = res.getString("Table") + tbl.getName() + "\"";
			GeoLayer gl = loader.getTableLayer(tbl);
			if (gl != null) {
				txt += " (layer \"" + gl.getName() + "\")";
			}
			lst.add(txt);
		}
		//now add all the layers
		for (int i = 0; i < loader.getMapCount(); i++) {
			LayerManager lm = loader.getMap(i);
			for (int j = 0; j < lm.getLayerCount(); j++) {
				GeoLayer gl = lm.getGeoLayer(j);
				dataSets.addElement(gl);
				lst.add("Layer \"" + gl.getName() + "\"");
			}
		}
		if (dataSets.size() < 1) {
			// following string: "No tables or layers loaded yet!"
			core.getUI().showMessage(res.getString("No_tables_or_layers"), true);
			return;
		}
		Panel p = new Panel(new BorderLayout());
		// following string: "Specify the dataset to reload:"
		p.add(new Label(res.getString("Specify_the_dataset")), "North");
		p.add(lst, "Center");
		lst.select(0);
		// following string: "Reload data"
		OKDialog okd = new OKDialog(CManager.getAnyFrame(), res.getString("Reload_data"), true);
		okd.addContent(p);
		okd.show();
		if (okd.wasCancelled())
			return;
		if (loader.updateData(dataSets.elementAt(lst.getSelectedIndex()))) {
			// following string: "The data have been reloaded"
			core.getUI().showMessage(res.getString("The_data_have_been"), false);
		}
	}
}
