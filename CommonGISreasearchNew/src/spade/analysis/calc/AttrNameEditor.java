package spade.analysis.calc;

import java.awt.BorderLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.ScrollPane;
import java.awt.TextField;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.OKDialog;
import spade.lib.lang.Language;
import spade.vis.database.Attribute;
import spade.vis.database.DataTable;

/**
* Allows the user to edit names of attributes added to a table. If an attribute
* is a classification; allows also to edit class names, i.e. attribute values.
*/
public class AttrNameEditor {
	/**
	* Text Field Info
	* each text field is followedc by 2 Integres:
	* attribute number and values number (or -1)
	*/
	private static Vector tfi = null;
	static ResourceBundle res = Language.getTextResource("spade.analysis.calc.Res");

	/**
	* Allows the user to change automatically generated attribute names
	*/
	public static void attrAddedToTable(DataTable dTable, Vector resultAttrs) {
		if (resultAttrs == null)
			return;
		// get the top-level attributes
		Vector attrs = new Vector(resultAttrs.size(), 1);
		for (int j = 0; j < resultAttrs.size(); j++) {
			Attribute at = dTable.getAttribute((String) resultAttrs.elementAt(j));
			if (at.getParent() != null) {
				at = at.getParent();
			}
			if (!attrs.contains(at)) {
				attrs.addElement(at);
			}
		}
		// build the UI
		Panel tfPanel = new Panel();
		tfPanel.setLayout(new ColumnLayout());
		if (tfi == null) {
			tfi = new Vector(30, 15);
		} else {
			tfi.removeAllElements();
		}
		TextField tf = null;
		boolean valuesToBeEdited = false;
		int panLen = 0;
		for (int j = 0; j < attrs.size(); j++) {
			Attribute attr = (Attribute) attrs.elementAt(j);
			tfPanel.add(tf = new TextField(attr.getName()));
			++panLen;
			tfi.addElement(tf);
			tfi.addElement(new Integer(j));
			tfi.addElement(new Integer(-1));
			if (attr.getNClasses() > 0) {
				for (int k = 0; k < attr.getNClasses(); k++) {
					Panel pp = new Panel();
					pp.setLayout(new BorderLayout());
					tfPanel.add(pp);
					++panLen;
					pp.add(new Label("" + (k + 1) + " -> "), "West");
					pp.add(tf = new TextField(attr.getValueList()[k]), "Center");
					tfi.addElement(tf);
					tfi.addElement(new Integer(j));
					tfi.addElement(new Integer(k));
					valuesToBeEdited = true;
				}
			}
		}
		Panel p = new Panel();
		p.setLayout(new ColumnLayout());
		// following text: "Results of the calculations will be"
		p.add(new Label(res.getString("Results_of_the")));
		// following text: "stored in the database as new attribute(s)."
		p.add(new Label(res.getString("stored_in_the")));
		// following text: "You may edit names of new attributes"
		p.add(new Label(res.getString("You_may_edit_names_of")));
		if (valuesToBeEdited) {
			// following text: "and their values:"
			p.add(new Label(res.getString("and_their_values_")));
		}
		p.add(new Line(false));
		if (panLen <= 10) {
			p.add(tfPanel);
		} else {
			ScrollPane scp = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
			scp.add(tfPanel);
			p.add(scp);
		}
		p.add(new Line(false));
		// following text: "Attribute names"
		OKDialog dlg = new OKDialog(CManager.getAnyFrame(), res.getString("Attribute_names"), true);
		dlg.addContent(p);
		dlg.show();
		if (!dlg.wasCancelled()) {
			for (int j = 0; j < tfi.size(); j += 3) {
				tf = (TextField) tfi.elementAt(j);
				int attrn = ((Integer) tfi.elementAt(j + 1)).intValue(), valn = ((Integer) tfi.elementAt(j + 2)).intValue();
				String str = tf.getText();
				if (str == null) {
					continue;
				}
				str = str.trim();
				if (str.length() < 1) {
					continue;
				}
				int j3 = j / 3;
				if (attrs.size() <= j3) {
					continue;
				}
				Attribute attr = (Attribute) attrs.elementAt(j3);
				if (valn == -1) {
					attr.setName(str);
				} else {
					String valueList[] = attr.getValueList();
					if (valueList == null || str.equals(valueList[valn])) {
						continue;
					}
					valueList[valn] = str;
					attr.setValueListAndColors(valueList, attr.getValueColors());
				}
			}
		}
		dTable.notifyPropertyChange("new_attributes", null, resultAttrs);
	}
}