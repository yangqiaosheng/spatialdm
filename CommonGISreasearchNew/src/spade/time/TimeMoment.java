package spade.time;

import spade.lib.util.Comparable;

public interface TimeMoment extends Comparable {
	/**
	* Sets this TimeMoment to be equal to the given TimeMoment. Returns true
	* if successful.
	*/
	public boolean setMoment(TimeMoment moment);

	/**
	* Setups this TimeMoment using the given string representation of a time
	* moment. Returns true if successful.
	*/
	public boolean setMoment(String timeStr);

	/**
	 * Resets the current moment to zero
	 */
	public void reset();

	/**
	 * Checks the validity of the current date. Returns true if valid.
	 * Otherwise, the method getErrorMessage() returns an explanation of the
	 * error.
	 */
	public boolean isValid();

	/**
	 * Returns the error message, which explains why the date is invalid
	 */
	public String getErrorMessage();

	/**
	* Returns the minimum possible precision for specifying time moments:
	* 's' - seconds, 't' - minutes, 'h' - hours, 'd' - days, 'm' - month,
	* 'y' - years.
	*/
	public char getMinPrecision();

	/**
	* Returns the maximum possible precision for specifying time moments:
	* 's' - seconds, 't' - minutes, 'h' - hours, 'd' - days, 'm' - month,
	* 'y' - years.
	*/
	public char getMaxPrecision();

	/**
	* Sets the precision (time scale) with which all operations over dates will be
	* done. The precision is specified in terms of the smallest significant units:
	* 's' - seconds, 't' - minutes, 'h' - hours, 'd' - days, 'm' - month,
	* 'y' - years.
	*/
	public void setPrecision(char unit);

	/**
	* Returns the precision (time scale) with which all operations over dates are
	* done. The precision is specified in terms of the smallest significant units:
	* 's' - seconds, 't' - minutes, 'h' - hours, 'd' - days, 'm' - month,
	* 'y' - years.
	*/
	public char getPrecision();

	/**
	 * Returns the precision as the index of the unit in the array time_symbols
	 */
	public int getPrecisionIdx();

	/**
	 * Checks if the current precision is feasible, i.e. there is a corresponding
	 * date element. If not, corrects the precision.
	 */
	public void checkPrecision();

	/**
	 * Returns all date/time elements present in the specification of the time
	 * moment, from the smallest to the biggest. For example:
	 * 's' - seconds, 't' - minutes, 'h' - hours, 'd' - days, 'm' - month,
	 * 'y' - years.
	 * May return null.
	 */
	public char[] getAvailableTimeElements();

	/**
	* Returns the value of the given element:
	* 's' - seconds, 't' - minutes, 'h' - hours, 'd' - days, 'm' - month,
	* 'y' - years.
	* Returns -1 if there is no such element.
	*/
	public int getElementValue(char elem);

	/**
	* Changes its internal variables so that the result is the next time moment
	* (next day, next month, next second etc., depending on the time scale)
	*/
	public void stepForth();

	/**
	* Changes its internal variables so that the result is the previous time
	* moment (opposite to stepForth)
	*/
	public void stepBack();

	/**
	* Changes its internal variables so that the result is the n-th moment
	* starting from the current moment (n may be negative)
	* n is number of units to add. The units depend on the scale and may be
	* days, years, seconds, etc.
	*/
	public void add(long n);

	/*
	* Creates a new TimeMoment object equal to this moment.
	*/
	public TimeMoment getCopy();

	/**
	* Sets the given TimeMoment to be equal to this moment. If the time moment t
	* is null, creates this object.
	*/
	public TimeMoment copyTo(TimeMoment tm);

	/*
	* Creates a new TimeMoment object with the next moment. The internal settings
	* remain unchanged (cf. stepForth)
	*/
	public TimeMoment getNext();

	/*
	* Creates a new TimeMoment object with the previous moment. The internal
	* settings remain unchanged (cf. stepBack)
	*/
	public TimeMoment getPrevious();

	/**
	* Compares itself with another TimeMoment t. Returns true if the moments are
	* equal.
	*/
	@Override
	public boolean equals(Object t);

	/**
	* Compares itself with another TimeMoment t. Returns -1 if this moment is
	* earlier than t, 0 if they are the same moments, and 1 if this moment is
	* later than t. 0 may be also returned if the moments are uncompatible,
	* for example, seconds and years.
	*/
	@Override
	public int compareTo(Comparable t);

	/**
	* Finds the difference between this moment and the given moment t (in the
	* units depending on the scale)
	*/
	public long subtract(TimeMoment t);

	/**
	* Finds the difference between this moment and the given moment t in the
	* specified units: 's', 't', 'h', 'd', 'm', or 'y'
	*/
	public long subtract(TimeMoment t, char unit);

	/**
	* Returns its representation as a string
	*/
	@Override
	public String toString();

	/**
	* Returns its representation as a number
	*/
	public long toNumber();

	/**
	* Converts the given number to a time moment
	*/
	public TimeMoment valueOf(long n);

	/**
	* Returns the name of the (smallest) unit, in singular, in which the time
	* moments are specified, e.g. second or year. May return null.
	*/
	public String getUnit();

	/**
	* Returns the name of the (smallest) units, in plural, in which the time
	* moments are specified, e.g. seconds or years. May return null.
	*/
	public String getUnits();

	/**
	 * Returns the text for the given time unit symbol, e.g. "year" for "y" etc.
	 * May return null.
	 */
	public String getTextForUnit(char symb);

	/**
	 * Informs if this date is "nice", depending on the current precision
	 */
	public boolean isNice();
}