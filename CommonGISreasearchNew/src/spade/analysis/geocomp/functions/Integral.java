package spade.analysis.geocomp.functions;

public class Integral extends Sum {
	protected double cell = Double.NaN;

	@Override
	public void init(double cell) {
		init();
		this.cell = cell;
	}

	@Override
	public double getResult() {
		return super.getResult() * cell;
	}
}
