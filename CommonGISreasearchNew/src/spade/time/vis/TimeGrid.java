package spade.time.vis;

import java.util.Vector;

import spade.lib.util.IntArray;
import spade.time.Date;
import spade.time.TimeMoment;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 09-Feb-2007
 * Time: 11:53:45
 * Defines positions of grid lines for temporal displays, depending on the
 * available size of the time axis and the length of the time interval
 * represented by the axis
 */
public class TimeGrid {
	public static final int mm = Math.round(java.awt.Toolkit.getDefaultToolkit().getScreenResolution() / 25.33f);
	/**
	 * Minimum gap between grid lines or ticks
	 */
	public static final int minGap = 15 * mm;
	/**
	 * "Nice" numbers for abstract and unstructured time moments as well as
	 * for days and years
	 */
	public static final int nice_numbers[] = { 2, 5, 10 };
	/**
	 * "Nice numbers" for seconds and minutes
	 */
	public static final int nice_numbers_sec_min[] = { 2, 5, 10, 15, 20, 30 };
	/**
	 * "Nice numbers" for hours
	 */
	public static final int nice_numbers_hours[] = { 2, 3, 6, 12 };
	/**
	 * "Nice numbers" for days
	 */
	public static final int nice_numbers_days[] = { 2, 5, 10, 15 };
	/**
	 * "Nice numbers" for months
	 */
	public static final int nice_numbers_months[] = { 2, 3, 4, 6 };
	/**
	 * The start and end moments of the represented time interval.
	 */
	protected TimeMoment start = null, end = null;
	/**
	 * The length of the time interval
	 */
	protected int timeLen = 0;
	/**
	 * The available length of the time axis on the screen representing the interval
	 */
	protected int scrLen = 0;
	/**
	 * The time intervals to be represented by grid lines or ticks (this does not
	 * necessarily include the starting and ending moments)
	 */
	protected Vector gridTimes = null;
	/**
	 * The positions of the grid lines (this does not necessarily include the
	 * positions of the starting and ending moments)
	 */
	protected IntArray gridPos = null;

	/**
	 * Sets the represented time interval.
	 */
	public void setTimeInterval(TimeMoment t1, TimeMoment t2) {
		start = t1.getCopy();
		end = t2.getCopy();
		if (start != null && end != null) {
			if (start instanceof Date) {
				((Date) start).setHighestPossiblePrecision();
				((Date) end).setHighestPossiblePrecision();
			}
			timeLen = (int) end.subtract(start);
		} else {
			timeLen = 0;
		}
	}

	public TimeMoment getStart() {
		return start;
	}

	public TimeMoment getEnd() {
		return end;
	}

	/**
	 * Sets the available length of the time axis on the screen representing the
	 * interval
	 */
	public void setTimeAxisLength(int length) {
		scrLen = length;
	}

	/**
	 * Returns the length of the time axis on the screen representing the
	 * interval
	 */
	public int getTimeAxisLength() {
		return scrLen;
	}

	/**
	 * Computes appropriate positions for grid lines or ticks
	 */
	public void computeGrid() {
		if (gridTimes != null) {
			gridTimes.removeAllElements();
		}
		if (gridPos != null) {
			gridPos.removeAllElements();
		}
		if (timeLen < 2 || scrLen <= minGap)
			return;
		int unitIdx = start.getPrecisionIdx(), unitToRoundIdx = unitIdx;
		int nUnits = 1;
		//int nn[]=nice_numbers;
		double step = 1.0f * scrLen / timeLen;
		if (start instanceof Date) {
			Date d0 = (Date) start;
			while (step < minGap && unitToRoundIdx > 0) {
				int nn[] = (unitToRoundIdx >= 4) ? nice_numbers_sec_min : (unitToRoundIdx == 3) ? nice_numbers_hours : (unitToRoundIdx == 2) ? nice_numbers_days : nice_numbers_months;
				double step1 = step;
				for (int i = 0; i < nn.length && step1 < minGap; i++) {
					nUnits = nn[i];
					step1 = step * nUnits;
				}
				if (step1 < minGap) {
					if (d0.hasElement(unitToRoundIdx - 1)) {
						switch (unitToRoundIdx) {
						case 4:
						case 5:
							step *= 60;
							break;
						case 3:
							step *= 24;
							break;
						case 2:
							step *= 30;
							break;
						case 1:
							step *= 12;
							break;
						}
						--unitToRoundIdx;
						nUnits = 1;
					} else {
						break;
					}
				} else {
					step = step1;
				}
			}
		}
		if (step < minGap) {
			int factor = 1;
			double step1 = step;
			while (step1 < minGap) {
				for (int i = 0; i < nice_numbers.length && step1 < minGap; i++) {
					nUnits = nice_numbers[i] * factor;
					step1 = step * nUnits;
				}
				if (step1 < minGap) {
					factor *= 10;
				}
			}
			step = step1;
		}
		TimeMoment t = start.getCopy();
		Date d = (t instanceof Date) ? (Date) t : null;
		if (d != null) {
			d.setPrecisionIdx(unitToRoundIdx);
			for (int j = Date.time_symbols.length - 1; j >= unitToRoundIdx; j--) {
				if (d.hasElement(j)) {
					d.setElementValue(Date.time_symbols[j], (j >= 3) ? 0 : 1);
				}
			}
			d.adjustScheme();
		}
		if (gridTimes == null || gridPos == null) {
			gridTimes = new Vector(20, 10);
			gridPos = new IntArray(20, 10);
		}
		//step=1.0f*scrLen/timeLen;
		while (t.compareTo(end) <= 0) {
			long timePos = t.subtract(start, start.getPrecision());
			if (timePos >= 0) {
				gridTimes.addElement(t);
				int scrPos = (int) Math.round(((double) timePos) * scrLen / timeLen);
				gridPos.addElement(scrPos);
				t = t.getCopy();
			}
			if (nUnits > 1 && (t instanceof Date) && unitToRoundIdx == 2 && d.hasElement('m') && d.hasElement('y')) {
				d = (Date) t;
				int val = d.getElementValue('d');
				int nDaysInMonth = Date.nDaysInMonth(d.getElementValue('m'), d.getElementValue('y'));
				if (val + Math.round(1.5 * nUnits) > nDaysInMonth) {
					d.setElementValue('d', 1);
					d.setPrecisionIdx(1);
					d.stepForth();
					d.setPrecisionIdx(2);
				} else {
					d.add(nUnits);
				}
			} else {
				t.add(nUnits);
			}
		}
	}

	/**
	 * Returns the computed grid positions
	 */
	public IntArray getGridPositions() {
		return gridPos;
	}

	/**
	 * Returns the time moments corresponding to the grid positions
	 */
	public Vector getGridTimeMoments() {
		return gridTimes;
	}

	public TimeMoment getGridTimeMoment(int idx) {
		if (gridTimes == null || idx < 0 || idx >= gridTimes.size())
			return null;
		return (TimeMoment) gridTimes.elementAt(idx);
	}
}
