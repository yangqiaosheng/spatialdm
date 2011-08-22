package spade.analysis.tools.table_processing;

import java.awt.Checkbox;
import java.awt.GridLayout;
import java.awt.Panel;
import java.util.Vector;

import spade.analysis.system.DataKeeper;
import spade.analysis.system.ESDACore;
import spade.analysis.tools.BaseAnalyser;
import spade.lib.basicwin.OKDialog;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import ui.TableManager;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Jun 18, 2010
 * Time: 1:55:13 PM
 * Puts identifiers and/or names of obects in new table columns, which are created
 */
public class PutNamesInColumns extends BaseAnalyser {
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
			showMessage("No tables found loaded!", true);
			return;
		}
		TableManager tman = new TableManager();
		tman.setDataKeeper(dk);
		tman.setUI(core.getUI());
		int tn = tman.selectTableNumber("Select Table");
		if (tn < 0)
			return;
		AttributeDataPortion tbl = dk.getTable(tn);
		if (!(tbl instanceof DataTable))
			return;
		DataTable table = (DataTable) tbl;
		Checkbox cbIds = new Checkbox("Make a column with the object identifiers", true);
		Checkbox cbNames = new Checkbox("Make a column with the object names", true);
		Panel p = new Panel(new GridLayout(2, 1));
		p.add(cbIds);
		p.add(cbNames);
		OKDialog okd = new OKDialog(getFrame(), "New columns", true);
		okd.addContent(p);
		okd.show();
		if (okd.wasCancelled())
			return;
		if (!cbIds.getState() && !cbNames.getState())
			return;
		int cnIds = -1, cnNames = -1;
		if (cbIds.getState()) {
			cnIds = table.getAttrCount();
			table.addAttribute("Identifier", "_id_", AttributeTypes.character);
		}
		if (cbNames.getState()) {
			cnNames = table.getAttrCount();
			table.addAttribute("Name", "_named_", AttributeTypes.character);
		}
		table.makeUniqueAttrIdentifiers();
		for (int i = 0; i < table.getDataItemCount(); i++) {
			DataRecord rec = table.getDataRecord(i);
			if (cnIds >= 0) {
				rec.setAttrValue(rec.getId(), cnIds);
			}
			if (cnNames >= 0) {
				rec.setAttrValue(rec.getName(), cnNames);
			}
		}
		Vector a = new Vector(2, 1);
		if (cnIds >= 0) {
			a.addElement(table.getAttributeId(cnIds));
		}
		if (cnNames >= 0) {
			a.addElement(table.getAttributeId(cnNames));
		}
		table.notifyPropertyChange("new_attributes", null, a);
	}
}
