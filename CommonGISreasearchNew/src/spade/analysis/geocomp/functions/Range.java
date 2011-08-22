package spade.analysis.geocomp.functions;

public class Range extends Sum {
	private Min lnkMin = new Min();
	private Max lnkMax = new Max();

	@Override
	public void init() {
		lnkMin.init();
		lnkMax.init();
	}

	@Override
	public void addData(double val) {
		lnkMin.addData(val);
		lnkMax.addData(val);
	}

	@Override
	public double getResult() {
		return lnkMax.getResult() - lnkMin.getResult();
	}
}