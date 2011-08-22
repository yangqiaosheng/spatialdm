package db_work.ui;

import java.awt.BorderLayout;
import java.awt.Choice;
import java.awt.Frame;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.Point;
import java.util.Vector;

import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.OKDialog;
import spade.lib.basicwin.TabbedPanel;
import db_work.data_descr.TableDescriptor;

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: 09-Jun-2006
 * Time: 12:13:54
 * To change this template use File | Settings | File Templates.
 */
public class AddAggrParameters {

	protected static int lastOidx = -1, lastAidx = -1;

	public static Vector select(Frame parent, TableDescriptor td) {
		TabbedPanel tp = new TabbedPanel();
		Panel p = new Panel(new ColumnLayout());
		Panel pp = new Panel(new BorderLayout());
		p.add(pp);
		pp.add(new Label("Operation:"), BorderLayout.WEST);
		Choice chOper1 = new Choice();
		pp.add(chOper1);
		chOper1.addItem("COUNT");
		chOper1.addItem("SUM");
		chOper1.addItem("AVG");
		chOper1.addItem("STDDEV");
		chOper1.addItem("MIN");
		chOper1.addItem("MAX");
		chOper1.addItem("MAX-MIN");
		chOper1.addItem("MEDIAN");
		p.add(new Label("Table column:"));
		Choice chAttr1 = new Choice();
		p.add(chAttr1);
		chAttr1.addItem("*");
		for (int i = 0; i < td.getNColumns(); i++) {
			chAttr1.addItem(td.getColumnDescriptor(i).name);
		}
		if (lastOidx >= 0) {
			chOper1.select(lastOidx);
		}
		if (lastAidx >= 0) {
			chAttr1.select(lastAidx);
		}
		p.add(new Line(false));
		tp.addComponent("Aggr(Attr)", p);

		p = new Panel(new ColumnLayout());
		pp = new Panel(new BorderLayout());
		p.add(pp);
		pp.add(new Label("Operation:"), BorderLayout.WEST);
		Choice chOper2 = new Choice();
		pp.add(chOper2);
		for (int i = 1; i < chOper1.getItemCount(); i++) {
			chOper2.addItem(chOper1.getItem(i));
		}
		p.add(new Label("Table columns:"));
		List l2 = new List(10, true);
		for (int i = 0; i < td.getNColumns(); i++) {
			l2.add(td.getColumnDescriptor(i).name);
		}
		p.add(l2);
		p.add(new Line(false));
		tp.addComponent("Aggr(*)", p);

		p = new Panel(new ColumnLayout());
		pp = new Panel(new BorderLayout());
		p.add(pp);
		pp.add(new Label("Operations:"), BorderLayout.WEST);
		List l3 = new List(7, true);
		for (int i = 1; i < chOper1.getItemCount(); i++) {
			l3.add(chOper1.getItem(i));
		}
		pp.add(l3, BorderLayout.CENTER);
		p.add(new Label("Table column:"));
		Choice chAttr3 = new Choice();
		p.add(chAttr3);
		for (int i = 0; i < td.getNColumns(); i++) {
			chAttr3.addItem(td.getColumnDescriptor(i).name);
		}
		p.add(new Line(false));
		tp.addComponent("*(Attr)", p);

		tp.makeLayout(false);
		OKDialog okDlg = new OKDialog(parent, "Add aggregation", true);
		okDlg.addContent(tp);
		okDlg.show(new Point((int) (parent.getBounds().getX() + parent.getBounds().getWidth()), (int) parent.getBounds().getY()));
		if (okDlg.wasCancelled())
			return null;

		Vector v = new Vector(10, 10);
		switch (tp.getActiveTabN()) {
		case 0:
			int nOper = chOper1.getSelectedIndex(),
			nAttr = chAttr1.getSelectedIndex();
			lastOidx = nOper;
			lastAidx = nAttr;
			v.addElement(chOper1.getSelectedItem());
			v.addElement((nOper == 0) ? "*" : chAttr1.getSelectedItem());
			break;
		case 1:
			for (int i = 0; i < l2.getItemCount(); i++)
				if (l2.isIndexSelected(i)) {
					v.addElement(chOper2.getSelectedItem());
					v.addElement(l2.getItem(i));
				}
			break;
		case 2:
			for (int i = 0; i < l3.getItemCount(); i++)
				if (l3.isIndexSelected(i)) {
					v.addElement(l3.getItem(i));
					v.addElement(chAttr3.getSelectedItem());
				}
			break;
		}
		return v;
	}
}
