package spade.analysis.tools.db_tools;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Choice;
import java.awt.Cursor;
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
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.StringTokenizer;
import java.util.Vector;

import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.DialogContent;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.NotificationLine;
import spade.lib.basicwin.NotificationLineManager;
import spade.lib.basicwin.OKDialog;
import spade.lib.basicwin.RowLayout;
import spade.lib.basicwin.TextCanvas;
import spade.time.Date;
import spade.time.TimeCount;
import spade.time.TimeMoment;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 27-Dec-2006
 * Time: 17:42:27
 * Allows the user to specify a division of a time period into intervals
 * according to the linear or cyclical time model.
 */
public class TimeDivisionUI extends Panel implements ActionListener, ItemListener, DialogContent, NotificationLineManager {
	public static final String cycles[] = { "seconds in minute", "minutes in hour", "hours in day", "days in week", "months in year" };
	public static final char cycleElements[] = { 's', 't', 'h', 'd', 'm' };
	public static final int nCycleElements[] = { 60, 60, 24, 7, 12 };
	public static final int minCycleElement[] = { 0, 0, 0, 1, 1 };
	public static final String units[] = { "year(s)", "month(s)", "day(s)", "hour(s)", "minute(s)", "second(s)" };
	public static final char unitSymbols[] = { 'y', 'm', 'd', 'h', 't', 's' };
	public static final String daysOfWeek[] = { "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday" };
	public static final String dateSchemes[] = { "yyyy-mm-dd,hh:tt:ss", "dd-mm-yyyy,hh:tt:ss", "mm-dd-yyyy,hh:tt:ss", "yyyy-mm-dd,hh:tt", "dd-mm-yyyy,hh:tt", "mm-dd-yyyy,hh:tt", "yyyy-mm-dd", "dd-mm-yyyy", "mm-dd-yyyy", "hh:tt:ss", "hh:tt",
			"yy-mm-dd,hh:tt:ss", "dd-mm-yy,hh:tt:ss", "mm-dd-yy,hh:tt:ss", "yy-mm-dd", "dd-mm-yy", "mm-dd-yy" };

	protected String dataStartStr = null, dataEndStr = null;

	protected TextField fromTF = null, toTF = null, breakTF = null, iNumTF = null, iLenTF = null;
	protected TextField dayOfWeekFromTF = null, dayOfWeekToTF = null;
	protected Checkbox lineCB = null, cycleCB = null;
	protected Choice schemeCh = null, cycleCh = null, unitCh = null;
	protected List brList = null;
	/**
	 * This checkbox adjusts the start date appropriately to the selected unit
	 */
	protected Checkbox adjCB = null;
	/**
	 * The breaks defined, instances of TimeMoment (i.e. either Date or TimeCount)
	 */
	protected Vector timeBreaks = null;

	protected int lastSchemeIdx = -1;
	/**
	 * Explains why cannot be closed
	 */
	protected String err = null;
	/**
	 * The notification line for messages
	 */
	protected NotificationLine lStatus = null;
	/**
	 * Used for the notification of possible listeners about changes of the
	 * temporal breaks
	 */
	protected PropertyChangeSupport pcSupport = null;

	public TimeDivisionUI(String startTime, String endTime) {
		setLayout(new BorderLayout());
		Panel pan = new Panel(new ColumnLayout());
		boolean haveDates = false;
		if (startTime != null && endTime != null) {
			dataStartStr = startTime;
			dataEndStr = endTime;
			pan.add(new Label("Time span in the data:", Label.CENTER));
			Panel pp = new Panel(new GridLayout(1, 2));
			Panel p = new Panel(new BorderLayout());
			p.add(new Label("from", Label.RIGHT), BorderLayout.WEST);
			TextField tf = new TextField(startTime);
			tf.setEditable(false);
			p.add(tf, BorderLayout.CENTER);
			pp.add(p);
			p = new Panel(new BorderLayout());
			p.add(new Label("to", Label.RIGHT), BorderLayout.WEST);
			tf = new TextField(endTime);
			tf.setEditable(false);
			p.add(tf, BorderLayout.CENTER);
			pp.add(p);
			pan.add(pp);
			//check if startTime and endTime are dates or abstract time counts
			StringTokenizer st = new StringTokenizer(startTime, " ./-:;,\t");
			int nNum = 0;
			while (st.hasMoreTokens()) {
				String str = st.nextToken();
				try {
					int k = Integer.valueOf(str).intValue();
					if (k >= 0) {
						++nNum;
					}
				} catch (NumberFormatException e) {
				}
			}
			haveDates = nNum > 1;
			if (haveDates) {
				schemeCh = new Choice();
				schemeCh.addItemListener(this);
				for (int i = 0; i < dateSchemes.length; i++) {
					schemeCh.add(dateSchemes[i]);
					if (lastSchemeIdx < 0 && dateSchemes[i].length() == startTime.length()) {
						st = new StringTokenizer(startTime, " ./-:;,\t");
						StringTokenizer st0 = new StringTokenizer(dateSchemes[i], " ./-:;,\t");
						boolean similar = true;
						while (similar && st.hasMoreTokens() && st0.hasMoreTokens()) {
							String s1 = st.nextToken(), s0 = st0.nextToken();
							similar = s1.length() == s0.length();
						}
						if (similar && !st.hasMoreTokens() && !st0.hasMoreTokens()) {
							lastSchemeIdx = i;
						}
					}
				}
				schemeCh.add("other");
				if (lastSchemeIdx < 0) {
					lastSchemeIdx = 0;
				}
				schemeCh.select(lastSchemeIdx);
				p = new Panel(new FlowLayout());
				p.add(new Label("Date/time specification scheme:", Label.RIGHT));
				p.add((schemeCh));
				pan.add(p);
				pan.add(new Label("(\'t\' stands for minutes; separator symbols are irrelevant)", Label.CENTER));
				pan.add(new Line(false));
			}
		}
		pan.add(new Label("Divide the time period", Label.CENTER));
		Panel pp = new Panel(new GridLayout(1, 2));
		Panel p = new Panel(new BorderLayout());
		p.add(new Label("from"), BorderLayout.WEST);
		fromTF = new TextField(startTime);
		fromTF.addActionListener(this);
		p.add(fromTF, BorderLayout.CENTER);
		pp.add(p);
		p = new Panel(new BorderLayout());
		p.add(new Label("to"), BorderLayout.WEST);
		toTF = new TextField(endTime);
		toTF.addActionListener(this);
		p.add(toTF, BorderLayout.CENTER);
		pp.add(p);
		pan.add(pp);
		if (haveDates) {
			dayOfWeekFromTF = new TextField(10);
			dayOfWeekFromTF.setEnabled(false);
			dayOfWeekToTF = new TextField(10);
			dayOfWeekToTF.setEnabled(false);
			pp = new Panel(new GridLayout(1, 2));
			p = new Panel(new FlowLayout(FlowLayout.RIGHT));
			p.add(dayOfWeekFromTF);
			pp.add(p);
			p = new Panel(new FlowLayout(FlowLayout.RIGHT));
			p.add(dayOfWeekToTF);
			pp.add(p);
			pan.add(pp);
			//
			cycleCh = new Choice();
			if (fillCycleCh()) {
				p = new Panel(new RowLayout(5, 2));
				p.add(new Label("as", Label.CENTER));
				CheckboxGroup cbg = new CheckboxGroup();
				lineCB = new Checkbox("time line", true, cbg);
				lineCB.addItemListener(this);
				cycleCB = new Checkbox("cycle of", false, cbg);
				cycleCB.addItemListener(this);
				cycleCh.setEnabled(false);
				cycleCh.addItemListener(this);
				p.add(lineCB);
				p.add(new Label(""));
				p.add(cycleCB);
				p.add(cycleCh);
				pan.add(p);
			} else {
				cycleCh = null;
				haveDates = false;
			}
		}
		pan.add(new Label("by breaks:", Label.CENTER));
		add(pan, BorderLayout.NORTH);
		brList = new List(5);
		pp = new Panel(new BorderLayout());
		pp.add(brList, BorderLayout.CENTER);
		Button b = new Button("Remove");
		b.setActionCommand("remove_break");
		b.addActionListener(this);
		p = new Panel(new BorderLayout(5, 0));
		p.add(b, BorderLayout.NORTH);
		b = new Button("Remove all");
		b.setActionCommand("remove_all_breaks");
		b.addActionListener(this);
		p.add(b, BorderLayout.SOUTH);
		pp.add(p, BorderLayout.EAST);
		add(pp, BorderLayout.CENTER);
		pan = new Panel(new ColumnLayout());
		pp = new Panel(new BorderLayout());
		pp.add(new Label("New break:"), BorderLayout.WEST);
		breakTF = new TextField(20);
		breakTF.addActionListener(this);
		pp.add(breakTF, BorderLayout.CENTER);
		b = new Button("Add");
		b.setActionCommand("add_break");
		b.addActionListener(this);
		pp.add(b, BorderLayout.EAST);
		pan.add(pp);
		pan.add(new Label("Automatic division:", Label.CENTER));
		p = new Panel(new RowLayout(5, 2));
		p.add(new Label("- into"));
		iNumTF = new TextField("2", 3);
		iNumTF.addActionListener(this);
		p.add(iNumTF);
		p.add(new Label("equal intervals"));
		b = new Button("Divide");
		b.setActionCommand("divide_num");
		b.addActionListener(this);
		p.add(b);
		pan.add(p);
		p = new Panel(new RowLayout(5, 2));
		p.add(new Label("- into intervals of length"));
		iLenTF = new TextField("1", 5);
		iLenTF.addActionListener(this);
		p.add(iLenTF);
		if (haveDates) {
			unitCh = new Choice();
			for (String unit : units) {
				unitCh.add(unit);
			}
			p.add(unitCh);
			selectSuitableDefaultUnit();
		}
		b = new Button("Divide");
		b.setActionCommand("divide_len");
		b.addActionListener(this);
		p.add(b);
		pan.add(p);
		if (haveDates && unitCh != null) {
			adjCB = new Checkbox("Adjust the breaks to the selected unit", true);
			pan.add(adjCB);
		}
		add(pan, BorderLayout.SOUTH);
	}

	/**
	 * Registers a listener of changes of selected temporal breaks
	 */
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		if (listener == null)
			return;
		if (pcSupport == null) {
			pcSupport = new PropertyChangeSupport(this);
		}
		pcSupport.addPropertyChangeListener(listener);
	}

	/**
	 * Removes the listener of changes of selected temporal breaks
	 */
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		if (listener == null || pcSupport == null)
			return;
		pcSupport.removePropertyChangeListener(listener);
	}

	/**
	 * Notifies the registered listeners about a change of selected temporal breaks
	 */
	public void notifyBreaksChange() {
		if (pcSupport == null)
			return;
		pcSupport.firePropertyChange("breaks", null, timeBreaks);
	}

	private boolean fillCycleCh() {
		if (cycleCh == null)
			return false;
		TimeMoment t1 = recogniseDateOrTime(dataStartStr);
		if (t1 == null || !(t1 instanceof Date))
			return false;
		TimeMoment t2 = recogniseDateOrTime(dataEndStr);
		if (t2 == null || !(t2 instanceof Date))
			return false;
		Date d1 = (Date) t1, d2 = (Date) t2;
		if (!d1.isValid())
			return false;
		if (!d2.isValid())
			return false;
		if (d1.compareTo(d2) >= 0)
			return false;
		cycleCh.removeAll();
		for (int i = 0; i < cycleElements.length; i++) {
			cycleCh.add(cycles[i]);
			/* the following code allows time cycles only if time interval is long enough 
			if (d1.hasElement(cycleElements[i]) && d2.hasElement(cycleElements[i])) {
			  d1.setPrecision(cycleElements[i]); d2.setPrecision(cycleElements[i]);
			  if (d2.subtract(d1)>nCycleElements[i])
			    cycleCh.add(cycles[i]);
			} */
			// instead, we just pre-select the longest meaningful time interval
			if (d1.hasElement(cycleElements[i]) && d2.hasElement(cycleElements[i])) {
				d1.setPrecision(cycleElements[i]);
				d2.setPrecision(cycleElements[i]);
				if (d2.subtract(d1) > 1) {
					cycleCh.select(i);
				}
			}
		}
		int nItems = cycleCh.getItemCount();
		if (nItems < 1)
			return false;
		if (cycleCh.getSelectedIndex() == -1) {
			cycleCh.select(nItems - 1);
		}
		return true;
	}

	protected void selectSuitableDefaultUnit() {
		if (unitCh == null)
			return;
		TimeMoment t1 = recogniseDateOrTime(dataStartStr);
		if (t1 == null || !(t1 instanceof Date))
			return;
		TimeMoment t2 = recogniseDateOrTime(dataEndStr);
		if (t2 == null || !(t2 instanceof Date))
			return;
		Date d1 = (Date) t1, d2 = (Date) t2;
		if (!d1.isValid())
			return;
		if (!d2.isValid())
			return;
		if (d1.compareTo(d2) >= 0)
			return;
		for (int i = 0; i < unitSymbols.length; i++)
			if (d1.hasElement(unitSymbols[i]) && d2.hasElement(unitSymbols[i])) {
				long diff = d2.subtract(d1, unitSymbols[i]);
				if (diff >= 10) {
					unitCh.select(i);
					return;
				}
			}
		unitCh.select(units.length - 1);
	}

	public void setNotificationLine(NotificationLine infoLine) {
		lStatus = infoLine;
		if (lStatus != null)
			if (!checkOriginalDates()) {
				showMessage(err, true);
			}
	}

	/**
	* Shows the given message. If "trouble" is true, then this
	* is an error message.
	*/
	public void showMessage(String msg, boolean trouble) {
		if (lStatus != null) {
			lStatus.showMessage(msg, trouble);
		}
		if (trouble) {
			System.out.println("ERROR: " + msg);
		}
	}

	/**
	* Shows the given message. Assumes that this is not an error message.
	*/
	public void showMessage(String msg) {
		showMessage(msg, false);
	}

	/**
	* Clears the status line
	*/
	public void clearStatusLine() {
		if (lStatus != null) {
			lStatus.showMessage(null, false);
		}
		err = null;
	}

	/**
	 * Tries to interpret the given string as a date according to the currently
	 * specified date scheme. If no date scheme is specified, tries to interpret
	 * the string as an abstract integer time count. Returns an instance of Date
	 * or TimeCount in case of success or null in case of error.
	 */
	protected TimeMoment recogniseDateOrTime(String dateTimeStr) {
		if (dateTimeStr == null)
			return null;
		if (schemeCh == null) {
			TimeCount count = new TimeCount();
			if (count.setMoment(dateTimeStr))
				return count;
			return null;
		}
		String scheme = schemeCh.getSelectedItem();
		Date date = new Date();
		date.setDateScheme(scheme);
		if (date.setMoment(dateTimeStr))
			return date;
		return null;
	}

	/**
	 * Checks if the original minimum and maximum dates retrieved from the
	 * data can be interpreted using the currently selected date scheme.
	 */
	protected boolean checkOriginalDates() {
		if (dataStartStr == null && dataEndStr == null)
			return true;
		TimeMoment date1 = null, date2 = null;
		if (dataStartStr != null) {
			date1 = recogniseDateOrTime(dataStartStr);
			if (date1 == null) {
				err = "Failed to recognise the start date!";
				return false;
			}
			if (!date1.isValid()) {
				err = date1.toString() + ": " + date1.getErrorMessage();
				return false;
			}
		}
		if (dataEndStr != null) {
			date2 = recogniseDateOrTime(dataEndStr);
			if (date2 == null) {
				err = "Failed to recognise the end date!";
				return false;
			}
			if (!date2.isValid()) {
				err = date2.toString() + ": " + date2.getErrorMessage();
				return false;
			}
		}
		if (date1 == null || date2 == null) {
			if (date1 != null) {
				showMessage("Recognised the start date as " + date1.toString());
			} else {
				showMessage("Recognised the end date as " + date2.toString());
			}
			return true;
		}
		int cmp = date1.compareTo(date2);
		if (cmp > 0) {
			err = "The start date " + date1.toString() + " is earlier than the end date " + date2.toString() + "!";
			return false;
		}
		if (cmp == 0) {
			err = "The start date " + date1.toString() + " is the same as the end date " + date2.toString() + "!";
			return false;
		}
		showMessage("Recognised the start and end dates as " + date1.toString() + " and " + date2.toString());
		return true;
	}

	private boolean checkDateInField(TextField tf, String fieldName) {
		String str = tf.getText();
		if (str != null) {
			str = str.trim();
			if (str.length() > 0) {
				TimeMoment t = recogniseDateOrTime(str);
				if (t == null) {
					err = "Failed to recognise the date <" + str + "> in field \"" + fieldName + "\"!";
					showMessage(err, true);
					return false;
				}
				if (t.isValid()) {
					if ((t instanceof Date) && (tf.equals(fromTF) || tf.equals(toTF))) {
						Date date = (Date) t;
						if (date.useElement('d') && date.useElement('m') && date.useElement('y')) {
							int dow = date.getDayOfWeek();
							if (tf.equals(fromTF)) {
								dayOfWeekFromTF.setText(daysOfWeek[dow - 1]);
							} else {
								dayOfWeekToTF.setText(daysOfWeek[dow - 1]);
							}
						}
					}
					return true;
				}
				err = "Date <" + t.toString() + "> in field \"" + fieldName + "\": " + t.getErrorMessage();
				showMessage(err, true);
				return false;
			}
		}
		err = "No date in field \"" + fieldName + "\"!";
		return false;
	}

	private void setUnitsAsInCycle() {
		if (cycleCB == null || cycleCh == null || unitCh == null || !cycleCB.getState())
			return;
		unitCh.setEnabled(true);
		int cycleIdx = getCycleIndex();
		if (cycleIdx < 0)
			return;
		int idx = -1;
		for (int i = 0; i < unitSymbols.length && idx < 0; i++)
			if (unitSymbols[i] == cycleElements[cycleIdx]) {
				idx = i;
			}
		if (idx >= 0) {
			unitCh.select(idx);
			unitCh.setEnabled(false);
			iNumTF.setText(String.valueOf(nCycleElements[cycleIdx]));
			iLenTF.setText("1");
		}
	}

	protected TimeMoment adjustFirstDateToCurrentUnit(TimeMoment t) {
		if (t == null || !(t instanceof Date) || unitCh == null)
			return t;
		Date d = (Date) t.getCopy();
		int unitIdx = unitCh.getSelectedIndex();
		if (!d.hasElement(unitSymbols[unitIdx]))
			return t;
		for (int i = unitIdx + 1; i < unitSymbols.length; i++)
			if (d.hasElement(unitSymbols[i])) {
				d.setElementValue(unitSymbols[i], (i < 3) ? 1 : 0);
			}
		d.setPrecision(unitSymbols[unitIdx]);
		d.adjustScheme();
		return d;
	}

	protected TimeMoment adjustLastDateToCurrentUnit(TimeMoment t) {
		if (t == null || !(t instanceof Date) || unitCh == null)
			return t;
		Date d = (Date) t.getCopy();
		int unitIdx = unitCh.getSelectedIndex();
		if (!d.hasElement(unitSymbols[unitIdx]))
			return t;
		for (int i = unitIdx + 1; i < unitSymbols.length; i++)
			if (d.hasElement(unitSymbols[i])) {
				d.setElementValue(unitSymbols[i], (i < 3) ? 1 : 0);
			}
		long diff = t.subtract(d);
		d.setPrecision(unitSymbols[unitIdx]);
		if (diff > 0) {
			d.add(1);
		}
		d.adjustScheme();
		return d;
	}

	public void actionPerformed(ActionEvent e) {
		clearStatusLine();
		if (e.getSource().equals(fromTF)) {
			checkDateInField(fromTF, "from");
			return;
		}
		if (e.getSource().equals(toTF)) {
			checkDateInField(toTF, "to");
			return;
		}
		String cmd = e.getActionCommand();
		if (e.getSource().equals(iNumTF)) {
			cmd = "divide_num";
		} else if (e.getSource().equals(iLenTF)) {
			cmd = "divide_len";
		} else if (e.getSource().equals(breakTF)) {
			cmd = "add_break";
		}
		if (cmd.equals("remove_all_breaks")) {
			brList.removeAll();
			if (timeBreaks != null) {
				timeBreaks.removeAllElements();
			}
			notifyBreaksChange();
			return;
		}
		if (cmd.equals("remove_break")) {
			if (timeBreaks == null || timeBreaks.size() < 1)
				return;
			int idx = brList.getSelectedIndex();
			if (idx >= 0) {
				brList.remove(idx);
				timeBreaks.removeElementAt(idx);
				notifyBreaksChange();
			}
			return;
		}
		if (!checkDateInField(fromTF, "from") || !checkDateInField(toTF, "to")) {
			showMessage(err, true);
			return;
		}
		TimeMoment t1 = recogniseDateOrTime(fromTF.getText()), t2 = recogniseDateOrTime(toTF.getText());
		int cmp = t1.compareTo(t2);
		if (cmp >= 0) {
			showMessage("The time \"from\" [" + t1.toString() + "] is " + ((cmp > 0) ? "later than " : "the same as ") + "\"to\" [" + t2.toString() + "]!", true);
			return;
		}
		Date d1 = null, d2 = null;
		if (t1 instanceof Date) {
			d1 = (Date) t1;
		}
		if (t2 instanceof Date) {
			d2 = (Date) t2;
		}
		int cycleIdx = getCycleIndex(), min = 0, max = 0;
		boolean cycle = cycleIdx >= 0 && d1 != null && d2 != null;
		if (cycle) {
			min = minCycleElement[cycleIdx];
			max = min + nCycleElements[cycleIdx] - 1;
		}
		char precision = 's';
		if (dataStartStr != null) {
			TimeMoment start = recogniseDateOrTime(dataStartStr);
			if (start != null && (start instanceof Date)) {
				precision = start.getPrecision();
			}
		}
		if (cmd.equals("add_break")) {
			String str = breakTF.getText();
			if (str == null)
				return;
			str = str.trim();
			if (str.length() < 1)
				return;
			TimeMoment t = null;
			if (!cycle) {
				t = recogniseDateOrTime(str);
				if (t == null) {
					showMessage("Cannot interpret the string [" + str + "] as date/time!", true);
					return;
				}
				if (t.compareTo(t1) <= 0) {
					showMessage("The break date/time must be later than the start date/time [" + t1.toString() + "]!", true);
					return;
				}
				if (t.compareTo(t2) >= 0) {
					showMessage("The break date/time must be earlier than the end date/time [" + t2.toString() + "]!", true);
					return;
				}
			} else {
				int k = -1;
				try {
					k = Integer.valueOf(str).intValue();
				} catch (NumberFormatException nfe) {
					showMessage("A break must be an integer number from " + min + " to " + max + "!", true);
					return;
				}
				if (k < min || k > max) {
					showMessage("A break value must be between " + min + " and " + max + "!", true);
					return;
				}
				t = new TimeCount((long) k);
			}
			t.setPrecision(precision);
			if (timeBreaks == null) {
				timeBreaks = new Vector(100, 100);
			}
			if (timeBreaks.size() < 1) {
				timeBreaks.addElement(t);
				brList.add(t.toString());
				brList.select(0);
				notifyBreaksChange();
				return;
			}
			//insert the break in the appropriate place in the list
			int idx = -1;
			for (int i = 0; i < timeBreaks.size() && idx < 0; i++) {
				cmp = t.compareTo((TimeMoment) timeBreaks.elementAt(i));
				if (cmp == 0) {
					showMessage("The break [" + t.toString() + "] already exists!", true);
					return;
				}
				if (cmp < 0) {
					idx = i;
				}
			}
			if (idx < 0) {
				idx = timeBreaks.size();
			}
			timeBreaks.insertElementAt(t, idx);
			brList.add(t.toString(), idx);
			brList.select(idx);
			notifyBreaksChange();
			return;
		}
		if (cmd.equals("divide_num")) {
			String str = iNumTF.getText();
			if (str == null || str.trim().length() < 1) {
				showMessage("The desired number of intervals is not specified!", true);
				return;
			}
			str = str.trim();
			int num = -1;
			try {
				num = Integer.valueOf(str).intValue();
			} catch (NumberFormatException nfe) {
				showMessage("Illegal value given for the number of intervals: [" + str + "]!", true);
				return;
			}
			if (num < 2) {
				showMessage("The number of intervals must be at least 2!", true);
				return;
			}
			if (cycle && num > nCycleElements[cycleIdx]) {
				showMessage("The number of intervals must not exceed " + nCycleElements[cycleIdx] + " (number of " + cycles[cycleIdx] + ")!", true);
				return;
			}
			boolean adjust = !cycle && unitCh != null && adjCB != null && adjCB.getState();
			if (adjust) {
				t1 = adjustFirstDateToCurrentUnit(t1);
				t2 = adjustLastDateToCurrentUnit(t2);
				precision = unitSymbols[unitCh.getSelectedIndex()];
			}
			long len = (cycle) ? (long) nCycleElements[cycleIdx] : t2.subtract(t1, precision);
			int step = (int) Math.round(1.0 * len / num);
			if (step < 1) {
				showMessage("Too many intervals! The length of the period is only " + len + "!", true);
				return;
			}
			brList.setVisible(false);
			brList.removeAll();
			if (timeBreaks != null) {
				timeBreaks.removeAllElements();
			} else {
				timeBreaks = new Vector(100, 100);
			}
			if (!cycle) {
				TimeMoment t = t1.getCopy();
				for (int i = 0; i < num - 1; i++) {
					t.add(step);
					TimeMoment tBreak = t.getCopy();
					tBreak.setPrecision(t.getPrecision());
					brList.add(tBreak.toString());
					timeBreaks.addElement(tBreak);
				}
			} else {
				int val = d1.getElementValue(cycleElements[cycleIdx]);
				if (num == nCycleElements[cycleIdx] || cycleElements[cycleIdx] == 'd') {
					val = min;
				}
				if (val > min) {
					while (val <= max) {
						val += step;
					}
					val = val % nCycleElements[cycleIdx];
				}
				for (int i = 0; i < num && val <= max; i++) {
					brList.add(String.valueOf(val));
					timeBreaks.addElement(new TimeCount((long) val));
					val += step;
				}
			}
			brList.setVisible(true);
			notifyBreaksChange();
			return;
		}
		if (cmd.equals("divide_len")) {
			String str = iLenTF.getText();
			if (str == null || str.trim().length() < 1) {
				showMessage("The desired interval length is not specified!", true);
				return;
			}
			str = str.trim();
			int iLen = -1;
			try {
				iLen = Integer.valueOf(str).intValue();
			} catch (NumberFormatException nfe) {
				showMessage("Illegal value given for the interval length: [" + str + "]!", true);
				return;
			}
			if (iLen < 1) {
				showMessage("The interval length must be at least 1!", true);
				return;
			}
			if (unitCh != null) {
				int k = unitCh.getSelectedIndex();
				d1.setPrecision(unitSymbols[k]);
				d2.setPrecision(unitSymbols[k]);
			}
			long len = (cycle) ? (long) nCycleElements[cycleIdx] : t2.subtract(t1);
			if (iLen >= len) {
				showMessage("Too long interval! The length of the period is only " + len + "!", true);
				return;
			}
			long num = len / iLen;
			if (num > 20) {
				Panel p = new Panel(new GridLayout(2, 1));
				p.add(new Label("This will produce " + num + " breaks.", Label.CENTER));
				p.add((new Label("Continue?", Label.CENTER)));
				OKDialog ok = new OKDialog(CManager.getAnyFrame(this), "Produce " + num + " breaks?", OKDialog.YES_NO_MODE, true);
				ok.addContent(p);
				ok.show();
				if (ok.wasCancelled())
					return;
			}
			brList.setVisible(false);
			brList.removeAll();
			if (timeBreaks != null) {
				timeBreaks.removeAllElements();
			} else {
				timeBreaks = new Vector(100, 100);
			}
			if (!cycle) {
				boolean adjust = unitCh != null && adjCB != null && adjCB.getState();
				TimeMoment t = (adjust) ? adjustFirstDateToCurrentUnit(t1) : t1.getCopy();
				t.add(iLen);
				do {
					TimeMoment tBreak = t.getCopy();
					tBreak.setPrecision(t.getPrecision());
					brList.add(tBreak.toString());
					timeBreaks.addElement(tBreak);
					t.add(iLen);
				} while (t.compareTo(t2) <= 0); // code by N: <0 => wrong division for birds (no 1/1/2006)
			} else {
				int val = d1.getElementValue(cycleElements[cycleIdx]);
				if (iLen == 1 || cycleElements[cycleIdx] == 'd') {
					val = min;
				}
				if (val > min) {
					while (val <= max) {
						val += iLen;
					}
					val = val % nCycleElements[cycleIdx];
				}
				while (val <= max) {
					brList.add(String.valueOf(val));
					timeBreaks.addElement(new TimeCount((long) val));
					val += iLen;
				}
				;
			}
			brList.setVisible(true);
			notifyBreaksChange();
			return;
		}
	}

	public void itemStateChanged(ItemEvent e) {
		clearStatusLine();
		if (e.getSource().equals(schemeCh)) {
			if (schemeCh.getSelectedIndex() == schemeCh.getItemCount() - 1) {
				//the item "other" is selected; ask the user to enter the scheme
				Panel p = new Panel(new ColumnLayout());
				p.add((new Label("Specify the scheme of the dates and/or times:")));
				TextField tf = new TextField(schemeCh.getItem(lastSchemeIdx), 20);
				p.add(tf);
				TextCanvas tc = new TextCanvas();
				tc.addTextLine("Use symbols { y m d h t s } for date/time " + "components ('t' means minutes) and symbols { - . : , / ; } or space " + "as separators.");
				p.add(tc);
				OKDialog ok = new OKDialog(CManager.getAnyFrame(this), "Date scheme?", true);
				ok.addContent(p);
				ok.show();
				if (ok.wasCancelled()) {
					schemeCh.select(lastSchemeIdx);
				} else {
					String str = tf.getText();
					if (str == null || str.trim().length() < 1) {
						showMessage("No scheme specified!", true);
						schemeCh.select(lastSchemeIdx);
						return;
					}
					str = str.trim();
					String symbols = "ymdhts-.:,/;";
					for (int i = 0; i < str.length(); i++) {
						if (symbols.indexOf(str.charAt(i)) < 0) {
							showMessage("Invalid symbol [" + String.valueOf(str.charAt(i)) + "] in scheme [" + str + "]!", true);
							schemeCh.select(lastSchemeIdx);
							return;
						}
					}
					String result = Date.checkTemplateValidity(str);
					if (result != null) {
						showMessage(result, true);
						schemeCh.select(lastSchemeIdx);
						return;
					}
					lastSchemeIdx = schemeCh.getItemCount() - 1;
					schemeCh.insert(str, lastSchemeIdx);
					schemeCh.select(lastSchemeIdx);
				}
			}
			if (!checkOriginalDates()) {
				showMessage(err, true);
				return;
			}
			fillCycleCh();
			if (!checkDateInField(fromTF, "from")) {
				String str = fromTF.getText();
				if (str != null && str.trim().length() > 0)
					return;
			}
			if (!checkDateInField(toTF, "to")) {
				String str = toTF.getText();
				if (str != null && str.trim().length() > 0)
					return;
			}
			return;
		}
		if (e.getSource().equals(lineCB) || e.getSource().equals(cycleCB)) {
			brList.removeAll();
			if (timeBreaks != null) {
				timeBreaks.removeAllElements();
			}
			notifyBreaksChange();
			cycleCh.setEnabled(cycleCB.getState());
			if (cycleCB.getState()) {
				setUnitsAsInCycle();
			} else if (unitCh != null) {
				unitCh.setEnabled(true);
			}
			return;
		}
		if (e.getSource().equals(cycleCh)) {
			setUnitsAsInCycle();
			brList.removeAll();
			if (timeBreaks != null) {
				timeBreaks.removeAllElements();
			}
			notifyBreaksChange();
			return;
		}
	}

	public boolean canClose() {
		clearStatusLine();
		if (timeBreaks == null || timeBreaks.size() < 1) {
			err = "No breaks specified!";
			return false;
		}
		if (!useCycle() && dataStartStr != null && dataEndStr != null) {
			TimeMoment t1 = recogniseDateOrTime(dataStartStr), t2 = recogniseDateOrTime(dataEndStr);
			if (t1 != null && t2 != null) {
				int nRemoved = 0;
				Cursor cur = null;
				Window w = CManager.getWindow(this);
				if (timeBreaks.size() > 20) {
					showMessage("Checking the breaks and removing useless breaks; please wait...", false);
					if (w != null) {
						cur = w.getCursor();
						w.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
					}
				}
				//remove breaks that do not fit in this interval
				brList.setVisible(false);
				for (int i = timeBreaks.size() - 1; i >= 0; i--) {
					TimeMoment t = (TimeMoment) timeBreaks.elementAt(i);
					if (t.compareTo(t1) <= 0 || t.compareTo(t2) > 0) {
						timeBreaks.removeElementAt(i);
						brList.remove(i);
						++nRemoved;
					}
				}
				brList.setVisible(true);
				if (cur != null && w != null) {
					w.setCursor(cur);
				}
				clearStatusLine();
				if (timeBreaks.size() < 1) {
					err = "All " + nRemoved + " breaks were useless " + "(not in the time range of the data)!";
					return false;
				}
				if (nRemoved > 0) {
					notifyBreaksChange();
					err = nRemoved + " breaks were removed as useless (not in the time range " + "of the data); press the button again after checking";
					return false;
				}
			}
		}
		return true;
	}

	public String getErrorMessage() {
		return err;
	}

	public Vector getTimeBreaks() {
		return timeBreaks;
	}

	public TimeMoment getStart() {
		TimeMoment t = recogniseDateOrTime(fromTF.getText());
		if (!(t instanceof Date))
			return t;
		boolean adjust = getCycleIndex() < 0 && unitCh != null && adjCB != null && adjCB.getState();
		if (!adjust)
			return t;
		return adjustFirstDateToCurrentUnit(t);
	}

	public TimeMoment getEnd() {
		TimeMoment t = recogniseDateOrTime(toTF.getText());
		if (!(t instanceof Date))
			return t;
		boolean adjust = getCycleIndex() < 0 && unitCh != null && adjCB != null && adjCB.getState();
		if (!adjust)
			return t;
		return adjustLastDateToCurrentUnit(t);
	}

	public boolean useCycle() {
		return cycleCB != null && cycleCB.getState();
	}

	public int getCycleIndex() {
		if (cycleCh == null || !useCycle())
			return -1;
		int cycleIdx = -1;
		String cycleTxt = cycleCh.getSelectedItem();
		if (cycleTxt == null)
			return -1;
		for (int i = 0; i < cycles.length && cycleIdx < 0; i++)
			if (cycleTxt.equalsIgnoreCase(cycles[i])) {
				cycleIdx = i;
			}
		return cycleIdx;
	}

	public char getCycleUnit() {
		int cycleIdx = getCycleIndex();
		if (cycleIdx < 0)
			return 0;
		return cycleElements[cycleIdx];
	}

	public String getCycleName() {
		if (cycleCh == null || !useCycle())
			return null;
		return cycleCh.getSelectedItem();
	}

	public int getNCycleElements() {
		int cycleIdx = getCycleIndex();
		if (cycleIdx < 0)
			return 0;
		return nCycleElements[cycleIdx];
	}

	public String getDateScheme() {
		if (schemeCh == null)
			return null;
		return schemeCh.getSelectedItem();
	}
}
