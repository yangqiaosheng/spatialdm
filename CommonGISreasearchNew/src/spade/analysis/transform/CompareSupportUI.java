package spade.analysis.transform;

import java.awt.Button;
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
* The controls for setting parameters in a CompareSupport
*/
public class CompareSupportUI extends Panel implements ActionListener, ItemListener {
	static ResourceBundle res = Language.getTextResource("spade.analysis.transform.Res");
	/**
	* The transformer of numeric attributes to be controlled through this
	* panel
	*/
	protected CompareSupport ctrans = null;
	/**
	* Used for choosing between differences and ratios
	*/
	protected Choice difRatioCh = null;
	/**
	* Used for specifying the name of the object or the value to compare with
	*/
	protected TextField objValTF = null;
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
	public CompareSupportUI(CompareSupport transformer) {
		ctrans = transformer;
		if (ctrans == null)
			return;
		ColumnLayout cl = new ColumnLayout();
		cl.setAlignment(ColumnLayout.Hor_Left);
		setLayout(cl);
		add(new Label(res.getString("comparison") + ":"));
		changeBt = new Button(res.getString("change"));
		changeBt.setActionCommand("change");
		changeBt.addActionListener(this);
		difRatioCh = new Choice();
		difRatioCh.addItemListener(this);
		difRatioCh.add(res.getString("difference_to"));
		difRatioCh.add(res.getString("ratio_to"));
		add(infoPan = makeInfoPanel());
	}

	/**
	* Depending on the current transformation mode and settings, constructs a
	* component with corresponding information and control elements
	*/
	protected Component makeInfoPanel() {
		if (ctrans == null)
			return new Label(res.getString("off"));
		if (ctrans.getTransMode() == CompareSupport.COMP_NONE) {
			Panel p = new Panel(new RowLayout(5, 0));
			p.add(new Label(res.getString("off")));
			p.add(changeBt);
			return p;
		}
		ColumnLayout cl = new ColumnLayout();
		cl.setAlignment(ColumnLayout.Hor_Left);
		Panel p = new Panel(cl);
		p.add(difRatioCh);
		if (ctrans.getComputeRatios()) {
			difRatioCh.select(1);
		} else {
			difRatioCh.select(0);
		}
		Panel pp = new Panel(new RowLayout(5, 0));
		if (ctrans.getTransMode() == CompareSupport.COMP_AVG) {
			pp.add(new Label(res.getString("mean")));
		} else if (ctrans.getTransMode() == CompareSupport.COMP_MEDIAN) {
			pp.add(new Label(res.getString("median")));
		} else if (ctrans.getTransMode() == CompareSupport.COMP_VALUE) {
			pp.add(new Label(res.getString("value")));
			if (objValTF == null) {
				objValTF = new TextField(10);
				objValTF.addActionListener(this);
			}
			objValTF.setText(String.valueOf(ctrans.getVisCompValue()));
			pp.add(objValTF);
		} else if (ctrans.getTransMode() == CompareSupport.COMP_OBJECT) {
			pp.add(new Label(res.getString("object")));
			if (objValTF == null) {
				objValTF = new TextField(10);
				objValTF.addActionListener(this);
			}
			objValTF.setText(ctrans.getVisCompObjName());
			pp.add(objValTF);
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
				CompareSupportDlg pdlg = new CompareSupportDlg(ctrans);
				OKDialog okd = new OKDialog(CManager.getAnyFrame(this), res.getString("comparison"), true);
				okd.add(pdlg);
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
		} else if (e.getSource().equals(objValTF)) {
			String str = objValTF.getText();
			if (str != null) {
				str = str.trim();
				if (str.length() < 1) {
					str = null;
				}
			}
			if (str == null) {
				if (ctrans.getTransMode() == CompareSupport.COMP_OBJECT) {
					objValTF.setText(ctrans.getVisCompObjName());
				} else {
					objValTF.setText(String.valueOf(ctrans.getVisCompValue()));
				}
				return;
			}
			if (ctrans.getTransMode() == CompareSupport.COMP_OBJECT)
				if (ctrans.setVisCompObjName(str)) {
					ctrans.doTransformation();
				} else {
					objValTF.setText(ctrans.getVisCompObjName());
				}
			else {
				float val = Float.NaN;
				try {
					val = Float.valueOf(str).floatValue();
				} catch (NumberFormatException nfe) {
				}
				if (!Float.isNaN(val) && ctrans.setVisCompValue(val)) {
					ctrans.doTransformation();
				} else {
					objValTF.setText(String.valueOf(ctrans.getVisCompValue()));
				}
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
			if (ctrans.getComputeRatios() != (difRatioCh.getSelectedIndex() == 1)) {
				ctrans.setComputeRatios(difRatioCh.getSelectedIndex() == 1);
				ctrans.doTransformation();
			}
		}
	}
}