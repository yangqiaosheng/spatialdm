package spade.time.ui;

import java.awt.BorderLayout;
import java.awt.Label;
import java.awt.MenuItem;
import java.awt.Panel;
import java.awt.PopupMenu;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import spade.time.TimeMoment;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Nov 10, 2008
 * Time: 12:19:02 PM
 * Allows the user to specify the length of a time interval in convenient units (e.g. hours or days instead of seconds).
 */
public class GetIntervalLengthUI extends Panel implements ActionListener, MouseListener {
	protected static char maxPrecision = 'd';
	protected TimeMoment minTime = null, maxTime = null;
	protected long iLen = 0;
	protected char origPrecision = 's', precision = origPrecision;
	protected TextField lengthTF = null;
	/**
	 * This label contains the name of the current time unit, if exists.
	 * For abstract time moments, no units exist.
	 */
	protected Label timeUnitLabel = null;
	private PopupMenu timeGranularityPopupMenu = null;

	public GetIntervalLengthUI(TimeMoment minTime, TimeMoment maxTime) {
		if (minTime == null || maxTime == null)
			return;
		this.minTime = minTime.getCopy();
		this.maxTime = maxTime.getCopy();
		precision = origPrecision = minTime.getPrecision();
		iLen = maxTime.subtract(minTime);
		if (iLen < 1)
			return;
		setLayout(new BorderLayout());
		lengthTF = new TextField(String.valueOf(iLen));
		lengthTF.addActionListener(this);
		add(lengthTF, BorderLayout.CENTER);
		String str = minTime.getUnits();
		if (str != null) {
			timeUnitLabel = new Label(str);
			add(timeUnitLabel, BorderLayout.EAST);
			if (minTime.getMinPrecision() != minTime.getMaxPrecision()) {
				timeUnitLabel.addMouseListener(this);
				//by clicking on this label, the user will be able to change the
				//precision of specifying the interval length
			}
		}
	}

	/**
	 * @return user-specified interval length according to the original precision
	 * of minTime and maxTime
	 */
	public long getIntervalLengthInOrigUnits() {
		getIntervalLengthInCurrentUnits();
		if (iLen < 1)
			return 0;
		if (precision == origPrecision)
			return iLen;
		long len = iLen;
		while (precision != origPrecision && precision != 's') {
			switch (precision) {
			case 'd':
				len *= 24;
				precision = 'h';
				break;
			case 'h':
				len *= 60;
				precision = 't';
				break;
			case 't':
				len *= 60;
				precision = 's';
				break;
			}
		}
		return len;
	}

	/**
	 * @return user-specified interval length in the currently selected units
	 */
	public long getIntervalLengthInCurrentUnits() {
		String str = lengthTF.getText();
		long l = 0L;
		try {
			l = Long.valueOf(str).longValue();
		} catch (NumberFormatException nfe) {
		}
		if (l < 1) {
			l = 1;
		}
		long maxL = maxTime.subtract(minTime);
		if (l > maxL) {
			l = maxL;
		}
		iLen = l;
		lengthTF.setText(String.valueOf(iLen));
		return iLen;
	}

	public char getCurrentPrecision() {
		return precision;
	}

	public String getCurrentUnits() {
		return minTime.getUnits();
	}

	public String getCurrentUnit() {
		return minTime.getUnit();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource().equals(lengthTF)) {
			getIntervalLengthInCurrentUnits();
		} else if (e.getActionCommand().startsWith("granularity_")) {
			String cmd = e.getActionCommand();
			char symb = cmd.charAt(cmd.length() - 1);
			if (symb != precision) {
				precision = symb;
				minTime.setPrecision(precision);
				maxTime.setPrecision(precision);
				iLen = maxTime.subtract(minTime);
				lengthTF.setText(String.valueOf(iLen));
				timeUnitLabel.setText(minTime.getUnits());
			}
		}
	}

	/**
	 * Reaction to mouse clicks on the label showing the time units in which the
	 * length of the interval is specified
	 */
	@Override
	public void mouseClicked(MouseEvent e) {
		if (timeUnitLabel == null)
			return;
		if (timeGranularityPopupMenu == null) {
			TimeMoment t = minTime;
			if (t == null)
				return;
			char elements[] = t.getAvailableTimeElements();
			if (elements == null || elements.length < 2)
				return;
			TimeMoment t2 = maxTime;
			int len = elements.length;
			for (int i = elements.length - 1; i > 0; i--) {
				if (elements[i] == 'd' || elements[i] == 'h' || elements[i] == 't' || elements[i] == 's') {
					int n1 = t.getElementValue(elements[i]), n2 = t2.getElementValue(elements[i]);
					if (n2 > n1) {
						break;
					}
				}
				--len;
			}
			if (len < 2)
				return;
			timeGranularityPopupMenu = new PopupMenu();
			for (int i = 0; i < len; i++) {
				MenuItem mi = new MenuItem(t.getTextForUnit(elements[i]));
				mi.setActionCommand("granularity_" + String.valueOf(elements[i]));
				timeGranularityPopupMenu.add(mi);
			}
			add(timeGranularityPopupMenu);
			timeGranularityPopupMenu.addActionListener(this);
		}
		timeGranularityPopupMenu.show(timeUnitLabel, e.getX(), e.getY());
	}

	/**
	 * Invoked when a mouse button has been pressed on a component.
	 */
	@Override
	public void mousePressed(MouseEvent e) {
	}

	/**
	 * Invoked when a mouse button has been released on a component.
	 */
	@Override
	public void mouseReleased(MouseEvent e) {
	}

	/**
	 * Invoked when the mouse enters a component.
	 */
	@Override
	public void mouseEntered(MouseEvent e) {
	}

	/**
	 * Invoked when the mouse exits a component.
	 */
	@Override
	public void mouseExited(MouseEvent e) {
	}
}
