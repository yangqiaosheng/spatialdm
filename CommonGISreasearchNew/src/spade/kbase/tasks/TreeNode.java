package spade.kbase.tasks;

import java.util.Vector;

public class TreeNode {
	protected String id = null, parentId = null;
	public Vector nextTasks = null, // includes NextTaskID (String), NextTaskExpl (Vector)
			context = null, // contextElements
			operations = null; //tools to support the task

	public String name = null;
	public String explanation = null;
	public String showDataOnMap = null;
	public String mapViewInstruction = null;

	public Object visSpec = null;

	public TreeNode(String id, String parentId) {
		this.id = id;
		if (parentId != null) {
			parentId = parentId.trim();
			if (parentId.length() > 0) {
				this.parentId = parentId;
			}
		}
	}

	public String getId() {
		return id;
	}

	public String getParentId() {
		return parentId;
	}

	public String getName() {
		return name;
	}

	public void setName(String txt) {
		name = txt;
	}

	public String getExplanation() {
		return explanation;
	}

	public void setExplanation(String txt) {
		explanation = txt;
	}

	public void setVisRequirements(VisReq vr) {
		visSpec = vr;
	}

	public void setVisCombination(VisCombination vc) {
		if (vc != null && vc.getNComponents() > 0)
			if (vc.getNComponents() > 1) {
				visSpec = vc;
			} else {
				visSpec = vc.getVisComponent(0);
			}
	}

	public void addOperationSpec(OperationSpec op) {
		if (op == null)
			return;
		if (operations == null) {
			operations = new Vector(10, 5);
		}
		if (!operations.contains(op)) {
			operations.addElement(op);
		}
	}

	public int getNOperations() {
		if (operations == null)
			return 0;
		return operations.size();
	}

	public OperationSpec getOperationSpec(int idx) {
		if (idx < 0 || idx >= getNOperations())
			return null;
		return (OperationSpec) operations.elementAt(idx);
	}

	public void addNextTaskSpec(NextTaskSpec task) {
		if (task == null)
			return;
		if (nextTasks == null) {
			nextTasks = new Vector(10, 5);
		}
		if (!nextTasks.contains(task)) {
			nextTasks.addElement(task);
		}
	}

	public int getNNextTasks() {
		if (nextTasks == null)
			return 0;
		return nextTasks.size();
	}

	public NextTaskSpec getNextTaskSpec(int idx) {
		if (idx < 0 || idx >= getNNextTasks())
			return null;
		return (NextTaskSpec) nextTasks.elementAt(idx);
	}
}
