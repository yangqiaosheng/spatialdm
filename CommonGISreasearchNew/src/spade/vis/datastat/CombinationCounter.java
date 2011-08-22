package spade.vis.datastat;

import java.util.Vector;

/**
* Counts different combinations of values of two or more attributes
*/
public class CombinationCounter {
	/**
	* A vector of instances of CombinationInfo providing information about
	* particular value combinations
	*/
	protected Vector combInfos = null;
	/**
	* Current value combination read from data
	*/
	protected Vector currComb = null;

	public void combinationStart() {
		if (currComb != null) {
			currComb.removeAllElements();
		}
	}

	public void registerValue(String value) {
		if (currComb == null) {
			currComb = new Vector(5, 5);
		}
		currComb.addElement(value);
	}

	public void combinationEnd() {
		if (currComb == null || currComb.size() < 1)
			return;
		//if the current combination contains nulls (missing values), discard it
		for (int i = 0; i < currComb.size(); i++)
			if (currComb.elementAt(i) == null)
				return;
		CombinationInfo cinfo = findCombination(currComb);
		if (cinfo != null) {
			cinfo.incrementCount();
		} else {
			cinfo = new CombinationInfo();
			cinfo.setValues(currComb);
			cinfo.incrementCount();
			if (combInfos == null) {
				combInfos = new Vector(50, 20);
			}
			combInfos.addElement(cinfo);
		}
	}

	public CombinationInfo findCombination(Vector values) {
		if (values == null)
			return null;
		for (int i = 0; i < getCombinationCount(); i++) {
			CombinationInfo cinfo = getCombinationInfo(i);
			if (cinfo.sameValues(values))
				return cinfo;
		}
		return null;
	}

	public int getCombinationCount() {
		if (combInfos == null)
			return 0;
		return combInfos.size();
	}

	public CombinationInfo getCombinationInfo(int idx) {
		if (idx < 0 || idx > getCombinationCount())
			return null;
		return (CombinationInfo) combInfos.elementAt(idx);
	}

	public Vector getCombinationInfos() {
		return combInfos;
	}

	public void reset() {
		for (int i = 0; i < getCombinationCount(); i++) {
			getCombinationInfo(i).reset();
		}
	}
}