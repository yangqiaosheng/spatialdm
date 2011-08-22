package spade.vis.database;

import java.util.Vector;

public interface ThematicDataItem extends DataItem {
	/**
	* Returns the number of attributes.
	*/
	public int getAttrCount();

	/**
	* Returns the index of the attribute with the given identifier in the
	* attribute list
	*/
	public int getAttrIndex(String attrId);

	/**
	* Returns the identifier of the attribute with the given index in the
	* attribute list
	*/
	public String getAttributeId(int attrN);

	/**
	* Returns the name of the attribute with the given index in the
	* attribute list
	*/
	public String getAttributeName(int attrN);

	/**
	* Returns the type of the specified attribute
	*/
	public char getAttrType(String attrId);

	/**
	* Returns the type of the specified attribute
	*/
	public char getAttrType(int attrN);

	/**
	* Returns the vector of all attribute values
	*/
	public Vector getAttrValues();

	/**
	* Returns the value of the attribute with the given identifier
	*/
	public Object getAttrValue(String attrId);

	/**
	* Returns the value of the attribute with the given number (index in the list)
	*/
	public Object getAttrValue(int attrN);

	/**
	* Returns a string representation of the value of the attribute with the
	* given identifier
	*/
	public String getAttrValueAsString(String attrId);

	/**
	* Returns a string representation of the value of the attribute with the
	* given number (index in the list)
	*/
	public String getAttrValueAsString(int attrN);

	/**
	* Assuming that the attribute is numeric, returns its value as a double number
	* (to avoid transformation from strings to numbers in each place where
	* numbers are needed). If the attribute is not of a numeric type, returns
	* Double.NaN.
	*/
	public double getNumericAttrValue(String attrId);

	public double getNumericAttrValue(int attrN);

	/**
	 * Informs whether this data item contains no actual values but only nulls.
	 */
	public boolean isEmpty();
}