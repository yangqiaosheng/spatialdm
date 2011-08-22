package spade.time;

import java.util.ResourceBundle;

import spade.lib.lang.Language;
import spade.lib.util.Comparable;

/**
* A TimeCount is a simple implementation of the interface TimeMoment: the
* time moments are just integer numbers.
*/
public class TimeCount implements TimeMoment, java.io.Serializable {
	static ResourceBundle res = Language.getTextResource("spade.time.Res");
	/**
	 * The time symbols that can be returned as units
	 */
	public static final char time_symbols[] = { 'y', 'm', 'd', 'h', 't', 's' };

	/**
	 * Returns the index of the given time unit in the array time_symbols
	 * @param unit  - one of the following characters:
	 *   's' - seconds, 't' - minutes, 'h' - hours, 'd' - days, 'm' - month, 'y' - years.
	 * @return the index or -1 if the symbol is incvalid
	 */
	public static int getTimeUnitIdx(char unit) {
		for (int i = 0; i < time_symbols.length; i++)
			if (unit == time_symbols[i])
				return i;
		return -1;
	}

	/**
	* The integer number representing this time moment.
	*/
	protected long t = 0L;
	/**
	 * A string (optional) representing this moment
	 */
	public String shownValue = null;
	/**
	 * The precision (time scale) with which all operations over dates are
	 * done. The precision is specified in terms of the smallest significant units:
	 * 's' - seconds, 't' - minutes, 'h' - hours, 'd' - days, 'm' - month,
	 * 'y' - years.
	 *  The variable specifies the index of the unit in the array time_symbols
	 */
	public int precIdx = -1;

	public TimeCount() {
	}

	/**
	* Initializes the TimeCount with the given integer number.
	*/
	public TimeCount(long moment) {
		t = moment;
	}

	/**
	* Sets the given number as the current time moment.
	*/
	public boolean setMoment(long moment) {
		t = moment;
		return true;
	}

	/**
	* Sets this TimeMoment to be equal to the given TimeMoment. Returns true
	* if successful.
	*/
	@Override
	public boolean setMoment(TimeMoment moment) {
		if (moment == null || !(moment instanceof TimeCount))
			return false;
		TimeCount tc = (TimeCount) moment;
		return setMoment(tc.getMoment());
	}

	/**
	* Setups this TimeMoment using the given string representation of a time
	* moment. Returns true if successful.
	*/
	@Override
	public boolean setMoment(String timeStr) {
		if (timeStr == null)
			return false;
		try {
			t = Integer.valueOf(timeStr).intValue();
		} catch (NumberFormatException nfe) {
			return false;
		}
		return true;
	}

	/**
	* Returns the integer number representing this time moment.
	*/
	public long getMoment() {
		return t;
	}

	/**
	 * Resets the current moment to zero
	 */
	@Override
	public void reset() {
		t = 0L;
	}

	/**
	 * Returns null.
	 */
	@Override
	public char[] getAvailableTimeElements() {
		return null;
	}

	/**
	* Returns -1 (no date elements are used).
	*/
	@Override
	public int getElementValue(char elem) {
		return -1;
	}

	/**
	 * Returns the minimum possible precision for specifying time moments:
	 * 's' - seconds, 't' - minutes, 'h' - hours, 'd' - days, 'm' - month,
	 * 'y' - years.
	 * May return 0 (zero), if this is an abstract time count.
	 */
	@Override
	public char getMinPrecision() {
		return getPrecision();
	}

	/**
	 * Returns the maximum possible precision for specifying time moments:
	 * 's' - seconds, 't' - minutes, 'h' - hours, 'd' - days, 'm' - month,
	 * 'y' - years.
	 * For TimeCount, the maximum precision is the same as the minimum precision.
	 */
	@Override
	public char getMaxPrecision() {
		return getPrecision();
	}

	/**
	* Sets the precision (time scale). The precision is specified in terms of the
	* smallest significant units:
	* 's' - seconds, 't' - minutes, 'h' - hours, 'd' - days, 'm' - month,
	* 'y' - years.
	*/
	@Override
	public void setPrecision(char unit) {
		precIdx = getTimeUnitIdx(unit);
	}

	/**
	 * Returns the precision (time scale) with which all operations over dates are
	 * done. The precision is specified in terms of the smallest significant units:
	 * 's' - seconds, 't' - minutes, 'h' - hours, 'd' - days, 'm' - month,
	 * 'y' - years.
	 * May return 0 (zero), if this is an abstract time count.
	 */
	@Override
	public char getPrecision() {
		if (precIdx < 0)
			return 0;
		return time_symbols[precIdx];
	}

	/**
	 * Returns the precision as the index of the unit in the array time_symbols
	 */
	@Override
	public int getPrecisionIdx() {
		return precIdx;
	}

	/**
	 * Must checks if the current precision is feasible, i.e. there is a corresponding
	 * date element. Does not do anything in TimeCount.
	 */
	@Override
	public void checkPrecision() {
	}

	/**
	* Changes its internal variables so that the result is the next time moment
	* (next day, next month, next second etc., depending on the time scale)
	*/
	@Override
	public void stepForth() {
		++t;
	}

	/**
	* Changes its internal variables so that the result is the previous time
	* moment (opposite to stepForth)
	*/
	@Override
	public void stepBack() {
		--t;
	}

	/**
	* Changes its internal variables so that the result is the n-th moment
	* starting from the current moment (n may be negative)
	* n is number of units to add. The units depend on the scale and may be
	* days, years, seconds, etc.
	*/
	@Override
	public void add(long n) {
		t += n;
	}

	/*
	* Creates a new TimeMoment object equal to this moment.
	*/
	@Override
	public TimeMoment getCopy() {
		return copyTo(null);
	}

	/**
	* Sets the given TimeMoment to be equal to this moment. If the time moment t
	* is null, creates this object.
	*/
	@Override
	public TimeMoment copyTo(TimeMoment tm) {
		if (tm == null || !(tm instanceof TimeCount))
			return new TimeCount(t);
		TimeCount tc = (TimeCount) tm;
		tc.setMoment(t);
		tc.precIdx = precIdx;
		return tc;
	}

	/*
	* Creates a new TimeMoment object with the next moment. The internal settings
	* remain unchanged (cf. stepForth)
	*/
	@Override
	public TimeMoment getNext() {
		return new TimeCount(t + 1);
	}

	/*
	* Creates a new TimeMoment object with the previous moment. The internal
	* settings remain unchanged (cf. stepBack)
	*/
	@Override
	public TimeMoment getPrevious() {
		return new TimeCount(t - 1);
	}

	/**
	* Compares itself with another TimeMoment tm. Returns true if the moments are
	* equal.
	*/
	@Override
	public boolean equals(Object tm) {
		if (tm == null || !(tm instanceof TimeCount))
			return false;
		TimeCount tc = (TimeCount) tm;
		return tc.getMoment() == t;
	}

	/**
	* Compares itself with another TimeMoment tm. Returns -1 if this moment is
	* earlier than tm, 0 if they are the same moments, and 1 if this moment is
	* later than tm. 0 may be also returned if the moments are uncompatible,
	* for example, seconds and years.
	*/
	@Override
	public int compareTo(Comparable tm) {
		if (tm == null)
			return -1;
		if (!(tm instanceof TimeCount))
			return 0;
		TimeCount tc = (TimeCount) tm;
		if (tc.getMoment() == t)
			return 0;
		if (tc.getMoment() < t)
			return 1;
		return -1;
	}

	/**
	* Finds the difference between this moment and the given moment tm (in the
	* units depending on the scale)
	*/
	@Override
	public long subtract(TimeMoment tm) {
		if (tm == null || !(tm instanceof TimeCount))
			return 0l;
		TimeCount tc = (TimeCount) tm;
		return t - tc.getMoment();
	}

	/**
	* Finds the difference between this moment and the given moment t in the
	* specified units: 's', 't', 'h', 'd', 'm', or 'y'.
	 * For TimeCount, returns subtract(t)
	*/
	@Override
	public long subtract(TimeMoment t, char unit) {
		return subtract(t);
	}

	/**
	 * Sets a string to represent this moment
	 */
	public void setShownValue(String shownValue) {
		this.shownValue = shownValue;
	}

	/**
	* Returns its representation as a string
	*/
	@Override
	public String toString() {
		if (shownValue != null)
			return shownValue;
		return String.valueOf(t);
	}

	/**
	* Returns its representation as a number
	*/
	@Override
	public long toNumber() {
		return t;
	}

	/**
	* Converts the given number to a time moment
	*/
	@Override
	public TimeMoment valueOf(long n) {
		return new TimeCount(n);
	}

	/**
	* Returns the name of the (smallest) unit, in singular, in which the time
	* moments are specified, e.g. second or year. May return null.
	*/
	@Override
	public String getUnit() {
		char precision = getPrecision();
		if (precision == 0)
			return null;
		if (precision == 's')
			return res.getString("sec_");
		if (precision == 't')
			return res.getString("min_");
		if (precision == 'h')
			return res.getString("hour");
		if (precision == 'd')
			return res.getString("day");
		if (precision == 'm')
			return res.getString("month");
		if (precision == 'y')
			return res.getString("year");
		return null;
	}

	/**
	* Returns the name of the (smallest) units, in plural, in which the time
	* moments are specified, e.g. seconds or years. May return null.
	*/
	@Override
	public String getUnits() {
		char precision = getPrecision();
		if (precision == 0)
			return null;
		if (precision == 's')
			return res.getString("sec_");
		if (precision == 't')
			return res.getString("min_");
		if (precision == 'h')
			return res.getString("hours");
		if (precision == 'd')
			return res.getString("days");
		if (precision == 'm')
			return res.getString("months");
		if (precision == 'y')
			return res.getString("years");
		return null;
	}

	/**
	 * Returns the text for the given time symbol, e.g. "year" for "y" etc.
	 */
	public static String getTextForTimeSymbol(char symb) {
		switch (symb) {
		case 'y':
		case 'Y':
			return res.getString("year");
		case 'm':
		case 'M':
			return res.getString("month");
		case 'd':
		case 'D':
			return res.getString("day");
		case 'h':
		case 'H':
			return res.getString("hour");
		case 't':
		case 'T':
			return res.getString("minute");
		case 's':
		case 'S':
			return res.getString("second");
		}
		return null;
	}

	/**
	 * Returns the text for the given time unit symbol, e.g. "year" for "y" etc.
	 */
	@Override
	public String getTextForUnit(char symb) {
		return getTextForTimeSymbol(symb);
	}

	/**
	 * Returns null
	 */
	@Override
	public String getErrorMessage() {
		return null;
	}

	/**
	 * Always returns true.
	 */
	@Override
	public boolean isValid() {
		return true;
	}

	/**
	 * Informs if this date is "nice"
	 */
	@Override
	public boolean isNice() {
		return t % 10 == 0;
	}
}