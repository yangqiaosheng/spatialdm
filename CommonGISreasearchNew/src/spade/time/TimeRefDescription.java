package spade.time;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.Vector;

import spade.lib.util.StringUtil;
import spade.vis.spec.ParamSpec;
import spade.vis.spec.TagReader;

/**
* This structure describes time references in a table with thematic data.
* Time references may be contained in a single column or in a combination of
* columns, e.g., year, month, and day in separate columns. When time references
* are in a single column, they may be either abstract time counts specified as
* integer numbers or complex strings built according to a certain scheme, e.g.,
* dd.mm.yyyy.
*/
public class TimeRefDescription implements TagReader, ParamSpec, java.io.Serializable {
	/**
	* The symbols used to specify elements of date/time strings:
	* 's' - seconds, 't' - minutes, 'h' - hours, 'd' - days, 'm' - month,
	* 'y' - years, for example, dd.mm.yyyy.
	*/
	public static final char TIME_SYMBOLS[] = { 's', 't', 'h', 'd', 'm', 'y' };
	/**
	* Possible meanings of time references
	*/
	public static final int OCCURRED_AT = 0, VALID_FROM = 1, VALID_UNTIL = 2;
	/**
	* The text constants corresponding to the possible meanings of time references
	*/
	public static final String MEANINGS[] = { "OCCURRED_AT", "VALID_FROM", "VALID_UNTIL" };
	/**
	* The meaning of the time reference: one of the constants OCCURRED_AT,
	* VALID_FROM, or VALID_UNTIL. By default, the meaning is OCCURRED_AT.
	*/
	public int meaning = OCCURRED_AT;
	/**
	* The source column(s) of the table containing the time reference
	*/
	public String sourceColumns[] = null;
	/**
	* The schemes describing the content of each source column. A scheme may be
	* built of a following elements:
	* 's' - seconds, 't' - minutes, 'h' - hours, 'd' - days, 'm' - month,
	* 'y' - years,
	* for example, dd.mm.yyyy.
	* If a column contains only numbers, e.g., years, it is sufficient to specify
	* just a single character, i.e. "y" rather than "yyyy".
	* The character "a" indicates a column containing abstract time counts.
	*/
	public String schemes[] = null;
	/**
	* If time references are specified using a combination of columns (e.g.
	* separate columns with days, months, and years), a new attribute containing
	* the full references must be built from them. This variable specifies the
	* name of this attribute.
	*/
	public String attrName = null;
	/**
	* May specify the scheme of the resulting attribute, i.e. how the dates must
	* be shown to users.
	*/
	public String attrScheme = null;
	/**
	* Indicates whether this time reference is a parameter
	*/
	public boolean isParameter = false;
	/**
	 * For a temporal parameter, indicates whether missing values of parameter-
	 * dependent attributes should be filled with the last known values
	 */
	public boolean protractKnownValues = false;
	/**
	 * Indicates whether the original table columns (treated as strings) must
	 * be preserved. If the time references are parameters, the original
	 * columns are always removed.
	 */
	public boolean keepOrigColumns = false;
	/**
	* If the time reference is a parameter, this variable contains the list of
	* names of columns depending on this parameter. If the list is
	* empty, but isParameter is true, this means that all columns in the table
	* depend on the temporal parameter.
	*/
	public Vector dependentCols = null;
	/**
	* Indicates whether the specified temporal attribute has been already constructed
	*/
	public boolean attrBuilt = false;

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
	* Reads a description of a time reference. A description must be a sequence of
	* lines starting with <TimeReference> and ending with </TimeReference>. The
	* first argument is the first string of the tag (i.e. the string starting with
	* "<TimeReference"), the second argument is the BufferedReader used to read the
	* description. Returns true if the description has been successfully read,
	* or at least the end line has been encountered.
	*/
	@Override
	public boolean readDescription(String str, BufferedReader br) throws IOException {
		if (str == null || br == null || !str.startsWith("<") || !str.substring(1).toLowerCase().startsWith("timereference"))
			return false;
		boolean endFound = false;
		Vector pairs = new Vector(10, 10);
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
			if (str.equalsIgnoreCase("</timereference>")) {
				endFound = true;
				break;
			}
			StringTokenizer st = new StringTokenizer(str, "=");
			if (st.countTokens() < 2) {
				continue;
			}
			String w1 = StringUtil.removeQuotes(st.nextToken().trim()), w2 = st.nextToken().trim();
			if (!w1.equalsIgnoreCase("dependent_columns")) {
				w2 = StringUtil.removeQuotes(w2);
			}
			if (w1 == null || w2 == null || w1.length() < 1 || w2.length() < 1) {
				continue;
			}
			if (w1.equalsIgnoreCase("attr_name")) {
				attrName = w2;
			} else if (w1.equalsIgnoreCase("attr_scheme")) {
				attrScheme = w2;
			} else if (w1.equalsIgnoreCase("meaning")) {
				for (int i = 0; i < MEANINGS.length; i++)
					if (w2.equalsIgnoreCase(MEANINGS[i])) {
						meaning = i;
					}
			} else if (w1.equalsIgnoreCase("is_parameter")) {
				isParameter = w2.equalsIgnoreCase("yes");
			} else if (w1.equalsIgnoreCase("protract_known_values")) {
				protractKnownValues = w2.equalsIgnoreCase("yes");
			} else if (w1.equalsIgnoreCase("keep_original_columns")) {
				keepOrigColumns = w2.equalsIgnoreCase("yes");
			} else if (w1.equalsIgnoreCase("dependent_columns")) {
				dependentCols = StringUtil.getNames(w2, ";,", false);
			} else {
				pairs.addElement(w1);
				pairs.addElement(w2);
			}
		}
		if (!endFound)
			return false;
		if (pairs.size() > 1) {
			int ncol = pairs.size() / 2;
			sourceColumns = new String[ncol];
			schemes = new String[ncol];
			for (int i = 0; i < ncol; i++) {
				sourceColumns[i] = (String) pairs.elementAt(i * 2);
				schemes[i] = (String) pairs.elementAt(i * 2 + 1);
			}
		}
		if (dependentCols != null && dependentCols.size() > 0) {
			isParameter = true;
		}
		return true;
	}

	/**
	* Stores the description of a time reference to a file as a sequence of
	* lines starting with <TimeReference> and ending with </TimeReference>.
	*/
	@Override
	public void writeDescription(DataOutputStream writer) throws IOException {
		if (writer == null)
			return;
		writer.writeBytes("<TimeReference>\n");
		if (meaning < 0 || meaning >= MEANINGS.length) {
			meaning = OCCURRED_AT;
		}
		writer.writeBytes("meaning=\"" + MEANINGS[meaning] + "\"\n");
		if (sourceColumns != null) {
			for (int i = 0; i < sourceColumns.length; i++) {
				writer.writeBytes("\"" + sourceColumns[i] + "\"=\"" + schemes[i] + "\"\n");
			}
		}
		if (attrName != null) {
			writer.writeBytes("attr_name=\"" + attrName + "\"\n");
		}
		if (attrScheme != null) {
			writer.writeBytes("attr_scheme=\"" + attrScheme + "\"\n");
		}
		if (!isParameter && dependentCols != null && dependentCols.size() > 0) {
			isParameter = true;
		}
		if (isParameter) {
			writer.writeBytes("is_parameter=yes\n");
			if (dependentCols != null && dependentCols.size() > 0) {
				writer.writeBytes("dependent_columns=");
				for (int i = 0; i < dependentCols.size(); i++) {
					if (i > 0) {
						writer.writeBytes(";");
					}
					writer.writeBytes("\"" + (String) dependentCols.elementAt(i) + "\"");
				}
				writer.writeBytes("\n");
			}
			if (protractKnownValues) {
				writer.writeBytes("protract_known_values=yes\n");
			}
		} else if (keepOrigColumns) {
			writer.writeBytes("keep_original_columns=yes\n");
		} else {
			writer.writeBytes("keep_original_columns=no\n");
		}
		writer.writeBytes("</TimeReference>\n");
	}

	/**
	* Replies whether this is a temporal parameter
	*/
	@Override
	public boolean isTemporalParameter() {
		return isParameter;
	}

	/**
	* Returns true, i.e. the values of a temporal parameter must always be ordered
	*/
	@Override
	public boolean mustBeOrdered() {
		return true;
	}

	/**
	* For a parameter with ordered values, the order may be explicitly specified.
	* However, a temporal parameter must always be ordered according to time.
	* Hence, this method always returns null.
	*/
	@Override
	public Vector getValueOrder() {
		return null;
	}
}