package spade.time.ui;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Choice;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.List;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Vector;

import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.DialogContent;
import spade.lib.basicwin.NotificationLine;
import spade.lib.basicwin.NotificationLineManager;
import spade.lib.basicwin.TextCanvas;
import spade.time.Date;
import spade.time.TimeCount;
import spade.time.TimeMoment;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 03-Jan-2007
 * Time: 16:08:34
 * For dates/times specified as strings, asks the user to provide the
 * information necessary for transforming the strings into time moments (dates
 * or time counts).
 */
public class TimeFormatUI extends Panel implements ItemListener, ActionListener, NotificationLineManager, DialogContent {
	/**
	 * souList contains the source strings; resList will show the transformed
	 * values
	 */
	protected List souList = null, resList = null;
	/**
	 * Used to specify the format of time references: simple or compound
	 */
	protected Choice formatCh = null;
	/**
	 * Used to specify the meaning of the time reference, e.g. year, month, day, etc.
	 */
	protected Choice meaningCh = null;
	/**
	 * Usef for entering the template describing the values in the column.
	 */
	protected TextField templateTF = null;

	protected Panel cPan = null;
	/**
	 * The notification line for messages
	 */
	protected NotificationLine lStatus = null;
	/**
	 * Explains why cannot be closed
	 */
	protected String err = null;

	/**
	 * The vector sampleValues should contain samples of the string values to be
	 * transformed into time moments
	 */
	public TimeFormatUI(String prompt, Vector sampleValues) {
		setLayout(new BorderLayout());
		if (prompt == null) {
			prompt = "Specify information for interpreting the string values as " + "dates and/or times";
		}
		TextCanvas tc = new TextCanvas();
		tc.addTextLine(prompt);
		add(tc, BorderLayout.NORTH);
		souList = new List(Math.min(10, sampleValues.size()));
		resList = new List(Math.min(10, sampleValues.size()));
		for (int i = 0; i < sampleValues.size(); i++) {
			souList.add(sampleValues.elementAt(i).toString());
		}
		Panel p = new Panel(new GridLayout(1, 2, 10, 0));
		p.add(souList);
		p.add(resList);
		add(p, BorderLayout.CENTER);
		cPan = new Panel(new GridLayout(1, 2, 10, 0));
		formatCh = new Choice();
		formatCh.add("Simple value");
		formatCh.add("Compound value");
		formatCh.select(0);
		formatCh.addItemListener(this);
		cPan.add(formatCh);
		meaningCh = new Choice();
		meaningCh.add("year");
		meaningCh.add("month");
		meaningCh.add("day");
		meaningCh.add("hour");
		meaningCh.add("minute");
		meaningCh.add("second");
		meaningCh.add("abstract");
		cPan.add(meaningCh);
		Button b = new Button("Test");
		b.setActionCommand("test");
		b.addActionListener(this);
		p = new Panel(new FlowLayout(FlowLayout.CENTER, 5, 0));
		p.add(b);
		Panel pp = new Panel(new BorderLayout());
		pp.add(cPan, BorderLayout.CENTER);
		pp.add(p, BorderLayout.EAST);
		p = new Panel(new ColumnLayout());
		p.add(pp);
		tc = new TextCanvas();
		tc.addTextLine("Examples of compound values: 21/11; 21/11/2006; 11/2006; " + "21/11/2006 18:05; 18:05:48 (the delimiters are irrelevant).");
		tc.addTextLine("Compound values are interpreted according to an apropriate " + "template, such as dd/mm/yyyy hh:tt:ss.");
		tc.addTextLine("The symbols { y m d h t s } indicate date/time " + "components ('t' means minutes) and symbols { - . : , / ; } or space " + "are used as separators.");
		p.add(tc);
		add(p, BorderLayout.SOUTH);
		templateTF = new TextField("enter the template");
		templateTF.addActionListener(this);
	}

	@Override
	public void setNotificationLine(NotificationLine infoLine) {
		lStatus = infoLine;
	}

	/**
	* Shows the given message. If "trouble" is true, then this
	* is an error message.
	*/
	public void showMessage(String msg, boolean trouble) {
		if (lStatus != null) {
			lStatus.showMessage(msg, trouble);
		}
		if (trouble) {
			System.out.println("ERROR: " + msg);
		}
	}

	/**
	* Clears the status line
	*/
	public void clearStatusLine() {
		if (lStatus != null) {
			lStatus.showMessage(null, false);
		}
		err = null;
	}

	/**
	 * Reaction to switching from simple to compound value format and back
	 */
	@Override
	public void itemStateChanged(ItemEvent e) {
		clearStatusLine();
		if (e.getSource().equals(formatCh)) {
			Component oldC = null, newC = null;
			if (formatCh.getSelectedIndex() == 0) { //simple value
				oldC = templateTF;
				newC = meaningCh;
			} else {
				oldC = meaningCh;
				newC = templateTF;
			}
			int idx = -1;
			for (int i = cPan.getComponentCount() - 1; i >= 0 && idx < 0; i--)
				if (cPan.getComponent(i).equals(oldC)) {
					idx = i;
				}
			if (idx < 0)
				return;
			cPan.setVisible(false);
			cPan.remove(idx);
			cPan.add(newC, idx);
			cPan.setVisible(true);
			newC.invalidate();
			cPan.invalidate();
			cPan.validate();
		}
	}

	/**
	 * Tries to transform the strings into dates
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		clearStatusLine();
		String cmd = e.getActionCommand();
		if (e.getSource().equals(templateTF)) {
			cmd = "test";
		}
		if (cmd == null)
			return;
		if (cmd.equals("test")) {
			err = checkScheme();
			if (err != null) {
				showMessage(err, true);
				return;
			}
			int nFailed = checkTransformation();
			if (nFailed < 1) {
				showMessage("Successfully transfromed all the values!", false);
			} else if (nFailed == souList.getItemCount()) {
				showMessage("Could not transform any of the values!", true);
			} else {
				showMessage("Could not transform " + nFailed + " of " + souList.getItemCount() + " values!", true);
			}
		}
	}

	/**
	 * Informs whether the user has specified the time reference format as simple
	 */
	public boolean isFormatSimple() {
		return formatCh.getSelectedIndex() == 0;
	}

	/**
	 * Check correctness of the template specification (in a case of a compound
	 * value format). If error, returns the error message. If OK, returns null.
	 */
	public String checkScheme() {
		if (isFormatSimple()) //simple value
			return null;
		String str = getScheme();
		if (str == null || str.length() < 1)
			return "No template given!";
		if (str.equalsIgnoreCase("enter the template"))
			return "No template given!";
		String symbols = "ymdhts-.:,/; ";
		for (int i = 0; i < str.length(); i++) {
			if (symbols.indexOf(str.charAt(i)) < 0) {
				err = "Invalid symbol [" + String.valueOf(str.charAt(i)) + "] in scheme [" + str + "]!";
				return err;
			}
		}
		return Date.checkTemplateValidity(str);
	}

	/**
	 * Checks if the given sample values can be transformed into time moments.
	 * Returns the number of values which could not be transformed.
	 */
	public int checkTransformation() {
		clearStatusLine();
		resList.removeAll();
		boolean simple = isFormatSimple();
		String scheme = (simple) ? null : getScheme();
		TimeMoment time = null;
		if (simple) {
			time = new TimeCount();
		} else {
			time = new Date();
			((Date) time).setDateScheme(scheme);
		}
		int nWrong = 0;
		for (int i = 0; i < souList.getItemCount(); i++) {
			String str = souList.getItem(i);
			if (time.setMoment(str)) {
				resList.add(time.toString());
			} else {
				++nWrong;
				resList.add("!--> failed to transform [" + str + "]!");
			}
		}
		return nWrong;
	}

	/**
	 * Returns the user-specified template or selected meaning of the time
	 * references. The meaning is returned as a string consisting of a single
	 * symbol, e.g. "y" for year, "m" for month, etc.
	 */
	public String getScheme() {
		if (isFormatSimple()) {
			switch (meaningCh.getSelectedIndex()) {
			case 0:
				return "y";
			case 1:
				return "m";
			case 2:
				return "d";
			case 3:
				return "h";
			case 4:
				return "t";
			case 5:
				return "s";
			default:
				return "a";
			}
		}
		String template = templateTF.getText();
		if (template != null) {
			template = template.trim().toLowerCase();
		}
		return template;
	}

	@Override
	public boolean canClose() {
		err = checkScheme();
		if (err != null)
			return false;
		int nFailed = checkTransformation();
		if (nFailed == 0)
			return true;
		if (nFailed == souList.getItemCount()) {
			err = "Could not transform any of the values!";
			return false;
		}
		if (nFailed > souList.getItemCount() / 4) {
			err = "Could not transform " + nFailed + " of " + souList.getItemCount() + " values!";
			return false;
		}
		return true;
	}

	@Override
	public String getErrorMessage() {
		return err;
	}

	/**
	 * Returns the resulting time moments obtained from the dates
	 */
	public Vector getTimes() {
		Vector times = new Vector(souList.getItemCount(), 100);
		boolean simple = isFormatSimple();
		String scheme = (simple) ? null : getScheme();
		TimeMoment time = null;
		if (simple) {
			time = new TimeCount();
		} else {
			time = new Date();
			((Date) time).setDateScheme(scheme);
		}
		int nWrong = 0;
		for (int i = 0; i < souList.getItemCount(); i++) {
			String str = souList.getItem(i);
			if (time.setMoment(str)) {
				times.addElement(time.getCopy());
			} else {
				++nWrong;
				times.addElement(null);
			}
		}
		if (nWrong == souList.getItemCount())
			return null;
		return times;
	}
}
