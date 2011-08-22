package spade.analysis.geocomp.functions;

public class Local extends Mean {
	protected double orig = Double.NaN;

	@Override
	public void init(double orig) {
		init();
		this.orig = orig;
	}

	@Override
	public double getResult() {
		return orig - super.getResult();
	}
}
