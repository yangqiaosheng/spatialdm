package spade.lib.basicwin;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Choice;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.lib.lang.Language;

/**
* Allows the user to select multiple items from a list. The items are moved to
* another list. This is more convenient than simply selection in a standard
* multi-choice list: when the list is long, one cannot see all the items that
* have been selected.
*/
public class MultiSelectHelper extends Panel implements ItemListener {
	static ResourceBundle res = Language.getTextResource("spade.lib.basicwin.Res");
	/**
	* The initial set of items (strings) to select.
	*/
	protected Vector items = null;
	/**
	* The choices used for advanced selection
	*/
	protected Choice chSelectEach = null, chSelectFrom = null, chSelectTo = null;
	protected int nValuesThreshold = 10;
	protected String sSelectEach = null, sSelectFrom = null, sSelectTo = null;

	/**
	* Constructs a MultiSelectHelper with the given set of items (strings) to select.
	* The second argument specifies whether the order of the selected items is
	* important. In this case buttons for reordering are provided.
	*/
	public MultiSelectHelper(Vector items, ActionListener alOwner) {
		if (alOwner == null)
			return;
		setLayout(new BorderLayout());
		Panel pSelectEvery = new Panel();
		Panel pSelectFromTo = new Panel();
		pSelectEvery.setLayout(new GridLayout(1, 2));
		pSelectFromTo.setLayout(new GridLayout(1, 4));
		chSelectEach = new Choice();
		chSelectFrom = new Choice();
		chSelectTo = new Choice();
		chSelectEach.addItemListener(this);
		chSelectFrom.addItemListener(this);
		chSelectTo.addItemListener(this);
		Button bAcceptRange = new Button(res.getString("go"));
		bAcceptRange.setActionCommand("select_now");
		bAcceptRange.addActionListener(alOwner);
		pSelectEvery.add(new Label(res.getString("S_every") + " ", Label.RIGHT));
		pSelectEvery.add(chSelectEach);
		pSelectFromTo.add(new Label(res.getString("S_from") + " ", Label.RIGHT));
		pSelectFromTo.add(chSelectFrom);
		pSelectFromTo.add(new Label(res.getString("S_to") + " ", Label.RIGHT));
		pSelectFromTo.add(chSelectTo);
		Panel pSelect = new Panel();
		pSelect.setLayout(new GridLayout(2, 1));

		pSelect.add(pSelectEvery);
		pSelect.add(pSelectFromTo);
		add(pSelect, BorderLayout.CENTER);
		add(bAcceptRange, BorderLayout.EAST);
		//System.out.println("...has been created");
		setup(items);
	}

	/*
	*  Creates and initialises interface widgets of advanced selector
	*/
	public void setup(Vector items) {

		if (chSelectEach.getItemCount() > 0) {
			chSelectEach.removeAll();
		}
		if (chSelectFrom.getItemCount() > 0) {
			chSelectFrom.removeAll();
		}
		if (chSelectTo.getItemCount() > 0) {
			chSelectTo.removeAll();
		}
		int currN = 1;
		for (int j = 0; j < items.size(); j++) {
			String sItemCurrent = null;
			Object objItem = items.elementAt(j);
			if (objItem != null) {
				sItemCurrent = objItem.toString();
			}
			if (sItemCurrent != null) {
				sItemCurrent.trim();
				if ((sItemCurrent.indexOf("t") == 0 && sItemCurrent.length() == 1) || (sItemCurrent.indexOf("t") == 0 && sItemCurrent.indexOf("+") > 0)) {
					continue;
				}
				if ((++currN) % 2 != 0) {
					chSelectEach.addItem(String.valueOf(currN / 2));
				}
				chSelectFrom.addItem(sItemCurrent);
				chSelectTo.addItem(sItemCurrent);
			}
		}
		chSelectTo.select(chSelectTo.getItemCount() - 1);
		sSelectEach = chSelectEach.getSelectedItem();
		sSelectFrom = chSelectFrom.getSelectedItem();
		sSelectTo = chSelectTo.getSelectedItem();
	}

	/*
	*  Returns value of SelectEach
	*/
	public String getSelectEach() {
		return chSelectEach.getSelectedItem();
	}

	/*
	*  Returns value of SelectFrom
	*/
	public String getSelectFrom() {
		return chSelectFrom.getSelectedItem();
	}

	/*
	*  Returns value of SelectTo
	*/
	public String getSelectTo() {
		return chSelectTo.getSelectedItem();
	}

	/*
	*  Returns index of SelectEach
	*/
	public int getSelectEachIdx() {
		return chSelectEach.getSelectedIndex();
	}

	/*
	*  Returns index of SelectFrom
	*/
	public int getSelectFromIdx() {
		return chSelectFrom.getSelectedIndex();
	}

	/*
	*  Returns index of SelectTo
	*/
	public int getSelectToIdx() {
		return chSelectTo.getSelectedIndex();
	}

	/*
	*  Implementation of ItemListener interface
	*/
	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e != null && (e.getSource() instanceof Choice)) {
			Choice ch = (Choice) e.getSource();
			if (ch == chSelectEach) {
				sSelectEach = chSelectEach.getSelectedItem();
			} else if (ch == chSelectFrom) {
				sSelectFrom = chSelectFrom.getSelectedItem();
			} else if (ch == chSelectTo) {
				sSelectTo = chSelectTo.getSelectedItem();
			}
		} else {
		}
	}

	/*
	*  Sets the threshold number of different values of parameter after
	*  advanced selector panel will be activated
	*/
	public void setActivationThreshold(int nParameters) {
		nValuesThreshold = nParameters;
	}
}