package spade.analysis.geocomp.functions;

public class Min extends Sum {
	@Override
	public void init() {
		sum = Double.POSITIVE_INFINITY;
	}

	@Override
	public void addData(double val) {
		sum = Math.min(sum, val);
	}

	@Override
	public double getResult() {
		if (Double.isInfinite(sum))
			return Double.NaN;
		else
			return sum;
	}
}