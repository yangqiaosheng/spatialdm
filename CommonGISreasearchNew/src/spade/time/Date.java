package spade.time;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.ResourceBundle;

import spade.lib.lang.Language;
import spade.lib.util.Comparable;
import spade.lib.util.StringUtil;

public class Date implements TimeMoment, java.io.Serializable {
	static ResourceBundle res = Language.getTextResource("spade.time.Res");
	/**
	 * The time symbols that can be used in date templates (schemes)
	 */
	public static final char time_symbols[] = { 'y', 'm', 'd', 'h', 't', 's' };
	/**
	* The default date scheme for the case when day, month, and year are specified.
	*/
	public static final String defDateOfTheYearScheme = "dd.mm.yyyy";
	/**
	* The default date scheme for the case when hour, minute, and second are specified.
	*/
	public static final String defTimeOfTheDayScheme = "hh:tt:ss";
	/**
	* The default date scheme for the case when full dates including day, month,
	* year, hour, minute, and second are specified.
	*/
	public static final String defFullDateScheme = "dd.mm.yyyy; hh:tt:ss";

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
	* Checks if the given symbol is a right time/date symbol, i.e. one of the
	* symbols 's','t','h','d','m','y', or 'a'.
	*/
	public static boolean isTimeSymbol(char symbol) {
		if (symbol == 'a')
			return true;
		for (char time_symbol : time_symbols)
			if (symbol == time_symbol)
				return true;
		return false;
	}

	/**
	 * Checks if the given "scheme" actually specifies a simple time count, i.e.
	 * with a single element.
	 */
	public static boolean isSimple(String scheme) {
		if (scheme == null)
			return true;
		scheme = scheme.trim();
		if (scheme.length() < 1)
			return true;
		char first = scheme.charAt(0);
		if (!isTimeSymbol(first))
			return false;
		for (int i = 1; i < scheme.length(); i++)
			if (scheme.charAt(i) != first)
				return false;
		return true;
	}

	/**
	* The elements of a date
	*/
	public int year = -1, month = -1, day = -1, hour = -1, min = -1, sec = -1;
	/**
	 * Numeric representation of the date; <0 means invalid
	 */
	protected long numVal = -1;
	/**
	 * The precision for which numVal was computed
	 */
	protected int numValPrecIdx = -1;
	/**
	 * Indicates which elements of a date are present. The length of the array is
	 * the same as the length of time_symbols.
	 */
	protected boolean hasDateElement[] = { false, false, false, false, false, false };

	/**
	* Checks if the given date element is present. The date element is specified
	* as follows:
	* 's' - seconds, 't' - minutes, 'h' - hours, 'd' - days, 'm' - month,
	* 'y' - years.
	*/
	public boolean hasElement(char elem) {
		int idx = getTimeUnitIdx(elem);
		if (idx < 0)
			return false;
		return hasDateElement[idx];
	}

	public boolean hasElement(int unitIdx) {
		if (unitIdx < 0)
			return false;
		return hasDateElement[unitIdx];
	}

	/**
	 * Error message: explains why the date is invalid
	 */
	protected String err = null;

	/**
	 * Returns the error message, which explains why the date is invalid
	 */
	@Override
	public String getErrorMessage() {
		return err;
	}

	/**
	 * Resets the current moment to zero
	 */
	@Override
	public void reset() {
		for (int i = 0; i < time_symbols.length; i++)
			if (hasDateElement[i]) {
				switch (time_symbols[i]) {
				case 's':
					sec = 0;
					break;
				case 't':
					min = 0;
					break;
				case 'h':
					hour = 0;
					break;
				case 'd':
					day = (hasElement('m')) ? 1 : 0;
					break;
				case 'm':
					month = (hasElement('y')) ? 1 : 0;
					break;
				case 'y':
					year = 2000;
					break;
				}
			}
		numVal = -1;
	}

	/**
	 * Checks the validity of the current date. Returns true if valid.
	 * Otherwise, the method getErrorMessage() returns an explanation of the
	 * error.
	 */
	@Override
	public boolean isValid() {
		boolean set = false;
		err = null;
		for (int i = 0; i < time_symbols.length; i++)
			if (hasDateElement[i]) {
				set = true;
				switch (time_symbols[i]) {
				case 's':
					if (sec < 0 || (sec >= 60 && hasElement('t'))) {
						err = "Invalid number of seconds: " + sec;
						return false;
					}
					break;
				case 't':
					if (min < 0 || (min >= 60 && hasElement('h'))) {
						err = "Invalid number of minutes: " + min;
						return false;
					}
					break;
				case 'h':
					if (hour < 0 || (hour >= 24 && hasElement('d'))) {
						err = "Invalid number of hours: " + hour;
						return false;
					}
					break;
				case 'd':
					boolean ok = (hasElement('m')) ? day > 0 : day >= 0;
					if (ok && hasElement('m')) {
						ok = day <= 31;
						if (ok && day > 28) {
							switch (month) {
							case 4:
							case 6:
							case 9:
							case 11:
								ok = day <= 30;
								break;
							case 2:
								ok = day <= 29 && hasElement('y') && nDaysInYear(year) == 366;
								break;
							}
						}
					}
					if (!ok) {
						err = "Invalid number of day: " + day;
						if (hasElement('m')) {
							err += " in month " + month;
						}
						return false;
					}
					break;
				case 'm':
					if (month < 0 || (month > 12 && hasElement('y'))) {
						err = "Invalid number of month: " + month;
						return false;
					}
					break;
				case 'y':
					if (year < 0) {
						err = "Invalid year: " + year;
						return false;
					}
					break;
				}
			}
		if (!set) {
			err = "The date is not set up!";
			return false;
		}
		return true;
	}

	/**
	 * Returns all date/time elements present in the specification of the time
	 * moment, from the smallest to the biggest. May return null.
	 */
	@Override
	public char[] getAvailableTimeElements() {
		int len = 0;
		for (int i = 0; i < time_symbols.length; i++)
			if (hasDateElement[i]) {
				++len;
			}
		if (len < 1)
			return null;
		char elements[] = new char[len];
		len = 0;
		for (int i = time_symbols.length - 1; i >= 0; i--)
			if (hasDateElement[i]) {
				elements[len++] = time_symbols[i];
			}
		return elements;
	}

	/**
	* Sets the indicator of the presence or absence of the given date element.
	* The date element is specified as follows:
	* 's' - seconds, 't' - minutes, 'h' - hours, 'd' - days, 'm' - month,
	* 'y' - years.
	*/
	public void setHasElement(char elem, boolean value) {
		int idx = getTimeUnitIdx(elem);
		if (idx < 0)
			return;
		hasDateElement[idx] = value;
		numVal = -1;
		return;
	}

	/**
	* The date scheme currently used
	*/
	public String scheme = null;
	/**
	 * The precision (time scale) with which all operations over dates are
	 * done. The precision is specified in terms of the smallest significant units:
	 * 's' - seconds, 't' - minutes, 'h' - hours, 'd' - days, 'm' - month,
	 * 'y' - years.
	 *  The variable specifies the index of the unit in the array time_symbols
	 */
	public int precIdx = time_symbols.length - 1;

	public void setYear(int year) {
		this.year = year;
		setHasElement('y', true);
		numVal = -1;
	}

	public void setMonth(int month) {
		this.month = month;
		setHasElement('m', true);
		numVal = -1;
	}

	public void setDay(int day) {
		this.day = day;
		setHasElement('d', true);
		numVal = -1;
	}

	public void setHour(int hour) {
		this.hour = hour;
		setHasElement('h', true);
		numVal = -1;
	}

	public void setMinute(int minute) {
		this.min = minute;
		setHasElement('t', true);
		numVal = -1;
	}

	public void setSecond(int second) {
		this.sec = second;
		setHasElement('s', true);
		numVal = -1;
	}

	public void setDate(java.util.Date dateSQL, java.sql.Time timeSQL) {
		GregorianCalendar gc = new GregorianCalendar();
		gc.setTime(dateSQL);
		setYear(gc.get(Calendar.YEAR));
		if (requiresElement('m')) {
			setMonth(1 + gc.get(Calendar.MONTH)); // I have no idea WHY?
		}
		if (requiresElement('d')) {
			setDay(gc.get(Calendar.DAY_OF_MONTH));
		}
		if (timeSQL != null) {
			if (requiresElement('h')) {
				setHour(timeSQL.getHours());
			}
			if (requiresElement('t')) {
				setMinute(timeSQL.getMinutes());
			}
			if (requiresElement('s')) {
				setSecond(timeSQL.getSeconds());
			}
		}
		numVal = -1;
	}

	/**
	* Sets this TimeMoment to be equal to the given TimeMoment. Returns true
	* if successful.
	*/
	@Override
	public boolean setMoment(TimeMoment moment) {
		if (moment == null || !(moment instanceof Date))
			return false;
		Date d = (Date) moment;
		if (d.hasElement('y')) {
			setYear(d.year);
		}
		if (d.hasElement('m')) {
			setMonth(d.month);
		}
		if (d.hasElement('d')) {
			setDay(d.day);
		}
		if (d.hasElement('h')) {
			setHour(d.hour);
		}
		if (d.hasElement('t')) {
			setMinute(d.min);
		}
		if (d.hasElement('s')) {
			setSecond(d.sec);
		}
		numVal = -1;
		return isSet();
	}

	/**
	* Sets the date scheme to be used
	*/
	public void setDateScheme(String scheme) {
		this.scheme = scheme.toLowerCase();
	}

	/**
	* Setups this TimeMoment using the given string representation of a time
	* moment. Returns true if successful. Needs the date scheme to be previously set.
	*/
	@Override
	public boolean setMoment(String timeStr) {
		if (timeStr == null || scheme == null)
			return false;
		int i = 0;
		int minLength = Math.min(scheme.length(), timeStr.length());
		while (i < minLength) {
			char ch = scheme.charAt(i);
			if (ch == 's' || ch == 't' || ch == 'h' || ch == 'd' || ch == 'm' || ch == 'y') {
				int len = 1;
				for (int j = i + 1; j < minLength && scheme.charAt(j) == ch; j++) {
					++len;
				}
				String val = timeStr.substring(i, i + len);
				int num = -1;
				try {
					num = Integer.valueOf(val).intValue();
				} catch (NumberFormatException e) {
				}
				if (num >= 0)
					if (ch == 's') {
						setSecond(num);
					} else if (ch == 't') {
						setMinute(num);
					} else if (ch == 'h') {
						setHour(num);
					} else if (ch == 'd') {
						setDay(num);
					} else if (ch == 'm') {
						setMonth(num);
					} else if (ch == 'y') {
						if (len == 2 && num < 100)
							if (num > 20) {
								num += 1900;
							} else {
								num += 2000;
							}
						setYear(num);
					}
				i += len;
			} else {
				++i;
			}
		}
		numVal = -1;
		return isSet();
	}

	/**
	* Returns the minimum possible precision for specifying time moments:
	* 's' - seconds, 't' - minutes, 'h' - hours, 'd' - days, 'm' - month,
	* 'y' - years.
	*/
	@Override
	public char getMinPrecision() {
		for (int i = hasDateElement.length - 1; i >= 0; i--)
			if (hasDateElement[i])
				return time_symbols[i];
		return 0;
	}

	/**
	* Returns the maximum possible precision for specifying time moments:
	* 's' - seconds, 't' - minutes, 'h' - hours, 'd' - days, 'm' - month,
	* 'y' - years.
	*/
	public char getMaxPrecision() {
		for (int i = 0; i < hasDateElement.length; i++)
			if (hasDateElement[i])
				return time_symbols[i];
		return 0;
	}

	/**
	* Sets the precision (time scale) with which all operations over dates will be
	* done. The precision is specified in terms of the smallest significant units:
	* 's' - seconds, 't' - minutes, 'h' - hours, 'd' - days, 'm' - month,
	* 'y' - years.
	*/
	public synchronized void setPrecision(char unit) {
		int idx = getTimeUnitIdx(unit);
		if (idx >= 0 && idx != precIdx) {
			precIdx = idx;
		}
	}

	public synchronized void setPrecisionIdx(int idx) {
		if (idx != precIdx && idx >= 0 && idx < time_symbols.length) {
			precIdx = idx;
		}
	}

	/**
	 * Checks if the current precision is feasible, i.e. there is a corresponding
	 * date element. If not, corrects the precision.
	 */
	public void checkPrecision() {
		if (precIdx >= 0 && hasDateElement[precIdx])
			return;
		while (precIdx > 0 && !hasDateElement[precIdx]) {
			--precIdx;
		}
		if (!hasDateElement[precIdx]) {
			for (precIdx = 0; precIdx < hasDateElement.length && !hasDateElement[precIdx]; precIdx++) {
				;
			}
			if (precIdx >= hasDateElement.length) {
				precIdx = hasDateElement.length - 1;
			}
		}
		setDefaultScheme();
	}

	/**
	* Returns the precision (time scale) with which all operations over dates are
	* done. The precision is specified in terms of the smallest significant units:
	* 's' - seconds, 't' - minutes, 'h' - hours, 'd' - days, 'm' - month,
	* 'y' - years.
	*/
	public char getPrecision() {
		checkPrecision();
		return time_symbols[precIdx];
	}

	/**
	 * Returns the precision as the index of the unit in the array time_symbols
	 */
	public int getPrecisionIdx() {
		return precIdx;
	}

	/**
	 * Returns the highest possible precision for this date
	 */
	public int getHighestPossiblePrecisionIdx() {
		for (int i = hasDateElement.length - 1; i > 0; i--)
			if (hasDateElement[i])
				return i;
		return 0;
	}

	/**
	 * Sets the highest possible precision for this date
	 */
	public void setHighestPossiblePrecision() {
		int idx = getHighestPossiblePrecisionIdx();
		if (idx >= 0 && idx != precIdx) {
			precIdx = idx;
		}
	}

	/**
	* Checks whether the given date element is used in operations over dates,
	* depending on the current precision. The date element is specified as follows:
	* 's' - seconds, 't' - minutes, 'h' - hours, 'd' - days, 'm' - month,
	* 'y' - years.
	*/
	public boolean useElement(char elem) {
		checkPrecision();
		int idx = getTimeUnitIdx(elem);
		if (idx < 0)
			return false;
		if (!hasDateElement[idx])
			return false;
		return precIdx >= idx;
	}

	/**
	* Checks whether the given date element is required
	* depending on the current precision. The date element is specified as follows:
	* 's' - seconds, 't' - minutes, 'h' - hours, 'd' - days, 'm' - month,
	* 'y' - years.
	*/
	public boolean requiresElement(char elem) {
		int idx = getTimeUnitIdx(elem);
		if (idx < 0)
			return false;
		return precIdx >= idx;
	}

	/**
	 * Modifies the given scheme according to the given precision, i.e.
	 * removes unnecessary elements
	 */
	public static String removeExtraElementsFromScheme(String origScheme, char prec) {
		if (origScheme == null)
			return null;
		int idx = getTimeUnitIdx(prec);
		if (idx < 0 || idx >= time_symbols.length - 1)
			return origScheme;
		StringBuffer sb = new StringBuffer(origScheme.length());
		for (int i = 0; i < origScheme.length(); i++) {
			char c = origScheme.charAt(i);
			int k = getTimeUnitIdx(c);
			if (k > idx) {
				continue;
			}
			if (k >= 0) {
				sb.append(c);
				continue;
			}
			//this is a separator
			if (sb.length() < 1) {
				continue;
			}
			for (int j = i + 1; j < origScheme.length() && k < 0; j++) {
				k = getTimeUnitIdx(origScheme.charAt(j));
			}
			if (k >= 0 && k <= idx) {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	/**
	* Returns the value of the given element:
	* 's' - seconds, 't' - minutes, 'h' - hours, 'd' - days, 'm' - month,
	* 'y' - years.
	 * Returns -1 if there is no such element.
	*/
	public int getElementValue(char elem) {
		switch (elem) {
		case 's':
			return sec;
		case 't':
			return min;
		case 'h':
			return hour;
		case 'd':
			return day;
		case 'm':
			return month;
		case 'y':
			return year;
		}
		return -1;
	}

	// returns 1:Mon..7:Sun
	public int getDayOfWeek() {
		if (year < 1 || month < 1 || day < 1)
			return -1;
		GregorianCalendar gc = new GregorianCalendar();
		gc.set(year, month - 1, day);
		int dow = gc.get(gc.DAY_OF_WEEK) - 1;
		if (dow == 0) {
			dow = 7;
		}
		return dow;
	}

	public int getWeekOfYear() {
		if (year < 1 || month < 1 || day < 1)
			return -1;
		GregorianCalendar gc = new GregorianCalendar();
		gc.set(year, month - 1, day);
		int woy = gc.get(GregorianCalendar.WEEK_OF_YEAR);
		if (woy == 1 && month == 12) {
			woy = 53;
		}
		return woy;
	}

	public int getDayOfYear() {
		if (year < 1 || month < 1 || day < 1)
			return -1;
		GregorianCalendar gc = new GregorianCalendar();
		gc.set(year, month - 1, day);
		return gc.get(GregorianCalendar.DAY_OF_YEAR);
	}

	public int getDSTimeOffset() {
		if (year < 1 || month < 1 || day < 1)
			return 0;
		GregorianCalendar gc = new GregorianCalendar();
		gc.set(year, month - 1, day);
		int off = gc.get(GregorianCalendar.DST_OFFSET);
		if (off > 0) {
			off /= 3600000;
		}
		return off;
	}

	/**
	* Assigns the given value of the given element:
	* 's' - seconds, 't' - minutes, 'h' - hours, 'd' - days, 'm' - month,
	* 'y' - years.
	*/
	public void setElementValue(char elem, int value) {
		switch (elem) {
		case 's':
			setSecond(value);
			break;
		case 't':
			setMinute(value);
			break;
		case 'h':
			setHour(value);
			break;
		case 'd':
			setDay(value);
			break;
		case 'm':
			setMonth(value);
			break;
		case 'y':
			setYear(value);
		}
		numVal = -1;
	}

	/**
	* Returns the name of the (smallest) unit, in singular, in which the time
	* moments are specified, e.g. second or year. May return null.
	*/
	public String getUnit() {
		if (useElement('s'))
			return res.getString("sec_");
		if (useElement('t'))
			return res.getString("min_");
		if (useElement('h'))
			return res.getString("hour");
		if (useElement('d'))
			return res.getString("day");
		if (useElement('m'))
			return res.getString("month");
		if (useElement('y'))
			return res.getString("year");
		return null;
	}

	/**
	* Returns the name of the (smallest) units, in plural, in which the time
	* moments are specified, e.g. seconds or years. May return null.
	*/
	public String getUnits() {
		if (useElement('s'))
			return res.getString("sec_");
		if (useElement('t'))
			return res.getString("min_");
		if (useElement('h'))
			return res.getString("hours");
		if (useElement('d'))
			return res.getString("days");
		if (useElement('m'))
			return res.getString("months");
		if (useElement('y'))
			return res.getString("years");
		return null;
	}

	/**
	* Checks if any of the internal date or time variables have been set
	*/
	public boolean isSet() {
		for (boolean element : hasDateElement)
			if (element)
				return true;
		return false;
	}

	/**
	* Creates and sets a default scheme, depending on the current precision and
	* the date elements used
	*/
	public void setDefaultScheme() {
		StringBuffer sb = new StringBuffer(25);
		if (useElement('d')) {
			sb.append("dd");
			if (useElement('m')) {
				sb.append(".");
			}
		}
		if (useElement('m')) {
			sb.append("mm");
			if (useElement('y')) {
				sb.append(".");
			}
		}
		if (useElement('y')) {
			sb.append("yyyy");
		}
		if (sb.length() > 0 && (useElement('h') || useElement('t') || useElement('s'))) {
			sb.append("; ");
		}
		if (useElement('h')) {
			sb.append("hh");
			if (useElement('t')) {
				sb.append(":");
			} else {
				sb.append("h");
			}
		}
		if (useElement('t')) {
			sb.append("tt");
			if (useElement('s')) {
				sb.append(":");
			} else if (!useElement('h')) {
				sb.append("\'");
			}
		}
		if (useElement('s')) {
			sb.append("ss");
			if (!useElement('h')) {
				sb.append("\"");
			}
		}
		scheme = sb.toString();
	}

	/**
	 * Adjusts the scheme to the date depending on which elements are used
	 * (taking into account the desired precision)
	 */
	public void adjustScheme() {
		if (scheme == null) {
			setDefaultScheme();
			return;
		}
		for (int i = time_symbols.length - 1; i >= 0; i--)
			if (!useElement(time_symbols[i])) {
				int idx = scheme.indexOf(time_symbols[i]);
				if (idx < 0) {
					continue;
				}
				int lastIdx = idx;
				for (int j = idx + 1; j < scheme.length(); j++)
					if (scheme.charAt(j) == time_symbols[i]) {
						lastIdx = j;
					} else {
						break;
					}
				while (lastIdx + 1 < scheme.length() && !isTimeSymbol(scheme.charAt(lastIdx + 1))) {
					++lastIdx;
				}
				int idx0 = idx;
				for (int j = idx - 1; j >= 0; j--)
					if (!isTimeSymbol(scheme.charAt(j))) {
						idx0 = j;
					} else {
						break;
					}
				String str = (idx0 > 0) ? scheme.substring(0, idx0) : "";
				if (lastIdx + 1 < scheme.length()) {
					str += scheme.substring(lastIdx + 1);
				}
				scheme = str;
			}
		if (scheme.length() < 1) {
			setDefaultScheme();
		}
	}

	/**
	* Returns its representation as a string
	*/
	public String toString() {
		if (!isSet())
			return null;
		if (scheme == null) {
			setDefaultScheme();
		}
		int i = 0;
		StringBuffer sb = new StringBuffer(scheme.length() + 10);
		while (i < scheme.length()) {
			char ch = scheme.charAt(i);
			if (ch == 's' || ch == 't' || ch == 'h' || ch == 'd' || ch == 'm' || ch == 'y') {
				int len = 1;
				for (int j = i + 1; j < scheme.length() && scheme.charAt(j) == ch; j++) {
					++len;
				}
				i += len;
				int num = sec;
				if (ch == 't') {
					num = min;
				} else if (ch == 'h') {
					num = hour;
				} else if (ch == 'd') {
					num = day;
				} else if (ch == 'm') {
					num = month;
				} else if (ch == 'y') {
					num = year;
				}
				if (num < 0) {
					num = 0;
				}
				String str = String.valueOf(num);
				if (str.length() > len) {
					str = str.substring(str.length() - len);
				} else if (str.length() < len) {
					for (int j = 0; j < len - str.length(); j++) {
						sb.append('0');
					}
				}
				sb.append(str);
			} else {
				sb.append(scheme.charAt(i++));
			}
		}
		return sb.toString();
	}

	/**
	 * returns SQL representation of the date
	 */
	public String toSQLstring() {
		String r[] = new String[2];
		r[1] = "ddmmyyyy";
		r[0] = StringUtil.padString("" + Math.max(1, day), '0', 2, true) + StringUtil.padString("" + Math.max(1, month), '0', 2, true) + StringUtil.padString("" + Math.max(1, year), '0', 4, true);
		if (this.getPrecisionIdx() >= 3) { // date and time
			r[1] += " hh24";
			r[0] += " " + StringUtil.padString("" + hour, '0', 2, true);
			if (getPrecisionIdx() >= 4) {
				r[1] += "mi";
				r[0] += StringUtil.padString("" + min, '0', 2, true);
			}
			if (getPrecisionIdx() >= 5) {
				r[1] += "ss";
				r[0] += StringUtil.padString("" + sec, '0', 2, true);
			}
		}
		return "to_date(\'" + r[0] + "\',\'" + r[1] + "\')";
	}

	/**
	* Creates a new TimeMoment object equal to this moment.
	*/
	public TimeMoment getCopy() {
		return copyTo(null);
	}

	/**
	* Creates a new TimeMoment object equal to this moment.
	*/
	public Date getCopyWithTimeOnly() {
		Date d = new Date();
		d.scheme = scheme;
		d.precIdx = precIdx;
		d.hour = hour;
		d.min = min;
		d.sec = sec;
		d.setHasElement('h', true);
		d.setHasElement('t', true);
		d.setHasElement('s', true);
		return d;
	}

	/**
	* Sets the given TimeMoment to be equal to this moment. If the time moment t
	* is null, creates this object.
	*/
	public TimeMoment copyTo(TimeMoment t) {
		Date d = null;
		if (t == null || !(t instanceof Date)) {
			d = new Date();
		} else {
			d = (Date) t;
		}
		d.scheme = scheme;
		d.precIdx = precIdx;
		d.year = year;
		d.month = month;
		d.day = day;
		d.hour = hour;
		d.min = min;
		d.sec = sec;
		for (int i = 0; i < hasDateElement.length; i++) {
			d.setHasElement(time_symbols[i], hasDateElement[i]);
		}
		d.numVal = this.numVal;
		d.numValPrecIdx = this.numValPrecIdx;
		d.checkPrecision();
		return d;
	}

	/**
	* Changes its internal variables so that the result is the next time moment
	* (next day, next month, next second etc., depending on the time scale)
	*/
	public void stepForth() {
		if (!isSet())
			return;
		numVal = -1;
		if (useElement('s')) {
			++sec;
			if (sec >= 60 && hasElement('t')) {
				sec = 0;
			} else
				return;
		}
		if (useElement('t')) {
			++min;
			if (min >= 60 && hasElement('h')) {
				min = 0;
			} else
				return;
		}
		if (useElement('h')) {
			++hour;
			if (hour >= 24 && hasElement('d')) {
				hour = 0;
			} else
				return;
		}
		if (useElement('d')) {
			++day;
			if (!hasElement('m') || day <= 28)
				return;
			switch (month) {
			case 1:
			case 3:
			case 5:
			case 7:
			case 8:
			case 10:
			case 12:
				if (day <= 31)
					return;
				break;
			case 2:
				if (hasElement('y'))
					if (nDaysInYear(year) == 366)
						if (day <= 29)
							return;
				break;
			default:
				if (day <= 30)
					return;
			}
			day = 1;
		}
		if (useElement('m')) {
			++month;
			if (month > 12 && hasElement('y')) {
				month = 1;
			} else
				return;
		}
		++year;
	}

	/**
	* Changes its internal variables so that the result is the previous time
	* moment (opposite to stepForth)
	*/
	public void stepBack() {
		if (!isSet())
			return;
		numVal = -1;
		if (useElement('s')) {
			--sec;
			if (sec < 0 && hasElement('t')) {
				sec = 59;
			} else
				return;
		}
		if (useElement('t')) {
			--min;
			if (min < 0 && hasElement('h')) {
				min = 59;
			} else
				return;
		}
		if (useElement('h')) {
			--hour;
			if (hour < 0 && hasElement('d')) {
				hour = 23;
			} else
				return;
		}
		if (useElement('d')) {
			--day;
			if (day > 0 || !hasElement('m'))
				return;
			switch (month - 1) {
			case 1:
			case 3:
			case 5:
			case 7:
			case 8:
			case 10:
			case 0:
				day = 31;
				break;
			case 2:
				day = 28;
				if (hasElement('y'))
					if (nDaysInYear(year) == 366) {
						day = 29;
					}
				break;
			default:
				day = 30;
			}
		}
		if (useElement('m')) {
			--month;
			if (month < 1 && hasElement('y')) {
				month = 12;
			} else
				return;
		}
		--year;
	}

	/**
	* Compares itself with another TimeMoment t. Returns true if the moments are
	* equal.
	*/
	public boolean equals(Object t) {
		if (t == null || !(t instanceof Date))
			return false;
		Date d = (Date) t;
		if (useElement('y') && year != d.year)
			return false;
		if (useElement('m') && month != d.month)
			return false;
		if (useElement('d') && day != d.day)
			return false;
		if (useElement('h') && hour != d.hour)
			return false;
		if (useElement('t') && min != d.min)
			return false;
		if (useElement('s') && sec != d.sec)
			return false;
		return true;
	}

	/**
	* Compares itself with another TimeMoment tm. Returns -1 if this moment is
	* earlier than tm, 0 if they are the same moments, and 1 if this moment is
	* later than tm. 0 may be also returned if the moments are uncompatible,
	* for example, seconds and years.
	*/
	public int compareTo(Comparable t) {
		if (t == null)
			return -1;
		if (!(t instanceof Date))
			return 0;
		Date d = (Date) t;
		if (useElement('y'))
			if (!d.hasElement('y'))
				return 0;
			else if (year < d.year)
				return -1;
			else if (year > d.year)
				return 1;
		if (useElement('m'))
			if (!d.hasElement('m'))
				return 0;
			else if (month < d.month)
				return -1;
			else if (month > d.month)
				return 1;
		if (useElement('d'))
			if (!d.hasElement('d'))
				return 0;
			else if (day < d.day)
				return -1;
			else if (day > d.day)
				return 1;
		if (useElement('h'))
			if (!d.hasElement('h'))
				return 0;
			else if (hour < d.hour)
				return -1;
			else if (hour > d.hour)
				return 1;
		if (useElement('t'))
			if (!d.hasElement('t'))
				return 0;
			else if (min < d.min)
				return -1;
			else if (min > d.min)
				return 1;
		if (useElement('s'))
			if (!d.hasElement('s'))
				return 0;
			else if (sec < d.sec)
				return -1;
			else if (sec > d.sec)
				return 1;
		return 0;
	}

	public static int nDaysInYear(int year) {
		if (year <= 0)
			return 0;
		if (year % 4 == 0 && (year % 100 != 0 || (year / 100) % 4 == 0))
			return 366;
		return 365;
	}

	public static int nOfDayInYear(int day, int month, int year) {
		int n = 0;
		for (int i = 1; i < month; i++) {
			switch (i) {
			case 1:
			case 3:
			case 5:
			case 7:
			case 8:
			case 10:
			case 12:
				n += 31;
				break;
			case 2:
				if (nDaysInYear(year) == 366) {
					n += 29;
				} else {
					n += 28;
				}
				break;
			default:
				n += 30;
			}
		}
		return n + day;
	}

	public static long nOfDay(int day, int month, int year) {
		int y = year - 1;
		if (y < 0) {
			y = 0;
		}
		return 365l * y + y / 4 - y / 100 + y / 400 + nOfDayInYear(day, month, year);
	}

	public static int yearForNOfDay(long nOfDay) {
		int y0 = (int) (nOfDay / 366);
		long nd = nOfDay(1, 1, y0);
		int ndy = nDaysInYear(y0);
		long diff = nOfDay - nd;
		while (diff > ndy) {
			diff -= ndy;
			++y0;
			ndy = nDaysInYear(y0);
		}
		return y0;
	}

	public static int[] ymdForNOfDay(long nOfDay) {
		int y0 = (int) (nOfDay / 366);
		long nd = nOfDay(1, 1, y0);
		int ndy = nDaysInYear(y0);
		while (nOfDay - nd >= ndy) {
			++y0;
			nd = nOfDay(1, 1, y0);
			ndy = nDaysInYear(y0);
		}
		nOfDay -= (nd - 1);
		nd = 1;
		int m0 = 1;
		int ndm = nDaysInMonth(m0, y0);
		while (nOfDay - nd >= ndm) {
			++m0;
			ndm = nDaysInMonth(m0, y0);
			nd = nOfDayInYear(1, m0, y0);
		}
		int ymd[] = { y0, m0, (int) (nOfDay - nd + 1) };
		return ymd;
	}

	public static int nDaysInMonth(int month, int year) {
		switch (month) {
		case 1:
		case 3:
		case 5:
		case 7:
		case 8:
		case 10:
		case 12:
			return 31;
		case 2: {
			if (nDaysInYear(year) == 366)
				return 29;
			return 28;
		}
		}
		return 30;
	}

	/**
	* Finds the difference between this moment and the given moment t (in the
	* units depending on the current precision)
	*/
	public long subtract(TimeMoment t) {
		if (t == null || !(t instanceof Date))
			return 0l;
		Date d = (Date) t;
		int precIdx1 = this.getPrecisionIdx();
		int precIdxH1 = this.getHighestPossiblePrecisionIdx(), precIdxH2 = d.getHighestPossiblePrecisionIdx();
		if (precIdxH1 > precIdxH2) {
			precIdxH1 = precIdxH2;
		}
		long n1 = this.toNumber(precIdxH1);
		long n2 = d.toNumber(precIdxH1);
		long diff = n1 - n2;
		if (diff == 0)
			return diff;
		while (precIdxH1 > precIdx1 && diff != 0) {
			if (precIdxH1 >= 4) {
				diff = Math.round(diff / 60d); //diff in minutes or in hours
			} else if (precIdxH1 == 3) {
				diff = Math.round(diff / 24d); //diff in days
			} else if (precIdxH1 == 2) {//days
				if (precIdx1 == 1) {
					diff = Math.round(diff / 30d); //diff in months
				} else {
					diff = Math.round(diff / 365d); //diff in years, computed from days
					--precIdxH1;
				}
			} else if (precIdxH1 == 1) {
				diff = Math.round(diff / 12d); //diff in years, computed from months
			}
			--precIdxH1;
		}
		return diff;
	}

	/**
	* Finds the difference between this moment and the given moment t in the
	* specified units: 's', 't', 'h', 'd', 'm', or 'y'
	*/
	public long subtract(TimeMoment t, char unit) {
		if (t == null || !(t instanceof Date))
			return 0l;
		Date d = (Date) t;
		int precIdxH1 = this.getHighestPossiblePrecisionIdx(), precIdxH2 = d.getHighestPossiblePrecisionIdx();
		if (precIdxH1 > precIdxH2) {
			precIdxH1 = precIdxH2;
		}
		long n1 = this.toNumber(precIdxH1);
		long n2 = d.toNumber(precIdxH1);
		long diff = n1 - n2;
		if (diff == 0)
			return diff;
		int unitIdx = getTimeUnitIdx(unit);
		while (precIdxH1 > unitIdx && diff != 0) {
			if (precIdxH1 >= 4) {
				diff = Math.round(diff / 60d); //diff in minutes or in hours
			} else if (precIdxH1 == 3) {
				diff = Math.round(diff / 24d); //diff in days
			} else if (precIdxH1 == 2) {//days
				if (unitIdx == 1) {
					diff = Math.round(diff / 30d); //diff in months
				} else {
					diff = Math.round(diff / 365d); //diff in years, computed from days
					--precIdxH1;
				}
			} else if (precIdxH1 == 1) {
				diff = Math.round(diff / 12d); //diff in years, computed from months
			}
			--precIdxH1;
		}
		return diff;
	}

	/**
	* Returns its representation as a number, depending on the current precision
	*/
	public synchronized long toNumber() {
		if (numVal >= 0 && precIdx == numValPrecIdx)
			return numVal;
		if (!isSet())
			return -1L;
		numValPrecIdx = precIdx;
		long val = 0L;
		if (useElement('d')) { //count number of days
			if (hasElement('m'))
				if (hasElement('y')) {
					val = nOfDay(day, month, year);
				} else {
					val = nOfDay(day, month, 0);
				}
			else {
				val = day;
			}
		} else if (useElement('y') || useElement('m')) {
			if (useElement('y')) {
				val = year;
			}
			if (useElement('m')) {
				val = val * 12 + month; //number of months
			}
			numVal = val;
			return val; //number of years
		}

		if (useElement('h')) {
			val = val * 24 + hour;
		}
		if (useElement('t')) {
			val = val * 60 + min;
		}
		if (useElement('s')) {
			val = val * 60 + sec;
		}
		numVal = val;
		return val;
	}

	/**
	* Returns its representation as a number according to the specified precision
	*/
	public synchronized long toNumber(int precIdx) {
		if (numVal >= 0 && precIdx == numValPrecIdx)
			return numVal;
		if (!isSet())
			return -1L;
		numValPrecIdx = precIdx;
		long val = 0L;
		if (precIdx >= 2 && hasDateElement[2]) { //count number of days
			if (hasDateElement[1])
				if (hasDateElement[0]) {
					val = nOfDay(day, month, year);
				} else {
					val = nOfDay(day, month, 0);
				}
			else {
				val = day;
			}
		} else if (precIdx <= 2) {
			if (hasDateElement[0]) {
				val = year;
			}
			if (precIdx > 0 && hasDateElement[1]) {
				val = val * 12 + month; //number of months
			}
			numVal = val;
			return val; //number of years
		}

		if (precIdx >= 3 && hasDateElement[3]) {
			val = val * 24 + hour;
		}
		if (precIdx >= 4 && hasDateElement[4]) {
			val = val * 60 + min;
		}
		if (precIdx >= 5 && hasDateElement[5]) {
			val = val * 60 + sec;
		}
		numVal = val;
		return val;
	}

	/**
	* Converts the given number to a time moment
	*/
	public TimeMoment valueOf(long n) {
		if (n < 0)
			return null;
		Date d = (Date) getCopy();
		d.reset();
		long val = n;
		if (useElement('s')) {
			d.setElementValue('s', (int) (val % 60));
			val /= 60;
		}
		if (useElement('t')) {
			d.setElementValue('t', (int) (val % 60));
			val /= 60;
		}
		if (useElement('h')) {
			d.setElementValue('h', (int) (val % 24));
			val /= 24;
		}
		if (useElement('d')) {
			if (!hasElement('m')) {
				d.day = (int) val;
				return d;
			}
			if (hasElement('y')) {
				int ymd[] = ymdForNOfDay(val);
				d.setElementValue('y', ymd[0]);
				d.setElementValue('m', ymd[1]);
				d.setElementValue('d', ymd[2]);
				return d;
			}
			d.month = -1;
			for (int i = 1; i <= 12 && d.month < 0; i++) {
				int ndays = 30;
				switch (i) {
				case 1:
				case 3:
				case 5:
				case 7:
				case 8:
				case 10:
				case 12:
					ndays = 31;
					break;
				case 2:
					ndays = 28;
					break;
				}
				if (val > ndays) {
					val -= ndays;
				} else {
					d.setElementValue('m', i);
					d.setElementValue('d', (int) val + 1);
				}
			}
			return d;
		}
		if (useElement('m')) {
			if (!hasElement('y')) {
				d.setElementValue('m', (int) val);
				return d;
			}
			d.setElementValue('m', (int) (val % 12));
			val /= 12;
		}
		if (useElement('y')) {
			d.setElementValue('y', (int) val);
		}
		return d;
	}

	/**
	* Changes its internal variables so that the result is the n-th moment
	* starting from the current moment (n may be negative)
	* n is number of units to add. The units depend on the scale and may be
	* days, years, seconds, etc.
	*/
	public void add(long nUnits) {
		if (nUnits == 0)
			return;
		if (nUnits > 0) {
			for (int i = 0; i < nUnits; i++) {
				stepForth();
			}
		} else {
			for (int i = 0; i < -nUnits; i++) {
				stepBack();
			}
		}
	}

	/*
	* Creates a new TimeMoment object with the next moment. The internal settings
	* remain unchanged (cf. stepForth)
	*/
	public TimeMoment getNext() {
		if (!isSet())
			return null;
		Date d = (Date) getCopy();
		d.stepForth();
		return d;
	}

	/*
	* Creates a new TimeMoment object with the previous moment. The internal
	* settings remain unchanged (cf. stepBack)
	*/
	public TimeMoment getPrevious() {
		if (!isSet())
			return null;
		Date d = (Date) getCopy();
		d.stepBack();
		return d;
	}

	/**
	 * Checks the validity of the given date template (scheme). If error, returns
	 * the message explaining the error. If OK, returns null.
	 * @param template - the template to be checked for validity
	 * @return the message explaining the error or null if the template is valid
	 */
	public static String checkTemplateValidity(String template) {
		if (template == null)
			return res.getString("null_template");
		template = template.trim();
		if (template.length() < 1)
			return res.getString("empty_template");
		template = template.toLowerCase();
		String inTemplText = res.getString("in_template") + " \"" + template + "\"";
		int idx = -1;
		for (int i = 0; i < time_symbols.length && idx < 0; i++) {
			idx = template.indexOf(time_symbols[i]);
		}
		if (idx < 0)
			return res.getString("no_time_symbols") + " (y, m, d, h, t, s) " + inTemplText + "!";
		boolean used[] = new boolean[time_symbols.length];
		for (int i = 0; i < time_symbols.length; i++) {
			used[i] = false;
			idx = template.indexOf(time_symbols[i]);
			if (idx < 0) {
				continue;
			}
			used[i] = true;
			char ch = template.charAt(idx);
			do {
				++idx;
			} while (idx < template.length() && template.charAt(idx) == ch);
			--idx;
			if (template.lastIndexOf(ch) > idx)
				return res.getString("Symbols") + " \"" + String.valueOf(ch) + "\" " + res.getString("must_come_in_sequence") + " " + inTemplText + "!";
		}
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
				String err = res.getString("Symbols") + " \"" + time_symbols[i] + "\" " + res.getString("and") + " \"" + time_symbols[next] + "\" " + res.getString("but_no") + " ";
				for (int j = i + 1; j < next; j++) {
					err += "\"" + time_symbols[j] + ((j < next - 1) ? "\", " : "\" ");
				}
				err += res.getString("found") + " " + inTemplText + "!";
				return err;
			}
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
	 * Returns the text for the given time symbol in plural, e.g. "years" for "y" etc.
	 */
	public static String getTextForTimeSymbolInPlural(char symb) {
		switch (symb) {
		case 'y':
		case 'Y':
			return res.getString("years");
		case 'm':
		case 'M':
			return res.getString("months");
		case 'd':
		case 'D':
			return res.getString("days");
		case 'h':
		case 'H':
			return res.getString("hours");
		case 't':
		case 'T':
			return res.getString("minutes");
		case 's':
		case 'S':
			return res.getString("seconds");
		}
		return null;
	}

	/**
	 * Returns the text for the given time unit symbol, e.g. "year" for "y" etc.
	 */
	public String getTextForUnit(char symb) {
		return getTextForTimeSymbol(symb);
	}

	/**
	 * Rounds the date according to the current precision so than the resulting
	 * date is not later than the original date
	 */
	public void roundDown() {
		if (!isSet())
			return;
		char precision = getPrecision();
		if (precision == getMinPrecision())
			return; //no need to round
		numVal = -1;
		if (hasElement('s') && sec > 0) {
			sec = 0;
		}
		if (precision == 't')
			return;
		if (hasElement('t') && min > 0) {
			min = 0;
		}
		if (precision == 'h')
			return;
		if (hasElement('h') && hour > 0) {
			hour = 0;
		}
		if (precision == 'd')
			return;
		if (hasElement('d') && day > 1) {
			day = 1;
		}
		if (precision == 'm')
			return;
		if (hasElement('m') && month > 1) {
			month = 1;
		}
	}

	/**
	 * Rounds the date according to the current precision so than the resulting
	 * date is not earlier than the original date
	 */
	public void roundUp() {
		if (!isSet())
			return;
		char precision = getPrecision();
		if (precision == getMinPrecision())
			return; //no need to round
		char currPecision = precision;
		numVal = -1;
		if (hasElement('s') && sec > 0) {
			sec = 0;
			precision = 't';
			stepForth();
			precision = currPecision;
		}
		if (precision == 't')
			return;
		if (hasElement('t') && min > 0) {
			min = 0;
			precision = 'h';
			stepForth();
			precision = currPecision;
		}
		if (precision == 'h')
			return;
		if (hasElement('h') && hour > 0) {
			hour = 0;
			precision = 'd';
			stepForth();
			precision = currPecision;
		}
		if (precision == 'd')
			return;
		if (hasElement('d') && day > 1) {
			day = 1;
			precision = 'm';
			stepForth();
			precision = currPecision;
		}
		if (precision == 'm')
			return;
		if (hasElement('m') && month > 1) {
			month = 1;
			stepForth();
		}
	}

	/**
	 * Informs if this date is "nice", depending on the current precision
	 */
	public boolean isNice() {
		int val = getElementValue(getPrecision());
		if (precIdx >= 3)
			return val == 0;
		if (precIdx > 0)
			return val == 1;
		return val % 10 == 0;
	}
}
