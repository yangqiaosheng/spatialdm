package spade.vis.spec;

import java.util.Hashtable;

/**
* Used for saving and restoring query conditions.
*/
public class ConditionSpec {
	/**
	* Specifies the type of this condition, e.g. "NumAttrCondition". The type must
	* be unique for each class implementing a specific type of condition
	* (according to the interface spade.vis.database.Condition)
	*/
	public String type = null;
	/**
	* Contains the description of the condition in a form of hashtable. The
	* hashtable consists of pairs <key, value>. It is required that both the keys
	* and the values are strings, because they are used for storing descriptions
	* of query conditions in an ASCII file.
	*/
	public Hashtable description = null;
}
