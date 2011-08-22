package spade.analysis.tools.table_processing;

import java.util.Vector;

import spade.analysis.system.DataKeeper;
import spade.analysis.system.ESDACore;
import spade.analysis.tools.BaseAnalyser;
import spade.lib.basicwin.Dialogs;
import spade.vis.database.AttributeDataPortion;
import ui.TableManager;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Jun 18, 2010
 * Time: 1:34:05 PM
 * Removes selected records from a table
 */
public class RecordRemover extends BaseAnalyser {
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
		AttributeDataPortion table = dk.getTable(tn);
		Vector selected = core.getHighlighterForSet(table.getEntitySetIdentifier()).getSelectedObjects();
		if (selected == null || selected.size() < 1) {
			showMessage("None of the table records is selected!", true);
			return;
		}
		if (!Dialogs.askYesOrNo(getFrame(), "Do you really wish to delete the selected table records?", "Sure to delete?"))
			return;
		for (int i = table.getDataItemCount() - 1; i >= 0; i--)
			if (selected.contains(table.getDataItemId(i))) {
				table.removeDataItem(i);
			}
		table.rebuildDataIndex();
		table.notifyPropertyChange("data_updated", null, null);
	}
}
