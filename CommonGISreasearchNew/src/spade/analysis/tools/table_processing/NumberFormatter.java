package spade.analysis.tools.table_processing;

import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.FlowLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.util.Vector;

import spade.analysis.system.DataKeeper;
import spade.analysis.system.ESDACore;
import spade.analysis.tools.BaseAnalyser;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.OKDialog;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.DataTable;
import ui.AttributeChooser;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Nov 16, 2010
 * Time: 2:51:26 PM
 * Transforms string representations of numbers in a table to
 * an appropriate (automatically selected) or desired (user-chosen) precision
 */
public class NumberFormatter extends BaseAnalyser {
	/**
	* This method constructs and starts the tool. Everything the tool may need
	* for integration with other components of the system can be received
	* from the system's core passed as an argument.
	*/
	@Override
	public void run(ESDACore core) {
		this.core = core;
		if (core == null)
			return;
		DataKeeper dk = core.getDataKeeper();
		if (dk == null || dk.getTableCount() < 1) {
			showMessage("No tables found!", true);
			return;
		}
		AttributeDataPortion table = TableProcessor.selectTable(core);
		if (table == null)
			return;
		if (table.getDataItemCount() < 1) {
			showMessage("No data in the table!", true);
			return;
		}
		if (!(table instanceof DataTable)) {
			showMessage("This is not a DataTable!", true);
			return;
		}
		AttributeChooser ac = new AttributeChooser();
		Vector attr = ac.selectColumns(table, null, null, true, "Select the columns to re-format the strings representing the numbers", core.getUI());
		if (attr == null || attr.size() < 1)
			return;
		int colNs[] = new int[attr.size()];
		Vector attrIds = new Vector(colNs.length, 1);
		for (int i = 0; i < attr.size(); i++) {
			colNs[i] = -1;
			Attribute at = (Attribute) attr.elementAt(i);
			if (!at.isNumeric()) {
				continue;
			}
			colNs[i] = table.getAttrIndex(at.getIdentifier());
			if (colNs[i] >= 0) {
				attrIds.addElement(at.getIdentifier());
			}
		}
		if (attrIds.size() < 1) {
			if (attr.size() == 1) {
				showMessage("Not a numeric attribute!", true);
			} else {
				showMessage("Not numeric attributes!", true);
			}
			return;
		}
		Panel p = new Panel(new ColumnLayout());
		p.add(new Label("What do you prefer?"));
		CheckboxGroup cbg = new CheckboxGroup();
		Checkbox cbAuto = new Checkbox("A suitable precision for each column is chosen automatically", true, cbg);
		p.add(cbAuto);
		Checkbox cbManual = new Checkbox("The precision must be", false, cbg);
		Panel pp = new Panel(new FlowLayout(FlowLayout.LEFT, 1, 1));
		pp.add(cbManual);
		TextField tf = new TextField(2);
		pp.add(tf);
		pp.add(new Label("digits after the decimal point"));
		p.add(pp);
		OKDialog dia = new OKDialog(getFrame(), "Desired precision", true);
		dia.addContent(p);
		dia.show();
		if (dia.wasCancelled())
			return;
		if (cbAuto.getState()) {
			((DataTable) table).setNiceStringsForNumbers(colNs, false);
		} else {
			int prec = -1;
			while (prec < 0) {
				String str = tf.getText();
				if (str == null) {
					showMessage("No precision specified!", true);
				} else {
					str = str.trim();
					if (str.length() < 1) {
						showMessage("No precision specified!", true);
					} else {
						try {
							prec = Integer.parseInt(str);
							if (prec < 0) {
								showMessage("The precision must be positive or 0!", true);
							}
						} catch (Exception e) {
							showMessage("Not a numeric value entered!", true);
						}
					}
				}
				if (prec < 0) {
					dia.show();
					if (dia.wasCancelled())
						return;
				}
			}
			((DataTable) table).setNiceStringsForNumbers(colNs, prec);
		}
		showMessage("Reformatting completed!", false);
		table.notifyPropertyChange("values", null, attrIds);
	}
}
