package spade.lib.util;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Nov 3, 2009
 * Time: 10:05:35 AM
 * Used for sorting objects when it is needed to remember their original
 * indexes.
 */
public class ObjectWithIndex implements Comparable {
	public Object obj = null;
	public int index = 0;

	public ObjectWithIndex() {
	}

	public ObjectWithIndex(Object obj, int ind) {
		this.obj = obj;
		index = ind;
	}

	/**
	*  Returns 0 if equal, <0 if THIS must come earlier than the argument, >0 otherwise
	*/
	@Override
	public int compareTo(Comparable c) {
		if (c == null || !(c instanceof ObjectWithIndex))
			return -1;
		ObjectWithIndex oInd = (ObjectWithIndex) c;
		if (obj == null)
			if (oInd.obj == null)
				return 0;
			else
				return 1;
		if (oInd.obj == null)
			return -1;
		if ((obj instanceof String) && (oInd.obj instanceof String)) {
			String st1 = (String) obj, st2 = (String) oInd.obj;
			return st1.compareTo(st2);
		}
		if ((obj instanceof Comparable) && (oInd.obj instanceof Comparable)) {
			Comparable c1 = (Comparable) obj, c2 = (Comparable) oInd.obj;
			return c1.compareTo(c2);
		}
		return 0;
	}
}
