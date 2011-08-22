package data_load.read_oracle;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Choice;
import java.awt.FlowLayout;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ItemEvent;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.lib.basicwin.ColumnLayout;
import spade.lib.lang.Language;
import data_load.read_db.ColumnSelectPanel;

public class OracleColumnSelectPanel extends ColumnSelectPanel {
	static ResourceBundle res = Language.getTextResource("data_load.read_oracle.Res");
	/**
	* Names of columns containing geoinformation
	*/
	protected Vector geoColumns = null;
	protected Choice geoCh = null;
	protected Checkbox useGeoCB = null;

	public OracleColumnSelectPanel(Vector columns, Vector geoColumns) {
		super();
		init(columns, geoColumns);
	}

	public void init(Vector columns, Vector geoColumns) {
		this.columns = columns;
		this.geoColumns = geoColumns;
		if (geoColumns == null || geoColumns.size() < 1) {
			init(columns);
			return;
		}
		colList = new List(10, true);
		for (int i = 0; i < columns.size(); i++) {
			colList.add((String) columns.elementAt(i));
		}
		Panel p = new Panel(new BorderLayout());
		p.add(colList, "Center");
		//following text:"Select columns to load:"
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
		p = new Panel(new ColumnLayout());
		//following text:"Identifiers of objects are in column:"
		p.add(new Label(res.getString("Identifiers_of")));
		idTF = new TextField(20);
		p.add(idTF);
		//following text:"Names of objects are in column:"
		p.add(new Label(res.getString("Names_of_objects_are")));
		nameTF = new TextField(20);
		p.add(nameTF);
		//following text:"Take geographic data from column:"
		useGeoCB = new Checkbox(res.getString("Take_geographic_data"), true);
		p.add(useGeoCB);
		if (geoColumns.size() < 2) {
			p.add(new Label((String) geoColumns.elementAt(0), Label.CENTER));
		} else {
			geoCh = new Choice();
			for (int i = 0; i < geoColumns.size(); i++) {
				geoCh.add((String) geoColumns.elementAt(i));
			}
			p.add(geoCh);
			useGeoCB.addItemListener(this);
		}
		add(p, "East");
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource().equals(useGeoCB) && geoCh != null) {
			geoCh.setEnabled(useGeoCB.getState());
		}
	}

	public String getGeoColName() {
		if (geoColumns == null || geoColumns.size() < 1)
			return null;
		if (!useGeoCB.getState())
			return null;
		if (geoCh != null)
			return geoCh.getSelectedItem();
		return (String) geoColumns.elementAt(0);
	}

}
