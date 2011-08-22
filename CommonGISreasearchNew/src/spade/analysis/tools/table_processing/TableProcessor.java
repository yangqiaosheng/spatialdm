package spade.analysis.tools.table_processing;

import spade.analysis.system.DataKeeper;
import spade.analysis.system.ESDACore;
import spade.analysis.tools.BaseAnalyser;
import spade.analysis.tools.DataAnalyser;
import spade.analysis.tools.ToolKeeper;
import spade.lib.basicwin.SelectDialog;
import spade.vis.database.AttributeDataPortion;
import ui.TableManager;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Apr 9, 2010
 * Time: 10:50:33 AM
 * Performs various operations on tables
 */
public class TableProcessor extends BaseAnalyser {
	/**
	 * The register of the available tools
	 */
	protected TableToolsRegister toolReg = new TableToolsRegister();
	/**
	* The object managing descriptions of all available tools. A tool
	* must implement the interface DataAnalyser
	*/
	protected ToolKeeper toolKeeper = new ToolKeeper(toolReg);

	/**
	 * Returns true when the tool has everything necessary for its operation.
	 * For example, if the tool manages a number of analysis methods, it should
	 * check whether the class for at least one method is available.
	 * An EventExplorer always returns true.
	 */
	@Override
	public boolean isValid(ESDACore core) {
		return true;
	}

	/**
	* This method constructs and starts the tool. Everything the tool may need
	* for integration with other components of the system can be received
	* from the system's core passed as an argument.
	*/
	@Override
	public void run(ESDACore core) {
		if (core == null || core.getUI() == null)
			return;
		this.core = core;
		SelectDialog selDlg = new SelectDialog(core.getUI().getMainFrame(), "Table data processing", "Select the operation:");
		String tools[][] = toolReg.getToolDescription();
		for (String[] tool : tools) {
			selDlg.addOption(tool[1], tool[0], false);
		}
		selDlg.show();
		if (selDlg.wasCancelled())
			return;
		int idx = selDlg.getSelectedOptionN();
		if (idx < 0)
			return;
		Object obj = toolKeeper.getTool(tools[idx][0]);
		if (obj == null) {
			if (core.getUI() != null) {
				core.getUI().showMessage(toolKeeper.getErrorMessage(), true);
			}
			return;
		}
		if (!(obj instanceof DataAnalyser)) {
			if (core.getUI() != null) {
				core.getUI().showMessage("Incorrect tool implementation: " + obj.getClass().getName() + " is not a DataAnalyser!", true);
			}
			return;
		}
		DataAnalyser dan = (DataAnalyser) obj;
		dan.run(core);
	}

	public static AttributeDataPortion selectTable(ESDACore core) {
		DataKeeper dk = core.getDataKeeper();
		if (dk.getTableCount() < 1)
			return null;
		TableManager tman = new TableManager();
		tman.setDataKeeper(dk);
		tman.setUI(core.getUI());
		int tn = tman.selectTableNumber("Select a table");
		if (tn < 0)
			return null;
		return dk.getTable(tn);
	}

}
