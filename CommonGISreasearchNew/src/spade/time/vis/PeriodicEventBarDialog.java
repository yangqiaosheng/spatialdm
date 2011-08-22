package spade.time.vis;

import java.awt.BorderLayout;
import java.awt.Choice;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Label;
import java.awt.Panel;
import java.awt.ScrollPane;
import java.awt.TextField;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Vector;

import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.OKDialog;
import spade.time.Date;
import spade.time.TimeLimits;
import spade.time.TimeMoment;
import spade.vis.action.Highlighter;
import spade.vis.database.DataTable;
import spade.vis.database.Parameter;

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: Mar 12, 2010
 * Time: 1:33:59 PM
 * To change this template use File | Settings | File Templates.
 */
public class PeriodicEventBarDialog extends OKDialog implements ItemListener, ActionListener {

	Parameter par = null;
	Highlighter highlighter = null;
	TimeGraph tigr = null;
	Vector<Vector<String>> vvIds = null;
	Vector<Vector<Integer>> vvEvtNums = null;

	int nColumns = 1, offset = 0;

	TimeArrangerWithIDsCanvas taCanvasMatrix = null, taCanvasRow = null, taCanvasColumn = null;
	Panel pControls = null;
	Choice chPeriod = null;
	TextField tfNColumns = null, tfOffset = null;
	char timeBreak = ' ';

	public PeriodicEventBarDialog(Frame owner, Parameter par, Highlighter highlighter, TimeGraph tigr, Vector<Vector<String>> vvIds, Vector<Vector<Integer>> vvEvtNums) {
		super(false, owner, "Periodic Event Bar", false);
		this.par = par;
		this.highlighter = highlighter;
		this.tigr = tigr;
		this.vvIds = vvIds;
		this.vvEvtNums = vvEvtNums;
		nColumns = tigr.getLiderPeriod();
		offset = tigr.getLiderOffset();
		createControls();
		update();
		show();
	}

	public void update() {
		TimeMoment times[] = new TimeMoment[par.getValueCount()];
		for (int i = 0; i < par.getValueCount(); i++) {
			times[i] = (TimeMoment) par.getValue(i);
		}
		taCanvasMatrix = new TimeArrangerWithIDsCanvas((DataTable) tigr.getTable(), tigr, tigr.getEventTable(), null, highlighter, times, vvIds, vvEvtNums);
		if (timeBreak == ' ') {
			taCanvasMatrix.setNColumns(nColumns, offset);
		} else {
			taCanvasMatrix.setNColumns(timeBreak);
		}
		taCanvasMatrix.desiredWidth = nColumns * taCanvasMatrix.getCellSizeX();
		taCanvasMatrix.desiredHeight = taCanvasMatrix.getNRows() * taCanvasMatrix.getCellSizeY();
		nColumns = taCanvasMatrix.getNColumns();
		taCanvasMatrix.makeTimeLimits();

		times = new TimeMoment[nColumns];
		Vector<Vector<String>> vvIdsRow = new Vector<Vector<String>>(nColumns, 10);
		Vector<Vector<Integer>> vvEvtNumsRow = new Vector<Vector<Integer>>(nColumns, 10);
		for (int i = 0; i < nColumns; i++) {
			vvIdsRow.addElement(null);
			vvEvtNumsRow.addElement(null);
		}
		for (int i = 0; i < par.getValueCount(); i++) {
			int xx = taCanvasMatrix.getX(i);
			if (xx < times.length && times[xx] == null) {
				times[xx] = (TimeMoment) par.getValue(i);
			}
			if (vvIds.elementAt(i) != null && vvIds.elementAt(i).size() > 0) {
				if (vvIdsRow.elementAt(xx) == null) {
					vvIdsRow.setElementAt(new Vector<String>(10, 10), xx);
					vvEvtNumsRow.setElementAt(new Vector<Integer>(10, 10), xx);
				}
				for (int k = 0; k < vvIds.elementAt(i).size(); k++) {
					vvIdsRow.elementAt(xx).addElement(vvIds.elementAt(i).elementAt(k));
					vvEvtNumsRow.elementAt(xx).addElement(vvEvtNums.elementAt(i).elementAt(k));
				}
			}
		}
		taCanvasRow = new TimeArrangerWithIDsCanvas((DataTable) tigr.getTable(), null, tigr.getEventTable(), null, highlighter, times, vvIdsRow, vvEvtNumsRow);
		taCanvasRow.setNColumns(nColumns, 0);
		taCanvasRow.desiredWidth = nColumns * taCanvasRow.getCellSizeX();
		taCanvasRow.desiredHeight = taCanvasRow.getCellSizeY();

		int nRows = taCanvasMatrix.getNRows();

		TimeLimits tLimits[] = new TimeLimits[nColumns];
		for (int i = 0; i < nColumns; i++) {
			tLimits[i] = new TimeLimits();
			tLimits[i].init(nRows, 1);
		}
		for (int i = 0; i < par.getValueCount(); i++) {
			int xx = taCanvasMatrix.getX(i);
			if (xx < tLimits.length) {
				tLimits[xx].addLimits(taCanvasMatrix.getTimeLimit(i));
			}
		}
		taCanvasRow.setTimeLimits(tLimits);
		taCanvasRow.addTimeLimitsListener(tigr);

		times = new TimeMoment[nRows];
		Vector<Vector<String>> vvIdsColumn = new Vector<Vector<String>>(nRows, 10);
		Vector<Vector<Integer>> vvEvtNumsColumn = new Vector<Vector<Integer>>(nRows, 10);
		for (int i = 0; i < nRows; i++) {
			vvIdsColumn.addElement(null);
			vvEvtNumsColumn.addElement(null);
		}
		tLimits = new TimeLimits[nRows];
		for (int i = 0; i < nRows; i++) {
			tLimits[i] = new TimeLimits();
			tLimits[i].init(1, 1);
		}
		TimeMoment t1 = (TimeMoment) par.getFirstValue();
		int rowN = 0;
		long diff = -1;
		for (int i = 0; i < par.getValueCount(); i++) {
			int yy = taCanvasMatrix.getY(i);
			if (yy > rowN) {
				TimeMoment t2 = ((TimeMoment) par.getValue(i)).getCopy();
				t2.add(-1);
				tLimits[rowN].addLimit(t1, t2);
				if (diff < 0) {
					diff = t2.subtract(t1);
				}
				t1 = (TimeMoment) par.getValue(i);
				rowN = yy;
			}
			if (times[yy] == null) {
				times[yy] = (TimeMoment) par.getValue(i);
			}
			if (vvIds.elementAt(i) != null && vvIds.elementAt(i).size() > 0) {
				if (vvIdsColumn.elementAt(yy) == null) {
					vvIdsColumn.setElementAt(new Vector<String>(10, 10), yy);
					vvEvtNumsColumn.setElementAt(new Vector<Integer>(10, 10), yy);
				}
				for (int k = 0; k < vvIds.elementAt(i).size(); k++) {
					vvIdsColumn.elementAt(yy).addElement(vvIds.elementAt(i).elementAt(k));
					vvEvtNumsColumn.elementAt(yy).addElement(vvEvtNums.elementAt(i).elementAt(k));
				}
			}
		}
		TimeMoment t2 = t1.getCopy();
		t2.add(diff);
		tLimits[rowN].addLimit(t1, t2);
		taCanvasColumn = new TimeArrangerWithIDsCanvas((DataTable) tigr.getTable(), null, tigr.getEventTable(), null, highlighter, times, vvIdsColumn, vvEvtNumsColumn);
		taCanvasColumn.setNColumns(1, 0);
		taCanvasColumn.desiredWidth = 20; //taCanvasRow.getCellSizeX();
		taCanvasColumn.desiredHeight = nRows * taCanvasRow.getCellSizeY();
		taCanvasColumn.setTimeLimits(tLimits);
		taCanvasColumn.addTimeLimitsListener(tigr);

		Panel p = new Panel(new BorderLayout()), pp = new Panel(new BorderLayout()), ppp = new Panel(new ColumnLayout());
		ppp.add(taCanvasMatrix);
		ppp.add(new Line(false));
		ppp.add(taCanvasRow);
		ppp.add(new Line(false));
		pp.add(ppp, BorderLayout.CENTER);
		ppp = new Panel(new ColumnLayout());
		ppp.add(taCanvasColumn);
		pp.add(ppp, BorderLayout.EAST);

		Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
		int h = 10 + taCanvasColumn.desiredHeight + taCanvasRow.desiredHeight;
		System.out.println("* height=" + h);
		if (h > d.height * 2 / 3) {
			System.out.println("* ... creating ScrollPane");
			ScrollPane sp = new ScrollPane(ScrollPane.SCROLLBARS_ALWAYS);
			sp.add(pp);
			p.add(sp, BorderLayout.CENTER);
		} else {
			p.add(pp, BorderLayout.CENTER);
		}

		p.add(pControls, BorderLayout.SOUTH);
		addContent(p);
		invalidate();
		validate();
	}

	public void createControls() {
		pControls = new Panel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		chPeriod = new Choice();
		chPeriod.addItem("fixed");
		if ((TimeMoment) par.getValue(0) instanceof Date && ((Date) par.getValue(0)).getPrecisionIdx() > 0) {
			for (int i = 0; i < ((Date) par.getValue(0)).getPrecisionIdx(); i++) {
				String str = "";
				switch (i) {
				case 0:
					str = "years";
					break;
				case 1:
					str = "months";
					break;
				case 2:
					str = "days";
					break;
				case 3:
					str = "hours";
					break;
				case 4:
					str = "minutes";
					break;
				}
				chPeriod.addItem(str);
			}
		}
		if (chPeriod.getItemCount() > 1) {
			chPeriod.addItemListener(this);
		}
		tfNColumns = new TextField("" + nColumns, 3);
		tfNColumns.addActionListener(this);
		tfOffset = new TextField("" + offset, 3);
		tfOffset.addActionListener(this);
		pControls.add(new Label("Period:"));
		pControls.add(chPeriod);
		pControls.add(new Label("length"));
		pControls.add(tfNColumns);
		pControls.add(new Label("offset"));
		pControls.add(tfOffset);
	}

	protected void getNColumnsAndOffset() {
		try {
			nColumns = Integer.valueOf(tfNColumns.getText()).intValue();
			offset = Integer.valueOf(tfOffset.getText()).intValue();
			if (nColumns < 1) {
				nColumns = 1;
			}
			if (offset < 0) {
				offset = 0;
			}
		} catch (NumberFormatException nfo) {
			nColumns = 24;
			offset = 0;
		}
	}

	@Override
	public void itemStateChanged(ItemEvent ie) {
		if (chPeriod.getSelectedIndex() == 0) {
			timeBreak = ' ';
			getNColumnsAndOffset();
		} else {
			String str = chPeriod.getSelectedItem();
			if (str.equals("years")) {
				timeBreak = 'y';
			} else if (str.equals("months")) {
				timeBreak = 'm';
			} else if (str.equals("days")) {
				timeBreak = 'd';
			} else if (str.equals("hours")) {
				timeBreak = 'h';
			} else if (str.equals("minutes")) {
				timeBreak = 't';
			} else {
				timeBreak = ' ';
			}
		}
		update();
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		if (ae.getSource() instanceof TextField) {
			getNColumnsAndOffset();
			update();
		}
	}

}
