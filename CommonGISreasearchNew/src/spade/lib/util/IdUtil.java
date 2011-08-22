package spade.lib.util;

import java.util.Vector;

/**
* Transforms "extended" attribute identifiers into "simple".
* An extended attribute identifier includes the identifier of the table the
* attribute belongs to.
*/

public class IdUtil {
	/**
	* Attaches the table identifier to the given attribute identifier.
	* The resulting string is
	* ##<table-id>##<attr-id>
	*/
	static public String makeUniqueAttrId(String attrId, String tableId) {
		if (attrId == null)
			return null;
		if (tableId == null || tableId.length() < 1)
			return attrId;
		return "##" + tableId + "##" + getPureAttrId(attrId);
	}

	/**
	* Extracts the table identifier from the given "extended" attribute identifier.
	* May return null if the attribute identifier does not contain a table
	* identifier.
	*/
	static public String getTableId(String attrId) {
		if (attrId == null)
			return null;
		if (!attrId.startsWith("##"))
			return null;
		int idx = attrId.indexOf("##", 2);
		if (idx < 2)
			return null;
		return attrId.substring(2, idx);
	}

	/**
	* Removes the table identifier from the given attribute identifier.
	*/
	static public String getPureAttrId(String attrId) {
		if (attrId == null)
			return null;
		if (!attrId.startsWith("##"))
			return attrId;
		int idx = attrId.indexOf("##", 2);
		if (idx < 2)
			return attrId;
		return attrId.substring(idx + 2);
	}

	/**
	* Removes the table identifiers from all attribute identifiers contained
	* in the given vector. Returns another vector with "pure" identifiers.
	*/
	static public Vector getPureAttrIds(Vector attrs) {
		if (attrs == null || attrs.size() < 1)
			return attrs;
		Vector v = new Vector(attrs.size(), 1);
		for (int i = 0; i < attrs.size(); i++) {
			v.addElement(getPureAttrId((String) attrs.elementAt(i)));
		}
		return v;
	}
}