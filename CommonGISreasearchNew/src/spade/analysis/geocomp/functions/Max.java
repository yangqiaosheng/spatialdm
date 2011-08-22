package spade.analysis.geocomp.functions;

public class Max extends Sum {
	@Override
	public void init() {
		sum = Double.NEGATIVE_INFINITY;
	}

	@Override
	public void addData(double val) {
		sum = Math.max(sum, val);
	}

	@Override
	public double getResult() {
		if (Double.isInfinite(sum))
			return Double.NaN;
		else
			return sum;
	}
}
