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
import java.awt.TextField;
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
import spade.lib.basicwin.OKDialog;
import spade.lib.basicwin.TabbedPanel;
import spade.lib.basicwin.TextCanvas;
import spade.lib.lang.Language;
import spade.lib.util.IntArray;
import spade.lib.util.QSortAlgorithm;
import spade.time.Date;
import spade.time.TimeCount;
import spade.time.TimeMoment;
import spade.time.TimeRefDescription;
import spade.vis.database.AttributeDataPortion;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 13-Jul-2004
 * Time: 10:56:10
 * In a dialog with a user, finds out what column(s) of a given table contain(s)
 * temporal references.
 */
public class TimeRefIntelligence extends Panel implements ActionListener, ItemListener, DialogContent {
	static ResourceBundle res = Language.getTextResource("data_load.intelligence.Res");
	/**
	 * The table in which indexing is done.
	 */
	protected AttributeDataPortion table = null;
	/**
	 * The text field for entering the name of the resulting column.
	 */
	protected TextField colNameTF = null;
	/**
	 * The text field for entering the scheme for showing the resultind dates/times
	 * to the user
	 */
	protected TextField schemeTF = null;
	/**
	 * Identifies whether the temporal references are parameters
	 */
	protected Checkbox isParamCB = null;
	/**
	 * For a temporal parameter, indicates whether missing values of parameter-
	 * dependent attributes should be filled with the last known values
	 */
	protected Checkbox protractCB = null;
	/**
	 * Indicates whether the original column(s) with times or time components
	 * should be preserved in the table after transforming the times into
	 * time objects. When the times are parameters, the original columns cannot be
	 * preserved.
	 */
	protected Checkbox keepOrigCB = null;
	/**
	 * The list with column names
	 */
	protected List colNameList = null;
	/**
	 * The list with values retrieved from a selected column.
	 */
	protected List valList = null;
	/**
	 * Identifies whether values in the list of values must be sorted alphabetically
	 */
	protected Checkbox alphaCB = null;
	/**
	 * The panel where selected columns will be put for further describing.
	 */
	protected Panel timeRefPan = null;
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
	 * Constructs the UI for indexing and extracting temporal references.
	 */
	public TimeRefIntelligence(AttributeDataPortion tbl) {
		table = tbl;
		if (table == null || !table.hasData())
			return;
		Panel p = new Panel(new ColumnLayout());
		p.add(new Label(res.getString("Table_indexing_stage") + " 1 " + res.getString("of") + " 3", Label.CENTER));
		p.add(new Label(res.getString("Extr_time_ref"), Label.CENTER));
		setLayout(new BorderLayout());
		add(p, BorderLayout.NORTH);

		Panel mainP = new Panel(new BorderLayout());
		TextCanvas tc = new TextCanvas();
		tc.setText(res.getString("Has_columns_with_time_ref"));
		mainP.add(tc, BorderLayout.NORTH);
		p = new Panel(new ColumnLayout());
		p.add(new Line(false));
		Panel pp = new Panel(new BorderLayout());
		pp.add(new Label(res.getString("Result_col_name")), BorderLayout.WEST);
		colNameTF = new TextField();
		pp.add(colNameTF, BorderLayout.CENTER);
		p.add(pp);
		pp = new Panel(new BorderLayout());
		isParamCB = new Checkbox(res.getString("This_is_param"), true);
		pp.add(isParamCB, BorderLayout.WEST);
		isParamCB.addItemListener(this);
		keepOrigCB = new Checkbox(res.getString("Keep_orig_columns"), false);
		keepOrigCB.setEnabled(false);
		pp.add(keepOrigCB, BorderLayout.EAST);
		p.add(pp);
		protractCB = new Checkbox(res.getString("protract_values"), false);
		p.add(protractCB);
		p.add(new Label("(" + res.getString("explain_protract") + ")"));
		pp = new Panel(new BorderLayout());
		pp.add(new Label(res.getString("Result_template")), BorderLayout.WEST);
		schemeTF = new TextField();
		pp.add(schemeTF, BorderLayout.CENTER);
		Button b = new Button(res.getString("Retrieve_times"));
		b.setActionCommand("Retrieve_times");
		b.addActionListener(this);
		pp.add(b, BorderLayout.EAST);
		p.add(pp);
		mainP.add(p, BorderLayout.SOUTH);
		pp = new Panel(new BorderLayout());
		mainP.add(pp, BorderLayout.CENTER);
		p = new Panel(new GridLayout(1, 2, 0, 5));
		p.add(new Label(res.getString("Columns")));
		p.add(new Label(res.getString("Values")));
		pp.add(p, BorderLayout.NORTH);
		colNameList = new List(8);
		for (int i = 0; i < table.getAttrCount(); i++) {
			colNameList.add(table.getAttributeName(i));
		}
		colNameList.addActionListener(this);
		colNameList.addItemListener(this);
		valList = new List(8);
		p = new Panel(new GridLayout(1, 2, 0, 5));
		p.add(colNameList);
		p.add(valList);
		pp.add(p, BorderLayout.CENTER);
		Panel bp = new Panel(new ColumnLayout());
		pp.add(bp, BorderLayout.SOUTH);
		p = new Panel(new FlowLayout(FlowLayout.CENTER, 10, 2));
		b = new Button(res.getString("Select"));
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
		bp.add(p);
		bp.add(new Line(false));
		bp.add(new Label(res.getString("Time_refs_are_in_col")));
		p = new Panel(new GridLayout(1, 4, 2, 2));
		p.add(new Label(res.getString("Col_name") + ":"));
		p.add(new Label(res.getString("Format") + ":"));
		p.add(new Label(res.getString("Meaning_or_template") + ":"));
		p.add(new Label(""));
		bp.add(p);
		timeRefPan = new Panel(new ColumnLayout());
		p.add(timeRefPan);
		Label l = new Label(res.getString("No_selection_yet"));
		l.setForeground(Color.red.darker());
		timeRefPan.add(l);
		bp.add(timeRefPan);

		TabbedPanel tpa = new TabbedPanel();
		tpa.addComponent(res.getString("Task"), mainP);
		tc = new TextCanvas();
		tc.addTextLine(res.getString("Time_ref_expl1"));
		tc.addTextLine(res.getString("Time_ref_expl2"));
		tc.addTextLine(res.getString("Time_ref_expl3"));
		tc.addTextLine(res.getString("Time_ref_expl4"));
		tc.setPreferredSize(700, 200);
		tpa.addComponent(res.getString("Explanations"), tc);
		tc = new TextCanvas();
		tc.addTextLine(res.getString("Time_ref_example1"));
		tc.addTextLine(res.getString("Time_ref_example2"));
		tc.addTextLine(res.getString("Time_ref_example3"));
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
			if (colIdxs != null && colIdxs.indexOf(idx) >= 0)
				return; //already selected
			OneTimeRefPanel op = new OneTimeRefPanel(table.getAttributeName(idx), idx, this);
			Dimension d = timeRefPan.getPreferredSize();
			timeRefPan.setVisible(false);
			if (colIdxs == null || colIdxs.size() < 1) {
				timeRefPan.removeAll();
			}
			timeRefPan.add(op);
			timeRefPan.setVisible(true);
			CManager.invalidateAll(timeRefPan);
			Dimension d1 = timeRefPan.getPreferredSize();
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
			if (idx < 0 || idx == colIdx)
				return;
			valList.setVisible(false);
			valList.removeAll();
			colIdx = idx;
			IntArray iar = new IntArray(1, 1);
			iar.addElement(idx);
			values = table.getKValuesFromColumnsAsStrings(iar, 50);
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
			Dimension d = timeRefPan.getPreferredSize();
			timeRefPan.setVisible(false);
			timeRefPan.remove(k);
			if (colIdxs.size() < 1) {
				Label l = new Label(res.getString("No_selection_yet"));
				l.setForeground(Color.red.darker());
				timeRefPan.add(l);
			}
			timeRefPan.invalidate();
			timeRefPan.setVisible(true);
			CManager.invalidateAll(timeRefPan);
			Dimension d1 = timeRefPan.getPreferredSize();
			Window win = CManager.getWindow(this);
			if (win != null) {
				int h = d1.height - d.height;
				d = win.getSize();
				win.setSize(d.width, d.height + h);
				win.validate();
			} else {
				validate();
			}
		} else if (cmd.equals("Retrieve_times")) {
			retrieveDates();
		}
	}

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
		} else if (e.getSource().equals(isParamCB)) {
			if (isParamCB.getState()) {
				keepOrigCB.setState(false);
				keepOrigCB.setEnabled(false);
				protractCB.setEnabled(true);
			} else {
				keepOrigCB.setEnabled(true);
				protractCB.setState(false);
				protractCB.setEnabled(false);
			}
		}
	}

	public boolean canClose() {
		if (colIdxs == null || colIdxs.size() < 1) {
			err = res.getString("no_time_ref_column_selected");
			return false;
		}
		boolean used[] = new boolean[Date.time_symbols.length];
		boolean isAbstract = false;
		for (int i = 0; i < used.length; i++) {
			used[i] = false;
		}
		for (int i = 0; i < timeRefPan.getComponentCount(); i++)
			if (timeRefPan.getComponent(i) instanceof OneTimeRefPanel) {
				OneTimeRefPanel op = (OneTimeRefPanel) timeRefPan.getComponent(i);
				err = op.check();
				if (err != null)
					return false;
				String scheme = op.getScheme();
				if (scheme.equals("a")) {
					isAbstract = true;
					if (timeRefPan.getComponentCount() > 1) {
						err = res.getString("Abstract_only_in_one_column");
						return false;
					}
				} else {
					for (int j = 0; j < Date.time_symbols.length; j++)
						if (scheme.indexOf(Date.time_symbols[j]) >= 0)
							if (used[j]) {
								err = res.getString("Repeated_occurrence_of") + " " + Date.getTextForTimeSymbol(Date.time_symbols[j]) + " " + res.getString("in_more_than_one_columns") + "!";
								return false;
							} else {
								used[j] = true;
							}
				}
			}
		if (!isAbstract) {
			for (int i = 0; i < used.length - 2; i++)
				if (used[i] && !used[i + 1]) {
					int next = -1;
					for (int j = i + 2; j < used.length && next < 0; j++)
						if (used[j]) {
							next = j;
						}
					if (next < 0) {
						continue;
					}
					err = res.getString("Elements") + " \"" + Date.getTextForTimeSymbol(Date.time_symbols[i]) + "\" " + res.getString("and") + " \"" + Date.getTextForTimeSymbol(Date.time_symbols[next]) + "\" " + res.getString("but_no") + " ";
					for (int j = i + 1; j < next; j++) {
						err += "\"" + Date.getTextForTimeSymbol(Date.time_symbols[j]) + ((j < next - 1) ? "\", " : "\" ");
					}
					err += res.getString("found") + "!";
					return false;
				}
		}
		String scheme = schemeTF.getText();
		if (scheme == null)
			return true;
		scheme = scheme.trim();
		if (scheme.length() < 1)
			return true;
		if (isAbstract) {
			schemeTF.setText("");
			err = res.getString("No_templ_for_abstract");
			return false;
		}
		scheme = scheme.toLowerCase();
		err = Date.checkTemplateValidity(scheme);
		if (err != null)
			return false;
		for (int i = 0; i < used.length; i++) {
			int idx = scheme.indexOf(Date.time_symbols[i]);
			if (idx >= 0)
				if (!used[i]) {
					err = res.getString("Element") + " \"" + Date.getTextForTimeSymbol(Date.time_symbols[i]) + "\" " + res.getString("absent_in_table_but_occurs_in_template") + "!";
					return false;
				} else {
					;
				}
			else if (used[i]) {
				err = res.getString("Resulting_template_has_no_positions_for") + " \"" + Date.getTextForTimeSymbol(Date.time_symbols[i]) + "\"!";
				return false;
			}
		}
		return true;
	}

	public String getErrorMessage() {
		return err;
	}

	/**
	 * From the information contained in the dialog controls, constructs a
	 * specification for building time references
	 */
	public TimeRefDescription getTimeRefDescription() {
		if (!canClose())
			return null;
		TimeRefDescription trd = new TimeRefDescription();
		trd.attrName = colNameTF.getText();
		if (trd.attrName != null) {
			trd.attrName = trd.attrName.trim();
			if (trd.attrName.length() < 1) {
				trd.attrName = null;
			}
		}
		if (trd.attrName == null) {
			trd.attrName = table.getAttributeName(colIdxs.elementAt(0));
			colNameTF.setText(trd.attrName);
		}
		trd.sourceColumns = new String[colIdxs.size()];
		for (int i = 0; i < colIdxs.size(); i++) {
			trd.sourceColumns[i] = table.getAttributeName(colIdxs.elementAt(i));
		}
		trd.schemes = new String[colIdxs.size()];
		for (int i = 0; i < timeRefPan.getComponentCount(); i++)
			if (timeRefPan.getComponent(i) instanceof OneTimeRefPanel) {
				OneTimeRefPanel op = (OneTimeRefPanel) timeRefPan.getComponent(i);
				trd.schemes[i] = op.getScheme();
			}
		trd.attrScheme = schemeTF.getText();
		if (trd.attrScheme != null) {
			trd.attrScheme = trd.attrScheme.trim();
			if (trd.attrScheme.length() < 1) {
				trd.attrScheme = null;
			} else {
				trd.attrScheme = trd.attrScheme.toLowerCase();
			}
		}
		trd.isParameter = isParamCB.getState();
		trd.keepOrigColumns = keepOrigCB.getState();
		trd.protractKnownValues = protractCB.getState();
		return trd;
	}

	/**
	 * Returns indexes of the columns which contain time references
	 */
	public IntArray getTimeRefColIndexes() {
		return colIdxs;
	}

	/**
	 * Tries to transform values retrieved from columns into dates/times according
	 * to the given specification
	 */
	protected void retrieveDates() {
		TimeRefDescription td = getTimeRefDescription();
		if (td == null) {
			if (lStatus != null) {
				lStatus.showMessage(res.getString("No_valid_description"), true);
			}
			return;
		}
		int cn[] = new int[colIdxs.size()];
		for (int i = 0; i < colIdxs.size(); i++) {
			cn[i] = colIdxs.elementAt(i);
		}
		//check each scheme whether it is simple (i.e. contains a single count)
		//or compound
		boolean isSimple[] = new boolean[cn.length];
		for (int k = 0; k < cn.length; k++) {
			int nsymb = 0;
			for (int j = 0; j < TimeRefDescription.TIME_SYMBOLS.length && nsymb < 2; j++)
				if (td.schemes[k].indexOf(TimeRefDescription.TIME_SYMBOLS[j]) >= 0) {
					++nsymb;
				}
			isSimple[k] = nsymb < 2;
			//check if the scheme contains any symbols other than the legal date/time
			//symbols
			for (int j = 0; j < td.schemes[k].length() && isSimple[k]; j++) {
				isSimple[k] = TimeRefDescription.isTimeSymbol(td.schemes[k].charAt(j));
			}
		}
		boolean simpleDate = cn.length == 1 && isSimple[0];
		//these Date structures will be used for transforming strings to dates
		Date utilDates[] = null;
		if (!simpleDate) {
			utilDates = new Date[cn.length];
			for (int j = 0; j < cn.length; j++)
				if (!isSimple[j]) {
					utilDates[j] = new Date();
					utilDates[j].setDateScheme(td.schemes[j]);
				}
		}
		//create time moments from strings
		String dateScheme = td.attrScheme; //the scheme of the dates to be formed
		TimeMoment times[] = new TimeMoment[table.getDataItemCount()];
		for (int j = 0; j < table.getDataItemCount(); j++) {
			times[j] = null;
			boolean missing = false;
			for (int k = 0; k < cn.length && !missing; k++) {
				String val = table.getAttrValueAsString(cn[k], j);
				missing = (val == null || val.length() < 1);
				if (missing) {
					break;
				}
				if (simpleDate) {
					times[j] = new TimeCount();
					missing = !times[j].setMoment(val);
				} else {
					if (utilDates[k] != null) {
						missing = !utilDates[k].setMoment(val);
						if (!missing) {
							Date d = null;
							if (cn.length == 1) {
								d = (Date) utilDates[k].getCopy();
							} else {
								if (times[j] != null) {
									d = (Date) times[j];
								} else {
									d = new Date();
								}
								for (char elem : TimeRefDescription.TIME_SYMBOLS) {
									if (utilDates[k].hasElement(elem)) {
										d.setElementValue(elem, utilDates[k].getElementValue(elem));
									}
								}
							}
							if (times[j] == null) {
								times[j] = d;
							}
						}
					} else {
						//transform the string into a number and set the corresponding date element
						try {
							int num = Integer.valueOf(val).intValue();
							Date d = (times[j] != null) ? (Date) times[j] : new Date();
							for (char elem : TimeRefDescription.TIME_SYMBOLS) {
								if (td.schemes[k].indexOf(elem) >= 0) {
									d.setElementValue(elem, num);
									break;
								}
							}
							if (times[j] == null) {
								times[j] = d;
							}
						} catch (NumberFormatException nfe) {
							missing = true;
						}
					}
				}
			}
			if (missing) {
				continue;
			}
			if (times[j] != null) {
				if (times[j] instanceof Date) {
					Date d = (Date) times[j];
					if (dateScheme != null) {
						d.setDateScheme(dateScheme);
					} else if (d.scheme == null) {
						d.setDefaultScheme();
						dateScheme = d.scheme;
					}
				}
				//System.out.println(tm.toString());
			}
		}
		//retrieve different dates
		Vector diffDates = new Vector((td.isParameter) ? 50 : times.length, 50);
		int nFailed = 0;
		for (TimeMoment time : times)
			if (time == null) {
				++nFailed;
			} else if (diffDates.indexOf(time) < 0) {
				diffDates.addElement(time);
			}
		if (diffDates.size() < 1) {
			if (lStatus != null) {
				lStatus.showMessage(res.getString("Failed_retrieve_times"), true);
			}
			return;
		}
		if (diffDates.size() <= times.length / 2 && !isParamCB.getState()) {
			isParamCB.setState(true);
			keepOrigCB.setState(false);
			keepOrigCB.setEnabled(false);
		}
		if (diffDates.size() > 1) {
			QSortAlgorithm.sort(diffDates);
		}
		Panel p = new Panel(new ColumnLayout());
		if (diffDates.size() < 2) {
			p.add(new Label(res.getString("Retrieved") + " 1 " + res.getString("time_reference") + ": " + diffDates.elementAt(0).toString()));
		} else {
			p.add(new Label(res.getString("Retrieved") + " " + diffDates.size() + " " + res.getString("different_time_references")));
			p.add(new Label(res.getString("Time_range") + ": " + res.getString("from") + " " + diffDates.elementAt(0).toString() + " " + res.getString("to") + " " + diffDates.elementAt(diffDates.size() - 1).toString()));
		}
		if (nFailed > 0) {
			Label l = new Label(res.getString("Failed_to_retrieve_in") + " " + nFailed + " " + res.getString("cases_of") + " " + times.length);
			l.setForeground(Color.red.darker());
			p.add(l);
		} else {
			p.add(new Label(res.getString("All_orig_values_processed")));
		}
		Panel pp = new Panel(new GridLayout(1, 2, 5, 0));
		pp.add(new Label(res.getString("Diff_times")));
		pp.add(new Label(res.getString("Transformation_results")));
		p.add(pp);
		pp = new Panel(new BorderLayout());
		pp.add(p, BorderLayout.NORTH);
		p = new Panel(new GridLayout(1, 2, 5, 0));
		List lst = new List(10);
		for (int i = 0; i < diffDates.size(); i++) {
			lst.add(diffDates.elementAt(i).toString());
		}
		p.add(lst);
		lst = new List(10);
		for (int i = 0; i < times.length; i++) {
			String str = null;
			if (times[i] == null) {
				str = "***ERROR";
			} else {
				str = times[i].toString();
			}
			str += " <-- ";
			for (int k = 0; k < cn.length; k++) {
				String val = table.getAttrValueAsString(cn[k], i);
				if (val == null || val.length() < 1) {
					str += "null";
				} else {
					str += val;
				}
				if (k < cn.length - 1) {
					str += ", ";
				}
			}
			lst.add(str);
		}
		p.add(lst);
		pp.add(p, BorderLayout.CENTER);
		OKDialog okd = new OKDialog(CManager.getAnyFrame(this), res.getString("Transformation_results"), false);
		okd.addContent(pp);
		okd.show();
	}
}
