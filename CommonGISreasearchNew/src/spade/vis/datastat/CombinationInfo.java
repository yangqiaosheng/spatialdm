package spade.vis.datastat;

import java.util.Vector;

import spade.lib.util.StringUtil;

/**
* Contains information about a combination of two or more string values:
* the values themselves and the number of occurresnces of this combination
*/
public class CombinationInfo {
	/**
	* The vector contains the combination of values
	*/
	public Vector values = null;
	/**
	* The number of occurrences
	*/
	protected int nOcc = 0;

	public void setValues(Vector val) {
		if (val == null) {
			values = null;
		} else {
			values = (Vector) val.clone();
		}
	}

	public Vector getValues() {
		return values;
	}

	public String getValue(int idx) {
		if (idx < 0 || values == null || idx >= values.size())
			return null;
		return (String) values.elementAt(idx);
	}

	public void addValue(String value) {
		if (values == null) {
			values = new Vector(5, 5);
		}
		values.addElement(value);
	}

	/**
	* Checks if the given vector contains the same values as the values stored
	* in the object, and in the same order. The case is ignored.
	*/
	public boolean sameValues(Vector val) {
		if (values == null || val == null)
			return false;
		if (values.size() != val.size())
			return false;
		for (int i = 0; i < values.size(); i++)
			if (!StringUtil.sameStringsIgnoreCase((String) values.elementAt(i), (String) val.elementAt(i)))
				return false;
		return true;
	}

	/**
	* Increments the combination counter
	*/
	public void incrementCount() {
		++nOcc;
	}

	/**
	* Returns the counted number of combinations
	*/
	public int getCount() {
		return nOcc;
	}

	/**
	* Resets the counter to zero
	*/
	public void reset() {
		nOcc = 0;
	}

}