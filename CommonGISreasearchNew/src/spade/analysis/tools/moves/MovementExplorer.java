package spade.analysis.tools.moves;

import spade.analysis.system.ESDACore;
import spade.analysis.tools.DataAnalyser;
import spade.analysis.tools.SingleInstanceTool;
import spade.analysis.tools.ToolKeeper;
import spade.lib.basicwin.SelectDialog;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 05-Apr-2007
 * Time: 11:45:19
 * Allows the user to select and run one of the existing tools
 * designed for movement data, i.e. trajectories or moves (vectors).
 */
public class MovementExplorer implements DataAnalyser, SingleInstanceTool {
	protected ESDACore core = null;

	/**
	 * The register of the available tools
	 */
	protected MovementToolRegister toolReg = new MovementToolRegister();
	/**
	* The object managing descriptions of all available tools. A tool
	* must implement the interface DataAnalyser
	*/
	protected ToolKeeper toolKeeper = new ToolKeeper(toolReg);

	/**
	 * Returns true when the tool has everything necessary for its operation.
	 * For example, if the tool manages a number of analysis methods, it should
	 * check whether the class for at least one method is available.
	 * A MovementExplorer always returns true.
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
		if (core.getUI().getCurrentMapViewer() == null || core.getUI().getCurrentMapViewer().getLayerManager() == null) {
			if (core.getDataKeeper().getTableCount() > 0) {
				try {
					Object gen = Class.forName("spade.analysis.tools.moves.MovementLayerBuilder").newInstance();
					if (gen != null && (gen instanceof DataAnalyser)) {
						((DataAnalyser) gen).run(core);
						return;
					}
				} catch (Exception e) {
					showMessage("Could not find class MovementLayerBuilder for building trajectories!", true);
				}
			}
			showMessage("No map exists!", true);
			return;
		}
		SelectDialog selDlg = new SelectDialog(core.getUI().getMainFrame(), "Explore movement data", "Select the operation:");
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

	protected void showMessage(String msg, boolean error) {
		if (core != null && core.getUI() != null) {
			core.getUI().showMessage(msg, error);
		} else if (error) {
			System.out.println("!--> " + msg);
		}
	}
}
