package spade.analysis.classification;

import spade.analysis.transform.AttributeTransformer;
import spade.lib.util.DoubleArray;
import spade.lib.util.NumValManager;
import spade.lib.util.QSortAlgorithm;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.ObjectFilter;

/**
* The class implements various automatic classification methods on the basis
* of values of a numeric attribute
*/
public class AutoNumClassifier {
	// internal variables: data about table, its filter, and classification attribute
	private AttributeDataPortion dTable = null;
	private ObjectFilter tFilter = null;
	private int attrN = -1;
	private boolean useFilter = false;
	/**
	* An AutoNumClassifier may optionally be connected to a transformer of attribute
	* values. In this case, it classifies transformed attribute values.
	*/
	protected AttributeTransformer aTrans = null;
	// sorted array of data values, used by OptimalClassifier
	private double vals[] = null;
	// array of Optimal Classifiers
	private OptNumClass onc[] = null;
	double errors[][] = null;

	/*
	* constructor
	*/
	public AutoNumClassifier(AttributeDataPortion dTable, int attrN) {
		this.dTable = dTable;
		tFilter = dTable.getObjectFilter();
		this.attrN = attrN;
	}

	public void setUseFilter(boolean useFilter) {
		if (this.useFilter == useFilter)
			return;
		this.useFilter = useFilter;
		clearAll();
	}

	/**
	* Connects the classifier to a transformer of attribute values. After this,
	* it must classify transformed attribute values.
	*/
	public void setAttributeTransformer(AttributeTransformer transformer) {
		aTrans = transformer;
	}

	/**
	* Returns its transformer of attribute values (if exists)
	*/
	public AttributeTransformer getAttributeTransformer() {
		return aTrans;
	}

	/**
	* Depending on the presence of an attribute transformer, gets attribute values
	* either from the transformer or the table
	*/
	public double getNumericAttrValue(int attrN, int recN) {
		if (aTrans != null)
			return aTrans.getNumericAttrValue(attrN, recN);
		if (dTable != null)
			return dTable.getNumericAttrValue(attrN, recN);
		return Float.NaN;
	}

	/**
	* Sets the number of the table column with the values to classify
	*/
	public void setColumnNumber(int colN) {
		if (colN == attrN)
			return;
		attrN = colN;
		clearAll();
	}

	/**
	* Clears all its internal structures
	*/
	public void clearAll() {
		vals = null;
		onc = null;
		errors = null;
	}

	/**
	* Equal classes for objects from dTable according to values of the numeric
	* attribute attrN.
	* If TableFilter tf!=null, classes are generated for objects selected by query
	*/
	public float[] doEqualClasses(int ncl) {
		if (ncl < 2)
			return null;
		DoubleArray values = new DoubleArray(dTable.getDataItemCount(), 5);
		for (int i = 0; i < dTable.getDataItemCount(); i++)
			if (!useFilter || tFilter == null || tFilter.isActive(i)) {
				double val = getNumericAttrValue(attrN, i);
				if (Double.isNaN(val)) {
					continue;
				}
				values.addElement(val);
			}
		if (values.size() < 2)
			return null;
		return DoubleArray.double2float(NumValManager.breakToIntervals(values, ncl, true));
	}

	/**
	* Equal intervals for objects from dTable according to values of the numeric
	* attribute attrN.
	* If TableFilter tf!=null, classes are generated for objects selected by query
	*/
	public float[] doEqualIntervals(int ncl) {
		if (ncl < 2)
			return null;
		double min = Double.NaN, max = Double.NaN;
		for (int i = 0; i < dTable.getDataItemCount(); i++)
			if (!useFilter || tFilter == null || tFilter.isActive(i)) {
				double val = getNumericAttrValue(attrN, i);
				if (Double.isNaN(val)) {
					continue;
				}
				if (Double.isNaN(min) || min > val) {
					min = val;
				}
				if (Double.isNaN(max) || max < val) {
					max = val;
				}
			}
		return doEqualIntervals(ncl, min, max);
	}

	/**
	* Equal interval breaks for the given minimum and maximum values
	*/
	public static float[] doEqualIntervals(int ncl, double min, double max) {
		if (ncl < 2)
			return null;
		if (Double.isNaN(min) || max <= min)
			return null;
		float breaks[] = new float[ncl + 1];
		breaks[0] = (float) min;
		breaks[ncl] = (float) max;
		for (int i = 1; i < ncl; i++) {
			breaks[i] = (float) (min + i * (max - min) / ncl);
		}
		return breaks;
	}

	public float[] doNestedMeans() {
		double min = Double.NaN, max = Double.NaN, sum = 0f;
		int N = 0;
		for (int i = 0; i < dTable.getDataItemCount(); i++)
			if (!useFilter || tFilter == null || tFilter.isActive(i)) {
				double val = getNumericAttrValue(attrN, i);
				if (Double.isNaN(val)) {
					continue;
				}
				if (Double.isNaN(min) || min > val) {
					min = val;
				}
				if (Double.isNaN(max) || max < val) {
					max = val;
				}
				sum += val;
				N++;
			}
		if (Double.isNaN(min) || max <= min)
			return null;
		float breaks[] = new float[5];
		breaks[0] = (float) min;
		breaks[4] = (float) max;
		breaks[2] = (float) (sum / N);
		for (int k = 0; k <= 1; k++) {
			N = 0;
			sum = 0;
			for (int i = 0; i < dTable.getDataItemCount(); i++)
				if (!useFilter || tFilter == null || tFilter.isActive(i)) {
					double val = getNumericAttrValue(attrN, i);
					if (Double.isNaN(val)) {
						continue;
					}
					if (k == 0 && val > breaks[2]) {
						continue;
					}
					if (k == 1 && val < breaks[2]) {
						continue;
					}
					sum += val;
					N++;
				}
			breaks[(k == 0) ? 1 : 3] = (float) (sum / N);
		}
		return breaks;
	}

	/*
	* filling a list of values taking into account current filter, if needed
	*/
	public void fillVals() {
		long t = System.currentTimeMillis();
		DoubleArray values = new DoubleArray(dTable.getDataItemCount(), 1);
		for (int i = 0; i < dTable.getDataItemCount(); i++) {
			double val = getNumericAttrValue(attrN, i);
			if (Double.isNaN(val)) {
				continue;
			}
			if (!useFilter || tFilter == null || tFilter.isActive(i)) {
				values.addElement(val);
			}
		}
		if (values.size() < 2)
			return;
		vals = values.getTrimmedArray();
		QSortAlgorithm.sort(vals);
		t = System.currentTimeMillis() - t;
	}

	public void initOptimalClassification(int ncl, int subMethod) {
		if (onc == null) {
			onc = new OptNumClass[3];
		}
		if (vals == null) {
			fillVals();
		}
		if (onc[subMethod - OptNumClass.SUBMETHOD_MEAN] == null) {
			onc[subMethod - OptNumClass.SUBMETHOD_MEAN] = new OptNumClass();
			onc[subMethod - OptNumClass.SUBMETHOD_MEAN].setDataArray(vals);
			onc[subMethod - OptNumClass.SUBMETHOD_MEAN].setMethod(OptNumClass.METHOD_OPTIMAL);
			onc[subMethod - OptNumClass.SUBMETHOD_MEAN].setSubmethod(subMethod);
			onc[subMethod - OptNumClass.SUBMETHOD_MEAN].setMaxClasses(10);
			onc[subMethod - OptNumClass.SUBMETHOD_MEAN].fish(ncl); // run
		}
	}

	/**
	* Optimal classification for objects from dTable according to values of the
	* numeric attribute attrN.
	* If TableFilter tf!=null, classes are generated for objects selected by query
	*/
	public float[] doOptimalClassification(int ncl, int subMethod) {
		if (ncl < 2 || subMethod > 3)
			return null;
		if (onc == null || vals == null || onc[subMethod - OptNumClass.SUBMETHOD_MEAN] == null) {
			initOptimalClassification(ncl, subMethod);
		} else {
			onc[subMethod - OptNumClass.SUBMETHOD_MEAN].fish(ncl); // run
		}
		float breaks[] = onc[subMethod - OptNumClass.SUBMETHOD_MEAN].getClassBreakValues(ncl);
		return breaks;
	}

	/*
	* compute errors of optimal classifications
	*/
	public double[][] getOptimalClassificationErrors(int ncl, int subMethod) {
		if (ncl < 2)
			return null;
		if (onc == null || vals == null || onc[subMethod - OptNumClass.SUBMETHOD_MEAN] == null) {
			initOptimalClassification(ncl, subMethod);
		}
		if (errors == null) {
			errors = new double[3][];
		}
		onc[subMethod - OptNumClass.SUBMETHOD_MEAN].fish(ncl);
		errors[subMethod - OptNumClass.SUBMETHOD_MEAN] = onc[subMethod - OptNumClass.SUBMETHOD_MEAN].getPartitionErrors();
		return errors;
	}

	/**
	* compute error of current classification
	*/
	public float estimateError(float breaks[], int subMethod) {
		if (onc == null || vals == null || onc[subMethod - OptNumClass.SUBMETHOD_MEAN] == null) {
			initOptimalClassification(breaks.length + 1, subMethod);
		}
		double d = onc[subMethod - OptNumClass.SUBMETHOD_MEAN].getPartitionError(breaks);
		/*
		System.out.println("* error="+d);
		*/
		return (float) d;
	}

}