package spade.time.vis;

import java.util.Vector;

import spade.lib.util.IntArray;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Mar 15, 2010
 * Time: 4:20:53 PM
 * Contains information about a grouping attribute
 */
public class GroupAttrInfo {
	/**
	 * Id and name of the attribute
	 */
	public String id = null, name = null;
	/**
	 * The number of the table column with the values of this attribute
	 */
	public int colN = -1;
	/**
	 * The list of all values of this attribute
	 */
	public Vector values = null;
	/**
	 * For each value, the index of the first object with this value
	 */
	public IntArray objIdxs = null;
}
