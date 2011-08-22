package spade.lib.basicwin;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Label;
import java.awt.Panel;
import java.awt.ScrollPane;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.lib.lang.Language;

public class SelectDialog extends Dialog implements ActionListener {
	//static ResourceBundle res = ResourceBundle.getBundle("spade.lib.basicwin.Res", Language.lang);
	static ResourceBundle res = Language.getTextResource("spade.lib.basicwin.Res");
	protected CheckboxGroup bGroup = null;
	protected Panel panel = null;
	protected boolean cancelled = false;
	protected Vector optIds = null, optTexts = null;

	public SelectDialog(Frame owner, String title, String prompt) {
		super(owner, title, true); //the dialog is modal
		setLayout(new BorderLayout(3, 3));
		if (prompt != null) {
			Label lab = new Label(prompt, Label.CENTER);
			add(lab, BorderLayout.NORTH);
		}
		panel = new Panel(new ColumnLayout());
		add(panel, BorderLayout.CENTER);
		Panel pp = new Panel(new BorderLayout(10, 0));
		add(pp, BorderLayout.SOUTH);
		Button b = new Button("OK");
		b.addActionListener(this);
		b.setActionCommand("OK");
		Panel p = new Panel(new FlowLayout(FlowLayout.CENTER, 10, 3));
		p.add(b);
		pp.add(p, BorderLayout.CENTER);
		// following text:"Cancel"
		b = new Button(res.getString("Cancel"));
		b.addActionListener(this);
		b.setActionCommand("cancel");
		p = new Panel(new FlowLayout(FlowLayout.CENTER, 5, 3));
		p.add(b);
		pp.add(p, BorderLayout.EAST);
		bGroup = new CheckboxGroup();
	}

	public void addOption(String text, String id, boolean isSelected) {
		Checkbox rb = new Checkbox(text, isSelected, bGroup);
		panel.add(rb);
		if (optIds == null) {
			optIds = new Vector(10, 10);
		}
		if (optTexts == null) {
			optTexts = new Vector(10, 10);
		}
		if (id == null) {
			id = text;
		}
		optIds.addElement(id);
		optTexts.addElement(text);
	}

	public void addLabel(String text) {
		panel.add(new Label(text));
	}

	public void addSeparator() {
		panel.add(new Line(false));
	}

	public void addComponent(Component c) {
		if (c != null) {
			panel.add(c);
		}
	}

	public String getSelectedOptionName() {
		Checkbox rb = bGroup.getSelectedCheckbox();
		if (rb == null)
			return null;
		return rb.getLabel();
	}

	public String getSelectedOptionId() {
		if (optIds == null || optIds.size() < 1)
			return null;
		Checkbox rb = bGroup.getSelectedCheckbox();
		if (rb == null)
			return null;
		String txt = rb.getLabel();
		for (int i = 0; i < optIds.size(); i++)
			if (txt.equals(optTexts.elementAt(i)))
				return (String) optIds.elementAt(i);
		return null;
	}

	public int getSelectedOptionN() {
		if (optIds == null || optIds.size() < 1)
			return -1;
		Checkbox rb = bGroup.getSelectedCheckbox();
		if (rb == null)
			return -1;
		String txt = rb.getLabel();
		for (int i = 0; i < optIds.size(); i++)
			if (txt.equals(optTexts.elementAt(i)))
				return i;
		return -1;
	}

	@Override
	public void show() {
		pack();
		Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension sz = getSize();
		if (sz.height > d.height * 2 / 3) {
			sz.height = d.height * 2 / 3;
			remove(panel);
			ScrollPane scp = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
			scp.add(panel);
			add(scp, BorderLayout.CENTER);
			if (sz.width + 20 < d.width) {
				sz.width += 20;
			}
		}
		if (sz.width > d.width - 20) {
			sz.width = d.width - 20;
		}
		setBounds((d.width - sz.width) / 2, (d.height - sz.height) / 2, sz.width, sz.height);
		super.show();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		if (cmd.equals("cancel")) {
			cancelled = true;
			dispose();
			return;
		}
		if (cmd.equals("OK")) {
			cancelled = false;
			dispose();
			return;
		}
	}

	public boolean wasCancelled() {
		return cancelled;
	}
}
