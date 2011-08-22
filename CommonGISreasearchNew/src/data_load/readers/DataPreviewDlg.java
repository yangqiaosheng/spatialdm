package data_load.readers;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.ScrollPane;
import java.awt.TextField;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ResourceBundle;

import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.NotificationLine;
import spade.lib.basicwin.TextCanvas;
import spade.lib.lang.Language;

public class DataPreviewDlg extends Dialog implements ActionListener, ItemListener {
	static ResourceBundle res = Language.getTextResource("data_load.readers.Res");
	protected TextField tfIdField = null, tfNameField = null, tfXField = null, tfYField = null;
	protected Checkbox cbCoord = null;
	public boolean cancelled = false;
	protected NotificationLine lStatus = null;
	protected DataSample sample = null;
	protected int idFieldN = -1, nameFieldN = -1;
	protected String xFName = null, yFName = null;

	public DataPreviewDlg(Frame owner, DataSample data) {
		this(owner, data, false);
	}

	public DataPreviewDlg(Frame owner, DataSample data, boolean withoutXYFields) {
		//following text:"Data preview"
		super(owner, res.getString("Data_preview"), true);
		sample = data;
		setLayout(new BorderLayout());
		if (sample == null || sample.getNFields() < 1 || sample.getNRecords() < 1) {
			lStatus = new NotificationLine("");
			add(lStatus, "North");
			Panel p = new Panel(new FlowLayout(FlowLayout.CENTER, 30, 0));
			//following text:"Cancel"
			Button b = new Button(res.getString("Cancel"));
			b.addActionListener(this);
			p.add(b);
			add(p, "South");
			//following text:"No data available"
			lStatus.showMessage(res.getString("No_data_available"), true);
		} else {
			Panel p = new Panel(new GridLayout(2, 1));
			lStatus = new NotificationLine("");
			p.add(lStatus);
			//following text:"Data content:"
			p.add(new Label(res.getString("Data_content_")));
			add(p, "North");
			TableDraw tdraw = new TableDraw(sample);
			//tdraw.addActionListener(this);
			ScrollPane scp = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
			scp.add(tdraw);
			add(scp, "Center");
			p = new Panel(new ColumnLayout());
			p.setBackground(Color.lightGray);
			Panel pp = new Panel(new BorderLayout());
			//following text:"Identifiers are in field"
			pp.add(new Label(res.getString("Identifiers_are_in"), Label.LEFT), "West");
			tfIdField = new TextField(6);
			pp.add(tfIdField, "East");
			p.add(pp);
			//following text:"(enter the name or the number of the field)"
			p.add(new Label(res.getString("_enter_the_name_or"), Label.RIGHT));
			TextCanvas tc = new TextCanvas();
			//following text:"If there is no field with identifiers,"
			//following text:"the system will produce default identifiers from record numbers"
			tc.addTextLine(res.getString("If_there_is_no_field") + res.getString("the_system_will"));
			tc.setBackground(new Color(255, 255, 192));
			p.add(tc);
			pp = new Panel(new BorderLayout());
			//following text:"Names of entities are in fiel
			pp.add(new Label(res.getString("Names_of_entities_are"), Label.LEFT), "West");
			tfNameField = new TextField(6);
			pp.add(tfNameField, "East");
			p.add(pp);
			//following text:"(set name or number or leave empty)"
			p.add(new Label(res.getString("_set_name_or_number"), Label.RIGHT));

			//following text:"Coordinates of entities are in fields"

			if (!withoutXYFields) { // ~MO
				cbCoord = new Checkbox(res.getString("Coordinates_of"));
				cbCoord.addItemListener(this);
				p.add(cbCoord);
				pp = new Panel(new GridLayout(1, 2));
				tfXField = new TextField("X", 6);
				tfXField.setEnabled(false);
				Panel p1 = new Panel(new BorderLayout());
				p1.add(new Label("X:", Label.RIGHT), "West");
				p1.add(tfXField, "Center");
				pp.add(p1);
				tfYField = new TextField("Y", 6);
				tfYField.setEnabled(false);
				p1 = new Panel(new BorderLayout());
				p1.add(new Label("Y:", Label.RIGHT), "West");
				p1.add(tfYField, "Center");
				pp.add(p1);
				p.add(pp);
			}
			//following text:"(enter names or numbers)"
			p.add(new Label(res.getString("_enter_names_or"), Label.RIGHT));
			p.add(new Label(""));
			pp = new Panel(new FlowLayout(FlowLayout.CENTER, 30, 0));
			Button bLoad = new Button("OK");
			bLoad.addActionListener(this);
			pp.add(bLoad);
			//following text:"Cancel"
			Button b = new Button(res.getString("Cancel"));
			b.addActionListener(this);
			pp.add(b);
			p.add(pp);
			add(p, "South");
		}
		Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
		setBounds((d.width - 400) / 2, (d.height - 400) / 2, 400, 400);
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource() == cbCoord) {
			tfXField.setEnabled(cbCoord.getState());
			tfYField.setEnabled(cbCoord.getState());
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		showStatus(null, false);
		String cmd = e.getActionCommand();
		if (cmd == null)
			return;
		if (cmd.equals("OK")) {
			if (checkCanClose()) {
				dispose();
			}
			return;
		} else if (cmd.equals(res.getString("Cancel"))) {
			cancelled = true;
			dispose();
		}
	}

	public void showStatus(String msg, boolean isError) {
		if (lStatus != null) {
			lStatus.showMessage(msg, isError);
		}
	}

	protected boolean checkCanClose() {
		String str = tfIdField.getText();
		if (str != null) {
			str = str.trim();
		}
		if (str == null || str.length() < 1) {
			idFieldN = -1;
//      showStatus("The name or number of the field with identifiers is not specified!",true);
		} else {
			try {
				int n = Integer.valueOf(str).intValue() - 1;
				if (n < 0 || n >= sample.getNFields()) {
					//following text:"Illegal number of the field with identifiers of entities!"
					showStatus(res.getString("Illegal_number_of_the2"), true);
					return false;
				}
				idFieldN = n;
			} catch (NumberFormatException nfe) {
				int n = sample.getFieldN(str);
				if (n < 0) {
					//following text:"Illegal name of the field with identifiers of entities!"
					showStatus(res.getString("Illegal_name_of_the"), true);
					return false;
				}
				idFieldN = n;
			}
		}
		str = tfNameField.getText();
		if (str != null) {
			str = str.trim();
			if (str.length() > 0) {
				try {
					int n = Integer.valueOf(str).intValue() - 1;
					if (n < 0) {
						//following text:"Illegal number of the field with identifiers of entities!"
						showStatus(res.getString("Illegal_number_of_the3"), true);
						return false;
					}
					nameFieldN = n;
				} catch (NumberFormatException nfe) {
					int n = sample.getFieldN(str);
					if (n < 0) {
						//following text:"Illegal name of the field with identifiers of entities!"
						showStatus(res.getString("Illegal_name_of_the"), true);
						return false;
					}
					nameFieldN = n;
				}
			}
		}
		xFName = yFName = null;
		if (cbCoord != null && cbCoord.getState()) {
			int xFN = -1, yFN = -1;
			str = tfXField.getText();
			if (str != null) {
				str = str.trim();
			}
			if (str == null || str.length() < 1) {
				//following text:"The name or number of the field with X-coordinates is not specified!"
				showStatus(res.getString("The_name_or_number_of"), true);
				return false;
			}
			try {
				int n = Integer.valueOf(str).intValue() - 1;
				if (n < 0 || n >= sample.getNFields()) {
					//following text:"Illegal number of the field with X-coordinates!"
					showStatus(res.getString("Illegal_number_of_the4"), true);
					return false;
				}
				xFN = n;
			} catch (NumberFormatException nfe) {
				int n = sample.getFieldN(str);
				if (n < 0) {
					//following text:"Illegal name of the field with X-coordinates!"
					showStatus(res.getString("Illegal_name_of_the1"), true);
					return false;
				}
				xFN = n;
			}
			str = tfYField.getText();
			if (str != null) {
				str = str.trim();
			}
			if (str == null || str.length() < 1) {
				//following text:"The name or number of the field with Y-coordinates is not specified!"
				showStatus(res.getString("The_name_or_number_of1"), true);
				return false;
			}
			try {
				int n = Integer.valueOf(str).intValue() - 1;
				if (n < 0 || n == xFN || n >= sample.getNFields()) {
					//following text:"Illegal number of the field with Y-coordinates!"
					showStatus(res.getString("Illegal_number_of_the5"), true);
					return false;
				}
				yFN = n;
			} catch (NumberFormatException nfe) {
				int n = sample.getFieldN(str);
				if (n < 0) {
					//following text:"Illegal name of the field with Y-coordinates!"
					showStatus(res.getString("Illegal_name_of_the2"), true);
					return false;
				}
				yFN = n;
			}
			xFName = sample.getFieldName(xFN);
			yFName = sample.getFieldName(yFN);
		}
		return true;
	}

	public boolean wasCancelled() {
		return cancelled;
	}

	public int getIdFieldN() {
		return idFieldN;
	}

	public int getNameFieldN() {
		return nameFieldN;
	}

	public String getXCoordFName() {
		return xFName;
	}

	public String getYCoordFName() {
		return yFName;
	}
}
