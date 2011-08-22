package guide_tools.tutorial;

import guide_tools.GuidingTool;
import spade.analysis.system.ESDACore;
import spade.analysis.system.SystemUI;

/**
* Starts the tutorial (test wizard)
*/
public class TutorialStarter implements GuidingTool {
	/**
	* The component providing the tutorial or doing the test
	*/
	protected Tutor tutor = null;
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
		if (core.getSystemSettings().getParameterAsString("TUTORIAL") == null)
			return false;
		boolean isApplet = !core.getSystemSettings().checkParameterValue("isLocalSystem", "true");
		if (isApplet && core.getSystemSettings().getParameterAsString("TutorialScript") == null)
			return false;
		return true;
	}

	/**
	* Starts the tool. All necessary information and references are taken from
	* the system's core passed as an argument. Returns true if successfully started.
	*/
	@Override
	public boolean start(ESDACore core) {
		if (tutor != null) {
			if (!tutor.isRunning()) {
				tutor.runTutorial();
			}
			return true; //already started
		}
		if (core == null)
			return false;
		ui = core.getUI();
		String pathToTutorial = core.getSystemSettings().getParameterAsString("TUTORIAL");
		if (pathToTutorial == null) {
			showMessage("No path to the tutorial description given!", true);
			return false;
		}
		boolean isApplet = !core.getSystemSettings().checkParameterValue("isLocalSystem", "true");
		String pathToScript = null;
		if (isApplet) {
			pathToScript = core.getSystemSettings().getParameterAsString("TutorialScript");
		}
		if (isApplet && pathToScript == null) {
			showMessage("The path to the script for storing test results is not specified!", true);
			return false;
		}
		TutorialReader tr = new TutorialReader();
		tr.setDataSource(pathToTutorial);
		if (ui != null) {
			tr.addProcessListener(ui.getStatusLine());
		}
		TutorialContent tc = tr.read();
		if (tc == null) {
			showMessage("Could not read the tutorial description!", true);
			return false;
		}
		/*
		//temporarily prints the content of the tutorial to a file tasks.txt
		try {
		  PrintStream ps=new PrintStream(new FileOutputStream("tasks.txt"));
		  tc.printToStream(ps);
		  ps.close();
		} catch (IOException ioe) {
		  System.out.println("Cannot open the output file tasks.txt:\n"+
		                      ioe.toString());
		}
		*/
		tutor = new Tutor();
		tutor.setContent(tc);
		TutorSupport ts = new TutorSupport();
		ts.setDataKeeper(core.getDataKeeper());
		ts.setDisplayProducer(core.getDisplayProducer());
		ts.setIsApplet(isApplet);
		ts.setPathToResultStoringScript(pathToScript);
		String resultDir = core.getSystemSettings().getParameterAsString("TestResultsDir");
		if (resultDir == null) {
			resultDir = "test_results";
		}
		ts.setResultDir(resultDir);
		tutor.setTutorSupport(ts);
		tutor.runTutorial();
		return true;
	}

	/**
	* Stops the tool.
	*/
	@Override
	public void stop() {
		if (tutor != null) {
			tutor.quit();
		}
		tutor = null;
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