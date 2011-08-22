package spade.analysis.geocomp.mutil;

public class CInteger implements Compare {
	public Integer v;

	public CInteger(int f) {
		v = new Integer(f);
	}

	@Override
	public int Comp(Object k) {
		CInteger f = (CInteger) k;
		if (f.v.intValue() < v.intValue())
			return -1;
		if (f.v.intValue() > v.intValue())
			return 1;
		return 0;
	}

	public int intValue() {
		return v.intValue();
	}
}