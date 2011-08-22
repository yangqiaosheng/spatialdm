package db_work.ui;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Frame;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.Point;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Vector;

import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.OKDialog;
import spade.lib.util.StringUtil;
import db_work.data_descr.ColumnDescriptor;
import db_work.data_descr.ColumnDescriptorDate;
import db_work.data_descr.ColumnDescriptorNum;
import db_work.data_descr.ColumnDescriptorQual;
import db_work.data_descr.TableDescriptor;
import db_work.database.JDBCConnector;
import db_work.vis.MatrixFrame;

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: 09-Jun-2006
 * Time: 15:23:10
 * To change this template use File | Settings | File Templates.
 */
public class MDAggrFrame extends Frame implements WindowListener, ActionListener {

	protected JDBCConnector reader = null;
	protected TableDescriptor td = null;

	protected Vector opers = null, // aggregation operations and
			attrs = null; // attributes

	protected Vector referrers = null, // attribute names as strings
			breaks = null; // null for unique value, float[] overwise

	protected Panel pAggr = null, pRefs = null;
	protected Button bAddAttr = null, bAddSpace = null, bAddTime = null, bCreate = null, bAdd = null, bRemoveAO[] = null;

	public MDAggrFrame(Point point, JDBCConnector reader, TableDescriptor td) {
		super("Multi-dimensional aggregation");
		addWindowListener(this);
		setVisible(true);
		setLocation(point);
		this.reader = reader;
		this.td = td;
		setLayout(new BorderLayout());
		Panel p = new Panel();
		add(p, BorderLayout.CENTER);
		p.setLayout(new ColumnLayout());
		p.add(new Label("Aggregation basis:"));
		p.add(pRefs = new Panel());
		pRefs.setLayout(new ColumnLayout());
		referrers = new Vector(5, 5);
		breaks = new Vector(5, 5);
		//updatePRefs(false);
		Panel pp = new Panel(/*new FlowLayout(FlowLayout.LEFT,0,0)*/);
		pp.add(bAddAttr = new Button("Add attribute"));
		bAddAttr.addActionListener(this);
		pp.add(bAddSpace = new Button("Add space"));
		bAddSpace.addActionListener(this);
		pp.add(bAddTime = new Button("Add time"));
		bAddTime.addActionListener(this);
		p.add(pp);
		p.add(new Line(false));
		opers = new Vector(5, 5);
		opers.addElement("COUNT");
		attrs = new Vector(5, 5);
		attrs.addElement("*");
		bAdd = new Button("Add...");
		//bAdd.setEnabled(cd.nUniqueValues>1);
		bAdd.addActionListener(this);
		p.add(pAggr = new Panel());
		updatePAggr(false);
		p.add(new Line(false));
		p.add(bCreate = new Button("Create"));
		bCreate.addActionListener(this);
		bCreate.setEnabled(referrers.size() == 2 && opers.size() > 0);
		pack();
	}

	protected void updatePRefs(boolean rePack) {
		pRefs.removeAll();
		for (int i = 0; i < referrers.size(); i++) {
			pRefs.add(new Label(" " + i + ") " + (String) referrers.elementAt(i) + " " + ((breaks.elementAt(i) == null) ? "" : "(breaks)")));
		}
		if (bCreate != null) {
			bCreate.setEnabled(referrers.size() == 2 && opers.size() > 0);
		}
		if (rePack) {
			pack();
		}
	}

	protected void updatePAggr(boolean rePack) {
		pAggr.removeAll();
		if (opers.size() > 0) {
			bRemoveAO = new Button[opers.size()];
			for (int i = 0; i < bRemoveAO.length; i++) {
				bRemoveAO[i] = new Button("-");
				bRemoveAO[i].addActionListener(this);
			}
		} else {
			bRemoveAO = null;
		}
		pAggr.setLayout(new ColumnLayout());
		Panel pp = new Panel(new BorderLayout());
		pAggr.add(pp);
		pp.add(new Label("Aggregation operations:"), BorderLayout.CENTER);
		pp.add(bAdd, BorderLayout.EAST);
		for (int i = 0; i < opers.size(); i++) {
			pp = new Panel(new BorderLayout());
			pp.add(bRemoveAO[i], BorderLayout.WEST);
			pp.add(new Label(((String) opers.elementAt(i)) + "(" + ((String) attrs.elementAt(i)) + ")"), BorderLayout.CENTER);
			pAggr.add(pp);
		}
		if (bCreate != null) {
			bCreate.setEnabled(referrers.size() == 2 && opers.size() > 0);
		}
		if (rePack) {
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

	protected void addReferrer() {
		List cl = new List(20);
		for (int i = 0; i < td.getNColumns(); i++) {
			cl.add(td.getColumnDescriptor(i).name);
		}
		cl.select(0);
		OKDialog ok = new OKDialog(this, "1. Select referrer", false);
		ok.addContent(cl);
		ok.show();
		ColumnDescriptor cd = td.getColumnDescriptor(cl.getSelectedIndex());
		if (cd instanceof ColumnDescriptorDate) {
			cd = reader.getColumnDescriptorNumFromDate(td, (ColumnDescriptorDate) cd);
		}
		if (cd == null)
			return;
		if (!cd.numsDefined) {
			reader.getColumnDescriptorDetails(td, cd);
		}
		Checkbox cbDiscr = null, cbCont = null, cbRegroup = null;
		TextField tfNint = null;
		Panel p = new Panel();
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
			//cbDiscr.addItemListener(this);
			pp.add(cbRegroup = new Checkbox("group values (from " + cd.nUniqueValues + ")", cd.nUniqueValues > 100), BorderLayout.CENTER);
			p.add(pp);
			if (cd instanceof ColumnDescriptorNum) {
				pp = new Panel();
				pp.setLayout(new BorderLayout());
				pp.add(cbCont = new Checkbox("Intervals:", true, cbg), BorderLayout.WEST);
				//cbCont.addItemListener(this);
				pp.add(tfNint = new TextField("20"), BorderLayout.CENTER);
				p.add(pp);
			}
			//if (cbRegroup!=null && cbDiscr!=null)
			//  cbRegroup.setEnabled(cbDiscr.getState());
			//if (tfNint!=null && cbCont!=null)
			//  tfNint.setEnabled(cbCont.getState());
			p.add(new Line(false));
		}
		ok = new OKDialog(this, "2. Set binning parameters", false);
		ok.addContent(p);
		ok.show();
		referrers.addElement(new String(cd.name));
		if (cbDiscr.getState()) {
			breaks.addElement(null);
		} else {
			int nBins = 2;
			try {
				nBins = Integer.valueOf(tfNint.getText()).intValue();
			} catch (NumberFormatException nfe) {
			}
			if (nBins < 2) {
				nBins = 2;
			}
			ColumnDescriptorNum cdn = (ColumnDescriptorNum) cd;
			float br[] = new float[nBins - 1];
			for (int i = 0; i < br.length; i++) {
				br[i] = cdn.min + (cdn.max - cdn.min) * (i + 1) / nBins;
			}
			breaks.addElement(br);
		}
		updatePRefs(true);
	}

	public void createMatrices(Vector labelsV, Vector labelsH, float v[][][]) {
		for (int i = 0; i < opers.size(); i++) {
			new MatrixFrame(opers.elementAt(i) + "(" + attrs.elementAt(i) + ")", labelsV, labelsH, v[i]);
		}
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		if (ae.getSource() instanceof Button && ae.getSource().equals(bCreate)) {
			Vector v = reader.get2dBinCounts(td, referrers, breaks, opers, attrs);
			if (v != null) {
				createMatrices((Vector) v.elementAt(0), (Vector) v.elementAt(1), (float[][][]) v.elementAt(2));
			}
			return;
		}
		if (ae.getSource() instanceof Button && ae.getSource().equals(bAddAttr)) {
			addReferrer();
			return;
		}
		if (ae.getSource() instanceof Button && ae.getSource().equals(bAdd)) {
			addAggrParameters();
			return;
		}
		if (ae.getSource() instanceof Button && bRemoveAO != null) {
			for (int i = 0; i < bRemoveAO.length; i++)
				if (ae.getSource().equals(bRemoveAO[i])) {
					opers.remove(i);
					attrs.remove(i);
					updatePAggr(true);
					return;
				}
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
