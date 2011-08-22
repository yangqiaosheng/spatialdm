package db_work.ui;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Frame;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Point;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Vector;

import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Line;
import spade.lib.util.StringUtil;
import db_work.data_descr.ColumnDescriptor;
import db_work.data_descr.ColumnDescriptorNum;
import db_work.data_descr.ColumnDescriptorQual;
import db_work.data_descr.TableDescriptor;
import db_work.database.JDBCConnector;
import db_work.vis.BarChartFrame;

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: 07-Feb-2006
 * Time: 15:25:16
 * To change this template use File | Settings | File Templates.
 */
public class ColumnDetailsFrame extends Frame implements WindowListener, ActionListener, ItemListener {

	protected JDBCConnector reader = null;
	protected TableDescriptor td = null;
	protected ColumnDescriptor cd = null;

	protected Vector opers = null, // aggregation operations and
			attrs = null; // attributes

	protected Panel pAggr = null;
	protected Button bCreate = null, bAdd = null, bRemove[] = null;
	protected Checkbox cbDiscr = null, cbCont = null, cbRegroup = null;
	protected TextField tfNint = null;

	public ColumnDetailsFrame(Point point, JDBCConnector reader, TableDescriptor td, ColumnDescriptor cd) {
		super(cd.name);
		addWindowListener(this);
		setVisible(true);
		setLocation(point);
		this.reader = reader;
		this.td = td;
		this.cd = cd;
		setLayout(new BorderLayout());
		Panel p = new Panel();
		add(p, BorderLayout.CENTER);
		p.setLayout(new ColumnLayout());
		p.add(new Label(cd.name + " (" + cd.type + ")"));
		p.add(new Line(false));
		p.add(new Label("N unique = " + cd.nUniqueValues + " (Nrows=" + td.dbRowCount + ")"));
		p.add(new Label("N nulls = " + cd.nNulls));
		if (cd.nUniqueValues + cd.nNulls == td.dbRowCount)
			if (cd.nNulls == 0) {
				p.add(new Label("=> All rows have unique non-null values"));
			} else {
				p.add(new Label("=> All rows have NULLs or unique values"));
			}
		if (cd.nUniqueValues == 1)
			if (cd.nNulls == 0) {
				p.add(new Label("=> All values are the same"));
			} else {
				p.add(new Label("=> All values (except NULLs) are the same"));
			}
		if (cd instanceof ColumnDescriptorNum) {
			ColumnDescriptorNum cdn = (ColumnDescriptorNum) cd;
			p.add(new Label("min / avg / max = " + StringUtil.floatToStr(cdn.min, cdn.min, cdn.max) + " / " + StringUtil.floatToStr(cdn.avg, cdn.min, cdn.max) + " / " + StringUtil.floatToStr(cdn.max, cdn.min, cdn.max)));
		}
		p.add(new Line(false));
		if (cd.nUniqueValues > 1) {
			p.add(new Label("Binning by:"));
			CheckboxGroup cbg = new CheckboxGroup();
			Panel pp = new Panel();
			pp.setLayout(new BorderLayout());
			pp.add(cbDiscr = new Checkbox("Discrete values", cd instanceof ColumnDescriptorQual, cbg), BorderLayout.WEST);
			cbDiscr.addItemListener(this);
			pp.add(cbRegroup = new Checkbox("group values (from " + cd.nUniqueValues + ")", cd.nUniqueValues > 100), BorderLayout.CENTER);
			p.add(pp);
			if (cd instanceof ColumnDescriptorNum) {
				pp = new Panel();
				pp.setLayout(new BorderLayout());
				pp.add(cbCont = new Checkbox("Intervals:", true, cbg), BorderLayout.WEST);
				cbCont.addItemListener(this);
				pp.add(tfNint = new TextField("20"), BorderLayout.CENTER);
				p.add(pp);
			}
			if (cbRegroup != null && cbDiscr != null) {
				cbRegroup.setEnabled(cbDiscr.getState());
			}
			if (tfNint != null && cbCont != null) {
				tfNint.setEnabled(cbCont.getState());
			}
			p.add(new Line(false));
		}
		opers = new Vector(5, 5);
		opers.addElement("COUNT");
		attrs = new Vector(5, 5);
		attrs.addElement("*");
		bAdd = new Button("Add...");
		bAdd.setEnabled(cd.nUniqueValues > 1);
		bAdd.addActionListener(this);
		p.add(pAggr = new Panel());
		updatePAggr(false);
		p.add(new Line(false));
		p.add(bCreate = new Button("Create"));
		if (cd.nUniqueValues < 2) {
			bCreate.setEnabled(false);
		} else {
			bCreate.addActionListener(this);
		}
		pack();
	}

	protected void updatePAggr(boolean rePack) {
		pAggr.removeAll();
		if (opers.size() > 0) {
			bRemove = new Button[opers.size()];
			for (int i = 0; i < bRemove.length; i++) {
				bRemove[i] = new Button("-");
				bRemove[i].addActionListener(this);
			}
		} else {
			bRemove = null;
		}
		pAggr.setLayout(new ColumnLayout());
		Panel pp = new Panel(new BorderLayout());
		pAggr.add(pp);
		pp.add(new Label("Aggregation operations:"), BorderLayout.CENTER);
		pp.add(bAdd, BorderLayout.EAST);
		for (int i = 0; i < opers.size(); i++) {
			pp = new Panel(new BorderLayout());
			pp.add(bRemove[i], BorderLayout.WEST);
			pp.add(new Label(((String) opers.elementAt(i)) + "(" + ((String) attrs.elementAt(i)) + ")"), BorderLayout.CENTER);
			pAggr.add(pp);
		}
		if (rePack) {
			bCreate.setEnabled(opers.size() > 0);
			pack();
		}
	}

	protected void addAggrParameters() {
		Vector v = AddAggrParameters.select(this, td);
		if (v != null && v.size() > 0) {
			for (int i = 0; i < v.size(); i += 2) {
				opers.addElement(v.elementAt(i));
				attrs.addElement(v.elementAt(i + 1));
			}
			updatePAggr(true);
		}
	}

	protected void createBarChart() {
		Vector subnames = new Vector(opers.size(), 10);
		for (int i = 0; i < opers.size(); i++) {
			subnames.addElement((String) opers.elementAt(i) + "(" + (String) attrs.elementAt(i) + ")");
		}
		if (cbCont != null && cbCont.getState()) {
			int nBins = 0;
			try {
				nBins = Integer.valueOf(tfNint.getText()).intValue();
			} catch (NumberFormatException nfe) {
			}
			if (nBins < 1)
				return;
			int m = cd.getNAggr();
			int n = reader.getColumnDescriptorBinCounts(td, (ColumnDescriptorNum) cd, nBins, opers, attrs);
			//displayInfo(true);
			Vector v = new Vector(n, 1);
			for (int i = 0; i < n; i++) {
				v.addElement(cd.getAVals(m + i));
			}
			new BarChartFrame(cd.name + " (" + nBins + " bins)", subnames, v, cd.getALabels(m));
			return;
		}
		if (cbDiscr != null && cbDiscr.getState()) {
			if (cbRegroup != null && cbRegroup.getState()) {
				// ...
			}
			int m = cd.getNAggr();
			int n = reader.getColumnDescriptorBinCounts(td, cd, opers, attrs);
			//displayInfo(true);
			Vector v = new Vector(n, 1);
			for (int i = 0; i < n; i++) {
				v.addElement(cd.getAVals(m + i));
			}
			new BarChartFrame(cd.name + " (" + cd.getANBins(m) + " bins)", subnames, v, cd.getALabels(m));
			return;
		}
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		if (ae.getSource() instanceof Button && ae.getSource().equals(bCreate)) {
			createBarChart();
			return;
		}
		if (ae.getSource() instanceof Button && ae.getSource().equals(bAdd)) {
			addAggrParameters();
			return;
		}
		if (ae.getSource() instanceof Button && bRemove != null) {
			for (int i = 0; i < bRemove.length; i++)
				if (ae.getSource().equals(bRemove[i])) {
					opers.remove(i);
					attrs.remove(i);
					updatePAggr(true);
					return;
				}
		}
	}

	@Override
	public void itemStateChanged(ItemEvent ie) {
		if (cbRegroup != null && cbDiscr != null) {
			cbRegroup.setEnabled(cbDiscr.getState());
		}
		if (tfNint != null && cbCont != null) {
			tfNint.setEnabled(cbCont.getState());
		}
	}

	@Override
	public void windowClosing(WindowEvent e) {
		if (e.getSource().equals(this)) {
			dispose();
			//any other cleenup...
		}
	}

	@Override
	public void windowClosed(WindowEvent e) {
	}

	@Override
	public void windowOpened(WindowEvent e) {
	}

	@Override
	public void windowIconified(WindowEvent e) {
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
	}

	@Override
	public void windowActivated(WindowEvent e) {
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
	}

}
