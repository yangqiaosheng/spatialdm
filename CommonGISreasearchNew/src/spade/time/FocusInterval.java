package spade.time;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Hashtable;
import java.util.Vector;

/**
* Used for selection of the focus time interval for visualization and analysis
* of time-series data.
*/
public class FocusInterval {
	/**
	* What can be fixed during automated or manual animation: interval beginning,
	* end, or length
	*/
	public static final int NONE = 0, START = 1, END = 2, LENGTH = 3;
	/**
	* The whole time interval
	*/
	protected TimeMoment start = null, end = null;
	/**
	 * Since the start and end moment may be "rounded", depending on the
	 * selected time granularity, these are untouched copies keeping the
	 * original values.
	 */
	protected TimeMoment origStart = null, origEnd = null;
	/**
	* Indicates whether the whole time interval is valid
	*/
	protected boolean validInterval = false;
	/**
	* The length of the whole time interval, in some time units
	*/
	protected long iLen = 0l;
	/**
	* The currently selected time interval
	*/
	protected TimeMoment currStart = null, currEnd = null;
	/**
	* The positions of the ends of the currently selected time interval within
	* the whole interval
	*/
	protected long currStartPos = 0, currEndPos = 0;
	/**
	* What must be fixed during animation: interval beginning, end, or length.
	* The value must be one of the constants START(=1), END(=2), or LENGTH(=3)
	*/
	protected int whatIsFixed = START;
	/**
	* The list of property change listeners to be notified
	* about changes of the current moment or interval.
	*/
	protected Vector listeners = null;

	/**
	* Registers a listener of changes of the current moment or interval
	*/
	public synchronized void addPropertyChangeListener(PropertyChangeListener l) {
		if (l == null)
			return;
		if (listeners == null) {
			listeners = new Vector(5, 5);
		}
		if (!listeners.contains(l)) {
			listeners.addElement(l);
		}
	}

	/**
	* Removes the listener of changes of the current moment or interval
	*/
	public synchronized void removePropertyChangeListener(PropertyChangeListener l) {
		if (l == null || listeners == null)
			return;
		listeners.removeElement(l);
	}

	protected void notifyIntervalChange() {
		if (listeners == null || listeners.size() < 1)
			return;
		PropertyChangeEvent pce = new PropertyChangeEvent(this, "current_interval", currStart, currEnd);
		for (int i = 0; i < listeners.size(); i++) {
			((PropertyChangeListener) listeners.elementAt(i)).propertyChange(pce);
		}
	}

	public void notifyAnimationStart() {
		if (listeners == null || listeners.size() < 1)
			return;
		PropertyChangeEvent pce = new PropertyChangeEvent(this, "animation", null, "start");
		for (int i = 0; i < listeners.size(); i++) {
			((PropertyChangeListener) listeners.elementAt(i)).propertyChange(pce);
		}
	}

	public void notifyAnimationStop() {
		if (listeners == null || listeners.size() < 1)
			return;
		PropertyChangeEvent pce = new PropertyChangeEvent(this, "animation", null, "stop");
		for (int i = 0; i < listeners.size(); i++) {
			((PropertyChangeListener) listeners.elementAt(i)).propertyChange(pce);
		}
	}

	public void notifyGranularityChange() {
		if (listeners == null || listeners.size() < 1)
			return;
		PropertyChangeEvent pce = new PropertyChangeEvent(this, "granularity", null, start.getUnits());
		for (int i = 0; i < listeners.size(); i++) {
			((PropertyChangeListener) listeners.elementAt(i)).propertyChange(pce);
		}
	}

	public void setDataInterval(TimeMoment t1, TimeMoment t2) {
		if (t1 != null) {
			start = t1.getCopy();
			origStart = t1.getCopy();
		}
		if (t2 != null) {
			end = t2.getCopy();
			origEnd = t2.getCopy();
		}
		validInterval = start != null && end != null && start.compareTo(end) < 0;
		if (validInterval) {
			iLen = end.subtract(start);
			currStartPos = 0;
			currEndPos = iLen;
			currStart = start.getCopy();
			currEnd = end.getCopy();
		}
	}

	public boolean hasValidDataInterval() {
		return validInterval;
	}

	public TimeMoment getDataIntervalStart() {
		return start;
	}

	public TimeMoment getDataIntervalEnd() {
		return end;
	}

	public long getDataIntervalLength() {
		if (!validInterval)
			return 0;
		return iLen;
	}

	public TimeMoment getCurrIntervalStart() {
		return currStart;
	}

	public TimeMoment getCurrIntervalEnd() {
		return currEnd;
	}

	public long getCurrStartPos() {
		return currStartPos;
	}

	public long getCurrEndPos() {
		return currEndPos;
	}

	public long getCurrIntervalLength() {
		TimeMoment s = (currStart == null) ? start : currStart, e = (currEnd == null) ? end : currEnd;
		if (s == null || e == null)
			return 0;
		return e.subtract(s);
	}

	/**
	* Returns the precision (time scale) with which all operations over dates are
	* done. The precision is specified in terms of the smallest significant units:
	* 's' - seconds, 't' - minutes, 'h' - hours, 'd' - days, 'm' - month,
	* 'y' - years.
	*/
	public char getPrecision() {
		if (start == null)
			return 0;
		return start.getPrecision();
	}

	/**
	* Sets the precision (time scale) with which all operations over dates will be
	* done. The precision is specified in terms of the smallest significant units:
	* 's' - seconds, 't' - minutes, 'h' - hours, 'd' - days, 'm' - month,
	* 'y' - years.
	*/
	public void setPrecision(char unit) {
		if (start != null) {
			origStart.copyTo(start);
			start.setPrecision(unit);
			if (start instanceof Date) {
				((Date) start).roundDown();
			}
		}
		if (end != null) {
			origEnd.copyTo(end);
			end.setPrecision(unit);
			if (end instanceof Date) {
				((Date) end).roundUp();
			}
		}
		if (currStart != null) {
			currStart.setPrecision(unit);
			if (currStart instanceof Date) {
				((Date) currStart).roundDown();
			}
			if (currStart.compareTo(start) < 0) {
				start.copyTo(currStart);
			}
		}
		if (currEnd != null) {
			currEnd.setPrecision(unit);
			if (currEnd instanceof Date) {
				((Date) currEnd).roundUp();
			}
			if (currEnd.compareTo(end) > 0) {
				end.copyTo(currEnd);
			}
		}
		iLen = end.subtract(start);
		currStartPos = (currStart == null) ? 0 : currStart.subtract(start);
		currEndPos = (currEnd == null) ? iLen : currEnd.subtract(start);
		notifyGranularityChange();
		long len = getCurrIntervalLength();
		TimeMoment t = (currStart == null) ? start.getCopy() : currStart.getCopy();
		t.add((int) len);
		if (!t.equals((currEnd == null) ? end : currEnd)) {
			TimeMoment t1 = null, t2 = null;
			switch (whatIsFixed) {
			case START:
			case LENGTH:
				t2 = t;
				t1 = (currStart == null) ? start.getCopy() : currStart.getCopy();
				break;
			case END:
				t2 = (currEnd == null) ? end.getCopy() : currEnd.getCopy();
				t1 = t2.getCopy();
				t1.add((int) -len);
				break;
			}
			setCurrInterval(t1, t2);
		}
	}

	/**
	* Returns true if actually changed
	*/
	public boolean setCurrIntervalStart(TimeMoment t) {
		if (t == null) {
			TimeMoment t1 = (currEnd == null) ? null : currEnd.getCopy();
			return setCurrInterval(t, t1);
		}
		if (start != null && t.getPrecision() != start.getPrecision()) {
			t = t.getCopy();
			t.setPrecision(start.getPrecision());
		}
		if (t.compareTo(start) >= 0 && t.compareTo(end) <= 0) {
			if (whatIsFixed == LENGTH) {
				long len = t.subtract((currStart == null) ? start : currStart);
				TimeMoment t1 = (currEnd == null) ? end.getCopy() : currEnd.getCopy();
				t1.add((int) len);
				return setCurrInterval(t, t1);
			}
			if (currEnd != null && t.compareTo(currEnd) >= 0) {
				t.setMoment(currEnd);
			}
			if (!t.equals(currStart)) {
				currStart = t.copyTo(currStart);
				currStartPos = currStart.subtract(start);
				notifyIntervalChange();
				return true;
			}
		}
		return false;
	}

	/**
	* Returns true if actually changed
	*/
	public boolean setCurrIntervalEnd(TimeMoment t) {
		if (t == null) {
			TimeMoment t1 = (currStart == null) ? null : currStart.getCopy();
			return setCurrInterval(t1, t);
		}
		if (start != null && t.getPrecision() != start.getPrecision()) {
			t = t.getCopy();
			t.setPrecision(start.getPrecision());
		}
		if (t.compareTo(start) >= 0 && t.compareTo(end) <= 0) {
			if (whatIsFixed == LENGTH) {
				long len = t.subtract((currEnd == null) ? end : currEnd);
				TimeMoment t1 = (currStart == null) ? start.getCopy() : currStart.getCopy();
				t1.add((int) len);
				return setCurrInterval(t1, t);
			}
			if (currStart != null && t.compareTo(currStart) <= 0) {
				t.setMoment(currStart);
			}
			if (!t.equals(currEnd)) {
				currEnd = t.copyTo(currEnd);
				currEndPos = currEnd.subtract(start);
				notifyIntervalChange();
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns true if both objects are either null or equal
	 */
	private static boolean same(Object o1, Object o2) {
		return (o1 == null && o2 == null) || (o1 != null && o2 != null && o1.equals(o2));
	}

	/**
	* Returns true if actually changed
	*/
	public boolean setCurrInterval(TimeMoment t1, TimeMoment t2) {
		if (t1 != null && t2 != null && t1.compareTo(t2) > 0)
			return false;
		if (t1 != null && start != null && t1.getPrecision() != start.getPrecision()) {
			t1 = t1.getCopy();
			t1.setPrecision(start.getPrecision());
		}
		if (t2 != null && start != null && t2.getPrecision() != start.getPrecision()) {
			t2 = t2.getCopy();
			t2.setPrecision(start.getPrecision());
		}
		if (t1 != null && t1.compareTo(start) <= 0) {
			t1.setMoment(start);
		}
		if (t2 != null && t2.compareTo(end) >= 0) {
			t2.setMoment(end);
		}
		if (same(t1, currStart) && same(t2, currEnd))
			return false;
		boolean changed = false;
		if (!same(t1, currStart)) {
			currStart = (t1 == null) ? null : t1.copyTo(currStart);
			currStartPos = (currStart == null) ? 0 : currStart.subtract(start);
			changed = true;
		}
		if (!same(t2, currEnd)) {
			currEnd = (t2 == null) ? null : t2.copyTo(currEnd);
			currEndPos = (currEnd == null) ? iLen : currEnd.subtract(start);
			changed = true;
		}
		if (changed) {
			notifyIntervalChange();
		}
		return changed;
	}

	/**
	* Returns true if actually changed
	*/
	public boolean setCurrIntervalLength(long l) {
		if (l < 1)
			return false;
		if (l > iLen) {
			l = iLen;
		}
		if (l == getCurrIntervalLength())
			return false;
		TimeMoment t2 = (currStart == null) ? start.getCopy() : currStart.getCopy();
		t2.add((int) l);
		if (t2.compareTo(end) <= 0) {
			setCurrInterval(currStart, t2);
			return true;
		}
		t2.setMoment(end);
		TimeMoment t1 = t2.getCopy();
		t1.add((int) -l);
		setCurrInterval(t1, t2);
		return true;
	}

	/**
	* Sets the current interval to the whole data interval. Returns true if
	* actually changed.
	*/
	public boolean showWholeInterval() {
		if (start == null && end == null)
			return false;
		if (same(currStart, start) && same(currEnd, end))
			return false;
		if (currStart == null) {
			currStart = start.getCopy();
		} else {
			currStart.setMoment(start);
		}
		currStartPos = 0;
		if (currEnd == null) {
			currEnd = end.getCopy();
		} else {
			currEnd.setMoment(end);
		}
		currEndPos = iLen;
		notifyIntervalChange();
		return true;
	}

	/**
	 * Clears the current time limits
	 */
	public boolean clearTimeLimits() {
		if (start == null && end == null)
			return false;
		if (currStart == null && currEnd == null)
			return false;
		currStartPos = 0;
		currEndPos = iLen;
		currStart = null;
		currEnd = null;
		notifyIntervalChange();
		return true;
	}

	/**
	 * Informs whether any time limits are currently set
	 */
	public boolean hasTimeLimits() {
		if (start == null && end == null)
			return false;
		if (currStart == null && currEnd == null)
			return false;
		if ((currStart == null || currStart.equals(start)) && (currEnd == null || currEnd.equals(end)))
			return false;
		return true;
	}

	/**
	* Sets what must be fixed during dragging: interval beginning, end, or length.
	* The value must be one of the constants START(=1), END(=2), or LENGTH(=3)
	*/
	public void setWhatIsFixed(int value) {
		if (value >= NONE && value <= LENGTH) {
			whatIsFixed = value;
		}
	}

	/**
	* Returns what is fixed during dragging: interval beginning, end, or length.
	* The return value is one of the constants START(=1), END(=2), or LENGTH(=3)
	*/
	public int getWhatIsFixed() {
		return whatIsFixed;
	}

	public boolean moveIntervalBy(long absdx) {
		if (absdx == 0)
			return false;
		boolean left = absdx < 0;
		if (left) {
			absdx = -absdx;
		}
		if (left && currStartPos - absdx < 0) {
			absdx = currStartPos;
		}
		if (!left && currEndPos + absdx > iLen) {
			absdx = iLen - currEndPos;
		}
		if (absdx <= 0)
			return false;
		if (left) {
			absdx = -absdx;
		}
		if (currStart == null) {
			currStart = start.getCopy();
		}
		if (currEnd == null) {
			currEnd = end.getCopy();
		}
		TimeMoment t1 = currStart.getCopy(), t2 = currEnd.getCopy();
		long len = currEnd.subtract(currStart);
		currStartPos += absdx;
		currEndPos += absdx;
		currStart.add((int) absdx);
		currEnd.add((int) absdx);
		if (currEnd.compareTo(end) > 0) {
			end.copyTo(currEnd);
			end.copyTo(currStart);
			currStart.add((int) -len);
			currEndPos = iLen;
			currStartPos = iLen - len;
		}
		if (currEnd.subtract(currStart) != len) {
			currEnd.copyTo(currStart);
			currStart.add((int) -len);
		}
		if (currStart.compareTo(start) < 0) {
			start.copyTo(currStart);
		}
		if (currEnd.compareTo(end) > 0) {
			end.copyTo(currEnd);
		}
		if (t1.equals(currStart) && t2.equals(currEnd))
			return false;
		notifyIntervalChange();
		return true;
	}

	public boolean moveStartBy(long absdx) {
		if (absdx == 0)
			return false;
		boolean left = absdx < 0;
		if (left) {
			absdx = -absdx;
		}
		if (left && currStartPos - absdx < 0) {
			absdx = currStartPos;
		}
		if (!left && currStartPos + absdx > currEndPos) {
			absdx = currEndPos - currStartPos;
		}
		if (absdx <= 0)
			return false;
		if (left) {
			absdx = -absdx;
		}
		currStartPos += absdx;
		TimeMoment t = (currStart == null) ? null : currStart.getCopy();
		if (currStart == null) {
			currStart = start.getCopy();
		}
		currStart.add((int) absdx);
		if (currStart.compareTo(start) < 0) {
			start.copyTo(currStart);
		}
		if (currEnd != null)
			if (currStart.compareTo(currEnd) > 0) {
				currEnd.copyTo(currStart);
			} else {
				;
			}
		else if (currStart.compareTo(end) > 0) {
			end.copyTo(currStart);
		}
		if (currStart.equals(t))
			return false;
		notifyIntervalChange();
		return true;
	}

	public boolean moveEndBy(long absdx) {
		if (absdx == 0)
			return false;
		boolean left = absdx < 0;
		if (left) {
			absdx = -absdx;
		}
		if (left && currEndPos - absdx < currStartPos) {
			absdx = currEndPos - currStartPos;
		}
		if (!left && currEndPos + absdx > iLen) {
			absdx = iLen - currEndPos;
		}
		if (absdx <= 0)
			return false;
		if (left) {
			absdx = -absdx;
		}
		//System.out.println("currStartPos="+currStartPos+", currEndPos="+currEndPos+
		//                   ", dx="+absdx);
		currEndPos += absdx;
		TimeMoment t = (currEnd == null) ? null : currEnd.getCopy();
		if (currEnd == null) {
			currEnd = end.getCopy();
		}
		currEnd.add((int) absdx);
		if (currStart != null)
			if (currEnd.compareTo(currStart) < 0) {
				currStart.copyTo(currEnd);
			} else {
				;
			}
		else if (currEnd.compareTo(start) < 0) {
			start.copyTo(currEnd);
		}
		if (currEnd.compareTo(end) > 0) {
			end.copyTo(currEnd);
		}
		if (currEnd.equals(t))
			return false;
		//System.out.println("currEnd="+currEnd.toString()+", currEndPos="+currEndPos);
		notifyIntervalChange();
		return true;
	}

	/**
	* Returns the properties which can be stored in an ASCII file. Each property
	* has its unique key and a value, both are strings.
	*/
	public Hashtable getProperties() {
		Hashtable prop = new Hashtable();
		if (start != null) {
			prop.put("start_pos", String.valueOf(currStartPos));
		}
		if (end != null) {
			prop.put("end_pos", String.valueOf(currEndPos));
		}
		prop.put("whatIsFixed", String.valueOf(whatIsFixed));
		return prop;
	}

	/**
	* Restores the properties, which could be, for example, retrieved from a file.
	* Each property has its unique key and a value, both are strings.
	*/
	public void setProperties(Hashtable param) {
		if (param == null || param.isEmpty())
			return;
		String str = (String) param.get("whatIsFixed");
		if (str != null) {
			try {
				int w = Integer.valueOf(str).intValue();
				if (w == START || w == END || w == LENGTH) {
					whatIsFixed = w;
				}
			} catch (NumberFormatException e) {
			}
		}
		str = (String) param.get("start_pos");
		long sp = -1, ep = -1;
		if (str != null) {
			try {
				sp = Long.valueOf(str).longValue();
			} catch (NumberFormatException e) {
			}
		}
		str = (String) param.get("end_pos");
		if (str != null) {
			try {
				ep = Long.valueOf(str).longValue();
			} catch (NumberFormatException e) {
			}
		}
		if (sp < 0) {
			sp = 0;
		} else if (sp > iLen) {
			sp = iLen;
		}
		if (ep < sp) {
			ep = sp;
		} else if (ep > iLen) {
			ep = iLen;
		}
		if (sp != currStartPos || ep != currEndPos) {
			if (sp != currStartPos) {
				if (currStart == null) {
					currStart = start.getCopy();
				}
				currStart.add((int) (sp - currStartPos));
				currStartPos = sp;
			}
			if (ep != currEndPos) {
				if (currEnd == null) {
					currEnd = end.getCopy();
				}
				currEnd.add((int) (ep - currEndPos));
				currEndPos = ep;
			}
			notifyIntervalChange();
		}
	}
}