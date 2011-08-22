package spade.kbase.scenarios;

import java.util.ResourceBundle;
import java.util.Vector;

import spade.lib.lang.Language;

/**
* Destriptions of scenarios, tasks and subasks
*/

public class TaskTree {
	/**
	* Vector of instances of TreeNode
	*/
	static ResourceBundle res = Language.getTextResource("spade.kbase.scenarios.Res_de");
	protected Vector tasks = null;

	public Vector getTasks() {
		return tasks;
	}

	public String getTaskCollectionName() {
		// following text: "Spatial analysis tasks"
		return res.getString("Spatial_analysis");
	}

	public void addTask(TreeNode task) {
		if (task == null)
			return;
		if (tasks == null) {
			tasks = new Vector(50, 20);
		}
		tasks.addElement(task);
	}

	public void setTasks(Vector tasks) {
		this.tasks = tasks;
	}

	public int getTaskCount() {
		if (tasks == null)
			return 0;
		return tasks.size();
	}

	public TreeNode getTask(int idx) {
		if (idx < 0 || idx >= getTaskCount())
			return null;
		return (TreeNode) tasks.elementAt(idx);
	}

	public String getTaskName(int idx) {
		TreeNode tn = getTask(idx);
		if (tn != null)
			return tn.getName();
		else
			return null;
	}

	public String getTaskShortName(int idx) {
		TreeNode tn = getTask(idx);
		if (tn != null)
			return tn.getShortName();
		else
			return null;
	}

	public void printTasks() {
		for (int i = 0; i < getTaskCount(); i++) {
			System.out.println(tasks.elementAt(i).toString());
		}
	}

	public TreeNode findTreeNode(String ID) {
		if (ID == null)
			return null;
		for (int i = 0; i < getTaskCount(); i++) {
			TreeNode tn = (TreeNode) tasks.elementAt(i);
			if (ID.equals(tn.getId()))
				return tn;
		}
		return null;
	}

	public int findTreeNodeIndex(String ID) {
		if (ID == null)
			return -1;
		for (int i = 0; i < getTaskCount(); i++) {
			TreeNode tn = (TreeNode) tasks.elementAt(i);
			if (ID.equals(tn.getId()))
				return i;
		}
		return -1;
	}

	public boolean treeNodeHasChildren(String ID) {
		if (ID == null || ID.length() == 0)
			return false;
		for (int i = 0; i < getTaskCount(); i++) {
			TreeNode tn = (TreeNode) tasks.elementAt(i);
			if (ID.equals(tn.getParentId()))
				return true;
		}
		return false;
	}

	public Vector getChildrenOf(String ID) {
		if (ID == null || ID.length() == 0)
			return null;
		Vector children = new Vector(10, 5);
		for (int i = 0; i < getTaskCount(); i++) {
			TreeNode tn = (TreeNode) tasks.elementAt(i);
			if (ID.equals(tn.getParentId())) {
				children.addElement(tn);
			}
		}
		if (children.size() < 1)
			return null;
		children.trimToSize();
		return children;
	}

	public Vector getAllDescendantsOf(String ID) {
		Vector children = new Vector(20, 10);
		addDescendantsOf(ID, children);
		if (children.size() < 1)
			return null;
		children.trimToSize();
		return children;
	}

	protected void addDescendantsOf(String ID, Vector children) {
		if (ID == null || ID.length() == 0)
			return;
		for (int i = 0; i < getTaskCount(); i++) {
			TreeNode tn = (TreeNode) tasks.elementAt(i);
			if (ID.equals(tn.getParentId())) {
				children.addElement(tn);
				addDescendantsOf(tn.getId(), children);
			}
		}
	}

	/**
	* Retrieves from all the task collection only the nodes having the type
	* "scenario". Returns a vector of such nodes.
	*/
	public Vector getScenarios() {
		if (tasks == null)
			return null;
		Vector scv = new Vector(10, 10);
		for (int i = 0; i < getTaskCount(); i++) {
			String type = getTask(i).getNodeType();
			if (type != null && type.equalsIgnoreCase("scenario")) {
				scv.addElement(getTask(i));
			}
		}
		scv.trimToSize();
		return scv;
	}

	/**
	* Returns the path from a topmost node (i.e. without parents) to the given
	* task. The path is a vector consisting of TreeNodes, starting from the
	* topmost node and ending with the node passed as the argument.
	*/
	public Vector getPathToTask(TreeNode node) {
		if (node == null)
			return null;
		Vector path = new Vector(5, 5);
		while (node != null) {
			path.insertElementAt(node, 0);
			if (node.getParentId() != null) {
				node = findTreeNode(node.getParentId());
			} else {
				node = null;
			}
		}
		return path;
	}

	/**
	* Returns the context of the given task. For this purpose collects all
	* context specifications along the path from the topmost ancestor to the
	* given node. The result is a vector of ContextElements.
	*/
	public Vector getTaskContext(TreeNode task) {
		Vector path = getPathToTask(task);
		if (path == null)
			return null;
		Vector context = new Vector(10, 5);
		for (int i = 0; i < path.size(); i++) {
			TreeNode node = (TreeNode) path.elementAt(i);
			if (node.context != null) {
				for (int j = 0; j < node.context.size(); j++) {
					context.addElement(node.context.elementAt(j));
				}
			}
		}
		if (context.size() < 1)
			return null;
		return context;
	}

	/**
	* Returns the vector of context elements that are necessary for the given
	* task. For this purpose collects all context specifications along the path
	* from the topmost ancestor to the given node (using the method
	* getTaskContext) and removes from them optional context elements unless
	* they are not required by restrictions of any of the tasks in the path
	*/
	public Vector getObligatoryTaskContext(TreeNode task) {
		Vector path = getPathToTask(task);
		if (path == null)
			return null;
		Vector context = new Vector(10, 5);
		for (int i = 0; i < path.size(); i++) {
			TreeNode node = (TreeNode) path.elementAt(i);
			if (node.context != null) {
				for (int j = 0; j < node.context.size(); j++) {
					ContextElement cel = (ContextElement) node.context.elementAt(j);
					if (cel.isOptional) { //check the restrictions of the tasks
						boolean required = false;
						for (int k = i + 1; k < path.size() && !required; k++) {
							TreeNode t = (TreeNode) path.elementAt(k);
							if (t.restrictions != null) {
								for (int n = 0; n < t.restrictions.size() && !required; n++) {
									Restriction r = (Restriction) t.restrictions.elementAt(n);
									if (r.type.equals("is_defined") && r.values.contains(cel.localId)) {
										required = true;
									}
								}
							}
						}
						if (!required) {
							continue;
						}
					}
					context.addElement(cel);
				}
			}
		}
		if (context.size() < 1)
			return null;
		return context;
	}

	/**
	* Checks whether the given context element is obligatory for the given task
	*/
	public boolean isObligatory(ContextElement cel, TreeNode task) {
		Vector path = getPathToTask(task);
		for (int i = 0; i < path.size(); i++) {
			TreeNode node = (TreeNode) path.elementAt(i);
			if (node.context != null && node.context.contains(cel) && !cel.isOptional)
				return true;
			if (node.restrictions != null) {
				for (int n = 0; n < node.restrictions.size(); n++) {
					Restriction r = (Restriction) node.restrictions.elementAt(n);
					if (r.type.equals("is_defined") && r.values.contains(cel.localId))
						return true;
				}
			}
		}
		return false;
	}

}
