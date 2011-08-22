package spade.analysis.tools;

import spade.analysis.system.ESDACore;
import spade.lib.basicwin.SelectDialog;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Oct 23, 2009
 * Time: 11:47:26 AM
 * Groups all clustering tools, to reduce the number of items in the
 * menu of tools.
 */
public class ClusteringTools extends BaseAnalyser implements SingleInstanceTool {
	/**
	 * The register of the available tools
	 */
	protected ClusteringToolsRegister toolReg = new ClusteringToolsRegister();
	/**
	* The object managing descriptions of all available tools. A tool
	* must implement the interface DataAnalyser
	*/
	protected ToolKeeper toolKeeper = new ToolKeeper(toolReg);

	/**
	* This method constructs and starts the tool. Everything the tool may need
	* for integration with other components of the system can be received
	* from the system's core passed as an argument.
	*/
	@Override
	public void run(ESDACore core) {
		this.core = core;
		if (core == null || core.getUI() == null)
			return;
		String tools[][] = toolReg.getToolDescription();
		if (tools == null || tools.length < 1) {
			showMessage("No descriptions of clustering tools found!", true);
			return;
		}
		SelectDialog selDlg = new SelectDialog(core.getUI().getMainFrame(), "Clustering tools", "Select the tool:");
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
}
