package spade.analysis.geocomp.functions;

/**
* Calcualtes the area of a polygon on the basis of a raster. It is possible to
* specify the lower or/and upper limits for the values in the raster. In this
* case the class will calculate the area with raster values lying within the
* specified limits
*/
public class Area extends Integral {
	protected float lowLimit = Float.NaN, upLimit = Float.NaN;

	public void setLimits(float min, float max) {
		lowLimit = min;
		upLimit = max;
	}

	@Override
	public void addData(double val) {
		if ((Float.isNaN(lowLimit) || val >= lowLimit) && (Float.isNaN(upLimit) || val <= upLimit)) {
			super.addData(1.0d);
		} else {
			++counter;
		}
	}
}
