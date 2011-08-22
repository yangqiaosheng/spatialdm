package spade.analysis.tools.db_tools.movement;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Choice;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Vector;

import spade.analysis.tools.db_tools.TimeDivisionUI;
import spade.lib.basicwin.ActiveFoldablePanel;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.DialogContent;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.NotificationLine;
import spade.lib.basicwin.OKDialog;
import spade.lib.util.StringUtil;
import spade.time.Date;
import spade.time.TimeCount;
import spade.time.TimeMoment;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.dataview.TableViewer;
import db_work.data_descr.ColumnDescriptor;
import db_work.data_descr.ColumnDescriptorDate;
import db_work.data_descr.TableDescriptor;
import db_work.database.OracleConnector;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 24-Jan-2007
 * Time: 13:02:47
 * Allows the user to specify the criteria for trajectory separation:
 * 1) Temporal gap (minimum time interval between successive measurements)
 * 2) Spatial gap (minimum distance in space between successive measurements)
 * 3) Temporal breaks
 */
public class SeparationCriteriaUI extends Panel implements ItemListener, ActionListener, PropertyChangeListener, DialogContent {
	public static final String timeUnits[] = { "years", "months", "days", "hours", "minutes", "seconds" };
	public static final String timeUnit[] = { "year", "month", "day", "hour", "minute", "second" };
	public static final char timeUnitSymbols[] = { 'y', 'm', 'd', 'h', 'm', 's' };
	/**
	 * Valid maximum values for each of the time components
	 */
	public static final int validMaxVals[] = { Integer.MAX_VALUE, 12, 31, 23, 59, 59 };
	/**
	 * The reader used for connection to the database and processing the data
	 */
	protected OracleConnector reader = null;

	protected String dateCName = null, idCName = null, diffTimeCName = null, nextDateCName = null, distanceCName = null;

	/**
	 * Column name with IDs of trajectories, or NULL if this is the 1st separation
	 */
	protected String trIdColumn = null;

	protected Checkbox timeGapCB = null, spaceGapCB = null, timeIntervalsCB = null;
	protected TextField timeGapTF = null, spaceGapTF = null, trajectNumTF = null;
	protected Choice timeUnitChoice = null;
	protected Button bFind = null, bDetails = null;
	protected ActiveFoldablePanel foldP = null;
	protected TimeDivisionUI timeDivUI = null;

	/**
	 * resulting logical condition for separating trajectories - SQL ready
	 */
	protected String condition = null, conditionAttr = null;

	public String getCondition() {
		return condition;
	}

	public String getConditionAttr() {
		return conditionAttr;
	}

	/**
	 * This is a reference to the notification line from the dialog, in which
	 * this panel is inserted. May be null.
	 */
	protected NotificationLine lStatus = null;
	/**
	 * The user-specified threshold value for the temporal gap
	 */
	protected int timeGap = 0;
	/**
	 * The user-specified threshold value for the spatial gap
	 */
	protected double spaceGap = 0.;

	protected String errMsg = null;

	public SeparationCriteriaUI(OracleConnector reader, String dateCName, String idCName, String diffTimeCName, String nextDateCName, String distanceCName, String trIdColumn) {
		this.reader = reader;
		this.dateCName = dateCName;
		this.idCName = idCName;
		this.diffTimeCName = diffTimeCName;
		this.nextDateCName = nextDateCName;
		this.distanceCName = distanceCName;
		this.trIdColumn = trIdColumn;

		//find the descriptor of the column with the dates/times
		ColumnDescriptorDate cdd = null;
		TableDescriptor td = reader.getTableDescriptor(reader.getNumOfTableDescriptors() - 1);
		for (int i = 0; i < td.getNColumns() && cdd == null; i++) {
			ColumnDescriptor cd = td.getColumnDescriptor(i);
			if (dateCName.equals(cd.name) && (cd instanceof ColumnDescriptorDate)) {
				cdd = (ColumnDescriptorDate) cd;
			}
		}

		Panel p = new Panel();
		GridBagLayout gridbag = new GridBagLayout();
		p.setLayout(gridbag);
		GridBagConstraints c = new GridBagConstraints();
		c.weightx = 1.0f;
		c.weighty = 1.0f;
		c.fill = GridBagConstraints.HORIZONTAL;
		Label l = new Label("Choose the criterion for separation", Label.CENTER);
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(l, c);
		p.add(l);
		CheckboxGroup cbg = new CheckboxGroup();
		if (diffTimeCName != null) {
			timeGapCB = new Checkbox("Temporal gap", false, cbg);
			timeGapCB.addItemListener(this);
			c.gridwidth = 2;
			gridbag.setConstraints(timeGapCB, c);
			p.add(timeGapCB);
			l = new Label("min=", Label.RIGHT);
			c.gridwidth = 1;
			gridbag.setConstraints(l, c);
			p.add(l);
			timeGapTF = new TextField(10);
			timeGapTF.setEnabled(false);
			gridbag.setConstraints(timeGapTF, c);
			p.add(timeGapTF);
			if (cdd == null) {
				Label label = new Label("units");
				c.gridwidth = GridBagConstraints.REMAINDER;
				gridbag.setConstraints(label, c);
				p.add(label);
			} else {
				timeUnitChoice = new Choice();
				for (String timeUnit2 : timeUnits) {
					timeUnitChoice.addItem(timeUnit2);
				}
				c.gridwidth = GridBagConstraints.REMAINDER;
				gridbag.setConstraints(timeUnitChoice, c);
				p.add(timeUnitChoice);
			}
			l = new Label("(minimum interval in time between successive measurements)");
			c.gridwidth = GridBagConstraints.REMAINDER;
			gridbag.setConstraints(l, c);
			p.add(l);
		}
		if (distanceCName != null) {
			spaceGapCB = new Checkbox("Spatial gap", false, cbg);
			spaceGapCB.addItemListener(this);
			c.gridwidth = 2;
			gridbag.setConstraints(spaceGapCB, c);
			p.add(spaceGapCB);
			l = new Label("min=", Label.RIGHT);
			c.gridwidth = 1;
			gridbag.setConstraints(l, c);
			p.add(l);
			spaceGapTF = new TextField(10);
			spaceGapTF.setEnabled(false);
			gridbag.setConstraints(spaceGapTF, c);
			p.add(spaceGapTF);
			l = new Label("");
			c.gridwidth = GridBagConstraints.REMAINDER;
			gridbag.setConstraints(l, c);
			p.add(l);
			l = new Label("(minimum distance in space between successive measurements)");
			c.gridwidth = GridBagConstraints.REMAINDER;
			gridbag.setConstraints(l, c);
			p.add(l);
		}
		setLayout(new BorderLayout(0, 5));
		add(p, BorderLayout.NORTH);

		Panel infop = new Panel(new ColumnLayout());
		add(infop, BorderLayout.CENTER);

		if (cdd != null) {
			timeIntervalsCB = new Checkbox("Time intervals", false, cbg);
			timeIntervalsCB.addItemListener(this);
			gridbag.setConstraints(timeIntervalsCB, c);
			p.add(timeIntervalsCB);
			timeDivUI = new TimeDivisionUI(cdd.min, cdd.max);
			timeDivUI.addPropertyChangeListener(this);
			foldP = new ActiveFoldablePanel(timeDivUI, null);
			foldP.setEnabled(false);
			infop.add(foldP);
		}

		infop.add(new Line(false));
		Panel pp = new Panel(new FlowLayout(FlowLayout.LEFT, 10, 0));
		infop.add(pp);
		pp.add(new Label("Estimated N of trajectories:"));
		trajectNumTF = new TextField("    ?    ");
		trajectNumTF.setEditable(false);
		pp.add(trajectNumTF);
		bFind = new Button("Find");
		bFind.addActionListener(this);
		pp.add(bFind);
		bDetails = new Button("Show details...");
		bDetails.addActionListener(this);
		bDetails.setEnabled(false);
		infop.add(bDetails);
	}

	/**
	 * Sets a reference to the notification line from the dialog, in which this
	 * panel is inserted.
	 */
	public void setStatusLine(NotificationLine lStatus) {
		this.lStatus = lStatus;
		if (timeDivUI != null) {
			timeDivUI.setNotificationLine(lStatus);
		}
	}

	/**
	 * Shows the given message in the status line, if any. If "trouble" is true,
	 * then this is an error message.
	 */
	public void showMessage(String msg, boolean trouble) {
		if (lStatus != null) {
			lStatus.showMessage(msg, trouble);
		}
	}

	public void itemStateChanged(ItemEvent e) {
		showMessage(null, false);
		if (timeGapCB != null) {
			timeGapTF.setEnabled(timeGapCB.getState());
		}
		if (spaceGapCB != null) {
			spaceGapTF.setEnabled(spaceGapCB.getState());
		}
		if (foldP != null) {
			boolean b = timeIntervalsCB != null && timeIntervalsCB.getState();
			foldP.setEnabled(b);
			if (b) {
				foldP.open();
			}
		}
		trajectNumTF.setText("    ?    ");
		bDetails.setEnabled(false);
	}

	public void actionPerformed(ActionEvent e) {
		showMessage(null, false);
		if (e.getSource().equals(bFind) || e.getSource().equals(bDetails)) {
			if (!canClose()) {
				showMessage(errMsg, true);
				trajectNumTF.setText("    ?    ");
				return;
			}
		}
		if (e.getSource().equals(bFind)) {
			showMessage("Database operation; please wait...", false);
			Cursor cur = null;
			Window w = CManager.getWindow(this);
			if (w != null) {
				cur = w.getCursor();
				w.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			}
			int n = reader.getCountOfTrajectories(reader.getTableDescriptor(reader.getNumOfTableDescriptors() - 1).tableName, idCName, dateCName, nextDateCName, trIdColumn, condition, conditionAttr);
			if (cur != null && w != null) {
				w.setCursor(cur);
			}
			showMessage(null, false);
			System.out.println("* n=" + n);
			trajectNumTF.setText("" + n);
			bDetails.setEnabled(n > 0 && n <= 1000);
		} else if (e.getSource().equals(bDetails)) {
			showMessage("Database operation; please wait...", false);
			Cursor cur = null;
			Window w = CManager.getWindow(this);
			if (w != null) {
				cur = w.getCursor();
				w.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			}
			Vector v = reader.getDetailsOfTrajectories(reader.getTableDescriptor(reader.getNumOfTableDescriptors() - 1).tableName, idCName, dateCName, nextDateCName, trIdColumn, condition, conditionAttr);
			if (cur != null && w != null) {
				w.setCursor(cur);
			}
			showMessage(null, false);
			if (v != null && v.size() > 0) {
				DataTable dt = new DataTable();
				dt.setEntitySetIdentifier("tmp-traj_sep");
				dt.addAttribute("Entity ID", "EID", AttributeTypes.character);
				dt.addAttribute("Duration", "Duration", AttributeTypes.real);
				dt.addAttribute("Start", "Start", AttributeTypes.character);
				dt.addAttribute("End", "End", AttributeTypes.character);
				for (int i = 0; i < v.size(); i++) {
					String s[] = (String[]) v.elementAt(i);
					DataRecord dr = new DataRecord(s[0]);
					dr.setAttrValue(s[1], 0);
					dr.setNumericAttrValue(Float.valueOf(s[4]).floatValue(), 1);
					dr.setAttrValue(s[2], 2);
					dr.setAttrValue(s[3], 3);
					dt.addDataRecord(dr);
				}
				TableViewer tv = new TableViewer(dt, null, this);
				tv.setTreatItemNamesAsNumbers(true);
				Vector attr = new Vector(4, 4);
				attr.addElement("EID");
				attr.addElement("Duration");
				attr.addElement("Start");
				attr.addElement("End");
				tv.setVisibleAttributes(attr);
				tv.setTableLens(true);
				OKDialog ok = new OKDialog(CManager.getAnyFrame(this), "Details of " + v.size() + " trajectories", false);
				ok.addContent(tv);
				ok.show();
				if (!ok.wasCancelled()) {
					;
				}
			}
		}
	}

	/**
	 * Reacts to changes of temporal breaks in timeDivUI
	 */
	public void propertyChange(PropertyChangeEvent e) {
		if (e.getSource().equals(timeDivUI) && e.getPropertyName().equals("breaks")) {
			trajectNumTF.setText("    ?    ");
		}
	}

	public boolean timeGapChosen() {
		return timeGapCB != null && timeGapCB.getState();
	}

	public boolean spaceGapChosen() {
		return spaceGapCB != null && spaceGapCB.getState();
	}

	public boolean timeIntervalsChosen() {
		return timeIntervalsCB != null && timeIntervalsCB.getState();
	}

	public int getTimeGap() {
		return timeGap;
	}

	public char getTimeGapUnit() {
		int idx = (timeUnitChoice == null) ? 2 : timeUnitChoice.getSelectedIndex();
		return timeUnitSymbols[idx];
	}

	public double getSpaceGap() {
		return spaceGap;
	}

	public String getErrorMessage() {
		return errMsg;
	}

	public boolean canClose() {
		condition = null;
		if (timeGapChosen()) {
			//check in the temporal distance threshold is specified
			String str = timeGapTF.getText();
			if (str == null || str.trim().length() < 1) {
				errMsg = "The temporal distance threshold is not specified!";
				return false;
			}
			try {
				int k = Integer.valueOf(str.trim()).intValue();
				if (k <= 0) {
					errMsg = "Invalid number entered as the temporal distance threshold!";
					return false;
				}
				timeGap = k;
			} catch (NumberFormatException nfe) {
				errMsg = "Not a number entered as the temporal distance threshold!";
				return false;
			}
			float f = timeGap;
			int idx = (timeUnitChoice == null) ? 2 : timeUnitChoice.getSelectedIndex();
			switch (idx) {
			case 0:
				f *= 365;
				break; // !
			case 1:
				f *= 31;
				break; // !
			case 2:
				break;
			case 3:
				f /= 24f;
				break;
			case 4:
				f /= (24f * 60);
				break;
			case 5:
				f /= (24f * 60 * 60);
				break;
			}
			conditionAttr = diffTimeCName;
			condition = diffTimeCName + " > " + f;
			System.out.println("* condition=<" + condition + ">");
		} else if (spaceGapChosen()) {
			//check in the spatial distance threshold is specified
			String str = spaceGapTF.getText();
			if (str == null || str.trim().length() < 1) {
				errMsg = "The spatial distance threshold is not specified!";
				return false;
			}
			try {
				double f = Double.valueOf(str.trim()).doubleValue();
				if (f <= 0) {
					errMsg = "Invalid number entered as the spatial distance threshold!";
					return false;
				}
				spaceGap = f;
			} catch (NumberFormatException nfe) {
				errMsg = "Not a number entered as the spatial distance threshold!";
				return false;
			}
			conditionAttr = distanceCName;
			condition = distanceCName + " > " + spaceGap;
			System.out.println("* condition=<" + condition + ">");
		} else if (timeIntervalsChosen()) {
			//check if any breaks are specified
			if (!timeDivUI.canClose()) {
				errMsg = timeDivUI.getErrorMessage();
				return false;
			}
			conditionAttr = nextDateCName;
			Vector v = timeDivUI.getTimeBreaks();
			switch (timeDivUI.getCycleIndex()) {
			case -1: // time line
				condition = "";
				for (int i = 0; i < v.size(); i++) {
					if (i > 0) {
						condition += " or ";
					}
					if (v.elementAt(i) instanceof Date) {
						Date dt = (Date) v.elementAt(i);
						String st = StringUtil.padString(((dt.hasElement('y')) ? "" + dt.getElementValue('y') : "2001"), '0', 4, true) + StringUtil.padString(((dt.hasElement('m')) ? "" + dt.getElementValue('m') : "1"), '0', 2, true)
								+ StringUtil.padString(((dt.hasElement('d')) ? "" + dt.getElementValue('d') : "1"), '0', 2, true) + StringUtil.padString(((dt.hasElement('h')) ? "" + dt.getElementValue('h') : "0"), '0', 2, true)
								+ StringUtil.padString(((dt.hasElement('t')) ? "" + dt.getElementValue('t') : "0"), '0', 2, true) + StringUtil.padString(((dt.hasElement('s')) ? "" + dt.getElementValue('s') : "0"), '0', 2, true);
						condition += dateCName + "<to_date(" + st + ",'yyyymmddhh24miss') and " + nextDateCName + ">=to_date(" + st + ",'yyyymmddhh24miss')";
					} else { // instanceof TimeCount
						String st = "" + ((TimeCount) v.elementAt(i)).getMoment();
						condition += dateCName + "<" + st + " and " + nextDateCName + ">=" + st;
					}
				}
				// dt<to_date('01012007000000','ddmmyyyyhh24miss') and nexttime>=to_date('01012007000000','ddmmyyyyhh24miss')
				break;
			case 0: // seconds in minute
				condition = "";
				for (int i = 0; i < v.size(); i++) {
					if (i > 0) {
						condition += " or ";
					}
					long lt = ((TimeCount) v.elementAt(i)).getMoment();
					condition += dateCName + "<to_date(nvl(to_char(" + nextDateCName + ",'dd'),'01') || nvl(to_char(" + nextDateCName + ",'mm'),'01') || nvl(to_char(" + nextDateCName + ",'YYYY'), '2000') || nvl(to_char(" + nextDateCName
							+ ",'hh24'),'00') || nvl(to_char(" + nextDateCName + ",'mi'),'00') || '" + StringUtil.padString("" + lt, '0', 2, true) + "','ddmmyyyyhh24miss') and " + nextDateCName + ">=to_date(nvl(to_char(" + nextDateCName
							+ ",'dd'),'01') || nvl(to_char(" + nextDateCName + ",'mm'),'01') || nvl(to_char(" + nextDateCName + ",'YYYY'),'2000') || nvl(to_char(" + nextDateCName + ",'hh24'),'00') || nvl(to_char(" + nextDateCName
							+ ",'mi'),'00') || '" + StringUtil.padString("" + lt, '0', 2, true) + "','ddmmyyyyhh24miss') or \n" + nextDateCName + "-" + dateCName + ">=1/24";
				}
				break;
			case 1: // minutes in hour
				condition = "";
				for (int i = 0; i < v.size(); i++) {
					if (i > 0) {
						condition += " or ";
					}
					long lt = ((TimeCount) v.elementAt(i)).getMoment();
					condition += dateCName + "<to_date(nvl(to_char(" + nextDateCName + ",'dd'),'01') || nvl(to_char(" + nextDateCName + ",'mm'),'01') || nvl(to_char(" + nextDateCName + ",'YYYY'),'2000') || nvl(to_char(" + nextDateCName
							+ ",'hh24'),'00') || '" + StringUtil.padString("" + lt, '0', 2, true) + "','ddmmyyyyhh24mi') and " + nextDateCName + ">=to_date(nvl(to_char(" + nextDateCName + ",'dd'),'01') || nvl(to_char(" + nextDateCName
							+ ",'mm'),'01') || nvl(to_char(" + nextDateCName + ",'YYYY'),'2000') || nvl(to_char(" + nextDateCName + ",'hh24'),'00') || '" + StringUtil.padString("" + lt, '0', 2, true) + "','ddmmyyyyhh24mi') or " + nextDateCName + "-"
							+ dateCName + ">=1/24";
				}
				break;
			case 2: // hours in day
				condition = "";
				for (int i = 0; i < v.size(); i++) {
					if (i > 0) {
						condition += " or ";
					}
					long lt = ((TimeCount) v.elementAt(i)).getMoment();
					condition += dateCName + "<to_date(nvl(to_char(" + nextDateCName + ",'dd'),'01') || nvl(to_char(" + nextDateCName + ",'mm'),'01') || nvl(to_char(" + nextDateCName + ",'YYYY'),'2000') || '"
							+ StringUtil.padString("" + lt, '0', 2, true) + "','ddmmyyyyhh24') and " + nextDateCName + ">=to_date(nvl(to_char(" + nextDateCName + ",'dd'),'01') || nvl(to_char(" + nextDateCName + ",'mm'),'01') || nvl(to_char("
							+ nextDateCName + ",'YYYY'),'2000') || '" + StringUtil.padString("" + lt, '0', 2, true) + "','ddmmyyyyhh24') or \n" + nextDateCName + "-" + dateCName + ">=1";
				}
				break;
			case 3: // days in week
				condition = "";
				for (int i = 0; i < v.size(); i++) {
					if (i > 0) {
						condition += " or ";
					}
					long lt = ((TimeCount) v.elementAt(i)).getMoment();
					String dw = "";
					switch ((int) lt) {
					case 1:
						dw = "monday";
						break;
					case 2:
						dw = "tuesday";
						break;
					case 3:
						dw = "wednesday";
						break;
					case 4:
						dw = "thursday";
						break;
					case 5:
						dw = "friday";
						break;
					case 6:
						dw = "saturday";
						break;
					case 7:
						dw = "sunday";
						break;
					}
					condition += "trunc(next_day(" + dateCName + ",'" + dw + "'),'j')<trunc(next_day(" + nextDateCName + ",'" + dw + "'),'j')";
					// trunc(next_day(dt,'monday'),'j')<trunc(next_day(nexttime,'monday'),'j')
				}
				break;
			case 4: // months in year
				condition = "";
				for (int i = 0; i < v.size(); i++) {
					if (i > 0) {
						condition += " or ";
					}
					long lt = ((TimeCount) v.elementAt(i)).getMoment();
					condition += dateCName + "<to_date('01" + StringUtil.padString("" + lt, '0', 2, true) + "' || nvl(to_char(" + nextDateCName + ",'YYYY'),'2000'),'DDMMYYYY') and " + nextDateCName + ">=to_date('01"
							+ StringUtil.padString("" + lt, '0', 2, true) + "' || nvl(to_char(" + nextDateCName + ",'YYYY'),'2000'),'DDMMYYYY')";
					condition += "\nor\n" + dateCName + "<to_date('01" + StringUtil.padString("" + lt, '0', 2, true) + "' || nvl(to_char(" + dateCName + ",'YYYY'),'2000'),'DDMMYYYY') and " + nextDateCName + ">=to_date('01"
							+ StringUtil.padString("" + lt, '0', 2, true) + "' || nvl(to_char(" + dateCName + ",'YYYY'),'2000'),'DDMMYYYY')";
					// dt<to_date('0101' || to_char(nexttime,'YYYY'),'DDMMYYYY') and nexttime>=to_date('0101' || to_char(nexttime,'YYYY'),'DDMMYYYY')
					// or
					// nexttime_-dt_>=365
				}
				break;
			}
			System.out.println("* condition=<" + condition + ">");
		} else {
			errMsg = "None of the possible criteria is chosen!";
			return false;
		}
		return true;
	}

	/**
	 * Returns the time breaks specified, instances of TimeMoment (i.e. either
	 * Date or TimeCount)
	 */
	public Vector getTimeBreaks() {
		if (timeDivUI != null)
			return timeDivUI.getTimeBreaks();
		return null;
	}

	/**
	 * Informs whether the division is done according to the cyclical time model
	 */
	public boolean useCycle() {
		if (timeDivUI != null)
			return timeDivUI.useCycle();
		return false;
	}

	/**
	 * Returns the index of the selected time cycle in the array
	 * TimeDivisionUI.cycles (as well as TimeDivisionUI.cycleElements,
	 * TimeDivisionUI.nCycleElements, etc.)
	 */
	public int getCycleIndex() {
		if (timeDivUI != null)
			return timeDivUI.getCycleIndex();
		return -1;
	}

	/**
	 * Depending on the selected temporal cycle, returns one of the symbols
	 * 's','t','h','d', or 'm', which mean, respectively,
	 * seconds in minute, minutes in hour, hours in day, days in week,
	 * or months in year
	 */
	public char getCycleUnit() {
		if (timeDivUI != null)
			return timeDivUI.getCycleUnit();
		return 0;
	}

	/**
	 * Returns the starting moment of the division (which may be different from
	 * the minimum moment in the data) 
	 */
	public TimeMoment getStart() {
		if (timeDivUI != null)
			return timeDivUI.getStart();
		return null;
	}
}
