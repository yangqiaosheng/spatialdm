package spade.lib.util;

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: 12-Oct-2007
 * Time: 16:38:02
 * To change this template use File | Settings | File Templates.
 */
public class LongArray implements java.io.Serializable {
	protected long numbers[] = null;
	protected int incr = 10, nelements = 0;

	public LongArray() {
		initialize(10, 10);
	}

	public LongArray(int initialCapacity, int increment) {
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
		numbers = new long[initialCapacity];
	}

	public int size() {
		return nelements;
	}

	protected void increaseCapacity() {
		int capacity = numbers.length + incr;
		long ext[] = new long[capacity];
		if (nelements > numbers.length) {
			nelements = numbers.length;
		}
		for (int i = 0; i < nelements; i++) {
			ext[i] = numbers[i];
		}
		numbers = ext;
	}

	public void addElement(long element) {
		if (nelements + 1 > numbers.length) {
			increaseCapacity();
		}
		numbers[nelements++] = element;
	}

	public void insertElementAt(long element, int idx) {
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

	public long elementAt(int idx) {
		if (idx < 0 || idx >= nelements)
			return Long.MIN_VALUE;
		return numbers[idx];
	}

	public int indexOf(long number) {
		for (int i = 0; i < nelements; i++)
			if (number == numbers[i])
				return i;
		return -1;
	}

	public void setElementAt(long val, int idx) {
		if (idx < 0 || idx >= nelements)
			return;
		numbers[idx] = val;
	}

	public long[] getArray() {
		return numbers;
	}

	public long[] getTrimmedArray() {
		if (nelements < 1)
			return null;
		if (nelements == numbers.length)
			return numbers;
		long res[] = new long[nelements];
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
		LongArray copy = new LongArray(count, incr);
		for (int i = 0; i < nelements; i++) {
			copy.addElement(numbers[i]);
		}
		return copy;
	}

	/**
	 * Returns true if this IntArray contains all elements of the given IntArray.
	 * If the given IntArray is null or empty, returns true.
	 */
	public boolean contains(LongArray anotherArray) {
		if (anotherArray == null || anotherArray.size() < 1)
			return true;
		if (nelements < 1)
			return false;
		for (int i = 0; i < anotherArray.size(); i++)
			if (indexOf(anotherArray.elementAt(i)) < 0)
				return false;
		return true;
	}
}
