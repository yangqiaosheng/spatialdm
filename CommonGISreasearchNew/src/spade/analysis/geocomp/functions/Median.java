package spade.analysis.geocomp.functions;

import spade.analysis.geocomp.mutil.CDouble;
import spade.analysis.geocomp.mutil.SortVector;

public class Median extends Sum {
	protected SortVector sv = new SortVector(true);

	@Override
	public void init() {
		sv.removeAllElements();
	}

	@Override
	public void addData(double val) {
		sv.Insert(new CDouble(val));
	}

	@Override
	public double getResult() {
		if (sv.isEmpty())
			return Double.NaN;
		int sz = sv.size();
		if (sz == 1)
			return ((CDouble) sv.elementAt(0)).v.doubleValue();
		double d1 = ((CDouble) sv.elementAt(sz / 2)).v.doubleValue();
		if (sz % 2 == 1)
			return d1;
		double d0 = ((CDouble) sv.elementAt(sz / 2 - 1)).v.doubleValue();
		return (d0 + d1) / 2;
	}

	@Override
	public String toString() {
		Double result = ((CDouble) sv.elementAt(sv.size() / 2)).v;
		if (result.isNaN())
			return "";
		else
			return result.toString();
	}
}