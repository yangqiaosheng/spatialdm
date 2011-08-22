package spade.lib.util;

import java.util.Vector;

/*  C.A.R. Hoare's QuickSort Method
**  Original on Java written by J.Gosling, K.Smith,  29/02/1996
**  Adaptation made by P.G., N.&G. A.
**  The Algorithm sorts arrays by default in ascending order.
**  Extended by hdz for Integers and sort_as_number

**  Usage:
**  if needed descending sort order set reverseSorting=true;
**  QSortAlgorithm.sort(your_array[]);
**
**/

public class QSortAlgorithm {

	public static void sort(String a[], boolean reverseSorting) {
		QuickSort(a, 0, a.length - 1);
		if (reverseSorting) {
			reverse(a);
		}
	}

	public static void sort(float a[], boolean reverseSorting) {
		QuickSort(a, 0, a.length - 1);
		if (reverseSorting) {
			reverse(a);
		}
	}

	public static void sort(double a[], boolean reverseSorting) {
		QuickSort(a, 0, a.length - 1);
		if (reverseSorting) {
			reverse(a);
		}
	}

	public static void sort(int a[], boolean reverseSorting) {
		QuickSort(a, 0, a.length - 1);
		if (reverseSorting) {
			reverse(a);
		}
	}

	public static void sort(Comparable a[], boolean reverseSorting) {
		QuickSort(a, 0, a.length - 1);
		if (reverseSorting) {
			reverse(a);
		}
	}

	public static void sort(float a[][], boolean reverseSorting, int comparing) {
		QuickSort(a, 0, a.length - 1, comparing);
		if (reverseSorting) {
			reverse(a);
		}
	}

	public static void sort(double a[][], boolean reverseSorting, int comparing) {
		QuickSort(a, 0, a.length - 1, comparing);
		if (reverseSorting) {
			reverse(a);
		}
	}

	//hdz
	public static void sort(Integer a[], boolean reverseSorting) {
		QuickSort(a, 0, a.length - 1);
		if (reverseSorting) {
			reverse(a);
		}
	}

	public static void sort_as_number(String a[], boolean reverseSorting) {
		QuickSort_as_number(a, 0, a.length - 1);
		if (reverseSorting) {
			reverse(a);
		}
	}

	/**
	* Sorts a vector of Strings or Comparable objects. If the vector contains
	* nulls, moves them to the end.
	*/
	public static void sort(Vector sorted, boolean reverseSorting) {
		if (sorted == null || sorted.size() < 2)
			return;
		int nNuls = 0;
		for (int i = sorted.size() - 1; i >= 0; i--)
			if (sorted.elementAt(i) == null) {
				++nNuls;
				sorted.removeElementAt(i);
			}
		if (sorted.size() > 1) {
			QuickSort(sorted, 0, sorted.size() - 1);
		}
		if (reverseSorting) {
			reverse(sorted);
		}
		for (int i = 0; i < nNuls; i++) {
			sorted.addElement(null);
		}
	}

	/**
	 * Sorts a vector of arbitrary objects. The pairwise comparisons are performed
	 * by an externam object comparator, who must take care about possible nulls in
	 * the vector.
	 */
	public static void sort(Vector sorted, Comparator comparator, boolean reverseSorting) {
		if (sorted == null || sorted.size() < 2)
			return;
		QuickSort(sorted, 0, sorted.size() - 1, comparator);
		if (reverseSorting) {
			reverse(sorted);
		}
	}

	public static void sort(String a[]) {
		sort(a, false);
	}

	public static void sort(float a[]) {
		sort(a, false);
	}

	public static void sort(double a[]) {
		sort(a, false);
	}

	public static void sort(int a[]) {
		sort(a, false);
	}

	public static void sort(Comparable a[]) {
		sort(a, false);
	}

	public static void sort(float a[][], int comparing) {
		sort(a, false, comparing);
	}

	public static void sort(double a[][], int comparing) {
		sort(a, false, comparing);
	}

	//hdz
	public static void sort(Integer a[]) {
		sort(a, false);
	}

	public static void sort_as_number(String a[]) {
		sort_as_number(a, false);
	}

	/**
	* Sorts a vector of Comparable objects. If the vector contains nulls, moves
	* them to the end.
	*/
	public static void sort(Vector sorted) {
		sort(sorted, false);
	}

	public static void QuickSort(String a[], int lo0, int hi0) {
		int lo = lo0;
		int hi = hi0;
		if (hi0 > lo0) {
			int midPos = (lo0 + hi0) / 2;
			while (lo <= midPos && hi >= midPos) {
				String middle = a[midPos];
				while (lo < midPos && a[lo].compareTo(middle) < 0) {
					++lo;
				}
				while (hi > midPos && a[hi].compareTo(middle) > 0) {
					--hi;
				}
				swap(a, lo, hi);
				++lo;
				--hi;
				if (lo - 1 == midPos) {
					midPos = hi = hi + 1;
				} else if (hi + 1 == midPos) {
					midPos = lo = lo - 1;
				}
			}
			if (lo0 < midPos - 1) {
				QuickSort(a, lo0, midPos - 1);
			}
			if (midPos + 1 < hi0) {
				QuickSort(a, midPos + 1, hi0);
			}
		}
	}

	public static void QuickSort(int a[], int lo0, int hi0) {
		int lo = lo0;
		int hi = hi0;
		if (hi0 > lo0) {
			int midPos = (lo0 + hi0) / 2;
			while (lo <= midPos && hi >= midPos) {
				int middle = a[midPos];
				while (lo < midPos && a[lo] < middle) {
					++lo;
				}
				while (hi > midPos && a[hi] > middle) {
					--hi;
				}
				swap(a, lo, hi);
				++lo;
				--hi;
				if (lo - 1 == midPos) {
					midPos = hi = hi + 1;
				} else if (hi + 1 == midPos) {
					midPos = lo = lo - 1;
				}
			}
			if (lo0 < midPos - 1) {
				QuickSort(a, lo0, midPos - 1);
			}
			if (midPos + 1 < hi0) {
				QuickSort(a, midPos + 1, hi0);
			}
		}
	}

	public static void QuickSort(float a[], int lo0, int hi0) {
		int lo = lo0;
		int hi = hi0;
		if (hi0 > lo0) {
			int midPos = (lo0 + hi0) / 2;
			while (lo <= midPos && hi >= midPos) {
				float middle = a[midPos];
				while (lo < midPos && a[lo] < middle) {
					++lo;
				}
				while (hi > midPos && a[hi] > middle) {
					--hi;
				}
				swap(a, lo, hi);
				++lo;
				--hi;
				if (lo - 1 == midPos) {
					midPos = hi = hi + 1;
				} else if (hi + 1 == midPos) {
					midPos = lo = lo - 1;
				}
			}
			if (lo0 < midPos - 1) {
				QuickSort(a, lo0, midPos - 1);
			}
			if (midPos + 1 < hi0) {
				QuickSort(a, midPos + 1, hi0);
			}
		}
	}

	public static void QuickSort(float a[][], int lo0, int hi0, int comparing) {
		int lo = lo0;
		int hi = hi0;
		if (hi0 > lo0) {
			int midPos = (lo0 + hi0) / 2;
			while (lo <= midPos && hi >= midPos) {
				float middle = a[midPos][comparing];
				while (lo < midPos && a[lo][comparing] < middle) {
					++lo;
				}
				while (hi > midPos && a[hi][comparing] > middle) {
					--hi;
				}
				swap(a, lo, hi);
				++lo;
				--hi;
				if (lo - 1 == midPos) {
					midPos = hi = hi + 1;
				} else if (hi + 1 == midPos) {
					midPos = lo = lo - 1;
				}
			}
			if (lo0 < midPos - 1) {
				QuickSort(a, lo0, midPos - 1, comparing);
			}
			if (midPos + 1 < hi0) {
				QuickSort(a, midPos + 1, hi0, comparing);
			}
		}
	}

	public static void QuickSort(double a[], int lo0, int hi0) {
		int lo = lo0;
		int hi = hi0;
		if (hi0 > lo0) {
			int midPos = (lo0 + hi0) / 2;
			while (lo <= midPos && hi >= midPos) {
				double middle = a[midPos];
				while (lo < midPos && a[lo] < middle) {
					++lo;
				}
				while (hi > midPos && a[hi] > middle) {
					--hi;
				}
				swap(a, lo, hi);
				++lo;
				--hi;
				if (lo - 1 == midPos) {
					midPos = hi = hi + 1;
				} else if (hi + 1 == midPos) {
					midPos = lo = lo - 1;
				}
			}
			if (lo0 < midPos - 1) {
				QuickSort(a, lo0, midPos - 1);
			}
			if (midPos + 1 < hi0) {
				QuickSort(a, midPos + 1, hi0);
			}
		}
	}

	public static void QuickSort(double a[][], int lo0, int hi0, int comparing) {
		int lo = lo0;
		int hi = hi0;
		if (hi0 > lo0) {
			int midPos = (lo0 + hi0) / 2;
			while (lo <= midPos && hi >= midPos) {
				double middle = a[midPos][comparing];
				while (lo < midPos && a[lo][comparing] < middle) {
					++lo;
				}
				while (hi > midPos && a[hi][comparing] > middle) {
					--hi;
				}
				swap(a, lo, hi);
				++lo;
				--hi;
				if (lo - 1 == midPos) {
					midPos = hi = hi + 1;
				} else if (hi + 1 == midPos) {
					midPos = lo = lo - 1;
				}
			}
			if (lo0 < midPos - 1) {
				QuickSort(a, lo0, midPos - 1, comparing);
			}
			if (midPos + 1 < hi0) {
				QuickSort(a, midPos + 1, hi0, comparing);
			}
		}
	}

	public static void QuickSort(Comparable a[], int lo0, int hi0) {
		int lo = lo0;
		int hi = hi0;
		if (hi0 > lo0) {
			int midPos = (lo0 + hi0) / 2;
			while (lo <= midPos && hi >= midPos) {
				Comparable middle = a[midPos];
				while (lo < midPos && a[lo].compareTo(middle) < 0) {
					++lo;
				}
				while (hi > midPos && a[hi].compareTo(middle) > 0) {
					--hi;
				}
				swap(a, lo, hi);
				++lo;
				--hi;
				if (lo - 1 == midPos) {
					midPos = hi = hi + 1;
				} else if (hi + 1 == midPos) {
					midPos = lo = lo - 1;
				}
			}
			if (lo0 < midPos - 1) {
				QuickSort(a, lo0, midPos - 1);
			}
			if (midPos + 1 < hi0) {
				QuickSort(a, midPos + 1, hi0);
			}
		}
	}

	/**
	* Sorts a vector of Strings or Comparable objects. The vector MUST NOT
	* contain nulls!
	*/
	public static void QuickSort(Vector sorted, int lo0, int hi0) {
		int lo = lo0;
		int hi = hi0;
		if (hi0 > lo0) {
			int midPos = (lo0 + hi0) / 2;
			while (lo <= midPos && hi >= midPos) {
				Object middle = sorted.elementAt(midPos);
				if (middle instanceof Comparable) {
					while (lo < midPos && ((Comparable) sorted.elementAt(lo)).compareTo((Comparable) middle) < 0) {
						++lo;
					}
					while (hi > midPos && ((Comparable) sorted.elementAt(hi)).compareTo((Comparable) middle) > 0) {
						--hi;
					}
				} else if (middle instanceof String) {
					while (lo < midPos && ((String) sorted.elementAt(lo)).compareTo((String) middle) < 0) {
						++lo;
					}
					while (hi > midPos && ((String) sorted.elementAt(hi)).compareTo((String) middle) > 0) {
						--hi;
					}
				} else
					return;
				swap(sorted, lo, hi);
				++lo;
				--hi;
				if (lo - 1 == midPos) {
					midPos = hi = hi + 1;
				} else if (hi + 1 == midPos) {
					midPos = lo = lo - 1;
				}
			}
			if (lo0 < midPos - 1) {
				QuickSort(sorted, lo0, midPos - 1);
			}
			if (midPos + 1 < hi0) {
				QuickSort(sorted, midPos + 1, hi0);
			}
		}
	}

	//hdz
	public static void QuickSort(Integer a[], int lo0, int hi0) {
		int lo = lo0;
		int hi = hi0;
		if (hi0 > lo0) {
			int midPos = (lo0 + hi0) / 2;
			while (lo <= midPos && hi >= midPos) {
				Integer middle = a[midPos];
				while (lo < midPos && a[lo].compareTo(middle) < 0) {
					++lo;
				}
				while (hi > midPos && a[hi].compareTo(middle) > 0) {
					--hi;
				}
				swap(a, lo, hi);
				++lo;
				--hi;
				if (lo - 1 == midPos) {
					midPos = hi = hi + 1;
				} else if (hi + 1 == midPos) {
					midPos = lo = lo - 1;
				}
			}
			if (lo0 < midPos - 1) {
				QuickSort(a, lo0, midPos - 1);
			}
			if (midPos + 1 < hi0) {
				QuickSort(a, midPos + 1, hi0);
			}
		}
	}

	public static void QuickSort_as_number(String a[], int lo0, int hi0) {
		int lo = lo0;
		int hi = hi0;
		if (hi0 > lo0) {
			int midPos = (lo0 + hi0) / 2;
			try {
				while (lo <= midPos && hi >= midPos) {
					String middle = a[midPos];
					float midVal = Float.parseFloat(middle);
					while (lo < midPos && Float.parseFloat(a[lo]) < midVal) {
						++lo;
					}
					while (hi > midPos && Float.parseFloat(a[hi]) > midVal) {
						--hi;
					}
					swap(a, lo, hi);
					++lo;
					--hi;
					if (lo - 1 == midPos) {
						midPos = hi = hi + 1;
					} else if (hi + 1 == midPos) {
						midPos = lo = lo - 1;
					}
				}
			} catch (Exception ex) {
				return;
			}
			if (lo0 < midPos - 1) {
				QuickSort_as_number(a, lo0, midPos - 1);
			}
			if (midPos + 1 < hi0) {
				QuickSort_as_number(a, midPos + 1, hi0);
			}
		}
	}

	/**
	 * Sorts a vector of arbitrary objects. The comparison is done by the object comparator.
	 * The comparator must care about possible nulls in the vector.
	 */
	public static void QuickSort(Vector sorted, int lo0, int hi0, Comparator comparator) {
		int lo = lo0;
		int hi = hi0;
		if (hi0 > lo0) {
			int midPos = (lo0 + hi0) / 2;
			while (lo <= midPos && hi >= midPos) {
				Object middle = sorted.elementAt(midPos);
				while (lo < midPos && comparator.compare(sorted.elementAt(lo), middle) < 0) {
					++lo;
				}
				while (hi > midPos && comparator.compare(sorted.elementAt(hi), middle) > 0) {
					--hi;
				}
				swap(sorted, lo, hi);
				++lo;
				--hi;
				if (lo - 1 == midPos) {
					midPos = hi = hi + 1;
				} else if (hi + 1 == midPos) {
					midPos = lo = lo - 1;
				}
			}
			if (lo0 < midPos - 1) {
				QuickSort(sorted, lo0, midPos - 1, comparator);
			}
			if (midPos + 1 < hi0) {
				QuickSort(sorted, midPos + 1, hi0, comparator);
			}
		}
	}

	public static int compare(Comparable a1, Comparable a2) {
		return a1.compareTo(a2);
	}

	private static void swap(Vector sorted, int i, int j) {
		if (i == j)
			return;
		Object tmp = sorted.elementAt(i);
		sorted.setElementAt(sorted.elementAt(j), i);
		sorted.setElementAt(tmp, j);
	}

	private static void swap(Comparable a[], int i, int j) {
		if (i == j)
			return;
		Comparable tmp;
		tmp = a[i];
		a[i] = a[j];
		a[j] = tmp;
	}

	private static void swap(String a[], int i, int j) {
		if (i == j)
			return;
		String tmp;
		tmp = a[i];
		a[i] = a[j];
		a[j] = tmp;
	}

	private static void swap(float a[], int i, int j) {
		if (i == j)
			return;
		float tmp;
		tmp = a[i];
		a[i] = a[j];
		a[j] = tmp;
	}

	private static void swap(float a[][], int i, int j) {
		if (i == j)
			return;
		float tmp;
		int l = a[i].length;
		for (int o = 0; o < l; o++) {
			tmp = a[i][o];
			a[i][o] = a[j][o];
			a[j][o] = tmp;
		}
	}

	private static void swap(double a[], int i, int j) {
		if (i == j)
			return;
		double tmp;
		tmp = a[i];
		a[i] = a[j];
		a[j] = tmp;
	}

	private static void swap(double a[][], int i, int j) {
		if (i == j)
			return;
		double tmp;
		int l = a[i].length;
		for (int o = 0; o < l; o++) {
			tmp = a[i][o];
			a[i][o] = a[j][o];
			a[j][o] = tmp;
		}
	}

	private static void swap(int a[], int i, int j) {
		if (i == j)
			return;
		int tmp;
		tmp = a[i];
		a[i] = a[j];
		a[j] = tmp;
	}

	//hdz
	private static void swap(Integer a[], int i, int j) {
		if (i == j)
			return;
		Integer tmp;
		tmp = a[i];
		a[i] = a[j];
		a[j] = tmp;
	}

	private static void reverse(Comparable a[]) {
		int size = a.length;
		for (int i = 0; i < size / 2; i++) {
			swap(a, i, size - i - 1);
		}
	}

	private static void reverse(Vector sorted) {
		int size = sorted.size();
		for (int i = 0; i < size / 2; i++) {
			swap(sorted, i, size - i - 1);
		}
	}

	private static void reverse(double a[]) {
		int size = a.length;
		for (int i = 0; i < size / 2; i++) {
			swap(a, i, size - i - 1);
		}
	}

	private static void reverse(float a[]) {
		int size = a.length;
		for (int i = 0; i < size / 2; i++) {
			swap(a, i, size - i - 1);
		}
	}

	private static void reverse(float a[][]) {
		int size = a.length;
		for (int i = 0; i < size / 2; i++) {
			swap(a, i, size - i - 1);
		}
	}

	private static void reverse(double a[][]) {
		int size = a.length;
		for (int i = 0; i < size / 2; i++) {
			swap(a, i, size - i - 1);
		}
	}

	private static void reverse(int a[]) {
		int size = a.length;
		for (int i = 0; i < size / 2; i++) {
			swap(a, i, size - i - 1);
		}
	}

	private static void reverse(String a[]) {
		int size = a.length;
		for (int i = 0; i < size / 2; i++) {
			swap(a, i, size - i - 1);
		}
	}

	//hdz
	private static void reverse(Integer a[]) {
		int size = a.length;
		for (int i = 0; i < size / 2; i++) {
			swap(a, i, size - i - 1);
		}
	}

	/**
	* The following functions work with float and double arrays containing
	* NaN-values and handle them carefully. NaN is treated as value that is
	* lower than all others in the array.
	*/
	public static float[] handleNaN(float a[]) {
		return handleNaN(a, false);
	}

	public static double[] handleNaN(double a[]) {
		return handleNaN(a, false);
	}

	public static float[] handleNaN(float a[], boolean reverseSorting) {
		float[] b = new float[a.length];
		int j = -1, startIdx = 0;
		for (float element : a)
			if (Float.isNaN(element)) {
				b[++j] = element;
			}
		startIdx = j + 1;
		for (int i = 0; i < a.length; i++)
			if (!Float.isNaN(a[i])) {
				b[++j] = a[i];
			}
		QuickSort(b, startIdx, b.length - 1);
		if (reverseSorting) {
			reverse(b);
		}
		return b;
	}

	public static double[] handleNaN(double a[], boolean reverseSorting) {
		double[] b = new double[a.length];
		int j = -1, startIdx = 0;
		for (double element : a)
			if (Double.isNaN(element)) {
				b[++j] = element;
			}
		startIdx = j + 1;
		for (int i = 0; i < a.length; i++)
			if (!Double.isNaN(a[i])) {
				b[++j] = a[i];
			}
		QuickSort(b, startIdx, b.length - 1);
		if (reverseSorting) {
			reverse(b);
		}
		return b;
	}
}
