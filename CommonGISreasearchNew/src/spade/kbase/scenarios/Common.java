package spade.kbase.scenarios;

/**
* Common constants and utilities used in different elements.
*/

public class Common {
	/**
	* Types of attributes distinguished in designing visualizations
	* Q - qualitative, L - logical, O - ordinal, 'T' - time, 'N' - numeric
	*/
	static public final char attrTypes[] = { 'Q', 'L', 'O', 'T', 'N' };
	/**
	* Possible relationships between attributes
	* U - unknown, I - incomparable, C - comparable, P - part of, S - summable
	* (attributes can be added together to form some total)
	*/
	static public final char attrRelations[] = { 'U', 'I', 'C', 'P', 'S' };

	/**
	* Recognizes the type of an attribute sent as a string and encodes it by
	* a character (see the array attrTypes above)
	* If the type is not recognized, returns 'U' - ubknown
	*/
	static public char encodeAttrType(String type) {
		if (type == null)
			return 'U';
		if (type.equalsIgnoreCase("logical") || type.equalsIgnoreCase("boolean"))
			return 'L';
		if (type.equalsIgnoreCase("qualitative") || type.equalsIgnoreCase("nominal") || type.equalsIgnoreCase("character"))
			return 'Q';
		if (type.equalsIgnoreCase("time") || type.equalsIgnoreCase("date"))
			return 'T';
		if (type.equalsIgnoreCase("ordinal"))
			return 'O';
		if (type.equalsIgnoreCase("numeric") || type.equalsIgnoreCase("real") || type.equalsIgnoreCase("integer"))
			return 'N';
		return 'U';
	}

	/**
	* Recognizes the type of an attribute sent as a character (one of
	* {'I','R','C','L','T'}, defined in spade.vis.database.AttributeTypes)
	* and encodes it by one of the characters defined in the array attrTypes.
	* If the type is not recognized, returns 'U' - ubknown
	*/
	static public char encodeAttrType(char type) {
		if (type == 'I' || type == 'R')
			return 'N'; //integer or real -> numeric
		if (type == 'C')
			return 'Q'; //character -> qualitative
		if (type == 'L' || type == 'T')
			return type;
		return 'U';
	}
}