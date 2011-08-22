package spade.vis.geometry;

import java.awt.Color;

import spade.lib.color.CS;
import spade.lib.util.IntArray;

public abstract class StructSign extends Sign {
	/**
	* Constants used to specify the order of segments in a sign
	*/
	public static final int OrderPreserved = 0, OrderDescending = 1, OrderAscending = 2;
	/**
	* Maximum number of colors
	*/
	public static int maxNColors = 15;
	/**
	* Number of segments in a sign
	*/
	protected int nsegm = 1;
	/**
	* parts[i] is a number between 0 and 1
	* control heights of bars in bar charts, or angles in pies, or radiuses in
	* utility wheels
	*/
	protected float parts[] = null;
	/**
	* weights[i] - numbers between 0 and 1, Sum=1
	* control widths of bars in utility bars or angles in utility wheels
	*/
	protected float weights[] = null;
	/**
	* Colors used to paint the segments when a diagram is drawn
	*/
	protected Color colors[] = null;
	/**
	* The method of ordering of the parts while drawing. This should be
	* one of the constants OrderPreserved (default), OrderDescending,
	* or OrderAscending
	*/
	protected int orderMethod = OrderPreserved;
	/**
	* An internal array used for ordering of the segments
	*/
	protected static IntArray order = null;

	public void setNSegments(int ns) {
		nsegm = ns;
	}

	public int getNSegments() {
		return nsegm;
	}

	public void setDefaultColors() {
		if (colors == null || colors.length < nsegm) {
			colors = new Color[(nsegm > maxNColors) ? nsegm : maxNColors];
			for (int i = 0; i < colors.length; i++) {
				colors[i] = CS.getNthPureColor(i, colors.length);
			}
		}
	}

	public void setSegmentColor(int segmN, Color color) {
		if (segmN < 0 || segmN >= nsegm)
			return;
		if (colors == null || colors.length < nsegm) {
			setDefaultColors();
		}
		colors[segmN] = color;
	}

	public Color getSegmentColor(int segmN) {
		if (segmN < 0 || segmN >= nsegm)
			return null;
		if (colors == null || colors.length < nsegm) {
			setDefaultColors();
		}
		return colors[segmN];
	}

	public void setSegmentWeight(int segmN, float weight) {
		if (segmN < 0 || segmN >= nsegm)
			return;
		if (weights == null || weights.length < nsegm) {
			weights = new float[nsegm];
		}
		if (weight < 0) {
			weight = -weight;
		}
		weights[segmN] = weight;
	}

	public void setSegmentPart(int segmN, float part) {
		if (segmN < 0 || segmN >= nsegm)
			return;
		if (parts == null || parts.length < nsegm) {
			parts = new float[nsegm];
		}
		//if (part<0) part=0;
		parts[segmN] = part;
	}

	public float getSegmentPart(int segmN) {
		if (parts == null | parts.length <= segmN)
			return Float.NaN;
		return parts[segmN];
	}

	protected boolean areAllPositive() {
		for (float part : parts)
			if (part < 0)
				return false;
		return true;
	}

	/**
	* Sets the method of ordering of the parts while drawing. This should be
	* one of the constants OrderPreserved (default), OrderDescending,
	* or OrderAscending
	*/
	public void setOrderingMethod(int method) {
		if (method >= OrderPreserved && method <= OrderAscending) {
			orderMethod = method;
		}
	}

	/**
	* Returns the method of ordering of the segments. This is
	* one of the constants OrderPreserved (default), OrderDescending,
	* or OrderAscending
	*/
	public int getOrderingMethod() {
		return orderMethod;
	}

	/**
	* Returns the order of the segments according to the values.
	* The argument "length" specifies the number of items to take into account
	* (the real length of the array with values may be different).
	*/
	protected static IntArray getOrder(float values[], int length, int method) {
		if (values == null)
			return null;
		if (order == null) {
			order = new IntArray(10, 5);
		} else {
			order.removeAllElements();
		}
		if (method == OrderPreserved || length == 1) {
			for (int i = 0; i < length; i++) {
				order.addElement(i);
			}
		} else {
			order.addElement(0);
			for (int i = 1; i < length; i++) {
				boolean inserted = false;
				for (int j = 0; j < order.size() && !inserted; j++)
					if ((method == OrderDescending && values[i] > values[order.elementAt(j)]) || (method == OrderAscending && values[i] < values[order.elementAt(j)])) {
						order.insertElementAt(i, j);
						inserted = true;
					}
				if (!inserted) {
					order.addElement(i);
				}
			}
		}
		return order;
	}

	/**
	* Returns the order of the segments according to the values contained in
	* the array "parts".
	* Returns an array of integers of the same length as the array "parts".
	*/
	protected IntArray getOrder() {
		return getOrder(parts, nsegm, orderMethod);
	}

}
