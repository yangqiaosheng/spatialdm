package spade.lib.util;

import java.util.Vector;

import spade.time.TimeMoment;

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: 18.01.2007
 * Time: 16:25:12
 * Used for storing frequencies of any items (typically string values)
 */
public class Frequencies {
	/**
	 * Methods of ordering
	 */
	public static final int order_alphabet = 1, order_as_numbers_ascend = 2, order_as_numbers_descend = 3, order_by_frequency_ascend = 4, order_by_frequency_descend = 5, order_first = 1, order_last = 5;
	/**
	 * The items (e.g. attribute values). Nulls are not allowed.
	 */
	public Vector items = null;
	/**
	 * The frequencies of these items
	 */
	public IntArray frequencies = null;
	/**
	 * Indicates that the items are supposed to be strings; by default true
	 */
	public boolean itemsAreStrings = true;

	/**
	 * Initialises its internal vectors with the given initial capacity and
	 * increment
	 */
	public void init(int capacity, int increment) {
		items = new Vector(capacity, increment);
		frequencies = new IntArray(capacity, increment);
	}

	/**
	 * Adds an item with the given frequency. Checks if the item
	 * already occurs in the vector. If so, increments its frequency
	 * by the given number.
	 */
	public void addItem(Object item, int frequency) {
		if (item == null)
			return;
		if (items == null) {
			init(100, 100);
		}
		int idx = -1;
		if (itemsAreStrings) {
			idx = StringUtil.indexOfStringInVectorIgnoreCase(item.toString(), items);
		} else {
			idx = items.indexOf(item);
		}
		if (idx < 0) {
			items.addElement((itemsAreStrings) ? item.toString() : item);
			frequencies.addElement(frequency);
		} else {
			int fr = frequencies.elementAt(idx) + frequency;
			frequencies.setElementAt(fr, idx);
		}
	}

	/**
	 * Increments the frequency of the given item by one. If the item is not
	 * yet in the vector, it is added, and the frequeny is set to 1
	 */
	public void incrementFrequency(Object item) {
		addItem(item, 1);
	}

	/**
	 * Returns the number of items in the vector
	 */
	public int getItemCount() {
		if (items == null)
			return 0;
		return items.size();
	}

	/**
	 * Returns the item with the given index
	 */
	public Object getItem(int idx) {
		if (items == null || idx < 0 || idx >= items.size())
			return null;
		return items.elementAt(idx);
	}

	public String getItemAsString(int idx) {
		Object item = getItem(idx);
		if (item == null)
			return null;
		return item.toString();
	}

	/**
	 * Returns the frequency of the item with the given index
	 */
	public int getFrequency(int idx) {
		if (frequencies == null || idx < 0 || idx >= frequencies.size())
			return -1; //indicates an error
		return frequencies.elementAt(idx);
	}

	/**
	 * Returns the frequency of the given item
	 */
	public int getFrequency(Object item) {
		if (item == null || items == null)
			return 0;
		int idx = -1;
		if (itemsAreStrings) {
			idx = StringUtil.indexOfStringInVectorIgnoreCase(item.toString(), items);
		} else {
			idx = items.indexOf(item);
		}
		if (idx < 0)
			return 0;
		return frequencies.elementAt(idx);
	}

	public void trimToSize() {
		if (items != null) {
			items.trimToSize();
		}
	}

	/**
	 * Tries to transform the given item to a number, depending on its type
	 */
	public float getNumber(Object item) {
		if (item == null)
			return Float.NaN;
		if (item instanceof Float)
			return ((Float) item).floatValue();
		if (item instanceof Double)
			return ((Double) item).floatValue();
		if (item instanceof Integer)
			return ((Integer) item).floatValue();
		if (item instanceof Long)
			return ((Long) item).floatValue();
		if (item instanceof TimeMoment)
			return ((TimeMoment) item).toNumber();
		String str = item.toString();
		try {
			float f = Float.valueOf(str).floatValue();
			return f;
		} catch (NumberFormatException e) {
		}
		return Float.NaN;
	}

	/**
	 * Compares two items according to the specified ordering method. Returns 0
	 * if the items are equivalent, -1 if the first item precedes the second one,
	 * and 1 if the second item precedes the first one
	 */
	public int compareItems(int idx1, int idx2, int orderingMethod) {
		if (orderingMethod < order_first || orderingMethod > order_last)
			return 0;
		if (items == null || idx1 < 0 || idx1 >= items.size() || idx2 < 0 || idx2 >= items.size())
			return 0;
		if (idx1 == idx2)
			return 0;
		switch (orderingMethod) {
		case order_alphabet:
			String str1 = getItemAsString(idx1),
			str2 = getItemAsString(idx2);
			if (str1 == null)
				return 1;
			if (str2 == null)
				return -1;
			return str1.compareTo(str2);
		case order_as_numbers_ascend:
		case order_as_numbers_descend:
			float num1 = getNumber(items.elementAt(idx1)),
			num2 = getNumber(items.elementAt(idx2));
			if (Float.isNaN(num1))
				if (Float.isNaN(num2))
					return 0;
				else
					return 1;
			if (Float.isNaN(num2))
				return -1;
			if (num1 < num2)
				if (orderingMethod == order_as_numbers_ascend)
					return -1;
				else
					return 1;
			if (num1 > num2)
				if (orderingMethod == order_as_numbers_ascend)
					return 1;
				else
					return -1;
			return 0;
		case order_by_frequency_ascend:
		case order_by_frequency_descend:
			int fr1 = getFrequency(idx1),
			fr2 = getFrequency(idx2);
			if (fr1 < fr2)
				if (orderingMethod == order_by_frequency_ascend)
					return -1;
				else
					return 1;
			if (fr1 > fr2)
				if (orderingMethod == order_by_frequency_descend)
					return 1;
				else
					return -1;
			return 0;
		}
		return 0;
	}

	/**
	 * Sorts the items and the corresponding frequencies according to the
	 * specified ordering method
	 */
	public void sortItems(int orderingMethod) {
		if (items == null || items.size() < 2)
			return;
		if (orderingMethod < order_first || orderingMethod > order_last)
			return;
		for (int i = 1; i < items.size(); i++) {
			if (compareItems(i - 1, i, orderingMethod) > 0) {
				int idx = i - 1;
				for (int j = i - 2; j >= 0; j--)
					if (compareItems(j, i, orderingMethod) > 0) {
						idx = j;
					} else {
						break;
					}
				Object item = items.elementAt(i);
				items.removeElementAt(i);
				items.insertElementAt(item, idx);
				int freq = frequencies.elementAt(i);
				frequencies.removeElementAt(i);
				frequencies.insertElementAt(freq, idx);
			}
		}
	}
}
