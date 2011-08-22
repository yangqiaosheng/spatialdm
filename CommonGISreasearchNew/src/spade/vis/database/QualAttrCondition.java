package spade.vis.database;

import java.util.Hashtable;
import java.util.Vector;

import spade.lib.util.IdUtil;
import spade.lib.util.StringUtil;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 09-Jan-2007
 * Time: 10:10:42
 * Implements a component of an attribute filter - a filter condition for a
 * qualitative attribute.
 */
public class QualAttrCondition implements AttrCondition {
	/**
	* The table, in which to check the condition satisfaction
	*/
	protected AttributeDataPortion table = null;
	/**
	* The index of the attribute (i.e. table column) the query condition
	* refers to.
	*/
	protected int colN = -1;
	/**
	* Indicates whether missing values must be treated as satisfying the condition
	* (missingValuesOK==true, default value) or as not satisfying
	* (missingValuesOK==false).
	*/
	protected boolean missingValuesOK = true;
	/**
	 * The "right" (selected) values of the attributes. If null or empty, all
	 * values are treated as "right".
	 */
	protected Vector rightValues = null;
	/**
	 * Indicates whether it is allowed to adjust the condition to the actual
	 * data values occurring in the table
	 */
	protected boolean allowAdjust = true;
	/**
	 * For optimisation, contains all the values from the table column
	 */
	protected Vector values = null;
	/**
	 * Used to filter out all values
	 */
	protected boolean allValuesOff = false;

	/**
	* Sets the table, in which to check the condition satisfaction
	*/
	@Override
	public void setTable(AttributeDataPortion table) {
		this.table = table;
	}

	/**
	* Returns a reference to the table the condition is attached to
	*/
	public AttributeDataPortion getTable() {
		return table;
	}

	/**
	* Sets the index of the attribute (i.e. table column) the query condition
	* refers to.
	*/
	@Override
	public void setAttributeIndex(int idx) {
		colN = idx;
	}

	/**
	* Returns the index of the attribute (i.e. table column) the query condition
	* refers to.
	*/
	@Override
	public int getAttributeIndex() {
		return colN;
	}

	/**
	* Returns the identifier of the attribute (i.e. table column) the query
	* condition refers to.
	*/
	@Override
	public String getAttributeId() {
		if (table != null)
			return table.getAttributeId(colN);
		return null;
	}

	/**
	 * Sets whether it is allowed to adjust the condition to the actual
	 * data values occurring in the table
	 */
	@Override
	public void setAllowAdjust(boolean allowAdjust) {
		this.allowAdjust = allowAdjust;
	}

	/**
	 * Whether all values are filtered out
	 */
	public boolean areAllValuesOff() {
		return allValuesOff;
	}

	/**
	 * Sets all values to be filtered out
	 */
	public void setAllValuesOff(boolean allValuesOff) {
		this.allValuesOff = allValuesOff;
	}

	/**
	* Replies whether any limit is set in this query condition
	*/
	@Override
	public boolean hasLimit() {
		return allValuesOff || (rightValues != null && rightValues.size() > 0);
	}

	/**
	* Clears the limits set earlier.
	*/
	@Override
	public void clearLimits() {
		if (rightValues != null) {
			rightValues.removeAllElements();
		}
		allValuesOff = false;
	}

	/**
	* Informs whether missing values are currently treated as satisfying the query
	* (returns true) or not (returns false).
	*/
	@Override
	public boolean getMissingValuesOK() {
		return missingValuesOK;
	}

	/**
	* Sets whether missing values must be treated as satisfying the query
	* (argument is true) or not (argument is false).
	*/
	@Override
	public void setMissingValuesOK(boolean ok) {
		missingValuesOK = ok;
	}

	/**
	 * Checks if the given value satisfies the condition
	 */
	public boolean doesSatisfy(String value) {
		if (value == null)
			if (hasLimit())
				return false;
			else
				return missingValuesOK;
		if (allValuesOff)
			return false;
		if (!hasLimit())
			return true;
		return StringUtil.isStringInVectorIgnoreCase(value, rightValues);
	}

	/**
	* Checks the satisfaction of this query condition by the given data item
	*/
	@Override
	public boolean doesSatisfy(ThematicDataItem data) {
		if (data == null)
			return false;
		return doesSatisfy(data.getAttrValueAsString(colN));
	}

	/**
	* Checks the satisfaction of this query condition by the table record (row)
	* with the given index.
	*/
	@Override
	public boolean doesSatisfy(int rowN) {
		if (table == null || rowN < 0 || rowN >= table.getDataItemCount())
			return false;
		if (values != null && rowN < values.size())
			return doesSatisfy((String) values.elementAt(rowN));
		String str = table.getAttrValueAsString(colN, rowN);
		if (str != null && str.length() < 1) {
			str = null;
		}
		return doesSatisfy(str);
	}

	/**
	* Gets the values from the column this condition refers to
	*/
	protected void getValues() {
		if (colN < 0 || table == null) {
			values = null;
			return;
		}
		if (values == null) {
			values = new Vector(table.getDataItemCount(), 100);
		} else {
			values.removeAllElements();
		}
		for (int i = 0; i < table.getDataItemCount(); i++) {
			String str = table.getAttrValueAsString(colN, i);
			if (str != null && str.length() < 1) {
				str = null;
			}
			values.addElement(str);
		}
	}

	/**
	 * Returns all unique attribute values available in the table
	 */
	public Vector getAllUniqueValues() {
		if (values == null || values.size() < 1) {
			getValues();
			if (values == null || values.size() < 1)
				return null;
		}
		//make a vector containing only unique values
		Vector unique = new Vector(values.size(), 1);
		for (int i = 0; i < values.size(); i++)
			if (values.elementAt(i) != null && !StringUtil.isStringInVectorIgnoreCase((String) values.elementAt(i), unique)) {
				unique.addElement(values.elementAt(i));
			}
		if (unique.size() < 1)
			return null;
		return unique;
	}

	/**
	 * Returns the "right" (currently selected) values
	 */
	public Vector getRightValues() {
		return rightValues;
	}

	/**
	 * Sets the "right" values
	 */
	public void setRightValues(Vector rightValues) {
		this.rightValues = rightValues;
		if (rightValues != null && rightValues.size() > 0) {
			allValuesOff = false;
		}
	}

	/**
	* Checks whether the value in the condition-controlled column of the given
	* record is missing
	*/
	@Override
	public boolean isValueMissing(int rowN) {
		if (table == null || rowN < 0 || rowN >= table.getDataItemCount())
			return false;
		String val = (values != null && rowN < values.size()) ? (String) values.elementAt(rowN) : table.getAttrValueAsString(colN, rowN);
		return val == null || val.length() < 1;
	}

	/**
	* Adjusts its internal settings, if necessary, when the data in the column
	* controlled by this condition change.
	*/
	@Override
	public void adaptToDataChange() {
		getValues();
		if (!allowAdjust)
			return;
		if (rightValues == null || rightValues.size() < 1)
			return;
		if (values == null || values.size() < 1) {
			clearLimits();
			return;
		}
		for (int i = rightValues.size() - 1; i >= 0; i--)
			if (!StringUtil.isStringInVectorIgnoreCase((String) rightValues.elementAt(i), values)) {
				rightValues.removeElementAt(i);
			}
	}

	/**
	* Returns the type of this condition, e.g. "NumAttrCondition". The type must be
	* unique for each class implementing the interface spade.vis.database.Condition
	*/
	@Override
	public String getConditionType() {
		return "QualAttrCondition";
	}

	/**
	* Returns the description of the condition in a form of hashtable. The
	* hashtable consists of pairs <key, value>. It is required that both the keys
	* and the values are strings, because they are used for storing descriptions
	* of query conditions in an ASCII file.
	*/
	@Override
	public Hashtable getDescription() {
		if (table == null || colN < 0 || colN >= table.getAttrCount())
			return null;
		Hashtable d = new Hashtable();
		d.put("attribute", IdUtil.getPureAttrId(table.getAttributeId(colN)));
		d.put("missing_values_ok", String.valueOf(missingValuesOK));
		if (!hasLimit()) {
			d.put("has_limit", "false");
		} else {
			StringBuffer sb = new StringBuffer(1000);
			for (int i = 0; i < rightValues.size(); i++) {
				if (i > 0) {
					sb.append(",");
				}
				sb.append("\"" + (String) rightValues.elementAt(i) + "\"");
			}
			d.put("right_values", sb.toString());
		}
		return d;
	}

	/**
	* Setups the condition according to the description specified in a form of
	* hashtable. The hashtable consists of pairs <key, value>, where both the keys
	* and the values are strings.
	*/
	@Override
	public void setup(Hashtable descr) {
		if (descr == null || table == null)
			return;
		String str = (String) descr.get("attribute");
		if (str == null)
			return;
		int idx = table.getAttrIndex(str);
		if (idx < 0)
			return;
		setAttributeIndex(idx);
		str = (String) descr.get("missing_values_ok");
		if (str != null) {
			setMissingValuesOK(str.equalsIgnoreCase("true"));
		}
		str = (String) descr.get("has_limit");
		if (str != null && str.equalsIgnoreCase("false"))
			return;
		str = (String) descr.get("right_values");
		if (str == null || str.length() < 1)
			return;
		rightValues = StringUtil.getNames(str, ",");
	}

	/**
	* Returns the number of records satisfying the constraints on the given
	* attribute
	*/
	@Override
	public int getNSatisfying() {
		if (values == null) {
			getValues();
		}
		if (values == null || values.size() < 1)
			return 0;
		//if (!hasLimit()) return values.size();
		int nsat = 0;
		for (int i = 0; i < values.size(); i++)
			if (doesSatisfy((String) values.elementAt(i))) {
				++nsat;
			}
		return nsat;
	}

	/**
	* Returns the number of records with missing values of the given
	* attribute
	*/
	@Override
	public int getNMissingValues() {
		if (values == null)
			return 0;
		int nmiss = 0;
		for (int i = 0; i < values.size(); i++)
			if (values.elementAt(i) == null) {
				++nmiss;
			}
		return nmiss;
	}
}
