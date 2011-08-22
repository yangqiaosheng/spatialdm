package data_load.connect_server;

import java.awt.Checkbox;
import java.awt.Choice;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ResourceBundle;

import spade.lib.basicwin.DialogContent;
import spade.lib.lang.Language;
import spade.vis.spec.DataSourceSpec;

/**
* For an ASCII file, asks the user about the delimiter and the numbers of the
* rows with field names and types.
*/
public class ASCIISpecPanel extends Panel implements DialogContent, ItemListener, ActionListener {
	static ResourceBundle res = Language.getTextResource("data_load.connect_server.Res");
	protected DataSourceSpec spec = null;
	protected String err = null;
	protected Choice choice = null;
	protected Label specLabel = null;
	protected TextField delimTF = null, fieldNameRowTF = null, fieldTypeRowTF = null;
	protected Checkbox cbFieldNamesRow = null, cbFieldTypesRow = null;
	protected Label errLabel = null;

	public ASCIISpecPanel(DataSourceSpec dss) {
		spec = dss;
		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		c.weightx = 1.0f;
		c.weighty = 1.0f;
		c.fill = GridBagConstraints.HORIZONTAL;
		setLayout(gridbag);
		//followingt text:"Specify information for the ASCII file"
		Label l = new Label(res.getString("Specify_information"));
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(l, c);
		add(l);
		l = new Label(spec.source, Label.CENTER);
		gridbag.setConstraints(l, c);
		add(l);
		//followingt text:"Delimiter:"
		l = new Label(res.getString("Delimiter_"));
		c.gridwidth = 1;
		gridbag.setConstraints(l, c);
		add(l);
		choice = new Choice();
		//followingt text:"comma (,)"
		choice.addItem(res.getString("comma_"));
		//followingt text:"semicolon (;)"
		choice.addItem(res.getString("semicolon_"));
		//followingt text:"TAB character"
		choice.addItem(res.getString("TAB_character"));
		//followingt text:"space"
		choice.addItem(res.getString("space"));
		//followingt text:"other character"
		choice.addItem(res.getString("other_character"));
		choice.addItemListener(this);
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(choice, c);
		add(choice);
		//followingt text:"specify:"
		specLabel = new Label(res.getString("specify_"));
		c.gridwidth = 1;
		gridbag.setConstraints(specLabel, c);
		add(specLabel);
		delimTF = new TextField(2);
		delimTF.addActionListener(this);
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(delimTF, c);
		add(delimTF);
		//followingt text:"Take field names from row N"
		cbFieldNamesRow = new Checkbox(res.getString("Take_field_names_from"));
		cbFieldNamesRow.addItemListener(this);
		c.gridwidth = 1;
		gridbag.setConstraints(cbFieldNamesRow, c);
		add(cbFieldNamesRow);
		fieldNameRowTF = new TextField(2);
		fieldNameRowTF.addActionListener(this);
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(fieldNameRowTF, c);
		add(fieldNameRowTF);
		//followingt text:"Take field types from row N"
		cbFieldTypesRow = new Checkbox(res.getString("Take_field_types_from"));
		cbFieldTypesRow.addItemListener(this);
		c.gridwidth = 1;
		gridbag.setConstraints(cbFieldTypesRow, c);
		add(cbFieldTypesRow);
		fieldTypeRowTF = new TextField(2);
		fieldTypeRowTF.addActionListener(this);
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(fieldTypeRowTF, c);
		add(fieldTypeRowTF);
		errLabel = new Label("");
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(errLabel, c);
		add(errLabel);
		setDelimiterFields();
		setFieldNameFields();
		setFieldTypeFields();
	}

	protected void setDelimiterFields() {
		if (spec.delimiter == null || spec.delimiter.startsWith(",")) {
			choice.select(0);
		} else if (spec.delimiter.startsWith(";")) {
			choice.select(1);
		} else if (spec.delimiter.startsWith("\t")) {
			choice.select(2);
		} else if (spec.delimiter.startsWith(" ")) {
			choice.select(3);
		} else {
			choice.select(4);
		}
		if (choice.getSelectedIndex() > 3) {
			specLabel.setForeground(getForeground());
			delimTF.setEnabled(true);
			delimTF.setText(spec.delimiter);
		} else {
			delimTF.setEnabled(false);
			delimTF.setText("");
			specLabel.setForeground(Color.gray);
		}
	}

	protected void setFieldNameFields() {
		if (spec.nRowWithFieldNames >= 0) {
			cbFieldNamesRow.setState(true);
			fieldNameRowTF.setEnabled(true);
			fieldNameRowTF.setText(String.valueOf(spec.nRowWithFieldNames + 1));
		} else {
			cbFieldNamesRow.setState(false);
			fieldNameRowTF.setEnabled(false);
			fieldNameRowTF.setText("");
		}
	}

	protected void setFieldTypeFields() {
		if (spec.nRowWithFieldTypes >= 0) {
			cbFieldTypesRow.setState(true);
			fieldTypeRowTF.setEnabled(true);
			fieldTypeRowTF.setText(String.valueOf(spec.nRowWithFieldTypes + 1));
		} else {
			cbFieldTypesRow.setState(false);
			fieldTypeRowTF.setEnabled(false);
			fieldTypeRowTF.setText("");
		}
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		eraseErrorMessage();
		if (e.getSource().equals(choice)) {
			switch (choice.getSelectedIndex()) {
			case 0:
				spec.delimiter = ",";
				break;
			case 1:
				spec.delimiter = ";";
				break;
			case 2:
				spec.delimiter = "\t";
				break;
			case 3:
				spec.delimiter = " ";
				break;
			case 4:
				spec.delimiter = null;
				break;
			}
			if (choice.getSelectedIndex() == 4) {
				specLabel.setForeground(getForeground());
				delimTF.setEnabled(true);
			} else {
				delimTF.setEnabled(false);
				delimTF.setText("");
				specLabel.setForeground(Color.gray);
			}
		} else if (e.getSource().equals(cbFieldNamesRow)) {
			if (cbFieldNamesRow.getState()) {
				fieldNameRowTF.setEnabled(true);
			} else {
				fieldNameRowTF.setEnabled(false);
				fieldNameRowTF.setText("");
				spec.nRowWithFieldNames = -1;
			}
		} else if (e.getSource().equals(cbFieldTypesRow)) {
			if (cbFieldTypesRow.getState()) {
				fieldTypeRowTF.setEnabled(true);
			} else {
				fieldTypeRowTF.setEnabled(false);
				fieldTypeRowTF.setText("");
				spec.nRowWithFieldTypes = -1;
			}
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		eraseErrorMessage();
		String str = null;
		if (e.getSource() instanceof TextField) {
			TextField tf = (TextField) e.getSource();
			str = tf.getText();
			if (str != null) {
				str = str.trim();
				if (str.length() < 1) {
					str = null;
				}
			}
			if (str == null) {
				tf.setText("");
				return;
			}
		}
		if (e.getSource().equals(delimTF)) {
			if (str.length() > 1) {
				//followingt text:"The delimiter must be a single character!"
				showErrorMessage(res.getString("The_delimiter_must_be"));
				delimTF.setText(str.substring(0, 1));
				return;
			}
			spec.delimiter = str;
			setDelimiterFields();
		} else if (e.getSource().equals(fieldNameRowTF) || e.getSource().equals(fieldTypeRowTF)) {
			boolean isNameRow = e.getSource().equals(fieldNameRowTF);
			int n = 0;
			try {
				n = Integer.parseInt(str);
			} catch (NumberFormatException nfe) {
			}
			if (n < 1) {
				//followingt text:"Illegal row number for field "
				showErrorMessage(res.getString("Illegal_row_number") +
				//followingt text:"names!"
				//followingt text:"types!"
						((isNameRow) ? res.getString("names_") : res.getString("types_")));
				if (isNameRow) {
					fieldNameRowTF.setText("");
				} else {
					fieldTypeRowTF.setText("");
				}
			} else if (isNameRow) {
				spec.nRowWithFieldNames = n - 1;
			} else {
				spec.nRowWithFieldTypes = n - 1;
			}
		}
	}

	protected void eraseErrorMessage() {
		errLabel.setText("");
		errLabel.setForeground(getForeground());
		errLabel.setBackground(getBackground());
	}

	protected void showErrorMessage(String message) {
		errLabel.setBackground(Color.red);
		errLabel.setForeground(Color.white);
		errLabel.setText(message);
	}

	@Override
	public boolean canClose() {
		switch (choice.getSelectedIndex()) {
		case 0:
			spec.delimiter = ",";
			break;
		case 1:
			spec.delimiter = ";";
			break;
		case 2:
			spec.delimiter = "\t";
			break;
		case 3:
			spec.delimiter = " ";
			break;
		case 4:
			String str = delimTF.getText();
			if (str != null) {
				str = str.trim();
				if (str.length() < 1) {
					str = null;
				}
			}
			if (str == null) {
				//followingt text:"No delimiter specified!"
				err = res.getString("No_delimiter");
				return false;
			}
			if (str.length() > 1) {
				//followingt text:"The delimiter must be a single character!"
				err = res.getString("The_delimiter_must_be");
				return false;
			}
			spec.delimiter = str;
			break;
		}
		if (cbFieldNamesRow.getState()) {
			String str = fieldNameRowTF.getText();
			if (str != null) {
				str = str.trim();
				if (str.length() < 1) {
					str = null;
				}
			}
			if (str == null) {
				//followingt text:"The number of the row with field names is not specified!"
				err = res.getString("The_number_of_the_row");
				return false;
			}
			int n = 0;
			try {
				n = Integer.parseInt(str);
			} catch (NumberFormatException nfe) {
			}
			if (n < 1) {
				//followingt text:"Illegal row number for field names!"
				err = res.getString("Illegal_row_number1");
				return false;
			}
			spec.nRowWithFieldNames = n - 1;
		} else {
			spec.nRowWithFieldNames = -1;
		}
		if (cbFieldTypesRow.getState()) {
			String str = fieldTypeRowTF.getText();
			if (str != null) {
				str = str.trim();
				if (str.length() < 1) {
					str = null;
				}
			}
			if (str == null) {
				//followingt text:"The number of the row with field types is not specified!"
				err = res.getString("The_number_of_the_row1");
				return false;
			}
			int n = 0;
			try {
				n = Integer.parseInt(str);
			} catch (NumberFormatException nfe) {
			}
			if (n < 1) {
				//followingt text:"Illegal row number for field types!"
				err = res.getString("Illegal_row_number2");
				return false;
			}
			spec.nRowWithFieldTypes = n - 1;
		} else {
			spec.nRowWithFieldTypes = -1;
		}
		return true;
	}

	@Override
	public String getErrorMessage() {
		return err;
	}
}
