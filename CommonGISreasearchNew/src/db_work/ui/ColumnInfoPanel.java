package db_work.ui;

import java.awt.Button;
import java.awt.Frame;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.PopupManager;
import spade.lib.util.StringUtil;
import db_work.data_descr.ColumnDescriptor;
import db_work.data_descr.ColumnDescriptorDate;
import db_work.data_descr.ColumnDescriptorNum;
import db_work.data_descr.ColumnDescriptorQual;
import db_work.data_descr.TableDescriptor;
import db_work.database.JDBCConnector;

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: 03-Feb-2006
 * Time: 12:40:00
 * To change this template use File | Settings | File Templates.
 */
public class ColumnInfoPanel extends Panel implements ActionListener {

	protected JDBCConnector reader = null;
	protected TableDescriptor td = null;
	protected int cnum = -1;
	protected ColumnDescriptor cd = null;
	protected Button b = null;

	protected PopupManager pm = null;

	protected static String buttonLabel = "Further details...";

	public void setColumnNum(int cnum) {
		this.cnum = cnum;
		cd = td.getColumnDescriptor(cnum);
		b.setLabel(buttonLabel);
		displayInfo(true);
	}

	public ColumnInfoPanel(JDBCConnector reader, TableDescriptor td, int cnum) {
		this.reader = reader;
		this.td = td;
		this.cnum = cnum;
		cd = td.getColumnDescriptor(cnum);
		b = new Button(buttonLabel);
		b.addActionListener(this);
		setLayout(new ColumnLayout());
		displayInfo(false);
	}

	public void refresh() {
		setColumnNum(0);
	}

	public void displayInfo(boolean validateAll) {
		removeAll();
		add(new Label(cd.name + " (" + cd.type + ")"));
		add(new Line(false));
		Label l = null;
		if (cd.numsDefined) {
			add(l = new Label("N unique = " + cd.nUniqueValues));
			add(new Label("N nulls = " + cd.nNulls));
		}
		if (cd instanceof ColumnDescriptorQual) {
			// any specifics?
		}
		if (cd instanceof ColumnDescriptorDate) {
			ColumnDescriptorDate cdd = (ColumnDescriptorDate) cd;
			add(new Label("min = " + cdd.min));
			add(new Label("max = " + cdd.max));
		}
		if (cd instanceof ColumnDescriptorNum) {
			ColumnDescriptorNum cdn = (ColumnDescriptorNum) cd;
			add(new Label("min / avg / max = " + StringUtil.floatToStr(cdn.min, cdn.min, cdn.max) + " / " + StringUtil.floatToStr(cdn.avg, cdn.min, cdn.max) + " / " + StringUtil.floatToStr(cdn.max, cdn.min, cdn.max)));
		}
		add(b);
		if (validateAll) {
			CManager.validateAll(this);
		}
		if (cd.getNAggr() > 0 && cd.getALabels(0) != null) {
			String names[] = cd.getALabels(0);
			String str = "Unique values:\n";
			for (int i = 0; i < 20; i++)
				if (i < names.length) {
					str += names[i] + ",\n";
				} else {
					break;
				}
			if (names.length > 20) {
				str += "...";
			}
			if (pm == null) {
				pm = new PopupManager(l, str, true);
			} else {
				pm.attachTo(l);
				pm.setText(str);
			}
		}
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		if (ae.getSource() instanceof Button) {
			createBarCharts();
		}
	}

	private void createBarCharts() {
		// ... collect detailed statistics, show graphics
		ColumnDescriptor cdtemp = (cd instanceof ColumnDescriptorDate) ? reader.getColumnDescriptorNumFromDate(td, (ColumnDescriptorDate) cd) : cd;
		if (!cdtemp.numsDefined) { // somehow calculate statistics
			b.setLabel("calculating...");
			b.setEnabled(false);
			reader.getColumnDescriptorDetails(td, cdtemp);
			b.setLabel(buttonLabel);
			b.setEnabled(true);
		}
		displayInfo(true);
		if (!cdtemp.numsDefined)
			return;
		// histogram parameters dialog
		Frame fr = CManager.getAnyFrame(this);
		new ColumnDetailsFrame(new Point((int) (fr.getBounds().getX() + fr.getBounds().getWidth()), (int) fr.getBounds().getY()), reader, td, cdtemp);
	}

}
