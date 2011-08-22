package spade.vis.database;

public class AttrTransform {

	/**
	* The source of the data for statistics
	*/
	protected AttributeDataPortion dataTable = null;

	/*
	* field numbers
	*/
	int fn[] = null;

	/*
	* Statistics for data
	*/
	protected TableStat tStat = null;

	protected double RefVal[] = null, // Reference Values - if Alignment to objects
			MaxRefValRatio = 0f;
	protected double r[][] = null; // for alignment to 2 classes,
									// see TableStat.computeScaleTransformations
	protected float weights[] = null; // used for multiple criteria evaluation
	// last 2 weights have a special meaning: +1 corresponds to the atribute
	// representing the integrated criterion (to be maximized);
	// -1 - order by integrated criterion (to be minimized)
	protected boolean hasCostCriteria = false, hasBenefitCriteria = false;

	public boolean getHasCostCriteria() {
		return hasCostCriteria;
	}

	public boolean getHasBenefitCriteria() {
		return hasBenefitCriteria;
	}

	protected boolean lastIsOrder = true;
	protected float maxAbsW = 0f;

	public AttrTransform(AttributeDataPortion dataTable, TableStat tStat, int fn[]) {
		this.dataTable = dataTable;
		this.tStat = tStat;
		this.fn = fn;
		RefVal = new double[fn.length];
		for (int j = 0; j < fn.length; j++) {
			RefVal[j] = Double.NaN;
		}
	}

	public void setRefVal(double RefVal[], double MaxRefValRatio) {
		this.MaxRefValRatio = MaxRefValRatio;
		this.RefVal = RefVal;
	}

	public void setR(double[][] r) {
		this.r = r;
	}

	public void setFn(int fn[]) {
		this.fn = fn;
	}

	public void setWeights(float weights[]) {
		this.weights = weights;
		hasCostCriteria = false;
		hasBenefitCriteria = false;
		maxAbsW = 0f;
		for (int i = 0; i < weights.length - 2; i++) {
			if (weights[i] < 0) {
				hasCostCriteria = true;
			}
			if (weights[i] > 0) {
				hasBenefitCriteria = true;
			}
			if (Math.abs(weights[i]) > maxAbsW) {
				maxAbsW = Math.abs(weights[i]);
			}
		}
		lastIsOrder = weights[weights.length - 1] < 0;
	}

	public float getClassBias() {
		return (float) (1f / (1 + r[4][fn.length]));
	}

	public int getScaleOrientation(int n) {
		return (int) r[0][n];
	}

	public double value(double v, int n, int mode) {
		if (!dataTable.isAttributeNumeric(fn[n]) && dataTable instanceof DataTable) {
			Attribute attr = ((DataTable) dataTable).getAttribute(fn[n]);
			if (attr.isClassification()) {
				if (mode == 8)
					return 0.5 + 0.5 * (v - 1) * getClassBias();
				else
					return (v < 0) ? Double.NaN : v / (attr.getNClasses() - 1);
			}
		}
		switch (mode) {
		case 14:
		case 10: // AlignICLwithoutWeights or AlignICRwithoutWeights
			if (weights == null)
				return Double.NaN;
			double rv = (v - tStat.getMin(fn[n])) / (tStat.getMax(fn[n]) - tStat.getMin(fn[n]));
			if (n < fn.length - 2)
				return (weights[n] > 0) ? rv : 1 - rv;
			else
				return (n == fn.length - 2 && lastIsOrder || n == fn.length - 1 && !lastIsOrder) ? rv : 1 - rv;
		case 13: // AlignICLwithWeights
			if (weights == null)
				return Double.NaN;
			rv = (v - tStat.getMin(fn[n])) / (tStat.getMax(fn[n]) - tStat.getMin(fn[n]));
			if (n < fn.length - 2)
				return Math.abs(weights[n] * ((weights[n] > 0) ? rv : 1 - rv)) / maxAbsW;
			else
				return (n == fn.length - 2 && lastIsOrder || n == fn.length - 1 && !lastIsOrder) ? rv : 1 - rv;
		case 12: // AlignICCwithoutWeights
			if (weights == null)
				return Double.NaN;
			rv = (v - tStat.getMin(fn[n])) / (tStat.getMax(fn[n]) - tStat.getMin(fn[n]));
			if (n < fn.length - 2)
				return 0.5 + ((weights[n] > 0) ? 1 : -1) * rv * 0.5;
			else {
				double vv = 0.5f + ((n == fn.length - 2 && lastIsOrder || n == fn.length - 1 && !lastIsOrder) ? 1 : -1) * rv * 0.5;
				if (weights[n] == -1) {
					vv += 0.5f;
				}
				return vv;
			}
		case 11: // AlignICCwithWeights
			if (weights == null)
				return Double.NaN;
			rv = (v - tStat.getMin(fn[n])) / (tStat.getMax(fn[n]) - tStat.getMin(fn[n]));
			if (n < fn.length - 2)
				return 0.5f + ((weights[n] > 0) ? 1 : -1) * 0.5 * rv * Math.abs(weights[n]) / maxAbsW;
			else {
				double vv = 0.5f + ((n == fn.length - 2 && lastIsOrder || n == fn.length - 1 && !lastIsOrder) ? 1 : -1) * rv * 0.5;
				if (weights[n] == -1) {
					vv += 0.5f;
				}
				return vv;
			}
		case 9: // AlignICRwithWeights
			if (weights == null)
				return Double.NaN;
			rv = (v - tStat.getMin(fn[n])) / (tStat.getMax(fn[n]) - tStat.getMin(fn[n]));
			if (n < fn.length - 2) {
				double rv1 = 1 - rv;
				return 1 - Math.abs(weights[n] * ((weights[n] > 0) ? rv1 : 1 - rv1)) / maxAbsW;
			} else
				return (n == fn.length - 2 && lastIsOrder || n == fn.length - 1 && !lastIsOrder) ? rv : 1 - rv;
		case 8: // alignment to 2 classes
			if (r == null)
				return Double.NaN;
			if (r[0][n] == 0)
				return value(v, n, 1);
			if (r[0][n] == 1) {
				if (v >= r[2][n]) {
					double dv = (v - r[2][n]) / (r[2][n] - r[3][n]);
					return 0.5f + 0.5f * getClassBias() * (1 + dv);
				} else if (v >= r[3][n]) {
					double dv = (r[2][n] - v) / (r[2][n] - r[3][n]);
					return 0.5f + 0.5f * getClassBias() * (1 - dv);
				} else if (v > r[1][n]) {
					double dv = (v - r[1][n]) / (r[2][n] - r[3][n]);
					return 0.5f - 0.5f * getClassBias() * (1 - dv);
				} else {
					double dv = (r[1][n] - v) / (r[2][n] - r[3][n]);
					return 0.5f - 0.5f * getClassBias() * (1 + dv);
				}
			} else { // r[0][n]==-1
				if (v >= r[1][n]) {
					double dv = (v - r[1][n]) / (r[1][n] - r[3][n]);
					return 0.5f - 0.5f * getClassBias() * (1 + dv);
				} else if (v >= r[3][n]) {
					double dv = (r[1][n] - v) / (r[1][n] - r[3][n]);
					return 0.5f - 0.5f * getClassBias() * (1 - dv);
				} else if (v > r[2][n]) {
					double dv = (v - r[2][n]) / (r[1][n] - r[3][n]);
					return 0.5f + 0.5f * getClassBias() * (1 - dv);
				} else {
					double dv = (r[2][n] - v) / (r[1][n] - r[3][n]);
					return 0.5f + 0.5f * getClassBias() * (1 + dv);
				}
			}
		case 7: // std. deviation
			double dv = v / tStat.getStdDev(fn[n]) - tStat.getGlobalMinStdd(fn);
			return dv / (tStat.getGlobalMaxStdd(fn) - tStat.getGlobalMinStdd(fn));
		case 6: // mean and std.deviation
			dv = (v - tStat.getMean(fn[n])) / tStat.getStdDev(fn[n]);
			return (1f + dv / tStat.getRatioMStdD(fn)) / 2;
		case 5: // scaled median and quartiles
			dv = 0f;
			double rattr = 0f;
			if (v > tStat.getHQ(fn[n])) {
				dv = (v - tStat.getHQ(fn[n])) / (tStat.getMax(fn[n]) - tStat.getHQ(fn[n]));
				rattr = (tStat.getMax(fn[n]) - tStat.getHQ(fn[n])) / (tStat.getHQ(fn[n]) - tStat.getMedian(fn[n]));
				return 0.5f + 0.5f / (1f + tStat.getRatioMQ(fn)) + 0.5f * dv * rattr / (1f + tStat.getRatioMQ(fn));
			}
			if (v > tStat.getMedian(fn[n])) {
				dv = (v - tStat.getMedian(fn[n])) / (tStat.getHQ(fn[n]) - tStat.getMedian(fn[n]));
				return 0.5f + 0.5f * dv / (1f + tStat.getRatioMQ(fn));
			}
			if (v > tStat.getLQ(fn[n])) {
				dv = (v - tStat.getMedian(fn[n])) / (tStat.getMedian(fn[n]) - tStat.getLQ(fn[n]));
				return 0.5f + 0.5f * dv / (1f + tStat.getRatioMQ(fn));
			}
			dv = (v - tStat.getLQ(fn[n])) / (tStat.getLQ(fn[n]) - tStat.getMin(fn[n]));
			rattr = (tStat.getMin(fn[n]) - tStat.getLQ(fn[n])) / (tStat.getLQ(fn[n]) - tStat.getMedian(fn[n]));
			return 0.5f - 0.5f / (1f + tStat.getRatioMQ(fn)) + 0.5f * dv * rattr / (1f + tStat.getRatioMQ(fn));
		case 4: // median and quartiles
			if (v > tStat.getHQ(fn[n]))
				return 0.75f + 0.25f * (v - tStat.getHQ(fn[n])) / (tStat.getMax(fn[n]) - tStat.getHQ(fn[n]));
			if (v > tStat.getMedian(fn[n]))
				return 0.5f + 0.25f * (v - tStat.getMedian(fn[n])) / (tStat.getHQ(fn[n]) - tStat.getMedian(fn[n]));
			if (v > tStat.getLQ(fn[n]))
				return 0.25f + 0.25f * (v - tStat.getLQ(fn[n])) / (tStat.getMedian(fn[n]) - tStat.getLQ(fn[n]));
			return 0.25f * (v - tStat.getMin(fn[n])) / (tStat.getLQ(fn[n]) - tStat.getMin(fn[n]));
		case 2: // common min and max
			return (v - tStat.getCommonMin(fn)) / (tStat.getCommonMax(fn) - tStat.getCommonMin(fn));
		case 3: // // alignment to selected object(s)
			if (MaxRefValRatio > 0)
				return 0.5f + ((Double.isNaN(RefVal[n])) ? 0 : ((MaxRefValRatio > 0) ? 0.5f / MaxRefValRatio : 0.5f) * (v - RefVal[n]) / (tStat.getMax(fn[n]) - tStat.getMin(fn[n])));
		default:
			if (tStat.getMax(fn[n]) == tStat.getMin(fn[n]))
				return 0.5f;
			else
				return (v - tStat.getMin(fn[n])) / (tStat.getMax(fn[n]) - tStat.getMin(fn[n]));
		}
	}

}
