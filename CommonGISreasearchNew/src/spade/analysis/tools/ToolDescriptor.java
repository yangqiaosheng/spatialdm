package spade.analysis.tools;

/**
* A ToolDescriptor contains a list (array) of descriptions of some tools where
* for each tool the following information is specified:
* 1) tool identifier (internal);
* 2) tool name (may be shown to users);
* 3) the full name of the class implementing the tool
*/
public interface ToolDescriptor {
	/**
	* Returns the description of the known tools
	*/
	public String[][] getToolDescription();
}