package spade.lib.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.List;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import spade.lib.basicwin.CrossDrawer;
import spade.lib.basicwin.TImgButton;
import spade.lib.basicwin.TriangleDrawer;
import spade.lib.util.IntArray;
import spade.lib.util.Named;

/**
* Allows the user to change the order of items in the given list. The items
* of the list are either strings, or some objects implementing the interface
* spade.lib.util.Named, or any objects with the properly defined method toString().
*/
public class ListOrderer extends Panel implements ActionListener {
	/**
	* The list to be reordered. The items of the list are either strings or some
	* objects implementing the interface spade.lib.util.Named.
	*/
	protected Vector items = null;
	/**
	* The source list (preserved without changes)
	*/
	protected Vector sourceItems = null;
	/**
	* Array of indexes of the source items after reordering
	*/
	protected IntArray itemIdxs = null;
	/**
	* The listbox in which the items are displayed
	*/
	protected List list = null;
	/**
	* Contains the buttons constructed (to be able to disable, when necessary)
	*/
	protected Vector buttons = null;

	/**
	* Constructs a UI for reordering the given list of items. It is assumed that
	* items may not be removed from the list.
	*/
	public ListOrderer(Vector items) {
		this(items, false);
	}

	/**
	* Constructs a UI for reordering the given list of items. The argument
	* mayBeRemoved indicates whether the user should be allowed to remove
	* any of the items.
	*/
	public ListOrderer(Vector items, boolean mayBeRemoved) {
		if (items == null || items.size() < 2)
			return;
		sourceItems = items;
		this.items = (Vector) items.clone();
		itemIdxs = new IntArray(items.size(), 1);
		for (int i = 0; i < items.size(); i++) {
			itemIdxs.addElement(i);
		}
		int nLines = items.size() + 2;
		if (nLines < 5) {
			nLines = 5;
		} else if (nLines > 10) {
			nLines = 10;
		}
		list = new List(nLines, false);
		buttons = new Vector(3, 1);
		for (int i = 0; i < items.size(); i++)
			if (items.elementAt(i) == null) {
				list.add("<null>");
			} else if (items.elementAt(i) instanceof Named) {
				list.add(((Named) items.elementAt(i)).getName());
			} else {
				list.add(items.elementAt(i).toString());
			}
		setLayout(new BorderLayout());
		add(list, BorderLayout.CENTER);
		Panel pp = new Panel(new FlowLayout(FlowLayout.CENTER, 3, 0));
		TriangleDrawer td = new TriangleDrawer(TriangleDrawer.N);
		td.setPreferredSize(14, 14);
		td.setMargins(2, 1);
		TImgButton ib = new TImgButton(td);
		pp.add(ib);
		buttons.addElement(ib);
		ib.setActionCommand("up");
		ib.addActionListener(this);
		td = new TriangleDrawer(TriangleDrawer.S);
		td.setPreferredSize(14, 14);
		td.setMargins(2, 1);
		ib = new TImgButton(td);
		pp.add(ib);
		buttons.addElement(ib);
		ib.setActionCommand("down");
		ib.addActionListener(this);
		if (!mayBeRemoved) {
			add(pp, BorderLayout.SOUTH);
		} else {
			Panel p1 = new Panel(new BorderLayout());
			p1.add(pp, BorderLayout.CENTER);
			add(p1, BorderLayout.SOUTH);
			pp = new Panel(new FlowLayout(FlowLayout.CENTER, 3, 0));
			CrossDrawer cd = new CrossDrawer();
			cd.setPreferredSize(14, 14);
			cd.setMargins(2, 1);
			ib = new TImgButton(cd);
			pp.add(ib);
			buttons.addElement(ib);
			p1.add(pp, BorderLayout.EAST);
			ib.setActionCommand("remove");
			ib.addActionListener(this);
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		if (cmd == null)
			return;
		if (cmd.equals("up")) {
			itemUp();
		} else if (cmd.equals("down")) {
			itemDown();
		} else if (cmd.equals("remove")) {
			removeItem();
		}
	}

	protected void itemUp() {
		if (list.getItemCount() <= 1)
			return;
		int n = list.getSelectedIndex();
		if (n < 1)
			return;
		Object it = items.elementAt(n - 1);
		items.setElementAt(items.elementAt(n), n - 1);
		items.setElementAt(it, n);
		int k = itemIdxs.elementAt(n - 1);
		itemIdxs.setElementAt(itemIdxs.elementAt(n), n - 1);
		itemIdxs.setElementAt(k, n);
		String str = list.getItem(n - 1), str1 = list.getItem(n);
		//list.replaceItem(str1,n-1);   // These 2 lines produce a strange
		//list.replaceItem(str,n);      // error in JDK 1.2 Borland
		list.setVisible(false);
		list.remove(n);
		list.remove(n - 1);
		list.add(str1, n - 1);
		list.add(str, n);
		list.select(n - 1);
		list.setVisible(true);
	}

	protected void itemDown() {
		if (list.getItemCount() <= 1)
			return;
		int n = list.getSelectedIndex();
		if (n < 0 || n > list.getItemCount() - 2)
			return;
		Object it = items.elementAt(n + 1);
		items.setElementAt(items.elementAt(n), n + 1);
		items.setElementAt(it, n);
		int k = itemIdxs.elementAt(n + 1);
		itemIdxs.setElementAt(itemIdxs.elementAt(n), n + 1);
		itemIdxs.setElementAt(k, n);
		String str = list.getItem(n + 1), str1 = list.getItem(n);
		//list.replaceItem(str1,n+1);   // These 2 lines produce a strange
		//list.replaceItem(str,n);      // error in JDK 1.2 Borland
		list.setVisible(false);
		list.remove(n + 1);
		list.remove(n);
		list.add(str, n);
		list.add(str1, n + 1);
		list.select(n + 1);
		list.setVisible(true);
	}

	protected void removeItem() {
		if (list.getItemCount() <= 1)
			return;
		int n = list.getSelectedIndex();
		if (n < 0)
			return;
		items.removeElementAt(n);
		itemIdxs.removeElementAt(n);
		list.setVisible(false);
		list.remove(n);
		list.setVisible(true);
		if (items.size() < 2) {
			for (int i = 0; i < buttons.size(); i++) {
				((Component) buttons.elementAt(i)).setEnabled(false);
			}
		}
	}

	/**
	* Returns the ordered list of items
	*/
	public Vector getOrderedItems() {
		return items;
	}

	/**
	* Returns an array of indexes of the source items after reordering
	*/
	public IntArray getItemOrder() {
		return itemIdxs;
	}
}