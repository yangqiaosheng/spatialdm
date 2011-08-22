package guide_tools.guide;

import guide_tools.GuidingTool;
import spade.analysis.system.ESDACore;
import spade.analysis.system.SystemUI;
import spade.analysis.system.ToolManager;
import spade.kbase.scenarios.KBReader;
import spade.kbase.scenarios.TaskKBase;

/**
* Starts the intelligent guidance.
*/
public class GuideStarter implements GuidingTool {
	/**
	* The component doing the guidance
	*/
	protected AnalysisGuide guide = null;
	/**
	* The UI is used for displaying diagnostic messages
	*/
	protected SystemUI ui = null;

	/**
	* Checks if the tool can run (the necessary classes are present, the
	* appropriate information is available in the system settings, etc.)
	*/
	@Override
	public boolean canRun(ESDACore core) {
		if (core == null)
			return false;
		if (core.getSystemSettings().getParameterAsString("TASK_KBASE") == null)
			return false;
		return true;
	}

	/**
	* Starts the tool. All necessary information and references are taken from
	* the system's core passed as an argument. Returns true if successfully started.
	*/
	@Override
	public boolean start(ESDACore core) {
		if (guide != null && guide.isRunning())
			return true; //already started
		if (guide != null) {
			guide.start();
			return true;
		}
		if (core == null)
			return false;
		ui = core.getUI();
		String pathToTaskKB = core.getSystemSettings().getParameterAsString("TASK_KBASE");
		if (pathToTaskKB == null) {
			showMessage("No path to the knowledge base specified!", true);
			return false;
		}
		KBReader kbReader = new KBReader();
		TaskKBase kb = kbReader.getKB(pathToTaskKB);
		if (kb == null) {
			showMessage("Could not read the knowledge base!", true);
			return false;
		}
		GuideSupport gs = new GuideSupport();
		gs.setDataKeeper(core.getDataKeeper());
		gs.setDisplayProducer(core.getDisplayProducer());
		Object calcMan = core.getTool("CalcManager");
		if (calcMan != null && (calcMan instanceof ToolManager)) {
			gs.setCalculationManager((ToolManager) calcMan);
		}
		guide = new AnalysisGuide(kb, gs);
		if (core.getUI() != null) {
			guide.setMainWindow(core.getUI().getMainFrame());
		}
		guide.start();
		return true;
	}

	/**
	* Stops the tool.
	*/
	@Override
	public void stop() {
		if (guide != null) {
			guide.finish();
		}
		guide = null;
	}

	/**
	* Displays the notification message using the system UI. The second argument
	* indicates whether this is an error message.
	*/
	protected void showMessage(String msg, boolean error) {
		if (ui != null) {
			ui.showMessage(msg, error);
		}
		if (msg != null && error) {
			System.err.println("ERROR: " + msg);
		}
	}
}