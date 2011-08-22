package spade.time.ui;

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
import java.util.Vector;

import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.DialogContent;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.NotificationLine;
import spade.lib.basicwin.NotificationLineManager;
import spade.lib.basicwin.OKDialog;
import spade.lib.basicwin.RowLayout;
import spade.time.Date;
import spade.time.TimeCount;
import spade.time.TimeMoment;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Aug 4, 2009
 * Time: 12:35:16 PM
 * Allows the user to specify a division of a time period into intervals
 * according to the linear or cyclical time model.
 */
public class SetTimeBreaksUI extends Panel implements ActionListener, ItemListener, DialogContent, NotificationLineManager {
	public static final String cycles[] = { "seconds in minute", "minutes in hour", "hours in day", "days in week", "months in year" };
	public static final char cycleElements[] = { 's', 't', 'h', 'd', 'm' };
	public static final int nCycleElements[] = { 60, 60, 24, 7, 12 };
	public static final int minCycleElement[] = { 0, 0, 0, 1, 1 };
	public static final String units[] = { "year(s)", "month(s)", "day(s)", "hour(s)", "minute(s)", "second(s)" };
	public static final char unitSymbols[] = { 'y', 'm', 'd', 'h', 't', 's' };
	public static final String daysOfWeek[] = { "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday" };

	protected TimeMoment dataStart = null, dataEnd = null;
	protected Date dataStartDate = null, dataEndDate = null;
	protected String dateScheme = null;

	protected TextField fromTF = null, toTF = null, breakTF = null, iNumTF = null, iLenTF = null;
	protected TextField dayOfWeekFromTF = null, dayOfWeekToTF = null;
	protected Checkbox lineCB = null, cycleCB = null;
	protected Choice cycleCh = null, unitCh = null;
	protected List brList = null;
	/**
	 * The breaks defined, instances of TimeMoment (i.e. either Date or TimeCount)
	 */
	protected Vector<TimeMoment> timeBreaks = null;
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

	public SetTimeBreaksUI(TimeMoment tStart, TimeMoment tEnd) {
		if (tStart == null || tEnd == null || tEnd.subtract(tStart) < 2)
			return;
		dataStart = tStart;
		dataEnd = tEnd;
		setLayout(new BorderLayout());
		Panel pan = new Panel(new ColumnLayout());
		boolean haveDates = (tStart instanceof Date) && ((Date) tStart).scheme != null;
		if (haveDates) {
			dataStartDate = (Date) tStart;
			dataEndDate = (Date) tEnd;
			dateScheme = dataStartDate.scheme;
		}
		if (haveDates) {
			pan.add(new Label("Date/time specification scheme:  " + dateScheme));
			if (dataStartDate.useElement('t')) {
				pan.add(new Label("(\'t\' stands for minutes; separator symbols are irrelevant)", Label.CENTER));
			}
			pan.add(new Line(false));
		}
		pan.add(new Label("Divide the time period", Label.CENTER));
		Panel pp = new Panel(new GridLayout(1, 2));
		Panel p = new Panel(new BorderLayout());
		p.add(new Label("from"), BorderLayout.WEST);
		fromTF = new TextField(dataStart.toString());
		fromTF.addActionListener(this);
		p.add(fromTF, BorderLayout.CENTER);
		pp.add(p);
		p = new Panel(new BorderLayout());
		p.add(new Label("to"), BorderLayout.WEST);
		toTF = new TextField(dataEnd.toString());
		toTF.addActionListener(this);
		p.add(toTF, BorderLayout.CENTER);
		pp.add(p);
		pan.add(pp);
		if (haveDates && dataStartDate.useElement('d') && dataStartDate.useElement('m') && dataStartDate.useElement('y')) {
			int dow = dataStartDate.getDayOfWeek();
			dayOfWeekFromTF = new TextField(daysOfWeek[dow - 1], 10);
			dayOfWeekFromTF.setEnabled(false);
			dow = dataEndDate.getDayOfWeek();
			dayOfWeekToTF = new TextField(daysOfWeek[dow - 1], 10);
			dayOfWeekToTF.setEnabled(false);
			pp = new Panel(new GridLayout(1, 2));
			p = new Panel(new FlowLayout(FlowLayout.RIGHT));
			p.add(dayOfWeekFromTF);
			pp.add(p);
			p = new Panel(new FlowLayout(FlowLayout.RIGHT));
			p.add(dayOfWeekToTF);
			pp.add(p);
			pan.add(pp);
		}
		if (haveDates) {
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
		iLenTF = new TextField("10", 5);
		iLenTF.addActionListener(this);
		p.add(iLenTF);
		if (haveDates) {
			unitCh = new Choice();
			for (String unit : units) {
				unitCh.add(unit);
			}
			p.add(unitCh);
			unitCh.select(units.length - 1);
		}
		b = new Button("Divide");
		b.setActionCommand("divide_len");
		b.addActionListener(this);
		p.add(b);
		pan.add(p);
		add(pan, BorderLayout.SOUTH);
	}

	private boolean fillCycleCh() {
		if (cycleCh == null || dataStartDate == null || dataEndDate == null)
			return false;
		if (!dataStartDate.isValid())
			return false;
		if (!dataEndDate.isValid())
			return false;
		if (dataStartDate.compareTo(dataEndDate) >= 0)
			return false;
		Date d1 = (Date) dataStartDate.getCopy(), d2 = (Date) dataEndDate.getCopy();
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

	public void setNotificationLine(NotificationLine infoLine) {
		lStatus = infoLine;
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
		if (dateScheme == null) {
			TimeCount count = new TimeCount();
			if (count.setMoment(dateTimeStr))
				return count;
			return null;
		}
		Date date = new Date();
		date.setDateScheme(dateScheme);
		if (date.setMoment(dateTimeStr))
			return date;
		return null;
	}

	private boolean checkDateInField(TextField tf, String fieldName) {
		String str = tf.getText();
		if (str != null) {
			str = str.trim();
			if (str.length() > 0) {
				TimeMoment time = recogniseDateOrTime(str);
				if (time == null) {
					err = "Failed to recognise the date <" + str + "> in field \"" + fieldName + "\"!";
					showMessage(err, true);
					return false;
				}
				if (time.isValid()) {
					if (dayOfWeekFromTF != null) {
						Date d = (Date) time;
						int dow = d.getDayOfWeek();
						if (fieldName.equalsIgnoreCase("from")) {
							dayOfWeekFromTF.setText(daysOfWeek[dow - 1]);
						} else if (fieldName.equalsIgnoreCase("to")) {
							dayOfWeekToTF.setText(daysOfWeek[dow - 1]);
						}
					}
					return true;
				}
				err = "Date <" + time.toString() + "> in field \"" + fieldName + "\": " + time.getErrorMessage();
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

	public void itemStateChanged(ItemEvent e) {
		clearStatusLine();
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
		if (dataStartDate != null) {
			precision = dataStartDate.getPrecision();
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
			if (t instanceof Date) {
				((Date) t).setPrecision(precision);
			}
			if (timeBreaks == null) {
				timeBreaks = new Vector<TimeMoment>(100, 100);
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
			long len = (cycle) ? (long) nCycleElements[cycleIdx] : t2.subtract(t1);
			int step = (int) (len / num);
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
					if (tBreak instanceof Date) {
						((Date) tBreak).setPrecision(precision);
					}
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
				TimeMoment t = t1.getCopy();
				t.add(iLen);
				do {
					TimeMoment tBreak = t.getCopy();
					if (tBreak instanceof Date) {
						((Date) tBreak).setPrecision(precision);
					}
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

	public boolean canClose() {
		clearStatusLine();
		if (timeBreaks == null || timeBreaks.size() < 1) {
			err = "No breaks specified!";
			return false;
		}
		if (!useCycle() && dataStart != null && dataEnd != null) {
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
				if (t.compareTo(dataStart) <= 0 || t.compareTo(dataEnd) >= 0) {
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
		return true;
	}

	public String getErrorMessage() {
		return err;
	}

	public Vector<TimeMoment> getTimeBreaks() {
		return timeBreaks;
	}

	public TimeMoment getStart() {
		return recogniseDateOrTime(fromTF.getText());
	}

	public TimeMoment getEnd() {
		return recogniseDateOrTime(toTF.getText());
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
}
