package spade.vis.spec;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.Vector;

import spade.lib.util.IntArray;
import spade.lib.util.IntRange;
import spade.lib.util.StringUtil;

/**
* This structure describes references to values of a parameter in a table with
* thematic data in a case when there are multiple columns with values of the
* same attribute referring to different parameter values.
*/
public class CaptionParamDescription extends ProtoParam implements TagReader, java.io.Serializable {
	/**
	* The symbols used to specify elements of date/time strings:
	* 's' - seconds, 't' - minutes, 'h' - hours, 'd' - days, 'm' - month,
	* 'y' - years, for example, dd.mm.yyyy.
	*/
	public static final char TIME_SYMBOLS[] = { 's', 't', 'h', 'd', 'm', 'y' };
	/**
	* The name of the parameter
	*/
	public String paramName = null;
	/**
	* The list of values of the parameter
	*/
	public Vector paramValues = null;
	/**
	* If the parameter is temporal, the scheme indicates how it must be interpreted.
	* A scheme may be built of a following elements:
	* 's' - seconds, 't' - minutes, 'h' - hours, 'd' - days, 'm' - month,
	* 'y' - years,
	* for example, dd.mm.yyyy.
	* If the values are simple numbers, e.g., years, it is sufficient to specify
	* just a single character, i.e. "y" rather than "yyyy".
	* The character "a" indicates abstract time counts.
	*/
	public String scheme = null;
	/**
	 * How the date will be shown to the user (may be different from the original scheme)
	 */
	public String shownScheme = null;
	/**
	* The list of the names of the attributes depending on the parameter
	*/
	public Vector attrs = null;
	/**
	* For each of the attributes depending on the parameter, a list
	* (spade.lib.util.IntArray) or range (spade.lib.util.IntRange) of numbers of
	* columns with its values.
	*/
	public Vector colNumbers = null;

	/**
	* Checks if the given symbol is a right time/date symbol, i.e. one of the
	* symbols 's','t','h','d','m','y', or 'a'.
	*/
	public static boolean isTimeSymbol(char symbol) {
		if (symbol == 'a')
			return true;
		for (char element : TIME_SYMBOLS)
			if (symbol == element)
				return true;
		return false;
	}

	/**
	* Reads a description of a parameter. A description must be a sequence of
	* lines starting with <CaptionParameter> and ending with </CaptionParameter>.
	* The first argument is the first string of the tag (i.e. the string starting
	* with "<CaptionParameter"), the second argument is the BufferedReader used
	* to read the description. Returns true if the description has been
	* successfully read, or at least the end line has been encountered.
	*/
	@Override
	public boolean readDescription(String str, BufferedReader br) throws IOException {
		if (str == null || br == null || !str.startsWith("<") || !str.substring(1).toLowerCase().startsWith("captionparameter"))
			return false;
		boolean endFound = false;
		while (true) {
			str = br.readLine();
			if (str == null) {
				break;
			}
			str = str.trim();
			if (str.length() < 1) {
				continue;
			}
			if (str.startsWith("*")) {
				continue; //this is a comment
			}
			if (str.equalsIgnoreCase("</captionparameter>")) {
				endFound = true;
				break;
			}
			StringTokenizer st = new StringTokenizer(str, "=");
			if (st.countTokens() < 2) {
				continue;
			}
			String w1 = StringUtil.removeQuotes(st.nextToken().trim()), w2 = st.nextToken().trim();
			if (!w1.equalsIgnoreCase("values") && !w1.equalsIgnoreCase("order")) {
				w2 = StringUtil.removeQuotes(w2);
			}
			if (w1 == null || w2 == null || w1.length() < 1 || w2.length() < 1) {
				continue;
			}
			if (w1.equalsIgnoreCase("param_name")) {
				paramName = w2;
			} else if (w1.equalsIgnoreCase("time_scheme")) {
				scheme = w2;
			} else if (w1.equalsIgnoreCase("shown_scheme")) {
				shownScheme = w2;
			} else if (w1.equalsIgnoreCase("is_temporal")) {
				isTemporal = w2.equalsIgnoreCase("yes");
			} else if (w1.equalsIgnoreCase("values")) {
				paramValues = StringUtil.getNames(w2, ";,", false);
			} else if (w1.equalsIgnoreCase("order")) {
				if (w2.equalsIgnoreCase("SORTED")) {
					ordered = true;
				} else {
					order = StringUtil.getNames(w2, ";,", false);
					ordered = order != null && order.size() > 1;
					if (!ordered) {
						order = null;
					}
				}
			} else { //w1 is the attribute name, w2 specifies its column range
				Vector v = StringUtil.getNames(w2, ";,", true);
				if (v != null && v.size() > 0) {
					IntArray numbers = new IntArray(v.size(), 1);
					for (int i = 0; i < v.size(); i++) {
						String val = (String) v.elementAt(i);
						if (val.equalsIgnoreCase("LAST")) {
							numbers.addElement(-100);
						} else {
							try {
								int k = Integer.valueOf(val).intValue() - 1;
								numbers.addElement(k);
							} catch (NumberFormatException nfe) {
								break;
							}
						}
					}
					if (numbers.size() > 1) {
						if (attrs == null) {
							attrs = new Vector(20, 10);
						}
						attrs.addElement(w1);
						if (colNumbers == null) {
							colNumbers = new Vector(20, 10);
						}
						if (numbers.size() == 2 && (numbers.elementAt(1) == -100 || w2.indexOf(';') > 0)) {
							IntRange range = new IntRange();
							range.from = numbers.elementAt(0);
							range.to = numbers.elementAt(1);
							colNumbers.addElement(range);
						} else {
							colNumbers.addElement(numbers);
						}
					}
				}
			}
		}
		if (!endFound)
			return false;
		return true;
	}

	/**
	* Stores the description of a parameter to a file as a sequence of
	* lines starting with <CaptionParameter> and ending with </CaptionParameter>.
	*/
	@Override
	public void writeDescription(DataOutputStream writer) throws IOException {
		if (writer == null)
			return;
		writer.writeBytes("<CaptionParameter>\n");
		if (paramName != null) {
			writer.writeBytes("param_name=\"" + paramName + "\"\n");
		}
		if (paramValues != null) {
			writer.writeBytes("values=");
			for (int i = 0; i < paramValues.size(); i++) {
				if (i > 0) {
					writer.writeBytes(";");
				}
				writer.writeBytes("\"" + (String) paramValues.elementAt(i) + "\"");
			}
			writer.writeBytes("\n");
		}
		if (isTemporal) {
			writer.writeBytes("is_temporal=yes\n");
			if (scheme != null) {
				writer.writeBytes("time_scheme=\"" + scheme + "\"\n");
			}
			if (shownScheme != null) {
				writer.writeBytes("shown_scheme=\"" + shownScheme + "\"\n");
			}
		}
		if (ordered)
			if (order == null || order.size() < 2) {
				writer.writeBytes("order=SORTED\n");
			} else {
				writer.writeBytes("order=");
				for (int i = 0; i < order.size(); i++) {
					if (i > 0) {
						writer.writeBytes(";");
					}
					writer.writeBytes("\"" + (String) order.elementAt(i) + "\"");
				}
				writer.writeBytes("\n");
			}
		if (attrs != null && attrs.size() > 0) {
			for (int i = 0; i < attrs.size(); i++) {
				writer.writeBytes("\"" + (String) attrs.elementAt(i) + "\"=");
				if (colNumbers.elementAt(i) instanceof IntArray) {
					IntArray numbers = (IntArray) colNumbers.elementAt(i);
					for (int j = 0; j < numbers.size(); j++) {
						writer.writeBytes(((j == 0) ? "" : ",") + (numbers.elementAt(j) + 1));
					}
				} else if (colNumbers.elementAt(i) instanceof IntRange) {
					IntRange range = (IntRange) colNumbers.elementAt(i);
					if (range.to >= 0) {
						writer.writeBytes((range.from + 1) + ";" + (range.to + 1));
					} else {
						writer.writeBytes((range.from + 1) + ";LAST");
					}
				}
				writer.writeBytes("\n");
			}
		}
		writer.writeBytes("</CaptionParameter>\n");
	}
}