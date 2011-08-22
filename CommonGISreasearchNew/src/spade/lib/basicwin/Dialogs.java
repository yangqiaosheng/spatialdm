package spade.lib.basicwin;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Frame;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextArea;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 26-Apr-2007
 * Time: 17:13:22
 * Contains various utility methods for displaying messages, asking for
 * "yes" or "no" answers or for some values.
 */
public class Dialogs {

	public static void showMessage(Frame mainFrame, String text, String title) {
		if (text == null)
			return;
		if (mainFrame == null) {
			mainFrame = CManager.getAnyFrame();
		}
		Component c = null;
		if (text.length() <= 60) {
			c = new Label(text, Label.CENTER);
		} else {
			TextCanvas tc = new TextCanvas();
			tc.setText(text);
			c = tc;
		}
		OKDialog dia = new OKDialog(mainFrame, title, false);
		dia.addContent(c);
		dia.show();
	}

	public static boolean askYesOrNo(Frame mainFrame, String text, String title) {
		if (text == null)
			return false;
		if (mainFrame == null) {
			mainFrame = CManager.getAnyFrame();
		}
		Component c = null;
		if (text.length() <= 60) {
			c = new Label(text, Label.CENTER);
		} else {
			TextCanvas tc = new TextCanvas();
			tc.setText(text);
			c = tc;
		}
		OKDialog dia = new OKDialog(mainFrame, title, OKDialog.YES_NO_MODE, true);
		dia.addContent(c);
		dia.show();
		return !dia.wasCancelled();
	}

	public static String askForStringValue(Frame mainFrame, String question, String defaultValue, String explanation, String title, boolean allowCancel) {
		if (mainFrame == null) {
			mainFrame = CManager.getAnyFrame();
		}
		InputStringPanel isp = new InputStringPanel(question, defaultValue, explanation);
		OKDialog dia = new OKDialog(mainFrame, title, allowCancel);
		dia.addContent(isp);
		dia.show();
		if (allowCancel && dia.wasCancelled())
			return null;
		return isp.getEnteredValue();
	}

	/**
	 * Creates a dialog for editing or entering string values in multiple text fields
	 * @param fieldLabels - used for labels on the left of the text fields; no label if null
	 * @param defaultValues - default values
	 * @param explanation - optional; when not null, a label or text canvas with
	 *                      this text is included in the panel above the text field
	 */
	public static String[] editStringValues(Frame mainFrame, String fieldLabels[], String defaultValues[], String explanation, String title, boolean allowCancel) {
		if (mainFrame == null) {
			mainFrame = CManager.getAnyFrame();
		}
		EditStringsPanel esp = new EditStringsPanel(fieldLabels, defaultValues, explanation);
		OKDialog dia = new OKDialog(mainFrame, title, allowCancel);
		dia.addContent(esp);
		dia.show();
		if (allowCancel && dia.wasCancelled())
			return null;
		return esp.getEnteredValues();
	}

	/**
	 * Returns Integer.MIN_VALUE if the dialog has been cancelled
	 */
	public static int askForIntValue(Frame mainFrame, String question, int defaultValue, int min, int max, String explanation, String title, boolean allowCancel) {
		if (mainFrame == null) {
			mainFrame = CManager.getAnyFrame();
		}
		InputIntPanel iip = new InputIntPanel(question, defaultValue, min, max, explanation);
		OKDialog dia = new OKDialog(mainFrame, title, allowCancel);
		dia.addContent(iip);
		dia.show();
		if (allowCancel && dia.wasCancelled())
			return Integer.MIN_VALUE;
		return iip.getEnteredValue();
	}

	/**
	 * Returns Double.NaN if the dialog has been cancelled
	 */
	public static double askForDoubleValue(Frame mainFrame, String question, double defaultValue, double min, double max, String explanation, String title, boolean allowCancel) {
		if (mainFrame == null) {
			mainFrame = CManager.getAnyFrame();
		}
		InputDoublePanel idp = new InputDoublePanel(question, defaultValue, min, max, explanation);
		OKDialog dia = new OKDialog(mainFrame, title, allowCancel);
		dia.addContent(idp);
		dia.show();
		if (allowCancel && dia.wasCancelled())
			return Double.NaN;
		return idp.getEnteredValue();
	}

	public static String askForComment(Frame mainFrame, String question, String explanation, String title, boolean allowCancel) {
		Panel p = new Panel(new BorderLayout());
		if (question != null) {
			p.add(new Label(question, Label.CENTER), BorderLayout.NORTH);
		}
		if (explanation != null) {
			TextCanvas tc = new TextCanvas();
			tc.setText(explanation);
			p.add(tc, BorderLayout.SOUTH);
		}
		TextArea tArea = new TextArea(10, 60);
		p.add(tArea, BorderLayout.CENTER);
		if (mainFrame == null) {
			mainFrame = CManager.getAnyFrame();
		}
		OKDialog dia = new OKDialog(mainFrame, title, allowCancel);
		dia.addContent(p);
		dia.show();
		if (allowCancel && dia.wasCancelled())
			return null;
		return tArea.getText();
	}
}
