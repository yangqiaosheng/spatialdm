package spade.analysis.geocomp.functions;

import java.util.Enumeration;
import java.util.Hashtable;

/**
 * Created by IntelliJ IDEA.
 * User: DIvan
 * Date: Jan 21, 2004
 * Time: 9:06:55 PM
 * To change this template use Options | File Templates.
 */
public class Mode extends Sum {
	protected Hashtable freq = new Hashtable();

	@Override
	public void addData(double val) {
		Double key = new Double(val);
		Integer curFreq = (Integer) freq.get(key);
		if (curFreq == null) {
			curFreq = new Integer(0);
		}
		freq.put(key, new Integer(curFreq.intValue() + 1));
	}

	@Override
	public double getResult() {
		int maxFreq = 0;
		double best = Double.NaN;
		for (Enumeration e = freq.keys(); e.hasMoreElements();) {
			Object key = e.nextElement();
			if (key == null) {
				continue;
			}
			int curFreq = ((Integer) freq.get(key)).intValue();
			if (curFreq > maxFreq) {
				maxFreq = curFreq;
				best = ((Double) key).doubleValue();
			}
		}
		return best;
	}

	@Override
	public void init() {
		freq = new Hashtable();
	}

	@Override
	public String toString() {
		double res = getResult();
		if (!Double.isNaN(res))
			return String.valueOf(res);
		else
			return "";
	}
}
