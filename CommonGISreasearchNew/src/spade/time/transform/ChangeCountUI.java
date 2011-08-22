package spade.time.transform;

import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Choice;
import java.awt.Component;
import java.awt.Container;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ResourceBundle;

import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.OKDialog;
import spade.lib.basicwin.RowLayout;
import spade.lib.lang.Language;

/**
* A set of controls for setting the parameters for transforming time-dependent
* attributes (i.e. various visual comparison modes).
*/
public class ChangeCountUI extends Panel implements ActionListener, ItemListener {
	static ResourceBundle res = Language.getTextResource("spade.time.transform.Res");
	/**
	* The transformer of time-dependent attributes to be controlled through this
	* panel
	*/
	protected ChangeCounter ttrans = null;
	/**
	* Used for choosing between differences and ratios
	*/
	protected Choice difRatioCh = null;
	/**
	* Used for selecting the time moment to compare with
	*/
	protected TextField momentTF = null;
	/**
	 * Whether to normalise the differences by standard deviation or inter-quartile distance
	 */
	protected Checkbox normalizeCB = null;
	/**
	* Button "Change"
	*/
	protected Button changeBt = null;
	/**
	* The component that changes its content depending on the current transformation
	* mode and its parameters.
	*/
	protected Component infoPan = null;

	/**
	* Constructs the panel
	*/
	public ChangeCountUI(ChangeCounter transformer) {
		ttrans = transformer;
		if (ttrans == null)
			return;
		ColumnLayout cl = new ColumnLayout();
		cl.setAlignment(ColumnLayout.Hor_Left);
		setLayout(cl);
		add(new Label(res.getString("temp_comp") + ":"));
		changeBt = new Button(res.getString("change"));
		changeBt.setActionCommand("change");
		changeBt.addActionListener(this);
		difRatioCh = new Choice();
		difRatioCh.addItemListener(this);
		difRatioCh.add(res.getString("difference_to"));
		difRatioCh.add(res.getString("ratio_to"));
		normalizeCB = new Checkbox("normalise", canNormalize() && ttrans.getNormalize());
		normalizeCB.addItemListener(this);
		add(infoPan = makeInfoPanel());
	}

	protected boolean canNormalize() {
		int mode = ttrans.getVisCompMode();
		return (mode == ChangeCounter.COMP_MEAN || mode == ChangeCounter.COMP_MEDIAN) && !ttrans.computeRatios;
	}

	/**
	* Depending on the current transformation mode and settings, constructs a
	* component with corresponding information and control elements
	*/
	protected Component makeInfoPanel() {
		if (ttrans == null)
			return new Label(res.getString("off"));
		int mode = ttrans.getVisCompMode();
		if (mode == ChangeCounter.COMP_NONE) {
			Panel p = new Panel(new RowLayout(5, 0));
			p.add(new Label(res.getString("off")));
			p.add(changeBt);
			return p;
		}
		ColumnLayout cl = new ColumnLayout();
		cl.setAlignment(ColumnLayout.Hor_Left);
		Panel p = new Panel(cl);
		p.add(difRatioCh);
		if (ttrans.getComputeRatios()) {
			difRatioCh.select(1);
		} else {
			difRatioCh.select(0);
		}
		Panel pp = new Panel(new RowLayout(5, 0));
		if (mode == ChangeCounter.COMP_PREV_MOMENT) {
			pp.add(new Label(res.getString("previous")));
		} else if (mode == ChangeCounter.COMP_FIXED_MOMENT) {
			pp.add(new Label(res.getString("moment")));
			if (momentTF == null) {
				momentTF = new TextField(10);
				momentTF.addActionListener(this);
			}
			if (ttrans.getVisCompMoment() != null) {
				momentTF.setText(ttrans.getVisCompMoment().toString());
			}
			pp.add(momentTF);
		} else if (mode == ChangeCounter.COMP_MEAN || mode == ChangeCounter.COMP_MEDIAN) {
			String str = (mode == ChangeCounter.COMP_MEAN) ? res.getString("mean") : res.getString("median");
			pp.add(new Label(str));
			pp.add(normalizeCB);
			normalizeCB.setEnabled(canNormalize());
			normalizeCB.setState(ttrans.getNormalize());
		}
		pp.add(changeBt);
		p.add(pp);
		return p;
	}

	/**
	* After pressing the button "change", displays a dialog for changing the
	* temporal transformation mode and parameters
	*/
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() instanceof Button) {
			String cmd = e.getActionCommand();
			if (cmd.equals("change")) {
				ChangeCountDlg pdlg = new ChangeCountDlg(ttrans);
				OKDialog okd = new OKDialog(CManager.getAnyFrame(this), res.getString("temp_comp"), true);
				okd.addContent(pdlg);
				okd.show();
				if (okd.wasCancelled())
					return;
				if (pdlg.setupTransformer()) {
					setVisible(false);
					remove(infoPan);
					if (infoPan instanceof Container) {
						((Container) infoPan).removeAll();
					}
					add(infoPan = makeInfoPanel());
					setVisible(true);
					CManager.validateAll(infoPan);
				}
			}
		} else if (e.getSource().equals(momentTF)) {
			if (!ttrans.setVisCompMoment(momentTF.getText())) {
				momentTF.setText(ttrans.getVisCompMoment().toString());
			} else {
				ttrans.doTransformation();
			}
		}
	}

	/**
	* Reacts to a selection of the reference time moment in the combobox with
	* time moments and to the switch between computing differences and ratios
	*/
	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource().equals(difRatioCh)) {
			boolean ratios = difRatioCh.getSelectedIndex() == 1;
			if (ttrans.getComputeRatios() != ratios) {
				int mode = ttrans.getVisCompMode();
				if (mode == ChangeCounter.COMP_MEAN || mode == ChangeCounter.COMP_MEDIAN) {
					normalizeCB.setEnabled(!ratios);
					if (ratios && normalizeCB.getState()) {
						normalizeCB.setState(false);
						ttrans.setNormalize(false);
					}
				}
				ttrans.setComputeRatios(difRatioCh.getSelectedIndex() == 1);
				ttrans.doTransformation();
			}
		} else if (e.getSource().equals(normalizeCB)) {
			if (normalizeCB.getState() != ttrans.getNormalize()) {
				ttrans.setNormalize(normalizeCB.getState());
				ttrans.doTransformation();
			}
		}
	}
}