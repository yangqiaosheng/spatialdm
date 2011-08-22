package db_work.ui;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Frame;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Vector;

import spade.lib.basicwin.CManager;
import db_work.data_descr.ColumnDescriptor;
import db_work.data_descr.TableDescriptor;
import db_work.database.JDBCConnector;

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: 03-Feb-2006
 * Time: 12:53:35
 * To change this template use File | Settings | File Templates.
 */
public class TableInfoPanel extends Panel implements ActionListener, ItemListener {

	protected JDBCConnector reader = null;
	protected TableDescriptor td = null;

	protected ColumnInfoPanel cip = null;

	protected Button bRefresh = null, bMDBinning = null;
	protected Label l = null;
	protected List cl = null;

	public TableInfoPanel(JDBCConnector reader, TableDescriptor td) {
		super();
		this.reader = reader;
		this.td = td;
		reader.retrieveRowCount(0);
		ColumnDescriptor cds[] = new ColumnDescriptor[td.getNColumns()];
		for (int i = 0; i < cds.length; i++) {
			cds[i] = td.getColumnDescriptor(i);
		}
		reader.getStatsForColumnDescriptors(td, cds);
		setLayout(new BorderLayout());
		Panel p = new Panel(new BorderLayout());
		add(p, BorderLayout.NORTH);
		p.add(l = new Label("cols=" + td.columns.size() + ", rows=" + td.dbRowCount), BorderLayout.WEST);
		p.add(bRefresh = new Button("refresh..."), BorderLayout.EAST);
		bRefresh.addActionListener(this);
		p.add(bMDBinning = new Button("Multidimensional aggregation..."), BorderLayout.SOUTH);
		bMDBinning.addActionListener(this);
		cl = new List(20);
		add(cl, BorderLayout.CENTER);
		for (int i = 0; i < td.getNColumns(); i++) {
			cl.add(td.getColumnDescriptor(i).name);
		}
		cl.select(0);
		cl.addItemListener(this);
		cip = new ColumnInfoPanel(reader, td, 0);
		add(cip, BorderLayout.SOUTH);
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		List cl = (List) e.getSource();
		cip.setColumnNum(cl.getSelectedIndex());
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource().equals(bRefresh)) {
			Vector columnNames = new Vector(td.getNColumns(), 10);
			for (int i = 0; i < td.getNColumns(); i++) {
				columnNames.addElement(td.getColumnDescriptor(i).name);
			}
			td.clear();
			reader.loadTableDescriptor(0, columnNames);
			l.setText("cols=" + td.columns.size() + ", rows=" + td.dbRowCount);
			cl.removeAll();
			for (int i = 0; i < td.getNColumns(); i++) {
				cl.add(td.getColumnDescriptor(i).name);
			}
			cl.select(0);
			cip.refresh();
			return;
		}
		if (e.getSource().equals(bMDBinning)) {
			Frame fr = CManager.getAnyFrame(this);
			MDAggrFrame mdAggrFrame = new MDAggrFrame(new Point((int) (fr.getBounds().getX() + fr.getBounds().getWidth()), (int) fr.getBounds().getY()), reader, td);
		}
	}

}
