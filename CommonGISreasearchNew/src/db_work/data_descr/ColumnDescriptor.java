package db_work.data_descr;

import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: 02-Feb-2006
 * Time: 12:48:27
 * To change this template use File | Settings | File Templates.
 */
public class ColumnDescriptor {

	public String name = null;
	public String type = null;

	public boolean numsDefined = false;
	public int nUniqueValues = -1;
	public int nNulls = 0;

	protected Vector aNBins; // number of bins in each aggregation
	protected Vector aOper; // aggregation operations (Strings): COUNT; MIN; MAX; AVG; SUM; MEDIAN
	protected Vector aBAttr; // aggregation based on attribute ...; NULL for count(*)
	protected Vector aVals; // aggregation results as float[]
	protected Vector aLabels; // labels for aggregates as String[]

	public int getNAggr() {
		return (aNBins == null) ? 0 : aNBins.size();
	}

	public void addAggregation(int nBins, String oper, String baseAttr, float[] vals, String[] labels) {
		if (aNBins == null) {
			aNBins = new Vector(10, 5);
			aOper = new Vector(10, 5);
			aBAttr = new Vector(10, 5);
			aVals = new Vector(10, 5);
			aLabels = new Vector(10, 5);
		}
		aNBins.addElement(new Integer(nBins));
		aOper.addElement(oper);
		aBAttr.addElement(baseAttr);
		aVals.addElement(vals);
		aLabels.addElement(labels);
	}

	public int getANBins(int n) {
		return (aNBins == null || n >= aNBins.size()) ? 0 : ((Integer) aNBins.elementAt(n)).intValue();
	}

	public String getAOper(int n) {
		return (aOper == null || n >= aOper.size()) ? null : (String) (aOper.elementAt(n));
	}

	public String getABaseAttr(int n) {
		return (aBAttr == null || n >= aBAttr.size()) ? null : (String) (aBAttr.elementAt(n));
	}

	public float[] getAVals(int n) {
		return (aVals == null || n >= aVals.size()) ? null : (float[]) (aVals.elementAt(n));
	}

	public String[] getALabels(int n) {
		return (aLabels == null || n >= aLabels.size()) ? null : (String[]) (aLabels.elementAt(n));
	}

}
