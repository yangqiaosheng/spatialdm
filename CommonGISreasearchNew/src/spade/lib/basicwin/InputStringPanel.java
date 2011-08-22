package spade.lib.basicwin;

import java.awt.BorderLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 27-Apr-2007
 * Time: 11:53:17
 */
public class InputStringPanel extends Panel implements DialogContent {
	protected String defValue = null;
	protected String question = null;
	protected String err = null;
	protected TextField tf = null;
	protected String enteredVal = null;

	/**
	 * Constructs a UI for entering a single string value in a text field
	 * @param question - used for a label on the left of the text field; no label if null
	 * @param defValue - default value
	 * @param explanation - optional; when not null, a label or text canvas with
	 *                      this text is included in the panel above the text field
	 */
	public InputStringPanel(String question, String defValue, String explanation) {
		super();
		this.defValue = defValue;
		this.question = question;
		tf = new TextField(defValue, Math.max(40, defValue.length()));
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

	public String getEnteredValue() {
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
		enteredVal = str;
		return true;
	}

	@Override
	public String getErrorMessage() {
		return err;
	}
}
