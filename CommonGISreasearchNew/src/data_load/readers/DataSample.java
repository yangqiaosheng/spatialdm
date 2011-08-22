package data_load.readers;

import java.util.Vector;

import spade.lib.util.StringUtil;
import spade.vis.database.AttributeTypes;

/**
* Used to read field identifiers, types (when available), and some portion of
* data values for preview. Then the user can, for example, point, from what
* field to take object identifiers.
*/
public class DataSample {
	protected Vector fieldNames = null;
	protected Vector fieldTypes = null;
	/**
	* Vector of vectors each consisting of strings
	*/
	protected Vector data = null;

	public void setFieldNames(Vector names) {
		fieldNames = names;
	}

	public void setFieldTypes(Vector types) {
		fieldTypes = types;
	}

	public int getNFields() {
		if (fieldNames == null)
			return 0;
		return fieldNames.size();
	}

	public void setFieldName(String name, int fieldN) {
		if (fieldN < 0)
			return;
		for (int i = getNFields(); i < fieldN; i++) {
			addField("F" + (i + 1));
		}
		if (getNFields() > fieldN) {
			fieldNames.setElementAt(name, fieldN);
		} else {
			addField(name);
		}
	}

	public String getFieldName(int fieldN) {
		if (fieldNames == null || fieldN < 0 || fieldN >= fieldNames.size())
			return null;
		return (String) fieldNames.elementAt(fieldN);
	}

	public void addField(String name, char type) {
		if (fieldNames == null) {
			fieldNames = new Vector(20, 20);
		}
		fieldNames.addElement(name);
		addFieldType(type);
	}

	public void addField(String name) {
		addField(name, AttributeTypes.character);
	}

	public int getFieldN(String name) {
		if (name == null || fieldNames == null)
			return -1;
		return StringUtil.indexOfStringInVectorIgnoreCase(name, fieldNames);
	}

	public void addFieldType(char type) {
		if (type == 'N') {
			type = AttributeTypes.real; //'N' stands for "numeric"
		}
		if (!AttributeTypes.isValidType(type)) {
			type = AttributeTypes.character;
		}
		if (fieldTypes == null) {
			fieldTypes = new Vector(20, 20);
		}
		fieldTypes.addElement(new Character(type));
	}

	public void setFieldType(char type, int fieldN) {
		if (fieldN < 0)
			return;
		if (type == 'N') {
			type = AttributeTypes.real; //'N' stands for "numeric"
		}
		int size = 0;
		if (fieldTypes != null) {
			size = fieldTypes.size();
		}
		for (int i = size; i < fieldN; i++) {
			addFieldType(AttributeTypes.character);
		}
		if (fieldTypes != null) {
			size = fieldTypes.size();
		}
		if (size > fieldN)
			if (!AttributeTypes.isValidType(type)) {
				fieldTypes.setElementAt(new Character(type), fieldN);
			} else {
				;
			}
		else {
			addFieldType(type);
		}
	}

	public char getFieldType(int fieldN) {
		if (fieldN < 0 || fieldN >= getNFields())
			return 0;
		if (fieldTypes == null || fieldN >= fieldTypes.size())
			return AttributeTypes.character;
		Character ch = (Character) fieldTypes.elementAt(fieldN);
		return ch.charValue();
	}

	public void addDataRecord(Vector values) {
		if (values == null)
			return;
		if (data == null) {
			data = new Vector(20, 20);
		}
		data.addElement(values);
	}

	public int getNRecords() {
		if (data == null)
			return 0;
		return data.size();
	}

	public Vector getDataRecord(int recN) {
		if (recN < 0 || data == null || recN >= data.size())
			return null;
		return (Vector) data.elementAt(recN);
	}

	public String getValue(int recN, int fieldN) {
		if (fieldN < 0 || fieldN >= getNFields())
			return null;
		Vector values = getDataRecord(recN);
		if (values == null)
			return null;
		if (fieldN >= values.size())
			return null;
		return (String) values.elementAt(fieldN);
	}

	/**
	* Sometimes attribute types are not specified. By default, all attributes
	* are assumed to be of the character type. In this method the DataSample
	* looks through the available values of the attributes in order to
	* determine the actual type of each attribute. It can recornize numeric
	* attributes (all values are transformable into numbers) and logical
	* attributes (having values T and F). Missing values are ignored.
	*/
	public void determineAttributeTypes() {
		if (getNFields() < 1 || getNRecords() < 1)
			return;
		int nattr = getNFields();
		if (nattr < 1)
			return;
		char types[] = new char[nattr];
		for (int i = 0; i < nattr; i++) {
			types[i] = 0; //type is unknown
		}
		int nCharAttr = 0;
		for (int i = 0; i < getNRecords() && nCharAttr < nattr; i++) {
			Vector rec = getDataRecord(i);
			if (rec == null) {
				continue;
			}
			for (int j = 0; j < nattr; j++)
				if (types[j] != AttributeTypes.character) {
					String str = (String) rec.elementAt(j);
					if (str == null) {
						continue;
					}
					if (types[j] == 0 || types[j] == AttributeTypes.logical) {
						boolean isLogical = str.equalsIgnoreCase("T") || str.equalsIgnoreCase("F");
						if (isLogical)
							if (types[j] == 0) {
								types[j] = AttributeTypes.logical; //the unknown type becomes logical
							} else {
								; //the type remains logical
							}
						else if (types[j] == AttributeTypes.logical) {
							types[j] = AttributeTypes.character;
						} else {
							; //the type is still unknown
						}
					}
					if (types[j] == 0 || AttributeTypes.isNumericType(types[j])) {
						boolean isNumeric = false;
						try { //transform the string into a number
							float v = Float.valueOf(str).floatValue();
							isNumeric = !Float.isNaN(v);
						} catch (NumberFormatException nfe) {
						}
						if (!isNumeric) {
							types[j] = AttributeTypes.character;
						} else //determine whether the value is integer or real
						if (types[j] == 0 || types[j] == AttributeTypes.integer)
							//look whether there is a decimal separator
							if (str.indexOf('.') >= 0 || str.indexOf('E') >= 0 || str.indexOf('e') >= 0) {
								types[j] = AttributeTypes.real;
							} else {
								types[j] = AttributeTypes.integer;
							}
					}
					if (types[j] == AttributeTypes.character) {
						++nCharAttr;
					}
				}
		}
		for (int i = 0; i < nattr; i++) {
			if (types[i] == 0) {
				types[i] = AttributeTypes.character;
			}
			fieldTypes.setElementAt(new Character(types[i]), i);
		}
	}
}