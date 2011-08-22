package spade.analysis.geocomp.functions;

public interface Function {
	public void init();

	void init(double orig);

	public void addData(double val);

	public double getResult();

	@Override
	String toString();
}
