package spade.analysis.geocomp.mutil;

public class CFloat implements Compare {
	public Float v;

	public CFloat(float f) {
		v = new Float(f);
	}

	@Override
	public int Comp(Object k) {
		CFloat f = (CFloat) k;
		if (f.v.floatValue() < v.floatValue())
			return -1;
		if (f.v.floatValue() > v.floatValue())
			return 1;
		return 0;
	}
}