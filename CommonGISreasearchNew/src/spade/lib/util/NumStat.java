package spade.lib.util;

/**
* Contains statistics about a set of numeric values
*/
public class NumStat {
	public int nValues = 0;
	public double minValue = Float.NaN, maxValue = Float.NaN, sum = Float.NaN, mean = Float.NaN, median = Float.NaN, lowerQuart = Float.NaN, upperQuart = Float.NaN;
}