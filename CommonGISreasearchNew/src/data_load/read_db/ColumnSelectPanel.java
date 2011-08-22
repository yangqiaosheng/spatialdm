package data_load.read_db;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.lib.basicwin.DialogContent;
import spade.lib.lang.Language;
import spade.lib.util.StringUtil;

public class ColumnSelectPanel extends Panel implements ActionListener, ItemListener, DialogContent {
	static ResourceBundle res = Language.getTextResource("data_load.read_db.Res");
	protected Vector columns = null;
	protected List colList = null;
	protected TextField idTF = null, nameTF = null, xTF = null, yTF = null;
	protected Checkbox coordCB = null;
	protected String err = null;

	public ColumnSelectPanel() {
	}

	public ColumnSelectPanel(Vector columns) {
		init(columns);
	}

	public void init(Vector columns) {
		if (columns == null || columns.size() < 1)
			return;
		this.columns = columns;
		colList = new List(10, true);
		for (int i = 0; i < columns.size(); i++) {
			colList.add((String) columns.elementAt(i));
		}
		Panel p = new Panel(new BorderLayout());
		p.add(colList, "Center");
		//following text: "Select columns to load:"
		p.add(new Label(res.getString("Select_columns_to")), "North");
		//following text:"Select all"
		Button b = new Button(res.getString("Select_all"));
		b.addActionListener(this);
		b.setActionCommand("select_all");
		Panel pp = new Panel(new FlowLayout(FlowLayout.CENTER, 20, 3));
		pp.add(b);
		//following text:"Deselect all"
		b = new Button(res.getString("Deselect_all"));
		b.addActionListener(this);
		b.setActionCommand("deselect_all");
		pp.add(b);
		p.add(pp, "South");
		setLayout(new BorderLayout());
		add(p, "Center");
		p = new Panel(new GridLayout(9, 1));
		//following text:"Identifiers of objects are in column:"
		p.add(new Label(res.getString("Identifiers_of")));
		idTF = new TextField(20);
		p.add(idTF);
		//following text:"Names of objects are in column:"
		p.add(new Label(res.getString("Names_of_objects_are")));
		nameTF = new TextField(20);
		p.add(nameTF);
		//following text:"The table contains coordinates"
		coordCB = new Checkbox(res.getString("The_table_contains"), false);
		coordCB.addItemListener(this);
		p.add(coordCB);
		//following text:"X-coordinates are in column:"
		p.add(new Label(res.getString("Xcoordinates_are_in")));
		xTF = new TextField("X", 20);
		xTF.setEnabled(false);
		p.add(xTF);
		//following text:"Y-coordinates are in column:"
		p.add(new Label(res.getString("Ycoordinates_are_in")));
		yTF = new TextField("Y", 20);
		yTF.setEnabled(false);
		p.add(yTF);
		add(p, "East");
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		if (cmd == null)
			return;
		if (cmd.equals("select_all")) {
			for (int i = 0; i < colList.getItemCount(); i++) {
				colList.select(i);
			}
		} else if (cmd.equals("deselect_all")) {
			for (int i = 0; i < colList.getItemCount(); i++) {
				colList.deselect(i);
			}
		}
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if (coordCB != null && xTF != null && yTF != null) {
			xTF.setEnabled(coordCB.getState());
			yTF.setEnabled(coordCB.getState());
		}
	}

	@Override
	public boolean canClose() {
		err = null;
		int selInd[] = colList.getSelectedIndexes();
		if (selInd == null || selInd.length < 1) {
			//following text:"No columns selected!"
			err = res.getString("No_columns_selected_");
			return false;
		}
		String s = getIdColName();
		if (s != null)
			if (!StringUtil.isStringInVectorIgnoreCase(s, columns)) {
				//following text:"Invalid name of column with identifiers: "
				err = res.getString("Invalid_name_of") + s;
				return false;
			}
		s = getNameColName();
		if (s != null)
			if (!StringUtil.isStringInVectorIgnoreCase(s, columns)) {
				//following text:"Invalid name of column with names: "
				err = res.getString("Invalid_name_of1") + s;
				return false;
			}
		if (coordCB != null && coordCB.getState()) {
			s = getXColName();
			if (s == null) {
				//following text:"The column with X-coordinates is not specified!"
				err = res.getString("The_column_with_X");
				return false;
			}
			if (!StringUtil.isStringInVectorIgnoreCase(s, columns)) {
				//following text:"Invalid name of column with X-coordinates: "
				err = res.getString("Invalid_name_of2") + s;
				return false;
			}
			s = getYColName();
			if (s == null) {
				//following text:"The column with Y-coordinates is not specified!"
				err = res.getString("The_column_with_Y");
				return false;
			}
			if (!StringUtil.isStringInVectorIgnoreCase(s, columns)) {
				//following text:"Invalid name of column with Y-coordinates: "
				err = res.getString("Invalid_name_of3") + s;
				return false;
			}
		}
		return true;
	}

	@Override
	public String getErrorMessage() {
		return err;
	}

	protected String getTextFromField(TextField tf) {
		if (tf == null)
			return null;
		if (!tf.isEnabled())
			return null;
		String s = tf.getText();
		if (s == null)
			return null;
		s = s.trim();
		if (s.length() < 1)
			return null;
		if (!StringUtil.isStringInVectorIgnoreCase(s, columns)) {
			//may be, this is a column number (index)?
			try {
				int n = Integer.valueOf(s).intValue() - 1;
				if (n >= 0 && n < columns.size()) {
					s = (String) columns.elementAt(n);
					tf.setText(s);
				}
			} catch (NumberFormatException nfe) {
			}
		}
		return s;
	}

	public String getIdColName() {
		return getTextFromField(idTF);
	}

	public String getNameColName() {
		return getTextFromField(nameTF);
	}

	public String getXColName() {
		if (xTF != null)
			return getTextFromField(xTF);
		return null;
	}

	public String getYColName() {
		if (yTF != null)
			return getTextFromField(yTF);
		return null;
	}

	public Vector getSelectedColumns() {
		int ind[] = colList.getSelectedIndexes();
		if (ind == null)
			return null;
		Vector v = new Vector(ind.length, 5);
		for (int element : ind) {
			v.addElement(columns.elementAt(element));
		}
		return v;
	}
}
