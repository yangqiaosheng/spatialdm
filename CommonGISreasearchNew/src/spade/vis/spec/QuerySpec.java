package spade.vis.spec;

import java.util.Vector;

/**
* Used for saving and restoring query conditions.
*/
public class QuerySpec extends ToolSpec {
	/**
	* Query conditions. Elements of the vector must be instances of
	* spade.vis.spec.ConditionSpec.
	*/
	public Vector conditions = null;
}
