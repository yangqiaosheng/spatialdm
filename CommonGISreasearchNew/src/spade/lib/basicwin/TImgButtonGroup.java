//======================= TImgButtonGroup.java =======================
//
// Copyright 1998, all rights reserved.
// Professional GEO Systems BV.
//
// Changed nl.pgs.lava.javaui.ExtButtonGroup.java by
// GMD, AiS.KD, 22-09-2000, 17-10-2000
// with permission of Professional GEO Systems BV.

package spade.lib.basicwin;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

/**
* An TImgButtonGroup groups a number of TImgButtons together, to form
* a checkbox group.
*
*/
public class TImgButtonGroup extends Panel implements ActionListener {

	// $AUTO: Class fields.

	public static final int HORIZONTAL = 0;
	public static final int VERTICAL = 1;

	// $AUTO: Instance fields.

	protected Vector buttons;
	protected int selectedNr;
	protected TImgButton selectedButton;

	protected GridBagLayout gbl;
	protected GridBagConstraints gbc;
	protected Vector listeners = null; //ActionListeners

	// $AUTO: Constructors.

	//-------------------- TImgButtonGroup --------------------
	public TImgButtonGroup() {
		init(HORIZONTAL);
	}

	//-------------------- TImgButtonGroup --------------------
	public TImgButtonGroup(int orientation) {
		init(orientation);
	}

	public synchronized void addActionListener(ActionListener l) {
		if (l == null)
			return;
		if (listeners == null) {
			listeners = new Vector(5, 5);
		}
		listeners.addElement(l);
	}

	protected void sendActionEventToListeners(String cmd) {
		if (listeners != null) {
			ActionEvent ae = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, cmd);
			for (int i = 0; i < listeners.size(); i++) {
				((ActionListener) listeners.elementAt(i)).actionPerformed(ae);
			}
		}
	}

	// $AUTO: Instance methods.

	//-------------------- actionPerformed --------------------
	@Override
	public void actionPerformed(ActionEvent e) {
		// Find and select clicked button
		int index = buttons.indexOf(e.getSource());
		setSelect(index);

		// Send an action event to listeners
		sendActionEventToListeners(e.getActionCommand());
	}

	//-------------------- addButton --------------------
	public void addButton(TImgButton b) {
		buttons.addElement(b);
		b.setMode(TImgButton.MODE_SELECT);
		b.setGridCell(true);
		b.setParentGrid(this);
		if (buttons.size() == 1) {
			b.setHighlight(true);
			selectedNr = 0;
			selectedButton = b;
		}

		gbl.setConstraints(b, gbc);
		add(b);
	}

	//-------------------- getSelect --------------------
	public int getSelect() {
		return selectedNr;
	}

	public TImgButton getSelectedButton() {
		return selectedButton;
	}

	//-------------------- init --------------------
	public void init(int orientation) {
		buttons = new Vector();
		gbl = new GridBagLayout();
		setLayout(gbl);
		gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.NONE;

		if (orientation == HORIZONTAL) {
			gbc.gridx = GridBagConstraints.RELATIVE;
			gbc.gridy = 0;
		} else {
			gbc.gridx = 0;
			gbc.gridy = GridBagConstraints.RELATIVE;
		}
	}

	//-------------------- setSelect --------------------
	public boolean setSelect(int buttonNr) {
		// Find button
		TImgButton b;
		try {
			b = (TImgButton) buttons.elementAt(buttonNr);
		} catch (ArrayIndexOutOfBoundsException e) {
			return false;
		}

		if (b != selectedButton) {
			selectedButton.setHighlight(false);
			selectedButton = b;
			selectedButton.setHighlight(true);
			selectedNr = buttonNr;
		}
		return true;
	}

}
