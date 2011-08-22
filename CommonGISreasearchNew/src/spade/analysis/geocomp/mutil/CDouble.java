package spade.analysis.geocomp.mutil;

public class CDouble implements Compare {
	public Double v;

	public CDouble(double f) {
		v = new Double(f);
	}

	@Override
	public int Comp(Object k) {
		CDouble f = (CDouble) k;
		if (f.v.doubleValue() < v.doubleValue())
			return -1;
		if (f.v.doubleValue() > v.doubleValue())
			return 1;
		return 0;
	}
}