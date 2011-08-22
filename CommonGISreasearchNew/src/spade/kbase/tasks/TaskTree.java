package spade.kbase.tasks;

import java.util.Vector;

public class TaskTree {
	protected Vector tree = null; // Vector of TreeNode

	public Vector getTasks() {
		return tree;
	}

	public String getTaskCollectionName() {
		return "Spatial analysis tasks";
	}

	public void addTask(TreeNode task) {
		if (task == null)
			return;
		if (tree == null) {
			tree = new Vector(50, 20);
		}
		tree.addElement(task);
	}

	public int getTaskCount() {
		if (tree == null)
			return 0;
		return tree.size();
	}

	public TreeNode getTask(int idx) {
		if (idx < 0 || idx >= getTaskCount())
			return null;
		return (TreeNode) tree.elementAt(idx);
	}

	public String getTaskName(int idx) {
		TreeNode tn = getTask(idx);
		if (tn != null)
			return tn.getName();
		else
			return null;
	}

	public void propagateConstraints() {
		for (int i = 0; i < getTaskCount(); i++) {
			TreeNode tn = (TreeNode) tree.elementAt(i);
			if (tn.getParentId() == null) {
				propagateConstraintsFromNode(i);
			}
		}
	}

	protected void propagateConstraintsFromNode(int nn) {
		TreeNode tn = (TreeNode) tree.elementAt(nn);
		for (int i = nn + 1; i < tree.size(); i++) {
			TreeNode child = (TreeNode) tree.elementAt(i);
			if (tn.getId() == null && child.getParentId() == null || tn.getId().equals(child.getParentId())) {
				if (tn.context != null && tn.context.size() > 0) {
					if (child.context == null) {
						child.context = new Vector(tn.context.size(), tn.context.size());
					}
					for (int j = 0; j < tn.context.size(); j++) {
						child.context.insertElementAt(tn.context.elementAt(j), j);
					}
				}
				propagateConstraintsFromNode(i);
			}
		}
	}

	public void printTasks() {
		for (int i = 0; i < getTaskCount(); i++) {
			TreeNode tn = (TreeNode) tree.elementAt(i);
			System.out.println("* id=" + tn.getId() + ", parent=" + tn.getParentId());
			System.out.println("* Text=" + tn.getName());
			System.out.println("* Explanation=" + tn.getExplanation());
			if (tn.context != null) {
				for (int j = 0; j < tn.context.size(); j++) {
					ContextElement ce = (ContextElement) tn.context.elementAt(j);
					System.out.println("* ContextElement type=" + ce.getTypeName());
					System.out.println("* ContextElement name=" + ce.getName());
					System.out.println("** Explanation=" + ce.getExplanation());
					System.out.println("** Instruction=" + ce.getInstruction());
					if (ce.getRestriction() != null) {
						System.out.println(ce.getRestriction().toString());
					}
				}
			}
			if (tn.nextTasks != null) {
				//...
			}
		}
	}

	public TreeNode findTreeNode(String ID) {
		if (ID == null)
			return null;
		for (int i = 0; i < getTaskCount(); i++) {
			TreeNode tn = (TreeNode) tree.elementAt(i);
			if (ID.equals(tn.getId()))
				return tn;
		}
		return null;
	}

	public int findTreeNodeIndex(String ID) {
		if (ID == null)
			return -1;
		for (int i = 0; i < getTaskCount(); i++) {
			TreeNode tn = (TreeNode) tree.elementAt(i);
			if (ID.equals(tn.getId()))
				return i;
		}
		return -1;
	}

	public boolean treeNodeHasChildren(String ID) {
		if (ID == null || ID.length() == 0)
			return false;
		for (int i = 0; i < getTaskCount(); i++) {
			TreeNode tn = (TreeNode) tree.elementAt(i);
			if (ID.equals(tn.getParentId()))
				return true;
		}
		return false;
	}

}