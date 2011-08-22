package spade.analysis.geocomp;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.geocomp.trans.Calc;
import spade.lib.basicwin.DialogContent;
import spade.lib.lang.Language;

/**
* A user interface component for entering logical conditions for querying
* raster data
*/
public class EnterCondition extends Panel implements DialogContent, ActionListener {
	static ResourceBundle res = Language.getTextResource("spade.analysis.geocomp.Res");
	protected List lFields = null;
	protected TextField tFunct = null;
	protected String err = null;

	public EnterCondition(Vector operands) {
		setBackground(Color.white);
		setLayout(new BorderLayout());
		//panel with fields
		Panel p = new Panel(new BorderLayout());
		Panel pp = new Panel(new GridLayout(2, 1));
		p.add(pp, "North");
		// following text:"Construct a logical expression"
		pp.add(new Label(res.getString("Construct_a_logical"), Label.CENTER));
		// following text:"Fields:"
		pp.add(new Label(res.getString("Fields_")));
		int nfields = operands.size();
		if (nfields > 5) {
			nfields = 5;
		} else if (nfields < 2) {
			nfields = 2;
		}
		lFields = new List(nfields, false);
		p.add(lFields, "Center");
		for (int j = 0; j < operands.size(); j++) {
			lFields.add(String.valueOf(j + 1) + ") " + (String) operands.elementAt(j));
		}
		lFields.addActionListener(this);
		add(p, "North");
		//panel for entering the formula
		p = new Panel(new BorderLayout());
		// following text:"Formula:"
		p.add(new Label(res.getString("Formula_")), "North");
		tFunct = new TextField(64);
		p.add(tFunct, "Center");
		add(p, "South");
		Panel pop = new Panel(new GridLayout(3, 1));
		add(pop, "Center");
		p = new Panel(new BorderLayout());
		// following text:"Comparison operations:"
		p.add(new Label(res.getString("Comparison_operations")), "North");
		pp = new Panel(new GridLayout(1, 6));
		Button b = new Button("=");
		b.addActionListener(this);
		pp.add(b);
		b = new Button("<");
		b.addActionListener(this);
		pp.add(b);
		b = new Button("<=");
		b.addActionListener(this);
		pp.add(b);
		b = new Button(">");
		b.addActionListener(this);
		pp.add(b);
		b = new Button(">=");
		b.addActionListener(this);
		pp.add(b);
		b = new Button("<>");
		b.addActionListener(this);
		pp.add(b);
		p.add(pp, "South");
		pop.add(p);
		p = new Panel(new BorderLayout());
		// following text:"Logical operations:"
		p.add(new Label(res.getString("Logical_operations_")), "North");
		pp = new Panel(new GridLayout(1, 3));
		// following text:
		b = new Button(res.getString("AND_"));
		b.setActionCommand("&");
		b.addActionListener(this);
		pp.add(b);
		// following text:
		b = new Button(res.getString("OR_"));
		b.setActionCommand("|");
		b.addActionListener(this);
		pp.add(b);
		// following text:
		b = new Button(res.getString("NOT_"));
		b.setActionCommand("!");
		b.addActionListener(this);
		pp.add(b);
		p.add(pp, "South");
		pop.add(p);
		p = new Panel(new BorderLayout());
		// following text:"Arithmetic operations:"
		p.add(new Label(res.getString("Arithmetic_operations")), "North");
		pp = new Panel(new GridLayout(1, 4));
		// following text:"+ (plus)"
		b = new Button(res.getString("_plus_"));
		b.setActionCommand("+");
		b.addActionListener(this);
		pp.add(b);
		// following text:"- (minus)"
		b = new Button(res.getString("_minus_"));
		b.setActionCommand("-");
		b.addActionListener(this);
		pp.add(b);
		// following text:"* (multiply)"
		b = new Button(res.getString("_multiply_"));
		b.setActionCommand("*");
		b.addActionListener(this);
		pp.add(b);
		// following text:"/ (divide)"
		b = new Button(res.getString("_divide_"));
		b.setActionCommand("/");
		b.addActionListener(this);
		pp.add(b);
		p.add(pp, "South");
		pop.add(p);
	}

	protected void insertOperation(String op) {
		int p = tFunct.getCaretPosition();
		String s = tFunct.getText().trim();
		int l = s.length();
		int p1 = p;
		if (p1 > l) {
			p1 = l;
		}
		tFunct.setText(s.substring(0, p1) + op + s.substring(p1, l));
		tFunct.setCaretPosition(p1 + op.length());
		tFunct.requestFocus();
	}

	protected void insertField() {
		int ind = lFields.getSelectedIndex();
		if (ind < 0)
			return;
		int p = tFunct.getCaretPosition();
		String s = tFunct.getText().trim();
		int l = s.length();
		int p1 = p;
		if (p1 > l) {
			p1 = l;
		}
		String sins = "$" + String.valueOf(ind + 1);
		tFunct.setText(s.substring(0, p1) + sins + s.substring(p1, l));
		tFunct.setCaretPosition(p1 + sins.length());
		tFunct.requestFocus();
	}

	@Override
	public void actionPerformed(ActionEvent event) {
		if (event.getSource().equals(lFields)) {
			insertField();
		} else if (event.getSource() instanceof Button) {
			insertOperation(event.getActionCommand());
		}
	}

	@Override
	public boolean canClose() {
		err = null;
		Calc c = new Calc(lFields.getItemCount());
		if (!c.MakeCalcTrack(getFormula())) {
			//following text: Invalid formula!
			err = res.getString("Invalid_formula_");
			return false;
		}
		return true;
	}

	@Override
	public String getErrorMessage() {
		return err;
	}

	public String getFormula() {
		return "IF(" + getCondition() + ",1,0)";
	}

	public String getCondition() {
		return tFunct.getText().trim();
	}
}
