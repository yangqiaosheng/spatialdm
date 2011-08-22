package spade.analysis.transform;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.ScrollPane;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.OKDialog;
import spade.lib.basicwin.SelectDialog;
import spade.lib.lang.Language;
import spade.lib.util.IntArray;
import spade.lib.util.NumRange;
import spade.lib.util.StringUtil;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.DataRecord;
import spade.vis.database.ThematicDataItem;

/**
* The component that saves results of attribute transformation to a table.
* Implements the TransformedDataSaver interface, which is introduced in order
* to make the component itself easily removable from the system.
*/
public class TransformedDataSaverImplement implements TransformedDataSaver, ActionListener {

	static ResourceBundle res = Language.getTextResource("spade.analysis.transform.Res");
	/**
	* The attribute transformer, the results of which need to be saved.
	*/
	protected AttributeTransformer trans = null;
	/**
	* The new top-level attributes created by the saver. It is possible to save
	* the data for several times in the same table columns.
	*/
	protected Vector transTopAttr = null;
	/**
	* The indexes of the new table columns created by the saver
	*/
	protected IntArray transColNs = null;
	/**
	* The number of save operations this saver made
	*/
	protected int nSaves = 0;

	/**
	* Setups the saver. For this purpose, needs the attribute transformer, the
	* results of which need to be saved. Returns true if successfully set.
	*/
	@Override
	public boolean setup(AttributeTransformer transformer) {
		if (transformer == null || transformer.getDataTable() == null)
			return false;
		trans = transformer;
		return true;
	}

	/**
	* Returns a UI component for accessing this saver (for example, a panel with
	* a "Save" button)
	*/
	@Override
	public Component getUI() {
		if (trans == null)
			return null;
		Button b = new Button(res.getString("save_transformed"));
		b.setActionCommand("save_transformed");
		b.addActionListener(this);
		Panel p = new Panel(new FlowLayout(FlowLayout.CENTER, 0, 2));
		p.add(b);
		return p;
	}

	/**
	* Listens to pressing the "Save" button
	*/
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand() == null || !e.getActionCommand().equals("save_transformed"))
			return;
		Frame fr = CManager.getAnyFrame((Component) e.getSource());
		boolean newColumns = false;
		if (transTopAttr == null || transColNs == null || transColNs.size() < 1) //create new attributes
			if (!makeAttributes(fr))
				return;
			else {
				newColumns = true;
			}
		else { //ask the user whether to create new table columns or to save
			//the data in the previously created columns
			SelectDialog selDia = new SelectDialog(fr, res.getString("save_transformed"), res.getString("where_to_store"));
			selDia.addOption(res.getString("use_previous"), "old", true);
			selDia.addOption(res.getString("create_new"), "new", false);
			selDia.addLabel(transColNs.size() + " " + res.getString("columns_required"));
			selDia.show();
			if (selDia.wasCancelled())
				return;
			if (selDia.getSelectedOptionN() == 1)
				if (!makeAttributes(fr))
					return;
				else {
					newColumns = true;
				}
		}
		storeData();
		AttributeDataPortion table = trans.getDataTable();
		Vector resultAttrs = new Vector(transColNs.size(), 1);
		for (int i = 0; i < transColNs.size(); i++) {
			resultAttrs.addElement(table.getAttributeId(transColNs.elementAt(i)));
		}
		if (newColumns) {
			table.notifyPropertyChange("new_attributes", null, resultAttrs);
		} else {
			table.notifyPropertyChange("values", null, resultAttrs);
		}
		OKDialog okd = new OKDialog(fr, res.getString("save_transformed"), false);
		okd.addContent(new Label(res.getString("data_stored"), Label.CENTER));
		okd.show();
	}

	/**
	* Used for creating unique attribute identifiers
	*/
	static private int nGlobalSaves = 0;

	/**
	* Creates new attributes and allows the user to edit their names. Adds new
	* columns to the table. Requires a frame for constructing a dialog.
	* Returns true if the new attributes have been successfully constructed.
	*/
	protected boolean makeAttributes(Frame mainFrame) {
		AttributeDataPortion table = trans.getDataTable();
		IntArray colNs = trans.getColumnNumbers();
		if (colNs == null || colNs.size() < 1)
			return false;
		IntArray topAttrNs = new IntArray(colNs.size(), 1);
		Vector tTopAttr = new Vector(20, 10);
		for (int i = 0; i < colNs.size(); i++) {
			Attribute at = table.getAttribute(colNs.elementAt(i));
			if (at.getParent() != null) {
				at = at.getParent();
			}
			int idx = tTopAttr.indexOf(at);
			if (idx < 0) {
				idx = tTopAttr.size();
				tTopAttr.addElement(at);
			}
			topAttrNs.addElement(idx);
		}
		if (tTopAttr.size() < 1)
			return false;
		String modifier = res.getString("transformed");
		if (nSaves > 0) {
			modifier += " " + (nSaves + 1);
		}
		for (int i = 0; i < tTopAttr.size(); i++) {
			Attribute at1 = (Attribute) tTopAttr.elementAt(i), at2 = new Attribute(at1.getIdentifier(), at1.getType());
			at2.setName(at1.getName() + " (" + modifier + ")");
			Vector v = new Vector(1, 1);
			v.addElement(at1.getIdentifier());
			at2.setSourceAttributes(v);
			tTopAttr.setElementAt(at2, i);
		}
		//display a dialog for editing attribute names
		Panel attrP = new Panel(new GridLayout(tTopAttr.size(), 1, 0, 2));
		TextField tf[] = new TextField[tTopAttr.size()];
		for (int i = 0; i < tTopAttr.size(); i++) {
			Attribute at = (Attribute) tTopAttr.elementAt(i);
			tf[i] = new TextField(at.getName(), 50);
			attrP.add(tf[i]);
		}
		Panel p = new Panel(new ColumnLayout()), mainP = p;
		p.add(new Label(res.getString("new_attr_added")));
		p.add(new Label(res.getString("n_new_columns") + " " + colNs.size()));
		p.add(new Label(res.getString("edit_attr_names")));
		if (tf.length <= 5) {
			p.add(attrP);
		} else {
			ScrollPane scp = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
			scp.add(attrP);
			mainP = new Panel(new BorderLayout());
			mainP.add(p, BorderLayout.NORTH);
			mainP.add(scp, BorderLayout.CENTER);
		}
		OKDialog okd = new OKDialog(mainFrame, res.getString("new_attr"), true);
		okd.addContent(mainP);
		okd.show();
		if (okd.wasCancelled())
			return false;
		for (int i = 0; i < tTopAttr.size(); i++) {
			String str = tf[i].getText();
			if (str == null) {
				continue;
			}
			str = str.trim();
			if (str.length() < 1) {
				continue;
			}
			Attribute at = (Attribute) tTopAttr.elementAt(i);
			at.setName(str);
		}
		//create new columns in the table
		transTopAttr = tTopAttr;
		if (transColNs == null) {
			transColNs = new IntArray(colNs.size(), 1);
		} else {
			transColNs.removeAllElements();
		}
		++nGlobalSaves;
		++nSaves;
		for (int i = 0; i < colNs.size(); i++) {
			Attribute at1 = table.getAttribute(colNs.elementAt(i)), at2 = (Attribute) tTopAttr.elementAt(topAttrNs.elementAt(i));
			if (at1.getParent() == null) {
				at2.setIdentifier(at2.getIdentifier() + "_t" + nGlobalSaves);
				table.addAttribute(at2);
			} else {
				Attribute child = new Attribute(at1.getIdentifier() + "_t" + nGlobalSaves, at1.getType());
				for (int j = 0; j < at1.getParameterCount(); j++) {
					child.addParamValPair(at1.getParamValPair(j));
				}
				at2.addChild(child);
				table.addAttribute(child);
			}
			transColNs.addElement(table.getAttrCount() - 1);
		}
		return true;
	}

	/**
	* Writes the transformed data to the earlier created columns
	*/
	protected void storeData() {
		if (transColNs == null || transColNs.size() < 1)
			return;
		AttributeDataPortion table = trans.getDataTable();
		IntArray colNs = trans.getColumnNumbers();
		int prec[] = new int[colNs.size()];
		for (int i = 0; i < colNs.size(); i++) {
			NumRange range = trans.getAttrValueRange(table.getAttributeId(colNs.elementAt(i)));
			if (range == null) {
				prec[i] = -1;
			} else {
				prec[i] = StringUtil.getPreferredPrecision(range.minValue, range.minValue, range.maxValue);
			}
		}
		for (int i = 0; i < table.getDataItemCount(); i++) {
			ThematicDataItem dit = (ThematicDataItem) table.getDataItem(i);
			if (dit == null || !(dit instanceof DataRecord)) {
				continue;
			}
			DataRecord rec = (DataRecord) dit;
			for (int j = 0; j < colNs.size(); j++)
				if (prec[j] < 0) {
					rec.setAttrValue(null, transColNs.elementAt(j));
				} else {
					double val = trans.getNumericAttrValue(colNs.elementAt(j), rec);
					String strVal = (Double.isNaN(val)) ? null : StringUtil.doubleToStr(val, prec[j]);
					rec.setNumericAttrValue(val, strVal, transColNs.elementAt(j));
				}
		}
	}
}