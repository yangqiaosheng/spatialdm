package spade.lib.util;

import java.util.Vector;

public class VectorUtil {
	/**
	* The standard Vector.contains(Object element) fails when element==null
	* (produces NullPointerException). This method may be used to replace
	* the standard method when the element or the vector itself may be null.
	*/
	public static boolean contains(Vector v, Object element) {
		if (v == null || v.size() < 1)
			return false;
		if (element != null)
			return v.contains(element);
		for (int i = 0; i < v.size(); i++)
			if (v.elementAt(i) == null)
				return true;
		return false;
	}

	/**
	* The standard Vector.indexOf(Object element) fails when element==null
	* (produces NullPointerException). This method may be used to replace
	* the standard method when the element or the vector itself may be null.
	*/
	public static int indexOf(Vector v, Object element) {
		if (v == null || v.size() < 1)
			return -1;
		if (element != null)
			return v.indexOf(element);
		for (int i = 0; i < v.size(); i++)
			if (v.elementAt(i) == null)
				return i;
		return -1;
	}

	/**
	* Checks whether the two vectors contain the same elements
	*/
	public static boolean sameVectors(Vector v1, Vector v2) {
		if (v1 == null)
			return v2 == null;
		if (v2 == null)
			return false;
		if (v1.size() != v2.size())
			return false;
		for (int i = 0; i < v1.size(); i++) {
			int idx = v2.indexOf(v1.elementAt(i));
			if (idx < 0)
				return false;
			if (idx != i && !v1.contains(v2.elementAt(i)))
				return false;
		}
		return true;
	}

	/**
	* For the given array of non-empty vectors, returns all possible combinations
	* of their elements, where k-th element of each combination is taken from
	* the k-th vector. Returns a vector consisting of arrays of objects with the
	* dimensionality equal to the number of vectors in the source array.
	*/
	public static Vector getAllCombinations(Vector lists[]) {
		if (lists == null)
			return null;
		for (Vector list : lists)
			if (list == null || list.size() < 1)
				return null;
		return getCombinations(lists, 0);
	}

	/**
	* For the vectors starting in the given array from the index beginIndex,
	* returns all possible combinations of their elements.
	*/
	protected static Vector getCombinations(Vector lists[], int beginIndex) {
		if (lists == null || beginIndex < 0 || beginIndex >= lists.length)
			return null;
		if (lists[beginIndex] == null || lists[beginIndex].size() < 1)
			return null;
		if (beginIndex == lists.length - 1) {
			Vector result = new Vector(lists[beginIndex].size(), 1);
			for (int i = 0; i < lists[beginIndex].size(); i++) {
				Object single[] = new Object[1];
				single[0] = lists[beginIndex].elementAt(i);
				result.addElement(single);
			}
			return result;
		}
		Vector tails = getCombinations(lists, beginIndex + 1);
		if (tails == null || tails.size() < 1)
			return null;
		Vector result = new Vector(lists[beginIndex].size() * tails.size(), 1);
		for (int i = 0; i < lists[beginIndex].size(); i++) {
			Object elem = lists[beginIndex].elementAt(i);
			for (int j = 0; j < tails.size(); j++) {
				Object tail[] = (Object[]) tails.elementAt(j);
				Object tuple[] = new Object[tail.length + 1];
				tuple[0] = elem;
				for (int k = 0; k < tail.length; k++) {
					tuple[k + 1] = tail[k];
				}
				result.addElement(tuple);
			}
		}
		return result;
	}
}
