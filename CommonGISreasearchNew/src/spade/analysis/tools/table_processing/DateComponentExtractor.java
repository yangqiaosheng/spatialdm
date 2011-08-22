package spade.analysis.tools.table_processing;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.TextField;
import java.util.Vector;

import spade.analysis.system.DataKeeper;
import spade.analysis.system.ESDACore;
import spade.analysis.tools.BaseAnalyser;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.OKDialog;
import spade.lib.util.IdMaker;
import spade.lib.util.IntArray;
import spade.time.Date;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataTable;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Apr 9, 2010
 * Time: 11:04:57 AM
 * Extracts components from dates contained in a table column.
 */
public class DateComponentExtractor extends BaseAnalyser {
	/**
	 * Additional date components, obtained with the help of GregorianCalendar
	 */
	public static final char addDC[] = { 'D', 'E', 'W', 'O' };
	public static final String addDCNames[] = { "day of week", "day of year", "week of year", "daylight saving time offset (hours)" };

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
		AttributeDataPortion tbl = TableProcessor.selectTable(core);
		if (tbl == null)
			return;
		if (!(tbl instanceof DataTable)) {
			showMessage("The table is not DataTable!", true);
			return;
		}
		if (tbl.getDataItemCount() < 1) {
			showMessage("No data in the table!", true);
			return;
		}
		DataTable table = (DataTable) tbl;
		IntArray colNs = new IntArray(table.getAttrCount(), 10);
		List list = new List(Math.min(table.getAttrCount(), 5));
		for (int i = 0; i < table.getAttrCount(); i++) {
			Attribute at = table.getAttribute(i);
			if (at.isTemporal()) {
				IntArray iar = new IntArray(1, 1);
				iar.addElement(i);
				Vector v = table.getKValuesFromColumns(iar, 1);
				if (v == null || v.size() < 1) {
					continue;
				}
				if (!(v.elementAt(0) instanceof Date)) {
					continue;
				}
				Date d = (Date) v.elementAt(0);
				int nComp = 0;
				for (int j = 0; j < Date.time_symbols.length && nComp < 2; j++)
					if (d.hasElement(Date.time_symbols[j])) {
						++nComp;
					}
				if (nComp < 2) {
					continue;
				}
				colNs.addElement(i);
				list.add(at.getName());
			}
		}
		if (colNs.size() < 1) {
			showMessage("The table has no columns with dates!", true);
			return;
		}
		list.select(0);
		Panel p = new Panel(new BorderLayout());
		p.add(new Label("Select the table column containing dates:"), BorderLayout.NORTH);
		p.add(list, BorderLayout.CENTER);
		OKDialog dia = new OKDialog(getFrame(), "Extract date components", true);
		dia.addContent(p);
		dia.show();
		if (dia.wasCancelled())
			return;
		int colN = colNs.elementAt(list.getSelectedIndex());
		IntArray iar = new IntArray(1, 1);
		iar.addElement(colN);
		Vector v = table.getKValuesFromColumns(iar, 1);
		Date d = (Date) v.elementAt(0);
		int nComp = Date.time_symbols.length;
		boolean weeksDefined = d.hasElement('y') && d.hasElement('m') && d.hasElement('d');
		if (weeksDefined) {
			nComp += addDC.length;
		}
		Checkbox cb[] = new Checkbox[nComp];
		p = new Panel(new ColumnLayout());
		p.add(new Label("Which components of the dates to extract?"));
		for (int i = 0; i < Date.time_symbols.length; i++) {
			cb[i] = null;
			if (d.hasElement(Date.time_symbols[i])) {
				cb[i] = new Checkbox(Date.getTextForTimeSymbol(Date.time_symbols[i]), false);
				p.add(cb[i]);
			}
		}
		for (int i = Date.time_symbols.length; i < nComp; i++) {
			int ii = i - Date.time_symbols.length;
			cb[i] = new Checkbox(addDCNames[ii], false);
			p.add(cb[i]);
		}
		dia = new OKDialog(getFrame(), "Extract date components", true);
		dia.addContent(p);
		dia.show();
		if (dia.wasCancelled())
			return;
		int nSelected = 0;
		for (Checkbox element : cb)
			if (element != null && element.getState()) {
				++nSelected;
			}
		if (nSelected < 1) {
			showMessage("None of the components have been selected!", true);
			return;
		}
		char cs[] = new char[nSelected];
		TextField tf[] = new TextField[nSelected];
		int idx = 0;
		String aName = table.getAttributeName(colN);
		for (int i = 0; i < Date.time_symbols.length; i++)
			if (cb[i] != null && cb[i].getState()) {
				cs[idx] = Date.time_symbols[i];
				tf[idx] = new TextField(aName + ": " + cb[i].getLabel());
				idx++;
			}
		for (int i = Date.time_symbols.length; i < nComp; i++)
			if (cb[i] != null && cb[i].getState()) {
				int ii = i - Date.time_symbols.length;
				cs[idx] = addDC[ii];
				tf[idx] = new TextField(aName + ": " + cb[i].getLabel());
				idx++;
			}
		p = new Panel(new ColumnLayout());
		if (nSelected == 1) {
			p.add(new Label("A new attribute (column) will be added to the table."));
			p.add(new Label("Edit the name of the attribute if desired."));
		} else {
			p.add(new Label(nSelected + " new attributes (columns) will be added to the table."));
			p.add(new Label("Edit the names of the attributes if desired."));
		}
		for (int i = 0; i < nSelected; i++) {
			p.add(tf[i]);
		}
		dia = new OKDialog(getFrame(), "New attributes", true);
		dia.addContent(p);
		dia.show();
		if (dia.wasCancelled())
			return;
		int aIdx0 = table.getAttrCount();
		for (int i = 0; i < nSelected; i++) {
			String cName = tf[i].getText().trim();
			table.addAttribute(cName, IdMaker.makeId(cName, table), AttributeTypes.integer);
		}
		table.makeUniqueAttrIdentifiers();
		for (int nr = 0; nr < table.getDataItemCount(); nr++) {
			Object val = table.getAttrValue(colN, nr);
			if (val == null || !(val instanceof Date)) {
				continue;
			}
			d = (Date) val;
			for (int i = 0; i < nSelected; i++) {
				int num = -1;
				switch (cs[i]) {
				case 'D':
					num = d.getDayOfWeek();
					break;
				case 'E':
					num = d.getDayOfYear();
					break;
				case 'W':
					num = d.getWeekOfYear();
					break;
				case 'O':
					num = d.getDSTimeOffset();
					break;
				default:
					num = d.getElementValue(cs[i]);
				}
				table.setNumericAttributeValue(num, aIdx0 + i, nr);
			}
		}
		v = new Vector(nSelected, 1);
		for (int i = 0; i < nSelected; i++) {
			v.addElement(table.getAttributeId(aIdx0 + i));
		}
		table.notifyPropertyChange("new_attributes", null, v);
		showMessage("Finished extracting date components", false);
	}
}
