package spade.lib.util;

import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 26-Jan-2006
 * Time: 15:02:42
 * Contains utilities for sorting that preserves as much as possible the
 * original order of the items.
 */
public class BubbleSort {
	/**
	 * Sorts a vector of strings or items implementing the interface Comparable
	 */
	public static void sort(Vector items) {
		if (items == null || items.size() < 2)
			return;
		for (int i = 1; i < items.size(); i++) {
			Object item = items.elementAt(i);
			if (compare(items.elementAt(i - 1), item) > 0) {
				int idx = i - 1;
				for (int j = i - 2; j >= 0; j--)
					if (compare(items.elementAt(j), item) > 0) {
						idx = j;
					} else {
						break;
					}
				items.removeElementAt(i);
				items.insertElementAt(item, idx);
			}
		}
	}

	/**
	 * Compares two items, which must either be strings or implement the interface
	 * Comparable. If this condition is not fulfilled, returns 0.
	 * Normally, returns 0 if equal, <0 if item1 is less than item2, >0 otherwise
	 */
	public static int compare(Object item1, Object item2) {
		if (item1 == null)
			if (item2 == null)
				return 0;
			else
				return 1;
		if (item2 == null)
			return -1;
		if ((item1 instanceof String) && (item2 instanceof String))
			return StringUtil.compareStrings((String) item1, (String) item2);
		if ((item1 instanceof Comparable) && (item2 instanceof Comparable)) {
			Comparable c1 = (Comparable) item1, c2 = (Comparable) item2;
			return c1.compareTo(c2);
		}
		if ((item1 instanceof Object[]) && (item2 instanceof Object[])) {
			Object ar1[] = (Object[]) item1, ar2[] = (Object[]) item2;
			if (ar1.length > 0 && ar2.length > 0)
				return compare(ar1[0], ar2[0]);
		}
		return 0;
	}

	/**
	 * Sorts a vector of items, which are compared by the specified Comparator
	 */
	public static void sort(Vector items, Comparator comparator) {
		if (items == null || items.size() < 2 || comparator == null)
			return;
		for (int i = 1; i < items.size(); i++) {
			Object item = items.elementAt(i);
			if (comparator.compare(items.elementAt(i - 1), item) > 0) {
				int idx = i - 1;
				for (int j = i - 2; j >= 0; j--)
					if (comparator.compare(items.elementAt(j), item) > 0) {
						idx = j;
					} else {
						break;
					}
				items.removeElementAt(i);
				items.insertElementAt(item, idx);
			}
		}
	}
}
