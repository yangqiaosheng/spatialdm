package guide_tools.guide;

import java.awt.Color;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import spade.analysis.system.SystemManager;
import spade.kbase.scenarios.ContextElement;
import spade.kbase.scenarios.TaskKBase;
import spade.kbase.scenarios.TreeNode;
import spade.lib.help.Helper;

/**
* The GuideCore contains the knowledge base, the context currently defined,
* references to the system manager, current scenario etc. as well as
* some constants used in different components of the Guide. The GuideCore
* defines also some methods to be shared by the components.
*/
public class GuideCore implements ActionListener {
	public static Color bkgPanelColor = new Color(255, 255, 192), bkgPromptColor = new Color(255, 192, 192), bkgTaskNameColor = new Color(192, 192, 255), bkgSubtasksColor = new Color(200, 210, 255), bkgInstrumentsColor = new Color(210, 255, 210),
			bkgInstructionColor = new Color(192, 255, 192), visFailBkgColor = new Color(255, 192, 192), visMethodsBkgColor = new Color(255, 255, 192), txtPrimTaskColor = Color.blue.darker(), txtManipulatorNameColor = Color.red.darker(),
			bkgManipulatorColor = new Color(208, 255, 164), bkgManipulatorInstrColor = new Color(208, 255, 140);

	public SystemManager sysMan = null;
	public TaskKBase kb = null;
	public Context context = null;
	/**
	* Current scenario
	*/
	public TreeNode scenario = null;
	/**
	* Number of the current work map
	*/
	public int mapN = 0;
	/**
	* The current window of the guide (if exists)
	*/
	protected Frame currWin = null;

	public GuideCore(SystemManager sMan, TaskKBase kbase) {
		sysMan = sMan;
		kb = kbase;
		context = new Context();
		context.setSystemManager(sysMan);
	}

	/**
	* Sets the current window of the guide (the argument may be null).
	* The window may be needed for constructing dialogs.
	*/
	public void setCurrentFrame(Frame fr) {
		currWin = fr;
	}

	/**
	* Returns the current window of the guide (may be null).
	* The window may be needed for constructing dialogs.
	*/
	public Frame getCurrentFrame() {
		return currWin;
	}

	/**
	* Checks if the obligatory context of the given task is defined
	*/
	public boolean isContextDefined(TreeNode task) {
		if (task == null)
			return true;
		Vector tcon = kb.tasks.getObligatoryTaskContext(task);
		if (tcon == null)
			return true;
		for (int i = 0; i < tcon.size(); i++) {
			ContextElement cel = (ContextElement) tcon.elementAt(i);
			if (!context.isContextElementDefined(cel.localId))
				return false;
		}
		return true;
	}

	/**
	* Checks if the context of the given task can be defined and if
	* the restrictions are fulfilled
	*/
	public boolean isTaskPerformable(TreeNode task) {
		if (task == null)
			return false;
		Vector tcon = kb.tasks.getObligatoryTaskContext(task);
		if (tcon == null)
			return true;
		for (int i = 0; i < tcon.size(); i++) {
			ContextElement cel = (ContextElement) tcon.elementAt(i);
			if (cel.type == null) {
				continue;
			}
			if (!context.isContextTypeSupported(cel.type)) {
				continue;
			}
			if (!context.canBeDefined(cel, mapN))
				return false;
		}
		return true;
	}

	/**
	* Finds in the task base the task with the given identifier
	*/
	public TreeNode getTask(String taskId) {
		if (taskId == null)
			return null;
		return kb.tasks.findTreeNode(taskId);
	}

	/**
	* Reacts to "help_..." commands. The command should end with the identifier
	* of the help topic to display
	*/
	@Override
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		if (cmd.startsWith("help_")) {
			Helper.help(cmd.substring(5));
			return;
		}
	}
}