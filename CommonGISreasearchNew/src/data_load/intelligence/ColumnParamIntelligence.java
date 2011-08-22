package data_load.intelligence;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.DialogContent;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.NotificationLine;
import spade.lib.basicwin.TabbedPanel;
import spade.lib.basicwin.TextCanvas;
import spade.lib.lang.Language;
import spade.lib.util.IntArray;
import spade.lib.util.QSortAlgorithm;
import spade.vis.database.AttributeDataPortion;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 20-Jul-2004
 * Time: 11:42:57
 * In a dialog with a user, finds out what column(s) of a given table contain(s)
 * values of (a) non-temporal parameter(s).
 */
public class ColumnParamIntelligence extends Panel implements ActionListener, ItemListener, DialogContent {
	static ResourceBundle res = Language.getTextResource("data_load.intelligence.Res");
	/**
	 * The table in which indexing is done.
	 */
	protected AttributeDataPortion table = null;
	/**
	 * The list with column names
	 */
	protected List colNameList = null;
	/**
	 * The indexes of the columns included in the list (not all columns may be
	 * shown; in particular, columns containing time references are excluded
	 * from the further indexing)
	 */
	protected IntArray listColIndexes = null;
	/**
	 * The list with values retrieved from a selected column.
	 */
	protected List valList = null;
	/**
	 * Identifies whether values in the list of values must be sorted alphabetically
	 */
	protected Checkbox alphaCB = null;
	/**
	 * The vector of values from a selected column
	 */
	protected Vector values = null;
	/**
	 * The index of the column the values have been retrieved from.
	 */
	protected int colIdx = -1;
	/**
	 * The indexes of the selected columns with time references
	 */
	protected IntArray colIdxs = null;
	/**
	 * The error message to notify the user about errors in time reference
	 * definition.
	 */
	protected String err = null;
	/**
	 * The status line where this component may put its notifications (taken
	 * from the dialog or the system's UI
	 */
	protected NotificationLine lStatus = null;
	/**
	 * The panel where selected columns will be put for further describing.
	 */
	protected Panel paramPan = null;

	/**
	 * Constructs the UI for indexing and extracting non-temporal references from
	 * table columns.
	 * @param tbl            the table under indexing
	 * @param colsToExclude  indexes of columns which must be excluded from the
	 *                       consideration; in particular, columns with time
	 *                       references
	 */
	public ColumnParamIntelligence(AttributeDataPortion tbl, IntArray colsToExclude) {
		table = tbl;
		if (table == null || !table.hasData())
			return;
		Panel p = new Panel(new ColumnLayout());
		p.add(new Label(res.getString("Table_indexing_stage") + " 2 " + res.getString("of") + " 3", Label.CENTER));
		p.add(new Label(res.getString("Extr_col_params"), Label.CENTER));
		setLayout(new BorderLayout());
		add(p, BorderLayout.NORTH);

		Panel mainP = new Panel(new BorderLayout());
		TextCanvas tc = new TextCanvas();
		tc.setText(res.getString("Has_columns_with_params"));
		mainP.add(tc, BorderLayout.NORTH);
		Panel pp = new Panel(new BorderLayout());
		mainP.add(pp, BorderLayout.CENTER);
		p = new Panel(new GridLayout(1, 2, 0, 5));
		p.add(new Label(res.getString("Columns")));
		p.add(new Label(res.getString("Values")));
		pp.add(p, BorderLayout.NORTH);
		colNameList = new List(8);
		if (colsToExclude != null && colsToExclude.size() > 0) {
			listColIndexes = new IntArray(table.getAttrCount(), 1);
		} else {
			listColIndexes = null;
		}
		for (int i = 0; i < table.getAttrCount(); i++)
			if (listColIndexes == null) {
				colNameList.add(table.getAttributeName(i));
			} else if (colsToExclude.indexOf(i) < 0) {
				colNameList.add(table.getAttributeName(i));
				listColIndexes.addElement(i);
			}
		colNameList.addActionListener(this);
		colNameList.addItemListener(this);
		valList = new List(8);
		p = new Panel(new GridLayout(1, 2, 0, 5));
		p.add(colNameList);
		p.add(valList);
		pp.add(p, BorderLayout.CENTER);
		p = new Panel(new FlowLayout(FlowLayout.CENTER, 10, 2));
		Button b = new Button(res.getString("Select"));
		b.setActionCommand("Select");
		b.addActionListener(this);
		p.add(b);
		b = new Button(res.getString("Show_values"));
		b.setActionCommand("Show_values");
		b.addActionListener(this);
		p.add(b);
		alphaCB = new Checkbox(res.getString("Sort_alpha"), false);
		alphaCB.addItemListener(this);
		p.add(alphaCB);
		Panel bp = new Panel(new ColumnLayout());
		bp.add(p);
		pp.add(bp, BorderLayout.SOUTH);
		bp.add(new Line(false));
		bp.add(new Label(res.getString("Non_time_refs_are_in_col")));
		p = new Panel(new GridLayout(1, 5, 2, 2));
		p.add(new Label(res.getString("Col_name") + ":"));
		p.add(new Label(res.getString("Values") + ":"));
		p.add(new Label(res.getString("Sorting") + ":"));
		p.add(new Label(""));
		p.add(new Label(""));
		bp.add(p);
		paramPan = new Panel(new ColumnLayout());
		p.add(paramPan);
		Label l = new Label(res.getString("No_selection_yet"));
		l.setForeground(Color.red.darker());
		paramPan.add(l);
		bp.add(paramPan);

		TabbedPanel tpa = new TabbedPanel();
		tpa.addComponent(res.getString("Task"), mainP);
		tc = new TextCanvas();
		tc.addTextLine(res.getString("Param_expl_1"));
		tc.addTextLine(res.getString("Param_expl_2"));
		tc.addTextLine(res.getString("Param_expl_3"));
		tc.addTextLine(res.getString("Col_param_expl_1"));
		tc.addTextLine(res.getString("Col_param_expl_2"));
		tc.setPreferredSize(700, 200);
		tpa.addComponent(res.getString("Explanations"), tc);
		tc = new TextCanvas();
		tc.addTextLine(res.getString("Col_param_example1"));
		tc.addTextLine(res.getString("Col_param_example2"));
		tc.addTextLine(res.getString("Col_param_example3"));
		tc.setPreferredSize(700, 200);
		tpa.addComponent(res.getString("Examples"), tc);
		add(tpa, BorderLayout.CENTER);
		tpa.makeLayout();
	}

	public void setNotificationLine(NotificationLine notLine) {
		lStatus = notLine;
	}

	protected void putValuesInList() {
		valList.removeAll();
		if (values == null || values.size() < 1)
			return;
		if (values.size() > 1 && alphaCB.getState()) {
			Vector sorted = (Vector) values.clone();
			QSortAlgorithm.sort(sorted);
			for (int i = 0; i < sorted.size(); i++) {
				valList.add(sorted.elementAt(i).toString());
			}
		} else {
			for (int i = 0; i < values.size(); i++) {
				valList.add(values.elementAt(i).toString());
			}
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (lStatus != null) {
			lStatus.showMessage(null, false);
		}
		String cmd = e.getActionCommand();
		if (cmd == null) {
			cmd = "Select";
		}
		if (e.getSource().equals(colNameList) || cmd.equals("Select")) {
			//selection of a column with time references
			int idx = colNameList.getSelectedIndex();
			if (idx < 0)
				return;
			if (listColIndexes != null) {
				idx = listColIndexes.elementAt(idx);
			}
			if (colIdxs != null && colIdxs.indexOf(idx) >= 0)
				return; //already selected
			Vector vals = null;
			if (colIdx == idx && values != null && values.size() > 0) {
				vals = values;
			} else {
				IntArray iar = new IntArray(1, 1);
				iar.addElement(idx);
				vals = table.getAllValuesInColumnsAsStrings(iar);
			}
			OneParamPanel op = new OneParamPanel(table.getAttributeName(idx), idx, vals, this);
			Dimension d = paramPan.getPreferredSize();
			paramPan.setVisible(false);
			if (colIdxs == null || colIdxs.size() < 1) {
				paramPan.removeAll();
			}
			paramPan.add(op);
			paramPan.setVisible(true);
			CManager.invalidateAll(paramPan);
			Dimension d1 = paramPan.getPreferredSize();
			Window win = CManager.getWindow(this);
			if (win != null) {
				int h = d1.height - d.height;
				d = getSize();
				int w = d1.width - d.width;
				if (w < 0) {
					w = 0;
				}
				d = win.getSize();
				win.setSize(d.width + w, d.height + h);
				win.validate();
			} else {
				validate();
			}
			if (colIdxs == null) {
				colIdxs = new IntArray(10, 5);
			}
			colIdxs.addElement(idx);
		} else if (cmd.equals("Show_values")) {
			int idx = colNameList.getSelectedIndex();
			if (idx < 0)
				return;
			if (listColIndexes != null) {
				idx = listColIndexes.elementAt(idx);
			}
			if (idx == colIdx)
				return;
			valList.setVisible(false);
			valList.removeAll();
			colIdx = idx;
			IntArray iar = new IntArray(1, 1);
			iar.addElement(idx);
			values = table.getAllValuesInColumnsAsStrings(iar);
			if (values == null || values.size() < 1) {
				valList.add(res.getString("No_values_found"));
			} else {
				putValuesInList();
			}
			valList.setVisible(true);
		} else if (cmd.startsWith("remove_")) {
			cmd = cmd.substring(7);
			int idx = -1;
			try {
				idx = Integer.valueOf(cmd).intValue();
			} catch (NumberFormatException ex) {
			}
			if (idx < 0)
				return;
			int k = colIdxs.indexOf(idx);
			if (k < 0)
				return;
			colIdxs.removeElementAt(k);
			Dimension d = paramPan.getPreferredSize();
			paramPan.setVisible(false);
			paramPan.remove(k);
			if (colIdxs.size() < 1) {
				Label l = new Label(res.getString("No_selection_yet"));
				l.setForeground(Color.red.darker());
				paramPan.add(l);
			}
			paramPan.invalidate();
			paramPan.setVisible(true);
			CManager.invalidateAll(paramPan);
			Dimension d1 = paramPan.getPreferredSize();
			Window win = CManager.getWindow(this);
			if (win != null) {
				int h = d1.height - d.height;
				d = win.getSize();
				win.setSize(d.width, d.height + h);
				win.validate();
			} else {
				validate();
			}
		}
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if (lStatus != null) {
			lStatus.showMessage(null, false);
		}
		if (e.getSource().equals(alphaCB)) {
			if (values == null || values.size() < 2 || valList.getItemCount() < 2)
				return;
			valList.setVisible(false);
			putValuesInList();
			valList.setVisible(true);
		} else if (e.getSource().equals(colNameList)) {
			int idx = colNameList.getSelectedIndex();
			if (idx >= 0 && listColIndexes != null) {
				idx = listColIndexes.elementAt(idx);
			}
			if (idx < 0 || idx == colIdx)
				return;
			valList.setVisible(false);
			if (idx == colIdx)
				if (values == null || values.size() < 1) {
					valList.add(res.getString("No_values_found"));
				} else {
					putValuesInList();
				}
			else if (valList.getItemCount() > 0) {
				valList.removeAll();
			}
			valList.setVisible(true);
		}
	}

	@Override
	public boolean canClose() {
		if (colIdxs == null || colIdxs.size() < 1) {
			err = res.getString("no_param_column_selected");
			return false;
		}
		return true;
	}

	@Override
	public String getErrorMessage() {
		return err;
	}

	/**
	 * Returns indexes of the columns which contain non-temporal references
	 */
	public IntArray getParamColIndexes() {
		return colIdxs;
	}

	/**
	 * Constructs and returns a vector of description of the column parameter
	 * according to the user's selection and the states of the controls
	 */
	public Vector getParamDescriptions() {
		if (!canClose())
			return null;
		Vector result = new Vector(paramPan.getComponentCount(), 1);
		for (int i = 0; i < paramPan.getComponentCount(); i++)
			if (paramPan.getComponent(i) instanceof OneParamPanel) {
				OneParamPanel op = (OneParamPanel) paramPan.getComponent(i);
				result.addElement(op.getParamDescription());
			}
		if (result.size() < 1)
			return null;
		return result;
	}
}
