package guide_tools;

import spade.analysis.system.ESDACore;

/**
* The register of known tools for user guidance, training, or testing.
*/
public class GuidingToolRegister {
	/**
	* For each tool the following information is specified:
	* 1) tool identifier (internal);
	* 2) tool name (may be shown to users);
	* 3) the full name of the class implementing the tool
	* All tools must implement the interface GuidingTool.
	*/
	protected String toolInfo[][] = { { "tutorial", "Tutorial", "guide_tools.tutorial.TutorialStarter" }, { "guide", "Task support guide", "guide_tools.guide.GuideStarter" } };
	/**
	* Instances of the classes implementing the tools. The instances are kept
	* only if the classes are available and the tools can be run (i.e. can
	* get everything what they need from the system's settings).
	*/
	protected GuidingTool toolsAvail[] = null;

	/**
	* Checks if the classes for the tools are available and the tools can be run
	* (i.e. can get everything what they need from the system's settings).
	* If so, keeps the instances of the tools in its internal array.
	* Returns the number of available tools.
	*/
	public int checkTools(ESDACore core) {
		if (toolsAvail == null) {
			toolsAvail = new GuidingTool[toolInfo.length];
			for (int i = 0; i < toolsAvail.length; i++) {
				toolsAvail[i] = null;
			}
		}
		int n = 0;
		for (int i = 0; i < toolsAvail.length; i++) {
			if (toolsAvail[i] == null) {
				try {
					Class cl = Class.forName(toolInfo[i][2]);
					if (cl != null) {
						Object obj = cl.newInstance();
						if (obj != null && (obj instanceof GuidingTool)) {
							toolsAvail[i] = (GuidingTool) obj;
						}
					}
				} catch (Exception e) {
				}
			}
			if (toolsAvail[i] != null)
				if (!toolsAvail[i].canRun(core)) {
					toolsAvail[i] = null;
				} else {
					++n;
				}
		}
		return n;
	}

	protected int getAvailableToolCount() {
		if (toolsAvail == null)
			return 0;
		int n = 0;
		for (GuidingTool element : toolsAvail)
			if (element != null) {
				++n;
			}
		return n;
	}

	public int getToolCount() {
		return toolInfo.length;
	}

	public String getToolId(int idx) {
		if (idx < 0 || idx >= toolInfo.length)
			return null;
		return toolInfo[idx][0];
	}

	public String getToolName(int idx) {
		if (idx < 0 || idx >= toolInfo.length)
			return null;
		return toolInfo[idx][1];
	}

	public boolean isToolValid(int idx) {
		return toolsAvail != null && toolsAvail[idx] != null;
	}

	public int getToolIndex(String toolId) {
		if (toolId == null)
			return -1;
		for (int i = 0; i < toolInfo.length; i++)
			if (toolId.equals(toolInfo[i][0]))
				return i;
		return -1;
	}

	public boolean startTool(int idx, ESDACore core) {
		if (toolsAvail == null) {
			checkTools(core);
		}
		if (idx < 0 || idx >= toolsAvail.length || toolsAvail[idx] == null)
			return false;
		return toolsAvail[idx].start(core);
	}

	public boolean startTool(String toolId, ESDACore core) {
		return startTool(getToolIndex(toolId), core);
	}

	public void stopAllTools() {
		if (toolsAvail == null)
			return;
		for (GuidingTool element : toolsAvail)
			if (element != null) {
				element.stop();
			}
	}
}
