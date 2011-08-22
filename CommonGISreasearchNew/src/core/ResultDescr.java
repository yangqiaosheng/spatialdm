package core;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Mar 10, 2009
 * Time: 11:32:01 AM
 * Describes a result of an analysis action
 */
public class ResultDescr {
	/**
	 * The object produced, e.g. map layer, table, attribute in a table, ...
	 */
	public Object product = null;
	/**
	 * The "owner" of the produced object, e.g. for a map layer - the layer manager,
	 * for a table attribute - the table, ...
	 */
	public Object owner = null;
	/**
	 * May explain the meaning and/or properties of the object
	 */
	public String comment = null;
}
