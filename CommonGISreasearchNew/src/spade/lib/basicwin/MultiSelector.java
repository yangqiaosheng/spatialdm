package spade.lib.basicwin;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.List;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.lib.lang.Language;
import spade.lib.util.IntArray;

/**
* Allows the user to select multiple items from a list. The items are moved to
* another list. This is more convenient than simply selection in a standard
* multi-choice list: when the list is long, one cannot see all the items that
* have been selected.
*/
public class MultiSelector extends Panel implements ActionListener {
	static ResourceBundle res = Language.getTextResource("spade.lib.basicwin.Res");
	/**
	* The initial set of items (strings) to select.
	*/
	protected Vector items = null;
	/**
	* The listboxes used for selection
	*/
	protected List leftList = null, rightList = null;
	/**
	* The numbers of the items currently included in the left and in the right
	* listbox, respectively.
	*/
	protected IntArray leftNs = null, rightNs = null;
	/*
	* The MultiSelectHelper panel is created on demand, if there are more than
	* nValuesThreshold values of attribute or parameter.
	* It should be helpful for multiple selection
	* of parameters with certain periodicity, for instance,
	* it is relevant to time moments selection:
	* select each N time moment from range T1-T2
	*/
	protected MultiSelectHelper pSelectHelper = null;
	protected int nValuesThreshold = 10;
	protected boolean isUsedForParameters = false;
	/**
	 * May have listeners of changes of the selection
	 */
	protected Vector listeners = null;

	/**
	* Constructs a MultiSelector with the given set of items (strings) to select.
	* The second argument specifies whether the order of the selected items is
	* important. In this case buttons for reordering are provided.
	*/
	public MultiSelector(Vector items, boolean orderIsImportant) {
		this(items, orderIsImportant, false);
	}

	public MultiSelector(Vector items, boolean orderIsImportant, boolean isUsedForParameters) {
		if (items == null || items.size() < 1)
			return;
		this.items = items;
		this.isUsedForParameters = isUsedForParameters;
		leftNs = new IntArray(items.size(), 1);
		rightNs = new IntArray(items.size(), 1);
		int nLines = items.size() + 1;
		if (nLines < 5) {
			nLines = 5;
		} else if (nLines > 10) {
			nLines = 10;
		}

		leftList = new List(nLines, true);
		rightList = new List(nLines, true);
		leftList.addActionListener(this);
		rightList.addActionListener(this);
		for (int i = 0; i < items.size(); i++) {
			leftList.add(items.elementAt(i).toString());
			leftNs.addElement(i);
		}
		Panel p = new Panel(new BorderLayout(0, 3));
		p.add(leftList, "Center");
		Panel pfl = new Panel(new FlowLayout(FlowLayout.CENTER, 0, 0));
		Button b = new Button(res.getString("Select_all"));
		b.setActionCommand("select_all");
		b.addActionListener(this);
		pfl.add(b);
		p.add(pfl, "South");

		Panel pp = new Panel(new ColumnLayout());
		TriangleDrawer td = new TriangleDrawer(TriangleDrawer.E);
		td.setPreferredSize(15, 15);
		td.setMargins(2, 1);
		TImgButton ib = new TImgButton(td);
		ib.setActionCommand("select");
		ib.addActionListener(this);
		pp.add(ib);
		td = new TriangleDrawer(TriangleDrawer.W);
		td.setPreferredSize(15, 15);
		td.setMargins(2, 1);
		ib = new TImgButton(td);
		ib.setActionCommand("deselect");
		ib.addActionListener(this);
		pp.add(ib);
		p.add(pp, "East");

		Panel pMain = new Panel(new GridLayout(1, 2));
		setLayout(new BorderLayout(0, 3));
		pMain.add(p);

		p = new Panel(new BorderLayout(0, 3));
		p.add(rightList, "Center");
		pp = new Panel(new FlowLayout(FlowLayout.CENTER, 3, 0));
		if (orderIsImportant) {
			td = new TriangleDrawer(TriangleDrawer.N);
			td.setPreferredSize(14, 14);
			td.setMargins(2, 1);
			ib = new TImgButton(td);
			pp.add(ib);
			ib.setActionCommand("attrUp");
			ib.addActionListener(this);
			td = new TriangleDrawer(TriangleDrawer.S);
			td.setPreferredSize(14, 14);
			td.setMargins(2, 1);
			ib = new TImgButton(td);
			pp.add(ib);
			ib.setActionCommand("attrDown");
			ib.addActionListener(this);
		}
		b = new Button(res.getString("Clear_list"));
		pp.add(b);
		b.setActionCommand("clearList");
		b.addActionListener(this);
		p.add(pp, BorderLayout.SOUTH);
		pMain.add(p);
		add(pMain, BorderLayout.CENTER);

		Panel pp2 = null;
		initAdvancedSelect(items);
		// if advanced selection panel was created, insert it to the panel
		if (pSelectHelper != null) {
			pp2 = new Panel(new BorderLayout(0, 3));
			pp2.add(pSelectHelper, BorderLayout.CENTER);
			//pp2.add(pp,BorderLayout.SOUTH);
		}
		if (pp2 != null) {
			add(pp2, BorderLayout.SOUTH);
		}
	}

	/**
	* Replaces the current list of items to select with the given new list.
	*/
	public void replaceItemList(Vector newItems) {
		setVisible(false);
		leftList.removeAll();
		rightList.removeAll();
		leftNs.removeAllElements();
		rightNs.removeAllElements();
		items = newItems;
		for (int i = 0; i < items.size(); i++) {
			leftList.add(items.elementAt(i).toString());
			leftNs.addElement(i);
		}
		if (pSelectHelper != null) {
			pSelectHelper.setup(items);
		}
		// System.out.println("...updated");
		setVisible(true);
	}

	/*
	*  Creates advanced selector if needed or possible
	*/
	protected void initAdvancedSelect(Vector items) {
		if (pSelectHelper == null) {
			//System.out.println("Init: advanced selection panel");
			if (!isUsedForParameters || items == null || items.size() < nValuesThreshold)
				//System.out.println("... NOT created");
				return;
			pSelectHelper = new MultiSelectHelper(items, this);
		}
	}

	/**
	* Selects the items with the specified indexes.
	*/
	public void selectItems(int idxs[]) {
		if (idxs == null)
			return;
		boolean changed = false;
		for (int i = 0; i < idxs.length; i++) {
			int idx = idxs[i];
			if (idx < 0 || idx >= leftNs.size()) {
				continue;
			}
			leftList.deselect(idx);
			int n = leftNs.elementAt(idx);
			if (rightNs.indexOf(n) < 0) {
				rightNs.addElement(n);
				rightList.add(leftList.getItem(idx));
			}
			leftNs.removeElementAt(idx);
			leftList.remove(idx);
			for (int j = i + 1; j < idxs.length; j++)
				if (idxs[j] > idx) {
					idxs[j]--;
				}
			changed = true;
		}
		if (changed) {
			notifySelectionChange();
		}
	}

	/**
	 * Highlights the items with the specified indexes without moving them
	 * from one list to another
	 */
	public void highlightItems(int idxs[]) {
		int selIdxs[] = leftList.getSelectedIndexes();
		if (selIdxs != null && selIdxs.length > 0) {
			for (int selIdx : selIdxs) {
				leftList.deselect(selIdx);
			}
		}
		selIdxs = rightList.getSelectedIndexes();
		if (selIdxs != null && selIdxs.length > 0) {
			for (int selIdx : selIdxs) {
				rightList.deselect(selIdx);
			}
		}
		if (idxs == null || idxs.length < 1)
			return;
		for (int idx : idxs) {
			int lidx = leftNs.indexOf(idx);
			if (lidx >= 0) {
				leftList.select(lidx);
			} else {
				lidx = rightNs.indexOf(idx);
				if (lidx >= 0) {
					rightList.select(lidx);
				}
			}
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() instanceof List) {
			if (e.getSource() == leftList) {
				select();
			} else if (e.getSource() == rightList) {
				deselect();
			}
		} else {
			String cmd = e.getActionCommand();
			if (cmd == null)
				return;
			if (cmd.equals("attrUp")) {
				itemUp();
			} else if (cmd.equals("attrDown")) {
				itemDown();
			} else if (cmd.equals("select")) {
				select();
			} else if (cmd.equals("select_now")) {
				selectFromTo();
			} else if (cmd.equals("deselect")) {
				deselect();
			} else if (cmd.equals("clearList")) {
				clearList();
			} else if (cmd.equals("select_all")) {
				selectAll();
			}
		}
	}

	protected void selectFromTo() {
		int eachN = 1, fromN = 0, toN = leftList.getItemCount() - 1;
		int[] current = getSelectedIndexes();
		clearList();
		try {
			eachN = Integer.valueOf(pSelectHelper.getSelectEach()).intValue();
		} catch (NumberFormatException ex) {
			eachN = 1;
		}
		fromN = pSelectHelper.getSelectFromIdx();
		toN = pSelectHelper.getSelectToIdx();
		// Skip all the time moments that are relative to the current (if any)
		int beginIdx = 0;
		for (int i = 0; i < leftList.getItemCount(); i++) {
			String sItemCurr = leftList.getItem(i);
			sItemCurr.trim();
			if ((sItemCurr.indexOf("t") == 0 && sItemCurr.length() == 1) || (sItemCurr.indexOf("t") == 0 && sItemCurr.indexOf("+") > 0)) {
				continue;
			}
			beginIdx = i;
			break;
		}
		fromN += beginIdx;
		toN += beginIdx;
		for (int i = fromN; i <= toN; i += eachN) {
			leftList.select(i);
		}
		if (current != null && current.length > 0) {
			for (int element : current) {
				leftList.select(element);
			}
		}
		select();
	}

	protected void select() {
		selectItems(leftList.getSelectedIndexes());
	}

	protected void deselect() {
		if (rightNs == null || rightNs.size() < 1)
			return;
		int idxs[] = rightList.getSelectedIndexes();
		if (idxs == null)
			return;
		for (int i = 0; i < idxs.length; i++) {
			int idx = idxs[i];
			if (idx < 0 || idx >= rightNs.size()) {
				continue;
			}
			rightList.deselect(idx);
			int itemN = rightNs.elementAt(idx);
			boolean inserted = false;
			for (int n = 0; n < leftNs.size() && !inserted; n++)
				if (leftNs.elementAt(n) > itemN) {
					leftNs.insertElementAt(itemN, n);
					leftList.add(rightList.getItem(idx), n);
					inserted = true;
				}
			if (!inserted) {
				leftNs.addElement(itemN);
				leftList.add(rightList.getItem(idx));
			}
			rightNs.removeElementAt(idx);
			rightList.remove(idx);
			for (int j = i + 1; j < idxs.length; j++)
				if (idxs[j] > idx) {
					idxs[j]--;
				}
		}
		notifySelectionChange();
	}

	protected void selectAll() {
		if (leftNs.size() < 1)
			return;
		rightList.setVisible(false);
		leftList.setVisible(false);
		for (int i = 0; i < leftNs.size(); i++) {
			rightNs.addElement(leftNs.elementAt(i));
			rightList.add(leftList.getItem(i));
		}
		leftNs.removeAllElements();
		leftList.removeAll();
		rightList.setVisible(true);
		leftList.setVisible(true);
		notifySelectionChange();
	}

	protected void clearList() {
		if (rightNs.size() < 1)
			return;
		rightList.setVisible(false);
		leftList.setVisible(false);
		for (int j = 0; j < rightNs.size(); j++) {
			int itemN = rightNs.elementAt(j);
			boolean inserted = false;
			for (int i = 0; i < leftNs.size() && !inserted; i++)
				if (leftNs.elementAt(i) > itemN) {
					leftNs.insertElementAt(itemN, i);
					leftList.add(rightList.getItem(j), i);
					inserted = true;
				}
			if (!inserted) {
				leftNs.addElement(itemN);
				leftList.add(rightList.getItem(j));
			}
		}
		rightList.removeAll();
		rightNs.removeAllElements();
		rightList.setVisible(true);
		leftList.setVisible(true);
		notifySelectionChange();
	}

	protected void itemUp() {
		if (rightList.getItemCount() <= 1)
			return;
		int n = rightList.getSelectedIndex();
		if (n < 1)
			return;
		int k = rightNs.elementAt(n - 1);
		rightNs.setElementAt(rightNs.elementAt(n), n - 1);
		rightNs.setElementAt(k, n);
		String str = rightList.getItem(n - 1), str1 = rightList.getItem(n);
		//rightList.replaceItem(str1,n-1);   // These 2 lines produce a strange
		//rightList.replaceItem(str,n);      // error in JDK 1.2 Borland
		rightList.remove(n);
		rightList.remove(n - 1);
		rightList.add(str1, n - 1);
		rightList.add(str, n);
		rightList.select(n - 1);
	}

	protected void itemDown() {
		if (rightList.getItemCount() <= 1)
			return;
		int n = rightList.getSelectedIndex();
		if (n < 0 || n > rightList.getItemCount() - 2)
			return;
		int k = rightNs.elementAt(n + 1);
		rightNs.setElementAt(rightNs.elementAt(n), n + 1);
		rightNs.setElementAt(k, n);
		String str = rightList.getItem(n + 1), str1 = rightList.getItem(n);
		//rightList.replaceItem(str1,n+1);   // These 2 lines produce a strange
		//rightList.replaceItem(str,n);      // error in JDK 1.2 Borland
		rightList.remove(n + 1);
		rightList.remove(n);
		rightList.add(str, n);
		rightList.add(str1, n + 1);
		rightList.select(n + 1);
	}

	public String[] getSelectedItems() {
		if (rightNs.size() < 1)
			return null;
		String result[] = new String[rightNs.size()];
		for (int i = 0; i < rightNs.size(); i++) {
			result[i] = items.elementAt(rightNs.elementAt(i)).toString();
		}
		return result;
	}

	public int[] getSelectedIndexes() {
		if (rightNs.size() < 1)
			return null;
		int result[] = new int[rightNs.size()];
		for (int i = 0; i < rightNs.size(); i++) {
			result[i] = rightNs.elementAt(i);
		}
		return result;
	}

	public boolean getUsedForParameters() {
		return isUsedForParameters;
	}

	/*
	*  Sets the threshold number of different values of parameter after
	*  advanced selector panel will be activated
	*/
	public void setAdvancedSelectorThreshold(int nParameters) {
		nValuesThreshold = nParameters;
	}

	/**
	 * Registers a listener of changes of the selection. In such cases, the
	 * MultiSelector sends a PropertyChangeEvent with the property name "selection"
	 * and the new value being an array of currently selected item indexes.
	 */
	public void addSelectionChangeListener(PropertyChangeListener listener) {
		if (listener == null)
			return;
		if (listeners == null) {
			listeners = new Vector(5, 5);
		}
		if (listeners.contains(listener))
			return;
		listeners.addElement(listener);
	}

	/**
	 * Removes the listener of changes of the selection.
	 */
	public void removeSelectionChangeListener(PropertyChangeListener listener) {
		if (listener == null || listeners == null)
			return;
		int idx = listeners.indexOf(listener);
		if (idx >= 0) {
			listeners.removeElementAt(idx);
		}
	}

	/**
	 * Notifies the listeners, if any, about a change of the selection
	 */
	protected void notifySelectionChange() {
		if (listeners != null && listeners.size() > 0) {
			PropertyChangeEvent pce = new PropertyChangeEvent(this, "selection", null, getSelectedIndexes());
			for (int i = 0; i < listeners.size(); i++) {
				((PropertyChangeListener) listeners.elementAt(i)).propertyChange(pce);
			}
		}
	}

}