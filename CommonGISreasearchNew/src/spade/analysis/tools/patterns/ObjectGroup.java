package spade.analysis.tools.patterns;

import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Jan 30, 2009
 * Time: 11:22:45 AM
 * Represents a group of objects, in particular, geographical objects from some
 * layer.
 */
public class ObjectGroup {
	/**
	 * The identifier of the group
	 */
	public String id = null;
	/**
	 * The identifiers of the member objects
	 */
	public Vector<String> objIds = null;
}
