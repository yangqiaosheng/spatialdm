package spade.analysis.transform;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Choice;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ResourceBundle;

import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Line;
import spade.lib.lang.Language;

/**
* Used in a dialog for the selection of the mode and parameters for transforming
* a time-dependent numeric attribute
*/
public class CompareSupportDlg extends Panel implements ItemListener {
	static ResourceBundle res = Language.getTextResource("spade.analysis.transform.Res");
	/**
	* The identifiers of the names of the transformation modes in the resource file
	*/
	public static final String modeIds[] = { "off", "sel_object", "sel_value", "mean", "median" };
	/**
	* The transformer of numeric attributes to be controlled through this
	* panel
	*/
	protected CompareSupport ctrans = null;
	/**
	* Used for selection of the temporal transformation (comparison) mode
	*/
	protected Checkbox modeCB[] = null;
	/**
	* Used for choosing between differences and ratios
	*/
	protected Choice difRatioCh = null;
	/**
	* Used for specifying the name of the object to compare with
	*/
	protected TextField objTF = null;
	/**
	* Used for specifying the value to compare with
	*/
	protected TextField valTF = null;

	/**
	* Constructs the dialog panel
	*/
	public CompareSupportDlg(CompareSupport transformer) {
		ctrans = transformer;
		if (ctrans == null)
			return;

		CheckboxGroup cbg = new CheckboxGroup();
		modeCB = new Checkbox[modeIds.length];
		for (int i = 0; i < modeIds.length; i++) {
			modeCB[i] = new Checkbox(res.getString(modeIds[i]), i == ctrans.getTransMode(), cbg);
			modeCB[i].addItemListener(this);
		}
		difRatioCh = new Choice();
		difRatioCh.add(res.getString("difference_to"));
		difRatioCh.add(res.getString("ratio_to"));
		if (ctrans.getComputeRatios()) {
			difRatioCh.select(1);
		}
		if (ctrans.getTransMode() == CompareSupport.COMP_NONE) {
			difRatioCh.setEnabled(false);
		}
		objTF = new TextField(10);
		String str = ctrans.getVisCompObjName();
		if (str != null) {
			objTF.setText(str);
		}
		if (ctrans.getTransMode() != CompareSupport.COMP_OBJECT) {
			objTF.setEnabled(false);
		}
		valTF = new TextField(10);
		double val = ctrans.getVisCompValue();
		if (!Double.isNaN(val)) {
			valTF.setText(String.valueOf(val));
		}
		if (ctrans.getTransMode() != CompareSupport.COMP_VALUE) {
			valTF.setEnabled(false);
		}

		setLayout(new ColumnLayout());
		add(new Label(res.getString("comparison") + ":"), BorderLayout.CENTER);
		add(modeCB[0]);
		add(new Line(false));
		Panel p = new Panel(new BorderLayout());
		p.add(new Label(res.getString("compute"), Label.CENTER), BorderLayout.CENTER);
		p.add(difRatioCh, BorderLayout.EAST);
		add(p);
		p = new Panel(new BorderLayout());
		p.add(modeCB[1], BorderLayout.WEST);
		p.add(objTF, BorderLayout.EAST);
		add(p);
		p = new Panel(new BorderLayout());
		p.add(modeCB[2], BorderLayout.WEST);
		p.add(valTF, BorderLayout.EAST);
		add(p);
		add(modeCB[3]);
		add(modeCB[4]);
	}

	/**
	* Reacts to a selection of the comparison mode (checkboxes) by enabling or
	* disabling the remaining controls
	*/
	@Override
	public void itemStateChanged(ItemEvent e) {
		difRatioCh.setEnabled(!modeCB[0].getState());
		objTF.setEnabled(modeCB[1].getState());
		valTF.setEnabled(modeCB[2].getState());
	}

	/**
	* Makes the settings in the time attribute transformer according to the
	* current states of the controls. Returns true if anything changed.
	*/
	public boolean setupTransformer() {
		if (ctrans == null)
			return false;
		boolean changed = false;
		int modeIdx = -1;
		for (int i = 0; i < modeCB.length && modeIdx < 0; i++)
			if (modeCB[i].getState()) {
				modeIdx = i;
			}
		changed = modeIdx != ctrans.getTransMode();
		if (changed) {
			ctrans.setTransMode(modeIdx);
		}
		if (modeIdx > 0 && ctrans.getComputeRatios() != (difRatioCh.getSelectedIndex() == 1)) {
			ctrans.setComputeRatios(difRatioCh.getSelectedIndex() == 1);
			changed = true;
		}
		switch (modeIdx) {
		case 1:
			String str = objTF.getText();
			if (str != null) {
				str = str.trim();
				if (str.length() > 0) {
					changed = ctrans.setVisCompObjName(str) || changed;
				}
			}
			break;
		case 2:
			str = valTF.getText();
			if (str != null) {
				str = str.trim();
				if (str.length() > 0) {
					try {
						float val = Float.valueOf(str).floatValue();
						changed = ctrans.setVisCompValue(val) || changed;
					} catch (NumberFormatException nfe) {
					}
				}
			}
			break;
		}
		if (changed) {
			ctrans.doTransformation();
		}
		return changed;
	}
}