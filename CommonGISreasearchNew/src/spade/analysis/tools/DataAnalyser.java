package spade.analysis.tools;

import spade.analysis.system.ESDACore;

/**
* The basic interface to be implemented by various tools extending the
* functionality of DescartesXXI.
*/
public interface DataAnalyser {
	/**
	* Returns true when the tool has everything necessary for its operation.
	* For example, if the tool manages a number of analysis methods, it should
	* check whether the class for at least one method is available.
	*/
	public boolean isValid(ESDACore core);

	/**
	* This method constructs and starts the tool. Everything the tool may need
	* for integration with other components of the system can be received
	* from the system's core passed as an argument.
	*/
	public void run(ESDACore core);
}