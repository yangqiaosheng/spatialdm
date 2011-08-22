package spade.lib.util;

import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 24-Apr-2007
 * Time: 14:42:32
 * A sorted vector containing objects implementing the interface Comparable
 */
public class OrderedList extends Vector {
	/**
	 * Inserts an element in a proper place to preserve the order
	 */
	public void addElement(java.lang.Comparable c) {
		if (size() < 1) {
			super.addElement(c);
			return;
		}
		//binary search of a proper place to insert the new element
		int first = 0, last = size() - 1;
		while (first < last) {
			int idx = (first + last) / 2;
			int result = c.compareTo(elementAt(idx));
			if (result < 0) {
				last = idx - 1;
			} else if (result >= 0) {
				first = idx + 1;
			}
		}
		int result = c.compareTo(elementAt(first));
		if (result < 0) {
			insertElementAt(c, first);
		} else {
			insertElementAt(c, first + 1);
		}
	}

	/**
	 * Inserts an element in a proper place to preserve the order
	 */
	public void add(java.lang.Comparable c) {
		addElement(c);
	}
}
