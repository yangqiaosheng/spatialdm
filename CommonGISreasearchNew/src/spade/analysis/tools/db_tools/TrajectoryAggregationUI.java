package spade.analysis.tools.db_tools;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Choice;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Line;
import spade.lib.util.IntArray;

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: 21-Mar-2007
 * Time: 15:49:50
 * To change this template use File | Settings | File Templates.
 */
public class TrajectoryAggregationUI extends Panel implements ActionListener {

	protected String columnNames[] = null;

	protected IntArray iaAttr = null, iaOper = null;

	public String[] getAttr() {
		if (iaAttr == null || iaAttr.size() == 0)
			return null;
		String st[] = new String[iaAttr.size()];
		for (int i = 0; i < st.length; i++) {
			st[i] = columnNames[iaAttr.elementAt(i)];
		}
		return st;
	}

	public int[] getOper() {
		return (iaOper == null || iaOper.size() == 0) ? null : iaOper.getTrimmedArray();
	}

	public Vector getQAttr() {
		if (lQual == null || lQual.getItemCount() == 0)
			return null;
		Vector v = null;
		for (int i = 0; i < lQual.getItemCount(); i++)
			if (lQual.isIndexSelected(i)) {
				if (v == null) {
					v = new Vector(lQual.getItemCount(), 100);
				}
				v.addElement(lQual.getItem(i));
			}
		return v;
	}

	protected Button bAdd = null, bRemove = null;
	protected List lNum = null, lQual = null;
	protected Choice chAttr = null, chOper = null;
	protected Checkbox defStatCB[] = null;

	public TrajectoryAggregationUI(String defaultStat[], boolean bHasTID, Vector vIntColumnNames, Vector vQualColumnNames) {
		super();
		columnNames = new String[vIntColumnNames.size()];
		for (int i = 0; i < columnNames.length; i++) {
			columnNames[i] = (String) vIntColumnNames.elementAt(i);
		}
		setLayout(new BorderLayout());
		int nDefStat = 0;
		if (defaultStat != null && defaultStat.length > 0) {
			nDefStat = defaultStat.length + 1;
		}
		Panel p = new Panel(new ColumnLayout());
		if (nDefStat > 0) {
			p.add(new Label("Default statistics:", Label.CENTER));
			defStatCB = new Checkbox[defaultStat.length];
			for (int i = 0; i < defaultStat.length; i++) {
				defStatCB[i] = new Checkbox(defaultStat[i], i > 2 || defaultStat.length == 1);
				p.add(defStatCB[i]);
				if (defaultStat[i].contains("trajectories") && !bHasTID) {
					defStatCB[i].setState(false);
					defStatCB[i].setEnabled(false);
				}
			}
			p.add(new Line(false));
		}
		p.add(new Label("Get additional statistics:", Label.CENTER));
		add(p, BorderLayout.NORTH);
		// numeric columns
		p = new Panel(new ColumnLayout());
		add(p, BorderLayout.CENTER);
		p.add(new Label("Numeric attributes:"));
		chAttr = new Choice();
		chOper = new Choice();
		p.add(chAttr);
		p.add(chOper);
		chAttr.add("Select attribute:");
		for (String columnName : columnNames) {
			chAttr.add(columnName);
		}
		String aopts[] = { "max", "min", "max-min", "average", "standard deviation", "median"/*,"median & quartiles","deciles"*/};
		chOper.add("Select operation:");
		for (String aopt : aopts) {
			chOper.add(aopt);
		}
		p.add(bAdd = new Button("Add"));
		bAdd.addActionListener(this);
		p.add(lNum = new List(10, true));
		p.add(bRemove = new Button("Remove"));
		bRemove.addActionListener(this);
		iaAttr = new IntArray();
		iaOper = new IntArray();
		// qualitative columns
		if (vQualColumnNames.size() > 0) {
			Panel pq = new Panel(new ColumnLayout());
			pq.add(new Label("String attributes:", Label.CENTER));
			pq.add(lQual = new List(10, true));
			for (int i = 0; i < vQualColumnNames.size(); i++) {
				lQual.add((String) vQualColumnNames.elementAt(i));
			}
			pq.add(new Label("(most frequent values)"));
			add(pq, BorderLayout.EAST);
		}
		add(new Line(false), BorderLayout.SOUTH);
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		if (ae.getSource().equals(bAdd))
			if (chAttr.getSelectedIndex() > 0 && chOper.getSelectedIndex() > 0) {
				iaAttr.addElement(chAttr.getSelectedIndex() - 1);
				iaOper.addElement(chOper.getSelectedIndex() - 1);
				lNum.add(chOper.getSelectedItem() + "(" + chAttr.getSelectedItem() + ")");
			}
		if (ae.getSource().equals(bRemove)) {
			int sel[] = lNum.getSelectedIndexes();
			for (int i = sel.length - 1; i >= 0; i--) {
				lNum.remove(sel[i]);
				iaAttr.removeElementAt(sel[i]);
				iaOper.removeElementAt(sel[i]);
			}
		}
	}

	public boolean isDefStatSelected(int idx) {
		if (defStatCB == null || idx < 0 || idx >= defStatCB.length)
			return false;
		return defStatCB[idx].getState();
	}

}
