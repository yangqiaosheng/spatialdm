package spade.lib.util;

import java.util.Calendar;
import java.util.StringTokenizer;
import java.util.Vector;

public class StringUtil {
	/**
	* Checks if the given vector contains the given string. In comparison of
	* strings the case is ignored. Correctly processes the situation of null
	* string or/and null vector.
	*/
	public static boolean isStringInVectorIgnoreCase(String str, Vector v) {
		if (v == null || v.size() < 1 || str == null)
			return false;
		for (int i = 0; i < v.size(); i++)
			if (v.elementAt(i) != null && v.elementAt(i) instanceof String)
				if (str.equalsIgnoreCase((String) v.elementAt(i)))
					return true;
		return false;
	}

	/**
	* Returns the index of the given string in the given vector. In comparison of
	* strings the case is ignored. Correctly processes the situation of null
	* string or/and null vector.
	*/
	public static int indexOfStringInVectorIgnoreCase(String str, Vector v) {
		if (v == null || v.size() < 1 || str == null)
			return -1;
		for (int i = 0; i < v.size(); i++)
			if (v.elementAt(i) != null && v.elementAt(i) instanceof String)
				if (str.equalsIgnoreCase((String) v.elementAt(i)))
					return i;
		return -1;
	}

	/**
	* Checks whether all the strings of the first vector occur in the second one.
	* Ignores nulls.
	*/
	public static boolean isSubsetOf(Vector subset, Vector superset) {
		if (subset == null || superset == null)
			return false;
		if (subset.size() < 1 || superset.size() < 1)
			return false;
		for (int i = 0; i < subset.size(); i++)
			if (subset.elementAt(i) != null && subset.elementAt(i) instanceof String)
				if (!isStringInVectorIgnoreCase((String) subset.elementAt(i), superset))
					return false;
		return true;
	}

	/**
	* Checks whether all the strings of the first vector occur in the second one,
	* starting from the given index. Ignores nulls.
	*/
	public static boolean isSubsetOf(Vector subset, Vector superset, int startIdx) {
		if (subset == null || superset == null)
			return false;
		if (subset.size() < 1 || superset.size() <= startIdx)
			return false;
		for (int i = 0; i < subset.size(); i++)
			if (subset.elementAt(i) != null) {
				String str = (String) subset.elementAt(i);
				boolean found = false;
				for (int j = startIdx; j < superset.size() && !found; j++)
					if (superset.elementAt(j) != null && superset.elementAt(i) instanceof String) {
						found = str.equalsIgnoreCase((String) superset.elementAt(j));
					}
				if (!found)
					return false;
			}
		return true;
	}

	/**
	* Compares two strings. When one of the strings is null,
	* returns true when the other is also null.
	*/
	public static boolean sameStrings(String str1, String str2) {
		if (str1 == null)
			return str2 == null;
		if (str2 == null)
			return false;
		return str1.equals(str2);
	}

	/**
	* Compares two strings ignoring the case. When one of the strings is null,
	* returns true when the other is also null.
	*/
	public static boolean sameStringsIgnoreCase(String str1, String str2) {
		if (str1 == null)
			return str2 == null;
		if (str2 == null)
			return false;
		return str1.equalsIgnoreCase(str2);
	}

	/**
	* Alphabetically compares two strings. Returns true if the first string
	* is larger, i.e. should come after the second string
	*/
	public static boolean Larger(String st1, String st2) {
		int maxl = (st1.length() > st2.length()) ? st1.length() : st2.length();
		for (int i = 0; i < maxl; i++) {
			if (i > st1.length() - 1)
				return false;
			if (i > st2.length() - 1)
				return true;
			if (st1.charAt(i) == st2.charAt(i)) {
				continue;
			}
			return st1.charAt(i) > st2.charAt(i);
		}
		return false;
	}

	/**
	 * Compares two strings: first checks if they represent integer numbers and, if so,
	 * compares them as numbers, otherwise uses String.compareTo(String)
	 */
	public static int compareStrings(String st1, String st2) {
		if (st1 == null)
			if (st2 == null)
				return 0;
			else
				return 1;
		if (st2 == null)
			return -1;
		try {
			int n1 = Integer.parseInt(st1);
			try {
				int n2 = Integer.parseInt(st2);
				if (n1 < n2)
					return -1;
				if (n1 > n2)
					return 1;
			} catch (NumberFormatException nfe2) {
			}
		} catch (NumberFormatException nfe1) {
		}
		return st1.compareTo(st2);
	}

	/**
	* If a string contains some text in quotes, this function removes the
	* quotes. If not, returns the original string.
	*/
	public static String removeQuotes(String str) {
		if (str == null)
			return null;
		str = str.trim();
		if (str.charAt(0) != '\"')
			return str;
		int idx = str.indexOf('\"', 1);
		if (idx < 0) {
			idx = str.length();
		}
		return str.substring(1, idx);
	}

	/**
	* Reads a name enclosed in quotes from a string. The difference from the
	* removeQuotes(String) is that tha name is not necessarily starts at
	* the beginning of the string.
	*/
	public static String readName(String p) {
		int bg = p.indexOf("\"");
		if (bg >= 0) {
			int end = p.indexOf("\"", bg + 1);
			if (end > bg)
				return p.substring(bg + 1, end);
		}
		return p;
	}

	/**
	* Reads names from a string where they are delimited with commas or semicolons.
	* The names may be enclosed in quotes
	*/
	public static Vector getNames(String nameStr) {
		if (nameStr == null)
			return null;
		nameStr = nameStr.trim();
		if (nameStr.length() < 1)
			return null;
		Vector names = null;
		if (nameStr.startsWith("\"")) {
			int i1 = 0;
			while (i1 >= 0 && i1 < nameStr.length()) {
				int i2 = nameStr.indexOf('\"', i1 + 1);
				if (i2 < 0) {
					i2 = nameStr.length();
				}
				if (i2 > i1 + 1) {
					String s = nameStr.substring(i1 + 1, i2).trim();
					if (s.length() > 0) {
						if (names == null) {
							names = new Vector(10, 10);
						}
						names.addElement(s);
					}
				}
				if (i2 < nameStr.length() - 1) {
					i1 = nameStr.indexOf('\"', i2 + 1);
				} else {
					i1 = -1;
				}
			}
		} else {
			StringTokenizer st = new StringTokenizer(nameStr, ",\r\n");
			if (!st.hasMoreTokens())
				return null;
			names = new Vector(st.countTokens(), 5);
			while (st.hasMoreTokens()) {
				names.addElement(st.nextToken());
			}
		}
		return names;
	}

	/**
	* Reads names from a string where they are delimited with the specified
	* delimiter. Some or all names may be enclosed in quotes. If so, the quotes
	* are removed. The last argument indicates what to do if several delimiters
	* occur in sequence: just skip them or add empty names to the resulting list.
	*/
	public static Vector getNames(String nameStr, String delimiter, boolean retrieveEmptyNames) {
		if (nameStr == null)
			return null;
		nameStr = nameStr.trim();
		if (nameStr.length() < 1)
			return null;
		Vector names = new Vector(20, 10);
		;
		while (nameStr != null && nameStr.length() > 0) {
			if (delimiter != null && delimiter.length() > 0) {
				int i = 0;
				while (delimiter.indexOf(nameStr.charAt(i)) >= 0) {
					++i;
					if (retrieveEmptyNames) {
						names.addElement("");
					}
				}
				if (i > 0) {
					nameStr = nameStr.substring(i);
				}
				nameStr = nameStr.trim();
				if (nameStr.length() < 1) {
					break;
				}
			}
			if (nameStr.startsWith("\"")) {
				int i2 = nameStr.indexOf('\"', 1);
				if (i2 < 0) {
					i2 = nameStr.length();
				}
				String s = nameStr.substring(1, i2).trim();
				names.addElement(s);
				++i2; //must skip the next delimiter after the quote
				while (i2 < nameStr.length() && delimiter.indexOf(nameStr.charAt(i2)) < 0) {
					++i2;
				}
				if (i2 >= nameStr.length()) {
					nameStr = null;
				} else {
					nameStr = nameStr.substring(i2 + 1);
				}
			} else {
				if (delimiter == null || delimiter.length() < 1) {
					names.addElement(nameStr);
					return names;
				}
				int i2 = 0;
				while (i2 < nameStr.length() && delimiter.indexOf(nameStr.charAt(i2)) < 0) {
					++i2;
				}
				if (i2 > 0) {
					names.addElement(nameStr.substring(0, i2).trim());
				} else {
					names.addElement("");
				}
				if (i2 >= nameStr.length()) {
					nameStr = null;
				} else {
					nameStr = nameStr.substring(i2 + 1);
				}
			}
		}
		if (names.size() < 1)
			return null;
		return names;
	}

	/**
	* Reads names from a string where they are delimited with the specified
	* delimiter. Some or all names may be enclosed in quotes. If so, the quotes
	* are removed. If several delimiters occur in sequence, they are just skipped.
	*/
	public static Vector getNames(String nameStr, String delimiter) {
		return getNames(nameStr, delimiter, false);
	}

	/**
	 * Adds the specified character to the string until the specified length is
	 * achieved. If the string is longer, returns it without changes.
	 */
	public static String padString(String str, char ch, int len, boolean left) {
		if (str == null) {
			str = "";
		}
		int k = len - str.length();
		if (k <= 0)
			return str;
		StringBuffer sb = new StringBuffer(k);
		for (int i = 0; i < k; i++) {
			sb.append(ch);
		}
		if (left)
			return sb.toString() + str;
		return str + sb.toString();
	}

	/**
	 * Returns a string representation of the given date (only date; no time and
	 * no day of the week)
	 */
	public static String dateToString(Calendar date, String separator) {
		if (date == null)
			return null;
		return padString(String.valueOf(date.get(Calendar.DAY_OF_MONTH)), '0', 2, true) + separator + padString(String.valueOf(date.get(Calendar.MONTH) + 1), '0', 2, true) + separator
				+ padString(String.valueOf(date.get(Calendar.YEAR)), '0', 4, true);
	}

	/**
	 * Returns a string representation of the time of the day specified in the
	 * given date
	 */
	public static String timeToString(Calendar date, String separator, boolean includeSeconds) {
		if (date == null)
			return null;
		String str = padString(String.valueOf(date.get(Calendar.HOUR_OF_DAY)), '0', 2, true) + separator + padString(String.valueOf(date.get(Calendar.MINUTE)), '0', 2, true);
		if (includeSeconds) {
			str += separator + padString(String.valueOf(date.get(Calendar.SECOND)), '0', 2, true);
		}
		return str;
	}

	/**
	 * Returns a string representation of the time of the day specified in the
	 * given date, including seconds
	 */
	public static String timeToString(Calendar date, String separator) {
		return timeToString(date, separator, true);
	}

	/**
	 * Returns a string representation of the given date and time, including seconds
	 */
	public static String dateTimeToString(Calendar date) {
		if (date == null)
			return null;
		return dateToString(date, ".") + " " + timeToString(date, ":", true);
	}

	/**
	* Returns a string representation of the given float number with the given
	* precision
	*/
	public static String floatToStr(float fl, int prec) {
		if (fl >= Long.MAX_VALUE || fl <= Long.MIN_VALUE)
			return String.valueOf(fl);
		long factor = 1;
		for (int i = 0; i < prec; i++) {
			factor *= 10;
		}
		boolean negative = fl < 0;
		if (negative) {
			fl = -fl;
		}
		long v = Math.round(fl - 0.5f); //integer part
		long r = Math.round((fl - v) * factor);
		if (r >= factor) {
			r -= factor;
			++v;
		}
		String s = "";
		if (r > 0) {
			s = String.valueOf(r + factor);
			s = "." + s.substring(1);
		}
		if (prec > 0) {
			if (s.length() < 1) {
				s = ".";
			}
			while (s.length() - 1 < prec) {
				s = s + "0";
			}
		}
		if (negative)
			return "-" + String.valueOf(v) + s;
		return String.valueOf(v) + s;
	}

	public static String floatToStr(float fl) {
		return floatToStr(fl, getPreferredPrecision(fl, fl / 10, fl * 10));
	}

	/**
	* Returns a string representation of the given float number with the given
	* precision so that the represented number is guaranteed to be not less
	* (if "more" is true) or not more (if "more" is false) than the original
	* number.
	*/
	public static String floatToStr(float fl, int prec, boolean more) {
		if (fl >= Long.MAX_VALUE || fl <= Long.MIN_VALUE)
			return String.valueOf(fl);
		long factor = 1;
		for (int i = 0; i < prec; i++) {
			factor *= 10;
		}
		boolean negative = fl < 0;
		if (negative) {
			fl = -fl;
		}
		long v = Math.round(fl - 0.5f); //integer part
		long r = (more != negative) ? Math.round(Math.ceil((fl - v) * factor)) : Math.round(Math.floor((fl - v) * factor));
		if (r >= factor) {
			r -= factor;
			++v;
		}
		String s = "";
		if (r > 0) {
			s = String.valueOf(r + factor);
			s = "." + s.substring(1);
		}
		if (prec > 0) {
			if (s.length() < 1) {
				s = ".";
			}
			while (s.length() - 1 < prec) {
				s = s + "0";
			}
		}
		if (negative)
			return "-" + String.valueOf(v) + s;
		return String.valueOf(v) + s;
	}

	/**
	* Heuristically calculates a suitable precision dependig on minimum
	* and maximum values
	*/
	public static int getPreferredPrecision(float fl, float min, float max) {
		if (Float.isNaN(fl))
			return 0;
		if (Float.isNaN(min)) {
			min = fl;
		}
		if (Float.isNaN(max)) {
			max = fl;
		}
		float dif = (max > min) ? max - min : max;
		int prec = 0;
		while (dif > 0 && dif < 1000.0f) {
			dif *= 10;
			++prec;
		}
		return prec;
	}

	/**
	* Transforms the float number to a string with heuristic selection of a
	* suitable precision dependig on minimum and maximum values
	*/
	public static String floatToStr(float fl, float min, float max) {
		if (Float.isNaN(fl))
			return String.valueOf(fl);
		return floatToStr(fl, getPreferredPrecision(fl, min, max));
	}

	/**
	* Transforms the float number to a string with heuristic selection of a
	* suitable precision dependig on minimum and maximum values. The value
	* is transformed so that the represented number is guaranteed to be not less
	* (if "more" is true) or not more (if "more" is false) than the original
	* number.
	*/
	public static String floatToStr(float fl, float min, float max, boolean more) {
		if (Float.isNaN(fl))
			return String.valueOf(fl);
		return floatToStr(fl, getPreferredPrecision(fl, min, max), more);
	}

	/**
	* Heuristically calculates a suitable precision dependig on minimum
	* and maximum values
	*/
	public static int getPreferredPrecision(double fl, double min, double max) {
		if (Double.isNaN(fl))
			return 0;
		if (Double.isNaN(min)) {
			min = fl;
		}
		if (Double.isNaN(max)) {
			max = fl;
		}
		double dif = (max > min) ? max - min : max;
		int prec = 0;
		while (dif > 0 && dif < 1000.0f) {
			dif *= 10;
			++prec;
		}
		return prec;
	}

	/**
	* Returns a string representation of the given double number with the given
	* precision
	*/
	public static String doubleToStr(double fl, int prec) {
		if (fl >= Long.MAX_VALUE || fl <= Long.MIN_VALUE)
			return String.valueOf(fl);
		long factor = 1;
		for (int i = 0; i < prec; i++) {
			factor *= 10;
		}
		boolean negative = fl < 0;
		if (negative) {
			fl = -fl;
		}
		long v = Math.round(fl - 0.5f); //integer part
		long r = Math.round((fl - v) * factor);
		if (r >= factor) {
			r -= factor;
			++v;
		}
		String s = "";
		if (r > 0) {
			s = String.valueOf(r + factor);
			s = "." + s.substring(1);
		}
		if (prec > 0) {
			if (s.length() < 1) {
				s = ".";
			}
			while (s.length() - 1 < prec) {
				s = s + "0";
			}
		}
		if (negative)
			return "-" + String.valueOf(v) + s;
		return String.valueOf(v) + s;
	}

	public static String doubleToStr(double fl) {
		return doubleToStr(fl, getPreferredPrecision(fl, fl / 10, fl * 10));
	}

	/**
	* Transforms the double number to a string with heuristic selection of a
	* suitable precision dependig on minimum and maximum values
	*/
	public static String doubleToStr(double fl, double min, double max) {
		if (Double.isNaN(fl))
			return String.valueOf(fl);
		return doubleToStr(fl, getPreferredPrecision(fl, min, max));
	}

	/**
	* Transforms the double number to a string with heuristic selection of a
	* suitable precision dependig on minimum and maximum values. The value
	* is transformed so that the represented number is guaranteed to be not less
	* (if "more" is true) or not more (if "more" is false) than the original
	* number.
	*/
	public static String doubleToStr(double fl, double min, double max, boolean more) {
		if (Double.isNaN(fl))
			return String.valueOf(fl);
		return doubleToStr(fl, getPreferredPrecision(fl, min, max), more);
	}

	/**
	* Returns a string representation of the given double number with the given
	* precision so that the represented number is guaranteed to be not less
	* (if "more" is true) or not more (if "more" is false) than the original
	* number.
	*/
	public static String doubleToStr(double fl, int prec, boolean more) {
		if (fl >= Long.MAX_VALUE || fl <= Long.MIN_VALUE)
			return String.valueOf(fl);
		long factor = 1;
		for (int i = 0; i < prec; i++) {
			factor *= 10;
		}
		boolean negative = fl < 0;
		if (negative) {
			fl = -fl;
		}
		long v = Math.round(fl - 0.5f); //integer part
		long r = (more != negative) ? Math.round(Math.ceil((fl - v) * factor)) : Math.round(Math.floor((fl - v) * factor));
		if (r >= factor) {
			r -= factor;
			++v;
		}
		String s = "";
		if (r > 0) {
			s = String.valueOf(r + factor);
			s = "." + s.substring(1);
		}
		if (prec > 0) {
			if (s.length() < 1) {
				s = ".";
			}
			while (s.length() - 1 < prec) {
				s = s + "0";
			}
		}
		if (negative)
			return "-" + String.valueOf(v) + s;
		return String.valueOf(v) + s;
	}

	/**
	* Replaces all occurrences of the substring oldS in the string txt by the
	* substring newS
	*/
	public static String replace(String txt, String oldS, String newS) {
		if (txt == null || oldS == null || oldS.equals(newS))
			return txt;
		if (txt.indexOf(oldS) < 0)
			return txt;
		int i = 0;
		String result = "";
		while (i < txt.length()) {
			int j = txt.indexOf(oldS, i);
			if (j < 0) {
				j = txt.length();
			}
			if (j > i) {
				result += txt.substring(i, j);
			}
			if (j < txt.length()) {
				result += newS;
				i = j + oldS.length();
			} else {
				i = j;
			}
		}
		return result;
	}

	/**
	* Counts occurrences of the given character in the given string
	*/
	public static int countOccurrences(char chr, String str) {
		if (str == null)
			return 0;
		int k = 0;
		for (int i = 0; i < str.length(); i++)
			if (str.charAt(i) == chr) {
				++k;
			}
		return k;
	}

	/**
	* Decodes a string received over the Web and, possibly, containing codes
	* like %E4 instead of national characters (e.g. with umlauts).
	*/
	public static String decodeWebString(String sourceStr) {
		if (sourceStr == null)
			return null;
		int idx = sourceStr.indexOf('%');
		while (idx >= 0) {
			String code = sourceStr.substring(idx + 1, idx + 3);
			try {
				int val = Integer.parseInt(code, 16);
				if (val > 0 && val <= 255) {
					sourceStr = sourceStr.substring(0, idx) + (char) val + sourceStr.substring(idx + 3);
				}
			} catch (NumberFormatException nfe) {
			}
			idx = sourceStr.indexOf('%', idx + 1);
		}
		return sourceStr;
	}

//ID
	/**
	* Replaces commas with semicolons
	* Useful for CSV export
	*/
	public static String eliminateCommas(String s) {
		if (s == null || s == "")
			return s;
		String s1 = "";
		for (int i = 0; i < s.length(); i++) {
			char ch = s.charAt(i);
			s1 += (ch == ',') ? ';' : ch;
		}
		return s1;
	}

	/**
	 * limits the length of the string and replaces bad characters with underscores
	 */
	public static String SQLid(String s, int length) {
		String ss = "";
		for (int i = 0; i < s.length(); i++) {
			ss += Character.isJavaIdentifierPart(s.charAt(i)) ? s.charAt(i) : '_';
		}
		if (ss.charAt(0) == '_') {
			ss = "x" + ss.substring(1);
		}
		try {
			new Integer(ss);
			ss = "x" + ss;
		} catch (Exception ex) {
		}
		return ss.substring(0, Math.min(length, ss.length()));

// more restrictive rules:
		/*
		      ss+= Character.isLetterOrDigit(s.charAt(i)) ? s.charAt(i) : '_';
		    return ss.toUpperCase().substring(0,Math.min(8,ss.length()));
		 */
	}

	/**
	 * returns the same string, replacing last characters by next number
	 */
	public static String modifyId(String s, boolean canGrow) {
		if (s == null)
			return s;
		if (s.length() < 2)
			return s + "_1";
		if (s.charAt(s.length() - 2) != '_')
			return (canGrow ? s : s.substring(0, s.length() - 2)) + "_1";
		else
			// already with number
			return s.substring(0, s.length() - 1) + (char) (s.charAt(s.length() - 1) + 1);
	}

	/**
	 * returns the same string, replacing last characters by next number
	 */
	public static String modifyId(String s, int maxlen) {
		if (maxlen <= 0)
			return modifyId(s, true);
		if (s.length() <= maxlen - 2)
			return modifyId(s, true);
		else
			return modifyId(s, false);
	}

//~ID

	/** Method cuts given string down to a certain pixel-width <= a specified int-value
	 * Uses getFontMetrics.stringWidth of the given Graphics-Object to determine space usage.
	 * Returns an empty string if g==null.
	 *
	 * @param str the string to be cut
	 * @param width the limitation of the results string's width
	 * @param g the Graphics-Object the String is to be cut for
	 * @return as many of the leading characters as fit the specified width
	 */
	public static String getCutString(String str, int width, java.awt.Graphics g) {
		if (str == null || g == null)
			return "";
		char[] chars = str.toCharArray();
		String res = "";
		int i = 0;
		while ((i < chars.length) && (g.getFontMetrics().stringWidth(res + chars[i]) <= width)) {
			res = res + chars[i];
			i++;
		}
		return res;
	}

//ID
	public static String makeID(String str) {
		return SQLid(str, str.length());
	}

//~ID

	public static final String delimiters = " ,;.!?-:/\\&%+=*#'\"()[]{}_\r\n";

	/**
	 * Checks if the given term occurs in the given text.
	 * The term may consist of two or more words.
	 * In the latter case, the method checks if all words occur in the string
	 * in the given order. Depending on the value of the third argument,
	 * may ignore occurrences of other words between the words of the term
	 */
	public static boolean termOccursInText(String term, String text, boolean ignoreWordsBetween) {
		if (term == null || text == null || term.length() < 1 || text.length() < 1)
			return false;
		if (text.indexOf(term) >= 0)
			return true;
		StringTokenizer st = new StringTokenizer(term, delimiters);
		int nWords = st.countTokens();
		if (nWords < 1)
			return false;
		if (nWords == 1)
			return text.indexOf(st.nextToken()) >= 0;
		String words[] = new String[nWords];
		int idxs[] = new int[nWords];
		for (int i = 0; i < nWords; i++) {
			words[i] = st.nextToken();
			idxs[i] = -1;
		}
		int start = 0;
		while (start < text.length() - 1 && idxs[nWords - 1] < 0) {
			for (int i = 0; i < nWords; i++) {
				idxs[i] = text.indexOf(words[i], start);
				if (idxs[i] < 0)
					if (i == 0)
						return false;
					else {
						break;
					}
				start = idxs[i] + words[i].length();
				if (i > 0 && !ignoreWordsBetween) {
					//check if only deliniters are betweent the occurrences
					boolean onlyDelim = true;
					for (int j = idxs[i - 1] + words[i - 1].length(); j < idxs[i] && onlyDelim; j++) {
						onlyDelim = delimiters.indexOf(text.charAt(j)) >= 0;
					}
					if (!onlyDelim) {
						idxs[i] = -1;
						break;
					}
				}
			}
		}
		return idxs[nWords - 1] >= 0;
	}
}
