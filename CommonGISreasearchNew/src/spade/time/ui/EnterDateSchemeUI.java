package spade.time.ui;

import java.awt.BorderLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;

import spade.lib.basicwin.DialogContent;
import spade.lib.basicwin.TextCanvas;
import spade.time.Date;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Sep 29, 2009
 * Time: 12:57:31 PM
 * Allows the user to enter or edit a scheme (template) for dates.
 * Checks if the entered template is valid.
 */
public class EnterDateSchemeUI extends Panel implements DialogContent {
	protected TextField tf = null;
	protected String err = null;

	public EnterDateSchemeUI(String explanation, String label, String origScheme) {
		setLayout(new BorderLayout());
		if (explanation != null) {
			TextCanvas tc = new TextCanvas();
			tc.setText(explanation);
			add(tc, BorderLayout.NORTH);
		}
		if (label != null) {
			add(new Label(label), BorderLayout.WEST);
		}
		tf = new TextField(20);
		if (origScheme != null) {
			tf.setText(origScheme);
		}
		add(tf, BorderLayout.CENTER);
	}

	@Override
	public boolean canClose() {
		err = Date.checkTemplateValidity(tf.getText());
		return err == null;
	}

	@Override
	public String getErrorMessage() {
		return err;
	}

	public String getScheme() {
		return tf.getText();
	}
}
