package spade.lib.basicwin;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Jan 22, 2009
 * Time: 2:48:35 PM
 * To change this template use File | Settings | File Templates.
 */
public class InputDoublePanel extends Panel implements DialogContent {
	protected double defaultVal = Double.NaN, min = Double.NaN, max = Double.NaN;
	protected String question = null;
	protected String err = null;
	protected TextField tf = null;
	protected Checkbox cb = null;
	protected double enteredVal = Double.NaN;

	/**
	 * Constructs a UI for entering a single double number in a text field
	 * @param question - used for a label on the left of the text field; no label if null
	 * @param defaultValue
	 * @param min - minimum allowed value
	 * @param max - maximum allowed value
	 * @param explanation - optional; when not null, a label or text canvas with
	 *                      this text is included in the panel above the text field
	 */
	public InputDoublePanel(String question, double defaultValue, double min, double max, String explanation) {
		this(question, false, defaultValue, min, max, explanation);
	}

	/**
	 * Constructs a UI for entering a single double number in a text field
	 * @param question - used for a label on the left of the text field; no label if null
	 * @param includeCheckBox - whether a checkbox must be included
	 * @param defaultValue
	 * @param min - minimum allowed value
	 * @param max - maximum allowed value
	 * @param explanation - optional; when not null, a label or text canvas with
	 *                      this text is included in the panel above the text field
	 */
	public InputDoublePanel(String question, boolean includeCheckBox, double defaultValue, double min, double max, String explanation) {
		super();
		defaultVal = defaultValue;
		this.min = min;
		this.max = max;
		this.question = question;
		tf = new TextField(String.valueOf(defaultValue), 10);
		setLayout(new ColumnLayout());
		Panel p = new Panel(new BorderLayout());
		p.add(tf, BorderLayout.CENTER);
		if (includeCheckBox) {
			cb = new Checkbox((question == null) ? "" : question, false);
			p.add(cb, BorderLayout.WEST);
		} else if (question != null) {
			p.add(new Label(question), BorderLayout.WEST);
		}
		if (explanation != null)
			if (explanation.length() <= 60) {
				add(new Label(explanation, Label.CENTER));
			} else {
				TextCanvas tc = new TextCanvas();
				tc.setText(explanation);
				add(tc);
			}
		add(p);
	}

	public boolean isSelected() {
		if (cb == null)
			return true;
		return cb.getState();
	}

	public double getEnteredValue() {
		if (Double.isNaN(enteredVal)) {
			canClose();
		}
		return enteredVal;
	}

	protected void addFieldName() {
		if (err == null || question == null)
			return;
		err += " in the field <" + question + ">";

	}

	@Override
	public boolean canClose() {
		err = null;
		enteredVal = Double.NaN;
		String str = tf.getText();
		if (str != null) {
			str = str.trim();
		}
		if (str == null || str.length() < 1) {
			err = "No value entered";
			addFieldName();
			return false;
		}
		double k = Double.NaN;
		try {
			k = Double.parseDouble(str);
		} catch (NumberFormatException nfe) {
			err = "A number is expected";
			addFieldName();
			return false;
		}
		if (!Double.isNaN(min) && k < min) {
			err = "The value";
			addFieldName();
			err += " must be not less than " + min + "!";
			return false;
		}
		if (!Double.isNaN(max) && k > max) {
			err = "The value";
			addFieldName();
			err += " must be not bigger than " + max + "!";
			return false;
		}
		enteredVal = k;
		return true;
	}

	@Override
	public String getErrorMessage() {
		return err;
	}
}
