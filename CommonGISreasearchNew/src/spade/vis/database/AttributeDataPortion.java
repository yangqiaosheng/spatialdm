package spade.vis.database;

import java.util.Vector;

import spade.lib.util.IntArray;
import spade.lib.util.NumRange;
import spade.lib.util.NumStat;

/**
* A collection of data items (records) consisting of identifiers, names
* (optionally), and values of attributes.
* Each attribute has its unique identifier.
*/

public interface AttributeDataPortion extends DataPortion {
	/**
	* Returns the number of attributes.
	*/
	public int getAttrCount();

	/**
	* Returns its top-level attributes. This means that, if some attribute has a
	* parent attribute, the parent is included in the result rather than the
	* child attribute. The elements of the resulting vector are instances of
	* Attribute.
	*/
	public Vector getTopLevelAttributes();

	/**
	* Returns the descriptor of the attribute with the given index in the
	* attribute list
	*/
	public Attribute getAttribute(int n);

	/**
	* Returns the descriptor of the attribute with the given identifier.
	* This may be, in particular, a super-attribute, i.e. a parameter-dependent
	* attribute.
	*/
	public Attribute getAttribute(String attrId);

	/**
	* Returns the identifier of the attribute with the given index .
	*/
	public String getAttributeId(int n);

	/**
	* Returns the name of the attribute with the given index .
	*/
	public String getAttributeName(int n);

	/**
	* Returns the name of the attribute with the given identifier.
	*/
	public String getAttributeName(String attrId);

	/**
	* Returns the type of the attribute with the given index.
	*/
	public char getAttributeType(int n);

	/**
	* Returns the type of the attribute with the given identifier.
	*/
	public char getAttributeType(String attrId);

	/**
	* Returns true if the attribute with the given index is numeric.
	*/
	public boolean isAttributeNumeric(int n);

	/**
	* Returns true if the attribute with the given index is temporal.
	*/
	public boolean isAttributeTemporal(int n);

	/**
	* Returns the origin of the attribute, i.e. whether is was derived and,
	* if yes, how. The return value is one of the constants defined in
	* the class AttributeTypes (@see AttributeTypes)
	* Returns -1 if there is no such attribute.
	*/
	public int getAttributeOrigin(int n);

	/**
	* Removes the attribute with the given index
	*/
	public void removeAttribute(int n);

	/**
	* Removes the attribute with the given identifier
	*/
	public void removeAttribute(String attrId);

	/**
	* Returns the number of its parameters (a table may have attributes depending
	* on parameters).
	*/
	public int getParamCount();

	/**
	* Returns the parameter with the given index (a table may have attributes
	* depending on parameters).
	*/
	public Parameter getParameter(int idx);

	/**
	* Returns the parameter with the given name.
	*/
	public Parameter getParameter(String name);

	/**
	* Returns all parameters.
	*/
	public Vector getParameters();

	/**
	* Informs whether the table has at least one temporal parameter
	*/
	public boolean hasTemporalParameter();

	/**
	* Returns the temporal parameter, if exists.
	*/
	public Parameter getTemporalParameter();

	/**
	* Returns the index of the attribute with the given identifier.
	*/
	public int getAttrIndex(String attrId);

	/**
	* Returns an array with the indices of the attributes with the given identifiers.
	*/
	public int[] getAttrIndices(Vector attrIds);

	/**
	* Returns the index of the attribute with the given name.
	*/
	public int findAttrByName(String attrName);

	/**
	* Returns the value (as Object) of the attribute with the index attrN
	* from the data record with the number recN.
	*/
	public Object getAttrValue(int attrN, int recN);

	/**
	* Returns the value (in String format) of the attribute with the index attrN
	* from the data record with the number recN.
	*/
	public String getAttrValueAsString(int attrN, int recN);

	/**
	* Returns the value (in double format) of the attribute with the index attrN
	* from the data record with the number recN. If the attribute is non-numeric,
	* returns Double.NaN.
	*/
	public double getNumericAttrValue(int attrN, int recN);

	/**
	* For the given list of attribute identifiers, returns an array of numbers of
	* the relevant columns. If the attribute list contains super-attributes, all
	* its children are included.
	*/
	public IntArray getRelevantColumnNumbers(Vector attrIds);

	/**
	* Retrieves all (different and not null) values of the attribute with the
	* given identifier. If this is a super-attribute, processes all its children.
	*/
	public Vector getAllAttrValues(String attrId);

	/**
	* Retrieves all (different and not null) values of the attributes with the
	* specified identifiers.
	*/
	public Vector getAllAttrValues(Vector attrIds);

	/**
	* Retrieves all (different and not null) values in the columns with the
	* specified numbers.
	*/
	public Vector getAllValuesInColumns(IntArray colNs);

	/**
	* Retrieves all (different and not null) values of the attribute with the
	* given identifier. If this is a super-attribute, processes all its children.
	* If the values are not strings, transforms them into strings.
	*/
	public Vector getAllAttrValuesAsStrings(String attrId);

	/**
	* Retrieves all (different and not null) values of the attributes with the
	* specified identifiers. If the values are not strings, transforms them into
	* strings.
	*/
	public Vector getAllAttrValuesAsStrings(Vector attrIds);

	/**
	* Retrieves all (different and not null) values in the columns with the
	* specified numbers. If the values are not strings, transforms them into
	* strings.
	*/
	public Vector getAllValuesInColumnsAsStrings(IntArray colNs);

	/**
	* Retrieves K different and not null values of the attribute with the
	* given identifier. If this is a super-attribute, processes all its children.
	*/
	public Vector getKAttrValues(String attrId, int K);

	/**
	* Retrieves K different and not null values of the attributes with the
	* specified identifiers.
	*/
	public Vector getKAttrValues(Vector attrIds, int K);

	/**
	* Retrieves K different and not null values from the columns with the
	* specified numbers.
	*/
	public Vector getKValuesFromColumns(IntArray colNs, int K);

	/**
	* Retrieves K different and not null values of the attribute with the
	* given identifier. If this is a super-attribute, processes all its children.
	* If the values are not strings, transforms them into strings.
	*/
	public Vector getKAttrValuesAsStrings(String attrId, int K);

	/**
	* Retrieves K different and not null values of the attributes with the
	* specified identifiers. If the values are not strings, transforms them into
	* strings.
	*/
	public Vector getKAttrValuesAsStrings(Vector attrIds, int K);

	/**
	* Retrieves K different and not null values from the columns with the
	* specified numbers. If the values are not strings, transforms them into
	* strings.
	*/
	public Vector getKValuesFromColumnsAsStrings(IntArray colNs, int K);

	/**
	 * hdz, 2004.04.28
	 * Compares the Number of Values with a given Limit
	 * @param strId: Identifier for Attribut, maxCount: Limit to Compare with
	 * @return (boolean) true if valuesCount <= maxCount, else false
	 */
	public boolean isValuesCountBelow(Vector attributes, int maxCount);

	/**
	* Determines the value range of the numeric attribute with the given identifier.
	* If this is a super-attribute, processes all its children.
	*/
	public NumRange getAttrValueRange(String attrId);

	/**
	* Determines the common value range of the numeric attributes with the
	* specified identifiers.
	*/
	public NumRange getAttrValueRange(Vector attrIds);

	/**
	* Determines the common value range in the columns with the specified numbers.
	*/
	public NumRange getValueRangeInColumns(IntArray colNs);

	/**
	* Returns statistics for the numeric attribute specified through its column
	* index
	*/
	public NumStat getNumAttrStatistics(int attrIdx);

	/**
	* If the data actually have not been loaded in the table yet, this method
	* loads them. Returns true if data has been successfully loaded.
	*/
	public boolean loadData();

	/**
	* Constructs the descriptor of the attribute with the given name,
	* identifier (possibly, null) and
	* type and adds it to the list of attribute descriptors.
	*/
	public void addAttribute(String name, String identifier, char attrType);

	/**
	* Adds the attribute descriptor to the list of attribute descriptors
	*/
	public void addAttribute(Attribute attr);

	/**
	* Adds new columns to the table. Arguments:
	* objIds - identifiers of objects (array of strings);
	* attrNames - names of the attributes to add (array of strings);
	* attrTypes - types of the attributes (array of characters;
	*             see spade.vis.database.AttributeTypes);
	* values - values of the attributes (2D array; the first index corresponds
	*          to attributes and the second to the objects in the vector objIds).
	*          The values are specified as strings.
	* Returns true if the data have been successfully added, i.e. all objects
	* listed in the array objIds have been found in the table.
	*/
	public boolean addColumns(String objIds[], String attrNames[], char attrTypes[], String values[][]);

	/**
	* Adds new columns with numeric (double) values to the table. Arguments:
	* objIds - identifiers of objects (array of strings);
	* attrNames - names of the attributes to add (array of strings);
	* values - values of the attributes (2D array; the first index corresponds
	*          to attributes and the second to the objects in the vector objIds).
	*          The values are specified as double numbers.
	* Returns true if the data have been successfully added, i.e. all objects
	* listed in the array objIds have been found in the table.
	*/
	public boolean addNumericColumns(String objIds[], String attrNames[], double values[][]);

	/**
	* For the given list of column identifiers (not superattribute identifiers!)
	* checks whether the columns are children of the same parent. If not, returns
	* null. If yes, returns a vector of vectors: for each parameter by which
	* these columns differentiate the corresponding vector contains the parameter
	* name as its first element and the relevant parameter values following the
	* name.
	*/
	public Vector getDistinguishingParameters(Vector columnIds);

	/**
	* Reports if an object index is used. An index, if exists, may speed up the
	* search for data items by identifiers (method indexOf(String)).
	*/
	public boolean getUsesObjectIndex();

	/**
	* Returns true if there is at least one temporal attribute.
	*/
	public boolean hasTimeReferences();

	/**
	 * Returns the indexes of its temporal attributes, i.e. columns with temporal
	 * references.
	 */
	public IntArray getTimeRefColumnNs();
}
