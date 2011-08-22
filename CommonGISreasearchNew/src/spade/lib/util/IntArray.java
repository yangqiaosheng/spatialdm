package spade.lib.util;

public class IntArray implements java.io.Serializable {
	protected int numbers[] = null;
	protected int incr = 10, nelements = 0;

	public IntArray() {
		initialize(10, 10);
	}

	public IntArray(int initialCapacity, int increment) {
		initialize(initialCapacity, increment);
	}

	public void initialize(int initialCapacity, int increment) {
		incr = increment;
		nelements = 0;
		if (initialCapacity < 5) {
			initialCapacity = 5;
		}
		if (incr < 1) {
			incr = 1;
		}
		numbers = new int[initialCapacity];
	}

	public int size() {
		return nelements;
	}

	protected void increaseCapacity() {
		int capacity = numbers.length + incr;
		int ext[] = new int[capacity];
		if (nelements > numbers.length) {
			nelements = numbers.length;
		}
		for (int i = 0; i < nelements; i++) {
			ext[i] = numbers[i];
		}
		numbers = ext;
	}

	public void addElement(int element) {
		if (nelements + 1 > numbers.length) {
			increaseCapacity();
		}
		numbers[nelements++] = element;
	}

	public void insertElementAt(int element, int idx) {
		if (idx > nelements - 1) {
			addElement(element);
			return;
		}
		if (nelements + 1 > numbers.length) {
			increaseCapacity();
		}
		for (int i = nelements - 1; i >= idx; i--) {
			numbers[i + 1] = numbers[i];
		}
		numbers[idx] = element;
		++nelements;
	}

	public void removeElementAt(int idx) {
		if (idx >= nelements)
			return;
		for (int i = idx; i < nelements - 1; i++) {
			numbers[i] = numbers[i + 1];
		}
		--nelements;
	}

	public void removeAllElements() {
		nelements = 0;
	}

	public int elementAt(int idx) {
		if (idx < 0 || idx >= nelements)
			return Integer.MIN_VALUE;
		return numbers[idx];
	}

	public int indexOf(int number) {
		for (int i = 0; i < nelements; i++)
			if (number == numbers[i])
				return i;
		return -1;
	}

	public void setElementAt(int val, int idx) {
		if (idx < 0 || idx >= nelements)
			return;
		numbers[idx] = val;
	}

	public int[] getArray() {
		return numbers;
	}

	public int[] getTrimmedArray() {
		if (nelements < 1)
			return null;
		if (nelements == numbers.length)
			return numbers;
		int res[] = new int[nelements];
		for (int i = 0; i < nelements; i++) {
			res[i] = numbers[i];
		}
		return res;
	}

	@Override
	public Object clone() {
		int count = nelements;
		if (numbers != null && count < numbers.length) {
			count = numbers.length;
		}
		if (count == 0) {
			count = 10;
		}
		IntArray copy = new IntArray(count, incr);
		for (int i = 0; i < nelements; i++) {
			copy.addElement(numbers[i]);
		}
		return copy;
	}

	/**
	 * Returns true if this IntArray contains all elements of the given IntArray.
	 * If the given IntArray is null or empty, returns true.
	 */
	public boolean contains(IntArray anotherArray) {
		if (anotherArray == null || anotherArray.size() < 1)
			return true;
		if (nelements < 1)
			return false;
		for (int i = 0; i < anotherArray.size(); i++)
			if (indexOf(anotherArray.elementAt(i)) < 0)
				return false;
		return true;
	}

	/**
	* Checks whether the two vectors contain the same elements
	*/
	public static boolean containSameElements(IntArray v1, IntArray v2) {
		if (v1 == null)
			return v2 == null;
		if (v2 == null)
			return false;
		for (int i = 0; i < v1.size(); i++) {
			int idx = v2.indexOf(v1.elementAt(i));
			if (idx < 0)
				return false;
		}
		for (int i = 0; i < v2.size(); i++) {
			int idx = v1.indexOf(v2.elementAt(i));
			if (idx < 0)
				return false;
		}
		return true;
	}

	/**
	 * Returns the index of the given integer in the given integer array or -1 if not found
	 */
	public static int indexOf(int num, int numArray[]) {
		if (numArray == null)
			return -1;
		for (int i = 0; i < numArray.length; i++)
			if (num == numArray[i])
				return i;
		return -1;
	}
}