package spade.lib.basicwin;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.List;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
public class TimeParameterSelector extends Panel implements ActionListener {
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
	 * The listboxes used for selection of absolute and relative time moments
	 */
	protected List leftListAbsolute = null, leftListRelative = null;
	/**
	* The numbers of the items currently included in the right and left
	* listbox, respectively.
	*/
	protected IntArray rightNs = null;
	/**
	* The numbers of the items currently included in the left lists
	* 2nd list has the following numeration:
	* N of Items in List + N in List 2
	*/

	protected IntArray leftNs = null;
	/**
	* The numbers of the items currently included in the absolute and relative
	* listbox, respectively.
	*/
// protected IntArray leftAbsoluteNs=null, leftRelativeNs=null;
	/*
	* The following panel is created on demand, if there are more than 10 different
	* values of attribute or parameter. It should be helpful for multiple selection
	* of parameters with certain periodicity, for instance,
	* it is relevant to time moments selection:
	* select each N time moment from range T1-T2
	*/
	protected MultiSelectHelper pSelectHelper = null;
	protected int nValuesThreshold = 10;

	/**
	* Constructs a TimeParameterSelector with the given set of items (TimeCount) to select.
	* The second argument specifies whether the order of the selected items is
	* important. In this case buttons for reordering are provided.
	*/
	public TimeParameterSelector(Vector items, boolean orderIsImportant) {
		int nLines = items.size() + 1;
		//leftAbsoluteNs=new IntArray(items.size(),1);
		//leftRelativeNs=new IntArray(items.size(),1);
		leftNs = new IntArray(items.size(), 1);
		rightNs = new IntArray(items.size(), 1);

		if (nLines < 5) {
			nLines = 5;
		} else if (nLines > 10) {
			nLines = 10;
		}

		leftListAbsolute = new List(nLines, true);
		leftListRelative = new List(nLines, true);
		leftListAbsolute.addActionListener(this);
		leftListRelative.addActionListener(this);
		rightList = new List(nLines, true);
		rightList.addActionListener(this);
		for (int i = 0; i < items.size(); i++) {
			if (items.elementAt(i) instanceof spade.time.TimeCount) {
				leftListAbsolute.add(items.elementAt(i).toString());
				//leftAbsoluteNs.addElement(i);
			} else {
				leftListRelative.add(items.elementAt(i).toString());
				//leftRelativeNs.addElement(i);
			}
			leftNs.addElement(i);
		}
		Panel p = new Panel(new BorderLayout(0, 3));
		SplitPanel spList = new SplitPanel(false);
		if (leftListRelative != null && leftListRelative.getItemCount() > 0) {
			spList.addSplitComponent(leftListRelative);
		}
		spList.addSplitComponent(leftListAbsolute);
		p.add(spList, "Center");
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
		leftListAbsolute.removeAll();
		leftListRelative.removeAll();
		rightList.removeAll();
		//leftAbsoluteNs.removeAllElements();
		//leftRelativeNs.removeAllElements();
		rightNs.removeAllElements();
		items = newItems;
		for (int i = 0; i < items.size(); i++) {
			if (items.elementAt(i) instanceof spade.time.TimeCount) {
				leftListAbsolute.add(items.elementAt(i).toString());
				//leftAbsoluteNs.addElement(i);
			} else {
				leftListRelative.add(items.elementAt(i).toString());
				//leftRelativeNs.addElement(i);
			}
		}
		pSelectHelper.setup(items);
		// System.out.println("...updated");
		setVisible(true);
	}

	/*
	*  Creates interface widgets of advanced selector if needed or possible
	*/
	protected void initAdvancedSelect(Vector items) {
		if (pSelectHelper == null) {
			//System.out.println("Init: advanced selection panel");
			if (items == null || items.size() < nValuesThreshold)
				//System.out.println("... NOT created");
				return;
			if (pSelectHelper != null) {
				pSelectHelper = new MultiSelectHelper(items, this);
			}
		}
	}

	/**
	* Selects the items with the specified indexes.
	*/
	public void selectItemsRelative(int idxs[]) {
		List targetList = leftListRelative;
		//targetList
		if (idxs == null)
			return;
		for (int i = 0; i < idxs.length; i++) {
			int idx = idxs[i];
			if (idx < 0 || idx >= leftNs.size()) {
				continue;
			}
			targetList.deselect(idx);
			int n = leftNs.elementAt(idx);
			if (rightNs.indexOf(n) < 0) {
				rightNs.addElement(n);
				rightList.add(targetList.getItem(idx));
			}
			leftNs.removeElementAt(idx);
			targetList.remove(idx);
			for (int j = i + 1; j < idxs.length; j++)
				if (idxs[j] > idx) {
					idxs[j]--;
				}
		}
	}

	/**
	* Selects the items with the specified indexes.
	*/
	public void selectItemsAbsolute(int idxs[]) {
		List targetList = leftListAbsolute;
		//targetList
		if (idxs == null)
			return;
		for (int i = 0; i < idxs.length; i++) {
			int idx = idxs[i];
			if (idx < 0 || idx >= leftNs.size()) {
				continue;
			}
			targetList.deselect(idx);
			int n = leftNs.elementAt(idx);
			if (rightNs.indexOf(n) < 0) {
				rightNs.addElement(n);
				rightList.add(targetList.getItem(idx));
			}
			leftNs.removeElementAt(idx);
			targetList.remove(idx);
			for (int j = i + 1; j < idxs.length; j++)
				if (idxs[j] > idx) {
					idxs[j]--;
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
			} else if (cmd.equals("select") || cmd.equals("select_now")) {
				select();
			} else if (cmd.equals("deselect")) {
				deselect();
			} else if (cmd.equals("clearList")) {
				clearList();
			} else if (cmd.equals("select_all")) {
				selectAll();
			}
		}
	}

	protected void select() {
		if (getRelListAvailable()) {
			selectItemsRelative(leftListRelative.getSelectedIndexes());
		}
		if (getAbsListAvailable()) {
			selectItemsAbsolute(leftListAbsolute.getSelectedIndexes());
		}
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
	}

	private boolean getAbsListAvailable() {
		return (leftListAbsolute != null && leftListAbsolute.getItemCount() > 0);
	}

	private boolean getRelListAvailable() {
		return (leftListRelative != null && leftListRelative.getItemCount() > 0);
	}

	protected void selectAll() {
		if (leftNs.size() < 1)
			return;
		rightList.setVisible(false);
		//leftList.setVisible(false);
		boolean leftListAbsAvailable = getAbsListAvailable();
		boolean leftListRelAvailable = getRelListAvailable();

		if (leftListAbsAvailable) {
			leftListAbsolute.setVisible(false);
		}
		if (leftListRelAvailable) {
			leftListRelative.setVisible(false);
		}
		for (int i = 0; i < leftNs.size(); i++) {
			rightNs.addElement(leftNs.elementAt(i));
			if (leftListRelAvailable && leftListAbsAvailable)
				if (i >= leftListRelative.getItemCount()) {
					rightList.add(leftListAbsolute.getItem(i - leftListRelative.getItemCount()));
				} else {
					rightList.add(leftListRelative.getItem(i));
				}
		}
		leftNs.removeAllElements();
		//leftList.removeAll();

		if (leftListAbsAvailable) {
			leftListAbsolute.removeAll();
		}
		if (leftListRelAvailable) {
			leftListRelative.removeAll();
		}
		rightList.setVisible(true);
		if (leftListAbsAvailable) {
			leftListAbsolute.setVisible(true);
		}
		if (leftListRelAvailable) {
			leftListRelative.setVisible(true);
		}
	}

	protected void clearList() {
		if (rightNs.size() < 1)
			return;
		boolean leftListAbsAvailable = getAbsListAvailable();
		boolean leftListRelAvailable = getRelListAvailable();

		List targetList = null;
		if (leftListAbsolute == null) {
			targetList = leftListRelative;
		} else if (leftListRelative == null) {
			targetList = leftListAbsolute;
		}

		rightList.setVisible(false);

		if (leftListAbsAvailable) {
			leftListAbsolute.setVisible(false);
		}
		if (leftListRelAvailable) {
			leftListRelative.setVisible(false);
		}

		for (int j = 0; j < rightNs.size(); j++) {
			int itemN = rightNs.elementAt(j);

			if (getAbsListAvailable() && getRelListAvailable())
				if (itemN > leftListRelative.getItemCount()) {
					targetList = leftListAbsolute;
				} else {
					targetList = leftListRelative;
				}

			boolean inserted = false;
			for (int i = 0; i < leftNs.size() && !inserted; i++)
				if (leftNs.elementAt(i) > itemN) {
					leftNs.insertElementAt(itemN, i);
					if (targetList != null) {
						targetList.add(rightList.getItem(j), i);
					}
					inserted = true;
				}
			if (!inserted) {
				leftNs.addElement(itemN);
				if (targetList != null) {
					targetList.add(rightList.getItem(j));
				}
			}
		}
		rightList.removeAll();
		rightNs.removeAllElements();
		rightList.setVisible(true);
		if (leftListAbsAvailable) {
			leftListAbsolute.setVisible(true);
		}
		if (leftListRelAvailable) {
			leftListRelative.setVisible(true);
		}
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

	/*
	*  Sets the threshold number of different values of parameter after
	*  advanced selector panel will be activated
	*/
	public void setAdvancedSelectorThreshold(int nParameters) {
		nValuesThreshold = nParameters;
	}
}