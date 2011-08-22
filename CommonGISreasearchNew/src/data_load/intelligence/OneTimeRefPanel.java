package data_load.intelligence;

import java.awt.Button;
import java.awt.Choice;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ResourceBundle;

import spade.lib.lang.Language;
import spade.time.Date;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 14-Jul-2004
 * Time: 17:09:12
 * Contains UI controls for describing a single column with full or partial
 * time references. Values in a column may be simple, e.g. years, or compound
 * (i.e. consisting of several components) and specified according to some
 * template.
 */
public class OneTimeRefPanel extends Panel implements ActionListener, ItemListener {
	static ResourceBundle res = Language.getTextResource("data_load.intelligence.Res");
	/**
	 * The index of the column with the time references
	 */
	protected int colIdx = -1;
	/**
	 * The name of the column with the time references
	 */
	protected String colName = null;
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
	/**
	 * The listener of the user pressing the "remove" button.
	 */
	protected ActionListener owner = null;

	/**
	 * Constructs the panel for describing one column with time references
	 * @param colName - the name of the column
	 * @param colIdx - the index of the column in the table
	 * @param owner - who will be notified when the user presses the "remove"
	 *                button
	 */
	public OneTimeRefPanel(String colName, int colIdx, ActionListener owner) {
		this.colName = colName;
		this.colIdx = colIdx;
		this.owner = owner;
		setLayout(new GridLayout(1, 4, 2, 2));
		add(new Label(colName));
		formatCh = new Choice();
		formatCh.add(res.getString("Simple_val"));
		formatCh.add(res.getString("Comp_val"));
		formatCh.select(0);
		formatCh.addItemListener(this);
		add(formatCh);
		meaningCh = new Choice();
		meaningCh.add(res.getString("year"));
		meaningCh.add(res.getString("month"));
		meaningCh.add(res.getString("day"));
		meaningCh.add(res.getString("hour"));
		meaningCh.add(res.getString("minute"));
		meaningCh.add(res.getString("second"));
		meaningCh.add(res.getString("abstract"));
		add(meaningCh);
		templateTF = new TextField(res.getString("enter_template"));
		Button b = new Button(res.getString("Remove"));
		b.setActionCommand("remove");
		b.addActionListener(this);
		Panel p = new Panel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
		p.add(b);
		add(p);
	}

	/**
	 * Reaction to pressing the "remove" button
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("remove") && owner != null) {
			owner.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "remove_" + colIdx));
		}
	}

	/**
	 * Reaction to switching from simple to compound value format and back
	 */
	@Override
	public void itemStateChanged(ItemEvent e) {
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
			for (int i = getComponentCount() - 1; i >= 0 && idx < 0; i--)
				if (getComponent(i).equals(oldC)) {
					idx = i;
				}
			if (idx < 0)
				return;
			setVisible(false);
			remove(idx);
			add(newC, idx);
			setVisible(true);
			newC.invalidate();
			invalidate();
			validate();
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
	public String check() {
		if (isFormatSimple()) //simple value
			return null;
		String template = templateTF.getText();
		if (template == null)
			return res.getString("no_template_for_column") + " \"" + colName + "\"!";
		template = template.trim();
		if (template.length() < 1 || template.equalsIgnoreCase(res.getString("enter_template")))
			return res.getString("no_template_for_column") + " \"" + colName + "\"!";
		return Date.checkTemplateValidity(template);
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
}
