package spade.vis.database;

import java.util.Hashtable;

import spade.lib.util.IdUtil;
import spade.lib.util.NumRange;

/**
* Implements a component of an attribute filter - a filter condition for a
* numeric attribute. In such a condition, the minimum and maximum limits
* may be specified.
*/
public class NumAttrCondition implements AttrCondition {
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
	* The value range of the attribute
	*/
	protected NumRange range = null;
	/**
	* The minimum and maximum limits for values of the attribute
	*/
	protected double minLimit = Double.NaN, maxLimit = Double.NaN;
	/**
	 * Indicates whether it is allowed to adjust the condition to the actual
	 * data values occurring in the table
	 */
	protected boolean allowAdjust = true;
	/**
	* Indicates whether missing values must be treated as satisfying the condition
	* (missingValuesOK==true, default value) or as not satisfying
	* (missingValuesOK==false).
	*/
	protected boolean missingValuesOK = false;
	/**
	* The numeric values from the condition-controlled column.
	*/
	protected double values[] = null;

	/**
	* Sets the table, in which to check the condition satisfaction
	*/
	@Override
	public void setTable(AttributeDataPortion table) {
		this.table = table;
	}

	/**
	* Sets the index of the attribute (i.e. table column) the query condition
	* refers to.
	*/
	@Override
	public void setAttributeIndex(int idx) {
		colN = idx;
		getValues();
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
	* Gets the values from the column this condition refers to
	*/
	protected void getValues() {
		if (colN < 0 || table == null) {
			range = null;
			values = null;
			return;
		}
		if (values == null || values.length != table.getDataItemCount()) {
			values = new double[table.getDataItemCount()];
		}
		if (range == null) {
			range = new NumRange();
		} else {
			range.minValue = range.maxValue = Double.NaN;
		}
		for (int i = 0; i < table.getDataItemCount(); i++) {
			values[i] = table.getNumericAttrValue(colN, i);
			if (!Double.isNaN(values[i])) {
				if (Double.isNaN(range.minValue) || range.minValue > values[i]) {
					range.minValue = values[i];
				}
				if (Double.isNaN(range.maxValue) || range.maxValue < values[i]) {
					range.maxValue = values[i];
				}
			}
		}
		if (Double.isNaN(range.maxValue)) {
			range = null;
		}
	}

	/**
	* Returns the value range in the column this condition refers to
	*/
	public NumRange getAttrValueRange() {
		return range;
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
	* Replies whether any limit is set in this query condition
	*/
	@Override
	public boolean hasLimit() {
		return table != null && colN >= 0 && (!missingValuesOK || !Double.isNaN(minLimit) || !Double.isNaN(maxLimit));
	}

	/**
	* Clears the limits set earlier.
	*/
	@Override
	public void clearLimits() {
		minLimit = Double.NaN;
		maxLimit = Double.NaN;
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
	* Sets the minimum limit
	*/
	public void setMinLimit(double value) {
		minLimit = value;
	}

	/**
	* Sets the maximum limit
	*/
	public void setMaxLimit(double value) {
		maxLimit = value;
	}

	/**
	* Sets the minimum and the maximum limits
	*/
	public void setLimits(double min, double max) {
		minLimit = min;
		maxLimit = max;
	}

	/**
	* Returns the minimum limit
	*/
	public double getMinLimit() {
		return minLimit;
	}

	/**
	* Returns the maxnimum limit
	*/
	public double getMaxLimit() {
		return maxLimit;
	}

	/**
	* Checks the satisfaction of this query condition by the given data item
	*/
	@Override
	public boolean doesSatisfy(ThematicDataItem data) {
		if (data == null)
			return false;
		double val = data.getNumericAttrValue(colN);
		if (Double.isNaN(val))
			return missingValuesOK;
		return (Double.isNaN(minLimit) || val >= minLimit) && (Double.isNaN(maxLimit) || val <= maxLimit);
	}

	/**
	* Checks the satisfaction of this query condition by the table record (row)
	* with the given index.
	*/
	@Override
	public boolean doesSatisfy(int rowN) {
		if (values == null || rowN < 0 || rowN >= values.length)
			return false;
		if (Double.isNaN(values[rowN]))
			return missingValuesOK;
		return (Double.isNaN(minLimit) || values[rowN] >= minLimit) && (Double.isNaN(maxLimit) || values[rowN] <= maxLimit);
	}

	/**
	* Checks whether the value in the condition-controlled column of the given
	* record is missing
	*/
	@Override
	public boolean isValueMissing(int rowN) {
		if (values == null || rowN < 0 || rowN >= values.length)
			return false;
		return Double.isNaN(values[rowN]);
	}

	/**
	* Returns the number of records satisfying the constraints on the given
	* attribute
	*/
	@Override
	public int getNSatisfying() {
		if (values == null)
			return 0;
		if (Double.isNaN(minLimit) && Double.isNaN(maxLimit))
			return values.length;
		int nsat = 0;
		for (double value : values)
			if (Double.isNaN(value))
				if (missingValuesOK) {
					++nsat;
				} else {
					;
				}
			else if ((Double.isNaN(minLimit) || value >= minLimit) && (Double.isNaN(maxLimit) || value <= maxLimit)) {
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
		for (double value : values)
			if (Double.isNaN(value)) {
				++nmiss;
			}
		return nmiss;
	}

	/**
	* Adjusts its internal settings, if necessary, when the data in the column
	* controlled by this condition change.
	*/
	@Override
	public void adaptToDataChange() {
		getValues();
		if (allowAdjust) {
			checkLimits();
		}
	}

	/**
	* Checks if the limits lie within the attribute's value range. If not, the
	* limits are cancelled.
	*/
	protected void checkLimits() {
		if (!allowAdjust)
			return;
		if (range == null) {
			clearLimits();
			return;
		}
		if (Double.isNaN(minLimit) && Double.isNaN(maxLimit))
			return;
		double epsilon = 0.0001 * (range.maxValue - range.minValue);
		if (!Double.isNaN(minLimit) && minLimit <= range.minValue + epsilon) {
			minLimit = Double.NaN;
		}
		if (!Double.isNaN(maxLimit) && maxLimit >= range.maxValue - epsilon) {
			maxLimit = Double.NaN;
		}
	}

	/**
	* Returns the type of this condition: "NumAttrCondition". The type must be
	* unique for each class implementing the interface spade.vis.database.Condition
	*/
	@Override
	public String getConditionType() {
		return "NumAttrCondition";
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
		if (!Double.isNaN(minLimit)) {
			d.put("min_limit", String.valueOf(minLimit));
		}
		if (!Double.isNaN(maxLimit)) {
			d.put("max_limit", String.valueOf(maxLimit));
		}
		d.put("missing_values_ok", String.valueOf(missingValuesOK));
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
		str = (String) descr.get("min_limit");
		if (str != null) {
			try {
				double val = Double.valueOf(str).doubleValue();
				setMinLimit(val);
			} catch (NumberFormatException e) {
			}
		}
		str = (String) descr.get("max_limit");
		if (str != null) {
			try {
				double val = Double.valueOf(str).doubleValue();
				setMaxLimit(val);
			} catch (NumberFormatException e) {
			}
		}
		checkLimits();
	}
}
