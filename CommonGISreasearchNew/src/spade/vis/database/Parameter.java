package spade.vis.database;

import java.util.Vector;

import spade.lib.util.QSortAlgorithm;
import spade.lib.util.StringUtil;
import spade.time.TimeMoment;

/**
* A dataset may contain attributes depending on parameters, for  example,
* population numbers in different years. A Parameter structure describes such
* a parameter, in particular, specifies its name and the list of possible
* values.
*/
public class Parameter implements java.io.Serializable {
	/**
	* Parameter's name
	*/
	protected String name = null;
	/**
	* The list of parameter values that occur in the dataset. Values are, in
	* general, any objects. In particular, these may be time moments.
	*/
	protected Vector values = null;
	/**
	* Indicates whether this parameter is temporal, i.e. has time moments as values
	*/
	protected boolean temporal = false;

	/**
	 * Checks if the given parameter is the same as this parameter, i.e. has the same
	 * name and same values
	 */
	public boolean isSame(Parameter par) {
		if (par == null)
			return false;
		if (temporal != par.isTemporal())
			return false;
		if (name == null || !name.equalsIgnoreCase(par.getName()))
			return false;
		if (values == null || par.getValueCount() < 0)
			return false;
		if (par.getValueCount() != getValueCount())
			return false;
		for (int i = 0; i < values.size(); i++)
			if (par.getValueIndex(values.elementAt(i)) < 0)
				return false;
		for (int i = 0; i < par.getValueCount(); i++)
			if (getValueIndex(par.getValue(i)) < 0)
				return false;
		return true;
	}

	/**
	* Returns the name of this parameter.
	*/
	public String getName() {
		return name;
	}

	/**
	* Sets the name of the parameter.
	*/
	public void setName(String paramName) {
		name = paramName;
	}

	/**
	* Returns the number of known parameter values.
	*/
	public int getValueCount() {
		if (values == null)
			return 0;
		return values.size();
	}

	/**
	* Returns the vector of known parameter values.
	*/
	public Vector getValues() {
		return values;
	}

	/**
	* Returns the value with the given index, if exists.
	*/
	public Object getValue(int idx) {
		if (values == null || idx < 0 || idx >= values.size())
			return null;
		return values.elementAt(idx);
	}

	public Object getFirstValue() {
		if (values == null || values.size() < 1)
			return null;
		else
			return values.elementAt(0);
	}

	public Object getLastValue() {
		if (values == null || values.size() < 1)
			return null;
		else
			return values.elementAt(values.size() - 1);
	}

	/**
	* Returns the index of the given index, if it is present in the list of
	* parameter values.
	*/
	public int getValueIndex(Object val) {
		if (val == null || values == null || values.size() < 1)
			return -1;
		if (val instanceof String)
			return StringUtil.indexOfStringInVectorIgnoreCase((String) val, values);
		return values.indexOf(val);
	}

	/**
	* Adds the given value to the list of known values.
	*/
	public void addValue(Object value) {
		if (value == null)
			return;
		if (values == null) {
			values = new Vector(50, 20);
		}
		if (value instanceof String)
			if (!StringUtil.isStringInVectorIgnoreCase((String) value, values)) {
				values.addElement(value);
			} else {
				;
			}
		else if (!values.contains(value)) {
			values.addElement(value);
			if (temporal || values.size() == 1) {
				temporal = value instanceof TimeMoment;
			}
		}
	}

	/**
	* Sorts the values of the parameter
	*/
	public void sortValues() {
		if (values == null || values.size() < 2)
			return;
		QSortAlgorithm.sort(values);
	}

	/**
	* Arranges the values of the parameter in the prescribed order
	*/
	public void setValueOrder(Vector order) {
		if (order == null || order.size() < 2)
			return;
		if (values == null || values.size() < 1) {
			values = (Vector) order.clone();
			return;
		}
		for (int i = values.size() - 1; i >= 0; i--)
			if (values.elementAt(i) instanceof String)
				if (StringUtil.isStringInVectorIgnoreCase((String) values.elementAt(i), order)) {
					values.removeElementAt(i);
				} else {
					;
				}
			else if (order.contains(values.elementAt(i))) {
				values.removeElementAt(i);
			}
		for (int i = 0; i < order.size(); i++) {
			values.insertElementAt(order.elementAt(i), i);
		}
	}

	/**
	* Informs whether this parameter is temporal, i.e. has time moments as values
	*/
	public boolean isTemporal() {
		return temporal;
	}

	public void setTemporal(boolean temporal) {
		this.temporal = temporal;
	}
}