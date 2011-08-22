package spade.lib.basicwin;

import java.awt.event.ActionEvent;

/**
* Unlike the base class TreeView, allows to expand only one branch of the tree
* at a time
*/
public class HierList extends TreeView {

	public HierList() {
		super();
	}

	/**
	* In adding the item sets it to be initially collapsed
	*/
	@Override
	public int addItem(String id, String name, String parentId) {
		return super.addItem(id, name, parentId, true);
	}

	/**
	* Expanding all branches is not allowed in a HierList, therefore this
	* method does nothing
	*/
	@Override
	public void expandAll() {
	}

	/**
	* If a node has an expanded child, the other children are not shown
	*/
	@Override
	protected void addChildrenOf(String parentId, int level) {
		int expChildIdx = -1, first = 0, last = getItemCount() - 1;
		for (int i = first; i <= last && expChildIdx < 0; i++) {
			ListItem lit = getItem(i);
			if (lit.hasChildren && !lit.isCollapsed && isChildOf(lit, parentId)) {
				expChildIdx = i;
			}
		}
		if (expChildIdx >= 0) {
			first = last = expChildIdx;
		}
		for (int i = first; i <= last; i++) {
			ListItem lit = getItem(i);
			if (isChildOf(lit, parentId)) {
				lit.level = level;
				order.addElement(i);
				if (lit.hasChildren && !lit.isCollapsed) {
					addChildrenOf(lit.id, level + 1);
				}
			}
		}
	}

	/**
	* Upon "action" the hierarchical list expands or collapses the corresponding
	* tree node (if it has children). If the node is expanded, all the
	* currently expanded nodes are collapsed
	*/
	@Override
	public void actionPerformed(ActionEvent evt) {
		if (order == null || order.size() < 1)
			return;
		if (evt.getSource() == ld) {
			int n = ld.getSelectedIndex();
			if (n < 0)
				return;
			activeIdx = order.elementAt(n);
			ListItem lit = getItem(activeIdx);
			if (!lit.hasChildren)
				return;
			if (lit.isCollapsed) {
				//this node must be expanded, but all the other nodes must be collapsed
				for (int i = 0; i < getItemCount(); i++) {
					ListItem item = getItem(i);
					if (item.level >= lit.level && item.hasChildren && !item.isCollapsed) {
						item.isCollapsed = true;
					}
				}
			}
			lit.isCollapsed = !lit.isCollapsed;
			setup();
			sendActionEvent();
		}
	}
}
