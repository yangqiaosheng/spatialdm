package spade.analysis.geocomp.functions;

public class Sum implements Function {
	protected double sum;
	protected int counter;

	@Override
	public void init() {
		sum = 0;
		counter = 0;
	}

	@Override
	public void init(double orig) {
		init();
	}

	@Override
	public void addData(double val) {
		sum += val;
		counter++;
	}

	@Override
	public double getResult() {
		if (counter == 0)
			return Double.NaN;
		else
			return sum;
	}

	public int getCounter() {
		return counter;
	}

	@Override
	public String toString() {
		Float result = new Float(getResult());
		if (result.isNaN())
			return "";
		else
			return result.toString();
	}
}
