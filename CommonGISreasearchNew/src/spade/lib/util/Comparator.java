package spade.lib.util;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 09-Feb-2007
 * Time: 17:13:53
 */
public interface Comparator {
	/**
	 * Returns 0 if the objects are equal, <0 if the first object is less than the
	 * second one, >0 otherwise
	 */
	public int compare(Object obj1, Object obj2);
}
