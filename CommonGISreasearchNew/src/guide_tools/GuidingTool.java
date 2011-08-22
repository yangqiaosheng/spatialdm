package guide_tools;

import spade.analysis.system.ESDACore;

/**
* This is the interface implemented by the components for intelligent user
* guidance and for tutoring or testing. This interface is introduce in order
* to remove direct references to the components from the main class.
*/
public interface GuidingTool {
	/**
	* Checks if the tool can run (the necessary classes are present, the
	* appropriate information is available in the system settings, etc.)
	*/
	public boolean canRun(ESDACore core);

	/**
	* Starts the tool. All necessary information and references are taken from
	* the system's core passed as an argument. Returns true if successfully started.
	*/
	public boolean start(ESDACore core);

	/**
	* Stops the tool.
	*/
	public void stop();
}
