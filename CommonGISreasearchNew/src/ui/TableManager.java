package ui;

import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.Panel;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.system.DataKeeper;
import spade.analysis.system.SystemUI;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.OKDialog;
import spade.lib.lang.Language;
import spade.lib.util.InfoSaver;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.DataTable;
import spade.vis.space.GeoLayer;

/**
* Runs dialogs on selection of a table for data visualization, selection of
* attributes, saving tables on the disk etc.
*/
public class TableManager {
	static ResourceBundle res = Language.getTextResource("ui.Res");
	/**
	* The system UI that can, in particular, display status messages
	*/
	protected SystemUI ui = null;
	/**
	* The DataKeeper keeps available tables and maps
	*/
	protected DataKeeper dataLoader = null;
	/**
	* Remembered number of the last selected table
	*/
	protected int currTableN = 0;
	/**
	* Current (last) selection of attributes for visualisation or calculation
	*/
	protected Vector currentSelectionOfAttrs = null;

	public void setDataKeeper(DataKeeper dk) {
		dataLoader = dk;
	}

	/**
	* Sets the system UI (the actual implementation of this interface depends
	* on the current system configuration)
	* The system UI that can, in particular, display status messages
	*/
	public void setUI(SystemUI ui) {
		this.ui = ui;
	}

	protected Frame getFrame() {
		if (ui != null && ui.getMainFrame() != null)
			return ui.getMainFrame();
		return CManager.getAnyFrame();
	}

	public int selectTableNumber(String title) {
		if (dataLoader.getTableCount() > 1) {
			CheckboxGroup cgr = new CheckboxGroup();
			Checkbox ch[] = new Checkbox[dataLoader.getTableCount()];
			Panel p = new Panel();
			p.setLayout(new ColumnLayout());
			for (int i = 0; i < dataLoader.getTableCount(); i++) {
				String str = "" + (i + 1) + ") ";
				AttributeDataPortion table = dataLoader.getTable(i);
				if (table.getName() != null) {
					str += table.getName();
				}
				GeoLayer layer = dataLoader.getTableLayer(table);
				if (layer != null) {
					str += " (" + layer.getName() + ")";
				}
				p.add(ch[i] = new Checkbox(str, i == currTableN, cgr));
			}
			OKDialog dlg = new OKDialog(getFrame(), title, true);
			dlg.addContent(p);
			dlg.show();
			if (dlg.wasCancelled())
				return -1;
			for (int i = 0; i < ch.length; i++)
				if (ch[i].getState())
					return i;
		}
		return 0;
	}

//ID
	public TableData selectTable(String title, String entityId) {
		if (dataLoader == null || dataLoader.getTableCount() < 1 || entityId == null)
			return null;
		TableData td = new TableData();

		Vector tables = new Vector();
		Vector tableN = new Vector();
		Vector checkboxes = new Vector();
		CheckboxGroup cgr = new CheckboxGroup();

		if (dataLoader.getTableCount() == 1) {
			tables.addElement(dataLoader.getTable(0));
			tableN.addElement(new Integer(0));
		}
		if (dataLoader.getTableCount() > 1) {
			for (int i = 0; i < dataLoader.getTableCount(); i++) {
				AttributeDataPortion table = dataLoader.getTable(i);
				if (table.getEntitySetIdentifier().equals(entityId)) {
					tables.addElement(table);
					tableN.addElement(new Integer(i));
					checkboxes.addElement(new Checkbox((table.getName() != null) ? table.getName() : "", tables.size() == 1, cgr));
				}
			}
		}
		if (tables.size() == 0)
			return null;
		else if (tables.size() == 1) {
			currTableN = ((Integer) tableN.elementAt(0)).intValue();
		} else {
			Panel p = new Panel();
			p.setLayout(new ColumnLayout());
			for (int i = 0; i < tables.size(); i++) {
				p.add((Checkbox) checkboxes.elementAt(i));
			}
			OKDialog dlg = new OKDialog(getFrame(), title, true);
			dlg.addContent(p);
			dlg.show();
			if (dlg.wasCancelled()) {
				currTableN = -1;
			} else {
				for (int i = 0; i < tables.size(); i++)
					if (((Checkbox) checkboxes.elementAt(i)).getState()) {
						currTableN = ((Integer) tableN.elementAt(i)).intValue();
					}
			}
		}

		if (currTableN == -1)
			return null;
		td.tableN = currTableN;
		td.table = dataLoader.getTable(td.tableN);
		//System.out.println("Visualization to be done in table N "+td.tableN+" "+td.table);
		td.mapN = dataLoader.getTableMapN(td.table);
		if (td.mapN >= 0) {
			td.lman = dataLoader.getMap(td.mapN);
			td.themLayer = dataLoader.getTableLayer(td.table);
			if (td.themLayer != null) {
				td.layerId = td.themLayer.getContainerIdentifier();
			}
		}
		return td;
	}

//~ID

	public TableData selectCurrTable(String title) {
		if (dataLoader == null || dataLoader.getTableCount() < 1)
			return null;
		TableData td = new TableData();
		currTableN = selectTableNumber(title);
		if (currTableN == -1)
			return null;
		td.tableN = currTableN;
		td.table = dataLoader.getTable(td.tableN);
		//System.out.println("Visualization to be done in table N "+td.tableN+" "+td.table);
		td.mapN = dataLoader.getTableMapN(td.table);
		if (td.mapN >= 0) {
			td.lman = dataLoader.getMap(td.mapN);
			td.themLayer = dataLoader.getTableLayer(td.table);
			if (td.themLayer != null) {
				td.layerId = td.themLayer.getContainerIdentifier();
			}
		}
		return td;
	}

	public void exportTableAsCSV() {
		// following string: "Select the table to export"
		TableData td = selectCurrTable(res.getString("Select_the_table_to"));
		if (td == null)
			return;
		if (!(td.table instanceof DataTable))
			return;
		DataTable dt = (DataTable) td.table;
		FileDialog fd = new FileDialog(getFrame(),
		// following string: "Export the table to the file"
				res.getString("Export_the_table_to"), FileDialog.SAVE);
		fd.setFile("*.csv");
		fd.show();
		String fname = fd.getFile(), dir = fd.getDirectory();
		if (fname == null)
			return;
		DataOutputStream dos = null;
		try {
			dos = new DataOutputStream(new FileOutputStream(dir + fname));
		} catch (IOException ioe) {
			if (ui != null) {
				// following string: "Error writing to the file: "
				ui.showMessage(res.getString("Error_writing_to_the") + ioe.toString(), true);
			}
		}
		if (dos != null) {
			dt.saveTableAsCsv(dos);
			try {
				dos.close();
			} catch (IOException ioe) {
			}
		}
	}

	public void storeDataFromTable(String dirToStore, String scriptToUse, boolean isApplet) {
		System.out.println("store data: isApplet=" + isApplet);
		if (isApplet && (dirToStore == null || scriptToUse == null)) {
			if (ui != null)
				if (dirToStore == null) {
					// following string: "The directory for storing data is not specified!"
					ui.showMessage(res.getString("The_directory_for1"), true);
				} else {
					// following string: "The CGI script for storing data is not specified!"
					ui.showMessage(res.getString("The_CGI_script_for"), true);
				}
			return;
		}
		// following string: "Select the table"
		TableData td = selectCurrTable(res.getString("Select_the_table"));
		if (td == null)
			return;
		if (!(td.table instanceof DataTable))
			return;
		AttributeChooser attrSel = new AttributeChooser();
		Vector attrs = null;
		if (attrSel.selectColumns(td.table, null, null, false, res.getString("Select_attributes_to"), ui) != null) {
			attrs = attrSel.getSelectedColumnIds();
		}
		if (attrs == null || attrs.size() < 1)
			return;
		String fname = null;
		if (dirToStore == null) { //propose the user to select the directory
			FileDialog fd = new FileDialog(getFrame(),
			// following string: "Store the data to the file"
					res.getString("Store_the_data_to_the"), FileDialog.SAVE);
			fd.setFile("*.csv");
			fd.show();
			fname = fd.getFile();
			if (fname == null)
				return;
			dirToStore = fd.getDirectory();
		}
		if (!dirToStore.endsWith("/") && !dirToStore.endsWith("\\")) {
			dirToStore += "/";
		}
		InfoSaver saver = new InfoSaver();
		saver.setIsApplet(isApplet);
		if (fname == null) {
			fname = saver.generateFileName() + ".csv";
		}
		saver.setFileName(dirToStore + fname);
		saver.setPathToScript(scriptToUse);
		DataTable dt = (DataTable) td.table;
		dt.storeData(attrs, true, saver);
	}

	public int getCurrentTableN() {
		return currTableN;
	}

}
