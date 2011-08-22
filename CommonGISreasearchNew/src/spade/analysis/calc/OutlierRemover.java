package spade.analysis.calc;

import java.awt.Checkbox;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.util.Vector;

import spade.lib.basicwin.CManager;
import spade.lib.basicwin.Dialogs;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.OKDialog;
import spade.lib.util.IntArray;
import spade.lib.util.NumRange;
import spade.lib.util.StringUtil;
import spade.vis.database.DataRecord;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Nov 4, 2009
 * Time: 10:24:31 AM
 * Replaces too high and/or too low values in selected table columns
 * by specified "normal" values
 */
public class OutlierRemover extends BaseCalculator {
	/**
	* Returns the minimum number of attributes needed for this computation
	*/
	@Override
	public int getMinAttrNumber() {
		return 1;
	}

	/**
	* Returns the maximum number of attributes needed for this computation.
	* If the maximum number is unlimited, returns -1.
	*/
	@Override
	public int getMaxAttrNumber() {
		return -1;
	}

	/**
	* Returns an explanation about this calculation method
	*/
	@Override
	public String getExplanation() {
		return "Replaces too high and/or too low values in selected table columns" + " by specified \"normal\" values.";
	}

	/**
	* Returns an instruction for the user about what and how many attributes to
	* select.
	*/
	@Override
	public String getAttributeSelectionPrompt() {
		return "Select one or more numeric attributes for removing outliers";
	}

	/**
	 * Should return false if the Calculator only modifies the values of the selected
	 * attributes but does not create any new attributes.
	 * OutlierRemover returns false.
	 */
	@Override
	public boolean doesCreateNewAttributes() {
		return false;
	}

	/**
	* Performs the calculation and returns the list of identifiers of attributes
	* added to the table
	*/
	@Override
	public Vector doCalculation() {
		if (fn == null || fn.length < 1)
			return null;
		if (dTable == null)
			return null;
		IntArray iar = new IntArray(fn.length, 1);
		for (int element : fn) {
			iar.addElement(element);
		}
		NumRange nr = dTable.getValueRangeInColumns(iar);
		if (nr == null || Double.isNaN(nr.minValue) || Double.isNaN(nr.maxValue)) {
			err = "Could not get numeric values from the selected columns!";
			return null;
		}
		if (nr.minValue >= nr.maxValue) {
			err = "The minimum and maximum values in the selected columns are the same!";
			return null;
		}
		int prec = StringUtil.getPreferredPrecision((nr.minValue + nr.maxValue) / 2, nr.minValue, nr.maxValue);
		Panel p = new Panel();
		GridBagLayout gridbag = new GridBagLayout();
		p.setLayout(gridbag);
		GridBagConstraints c = new GridBagConstraints();
		c.weightx = 1.0f;
		c.weighty = 1.0f;
		c.fill = GridBagConstraints.HORIZONTAL;
		Label l = new Label("Current value range: from");
		c.gridwidth = 3;
		gridbag.setConstraints(l, c);
		p.add(l);
		TextField tf = new TextField(StringUtil.doubleToStr(nr.minValue, prec));
		tf.setEditable(false);
		c.gridwidth = 2;
		gridbag.setConstraints(tf, c);
		p.add(tf);
		l = new Label("to", Label.CENTER);
		c.gridwidth = 1;
		gridbag.setConstraints(l, c);
		p.add(l);
		tf = new TextField(StringUtil.doubleToStr(nr.maxValue, prec));
		tf.setEditable(false);
		c.gridwidth = 2;
		gridbag.setConstraints(tf, c);
		p.add(tf);
		l = new Label("");
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(l, c);
		p.add(l);
		Line line = new Line(false);
		gridbag.setConstraints(line, c);
		p.add(line);
		Checkbox cbReplaceLow = new Checkbox("Replace values below", false);
		c.gridwidth = 3;
		gridbag.setConstraints(cbReplaceLow, c);
		p.add(cbReplaceLow);
		TextField tfLowLim = new TextField();
		c.gridwidth = 2;
		gridbag.setConstraints(tfLowLim, c);
		p.add(tfLowLim);
		l = new Label("by", Label.CENTER);
		c.gridwidth = 1;
		gridbag.setConstraints(l, c);
		p.add(l);
		TextField tfLowRepl = new TextField();
		c.gridwidth = 2;
		gridbag.setConstraints(tfLowRepl, c);
		p.add(tfLowRepl);
		l = new Label("");
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(l, c);
		p.add(l);
		Checkbox cbReplaceHigh = new Checkbox("Replace values above", false);
		c.gridwidth = 3;
		gridbag.setConstraints(cbReplaceHigh, c);
		p.add(cbReplaceHigh);
		TextField tfHighLim = new TextField();
		c.gridwidth = 2;
		gridbag.setConstraints(tfHighLim, c);
		p.add(tfHighLim);
		l = new Label("by", Label.CENTER);
		c.gridwidth = 1;
		gridbag.setConstraints(l, c);
		p.add(l);
		TextField tfHighRepl = new TextField();
		c.gridwidth = 2;
		gridbag.setConstraints(tfHighRepl, c);
		p.add(tfHighRepl);
		l = new Label("");
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(l, c);
		p.add(l);
		Checkbox cbReplaceMissing = new Checkbox("Replace missing values by", false);
		c.gridwidth = 3;
		gridbag.setConstraints(cbReplaceMissing, c);
		p.add(cbReplaceMissing);
		TextField tfMissRepl = new TextField();
		c.gridwidth = 2;
		gridbag.setConstraints(tfMissRepl, c);
		p.add(tfMissRepl);
		l = new Label("");
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(l, c);
		p.add(l);

		double lowLim = Double.NaN, highLim = Double.NaN, lowRepl = Double.NaN, highRepl = Double.NaN, missRepl = Double.NaN;
		boolean ok = false;
		do {
			err = null;
			OKDialog okd = new OKDialog(CManager.getAnyFrame(), "Set parameters", true);
			okd.addContent(p);
			okd.show();
			if (okd.wasCancelled()) {
				err = "Operation cancelled";
				return null;
			}
			if (!cbReplaceLow.getState() && !cbReplaceHigh.getState() && !cbReplaceMissing.getState()) {
				Dialogs.showMessage(CManager.getAnyFrame(), "You should select at least one of the checkboxes!", "Nothing is selected!");
			} else {
				ok = true;
				if (cbReplaceLow.getState()) {
					lowLim = tryGetNumberFromTextField(tfLowLim, "Lower limit");
					if (Double.isNaN(lowLim)) {
						Dialogs.showMessage(CManager.getAnyFrame(), err, "Error!");
						ok = false;
					} else {
						lowRepl = tryGetNumberFromTextField(tfLowRepl, "Replacement for too low values");
						if (Double.isNaN(lowRepl)) {
							Dialogs.showMessage(CManager.getAnyFrame(), err, "Error!");
							ok = false;
						}
					}
				}
				if (ok && cbReplaceHigh.getState()) {
					highLim = tryGetNumberFromTextField(tfHighLim, "Upper limit");
					if (Double.isNaN(highLim)) {
						Dialogs.showMessage(CManager.getAnyFrame(), err, "Error!");
						ok = false;
					} else {
						highRepl = tryGetNumberFromTextField(tfHighRepl, "Replacement for too high values");
						if (Double.isNaN(highRepl)) {
							Dialogs.showMessage(CManager.getAnyFrame(), err, "Error!");
							ok = false;
						}
					}
				}
				if (ok && !Double.isNaN(lowLim) && !Double.isNaN(highLim) && lowLim >= highLim) {
					Dialogs.showMessage(CManager.getAnyFrame(), "The lower limit exceeds or equals the upper limit!!", "Error!");
					ok = false;
				}
				if (ok && cbReplaceMissing.getState()) {
					missRepl = tryGetNumberFromTextField(tfMissRepl, "Replacement for missing values");
					if (Double.isNaN(missRepl)) {
						Dialogs.showMessage(CManager.getAnyFrame(), err, "Error!");
						ok = false;
					}
				}
			}
		} while (!ok);

		for (int i = 0; i < dTable.getDataItemCount(); i++) {
			DataRecord rec = dTable.getDataRecord(i);
			for (int cN : fn) {
				double val = rec.getNumericAttrValue(cN);
				double repl = Double.NaN;
				if (Double.isNaN(val))
					if (!Double.isNaN(missRepl)) {
						repl = missRepl;
					} else {
						;
					}
				else if (!Double.isNaN(lowLim) && val < lowLim) {
					repl = lowRepl;
				} else if (!Double.isNaN(highLim) && val > highLim) {
					repl = highRepl;
				}
				if (!Double.isNaN(repl)) {
					rec.setNumericAttrValue(repl, StringUtil.doubleToStr(repl, prec), cN);
				}
			}
		}

		return null;
	}

	protected double tryGetNumberFromTextField(TextField tf, String tfName) {
		if (tf == null)
			return Double.NaN;
		String str = tf.getText();
		if (str == null || str.trim().length() < 1) {
			err = "No text in the text field";
			if (tfName != null) {
				err += " \"" + tfName + "\"!";
			} else {
				err += "!";
			}
			return Double.NaN;
		}
		str = str.trim();
		try {
			double value = Double.parseDouble(str);
			return value;
		} catch (Exception e) {
		}
		err = "Not a number in the text field";
		if (tfName != null) {
			err += " \"" + tfName + "\"!";
		} else {
			err += "!";
		}
		return Double.NaN;
	}
}
