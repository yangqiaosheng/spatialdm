package spade.vis.database;

/**
* The interface for the classes implementing query conditions for various types
* of attributes in an attribute-based filter: numeric, qualitative with a limited
* value set, qualitative with an unlimited value set, logical, etc. Each such
* condition refers to a single table column!
*/
public interface AttrCondition extends Condition {
	/**
	* Sets the index of the attribute (i.e. table column) the query condition
	* refers to.
	*/
	public void setAttributeIndex(int idx);

	/**
	* Returns the index of the attribute (i.e. table column) the query condition
	* refers to.
	*/
	public int getAttributeIndex();

	/**
	* Returns the identifier of the attribute (i.e. table column) the query
	* condition refers to.
	*/
	public String getAttributeId();

	/**
	* Returns the number of records satisfying the constraints on the given
	* attribute
	*/
	public int getNSatisfying();

	/**
	* Returns the number of records with missing values of the given
	* attribute
	*/
	public int getNMissingValues();

	/**
	 * Sets whether it is allowed to adjust the condition to the actual
	 * data values occurring in the table
	 */
	public void setAllowAdjust(boolean allowAdjust);
}