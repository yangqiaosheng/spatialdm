package spade.lib.util;

public class Arrays {

	private Arrays() {
	}

	// Equality Testing

	/**
	 * Returns <tt>true</tt> if the two specified arrays of longs are
	 * <i>equal</i> to one another.  Two arrays are considered equal if both
	 * arrays contain the same number of elements, and all corresponding pairs
	 * of elements in the two arrays are equal.  In other words, two arrays
	 * are equal if they contain the same elements in the same order.  Also,
	 * two array references are considered equal if both are <tt>null</tt>.<p>
	 *
	 * @param a one array to be tested for equality.
	 * @param a2 the other array to be tested for equality.
	 * @return <tt>true</tt> if the two arrays are equal.
	 */
	public static boolean equals(long[] a, long[] a2) {
		if (a == a2)
			return true;
		if (a == null || a2 == null)
			return false;

		int length = a.length;
		if (a2.length != length)
			return false;

		for (int i = 0; i < length; i++)
			if (a[i] != a2[i])
				return false;

		return true;
	}

	/**
	 * Returns <tt>true</tt> if the two specified arrays of ints are
	 * <i>equal</i> to one another.  Two arrays are considered equal if both
	 * arrays contain the same number of elements, and all corresponding pairs
	 * of elements in the two arrays are equal.  In other words, two arrays
	 * are equal if they contain the same elements in the same order.  Also,
	 * two array references are considered equal if both are <tt>null</tt>.<p>
	 *
	 * @param a one array to be tested for equality.
	 * @param a2 the other array to be tested for equality.
	 * @return <tt>true</tt> if the two arrays are equal.
	 */
	public static boolean equals(int[] a, int[] a2) {
		if (a == a2)
			return true;
		if (a == null || a2 == null)
			return false;

		int length = a.length;
		if (a2.length != length)
			return false;

		for (int i = 0; i < length; i++)
			if (a[i] != a2[i])
				return false;

		return true;
	}

	/**
	 * Returns <tt>true</tt> if the two specified arrays of shorts are
	 * <i>equal</i> to one another.  Two arrays are considered equal if both
	 * arrays contain the same number of elements, and all corresponding pairs
	 * of elements in the two arrays are equal.  In other words, two arrays
	 * are equal if they contain the same elements in the same order.  Also,
	 * two array references are considered equal if both are <tt>null</tt>.<p>
	 *
	 * @param a one array to be tested for equality.
	 * @param a2 the other array to be tested for equality.
	 * @return <tt>true</tt> if the two arrays are equal.
	 */
	public static boolean equals(short[] a, short a2[]) {
		if (a == a2)
			return true;
		if (a == null || a2 == null)
			return false;

		int length = a.length;
		if (a2.length != length)
			return false;

		for (int i = 0; i < length; i++)
			if (a[i] != a2[i])
				return false;

		return true;
	}

	/**
	 * Returns <tt>true</tt> if the two specified arrays of chars are
	 * <i>equal</i> to one another.  Two arrays are considered equal if both
	 * arrays contain the same number of elements, and all corresponding pairs
	 * of elements in the two arrays are equal.  In other words, two arrays
	 * are equal if they contain the same elements in the same order.  Also,
	 * two array references are considered equal if both are <tt>null</tt>.<p>
	 *
	 * @param a one array to be tested for equality.
	 * @param a2 the other array to be tested for equality.
	 * @return <tt>true</tt> if the two arrays are equal.
	 */
	public static boolean equals(char[] a, char[] a2) {
		if (a == a2)
			return true;
		if (a == null || a2 == null)
			return false;

		int length = a.length;
		if (a2.length != length)
			return false;

		for (int i = 0; i < length; i++)
			if (a[i] != a2[i])
				return false;

		return true;
	}

	/**
	 * Returns <tt>true</tt> if the two specified arrays of bytes are
	 * <i>equal</i> to one another.  Two arrays are considered equal if both
	 * arrays contain the same number of elements, and all corresponding pairs
	 * of elements in the two arrays are equal.  In other words, two arrays
	 * are equal if they contain the same elements in the same order.  Also,
	 * two array references are considered equal if both are <tt>null</tt>.<p>
	 *
	 * @param a one array to be tested for equality.
	 * @param a2 the other array to be tested for equality.
	 * @return <tt>true</tt> if the two arrays are equal.
	 */
	public static boolean equals(byte[] a, byte[] a2) {
		if (a == a2)
			return true;
		if (a == null || a2 == null)
			return false;

		int length = a.length;
		if (a2.length != length)
			return false;

		for (int i = 0; i < length; i++)
			if (a[i] != a2[i])
				return false;

		return true;
	}

	/**
	 * Returns <tt>true</tt> if the two specified arrays of equals are
	 * <i>equal</i> to one another.  Two arrays are considered equal if both
	 * arrays contain the same number of elements, and all corresponding pairs
	 * of elements in the two arrays are equal.  In other words, two arrays
	 * are equal if they contain the same elements in the same order.  Also,
	 * two array references are considered equal if both are <tt>null</tt>.<p>
	 *
	 * @param a one array to be tested for equality.
	 * @param a2 the other array to be tested for equality.
	 * @return <tt>true</tt> if the two arrays are equal.
	 */
	public static boolean equals(boolean[] a, boolean[] a2) {
		if (a == a2)
			return true;
		if (a == null || a2 == null)
			return false;

		int length = a.length;
		if (a2.length != length)
			return false;

		for (int i = 0; i < length; i++)
			if (a[i] != a2[i])
				return false;

		return true;
	}

	/**
	 * Returns <tt>true</tt> if the two specified arrays of doubles are
	 * <i>equal</i> to one another.  Two arrays are considered equal if both
	 * arrays contain the same number of elements, and all corresponding pairs
	 * of elements in the two arrays are equal.  In other words, two arrays
	 * are equal if they contain the same elements in the same order.  Also,
	 * two array references are considered equal if both are <tt>null</tt>.<p>
	 *
	 * Two doubles <tt>d1</tt> and <tt>d2</tt> are considered equal if:
	 * <pre>    <tt>new Double(d1).equals(new Double(d2))</tt></pre>
	 * (Unlike the <tt>==</tt> operator, this method considers
	 * <tt>NaN</tt> equals to itself, and 0.0d unequal to -0.0d.)
	 *
	 * @param a one array to be tested for equality.
	 * @param a2 the other array to be tested for equality.
	 * @return <tt>true</tt> if the two arrays are equal.
	 * @see Double#equals(Object)
	 */
	public static boolean equals(double[] a, double[] a2) {
		if (a == a2)
			return true;
		if (a == null || a2 == null)
			return false;

		int length = a.length;
		if (a2.length != length)
			return false;

		for (int i = 0; i < length; i++)
			if (Double.doubleToLongBits(a[i]) != Double.doubleToLongBits(a2[i]))
				return false;

		return true;
	}

	/**
	 * Returns <tt>true</tt> if the two specified arrays of floats are
	 * <i>equal</i> to one another.  Two arrays are considered equal if both
	 * arrays contain the same number of elements, and all corresponding pairs
	 * of elements in the two arrays are equal.  In other words, two arrays
	 * are equal if they contain the same elements in the same order.  Also,
	 * two array references are considered equal if both are <tt>null</tt>.<p>
	 *
	 * Two floats <tt>f1</tt> and <tt>f2</tt> are considered equal if:
	 * <pre>    <tt>new Float(f1).equals(new Float(f2))</tt></pre>
	 * (Unlike the <tt>==</tt> operator, this method considers
	 * <tt>NaN</tt> equals to itself, and 0.0f unequal to -0.0f.)
	 *
	 * @param a one array to be tested for equality.
	 * @param a2 the other array to be tested for equality.
	 * @return <tt>true</tt> if the two arrays are equal.
	 * @see Float#equals(Object)
	 */
	public static boolean equals(float[] a, float[] a2) {
		if (a == a2)
			return true;
		if (a == null || a2 == null)
			return false;

		int length = a.length;
		if (a2.length != length)
			return false;

		for (int i = 0; i < length; i++)
			if (Float.floatToIntBits(a[i]) != Float.floatToIntBits(a2[i]))
				return false;

		return true;
	}

	/**
	 * Returns <tt>true</tt> if the two specified arrays of Objects are
	 * <i>equal</i> to one another.  The two arrays are considered equal if
	 * both arrays contain the same number of elements, and all corresponding
	 * pairs of elements in the two arrays are equal.  Two objects <tt>e1</tt>
	 * and <tt>e2</tt> are considered <i>equal</i> if <tt>(e1==null ? e2==null
	 * : e1.equals(e2))</tt>.  In other words, the two arrays are equal if
	 * they contain the same elements in the same order.  Also, two array
	 * references are considered equal if both are <tt>null</tt>.<p>
	 *
	 * @param a one array to be tested for equality.
	 * @param a2 the other array to be tested for equality.
	 * @return <tt>true</tt> if the two arrays are equal.
	 */
	public static boolean equals(Object[] a, Object[] a2) {
		if (a == a2)
			return true;
		if (a == null || a2 == null)
			return false;

		int length = a.length;
		if (a2.length != length)
			return false;

		for (int i = 0; i < length; i++) {
			Object o1 = a[i];
			Object o2 = a2[i];
			if (!(o1 == null ? o2 == null : o1.equals(o2)))
				return false;
		}

		return true;
	}

}