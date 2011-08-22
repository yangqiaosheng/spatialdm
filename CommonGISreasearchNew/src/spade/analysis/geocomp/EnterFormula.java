package spade.analysis.geocomp;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Choice;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.Vector;

import spade.analysis.geocomp.trans.Calc;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.DialogContent;
import spade.lib.lang.Language;

public class EnterFormula extends Panel implements DialogContent, ActionListener {
	static ResourceBundle res = Language.getTextResource("spade.analysis.geocomp.Res");
	protected List lFields = null;
	protected Choice lFunct = null, lOper = null;
	protected TextField tFunct = null;
	protected String err = null;

	public EnterFormula(Vector operands) {
		setBackground(Color.white);
		//panel with fields
		Panel pw1 = new Panel(new BorderLayout());
		// following text:"Fields"
		pw1.add(new Label(res.getString("Fields")), "North");
		lFields = new List(5, false);
		pw1.add(lFields, "Center");
		for (int j = 0; j < operands.size(); j++) {
			lFields.add(String.valueOf(j + 1) + ") " + (String) operands.elementAt(j));
		}
		lFields.addActionListener(this);
		//panel for entering the formula
		Panel pw2 = new Panel(new BorderLayout());
		// following text:"Formula:"
		pw2.add(new Label(res.getString("Formula_")), "North");
		tFunct = new TextField(64);
		pw2.add(tFunct, "Center");
		//Panel for selection of functions and operations
		Panel pw3 = new Panel(new BorderLayout());
		Panel p = new Panel(new GridLayout(2, 1));
		// following text:"Functions:"
		p.add(new Label(res.getString("Functions_")));
		// following text:"Operations:"
		p.add(new Label(res.getString("Operations_")));
		pw3.add(p, "West");
		lFunct = new Choice();
		lFunct.addItem("ABS()");
		lFunct.addItem("ATAN()");
		lFunct.addItem("BLK()");
		lFunct.addItem("COS()");
		lFunct.addItem("DAY(,,)");
		lFunct.addItem("EXP()");
		lFunct.addItem("IF(,,)");
		lFunct.addItem("INTR(,,,)");
		lFunct.addItem("LN()");
		lFunct.addItem("LG()");
		lFunct.addItem("MAX(,)");
		lFunct.addItem("MIN(,)");
		lFunct.addItem("RAND()");
		lFunct.addItem("ROUND()");
		lFunct.addItem("SIGN()");
		lFunct.addItem("SIN()");
		lFunct.addItem("SQRT()");
		p = new Panel(new GridLayout(2, 1));
		p.add(lFunct);
		lOper = new Choice();
		// following text: "+  - arithmetic addition"
		lOper.addItem(res.getString("_arithmetic_addition"));
		// following text: "-  - arithmetic subtraction"
		lOper.addItem(res.getString("_arithmetic"));
		// following text: "*  - arithmetic multiplication"
		lOper.addItem(res.getString("_arithmetic1"));
		// following text: "/  - arithmetic division"
		lOper.addItem(res.getString("_arithmetic_division"));
		// following text: "<  - comparison less"
		lOper.addItem(res.getString("_comparison_less"));
		// following text: "<= - comparison less or equal"
		lOper.addItem(res.getString("_comparison_less_or"));
		// following text: ">  - comparison greater"
		lOper.addItem(res.getString("_comparison_greater"));
		// following text: ">= - comparison greater or equal"
		lOper.addItem(res.getString("_comparison_greater1"));
		// following text:"=  - comparison equal"
		lOper.addItem(res.getString("_comparison_equal"));
		// following text:"<> - comparison not equal"
		lOper.addItem(res.getString("_comparison_not_equal"));
		// following text:"!  - logical not"
		lOper.addItem(res.getString("_logical_not"));
		// following text:"&  - logical and"
		lOper.addItem(res.getString("_logical_and"));
		// following text:"|  - logical or"
		lOper.addItem(res.getString("_logical_or"));
		p.add(lOper);
		pw3.add(p, "Center");
		// following text:"Insert"
		Button b = new Button(res.getString("Insert"));
		b.setActionCommand("insert_function");
		b.addActionListener(this);
		p = new Panel(new GridLayout(2, 1));
		p.add(b);
		// following text:"Insert"
		b = new Button(res.getString("Insert"));
		b.setActionCommand("insert_operation");
		b.addActionListener(this);
		p.add(b);
		pw3.add(p, "East");
		setLayout(new ColumnLayout());
		// following text:"Specify the formula for computation:"
		add(new Label(res.getString("Specify_the_formula")));
		add(pw1);
		add(pw3);
		add(pw2);
	}

	protected void insertFunction() {
		int p = tFunct.getCaretPosition();
		String s = tFunct.getText().trim();
		int l = s.length();
		int p1 = p;
		if (p1 > l) {
			p1 = l;
		}
		String sins = lFunct.getSelectedItem();
		int p2 = sins.indexOf("(");
		if (p2 < 0) {
			p2 = sins.length();
		} else {
			p2++;
		}
		tFunct.setText(s.substring(0, p1) + sins + s.substring(p1, l));
		tFunct.setCaretPosition(p1 + p2);
		tFunct.requestFocus();
	}

	protected void insertOperation() {
		int p = tFunct.getCaretPosition();
		String s = tFunct.getText().trim();
		int l = s.length();
		int p1 = p;
		if (p1 > l) {
			p1 = l;
		}
		StringTokenizer st = new StringTokenizer(lOper.getSelectedItem(), " \t\r\n");
		String sins = st.nextToken();
		tFunct.setText(s.substring(0, p1) + sins + s.substring(p1, l));
		tFunct.setCaretPosition(p1 + sins.length());
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
		} else {
			String cmd = event.getActionCommand();
			if (cmd == null)
				return;
			if (cmd.equals("insert_function")) {
				insertFunction();
			} else if (cmd.equals("insert_operation")) {
				insertOperation();
			}
		}
	}

	@Override
	public boolean canClose() {
		err = null;
		Calc c = new Calc(lFields.getItemCount());
		if (!c.MakeCalcTrack(tFunct.getText())) {
			err = (res.getString("Invalid_formula_"));
			return false;
		}
		return true;
	}

	@Override
	public String getErrorMessage() {
		return err;
	}

	public String getFormula() {
		return tFunct.getText();
	}
}
