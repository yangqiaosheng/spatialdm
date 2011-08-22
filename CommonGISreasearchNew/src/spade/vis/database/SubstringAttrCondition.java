package spade.vis.database;

import java.util.Hashtable;
import java.util.Vector;

import spade.lib.util.IdUtil;

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: Jan 19, 2010
 * Time: 3:15:49 PM
 * To change this template use File | Settings | File Templates.
 */
public class SubstringAttrCondition implements AttrCondition {

	/**
	* The table, in which to check the condition satisfaction
	*/
	protected AttributeDataPortion table = null;

	public AttributeDataPortion getTable() {
		return table;
	}

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
	 * condition string and operation parameters;
	 */
	public final int CondContains = 0, CondEqual = 1, CondStartsWith = 2, CondEndsWith = 3, CondList = 4, CondOrderedList = 5;
	protected int condType = CondContains;
	protected String condStr = "";
	Vector<String> vCondStr = null;

	public String getConditionsAsString() {
		String str = "";
		switch (condType) {
		case CondContains:
			str = "contains";
			break;
		case CondEqual:
			str = "equals to";
			break;
		case CondStartsWith:
			str = "starts with";
			break;
		case CondEndsWith:
			str = "ends with";
			break;
		case CondList:
			str = "contains list";
			break;
		case CondOrderedList:
			str = "contains ordered list";
			break;
		}
		return str + " < " + condStr + ">";
	}

	public void setCondParams(String condStr, int condType) {
		this.condStr = condStr;
		this.condType = condType;
		if (condType == CondList || condType == CondOrderedList) {
			parseCondition();
		}
	}

	public void parseCondition() {
		vCondStr = new Vector<String>(10, 10);
		String str = condStr;
		while (str != null && str.length() > 0) {
			int pos = str.indexOf(",");
			if (pos > 0) {
				vCondStr.addElement(str.substring(0, pos - 1).trim());
				str = str.substring(pos + 1);
			} else {
				vCondStr.addElement(str.trim());
				str = null;
			}
		}
	}

	// -------------------- AttrCondition interface -----------------------------
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
	* Returns the number of records satisfying the constraints on the given
	* attribute
	*/
	@Override
	public int getNSatisfying() {
		int n = 0;
		if (table != null) {
			for (int r = 0; r < table.getDataItemCount(); r++)
				if (doesSatisfy(table.getAttrValueAsString(colN, r))) {
					n++;
				}
		}
		return n;
	}

	/**
	* Returns the number of records with missing values of the given
	* attribute
	*/
	@Override
	public int getNMissingValues() {
		int n = 0;
		if (table != null) {
			for (int r = 0; r < table.getDataItemCount(); r++)
				if (isValueMissing(r)) {
					n++;
				}
		}
		return n;
	}

	/**
	 * Sets whether it is allowed to adjust the condition to the actual
	 * data values occurring in the table
	 */
	@Override
	public void setAllowAdjust(boolean allowAdjust) {
	}

	// -------------------- Condition interface ---------------------------------

	/**
	* Sets the table, in which to check the condition satisfaction
	*/
	@Override
	public void setTable(AttributeDataPortion table) {
		this.table = table;
	}

	/**
	* Replies whether any limit is set in this query condition
	*/
	@Override
	public boolean hasLimit() {
		return condStr.length() > 0;
	}

	/**
	* Clears the limits set earlier.
	*/
	@Override
	public void clearLimits() {
		condStr = "";
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
		return doesSatisfy(table.getAttrValueAsString(colN, rowN));
	}

	protected boolean doesSatisfy(String str) {
		if (str == null || str.length() == 0)
			if (hasLimit())
				return false;
			else
				return missingValuesOK;
		if (!hasLimit())
			return true;
		switch (condType) {
		case CondContains:
			return str.indexOf(condStr) >= 0;
		case CondEqual:
			return str.equals(condStr);
		case CondStartsWith:
			return str.startsWith(condStr);
		case CondEndsWith:
			return str.endsWith(condStr);
		case CondList:
			if (vCondStr == null || vCondStr.size() == 0)
				return true;
			for (int i = 0; i < vCondStr.size(); i++)
				if (str.indexOf(vCondStr.elementAt(i)) < 0)
					return false;
			return true;
		case CondOrderedList:
			if (vCondStr == null || vCondStr.size() == 0)
				return true;
			int pos = 0;
			for (int i = 0; i < vCondStr.size(); i++) {
				pos = str.indexOf(vCondStr.elementAt(i), pos);
				if (pos < 0)
					return false;
				else {
					pos += vCondStr.elementAt(i).length();
				}
			}
			return true;
		default: // should never happen
			return true;
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
		String val = table.getAttrValueAsString(colN, rowN);
		return val == null || val.length() == 0;
	}

	/**
	* Adjusts its internal settings, if necessary, when the data in the column
	* controlled by this condition change.
	*/
	@Override
	public void adaptToDataChange() {
		clearLimits();
	}

	/**
	* Returns the type of this condition, e.g. "NumAttrCondition". The type must be
	* unique for each class implementing the interface spade.vis.database.Condition
	*/
	@Override
	public String getConditionType() {
		return "SubstringAttrCondition";
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
			d.put("CondType", String.valueOf(condType));
			d.put("CondStr", condStr);
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
		condType = Integer.valueOf((String) descr.get("CondType")).intValue();
		condStr = (String) descr.get("CondStr");
	}

}
