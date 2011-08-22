package spade.analysis.geocomp.functions;

import java.util.Vector;

public class RMS extends Sum {
	protected double sum1;
	protected double val;
	protected double avg;
	protected Vector v = new Vector();

	@Override
	public void init() {
		super.init();
		v.removeAllElements();
	}

	@Override
	public void addData(double val) {
		super.addData(val);
		v.addElement(new Double(val));
	}

	@Override
	public double getResult() {
		if (counter <= 1)
			return Double.NaN;
		else {
			avg = sum / counter;
			sum1 = 0;
			for (int i = 0; i < counter; i++) {
				val = ((Double) v.elementAt(i)).doubleValue() - avg;
				sum1 += val * val;
			}
			return Math.sqrt(sum1 / (counter - 1));
		}
	}
}