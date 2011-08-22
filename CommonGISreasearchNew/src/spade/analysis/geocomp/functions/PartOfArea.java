package spade.analysis.geocomp.functions;

/**
* Calcualtes the part of the area of a polygon with raster values within the
* specified limits
*/
public class PartOfArea implements Function {
	protected int total = 0, selected = 0;
	protected float lowLimit = Float.NaN, upLimit = Float.NaN;

	@Override
	public void init() {
		total = 0;
		selected = 0;
	}

	@Override
	public void init(double orig) {
		init();
	}

	public void setLimits(float min, float max) {
		lowLimit = min;
		upLimit = max;
	}

	@Override
	public void addData(double val) {
		++total;
		if ((Float.isNaN(lowLimit) || val >= lowLimit) && (Float.isNaN(upLimit) || val <= upLimit)) {
			++selected;
		}
	}

	@Override
	public double getResult() {
		if (total == 0)
			return Double.NaN;
		return 100.0 * selected / total;
	}

	@Override
	public String toString() {
		Float result = new Float(getResult());
		if (result.isNaN())
			return "";
		return result.toString();
	}
}
