package spade.lib.basicwin;

import java.awt.BorderLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 27-Apr-2007
 * Time: 11:02:26
 */
public class InputIntPanel extends Panel implements DialogContent {
	protected int defaultVal, min, max;
	protected String question = null;
	protected String err = null;
	protected TextField tf = null;
	protected int enteredVal;

	/**
	 * Constructs a UI for entering a single integer number in a text field
	 * @param question - used for a label on the left of the text field; no label if null
	 * @param defaultValue
	 * @param min - minimum allowed value
	 * @param max - maximum allowed value
	 * @param explanation - optional; when not null, a label or text canvas with
	 *                      this text is included in the panel above the text field
	 */
	public InputIntPanel(String question, int defaultValue, int min, int max, String explanation) {
		super();
		defaultVal = defaultValue;
		this.min = min;
		this.max = max;
		this.question = question;
		tf = new TextField(String.valueOf(defaultValue), 10);
		setLayout(new ColumnLayout());
		Panel p = new Panel(new BorderLayout());
		p.add(tf, BorderLayout.CENTER);
		if (question != null) {
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

	public int getEnteredValue() {
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
		String str = tf.getText();
		if (str != null) {
			str = str.trim();
		}
		if (str == null || str.length() < 1) {
			err = "No value entered";
			addFieldName();
			return false;
		}
		int k = 0;
		try {
			k = Integer.parseInt(str);
		} catch (NumberFormatException nfe) {
			err = "A number is expected";
			addFieldName();
			return false;
		}
		if (k < min) {
			err = "The value";
			addFieldName();
			err += " must be not less than " + min + "!";
			return false;
		}
		if (k > max) {
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
