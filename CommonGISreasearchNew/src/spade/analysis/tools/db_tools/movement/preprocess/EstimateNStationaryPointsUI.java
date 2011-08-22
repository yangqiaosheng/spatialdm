package spade.analysis.tools.db_tools.movement.preprocess;

import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Line;
import db_work.data_descr.TableDescriptor;
import db_work.database.OracleConnector;

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: Jul 7, 2010
 * Time: 5:44:57 PM
 * To change this template use File | Settings | File Templates.
 */
public class EstimateNStationaryPointsUI extends Panel implements ActionListener {

	OracleConnector reader = null;

	protected String tname = "";
	public float speed = 1f;

	TextField tf = null;
	Label lcount = null;
	Checkbox cb = null;
	List list = null;

	public String getSpeedAttr() {
		if (cb != null && cb.getState() && list.getSelectedIndex() >= 0)
			return list.getSelectedItem();
		else
			return null;
	}

	public EstimateNStationaryPointsUI(OracleConnector reader, String tname, String extraAttrColumns[], float speed, boolean bCoordinatesAreGeo) {
		super();
		this.reader = reader;
		this.tname = tname;
		this.speed = speed;
		tf = new TextField("" + speed, 10);
		setLayout(new ColumnLayout());
		cb = new Checkbox("use measured speed (in addition to computed)", false);
		list = new List(5);
		if (extraAttrColumns != null && extraAttrColumns.length > 0) {
			add(cb);
			add(new Label("Select table column with measured speed:"));
			add(list);
			TableDescriptor td = reader.getTableDescriptor(0);
			for (String extraAttrColumn : extraAttrColumns) {
				list.add(extraAttrColumn);
			}
			add(new Line(false));
		}
		add(new Label("Set speed threshold in " + ((bCoordinatesAreGeo) ? "km/h" : "units")));
		add(tf);
		add(new Line(false));
		lcount = new Label("bush button below to get the amount of stationary points");
		add(lcount);
		Button b = new Button("estimate N points");
		add(b);
		b.addActionListener(this);
		add(new Line(false));
		add(new Label("Do you want to remove stationary points and create new trajectories?"));
		add(new Label("Expected run time is the same as for the original data."));
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		speed = Float.valueOf(tf.getText()).floatValue();
		long[] N = reader.countStationaryPoints(tname, speed, getSpeedAttr());
		lcount.setText("N stationary points = " + N[0] + " of " + N[1]);
	}

}
