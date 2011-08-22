package ui;

import java.awt.BorderLayout;
import java.awt.Choice;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.ScrollPane;
import java.awt.TextField;
import java.io.File;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.system.Supervisor;
import spade.analysis.system.SystemUI;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.OKDialog;
import spade.lib.lang.Language;
import spade.lib.util.InfoSaver;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataTable;

public class AddAttribute {
	static ResourceBundle res = Language.getTextResource("ui.Res");
	/**
	* The system UI that can, in particular, provide access to the map viewer
	*/
	protected SystemUI ui = null;
	/**
	* Information about the table in which the attribute is added
	*/
	protected TableData tableData = null;

	public int addAttribute(TableManager tman, Supervisor supervisor) {
		ui = supervisor.getUI();
		OKDialog dlg = null;
		TextField tfAttrName = null, tfDefaultVal = null;
		Choice chAttrType = null;
		ScrollPane sp = null;
		TextField tfVals[] = null;
		DataTable dTable = null;
		do {
			// following string: "Select the table for adding an attribute"
			TableData td = tman.selectCurrTable(res.getString("Select_the_table_for"));
			if (td == null)
				return -1;
			dTable = (DataTable) td.table;
			if (!dTable.hasData()) {
				dTable.loadData();
			}
			if (!dTable.hasData()) {
				if (ui != null) {
					ui.showMessage(res.getString("no_data_in_table"), true);
				}
				return -1;
			}
			Panel mainP = new Panel();
			mainP.setLayout(new BorderLayout());
			Panel p = new Panel();
			p.setLayout(new ColumnLayout());
			Panel pp = new Panel();
			pp.setLayout(new BorderLayout());
			pp.add(new Label("Name"), "West");
			if (tfAttrName == null) {
				tfAttrName = new TextField("", 20);
			} else {
				tfAttrName.setText(tfAttrName.getText().trim());
			}
			pp.add(tfAttrName, "Center");
			p.add(pp);
			pp = new Panel();
			pp.setLayout(new BorderLayout());
			// following string: "Type"
			pp.add(new Label(res.getString("Type")), "West");
			if (chAttrType == null) {
				chAttrType = new Choice();
			}
			pp.add(chAttrType, "Center");
			// following string: "Integer"
			chAttrType.addItem(res.getString("Integer"));
			// following string: "Real"
			chAttrType.addItem(res.getString("Real"));
			// following string: "String"
			chAttrType.addItem(res.getString("String"));
			p.add(pp);
			pp = new Panel();
			pp.setLayout(new BorderLayout());
			// following string: "Default value"
			pp.add(new Label(res.getString("Default_value")), "West");
			if (tfDefaultVal == null) {
				tfDefaultVal = new TextField("", 10);
			} else {
				tfDefaultVal.setText(tfDefaultVal.getText().trim());
			}
			pp.add(tfDefaultVal, "Center");
			p.add(pp);
			p.add(new Line(false));
			mainP.add(p, "North");
			sp = new ScrollPane();
			tfVals = new TextField[Math.min(dTable.getDataItemCount(), 50)];
			p = new Panel();
			p.setLayout(new GridLayout(tfVals.length, 1));
			for (int i = 0; i < tfVals.length; i++) {
				pp = new Panel();
				pp.setLayout(new BorderLayout());
				pp.add(new Label(dTable.getDataItemName(i)), "Center");
				pp.add(tfVals[i] = new TextField("", 5), "West");
				p.add(pp);
			}
			sp.add(p);
			mainP.add(sp, "Center");
			mainP.add(new Line(false), "South");
			do {
				// following string: "Specify a new attribute"
				dlg = new OKDialog(getFrame(), res.getString("Specify_a_new"), true, true);
				dlg.addContent(mainP);
				supervisor.getWindowManager().registerWindow(dlg);
				dlg.show();
				if (dlg.wasCancelled())
					return -1;
			} while (tfAttrName.getText().trim().length() == 0);
			tableData = td;
		} while (dlg.wasBackPressed());
		char type = (chAttrType.getSelectedIndex() == 0) ? AttributeTypes.integer : ((chAttrType.getSelectedIndex() == 0) ? AttributeTypes.real : AttributeTypes.character);
		dTable.addAttribute(tfAttrName.getText().trim(), null, type);
		int aIdx = dTable.getAttrCount() - 1;
		String defValStr = tfDefaultVal.getText().trim();
		for (int i = 0; i < tfVals.length; i++) {
			String str = tfVals[i].getText().trim();
			if (str.length() == 0) {
				str = defValStr;
			}
			dTable.getDataRecord(i).setAttrValue(str, aIdx);
		}
		for (int i = tfVals.length; i < dTable.getDataItemCount(); i++) {
			dTable.getDataRecord(i).setAttrValue(defValStr, aIdx);
		}
		Vector resultAttrs = new Vector(1, 1);
		resultAttrs.addElement(dTable.getAttributeId(aIdx));
		dTable.notifyPropertyChange("new_attributes", null, resultAttrs);
		// following string: "Attribute "+dTable.getAttributeName(aIdx)+" added to the table"
		showMessage(res.getString("Attribute") + dTable.getAttributeName(aIdx) + res.getString("added_to_the_table"));
		String dir = supervisor.getSystemSettings().getParameterAsString("SaveDataDir");
		if (dir == null || dir.length() < 1)
			return aIdx;
		File file = new File(dir);
		if (!file.exists() || !file.isDirectory())
			return aIdx;
		boolean isApplet = !supervisor.getSystemSettings().checkParameterValue("isLocalSystem", "true");
		String pathToScript = null;
		if (isApplet) {
			pathToScript = supervisor.getSystemSettings().getParameterAsString("SaveDataScript");
			if (pathToScript == null || pathToScript.length() < 1)
				return aIdx;
		}
		InfoSaver is = new InfoSaver();
		is.setIsApplet(isApplet);
		is.setPathToScript(pathToScript);
		if (!dir.endsWith("/") && !dir.endsWith("\\")) {
			dir += "/";
		}
		String fname = dir + dTable.getAttributeId(aIdx) + ".csv";
		is.setFileName(fname);
		Vector attr = new Vector(1, 1);
		attr.addElement(dTable.getAttributeId(aIdx));
		dTable.storeData(attr, true, is);
		// following string: "The attribute is stored in file "
		showMessage(res.getString("The_attribute_is") + fname);
		return aIdx;
	}

	protected Frame getFrame() {
		if (ui != null && ui.getMainFrame() != null)
			return ui.getMainFrame();
		return CManager.getAnyFrame();
	}

	protected void showMessage(String msg) {
		if (ui != null) {
			ui.showMessage(msg, false);
		}
	}

	public TableData getTableData() {
		return tableData;
	}
}
