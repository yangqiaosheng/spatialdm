package spade.lib.util;

import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Apr 16, 2009
 * Time: 3:55:21 PM
 * Used for counting instances of some objects.
 */
public class ObjectWithCount implements Comparable {
	public Object obj = null;
	public int count = 0;

	public ObjectWithCount() {
	}

	public ObjectWithCount(Object obj) {
		this.obj = obj;
	}

	public ObjectWithCount(Object obj, int count) {
		this.obj = obj;
		this.count = count;
	}

	public boolean hasObject(Object anObject) {
		return obj != null && anObject != null && obj.equals(anObject);
	}

	public void add() {
		++count;
	}

	public void add(int n) {
		count += n;
	}

	/**
	*  Returns 0 if equal, <0 if THIS must come earlier than the argument, >0 otherwise
	*/
	@Override
	public int compareTo(Comparable c) {
		if (c == null || !(c instanceof ObjectWithCount))
			return -1;
		ObjectWithCount oCount = (ObjectWithCount) c;
		if (count > oCount.count)
			return -1;
		if (count < oCount.count)
			return 1;
		return 0;
	}

	/**
	 * Finds the given object in the list of objects with counts and
	 * returns the index or -1 if not found
	 */
	public static int findObjectInList(Object objToFind, Vector<ObjectWithCount> objCounts) {
		if (objToFind == null || objCounts == null)
			return -1;
		for (int i = 0; i < objCounts.size(); i++)
			if (objToFind.equals(objCounts.elementAt(i).obj))
				return i;
		return -1;
	}
}
