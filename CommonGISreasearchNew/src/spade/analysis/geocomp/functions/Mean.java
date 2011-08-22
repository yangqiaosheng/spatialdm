package spade.analysis.geocomp.functions;

public class Mean extends Sum {
	@Override
	public double getResult() {
		return super.getResult() / counter;
	}
}
