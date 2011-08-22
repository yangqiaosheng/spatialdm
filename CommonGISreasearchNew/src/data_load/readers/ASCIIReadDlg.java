package data_load.readers;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.lib.basicwin.NotificationLine;
import spade.lib.basicwin.TextCanvas;
import spade.lib.lang.Language;
import spade.lib.util.StringUtil;
import spade.vis.spec.DataSourceSpec;

public class ASCIIReadDlg extends Dialog implements ActionListener, ItemListener {
	static ResourceBundle res = Language.getTextResource("data_load.readers.Res");
	protected TextField tfSep = null, tfNTypes = null, tfNNames = null, tfIdField = null, tfNameField = null, tfXField = null, tfYField = null, tfRadField = null;
	protected Checkbox cbNTypes = null, cbNNames = null, cbCoord = null, cbRad = null, cbMultiLineObj = null;
	public boolean cancelled = false;
	protected NotificationLine lStatus = null;
	protected DataSourceSpec spec = null;

	public ASCIIReadDlg(Frame owner, DataSourceSpec spec, Vector dataSample) {
		//following text:"Reading data from ASCII format"
		super(owner, res.getString("Reading_data_from"), true);
		this.spec = spec;
		setLayout(new BorderLayout());
		//trying to read data from the text file or clipboard
		Panel p = new Panel(new GridLayout(2, 1));
		lStatus = new NotificationLine("");
		p.add(lStatus);
		//following text:"Data sample:"
		p.add(new Label(res.getString("Data_sample_")));
		add(p, BorderLayout.NORTH);
		TextArea tar = new TextArea(10, 40);
		add(tar, BorderLayout.CENTER);
		for (int i = 0; i < dataSample.size(); i++) {
			String str = (String) dataSample.elementAt(i);
			if (str.length() > 1000) {
				str = str.substring(0, 997) + "...";
			}
			tar.append(str + "\n");
		}
		p = new Panel();
		GridBagLayout gridbag = new GridBagLayout();
		p.setLayout(gridbag);
		GridBagConstraints c = new GridBagConstraints();
		c.weightx = 1.0f;
		c.weighty = 1.0f;
		c.fill = GridBagConstraints.HORIZONTAL;
		Label l = new Label(res.getString("Value_separator_"));
		c.gridwidth = 3;
		gridbag.setConstraints(l, c);
		p.add(l);
		tfSep = new TextField(3);
		String delim = spec.getDelimiter();
		if (delim == null) {
			delim = ",";
			String str = (dataSample.size() > 1) ? (String) dataSample.elementAt(1) : (String) dataSample.elementAt(0);
			int n = StringUtil.countOccurrences(',', str), n1 = StringUtil.countOccurrences(';', str);
			if (n1 > n) {
				delim = ";";
				n = n1;
			}
			n1 = StringUtil.countOccurrences('\t', str);
			if (n1 > n) {
				delim = "\t";
				n = n1;
			}
		}
		if (delim.charAt(0) == '\t') {
			tfSep.setText("TAB");
		} else {
			tfSep.setText(delim);
		}
		c.gridwidth = 1;
		gridbag.setConstraints(tfSep, c);
		p.add(tfSep);
		l = new Label("");
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(l, c);
		p.add(l);
		cbNTypes = new Checkbox(res.getString("Take_field_types_from"));
		cbNTypes.addItemListener(this);
		c.gridwidth = 3;
		gridbag.setConstraints(cbNTypes, c);
		p.add(cbNTypes);
		int n = spec.nRowWithFieldTypes;
		cbNTypes.setState(n >= 0);
		tfNTypes = new TextField(2);
		if (n < 0) {
			tfNTypes.setEnabled(false);
		} else {
			tfNTypes.setText(String.valueOf(n + 1));
		}
		c.gridwidth = 1;
		gridbag.setConstraints(tfNTypes, c);
		p.add(tfNTypes);
		//following text:"(N - numeric, C - character, L - logical, D - date)"
		l = new Label(res.getString("_N_numeric_C"));
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(l, c);
		p.add(l);
		cbNNames = new Checkbox(res.getString("Take_field_names_from"));
		cbNNames.addItemListener(this);
		c.gridwidth = 3;
		gridbag.setConstraints(cbNNames, c);
		p.add(cbNNames);
		n = spec.nRowWithFieldNames;
		cbNNames.setState(n >= 0);
		tfNNames = new TextField(2);
		if (n < 0) {
			tfNNames.setEnabled(false);
		} else {
			tfNNames.setText(String.valueOf(n + 1));
		}
		c.gridwidth = 1;
		gridbag.setConstraints(tfNNames, c);
		p.add(tfNNames);
		l = new Label("");
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(l, c);
		p.add(l);
		l = new Label(res.getString("Identifiers_are_in"));
		c.gridwidth = 3;
		gridbag.setConstraints(l, c);
		p.add(l);
		String str = spec.idFieldName;
		if (str == null) {
			n = spec.idFieldN;
			if (n < 0) {
				str = "";
			} else {
				str = String.valueOf(n + 1);
			}
		}
		tfIdField = new TextField(str, 6);
		c.gridwidth = 1;
		gridbag.setConstraints(tfIdField, c);
		p.add(tfIdField);
		//following text:"(enter the name or the number of the field)"
		l = new Label(res.getString("_enter_the_name_or"));
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(l, c);
		p.add(l);
		Dimension scrSize = getToolkit().getScreenSize();
		TextCanvas tc = new TextCanvas();
		//following text:"If there is no field with identifiers,"
		//following text:"the system will produce default identifiers from record numbers"
		tc.addTextLine(res.getString("If_there_is_no_field") + res.getString("the_system_will"));
		tc.setBackground(new Color(255, 255, 192));
		tc.setPreferredSize(scrSize.width / 2, 20);
		gridbag.setConstraints(tc, c);
		p.add(tc);
		cbMultiLineObj = new Checkbox("Multiple lines may describe a single object");
		gridbag.setConstraints(cbMultiLineObj, c);
		p.add(cbMultiLineObj);
		tc = new TextCanvas();
		tc.addTextLine("For instance, these may be points of a line or trajectory.");
		tc.addTextLine("In such a case, the identifiers in the rows describing " + "one and the same  object must coincide.");
		tc.setBackground(new Color(192, 255, 192));
		tc.setPreferredSize(scrSize.width / 2, 20);
		gridbag.setConstraints(tc, c);
		p.add(tc);
		//following text:"Names of entities are in field"
		l = new Label(res.getString("Names_of_entities_are"));
		c.gridwidth = 3;
		gridbag.setConstraints(l, c);
		p.add(l);
		str = spec.nameFieldName;
		if (str == null) {
			n = spec.nameFieldN;
			if (n < 0) {
				str = "";
			} else {
				str = String.valueOf(n + 1);
			}
		}
		tfNameField = new TextField(str, 6);
		c.gridwidth = 1;
		gridbag.setConstraints(tfNameField, c);
		p.add(tfNameField);
		//following text:"(enter name or number or leave empty)"
		l = new Label(res.getString("_enter_name_or_number"));
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(l, c);
		p.add(l);
		//following text:"Coordinates of entities are in fields"
		cbCoord = new Checkbox(res.getString("Coordinates_of"));
		cbCoord.addItemListener(this);
		gridbag.setConstraints(cbCoord, c);
		p.add(cbCoord);
		l = new Label("X:", Label.RIGHT);
		c.gridwidth = 1;
		gridbag.setConstraints(l, c);
		p.add(l);
		tfXField = new TextField("X", 6);
		gridbag.setConstraints(tfXField, c);
		p.add(tfXField);
		l = new Label("Y:", Label.RIGHT);
		gridbag.setConstraints(l, c);
		p.add(l);
		tfYField = new TextField("Y", 6);
		gridbag.setConstraints(tfYField, c);
		p.add(tfYField);
		l = new Label("(enter names)");
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(l, c);
		p.add(l);
		cbRad = new Checkbox("Circle radii are in field:");
		cbRad.setEnabled(false);
		cbRad.addItemListener(this);
		c.gridwidth = 3;
		gridbag.setConstraints(cbRad, c);
		p.add(cbRad);
		tfRadField = new TextField("radius", 10);
		tfRadField.setEnabled(false);
		c.gridwidth = 1;
		gridbag.setConstraints(tfRadField, c);
		p.add(tfRadField);
		//following text:"(enter names or numbers)"
		l = new Label("(enter name)");
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(l, c);
		p.add(l);
		if (spec.xCoordFieldName != null && spec.yCoordFieldName != null) {
			tfXField.setText(spec.xCoordFieldName);
			tfYField.setText(spec.yCoordFieldName);
			cbCoord.setState(true);
			cbRad.setEnabled(true);
			if (spec.radiusFieldName != null) {
				cbRad.setState(true);
				tfRadField.setText(spec.radiusFieldName);
				tfRadField.setEnabled(true);
			}
		} else {
			tfXField.setEnabled(false);
			tfYField.setEnabled(false);
		}
		Panel pp = new Panel(new FlowLayout(FlowLayout.CENTER, 30, 5));
		Button bLoad = new Button("OK");
		bLoad.setActionCommand("ok");
		bLoad.addActionListener(this);
		pp.add(bLoad);
		//following text:"Cancel"
		Button b = new Button(res.getString("Cancel"));
		b.setActionCommand("cancel");
		b.addActionListener(this);
		pp.add(b);
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(pp, c);
		p.add(pp);
		add(p, BorderLayout.SOUTH);

		pack();
		Dimension d = Toolkit.getDefaultToolkit().getScreenSize(), sz = getSize();
		if (sz.width > d.width * 3 / 4) {
			sz.width = d.width * 3 / 4;
		}
		if (sz.height > d.height * 3 / 4) {
			sz.height = d.height * 3 / 4;
		}
		setBounds((d.width - sz.width) / 2, (d.height - sz.height) / 2, sz.width, sz.height);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		showStatus(null, false);
		String cmd = e.getActionCommand();
		if (cmd == null)
			return;
		if (cmd.equals("ok"))
			if (checkFields()) {
				dispose();
			} else {
				;
			}
		else if (cmd.equals("cancel")) {
			cancelled = true;
			dispose();
		}
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource() == cbNTypes) {
			boolean flag = cbNTypes.getState();
			if (!flag) {
				tfNTypes.setText("");
			}
			tfNTypes.setEnabled(flag);
		} else if (e.getSource() == cbNNames) {
			boolean flag = cbNNames.getState();
			if (!flag) {
				tfNNames.setText("");
			}
			tfNNames.setEnabled(flag);
		} else if (e.getSource() == cbCoord) {
			tfXField.setEnabled(cbCoord.getState());
			tfYField.setEnabled(cbCoord.getState());
			cbRad.setEnabled(cbCoord.getState());
			tfRadField.setEnabled(cbCoord.getState() && cbRad.getState());
		} else if (e.getSource() == cbRad) {
			tfRadField.setEnabled(cbRad.getState());
		}
	}

	public void showStatus(String msg, boolean isError) {
		if (lStatus != null) {
			lStatus.showMessage(msg, isError);
		}
	}

	protected boolean checkFields() {
		String str = tfSep.getText();
		if (str == null || str.trim().length() < 1) {
			//following text:"No separator specified!"
			showStatus(res.getString("No_separator"), true);
			return false;
		}
		str = str.trim();
		if (str.equalsIgnoreCase("TAB") || str.equalsIgnoreCase("\\T")) {
			str = "\t";
		}
		if (str.length() > 1) {
			//following text:"The separator must be one character!"
			showStatus(res.getString("The_separator_must_be"), true);
			return false;
		}
		spec.setDelimiter(str);
		int n = -1;
		if (cbNTypes.getState()) {
			str = tfNTypes.getText();
			if (str != null) {
				str = str.trim();
				if (str.length() > 0) {
					try {
						n = Integer.valueOf(str).intValue() - 1;
					} catch (NumberFormatException nfe) {
						n = -1;
					}
				}
			}
			if (n < 0) {
				//following text:"Illegal number of the line with field types!"
				showStatus(res.getString("Illegal_number_of_the"), true);
				return false;
			}
		}
		spec.nRowWithFieldTypes = n;

		n = -1;
		if (cbNNames.getState()) {
			str = tfNNames.getText();
			if (str != null) {
				str = str.trim();
				if (str.length() > 0) {
					try {
						n = Integer.valueOf(str).intValue() - 1;
					} catch (NumberFormatException nfe) {
						n = -1;
					}
				}
			}
			if (n < 0) {
				//following text:"Illegal number of the line with field names!"
				showStatus(res.getString("Illegal_number_of_the1"), true);
				return false;
			}
		}
		spec.nRowWithFieldNames = n;

		spec.idFieldN = -1;
		spec.idFieldName = null;
		str = tfIdField.getText();
		if (str != null) {
			str = str.trim();
		}
		if (str != null && str.length() > 0) {
			try {
				n = Integer.valueOf(str).intValue() - 1;
				if (n < 0) {
					//following text:"Illegal number of the field with identifiers of entities!"
					showStatus(res.getString("Illegal_number_of_the2"), true);
					return false;
				} else {
					spec.idFieldN = n;
					spec.idFieldName = null;
				}
			} catch (NumberFormatException nfe) {
				spec.idFieldName = str;
				spec.idFieldN = -1;
			}
		}
		if (cbMultiLineObj.getState() && spec.idFieldN < 0) {
			showStatus("Identifiers are required when multiple lines may describe a single object!", true);
			return false;
		}
		spec.multipleRowsPerObject = cbMultiLineObj.getState();

		spec.nameFieldName = null;
		spec.nameFieldN = -1;
		str = tfNameField.getText();
		if (str != null) {
			str = str.trim();
			if (str.length() > 0) {
				try {
					n = Integer.valueOf(str).intValue() - 1;
					if (n < 0) {
						//following text:"Illegal number of the field with names of entities!"
						showStatus(res.getString("Illegal_number_of_the3"), true);
						return false;
					} else {
						spec.nameFieldN = n;
					}
				} catch (NumberFormatException nfe) {
					spec.nameFieldName = str;
				}
			}
		}
		spec.xCoordFieldName = spec.yCoordFieldName = null;
		if (cbCoord.getState()) {
			str = tfXField.getText();
			if (str != null) {
				str = str.trim();
				if (str.length() > 0) {
					spec.xCoordFieldName = str;
				}
			}
			str = tfYField.getText();
			if (str != null) {
				str = str.trim();
				if (str.length() > 0) {
					spec.yCoordFieldName = str;
				}
			}
			if (spec.xCoordFieldName == null) {
				//following text:"Name of the field with X-coordinate is not specified!"
				showStatus(res.getString("Name_of_the_field"), true);
				return false;
			}
			if (spec.yCoordFieldName == null) {
				//following text:"Name of the field with Y-coordinate is not specified!"
				showStatus(res.getString("Name_of_the_field1"), true);
				return false;
			}
			if (cbRad.getState()) {
				str = tfRadField.getText();
				if (str != null) {
					str = str.trim();
					if (str.length() > 0) {
						spec.radiusFieldName = str;
					}
				}
				if (spec.radiusFieldName == null) {
					showStatus("Name of the field with circle radii is not specified!", true);
					return false;
				}
			}
		}
		return true;
	}

	public boolean wasCancelled() {
		return cancelled;
	}
}
