package spade.vis.database;

import java.util.Hashtable;

/**
* The interface for the classes implementing query conditions for filtering
* a table with thematic data.
*/
public interface Condition {
	/**
	* Sets the table, in which to check the condition satisfaction
	*/
	public void setTable(AttributeDataPortion table);

	/**
	* Replies whether any limit is set in this query condition
	*/
	public boolean hasLimit();

	/**
	* Clears the limits set earlier.
	*/
	public void clearLimits();

	/**
	* Informs whether missing values are currently treated as satisfying the query
	* (returns true) or not (returns false).
	*/
	public boolean getMissingValuesOK();

	/**
	* Sets whether missing values must be treated as satisfying the query
	* (argument is true) or not (argument is false).
	*/
	public void setMissingValuesOK(boolean ok);

	/**
	* Checks the satisfaction of this query condition by the given data item
	*/
	public boolean doesSatisfy(ThematicDataItem data);

	/**
	* Checks the satisfaction of this query condition by the table record (row)
	* with the given index.
	*/
	public boolean doesSatisfy(int rowN);

	/**
	* Checks whether the value in the condition-controlled column of the given
	* record is missing
	*/
	public boolean isValueMissing(int rowN);

	/**
	* Adjusts its internal settings, if necessary, when the data in the column
	* controlled by this condition change.
	*/
	public void adaptToDataChange();

	/**
	* Returns the type of this condition, e.g. "NumAttrCondition". The type must be
	* unique for each class implementing the interface spade.vis.database.Condition
	*/
	public String getConditionType();

	/**
	* Returns the description of the condition in a form of hashtable. The
	* hashtable consists of pairs <key, value>. It is required that both the keys
	* and the values are strings, because they are used for storing descriptions
	* of query conditions in an ASCII file.
	*/
	public Hashtable getDescription();

	/**
	* Setups the condition according to the description specified in a form of
	* hashtable. The hashtable consists of pairs <key, value>, where both the keys
	* and the values are strings.
	*/
	public void setup(Hashtable descr);
}
