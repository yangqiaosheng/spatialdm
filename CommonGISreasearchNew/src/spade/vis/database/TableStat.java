package spade.vis.database;

import java.util.Vector;

import spade.analysis.transform.AttributeTransformer;
import spade.lib.util.DoubleArray;
import spade.lib.util.NumValManager;

public class TableStat {

	/**
	* The source of the data for statistics
	*/
	protected AttributeDataPortion dataTable = null;
	/**
	* A TableStat may optionally be connected to a transformer of attribute
	* values. In this case, it works with transformed attribute values.
	*/
	protected AttributeTransformer aTrans = null;

	private double AbsMin[] = null, AbsMax[] = null, med[] = null, // medians
			qlow[] = null, // low quartiles
			qhigh[] = null, // upper quartiles
			mean[] = null, // mean values
			stdd[] = null, // standard deviations
			RefVal[] = null, // reference values
			MaxRefValRatio = 0f;

	public double getMedian(int n) {
		check(n);
		return med[n];
	}

	public double getMin(int n) {
		check(n);
		return AbsMin[n];
	}

	public double getMax(int n) {
		check(n);
		return AbsMax[n];
	}

	public double getStdDev(int n) {
		check(n);
		return stdd[n];
	}

	public double getMean(int n) {
		check(n);
		return mean[n];
	}

	public double getLQ(int n) {
		check(n);
		return qlow[n];
	}

	public double getHQ(int n) {
		check(n);
		return qhigh[n];
	}

	public double[] getRefVal() {
		return RefVal;
	}

	public double getMaxRefValRatio() {
		return MaxRefValRatio;
	}

	public void setDataTable(AttributeDataPortion dataTable) {
		this.dataTable = dataTable;
		reset();
	}

	public void setDataTable(AttributeDataPortion dataTable, AttributeTransformer trans) {
		this.dataTable = dataTable;
		aTrans = trans;
		reset();
	}

	/**
	* Depending on the presence of an attribute transformer, gets attribute values
	* either from the transformer or the table
	*/
	public double getNumericAttrValue(int attrN, int recN) {
		if (aTrans != null)
			return aTrans.getNumericAttrValue(attrN, recN);
		if (dataTable != null)
			return dataTable.getNumericAttrValue(attrN, recN);
		return Double.NaN;
	}

	public void reset() {
		int aCount = dataTable.getAttrCount();
		if (AbsMin == null || AbsMin.length != aCount) {
			AbsMin = new double[aCount];
		}
		if (AbsMax == null || AbsMax.length != aCount) {
			AbsMax = new double[aCount];
		}
		if (med == null || med.length != aCount) {
			med = new double[aCount];
		}
		if (qlow == null || qlow.length != aCount) {
			qlow = new double[aCount];
		}
		if (qhigh == null || qhigh.length != aCount) {
			qhigh = new double[aCount];
		}
		if (mean == null || mean.length != aCount) {
			mean = new double[aCount];
		}
		if (stdd == null || stdd.length != aCount) {
			stdd = new double[aCount];
		}
		for (int attrn = 0; attrn < med.length; attrn++) {
			AbsMin[attrn] = Double.NaN;
			AbsMax[attrn] = Double.NaN;
			med[attrn] = Double.NaN;
			qlow[attrn] = Double.NaN;
			qhigh[attrn] = Double.NaN;
			mean[attrn] = Double.NaN;
			stdd[attrn] = Double.NaN;
		}
		ComputeAbsMinMax();
		ComputeMeanAndStdDeviation();
		ComputeMedianAndQuartiles();
	}

	public void check(int n) {
		int aCount = dataTable.getAttrCount();
		if (n >= AbsMin.length && n < aCount) {
			int old = AbsMin.length;
			double newAbsMin[] = new double[aCount], newAbsMax[] = new double[aCount], newMed[] = new double[aCount], newQlow[] = new double[aCount], newQhigh[] = new double[aCount], newMean[] = new double[aCount], newStdd[] = new double[aCount];
			for (int i = 0; i < newAbsMin.length; i++)
				if (i < old) {
					newAbsMin[i] = AbsMin[i];
					newAbsMax[i] = AbsMax[i];
					newMed[i] = med[i];
					newQlow[i] = qlow[i];
					newQhigh[i] = qhigh[i];
					newMean[i] = mean[i];
					newStdd[i] = stdd[i];
				} else {
					newAbsMin[i] = Double.NaN;
					newAbsMax[i] = Double.NaN;
					newMed[i] = Double.NaN;
					newQlow[i] = Double.NaN;
					newQhigh[i] = Double.NaN;
					newMean[i] = Double.NaN;
					newStdd[i] = Double.NaN;
				}
			AbsMin = newAbsMin;
			AbsMax = newAbsMax;
			med = newMed;
			qlow = newQlow;
			qhigh = newQhigh;
			mean = newMean;
			stdd = newStdd;
			ComputeAbsMinMax(old);
			ComputeMeanAndStdDeviation(old);
			ComputeMedianAndQuartiles(old);
		}
	}

	public double getCommonMin(int fn[]) {
		double CommonMin = Double.NaN;
		for (int element : fn)
			if (dataTable.isAttributeNumeric(element) || dataTable.isAttributeTemporal(element))
				if (Double.isNaN(CommonMin) || CommonMin > AbsMin[element]) {
					CommonMin = AbsMin[element];
				}
		return CommonMin;
	}

	public double getCommonMax(int fn[]) {
		double CommonMax = Double.NaN;
		for (int element : fn)
			if (dataTable.isAttributeNumeric(element) || dataTable.isAttributeTemporal(element))
				if (Double.isNaN(CommonMax) || CommonMax < AbsMax[element]) {
					CommonMax = AbsMax[element];
				}
		return CommonMax;
	}

	public double getRatioMQ(int fn[]) {
		double RatioMQ = 0f, Ratio4to3mq = 0f, Ratio1to2mq = 0f;
		for (int element : fn)
			if (dataTable.isAttributeNumeric(element) || dataTable.isAttributeTemporal(element)) {
				double r = (AbsMax[element] - qhigh[element]) / (qhigh[element] - med[element]);
				if (r > Ratio4to3mq) {
					Ratio4to3mq = r;
				}
				r = (AbsMin[element] - qlow[element]) / (qlow[element] - med[element]);
				if (r > Ratio1to2mq) {
					Ratio1to2mq = r;
				}
			}
		RatioMQ = Ratio1to2mq;
		if (Ratio4to3mq > RatioMQ) {
			RatioMQ = Ratio4to3mq;
		}
		return RatioMQ;
	}

	public double getGlobalMinStdd(int fn[]) {
		double absminstdd = Double.NaN;
		for (int attrn = 0; attrn < fn.length; attrn++)
			if ((dataTable.isAttributeNumeric(fn[attrn]) || dataTable.isAttributeTemporal(fn[attrn])) && !Double.isNaN(stdd[fn[attrn]]))
				if (Double.isNaN(absminstdd) || absminstdd > AbsMin[fn[attrn]] / stdd[fn[attrn]]) {
					absminstdd = AbsMin[fn[attrn]] / stdd[fn[attrn]];
				}
		return absminstdd;
	}

	public double getGlobalMaxStdd(int fn[]) {
		double absmaxstdd = Double.NaN;
		for (int attrn = 0; attrn < fn.length; attrn++)
			if ((dataTable.isAttributeNumeric(fn[attrn]) || dataTable.isAttributeTemporal(fn[attrn])) && !Double.isNaN(stdd[fn[attrn]]) && stdd[fn[attrn]] > 0)
				if (Double.isNaN(absmaxstdd) || absmaxstdd < AbsMax[fn[attrn]] / stdd[fn[attrn]]) {
					absmaxstdd = AbsMax[fn[attrn]] / stdd[fn[attrn]];
				}
		return absmaxstdd;
	}

	public double getRatioMStdD(int fn[]) {
		double RatioMStdD = 0f;
		for (int attrn = 0; attrn < fn.length; attrn++)
			if ((dataTable.isAttributeNumeric(fn[attrn]) || dataTable.isAttributeTemporal(fn[attrn])) && !Double.isNaN(stdd[fn[attrn]]) && stdd[fn[attrn]] > 0) {
				if (RatioMStdD < (AbsMax[fn[attrn]] - mean[fn[attrn]]) / stdd[fn[attrn]]) {
					RatioMStdD = (AbsMax[fn[attrn]] - mean[fn[attrn]]) / stdd[fn[attrn]];
				}
				if (RatioMStdD < (mean[fn[attrn]] - AbsMin[fn[attrn]]) / stdd[fn[attrn]]) {
					RatioMStdD = (mean[fn[attrn]] - AbsMin[fn[attrn]]) / stdd[fn[attrn]];
				}
			}
		return RatioMStdD;
	}

	protected void ComputeAbsMinMax() {
		ComputeAbsMinMax(0);
	}

	protected void ComputeAbsMinMax(int startN) {
		for (int recn = 0; recn < dataTable.getDataItemCount(); recn++) {
			for (int attrn = startN; attrn < mean.length; attrn++)
				if (dataTable.isAttributeNumeric(attrn) || dataTable.isAttributeTemporal(attrn)) {
					double v = getNumericAttrValue(attrn, recn);
					if (!Double.isNaN(v)) {
						if (Double.isNaN(AbsMin[attrn]) || v < AbsMin[attrn]) {
							AbsMin[attrn] = v;
						}
						if (Double.isNaN(AbsMax[attrn]) || v > AbsMax[attrn]) {
							AbsMax[attrn] = v;
						}
					}
				} else if (dataTable instanceof DataTable) {
					Attribute attr = ((DataTable) dataTable).getAttribute(attrn);
					if (attr.isClassification()) {
						String strv = dataTable.getAttrValueAsString(attrn, recn);
						if (strv == null || strv.length() == 0) {
							continue;
						}
						int v = attr.getValueN(strv);
						if (v == -1) {
							continue;
						}
						if (Double.isNaN(AbsMin[attrn]) || v < AbsMin[attrn]) {
							AbsMin[attrn] = v;
						}
						if (Double.isNaN(AbsMax[attrn]) || v > AbsMax[attrn]) {
							AbsMax[attrn] = v;
						}
					}
				}
		}
	}

	protected void ComputeMeanAndStdDeviation() {
		ComputeMeanAndStdDeviation(0);
	}

	protected void ComputeMeanAndStdDeviation(int startN) {
		for (int attrn = startN; attrn < mean.length; attrn++)
			if (dataTable.isAttributeNumeric(attrn) || dataTable.isAttributeTemporal(attrn)) {
				DoubleArray v = new DoubleArray(dataTable.getDataItemCount(), 10);
				for (int i = 0; i < dataTable.getDataItemCount(); i++) {
					v.addElement(getNumericAttrValue(attrn, i));
				}
				mean[attrn] = NumValManager.getMean(v);
				stdd[attrn] = NumValManager.getStdD(v, mean[attrn]);
			}
	}

	protected void ComputeMedianAndQuartiles() {
		ComputeMedianAndQuartiles(0);
	}

	protected void ComputeMedianAndQuartiles(int startN) {
		for (int attrn = startN; attrn < med.length; attrn++)
			if (dataTable.isAttributeNumeric(attrn) || dataTable.isAttributeTemporal(attrn)) {
				int n = 0;
				for (int recn = 0; recn < dataTable.getDataItemCount(); recn++)
					if (!Double.isNaN(getNumericAttrValue(attrn, recn))) {
						n++;
					}
				double v[] = new double[n];
				n = 0;
				for (int recn = 0; recn < dataTable.getDataItemCount(); recn++)
					if (!Double.isNaN(getNumericAttrValue(attrn, recn))) {
						v[n] = getNumericAttrValue(attrn, recn);
						n++;
					}
				double breaks[] = NumValManager.breakToIntervals(v, 4, false);
				if (breaks != null && breaks.length >= 2) {
					qlow[attrn] = breaks[1];
				} else {
					qlow[attrn] = Double.NaN;
				}
				if (breaks != null && breaks.length >= 3) {
					med[attrn] = breaks[2];
				} else {
					med[attrn] = Double.NaN;
				}
				if (breaks != null && breaks.length >= 4) {
					qhigh[attrn] = breaks[3];
				} else {
					qhigh[attrn] = Double.NaN;
				}
			}
	}

	public double ComputeAverageValueForIDs(Vector IDList, int fn) {
		if (IDList == null || IDList.size() == 0)
			return Double.NaN;
		double sum = 0f;
		int n = 0;
		for (int j = 0; j < dataTable.getDataItemCount(); j++) {
			String id = dataTable.getDataItemId(j);
			if (id == null || id.trim().length() == 0) {
				continue;
			}
			boolean isSelected = false;
			for (int k = 0; k < IDList.size(); k++)
				if (id.equals(IDList.elementAt(k))) {
					isSelected = true;
					break;
				}
			if (isSelected) {
				double v = getNumericAttrValue(fn, j);
				if (!Double.isNaN(v)) {
					sum += v;
					n++;
				}
			}
		}
		return (n == 0) ? Double.NaN : sum / n;
	}

	public void ComputeMaxRefValRatio(Vector IDList, int fn[]) {
		RefVal = new double[fn.length];
		for (int j = 0; j < fn.length; j++) {
			RefVal[j] = Double.NaN;
		}
		MaxRefValRatio = 0f;
		if (IDList == null || IDList.size() == 0)
			return;
		for (int i = 0; i < RefVal.length; i++) { // compute average value
			RefVal[i] = ComputeAverageValueForIDs(IDList, fn[i]);
			if (!Double.isNaN(RefVal[i])) {
				double f1 = (RefVal[i] - getMin(fn[i])) / (getMax(fn[i]) - getMin(fn[i]));
				double f = (f1 > 0.5f) ? f1 : 1f - f1;
				if (f > MaxRefValRatio) {
					MaxRefValRatio = f;
				}
			}
		}
	}

	public void ComputeMaxRefValRatio(int recNum, int fn[]) {
		RefVal = new double[fn.length];
		for (int j = 0; j < fn.length; j++) {
			RefVal[j] = Double.NaN;
		}
		MaxRefValRatio = 0f;
		for (int i = 0; i < RefVal.length; i++) { // set reference value
			double v = getNumericAttrValue(fn[i], recNum);
			if (!Double.isNaN(v)) {
				RefVal[i] = v;
				double f1 = (RefVal[i] - getMin(fn[i])) / (getMax(fn[i]) - getMin(fn[i]));
				double f = (f1 > 0.5f) ? f1 : 1f - f1;
				if (f > MaxRefValRatio) {
					MaxRefValRatio = f;
				}
			}
		}
	}

	/*
	* computing data for scale transformations for classification
	*         A     0     B
	*    lv   |  1  |  1  |        rv
	*     +---|-----+-----|--------+
	*       vals1      vals2
	*/
	public double[][] computeScaleTransformations(Vector IDList1, Vector IDList2, int fn[]) {
		double r[][] = new double[6][];
		r[0] = new double[fn.length]; // 0 - vals1==vals2, 1 - vals2>vals1, -1 - vals1>vals2
		r[1] = new double[fn.length]; // vals1
		r[2] = new double[fn.length]; // vals2
		r[3] = new double[fn.length]; // 0.5*(vals1+vals2)
		r[4] = new double[fn.length + 1]; // lv
		r[5] = new double[fn.length + 1]; // rv
		// computing values for reference objects and orientation of scales
		for (int i = 0; i < fn.length; i++) {
			r[1][i] = ComputeAverageValueForIDs(IDList1, fn[i]);
			r[2][i] = ComputeAverageValueForIDs(IDList2, fn[i]);
			r[3][i] = 0.5f * (r[1][i] + r[2][i]);
			if (r[1][i] == r[2][i]) {
				r[0][i] = 0;
				r[4][i] = r[5][i] = Double.NaN;
			} else if (r[2][i] > r[1][i]) {
				r[0][i] = 1;
				r[4][i] = (r[1][i] - AbsMin[fn[i]]) / (r[2][i] - r[3][i]);
				r[5][i] = (AbsMax[fn[i]] - r[2][i]) / (r[2][i] - r[3][i]);
			} else {
				r[0][i] = -1;
				r[4][i] = (AbsMax[fn[i]] - r[1][i]) / (r[1][i] - r[3][i]);
				r[5][i] = (r[2][i] - AbsMin[fn[i]]) / (r[1][i] - r[3][i]);
			}
		}
		// computing max LV and RV
		r[4][fn.length] = r[5][fn.length] = Double.NaN;
		for (int i = 0; i < fn.length; i++) {
			if (!Double.isNaN(r[5][i]))
				if (Double.isNaN(r[5][fn.length]) || r[5][fn.length] < r[5][i]) {
					r[5][fn.length] = r[5][i];
				}
			if (!Double.isNaN(r[4][i]))
				if (Double.isNaN(r[4][fn.length]) || r[4][fn.length] < r[4][i]) {
					r[4][fn.length] = r[4][i];
				}
		}
		if (r[5][fn.length] > r[4][fn.length]) {
			r[4][fn.length] = r[5][fn.length];
		} else {
			r[5][fn.length] = r[4][fn.length];
		}
		return r;
	}

	public static double[] getMeanOfColumns(AttributeDataPortion dataTable, int AttrN[]) {
		double sum[] = new double[dataTable.getDataItemCount()];
		for (int j = 0; j < dataTable.getDataItemCount(); j++) {
			sum[j] = 0;
			int nVal = 0;
			for (int element : AttrN) {
				double f = dataTable.getNumericAttrValue(element, j);
				if (!Double.isNaN(f)) {
					++nVal;
					sum[j] += f;
				}
			}
			if (nVal < 1) {
				sum[j] = Double.NaN;
			} else {
				sum[j] /= nVal;
			}
		}
		return sum;
	}

	public static double[] getVarianceOfColumns(AttributeDataPortion dataTable, int AttrN[]) {
		double variance[] = new double[dataTable.getDataItemCount()];
		for (int i = 0; i < variance.length; i++) {
			DoubleArray fa = new DoubleArray(AttrN.length, 10);
			for (int element : AttrN) {
				fa.addElement(dataTable.getNumericAttrValue(element, i));
			}
			variance[i] = NumValManager.getVariance(fa);
		}
		return variance;
	}

	public static int[] getOrderOfColumn(AttributeDataPortion dataTable, int fn) {
		return getOrderOfColumn(dataTable, null, fn);
	}

	public static int[] getOrderOfColumn(AttributeDataPortion dataTable, AttributeTransformer aTrans, int fn) {
		// load table column
		double val[] = new double[dataTable.getDataItemCount()];
		if (aTrans != null) {
			for (int i = 0; i < val.length; i++) {
				val[i] = aTrans.getNumericAttrValue(fn, i);
			}
		} else {
			for (int i = 0; i < val.length; i++) {
				val[i] = dataTable.getNumericAttrValue(fn, i);
			}
		}
		return NumValManager.getOrderDecrease(val);
	}

	public static int[] getOrderOfColumnIncrease(AttributeDataPortion dataTable, int fn) {
		return getOrderOfColumnIncrease(dataTable, null, fn);
	}

	public static int[] getOrderOfColumnIncrease(AttributeDataPortion dataTable, AttributeTransformer aTrans, int fn) {
		int order[] = new int[dataTable.getDataItemCount()];
		// load table column
		double val[] = new double[dataTable.getDataItemCount()];
		if (aTrans != null) {
			for (int i = 0; i < val.length; i++) {
				val[i] = aTrans.getNumericAttrValue(fn, i);
			}
		} else {
			for (int i = 0; i < val.length; i++) {
				val[i] = dataTable.getNumericAttrValue(fn, i);
			}
		}
		return NumValManager.getOrderIncrease(val);
	}

}
