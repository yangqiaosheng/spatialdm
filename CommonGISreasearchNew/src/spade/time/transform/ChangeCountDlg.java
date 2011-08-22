package spade.time.transform;

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
import spade.lib.basicwin.DialogContent;
import spade.lib.basicwin.Line;
import spade.lib.lang.Language;
import spade.time.TimeMoment;
import spade.vis.database.Parameter;

/**
* Used in a dialog for the selection of the mode and parameters for transforming
* a time-dependent numeric attribute
*/
public class ChangeCountDlg extends Panel implements ItemListener, DialogContent {
	static ResourceBundle res = Language.getTextResource("spade.time.transform.Res");
	/**
	* The identifiers of the names of the transformation modes in the resource file
	*/
	public static final String modeIds[] = { "off", "previous", "sel_moment", "mean", "median" };
	/**
	* The transformer of time-dependent attributes to be controlled through this
	* panel
	*/
	protected ChangeCounter ttrans = null;
	/**
	* The temporal parameter from the table with the data
	*/
	protected Parameter tPar = null;
	/**
	* Used for selection of the temporal transformation (comparison) mode
	*/
	protected Checkbox modeCB[] = null;
	/**
	* Used for choosing between differences and ratios
	*/
	protected Choice difRatioCh = null;
	/**
	* Used for setting the time moment to compare with
	*/
	protected TextField momentTF = null;
	/**
	 * Whether to normalise the differences by standard deviation or inter-quartile distance
	 */
	protected Checkbox normalizeCB = null;
	/**
	* This variable contains the error message if the settings in the dialog are
	* invalid
	*/
	public String err = null;

	/**
	* Constructs the dialog panel
	*/
	public ChangeCountDlg(ChangeCounter transformer) {
		ttrans = transformer;
		if (ttrans == null)
			return;
		tPar = ttrans.getTemporalParameter();
		if (tPar == null)
			return;

		CheckboxGroup cbg = new CheckboxGroup();
		modeCB = new Checkbox[modeIds.length];
		int mode = ttrans.getVisCompMode();
		for (int i = 0; i < modeIds.length; i++) {
			modeCB[i] = new Checkbox(res.getString(modeIds[i]), i == mode, cbg);
			modeCB[i].addItemListener(this);
		}
		difRatioCh = new Choice();
		difRatioCh.add(res.getString("difference_to"));
		difRatioCh.add(res.getString("ratio_to"));
		if (ttrans.getComputeRatios()) {
			difRatioCh.select(1);
		}
		if (mode == ChangeCounter.COMP_NONE) {
			difRatioCh.setEnabled(false);
		}
		momentTF = new TextField(10);
		if (mode != ChangeCounter.COMP_FIXED_MOMENT) {
			momentTF.setEnabled(false);
		} else {
			momentTF.setText(ttrans.getVisCompMoment().toString());
		}

		setLayout(new ColumnLayout());
		add(new Label(res.getString("temp_comp") + ":"), BorderLayout.CENTER);
		add(modeCB[0]);
		add(new Line(false));
		Panel p = new Panel(new BorderLayout());
		p.add(new Label(res.getString("compute"), Label.CENTER), BorderLayout.CENTER);
		p.add(difRatioCh, BorderLayout.EAST);
		add(p);
		add(modeCB[1]);
		p = new Panel(new BorderLayout());
		p.add(modeCB[2], BorderLayout.WEST);
		p.add(momentTF, BorderLayout.EAST);
		add(p);
		add(new Line(false));
		add(modeCB[3]);
		add(modeCB[4]);
		normalizeCB = new Checkbox("normalise", ttrans.getNormalize());
		add(normalizeCB);
		normalizeCB.setEnabled(canNormalize());
		if (!normalizeCB.isEnabled()) {
			normalizeCB.setState(false);
		}
		if (modeCB.length > 5) {
			add(new Line(false));
			for (int i = 5; i < modeCB.length; i++) {
				add(modeCB[i]);
			}
		}
	}

	protected boolean canNormalize() {
		return (modeCB[3].getState() || modeCB[4].getState()) && difRatioCh.getSelectedIndex() == 0;
	}

	/**
	* Reacts to a selection of the comparison mode (checkboxes) by enabling or
	* disabling the remaining controls
	*/
	@Override
	public void itemStateChanged(ItemEvent e) {
		difRatioCh.setEnabled(!modeCB[0].getState());
		momentTF.setEnabled(modeCB[2].getState());
		normalizeCB.setEnabled(canNormalize());
		if (!normalizeCB.isEnabled()) {
			normalizeCB.setState(false);
		}
	}

	/**
	* Checks the validity of the settings in the dialog
	*/
	@Override
	public boolean canClose() {
		int modeIdx = -1;
		for (int i = 0; i < modeCB.length && modeIdx < 0; i++)
			if (modeCB[i].getState()) {
				modeIdx = i;
			}
		if (modeIdx == 2) {
			String str = momentTF.getText();
			if (str == null || str.trim().length() < 1) {
				err = res.getString("no_cmp_moment");
				return false;
			}
			str = str.trim();
			TimeMoment tm = (TimeMoment) tPar.getFirstValue(), tm1 = tm.getCopy();
			if (!tm1.setMoment(str)) {
				err = res.getString("ill_moment_string");
				return false;
			}
			if (tm1.compareTo(tm) < 0) {
				err = res.getString("ill_cmp_moment") + ": " + res.getString("must_be") + " " + res.getString("not_earlier_than") + " " + tm.toString() + "!";
				return false;
			}
			tm = (TimeMoment) tPar.getValue(tPar.getValueCount() - 1);
			if (tm1.compareTo(tm) > 0) {
				err = res.getString("ill_cmp_moment") + ": " + res.getString("must_be") + " " + res.getString("not_later_than") + " " + tm.toString() + "!";
				return false;
			}
		}
		return true;
	}

	/**
	* Makes the settings in the time attribute transformer according to the
	* current states of the controls. Returns true if anything changed.
	*/
	public boolean setupTransformer() {
		if (ttrans == null)
			return false;
		boolean changed = false;
		int modeIdx = -1;
		for (int i = 0; i < modeCB.length && modeIdx < 0; i++)
			if (modeCB[i].getState()) {
				modeIdx = i;
			}
		changed = modeIdx != ttrans.getVisCompMode();
		if (changed) {
			ttrans.setVisCompMode(modeIdx);
		}
		if (modeIdx > 0 && ttrans.getComputeRatios() != (difRatioCh.getSelectedIndex() == 1)) {
			ttrans.setComputeRatios(difRatioCh.getSelectedIndex() == 1);
			changed = true;
		}
		if (modeIdx == 2) {
			changed = ttrans.setVisCompMoment(momentTF.getText()) || changed;
		}
		if ((modeIdx == 3 || modeIdx == 4) && normalizeCB.getState() != ttrans.getNormalize()) {
			changed = true;
			ttrans.setNormalize(normalizeCB.getState());
		}
		if (changed) {
			ttrans.doTransformation();
		}
		return changed;
	}

	/**
	* Returns the error message if the settings in the dialog are invalid
	*/
	@Override
	public String getErrorMessage() {
		return err;
	}
}