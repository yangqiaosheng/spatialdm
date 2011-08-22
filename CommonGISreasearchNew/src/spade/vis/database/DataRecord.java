package spade.vis.database;

import java.util.Vector;

import spade.lib.util.IdUtil;
import spade.time.TimeMoment;
import spade.time.TimeReference;

public class DataRecord implements ThematicDataItem, java.io.Serializable {
	/**
	* The identifier of the object the data refer to.
	*/
	protected String id = null;
	/**
	* The name of the object the data refer to (may be null).
	*/
	protected String name = null;
	/**
	* The index of this data item in the container in which it is included.
	* -1 means that it is not included in any container.
	*/
	protected int index = -1;
	/**
	* A DataRecord may have a time reference
	*/
	protected TimeReference tref = null;
	/**
	* The list of descriptors of attributes.
	*/
	protected Vector attrList = null;
	/**
	* The vector with values of attributes. The values are, in general, objects
	* (typically strings, but may be, for example, time moments)
	*/
	protected Vector values = null;
	/**
	* An array with values of attributes in the double format. For the non-numeric
	* attributes contains Double.NaN
	*/
	private double fval[] = null;
	/**
	 * If the data record describes some object existing in the system
	 * (e.g. time moment, geo object), this may be a reference to the object
	 */
	protected Object describedObject = null;

	public DataRecord(String identifier) {
		id = identifier;
	}

	public DataRecord(String identifier, String name) {
		id = identifier;
		this.name = name;
	}

	/**
	* Returns the index of this data item in the container in which it is included.
	* May return -1 if not included in any container.
	*/
	@Override
	public int getIndexInContainer() {
		return index;
	}

	/**
	* Sets the index of this data item in the container in which it is included.
	*/
	@Override
	public void setIndexInContainer(int idx) {
		index = idx;
	}

	/**
	 * If the data record describes some object existing in the system
	 * (e.g. time moment, geo object), returns a reference to the object
	 */
	public Object getDescribedObject() {
		return describedObject;
	}

	/**
	 * If the data record describes some object existing in the system
	 * (e.g. time moment, geo object), sets a reference to the object
	 */
	public void setDescribedObject(Object describedObject) {
		this.describedObject = describedObject;
	}

	/**
	* The method copyTo(DataItem) is used for updating data items and spatial
	* objects derived from them when data change events occur, for example,
	* in visualisation of temporal data.
	* The DataItem passed as an argument should be an instance of DataRecord.
	* The identifier of the data item is not copied! It is assumed that the
	* DataItem passed as an argument has the same identifier as this DataItem.
	*/
	@Override
	public void copyTo(DataItem dit) {
		if (dit == null)
			return;
		if (dit instanceof DataRecord) {
			DataRecord dr = (DataRecord) dit;
			dr.setName(getName());
			dr.setAttrList(getAttrList());
			dr.setAttrValues(getAttrValues());
		}
	}

	/**
	* Produces and returns a copy of itself.
	*/
	@Override
	public Object clone() {
		DataRecord dr = new DataRecord(id, name);
		Vector v = getAttrList();
		if (v != null) {
			dr.setAttrList((Vector) v.clone());
		}
		v = getAttrValues();
		if (v != null) {
			dr.setAttrValues((Vector) v.clone());
		}
		return dr;
	}

	/**
	 * Makes a copy of itsels
	 * @param cloneAttrList - whether to clone the list of attributes or just
	 *   copy the reference
	 * @param cloneValueList - whether to clone the list of attribute values
	 *   or just copy the reference
	 */
	public DataRecord makeCopy(boolean cloneAttrList, boolean cloneValueList) {
		DataRecord dr = new DataRecord(id, name);
		Vector v = getAttrList();
		if (v != null) {
			dr.setAttrList((cloneAttrList) ? Attribute.makeCopyOfAttributeList(v) : v);
		}
		v = getAttrValues();
		if (v != null) {
			dr.setAttrValues((cloneValueList) ? (Vector) v.clone() : v);
		}
		return dr;
	}

	@Override
	public String getId() {
		return id;
	}

	/**
	* Changes the identifier of the entity; use cautiously!
	*/
	@Override
	public void setId(String ident) {
		id = ident;
	}

	@Override
	public String getName() {
		return (name == null) ? id : name;
	}

	@Override
	public boolean hasName() {
		return name != null && name.length() > 0;
	}

	@Override
	public void setName(String aName) {
		name = aName;
	}

	/**
	* Associated this DataRecord with a time reference.
	*/
	@Override
	public void setTimeReference(TimeReference ref) {
		tref = ref;
	}

	/**
	* Returns its time reference
	*/
	@Override
	public TimeReference getTimeReference() {
		return tref;
	}

	/**
	* Returns a list of descriptors of attributes.
	*/
	public Vector getAttrList() {
		return attrList;
	}

	/**
	* Sets a reference to the list of attribute descriptors
	*/
	public void setAttrList(Vector list) {
		attrList = list;
	}

	/**
	* Returns the number of attributes.
	*/
	@Override
	public int getAttrCount() {
		if (attrList == null)
			return 0;
		return attrList.size();
	}

	/**
	* removes an attribute with the given number/identifier
	*/
	public void removeAttribute(int n) {
		if (values == null || values.size() <= n)
			return;
		values.removeElementAt(n);
		if (fval != null && fval.length > n) {
			fval = null;
		}
	}

	public void removeAttribute(String attrId) {
		int n = getAttrIndex(attrId);
		if (n >= 0) {
			removeAttribute(n);
		}
	}

	/**
	* removes attributes within a given interval of numbers
	*/
	public void removeAttributes(int n1, int n2) {
		if (values == null || values.size() <= n1)
			return;
		if (n2 >= values.size()) {
			n2 = values.size() - 1;
		}
		for (int n = n2; n >= n1; n--) {
			values.removeElementAt(n);
		}
		if (fval != null && fval.length > n1) {
			fval = null;
		}
	}

	/**
	* Returns the descriptor of the attribute with the given index in the
	* attribute list
	*/
	public Attribute getAttribute(int n) {
		if (n < 0 || n >= getAttrCount())
			return null;
		return (Attribute) attrList.elementAt(n);
	}

	/**
	* Returns the identifier of the attribute with the given index in the
	* attribute list
	*/
	@Override
	public String getAttributeId(int n) {
		if (n < 0 || n >= getAttrCount())
			return null;
		return ((Attribute) attrList.elementAt(n)).getIdentifier();
	}

	/**
	* Returns the name of the attribute with the given index in the
	* attribute list
	*/
	@Override
	public String getAttributeName(int n) {
		if (n < 0 || n >= getAttrCount())
			return null;
		return ((Attribute) attrList.elementAt(n)).getName();
	}

	/**
	* Returns the index of the attribute with the given identifier in the
	* attribute list
	*/
	@Override
	public int getAttrIndex(String attrId) {
		if (attrId == null || attrList == null)
			return -1;
		for (int i = 0; i < attrList.size(); i++) {
			Attribute attr = (Attribute) attrList.elementAt(i);
			if (attrId.equalsIgnoreCase(attr.getIdentifier()))
				return i;
		}
		if (attrId.equals(IdUtil.getPureAttrId(attrId))) {
			for (int i = 0; i < attrList.size(); i++) {
				Attribute attr = (Attribute) attrList.elementAt(i);
				if (attrId.equalsIgnoreCase(IdUtil.getPureAttrId(attr.getIdentifier())))
					return i;
			}
		}
		return -1;
	}

	/**
	* Returns the descriptor of the attribute with the given identifier
	*/
	public Attribute getAttribute(String attrId) {
		return getAttribute(getAttrIndex(attrId));
	}

	/**
	* Returns the type of the specified attribute
	*/
	@Override
	public char getAttrType(String attrId) {
		return getAttrType(getAttrIndex(attrId));
	}

	/**
	* Returns the type of the specified attribute
	*/
	@Override
	public char getAttrType(int attrN) {
		Attribute attr = getAttribute(attrN);
		if (attr != null)
			return attr.getType();
		return 0;
	}

	/**
	* Sets the list of values of all the attributes
	*/
	public void setAttrValues(Vector attrValues) {
		values = attrValues;
	}

	/**
	* Returns the vector of all attribute values
	*/
	@Override
	public Vector getAttrValues() {
		return values;
	}

	/**
	* Adds a value to the vector of attribute values. The value is, most usually,
	* a string, but may be, for example, a time moment.
	*/
	public void addAttrValue(Object value) {
		if (values == null) {
			int nattr = getAttrCount();
			if (nattr < 10) {
				nattr = 10;
			}
			values = new Vector(nattr, 100);
		}
		values.addElement(value);
	}

	/**
	* Sets the specified element of the vector of values.
	*/
	public void setAttrValue(Object value, int idx) {
		if (idx < 0)
			return;
		if (values == null) {
			int nattr = getAttrCount();
			if (nattr < 10) {
				nattr = 10;
			}
			values = new Vector(nattr, 100);
		}
		while (values.size() <= idx) {
			values.addElement(null);
		}
		values.setElementAt(value, idx);
		if (fval != null && fval.length > idx) {
			fval[idx] = Double.NaN;
			if (value != null) {
				Attribute attr = getAttribute(idx);
				if (attr != null)
					if (attr.isNumeric()) {
						try {
							fval[idx] = Double.valueOf(value.toString()).doubleValue();
						} catch (NumberFormatException nfe) {
							fval[idx] = Double.NaN;
						}
					} else if (attr.isTemporal()) {
						if (value instanceof TimeMoment) {
							fval[idx] = (((TimeMoment) value).toNumber());
						}
					}
			}
		}
	}

	public void setNumericAttrValue(double value, int idx) {
		setNumericAttrValue(value, String.valueOf(value), idx);
	}

	public void setNumericAttrValue(double value, String strValue, int idx) {
		if (idx < 0)
			return;
		if (values == null) {
			int nattr = getAttrCount();
			if (nattr < 10) {
				nattr = 10;
			}
			values = new Vector(nattr, 100);
		}
		while (values.size() <= idx) {
			values.addElement(null);
		}
		if (!Double.isNaN(value)) {
			values.setElementAt(strValue, idx);
		} else {
			values.setElementAt(null, idx);
		}
		if (fval != null && fval.length > idx) {
			fval[idx] = value;
		}
	}

	/**
	* Returns the value of the attribute with the given identifier
	*/
	@Override
	public Object getAttrValue(String attrId) {
		return getAttrValue(getAttrIndex(attrId));
	}

	/**
	* Returns the value of the attribute with the given index in the list of
	* attributes
	*/
	@Override
	public Object getAttrValue(int attrN) {
		if (values == null)
			return null;
		if (attrN < 0 || attrN >= values.size())
			return null;
		Object val = values.elementAt(attrN);
		if (val == null)
			return null;
		if (val instanceof String) {
			String str = ((String) val).trim();
			if (str.length() < 1)
				return null;
			return str;
		}
		return val;
	}

	/**
	* Returns a string representation of the value of the attribute with the
	* given identifier
	*/
	@Override
	public String getAttrValueAsString(String attrId) {
		Object val = getAttrValue(attrId);
		if (val != null)
			return val.toString();
		return null;
	}

	/**
	* Returns a string representation of the value of the attribute with the
	* given number (index in the list)
	*/
	@Override
	public String getAttrValueAsString(int attrN) {
		Object val = getAttrValue(attrN);
		if (val != null)
			return val.toString();
		return null;
	}

	/**
	* Assuming that the attribute is numeric, returns its value as a double number
	* (to avoid transformation from strings to numbers in each place where
	* numbers are needed). If the attribute is not of a numeric type, returns
	* Double.NaN.
	*/
	@Override
	public double getNumericAttrValue(String attrId) {
		return getNumericAttrValue(getAttrIndex(attrId));
	}

	@Override
	public double getNumericAttrValue(int attrN) {
		if (attrN < 0 || attrN >= values.size())
			return Double.NaN;
		Attribute attr = getAttribute(attrN);
		if (attr == null)
			return Double.NaN;
		if (!attr.isNumeric() && !attr.isTemporal())
			return Double.NaN;
		if (fval == null || fval.length <= attrN) {
			fval = new double[getAttrCount()];
			for (int i = 0; i < getAttrCount(); i++) {
				fval[i] = Double.NaN;
				Attribute a1 = getAttribute(i);
				if (a1.isNumeric()) {
					String str = getAttrValueAsString(i);
					if (str != null) {
						try {
							fval[i] = Double.valueOf(str).doubleValue();
						} catch (NumberFormatException nfe) {
							fval[i] = Double.NaN;
						}
					}
				} else if (a1.isTemporal()) {
					Object val = getAttrValue(i);
					if (val != null && (val instanceof TimeMoment)) {
						fval[i] = (((TimeMoment) val).toNumber());
					}
				}
			}
		}
		return fval[attrN];
	}

	/**
	 * Informs whether this data record contains no actual values but only nulls.
	 */
	@Override
	public boolean isEmpty() {
		if (values == null || values.size() < 1)
			return true;
		for (int i = 0; i < values.size(); i++)
			if (values.elementAt(i) != null)
				return false;
		return true;
	}

}
