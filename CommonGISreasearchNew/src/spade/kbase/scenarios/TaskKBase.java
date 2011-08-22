package spade.kbase.scenarios;

import java.util.Vector;

/**
* An object containing the whole knowledge base on task support in internal
* structures.
*/
public class TaskKBase {
	/**
	* Destriptions of scenarios, tasks and subasks
	*/
	public TaskTree tasks = null;
	/**
	* Destriptions of cartographic visualisation methods
	*/
	public VisCollection visMethods = null;
	/**
	* Primitive tasks like "compare", "select" etc.
	*/
	public Vector primTasks = null;
	/**
	* Destriptions of analysis tools such as plots etc.
	*/
	public Vector tools = null;

	public int getPrimTaskCount() {
		if (primTasks == null)
			return 0;
		return primTasks.size();
	}

	public PrimitiveTask getPrimTask(int idx) {
		if (idx < 0 || idx >= getPrimTaskCount())
			return null;
		return (PrimitiveTask) primTasks.elementAt(idx);
	}

	public int getPrimTaskIndex(String taskId) {
		if (taskId == null)
			return -1;
		for (int i = 0; i < getPrimTaskCount(); i++) {
			PrimitiveTask pt = (PrimitiveTask) primTasks.elementAt(i);
			if (pt.getId().equals(taskId))
				return i;
		}
		return -1;
	}

	public PrimitiveTask getPrimTask(String taskId) {
		return getPrimTask(getPrimTaskIndex(taskId));
	}

	public String getPrimTaskName(int idx) {
		PrimitiveTask pt = getPrimTask(idx);
		if (pt == null)
			return null;
		return pt.getName();
	}

	public String getPrimTaskName(String taskId) {
		return getPrimTaskName(getPrimTaskIndex(taskId));
	}

	public int getToolCount() {
		if (tools == null)
			return 0;
		return tools.size();
	}

	public Tool getTool(int idx) {
		if (idx < 0 || idx >= getToolCount())
			return null;
		return (Tool) tools.elementAt(idx);
	}

	public String getToolName(int idx) {
		Tool t = getTool(idx);
		if (t == null)
			return null;
		return t.getName();
	}

	public Tool getTool(String function) {
		if (function == null)
			return null;
		for (int i = 0; i < getToolCount(); i++) {
			Tool tool = getTool(i);
			if (function.equals(tool.function))
				return tool;
		}
		return null;
	}
}
