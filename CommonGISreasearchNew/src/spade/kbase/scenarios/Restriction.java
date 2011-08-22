package spade.kbase.scenarios;

import java.util.StringTokenizer;
import java.util.Vector;

/**
* Restrictions for defining task context elements or for application of data
* analysis instruments, an internal representation of the "Restriction" element
* of the task knowledge base.
*/
public class Restriction {
	public static final String restrTypes[] = { "function_presence", "data_presence", "layer_content", "is_defined", "exclude_items", "max_attr_number", "min_attr_number", "attr_type", "attr_relation" };
	public static final String contentTypes[] = { "entities", "localities", "occurrences", "territory_division", "sample_locations", "grid" };

	public String type = null;
	public Vector values = null;

	public void setRestrictionType(String value) {
		if (value != null && value.length() > 0) {
			for (String restrType : restrTypes)
				if (value.equalsIgnoreCase(restrType)) {
					type = value;
					break;
				}
		}
	}

	/**
	* The argument liststr contains a list of values separated by whitespaces
	*/
	public void setValues(String liststr) {
		if (liststr == null)
			return;
		StringTokenizer st = new StringTokenizer(liststr, " ,;\t\n\r\f");
		if (!st.hasMoreTokens())
			return;
		values = new Vector(st.countTokens(), 5);
		while (st.hasMoreTokens()) {
			values.addElement(st.nextToken());
		}
		if (values.size() < 1) {
			values = null;
		}
	}

	public int getValuesCount() {
		if (values == null)
			return 0;
		return values.size();
	}

	public boolean hasValues() {
		return getValuesCount() > 0;
	}

	public String getValue(int idx) {
		if (idx < 0 || idx >= getValuesCount())
			return null;
		return (String) values.elementAt(idx);
	}

	public boolean isValid() {
		return type != null && hasValues();
	}

	@Override
	public String toString() {
		String str = "Restriction: " + type + "=" + "<";
		if (values == null) {
			str += "null";
		} else {
			for (int i = 0; i < values.size(); i++)
				if (i > 0) {
					str += "," + values.elementAt(i);
				} else {
					str += values.elementAt(i);
				}
		}
		str += ">";
		return str;
	}
}
