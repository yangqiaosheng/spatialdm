package ui;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.system.SystemUI;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.PopupManager;
import spade.lib.help.Helper;
import spade.lib.lang.Language;
import spade.vis.database.AttributeDataPortion;
import spade.vis.dataview.ShowRecManager;

public class ShowRecTuner extends Panel implements ActionListener, ItemListener {
	static ResourceBundle res = Language.getTextResource("ui.Res");

	protected Checkbox cbShowSR = null, cbTooltipEnable = null;
	protected Checkbox cbDisplayAll = null, cbListChoice = null;
	protected Checkbox cbAddParents = null, cbAddDerived = null;
	protected TextField tfMaxRows = null;
	protected Button[] b = null;
	// following string: "Select all"," Clear all","Currently displayed"
	protected String[] buttonText = { res.getString("Select_all"), res.getString("Clear_all"), res.getString("Currently_displayed") };
	protected String[] buttonImgURL = { "icons/History24.gif", "icons/Delete24.gif", "icons/Host24.gif" };
	// following string: "Select"," Clear","Current"
	protected String[] buttonCmd = { "Select", " Clear", "Current" };
	// following string: { "Click here to get help",
	/*"You can enable popup displays for interactive access to data records",
	* "You can use separate window as additional display for data record showing purposes",
	* "When this is switched ON you can see the latest data output in Show Record window",
	* "You can set limit of lines used to show data records in popup and Show Record window",
	* "Select all attributes and add them to Show Record window",
	* "Deselect all previously selected attributes and remove them from Show Record window",
	* "Set attributes showed on all data displays as current selection",
	* "When switched ON, new attributes are added automatically to the Show Record window",
	* "When switched ON, parental attributes are added automatically to the Show Record window"
	};*/
	protected String[] comments = { res.getString("Click_here_to_get"), res.getString("You_can_enable_popup"), res.getString("You_can_use_separate"), res.getString("When_this_is_switched"), res.getString("You_can_set_limit_of"),
			res.getString("Select_all_attributes"), res.getString("Deselect_all"), res.getString("Set_attributes_showed"), res.getString("When_switched_ON_new"), res.getString("When_switched_ON") };

	protected List listAttr = null, popupAddAttrList = null;

	protected ShowRecManager recMan = null;
	protected SystemUI ui = null;

	public ShowRecTuner(ShowRecManager rm, SystemUI ui) {
		super();
		recMan = rm;
		this.ui = ui;
		Panel p = this;
		p.setLayout(new BorderLayout());
		Panel pHeader = new Panel();
		Panel p1 = new Panel();
		pHeader.setLayout(new BorderLayout());
		p1.setLayout(new ColumnLayout());
		Button bHelp = new Button("?");
		bHelp.setActionCommand("help_lookup_values");
		bHelp.addActionListener(this);
		new PopupManager(bHelp, comments[0], true);
		// following string: "Object view parameters"
		pHeader.add(new Label(res.getString("Object_view")), "Center");
		pHeader.add(bHelp, "East");
		p1.add(pHeader);
		p1.add(new Line(false));
		p.add(p1, "North"); // Header setup finished

		Panel panelMain = new Panel();
		panelMain.setLayout(new BorderLayout());
		p1 = new Panel();
		p1.setLayout(new GridLayout(3, 1, 0, 15));
		b = new Button[3];
		for (int i = 0; i < 3; i++) {
			b[i] = new Button(buttonText[i]);
			b[i].setActionCommand(buttonCmd[i]);
			b[i].addActionListener(this);
			new PopupManager(b[i], comments[i + 5], true);
			p1.add(b[i]);
		}
		panelMain.add(p1, "East");
		p1 = new Panel();
		p1.setLayout(new ColumnLayout());
		// following string: "Enable record display in popup window"
		cbTooltipEnable = new Checkbox(res.getString("Enable_record_display"), recMan.getEnabled());
		// following string: "Use persistent record view"
		cbShowSR = new Checkbox(res.getString("Use_persistent_record"), recMan.hasPersistentRecordView());
		new PopupManager(cbTooltipEnable, comments[1], true);
		new PopupManager(cbShowSR, comments[2], true);
		cbTooltipEnable.addItemListener(this);
		cbShowSR.addItemListener(this);
		Panel pp = new Panel(new BorderLayout());
		pp.add(cbTooltipEnable, "West");
		Panel p3 = new Panel();
		p3.setLayout(new FlowLayout(FlowLayout.RIGHT));
		// following string: "Show maximum"
		Label lShowMaxLines = new Label(res.getString("Show_maximum"), Label.RIGHT);
		p3.add(lShowMaxLines);
		tfMaxRows = new TextField("30", 2);
		tfMaxRows.addActionListener(this);
		p3.add(tfMaxRows);
		// following string: "lines"
		p3.add(new Label(res.getString("lines"), Label.LEFT));
		pp.add(p3, "East");
		p1.add(pp);
		p1.add(new Label("Include additional attributes:"));
		popupAddAttrList = new List(7, true);
		AttributeDataPortion dTable = recMan.getDataTable();
		int nf = dTable.getAttrCount();
		for (int i = 0; i < nf; i++) {
			popupAddAttrList.add(dTable.getAttributeName(i));
		}
		Vector attr = recMan.getPopupAddAttrs();
		if (attr != null) {
			for (int i = 0; i < attr.size(); i++) {
				String aid = (String) attr.elementAt(i);
				int aidx = dTable.getAttrIndex(aid);
				if (aidx >= 0) {
					popupAddAttrList.select(aidx);
				}
			}
		}
		p1.add(popupAddAttrList);
		p1.add(new Line(false));
		pp = new Panel();
		pp.setLayout(new BorderLayout());
		pp.add(cbShowSR, "West");
		p1.add(pp);
		// following string: "What to show in the persistent view: "
		p1.add(new Label(res.getString("What_to_show_in_the")));
		CheckboxGroup cbgDC = new CheckboxGroup();
		int mode = recMan.getShowAttrMode();
		// following string: "Attributes on all displays"
		cbDisplayAll = new Checkbox(res.getString("Attributes_on_all"), mode == ShowRecManager.showDisplayedAttr, cbgDC);
		// following string: "Selection from list"
		cbListChoice = new Checkbox(res.getString("Selection_from_list"), mode == ShowRecManager.showSelectedAttr, cbgDC);

		// following string: "new attributes added to displays"
		cbAddDerived = new Checkbox(res.getString("new_attributes_added"), recMan.addNew, null);
		// following string: "parents of derived attributes"
		cbAddParents = new Checkbox(res.getString("parents_of_derived"), recMan.addParents, null);
		new PopupManager(cbAddDerived, comments[8], true);
		new PopupManager(cbAddParents, comments[9], true);
		p1.add(cbDisplayAll);
		p1.add(cbListChoice);

		cbDisplayAll.addItemListener(this);
		cbListChoice.addItemListener(this);
		cbAddDerived.addItemListener(this);
		cbAddParents.addItemListener(this);

		listAttr = new List(2, true); // default list with 2 empty elements
		listAttr.addItemListener(this);
		for (int i = 0; i < nf; i++) {
			listAttr.add(dTable.getAttributeName(i));
		}
		Vector selected = recMan.getPersistentlyShownAttrs();
		if (selected != null) {
			for (int i = 0; i < selected.size(); i++) {
				listAttr.select(dTable.getAttrIndex((String) selected.elementAt(i)));
			}
		}
		panelMain.add(p1, "North");
		p1 = new Panel();
		p1.setLayout(new ColumnLayout());
		// following string: "Automatically add:"
		p1.add(new Label(res.getString("Automatically_add_"), Label.LEFT));
		p1.add(cbAddDerived);
		p1.add(cbAddParents);
		panelMain.add(listAttr, "Center");
		panelMain.add(p1, "South");
		p.add(panelMain, "Center");
		setControlsState();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() instanceof Button) {
			String cmd = e.getActionCommand();
			if (cmd.startsWith("help_")) {
				Helper.help(cmd.substring(5));
				return;
			}
			boolean ugu = true;
			int i = -1;
			while (ugu && ++i < 3)
				if (cmd.equals(buttonCmd[i])) {
					ugu = !ugu;
				}
			switch (i) {
			case 0: {
				selListItems(true);
				break;
			}
			case 1: {
				selListItems(false);
				break;
			}
			case 2: {
				selListDisplayed();
				break;
			}
			}
		} else if (e.getSource() instanceof TextField) {
			TextField tfCurrent = (TextField) e.getSource();
			String val = tfCurrent.getText();
			int mrows = 100;
			try {
				mrows = Integer.parseInt(val);
			} catch (NumberFormatException nfe) {
				mrows = 100;
			}
			if (mrows <= 0 || mrows > 80) {
				mrows = recMan.getLinesLimit();
				tfCurrent.setText(Integer.toString(mrows));
			} else if (recMan != null) {
				recMan.setLinesLimit(mrows);
			}
		}
	}

	private void selListItems(boolean all) {
		if (listAttr == null)
			return;
		if (all) {
			for (int i = 0; i < listAttr.getItemCount(); i++) {
				listAttr.select(i);
			}
		} else {
			for (int i = 0; i < listAttr.getItemCount(); i++) {
				listAttr.deselect(i);
			}
		}
		setListSelection();
	}

	/* This function has been called when user had pressed
	* the button "Currently displayed"
	*/
	private void selListDisplayed() {
		if (listAttr == null)
			return;
		AttributeDataPortion dTable = recMan.getDataTable();
		int idxInList = -1;
		String attrId = null, attrName = null;
		for (int i = 0; i < listAttr.getItemCount(); i++) {
			listAttr.deselect(i);
		}
		Vector displayedAttr = recMan.getSupervisor().getAllPresentedAttributes();
		if (displayedAttr != null) {
			for (int i = 0; i < displayedAttr.size(); i++) {
				if (displayedAttr.elementAt(i) != null) {
					attrId = displayedAttr.elementAt(i).toString();
					attrName = dTable.getAttributeName(dTable.getAttrIndex(attrId));
				}
				if (attrName != null) {
					for (int j = 0; j < listAttr.getItemCount(); j++)
						if (listAttr.getItem(j).equalsIgnoreCase(attrName)) {
							listAttr.select(j);
						}
				}
			}
		}
		setListSelection();
	}

	private void setControlsState() {
		boolean persViewExists = recMan.hasPersistentRecordView(), showSelected = recMan.mode == ShowRecManager.showSelectedAttr;
		for (int i = 0; i < 3; i++) {
			b[i].setEnabled(persViewExists && showSelected);
		}
		cbDisplayAll.setEnabled(persViewExists);
		cbListChoice.setEnabled(persViewExists);
		cbAddDerived.setEnabled(persViewExists && showSelected);
		cbAddParents.setEnabled(persViewExists && showSelected);
		listAttr.setEnabled(persViewExists && showSelected);
		tfMaxRows.setEnabled(cbTooltipEnable.getState());
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		Object src = e.getSource();
		if (src instanceof Checkbox) {
			Checkbox cbSource = (Checkbox) src;
			if (cbSource == cbTooltipEnable && recMan != null) {
				recMan.setEnabled(cbTooltipEnable.getState());
				tfMaxRows.setEnabled(cbTooltipEnable.getState());
				return;
			}
			if (cbSource == cbShowSR) {
				boolean showSR = cbSource.getState();
				if (ui != null)
					if (showSR) {
						ui.placeComponent(recMan.getPersistentRecordShow());
					} else {
						ui.removeComponent(recMan.destroyPersistentRecordShow());
					}
				setControlsState();
				return;
			}
			if (cbSource == cbAddDerived) {
				setAddDerived(cbAddDerived.getState());
				return;
			}
			if (cbSource == cbAddParents) {
				setAddParental(cbAddParents.getState());
				return;
			}
			if (cbSource == cbDisplayAll) {
				recMan.setShowAttrMode(ShowRecManager.showDisplayedAttr);
				setControlsState();
			} else if (cbSource == cbListChoice) {
				recMan.setShowAttrMode(ShowRecManager.showSelectedAttr);
				setControlsState();
				setListSelection();
			}
		} else if (src instanceof List) {
			listSelectionChanged(((Integer) e.getItem()).intValue(), (e.getStateChange() == ItemEvent.SELECTED));
		}
	}

	protected void setAddParental(boolean flag) {
		recMan.addParents = flag;
	}

	protected void setAddDerived(boolean flag) {
		recMan.addNew = flag;
	}

	protected void setListSelection() {
		int selected[] = listAttr.getSelectedIndexes();
		Vector shownAttr = new Vector(2, 2);
		AttributeDataPortion dTable = recMan.getDataTable();
		if (selected != null) {
			for (int element : selected) {
				shownAttr.addElement(dTable.getAttributeId(element));
			}
		}
		recMan.setPersistentlyShownAttrs(shownAttr);
	}

	protected void listSelectionChanged(int n, boolean isSelected) {
		if (n < 0 || n > listAttr.getItemCount() - 1)
			return;
		AttributeDataPortion dTable = recMan.getDataTable();
		if (isSelected) {
			recMan.addAttrToShow(dTable.getAttributeId(n), true);
		} else {
			recMan.removeShownAttr(dTable.getAttributeId(n), true);
		}
	}

	public void setPopupAddAttrs() {
		if (popupAddAttrList == null)
			return;
		int idxs[] = popupAddAttrList.getSelectedIndexes();
		if (idxs == null || idxs.length < 1) {
			recMan.setPopupAddAttrs(null);
			return;
		}
		Vector attr = new Vector(idxs.length, 1);
		AttributeDataPortion dTable = recMan.getDataTable();
		for (int idx : idxs) {
			attr.addElement(dTable.getAttributeId(idx));
		}
		recMan.setPopupAddAttrs(attr);
	}
}
