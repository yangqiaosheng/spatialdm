package spade.analysis.tools;

import java.util.Vector;

import spade.analysis.system.ESDACore;

/**
* The class is used for keeping the register of available tools extending
* the functionality of Descartes. Checks availability of the tools registered
* in spade.analysis.tools.ExtraToolRegister (i.e. whether the corresponding
* classes are included in system configuration). Informs about the number of
* available tools and their names and starts the tools.
*/
public class ExtraToolManager {
	/**
	* The object managing descriptions of all available tools. A tool
	* must implement the interface DataAnalyser
	*/
	protected ToolKeeper toolKeeper = new ToolKeeper(new ExtraToolRegister());
	/**
	* Indicates which of the available tools are valid (can run in the current
	* configuration)
	*/
	protected boolean isValid[] = null;

	/**
	* Check which of the available tools are valid
	*/
	protected void checkValidity(ESDACore core) {
		if (isValid == null) {
			Vector tools = toolKeeper.getAvailableToolList();
			if (tools == null || tools.size() < 1)
				return;
			isValid = new boolean[tools.size()];
			for (int i = 0; i < isValid.length; i++) {
				Object obj = toolKeeper.getTool(toolKeeper.getAvailableToolId(i));
				if (obj == null || !(obj instanceof DataAnalyser)) {
					isValid[i] = false;
				} else {
					DataAnalyser dan = (DataAnalyser) obj;
					isValid[i] = dan.isValid(core);
				}
			}
		}
	}

	/**
	* Returns the number of available tools
	*/
	public int getAvailableToolCount(ESDACore core) {
		if (isValid == null) {
			checkValidity(core);
		}
		if (isValid == null)
			return 0;
		int num = 0;
		for (boolean element : isValid)
			if (element) {
				++num;
			}
		return num;
	}

	/**
	* Returns the identifier of the AVAILABLE tool with the given index
	*/
	public String getAvailableToolId(int toolN) {
		if (isValid == null)
			return null;
		int num = -1;
		for (int i = 0; i < isValid.length; i++)
			if (isValid[i]) {
				++num;
				if (num == toolN)
					return toolKeeper.getAvailableToolId(i);
			}
		return null;
	}

	/**
	* Returns the full name of the AVAILABLE tool with the given index
	*/
	public String getAvailableToolName(int toolN) {
		if (isValid == null)
			return null;
		int num = -1;
		for (int i = 0; i < isValid.length; i++)
			if (isValid[i]) {
				++num;
				if (num == toolN)
					return toolKeeper.getAvailableToolName(i);
			}
		return null;
	}

	/**
	* Starts the tool with the given identifier
	*/
	public void startTool(String toolId, ESDACore core) {
		if (core == null)
			return;
		Object obj = toolKeeper.getTool(toolId);
		if (obj == null) {
			if (core.getUI() != null) {
				core.getUI().showMessage(toolKeeper.getErrorMessage(), true);
			}
			return;
		}
		if (!(obj instanceof DataAnalyser)) {
			if (core.getUI() != null) {
				core.getUI().showMessage("Incorrect tool implementation: " + toolId + " is not a DataAnalyser!", true);
			}
			return;
		}
		DataAnalyser dan = (DataAnalyser) obj;
		dan.run(core);
	}
}
